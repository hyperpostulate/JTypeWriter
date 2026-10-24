<p align="center">
  <img src="src/main/resources/icons/jtypewriter-512.png" alt="JTypeWriter" width="512" height="512">
</p>

<h1 align="center">JTypeWriter</h1>

<p align="center">
  <a href="https://github.com/hyperpostulate/JTypeWriter/blob/main/pom.xml"><img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java 25"></a>
  <img src="https://img.shields.io/badge/Platforms-Linux%20%7C%20Windows%20%7C%20macOS-lightgrey.svg" alt="Platforms">
  <img src="https://img.shields.io/badge/Languages-6-blue.svg" alt="6 languages">
  <a href="https://www.gnu.org/licenses/gpl-3.0"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPL v3"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml/badge.svg" alt="Java CI"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhyperpostulate%2FJTypeWriter%2Fgh-pages%2Fbadges%2Ftests.json" alt="Tests"></a>
</p>

<p align="center">Yaratıcı yazarlar için sessiz, çevrimdışı bir masaüstü metin editörü.</p>
<p align="center">Daktilo gibi ses çıkarır, yazdığını sayar ve o yazıyı kaybetmeyi olabildiğince zorlaştırır.</p>

<p align="center">English version: <a href="README.md">README.md</a></p>

JTypeWriter tek bir düz metin belgesi açar ve sonra geri planda kalır: hesap yok, bulut yok, proje biçimi yok. Metnin, seçtiğin dosyada ve o dosyanın zaten sahip olduğu kodlamada kalır. Java 25 ile Linux, Windows ve macOS'ta çalışır; arayüzü İngilizce, Türkçe, Almanca, Fransızca, İspanyolca ve İtalyanca konuşur.

**Neler var?**

- **Kazara mahvedemeyeceğin bir taslak.** YOLO modu Backspace, Delete, Kes ve Geri Al/İleri Al'ı kapatır; yalnızca ileri yazabilirsin. Her kayıt atomiktir ve dönen yedekler tutar; kaydedilmemiş iş 30 saniyede bir anlık görüntülenir - çökmeden sonra taslak geri teklif edilir ve o iletiyi kapatmak taslağı asla silmez.
- **Yazma hissi.** Bastığın her tuşta daktilo sesi, üç tema (koyu, açık, sepya) ve altı metin rengi; hepsi yazarken değiştirilebilir.
- **Yazdıkça sayılar.** Kelime, boşluklu ve boşluksuz karakter, satır, tuş vuruşu, oturum süresi ve molaları saymayan sürdürülebilir kelime/dakika.
- **Ekranda yalnızca metin.** `F11` tam ekran, `Ctrl+T` araç çubuğunu gizler; pencere başlığı hangi belgenin açık olduğunu ve kaydedilip kaydedilmediğini söyler.
- **Kaldığın yerden devam eden oturum.** Dil, tema, metin rengi, yazı boyutu, son belge ve pencere geometrisi bir sonraki açılışta geri yüklenir.

---

## İçindekiler

