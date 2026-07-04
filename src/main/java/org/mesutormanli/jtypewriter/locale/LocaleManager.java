package org.mesutormanli.jtypewriter.locale;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class LocaleManager {

    private final List<Consumer<Language>> listeners = new CopyOnWriteArrayList<>();
    private Language currentLanguage = detectSystemLanguage();

    private static Language detectSystemLanguage() {
        return switch (Locale.getDefault().getLanguage()) {
            case "tr" -> Language.TR;
            case "de" -> Language.DE;
            case "fr" -> Language.FR;
            case "it" -> Language.IT;
            case "es" -> Language.ES;
            default -> Language.EN;
        };
    }

    public Language getCurrent() {
        return currentLanguage;
    }

    public void setLanguage(Language language) {
        if (currentLanguage == language) return;
        currentLanguage = language;
        listeners.forEach(l -> l.accept(language));
    }

    public void toggle() {
        setLanguage(currentLanguage.toggle());
    }

    public void addListener(Consumer<Language> listener) {
        listeners.add(listener);
    }
}
