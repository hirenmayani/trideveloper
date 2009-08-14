package com.trideveloper.tririga;

import java.io.IOException;

import javax.activation.DataSource;

/**
 * This is a simple Document Manager client; it provides an interface to
 * obtain a document by either record ID or document path.  It can also
 * "render" the document against a specified record (effectively providing
 * the "print preview" for the record).
 */
public interface TririgaDocumentClient {

    /**
     * Retrieves a DataSource which provides access to the document at
     * the specified path within the Document Manager.
     */
    public DataSource getDocument(String path) throws IOException;

    /**
     * Retrieves a DataSource which provides access to the document with
     * the specified record ID.
     */
    public DataSource getDocument(long documentId) throws IOException;

    /**
     * Retrieves a DataSource which provides access to the document at
     * the specified path within the Document Manager, rendering the document
     * against the record with the provided record ID.
     */
    public DataSource getDocument(String path, long recordId)
            throws IOException;

    /**
     * Retrieves a DataSource which provides access to the document with
     * the specified document record ID, rendering the document
     * against the record with the provided record ID.
     */
    public DataSource getDocument(long documentId, long recordId)
            throws IOException;

}
