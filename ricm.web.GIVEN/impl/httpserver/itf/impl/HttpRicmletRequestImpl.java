package httpserver.itf.impl;

import httpserver.itf.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

public class HttpRicmletRequestImpl extends HttpRicmletRequest {

    private static final Long SESSION_LIFETIME = 5000L;
    // keep the already launched ricmlets
    static HashMap<String, HttpRicmlet> ricmlets = new HashMap<>();
    static HashMap<String, HttpSession> sessions = new HashMap<>();
    static HashMap<String, Long> lastSessionCalls = new HashMap<>();
    private final HashMap<String, String> arguments = new HashMap<>();
    static HashMap<String, String> cookies = new HashMap<>();
    private BufferedReader br;
    private HttpSession my_session;

    public HttpRicmletRequestImpl(HttpServer hs, String method, String resourceName, BufferedReader br) throws IOException {
        super(hs, method, resourceName, br);
        //parsing arguments
        String line;
        while ((line = br.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Cookie:")) {
                readCookie(line);
            }
        }
        if (my_session == null) {
            HttpSession newSession = new Session();
            sessions.put(newSession.getId(), newSession);
            my_session = newSession;
        }
        lastSessionCalls.put(my_session.getId(), System.currentTimeMillis());
    }

    private void readCookie(String line) {
        //System.out.println(line);
        String cookieStr = line.substring("Cookie: ".length());

        String[] pairs = cookieStr.split(";");

        for (String pair : pairs) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2) {
                // handling session
                if (kv[0].equals("SessionID")) {
                    String sessionID = kv[1];
                    if (sessions.containsKey(sessionID) && System.currentTimeMillis()-lastSessionCalls.get(sessionID) < SESSION_LIFETIME) {
                        my_session = sessions.get(sessionID);
                    }
                }
                cookies.put(kv[0], kv[1]);
            }
        }
    }

    @Override
    public HttpSession getSession() {
        return my_session;
    }

    @Override
    public String getArg(String name) {
        return arguments.getOrDefault(name, null);
    }

    @Override
    public String getCookie(String name) {
        return cookies.getOrDefault(name, null);
    }

    @Override
    public void process(HttpResponse resp) throws Exception {
        HttpRicmletResponse ricmletResp = (HttpRicmletResponse) resp;
        //separate classPart and arguments
        String classPart = m_resourceName;
        if (m_resourceName.contains("?")) {
            int separation = m_resourceName.indexOf("?");
            classPart = m_resourceName.substring(0, separation);
            String query = m_resourceName.substring(separation + 1);
            String[] argsTable = query.split("&");

            for (String arg : argsTable) {
                String[] kv = arg.split("=");
                if (kv.length == 2) {
                    arguments.put(kv[0], kv[1]);
                }
            }
        }
        String className = classPart
                .replaceFirst("/ricmlets/", "")
                .replace("/", ".");
        HttpRicmlet ricmlet;
        if (ricmlets.containsKey(className)) {
            ricmlet = ricmlets.get(className);
        } else {
            Class<?> c = Class.forName(className);
            ricmlet = (HttpRicmlet) c.getDeclaredConstructor().newInstance();
            ricmlets.put(className, ricmlet);
        }
        // the ricmlet will now recover its args and directly send the answer to the HttpRicmletResponse
        ricmletResp.setCookie("SessionID", my_session.getId());
        ricmlet.doGet(this, ricmletResp);
    }


}
