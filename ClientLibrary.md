# Introduction #

This is a client factory for the TRIRIGA APIs (both BusinessConnect and API1); it also provides a rudimentary API for downloading documents from the Document Manager.


# Details #

The client factory provides a fail-fast factory for API clients.  All clients obtained from the factory (for a given server/user) share the same application session.  Clients are not thread-safe, but the factory is; so a single factory can be shared across threads, to create any number of clients which share a single application session.  Terminating the session (through interactive external login, or programmatically through the API) invalidates all open clients instantly and leaves the system in a consistent state.  So in the event of session issues, the system fails cleanly rather than allowing clients in other threads to respawn independent competing application sessions.

The document manager API allows callers to retrieve a document by either record ID or document path; it can also "render" the document against a specific record ID (as is done in a Print Preview).

This library builds the TRIRIGA client stubs under the "com.trideveloper.tririga.`*`" package namespace to avoid classloader conflicts with the existing TRIRIGA stubs (if the library is used in a custom task or otherwise under the same classloader as the TRIRIGA application).