/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 *
 * The language switch for the introduction page.
 *
 * Every sentence on the page is authored six times, in the markup itself, as
 * data-en / data-tr / data-de / data-fr / data-es / data-it attributes: the
 * English text is what a reader without JavaScript sees, and this file only
 * swaps in another language. Nothing is fetched - the page makes no request of
 * its own, which is the same promise the editor makes - and the choice is
 * remembered in localStorage so a returning reader lands in the language they
 * picked last time.
 *
 * The first visit follows the browser's own language list, falling back to
 * English. A stored choice always wins over the browser, so a Turkish reader on
 * an English system is not switched back on every visit.
 */
(function () {
    "use strict";

    var STORAGE_KEY = "jtypewriter.site.language";
    var FALLBACK = "en";

    // The six languages the editor itself ships, in the order the switch lists
    // them. Kept here rather than read off the <option> elements: this array is
    // what decides whether a stored or requested language is honoured, and it
    // must not grow by accident when the menu gains a disabled entry.
    var LANGUAGES = ["en", "tr", "de", "fr", "es", "it"];

    var select = document.getElementById("language");

    function known(value) {
        return LANGUAGES.indexOf(value) >= 0 ? value : null;
    }

    function stored() {
        try {
            return known(window.localStorage.getItem(STORAGE_KEY));
        } catch (error) {
            // Private mode, or storage disabled: the page still works, it just
            // forgets between visits.
            return null;
        }
    }

    function remember(language) {
        try {
            window.localStorage.setItem(STORAGE_KEY, language);
        } catch (error) {
            /* nothing to do: the switch is still correct for this visit */
        }
    }

    /**
     * A `?lang=de` (or `?lang=en`, ...) wins over everything: it makes each
     * translation a link you can hand to someone, which is the whole reason the
     * page exists. It is read but never persisted, so following a shared link
     * does not silently change the reader's own default.
     */
    function requested() {
        try {
            return known(new URLSearchParams(window.location.search).get("lang"));
        } catch (error) {
            return null;
        }
    }

    function preferred() {
        var asked = requested();
        if (asked) {
            return asked;
        }
        var saved = stored();
        if (saved) {
            return saved;
        }
        var languages = navigator.languages || [navigator.language || ""];
        for (var i = 0; i < languages.length; i++) {
            var tag = String(languages[i]).toLowerCase();
            var match = known(tag.split("-")[0]);
            if (match) {
                return match;
            }
        }
        return FALLBACK;
    }

    function apply(language) {
        document.documentElement.lang = language;

        document.querySelectorAll("[data-en]").forEach(function (element) {
            var text = element.getAttribute("data-" + language);
            if (text === null) {
                return;
            }
            if (element.tagName === "META") {
                element.setAttribute("content", text);
            } else {
                element.textContent = text;
            }
        });

        document.querySelectorAll("[data-src-en]").forEach(function (image) {
            // Only English and Turkish were recorded, so every other language
            // shows the English clip rather than the previous language's: a
            // German reader seeing the Turkish recording would be worse than
            // seeing a language they can at least follow.
            var source = image.getAttribute("data-src-" + language)
                    || image.getAttribute("data-src-" + FALLBACK);
            if (source) {
                image.setAttribute("src", source);
            }
            var alternative = image.getAttribute("data-alt-" + language)
                    || image.getAttribute("data-alt-" + FALLBACK);
            if (alternative) {
                image.setAttribute("alt", alternative);
            }
        });

        if (select) {
            select.value = language;
        }
    }

    if (select) {
        select.addEventListener("change", function () {
            var language = known(select.value);
            if (language) {
                apply(language);
                remember(language);
            }
        });
    }

    apply(preferred());
}());
