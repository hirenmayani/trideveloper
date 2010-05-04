package com.trideveloper.ftp;

import java.io.IOException;

public interface DataChannel {

    public void send(Transfer transfer) throws IOException;

    public boolean isClosed();

    public void close();

    public String getConnectionInfo();

}
