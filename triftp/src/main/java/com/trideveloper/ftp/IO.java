package com.trideveloper.ftp;

import java.io.IOException;

public interface IO {

    public void println(String data) throws IOException;

    public String readLine() throws IOException;

}
