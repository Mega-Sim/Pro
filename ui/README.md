# Graph Direction Studio (ui)

`graph_maker/graph.json` 파일을 렌더링하고, 엣지를 클릭해서 방향을 뒤집을 수 있는 UI입니다.

## 실행

```bash
cd ui
npm install
npm run dev
```

- `npm run dev/build` 실행 시 `../graph_maker/graph.json`을 `ui/public/graph.json`으로 자동 복사합니다.
- 앱 상단의 **기본 그래프 불러오기** 버튼으로 동기화된 파일을 읽습니다.
- 또는 **JSON 업로드**로 다른 graph json 파일을 직접 불러올 수 있습니다.

## 조작

- **엣지 클릭**: 방향 반전
- **드래그 박스 선택**: 여러 노드/엣지 선택
- **노드 드래그**: 위치 이동
