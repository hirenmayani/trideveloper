/*
Copyright (c) 2007, 2012 Eric Glass

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

package com.tririga.custom.print;

import com.tririga.pub.workflow.CustomBusinessConnectTask;
import com.tririga.pub.workflow.Record;

import com.tririga.ws.TririgaWS;

import com.tririga.ws.dto.Association;
import com.tririga.ws.dto.AssociationFilter;
import com.tririga.ws.dto.DisplayLabel;
import com.tririga.ws.dto.Field;
import com.tririga.ws.dto.FieldSortOrder;
import com.tririga.ws.dto.Filter;
import com.tririga.ws.dto.ObjectType;
import com.tririga.ws.dto.QueryResponseColumn;
import com.tririga.ws.dto.QueryResponseHelper;
import com.tririga.ws.dto.QueryResult;
import com.tririga.ws.dto.Section;

import com.tririga.ws.dto.content.Content;
import com.tririga.ws.dto.content.Response;

import com.tririga.ws.dto.gui.GUI;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.print.PageFormat;
import java.awt.print.Printable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import javax.print.StreamPrintServiceFactory;

import javax.swing.JEditorPane;
import javax.swing.RepaintManager;

import javax.swing.text.EditorKit;

import javax.swing.text.html.HTMLEditorKit;

import javax.swing.text.rtf.RTFEditorKit;

import jcifs.Config;

import jcifs.smb.SmbFile;

/**
 * Custom workflow task to print a record.  This formats the record against
 * a template in the Document Manager, then streams the output as either
 * plain text or PostScript to a Windows shared printer.
 */
public class PrintTask implements CustomBusinessConnectTask {

    public static final String HTML_CONTENT_TYPE = "HTML";

    public static final String RTF_CONTENT_TYPE = "RTF";

    public static final String TEXT_CONTENT_TYPE = "Text";

    private static final Map<String, ValueFormatter> FORMATTERS =
            createFormatterMap();

    private static final String PRINT_HELPER_MODULE = "triHelper";

    private static final String PRINT_HELPER_OBJECT_TYPE = "devPrintHelper";

    private static final String PRINT_HELPER_SERVER_FIELD = "devPrintServerTX";

    private static final String PRINT_HELPER_SHARE_FIELD = "devPrintShareTX";

    private static final String PRINT_HELPER_USERNAME_FIELD = "triUserNameTX";

    private static final String PRINT_HELPER_PASSWORD_FIELD = "triPasswordPA";

    private static final String PRINT_HELPER_CONTENT_TYPE_FIELD =
            "devContentTypeLI";

    private static final String RECORD_ID_FIELD = "triRecordIdSY";

    private static final String DOCUMENT_ASSOCIATION = "Has Document";

    private static final String RECORD_ASSOCIATION = "For";

    private static final DisplayLabel[] PRINT_HELPER_FIELDS =
            new DisplayLabel[] {
        createDisplayLabel(null, PRINT_HELPER_SERVER_FIELD),
        createDisplayLabel(null, PRINT_HELPER_SHARE_FIELD),
        createDisplayLabel(null, PRINT_HELPER_USERNAME_FIELD),
        createDisplayLabel(null, PRINT_HELPER_PASSWORD_FIELD),
        createDisplayLabel(null, PRINT_HELPER_CONTENT_TYPE_FIELD)
    };

    private static final String GENERAL_SECTION_TYPE = "General";

    private static final String DATE_TIME_FORMAT_TYPE = "Date and Time";

    private static final String DATE_FORMAT_TYPE = "Date";

    private static final String TIME_FORMAT_TYPE = "Time";

    private static final String DURATION_FORMAT_TYPE = "Duration";

    private static final int STRING_DATA_TYPE = 320;

    private static final int EQUALS_FILTER_OPERATION = 10;

    private static final int ALL_PROJECT_SCOPE = 2;

