#pragma once

#include <cstddef>
#include <memory>
#include <vector>

#include "oht/path_finder/graph.hpp"

namespace oht::sim_core {

class GraphContext;

struct WorldSnapshot {
    struct VehicleSnapshot {
        int vehicle_id = -1;
        int current_node = -1;
        int current_resource_id = -1;
        std::vector<oht::path_finder::NodeId> route_nodes;
        std::size_t route_index = 0;
        bool has_active_route = false;
    };

    double current_time_sec = 0.0;
    std::vector<VehicleSnapshot> vehicles;
    std::shared_ptr<const GraphContext> graph_context;
};

}  // namespace oht::sim_core
