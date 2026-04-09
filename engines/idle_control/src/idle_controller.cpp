#include "oht/idle_control/idle_controller.hpp"

#include <algorithm>
#include <cmath>
#include <limits>
#include <sstream>
#include <unordered_map>

namespace oht::idle_control {

namespace {

struct CandidateEval {
    const IdleNodeInfo* node = nullptr;
    double score = std::numeric_limits<double>::infinity();
};

bool is_blocking_position(const IdleVehicleState& vehicle, double threshold) {
    return vehicle.blocking_score >= threshold ||
           vehicle.near_station ||
           vehicle.near_merge ||
           vehicle.on_main_flow;
}

bool can_use_node(const IdleNodeInfo& node, const IdleWorldSnapshot& snapshot) {
    if (node.is_protected || node.is_reserved || node.is_occupied) {
        return false;
    }
    if (snapshot.blocked_nodes.find(node.node_id) != snapshot.blocked_nodes.end()) {
        return false;
    }
    if (snapshot.reserved_nodes.find(node.node_id) != snapshot.reserved_nodes.end()) {
        return false;
    }
    if (snapshot.protected_nodes.find(node.node_id) != snapshot.protected_nodes.end()) {
        return false;
    }
    return true;
}

}  // namespace

struct IdleController::BatchContext {
    std::unordered_map<NodeId, int> reserved_counts;
};

IdleController::IdleController() = default;

IdleController::IdleController(IdleControlConfig config) : config_(std::move(config)) {}

void IdleController::set_config(const IdleControlConfig& config) {
    config_ = config;
}

const IdleControlConfig& IdleController::config() const {
    return config_;
}

void IdleController::set_travel_cost_fn(TravelCostFn fn) {
    travel_cost_fn_ = std::move(fn);
}

IdleDecision IdleController::decide_for_vehicle(
    const IdleVehicleState& vehicle,
    const IdleWorldSnapshot& snapshot) const {
    return decide_impl(vehicle, snapshot, nullptr);
}

std::vector<IdleDecision> IdleController::decide_for_fleet(
    const std::vector<IdleVehicleState>& vehicles,
    const IdleWorldSnapshot& snapshot) const {
    std::vector<const IdleVehicleState*> sorted;
    sorted.reserve(vehicles.size());
    for (const IdleVehicleState& vehicle : vehicles) {
        if (vehicle.is_idle && !vehicle.has_job && !vehicle.is_loaded) {
            sorted.push_back(&vehicle);
        }
    }

    std::sort(sorted.begin(), sorted.end(), [](const IdleVehicleState* lhs, const IdleVehicleState* rhs) {
        return lhs->vehicle_id < rhs->vehicle_id;
    });

    std::vector<IdleDecision> decisions;
    decisions.reserve(sorted.size());

    BatchContext batch;
    for (const IdleVehicleState* vehicle : sorted) {
        IdleDecision decision = decide_impl(*vehicle, snapshot, &batch);
        if (decision.target_node.has_value() &&
            decision.action_type != IdleActionType::HoldPosition) {
            ++batch.reserved_counts[*decision.target_node];
        }
        decisions.push_back(std::move(decision));
    }

    return decisions;
}

IdleDecision IdleController::decide_impl(
    const IdleVehicleState& vehicle,
    const IdleWorldSnapshot& snapshot,
    BatchContext* batch) const {
    IdleDecision hold;
    hold.vehicle_id = vehicle.vehicle_id;
    hold.action_type = IdleActionType::HoldPosition;

    if (!vehicle.is_idle || vehicle.has_job || vehicle.is_loaded) {
        hold.reason = "vehicle_not_eligible_for_idle_relocation";
        return hold;
    }

    const bool blocking_now = is_blocking_position(vehicle, config_.blocking_priority_threshold);
    const double elapsed_since_last_action = snapshot.current_time_sec - vehicle.last_idle_action_time_sec;
    const bool idle_too_long = vehicle.dwell_time_sec >= config_.min_idle_seconds_before_relocate;

    if (!blocking_now && elapsed_since_last_action < config_.relocate_cooldown_sec) {
        hold.reason = "relocate_cooldown_active";
        return hold;
    }

    if (!blocking_now && !idle_too_long) {
        hold.reason = "minimum_idle_dwell_not_met";
        return hold;
    }

    CandidateEval best;
    for (const IdleNodeInfo& node : snapshot.candidate_nodes) {
        if (!can_use_node(node, snapshot)) {
            continue;
        }

        const int batch_reserved = (batch != nullptr && batch->reserved_counts.count(node.node_id) > 0)
            ? batch->reserved_counts.at(node.node_id)
            : 0;

        const int hard_cap = config_.max_vehicles_per_idle_node.value_or(node.capacity);
        if (hard_cap > 0 && node.used_count + batch_reserved >= hard_cap) {
            continue;
        }

        const int node_capacity = (node.capacity > 0) ? node.capacity : 1;
        if (node.used_count + batch_reserved >= node_capacity) {
            continue;
        }

        const double distance = travel_cost(vehicle.current_node, node.node_id);
        if (!std::isfinite(distance)) {
            continue;
        }
        if (config_.max_candidate_distance.has_value() && distance > *config_.max_candidate_distance) {
            continue;
        }

        const double target_traffic_penalty = node.is_high_traffic ? 1.0 : 0.0;
        const double target_main_flow_penalty = node.is_on_main_flow ? 1.0 : 0.0;
        const double target_station_penalty = node.is_near_station ? 1.0 : 0.0;
        const double target_merge_penalty = node.is_near_merge ? 1.0 : 0.0;

        const double crowding = static_cast<double>(node.used_count + batch_reserved) / static_cast<double>(node_capacity);

        double spread_penalty = 0.0;
        if (vehicle.current_zone.has_value() && node.zone_id.has_value() &&
            *vehicle.current_zone == *node.zone_id) {
            spread_penalty = 0.5;
        }

        const double parking_bonus = node.is_parking_node ? 1.0 : 0.0;
        const double standby_bonus = (node.is_standby_node || node.is_wait_node) ? 1.0 : 0.0;
        const double hysteresis_bonus =
            (vehicle.last_target_node.has_value() && *vehicle.last_target_node == node.node_id) ? 1.0 : 0.0;

        const double current_blocking_penalty = blocking_now ? 1.0 + vehicle.blocking_score : vehicle.blocking_score;

        const double score =
            (config_.distance_weight * distance) +
            (config_.blocking_penalty_weight * current_blocking_penalty) +
            (config_.traffic_penalty_weight * target_traffic_penalty) +
            (config_.main_flow_penalty_weight * target_main_flow_penalty) +
            (config_.station_penalty_weight * target_station_penalty) +
            (config_.merge_penalty_weight * target_merge_penalty) +
            (config_.crowding_penalty_weight * crowding) +
            (config_.spread_penalty_weight * spread_penalty) -
            (config_.parking_bonus_weight * parking_bonus) -
            (config_.standby_bonus_weight * standby_bonus) -
            (config_.hysteresis_bonus_weight * hysteresis_bonus);

        if (score < best.score) {
            best.node = &node;
            best.score = score;
        }
    }

    if (best.node == nullptr || !std::isfinite(best.score)) {
        hold.reason = blocking_now
            ? "blocking_detected_but_no_safe_target"
            : "no_viable_idle_target";
        return hold;
    }

    IdleDecision decision;
    decision.vehicle_id = vehicle.vehicle_id;
    decision.target_node = best.node->node_id;
    decision.score = best.score;

    std::ostringstream reason_builder;
    bool has_reason = false;
    if (vehicle.blocking_score >= config_.blocking_priority_threshold) {
        reason_builder << "high_blocking_score";
        has_reason = true;
    }
    if (vehicle.near_station) {
        reason_builder << (has_reason ? "+" : "") << "near_station";
        has_reason = true;
    }
    if (vehicle.near_merge) {
        reason_builder << (has_reason ? "+" : "") << "near_merge";
        has_reason = true;
    }
    if (vehicle.on_main_flow) {
        reason_builder << (has_reason ? "+" : "") << "on_main_flow";
        has_reason = true;
    }
    if (idle_too_long) {
        reason_builder << (has_reason ? "+" : "") << "idle_too_long";
        has_reason = true;
    }

    if (blocking_now) {
        decision.action_type = IdleActionType::RetreatFromBlocking;
        decision.reason = has_reason ? reason_builder.str() : "blocking_position_escape_requested";
        return decision;
    }

    if (best.node->is_parking_node) {
        decision.action_type = IdleActionType::MoveToParking;
        decision.reason = has_reason ? reason_builder.str() : "parking_node_preferred";
        return decision;
    }

    if (best.node->is_standby_node || best.node->is_wait_node) {
        decision.action_type = IdleActionType::MoveToStandby;
        decision.reason = has_reason ? reason_builder.str() : "standby_or_wait_fallback";
        return decision;
    }

    decision.action_type = IdleActionType::HoldPosition;
    decision.target_node.reset();
    decision.reason = "no_supported_idle_action_for_selected_target";
    return decision;
}

double IdleController::travel_cost(NodeId from, NodeId to) const {
    if (from == to) {
        return 0.0;
    }

    if (travel_cost_fn_) {
        return travel_cost_fn_(from, to);
    }

    return std::abs(static_cast<double>(from - to));
}

}  // namespace oht::idle_control
