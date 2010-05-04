package com.trideveloper.ftp;

import java.net.InetAddress;

public interface FtpSession {

    public InetAddress getEndpoint();

    public Object getAttribute(String name);

    public void setAttribute(String name, Object value);

}
