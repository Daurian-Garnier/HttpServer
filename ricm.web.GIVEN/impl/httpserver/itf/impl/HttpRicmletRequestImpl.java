package httpserver.itf.impl;

import httpserver.itf.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HttpRicmletRequestImpl extends HttpRicmletRequest {

    // Session lifetime in milliseconds (10 seconds for testing)
    private static final long SESSION_LIFETIME = 10000L;
    private boolean sessionChanged = false;


    // Shared between all threads → ConcurrentHashMap for thread safety
    static final ConcurrentHashMap<String, HttpRicmlet> ricmlets         = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, HttpSession> sessions         = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Long>        lastSessionCalls = new ConcurrentHashMap<>();

    // Background daemon thread that cleans expired sessions every 10 seconds
    static {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-cleaner");
            t.setDaemon(true); // stops automatically when server stops
            return t;
        });
        cleaner.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            lastSessionCalls.forEach((sessionId, lastCall) -> {
                if (now - lastCall > SESSION_LIFETIME) {
                    sessions.remove(sessionId);
                    lastSessionCalls.remove(sessionId);
                    System.out.println("Session expired and removed: " + sessionId);
                }
            });
        }, 10, 10, TimeUnit.SECONDS); // every 10 seconds for testing
    }

    // Specific to each request → instance variables, not static
    private final HashMap<String, String> arguments = new HashMap<>();
    private final HashMap<String, String> cookies   = new HashMap<>();
    private HttpSession my_session;

    public HttpRicmletRequestImpl(HttpServer hs, String method,
                                  String resourceName, BufferedReader br) throws IOException {
        super(hs, method, resourceName, br);
        parseArguments(); // 1. extract arguments from URL
        parseHeaders(br); // 2. read HTTP headers including cookies
        initSession();    // 3. resolve existing session or create new one
    }

    // Extracts key=value arguments from the URL query string (?key=val&key2=val2)
    private void parseArguments() {
        if (m_resourceName.contains("?")) {
            int sep = m_resourceName.indexOf("?");
            String query = m_resourceName.substring(sep + 1);
            for (String arg : query.split("&")) {
                String[] kv = arg.split("=", 2);
                if (kv.length == 2) arguments.put(kv[0], kv[1]);
            }
            // Remove query string from resource name
            m_resourceName = m_resourceName.substring(0, sep);
        }
    }

    // Reads HTTP headers line by line until empty line
    private void parseHeaders(BufferedReader br) throws IOException {
        if (br == null) return;
        String line;
        try {
            while ((line = br.readLine()) != null && !line.isEmpty()) {
                if (line.startsWith("Cookie:")) {
                    parseCookieLine(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading headers: " + e.getMessage());
        }
    }

    // Parses a Cookie header line: Cookie: key1=val1; key2=val2
    private void parseCookieLine(String line) {
        if (line == null || line.isEmpty()) return;
        int colonIndex = line.indexOf(":");
        // Ignore malformed or empty cookie lines
        if (colonIndex == -1 || colonIndex == line.length() - 1) return;
        String cookieStr = line.substring(colonIndex + 1).trim();
        if (cookieStr.isEmpty()) return;
        for (String pair : cookieStr.split(";")) {
            if (pair.trim().isEmpty()) continue;
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2 && kv[0] != null && !kv[0].isEmpty() && kv[1] != null) {
                cookies.put(kv[0].trim(), kv[1].trim());
            }
        }
    }

    // Resolves the session from the SessionID cookie, or creates a new one
    private void initSession() {
        String sessionID = cookies.get("SessionID");
        if (sessionID != null && sessions.containsKey(sessionID)) {
            long lastCall = lastSessionCalls.getOrDefault(sessionID, 0L);
            if (System.currentTimeMillis() - lastCall < SESSION_LIFETIME) {
                my_session = sessions.get(sessionID);
                System.out.println("Session reused: " + sessionID);
            } else {
                sessions.remove(sessionID);
                lastSessionCalls.remove(sessionID);
                System.out.println("Session expired on access: " + sessionID);
                sessionChanged = true; // session expirée → forcer nouveau cookie
            }
        }
        if (my_session == null) {
            HttpSession newSession = new Session();
            sessions.put(newSession.getId(), newSession);
            my_session = newSession;
            sessionChanged = true; // nouvelle session → forcer cookie
            System.out.println("New session created: " + my_session.getId());
        }
        lastSessionCalls.put(my_session.getId(), System.currentTimeMillis());
    }

    @Override
    public HttpSession getSession() {
        return my_session;
    }

    @Override
    public String getArg(String name) {
        return arguments.get(name);
    }

    @Override
    public String getCookie(String name) {
        return cookies.get(name);
    }

    @Override
    public void process(HttpResponse resp) throws Exception {
        HttpRicmletResponse ricmletResp = (HttpRicmletResponse) resp;

        String className = m_resourceName
                .replaceFirst("/ricmlets/", "")
                .replace("/", ".");

        HttpRicmlet ricmlet = ricmlets.computeIfAbsent(className, name -> {
            try {
                Class<?> c = Class.forName(name);
                return (HttpRicmlet) c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Ricmlet not found: " + name, e);
            }
        });

        if (ricmletResp instanceof HttpRicmletResponseImpl respImpl) {
            if (sessionChanged) {
                // Force browser to update SessionID cookie with new value
                respImpl.setCookieWithMaxAge("SessionID", my_session.getId(), 3600);
            } else {
                respImpl.setCookie("SessionID", my_session.getId());
            }
        }

        ricmlet.doGet(this, ricmletResp);
    }
}