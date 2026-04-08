#include "oht/sim_core/simulator.hpp"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <limits>
#include <optional>
#include <string>

#include "oht/sim_core/action_batch.hpp"
#include "oht/sim_core/worker_pool.hpp"

namespace oht::sim_core {

bool Simulator::load_graph(const oht::path_finder::LoadedGraph& loaded_graph) {
    graph_context_ = std::make_shared<GraphContext>(GraphContext::from_loaded_graph(loaded_graph));
    route_planner_ = std::make_unique<oht::path_finder::RoutePlanner>(loaded_graph.graph);
    task_allocator_.set_route_planner(route_planner_.get());
    idle_controller_.set_travel_cost_fn(
        [this](oht::idle_control::NodeId from, oht::idle_control::NodeId to) -> double {
            if (!route_planner_) {
                return std::abs(static_cast<double>(from - to));
            }

            const oht::path_finder::PathResult result =
                route_planner_->find_shortest_path(
                    static_cast<oht::path_finder::NodeId>(from),
                    static_cast<oht::path_finder::NodeId>(to));
            if (!result.found) {
                return std::numeric_limits<double>::infinity();
            }

            return result.total_cost;
        });
    return true;
}

bool Simulator::spawn_vehicle(int vehicle_id, int start_node) {
    if (vehicle_id == Event::kInvalidId) {
        return false;
    }

    if (find_vehicle_const(vehicle_id) != nullptr) {
        return false;
    }

    world_.vehicles.push_back(VehicleRuntime{
        vehicle_id,
        start_node,
        VehicleState::Idle,
        VehicleRuntime::kInvalidId,
        VehicleRuntime::kInvalidId,
    });
    return true;
}

bool Simulator::set_vehicle_route(
    int vehicle_id,
    const std::vector<oht::path_finder::NodeId>& route_nodes) {
    if (route_nodes.empty()) {
        return false;
    }

    VehicleRuntime* vehicle = find_vehicle_mutable(vehicle_id);
    if (vehicle == nullptr) {
        return false;
    }

    vehicle->route_nodes = route_nodes;
    vehicle->route_index = 0;
    vehicle->has_active_route = true;
    vehicle->current_node = static_cast<int>(route_nodes.front());

    if (route_nodes.size() == 1U) {
        vehicle->has_active_route = false;
        vehicle->state = VehicleState::Idle;
    }

    return true;
}

bool Simulator::schedule_advance_route(double time_sec, int vehicle_id) {
    if (vehicle_id == Event::kInvalidId) {
        return false;
    }

    schedule_event(Event{
        time_sec,
        EventType::AdvanceRoute,
        vehicle_id,
        Event::kInvalidId,
        Event::kInvalidId,
    });
    return true;
}

bool Simulator::upsert_job(const oht::task_allocator::Job& job) {
    if (job.id == 0) {
        return false;
    }

    jobs_by_id_[static_cast<int>(job.id)] = job;
    return true;
}

void Simulator::schedule_event(const Event& event) {
    event_queue_.push(event);
}

bool Simulator::step() {
    if (event_queue_.empty()) {
        return false;
    }

    const Event event = event_queue_.pop();
    process_event(event);
    return true;
}

void Simulator::run_until(double end_time_sec) {
    while (!event_queue_.empty()) {
        const Event event = event_queue_.pop();
        if (event.time_sec > end_time_sec) {
            event_queue_.push(event);
            break;
        }

        process_event(event);
    }

    world_.current_time_sec = std::max(world_.current_time_sec, end_time_sec);
}

bool Simulator::try_reserve_resource(int resource_id, int vehicle_id) {
    return reservations_.try_reserve(resource_id, vehicle_id);
}

bool Simulator::release_reservation(int resource_id, int vehicle_id) {
    return reservations_.release_reservation(resource_id, vehicle_id);
}

void Simulator::set_worker_count(std::size_t worker_count) {
    worker_count_ = std::max<std::size_t>(1, worker_count);
    worker_pool_ = std::make_unique<WorkerPool>(worker_count_);
}

std::size_t Simulator::worker_count() const {
    return worker_count_;
}

WorldSnapshot Simulator::make_snapshot() const {
    WorldSnapshot snapshot;
    snapshot.current_time_sec = world_.current_time_sec;
    snapshot.vehicles.reserve(world_.vehicles.size());
    snapshot.graph_context = graph_context_;

    for (const VehicleRuntime& vehicle : world_.vehicles) {
        if (!vehicle.has_active_route) {
            continue;
        }

        snapshot.vehicles.push_back(WorldSnapshot::VehicleSnapshot{
            vehicle.vehicle_id,
            vehicle.current_node,
            vehicle.current_resource_id,
            vehicle.route_nodes,
            vehicle.route_index,
            vehicle.has_active_route,
        });
    }

    return snapshot;
}

std::vector<SimAction> Simulator::evaluate_active_routes_parallel() const {
    const WorldSnapshot snapshot = make_snapshot();
    if (snapshot.vehicles.empty()) {
        return {};
    }

    if (!worker_pool_ || worker_pool_->thread_count() != worker_count_) {
        worker_pool_ = std::make_unique<WorkerPool>(worker_count_);
    }

    ActionBatch action_batch(worker_pool_->thread_count());
    worker_pool_->parallel_for(
        snapshot.vehicles.size(),
        [&snapshot, &action_batch, this](std::size_t worker_index, std::size_t item_index) {
            if (!snapshot.graph_context) {
                return;
            }

            const SimAction action = evaluate_advance_route_snapshot(
                snapshot.vehicles[item_index],
                *snapshot.graph_context);
            if (action.type != SimActionType::NoOp) {
                action_batch.append(worker_index, action);
            }
        });

    return action_batch.merged_actions();
}

void Simulator::apply_actions(const std::vector<SimAction>& actions) {
    for (const SimAction& action : actions) {
        apply_action(action);
    }
}

void Simulator::advance_active_routes_parallel_once() {
    const std::vector<SimAction> actions = evaluate_active_routes_parallel();
    apply_actions(actions);
}

const WorldState& Simulator::world() const {
    return world_;
}

const oht::traffic_manager::OccupancyMap& Simulator::occupancy() const {
    return occupancy_;
}

const oht::traffic_manager::ReservationManager& Simulator::reservations() const {
    return reservations_;
}

const VehicleRuntime* Simulator::find_vehicle_const(int vehicle_id) const {
    const auto it = std::find_if(
        world_.vehicles.begin(),
        world_.vehicles.end(),
        [vehicle_id](const VehicleRuntime& vehicle) {
            return vehicle.vehicle_id == vehicle_id;
        });

    if (it == world_.vehicles.end()) {
        return nullptr;
    }

    return &(*it);
}

VehicleRuntime* Simulator::find_vehicle_mutable(int vehicle_id) {
    const auto it = std::find_if(
        world_.vehicles.begin(),
        world_.vehicles.end(),
        [vehicle_id](const VehicleRuntime& vehicle) {
            return vehicle.vehicle_id == vehicle_id;
        });

    if (it == world_.vehicles.end()) {
        return nullptr;
    }

    return &(*it);
}

SimAction Simulator::evaluate_advance_route(const VehicleRuntime& vehicle) const {
    if (!graph_context_) {
        return {};
    }

    return evaluate_advance_route_snapshot(
        WorldSnapshot::VehicleSnapshot{
            vehicle.vehicle_id,
            vehicle.current_node,
            vehicle.current_resource_id,
            vehicle.route_nodes,
            vehicle.route_index,
            vehicle.has_active_route,
        },
        *graph_context_);
}

SimAction Simulator::evaluate_advance_route_snapshot(
    const WorldSnapshot::VehicleSnapshot& vehicle,
    const GraphContext& graph_context) const {
    if (!vehicle.has_active_route) {
        return {};
    }

    if ((vehicle.route_index + 1U) >= vehicle.route_nodes.size()) {
        return SimAction{SimActionType::CompleteRoute, vehicle.vehicle_id};
    }

    const oht::path_finder::NodeId current_from = vehicle.route_nodes[vehicle.route_index];
    const oht::path_finder::NodeId current_to = vehicle.route_nodes[vehicle.route_index + 1U];

    const std::optional<int> next_resource_id = graph_context.find_resource_id(current_from, current_to);
    if (!next_resource_id.has_value()) {
        // TODO(phase-8): attach malformed-route diagnostics to action batches.
        return {};
    }

    if (vehicle.current_resource_id == VehicleRuntime::kInvalidId) {
        return SimAction{
            SimActionType::RequestEnterResource,
            vehicle.vehicle_id,
            *next_resource_id,
            static_cast<int>(current_to),
        };
    }

    if (vehicle.current_resource_id == *next_resource_id) {
        return SimAction{
            SimActionType::RequestLeaveResource,
            vehicle.vehicle_id,
            *next_resource_id,
            static_cast<int>(current_to),
        };
    }

    // TODO(phase-8): resolve cross-resource transitions through deterministic batch decisions.
    return {};
}

void Simulator::apply_enter_resource(VehicleRuntime& vehicle, int resource_id) {
    if (reservations_.can_enter(resource_id, vehicle.vehicle_id, occupancy_)) {
        const bool occupied = occupancy_.occupy(resource_id, vehicle.vehicle_id);
        if (!occupied) {
            vehicle.state = VehicleState::WaitingForResource;
            vehicle.target_resource_id = resource_id;
            return;
        }

        vehicle.state = VehicleState::OnResource;
        vehicle.current_resource_id = resource_id;
        if (vehicle.target_resource_id == resource_id) {
            vehicle.target_resource_id = VehicleRuntime::kInvalidId;
        }
        return;
    }

    vehicle.state = VehicleState::WaitingForResource;
    vehicle.target_resource_id = resource_id;
}

void Simulator::apply_leave_resource(VehicleRuntime& vehicle, int resource_id, bool advance_route) {
    if (!occupancy_.is_occupied_by(resource_id, vehicle.vehicle_id)) {
        return;
    }

    occupancy_.release(resource_id, vehicle.vehicle_id);

    if (reservations_.is_reserved_by(resource_id, vehicle.vehicle_id)) {
        reservations_.release_reservation(resource_id, vehicle.vehicle_id);
    }

    if (vehicle.current_resource_id == resource_id) {
        vehicle.current_resource_id = VehicleRuntime::kInvalidId;
    }

    if (vehicle.state == VehicleState::OnResource) {
        vehicle.state = VehicleState::Idle;
    }

    if (advance_route && vehicle.has_active_route && ((vehicle.route_index + 1U) < vehicle.route_nodes.size())) {
        const oht::path_finder::NodeId next_node = vehicle.route_nodes[vehicle.route_index + 1U];
        vehicle.current_node = static_cast<int>(next_node);
        ++vehicle.route_index;

        if ((vehicle.route_index + 1U) >= vehicle.route_nodes.size()) {
            apply_complete_route(vehicle);
        }
    }

    // TODO(phase-8): evaluate->apply batch should handle queue retry and deterministic merge order.
}

void Simulator::apply_complete_route(VehicleRuntime& vehicle) {
    vehicle.has_active_route = false;
    vehicle.target_resource_id = VehicleRuntime::kInvalidId;

    if (vehicle.current_resource_id == VehicleRuntime::kInvalidId) {
        vehicle.state = VehicleState::Idle;
    }
}

void Simulator::apply_action(const SimAction& action) {
    if (action.type == SimActionType::NoOp) {
        return;
    }

    VehicleRuntime* vehicle = find_vehicle_mutable(action.vehicle_id);
    if (vehicle == nullptr) {
        return;
    }

    switch (action.type) {
        case SimActionType::NoOp:
            return;
        case SimActionType::RequestEnterResource:
            if (action.resource_id == Event::kInvalidId) {
                return;
            }
            apply_enter_resource(*vehicle, action.resource_id);
            return;
        case SimActionType::RequestLeaveResource:
            if (action.resource_id == Event::kInvalidId) {
                return;
            }
            apply_leave_resource(*vehicle, action.resource_id, true);
            return;
        case SimActionType::AdvanceRouteIndex:
            if (vehicle->has_active_route && ((vehicle->route_index + 1U) < vehicle->route_nodes.size())) {
                vehicle->current_node = static_cast<int>(vehicle->route_nodes[vehicle->route_index + 1U]);
                ++vehicle->route_index;
            }
            return;
        case SimActionType::CompleteRoute:
            apply_complete_route(*vehicle);
            return;
    }
}

void Simulator::process_event(const Event& event) {
    world_.current_time_sec = std::max(world_.current_time_sec, event.time_sec);

    switch (event.event_type) {
        case EventType::VehicleSpawn: {
            (void)spawn_vehicle(event.vehicle_id, 0);
            return;
        }
        case EventType::JobReady:
            process_job_ready_event(event);
            return;
        case EventType::Tick:
            process_tick_event();
            return;
        case EventType::EnterResource: {
            if (event.vehicle_id == Event::kInvalidId || event.resource_id == Event::kInvalidId) {
                return;
            }

            apply_action(SimAction{
                SimActionType::RequestEnterResource,
                event.vehicle_id,
                event.resource_id,
                Event::kInvalidId,
            });
            return;
        }
        case EventType::LeaveResource: {
            if (event.vehicle_id == Event::kInvalidId || event.resource_id == Event::kInvalidId) {
                return;
            }

            VehicleRuntime* vehicle = find_vehicle_mutable(event.vehicle_id);
            if (vehicle == nullptr) {
                return;
            }

            apply_leave_resource(*vehicle, event.resource_id, false);

            // TODO(phase-4): add wait queue retry and re-entry scheduling.
            // TODO(phase-4): add multi-resource route reservation chain release.
            // TODO(phase-4): integrate graph block metadata and movement timing.
            // TODO(phase-4): add deadlock detection and recovery.
            return;
        }
        case EventType::AdvanceRoute: {
            const VehicleRuntime* vehicle = find_vehicle_const(event.vehicle_id);
            if (vehicle == nullptr) {
                return;
            }

            const SimAction action = evaluate_advance_route(*vehicle);
            apply_action(action);
            return;
        }
    }
}

void Simulator::process_job_ready_event(const Event& event) {
    if (!route_planner_) {
        // TODO(phase-4): support queue-only mode before static graph is loaded.
        return;
    }

    if (event.job_id == Event::kInvalidId) {
        // TODO(phase-4): consume batch-ready jobs once external ingestion provides queue cursor.
        return;
    }

    const auto job_it = jobs_by_id_.find(event.job_id);
    if (job_it == jobs_by_id_.end()) {
        // TODO(phase-4): wire external job store -> sim_core to avoid dropping unknown job ids.
        return;
    }

    job_queue_.remove(job_it->second.id);
    job_queue_.enqueue(job_it->second);

    std::vector<oht::task_allocator::VehicleState> available_vehicles;
    available_vehicles.reserve(world_.vehicles.size());
    for (const VehicleRuntime& vehicle : world_.vehicles) {
        if (vehicle.has_active_route || vehicle.state == VehicleState::WaitingForResource) {
            continue;
        }

        available_vehicles.push_back(oht::task_allocator::VehicleState{
            vehicle.vehicle_id,
            static_cast<oht::path_finder::NodeId>(vehicle.current_node),
            world_.current_time_sec});
    }

    const std::vector<oht::task_allocator::Assignment> assignments =
        task_allocator_.assign_from_queue(available_vehicles, job_queue_, world_.current_time_sec);

    for (const oht::task_allocator::Assignment& assignment : assignments) {
        VehicleRuntime* vehicle = find_vehicle_mutable(static_cast<int>(assignment.vehicle_id));
        if (vehicle == nullptr) {
            continue;
        }

        const auto assigned_job_it = jobs_by_id_.find(static_cast<int>(assignment.job_id));
        if (assigned_job_it == jobs_by_id_.end()) {
            // TODO(phase-4): guarantee assignment->job lookup through immutable event payload.
            continue;
        }

        const oht::task_allocator::Job& job = assigned_job_it->second;

        const oht::path_finder::PathResult to_pickup =
            route_planner_->find_shortest_path(
                static_cast<oht::path_finder::NodeId>(vehicle->current_node),
                job.pickup_node);
        if (!to_pickup.found || to_pickup.path.empty()) {
            continue;
        }

        const oht::path_finder::PathResult to_dropoff =
            route_planner_->find_shortest_path(job.pickup_node, job.dropoff_node);
        if (!to_dropoff.found || to_dropoff.path.empty()) {
            continue;
        }

        std::vector<oht::path_finder::NodeId> route_nodes = to_pickup.path;
        route_nodes.insert(route_nodes.end(), to_dropoff.path.begin() + 1, to_dropoff.path.end());
        if (!set_vehicle_route(vehicle->vehicle_id, route_nodes)) {
            continue;
        }

        vehicle->state = VehicleState::Busy;
        (void)schedule_advance_route(world_.current_time_sec, vehicle->vehicle_id);
    }

    // TODO(phase-4): keep congestion-aware dispatch behind a feature flag (default false).
    // TODO(phase-4): add predictive balancing hook without changing initial allocator behavior.
}

void Simulator::process_tick_event() {
    std::vector<oht::idle_control::IdleVehicleState> idle_vehicles;
    idle_vehicles.reserve(world_.vehicles.size());

    for (const VehicleRuntime& vehicle : world_.vehicles) {
        if (vehicle.has_active_route || vehicle.state == VehicleState::WaitingForResource) {
            continue;
        }

        oht::idle_control::IdleVehicleState idle_vehicle;
        idle_vehicle.vehicle_id = std::to_string(vehicle.vehicle_id);
        idle_vehicle.current_node = static_cast<oht::idle_control::NodeId>(vehicle.current_node);
        idle_vehicle.is_idle = (vehicle.state == VehicleState::Idle || vehicle.state == VehicleState::Parked);
        idle_vehicle.has_job = false;
        idle_vehicle.is_loaded = false;
        idle_vehicle.dwell_time_sec = world_.current_time_sec;
        idle_vehicle.last_idle_action_time_sec =
            last_idle_action_time_sec_by_vehicle_.count(vehicle.vehicle_id) > 0
                ? last_idle_action_time_sec_by_vehicle_.at(vehicle.vehicle_id)
                : 0.0;

        const auto last_target_it = last_idle_target_by_vehicle_.find(vehicle.vehicle_id);
        if (last_target_it != last_idle_target_by_vehicle_.end()) {
            idle_vehicle.last_target_node = static_cast<oht::idle_control::NodeId>(last_target_it->second);
        }

        idle_vehicles.push_back(std::move(idle_vehicle));
    }

    oht::idle_control::IdleWorldSnapshot snapshot;
    snapshot.current_time_sec = world_.current_time_sec;
    snapshot.vehicles = idle_vehicles;

    // TODO(phase-4): populate candidate_nodes from explicit parking/standby metadata.
    // NOTE(initial-engine): empty candidate list means HoldPosition no-op decisions only.
    const std::vector<oht::idle_control::IdleDecision> decisions =
        idle_controller_.decide_for_fleet(idle_vehicles, snapshot);

    for (const oht::idle_control::IdleDecision& decision : decisions) {
        if (!decision.target_node.has_value()) {
            continue;
        }

        const int vehicle_id = std::stoi(decision.vehicle_id);
        VehicleRuntime* vehicle = find_vehicle_mutable(vehicle_id);
        if (vehicle == nullptr || route_planner_ == nullptr) {
            continue;
        }

        const oht::path_finder::PathResult relocation =
            route_planner_->find_shortest_path(
                static_cast<oht::path_finder::NodeId>(vehicle->current_node),
                static_cast<oht::path_finder::NodeId>(*decision.target_node));
        if (!relocation.found || relocation.path.empty()) {
            continue;
        }

        if (!set_vehicle_route(vehicle->vehicle_id, relocation.path)) {
            continue;
        }

        vehicle->state = VehicleState::Busy;
        last_idle_action_time_sec_by_vehicle_[vehicle->vehicle_id] = world_.current_time_sec;
        last_idle_target_by_vehicle_[vehicle->vehicle_id] = relocation.path.back();
        (void)schedule_advance_route(world_.current_time_sec, vehicle->vehicle_id);
    }

    // TODO(phase-4): keep dynamic reroute disabled in initial engine.
    // TODO(phase-4): keep deadlock recovery disabled in initial engine.
    // TODO(phase-4): keep advanced zone optimization disabled in initial engine.
}

}  // namespace oht::sim_core
