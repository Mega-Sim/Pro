#include "oht/path_finder/graph.hpp"

#include <stdexcept>

namespace oht::path_finder {

namespace {

const std::vector<Edge>& empty_edges() {
    static const std::vector<Edge> kEmpty;
    return kEmpty;
}

}  // namespace

void Graph::add_node(NodeId id) {
    nodes_.insert_or_assign(id, Node{id});
    adjacency_.try_emplace(id);
}

void Graph::add_node(NodeId id, double x, double y) {
    nodes_.insert_or_assign(id, Node{id, x, y});
    adjacency_.try_emplace(id);
}

void Graph::add_edge(NodeId from, NodeId to, double distance, double cost) {
    if (!has_node(from) || !has_node(to)) {
        throw std::invalid_argument("add_edge requires existing from/to nodes");
    }
    adjacency_[from].push_back(Edge{to, distance, cost});
}

bool Graph::has_node(NodeId id) const {
    return nodes_.find(id) != nodes_.end();
}

const std::vector<Edge>& Graph::neighbors(NodeId id) const {
    const auto it = adjacency_.find(id);
    if (it == adjacency_.end()) {
        return empty_edges();
    }
    return it->second;
}

const Node* Graph::get_node(NodeId id) const {
    const auto it = nodes_.find(id);
    if (it == nodes_.end()) {
        return nullptr;
    }
    return &it->second;
}

}  // namespace oht::path_finder
