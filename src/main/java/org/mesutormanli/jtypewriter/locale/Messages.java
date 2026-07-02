package org.mesutormanli.jtypewriter.locale;

import org.springframework.stereotype.Component;

@Component
public class Messages {

    private final LocaleManager localeManager;

    public Messages(LocaleManager localeManager) {
        this.localeManager = localeManager;
    }

    private Language l() {
        return localeManager.getCurrent();
    }

    public String toolbarOpen() {
        return switch (l()) {
            case EN -> "Open";
            case TR -> "Aç";
            case DE -> "Öffnen";
            case FR -> "Ouvrir";
            case IT -> "Apri";
            case ES -> "Abrir";
        };
    }

    public String toolbarSave() {
        return switch (l()) {
            case EN -> "Save";
            case TR -> "Kaydet";
            case DE -> "Speichern";
            case FR -> "Enregistrer";
            case IT -> "Salva";
            case ES -> "Guardar";
        };
    }

    public String toolbarTheme() {
        return switch (l()) {
            case EN -> "Theme";
            case TR -> "Tema";
            case DE -> "Design";
            case FR -> "Thème";
            case IT -> "Tema";
            case ES -> "Tema";
        };
    }

    public String toolbarFocus() {
        return switch (l()) {
            case EN -> "Focus";
            case TR -> "Odak";
            case DE -> "Fokus";
            case FR -> "Focus";
            case IT -> "Focus";
            case ES -> "Enfoque";
        };
    }

    public String toolbarYolo() {
        return "YOLO";
    }

    public String toolbarSound() {
        return switch (l()) {
            case EN -> "Sound";
            case TR -> "Ses";
            case DE -> "Ton";
            case FR -> "Son";
            case IT -> "Suono";
            case ES -> "Sonido";
        };
    }

    public String toolbarFontUp() {
        return "A+";
    }

    public String toolbarFontDown() {
        return "A-";
    }

    public String langCode() {
        return l().code();
    }

    public String labelOn() {
        return switch (l()) {
            case EN -> "On";
            case TR -> "Açık";
            case DE -> "Ein";
            case FR -> "Activé";
            case IT -> "Attivo";
            case ES -> "Activado";
        };
    }

    public String labelOff() {
        return switch (l()) {
            case EN -> "Off";
            case TR -> "Kapalı";
            case DE -> "Aus";
            case FR -> "Désactivé";
            case IT -> "Disattivo";
            case ES -> "Desactivado";
        };
    }

    public String labelThemeLight() {
        return switch (l()) {
            case EN -> "Light";
            case TR -> "Açık";
            case DE -> "Hell";
            case FR -> "Clair";
            case IT -> "Chiaro";
            case ES -> "Claro";
        };
    }

    public String labelThemeDark() {
        return switch (l()) {
            case EN -> "Dark";
            case TR -> "Koyu";
            case DE -> "Dunkel";
            case FR -> "Sombre";
            case IT -> "Scuro";
            case ES -> "Oscuro";
        };
    }

    public String labelThemeSepia() {
        return switch (l()) {
            case EN -> "Sepia";
            case TR -> "Sepya";
            case DE -> "Sepia";
            case FR -> "Sépia";
            case IT -> "Seppia";
            case ES -> "Sepia";
        };
    }

    public String labelFocusLine() {
        return switch (l()) {
            case EN -> "Line";
            case TR -> "Satır";
            case DE -> "Zeile";
            case FR -> "Ligne";
            case IT -> "Riga";
            case ES -> "Línea";
        };
    }

    public String labelFocusParagraph() {
        return switch (l()) {
            case EN -> "Paragraph";
            case TR -> "Paragraf";
            case DE -> "Absatz";
            case FR -> "Paragraphe";
            case IT -> "Paragrafo";
            case ES -> "Párrafo";
        };
    }

    public String labelFocusOff() {
        return switch (l()) {
            case EN -> "Off";
            case TR -> "Kapalı";
            case DE -> "Aus";
            case FR -> "Désactivé";
            case IT -> "Disattivo";
            case ES -> "Desactivado";
        };
    }

