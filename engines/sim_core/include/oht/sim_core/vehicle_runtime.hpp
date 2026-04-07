#pragma once

#include <cstddef>
#include <vector>

#include "oht/path_finder/graph.hpp"

namespace oht::sim_core {

enum class VehicleState {
    Idle,
    Busy,
    Parked,
    WaitingForResource,
    OnResource,
};

struct VehicleRuntime {
    static constexpr int kInvalidId = -1;

    int vehicle_id = kInvalidId;
    int current_node = -1;
    VehicleState state = VehicleState::Idle;
    int current_resource_id = kInvalidId;
    int target_resource_id = kInvalidId;
    std::vector<oht::path_finder::NodeId> route_nodes;
    std::size_t route_index = 0;
    bool has_active_route = false;
};

}  // namespace oht::sim_core
