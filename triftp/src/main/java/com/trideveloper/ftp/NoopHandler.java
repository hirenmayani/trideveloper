package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class NoopHandler implements CommandHandler {

    public String getName() {
        return NOOP_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        String options = request.getArguments();
        if (options != null && !"".equals(options.trim())) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        response.setStatus(SC_OK);
        response.setMessage("Command " + getName() + " succeed");
    }

}
