package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

import static com.trideveloper.ftp.LoginState.LOGIN_USER_ATTRIBUTE;

public class PassHandler implements CommandHandler {

    public String getName() {
        return PASS_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        FtpSession session = request.getSession();
        String username = (String) session.getAttribute(LOGIN_USER_ATTRIBUTE);
        if (username == null) {
            throw new FtpException(SC_BAD_SEQUENCE, getName() +
                    " invalid without user");
        }
        String password = request.getArguments();
        if (password == null) password = "";
        TririgaFactory factory = (TririgaFactory)
                session.getAttribute(TRIRIGA_FACTORY_ATTRIBUTE);
        if (factory == null) {
            throw new FtpException(SC_LOCAL_ERROR, "Command " + getName() +
                    " failed (internal error)");
        }
        TririgaCredentials credentials =
                new TririgaCredentials(username, password);
        try {
            credentials = factory.authenticate(credentials);
        } catch (TririgaAuthenticationException ex) {
            response.setNextState(ConnectedState.class);
            throw new FtpException(SC_NOT_LOGGED_IN,
                    "Invalid username/password");
        }
        session.setAttribute(LOGIN_USER_ATTRIBUTE, null);
        session.setAttribute(AUTHENTICATED_CREDENTIALS_ATTRIBUTE, credentials);
        session.setAttribute(WORKING_DIRECTORY_ATTRIBUTE, "/");
        response.setNextState(InteractiveState.class);
        response.setStatus(SC_LOGGED_IN);
        response.setMessage("User logged in");
    }

}
