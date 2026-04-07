set(OHT_TRAFFIC_MANAGER_SOURCES
    src/occupancy_map.cpp
    src/reservation_manager.cpp)

add_library(oht_traffic_manager ${OHT_TRAFFIC_MANAGER_SOURCES})
target_include_directories(oht_traffic_manager
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_compile_features(oht_traffic_manager PUBLIC cxx_std_17)
