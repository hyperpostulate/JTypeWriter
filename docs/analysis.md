# JTypeWriter — Kapsamlı Analiz ve Ürünleştirme Değerlendirmesi

> **Kapsam.** Bu belge yalnızca **analiz**dir. P0 fazı (RW-01…RW-20, RW-54) kapatılmış
> durumdadır ve buradaki tüm bulgular **P0 sonrası** kaynak kod üzerinde doğrulanmıştır.
> Yol haritası (P1 ve sonrası) bu belgenin konusu **değildir**; ayrı bir adımda ele alınacaktır.
>
> **Doğrulama temeli.** HEAD = `d7bf8da`. Ölçümler 25 Ekim 2026'da bu makinede alınmıştır
> (Corretto 25.0.2, Maven 3.9.12, Linux x86-64). Test tabanı: `mvn -q test` →
> **218 test / 25 sınıf, 0 hata, 0 başarısız, 0 atlanmış**. Aşağıdaki her `dosya:satır`
> referansı bu HEAD'e aittir. Ölçülen sayılar **[ölçüldü]**, koddaki ayırma davranışından
> türetilen sayılar **[tahmin]** olarak işaretlenmiştir.

---

## 1. Yönetici Özeti

### 1.1 Tek cümle

JTypeWriter, **motoru ürünleşmeye hazır, ambalajı değil** bir uygulamadır: atomik yazma +
kurtarma disiplini ve Türkçe-öncelikli yerelleştirme gerçek ve ölçülebilir avantajlar, buna
karşılık **dağıtım yolu, imza/checksum, lisans paketlemesi, editörün asgari tabanı ve
koyu temanın okunabilirliği** eksik.

### 1.2 Ölçülen tablo

| Boyut | Ölçüm | Yorum |
|---|---|---|
| Üretim kodu | 43 sınıf / **5.936 satır** | Küçük, okunabilir, sınıf başına ~138 satır |
| Test kodu | 25 sınıf / **3.379 satır** / **218 `@Test`** | Oran 0,57 — sağlıklı |
| `@ParameterizedTest` / `@Disabled` | 0 / 0 | Locale matrisi elle, parametrik değil |
| Dil paketi | 6 × **90 anahtar**, birebir aynı | `MessagesTest` zorluyor |
| Tema / metin rengi | 3 / 6 | Tema başına kontrast ölçüldü (§6) |
| Uygulama JAR'ı (fat) | **11,1 MB** (6.065 girdi) | JavaFX native'leri dahil |
| Linux app-image | **146 MB** | Runtime **134 MB**, **52 modül** |
| Linux `.deb` | **47,3 MB** | 150 MB kurulu boyut |
| Git deposu (target/releases hariç) | **1,5 MB** | — |

### 1.3 En önemli on bulgu

| # | Bulgu | Önem | Bölüm |
|---|---|---|---|
| 1 | **Herkese açık bir sürüm yolu yok**: tag, GitHub Release, checksum, imza yok; CI artefaktları 90 gün sonra siliniyor | Kritik | §8 |
| 2 | **Lisans paketlenmiyor**: JAR/app-image/`.deb` içinde `LICENSE` yok, `.deb` telif dosyası `License: Unknown` yazıyor, GPLv3 kaynak teklifi yok | Kritik | §8.6 |
| 3 | **Koyu temada durum çubuğu okunamıyor**: `.stats-save` için CSS kuralı yok, kontrast **1,32:1** | Kritik | §6.2/U1 |
| 4 | **Kapatma isteği dışındaki her çıkışta taslak siliniyor** (⌘Q, oturum sonu, `stage.hide()`): `WINDOW_HIDDEN` → `store.clear()`, soru sorulmadan | Kritik | §4.2/V2 |
| 5 | **`InstanceLock` sözleşmesi "fail open" diyor, çağrı yeri "fail closed"** — kilit dosyası kullanılamazsa uygulama "zaten çalışıyor" diyip açılmıyor | Kritik | §4.2/V1 |
| 6 | **`MessageFormat` apostrof hatası**: kodlama uyarı diyaloğu EN/FR/IT'de bozuk, `{0}` düz metin olarak görünüyor (gerçek JVM çıktısıyla doğrulandı) | Yüksek | §7.2/I1 |
| 7 | **Dış değişiklik algılama yok**: git checkout / senkron istemcisi / başka editörün yazdığı sessizce eziliyor | Yüksek | §4.2/V3 |
| 8 | **Ara/Değiştir, yazdırma, dışa aktarma, komut satırından dosya açma, çoklu pencere yok** | Kritik (ürün) | §5.2 |
| 9 | **Metin renkleri açık ve sepya temada WCAG AA'yı geçmiyor** (2,32–3,74:1; eşik 4,5:1) | Yüksek | §6.2/U2 |
| 10 | **Otomatik kayıt sessizce ölebiliyor** ve kullanıcıya hiç söylenmiyor | Yüksek | §4.2/V4 |

### 1.4 Hüküm

**Herkese açık `1.0.0`: HAYIR. Kapalı beta: EVET.** Gerekçe §11'de. Kısaca: P0 veri
güvenliği kapısını gerçekten kapattı, ama ürünleştirme kapısı (dağıtım + lisans + editör
tabanı + okunabilirlik) kapanmadı ve bu dördü birbirinden bağımsız işler.

---

## 2. Ürün Kimliği ve Pazar Konumu

### 2.1 Ne satıyor?

JTypeWriter bir ofis paketi değil; **tek işi iyi yapan bir yazma aracı** olarak
konumlanıyor: sade arayüz, daktilo sesi, odak modu ve "taslağını asla kaybetme" güvencesi.
Değer önerisi kelime işlemci olmak değil, **yazma seansının kesintisiz ve kayıpsız
geçmesi**.

Kod bu tezi tutarlı biçimde destekliyor:

- `RecoveryStore` + `AutoSaveService` 30 saniyelik taslak + çökme sonrası üç seçenekli
  kurtarma sunuyor; "sonra sor" seçeneği taslağı **park ediyor**, silmiyor
  (`RecoveryStore.java:157-169`).
- `AtomicFiles.writeMoving` temp + `force(true)` + atomik rename + izin koruma + dizin
  fsync uyguluyor (`AtomicFiles.java:48-77`).
- `YoloPolicy` "kaydetmeden çık" davranışını bilinçli, test edilmiş bir karar olarak kodluyor.
- Editör alanı bilinçli minimal: `main.fxml` boş bir `BorderPane`, tüm UI kodla kuruluyor.

Bu tutarlılık ürünleştirmede **en büyük varlık**. Aynı zamanda en büyük sınır: ürün, "editör"
kategorisinin asgari tabanının altında duruyor (§5.2).

### 2.2 Kimin için?

| Profil | Kanıt | Ürünleştirme sonucu |
|---|---|---|
| Uzun metin yazan tek kişi (roman, tez, senaryo) | Odak modu, daktilo sesi, kelime/karakter sayacı, otomatik kurtarma, YOLO modu | Doğru hedef; ama bu kullanıcı `.docx`, yazım denetimi ve **yazdırma** bekler |
| Türkçe-öncelikli yazar | 6 dil paketi, `tr` case kuralları, CI'da `tr_TR`/`de_DE` tekrar koşuları, iki dilli README | Gerçek ayrıştırıcı; pazarlanabilir |
| Klavye-öncelikli kullanıcı | `KeyboardShortcutHandler`, karşılama ekranındaki kısayol listesi | Doğru niyet; menü olmadığı için keşfedilebilirlik zayıf (§6.2/U10) |
| Kurumsal / çok kullanıcılı | Tek örnek kilidi, tek pencere, eklenti yok, dışa aktarma yok | **Kapsam dışı**; iddia da bu olmamalı |

Kritik soru: hedef kullanıcı **kurulum yapmaya istekli mi?** §8'in cevabı "hayır" —
imzasız, notarize edilmemiş, checksum'sız, lisansı paketlenmemiş bir ikili bu profile
ulaşamaz.

### 2.3 Rekabet

| Rakip | Fiyat | JTypeWriter'ın durduğu yer |
|---|---|---|
| Word / LibreOffice Writer | Ücretsiz–abonelik | Özellik yarışı kaybedilir; kazanılacak tek eksen sadelik + Türkçe + kayıp güvencesi |
| iA Writer, Ulysses, Typora | Ücretli, cilalı, **imzalı** | Aynı pazar; onlar imzalı/notarize dağıtım, senkronizasyon, mobil sunuyor |
| Obsidian, VS Code | Ücretsiz | Markdown/kod odaklı; JTypeWriter "düz metin + daktilo" nişinde |
| focuswriter, Apostrophe | Açık kaynak, Linux | **En yakın rakip**; daha geniş özellik seti, daha zayıf Türkçe/veri güvenliği |

Ayrıştırıcı üç eksen gerçekten var ve kodla destekleniyor: **(1)** Türkçe-öncelikli
yerelleştirme kalitesi, **(2)** atomik yazma + kurtarma disiplini, **(3)** daktilo/odak
deneyimi. Buna karşılık dağıtım, lisans uyumu ve destek hikâyesi rakiplerin gerisinde —
yani durum "iyi motor, eksik ürün".

### 2.4 Ürünleştirme engelleri, ürün tezine göre

| Ürün vaadi | Vaadi çürüten bulgu | Bölüm |
|---|---|---|
| "Hiçbir şey kaybolmaz" | Kapatma isteği dışı çıkışta taslak silinmesi (V2); otomatik kaydın sessizce ölmesi (V4); kurtarma sonrası kodlama kaybı (V6) | §4.2 |
| "Kesintisiz yazma" | FX thread'inde senkron G/Ç (M3); her tuşta O(n) dirty-check (M4); 1 Hz O(n) istatistik (M5) | §3.4 |
| "Türkçe'de kusursuz" | Apostrof hatası (I1); sayı biçimi varsayılan yerel ayarda (I2) | §7 |
| "Sade ve okunabilir" | Koyu temada 1,32:1 durum etiketi (U1); AA altı metin renkleri (U2); temassız `Alert`'ler (U3) | §6 |
| "Kur ve çalıştır" | Yayınlanmış ikili yok, imza yok, lisans pakette yok (B1–B6) | §8 |

---

## 3. Mimari Değerlendirme

### 3.1 Güçlü yanlar

1. **Tek ve gerçek kompozisyon kökü.** `AppGraph` içinde her bileşen `public final` alan;
   **alan sırası = bağımlılık sırası**; dinleyiciler kurucusunda bağlanıyor
   (`AppGraph.java:42-109`). Statik singleton yok. 43 sınıflık bir projede test
   edilebilirliği ayakta tutan asıl karar bu.
2. **Davranış saf metotlara indirgenmiş.** `MainController.titleFor` / `savedStatusFor`,
   `MainStage.intersectsAny`, `ToolbarFit.decide`, `YoloPolicy`, `DialogService`
   dosya adı kuralları, `SessionStats` — hepsi headless test edilebiliyor.
3. **Hata yolları sessizce yutulmuyor** (genelde): `AppLogging` + `CrashHandler` + diyalog.
4. **Küçük sınıflar.** 500 satırı geçen sınıf yok; ortalama ~138 satır.
5. **Yazma yolu tek ilkel üzerinden.** Belge, taslak ve ayar aynı `AtomicFiles.writeMoving`
   çağırıyor (`FileService.java:257`, `RecoveryStore.java:107,201`, `AppSettings.java:299`).
6. **Yerelleştirme altyapısı doğru kurulmuş.** `Messages.NO_FALLBACK`, `Locale.ROOT`
   kullanımı, `Theme#getDisplayName` ile tema adlarının paketten gelmesi.

### 3.2 Yapısal borç

