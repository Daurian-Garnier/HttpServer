package httpserver.itf.impl;

import httpserver.itf.HttpRicmletResponse;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class HttpRicmletResponseImpl implements HttpRicmletResponse {

    protected PrintStream m_ps;

    // HTTP status stored and written later in beginBody()
    private int    statusCode = 200;
    private String statusMsg  = "OK";

    protected HttpRicmletResponseImpl(PrintStream ps) {
        m_ps = ps;
    }

    // Internal cookie representation with optional Max-Age
    private static class Cookie {
        final String name;
        final String value;
        final int maxAge; // -1 means no Max-Age directive

        Cookie(String name, String value, int maxAge) {
            this.name   = name;
            this.value  = value;
            this.maxAge = maxAge;
        }
    }

    // All cookies and headers are stored and written together in beginBody()
    private final List<Cookie> cookies        = new LinkedList<>();
    private final List<String> contentHeaders = new LinkedList<>();

    // Stores a cookie to be sent in the response
    @Override
    public void setCookie(String name, String value) {
        if (name == null || value == null) return;
        cookies.add(new Cookie(name, value, -1));
    }

    // Stores the OK status (written later in beginBody)
    @Override
    public void setReplyOk() throws IOException {
        statusCode = 200;
        statusMsg  = "OK";
    }

    // Writes an error response immediately since there is no dynamic body
    @Override
    public void setReplyError(int codeRet, String msg) throws IOException {
        if (msg == null) msg = "Unknown error";
        m_ps.println("HTTP/1.0 " + codeRet + " " + msg);
        m_ps.println("Date: "    + new Date());
        m_ps.println("Server: ricm-http 1.0");
        m_ps.println("Content-type: text/html");
        m_ps.println();
        m_ps.println("<HTML><HEAD><TITLE>" + msg + "</TITLE></HEAD>");
        m_ps.println("<BODY><H4>HTTP Error " + codeRet + ": " + msg + "</H4></BODY></HTML>");
        m_ps.flush();
    }

    // Not used for ricmlets: response length is determined dynamically by the browser
    @Override
    public void setContentLength(int length) throws IOException {
        // Nothing to do here
    }

    // Stores the content type header to be written later in beginBody()
    @Override
    public void setContentType(String type) throws IOException {
        if (type == null) return;
        contentHeaders.add("Content-type: " + type);
    }
    // Sets a cookie with explicit Max-Age to force browser update
    public void setCookieWithMaxAge(String name, String value, int maxAge) {
        if (name == null || value == null) return;
        cookies.add(new Cookie(name, value, maxAge));
    }

    // Writes all headers and cookies in the correct order, then returns the PrintStream for the body
    @Override
    public PrintStream beginBody() throws IOException {
        // 1. Status line
        m_ps.println("HTTP/1.0 " + statusCode + " " + statusMsg);
        m_ps.println("Date: "    + new Date());
        m_ps.println("Server: ricm-http 1.0");

        // 2. Content headers (Content-type, etc.)
        for (String header : contentHeaders) {
            if (header != null) m_ps.println(header);
        }

        // 3. Set-Cookie headers
        for (Cookie cookie : cookies) {
            if (cookie == null || cookie.name == null) continue;
            String line = "Set-Cookie: " + cookie.name + "=" + cookie.value;
            if (cookie.maxAge >= 0) line += "; Max-Age=" + cookie.maxAge;
            m_ps.println(line);
        }

        // 4. Empty line marks the end of headers and start of body
        m_ps.println();
        m_ps.flush();
        return m_ps;
    }
}
