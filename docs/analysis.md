# JTypeWriter — Kapsamlı Analiz ve Ürünleştirme Değerlendirmesi (P2 Öncesi)

> **Sürüm:** 3.0 · **Tarih:** 27 Ekim 2026 · **Kod tabanı:** `main` @ `8a649ed`
> **Kapsam:** P0 ve P1 kapanışının doğrulanması, mevcut ürün durumunun ölçülmesi, P2 için
> karar girdileri. **Bu belge bir yol haritası değildir**; iş sıralaması, sahiplik ve takvim
> ayrı bir `docs/product-roadmap.md` belgesinde üretilecektir.
> **Yöntem:** Her bulgu kaynak koda veya ölçüme bağlanmıştır. Ölçülemeyen iddialar
> "ölçülmedi" olarak işaretlenmiştir. Önceki analiz ve yol haritası `f5d00db` ile silindiği
> için bulgu kimlikleri bu belgede **yeniden ve bağımsız** verilmiştir (D/E/X/U/B/M/T).
> **Ölçüm ortamı:** Linux x64, sıcak önbellek, `DISPLAY=:0.0`; JDK 25.0.2.

---

## 1. Yönetici Özeti

### 1.1 Tek cümle

JTypeWriter bugün **güvenilir bir yazma motoru ve dürüst bir ambalajdır, henüz bir editör
değildir**: metni kaybetmemek için tasarlanmış yollar gerçekten çalışıyor, ama bir romancının
bir bölümü bitirmek için ihtiyaç duyduğu günlük döngünün yarısı (ara/değiştir, menü, komut
satırından açma, yazdırma) hiç yok — ve ürün hiç yayınlanmadı.

### 1.2 Ölçülen tablo

| Ölçüt | Değer | Kaynak |
|---|---|---|
| Dil / UI / test | Java 25, JavaFX 26.0.1, JUnit Jupiter 6.0.3, Mockito 5.23.0 | `pom.xml:52,62-64` |
| Üretim kodu | **51 sınıf, 7.611 satır** | `find src/main/java` |
| Test | **40 sınıf, 350 test** | `src/test/java` |
| Yerelleştirme | **6 bundle × 90 anahtar**, EN taban | `src/main/resources/messages*.properties` |
| Uygulama imajı | **145,6 MiB** (runtime 139,97 MB = **%91,7**) | `releases/linux/JTypeWriter` |
| Fat JAR | **11,2 MB** (6.105 giriş) | aynı imaj, `lib/app` |
| Taşınabilir `.tar.gz` | **59,3 MB** | yerel, bayat artefakt |
| `.deb` | **47,3 MB**, 13 runtime bağımlılığı | yerel, bayat artefakt |
| Açılış (ana pencere) | **1,59–1,66 s**, ilk sahne 0,57–0,61 s | 5 koşu, bu makine |
| Bellek (RSS) | **~280 MB** | aynı koşular |
| Yayınlanmış sürüm | **0** (git tag yok, GitHub Release yok) | `git tag -l`, API |
| CI | 3 OS × 3 locale koşusu, 350 test | `.github/workflows/maven.yml` |

### 1.3 En önemli on bulgu

1. **D1 — Dış değişiklik algılama yok.** Yüklemede mtime/boyut saklanmıyor; kaydetme koşulsuz
   yazıyor. `git checkout`, senkron istemcisi veya başka bir editörün değişikliği **sessizce
   ezilir** (`FileService.loadFile` / `writeFile`).
2. **E1 — Ara/Değiştir yok.** `Ctrl+F` yok, arama modeli yok, bundle'da tek bir arama anahtarı
   yok. 300 KB'lık bir roman taslağında cümle bulmanın tek yolu gözle taramak.
3. **B1/B2 — Ürün hiç yayınlanmadı ve CI artefaktı hiç çalıştırmıyor.** `v*` tag'i hiç
   atılmamış; release hattı uçtan uca hiç koşmamış; üretilen imaj/deb yalnızca dosya varlığı
   için doğrulanıyor, **açılmıyor** (`.github/workflows/maven.yml`, `scripts/package.sh`).
4. **X1/X2 — G/Ç JavaFX thread'inde.** Açma/kaydetme `Files.readAllBytes` + `fsync` ile
   doğrudan FX thread'inde; otomatik kayıt her 30 s'de tam metni kopyalayıp serileştiriyor ve
   bu kopya **kirli kontrolünden önce** alınıyor (`AutoSaveService.attach/snapshot`).
5. **U1 — Uyarı diyalogları taşıyor.** 9 `Alert` çağrısının hiçbiri yükseklik kelepçesi,
   satır sarma veya yeniden boyutlandırma kullanmıyor; Almanca/Fransızca uzun metinler
   768 px ekranı aşabilir (`DialogService`).
6. **D2/D3 — "Bu hangi dosya?" modeli yok.** Symlink'li belge kaydedilince linkin kendisi
   normal dosyayla değiştiriliyor (hedef hiç güncellenmiyor); taslak başlığındaki yol
   doğrulanmadan `Path.of` ile kabul edilip sonraki kaydetmenin hedefi olabiliyor.
7. **E2/E3 — OS'ten açılamıyor.** Menü çubuğu yok; `Application.getParameters()` hiç
   okunmuyor, dosya ilişkilendirmesi/MIME yok. Çift tıklama veya "birlikte aç" çalışmaz.
8. **B4 — 145 MB paket, %91,7'si tam JDK.** jlink kullanılmıyor; 52 modül (compiler, javadoc,
   jshell, jfr dahil) taşınıyor. Fat JAR'da yalnızca 71 KB'lık WAV için JavaFX Media, boş bir
   `BorderPane` için FXML var.
9. **P1 kapanışı kısmi.** Yedi çıkış kriterinden **2'si tam, 4'ü kısmi, 1'i açık**; P1'in
   doğrulama diliminde planlanan **artefakt smoke testi** ve **`AGENTS.md`'nin sürüm
   kontrolüne alınması** hiç başlamamış (§2.2–2.3).
10. **Pazar teması kanıtsız.** 350 test ve titiz bir güvenlik hikâyesi var; yayınlanmış sürüm
    olmadığı için ürünü kendisi yazmayan bir yazarın açtığına dair hiçbir kanıt yok. En büyük
    risk listedeki boşluklar değil, bu.

### 1.4 Hüküm

**Motor ≠ ürün.** P1, ürünün "amatör görünmesine" yol açan sinyalleri büyük ölçüde sildi;
kritik veri kaybı sınıfı kapandı. Ama ürün şu an **dürüst bir demo**: vaat ettiği şeyi
(kaybetmemek) tek yazarlı, düz dosyalı, sağlam dosya sistemi varsayımı altında yapıyor; ve
yazarın asıl işini (bölüm yazmak) uçtan uca desteklemiyor.

Skorlar (§11): **motor 3,6 · deneyim 2,2 · ambalaj 2,0 · güven 2,8 · pazara erişim 1,5.**

---

## 2. Yöntem, Kanıt Güvenilirliği ve P1 Kapanış Denetimi

### 2.1 Kanıt seviyeleri

| Seviye | Ne demek | Bu belgede |
|---|---|---|
| **Ölçüldü** | Bu makinede komut/araçla sayı veya süre alındı | Boyutlar, açılış, RSS, sınıf/test/anahtar sayıları |
| **Koddan okundu** | Davranış kaynakta izlendi | D, E, X, U, M bulgularının tamamı |
| **Çıkarıldı** | Koddan mantıksal sonuç, ölçüm yok | 10/50 MB senaryoları, Windows/macOS boyutları |
| **Doğrulanamadı** | Kod garanti vermiyor, test yok | macOS `Cmd+Q` davranışı, render edilmiş CSS, taze `.deb` payload'ı |

