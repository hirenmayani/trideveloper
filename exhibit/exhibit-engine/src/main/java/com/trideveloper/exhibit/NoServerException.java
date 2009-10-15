package com.trideveloper.exhibit;

public class NoServerException extends ExhibitException {

    public NoServerException() {
        super();
    }

    public NoServerException(String message) {
        super(message);
    }

    public NoServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoServerException(Throwable cause) {
        super(cause);
    }

}
