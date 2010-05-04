package com.trideveloper.ftp;

public interface FtpRequest {

    public String getCommand();

    public String getArguments();

    public String getRequestLine();

    public FtpSession getSession();

}
