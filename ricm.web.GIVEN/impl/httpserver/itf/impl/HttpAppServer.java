package httpserver.itf.impl;

import httpserver.itf.HttpRequest;
import httpserver.itf.HttpResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * Modify the HttpServer to use applications, that is jar files containing app-speific ricmlets
 */
public class HttpAppServer extends HttpServer {

    private static final String MAIN_FOLDER = System.getProperty("user.dir") + File.separator + "ricm.web.GIVEN" + File.separator;

    protected HttpAppServer(int port, String folderName) {
        super(port, folderName);
    }

    @Override
    public HttpRequest getRequest(BufferedReader br) throws IOException {
        HttpRequest request = null;
        String startline = br.readLine();
        StringTokenizer parseLine = new StringTokenizer(startline);
        String method = parseLine.nextToken().toUpperCase();
        String resourceName = parseLine.nextToken();
        if (method.equals("GET")) {
            if (resourceName.startsWith("/ricmlets")) {
                // ======= now we wait the ricmlet to be in an app ======
                request = new HttpAppRequestImpl(this, method, resourceName, br);
                // ======================================================
            } else {
                request = new HttpStaticRequest(this, method, resourceName);
            }
        } else {
            request = new UnknownRequest(this, method, resourceName);
        }
        return request;
    }

    @Override
    public HttpResponse getResponse(HttpRequest req, PrintStream ps) {
        // AppRequest replace RicmletRequest
        if (req instanceof HttpAppRequestImpl) {
            return new HttpRicmletResponseImpl(ps);
        }
        return new HttpResponseImpl(this, req, ps);
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args.length != 2) {
            //System.out.println("Usage: java Server <port-number> <file folder>");
            HttpServer hs = new HttpAppServer(port, MAIN_FOLDER);
            hs.loop();
        } else {
            port = Integer.parseInt(args[0]);
            String folderName = args[1];
            HttpServer hs = new HttpAppServer(port, folderName);
            hs.loop();
        }
    }

}

