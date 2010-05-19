This is the root directory of the triDeveloper suite.  The suite is built using
the Apache Maven 2 build automation tool.  While Maven is able to automatically
obtain most of the dependencies required to build the various subprojects,
some additional files are required from your TRIRIGA installation.

To build the entire triDeveloper suite:


1) Download and install Apache Maven 2 from the project's download site:

    http://maven.apache.org/download.html


2) Obtain the following WSDL files from your TRIRIGA application server,
   and save them under the "/tririga-client/src/main/wsdl" directory:

        businessconnect.wsdl    http://yourserver/ws/TririgaWS?WSDL

        query.wsdl              http://yourserver/services/Query?WSDL

        borecord.wsdl           http://yourserver/services/BoRecord?WSDL

        wftaskactions.wsdl      http://yourserver/services/WFTaskActions?WSDL

        cadquery.wsdl           http://yourserver/services/CadQuery?WSDL

   To download the files, point your browser at the URL indicated and then
   save the generated content under the given filename.


3) Copy the following files out of the "tririga-ibs.ear" archive (found
   under your TRIRIGA installation) to this directory:

        classes.jar
        kettle-core.jar
        kettle-engine.jar


4) Run the following commands from a command prompt in this directory:


mvn install:install-file -DgroupId=tririga -DartifactId=tririga-classes -Dversion=1.0 -Dpackaging=jar -Dfile=classes.jar

mvn install:install-file -DgroupId=org.pentaho -DartifactId=kettle-core -Dversion=1.0 -Dpackaging=jar -Dfile=kettle-core.jar

mvn install:install-file -DgroupId=org.pentaho -DartifactId=kettle-engine -Dversion=1.0 -Dpackaging=jar -Dfile=kettle-engine.jar

mvn install:install-file -DgroupId=jazzy -DartifactId=jazzy-core -Dversion=0.5.2 -Dpackaging=jar -Dfile=spelling/lib/jazzy-core-0.5.2.jar


   After Maven has finished importing the libraries, you can delete the .jar
   files from this directory.


5) Run the following command:

        mvn install

   This will build all subprojects; the build artifacts will be found in the
   "target" subdirectory of each subproject.

