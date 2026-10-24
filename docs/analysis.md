# JTypeWriter — Kapsamlı Analiz ve Ürünleştirme Değerlendirmesi

> Tarih: 25 Ekim 2026 · Sürüm: `1.0.0` (`pom.xml:9`) · Kapsam: tüm `src/main`, `src/test`, `pom.xml`, `scripts/`, `.github/workflows/maven.yml`, `README.md`, `README-TR.md`, `docs/`
> Ölçülen büyüklük: **43 üretim sınıfı / 5.936 satır**, **25 test sınıfı / 3.379 satır**, **218 test**, **90 bundle anahtarı** (altı dilde eşit).
> Yöntem: üç bağımsız alan denetimi (veri güvenliği · arayüz/erişilebilirlik/i18n · yapı/paketleme/hukuk) + her kritik iddianın `file:line` düzeyinde ikinci kez elden doğrulanması.
> **Bu doküman tam bir yeniden yazımdır.** Önceki sürüm 24 Ekim sabahına, yani **P0 kapanmadan önceye** aitti: 176 test / 22 sınıf diyor, tooltip'in, odak halkalarının ve araç çubuğu taşma stratejisinin olmadığını söylüyordu. Üçü de artık yanlış. Aşağıdaki her bulgu bugünkü koda karşı yeniden doğrulandı; hiçbir eski iddia taşınmadı.
> Faz planı, sıralama ve kabul kriterleri için tek doğruluk kaynağı **`docs/product-roadmap.md`**'dir. Bu doküman onun **gerekçesini ve kanıtını** tutar, iş listesi tutmaz.

---

## 1. Yönetici Özeti

JTypeWriter, mühendislik disiplini ürün olgunluğunun **önünde giden** bir JavaFX metin editörü. Elle kurulan temiz bir composition root (`AppGraph`), atomik yazma + `fsync` + dizin flush, üç nesil yedek rotasyonu, oturum başına atomik kurtarma dosyası, kodlama korumalı G/Ç ve 218 headless test — üstelik üç locale'de (varsayılan, `tr_TR`, `de_DE`) yeşil. Bu taban, çoğu ticari masaüstü editöründen daha titiz.

**P0 kapısı kapandı.** 20 iş paketi (`RW-01`…`RW-20` + `RW-54`) tamamlandı. Son haftada ürünün görünür yüzeyi değişti:

- Araç çubuğu artık **her genişlikte tek satırda kalıyor**: sığmadığında altı açılır listenin başlığı değere iniyor (FULL→COMPACT), 40 px histerezisle titreme engelleniyor (`ToolbarFit.java:78,85,196-201`). Ölçüm AWT ile yapıldığı için test **başsız** koşuyor (`ToolbarFit.java:203-213`).
- 11 kontrolün tamamında bundle kaynaklı **tooltip** var ve **Tab sırası** `viewOrder` ile sabitlendi (`ToolbarView.java:221-245`).
- **Odak halkaları geri geldi**: `-focus-ring` tema token'ı, `:focused` kuralları, ve `ContrastRatioTest` gerçek CSS'i ayrıştırıp AA eşiğinin altına düşüşü yakalıyor (`ContrastRatioTest.java:72-144`).
- **Görünüm durumu kalıcı**: araç çubuğu görünürlüğü, tam ekran ve istatistik çubuğu boyutu yeniden başlatmada korunuyor (`MainStage.java:62-66`, `MainController.java:65-67,164-165`).
- **Kayıt göstergesi** var: `FileService.onSaved` (kirli→temiz anı) üzerinden "Kaydedildi 12:04"; otomatik kayıt taslağının tik'i değil (`MainController.java:84-91,154-156`).
- **BOM'lu yolda sıkı çözme**: bozuk baytlar artık sessizce `U+FFFD` olmuyor, kullanıcıya soruluyor (`TextCodec.java:175-179`, `FileService.java:277-288`).

Buna karşılık ürün hâlâ **"editör" olarak eksik**: ara/değiştir, sekmeler, son dosyalar, sürükle-bırak, yazdır/dışa aktarma yok. Dağıtım tarafında imza, tag ve checksum yok; alınan **"yalnız portable"** kararı (`product-roadmap.md:92`) hâlâ koda yansımadı — `package.sh` `.deb`/`.msi`/`.dmg` üretmeye devam ediyor (`package.sh:148-166`) ve README bunu anlatıyor (`README.md:214,224-226`).

Bu turda **üç yeni, somut kusur** bulundu; hiçbiri önceki analizde yoktu:

