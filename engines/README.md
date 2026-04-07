# Engines Build Guide

`engines` 디렉토리는 `pkg_controller` 바깥으로 분리되어 독립적으로 빌드할 수 있습니다.

## 빠른 시작

```bash
cd engines/build
cmake ..
cmake --build .
```

위 명령으로 `path_finder`, `task_allocator`, `idle_control` 정적 라이브러리를 빌드합니다.
