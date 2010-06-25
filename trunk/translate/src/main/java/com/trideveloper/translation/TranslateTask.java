package com.trideveloper.translation;

import com.tririga.pub.workflow.CustomBusinessConnectTask;
import com.tririga.pub.workflow.Record;

import com.tririga.ws.TririgaWS;

import com.tririga.ws.dto.Association;
import com.tririga.ws.dto.IntegrationField;
import com.tririga.ws.dto.IntegrationRecord;
import com.tririga.ws.dto.IntegrationSection;
import com.tririga.ws.dto.ResponseHelperHeader;

import com.tririga.ws.dto.gui.Field;
import com.tririga.ws.dto.gui.GUI;
import com.tririga.ws.dto.gui.Section;
import com.tririga.ws.dto.gui.Tab;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

public class TranslateTask implements CustomBusinessConnectTask {

    private static final String FIELD_NAME_FIELD = "General|devFieldNameTX";

    private static final String SECTION_NAME_FIELD = "General|devSectionNameTX";

    private static final String SOURCE_LANGUAGE_FIELD =
            "General|devSourceLanguageTX";

    private static final String TARGET_LANGUAGE_FIELD =
            "General|devTargetLanguageTX";

    private static final String TARGET_RECORD_ASSOCIATION = "Alters";

    private static final String TRANSLATE_URL =
            "http://ajax.googleapis.com/ajax/services/language/translate";

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
            try {
                GUI gui = tririga.getGUI(record.getGuiId(), record.getId());
                Map<String, String> guiFields = getFields(gui);
                String fieldName = guiFields.get(FIELD_NAME_FIELD);
                if (fieldName == null) {
                    throw new IllegalStateException("No field name specified.");
                }
                fieldName = fieldName.trim();
                String targetLanguage = guiFields.get(TARGET_LANGUAGE_FIELD);
                if (targetLanguage == null) {
                    throw new IllegalStateException(
                            "No target language specified.");
                }
                targetLanguage = targetLanguage.trim();
                String sectionName = guiFields.get(SECTION_NAME_FIELD);
                if (sectionName != null &&
                        "".equals(sectionName = sectionName.trim())) {
                    sectionName = null;
                }
                String sourceLanguage = guiFields.get(SOURCE_LANGUAGE_FIELD);
                if (sourceLanguage != null &&
                        "".equals(sourceLanguage = sourceLanguage.trim())) {
                    sourceLanguage = null;
                }
                Map<Long, IntegrationRecord> updates =
                        new HashMap<Long, IntegrationRecord>();
                Association[] associations = tririga.getAssociatedRecords(
                        record.getId(), TARGET_RECORD_ASSOCIATION, -1);
                if (associations == null) continue;
                int count = associations.length;
                if (count <= 0) continue;
                try {
                    tririga.deassociateRecords(associations);
                } catch (Exception err) {
                    err.printStackTrace();
                    noErrors = false;
                }
                Set<Long> targetIds = new HashSet<Long>();
                for (Association association : associations) {
                    long targetId = association.getAssociatedRecordId();
                    if (targetId != -1l) targetIds.add(targetId);
                }
                long[] ids = new long[targetIds.size()];
                int index = 0;
                for (long id : targetIds) ids[index++] = id;
                com.tririga.ws.dto.Record[] targetHeaders =
                        tririga.getRecordDataHeaders(ids);
                for (com.tririga.ws.dto.Record target : targetHeaders) {
                    long id = target.getId();
                    gui = tririga.getGUI(target.getGuiId(), id);
                    IntegrationRecord update = createUpdateTemplate(gui,
                            sectionName, fieldName);
                    if (update != null) updates.put(id, update);
                }
                Iterator<Map.Entry<Long, IntegrationRecord>> entries =
                        updates.entrySet().iterator();
                while (entries.hasNext()) {
                    Map.Entry<Long, IntegrationRecord> entry = entries.next();
                    long id = entry.getKey();
                    IntegrationRecord update = entry.getValue();
                    if (update == null) {
                        entries.remove();
                        continue;
                    }
                    IntegrationSection[] sections = update.getSections();
                    if (sections == null || sections.length == 0 ||
                            sections[0] == null) {
                        entries.remove();
                        continue;
                    }
                    IntegrationField[] fields = sections[0].getFields();
                    if (fields == null || fields.length == 0 ||
                            fields[0] == null) {
                        entries.remove();
                        continue;
                    }
                    IntegrationField field = fields[0];
                    String value = field.getValue();
                    if (value == null || "".equals(value = value.trim())) {
                        entries.remove();
                        continue;
                    }
                    String newValue = null;
                    try {
                        newValue = translate(value, sourceLanguage,
                                targetLanguage);
                    } catch (Exception err) {
                        err.printStackTrace();
                        noErrors = false;
                    }
                    if (newValue == null ||
                            "".equals(newValue = newValue.trim()) ||
                                    newValue.equals(value)) {
                        entries.remove();
                        continue;
                    }
                    field.setValue(newValue);
                }
                List<IntegrationRecord> batch =
                        new ArrayList<IntegrationRecord>();
                for (IntegrationRecord update : updates.values()) {
                    batch.add(update);
                    if (batch.size() >= 200) {
                        try {
                            IntegrationRecord[] items = batch.toArray(
                                    new IntegrationRecord[batch.size()]);
                            ResponseHelperHeader result =
                                    tririga.saveRecord(items);
                            if (result.isAnyFailed()) {
                                noErrors = false;
                                System.err.println("Update batch had errors; " +
                                        result.getFailed() + " out of " +
                                                result.getTotal() + " failed.");
                            }
                        } catch (Exception err) {
                            err.printStackTrace();
                            noErrors = false;
                        } finally {
                            batch.clear();
                        }
                    }
                }
                if (!batch.isEmpty()) {
                    try {
                        IntegrationRecord[] items = batch.toArray(
                                new IntegrationRecord[batch.size()]);
                        ResponseHelperHeader result =
                                tririga.saveRecord(items);
                        if (result.isAnyFailed()) {
                            noErrors = false;
                            System.err.println("Update batch had errors; " +
                                    result.getFailed() + " out of " +
                                            result.getTotal() + " failed.");
                        }
                    } catch (Exception err) {
                        err.printStackTrace();
                        noErrors = false;
                    } finally {
                        batch.clear();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                noErrors = false;
            }
        }
        return noErrors;
    }

