package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class RetrHandler implements CommandHandler {

    public String getName() {
        return RETR_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        FtpSession session = request.getSession();
        DataChannel dataChannel = (DataChannel) 
                session.getAttribute(DATA_CHANNEL_ATTRIBUTE);
        if (dataChannel == null) {
            throw new FtpException(SC_NOT_FOUND, "Command " + getName() +
                    " failed");
        }
        try {
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
            if (!file.exists()) {
                throw new FtpException(SC_NOT_FOUND, "File " + path +
                        " not found");
            }
            if (!file.isFile()) {
                throw new FtpException(SC_NOT_FOUND, "Permission denied");
            }
            response.println(SC_TRANSFER_STARTING +
                    " Downloading in BINARY file " + path + " (" +
                            file.length() + ")");
            Transfer transfer = new TririgaFileDownload(file);
            dataChannel.send(transfer);
            response.setStatus(transfer.getStatus());
            response.setMessage(transfer.getMessage());
        } finally {
            if (!dataChannel.isClosed()) dataChannel.close();
            session.setAttribute(DATA_CHANNEL_ATTRIBUTE, null);
        }
    }

}
