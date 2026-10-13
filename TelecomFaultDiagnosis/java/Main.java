import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point. Run from the PROJECT ROOT (the folder that contains java/, python/, data/).
 *   java -cp out Main                          -> interactive NOC menu
 *   java -cp out Main --demo                   -> runs all 5 fault types automatically
 *   java -cp out Main T1 T6 LINK_FAILURE T2-T4 -> one diagnosis from the command line
 */
public class Main {
    private static NetworkGraph graph;
    private static DiagnosticAgent agent;
    private static Path baseDir;

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, "UTF-8"));   // so → and • print correctly

        baseDir = Paths.get("").toAbsolutePath();
        if (!Files.exists(baseDir.resolve("data/topology.json")) && baseDir.getParent() != null
                && Files.exists(baseDir.getParent().resolve("data/topology.json")))
            baseDir = baseDir.getParent();                            // allow running from inside java/

        try {
            graph = NetworkGraph.loadFromFile(baseDir.resolve("data/topology.json"));
        } catch (IOException | RuntimeException e) {
            System.out.println("ERROR: cannot load data/topology.json -> " + e.getMessage());
            System.out.println("Run this program from the project root folder.");
            return;
        }
        agent = new DiagnosticAgent(graph, baseDir);

        if (args.length == 1 && args[0].equals("--demo")) runDemo();
        else if (args.length >= 3) runFromArgs(args);
        else menu();
    }

    // ---------------- command-line modes ----------------

    private static void runDemo() {
        for (Fault.Type t : Fault.Type.values()) {
            try { run("T1", "T6", agent.suggestFault(t, "T1", "T6")); }
            catch (Exception e) { System.out.println("ERROR: " + e.getMessage()); }
        }
        finish();
    }

    private static void runFromArgs(String[] a) {
        try {
            Fault.Type type = parseType(a[2]);
            Fault fault = a.length > 3 ? new Fault(type, a[3]) : agent.suggestFault(type, a[0], a[1]);
            run(a[0], a[1], fault);
            finish();
        } catch (Exception e) { System.out.println("ERROR: " + e.getMessage()); }
    }

    // ---------------- interactive menu ----------------

    private static void menu() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== NOC MENU ===");
            System.out.println("1. Show network topology");
            System.out.println("2. Trace path (BFS & DFS)");
            System.out.println("3. Simulate a fault and diagnose");
            System.out.println("4. Run demo (all 5 fault types)");
            System.out.println("5. Open dashboard");
            System.out.println("0. Exit");
            System.out.print("Choose: ");
            if (!sc.hasNextLine()) return;
            String c = sc.nextLine().trim();
            try {
                switch (c) {
                    case "1": showTopology(); break;
                    case "2": tracePaths(sc); break;
                    case "3": simulate(sc); break;
                    case "4": runDemo(); break;
                    case "5": openDashboard(); break;
                    case "0": return;
                    default: System.out.println("Please enter 0-5.");
                }
            } catch (Exception e) { System.out.println("ERROR: " + e.getMessage()); }
        }
    }

    private static void showTopology() {
        System.out.println("\nTowers:");
        for (Tower t : graph.getTowers())
            System.out.println("  " + t + "  neighbours: " + graph.getNeighbors(t.getId()));
        System.out.println("Links: " + graph.getLinks().size());
    }

    private static void tracePaths(Scanner sc) {
        System.out.print("Source tower: ");      String s = sc.nextLine();
        System.out.print("Destination tower: "); String d = sc.nextLine();
        printPath("BFS Path", graph.bfs(s, d, null));
        printPath("DFS Path", graph.dfs(s, d, null));
    }

    private static void simulate(Scanner sc) throws Exception {
        System.out.print("Source tower: ");      String s = sc.nextLine();
        System.out.print("Destination tower: "); String d = sc.nextLine();
        Fault.Type[] types = Fault.Type.values();
        for (int i = 0; i < types.length; i++) System.out.println("  " + (i + 1) + ". " + types[i].getLabel());
        System.out.print("Fault type (1-5): ");
        Fault.Type type = parseType(sc.nextLine());
        System.out.print("Fault location (tower like T4, or link like T2-T4; Enter = automatic): ");
        String loc = sc.nextLine().trim();
        Fault fault = loc.isEmpty() ? agent.suggestFault(type, s, d) : new Fault(type, loc);
        run(s, d, fault);
        agent.exportDashboard();
        System.out.println("Dashboard updated -> open dashboard.html (menu option 5).");
    }

    // ---------------- helpers ----------------

    private static Fault.Type parseType(String text) {
        String t = text.trim().toUpperCase().replace(' ', '_');
        try {
            int n = Integer.parseInt(t);
            if (n >= 1 && n <= Fault.Type.values().length) return Fault.Type.values()[n - 1];
        } catch (NumberFormatException ignored) { /* maybe it is a name */ }
        for (Fault.Type f : Fault.Type.values()) if (f.name().equals(t)) return f;
        throw new IllegalArgumentException("Unknown fault type '" + text + "'. Use 1-5 or LINK_FAILURE, TOWER_FAILURE, "
                + "POWER_ISSUE, HARDWARE_FAILURE, CONGESTION.");
    }

    private static void run(String s, String d, Fault fault) throws Exception {
        DiagnosticAgent.Report r = agent.diagnose(s, d, fault);
        display(r);
    }

    private static void finish() {
        try {
            Path f = agent.exportDashboard();
            System.out.println("\nDashboard data saved: " + baseDir.relativize(f));
            System.out.println("Open dashboard.html in your browser to see the results.");
        } catch (IOException e) { System.out.println("ERROR: " + e.getMessage()); }
    }

    private static void openDashboard() throws IOException {
        agent.exportDashboard();
        Path html = baseDir.resolve("dashboard.html");
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            Desktop.getDesktop().browse(html.toUri());
        else System.out.println("Open this file in your browser: " + html);
    }

    private static void printPath(String title, List<String> path) {
        System.out.println(title + ":");
        System.out.println(path.isEmpty() ? "  No path found (towers disconnected)" : "  " + String.join(" → ", path));
    }

    /** NOC-style result screen. */
    private static void display(DiagnosticAgent.Report r) {
        String line = "========================================";
        System.out.println("\n" + line);
        System.out.println("      TELECOM FAULT DIAGNOSIS AGENT");
        System.out.println(line);
        System.out.println("Log ID       : " + r.id);
        System.out.println("Source Tower : " + r.source + " (" + graph.getTower(r.source).getName() + ")");
        System.out.println("Destination  : " + r.destination + " (" + graph.getTower(r.destination).getName() + ")");
        System.out.println("Simulated    : " + r.fault);
        System.out.println();
        printPath("BFS Path (shortest)", r.bfsPath);
        printPath("DFS Path", r.dfsPath);
        printPath("Path after fault", r.reroutePath);
        System.out.println("Affected towers: " + (r.affected.isEmpty() ? "none (traffic can be re-routed)" : r.affected));
        System.out.println("\nSymptoms:");
        for (String s : r.log.getSymptoms()) System.out.println("• " + s);
        System.out.println("\nDiagnosis:");
        System.out.println("FAULT TYPE : " + r.diagnosedType().toUpperCase());
        System.out.println("CONFIDENCE : " + ((Number) r.diagnosis.get("confidence")).intValue() + "%  (rule: " + r.diagnosis.get("matched_rule") + ")");
        System.out.println("SEVERITY   : " + r.diagnosis.get("severity"));
        System.out.println("REASON     : " + r.diagnosis.get("reason"));
        System.out.println("ACTION     : " + r.diagnosis.get("recommended_action"));
        System.out.println("CHECK      : injected '" + r.fault.getType().getLabel() + "' -> "
                + (r.isCorrect() ? "MATCH" : "MISMATCH"));
        System.out.println(line);
    }
}