**Uyarı — yerel artefaktlar bayat.** `releases/` git'te izlenmiyor ve `HEAD`'den eski.
İmajdaki `NOTICE` 713 B, repodaki 1.338 B; imajdaki jar 11.202.493 B, `.deb` içindeki jar
11.164.155 B (80'e karşı 91 uygulama sınıfı). Yani boyutlar **gösterge**, hukuki payload
iddiaları **temiz bir build ile yeniden doğrulanmalı**. Bu, kod hatası değil, hijyen
bulgusudur (B11).

### 2.2 P1 çıkış kriterleri denetimi

| # | P1 kriteri | Durum | Kanıt / eksik |
|---|---|---|---|
| 1 | Varsayılan temada AA altı tek metin yok; test tüm kombinasyonları zorluyor | **Kısmi** | `ContrastRatioTest` CTA, hover, e-posta, tagline, `.stats-save` ve 5 vurgu rengini kapsıyor; ama `-toolbar-text`, düz popup satırları, `.about-email:hover` ve varsayılan editör metni **ölçülmüyor**. Kullanılmayan `-toolbar-text-muted` token'ı açık/sepya'da AA altıdır ve hiçbir test görmez. |
| 2 | Açılışta odak editörde; hiçbir diyalog 768 px'i aşmıyor; 4 dilde kırpılma yok | **Kısmi** | Özel diyaloglar `DialogSizing` ile kelepçeli; **9 `Alert` çağrısı kelepçesiz, sarmasız, yeniden boyutlandırılamaz**. Odak aktarımı saf politika olarak testli, gerçek traversal değil. |
| 3 | Kapatma isteği dışı çıkış, salt-okunur ev, disk dolu, `kill -9` — dördünde de kayıp yok | **Kapalı** | `ExitDisposition.CLEAR/PARK/KEEP` (`ExitDisposition.java:55-60`), `AutoSaveService.shutdown` (`:271-282`), `ProcessCrashRecoveryTest`. Artıklar: D9, D10. |
| 4 | Hiçbir yolda sessiz kodlama değişimi yok | **Kapalı** | `restoreRecovered` legacy charset'i koruyor; `EncodingMatrixTest` (11 test) bayt-özdeş gidiş-dönüşü sabitliyor. |
| 5 | 6 dilde argümanlı anahtarlar doğru; sayı/tarih UI diliyle uyumlu; ölü anahtar yok | **Kısmi** | Anahtar paritesi, `MessageFormat` ve sayı biçimi testli; **tarih/saat desenleri hâlâ sabit** (`dd.MM.yyyy`, `HH:mm`), ayırıcılar kodda (`": "`, `" - "`), tooltip'ler macOS'ta `Ctrl` diyor. |
| 6 | `v*` tag'i checksum'lı Release üretiyor; her artefaktta lisanslar; POM metadata tam | **Kısmi** | Betikler ve `release.yml` var, POM metadata tam; **hat hiç koşmadı**, tag yok, imza yok, deb payload'ı taze build'de doğrulanmadı. |
| 7 | `mvn clean test` + TR/DE + paket smoke testi yeşil | **Açık** | İlk ikisi yeşil; **paket smoke testi yok** — imaj açılmıyor, deb kurulmuyor. |

**Sonuç:** P1'in en zor kısmı (veri güvenliği + kodlama) gerçekten kapandı. Görünür kalite ve
yayın tarafında ise "kapandı" iddiası **kapsam daraltılarak** doğru: kriter metni geniş,
uygulama dar.

### 2.3 P1'den devreden iki iş

| İş | Durum | Neden P2'ye girmeden kapanmalı |
|---|---|---|
| Üretilen artefaktı CI'da **aç** (app-image başlat, Linux'ta `.deb` kur/kaldır) | Yok | Bugün "paketlendi" ile "çalışıyor" arasındaki tek kanıt yok. Bir sonraki sürümde ilk gerçek regresyon burada yakalanır. |
| `AGENTS.md`'yi sürüm kontrolüne al | Yok (`.gitignore:57`, `git ls-files` boş) | Ajan kuralları PR'da görünmüyor; ekip/otomasyon bilgisi makineye bağlı kalıyor. |

---

## 3. Ürün Kimliği ve Pazar Konumu

### 3.1 Ne satıyor?

Üç katman var ve üçü farklı olgunlukta:

| Katman | İddia | Bugünkü gerçek |
|---|---|---|
| **Motor** | "Metnini kaybetmek çok zor" | Atomik yazma, dönen yedek, 30 s taslak, çıkış matrisi, kodlama koruması — **gerçek ve testli**. Ancak tek yazar / düz dosya / sağlam FS koşullu. |
| **Deneyim** | "Sessiz, dikkat dağıtmayan yazma" | Tek `TextArea`, zorunlu satır sarma, araç çubuğu, durum çubuğu. Arama, menü, yazdırma yok. "Sessiz" ama **çıplak**. |
| **Ambalaj** | "İndir, çalıştır; Java gerekmez" | 145 MB imaj, imzasız, hiç yayınlanmamış, OS'ten açılamıyor. |

Dolayısıyla ürünün **savunulabilir tek farklılaştırıcısı** daktilo sesi değil, **silme engeli +
veri güvenliği** kombinasyonu. Ses bir tatlandırıcı; YOLO ve kurtarma bir ürün tezi.

### 3.2 Kimin için?

- **Birincil:** Tek bir düz metin dosyasında roman/öykü yazan, bulut ve hesap istemeyen,
  "ilk taslak" aşamasında geri silmeyi kapatmak isteyen yazar.
- **İkincil:** Dikkat dağıtıcı olmayan, hafif bir yerel editör arayan Linux kullanıcısı.
- **Üçüncül (henüz değil):** Kurumsal/atölye kullanımı — çoklu belge, yazım denetimi ve
  iş birliği olmadan uygun değil.

### 3.3 Rekabet

| Ürün | Güçlü yanı | JTypeWriter'ın ayrıştığı yer | JTypeWriter'ın kaybettiği yer |
|---|---|---|---|
| Word / Google Docs | Her şey, ücretsiz, tanıdık | Hesapsız, çevrimdışı, düz metin, silme engeli | Ara/değiştir, yazdırma, biçimlendirme, güven |
| Scrivener | Proje/klasör, taslak yönetimi | Tek dosya, öğrenme eğrisi yok | Yapı, dışa aktarma, araştırma panosu |
| FocusWriter | Ücretsiz, tam ekran, odak | Silme engeli + kurtarma + istatistik | Tam ekran özelleştirme, günlük hedefler |
| iA Writer / Typora | Tipografi, Markdown | Düz `.txt`, proje formatı yok | Pazarlama, imza, marka |
| Not defteri / gedit | Her yerde var | YOLO, ses, kurtarma, otomatik kayıt | Hiçbiri — ama zaten yüklü |

**Konumlandırma cümlesi (öneri):** *"İlk taslağını yazarken yanlışlıkla silemeyeceğin, hesap
istemeyen, tek düz metin dosyası."* Bu cümle bugünkü kodla **kısmen** doğru; "yanlışlıkla
silemeyeceğin" doğru, "dosyanı kaybetmeyeceğin" koşullu.

### 3.4 Tez ile gerçek arasındaki mesafe

Ürünün vaadi üç cümleye indirgenebilir; her biri bugün test edilebilir bir koşula bağlı:

| Vaat | Koşul | Bugün |
|---|---|---|
| "Yazdığın metin kaybolmaz" | Tek yazar, düz dosya, sağlam dosya sistemi, çökme | ✅ Çoğu yol kapalı; **dış değişiklik ve symlink açık** |
| "Yanlışlıkla silemezsin" | YOLO açık | ✅ `YoloPolicy` + `EditorArea.replaceText` testli |
| "Kaldığın yerden devam edersin" | Tek son dosya | ✅ Son dosya açılıyor; **son dosyalar listesi yok**, karşılama her açılışta |

### 3.5 P2'nin sorusu

P2'nin teması "daha çok özellik" değil, şu tek soru olmalı:

> **Bir romancı, JTypeWriter'ı açtığı gün bir bölümü baştan sona yazıp kaydedebilir mi — ve
> ertesi gün aynı dosyaya güvenle dönebilir mi?**

Bugünkü cevap **hayır**: cümle arayamaz, dosyayı OS'ten açamaz, yazdıramaz ve dışarıdan
değişen dosyayı fark etmez.

---

## 4. Mimari Değerlendirme

### 4.1 Ölçülen şekil

| Paket | Sınıf | Satır | Not |
|---|---:|---:|---|
| `service/` | 9 | 2.014 | Dosya, kurtarma, otomatik kayıt, diyalog |
| `ui/component/` | 10 | 1.697 | Editör, araç çubuğu, durum çubuğu |
| `ui/` | 13 | 1.414 | Denetleyici, sahne, diyaloglar |
| kök | 9 | 1.320 | `AppGraph`, ayarlar, log, kilit, atomik dosya |
| `locale/` | 3 | 557 | |
| `ui/theme/` | 5 | 384 | |
| `audio/` + `stats/` | 2 | 225 | |
| **Toplam** | **51** | **7.611** | |

En büyük sınıflar: `ToolbarView` 662, `Messages` 468, `FileService` 401, `RecoveryStore` 400,
`AppSettings` 394, `AutoSaveService` 352, `TextCodec` 287, `DialogService` 286.

### 4.2 Güçlü yanlar

- **Framework'süz kompozisyon kökü.** `AppGraph` alan sırası = bağımlılık sırası; dinleyiciler
  kurucuda bağlanıyor. Yeni bir servis eklemek tek dosyada okunabilir bir iş.
- **Saf mantık ayrımı gerçekten uygulanmış.** `YoloPolicy`, `StatsBarModel`, `ToolbarFit`,
  `ExitDisposition`, `AutosaveHealth`, `DialogSizing`, `ShortcutRows`, `TextCodec` JavaFX'siz;
  testler toolkit açmadan karar mantığını doğruluyor. Bu, projenin en değerli kültürel mirası.
- **Veri yolu tek ilkelden geçiyor.** Tüm yazmalar `AtomicFiles.writeMoving`; yedek
  `BackupKeeper.prepare/install`; kodlama `TextCodec`. "Yeni bir yazma yolu" icat etmek zor.
- **Test disiplini.** 350 test, 40 sınıf, `@TempDir`, root/POSIX varsayımları, TR/DE locale
  koşuları, süreç öldürme testi. Bu ölçekte bir masaüstü uygulaması için nadir.

### 4.3 Yapısal borç