    public String yoloModeOn() {
        return switch (l()) {
            case EN -> "On";
            case TR -> "Açık";
            case DE -> "Ein";
            case FR -> "Activé";
            case IT -> "Attivo";
            case ES -> "Activado";
        };
    }

    public String yoloModeOff() {
        return switch (l()) {
            case EN -> "Off";
            case TR -> "Kapalı";
            case DE -> "Aus";
            case FR -> "Désactivé";
            case IT -> "Disattivo";
            case ES -> "Desactivado";
        };
    }

    public String soundOn() {
        return switch (l()) {
            case EN -> "On";
            case TR -> "Açık";
            case DE -> "Ein";
            case FR -> "Activé";
            case IT -> "Attivo";
            case ES -> "Activado";
        };
    }

    public String soundOff() {
        return switch (l()) {
            case EN -> "Off";
            case TR -> "Kapalı";
            case DE -> "Aus";
            case FR -> "Désactivé";
            case IT -> "Disattivo";
            case ES -> "Desactivado";
        };
    }

    public String statsFormat(int words, int chars, int charsNoSpace, int lines, long keystrokes, String duration) {
        return switch (l()) {
            case EN -> String.format("%d words · %d chars (%d no space) · %d lines · %d keys · %s",
                    words, chars, charsNoSpace, lines, keystrokes, duration);
            case TR -> String.format("%d kelime · %d karakter (%d boşluksuz) · %d satır · %d tuş · %s",
                    words, chars, charsNoSpace, lines, keystrokes, duration);
            case DE -> String.format("%d Wörter · %d Zeichen (%d ohne Leerzeichen) · %d Zeilen · %d Anschläge · %s",
                    words, chars, charsNoSpace, lines, keystrokes, duration);
            case FR -> String.format("%d mots · %d car. (%d sans espace) · %d lignes · %d frappes · %s",
                    words, chars, charsNoSpace, lines, keystrokes, duration);
            case IT -> String.format("%d parole · %d car. (%d senza spazi) · %d righe · %d battute · %s",
                    words, chars, charsNoSpace, lines, keystrokes, duration);
            case ES -> String.format("%d palabras · %d car. (%d sin espacios) · %d líneas · %d pulsaciones · %s",
                    words, chars, charsNoSpace, lines, keystrokes, duration);
        };
    }

    public String welcomeTitle() {
        return "JTypeWriter";
    }

    public String welcomeSubtitle() {
        return switch (l()) {
            case EN -> "Distraction-Free Text Editor";
            case TR -> "Dikkat Dağıtmayan Metin Editörü";
            case DE -> "Ablenkungsfreier Texteditor";
            case FR -> "Éditeur de texte sans distraction";
            case IT -> "Editor di testo senza distrazioni";
            case ES -> "Editor de texto sin distracciones";
        };
    }

