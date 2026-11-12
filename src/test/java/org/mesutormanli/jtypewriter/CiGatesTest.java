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
 * <p>Since P3.2.2 the gates live in one reusable workflow,
 * {@code verify-build.yml}, and both entry points - the branch build and the
 * release pipeline - call it. That is what stops a tag from publishing an
 * artifact that only passed the cheap half of the pipeline, so the tests below
 * pin two things: the gates are in the shared workflow, and both callers
 * actually invoke it. A gate moved out of the shared file still fails here.
 * </p>
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

    private static final Path WORKFLOWS = Path.of(".github", "workflows");

    private static final Path MAVEN_WORKFLOW = WORKFLOWS.resolve("maven.yml");

    private static final Path RELEASE_WORKFLOW = WORKFLOWS.resolve("release.yml");

    /** The one definition of the gate battery; both entry points call it. */
    private static final Path GATE_WORKFLOW = WORKFLOWS.resolve("verify-build.yml");

    private static final String GATE_WORKFLOW_REF =
            "uses: ./.github/workflows/verify-build.yml";

    private static final Path PACKAGE_SCRIPT = Path.of("scripts", "package.sh");

    private static String workflow() throws IOException {
        return Files.readString(GATE_WORKFLOW, StandardCharsets.UTF_8);
    }

    private static String entrypoint(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * A committed binary cannot be checked by the compiler: only the gate knows
     * whether a PNG still renders from {@code jtypewriter.svg}.
     */
    @Test
    void theWorkflowRunsTheIconCheck() throws IOException {
        assertTrue(workflow().contains("generate-icons.py --check"),
                GATE_WORKFLOW + " never runs scripts/generate-icons.py --check");
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

        assertTrue(check >= 0, GATE_WORKFLOW + " never runs scripts/generate-icons.py --check");
        assertTrue(build >= 0, GATE_WORKFLOW + " never runs mvn -B package");
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
        assertTrue(check >= 0, GATE_WORKFLOW + " never runs scripts/generate-icons.py --check");

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

        assertTrue(verify >= 0, GATE_WORKFLOW + " never runs scripts/verify-artifacts.sh");
        assertTrue(packaging >= 0, GATE_WORKFLOW + " never runs scripts/package.sh");
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
        assertTrue(verify >= 0, GATE_WORKFLOW + " never runs scripts/verify-artifacts.sh");

        assertTrue(stepContaining(workflow, verify).contains("if: runner.os == 'Linux'"),
                "the artifact gate is not limited to the Linux leg");
    }

    /**
     * The artifact comparison proves the bytes; nothing in it proves the
     * artifact runs. A bundled runtime that cannot find a library or a
     * {@code .deb} whose maintainer script aborts on a headless runner passes
     * every other gate and fails on the user's first double-click, which is
     * exactly the gap {@code scripts/smoke-appimage.sh} closes.
     *
     * <p>The job must consume what the verify job produced rather than package
     * again: a second packaging run would prove that packaging is reproducible
     * twice on the same runner, not that the artifact a user downloads works.
     * The {@code --skip-build} flag is the checkable form of that intent.</p>
     */
    @Test
    void theWorkflowSmokeTestsThePackagedApplication() throws IOException {
        var workflow = workflow();
        var job = workflow.indexOf("\n  smoke:");

        assertTrue(job >= 0, GATE_WORKFLOW + " has no smoke job, so no artifact is ever started");
        var block = jobBlock(workflow, job);
        assertTrue(block.contains("needs: verify"),
                "the smoke job does not depend on the verify job, so it may run before an artifact exists");
        assertTrue(block.contains("smoke-appimage.sh --skip-build"),
                "the smoke job does not run scripts/smoke-appimage.sh --skip-build");
        assertTrue(block.contains("download-artifact"),
                "the smoke job repackages instead of consuming the artifact the verify job uploaded");
        assertTrue(block.contains("--open-sample"),
                "the smoke job never names a document on the command line, so nothing in CI"
                        + " proves that a file association or a typed argument opens the file");
    }

    /**
     * The shared workflow is only shared if both pipelines call it. P3.2.2's
     * finding was that the tag pipeline ran {@code mvn package} and
     * {@code package.sh} while the branch pipeline ran five more gates, so the
     * difference was invisible until a release published it. These assertions
     * hold both entry points to the one definition.
     */
    @Test
    void bothEntrypointsRunTheSharedGateWorkflow() throws IOException {
        for (var entrypoint : java.util.List.of(MAVEN_WORKFLOW, RELEASE_WORKFLOW)) {
            assertTrue(entrypoint(entrypoint).contains(GATE_WORKFLOW_REF),
                    entrypoint + " no longer calls " + GATE_WORKFLOW_REF
                            + ", so its build is a separate copy of the gates");
        }
    }

    /**
     * A release job that still ran Maven or package.sh itself could look like it
     * used the shared gates while bypassing them: a job is either a
     * {@code uses:} call or a step list, and the return to a step list is how
     * the two pipelines diverged in the first place. The version input is
     * pinned beside it because without it the shared workflow verifies but
     * stages nothing, and the publish job has no dist/ to assemble.
     */
    @Test
    void theReleaseBuildIsTheSharedGateWorkflowAndNothingElse() throws IOException {
        var release = entrypoint(RELEASE_WORKFLOW);
        var build = jobBlock(release, release.indexOf("\n  build:"));

        assertTrue(build.contains(GATE_WORKFLOW_REF),
                RELEASE_WORKFLOW + " does not build through " + GATE_WORKFLOW_REF);
        assertTrue(build.contains("version: ${{ needs.prepare.outputs.version }}"),
                "the release call does not pass the version, so no dist/ assets are staged");
        assertFalse(build.contains("mvn -B package"),
                "the release build still runs Maven itself, so it can skip the shared gates");
        assertFalse(build.contains("package.sh"),
                "the release build still packages itself, so it can skip the shared gates");
    }

    /**
     * The publish job must consume the normalised assets only. The shared
     * workflow also uploads each leg's raw {@code releases/} tree for its own
     * smoke job, so a broad pattern would merge installers and application
     * images into {@code dist/} beside the normalised names.
     */
    @Test
    void theReleaseDownloadsOnlyTheNormalisedAssets() throws IOException {
        var release = entrypoint(RELEASE_WORKFLOW);
        var download = stepBlock(release, release.indexOf("Download staged assets"));

        assertTrue(download.contains("pattern: JTypeWriter-dist-*"),
                "the publish job does not download only the normalised dist artifacts:\n" + download);
        assertFalse(download.contains("pattern: JTypeWriter-*"),
                "the publish job merges the raw releases/ artifacts into dist/:\n" + download);
    }

    /**
     * The document-opening smoke is only as real as the flag it names. The
     * script has to accept {@code --open-sample} and launch the packaged
     * launcher with a file argument; a workflow step that passes a flag the
     * script rejects would fail the whole smoke run, which is loud but does not
     * explain the contract.
     */
    @Test
    void theSmokeScriptAcceptsTheOpenSampleFlag() throws IOException {
        var script = Files.readString(Path.of("scripts", "smoke-appimage.sh"),
                StandardCharsets.UTF_8);

        assertTrue(script.contains("--open-sample)"),
                "scripts/smoke-appimage.sh does not accept --open-sample");
        assertTrue(script.contains("cli-sample.txt"),
                "scripts/smoke-appimage.sh never creates a sample document to open");
    }

    /**
     * {@code .deb} maintainer scripts generated by {@code jpackage} call
     * {@code xdg-desktop-menu} unguarded under {@code set -e}, so installing on
     * a machine without a writable XDG menu directory - a CI runner, a server,
     * a container - aborts the dpkg transaction and leaves the package
     * half-configured. That is a packaging defect, not a test-environment
     * quirk, so it is repaired where the package is rebuilt rather than
     * papered over by running the smoke install differently.
     */
    @Test
    void packageScriptMakesTheDebMenuRegistrationBestEffort() throws IOException {
        var packageScript = Files.readString(PACKAGE_SCRIPT, StandardCharsets.UTF_8);

        assertTrue(packageScript.contains("relax_desktop_menu_registration"),
                PACKAGE_SCRIPT + " no longer relaxes the deb's menu registration, so a headless"
                        + " install aborts the dpkg transaction");
        assertTrue(packageScript.contains("xdg-desktop-menu"),
                PACKAGE_SCRIPT + " does not mention the maintainer-script call it has to relax");
    }

    /**
     * JavaFX must reach the packaged application as named modules. Two of the
     * three ways to get this wrong are silent: putting the classes back on the
     * classpath logs a warning and then works, and a runtime built from the host
     * JDK starts the application fine while shipping 74 MiB of tools nobody
     * uses. Both are asserted here because both were the shipped behaviour.
     *
     * <p>The module path is written with jpackage's {@code $ROOTDIR} macro
     * rather than {@code $APPDIR}: {@code $APPDIR} is the launcher's own
     * directory ({@code lib/app}), so a path built from it resolves to
     * {@code lib/app/lib/javafx} and the application boots to "Module
     * javafx.controls not found" - a failure that only shows up when the image
     * is started, not when it is written.</p>
     */
    @Test
    void packageScriptShipsJavaFxAsModules() throws IOException {
        var packageScript = Files.readString(PACKAGE_SCRIPT, StandardCharsets.UTF_8);

        // The option itself, not the prose: the comments name $ROOTDIR to
        // explain why it is wrong, so a whole-file search cannot tell the bug
        // from its explanation. $ROOTDIR is empty in an installed package (the
        // launcher only sets imageRoot for a plain app image), which turned
        // this option into /lib/javafx and killed the installed launcher with
        // "Module javafx.controls not found".
        var modulePathOption = packageScript.lines()
                .filter(line -> line.contains("--module-path="))
                .findFirst()
                .orElseThrow(() -> new AssertionError(PACKAGE_SCRIPT
                        + " no longer puts JavaFX on the module path, so the classes are loaded"
                        + " from the unnamed module again"));
        assertFalse(modulePathOption.contains("$ROOTDIR"),
                PACKAGE_SCRIPT + " builds the module path from $ROOTDIR, which does not resolve"
                        + " when the launcher runs from an installed package: " + modulePathOption);
        assertTrue(modulePathOption.contains("$JFX_MODULE_PATH"),
                PACKAGE_SCRIPT + " does not put the per-platform module path on the command line: "
                        + modulePathOption);
        assertTrue(packageScript.contains("--add-modules=javafx.controls"),
                PACKAGE_SCRIPT + " resolves no JavaFX modules, so the module path is decorative");
        // Not jpackage's --app-content: on Windows it produced an image whose
        // lib/javafx held the jars but not the legal/ subdirectory, so the
        // placement has to be the script's own and it has to copy both.
        assertFalse(packageScript.contains("--app-content \"$"),
                PACKAGE_SCRIPT + " hands the JavaFX modules to --app-content again, whose"
                        + " recursive copy is platform-dependent (Windows dropped legal/)");
        assertTrue(packageScript.contains("JFX_IMAGE_DIR"),
                PACKAGE_SCRIPT + " does not write lib/javafx itself, so the module path points"
                        + " at a directory nothing guarantees");
        assertTrue(packageScript.contains("$JFX_IMAGE_DIR/$LEGAL_DIR/"),
                PACKAGE_SCRIPT + " does not carry the JavaFX license next to the modules in the"
                        + " image, so the installer ships GPL code without its text");
        assertTrue(packageScript.contains("--enable-native-access=ALL-UNNAMED,javafx.graphics"),
                PACKAGE_SCRIPT + " does not grant javafx.graphics native access, so loading"
                        + " libglassgtk3.so warns on every launch");
    }

    /**
     * The runtime is built from a module list, not from the host JDK. The list
     * is the measured one ({@code jdeps --print-module-deps}); the entries that
     * matter are the ones that are easy to get wrong: {@code java.scripting} is
     * a hard requirement of {@code javafx.fxml} and its absence is a boot
     * failure, while {@code jdk.compiler} (10.4 MiB of compiler symbol tables),
     * {@code jdk.jshell} and {@code jdk.jconsole} are the weight jlink exists to
     * remove.
     */
    @Test
    void packageScriptBuildsTheRuntimeFromAModuleList() throws IOException {
        var packageScript = Files.readString(PACKAGE_SCRIPT, StandardCharsets.UTF_8);

        assertTrue(packageScript.contains("jlink"),
                PACKAGE_SCRIPT + " no longer runs jlink, so the image carries the host JDK");
        assertTrue(packageScript.contains("--runtime-image"),
                PACKAGE_SCRIPT + " does not hand the jlink runtime to jpackage");

        // The list itself, not the comments around it: the prose names the
        // modules that must stay out, so a search over the whole file cannot
        // tell an exclusion from an exclusion's explanation.
        var listLine = packageScript.lines()
                .filter(line -> line.stripLeading().startsWith("RUNTIME_MODULES="))
                .findFirst()
                .orElseThrow(() -> new AssertionError(PACKAGE_SCRIPT + " has no RUNTIME_MODULES list"));
        assertTrue(listLine.contains("java.base"),
                "the runtime list does not even name java.base: " + listLine);
        assertTrue(listLine.contains("java.desktop"),
                "the runtime list omits java.desktop, which every JavaFX window needs: " + listLine);
        assertTrue(listLine.contains("java.scripting"),
                PACKAGE_SCRIPT + " omits java.scripting, which javafx.fxml requires - the"
                        + " application would boot to 'Module java.scripting not found': " + listLine);
        for (var dead : java.util.List.of("jdk.compiler", "jdk.jshell", "jdk.jconsole")) {
            assertFalse(listLine.contains(dead),
                    PACKAGE_SCRIPT + " puts " + dead + " back on the runtime module path: " + listLine);
        }
    }

    /**
     * The artifact gate is where the layout is asserted. Its comparison loop
     * would happily pass an image whose runtime is the host JDK again, and the
     * smoke run would happily pass one whose JavaFX came from the classpath -
     * both only differ in size and in a log line.
     */
    @Test
    void theArtifactGateAssertsTheImageLayout() throws IOException {
        var script = Files.readString(Path.of("scripts", "verify-artifacts.sh"),
                StandardCharsets.UTF_8);

        assertTrue(script.contains("/release\""),
                "scripts/verify-artifacts.sh never reads the image's runtime module list");
        assertTrue(script.contains("IMAGE_RUNTIME_DIR"),
                "scripts/verify-artifacts.sh hard-codes lib/runtime, which is the Linux layout"
                        + " only - Windows puts the runtime at runtime/ and the check would fail"
                        + " there before it compared anything");
        assertTrue(script.contains("IMAGE_APP_DIR"),
                "scripts/verify-artifacts.sh hard-codes lib/app, which is the Linux layout only");
        assertTrue(script.contains("jdk.compiler"),
                "scripts/verify-artifacts.sh does not fail when the jlink gain is lost");
        assertTrue(script.contains("lib/javafx"),
                "scripts/verify-artifacts.sh does not compare the JavaFX modules in the image");
    }

    /**
     * jpackage lays an app image out differently per platform: Linux keeps the
     * application and the runtime under {@code lib/}, Windows puts them at the
     * image root as {@code app/} and {@code runtime/}. Asserting one spelling
     * passes everywhere the script is written and fails on the other leg - and
     * a Windows CI run is where that surfaced, after the Linux leg was green.
     * Both layouts therefore have to be spelled out, and the JavaFX directory
     * has to be the one the script writes itself, not whatever jpackage chose.
     */
    @Test
    void packageScriptKnowsBothAppImageLayouts() throws IOException {
        var packageScript = Files.readString(PACKAGE_SCRIPT, StandardCharsets.UTF_8);

        for (var spelling : java.util.List.of(
                "IMAGE_APP_DIR=\"lib/app\"", "IMAGE_APP_DIR=\"app\"",
                "IMAGE_RUNTIME_DIR=\"lib/runtime\"", "IMAGE_RUNTIME_DIR=\"runtime\"",
                "JFX_MODULE_PATH='$APPDIR/../javafx'", "JFX_MODULE_PATH='$APPDIR/../lib/javafx'")) {
            assertTrue(packageScript.contains(spelling),
                    PACKAGE_SCRIPT + " does not declare " + spelling + ", so the image assertions"
                            + " assume one platform's layout");
        }
        assertTrue(packageScript.contains("JFX_IMAGE_DIR=\"$DEST/$APP_NAME/lib/"),
                PACKAGE_SCRIPT + " no longer writes lib/javafx itself, so the module path and the"
                        + " license location depend on jpackage's copy behaviour");
    }

    /**
     * The installed launcher is a different code path from the app image, and
     * only the smoke run reaches it. It needs root to install the .deb, which a
     * developer machine usually does not have, so the script has to fall back to
     * unpacking the payload and standing in for {@code dpkg} - otherwise the
     * packaged branch is exercised for the first time on a CI runner, which is
     * where both of P3.2.1's installer bugs were found.
     */
    @Test
    void theSmokeRunReachesTheInstalledLauncherWithoutRoot() throws IOException {
        var script = Files.readString(Path.of("scripts", "smoke-appimage.sh"),
                StandardCharsets.UTF_8);

        assertTrue(script.contains("smoke_linux_deb_payload"),
                "scripts/smoke-appimage.sh no longer launches the .deb payload, so the packaged"
                        + " branch of jpackage's launcher is only tested where root exists");
        assertTrue(script.contains("libapplauncher.so"),
                "scripts/smoke-appimage.sh does not assert the payload's launcher library, which"
                        + " the installed launcher resolves through dpkg");
        assertTrue(script.contains("dpkg-deb -x"),
                "scripts/smoke-appimage.sh does not unpack the payload it launches");
    }

    /**
     * The unnamed-module warning is the one symptom of classpath JavaFX that a
     * user can see, and it appears in a log rather than in the UI - so the
     * smoke run is the only place it can be caught before a release. Checking it
     * on every launch, rather than once, costs a grep and covers the installed
     * package as well as the image.
     */
    @Test
    void theSmokeRunFailsOnClasspathJavaFx() throws IOException {
        var script = Files.readString(Path.of("scripts", "smoke-appimage.sh"),
                StandardCharsets.UTF_8);

        assertTrue(script.contains("assert_no_unnamed_module"),
                "scripts/smoke-appimage.sh does not look for the unnamed-module warning");
        assertTrue(script.contains("unnamed module"),
                "scripts/smoke-appimage.sh never names the warning it must reject");
    }

    /**
     * The job block around {@code offset}: from its own indentation level
     * onwards, up to the next two-space job key. Written generally because the
     * shared workflow's jobs are not the entry points' jobs, and a helper that
     * knows only one workflow's job names silently returns the rest of the file.
     */
    private static String jobBlock(String workflow, int offset) {
        assertTrue(offset >= 0, "the workflow no longer contains the job this test looks for");
        var position = offset + 1;
        var first = true;
        for (var line : workflow.substring(position).split("\n", -1)) {
            if (!first && line.matches("^  [A-Za-z0-9_-]+:.*")) {
                return workflow.substring(offset, position - 1);
            }
            first = false;
            position += line.length() + 1;
        }
        return workflow.substring(offset);
    }

    /** The step block around {@code offset}: from its own {@code - name:} to the next step. */
    private static String stepBlock(String workflow, int offset) {
        var start = workflow.lastIndexOf("- name:", offset);
        assertTrue(start >= 0, "the workflow no longer contains the step this test looks for");
        var end = workflow.indexOf("\n      - ", offset);
        return end < 0 ? workflow.substring(start) : workflow.substring(start, end);
    }

    /** The step block around {@code offset}: from its own {@code - name:} onwards. */
    private static String stepContaining(String workflow, int offset) {
        return workflow.substring(workflow.lastIndexOf("- name:", offset), offset);
    }
}
