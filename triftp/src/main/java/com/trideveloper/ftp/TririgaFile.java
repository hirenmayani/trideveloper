package com.trideveloper.ftp;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface TririgaFile {

    public InputStream getInputStream() throws IOException;

    public OutputStream getOutputStream() throws IOException;

    public String getName();

    public String getParent();

    public TririgaFile[] listFiles() throws IOException;

    public long length() throws IOException;

    public long lastModified() throws IOException;

    public void copyTo(TririgaFile dest) throws IOException;

    public void moveTo(TririgaFile dest) throws IOException;

    public boolean delete() throws IOException;

    public boolean isFile() throws IOException;

    public boolean isDirectory() throws IOException;

    public boolean exists() throws IOException;

    public void createNewFile() throws IOException;

    public void mkdir() throws IOException;

    public String toString();

}
