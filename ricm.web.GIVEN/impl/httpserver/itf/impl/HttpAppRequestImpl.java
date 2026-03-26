package httpserver.itf.impl;

import httpserver.itf.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class HttpAppRequestImpl extends HttpRicmletRequest{

    private static final long SESSION_LIFETIME = 5000L;
    private static final Application application = new Application();

    private static final HashMap<String, HttpSession> sessions = new HashMap<>();
    private static final HashMap<String, Long> lastSessionCalls = new HashMap<>();

    private final HashMap<String, String> arguments = new HashMap<>();
    private final HashMap<String, String> cookies = new HashMap<>();
    private HttpSession my_session;

    public HttpAppRequestImpl(HttpServer hs, String method, String resourceName, BufferedReader br) throws IOException {
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

    void readCookie(String line) {
        //System.out.println(line);
        String cookieStr = line.substring("Cookie: ".length());

        String[] pairs = cookieStr.split(";");

        for (String pair : pairs) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2) {
                // handling session
                if (kv[0].equals("SessionID")) {
                    String sessionID = kv[1];
                    Long last = lastSessionCalls.get(sessionID);

                    if (last != null &&
                            sessions.containsKey(sessionID) &&
                            System.currentTimeMillis() - last < SESSION_LIFETIME) {

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
        // ====== Change here : we now only use Application ======
        String[] parts = classPart.split("/");
        String appName = parts[2];
        String className = String.join(".", Arrays.copyOfRange(parts, 3, parts.length));
        // The application ensure the ricmlet's unicity
        HttpRicmlet ricmlet = application.getInstance(className, appName, ClassLoader.getSystemClassLoader());
        // the ricmlet will now recover its args and directly send the answer to the HttpRicmletResponse
        ricmletResp.setCookie("SessionID", my_session.getId());
        ricmlet.doGet(this, ricmletResp);
        // =======================================================
    }


}
