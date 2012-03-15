/*
Copyright (c) 2006 Eric Glass

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.tririga.custom.broadcast;

import com.tririga.pub.workflow.CustomTask;
import com.tririga.pub.workflow.Record;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Custom workflow task to submit records for broadcast.
 */
public class BroadcastTask implements CustomTask {

    /**
     * The name of the properties file that the task will look for; this
     * should be present in the "config" directory alongside the other
     * Tririga properties files.  The name of this file is
     * "TRIRIGABROADCAST.properties".  This file is required if a URL other
     * than the default is being used for broadcast connections.
     */
    public static final String PROPERTIES_NAME = "TRIRIGABROADCAST";

    /**
     * The default broadcast connection URL
     * ("http://localhost:8001/broadcast/broadcast").
     */ 
    public static final String DEFAULT_BROADCAST_URL =
            "http://localhost:8001/broadcast/broadcast";

    /**
     * The property entry in the properties file that specifies the broadcast
     * connection URL.  This string is "BROADCAST_URL".  If no such entry
     * is present in the file (or if the file is missing) the default
     * broadcast URL will be used.
     */
    public static final String BROADCAST_URL_PROPERTY = "BROADCAST_URL";

    /**
     * Executes the custom task; connects to the broadcast URL and submits
     * the specified records to the broadcaster.
     *
     * @param records The records from the workflow task.
     *
     * @return A <code>boolean</code> indicating the success or failure of
     * this workflow task execution.
     */ 
    public boolean execute(Record[] records) {
        if (records == null || records.length == 0) return true;
        try {
            String broadcastUrl = DEFAULT_BROADCAST_URL;
            try {
                ResourceBundle bundle = ResourceBundle.getBundle(
                        PROPERTIES_NAME);
                broadcastUrl = bundle.getString(BROADCAST_URL_PROPERTY);
            } catch (MissingResourceException ignore) { }
            URL url = new URL(broadcastUrl);
            StringBuffer requestBuffer = new StringBuffer();
            Record record;
            for (int i = records.length - 1; i >= 0; i--) {
                record = records[i];
                if (record == null) continue;
                requestBuffer.append(String.valueOf(record.getRecordId()));
                requestBuffer.append(" ");
            }
            String request = (requestBuffer.length() > 0) ? "records=" +
                    URLEncoder.encode(requestBuffer.toString().trim(),
                            "UTF-8") : "";
            HttpURLConnection connection = (HttpURLConnection)
                    url.openConnection();
            connection.addRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            byte[] content = request.getBytes("UTF-8");
            connection.addRequestProperty("Content-Length",
                    String.valueOf(content.length));
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.connect();
            OutputStream output = connection.getOutputStream();
            output.write(content);
            output.flush();
            output.close();
            byte[] buffer = new byte[1024];
            int count;
            try {
                InputStream input = connection.getInputStream();
                while ((count = input.read(buffer, 0, 1024)) != -1) {
                    // discard
                }
                input.close();
            } catch (IOException ioErr) {
                try {
                    InputStream error = connection.getErrorStream();
                    while ((count = error.read(buffer, 0, 1024)) != -1) {
                        // discard
                    }
                    error.close();
                    connection.disconnect();
                } catch (Exception ignore) { }
                throw ioErr;
            }
            connection.disconnect();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
