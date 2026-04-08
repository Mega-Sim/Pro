#include "oht/path_finder/compressed_graph_builder.hpp"

#include <unordered_map>
#include <unordered_set>
#include <vector>

namespace oht::path_finder {

namespace {

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

}  // namespace

CompressedGraph CompressedGraphBuilder::build(const Graph& graph) {
    CompressedGraph compressed;
    const std::vector<NodeId> nodes = graph.node_ids();
    if (nodes.empty()) {
        return compressed;
    }

    const auto in_degree = build_in_degree_map(graph);
    std::unordered_set<NodeId> keep_nodes;
    keep_nodes.reserve(nodes.size());

    for (const NodeId node_id : nodes) {
        const std::size_t out_degree = graph.neighbors(node_id).size();
        const auto in_degree_it = in_degree.find(node_id);
        const std::size_t node_in_degree =
            (in_degree_it != in_degree.end()) ? in_degree_it->second : 0;

        if (out_degree != 1 || node_in_degree != 1) {
            keep_nodes.insert(node_id);
        }
    }

    // Conservative fallback: avoid dropping isolated 1-in/1-out cycles.
    if (keep_nodes.empty()) {
        keep_nodes.insert(nodes.begin(), nodes.end());
    }

    for (const NodeId node_id : keep_nodes) {
        compressed.add_node(node_id);
    }

    for (const NodeId from : keep_nodes) {
        for (const Edge& edge : graph.neighbors(from)) {
            std::vector<NodeId> expanded_nodes{from, edge.to};
            double cost = edge.cost;
            NodeId current = edge.to;

            while (keep_nodes.find(current) == keep_nodes.end()) {
                const auto& neighbors = graph.neighbors(current);
                if (neighbors.size() != 1) {
                    break;
                }

                const Edge& next = neighbors.front();
                current = next.to;
                expanded_nodes.push_back(current);
                cost += next.cost;

                if (expanded_nodes.size() > (nodes.size() + 1)) {
                    break;
                }
            }

            if (!compressed.has_node(current)) {
                compressed.add_node(current);
            }
            compressed.add_edge(from, current, cost, std::move(expanded_nodes));
        }
    }

    return compressed;
}

}  // namespace oht::path_finder
