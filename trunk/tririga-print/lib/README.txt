Copy "TririgaCustomTask.jar" and "TririgaBusinessConnect.jar" from the
"tools/BusinessConnect" TRIRIGA subdirectory to this directory; then, from the
parent directory, run the following:


mvn install:install-file -DgroupId=tririga -DartifactId=tririga-custom-task -Dversion=1.0 -Dpackaging=jar -Dfile=lib/TririgaCustomTask.jar

mvn install:install-file -DgroupId=tririga -DartifactId=tririga-business-connect -Dversion=1.0 -Dpackaging=jar -Dfile=lib/TririgaBusinessConnect.jar

mvn install:install-file -DgroupId=jcifs -DartifactId=jcifs-print -Dversion=1.3.17 -Dpackaging=jar -Dfile=lib/jcifs-print-1.3.17.jar

