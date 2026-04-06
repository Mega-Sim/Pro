#pragma once

#include <cstdint>
#include <optional>
#include <string>
#include <unordered_set>
#include <vector>

namespace oht::idle_control {

using VehicleId = std::string;
using NodeId = std::int64_t;

struct IdleVehicleState {
    VehicleId vehicle_id;
    NodeId current_node = 0;
    bool is_idle = false;
    bool has_job = false;
    bool is_loaded = false;
    double dwell_time_sec = 0.0;
    double last_idle_action_time_sec = 0.0;
    std::optional<NodeId> last_target_node;
    std::optional<std::string> current_zone;
    double blocking_score = 0.0;
    bool near_station = false;
    bool near_merge = false;
    bool on_main_flow = false;
};

struct IdleNodeInfo {
    NodeId node_id = 0;
    bool is_parking_node = false;
    bool is_standby_node = false;
    bool is_wait_node = false;
    bool is_protected = false;
    bool is_high_traffic = false;
    bool is_near_station = false;
    bool is_near_merge = false;
    bool is_on_main_flow = false;
    bool is_reserved = false;
    bool is_occupied = false;
    int capacity = 1;
    int used_count = 0;
    std::optional<std::string> zone_id;
};

struct IdleWorldSnapshot {
    std::vector<IdleVehicleState> vehicles;
    std::vector<IdleNodeInfo> candidate_nodes;
    std::unordered_set<NodeId> blocked_nodes;
    std::unordered_set<NodeId> reserved_nodes;
    std::unordered_set<NodeId> protected_nodes;
    double current_time_sec = 0.0;
};

struct IdleControlConfig {
    double min_idle_seconds_before_relocate = 30.0;
    double relocate_cooldown_sec = 15.0;

    double blocking_penalty_weight = 120.0;
    double traffic_penalty_weight = 30.0;
    double main_flow_penalty_weight = 40.0;
    double station_penalty_weight = 30.0;
    double merge_penalty_weight = 25.0;
    double distance_weight = 1.0;
    double spread_penalty_weight = 15.0;
    double parking_bonus_weight = 35.0;
    double standby_bonus_weight = 15.0;
    double hysteresis_bonus_weight = 10.0;
    double crowding_penalty_weight = 50.0;

    std::optional<double> max_candidate_distance;
    std::optional<int> max_vehicles_per_idle_node;
    double blocking_priority_threshold = 0.65;
};

enum class IdleActionType {
    HoldPosition,
    MoveToParking,
    MoveToStandby,
    RetreatFromBlocking,
    SpreadOut,
    RepositionForFlow,
};

struct IdleDecision {
    VehicleId vehicle_id;
    IdleActionType action_type = IdleActionType::HoldPosition;
    std::optional<NodeId> target_node;
    double score = 0.0;
    std::string reason;
};

}  // namespace oht::idle_control
