set(OHT_PATH_FINDER_SOURCES
    src/graph.cpp
    src/route_planner.cpp)

add_library(oht_path_finder ${OHT_PATH_FINDER_SOURCES})
target_include_directories(oht_path_finder
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_compile_features(oht_path_finder PUBLIC cxx_std_17)
