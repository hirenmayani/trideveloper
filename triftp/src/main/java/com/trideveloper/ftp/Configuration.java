package com.trideveloper.ftp;

import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;

public class Configuration {

    private static final String CONFIGURATION_RESOURCE = "TRIFTP";

    private static final Properties CONFIGURATION_PROPERTIES = new Properties();

    static {
        try {
            ResourceBundle configuration =
                    ResourceBundle.getBundle(CONFIGURATION_RESOURCE);
            Enumeration keys = configuration.getKeys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                CONFIGURATION_PROPERTIES.setProperty(key,
                        configuration.getString(key));
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to load configuration: " +
                    ex.getMessage());
        }
    }

    public static Properties getProperties() {
        return (Properties) CONFIGURATION_PROPERTIES.clone();
    }

    public static String getString(String name) {
        return CONFIGURATION_PROPERTIES.getProperty(name);
    }

    public static boolean getBoolean(String name) {
        return Boolean.parseBoolean(getString(name));
    }

}
