# JTypeWriter — Kapsamlı Analiz ve Ürünleştirme Değerlendirmesi

> Tarih: 23 Ekim 2026 · Sürüm: `1.0.0` (`pom.xml`) · Kapsam: tüm `src/main`, `src/test`, `pom.xml`, `scripts/package.sh`, `.github/workflows/maven.yml`, `README.md`
> Yöntem: iki bağımsız tam-kaynak denetimi + bağımsız ürün stratejisi değerlendirmesi; kritik bulgular elden (`file:line`) doğrulandı; `mvn -B test` çalıştırıldı (çıkış kodu 0, 101 `@Test`).

---

## 1. Yönetici Özeti

JTypeWriter, küçük ama **mühendislik disiplini ortalamanın belirgin üstünde** olan bir JavaFX metin editörü. Kod tabanı 3.671 satır ana kod + ~1.431 satır test (34 ana sınıf, 18 test sınıfı, 101 test). Atomik yazma + `fsync` + yedek rotasyonu, kodlama koruma akışı, locale tuzaklarına karşı bilinçli i18n ve elle kurulan temiz bir composition root (`AppGraph`) gerçekten iyi tasarlanmış.

Buna karşılık ürün **"editör" olarak eksik, "ürün" olarak değil**: Yeni Belge yok, ara/değiştir yok, sekmeler yok, son dosyalar yok, sürükle-bırak yok, yazdır/dışa aktar yok, tercihler penceresi yok, uygulama ikonu yok, dosya ilişkilendirmesi yok. Dağıtım tarafında imzasız `.msi`/`.dmg` (Gatekeeper/SmartScreen), sürüm etiketi/checksum yok. Kurtarma akışında **veri kaybı üreten üç somut yol** var — yani ürünün iddia ettiği tek gerçek farklılaştırıcı ("asla taslağını kaybetmez") şu anda kendi kodunda çürütülüyor.

**Karar: herkese açık `1.0.0` olarak yayınlanmamalı.** `0.9` genel beta olarak yayınlanabilir; `1.0` için aşağıdaki 5 maddelik kapı kapanmalı. Olgunluk skoru: **2,5 / 5**.

En yüksek getirili iki bahis: **(1) "taslağını asla kaybetmeyen editör" güven hikâyesini bitirmek**, **(2) Türkçe-öncelikli (sonra DE/FR/IT/ES) yerelleştirme kalitesini ürün özelliği olarak satmak.**

---

## 2. Ürün Kimliği ve Pazar Konumu

### 2.1 Ne satıyor?

README'nin ve kodun ortaya koyduğu üç değer önerisi:

1. **Odaklanma / sade arayüz** — tek `TextArea`, tema sistemi, istatistik çubuğu, fullscreen.
2. **Daktilo hissi** — her tuş vuruşunda `keystroke.wav`, "YOLO modu" (silme/geri alma kilitli, sadece ileri yazma).
3. **Veri güvenliği** — atomik kayıt, dönen yedekler, otomatik kurtarma, kodlama koruma.

Bunlardan **sadece üçüncüsü savunulabilir bir farklılaştırıcı**; ilk ikisi hafta sonu projesi düzeyinde taklit edilebilir.

### 2.2 Kimin için?

Kod tabanındaki en güçlü sinyal **Türkçe-öncelikli** olması: i18n katmanı Türkçe noktalı-i tuzağına karşı özel olarak sertleştirilmiş (`Messages` `getNoFallbackControl`), CI testleri `tr_TR` ve `de_DE` locale'lerinde yeniden koşuyor (`maven.yml`), README iki dilli ve tam TR kopyası var. Altı dil (EN/TR/DE/FR/IT/ES) gerçek çeviri içeriyor, makine çevirisi kokmuyor.

Hedef kitle: **yerel-öncelikli, hesapsız, çevrimdışı yazma yüzeyi isteyen yazarlar/öğrenciler** — özellikle Türkçe konuşanlar.

### 2.3 Rekabet

| Rakip | Gücü | JTypeWriter'ın şansı |
|---|---|---|
| **FocusWriter** | Olgun, özellik dolu, 20+ dil, oturumlar | Özellikte **kazanamaz**. Duygu (daktilo sesi, temalar) ve Türkçe kalitesinde kazanabilir |
| **iA Writer** | Tasarım ikonu, Markdown, Apple ekosistemi | Ulaşılamaz; bu rekabete girilmemeli |
| **Typora / Obsidian** | Markdown + bilgi yönetimi | Yanlış savaş; plugin/Markdown altyapısı yok |
| **ghostwriter** | KDE/Markdown, ücretsiz | "Markdown zorunlu değil" ve Windows/macOS kullanıcısında kazanabilir |
| **Hemingway** | Okunabilirlik geri bildirimi, ücretli web | Çakışma yok (okunabilirlik eklenmedikçe) |

