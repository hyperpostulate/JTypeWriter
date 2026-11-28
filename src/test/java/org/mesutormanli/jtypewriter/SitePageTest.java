/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The introduction page under {@code site/} and the workflow that publishes it.
 *
 * <p>Nothing here renders the page - there is no browser in the suite and no
 * toolkit may be started - so these are the properties that would otherwise rot
 * silently: every sentence exists in both languages, the page asks no third
 * party for anything, the one action it offers goes to the release page rather
 * than to a hard-coded version, and the deploy job borrows the hero recordings
 * from {@code docs/media} instead of keeping a second copy that can drift from
 * the README's.</p>
 *
 * <p>Read-only: a handful of files, no network, no writes, no display.</p>
 */
class SitePageTest {

    private static final Path SITE = Path.of("site");
    private static final Path INDEX = SITE.resolve("index.html");
    private static final Path WORKFLOW = Path.of(".github", "workflows", "pages.yml");
    private static final Path GITIGNORE = Path.of(".gitignore");

    private static final String RELEASES_LATEST =
            "https://github.com/hyperpostulate/JTypeWriter/releases/latest";

    /**
     * The six languages the editor itself ships, in the order the page lists
     * them. Deliberately the same set as {@code Messages}' bundles: a page that
     * offers a language the application cannot speak would be a promise the
     * download does not keep.
     */
    private static final List<String> LANGUAGES =
            List.of("en", "tr", "de", "fr", "es", "it");

    /**
     * Every translated string is authored once per language in the markup, so a
     * new sentence added to one of them and forgotten in the others shows up as
     * an untranslated card rather than as a compile error. Counted rather than
     * parsed: the attribute names are unambiguous in this file, and it is the
     * exact {@code data-<language>} spelling the script reads.
     */
    @Test
    void everySentenceExistsInEveryLanguage() throws IOException {
        var html = read(INDEX);

        for (var prefix : List.of("data-", "data-alt-")) {
            for (var language : LANGUAGES) {
                var english = count(html, prefix + "en=\"");
                var actual = count(html, prefix + language + "=\"");
                assertTrue(english > 0, "the page has no " + prefix + "en attributes");
                assertTrue(english == actual,
                        actual + " " + prefix + language + " against " + english + " "
                                + prefix + "en: the " + language + " text is incomplete");
            }
        }

        // The recordings are the exception, and deliberately so: only English and
        // Turkish were ever captured (scripts/capture-hero-gif.sh records the
        // packaged image in those two halves), so the other four languages fall
        // back to the English clip inside the script rather than to a second pair
        // of files nobody recorded.
        assertTrue(count(html, "data-src-en=\"") == 1, "the page has no English recording");
        assertTrue(count(html, "data-src-tr=\"") == 1, "the page has no Turkish recording");
    }

    /**
     * The two sentences at the top of the page are also the two lines the
     * welcome and About cards print, in all six languages - the user asked for
     * one text rather than a second translation to keep in step, and this is
     * what makes "one text" true instead of aspirational. The page's
     * {@code .tagline} paragraph is the bundles' {@code appTagline} and its
     * {@code .lede} is {@code appTaglineDetail}; the card shows them as two
     * labels so the second starts on its own line.
     */
    @Test
    void theCardsOpeningLinesAreThePagesOpeningParagraphs() throws IOException {
        var html = read(INDEX);

        for (var language : LANGUAGES) {
            var messages = bundle(language);

            assertEquals(squash(attributeOf(html, "tagline", language)),
                    squash(messages.getString("appTagline")),
                    "the " + language + " appTagline has drifted from the page's .tagline");
            assertEquals(squash(attributeOf(html, "lede", language)),
                    squash(messages.getString("appTaglineDetail")),
                    "the " + language + " appTaglineDetail has drifted from the page's .lede");
        }
    }

    /**
     * The switch is a native dropdown, not a scripted listbox: on a phone it
     * opens the system picker, and each option carries its own {@code lang} so a
     * screen reader says "Türkçe" as Turkish instead of mangling it. The order
     * is pinned because the user asked for exactly this one, and a sorted or
     * reordered list is the kind of thing nobody notices until a reader does.
     */
    @Test
    void theLanguageSwitchOffersSixLanguagesInOrder() throws IOException {
        var html = read(INDEX);

        assertTrue(html.contains("<select id=\"language\" name=\"language\">"),
                "the language switch is not a labelled native select");
        assertTrue(html.contains("for=\"language\""),
                "the select has no label, so a screen reader cannot name it");

        var offered = new ArrayList<String>();
        var matcher = Pattern.compile("<option value=\"([a-z]{2})\"").matcher(html);
        while (matcher.find()) {
            offered.add(matcher.group(1));
        }
        assertEquals(LANGUAGES, offered, "the dropdown does not list the six languages in order");
    }

