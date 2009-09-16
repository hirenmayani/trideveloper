DESCRIPTION

This is a client factory for the TRIRIGA APIs (both BusinessConnect and API1).
It also provides a rudimentary API for downloading documents from the Document
Manager.  This library builds the TRIRIGA client stubs under the
"com.trideveloper.tririga.*" package namespace to avoid classloader conflicts
with the existing TRIRIGA stubs (if the library is used in a custom
task or otherwise under the same classloader as the TRIRIGA application).

The client factory provides a fail-fast factory for API clients.  All clients
obtained from the factory (for a given server/user) share the same application
session.  Clients are not thread-safe, but the factory is; so a single factory
can be shared across threads, to create any number of clients which share a
single application session.  Terminating the session (through interactive
external login, or programmatically through the API) invalidates all open
clients instantly and leaves the system in a consistent state.  So in the event
of session issues, the system fails cleanly rather than allowing clients in
other threads to respawn independent competing application sessions.

The document manager API allows callers to retrieve a document by either
record ID or document path; it can also "render" the document against a specific
record ID (as is done in a Print Preview).


BUILDING

The library is built using Apache Maven 2; this system is available here:

    http://maven.apache.org/download.html

As the TRIRIGA SOAP API WSDLs are required to build the library, the following
must be performed before proceeding:


1) Download the BusinessConnect WSDL from:

       http://yourserver/ws/TririgaWS?WSDL

   Save the file as "businessconnect.wsdl" in "src/main/wsdl".


2) Download the API1 Query Service WSDL from:

       http://yourserver/services/Query?WSDL

   Save the file as "query.wsdl" in "src/main/wsdl".


3) Download the API1 Records Service WSDL from:

       http://yourserver/services/BoRecord?WSDL

   Save the file as "borecord.wsdl" in "src/main/wsdl".


4) Download the API1 Task Actions Service WSDL from:

       http://yourserver/services/WFTaskActions?WSDL

   Save the file as "wftaskactions.wsdl" in "src/main/wsdl".


5) Download the API1 CAD Query Service WSDL from:

       http://yourserver/services/CadQuery?WSDL

   Save the file as "cadquery.wsdl" in "src/main/wsdl".


After the WSDLs have been downloaded and Maven installed, simply run the
following from this directory:


    mvn install

