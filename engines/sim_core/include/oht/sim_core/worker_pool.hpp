#pragma once

#include <atomic>
#include <condition_variable>
#include <cstddef>
#include <functional>
#include <mutex>
#include <thread>
#include <vector>

namespace oht::sim_core {

class WorkerPool {
public:
    explicit WorkerPool(std::size_t thread_count);
    ~WorkerPool();

    std::size_t thread_count() const;
    void parallel_for(
        std::size_t total,
        const std::function<void(std::size_t worker_index, std::size_t item_index)>& fn);

private:
    void worker_loop(std::size_t worker_index);
    void execute_task(std::size_t worker_index);

    std::size_t thread_count_ = 1;
    std::vector<std::thread> workers_;

    mutable std::mutex mutex_;
    std::condition_variable cv_task_;
    std::condition_variable cv_done_;

    bool stop_ = false;
    bool has_task_ = false;
    std::size_t generation_ = 0;

    std::size_t total_ = 0;
    std::function<void(std::size_t worker_index, std::size_t item_index)> fn_;
    std::atomic<std::size_t> next_index_{0};
    std::size_t completed_workers_ = 0;
};

}  // namespace oht::sim_core
