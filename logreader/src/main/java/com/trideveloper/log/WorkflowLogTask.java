package com.trideveloper.log;

import com.tririga.pub.workflow.CustomBusinessConnectTask;
import com.tririga.pub.workflow.Record;

import com.tririga.ws.TririgaWS;

import com.tririga.ws.dto.Association;
import com.tririga.ws.dto.IntegrationField;
import com.tririga.ws.dto.IntegrationRecord;
import com.tririga.ws.dto.IntegrationRows;
import com.tririga.ws.dto.IntegrationSection;
import com.tririga.ws.dto.ResponseHelper;
import com.tririga.ws.dto.ResponseHelperHeader;

import com.tririga.ws.dto.gui.Field;
import com.tririga.ws.dto.gui.GUI;
import com.tririga.ws.dto.gui.Section;
import com.tririga.ws.dto.gui.Tab;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileReader;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WorkflowLogTask implements CustomBusinessConnectTask {

    public static final String WORKFLOW_DATA_SECTION = "General";

    public static final String WORKFLOW_CREATE_ACTION = "triCreate";

    public static final String WORKFLOW_MODULE = "devUtil";

    public static final String WORKFLOW_OBJECT_TYPE = "devWorkflowMetrics";

    public static final String WORKFLOW_ASSOCIATION = "Has Item";

    public static final String WORKFLOW_REVERSE_ASSOCIATION = "Is Item For";

    public static final String FROM_FIELD = "triStartDT";

    public static final String TO_FIELD = "triEndDT";

    public static final String MODULE_FIELD = "triModuleNameTX";

    public static final String BO_FIELD = "triBONameTX";

    public static final String NAME_FIELD = "triNameTX";

    public static final String EVENT_FIELD = "devEventTX";

    public static final String FIRST_TIME_FIELD = "devFirstTimeDT";

    public static final String LAST_TIME_FIELD = "devLastTimeDT";

    public static final String MIN_DURATION_FIELD = "devMinimumDU";

    public static final String MAX_DURATION_FIELD = "devMaximumDU";

    public static final String TOTAL_EXECUTION_TIME_FIELD =
            "devTotalExecutionDU";

    public static final String EXECUTION_COUNT_FIELD = "devExecutionCountNU";

    private static final FilenameFilter LOG_FILENAME_FILTER =
            new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    name = name.toLowerCase();
                    return name.startsWith("awft.log") ||
                            name.startsWith("swft.log");
                }
            };

    private static final EntityResolver NULL_RESOLVER = new EntityResolver() {
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new ByteArrayInputStream(new byte[0]));
        }
    };

    private static final String INFO_PATTERN =
            "^.*?Name='(.*?)' Module='(.*?)' BO='(.*?)' Event='(.*?)'";

    private static final String LOG_CONFIG_FILE = "log4j.xml";

    private static final String AWFT_APPENDER_NAME = "AWFT";

    private static final String SWFT_APPENDER_NAME = "SWFT";

    private static final int DURATION_INDEX = 3;

    private static final int END_TIME_INDEX = 9;

    private static final int INFO_INDEX = 20;

    private static final int BATCH_SIZE = 50;

    static int workflowModuleId = -1;

    static long workflowObjectTypeId = -1l;

    public boolean execute(TririgaWS tririga, long userId, Record[] records) {
        com.tririga.ws.dto.Record[] recordHeaders = null;
        try {
            tririga.register(userId);
            long recordId = records[0].getId();
            com.tririga.ws.dto.Record record =
                    tririga.getRecordDataHeaders(new long[] { recordId })[0];
            GUI gui = tririga.getGUI(record.getGuiId(), recordId);
            Map<String, String> fields = getFields(gui);
            long from = Long.MIN_VALUE;
            long to = Long.MAX_VALUE;
            try {
                from = Long.parseLong(fields.get(FROM_FIELD));
                if (from <= 0l) from = Long.MIN_VALUE;
            } catch (Exception ignore) { }
            try {
                to = Long.parseLong(fields.get(TO_FIELD));
                if (to <= 0l) to = Long.MAX_VALUE;
            } catch (Exception ignore) { }
            Map<String, Workflow> workflows = new HashMap<String, Workflow>();
            for (File logDirectory : getLogDirectories()) {
                for (File log : logDirectory.listFiles(LOG_FILENAME_FILTER)) {
                    BufferedReader reader =
                            new BufferedReader(new FileReader(log));
                    Pattern infoPattern = Pattern.compile(INFO_PATTERN);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if ("".equals(line = line.trim())) continue;
                        try {
                            String[] logFields = line.split("\t");
                            long time = Long.parseLong(
                                    logFields[END_TIME_INDEX].trim());
                            if (time < from || time > to) continue;
                            long duration = Long.parseLong(
                                    logFields[DURATION_INDEX].trim());
                            String info = logFields[INFO_INDEX].trim();
                            Matcher matcher = infoPattern.matcher(info);
                            if (!matcher.find()) continue;
                            String workflowName = matcher.group(1);
                            String module = matcher.group(2);
                            String bo = matcher.group(3);
                            String event = matcher.group(4);
                            Workflow workflow = workflows.get(workflowName);
                            if (workflow == null) {
                                workflow = new Workflow(module, bo,
                                        workflowName, event);
                                workflows.put(workflowName, workflow);
                            }
                            workflow.addExecution(time, duration);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            synchronized (WorkflowLogTask.class) {
                if (workflowModuleId == -1) {
                    workflowModuleId = tririga.getModuleId(WORKFLOW_MODULE);
                }
                if (workflowObjectTypeId == -1l) {
                    workflowObjectTypeId = tririga.getObjectTypeId(
                            WORKFLOW_MODULE, WORKFLOW_OBJECT_TYPE);
                }
            }
            List<IntegrationRecord> workflowSet =
                    new ArrayList<IntegrationRecord>();
            for (Workflow workflow : workflows.values()) {
                IntegrationField[] dataFields = new IntegrationField[] {
                    new IntegrationField(MODULE_FIELD, workflow.getModule()),
                    new IntegrationField(BO_FIELD, workflow.getBo()),
                    new IntegrationField(NAME_FIELD, workflow.getName()),
                    new IntegrationField(EVENT_FIELD, workflow.getEvent()),
                    new IntegrationField(FIRST_TIME_FIELD,
                            String.valueOf(workflow.getFirstTime())),
                    new IntegrationField(LAST_TIME_FIELD,
                            String.valueOf(workflow.getLastTime())),
                    new IntegrationField(MIN_DURATION_FIELD,
                            String.valueOf(workflow.getMinDuration())),
                    new IntegrationField(MAX_DURATION_FIELD,
                            String.valueOf(workflow.getMaxDuration())),
                    new IntegrationField(TOTAL_EXECUTION_TIME_FIELD,
                            String.valueOf(workflow.getTotalExecutionTime())),
                    new IntegrationField(EXECUTION_COUNT_FIELD,
                            String.valueOf(workflow.getExecutionCount()))
                };
                IntegrationSection section = new IntegrationSection();
                section.setName(WORKFLOW_DATA_SECTION);
                section.setFields(dataFields);
                IntegrationSection[] sections = new IntegrationSection[] {
                    section
                };
                IntegrationRecord integrationRecord = new IntegrationRecord();
                integrationRecord.setActionName(WORKFLOW_CREATE_ACTION);
                integrationRecord.setModuleId(workflowModuleId);
                integrationRecord.setObjectTypeId(workflowObjectTypeId);
                integrationRecord.setObjectTypeName(WORKFLOW_OBJECT_TYPE);
                integrationRecord.setSections(sections);
                workflowSet.add(integrationRecord);
            }
            boolean success = true;
            if (workflowSet.isEmpty()) return true;
            int count = workflowSet.size();
            for (int i = 0; i < count; i += BATCH_SIZE) {
                List<IntegrationRecord> batch = workflowSet.subList(i,
                        Math.min(i + BATCH_SIZE, count));
                success = success && saveBatch(tririga, recordId, batch);
            }
            return success;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean saveBatch(TririgaWS tririga, long recordId,
            List<IntegrationRecord> batch) throws Exception {
        ResponseHelperHeader result = tririga.saveRecord(
                batch.toArray(new IntegrationRecord[batch.size()]));
        boolean success = !result.isAnyFailed();
        List<Association> associations = new ArrayList<Association>();
        for (ResponseHelper response : result.getResponseHelpers()) {
            if (response.getStatus().toUpperCase().indexOf("SUCCESS") == -1) {
                continue;
            }
            long assocId = response.getRecordId();
            if (assocId <= 0l) continue;
            Association association = new Association();
            association.setRecordId(recordId);
            association.setAssociatedRecordId(assocId);
            association.setAssociationName(WORKFLOW_ASSOCIATION);
            association.setReverseAssociationName(WORKFLOW_REVERSE_ASSOCIATION);
            associations.add(association);
        }
        if (associations.isEmpty()) return success;
        result = tririga.associateRecords(
                associations.toArray(new Association[associations.size()]));
        return success && !result.isAnyFailed();
    }

    private static List<File> getLogDirectories() throws Exception {
        List<File> directories = new ArrayList<File>();
        InputStream logConfig =
                ClassLoader.getSystemResourceAsStream(LOG_CONFIG_FILE);
        if (logConfig == null) {
            throw new IllegalStateException(
                    "Unable to find log configuration " + LOG_CONFIG_FILE);
        }
        DocumentBuilder builder = DocumentBuilderFactory.newInstance(
                ).newDocumentBuilder();
        builder.setEntityResolver(NULL_RESOLVER);
        Document logConfiguration = builder.parse(logConfig);
        NodeList appenders = logConfiguration.getElementsByTagName("appender");
        for (int i = appenders.getLength() - 1; i >= 0; i--) {
            Element appender = (Element) appenders.item(i);
            String name = appender.getAttribute("name");
            if (!AWFT_APPENDER_NAME.equals(name) &&
                    !SWFT_APPENDER_NAME.equals(name)) {
                continue;
            }
            NodeList params = appender.getElementsByTagName("param");
            for (int j = params.getLength() - 1; j >= 0; j--) {
                Element param = (Element) params.item(j);
                if ("file".equalsIgnoreCase(param.getAttribute("name"))) {
                    File file = new File(param.getAttribute("value"));
                    File directory = file.getCanonicalFile().getParentFile();
                    if (directory != null) directories.add(directory);
                    break;
                }
            }
        }
        return directories;
    }

    private static Map<String, String> getFields(GUI gui) throws Exception {
        Map<String, String> fieldData = new HashMap<String, String>();
        if (gui == null) throw new NullPointerException("Object GUI is null.");
        Tab[] tabs = gui.getTabs();
        if (tabs == null) return fieldData;
        for (Tab tab : tabs) {
            if (tab == null) continue;
            Section[] sections = tab.getSections();
            if (sections == null) continue;
            for (Section section : sections) {
                if (section == null) continue;
                Field[] fields = section.getFields();
                if (fields == null) continue;
                for (Field field : fields) {
                    String name = field.getName();
                    if (name == null) continue;
                    String value = field.getValue();
                    if (value != null && !"".equals(value)) {
                        fieldData.put(name, value);
                    }
                }
            }
        }
        return fieldData;
    }

}
