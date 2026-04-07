#pragma once

#include <cstdint>
#include <optional>
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
    NodeId from = 0;
    NodeId to = 0;
    EdgeKind kind = EdgeKind::Normal;
    std::optional<std::int64_t> resource_id;
    double speed_limit = 0.0;
};

struct GraphMetadata {
    std::vector<NodeMetadata> nodes;
    std::vector<EdgeMetadata> edges;
};

}  // namespace oht::path_finder
