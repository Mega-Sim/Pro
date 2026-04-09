#include <algorithm>
#include <cassert>
#include <iostream>

#include "oht/path_finder/graph.hpp"
#include "oht/path_finder/graph_loader.hpp"
#include "oht/path_finder/graph_metadata.hpp"
#include "oht/sim_core/event.hpp"
#include "oht/sim_core/simulator.hpp"

int main() {
    oht::path_finder::Graph graph;
    graph.add_node(1);
    graph.add_node(2);
    graph.add_node(3);
    graph.add_edge(1, 2, 1.0, 1.0);
    graph.add_edge(2, 3, 1.0, 1.0);
    graph.add_edge(2, 1, 1.0, 1.0);
    graph.add_edge(3, 2, 1.0, 1.0);

    oht::path_finder::LoadedGraph loaded_graph;
    loaded_graph.graph = std::move(graph);
    loaded_graph.metadata.nodes = {
        oht::path_finder::NodeMetadata{1, oht::path_finder::NodeKind::StationCandidate},
        oht::path_finder::NodeMetadata{2, oht::path_finder::NodeKind::ParkingCandidate},
        oht::path_finder::NodeMetadata{3, oht::path_finder::NodeKind::WaitCandidate},
    };
    loaded_graph.metadata.edges = {
        oht::path_finder::EdgeMetadata{0, 1, 2, oht::path_finder::EdgeKind::MainFlowCandidate},
        oht::path_finder::EdgeMetadata{1, 2, 3, oht::path_finder::EdgeKind::Normal},
        oht::path_finder::EdgeMetadata{2, 2, 1, oht::path_finder::EdgeKind::MainFlowCandidate},
        oht::path_finder::EdgeMetadata{3, 3, 2, oht::path_finder::EdgeKind::Normal},
    };
    loaded_graph.metadata.resource_to_edge_index = {
        {0, 0},
        {1, 1},
        {2, 2},
        {3, 3},
    };

    oht::sim_core::Simulator simulator;
    assert(simulator.load_graph(loaded_graph));
    assert(simulator.spawn_vehicle(101, 1));  // blocking-prone node

    simulator.schedule_event(oht::sim_core::Event{
        40.0,
        oht::sim_core::EventType::Tick,
        oht::sim_core::Event::kInvalidId,
        oht::sim_core::Event::kInvalidId,
        oht::sim_core::Event::kInvalidId,
    });
    simulator.run_until(40.0);

    const auto& vehicles = simulator.world().vehicles;
    const auto find_vehicle = [&vehicles](int vehicle_id) {
        const auto it = std::find_if(
            vehicles.begin(),
            vehicles.end(),
            [vehicle_id](const oht::sim_core::VehicleRuntime& vehicle) {
                return vehicle.vehicle_id == vehicle_id;
            });
        return (it == vehicles.end()) ? nullptr : &(*it);
    };
    const auto* vehicle_101 = find_vehicle(101);
    assert(vehicle_101 != nullptr);

    // Tick idle relocation expectation:
    // - v101 is near station/main flow and gets relocation route.
    assert(vehicle_101->has_active_route);
    assert(vehicle_101->current_node == 1 || vehicle_101->current_node == 2);

    std::cout << "tick_idle_relocation_example: PASS\n";
    return 0;
}
