package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class AborHandler implements CommandHandler {

    public String getName() {
        return ABOR_COMMAND;
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
            if (!dataChannel.isClosed()) dataChannel.close();
            session.setAttribute(DATA_CHANNEL_ATTRIBUTE, null);
        }
        response.setStatus(SC_CLOSING_DATA);
        response.setMessage("Abort successful");
    }

}
