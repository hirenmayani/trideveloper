package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class SiteHandler implements CommandHandler {

    public String getName() {
        return SITE_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        String options = request.getArguments();
        if (options == null || "".equals(options)) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        options = options.trim().toUpperCase();
        if (!options.equals("HELP")) {
            throw new FtpException(SC_NOT_FOUND, "Command " + getName() +
                    " failed");
        }
        response.println(SC_HELP +
                "-The following SITE commands are supported");
        response.println("    HELP");
        response.setStatus(SC_HELP);
        response.setMessage(getName() + " command successful");
    }

}
