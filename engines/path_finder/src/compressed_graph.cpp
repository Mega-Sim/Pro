#include "oht/path_finder/compressed_graph.hpp"

#include <stdexcept>
#include <utility>

namespace oht::path_finder {

namespace {

const std::vector<CompressedEdge>& empty_edges() {
    static const std::vector<CompressedEdge> kEmpty;
    return kEmpty;
}

}  // namespace

void CompressedGraph::add_node(NodeId id) {
    nodes_.insert_or_assign(id, CompressedNode{id});
    adjacency_.try_emplace(id);
}

void CompressedGraph::add_edge(NodeId from,
                               NodeId to,
                               double cost,
                               std::vector<NodeId> expanded_nodes) {
    if (!has_node(from) || !has_node(to)) {
        throw std::invalid_argument("add_edge requires existing from/to nodes");
    }

    adjacency_[from].push_back(
        CompressedEdge{from, to, cost, std::move(expanded_nodes)});
}

bool CompressedGraph::has_node(NodeId id) const {
    return nodes_.find(id) != nodes_.end();
}

const std::vector<CompressedEdge>& CompressedGraph::neighbors(NodeId id) const {
    const auto it = adjacency_.find(id);
    if (it == adjacency_.end()) {
        return empty_edges();
    }
    return it->second;
}

}  // namespace oht::path_finder
