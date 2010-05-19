package com.trideveloper.spelling;

import com.swabunga.spell.engine.SpellDictionary;
import com.swabunga.spell.engine.SpellDictionaryHashMap;
import com.swabunga.spell.event.SpellCheckEvent;
import com.swabunga.spell.event.SpellCheckListener;
import com.swabunga.spell.event.SpellChecker;
import com.swabunga.spell.event.StringWordTokenizer;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONStringer;

public class SpellingServlet extends HttpServlet {

    private static final String DICTIONARY_FILE = "spellcheck.txt";

    private static final String SCRIPT_PAGE = "/WEB-INF/spellcheck.jsp";

    private static final String RESULTS_PAGE = "/WEB-INF/spellcheck.html";

    private static final String TARGET_URL_ATTRIBUTE = "targetUrl";

    private static final Timer UPDATE_TIMER =
            new Timer("SpellingDictionaryUpdate", true);

    private static final long DEFAULT_UPDATE_PERIOD = 300000;

    private SpellDictionary dictionary;

    public void init() throws ServletException {
        try {
            loadDictionary();
        } catch (IOException ex) {
            log("Unable to load dictionary: " + ex.getMessage(), ex);
            throw new UnavailableException(ex.getMessage());
        }
        long updatePeriod = -1l;
        try {
            String period = getInitParameter("updatePeriod");
            if (period != null) updatePeriod = Long.parseLong(period);
        } catch (Exception ex) {
            log("Invalid updatePeriod value specified: " + ex.getMessage(), ex);
        }
        if (updatePeriod < 0l) {
            log("Using default updatePeriod value of " + DEFAULT_UPDATE_PERIOD +
                    ".");
            updatePeriod = DEFAULT_UPDATE_PERIOD;
        }
        if (updatePeriod != 0l) {
            TimerTask dictionaryUpdate = new TimerTask() {
                public void run() {
                    try {
                        loadDictionary();
                    } catch (IOException ex) {
                        log("Unable to update spelling dictionary: " +
                                ex.getMessage(), ex);
                    }
                }
            };
            UPDATE_TIMER.schedule(dictionaryUpdate, updatePeriod, updatePeriod);
            log("Scheduled spelling dictionary update at intervals of " +
                    updatePeriod + " milliseconds.");
        } else {
            log("Spelling dictionary updatePeriod value was set to 0; " +
                    "updates disabled.");
        }
    }

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        boolean results = Boolean.parseBoolean(request.getParameter("results"));
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(
                results ? RESULTS_PAGE : SCRIPT_PAGE);
        request.setAttribute(TARGET_URL_ATTRIBUTE, request.getRequestURI());
        dispatcher.forward(request, response);
    }

    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        String jsonString = "";
        String text = request.getParameter("text");
        try {
            if (text == null) {
                jsonString = "null";
            } else {
                text = text.replaceAll("\r\n", "\n");
                List<SpellCheckEvent> errors = getErrors(text);
                if (errors == null || errors.isEmpty()) {
                    jsonString = "null";
                } else {
                    JSONStringer json = new JSONStringer();
                    json.array();
                    for (SpellCheckEvent error : errors) {
                        json.object();
                        json.key("start");
                        json.value(error.getWordContextPosition());
                        json.key("value");
                        json.value(error.getInvalidWord());
                        List suggestions = error.getSuggestions();
                        if (suggestions != null && !suggestions.isEmpty()) {
                            json.key("suggestions");
                            json.array();
                            for (Object suggestion : suggestions) {
                                json.value(suggestion.toString());
                            }
                            json.endArray();
                        }
                        json.endObject();
                    }
                    json.endArray();
                    jsonString = json.toString();
                }
            }
        } catch (JSONException ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.flushBuffer();
            return;
        }
        response.setContentType("application/json");
        byte[] content = jsonString.getBytes("UTF-8");
        response.setContentLength(content.length);
        OutputStream out = response.getOutputStream();
        out.write(content);
        out.flush();
        out.close();
    }

    private List<SpellCheckEvent> getErrors(String text) {
        final List<SpellCheckEvent> errors = new ArrayList<SpellCheckEvent>();
        SpellChecker checker = new SpellChecker(dictionary);
        checker.addSpellCheckListener(new SpellCheckListener() {
            public void spellingError(SpellCheckEvent event) {
                errors.add(event);
            }
        });
        checker.checkSpelling(new StringWordTokenizer(text));
        return errors.isEmpty() ? null : errors;
    }

    private void loadDictionary() throws IOException {
        InputStream stream = getClass().getResourceAsStream("/" +
                DICTIONARY_FILE);
        if (stream == null) {
            throw new FileNotFoundException("No dictionary file \"" +
                    DICTIONARY_FILE + "\" found; this file should be in " +
                            "the \"config\" directory.");
        }
        SpellDictionary dictionary = new SpellDictionaryHashMap(
                new InputStreamReader(stream, "ISO-8859-1"));
        this.dictionary = dictionary;
    }

}
