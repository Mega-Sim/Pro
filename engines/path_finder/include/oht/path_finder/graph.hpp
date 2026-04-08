#pragma once

#include <cstdint>
#include <optional>
#include <unordered_map>
#include <vector>

namespace oht::path_finder {

using NodeId = std::int64_t;

struct Node {
    NodeId id = 0;
    std::optional<double> x;
    std::optional<double> y;
};

struct Edge {
    NodeId to = 0;
    double distance = 0.0;
    double cost = 0.0;
};

class Graph {
public:
    void add_node(NodeId id);
    void add_node(NodeId id, double x, double y);
    void add_edge(NodeId from, NodeId to, double distance, double cost);
    bool has_node(NodeId id) const;
    const std::vector<Edge>& neighbors(NodeId id) const;
    const Node* get_node(NodeId id) const;
    std::vector<NodeId> node_ids() const;

private:
    std::unordered_map<NodeId, Node> nodes_;
    std::unordered_map<NodeId, std::vector<Edge>> adjacency_;
};

}  // namespace oht::path_finder
