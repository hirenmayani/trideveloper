DESCRIPTION

This is a small application and supporting custom workflow task which allows
TRIRIGA to execute Pentaho Data Integration (Kettle) based ETL jobs and
transformations on a scheduled or on-demand basis. 


BUILDING

The library is built using Apache Maven 2; this system is available here:

    http://maven.apache.org/download.html

As some of the TRIRIGA Custom Workflow Task classes are required to build the
library, as well as the base Kettle libraries, the following must be performed
before proceeding:


1) Extract the following files from the "tririga-ibs.ear" file, and copy them
   to the "lib" subdirectory.

        lib/classes.jar
        lib/kettle-core.jar
        lib/kettle-engine.jar

2) Execute the following from the "etl-job" base directory (the directory
   where this README.txt is found) to install the libraries in the local
   Maven repository:

mvn install:install-file -DgroupId=org.pentaho -DartifactId=kettle-core -Dversion=1.0 -Dpackaging=jar -Dfile=lib/kettle-core.jar

mvn install:install-file -DgroupId=org.pentaho -DartifactId=kettle-engine -Dversion=1.0 -Dpackaging=jar -Dfile=lib/kettle-engine.jar

mvn install:install-file -DgroupId=tririga -DartifactId=tririga-classes -Dversion=1.0 -Dpackaging=jar -Dfile=lib/classes.jar


After the jarfiles have been installed to Maven, simply run the following from
this directory:

    mvn install

The custom task jarfile will be created in the "target" subdirectory as
"etl-job.jar".


INSTALLING

To install the custom workflow task, copy the "etl-job.jar" jarfile to the
"server/all/lib" subdirectory under the JBoss root (typically found directly
under the TRIRIGA installation root).  After installing you will need to
restart JBoss.

For other application server platforms, consult your server documentation.

The "etl-job-om.zip" Object Migration package can then be imported, which
will install the ETL Job application under the "devETLJob" module.
