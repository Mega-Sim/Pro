#pragma once

#include <optional>
#include <vector>

#include "oht/path_finder/graph_loader.hpp"
#include "oht/sim_core/vehicle_runtime.hpp"

namespace oht::sim_core {

struct WorldState {
    double current_time_sec = 0.0;
    std::vector<VehicleRuntime> vehicles;
    std::optional<oht::path_finder::LoadedGraph> loaded_graph;
};

}  // namespace oht::sim_core
