#pragma once

namespace oht::sim_core {

enum class VehicleState {
    Idle,
    Busy,
    Parked,
};

struct VehicleRuntime {
    int vehicle_id = -1;
    int current_node = -1;
    VehicleState state = VehicleState::Idle;
};

}  // namespace oht::sim_core
