package com.trideveloper.ftp;

import java.io.IOException;

public interface FtpResponse {

    public int getStatus();

    public void setStatus(int status);

    public String getMessage();

    public void setMessage(String message);

    public void setNextState(Class nextState);

    public Class getNextState();

    public void println(String info) throws IOException;

}
