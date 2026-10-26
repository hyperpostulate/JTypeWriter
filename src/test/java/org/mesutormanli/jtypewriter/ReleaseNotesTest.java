/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the release-publication contract: no workflow may upload an artifact
 * without a short retention, and the release body must stay the bilingual
 * document the project promises.
 *
 * <p>Both properties are invisible until a release happens, which is exactly
 * when there is no second chance: an artifact left on the default 90-day
 * retention bills storage on every {@code main} push, and a release note that
 * lost its Turkish half tells a Turkish user the project does not speak their
 * language. The checks live here because the repository is the only input they
 * need - no network, no JavaFX toolkit, no writes.</p>
 */
class ReleaseNotesTest {

    private static final Path WORKFLOWS = Path.of(".github", "workflows");
    private static final Path MAVEN_WORKFLOW = WORKFLOWS.resolve("maven.yml");
    private static final Path RELEASE_WORKFLOW = WORKFLOWS.resolve("release.yml");
    private static final Path TEMPLATE = Path.of("docs", "release-notes-template.md");

    /**
     * GitHub's default retention is 90 days. The budget here is "long enough
     * for the in-flight consumer, short enough to be free", which no upload
     * needs more than a week of.
     */
    private static final int MAX_RETENTION_DAYS = 7;

    @Test
    void everyArtifactUploadHasAShortRetention() throws IOException {
        var failures = new ArrayList<String>();
        for (var workflow : List.of(MAVEN_WORKFLOW, RELEASE_WORKFLOW)) {
            var lines = lines(workflow);
            for (var i = 0; i < lines.size(); i++) {
                if (!lines.get(i).contains("uses: actions/upload-artifact@")) {
                    continue;
                }
                var retention = retentionAfter(lines, i);
                if (retention == null) {
                    failures.add(workflow + ":" + (i + 1)
                            + " uploads an artifact without retention-days");
                } else if (retention < 1 || retention > MAX_RETENTION_DAYS) {
                    failures.add(workflow + ":" + (i + 1) + " keeps artifacts for " + retention
                            + " days, over the " + MAX_RETENTION_DAYS + "-day budget");
                }
            }
        }
        assertTrue(failures.isEmpty(), String.join("\n", failures));
    }

    @Test
    void theReleaseBodyComesFromTheCheckedInTemplate() throws IOException {
        var release = Files.readString(RELEASE_WORKFLOW, StandardCharsets.UTF_8);
        assertTrue(release.contains("docs/release-notes-template.md"),
                "release.yml does not render docs/release-notes-template.md");
        assertTrue(release.contains("@VERSION@") && release.contains("@TAG@"),
                "release.yml does not substitute the template placeholders");
        assertTrue(release.contains("unsubstituted placeholder"),
                "release.yml must fail when a placeholder survives the substitution");
        assertTrue(release.contains("missing the section"),
                "release.yml must fail when one language half of the note is gone");
    }

    @Test
    void theTemplateIsBilingualAndFreeOfLiteralVersions() throws IOException {
        var template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        assertFalse(template.isBlank(), TEMPLATE + " is empty");
        for (var heading : List.of("## English", "## Türkçe")) {
            assertTrue(template.contains("\n" + heading + "\n"),
                    TEMPLATE + " has no '" + heading + "' section");
        }
        assertTrue(template.indexOf("## English") < template.indexOf("## Türkçe"),
                TEMPLATE + " must read English first, Turkish second");

        for (var placeholder : List.of("@VERSION@", "@TAG@")) {
            assertTrue(template.contains(placeholder),
                    TEMPLATE + " does not use " + placeholder + "; the release job fills it in");
        }
        // The version has one source of truth (pom.xml); a literal version here
        // would be a second one, and it would drift silently.
        assertFalse(template.matches("(?s).*\\b\\d+\\.\\d+\\.\\d+\\b.*"),
                TEMPLATE + " hardcodes a version string instead of @VERSION@");
    }

    @Test
    void bothHalvesCarryTheSameVerificationCommands() throws IOException {
        var template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        var split = template.indexOf("## Türkçe");
        assertTrue(split > 0, TEMPLATE + " has no '## Türkçe' section, so the two halves "
                + "cannot be compared");
        var english = template.substring(0, split);
        var turkish = template.substring(split);
        for (var command : List.of("sha256sum -c SHA256SUMS", "certutil -hashfile")) {
            assertTrue(english.contains(command), "English half is missing: " + command);
            assertTrue(turkish.contains(command), "Turkish half is missing: " + command
                    + " - the two halves must offer the same steps");
        }
    }

    private static Integer retentionAfter(List<String> lines, int uploadLine) {
        for (var i = uploadLine + 1; i < lines.size(); i++) {
            var line = lines.get(i);
            var trimmed = line.strip();
            if (trimmed.startsWith("- name:") || indentOf(line) <= 2) {
                // A new step, or a new job: the step that owns this upload ended.
                return null;
            }
            if (trimmed.startsWith("retention-days:")) {
                return Integer.valueOf(trimmed.substring("retention-days:".length()).strip());
            }
        }
        return null;
    }

    private static int indentOf(String line) {
        var indent = 0;
        while (indent < line.length() && line.charAt(indent) == ' ') {
            indent++;
        }
        return indent;
    }

    private static List<String> lines(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), path + " does not exist; Surefire is not running "
                + "from the project root");
        return List.of(Files.readString(path, StandardCharsets.UTF_8).split("\n", -1));
    }
}