**Sonuç:** Konumlandırma "bir başka distraction-free editör" değil, **"Türkçe + Avrupa dilleri için güvenli, yerel, daktilo hisli editör"** olmalı. FocusWriter'ın özellik yarışına girmek kaybedilen bir yatırımdır.

---

## 3. Mimari Değerlendirme

### 3.1 Güçlü yanlar

- **Composition root disiplini.** `AppGraph` (satır 39-75) elle DI yapıyor; alan sırası bağımlılık sırası, dinleyiciler constructor'da bağlanıyor. Spring'den kurtulma kararı (`git log`: "Replace Spring Boot DI with a manual composition root") doğru.
- **Durum türetme.** Kirli bayrak ayrı tutulmuyor; `FileService.hasUnsavedChanges()` içerik karşılaştırmasıyla türetiliyor (`FileService.java:72-76`). Tek doğruluk kaynağı.
- **Test edilebilirlik için saf mantık ayrımı.** `MainController.titleFor`, `MainStage.intersectsAny`, `KeyboardShortcutHandler.resolve`, `YoloPolicy`, `DialogService.withDefaultExtension` — headless test edilebilir çekirdek. AGENTS.md bunu kural hâline getirmiş.
- **YOLO modu tek huniden geçiyor.** `EditorArea.replaceText` tüm yıkıcı düzenlemelerin tek kesişimi; `clear()` ve undo/redo ayrıca kapatılmış (`EditorArea.java:26-46,84-118`). Bu, "davranışı kontrol et, UI'ı değil" yaklaşımının iyi bir örneği.

### 3.2 Yapısal borç

1. **Tek pencere / tek belge varsayımı her yere işlemiş.** `FileService` tek `currentFilePath` + tek `savedContent` tutuyor (`FileService.java:53-57`); `AutoSaveService` tek `Timeline` ve tek draft slotuna bağlı; UI tek `EditorArea`. Sekme/çoklu belge **yama değil yeniden tasarım** gerektirir.
2. **Depolama soyutlaması yok.** `FileService` doğrudan `Files`/`AtomicFiles` kullanıyor, `BackupKeeper` içeride `new`'leniyor (`FileService.java:50`), `RecoveryStore` `AppDirs.recoveryDir()` ile sabit (`AppGraph.java:48`). Bulut/şifreli/alternatif depolama eklemek bu sınıfları değiştirmek demek.
3. **Yaşam döngüsü sözleşmesi yok.** `StatsBar` `Timeline`'ı, `AppGraph.stage`, `AudioClip`, `LocaleManager` dinleyicileri hiç bırakılmıyor. Tek pencere için zararsız; çoklu pencere/test izolasyonu için tuzak.
4. **JavaFX kontrolleri `Application.init()` içinde kuruluyor** (`FxApplication.java:20-24` → `AppGraph` alan başlatıcıları). `init()` FX uygulama thread'inde değil; bugün çalışıyor ama garanti edilen bir sözleşme değil ve çoklu pencere/headless geleceğini kilitliyor.
5. **`module-info` yok, fat JAR + classpath.** `javafx-media` sadece tek bir WAV tık sesi için taşınıyor — jpackage imajına platforma özgü medya native kütüphaneleri ekliyor.
6. **`AppDirs` her işletim sisteminde `~/.jtypewriter`** (`AppDirs.java:14-25`). Windows'ta `%APPDATA%`, macOS'ta `~/Library/Application Support`, Linux'ta XDG standardı yok; taşınabilir mod veya geçiş (migration) yok.

### 3.3 Threading

Tüm belge G/Ç'si **FX thread'inde senkron**: açma (`FileService.java:117`), kaydetme + `fsync` (`FileService.java:203`), otomatik kayıt (`AutoSaveService.java:103-106`), ayarlar (`AppSettings.java:195`). Ağ sürücüsünde veya büyük dosyada donma riski; dosya boyutu sınırı, ilerleme, iptal yok. Ayrıca kirli kontrolü her tuşta tam string karşılaştırması, istatistikler her saniye tam belge taraması (`SessionStats.java:64-79`) — büyük belgede ölçülebilir maliyet.

---

## 4. Veri Güvenliği — Ürünün Çekirdeği ve En Zayıf Noktası

