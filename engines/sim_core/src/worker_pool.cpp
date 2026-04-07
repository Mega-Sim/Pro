#include "oht/sim_core/worker_pool.hpp"

#include <algorithm>

namespace oht::sim_core {

WorkerPool::WorkerPool(std::size_t thread_count)
    : thread_count_(std::max<std::size_t>(1, thread_count)) {
    if (thread_count_ <= 1) {
        return;
    }

    workers_.reserve(thread_count_ - 1);
    for (std::size_t worker_index = 1; worker_index < thread_count_; ++worker_index) {
        workers_.emplace_back([this, worker_index]() { worker_loop(worker_index); });
    }
}

WorkerPool::~WorkerPool() {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        stop_ = true;
    }
    cv_task_.notify_all();

    for (std::thread& worker : workers_) {
        if (worker.joinable()) {
            worker.join();
        }
    }
}

std::size_t WorkerPool::thread_count() const {
    return thread_count_;
}

void WorkerPool::parallel_for(
    std::size_t total,
    const std::function<void(std::size_t worker_index, std::size_t item_index)>& fn) {
    if (total == 0) {
        return;
    }

    if (thread_count_ <= 1 || workers_.empty()) {
        for (std::size_t item_index = 0; item_index < total; ++item_index) {
            fn(0, item_index);
        }
        return;
    }

    {
        std::lock_guard<std::mutex> lock(mutex_);
        total_ = total;
        fn_ = fn;
        next_index_.store(0);
        completed_workers_ = 0;
        has_task_ = true;
        ++generation_;
    }

    cv_task_.notify_all();

    execute_task(0);

    std::unique_lock<std::mutex> lock(mutex_);
    cv_done_.wait(lock, [this]() { return completed_workers_ == thread_count_; });

    has_task_ = false;
    fn_ = nullptr;
    total_ = 0;
}

void WorkerPool::worker_loop(std::size_t worker_index) {
    std::size_t observed_generation = 0;

    while (true) {
        {
            std::unique_lock<std::mutex> lock(mutex_);
            cv_task_.wait(lock, [this, observed_generation]() {
                return stop_ || (has_task_ && generation_ != observed_generation);
            });

            if (stop_) {
                return;
            }

            observed_generation = generation_;
        }

        execute_task(worker_index);
    }
}

void WorkerPool::execute_task(std::size_t worker_index) {
    const std::function<void(std::size_t worker_index, std::size_t item_index)> local_fn = fn_;

    while (true) {
        const std::size_t item_index = next_index_.fetch_add(1);
        if (item_index >= total_) {
            break;
        }

        local_fn(worker_index, item_index);
    }

    std::lock_guard<std::mutex> lock(mutex_);
    ++completed_workers_;
    if (completed_workers_ == thread_count_) {
        cv_done_.notify_one();
    }
}

}  // namespace oht::sim_core
