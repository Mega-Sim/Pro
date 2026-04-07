#pragma once

namespace oht::sim_core {

enum class EventType {
    VehicleSpawn,
    JobReady,
    Tick,
    EnterResource,
    LeaveResource,
};

struct Event {
    static constexpr int kInvalidId = -1;

    double time_sec = 0.0;
    EventType event_type = EventType::Tick;
    int vehicle_id = kInvalidId;
    int job_id = kInvalidId;
    int resource_id = kInvalidId;
};

}  // namespace oht::sim_core
