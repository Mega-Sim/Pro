#include "oht/sim_core/event_queue.hpp"

#include <stdexcept>

namespace oht::sim_core {

void EventQueue::push(const Event& event) {
    queue_.push(QueuedEvent{event, next_sequence_++});
}

Event EventQueue::pop() {
    if (queue_.empty()) {
        throw std::runtime_error("EventQueue::pop called on empty queue");
    }

    const Event event = queue_.top().event;
    queue_.pop();
    return event;
}

bool EventQueue::empty() const {
    return queue_.empty();
}

std::size_t EventQueue::size() const {
    return queue_.size();
}

bool EventQueue::Compare::operator()(const QueuedEvent& lhs, const QueuedEvent& rhs) const {
    if (lhs.event.time_sec == rhs.event.time_sec) {
        return lhs.sequence > rhs.sequence;
    }

    return lhs.event.time_sec > rhs.event.time_sec;
}

}  // namespace oht::sim_core
