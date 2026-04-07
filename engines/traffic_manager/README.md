# traffic_manager (Phase-2 Skeleton)

`traffic_manager` is a minimal Phase-2 skeleton for traffic/resource control.

Current scope:
- Single-resource occupancy ownership (`OccupancyMap`)
- Single-resource reservation ownership (`ReservationManager`)
- Minimal deterministic entry check helper (`can_enter`)

Intentionally NOT implemented yet:
- Path-wide reservation
- Multi-resource or ordered corridor reservation
- Deadlock detection/recovery
- Merge/intersection scheduling
- Fairness policies
- Congestion metrics
- Coupling to graph/path_finder
- Coupling to sim_core vehicle movement

TODO (next phases):
- Multi-resource reservation APIs and conflict rules
- Ordered corridor reservation policies
- Deadlock detection/recovery strategies
- Merge token scheduling logic
- Integration with sim_core step/event processing
- Integration with graph block/resource metadata
