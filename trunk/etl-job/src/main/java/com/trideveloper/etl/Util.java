package com.trideveloper.etl;

import com.tririga.ws.TririgaWS;

import com.tririga.ws.dto.AssociationFilter;
import com.tririga.ws.dto.DisplayLabel;
import com.tririga.ws.dto.FieldSortOrder;
import com.tririga.ws.dto.Filter;
import com.tririga.ws.dto.QueryResponseColumn;
import com.tririga.ws.dto.QueryResponseHelper;
import com.tririga.ws.dto.QueryResult;

import com.tririga.ws.dto.gui.Field;
import com.tririga.ws.dto.gui.GUI;
import com.tririga.ws.dto.gui.Section;
import com.tririga.ws.dto.gui.Tab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Util {

    private static final String ID_FIELD = "triRecordIdSY";

    private static final int STRING_DATA_TYPE = 320;

    private static final int EQUALS_OPERATOR = 10;

    private static final int ALL_PROJECT_SCOPE = 2;

    public static Map<String, String> getRecordData(TririgaWS tririga,
            long recordId) throws Exception {
        return getRecordData(tririga, tririga.getDefaultGUI(recordId));
    }

    public static Map<String, String> getRecordData(TririgaWS tririga, GUI gui)
            throws Exception {
        if (gui == null) throw new NullPointerException("Null GUI prohibited.");
        long id = gui.getRecordId();
        if (id < 0) throw new IllegalStateException("No Record ID found.");
        String projectName = "";
        String moduleName = gui.getModuleName();
        String objectTypeName = gui.getObjectTypeName();
        String[] objectTypeNames = new String[] { objectTypeName };
        String[] guiNames = null;
        String associatedModuleName = "";
        String associatedObjectTypeName = "";
        int projectScope = ALL_PROJECT_SCOPE;
        Map<String, String> fieldSections = getFieldSections(gui);
        List<DisplayLabel> fields = new ArrayList<DisplayLabel>();
        for (Map.Entry<String, String> entry : fieldSections.entrySet()) {
            String fieldName = entry.getKey();
            String sectionName = entry.getValue();
            DisplayLabel displayLabel = new DisplayLabel();
            displayLabel.setFieldName(fieldName);
            displayLabel.setLabel(fieldName);
            displayLabel.setSectionName(sectionName);
            fields.add(displayLabel);
        }
        DisplayLabel[] displayFields = fields.toArray(new DisplayLabel[0]);
        DisplayLabel[] associatedDisplayFields = null;
        Filter filter = new Filter();
        filter.setDataType(STRING_DATA_TYPE);
        filter.setFieldName(ID_FIELD);
        filter.setOperator(EQUALS_OPERATOR);
        // query engine will accommodate if General section is wrong on filter
        filter.setSectionName("General");
        filter.setValue(String.valueOf(id));
        Filter[] filters = new Filter[] { filter };
        AssociationFilter[] associationFilters = null;
        FieldSortOrder[] fieldSortOrders = null;
        int start = 1;
        int maximumResultCount = 2;
        QueryResult queryResult = tririga.runDynamicQuery(projectName,
                moduleName, objectTypeNames, guiNames, associatedModuleName,
                        associatedObjectTypeName, projectScope, displayFields,
                                associatedDisplayFields, fieldSortOrders,
                                        filters, associationFilters, start,
                                                maximumResultCount);
        if (queryResult == null) return null;
        QueryResponseHelper[] responses = queryResult.getQueryResponseHelpers();
        // if no responses, ID not found
        // if multiple responses, triRecordIdSY field does not exist on record
        if (responses == null || responses.length != 1) return null;
        QueryResponseHelper response = responses[0];
        if (response == null) return null;
        String responseId = response.getRecordId();
        if (responseId == null || !responseId.equals(String.valueOf(id))) {
            // record ID does not match - means that the triRecordIdSY field
            // does not exist on the record, and there just happens to be a
            // single result
            return null;
        }
        return getData(response);
    }

    private static Map<String, String> getData(QueryResponseHelper result) {
        Map<String, String> data = new HashMap<String, String>();
        for (QueryResponseColumn column : result.getQueryResponseColumns()) {
            String name = column.getLabel();
            if (name == null || "".equals(name)) {
                name = column.getName();
                if (name == null || "".equals(name)) continue;
            }
            String value = column.getDisplayValue();
            if (value == null || "".equals(value)) value = column.getValue();
            data.put(name, value);
        }
        return data;
    }

    private static Map<String, String> getFieldSections(GUI gui) {
        Map<String, String> fieldSections = new HashMap<String, String>();
        if (gui == null) throw new NullPointerException("Object GUI is null.");
        Tab[] tabs = gui.getTabs();
        if (tabs == null) return fieldSections;
        for (Tab tab : tabs) {
            if (tab == null) continue;
            Section[] sections = tab.getSections();
            if (sections == null) continue;
            for (Section section : sections) {
                if (section == null) continue;
                Field[] fields = section.getFields();
                if (fields == null) continue;
                for (Field field : fields) {
                    String fieldName = field.getName();
                    if (fieldName == null) continue;
                    String sectionName = field.getSectionName();
                    if (sectionName != null && !"".equals(sectionName)) {
                        fieldSections.put(fieldName, sectionName);
                    }
                }
            }
        }
        return fieldSections;
    }

}
