#pragma once

#include <cstddef>
#include <optional>
#include <unordered_map>

#include "oht/traffic_manager/occupancy_map.hpp"
#include "oht/traffic_manager/types.hpp"

namespace oht::traffic_manager {

class ReservationManager {
public:
    // Idempotent: reserving an already-owned resource by the same vehicle returns true.
    bool try_reserve(ResourceId resource_id, VehicleId vehicle_id);
    bool release_reservation(ResourceId resource_id, VehicleId vehicle_id);

    bool has_reservation(ResourceId resource_id) const;
    bool is_reserved_by(ResourceId resource_id, VehicleId vehicle_id) const;
    std::optional<VehicleId> reservation_owner(ResourceId resource_id) const;

    bool can_enter(ResourceId resource_id, VehicleId vehicle_id, const OccupancyMap& occupancy) const;

    std::size_t size() const;

private:
    std::unordered_map<ResourceId, VehicleId> reservations_;

    // TODO(phase-3): Expand to multi-resource/path-wide reservation state.
};

}  // namespace oht::traffic_manager
