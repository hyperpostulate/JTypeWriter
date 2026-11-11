/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) 2026 Mesut ORMANLI
 */

package org.mesutormanli.jtypewriter.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The search and replace decisions behind the find panel, with no JavaFX type
 * in sight.
 *
 * <p>{@code TextArea} has no search API of its own, so the only way to keep the
 * panel testable is to lift the whole decision out of the control: which
 * occurrences exist, which one is current, what "next" means at the end of the
 * document, and what the text becomes after a replacement. Those are plain
 * string and integer questions; the panel that shows them is only the glue that
 * paints the answers.</p>
 *
 * <p><strong>The query is literal text, never a regular expression.</strong> A
 * writer searching for {@code a.*b} means those five characters, and a
 * replacement of {@code $1} is four characters, not a group reference. Regex
 * syntax is therefore not interpreted anywhere: a pattern that happened to be
 * invalid could not even be reported without inventing a second error path, and
 * the panel has no "regex" switch to explain.</p>
 *
 * <p><strong>Matches are non-overlapping and left to right.</strong> Searching
 * {@code aa} in {@code aaaa} reports two matches, not three: after each match
 * the scan continues at its end, which is what a replace pass needs to stay
 * linear. This is also why a needle that is not there costs one scan and no
 * allocation beyond the empty list.</p>
 *
 * <p><strong>Case folding is locale independent.</strong>
 * {@link String#regionMatches(boolean, int, String, int, int)} is used instead
 * of {@code toLowerCase()}: a {@code String} lowercase conversion follows the
 * default locale, and under a Turkish default {@code "I"} becomes {@code "ı"}
 * while {@code "i"} stays {@code "i"}, so the same query would find different
 * text on different machines. The tests prove it under a {@code tr-TR}
 * default.</p>
 *
 * <p><strong>Navigation wraps in both directions.</strong> {@code F3} on the
 * last match selects the first, {@code Shift+F3} on the first selects the
 * last; a search starts <em>before</em> the first match, so the first
 * {@code F3} selects it rather than skipping it. A document with no match has
 * no current selection and both directions stay empty - wrapping over an empty
 * list would be a fake hit.</p>
 *
 * <p><strong>Replacements re-scan instead of shifting indices by hand.</strong>
 * Replacing a match changes the length of the document, so every later match
 * moves. The result is therefore rebuilt in one left-to-right pass and the match
 * list is recomputed from the new text, which is the only way the cursor cannot
 * end up pointing one character into a word. "Replace all" deliberately does
 * <em>not</em> scan its own output: replacing {@code a} with {@code aa} would
 * otherwise never terminate, so the pass is driven by the matches of the
 * original text and the count is exact.</p>
 *
 * <p><strong>A range that does not fit is refused, never clamped.</strong> The
 * matches are a snapshot: the panel may be holding a scan of the previous text
 * while the user keeps typing. Applying a range that runs past the end of the
 * current document would throw or, worse, overwrite the wrong characters, so
 * {@link #replace(String, Match, String)} returns nothing and
 * {@link #install(String, String, boolean, List)} drops ranges that no longer
 * fit. The next scan repairs the list; until then the panel can only select and
 * replace what still lies inside the text.</p>
 */
public final class FindModel {

    /**
     * One occurrence of the query, as a half-open {@code [start, end)} range.
     *
     * <p>The invariant is checked at construction so a negative or inverted
     * range cannot travel through the model. A range that is structurally valid
     * but longer than the document is a different matter - that is a stale scan,
     * decided against the current length by {@link #fitsIn(int)}.</p>
     */
    public record Match(int start, int end) {

        public Match {
            if (start < 0 || end < start) {
                throw new IllegalArgumentException("not a range: " + start + ".." + end);
            }
        }

        /** Number of characters covered by this match. */
        public int length() {
            return end - start;
        }

        /** Whether this range lies inside a document of {@code documentLength} characters. */
        public boolean fitsIn(int documentLength) {
            return end <= documentLength;
        }
    }

    /**
     * The document after a replacement, how many occurrences changed, and the
     * match the panel should select next (absent when there is none).
     *
     * {@link #count()} is the number of replacements actually applied: {@code 0}
     * means nothing changed, which is also the answer for a stale or missing
     * selection. It is not the number of matches that remain.
     */
    public record Replacement(String document, int count, Optional<Match> current) {

        public Replacement {
            document = document == null ? "" : document;
            current = current == null ? Optional.empty() : current;
        }

        /** Whether the document actually changed. */
        public boolean changed() {
            return count > 0;
        }
    }

    private String document = "";
    private String needle = "";
    private boolean caseSensitive;
    private List<Match> matches = List.of();
    private int cursor = -1;

    /**
     * A new model over an empty document and an empty query. The panel calls
     * {@link #setQuery(String, boolean)} and {@link #setDocument(String)} before
     * the first scan; nothing matches until it does.
     */
    public FindModel() {
    }

    /**
     * The whole search in one call: adopts the document and the query, scans
     * them, and leaves the cursor before the first match.
     *
     * <p>This does the O(n) work on the calling thread. A panel that must not
     * scan on the FX thread (a 64 MiB document) runs {@link #findAll(String,
     * String, boolean)} on its worker instead, then hands the result to
     * {@link #install(String, String, boolean, List)} back on the FX thread.</p>
     */
    public void search(String document, String needle, boolean caseSensitive) {
        setQuery(needle, caseSensitive);
        setDocument(document);
        matches = findAll(this.document, this.needle, this.caseSensitive);
    }

    /**
     * Adopts a new query and drops the old match list, because every range in it
     * described the previous query. The document is left untouched, so a query
     * typed into the panel does not need the editor text again.
     */
    public void setQuery(String needle, boolean caseSensitive) {
        this.needle = Objects.requireNonNull(needle, "needle");
        this.caseSensitive = caseSensitive;
        this.matches = List.of();
        this.cursor = -1;
    }

    /**
     * Adopts a new document text and drops the match list: the old ranges
     * described the old text, and keeping them would let the panel select text
     * the user has already rewritten. The query survives, so the panel can scan
     * the new text with the same needle.
     */
    public void setDocument(String document) {
        this.document = Objects.requireNonNull(document, "document");
        this.matches = List.of();
        this.cursor = -1;
    }

    /**
     * Installs a match list produced by {@link #findAll(String, String, boolean)}
     * on another thread.
     *
     * <p>The scan is only accepted when it describes the model's current
     * document, query and case mode; a result that arrives after the user has
     * typed another character or changed the query is stale and is discarded
     * (the panel schedules a new scan). Even an accepted list is filtered to the
     * ranges that still fit the document, so a malformed or stale range can
     * never make the panel select past the end.</p>
     *
     * @return whether the scan described the current state; {@code false} means
     *         it was ignored and the caller should scan again
     */
    public boolean install(String scannedDocument, String scannedNeedle, boolean scannedCaseSensitive,
                           List<Match> scannedMatches) {
        if (!Objects.equals(document, scannedDocument)
                || !Objects.equals(needle, scannedNeedle)
                || caseSensitive != scannedCaseSensitive) {
            return false;
        }
        var length = document.length();
        matches = scannedMatches == null
                ? List.of()
                : scannedMatches.stream()
                        .filter(match -> match != null && match.fitsIn(length))
                        .toList();
        cursor = -1;
        return true;
    }

    /** The text the model currently describes. */
    public String document() {
        return document;
    }

    /** The current query; empty when nothing is being searched for. */
    public String needle() {
        return needle;
    }

    /** Whether the current query distinguishes upper and lower case. */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /** The matches of the current scan, left to right and non-overlapping. */
    public List<Match> matches() {
        return matches;
    }

    /** How many occurrences the current scan found. */
    public int matchCount() {
        return matches.size();
    }

    /** The selected match, empty before the first {@code F3} and when nothing matches. */
    public Optional<Match> current() {
        return cursor >= 0 && cursor < matches.size()
                ? Optional.of(matches.get(cursor))
                : Optional.empty();
    }

    /** Index of the selected match, or {@code -1} when there is no selection. */
    public int currentIndex() {
        return current().isPresent() ? cursor : -1;
    }

    /**
     * Selects the next match, wrapping from the last to the first. With no
     * selection this selects the first match, so the first {@code F3} after a
     * search does not skip it. Returns empty when the document has no match.
     */
    public Optional<Match> next() {
        if (matches.isEmpty()) {
            cursor = -1;
            return Optional.empty();
        }
        cursor = cursor < 0 ? 0 : (cursor + 1) % matches.size();
        return current();
    }

    /**
     * Selects the previous match, wrapping from the first to the last. With no
     * selection this selects the last match, the mirror of {@link #next()}.
     */
    public Optional<Match> previous() {
        if (matches.isEmpty()) {
            cursor = -1;
            return Optional.empty();
        }
        cursor = cursor < 0 ? matches.size() - 1 : (cursor - 1 + matches.size()) % matches.size();
        return current();
    }

    /**
     * Replaces the selected match and selects the first occurrence that starts
     * after the inserted text, so the next {@code Replace} never lands on the
     * replacement itself. Returns an unchanged document and a count of {@code 0}
     * when no match is selected or its range no longer fits the document.
     */
    public Replacement replaceCurrent(String replacement) {
        var current = current();
        if (current.isEmpty()) {
            return new Replacement(document, 0, Optional.empty());
        }
        var match = current.get();
        var replacementText = replacement == null ? "" : replacement;
        var replaced = replace(document, match, replacementText);
        if (replaced.isEmpty()) {
            return new Replacement(document, 0, Optional.empty());
        }
        return applyReplacement(replaced.get(), replacementText, match.start());
    }

    /**
     * Replaces every match of the current scan in one pass and leaves no
     * selection (the caller can scan again to highlight what remains).
     *
     * <p>The replacement text is appended verbatim and is never searched, so a
     * replacement that contains the query cannot extend the pass forever. Every
     * match is applied exactly once, even when the replacement changes the
     * document length.</p>
     */
    public Replacement replaceAll(String replacement) {
        if (matches.isEmpty()) {
            return new Replacement(document, 0, Optional.empty());
        }
        var replacementText = replacement == null ? "" : replacement;
        var builder = new StringBuilder(document.length());
        var copied = 0;
        var count = 0;
        for (var match : matches) {
            // findAll only produces ordered non-overlapping ranges; refusing a
            // malformed one here is the last line of defence against corruption.
            if (!match.fitsIn(document.length()) || match.start() < copied) {
                return new Replacement(document, 0, Optional.empty());
            }
            builder.append(document, copied, match.start());
            builder.append(replacementText);
            copied = match.end();
            count++;
        }
        builder.append(document, copied, document.length());
        document = builder.toString();
        matches = findAll(document, needle, caseSensitive);
        cursor = -1;
        return new Replacement(document, count, Optional.empty());
    }

    /**
     * The pure scan: every non-overlapping occurrence of {@code needle} in
     * {@code document}, left to right. Safe to call on any thread; it touches no
     * field and starts no JavaFX toolkit.
     *
     * <p>An empty document or an empty query has no matches. A query longer than
     * the remaining text simply does not match - the scan never steps outside
     * the document.</p>
     */
    public static List<Match> findAll(String document, String needle, boolean caseSensitive) {
        if (document == null || needle == null || document.isEmpty() || needle.isEmpty()) {
            return List.of();
        }
        var found = new ArrayList<Match>();
        var from = 0;
        while (from <= document.length() - needle.length()) {
            var at = indexOf(document, needle, from, caseSensitive);
            if (at < 0) {
                break;
            }
            found.add(new Match(at, at + needle.length()));
            from = at + needle.length();
        }
        return List.copyOf(found);
    }

    /**
     * Applies one replacement if, and only if, the range lies inside the
     * document.
     *
     * <p>This is the guard against a stale range: a match captured before an
     * edit can end past the current text, and substring arithmetic on it would
     * throw {@code StringIndexOutOfBoundsException} after the user already
     * confirmed the replace. An empty result means "refused, nothing changed".</p>
     */
    public static Optional<String> replace(String document, Match match, String replacement) {
        if (document == null || match == null || !match.fitsIn(document.length())) {
            return Optional.empty();
        }
        var replacementText = replacement == null ? "" : replacement;
        return Optional.of(document.substring(0, match.start())
                + replacementText
                + document.substring(match.end()));
    }

    /**
     * Re-scans the text produced by a replacement and points the cursor at the
     * first match after the inserted text. A single shared path means
     * replace-one cannot disagree with the scan about where a match now starts.
     */
    private Replacement applyReplacement(String newDocument, String replacementText, int start) {
        document = newDocument;
        matches = findAll(document, needle, caseSensitive);
        var after = start + replacementText.length();
        cursor = -1;
        for (var index = 0; index < matches.size(); index++) {
            if (matches.get(index).start() >= after) {
                cursor = index;
                break;
            }
        }
        return new Replacement(document, 1, current());
    }

    private static int indexOf(String document, String needle, int from, boolean caseSensitive) {
        var last = document.length() - needle.length();
        for (var at = from; at <= last; at++) {
            if (document.regionMatches(!caseSensitive, at, needle, 0, needle.length())) {
                return at;
            }
        }
        return -1;
    }
}
