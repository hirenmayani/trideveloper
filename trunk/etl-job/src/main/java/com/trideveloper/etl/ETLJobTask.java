package com.trideveloper.etl;

import com.tririga.pub.workflow.CustomBusinessConnectTask;
import com.tririga.pub.workflow.Record;

import com.tririga.ws.TririgaWS;

import com.tririga.ws.dto.Association;

import com.tririga.ws.dto.gui.Field;
import com.tririga.ws.dto.gui.GUI;
import com.tririga.ws.dto.gui.Section;
import com.tririga.ws.dto.gui.Tab;

import java.io.File;
import java.io.FileNotFoundException;

import java.sql.Connection;
import java.sql.PreparedStatement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;

import javax.sql.DataSource;

import org.pentaho.di.core.Result;

import org.pentaho.di.core.logging.LogWriter;

import org.pentaho.di.core.util.EnvUtil;

import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobEntryLoader;
import org.pentaho.di.job.JobMeta;

import org.pentaho.di.trans.StepLoader;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;

public class ETLJobTask implements CustomBusinessConnectTask {

    public static final String JOB_NUMBER_VARIABLE = "dcJobNumber";

    public static final String VARIABLE_ASSOCIATION = "Has Item";

    public static final String JOB_FILE_FIELD = "devJobFileTX";

    public static final String LOG_FILE_FIELD = "devLogFileTX";

    public static final String LOG_LEVEL_FIELD = "devLogLevelLI";

    public static final String IS_DATA_CONNECT_FIELD = "devDataConnectBL";

    public static final String JOB_NAME_FIELD = "triNameTX";

    public static final String BO_NAME_FIELD = "devJobBONameTX";

    public static final String VARIABLE_NAME_FIELD = "devVariableNameTX";

    public static final String VARIABLE_VALUE_FIELD = "devVariableValueTX";

    private static final String BASIC_LOG_LEVEL = "Basic";

    private static final String TRIRIGA_DATA_SOURCE =
            "jdbc/local/DataSource-TRIRIGA-data";

    private static final String INSERT_STATEMENT =
            "INSERT INTO DC_JOB (JOB_NUMBER, JOB_TYPE, JOB_NAME, " +
                    "JOB_RUN_CTL, BO_NAME, STATE) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_STATEMENT =
            "UPDATE DC_JOB SET STATE = ? WHERE JOB_NUMBER = ?";

    private static final Set<String> RUNNING_JOBS = new HashSet<String>();

    private static DataSource dataSource;

