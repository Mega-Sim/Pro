#pragma once

#include <functional>
#include <vector>

#include "oht/idle_control/idle_types.hpp"

namespace oht::idle_control {

class IdleController {
public:
    using TravelCostFn = std::function<double(NodeId, NodeId)>;

    IdleController();
    explicit IdleController(IdleControlConfig config);

    void set_config(const IdleControlConfig& config);
    const IdleControlConfig& config() const;

    void set_travel_cost_fn(TravelCostFn fn);

    IdleDecision decide_for_vehicle(
        const IdleVehicleState& vehicle,
        const IdleWorldSnapshot& snapshot) const;

    std::vector<IdleDecision> decide_for_fleet(
        const std::vector<IdleVehicleState>& vehicles,
        const IdleWorldSnapshot& snapshot) const;

private:
    struct BatchContext;

    IdleDecision decide_impl(
        const IdleVehicleState& vehicle,
        const IdleWorldSnapshot& snapshot,
        BatchContext* batch) const;

    double travel_cost(NodeId from, NodeId to) const;

    IdleControlConfig config_;
    TravelCostFn travel_cost_fn_;
};

}  // namespace oht::idle_control
