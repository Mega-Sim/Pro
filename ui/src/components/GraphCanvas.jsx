import { useCallback, useEffect, useMemo } from 'react';
import {
  Background,
  Controls,
  MiniMap,
  ReactFlow,
  addEdge,
  applyEdgeChanges,
  applyNodeChanges,
  useReactFlow,
  MarkerType
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';

export default function GraphCanvas({ flowData, setFlowData }) {
  const { fitView } = useReactFlow();

  useEffect(() => {
    if (flowData.nodes.length) {
      fitView({ padding: 0.15, duration: 450 });
    }
  }, [fitView, flowData.nodes.length]);

  const styledEdges = useMemo(
    () =>
      flowData.edges.map((edge) => ({
        ...edge,
        markerEnd: { type: MarkerType.ArrowClosed, width: 24, height: 24 },
        style: { stroke: '#7c3aed', strokeWidth: 2.4 }
      })),
    [flowData.edges]
  );

  const onNodesChange = useCallback(
    (changes) => {
      setFlowData((current) => ({
        ...current,
        nodes: applyNodeChanges(changes, current.nodes)
      }));
    },
    [setFlowData]
  );

  const onEdgesChange = useCallback(
    (changes) => {
      setFlowData((current) => ({
        ...current,
        edges: applyEdgeChanges(changes, current.edges)
      }));
    },
    [setFlowData]
  );

  const onConnect = useCallback(
    (connection) => {
      setFlowData((current) => ({
        ...current,
        edges: addEdge({ ...connection, animated: false }, current.edges)
      }));
    },
    [setFlowData]
  );

  const onEdgeClick = useCallback(
    (_, clickedEdge) => {
      setFlowData((current) => ({
        ...current,
        edges: current.edges.map((edge) =>
          edge.id === clickedEdge.id
            ? {
                ...edge,
                source: edge.target,
                target: edge.source
              }
            : edge
        )
      }));
    },
    [setFlowData]
  );

  return (
    <main className="canvas glass">
      <ReactFlow
        nodes={flowData.nodes}
        edges={styledEdges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onEdgeClick={onEdgeClick}
        fitView
        selectionOnDrag
        panOnDrag={[1, 2]}
        multiSelectionKeyCode="Shift"
        defaultEdgeOptions={{
          markerEnd: { type: MarkerType.ArrowClosed }
        }}
      >
        <MiniMap pannable zoomable nodeStrokeWidth={3} />
        <Controls />
        <Background color="#5b21b6" gap={24} size={1.2} />
      </ReactFlow>
    </main>
  );
}
