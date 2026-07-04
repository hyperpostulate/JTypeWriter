# JTypeWriter

**A distraction-free text editor** built with JavaFX and Spring Boot, designed for focused and productive writing.

---

## ✨ Features

- **Distraction-Free Interface** – Minimal UI that keeps you focused on your writing. The toolbar is hidden by default and can be toggled with a single shortcut.
- **Multiple Themes** – Choose from Dark, Light, and Sepia themes to match your environment and mood.
- **YOLO Mode** – A focus mode that prevents deletion (Backspace, Delete, Cut). Forces you to keep writing forward — perfect for rough drafts and stream-of-consciousness writing.
- **Typewriter Sound Effects** – Each keystroke plays a mechanical typewriter sound for an immersive writing experience. Can be toggled on/off.
- **Real-Time Statistics** – Track your progress live: word count, character count (with/without spaces), line count, total keystrokes, session duration, and WPM (words per minute).
- **Live Clock** – Displays current date and time in the status bar. The status bar size can be cycled (Small / Medium / Large) by double-clicking.
- **Text Color Customization** – Choose from a palette of muted colors: Default, Sage, Lavender, Sand, Steel, Rose.
- **Font Size Control** – Increase, decrease, or reset font size (range: 10px – 48px, default: 16px).
- **File Operations** – Open, Save, and Save As for `.txt` files. Unsaved changes prompt on exit.
- **Undo / Redo** – Full undo/redo support.
- **Multi-Language Support** – Interface available in English, Turkish, German, French, Italian, and Spanish. Switch languages on the fly.
- **Keyboard Shortcuts** – All major features accessible via keyboard.

---

## 🖥️ Screenshots

> *(Add screenshots here)*

---

## 🚀 Getting Started

### Prerequisites

- **JDK 25** or later
- **Maven 3.9+**

### Build & Run

```bash
# Clone the repository
git clone https://github.com/mesutormanli/JTypeWriter.git
cd JTypeWriter

# Build the project
mvn clean package

# Run the application
mvn javafx:run
```

Or run the packaged JAR:

```bash
java -jar target/jtypewriter-0.0.1-SNAPSHOT.jar
```

---

## ⌨️ Keyboard Shortcuts

| Shortcut              | Action                          |
|-----------------------|---------------------------------|
| `Ctrl + T`            | Toggle Toolbar                  |
| `Ctrl + Shift + T`    | Cycle Theme (Dark/Light/Sepia)  |
| `Ctrl + Shift + Y`    | Toggle YOLO Mode                |
| `Ctrl + Shift + C`    | Cycle Text Color                |
| `Ctrl + +`            | Increase Font Size              |
| `Ctrl + -`            | Decrease Font Size              |
| `Ctrl + 0`            | Reset Font Size                 |
| `Ctrl + Z`            | Undo                            |
| `Ctrl + Y`            | Redo                            |
| `Ctrl + O`            | Open File                       |
| `Ctrl + S`            | Save / Save As                  |
| `Escape`              | Exit Fullscreen                 |

---

## 🏗️ Architecture

```
org.mesutormanli.jtypewriter
├── JTypeWriterApp.java              # Spring Boot main application entry point
├── SpringBootFxApplication.java     # JavaFX Application bootstrap (launches Spring context + JavaFX stage)
│
├── audio/
│   └── TypewriterSound.java         # AudioClip-based keystroke sound playback
│
├── config/
│   └── AppConfig.java               # Spring configuration (editor font bean)
│
├── locale/
│   ├── Language.java                # Supported language enum (EN, TR, DE, FR, IT, ES)
│   ├── LocaleManager.java           # Language state management with listener support
│   └── Messages.java                # i18n resource bundle accessor
│
├── service/
│   └── FileService.java             # File open/save/unsaved-changes logic
│
├── stats/
│   └── SessionStats.java            # Real-time writing session statistics
│
└── ui/
    ├── MainController.java          # FXML controller — wiring, keyboard shortcuts, toolbar logic
    ├── MainStage.java               # Stage configuration (maximized, close handler)
    ├── WelcomeDialog.java           # Welcome dialog with shortcut reference
    ├── AboutDialog.java             # About dialog with author info
    │
    ├── component/
    │   ├── EditorArea.java          # Custom TextArea with key handling, YOLO mode, sound
    │   ├── StatsBar.java            # Bottom status bar with live stats and clock
    │   └── ToolbarView.java         # Top toolbar with all action buttons
    │
    └── theme/
        ├── Theme.java               # Theme enum (DARK, LIGHT, SEPIA)
        ├── ThemeManager.java        # Theme application and cycling
        ├── TextColor.java           # Text color enum (Default, Sage, Lavender, Sand, Steel, Rose)
        ├── TextColorManager.java    # Text color state and cycling
        └── FontSizeManager.java     # Font size state with min/max clamping
```

