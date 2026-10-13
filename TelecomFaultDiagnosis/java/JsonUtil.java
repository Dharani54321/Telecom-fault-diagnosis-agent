import java.util.*;

/**
 * Tiny JSON reader/writer so the project needs NO external libraries.
 * parse()  : JSON text -> Map / List / String / Double / Boolean / null
 * toJson() : Map / List / String / Number / Boolean -> pretty JSON text
 */
public class JsonUtil {
    private final String s;
    private int i = 0;

    private JsonUtil(String s) { this.s = s; }

    public static Object parse(String text) {
        JsonUtil p = new JsonUtil(text);
        Object v = p.value();
        p.ws();
        if (p.i != p.s.length()) throw p.err("Unexpected extra data");
        return v;
    }

    private IllegalArgumentException err(String m) {
        return new IllegalArgumentException("Invalid JSON at position " + i + ": " + m);
    }
    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    private char peek() { ws(); if (i >= s.length()) throw err("Unexpected end"); return s.charAt(i); }
    private void expect(char c) { if (peek() != c) throw err("Expected '" + c + "'"); i++; }

    private Object value() {
        char c = peek();
        if (c == '{') return object();
        if (c == '[') return array();
        if (c == '"') return string();
        if (s.startsWith("true", i))  { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i))  { i += 4; return null; }
        return number();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        expect('{');
        if (peek() == '}') { i++; return m; }
        while (true) {
            String k = string();
            expect(':');
            m.put(k, value());
            char c = peek(); i++;
            if (c == '}') break;
            if (c != ',') throw err("Expected ',' or '}'");
        }
        return m;
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        expect('[');
        if (peek() == ']') { i++; return l; }
        while (true) {
            l.add(value());
            char c = peek(); i++;
            if (c == ']') break;
            if (c != ',') throw err("Expected ',' or ']'");
        }
        return l;
    }

    private String string() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'u': sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                    default:  sb.append(e);
                }
            } else sb.append(c);
        }
        throw err("Unterminated string");
    }

    private Double number() {
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (st == i) throw err("Unexpected character '" + s.charAt(i) + "'");
        return Double.valueOf(s.substring(st, i));
    }

    // ---------- writer ----------
    public static String toJson(Object v) { StringBuilder sb = new StringBuilder(); write(sb, v, 0); return sb.toString(); }

    private static void write(StringBuilder sb, Object v, int ind) {
        String pad = "  ".repeat(ind + 1), end = "  ".repeat(ind);
        if (v == null) sb.append("null");
        else if (v instanceof String) sb.append(quote((String) v));
        else if (v instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) v;
            if (m.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n"); int n = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                sb.append(pad).append(quote(String.valueOf(e.getKey()))).append(": ");
                write(sb, e.getValue(), ind + 1);
                sb.append(++n < m.size() ? ",\n" : "\n");
            }
            sb.append(end).append("}");
        } else if (v instanceof List) {
            List<?> l = (List<?>) v;
            if (l.isEmpty()) { sb.append("[]"); return; }
            sb.append("[\n");
            for (int k = 0; k < l.size(); k++) {
                sb.append(pad); write(sb, l.get(k), ind + 1);
                sb.append(k + 1 < l.size() ? ",\n" : "\n");
            }
            sb.append(end).append("]");
        } else if (v instanceof Double && ((Double) v) % 1 == 0) sb.append(((Double) v).longValue());
        else sb.append(v);   // Integer, Boolean, etc.
    }

    private static String quote(String t) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : t.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
