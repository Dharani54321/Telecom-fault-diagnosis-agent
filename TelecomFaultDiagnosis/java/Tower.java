import java.util.LinkedHashMap;
import java.util.Map;

/** One node of the network graph (a telecom tower). Fields are private (encapsulation). */
public class Tower {
    private final String id;      // e.g. "T1"
    private final String name;    // e.g. "Central Hub"
    private final String type;    // Core / Aggregation / Access
    private final int x, y;       // position used only by the dashboard drawing
    private String status;        // ACTIVE or FAULTY

    public Tower(String id, String name, String type, int x, int y) {
        this.id = id.trim().toUpperCase();
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.status = "ACTIVE";
    }

    public String getId()     { return id; }
    public String getName()   { return name; }
    public String getType()   { return type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Used when exporting the topology to the dashboard. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id); m.put("name", name); m.put("type", type); m.put("x", x); m.put("y", y);
        return m;
    }

    @Override
    public String toString() { return id + " (" + name + ", " + type + ")"; }
}