### Key Design Decisions

- **Spring Boot + JavaFX Integration**: The JavaFX `Application` class launches a Spring Boot `ConfigurableApplicationContext`, allowing Spring-managed beans (via `@Component`) to be injected into JavaFX controllers and components through an FXML controller factory.
- **Component-Based UI**: `EditorArea`, `ToolbarView`, and `StatsBar` are custom `@Component`-annotated JavaFX nodes that are wired together by `MainController`.
- **Observer Pattern for Locale Changes**: `LocaleManager` maintains a listener list; `Messages` and `ToolbarView` register callbacks to refresh UI text when the language changes.
- **CSS Theme System**: Three theme stylesheets (`dark.css`, `light.css`, `sepia.css`) define CSS custom properties (lookup colors). `base.css` contains shared structural styles that reference these variables, enabling seamless theme switching at runtime.

---

## 🧪 Testing

Tests use **JUnit 5** with **Mockito** for mocking.

```bash
mvn test
```

**Test coverage includes:**
- `SessionStatsTest` – Word count, character count, line count, keystroke tracking, and reset behavior.
- `FileServiceTest` – Unsaved changes detection.
- `JTypeWriterAppTests` – Application entry point verification.

---

## 🌍 Internationalization

Supported languages and their resource bundles:

| Language   | Code | File                    |
|------------|------|-------------------------|
| English    | EN   | `messages.properties`   |
| Türkçe     | TR   | `messages_tr.properties`|
| Deutsch    | DE   | `messages_de.properties`|
| Français   | FR   | `messages_fr.properties`|
| Italiano   | IT   | `messages_it.properties`|
| Español    | ES   | `messages_es.properties`|

The language is detected from the system locale on first launch and can be cycled at runtime via the toolbar language button.

---

## 🛠️ Technology Stack

| Technology       | Version     |
|------------------|-------------|
| Java             | 25          |
| JavaFX           | 26.0.1      |
| Spring Boot      | 4.1.0       |
| Maven            | 3.9+        |
| JUnit            | 5           |
| Mockito          | (bundled)   |

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Mesut ORMANLI**  
✉️ mesutormanli@gmail.com

---

---

# JTypeWriter

**Dikkat dağıtmayan metin editörü** — JavaFX ve Spring Boot ile geliştirilmiş, odaklanarak yazma deneyimi için tasarlanmış bir masaüstü uygulaması.

---

## ✨ Özellikler

