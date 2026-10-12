package org.mesutormanli.jtypewriter.service;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Encoding detection and lossless round-tripping for text documents.
 * Deliberately free of JavaFX/UI types so it can be unit tested headlessly.
 *
 * <p>Detection order: byte-order marks (UTF-8 / UTF-16 LE / UTF-16 BE), then
 * strict UTF-8 decoding. Bytes that are neither BOM-marked nor valid UTF-8 are
 * reported as <em>undecodable</em> ({@code null} from {@link #decode}) so the
 * caller can ask the user for a legacy encoding (e.g. {@code windows-1254} for
 * Turkish ANSI files) instead of silently corrupting the text. Whichever
 * encoding a file was read with - including its BOM flag - is what
 * {@link #encode} writes back, so opening and saving never changes a file's
 * encoding behind the user's back.</p>
 */
public final class TextCodec {

    /** Legacy fallback suggested for Turkish ANSI documents. */
    public static final Charset WINDOWS_1254 = Charset.forName("windows-1254");

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF16BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] NO_BYTES = {};

    /** A decoded document together with the exact encoding it came from. */
    public record Result(String text, Charset charset, boolean bom) {
    }

    private TextCodec() {
    }

    /**
     * Detects the encoding of a document.
     *
     * @return the decoded document, or {@code null} when the bytes carry no
     * recognizable BOM and are not valid UTF-8
     */
    public static Result decode(byte[] bytes) {
        if (startsWith(bytes, UTF8_BOM)) {
            return new Result(decodeLenient(slice(bytes, UTF8_BOM.length), StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8, true);
        }
        if (startsWith(bytes, UTF16LE_BOM)) {
            return new Result(decodeLenient(slice(bytes, UTF16LE_BOM.length), StandardCharsets.UTF_16LE),
                    StandardCharsets.UTF_16LE, true);
        }
        if (startsWith(bytes, UTF16BE_BOM)) {
            return new Result(decodeLenient(slice(bytes, UTF16BE_BOM.length), StandardCharsets.UTF_16BE),
                    StandardCharsets.UTF_16BE, true);
        }
        var utf8 = decodeStrict(bytes, StandardCharsets.UTF_8);
        return utf8 == null ? null : new Result(utf8, StandardCharsets.UTF_8, false);
    }

    /** Decodes with an explicitly chosen legacy encoding (never fails). */
    public static Result decodeWith(byte[] bytes, Charset charset) {
        return new Result(decodeLenient(bytes, charset), charset, false);
    }

    /**
     * Encodes text in the given charset, reproducing the BOM when requested.
     * Encoding is strict: a character the charset cannot represent raises
     * {@link CharacterCodingException} instead of being silently replaced with
     * {@code ?} - a save must never quietly destroy text (the caller can offer
     * the user a switch to UTF-8, see {@code FileService}).
     */
    public static byte[] encode(String text, Charset charset, boolean bom) throws CharacterCodingException {
        var encoder = charset.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        var buffer = encoder.encode(CharBuffer.wrap(text));
        var body = new byte[buffer.remaining()];
        buffer.get(body);
        var prefix = bom ? bomFor(charset) : NO_BYTES;
        if (prefix.length == 0) {
            return body;
        }
        var result = Arrays.copyOf(prefix, prefix.length + body.length);
        System.arraycopy(body, 0, result, prefix.length, body.length);
        return result;
    }

    private static byte[] bomFor(Charset charset) {
        if (StandardCharsets.UTF_8.equals(charset)) {
            return UTF8_BOM;
        }
        if (StandardCharsets.UTF_16LE.equals(charset)) {
            return UTF16LE_BOM;
        }
        if (StandardCharsets.UTF_16BE.equals(charset)) {
            return UTF16BE_BOM;
        }
        return NO_BYTES;
    }

    private static String decodeStrict(byte[] bytes, Charset charset) {
        var decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static String decodeLenient(byte[] bytes, Charset charset) {
        var decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            // REPLACE mode never reports; belt and braces
            return new String(bytes, charset);
        }
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(slice(bytes, 0, prefix.length), prefix);
    }

    private static byte[] slice(byte[] bytes, int from) {
        return slice(bytes, from, bytes.length);
    }

    private static byte[] slice(byte[] bytes, int from, int to) {
        return Arrays.copyOfRange(bytes, from, to);
    }
}