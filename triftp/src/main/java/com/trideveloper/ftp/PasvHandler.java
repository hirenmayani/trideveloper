package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class PasvHandler implements CommandHandler {

    public String getName() {
        return PASV_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        String options = request.getArguments();
        if (options != null && !"".equals(options.trim())) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        FtpSession session = request.getSession();
        DataChannel dataChannel = (DataChannel) 
                session.getAttribute(DATA_CHANNEL_ATTRIBUTE);
        if (dataChannel != null) {
            try {
                dataChannel.close();
            } catch (Exception ignore) { }
            session.setAttribute(DATA_CHANNEL_ATTRIBUTE, null);
        }
        try {
            dataChannel = new PassiveDataChannel(session.getEndpoint());
        } catch (Exception ex) {
            throw new FtpException(SC_LOCAL_ERROR,
                    "Unable to open passive connection");
        }
        session.setAttribute(DATA_CHANNEL_ATTRIBUTE, dataChannel);
        response.setStatus(SC_ENTERING_PASV);
        response.setMessage("Entering Passive Mode (" +
                dataChannel.getConnectionInfo() + ").");
    }

}
