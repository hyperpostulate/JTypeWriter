# JTypeWriter — Kapsamlı Analiz ve Ürünleştirme Değerlendirmesi

> Tarih: 24 Ekim 2026 · Sürüm: `1.0.0` (`pom.xml`) · Kapsam: tüm `src/main`, `src/test`, `pom.xml`, `scripts/`, `.github/workflows/maven.yml`, `README.md`, `README-TR.md`, `docs/`
> Yöntem: üç bağımsız alan denetimi (veri güvenliği · arayüz/erişilebilirlik/i18n · yapı/paketleme/doküman) + kritik bulguların `file:line` düzeyinde elden doğrulanması; `mvn -B test` (çıkış kodu 0, **176 test / 22 test sınıfı**).
> **Bu sürüm bir yeniden yazımdır.** 23 Ekim tarihli önceki analiz, o günden bu yana kapanan işleri hâlâ açık gösteriyordu; aşağıdaki her bulgu bugünkü koda karşı yeniden doğrulandı. Faz planı için tek doğruluk kaynağı `product-roadmap.md`'dir; bu doküman onun **gerekçesini ve kanıtını** tutar.

---

## 1. Yönetici Özeti

JTypeWriter, mühendislik disiplini ürün olgunluğunun **önünde giden** bir JavaFX metin editörü. 40 üretim sınıfı / ~5.000 satır kod, 22 test sınıfı / ~2.700 satır test, 176 yeşil test — üstelik üç locale'de (varsayılan, `tr_TR`, `de_DE`). Atomik yazma + `fsync` + 3 nesil yedek rotasyonu, oturum başına atomik kurtarma dosyası, kodlama koruma akışı ve elle kurulan temiz composition root gerçekten iyi tasarlanmış.

Son bir haftada ürünün **en kritik vaadi onarıldı**: 23 Ekim'deki analizin işaret ettiği üç veri kaybı yolu (kapatınca taslağın silinmesi, bozuk metadata'nın taslağı düşürmesi, iki ayrı atomik yazma) kapandı; tek örnek kilidi, uygulama ikonu, diyalog başlıkları, log yığın izleri, ayar karantinası ve `Ctrl+N` geldi. Yani "taslağımı asla kaybetmez" iddiası artık **kodda büyük ölçüde doğru** — ki bu, ürünün tek gerçek farklılaştırıcısı olduğu için en değerli ilerleme.

Buna karşılık ürün hâlâ **"editör" olarak eksik**: ara/değiştir, sekmeler, son dosyalar, sürükle-bırak, yazdır/dışa aktarma, tercihler penceresi yok. Arayüz tarafında döngüsel düğmeler altı açılır listeye dönüştü ve tek bir `refresh()` noktasında toplandı (gerçek bir mimari kazanç), ama **tooltip yok, odak halkaları bastırılmış, araç çubuğu Tab ile gezilemiyor, taşma stratejisi yok**. Dağıtım tarafında imza, tag, checksum ve release hattı yok; ayrıca 24 Ekim'de alınan **"yalnız portable"** kararı henüz koda yansımadı — `package.sh` hâlâ `.deb`/`.msi`/`.dmg` üretiyor.

Dahası, kurtarma düzeltmesi **yeni bir açık** doğurdu: ertelenen ("sonra sor") taslak, yeni belgenin otomatik kayıtını tamamen susturuyor (`AutoSaveService.java:148-151`). Tek yuvalı taslak deposu ile birleşince, kullanıcı "sonra sor" dediği sürece **yeni yazdığı hiçbir şey için kurtarma kopyası üretilmiyor**. Bu, P0'ın kapatması gereken sınıftan bir bulgu.

**Karar: herkese açık `1.0.0` olarak yayınlanmamalı.** `0.9` genel betası, P0'ın kalan görünürlük işleri (RW-15..20) kapandığında yayınlanabilir — yani roadmap'in hafta 4 hedefi gerçekçi. Olgunluk skoru: **3,0 / 5** (önceki: 2,5). `1.0` kapısı artık "her şeyi bitir" değil, **beş maddelik dar bir liste** (§12).

En yüksek getirili iki bahis değişmedi: **(1) "taslağını asla kaybetmeyen editör" güven hikâyesini kapatmak**, **(2) Türkçe-öncelikli yerelleştirmeyi ürün özelliği olarak satmak.**

---

## 2. Ürün Kimliği ve Pazar Konumu

### 2.1 Ne satıyor?

1. **Odaklanma / sade arayüz** — tek `TextArea`, 3 tema, 6 metin rengi, istatistik çubuğu, tam ekran.
2. **Daktilo hissi** — her tuş vuruşunda `keystroke.wav`; "YOLO modu" (silme/geri alma kilitli, yalnız ileri yazma).
3. **Veri güvenliği** — atomik kayıt, 3 nesil yedek, oturum başına kurtarma taslağı, kodlama koruma, tek örnek kilidi.

Bunlardan **yalnız üçüncüsü savunulabilir bir farklılaştırıcı**; ilk ikisi bir hafta sonu projesinde taklit edilebilir. Son haftadaki işler tam olarak bu eksende yapıldı ve doğru bir yatırımdı.

### 2.2 Kimin için?

Kod tabanının en güçlü sinyali **Türkçe-öncelikli** olması: `Messages` `getNoFallbackControl` ile sistem locale'ine sessiz düşmeyi engelliyor, `Language` adları native sabit (Türkçe noktalı-i tuzağına karşı), CI `tr_TR` ve `de_DE`'de yeniden koşuyor, altı bundle 82 anahtarın tamamında eşleşiyor ve `MessagesTest` bunu zorluyor. Dokümantasyon da iki dilli: `README.md` (EN, repo girişi) + `README-TR.md` (tam çeviri, aynı bölüm sırası).

Hedef kitle: **yerel-öncelikli, hesapsız, çevrimdışı yazma yüzeyi isteyen yazarlar/öğrenciler** — özellikle Türkçe konuşanlar.

### 2.3 Rekabet

