# Telecom Fault Diagnosis Agent

A NOC (Network Operations Center) tool that traces a telecom network with **BFS/DFS**, simulates a fault,
lets a **Python rule-based classifier** identify the fault type, and shows the result in the console and in a
**browser dashboard**. Java and Python talk **only through JSON files**. Standard Java + Python libraries only.

## 1. Problem statement
Finding the cause of a network outage by reading logs manually is slow. A NOC needs a tool that follows the
network topology, finds the affected path and automatically suggests the likely fault and the repair action.

## 2. Objectives
- Represent the network as a graph (towers = nodes, links = edges) loaded from JSON.
- Trace fault paths with BFS and DFS; show the detour after a fault.
- Simulate 5 fault types and generate symptoms.
- Classify the fault with rules (Python) and show fault type, confidence, reason, action.
- Demonstrate Java OOP, file handling and Java-Python integration.

## 3. Project structure
```
TelecomFaultDiagnosis/
├── java/   Main, Tower, NetworkGraph, Fault, SymptomLog, DiagnosticAgent, JsonUtil
├── python/ fault_classifier.py, test_classifier.py
├── data/   topology.json, symptoms.json, diagnosis.json, dashboard_data.js
├── dashboard.html      <- open in a browser
├── run.bat / run.sh    <- compile + demo
└── README.md
```
`JsonUtil.java` is a small JSON reader/writer, so no external library (Gson/Jackson) is needed.

## 4. Modules
| Module | File | Job |
|---|---|---|
| Tower | Tower.java | One node: id, name, type, position (encapsulated fields) |
| NetworkGraph | NetworkGraph.java | Adjacency-list graph, JSON loading, BFS, DFS, impact analysis |
| Fault | Fault.java | Fault type + location, symptom generation |
| SymptomLog | SymptomLog.java | Symptom record written to symptoms.json |
| DiagnosticAgent | DiagnosticAgent.java | Runs the whole workflow, calls Python, reads the answer |
| Main | Main.java | Menu / command line / NOC display |
| Classifier | fault_classifier.py | Rule matching, confidence, reason, action |
| Dashboard | dashboard.html | Topology map, paths, diagnosis, history |

## 5. How Java and Python communicate (files only)
```
Java  --writes-->  data/symptoms.json  --read by-->  Python
Java  <--reads---  data/diagnosis.json <--written--  Python
```
1. `DiagnosticAgent` writes `data/symptoms.json`.
2. It starts `python python/fault_classifier.py data/symptoms.json data/diagnosis.json` (tries `python`, `python3`, `py`).
3. Python classifies and writes `data/diagnosis.json`.
4. Java deletes the old diagnosis before each run (no stale results), then reads the new one and displays it.
5. Java also writes `data/dashboard_data.js`, which `dashboard.html` loads (works by double-click, no server).

Sample `symptoms.json`:
```json
{ "log_id": "LOG-001", "timestamp": "2026-09-30 10:15:00", "source": "T1", "destination": "T6",
  "symptoms": ["Link down", "Connection lost", "Packet loss on T2-T4"] }
```
Sample `diagnosis.json`:
```json
{ "log_id": "LOG-001", "fault_type": "Link Failure", "confidence": 100,
  "matched_rule": "link down + connection lost", "severity": "HIGH",
  "reason": "Link Failure symptoms detected (link down + connection lost)",
  "recommended_action": "Inspect the affected network link (fiber/microwave), ..." }
```

## 6. Topology (data/topology.json)
```
T1 --- T2 --- T3 --- T5
        |      |      |
        T4     T7     |
        |      |      |
        T6 ----+------+     (T6-T5, T6-T8, T7-T8)
         \_____ T8 ____/          T9 = isolated
```
Precisely: T1-T2, T2-T3, T2-T4, T3-T5, T3-T7, T4-T6, T5-T6, T6-T8, T7-T8. **T9 has no links** (used to test disconnected nodes).

## 7. Setup: VS Code
1. Install JDK 11+ and Python 3. Check: `javac -version` and `python --version`.
2. Open the `TelecomFaultDiagnosis` folder in VS Code (**Open Folder**) and use the terminal there.
3. Compile (from the project root):
```
javac -d out java/*.java
```
4. Run:
```
java -cp out Main                            # interactive menu
java -cp out Main --demo                     # all 5 fault types
java -cp out Main T1 T6 LINK_FAILURE T2-T4   # one diagnosis
```
5. Open `dashboard.html` (double-click, or menu option 5).
6. If arrows look broken on Windows: run `chcp 65001` first.