| ID | Borç | Etki | Kanıt |
|---|---|---|---|
| **M1** | **Komut katmanı yok.** Her eylem `KeyboardShortcutHandler` + `ToolbarView` + `MainController` üçlüsüne ayrı ayrı bağlanıyor; menü, CLI, arama, palet için ortak bir "Action" soyutlaması yok. | Menü/CLI/find her biri yeni kablolama demek; keşfedilebilirlik borcu kalıcılaşır | `KeyboardShortcutHandler.java:46-74`, `ToolbarView.java:134-138` |
| **M2** | **Tek belge varsayımı kök hücrelerde.** `FileService` tek `currentFilePath/charset/bom`, `AppGraph` tek `EditorArea`, `MainWindow` tek sahne. | P4 (sekmeler) için `DocumentSession` refactor'u şart; P2'de yapılmamalı ama **P2 kararları bunu kilitlememeli** | `FileService.java:60-80`, `AppGraph.java:68` |
| **M3** | **Diyalog tekrarı.** 9 `Alert` çağrısı aynı kurulumu tekrarlıyor; sarma/kelepçe/yeniden boyutlandırma yok. | U1'in doğrudan nedeni; her yeni diyalog aynı hatayı çoğaltır | `DialogService.java:96-263` |
| **M4** | **`AppLogging` global statik.** Enjeksiyon seam'i yok; Surefire `user.home` vermiyor. | Testler gerçek `~/.jtypewriter/logs`'a yazıyor (T6); "asla gerçek eve dokunma" kuralı uygulanamaz | `AppLogging.java:37,153-164`; `pom.xml:145-149` |
| **M5** | **Dinleyici sırası örtük.** Dil değişiminde `Messages.reload → ToolbarView → başlık` sırası alan sırasına bağlı; test kırılmıyor. | Yeniden sıralama sessiz regresyon üretir | `AppGraph.java:47-86`, `MainController` |
| **M6** | **Yansıma ile undo temizliği.** `UndoHistoryReset` JavaFX iç alanlarına gidiyor; modül yolunda sessizce yetenek kaybı; dönüş değeri **kontrol edilmiyor**. | jlink/modül yolu engeli; belge sınırını aşan undo riski | `UndoHistoryReset.java:16-31,40-49`; `EditorArea.java:114` |
| **M7** | **Undo geçmişi sınırsız.** JavaFX varsayılanı; büyük belgede bellek büyümesi. | 10 MB+ belgede ölçülmedi; risk | `EditorArea.java:121-132` |
| **M8** | **Dispose sözleşmesi yok.** `StatsBar` timeline'ı, dinleyiciler kaldırılamıyor. | İkinci pencere/P4 için sızıntı | `StatsBar.java:105-113` |

### 4.4 Threading — mimarinin en zayıf ekseni

`src/main` içinde **tek bir** eşzamanlılık çağrısı var: `CrashHandler`'daki `Platform.runLater`.
`Task`, `Service`, `ExecutorService`, `Thread`, `CompletableFuture` **hiç yok**.

Bunun sonuçları:

- Açma: `Files.readAllBytes` + kod çözme + `setText` FX thread'inde (`FileService.loadFile`).
- Kaydetme: encode + temp yazma + `fsync` + rename FX thread'inde (`FileService.writeFile`).
- Otomatik kayıt: 30 s'de bir `editorArea.getText()` (tam kopya) + UTF-8 serileştirme + atomik
  yazma + `fsync` FX thread'inde (`AutoSaveService.snapshot`).
- Oturum geri yükleme: pencere gösterildikten **sonra**, senkron (`FxApplication.java:59-67`).

1 MB'lık bir roman taslağında bu, her kayıtta hissedilir bir duraklama; 10 MB'da donma.
Ürünün "kesintisiz yazma" vaadiyle doğrudan çelişen tek mimari eksen budur.

### 4.5 Genişleme noktaları ve eksik komut katmanı

Bugün yeni bir özellik eklemenin maliyeti:

| Özellik | Gereken dosyalar | Neden pahalı |
|---|---|---|
| Ara/Değiştir | Yeni saf model + yeni UI paneli + `EditorArea` entegrasyonu + kısayol + 6 bundle | Ortak komut/eylem katmanı yok; YOLO ile etkileşim ayrıca tasarlanmalı |
| Menü çubuğu | `ToolbarView`'daki her eylemin yeniden bağlanması + hızlandırıcı tablosu | Eylemler düğümlere gömülü, komut nesnesi yok |
| CLI ile açma | `FxApplication` + `FileService` + tek örnek kilidi ile el sıkışma | İkinci örnek yolu belgeyi **devredemiyor**; sadece reddediyor |
| Yazdırma | Yeni servis + sayfa düzeni + yazıcı diyaloğu | Metin modeli düz `TextArea`; sayfalama yok |

**Mimari sonuç:** P2'ye girmeden önce **küçük bir komut/eylem katmanı** (M1) kurulursa menü,
CLI, arama ve gelecekteki palet aynı yerden beslenir. Kurulmazsa her özellik kendi
kablolamasını getirir ve keşfedilebilirlik borcu kalıcılaşır.

---

## 5. Veri Güvenliği — P1 Sonrası Durum

### 5.1 P1 ile kapanan yollar

| Yol | Durum | Kanıt |
|---|---|---|
| Kilit "başka örnek var" ile "kilit kullanılamıyor"u ayırmıyordu | **Kapalı** — üç durumlu; yalnız `ALREADY_RUNNING` engeller | `InstanceLock.java:92-111`, `JTypeWriterApp.java:25-30` |
| Kapatma isteği dışı her çıkış taslağı siliyordu | **Kapalı** — `CLEAR/PARK/KEEP` matrisi; yalnız yanıtlanmış kapatma siler | `ExitDisposition.java:55-60`, `AutoSaveService.java:271-282` |
| Kurtarma sonrası legacy kodlama UTF-8'e düşüyordu | **Kapalı** — decode `null` ise Windows-1254 korunuyor | `FileService.restoreRecovered` |
| Yedek hatası kaydı iptal ediyordu | **Kapalı** — önce yaz, sonra yedek kur; hata yalnız uyarı | `FileService.java:300-327`, `BackupKeeper.java:49-79` |
| Başarılı kayıt dinleyici hatasıyla "başarısız" görünüyordu | **Kapalı** — dinleyiciler try dışında, tek tek izole | `FileService.java:342-349` |
| `NaN`/`Infinity` pencere geometrisi | **Kapalı** — `Double.isFinite` + pozitiflik | `AppSettings.java:339-350` |
| `AppLogging.init()` çökme güvensizdi | **Kapalı** — `initialized` en sonda, `RuntimeException` yakalanıyor | `AppLogging.java:60-125` |
| Bozuk ayar dosyası üzerine yazılıyordu | **Kapalı** — `.corrupt` karantinası | `AppSettings.java:236-270` |

### 5.2 Kalan açıklar

#### D1 — Dış değişiklik algılama yok — **Yüksek**

`loadFile` yalnız içerik/kodlama/yol saklıyor; `(mtime, size)` veya içerik hash'i
**saklanmıyor** (`FileService.loadFile`). `writeFile` hiçbir karşılaştırma yapmadan yazıyor
(`FileService.java:275-351`). `getLastModifiedTime` yalnız taslak karşılaştırmasında geçiyor.

Sonuç: `git checkout`, Dropbox/Syncthing, başka bir editör veya ikinci bir örnek dosyayı
değiştirdiyse kullanıcının **başkasının metni** sessizce kaybolur. Tek telafi 3 nesillik
`.bak`; o da D10'a açık.

Bu, ürünün "hiçbir şey kaybolmaz" tezinin en doğrudan çürütücüsü. Aynı zamanda P2'nin en
yüksek getirili tek işi (§12.2/A1).

#### D2 — Symlink'li belge kaydedilince link yok oluyor — **Yüksek**

`AtomicFiles.writeMoving` yolu mutlaklaştırıyor ama `toRealPath()` kullanmıyor; temp dosya
linkin yanında oluşturulup `Files.move(..., REPLACE_EXISTING)` ile **linkin üzerine** taşınıyor
(`AtomicFiles.java:54-74`). `~/notlar.txt -> /mnt/data/notlar.txt` durumunda gerçek hedef hiç
güncellenmez; `.bak` da yanlış dizine yazılır.

#### D3 — Taslak başlığındaki yol doğrulanmıyor — **Yüksek**

`RecoveryStore` `original.path.b64` değerini Base64'ten çözüp doğrudan `Path.of` veriyor
(`RecoveryStore.java:339`); mutlaklık, varlık, kanoniklik veya izin verilen kök kontrolü yok.
Değer `FileService.restoreRecovered` üzerinden `currentFilePath` oluyor ve **sonraki Ctrl+S'in
hedefi** haline geliyor. Kurtarma dizini kullanıcı tarafından yazılabilir; yedekten veya başka
makineden gelen bir taslak, kaydetmeyi keyfi bir dosyaya yönlendirebilir.

#### D4 — Taslak taraması symlink takip ediyor ve sınırsız — **Orta**

`Files.list(dir).filter(Files::isRegularFile)` symlink'leri takip ediyor
(`RecoveryStore.java:131-139`); her aday, en yenisi seçilmeden **tamamen** `readAllBytes` ile
okunuyor (`:271`). Boyut/sayı sınırı yok → açılışta donma veya `OutOfMemoryError`; ayrıca
açılış başına yalnızca **en yeni** taslak sunuluyor, N ertelenmiş taslak N yeniden başlatma
ister.

#### D5 — Park edilen taslaklar hiç toplanmıyor — **Orta**

`pending-*.jtw` dosyalarını yaş/sayı/boyut sınırıyla silen hiçbir yol yok
(`RecoveryStore`). Disk sınırsız büyür; kullanıcının **diğer belgelerinin metinleri** profilde
süresiz kalır (gizlilik sorunu).

#### D6 — Atomik olmayan fallback doğrulanmıyor; temp süpürme yok — **Orta**

`ATOMIC_MOVE` desteklenmediğinde `Files.move(REPLACE_EXISTING)`'e düşülüyor ve bu yolda
boyut/hash doğrulaması yok (`AtomicFiles.java:67-69`). SMB/NFS'te süreç ölürse hedef yarım
yazılmış olabilir. `SIGKILL` sonrası `jtw-*.tmp` hiç süpürülmüyor; `finally` içindeki
`deleteIfExists` kendisi hata atarsa **asıl istisnayı maskeliyor**.

#### D7 — Disk alanı kontrolü yok; `OutOfMemoryError` kaçıyor — **Orta**

Boş alan ön kontrolü yok. `readAllBytes` yükleme ve kurtarmada kullanılıyor; kaydetme yolu
`IOException | RuntimeException` yakalıyor — bir `Error` **kaydetme hata yolunu atlar**.
Tek kaydetmenin disk maliyeti hedef + temp + 3 yedek ile belgenin ~5 katına çıkabilir.

