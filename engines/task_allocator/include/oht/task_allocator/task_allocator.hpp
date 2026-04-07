#pragma once

#include <vector>

#include "oht/path_finder/route_planner.hpp"
#include "oht/task_allocator/job.hpp"

namespace oht::task_allocator {

class TaskAllocator {
public:
    struct Config {
        double deadhead_weight = 1.0;
        double loaded_weight = 1.0;
        double wait_penalty_weight = 0.5;
        double priority_weight = 1.0;
    };

    TaskAllocator();
    explicit TaskAllocator(Config config);

    void set_route_planner(const oht::path_finder::RoutePlanner* planner);

    std::vector<Assignment> assign(
        const std::vector<VehicleState>& vehicles,
        const std::vector<Job>& jobs) const;

private:
    struct ScoreBreakdown {
        double total = 0.0;
        double deadhead_distance = 0.0;
        double loaded_distance = 0.0;
    };

    ScoreBreakdown score_candidate(const VehicleState& vehicle, const Job& job) const;
    double route_cost(oht::path_finder::NodeId from, oht::path_finder::NodeId to) const;

    Config config_;
    const oht::path_finder::RoutePlanner* route_planner_ = nullptr;
};

}  // namespace oht::task_allocator
