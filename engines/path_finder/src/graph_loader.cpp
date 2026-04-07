#include "oht/path_finder/graph_loader.hpp"

#include <cmath>
#include <cctype>
#include <cstddef>
#include <fstream>
#include <iterator>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

namespace oht::path_finder {

namespace {

class MinimalJsonReader {
public:
    explicit MinimalJsonReader(std::string text) : text_(std::move(text)) {}

    char peek() {
        skip_ws();
        if (at_end()) {
            throw std::runtime_error("unexpected end of graph.json");
        }
        return text_[pos_];
    }

    bool consume_if(char expected) {
        skip_ws();
        if (!at_end() && text_[pos_] == expected) {
            ++pos_;
            return true;
        }
        return false;
    }

    void expect(char expected, const char* context) {
        skip_ws();
        if (at_end() || text_[pos_] != expected) {
            throw std::runtime_error(std::string("invalid graph.json: expected ") + expected +
                                     " while parsing " + context);
        }
        ++pos_;
    }

    std::string read_string() {
        skip_ws();
        expect('"', "string key");

        std::string result;
        while (!at_end()) {
            const char ch = text_[pos_++];
            if (ch == '"') {
                return result;
            }
            if (ch == '\\') {
                if (at_end()) {
                    throw std::runtime_error("invalid graph.json: dangling escape in string");
                }
                const char escaped = text_[pos_++];
                if (escaped == '"' || escaped == '\\' || escaped == '/') {
                    result.push_back(escaped);
                    continue;
                }
                throw std::runtime_error("invalid graph.json: unsupported escaped character");
            }
            result.push_back(ch);
        }

        throw std::runtime_error("invalid graph.json: unterminated string");
    }

    double read_number() {
        skip_ws();
        const std::size_t start = pos_;

        if (!at_end() && (text_[pos_] == '-' || text_[pos_] == '+')) {
            ++pos_;
        }
        while (!at_end() && std::isdigit(static_cast<unsigned char>(text_[pos_]))) {
            ++pos_;
        }
        if (!at_end() && text_[pos_] == '.') {
            ++pos_;
            while (!at_end() && std::isdigit(static_cast<unsigned char>(text_[pos_]))) {
                ++pos_;
            }
        }

        if (!at_end() && (text_[pos_] == 'e' || text_[pos_] == 'E')) {
            ++pos_;
            if (!at_end() && (text_[pos_] == '-' || text_[pos_] == '+')) {
                ++pos_;
            }
            while (!at_end() && std::isdigit(static_cast<unsigned char>(text_[pos_]))) {
                ++pos_;
            }
        }

        if (start == pos_) {
            throw std::runtime_error("invalid graph.json: expected numeric value");
        }

        return std::stod(text_.substr(start, pos_ - start));
    }

    std::int64_t read_int64() {
        const double value = read_number();
        const auto int_value = static_cast<std::int64_t>(value);
        if (value != static_cast<double>(int_value)) {
            throw std::runtime_error("invalid graph.json: expected integer value");
        }
        return int_value;
    }

    bool read_null() {
        skip_ws();
        if (text_.compare(pos_, 4, "null") == 0) {
            pos_ += 4;
            return true;
        }
        return false;
    }

    void ensure_finished() {
        skip_ws();
        if (!at_end()) {
            throw std::runtime_error("invalid graph.json: trailing content");
        }
    }

private:
    bool at_end() const { return pos_ >= text_.size(); }

    void skip_ws() {
        while (!at_end() && std::isspace(static_cast<unsigned char>(text_[pos_]))) {
            ++pos_;
        }
    }