Kayıt yolu gerçekten iyi: temp dosya → `FileChannel.force(true)` → izin koruması → `ATOMIC_MOVE` → dizin `fsync`; hata hâlinde temp temizliği (`AtomicFiles.java:50-90`); `ATOMIC_MOVE` desteklenmezse **atomik olmayan** `Files.move`'a düşüyor (`AtomicFiles.java:66-69`) — bu, "asla yarım dosya kalmaz" garantisini sessizce gevşetiyor. Yazma yolu kodlamada `REPORT` kullanıyor, `?` ile sessiz ikame yok (`TextCodec.encode`). Bunlar doğru kararlar.

### P0 — Veri kaybı üreten doğrulanmış yollar

**A. Kurtarma diyaloğu kapatılınca taslak siliniyor.**
`DialogService.showRecoveryDialog` yalnızca "Geri yükle / Sil" düğmeleri sunuyor (`DialogService.java:185-200`); pencere kapatma `Optional.empty()` döndürüyor → `AutoSaveService.offerRecovery` bunu "reddet" sayıp `store.clear()` çağırıyor (`AutoSaveService.java:86-92`). Kullanıcı yanlışlıkla çarpıya basarsa kurtarılabilir metin gider. "İptal / sonra sor" yolu yok.

**B. Bozuk metadata, kurtarılabilir metni çöpe atıyor.**
`RecoveryStore.loadDraft()` tüm gövdeyi tek `try` içinde; `Instant.parse` bozuksa veya `Properties.load` hata verirse `Optional.empty()` dönüyor (`RecoveryStore.java:81-90`) — oysa `draft.txt` sağlam. Açıklamada "draft kurtarılır, sadece bağlantı kaybolur" deniyor ama zaman damgası hatası taslağı da düşürüyor. Ayrıca `draft.txt` ve `draft.properties` **iki ayrı atomik yazma**; arada çökme yarım durum bırakır (`RecoveryStore.java:49-58`).

**C. Tek draft slotu, çoklu örnek çakışması.**
Dosya adı sabit `draft.txt` (`RecoveryStore.java:35`), dizin global (`AppDirs.recoveryDir()`). İki JTypeWriter örneği (veya ikinci bir kullanıcı oturumu) aynı taslağı ezer; `settings.properties` için de aynı sorun var (`AppSettings.save` → son yazan kazanır). Kilit/single-instance koruması yok.

**D. Dış değişiklik algılama yok.**
`FileService.writeFile` yazmadan önce dosyanın mtime/boyut/hash'ini karşılaştırmıyor (`FileService.java:158-216`). Bulut senkronizasyonu veya başka bir editör aynı dosyayı değiştirdiyse sessizce üzerine yazılıyor. "Güvenli editör" iddiası için bu boşluk kritik.

**E. Gevşek (lenient) kod çözme sessiz bozulma üretiyor.**
BOM'lu UTF-8/UTF-16 yolları `decodeLenient` kullanıyor (`TextCodec.java:48-58`); BOM'lu ama bozuk baytlar `U+FFFD` ile değiştiriliyor ve kullanıcı haberdar edilmiyor. BOM'suz yolda strict decode + legacy prompt doğru; BOM yolunda aynı titizlik yok.

### P1 — Dayanıklılık

- **Çökme sonrası temp dosya çöpü.** `Files.createTempFile(dir, "jtw-...")` (`AtomicFiles.java:52`); çökmede tam belge metni `jtw-*.tmp` olarak kalır, açılışta temizlik yok. Aynı sınıf `TypewriterSound` içindeki `deleteOnExit` WAV çıkarımı için de geçerli (`TypewriterSound.java:103-105`).
- **Bozuk ayarlar karantinaya alınmıyor.** `AppSettings.readFromDisk` hatayı loglayıp varsayılana dönüyor, sonraki `save()` bozuk dosyanın üzerine yazıyor (`AppSettings.java:179-199`). Şema sürümü/migration yok.
- **Log'lar teşhis için yetersiz.** `ONE_LINE` formatter throwable'ı `toString()` ile ekliyor — **stack trace yok** (`AppLogging.java:37-43`). Uzaktan hata ayıklama neredeyse imkânsız.
- **Geometri doğrulaması eksik.** `Double.parseDouble` ile `Infinity`/`NaN` kabul edilebiliyor; 1×1 pencere riski (`AppSettings.java:219-229` → `MainStage.java:38-39`).
- **`showError` sahipsiz** (`DialogService.java:203-210`), ana pencerenin arkasında kalabilir.
- **Kodlama matrisi dar:** UTF-8, UTF-16 BOM, Windows-1254. Latin-9/1252/Shift-JIS yok — Türkçe dışı eski belgelerde tıkanır.

