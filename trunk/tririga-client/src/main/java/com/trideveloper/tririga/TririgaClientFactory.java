package com.trideveloper.tririga;

import com.trideveloper.tririga.api1.BoRecordActionsInterface;
import com.trideveloper.tririga.api1.BoRecordActionsInterfaceServiceLocator;
import com.trideveloper.tririga.api1.CadQueryInterface;
import com.trideveloper.tririga.api1.CadQueryInterfaceServiceLocator;
import com.trideveloper.tririga.api1.QueryActionsInterface;
import com.trideveloper.tririga.api1.QueryActionsInterfaceServiceLocator;
import com.trideveloper.tririga.api1.WFTaskActionsInterface;
import com.trideveloper.tririga.api1.WFTaskActionsInterfaceServiceLocator;

import com.trideveloper.tririga.ws.TririgaWSLocator;
import com.trideveloper.tririga.ws.TririgaWSPortType;

import com.trideveloper.tririga.ws.dto.AssociationFilter;
import com.trideveloper.tririga.ws.dto.DisplayLabel;
import com.trideveloper.tririga.ws.dto.FieldSortOrder;
import com.trideveloper.tririga.ws.dto.Filter;
import com.trideveloper.tririga.ws.dto.HttpSession;
import com.trideveloper.tririga.ws.dto.QueryResponseHelper;
import com.trideveloper.tririga.ws.dto.QueryResult;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;

import javax.xml.rpc.Stub;

/**
 * This is a client factory for obtaining TRIRIGA API client instances.
 */
public class TririgaClientFactory {

    private static final String BUSINESS_CONNECT_URI = "/ws/TririgaWS";

    private static final String BO_RECORD_URI = "/services/BoRecord";

    private static final String CAD_QUERY_URI = "/services/CadQuery";

    private static final String QUERY_URI = "/services/Query";

    private static final String WF_TASK_URI = "/services/WFTaskActions";

    private static final String DOCUMENT_URI =
            "/WebProcess.srv?objectId=410000&actionId=410014&disp=false" +
                    "&documentID=";

    private static final String RENDER_URI =
            "/WebProcess.srv?objectId=950000&actionId=950013&docId=";

    private static final String DOCUMENT_MODULE = "Document";

    private static final String DOCUMENT_TYPE = "Document";

    private static final String DOCUMENT_PATH_SECTION = "General Info";

    private static final String DOCUMENT_PATH_FIELD = "DM_PATH";

    private static final long FIXED_COMPANY_ID = 208133l;

    private static final int EQUALS_OPERATOR = 10;

    private static final int STRING_DATA_TYPE = 320;

    private static final int ALL_PROJECT_SCOPE = 2;

    private static final Map FACTORY_CACHE = new HashMap();

    private final URL url;

    private final String username;

    private final String password;

    private String session;

    private HttpSession httpSession;

