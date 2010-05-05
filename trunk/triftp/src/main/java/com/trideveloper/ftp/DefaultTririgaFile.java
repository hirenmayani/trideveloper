package com.trideveloper.ftp;

import com.trideveloper.tririga.TririgaClient;
import com.trideveloper.tririga.TririgaClientFactory;

import com.trideveloper.tririga.ws.dto.AssociationFilter;
import com.trideveloper.tririga.ws.dto.ContinuationToken;
import com.trideveloper.tririga.ws.dto.DisplayLabel;
import com.trideveloper.tririga.ws.dto.FieldSortOrder;
import com.trideveloper.tririga.ws.dto.Filter;
import com.trideveloper.tririga.ws.dto.IntegrationField;
import com.trideveloper.tririga.ws.dto.IntegrationRecord;
import com.trideveloper.tririga.ws.dto.IntegrationSection;
import com.trideveloper.tririga.ws.dto.QueryResponseColumn;
import com.trideveloper.tririga.ws.dto.QueryResponseHelper;
import com.trideveloper.tririga.ws.dto.QueryResult;
import com.trideveloper.tririga.ws.dto.ResponseHelper;
import com.trideveloper.tririga.ws.dto.ResponseHelperHeader;
import com.trideveloper.tririga.ws.dto.TriggerActions;

import com.trideveloper.tririga.ws.dto.content.Content;
import com.trideveloper.tririga.ws.dto.content.Response;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.trideveloper.ftp.FtpConstants.*;

public class DefaultTririgaFile implements TririgaFile {

    private static final String MODIFIED_FORMAT = "MM/dd/yyyy h:mm:ss a";

    private static final String PARENT_CHILD_ASSOCIATION = "Is Parent Of";

    private static final String APPLY_ACTION = "APPLY";

    private static final String CREATE_ACTION = "CREATE";

    private static final String DELETE_ACTION = "DELETE";

    private static final String FINAL_DELETE_ACTION = "FINAL_DELETE";

    private static final String DELETE_LABEL = "Delete";

    private static final String FINAL_DELETE_LABEL = "Final Delete";

    private static final String DELETED_STATE = "Deleted";

    private static final String CHECKED_IN_STATUS = "Checked In";

    private static final String DOCUMENT_MODULE_NAME = "Document";

    private static final String ROOT_OBJECT_TYPE_NAME = "ROOT";

    private static final String DOCUMENT_OBJECT_TYPE_NAME = "Document";

    private static final String FOLDER_OBJECT_TYPE_NAME = "Folder";

    private static final String ROOT_PATH = "\\ROOT";

    private static final String RECORD_INFORMATION_SECTION_NAME =
            "RecordInformation";

    private static final String GENERAL_INFO_SECTION_NAME =
            "General Info";

    private static final String PATH_FIELD_NAME = "DM_PATH";

    private static final String PATH_FIELD = GENERAL_INFO_SECTION_NAME + "." +
            PATH_FIELD_NAME;

    private static final String RECORD_STATE_FIELD_NAME = "triRecordStateSY";

    private static final String RECORD_STATE_FIELD =
            RECORD_INFORMATION_SECTION_NAME + "." + RECORD_STATE_FIELD_NAME;

    private static final String STATUS_FIELD_NAME = "DM_FILE_STATUS";

    private static final String SIZE_FIELD_NAME = "DM_FILE_SIZE";

    private static final String FILENAME_FIELD_NAME = "DM_FILE_NAME";

    private static final String NAME_FIELD_NAME = "Name";

    private static final String NAME_FIELD = GENERAL_INFO_SECTION_NAME + "." +
            NAME_FIELD_NAME;

    private static final String OBJECT_TYPE_FIELD_NAME = "BOTypeName";

    private static final String OBJECT_TYPE_FIELD =
            RECORD_INFORMATION_SECTION_NAME + "." + OBJECT_TYPE_FIELD_NAME;

    private static final String MODIFIED_FIELD_NAME = "SysModifiedDateTime";

    private static final String MODIFIED_FIELD =
            RECORD_INFORMATION_SECTION_NAME + "." + MODIFIED_FIELD_NAME;

