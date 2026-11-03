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
 * <p>Each gate is also pinned to the leg that can honestly run it. The artifact
 * comparison unpacks a {@code .deb} with {@code dpkg-deb}, which only exists on
 * Linux. The icon comparison on the other hand needs the rasteriser the
 * committed PNGs were rendered with - ImageMagick 7's internal MSVG renderer,
 * which the Windows runner image ships and the ubuntu image does not - so
 * running it on the Linux leg would report a false failure rather than a real
 * one.</p>
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
