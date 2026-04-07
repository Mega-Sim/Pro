#pragma once

namespace oht::sim_core {

enum class SimActionType {
    NoOp,
    RequestEnterResource,
    RequestLeaveResource,
    AdvanceRouteIndex,
    CompleteRoute,
};

struct SimAction {
    SimActionType type = SimActionType::NoOp;
    int vehicle_id = -1;
    int resource_id = -1;
    int next_node = -1;
};

}  // namespace oht::sim_core
