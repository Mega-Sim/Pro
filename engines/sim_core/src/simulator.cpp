#include "oht/sim_core/simulator.hpp"

#include <algorithm>
#include <cstdint>

namespace oht::sim_core {
namespace {

VehicleRuntime* find_vehicle(WorldState& world, int vehicle_id) {
    const auto it = std::find_if(
        world.vehicles.begin(),
        world.vehicles.end(),
        [vehicle_id](const VehicleRuntime& vehicle) {
            return vehicle.vehicle_id == vehicle_id;
        });

    if (it == world.vehicles.end()) {
        return nullptr;
    }

    return &(*it);
}

}  // namespace

bool Simulator::load_graph(const oht::path_finder::LoadedGraph& loaded_graph) {
    world_.loaded_graph = loaded_graph;
    return true;
}

bool Simulator::spawn_vehicle(int vehicle_id, int start_node) {
    if (vehicle_id == Event::kInvalidId) {
        return false;
    }

    if (find_vehicle(world_, vehicle_id) != nullptr) {
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

    VehicleRuntime* vehicle = find_vehicle(world_, vehicle_id);
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

const WorldState& Simulator::world() const {
    return world_;
}

const oht::traffic_manager::OccupancyMap& Simulator::occupancy() const {
    return occupancy_;
}

const oht::traffic_manager::ReservationManager& Simulator::reservations() const {
    return reservations_;
}

void Simulator::process_event(const Event& event) {
    world_.current_time_sec = std::max(world_.current_time_sec, event.time_sec);

    switch (event.event_type) {
        case EventType::VehicleSpawn: {
            (void)spawn_vehicle(event.vehicle_id, 0);
            return;
        }
        case EventType::JobReady:
            // TODO(phase-4): integrate task allocator and dispatch decisions.
            // TODO(phase-4): connect congestion-aware dispatch policy.
            return;
        case EventType::Tick:
            return;
        case EventType::EnterResource: {
            if (event.vehicle_id == Event::kInvalidId || event.resource_id == Event::kInvalidId) {
                return;
            }

            VehicleRuntime* vehicle = find_vehicle(world_, event.vehicle_id);
            if (vehicle == nullptr) {
                return;
            }

            if (reservations_.can_enter(event.resource_id, event.vehicle_id, occupancy_)) {
                const bool occupied = occupancy_.occupy(event.resource_id, event.vehicle_id);
                if (!occupied) {
                    vehicle->state = VehicleState::WaitingForResource;
                    vehicle->target_resource_id = event.resource_id;
                    return;
                }

                vehicle->state = VehicleState::OnResource;
                vehicle->current_resource_id = event.resource_id;
                if (vehicle->target_resource_id == event.resource_id) {
                    vehicle->target_resource_id = VehicleRuntime::kInvalidId;
                }
                return;
            }

            vehicle->state = VehicleState::WaitingForResource;
            vehicle->target_resource_id = event.resource_id;
            return;
        }
        case EventType::LeaveResource: {
            if (event.vehicle_id == Event::kInvalidId || event.resource_id == Event::kInvalidId) {
                return;
            }

            VehicleRuntime* vehicle = find_vehicle(world_, event.vehicle_id);
            if (vehicle == nullptr) {
                return;
            }

            if (!occupancy_.is_occupied_by(event.resource_id, event.vehicle_id)) {
                return;
            }

            occupancy_.release(event.resource_id, event.vehicle_id);

            if (reservations_.is_reserved_by(event.resource_id, event.vehicle_id)) {
                reservations_.release_reservation(event.resource_id, event.vehicle_id);
            }

            if (vehicle->current_resource_id == event.resource_id) {
                vehicle->current_resource_id = VehicleRuntime::kInvalidId;
            }

            if (vehicle->state == VehicleState::OnResource) {
                vehicle->state = VehicleState::Idle;
            }

            // TODO(phase-4): add wait queue retry and re-entry scheduling.
            // TODO(phase-4): add multi-resource route reservation chain release.
            // TODO(phase-4): integrate graph block metadata and movement timing.
            // TODO(phase-4): add deadlock detection and recovery.
            return;
        }
        case EventType::AdvanceRoute: {
            VehicleRuntime* vehicle = find_vehicle(world_, event.vehicle_id);
            if (vehicle == nullptr) {
                return;
            }

            if (!vehicle->has_active_route) {
                return;
            }

            if ((vehicle->route_index + 1U) >= vehicle->route_nodes.size()) {
                vehicle->has_active_route = false;
                if (vehicle->state != VehicleState::WaitingForResource &&
                    vehicle->state != VehicleState::OnResource) {
                    vehicle->state = VehicleState::Idle;
                }
                return;
            }

            const oht::path_finder::NodeId current_from = vehicle->route_nodes[vehicle->route_index];
            const oht::path_finder::NodeId current_to = vehicle->route_nodes[vehicle->route_index + 1U];

            if (!world_.loaded_graph.has_value()) {
                // TODO(phase-7): add route diagnostics when graph metadata is unavailable.
                return;
            }

            const oht::path_finder::EdgeMetadata* edge =
                world_.loaded_graph->metadata.find_edge(current_from, current_to);
            if (edge == nullptr || edge->resource_id < 0) {
                // TODO(phase-7): add malformed-route diagnostics and fallback strategy.
                return;
            }

            const int next_resource_id = static_cast<int>(edge->resource_id);

            if (vehicle->current_resource_id != VehicleRuntime::kInvalidId &&
                vehicle->current_resource_id != next_resource_id) {
                schedule_event(Event{
                    world_.current_time_sec,
                    EventType::LeaveResource,
                    event.vehicle_id,
                    Event::kInvalidId,
                    vehicle->current_resource_id,
                });
                return;
            }

            if (vehicle->current_resource_id == VehicleRuntime::kInvalidId) {
                schedule_event(Event{
                    world_.current_time_sec,
                    EventType::EnterResource,
                    event.vehicle_id,
                    Event::kInvalidId,
                    next_resource_id,
                });
                return;
            }

            schedule_event(Event{
                world_.current_time_sec,
                EventType::LeaveResource,
                event.vehicle_id,
                Event::kInvalidId,
                next_resource_id,
            });

            vehicle->current_node = static_cast<int>(current_to);
            ++vehicle->route_index;

            if ((vehicle->route_index + 1U) >= vehicle->route_nodes.size()) {
                vehicle->has_active_route = false;
                vehicle->state = VehicleState::Idle;
                vehicle->current_resource_id = VehicleRuntime::kInvalidId;
            }

            // TODO(phase-7): add blocked-resource retry, dwell-time, and multi-resource reservation.
            return;
        }
    }
}

}  // namespace oht::sim_core
