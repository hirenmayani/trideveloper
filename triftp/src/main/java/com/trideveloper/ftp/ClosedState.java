package com.trideveloper.ftp;

import java.io.IOException;

public class ClosedState implements FtpState {

    private static final String STATE_NAME = "Closed";

    public ClosedState(IO io, FtpSession session) { }

    public String getName() {
        return STATE_NAME;
    }

    public boolean isActive() {
        return false;
    }

    public FtpState next() throws IOException {
        return null;
    }

}
