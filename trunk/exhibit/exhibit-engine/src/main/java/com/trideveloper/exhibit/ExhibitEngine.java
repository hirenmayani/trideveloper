package com.trideveloper.exhibit;

import com.trideveloper.tririga.TririgaClientFactory;
import com.trideveloper.tririga.TririgaClient;

import com.trideveloper.tririga.ws.dto.AssociationFilter;
import com.trideveloper.tririga.ws.dto.ContinuationToken;
import com.trideveloper.tririga.ws.dto.DisplayLabel;
import com.trideveloper.tririga.ws.dto.FieldSortOrder;
import com.trideveloper.tririga.ws.dto.Filter;
import com.trideveloper.tririga.ws.dto.QueryResponseColumn;
import com.trideveloper.tririga.ws.dto.QueryResponseHelper;
import com.trideveloper.tririga.ws.dto.QueryResult;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import static com.trideveloper.exhibit.Constants.*;

public class ExhibitEngine {

    private static final String ASSOCIATED_PREFIX = "ASSOCIATED:";

    private static final String RECORD_ID_KEY = "____recordId";

    private static final String PROPERTY_MAP_KEY = "____propertyMap";

    private static final String ACTIVE_STATUS = "Active";

    private static final int STRING_DATA_TYPE = 320;

    private static final int EQUALS_OPERATOR = 10;

    private static final int ALL_PROJECT_SCOPE = 2;

    private static final Map<String, ExhibitEngine> INSTANCE_MAP =
            new HashMap<String, ExhibitEngine>();

    private final Cache<String, Exhibit> exhibitCache =
            createExhibitCache();

    private final String server;

    private final String username;

    private final String password;

    private ExhibitEngine(String server, String username, String password) {
        this.server = server;
        this.username = username;
        this.password = password;
    }

    public static ExhibitEngine getInstance() throws ExhibitException {
        return getInstance(null, null, null);
    }

    public static ExhibitEngine getInstance(String server)
            throws ExhibitException {
        return getInstance(server, null, null);
    }

    public static ExhibitEngine getInstance(String server, String username,
            String password) throws ExhibitException {
        if (server == null) {
            server = Configuration.getProperty(SERVER_PROPERTY);
            if (server == null) {
                throw new NoServerException("No server specified.");
            }
        }
        if (username == null) {
            username = Configuration.getProperty(USERNAME_PROPERTY);
            if (username == null) {
                throw new AuthenticationException("No username specified.");
            }
        }
        if (password == null) {
            password = Configuration.getProperty(PASSWORD_PROPERTY);
            if (password == null) {
                throw new AuthenticationException("No password specified.");
            }
        }
        String instanceKey = server + "|" + username + "|" + password;
        synchronized (INSTANCE_MAP) {
            ExhibitEngine instance = INSTANCE_MAP.get(instanceKey);
            if (instance == null) {
                instance = new ExhibitEngine(server, username, password);
                INSTANCE_MAP.put(instanceKey, instance);
            }
            return instance;
        }
    }