    private static String translate(String value, String source, String target)
            throws Exception {
        StringBuilder content = new StringBuilder();
        content.append("v=1.0");
        content.append("&langpair=");
        String langpair = (source != null) ? source + "|" + target :
                "|" + target;
        content.append(URLEncoder.encode(langpair, "UTF-8"));
        content.append("&q=");
        content.append(URLEncoder.encode(value));
        URL url = new URL(TRANSLATE_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        connection.setRequestProperty("Referer",
                "http://localhost:8001/translateTask");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        OutputStream output = connection.getOutputStream();
        output.write(content.toString().getBytes("UTF-8"));
        output.flush();
        output.close();
        InputStream input = connection.getInputStream();
        Reader reader = new InputStreamReader(input, "UTF-8");
        StringWriter writer = new StringWriter();
        char[] buf = new char[8192];
        int count;
        while ((count = reader.read(buf)) != -1) {
            writer.write(buf, 0, count);
        }
        reader.close();
        writer.close();
        JSONObject object = new JSONObject(writer.toString());
        int status = object.getInt("responseStatus");
        if (status != 200) {
            throw new IllegalArgumentException("Translation error.");
        }
        object = object.getJSONObject("responseData");
        return object.getString("translatedText");
    }

    private static IntegrationRecord createUpdateTemplate(GUI gui,
            String sectionName, String fieldName) throws Exception {
        Map<String, String> fields = getFields(gui);
        String value = null;
        if (sectionName != null) {
            value = fields.get(sectionName + "|" + fieldName);
        } else {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                String field = entry.getKey();
                if (field.endsWith("|" + fieldName)) {
                    sectionName = field.substring(0, field.lastIndexOf('|'));
                    value = entry.getValue();
                    break;
                }
            }
        }
        if (value == null || "".equals(value = value.trim())) return null;
        IntegrationField field = new IntegrationField();
        field.setName(fieldName);
        field.setValue(value);
        IntegrationField[] sectionFields = new IntegrationField[] { field };
        IntegrationSection section = new IntegrationSection();
        section.setName(sectionName);
        section.setFields(sectionFields);
        IntegrationSection[] sections = new IntegrationSection[] { section };
        IntegrationRecord integrationRecord = new IntegrationRecord();
        integrationRecord.setId(gui.getRecordId());
        integrationRecord.setSections(sections);
        integrationRecord.setActionName("");
        integrationRecord.setModuleId(gui.getModuleId());
        integrationRecord.setObjectTypeId(gui.getObjectTypeId());
        integrationRecord.setObjectTypeName(gui.getObjectTypeName());
        integrationRecord.setObjectPath(null);
        return integrationRecord;
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
                String sectionName = section.getName();
                Field[] fields = section.getFields();
                if (fields == null) continue;
                for (Field field : fields) {
                    String name = field.getName();
                    if (name == null) continue;
                    String value = field.getValue();
                    if (value != null && !"".equals(value)) {
                        fieldData.put(sectionName + "|" + name, value);
                    }
                }
            }
        }
        return fieldData;
    }

}
