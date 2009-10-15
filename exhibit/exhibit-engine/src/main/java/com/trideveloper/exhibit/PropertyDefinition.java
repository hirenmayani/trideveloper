package com.trideveloper.exhibit;

class PropertyDefinition implements Comparable<PropertyDefinition> {

    private final String name;

    private final String label;

    private final String reverseLabel;

    private final String valueType;

    public PropertyDefinition(String name, String label, String reverseLabel,
            String valueType) {
        this.name = name;
        this.label = label;
        this.reverseLabel = reverseLabel;
        this.valueType = valueType;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getReverseLabel() {
        return reverseLabel;
    }

    public String getValueType() {
        return valueType;
    }

    public boolean equals(Object o) {
        if (!(o instanceof PropertyDefinition)) return false;
        String name = getName();
        return (name != null) ?
                name.equals(((PropertyDefinition) o).getName()) :
                        (((PropertyDefinition) o).getName() == null);
    }

    public int hashCode() {
        String name = getName();
        return (name != null) ? name.hashCode() : 0;
    }

    public int compareTo(PropertyDefinition other) {
        if (other == null) return 1;
        String name = getName();
        String otherName = other.getName();
        if (name != null) {
            return (otherName != null) ? name.compareTo(otherName) : 1;
        }
        return (otherName != null) ? -1 : 0;
    }

}