    /**
     * The page may reach GitHub and nothing else. This is the editor's own
     * promise applied to its introduction: no font CDN, no analytics, no
     * tracking pixel, no embed. A stylesheet or script from another origin would
     * also make the page break wherever GitHub is reachable but that host is
     * not.
     */
    @Test
    void thePageMakesNoThirdPartyRequest() throws IOException {
        var html = read(INDEX);

        for (var tag : List.of("<link", "<script", "<img")) {
            var matcher = Pattern.compile(tag + "[^>]*\\b(?:href|src)=\"([^\"]+)\"")
                    .matcher(html);
            while (matcher.find()) {
                var target = matcher.group(1);
                assertFalse(isRemote(target),
                        tag + " loads a third-party resource: " + target);
            }
        }

        for (var external : externalLinks(html)) {
            assertTrue(external.startsWith("https://github.com/hyperpostulate/JTypeWriter"),
                    "the page links outside the project: " + external);
        }
    }

    /**
     * The download button must not name a version: the site would then have to
     * be edited for every release, and it would be wrong the moment that was
     * forgotten. {@code /releases/latest} is the same address the in-app update
     * check reads.
     */
    @Test
    void theDownloadButtonGoesToTheLatestRelease() throws IOException {
        var html = read(INDEX);

        assertTrue(html.contains("class=\"button\""), "the page offers no download button");
        assertTrue(html.contains(RELEASES_LATEST),
                "no download button points at " + RELEASES_LATEST);
        assertFalse(Pattern.compile("\\b\\d+\\.\\d+\\.\\d+\\b").matcher(html).find(),
                "the page hardcodes a version; it must follow /releases/latest instead");
    }

    /**
     * The hero recordings have one source - {@code docs/media}, written by
     * {@code scripts/capture-hero-gif.sh} and pinned by {@code ReadmeMediaTest} -
     * so the deploy job copies them in rather than the site keeping its own pair.
     * A stale second copy would be invisible until someone looked at the
     * published page.
     */
    @Test
    void theWorkflowPublishesTheSameRecordingsTheReadmeUses() throws IOException {
        var workflow = read(WORKFLOW);

        assertTrue(workflow.contains("name: Pages"), "the workflow is not the Pages publisher");
        assertTrue(workflow.contains("actions/configure-pages@")
                        && workflow.contains("actions/upload-pages-artifact@")
                        && workflow.contains("actions/deploy-pages@"),
                "the deploy job is missing one of the three Pages actions");
        assertTrue(workflow.contains("pages: write") && workflow.contains("id-token: write"),
                "a Pages deployment needs its permissions declared");
        assertTrue(workflow.contains("path: site"), "the artifact is not the site directory");
        assertTrue(workflow.contains("concurrency:"),
                "two pushes must not deploy over each other");

        for (var language : List.of("en", "tr")) {
            var source = "docs/media/hero-" + language + ".gif";
            assertTrue(workflow.contains(source),
                    "the deploy job does not copy " + source + " into the site");
            assertTrue(Files.isRegularFile(Path.of(source)),
                    source + " is missing; run scripts/capture-hero-gif.sh first");
        }

        // The staged copies are build output, not sources: a run of the workflow
        // leaves site/assets behind, and committing it would create the second
        // copy this test exists to prevent.
        assertTrue(read(GITIGNORE).contains("site/assets/"),
                ".gitignore does not ignore the staged site/assets copy");

        // The icon has one source too. The page wears the application's own
        // icon, so a reader who opens it twice recognises it in the tab strip.
        assertTrue(workflow.contains("src/main/resources/icons/jtypewriter.svg"),
                "the deploy job does not copy the canonical application icon");
        assertTrue(read(INDEX).contains("assets/icon.svg"),
                "the page does not link the staged icon as its favicon");
    }

    /**
     * The {@code data-<language>} value inside the element carrying the given
     * class. Written against this one file rather than a parser: the page is a
     * static document the suite already reads as text, and the class names are
     * unique in it.
     */
    private static String attributeOf(String html, String className, String language) {
        var element = Pattern.compile("<p class=\"" + className + "\"(.*?)>", Pattern.DOTALL)
                .matcher(html);
        assertTrue(element.find(), "the page has no ." + className + " paragraph");
        var value = Pattern.compile("data-" + language + "=\"([^\"]*)\"", Pattern.DOTALL)
                .matcher(element.group(1));
        assertTrue(value.find(), "the ." + className + " paragraph has no " + language + " text");
        return value.group(1);
    }

    /** The bundle of one of the six UI languages, never the system default. */
    private static ResourceBundle bundle(String language) {
        return ResourceBundle.getBundle("messages",
                Locale.forLanguageTag(language),
                ResourceBundle.Control.getNoFallbackControl(
                        ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    /** Whatever the markup wrapped, the sentence is one run of text. */
    private static String squash(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static boolean isRemote(String target) {
        return target.startsWith("http://") || target.startsWith("https://")
                || target.startsWith("//");
    }

    private static List<String> externalLinks(String html) {
        var links = new ArrayList<String>();
        var matcher = Pattern.compile("href=\"(https?://[^\"]+)\"").matcher(html);
        while (matcher.find()) {
            links.add(matcher.group(1));
        }
        return links;
    }

    private static int count(String haystack, String needle) {
        var total = 0;
        var at = haystack.indexOf(needle);
        while (at >= 0) {
            total++;
            at = haystack.indexOf(needle, at + needle.length());
        }
        return total;
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), path + " does not exist; Surefire is not running "
                + "from the project root");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