#### D8 — Belge düzeyinde kilit yok — **Orta**

`InstanceLock` kullanıcı başına tek kilit (`InstanceLock.java:92-112`); belge üzerinde
`FileLock` yok. Farklı `user.home` ile çalışan ikinci bir örnek veya kilit `UNAVAILABLE`
durumundaki ikinci süreç aynı dosyayı yazabilir. D1 ile birlikte gerçek yarış senaryosu.

#### D9 — "Vazgeç" yolunda taslak silme hatası yutuluyor — **Düşük**

`offerRecovery` içinde `RecoveryChoice.DISCARD` dalı `store.clear()` dönüşünü yok sayıyor
(`AutoSaveService.java:201-203`); diğer temizleme yolları `DRAFT_DELETE` uyarısı üretirken bu
yol yalnız log bırakıyor.

#### D10 — Yedek kurulumu en yeni nesli kaybedebilir — **Düşük**

`BackupKeeper.install` önce `.bak.1→.bak.2`, `.bak→.bak.1` döndürüp sonra yeni kopyayı terfi
ettiriyor; son taşıma başarısız olursa geçmiş kaymış, yeni nesil yok (`BackupKeeper.java:69-79`).
`FileService` staged dosyayı siliyor (`:325`).

#### D11 — `user.home` yoksa açılış NPE ile ölür — **Düşük olasılık / yüksek etki**

`AppDirs.dataDir()` `Path.of(System.getProperty("user.home"), ...)` (`AppDirs.java:20`);
`user.home` tanımsızsa `JTypeWriterApp` satır 25'te, crash handler kurulmadan NPE alınır.
Sandbox/konteyner profillerinde gerçekleşebilir.

#### D12 — Ayar yazma hatası sessiz; `SecurityException` kaçıyor — **Orta**

`AppSettings.save()` yalnız `IOException` yakalayıp WARNING'e indiriyor
(`AppSettings.java:300-307`); `SecurityException` çağırana kaçar ve in-memory değer
değişmişken UI eylemi kesilebilir. Kalıcı yazılamayan ayar dosyası arayüzde görünmüyor —
her tercih her açılışta kaybolur.

#### D13 — Ayar şeması sürümü yok — **Orta (gelecek)**

Bilinmeyen değer varsayılana düşüyor ama anahtar yeniden adlandırma/göç yok. P2'de eklenecek
her yeni ayar (otomatik kayıt aralığı, son dosyalar, yazı tipi ailesi) bu borcu büyütür.

### 5.3 Kullanıcıya görünen kayıp/bozulma tablosu

| # | Senaryo | Önem |
|---|---|---|
| D1 | Senkron/git/başka editör değişikliği uyarısız ezilir | Yüksek |
| D2 | Symlink'li belge kaydedilir; gerçek hedef sonsuza kadar eski kalır | Yüksek |
| D3 | Taslak başlığı kaydetmeyi keyfi bir dosyaya yönlendirir | Yüksek |
| D4 | Çok sayıda/büyük taslak açılışı kilitler veya OOM | Orta |
| D5 | Diğer belgelerin taslak metinleri profilde süresiz kalır | Orta |
| D6 | SMB/NFS'te yarım hedef; `jtw-*.tmp` artıkları | Orta |
| D7 | Disk dolu → yedek döndürmesi en eski nesli kaybeder; OOM kaydı atlar | Orta |
| D8 | İki süreç aynı belgeyi yazar; bir yazarın metni hiçbir yerde kalmaz | Orta |
| D9 | "Vazgeç" sonrası silinemeyen taslak sessizce kalır | Düşük |
| D10 | Yedek terfisi başarısız → en yeni yedek nesli yok | Düşük |
| D11 | `user.home` yoksa uygulama açılmaz | Düşük |
| D12 | Yazılamayan ayar dosyası → tercihler her açılışta kaybolur | Orta |

### 5.4 Bu bölümün test boşlukları

Hiçbir test şunları kapsamıyor: dış değişiklik, symlink hedefi, hard link, taslak başlığında
yol doğrulama, `pending-*.jtw` toplama, disk dolu, `OutOfMemoryError`, iki süreç aynı dosya,
`ATOMIC_MOVE` desteklemeyen dosya sistemi, `DISCARD` sonrası başarısız silme, `install`
sırasında yedek terfi hatası.

### 5.5 Vaat koşulları

Özetle güvenlik hikâyesi şu koşullarda **doğru**:

1. Belgeyi yazan tek süreç var (D1, D8 kapalı değil).
2. Belge symlink değil (D2).
3. Kurtarma dizini güvenilir (D3).
4. Dosya sistemi atomik rename destekliyor (D6).
5. Diskte yer var ve belge belleğe sığıyor (D7).

Bir yazma aracı için bu koşulların **hiçbiri** kullanıcıya söylenmiyor. P2'nin "güven"
işi, bu listenin ilk üç maddesini kapatmak olmalı.

---

## 6. Editör Yetkinliği

### 6.1 Var olanlar

Tek düz metin belgesi; atomik kaydetme + dönen yedek; kodlama koruması (BOM, UTF-16 parite,
Windows-1254 sorusu); YOLO modu; kelime/karakter/satır/tuş/süre/WPM; 3 tema; 6 metin rengi;
10–48 px yazı boyutu merdiveni; 6 dil; tam ekran; araç çubuğunu gizleme; 30 s otomatik kayıt +
çökme kurtarma; son dosya ve pencere geometrisi geri yükleme; daktilo sesi.

Bu liste, **tek belgeli bir yazma aracı** için sağlam bir çekirdek. Eksik olan çekirdek değil,
**günlük döngü**.

### 6.2 Günlük döngü — dokuz adım

| # | Romancının adımı | Bugün | Kanıt |
|---|---|---|---|
| 1 | Belgeyi OS'ten aç (çift tık / "birlikte aç") | **Yok** | CLI argümanı okunmuyor (E3), MIME/ilişkilendirme yok (E10) |
| 2 | Yaz | **Var** | `EditorArea`, YOLO |
| 3 | Bir cümleyi bul | **Yok** | Arama yok (E1) |
| 4 | Bulduğunu değiştir | **Yok** | Değiştirme yok (E1) |
| 5 | Sahneyi kes/taşı | **Var** (YOLO kapalıyken) | JavaFX varsayılanları |
| 6 | Kaydet | **Var**, ama FX thread'ini bloke eder | X1 |
| 7 | Yazdır / PDF al | **Yok** | E7 |
| 8 | Ertesi gün devam et | **Kısmi** — son dosya açılır; son dosyalar listesi yok, karşılama her açılışta | E5, U7 |
| 9 | Sorun bildir | **Kısmi** — mail adresi + log yolu; tanılama paketi yok | B9 |

Dokuz adımın **dördü yok**, biri kısmi, biri performans riskli. P2'nin gerekçesi bu tablodur.

### 6.3 Olmayanlar

| ID | Eksik | Etki | Not |
|---|---|---|---|
| **E1** | Ara / Değiştir (regex dahil değil) | **5** | En çok istenen; `Ctrl+F` yok, model yok, 6 dilde anahtar yok |
| **E2** | Menü çubuğu | **4** | Tüm komutlar araç çubuğunda; **Yeni** ve **Farklı Kaydet** için görünür kontrol yok |
| **E3** | CLI ile dosya açma | **4** | `Application.getParameters()` hiç çağrılmıyor; ikinci örnek yolu devretmiyor |
| **E4** | Sürükle-bırak | **2** | `Dragboard` yok |
| **E5** | Son dosyalar | **3** | Yalnız `last.file` |
| **E6** | Tercihler penceresi | **2** | Otomatik kayıt aralığı, yedek nesli, yazı tipi ailesi ayarlanamıyor |
| **E7** | Yazdırma / PDF | **3** | `PrinterJob` yok |
| **E8** | Satır numaraları | **3** | Düz `TextArea`; uzun taslakta yön kaybı |
| **E9** | Satır sarma anahtarı, yazı tipi ailesi, satır sonu, sekme politikası | **2** | Sarma zorunlu açık (`EditorArea.java:48`) |
| **E10** | Dosya ilişkilendirmesi / MIME / `.desktop` | **4** | Paketleme ile birlikte; OS entegrasyonunun önkoşulu |

**Şiddetli olan üçlü:** E1, E2, E3. Bunlar "özellik" değil, bir metin editörünün **asgari
tabanı**. E1 olmadan uzun metin, E2 olmadan keşfedilebilirlik, E3 olmadan masaüstü varlığı yok.

### 6.4 Keşfedilebilirlik

- Araç çubuğunda **5 düğme**: Aç, Kaydet, Geri Al, Yinele, Hakkında
  (`ToolbarView.java:134-138`). **Yeni** ve **Farklı Kaydet** yalnız klavyede.
- 16 kısayol var (README tablosuyla birebir), hepsi `KeyboardShortcutHandler`'da; karşılama kartı bunların bir kısmını
  listeliyor. Menü olmadığı için **hiçbir komut hiyerarşisi** yok.
- Araç çubuğu 900 px altında başlıkları değere indiriyor (`ToolbarFit`); pencere minimum
  genişliği olmadığı için daha dar ekranda kırpılıyor (U8).
- Sağ tık menüsü yalnız YOLO bağlamında var (`EditorArea.java:134-148`).

---

## 7. Performans ve Ölçek

### 7.1 Ölçümler

| Ölçüt | Değer | Koşul |
|---|---|---|
| İlk sahne | 565–613 ms | 5 koşu, sıcak önbellek |
| Ana pencere | 1.586–1.657 ms | aynı |
| Karşılama diyaloğu | ~1.691 ms | bir koşu |
| RSS | ~280 MB | açılış sonrası |
| İmaj | 145,6 MiB | Linux x64 |
| Runtime payı | 139,97 MB (**%91,7**) | 52 modül |
| Fat JAR | 11,2 MB | 6.105 giriş |