    public Exhibit getExhibit(String name) throws ExhibitException,
            IOException {
        if (name == null || "".equals(name = name.trim())) return null;
        Exhibit exhibit = exhibitCache.get(name);
        if (exhibit != null) return exhibit;
        TririgaClientFactory factory = TririgaClientFactory.getInstance(server,
                username, password);
        TririgaClient tririga = factory.newTririgaClient();
        String projectName = "";
        String module = Configuration.getProperty(EXHIBIT_MODULE_PROPERTY);
        String exhibitObjectType =
                Configuration.getProperty(EXHIBIT_BUSINESS_OBJECT_PROPERTY);
        String[] objectTypeNames = new String[] { exhibitObjectType };
        String[] guiNames = null;
        String associatedModuleName = null;
        String associatedObjectTypeName = null;
        int projectScope = ALL_PROJECT_SCOPE;
        String nameField =
                Configuration.getProperty(EXHIBIT_NAME_FIELD_PROPERTY);
        DisplayLabel[] displayFields = new DisplayLabel[] {
            createDisplayLabel(nameField),
            createDisplayLabel(Configuration.getProperty(
                EXHIBIT_USERNAME_FIELD_PROPERTY)),
            createDisplayLabel(Configuration.getProperty(
                EXHIBIT_PASSWORD_FIELD_PROPERTY)),
            createDisplayLabel(Configuration.getProperty(
                EXHIBIT_CACHE_TIME_FIELD_PROPERTY)),
            createDisplayLabel(Configuration.getProperty(
                EXHIBIT_DOCUMENT_FIELD_PROPERTY))
        };
        DisplayLabel[] associatedDisplayFields = null;
        FieldSortOrder[] fieldSortOrders = null;
        Filter[] filters = new Filter[] {
            createFilter(nameField, name),
            createFilter(Configuration.getProperty(
                    EXHIBIT_STATUS_FIELD_PROPERTY), ACTIVE_STATUS)
        };
        AssociationFilter[] associationFilters = null;
        int start = 1;
        int maxResults = 2;
        QueryResult queryResult = tririga.runDynamicQuery(projectName,
                module, objectTypeNames, guiNames,
                associatedModuleName, associatedObjectTypeName, projectScope,
                displayFields, associatedDisplayFields, fieldSortOrders,
                filters, associationFilters, start, maxResults);
        Map dataMap = new HashMap();
        processExhibit(name, queryResult, dataMap);
        String exhibitRecordId = (String) dataMap.get(RECORD_ID_KEY);
        exhibit = createExhibit(dataMap);
        objectTypeNames = new String[] {
            Configuration.getProperty(DATA_SOURCE_BUSINESS_OBJECT_PROPERTY)
        };
        guiNames = null;
        associatedModuleName = module;
        associatedObjectTypeName = Configuration.getProperty(
                PROPERTY_BUSINESS_OBJECT_PROPERTY);
        projectScope = ALL_PROJECT_SCOPE;
        displayFields = new DisplayLabel[] {
            createDisplayLabel(Configuration.getProperty(
                    DATA_SOURCE_TYPE_FIELD_PROPERTY)),
            createDisplayLabel(Configuration.getProperty(
                    DATA_SOURCE_PLURAL_FIELD_PROPERTY)),
            createDisplayLabel(Configuration.getProperty(
                    DATA_SOURCE_QUERY_MODULE_FIELD_PROPERTY)),
            createDisplayLabel(Configuration.getProperty(
                    DATA_SOURCE_QUERY_BUSINESS_OBJECT_FIELD_PROPERTY)),
            createDisplayLabel(Configuration.getProperty(
                    DATA_SOURCE_QUERY_NAME_FIELD_PROPERTY))
        };
        associatedDisplayFields = new DisplayLabel[] {
            createAssociatedDisplayLabel(Configuration.getProperty(
                    PROPERTY_NAME_FIELD_PROPERTY)),
            createAssociatedDisplayLabel(Configuration.getProperty(
                    PROPERTY_LABEL_FIELD_PROPERTY)),
            createAssociatedDisplayLabel(Configuration.getProperty(
                    PROPERTY_REVERSE_LABEL_FIELD_PROPERTY)),
            createAssociatedDisplayLabel(Configuration.getProperty(
                    PROPERTY_VALUE_TYPE_FIELD_PROPERTY))
        };
        fieldSortOrders = null;
        filters = new Filter[] {
            createFilter(Configuration.getProperty(
                    DATA_SOURCE_STATUS_FIELD_PROPERTY), ACTIVE_STATUS)
        };
        associationFilters = new AssociationFilter[] {
            new AssociationFilter(Configuration.getProperty(
                    EXHIBIT_DATA_SOURCE_ASSOCIATION_PROPERTY), module,
                            exhibitObjectType, false, exhibitRecordId)
        };
        start = 1;
        maxResults = 100;
        queryResult = tririga.runDynamicQuery(projectName,
                module, objectTypeNames, guiNames,
                associatedModuleName, associatedObjectTypeName, projectScope,
                displayFields, associatedDisplayFields, fieldSortOrders,
                filters, associationFilters, start, maxResults);
        ContinuationToken token;
        dataMap = new HashMap();
        while ((token = processDataSources(queryResult, dataMap)) != null) {
            queryResult = tririga.runDynamicQueryContinue(token);
        }
        populateDataSources((BasicExhibit) exhibit, dataMap);
        exhibitCache.put(name, exhibit);
        return exhibit;
    }

