# JTypeWriter

[![Java CI](https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml/badge.svg)](https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml) [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

A distraction-free text editor built with JavaFX and Spring Boot. Designed for focused writing with typewriter sound effects, real-time statistics, multiple themes, and multi-language support.

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Build & Run](#build--run)
- [CI/CD](#cicd)
- [Architecture](#architecture)
- [Features](#features)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Themes](#themes)
- [Supported Languages](#supported-languages)
- [Testing](#testing)
- [License](#license)
- [Developer](#developer)
- [Contributing](#contributing)

---

## Requirements

| Requirement | Version |
|-------------|---------|
| Java        | 25+     |
| Maven       | 3.8+    |

---

## Installation

### Manual Build

```bash
git clone https://github.com/hyperpostulate/JTypeWriter.git
cd JTypeWriter
mvn clean install
```

---

## Build & Run

```bash
mvn clean javafx:run          # Build and launch the application
mvn clean test                # Run all tests
mvn clean package             # Build + tests + package as JAR
mvn test -Dtest=SessionStatsTest     # Run a single test class
mvn test -Dtest=FileServiceTest      # Run a single test class
```

---

## CI/CD

GitHub Actions workflow (`.github/workflows/maven.yml`):

- **Trigger**: Runs on every push and pull request to `main`
- **Environment**: Ubuntu-latest
- **JDK**: Temurin 25
- **Command**: `mvn -B package`

---

## Architecture

```
org.mesutormanli.jtypewriter
├── audio/
│   └── TypewriterSound.java              # Typewriter keystroke sound effect
├── config/
│   └── AppConfig.java                    # Spring configuration (editor font)
├── locale/
│   ├── Language.java                     # Supported languages enum
│   ├── LocaleManager.java                # Language switching + listener support
│   └── Messages.java                     # ResourceBundle-based i18n accessor
├── service/
│   ├── DialogService.java                # FileChooser & Alert dialog management
│   └── FileService.java                  # File I/O operations (read/write)
├── stats/
│   └── SessionStats.java                 # Writing session statistics (WPM, counts)
├── ui/
│   ├── MainController.java               # Root layout controller
│   ├── MainStage.java                    # Stage configuration (fullscreen, close)
│   ├── WelcomeDialog.java                # Welcome screen with shortcuts
│   ├── AboutDialog.java                  # About dialog with author info
│   ├── KeyboardShortcutHandler.java      # Centralized keyboard shortcut handling
│   └── component/
│       ├── EditorArea.java               # Pure TextArea wrapper
│       ├── EditorKeyHandler.java         # Keystroke sound + stats tracking
│       ├── StatsBar.java                 # Bottom status bar (clock + stats)
│       ├── ToolbarView.java              # Top toolbar (buttons + labels)
│       └── ToolbarState.java             # YOLO mode + sound on/off state
│   └── theme/
│       ├── Theme.java                    # Theme enum (Light/Dark/Sepia)
│       ├── ThemeManager.java             # CSS stylesheet loading & switching
│       ├── FontSizeManager.java          # Editor font size state
│       ├── TextColorManager.java         # Text color cycling state
│       └── TextColor.java                # Text color enum with CSS values
└── JTypeWriterApp.java                   # Main entry point
    SpringBootFxApplication.java          # JavaFX + Spring Boot bridge
```

### Core Components

| Component | Description |
|-----------|-------------|
| `EditorArea` | Pure TextArea wrapper with YOLO mode support |
| `EditorKeyHandler` | Handles keystroke sound effects and session stats tracking |
| `KeyboardShortcutHandler` | Centralized keyboard shortcut routing |
| `ToolbarView` | Top toolbar with all editor controls |
| `ToolbarState` | Manages YOLO mode and sound on/off state |
| `FileService` | File I/O operations (read/write) |
| `DialogService` | UI dialog management (FileChooser, Alert) |
| `ThemeManager` | CSS theme loading and switching |
| `SessionStats` | Real-time writing statistics (WPM, word/char/line count) |
| `TypewriterSound` | Typewriter keystroke audio playback |
| `LocaleManager` | Multi-language support with change listeners |
| `Messages` | Resource bundle-based message accessor |

### Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot Starter | 4.1.0 | Application framework |
| JavaFX Controls | 26.0.1 | UI components |
| JavaFX FXML | 26.0.1 | FXML loading |
| JavaFX Media | 26.0.1 | Audio playback |
| JUnit Jupiter | 5.11.0 | Test framework |
| Mockito | 5.x | Test mocking |

---

## Features

### Typewriter Sound Effects

Authentic typewriter keystroke sound on every key press. Can be toggled on/off via toolbar or shortcut.

### YOLO Mode

"YOLO Mode" (You Only Live Once) prevents accidental deletion. When enabled, Backspace, Delete, and Cut are disabled. Perfect for first-draft writing where you push forward without looking back.

### Real-Time Statistics

Live stats bar showing:
- Word count, character count (with/without spaces), line count
- Total keystrokes
- Session duration (HH:MM:SS)
- Words per minute (WPM)

### Multiple Themes

Three built-in themes:
- **Dark** - Default, easy on the eyes
- **Light** - Clean and bright
- **Sepia** - Warm, paper-like feel

### Text Colors

Five accent colors for text: Default, Sage, Lavender, Sand, Steel, Rose.

### Multi-Language Support

6 languages: English, Turkish, German, French, Italian, Spanish. UI updates instantly on language switch.

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+T` | Toggle toolbar |
| `Ctrl+Shift+T` | Cycle theme (Dark/Light/Sepia) |
| `Ctrl+Shift+Y` | Toggle YOLO mode |
| `Ctrl+Shift+C` | Cycle text color |
| `Ctrl++` | Increase font size |
| `Ctrl+-` | Decrease font size |
| `Ctrl+0` | Reset font size to 16px |
| `Ctrl+Z` | Undo |
| `Ctrl+Y` | Redo |
| `Ctrl+O` | Open file |
| `Ctrl+S` | Save file |
| `Ctrl+Shift+S` | Save file as |
| `Escape` | Exit fullscreen |

---

## Themes

CSS files are located in `src/main/resources/css/`:

| Theme | File | Description |
|-------|------|-------------|
| Base | `base.css` | Shared styles across all themes |
| Dark | `dark.css` | Dark background, light text |
| Light | `light.css` | Light background, dark text |
| Sepia | `sepia.css` | Warm sepia-toned background |

---

## Supported Languages

| Language | File | Code |
|----------|------|------|
| English | `messages.properties` | `EN` |
| Turkish | `messages_tr.properties` | `TR` |
| German | `messages_de.properties` | `DE` |
| French | `messages_fr.properties` | `FR` |
| Italian | `messages_it.properties` | `IT` |
| Spanish | `messages_es.properties` | `ES` |

---

## Testing

### Running Tests

```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=SessionStatsTest

# Single test method
mvn test -Dtest=SessionStatsTest#wordCountIsCorrect
```

### Test Classes

| Test Class | Coverage |
|------------|----------|
| `SessionStatsTest` | Word count, char count, line count, keystrokes, reset |
| `FileServiceTest` | Unsaved change detection |
| `JTypeWriterAppTests` | Main method presence |

---

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for details.

---

## Developer

**Mesut ORMANLI**

- Email: [mesutormanli@gmail.com](mailto:mesutormanli@gmail.com)
- GitHub: [@hyperpostulate](https://github.com/hyperpostulate)

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -m 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

---

# JTypeWriter

[![Java CI](https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml/badge.svg)](https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml) [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

JavaFX ve Spring Boot ile oluşturulmuş, dikkat dağıtmayan bir metin editörü. Daktilo ses efektleri, gerçek zamanlı istatistikler, çoklu tema ve çoklu dil desteği ile odaklanarak yazma deneyimi sunar.

---

## İçindekiler

- [Gereksinimler](#gereksinimler)
- [Kurulum](#kurulum)
- [Derleme ve Çalıştırma](#derleme-ve-çalıştırma)
- [CI/CD](#cicd-1)
- [Mimari](#mimari)
- [Özellikler](#özellikler)
- [Klavye Kısayolları](#klavye-kısayolları)
- [Temalar](#temalar)
- [Desteklenen Diller](#desteklenen-diller)
- [Testler](#testler)
- [Lisans](#lisans-1)
- [Geliştirici](#geliştirici)
- [Katkıda Bulunma](#katkıda-bulunma)

---

## Gereksinimler

| Gereksinim | Sürüm |
|------------|-------|
| Java       | 25+   |
| Maven      | 3.8+  |

---

## Kurulum

### Manuel Derleme

```bash
git clone https://github.com/hyperpostulate/JTypeWriter.git
cd JTypeWriter
mvn clean install
```

---

## Derleme ve Çalıştırma

```bash
mvn clean javafx:run          # Uygulamayı derle ve başlat
mvn clean test                # Tüm testleri çalıştır
mvn clean package             # Derleme + test + JAR paketleme
mvn test -Dtest=SessionStatsTest     # Tek test sınıfı
mvn test -Dtest=FileServiceTest      # Tek test sınıfı
```

---

## CI/CD

GitHub Actions iş akışı (`.github/workflows/maven.yml`):

- **Tetikleme**: `main` dalına her push ve pull request'te çalışır
- **Ortam**: Ubuntu-latest
- **JDK**: Temurin 25
- **Komut**: `mvn -B package`

---

## Mimari

```
org.mesutormanli.jtypewriter
├── audio/
│   └── TypewriterSound.java              # Daktilo tuş sesi efekti
├── config/
│   └── AppConfig.java                    # Spring yapılandırması (editör fontu)
├── locale/
│   ├── Language.java                     # Desteklenen diller enum'u
│   ├── LocaleManager.java                # Dil değiştirme + dinleyici desteği
│   └── Messages.java                     # ResourceBundle tabanlı i18n erişici
├── service/
│   ├── DialogService.java                # FileChooser ve Alert dialog yönetimi
│   └── FileService.java                  # Dosya G/Ç işlemleri (okuma/yazma)
├── stats/
│   └── SessionStats.java                 # Yazma oturumu istatistikleri (WPM, sayımlar)
├── ui/
│   ├── MainController.java               # Kök yerleşim denetleyicisi
│   ├── MainStage.java                    # Sahne yapılandırması (tam ekran, kapatma)
│   ├── WelcomeDialog.java                # Kısayollarla hoş geldin ekranı
│   ├── AboutDialog.java                  # Yazar bilgisiyle hakkında dialogu
│   ├── KeyboardShortcutHandler.java      # Merkezi klavye kısayolu yönetimi
│   └── component/
│       ├── EditorArea.java               # Saf TextArea sarmalayıcı
│       ├── EditorKeyHandler.java         # Tuş vuruşu sesi + istatistik takibi
│       ├── StatsBar.java                 # Alt durum çubuğu (saat + istatistikler)
│       ├── ToolbarView.java              # Üst araç çubuğu (düğmeler + etiketler)
│       └── ToolbarState.java             # YOLO modu + ses aç/kapa durumu
│   └── theme/
│       ├── Theme.java                    # Tema enum'u (Açık/Koyu/Sepya)
│       ├── ThemeManager.java             # CSS stil sayfası yükleme ve değiştirme
│       ├── FontSizeManager.java          # Editör font boyutu durumu
│       ├── TextColorManager.java         # Metin rengi döngüsü durumu
│       └── TextColor.java                # CSS değerleriyle metin rengi enum'u
└── JTypeWriterApp.java                   # Ana giriş noktası
    SpringBootFxApplication.java          # JavaFX + Spring Boot köprüsü
```

### Temel Bileşenler

| Bileşen | Açıklama |
|---------|----------|
| `EditorArea` | YOLO modu destekli saf TextArea sarmalayıcı |
| `EditorKeyHandler` | Tuş vuruşu ses efektleri ve oturum istatistiklerini yönetir |
| `KeyboardShortcutHandler` | Merkezi klavye kısayolu yönlendirmesi |
| `ToolbarView` | Tüm editör kontrollerini içeren üst araç çubuğu |
| `ToolbarState` | YOLO modu ve ses aç/kapa durumunu yönetir |
| `FileService` | Dosya G/Ç işlemleri (okuma/yazma) |
| `DialogService` | UI dialog yönetimi (FileChooser, Alert) |
| `ThemeManager` | CSS tema yükleme ve değiştirme |
| `SessionStats` | Gerçek zamanlı yazma istatistikleri (WPM, kelime/karakter/satır sayısı) |
| `TypewriterSound` | Daktilo tuş sesi oynatma |
| `LocaleManager` | Değişiklik dinleyicileriyle çoklu dil desteği |
| `Messages` | Resource bundle tabanlı mesaj erişici |

### Bağımlılıklar

| Bağımlılık | Sürüm | Amaç |
|------------|-------|------|
| Spring Boot Starter | 4.1.0 | Uygulama çerçevesi |
| JavaFX Controls | 26.0.1 | UI bileşenleri |
| JavaFX FXML | 26.0.1 | FXML yükleme |
| JavaFX Media | 26.0.1 | Ses oynatma |
| JUnit Jupiter | 5.11.0 | Test çerçevesi |
| Mockito | 5.x | Test mock'lama |

---

## Özellikler

### Daktilo Ses Efektleri

Her tuş basımında otantik daktilo tuş sesi. Araç çubuğu veya kısayolla açılıp kapatılabilir.

### YOLO Modu

"YOLO Modu" (You Only Live Once) kazara silmeyi önler. Etkinleştirildiğinde, Backspace, Delete ve Cut devre dışı kalır. İlk taslak yazımında geriye bakmadan ileriye giden yazım için mükemmeldir.

### Gerçek Zamanlı İstatistikler

Canlı istatistik çubuğu:
- Kelime sayısı, karakter sayısı (boşluklu/boşluksuz), satır sayısı
- Toplam tuş vuruşu
- Oturum süresi (SS:DD:SS)
- Dakikada kelime (WPM)

### Çoklu Tema

Üç yerleşik tema:
- **Koyu** - Varsayılan, göz yormaz
- **Açık** - Temiz ve parlak
- **Sepya** - Sıcak, kağıt hissi

### Metin Renkleri

Beş vurgu rengi: Varsayılan, Adaçayı, Lavanta, Kum, Çelik, Gül

### Çoklu Dil Desteği

6 dil: İngilizce, Türkçe, Almanca, Fransızca, İtalyanca, İspanyolca. Dil değişikliğinde anında güncellenir.

---

## Klavye Kısayolları

| Kısayol | İşlem |
|---------|-------|
| `Ctrl+T` | Araç çubuğunu aç/kapat |
| `Ctrl+Shift+T` | Tema değiştir (Koyu/Açık/Sepya) |
| `Ctrl+Shift+Y` | YOLO modunu aç/kapat |
| `Ctrl+Shift+C` | Metin rengini değiştir |
| `Ctrl++` | Font boyutunu artır |
| `Ctrl+-` | Font boyutunu azalt |
| `Ctrl+0` | Font boyutunu 16px'e sıfırla |
| `Ctrl+Z` | Geri al |
| `Ctrl+Y` | İleri al |
| `Ctrl+O` | Dosya aç |
| `Ctrl+S` | Dosyayı kaydet |
| `Ctrl+Shift+S` | Farklı kaydet |
| `Escape` | Tam ekrandan çık |

---

## Temalar

CSS dosyaları `src/main/resources/css/` konumundadır:

| Tema | Dosya | Açıklama |
|------|-------|----------|
| Temel | `base.css` | Tüm temalarda paylaşılan stiller |
| Koyu | `dark.css` | Koyu arka plan, açık metin |
| Açık | `light.css` | Açık arka plan, koyu metin |
| Sepya | `sepia.css` | Sıcak sepya tonlu arka plan |

---

## Desteklenen Diller

| Dil | Dosya | Kod |
|-----|-------|-----|
| İngilizce | `messages.properties` | `EN` |
| Türkçe | `messages_tr.properties` | `TR` |
| Almanca | `messages_de.properties` | `DE` |
| Fransızca | `messages_fr.properties` | `FR` |
| İtalyanca | `messages_it.properties` | `IT` |
| İspanyolca | `messages_es.properties` | `ES` |

---

## Testler

### Test Çalıştırma

```bash
# Tüm testler
mvn test

# Tek test sınıfı
mvn test -Dtest=SessionStatsTest

# Tek test metodu
mvn test -Dtest=SessionStatsTest#wordCountIsCorrect
```

### Test Sınıfları

| Test Sınıfı | Kapsam |
|-------------|--------|
| `SessionStatsTest` | Kelime sayısı, karakter sayısı, satır sayısı, tuş vuruşları, sıfırlama |
| `FileServiceTest` | Kaydedilmemiş değişiklik algılama |
| `JTypeWriterAppTests` | Ana metod varlığı |

---

## Lisans

Bu proje GNU General Public License v3.0 altında lisanslanmıştır. Detaylı bilgi için [LICENSE](LICENSE) dosyasına bakın.

---

## Geliştirici

**Mesut ORMANLI**

- E-posta: [mesutormanli@gmail.com](mailto:mesutormanli@gmail.com)
- GitHub: [@hyperpostulate](https://github.com/hyperpostulate)

---

## Katkıda Bulunma

1. Depoyu fork edin
2. Bir özellik dalı oluşturun (`git checkout -b feature/yeni-ozellik`)
3. Değişikliklerinizi Commit edin (`git commit -m 'Yeni özellik ekle'`)
4. Dalı push edin (`git push origin feature/yeni-ozellik`)
5. Bir Pull Request oluşturun
