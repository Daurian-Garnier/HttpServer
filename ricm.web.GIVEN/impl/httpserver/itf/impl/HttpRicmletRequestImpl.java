package httpserver.itf.impl;

import httpserver.itf.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class HttpRicmletRequestImpl extends HttpRicmletRequest {

    private static final long SESSION_LIFETIME = 5000L;

    // Shared between all threads → ConcurrentHashMap
    static final ConcurrentHashMap<String, HttpRicmlet> ricmlets = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, HttpSession> sessions = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<String, Long> lastSessionCalls = new ConcurrentHashMap<>();

    // Specific to each request → instance, not static
    private final HashMap<String, String> arguments = new HashMap<>();
    private final HashMap<String, String> cookies = new HashMap<>();
    private HttpSession my_session;

    public HttpRicmletRequestImpl(HttpServer hs, String method,
                                  String resourceName, BufferedReader br) throws IOException {
        super(hs, method, resourceName, br);
        parseArguments();   // 1. extract the URL arguments
        parseHeaders(br);   // 2. read the headers (including Cookie)
        initSession();     // 3. resolve or create the session
    }

    // --- Parsing des arguments dans l'URL (?key=val&...) ---
    private void parseArguments() {
        if (m_resourceName.contains("?")) {
            int sep = m_resourceName.indexOf("?");
            String query = m_resourceName.substring(sep + 1);
            for (String arg : query.split("&")) {
                String[] kv = arg.split("=", 2);
                if (kv.length == 2) arguments.put(kv[0], kv[1]);
            }
            m_resourceName = m_resourceName.substring(0, sep); // nettoyer l'URL
        }
    }

    // --- Reading HTTP headers ---
    private void parseHeaders(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Cookie:")) {
                parseCookieLine(line);
            }
        }
    }

    // --- Parsing Cookie line: key=val;key2=val2 ---
    private void parseCookieLine(String line) {
        String cookieStr = line.substring("Cookie: ".length());
        for (String pair : cookieStr.split(";")) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2) {
                cookies.put(kv[0].trim(), kv[1].trim());
            }
        }
    }

    // --- Resolution or creation of the  session ---
    private void initSession() {
        String sessionID = cookies.get("SessionID");
        if (sessionID != null && sessions.containsKey(sessionID)) {
            long lastCall = lastSessionCalls.getOrDefault(sessionID, 0L);
            if (System.currentTimeMillis() - lastCall < SESSION_LIFETIME) {
                my_session = sessions.get(sessionID); // session existante valide
            }
        }
        if (my_session == null) {
            // New session
            HttpSession newSession = new Session();
            sessions.put(newSession.getId(), newSession);
            my_session = newSession;
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

        // Class name resolution from the URL
        String className = m_resourceName
                .replaceFirst("/ricmlets/", "")
                .replace("/", ".");

        // Thread‑safe singleton instantiation
        HttpRicmlet ricmlet = ricmlets.computeIfAbsent(className, name -> {
            try {
                Class<?> c = Class.forName(name);
                return (HttpRicmlet) c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Ricmlet not found: " + name, e);
            }
        });

        // Session cookie added before setReplyOk()
        ricmletResp.setCookie("SessionID", my_session.getId());
        ricmlet.doGet(this, ricmletResp);
    }
}