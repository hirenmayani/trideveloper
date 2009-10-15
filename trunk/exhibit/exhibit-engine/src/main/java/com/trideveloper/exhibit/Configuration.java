package com.trideveloper.exhibit;

import java.io.FileInputStream;
import java.io.InputStream;

import java.net.URL;

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

class Configuration {

    private static final String BASE_CONFIGURATION_RESOURCE =
            Configuration.class.getName();

    private static final String CONFIGURATION_RESOURCE = "EXHIBIT";

    private static final Properties CONFIGURATION_PROPERTIES =
            createProperties();

    public static Properties getProperties() {
        return (Properties) CONFIGURATION_PROPERTIES.clone();
    }

    public static String getProperty(String name) {
        return CONFIGURATION_PROPERTIES.getProperty(name);
    }

    private static Properties createProperties() {
        Properties baseProperties = new Properties();
        ResourceBundle configuration =
                ResourceBundle.getBundle(BASE_CONFIGURATION_RESOURCE);
        Enumeration keys = configuration.getKeys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            baseProperties.setProperty(key, configuration.getString(key));
        }
        Properties userProperties = new Properties();
        String file = null;
        try {
            file = System.getProperty(CONFIGURATION_RESOURCE + ".properties");
        } catch (Exception ignore) { }
        if (file != null) {
            InputStream stream = null;
            try {
                stream = new FileInputStream(file);
            } catch (Exception ex) {
                try {
                    stream = (new URL(file)).openStream();
                } catch (Exception e) { }
            }
            if (stream == null) {
                throw new IllegalStateException(
                        "Unable to load configuration.");
            }
            try {
                userProperties.load(stream);
            } catch (Exception ex) {
                throw new IllegalStateException("Error loading configuration.");
            }
        } else {
            try {
                configuration =
                        ResourceBundle.getBundle(CONFIGURATION_RESOURCE);
                keys = configuration.getKeys();
                while (keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    userProperties.setProperty(key,
                            configuration.getString(key));
                }
            } catch (MissingResourceException ex) { }
        }
        baseProperties.putAll(userProperties);
        return baseProperties;
    }

}
