import ezdxf
import math
from collections import defaultdict, deque

# -----------------------------
# 벡터 유틸
# -----------------------------
def vec(p1, p2):
    return (p2[0] - p1[0], p2[1] - p1[1])

def sign(v):
    if v > 0: return 1
    if v < 0: return -1
    return 0

def length(v):
    return math.hypot(v[0], v[1])

def normalize(v):
    l = length(v)
    if l == 0:
        return (0, 0)
    return (v[0]/l, v[1]/l)

def dot(v1, v2):
    return v1[0]*v2[0] + v1[1]*v2[1]

# -----------------------------
# DXF 읽기
# -----------------------------
def load_dxf_lines(filename):
    doc = ezdxf.readfile(filename)
    msp = doc.modelspace()

    segments = []

    for e in msp:
        if e.dxftype() == 'LINE':
            start = (e.dxf.start.x, e.dxf.start.y)
            end = (e.dxf.end.x, e.dxf.end.y)
            segments.append((start, end))

        elif e.dxftype() == 'ARC':
            center = (e.dxf.center.x, e.dxf.center.y)
            radius = e.dxf.radius
            start_angle = math.radians(e.dxf.start_angle)
            end_angle = math.radians(e.dxf.end_angle)

            steps = 10
            pts = []
            for i in range(steps + 1):
                t = start_angle + (end_angle - start_angle) * i / steps
                x = center[0] + radius * math.cos(t)
                y = center[1] + radius * math.sin(t)
                pts.append((x, y))

            for i in range(len(pts)-1):
                segments.append((pts[i], pts[i+1]))

    return segments

# -----------------------------
# 노드 생성
# -----------------------------
def build_nodes_edges(segments):
    node_map = {}
    nodes = []
    edges = []

    def get_node(p):
        key = (round(p[0], 3), round(p[1], 3))
        if key not in node_map:
            node_map[key] = len(nodes)
            nodes.append(key)
        return node_map[key]

    for s, e in segments:
        n1 = get_node(s)
        n2 = get_node(e)
        edges.append({
            "start": n1,
            "end": n2,
            "dir": None
        })

    return nodes, edges

# -----------------------------
# 방향 생성 (Thread 방식)
# -----------------------------
def build_directed_graph(nodes, edges):

    node_edges = defaultdict(list)
    for i, e in enumerate(edges):
        node_edges[e["start"]].append(i)
        node_edges[e["end"]].append(i)

    # seed 간선 하나 선택
    seed = 0
    e = edges[seed]
    e["dir"] = (e["start"], e["end"])

    q = deque()
    q.append(seed)

    while q:
        eid = q.popleft()
        e = edges[eid]

        start, end = e["dir"]
        prev_vec = vec(nodes[start], nodes[end])

        # end node에서만 탐색
        next_edges = node_edges[end]

        candidates = []
        for nid in next_edges:
            if edges[nid]["dir"] is None:
                candidates.append(nid)

        if not candidates:
            continue

        # 가장 자연스럽게 이어지는 간선 찾기
        best = None
        best_score = -999

        for nid in candidates:
            e2 = edges[nid]
            if e2["start"] == end:
                v = vec(nodes[e2["start"]], nodes[e2["end"]])
                dir_pair = (e2["start"], e2["end"])
            else:
                v = vec(nodes[e2["end"]], nodes[e2["start"]])
                dir_pair = (e2["end"], e2["start"])

            score = dot(normalize(prev_vec), normalize(v))
            if score > best_score:
                best_score = score
                best = (nid, dir_pair, v)

        # 기존 스레드 진행
        nid, dir_pair, v = best
        edges[nid]["dir"] = dir_pair
        q.append(nid)

        # 나머지는 분기 스레드 후보
        for nid2 in candidates:
            if nid2 == nid:
                continue

            e2 = edges[nid2]

            if e2["start"] == end:
                v2 = vec(nodes[e2["start"]], nodes[e2["end"]])
                dir_pair2 = (e2["start"], e2["end"])
            else:
                v2 = vec(nodes[e2["end"]], nodes[e2["start"]])
                dir_pair2 = (e2["end"], e2["start"])

            # ----- 부호 규칙 -----
            if sign(prev_vec[0]) == sign(v2[0]) and \
               sign(prev_vec[1]) == sign(v2[1]):

                edges[nid2]["dir"] = dir_pair2
                q.append(nid2)
            # 부호 다르면 스레드 생성 안함

    return edges

# -----------------------------
# 저장
# -----------------------------
import json

def save_graph(nodes, edges, filename):
    out = {
        "nodes": nodes,
        "edges": edges
    }
    with open(filename, "w") as f:
        json.dump(out, f, indent=2)

# -----------------------------
# 실행
# -----------------------------
if __name__ == "__main__":
    segments = load_dxf_lines("Drawing1.dxf")
    nodes, edges = build_nodes_edges(segments)
    edges = build_directed_graph(nodes, edges)
    save_graph(nodes, edges, "graph.json")

    print("완료: graph.json 생성")