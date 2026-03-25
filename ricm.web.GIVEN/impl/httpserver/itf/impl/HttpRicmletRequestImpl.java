package httpserver.itf.impl;

import httpserver.itf.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

public class HttpRicmletRequestImpl extends HttpRicmletRequest {

    // keep the already launched ricmlets
    static HashMap<String, HttpRicmlet> cacheRicmlet = new HashMap<>();
    private final HashMap<String, String> arguments = new HashMap<>();
    static HashMap<String, String> cookies = new HashMap<>();
    private BufferedReader br;

    public HttpRicmletRequestImpl(HttpServer hs, String method, String resourceName, BufferedReader br) throws IOException {
        super(hs, method, resourceName, br);
        //parsing arguments
        String line;
        while ((line = br.readLine()) != null && !line.isEmpty()) {
            if (line.startsWith("Cookie:")) {
                System.out.println(line);
                String cookieStr = line.substring("Cookie: ".length());

                String[] pairs = cookieStr.split(";");

                for (String pair : pairs) {
                    String[] kv = pair.trim().split("=", 2);
                    if (kv.length == 2) {
                        cookies.put(kv[0], kv[1]);
                    }
                }
            }
        }
        System.out.println(cookies);
    }

    @Override
    public HttpSession getSession() {
        //nothing yet to do
        return null;
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
        if (cacheRicmlet.containsKey(className)) {
            ricmlet = cacheRicmlet.get(className);
        } else {
            Class<?> c = Class.forName(className);
            ricmlet = (HttpRicmlet) c.getDeclaredConstructor().newInstance();
            cacheRicmlet.put(className, ricmlet);
        }
        // the ricmlet will now recover its args and directly send the answer to the HttpRicmletResponse
        ricmlet.doGet(this, ricmletResp);
    }


}
