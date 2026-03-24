package httpserver.itf.impl;

import httpserver.itf.HttpRequest;
import httpserver.itf.HttpRicmletResponse;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

public class HttpRicmletResponseImpl implements HttpRicmletResponse {
    protected PrintStream m_ps;

    protected HttpRicmletResponseImpl(PrintStream ps) {
        m_ps = ps;
    }

    @Override
    public void setCookie(String name, String value) {
        // Nothing yet to do
    }

    @Override
    public void setReplyOk() throws IOException {
        m_ps.println("HTTP/1.0 200 OK");
        m_ps.println("Date: " + new Date());
        m_ps.println("Server: ricm-http 1.0");
    }

    @Override
    public void setReplyError(int codeRet, String msg) throws IOException {
        m_ps.println("HTTP/1.0 "+codeRet+" "+msg);
        m_ps.println("Date: " + new Date());
        m_ps.println("Server: ricm-http 1.0");
        m_ps.println("Content-type: text/html");
        m_ps.println();
        m_ps.println("<HTML><HEAD><TITLE>"+msg+"</TITLE></HEAD>");
        m_ps.println("<BODY><H4>HTTP Error "+codeRet+": "+msg+"</H4></BODY></HTML>");
        m_ps.flush();
    }

    @Override
    public void setContentLength(int length) throws IOException {
        // Nothing to do here : the end of the page will be dynamically determined by the browser
    }

    @Override
    public void setContentType(String type) throws IOException {m_ps.println("Content-type: " + type);
    }


    @Override
    public PrintStream beginBody() throws IOException {
        m_ps.println();
        return m_ps;
    }
}
