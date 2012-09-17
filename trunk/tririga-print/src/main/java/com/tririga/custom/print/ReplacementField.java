package com.tririga.custom.print;

public class ReplacementField {

    private final String sectionName;

    private final String fieldName;

    public ReplacementField(String sectionName, String fieldName) {
        this.sectionName = sectionName;
        this.fieldName = fieldName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public int hashCode() {
        String s = getSectionName();
        int hashCode = (s != null) ? s.hashCode() : 0;
        s = getFieldName();
        return (s != null) ? (hashCode ^ s.hashCode()) : hashCode;
    }

    public boolean equals(Object o) {
        if (!(o instanceof ReplacementField)) {
            return false;
        }
        ReplacementField other = (ReplacementField) o;
        String s = getSectionName();
        if ((s != null) ? !s.equals(other.getSectionName()) :
                (other.getSectionName() != null)) {
            return false;
        }
        s = getFieldName();
        return (s != null) ? s.equals(other.getFieldName()) :
                (other.getFieldName() == null);
    }

}
