package com.trideveloper.ftp;

public class FtpException extends Exception {

    private final int status;

    public FtpException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public String toString() {
        String status = String.valueOf(getStatus());
        String message = getMessage();
        return (message != null) ? (status + " " + message) : status;
    }

}
