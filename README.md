# JTypeWriter

A distraction-free text editor built with Java 25, Spring Boot 3.4, and JavaFX 24.

---

## English

### Overview

JTypeWriter is a minimal desktop text editor designed for focused writing. It removes all distractions and provides a clean, typewriter-inspired experience.

### Features

- **Minimal Interface** — Full-screen editor with no visual clutter. Toolbar toggles with `Ctrl+T`.
- **YOLO Mode** (`Ctrl+Shift+Y`) — Disables backspace and delete keys. Write forward only.
- **Typewriter Sound** — Authentic mechanical typewriter keystroke audio, toggleable from the toolbar.
- **Themes** — Dark, Light, and Sepia. Cycle with `Ctrl+Shift+T`.
- **Live Statistics** — Word count, character count (with/without spaces), line count, keystroke count, WPM, and session duration displayed in the bottom bar.
- **Date & Time** — Current date and time shown in the bottom bar.
- **Language Support** — English, Turkish, German, French, Italian, Spanish. Auto-detects system locale; toggle with the toolbar language button. UI strings loaded via `ResourceBundle` from `.properties` files.
- **Auto-save Prompt** — On close, prompts to save unsaved changes.
- **Font Size Control** — Adjustable font size (`Ctrl++` / `Ctrl+-` / `Ctrl+0`).

### Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+T` | Toggle toolbar |
| `Ctrl+Shift+T` | Cycle theme (Dark/Light/Sepia) |
| `Ctrl+Shift+Y` | YOLO mode (no deletion) |
| `Ctrl+Shift+C` | Cycle text color |
| `Ctrl++` | Increase font size |
| `Ctrl+-` | Decrease font size |
| `Ctrl+0` | Reset font size |
| `Ctrl+O` | Open file |
| `Ctrl+S` | Save / Save As |
| `Escape` | Exit fullscreen |

### Tech Stack

| Component | Version |
|---|---|
| Java | 25 |
| Spring Boot | 3.4.4 |
| JavaFX | 24 |
| Build Tool | Maven |
| UI Layout | FXML + CSS |

### Prerequisites

- JDK 25+
- Maven 3.9+

### Build & Run

```bash
# Build
mvn clean package

# Run
java -jar target/jtypewriter-0.0.1-SNAPSHOT.jar

# Or via Maven plugin
mvn javafx:run
```

### CI/CD

This project uses **GitHub Actions** for continuous integration.

**Workflow:** `.github/workflows/maven.yml`

| Trigger | Event |
|---|---|
| `push` | `main` branch |
| `pull_request` | `main` branch |

The workflow runs on `ubuntu-latest` with JDK 25 (Temurin) and executes:

```bash
mvn -B package --file pom.xml
```

Build artifacts (JAR) are available as workflow outputs. Maven dependencies are cached to speed up subsequent runs.

### Project Structure

```
src/main/java/org/mesutormanli/jtypewriter/
├── JTypeWriterApp.java              # Spring Boot entry point
├── SpringBootFxApplication.java     # JavaFX Application bridge
├── audio/
│   └── TypewriterSound.java         # Keystroke audio player
├── config/
│   └── AppConfig.java               # Spring configuration
├── locale/
│   ├── Language.java                # EN / TR enum
│   ├── LocaleManager.java           # Language state & observers
│   └── Messages.java                # ResourceBundle-backed UI strings
├── resources/
│   ├── application.properties
│   ├── audio/
│   ├── css/
│   ├── fxml/
│   └── messages*.properties         # 6 dil için localization files
├── service/
│   └── FileService.java             # File open/save operations
├── stats/
│   └── SessionStats.java            # Word/char/keystroke counters
└── ui/
    ├── MainController.java          # Main controller & shortcuts
    ├── MainStage.java               # Stage setup
    ├── WelcomeDialog.java           # Start-up shortcut guide
    ├── component/
    │   ├── EditorArea.java          # Custom TextArea (YOLO)
    │   ├── StatsBar.java            # Bottom stats bar with clock
    │   └── ToolbarView.java         # Toggleable toolbar
    └── theme/
        ├── Theme.java               # LIGHT / DARK / SEPIA enum
        └── ThemeManager.java        # CSS theme switcher
```

### Audio Credits