- **Dikkat Dağıtmayan Arayüz** – Minimal tasarım sayesinde yazınıza odaklanın. Araç çubuğu varsayılan olarak gizlidir, tek kısayol ile açılıp kapatılabilir.
- **Birden Çok Tema** – Koyu, Açık ve Sepya temalar arasında geçiş yaparak ortamınıza ve ruh halinize uygun görünümü seçin.
- **YOLO Modu** – Silme işlemlerini (Backspace, Delete, Kes) devre dışı bırakan odak modu. Sadece ileriye doğru yazmaya devam etmenizi sağlar — taslak yazımı ve bilinç akışı yazma için idealdir.
- **Daktilo Ses Efektleri** – Her tuş vuruşunda mekanik daktilo sesi çalar. İsterseniz açılıp kapatılabilir.
- **Gerçek Zamanlı İstatistikler** – Kelime sayısı, karakter sayısı (boşluklu/boşluksuz), satır sayısı, toplam tuş vuruşu, oturum süresi ve WPM (dakikadaki kelime sayısı) gibi metrikleri anlık olarak takip edin.
- **Canlı Saat** – Durum çubuğunda güncel tarih ve saat görüntülenir. Durum çubuğu boyutu çift tıklayarak değiştirilebilir (Küçük / Orta / Büyük).
- **Metin Rengi Özelleştirme** – Varsayılan, Sage, Lavanta, Kum, Çelik, Gül renkleri arasından seçim yapın.
- **Yazı Boyutu Kontrolü** – Yazı boyutunu artırın, azaltın veya sıfırlayın (10px – 48px aralığı, varsayılan: 16px).
- **Dosya İşlemleri** – `.txt` dosyaları açma, kaydetme ve farklı kaydetme. Çıkışta kaydedilmemiş değişiklik uyarısı.
- **Geri Al / İleri Al** – Tam geri al/ileri al desteği.
- **Çoklu Dil Desteği** – İngilizce, Türkçe, Almanca, Fransızca, İtalyanca ve İspanyolca dillerinde kullanılabilir. Çalışma anında dil değiştirilebilir.
- **Klavye Kısayolları** – Tüm önemli özelliklere klavyeden erişim.

---

## 🚀 Başlarken

### Gereksinimler

- **JDK 25** veya üzeri
- **Maven 3.9+**

### Derleme ve Çalıştırma

```bash
# Depoyu klonlayın
git clone https://github.com/mesutormanli/JTypeWriter.git
cd JTypeWriter

# Projeyi derleyin
mvn clean package

# Uygulamayı çalıştırın
mvn javafx:run
```

Veya paketlenmiş JAR dosyasını çalıştırın:

```bash
java -jar target/jtypewriter-0.0.1-SNAPSHOT.jar
```

---

## ⌨️ Klavye Kısayolları

| Kısayol               | Eylem                             |
|------------------------|-----------------------------------|
| `Ctrl + T`             | Araç çubuğunu aç/kapa             |
| `Ctrl + Shift + T`     | Tema değiştir (Koyu/Açık/Sepya)   |
| `Ctrl + Shift + Y`     | YOLO modunu aç/kapa               |
| `Ctrl + Shift + C`     | Metin rengini değiştir            |
| `Ctrl + +`             | Yazı boyutunu büyüt               |
| `Ctrl + -`             | Yazı boyutunu küçült              |
| `Ctrl + 0`             | Yazı boyutunu sıfırla             |
| `Ctrl + Z`             | Geri al                           |
| `Ctrl + Y`             | İleri al                          |
| `Ctrl + O`             | Dosya aç                          |
| `Ctrl + S`             | Kaydet / Farklı kaydet            |
| `Escape`               | Tam ekrandan çık                  |

---

## 🏗️ Mimari

