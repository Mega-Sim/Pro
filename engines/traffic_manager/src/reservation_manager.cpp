#include "oht/traffic_manager/reservation_manager.hpp"

namespace oht::traffic_manager {

bool ReservationManager::try_reserve(ResourceId resource_id, VehicleId vehicle_id) {
    const auto it = reservations_.find(resource_id);
    if (it == reservations_.end()) {
        reservations_.emplace(resource_id, vehicle_id);
        return true;
    }

    return it->second == vehicle_id;
}

bool ReservationManager::release_reservation(ResourceId resource_id, VehicleId vehicle_id) {
    const auto it = reservations_.find(resource_id);
    if (it == reservations_.end() || it->second != vehicle_id) {
        return false;
    }

    reservations_.erase(it);
    return true;
}

bool ReservationManager::has_reservation(ResourceId resource_id) const {
    return reservations_.find(resource_id) != reservations_.end();
}

bool ReservationManager::is_reserved_by(ResourceId resource_id, VehicleId vehicle_id) const {
    const auto it = reservations_.find(resource_id);
    return it != reservations_.end() && it->second == vehicle_id;
}

std::optional<VehicleId> ReservationManager::reservation_owner(ResourceId resource_id) const {
    const auto it = reservations_.find(resource_id);
    if (it == reservations_.end()) {
        return std::nullopt;
    }

    return it->second;
}

bool ReservationManager::can_enter(
    ResourceId resource_id,
    VehicleId vehicle_id,
    const OccupancyMap& occupancy) const {
    if (occupancy.is_occupied_by(resource_id, vehicle_id)) {
        return true;
    }

    if (occupancy.is_occupied(resource_id) && !occupancy.is_occupied_by(resource_id, vehicle_id)) {
        return false;
    }

    if (is_reserved_by(resource_id, vehicle_id)) {
        return true;
    }

    return !has_reservation(resource_id);
}

std::size_t ReservationManager::size() const {
    return reservations_.size();
}

}  // namespace oht::traffic_manager
