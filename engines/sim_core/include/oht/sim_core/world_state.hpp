#pragma once

#include <vector>

#include "oht/sim_core/vehicle_runtime.hpp"

namespace oht::sim_core {

struct WorldState {
    double current_time_sec = 0.0;
    std::vector<VehicleRuntime> vehicles;
};

}  // namespace oht::sim_core
