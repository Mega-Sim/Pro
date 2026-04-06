#include <iostream>
#include <vector>

#include "oht/path_finder/graph.hpp"
#include "oht/path_finder/route_planner.hpp"
#include "oht/task_allocator/task_allocator.hpp"

int main() {
    using oht::path_finder::Graph;
    using oht::path_finder::RoutePlanner;
    using oht::task_allocator::Job;
    using oht::task_allocator::TaskAllocator;
    using oht::task_allocator::VehicleState;

    Graph graph;
    graph.add_node(1, 0.0, 0.0);
    graph.add_node(2, 1.0, 0.0);
    graph.add_node(3, 2.0, 0.0);
    graph.add_node(4, 1.0, 1.0);

    graph.add_edge(1, 2, 1.0, 1.0);
    graph.add_edge(2, 1, 1.0, 1.0);
    graph.add_edge(2, 3, 1.0, 1.0);
    graph.add_edge(3, 2, 1.0, 1.0);
    graph.add_edge(2, 4, 1.0, 1.2);
    graph.add_edge(4, 3, 1.5, 1.5);

    RoutePlanner route_planner(graph);
    const auto path = route_planner.find_shortest_path(1, 3);

    std::cout << "Route found: " << (path.found ? "yes" : "no") << '\n';
    if (path.found) {
        std::cout << "Path: ";
        for (const auto node_id : path.path) {
            std::cout << node_id << ' ';
        }
        std::cout << "\nTotal cost: " << path.total_cost << "\n\n";
    }

    const std::vector<VehicleState> vehicles = {
        {101, 1, 0.0},
        {102, 4, 0.5},
        {103, 2, 0.0},
    };

    const std::vector<Job> jobs = {
        {201, 2, 3, 0.0, 1.0},
        {202, 1, 4, 0.0, 2.0},
        {203, 4, 3, 1.0, 0.0},
    };

    TaskAllocator allocator;
    allocator.set_route_planner(&route_planner);

    const auto assignments = allocator.assign(vehicles, jobs);

    std::cout << "Assignments:\n";
    for (const auto& assignment : assignments) {
        std::cout << "  vehicle=" << assignment.vehicle_id
                  << " job=" << assignment.job_id
                  << " score=" << assignment.score
                  << " deadhead=" << assignment.deadhead_distance
                  << " loaded=" << assignment.loaded_distance << '\n';
    }

    return 0;
}
