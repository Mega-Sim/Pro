#include "oht/path_finder/route_planner.hpp"

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <limits>
#include <queue>
#include <unordered_map>
#include <vector>

#include "oht/path_finder/compressed_graph_builder.hpp"

namespace oht::path_finder {

namespace {

constexpr double kInfinity = std::numeric_limits<double>::infinity();

struct QueueEntry {
    NodeId node = 0;
    double f_score = 0.0;
};

struct QueueEntryCompare {
    bool operator()(const QueueEntry& lhs, const QueueEntry& rhs) const {
        return lhs.f_score > rhs.f_score;
    }
};

struct AnchorResolution {
    bool resolved = false;
    NodeId anchor = 0;
    // Inclusive path segment [original_node ... anchor].
    std::vector<NodeId> expanded_segment;
};

std::vector<NodeId> reconstruct_path(
    const std::unordered_map<NodeId, NodeId>& came_from,
    NodeId current) {
    std::vector<NodeId> path{current};
    auto it = came_from.find(current);
    while (it != came_from.end()) {
        current = it->second;
        path.push_back(current);
        it = came_from.find(current);
    }
    std::reverse(path.begin(), path.end());
    return path;
}

std::unordered_map<NodeId, std::size_t> build_in_degree_map(const Graph& graph) {
    std::unordered_map<NodeId, std::size_t> in_degree;

    for (const NodeId node_id : graph.node_ids()) {
        in_degree.try_emplace(node_id, 0);
    }

    for (const NodeId from : graph.node_ids()) {
        for (const Edge& edge : graph.neighbors(from)) {
            ++in_degree[edge.to];
        }
    }

    return in_degree;
}

std::unordered_map<NodeId, std::vector<NodeId>> build_predecessors(const Graph& graph) {
    std::unordered_map<NodeId, std::vector<NodeId>> predecessors;

    for (const NodeId node_id : graph.node_ids()) {
        predecessors.try_emplace(node_id);
    }

    for (const NodeId from : graph.node_ids()) {
        for (const Edge& edge : graph.neighbors(from)) {
            predecessors[edge.to].push_back(from);
        }
    }

    return predecessors;
}

AnchorResolution resolve_forward_anchor(const Graph& graph,
                                       const CompressedGraph& compressed,
                                       const std::unordered_map<NodeId, std::size_t>& in_degree,
                                       NodeId start,
                                       std::size_t max_steps) {
    if (compressed.has_node(start)) {
        return AnchorResolution{true, start, {start}};
    }

    std::vector<NodeId> segment{start};
    NodeId current = start;
    std::size_t steps = 0;

    while (!compressed.has_node(current)) {
        const auto& outgoing = graph.neighbors(current);
        const auto in_it = in_degree.find(current);
        const std::size_t current_in_degree = (in_it != in_degree.end()) ? in_it->second : 0;

        if (outgoing.size() != 1 || current_in_degree != 1) {
            return AnchorResolution{};
        }

        current = outgoing.front().to;
        segment.push_back(current);

        ++steps;
        if (steps > max_steps) {
            return AnchorResolution{};
        }
    }

    return AnchorResolution{true, current, std::move(segment)};
}

AnchorResolution resolve_backward_anchor(const Graph& graph,
                                        const CompressedGraph& compressed,
                                        const std::unordered_map<NodeId, std::size_t>& in_degree,
                                        const std::unordered_map<NodeId, std::vector<NodeId>>& predecessors,
                                        NodeId goal,
                                        std::size_t max_steps) {
    if (compressed.has_node(goal)) {
        return AnchorResolution{true, goal, {goal}};
    }

    std::vector<NodeId> reverse_segment{goal};
    NodeId current = goal;
    std::size_t steps = 0;

    while (!compressed.has_node(current)) {
        const auto pred_it = predecessors.find(current);
        const auto in_it = in_degree.find(current);
        const std::size_t current_in_degree = (in_it != in_degree.end()) ? in_it->second : 0;

        if (pred_it == predecessors.end() || pred_it->second.size() != 1 || current_in_degree != 1) {
            return AnchorResolution{};
        }

        const NodeId predecessor = pred_it->second.front();
        if (graph.neighbors(predecessor).size() != 1) {
            return AnchorResolution{};
        }

        current = predecessor;
        reverse_segment.push_back(current);

        ++steps;
        if (steps > max_steps) {
            return AnchorResolution{};
        }
    }

    std::reverse(reverse_segment.begin(), reverse_segment.end());
    return AnchorResolution{true, current, std::move(reverse_segment)};
}

void append_segment_without_duplicate(std::vector<NodeId>& destination,
                                      const std::vector<NodeId>& segment) {
    if (segment.empty()) {
        return;
    }

    std::size_t begin = 0;
    if (!destination.empty() && destination.back() == segment.front()) {
        begin = 1;
    }

    destination.insert(destination.end(), segment.begin() + static_cast<std::ptrdiff_t>(begin), segment.end());
}

double calculate_path_cost(const Graph& graph, const std::vector<NodeId>& path) {
    if (path.size() < 2) {
        return 0.0;
    }

    double total_cost = 0.0;
    for (std::size_t i = 1; i < path.size(); ++i) {
        const NodeId from = path[i - 1];
        const NodeId to = path[i];

        bool edge_found = false;
        for (const Edge& edge : graph.neighbors(from)) {
            if (edge.to == to) {
                total_cost += edge.cost;
                edge_found = true;
                break;
            }
        }

        if (!edge_found) {
            return kInfinity;
        }
    }

    return total_cost;
}

}  // namespace

RoutePlanner::RoutePlanner(const Graph& graph)
    : graph_(graph), compressed_graph_(CompressedGraphBuilder::build(graph)) {}

PathResult RoutePlanner::find_shortest_path(NodeId start, NodeId goal) const {
    const PathResult compressed_result = find_shortest_path_compressed(start, goal);
    if (compressed_result.found) {
        return compressed_result;
    }

    // Safe hybrid behavior: if compressed search cannot safely apply, fallback to
    // the original full-graph planner so route_nodes behavior remains unchanged.
    return find_shortest_path_original(start, goal);
}

PathResult RoutePlanner::find_shortest_path_original(NodeId start, NodeId goal) const {
    PathResult result;
    if (!graph_.has_node(start) || !graph_.has_node(goal)) {
        return result;
    }

    std::unordered_map<NodeId, NodeId> came_from;
    std::unordered_map<NodeId, double> g_score;

    g_score[start] = 0.0;

    std::priority_queue<QueueEntry, std::vector<QueueEntry>, QueueEntryCompare> open_set;
    open_set.push(QueueEntry{start, heuristic(start, goal)});

    while (!open_set.empty()) {
        const NodeId current = open_set.top().node;
        open_set.pop();

        if (current == goal) {
            result.found = true;
            result.path = reconstruct_path(came_from, current);
            result.total_cost = g_score[current];
            return result;
        }

        const auto current_score = g_score.find(current);
        const double current_g = (current_score != g_score.end()) ? current_score->second : kInfinity;
        if (current_g == kInfinity) {
            continue;
        }

        for (const Edge& edge : graph_.neighbors(current)) {
            const double tentative_g_score = current_g + edge.cost;
            const auto existing_neighbor = g_score.find(edge.to);
            const double known_neighbor_score =
                (existing_neighbor != g_score.end()) ? existing_neighbor->second : kInfinity;

            if (tentative_g_score >= known_neighbor_score) {
                continue;
            }

            came_from[edge.to] = current;
            g_score[edge.to] = tentative_g_score;

            const double estimate = tentative_g_score + heuristic(edge.to, goal);
            open_set.push(QueueEntry{edge.to, estimate});
        }
    }

    return result;
}

PathResult RoutePlanner::find_shortest_path_compressed(NodeId start, NodeId goal) const {
    PathResult result;
    if (!graph_.has_node(start) || !graph_.has_node(goal)) {
        return result;
    }

    const std::size_t node_count = graph_.node_ids().size();
    const auto in_degree = build_in_degree_map(graph_);
    const auto predecessors = build_predecessors(graph_);

    const AnchorResolution start_anchor =
        resolve_forward_anchor(graph_, compressed_graph_, in_degree, start, node_count);
    const AnchorResolution goal_anchor =
        resolve_backward_anchor(graph_, compressed_graph_, in_degree, predecessors, goal, node_count);

    if (!start_anchor.resolved || !goal_anchor.resolved) {
        return result;
    }

    std::unordered_map<NodeId, NodeId> came_from;
    std::unordered_map<NodeId, std::vector<NodeId>> came_from_expanded;
    std::unordered_map<NodeId, double> g_score;

    const NodeId compressed_start = start_anchor.anchor;
    const NodeId compressed_goal = goal_anchor.anchor;

    g_score[compressed_start] = 0.0;

    std::priority_queue<QueueEntry, std::vector<QueueEntry>, QueueEntryCompare> open_set;
    open_set.push(QueueEntry{compressed_start, heuristic(compressed_start, compressed_goal)});

    while (!open_set.empty()) {
        const NodeId current = open_set.top().node;
        open_set.pop();

        if (current == compressed_goal) {
            result.found = true;

            const std::vector<NodeId> compressed_nodes = reconstruct_path(came_from, current);

            std::vector<NodeId> expanded_path;
            append_segment_without_duplicate(expanded_path, start_anchor.expanded_segment);

            if (compressed_nodes.size() == 1) {
                append_segment_without_duplicate(expanded_path, goal_anchor.expanded_segment);
            } else {
                for (std::size_t i = 1; i < compressed_nodes.size(); ++i) {
                    const NodeId node = compressed_nodes[i];
                    const auto segment_it = came_from_expanded.find(node);
                    if (segment_it == came_from_expanded.end()) {
                        result.found = false;
                        result.path.clear();
                        return result;
                    }
                    append_segment_without_duplicate(expanded_path, segment_it->second);
                }
                append_segment_without_duplicate(expanded_path, goal_anchor.expanded_segment);
            }

            result.path = std::move(expanded_path);
            result.total_cost = calculate_path_cost(graph_, result.path);
            if (result.total_cost == kInfinity) {
                result.found = false;
                result.path.clear();
                result.total_cost = 0.0;
            }
            return result;
        }

        const auto current_score = g_score.find(current);
        const double current_g = (current_score != g_score.end()) ? current_score->second : kInfinity;
        if (current_g == kInfinity) {
            continue;
        }

        for (const CompressedEdge& edge : compressed_graph_.neighbors(current)) {
            const double tentative_g_score = current_g + edge.cost;
            const auto existing_neighbor = g_score.find(edge.to);
            const double known_neighbor_score =
                (existing_neighbor != g_score.end()) ? existing_neighbor->second : kInfinity;

            if (tentative_g_score >= known_neighbor_score) {
                continue;
            }

            came_from[edge.to] = current;
            came_from_expanded[edge.to] = edge.expanded_nodes;
            g_score[edge.to] = tentative_g_score;

            const double estimate = tentative_g_score + heuristic(edge.to, compressed_goal);
            open_set.push(QueueEntry{edge.to, estimate});
        }
    }

    return result;
}

double RoutePlanner::heuristic(NodeId from, NodeId to) const {
    const Node* from_node = graph_.get_node(from);
    const Node* to_node = graph_.get_node(to);

    if (from_node == nullptr || to_node == nullptr) {
        return 0.0;
    }
    if (!from_node->x || !from_node->y || !to_node->x || !to_node->y) {
        return 0.0;
    }

    const double dx = *from_node->x - *to_node->x;
    const double dy = *from_node->y - *to_node->y;
    return std::sqrt((dx * dx) + (dy * dy));
}

}  // namespace oht::path_finder