Her açılışta stderr'e şu uyarı düşüyor: `Unsupported JavaFX configuration: classes were
loaded from 'unnamed module ...'`. Bu, classpath ile başlatmanın **bilinçli** sonucu
(`JTypeWriterApp` javadoc'u); ama kullanıcıya/terminale sızan bir "polish" sinyali.

### 7.2 Tuş başına maliyet

- `EditorKeyHandler` tuş başına yalnız sayaç artırıyor; `SessionStats.updateText` **O(1)**
  referans ataması (`SessionStats.java:65-67`).
- Kirli bayrak O(1); yalnız **aynı uzunluktaki** düzenlemelerde tam `String.equals`
  (`FileService.java:106`).
- `MainController` tuş başına başlık dizesi üretiyor ve kaydetme etiketini siliyor
  (`MainController.java:216-222`). Küçük ama gereksiz alokasyon.

**Sonuç:** tuş gecikmesi bugün iyi. Asıl risk tuş yolunda değil, **1 Hz istatistik** ve
**30 s otomatik kayıt** yolunda.

### 7.3 1 Hz istatistik

`StatsBar` saniyede bir `StatsBarModel.refresh` çağırıyor (`StatsBar.java:105-113`); bu,
`SessionStats` üzerinden:

- `trim().split("\\s+")` — kelime dizisi tahsisi (tam metin),
- üç ayrı `replace(...)` — karakter sayısı için üç tam kopya,
- `split("\n", -1)` — satır dizisi tahsisi.

10 MB'lık bir belgede bu, saniyede onlarca MB geçici çöp demek. **Ölçülmedi**, ama karmaşıklık
O(n) ve saniyede bir tekrar ediyor. Kabul edilebilir sınır, belge boyutuna göre tanımlanmalı.

### 7.4 Otomatik kayıt

`attach` içinde `new KeyFrame(..., e -> snapshot(editorArea.getText()))`
(`AutoSaveService.java:136`). Argüman, `snapshot` içindeki kirli kontrolünden **önce**
değerlendirildiği için **temiz belgede bile tam metin kopyası** alınıyor; odak kaybında da
aynı (`:140-144`). Sonra `store.saveDraft` UTF-8'e serileştirip atomik yazıyor
(`RecoveryStore.java:112-116,387-399`) ve `AtomicFiles` `fsync` yapıyor — hepsi FX thread'inde.

Yani 30 s'de bir: tam kopya + tam serileştirme + disk senkronizasyonu. 1 MB'da hissedilir,
10 MB'da donma.

### 7.5 G/Ç ve bellek çarpanı

- Yükleme: `byte[]` → `String` → `TextArea` metni (en az 3 kopya).
- Kaydetme: `String` → `CharBuffer` → `byte[]` → temp dosya → rename.
- Otomatik kayıt: ek olarak `String` kopyası + `byte[]`.
- Kurtarma taraması: her aday için tam `readAllBytes` (`RecoveryStore.java:271`).

Belge boyutunun 3–5 katı bellek ve disk geçici kullanımı mümkün. Boyut sınırı, uyarı veya
ilerleme göstergesi yok.

### 7.6 Ölçülmemiş senaryolar

| Senaryo | Durum |
|---|---|
| 1 MB belge: açma/kaydetme süresi | Ölçülmedi |
| 10 MB belge: tuş gecikmesi, kaydetme donması | Ölçülmedi |
| 50 MB belge: `readAllBytes`, OOM, undo belleği | Ölçülmedi |
| 1.000 taslak dosyası olan kurtarma dizini | Ölçülmedi |

P2'de performans iddiası kurulacaksa **önce bu dört ölçüm** alınmalı; bugünkü tek veri,
mimarinin senkron olduğu ve karmaşıklığın O(n) olduğu.

---

## 8. UX, Erişilebilirlik ve i18n

### 8.1 Kontrast kapsamı

`ContrastRatioTest` gerçek CSS dosyalarını ayrıştırıp şunları zorluyor (eşikler 4,5:1 metin,
3:1 bileşen):

- `.stats-label`/`.stats-clock`/`.stats-save`/`.stats-warning`,
- odak halkası ve odaklı popup satırı,
- karşılama CTA (dinlenme + hover), About e-postası, tagline,
- 5 vurgu rengi × 3 tema,
- yazı boyutu ≥ 13 px, popup hücresi = kapalı combo, geçersiz CSS özelliği yok.

**Ölçülmeyenler:** `-toolbar-text` (düğme ve combo metni), düz popup satırları, seçili popup
satırı, `.about-email:hover`, varsayılan editör metni (`TextColor.DEFAULT` bilinçli atlanıyor).
Ayrıca `-toolbar-text-muted` tanımlı ama **hiçbir kuralda kullanılmıyor** ve açık/sepya
temalarında AA altıdır; ölü token olduğu için test de görmüyor.

### 8.2 Diyaloglar

- Tema aktarımı merkezî ve doğru: `DialogStyling.applyFromScene` her `Alert`'a uygulanıyor.
- Boyut kelepçesi **yalnız özel sahnelere** uygulanıyor (`WelcomeDialog`, `AboutDialog`:
  `DialogSizing`, kaydırma, yeniden boyutlandırma).
- **9 `Alert`** (`DialogService` ×7, `CrashHandler`, `SingleInstanceWarning`) için
  `setResizable`, `setWrapText` veya yükseklik kelepçesi **yok**. Almanca
  `encodingUpgradeContent` gibi uzun metinler 768 px ekranı aşabilir — **U1**.
- İki başlıkta `" - "` ayırıcısı kodda birleştiriliyor (`DialogService.java:189,204`).
- Çökme diyaloğu ham `Throwable.toString()` basıyor (`CrashHandler.java:71`) → İngilizce JVM
  metni UI diline karışıyor — **U11**.

### 8.3 Klavye ve erişilebilirlik

- Erişilebilir ad: yalnız 6 combo için `setAccessibleText` + 1 `COMBO_BOX` rolü
  (`ToolbarView.java:399-404,539`). Editör, durum çubuğu, düğmeler, karşılama/Hakkında
  düğmeleri **adsız** — **U6**.
- Durum çubuğu yazı boyutu **yalnız fare çift tıklamasıyla** değişiyor
  (`StatsBar.java:98-103`) — **U2**. Klavyeyle büyütme yok.
- Odak aktarımı saf politika olarak var ve testli (`MainController.handOffFocusToEditor`),
  ama gerçek Tab sırası/traversal doğrulanmıyor.
- `viewOrder` ile görsel sıra sabitlenmiş; JavaFX bunu odak için **garanti etmiyor**
  (README bunu dürüstçe yazıyor).
- Mnemonic yok; tüm erişim Tab ve kısayollara bağlı.

### 8.4 Platform kısaltma gösterimi

`KeyboardShortcutHandler` macOS'ta `isShortcutDown()` (Cmd) **ve** Ctrl kabul ediyor
(`:76-83`), ama araç çubuğu tooltip'leri sabit `"Ctrl+O"…` (`ToolbarView.java:84-91`), karşılama
kartı da 6 dilde `Ctrl+...` yazıyor. macOS kullanıcısına yanlış tuş söyleniyor — **U4**.

### 8.5 Yerelleştirme borcu

| Sorun | Kanıt | Etki |
|---|---|---|
| Sabit `dd.MM.yyyy` ve `HH:mm:ss` | `StatsBarModel.java:51-52` | FR/EN kullanıcısı kendi biçimini görmez — **U5** |
| Sabit `HH:mm` kaydetme etiketi | `MainController.java:33` | aynı |
| `": "` ve `" - "` ayırıcıları kodda | `ToolbarFit.java:93`, `MainController.java:184` | düşük |
| ES/IT bundle'larında formal/informal karışıklığı | `messages_es`, `messages_it` | düşük |
| Çökme başlıkları İngilizce | `CrashHandler.java:43`, `FxApplication.java:77` | düşük-orta |
| `MessageFormat` apostrofları kaçırılmış | 6 bundle | tipografik `’` hiç görünmüyor; kabul edilebilir |

**İyi olan:** anahtar paritesi iki yönlü testli; sayı biçimi UI dilini kullanıyor; TR/DE locale
koşuları CI'da; ölü anahtar yok; argümanlı anahtarlar 6 dilde biçimleniyor.

### 8.6 İlk açılış ve pencere

- Karşılama kartı **her açılışta** gösteriliyor (`FxApplication.java:70`); "bir daha gösterme"
  ayarı yok — **U7**. İlk kullanıcı için iyi, günlük kullanıcı için sürtünme.
- Pencere **minimum genişlik yok**; 900 px altında araç çubuğu kırpılabiliyor — **U8**.
  README bunu açıkça yazıyor.
- Geometri geri yükleme ekran dışı konumlara karşı korumalı ve testli (`MainStage`).
- Tam ekran geri yükleme, `Escape` ile çıkış, `F11` — çalışıyor.
- İlk odak: modal karşılama kapandıktan sonra editöre veriliyor; kartın **içindeki** ilk odak
  doğrulanmadı (toolkit testi yok).

### 8.7 Ses

- Her `KEY_PRESSED` için, yalnız modifier'lar hariç (`EditorKeyHandler.java:31-35`). Yani ok
  tuşları, Home/End, F tuşları, Caps Lock ve **Ctrl+S gibi kısayol akorları** da ses çıkarıyor.
  İstatistik sayacı ise `KEY_TYPED` ile ayrı sayıyor → ses ile sayaç çelişebilir — **U9**.
- Ses seviyesi sabit `0.5`, UI'da yalnız aç/kapat var (`TypewriterSound.java:45-46`).
- Hızlı yazımda `stop()+play()` her tuşta klibi yeniden başlatıyor → takılma hissi.
- Ses hatası sessizce WARN'a düşüyor; kullanıcı sese ne olduğunu bilmiyor.

### 8.8 macOS

Kodda `QuitHandler`, `SystemMenuBar`, AWT `Desktop` (mail hariç) veya Dock entegrasyonu yok.
Kapatma yalnız `stage.setOnCloseRequest` + `Application.stop()` üzerinden. **macOS `Cmd+Q`
davranışı kodla garanti edilmiyor**; test de yok. Bu, P2'de doğrulanması gereken bir
belirsizliktir (U10) — ve P1'in "hiçbir çıkış taslağı silmez" garantisinin platform ayağıdır.

---

## 9. Paketleme, Dağıtım ve Destek

### 9.1 Artefakt envanteri (yerel, Linux x64)

| Artefakt | Boyut | Not |
|---|---:|---|
| `JTypeWriter/` app image | **145,6 MiB** | 125 dosya |
| ├ `lib/runtime` | 139,97 MB | **%91,7**; 52 modül |
| └ `lib/app` | 11,20 MB | tek fat JAR |
| `jtypewriter_1.0.0_amd64.deb` | **47,3 MB** | 13 runtime bağımlılığı |
| `JTypeWriter-1.0.0-linux-x64.tar.gz` | **59,3 MB** | imajın %40,7'si |
| Kaynak arşivi | 0,69 MB | `git archive` |

İmajda `bin/java` yok (jpackage başlatıcısı), bu iyi. Ama `modules` dosyası tek başına
90,6 MB; `jdk.compiler`, `jdk.javadoc`, `jdk.jshell`, `jdk.jfr`, `jdk.jdwp.agent` gibi
uygulamanın kullanmadığı modüller taşınıyor. **jlink yok.**

### 9.2 Runtime ağırlığı ve ölü ağırlık

- JavaFX Media: 0,43 MB sınıf + `libgstreamer-lite.so` 2,31 MB + 11 `libavplugin*.so`;
  uygulamada yalnızca 71 KB'lık `keystroke.wav` için kullanılıyor.
- JavaFX FXML: 0,12 MB; `fxml/main.fxml` yalnızca boş bir `BorderPane`.
- İkisinin çıkarılması fat JAR'ı küçültür; jpackage imajında büyük kazanç **jlink** ile gelir.

### 9.3 CI kapıları ve eksik smoke test

| Kapı | Var? | Not |
|---|---|---|
| Enforcer (JDK 25, Maven 3.8) | ✅ | `validate` aşamasında |
| 350 test × 3 OS | ✅ | |
| TR/DE locale koşusu | ✅ | 3 kez suite |
| Lisans/üçüncü taraf bildirimleri | ✅ | `PackagingNoticesTest` + `package.sh` |
| Sürüm/tag eşleşmesi | ✅ | `release.yml` `prepare` işi |
| **Artefaktı çalıştırma** | ❌ | İmaj açılmıyor, deb kurulmuyor |
| Kod imzalama / notarization | ❌ | |
| Kapsam ölçümü (JaCoCo) | ❌ | |
| Statik analiz / SBOM / zafiyet taraması | ❌ | |
| Maven bağımlılık güncellemesi (Dependabot) | ❌ | Yalnız `github-actions` |

### 9.4 Sürüm, imza, güncelleme

- `pom.xml` tek sürüm kaynağı; tag `v<version>` olmalı; `package.sh` jpackage kurallarına
  uymayan sürümü reddediyor. Doğru tasarım.
- **Hiç tag atılmamış, hiç Release yok.** Yani release hattı hiç uçtan uca çalışmadı; ilk
  sürümde sürpriz çıkma olasılığı yüksek.
- İmza/notarization yok; release notu bunu dürüstçe yazıyor. Windows SmartScreen ve macOS
  Gatekeeper ilk açılışta uyaracak.
- Güncelleme denetimi yok (gizlilik tercihiyle tutarlı); kullanıcı yeni sürümü kendisi bulmalı.
- Yeniden üretilebilirlik: `outputTimestamp` var; ama jpackage dosya mtime'larını
  normalleştirmiyor, çift build karşılaştırması yok → **doğrulanmadı**.

### 9.5 Platform veri yolu ve masaüstü entegrasyonu

- `AppDirs` tüm OS'lerde `~/.jtypewriter` kullanıyor (`AppDirs.java:20`); XDG ve macOS
  `Application Support` değil — **B7**. Kanal kararı verilmeden taşınması riskli, ama P3'te
  kaçınılmaz.
- Yerel `.deb`'de `.desktop` dosyası `Categories=Unknown`, `MimeType=` boş, ikon mutlak yol;
  hicolor ikon seti, MIME XML, dosya ilişkilendirmesi yok — **B8**. (Bayat artefaktta ölçüldü,
  taze build'de doğrulanmalı.)
- App image'da `.desktop` yok; MSI menü/kısayol seçicisi var; DMG yalnız uygulama paketi.

### 9.6 Destek yüzeyi

| Beklenen | Durum |
|---|---|
| `SECURITY.md` | ❌ |
| Issue / PR şablonları | ❌ |
| `CHANGELOG.md` | ❌ |
| Kurulum/kaldırma/sorun giderme belgesi | ❌ (yalnız README) |
| "Log klasörünü aç" | ❌ (yalnız mailto) |
| Tanılama paketi (log + ayar + sürüm) | ❌ |
| Çökme raporu (yerel) | ✅ log + diyalog |
| Uygulama içi sürüm | ✅ About |

Bir kullanıcı hata bildireceğinde elinde yalnızca bir e-posta adresi ve bir dizin yolu var.
Bu, ilk 100 kullanıcıda destek maliyetini doğrudan artırır.

### 9.7 Tedarik zinciri

- Action'lar commit SHA'sına pinli, `permissions: contents: read`, `persist-credentials: false`
  — olgun.
- Dependabot yalnız action'ları güncelliyor; **Maven bağımlılıkları için giriş yok**.
- SBOM, imza, provenance attestation yok.

### 9.8 Yerel artefaktların bayatlığı (hijyen uyarısı)

`releases/` ve `target/` git'te izlenmiyor; yerel imaj `HEAD`'den eski. İmajdaki `NOTICE`
713 B (repo: 1.338 B), `.deb` içindeki jar farklı boyutta ve 80 sınıf içeriyor (güncel: 91),
`audio/PROVENANCE.md` jar'da yok. Bu, **kod hatası değil**: `mvn clean package &&
./scripts/package.sh --installer` ile yeniden üretilip doğrulanmalı. Aksi halde bu belgedeki
boyutlar ve lisans payload'ı iddiaları gösterge olarak kalır.

### 9.9 Lisans ve ticari model

- Lisans **Apache-2.0** (GPLv3'ten geçilmiş); patent grant ve değişiklik özgürlüğü var,
  copyleft yok. Üçüncü taraf bileşenler ayrı lisanslarla taşınıyor (JavaFX GPLv2+CE,
  GStreamer Lite LGPL-2.1-or-later) ve bu, dağıtımda doğru şekilde beyan edilmiş.
- **Ticari model tanımlı değil:** fiyat, web sitesi, EULA, ticari destek, bağış bağlantısı
  veya marka politikası repoda yok. Apache-2.0, üçüncü bir tarafın ürünü markasız kopyalayıp
  satmasını engellemez; marka tescili/politikası olmadan bu bir iş riskidir.
- Katkı modeli de tanımsız: `CONTRIBUTING.md`, CLA/DCO, kod sahipliği yok. Tek geliştirici
  varsayımı hem yol haritasının hem destek yükünün gerçek sınırı.

P2 bunu çözmek zorunda değil; ama §12.5'teki "ilk kullanıcı" kararı verilirken lisans/ticari
modelin **açıkça ertelendiği** bilinmeli — aksi halde dağıtım ve imza yatırımı geri dönüşsüz
bir varsayıma dayanır.

---

## 10. Test ve Kalite Güvencesi

### 10.1 Ölçülen şekil

- **40 test sınıfı, 350 test**, 3 OS × 3 locale.
- "Toolkit açma yok" kuralı gerçekten uygulanmış: hiçbir test `Platform.startup`,
  `Application.launch` veya Monocle kullanmıyor.
- JavaFX'e dokunan testler yalnız veri tipleri (`KeyCode`, `Rectangle2D`, `ButtonType`) veya
  bayt düzeyi (`AppIconsTest`).
- Toolkit'siz ölçüm için AWT metin ölçümü (`ToolbarFit`), saf geometri (`DialogSizing`).

### 10.2 Gerçekten iyi kapsanan alanlar

Kodlama matrisi (11 test, bayt-özdeş), kurtarma yaşam döngüsü, çıkış matrisi, otomatik kayıt
sağlığı, yedek rotasyonu, atomik yazma, ayar karantinası, i18n paritesi ve biçimlendirme,
kontrast matematiği, araç çubuğu taşma politikası, kısayol çözümleme, süreç öldürme.

### 10.3 Toolkit duvarı

Şunlar **hiç test edilmiyor** ve edilemez (mevcut mimaride):

- gerçek diyalog yerleşimi/taşması (U1),
- odak ve Tab sırası,
- render edilmiş CSS (kontrast yalnız hex matematiği),
- ses politikası (hangi tuş ses çıkarır),
- ilk karenin görünümü, karşılama kartının iç odağı,
- gerçek menü/araç çubuğu etkileşimi.

Bu duvar, "her UI kararını saf sınıfa çıkar" kültürünün bedeli. P2'de ara/değiştir gibi
özellikler bu yüzden **saf model + ince UI** olarak tasarlanmalı; aksi halde ya testsiz kalır
ya da toolkit testi için kural delinir.

### 10.4 Zayıf veya yanlış güven veren testler

| Test | Sorun |
|---|---|
| `ProcessCrashRecoveryTest` | Yalnız Linux; GUI değil servis grafiği fork ediliyor. Doğru kapsam, ama Windows/macOS çökme yolu doğrulanmıyor. |
| `DialogSizingTest` | Yalnız özel diyalogların kelepçe matematiği; `Alert`'lar kapsam dışı. |
| `AppSettingsTest` | `SecurityException` ve gerçek `save()` hatası yok. |
| `ToolbarFitTest` | Kabul testleri ölçülen etiketlerle; gerçek render yok. |
| `ContrastRatioTest` | Token çiftleri doğru, ama kapsam §8.1'deki seçicilerle sınırlı. |

### 10.5 Altyapı boşlukları

- Kapsam ölçümü yok → "350 test" hacim verir, **hangi yolların** koştuğunu vermez.
- Statik analiz/format yok (bilinçli); lisans ve sözleşme kuralları testle korunuyor.
- Artefakt düzeyi test yok (B1).
- Testler gerçek `~/.jtypewriter`'a yazıyor (M4/T6) → kural ihlali, temiz CI'da görünmez.

---

## 11. Olgunluk Skorları

| Boyut | Skor | Gerekçe | Önceki |
|---|---:|---|---:|
| **Motor** | **3,6** | Veri yolu tek ilkelden geçiyor, kodlama/çıkış/kurtarma gerçekten testli. Ama güvenlik vaadi tek yazar + düz dosya + sağlam FS koşullu (D1/D2/D3/D6). | 3,8 |
| **Deneyim** | **2,2** | Ara/değiştir, menü, yazdırma yok; uzun diyalog taşması ve sabit tarih biçimi 6 dilin 3'ünde görünür kusur. "Sessiz" ama çıplak. | 2,5 |
| **Ambalaj** | **2,0** | Artefaktlar üretiliyor ve lisanslı; ama hiç çalıştırılmadı, hiç yayınlanmadı, imza yok, jlink yok. | 2,0 |
| **Güven** | **2,8** | SPDX, üçüncü taraf bildirimleri, SHA pinleme, çıkış matrisi gerçek altyapı. Ama yayınlanmış sürüm, `SECURITY.md`, smoke test ve imza yok. | — |
| **Pazara erişim** | **1,5** | Hiç Release yok, dosya ilişkilendirmesi yok, CLI yok, notarization yok: ürün bulunsa bile OS'ten açılamıyor. | — |

### 11.1 Neden "motor ≠ ürün"

Motor skoru, "veri bozulmasın" tasarımının kalitesini ölçüyor ve 3,6 hak ediyor. Ürün skoru
ise kullanıcının **işini bitirip bitiremediğini** ölçüyor: ara/değiştir olmadan uzun metin,
menü olmadan keşfedilebilirlik, yayın olmadan erişim yok. Bu yüzden deneyim ve erişim
skorları motorun çok altında.

**Önceki skorlara göre düşüşler bilgi kaybı değil, ölçümün dürüstleşmesi:** P1'in kapattığı
yollar gerçek, ama dış değişiklik/symlink gibi vaadi koşullu kılan yollar artık görünür
durumda; skorlar bu koşulları yansıtıyor.

---

## 12. Go / No-Go ve P2 Girdileri

### 12.1 Hüküm

- **`1.0` için hazır değil.** Ürün, kullanıcının günlük döngüsünü tamamlamıyor ve hiç
  yayınlanmamış.
- **Ama P2 için sağlam bir taban var.** Kritik veri kaybı sınıfı kapalı; mimari test
  edilebilir; ambalaj dürüst.
- **P2'nin tanımı:** *"Bir romancı bir bölümü bitirebilir"* + *"indirdiği ikili gerçekten
  açılır"*. Yeni platform değil, **editör olma** ve **ilk gerçek sürüm**.

### 12.2 P2 kapsam adayları

> Bu bir **aday havuzudur**, sıralı bir plan değildir. Skor yalnız göreli öncelik
> göstergesidir; nihai sıra, bağımlılıklar ve takvim yol haritasında belirlenecek.
> Formül: `(Etki × Güven) / Efor`; Efor S=1, M=2, L=3, XL=5.

| ID | İş | Bulgular | Etki | Güven | Efor | Skor |
|---|---|---|---:|---:|---:|---:|
| **A1** | Belge kimliği: dış değişiklik algılama + symlink'e yazma + taslak yolu doğrulama | D1, D2, D3, D8 | 5 | 2 | L | 3,3 |
| **A2** | Ara / Değiştir (saf arama modeli + YOLO etkileşimi + Ctrl+F/H) | E1 | 5 | 3 | L | **5,0** |
| **A3** | G/Ç'yi FX thread'inden çıkar; otomatik kayıt maliyetini düşür | X1, X2, X4 | 4 | 2 | M | 4,0 |
| **A4** | Komut katmanı + menü çubuğu + CLI ile açma + sürükle-bırak | E2, E3, E4, M1 | 4 | 2 | M | 4,0 |
| **A5** | Diyalog taşması + tarih/saat ve ayırıcı yerelleşmesi | U1, U5 | 3 | 3 | M | 4,5 |
| **A6** | İlk gerçek sürüm + CI artefakt smoke testi | B1, B2 | 5 | 3 | M | **7,5** |
| **A7** | Yazar ergonomisi: son dosyalar, "bir daha gösterme", min genişlik, klavyeyle yazı boyutu | E5, U7, U8, U2 | 3 | 3 | M | 4,5 |
| **A8** | Taslak yaşam döngüsü + disk/boyut sınırları + hata maskeleme | D4, D5, D6, D7, D9 | 3 | 3 | M | 4,5 |
| **A9** | Test hijyeni: `AppLogging` enjeksiyonu, gerçek eve yazmayı bitir | M4, T6 | 2 | 3 | M | 3,0 |
| **A10** | JAR temizliği: JavaFX Media/FXML çıkar | B4 | 2 | 3 | S | 6,0 |
| **A11** | macOS çıkış yolunu doğrula (Cmd+Q → kaydetme sor.) | U10 | 4 | 1 | S | 4,0 |
| **A12** | Yazdırma / PDF | E7 | 3 | 2 | M | 3,0 |

**Bağımlılık notları:**

- A2 ve A4, A4'ün komut katmanına bağlıysa **tek seferde** yapılmalı; ayrı yapılırsa arama
  iki kez kablolanır.
- A1, A8 ile aynı "belge/taslak yaşam döngüsü" modelini paylaşır; A1 önce gelirse A8 küçülür.
- A3, A2'den önce yapılmalı: arama büyük belgede UI'yı kilitlememeli.
- A6, diğerlerinden bağımsız ve **en yüksek skorlu**; ilk sürüm çıkmadan diğer işlerin
  doğrulanması mümkün değil.

### 12.3 P2 dışı bırakılması önerilenler

| Öneri | Gerekçe |
|---|---|
| **Sekmeler / çoklu belge** | Tek belge, romancı için tutarlı bir kimlik; P1'in stabilize ettiği çıkış/kirli/taslak matrisini ikiye katlar. P4 işi. |
| **Yazım denetimi** | Sözlük lisansı + büyük UI yüzeyi (XL); farklılaştırıcı değil. Kullanıcı kanıtı olmadan girilmemeli. |
| **jlink ile runtime budama** | 145 MB → ~50 MB iyi bir kazanç ama ilk kullanıcıyı engellemiyor. Ölçüm ve karar P3. (Media/FXML temizliği A10 olarak kalabilir.) |
| **Kod imzalama / notarization** | **Koşullu**: ilk kullanıcı ve OS kararına bağlı (§12.5). macOS/Windows hedefleniyorsa P2'ye girer ve A6'yı değiştirir. |
| **Otomatik güncelleme / telemetri** | Yayınlanmış kurulu taban yokken anlamsız; gizlilik duruşuyla da çelişir. |
| **Satır numaraları, sarma anahtarı, girinti politikası** | A4'ün komut katmanından sonra ucuzlar; bölüm bitirmeyi engellemiyor. |
| **Dışa aktarma (HTML/DOCX), dosya ilişkilendirmesi** | P3; yazdırma (A12) ile aynı çıktı hattını paylaşır. |
| **Kapsam ölçümü (JaCoCo) kapısı** | Bu projede test suite'i kapı; yüzde metrik burada gürültü. |
| **XDG / macOS Application Support taşıması** | Kanal kararı ve kurulu taban olmadan riskli; P3. |
| **ES/IT register temizliği, tipografik apostrof** | Düşük etki; A5'in yanında bedava gelirse alınır. |

### 12.4 Üç bahis ve en büyük risk

**En yüksek getirili üç bahis:**

1. **A1 — Belge kimliği ve dış değişiklik.** Ürünün tek savunulabilir farklılaştırıcısını
   koşullu olmaktan çıkarır. Bir romancının fark edeceği tek "vay" anı burada.
2. **A2 + A4 — Arama ve komut yüzeyi.** "Sesli bir metin alanı" ile "editör" arasındaki fark;
   aynı zamanda gelecekteki her eylemin bağlanacağı yer.
3. **A6 — Gerçekten yayınla.** Listenin en yüksek bilgi/efor oranı; diğer her şey gerçek
   kullanıcı görülene kadar tahmin.

**En büyük ürün riski: pazar temasının kanıtsız olması.** 350 test ve titiz bir güvenlik
hikâyesi var, ama yayınlanmış sürüm olmadığı için ürünü kendisi yazmayan bir yazarın açtığına
dair kanıt yok. P2'nin en büyük riski
listede bir maddenin eksik olması değil, **bir yıl daha mükemmelleştirip hiç yayınlamamak**.

### 12.5 Yol haritasından önce insanın vermesi gereken tek karar

> **İlk benimseyen kullanıcı kim ve hangi işletim sisteminde?**

Bu tek cevap şunları belirler:

- macOS/Windows hedefleniyorsa **kod imzalama/notarization bütçesi** P2'ye girer ve A6'nın
  kapsamını değiştirir ("artefakt yayınla" → "sertifika al, imzala, yayınla").
- Linux hedefleniyorsa `.deb` + `.tar.gz` yeterli; imza ertelenebilir.
- Dağıtım kanalı (taşınabilir / installer) kararı bu cevaba bağlı olarak P3'te ADR'ye dönüşür.

Bu karar verilmeden yazılacak bir yol haritası, kodun ifade edemediği tek kısıtı tahmin etmiş
olur.

### 12.6 P2 için yanlışlanabilir çıkış kapısı (öneri)

1. **Üzerine yazma güvenliği:** Yüklemeden sonra diskte değişen bir dosyaya kaydetmek ya
   sorar ya reddeder; symlink'li belge kaydedildikten sonra **hâlâ symlink**'tir. Testli.
2. **Asgari döngü:** `Ctrl+F` bulur, `F3`/`Shift+F3` gezer, `Ctrl+H` bir/tümünü değiştirir;
   aynı işlevler menüden de erişilebilir; arama modeli toolkit'siz testli.
3. **Yerel ayar sığması:** En uzun DE/FR metinleriyle her `Alert` 768 px'e sığar (kelepçe +
   sarma + yeniden boyutlandırma); tüm tarih/saat gösterimi UI dilini kullanır.
4. **Donma yok:** 2 MB'lık bir belge açılır, kaydedilir ve otomatik kaydedilirken FX thread'inde
   dosya G/Ç'si veya `fsync` yoktur; `getText()` kirli kontrolünden önce kopyalanmaz.
5. **Sürüm var ve çalışıyor:** `v0.x` GitHub Releases'te yayınlanmıştır; CI, yayından önce
   app image'ı başlatıp `.deb`'i kurup kaldırmıştır.

---

## 13. Bulgu Dizini

**Dağılım:** 0 kritik · 8 yüksek · 39 orta · 13 düşük.

### Yüksek

| ID | Bulgu | Kanıt |
|---|---|---|
| D1 | Dış değişiklik algılama yok; sessiz üzerine yazma | `FileService.loadFile/writeFile` |
| D2 | Symlink'li belgede linkin kendisi değişiyor | `AtomicFiles.java:54-74` |
| D3 | Taslak başlığındaki yol doğrulanmıyor | `RecoveryStore.java:339` |
| E1 | Ara/Değiştir yok | `src/main` taraması; bundle'lar |
| X1 | Dosya G/Ç'si FX thread'inde | `FileService`, `AutoSaveService` |
| B1 | CI artefaktı çalıştırmıyor | `.github/workflows/maven.yml` |
| B2 | Hiç sürüm yayınlanmadı; hat denenmedi | `git tag -l` boş |
| U1 | `Alert` diyalogları kelepçesiz/sarmasız | `DialogService.java:96-263` |

### Orta

| ID | Bulgu | Kanıt |
|---|---|---|
| D4 | Taslak taraması symlink takip ediyor, sınırsız okuma, yalnız en yeni | `RecoveryStore.java:131-139,271` |
| D5 | `pending-*.jtw` toplanmıyor | `RecoveryStore` |
| D6 | Atomik olmayan fallback doğrulanmıyor; temp süpürme yok; istisna maskeleniyor | `AtomicFiles.java:67-80` |
| D7 | Disk kontrolü yok; OOM kaydı atlıyor | `FileService`, `RecoveryStore` |
| D8 | Belge düzeyinde kilit yok | `InstanceLock` |
| D12 | Ayar yazma hatası sessiz; `SecurityException` kaçıyor | `AppSettings.java:300-307` |
| D13 | Ayar şeması sürümü yok | `AppSettings.java:67-81` |
| E2 | Menü çubuğu yok; Yeni/Farklı Kaydet görünmez | `ToolbarView.java:134-138` |
| E3 | CLI ile dosya açma yok | `Application.getParameters()` yok |
| E7 | Yazdırma/PDF yok | `PrinterJob` yok |
| E8 | Satır numaraları yok | düz `TextArea` |
| E10 | Dosya ilişkilendirmesi/MIME yok | paketleme + `main.fxml` |
| X2 | Otomatik kayıt kirli kontrolünden önce tam kopya alıyor | `AutoSaveService.java:136-144` |
| X3 | 1 Hz istatistik O(n), çoklu tam kopya | `SessionStats.java:69-85` |
| X4 | `readAllBytes` bellek çarpanı, boyut sınırı yok | `FileService.java:191` |
| U3 | Toolbar/popup/editör kontrastı ölçülmüyor; ölü AA-altı token | `ContrastRatioTest`, `css/*` |
| U4 | macOS'ta tooltip `Ctrl` diyor | `ToolbarView.java:84-91` |
| U5 | Sabit tarih/saat biçimi | `StatsBarModel.java:51-52` |
| U6 | Erişilebilir adlar yalnız 6 combo'da | `ToolbarView.java:399-404` |
| U7 | Karşılama her açılışta, opt-out yok | `FxApplication.java:70` |
| U8 | Minimum pencere genişliği yok | `MainStage` |
| U9 | Ses politikası: ok/kısayol tuşları da ses çıkarıyor; seviye yok | `EditorKeyHandler.java:31-35` |
| U10 | macOS çıkış yolu garantisi yok | `MainStage`, `FxApplication` |
| B3 | İmza/notarization yok | `release.yml` |
| B4 | jlink yok; media/FXML ölü ağırlık | `pom.xml:73-82`, imaj |
| B5 | Yeniden üretilebilirlik doğrulanmadı | `pom.xml:61`, jpackage mtime |
| B7 | `~/.jtypewriter` tüm OS'lerde | `AppDirs.java:20` |
| B8 | Masaüstü entegrasyonu eksik (`Categories=Unknown`, MIME yok) | yerel `.deb` |
| B9 | Destek yüzeyi yok (`SECURITY.md`, şablon, tanılama) | repoda yok |
| B10 | Dependabot yalnız action'lar; SBOM/zafiyet taraması yok | `.github/dependabot.yml` |
| M1 | Komut/eylem katmanı yok | `KeyboardShortcutHandler`, `ToolbarView` |
| M2 | Tek belge varsayımı kök hücrelerde | `FileService`, `AppGraph` |
| M3 | 9 `Alert` tekrarı | `DialogService` |
| M4 | `AppLogging` global statik, enjeksiyon yok | `AppLogging.java:37,153-164` |
| M5 | Dinleyici sırası örtük | `AppGraph.java:47-86` |
| M6 | `UndoHistoryReset` yansıma + dönüş değeri yok sayılıyor | `EditorArea.java:114` |
| M7 | Undo geçmişi sınırsız | JavaFX varsayılanı |
| M8 | Dispose sözleşmesi yok | `StatsBar` timeline |
| T6 | Testler gerçek `~/.jtypewriter`'a yazıyor | `pom.xml:145-149` |

### Düşük

| ID | Bulgu |
|---|---|
| D9 | "Vazgeç" sonrası başarısız taslak silme yutuluyor |
| D10 | Yedek terfisi başarısızsa en yeni nesil kayboluyor |
| D11 | `user.home` yoksa açılış NPE |
| E4 | Sürükle-bırak yok |
| E5 | Son dosyalar yok |
| E6 | Tercihler penceresi yok |
| E9 | Sarma anahtarı/yazı tipi ailesi/satır sonu yok |
| U11 | Çökme diyaloğu ham `Throwable` basıyor |
| U12 | `" - "`/`": "` ayırıcıları ve register karışıklığı |
| B6 | Güncelleme denetimi yok (bilinçli) |
| B11 | Yerel artefaktlar bayat (`NOTICE`/jar uyuşmazlığı) |
| B12 | Kapsam/statik analiz yok (kısmen bilinçli) |
| B13 | `AGENTS.md` sürüm kontrolünde değil |

---

## 14. Ek — Ölçüm Yöntemi

| Ölçüm | Komut / kaynak |
|---|---|
| Sınıf ve satır sayısı | `find src/main/java -name '*.java' \| wc -l`; paket başına `wc -l` |
| Test sayısı | `grep -rho '@Test' src/test/java \| wc -l` (350); sınıf: 40 |
| Bundle anahtarları | `grep -cE '^[a-zA-Z][a-zA-Z0-9]*=' src/main/resources/messages*.properties` (her biri 90) |
| Artefakt boyutları | `du -sh releases/linux/...`; `ls -la` |
| Açılış / RSS | Yerel imaj 5 kez başlatıldı, ilk sahne ve ana pencere zaman damgaları; `ps` ile RSS |
| Runtime modülleri | İmajdaki `lib/runtime/release` ve `modules` dosyası; 52 modül |
| Tag/Release | `git tag -l` (boş); GitHub Releases API (`[]`) |
| CI kapıları | `.github/workflows/maven.yml`, `release.yml`, `scripts/package.sh` |
| Kontrast | `ContrastRatioTest` + `css/*.css` token çiftleri |

**Yeniden üretilebilirlik notu:** Bu belgedeki imaj/deb/tar.gz boyutları yerel, bayat
artefaktlardan alındı. Temiz bir `mvn clean package && ./scripts/package.sh --installer`
sonrası yeniden ölçülmeli; lisans payload'ı ve `NOTICE` boyutu da aynı koşuda doğrulanmalı.
