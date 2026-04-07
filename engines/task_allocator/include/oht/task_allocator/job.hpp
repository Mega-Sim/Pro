#pragma once

#include <cstdint>

#include "oht/path_finder/graph.hpp"

namespace oht::task_allocator {

using VehicleId = std::int64_t;
using JobId = std::int64_t;

struct VehicleState {
    VehicleId id = 0;
    oht::path_finder::NodeId current_node = 0;
    double available_at = 0.0;
};

struct Job {
    JobId id = 0;
    oht::path_finder::NodeId pickup_node = 0;
    oht::path_finder::NodeId dropoff_node = 0;
    double ready_time = 0.0;
    double priority = 0.0;
};

struct Assignment {
    VehicleId vehicle_id = 0;
    JobId job_id = 0;
    double score = 0.0;
    double deadhead_distance = 0.0;
    double loaded_distance = 0.0;
};

}  // namespace oht::task_allocator
