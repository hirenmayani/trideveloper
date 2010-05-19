<%@ page contentType="text/javascript" %>

<jsp:useBean id="targetUrl" scope="request" type="java.lang.String"/>

if (typeof XMLHttpRequest == "undefined") {
    XMLHttpRequest = function () {
        try {
            return new ActiveXObject("Msxml2.XMLHTTP.6.0");
        } catch (ignore) { }
        try {
            return new ActiveXObject("Msxml2.XMLHTTP.3.0");
        } catch (ignore) { }
        try {
            return new ActiveXObject("Msxml2.XMLHTTP");
        } catch (ignore) { }
        throw new Error("This browser does not support XMLHttpRequest.");
    };
}

var SpellChecker = function() {

    var errorWindow = null;

    var errors = null;

    var index = -1;

    var ignoreList = [];

    var replacements = {};

    function getErrorReplacement() {
        var value = null;
        try {
            if (!errors || index < 0) return true;
            var error = errors[index];
            if (!error) return true;
            value = error.value;
        } catch (ignore) { }
        if (!replacements[value]) return value;
        return replacements[value];
    }

    function ignoreError() {
        var value = null;
        try {
            if (!errors || index < 0) return;
            var error = errors[index];
            if (!error) return true;
            value = error.value;
            if (!value || value == "") return;
            ignoreList.push(value);
        } catch (ignore) {
            // do nothing
        } finally {
            nextError();
        }
    }

    function replaceError(replaceText) {
        try {
            if (!errors || index < 0) return;
            var error = errors[index];
            if (!error) return;
            try {
                error.range.text = replaceText;
                var element = error.range.parentElement();
            } catch (e) { }
            replacements[error.value] = replaceText;
        } catch (ignore) {
            // do nothing
        } finally {
            nextError();
        }
    }

    function cancelSpellCheck() {
        if (errorWindow) {
            try {
                errorWindow.close();
                errorWindow = null;
            } catch (ignore) { }
        }
        errors = null;
        index = -1;
    }

    function getErrorSuggestions() {
        try {
            if (!errors || index < 0) return null;
            var error = errors[index];
            if (!error) return null;
            return error.suggestions;
        } catch (e) {
            return null;
        }
    }

    function getErrorWord() {
        if (!errors || index < 0) return null;
        var error = errors[index];
        if (!error) return null;
        return error.value;
    }

    function nextError() {
        function isIgnored() {
            var value = null;
            try {
                if (!errors || index < 0) return true;
                var error = errors[index];
                if (!error) return true;
                value = error.value;
            } catch (ignore) { }
            if (!value || value == "") return true;
            for (var i = 0; i < ignoreList.length; i++) {
                if (ignoreList[i] == value) return true;
            }
            return false;
        }
        function selectError() {
            try {
                if (!errors || index < 0) return false;
                var error = errors[index];
                if (!error) return false;
                error.range.select();
                return true;
            } catch (e) {
                return false;
            }
        }
        var error = null;
        index++;
        if (index >= errors.length) {
            cancelSpellCheck();
            return;
        }
        error = errors[index];
        if (!error || isIgnored() || !selectError()) {
            nextError();
            return;
        }
        var width = 440;
        var height = 220;
        var left = parseInt((screen.availWidth / 2) - (width / 2));
        var top = parseInt((screen.availHeight / 2) - (height / 2))
        errorWindow = window.open("<%= targetUrl %>?results=true", "spellcheck",
                "top=" + top + ",left=" + left + ",width=" + width +
                ",height=" + height + ",resizable=no,scrollbars=no,toolbar=no" +
                ",location=no,directories=no,status=no,menubar=no" +
                ",copyhistory=no,personalbar=no,channelmode=no,dependent=yes" +
                ",dialog=yes,minimizable=no");
        selectError();
    }

    function spellCheck(target) {
        if (typeof target == "undefined" || !target) {
            target = parent.document;
        }
        function trim(str) {
            return str.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
        }
        function createRange(element, selStart, selLength) {
            var range = element.createTextRange();
            var elementContent = element.value ? element.value :
                    element.innerText;
            var numLines = elementContent.split("\n").length - 1;
            range.move("character", elementContent.length);
            range.move("character",
                    selStart - elementContent.length + numLines);
            range.moveEnd("character", selLength);
            return range;
        }
        function getErrors(text) {
            var request = new XMLHttpRequest();
            request.open("POST", "<%= targetUrl %>", false);
            request.setRequestHeader("Content-Type",
                    "application/x-www-form-urlencoded");
            request.send("text=" + encodeURIComponent(text));
            if (typeof JSON == "undefined") {
                return eval("(" + request.responseText + ")");
            } else {
                return JSON.parse(request.responseText);
            }
        }
        function checkIgnored(value) {
            if (!value || value == "") return true;
            for (var i = 0; i < ignoreList.length; i++) {
                if (ignoreList[i] == value) return true;
            }
            return false;
        }
        var errorList = [];
        function processItem(item) {
            var itemType = item.tagName.toLowerCase();
            if (itemType == "textarea" || (itemType == "input" &&
                    item.type.toLowerCase() == "text")) {
                var text = item.value;
                if (!text || trim(text) == "") return;
                try {
                    item.focus();
                } catch (ignore) { }
                var textErrors = getErrors(text);
                if (textErrors) {
                    for (var j = 0; j < textErrors.length; j++) {
                        var textError = textErrors[j];
                        if (checkIgnored(textError.value)) continue;
                        var range = createRange(item, textError.start,
                                textError.value.length);
                        if (!range) continue;
                        textError.range = range;
                        errorList.push(textError);
                    }
                }
            } else if (itemType == "iframe") {
                var itemDocument = item.contentDocument ? item.contentDocument :
                        item.contentWindow.document;
                if (itemDocument && (itemDocument.designMode.toString(
                        ).toLowerCase() == "on" ||
                                itemDocument.body.contentEditable.toString(
                                        ).toLowerCase() == "true")) {
                    var text = itemDocument.body.innerText;
                    if (!text || trim(text) == "") return;
                    try {
                        item.focus();
                    } catch (ignore) { }
                    var textErrors = getErrors(text);
                    if (textErrors) {
                        for (var j = 0; j < textErrors.length; j++) {
                            var textError = textErrors[j];
                            if (checkIgnored(textError.value)) continue;
                            var range = createRange(itemDocument.body,
                                    textError.start, textError.value.length);
                            if (!range) continue;
                            textError.range = range;
                            errorList.push(textError);
                        }
                    }
                }
            }
        }
        if (target.tagName) {
            processItem(target);
        } else {
            var items = parent.document.getElementsByTagName("*");
            for (var i = 0; i < items.length; i++) {
                processItem(items[i]);
            }
        }
        index = -1;
        if (errorList.length > 0) {
            errors = errorList;
            nextError();
        } else {
            alert("No spelling errors found.");
            cancelSpellCheck();
        }
    }

    return {
        check: spellCheck,
        getWord: getErrorWord,
        getReplacement: getErrorReplacement,
        getSuggestions: getErrorSuggestions,
        next: nextError,
        ignore: ignoreError,
        replace: replaceError,
        cancel: cancelSpellCheck
    };

}();


