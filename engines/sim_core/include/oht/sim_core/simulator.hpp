#pragma once

#include "oht/sim_core/event.hpp"
#include "oht/sim_core/event_queue.hpp"
#include "oht/sim_core/world_state.hpp"
#include "oht/traffic_manager/occupancy_map.hpp"
#include "oht/traffic_manager/reservation_manager.hpp"

namespace oht::sim_core {

class Simulator {
public:
    void schedule_event(const Event& event);
    bool step();
    void run_until(double end_time_sec);

    bool try_reserve_resource(int resource_id, int vehicle_id);
    bool release_reservation(int resource_id, int vehicle_id);

    const WorldState& world() const;
    const oht::traffic_manager::OccupancyMap& occupancy() const;
    const oht::traffic_manager::ReservationManager& reservations() const;

private:
    void process_event(const Event& event);

    EventQueue event_queue_;
    WorldState world_;
    oht::traffic_manager::OccupancyMap occupancy_;
    oht::traffic_manager::ReservationManager reservations_;
};

}  // namespace oht::sim_core