    public boolean execute(TririgaWS tririga, long userId, Record[] records) {
        if (records == null || records.length == 0) return true;
        boolean success = true;
        try {
            tririga.register(userId);
            for (Record record : records) {
                long recordId = record.getRecordId();
                String projectName = "";
                String moduleName = PRINT_HELPER_MODULE;
                String[] objectTypeNames = new String[] {
                    PRINT_HELPER_OBJECT_TYPE
                };
                String[] guiNames = null;
                String associatedModuleName = "";
                String associatedObjectTypeName = "";
                int projectScope = ALL_PROJECT_SCOPE;
                DisplayLabel[] displayFields = PRINT_HELPER_FIELDS;
                DisplayLabel[] associatedDisplayFields = null;
                Filter[] filters = new Filter[] {
                    createFieldFilter(RECORD_ID_FIELD,
                            STRING_DATA_TYPE, EQUALS_FILTER_OPERATION,
                                    String.valueOf(recordId))
                };
                AssociationFilter[] associationFilters = null;
                FieldSortOrder[] fieldSortOrders = null;
                int start = 1;
                int maximumResultCount = 1;
                QueryResult queryResult = tririga.runDynamicQuery(projectName,
                        moduleName, objectTypeNames, guiNames,
                        associatedModuleName, associatedObjectTypeName,
                        projectScope, displayFields, associatedDisplayFields,
                        fieldSortOrders, filters, associationFilters, start,
                        maximumResultCount);
                if (queryResult == null) {
                    throw new IllegalStateException(
                            "Unable to obtain PrintHelper record.");
                }
                QueryResponseHelper[] responses =
                        queryResult.getQueryResponseHelpers();
                if (responses == null || responses.length == 0) {
                    throw new IllegalStateException(
                            "No PrintHelper record returned.");
                }
                QueryResponseHelper response = responses[0];
                if (response == null) {
                    throw new IllegalStateException(
                            "Null PrintHelper record returned.");
                }
                Map<String, String> fields = getFields(response);
                String server = fields.get(PRINT_HELPER_SERVER_FIELD);
                String share = fields.get(PRINT_HELPER_SHARE_FIELD);
                String username = fields.get(PRINT_HELPER_USERNAME_FIELD);
                String password = fields.get(PRINT_HELPER_PASSWORD_FIELD);
                String contentType = fields.get(
                        PRINT_HELPER_CONTENT_TYPE_FIELD);
                if (server == null || "".equals(server)) {
                    throw new NullPointerException("No server provided.");
                }
                if (share == null || "".equals(share)) {
                    throw new NullPointerException("No share name provided.");
                }
                SmbFile printer = createPrinter(server, share, username,
                        password);
                Association[] associations = tririga.getAssociatedRecords(
                        recordId, DOCUMENT_ASSOCIATION, 2);
                if (associations == null || associations.length != 1) {
                    throw new IllegalStateException(
                            "Exactly one Document must be associated.");
                }
                long documentId = associations[0].getAssociatedRecordId();
                byte[] content = getDocumentContent(tririga, documentId);
                if (content == null) {
                    throw new IllegalStateException("No document content.");
                }
                associations = tririga.getAssociatedRecords(
                        recordId, RECORD_ASSOCIATION, -1);
                if (associations == null || associations.length == 0) {
                    render(content, contentType, printer, documentId + "-" +
                            System.currentTimeMillis());
                } else {
                    ContentReplacer replacer =
                            new ContentReplacer(new String(content, "UTF-8"));
                    for (Association association : associations) {
                        try {
                            long associatedId =
                                    association.getAssociatedRecordId();
                            printRecord(tririga, replacer, associatedId,
                                    contentType, printer);
                        } catch (Exception printError) {
                            success = false;
                            printError.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            success = false;
            ex.printStackTrace();
        } finally {
            return success;
        }
    }

    private static Map<String, ValueFormatter> createFormatterMap() {
        Map<String, ValueFormatter> formatters =
                new HashMap<String, ValueFormatter>();
        formatters.put(DATE_TIME_FORMAT_TYPE, new DateTimeValueFormatter());
        formatters.put(DATE_FORMAT_TYPE, new DateValueFormatter());
        formatters.put(TIME_FORMAT_TYPE, new TimeValueFormatter());
        formatters.put(DURATION_FORMAT_TYPE, new DurationValueFormatter());
        return formatters;
    }

    private static byte[] getDocumentContent(TririgaWS tririga, long docId) {
        try {
            Content content = new Content();
            content.setRecordId(docId);
            Response response = tririga.download(content);
            if (!response.getStatus().equalsIgnoreCase("success")) return null;
            ByteArrayOutputStream collector = new ByteArrayOutputStream();
            InputStream input = response.getContent().getInputStream();
            try {
                byte[] buf = new byte[8192];
                int count;
                while ((count = input.read(buf)) != -1) {
                    collector.write(buf, 0, count);
                }
                collector.flush();
                collector.close();
            } finally {
                try {
                    input.close();
                } catch (Exception ignore) { }
            }
            return collector.toByteArray();
        } catch (Exception ex) {
            return null;
        }
    }

    private static Map<ReplacementField, ValueFormatter> getFormatters(
            ObjectType bo) {
        Map<ReplacementField, ValueFormatter> formatters =
                new HashMap<ReplacementField, ValueFormatter>();
        Section[] sections = bo.getSections();
        if (sections == null || sections.length == 0) return formatters;
        for (Section section : sections) {
            if (section == null) continue;
            Field[] fields = section.getFields();
            if (fields == null || fields.length == 0) continue;
            String sectionName = section.getName();
            boolean isGeneral = GENERAL_SECTION_TYPE.equals(section.getType());
            for (Field field : fields) {
                if (field == null) continue;
                String fieldName = field.getName();
                ValueFormatter formatter = FORMATTERS.get(field.getType());
                if (formatter == null) continue;
                formatters.put(new ReplacementField(sectionName, fieldName),
                        formatter);
                if (isGeneral) {
                    formatters.put(new ReplacementField(null, fieldName),
                            formatter);
                }
            }
        }
        return formatters;
    }

    private static Set<String> getGeneralSections(ObjectType bo) {
        Set<String> generalSections = new HashSet<String>();
        Section[] sections = bo.getSections();
        if (sections == null || sections.length == 0) return generalSections;
        for (Section section : sections) {
            if (section == null) continue;
            if (!GENERAL_SECTION_TYPE.equals(section.getType())) continue;
            String sectionName = section.getName();
            if (sectionName != null) generalSections.add(sectionName);
        }
        return generalSections;
    }

    private static void printRecord(TririgaWS tririga, ContentReplacer replacer,
            long recordId, String fileType, SmbFile printer) throws Exception {
        Set<ReplacementField> replaceFields = replacer.getReplacementFields();
        if (replaceFields == null || replaceFields.size() == 0) {
            render(replacer.toString().getBytes("UTF-8"), fileType, printer,
                    recordId + "-" + System.currentTimeMillis());
            return;
        }
        GUI gui = tririga.getDefaultGUI(recordId);
        String moduleName = gui.getModuleName();
        String objectTypeName = gui.getObjectTypeName();
        String[] objectTypeNames = new String[] { objectTypeName };
        ObjectType bo = tririga.getObjectTypeByName(moduleName, objectTypeName);
        Set<String> generalSections = getGeneralSections(bo);
        Map<ReplacementField, ValueFormatter> formatters = getFormatters(bo);
        String[] guiNames = null;
        String projectName = "";
        String associatedModuleName = "";
        String associatedObjectTypeName = "";
        int projectScope = ALL_PROJECT_SCOPE;
        List<DisplayLabel> fields = new ArrayList<DisplayLabel>();
        for (ReplacementField replaceField : replaceFields) {
            fields.add(createDisplayLabel(replaceField.getSectionName(),
                    replaceField.getFieldName()));
        }
        DisplayLabel[] displayFields =
                fields.toArray(new DisplayLabel[fields.size()]);
        DisplayLabel[] associatedDisplayFields = null;
        Filter[] filters = new Filter[] {
            createFieldFilter(RECORD_ID_FIELD,
                    STRING_DATA_TYPE, EQUALS_FILTER_OPERATION,
                            String.valueOf(recordId))
        };
        AssociationFilter[] associationFilters = null;
        FieldSortOrder[] fieldSortOrders = null;
        int start = 1;
        int maximumResultCount = 2;
        QueryResult queryResult = tririga.runDynamicQuery(projectName,
                moduleName, objectTypeNames, guiNames,
                associatedModuleName, associatedObjectTypeName,
                projectScope, displayFields, associatedDisplayFields,
                fieldSortOrders, filters, associationFilters, start,
                maximumResultCount);
        if (queryResult == null) {
            throw new IllegalStateException(
                    "Unable to obtain content record.");
        }
        QueryResponseHelper[] responses =
                queryResult.getQueryResponseHelpers();
        if (responses == null || responses.length == 0) {
            throw new IllegalStateException(
                    "No content record returned.");
        }
        if (responses.length > 1) {
            throw new IllegalStateException(
                    "Multiple content records returned.");
        }
        QueryResponseHelper response = responses[0];
        if (response == null) {
            throw new IllegalStateException(
                    "Null content record returned.");
        }
        Map<ReplacementField, String> replacements =
                new HashMap<ReplacementField, String>();
        QueryResponseColumn[] columns = response.getQueryResponseColumns();
        for (QueryResponseColumn column : columns) {
            String fieldName = column.getName();
            if (fieldName == null || "".equals(fieldName)) continue;
            String sectionName = column.getSection();
            if (sectionName == null || "".equals(sectionName)) {
                sectionName = null;
            }
            String value = column.getDisplayValue();
            if (value == null || "".equals(value)) {
                value = column.getValue();
                if (value == null || "".equals(value)) value = "";
            }
            ReplacementField columnField =
                    new ReplacementField(sectionName, fieldName);
            ValueFormatter formatter = formatters.get(columnField);
            if (formatter != null) value = formatter.toString(value);
            if (value == null) value = "";
            replacements.put(columnField, value);
            if (generalSections.contains(sectionName)) {
                replacements.put(new ReplacementField(null, fieldName), value);
            }
        }
        String content = replacer.toString(replacements);
        render(content.getBytes("UTF-8"), fileType, printer,
                recordId + "-" + System.currentTimeMillis());
    }
 
    private static void render(byte[] data, String fileType,
            SmbFile printer, String jobName) throws Exception {
        if (data == null) throw new IllegalStateException("No content.");
        EditorKit fileHandler;
        if (HTML_CONTENT_TYPE.equals(fileType)) {
            fileHandler = new HTMLEditorKit();
        } else if (RTF_CONTENT_TYPE.equals(fileType)) {
            fileHandler = new RTFEditorKit();
        } else {
            fileHandler = null;
        }
        if (fileHandler != null) {
            final JEditorPane pane = new JEditorPane();
            pane.setEditorKit(fileHandler);
            pane.setText(new String(data, "UTF-8"));
            Dimension preferred = pane.getPreferredSize();
            pane.setSize(preferred.width, preferred.height);
            pane.validate();
            Printable printable = new Printable() {
                public int print(Graphics g, PageFormat format, int pageIndex) {
                    RepaintManager.currentManager(
                            pane).setDoubleBufferingEnabled(false);
                    Dimension size = pane.getSize();
                    double panelWidth = size.width;
                    double panelHeight = size.height;
                    double pageHeight = format.getImageableHeight();
                    double pageWidth = format.getImageableWidth();
                    double scale = Math.max(pageWidth / panelWidth, 1.0d);
                    int totalNumPages = (int)
                            Math.ceil(scale * panelHeight / pageHeight);
                    if (pageIndex >= totalNumPages) {
                        return Printable.NO_SUCH_PAGE;
                    }
                    Graphics2D g2 = (Graphics2D) g;
                    g2.translate(format.getImageableX(),
                            format.getImageableY());
                    g2.translate(0f, -pageIndex * pageHeight);
                    g2.scale(scale, scale);
                    pane.paint(g2);
                    return Printable.PAGE_EXISTS;
                }
            };
            DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;
            Doc doc = new SimpleDoc(printable, flavor, null);
            StreamPrintServiceFactory[] factories =
                    StreamPrintServiceFactory.lookupStreamPrintServiceFactories(
                            null, "application/postscript");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            PrintService service = factories[0].getPrintService(buffer);
            DocPrintJob job = service.createPrintJob();
            job.print(doc, null);
            buffer.flush();
            buffer.close();
            data = buffer.toByteArray();
        }
        printer.print(new ByteArrayInputStream(data), jobName);
    }

    private static ByteArrayOutputStream readStream(InputStream input)
            throws IOException {
        if (input == null) return null;
        ByteArrayOutputStream collector = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int c;
        while ((c = input.read(buffer, 0, 1024)) != -1) {
            collector.write(buffer, 0, c);
        }
        return collector;
    }

    private static Map<String, String> getFields(QueryResponseHelper result) {
        Map<String, String> fields = new HashMap<String, String>();
        for (QueryResponseColumn column : result.getQueryResponseColumns()) {
            String name = column.getName();
            if (name == null || "".equals(name)) continue;
            String value = column.getDisplayValue();
            if (value == null || "".equals(value)) {
                value = column.getValue();
                if (value == null || "".equals(value)) value = "";
            }
            fields.put(name, value);
        }
        return fields;
    }

    private static SmbFile createPrinter(String server, String share,
            String username, String password) throws Exception {
        StringBuilder str = new StringBuilder("smb://");
        if (username != null) {
            int index = username.indexOf('\\');
            if (index != -1) {
                String domain = URLEncoder.encode(username.substring(0, index),
                        "UTF-8");
                username = domain + ';' +
                        URLEncoder.encode(username.substring(index + 1),
                                "UTF-8");
            } else {
                username = URLEncoder.encode(username, "UTF-8");
            }
        }
        password = (password == null) ? "" :
                URLEncoder.encode(password, "UTF-8");
        if (username != null) {
            str.append(username);
            str.append(':');
            str.append(password);
            str.append('@');
        }
        str.append(server);
        str.append("/");
        str.append(share);
        str.append("/");
        return new SmbFile(str.toString());
    }

    private static DisplayLabel createDisplayLabel(String sectionName,
            String fieldName) {
        if (sectionName == null) sectionName = "";
        DisplayLabel displayLabel = new DisplayLabel();
        displayLabel.setFieldName(fieldName);
        displayLabel.setLabel(fieldName);
        displayLabel.setSectionName(sectionName);
        return displayLabel;
    }

    private static Filter createFieldFilter(String fieldName, int dataType,
            int filterOperator, String value) {
        Filter filter = new Filter();
        filter.setDataType(dataType);
        filter.setFieldName(fieldName);
        filter.setOperator(filterOperator);
        filter.setSectionName("");
        filter.setValue(value);
        return filter;
    }

}
