DESCRIPTION

This is a servlet that extracts content from TRIRIGA and presents it in a
format compatible with the SIMILE Exhibit framework.


BUILDING

The library is built using Apache Maven 2; this system is available here:

    http://maven.apache.org/download.html

Simply run the following from this directory:

    mvn install

The middleware warfile will be created in the "exhibit-web/target" subdirectory
as "exhibit.war".


INSTALLING

To install the middleware servlet, edit the "EXHIBIT.properties" configuration
file (found in the same directory as this README) and copy it alongside the
TRIRIGA configuration properties (found in the "config" subdirectory of your
TRIRIGA installation).  Then deploy the "exhibit.war" warfile.  Under JBoss,
this can be done by copying the warfile to the "server/all/deploy"
subdirectory under the JBoss root; similar steps would be taken for other
application servers (consult your documentation).  The application should be
available at "/exhibit" on your server.

The "exhibit-om.zip" Object Migration package should be migrated into the
target TRIRIGA environment to provide the necessary application objects.
