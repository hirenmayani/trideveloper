DESCRIPTION

This is a small application object and supporting custom workflow task which
provides language translation of text fields using the Google AJAX Language API.


BUILDING

The library is built using Apache Maven 2; this system is available here:

    http://maven.apache.org/download.html

As some of the TRIRIGA Custom Workflow Task classes are required to build the
library, the following must be performed before proceeding:


1) Extract the "lib/classes.jar" file from the "tririga-ibs.ear" file, and
   copy it to the "lib" subdirectory.

2) Execute the following from the "translate" base directory (the directory
   where this README.txt is found) to install the library in the local
   Maven repository:

mvn install:install-file -DgroupId=tririga -DartifactId=tririga-classes -Dversion=1.0 -Dpackaging=jar -Dfile=lib/classes.jar


After the jarfile has been installed to Maven, simply run the following from
this directory:

    mvn install

The custom task jarfile will be created in the "target" subdirectory as
"translate.jar".


For installation instructions, see "INSTALLATION.txt" in this directory.
