package com.trideveloper.ftp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface Transfer {

    public boolean isUpload();

    public OutputStream getOutputStream() throws IOException;

    public InputStream getInputStream() throws IOException;

    public int getStatus();

    public void setStatus(int status);

    public String getMessage();

    public void setMessage(String message);

}
