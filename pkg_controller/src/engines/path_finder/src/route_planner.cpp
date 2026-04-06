#include "oht/path_finder/route_planner.hpp"

#include <algorithm>
#include <cmath>
#include <limits>
#include <queue>
#include <unordered_map>

namespace oht::path_finder {

namespace {

struct QueueEntry {
    NodeId node = 0;
    double f_score = 0.0;
};

struct QueueEntryCompare {
    bool operator()(const QueueEntry& lhs, const QueueEntry& rhs) const {
        return lhs.f_score > rhs.f_score;
    }
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

}  // namespace

RoutePlanner::RoutePlanner(const Graph& graph) : graph_(graph) {}

PathResult RoutePlanner::find_shortest_path(NodeId start, NodeId goal) const {
    PathResult result;
    if (!graph_.has_node(start) || !graph_.has_node(goal)) {
        return result;
    }

    constexpr double kInfinity = std::numeric_limits<double>::infinity();
    std::unordered_map<NodeId, NodeId> came_from;
    std::unordered_map<NodeId, double> g_score;
    std::unordered_map<NodeId, double> f_score;

    g_score[start] = 0.0;
    f_score[start] = heuristic(start, goal);

    std::priority_queue<QueueEntry, std::vector<QueueEntry>, QueueEntryCompare> open_set;
    open_set.push(QueueEntry{start, f_score[start]});

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
            f_score[edge.to] = estimate;
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
