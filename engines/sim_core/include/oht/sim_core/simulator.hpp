#pragma once

#include <memory>
#include <unordered_map>
#include <vector>

#include "oht/idle_control/idle_controller.hpp"
#include "oht/path_finder/graph.hpp"
#include "oht/path_finder/graph_loader.hpp"
#include "oht/path_finder/route_planner.hpp"
#include "oht/sim_core/graph_context.hpp"
#include "oht/sim_core/worker_pool.hpp"
#include "oht/sim_core/event.hpp"
#include "oht/sim_core/event_queue.hpp"
#include "oht/sim_core/sim_action.hpp"
#include "oht/sim_core/world_snapshot.hpp"
#include "oht/sim_core/world_state.hpp"
#include "oht/sim_core/vehicle_runtime.hpp"
#include "oht/task_allocator/job.hpp"
#include "oht/task_allocator/job_queue.hpp"
#include "oht/task_allocator/task_allocator.hpp"
#include "oht/traffic_manager/occupancy_map.hpp"
#include "oht/traffic_manager/reservation_manager.hpp"

namespace oht::sim_core {

class WorkerPool;

class Simulator {
public:
    ~Simulator();

    bool load_graph(const oht::path_finder::LoadedGraph& loaded_graph);
    bool spawn_vehicle(int vehicle_id, int start_node);
    bool set_vehicle_route(int vehicle_id, const std::vector<oht::path_finder::NodeId>& route_nodes);
    bool schedule_advance_route(double time_sec, int vehicle_id);
    bool upsert_job(const oht::task_allocator::Job& job);

    void schedule_event(const Event& event);
    bool step();
    void run_until(double end_time_sec);

    bool try_reserve_resource(int resource_id, int vehicle_id);
    bool release_reservation(int resource_id, int vehicle_id);

    void set_worker_count(std::size_t worker_count);
    std::size_t worker_count() const;

    WorldSnapshot make_snapshot() const;
    std::vector<SimAction> evaluate_active_routes_parallel() const;
    void apply_actions(const std::vector<SimAction>& actions);
    void advance_active_routes_parallel_once();

    const WorldState& world() const;
    const oht::traffic_manager::OccupancyMap& occupancy() const;
    const oht::traffic_manager::ReservationManager& reservations() const;

private:
    void process_event(const Event& event);
    void process_job_ready_event(const Event& event);
    void process_tick_event();

    const VehicleRuntime* find_vehicle_const(int vehicle_id) const;
    VehicleRuntime* find_vehicle_mutable(int vehicle_id);

    SimAction evaluate_advance_route(const VehicleRuntime& vehicle) const;
    SimAction evaluate_advance_route_snapshot(
        const WorldSnapshot::VehicleSnapshot& vehicle,
        const GraphContext& graph_context) const;

    void apply_action(const SimAction& action);
    void apply_enter_resource(VehicleRuntime& vehicle, int resource_id);
    void apply_leave_resource(VehicleRuntime& vehicle, int resource_id, bool advance_route);
    void apply_complete_route(VehicleRuntime& vehicle);

    EventQueue event_queue_;
    WorldState world_;
    std::shared_ptr<const GraphContext> graph_context_;
    oht::traffic_manager::OccupancyMap occupancy_;
    oht::traffic_manager::ReservationManager reservations_;
    std::unique_ptr<oht::path_finder::RoutePlanner> route_planner_;
    oht::task_allocator::TaskAllocator task_allocator_;
    oht::task_allocator::JobQueue job_queue_;
    std::unordered_map<int, oht::task_allocator::Job> jobs_by_id_;
    oht::idle_control::IdleController idle_controller_;
    std::unordered_map<int, double> last_idle_action_time_sec_by_vehicle_;
    std::unordered_map<int, oht::path_finder::NodeId> last_idle_target_by_vehicle_;
    std::size_t worker_count_ = 1;
    mutable std::unique_ptr<WorkerPool> worker_pool_;
};

}  // namespace oht::sim_core
