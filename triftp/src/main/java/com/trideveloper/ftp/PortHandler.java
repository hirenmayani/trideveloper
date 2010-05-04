package com.trideveloper.ftp;

import java.io.IOException;

import java.net.InetAddress;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

public class PortHandler implements CommandHandler {

    public String getName() {
        return PORT_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        String options = request.getArguments();
        if (options == null || "".equals(options.trim())) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        InetAddress address = null;
        int port = -1;
        try {
            options = options.replaceFirst(
                    "([0-9]*),([0-9]*),([0-9]*),([0-9]*),([0-9]*),([0-9]*)",
                            "$1.$2.$3.$4:$5.$6");
            int index = options.indexOf(':');
            String[] octets = options.substring(0, index).split("\\.");
            String portInfo = options.substring(index + 1);
            index = portInfo.indexOf('.');
            port = Integer.parseInt(portInfo.substring(0, index)) * 256 +
                    Integer.parseInt(portInfo.substring(index + 1));
            byte[] bytes = new byte[octets.length];
            for (int i = 0; i < octets.length; i++) {
                bytes[i] = (byte) (Integer.parseInt(octets[i]) & 0xff);
            }
            address = InetAddress.getByAddress(bytes);
        } catch (Exception ex) {
            // handle errors in validation
        }
        if (address == null || port < 1 || port > 65535) {
            throw new FtpException(SC_NOT_FOUND, "Command " + getName() +
                    " failed");
        }
        FtpSession session = request.getSession();
        DataChannel dataChannel = (DataChannel) 
                session.getAttribute(DATA_CHANNEL_ATTRIBUTE);
        if (dataChannel != null) {
            try {
                dataChannel.close();
            } catch (Exception ignore) { }
            session.setAttribute(DATA_CHANNEL_ATTRIBUTE, null);
        }
        try {
            dataChannel = new ActiveDataChannel(session.getEndpoint(),
                    address, port);
        } catch (Exception ex) {
            throw new FtpException(SC_LOCAL_ERROR,
                    "Unable to open active connection");
        }
        session.setAttribute(DATA_CHANNEL_ATTRIBUTE, dataChannel);
        response.setStatus(SC_OK);
        response.setMessage("Command " + getName() + " succeed");
    }

}
