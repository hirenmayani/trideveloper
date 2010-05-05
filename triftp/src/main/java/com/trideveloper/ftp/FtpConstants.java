package com.trideveloper.ftp;

interface FtpConstants {

    public static final String FTP_THREAD_GROUP_NAME = "triFTP";

    public static final String TRIRIGA_SERVER_PROPERTY = "tririgaServer";

    public static final String FTP_PORT_PROPERTY = "ftpPort";

    public static final String FTP_GROUP_PROPERTY = "ftpGroup";

    public static final String GLOBAL_ROOT_PROPERTY = "globalRoot";

    public static final String PASSIVE_PORT_RANGE_PROPERTY = "passivePortRange";

    public static final String DEFAULT_FTP_GROUP = "Admin Group";

    public static final int DEFAULT_FTP_PORT = 21;

    public static final String TRIRIGA_FACTORY_ATTRIBUTE =
            FtpConnection.class.getName() + ".tririgaFactory";

    public static final String AUTHENTICATED_CREDENTIALS_ATTRIBUTE =
            FtpConnection.class.getName() + ".credentials";

    public static final String WORKING_DIRECTORY_ATTRIBUTE =
            FtpConnection.class.getName() + ".workingDirectory";

    public static final String TRANSFER_MODE_ATTRIBUTE =
            FtpConnection.class.getName() + ".transferMode";

    public static final String DATA_CHANNEL_ATTRIBUTE =
            FtpConnection.class.getName() + ".dataChannel";

}
