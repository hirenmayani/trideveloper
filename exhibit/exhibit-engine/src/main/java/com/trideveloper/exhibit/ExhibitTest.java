package com.trideveloper.exhibit;

import java.io.InputStream;

class ExhibitTest {

    public static void main(String[] args) throws Exception {
        String server = null;
        String username = null;
        String password = null;
        String exhibitName = null;
        boolean document = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-server".equalsIgnoreCase(arg)) {
                server = args[++i];
            } else if ("-user".equalsIgnoreCase(arg) ||
                    "-username".equalsIgnoreCase(arg)) {
                username = args[++i];
            } else if ("-pass".equalsIgnoreCase(arg) ||
                    "-password".equalsIgnoreCase(arg)) {
                password = args[++i];
            } else if ("-exhibit".equalsIgnoreCase(arg) ||
                    "-name".equalsIgnoreCase(arg) ||
                            "-exhibitname".equalsIgnoreCase(arg)) {
                exhibitName = args[++i];
            } else if ("-document".equalsIgnoreCase(arg)) {
                document = true;
            } else {
                doUsage();
            }
        }
        if ("".equals(server)) server = null;
        if ("".equals(username)) username = null;
        if ("".equals(password)) password = null;
        if (username == null) password = null;
        if ("".equals(exhibitName)) exhibitName = null;
        if (exhibitName == null) doUsage();
        if (username != null && password == null) doUsage();
        ExhibitEngine engine =
                ExhibitEngine.getInstance(server, username, password);
        Exhibit exhibit = engine.getExhibit(exhibitName);
        if (document) {
            InputStream input = exhibit.getDocument().getInputStream();
            byte[] buffer = new byte[65536];
            int count;
            while ((count = input.read(buffer, 0, 65536)) != -1) {
                System.out.write(buffer, 0, count);
            }
            System.out.flush();
        } else {
            System.out.println(exhibit.getData());
        }
        System.exit(0);
    }

    private static void doUsage() {
        System.err.println();
        System.err.println("Usage:");
        System.err.println();
        System.err.println("ExhibitTest [-server serverName] " +
                "[-user username -password password]");
        System.err.println("            [-exhibit exhibitName] " +
                "[-document]");
        System.err.println();
        System.err.println("      -server : Specifies the TRIRIGA " +
                "server that will service the request.");
        System.err.println("        -user : Specifies the TRIRIGA " +
                "username that should be used.");
        System.err.println("    -password : Specifies the password " +
                "for the TRIRIGA user.");
        System.err.println("     -exhibit : Specifies the name of " +
                "the Exhibit that should be retrieved.");
        System.err.println("    -document : Retrieves the Exhibit " +
                "document instead of the data.");
        System.err.println();
        System.exit(1);
    }

}
