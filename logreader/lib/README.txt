Copy "classes.jar" from the "tririga-ibs.ear" archive to this directory; then,
from the parent directory, run the following:


mvn install:install-file -DgroupId=tririga -DartifactId=tririga-classes -Dversion=1.0 -Dpackaging=jar -Dfile=lib/classes.jar

