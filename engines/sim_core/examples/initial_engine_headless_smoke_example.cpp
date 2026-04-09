#include <algorithm>
#include <cstdint>
#include <exception>
#include <fstream>
#include <iostream>
#include <optional>
#include <string>
#include <unordered_set>
#include <utility>
#include <vector>

#include "oht/path_finder/graph_loader.hpp"
#include "oht/path_finder/route_planner.hpp"
#include "oht/sim_core/event.hpp"
#include "oht/sim_core/simulator.hpp"
#include "oht/sim_core/vehicle_runtime.hpp"
#include "oht/task_allocator/job.hpp"

namespace {

std::optional<std::string> resolve_graph_json_path() {
    const std::vector<std::string> candidates = {
        "graph_maker/graph.json",
        "../graph_maker/graph.json",
        "../../graph_maker/graph.json",
        "../../../graph_maker/graph.json",
    };

    for (const std::string& path : candidates) {
        std::ifstream file(path);
        if (file.good()) {
            return path;
        }
    }

    return std::nullopt;
}

const oht::path_finder::NodeMetadata* find_node_metadata(
    const oht::path_finder::GraphMetadata& metadata,
    oht::path_finder::NodeId node_id) {
    const auto it = std::find_if(
        metadata.nodes.begin(),
        metadata.nodes.end(),
        [node_id](const oht::path_finder::NodeMetadata& node) {
            return node.node_id == node_id;
        });
    if (it == metadata.nodes.end()) {
        return nullptr;
    }

    return &(*it);
}

std::optional<std::pair<oht::path_finder::NodeId, oht::path_finder::NodeId>> find_reachable_job_pair(
    const oht::path_finder::GraphMetadata& metadata,
    oht::path_finder::RoutePlanner& planner) {
    for (const oht::path_finder::NodeMetadata& from : metadata.nodes) {
        for (const oht::path_finder::NodeMetadata& to : metadata.nodes) {
            if (from.node_id == to.node_id) {
                continue;
            }

            const oht::path_finder::PathResult path =
                planner.find_shortest_path(from.node_id, to.node_id);
            if (path.found && path.path.size() >= 2U) {
                return std::make_pair(from.node_id, to.node_id);
            }
        }
    }

    return std::nullopt;
}

bool can_reach_pickup(
    const std::vector<oht::sim_core::VehicleRuntime>& vehicles,
    oht::path_finder::RoutePlanner& planner,
    oht::path_finder::NodeId pickup_node) {
    for (const oht::sim_core::VehicleRuntime& vehicle : vehicles) {
        const oht::path_finder::PathResult to_pickup =
            planner.find_shortest_path(static_cast<oht::path_finder::NodeId>(vehicle.current_node), pickup_node);
        if (to_pickup.found && !to_pickup.path.empty()) {
            return true;
        }
    }

    return false;
}

}  // namespace

