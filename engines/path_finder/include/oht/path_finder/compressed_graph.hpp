#pragma once

#include <unordered_map>
#include <vector>

#include "oht/path_finder/graph.hpp"

namespace oht::path_finder {

// A compact decision-graph node used for future path-search acceleration.
// Nodes are expected to represent branch points and required endpoints.
struct CompressedNode {
    NodeId id = 0;
};

// A compact edge between two compressed nodes.
// expanded_nodes keeps the full original node sequence for this corridor so a
// future planner can rebuild route_nodes for sim_core without losing fidelity.
struct CompressedEdge {
    NodeId from = 0;
    NodeId to = 0;
    double cost = 0.0;
    std::vector<NodeId> expanded_nodes;
};

// Data model only: this graph is not wired into RoutePlanner yet.
class CompressedGraph {
public:
    void add_node(NodeId id);
    void add_edge(NodeId from, NodeId to, double cost, std::vector<NodeId> expanded_nodes);

    bool has_node(NodeId id) const;
    const std::vector<CompressedEdge>& neighbors(NodeId id) const;

private:
    std::unordered_map<NodeId, CompressedNode> nodes_;
    std::unordered_map<NodeId, std::vector<CompressedEdge>> adjacency_;
};

}  // namespace oht::path_finder
