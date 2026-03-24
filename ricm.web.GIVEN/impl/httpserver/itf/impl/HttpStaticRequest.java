package httpserver.itf.impl;

import java.io.*;


import httpserver.itf.HttpRequest;
import httpserver.itf.HttpResponse;

/*
 * This class allows to build an object representing an HTTP static request
 */
public class HttpStaticRequest extends HttpRequest {
	static final String DEFAULT_FILE = "index.html";
	static final Integer CHUNK_LENGTH = 1024;
	
	public HttpStaticRequest(HttpServer hs, String method, String resourceName) throws IOException {
		super(hs, method, resourceName);
	}
	
	public void process(HttpResponse resp) throws Exception {
		String resource = m_resourceName.equals("/")
				? DEFAULT_FILE
				: m_resourceName.replace("/", File.separator);
		try {
			File file = new File(m_hs.getFolder(), resource);
			System.out.println("Requested: "+m_hs.getFolder()+File.separator+resource);
			BufferedInputStream data = new BufferedInputStream(new FileInputStream(file));
			int length = (int) file.length();

			resp.setReplyOk();
			resp.setContentType(getContentType(resource));
			resp.setContentLength(length);

			OutputStream out = resp.beginBody();

			byte[] buffer = new byte[CHUNK_LENGTH];
			int read;

			//reading the entier file
			while ((read = data.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}

			out.close();
			data.close();
		} catch (FileNotFoundException e) {
			resp.setReplyError(404, "Resource not found");
		} catch (Exception e) {
			resp.setReplyError(400, "Unknown resource error");
		}
	}

}
