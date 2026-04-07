set(OHT_TASK_ALLOCATOR_SOURCES
    src/task_allocator.cpp
    src/job_queue.cpp)

add_library(oht_task_allocator ${OHT_TASK_ALLOCATOR_SOURCES})
target_include_directories(oht_task_allocator
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_CURRENT_SOURCE_DIR}/../path_finder/include)
target_link_libraries(oht_task_allocator PUBLIC oht_path_finder)
target_compile_features(oht_task_allocator PUBLIC cxx_std_17)
