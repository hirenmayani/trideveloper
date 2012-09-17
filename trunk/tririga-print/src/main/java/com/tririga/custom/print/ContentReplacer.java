package com.tririga.custom.print;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContentReplacer {

    private final List<Object> tokens = new ArrayList<Object>();

    private final Set<ReplacementField> replacementFields =
            new HashSet<ReplacementField>();

    public ContentReplacer(String content) {
        Pattern pattern = Pattern.compile("%%([^%_]*?)_([^%]*?)%%");
        Matcher matcher = pattern.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String sectionName = matcher.group(1).trim();
            String fieldName = matcher.group(2).trim();
            buffer.setLength(0);
            matcher.appendReplacement(buffer, "");
            tokens.add(buffer.toString());
            ReplacementField replace =
                    new ReplacementField(sectionName, fieldName);
            tokens.add(replace);
            replacementFields.add(replace);
        }
        buffer.setLength(0);
        matcher.appendTail(buffer);
        tokens.add(buffer.toString());
    }

    public Set<ReplacementField> getReplacementFields() {
        return Collections.unmodifiableSet(replacementFields);
    }

    public String toString() {
        return toString(new HashMap<ReplacementField, String>());
    }

    public String toString(Map<ReplacementField, String> replacements) {
        StringBuilder str = new StringBuilder();
        for (Object token : tokens) {
            if (token instanceof String) {
                str.append(token);
            } else if (token instanceof ReplacementField) {
                String value = "";
                ReplacementField field = (ReplacementField) token;
                if (replacements.containsKey(field)) {
                    value = replacements.get(field);
                } else {
                    field = new ReplacementField(null, field.getFieldName());
                    if (replacements.containsKey(field)) {
                        value = replacements.get(field);
                    }
                }
                if (value == null) value = "";
                str.append(value);
            } else {
                throw new IllegalStateException("Replacement error.");
            }
        }
        return str.toString();
    }

}
