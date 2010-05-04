package com.trideveloper.ftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class ListHandler implements CommandHandler {

    private static final long MILLISECONDS_IN_DAY = 1000l * 60l * 60l * 24l;

    public String getName() {
        return LIST_COMMAND;
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
                writer.write(getListingLine(file, "."));
                String parent = file.getParent();
                if (parent != null && !"".equals(parent)) {
                    writer.write(getListingLine(
                            factory.createFile(parent, credentials), ".."));
                }
                for (TririgaFile child : file.listFiles()) {
                    writer.write(getListingLine(child, null));
                }
            } else {
                writer.write(getListingLine(file, null));
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

    private String getListingLine(TririgaFile file, String name)
            throws IOException {
        String[] fields = new String[9];
        long lastModified = file.lastModified();
        boolean isOld = lastModified < (System.currentTimeMillis() -
                (270 * MILLISECONDS_IN_DAY));
        DateFormat format = new SimpleDateFormat("MMM\tdd\tyyyy\tHH:mm");
        String[] dateInfo = format.format(new Date(lastModified)).split("\t");
        if (file.isDirectory()) {
            fields[0] = "drwxr-xr-x";
            fields[1] = "1";
            fields[2] = "owner";
            fields[3] = "group";
            fields[4] = "0";
            fields[5] = dateInfo[0];
            fields[6] = dateInfo[1];
            fields[7] = isOld ? dateInfo[2] : dateInfo[3];
            fields[8] = (name != null) ? name : file.getName();
        } else {
            fields[0] = "-rwxr-xr-x";
            fields[1] = "1";
            fields[2] = "owner";
            fields[3] = "group";
            fields[4] = String.valueOf(file.length());
            fields[5] = dateInfo[0];
            fields[6] = dateInfo[1];
            fields[7] = isOld ? dateInfo[2] : dateInfo[3];
            fields[8] = (name != null) ? name : file.getName();
        }
        StringBuilder collector = new StringBuilder();
        collector.append(fields[0]);
        collector.append(" ");
        collector.append(fields[1]);
        collector.append(" ");
        collector.append(fields[2]);
        collector.append(" ");
        collector.append(fields[3]);
        collector.append(" ");
        collector.append("             ");
        collector.replace(collector.length() - fields[4].length(),
                collector.length(), fields[4]);
        collector.append(" ");
        collector.append(fields[5]);
        collector.append(" ");
        collector.append(fields[6]);
        collector.append("     ");
        collector.replace(collector.length() - fields[7].length(),
                collector.length(), fields[7]);
        collector.append(" ");
        collector.append(fields[8]);
        collector.append("\r\n");
        return collector.toString();
    }

}
