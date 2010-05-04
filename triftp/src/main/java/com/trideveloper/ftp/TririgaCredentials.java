package com.trideveloper.ftp;

import java.security.Principal;

public class TririgaCredentials implements Principal {

    private String name;

    private String password;

    public TririgaCredentials(String username, String password) {
        this.name = username;
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String toString() {
        return name;
    }

    public boolean equals(Object o) {
        if (!(o instanceof TririgaCredentials)) return false;
        TririgaCredentials other = (TririgaCredentials) o;
        String val = getName();
        if ((val != null) ? !val.equals(other.getName()) :
                other.getName() != null) {
            return false;
        }
        val = getPassword();
        return (val != null) ? val.equals(other.getPassword()) :
                other.getPassword() == null;
    }

    public int hashCode() {
        String val = getName();
        int hashCode = (val != null) ? val.hashCode() : 0;
        val = getPassword();
        return hashCode ^ ((val != null) ? val.hashCode() : 0);
    }

}