    public boolean execute(TririgaWS tririga, long userId, Record[] records) {
        com.tririga.ws.dto.Record[] recordHeaders = null;
        try {
            tririga.register(userId);
            int count = records.length;
            long[] recordIds = new long[count];
            for (int i = 0; i < count; i++) {
                recordIds[i] = records[i].getId();
            }
            recordHeaders = tririga.getRecordDataHeaders(recordIds);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        boolean noErrors = true;
        for (com.tririga.ws.dto.Record record : recordHeaders) {
            Result result = null;
            Job job = null;
            Trans trans = null;
            boolean isTrans = false;
            LogWriter log = null;
            boolean isDataConnect = false;
            Connection connection = null;
            PreparedStatement statement = null;
            Integer jobNumber = null;
            boolean createdEntry = false;
            try {
                GUI gui = tririga.getGUI(record.getGuiId(), record.getId());
                Map<String, String> fields = getFields(gui);
                String jobFile = fields.get(JOB_FILE_FIELD);
                if (jobFile == null) {
                    throw new IllegalStateException("No filename specified.");
                }
                File file = new File(jobFile);
                if (!file.isFile()) {
                    throw new FileNotFoundException("File \"" + file +
                            "\" does not exist.");
                }
                jobFile = file.getCanonicalPath();
                isTrans = jobFile.toLowerCase().endsWith(".ktr");
                isDataConnect = Boolean.valueOf(
                        fields.get(IS_DATA_CONNECT_FIELD));
                if (isDataConnect) {
                    String jobName = fields.get(JOB_NAME_FIELD);
                    String boName = fields.get(BO_NAME_FIELD);
                    if (jobName == null || boName == null) {
                        throw new IllegalStateException(
                                "DataConnect job requires job name and " +
                                        "Job Business Object name.");
                    }
                    jobNumber = (int) (Math.random() * 99999999.0d);
                    connection = getDataSource().getConnection();
                    statement = connection.prepareStatement(INSERT_STATEMENT);
                    statement.setInt(1, jobNumber);
                    statement.setInt(2, 1); // inbound job type
                    statement.setString(3, jobName);
                    statement.setInt(4, 1); // single-threaded job
                    statement.setString(5, boName);
                    statement.setInt(6, 0); // job state is New
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalStateException(
                                "Unable to insert job entry in DC_JOB.");
                    }
                    statement.close();
                    statement = null;
                    createdEntry = true;
                }
                String logFile = fields.get(LOG_FILE_FIELD);
                String logLevel = fields.get(LOG_LEVEL_FIELD);
                if (logLevel == null) logLevel = BASIC_LOG_LEVEL;
                Association[] associations = tririga.getAssociatedRecords(
                        record.getId(), VARIABLE_ASSOCIATION, -1);
                int count = associations.length;
                Map<String, String> variables = new HashMap<String, String>();
                if (associations != null && count > 0) {
                    Set<Long> variableIds = new HashSet<Long>();
                    for (Association association : associations) {
                        long variableId = association.getAssociatedRecordId();
                        if (variableId != -1l) variableIds.add(variableId);
                    }
                    long[] ids = new long[variableIds.size()];
                    int index = 0;
                    for (long id : variableIds) ids[index++] = id;
                    com.tririga.ws.dto.Record[] variableHeaders =
                            tririga.getRecordDataHeaders(ids);
                    for (com.tririga.ws.dto.Record variable : variableHeaders) {
                        gui = tririga.getGUI(variable.getGuiId(),
                                variable.getId());
                        fields = getFields(gui);
                        String variableName = fields.get(VARIABLE_NAME_FIELD);
                        if (variableName == null) continue;
                        String variableValue = fields.get(VARIABLE_VALUE_FIELD);
                        variables.put(variableName, variableValue);
                    }
                }
                if (isDataConnect) {
                    variables.put(JOB_NUMBER_VARIABLE,
                            String.valueOf(jobNumber));
                }
                synchronized (RUNNING_JOBS) {
                    if (RUNNING_JOBS.contains(jobFile)) {
                        throw new IllegalStateException("Job " + jobFile +
                                " is already executing.");
                    }
                    RUNNING_JOBS.add(jobFile);
                }
                try {
                    log = (logFile == null) ?
                            LogWriter.getInstance(LogWriter.LOG_LEVEL_BASIC) :
                                    LogWriter.getInstance(logFile, true,
                                            LogWriter.LOG_LEVEL_BASIC);
                    log.setLogLevel(logLevel);
                    synchronized (RUNNING_JOBS) {
                        EnvUtil.environmentInit();
                        StepLoader.init();
                        StepLoader stepLoader = StepLoader.getInstance();
                        JobEntryLoader.init();
                        if (isTrans) {
                            TransMeta transMeta = new TransMeta(jobFile);
                            trans = new Trans(transMeta);
                            trans.getTransMeta().setArguments(null);
                            trans.initializeVariablesFrom(null);
                            trans.getTransMeta().setInternalKettleVariables(
                                    trans);
                        } else {
                            JobMeta jobMeta = new JobMeta(log, jobFile, null,
                                    null);
                            job = new Job(log, stepLoader, null, jobMeta);
                            job.getJobMeta().setArguments(null);
                            job.initializeVariablesFrom(null);
                            job.getJobMeta().setInternalKettleVariables(job);
                        }
                    }
                    for (Map.Entry<String, String> variable :
                            variables.entrySet()) {
                        String variableName = variable.getKey();
                        String variableValue = variable.getValue();
                        if (isTrans) {
                            trans.setVariable(variableName, variableValue);
                        } else {
                            job.setVariable(variableName, variableValue);
                        }
                        log.logDebug("ETL Job", "Set variable {0} to value {1}",
                                new String[] { variableName, variableValue });
                    }
                    if (isTrans) {
                        log.logDebug("ETL Job", "Executing transformation {0}.",
                                new String[] { jobFile });
                        trans.execute(null);
                        trans.waitUntilFinished();
                        trans.endProcessing("end");
                        result = trans.getResult();
                    } else {
                        log.logDebug("ETL Job", "Executing job {0}.",
                                new String[] { jobFile });
                        result = job.execute();
                        job.endProcessing("end", result);
                    }
                } finally {
                    synchronized (RUNNING_JOBS) {
                        RUNNING_JOBS.remove(jobFile);
                    }
                }
                if (result != null && result.getNrErrors() != 0l) {
                    job = null;
                    trans = null;
                    throw new Exception("ETL Job encountered " +
                            result.getNrErrors() + " errors.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if (job != null) {
                    try {
                        job.endProcessing("error", result);
                    } catch (Exception ignore) { }
                }
                if (trans != null) {
                    try {
                        trans.endProcessing("error");
                    } catch (Exception ignore) { }
                }
                noErrors = false;
            } finally {
                if (log != null) {
                    try {
                        log.close();
                    } catch (Exception ignore) { }
                }
                if (createdEntry && jobNumber != null) {
                    try {
                        statement =
                                connection.prepareStatement(UPDATE_STATEMENT);
                        if (noErrors) {
                            statement.setInt(1, 1); // set job state to Ready
                        } else {
                            statement.setInt(1, 5); // set job state to Obsolete
                        }
                        statement.setInt(2, jobNumber);
                        statement.executeUpdate();
                        statement.close();
                        statement = null;
                    } catch (Exception ignore) { }
                }
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (Exception ignore) { }
                    statement = null;
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (Exception ignore) { }
                    connection = null;
                }
            }
        }
        return noErrors;
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

    private static DataSource getDataSource() throws Exception {
        synchronized (ETLJobTask.class) {
            if (dataSource != null) return dataSource;
            Context context = new InitialContext();
            try {
                dataSource = (DataSource) context.lookup(TRIRIGA_DATA_SOURCE);
            } catch (Exception ignore) { }
            if (dataSource == null) {
                try {
                    dataSource = (DataSource)
                            context.lookup("/" + TRIRIGA_DATA_SOURCE);
                } catch (Exception ignore) { }
                if (dataSource == null) {
                    try {
                        dataSource = (DataSource)
                                context.lookup("java:/" + TRIRIGA_DATA_SOURCE);
                    } catch (Exception ignore) { }
                    if (dataSource == null) {
                        throw new IllegalStateException(
                                "Unable to obtain data source " +
                                        TRIRIGA_DATA_SOURCE);
                    }
                }
            }
            return dataSource;
        }
    }

}
