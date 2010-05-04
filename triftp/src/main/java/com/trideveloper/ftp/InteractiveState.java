package com.trideveloper.ftp;

import java.lang.reflect.Constructor;

import java.util.HashMap;
import java.util.Map;

import static com.trideveloper.ftp.FtpCommand.*;

public class InteractiveState extends AbstractFtpState {

    private static final String STATE_NAME = "Interactive";

    private static final Map<String, CommandHandler> HANDLERS =
            new HashMap<String, CommandHandler>();

    static {
        HANDLERS.put(QUIT_COMMAND, new QuitHandler());
        HANDLERS.put(OPTS_COMMAND, new OptsHandler());
        HANDLERS.put(SYST_COMMAND, new SystHandler());
        HANDLERS.put(SITE_COMMAND, new SiteHandler());
        HANDLERS.put(PWD_COMMAND, new PwdHandler());
        HANDLERS.put(TYPE_COMMAND, new TypeHandler());
        HANDLERS.put(PASV_COMMAND, new PasvHandler());
        HANDLERS.put(PORT_COMMAND, new PortHandler());
        HANDLERS.put(LIST_COMMAND, new ListHandler());
        HANDLERS.put(NLST_COMMAND, new NlstHandler());
        HANDLERS.put(NOOP_COMMAND, new NoopHandler());
        HANDLERS.put(CWD_COMMAND, new CwdHandler());
        HANDLERS.put(SIZE_COMMAND, new SizeHandler());
        HANDLERS.put(RETR_COMMAND, new RetrHandler());
        HANDLERS.put(ABOR_COMMAND, new AborHandler());
        HANDLERS.put(DELE_COMMAND, new DeleHandler());
        HANDLERS.put(MDTM_COMMAND, new MdtmHandler());
        HANDLERS.put(MKD_COMMAND, new MkdHandler());
        HANDLERS.put(RMD_COMMAND, new RmdHandler());
        HANDLERS.put(RNFR_COMMAND, new RnfrHandler());
        HANDLERS.put(REIN_COMMAND, new ReinHandler());
        HANDLERS.put(STOR_COMMAND, new StorHandler());
    }

    public InteractiveState(IO io, FtpSession session) {
        super(io, session);
    }

    public String getName() {
        return STATE_NAME;
    }

    protected CommandHandler getCommandHandler(String command) {
        return HANDLERS.get(command);
    }

}