    private void processExhibit(String name,
            QueryResult queryResult, Map dataMap) throws ExhibitException,
                    IOException {
        if (queryResult == null) return;
        QueryResponseHelper[] queryResults =
                queryResult.getQueryResponseHelpers();
        if (queryResults == null || queryResults.length == 0) {
            throw new ExhibitException("No exhibit with name \"" + name +
                    "\".");
        }
        if (queryResults.length != 1) {
            throw new ExhibitException("More than one exhibit for name \"" +
                    name + "\".");
        }
        QueryResponseHelper result = queryResults[0];
        String recordId = result.getRecordId();
        dataMap.put(RECORD_ID_KEY, recordId);
        for (QueryResponseColumn column :
                result.getQueryResponseColumns()) {
            String fieldName = column.getLabel();
            String value = column.getDisplayValue();
            if (value == null || "".equals(value)) {
                value = column.getValue();
            }
            if (!dataMap.containsKey(fieldName)) dataMap.put(fieldName, value);
        }
    }

    private ContinuationToken processDataSources(QueryResult queryResult,
            Map dataMap) throws IOException {
        if (queryResult == null) return null;
        QueryResponseHelper[] queryResults =
                queryResult.getQueryResponseHelpers();
        if (queryResults == null || queryResults.length == 0) return null;
        Map<String, Integer> propCounter = new HashMap<String, Integer>();
        for (QueryResponseHelper result : queryResults) {
            String dsRecordId = result.getRecordId();
            Map<String, Object> dsMap = (Map<String, Object>)
                    dataMap.get(dsRecordId);
            if (dsMap == null) {
                dataMap.put(dsRecordId, dsMap = new HashMap<String, Object>());
            }
            // bug with getAssocId - always returns the first associated ID
            //String propertyRecordId = result.getAssocId();
            Integer assocId = propCounter.get(dsRecordId);
            if (assocId == null) {
                assocId = 0;
            } else {
                assocId++;
            }
            propCounter.put(dsRecordId, assocId);
            String propertyRecordId = String.valueOf(assocId);
            for (QueryResponseColumn column :
                    result.getQueryResponseColumns()) {
                String fieldName = column.getLabel();
                String value = column.getDisplayValue();
                if (value == null || "".equals(value)) {
                    value = column.getValue();
                }
                if (fieldName.startsWith(ASSOCIATED_PREFIX)) {
                    if (propertyRecordId != null) {
                        Map<String, Map<String, String>> properties =
                                (Map<String, Map<String, String>>)
                                        dsMap.get(PROPERTY_MAP_KEY);
                        if (properties == null) {
                            dsMap.put(PROPERTY_MAP_KEY, properties =
                                    new HashMap<String, Map<String, String>>());
                        }
                        Map<String, String> propMap =
                                properties.get(propertyRecordId);
                        if (propMap == null) {
                            properties.put(propertyRecordId, propMap =
                                    new HashMap<String, String>());
                        }
                        fieldName = fieldName.substring(
                                ASSOCIATED_PREFIX.length());
                        if (!propMap.containsKey(fieldName)) {
                            propMap.put(fieldName, value);
                        }
                    }
                } else if (!dsMap.containsKey(fieldName)) {
                    dsMap.put(fieldName, value);
                }
            }
        }
        ContinuationToken token = queryResult.getContinuationToken();
        return (token == null || token.getTokenString() == null) ? null :
                token;
    }

    private static Cache<String, Exhibit> createExhibitCache() {
        String exhibitCacheSize = Configuration.getProperty(
                EXHIBIT_CACHE_SIZE_PROPERTY);
        String exhibitCacheTimeout = Configuration.getProperty(
                EXHIBIT_CACHE_TIMEOUT_PROPERTY);
        int cacheSize = Integer.parseInt(exhibitCacheSize);
        long cacheTimeout = Long.parseLong(exhibitCacheTimeout);
        return new Cache<String, Exhibit>(cacheTimeout,
                cacheSize);
    }

