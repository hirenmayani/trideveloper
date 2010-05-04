package com.trideveloper.ftp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TririgaFileUpload implements Transfer {

    private final TririgaFile file;

    private int status;

    private String message;

    public TririgaFileUpload(TririgaFile file) throws IOException {
        this.file = file;
    }

    public boolean isUpload() {
        return true;
    }

    public OutputStream getOutputStream() throws IOException {
        return file.getOutputStream();
    }

    public InputStream getInputStream() throws IOException {
        throw new IOException("Download prohibited.");
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
