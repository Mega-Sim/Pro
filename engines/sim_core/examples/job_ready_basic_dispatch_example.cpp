#include <cassert>
#include <iostream>

#include "oht/path_finder/graph.hpp"
#include "oht/path_finder/graph_loader.hpp"
#include "oht/sim_core/event.hpp"
#include "oht/sim_core/simulator.hpp"
#include "oht/task_allocator/job.hpp"

int main() {
    oht::path_finder::Graph graph;
    graph.add_node(1);
    graph.add_node(2);
    graph.add_node(3);
    graph.add_node(4);
    graph.add_node(5);

    graph.add_edge(1, 2, 1.0, 1.0);
    graph.add_edge(2, 3, 1.0, 1.0);
    graph.add_edge(3, 4, 1.0, 1.0);
    graph.add_edge(2, 1, 1.0, 1.0);
    graph.add_edge(3, 2, 1.0, 1.0);
    graph.add_edge(4, 3, 1.0, 1.0);
    // Node 5 is intentionally disconnected to emulate path-not-found.

    oht::path_finder::LoadedGraph loaded_graph;
    loaded_graph.graph = std::move(graph);

    oht::sim_core::Simulator simulator;
    assert(simulator.load_graph(loaded_graph));
    assert(simulator.spawn_vehicle(101, 1));
    assert(simulator.spawn_vehicle(202, 4));

    const oht::task_allocator::Job reachable_job{
        1001,
        2,
        4,
        0.0,
        10.0,
    };
    const oht::task_allocator::Job unreachable_job{
        1002,
        2,
        5,  // unreachable dropoff
        0.0,
        9.0,
    };

    assert(simulator.upsert_job(reachable_job));
    assert(simulator.upsert_job(unreachable_job));

    simulator.schedule_event(oht::sim_core::Event{
        0.0,
        oht::sim_core::EventType::JobReady,
        oht::sim_core::Event::kInvalidId,
        static_cast<int>(reachable_job.id),
        oht::sim_core::Event::kInvalidId,
    });
    simulator.schedule_event(oht::sim_core::Event{
        0.0,
        oht::sim_core::EventType::JobReady,
        oht::sim_core::Event::kInvalidId,
        static_cast<int>(unreachable_job.id),
        oht::sim_core::Event::kInvalidId,
    });

    simulator.run_until(0.0);

    const auto& vehicles = simulator.world().vehicles;
    std::size_t active_route_count = 0;
    for (const auto& vehicle : vehicles) {
        if (vehicle.has_active_route) {
            ++active_route_count;
        }
    }

    // Basic dispatch example expectation:
    // - reachable job is assigned once
    // - unreachable job is skipped safely
    assert(active_route_count == 1U);
    std::cout << "job_ready_basic_dispatch_example: PASS (active_routes=" << active_route_count << ")\n";
    return 0;
}
