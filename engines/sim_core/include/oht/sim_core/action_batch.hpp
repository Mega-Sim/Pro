#pragma once

#include <cstddef>
#include <vector>

#include "oht/sim_core/sim_action.hpp"

namespace oht::sim_core {

class ActionBatch {
public:
    explicit ActionBatch(std::size_t worker_count);

    void append(std::size_t worker_index, const SimAction& action);
    std::vector<SimAction> merged_actions() const;

private:
    struct WorkerAction {
        SimAction action;
        std::size_t worker_index = 0;
        std::size_t insertion_sequence = 0;
    };

    std::vector<std::vector<WorkerAction>> per_worker_actions_;
};

}  // namespace oht::sim_core
