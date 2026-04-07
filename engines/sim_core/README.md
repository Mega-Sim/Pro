# sim_core (Phase 3 Skeleton)

`sim_core`는 OHT 시뮬레이터 런타임 골격을 위한 3차(Phase-3) 최소 연계 모듈입니다.

현재 포함 범위:
- 이벤트 타입/구조체 (`VehicleSpawn`, `JobReady`, `Tick`, `EnterResource`, `LeaveResource`)
- 시간 오름차순 이벤트 큐
- 월드 상태(`current_time_sec`, 차량 런타임 목록)
- 최소 시뮬레이터 루프(`schedule_event`, `step`, `run_until`)
- `traffic_manager`의 `OccupancyMap`, `ReservationManager`와 최소 연계
- 단일 리소스 기준 enter/leave 처리 (대기 상태 포함)

의도적으로 아직 미포함:
- 그래프 엣지 기반 이동 시간/이동 모델
- path planner 연계
- dispatch/task allocator 연계
- wait queue/retry 자동 재시도
- deadlock handling
- merge/intersection control
- metrics/telemetry

TODO(Phase-4):
- 이동 경로/엣지 시간 모델 연결
- 다중 리소스 체인 예약/해제
- wait queue 및 재시도 정책
- 혼잡(congestion) 인지 dispatch 정책 연계
- 그래프 블록 메타데이터 연계
- deadlock 감지/복구