---

## 5. Özellik Bütünlüğü

### 5.1 Var olanlar (ve gerçekten çalışanlar)

Dosya aç/kaydet/farklı kaydet, oturum geri yükleme, atomik kayıt + 3 nesil yedek, otomatik kayıt + kurtarma, kodlama tespiti/koruma/UTF-8'e yükseltme istemi, 3 tema + 6 metin rengi, yazı boyutu, 6 dilde anlık değişim, daktilo sesi, YOLO modu, canlı istatistik (kelime/karakter/satır/tuş/süre/WPM), tam ekran, pencere geometrisi hatırlama, hoş geldin ve hakkında diyalogları, çökme yakalayıcı.

### 5.2 Olmayanlar — bir metin editörünün asgari tabanı

| Eksik | Etki | Kanıt |
|---|---|---|
| **Yeni Belge** | `FileService` yalnızca aç/kaydet sunuyor; `Ctrl+N` yok. YOLO açıkken "hepsini seç + sil" de engelli → yeni belge pratikte imkânsız | `FileService.java:87-170`, `MainController.java:108-112` |
| **Ara / Değiştir** | Hiç yok (`grep` yalnızca `replace` iç çağrılarını buluyor) | tüm `src/main` |
| **Sekmeler / çoklu belge** | Açmak mevcut tamponu eziyor | `FileService.java:53-57` |
| **Son dosyalar** | Yalnız `last.file` + `last.directory` | `AppSettings.java:46-57` |
| **Sürükle-bırak** | `setOnDragOver`/`setOnDragDropped` yok | tüm `src/main` |
| **Yazdır / dışa aktar** | Yok | — |
| **Yazım denetimi** | Yok | — |
| **Tercihler penceresi** | Tema/dil/ses düğmelerle döngüsel | `ToolbarView.java:122` |
| **Dosya ilişkilendirmesi / CLI argümanı** | `getParameters()` okunmuyor; `.txt` çift tıklama çalışmaz | `FxApplication.java:39-84` |
| **Uygulama ikonu** | `src/main/resources` altında görsel yok; `package.sh` `--icon` geçmiyor | `package.sh` |
| **Yeni belgede undo sıfırlama** | `setText` undo geçmişini temizlemiyor → belgeler arası undo riski | `FxApplication.java:77-80`, `FileService.loadFile` |

**Değerlendirme:** Özellik bütünlüğü 5 üzerinden **2**. Ücretsiz rakiplerin taban çizgisinin altında. "Distraction-free" sadeliği savunulabilir, ama **Yeni Belge ve Ara/Değiştir sadelik değil, eksikliktir.**

### 5.3 README ile gerçek arasındaki farklar

