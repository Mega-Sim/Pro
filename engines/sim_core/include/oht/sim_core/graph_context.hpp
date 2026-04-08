#pragma once

#include <cstddef>
#include <cstdint>
#include <functional>
#include <optional>
#include <unordered_map>

#include "oht/path_finder/graph_loader.hpp"

namespace oht::sim_core {

class GraphContext {
public:
    GraphContext() = default;

    static GraphContext from_loaded_graph(const oht::path_finder::LoadedGraph& loaded_graph);

    [[nodiscard]] std::optional<int> find_resource_id(
        oht::path_finder::NodeId from,
        oht::path_finder::NodeId to) const;

private:
    struct DirectedEdgeKey {
        oht::path_finder::NodeId from = 0;
        oht::path_finder::NodeId to = 0;

        bool operator==(const DirectedEdgeKey& other) const {
            return from == other.from && to == other.to;
        }
    };

    struct DirectedEdgeKeyHash {
        std::size_t operator()(const DirectedEdgeKey& key) const {
            const std::size_t from_hash = std::hash<oht::path_finder::NodeId>{}(key.from);
            const std::size_t to_hash = std::hash<oht::path_finder::NodeId>{}(key.to);
            return from_hash ^ (to_hash << 1U);
        }
    };

    std::unordered_map<DirectedEdgeKey, int, DirectedEdgeKeyHash> resource_ids_by_edge_;
};

}  // namespace oht::sim_core
