package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class CwdHandler implements CommandHandler {

    public String getName() {
        return CWD_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        String path = request.getArguments();
        if (path == null || "".equals(path.trim())) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        FtpSession session = request.getSession();
        if (!path.startsWith("/")) {
            String wd = (String)
                    session.getAttribute(WORKING_DIRECTORY_ATTRIBUTE);
            if (wd == null) wd = "";
            if (wd.endsWith("/")) wd = wd.substring(0, wd.length() - 1);
            path = wd + "/" + path;
        }
        TririgaCredentials credentials = (TririgaCredentials)
                session.getAttribute(AUTHENTICATED_CREDENTIALS_ATTRIBUTE);
        if (credentials == null) {
            throw new FtpException(SC_LOCAL_ERROR, "Command " + getName() +
                    " failed (internal error)");
        }
        TririgaFactory factory = (TririgaFactory)
                session.getAttribute(TRIRIGA_FACTORY_ATTRIBUTE);
        if (factory == null) {
            throw new FtpException(SC_LOCAL_ERROR, "Command " + getName() +
                    " failed (internal error)");
        }
        TririgaFile file = factory.createFile(path, credentials);
        if (!file.isDirectory()) {
            throw new FtpException(SC_NOT_FOUND, "Folder " + path +
                    " not found");
        }
        session.setAttribute(WORKING_DIRECTORY_ATTRIBUTE, path);
        response.setStatus(SC_FILE_ACTION_COMPLETED);
        response.setMessage("Command " + getName() + " succeed");
    }

}
