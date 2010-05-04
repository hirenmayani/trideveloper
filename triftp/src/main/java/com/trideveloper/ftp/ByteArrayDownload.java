package com.trideveloper.ftp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteArrayDownload implements Transfer {

    private final byte[] data;

    private int status;

    private String message;

    public ByteArrayDownload(byte[] data) throws IOException {
        this.data = data;
    }

    public boolean isUpload() {
        return false;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Upload prohibited.");
    }

    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(data);
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
