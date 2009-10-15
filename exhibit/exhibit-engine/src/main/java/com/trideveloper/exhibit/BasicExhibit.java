package com.trideveloper.exhibit;

import com.trideveloper.tririga.TririgaClientFactory;
import com.trideveloper.tririga.TririgaClient;

import com.trideveloper.tririga.ws.dto.ContinuationToken;
import com.trideveloper.tririga.ws.dto.Filter;
import com.trideveloper.tririga.ws.dto.QueryResponseColumn;
import com.trideveloper.tririga.ws.dto.QueryResponseHelper;
import com.trideveloper.tririga.ws.dto.QueryResult;

import java.io.IOException;

import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.activation.DataSource;

import org.json.JSONException;
import org.json.JSONStringer;

import static com.trideveloper.exhibit.Constants.*;

class BasicExhibit implements Exhibit {

    private static final Timer CACHE_TIMER = new Timer("Exhibit Cache Timer", true);

    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    private static final String ID_KEY = "id";

    private static final String LABEL_KEY = "label";

    private static final String URI_KEY = "uri";

    private static final String TYPE_KEY = "type";

    private static final String INTERNAL_ID_KEY = "_________internal-id";

    private static final long MAX_CACHE_TIME = 60000l * 120l; // 2 hours max

    private final TririgaClientFactory factory;

    private final String uriBase;

    private final String document;

    private final long cacheTime;

    private final Set<ExhibitDataSource> sources =
            new HashSet<ExhibitDataSource>();

    private String cachedData;

    public BasicExhibit(String server, String username, String password,
            String document, long cacheTime) {
        this.factory = TririgaClientFactory.getInstance(server, username,
                password);
        this.cacheTime = Math.min(Math.max(0l, cacheTime), MAX_CACHE_TIME);
        String uriBase = null;
        try {
            String recordUri =
                    Configuration.getProperty(EXHIBIT_RECORD_URI_PROPERTY);
            if (recordUri != null && !"".equals(recordUri)) {
                String loginRedirectUri = Configuration.getProperty(
                        EXHIBIT_LOGIN_REDIRECT_URI_PROPERTY);
                URL serverUrl = new URL(server);
                boolean isLocal = false;
                if (!Boolean.valueOf(Configuration.getProperty(
                        EXHIBIT_ASSUME_REMOTE_PROPERTY))) {
                    // Check if TRIRIGA is on the same server.  Unless
                    // we are configured to assume remote, use a relative
                    // URL for local.
                    try {
                        InetAddress host =
                                InetAddress.getByName(serverUrl.getHost());
                        if (host.isLoopbackAddress()) serverUrl = null;
                    } catch (Exception e) { }
                }
                if (loginRedirectUri != null && !"".equals(loginRedirectUri)) {
                    recordUri = URLEncoder.encode(recordUri, "UTF-8");
                    recordUri = loginRedirectUri + recordUri;
                }
                uriBase = (serverUrl != null) ?
                        new URL(serverUrl, recordUri).toExternalForm() :
                                recordUri;
            }
        } catch (Exception ignore) { }
        this.uriBase = uriBase;
        this.document = document;
    }

    public String getData() throws IOException {
        if (cacheTime == 0l) return getDataImpl();
        synchronized (this) {
            if (cachedData != null) return cachedData;
            cachedData = getDataImpl();
            if (cachedData == null) return null;
            TimerTask task = new TimerTask() {
                public void run() {
                    synchronized (BasicExhibit.this) {
                        cachedData = null;
                    }
                }
            };
            CACHE_TIMER.schedule(task, cacheTime);
            return cachedData;
        }
    }

    public DataSource getDocument() throws IOException {
        if (document == null) {
            throw new IOException("No document provided for this exhibit.");
        }
        return factory.newTririgaClient().getDocument(document);
    }

    public void add(ExhibitDataSource source) {
        sources.add(source);
    }