int main() {
    try {
        const std::optional<std::string> graph_path = resolve_graph_json_path();
        if (!graph_path.has_value()) {
            std::cerr << "[smoke] FAIL: graph_maker/graph.json 경로를 찾지 못했습니다.\n";
            return 1;
        }

        const oht::path_finder::LoadedGraph loaded_graph =
            oht::path_finder::GraphLoader::load_from_graph_json(*graph_path);
        if (loaded_graph.metadata.nodes.size() < 4U) {
            std::cerr << "[smoke] FAIL: 예제 실행에 필요한 노드 수(>=4)가 부족합니다.\n";
            return 1;
        }

        const oht::path_finder::NodeId start_a = loaded_graph.metadata.nodes[0].node_id;
        const oht::path_finder::NodeId start_b = loaded_graph.metadata.nodes[1].node_id;
        const oht::path_finder::NodeId start_c = loaded_graph.metadata.nodes[2].node_id;
        oht::path_finder::RoutePlanner planner(loaded_graph.graph);
        const std::optional<std::pair<oht::path_finder::NodeId, oht::path_finder::NodeId>> reachable_pair =
            find_reachable_job_pair(loaded_graph.metadata, planner);
        if (!reachable_pair.has_value()) {
            std::cerr << "[smoke] FAIL: graph 안에서 reachable pickup/dropoff 쌍을 찾지 못했습니다.\n";
            return 1;
        }

        const oht::path_finder::NodeId pickup = reachable_pair->first;
        const oht::path_finder::NodeId reachable_dropoff = reachable_pair->second;
        const oht::path_finder::NodeId invalid_dropoff = static_cast<oht::path_finder::NodeId>(1'000'000'007LL);

        oht::sim_core::Simulator simulator;
        if (!simulator.load_graph(loaded_graph)) {
            std::cerr << "[smoke] FAIL: simulator.load_graph 실패\n";
            return 1;
        }

        const bool spawned_all =
            simulator.spawn_vehicle(101, static_cast<int>(start_a)) &&
            simulator.spawn_vehicle(202, static_cast<int>(start_b)) &&
            simulator.spawn_vehicle(303, static_cast<int>(start_c));
        if (!spawned_all) {
            std::cerr << "[smoke] FAIL: vehicle spawn 실패\n";
            return 1;
        }

        const std::vector<oht::task_allocator::Job> jobs = {
            oht::task_allocator::Job{1001, pickup, reachable_dropoff, 0.0, 60.0},
            oht::task_allocator::Job{1002, pickup, invalid_dropoff, 0.0, 60.0},
            oht::task_allocator::Job{1003, pickup, reachable_dropoff, 0.0, 60.0},
        };

        int path_not_found_count = 0;
        for (const oht::task_allocator::Job& job : jobs) {
            const oht::path_finder::PathResult pickup_to_dropoff =
                planner.find_shortest_path(job.pickup_node, job.dropoff_node);
            const bool pickup_to_dropoff_ok = pickup_to_dropoff.found && !pickup_to_dropoff.path.empty();
            const bool any_vehicle_can_reach_pickup =
                can_reach_pickup(simulator.world().vehicles, planner, job.pickup_node);
            if (!pickup_to_dropoff_ok || !any_vehicle_can_reach_pickup) {
                ++path_not_found_count;
            }

            if (!simulator.upsert_job(job)) {
                std::cerr << "[smoke] FAIL: job upsert 실패 id=" << job.id << "\n";
                return 1;
            }
            simulator.schedule_event(oht::sim_core::Event{
                0.0,
                oht::sim_core::EventType::JobReady,
                oht::sim_core::Event::kInvalidId,
                static_cast<int>(job.id),
                oht::sim_core::Event::kInvalidId,
            });
        }

        simulator.run_until(0.0);

        int assigned_job_count = 0;
        for (const oht::sim_core::VehicleRuntime& vehicle : simulator.world().vehicles) {
            if (vehicle.has_active_route) {
                ++assigned_job_count;
            }
        }

        for (int step = 0; step < 3; ++step) {
            const double event_time_sec = 1.0 + static_cast<double>(step);
            for (const oht::sim_core::VehicleRuntime& vehicle : simulator.world().vehicles) {
                if (!vehicle.has_active_route) {
                    continue;
                }
                simulator.schedule_event(oht::sim_core::Event{
                    event_time_sec,
                    oht::sim_core::EventType::AdvanceRoute,
                    vehicle.vehicle_id,
                    oht::sim_core::Event::kInvalidId,
                    oht::sim_core::Event::kInvalidId,
                });
            }
            simulator.run_until(event_time_sec);
        }

        std::unordered_set<int> idle_before_tick;
        for (const oht::sim_core::VehicleRuntime& vehicle : simulator.world().vehicles) {
            const bool is_idle =
                !vehicle.has_active_route &&
                (vehicle.state == oht::sim_core::VehicleState::Idle ||
                 vehicle.state == oht::sim_core::VehicleState::Parked);
            if (is_idle) {
                idle_before_tick.insert(vehicle.vehicle_id);
            }
        }

        simulator.schedule_event(oht::sim_core::Event{
            10.0,
            oht::sim_core::EventType::Tick,
            oht::sim_core::Event::kInvalidId,
            oht::sim_core::Event::kInvalidId,
            oht::sim_core::Event::kInvalidId,
        });
        simulator.run_until(10.0);

        int moving_vehicle_count = 0;
        int idle_relocation_count = 0;
        int parking_result_count = 0;
        int standby_result_count = 0;
        for (const oht::sim_core::VehicleRuntime& vehicle : simulator.world().vehicles) {
            if (vehicle.has_active_route) {
                ++moving_vehicle_count;
            }

            if (idle_before_tick.count(vehicle.vehicle_id) == 0U || !vehicle.has_active_route) {
                continue;
            }

            ++idle_relocation_count;
            if (vehicle.route_nodes.empty()) {
                continue;
            }

            const oht::path_finder::NodeId target = vehicle.route_nodes.back();
            const oht::path_finder::NodeMetadata* target_metadata = find_node_metadata(loaded_graph.metadata, target);
            if (target_metadata == nullptr) {
                continue;
            }

            if (target_metadata->kind == oht::path_finder::NodeKind::ParkingCandidate) {
                ++parking_result_count;
            } else if (target_metadata->kind == oht::path_finder::NodeKind::WaitCandidate) {
                ++standby_result_count;
            }
        }

        const int hold_result_count =
            static_cast<int>(idle_before_tick.size()) - idle_relocation_count;

        std::cout << "[smoke] graph=" << *graph_path << "\n";
        std::cout << "[smoke] assigned_jobs=" << assigned_job_count << "\n";
        std::cout << "[smoke] path_not_found=" << path_not_found_count << "\n";
        std::cout << "[smoke] moving_vehicles=" << moving_vehicle_count << "\n";
        std::cout << "[smoke] idle_relocation=" << idle_relocation_count << "\n";
        std::cout << "[smoke] idle_result parking=" << parking_result_count
                  << " standby=" << standby_result_count
                  << " hold=" << hold_result_count << "\n";

        return 0;
    } catch (const std::exception& ex) {
        std::cerr << "[smoke] FAIL: " << ex.what() << "\n";
        return 1;
    }
}
