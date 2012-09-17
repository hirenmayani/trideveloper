package com.tririga.custom.print;

import java.text.SimpleDateFormat;

import java.util.Date;

public class DateValueFormatter implements ValueFormatter {

    private static final String DATE_FORMAT = "M/d/yyyy";

    public String toString(String value) {
        try {
            if (value == null || "".equals(value = value.trim())) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            return sdf.format(new Date(Long.parseLong(value)));
        } catch (Exception ex) {
            return null;
        }
    }

}
