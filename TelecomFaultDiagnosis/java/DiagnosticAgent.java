import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The "brain" of the project. For one request it:
 *  1. traces paths with BFS and DFS      2. applies the simulated fault and re-routes
 *  3. generates symptoms                 4. writes data/symptoms.json
 *  5. runs the Python classifier         6. reads data/diagnosis.json back
 */
public class DiagnosticAgent {

    /** Plain data holder for one finished diagnosis (public final fields keep it short). */
    public static class Report {
        public final String id, source, destination;
        public final List<String> bfsPath, dfsPath, reroutePath, affected;
        public final Fault fault;
        public final SymptomLog log;
        public final Map<String, Object> diagnosis;

        Report(String id, String s, String d, List<String> bfs, List<String> dfs, List<String> re,
               List<String> aff, Fault f, SymptomLog log, Map<String, Object> diag) {
            this.id = id; this.source = s; this.destination = d; this.bfsPath = bfs; this.dfsPath = dfs;
            this.reroutePath = re; this.affected = aff; this.fault = f; this.log = log; this.diagnosis = diag;
        }

        public String diagnosedType() { return String.valueOf(diagnosis.get("fault_type")); }
        public boolean isCorrect()    { return diagnosedType().equals(fault.getType().getLabel()); }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id); m.put("timestamp", log.getTimestamp());
            m.put("source", source); m.put("destination", destination);
            m.put("bfsPath", bfsPath); m.put("dfsPath", dfsPath);
            m.put("reroutePath", reroutePath); m.put("affected", affected);
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("type", fault.getType().getLabel()); f.put("location", fault.getLocation());
            m.put("fault", f);
            m.put("symptoms", log.getSymptoms());
            m.put("diagnosis", diagnosis);
            m.put("correct", isCorrect());
            return m;
        }
    }

    private final NetworkGraph graph;
    private final Path dataDir;
    private final Path script;
    private final List<Report> history = new ArrayList<>();

    public DiagnosticAgent(NetworkGraph graph, Path projectDir) {
        this.graph = graph;
        this.dataDir = projectDir.resolve("data");
        this.script = projectDir.resolve("python").resolve("fault_classifier.py");
    }

    public List<Report> getHistory() { return history; }

    /** Picks a sensible fault location: the middle of the BFS path (a tower, or a link for Link Failure). */
    public Fault suggestFault(Fault.Type type, String src, String dst) {
        List<String> p = graph.bfs(src, dst, null);
        if (p.size() < 2) throw new IllegalArgumentException(NetworkGraph.norm(src) + " and " + NetworkGraph.norm(dst) + " are disconnected - no path exists in the topology.");
        int mid = p.size() / 2;
        String location = (type == Fault.Type.LINK_FAILURE) ? p.get(mid - 1) + "-" + p.get(mid) : p.get(mid);
        return new Fault(type, location);
    }

    public Report diagnose(String src, String dst, Fault fault) throws IOException, InterruptedException {
        src = NetworkGraph.norm(src); dst = NetworkGraph.norm(dst);
        graph.checkTower(src);                                   // invalid tower names -> friendly error
        graph.checkTower(dst);
        if (src.equals(dst)) throw new IllegalArgumentException("Source and destination are the same tower.");

        // validate fault location
        String loc = fault.getLocation();
        if (fault.isLinkFault()) {
            String[] ab = loc.split("-");
            if (ab.length != 2 || !graph.hasLink(ab[0], ab[1]))
                throw new IllegalArgumentException("No link '" + loc + "' in the topology (use format T2-T4).");
        } else graph.checkTower(loc);

        // STEP 1: trace on the healthy network with both algorithms
        List<String> bfs = graph.bfs(src, dst, null);
        List<String> dfs = graph.dfs(src, dst, null);
        if (bfs.isEmpty())
            throw new IllegalArgumentException(src + " and " + dst + " are disconnected - no path exists in the topology.");

        // STEP 2: apply the fault and look for a detour + list of cut-off towers
        List<String> reroute = graph.bfs(src, dst, fault);
        List<String> affected = graph.affectedTowers(src, fault);

        // STEP 3-4: generate symptoms and export them for Python
        String id = String.format("LOG-%03d", history.size() + 1);
        SymptomLog log = new SymptomLog(id, src, dst, fault.generateSymptoms());
        Path in = dataDir.resolve("symptoms.json");
        Path out = dataDir.resolve("diagnosis.json");
        log.writeTo(in);
        Files.deleteIfExists(out);                               // never read a stale result

        // STEP 5-6: Python classifies, Java reads the answer
        runClassifier(in, out);
        if (!Files.exists(out)) throw new IOException("Python did not create " + out);
        @SuppressWarnings("unchecked")
        Map<String, Object> diagnosis =
                (Map<String, Object>) JsonUtil.parse(new String(Files.readAllBytes(out), StandardCharsets.UTF_8));

        Report r = new Report(id, src, dst, bfs, dfs, reroute, affected, fault, log, diagnosis);
        history.add(r);
        return r;
    }

    /** Java-Python integration: start the script as a separate process; they only share files. */
    private void runClassifier(Path in, Path out) throws IOException, InterruptedException {
        String lastError = "";
        for (String cmd : new String[]{"python", "python3", "py"}) {
            Process p;
            try {
                p = new ProcessBuilder(cmd, script.toString(), in.toString(), out.toString())
                        .redirectErrorStream(true).start();
            } catch (IOException notFound) { continue; }         // this command name doesn't exist, try next
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (p.waitFor() == 0) return;
            lastError = output;
        }
        throw new IOException("Could not run the Python classifier. " + lastError
                + "\nInstall Python 3 or run manually:  python python/fault_classifier.py");
    }

    /** Writes data/dashboard_data.js (a JS file so dashboard.html works by double-click, no server needed). */
    public Path exportDashboard() throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("topology", graph.toMap());
        List<Object> runs = new ArrayList<>();
        for (Report r : history) runs.add(r.toMap());
        root.put("runs", runs);
        Path file = dataDir.resolve("dashboard_data.js");
        Files.write(file, ("window.NOC_DATA = " + JsonUtil.toJson(root) + ";\n").getBytes(StandardCharsets.UTF_8));
        return file;
    }
}
