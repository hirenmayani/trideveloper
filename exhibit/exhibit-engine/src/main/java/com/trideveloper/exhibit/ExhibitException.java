package com.trideveloper.exhibit;

public class ExhibitException extends Exception {

    public ExhibitException() {
        super();
    }

    public ExhibitException(String message) {
        super(message);
    }

    public ExhibitException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExhibitException(Throwable cause) {
        super(cause);
    }

}