## 8. Sample input and expected output
Input: `java -cp out Main T1 T6 LINK_FAILURE T2-T4`
```
========================================
      TELECOM FAULT DIAGNOSIS AGENT
========================================
Log ID       : LOG-001
Source Tower : T1 (Central Hub)
Destination  : T6 (Harbor Point)
Simulated    : Link Failure @ T2-T4

BFS Path (shortest):
  T1 → T2 → T4 → T6
DFS Path:
  T1 → T2 → T3 → T5 → T6
Path after fault:
  T1 → T2 → T3 → T5 → T6
Affected towers: none (traffic can be re-routed)

Symptoms:
• Link down
• Connection lost
• Packet loss on T2-T4

Diagnosis:
FAULT TYPE : LINK FAILURE
CONFIDENCE : 100%  (rule: link down + connection lost)
SEVERITY   : HIGH
REASON     : Link Failure symptoms detected (link down + connection lost)
ACTION     : Inspect the affected network link (fiber/microwave), check connectors and switch over to the backup route.
CHECK      : injected 'Link Failure' -> MATCH
========================================
```

## 9. Test cases
| # | Command / input | Expected |
|---|---|---|
| 1 | `Main T1 T6 LINK_FAILURE T2-T4` | Link Failure, 100%, re-route via T3-T5 |
| 2 | `Main T1 T6 TOWER_FAILURE T4` | Tower Failure, affected `[T4]`, re-route T1→T2→T3→T5→T6 |
| 3 | `Main T1 T6 POWER_ISSUE T4` | Signal/Power Issue, 100% |
| 4 | `Main T1 T6 HARDWARE_FAILURE T4` | Hardware Failure, 100% |
| 5 | `Main T1 T6 CONGESTION T4` | Congestion, path unchanged (traffic still flows) |
| 6 | `Main T1 T99 LINK_FAILURE` | `Unknown tower 'T99'` + list of valid towers |
| 7 | `Main T1 T9 TOWER_FAILURE T4` | `T1 and T9 are disconnected` |
| 8 | `Main T1 T6 LINK_FAILURE T1-T6` | `No link 'T1-T6' in the topology` |
| 9 | `Main T1 T6 banana` | Unknown fault type message |
| 10 | `Main T1 T1 CONGESTION T1` | Source and destination are the same tower |
| 11 | `python python/test_classifier.py` | 9 classifier tests pass (includes partial 50% match and unknown fault) |

## 10. Classifier rules
| Fault | Required symptoms |
|---|---|
| Tower Failure | "no signal" + "tower unreachable" |
| Link Failure | "link down" + "connection lost" |
| Congestion | "high traffic" + "slow network" |
| Signal/Power Issue | "power low" + "equipment offline" |
| Hardware Failure | "device error" + "hardware alarm" |

Confidence = matched phrases / required phrases (both = 100%, one = 50%). No match = *Unknown Fault*, escalate to an engineer.

## 11. Algorithms in one minute
- **BFS**: queue, visits towers level by level, so the path has the fewest hops. Time O(V+E).
- **DFS**: recursion (stack), goes deep then backtracks; finds *a* path, not always the shortest. O(V+E).
- **Fault re-routing**: BFS is run again while ignoring the failed tower/link; if it finds a path, traffic can be re-routed.
- **Affected towers**: towers reachable before the fault but not after it.

## 12. Advantages
Faster than reading logs by hand; visual topology; explainable rules (reason + action); no external libraries; works offline; easy to extend with new rules or towers by editing JSON/Python only.

## 13. Limitations
Faults are simulated (no live network or SNMP feed); rules are exact phrase matches; one fault at a time; links are unweighted (BFS = fewest hops, not lowest latency); Python must be installed and on PATH.

## 14. Future enhancements
Read real logs/SNMP traps; train an ML classifier (scikit-learn) on labelled logs; weighted links with Dijkstra; multiple simultaneous faults; live auto-refresh dashboard; alerts by email/SMS; store history in a database.

## 15. Viva questions
1. **Why a graph?** Towers and links are naturally nodes and edges; traversal answers "how does traffic get from A to B?".
2. **BFS vs DFS?** BFS = queue, shortest hop path. DFS = stack/recursion, any path, less memory on wide graphs.
3. **Why an adjacency list?** O(V+E) memory and fast neighbour lookup for sparse networks; a matrix would waste space.
4. **Complexity of BFS/DFS?** O(V+E) time, O(V) space.
5. **Where is encapsulation?** Private fields with getters in Tower, Fault, SymptomLog, NetworkGraph.
6. **Where is OOP abstraction/enum use?** `Fault.Type` enum holds the five types and their labels.
7. **How do Java and Python communicate?** Through JSON files: symptoms.json in, diagnosis.json out, started with `ProcessBuilder`.
8. **Why files and not sockets?** Simple, debuggable, language-neutral, and each side can be tested alone.
9. **Is the classifier AI?** It is rule-based (expert-system style AI); the confidence score is matched/required phrases. It can be replaced with an ML model without changing Java.
10. **How are disconnected nodes handled?** BFS/DFS return an empty path and Java shows a "disconnected" message (test T9).
11. **How do you handle invalid input?** `checkTower()` throws `IllegalArgumentException` with the valid tower list; `main` catches and prints it.
12. **Why does congestion not change the path?** It slows traffic but does not cut it, so `blocksTraffic()` is false.
13. **How would you make it real-time?** Replace the simulated symptoms with SNMP traps/syslog and refresh the dashboard by polling.
