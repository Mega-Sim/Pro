#pragma once

#include <cstdint>
#include <optional>
#include <unordered_map>
#include <vector>

#include "oht/path_finder/graph.hpp"

namespace oht::path_finder {

enum class NodeKind {
    Normal,
    Junction,
    StationCandidate,
    ParkingCandidate,
    WaitCandidate,
};

enum class EdgeKind {
    Normal,
    MainFlowCandidate,
    BranchCandidate,
    MergeCandidate,
};

struct NodeMetadata {
    NodeId node_id = 0;
    NodeKind kind = NodeKind::Normal;
    std::optional<std::int64_t> zone_id;
    bool is_protected = false;
};

struct EdgeMetadata {
    std::int64_t resource_id = -1;
    NodeId from = 0;
    NodeId to = 0;
    EdgeKind kind = EdgeKind::Normal;
    double speed_limit = 0.0;
};

struct GraphMetadata {
    std::vector<NodeMetadata> nodes;
    std::vector<EdgeMetadata> edges;
    std::unordered_map<std::int64_t, std::size_t> resource_to_edge_index;

    [[nodiscard]] bool has_resource(std::int64_t resource_id) const {
        return resource_to_edge_index.find(resource_id) != resource_to_edge_index.end();
    }

    [[nodiscard]] const EdgeMetadata* find_edge(NodeId from, NodeId to) const {
        for (const EdgeMetadata& edge : edges) {
            if (edge.from == from && edge.to == to) {
                return &edge;
            }
        }
        return nullptr;
    }

    [[nodiscard]] const EdgeMetadata* find_edge_by_resource_id(std::int64_t resource_id) const {
        const auto it = resource_to_edge_index.find(resource_id);
        if (it == resource_to_edge_index.end()) {
            return nullptr;
        }
        return &edges[it->second];
    }
};

}  // namespace oht::path_finder