    /**
     * Obtains a client factory for the specified server, username, and
     * password.  Client factories are thread-safe, and can be used across
     * multiple threads; note, however, that clients themselves (returned
     * by the <code>newTririgaClient</code> call) are <i>not</i> thread-safe;
     * a new client should be created for each thread.
     */
    public static TririgaClientFactory getInstance(String server,
            String username, String password) {
        try {
            if (username == null) username = "";
            if (password == null) password = "";
            URL url = new URL(new URL(server), BUSINESS_CONNECT_URI);
            TririgaConnection connection = new TririgaConnection(url, username,
                    password);
            TririgaClientFactory factory;
            synchronized (FACTORY_CACHE) {
                factory = (TririgaClientFactory) FACTORY_CACHE.get(connection);
                if (factory == null) {
                    factory = new TririgaClientFactory(connection.url,
                            connection.username, connection.password);
                    FACTORY_CACHE.put(connection, factory);
                }
            }
            return factory;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    private TririgaClientFactory(URL url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * Creates a new client object.  Clients are not thread-safe, so this
     * call should be used to create a new client object for each thread.
     */
    public TririgaClient newTririgaClient() throws IOException {
        synchronized (this) {
            try {
                TririgaWSPortType connection = newConnection(); 
                if (httpSession == null) {
                    throw new IllegalStateException("No TRIRIGA Session.");
                }
                URL endpoint = new URL(url, BO_RECORD_URI);
                BoRecordActionsInterfaceServiceLocator boRecordLocator =
                        new BoRecordActionsInterfaceServiceLocator();
                BoRecordActionsInterface boRecord =
                        boRecordLocator.getBoRecord(endpoint);
                endpoint = new URL(url, CAD_QUERY_URI);
                CadQueryInterfaceServiceLocator cadQueryLocator =
                        new CadQueryInterfaceServiceLocator();
                CadQueryInterface cadQuery =
                        cadQueryLocator.getCadQuery(endpoint);
                endpoint = new URL(url, QUERY_URI);
                QueryActionsInterfaceServiceLocator queryLocator =
                        new QueryActionsInterfaceServiceLocator();
                QueryActionsInterface query = queryLocator.getQuery(endpoint);
                endpoint = new URL(url, WF_TASK_URI);
                WFTaskActionsInterfaceServiceLocator wfTaskLocator =
                        new WFTaskActionsInterfaceServiceLocator();
                WFTaskActionsInterface wfTask =
                        wfTaskLocator.getWFTaskActions(endpoint);
                TririgaDocumentClient docClient =
                        new BasicDocumentClient(connection, session, url);
                TririgaProxyClient client = new TririgaProxyClient(httpSession,
                        connection, boRecord, cadQuery, query, wfTask,
                                docClient);
                return (TririgaClient) Proxy.newProxyInstance(
                        TririgaClient.class.getClassLoader(),
                                new Class[] { TririgaClient.class }, client);
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    private TririgaWSPortType newConnection() throws IOException {
        synchronized (this) {
            try {
                TririgaWSLocator locator = new TririgaWSLocator();
                if (session != null) {
                    TririgaWSPortType client =
                            locator.getTririgaWSHttpPort(url);
                    Stub stub = (Stub) client;
                    stub._setProperty(Stub.SESSION_MAINTAIN_PROPERTY,
                            Boolean.TRUE);
                    stub._setProperty("Cookie", session);
                    try {
                        httpSession = client.getHttpSession();
                        return client;
                    } catch (Exception ex) {
                        session = null;
                        httpSession = null;
                    }
                }
                HttpURLConnection conn = (HttpURLConnection)
                        url.openConnection();
                conn.connect();
                String cookie = null;
                Map headers = conn.getHeaderFields();
                Iterator entries = headers.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    String headerName = (String) entry.getKey();
                    if (!"set-cookie".equalsIgnoreCase(headerName)) continue;
                    List headerValues = (List) entry.getValue();
                    Iterator values = headerValues.iterator();
                    while (values.hasNext()) {
                        String value = (String) values.next();
                        int index = value.indexOf("JSESSIONID=");
                        if (index == -1) continue;
                        cookie = value.substring(index).replaceFirst(";.*", "");
                    }
                }
                TririgaWSPortType client = locator.getTririgaWSHttpPort(url);
                Stub stub = (Stub) client;
                stub._setProperty(Stub.USERNAME_PROPERTY, username);
                stub._setProperty(Stub.PASSWORD_PROPERTY, password);
                stub._setProperty(Stub.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
                stub._setProperty("Cookie", cookie);
                httpSession = client.getHttpSession();
                client = locator.getTririgaWSHttpPort(url);
                stub = (Stub) client;
                stub._setProperty(Stub.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
                stub._setProperty("Cookie", cookie);
                session = cookie;
                return client;
            } catch (RuntimeException ex) {
                throw ex;
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof TririgaClientFactory)) return false;
        TririgaClientFactory other = (TririgaClientFactory) o;
        TririgaConnection connection =
                new TririgaConnection(url, username, password);
        TririgaConnection otherConnection =
                new TririgaConnection(other.url, other.username,
                        other.password);
        return connection.equals(otherConnection);
    }

    public int hashCode() {
        int hashCode = (url != null) ? url.hashCode() : 0;
        hashCode ^= (username != null) ? username.hashCode() : 0;
        return hashCode ^ ((password != null) ? password.hashCode() : 0);
    }

    private static class BasicDocumentClient implements TririgaDocumentClient {

        private final TririgaWSPortType tririga;

        private final String session;

        private final URL url;

        public BasicDocumentClient(TririgaWSPortType tririga, String session,
                URL url) {
            this.tririga = tririga;
            this.session = session;
            this.url = url;
        }

        public DataSource getDocument(String path) throws IOException {
            if (path == null) {
                throw new NullPointerException("Null document path.");
            }
            long docId = getDocumentRecordId(path);
            if (docId < 0l) throw new FileNotFoundException(path);
            if (session == null) {
                throw new IllegalStateException("Session not established.");
            }
            return new DocumentDataSource(path, session,
                    new URL(url, DOCUMENT_URI + docId));
        }

        public DataSource getDocument(long docId) throws IOException {
            if (docId < 0l) {
                throw new FileNotFoundException(String.valueOf(docId));
            }
            if (session == null) {
                throw new IllegalStateException("Session not established.");
            }
            return new DocumentDataSource(String.valueOf(docId), session,
                    new URL(url, DOCUMENT_URI + docId));
        }

        public DataSource getDocument(String path, long recordId)
                throws IOException {
            if (path == null) {
                throw new NullPointerException("Null document path.");
            }
            if (recordId < 0l) {
                throw new IllegalArgumentException("Invalid record ID: " +
                        recordId);
            }
            long docId = getDocumentRecordId(path);
            if (docId < 0l) throw new FileNotFoundException(path);
            if (session == null) {
                throw new IOException("Session not established.");
            }
            return new DocumentDataSource(path, session,
                    new URL(url, RENDER_URI + docId + "&specId=" + recordId));
        }

        public DataSource getDocument(long docId, long recordId)
                throws IOException {
            if (recordId < 0l) {
                throw new IllegalArgumentException("Invalid record ID: " +
                        recordId);
            }
            if (docId < 0l) {
                throw new FileNotFoundException(String.valueOf(docId));
            }
            if (session == null) {
                throw new IOException("Session not established.");
            }
            return new DocumentDataSource(String.valueOf(docId), session,
                    new URL(url, RENDER_URI + docId + "&specId=" + recordId));
        }

        private long getDocumentRecordId(String path) throws IOException {
            String projectName = "";
            String moduleName = DOCUMENT_MODULE;
            String[] objectTypeNames = new String[] { DOCUMENT_TYPE };
            String[] guiNames = null;
            String associatedModuleName = "";
            String associatedObjectTypeName = "";
            int projectScope = ALL_PROJECT_SCOPE;
            DisplayLabel[] displayFields = new DisplayLabel[] {
                new DisplayLabel(DOCUMENT_PATH_FIELD, DOCUMENT_PATH_FIELD,
                        DOCUMENT_PATH_SECTION)
            };
            Filter[] filters = new Filter[] {
                new Filter(STRING_DATA_TYPE, DOCUMENT_PATH_FIELD,
                        EQUALS_OPERATOR, DOCUMENT_PATH_SECTION, path)
            };
            DisplayLabel[] associatedDisplayFields = null;
            FieldSortOrder[] fieldSortOrders = null;
            AssociationFilter[] associationFilters = null;
            int start = 1;
            int maxResults = 1;
            QueryResult queryResult = tririga.runDynamicQuery(projectName,
                    moduleName, objectTypeNames, guiNames,
                    associatedModuleName, associatedObjectTypeName,
                    projectScope, displayFields, associatedDisplayFields,
                    fieldSortOrders, filters, associationFilters, start,
                    maxResults);
            QueryResponseHelper[] queryResults =
                    queryResult.getQueryResponseHelpers();
            if (queryResults == null || queryResults.length == 0) return -1l;
            QueryResponseHelper result = queryResults[0];
            if (result == null) return -1l;
            try {
                return Long.parseLong(result.getRecordId());
            } catch (Exception ex) {
                return -1l;
            }
        }

    }

    private static class DocumentDataSource implements DataSource {

        private final String path;

        private final String session;

        private final URL url;

        private final String contentType;

        public DocumentDataSource(String path, String session, URL url)
                throws IOException {
            this.path = path;
            this.session = session;
            this.url = url;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Cookie", session);
            conn.setRequestMethod("HEAD");
            conn.connect();
            String contentType = conn.getContentType();
            InputStream stream = null;
            try {
                stream = conn.getInputStream();
            } catch (IOException ex) {
                stream = conn.getErrorStream();
            }
            if (stream != null) {
                byte[] buf = new byte[65536];
                int count;
                try {
                    while ((count = stream.read(buf, 0, 65536)) != -1);
                } catch (Exception ignore) { }
                try {
                    stream.close();
                } catch (Exception ignore) { }
            }
            this.contentType = (contentType != null) ? contentType :
                    "application/octet-stream";
        }

        public String getName() {
            return path;
        }

        public String getContentType() {
            return contentType;
        }

        public InputStream getInputStream() throws IOException {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Cookie", session);
            conn.connect();
            String error = null;
            InputStream stream = null;
            try {
                stream = conn.getInputStream();
            } catch (IOException ex) {
                int responseCode = -1;
                try {
                    responseCode = conn.getResponseCode();
                } catch (Exception ignore) { }
                error = "Encountered response " + responseCode +
                        " retrieving content: " + ex.getMessage();
                try {
                    stream = conn.getErrorStream();
                } catch (Exception ignore) { }
            }
            if (stream == null) {
                throw new IOException((error != null) ? error :
                        "No content stream.");
            }
            if (error == null) return stream;
            byte[] buf = new byte[65536];
            int count;
            try {
                while ((count = stream.read(buf, 0, 65536)) != -1);
            } catch (Exception ignore) { }
            try {
                stream.close();
            } catch (Exception ignore) { }
            throw new IOException(error);
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("Write not supported.");
        }

    }

    private static class TririgaConnection {

        public final URL url;

        public final String username;

        public final String password;

        public TririgaConnection(URL url, String username, String password) {
            this.url = url;
            this.username = (username != null) ? username : "";
            this.password = (password != null) ? password : "";
            if (url == null) throw new NullPointerException();
        }

        public boolean equals(Object o) {
            if (!(o instanceof TririgaConnection)) return false;
            TririgaConnection other = (TririgaConnection) o;
            return url.equals(other.url) && username.equals(other.username) &&
                    password.equals(other.password);
        }

        public int hashCode() {
            return url.hashCode() ^ username.hashCode() ^ password.hashCode();
        }

    }

    private static class TririgaProxyClient implements InvocationHandler {

        private final Map<Method, Object> methodMap =
                new HashMap<Method, Object>();

        private final long userId;

        private final String securityToken;

        public TririgaProxyClient(HttpSession httpSession,
                TririgaWSPortType businessConnect,
                BoRecordActionsInterface boRecord,
                CadQueryInterface cadQuery,
                QueryActionsInterface query,
                WFTaskActionsInterface wfTask,
                TririgaDocumentClient docClient) {
            this.userId = httpSession.getId();
            this.securityToken = httpSession.getToken();
            for (Method method :
                    BoRecordActionsInterface.class.getDeclaredMethods()) {
                methodMap.put(method, boRecord);
            }
            for (Method method : CadQueryInterface.class.getDeclaredMethods()) {
                methodMap.put(method, cadQuery);
            }
            for (Method method :
                    QueryActionsInterface.class.getDeclaredMethods()) {
                methodMap.put(method, query);
            }
            for (Method method :
                    WFTaskActionsInterface.class.getDeclaredMethods()) {
                methodMap.put(method, wfTask);
            }
            for (Method method :
                    TririgaDocumentClient.class.getDeclaredMethods()) {
                methodMap.put(method, docClient);
            }
            for (Method method : TririgaWSPortType.class.getDeclaredMethods()) {
                methodMap.put(method, businessConnect);
            }
        }

        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            Object target = methodMap.get(method);
            if (target != null) return method.invoke(target, args);
            Method redirect = getClass().getDeclaredMethod(method.getName(),
                    method.getParameterTypes());
            return redirect.invoke(this, args);
        }

        public String getSecurityToken() {
            return securityToken;
        }

        public long getUserId() {
            return userId;
        }

        public long getCompanyId() {
            return FIXED_COMPANY_ID;
        }

    }

}