    public String welcomeShortcuts() {
        return switch (l()) {
            case EN -> """
                    Ctrl+T          Toggle Toolbar
                    Ctrl+Shift+T    Switch Theme (Dark/Light/Sepia)
                    Ctrl+Shift+F    Line Focus Mode
                    Ctrl+Shift+P    Paragraph Focus Mode
                    Ctrl+Shift+Y    YOLO Mode (no deletion)
                    Ctrl++          Increase Font Size
                    Ctrl+-          Decrease Font Size
                    Ctrl+0          Reset Font Size
                    Ctrl+O          Open File
                    Ctrl+S          Save / Save As
                    Escape          Exit Fullscreen
                    """;
            case TR -> """
                    Ctrl+T          Toolbar aç/kapa
                    Ctrl+Shift+T    Tema değiştir (Dark/Light/Sepya)
                    Ctrl+Shift+F    Satır odak modu
                    Ctrl+Shift+P    Paragraf odak modu
                    Ctrl+Shift+Y    YOLO modu (silme devre dışı)
                    Ctrl++          Font büyüt
                    Ctrl+-          Font küçült
                    Ctrl+0          Font sıfırla
                    Ctrl+O          Dosya aç
                    Ctrl+S          Kaydet / Farklı kaydet
                    Escape          Tam ekrandan çık
                    """;
            case DE -> """
                    Ctrl+T          Toolbar umschalten
                    Ctrl+Shift+T    Design wechseln (Dunkel/Hell/Sepia)
                    Ctrl+Shift+F    Zeilenfokus-Modus
                    Ctrl+Shift+P    Absatzfokus-Modus
                    Ctrl+Shift+Y    YOLO-Modus (kein Löschen)
                    Ctrl++          Schrift vergrößern
                    Ctrl+-          Schrift verkleinern
                    Ctrl+0          Schrift zurücksetzen
                    Ctrl+O          Datei öffnen
                    Ctrl+S          Speichern / Speichern unter
                    Escape          Vollbild verlassen
                    """;
            case FR -> """
                    Ctrl+T          Afficher/Masquer la barre
                    Ctrl+Shift+T    Changer le thème (Sombre/Clair/Sépia)
                    Ctrl+Shift+F    Mode focus ligne
                    Ctrl+Shift+P    Mode focus paragraphe
                    Ctrl+Shift+Y    Mode YOLO (pas de suppression)
                    Ctrl++          Augmenter la police
                    Ctrl+-          Réduire la police
                    Ctrl+0          Réinitialiser la police
                    Ctrl+O          Ouvrir un fichier
                    Ctrl+S          Enregistrer / Enregistrer sous
                    Escape          Quitter le plein écran
                    """;
            case IT -> """
                    Ctrl+T          Attiva/disattiva barra
                    Ctrl+Shift+T    Cambia tema (Scuro/Chiaro/Seppia)
                    Ctrl+Shift+F    Modalità focus riga
                    Ctrl+Shift+P    Modalità focus paragrafo
                    Ctrl+Shift+Y    Modalità YOLO (nessuna cancellazione)
                    Ctrl++          Aumenta carattere
                    Ctrl+-          Riduci carattere
                    Ctrl+0          Reimposta carattere
                    Ctrl+O          Apri file
                    Ctrl+S          Salva / Salva con nome
                    Escape          Esci da schermo intero
                    """;
            case ES -> """
                    Ctrl+T          Mostrar/ocultar barra
                    Ctrl+Shift+T    Cambiar tema (Oscuro/Claro/Sepia)
                    Ctrl+Shift+F    Modo enfoque línea
                    Ctrl+Shift+P    Modo enfoque párrafo
                    Ctrl+Shift+Y    Modo YOLO (sin eliminación)
                    Ctrl++          Aumentar fuente
                    Ctrl+-          Disminuir fuente
                    Ctrl+0          Restablecer fuente
                    Ctrl+O          Abrir archivo
                    Ctrl+S          Guardar / Guardar como
                    Escape          Salir de pantalla completa
                    """;
        };
    }

    public String welcomeButton() {
        return switch (l()) {
            case EN -> "Start Writing";
            case TR -> "Yazmaya Başla";
            case DE -> "Schreiben beginnen";
            case FR -> "Commencer à écrire";
            case IT -> "Inizia a scrivere";
            case ES -> "Empezar a escribir";
        };
    }

    public String fileOpenTitle() {
        return switch (l()) {
            case EN -> "Open File";
            case TR -> "Dosya Aç";
            case DE -> "Datei öffnen";
            case FR -> "Ouvrir un fichier";
            case IT -> "Apri file";
            case ES -> "Abrir archivo";
        };
    }

    public String fileSaveTitle() {
        return switch (l()) {
            case EN -> "Save As";
            case TR -> "Farklı Kaydet";
            case DE -> "Speichern unter";
            case FR -> "Enregistrer sous";
            case IT -> "Salva con nome";
            case ES -> "Guardar como";
        };
    }

    public String fileTextFiles() {
        return switch (l()) {
            case EN -> "Text Files";
            case TR -> "Metin Dosyaları";
            case DE -> "Textdateien";
            case FR -> "Fichiers texte";
            case IT -> "File di testo";
            case ES -> "Archivos de texto";
        };
    }

