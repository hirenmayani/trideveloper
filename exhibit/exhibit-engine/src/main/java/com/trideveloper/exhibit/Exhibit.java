package com.trideveloper.exhibit;

import java.io.IOException;
import java.io.Writer;

import javax.activation.DataSource;

public interface Exhibit {

    public String getData() throws IOException;

    public DataSource getDocument() throws IOException;

}
