DESCRIPTION

This is a utility object and supporting custom workflow task which reads
the "awft.log" and "swft.log" logfiles (and archived logfiles) to extract
metrics on workflow performance.


BUILDING

The library is built using Apache Maven 2; this system is available here:

    http://maven.apache.org/download.html

As some of the TRIRIGA Custom Workflow Task classes are required to build the
library, the following must be performed before proceeding:


1) Create a directory named "lib" under this directory (the "logreader"
   base directory).

2) Extract the "classes.jar" archive from the "tririga-ibs.ear" file, and
   copy it to the newly-created "lib" directory.

3) Execute the following from the "lograder" base directory (the directory
   where this README.txt is found) to install the library in the local
   Maven repository:

mvn install:install-file -DgroupId=tririga -DartifactId=tririga-classes -Dversion=1.0 -Dpackaging=jar -Dfile=lib/classes.jar


After the jarfile has been installed to Maven, simply run the following from
this directory:

    mvn install

The custom task jarfile will be created in the "target" subdirectory as
"workflow-logs.jar".

