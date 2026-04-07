# sim_core (Phase 1 Skeleton)

`sim_core`는 OHT 시뮬레이터 런타임 골격을 위한 1차(Phase-1) 모듈입니다.

현재 포함 범위:
- 이벤트 타입/구조체 (`VehicleSpawn`, `JobReady`, `Tick`)
- 시간 오름차순 이벤트 큐
- 월드 상태(`current_time_sec`, 차량 런타임 목록)
- 최소 시뮬레이터 루프(`schedule_event`, `step`, `run_until`)

의도적으로 아직 미포함:
- 실제 라우팅/할당/예약 기반 차량 주행
- 교착(deadlock) 처리
- 시나리오 로더/대규모 실행 파이프라인

TODO(Phase-2):
- task_allocator 연계
- idle_control 연계
- graph loader / graph compaction 연계
- reservation / occupancy / deadlock handling