    private static final String GLOBAL_ROOT = getGlobalRoot();

    private static final int ALL_PROJECT_SCOPE = 2;

    private static final int STRING_DATA_TYPE = 320;

    private static final int EQUALS_OPERATOR = 10;

    private static final int NOT_EQUALS_OPERATOR = 11;

    private static int documentModuleId = -1;

    private static long documentObjectTypeId = -1l;

    private static long folderObjectTypeId = -1l;

    private final TririgaClientFactory factory;

    private final String parentPath;

    private final String name;

    private TririgaClient client;

    private long id = -1l;

    private long modified = 0l;

    private boolean folder = false;

    public DefaultTririgaFile(TririgaClientFactory factory, String path) {
        this(factory, null, path, -1l, 0l, false);
    }

    public DefaultTririgaFile(DefaultTririgaFile base, String relative) {
        this(base.factory, null, base.toString() + "/" + relative, -1l, 0l,
                false);
    }

    private DefaultTririgaFile(TririgaClientFactory factory,
            TririgaClient client, String path, long id, long modified,
                    boolean folder) {
        this.factory = factory;
        this.client = client;
        path = path.replace('\\', '/');
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        int index = path.lastIndexOf("/");
        if (index != -1) {
            this.parentPath = path.substring(0, index);
            this.name = path.substring(index + 1);
        } else {
            this.parentPath = "";
            this.name = path;
        }
        this.id = id;
        this.modified = modified;
        this.folder = folder;
    }

