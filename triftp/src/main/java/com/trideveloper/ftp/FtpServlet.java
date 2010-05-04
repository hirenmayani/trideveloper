package com.trideveloper.ftp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.trideveloper.ftp.FtpConstants.*;

public class FtpServlet extends HttpServlet {

    private final ThreadGroup ftpThreadGroup =
            new ThreadGroup(FTP_THREAD_GROUP_NAME);

    private TririgaFactory factory;

    public void init() throws ServletException {
        Properties config = Configuration.getProperties();
        String tririgaServer = config.getProperty(TRIRIGA_SERVER_PROPERTY);
        if (tririgaServer == null) {
            throw new UnavailableException("No TRIRIGA Server Specified.");
        }
        factory = TririgaFactory.newInstance();
        factory.setServer(tririgaServer);
        int port;
        String portString = config.getProperty(FTP_PORT_PROPERTY);
        try {
            port = Integer.parseInt(portString);
        } catch (Exception ex) {
            port = DEFAULT_FTP_PORT;
        }
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            throw new UnavailableException(
                    "Unable to start FTP server on port " + port + ": " +
                            ex.getMessage());
        }
        final ServerSocket listener = serverSocket;
        Runnable server = new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        final Socket socket = listener.accept();
                        Runnable client = new Runnable() {
                            public void run() {
                                try {
                                    OutputStream output =
                                            socket.getOutputStream();
                                    InputStream input =
                                            socket.getInputStream();
                                    FtpConnection connection =
                                            new FtpConnection(factory,
                                                    socket.getInetAddress(),
                                                            input, output);
                                    while (connection.isActive()) {
                                        connection.next();
                                    }
                                    try {
                                        socket.close();
                                    } catch (IOException ignore) { }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        };
                        Thread clientThread = new Thread(ftpThreadGroup,
                                client);
                        clientThread.setDaemon(true);
                        clientThread.start();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }
        };
        Thread serverThread = new Thread(ftpThreadGroup, server);
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void destroy() {
        try {
            ftpThreadGroup.interrupt();
        } catch (Exception ignore) { }
    }

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        response.getWriter().println("triFTP is running.");
    }

}
