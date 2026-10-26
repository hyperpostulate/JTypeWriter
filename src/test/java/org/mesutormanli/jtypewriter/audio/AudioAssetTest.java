package org.mesutormanli.jtypewriter.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * Guards the provenance contract introduced for PW-36 (finding B32): the
 * bundled keystroke sample has a written, unambiguous licence and origin.
 *
 * <p>The sample was made by hand in Audacity, so there is nothing to
 * regenerate and no generator script is claimed. The SHA-256 recorded in
 * {@code audio/PROVENANCE.md} is the contract that keeps the shipped bytes and
 * the statement in sync in both directions: editing the sample without
 * updating the statement fails here, and editing the statement without
 * editing the sample fails here too.</p>
 *
 * <p>No JavaFX toolkit, no filesystem writes, no audio device: the WAV and the
 * statement are read from the classpath and the RIFF/WAVE bytes are parsed by
 * hand with a little-endian {@link ByteBuffer}, following the same headless
 * discipline as {@code PackagingNoticesTest}.</p>
 */
class AudioAssetTest {

    private static final String SAMPLE_RESOURCE = "/audio/keystroke.wav";
    private static final String STATEMENT_RESOURCE = "/audio/PROVENANCE.md";

    private static final int EXPECTED_SIZE = 71_204;
    private static final int EXPECTED_CHANNELS = 2;
    private static final int EXPECTED_SAMPLE_RATE = 48_000;
    private static final int EXPECTED_BITS = 16;
    private static final int EXPECTED_FRAMES = 17_790;
    private static final String EXPECTED_HASH =
            "e4ed1ba70dc0f8b94b0edc79ed097f3285ab9812ae94a7ee0603f2a652c85a13";

    private static final String EXPECTED_SIZE_TEXT = "71,204";
    private static final String EXPECTED_RATE_TEXT = "48,000 Hz";
    private static final String EXPECTED_BITS_TEXT = "16-bit";
    private static final String EXPECTED_CHANNELS_TEXT = "stereo";
    private static final String EXPECTED_FRAMES_TEXT = "17,790";
    private static final String EXPECTED_DURATION_TEXT = "0.370625";

    @Test
    void keystrokeSampleIsOnTheClasspath() throws IOException {
        var bytes = readBytes(SAMPLE_RESOURCE);
        assertNotNull(bytes, SAMPLE_RESOURCE + " is not on the classpath; "
                + "src/main/resources/audio/keystroke.wav is not being copied as a resource");
        assertEquals(EXPECTED_SIZE, bytes.length,
                SAMPLE_RESOURCE + " size changed; update PROVENANCE.md and this test together");
        assertEquals("RIFF", ascii(bytes, 0, 4), SAMPLE_RESOURCE + " does not start with RIFF");
        assertEquals("WAVE", ascii(bytes, 8, 4), SAMPLE_RESOURCE + " is not a WAVE file");
    }

    @Test
    void keystrokeSampleFormatMatchesTheStatement() throws IOException {
        var bytes = readBytes(SAMPLE_RESOURCE);
        assertNotNull(bytes, SAMPLE_RESOURCE + " is not on the classpath");
        var fmt = chunkPosition(bytes, "fmt ");
        var dataPosition = chunkPosition(bytes, "data");
        assertTrue(fmt >= 0, SAMPLE_RESOURCE + " has no fmt chunk");
        assertTrue(dataPosition >= 0, SAMPLE_RESOURCE + " has no data chunk");

        var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        var audioFormat = Short.toUnsignedInt(buffer.getShort(fmt + 8));
        var channels = Short.toUnsignedInt(buffer.getShort(fmt + 10));
        var sampleRate = buffer.getInt(fmt + 12);
        var bits = Short.toUnsignedInt(buffer.getShort(fmt + 22));
        var dataLength = buffer.getInt(dataPosition + 4);

        assertEquals(1, audioFormat, SAMPLE_RESOURCE + " is not PCM");
        assertEquals(EXPECTED_CHANNELS, channels, SAMPLE_RESOURCE + " channel count changed");
        assertEquals(EXPECTED_SAMPLE_RATE, sampleRate, SAMPLE_RESOURCE + " sample rate changed");
        assertEquals(EXPECTED_BITS, bits, SAMPLE_RESOURCE + " bit depth changed");

        var frames = dataLength / (channels * (bits / 8));
        assertEquals(EXPECTED_FRAMES, frames, SAMPLE_RESOURCE + " frame count changed");
        var duration = (double) frames / sampleRate;

        var statement = readText(STATEMENT_RESOURCE);
        assertNotNull(statement, STATEMENT_RESOURCE + " is not on the classpath");
        assertContains(statement, EXPECTED_SIZE_TEXT, "file size");
        assertContains(statement, EXPECTED_RATE_TEXT, "sample rate");
        assertContains(statement, EXPECTED_BITS_TEXT, "bit depth");
        assertContains(statement, EXPECTED_CHANNELS_TEXT, "channel layout");
        assertContains(statement, EXPECTED_FRAMES_TEXT, "frame count");
        assertContains(statement, EXPECTED_DURATION_TEXT, "duration");
        assertEquals(EXPECTED_DURATION_TEXT, format(Locale.ROOT, "%.6f", duration),
                SAMPLE_RESOURCE + " duration changed; update PROVENANCE.md and this test together");
    }

