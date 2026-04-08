#pragma once

#include "oht/path_finder/compressed_graph.hpp"
#include "oht/path_finder/graph.hpp"

namespace oht::path_finder {

class CompressedGraphBuilder {
public:
    static CompressedGraph build(const Graph& graph);
};

}  // namespace oht::path_finder
