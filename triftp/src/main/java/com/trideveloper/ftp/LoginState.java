package com.trideveloper.ftp;

import java.lang.reflect.Constructor;

import java.util.HashMap;
import java.util.Map;

import static com.trideveloper.ftp.FtpCommand.*;

public class LoginState extends AbstractFtpState {

    public static final String LOGIN_USER_ATTRIBUTE =
            LoginState.class.getName() + ".user";

    private static final String STATE_NAME = "Login";

    private static final Map<String, CommandHandler> HANDLERS =
            new HashMap<String, CommandHandler>();

    static {
        HANDLERS.put(QUIT_COMMAND, new QuitHandler());
        HANDLERS.put(REIN_COMMAND, new ReinHandler());
        HANDLERS.put(PASS_COMMAND, new PassHandler());
    }

    public LoginState(IO io, FtpSession session) {
        super(io, session);
    }

    public String getName() {
        return STATE_NAME;
    }

    protected CommandHandler getCommandHandler(String command) {
        return HANDLERS.get(command);
    }

}
