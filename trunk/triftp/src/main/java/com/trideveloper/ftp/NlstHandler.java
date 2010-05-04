package com.trideveloper.ftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class NlstHandler implements CommandHandler {

    public String getName() {
        return NLST_COMMAND;
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
            boolean expandDirectory = false;
            if (path == null || "".equals(path.trim())) {
                expandDirectory = true;
                path = null;
            }
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
            ByteArrayOutputStream collector = new ByteArrayOutputStream();
            Writer writer = new OutputStreamWriter(collector, "UTF-8");
            if (file.isDirectory() && expandDirectory) {
                for (TririgaFile child : file.listFiles()) {
                    writer.write(child.getName() + "\r\n");
                }
            } else {
                writer.write(file.getName() + "\r\n");
            }
            writer.flush();
            response.println(SC_TRANSFER_STARTING + " Transferring directory");
            Transfer transfer = new ByteArrayDownload(collector.toByteArray());
            dataChannel.send(transfer);
            response.setStatus(transfer.getStatus());
            response.setMessage(transfer.getMessage());
        } finally {
            if (!dataChannel.isClosed()) dataChannel.close();
            session.setAttribute(DATA_CHANNEL_ATTRIBUTE, null);
        }
    }

}
