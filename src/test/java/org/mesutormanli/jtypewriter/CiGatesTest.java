/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two evidence gates - {@code scripts/verify-artifacts.sh} and
 * {@code scripts/generate-icons.py --check} - are only worth having if they run
 * where nobody remembers to run them.
 *
 * <p>Both scripts existed for a whole release cycle while being invoked by hand
 * only, and that is the failure mode this class guards: a guarantee that lives
 * in a script nobody calls is one broken commit away from being a comment. The
 * workflow is the only input these checks need - no network, no toolkit, no
 * writes - so they run with the rest of the headless suite.</p>
 *
 * <p>Each gate also has to run where it can honestly run. The artifact
 * comparison unpacks a {@code .deb} with {@code dpkg-deb}, which exists on the
 * Linux runner only, so it is pinned there.</p>
 *
 * <p>The icon gate is the mirror image of that decision, and it is the one that
 * has already gone wrong once. It used to re-rasterise the SVG in CI and demand
 * byte-identical output, which cannot hold: ImageMagick encodes the same pixels
 * to different bytes on different builds, so the Windows leg failed on assets
 * that were perfectly in date. {@code --check} now verifies hashes recorded at
 * generation time, which needs no ImageMagick and no display, so it runs on both
 * legs - and these tests hold it there: moving it back behind a
 * {@code runner.os} guard, or after the build, fails here rather than in a
 * release.</p>
 */
class CiGatesTest {

    private static final Path MAVEN_WORKFLOW =
            Path.of(".github", "workflows", "maven.yml");

    private static String workflow() throws IOException {
        return Files.readString(MAVEN_WORKFLOW, StandardCharsets.UTF_8);
    }

    /**
     * A committed binary cannot be checked by the compiler: only the gate knows
     * whether a PNG still renders from {@code jtypewriter.svg}.
     */
    @Test
    void theWorkflowRunsTheIconCheck() throws IOException {
        assertTrue(workflow().contains("generate-icons.py --check"),
                MAVEN_WORKFLOW + " never runs scripts/generate-icons.py --check");
    }

    /**
     * A stale icon set is the cheapest possible failure to fix and the most
     * expensive to notice, so the gate runs before the build rather than after
     * it: red in a minute instead of red in ten.
     */
    @Test
    void theIconCheckRunsBeforeTheBuild() throws IOException {
        var workflow = workflow();
        var check = workflow.indexOf("generate-icons.py --check");
        var build = workflow.indexOf("mvn -B package");

        assertTrue(check >= 0, MAVEN_WORKFLOW + " never runs scripts/generate-icons.py --check");
        assertTrue(build >= 0, MAVEN_WORKFLOW + " never runs mvn -B package");
        assertTrue(check < build, "the icon gate runs after the build, so it fails late");
    }

    /**
     * The icon check verifies hashes, so it needs neither ImageMagick nor a
     * display and must run on every leg of the matrix. Restricting it to one OS
     * would leave the other leg's package built from icons nobody verified -
     * and the reason the restriction existed in the first place, a rasteriser
     * that renders differently per runner, is exactly what the hash comparison
     * removed.
     */
    @Test
    void theIconCheckRunsOnEveryLeg() throws IOException {
        var workflow = workflow();
        var check = workflow.indexOf("generate-icons.py --check");
        assertTrue(check >= 0, MAVEN_WORKFLOW + " never runs scripts/generate-icons.py --check");

        var step = stepContaining(workflow, check);
        assertFalse(step.contains("if:"),
                "the icon gate is restricted to one runner, but it no longer needs one:\n" + step);
        assertFalse(step.contains("--render"),
                "the icon gate re-rasterises in CI, which compares runners, not artwork:\n" + step);
    }

    /**
     * {@code package.sh} only asserts presence; this gate asserts the bytes.
     * It has something to compare only after the image and the installer exist,
     * so it must come after the packaging step - before it, the release
     * artifacts under test are a previous run's.
     */
    @Test
    void theWorkflowRunsTheArtifactCheckAfterPackaging() throws IOException {
        var workflow = workflow();
        var packaging = workflow.indexOf("scripts/package.sh --skip-build --installer");
        var verify = workflow.indexOf("verify-artifacts.sh --skip-build");

        assertTrue(verify >= 0, MAVEN_WORKFLOW + " never runs scripts/verify-artifacts.sh");
        assertTrue(packaging >= 0, MAVEN_WORKFLOW + " never runs scripts/package.sh");
        assertTrue(packaging < verify,
                "the artifact gate runs before packaging, so it compares nothing built by this run");
    }

    /**
     * The {@code .deb} is the only installer the script unpacks, and
     * {@code dpkg-deb} exists on the Linux runner only: without the guard the
     * Windows leg would fail on a check that could not have told it anything.
     */
    @Test
    void theArtifactGateIsLimitedToTheLinuxLeg() throws IOException {
        var workflow = workflow();
        var verify = workflow.indexOf("verify-artifacts.sh --skip-build");
        assertTrue(verify >= 0, MAVEN_WORKFLOW + " never runs scripts/verify-artifacts.sh");

        assertTrue(stepContaining(workflow, verify).contains("if: runner.os == 'Linux'"),
                "the artifact gate is not limited to the Linux leg");
    }

    /** The step block around {@code offset}: from its own {@code - name:} onwards. */
    private static String stepContaining(String workflow, int offset) {
        return workflow.substring(workflow.lastIndexOf("- name:", offset), offset);
    }
}
