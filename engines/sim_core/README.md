# sim_core (Phase 7B: Parallel Evaluate Skeleton)

`sim_core`는 OHT 시뮬레이터 런타임 골격 위에서 Phase-7A의 evaluate/apply 분리를 유지하면서,
Phase-7B에서 **멀티스레드 가능한 평가(runtime skeleton)** 를 최소 범위로 추가한 모듈입니다.

핵심 원칙:
- 시뮬레이터(`Simulator`)가 유일한 authoritative world owner
- worker는 `WorldSnapshot` 기반 읽기 전용 평가만 수행
- action은 `ActionBatch`로 모은 뒤 deterministic 규칙으로 병합
- apply/commit은 단일 스레드에서만 수행

현재 포함 범위:
- 이벤트 타입/구조체 (`VehicleSpawn`, `JobReady`, `Tick`, `EnterResource`, `LeaveResource`, `AdvanceRoute`)
- 시간 오름차순 이벤트 큐
- 월드 상태(`current_time_sec`, 차량 런타임 목록)
- 최소 시뮬레이터 루프(`schedule_event`, `step`, `run_until`)
- `traffic_manager`의 `OccupancyMap`, `ReservationManager`와 최소 연계
- 최소 action 모델(`SimAction`) 기반의 route evaluate -> apply 실행
- read-only `WorldSnapshot` 기반 active-route batch evaluate
- 고정 크기 `WorkerPool`(C++17 std::thread/condition_variable 기반)
- deterministic action merge (`vehicle_id`, `action_type`, worker-local insertion 순)
- 단일 스레드 apply (`apply_actions`)

현재 parallel scope:
- active route vehicle 평가 1회 배치(`evaluate_active_routes_parallel`, `advance_active_routes_parallel_once`)만 지원

아직 의도적으로 미포함:
- full task_allocator parallel integration
- full idle_control parallel integration
- rerouting
- deadlock recovery
- merge/intersection scheduling
- graph compaction
- travel-time/dwell-time model
- zone/partition sharding
- telemetry/metrics pipeline

Phase-8 TODO:
- task_allocator batch scoring on worker pool
- idle_control fleet decision batches on worker pool
- traffic_manager precheck actions
- graph static-context cleanup (dynamic state와 분리)
- zone/partition sharding
- metrics/telemetry hooks
- benchmark harness for 500 vehicles
