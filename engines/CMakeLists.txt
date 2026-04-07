cmake_minimum_required(VERSION 3.14)

if(CMAKE_SOURCE_DIR STREQUAL CMAKE_CURRENT_SOURCE_DIR)
    project(oht_engines LANGUAGES CXX)
endif()

add_subdirectory(path_finder)
add_subdirectory(task_allocator)
add_subdirectory(idle_control)
add_subdirectory(traffic_manager)
add_subdirectory(sim_core)