    std::string text_;
    std::size_t pos_ = 0;
};

struct RawEdge {
    NodeId start = 0;
    NodeId end = 0;
    bool has_dir = false;
    NodeId dir_from = 0;
    NodeId dir_to = 0;
};

double distance_between(const Node& from, const Node& to) {
    if (!from.x || !from.y || !to.x || !to.y) {
        throw std::runtime_error("graph.json contains node without coordinates");
    }
    const double dx = *from.x - *to.x;
    const double dy = *from.y - *to.y;
    return std::sqrt((dx * dx) + (dy * dy));
}

std::vector<Node> parse_nodes(MinimalJsonReader& reader) {
    reader.expect('[', "nodes array");

    std::vector<Node> nodes;
    NodeId next_id = 0;

    if (reader.consume_if(']')) {
        return nodes;
    }

    while (true) {
        reader.expect('[', "node coordinate pair");
        const double x = reader.read_number();
        reader.expect(',', "node x/y separator");
        const double y = reader.read_number();
        reader.expect(']', "node coordinate pair end");
        nodes.push_back(Node{next_id++, x, y});

        if (reader.consume_if(']')) {
            break;
        }
        reader.expect(',', "nodes array separator");
    }

    return nodes;
}

RawEdge parse_edge(MinimalJsonReader& reader) {
    reader.expect('{', "edge object");

    bool has_start = false;
    bool has_end = false;
    bool has_dir = false;
    RawEdge edge;

    while (true) {
        const std::string key = reader.read_string();
        reader.expect(':', "edge field separator");

        if (key == "start") {
            edge.start = reader.read_int64();
            has_start = true;
        } else if (key == "end") {
            edge.end = reader.read_int64();
            has_end = true;
        } else if (key == "dir") {
            has_dir = true;
            if (reader.read_null()) {
                edge.has_dir = false;
            } else {
                reader.expect('[', "dir array");
                edge.dir_from = reader.read_int64();
                reader.expect(',', "dir separator");
                edge.dir_to = reader.read_int64();
                reader.expect(']', "dir array end");
                edge.has_dir = true;
            }
        } else {
            throw std::runtime_error("invalid graph.json: unsupported edge field '" + key + "'");
        }

        if (reader.consume_if('}')) {
            break;
        }
        reader.expect(',', "edge field separator");
    }

    if (!has_start || !has_end || !has_dir) {
        throw std::runtime_error("invalid graph.json: edge requires start, end, and dir fields");
    }
    return edge;
}

std::vector<RawEdge> parse_edges(MinimalJsonReader& reader) {
    reader.expect('[', "edges array");

    std::vector<RawEdge> edges;
    if (reader.consume_if(']')) {
        return edges;
    }

    while (true) {
        edges.push_back(parse_edge(reader));
        if (reader.consume_if(']')) {
            break;
        }
        reader.expect(',', "edge object separator");
    }

    return edges;
}

void parse_graph_json(
    MinimalJsonReader& reader,
    std::vector<Node>* nodes,
    std::vector<RawEdge>* edges) {
    reader.expect('{', "root object");

    bool seen_nodes = false;
    bool seen_edges = false;

    while (true) {
        const std::string key = reader.read_string();
        reader.expect(':', "root field separator");

        if (key == "nodes") {
            *nodes = parse_nodes(reader);
            seen_nodes = true;
        } else if (key == "edges") {
            *edges = parse_edges(reader);
            seen_edges = true;
        } else {
            throw std::runtime_error("invalid graph.json: unsupported root field '" + key + "'");
        }

        if (reader.consume_if('}')) {
            break;
        }
        reader.expect(',', "root field separator");
    }

    if (!seen_nodes || !seen_edges) {
        throw std::runtime_error("invalid graph.json: root must contain nodes and edges");
    }

    reader.ensure_finished();
}

void add_directed_edge(
    const std::vector<Node>& parsed_nodes,
    NodeId from,
    NodeId to,
    Graph* graph,
    GraphMetadata* metadata,
    std::int64_t* next_resource_id,
    std::vector<std::size_t>* out_degrees,
    std::vector<std::size_t>* in_degrees) {
    if (from < 0 || to < 0 || static_cast<std::size_t>(from) >= parsed_nodes.size() ||
        static_cast<std::size_t>(to) >= parsed_nodes.size()) {
        throw std::runtime_error("invalid graph.json: edge references unknown node index");
    }

    const double distance = distance_between(parsed_nodes[from], parsed_nodes[to]);
    graph->add_edge(from, to, distance, distance);

    EdgeMetadata edge;
    edge.resource_id = (*next_resource_id)++;
    edge.from = from;
    edge.to = to;
    metadata->resource_to_edge_index.emplace(edge.resource_id, metadata->edges.size());
    metadata->edges.push_back(edge);
    ++(*out_degrees)[from];
    ++(*in_degrees)[to];
}

}  // namespace

LoadedGraph GraphLoader::load_from_graph_json(const std::string& path) {
    std::ifstream input(path);
    if (!input) {
        throw std::runtime_error("failed to open graph json: " + path);
    }

    const std::string text{
        std::istreambuf_iterator<char>(input),
        std::istreambuf_iterator<char>()};
    MinimalJsonReader reader(text);

    std::vector<Node> parsed_nodes;
    std::vector<RawEdge> parsed_edges;
    parse_graph_json(reader, &parsed_nodes, &parsed_edges);

    LoadedGraph loaded;
    loaded.metadata.nodes.reserve(parsed_nodes.size());
    loaded.metadata.resource_to_edge_index.reserve(parsed_edges.size() * 2U);
    std::vector<std::size_t> out_degrees(parsed_nodes.size(), 0);
    std::vector<std::size_t> in_degrees(parsed_nodes.size(), 0);
    std::int64_t next_resource_id = 0;

    for (const Node& node : parsed_nodes) {
        loaded.graph.add_node(node.id, *node.x, *node.y);
        loaded.metadata.nodes.push_back(NodeMetadata{node.id});
    }

    for (const RawEdge& edge : parsed_edges) {
        if (edge.has_dir) {
            add_directed_edge(
                parsed_nodes,
                edge.dir_from,
                edge.dir_to,
                &loaded.graph,
                &loaded.metadata,
                &next_resource_id,
                &out_degrees,
                &in_degrees);
        } else {
            add_directed_edge(
                parsed_nodes,
                edge.start,
                edge.end,
                &loaded.graph,
                &loaded.metadata,
                &next_resource_id,
                &out_degrees,
                &in_degrees);
            add_directed_edge(
                parsed_nodes,
                edge.end,
                edge.start,
                &loaded.graph,
                &loaded.metadata,
                &next_resource_id,
                &out_degrees,
                &in_degrees);
        }
    }

    for (std::size_t i = 0; i < loaded.metadata.nodes.size(); ++i) {
        const std::size_t degree = in_degrees[i] + out_degrees[i];
        if (degree != 2U) {
            loaded.metadata.nodes[i].kind = NodeKind::Junction;
        }
    }

    for (EdgeMetadata& edge : loaded.metadata.edges) {
        const NodeMetadata& from = loaded.metadata.nodes[static_cast<std::size_t>(edge.from)];
        const NodeMetadata& to = loaded.metadata.nodes[static_cast<std::size_t>(edge.to)];
        if (from.kind == NodeKind::Junction || to.kind == NodeKind::Junction) {
            edge.kind = EdgeKind::BranchCandidate;
        }
    }

    // TODO(phase-6): add graph compaction and richer block/resource segmentation.
    // TODO(phase-6): infer parking/wait/station nodes, zones, and merge semantics.
    // TODO(phase-6): integrate route execution in sim_core and couple traffic_manager beyond per-edge resource ids.
    // TODO(phase-6): enrich idle_control metadata and replace fixed cost with congestion-aware travel-time models.

    return loaded;
}

}  // namespace oht::path_finder
