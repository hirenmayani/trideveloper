package com.trideveloper.ftp;

import java.io.IOException;

import java.lang.reflect.Constructor;

import java.net.InetAddress;

import java.util.HashMap;
import java.util.Map;

import static com.trideveloper.ftp.FtpCommand.*;

public class ConnectedState extends AbstractFtpState {

    private static final String STATE_NAME = "Connected";

    private static final String HELLO_SENT_ATTRIBUTE =
            ConnectedState.class.getName() + ".helloSent";

    private static final Map<String, CommandHandler> HANDLERS =
            new HashMap<String, CommandHandler>();

    static {
        HANDLERS.put(QUIT_COMMAND, new QuitHandler());
        HANDLERS.put(REIN_COMMAND, new ReinHandler());
        HANDLERS.put(USER_COMMAND, new UserHandler());
    }

    public ConnectedState(IO io, FtpSession session) {
        super(io, session);
    }

    public String getName() {
        return STATE_NAME;
    }

    public FtpState next() throws IOException {
        boolean helloSent = Boolean.parseBoolean((String)
                session.getAttribute(HELLO_SENT_ATTRIBUTE));
        if (!helloSent) {
            session.setAttribute(HELLO_SENT_ATTRIBUTE, "true");
            io.println("220 " + InetAddress.getLocalHost().getHostName() +
                    " triFTP");
        }
        return super.next();
    }

    protected CommandHandler getCommandHandler(String command) {
        return HANDLERS.get(command);
    }

}
