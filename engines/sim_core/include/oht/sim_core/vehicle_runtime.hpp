#pragma once

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
};

}  // namespace oht::sim_core
