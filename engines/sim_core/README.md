# sim_core (Phase 7A: Evaluate -> Apply Split)

`sim_core`는 OHT 시뮬레이터 런타임 골격 위에서 Phase-7A 리팩터링을 통해
경로 실행을 **평가(evaluate)** 와 **적용(apply/commit)** 으로 분리한 모듈입니다.

현재 포함 범위:
- 이벤트 타입/구조체 (`VehicleSpawn`, `JobReady`, `Tick`, `EnterResource`, `LeaveResource`, `AdvanceRoute`)
- 시간 오름차순 이벤트 큐
- 월드 상태(`current_time_sec`, 차량 런타임 목록)
- 최소 시뮬레이터 루프(`schedule_event`, `step`, `run_until`)
- `traffic_manager`의 `OccupancyMap`, `ReservationManager`와 최소 연계
- 최소 action 모델(`SimAction`) 기반의 route evaluate -> apply 실행
- directed edge(resource) 기반 최소 route step 실행(결정/변이 분리)
- 현재 동작의 결정성(deterministic) 유지

Phase-7A에서 의도적으로 아직 미포함:
- worker pool
- parallel batch evaluation
- dispatch/task allocator 연계
- idle_control 연계
- deadlock handling/recovery
- automatic rerouting
- merge/intersection policies
- graph compaction
- travel-time/dwell-time 이동 모델

TODO(Phase-7B):
- worker pool 기반 병렬 평가 파이프라인
- snapshot 모델(읽기 전용 평가 상태)
- batch action 수집/적용 및 deterministic merge
- task_allocator 연계
- idle_control 연계
- static graph context / dynamic world state 분리
