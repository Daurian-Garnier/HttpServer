package httpserver.itf.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.StringTokenizer;

import httpserver.itf.HttpRequest;
import httpserver.itf.HttpResponse;
import httpserver.itf.HttpRicmlet;


/**
 * Basic HTTP Server Implementation
 * <p>
 * Only manages static requests
 * The url for a static ressource is of the form: "http//host:port/<path>/<ressource name>"
 * For example, try accessing the following urls from your brower:
 * http://localhost:<port>/
 * http://localhost:<port>/voile.jpg
 * ...
 */
public class HttpServer {

    private static final String MAIN_FOLDER = System.getProperty("user.dir") + File.separator + "ricm.web.GIVEN"+File.separator;
    private int m_port;
    private File m_folder;  // default folder for accessing static resources (files)
    private ServerSocket m_ssoc;

    protected HttpServer(int port, String folderName) {
        m_port = port;
        if (!folderName.endsWith(File.separator))
            folderName = folderName + File.separator;
        m_folder = new File(folderName);
        try {
            m_ssoc = new ServerSocket(m_port);
            System.out.println("HttpServer started on port " + m_port);
        } catch (IOException e) {
            System.out.println("HttpServer Exception:" + e);
            System.exit(1);
        }
    }

    public File getFolder() {
        return m_folder;
    }


    public HttpRicmlet getInstance(String clsname)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, MalformedURLException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        throw new Error("No Support for Ricmlets");
    }


    /*
     * Reads a request on the given input stream and returns the corresponding HttpRequest object
     */
    public HttpRequest getRequest(BufferedReader br) throws IOException {
        HttpRequest request = null;

        String startline = br.readLine();
        StringTokenizer parseLine = new StringTokenizer(startline);
        String method = parseLine.nextToken().toUpperCase();
        String resourceName = parseLine.nextToken();
        if (method.equals("GET")) {
            if (resourceName.startsWith("/ricmlets")){
                request = new HttpRicmletRequestImpl(this, method, resourceName, br);
            } else {
            request = new HttpStaticRequest(this, method, resourceName);}
        } else
            request = new UnknownRequest(this, method, resourceName);
        return request;
    }


    /*
     * Returns an HttpResponse object associated to the given HttpRequest object
     */
    public HttpResponse getResponse(HttpRequest req, PrintStream ps) {
        if (req instanceof HttpRicmletRequestImpl) {
            return new HttpRicmletResponseImpl(ps);
        }
        return new HttpResponseImpl(this, req, ps);
    }


    /*
     * Server main loop
     */
    protected void loop() {
        try {
            while (true) {
                Socket soc = m_ssoc.accept();
                (new HttpWorker(this, soc)).start();
            }
        } catch (IOException e) {
            System.out.println("HttpServer Exception, skipping request");
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        int port = 8080;
        if (args.length != 2) {
            //System.out.println("Usage: java Server <port-number> <file folder>");
            HttpServer hs = new HttpServer(port, MAIN_FOLDER);
            hs.loop();
        } else {
            port = Integer.parseInt(args[0]);
            String folderName = args[1];
            HttpServer hs = new HttpServer(port, folderName);
            hs.loop();
        }
    }

}

