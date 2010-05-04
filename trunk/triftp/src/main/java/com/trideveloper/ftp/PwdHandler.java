package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class PwdHandler implements CommandHandler {

    public String getName() {
        return PWD_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        String options = request.getArguments();
        if (options != null && !"".equals(options.trim())) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        FtpSession session = request.getSession();
        String workingDirectory = (String)
                session.getAttribute(WORKING_DIRECTORY_ATTRIBUTE);
        if (workingDirectory == null) workingDirectory = "/";
        response.setStatus(SC_CREATED);
        response.setMessage("\"" + workingDirectory +
                "\" is current directory");
    }

}
