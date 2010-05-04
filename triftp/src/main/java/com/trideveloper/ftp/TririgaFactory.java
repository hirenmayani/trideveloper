package com.trideveloper.ftp;

import com.trideveloper.tririga.TririgaClient;
import com.trideveloper.tririga.TririgaClientFactory;

import com.trideveloper.tririga.ws.dto.AssociationFilter;
import com.trideveloper.tririga.ws.dto.DisplayLabel;
import com.trideveloper.tririga.ws.dto.FieldSortOrder;
import com.trideveloper.tririga.ws.dto.Filter;
import com.trideveloper.tririga.ws.dto.QueryResponseHelper;
import com.trideveloper.tririga.ws.dto.QueryResult;

import java.io.InputStream;
import java.io.IOException;

import java.util.Enumeration;
import java.util.Properties;

import static com.trideveloper.ftp.FtpConstants.*;

public abstract class TririgaFactory {

    private static final String RESOURCE = "/META-INF/services/" +
            TririgaFactory.class.getName();

    private static final String USER_GROUP_ASSOCIATION = "Belongs To";

    private static final String DATA_ACCESS_SECTION_NAME = "Data Access";

    private static final String NAME_FIELD_NAME = "Name";

    private static final String NAME_FIELD = DATA_ACCESS_SECTION_NAME + "." +
            NAME_FIELD_NAME;

    private static final String TRIPEOPLE_MODULE_NAME = "triPeople";

    private static final String GROUP_MODULE_NAME = "Group";

    private static final String MY_PROFILE_OBJECT_TYPE_NAME = "My Profile";

    private static final String GROUP_OBJECT_TYPE_NAME = "Group";

    private static final int ALL_PROJECT_SCOPE = 2;

    private static final int STRING_DATA_TYPE = 320;

    private static final int EQUALS_OPERATOR = 10;

    private String server;

    public static TririgaFactory newInstance() {
        String factoryClass = null;
        try {
            factoryClass = System.getProperty(TririgaFactory.class.getName());
        } catch (SecurityException ex) { }
        if (factoryClass == null) {
            try {
                InputStream resource =
                        TririgaFactory.class.getResourceAsStream(RESOURCE);
                if (resource == null) {
                    resource = ClassLoader.getSystemResourceAsStream(RESOURCE);
                }
                if (resource != null) {
                    Properties properties = new Properties();
                    properties.load(resource);
                    Enumeration propertyNames = properties.propertyNames();
                    if (propertyNames.hasMoreElements()) {
                        factoryClass = (String) propertyNames.nextElement();
                    }
                }
            } catch (Exception ex) { }
        }
        if (factoryClass != null) {
            try {
                return (TririgaFactory)
                        Class.forName(factoryClass).newInstance();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IllegalStateException(ex.getMessage());
            }
        }
        return new DefaultTririgaFactory();
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public abstract TririgaFile createFile(String path,
            TririgaCredentials auth);

    public abstract TririgaFile createFile(TririgaFile base, String name);

    public abstract TririgaCredentials authenticate(TririgaCredentials auth)
            throws TririgaAuthenticationException;

    private static class DefaultTririgaFactory extends TririgaFactory {

        private final String ftpGroup;

        public DefaultTririgaFactory() {
            String group = Configuration.getString(FTP_GROUP_PROPERTY);
            if (group == null) group = DEFAULT_FTP_GROUP;
            ftpGroup = group;
        }

        public TririgaFile createFile(String path, TririgaCredentials auth) {
            TririgaClientFactory factory = TririgaClientFactory.getInstance(
                    getServer(), auth.getName(), auth.getPassword());
            return new DefaultTririgaFile(factory, path);
        }

        public TririgaFile createFile(TririgaFile base, String name) {
            return new DefaultTririgaFile((DefaultTririgaFile) base, name);
        }

        public TririgaCredentials authenticate(TririgaCredentials auth)
                throws TririgaAuthenticationException {
            try {
                String username = auth.getName();
                String password = auth.getPassword();
                TririgaClientFactory factory = TririgaClientFactory.getInstance(
                        getServer(), username, password);
                int index = username.indexOf('\\');
                TririgaClient client = null;
                if (index == -1) {
                    client = factory.newTririgaClient();
                } else {
                    try {
                        client = factory.newTririgaClient();
                    } catch (IOException e) {
                        // strip windows domain if present
                        username = username.substring(index + 1);
                        auth = new TririgaCredentials(username, password);
                        factory = TririgaClientFactory.getInstance(getServer(),
                                username, password);
                        client = factory.newTririgaClient();
                    }
                }
                String projectName = "";
                String moduleName = GROUP_MODULE_NAME;
                String[] objectTypeNames = new String[] {
                    GROUP_OBJECT_TYPE_NAME
                };
                String[] guiNames = null;
                String associatedModuleName = "";
                String associatedObjectTypeName = "";
                int projectScope = ALL_PROJECT_SCOPE;
                DisplayLabel[] displayFields = new DisplayLabel[] {
                    Util.createDisplayLabel(NAME_FIELD)
                };
                Filter[] filters = new Filter[] {
                    Util.createFieldFilter(NAME_FIELD, STRING_DATA_TYPE,
                            EQUALS_OPERATOR, ftpGroup)
                };
                DisplayLabel[] associatedDisplayFields = null;
                FieldSortOrder fieldSortOrder = new FieldSortOrder();
                fieldSortOrder.setDataType(STRING_DATA_TYPE);
                fieldSortOrder.setFieldLabel(NAME_FIELD);
                fieldSortOrder.setFieldName(NAME_FIELD_NAME);
                fieldSortOrder.setSectionName(DATA_ACCESS_SECTION_NAME);
                FieldSortOrder[] fieldSortOrders =
                        new FieldSortOrder[] { fieldSortOrder };
                AssociationFilter associationFilter = new AssociationFilter();
                associationFilter.setAssociationName(USER_GROUP_ASSOCIATION);
                associationFilter.setModuleName(TRIPEOPLE_MODULE_NAME);
                associationFilter.setObjectTypeName(
                        MY_PROFILE_OBJECT_TYPE_NAME);
                associationFilter.setReverseAssociation(false);
                //associationFilter.setRunTimeData("$$USERID$$");
                associationFilter.setRunTimeData(
                        String.valueOf(client.getUserId()));
                AssociationFilter[] associationFilters =
                        new AssociationFilter[] { associationFilter };
                int start = 1;
                int maximumResultCount = 1;
                QueryResult queryResult = client.runDynamicQuery(projectName,
                        moduleName, objectTypeNames, guiNames,
                        associatedModuleName, associatedObjectTypeName,
                        projectScope, displayFields, associatedDisplayFields,
                        fieldSortOrders, filters, associationFilters, start,
                        maximumResultCount);
                if (queryResult == null) {
                    throw new TririgaAuthenticationException(
                            "User is not a member of authorized group \"" +
                                    ftpGroup + "\".");
                }
                QueryResponseHelper[] responses =
                        queryResult.getQueryResponseHelpers();
                if (responses == null || responses.length < 1) {
                    throw new TririgaAuthenticationException(
                            "User is not a member of authorized group \"" +
                                    ftpGroup + "\".");
                }
                return auth;
            } catch (IOException ex) {
                throw new TririgaAuthenticationException(ex.getMessage());
            }
        }

    }
}
