package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class MkdHandler implements CommandHandler {

    public String getName() {
        return MKD_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        FtpSession session = request.getSession();
        String path = request.getArguments();
        if (path == null) {
            path = (String)
                    session.getAttribute(WORKING_DIRECTORY_ATTRIBUTE);
            if (path == null) path = "/";
        } else if (!path.startsWith("/")) {
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
        if (file.isDirectory()) {
            throw new FtpException(SC_NOT_FOUND, "Folder " + path +
                    " already exists");
        }
        if (file.exists()) {
            throw new FtpException(SC_NOT_FOUND, "Permission denied");
        }
        try {
            file.mkdir();
        } catch (Exception ex) {
            throw new FtpException(SC_NOT_FOUND, "Unable to create directory");
        }
        response.setStatus(SC_CREATED);
        response.setMessage(path + " created.");
    }

}