    private String getDataImpl() throws IOException {
        Set<TypeDefinition> types = new TreeSet<TypeDefinition>();
        Map<String, PropertyDefinition> properties =
            new TreeMap<String, PropertyDefinition>();
        Map<String, Map<String, Object>> data =
                new HashMap<String, Map<String, Object>>();
        for (ExhibitDataSource source : sources) {
            String type = source.getType();
            if (type != null) {
                String plural = source.getPlural();
                if (plural != null) {
                    types.add(new TypeDefinition(type, plural));
                }
            }
            for (PropertyDefinition property : source.getProperties()) {
                String propertyName = property.getName();
                if (propertyName != null) {
                    properties.put(propertyName, property);
                }
            }
        }
        String projectName = "";
        Filter[] queryFilters = null;
        int start = 1;
        int maxResults = 500;
        TririgaClient tririga = factory.newTririgaClient();
        for (ExhibitDataSource source : sources) {
            String module = source.getQueryModule();
            String businessObject = source.getQueryBusinessObject();
            String query = source.getQueryName();
            String type = source.getType();
            QueryResult queryResult = tririga.runNamedQuery(projectName,
                    module, businessObject, query, queryFilters, start,
                            maxResults);
            ContinuationToken token;
            while ((token = processData(queryResult, data, type)) != null) {
                queryResult = tririga.runNamedQueryContinue(token);
            }
        }
        JSONStringer json = new JSONStringer();
        try {
            json.object();
            if (!types.isEmpty()) {
                json.key("types");
                json.object();
                for (TypeDefinition type : types) {
                    json.key(type.getType());
                    json.object();
                    json.key("pluralLabel");
                    json.value(type.getPlural());
                    json.endObject();
                }
                json.endObject();
            }
            if (!properties.isEmpty()) {
                json.key("properties");
                json.object();
                for (PropertyDefinition property : properties.values()) {
                    json.key(property.getName());
                    json.object();
                    String label = property.getLabel();
                    if (label != null) {
                        json.key("label");
                        json.value(label);
                    }
                    String reverseLabel = property.getReverseLabel();
                    if (reverseLabel != null) {
                        json.key("reverseLabel");
                        json.value(reverseLabel);
                    }
                    String valueType = property.getValueType();
                    if (valueType != null) {
                        json.key("valueType");
                        json.value(valueType);
                    }
                    json.endObject();
                }
                json.endObject();
            }
            json.key("items");
            json.array();
            for (Map<String, Object> record : data.values()) {
                // records must have a label
                if (!record.containsKey(LABEL_KEY)) {
                    String id = (String) record.get(ID_KEY);
                    // if we can't use the ID as the label then discard
                    if (id == null || "".equals(id = id.trim())) continue;
                    record.put(LABEL_KEY, id);
                }
                String internalId = (String) record.remove(INTERNAL_ID_KEY);
                if (!record.containsKey(URI_KEY) && internalId != null &&
                        !"".equals(internalId = internalId.trim()) &&
                                uriBase != null) {
                    // if the record doesn't have a URI defined, add
                    // TRIRIGA link.
                    record.put(URI_KEY, uriBase + internalId);
                }
                json.object();
                for(Map.Entry<String, Object> field : record.entrySet()) {
                    String key = field.getKey();
                    Object value = field.getValue();
                    PropertyDefinition property = properties.get(key);
                    if (property != null) {
                        String valueType = property.getValueType();
                        if ("number".equals(valueType)) {
                            if (value instanceof String) {
                                try {
                                    value = Double.parseDouble((String) value);
                                } catch (Exception ex) {
                                    value = null;
                                }
                            } else if (value instanceof String[]) {
                                String[] array = (String[]) value;
                                List<Double> numValues =
                                        new ArrayList<Double>();
                                for (String val : array) {
                                    try {
                                        double dv = Double.parseDouble(val);
                                        numValues.add(dv);
                                    } catch (Exception ignore) { }
                                }
                                if (numValues.isEmpty()) {
                                    value = null;
                                } else if (numValues.size() == 1) {
                                    value = numValues.get(0);
                                } else {
                                    value = numValues.toArray(new Double[0]);
                                }
                            } else {
                                value = null;
                            }
                        } else if ("date".equals(valueType)) {
                            if (value instanceof String) {
                                try {
                                    String v = (String) value;
                                    long dateValue = Long.parseLong(v);
                                    DateFormat format = new SimpleDateFormat(
                                            ISO_8601_FORMAT);
                                    v = format.format(new Date(dateValue));
                                    v = v.substring(0, v.length() - 2) + ":" +
                                            v.substring(v.length() - 2);
                                    value = v;
                                } catch (Exception ignore) {
                                    value = null;
                                }
                            } else if (value instanceof String[]) {
                                String[] array = (String[]) value;
                                List<String> dateValues =
                                        new ArrayList<String>();
                                DateFormat format =
                                        new SimpleDateFormat(ISO_8601_FORMAT);
                                for (String val : array) {
                                    try {
                                        long dateValue = Long.parseLong(val);
                                        String v = format.format(
                                                new Date(dateValue));
                                        v = v.substring(0, v.length() - 2) +
                                                ":" + v.substring(v.length() -
                                                        2);
                                        dateValues.add(v);
                                    } catch (Exception ignore) { }
                                }
                                if (dateValues.isEmpty()) {
                                    value = null;
                                } else if (dateValues.size() == 1) {
                                    value = dateValues.get(0);
                                } else {
                                    value = dateValues.toArray(new String[0]);
                                }
                            } else {
                                value = null;
                            }
                        } else if ("boolean".equals(valueType)) {
                            if (value instanceof String) {
                                value = new Boolean((String) value);
                            } else if (value instanceof String[]) {
                                String[] array = (String[]) value;
                                int length = array.length;
                                if (length == 0) {
                                    value = null;
                                } else if (length == 1) {
                                    value = new Boolean(array[0]);
                                } else {
                                    Boolean[] booleanValues =
                                            new Boolean[length];
                                    for (int i = 0; i < length; i++) {
                                        booleanValues[i] =
                                                new Boolean(array[i]);
                                    }
                                    value = booleanValues;
                                }
                            } else {
                                value = null;
                            }
                        } else {
                            if (value instanceof String) {
                                // do nothing
                            } else if (value instanceof String[]) {
                                String[] array = (String[]) value;
                                int length = array.length;
                                if (length == 0) {
                                    value = null;
                                } else if (length == 1) {
                                    value = array[0];
                                }
                            } else {
                                value = null;
                            }
                        }
                    }
                    json.key(key);
                    if (value instanceof Object[]) {
                        json.array();
                        for (Object object : (Object[]) value) {
                            json.value(object);
                        }
                        json.endArray();
                    } else if (value != null) {
                        json.value(value);
                    }
                }
                json.endObject();
            }
            json.endArray();
            json.endObject();
            return json.toString();
        } catch (JSONException jsonException) {
            throw new IOException(jsonException.getMessage());
        }
    }

