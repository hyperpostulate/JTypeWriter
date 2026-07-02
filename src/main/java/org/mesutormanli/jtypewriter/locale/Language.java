package org.mesutormanli.jtypewriter.locale;

public enum Language {
    EN, TR, DE, FR, IT, ES;

    public Language toggle() {
        Language[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String code() {
        return name();
    }
}
