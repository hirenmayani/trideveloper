This is a "record broadcaster".  A custom workflow task dispatches the records
from the workflow to a listener servlet; these are delivered via HTTP streaming
push to a client portal section, where the record is opened on the client.  The
end effect is that the workflow task triggers the record to open on the client
in real time.

Building this example requires Apache Ant, available at:

    http://ant.apache.org

Running "ant" in this directory should retrieve dependency libraries if they do
not exist and build the application.  The Tririga core library classes are
also required to compile; these are available in the "tririga-ibs.ear" archive
as "lib/classes.jar", and should be extracted from the .ear file and added
to the classpath.  You can also copy "classes.jar" to the "dep" subdirectory of
the build root (this subdirectory will be created when ant is run).

The build process will create the following:


    broadcast.war - The "broadcast agent"; a standalone Servlet application
                    that receives records from the workflow and dispatches
                    them to connected clients.

    broadcast-task.jar - A custom workflow task that publishes records to the
                         broadcast agent.

The broadcast agent can be deployed on the Tririga server using the deployment
instructions provided by your container provider.  For JBoss, "broadcast.war"
can be copied to the "server/all/deploy" subdirectory.  "broadcast-task.jar"
contains a Custom Task class, and should be added to your container's
classpath; for JBoss, it should be sufficient to copy the jarfile into the
"server/all/deploy" subdirectory alongside the warfile.  Restarting JBoss
should deploy the agent application and include the new .jar in the classpath.

To dispatch records to the broadcaster, simply add a Custom Task to the
desired workflow; the "Class Name" field should specify
"com.trideveloper.broadcast.BroadcastTask".  Remember to flush the Workflow
Cache if you are running in production mode.  By default, the task will
connect to "http://localhost:8001/broadcast/broadcast" (it assumes that the
broadcast agent is deployed on the Tririga application server, and running on
port 8001).  The included "TRIRIGABROADCAST.properties" can be modified as
needed to reflect your deployment, and added to the Tririga "config" directory.

To add broadcast clients, create a portal section in the Portal Section
Manager; the section type should be "External", and the URL should be
"/broadcast/index.html".  Add the portal section to the appropriate
users' portal (they will need to log out and back in to Tririga to pick up the
portal changes).

When the workflow task is triggered, the target record(s) from the workflow
task should pop up immediately on one of the connected clients.  If no
clients are connected, pushes will queue up on the server for delivery, and
will be sent to the first user that logs on (basically popping up on login).
