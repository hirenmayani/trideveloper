package com.trideveloper.ftp;

import java.io.IOException;

import static com.trideveloper.ftp.FtpCommand.*;
import static com.trideveloper.ftp.FtpConstants.*;
import static com.trideveloper.ftp.FtpStatus.*;

import static com.trideveloper.ftp.LoginState.LOGIN_USER_ATTRIBUTE;

public class UserHandler implements CommandHandler {

    public String getName() {
        return USER_COMMAND;
    }

    public void service(FtpRequest request, FtpResponse response)
            throws IOException, FtpException {
        FtpSession session = request.getSession();
        session.setAttribute(AUTHENTICATED_CREDENTIALS_ATTRIBUTE, null);
        String user = request.getArguments();
        if (user == null || "".equals(user)) {
            throw new FtpException(SC_SYNTAX_ERROR_ARGUMENTS,
                    "Invalid number of arguments " + getName());
        }
        session.setAttribute(LOGIN_USER_ATTRIBUTE, user);
        response.setNextState(LoginState.class);
        response.setStatus(SC_USER_OK_NEED_PASSWORD);
        response.setMessage("Enter password");
    }

}