1. **`savedStatus` etiketinin teması yok.** `StatsBar` `"stats-save"` stil sınıfını ekliyor (`StatsBar.java:63`) ama hiçbir CSS dosyasında `.stats-save` kuralı yok. Yani RW-19 ile eklenen metin, doğrulanmış `-stats-text` rengini **almıyor** ve `ContrastRatioTest` onu hiç görmüyor. RW-17'nin kontrast kazanımı bu tek etikette delik.
2. **`MessageFormat` apostrof tuzağı kullanıcıya görünüyor.** `encodingMalformedContent` deseni EN/FR/IT'de kaçışsız kesme işareti içeriyor (`messages.properties:84`, `messages_fr.properties:84`, `messages_it.properties:84`). `MessageFormat` `'` karakterini alıntı başlatıcısı saydığı için EN'de `"This file's"` → `"This files"` oluyor; FR/IT'de ilk `{0}` **literal** kalıyor. Yani bozuk BOM'lu bir dosyayı açmaya çalışan kullanıcı, tam da veri kaybı riskini anlatan diyalogda bozuk metin görüyor.
3. **Araç çubuğunun alt sınırı yok.** Politika COMPACT'ın da sığmadığı durumu "yine de cevap bu" diye geçiyor (`ToolbarFit.java:193-195`) ve hiçbir yerde `setMinWidth` yok. Pencere yeterince daraltılırsa kırpılma geri geliyor — "her genişlikte tek satır" iddiasının ölçülmemiş kısmı bu.

**Karar: herkese açık `1.0.0` olarak yayınlanmamalı; genel beta (`0.9`) yayınlanabilir.** Olgunluk skoru **3,2 / 5** (önceki: 3,0). `1.0` kapısı artık "her şeyi bitir" değil, **altı maddelik dar bir liste** (§12).

En yüksek getirili iki bahis değişmedi: **(1) "taslağını asla kaybetmeyen editör" güven hikâyesini kapatmak**, **(2) Türkçe-öncelikli yerelleştirmeyi ürün özelliği olarak satmak.**

---

## 2. Ürün Kimliği ve Pazar Konumu

### 2.1 Ne satıyor?

1. **Odaklanma / sade arayüz** — tek `EditorArea` (`AppGraph.java:62`), 3 tema, 6 metin rengi, istatistik çubuğu, tam ekran.
2. **Daktilo hissi** — her tuş vuruşunda `keystroke.wav` (`TypewriterSound.java:31`); YOLO modu (silme/geri alma kilitli, yalnız ileri yazma — `YoloPolicy`).
3. **Veri güvenliği** — atomik kayıt, 3 nesil yedek, oturum başına kurtarma taslağı, kodlama koruma, tek örnek kilidi.

Bunlardan **yalnız üçüncüsü savunulabilir bir farklılaştırıcı**; ilk ikisi bir hafta sonu projesinde taklit edilebilir. Son iki haftanın işi tam olarak bu eksende yapıldı ve doğru bir yatırımdı.

### 2.2 Kimin için?

Kod tabanının en güçlü sinyali **Türkçe-öncelikli** olması: `Messages` `getNoFallbackControl` ile sistem locale'ine sessiz düşmeyi engelliyor (`Messages.java:20-21`), dil çözümlemesi `Locale.ROOT` ile yapılıyor (`Messages.java:45-46` — Türkçe noktalı-i tuzağı), CI `tr_TR` ve `de_DE`'de yeniden koşuyor (`.github/workflows/maven.yml:45-49`), altı bundle 90 anahtarın tamamında eşleşiyor ve `MessagesTest` bunu zorluyor (`MessagesTest.java:31-39`). Dokümantasyon iki dilli: `README.md` (EN) + `README-TR.md` (TR), aynı bölüm sırasıyla.

Hedef kitle: **yerel-öncelikli, hesapsız, çevrimdışı yazma yüzeyi isteyen yazarlar/öğrenciler** — özellikle Türkçe konuşanlar.

### 2.3 Rekabet

| Rakip | Gücü | JTypeWriter'ın şansı |
|---|---|---|
| **FocusWriter** | Olgun, özellik dolu, 20+ dil, oturumlar | Özellikte **kazanamaz**. Duygu, Türkçe kalitesi ve veri dayanıklılığında kazanabilir |
| **iA Writer** | Tasarım ikonu, Markdown, Apple ekosistemi | Ulaşılamaz; bu rekabete girilmemeli |
| **Typora / Obsidian** | Markdown + bilgi yönetimi | Yanlış savaş; plugin/Markdown altyapısı yok |
| **ghostwriter** | KDE/Markdown, ücretsiz | "Markdown zorunlu değil" ve Windows/macOS'ta kazanabilir |
| **Hemingway** | Okunabilirlik geri bildirimi, ücretli web | Çakışma yok |

**Sonuç:** Konumlandırma "bir başka distraction-free editör" değil, **"Türkçe + Avrupa dilleri için güvenli, yerel, daktilo hisli editör"**. FocusWriter'ın özellik yarışına girmek kaybedilen bir yatırımdır.

---

## 3. Mimari Değerlendirme

### 3.1 Güçlü yanlar

- **Composition root disiplini.** `AppGraph` elle DI yapıyor: her bileşen tek `public final` alan, **alan sırası = bağımlılık sırası**, dinleyiciler constructor'da bağlanıyor. Spring'den kurtulma kararı doğruydu.
- **Durum türetme.** Kirli bayrak ayrı tutulmuyor; `hasUnsavedChanges()` iki string'i karşılaştırarak türetiyor (`FileService.java:81-84`). Tek doğruluk kaynağı.
- **Tek senkron noktası.** Araç çubuğundaki 11 kontrolün tamamı `ToolbarView.refresh()` üzerinden yenileniyor; açılır liste seçimleri, kısayollar ve dil değişimi aynı yola giriyor (`ToolbarView.java:233-256,358-368`). "İki kaynak, bir etiket" sınıfı hata yapısal olarak kapandı.
- **Test edilebilirlik için saf mantık ayrımı.** `MainController.titleFor`/`savedStatusFor`, `MainStage.intersectsAny`/`maximizeOnRestore`, `KeyboardShortcutHandler.resolve`, `YoloPolicy`, `DialogService.choiceFor`, `RecoveryStore` çözümleyicileri, `ToolbarFit.decide`, `StatsBarState` eşlemesi — hepsi headless test edilebilir. `AGENTS.md` bunu kural hâline getirmiş ve `MainStageTest`/`MainControllerTest`/`ToolbarFitTest`/`StatsBarStateTest` bunu fiilen uyguluyor.
- **YOLO tek huniden geçiyor.** `EditorArea.replaceText` tüm yıkıcı düzenlemelerin tek kesişimi; `clear()` ve undo/redo ayrıca kapatılmış (`EditorArea.java:74-123`).
- **Ölçüm bağımsız, politika saf.** `ToolbarFit` JavaFX'e hiç dokunmuyor; AWT `TextLayout` kullanıyor (`ToolbarFit.java:203-222`). Bu, "arayüz test edilemez" varsayımını kıran doğru bir desen.

### 3.2 Yapısal borç

1. **Tek pencere / tek belge varsayımı her yere işlemiş.** Tek `EditorArea` (`AppGraph.java:62`), tek `Stage` (`AppGraph.java:82`), tek `currentFilePath` (`FileService.java:62`), tek `Timeline` (`AutoSaveService.java:89-91`). Sekme/çoklu belge **yama değil yeniden tasarım**.
2. **Depolama soyutlaması yok.** `FileService` doğrudan `Files`/`AtomicFiles` kullanıyor, `BackupKeeper` içeride kuruluyor, `RecoveryStore` varsayılan olarak `AppDirs.recoveryDir()`. Bulut/şifreli/alternatif depolama eklemek bu sınıfları değiştirmek demek.
3. **Yaşam döngüsü sözleşmesi yok.** `StatsBar` `Timeline`'ı (`StatsBar.java:84-88`), `AutoSaveService` `Timeline`'ı, `TypewriterSound` `AudioClip`'i bırakılmıyor. Tek pencere için zararsız; çoklu pencere/test izolasyonu için tuzak.
4. **`AppDirs` hâlâ her işletim sisteminde `~/.jtypewriter`** (`AppDirs.java:14-16`). Portable hedefin merkezî engeli. `AppSettings`/`RecoveryStore`/`InstanceLock` yol enjekte edilebilir; **`AppLogging` edilemez** — yalnız parametresiz `init()` var ve `AppDirs.logsDir()`'a sabit (`AppLogging.java:121-137`). `logger()` otomatik init ettiği için (`AppLogging.java:152-155`) testlerdeki statik `LOG` alanları bile gerçek `~/.jtypewriter/logs` dizinini oluşturuyor. AGENTS.md'nin "@TempDir ile izole et" kuralı **loglama için ihlal edilemez durumda**. Portable F1.2'nin ilk somut işi budur.
5. **`module-info` yok, fat JAR + classpath.** `javafx-media` tek bir WAV tık sesi için taşınıyor (`pom.xml:34-38`, `TypewriterSound.java:3`); `javafx-fxml` ise yalnız **boş** `main.fxml`'i yüklemek için duruyor (`main.fxml:1-7` gerçekten boş bir `BorderPane`, `FxApplication.java:30-42`). jlink/native-image yolu kapalı.
6. **JavaFX kontrolleri `Application.init()` içinde kuruluyor.** `FxApplication.java:20-25` içinde `AppLogging.init(); app = new AppGraph(); app.crashHandler.install();`. `init()` FX Application Thread'inde değil, Launcher thread'inde çalışır; bugün çalışıyor ama garanti edilen bir sözleşme değil.

### 3.3 Threading ve ölçeklenme

Tüm belge G/Ç'si **FX thread'inde senkron**; hiçbir yerde `Task`/executor/`CompletableFuture` yok. Engelleme noktaları: `Files.readAllBytes` (`FileService.java:164,189`), `AtomicFiles.writeMoving` + `fsync` (`FileService.java:257`), taslak yazımı (`AutoSaveService.java:157-166`). Autosave tetikleyicileri de (30 sn'lik `KeyFrame`, odak kaybı, `WINDOW_HIDDEN`) FX thread'inde (`AutoSaveService.java:30,89-97`).

**İstatistik tarafı gerçekten O(n).** `SessionStats.getWordCount()` her çağrıda `text.trim().split("\\s+")` yapıyor (`SessionStats.java:60-67`); `getCharCountNoSpaces()` her çağrıda yeni string üretiyor, `getLineCount()` her çağrıda `split("\n", -1)` (`SessionStats.java:73-79`). `StatsBar` bunu **saniyede bir** çağırıyor (`StatsBar.java:84-88`), debounce yok. 50 MB'lık bir belgede bu, saniyede bir tam belge taraması demek — P1'in "50 MB'de UI donmuyor" kriteriyle doğrudan çelişir.

**Kirli kontrolü abartılmamalı.** `hasUnsavedChanges()` her tuşta çağrılıyor (`MainController.java:171-173,137`) ama `String.equals` uzunluklar farklıysa ilk adımda dönüyor (`FileService.java:81-84`). Yani "her tuşta tam string karşılaştırması" ekleme/silme için yanlış; **aynı uzunluktaki** düzenlemelerde (seçimi değiştir, yaz-sil) doğru. Asıl maliyet `stage.setTitle(...)`'ın her tuşta çalışması.

---

## 4. Veri Güvenliği — Çekirdek ve Kalan Açıklar

Kayıt yolu gerçekten iyi: hedef dizinde temp dosya → `FileChannel.force(true)` → izin/ACL koruma → `ATOMIC_MOVE` → **dizin fsync**; hata hâlinde temp temizliği (`AtomicFiles.java:49-76`). Yazma kodlamada sıkı `REPORT` kullanıyor: temsil edilemeyen karakter `?` ile sessizce ikame edilmiyor, kullanıcıya UTF-8'e yükseltme soruluyor (`TextCodec.java:135-149`, `FileService.java:273-297`). Okuma tarafı da artık BOM'lu yolda sıkı (`TextCodec.java:169-180`). Bunlar doğru kararlar.

### 4.1 Kapanan yollar

| # | Eski bulgu | Bugünkü durum ve kanıt |
|---|---|---|
| A | Kurtarma diyaloğu kapatılınca taslak siliniyordu | **Kapandı.** Üç seçenek: geri yükle / sil / sonra sor; kapatma (X/Escape) `LATER` sayılıyor ve taslağa dokunmuyor (`DialogService.java:195-213`, `RecoveryChoice`). `DialogServiceTest` regresyon altında |
| B | Bozuk metadata tüm taslağı düşürüyordu | **Kapandı.** Magic/bölücü yoksa tüm içerik metin olarak sunuluyor; bozuk `saved.at` → dosya mtime; bozuk/eksik yol → `null`; okunamayan mtime → `Instant.EPOCH`; geçersiz UTF-8 → değiştirme karakterleriyle (`RecoveryStore.java:247-292,294-304,312-323,340-347,353-364`) |
| C | Taslak + metadata iki ayrı atomik yazmaydı | **Kapandı.** Tek `draft-<uuid>.jtw` dosyası, tek `AtomicFiles.writeMoving` (`RecoveryStore.java:105-107`) |
| D | Sabit `draft.txt`, iki örnek birbirini eziyordu | **Kapandı.** Oturum başına UUID'li dosya; `loadDraft` en yenisini seçer (`savedAt` → mtime → dosya adı), başka oturumun taslağını kendi slotuna devralır; `clear()` yalnız kendi dosyasını siler (`RecoveryStore.java:116-143,181-188,228-239`) |
| E | Tek örnek kilidi yoktu | **Kapandı (davranış olarak).** `InstanceLock` advisory `FileLock` ile `~/.jtypewriter/instance.lock` üzerinde; graph kurulmadan alınıyor, ikinci örnek yerelleştirilmiş uyarıyla çıkıyor (`JTypeWriterApp.java:20-24`) |
| F | Bozuk ayarlar sessizce eziliyordu | **Kapandı.** `settings.properties.corrupt` (+ çakışmada `.corrupt.<millis>`) karantinası; taşınamazsa iki uyarı loglanıp dosya yerinde bırakılıyor (`AppSettings.java:231-265`) |
| G | Log'da yığın izi yoktu | **Kapandı.** Girintili tam yığın izi + `Caused by:` zinciri; biçimlendirici asla fırlatmıyor (`AppLogging.java:52-110`) |
| H | "Sonra sor" yanıtı otomatik kaydı tamamen susturuyordu | **Kapandı.** Ertelenen taslak `pending-<uuid>.jtw` olarak park ediliyor, aktif slot serbest kalıyor ve snapshot'lar devam ediyor (`RecoveryStore.java:157-169`, `AutoSaveService.java:139-141`). `draftFrozen` yalnız park **başarısız** olursa devreye giriyor (`AutoSaveService.java:140,148-150,158-160`) |
| I | BOM'lu yolda lenient decode | **Kapandı.** BOM'lu baytlar `decodeBomStrict` ile çözülüyor; bozuk baytta `MalformedDocumentException` fırlatılıp WARNING loglanıyor, kullanıcıya soruluyor (`TextCodec.java:169-180`, `FileService.java:277-288`) |
| J | Araç çubuğu taşma stratejisi / tooltip / Tab / odak halkaları / kontrast yoktu | **Kapandı.** `ToolbarFit` + 11 tooltip + `viewOrder` + `-focus-ring`; AA oranları light 4.68 / dark 5.15 / sepia 4.83 ve `ContrastRatioTest` gerçek CSS'i denetliyor (`ContrastRatioTest.java:72-144`) |
| K | Görünüm durumu kalıcı değildi | **Kapandı.** `toolbar.visible`, `window.fullscreen`, `stats.size` write-through; tam ekran `fullScreenProperty` dinleyicisiyle **anında** kaydediliyor, çıkışta değil (`MainStage.java:62-66`) |

### 4.2 Kalan açıklar — önem sırasıyla

**A. Dış değişiklik algılama yok.** *(Açık, yüksek)*
`loadFile` diskteki dosyanın mtime/boyut/hash'ini kaydetmiyor (`FileService.java:162-179`); `writeFile` de kaydetmeden önce karşılaştırmıyor (`FileService.java:232-270`). Repo genelinde `WatchService|getLastModifiedTime|MessageDigest` araması yalnız `RecoveryStore.java:342`'de eşleşiyor. Bulut senkronizasyonu veya başka bir editör aynı dosyayı değiştirdiyse **sessizce üzerine yazılıyor**. "Güvenli editör" iddiasının en büyük deliği budur.

**B. Kurtarılan belge, okunamayan orijinali UTF-8 olarak yazabilir.** *(Açık, orta-yüksek)*
`restoreRecovered`'da `currentFilePath = file` ataması **koşulsuz** — `TextCodec.decode` `null` dönerse bile (`FileService.java:188-194`). `currentCharset`/`currentBom` yalnız `onDisk != null` iken güncelleniyor; taze bir örnekte bunlar `UTF_8`/`false` (`FileService.java:65-66`). Sonuç: **BOM'suz Windows-1254** bir dosyanın taslağı geri yüklendiğinde sonraki kayıt onu UTF-8'e çevirir. Sınıf yorumu (`FileService.java:181-185`) yalnız BOM/UTF-8 orijinaller için doğru. `MalformedDocumentException` dalında da yol atanıyor (`FileService.java:195-201`); yalnız `IOException` dalında atanmıyor. **Not:** `FileServiceTest`'in bu yolu sınadığı iddia edilen testi ASCII metinle yazıyor (`"published chapter"`), ASCII her iki kodlamada da geçerli olduğu için legacy dalı **hiç çalışmıyor** — yani bu hata test tarafından görülmüyor.

**C. `ATOMIC_MOVE` desteklenmeyen dosya sistemlerinde doğrulama yok.** *(Açık, orta)*
`AtomicMoveNotSupportedException` → düz `Files.move(REPLACE_EXISTING)` (`AtomicFiles.java:67-68`). Sonrasında **hiçbir SHA-256/boyut doğrulaması yok** (repo genelinde `MessageDigest|SHA-256` eşleşmiyor). FAT32/exFAT USB'de "yarım dosya yok" garantisi zayıflıyor ve bu, portable hedefte kenar durum değil **ana yol**.

**D. `savedStatus` etiketi temalanmıyor.** *(Açık, orta — bu turda yeni bulundu)*
`StatsBar` `"stats-save"` sınıfını ekliyor (`StatsBar.java:63`) ama `base.css`/`light.css`/`dark.css`/`sepia.css` içinde `.stats-save` kuralı yok; `applySize()` ona yalnız font boyutu veriyor (`StatsBar.java:98`), `.stats-clock`/`.stats-label` gibi `-fx-text-fill: -stats-text` almıyor (`base.css:311-321`). Yani RW-19'un ürettiği metnin rengi Modena varsayılanına kalıyor ve `ContrastRatioTest` onu denetlemiyor. RW-17'nin kontrast kazanımı bu tek etikette delik.

**E. Açılışta `jtw-*.tmp` temizliği yok.** *(Açık, düşük-orta)*
Temizlik yalnız çağrının `finally` bloğunda (`AtomicFiles.java:72-76`); süreç ölürse `jtw-<ad><rastgele>.tmp` belge dizininde kalıyor ve hiçbir yerde süpürülmüyor (`"jtw-"` araması yalnız `AtomicFiles.java:53`). Çökmede tam belge metni diskte kalır — gizlilik ve dağınıklık sorunu.

**F. Yalnız en yeni taslak sunuluyor; eski `draft-*` dosyaları ne sunuluyor ne temizleniyor.** *(Açık, düşük)*
`loadDraft` tek `Optional<Draft>` döndürüyor (`RecoveryStore.java:116-143`). Silme noktaları yalnız kendi slotu (`:183`) ve devralınan kazanan (`:202`). Başka UUID'lerden kalan taslaklar `~/.jtypewriter/recovery/` altında süresiz birikir.

**G. Yedek dosyası atomik değil.** *(Açık, düşük)*
`BackupKeeper` düz `Files.copy` kullanıyor, temp/fsync/`ATOMIC_MOVE` yok (`BackupKeeper.java:23-30`). Ayrıca `backup()` **hiç logger kullanmıyor** ve `IOException` fırlatıyor; `FileService` onu asıl yazmadan **önce** çağırdığı için (`FileService.java:253-257`) **yedek hatası kaydı iptal ediyor** ve belge kirli kalıyor.

**H. Geometride `NaN`/`Infinity` ayıklaması yok.** *(Açık, düşük)*
`optionalDouble` `Double.parseDouble` sonucunu olduğu gibi kabul ediyor (`AppSettings.java:323-333`); `getWindowState` yalnız varlığa bakıyor (`:191-203`). `NaN` alt tarafta tesadüfen eleniyor (`MainStage.java` boyut kontrolleri ve `intersectsAny` karşılaştırmaları), ama **`+Infinity`** boyutlu bir ayar `stage.setWidth/setHeight`'e ulaşabiliyor.

**I. Adoption, ikinci bir örnek çalışsaydı canlı taslağı silerdi.** *(Açık, teorik)*
`adoptIfForeign` diğer oturumun dosyasını kendi slotuna kopyaladıktan sonra `Files.deleteIfExists(winner.file())` çağırıyor (`RecoveryStore.java:196-206`). Bugün bu senaryo erişilemez, çünkü `InstanceLock` alınamazsa `JTypeWriterApp` çıkıyor (`JTypeWriterApp.java:20-24`). Ama **kilit davranışı ile javadoc çelişiyor**: sınıf yorumu "uygulama yine de başlar" diyor (`InstanceLock.java:21-26`), gerçek ise fail-closed. `InstanceLockTest` yalnız `Optional.empty()`'i doğruluyor (`InstanceLockTest.java:62-68`), uygulama düzeyindeki sonucu değil. Kilit yolu bir gün gevşetilirse veri kaybı kapısı açık.

**J. Symlink semantiği.** *(Açık, düşük)*
`Files.move(REPLACE_EXISTING)` sembolik bağlantıyı hedefin yerine koyar; bağlantı üzerinden kaydetme beklenmedik sonuç üretir. Test edilmemiş.

---

## 5. Özellik Bütünlüğü

### 5.1 Var olanlar (ve gerçekten çalışanlar)

Dosya aç/kaydet/farklı kaydet, **Yeni Belge (`Ctrl+N`)** + belge yüklerken undo geçmişini sıfırlama, oturum geri yükleme, atomik kayıt + 3 nesil yedek, otomatik kayıt + üç seçenekli kurtarma, kodlama tespiti/koruma/UTF-8'e yükseltme/BOM'da sıkı çözme, 3 tema + 6 metin rengi + 19 kademelik yazı boyutu merdiveni (10-48), 6 dilde anlık değişim, daktilo sesi, YOLO modu, canlı istatistik, tam ekran, pencere geometrisi ve görünüm durumu hatırlama, kayıt göstergesi, araç çubuğu taşma yönetimi + tooltip + Tab gezinmesi, hoş geldin/hakkında diyalogları, uygulama ikonu, çökme yakalayıcı, tek örnek kilidi.

### 5.2 Olmayanlar — bir metin editörünün asgari tabanı

| Eksik | Etki | Kanıt |
|---|---|---|
| **Ara / Değiştir** | Hiç yok; 10k satırlık bir belgede gezinme imkânsız | `src/main` (eşleşme yok) |
| **Sekmeler / çoklu belge** | Açmak mevcut tamponu eziyor | Tek `currentFilePath` (`FileService.java:62`), tek `EditorArea` (`AppGraph.java:62`) |
| **Son dosyalar** | Yalnız `last.file` + `last.directory` | `AppSettings.java:62-76` |
| **Sürükle-bırak** | `setOnDragOver`/`setOnDragDropped` yok | `src/main` |
| **Yazdır / dışa aktar** | Yok (düz metin/MD/PDF/HTML) | — |
| **Yazım denetimi** | Yok | — |
| **Tercihler penceresi** | Altı ayar araç çubuğunda açılır liste; merkezî form yok | `ToolbarView` |
| **Dosya ilişkilendirmesi / CLI argümanı** | `getParameters()` hiçbir yerde okunmuyor; `.txt` çift tıklama çalışmaz (portable kararıyla **bilinçli kapsam dışı**) | `JTypeWriterApp.java:26` |

### 5.3 README ile gerçek arasındaki farklar

- **Bu turda kapatıldı:** mimari ağacında **6 sınıf eksikti** (`RecoveryChoice`, `AppIcons`, `UndoHistoryReset`, `StatsBarState`, `ToolbarFit`, `ContrastRatio`); ağaç artık kaynak ağacıyla birebir (commit `8421163`). Temel Bileşenler tablosunda `StatsBar` ve `TextCodec` açıklamaları eskiydi, düzeltildi. Test Sınıfları tablosunda **`AppIconsTest` eksikti** (24 satır / 25 sınıf), eklendi.
- **Açık (portable kararıyla ilgili):** README hâlâ `package.sh --installer` çıktısını ve fat JAR'ı olağan artefakt olarak anlatıyor (`README.md:214,219-226,245-247`; `README-TR.md:215,220-226,246-248`). Roadmap §2.1 installer'ı kapsam dışı ilan ediyor; bu bölüm F5.3'te yeniden yazılmalı.
- **Açık (küçük):** README'ler `MessageFormat` tuzağını ya da `.stats-save` eksiğini anlatmıyor — doğru, ama kullanıcıya yansıyan iki kusur oldukları için §4'te düzeltilmeleri gerekiyor.

---

## 6. UX ve Erişilebilirlik

### 6.1 Kapanan işler

- **Araç çubuğu taşması çözüldü.** Sabit tek satır; sığmadığında yalnız **başlıklar** değere iniyor (COMPACT), beş düğme ve altı liste hiç kaldırılmıyor (`ToolbarFit.java:14-21,196-201`). 40 px histerezis titremeyi engelliyor. Kabul testleri Almanca ve Fransızcanın 1280 px'te kırpılmadığını, daraltılmış satırın altı dilde 900 px'e sığdığını kanıtlıyor (`ToolbarFitTest.java:133-171`). Chrome sabitleri CSS ile hizalı ve yorumda bu açıkça yazılı (`ToolbarFit.java:44-69`).
- **Tooltip'ler geldi.** 11 kontrolün hepsinde, metin `Messages`'tan, kısayol ipucuyla ("Save (Ctrl+S)"), her `refresh()`'te yeniden okunuyor (`ToolbarView.java:221-231,398-400`).
- **Tab ile erişim geldi.** Sıra `viewOrder` ile soldan sağa sabitlendi; `setNextFocus` JavaFX 26'da yok (kaynak jar'a karşı doğrulandı) ve repo genelinde `setFocusTraversable|focusTraversable` eşleşmesi **sıfır** (`ToolbarView.java:233-245`).
- **Odak halkaları geri.** `-focus-ring` token'ı temaya bağlı; butonlar, altı liste, popup satırı ve editör için odak göstergesi var (`base.css:9-12,32-34,113-117,159-164,246-250`). `-fx-focus-color: transparent` yeniden girmesini `ContrastRatioTest` engelliyor (`:118-125`).
- **Kontrast ölçülü ve test edilmiş.** Durum metni light 4.68 / dark 5.15 / sepia 4.83; odak halkası ≥3.0; halka üstü metin ≥4.5 (`light.css:10-17`, `dark.css:10-15`, `sepia.css:10-17`, `ContrastRatioTest.java:72-116`). Varsayılan durum yazısı 13 px (`StatsBar.java:41-44`).
- **Görünüm durumu kalıcı.** Araç çubuğu görünürlüğü, tam ekran, istatistik boyutu write-through; `maximizeOnRestore` tam ekran ile maximize'ın aynı anda uygulanmasını engelliyor (`MainStage.java:87-89`, `MainStageTest.java:52-58`).
- **Kayıt göstergesi.** `onSaved` üzerinden, saf statik biçimlendirici, enjekte edilen saat, düzenleme/açma/yeni belgede temizleniyor, dil değişiminde yeniden basılıyor (`MainController.java:84-91,102-105,109-123,154-156`).

### 6.2 Açık kalanlar

| Bulgu | Kanıt | Etki |
|---|---|---|
| **`savedStatus` temalanmıyor** | `StatsBar.java:63` vs CSS'te `.stats-save` yok | Koyu temada okunabilirlik doğrulanmamış |
| **Hoş geldin diyaloğu sabit ve büyük** | `WelcomeDialog.java:86` → 488×778, `setResizable(false)`, **ScrollPane yok** (Hakkında'da var: `AboutDialog.java:98-101`), kısayol metni `setWrapText` almıyor | 1366×768'de dikey kırpılma; kaydırma yolu yok |
| **`Alert` diyalogları temayı almıyor** | `DialogService`/`CrashHandler` yalnız başlık/içerik/sahip atıyor; repo genelinde `getDialogPane` eşleşmesi **yok** | Koyu temada beyaz pencere |
| **Minimum pencere boyutu yok** | `src/main`'de `setMinWidth` yok; `ToolbarFit.decide` alt sınır tanımıyor (`:193-195`) | Daraltınca araç çubuğu yeniden kırpılır — §6.1'in ölçülmemiş kısmı |
| **Erişilebilirlik yalnız altı listede** | `ToolbarView.java:392-397,529`; başka `accessibleText`/`AccessibleRole`/`accessibleHelp` yok | Ekran okuyucu için editör, durum çubuğu ve kayıt etiketi sessiz |
| **RTL yok** | `NodeOrientation|RIGHT_TO_LEFT|Bidi` araması sıfır | Sağdan-soldan diller kapsam dışı |
| **Yardımcı diyalogların layout'u test edilmiyor** | `DialogServiceTest.java:83-90` yalnız metnin boş olmadığını kontrol ediyor | Uzun çeviri kırpılması görünmez |

**Erişilebilirlik skoru: 3 / 5** (önceki 1,5). Odak halkası, Tab sırası, tooltip ve AA kontrastı ölçen test gerçek ve kalıcı kazanç; kalan açıklar yardımcı diyaloglar, genel `AccessibleRole` kapsamı ve RTL.

**UX skoru: 3 / 5** (önceki 2,5). Taşma stratejisi, tooltip, kalıcı görünüm ve kayıt göstergesi görünür sıçrama; `stats-save` kontrast deliği, temassız `Alert`'ler ve sabit boyutlu hoş geldin ekranı puanı tutuyor.

---

## 7. i18n Kalitesi

**İyi (ve ölçülü):** Altı bundle'da tam **90 anahtar**, kümeler birebir aynı; `MessagesTest.everyBundleTranslatesTheSameKeys` bunu zorluyor (`MessagesTest.java:31-39`). Sistem locale'ine sessiz düşme `getNoFallbackControl` ile engelli (`Messages.java:20-21`); dil çözümlemesi `Locale.ROOT` ile yapıldığı için Türkçe noktalı-i tuzağı yok (`Messages.java:45-46`); `Language` adları native sabitler ve `LanguageTest` bunu üç sistem locale'inde sabitliyor (`LanguageTest.java:37-56`). Canlı dil değişimi tüm görünür metni yeniliyor — araç çubuğu (`ToolbarView.java:250`), pencere başlığı ve kayıt göstergesi (`MainController.java:102-105`) dahil.

**Kapandı:** DE "Redo" → **Wiederholen**; TR hoş geldin metni tamamen Türkçeleşti; `recoveryNo` artık "sil" demiyor; tema sırası altı dilde gerçek döngüyle uyumlu; yeni `toolbarFontSize`/`toolbarLanguage`/`savedStatus`/`encodingMalformed*` anahtarları altı dilde mevcut. Hepsi `MessagesTest`'te kalıcı olarak sabitlendi (`:73-139`).

### Açık — teknik

1. **`MessageFormat` varsayılan JVM locale'i ile biçimliyor.** Yedi üretim çağrısının tamamı iki argümanlı `format(String, Object...)` kullanıyor: `Messages.java:238,315,328,376,396,408` ve `MainController.java:155`. Sonuç: arayüz Türkçe, sayı grubu/ondalık ayırıcısı işletim sistemininki olabilir. `statsFormat`'ın kelime/karakter/satır sayıları bu tuzağa en açık olanı.
2. **Apostrof kaçışı yok — ve bu kez gerçekten kırılıyor.** `encodingMalformedContent` deseni EN/FR/IT'de kaçışsız `'` içeriyor (`messages.properties:84`, `messages_fr.properties:84`, `messages_it.properties:84`). `MessageFormat` `'`'yi alıntı karakteri saydığı için bugün üretilen metin: EN'de `"This files byte-order mark…"`; FR'de `"Lordre doctets…"` **ve ilk `{0}` literal**; IT'de aynı. Bu, bozuk BOM'lu dosyayı açan kullanıcının gördüğü diyalog. Çözüm ya `''` ile kaçırmak ya da `MessageFormat`'ı bırakmak. `MessagesTest` bunu yakalamıyor çünkü bu anahtarı **hiç biçimlemiyor**.
3. **Kurtarma zaman damgası sistem locale'inde.** `ofLocalizedDateTime(MEDIUM).withZone(ZoneId.systemDefault())` (`DialogService.java:265-268`). Arayüz dili ile tarih biçimi ayrışabilir.
4. **İstatistik saat/tarih biçimi sabit.** `dd.MM.yyyy` / `HH:mm:ss` (`StatsBar.java:26-27`), kayıt göstergesi `HH:mm` (`MainController.java:25`) — dile göre değişmiyor. Tarih için savunulabilir, saat için tartışmalı (12/24 saat tercihi).
5. **İki anahtar ölü.** `toolbarFontUp`/`toolbarFontDown` altı bundle'da ve `Messages`'ta erişici olarak duruyor ama **hiçbir çağıranı yok** (`Messages.java:86-92`). Anahtar eşitliği testi bunu göremez.

### `MessagesTest` neyi zorluyor, neyi zorlamıyor

**Zorluyor:** altı bundle anahtar eşitliği; `tr-TR`/`de-DE` altında locale bağımsızlığı; tema ve renk adlarının yerelleşmesi; dil değişiminde metin yenilenmesi; tehlikeli çeviri hatalarının sabitlenmesi; TR metninde İngilizce kalmaması; tema sırası.

**Zorlamıyor:** `MessageFormat` yer tutucu sayısı/tipi; apostrof kaçışı; `overwriteContent`, `recoveryContent`, `encodingUpgradeContent`, `encodingMalformedContent`, `errorLogged`, `statsFormat` için **biçimlenmiş çıktı**; kullanılmayan anahtarlar; layout/uzunluk/satır kırılması.

**i18n skoru: 4 / 5** (önceki 4). Anahtar eşitliği ve locale bağımsızlığı gerçekten sağlam; puanı tutan şey placeholder/apostrof testi yokluğu ve artık **kanıtlanmış** bir kullanıcı-görünür hata.

---

## 8. Build, Paketleme, Dağıtım

### 8.1 İyi

- **Sürüm tek kaynak:** `pom.xml:9` (`1.0.0`). `package.sh` sürümü derlenmiş artefakt adından geri okuyup jpackage kurallarına karşı doğruluyor (`package.sh:61-71`).
- **Bayat artefakt koruması:** tam olarak bir `jar-with-dependencies` yoksa script duruyor; `clean` öneriliyor (`package.sh:44-59`).
- **İkon hattı tamam:** tek SVG → 9 PNG (16…1024) + `.ico` + `.icns`; `--check` modu bayat asset'te `exit 1` veriyor (`generate-icons.py:31-53,146-173`); `package.sh` eksik ikonda net hata verip duruyor (`package.sh:80-94`).
- **CI sağlam:** üç OS matrisi (`ubuntu/windows/macos`), Temurin 25, `mvn -B package`, ardından **gerçekten koşan** TR ve DE locale tekrarı (`.github/workflows/maven.yml:23-49`), `permissions: contents: write`.
- **Bağımlılık hijyeni:** JUnit 6.0.3 ve Mockito 5.23.0 test kapsamında (`pom.xml:40-51`); surefire 3.5.6 yapılandırmasız.

### 8.2 Eksik / riskli

| Sorun | Kanıt | Etki |
|---|---|---|
| **Portable kararı koda yansımadı** | `package.sh:148-166` `.deb`/`.msi`/`.dmg` üretiyor; `maven.yml:65-67` her OS'te `--installer` çağırıyor | Roadmap §2.1 ile doğrudan çelişki; F5.3 |
| **`jlink` yok** | `package.sh` içinde `jlink` eşleşmesi yok | Runtime `jpackage`'ın ürettiği tam imaj; 134 MB / 53 modül (§2.3/E3) |
| **İmzalama / notarization yok** | `codesign|notar` eşleşmesi yok | macOS Gatekeeper, Windows SmartScreen |
| **Tag tetiği / checksum / release hattı yok** | Yalnız `push`/`PR → main` (`maven.yml:10-14`); `SHA256SUMS` adımı yok | Artefakt 90 gün sonra silinir; indirilebilir "sürüm" yok |
| **`--license-file` yok** | `package.sh` içinde eşleşme yok | GPL metni artefakta girmiyor (F4.5) |
| **Action'lar major tag'e pinli** | `@v4` (`maven.yml:31,34,70`) | Tedarik zinciri sertleştirmesi eksik |
| **`module-info` yok, boş `main.fxml`** | `main.fxml:1-7`; `FxApplication.java:30-42` | Gereksiz `javafx-fxml` + dolaylılık; jlink/native-image kapalı |
| **`AppDirs` her OS'te aynı yol** | `AppDirs.java:14-16` | Portable'ın merkezî engeli (E1/E8) |
| **Rozet betiğinin hata garantisi abartılı** | `maven.yml:52-53` "asla kırmızıya çeviremez" diyor; gerçekte yalnız son push döngüsü fail-soft (`publish-test-badge.sh:128-132`), rapor yoksa `exit 1` (`:30-33`) ve `git config/add/commit` `set -euo pipefail` altında korumasız (`:104-113`) | Yanlış dokümantasyon; düşük olasılık, gereksiz risk |

### 8.3 Hukuki

- `LICENSE` tam **GPLv3** metni (`LICENSE:1-2`), README'de bağlantı var (`README.md:405`).
- **Hiçbir kaynak dosyada SPDX başlığı yok** (repo genelinde `SPDX` yalnız bu dokümanın eski sürümünde geçiyordu). `pom.xml`'de `<licenses>`, `<developers>`, `<scm>`, `<url>` **yok**. `NOTICE`/`THIRD-PARTY` dosyası yok.
- **`src/main/resources/audio/keystroke.wav` kaynağı ve lisansı belgelenmemiş.** Tek referans `TypewriterSound.java:31`; README, LICENSE, yorum veya commit mesajında kaynak/atıf yok (dosya ilk commit'ten beri aynı). GPLv3 dağıtımında bu ciddi bir uyum sorusu.
- **Dağıtım ve hukuk skoru: 2,5 / 5.** İkon hattı ve tek örnek kilidi gerçek kazanç; imza/checksum/lisans paketleme ve portable geçişi açık.

---

## 9. Test ve Kalite Güvencesi

**Ölçülen durum:** **218 `@Test` / 25 test sınıfı** (statik sayım ve mevcut `target/surefire-reports` toplamı birebir: `tests=218, failures=0, errors=0, skipped=0`). Parametrik/dinamik test, `@Disabled` yok. İki test POSIX korumalı (`AtomicFilesTest.java:92-96`, `AppSettingsTest.java:221-231`) ve Windows'ta atlanabilir.

**İyi kapsanan (davranış odaklı, headless):**

| Sınıf | Test | Alan |
|---|---:|---|
| `FileServiceTest` | 30 | Aç/kaydet/yeni belge, kirli akış, yedek, kodlama, kurtarma, dinleyici sırası |
| `RecoveryStoreTest` | 18 | Taslak yaşam döngüsü, tolerans, adoption, park etme |
| `TextCodecTest` | 16 | BOM'lar, sıkı UTF-8, legacy sözleşmesi, yükseltme |
| `AppSettingsTest` | 15 | Kalıcılık, varsayılanlar, karantina, kullanılamaz dizin |
| `FontSizeManagerTest` | 13 | Merdiven, off-ladder, kırpma, kalıcı sıfırlama |
| `SessionStatsTest` | 12 | Sayımlar, WPM, durağanlık azalması |
| `ToolbarFitTest` | 12 | Genişlik modeli, FULL/COMPACT, histerezis, DE/FR kabul |
| `KeyboardShortcutHandlerTest` | 11 | Kısayol çözümleme, shift, numpad |
| `ContrastRatioTest` | 11 | WCAG matematiği + **gerçek CSS** ayrıştırma |
| `MessagesTest` | 8 | Anahtar eşitliği, locale bağımsızlığı, sabitlenmiş çeviriler |
| diğer 15 sınıf | 82 | Loglama, atomik yazma, kilit, diyalog, ikon, başlık, sahne, stats boyutu, araç çubuğu seçenekleri/durumu, YOLO |

**Locale disiplini gerçek:** `tr_TR` ve `de_DE` tekrarı **CI'da bağlı**, yalnız dokümante değil (`.github/workflows/maven.yml:45-49`). `Locale.getDefault()` değiştiren testler `finally` ile geri alıyor (`MessagesTest.java:53-70`, `LanguageTest.java:38-55`). Bundle kullanan her test dili açıkça set ediyor veya `@TempDir` ayar dosyası kullanıyor.

**Yok olanlar (ve olmaması bilinçli):**

| Yok | Neden önemli |
|---|---|
| Süreç öldürme / ikinci JVM testi | `kill -9` sonrası kurtarma iddiası **hiç** uçtan uca kanıtlanmadı; `InstanceLockTest` yalnız aynı JVM içinde ikinci kez alıyor |
| Gerçek JavaFX toolkit / TestFX | Doğru bir sınır (headless kural), ama tooltip/`viewOrder`/`refresh()` döngüsü gibi davranışlar **hiç** test edilmiyor |
| Hata enjeksiyonu | Disk dolu, `fsync` hatası, yedek hatası yok. `AtomicFilesTest.failedWritesCleanUpTheirTempFile` disk dolu değil — hedefi dizin yaparak move'u düşürüyor |
| JaCoCo / kapsam ölçümü | Kapsam hiç ölçülmüyor; regresyon "yeşil mi" ile sınırlı |
| `AtomicMoveNotSupportedException` dalı | Portable'ın ana yolu; testi yok |
| `restoreRecovered` + gerçek legacy dosya | Test ASCII yazdığı için legacy dalı hiç çalışmıyor (§4.2/B) |
| `NaN`/`Infinity` ayar değerleri | Testi yok |
| `MessageFormat` biçimlenmiş çıktısı | Apostrof hatası bu yüzden görünmedi (§7) |

**AGENTS.md kurallarından testle zorlanmayanlar:** "düz `Files.write` kullanma" (statik test yok), "hatalar loglanır ve gösterilir" (yalnız biçimlendirici test ediliyor), "testler headless" (var sayım, iddia değil), "`AppGraph` alan sırası bağımlılık sırası" (test yok), "README iki dilde aynı bölüm sırası" (test yok).

**Test skoru: 4 / 5** (önceki 3,5). 218 test ve locale CI gerçekten güçlü; puanı tutan şey süreç-düzeyi testlerin, hata enjeksiyonunun ve kapsam ölçümünün yokluğu.

---

## 10. Olgunluk Skorları

| Boyut | Puan | Δ | Gerekçe |
|---|---|---|---|
| Mühendislik temelleri | **4 / 5** | — | Atomik kayıt + dizin fsync, elle DI, tek `refresh()` noktası, saf politika sınıfları, locale CI. FX thread'inde senkron G/Ç ve `module-info` yokluğu puanı tutuyor |
| Veri güvenliği | **4 / 5** | — | Kurtarma ve kodlama yolları sağlam; kalan: dış değişiklik algılama yok, `restoreRecovered` kodlama hatası, atomik olmayan fallback doğrulanmıyor, yedek atomik değil, temp süpürme yok |
| Özellik bütünlüğü | **2,5 / 5** | — | Yeni Belge var; ara/değiştir, sekmeler, son dosyalar, dışa aktarma yok |
| UX | **3 / 5** | +0,5 | Taşma stratejisi, tooltip, kalıcı görünüm, kayıt göstergesi; `savedStatus` kontrastı, temassız `Alert`, sabit boyutlu hoş geldin, minimum genişlik yok |
| Erişilebilirlik | **3 / 5** | +1,5 | Odak halkaları + AA kontrast **testli**, Tab sırası, tooltip; rol/metin kapsamı yalnız altı listede, RTL yok |
| Dağıtım ve güven | **2,5 / 5** | — | İkon ve kilit tamam; imza, tag, checksum, `jlink`, portable geçişi yok |
| Bakım yapılabilirlik | **4 / 5** | — | Temiz composition root, AGENTS.md, saf mantık testleri; tek pencere varsayımı borç |
| Hukuki | **2,5 / 5** | — | GPLv3 + LICENSE var; SPDX/pom metadata/WAV kaynağı/artefaktta lisans yok |

**Ağırlıklı genel: ~3,2 / 5 — güvenilir bir beta, henüz 1.0 değil.**

---

## 11. Yol Haritası ile İlişki

Faz planı, sıralama ve kabul kriterleri **`product-roadmap.md`** içinde. Burada yalnız bu analizin hangi faza düştüğü özetlenir (ikinci bir iş listesi yaratmamak için).

| Faz | Bu analizdeki karşılığı |
|---|---|
| **P0** — Güven + ilk izlenim | **Tamamen kapandı.** `RW-01`…`RW-20` + `RW-54` tamamlandı; `product-roadmap.md:73-84` çıkış kriterlerinin altısı da karşılanmış durumda. Roadmap "P1 henüz başlamamıştır" diyor (`product-roadmap.md:85`) |
| **P1** — Portable (kurulumsuz) | §4.2/C (atomik olmayan fallback), §4.2/E (temp süpürme), §3.2/4 (`AppLogging` yolu), §3.2/5 (`module-info`, `javafx-media`, `main.fxml`), §8.2 (tek kanal, `jlink`, checksum, `--license-file`) |
| **P2** — Editör olma | §5.2: ara/değiştir; §4.2/A (dış değişiklik algılama); §4.2/B (kurtarılan belgenin kodlaması); §3.3 (asenkron G/Ç, istatistik artımlı sayım); §9 (süreç öldürme testi) |
| **P3** — Ürünleşme | §5.2: son dosyalar, sürükle-bırak, dışa aktarma, tercihler; §7/2 apostrof düzeltmesi + placeholder testi; §4.2/D (`stats-save` teması); §9 hata enjeksiyonu |
| **P4** — Platform | Sekmeler, depolama soyutlaması, erişilebilirlik denetimi (rol/metin kapsamı, RTL), TestFX/JaCoCo |

Bu analiz yol haritasına **yeni bir iş paketi eklemiyor**: §4.2/D, §7/2 ve §4.2/B'deki üç bulgu mevcut fazlara (P3, P3, P2) düşüyor; §4.2/J (symlink) ve §4.2/H (`Infinity`) düşük öncelikli olarak P3 kuyruğunda.

---

## 12. Go / No-Go

**Herkese açık `1.0.0`: HAYIR. Genel beta (`0.9`): EVET — P0 kapısı kapandığı için artık gerçekten hazır.**

`1.0` için "her şeyi bitir" gerekmiyor; dar bir kapı yeterli:

1. **Kurtarma yeni delik açmıyor** — ertelenen taslak park edildiği için otomatik kayıt hiç susmuyor (RW-54 ✓), ama gerçek `kill -9` senaryosu hâlâ otomatik testle kanıtlanmadı (RW-24).
2. **Kurtarılan belge kodlamayı bozmuyor** — `restoreRecovered`'ın BOM'suz legacy dalı bugün UTF-8'e çeviriyor (§4.2/B); testi de bu dalı çalıştırmıyor.
3. **Ara/Değiştir + dış değişiklik uyarısı** — bir yazarın günlük akışı (§5.2, §4.2/A).
4. **Arayüzün okunabilirlik tabanı gerçekten tamam** — `savedStatus` etiketine tema kuralı eklensin ve `ContrastRatioTest` onu da denetlesin (§4.2/D); bozuk kodlama diyaloğunun apostrof hatası düzeltilsin (§7/2).
5. **Tek kanal portable gerçekten tek** — `package.sh` ve CI'dan installer yolu çıkarılsın; artefakt zip/tar.gz + `SHA256SUMS` + tag; `LICENSE` pakette; README gerçeği anlatsın.
6. **Araç çubuğunun bir alt sınırı olsun** — ya `setMinWidth` ya da COMPACT altı bir davranış; "her genişlikte tek satır" ancak o zaman iddia edilebilir.

Bunlar yaklaşık **4-6 kişi-hafta**. Geri kalan her şey sonrasına bırakılabilir.

---

## 13. En Yüksek Getirili İki Bahis

### 1) "Taslağını asla kaybetmeyen editör" — güveni ürün yapın

Kod bu iddiaya çok yakın: üç seçenekli kurtarma, tek atomik taslak dosyası, toleranslı metadata, oturum başına slot, park etme, tek örnek kilidi, BOM'da sıkı çözme. Kalan iş **altyapı değil, kapatma**: (a) kurtarılan belgenin kodlamasını koru, (b) dış değişiklik uyarısı ekle, (c) gerçek `kill -9` testini yaz, (d) bunu pazarlanabilir bir cümleye çevir. Bu nişte **hiçbir rakip dayanıklılık satmıyor**, hepsi sadelik satıyor. Mühendislik yatırımınız zaten bu eksende; burada **birinci** olabilirsiniz — ama önce (a) ve (b) kapanmalı, çünkü ikisi de "sessizce veri değiştirme" sınıfından ve tam da bu iddiayı çürütür.

### 2) Türkçe-öncelikli yerelleştirme — kutulanacak bir madde değil, ürün özelliği

Ana dil kalitesinde TR arayüz, iki dilli dokümantasyon, locale tuzaklarında kanıtlanmış doğruluk (`LanguageTest` + `MessagesTest` + CI'da TR/DE tekrarı). FocusWriter'ın çevirileri "idare eder" seviyesinde; Türkçe yazma araçları nişini açık ara kazanmak ulaşılabilir. **Ama** §7/2'deki apostrof hatası tam da bu iddianın gösteri vitrininde (kodlama uyarı diyaloğu) duruyor; düzeltmesi bir satırlık iş, etkisi orantısız.

**Daktilo sesi iyi bir kanca ama hendek değil** — bir hafta sonu eklenebilecek bir şey. Hendek: **güven + yerelleştirme**.

**Yapılmaması gereken:** v1.2'den önce sekme/çoklu belgeye girişmek. Aylar yer ve "kimsenin kalıp kalmayacağını" belirleyen sıkıcı düzeltmeleri geciktirir.

---

## 14. Bulgu Dizini (25 Ekim 2026'da doğrulanmış kanıt)

### Kapanan

| Bulgu | Kanıt |
|---|---|
| Kurtarma diyaloğu kapatınca taslak silinmiyor | `DialogService.java:195-213`, `RecoveryChoice` |
| Bozuk metadata taslağı düşürmüyor | `RecoveryStore.java:247-364` |
| Taslak tek atomik dosyada | `RecoveryStore.java:105-107` |
| Oturum başına ayrı taslak + adoption | `RecoveryStore.java:116-143,196-239` |
| Tek örnek kilidi (davranış olarak fail-closed) | `InstanceLock.java:47-69`, `JTypeWriterApp.java:20-24` |
| Bozuk ayar karantinası | `AppSettings.java:231-265` |
| Log'da tam yığın izi | `AppLogging.java:52-110` |
| "Sonra sor" otomatik kaydı susturmuyor | `RecoveryStore.java:157-169`, `AutoSaveService.java:139-141` |
| BOM'lu yolda sıkı çözme + kullanıcıya sorma | `TextCodec.java:169-180`, `FileService.java:277-288` |
| Araç çubuğu taşma stratejisi + ölçüm | `ToolbarFit.java:78,85,196-201`, `ToolbarFitTest.java:133-171` |
| Tooltip + Tab sırası + başlıklı listeler | `ToolbarView.java:221-245,392-400` |
| Odak halkaları + AA kontrast (testli) | `base.css:9-12,113-164`, `ContrastRatioTest.java:72-144` |
| Kalıcı görünüm durumu (toolbar/fullscreen/stats) | `MainStage.java:62-66`, `MainController.java:65-67,164-165`, `AppSettings.java:62-76` |
| Kayıt göstergesi (`onSaved` ekseni) | `MainController.java:84-91,154-156`, `StatsBar.java:59-67,109-114` |
| `AppIcons` / diyalog başlıkları / `Ctrl+N` | `AppIcons`, `WelcomeDialog.java:96`, `AboutDialog.java:125`, `FileService.newDocument` |
| README mimari ağacı ve bileşen tablosu gerçekle hizalı | commit `8421163` + bu turdaki tablo düzeltmeleri |

### Açık

| Bulgu | Kanıt | Önem |
|---|---|---|
| Dış değişiklik algılama yok | `FileService.java:162-179,232-270` | Yüksek |
| `restoreRecovered` legacy kodlamayı UTF-8'e çeviriyor | `FileService.java:186-210` | Orta-yüksek |
| Atomik olmayan move fallback'i doğrulanmıyor | `AtomicFiles.java:67-68` | Orta |
| `.stats-save` için CSS kuralı yok | `StatsBar.java:63` vs `css/*.css` | Orta |
| `encodingMalformedContent` apostrof hatası (EN/FR/IT) | `messages*.properties:84`, `Messages.java:396` | Orta |
| Açılışta `jtw-*.tmp` süpürmesi yok | `AtomicFiles.java:53,72-76` | Düşük-orta |
| Yedek atomik değil + yedek hatası kaydı iptal ediyor | `BackupKeeper.java:23-30`, `FileService.java:253-257` | Düşük-orta |
| Eski `draft-*`/`pending-*` dosyaları temizlenmiyor | `RecoveryStore.java:116-143,181-188` | Düşük |
| `NaN`/`Infinity` ayar değerleri kabul ediliyor | `AppSettings.java:323-333` | Düşük |
| Araç çubuğu için alt sınır / `setMinWidth` yok | `ToolbarFit.java:193-195` | Düşük-orta |
| `Alert` diyalogları temayı almıyor | `DialogService.java:127-131`, `CrashHandler.java:64-70` | Orta |
| Hoş geldin diyaloğu 488×778, kaydırma yok | `WelcomeDialog.java:86,93,98` | Orta |
| Erişilebilirlik yalnız altı listede | `ToolbarView.java:392-397,529` | Orta |
| RTL desteği ve testi yok | `src/main` (eşleşme yok) | Düşük |
| `MessageFormat` varsayılan JVM locale'i | `Messages.java:238,315,328,376,396,408`, `MainController.java:155` | Orta |
| Kurtarma zaman damgası sistem locale'inde | `DialogService.java:265-268` | Düşük |
| `AppLogging.init()` enjekte edilemiyor | `AppLogging.java:121-137,152-155` | Orta (P1 blocker) |
| `AppDirs` her OS'te `~/.jtypewriter` | `AppDirs.java:14-16` | Yüksek (P1) |
| `module-info` yok, boş `main.fxml` + `javafx-fxml`/`javafx-media` | `main.fxml:1-7`, `pom.xml:24-38` | Orta (P1) |
| `package.sh`/CI hâlâ installer üretiyor | `package.sh:148-166`, `maven.yml:65-67` | Yüksek (karar çelişkisi) |
| `jlink` yok, runtime 134 MB | `package.sh` (`jlink` yok) | Orta (P1) |
| İmza/notarization/checksum/tag yok | `package.sh`, `maven.yml:10-14` | Orta-yüksek |
| `--license-file` yok, `pom.xml` metadata yok, SPDX yok | `package.sh`, `pom.xml:1-13,54-121` | Orta |
| `keystroke.wav` kaynağı/lisansı belgelenmemiş | `TypewriterSound.java:31` | Orta (hukuki) |
| Rozet betiğinin "asla kırmızıya çevirmez" iddiası fazla | `publish-test-badge.sh:30-33,104-113` vs `maven.yml:52-53` | Düşük |
| Süreç öldürme / hata enjeksiyonu / kapsam ölçümü yok | `src/test` (eşleşme yok), `pom.xml` (JaCoCo yok) | Orta |
| Symlink üzerinden kaydetme | `AtomicFiles.java:66` | Düşük |
| İki ölü bundle anahtarı (`toolbarFontUp/Down`) | `Messages.java:86-92` | Düşük |
