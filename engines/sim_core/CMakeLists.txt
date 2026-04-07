set(OHT_SIM_CORE_SOURCES
    src/simulator.cpp
    src/event_queue.cpp)

add_library(oht_sim_core ${OHT_SIM_CORE_SOURCES})
target_include_directories(oht_sim_core
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_link_libraries(oht_sim_core
    PUBLIC
        oht_path_finder
        oht_traffic_manager)
target_compile_features(oht_sim_core PUBLIC cxx_std_17)
