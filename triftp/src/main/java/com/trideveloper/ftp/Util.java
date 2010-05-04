package com.trideveloper.ftp;

import com.trideveloper.tririga.ws.dto.DisplayLabel;
import com.trideveloper.tririga.ws.dto.Filter;

class Util {

    public static DisplayLabel createDisplayLabel(String field) {
        int index = field.indexOf(".");
        String sectionName = field.substring(0, index);
        String fieldName = field.substring(index + 1);
        DisplayLabel displayLabel = new DisplayLabel();
        displayLabel.setFieldName(fieldName);
        displayLabel.setLabel(field);
        displayLabel.setSectionName(sectionName);
        return displayLabel;
    }

    public static Filter createFieldFilter(String field, int dataType,
            int filterOperator, String value) {
        int index = field.indexOf(".");
        String sectionName = field.substring(0, index);
        String fieldName = field.substring(index + 1);
        Filter filter = new Filter();
        filter.setDataType(dataType);
        filter.setFieldName(fieldName);
        filter.setOperator(filterOperator);
        filter.setSectionName(sectionName);
        filter.setValue(value);
        return filter;
    }

}