- Fat JAR "runnable" olarak sunuluyor (`README.md:60-67`) ama içinde **yalnızca derleyen platformun JavaFX native kütüphaneleri** var (yerel `target/`'da yalnız `.so`). Farklı OS'te çalışmaz.
- Kısayol listesi tüm dillerde "Ctrl" yazıyor, README macOS için `⌘` iddia ediyor (`README.md:227` vs `messages*.properties:28`).
- README "6 text color" diyor, `TextColor` enum'ı ile uyumlu; ancak tema sırası TR metinde "Dark/Light/Sepya" yazıyor, gerçek döngü LIGHT→DARK→SEPIA.

---

## 6. UX ve Erişilebilirlik

### 6.1 Görsel/etkileşim sorunları

- **Araç çubuğu taşıyor.** 18 çocuklu, sarma özelliği olmayan tek `HBox` (`ToolbarView.java:129-146`); Almanca/Fransızca uzun etiketlerde veya büyük sistem fontunda kırpılıyor. Ayırıcılar literal `Label("|")`.
- **Araç çubuğu gizlenince geri getirmenin görünür yolu yok** — sadece `Ctrl+T`.
- **Tema etiketi bayatlıyor:** `Ctrl+Shift+T` → `MainController.cycleTheme()` yalnız `themeManager.cycleTheme` çağırıyor, `themeLabel` güncellenmiyor (`MainController.java:123-128` vs `ToolbarView.java:189-196`).
- **Dil değişince pencere başlığı eski dilde kalıyor:** `MainController` `LocaleManager` dinlemiyor (`MainController.java:35-47`); `ToolbarView` dinliyor (`ToolbarView.java:148`).
- **Diyaloglar sabit boyutlu, dekorasyonsuz, başlıksız:** Hoş geldin 460×400, 14 satırlık kısayol bloğu var ve `ScrollPane` yok (`WelcomeDialog.java:59-68`); Hakkında 540×640 sabit (`AboutDialog.java:92-101`). Görev değiştiricide pencere adı boş.
- **Kalıcı olmayan görünüm durumu:** araç çubuğu görünürlüğü, tam ekran, istatistik çubuğu boyutu her açılışta sıfırlanıyor (`MainController.java:29`, `MainStage.java:45`, `StatsBar.java:26`).
- **Otomatik kayıt durumu görünmüyor.** Kirli göstergesi yalnız başlıktaki `*`; "30 sn sonra kaydedildi" hissi yok.
- **YOLO modunda düğmeler sessizce no-op** (`ToolbarView.java:108-113`); devre dışı görünmüyorlar.

### 6.2 Erişilebilirlik — yayınlanabilir değil

- Araç çubuğu düğmeleri `setFocusTraversable(false)` (`ToolbarView.java:259` civarı) → klavyeyle ulaşılamaz.
- Odak halkaları CSS'te küresel olarak bastırılmış: `-fx-focus-color: transparent; -fx-faint-focus-color: transparent` (`base.css:85-86, 116-117`).
- Tooltip yok, `accessibleText`/`accessibleRole` yok.
- Durum çubuğu 11px ve düşük kontrast: `#888` / `#f5f5f5`, `#777` / `#1e1e1e`, `#8b7355` / `#e8dcc8`.
- Sabit px tipografi; sistem font ölçeğine tepki yok.

**Erişilebilirlik skoru: 1/5.** Bu, "eksik özellik" değil, "belirli kullanıcı gruplarını dışlama".

---

## 7. i18n Kalitesi

**İyi:** Altı bundle'da 78 anahtarın tamamı eşleşiyor ve `MessagesTest` bunu zorluyor; `getNoFallbackControl` ile sistem locale'ine sessiz düşme engellenmiş; canlı dil değişimi çalışıyor; tema/renk adları bundle'dan geliyor (hardcoded değil).

**Sorunlar:**
1. **Almanca "Redo" yanlış:** `toolbarRedo=Wiederherstellen` ("geri getir") — aynı dosyada kısayol listesinde `Wiederholen` kullanılıyor (`messages_de.properties:56` vs `:28`).
2. **Türkçe hoş geldin metni yarım çeviri:** "Toolbar", "Dark/Light", "Font" İngilizce kalmış; tema sırası yanlış (`messages_tr.properties:28`).
3. **"Sil" vs "Vazgeç":** kurtarma diyaloğunda `recoveryNo` TR/FR/IT'de "Delete" anlamında; oysa niyet "kaydetme/vazgeç". Veri kaybı riskini artıran bir metin hatası (`messages_tr.properties:66` vb.).
4. **`MessageFormat` varsayılan JVM locale'i ile çalışıyor** (`Messages.java:222-224` ve benzerleri); istatistik sayıları ve kurtarma zaman damgası UI dilinden farklı biçimlenebilir.
5. **Test boşluğu:** anahtar eşitliği test ediliyor, **placeholder sayısı/tipi ve apostrof kaçışı test edilmiyor**. `{7}` gibi bir hata çalışma anında patlar.
6. **Dil düğmesi iki harfli kod gösteriyor** (`ToolbarView.java:80`), dil menüsü yok; kullanıcı altı dili sırayla döngüyle geziyor.
7. **RTL desteği yok**, uzun çeviri metinleri için layout testi yok.

---

## 8. Build, Paketleme, Dağıtım

### 8.1 İyi

- Sürüm tek kaynak: `pom.xml`; `package.sh` sürümü derlenmiş artefakt adından geri okuyor ve jpackage kurallarına karşı doğruluyor (`package.sh:36-67`).
- `--skip-build` ile bayat jar paketlenmesini engelleyen "tam olarak bir fat jar" kontrolü ve staging temizliği var.
- jpackage uygulama imajı + `.deb`/`.msi`/`.dmg`; Linux kısayolu, Windows menü/dizin seçici, macOS bundle id ayarlanmış.
- CI üç OS matrisi + TR/DE locale test tekrarı; artefakt yüklemesi var.

### 8.2 Eksik / riskli

| Sorun | Etki |
|---|---|
| **İmzalama / notarization yok** | macOS Gatekeeper bloklar, Windows SmartScreen uyarır — ilk izlenimde ürün "güvenilmez" görünür |
| `--icon`, `--license-file`, `--file-associations` yok | Native uygulama gibi davranmıyor; lisans dosyası kuruluma girmiyor |
| CI yalnız `push`/`PR` → `main`; tag/release tetiği yok | "Release" workflow artefaktı, sürüm yayını değil; checksum yok, 90 gün sonra artefakt silinir |
| Actions `@v4` gibi major tag'lere pinli, `permissions:` yok | Tedarik zinciri sertleştirmesi eksik |
| ARM64 (Linux/Windows) ve universal macOS yok | Kullanıcı tabanının bir kısmı dışarıda |
| `--skip-build` yalnız jar sayısını kontrol ediyor, kaynakla eşleşmeyi değil | Elle paketlemede bayat sürüm riski |
| Installer temizliği büyük/küçük harf duyarlı (`package.sh:82`) | Eski artefakt kalabilir, jpackage reddedebilir |
| `module-info` yok, fat jar classpath'te | Gelecekteki jlink/native-image yolu kapalı; JavaFX media native'leri imaja giriyor |

### 8.3 Hukuki

- GPLv3 + `LICENSE` mevcut, README'de bölüm var. Ancak **hiçbir kaynak dosyada SPDX/lisans başlığı yok**, `pom.xml`'de `<licenses>/<developers>/<scm>` yok, kurulum paketlerine lisans dosyası girmiyor.
- `src/main/resources/audio/keystroke.wav` **kaynağı ve lisansı belgelenmemiş** — GPLv3 dağıtımında ciddi bir uyum sorunu olabilir.
- "JTypeWriter" adı/markası için ayrı bir değerlendirme yok (düşük risk).

---

## 9. Test ve Kalite Güvencesi

**Çalıştırma sonucu:** `mvn -B test` → **başarılı (exit 0)**, 101 `@Test`. Uyarılar beklenen türde (Mockito self-attach, `sun.misc.Unsafe`). Testlerin gerçek `~/.jtypewriter` dizinine dokunmadığı iddiası **tam doğru değil**: statik logger'lar `AppLogging.init()` üzerinden gerçek log dizinini açabiliyor (`AppSettings.java:44`, `AppLogging.java:67-71`); testler sıraya duyarlı hâle gelebilir ve Windows'ta dosya tutamacı bırakabilir.

**İyi kapsanan (davranış odaklı, headless):** `TextCodec` (BOM/round-trip/strict encode), `AtomicFiles` (içerik, temp temizliği, izin koruma), `FileService` (kirli akış, yedek rotasyonu, 1254 round-trip, UTF-8 yükseltme), `RecoveryStore`, `AppSettings`, `BackupKeeper`, `SessionStats`, `Messages` (anahtar eşitliği + locale tuzakları), saf yardımcılar.

**Kritik boşluklar:**
- **Uçtan uca çökme/kurtarma senaryosu yok** (taslak yaz → süreç öldür → açılış → geri yükle → kaydet).
- `AutoSaveService.attach` (timeline, odak kaybı, `WINDOW_HIDDEN`), `offerRecovery`, `shutdown` neredeyse test edilmemiş.
- Hata enjeksiyonu yok: disk dolu, salt-okunur dizin, yedek hatası, `fsync` hatası.
- Dış değişiklik / symlink / hard-link senaryoları yok.
- JavaFX toolkit/TestFX testi **hiç yok**: `ToolbarView`, `EditorArea`, `StatsBar`, tema yöneticileri, diyaloglar, gerçek kısayol dağıtımı doğrulanmıyor.
- `YoloPolicy` politika olarak test edilmiş; `TextArea`'nın paste/IME/native menü yüzeyinin gerçekten `replaceText`'e aktığı **entegrasyon testi yok**.
- Placeholder doğruluğu, CSS geçerliliği, FXML yüklenebilirliği, paketleme betiği test edilmiyor.
- JaCoCo / kapsam ölçümü yok.

---

## 10. Olgunluk Skorları

| Boyut | Puan | Gerekçe |
|---|---|---|
| Mühendislik temelleri | **4/5** | Atomik kayıt, elle DI, headless test disiplini, locale CI — solo proje için çok iyi; FX thread ihlalleri ve senkron G/Ç puanı düşürüyor |
| Özellik bütünlüğü | **2/5** | Yeni Belge, Ara/Değiştir, sekmeler, son dosyalar, dışa aktarma yok |
| UX | **2/5** | Sabit boyutlu dekorasyonsuz diyaloglar, taşan araç çubuğu, bayat etiketler, tooltip yok |
| Erişilebilirlik | **1/5** | Odak halkaları bastırılmış, klavye erişimi yok, düşük kontrast |
| Dağıtım ve güven | **2/5** | jpackage çalışıyor; ikon yok, imzasız kurulumlar, release/tag/checksum yok |
| Bakım yapılabilirlik | **4/5** | Temiz composition root, AGENTS.md kuralları, saf mantık testleri; tek pencere varsayımı borç |
| Hukuki | **3/5** | GPLv3 + LICENSE var; kaynak başlıkları, pom metadata, WAV kaynağı, kurulumda lisans eksik |

**Ağırlıklı genel: ~2,5 / 5 — inandırıcı bir hobi projesi, yayına hazır bir ürün değil.**

---

## 11. Yol Haritası

### A. Genel yayın öncesi zorunlu (v1.0 kapısı) — tahmini 3-5 kişi-hafta

| # | İş | Efor |
|---|---|---|
| 1 | Kurtarma diyaloğuna **İptal** ekle; pencere kapatma taslağı silmesin; bozuk metadata taslağı düşürmesin; draft+meta tek atomik dosya | S |
| 2 | Çoklu örnek çakışması: kilit veya örnek-başına draft dosyası | S-M |
| 3 | **Yeni Belge** + belge yüklerken undo geçmişini sıfırla | S |
| 4 | **Ara / Değiştir** | M |
| 5 | Dış değişiklik algılama (mtime+boyut, isteğe bağlı hash) ve çakışma diyaloğu | M |
| 6 | Belge G/Ç'sini FX thread'inden çıkar (worker + ilerleme/iptal) | M |
| 7 | Uygulama ikonu + diyalog başlıkları + tooltip'ler | S |
| 8 | Odak halkalarını geri getir, araç çubuğunu Tab ile gezilebilir yap, durum metnini ≥13px ve kontrastlı yap | M |
| 9 | Araç çubuğunu sarma/taşma stratejisiyle düzelt; DE/FR metinlerini kısalt | S |
| 10 | DE "Redo", TR karışık metin, `recoveryNo` çevirileri; macOS'ta `⌘`; dil değişiminde başlık yenile | S |
| 11 | README dürüstlüğü: fat JAR platforma özgü; WAV kaynağı belgelenmeli; kuruluma lisans dosyası | S |
| 12 | Release akışı: tag tetiği, checksum, `--icon`, `--license-file`, `--file-associations`; imzalama (en azından macOS notarization) | M-L |

### B. v1.x farklılaştırıcıları

- Son dosyalar listesi + sürükle-bırak (M)
- Yazdır / Markdown ve PDF dışa aktarma (M)
- Tercihler penceresi; dil/tema için açılır menü (L)
- Belge-başına otomatik kayıt slotu ve görünür otomatik kayıt durumu (M)
- Ayarlar bozulunca `.corrupt` karantinası + şema sürümü/migration (S)
- Log'lara stack trace, sürüm/OS metadata'sı, "tanılama paketi" (S-M)
- Kodlama matrisini genişlet (Windows-1252, ISO-8859-9, Shift-JIS) (M)
- `javafx-media` yerine `javax.sound` (S) — bir bağımlılık ve native yük azalır

### C. v2 platform işleri

- Çoklu belge / sekmeler (XL — `FileService`, `AutoSaveService`, UI yeniden tasarımı)
- Depolama soyutlaması + yaşam döngüsü/dispose sözleşmesi (L)
- `AppDirs` için XDG / `%APPDATA%` / `Application Support` + taşınabilir mod (M)
- `module-info` (M), TestFX/JaCoCo ile E2E ve kapsam (L)
- Yazım denetimi (L), dosya ilişkilendirme + CLI argümanı (M)
- Erişilebilirlik denetimi (NVDA/VoiceOver) ve tam klavye navigasyonu (M)

---

## 12. Go / No-Go

**Herkese açık `1.0.0`: HAYIR.**

Gerekçe iki maddede: (a) kurtarma akışındaki üç veri kaybı yolu, ürünün **tek gerçek vaadini** çürütüyor; (b) ikonsuz, imzasız, 11px kontrastsız, dekorasyonsuz diyaloglu bir ilk izlenim, kullanıcıyı 30 saniyede kaybeder — özellik eksiklerinden bağımsız olarak.

**Öneri: `0.9` genel beta** ("JTypeWriter 0.9 beta — Türkçe-öncelikli, sade yazma editörü"). Beta için DMG'yi atlayın (jpackage macOS'ta `0.x` kabul etmez) veya sürüm şemasını `1.0.0-beta` yerine `0.9.0` + macOS atlaması olarak uygulayın.

