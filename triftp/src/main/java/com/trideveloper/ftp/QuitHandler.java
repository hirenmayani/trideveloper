package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class QuitHandler implements CommandHandler {

    public String getName() {
        return QUIT_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        response.setNextState(ClosedState.class);
        response.setStatus(SC_CLOSING_CONTROL);
        response.setMessage("bye");
    }

}