The typewriter keystroke sound is sourced from [typewriter.vim](https://github.com/AndrewRadev/typewriter.vim) (MIT license), originally collected in [lifepillar/typewriter-sounds](https://github.com/lifepillar/typewriter-sounds).

### Author

**Mesut ORMANLI** — [mesutormanli@gmail.com](mailto:mesutormanli@gmail.com)

### License

This project is licensed under the GNU General Public License v3.0 — see the [LICENSE](LICENSE) file for details.

---

## Türkçe

### Genel Bakış

JTypeWriter, odaklanmış yazma deneyimi için tasarlanmış minimal bir masaüstü metin editörüdür. Tüm dikkat dağıtıcı unsurları ortadan kaldırır ve temiz, daktilo ilhamlı bir deneyim sunar.

### Özellikler

- **Minimal Arayüz** — Görsel karmaşa olmayan tam ekran editör. Toolbar `Ctrl+T` ile açılıp kapanır.
- **YOLO Modu** (`Ctrl+Shift+Y`) — Backspace ve delete tuşlarını devre dışı bırakır. Sadece ileriye doğru yazın.
- **Daktilo Sesi** — Gerçek mekanik daktilo tuş vuruşu sesi, toolbar'dan açılıp kapatılabilir.
- **Temalar** — Koyu, Açık ve Sepya. `Ctrl+Shift+T` ile döngüsel geçiş.
- **Canlı İstatistikler** — Kelime sayısı, karakter sayısı (boşluksuz), satır sayısı, tuş vuruşu sayısı, WPM ve oturum süresi alt çubukta görüntülenir.
- **Tarih & Saat** — Geçerli tarih ve saat alt çubukta gösterilir.
- **Dil Desteği** — İngilizce, Türkçe, Almanca, Fransızca, İtalyanca, İspanyolca. Sistem dilini otomatik algılar; toolbar'daki dil butonu ile değiştirilir. UI metinleri `ResourceBundle` ile `.properties` dosyalarından yüklenir.
- **Kapatmada Kaydetme Uyarısı** — Kapatılırken kaydedilmemiş değişiklikler varsa kullanıcıyı uyarır.
- **Font Boyutu Ayarı** — Ayarlanabilir font boyutu (`Ctrl++` / `Ctrl+-` / `Ctrl+0`).

### Klavye Kısayolları

| Kısayol | Eylem |
|---|---|
| `Ctrl+T` | Toolbar aç/kapa |
| `Ctrl+Shift+T` | Tema değiştir (Koyu/Açık/Sepya) |
| `Ctrl+Shift+Y` | YOLO modu (silme devre dışı) |
| `Ctrl+Shift+C` | Metin rengini değiştir |
| `Ctrl++` | Font büyüt |
| `Ctrl+-` | Font küçült |
| `Ctrl+0` | Font sıfırla |
| `Ctrl+O` | Dosya aç |
| `Ctrl+S` | Kaydet / Farklı kaydet |
| `Escape` | Tam ekrandan çık |

### Teknoloji Altyapısı

| Bileşen | Sürüm |
|---|---|
| Java | 25 |
| Spring Boot | 3.4.4 |
| JavaFX | 24 |
| Derleme Aracı | Maven |
| UI Düzeni | FXML + CSS |

### Gereksinimler

- JDK 25+
- Maven 3.9+

### Derleme & Çalıştırma

```bash
# Derleme
mvn clean package

# Çalıştırma
java -jar target/jtypewriter-0.0.1-SNAPSHOT.jar

# Veya Maven plugin ile
mvn javafx:run
```

### CI/CD

Bu proje sürekli entegrasyon için **GitHub Actions** kullanmaktadır.

**Workflow:** `.github/workflows/maven.yml`

| Tetikleyici | Olay |
|---|---|
| `push` | `main` branch |
| `pull_request` | `main` branch |

Workflow, `ubuntu-latest` üzerinde JDK 25 (Temurin) ile çalışır:

```bash
mvn -B package --file pom.xml
```

Derleme çıktıları (JAR) workflow çıktısı olarak indirilebilir. Maven bağımlılıkları sonraki çalıştırmalar için önbelleğe alınır.

### Proje Yapısı

```
src/main/java/org/mesutormanli/jtypewriter/
├── JTypeWriterApp.java              # Spring Boot giriş noktası
├── SpringBootFxApplication.java     # JavaFX Application köprüsü
├── audio/
│   └── TypewriterSound.java         # Tuş sesi oynatıcı
├── config/
│   └── AppConfig.java               # Spring yapılandırması
├── locale/
│   ├── Language.java                # EN / TR enum
│   ├── LocaleManager.java           # Dil durumu ve dinleyiciler
│   └── Messages.java                # ResourceBundle tabanlı UI metinleri
├── resources/
│   ├── application.properties
│   ├── audio/
│   ├── css/
│   ├── fxml/
│   └── messages*.properties         # 6 dil i\u00e7in localization dosyalar\u0131
├── service/
│   └── FileService.java             # Dosya aç/kaydet işlemleri
├── stats/
│   └── SessionStats.java            # Kelime/karakter/tuş sayaçları
└── ui/
    ├── MainController.java          # Ana kontrolör ve kısayollar
    ├── MainStage.java               # Pencere ayarları
    ├── WelcomeDialog.java           # Başlangıç kısayol rehberi
    ├── component/
    │   ├── EditorArea.java          # Özel TextArea (YOLO)
    │   ├── StatsBar.java            # Alt istatistik çubuğu (saatli)
    │   └── ToolbarView.java         # Aç/kapatılabilir toolbar
    └── theme/
        ├── Theme.java               # LIGHT / DARK / SEPIA enum
        └── ThemeManager.java        # CSS tema yöneticisi
```

### Ses Katkısı

Daktilo tuş sesi [typewriter.vim](https://github.com/AndrewRadev/typewriter.vim) projesinden alınmıştır (MIT lisansı), [lifepillar/typewriter-sounds](https://github.com/lifepillar/typewriter-sounds) deposunda toplanmıştır.

### Yazar

**Mesut ORMANLI** — [mesutormanli@gmail.com](mailto:mesutormanli@gmail.com)

### Lisans

Bu proje GNU Genel Kamu Lisansı v3.0 ile lisanslanmıştır — ayrıntılar için [LICENSE](LICENSE) dosyasına bakın.
