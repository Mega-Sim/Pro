# Engines Build Guide

`engines` 디렉토리는 `pkg_controller` 바깥으로 분리되어 독립적으로 빌드할 수 있습니다.

## 빠른 시작

```bash
cd engines/build
cmake ..
cmake --build .
```

위 명령으로 `path_finder`, `task_allocator`, `idle_control` 정적 라이브러리를 빌드합니다.

## path_finder graph loader (Phase-4 minimal bridge)

- `path_finder`는 이제 `graph_maker/graph.json` 형식(`nodes`, `edges`, `dir`)을 직접 로드할 수 있습니다.
- 로더는 현재 `Graph`와 함께 작은 `GraphMetadata` 브리지 계층만 제공합니다.
- 로드된 모든 **directed operational edge**에는 안정적인 순차 `resource_id`가 부여되며, `(from, to)` / `resource_id` 기반 조회가 가능합니다.
- 이 변경은 향후 `sim_core` / `traffic_manager` 연동을 위한 최소 브리지입니다.

미구현(Phase-5 이후):
- graph compaction
- station/parking/wait inference
- merge inference
- block segmentation beyond simple directed-edge resource ids
- zone generation
- congestion-aware cost model
