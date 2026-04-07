#include "oht/traffic_manager/occupancy_map.hpp"

namespace oht::traffic_manager {

bool OccupancyMap::occupy(ResourceId resource_id, VehicleId vehicle_id) {
    const auto it = owners_.find(resource_id);
    if (it == owners_.end()) {
        owners_.emplace(resource_id, vehicle_id);
        return true;
    }

    return it->second == vehicle_id;
}

bool OccupancyMap::release(ResourceId resource_id, VehicleId vehicle_id) {
    const auto it = owners_.find(resource_id);
    if (it == owners_.end() || it->second != vehicle_id) {
        return false;
    }

    owners_.erase(it);
    return true;
}

bool OccupancyMap::is_occupied(ResourceId resource_id) const {
    return owners_.find(resource_id) != owners_.end();
}

bool OccupancyMap::is_occupied_by(ResourceId resource_id, VehicleId vehicle_id) const {
    const auto it = owners_.find(resource_id);
    return it != owners_.end() && it->second == vehicle_id;
}

std::optional<VehicleId> OccupancyMap::occupant(ResourceId resource_id) const {
    const auto it = owners_.find(resource_id);
    if (it == owners_.end()) {
        return std::nullopt;
    }

    return it->second;
}

std::size_t OccupancyMap::size() const {
    return owners_.size();
}

}  // namespace oht::traffic_manager
