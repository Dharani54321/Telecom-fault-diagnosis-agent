import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The telecom network as a GRAPH.
 *   nodes (vertices) = towers      -> Map<String, Tower>
 *   edges            = links       -> adjacency list  Map<String, List<String>>
 * Links are two-way (undirected), so each link is stored in both towers' neighbour lists.
 * Contains the two traversal algorithms: BFS (shortest path) and DFS (deep path).
 */
public class NetworkGraph {
    private final Map<String, Tower> towers = new LinkedHashMap<>();
    private final Map<String, List<String>> adjacency = new LinkedHashMap<>();
    private final List<String[]> links = new ArrayList<>();   // each entry: {from, to, type}

    // ---------------- building the graph ----------------

    public void addTower(Tower t) {
        towers.put(t.getId(), t);
        adjacency.put(t.getId(), new ArrayList<>());
    }

    public void addLink(String a, String b, String type) {
        a = norm(a); b = norm(b);
        checkTower(a);
        checkTower(b);
        adjacency.get(a).add(b);
        adjacency.get(b).add(a);
        links.add(new String[]{a, b, type});
    }

    /** Reads data/topology.json and builds the graph. */
    @SuppressWarnings("unchecked")
    public static NetworkGraph loadFromFile(Path file) throws IOException {
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        Map<String, Object> root = (Map<String, Object>) JsonUtil.parse(text);
        NetworkGraph g = new NetworkGraph();
        for (Object o : (List<Object>) root.get("towers")) {
            Map<String, Object> m = (Map<String, Object>) o;
            g.addTower(new Tower((String) m.get("id"), (String) m.get("name"), (String) m.get("type"),
                    ((Double) m.get("x")).intValue(), ((Double) m.get("y")).intValue()));
        }
        for (Object o : (List<Object>) root.get("links")) {
            Map<String, Object> m = (Map<String, Object>) o;
            g.addLink((String) m.get("from"), (String) m.get("to"), (String) m.get("type"));
        }
        return g;
    }

    // ---------------- helpers ----------------

    public static String norm(String id) { return id == null ? "" : id.trim().toUpperCase(); }

    public boolean hasTower(String id) { return towers.containsKey(norm(id)); }

    /** Throws a friendly error for an invalid tower name. */
    public void checkTower(String id) {
        if (!hasTower(id))
            throw new IllegalArgumentException("Unknown tower '" + id + "'. Valid towers: " + towers.keySet());
    }

    public boolean hasLink(String a, String b) {
        a = norm(a); b = norm(b);
        return towers.containsKey(a) && adjacency.get(a).contains(b);
    }

    public Tower getTower(String id)       { return towers.get(norm(id)); }
    public Collection<Tower> getTowers()   { return towers.values(); }
    public List<String[]> getLinks()       { return links; }
    public List<String> getNeighbors(String id) { return adjacency.get(norm(id)); }

    /** A step from 'from' to 'to' is blocked if the fault kills that tower or that link. */
    private boolean blocked(String from, String to, Fault f) {
        return f != null && (f.blocksTower(to) || f.blocksLink(from, to));
    }

    // ---------------- BFS ----------------

    /**
     * Breadth-First Search: explores tower-by-tower, level-by-level using a QUEUE.
     * Because it visits nearer towers first, the path it finds has the FEWEST hops.
     * Returns an empty list if no path exists (disconnected / blocked by the fault).
     */
    public List<String> bfs(String src, String dst, Fault fault) {
        src = norm(src); dst = norm(dst);
        checkTower(src); checkTower(dst);
        if (fault != null && (fault.blocksTower(src) || fault.blocksTower(dst))) return new ArrayList<>();

        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        Map<String, String> parent = new HashMap<>();   // remembers "who discovered me" to rebuild the path
        queue.add(src);
        visited.add(src);

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (cur.equals(dst)) {                       // rebuild path from destination back to source
                LinkedList<String> path = new LinkedList<>();
                for (String at = dst; at != null; at = parent.get(at)) path.addFirst(at);
                return path;
            }
            for (String next : adjacency.get(cur)) {
                if (!visited.contains(next) && !blocked(cur, next, fault)) {
                    visited.add(next);
                    parent.put(next, cur);
                    queue.add(next);
                }
            }
        }
        return new ArrayList<>();
    }

    // ---------------- DFS ----------------

    /**
     * Depth-First Search: goes as deep as possible along one branch, then backtracks.
     * Uses recursion (the call stack works like a STACK). It finds A path, not always the shortest.
     */
    public List<String> dfs(String src, String dst, Fault fault) {
        src = norm(src); dst = norm(dst);
        checkTower(src); checkTower(dst);
        if (fault != null && (fault.blocksTower(src) || fault.blocksTower(dst))) return new ArrayList<>();

        List<String> path = new ArrayList<>();
        if (dfsVisit(src, dst, fault, new HashSet<>(), path)) return path;
        return new ArrayList<>();
    }

    private boolean dfsVisit(String cur, String dst, Fault fault, Set<String> visited, List<String> path) {
        visited.add(cur);
        path.add(cur);
        if (cur.equals(dst)) return true;
        for (String next : adjacency.get(cur)) {
            if (!visited.contains(next) && !blocked(cur, next, fault)
                    && dfsVisit(next, dst, fault, visited, path)) return true;
        }
        path.remove(path.size() - 1);                    // dead end -> backtrack
        return false;
    }

    // ---------------- impact analysis ----------------

    /** All towers reachable from src (BFS flood), optionally with a fault applied. */
    private Set<String> reachable(String src, Fault fault) {
        Set<String> seen = new LinkedHashSet<>();
        if (fault != null && fault.blocksTower(src)) return seen;
        Queue<String> q = new LinkedList<>();
        q.add(src); seen.add(src);
        while (!q.isEmpty()) {
            String cur = q.poll();
            for (String n : adjacency.get(cur))
                if (!seen.contains(n) && !blocked(cur, n, fault)) { seen.add(n); q.add(n); }
        }
        return seen;
    }

    /** Towers that were reachable before the fault but are cut off (or down) after it. */
    public List<String> affectedTowers(String src, Fault fault) {
        Set<String> before = reachable(norm(src), null);
        before.removeAll(reachable(norm(src), fault));
        return new ArrayList<>(before);
    }

    // ---------------- export for dashboard ----------------

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Object> ts = new ArrayList<>();
        for (Tower t : towers.values()) ts.add(t.toMap());
        List<Object> ls = new ArrayList<>();
        for (String[] l : links) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("from", l[0]); e.put("to", l[1]); e.put("type", l[2]);
            ls.add(e);
        }
        m.put("towers", ts);
        m.put("links", ls);
        return m;
    }
}