| Rakip | Gücü | JTypeWriter'ın şansı |
|---|---|---|
| **FocusWriter** | Olgun, özellik dolu, 20+ dil, oturumlar | Özellikte **kazanamaz**. Duygu (daktilo sesi, temalar), Türkçe kalitesi ve veri dayanıklılığında kazanabilir |
| **iA Writer** | Tasarım ikonu, Markdown, Apple ekosistemi | Ulaşılamaz; bu rekabete girilmemeli |
| **Typora / Obsidian** | Markdown + bilgi yönetimi | Yanlış savaş; plugin/Markdown altyapısı yok |
| **ghostwriter** | KDE/Markdown, ücretsiz | "Markdown zorunlu değil" ve Windows/macOS'ta kazanabilir |
| **Hemingway** | Okunabilirlik geri bildirimi, ücretli web | Çakışma yok |

**Sonuç:** Konumlandırma "bir başka distraction-free editör" değil, **"Türkçe + Avrupa dilleri için güvenli, yerel, daktilo hisli editör"**. FocusWriter'ın özellik yarışına girmek kaybedilen bir yatırımdır.

**Not — portable kararı kimliği değiştiriyor:** 24 Ekim'de alınan "tek kanal portable, installer yok" kararı (§8.2) ürün kimliğine uyuyor ("sistemimde iz bırakma" garantisi, USB'den çalıştırma), ama **henüz uygulanmadı**. Bugünkü artefaktlar hâlâ kurulum paketleri üretiyor.

---

## 3. Mimari Değerlendirme

### 3.1 Güçlü yanlar

- **Composition root disiplini.** `AppGraph` elle DI yapıyor; alan sırası bağımlılık sırası, dinleyiciler constructor'da bağlanıyor. Spring'den kurtulma kararı doğruydu.
- **Durum türetme.** Kirli bayrak ayrı tutulmuyor; `FileService.hasUnsavedChanges()` içerik karşılaştırmasıyla türetiliyor. Tek doğruluk kaynağı.
- **Tek senkron noktası (yeni).** Araç çubuğundaki 11 kontrolün tamamı `ToolbarView.refresh()` üzerinden yenileniyor (`ToolbarView.java:223-242`); açılır liste seçimleri, kısayollar (`Ctrl+Shift+T/Y/C`, `Ctrl+±/0`) ve dil değişimi aynı yola giriyor. RW-12 sınıfı "iki kaynak, bir etiket" hatası yapısal olarak kapandı — kısayolun etiketi atlaması artık mümkün değil.
- **Test edilebilirlik için saf mantık ayrımı.** `MainController.titleFor`, `MainStage.intersectsAny`, `KeyboardShortcutHandler.resolve`, `YoloPolicy`, `DialogService.choiceFor`, `RecoveryStore` çözümleyicileri — hepsi headless test edilebilir. AGENTS.md bunu kural hâline getirmiş.
- **YOLO tek huniden geçiyor.** `EditorArea.replaceText` tüm yıkıcı düzenlemelerin tek kesişimi; `clear()` ve undo/redo ayrıca kapatılmış.

### 3.2 Yapısal borç

1. **Tek pencere / tek belge varsayımı her yere işlemiş.** `FileService` tek `currentFilePath` + tek `savedContent` tutuyor; `AutoSaveService` tek `Timeline`'a ve oturum başına tek aktif slota bağlı; UI tek `EditorArea`. Sekme/çoklu belge **yama değil yeniden tasarım**. (Ertelenen taslağın park edilmesi bu varsayımın tek somut sonucunu kapattı — §4.1/H — ama model aynı kaldı.)
2. **Depolama soyutlaması yok.** `FileService` doğrudan `Files`/`AtomicFiles` kullanıyor, `BackupKeeper` içeride `new`'leniyor, `RecoveryStore` `AppDirs.recoveryDir()` ile sabit. Bulut/şifreli/alternatif depolama eklemek bu sınıfları değiştirmek demek.
3. **Yaşam döngüsü sözleşmesi yok.** `StatsBar` `Timeline`'ı, `AppGraph.stage`, `AudioClip`, `LocaleManager` dinleyicileri bırakılmıyor. Tek pencere için zararsız; çoklu pencere/test izolasyonu için tuzak.
4. **`AppDirs` hâlâ her işletim sisteminde `~/.jtypewriter`** (`AppDirs.java:14-16`). Portable hedefin merkezî engeli; `AppSettings`/`RecoveryStore` artık yol enjekte edilebilir olsa da **`AppLogging` enjekte edilemez** — yalnız `init()` var ve `AppDirs.logsDir()`'a sabit (`AppLogging.java:121-144`). Bu, portable F1.2'nin ilk somut işi.
5. **`module-info` yok, fat JAR + classpath.** `javafx-media` tek bir WAV tık sesi için taşınıyor (`pom.xml:34-38`, `TypewriterSound.java:3`); `javafx-fxml` ise yalnız **boş** `main.fxml`'i yüklemek için duruyor (`main.fxml:3-7` boş `BorderPane`, `FxApplication.java:30-37`).
6. **JavaFX kontrolleri `Application.init()` içinde kuruluyor** (`FxApplication.java:20-24` → `AppGraph` alan başlatıcıları). `init()` FX uygulama thread'inde değil; bugün çalışıyor ama garanti edilen bir sözleşme değil.

### 3.3 Threading ve ölçeklenme