| # | Bulgu | Kanıt | Etki |
|---|---|---|---|
| M1 | **`AppLogging` enjekte edilemez.** `logger()` çağrısı `init()`'i tetikliyor, yol `AppDirs.logsDir()` ile sabit; `init()` bir kez çalışır (`initialized` bayrağı). | `AppLogging.java:121-155` | Testler gerçek `~/.jtypewriter/logs`'a yazıyor — bu makinede test koşusu **679 KB** log üretti. AGENTS.md "gerçek `~/.jtypewriter`'a dokunma" kuralı kod tarafından uygulanamaz kılınıyor. |
| M2 | **Kompozisyon kökü ve yaşam döngüsü köprüsü test edilemez.** `AppGraph` içinde `AppSettings.load()` ve `AppDirs.recoveryDir()` sabit; enjekte edilebilir kurucu yok. | `AppGraph.java:42,51`; `FxApplication.java:23` | Başlatma hatalarının yaşadığı iki sınıfın **sıfır** testi var. |
| M3 | **Tüm belge G/Ç'si FX thread'inde senkron.** Kodda `ExecutorService`/`CompletableFuture`/`Task`/`Service` **hiç yok**; `synchronized` blokları dekoratif (her şey aynı thread'de). | grep: eşleşme yok | Yavaş disk/ağ veya büyük dosya = donmuş arayüz. |
| M4 | **Her tuşta O(n) dirty-check + `stage.setTitle`.** | `MainController.java:171-177` → `hasUnsavedChanges()` = `!currentContent.equals(savedContent)` (`FileService.java:81-85`); `refreshTitle()` → `stage.setTitle` | 50 MB belgede tuş başına ~50 M karakter karşılaştırması **[tahmin 5–20 ms]** + native başlık güncellemesi. "Belge büyüdükçe yavaşlıyor" klasik hatası. |
| M5 | **İstatistik 1 Hz tam tarama + kelime başına ayırma.** | `StatsBar.java:84-88`; `SessionStats.java:64-80` (`split("\\s+")`, üç `replace(...)` çağrısı, `split("\n",-1)`) | 50 MB metinde saniyede ~4 tam geçiş + 3×50 MB geçici String **[tahmin 300–500 MB geçici, 100 ms–1 s duraklama]**. |
| M6 | **Otomatik kayıt her 30 s'de tam metni seri hale getiriyor** (3 kopya) ve metin değişmemiş olsa da (`dirty` kaldığı için) tekrar yazıyor. | `AutoSaveService.java:89,157-170`; `RecoveryStore.java:366-378` | Kirli büyük belgede 30 s'de bir tam blok. |
| M7 | **`UndoHistoryReset` yansıma ile çalışıyor ve başarısızlığı kimse okumuyor.** | `UndoHistoryReset.java:41-71`; `EditorArea.java:104-110` (dönüş değeri yok sayılıyor) | Modül yolunda/jlink görüntüsünde sessizce bozulur → Ctrl+Z **önceki belgenin** metnini geri getirebilir. |
| M8 | **FXML ve Media işlevsiz ağırlık.** `main.fxml` boş `BorderPane`; Media yalnızca 71 KB'lık tek WAV için. | `main.fxml`; `pom.xml:29-38` | Fat JAR'da ~3,5 MB GStreamer/FFmpeg native'i (§8.1). |
| M9 | **Tek belge/tek pencere varsayımı kök salmış.** Tek `FileService`, tek taslak slotu, tek `Stage`, tek `EditorArea`; belge oturumu nesnesi yok. | `FileService.java:62-66`; `RecoveryStore.java:95-101`; `MainController.java:39` | Sekme/çoklu pencere eklemek 6+ sınıfa dokunmayı gerektirir. |
| M10 | **Dispose sözleşmesi yok.** `Application.stop()` yok, `AutoSaveService` dinleyicileri hiç kaldırılmıyor, `StatsBar`'ın `Timeline`'ı yerel değişkende. | `FxApplication` (stop yok); `AutoSaveService.java:89-98`; `StatsBar.java:84` | Kaynak sızıntısı bugün JVM kapanışıyla maskeleniyor; ikinci pencere gelince gerçek hata olur. |
| M11 | **Dinleyici sırası örtük bir değişmez.** `Messages.reload()` `ToolbarView.refresh()`'ten önce çalışmalı; bu yalnızca `AppGraph` alan sırası sayesinde doğru. | `AppGraph.java:46` vs `:69`; `LocaleManager.java:42` | Bir alanın yeri değişirse "dil değişince eski etiketler" hatası, hiçbir test kırılmadan geri gelir. |
| M12 | **Ölü kod: 8 `Messages` getter'ı (7 anahtar × 6 paket).** `langCode`, `toolbarFontUp`, `toolbarFontDown`, `yoloModeOn/Off`, `soundOn/Off`, `emptyFile` — kodda ve testte **sıfır** kullanım. | `Messages.java:58,86,90,148,152,156,160,294` | `MessagesTest` bu anahtarları sonsuza kadar 6 dosyada eşit tutuyor. AGENTS.md "tüm paketlerde aynı anahtar" kuralı ölü anahtarı da konserve ediyor. |
| M13 | **Üretim kodunda yalnızca testin kullandığı sınıf/API'ler.** `ContrastRatio` (95 satır) sadece `ContrastRatioTest` tarafından kullanılıyor; `StatsBar.defaultStatusFontSizePx`, `TypewriterSound(TypewriterSound,double)` de test-only. | `ui/theme/ContrastRatio.java` | Sevk edilen JAR'a test yardımcısı giriyor; bakım yükü üretim sorumluluğu gibi görünüyor. |
| M14 | **Ayar yazma amplifikasyonu.** Her görünüm değişikliği tam dosya yeniden yazımı + 2 fsync; açılışta `applyTheme` değer değişmese de yazıyor. | `ThemeManager.java:46` (`setTheme`'de koruma var, `applyTheme`'de yok); `AppSettings.java:295-308` | Bir Ctrl+S ~3 tam ayar yazımı + fsync; her tema/font adımı aynı. |
| M15 | **Büyük dosya stratejisi yok.** `Files.readAllBytes`, tek `TextArea` + `wrapText(true)`, sınırsız undo yığını. | `FileService.java:164,189`; `EditorArea.java:43` | `TextArea` sanallaştırma yok; 10 MB zaten hantal, 50 MB kullanılamaz. Gerçek ölçek sınırı bu. |
| M16 | **Paket döngüsü: `AppSettings` → `ui.theme`.** Kök paket headless olması gerekirken UI enum'larını import ediyor; `StatsBarState`/`ToolbarState` ters yönde `AppSettings`'i import ediyor. | `AppSettings.java:3-5,78`; `StatsBarState.java:3` | Katman tersine dönmüş; `StatsBarState` kendi javadoc'unda bunu itiraf ediyor (`:12-17`). |

### 3.3 Tekrar eden kod

- `WelcomeDialog.show` (`:38-110`) ile `AboutDialog.show` (`:46-139`) neredeyse kopya:
  aynı `SHELL_MARGIN`/`ICON_SIZE`/`CONTENT_WIDTH`, aynı sahne boyutu bloğu, aynı
  `setOnKeyPressed(ESCAPE|ENTER)`, aynı stylesheet kopyalama. ~120 satır ortak fabrikaya
  indirilebilir; drift riski gerçek (biri düzeltilip diğeri unutulabilir).
- Dört ayrı enum döngüsü: `Theme.next`, `TextColor.next`, `Language.toggle`,
  `StatsBar.Size.next`.
- Aynı "atomik taşı + fallback" üç kez yazılmış: `AtomicFiles:65-69`,
  `RecoveryStore.move:172-178`, `AppSettings.move:286-292`.
- `confirmDiscardOrSave` (`:137-155`) ile `promptSaveOnExit` (`:299-306`) aynı
  Save/Discard/Cancel dalını iki farklı sonuçla tekrarlıyor.

### 3.4 Threading ve ölçeklenme — mimarinin en zayıf ekseni

| İşlem | Thread | Engelleme |
|---|---|---|
| Tuş vuruşu | FX | Tam belge `String.equals` + `stage.setTitle` native çağrısı |
| Aç / oturum geri yükleme | FX | `Files.readAllBytes` + decode |
| Kaydet | FX | encode kopyası + **yedek tam kopyası** + fsync ×2 |
| Otomatik kayıt (30 s, odak kaybı) | FX | `getText()` + tam serileştirme + fsync |
| Açılışta kurtarma teklifi | FX | **tüm** taslak dosyalarını okuyup ayrıştırma |
| İstatistik (1 Hz) | FX | tam belge taraması |

Eşzamanlılık ilkeli **hiç yok**. Yani "kesintisiz yazma" vaadi, 300–800 KB'lık gerçekçi bir
roman/tez boyutunda donma riskiyle doğrudan çelişiyor. Bu, ürünleştirmenin en pahalı ama
en görünmez borcu.

### 3.5 Uzantı noktaları

Yalnızca iki: `FileService.Listener` ve `LocaleManager` dinleyicisi. Ayrıca:

- **CLI / dosya ilişkilendirmesi yok.** `Application.launch(..., args)` argüman geçiriyor ama
  `FxApplication` `getParameters()`'ı **hiç okumuyor** → `jtypewriter not.txt` ve "Birlikte
  aç" çalışmaz. Paketlenmiş bir masaüstü uygulaması için bu ciddi bir eksik (§5/F7).
- `module-info.java` yok — bilinçli (yansıma + classpath lansmanı) ama jlink/JPMS'i
  engelliyor ve M7'yi kırılgan yapıyor.

---

## 4. Veri Güvenliği — Çekirdek ve Kalan Açıklar

Veri güvenliği bu projenin **en olgun** alanı; P0 gerçekten iş yaptı. Aşağıda önce kapanan
yollar, sonra kalan açıklar önem sırasıyla.

### 4.1 Kapanan yollar (P0 ile doğrulanmış)

| Yol | Durum | Kanıt |
|---|---|---|
| Düz `Files.write` ile yazma | Kapandı — tüm yazmalar tek ilkel üzerinden | `AtomicFiles.writeMoving` |
| Yazma sırasında çökme → yarım dosya | Kapandı — temp + `force(true)` + `ATOMIC_MOVE` | `AtomicFiles.java:48-77` |
| Mevcut dosya izinlerinin kaybı | Kapandı — POSIX modu korunuyor, testli | `AtomicFiles.java:85-121`, `AtomicFilesTest.java:92-106` |
| Kodlama sessizce değişmesi (BOM'lu yol) | Kapandı — sıkı çözme + kullanıcıya sorma | `TextCodec.java:92-107,169-180` |
| Çökme sonrası taslak kaybı | Kapandı — `RecoveryStore` + `AutoSaveService`, "sonra sor" park ediyor | `RecoveryStore.java:157-169` |
| Bozuk ayar dosyası uygulamayı çökertmesi | Kapandı — karantina (`.corrupt`, üzerine yazmadan) | `AppSettings.java:231-284` |
| Bozuk taslak metadata'sının taslağı düşürmesi | Kapandı — toleranslı ayrıştırma | `RecoveryStore.java:247-364` |
| Hataların sessizce yutulması | Büyük ölçüde kapandı — `AppLogging` + `CrashHandler` | `AppLogging.java`, `CrashHandler.java` |
| Kısa dosya adlarında temp dosya hatası | Kapandı — prefix workaround, testli | `AtomicFiles.java:53`, `AtomicFilesTest.java:70-76` |

### 4.2 Kalan açıklar

#### V1 — İki örnek aynı dosyayı yazabiliyor / kilit sözleşmesi ters — **Kritik**

`InstanceLock` javadoc'u açıkça **"Fail open"** diyor: kilit oluşturulamazsa uygulama yine
açılmalı. Gerçekte `acquire` hem "başka örnek var" hem "kilit dosyası kullanılamıyor"
durumunda `Optional.empty()` döndürüyor ve `JTypeWriterApp` boş sonucu **her zaman**
"zaten çalışıyor" sayıp çıkıyor:

```java
var lock = InstanceLock.acquire(AppDirs.dataDir());
if (lock.isEmpty()) { showAlreadyRunning(); return; }   // JTypeWriterApp.java:20-24
```

Sonuçlar:

1. **Salt-okunur ev dizini / sandbox / konteyner profili** = uygulama hiç açılmıyor ve
   kullanıcıya "JTypeWriter zaten çalışıyor" deniyor. Yalan bir mesaj.
2. **Fail-open arıza modunda** (NFS'te kilit desteği yok vb.) ikinci tam örnek çalışır.
   Dış değişiklik algılama (V3) olmadığı için en kötü yazma yarışı doğar: A v1'i `.bak`'a
   alır, B v1'i `.bak`'a alır, A vA yazar, B vB yazar → **vA hiçbir yerde kalmaz, yedekte
   bile**. Bu, tek dosyalık gerçek veri kaybı senaryosu.
3. `InstanceLockTest` bu belirsizliği **davranış olarak sabitliyor** (kilit adında dizin
   testi `isEmpty()` bekliyor).

Düzeltme şekli: üç durumlu sonuç (`acquired` / `already-running` / `unavailable`) ve yalnızca
`already-running` durumunda engelleme.

#### V2 — Kapatma isteği dışındaki her çıkış taslağı siliyor — **Kritik**

Tek kayıt sorusu `stage.setOnCloseRequest` içinde (`MainStage.java:68-73`). Taslağı silen
şey ise `WINDOW_HIDDEN` işleyicisi:

```java
stage.addEventHandler(WindowEvent.WINDOW_HIDDEN, e -> shutdown());   // AutoSaveService.java:98
public void shutdown() { if (timeline != null) timeline.stop(); if (!draftFrozen) store.clear(); }  // :144-151
```

Yani `setOnCloseRequest`'i atlayan her sonlandırma — macOS ⌘Q, `Platform.exit()`, oturum
kapatma, gelecekteki herhangi bir `stage.hide()` — **soru sormadan tek kopyayı silebilir**.
`Application.stop()` yok, JVM shutdown hook yok; ikinci savunma hattı hiç yok.

#### V3 — Dış değişiklik algılama yok — **Yüksek**

`FileService` kaydetmeden önce dosyanın mtime/boyut/hash'ini **hiç okumuyor**
(`getLastModifiedTime` yalnızca `RecoveryStore.java:342`'de geçiyor). `writeFile` yalnızca
bellekteki iki String'i karşılaştırıyor (`FileService.java:253`). Yükleme sonrasında
git checkout, senkron istemcisi, başka bir editör veya ikinci örnek dosyayı değiştirdiyse
**sessizce ezilir**. Tek telafi `.bak` rotasyonu — ki o da 3 nesil ve V9'da açıklanan
sorunlarla malul.

Bu bulgu, ürünün "hiçbir şey kaybolmaz" tezinin en doğrudan çürütücüsü: kullanıcının
metni değil, **başkasının** metni kayboluyor.

#### V4 — Otomatik kayıt sessizce ölebiliyor — **Yüksek**

Üç ayrı yol, üçü de kullanıcıya söylenmiyor:

| Yol | Davranış | Kanıt |
|---|---|---|
| `park()` başarısız | `draftFrozen = true` kalır, oturum boyunca `snapshot` hemen döner; sadece log WARNING, yeniden denenmez | `RecoveryStore.java:139-141,157-169`; `AutoSaveService.java:158-160` |
| `saveDraft` her tick'te başarısız (salt-okunur/dolu kurtarma dizini) | `catch (Exception)` → yalnızca `LOG.warning`; kullanıcıya hiçbir gösterge yok | `AutoSaveService.java:167-169` |
| İlk `WINDOW_HIDDEN` sonrası | Timeline durdurulur ve **hiç yeniden başlatılmaz** | `AutoSaveService.java:144-147` |

README "her 30 saniyede anlık görüntü" diyor (`README.md:25,96`). Bu üç yoldan birinde bu
cümle **yanlış** olur ve arayüzde bunu ele verecek tek bir gösterge yoktur.

Ek olarak: `AutoSaveServiceTest` `attach()`'i hiç çağırmıyor — 30 s timeline'ı, odak kaybı
dinleyicisi ve `WINDOW_HIDDEN` işleyicisi **test edilmiyor**.

#### V5 — Symlink hedefi değil, linkin kendisi değişiyor — **Yüksek**

`AtomicFiles` öznitelikleri link üzerinden okuyor ama rename **linki** yeni bir normal
dosyayla değiştiriyor (`AtomicFiles.java:64-66`). `~/notlar.txt -> /mnt/data/notlar.txt`
durumunda gerçek belge **hiç güncellenmez**, kullanıcı kaydettiğini sanır; `.bak` da linkin
yanına, yani yanlış dizine yazılır.

#### V6 — Kurtarma sonrası kodlama UTF-8'e düşüyor — **Yüksek**

`restoreRecovered` `currentFilePath`'i koşulsuz atıyor, ama `currentCharset`/`currentBom`'u
yalnızca decode **başarılıysa** yazıyor (`FileService.java:189-194`). `TextCodec.decode`
`null` döndüğünde (BOM'suz, geçerli UTF-8 olmayan eski dosya) nesne varsayılan
**UTF-8**'de kalıyor → bir sonraki Ctrl+S eski kodlamalı dosyayı sessizce UTF-8'e çeviriyor.

Bunu gizleyen ikinci kusur: testi ASCII içerik (`"published chapter"`) kullanıyor, yani
legacy dal **hiç çalışmıyor**. Test bu hatayı yapısal olarak göremez.

İlgili, daha geniş bir vaka: BOM'suz UTF-16 (ASCII aralığında) **geçerli UTF-8** sayılır
(NUL baytları) → uyarısız açılır, düzenlenip kaydedilince UTF-16 yapısı bozulur
(`TextCodec.java:105-106`).

#### V7 — Yedek hatası asıl kaydı iptal ediyor — **Yüksek**

`FileService.writeFile` önce `backupKeeper.backup(path)`, sonra
`AtomicFiles.writeMoving(path, bytes)` çağırıyor (`FileService.java:255-257`).
`BackupKeeper.backup` `IOException` fırlatıyor ve düz `Files.copy` kullanıyor. Yani
**yedek alınamazsa belge hiç kaydedilmiyor**.

Bu, güvenlik mantığının tersine dönmesi: yedek güvenlik ağıdır, asıl işlemi engellememeli.
Disk dolu, izin yok veya yedek dizini kilitliyse kullanıcı belgesini kaydedemez — ve hata
metni "yedek" hakkında olduğu için belgenin kaydedilmediğini anlamayabilir.

Ayrıca `BackupKeeper.backup` önce rotasyon yapıyor (`:23-29`), yani **en eski nesil, yeni
yedek başarılı olmadan** silinmiş oluyor. Yedek de atomik değil, fsync yok,
`COPY_ATTRIBUTES` yok → yarıda kalan kopya bozuk `.bak` bırakır ve `0600` bir belgenin
`.bak`'ı umask'a göre `0644` olabilir.

#### V8 — Başarılı kayıt "başarısız" olarak raporlanıyor — **Orta**

Dinleyici bildirimi `try` bloğunun **içinde** ve `catch (IOException | RuntimeException)`
bunu "kayıt başarısız"a çeviriyor:

```java
currentFilePath = path; savedContent = content; currentContent = content;
listeners.forEach(l -> l.onSaved(path));      // FileService.java:261
return Optional.of(path);
} catch (IOException | RuntimeException e) { ... return Optional.empty(); }   // :263-269
```

Bir dinleyici istisna atarsa dosya **diskte doğru**, ama çağırana `Optional.empty()`
dönüyor → `promptSaveOnExit` pencereyi kapatmayı reddediyor, `confirmDiscardOrSave`
aç/yeni işlemini blokluyor. Kalıcı "kaydedilmemiş" durumu + yanıltıcı hata diyaloğu.

Bugün yıkıcı olmaması **dinleyici sırasına** bağlı (`AppGraph.java:87-97`): AutoSave.clear
önce. Bu örtük bir güvenlik mekanizması, garanti değil.

#### V9 — Kaydetme ile taslak silme arasındaki çökme eski taslak bırakıyor — **Orta**

`writeFile` dosyayı yazar, `savedContent`'i günceller, **sonra** dinleyicileri çağırır
(`:257-261`); taslağı yalnızca AutoSave'in `onSaved` işleyicisi siliyor. Bu pencerede çökme
(fsync/yedek yavaşken pencere büyür) **dosyadan daha eski** bir taslak bırakır. Bir sonraki
açılışta "kurtar?" diye sunulur; kabul edilirse belge sessizce geri alınır. Taslak mtime'ı
ile diskteki dosya **karşılaştırılmıyor**.

#### V10 — Taslak başlığındaki yol keyfi bir dosyaya işaret edebiliyor — **Orta**

`RecoveryStore` taslak başlığındaki `original.path.b64`'ü Base64'ten çözüp doğrulama
yapmadan `Path.of(...)`'a veriyor (`RecoveryStore.java:312-323`). Değer `currentFilePath`'e
akıyor (`FileService.java:194`) ve bir sonraki Ctrl+S'in **hedefi** oluyor — `.bak`
rotasyonu dahil. Kurtarma dizini kullanıcı tarafından yazılabilir durum; yedekten veya başka
bir makineden gelebilir.

#### V11 — Taslak taraması symlink takip ediyor ve sınırsız — **Orta**

`Files.list(dir).filter(Files::isRegularFile)` linkleri takip ediyor
(`RecoveryStore.java:123-125`): herhangi bir okunabilir dosyaya işaret eden `draft-*.jtw`
symlink'i ayrıştırılıp kurtarılabilir metin olarak sunulur. Her aday **tamamen**
`readAllBytes` ile okunuyor, en yenisi seçilmeden önce (`:247-254`) → çok sayıda büyük parked
taslak açılışı OOM'a sürükleyebilir. Boyut/sayı sınırı yok.

#### V12 — Park edilen taslaklar hiç toplanmıyor — **Orta**

`pending-*.jtw` dosyalarını, daha sonraki bir `loadDraft`'ın kazananı olmaktan başka hiçbir
şey silmiyor (`:196-206`). Yaş/sayı/boyut sınırı yok → disk sınırsız büyür, açılış süresi ve
bellek o geçmişle birlikte büyür, ve **kullanıcının diğer belgelerinin taslakları profilde
süresiz kalır** (bir yazma aracı için gizlilik sorunu). Ayrıca açılış başına yalnızca **en
yeni** taslak sunuluyor, yani N ertelenmiş taslak N yeniden başlatma ister.

#### V13 — Atomik olmayan fallback doğrulanmıyor + temp süpürme yok — **Orta**

`ATOMIC_MOVE` desteklenmediğinde `Files.move(REPLACE_EXISTING)`'e düşülüyor
(`AtomicFiles.java:67-69`) ve bu yolda **boyut/hash doğrulaması yok**. SMB/NFS'te (atomik
move sık desteklenmez) süreç öldürülürse hedef yarım yazılmış olabilir; tek sağlam kopya
temp dosyasıdır ve normal hata yolunda `finally` onu siler (`:72-76`).

Ayrıca:

- `finally` içindeki `deleteIfExists` kendisi istisna atarsa **asıl istisnayı maskeler**.
- `SIGKILL`/elektrik kesintisi sonrası `jtw-*.tmp` **hiçbir zaman süpürülmüyor**
  (başlangıç taraması yok).
- `AtomicFiles` üst dizini **oluşturmuyor** (`:53`) — `RecoveryStore.saveDraft` oluşturuyor
  (`:105`), `FileService` ve `AppSettings` oluşturmuyor.

#### V14 — Boşluk amplifikasyonu ve disk dolu — **Orta**

Tek kaydetmenin disk maliyeti belgenin ~5 katına çıkabilir: hedef + temp + `.bak` +
`.bak.1` + `.bak.2`. Boş alan kontrolü hiç yok. `>2 GB` belgelerde `readAllBytes` hata
verir; sıradan büyük dosyalarda `OutOfMemoryError` **kayıt hata yolunu atlar** (bir `Error`,
`catch (IOException | RuntimeException)` tarafından yakalanmaz).

#### V15 — Ayarlarda `NaN`/`Infinity` kabul ediliyor — **Orta**

`AppSettings.optionalDouble` `Double.parseDouble`'un `NaN`/`Infinity` kabul etmesini
engellemiyor (`:323-333`). Elle düzenlenmiş `settings.properties` (`window.width=Infinity`)
`stage.setWidth/setHeight`'e kadar ulaşabilir; `MainStage`'in `width > 0` ve ekran kesişim
kontrolü NaN/Inf'i filtreliyor ama **`setWidth(Infinity)` çağrısını** engellemiyor.
Karantina mekanizması yalnızca *parse* hatasını yakalıyor, *anlamsız değeri* yakalamıyor.

#### V16 — Ayar dosyası ISO-8859-1 ve yazma hatası sessiz — **Orta**

`Properties.store(OutputStream)` ISO-8859-1 + `\uXXXX` yazıyor (`:298`), yani elle
düzenlenmiş UTF-8 bir ayar dosyası (Türkçe bir yol) okunduğunda mojibake olur. Ve
`save()` `IOException`'ı WARNING'e indiriyor (`:300-302`): kalıcı olarak yazılamayan ayar
dosyası arayüzde görünmüyor, her tercih her çalıştırmada sessizce kayboluyor.

#### V17 — `RecoveryStore.clear()` hatası FINE seviyesinde — **Orta**

Kaydettikten sonra silinmesi gereken taslak silinemezse yalnızca `FINE` log
(`:181-187`). Taslak hayatta kalır, sonraki açılışta sunulur; geri yüklenip kaydedilirse
**daha yeni dosyanın üzerine** yazılır — kendi kendine yapılmış bir geri alma.

#### V18 — `AppLogging.init()` çökme güvenli değil ve her şeyin statik başlatma yolunda — **Orta**

`initialized = true` işin **önünde** atanıyor (`:125`) ve yalnızca `IOException`
yakalanıyor (`:141`). Bir `SecurityException` (sıkılaştırılmış JVM, sandbox ev dizini)
`logger()`'dan kaçar — ki bu `AtomicFiles`, `FileService`, `AppSettings`, `RecoveryStore`
sınıflarının `static final Logger` başlatıcısında çalışır → `ExceptionInInitializerError`,
yani **uygulama açılmaz**.

#### V19 — Testler gerçek `~/.jtypewriter`'a yazıyor — **Orta (hijyen)**

M1'in doğrudan sonucu. AGENTS.md "asla gerçek `~/.jtypewriter`'a dokunma" diyor; kod bunu
uygulanamaz kılıyor. Bu makinede test koşusu 679 KB `jtypewriter-0.log` üretti — kanıt.

### 4.3 Kullanıcıya görünen kayıp/bozulma tablosu (öncelik sırasıyla)

| # | Senaryo | Önem |
|---|---|---|
| V1 | İki örnek aynı dosyayı kaydeder; bir yazarın metni hiçbir yerde, `.bak`'ta bile yok | Kritik |
| V2 | Kapatma isteği dışı çıkış (⌘Q/`Platform.exit`/oturum sonu) kirli taslağı sorusuz siler | Kritik |
| V3 | Dış düzenleme / senkron / git checkout uyarısız ezilir | Yüksek |
| V4 | Otomatik kayıt sessizce ölür → çökme koruması sıfır, arayüz söylemez | Yüksek |
| V5 | Symlink'li belge kaydedilir; gerçek hedef sonsuza kadar eski kalır | Yüksek |
| V6 | Legacy (windows-1254) belge taslaktan geri yüklenip UTF-8 olarak yazılır | Yüksek |
| V7 | Yedek alınamaz → belge kaydedilmez (ve kullanıcı nedenini anlamaz) | Yüksek |
| V8 | Başarılı kayıt "başarısız" görünür; pencere kapanmaz | Orta |
| V9 | Kaydetme sonrası çökme eski taslak bırakır; kabul edilirse belge geri alınır | Orta |
| V10 | Taslak başlığı belgeyi keyfi bir dosyaya yönlendirir; sonraki Ctrl+S onu ezer | Orta |
| V11 | Symlink'li sahte taslak kurtarma listesine düşer | Orta |
| V12 | `pending-*.jtw` sınırsız birikir; diğer belgelerin taslakları profilde kalır | Orta |
| V13 | SMB/NFS'te yarım yazılmış hedef; `jtw-*.tmp` artıkları kalıcı | Orta |
| V14 | Disk dolu → yedek döndürmesi en eski nesli kaybeder, sonra kayıt başarısız | Orta |
| V15 | Bozuk `window.width=Infinity` başlatmayı bozar | Orta |
| V16 | Yazılamayan ayar dosyası → her tercih her açılışta kaybolur | Orta |
| V17 | Silinemeyen taslak daha yeni dosyanın üzerine yazdırır | Orta |
| V18 | `SecurityException` → uygulama hiç açılmaz | Orta |
| V19 | Test koşusu gerçek kullanıcı loglarını kirletir | Orta |

### 4.4 Bu bölümün test boşlukları

Hiçbir test şunları kapsamıyor: symlink hedefi, hard link, **dış değişiklik**, `.bak`
çakışması, başarısız/salt-okunur kurtarma dizini, **aynı dosyayı paylaşan iki örnek**, disk
dolu/yarım kopya, sonsuz geometri, `restoreRecovered`'ın legacy dalı, BOM'suz UTF-16,
istisna atan `FileService.Listener`, kapatma isteği olmadan `WINDOW_HIDDEN`. Mevcut testler
mutlu yol ve temiz hata yoluyla sınırlı
(`AtomicFilesTest`, `BackupKeeperTest`, `RecoveryStoreTest`, `AppSettingsTest`).

Ve en önemlisi: **süreç öldürme testi yok**. Kurtarma, `store.saveDraft` çağırıp
`new RecoveryStore(dir)` kurularak "simüle" ediliyor. Uygulamayı fork edip yazı yazdırıp
`SIGKILL` ile öldürüp yeniden başlatıp kurtarmayı doğrulayan tek bir test yok — oysa tüm
otomatik kayıt tasarımı tam bu senaryo için var.

---

## 5. Özellik Bütünlüğü

### 5.1 Var olanlar (ve gerçekten çalışanlar)

| Alan | Durum | Not |
|---|---|---|
| Düz metin düzenleme, geri al/yinele | Var | `TextArea` + `UndoHistoryReset` (M7 kırılganlığıyla) |
| Aç / kaydet / farklı kaydet | Var | `FileService`, atomik, kodlama korumalı |
| Yeni belge (Ctrl+N) | Var | — |
| Otomatik kurtarma | Var | `AutoSaveService` + `RecoveryStore` |
| Otomatik kaydetme (ayarlanabilir) | Var | — |
| Kelime/karakter/satır sayacı, WPM, tuş sayısı | Var | `SessionStats` (1 Hz, O(n) — M5) |
| Tema (3), metin rengi (6), yazı boyutu (10–48, 19 basamak) | Var | Kalıcı, `ui/theme/` |
| Odak modu | Var | `EditorArea` |
| Daktilo sesi | Var | `TypewriterSound` (aç/kapat) |
| YOLO modu (kaydetmeden çık) | Var | `YoloPolicy`, kalıcı değil (bilinçli) |
| Araç çubuğu sığdırma (FIT/COMPACT) | Var | `ToolbarFit.decide` (saf, testli) |
| Tek örnek kilidi | Var | `InstanceLock` (V1 çelişkisiyle) |
| Çökme yakalama + dönen log | Var | `CrashHandler`, `AppLogging` |
| 6 dil | Var | EN temel + TR/DE/FR/IT/ES |
| Klavye kısayolları + kısayol listesi | Var | `KeyboardShortcutHandler`, `WelcomeDialog` |
| Pencere durumu kalıcılığı + çoklu ekran taşması | Var | `MainStage.intersectsAny` |
| Dosya adı doğrulama / üzerine yazma onayı | Var | `DialogService` (saf kurallar + test) |

### 5.2 Olmayanlar — bir metin editörünün asgari tabanı

"Olmaması" tek başına kusur değil; **hedef kullanıcının beklentisiyle çelişmesi** kusur.

| # | Eksik | Neden kritik | Önem |
|---|---|---|---|
| F1 | **Ara / değiştir** | 300 KB metinde gezinmenin tek yolu kaydırma çubuğu. Uzun metin yazarı için temel araç. | **Kritik** |
| F2 | **Yazdırma / PDF** | "Tez yazan" profil için çıktı almanın hiçbir yolu yok. | **Kritik** |
| F3 | **Dışa aktarma** (PDF/HTML/`.docx`/EPUB) | Ürün çıktısı yalnızca `.txt`; yayıncıya/öğretmene teslim edilemiyor. | **Kritik** |
| F4 | **Komut satırından dosya açma** | `getParameters()` hiç okunmuyor → OS dosya ilişkilendirmesi ve "Birlikte aç" işlemez. Paketlenmiş bir masaüstü uygulaması için temel. | **Kritik** |
| F5 | **Yazım denetimi** | 6 dil paketi var ama yazım denetimi yok; TR için pazarlanan avantajı zayıflatıyor. | Yüksek |
| F6 | **Çoklu pencere / sekme** | Tek pencere; iki belge yan yana imkânsız (M9). | Yüksek |
| F7 | **Son açılan dosyalar** | `last.file` var (oturum geri yükleme) ama liste/erişim yok. | Orta |
| F8 | **Sürükle-bırak ile dosya açma** | `setOnDragOver`/`setOnDragDropped` aramaları boş. | Orta |
| F9 | **Menü çubuğu** | `MenuBar` yok; her şey araç çubuğu + kısayol. Keşfedilebilirlik düşük. | Orta |
| F10 | **Belge içi gezinme** (başlık, yer imi, outline) | Uzun metinde yapı yok. | Orta |
| F11 | **Uygulama içinde sürüm/derleme bilgisi** | `AboutDialog` sürüm göstermiyor (`:60-80`). Hata bildirimi yapılamaz. | Orta |
| F12 | **Güncelleme kontrolü** | Yok (gizlilik açısından savunulabilir) ama **elle** kontrol yolu da yok. | Orta |
| F13 | **Tercihler penceresi** | Her şey araç çubuğunda; klavye kullanıcısı için ayar erişimi dağınık. | Düşük |
| F14 | **RTL / `NodeOrientation`** | Arapça/İbranice yön desteği yok (dil paketlerinde de yok, tutarlı). | Düşük |

### 5.3 README ile gerçek arasındaki farklar

| README iddiası | Gerçek | Kanıt |
|---|---|---|
| `--installer` ile `.deb`/`.msi`/`.dmg` kullanıcıya sunuluyor | Kayıtlı ürün kararı **yalnızca taşınabilir** dağıtım | `README.md:214,224-226`; `README-TR.md` aynı yerler |
| CI her OS için "native image + installer" üretiyor | Doğru, ama yalnızca 90 günlük Actions artefaktı; yayınlanan sürüm yok | `README.md:247`; `maven.yml:69-74` |
| Kullanıcı gereksinimi "Java 25+" | jpackage görüntüsü JDK 25.0.2'yi **içeriyor**; son kullanıcı Java kurmak zorunda değil | `README.md:21,189`; app-image `release` dosyası |
| Kurulum: `git clone` + `mvn clean install` | Son kullanıcı için kurulum yolu değil, geliştirici yolu | `README.md:194-202` |
| "Her 30 saniyede anlık görüntü" | Doğru — V4'teki üç yoldan birinde değil | `README.md:25,96` |
| "Araç çubuğu **her genişlikte** tek satır kalır, hiçbir şey taşmaz/kırpılmaz" | Yalnız 1280 ve 900 px'te ölçülmüş; `ToolbarFit` COMPACT'ın da sığmayabileceğini açıkça kabul ediyor; pencerede `setMinWidth` yok | `README.md:77-84`; `ToolbarFitTest.java:118-146`; `ToolbarFit.java:192-195`; `MainStage.java:23-54` |
| "Durum çubuğu metni üç temada da WCAG AA" | `.stats-clock`/`.stats-label` için doğru; `.stats-save` için **yanlış** (kural yok, 1,32:1) | `README.md:68`; `StatsBar.java:62-63`; `base.css:311-321` |
| "Dil değişimi durum çubuğunu **anında** günceller" | `StatsBar`'ın locale dinleyicisi yok; 1 s'lik timeline bekliyor | `README.md:130`; `StatsBar.java:84-88` |
| "Beklenmeyen hatalar hata diyaloğunda gösterilir" | Otomatik kayıt, ayar yazımı, ses, undo-reset ve About-mail hataları yalnız loglanıyor | `README.md:98`; `AutoSaveService.java:167-169`; `AppSettings.java:300-302`; `TypewriterSound.java:91-93`; `UndoHistoryReset.java:63-66` |
| "Her araç çubuğu kontrolüne Tab ile erişilir: önce düğmeler, sonra açılır listeler" | Sıra aslında Hakkında ve Dil'i de açılır listelerden **sonra** içeriyor; tek mekanizma `viewOrder` ve JavaFX bunu odak sırası için garanti etmiyor | `README.md:82-84`; `ToolbarView.java:209-216,238-245` |
| "`ContrastRatioTest` popup-cell kurallarını AA'ya karşı korur" | O test yalnız **yazı boyutu** eşitliğini kontrol ediyor | `README.md:400`; `ContrastRatioTest.java:137-144` |
| Tema döngüsü "Light, Dark, Sepia" sırasında | Varsayılan tema **Dark**; ilk `Ctrl+Shift+T` **Sepia**'ya gider | `README.md:119-123`; `AppSettings.java:106-108`; `Theme.java:50-53` |
| Karşılama listesi: "Ctrl+S Save / Save As" | `Ctrl+S` yalnız Kaydet; Farklı Kaydet `Ctrl+Shift+S` | `messages.properties:30`; `KeyboardShortcutHandler.java:62` |

Ayrıca CI, kayıtlı kararın aksine üç OS'ta da `--installer` çağırıyor
(`maven.yml:65-67`): belge, betik ve ürün kararı üç ayrı yerde üç ayrı şey söylüyor.

---

## 6. UX ve Erişilebilirlik

### 6.1 Kapanan işler (P0)

- Araç çubuğu taşması: `ToolbarFit.decide` ile FIT/COMPACT kararı + histerezis, saf ve testli.
- Pencere konum/boyut kalıcılığı ve çoklu ekran taşması.
- Kaydetmeden çıkma koruması (`YoloPolicy`).
- Dosya adı doğrulama ve çakışma diyalogları (saf kurallar + test).
- Tema/renk/boyut tercihlerinin kalıcılığı ve yerelleştirilmiş adları.
- Araç çubuğunda tooltip, Tab sırası, başlıklı açılır listeler.
- Odak halkaları ve `ContrastRatioTest` ile **CSS üzerinden** ölçülen AA taban kontrolleri
  (kapsamı dar — §6.2 notu).

### 6.2 Açık kalanlar — ölçülmüş kontrast dahil

Kontrast oranları WCAG 2.1 AA eşiği **4,5:1** (normal metin) üzerinden hesaplanmıştır.

| # | Bulgu | Ölçüm / kanıt | Önem |
|---|---|---|---|
| U1 | **`.stats-save` için CSS kuralı yok.** Durum çubuğundaki "kaydedildi" etiketi Modena varsayılanı `#333` ile koyu çubuk `#1e1e1e` üzerinde. | **1,32:1** — okunamaz. `StatsBar.java:63`; `.stats-save` `css/` içinde **hiç tanımlı değil**. | **Kritik** |
| U2 | **Metin renkleri açık/sepya temada AA'yı geçmiyor.** | SAGE 2,99 (açık) / 2,54 (sepya) · SAND 2,74 / 2,32 · STEEL 3,43 / 2,91 · ROSE 3,55 / 3,02 · LAVENDER 3,74 / 3,17. Yalnızca koyu tema geçiyor. | Yüksek |
| U3 | **Tüm `Alert` diyalogları temalandırılmıyor.** `getDialogPane().getStylesheets()` hiçbir yerde çağrılmıyor; yalnızca `WelcomeDialog`/`AboutDialog` sahnelerine stylesheet kopyalanıyor. | `DialogService`, `CrashHandler`, `SingleInstanceWarning` → koyu temada açık renk kutu. | Yüksek |
| U4 | **Karşılama/hakkında pencereleri sabit ve taşıyor.** Karşılama **488×778**, Hakkında **568×848** — ikisi de 768 px yüksekliğindeki bir ekranı aşıyor. Karşılama `setResizable(false)` (`WelcomeDialog.java:98`) ve **`ScrollPane` yok**; Hakkında'da `ScrollPane` var ama **pencere** yine ekran dışına taşıyor (`AboutDialog.java:115,127`). | `WelcomeDialog.java:86,98`; `AboutDialog.java:115,127` | Yüksek |
| U5 | **Kısayol bloğu hiç sarmıyor → DE/FR/IT/ES'te kırpılıyor.** Ölçülen en uzun satırlar 12 px monospace'te: DE **368 px**, FR **383**, IT **383**, ES **376** — kartın içerik kutusu **340 px** (ilan edilen 380 px maksimuma karşı). | `WelcomeDialog.java:49-50`; `AboutDialog.java:57-58`; `base.css:355-367`. Koddaki "Almanca iki kez sarar" yorumu **yanlış**. | Yüksek |
| U6 | **Düğme/bağlantı kontrastı sınırda veya altında.** Karşılama CTA: beyaz/`#8b7355` = **4,49** (açık), `#f4ecd8`/`#8b7355` = **3,81** (sepya); hover: **3,31** / **2,81**; koyu temada beyaz/`#c0883c` = **3,08**; Hakkında e-postası **4,16** (açık) / **3,62** (sepya); karşılama tagline `#777`/`#f8f6f2` = **4,15**. | `base.css:347-353,369-390,424-435`; `light.css:18,22,25-27`; `sepia.css:18,25-27` | Orta-Yüksek |
| U7 | **Açılışta editöre odak verilmiyor.** `requestFocus()` çağrısı tüm `src/main`'de **yok**. Karşılama modali kapandıktan sonra odak editörde değil; kullanıcı tıklayana kadar yazdığı harf kaybolabilir. | grep: `requestFocus` eşleşmesi yok; `FxApplication.java:54-65` | Yüksek |
| U8 | **Araç çubuğu gizlenince odak görünmez düğümde kalabiliyor.** `Ctrl+T` `visible`/`managed` false yapıyor ama odağı taşımıyor. | `MainController.java:180-186` | Orta |
| U9 | **Durum çubuğu ilk saniye boş.** Kurucu `update()` çağırmıyor; ilk dolgu 1 s timeline'ın ilk tick'inde. | `StatsBar.java:75,84-88` | Düşük |
| U10 | **Dil değişimi durum çubuğunda anında değil.** `StatsBar`'ın locale dinleyicisi yok; 1 s'lik timeline'ı bekliyor (README "anında" diyor). | `StatsBar.java:84-88,116-127`; `README.md:130` | Orta |
| U11 | **Özel `Alert` düğmelerinde default/cancel semantiği yok.** `ButtonType`'lar yalnız metinle kuruluyor, `ButtonData` verilmiyor → Enter/Escape garantili değil. | `DialogService.java:96-98,133-137,164-166,187-189,202-205,227-230` | Orta |
| U12 | **Yeni ve Farklı Kaydet yalnız klavyede.** Araç çubuğunda 5 düğme + 6 combo + Hakkında + Dil var; `Ctrl+N`/`Ctrl+Shift+S` için fare yolu yok. | `ToolbarView.java:209-216`; `KeyboardShortcutHandler.java:61-62` | Orta |
| U13 | **UI yazı tipi 11 px ve bağımsız ölçeklenmiyor.** Araç çubuğu 11 px, karşılama kısayolları 12 px, durum 13 px; editör boyutu değişse de UI sabit. | `base.css:106,137,192,238,355-357` | Orta |
| U14 | **Tema döngüsü README'nin anlattığı sırada değil.** Varsayılan tema **Dark**; ilk `Ctrl+Shift+T` Light değil **Sepia**'ya gidiyor. | `AppSettings.java:106-108`; `Theme.java:50-53`; `README.md:119-123` | Düşük |
| U15 | **YOLO Backspace/Delete'i engelliyor ama daktilo sesi yine çalıyor** — düzenleme olduğu izlenimi. | `EditorKeyHandler.java:26-29`; `EditorArea.java:73-82` | Düşük |
| U16 | **Kirli `*` ve `[YOLO]` başlığın sonuna ekleniyor** — uzun dosya adlarının OS/WM tarafından kırpıldığı yer. | `MainController.java:141-143` | Düşük |
| U17 | **CSS hijyeni: geçersiz JavaFX özellikleri.** `-fx-letter-spacing` ve `-fx-transition` JavaFX CSS'te yok; yok sayılıyor (ve muhtemelen loglanıyor). | `base.css:344,379` | Düşük |
| U18 | **YOLO dışındaki sağ tık menüsü uygulama dilinde değil.** JavaFX'in yerel menüsüne düşülüyor → JVM/OS yerel ayarı. | `EditorArea.java:129-133` | Düşük |
| U19 | **Karşılama diyaloğu her açılışta modal.** `stage.show()`'dan sonra `showAndWait` → ilk çizim engelleniyor; "bir daha gösterme" ayarı yok. | `WelcomeDialog.java:109`; `FxApplication.java:65`; `AppSettings.java:62-76` (anahtar yok) | Orta |
| U20 | **Dar genişlikte araç çubuğu ölçülmemiş.** | `ToolbarFit.decide` COMPACT'ın altında taban tanımlamıyor (`ToolbarFit.java:189-201`); kodda hiç `setMinWidth` yok. "Her genişlikte tek satır" COMPACT altında doğrulanmamış. | Orta |
| U21 | **Erişilebilirlik anlamları eksik.** | `accessibleText` yalnız araç çubuğu combo'larında (`ToolbarView.java:392-397`); `EditorArea`'nın erişilebilir adı/açıklaması yok, durum çubuğu canlı bölge değil, diyaloglarda erişilebilir ad ve klavye odak sırası testi yok (headless kuralı gereği test de edilemiyor). | Orta |
| U22 | **Yüksek kontrast / sistem tema modu.** | JavaFX yüksek kontrast moduna uyum yok; tema sistemi kendi renklerini dayatıyor. | Orta |
| U23 | **Kısayollar keşfedilebilir değil.** | Menü yok (F9); kısayollar yalnızca karşılama ekranında ve diyalogda. Karşılama listesi ayrıca yanlış: "Ctrl+S Save / Save As" diyor, Farklı Kaydet `Ctrl+Shift+S`; liste `Ctrl+Shift+S` ve `Ctrl+Shift+Z`'yi atlıyor (`messages.properties:30`). | Orta |
| U24 | **Hata mesajları teknik / yerelleştirilmemiş.** | `CrashHandler` yığın izi içeren diyalog gösteriyor (`:63-66`) ve başlıkları **her zaman İngilizce** (`"Uncaught exception in …"`, `"Startup failed"`, `:38,65`); tanılama paketi / "log klasörünü aç" yok. | Düşük |
| U25 | **Daktilo sesi her tuşta çalıyor** — Ctrl, ok tuşları, auto-repeat dahil; ilk tuşta `.wav` çıkarma + `AudioClip` kurulumu FX thread'inde **[tahmin 100–500 ms]**. | `EditorKeyHandler.java:26-30`; `TypewriterSound.java:82-107` | Orta |

**Öne çıkan tek satır:** U1 ve U3 birlikte, koyu temayı — ürünün en görünür özelliğini —
"çalışıyor ama cilasız" durumda bırakıyor. İkisi de tek dosyalık CSS düzeltmesiyle
kapanabilecek kadar küçük, algılanan kalitedeki etkisi ise orantısız büyük. U7 (açılışta
odak yok) ve U5 (kısayol bloğu sarmıyor) ise **işlevsel** kusurlar: ilki yazmayı geciktirir,
ikincisi dört dilde metni kırpar.

**Geçenler:** durum çubuğu metni (`-stats-text`/`-toolbar-bg`) ve araç çubuğu/editör metni
AA'yı rahat geçiyor — açık **4,68**, koyu **5,15**, sepya **4,83**; araç çubuğu 6,53+,
editör 7,51+. Sorun sistematik değil, **tek tek tanımsız/yanlış renklerde**.

**Not:** `ContrastRatioTest` CSS'i **gerçekten ayrıştırıp** ölçüyor — bu, projedeki en iyi
test fikri. Ama kapsamı dar: yalnız `-stats-text`/`-toolbar-bg`, `-focus-ring` (üç yüzey),
`-focus-ring-text` ve istatistik/popup hücre **yazı boyutu** eşitliği. Karşılama tagline'ı,
CTA/hover, Hakkında e-postası, `.stats-save`, vurgu renkleri, araç çubuğu ve editör metni
kapsam dışı — yani §6.2'deki açıkların çoğu testin **görmediği** yerlerde. Ayrıca
`README.md:400` testin "popup-cell kurallarını AA'ya karşı koruduğunu" söylüyor; o test
yalnız yazı boyutunu karşılaştırıyor — iddia fazla.

---

## 7. i18n Kalitesi

### 7.1 Güçlü yanlar

- 6 paket, 90 anahtar, birebir aynı — `MessagesTest` bunu zorluyor.
- Türkçe `toLowerCase()` tuzağı bilinçli çözülmüş: `Messages.NO_FALLBACK`, `Locale.ROOT`
  kullanımı (`Messages.java:20-21,46`; `AppSettings.java:340`).
- CI `tr_TR` ve `de_DE` ile tekrar koşuyor (`maven.yml:40-49`).
- Tema ve renk adları paketlerden geliyor; kodda koşul yok (`Theme#getDisplayName`).
- `MessagesTest` "veri kaybettirebilecek sabit çevirileri" de sabitliyor (`:111-117`).

### 7.2 Açık — teknik

#### I1 — `MessageFormat` apostrof hatası — **Yüksek, gerçek çıktıyla doğrulandı**

`encodingMalformedContent` anahtarı `'` (U+0027) içeriyor; `MessageFormat` bunu kaçış
karakteri sayıyor. Aynı harness ile üretilen gerçek JVM çıktısı:

| Yerel ayar | Çıktı |
|---|---|
| `en` | `This files byte-order mark … {0}` |
| `fr` | `Lordre doctets … {0}` |
| `it` | `Lordine … {0}` |
| `de` / `tr` / `es` | Doğru |

Yani **EN, FR, IT'de metin bozuk ve `{0}` argümanı düz metin olarak görünüyor**. Kaynak:
`src/main/resources/messages*.properties` ~satır 84 (EN dahil) →
`Messages.java:395-397`. Bu, kullanıcıya kodlama uyarısı gösteren diyalogda ortaya çıkıyor —
yani **en çok güven gereken anda**, kayıplı çözmenin riskini anlatması gereken yerde.

Aynı tehlike sınıfı, ileride çevrilecek her argümanlı anahtar için geçerli:
`overwriteContent`, `recoveryContent`, `encodingUpgradeContent`, `errorLogged`,
`savedStatus` ve yedi argümanlı `statsFormat` (paket satır 26).

Düzeltme iki katmanlı olmalı: (a) metinlerde `''` kaçışı veya `%s`+`String.format`'e geçiş,
(b) `MessageFormat` kullanan **her** anahtarı **her** dilde biçimlendirip `{n}` kalıntısı
olmadığını doğrulayan test.

#### I2 — `statsFormat` varsayılan yerel ayarı kullanıyor — **Orta**

`Messages.statsFormat` statik `MessageFormat.format` çağırıyor (`:314-317`), yani
`Locale.getDefault()`. Aynı harness ile: TR arayüzde sayılar **`1,234` / `4,500`** üretiliyor;
Türkçe'de beklenen `1.234` / `4.500`. Arayüz Türkçe, sayı biçimi İngilizce. Bu, ürünün
pazarladığı yerelleştirme kalitesini doğrudan zayıflatıyor.

#### I3 — `MessagesTest` neyi zorlamıyor

Şu an yalnızca **anahtar kümesi eşitliğini** kontrol ediyor. Kontrol etmedikleri:

- Değer boş mu?
- `MessageFormat` argüman sayısı tutuyor mu?
- Biçimlendirme sonucu bozuk mu (apostrof / kaçan `{n}`)?
- `statsFormat`'ın yedi yer tutucusu her dilde doğru sırada mı?

I1 tam olarak bu dört boşluktan kaçıyor. Bu kontroller eklendiğinde hata sınıfı kapanır.

#### I4 — Yerel ayar disiplini boşlukları

| Bulgu | Kanıt |
|---|---|
| Kurtarma diyaloğunun zaman damgası **sistem** yerel ayarında biçimleniyor, arayüz dilinde değil | `DialogService.java:265-269` (`ofLocalizedDateTime` varsayılan yerel ayarla) |
| İstatistik tarih/saat desenleri gömülü: `dd.MM.yyyy`, `HH:mm:ss` | `StatsBar.java:26-27` |
| Başlık saati 24 saatlik `HH:mm` gömülü | `MainController.java:25` |
| Yerelleştirilmemiş dizeler: `"JTypeWriter"`, `" *"`, `"[YOLO]"`, log/stderr metinleri | `FxApplication.java:45`; `MainController.java:142`; `InstanceLock.java:83`; `CrashHandler.java:38` |
| **Durum çubuğu dil değişiminde anında güncellenmiyor** — locale dinleyicisi yok, 1 s'lik timeline bekliyor (README "anında" diyor) | `StatsBar.java:84-88,116-127`; `README.md:130` |
| **Araç çubuğu başlıklarında gömülü `": "` ayırıcı** — Fransızca'da `Thème: Sombre` yerine tipografik `Thème : Sombre` | `ToolbarFit.java:88`; `ToolbarView.java:536-541` |
| **Tooltip'lerde gömülü `Ctrl+…`** — macOS'ta Cmd konvansiyonu README'de yazılı ama ipucu platforma duyarlı değil | `ToolbarView.java:79-86,648-651`; `README.md:136` |
| **Karşılama kısayol listesi yanlış**: "Ctrl+S Save / Save As" — Farklı Kaydet `Ctrl+Shift+S`; liste `Ctrl+Shift+S` ve `Ctrl+Shift+Z`'yi atlıyor | `messages.properties:30`; `KeyboardShortcutHandler.java:62` |
| **`MessageFormat` çağrılarının tamamı JVM varsayılan yerel ayarını kullanıyor** — yalnız `statsFormat` değil: `overwriteContent`, `recoveryContent`, `encodingUpgradeContent`, `encodingMalformedContent`, `errorLogged`, `savedStatus` | `Messages.java:237-239,314-317,327-329,375-377,395-397,407-409`; `MainController.java:154-156` |
| **`Alert` varsayılan `OK` düğmesi ve yerel sağ tık menüleri JVM yerel ayarını izliyor**, uygulama dilini değil (Türkçe arayüzde İngilizce "OK") | `CrashHandler.java:63`; `SingleInstanceWarning.java:22-26`; `EditorArea.java:129-133` |
| `-DargLine` Surefire'ın `argLine`'ını bütünüyle değiştiriyor → gelecekte `UndoHistoryReset` için gereken `--add-opens` locale işlerinde sessizce düşer | `maven.yml:45-49` |

#### I5 — Ölü anahtarlar

`toolbarFontUp`, `toolbarFontDown` (+ `langCode`, `yoloModeOn/Off`, `soundOn/Off`,
`emptyFile` getter'ları) — kodda ve testte **sıfır** kullanım (M12). `MessagesTest` bunları
6 dosyada sonsuza kadar eşit tutuyor.

### 7.3 Öncelik

i18n, ürünün **pazarlanabilir ayrıştırıcısı** — bu yüzden I1 ve I2 "küçük hata" değil,
ayrıştırıcıyı doğrudan zayıflatan kusurlar. İkisi de tek oturumluk iş; etkisi pazarlama
malzemesinde görünür.

---

## 8. Build, Paketleme, Dağıtım ve Hukuk

### 8.1 Artefakt envanteri ve gerçek boyutlar [ölçüldü]

| Artefakt | Yol / kanıt | Boyut | Nasıl üretiliyor |
|---|---|---:|---|
| İnce JAR | `pom.xml:69-80` | şu an yok | `mvn package` |
| Fat JAR | `pom.xml:88-111`; `releases/linux/JTypeWriter/lib/app/…-jar-with-dependencies.jar` | **11.112.653 B**, 6.065 girdi | `maven-assembly-plugin` |
| Linux app-image | `releases/linux/JTypeWriter/`; `package.sh:109-122` | **146 MiB**, 121 dosya | `jpackage --type app-image` |
| Runtime image | `lib/runtime/` | **134 MiB** (`lib/modules` 90,6 MB; `libjvm.so` 28,7 MB; `ct.sym` 10,9 MB) | jpackage'ın iç jlink'i |
| Runtime modül sayısı | `lib/runtime/release` | **52 modül**, JDK **25.0.2** | varsayılan tam runtime |
| Linux `.deb` | `releases/linux/jtypewriter_1.0.0_amd64.deb` | **49.598.510 B (~47,3 MiB)**, kurulu 150 MB | `package.sh:151-156` |
| Taşınabilir arşiv (tar.gz) | **üretilmiyor** | `tar -czf` ile 60 MiB olurdu | — |
| Windows / macOS artefaktı | `releases/` altında yok | — | yalnızca CI matrix |

Fat JAR JavaFX native'lerini taşıyor: `libgstreamer-lite.so` 2,3 MB, `libjfxmedia.so`
575 KB, 11 adet `libavplugin-ffmpeg-*.so`, `libglassgtk3.so` 2,1 MB.

`.deb` tamamen `/opt/jtypewriter/` altına kuruluyor; `/usr/share` girdisi yok; masaüstü
dosyası `Categories=Unknown` ve `MimeType=` **boş**, `--linux-app-category "Office"`
verilmesine rağmen. Telif dosyası **132 bayt** ve kelimesi kelimesine `License: Unknown`
yazıyor.

### 8.2 Paketleme ve build — ürünleştirme engelleri

| # | Blocker | Kanıt |
|---|---|---|
| B1 | **jlink yok; 52 modüllük tam runtime.** `jdk.compiler`, `jdk.jshell`, `jdk.javadoc`, `jdk.jconsole`, `jdk.jfr`, `jdk.attach` runtime'da. | `package.sh:109-122` (`--add-modules`/`--runtime-image` yok); `lib/runtime/release` |
| B2 | **Fat JAR + classpath lansmanı.** `JTypeWriterApp` bilinçli olarak `Application` alt sınıfı değil. | `pom.xml:88-111`; `JTypeWriterApp.java:7-11` |
| B3 | **JavaFX Media/FXML ağırlığı.** 71 KB'lık tek WAV için ~3,5 MB native; boş `main.fxml` için FXML. | `pom.xml:29-38`; `main.fxml` |
| B4 | **Maven wrapper ve enforcer yok.** README "Maven 3.8+" diyor, CI yalnız runner'ın Maven'ını kullanıyor. | `pom.xml`; `README.md:189-190` |
| B5 | **Yeniden üretilebilirlik zorlanmıyor.** `project.build.outputTimestamp` yok, bağımlılık kilidi/checksum yok, JDK vendor sabit değil (yerel Corretto 25.0.2, CI Temurin 25). | `pom.xml`; `maven.yml:33-38` |
| B6 | **`.gitattributes` yok.** Windows'ta CRLF checkout, `bash scripts/package.sh` çağrısını kırabilir. | `.gitattributes` yok; `maven.yml:65-67` |
| B7 | **İkon drift'i CI'da kontrol edilmiyor.** `generate-icons.py --check` var, hiç çağrılmıyor. | `scripts/generate-icons.py:146-180` |
| B8 | **Platforma özgü araçlar belgelenmemiş.** Linux `.deb` için `fakeroot` (CI kuruyor), macOS için `hdiutil`, Windows için WiX. | `maven.yml:61-63`; `package.sh` |

### 8.3 Dağıtım

| # | Blocker | Önem | Kanıt |
|---|---|---|---|
| B9 | **Tag ile tetiklenen sürüm ve yayınlanan sürüm varlığı yok.** Workflow yalnız `main` push/PR'da; `upload-artifact` (90 gün varsayılan) — GitHub Release yok. `releases/` gitignored. | **Kritik** | `maven.yml:10-14,69-74`; `.gitignore:38-40` |
| B10 | **Hiçbir yerde imza yok.** `signtool`/`codesign`/`notarytool`/GPG yok; macOS Gatekeeper ve Windows SmartScreen uyarısı kaçınılmaz. | **Kritik** | `package.sh:109-144` |
| B11 | **Checksum / provenance / SBOM yok.** `SHA256SUMS`, Sigstore attestation, `cyclonedx` yok; artefakt commit SHA'sı taşımıyor. | **Kritik** | `package.sh`; `maven.yml` |
| B12 | **Taşınabilir Linux artefaktı yok.** Yalnız `.deb`; AppImage/Flatpak/Snap/tar.gz üretilmiyor. | **Kritik** (kayıtlı karar taşınabilirlik) | `package.sh:148-165`; `releases/linux` |
| B13 | **Linux pratikte yalnız Ubuntu 24.04 x86-64.** `Depends: libasound2t64` (24.04 adlandırması) → Debian 12 / Ubuntu 22.04 karşılamaz; glibc 2.39. arm64 `.deb` yok. | Yüksek | `.deb` control; `ubuntu-latest` |
| B14 | **macOS yalnız Apple Silicon.** Tek `macos-latest` leg; Intel/universal DMG yok. | Yüksek | `maven.yml:25-26` |
| B15 | **Windows yalnız x64, imzasız MSI.** ARM64 yok. | Yüksek | `maven.yml:26`; `package.sh:157-160` |
| B16 | **Veri dizini platforma uygun değil.** Her OS'te `~/.jtypewriter`; macOS'ta `~/Library/Application Support`, Windows'ta `%APPDATA%` değil; `XDG_*` ve taşınabilir mod yok. | Orta | `AppDirs.java:14-16` |
| B17 | **JavaFX native cache + WAV temp çıkarma kontrolsüz.** İlk çalıştırmada per-user cache'e `.so` çıkar; WAV her çalıştırmada yeni temp dosyaya (`deleteOnExit`). Çökmede artık kalır. | Orta | `TypewriterSound.java:97-107` |
| B18 | **Wayland/X11 test edilmemiş ve belgelenmemiş.** `.deb` X11 kütüphanelerine bağımlı, Wayland paketi yok. | Orta | `.deb` control |
| B19 | **Desktop entegrasyonu minimal/yanlış.** `Categories=Unknown`, boş `MimeType`, `.txt` ilişkilendirmesi yok, man page yok, `/usr/share/doc` yok. | Orta | `.deb` desktop dosyası |
| B20 | **Java 25 kullanıcı gereksinimi gibi sunuluyor.** Paketlenmiş uygulama runtime'ı içeriyor. | Orta | `README.md:21,189` |

### 8.4 CI

| # | Bulgu | Önem | Kanıt |
|---|---|---|---|
| B21 | **CI paketlenmiş görüntünün açıldığını, `.deb`'in kurulduğunu, MSI/DMG'in açıldığını doğrulamıyor.** Zincir yüklemede bitiyor. | Yüksek | `maven.yml:65-74` |
| B22 | **Workflow seviyesinde `contents: write`.** Checkout, Maven build/test ve tüm üçüncü taraf action'lar yazma token'ı alıyor; `persist-credentials` kapatılmamış. | Yüksek | `maven.yml:17-18` |
| B23 | **Action'lar mutable major tag ile pinlenmiş** (`@v4`), commit SHA değil. | Yüksek | `maven.yml:31,34,70` |
| B24 | **Rozet betiği repo'yu sabit kodluyor, token'ı clone URL'ine gömüyor, hataları 0 ile yutuyor.** Rozet sessizce bayatlar; Almanca koşudan sonra okunduğu için **toplu** değil **son locale** sonucunu yansıtır. | Yüksek | `publish-test-badge.sh:21,116,128-132`; `maven.yml:45-49,59` |
| B25 | **Test suite 9 kez koşuyor** (3 OS × [package + tr + de]) ama kapsam ölçümü/rapor birleştirme yok. | Orta | `maven.yml:40-49` |
| B26 | **Artefakt yüklemesinde `retention-days` yok** → koşu başına ~194 MB, 90 gün; günlük `main` push'unda birikir. | Orta | `maven.yml:69-74` |
| B27 | **Sürüm mühendisliği yok:** `workflow_dispatch`, concurrency grubu, release notes, changelog, bağımlılık güvenlik taraması, shellcheck, `mvn verify` yok. | Orta | `maven.yml` |

### 8.5 Sürümleme ve güncelleme hikâyesi

- **Tek doğruluk kaynağı:** `pom.xml:9` (`1.0.0`). `package.sh:61-71` sürümü fat JAR adından
  türetip jpackage kurallarına göre doğruluyor — bu kısım sağlam.
- **Sürüm kimliği yok:** `git tag` boş, GitHub Release yok, `CHANGELOG.md` yok. `1.0.0`
  yalnızca POM'da ve dosya adlarında yaşıyor; artefakt commit SHA'sı veya build zamanı
  taşımıyor.
- **Uygulama içi sürüm yok:** `AboutDialog` başlık, tagline, kısayollar, yazar, e-posta,
  kapat gösteriyor (`:60-80`); sürüm, build tarihi veya lisans yok. Kullanıcı neyi
  çalıştırdığını bildiremez.
- **Güncelleme mekanizması yok** (gizlilik için iyi) ama **elle** kontrol yolu da yok.
- **Ayar şeması/sürümü yok** (M14/V16 ile birlikte): enum/anahtar yeniden adlandırması
  tercihi sessizce sıfırlar, downgrade koruması yok.
- **Destek matrisi yok:** minimum OS sürümleri, bilinen sınırlamalar, Gatekeeper/SmartScreen,
  Wayland, arm64 notları belgelenmemiş.

### 8.6 Hukuk

| # | Bulgu | Önem |
|---|---|---|
| B28 | **Lisans ikiliye girmiyor.** `package.sh` jpackage'ın `--license-file` seçeneğini kullanmıyor; JAR'da `LICENSE` yok; app-image'da yok; `.deb` telifi `License: Unknown`. | **Kritik** |
| B29 | **GPLv3 §6 kaynak teklifi yok.** Binary ne kaynağı ne yazılı teklifi taşıyor; README'deki GitHub bağlantısı artefaktla birlikte sevk edilmiyor. | **Kritik** |
| B30 | **Uygun yasal bildirimler yok.** Etkileşimli uygulama olmasına rağmen About diyaloğunda telif, lisans veya garanti reddi yok (GPLv3 §0/§5d). | **Kritik** |
| B31 | **JavaFX/FFmpeg/GStreamer bildirimleri yok.** Fat JAR'da JavaFX lisans/NOTICE girdisi yok (JDK runtime'ında 44 legal dosya var — olumlu, ama projeyi ve JavaFX'i kapsamıyor). | Yüksek |
| B32 | **`keystroke.wav` kaynağı/lisansı bilinmiyor.** 71.204 B, PCM 16-bit stereo 48 kHz; ilk commit'te eklenmiş; repoda hiçbir yerde kaynak, yazar, lisans veya atıf yok. | **Kritik** |
| B33 | **POM metadata eksik:** `<url>`, `<licenses>`, `<developers>`, `<scm>`, `<organization>` yok. Maven Central tarzı yayını ve otomatik SBOM/lisans raporlamasını engelliyor. | Yüksek |
| B34 | **SPDX/telif başlığı yok:** 43 Java dosyasının 0'ında. | Düşük |
| B35 | **İkon/artwork kaynağı belgelenmemiş.** `jtypewriter.svg`'de yazar/lisans metadata'sı yok. | Orta |
| B36 | **Gizlilik notu yok.** Telemetri/ağ yok (güçlü yan), ama loglar ve taslaklar kullanıcı yollarını/metnini içeriyor; nerede saklandığı, yerel olduğu ve nasıl silineceği belgelenmemiş. | Orta |
| B37 | **Marka incelemesi yok.** "Java" yalnızca düzyazıda; logo yok. Düşük risk, ticari dağıtımdan önce isim incelemesi gerekli. | Düşük |

### 8.7 Desteklenebilirlik

- **Loglar:** `~/.jtypewriter/logs/jtypewriter-%g.log`, 1 MB × 3, INFO; konsol WARNING+;
  tam girintili yığın izi (`AppLogging.java:50-101,135-138`).
- **Çökme bildirimi:** `CrashHandler` SEVERE loglar ve log dizinini metin olarak gösteren
  diyalog açar (`:63-66`). Tanılama paketi, "log klasörünü aç" düğmesi, çökme kimliği,
  telemetri yok.
- **`.github/` altında yalnızca workflow var:** `SECURITY.md`, `CONTRIBUTING.md`,
  `CODE_OF_CONDUCT.md`, `CHANGELOG.md`, `.github/ISSUE_TEMPLATE/` yok.
- **`docs/` boş** (bu belge dışında). README/README-TR özellik ve build belgesi; kullanıcı
  destek belgesi değil. SSS, sorun giderme, bilinen sınırlamalar, geçiş rehberi yok.

---

## 9. Test ve Kalite Güvencesi

### 9.1 Ölçülen şekil

| Metrik | Değer |
|---|---|
| Test sınıfı / `@Test` | 25 / **218** |
| `@ParameterizedTest` / `@Nested` / `assertAll` | 0 / 0 / 0 |
| Assumption ile kapılanan test | 2 (ikisi de POSIX'e özgü: `AppSettingsTest:223`, `AtomicFilesTest:94`) |
| En büyük suite'ler | `FileServiceTest` 30, `RecoveryStoreTest` 18, `TextCodecTest` 16, `AppSettingsTest` 15, `FontSizeManagerTest` 13, `SessionStatsTest`/`ToolbarFitTest` 12 |
| Kapsam ölçümü | **Yok** (JaCoCo yok) |
| Mutasyon testi | Yok |
| Statik analiz / mimari testi | Yok (ArchUnit yok, checkstyle/spotless yok) |
| Headless UI çerçevesi | Yok (TestFX/Monocle yok) |
| Süreç öldürme testi | **Yok** |
| Hata enjeksiyonu | **Yok** |

### 9.2 Gerçekten iyi kapsanan alanlar

Bu, kod tabanının ortalamasının üstünde:

- **Kodlama/veri güvenliği:** `TextCodecTest` (BOM algılama, UTF-8/UTF-16LE/BE ve yalnız
  surrogate için bozuk BOM reddi, `?` üretmeyi reddeden sıkı encode); `FileServiceTest`
  (bayt-özdeş legacy gidiş-dönüş `:142-157`, kodlama yükseltme kabul/iptal `:282-319`,
  bozuk BOM kabul/iptal `:331-396`, yedek rotasyonu, değişmemiş kayıt koruması `:262-275`,
  yeni belgede charset sıfırlama `:473-492`).
- **Kurtarma:** `RecoveryStoreTest` (18 test: bozuk başlık, geçersiz UTF-8, adoption,
  park etme, park edilmiş taslağın adoption'ı, oturum izolasyonu) ve
  `AutoSaveServiceTest` (parked-draft regresyonları `:68-103`).
- **Ayar dayanıklılığı:** `AppSettingsTest` (karantina `:149-219`, bozuk değerler, POSIX
  okunamaz dizin `:222-241`).
- **Kalıcı yazma:** `AtomicFilesTest` (temp temizliği, kısa adlar, izin koruma).
- **i18n altyapısı:** `MessagesTest` (anahtar eşitliği, `tr-TR`/`de-DE` altında
  `NO_FALLBACK` davranışı `:52-71`, görünen adlar, veri kaybettirebilecek sabit çeviriler
  `:111-117`).
- **Saf politika:** `ToolbarFitTest` (gerçek bundle ile genişlik modeli + histerezis),
  `YoloPolicyTest`, `MainStageTest`, `ContrastRatioTest` (**gerçek CSS'i ayrıştırıyor** —
  mükemmel fikir), `AppLoggingTest` (biçimlendirici + düşmanca throwable), `InstanceLockTest`,
  `BackupKeeperTest`.

### 9.3 Headless kuralı yüzünden hiç test edilemeyen yüzey

Hiç TestFX/toolkit/display olmadığı için şunlar **tamamen testsiz**:
`EditorArea` YOLO zorlaması, `UndoHistoryReset`, `StatsBar.update` + timeline, `ToolbarView`
(combolar, `refresh`, caption, tooltip, fit uygulaması), `MainController.setStage`/`initialize`
bağlantıları, `AutoSaveService.attach`/`offerRecovery`/`shutdown`, `FileService` diyalogları
ve `promptSaveOnExit`, `CrashHandler`, `TypewriterSound`, `FxApplication.start`,
`MainStage.configure`/close-request, `WelcomeDialog`/`AboutDialog`, `SingleInstanceWarning`,
`AppIcons.applyTo`.

Yani **uygulamanın çalışma zamanı davranışının tamamı** kod okuyarak doğrulanıyor.

### 9.4 İddia ettiği dalı çalıştırmayan testler

| Test | Sorun |
|---|---|
| `AutoSaveServiceTest` | `attach()`'i hiç çağırmıyor — 30 s timeline, odak kaybı ve `WINDOW_HIDDEN` testsiz. `aPostponedDraftNoLongerStopsAutosaveOfTheNewDocument` `hasUnsavedChanges()`'i mock'luyor, gerçek zincir atlanıyor. |
| `JTypeWriterAppTests.hasMainMethod` | `main`'in var olduğunu iddia ediyor — derlenen her giriş noktası için geçer; tautoloji. |
| `MainControllerTest` | Yalnız `titleFor`/`savedStatusFor`; tuş başına dinleyici, `refreshTitle`, `clearSaveStatus`, `toggleToolbar` kalıcılığı testsiz. |
| `KeyboardShortcutHandlerTest` | Yalnız `resolve()`; `handleKeyPress` (modifier guard, `event.consume()`, NONE tüketmemeli) testsiz. |
| `FileServiceTest` | Kayıt **hatası** (IOException/`showError` `:263-269`), `promptSaveOnExit`, servis üzerinden başarısız `AtomicFiles` yazımı testsiz. |
| `SessionStatsTest` | Editör→istatistik bağlantısı ve büyük girdi testsiz. |
| `MessagesTest` | Yalnız anahtar **kümeleri**; `MessageFormat` yer tutucuları/kaçışı doğrulanmıyor — I1 tam bu yüzden sevk edildi. |
| `restoreRecovered` testi | ASCII içerik → legacy kodlama dalı hiç çalışmıyor (V6). |
| `ToolbarFitTest` kabul testleri | AWT `TextLayout` + OS fontuna bağlı; "kırpılmamalı" iddiası runner fontuna duyarlı (64 px pay bunu hafifletiyor). |

### 9.5 Eksik test altyapısı (ürünleştirme boşlukları)

1. **Süreç düzeyinde öldürme testi yok.** Kurtarma yalnız `store.saveDraft` + yeni
   `RecoveryStore` ile simüle ediliyor. Uygulamayı fork edip yazı yazdırıp `SIGKILL` ile
   öldürüp yeniden başlatıp kurtarmayı doğrulayan test yok — tüm tasarımın var olma sebebi.
2. **TestFX/Monocle yok** → YOLO, undo-reset, kısayollar, diyaloglar, toolbar fit yalnız
   kod okuyarak doğrulanıyor.
3. **JaCoCo/kapsam kapısı/mutasyon testi yok**; `maven-enforcer` yok; AGENTS.md
   değişmezleri için ArchUnit kuralı yok.
4. **Hata enjeksiyonu yok:** enjekte edilen `IOException`, disk dolu, salt-okunur ev dizini
   (`InstanceLockTest` yalnız "kilit adında dizin" vakasını kapsıyor), `SecurityException`,
   yavaş dosya sistemi, yarıda kesilmiş `AtomicFiles.writeMoving` yok.
5. **Büyük belge testi yok** (10/50 MB), bellek/GC testi yok, açılış süresi testi yok.
6. **Tarih/saat biçimlendirmesi için locale matrisi yok** (I4), `LocaleManager` dinleyici
   sırasını doğrulayan test yok (M11).

### 9.6 AGENTS.md kurallarından testle zorlanmayanlar

"Düz `Files.write` kullanma" (statik test yok) · "hatalar loglanır ve gösterilir" (yalnız
biçimlendirici testli) · "testler headless" (var sayım, iddia değil) · "`AppGraph` alan
sırası bağımlılık sırası" (test yok) · "README iki dilde aynı bölüm sırası" (test yok) ·
"tüm paketlerde aynı anahtar" (test **var** — ve ölü anahtarları konserve ediyor).

Not: `.gitignore` `AGENTS.md`'yi dışladığı için CI'ın uygulaması gereken kurallar
sürüm kontrolünde değil.

---

## 10. Olgunluk Skorları

Önceki analizde genel skor **3,2 / 5** idi. Bu turda skorlar **düştü**, çünkü kod
kötüleşmedi — **ölçüm sıkılaştı**. Özellikle V1/V2 (kilit sözleşmesi, çıkış yolu), U1/U2
(ölçülmüş kontrast) ve B9–B12/B28–B32 (sürüm, imza, lisans) önceki turda bu ağırlıkta
sayılmamıştı.

| Boyut | Ağırlık | Puan | Δ | Gerekçe |
|---|---:|---:|---|---|
| Mühendislik temelleri | %15 | **4 / 5** | — | Atomik kayıt + dizin fsync, elle DI, tek yazma ilkesi, saf politika sınıfları, locale CI. FX thread'inde senkron G/Ç, `module-info` yokluğu, `AppLogging`'in enjekte edilemezliği puanı tutuyor. |
| Veri güvenliği | %20 | **3,5 / 5** | −0,5 | Kurtarma ve kodlama yolları sağlam. Ama V1 (kilit fail-closed / iki örnek), V2 (çıkışta taslak silme), V3 (dış değişiklik yok), V4 (sessiz autosave ölümü) doğrudan ürün vaadini çürütüyor. |
| Özellik bütünlüğü | %15 | **2,5 / 5** | — | Yeni Belge var; ara/değiştir, yazdırma, dışa aktarma, CLI, sekmeler, son dosyalar yok. |
| UX | %15 | **2,5 / 5** | −0,5 | Taşma stratejisi, tooltip, kalıcı görünüm, kayıt göstergesi iyi. Ama `.stats-save` **1,32:1**, temassız `Alert`, her açılışta modal karşılama, sabit boyutlu diyalog. |
| Erişilebilirlik | %10 | **2,5 / 5** | −0,5 | Odak halkaları + CSS tabanlı kontrast testi iyi fikir. Ama metin renkleri açık/sepya temada AA altı (2,32–3,74), erişilebilir adlar eksik, yüksek kontrast modu yok, RTL yok. |
| Dağıtım ve güven | %10 | **2 / 5** | −0,5 | İkon ve kilit var. Ama yayın yolu, tag, imza, notarization, checksum, SBOM, taşınabilir arşiv, jlink — hiçbiri yok. |
| Bakım yapılabilirlik | %10 | **3,5 / 5** | −0,5 | Temiz composition root, AGENTS.md, saf mantık testleri. Ama 8 ölü getter, üç ayrı atomik-taşı kopyası, üretimde test-only sınıf, tek pencere varsayımı, dispose sözleşmesi yok. |
| Hukuki | %5 | **2 / 5** | −0,5 | GPLv3 + `LICENSE` var. Ama binary'de lisans yok, `.deb` "License: Unknown", kaynak teklifi yok, WAV kaynağı bilinmiyor, POM metadata yok, SPDX yok. |

**Ağırlıklı genel: 2,95 ≈ 2,9 / 5** — "güvenilir bir kapalı beta, 1.0 değil".

### 10.1 Neden "motor ≠ ürün"

| Katman | Puan | Yorum |
|---|---:|---|
| Motor (veri yolu + mimari) | ~3,8 | P0 gerçekten iş yaptı; bu katman savunulabilir |
| Deneyim (UX + a11y) | ~2,5 | Ölçülen kontrast açıkları ve eksik editör tabanı |
| Ambalaj (dağıtım + hukuk) | ~2,0 | Ürünün **en zayıf** halkası; hiçbir kullanıcıya ulaşamaz |

Bu asimetri, ürünleştirme işinin nerede olduğunu tek bakışta söylüyor: **motor tarafında
değil.**

---

## 11. Go / No-Go

### 11.1 Hüküm

**Herkese açık `1.0.0`: HAYIR. Kapalı beta: EVET — P0 kapısı kapandığı için artık gerçekten
hazır.**

### 11.2 `1.0` için dar kapı

"Her şeyi bitir" gerekmiyor; altı madde yeterli:

1. **Çıkış yolu tek ve güvenli olsun** — V2 (kapatma isteği dışı çıkışta taslak silme) ve
   V1 (kilit sözleşmesi) kapatılsın; `Application.stop()` veya shutdown hook'u ile ikinci
   savunma hattı eklensin.
2. **Kurtarma yeni delik açmasın** — V6 (`restoreRecovered` legacy kodlamayı UTF-8'e
   çeviriyor) ve testi gerçek legacy içerikle çalışsın; V3 (dış değişiklik uyarısı) eklensin.
3. **Editör tabanı** — en az Ara/Değiştir (F1), komut satırından dosya açma (F4), yazdırma
   veya PDF (F2). Yazma aracı satan bir üründe bunlar pazarlık konusu değil.
4. **Okunabilirlik ve ilk kullanım gerçekten tamam olsun** — U1 (`.stats-save` 1,32:1) ve
   U2 (metin renkleri) düzeltilsin ve `ContrastRatioTest` bunları da denetlesin; U3
   (`Alert` temaları) ve U7 (açılışta editöre odak) kapansın; U4/U5 (taşan ve kırpılan
   karşılama/hakkında pencereleri) düzeltilsin; I1 (apostrof) düzeltilsin.
5. **Tek kanal taşınabilir dağıtım gerçekten tek olsun** — `package.sh` ve CI'dan `--installer`
   çıkarılsın; tag → GitHub Release + `tar.gz`/`.zip` + `SHA256SUMS` + imza; `LICENSE` ve
   `THIRD-PARTY-NOTICES` pakete girsin; README gerçeği anlatsın.
6. **Araç çubuğunun bir alt sınırı olsun** — `setMinWidth` ya da COMPACT altı davranış;
   "her genişlikte tek satır" ancak o zaman iddia edilebilir.

Kaba büyüklük **[tahmin]**: ~6–9 kişi-hafta, bunun yarısı ambalaj/hukuk (5. madde).

### 11.3 Beta için kabul edilebilir olanlar

Bunlar 1.0'ı bekleyebilir: sekmeler, dışa aktarma biçimleri, yazım denetimi, güncelleme
kontrolü, jlink, AppImage/Flatpak, arm64, RTL, `module-info`, ArchUnit, TestFX.

### 11.4 En yüksek getirili iki bahis

**1) "Taslağını asla kaybetmeyen editör" — güveni ürün yapın.**
Kod bu iddiaya çok yakın: üç seçenekli kurtarma, tek atomik taslak, toleranslı metadata,
oturum başına slot, park etme, tek örnek kilidi, sıkı BOM çözme. Kalan iş altyapı değil,
**kapatma**: (a) çıkış yolunu güvene al, (b) kurtarılan belgenin kodlamasını koru, (c) dış
değişiklik uyarısı ekle, (d) gerçek `kill -9` testini yaz. Bu nişte **hiçbir rakip
dayanıklılık satmıyor**, hepsi sadelik satıyor. Mühendislik yatırımı zaten bu eksende —
burada birinci olunabilir.

**2) Türkçe-öncelikli yerelleştirme — kutulanacak madde değil, ürün özelliği.**
Ana dil kalitesinde TR arayüz, iki dilli dokümantasyon, locale tuzaklarında kanıtlanmış
doğruluk. FocusWriter'ın çevirileri "idare eder" seviyesinde; bu nişi kazanmak ulaşılabilir.
**Ama** I1 (apostrof) tam da bu iddianın vitrininde — kodlama uyarı diyaloğunda — duruyor.

**Yapılmaması gereken:** 1.0'dan önce sekmelere/çoklu belgeye girişmek. Aylar yer ve
"kullanıcı kalır mı" sorusunu belirleyen sıkıcı düzeltmeleri geciktirir.

---

## 12. Bulgu Dizini

Aşağıdaki tablolar **her bulgunun kanıtını** taşır; §1–§11'deki anlatı bu dizinin üstüne
kuruludur. Kimlikler bölümlere karşılık gelir: **V** = veri güvenliği, **M** = mimari /
bakım, **F** = özellik, **U** = UX / erişilebilirlik, **I** = i18n, **B** = build /
dağıtım / hukuk.

| Bölüm | Kayıtlı bulgu | En yüksek önem | En kritik örnek |
|---|---:|---|---|
| Veri güvenliği (V1–V19) | 19 | Kritik | V2: çıkışta taslak silinmesi |
| Mimari / bakım (M1–M16) | 16 | Yüksek | M7: yansıma tabanlı undo sıfırlama |
| Özellik (F1–F14) | 14 | Kritik | F1: ara/değiştir yok |
| UX / erişilebilirlik (U1–U25) | 25 | Kritik | U1: `.stats-save` 1,32:1 |
| i18n (I1–I5 + I4'ün 10 alt maddesi) | 15 | Yüksek | I1: apostrof hatası |
| Build / dağıtım / hukuk (B1–B37) | 37 | Kritik | B9: yayınlanan sürüm yok |
| **Toplam açık** | **126** | — | **13 kritik + 28 yüksek + 85 orta/düşük** |

Sayılar önceki analizden yüksek çünkü **kapsam genişledi** (ölçülen kontrastlar, hukuk, CI
tedarik zinciri, i18n alt detayları, UX odak/ölçek sorunları) — kod kötüleşmedi.
Bunlara ek olarak **9 bulgu P0 ile kapandı** (§12.1).

### 12.1 Kapanan (P0 ile doğrulanmış)

| Bulgu | Kanıt |
|---|---|
| Düz `Files.write` ile yazma yok; tek atomik ilkel | `AtomicFiles.java:48-77` |
| Temp + fsync + atomik rename + dizin fsync | `AtomicFiles.java:56-71,124-133` |
| POSIX izin koruma (testli) | `AtomicFiles.java:85-121`; `AtomicFilesTest.java:92-106` |
| Kısa dosya adlarında temp prefix workaround | `AtomicFiles.java:53`; `AtomicFilesTest.java:70-76` |
| BOM'da sıkı çözme + kullanıcıya sorma | `TextCodec.java:92-107,169-180`; `FileService.java:277-288` |
| Bozuk ayar karantinası (üzerine yazmadan) | `AppSettings.java:231-284`; `AppSettingsTest.java:148-219` |
| Bozuk taslak metadata'sı taslağı düşürmüyor | `RecoveryStore.java:247-364` |
| "Sonra sor" otomatik kaydı susturmuyor (park) | `RecoveryStore.java:157-169`; `AutoSaveServiceTest.java:88-103` |
| Araç çubuğu taşma stratejisi + histerezis | `ToolbarFit.java:78,85,196-201`; `ToolbarFitTest.java:133-171` |

### 12.2 Açık — Kritik (13)

| ID | Bulgu | Kanıt | Bölüm |
|---|---|---|---|
| V1 | Kilit sözleşmesi ters: "fail open" javadoc, "fail closed" davranış; iki örnek senaryosunda yazar metni yedekte bile kalmıyor | `InstanceLock.java:21-26,64-68`; `JTypeWriterApp.java:20-24` | §4.2 |
| V2 | Kapatma isteği dışı her çıkış kirli taslağı sorusuz siliyor | `AutoSaveService.java:98,144-151`; `MainStage.java:68-73` | §4.2 |
| U1 | `.stats-save` için CSS kuralı yok → koyu temada **1,32:1** | `StatsBar.java:63`; `css/*.css` | §6.2 |
| F1 | Ara / değiştir yok | `src/main` (eşleşme yok) | §5.2 |
| F2 | Yazdırma / PDF yok | `src/main` (eşleşme yok) | §5.2 |
| F3 | Dışa aktarma yok (çıktı yalnız `.txt`) | `src/main` (eşleşme yok) | §5.2 |
| F4 | Komut satırından dosya açma yok; `getParameters()` hiç okunmuyor | `JTypeWriterApp.java:26`; `FxApplication.java:28-69` | §5.2 |
| B9 | Yayınlanan sürüm yolu yok; CI artefaktları 90 gün | `maven.yml:10-14,69-74`; `.gitignore:38-40` | §8.3 |
| B10 | Hiçbir yerde kod imzalama/notarization yok | `package.sh:109-144` | §8.3 |
| B28 | `LICENSE` ikiliye girmiyor; `.deb` telifi `License: Unknown` | `package.sh` (`--license-file` yok) | §8.6 |
| B29 | GPLv3 §6 kaynak teklifi yok | `LICENSE:245-291` | §8.6 |
| B30 | About diyaloğunda telif/lisans/garanti reddi yok (GPLv3 §0/§5d) | `AboutDialog.java:60-80` | §8.6 |
| B32 | `keystroke.wav` kaynağı/lisansı bilinmiyor | `src/main/resources/audio/keystroke.wav`; ilk commit | §8.6 |

### 12.3 Açık — Yüksek (28)

| ID | Bulgu | Kanıt |
|---|---|---|
| V3 | Dış değişiklik algılama yok | `FileService.java:232-270` |
| V4 | Otomatik kayıt sessizce ölebiliyor (3 yol) | `AutoSaveService.java:139-141,144-147,167-169` |
| V5 | Symlink hedefi değil link değişiyor | `AtomicFiles.java:64-66` |
| V6 | `restoreRecovered` legacy kodlamayı UTF-8'e çeviriyor; test dalı çalıştırmıyor | `FileService.java:189-194` |
| V7 | Yedek hatası kaydı iptal ediyor; yedek atomik değil | `FileService.java:253-257`; `BackupKeeper.java:23-30` |
| M3 | Tüm belge G/Ç'si FX thread'inde | grep: Executor/Task yok |
| M4 | Her tuşta O(n) dirty-check + `setTitle` | `MainController.java:171-177`; `FileService.java:81-85` |
| M5 | 1 Hz O(n) istatistik, kelime başına ayırma | `StatsBar.java:84-88`; `SessionStats.java:64-80` |
| M6 | 30 s'de tam belge serileştirme | `AutoSaveService.java:89,157-170` |
| M7 | `UndoHistoryReset` yansıma; başarısızlık okunmuyor | `UndoHistoryReset.java:41-71`; `EditorArea.java:109` |
| M9 | Tek belge/tek pencere varsayımı kök salmış | `FileService.java:62-66`; `RecoveryStore.java:95-101` |
| M15 | Büyük dosya stratejisi yok | `FileService.java:164`; `EditorArea.java:43` |
| U2 | Metin renkleri açık/sepya temada AA altı (2,32–3,74) | `TextColor.java:8-13`; `css/*.css` |
| U3 | `Alert` diyalogları temalandırılmıyor | `DialogService`; `CrashHandler`; `SingleInstanceWarning` |
| U4 | Karşılama **488×778**, Hakkında **568×848** — 768 px ekranı aşıyor, kaydırma yok | `WelcomeDialog.java:86,98`; `AboutDialog.java:115,127` |
| U5 | Kısayol bloğu sarmıyor → DE/FR/IT/ES'te 368–383 px satır, 340 px kutu | `WelcomeDialog.java:49-50`; `base.css:355-367` |
| U7 | Açılışta editöre odak verilmiyor (`requestFocus` hiç yok) | grep: eşleşme yok; `FxApplication.java:54-65` |
| I1 | `MessageFormat` apostrof hatası (EN/FR/IT — `{0}` düz metin) | `messages*.properties:84`; `Messages.java:395-397` |
| B13 | Linux yalnız Ubuntu 24.04 x86-64 | `.deb` control |
| B14 | macOS yalnız Apple Silicon | `maven.yml:25-26` |
| B21 | Paketlenmiş artefaktın açıldığı/kurulduğu doğrulanmıyor | `maven.yml:65-74` |
| B22 | Workflow seviyesinde `contents: write` | `maven.yml:17-18` |
| B23 | Action'lar mutable major tag ile pinlenmiş | `maven.yml:31,34,70` |
| B24 | Rozet betiği sabit repo + URL'de token + hatayı 0'a indiriyor | `publish-test-badge.sh:21,116,128-132` |
| B31 | JavaFX/FFmpeg/GStreamer bildirimleri JAR'da yok | JAR içerikleri |
| B33 | POM'da `<licenses>/<developers>/<scm>/<url>` yok | `pom.xml:7-12` |
| F5 | Yazım denetimi yok | — |
| F6 | Çoklu pencere / sekme yok | `MainController.java:160` |

### 12.4 Açık — Orta ve düşük (85)

**Veri/güvenlik (orta):** V8 (başarılı kayıt "başarısız"), V9 (kaydetme sonrası çökme eski
taslak), V10 (taslak başlığındaki keyfi yol), V11 (symlink takipli taslak taraması), V12
(`pending-*` birikmesi), V13 (fallback doğrulanmıyor + temp süpürme), V14 (boşluk
amplifikasyonu), V15 (`NaN`/`Infinity`), V16 (ISO-8859-1 + sessiz yazma hatası), V17
(`clear()` FINE), V18 (`SecurityException` → açılmıyor), V19 (testler gerçek ev dizinine).

**Mimari:** M1 (`AppLogging` enjekte edilemez), M2 (`AppGraph`/`FxApplication` testsiz),
M8 (FXML/Media ağırlığı), M10 (dispose sözleşmesi), M11 (dinleyici sırası), M12 (8 ölü
getter), M13 (`ContrastRatio` üretimde), M14 (ayar yazma amplifikasyonu), M16 (paket
döngüsü).

**Özellik:** F7 (son dosyalar), F8 (sürükle-bırak), F9 (menü çubuğu), F10 (belge içi
gezinme), F11 (uygulama içi sürüm), F12 (güncelleme kontrolü) — orta; F13 (tercihler
penceresi), F14 (RTL) — düşük.

**UX/a11y:** U6 (düğme/link/tagline kontrastı), U8 (araç çubuğu gizlenince odak),
U10 (dil değişimi 1 s gecikme), U11 (`Alert` default/cancel), U12 (Yeni/Farklı Kaydet yalnız
klavyede), U13 (11 px UI, ölçeklenmiyor), U19 (her açılışta modal karşılama), U20 (COMPACT
altı ölçülmemiş), U21 (erişilebilirlik adları), U22 (yüksek kontrast), U23 (kısayol
keşfedilebilirliği + yanlış liste), U25 (her tuşta ses) — orta; U9 (ilk saniye boş durum
çubuğu), U14 (tema döngüsü sırası), U15 (YOLO'da ses), U16 (başlık sonundaki işaretler),
U17 (geçersiz CSS özellikleri), U18 (sağ tık menüsü dili), U24 (teknik/İngilizce hata
metinleri) — düşük.

**i18n:** I2 (`statsFormat` varsayılan yerel ayar), I3 (`MessagesTest` kapsamı), I4 (yerel
ayar boşlukları — 10 alt madde), I5 (ölü anahtarlar).

**Build/dağıtım:** B1–B8 (jlink yok, fat JAR/classpath, FXML/Media, wrapper/enforcer yok,
yeniden üretilebilirlik, `.gitattributes`, ikon `--check`, platform araçları), B11 (checksum/
SBOM/provenance), B12 (taşınabilir Linux artefaktı yok), B15 (Windows ARM64), B16
(`~/.jtypewriter`), B17 (native cache/WAV temp), B18 (Wayland), B19 (desktop entegrasyonu),
B20 (Java 25 kullanıcı gereksinimi gibi), B25 (9 kez test), B26 (retention yok), B27 (sürüm
mühendisliği yok), B34 (SPDX), B35 (ikon kaynağı), B36 (gizlilik notu), B37 (marka).

**Test altyapısı:** süreç öldürme testi yok, TestFX/Monocle yok, JaCoCo/mutasyon kapısı yok,
hata enjeksiyonu yok, büyük belge testi yok, tarih/saat locale matrisi yok, `LocaleManager`
dinleyici sırası testi yok.

---

## 13. Kapsam Dışı — Yol Haritası

Bu belge **yeni bir iş paketi tanımlamıyor**. §11.2'deki altı madde bir *kapı* tanımıdır,
faz planı değil. P1 ve sonrasının sıralaması, kabul kriterleri ve sahiplenmesi ayrı bir
adımda ele alınacaktır. Buradaki bulgu kimlikleri (V*, M*, U*, I*, B*, F*) o çalışmada
referans olarak kullanılabilir.
