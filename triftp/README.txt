DESCRIPTION

This is a servlet that provides a "gateway" FTP service, allowing FTP access
to the TRIRIGA Document Manager.


BUILDING

The library is built using Apache Maven 2; this system is available here:

    http://maven.apache.org/download.html

Simply run the following from this directory:

    mvn install

The middleware warfile will be created in the "target" subdirectory
as "ftp.war".  Note that this project may have dependencies on other libraries
and projects in the triDeveloper suite; those can be resolved manually, or
this project can be built as part of the suite by running "mvn install" from
the parent directory (the suite root).


INSTALLING

To install the middleware servlet, edit the "TRIFTP.properties" configuration
file (found in the same directory as this README) and copy it alongside the
TRIRIGA configuration properties (found in the "config" subdirectory of your
TRIRIGA installation).  Then deploy the "ftp.war" warfile.  Under JBoss,
this can be done by copying the warfile to the "server/all/deploy"
subdirectory under the JBoss root; similar steps would be taken for other
application servers (consult your documentation).  After deploying, you
should be able to point your web browser at "/ftp/ftp" on your app server and
receive a message that the service is running.  You can then access your app
server using an FTP client, and log in using TRIRIGA credentials for a user
with membership in the specified FTP group (configured in the properties file).

