package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class TypeHandler implements CommandHandler {

    public String getName() {
        return TYPE_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        FtpSession session = request.getSession();
        String type = request.getArguments();
        if (type == null || "".equals(type)) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        type = type.trim().toUpperCase();
        if (type.equals("A")) {
            session.setAttribute(TRANSFER_MODE_ATTRIBUTE, TransferMode.ASCII);
        } else if (type.equals("I")) {
            session.setAttribute(TRANSFER_MODE_ATTRIBUTE, TransferMode.BINARY);
        } else {
            throw new FtpException(SC_NOT_IMPLEMENTED,
                    "Transfer mode " + type + " not supported");
        }
        response.setStatus(SC_OK);
        response.setMessage("Transfer mode set to " +
                session.getAttribute(TRANSFER_MODE_ATTRIBUTE));
    }

}
