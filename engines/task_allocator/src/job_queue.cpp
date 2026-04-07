#include "oht/task_allocator/job_queue.hpp"

#include <algorithm>

namespace oht::task_allocator {

namespace {

bool should_order_before(const Job& lhs, const Job& rhs) {
    if (lhs.priority != rhs.priority) {
        return lhs.priority > rhs.priority;
    }

    if (lhs.ready_time != rhs.ready_time) {
        return lhs.ready_time < rhs.ready_time;
    }

    return lhs.id < rhs.id;
}

}  // namespace

void JobQueue::enqueue(const Job& job) {
    pending_jobs_.push_back(job);
}

void JobQueue::enqueue(std::vector<Job> jobs) {
    pending_jobs_.insert(pending_jobs_.end(), jobs.begin(), jobs.end());
}

bool JobQueue::empty() const {
    return pending_jobs_.empty();
}

std::size_t JobQueue::size() const {
    return pending_jobs_.size();
}

std::vector<Job> JobQueue::list_ready(double now, std::size_t limit) const {
    std::vector<Job> ready_jobs;
    ready_jobs.reserve(std::min(limit, pending_jobs_.size()));

    for (const Job& job : pending_jobs_) {
        if (job.ready_time > now) {
            continue;
        }

        ready_jobs.push_back(job);
    }

    std::stable_sort(ready_jobs.begin(), ready_jobs.end(), should_order_before);

    if (ready_jobs.size() > limit) {
        ready_jobs.resize(limit);
    }

    return ready_jobs;
}

bool JobQueue::remove(JobId job_id) {
    const auto it = std::find_if(
        pending_jobs_.begin(), pending_jobs_.end(), [job_id](const Job& job) { return job.id == job_id; });

    if (it == pending_jobs_.end()) {
        return false;
    }

    pending_jobs_.erase(it);
    return true;
}

}  // namespace oht::task_allocator
