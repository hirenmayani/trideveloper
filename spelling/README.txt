DESCRIPTION

This is a small spellchecker extension for TRIRIGA.  A servlet performs the
dictionary lookup functions, and JavaScript is used to submit text content
from the GUI.


BUILDING

The library is built using Apache Maven 2; this system is available here:


    http://maven.apache.org/download.html


To build, the application, run the following commands from a command prompt
in this directory:


mvn install:install-file -DgroupId=jazzy -DartifactId=jazzy-core -Dversion=0.5.2 -Dpackaging=jar -Dfile=lib/jazzy-core-0.5.2.jar

mvn install


The spellcheck service warfile will be created in the "target" subdirectory
as "spelling.war".  Note that this project may have dependencies on other
libraries and projects in the triDeveloper suite; those can be resolved
manually, or this project can be built as part of the suite by running
"mvn install" from the parent directory (the suite root).


For installation instructions, see "INSTALLATION.txt" in this directory.
