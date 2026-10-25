package org.mesutormanli.jtypewriter.locale;

public enum Language {
    EN, TR, DE, FR, IT, ES;

    /**
     * The language's own name for itself, as shown in the toolbar dropdown.
     *
     * <p>Deliberately <em>not</em> a bundle key: a language's native name is
     * identical in every UI language ("Türkçe" is "Türkçe" in the English,
     * German and Turkish UIs alike), so bundling it would duplicate one string
     * across six files. Resolving the name through
     * {@code Locale#getDisplayLanguage} is not an option either - that routes
     * the result through the <em>system</em> locale, exactly the trap the
     * {@link Messages} bundle lookup exists to avoid ("English" once showed
     * Turkish on a Turkish machine).</p>
     */
    public String nativeName() {
        return switch (this) {
            case EN -> "English";
            case TR -> "Türkçe";
            case DE -> "Deutsch";
            case FR -> "Français";
            case IT -> "Italiano";
            case ES -> "Español";
        };
    }

    public String code() {
        return name();
    }
}
