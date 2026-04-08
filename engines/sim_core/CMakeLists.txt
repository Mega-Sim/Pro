set(OHT_SIM_CORE_SOURCES
    src/action_batch.cpp
    src/graph_context.cpp
    src/event_queue.cpp
    src/simulator.cpp
    src/worker_pool.cpp)

add_library(oht_sim_core ${OHT_SIM_CORE_SOURCES})
target_include_directories(oht_sim_core
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include)

find_package(Threads REQUIRED)

target_link_libraries(oht_sim_core
    PUBLIC
        oht_path_finder
        oht_traffic_manager
    PRIVATE
        Threads::Threads)
target_compile_features(oht_sim_core PUBLIC cxx_std_17)