### Minimal inandırıcı yayın kapısı (v1.0)

1. **Kurtarma asla sessizce metin kaybetmez** — diyalog kapatma, bozuk metadata, çoklu örnek.
2. **Yeni Belge + Ara/Değiştir + belge-başına undo.**
3. **İkon, diyalog başlıkları, tooltip, odak halkaları, okunabilir durum metni, taşmayan araç çubuğu.**
4. **DE/TR çeviri ve kısayol düzeltmeleri; macOS'ta `⌘`.**
5. **İmzalı ya da açıkça "imzasız" işaretlenmiş kurulumlar; dürüst README; tag'li, checksum'lı sürüm.**

Bunlar yaklaşık **3-5 kişi-hafta**. Geri kalan her şey sonrasına bırakılabilir.

---

## 13. En Yüksek Getirili İki Bahis

### 1) "Taslağını asla kaybetmeyen editör" — güveni ürün yapın

Çakışma algılama + kilitleme hikâyesini bitirin, belge-başına kurtarma slotu ekleyin, çökme güvenliğini uçtan uca test edin (gerçek kill senaryosu) ve bunu pazarlayın. Bu nişte **hiçbir rakip dayanıklılık satmıyor**, hepsi sadelik satıyor. Mühendislik yatırımınız zaten bu eksende; burada **birinci** olabilirsiniz, kopya olmak zorunda değilsiniz.

