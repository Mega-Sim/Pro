#pragma once

#include <cstddef>
#include <optional>
#include <unordered_map>

#include "oht/traffic_manager/types.hpp"

namespace oht::traffic_manager {

class OccupancyMap {
public:
    // Idempotent: occupying an already-owned resource by the same vehicle returns true.
    bool occupy(ResourceId resource_id, VehicleId vehicle_id);
    bool release(ResourceId resource_id, VehicleId vehicle_id);

    bool is_occupied(ResourceId resource_id) const;
    bool is_occupied_by(ResourceId resource_id, VehicleId vehicle_id) const;
    std::optional<VehicleId> occupant(ResourceId resource_id) const;

    std::size_t size() const;

private:
    std::unordered_map<ResourceId, VehicleId> owners_;
};

}  // namespace oht::traffic_manager
