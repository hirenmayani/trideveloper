package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

import static com.trideveloper.ftp.RenameState.RENAME_FILE_ATTRIBUTE;

public class RntoHandler implements CommandHandler {

    public String getName() {
        return RNTO_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        FtpSession session = request.getSession();
        try {
            String path = request.getArguments();
            if (path == null || "".equals(path)) {
                throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                        "Invalid number of arguments " + getName());
            }
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
            if (file.exists()) {
                throw new FtpException(SC_NOT_FOUND, path + " already exists");
            }
            TririgaFile sourceFile = (TririgaFile)
                    session.getAttribute(RENAME_FILE_ATTRIBUTE);
            if (!sourceFile.exists()) {
                throw new FtpException(SC_NOT_FOUND, path + " not found");
            }
            try {
                sourceFile.moveTo(file);
            } catch (Exception ex) {
                throw new FtpException(SC_NOT_FOUND, "Permission denied");
            }
            response.setStatus(SC_FILE_ACTION_COMPLETED);
            response.setMessage("Renamed");
        } finally {
            session.setAttribute(RENAME_FILE_ATTRIBUTE, null);
            response.setNextState(InteractiveState.class);
        }
    }

}