    public InputStream getInputStream() throws IOException {
        if (!isFile()) throw new FileNotFoundException(toString());
        assert (id != -1l);
        Content content = new Content();
        content.setRecordId(id);
        final File temp = File.createTempFile("sync", null);
        temp.deleteOnExit();
        try {
            content.setPath(temp.getCanonicalPath());
            Response[] responses = getClient().downloadTo(
                    new Content[] { content });
            if (!responses[0].getStatus().equalsIgnoreCase("success")) {
                return new ByteArrayInputStream(new byte[0]) {
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            temp.delete();
                        }
                    }
                };
            }
            return new FilterInputStream(new FileInputStream(temp)) {
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        temp.delete();
                    }
                }
            };
        } catch (IOException ex) {
            temp.delete();
            throw ex;
        } catch (Exception ex) {
            temp.delete();
            throw new IOException(ex.getMessage());
        }
    }

    public OutputStream getOutputStream() throws IOException {
        if (!exists()) {
            createNewFile();
        }
        if (!isFile()) {
            throw new IOException("Unable to obtain file: " + toString());
        }
        assert (id != -1l);
        final TririgaClient client = getClient();
        synchronized (DefaultTririgaFile.class) {
            if (documentModuleId == -1) {
                documentModuleId = client.getModuleId(DOCUMENT_MODULE_NAME);
            }
            if (documentObjectTypeId == -1l) {
                documentObjectTypeId = client.getObjectTypeId(
                        DOCUMENT_MODULE_NAME, DOCUMENT_OBJECT_TYPE_NAME);
            }
        }
        assert (documentModuleId != -1);
        assert (documentObjectTypeId != -1l);
        final File temp = File.createTempFile("sync", null);
        final Content content = new Content();
        content.setRecordId(id);
        temp.deleteOnExit();
        try {
            return new FilterOutputStream(new FileOutputStream(temp)) {
                public void close() throws IOException {
                    try {
                        super.close();
                        long fileSize = temp.length();
                        Response response;
                        if (fileSize > 0l) {
                            content.setPath(temp.getCanonicalPath());
                            Response[] responses = client.uploadFrom(
                                    new Content[] { content });
                            response = responses[0];
                        } else {
                            response = client.delete(content);
                        }
                        if (fileSize > 0l &&
                                !response.getStatus().equalsIgnoreCase(
                                        "success")) {
                            throw new FileNotFoundException(
                                    DefaultTririgaFile.this.toString());
                        }
                        IntegrationField sizeField = new IntegrationField();
                        sizeField.setName(SIZE_FIELD_NAME);
                        sizeField.setValue(String.valueOf(fileSize));
                        IntegrationField[] generalInfoFields =
                                new IntegrationField[] { sizeField };
                        IntegrationSection generalInfo =
                                new IntegrationSection();
                        generalInfo.setName(GENERAL_INFO_SECTION_NAME);
                        generalInfo.setFields(generalInfoFields);
                        IntegrationSection[] sections =
                                new IntegrationSection[] { generalInfo };
                        IntegrationRecord integrationRecord =
                                new IntegrationRecord();
                        integrationRecord.setId(content.getRecordId());
                        integrationRecord.setSections(sections);
                        integrationRecord.setActionName("");
                        integrationRecord.setModuleId(documentModuleId);
                        integrationRecord.setObjectTypeId(documentObjectTypeId);
                        integrationRecord.setObjectTypeName(
                                DOCUMENT_OBJECT_TYPE_NAME);
                        integrationRecord.setObjectPath(toPath(getParent()));
                        try {
                            client.saveRecord(new IntegrationRecord[] {
                                    integrationRecord });
                        } catch (Exception ignore) { }
                    } finally {
                        temp.delete();
                    }
                }
            };
        } catch (IOException ex) {
            temp.delete();
            throw ex;
        } catch (Exception ex) {
            temp.delete();
            throw new IOException(ex.getMessage());
        }
    }

    public String getName() {
        return name;
    }

    public String getParent() {
        return parentPath;
    }

    public TririgaFile[] listFiles() throws IOException {
        if (!isDirectory()) return null;
        assert (id != -1l);
        TririgaClient client = getClient();
        String projectName = "";
        String moduleName = DOCUMENT_MODULE_NAME;
        String[] objectTypeNames = new String[] {
            DOCUMENT_OBJECT_TYPE_NAME, FOLDER_OBJECT_TYPE_NAME
        };
        String[] guiNames = null;
        String associatedModuleName = "";
        String associatedObjectTypeName = "";
        int projectScope = ALL_PROJECT_SCOPE;
        DisplayLabel[] displayFields = new DisplayLabel[] {
            Util.createDisplayLabel(NAME_FIELD),
            Util.createDisplayLabel(OBJECT_TYPE_FIELD),
            Util.createDisplayLabel(MODIFIED_FIELD)
        };
        Filter[] filters = new Filter[] {
            Util.createFieldFilter(RECORD_STATE_FIELD, STRING_DATA_TYPE,
                    NOT_EQUALS_OPERATOR, DELETED_STATE),
            Util.createFieldFilter(RECORD_STATE_FIELD, STRING_DATA_TYPE,
                    NOT_EQUALS_OPERATOR, "")
        };
        DisplayLabel[] associatedDisplayFields = null;
        FieldSortOrder fieldSortOrder = new FieldSortOrder();
        fieldSortOrder.setDataType(STRING_DATA_TYPE);
        fieldSortOrder.setFieldLabel(NAME_FIELD);
        fieldSortOrder.setFieldName(NAME_FIELD_NAME);
        fieldSortOrder.setSectionName(GENERAL_INFO_SECTION_NAME);
        FieldSortOrder[] fieldSortOrders =
                new FieldSortOrder[] { fieldSortOrder };
        AssociationFilter associationFilter = new AssociationFilter();
        associationFilter.setAssociationName(PARENT_CHILD_ASSOCIATION);
        associationFilter.setModuleName(DOCUMENT_MODULE_NAME);
        associationFilter.setObjectTypeName(null);
        associationFilter.setReverseAssociation(false);
        associationFilter.setRunTimeData(String.valueOf(id));
        AssociationFilter[] associationFilters =
                new AssociationFilter[] { associationFilter };
        int start = 1;
        int maximumResultCount = 100;
        QueryResult queryResult = client.runDynamicQuery(projectName,
                moduleName, objectTypeNames, guiNames, associatedModuleName,
                        associatedObjectTypeName, projectScope, displayFields,
                                associatedDisplayFields, fieldSortOrders,
                                        filters, associationFilters, start,
                                                maximumResultCount);
        List<TririgaFile> children = new ArrayList<TririgaFile>();
        ContinuationToken token;
        while ((token = processChildren(queryResult, children)) != null) {
            queryResult = client.runDynamicQueryContinue(token);
        }
        return children.toArray(new TririgaFile[children.size()]);
    }

    public long length() throws IOException {
        TririgaClient client = getClient();
        try {
            if (!exists()) return 0l;
            assert (id != -1l);
            Content content = new Content();
            content.setRecordId(id);
            return client.getContentLength(content);
        } catch (Exception ex) {
            return 0l;
        }
    }

    public long lastModified() throws IOException {
        try {
            return exists() ? modified : 0l;
        } catch (IOException ex) {
            return 0l;
        }
    }

    public void copyTo(TririgaFile dest) throws IOException {
        if (!exists()) throw new FileNotFoundException(toString());
        if (dest.exists()) throw new IOException("Target exists: " + dest);
        if (isDirectory()) {
            TririgaFile[] children = listFiles();
            dest.mkdir();
            if (!dest.isDirectory()) {
                throw new FileNotFoundException("Unable to create target \"" +
                        dest + "\".");
            }
            for (TririgaFile child : children) {
                TririgaFile targetChild = new DefaultTririgaFile(
                        (DefaultTririgaFile) dest, child.getName());
                child.copyTo(targetChild);
            }
        } else {
            dest.createNewFile();
            long targetId = ((DefaultTririgaFile) dest).id;
            assert (id != -1l);
            assert (targetId != -1l);
            String result = getClient().copy(id, targetId);
            if (result == null || !"success".equalsIgnoreCase(result)) {
                throw new FileNotFoundException(dest.toString());
            }
        }
    }

    public void moveTo(TririgaFile dest) throws IOException {
        if (!exists()) throw new FileNotFoundException(toString());
        if (dest.exists()) throw new IOException("Target exists: " + dest);
        String targetName = dest.getName();
        if (targetName == null || "".equals(targetName.trim())) {
            throw new IOException("Invalid empty filename.");
        }
        if (getParent().equals(dest.getParent())) {
            rename(targetName);
            return;
        }
        if (isDirectory()) {
            TririgaFile[] children = listFiles();
            dest.mkdir();
            if (!dest.isDirectory()) {
                throw new FileNotFoundException(
                        "Unable to create target \"" + dest + "\".");
            }
            for (TririgaFile child : children) {
                TririgaFile targetChild = new DefaultTririgaFile(
                        (DefaultTririgaFile) dest, child.getName());
                child.copyTo(targetChild);
            }
        } else {
            dest.createNewFile();
            long targetId = ((DefaultTririgaFile) dest).id;
            assert (id != -1l);
            assert (targetId != -1l);
            String result = getClient().copy(id, targetId);
            if (result == null || !"success".equalsIgnoreCase(result)) {
                throw new FileNotFoundException(dest.toString());
            }
        }
        delete();
    }

    public boolean delete() throws IOException {
        if (!exists()) return false;
        assert (id != -1l);
        TririgaClient client = getClient();
        synchronized (DefaultTririgaFile.class) {
            if (documentObjectTypeId == -1l) {
                documentObjectTypeId = client.getObjectTypeId(
                        DOCUMENT_MODULE_NAME, DOCUMENT_OBJECT_TYPE_NAME);
            }
            if (folderObjectTypeId == -1l) {
                folderObjectTypeId = client.getObjectTypeId(
                        DOCUMENT_MODULE_NAME, FOLDER_OBJECT_TYPE_NAME);
            }
        }
        assert (documentObjectTypeId != -1l);
        assert (folderObjectTypeId != -1l);
        Content content = new Content();
        content.setRecordId(id);
        try {
            client.delete(content);
            try {
                client.doBoRecordAction(client.getSecurityToken(),
                        client.getUserId(), client.getCompanyId(),
                                id, this.folder ? folderObjectTypeId :
                                        documentObjectTypeId, DELETE_ACTION,
                                                DELETE_LABEL, 0);
            } catch (Exception ex) { }
            try {
                client.doBoRecordAction(client.getSecurityToken(),
                        client.getUserId(), client.getCompanyId(),
                                id, this.folder ? folderObjectTypeId :
                                        documentObjectTypeId,
                                                FINAL_DELETE_ACTION,
                                                        FINAL_DELETE_LABEL, 0);
            } catch (Exception ex) { }
            // Below works, but only if you make "Delete" and "Final Delete"
            // default display on the Document and Folder GUIs
            /*
            TriggerActions trigger = new TriggerActions();
            trigger.setRecordId(id);
            trigger.setActionName(DELETE_ACTION);
            try {
                client.triggerActions(new TriggerActions[] { trigger });
            } catch (Exception ex) { }
            trigger.setActionName(FINAL_DELETE_ACTION);
            try {
                client.triggerActions(new TriggerActions[] { trigger });
            } catch (Exception ex) { }
            */
            id = -1l;
            this.modified = 0l;
            this.folder = false;
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isFile() throws IOException {
        if (!exists()) return false;
        return !folder;
    }

    public boolean isDirectory() throws IOException {
        if (!exists()) return false;
        return folder;
    }

    public boolean exists() throws IOException {
        if (id != -1l) return true;
        String path = toPath(toString());
        TririgaClient client = getClient();
        String projectName = "";
        String moduleName = DOCUMENT_MODULE_NAME;
        String[] objectTypeNames = ROOT_PATH.equals(path) ?
                new String[] { ROOT_OBJECT_TYPE_NAME } :
                        new String[] { DOCUMENT_OBJECT_TYPE_NAME,
                                FOLDER_OBJECT_TYPE_NAME };
        String[] guiNames = null;
        String associatedModuleName = "";
        String associatedObjectTypeName = "";
        int projectScope = ALL_PROJECT_SCOPE;
        DisplayLabel[] displayFields = new DisplayLabel[] {
            Util.createDisplayLabel(OBJECT_TYPE_FIELD),
            Util.createDisplayLabel(MODIFIED_FIELD)
        };
        Filter[] filters = null;
        if (!ROOT_PATH.equals(path)) {
            filters = new Filter[] {
                Util.createFieldFilter(PATH_FIELD, STRING_DATA_TYPE,
                        EQUALS_OPERATOR, path),
                Util.createFieldFilter(RECORD_STATE_FIELD, STRING_DATA_TYPE,
                        NOT_EQUALS_OPERATOR, DELETED_STATE),
                Util.createFieldFilter(RECORD_STATE_FIELD, STRING_DATA_TYPE,
                        NOT_EQUALS_OPERATOR, "")
            };
        }
        DisplayLabel[] associatedDisplayFields = null;
        FieldSortOrder[] fieldSortOrders = null;
        AssociationFilter[] associationFilters = null;
        int start = 1;
        int maximumResultCount = 1;
        QueryResult queryResult = client.runDynamicQuery(projectName,
                moduleName, objectTypeNames, guiNames, associatedModuleName,
                        associatedObjectTypeName, projectScope, displayFields,
                                associatedDisplayFields, fieldSortOrders,
                                        filters, associationFilters, start,
                                                maximumResultCount);
        if (queryResult == null) return false;
        QueryResponseHelper[] responses = queryResult.getQueryResponseHelpers();
        if (responses == null || responses.length != 1) return false;
        QueryResponseHelper response = responses[0];
        long recordId = -1l;
        try {
            recordId = Long.parseLong(response.getRecordId());
        } catch (Exception ignore) { }
        if (recordId == -1l) {
            this.id = -1l;
            this.modified = 0l;
            this.folder = false;
            return false;
        }
        Map<String, String> fields = getFields(response);
        String objectType = fields.get(OBJECT_TYPE_FIELD);
        if (objectType == null) objectType = DOCUMENT_OBJECT_TYPE_NAME;
        long modifiedTime = 0l;
        String modifiedValue = fields.get(MODIFIED_FIELD);
        if (modifiedValue != null) {
            DateFormat format = new SimpleDateFormat(MODIFIED_FORMAT);
            try {
                modifiedTime = format.parse(modifiedValue).getTime();
            } catch (Exception ignore) { }
            if (modifiedTime == 0l) {
                try {
                    modifiedTime = Long.parseLong(modifiedValue);
                } catch (Exception ignore) { }
            }
        }
        this.id = recordId;
        this.modified = modifiedTime;
        this.folder = !objectType.equals(DOCUMENT_OBJECT_TYPE_NAME);
        return true;
    }

    public void createNewFile() throws IOException {
        TririgaClient client = getClient();
        synchronized (DefaultTririgaFile.class) {
            if (documentModuleId == -1) {
                documentModuleId = client.getModuleId(DOCUMENT_MODULE_NAME);
            }
            if (documentObjectTypeId == -1l) {
                documentObjectTypeId = client.getObjectTypeId(
                        DOCUMENT_MODULE_NAME, DOCUMENT_OBJECT_TYPE_NAME);
            }
        }
        assert (documentModuleId != -1);
        assert (documentObjectTypeId != -1l);
        IntegrationField typeNameField = new IntegrationField();
        typeNameField.setName(OBJECT_TYPE_FIELD_NAME);
        typeNameField.setValue(DOCUMENT_OBJECT_TYPE_NAME);
        IntegrationField nameField = new IntegrationField();
        nameField.setName(NAME_FIELD_NAME);
        nameField.setValue(getName());
        IntegrationField filenameField = new IntegrationField();
        filenameField.setName(FILENAME_FIELD_NAME);
        filenameField.setValue(getName());
        IntegrationField pathField = new IntegrationField();
        pathField.setName(PATH_FIELD_NAME);
        pathField.setValue(toPath(toString()));
        IntegrationField statusField = new IntegrationField();
        statusField.setName(STATUS_FIELD_NAME);
        statusField.setValue(CHECKED_IN_STATUS);
        IntegrationField[] generalInfoFields = new IntegrationField[] {
            nameField, filenameField, pathField, statusField
        };
        IntegrationField[] recordInformationFields = new IntegrationField[] {
            typeNameField
        };
        IntegrationSection generalInfo = new IntegrationSection();
        generalInfo.setName(GENERAL_INFO_SECTION_NAME);
        generalInfo.setFields(generalInfoFields);
        IntegrationSection recordInformation = new IntegrationSection();
        recordInformation.setName(RECORD_INFORMATION_SECTION_NAME);
        recordInformation.setFields(recordInformationFields);
        IntegrationSection[] sections = new IntegrationSection[] {
            recordInformation, generalInfo
        };
        IntegrationRecord integrationRecord = new IntegrationRecord();
        integrationRecord.setId(-1l);
        integrationRecord.setSections(sections);
        integrationRecord.setActionName(CREATE_ACTION);
        integrationRecord.setModuleId(documentModuleId);
        integrationRecord.setObjectTypeId(documentObjectTypeId);
        integrationRecord.setObjectTypeName(DOCUMENT_OBJECT_TYPE_NAME);
        integrationRecord.setObjectPath(toPath(getParent()));
        ResponseHelperHeader result = client.saveRecord(
                new IntegrationRecord[] { integrationRecord });
        if (result.getAnyFailed()) {
            throw new IOException("Unable to create file.");
        }
        ResponseHelper response = result.getResponseHelpers()[0];
        long recordId = response.getRecordId();
        if (recordId == -1l) {
            throw new IOException("Unable to create file - invalid record ID.");
        }
        this.folder = false;
        this.id = recordId;
    }

    public void mkdir() throws IOException {
        TririgaClient client = getClient();
        synchronized (DefaultTririgaFile.class) {
            if (documentModuleId == -1) {
                documentModuleId = client.getModuleId(DOCUMENT_MODULE_NAME);
            }
            if (folderObjectTypeId == -1l) {
                folderObjectTypeId = client.getObjectTypeId(
                        DOCUMENT_MODULE_NAME, FOLDER_OBJECT_TYPE_NAME);
            }
        }
        assert (documentModuleId != -1);
        assert (folderObjectTypeId != -1l);
        IntegrationField typeNameField = new IntegrationField();
        typeNameField.setName(OBJECT_TYPE_FIELD_NAME);
        typeNameField.setValue(FOLDER_OBJECT_TYPE_NAME);
        IntegrationField nameField = new IntegrationField();
        nameField.setName(NAME_FIELD_NAME);
        nameField.setValue(getName());
        IntegrationField pathField = new IntegrationField();
        pathField.setName(PATH_FIELD_NAME);
        pathField.setValue(toPath(toString()));
        IntegrationField[] generalInfoFields = new IntegrationField[] {
            nameField, pathField
        };
        IntegrationField[] recordInformationFields = new IntegrationField[] {
            typeNameField
        };
        IntegrationSection generalInfo = new IntegrationSection();
        generalInfo.setName(GENERAL_INFO_SECTION_NAME);
        generalInfo.setFields(generalInfoFields);
        IntegrationSection recordInformation = new IntegrationSection();
        recordInformation.setName(RECORD_INFORMATION_SECTION_NAME);
        recordInformation.setFields(recordInformationFields);
        IntegrationSection[] sections = new IntegrationSection[] {
            recordInformation, generalInfo
        };
        IntegrationRecord integrationRecord = new IntegrationRecord();
        integrationRecord.setId(-1l);
        integrationRecord.setSections(sections);
        integrationRecord.setActionName(CREATE_ACTION);
        integrationRecord.setModuleId(documentModuleId);
        integrationRecord.setObjectTypeId(folderObjectTypeId);
        integrationRecord.setObjectTypeName(FOLDER_OBJECT_TYPE_NAME);
        integrationRecord.setObjectPath(toPath(getParent()));
        ResponseHelperHeader result = client.saveRecord(
                new IntegrationRecord[] { integrationRecord });
        if (result.getAnyFailed()) {
            throw new IOException("Unable to create directory.");
        }
        ResponseHelper response = result.getResponseHelpers()[0];
        long recordId = response.getRecordId();
        if (recordId == -1l) {
            throw new IOException(
                    "Unable to create directory - invalid record ID.");
        }
        this.folder = true;
        this.id = recordId;
    }

    private void rename(String newName) throws IOException {
        if (!exists()) throw new FileNotFoundException(toString());
        if (newName == null || "".equals(newName.trim())) {
            throw new IOException("Invalid empty target name.");
        }
        if (newName.equals(getName())) {
            throw new IOException("Cannot rename source to itself.");
        }
        TririgaClient client = getClient();
        synchronized (DefaultTririgaFile.class) {
            if (documentModuleId == -1) {
                documentModuleId = client.getModuleId(DOCUMENT_MODULE_NAME);
            }
            if (folderObjectTypeId == -1l) {
                folderObjectTypeId = client.getObjectTypeId(
                        DOCUMENT_MODULE_NAME, FOLDER_OBJECT_TYPE_NAME);
            }
            if (documentObjectTypeId == -1l) {
                documentObjectTypeId = client.getObjectTypeId(
                        DOCUMENT_MODULE_NAME, DOCUMENT_OBJECT_TYPE_NAME);
            }
        }
        assert (documentModuleId != -1);
        assert (folderObjectTypeId != -1l);
        assert (documentObjectTypeId != -1l);
        IntegrationField nameField = new IntegrationField();
        nameField.setName(NAME_FIELD_NAME);
        nameField.setValue(newName);
        IntegrationField[] generalInfoFields = new IntegrationField[] {
            nameField
        };
        IntegrationSection generalInfo = new IntegrationSection();
        generalInfo.setName(GENERAL_INFO_SECTION_NAME);
        generalInfo.setFields(generalInfoFields);
        IntegrationSection[] sections = new IntegrationSection[] {
            generalInfo
        };
        IntegrationRecord integrationRecord = new IntegrationRecord();
        integrationRecord.setId(this.id);
        integrationRecord.setSections(sections);
        integrationRecord.setActionName(APPLY_ACTION);
        integrationRecord.setModuleId(documentModuleId);
        integrationRecord.setObjectTypeId(this.folder ? folderObjectTypeId :
                documentObjectTypeId);
        integrationRecord.setObjectTypeName(this.folder ?
                FOLDER_OBJECT_TYPE_NAME : DOCUMENT_OBJECT_TYPE_NAME);
        integrationRecord.setObjectPath(toPath(getParent()));
        ResponseHelperHeader result = client.saveRecord(
                new IntegrationRecord[] { integrationRecord });
        if (result.getAnyFailed()) throw new IOException("Unable to rename.");
        ResponseHelper response = result.getResponseHelpers()[0];
        long recordId = response.getRecordId();
        if (recordId != this.id) {
            throw new IOException("Unable to rename - incorrect record ID.");
        }
        this.id = -1l;
        this.modified = 0l;
        this.folder = false;
    }

    public String toString() {
        return parentPath + "/" + name;
    }

    public boolean equals(Object o) {
        if (!(o instanceof TririgaFile)) return false;
        String path = toString();
        return (path != null) ? path.equals(o.toString()) :
                (o.toString() == null);
    }

    public int hashCode() {
        String path = toString();
        return (path != null) ? path.hashCode() : 0;
    }

    private TririgaClient getClient() throws IOException {
        return (client != null) ? client :
                (client = factory.newTririgaClient());
    }

    private ContinuationToken processChildren(QueryResult queryResult,
            List<TririgaFile> children) throws IOException {
        if (queryResult == null) return null;
        QueryResponseHelper[] responses = queryResult.getQueryResponseHelpers();
        if (responses != null) {
            TririgaClient client = getClient();
            for (QueryResponseHelper response : responses) {
                long recordId = -1l;
                try {
                    recordId = Long.parseLong(response.getRecordId());
                } catch (Exception ignore) { }
                if (recordId == -1l) continue;
                Map<String, String> fields = getFields(response);
                String name = fields.get(NAME_FIELD);
                if (name == null || "".equals(name = name.trim())) continue;
                String uri = toString() + "/" + name;
                String objectType = fields.get(OBJECT_TYPE_FIELD);
                if (objectType == null) objectType = DOCUMENT_OBJECT_TYPE_NAME;
                if (!(objectType.equals(DOCUMENT_OBJECT_TYPE_NAME) ||
                        objectType.equals(FOLDER_OBJECT_TYPE_NAME))) {
                    continue;
                }
                long modifiedTime = 0l;
                String modifiedValue = fields.get(MODIFIED_FIELD);
                if (modifiedValue != null) {
                    DateFormat format = new SimpleDateFormat(MODIFIED_FORMAT);
                    try {
                        modifiedTime = format.parse(modifiedValue).getTime();
                    } catch (Exception ignore) { }
                    if (modifiedTime == 0l) {
                        try {
                            modifiedTime = Long.parseLong(modifiedValue);
                        } catch (Exception ignore) { }
                    }
                }
                children.add(new DefaultTririgaFile(factory, client,
                        uri, recordId, modifiedTime,
                                objectType.equals(FOLDER_OBJECT_TYPE_NAME)));
            }
        }
        ContinuationToken token = queryResult.getContinuationToken();
        return (token == null || token.getTokenString() == null) ? null :
                token;
    }

    private static Map<String, String> getFields(QueryResponseHelper result) {
        Map<String, String> fields = new HashMap<String, String>();
        for (QueryResponseColumn column : result.getQueryResponseColumns()) {
            String name = column.getLabel();
            if (name == null || "".equals(name)) {
                name = column.getName();
                if (name == null || "".equals(name)) continue;
            }
            String value = column.getDisplayValue();
            if (value == null || "".equals(value)) value = column.getValue();
            fields.put(name, value);
        }
        return fields;
    }

    private static String getGlobalRoot() {
        String globalRoot = Configuration.getString(GLOBAL_ROOT_PROPERTY);
        if (globalRoot == null || "".equals(globalRoot = globalRoot.trim())) {
            return "";
        }
        StringBuilder rootPath =
                new StringBuilder(globalRoot.replace('/', '\\'));
        if (rootPath.charAt(0) != '\\') rootPath.insert(0, '\\');
        int end = rootPath.length() - 1;
        if (rootPath.charAt(end) == '\\') rootPath.setLength(end);
        return rootPath.toString();
    }

    private String toPath(String uri) {
        if (uri == null) uri = "";
        StringBuilder path = new StringBuilder(uri.replace('/', '\\'));
        if (path.length() > 0 && path.charAt(0) != '\\') path.insert(0, '\\');
        path.insert(0, "\\ROOT" + GLOBAL_ROOT);
        int end = path.length() - 1;
        if (path.charAt(end) == '\\') path.setLength(end);
        return path.toString();
    }

}
