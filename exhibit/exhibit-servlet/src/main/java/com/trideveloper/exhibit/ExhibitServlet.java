package com.trideveloper.exhibit;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;

import java.net.URL;

import javax.activation.DataSource;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ExhibitServlet extends HttpServlet {

    private static final String DATA_REQUEST_PARAMETER = "data";

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        try {
            String path = request.getPathInfo();
            boolean hasDataPath = false;
            if (path != null) {
                int index = path.lastIndexOf("/");
                if (index != -1 && DATA_REQUEST_PARAMETER.equalsIgnoreCase(
                        path.substring(index + 1))) {
                    path = path.substring(0, index);
                    hasDataPath = true;
                }
                path = path.replaceAll("^/*", "").replaceAll("/*$", "");
                if ("".equals(path = path.trim())) path = null;
            }
            if (path == null) {
                throw new ExhibitException("No exhibit name was provided.");
            }
            boolean dataRequest = hasDataPath || Boolean.valueOf(
                    request.getParameter(DATA_REQUEST_PARAMETER));
            Exhibit exhibit = null;
            ExhibitEngine engine = null;
            try {
                engine = ExhibitEngine.getInstance();
            } catch (NoServerException ex) {
                String server = null;
                try {
                    server = request.getRequestURL().toString();
                    server = new URL(new URL(server), "/").toExternalForm();
                } catch (Exception ignore) {
                    throw new ExhibitException(
                            "Unable to identify a suitable Exhibit server.");
                }
                engine = ExhibitEngine.getInstance(server);
            }
            exhibit = engine.getExhibit(path);
            if (exhibit == null) {
                throw new ExhibitException("No exhibit found with name \"" +
                        path + "\".");
            }
            if (dataRequest) {
                String data = exhibit.getData();
                response.setContentType("application/json");
                byte[] buffer = data.getBytes("UTF-8");
                response.setContentLength(buffer.length);
                OutputStream output = response.getOutputStream();
                output.write(buffer);
                output.flush();
                output.close();
            } else {
                DataSource source = exhibit.getDocument();
                InputStream input = source.getInputStream();
                Reader reader = new InputStreamReader(input, "UTF-8");
                char[] buffer = new char[65536];
                int count;
                PrintWriter writer = response.getWriter();
                while ((count = reader.read(buffer, 0, 65536)) != -1) {
                    writer.write(buffer, 0, count);
                }
                writer.flush();
                reader.close();
                writer.close();
            }
        } catch (ExhibitException ex) {
            String message = ex.getMessage();
            if (message == null) message = "Unable to process the request.";
            response.getWriter().println(message);
            response.flushBuffer();
        }
    }

}
