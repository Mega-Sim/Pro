#pragma once

#include <vector>

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
    double heuristic(NodeId from, NodeId to) const;

    const Graph& graph_;
};

}  // namespace oht::path_finder
