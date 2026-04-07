# sim_core (Phase 6 Minimal Route Execution)

`sim_core`는 OHT 시뮬레이터 런타임 골격 위에 6차(Phase-6) 최소 경로 실행 흐름을 추가한 모듈입니다.

현재 포함 범위:
- 이벤트 타입/구조체 (`VehicleSpawn`, `JobReady`, `Tick`, `EnterResource`, `LeaveResource`, `AdvanceRoute`)
- 시간 오름차순 이벤트 큐
- 월드 상태(`current_time_sec`, 차량 런타임 목록)
- 최소 시뮬레이터 루프(`schedule_event`, `step`, `run_until`)
- `traffic_manager`의 `OccupancyMap`, `ReservationManager`와 최소 연계
- 단일 리소스 기준 enter/leave 처리 (대기 상태 포함)
- `path_finder::LoadedGraph` 메타데이터 기반 directed edge(resource) 단위 route step 실행

의도적으로 아직 미포함:
- travel-time/dwell-time 이동 모델
- 연속 위치/속도 기반 이동
- dispatch/task allocator 연계
- job lifecycle 연계
- automatic rerouting
- wait queue/retry 자동 재시도
- deadlock detection/recovery
- merge/intersection scheduling control
- compacted operational graph 지원
- congestion-aware route cost/decision

TODO(Phase-7):
- 이동 시간/정차 시간 모델 연결
- 다중 리소스 체인 예약/해제
- wait queue 및 재시도 정책
- dispatch/task allocator 및 job lifecycle 연계
- deadlock 감지/복구
- merge/intersection 스케줄링 정책
- compacted graph 메타데이터 브리지
- 혼잡(congestion) 인지 경로 비용 모델
