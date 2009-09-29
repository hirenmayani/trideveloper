Copy "classes.jar", "kettle-core.jar", and "kettle-engine.jar" from the
"tririga-ibs.ear" archive to this directory; then, from the parent directory,
run the following:


mvn install:install-file -DgroupId=org.pentaho -DartifactId=kettle-core -Dversion=1.0 -Dpackaging=jar -Dfile=lib/kettle-core.jar

mvn install:install-file -DgroupId=org.pentaho -DartifactId=kettle-engine -Dversion=1.0 -Dpackaging=jar -Dfile=lib/kettle-engine.jar

mvn install:install-file -DgroupId=tririga -DartifactId=tririga-classes -Dversion=1.0 -Dpackaging=jar -Dfile=lib/classes.jar

