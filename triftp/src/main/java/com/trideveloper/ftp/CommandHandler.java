package com.trideveloper.ftp;

import java.io.IOException;

public interface CommandHandler {

    public String getName();

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException;

}
