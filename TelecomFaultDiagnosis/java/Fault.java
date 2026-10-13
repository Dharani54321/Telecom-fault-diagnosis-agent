import java.util.ArrayList;
import java.util.List;

/** A simulated network fault: WHAT went wrong (type) and WHERE (a tower id like "T4" or a link like "T2-T4"). */
public class Fault {

    /** The five fault types required by the project. */
    public enum Type {
        LINK_FAILURE("Link Failure"),
        TOWER_FAILURE("Tower Failure"),
        POWER_ISSUE("Signal/Power Issue"),
        HARDWARE_FAILURE("Hardware Failure"),
        CONGESTION("Congestion");

        private final String label;
        Type(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final Type type;
    private final String location;

    public Fault(Type type, String location) {
        this.type = type;
        this.location = location.trim().toUpperCase();
    }

    public Type getType()       { return type; }
    public String getLocation() { return location; }
    public boolean isLinkFault() { return type == Type.LINK_FAILURE; }

    /** Congestion only slows traffic; every other fault stops traffic at that tower/link. */
    public boolean blocksTraffic() { return type != Type.CONGESTION; }

    public boolean blocksTower(String towerId) {
        return blocksTraffic() && !isLinkFault() && location.equals(towerId);
    }

    public boolean blocksLink(String a, String b) {
        return isLinkFault() && (location.equals(a + "-" + b) || location.equals(b + "-" + a));
    }

    /**
     * Generates the symptom messages a NOC would see for this fault.
     * The first two phrases of each type are the ones the Python rules look for;
     * the last one is extra "noise" that makes the log look realistic.
     */
    public List<String> generateSymptoms() {
        List<String> s = new ArrayList<>();
        switch (type) {
            case LINK_FAILURE:
                s.add("Link down"); s.add("Connection lost"); s.add("Packet loss on " + location); break;
            case TOWER_FAILURE:
                s.add("No signal"); s.add("Tower unreachable"); s.add("Heartbeat timeout from " + location); break;
            case POWER_ISSUE:
                s.add("Power low"); s.add("Equipment offline"); s.add("Battery backup draining at " + location); break;
            case HARDWARE_FAILURE:
                s.add("Device error"); s.add("Hardware alarm"); s.add("Line card fault at " + location); break;
            case CONGESTION:
                s.add("High traffic"); s.add("Slow network"); s.add("Utilisation above 90% at " + location); break;
        }
        return s;
    }

    @Override
    public String toString() { return type.getLabel() + " @ " + location; }
}
