package com.tririga.custom.print;

import java.text.SimpleDateFormat;

import java.util.Date;

public class TimeValueFormatter implements ValueFormatter {

    private static final String TIME_FORMAT = "h:mm:ss a";

    public String toString(String value) {
        try {
            if (value == null || "".equals(value = value.trim())) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat(TIME_FORMAT);
            return sdf.format(new Date(Long.parseLong(value)));
        } catch (Exception ex) {
            return null;
        }
    }

}