Tüm belge G/Ç'si **FX thread'inde senkron**; hiçbir yerde `Task`/executor/`CompletableFuture` yok. Engelleme noktaları: `Files.readAllBytes` (`FileService.java:164,189`), `AtomicFiles.writeMoving` + `fsync` (`FileService.java:250`, `AutoSaveService.java:154`). Autosave tetikleyicileri de (30 sn'lik `KeyFrame`, odak kaybı, `WINDOW_HIDDEN`) FX thread'inde.

Ayrıca her tuşta kirli kontrolü tam string karşılaştırması, istatistikler ise her saniye **tam belge taraması** (`SessionStats` kelime sayımı `split("\\s+")` ile). Bu, 50 MB'lık bir dosyada ölçülebilir maliyet demek ve P1'deki "50 MB'de UI donmuyor" kabul kriteriyle doğrudan çelişir.

---

## 4. Veri Güvenliği — Çekirdek ve Kalan Açıklar

Kayıt yolu gerçekten iyi: temp dosya → `FileChannel.force(true)` → izin/ACL koruma → `ATOMIC_MOVE` → dizin `fsync`; hata hâlinde temp temizliği (`AtomicFiles.java:49-75`). Yazma yolu kodlamada strict `REPORT` kullanıyor: temsil edilemeyen karakter `?` ile sessizce ikame edilmiyor, kullanıcıya UTF-8'e yükseltme soruluyor (`TextCodec.java:69-90`, `FileService.java:225-241`). Bunlar doğru kararlar.

### 4.1 Kapanan yollar (23 → 24 Ekim)

| # | Eski bulgu | Bugünkü durum |
|---|---|---|
| A | Kurtarma diyaloğu kapatılınca taslak siliniyordu | **Kapandı.** Üç seçenek: geri yükle / sil / sonra sor (`DialogService.java:198-211`). Kapatma (X/Escape) `LATER` sayılıyor ve taslak korunuyor (`DialogService.java:218-224`). Ertelenen taslak ayrı bir dosyaya park edildiği için temiz çıkıştan da etkilenmiyor (`RecoveryStore.park`, `AutoSaveService.keepDraftForNextStart`). `DialogServiceTest` + `AutoSaveServiceTest` regresyon altında |
| B | Bozuk metadata tüm taslağı düşürüyordu | **Kapandı.** Magic/bölücü eksikse tüm içerik metin olarak sunuluyor; bozuk `saved.at` → dosya mtime; bozuk/eksik yol → `null`; geçersiz UTF-8 → değiştirme karakterleriyle (`RecoveryStore.java:198-274, 300-315`) |
| C | Taslak + metadata iki ayrı atomik yazmaydı | **Kapandı.** Tek `draft-<uuid>.jtw` dosyası, tek `AtomicFiles.writeMoving` (`RecoveryStore.java:76-96`) |
| D | Sabit `draft.txt`, iki örnek birbirini eziyordu | **Kapandı.** Oturum başına UUID'li dosya; `loadDraft` en yenisini seçer, başka oturumun taslağını kendi slotuna devralır (adoption), `clear()` yalnız kendi dosyasını siler (`RecoveryStore.java:103-158`) |
| E | Tek örnek kilidi yoktu | **Kapandı (kısmen — §4.2/B).** `InstanceLock` advisory `FileLock` ile `~/.jtypewriter/instance.lock` üzerinde; başlangıçta graph kurulmadan alınıyor (`JTypeWriterApp.java:20-34`), ikinci örnek yerelleştirilmiş uyarıyla çıkıyor |
| F | Bozuk ayarlar sessizce eziliyordu | **Kapandı.** `settings.properties.corrupt` (+ çakışmada `.corrupt.<millis>`) karantinası (`AppSettings.java:184-193`) |
| G | Log'da stack trace yoktu | **Kapandı.** Girintili tam yığın izi + `Caused by:` zinciri, formatter asla fırlatmıyor (`AppLogging.java:52-66, 88-110`) |
| H | "Sonra sor" yanıtı yeni belgenin otomatik kaydını tamamen susturuyordu | **Kapandı.** Ertelenen taslak `pending-<uuid>.jtw` olarak park ediliyor; aktif slot serbest kalıyor ve snapshot'lar devam ediyor. Park başarısız olursa (rename hatası) slot yeniden donduruluyor — yalnız o durumda erken dönüş var (`RecoveryStore.park`, `AutoSaveService.snapshot`). `RecoveryStoreTest` + `AutoSaveServiceTest` regresyon altında; roadmap'te RW-54 |

### 4.2 Kalan açıklar — önem sırasıyla

**A. Dış değişiklik algılama yok.** *(Açık, yüksek)*
`writeFile` diskteki dosyanın mtime/boyut/hash'ini okumuyor (`FileService.java:225-263`); `loadFile` da parmak izi kaydetmiyor (`:162-179`). Bulut senkronizasyonu veya başka bir editör aynı dosyayı değiştirdiyse **sessizce üzerine yazılıyor**. "Güvenli editör" iddiası için kritik; roadmap'te RW-23.

**B. Kilit "fail-open" ve adoption birleşince taslak kaybı üretebilir.** *(Açık, orta-yüksek)*
`InstanceLock.acquire` herhangi bir `IOException`/`SecurityException` durumunda **kilidi vermeden uygulamayı başlatıyor** (`InstanceLock.java:47-68`). Salt-okunur veya kilitleme desteklemeyen bir medyada iki örnek birlikte çalışır; ikisi de aynı `draft-*.jtw` havuzunu görür ve adoption **diğer canlı oturumun taslağını siliyor** (`RecoveryStore.java:148-158`). Portable hedefte (USB, ağ paylaşımı) bu senaryo tam olarak beklenen senaryo.

**C. Kurtarılan belge, okunamayan orijinali UTF-8 olarak yazabilir.** *(Açık, orta)*
`restoreRecovered` orijinali çözemediğinde `currentFilePath` yine atanıyor (`onDisk == null` dalı `if` dışında, `FileService.java:186-203`), `currentCharset` ise önceki/varsayılan UTF-8 olarak kalıyor. Windows-1254 bir dosyanın taslağı geri yüklendiğinde, sonraki kaydetme onu UTF-8'e çevirebilir — "kodlamayı sessizce değiştirme" kuralının ihlali.

**D. BOM'lu yolda lenient decode.** *(Açık, orta)*
UTF-8/UTF-16 BOM'lu baytlar `decodeLenient` ile çözülüyor (`TextCodec.java:48-58, 116-126`); bozuk baytlar `U+FFFD` oluyor ve kullanıcı uyarılmıyor. BOM'suz yolda strict decode + legacy istemi doğru; BOM yolunda aynı titizlik yok. Roadmap RW-20.

**E. `ATOMIC_MOVE` desteklenmeyen dosya sistemlerinde atomik olmayan rename'e düşülüyor.** *(Açık, orta)*
`AtomicMoveNotSupportedException` → düz `Files.move(REPLACE_EXISTING)` (`AtomicFiles.java:65-69`). FAT32/exFAT USB'de "yarım dosya yok" garantisi zayıflıyor. Portable hedefte bu, kenar durum değil **ana yol**. Roadmap F3.1-F3.2.

**F. Açılışta `jtw-*.tmp` temizliği yok.** *(Açık, düşük)*
Temizlik yalnız çağrının `finally` bloğunda (`AtomicFiles.java:72-75`); çökmede tam belge metni `jtw-<ad>-*.tmp` olarak kalıyor. `TypewriterSound`'un `deleteOnExit` ile `/tmp`'e çıkardığı WAV için de aynı sınıf sorun.

**G. Yalnız en yeni taslak sunuluyor; eski `draft-*` dosyaları ne sunuluyor ne temizleniyor** (`RecoveryStore.java:124-129`). Toleranslı yükleme sayesinde veri kaybı yok ama disk sessizce büyüyor ve "kurtarılabilir taslaklar" görünümü (RW-36) olmadan kullanıcı erişemiyor.

**H. Yedek dosyası atomik değil.** `BackupKeeper` düz `Files.copy` kullanıyor; çökmede yarım `.bak` kalabilir. Ana yazma yedek hatasında iptal edildiği için belge güvende, ama **yedek kendisi** dayanıklı değil.

**I. Geometri ayıklaması eksik.** `Double.parseDouble` `NaN`/`Infinity` kabul ediyor (`AppSettings.java:276-286`); `MainStage`'in `> 0` kontrolü `Infinity`'yi geçiriyor.

**J. Symlink semantiği.** `Files.move(REPLACE_EXISTING)` sembolik bağlantıyı hedefin yerine koyar; bağlantı üzerinden kaydetme beklenmedik sonuç üretir.

---

## 5. Özellik Bütünlüğü

### 5.1 Var olanlar (ve gerçekten çalışanlar)

Dosya aç/kaydet/farklı kaydet, **Yeni Belge (`Ctrl+N`)** + belge yüklerken undo geçmişi sıfırlama, oturum geri yükleme, atomik kayıt + 3 nesil yedek, otomatik kayıt + kurtarma (3 seçenekli), kodlama tespiti/koruma/UTF-8'e yükseltme, 3 tema + 6 metin rengi + yazı boyutu merdiveni, 6 dilde anlık değişim, daktilo sesi, YOLO modu, canlı istatistik, tam ekran, pencere geometrisi hatırlama, hoş geldin/hakkında diyalogları, uygulama ikonu, çökme yakalayıcı, tek örnek kilidi.

### 5.2 Olmayanlar — bir metin editörünün asgari tabanı

| Eksik | Etki | Kanıt |
|---|---|---|
| **Ara / Değiştir** | Hiç yok; 10k satırlık bir belgede gezinme imkânsız | `src/main` (eşleşme yok) |
| **Sekmeler / çoklu belge** | Açmak mevcut tamponu eziyor | `FileService` tek `currentFilePath` |
| **Son dosyalar** | Yalnız `last.file` + `last.directory` | `AppSettings.java:51-62` |
| **Sürükle-bırak** | `setOnDragOver`/`setOnDragDropped` yok | `src/main` |
| **Yazdır / dışa aktar** | Yok (düz metin/MD/PDF/HTML) | — |
| **Yazım denetimi** | Yok | — |
| **Tercihler penceresi** | Altı ayar araç çubuğunda açılır liste; merkezî bir form yok | `ToolbarView` |
| **Kısayol/erişilebilirlik yüzeyi** | Tooltip yok, Tab ile erişim yok | §6.2 |
| **Dosya ilişkilendirmesi / CLI argümanı** | `getParameters()` okunmuyor; `.txt` çift tıklama çalışmaz (portable kararıyla **bilinçli kapsam dışı**) | `FxApplication.java:39-84` |

**Değerlendirme:** Özellik bütünlüğü **2,5 / 5** (önceki 2). Yeni Belge geldi ve undo sıfırlama doğru yapıldı; ama ara/değiştir ve son dosyalar hâlâ yok. Sadelik savunulabilir; **Ara/Değiştir ve son dosyalar sadelik değil, eksikliktir.**

### 5.3 README ile gerçek arasındaki farklar

- **Kapandı:** fat JAR'ın platforma özgü olduğu açıkça yazılıyor (`README.md:85-89`, `README-TR.md:85-92`); macOS `⌘` iddiası kodla uyumlu (`KeyboardShortcutHandler` `isShortcutDown() || isControlDown()`); tema sırası ve 6 renk sayısı enum'larla eşleşiyor; iki README aynı 15 bölümü aynı sırada taşıyor.
- **Açık (küçük):** Test sınıfı tablosunda 21 satır var, gerçekte 22 test sınıfı koşuyor — **`AppIconsTest` tabloda yok** (`README.md:347-369`, `README-TR.md:348-370`).
- **Açık (portable kararıyla ilgili):** README hâlâ kurulum/paketleme bölümünde `package.sh --installer` çıktısını anlatıyor; tek kanal portable kararı uygulandığında (roadmap F5.3) bu bölüm yeniden yazılmalı.

---

## 6. UX ve Erişilebilirlik

### 6.1 Kapanan işler

- **Araç çubuğu yeniden tasarlandı.** Döngüsel düğme + ayrı durum etiketi çiftleri (iki doğruluk kaynağı) yerine altı açılır liste; literal `"|"` ayırıcılar gitti, görsel gruplama geldi (`ToolbarView.java:164-177`).
- **Etiket bayatlaması bitti.** Tema etiketi `Ctrl+Shift+T` sonrası, pencere başlığı dil değişiminde yenileniyor (`ToolbarView.java:250-255`, `MainController.java:71-74`).
- **Diyaloglar sahiplendi ve başlıklandı.** Hoş geldin/Hakkında `setTitle` + `initOwner`; `showError` artık `Stage owner` alıyor (`DialogService.java:226-233`), `CrashHandler` de sahipleniyor. Hakkında diyaloğu `ScrollPane` içinde.
- **Uygulama ikonu** pencerede, görev çubuğunda/dock'ta ve paketleyicide (`AppIcons`, `package.sh --icon`).
- **"Yazar" → "Geliştirici"** terminolojisi altı bundle'da README ile hizalandı.

### 6.2 Açık kalanlar

**Araç çubuğu taşması.** Hâlâ sarma/taşma stratejisi yok: `BorderPane.top` içinde düz `HBox`, ne `setWrap`, ne taşma menüsü, ne yatay kaydırma (`ToolbarView.java:40, 164-177`). Tek azaltım 11 px yazı ve sıkılaştırılmış hücre dolgusu (`base.css:74-149`). Kodda/testte **genişlik ölçümü yok** — DE için elle ölçülen 896 px yalnız roadmap notunda, FR hiç ölçülmedi. Minimum pencere genişliği de zorlanmıyor.

**Tooltip yok.** `src/main` içinde tek bir `setTooltip` yok; araç çubuğu yalnız metin etiketiyle anlaşılıyor. Yazı boyutu listesi ise `Theme:/YOLO:/Ses:/Renk:` gibi bir başlık taşımıyor (yalnız sayı).

**Tab ile erişim yok.** Tüm düğmeler ve tüm combolar açıkça `setFocusTraversable(false)` (`ToolbarView.java:340, 463`). Klavyeyle tema/ses/dil değiştirmek için yalnız kısayollar var; odaklanabilir kontrol yok.

**Odak halkaları bastırılmış.** `-fx-focus-color`/`-fx-faint-focus-color` üç yerde `transparent` (`base.css:11-12, 85-86, 95-96`). Klavye kullanıcısı odağın nerede olduğunu göremiyor.

**Durum çubuğu kontrastı WCAG AA'yı geçmiyor.** Ölçülen oranlar: light `#888888`/`#f5f5f5` ≈ 3,25:1; dark `#777777`/`#1e1e1e` ≈ 3,72:1; sepia `#8b7355`/`#e8dcc8` ≈ 3,31:1 (`light.css:10`, `dark.css:10`, `sepia.css:10`). Normal metin için eşik 4,5:1. Boyut tarafı iyileşti: varsayılan `MEDIUM` = 13 px (`StatsBar.java:86-88`), ama `SMALL` hâlâ 11 px.

**Görünüm durumu kalıcı değil.** Araç çubuğu görünürlüğü (`MainController.java:29`), tam ekran (`MainStage.java:45` zorla kapatılıyor), istatistik çubuğu boyutu (`StatsBar.java:26` her açılışta `MEDIUM`) — üçü de sıfırlanıyor. Pencere geometrisi ise doğru şekilde saklanıyor.

**Diyaloglar sabit boyutlu.** Hoş geldin 488×778, `setResizable(false)`, **ScrollPane yok** (`WelcomeDialog.java:86, 98`) — 900×600 bir dizüstünde kısayol bloğu kırpılır. Hakkında'da scroll var.

**Temalı uygulama, temassız diyaloglar.** `Alert` tabanlı diyaloglar Modena varsayılanıyla açılıyor; uygulama stylesheet'i uygulanmıyor — koyu temada beyaz bir pencere.

**Erişilebilirlik metni yok.** Hiç `accessibleText`/`AccessibleRole` kullanılmıyor; ekran okuyucu için anlamlı bir yüzey yok.

**Erişilebilirlik skoru: 1,5 / 5** (önceki 1). Dialog sahipliği ve 13 px varsayılan gerçek kazanç; çekirdek sorunlar (odak, Tab, kontrast) aynı.

---

## 7. i18n Kalitesi

**İyi (ve ölçülü):** Altı bundle'da tam **82 anahtar**, anahtar kümeleri birebir aynı (`MessagesTest` zorluyor). Sistem locale'ine sessiz düşme engelli; `Language` adları native sabitler — `getDisplayLanguage` tuzağına düşülmüyor ve bu `LanguageTest` ile sabitlenmiş. Canlı dil değişimi tüm görünür metni yeniliyor.

**Kapandı:** DE "Redo" → **Wiederholen** (kısayol listesiyle aynı); TR hoş geldin metni tamamen Türkçeleşti; tema sırası altı dilde gerçek döngüyle (Light→Dark→Sepia) uyumlu; `recoveryNo` artık "sil" demiyor (TR/FR/IT'de "vazgeç/ignorer/ignora"). Üçü de `MessagesTest`'te kalıcı olarak sabitlendi.

**Açık — teknik:**
1. **Placeholder doğrulaması yok.** Anahtar eşitliği test ediliyor; **argüman sayısı/tipi ve apostrof kaçışı test edilmiyor**. `{7}` gibi bir hata ancak çalışma anında patlar.
2. **`MessageFormat.format` varsayılan JVM locale'i ile çalışıyor** (`Messages.java:228, 305, 318, 366, 378`); aynı şekilde kurtarma zaman damgası `ofLocalizedDateTime` ile sistem locale'ini kullanıyor (`DialogService.java:243`). Sonuç: arayüz Türkçe, sayı/tarih biçimi işletim sistemininki olabilir.
3. **İstatistik saat/tarih biçimi sabit** (`dd.MM.yyyy`, `HH:mm:ss`, `StatsBar.java:24-25`) — dile göre değişmiyor.
4. **RTL desteği ve uzun çeviri layout testi yok**; araç çubuğu taşması (§6.2) bunun pratikteki kanıtı.

**Açık — metin (küçük, kullanıcı etkisi düşük):** DE hoş geldinde "Toolbar umschalten" anglisizmi; DE'de tagline "du", diyaloglar "Sie" (sen/siz tutarsızlığı); ES'de aynı tutarsızlık; TR'de "yazdığın" ↔ "istiyor musunuz"; FR'de "codification" ↔ "encodage" karışıklığı.

**i18n skoru: 4 / 5** (önceki 3,5). Tek ciddi açık, placeholder/locale testlerinin yokluğu.

---

## 8. Build, Paketleme, Dağıtım

### 8.1 İyi

- Sürüm tek kaynak: `pom.xml` (`1.0.0`); `package.sh` sürümü derlenmiş artefakt adından geri okuyup jpackage kurallarına karşı doğruluyor.
- Bayat jar paketlenmesini engelleyen "tam olarak bir fat jar" kontrolü ve staging temizliği var.
- **İkon hattı tamam:** tek SVG kaynak → 16..1024 px PNG + `.ico` + `.icns`; `generate-icons.py --check` bayat asset'i yakalıyor; `package.sh` yoksa net hata verip duruyor.
- CI üç OS matrisi + TR/DE locale test tekrarı + test sayısı rozetinin `gh-pages`'e yayını (`publish-test-badge.sh`, hata durumunda build'i düşürmüyor).

### 8.2 Eksik / riskli

| Sorun | Etki |
|---|---|
| **Portable kararı koda yansımadı** | Roadmap "tek kanal portable, installer yok" diyor; `package.sh:148-165` ve `maven.yml:65-67` hâlâ `.deb`/`.msi`/`.dmg` üretiyor/yüklüyor. Karar ile artefakt arasındaki fark kapanmalı (F4) |
| **İmzalama / notarization yok** | macOS Gatekeeper bloklar, Windows SmartScreen uyarır — ilk izlenimde ürün "güvenilmez" görünür |
| **Tag/release tetiği, checksum yok** | CI yalnız `push`/`PR → main`; "release" artefaktı sürüm yayını değil, 90 gün sonra silinir. `SHA256SUMS` üretilmiyor |
| **`--license-file` yok** | GPL metni artefakta girmiyor; `.deb` yalnız jpackage runtime legal dosyalarını taşıyor |
| **`--file-associations` yok** | Portable kararıyla bilinçli kapsam dışı; README'de "birlikte aç" olarak anlatılmalı |
| Actions major tag'e pinli (`@v4`), SHA'ya değil | Tedarik zinciri sertleştirmesi eksik (`permissions: contents: write` artık var) |
| ARM64 / universal macOS yok | Kullanıcı tabanının bir kısmı dışarıda |
| `module-info` yok, fat jar classpath'te | jlink/native-image yolu kapalı; `javafx-media` native'leri imaja giriyor |
| `--skip-build` yalnız jar sayısını kontrol ediyor | Elle paketlemede bayat sürüm riski |

### 8.3 Hukuki

- GPLv3 + `LICENSE` mevcut, README'de bölüm var. Ancak **hiçbir kaynak dosyada SPDX/lisans başlığı yok**, `pom.xml`'de `<licenses>/<developers>/<scm>` yok, artefakta lisans dosyası girmiyor.
- `src/main/resources/audio/keystroke.wav` **kaynağı ve lisansı hâlâ belgelenmemiş** (ilk commit'ten beri). GPLv3 dağıtımında bu ciddi bir uyum sorusu; ya kaynak/lisans yazılmalı ya asset değiştirilmeli.
- "JTypeWriter" adı/markası için ayrı değerlendirme yok (düşük risk).

**Dağıtım ve hukuk skoru: 2,5 / 5** (önceki 2). İkon ve kilit gerçek kazanç; imza/checksum/lisans paketleme ve portable geçişi açık.

---

## 9. Test ve Kalite Güvencesi

**Çalıştırma sonucu:** `mvn -B test` → **başarılı (exit 0)**, **176 test / 22 test sınıfı**, varsayılan + `tr_TR` + `de_DE` locale'lerinde yeşil. Uyarılar beklenen türde (Mockito self-attach, `sun.misc.Unsafe`).

**İyi kapsanan (davranış odaklı, headless):** `FileService` (26 test — kirli akış, atomik yazma, yedek rotasyonu, 1254 round-trip, UTF-8 yükseltme, yaşam döngüsü), `RecoveryStore` (18 — tolerans, adoption, oturum izolasyonu, park etme), `AutoSaveService` (5 — bayat taslak temizliği, temiz çıkış, park sonrası devam), `FontSizeManager` (13 — merdiven, off-ladder değer, sınırlama), `AppSettings` (13), `SessionStats` (12), `TextCodec` (11), `KeyboardShortcutHandler` (11), `Messages` (8), `AppLogging` (7), `ToolbarOptionLists` (7), `AtomicFiles` (6), `DialogService` (6), `AppIcons` (6), `MainStage` (5), `YoloPolicy` (5), `InstanceLock` (4).

**Kritik boşluklar:**
- **Uçtan uca çökme/kurtarma senaryosu yok** (taslak yaz → süreç öldür → açılış → geri yükle → kaydet). Kurtarma deposunun birim testleri iyi, ama gerçek `kill -9` yolunu kanıtlayan test yok. Roadmap RW-24.
- **Park etme yolu (`RecoveryStore.park`)** artık test ediliyor; yalnız "park başarısız olursa slot donar" dalı yalnız kodla, testle kanıtlanmıyor (rename arızası yapay olarak üretilemiyor).
- **Hata enjeksiyonu yok:** disk dolu, salt-okunur dizin, yedek hatası, `fsync` hatası.
- **Dış değişiklik / symlink / hard-link** senaryoları yok.
- **JavaFX toolkit/TestFX testi hiç yok:** `ToolbarView`, `EditorArea`, `StatsBar`, tema yöneticileri, diyaloglar, gerçek kısayol dağıtımı doğrulanmıyor. Araç çubuğu genişliği gibi iddiaların ölçülebildiği tek yer burası olurdu.
- **`YoloPolicy` politika olarak test edilmiş**; `TextArea`'nın paste/IME/native menü yüzeyinin gerçekten `replaceText`'e aktığı entegrasyon testi yok.
- Placeholder doğruluğu, CSS geçerliliği, paketleme betiği test edilmiyor; JaCoCo/kapsam ölçümü yok.

**Test skoru: 4 / 5.** Sayı ve disiplin iyi; eksik olan katman (E2E + hata enjeksiyonu) tam olarak en yüksek riskli alanda.

---

## 10. Olgunluk Skorları

| Boyut | Puan | Δ | Gerekçe |
|---|---|---|---|
| Mühendislik temelleri | **4 / 5** | — | Atomik kayıt, elle DI, tek `refresh()` noktası, headless test disiplini, locale CI. FX thread'inde senkron G/Ç ve eksik `module-info` puanı tutuyor |
| Veri güvenliği | **4 / 5** | +2 | Üç veri kaybı yolu, kilit ve park etme kapandı; kalan: dış değişiklik algılama yok, kilit fail-open |
| Özellik bütünlüğü | **2,5 / 5** | +0,5 | Yeni Belge geldi; ara/değiştir, sekmeler, son dosyalar, dışa aktarma yok |
| UX | **2,5 / 5** | +0,5 | Açılır listeler, ikon, başlıklı diyaloglar; tooltip yok, taşma stratejisi yok, görünüm durumu kalıcı değil |
| Erişilebilirlik | **1,5 / 5** | +0,5 | Odak halkaları bastırılmış, Tab erişimi yok, kontrast AA altı |
| Dağıtım ve güven | **2,5 / 5** | +0,5 | İkon + tek örnek; imza, tag, checksum, release hattı yok, portable kararı uygulanmadı |
| Bakım yapılabilirlik | **4 / 5** | — | Temiz composition root, AGENTS.md kuralları, saf mantık testleri; tek pencere varsayımı borç |
| Hukuki | **2,5 / 5** | −0,5 | GPLv3 + LICENSE var; SPDX/pom metadata/WAV kaynağı/artefaktta lisans yok — bu kalem artık ölçüldüğü için puan gerçeğe çekildi |

**Ağırlıklı genel: ~3,0 / 5 — güvenilir bir beta, henüz 1.0 değil.**

---

## 11. Yol Haritası ile İlişki

Faz planı, sıralama ve kabul kriterleri **`product-roadmap.md`** içinde; burada yalnızca analizin hangi faza düştüğü özetlenir (tekrar kaynak yaratmamak için ayrıntı verilmez):

| Faz | Bu analizdeki karşılığı |
|---|---|
| **P0** — Güven + ilk izlenim | §4.1'deki tüm maddeler kapandı. Açık kalan P0 işleri: RW-15 (araç çubuğu taşması; DE ölçüldü, FR açık), RW-16 (tooltip + Tab + yazı boyutu başlığı), RW-17 (odak halkaları + kontrast), RW-18 (görünüm durumu), RW-19 (otomatik kayıt göstergesi), RW-20 (BOM'da strict decode) |
| **P1** — Portable (kurulumsuz) | §4.2/E (atomik olmayan fallback), §4.2/F (temp temizliği), §4.2/B (kilit fail-open), §3.2/4-5 (`AppLogging` yolu, `module-info`, `javafx-media`, `main.fxml`), §8.2 (tek kanal, checksum, imza, `LICENSE`) |
| **P2** — Editör olma | §5.2: ara/değiştir, dış değişiklik algılama (§4.2/A), kurtarılan belgenin kodlaması (§4.2/C), asenkron G/Ç (§3.3), çökme E2E testi (§9) |
| **P3** — Ürünleşme | §5.2: son dosyalar, sürükle-bırak, dışa aktarma, tercihler, kodlama matrisi; §7/1 placeholder testi; §9 hata enjeksiyonu |
| **P4** — Platform | Sekmeler, depolama soyutlaması, erişilebilirlik denetimi, TestFX/JaCoCo |

**Yol haritasına bu analizle eklenen tek iş kapandı:** ertelenen taslağın otomatik kaydı susturması (§4.1/H), `RecoveryStore.park()` ile çözüldü ve roadmap'te RW-54 olarak kayıtlı. Geriye kalan bulguların tamamı zaten bir faza bağlı.

---

## 12. Go / No-Go

**Herkese açık `1.0.0`: HAYIR. Genel beta (`0.9`): P0'ın görünürlük işleri kapanınca EVET.**

`1.0` için "her şeyi bitir" gerekmiyor; dar bir kapı yeterli:

1. **Kurtarma yeni delik açmıyor** — ertelenen taslak park edildiği için otomatik kayıt hiç susmuyor (RW-54), gerçek `kill -9` senaryosu otomatik testle kanıtlı (RW-24).
2. **Yeni Belge + Ara/Değiştir + dış değişiklik uyarısı** — bir yazarın günlük akışı.
3. **Klavye ve okunabilirlik tabanı** — odak halkaları geri, araç çubuğu Tab ile gezilebilir + tooltip, durum çubuğu AA kontrast.
4. **Araç çubuğu DE ve FR'de taşmıyor** (ölçümle kanıtlı, yalnız DE değil).
5. **Tek kanal portable gerçekten tek** — artefaktlar zip/tar.gz/AppImage, `SHA256SUMS` + tag, "imzasız" durumu dürüstçe yazılı, `LICENSE` pakette, README gerçeği anlatıyor.

Bunlar yaklaşık **4-6 kişi-hafta** (önceki tahmin 3-5'ti; portable geçişi kapsama girdi). Geri kalan her şey sonrasına bırakılabilir.

---

## 13. En Yüksek Getirili İki Bahis

### 1) "Taslağını asla kaybetmeyen editör" — güveni ürün yapın

Kod artık bu iddiaya çok yakın: üç seçenekli kurtarma, tek atomik taslak dosyası, toleranslı metadata, oturum başına slot, tek örnek kilidi. Ertelenen taslağın otomatik kaydı susturması da kapandı (park etme). Kalan iş **altyapı değil, kapatma**: dış değişiklik algılama eklemek, gerçek `kill -9` testini yazmak ve bunu pazarlamak. Bu nişte **hiçbir rakip dayanıklılık satmıyor**, hepsi sadelik satıyor. Mühendislik yatırımınız zaten bu eksende; burada **birinci** olabilirsiniz.

### 2) Türkçe-öncelikli yerelleştirme — kutulanacak bir madde değil, ürün özelliği

Ana dil kalitesinde TR arayüz, iki dilli dokümantasyon, locale tuzaklarında kanıtlanmış doğruluk (artık `LanguageTest` + `MessagesTest` ile sabitlenmiş). FocusWriter'ın çevirileri "idare eder" seviyesinde; Türkçe yazma araçları nişini açık ara kazanmak ulaşılabilir.

**Daktilo sesi iyi bir kanca ama hendek değil** — bir hafta sonu eklenebilecek bir şey. Hendek: **güven + yerelleştirme**.

**Yapılmaması gereken:** v1.2'den önce sekme/çoklu belgeye girişmek. Aylar yer ve "kimsenin kalıp kalmayacağını" belirleyen sıkıcı düzeltmeleri geciktirir.

---

## 14. Bulgu Dizini (24 Ekim 2026'da doğrulanmış kanıt)

### Kapanan

| Bulgu | Kanıt (bugünkü kod) |
|---|---|
| Kurtarma: üç seçenek, kapatma = "sonra sor" | `DialogService.java:198-224`, `AutoSaveService.java:106-136` |
| Toleranslı metadata; tek atomik taslak dosyası | `RecoveryStore.java:76-96, 198-274` |
| Oturum başına taslak + adoption | `RecoveryStore.java:103-158` |
| Tek örnek kilidi (advisory `FileLock`) | `InstanceLock.java:47-59`, `JTypeWriterApp.java:20-34` |
| Ayar karantinası (`.corrupt`) | `AppSettings.java:184-245` |
| Log'da yığın izi + cause zinciri | `AppLogging.java:52-66, 88-110` |
| Yeni Belge + undo sıfırlama | `FileService.newDocument`, `UndoHistoryReset` |
| Araç çubuğu: 6 açılır liste, tek `refresh()` | `ToolbarView.java:223-242, 335-349` |
| Tema etiketi / pencere başlığı tazelenmesi | `ToolbarView.java:250-255`, `MainController.java:71-74` |
| Diyalog sahipliği ve başlıkları | `DialogService.java:226-233`, `WelcomeDialog.java:91-96`, `AboutDialog.java:120-125` |
| Uygulama ikonu (pencere + paketleyici) | `AppIcons.java:26-111`, `package.sh:83-94, 119` |
| DE/TR/`recoveryNo` çevirileri, tema sırası | `messages_de.properties:57`, `messages_tr.properties:28,67`, `MessagesTest` |
| Native dil adları (locale tuzağı yok) | `Language.java:18-27`, `LanguageTest` |
| README: fat JAR platforma özgü, `⌘` doğru | `README.md:85-89, 281` |

### Açık

| Bulgu | Kanıt |
|---|---|
| Dış değişiklik algılama yok | `FileService.java:162-179, 225-263` |
| Kilit fail-open + adoption = canlı taslak silinebilir | `InstanceLock.java:47-68`, `RecoveryStore.java:148-158` |
| Kurtarılan belge okunamayan orijinali UTF-8 yazabilir | `FileService.java:186-203` |
| BOM'lu yolda lenient decode | `TextCodec.java:48-58, 116-126` |
| Atomik olmayan move fallback | `AtomicFiles.java:65-69` |
| Açılışta `jtw-*.tmp` temizliği yok | `AtomicFiles.java:72-75` |
| Yalnız en yeni taslak sunuluyor, eskiler temizlenmiyor | `RecoveryStore.java:124-129` |
| Yedek dosyası atomik değil | `BackupKeeper.java` (`Files.copy`) |
| Geometride `NaN`/`Infinity` ayıklaması yok | `AppSettings.java:276-286`, `MainStage.java:29-40` |
| Senkron G/Ç (FX thread) | `FileService.java:164,189,250`, `AutoSaveService.java:154` |
| Araç çubuğu taşma stratejisi yok, ölçüm yok | `ToolbarView.java:40, 164-177`, `base.css:74-149` |
| Tooltip yok | `src/main` (eşleşme yok) |
| Düğmeler/combolar `focusTraversable(false)` | `ToolbarView.java:340, 463` |
| Odak halkaları bastırılmış | `base.css:11-12, 85-86, 95-96` |
| Durum çubuğu kontrastı AA altı | `light.css:10`, `dark.css:10`, `sepia.css:10` |
| Görünüm durumu kalıcı değil (toolbar/fullscreen/stats) | `MainController.java:29`, `MainStage.java:45`, `StatsBar.java:26` |
| Hoş geldin diyaloğu kaydırılamıyor | `WelcomeDialog.java:86, 98` |
| `MessageFormat` varsayılan locale ile | `Messages.java:228, 305, 318, 366, 378`, `DialogService.java:243` |
| Placeholder/apostrof testi yok | `MessagesTest` (yalnız anahtar eşitliği) |
| `AppLogging.init()` yolu enjekte edilemiyor | `AppLogging.java:121-144` |
| `module-info` yok; `javafx-media` + `javafx-fxml` duruyor; `main.fxml` boş | `pom.xml:29-38`, `main.fxml:3-7`, `FxApplication.java:30-37` |
| `AppDirs` tüm OS'te `~/.jtypewriter` | `AppDirs.java:14-16` |
| Portable kararı uygulanmadı; installer üretimi sürüyor | `package.sh:148-165`, `maven.yml:65-67` |
| Tag/release tetiği ve checksum yok | `.github/workflows/maven.yml:10-14` |
| `--license-file` yok; SPDX/pom metadata yok | `package.sh`, `pom.xml`, `src/` |
| `keystroke.wav` kaynağı/lisansı belgesiz | `src/main/resources/audio/keystroke.wav` |
| README test tablosunda `AppIconsTest` yok | `README.md:347-369`, `README-TR.md:348-370` |
