package com.trideveloper.ftp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TririgaFileDownload implements Transfer {

    private final TririgaFile file;

    private int status;

    private String message;

    public TririgaFileDownload(TririgaFile file) throws IOException {
        this.file = file;
    }

    public boolean isUpload() {
        return false;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Upload prohibited.");
    }

    public InputStream getInputStream() throws IOException {
        return file.getInputStream();
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
