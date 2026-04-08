#pragma once

#include <vector>

#include "oht/path_finder/compressed_graph.hpp"
#include "oht/path_finder/graph.hpp"

namespace oht::path_finder {

struct PathResult {
    bool found = false;
    std::vector<NodeId> path;
    double total_cost = 0.0;
};

class RoutePlanner {
public:
    explicit RoutePlanner(const Graph& graph);

    PathResult find_shortest_path(NodeId start, NodeId goal) const;

private:
    PathResult find_shortest_path_compressed(NodeId start, NodeId goal) const;
    PathResult find_shortest_path_original(NodeId start, NodeId goal) const;

    double heuristic(NodeId from, NodeId to) const;

    const Graph& graph_;
    // Internal optimization graph; returned route path is always expanded back
    // into the original full node-by-node sequence.
    CompressedGraph compressed_graph_;
};

}  // namespace oht::path_finder
