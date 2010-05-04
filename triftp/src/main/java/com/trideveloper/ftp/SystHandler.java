package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class SystHandler implements CommandHandler {

    public String getName() {
        return SYST_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        String options = request.getArguments();
        if (options != null && !"".equals(options.trim())) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        response.setStatus(SC_SYSTEM_TYPE);
        response.setMessage("UNIX");
    }

}