    @Test
    void keystrokeSampleHashMatchesTheStatement() throws IOException {
        var bytes = readBytes(SAMPLE_RESOURCE);
        assertNotNull(bytes, SAMPLE_RESOURCE + " is not on the classpath");
        var actual = sha256(bytes);
        assertEquals(EXPECTED_HASH, actual,
                SAMPLE_RESOURCE + " bytes no longer match the recorded SHA-256; "
                        + "update PROVENANCE.md and this test if the sample changed on purpose");

        var statement = readText(STATEMENT_RESOURCE);
        assertNotNull(statement, STATEMENT_RESOURCE + " is not on the classpath");
        assertContains(statement, actual, "SHA-256");
        assertContains(statement, EXPECTED_HASH, "SHA-256");
    }

    @Test
    void keystrokeSampleIsAnAudibleTransient() throws IOException {
        var bytes = readBytes(SAMPLE_RESOURCE);
        assertNotNull(bytes, SAMPLE_RESOURCE + " is not on the classpath");
        var dataPosition = chunkPosition(bytes, "data");
        assertTrue(dataPosition >= 0, SAMPLE_RESOURCE + " has no data chunk");
        var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        var dataLength = buffer.getInt(dataPosition + 4);
        var sampleCount = dataLength / 2;

        var peak = 0;
        for (var i = 0; i < sampleCount; i++) {
            peak = Math.max(peak, Math.abs((int) buffer.getShort(dataPosition + 8 + i * 2)));
        }
        assertTrue(peak > 1_000,
                "peak sample magnitude " + peak + " is too quiet to be an audible keystroke");
        assertTrue(peak < 32_767,
                "peak sample magnitude " + peak + " is clipped; the sample is meant to be normalised");

        var window = Math.max(1, sampleCount / 10);
        var loudest = 0.0;
        var step = Math.max(1, window / 8);
        for (var start = 0; start + window <= sampleCount; start += step) {
            loudest = Math.max(loudest, meanAbsolute(buffer, dataPosition + 8, start, window));
        }
        var tail = meanAbsolute(buffer, dataPosition + 8, sampleCount - window, window);
        assertTrue(tail < loudest,
                "tail mean " + tail + " is not below the loudest window mean " + loudest
                        + "; the sample does not decay");
    }

    @Test
    void provenanceStatesAuthorshipAndLicence() throws IOException {
        var statement = readText(STATEMENT_RESOURCE);
        assertNotNull(statement, STATEMENT_RESOURCE + " is not on the classpath");
        assertContains(statement, "Mesut ORMANLI", "author");
        assertContains(statement, "Apache-2.0", "licence");
        assertContains(statement, "keystroke.wav", "file name");
        assertContains(statement, "no third-party sample", "no-third-party assertion");
        assertFalse(statement.contains("\r"), STATEMENT_RESOURCE + " must be LF-only");

        var lines = statement.split("\n", -1);
        for (var i = 0; i < lines.length; i++) {
            assertTrue(lines[i].length() <= 100,
                    STATEMENT_RESOURCE + " line " + (i + 1) + " is " + lines[i].length()
                            + " characters (limit 100): " + lines[i]);
        }
    }

    private static void assertContains(String text, String expected, String label) {
        assertTrue(text.contains(expected),
                STATEMENT_RESOURCE + " does not contain the " + label + " \"" + expected + "\"");
    }

    private static int chunkPosition(byte[] bytes, String id) {
        var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        var position = 12;
        while (position + 8 <= bytes.length) {
            var chunkId = ascii(bytes, position, 4);
            var size = buffer.getInt(position + 4);
            if (chunkId.equals(id)) {
                return position;
            }
            position += 8 + size + (size % 2);
        }
        return -1;
    }

    private static double meanAbsolute(ByteBuffer buffer, int dataStart, int start, int length) {
        var total = 0L;
        for (var i = 0; i < length; i++) {
            total += Math.abs((int) buffer.getShort(dataStart + (start + i) * 2));
        }
        return (double) total / length;
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    private static String sha256(byte[] bytes) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is required by the Java platform", e);
        }
    }

    private static String hex(byte[] digest) {
        var out = new StringBuilder(digest.length * 2);
        for (var b : digest) {
            out.append(String.format(Locale.ROOT, "%02x", b));
        }
        return out.toString();
    }

    private static byte[] readBytes(String resource) throws IOException {
        try (InputStream in = AudioAssetTest.class.getResourceAsStream(resource)) {
            return in == null ? null : in.readAllBytes();
        }
    }

    private static String readText(String resource) throws IOException {
        var bytes = readBytes(resource);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private static String format(Locale locale, String pattern, Object value) {
        return String.format(locale, pattern, value);
    }
}
