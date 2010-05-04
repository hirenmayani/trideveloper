package com.trideveloper.ftp;

import java.io.IOException;

public interface FtpState {

    public boolean isActive();

    public FtpState next() throws IOException;

    public String getName();

}
