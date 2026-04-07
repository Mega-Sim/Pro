#include "oht/sim_core/simulator.hpp"

#include <algorithm>
#include <vector>

namespace oht::sim_core {

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
    std::vector<Event> deferred_events;

    while (!event_queue_.empty()) {
        const Event event = event_queue_.pop();
        if (event.time_sec > end_time_sec) {
            deferred_events.push_back(event);
            break;
        }

        process_event(event);
    }

    for (const Event& event : deferred_events) {
        event_queue_.push(event);
    }

    world_.current_time_sec = std::max(world_.current_time_sec, end_time_sec);
}

const WorldState& Simulator::world() const {
    return world_;
}

void Simulator::process_event(const Event& event) {
    world_.current_time_sec = std::max(world_.current_time_sec, event.time_sec);

    switch (event.event_type) {
        case EventType::VehicleSpawn: {
            const auto it = std::find_if(
                world_.vehicles.begin(),
                world_.vehicles.end(),
                [&event](const VehicleRuntime& vehicle) {
                    return vehicle.vehicle_id == event.vehicle_id;
                });
            if (it != world_.vehicles.end()) {
                return;
            }

            world_.vehicles.push_back(VehicleRuntime{event.vehicle_id, 0, VehicleState::Idle});
            return;
        }
        case EventType::JobReady:
            // TODO(phase-2): integrate task_allocator dispatch flow.
            // TODO(phase-2): integrate idle_control repositioning policy.
            // TODO(phase-2): wire graph loader / graph compaction inputs.
            // TODO(phase-2): add reservation / occupancy / deadlock handling.
            return;
        case EventType::Tick:
            return;
    }
}

}  // namespace oht::sim_core