    public String fileAllFiles() {
        return switch (l()) {
            case EN -> "All Files";
            case TR -> "Tüm Dosyalar";
            case DE -> "Alle Dateien";
            case FR -> "Tous les fichiers";
            case IT -> "Tutti i file";
            case ES -> "Todos los archivos";
        };
    }

    public String fileTextFile() {
        return switch (l()) {
            case EN -> "Text File";
            case TR -> "Metin Dosyası";
            case DE -> "Textdatei";
            case FR -> "Fichier texte";
            case IT -> "File di testo";
            case ES -> "Archivo de texto";
        };
    }

    public String unsavedTitle() {
        return switch (l()) {
            case EN -> "Unsaved Changes";
            case TR -> "Kaydedilmemiş Değişiklikler";
            case DE -> "Ungespeicherte Änderungen";
            case FR -> "Modifications non enregistrées";
            case IT -> "Modifiche non salvate";
            case ES -> "Cambios no guardados";
        };
    }

    public String unsavedHeader() {
        return switch (l()) {
            case EN -> "You have unsaved changes.";
            case TR -> "Dosyada kaydedilmemiş değişiklikler var.";
            case DE -> "Sie haben ungespeicherte Änderungen.";
            case FR -> "Vous avez des modifications non enregistrées.";
            case IT -> "Ci sono modifiche non salvate.";
            case ES -> "Tiene cambios no guardados.";
        };
    }

    public String unsavedContent() {
        return switch (l()) {
            case EN -> "Do you want to save?";
            case TR -> "Kaydetmek istiyor musunuz?";
            case DE -> "Möchten Sie speichern?";
            case FR -> "Voulez-vous enregistrer ?";
            case IT -> "Vuoi salvare?";
            case ES -> "¿Quiere guardar?";
        };
    }

    public String unsavedSave() {
        return switch (l()) {
            case EN -> "Save";
            case TR -> "Kaydet";
            case DE -> "Speichern";
            case FR -> "Enregistrer";
            case IT -> "Salva";
            case ES -> "Guardar";
        };
    }

    public String unsavedDiscard() {
        return switch (l()) {
            case EN -> "Don't Save";
            case TR -> "Kaydetme";
            case DE -> "Nicht speichern";
            case FR -> "Ne pas enregistrer";
            case IT -> "Non salvare";
            case ES -> "No guardar";
        };
    }

    public String unsavedCancel() {
        return switch (l()) {
            case EN -> "Cancel";
            case TR -> "İptal";
            case DE -> "Abbrechen";
            case FR -> "Annuler";
            case IT -> "Annulla";
            case ES -> "Cancelar";
        };
    }

    public String errorTitle() {
        return switch (l()) {
            case EN -> "Error";
            case TR -> "Hata";
            case DE -> "Fehler";
            case FR -> "Erreur";
            case IT -> "Errore";
            case ES -> "Error";
        };
    }

    public String errorOpen() {
        return switch (l()) {
            case EN -> "Could not open file";
            case TR -> "Dosya açılamadı";
            case DE -> "Datei konnte nicht geöffnet werden";
            case FR -> "Impossible d'ouvrir le fichier";
            case IT -> "Impossibile aprire il file";
            case ES -> "No se pudo abrir el archivo";
        };
    }

    public String errorSave() {
        return switch (l()) {
            case EN -> "Could not save file";
            case TR -> "Dosya kaydedilemedi";
            case DE -> "Datei konnte nicht gespeichert werden";
            case FR -> "Impossible d'enregistrer le fichier";
            case IT -> "Impossibile salvare il file";
            case ES -> "No se pudo guardar el archivo";
        };
    }

    public String appName() {
        return "JTypeWriter";
    }

    public String emptyFile() {
        return switch (l()) {
            case EN -> "Empty File";
            case TR -> "Boş Dosya";
            case DE -> "Leere Datei";
            case FR -> "Fichier vide";
            case IT -> "File vuoto";
            case ES -> "Archivo vacío";
        };
    }
}
