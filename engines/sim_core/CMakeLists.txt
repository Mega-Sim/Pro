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
        oht_idle_control
        oht_path_finder
        oht_task_allocator
        oht_traffic_manager
    PRIVATE
        Threads::Threads)
target_compile_features(oht_sim_core PUBLIC cxx_std_17)

add_executable(oht_sim_core_job_ready_example
    examples/job_ready_basic_dispatch_example.cpp)
target_link_libraries(oht_sim_core_job_ready_example
    PRIVATE
        oht_sim_core)
target_compile_features(oht_sim_core_job_ready_example PRIVATE cxx_std_17)

add_executable(oht_sim_core_tick_idle_relocation_example
    examples/tick_idle_relocation_example.cpp)
target_link_libraries(oht_sim_core_tick_idle_relocation_example
    PRIVATE
        oht_sim_core)
target_compile_features(oht_sim_core_tick_idle_relocation_example PRIVATE cxx_std_17)

add_executable(oht_sim_core_initial_engine_smoke_example
    examples/initial_engine_headless_smoke_example.cpp)
target_link_libraries(oht_sim_core_initial_engine_smoke_example
    PRIVATE
        oht_sim_core)
target_compile_features(oht_sim_core_initial_engine_smoke_example PRIVATE cxx_std_17)
