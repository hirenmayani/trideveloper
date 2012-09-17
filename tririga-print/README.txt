DESCRIPTION

This is a custom workflow task that formats a record using a template from the
Document Manager, and renders it to a Windows shared printer.  This allows,
for example, Work Tasks to be printed upon assignment to a Work Group to the
shop's local printer.


BUILDING

The library is built using Apache Maven 2; this system is available here:

    http://maven.apache.org/download.html

As some of the TRIRIGA Custom Workflow Task classes are required to build the
library, as well as the base Kettle libraries, the following must be performed
before proceeding:


1) Copy "TririgaCustomTask.jar" and "TririgaBusinessConnect.jar" from the
"tools/BusinessConnect" directory under your TRIRIGA installation into the "lib"
subdirectory here.

2) Execute the following from the "tririga-print" base directory (the directory
   where this README.txt is found) to install the libraries in the local
   Maven repository:

mvn install:install-file -DgroupId=tririga -DartifactId=tririga-custom-task -Dversion=1.0 -Dpackaging=jar -Dfile=lib/TririgaCustomTask.jar

mvn install:install-file -DgroupId=tririga -DartifactId=tririga-business-connect -Dversion=1.0 -Dpackaging=jar -Dfile=lib/TririgaBusinessConnect.jar

mvn install:install-file -DgroupId=jcifs -DartifactId=jcifs-print -Dversion=1.3.17 -Dpackaging=jar -Dfile=lib/jcifs-print-1.3.17.jar


After the jarfiles have been installed to Maven, simply run the following from
this directory:

    mvn install

The custom task jarfile will be created in the "target" subdirectory as
"tririga-print.jar".


INSTALLING

The "tririga-print-om.zip" Object Migration package includes the required
TRIRIGA objects, as well as the custom task and related jarfiles.  After
importing the Object Migration package, the following must be done to install
the custom task classloader:


1) In the Report Manager, browse to the triPatchHelper Business Object
(in the triHelper Module).

2) Run the "triPatchHelper - triHelper - Manager Query" report.

3) Click the "Add" action to add a new triPatchHelper record.

4) In the Name field, enter "PrintInstall".

5) Click "Calculate" to execute the Patch Helper.


A usage example is in the "dev - triPatchHelper - Printing Example" workflow
in the triHelper module.  This workflow does the following:


1) Creates a Print Helper (devPrintHelper) record, which contains the printer
configuration information.  You will need to edit the object mappings in this
step to specify information appropriate to your environment (print server,
username, etc.).  In the example, a sample Document record (included in the
Object Migration package) is mapped to the Print Helper.

2) Associates a context record to the Print Helper.  Any number of records can
be associated via the "For" association; the Helper will print the document
once for each associated record, filling in document information taken from
each context record.  If no context records are linked, the document will be
printed as-is.

3) Triggers the "devPrint" action on the Print Helper record, to start the
print job.


To run the example, first revise and edit the workflow as mentioned above; then
create a Patch Helper record (as done during installation) with the name
"PrintExample" and click "Calculate" to execute the Patch Helper.

