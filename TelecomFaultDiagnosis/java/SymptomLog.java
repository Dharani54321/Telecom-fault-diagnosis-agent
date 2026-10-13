import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The symptom log Java hands to Python (written as data/symptoms.json). */
public class SymptomLog {
    private final String logId;
    private final String timestamp;
    private final String source;
    private final String destination;
    private final List<String> symptoms;

    public SymptomLog(String logId, String source, String destination, List<String> symptoms) {
        this.logId = logId;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.source = source;
        this.destination = destination;
        this.symptoms = symptoms;
    }

    public String getLogId()          { return logId; }
    public String getTimestamp()      { return timestamp; }
    public List<String> getSymptoms() { return symptoms; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("log_id", logId); m.put("timestamp", timestamp);
        m.put("source", source); m.put("destination", destination);
        m.put("symptoms", symptoms);
        return m;
    }

    /** File handling: write this log as JSON so the Python classifier can read it. */
    public void writeTo(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        Files.write(file, JsonUtil.toJson(toMap()).getBytes(StandardCharsets.UTF_8));
    }
}
