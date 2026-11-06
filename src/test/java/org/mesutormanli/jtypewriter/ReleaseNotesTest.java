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

    /**
     * Two things a reader needs before running an unknown binary: that this is
     * an early build, and how much they are about to download. Both were
     * missing from the note, and the second one cannot be written by hand
     * honestly - the Windows assets are produced on a Windows runner and have
     * never been measurable from the Linux machine the template is edited on.
     *
     * <p>The sizes are therefore rendered from the assembled files themselves,
     * which also keeps them from drifting: a number typed into the template is
     * a second source of truth that is wrong one release later. This test pins
     * both halves of that arrangement - the marker in both languages, the
     * placeholder in both halves, and the measurement in the release job.</p>
     */
    @Test
    void theNoteMarksEarlyAccessAndMeasuresTheDownloadSizes() throws IOException {
        var template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        var split = template.indexOf("## Türkçe");
        assertTrue(split > 0, TEMPLATE + " has no '## Türkçe' section");
        var english = template.substring(0, split);
        var turkish = template.substring(split);

        assertTrue(english.contains("Early access"),
                "the English half does not mark the release as early access");
        assertTrue(turkish.contains("Erken erişim"),
                "the Turkish half does not mark the release as early access");

        var first = template.indexOf("@ASSET_SIZES@");
        assertTrue(first >= 0, TEMPLATE + " has no @ASSET_SIZES@ placeholder; a hand-written "
                + "size is wrong for the platform it was not measured on");
        assertTrue(template.indexOf("@ASSET_SIZES@", first + 1) >= 0,
                "only one half carries the size list, so one language hides the download size");

        var release = Files.readString(RELEASE_WORKFLOW, StandardCharsets.UTF_8);
        assertTrue(release.contains("@ASSET_SIZES@"),
                "release.yml never fills @ASSET_SIZES@, so the note would ship the placeholder");
        assertTrue(release.contains("stat -c %s"),
                "release.yml does not measure the published files, so the sizes are not measured");
        assertTrue(release.contains("1048576"),
                "release.yml does not convert bytes to MiB; a raw byte count is not a usable size");
        assertTrue(release.indexOf("stat -c %s") < release.indexOf("unsubstituted placeholder"),
                "the sizes must be rendered before the leftover-placeholder check, or every "
                        + "release fails on a placeholder the job itself was supposed to fill");
    }

    /**
     * The note called itself "the first published build". That was true for the
     * very first release and false for every one after it, yet the template is
     * the body of each release, so the sentence shipped unchanged and told
     * readers of a later version that the project had just started. A note that
     * describes the release series instead of a single one cannot go stale this
     * way.
     */
    @Test
    void theNoteDoesNotClaimToBeTheFirstBuild() throws IOException {
        var template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        // Folded so the assertion does not depend on where a sentence starts.
        var folded = template.toLowerCase(java.util.Locale.ROOT).replace('İ', 'i');
        for (var phrase : List.of("first published build", "first release",
                "yayımlanan ilk derleme", "ilk sürüm")) {
            assertFalse(folded.contains(phrase),
                    TEMPLATE + " still claims to be the first release ('" + phrase + "');"
                            + " it is the body of every release, so it must describe the series");
        }
    }

    /**
     * The note tells the reader this is an early build; the repository page
     * derives its own label from the release metadata. Publishing without
     * {@code --prerelease} made GitHub call the same release "Latest", so a
     * visitor saw a stable release and a body saying "pre-release" - the two
     * claims lived in different places and could disagree.
     *
     * <p>The flag belongs in the publish step itself, next to
     * {@code --verify-tag}: dropping the mark is then a visible edit with a
     * roadmap card behind it (W4-05), never something that happens by
     * omission.</p>
     */
    @Test
    void theReleaseIsPublishedWithThePrereleaseFlagTheNoteClaims() throws IOException {
        var release = Files.readString(RELEASE_WORKFLOW, StandardCharsets.UTF_8);
        var publish = stepContaining(release, release.indexOf("gh release create"));

        assertTrue(publish.contains("--prerelease"),
                "the publish step does not mark the release as a pre-release, so GitHub "
                        + "labels an early-access build as Latest while its own note says otherwise:\n"
                        + publish);
        assertTrue(publish.contains("--verify-tag"),
                "the publish step dropped --verify-tag, so a local-only tag could get a Release:\n"
                        + publish);
    }

    /**
     * A release pipeline nobody has ever run is a hypothesis, and a tag is the
     * least convenient moment to find out that an asset name, the note
     * rendering or the two-OS merge was wrong. The dry run exists to convert
     * that hypothesis into evidence: it must be dispatchable by hand, and the
     * publication step must be unreachable without a tag.
     */
    @Test
    void theReleaseWorkflowCanDryRunWithoutPublishing() throws IOException {
        var release = Files.readString(RELEASE_WORKFLOW, StandardCharsets.UTF_8);

        assertTrue(release.contains("workflow_dispatch:"),
                "release.yml cannot be run by hand, so the pipeline can only be tried by tagging");
        assertTrue(release.contains("dry_run:"),
                "release.yml has no dry_run input, so a manual run has nothing to say about publishing");

        var publish = stepContaining(release, release.indexOf("gh release create"));
        assertTrue(publish.contains("if:") && publish.contains("publish == 'true'"),
                "the publish step is not guarded, so a manual run could create a Release:\n" + publish);

        var upload = stepContaining(release, release.indexOf("JTypeWriter-release-dry-run-"));
        assertTrue(upload.contains("publish != 'true'"),
                "the dry-run bundle is not limited to a dry run:\n" + upload);
        assertTrue(upload.contains("retention-days:"),
                "the dry-run bundle is uploaded without a retention limit:\n" + upload);
    }

    /**
     * A tag push is the only trigger that makes the provenance claim true, so a
     * manual run that asks to publish must be refused rather than quietly
     * published from a branch tip. The refusal has to be in prepare - before
     * two OS builds spend ten minutes on an artifact that could never be
     * uploaded.
     */
    @Test
    void aManualRunCannotPublish() throws IOException {
        var release = Files.readString(RELEASE_WORKFLOW, StandardCharsets.UTF_8);

        assertTrue(release.contains("a manual run cannot publish"),
                "release.yml does not refuse a manual run that asked to publish");
        assertTrue(release.contains("tag '$GITHUB_REF_NAME' does not match pom.xml version"),
                "release.yml no longer rejects a tag that disagrees with pom.xml");
    }

    /**
     * The expected-asset list is the assertion that catches a silently missing
     * Windows file. It must stay exactly the five shippable assets - the
     * application image is not one of them, it is archived into the portable
     * {@code .tar.gz} - because a sixth entry nobody produces would make every
     * release red, and a dropped entry would stop the check from noticing.
     */
    @Test
    void theReleaseWorkflowAssertsTheFiveAssets() throws IOException {
        var release = Files.readString(RELEASE_WORKFLOW, StandardCharsets.UTF_8);
        var start = release.indexOf("expected=(");
        var end = release.indexOf(")", start);

        assertTrue(start >= 0 && end > start, "release.yml has no expected-asset list");
        var entries = release.substring(start + "expected=(".length(), end).lines()
                .map(String::strip)
                .filter(line -> line.startsWith("\""))
                .toList();

        assertTrue(entries.size() == 5,
                "the expected-asset list has " + entries.size() + " entries, not 5: " + entries);
        for (var suffix : List.of("-linux-x64.deb", "-linux-x64.tar.gz",
                "-windows-x64.msi", "-windows-x64.tar.gz", "-source.tar.gz")) {
            assertTrue(entries.stream().anyMatch(entry -> entry.contains(suffix)),
                    "the expected-asset list is missing the " + suffix + " asset: " + entries);
        }
    }

    /** The step block around {@code offset}: from its own {@code - name:} onwards. */
    private static String stepContaining(String workflow, int offset) {
        assertTrue(offset >= 0, "the workflow no longer contains the step this test looks for");
        var start = workflow.lastIndexOf("- name:", offset);
        var end = workflow.indexOf("\n      - ", offset);
        return end < 0 ? workflow.substring(start) : workflow.substring(start, end);
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
