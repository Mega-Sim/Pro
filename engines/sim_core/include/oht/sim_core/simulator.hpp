#pragma once

#include "oht/sim_core/event.hpp"
#include "oht/sim_core/event_queue.hpp"
#include "oht/sim_core/world_state.hpp"

namespace oht::sim_core {

class Simulator {
public:
    void schedule_event(const Event& event);
    bool step();
    void run_until(double end_time_sec);

    const WorldState& world() const;

private:
    void process_event(const Event& event);

    EventQueue event_queue_;
    WorldState world_;
};

}  // namespace oht::sim_core