```
org.mesutormanli.jtypewriter
├── JTypeWriterApp.java              # Spring Boot ana uygulama giriş noktası
├── SpringBootFxApplication.java     # JavaFX Application başlatıcı (Spring context + JavaFX stage)
│
├── audio/
│   └── TypewriterSound.java         # AudioClip tabanlı tuş sesi oynatıcı
│
├── config/
│   └── AppConfig.java               # Spring konfigürasyonu (editor font bean)
│
├── locale/
│   ├── Language.java                # Desteklenen diller enum'u (EN, TR, DE, FR, IT, ES)
│   ├── LocaleManager.java           # Dil durum yönetimi ve dinleyici desteği
│   └── Messages.java                # Çoklu dil destek dosyası erişimcisi
│
├── service/
│   └── FileService.java             # Dosya aç/kaydet/kaydedilmemiş değişiklik mantığı
│
├── stats/
│   └── SessionStats.java            # Gerçek zamanlı yazma oturumu istatistikleri
│
└── ui/
    ├── MainController.java          # FXML denetleyici — bağlantılar, kısayollar, toolbar mantığı
    ├── MainStage.java               # Pencere yapılandırması (büyütülmüş, kapatma dinleyicisi)
    ├── WelcomeDialog.java           # Kısayol referansı içeren karşılama penceresi
    ├── AboutDialog.java             # Yazar bilgisi içeren hakkında penceresi
    │
    ├── component/
    │   ├── EditorArea.java          # Tuş işleme, YOLO modu, ses içeren özel TextArea
    │   ├── StatsBar.java            # Canlı istatistik ve saat içeren alt durum çubuğu
    │   └── ToolbarView.java         # Tüm eylem düğmelerini içeren üst araç çubuğu
    │
    └── theme/
        ├── Theme.java               # Tema enum'u (KOYU, AÇIK, SEPYA)
        ├── ThemeManager.java        # Tema uygulama ve döngüsel geçiş
        ├── TextColor.java           # Metin rengi enum'u
        ├── TextColorManager.java    # Metin rengi durumu ve döngüsel geçiş
        └── FontSizeManager.java     # Yazı boyutu durumu (min/max sınırlamalı)
```

### Temel Tasarım Kararları

- **Spring Boot + JavaFX Entegrasyonu**: JavaFX `Application` sınıfı, bir Spring Boot `ConfigurableApplicationContext` başlatır. `@Component` ile işaretlenmiş bean'ler, FXML controller factory aracılığıyla JavaFX bileşenlerine enjekte edilir.
- **Bileşen Tabanlı UI**: `EditorArea`, `ToolbarView` ve `StatsBar`, `@Component` ile işaretlenmiş özel JavaFX düğümleridir ve `MainController` tarafından bir araya getirilir.
- **Dil Değişikliklerinde Gözlemci Deseni**: `LocaleManager` bir dinleyici listesi tutar; `Messages` ve `ToolbarView`, dil değiştiğinde UI metinlerini güncellemek için geri çağırım kaydeder.
- **CSS Tema Sistemi**: Üç tema stil dosyası (`dark.css`, `light.css`, `sepia.css`), CSS özel değişkenlerini (lookup colors) tanımlar. `base.css` bu değişkenlere referans veren ortak yapısal stilleri içerir ve çalışma anında kesintisiz tema geçişine olanak tanır.

---

## 🧪 Test

Testler **JUnit 5** ve **Mockito** ile yazılmıştır.

```bash
mvn test
```

**Test kapsamı:**
- `SessionStatsTest` – Kelime sayısı, karakter sayısı, satır sayısı, tuş vuruşu takibi ve sıfırlama davranışı.
- `FileServiceTest` – Kaydedilmemiş değişiklik tespiti.
- `JTypeWriterAppTests` – Uygulama giriş noktası doğrulaması.

---

## 🌍 Uluslararasılaştırma

Desteklenen diller ve kaynak dosyaları:

| Dil         | Kod  | Dosya                    |
|-------------|------|--------------------------|
| English     | EN   | `messages.properties`    |
| Türkçe      | TR   | `messages_tr.properties` |
| Deutsch     | DE   | `messages_de.properties` |
| Français    | FR   | `messages_fr.properties` |
| Italiano    | IT   | `messages_it.properties` |
| Español     | ES   | `messages_es.properties` |

Dil, ilk açılışta sistem diline göre algılanır ve çalışma anında araç çubuğundaki dil düğmesi ile değiştirilebilir.

---

## 🛠️ Kullanılan Teknolojiler

| Teknoloji        | Sürüm        |
|------------------|--------------|
| Java             | 25           |
| JavaFX           | 26.0.1       |
| Spring Boot      | 4.1.0        |
| Maven            | 3.9+         |
| JUnit            | 5            |
| Mockito          | (dahili)     |

---

## 📄 Lisans

Bu proje **GNU Genel Kamu Lisansı v3.0** ile lisanslanmıştır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

---

## 👤 Yazar

**Mesut ORMANLI**  
✉️ mesutormanli@gmail.com
