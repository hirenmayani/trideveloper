package com.trideveloper.ftp;

import java.io.IOException;

import java.lang.reflect.Constructor;

import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public abstract class AbstractFtpState implements FtpState {

    protected IO io;

    protected FtpSession session;

    private boolean active = true;

    public AbstractFtpState(IO io, FtpSession session) {
        this.io = io;
        this.session = session;
    }

    public boolean isActive() {
        return active;
    }

    protected void setActive(boolean active) {
        this.active = active;
    }

    public FtpState next() throws IOException {
        String command = null;
        String args = null;
        String line = io.readLine();
        if (line != null) {
            int index = line.indexOf(' ');
            if (index != -1) {
                command = line.substring(0, index).toUpperCase();
                args = line.substring(index + 1);
            } else {
                command = line.toUpperCase();
            }
            if ("".equals(command.trim())) command = null;
        } else {
            setActive(false);
            return null;
        }
        final String ftpCommand = command;
        final String ftpArgs = args;
        final String ftpLine = line;
        FtpRequest request = new FtpRequest() {
            public String getCommand() {
                return ftpCommand;
            }
            public String getArguments() {
                return ftpArgs;
            }
            public String getRequestLine() {
                return ftpLine;
            }
            public FtpSession getSession() {
                return AbstractFtpState.this.session;
            }
        };
        FtpResponse response = new FtpResponse() {
            private int status = -1;
            private String message = null;
            private Class nextState = null;
            public int getStatus() {
                return status;
            }
            public void setStatus(int status) {
                this.status = status;
            }
            public String getMessage() {
                return message;
            }
            public void setMessage(String message) {
                this.message = message;
            }
            public Class getNextState() {
                return nextState;
            }
            public void setNextState(Class nextState) {
                this.nextState = nextState;
            }
            public void println(String info) throws IOException {
                AbstractFtpState.this.io.println(info);
            }
        };
        FtpState nextState = null;
        try {
            service(request, response);
            Class nextStateClass = response.getNextState();
            if (nextStateClass != null) {
                try {
                    Constructor constructor = nextStateClass.getConstructor(
                            IO.class, FtpSession.class);
                    nextState = (FtpState) constructor.newInstance(io, session);
                } catch (Exception ex) {
                    throw new FtpException(SC_LOCAL_ERROR, "Internal error");
                }
            }
            int status = response.getStatus();
            if (status > 0) {
                String message = response.getMessage();
                if (message == null) message = "";
                io.println(status + " " + message);
            }
        } catch (FtpException ex) {
            Class nextStateClass = response.getNextState();
            if (nextStateClass != null) {
                try {
                    Constructor constructor = nextStateClass.getConstructor(
                            IO.class, FtpSession.class);
                    nextState = (FtpState) constructor.newInstance(io, session);
                } catch (Exception e) {
                    ex = new FtpException(SC_LOCAL_ERROR, "Internal error");
                }
            }
            io.println(ex.toString());
        }
        return nextState;
    }

    protected void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        CommandHandler handler = getCommandHandler(request.getCommand());
        if (handler == null) {
            DataChannel dataChannel = (DataChannel) 
                    session.getAttribute(DATA_CHANNEL_ATTRIBUTE);
            if (dataChannel != null) {
                try {
                    dataChannel.close();
                } catch (Exception ignore) { }
                session.setAttribute(DATA_CHANNEL_ATTRIBUTE, null);
            }
            throw new FtpException(SC_BAD_SEQUENCE, "Command " +
                    request.getRequestLine() + " not accepted during " +
                            getName());
        }
        handler.service(request, response);
    }

    protected CommandHandler getCommandHandler(String command) {
        return null;
    }

    public abstract String getName();

}
