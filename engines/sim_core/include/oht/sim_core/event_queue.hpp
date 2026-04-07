#pragma once

#include <cstddef>
#include <queue>
#include <vector>

#include "oht/sim_core/event.hpp"

namespace oht::sim_core {

class EventQueue {
public:
    void push(const Event& event);
    Event pop();
    bool empty() const;
    std::size_t size() const;

private:
    struct QueuedEvent {
        Event event;
        std::size_t sequence = 0;
    };

    struct Compare {
        bool operator()(const QueuedEvent& lhs, const QueuedEvent& rhs) const;
    };

    std::priority_queue<QueuedEvent, std::vector<QueuedEvent>, Compare> queue_;
    std::size_t next_sequence_ = 0;
};

}  // namespace oht::sim_core
