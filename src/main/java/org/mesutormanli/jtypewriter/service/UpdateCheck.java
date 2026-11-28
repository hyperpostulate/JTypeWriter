/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.service;

import java.util.Locale;
import java.util.Optional;

import org.mesutormanli.jtypewriter.VersionOrder;

/**
 * What an update check concluded, decided without a socket: given the address
 * GitHub resolved a request to and the version this build runs, does a newer
 * release exist?
 *
 * <p><strong>Why a redirect and not the API.</strong> The check reads
 * {@code https://github.com/<owner>/<repo>/releases/latest}, which GitHub answers
 * with a redirect to {@code .../releases/tag/v1.1.5}. The version is the last
 * path segment of the address the request ended at, so the whole check is one
 * HTTP request and a string split: no JSON parser, no dependency, no
 * {@code User-Agent} rule to satisfy and no API rate limit - the constraints the
 * REST endpoint would add for a number the redirect already carries.</p>
 *
 * <p><strong>Why the tag, not the release title.</strong> A title is editable
 * prose; a tag is what the build was cut from, and this repository already
 * refuses any tag that is not {@code v<pom version>}. Reading the tag means the
 * answer cannot be changed by editing a release's text after the fact.</p>
 *
 * <p><strong>Unrecognised tags are refused.</strong> Only a plain dotted numeric
 * tag with an optional {@code v} (see {@link VersionOrder#isReleaseVersion})
 * counts, so a hypothetical {@code v2.0.0-beta} is not read as "2.0.0 is out".
 * The project publishes no pre-release tags, so a suffixed tag is not evidence
 * of anything a user should be told about.</p>
 *
 * <p>Pure and headless: the network half lives in {@link UpdateService}, which
 * feeds this class the address it ended up at. That split is what lets the
 * decision be tested without a server, the same way {@code ExitDisposition} and
 * {@code AutosaveHealth} keep their rules out of the classes that run them.</p>
 */
public final class UpdateCheck {

    /** The three things a check can conclude. */
    public enum Status {
        /** A release newer than this build exists. */
        NEWER,
        /** This build is the newest release. */
        UP_TO_DATE,
        /** The check did not produce an answer (offline, blocked, unreadable). */
        FAILED
    }

    /**
     * The outcome the dialogs and the menu act on.
     *
     * @param status        what was concluded
     * @param latestVersion the release's version without its {@code v} prefix;
     *                      empty only when the check failed
     * @param pageUrl       the release page the user is sent to; empty only
     *                      when the check failed
     */
    public record Result(Status status, String latestVersion, String pageUrl) {

        public Result {
            latestVersion = latestVersion == null ? "" : latestVersion;
            pageUrl = pageUrl == null ? "" : pageUrl;
        }

        static Result newer(String version, String pageUrl) {
            return new Result(Status.NEWER, version, pageUrl);
        }

        static Result upToDate(String version, String pageUrl) {
            return new Result(Status.UP_TO_DATE, version, pageUrl);
        }

        /** No answer: the caller only needs to know the check did not work. */
        public static Result failed() {
            return new Result(Status.FAILED, "", "");
        }
    }

    private UpdateCheck() {
    }

    /**
     * The version named by a GitHub release address, or empty when the address
     * is not one.
     *
     * <p>Accepts the absolute address GitHub redirects to
     * ({@code https://github.com/hyperpostulate/JTypeWriter/releases/tag/v1.1.5}),
     * tolerates a query string, a fragment and a trailing slash, and strips the
     * {@code v}. A {@code /releases/latest} address (the redirect not followed)
     * or a tag that is not a release version yields empty rather than a guess.</p>
     */
    public static Optional<String> versionOf(String pageUrl) {
        if (pageUrl == null || pageUrl.isBlank()) {
            return Optional.empty();
        }
        var path = pageUrl.strip();
        var cut = indexOfAny(path, '?', '#');
        if (cut >= 0) {
            path = path.substring(0, cut);
        }
        var marker = path.toLowerCase(Locale.ROOT).lastIndexOf("/releases/tag/");
        if (marker < 0) {
            return Optional.empty();
        }
        var tag = path.substring(marker + "/releases/tag/".length());
        if (tag.endsWith("/")) {
            tag = tag.substring(0, tag.length() - 1);
        }
        if (tag.isEmpty() || tag.contains("/") || !VersionOrder.isReleaseVersion(tag)) {
            return Optional.empty();
        }
        return Optional.of(tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag);
    }

    /**
     * Decides what to tell the user, or empty when the address did not name a
     * release at all - the caller turns that into a failure, because "I could
     * not tell" and "you are up to date" must never be the same message.
     */
    public static Optional<Result> evaluate(String pageUrl, String currentVersion) {
        return versionOf(pageUrl).map(latest -> VersionOrder.isNewer(latest, currentVersion)
                ? Result.newer(latest, pageUrl)
                : Result.upToDate(latest, pageUrl));
    }

    private static int indexOfAny(String text, char first, char second) {
        var a = text.indexOf(first);
        var b = text.indexOf(second);
        if (a < 0) {
            return b;
        }
        if (b < 0) {
            return a;
        }
        return Math.min(a, b);
    }
}