### 2) Türkçe-öncelikli yerelleştirme — kutulanacak bir madde değil, ürün özelliği

Ana dil kalitesinde TR arayüz, Türkçe dokümantasyon/topluluk, locale tuzaklarında kanıtlanmış doğruluk (zaten test ediliyor). FocusWriter'ın çevirileri "idare eder" seviyesinde; Türkçe yazma araçları nişini açık ara kazanmak ulaşılabilir ve size gerçek kullanıcı, geri bildirim ve katkı getirir.

**Daktilo sesi iyi bir kanca ama hendek değil** — herkesin bir hafta sonu ekleyebileceği bir şey. Hendek: güven + yerelleştirme.

**Yapılmaması gereken:** v1.2'den önce sekme/çoklu belgeye girişmek. Aylar yer ve "kimsenin kalıp kalmayacağını" belirleyen sıkıcı düzeltmeleri geciktirir.

---

## 14. Bulgu Dizini (doğrulanmış kanıt)

| Bulgu | Konum |
|---|---|
| Kurtarma diyaloğunda İptal yok; kapatma = sil | `DialogService.java:185-200`, `AutoSaveService.java:86-92` |
| Bozuk metadata tüm taslağı düşürüyor | `RecoveryStore.java:81-90` |
| Draft + meta ayrı atomik yazmalar | `RecoveryStore.java:49-58` |
| Sabit `draft.txt`, global dizin | `RecoveryStore.java:35`, `AppDirs.java:20-23` |
| `~/.jtypewriter` tüm OS'lerde | `AppDirs.java:14-25` |
| BOM'lu yolda lenient decode | `TextCodec.java:48-58` |
| Atomik olmayan move fallback | `AtomicFiles.java:66-69` |
| Temp dosya çöpü | `AtomicFiles.java:52`, `TypewriterSound.java:103-105` |
| Senkron G/Ç (FX thread) | `FileService.java:117,203`, `AutoSaveService.java:103-106` |
| Dış değişiklik kontrolü yok | `FileService.java:158-216` |
| Ayarlar karantinası/şema yok | `AppSettings.java:179-199` |
| Log'da stack trace yok | `AppLogging.java:37-43` |
| Tema etiketi kısayolda güncellenmiyor | `MainController.java:123-128` vs `ToolbarView.java:189-196` |
| Dil değişiminde başlık bayat | `MainController.java:35-67` |
| Odak halkaları bastırılmış | `base.css:85-86,116-117` |
| Sabit boyutlu, başlıksız diyaloglar | `WelcomeDialog.java:59-68`, `AboutDialog.java:92-101` |
| Panel yapısı FX thread dışında kuruluyor | `FxApplication.java:20-24` |
| İkon / lisans / dosya ilişkilendirmesi yok | `scripts/package.sh` |
| Yalnız push/PR tetikli, tag/release yok | `.github/workflows/maven.yml:10-14` |
| DE "Redo" yanlış çeviri | `messages_de.properties:56` |
| TR karışık çeviri / yanlış tema sırası | `messages_tr.properties:28` |
| `recoveryNo` = "Delete" (TR/FR/IT) | `messages_*.properties:66` |
| Fat JAR "portable" iddiası | `README.md:60-67` |
