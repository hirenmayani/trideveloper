package com.trideveloper.tririga;

import com.trideveloper.tririga.api1.BoRecordActionsInterface;
import com.trideveloper.tririga.api1.CadQueryInterface;
import com.trideveloper.tririga.api1.QueryActionsInterface;
import com.trideveloper.tririga.api1.WFTaskActionsInterface;

import com.trideveloper.tririga.ws.TririgaWSPortType;

/**
 * This is a unifying interface which extends the BusinessConnect API and
 * API1 interfaces, as well as the document client API.  It also provides
 * operations to retrieve the security token, user ID, and company ID
 * parameters required by the API1 calls.
 */
public interface TririgaClient extends TririgaDocumentClient, TririgaWSPortType,
        BoRecordActionsInterface, CadQueryInterface, QueryActionsInterface,
                WFTaskActionsInterface {

    /**
     * Retrieves the security token for the currently active session.
     */
    public String getSecurityToken();

    /**
     * Retrieves the user ID for the current session.
     */
    public long getUserId();

    /**
     * Retrieves the company ID for the current session.
     */
    public long getCompanyId();

}
