package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class OptsHandler implements CommandHandler {

    public String getName() {
        return OPTS_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        String options = request.getArguments();
        if (options == null || "".equals(options)) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        if (!options.trim().equalsIgnoreCase("UTF8 ON")) {
            throw new FtpException(SC_NOT_FOUND, "Command " + getName() +
                    " failed");
        }
        response.setStatus(SC_OK);
        response.setMessage("Command " + getName() + " succeed");
    }

}
