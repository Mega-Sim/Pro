#include "oht/sim_core/simulator.hpp"

#include <algorithm>

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
            if (event.vehicle_id == Event::kInvalidId) {
                return;
            }

            if (find_vehicle(world_, event.vehicle_id) != nullptr) {
                return;
            }

            world_.vehicles.push_back(VehicleRuntime{
                event.vehicle_id,
                0,
                VehicleState::Idle,
                VehicleRuntime::kInvalidId,
                VehicleRuntime::kInvalidId,
            });
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
    }
}

}  // namespace oht::sim_core
