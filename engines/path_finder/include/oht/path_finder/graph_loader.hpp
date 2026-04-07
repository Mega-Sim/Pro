#pragma once

#include <string>

#include "oht/path_finder/graph.hpp"
#include "oht/path_finder/graph_metadata.hpp"

namespace oht::path_finder {

struct LoadedGraph {
    Graph graph;
    GraphMetadata metadata;
};

class GraphLoader {
public:
    static LoadedGraph load_from_graph_json(const std::string& path);
};

}  // namespace oht::path_finder
