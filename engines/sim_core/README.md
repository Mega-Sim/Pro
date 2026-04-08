# sim_core (Phase 7B: Parallel Evaluate Skeleton + Boundary Hardening)

`sim_core`는 OHT 시뮬레이터 런타임 골격 위에서 Phase-7A의 evaluate/apply 분리를 유지하면서,
Phase-7B의 **멀티스레드 evaluate skeleton** 에 더해 **snapshot 경계 하드닝** 을 반영한 모듈입니다.

핵심 원칙:
- `Simulator`가 authoritative world owner
- `WorldState`는 동적 시뮬레이션 상태만 보유
- static graph lookup은 immutable `GraphContext`로 분리
- worker evaluate는 `WorldSnapshot + GraphContext` 읽기만 수행
- action은 `ActionBatch`로 수집 후 deterministic 규칙으로 병합
- apply/commit은 단일 스레드에서만 수행

현재 포함 범위:
- 이벤트 타입/구조체 (`VehicleSpawn`, `JobReady`, `Tick`, `EnterResource`, `LeaveResource`, `AdvanceRoute`)
- 시간 오름차순 이벤트 큐
- 월드 상태(`current_time_sec`, 차량 런타임 목록)
- 최소 시뮬레이터 루프(`schedule_event`, `step`, `run_until`)
- `traffic_manager`의 `OccupancyMap`, `ReservationManager`와 최소 연계
- 최소 action 모델(`SimAction`) 기반 route evaluate -> apply 실행
- `WorldSnapshot` 기반 active-route batch evaluate
- immutable `GraphContext` 기반 edge->resource lookup
- 고정 크기 `WorkerPool`(C++17 std::thread/condition_variable 기반)
- deterministic action merge (`vehicle_id`, `action_type`, worker-local insertion 순)
- 단일 스레드 apply (`apply_actions`)

현재 parallel scope:
- active route vehicle 평가 1회 배치(`evaluate_active_routes_parallel`, `advance_active_routes_parallel_once`)만 지원

이번 하드닝(Phase-7B cleanup)의 의미:
- public main에 이미 있던 Phase-7B skeleton을 유지
- `WorldState`에서 static graph ownership 제거
- worker-side route evaluate가 live mutable world 접근 없이 동작
- evaluate 단계는 snapshot + immutable graph context만 사용
- authoritative mutation은 apply/commit 단계에만 유지

아직 의도적으로 미포함:
- task_allocator parallel scoring/integration
- idle_control parallel scoring/integration
- rerouting
- deadlock recovery
- merge/intersection scheduling
- graph compaction
- travel-time/dwell-time model
- zone/partition sharding
- telemetry/metrics pipeline

Phase-8 TODO:
- malformed-route 진단 정보(action batch) 확장
- cross-resource transition 결정 정책 정교화
- task_allocator batch scoring on worker pool
- idle_control fleet decision batches on worker pool
- traffic_manager precheck actions
- zone/partition sharding
- metrics/telemetry hooks
- benchmark harness for 500 vehicles
