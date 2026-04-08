#include "oht/sim_core/graph_context.hpp"

namespace oht::sim_core {

GraphContext GraphContext::from_loaded_graph(const oht::path_finder::LoadedGraph& loaded_graph) {
    GraphContext context;

    for (const oht::path_finder::EdgeMetadata& edge : loaded_graph.metadata.edges) {
        if (edge.resource_id < 0) {
            continue;
        }

        context.resource_ids_by_edge_[DirectedEdgeKey{edge.from, edge.to}] = static_cast<int>(edge.resource_id);
    }

    return context;
}

std::optional<int> GraphContext::find_resource_id(
    oht::path_finder::NodeId from,
    oht::path_finder::NodeId to) const {
    const auto it = resource_ids_by_edge_.find(DirectedEdgeKey{from, to});
    if (it == resource_ids_by_edge_.end()) {
        return std::nullopt;
    }

    return it->second;
}

}  // namespace oht::sim_core
