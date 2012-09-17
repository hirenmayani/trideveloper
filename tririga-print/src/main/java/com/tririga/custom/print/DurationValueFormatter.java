package com.tririga.custom.print;

public class DurationValueFormatter implements ValueFormatter {

    public String toString(String value) {
        try {
            if (value == null || "".equals(value = value.trim())) {
                return null;
            }
            long duration = Long.parseLong(value);
            long monthCount = duration / 100000000000000l;
            duration %= 100000000000000l;
            long years = monthCount / 12l;
            long months = monthCount % 12l;
            long weeks = duration / 604800000l;
            duration %= 604800000l;
            long days = duration / 86400000l;
            duration %= 86400000l;
            long hours = duration / 3600000l;
            duration %= 3600000l;
            long minutes = duration / 60000l;
            duration %= 60000l;
            long seconds = duration / 1000l;
            long milliseconds = duration % 1000l;
            StringBuilder str = new StringBuilder();
            if (years > 0l) {
                str.append(years);
                str.append((years > 1l) ? " Years " : " Year ");
            }
            if (months > 0l) {
                str.append(months);
                str.append((months > 1l) ? " Months " : " Month ");
            }
            if (weeks > 0l) {
                str.append(weeks);
                str.append((weeks > 1l) ? " Weeks " : " Week ");
            }
            if (days > 0l) {
                str.append(days);
                str.append((days > 1l) ? " Days " : " Day ");
            }
            if (hours > 0l) {
                str.append(hours);
                str.append((hours > 1l) ? " Hours " : " Hour ");
            }
            if (minutes > 0l) {
                str.append(minutes);
                str.append((minutes > 1l) ? " Minutes " : " Minute ");
            }
            if (seconds > 0l) {
                str.append(seconds);
                str.append((seconds > 1l) ? " Seconds " : " Second ");
            }
            if (milliseconds > 0l) {
                str.append(milliseconds);
                str.append((milliseconds > 1l) ? " Milliseconds " :
                        " Millisecond ");
            }
            return str.toString().trim();
        } catch (Exception ex) {
            return null;
        }
    }

}