    private ContinuationToken processData(QueryResult queryResult,
            Map<String, Map<String, Object>> data, String type)
                    throws IOException {
        if (queryResult == null) return null;
        QueryResponseHelper[] queryResults =
                queryResult.getQueryResponseHelpers();
        if (queryResults == null || queryResults.length == 0) return null;
        for (QueryResponseHelper result : queryResults) {
            Map<String, Object> resultMap = new HashMap<String, Object>();
            for (QueryResponseColumn column :
                    result.getQueryResponseColumns()) {
                String fieldName = column.getLabel();
                String value = column.getDisplayValue();
                if (value == null || "".equals(value)) {
                    value = column.getValue();
                    if (value == null || "".equals(value)) continue;
                }
                resultMap.put(fieldName, value);
            }
            String internalId = result.getRecordId();
            String key = null;
            if (resultMap.containsKey(ID_KEY)) {
                // if the data source report has an "id" column, use that.
                key = (String) resultMap.get(ID_KEY);
            } else {
                // otherwise, fall back to the internal record ID.
                key = internalId;
            }
            if (key == null || "".equals(key = key.trim())) continue;
            if (!resultMap.containsKey(ID_KEY)) resultMap.put(ID_KEY, key);
            if (!resultMap.containsKey(INTERNAL_ID_KEY)) {
                // add the internal record ID in any case
                resultMap.put(INTERNAL_ID_KEY, internalId);
            }
            if (type != null) resultMap.put(TYPE_KEY, type);
            Map<String, Object> current = data.get(key);
            if (current == null) {
                data.put(key, resultMap);
            } else {
                for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
                    String fieldName = entry.getKey();
                    String value = (String) entry.getValue();
                    if (current.containsKey(fieldName)) {
                        Object currentValue = current.get(fieldName);
                        if (currentValue instanceof String) {
                            if (!currentValue.equals(value)) {
                                String[] array = new String[2];
                                array[0] = (String) currentValue;
                                array[1] = value;
                                current.put(fieldName, array);
                            }
                        } else if (currentValue instanceof String[]) {
                            String[] array = (String[]) currentValue;
                            if (!Arrays.asList(array).contains(value)) {
                                int length = array.length;
                                String[] newArray = new String[length + 1];
                                System.arraycopy(array, 0, newArray, 0, length);
                                newArray[length] = value;
                                current.put(fieldName, newArray);
                            }
                        } else {
                            current.put(fieldName, value);
                        }
                    } else {
                        current.put(fieldName, value);
                    }
                }
            }
        }
        ContinuationToken token = queryResult.getContinuationToken();
        return (token == null || token.getTokenString() == null) ? null :
                token;
    }

    private static class TypeDefinition implements Comparable<TypeDefinition> {

        private final String type;

        private final String plural;

        public TypeDefinition(String type, String plural) {
            this.type = type;
            this.plural = plural;
        }

        public String getType() {
            return type;
        }

        public String getPlural() {
            return plural;
        }

        public boolean equals(Object o) {
            if (!(o instanceof TypeDefinition)) return false;
            String type = getType();
            return (type != null) ?
                    type.equals(((TypeDefinition) o).getType()) :
                            (((TypeDefinition) o).getType() == null);
        }

        public int hashCode() {
            String type = getType();
            return (type != null) ? type.hashCode() : 0;
        }

        public int compareTo(TypeDefinition other) {
            if (other == null) return 1;
            String type = getType();
            String otherType = other.getType();
            if (type != null) {
                return (otherType != null) ? type.compareTo(otherType) : 1;
            }
            return (otherType != null) ? -1 : 0;
        }


    }

}