- [Özellikler](#özellikler)
- [Klavye Kısayolları](#klavye-kısayolları)
- [Temalar](#temalar)
- [Desteklenen Diller](#desteklenen-diller)
- [Gereksinimler](#gereksinimler)
- [Kurulum](#kurulum)
- [Derleme ve Çalıştırma](#derleme-ve-çalıştırma)
- [CI/CD](#cicd)
- [Mimari](#mimari)
- [Testler](#testler)
- [Lisans](#lisans)
- [Geliştirici](#geliştirici)
- [Katkıda Bulunma](#katkıda-bulunma)

---

## Özellikler

### Daktilo Ses Efektleri

Değiştirici olmayan her tuş basımında otantik daktilo tuş sesi. Araç çubuğundaki Ses açılır listesinden açılıp kapatılabilir.

### YOLO Modu

"YOLO Modu" (You Only Live Once) kazara silmeyi önler. Etkinleştirildiğinde Backspace, Delete, Cut ve Geri Al/İleri Al devre dışı kalır (sağ-tık menüsünde Kes soluk/devre dışı gösterilir; yalnızca Kopyala/Yapıştır/Tümünü Seç kullanılabilir); böylece hiçbir düzenleme belgedeki mevcut karakterleri silemez. İlk taslak yazımında geriye bakmadan ileriye giden yazım için mükemmeldir. Mod, `[YOLO]` başlık rozeti ve araç çubuğundaki YOLO açılır listesi ile görünür tutulur; tema renkleri her iki durumda da değişmez.

### Gerçek Zamanlı İstatistikler

Canlı istatistik çubuğu:
- Kelime sayısı, karakter sayısı (boşluklu/boşluksuz), satır sayısı
- Toplam tuş vuruşu
- Oturum süresi (SS:DD:SS)
- Dakikada kelime (WPM) - aktif yazım süresine dayalı kalıcı hız; molalar değeri düşürmez
- Durum çubuğu metni varsayılan olarak 13 px'tir ve çift tıklandığında küçük, orta ve büyük arasında döner; rengi üç temada da WCAG AA kontrastını (en az 4.5:1) karşılar
- Başarılı bir kayıt, saatini durum çubuğuna yazar (ör. `Kaydedildi 12:04`); sonraki düzenlemede temizlenir ve 30 saniyelik otomatik kayıt taslağı anlık görüntüsünü değil gerçek bir kaydı gösterir

### Tam Ekran ve Araç Çubuğu

`F11` tam ekranı açar/kapatır, `Escape` tam ekrandan çıkar (platformun varsayılan çıkış
ipucu gizlenir). Ekranda yalnızca metin kalsın istiyorsanız araç çubuğunu `Ctrl+T` ile
gizleyebilirsiniz.

Araç çubuğu her pencere genişliğinde tek satır kalır. Altı açılır listenin her biri normalde
değeriyle birlikte bir başlık gösterir ("Tema: Koyu", "Boyut: 16", "Dil: Deutsch"); başlıklı
satır sığmadığında altı başlığın tamamı yalın değere iner ("Koyu", "16", "Deutsch"). Bu,
gerçek yerelleştirilmiş etiketlerden ölçülür; böylece hiçbir şey taşmaz veya kırpılmaz. Beş
düğme hiçbir zaman daralmaz ve görünür kalır. Her araç çubuğu kontrolüne `Tab` ile soldan sağa
görsel sırayla ulaşılır (önce düğmeler, sonra açılır listeler), odaklanan kontrol görünür bir
halka gösterir ve arayüz dilinden gelen, varsa kısayolu da içeren bir tooltip'i vardır; örneğin
"Kaydet (Ctrl+S)".

Araç çubuğunun görünür veya gizli durumu, tam ekran ve durum çubuğunun boyutu yeniden
başlatmalarda korunur. Tam ekranda bırakılan bir pencere yeniden tam ekran açılır; bu
tercih yalnızca çıkışta değil, her değiştirdiğinizde kaydedilir. Tam ekranın geri
yüklenmesi hatırlanan pencere konumu ve boyutuyla çakışmaz: açılışta tam ekran kazanır,
sonradan çıkıldığında hatırlanan konuma dönülür.

### Veri Güvenliği ve Kurtarma

- **Atomik ve dayanıklı kayıt + yedek:** her kayıt geçici dosyaya yazılır, fsync edilir ve yeniden adlandırılarak yerine konur; kayıt ortasında çökme ya da elektrik kesintisi belgeyi yarım bırakamaz. Dosya sistemi atomik taşımayı desteklemiyorsa yeniden adlandırma düz taşımaya düşer; baytlar o anda zaten diskte olur. Üzerine yazmalarda dönen yedekler tutulur (`dosya.bak`, `.bak.1`, `.bak.2`) ve belgenin mevcut dosya izinleri korunur
- **Belge farkındalığı:** pencere başlığı açık belgenin adını ve kaydedilmemiş değişiklik varken bir `*` işaretini gösterir (`[YOLO]` rozetinin yanında). `Ctrl+N` boş, adlandırılmamış bir belge açar (YOLO modu açıkken de çalışır); dosya açarken sorulan kaydet / kaydetme / iptal sorusu burada da sorulur. Belge açmak veya yeni belge oluşturmak geri alma geçmişini sıfırlar, böylece `Ctrl+Z` önceki belgenin metnini asla geri getiremez
- **Otomatik kayıt ve kurtarma:** kaydedilmemiş çalışma 30 saniyede bir ve odak kaybında `~/.jtypewriter/recovery/` altına alınır; çökme sonrası taslak bir sonraki açılışta geri yükleme için teklif edilir. İstem geri yükle / sil / sonra sor seçeneklerini sunar — kapatmak taslağı asla silmez, ertelenen taslak sonraki açılışta yeniden sorulur ve üzerinde çalıştığınız belge için otomatik kayıt devam eder
- **Kodlama güvenliği:** BOM'lu bir dosya, BOM'unun belirttiği kodlamayla çözülür; BOM'suz bir dosya geçerli UTF-8 olmalıdır, aksi halde sessiz bozulma yerine Windows-1254 mü UTF-8 mi olduğu sorulur. Belgeler aynı kodlamayla geri yazılır; yalnızca metin o kodlamaya sığmadığında (örn. Windows-1254 dosyaya yapıştırılan emoji) sunulan UTF-8'e geçişi kabul ederseniz kodlama değişir — temsil edilemeyen karakterler asla sessizce `?` yapılmaz. UTF-8 veya UTF-16 bildiren ancak bazı baytları o kodlama için geçersiz olan bir bayt sıra işareti artık sessizce çözülmez: uygulama bayt sıra işaretinin içerikle eşleşmediğini söyler, bildirilen kodlamayı ve dosyayı adlandırır ve açmadan önce sorar. Yine de açmak bozuk baytları değiştirme karakterleri olarak gösterir ve bunu loglar; iptal etmek mevcut belgeye dokunmaz
- **Hata görünürlüğü:** beklenmeyen hatalar `~/.jtypewriter/logs/` altına yazılır ve sessizce kaybolmak yerine hata diyaloğuyla gösterilir. Fırlatılan istisnalar tam yığın iziyle (neden zinciri dahil) yazılır. Bozuk ayar dosyası sessizce üzerine yazılmaz: `settings.properties.corrupt` olarak kenara alınır ve içeriği tanılama için korunur

### Ayarlar ve Oturum Geri Yükleme

Dil, tema, metin rengi, font boyutu, ses, son belge ve klasörü ile pencere geometrisi
`~/.jtypewriter/settings.properties` dosyasına kaydedilir. Bir sonraki oturum,
öncekinin bittiği yerden başlar: aynı görünüm, aynı belge, aynı pencere. Aradan
geçen sürede silinmiş bir belge sessizce atlanır; bozuk bir ayar dosyası ise
başlangıcı engellemek yerine varsayılanlara döner. YOLO modu bilinçli olarak
*kaydedilmez*: silmeyi engelleyen bir modun bir sonraki açılışta hâlâ etkin olması
istenmez.

### Tek Örnek

Kullanıcı hesabı başına yalnızca bir JTypeWriter örneği çalışır: `~/.jtypewriter/`
altındaki bir kilit dosyası ikinci başlatmayı durdurur; ikinci bir pencere açılmaz,
yerelleştirilmiş bir uyarı penceresi gösterilir ve uygulama kapanır. Çalışan örnek
çökerse kilit işletim sistemi tarafından otomatik bırakılır; çökme sizi dışarıda
bırakmaz.

### Çoklu Tema

Üç yerleşik tema (`Ctrl+Shift+T` bu sırayla geçer; araç çubuğundaki Tema açılır listesinden doğrudan da seçilebilir):
- **Açık** - Temiz ve parlak
- **Koyu** - Varsayılan, göz yormaz
- **Sepya** - Sıcak, kağıt hissi

### Metin Renkleri

Altı metin rengi (bir varsayılan + beş vurgu rengi): Varsayılan, Adaçayı, Lavanta, Kum, Çelik, Gül

### Çoklu Dil Desteği

6 dil: İngilizce, Türkçe, Almanca, Fransızca, İtalyanca, İspanyolca. Dil değiştirildiğinde araç çubuğu, diyaloglar, pencere başlığı ve durum çubuğu anında güncellenir.

---

## Klavye Kısayolları

Kısayollar Windows/Linux'ta `Ctrl`, macOS'ta `⌘` (Cmd) ile çalışır — macOS'ta düz `Ctrl` de kabul edilir. Numpad `+`, `-` ve `0` tuşları ana klavye karşılıklarının eşdeğeri olarak çalışır.

| Kısayol | İşlem |
|---------|-------|
| `Ctrl+T` | Araç çubuğunu aç/kapat |
| `Ctrl+Shift+T` | Üç tema arasında geçiş yap (Açık, Koyu, Sepya) |
| `Ctrl+Shift+Y` | YOLO modunu aç/kapat |
| `Ctrl+Shift+C` | Metin rengini değiştir |
| `Ctrl++` | Font boyutunu artır |
| `Ctrl+-` | Font boyutunu azalt |
| `Ctrl+0` | Font boyutunu 16px'e sıfırla |
| `Ctrl+Z` | Geri al |
| `Ctrl+Shift+Z` | İleri al |
| `Ctrl+Y` | İleri al |
| `Ctrl+N` | Yeni belge |
| `Ctrl+O` | Dosya aç |
| `Ctrl+S` | Dosyayı kaydet |
| `Ctrl+Shift+S` | Farklı kaydet |
| `F11` | Tam ekranı aç/kapat |
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
mvn clean javafx:run                 # Uygulamayı derle ve başlat
mvn clean test                       # Tüm testleri çalıştır
mvn clean package                    # Derleme + test + ince JAR + fat JAR
./scripts/package.sh                 # Derleme + yerel uygulama görüntüsü (releases/<os>/)
./scripts/package.sh --skip-build    # Son derlemeyi kullan, yalnızca yeniden sar
./scripts/package.sh --installer     # Aynı + platform kurulum paketi (.deb / .msi / .dmg)
mvn test -Dtest=SessionStatsTest     # Tek test sınıfı
mvn test -Dtest=FileServiceTest      # Tek test sınıfı
```

Paketleme iki JAR üretir: ince olanı (yalnızca uygulama sınıfları) ve JavaFX'i kendi native
kütüphaneleriyle birlikte taşıyan `jar-with-dependencies`. Bu native kütüphaneler derlemenin
yapıldığı makineden geldiği için fat JAR yalnızca derlendiği platformda çalışır; kolaylık
artefaktıdır, platformlar arası dağıtım için `scripts/package.sh` kullanılır. Geliştirme
girişi `mvn javafx:run` komutudur. `scripts/package.sh`, fat JAR'ı `jpackage` ile gömülü
çalışma zamanlı yerel bir uygulama görüntüsüne sarar; `--installer` seçeneği bu görüntüyü
Linux, Windows ve macOS için platform kurulum paketine (`.deb`, `.msi`, `.dmg`) dönüştürür —
CI iş akışının çalıştırdığı komut da budur. Uygulama sürümünün tek kaynağı `pom.xml`'dir:
script sürümü üretilen artefaktten geri okur ve jpackage kurallarına karşı doğrular
(örneğin macOS `0.x` paket sürümlerini reddeder).

Başlatıcı, pencere ve görev çubuğu simgesi, görselin tek kaynağı olan
`src/main/resources/icons/jtypewriter.svg` dosyasından üretilir: `scripts/generate-icons.py`
PNG setini, Windows `.ico` ve macOS `.icns` dosyalarını yeniden oluşturur (`--check`
güncelliklerini doğrular); `scripts/package.sh` platforma göre doğru biçimi seçip
`jpackage`'a aktarır.

---

## CI/CD

GitHub Actions iş akışı (`.github/workflows/maven.yml`):

- **Tetikleme**: `main` dalına her push ve pull request'te çalışır
- **Ortam**: Ubuntu, Windows ve macOS (build matrisi)
- **JDK**: Temurin 25
- **Komut**: `mvn -B package` (derleme + testler), ardından `scripts/package.sh --skip-build --installer`
- **Diller**: test paketi Türkçe ve Almanca sistem dillerinde de çalışır; böylece yerelleştirme regresyonları (ör. bir dilin sessizce sistem diline çözülmesi) gözden kaçamaz
- **Artefaktlar**: her işletim sistemi için yerel uygulama görüntüsü ve platform kurulum paketi (`.deb` / `.msi` / `.dmg`)

---

## Mimari

```
org.mesutormanli.jtypewriter
├── audio/
│   └── TypewriterSound.java              # Daktilo tuş sesi efekti
├── locale/
│   ├── Language.java                     # Desteklenen diller enum'u
│   ├── LocaleManager.java                # Dil değiştirme + dinleyici desteği
│   └── Messages.java                     # ResourceBundle tabanlı i18n erişici
├── service/
│   ├── AutoSaveService.java              # Dönemsel taslak kaydı + kurtarma teklifi
│   ├── BackupKeeper.java                 # Yazımdan önce dönen .bak yedekleri
│   ├── DialogService.java                # FileChooser ve Alert dialog yönetimi
│   ├── FileService.java                  # Atomik dosya G/Ç (kodlama korumalı)
│   ├── RecoveryStore.java                # Çökme kurtarma taslak deposu
│   └── TextCodec.java                    # Kodlama tespiti + kayıpsız gidiş-dönüş
├── stats/
│   └── SessionStats.java                 # Yazma oturumu istatistikleri (WPM, sayımlar)
├── ui/
│   ├── MainController.java               # Kök yerleşim denetleyicisi
│   ├── MainStage.java                    # Sahne yapılandırması (tam ekran, kapatma)
│   ├── WelcomeDialog.java                # Kısayollarla hoş geldin ekranı
│   ├── AboutDialog.java                  # Yazar bilgisiyle hakkında dialogu
│   ├── KeyboardShortcutHandler.java      # Merkezi klavye kısayolu yönetimi
│   ├── CrashHandler.java                 # Global çökme ağı: log + hata diyaloğu
│   ├── SingleInstanceWarning.java        # "Zaten çalışıyor" uyarı penceresi
│   ├── component/
│   │   ├── EditorArea.java               # YOLO politikasını uygulayan TextArea
│   │   ├── YoloPolicy.java               # YOLO modu düzenleme politikası (saf mantık)
│   │   ├── EditorKeyHandler.java         # Tuş vuruşu sesi + istatistik takibi
│   │   ├── StatsBar.java                 # Alt durum çubuğu (saat + istatistikler)
│   │   ├── ToolbarView.java              # Üst araç çubuğu (dosya/düzenleme düğmeleri + ayar açılır listeleri)
│   │   └── ToolbarState.java             # YOLO modu + ses aç/kapa durumu
│   └── theme/
│       ├── Theme.java                    # Tema enum'u (Açık/Koyu/Sepya)
│       ├── ThemeManager.java             # CSS stil sayfası yükleme ve değiştirme
│       ├── FontSizeManager.java          # Editör font boyutu durumu
│       ├── TextColorManager.java         # Metin rengi döngüsü durumu
│       └── TextColor.java                # CSS değerleriyle metin rengi enum'u
├── JTypeWriterApp.java                   # Ana giriş noktası
├── AppDirs.java                          # Kullanıcı veri/log/kurtarma/ayar dizinleri
├── AppGraph.java                         # Kompozisyon kökü (manuel bağımlılık enjeksiyonu)
├── AppLogging.java                       # Dönen log kurulumu (java.util.logging)
├── AppSettings.java                      # Kalıcı tercihler (settings.properties)
├── AtomicFiles.java                      # Dayanıklı atomik dosya yazımı (fsync + rename)
├── InstanceLock.java                     # Kullanıcı başına örnek kilidi (tek örnek)
└── FxApplication.java                    # JavaFX yaşam döngüsü köprüsü
```

### Temel Bileşenler

| Bileşen | Açıklama |
|---------|----------|
| `EditorArea` | Her düzenlemeyi YOLO politikasından geçiren TextArea alt sınıfı; belgeleri `setDocument` ile yükler |
| `UndoHistoryReset` | Her belge yüklemesinde TextArea'nın geri al/ileri al geçmişini temizler (yansıma ile; JavaFX'te API yok) |
| `YoloPolicy` | YOLO modu düzenleme politikası (silmeyi engeller, eklemeye izin verir) |
| `EditorKeyHandler` | Tuş vuruşu ses efektleri ve oturum istatistiklerini yönetir |
| `StatsBar` | Saati ve canlı oturum istatistiklerini alt çubukta gösterir |
| `KeyboardShortcutHandler` | Merkezi klavye kısayolu yönlendirmesi |
| `ToolbarView` | Tüm editör kontrollerini içeren üst araç çubuğu |
| `ToolbarState` | YOLO modu ve ses aç/kapa durumunu yönetir |
| `FileService` | Atomik dosya G/Ç (geçici dosya + fsync + taşıma), kodlama korumalı |
| `DialogService` | UI dialog yönetimi (FileChooser, Alert) |
| `TextCodec` | Kodlama tespiti (BOM/UTF-8) ve kayıpsız gidiş-dönüşler |
| `BackupKeeper` | Yazımdan önce alınan dönen `.bak` yedekleri |
| `RecoveryStore` | Çökme kurtarma taslakları: `~/.jtypewriter/recovery/` altında oturum başına tek atomik `draft-<uuid>.jtw` dosyası; ayrıca "sonra sor" denen taslak için `pending-<uuid>.jtw` |
| `AutoSaveService` | Dönemsel taslak anlık görüntüleri ve açılışta kurtarma teklifi |
| `CrashHandler` | Global çökme ağı: log + kullanıcıya hata diyaloğu |
| `InstanceLock` | İkinci örneği kullanıcı başına reddeden danışma amaçlı dosya kilidi |
| `SingleInstanceWarning` | "Zaten çalışıyor" uyarısını gösterip kapanan minimal pencere |
| `AppSettings` | `~/.jtypewriter/settings.properties` altında kalıcı tercihler |
| `AtomicFiles` | Dayanıklı atomik dosya yazımı (fsync, rename, dizin flush) |
| `ThemeManager` | CSS tema yükleme ve değiştirme |
| `FontSizeManager` | Editör font boyutu durumu (10-48px) |
| `TextColorManager` | Metin rengi döngüsü durumu |
| `SessionStats` | Gerçek zamanlı yazma istatistikleri (WPM, kelime/karakter/satır sayısı) |
| `TypewriterSound` | Daktilo tuş sesi oynatma |
| `LocaleManager` | Değişiklik dinleyicileriyle çoklu dil desteği |
| `Messages` | Resource bundle tabanlı mesaj erişici |

### Bağımlılıklar

| Bağımlılık | Sürüm | Amaç |
|------------|-------|------|
| JavaFX Controls | 26.0.1 | UI bileşenleri |
| JavaFX FXML | 26.0.1 | FXML yükleme |
| JavaFX Media | 26.0.1 | Ses oynatma |
| JUnit Jupiter | 6.0.3 | Test çerçevesi |
| Mockito | 5.23.0 | Test mock'lama |

JavaFX Graphics ve Base, Controls ile dolaylı olarak gelir. Bağımlılık enjeksiyonu framework'süzdür: nesne grafiğini `AppGraph` elle kurar.

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
| `SessionStatsTest` | Kelime/karakter/satır sayısı, tuş vuruşları, sıfırlama, aktif yazım süreli WPM (durağanlık bozulması regresyonu) |
| `FileServiceTest` | Açık sırasında ve yeni belgede kaydedilmemiş değişiklik akışı (iptal/atla/önce kaydet), atomik yazım, dönen yedekler, kodlama gidiş-dönüşü (temsil edilemeyen karakterler için UTF-8'e geçiş), yeni belgede kodlama sıfırlama, yaşam döngüsü dinleyicileri, kurtarma bağlama |
| `TextCodecTest` | Kodlama tespiti (BOM, sıkı UTF-8), eski kodlama sorusu, kayıpsız kodlama gidiş-dönüşü, sıkı kodlama (temsil edilemeyen karakterler `?` olmak yerine hata verir) |
| `BackupKeeperTest` | Yazımdan önce yedek rotasyonu ve saklama |
| `RecoveryStoreTest` | Çökme kurtarma taslağı yaşam döngüsü (kaydet/geri yükle/sil); oturum başına tek atomik `.jtw` dosyası, en yeni taslağın devralınması, eşzamanlı oturumların izolasyonu, ertelenen taslağın ayrı dosyaya park edilip aktif slotun serbest bırakılması, bozuk başlık, hatalı zaman damgası, kullanılamayan yol ve geçersiz UTF-8'e tolerans |
| `AutoSaveServiceTest` | Belge açıldığında veya yeni belge oluşturulduğunda önceki belgenin bayat kurtarma taslağının silinmesi; ertelenen taslağın temiz çıkışta korunması ve yeni belgenin otomatik kaydını artık susturmaması (regresyon) |
| `YoloPolicyTest` | YOLO modu düzenleme politikası (silme engelli, ekleme serbest, güvenli değiştirme) |
| `KeyboardShortcutHandlerTest` | Kısayol çözümleme; shift hassas birleşimler (Ctrl+Shift+Y / Z / C / T / S), yeni belge için Ctrl+N ve numpad eşdeğerleri |
| `MainControllerTest` | Pencere başlığı oluşturma (uygulama + belge adı, kaydedilmemiş `*` işareti, `[YOLO]` rozeti) |
| `MainStageTest` | Ekran dışı pencere konumu koruması (`intersectsAny`: artık bağlı olmayan monitörlerdeki konumlar) |
| `AppSettingsTest` | Tercihlerin yeniden başlatmada korunması, geri yüklenen araç çubuğu/tam ekran/durum çubuğu boyutu durumu ve bozuk değerlerde varsayılana düşmesi, varsayılanlar, bilinmeyen değerlere ve kullanılamayan yollara tolerans, bozuk ayar dosyasının sessizce ezilmek yerine karantinaya alınması |
| `StatsBarStateTest` | Kalıcı durum çubuğu boyutu: ayardan başlar, yeni kurulum orta boyutta başlar, her döngü adımı kalıcıdır, bozuk değer orta boyuta düşer, eşleme her iki enum'u iki yönde kapsar |
| `FontSizeManagerTest` | Yazı boyutu kademeleri, kademede olmayan boyutun ayarı değiştirmeden listeye eklenmesi, Ctrl+± için kademe adımlama, desteklenen aralığa kısıtlama ve varsayılan boyuta kalıcı sıfırlama |
| `ToolbarOptionListsTest` | Altı araç çubuğu açılır listesi: seçenek sırası, yinelenen seçenek olmaması ve her listenin kendi güncel değerini gösterebilmesi |
| `AtomicFilesTest` | Dayanıklı atomik yazım: içerik bütünlüğü, artık temp dosyası yok, hata sonrası temizlik |
| `MessagesTest` | 6 dilin tamamında i18n anahtar eşitliği, yerelleştirilmiş tema/renk adları, sistem dilinden bağımsız bundle çözümlemesi |
| `DialogServiceTest` | "Farklı Kaydet" dosya adlandırma kuralları (uzantı işleme) |
| `JTypeWriterAppTests` | Ana metod varlığı |
| `AppLoggingTest` | Log biçimlendiricisinin başlık satırını hata vermeden üretmesi (regresyon) ve fırlatılan istisnalar için neden zinciriyle birlikte girintili yığın izi eklemesi |
| `InstanceLockTest` | Örnek kilidinin alınması, aynı JVM'de ikinci alımın reddedilmesi, bırakma ve yinelenebilir kapatma, kullanılamayan kilit dosyasında fail-open davranışı |
| `LanguageTest` | Yerel dil adlarının her sistem dilinde sabit kod sabitleri olması (`getDisplayLanguage` tuzağı yok) |
| `ToolbarStateTest` | Ses açılır listesinin tek ayarlayıcısı: yeniden başlatmada kalıcılık, ses motorunun takip etmesi, tekrarların etkisiz olması |
| `ToolbarFitTest` | Araç çubuğu taşma politikası: başlıklı satırın (düğmeler, açılır listeler, ayırıcı, çerçeve) enjekte edilen ölçülerle nasıl hesaplandığı, FULL→COMPACT geçişi ve 40 px histerezisi; ayrıca Almanca ve Fransızcanın 1280 px'te kırpılmadığını ve daraltılmış satırın altı dilde 900 px'e sığdığını kanıtlayan kabul testleri |
| `ContrastRatioTest` | WCAG kontrast matematiği (siyah-beyaz 21:1, aynı renkler 1:1, 4.5 sınırı) ve durum metni, odak halkası veya popup hücresi kuralları AA'nın / 13 px'in altına düşerse ya da odak rengi yeniden saydamlaşırsa derlemeyi başarısız kılan CSS ayrıştırma korumaları |

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
