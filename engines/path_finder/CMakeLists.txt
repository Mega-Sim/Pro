set(OHT_PATH_FINDER_SOURCES
    src/graph.cpp
    src/graph_loader.cpp
    src/route_planner.cpp
    src/compressed_graph.cpp)

add_library(oht_path_finder ${OHT_PATH_FINDER_SOURCES})
target_include_directories(oht_path_finder
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_compile_features(oht_path_finder PUBLIC cxx_std_17)
