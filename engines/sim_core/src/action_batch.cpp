#include "oht/sim_core/action_batch.hpp"

#include <algorithm>

namespace oht::sim_core {

ActionBatch::ActionBatch(std::size_t worker_count)
    : per_worker_actions_(std::max<std::size_t>(1, worker_count)) {}

void ActionBatch::append(std::size_t worker_index, const SimAction& action) {
    if (worker_index >= per_worker_actions_.size()) {
        return;
    }

    auto& worker_actions = per_worker_actions_[worker_index];
    worker_actions.push_back(WorkerAction{action, worker_index, worker_actions.size()});
}

std::vector<SimAction> ActionBatch::merged_actions() const {
    std::vector<WorkerAction> all_actions;
    for (const auto& worker_actions : per_worker_actions_) {
        all_actions.insert(all_actions.end(), worker_actions.begin(), worker_actions.end());
    }

    std::sort(
        all_actions.begin(),
        all_actions.end(),
        [](const WorkerAction& lhs, const WorkerAction& rhs) {
            if (lhs.action.vehicle_id != rhs.action.vehicle_id) {
                return lhs.action.vehicle_id < rhs.action.vehicle_id;
            }

            const int lhs_type = static_cast<int>(lhs.action.type);
            const int rhs_type = static_cast<int>(rhs.action.type);
            if (lhs_type != rhs_type) {
                return lhs_type < rhs_type;
            }

            if (lhs.insertion_sequence != rhs.insertion_sequence) {
                return lhs.insertion_sequence < rhs.insertion_sequence;
            }

            if (lhs.worker_index != rhs.worker_index) {
                return lhs.worker_index < rhs.worker_index;
            }

            if (lhs.action.resource_id != rhs.action.resource_id) {
                return lhs.action.resource_id < rhs.action.resource_id;
            }

            return lhs.action.next_node < rhs.action.next_node;
        });

    std::vector<SimAction> merged;
    merged.reserve(all_actions.size());
    for (const WorkerAction& worker_action : all_actions) {
        merged.push_back(worker_action.action);
    }

    return merged;
}

}  // namespace oht::sim_core
