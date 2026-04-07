import { useCallback, useMemo, useState } from 'react';
import GraphCanvas from './components/GraphCanvas';

const asFlowData = (raw) => {
  const nodes = (raw?.nodes ?? []).map((point, index) => ({
    id: String(index),
    position: { x: point[0], y: -point[1] },
    data: { label: `N${index}` },
    type: 'default'
  }));

  const edges = (raw?.edges ?? []).map((edge, index) => {
    const src = edge?.dir?.[0] ?? edge.start;
    const dst = edge?.dir?.[1] ?? edge.end;

    return {
      id: `e-${index}`,
      source: String(src),
      target: String(dst),
      data: {
        originalStart: edge.start,
        originalEnd: edge.end,
        index
      }
    };
  });

  return { nodes, edges };
};

export default function App() {
  const [graphName, setGraphName] = useState('graph_maker/graph.json');
  const [flowData, setFlowData] = useState({ nodes: [], edges: [] });

  const loadGraphFromUrl = useCallback(async () => {
    const response = await fetch('/graph.json');
    if (!response.ok) {
      throw new Error('graph.json을 읽지 못했습니다. 파일 업로드를 사용하세요.');
    }
    const json = await response.json();
    setFlowData(asFlowData(json));
    setGraphName('graph_maker/graph.json (동기화본)');
  }, []);

  const onFileChange = useCallback(async (event) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    const text = await file.text();
    const json = JSON.parse(text);
    setFlowData(asFlowData(json));
    setGraphName(file.name);
  }, []);

  const summary = useMemo(
    () => `노드 ${flowData.nodes.length}개 · 엣지 ${flowData.edges.length}개`,
    [flowData.edges.length, flowData.nodes.length]
  );

  return (
    <div className="app-shell">
      <header className="top-panel glass">
        <div>
          <h1>Graph Direction Studio</h1>
          <p>{graphName}</p>
        </div>
        <div className="actions">
          <button onClick={loadGraphFromUrl}>기본 그래프 불러오기</button>
          <label className="upload">
            JSON 업로드
            <input type="file" accept="application/json" onChange={onFileChange} />
          </label>
        </div>
      </header>

      <section className="hint-panel glass">
        <strong>{summary}</strong>
        <ul>
          <li>엣지를 클릭하면 화살표 방향이 즉시 반전됩니다.</li>
          <li>빈 공간 드래그로 다중 선택(박스 선택) 가능합니다.</li>
          <li>노드를 마우스로 드래그해 레이아웃을 재배치할 수 있습니다.</li>
        </ul>
      </section>

      <GraphCanvas flowData={flowData} setFlowData={setFlowData} />
    </div>
  );
}
