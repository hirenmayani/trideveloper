package com.trideveloper.ftp;

import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.HashMap;
import java.util.Map;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class RenameState extends AbstractFtpState {

    public static final String RENAME_FILE_ATTRIBUTE =
            RenameState.class.getName() + ".sourceFile";

    private static final String STATE_NAME = "Rename";

    private static final Map<String, CommandHandler> HANDLERS =
            new HashMap<String, CommandHandler>();

    static {
        HANDLERS.put(QUIT_COMMAND, new QuitHandler());
        HANDLERS.put(REIN_COMMAND, new ReinHandler());
        HANDLERS.put(RNTO_COMMAND, new RntoHandler());
    }

    public RenameState(IO io, FtpSession session) {
        super(io, session);
    }

    public String getName() {
        return STATE_NAME;
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
            response.setNextState(InteractiveState.class);
            throw new FtpException(SC_BAD_SEQUENCE, "Command " +
                    request.getRequestLine() + " not accepted during " +
                            getName());
        }
        handler.service(request, response);
    }

    protected CommandHandler getCommandHandler(String command) {
        return HANDLERS.get(command);
    }

}
