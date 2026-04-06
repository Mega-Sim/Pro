set(OHT_IDLE_CONTROL_SOURCES
    src/idle_controller.cpp)

add_library(oht_idle_control ${OHT_IDLE_CONTROL_SOURCES})
target_include_directories(oht_idle_control
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include)
target_compile_features(oht_idle_control PUBLIC cxx_std_17)
