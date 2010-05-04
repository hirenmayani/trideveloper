package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

import static com.trideveloper.ftp.LoginState.LOGIN_USER_ATTRIBUTE;
import static com.trideveloper.ftp.RenameState.RENAME_FILE_ATTRIBUTE;

public class ReinHandler implements CommandHandler {

    public String getName() {
        return REIN_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        FtpSession session = request.getSession();
        session.setAttribute(LOGIN_USER_ATTRIBUTE, null);
        session.setAttribute(RENAME_FILE_ATTRIBUTE, null);
        session.setAttribute(AUTHENTICATED_CREDENTIALS_ATTRIBUTE, null);
        session.setAttribute(WORKING_DIRECTORY_ATTRIBUTE, null);
        session.setAttribute(TRANSFER_MODE_ATTRIBUTE, null);
        DataChannel dataChannel = (DataChannel) 
                session.getAttribute(DATA_CHANNEL_ATTRIBUTE);
        if (dataChannel != null) {
            try {
                dataChannel.close();
            } catch (Exception ignore) { }
            session.setAttribute(DATA_CHANNEL_ATTRIBUTE, null);
        }
        response.setNextState(ConnectedState.class);
        response.setStatus(SC_OK);
        response.setMessage("Reinitialized");
    }

}
