package com.trideveloper.exhibit;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class ExhibitDataSource {

    private final Set<PropertyDefinition> properties =
            new HashSet<PropertyDefinition>();

    private final String type;

    private final String plural;

    private final String queryModule;

    private final String queryBusinessObject;

    private final String queryName;

    public ExhibitDataSource(String type, String plural, String queryModule,
            String queryBusinessObject, String queryName) {
        this.type = type;
        this.plural = plural;
        this.queryModule = queryModule;
        this.queryBusinessObject = queryBusinessObject;
        this.queryName = queryName;
    }

    public String getType() {
        return type;
    }

    public String getPlural() {
        return plural;
    }

    public String getQueryModule() {
        return queryModule;
    }

    public String getQueryBusinessObject() {
        return queryBusinessObject;
    }

    public String getQueryName() {
        return queryName;
    }

    public void add(PropertyDefinition property) {
        properties.add(property);
    }

    public Set<PropertyDefinition> getProperties() {
        return Collections.unmodifiableSet(properties);
    }

    public boolean equals(Object o) {
        if (!(o instanceof ExhibitDataSource)) return false;
        ExhibitDataSource other = (ExhibitDataSource) o;
        String queryModule = getQueryModule();
        if ((queryModule != null) ?
                !queryModule.equals(other.getQueryModule()) :
                        (other.getQueryModule() != null)) {
            return false;
        }
        String queryObject = getQueryBusinessObject();
        if ((queryObject != null) ?
                !queryObject.equals(other.getQueryBusinessObject()) :
                        (other.getQueryBusinessObject() != null)) {
            return false;
        }
        String queryName = getQueryName();
        return (queryName != null) ? queryName.equals(other.getQueryName()) :
                (other.getQueryName() == null);
    }

    public int hashCode() {
        String val = getQueryModule();
        int hashCode = (val != null) ? val.hashCode() : 0;
        val = getQueryBusinessObject();
        hashCode ^= (val != null) ? val.hashCode() : 0;
        val = getQueryName();
        return hashCode ^ ((val != null) ? val.hashCode() : 0);
    }

}