    private BasicExhibit createExhibit(Map dataMap) throws ExhibitException,
            IOException {
        String document = (String) dataMap.get(
                Configuration.getProperty(EXHIBIT_DOCUMENT_FIELD_PROPERTY));
        String cacheMinutes = (String) dataMap.get(
                Configuration.getProperty(EXHIBIT_CACHE_TIME_FIELD_PROPERTY));
        String username = (String) dataMap.get(Configuration.getProperty(
                EXHIBIT_USERNAME_FIELD_PROPERTY));
        if (username == null || "".equals(username = username.trim())) {
            username = null;
        }
        String password = (String) dataMap.get(Configuration.getProperty(
                EXHIBIT_PASSWORD_FIELD_PROPERTY));
        if (password == null || "".equals(password = password.trim())) {
            password = null;
        }
        if (username == null) {
            username = this.username;
            if (username == null) {
                throw new AuthenticationException("No default username found.");
            }
        }
        if (password == null) {
            password = this.password;
            if (password == null) {
                throw new AuthenticationException("No default password found.");
            }
        }
        if (cacheMinutes == null ||
                "".equals(cacheMinutes = cacheMinutes.trim())) {
            cacheMinutes = null;
        }
        long cacheTime = 0l;
        try {
            cacheTime = Long.parseLong(cacheMinutes);
        } catch (NumberFormatException ex) { }
        cacheTime *= 60000l; // convert to milliseconds
        return new BasicExhibit(server, username, password, document,
                cacheTime);
    }

    private void populateDataSources(BasicExhibit exhibit, Map dataMap)
            throws IOException {
        for (Object entry : dataMap.values()) {
            Map<String, Object> dsMap = (Map<String, Object>) entry;
            String type = (String) dsMap.get(Configuration.getProperty(
                    DATA_SOURCE_TYPE_FIELD_PROPERTY));
            String plural = (String) dsMap.get(Configuration.getProperty(
                    DATA_SOURCE_PLURAL_FIELD_PROPERTY));
            String queryModule = (String) dsMap.get(Configuration.getProperty(
                    DATA_SOURCE_QUERY_MODULE_FIELD_PROPERTY));
            String queryBusinessObject = (String)
                    dsMap.get(Configuration.getProperty(
                            DATA_SOURCE_QUERY_BUSINESS_OBJECT_FIELD_PROPERTY));
            String queryName = (String)
                    dsMap.get(Configuration.getProperty(
                            DATA_SOURCE_QUERY_NAME_FIELD_PROPERTY));
            ExhibitDataSource dataSource = new ExhibitDataSource(type, plural,
                    queryModule, queryBusinessObject, queryName);
            Map<String, Map<String, String>> properties =
                    (Map<String, Map<String, String>>)
                            dsMap.get(PROPERTY_MAP_KEY);
            if (properties != null) {
                for (Map<String, String> propMap : properties.values()) {
                    String name = (String)
                            propMap.get(Configuration.getProperty(
                                    PROPERTY_NAME_FIELD_PROPERTY));
                    String label = (String)
                            propMap.get(Configuration.getProperty(
                                    PROPERTY_LABEL_FIELD_PROPERTY));
                    String reverseLabel = (String)
                            propMap.get(Configuration.getProperty(
                                    PROPERTY_REVERSE_LABEL_FIELD_PROPERTY));
                    String valueType = (String)
                            propMap.get(Configuration.getProperty(
                                    PROPERTY_VALUE_TYPE_FIELD_PROPERTY));
                    dataSource.add(new PropertyDefinition(name, label,
                            reverseLabel, valueType));
                }
            }
            exhibit.add(dataSource);
        }
    }

    private static DisplayLabel createDisplayLabel(String field) {
        int index = field.indexOf(".");
        String sectionName = field.substring(0, index);
        String fieldName = field.substring(index + 1);
        return new DisplayLabel(fieldName, field, sectionName);
    }

    private static DisplayLabel createAssociatedDisplayLabel(String field) {
        int index = field.indexOf(".");
        String sectionName = field.substring(0, index);
        String fieldName = field.substring(index + 1);
        return new DisplayLabel(fieldName, ASSOCIATED_PREFIX + field,
                sectionName);
    }

    private static Filter createFilter(String field, String value) {
        int index = field.indexOf(".");
        String sectionName = field.substring(0, index);
        String fieldName = field.substring(index + 1);
        return new Filter(STRING_DATA_TYPE, fieldName,
                EQUALS_OPERATOR, sectionName, value);
    }

}
