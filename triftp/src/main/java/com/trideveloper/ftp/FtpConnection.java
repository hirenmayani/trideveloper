package com.trideveloper.ftp;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.net.InetAddress;

import java.util.HashMap;
import java.util.Map;

import static com.trideveloper.ftp.FtpConstants.*;

public class FtpConnection {

    private final TririgaFactory factory;

    private final InetAddress endpoint;

    private final BufferedReader reader;

    private final Writer writer;

    private final Map<String, Object> sessionMap =
            new HashMap<String, Object>();

    private FtpState state;

    public FtpConnection(TririgaFactory factory, InetAddress endpoint,
            InputStream input, OutputStream output) throws IOException {
        this.factory = factory;
        this.endpoint = endpoint;
        this.reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        this.writer = new OutputStreamWriter(output, "UTF-8");
        IO io = new IO() {
            public void println(String data) throws IOException {
                FtpConnection.this.writer.write(data);
                FtpConnection.this.writer.write("\r\n");
                FtpConnection.this.writer.flush();
            }
            public String readLine() throws IOException {
                return FtpConnection.this.reader.readLine();
            }
        };
        FtpSession session = new FtpSession() {
            public InetAddress getEndpoint() {
                return FtpConnection.this.endpoint;
            }
            public Object getAttribute(String name) {
                synchronized (FtpConnection.this.sessionMap) {
                    return FtpConnection.this.sessionMap.get(name);
                }
            }
            public void setAttribute(String name, Object value) {
                synchronized (FtpConnection.this.sessionMap) {
                    if (value != null) {
                        FtpConnection.this.sessionMap.put(name, value);
                    } else {
                        FtpConnection.this.sessionMap.remove(name);
                    }
                }
            }
        };
        session.setAttribute(TRIRIGA_FACTORY_ATTRIBUTE, factory);
        this.state = new ConnectedState(io, session);
    }

    public boolean isActive() {
        return state.isActive();
    }

    public void next() throws IOException {
        FtpState nextState = state.next();
        if (nextState != null) state = nextState;
    }

    public void close() {
        try {
            writer.close();
        } catch (Exception ignore) { }
        try {
            reader.close();
        } catch (Exception ignore) { }
        DataChannel dataChannel = null;
        synchronized (sessionMap) {
            dataChannel = (DataChannel) sessionMap.get(DATA_CHANNEL_ATTRIBUTE);
            sessionMap.clear();
        }
        if (dataChannel != null) {
            try {
                dataChannel.close();
            } catch (Exception ignore) { }
        }
    }

}
