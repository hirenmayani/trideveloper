package com.trideveloper.ftp;

public interface FtpStatus {

    public static final int SC_RESTART_MARKER_REPLY = 110;

    public static final int SC_DELAY = 120;

    public static final int SC_TRANSFER_STARTING = 125;

    public static final int SC_OPENING_DATA = 150;

    public static final int SC_OK = 200;

    public static final int SC_NOT_IMPLEMENTED_SUPERFLUOUS = 202;

    public static final int SC_SYSTEM_STATUS = 211;

    public static final int SC_DIRECTORY_STATUS = 212;

    public static final int SC_FILE_STATUS = 213;

    public static final int SC_HELP = 214;

    public static final int SC_SYSTEM_TYPE = 215;

    public static final int SC_SERVICE_READY = 220;

    public static final int SC_CLOSING_CONTROL = 221;

    public static final int SC_DATA_OPEN = 225;

    public static final int SC_CLOSING_DATA = 226;

    public static final int SC_ENTERING_PASV = 227;

    public static final int SC_LOGGED_IN = 230;

    public static final int SC_FILE_ACTION_COMPLETED = 250;

    public static final int SC_CREATED = 257;

    public static final int SC_USER_OK_NEED_PASSWORD = 331;

    public static final int SC_NEED_ACCOUNT_LOGIN = 332;

    public static final int SC_NEED_MORE_INFORMATION = 350;

    public static final int SC_SERVICE_NOT_AVAILABLE = 421;

    public static final int SC_CANNOT_OPEN_DATA = 425;

    public static final int SC_CLOSED_TRANSFER_ABORTED = 426;

    public static final int SC_FILE_BUSY = 450;

    public static final int SC_LOCAL_ERROR = 451;

    public static final int SC_INSUFFICIENT_STORAGE = 452;

    public static final int SC_SYNTAX_ERROR_COMMAND = 500;

    public static final int SC_SYNTAX_ERROR_ARGUMENTS = 501;

    public static final int SC_NOT_IMPLEMENTED = 502;

    public static final int SC_BAD_SEQUENCE = 503;

    public static final int SC_NOT_IMPLEMENTED_PARAMETER = 504;

    public static final int SC_NOT_LOGGED_IN = 530;

    public static final int SC_NEED_ACCOUNT_STORAGE = 532;

    public static final int SC_NOT_FOUND = 550;

    public static final int SC_PAGE_TYPE_UNKNOWN = 551;

    public static final int SC_EXCEEDED_STORAGE = 552;

    public static final int SC_FILENAME_NOT_ALLOWED = 553;

}
