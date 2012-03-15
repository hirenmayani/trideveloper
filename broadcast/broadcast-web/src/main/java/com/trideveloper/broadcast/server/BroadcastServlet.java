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

package com.trideveloper.broadcast.server;

import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A Servlet which uses HTTP streaming to dispatch events to connected
 * clients.
 */
public class BroadcastServlet extends HttpServlet {

    /**
     * Default maximum time client connections should remain open in
     * milliseconds (20000 milliseconds).
     */
    public static final long DEFAULT_MAX_CONNECTION_DURATION = 20000l;

    /**
     * Default maximum size of record batch that will be dispatched to a
     * single client connection (5 records).
     */
    public static final int DEFAULT_MAX_BATCH_SIZE = 5;

    private long maxConnectionDuration = DEFAULT_MAX_CONNECTION_DURATION;

    private int maxBatchSize = DEFAULT_MAX_BATCH_SIZE;

    private final List eventRecords = new LinkedList();

    /**
     * Initializes the servlet and configures properties according to
     * the initialization parameters in web.xml.  No parameters are required,
     * but the servlet supports "maxConnectionDuration", specifying the maximum
     * number of milliseconds client connections should remain open, and
     * "maxBatchSize", specifying the maximum number of records that will
     * be dispatched in a single batch.
     *
     * @throws ServletException If an error occurs during initialization. 
     */
    public void init() throws ServletException {
        try {
            ServletConfig config = getServletConfig();
            String maxConnectionDuration =
                    config.getInitParameter("maxConnectionDuration");
            if (maxConnectionDuration != null) {
                this.maxConnectionDuration =
                        Long.parseLong(maxConnectionDuration);
            }
            String maxBatchSize =
                    config.getInitParameter("maxBatchSize");
            if (maxBatchSize != null) {
                this.maxBatchSize = Integer.parseInt(maxBatchSize);
            }
        } catch (Exception ex) {
            throw new UnavailableException(ex.getMessage());
        }
    }

    /**
     * Handles POST requests to the servlet, which are used to submit records
     * for broadcast.  Submitted records are queued for delivery to
     * subscribers.
     *
     * @param request The request. 
     * @param response The response.
     *
     * @throws ServletException If an application error occurs.
     * @throws IOException If an IO error occurs. 
     */
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String records = request.getParameter("records");
        if (records == null || "".equals(records = records.trim())) return;
        Set recordSet = new HashSet();
        String[] requestRecords = records.split("\\s");
        String record;
        for (int i = 0; i < requestRecords.length; i++) {
            record = requestRecords[i];
            if (record == null || "".equals(record)) continue;
            try {
                recordSet.add(new Long(record));
            } catch (Exception ignore) { }
        }
        if (recordSet.isEmpty()) return;
        // lock the event queue; add the new records, and wake all clients.
        synchronized (eventRecords) {
            eventRecords.addAll(recordSet);
            eventRecords.notifyAll();
        }
        response.flushBuffer();
    }

    /**
     * Handles GET requests to the servlet, which are used to subscribe for
     * events.  The request will be held in a response status until either
     * maxConnectionDuration is reached or an event enters the queue.
     * Pending events are then consumed and sent to the client (up to
     * maxBatchSize). 
     *
     * @param request The request. 
     * @param response The response.
     *
     * @throws ServletException If an application error occurs.
     * @throws IOException If an IO error occurs. 
     */
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        long baseTime = System.currentTimeMillis();
        long timeout = baseTime + maxConnectionDuration;
        Set recordSet = new HashSet();
        synchronized (eventRecords) {
            while ((baseTime = System.currentTimeMillis()) < timeout &&
                    eventRecords.isEmpty()) {
                // if no records are in the queue, sleep until notified.
                try {
                    eventRecords.wait(Math.max(1000, timeout - baseTime));
                } catch (InterruptedException ex) { }
            }
            Iterator records = eventRecords.iterator();
            // deliver up to maxBatchSize from the pending queue.
            for (int i = 0; i < maxBatchSize && records.hasNext(); i++) {
                recordSet.add(records.next());
                records.remove();
            }
        }
        sendRecords(recordSet, request, response);
    }

    private void sendRecords(Set recordSet, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        StringBuffer buffer = new StringBuffer();
        if (recordSet != null && recordSet.size() > 0) {
            buffer.append("[");
            Iterator records = recordSet.iterator();
            while (records.hasNext()) {
                buffer.append(records.next());
                if (records.hasNext()) buffer.append(",");
            }
            buffer.append("]");
        } else {
            buffer.append("null");
        }
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "-1");
        response.setContentType("application/x-javascript");
        response.getWriter().println(buffer.toString());
        response.flushBuffer();
    }

}
