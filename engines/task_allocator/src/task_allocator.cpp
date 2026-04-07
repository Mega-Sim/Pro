#include "oht/task_allocator/task_allocator.hpp"

#include <cmath>
#include <limits>
#include <unordered_set>

namespace oht::task_allocator {

TaskAllocator::TaskAllocator() = default;

TaskAllocator::TaskAllocator(Config config) : config_(config) {}

void TaskAllocator::set_route_planner(const oht::path_finder::RoutePlanner* planner) {
    route_planner_ = planner;
}

std::vector<Assignment> TaskAllocator::assign(
    const std::vector<VehicleState>& vehicles,
    const std::vector<Job>& jobs) const {
    std::vector<Assignment> assignments;
    assignments.reserve(jobs.size());

    std::unordered_set<VehicleId> used_vehicles;

    for (const Job& job : jobs) {
        const VehicleState* best_vehicle = nullptr;
        ScoreBreakdown best_score;
        best_score.total = std::numeric_limits<double>::infinity();

        for (const VehicleState& vehicle : vehicles) {
            if (used_vehicles.find(vehicle.id) != used_vehicles.end()) {
                continue;
            }

            const ScoreBreakdown candidate = score_candidate(vehicle, job);
            if (candidate.total >= best_score.total) {
                continue;
            }

            best_vehicle = &vehicle;
            best_score = candidate;
        }

        if (best_vehicle == nullptr) {
            continue;
        }

        assignments.push_back(Assignment{
            best_vehicle->id,
            job.id,
            best_score.total,
            best_score.deadhead_distance,
            best_score.loaded_distance});
        used_vehicles.insert(best_vehicle->id);
    }

    return assignments;
}

std::vector<Assignment> TaskAllocator::assign_from_queue(
    const std::vector<VehicleState>& vehicles,
    JobQueue& job_queue,
    double now) const {
    if (vehicles.empty() || job_queue.empty()) {
        return {};
    }

    const std::vector<Job> ready_jobs = job_queue.list_ready(now, vehicles.size());
    if (ready_jobs.empty()) {
        return {};
    }

    std::vector<Assignment> assignments = assign(vehicles, ready_jobs);
    for (const Assignment& assignment : assignments) {
        job_queue.remove(assignment.job_id);
    }

    return assignments;
}

TaskAllocator::ScoreBreakdown TaskAllocator::score_candidate(
    const VehicleState& vehicle,
    const Job& job) const {
    ScoreBreakdown score;

    score.deadhead_distance = route_cost(vehicle.current_node, job.pickup_node);
    score.loaded_distance = route_cost(job.pickup_node, job.dropoff_node);

    const double wait_penalty = std::max(0.0, vehicle.available_at - job.ready_time);
    const double priority_adjustment = config_.priority_weight * job.priority;

    score.total =
        (config_.deadhead_weight * score.deadhead_distance) +
        (config_.loaded_weight * score.loaded_distance) +
        (config_.wait_penalty_weight * wait_penalty) -
        priority_adjustment;

    return score;
}

double TaskAllocator::route_cost(oht::path_finder::NodeId from, oht::path_finder::NodeId to) const {
    if (from == to) {
        return 0.0;
    }

    if (route_planner_ == nullptr) {
        return std::abs(static_cast<double>(from - to));
    }

    const oht::path_finder::PathResult path_result = route_planner_->find_shortest_path(from, to);
    if (!path_result.found) {
        return std::numeric_limits<double>::infinity();
    }

    return path_result.total_cost;
}

}  // namespace oht::task_allocator
