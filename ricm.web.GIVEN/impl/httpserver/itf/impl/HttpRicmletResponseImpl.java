package httpserver.itf.impl;

import httpserver.itf.HttpRicmletResponse;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of the HTTP response for dynamic requests (ricmlets).
 * Unlike static responses, the content length is not known in advance,
 * so headers are buffered and written all at once in beginBody().
 */
public class HttpRicmletResponseImpl implements HttpRicmletResponse {

    // Output stream to write the HTTP response to the client
    protected PrintStream m_ps;

    // HTTP status code and message, default is 200 OK
    private int statusCode = 200;
    private String statusMsg = "OK";

    /**
     * Constructor
     * @param ps the output stream connected to the client socket
     */
    protected HttpRicmletResponseImpl(PrintStream ps) {
        m_ps = ps;
    }

    /**
     * Inner class representing an HTTP cookie (name=value pair)
     */
    private static class Cookie {
        final String name;
        final String value;

        Cookie(String name, String value) {
            this.name  = name;
            this.value = value;
        }
    }

    // List of cookies to be sent in the response headers (Set-Cookie)
    private final List<Cookie> cookies = new LinkedList<>();

    // List of content headers (e.g. Content-type) to be sent before the body
    private final List<String> contentHeaders = new LinkedList<>();

    /**
     * Adds a cookie to the response.
     * The cookie will be written in beginBody() with the other headers.
     * @param name  the cookie name
     * @param value the cookie value
     */
    @Override
    public void setCookie(String name, String value) {
        cookies.add(new Cookie(name, value));
    }

    /**
     * Stores the OK status (200).
     * The status line is NOT written here — it will be written in beginBody()
     * to ensure all headers and cookies are written in the correct order.
     */
    @Override
    public void setReplyOk() throws IOException {
        statusCode = 200;
        statusMsg  = "OK";
    }

    /**
     * Writes an error response immediately to the client.
     * Unlike setReplyOk(), errors are written right away because
     * there is no dynamic body to wait for.
     * @param codeRet the HTTP error code (e.g. 404)
     * @param msg     the error message (e.g. "Ricmlet not found")
     */
    @Override
    public void setReplyError(int codeRet, String msg) throws IOException {
        m_ps.println("HTTP/1.0 " + codeRet + " " + msg);
        m_ps.println("Date: "    + new Date());
        m_ps.println("Server: ricm-http 1.0");
        m_ps.println("Content-type: text/html");
        m_ps.println();
        m_ps.println("<HTML><HEAD><TITLE>" + msg + "</TITLE></HEAD>");
        m_ps.println("<BODY><H4>HTTP Error " + codeRet + ": " + msg + "</H4></BODY></HTML>");
        m_ps.flush();
    }

    /**
     * Not used for ricmlet responses.
     * The content length cannot be determined in advance for dynamic responses,
     * so the browser will detect the end of the response through HTML tags.
     */
    @Override
    public void setContentLength(int length) throws IOException {
        // Nothing to do — length is unknown for dynamic responses
    }

    /**
     * Stores the content type header to be written later in beginBody().
     * @param type the MIME type (e.g. "text/html")
     */
    @Override
    public void setContentType(String type) throws IOException {
        // Store the header instead of writing it immediately
        // so that beginBody() can write all headers in the correct order
        contentHeaders.add("Content-type: " + type);
    }

    /**
     * Writes all buffered headers to the output stream in the correct order,
     * then writes an empty line to signal the end of the headers.
     * This method must be called last, after setReplyOk(), setContentType()
     * and all setCookie() calls.
     * Order of headers written:
     *   1. Status line  (HTTP/1.0 200 OK)
     *   2. Date + Server
     *   3. Content-type
     *   4. Set-Cookie headers
     *   5. Empty line (end of headers)
     *
     * @return the output stream to write the response body to
     */
    @Override
    public PrintStream beginBody() throws IOException {
        // 1. Write the status line
        m_ps.println("HTTP/1.0 " + statusCode + " " + statusMsg);

        // 2. Write date and server headers
        m_ps.println("Date: "  + new Date());
        m_ps.println("Server: ricm-http 1.0");

        // 3. Write content headers (e.g. Content-type: text/html)
        for (String header : contentHeaders) {
            m_ps.println(header);
        }

        // 4. Write all Set-Cookie headers
        for (Cookie cookie : cookies) {
            m_ps.println("Set-Cookie: " + cookie.name + "=" + cookie.value);
        }

        // 5. Write empty line to signal end of headers
        m_ps.println();
        m_ps.flush();

        // Return the stream so the ricmlet can write the response body
        return m_ps;
    }
}
