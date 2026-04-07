#pragma once

#include <cstddef>
#include <limits>
#include <vector>

#include "oht/task_allocator/job.hpp"

namespace oht::task_allocator {

class JobQueue {
public:
    void enqueue(const Job& job);
    void enqueue(std::vector<Job> jobs);

    [[nodiscard]] bool empty() const;
    [[nodiscard]] std::size_t size() const;

    std::vector<Job> list_ready(
        double now,
        std::size_t limit = std::numeric_limits<std::size_t>::max()) const;

    bool remove(JobId job_id);

private:
    std::vector<Job> pending_jobs_;
};

}  // namespace oht::task_allocator
