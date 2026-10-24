# JTypeWriter — Ürünleştirme Yol Haritası

> Sürüm: 1.1 · Tarih: 24 Ekim 2026 · Dayanak: `analysis.md` (bulgu dizini ve `file:line` kanıtları)
> Varsayım: 1 geliştirici, yarı zamanlı (~20 saat/hafta). Efor ölçeği: **S** ≤ 0,5 gün · **M** 1-2 gün · **L** 3-5 gün · **XL** > 1 hafta.
> **Dağıtım kararı (24 Ekim 2026):** JTypeWriter **yalnız portable** olarak dağıtılır. Installer, paket yöneticisi ve dosya ilişkilendirmesi kapsam dışıdır; ayrıntı ve gerekçe §2.1'de. Bu karar tek kanaldır — "kurulu mod" için ikinci bir kanal yoktur.

---

## 0. Yol Haritasının Mantığı

Sıralama "en çok iş" değil, **"en hızlı kazanım"** esasına göre yapıldı. Her iş şu skorla sıralandı:

```
Öncelik = (Kullanıcı Etkisi × Güven) / Efor
```

- **Kullanıcı Etkisi (1-5):** veri kaybı/engel = 5, görünür kusur = 3, konfor = 1.
- **Güven (1-3):** kanıt doğrulandı ve çözüm net = 3; keşif gerekiyor = 1.
- **Efor:** yukarıdaki ölçek.

Fazlar bu skorun doğal kümeleri:

| Faz | Tema | Süre | Çıkış kapısı |
|---|---|---|---|
| **P0** | Güven + ilk izlenim | İlk 5 iş günü | Ürün "taslağımı kaybetmez" diyebilir; 30 saniyede profesyonel görünür |
| **P1** | **Portable (kurulumsuz) sürüm** | Hafta 2-5 (~11-17 iş günü) | Tek kanal portable: izsiz çalışır, veri uygulama klasöründe taşınır, yönetici hakkı istemez |
| **P2** | Editör olma | Hafta 6-9 | Günlük sürdürülebilir yazma akışı: yeni belge, ara/değiştir, çökme testi |
| **P3** | Ürünleşme | Hafta 10-14 | Yayınlanabilir 1.x: tercihler, son dosyalar, dışa aktarma, performans |
| **P4** | Platform | v2 ve sonrası | Çoklu belge, depolama soyutlaması, erişilebilirlik denetimi |

**Kural:** P0 bitmeden P1'e, P1 bitmeden P2'ye geçilmez. P4 hiçbir zaman P0/P1/P2 işlerinin önüne alınmaz.

**Portable neden P0'dan hemen sonra:** veri yolu (`AppDirs`), runtime boyutu (jlink) ve dosya sistemi dayanıklılığı (`AtomicFiles`) sonradan değiştirildiğinde P2/P3'te yazılan her test ve doküman yeniden doğrulanmak zorunda kalır. Ayrıca ürünün "kaybetmeyi zorlaştır" vaadi, portable modda veri kaybı riskini büyüttüğü için güvenlik ağı (P0) kurulduktan **hemen sonra**, ürün özellikleri eklenmeden **önce** kapatılmalıdır.

---

## 1. Hızlı Kazanımlar — İlk 5 Gün (P0)

Tek tek ele alındığında 1-2 saatlik, toplamda ürün algısını değiştiren işler. Hiçbiri mimari değişiklik gerektirmez.

| ID | İş | Efor | Neden şimdi | Kanıt / dosya |
|---|---|---|---|---|
| RW-01 | Kurtarma diyaloğuna **"İptal / Sonra sor"** ekle; pencere kapatma taslağı silmesin | S | Ürünün tek vaadini çürüten en kısa yol | `DialogService.java`, `AutoSaveService.java` — **tamamlandı (ab6f92f):** üç seçenek (geri yükle / sil / sonra sor); kapatma = sonra sor; ertelenen taslak temiz çıkışta ve belge açılışlarında korunur (bu koruma otomatik kaydı susturuyordu — bkz. RW-54); `DialogServiceTest` + `AutoSaveServiceTest` regresyon testleri |
| RW-02 | Bozuk zaman damgası/metadata **taslağı düşürmesin**; metin sağlamsa teklif edilsin | S | Kurtarılabilir metni çöpe atan hata | `RecoveryStore.java:81-90` — **tamamlandı:** başlık alanları tek tek ve toleranslı çözülüyor; bozuk `saved.at` → dosya mtime, bozuk/eksik yol → `null`, bölücü/magic eksik dosya → tüm içerik metin olarak sunulur, geçersiz UTF-8 → değiştirme karakterleriyle; her durumda uyarı loglanır, metin korunur. `RecoveryStoreTest` 18 test |
| RW-03 | Draft + metadata'yı **tek atomik dosyaya** indir | S | İki yazma arası çökme yarım durum bırakıyor | `RecoveryStore.java:49-58` — **tamamlandı:** taslak tek `draft-<uuid>.jtw` dosyası (magic satırı + `saved.at` + Base64 yol + boş satır + metin), tek `AtomicFiles.writeMoving`; yarım durum fiziksel olarak imkânsız |
| RW-04 | Kurtarma dosyasını **örnek/oturum başına** ayır | S-M | İki pencere birbirinin taslağını eziyor | `RecoveryStore.java:35`, `AppDirs.java:20-23` — **tamamlandı:** oturum başına UUID'li dosya; `loadDraft` en yenisini seçer ve başka oturumun dosyasını kendi slotuna devralır (adoption), `clear()` yalnız kendi dosyasını siler. Legacy `draft.txt` göçü **bilinçli olarak yapılmadı** (henüz kullanıcı kitlesi yok) |
| RW-05 | **Yeni Belge** (`Ctrl+N`) + belge yüklerken undo geçmişini sıfırla | S | Editörün en temel eksiği; YOLO açıkken yeni belge imkânsız | `FileService.java:87-170`, `FxApplication.java:77-80` — **tamamlandı:** `FileService.newDocument` ortak kaydet/at/iptal kapısını kullanıyor; yeni belge yolsuz, temiz, UTF-8 (kodlama devralınmıyor). Tüm belge yükleme noktaları `EditorArea.setDocument` üzerinden geçiyor; `UndoHistoryReset` geri al/ileri al geçmişini sıfırlıyor (yansıma; sınıf yoluyla çalışır, modül yolunda uyarı loglayıp `setText` davranışına düşer). `Ctrl+N` altı bundle'da kısayol listesine eklendi |
| RW-06 | Log formatter'a **stack trace** ekle | S | Şu an hata raporu teşhis edilemez | `AppLogging.java:37-43` — **tamamlandı:** başlık satırı tek satır kalıyor; fırlatılan istisna 4 boşluk girintili tam yığın izi ve `Caused by:` zinciri olarak yazılıyor; biçimlendirici asla istisna fırlatmıyor (bozuk `printStackTrace` eski tek satırlık biçime düşer) |
| RW-07 | Bozuk ayarları `.corrupt` olarak **karantinaya al** | S | Sessiz üzerine yazma | `AppSettings.java:179-199` — **tamamlandı:** `Properties.load`/okuma hatasında dosya `settings.properties.corrupt` (doluysa `.corrupt.<millis>`, eskisi korunur) adına taşınıyor; taşınamazsa uyarı loglanıp dosya yerinde bırakılıyor. Varsayılanlara dönüş ve public API değişmedi |
| RW-08 | **Uygulama ikonu** ekle (`--icon`, pencereler, görev çubuğu) | S | İlk izlenimde "gerçek uygulama" sinyali | `scripts/generate-icons.py`, `AppIcons.java:49`, `package.sh:80-95` — **tamamlandı:** ikonun tek kaynağı `src/main/resources/icons/jtypewriter.svg`; `scripts/generate-icons.py` bundan 16..1024 px PNG setini + Windows `.ico` + macOS `.icns` üretiyor (`--check` bayat asset'i yakalar). `AppIcons.applyTo` altı PNG'yi `stage.getIcons()`'a veriyor (pencere/görev çubuğu/dock); eksik veya bozuk asset başlangıcı düşürmüyor, `AppLogging`'e yazılıyor. `package.sh` platforma göre PNG/.ico/.icns seçip `--icon` ile hem app-image hem installer'a geçiyor; eksikse net hata verip duruyor. `AppIconsTest` 5 test (classpath, PNG boyutları, ICO/ICNS magic byte'ları) |
| RW-09 | Diyaloglara pencere **başlığı** ver; `showError`'ı ana pencereye **sahiplendir** | S | Görev değiştiricide boş isim, arkada kalan hata | `WelcomeDialog.java:67`, `AboutDialog.java:100`, `DialogService.java:226-232` — **tamamlandı:** `welcomeWindowTitle`/`aboutWindowTitle` altı bundle'a eklendi ve `setTitle` ile uygulanıyor (dekorasyonsuz pencere olduğu için görünmez, görev değiştiricide görünür; koda bunu açıklayan not düşüldü). `showError` artık `Stage owner` alıyor ve `initOwner` çağırıyor; `FileService`'in üç çağrısı da pencereyi geçiyor, sahipsiz hata diyaloğu kalmadı. `CrashHandler` zaten sahiplendiriyordu, dokunulmadı. `DialogServiceTest` altı dilde başlıkların boş olmadığını doğruluyor |
| RW-10 | DE `toolbarRedo` → **Wiederholen**; TR hoş geldin metnini tamamla; `recoveryNo` → **Vazgeç** (TR/FR/IT) | S | Yanlış çeviri veri kaybı riskini artırıyor | `messages_de.properties:57`, `messages_tr.properties:28,67` — **tamamlandı:** DE `toolbarRedo=Wiederholen` (kısayol listesiyle aynı), `recoveryNo` TR=**Vazgeç**, FR=**Ignorer**, IT=**Ignora** (artık "sil" demiyor; EN `Discard` ve ES `Descartar` zaten doğruydu). TR `welcomeShortcuts` tamamen Türkçeleştirildi ("Araç çubuğu", "Yazı tipi", "Yeni belge / Belge aç" — README TR terimleriyle uyumlu). Ayrıca altı dilin tamamında hoş geldin metnindeki tema listesi gerçek döngü sırasına göre düzeltildi (LIGHT→DARK→SEPIA). `MessagesTest` bu üç sınıfı kalıcı olarak sabitliyor: çeviri kaybı riski taşıyan metinler, TR metninde İngilizce artık kalmaması, tema sırası |
| RW-11 | Dil değişiminde **pencere başlığını** yenile | S | Başlık eski dilde kalıyor | `MainController.java:35-67` — **tamamlandı:** `MainController` artık `LocaleManager` alıyor ve `addListener(language -> refreshTitle())` ile başlığı yeniden kuruyor; başlık `messages.appName()`/`windowUntitled()` üzerinden üretildiği için yeni dil doğru yansıyor. `AppGraph` bağlantısı güncellendi |
| RW-12 | `Ctrl+Shift+T` sonrası **tema etiketini** güncelle | S | Etiket ile gerçek tema ayrışıyor | `MainController.java:123-128` vs `ToolbarView.java:189-196` — **tamamlandı:** kök neden iki ayrı tema değiştirme yolu (düğme → `ToolbarView`, kısayol → `MainController`) ve kısayol yolunun etiketi atlamasıydı. `ToolbarView.cycleTheme()` public yapıldı; kısayol `toolbarView::cycleTheme` ile aynı yola bağlandı, `MainController`'daki kopya ve artık gereksiz `ThemeManager` bağımlılığı kaldırıldı |
| RW-13 | README dürüstlüğü: fat JAR platforma özgü; WAV kaynağı; macOS `⌘` | S | Yanlış vaat güven kaybı | `README.md` — **tamamlandı (17b72cf):** EN + TR eşlendi; ince/fat JAR ayrımı, fat JAR'ın platforma özgülüğü, atomik move fallback'i, BOM tabanlı kodlama tespiti, tema döngüsü, çoklu dil kapsamı, tek örnek kilidi, tam ekran/araç çubuğu, durum çubuğu boyut döngüsü, mimari ve test tabloları gerçek davranışa göre düzeltildi. O tarihte iki dil tek dosyadaydı; doküman sonradan `README.md` (EN) + `README-TR.md` (TR) olarak bölündü |
| RW-54 | **"Sonra sor" taslağını ayrı dosyaya park et** — ertelenen taslak yeni belgenin otomatik kaydını susturmasın | S | Tek yuvalı deponun doğrudan sonucu: "sonra sor" diyen oturum, yeni yazdığı metin için hiç kurtarma kopyası üretmiyordu — güven vaadinin tam ortasında sessiz açık | `RecoveryStore.park()`, `AutoSaveService.keepDraftForNextStart()` — **tamamlandı:** ertelenen taslak `pending-<uuid>.jtw` olarak aynı kurtarma dizinine taşınıyor; aktif slot serbest kalıyor ve snapshot'lar devam ediyor. `loadDraft()` park edilmiş dosyayı da okur (sonraki oturum devralır), `clear()` yalnız oturumun kendi slotunu siler, yani park edilen taslak temiz çıkışta da yaşar. Taşıma başarısız olursa (rename hatası) slot yeniden dondurulur — tek kopyanın üzerine yazmak yerine susmak. **Kabul kriteri:** ertelenen taslak ve canlı taslak yan yana yaşar, otomatik kayıt hiç susmaz; `RecoveryStoreTest` (18) + `AutoSaveServiceTest` (5) regresyon altında |

**P0 ilk gün sırası:** RW-01 → RW-02 → RW-03 → RW-04 (tek oturum, kurtarma alt sistemi) → RW-05 → RW-06/07 (bağımsız, paralel) → RW-10/11/12 (i18n/UI tek oturum) → RW-08/09. RW-01 (kurtarma iptali), RW-02..04 (kurtarma deposu), RW-05 (yeni belge + undo sıfırlama), RW-06 (log yığın izi), RW-07 (ayar karantinası), RW-08 (uygulama ikonu), RW-09 (diyalog başlıkları), RW-10/11/12 (i18n düzeltmeleri + dil değişiminde başlık + tema etiketi), RW-13 (README hizalaması), RW-15/16/17 (araç çubuğu taşma stratejisi, tooltip + Tab erişilebilirliği, odak halkaları + durum metni kontrastı) ve RW-54 ("sonra sor" taslağının park edilmesi) **tamamlandı**; kalan P0 işleri (RW-18..20) uygulandıkça `README.md` ve `README-TR.md`'ye birlikte işlenir.

> **RW-14 taşındı:** "`--license-file` ile kuruluma lisans dosyası ekle" işi installer kanalına aitti. Portable tek kanal kararıyla P1'e taşındı ve yeniden tanımlandı: lisans dosyası artefaktın **içine** konur (F4.5). Eşleme tablosu §2.5.0'da.

### P0 kalan işler (gün 2-5) — "profesyonel görünüm"

| ID | İş | Efor | Kabul kriteri |
|---|---|---|---|
| RW-15 | Araç çubuğu taşma/sarma stratejisi; literal `Label("\|")` ayırıcıları kaldır | M | 1280 px'te DE/FR metinleri kırpılmıyor; 900 px'te taşmıyor — **tamamlandı:** araç çubuğu her genişlikte tek satır kalıyor; sığmadığında altı açılır listenin başlığı değere iniyor (FULL→COMPACT), beş düğme hiç daralmıyor ve 40 px histerezis titremeyi önlüyor. Ölçülen satır genişlikleri (kontroller toplamı): DE tam satır 968 px, FR 1029 px, ES 1088 px; daraltılmış DE 727 px, FR 786 px. Böylece 1280 px'te hiçbir dilde kırpılma yok, 900 px'te de altı dil sığıyor. FR ölçümü (önceden açık madde) artık yapıldı |
| RW-16 | Tüm araç çubuğu düğmelerine **tooltip**; araç çubuğu **Tab ile gezilebilir** | M | Klavye ile aç/kaydet/tema erişilebilir; tooltip'ler bundle'dan. **tamamlandı:** 11 kontrolde bundle tabanlı tooltip var (kısayol ipucuyla, ör. "Save (Ctrl+S)"); Tab sırası `viewOrder` ile soldan sağa kuruldu (JavaFX 26'da `setNextFocus` yok); yazı boyutu ve dil listeleri başlık kazandı; altı listeye erişilebilir metin/rol eklendi; `toolbarFontSize`/`toolbarLanguage` anahtarları 6 bundle'a eklendi |
| RW-17 | **Odak halkalarını** geri getir; durum metni ≥13 px ve WCAG AA kontrast | M | `base.css`'te `-fx-focus-color: transparent` kaldırıldı; 3 temada kontrast ölçüldü. **tamamlandı:** `-focus-ring` tema token'ı tanımlandı (base.css'te hex yok); butonlar, altı liste, popup satırı ve editör için odak göstergesi var; durum metni varsayılan 13 px, AA kontrast light 4.68 / dark 5.15 / sepia 4.83, popup hücreleri 11 px. `ContrastRatioTest` CSS'i okuyup eşiğin altına düşüşü yakalıyor |
| RW-18 | Kalıcı görünüm durumu: araç çubuğu görünürlüğü, fullscreen, istatistik boyutu | S | Yeniden başlatmada korunuyor |
| RW-19 | Otomatik kayıt için görünür durum ("kaydedildi 12:04") | S | Kirli temiz geçişinde UI'da görünür |
| RW-20 | `TextCodec` BOM'lu yolda **strict decode**; başarısızsa kullanıcıya sor | S | Bozuk BOM'lu dosya sessizce `U+FFFD` üretmiyor; test eklendi |

### P0 çıkış kriterleri (Definition of Done)

1. Kurtarma: diyaloğu kapatmak, bozuk metadata, iki eşzamanlı örnek, "sonra sor" — **hiçbiri metin kaybetmiyor** ve ertelenen taslak otomatik kaydı susturmuyor; her biri için otomatik test. *(RW-01..04 ve RW-54 tamamlandı: `DialogServiceTest`, `AutoSaveServiceTest`, `RecoveryStoreTest`; uçtan uca kill-testi RW-24'te.)*
2. `Ctrl+N` çalışıyor; yeni belgede `Ctrl+Z` önceki belgeye gitmiyor. *(tamamlandı: `FileServiceTest` + `UndoHistoryReset`)*
3. Uygulama ikonu, pencere başlıkları, tooltip'ler, odak halkaları mevcut; toolbar DE/FR'de taşmıyor. *(RW-15/16/17 ile karşılandı: tooltip'ler ve odak halkaları mevcut, DE/FR taşmıyor.)*
4. 6 bundle'da çeviri hatası yok; dil değişimi tüm görünür metni yeniliyor. *(çeviri düzeltmeleri ve dil değişiminde pencere başlığının yenilenmesi tamamlandı — RW-10/11; "tüm görünür metin" kriterinin tooltip/erişilebilirlik ayağı RW-16 ile karşılandı)*
5. `mvn clean test` yeşil; TR/DE locale koşuları yeşil. *(Paketleme/dağıtım doğrulaması P1'e taşındı: installer kanalı kaldırılıyor, artefakt üretimi F4'te yeniden yazılıyor.)*
6. `README.md` (EN) ve `README-TR.md` (TR) ürünün gerçekten yaptığını anlatıyor — temel hizalama **tamamlandı**; kalan P0 işleri (RW-18..20) uygulandıkça iki dosyada birlikte güncellenir.

---

## 2. Faz P1 — Portable (Kurulumsuz) Sürüm (Hafta 2-5)

Amaç: JTypeWriter'ın **tek dağıtım kanalı** portable olsun. Kullanıcı indirir, açar, çalıştırır; verisi uygulama klasöründeki `data/` altında durur ve klasörle birlikte taşınır. Yönetici hakkı, kurulum, sisteme kayıt yok.

### 2.1 Karar ve kapsam

**Karar:** JTypeWriter yalnız **portable** olarak dağıtılacak. Installer (`.deb`/`.msi`/`.dmg`), dosya ilişkilendirmesi ve otomatik güncelleme kapsam dışı. Kullanıcı uygulamayı indirir, açar, çalıştırır; verisi uygulama klasöründe taşınır.

Bunun kabul edilen sonuçları:

| Kaybedilen | Etki | Telafi |
|---|---|---|
| `.txt` dosyasına çift tıkla açma | Yok | "Birlikte aç" ile manuel; ilk çalıştırma ekranında anlatılır |
| Menü/başlatıcı kısayolu | Küçük | Windows'ta `.exe`e kısayol; macOS'ta Dock'a sürükleme |
| Otomatik güncelleme | Küçük | Yeni zip'i `data/` klasörünü koruyarak üzerine aç; sürüm notlarında anlatılır |
| Paket yöneticisi üzerinden kurulum | Küçük | Doğrudan indirme + SHA-256 |

**Kazanılan:** admin hakkı gerekmez, sisteme kurulum yok, veri taşınır, "sistemimde iz bırakma" garantisi, dağıtım sürtünmesi düşük.

> Not: Bu, uygulamanın **varsayılan ve tek** davranışı olacak. "Kurulu mod" için ikinci bir kanal **yok**; yalnızca yazılamayan medya gibi zorunlu durumlarda sessiz olmayan bir geri düşüş (fallback) var (bkz. §2.4.2).

### 2.2 "Portable" tanımı — kabul kriterleri

Aşağıdakilerin tamamı sağlanmadan portable denilemez:

1. **İzsiz çalışma:** Temiz bir makinede çalıştır → yaz → kaydet → kapat sonrası `~` altında, `~/.openjfx` dahil, **hiçbir** JTypeWriter dosyası kalmaz.
2. **Veri taşınır:** `data/` klasörü uygulamayla birlikte kopyalandığında ayarlar, son dosya, tema, dil ve kurtarma taslağı yeni makinede aynen açılır.
3. **Yetki gerekmez:** Yönetici/root hakkı olmadan, `Program Files` dışında herhangi bir klasörden çalışır.
4. **Yazılamayan medya:** Uygulama klasörü salt-okunur ise (CD, kilitli USB, ağ paylaşımı) açılış **başarısız olmaz**; kullanıcıya açık bir bildirimle kullanıcı dizinine geçer.
5. **Çıkarılabilir medya güvenliği:** FAT32/exFAT USB'de kaydetme sırasında elektrik/USB kesintisi belgeyi bozmaz; en kötü durumda son yedek (`file.bak`) geçerlidir.
6. **Tek örnek:** Aynı USB'den iki kez çalıştırıldığında ikinci örnek veriyi ezmez.
7. **Boyut:** Dizin ≤ 70 MB, zip ≤ 45 MB (mevcut 146 MB'a karşı).
8. **Başlangıç:** Soğuk başlangıç medyanı ≤ 1,5 sn (10 koşu).

### 2.3 Bugünkü engeller (kanıtlı)

| # | Engel | Kanıt | Etki |
|---|---|---|---|
| E1 | Veri `~/.jtypewriter` altına yazılıyor | `AppDirs.java:15` | İz bırakır, veri taşınmaz |
| E2 | JavaFX native'leri `~/.openjfx/cache`e çıkarılıyor | `NativeLibLoader.cacheLibrary()` (OpenJFX kaynağı), bu makinede `~/.openjfx/cache` mevcut | İzsiz çalışma iddiasını bozar; salt-okunur home'da başlatma kırılır |
| E3 | Runtime 134 MB, tüm JDK modülleri (53 modül) | `releases/linux/JTypeWriter/lib/runtime/release` | USB dağıtımı için kabul edilemez |
| E4 | `javafx-media` yalnız tek WAV için taşınıyor (+ native medya) | `pom.xml`, `TypewriterSound.java` | Boyut + paketleme yüzeyi |
| E5 | WAV, `java.io.tmpdir`e çıkarılıyor | `TypewriterSound.java:103-105` | `/tmp` izi, çökmede artık |
| E6 | `ATOMIC_MOVE` yoksa atomik olmayan rename'e düşüyor | `AtomicFiles.java:66-69` | FAT32/exFAT USB'de "yarım dosya yok" garantisi zayıflar |
| E7 | Boş `main.fxml` + `FXMLLoader` | `FxApplication.java:29-39` | Gereksiz modül/dolaylılık |
| E8 | `AppDirs` her OS'te aynı yol | `AppDirs.java:14-25` | Portable'da anlamsız; yine de fallback için gerekli |
| E9 | Tek örnek kilidi yok | — | Aynı `data/` iki süreçte bozulur — **kısmen çözüldü:** `InstanceLock` + `SingleInstanceWarning` mevcut; kalan iş kilidin `data/` yoluna bağlanması (F1.4) |
| E10 | Linux'ta tıkla-çalıştır yok (`bin/JTypeWriter` betiği) | `releases/linux/JTypeWriter/bin/` | Çift tıklama güvenilmez |
| E11 | macOS imzasız/notarize edilmemiş | `package.sh` | Gatekeeper "açılamıyor" der |

### 2.4 Hedef mimari

#### 2.4.1 Dizin düzeni (dağıtım artefaktı)

```
JTypeWriter-1.0.0-linux-x64/
├── JTypeWriter              # başlatıcı (Windows: JTypeWriter.exe, macOS: JTypeWriter.app)
├── runtime/                 # jlink ile üretilmiş minimal JRE + JavaFX native'leri
├── app/jtypewriter.jar      # modüler uygulama
├── data/                    # TAŞINAN kullanıcı verisi
│   ├── settings.properties
│   ├── recovery/
│   ├── logs/
│   └── instance.lock
├── LICENSE
└── PORTABLE-README.txt      # ilk çalıştırma notları, sürüm, checksum
```

**Değişmez kural:** `data/` dışında hiçbir yere yazılmaz. Güncelleme = yeni klasörü aç, `data/`yı koru.

#### 2.4.2 Veri yolu çözümleme (tek kaynak, öncelik sırası)

1. `-Djtypewriter.home=<yol>` — geliştirici/test/ileri düzey.
2. `JTYPEWRITER_HOME` ortam değişkeni.
3. `<uygulama-dizini>/data` — **yazılabilirse varsayılan** (portable mod).
4. `<user.home>/.jtypewriter` — yalnızca (3) yazılamıyorsa; açılışta **bir kez** görünür uyarı: "Uygulama klasörü salt-okunur; veriler kullanıcı dizinine yazılıyor."

Uygulama dizini çözümü: `jpackage.app-path` → yoksa `ProtectionDomain` kod kaynağı → yoksa `user.dir`. Üçü de başarısızsa (4).

#### 2.4.3 Native çözümü (E2'nin kesin çözümü)

OpenJFX `NativeLibLoader` arama sırası: **önce `${java.home}` (jlink'lenmiş JavaFX modülleri) veya jar'ın yanındaki dizin**, sonra jar içinden çıkarıp `javafx.cachedir` (varsayılan `~/.openjfx/cache/<sürüm>/<arch>`) altına yazar.

Bu yüzden doğru çözüm **jlink**: JavaFX modülleri `${java.home}` içine girdiğinde çıkarım hiç yapılmaz. Alternatif/geçici çözüm `-Djavafx.cachedir=<app>/data/.fx-cache` olur ama bu, "data dışına yazma yok" kuralıyla çelişmez; yine de `data/` içinde native kopyaları taşımak istemiyoruz — **tercih jlink**.

Doğrulama: `-Djavafx.verbose=true` ile çalıştır, `~/.openjfx` oluşmamalı.

#### 2.4.4 Dosya sistemi dayanıklılığı

- Açılışta veri dizininde **atomik taşıma sondası** (küçük temp dosya + `ATOMIC_MOVE`); sonuç önbelleğe alınır.
- Sonda başarısızsa (FAT32/exFAT): yazma = temp + `force(true)` + **geri okuyup SHA-256 doğrulama** + rename + dizin `fsync` (mümkünse) + kullanıcıya bir kez "bu medya atomik kaydı desteklemiyor, yedekler kullanılıyor" bildirimi.
- `BackupKeeper` portable'da **zorunlu** (kapatılamaz).
- Çıkarılabilir medya kopması: `IOException` yakalanır, belge bellekte dirty kalır, "medya kayboldu; farklı kaydet" diyaloğu açılır.
- Açılışta eski `jtw-*.tmp` dosyaları temizlenir.

### 2.5 Fazlar

Sıra bağımlılığa göre; her faz kendi başına test edilebilir ve geri alınabilir. Efor: 1 geliştirici, yarı zamanlı.

#### 2.5.0 ID eşlemesi (eski ürün yol haritası → portable adımları)

Portable işleri eski RW kimlikleriyle değil **F adımlarıyla** yürür; RW numaraları başka bölümlerden referans verildiği için **korunmuştur** (boşluk normaldir).

| Eski ID | İş | Yeni yer | Not |
|---|---|---|---|
| RW-14 | Lisans dosyası (`--license-file`) | F4.5 | Installer kanalı kalktı: `LICENSE` artefaktın içine konur |
| RW-26 | Release pipeline | F4.6 | Tag → 3 OS portable artefakt + `SHA256SUMS`; installer adımları kaldırılır |
| RW-27 | İmzalama hazırlığı | F4.3 + §7 | macOS ad-hoc imza + ilk açılış talimatı; Windows "unsigned" notu; notarization ayrı (ücretli) karar |
| RW-38 | `javafx-media` → `javax.sound.sampled.Clip` | F2.1 | |
| RW-39 | Boş `main.fxml` + `FXMLLoader` kaldır | F2.2 | |
| RW-40 | `module-info.java` + jlink runtime | F2.3 / F2.4 | |
| RW-41 | Dosya ilişkilendirmesi (`.txt`, `.md`, MIME, "Open with") | **kapsam dışı** §2.8 | Portable tek kanal kararı: kayıt yok, "birlikte aç" manuel |
| RW-42 | `AppDirs` platform/portable | F1.1 | Platform yolu yalnız fallback olarak kalır |
| RW-43 | Tek örnek kilidi | F1.4 | **kısmen tamamlandı:** `InstanceLock`, `SingleInstanceWarning` |
| RW-44 | `AtomicFiles` atomik olmayan fallback | F3.1 / F3.2 / F3.4 | Fallback kaldırılmaz, doğrulanır ve raporlanır |

#### 2.5.1 F0 — Hazırlık ve Ölçüm (0,5-1 gün)

| # | Adım | Çıktı |
|---|---|---|
| F0.1 | Baseline ölç: dizin boyutu, zip boyutu, soğuk/sıcak başlangıç (10 koşu medyanı), `~/.openjfx` ve `/tmp` iz envanteri | `docs/portable-baseline.md` |
| F0.2 | İz denetim betiği: zaman damgası işaretle → uygulamayı çalıştır/kapat → `find ~ /tmp -newer marker` | `scripts/portable-trace-audit.sh` |
| F0.3 | **Kritik spike (yarım gün):** JavaFX modüllerini jlink ile runtime'a al; `-Djavafx.verbose=true` ile `~/.openjfx` oluşmadığını ve boyutun ≤70 MB olduğunu kanıtla | Spike notu: jlink yolu doğrulandı/çürütüldü |
| F0.4 | Karar kaydı: spike başarısızsa alternatif `-Djavafx.cachedir=<app>/data/.fxcache` planı devreye girer | Kısa ADR |

**Kapı:** F0.3 kanıtlanmadan F2'ye başlanmaz (en pahalı varsayım).

#### 2.5.2 F1 — Veri Yolu, Tek Örnek, Geçiş (2-3 gün)

| # | Adım | Dosyalar | Kabul |
|---|---|---|---|
| F1.1 | `AppDirs`'i **çözümleyiciye** çevir: öncelik sırası (§2.4.2), uygulama dizini tespiti, yazılabilirlik testi, tek seferlik uyarı | `AppDirs.java` (veya yeni `AppHome.java`) | 4 önceliğin her biri testte; salt-okunur dizinde fallback |
| F1.2 | Tüm çağrıları enjekte edilebilir hâle getir: `AppLogging.init(Path)`, `AppSettings.load(Path)`, `RecoveryStore(Path)`, `CrashHandler` log yolu | `AppLogging`, `AppSettings`, `AppGraph`, `CrashHandler` | Testler gerçek `~` dizinine dokunmuyor (`@TempDir`) |
| F1.3 | İlk çalıştırmada `data/` oluştur; `data/` yoksa ve eski `~/.jtypewriter` varsa **tek seferlik içe aktarma** teklifi (kaynak salt-okunur, geri yazma yok) | `AppGraph` / küçük `PortableDataMigrator` | İçe aktarma sonrası eski dizin değişmiyor |
| F1.4 | **Tek örnek kilidi**: `data/instance.lock` üzerinde `FileChannel.tryLock`; kilit alınamazsa "zaten açık" diyaloğu ve çıkış. *(`InstanceLock`/`SingleInstanceWarning` mevcut — kalan iş kilidin çözümlenen veri yoluna bağlanması ve `InstanceLockTest`)* | `JTypeWriterApp`/`FxApplication` başlangıcı | İkinci örnek veriye yazmıyor; kilit süreç ölünce serbest |
| F1.5 | Kilit/veri yolu için birim testleri + elle senaryo | `AppHomeTest`, `InstanceLockTest` | 2 süreçli test yeşil |

**Kapı:** Hiçbir kod yolu doğrudan `AppDirs.dataDir()` çağırmıyor; hepsi enjekte edilmiş yol kullanıyor.

#### 2.5.3 F2 — Runtime Temizliği ve Native İzsizlik (2-4 gün)

| # | Adım | Dosyalar | Kabul |
|---|---|---|---|
| F2.1 | `javafx-media` kaldır → `javax.sound.sampled.Clip`, kaynaktan belleğe yükle (temp dosya yok) | `pom.xml`, `TypewriterSound.java` | `/tmp`de WAV yok; hızlı ardışık tuşta ses gecikmesi artmadı; ses aygıtı yokken sessiz |
| F2.2 | Boş `main.fxml` + `FXMLLoader` + `Initializable` kaldır; kök düğüm `MainController`dan | `FxApplication`, `MainController`, `resources/fxml/` | `javafx-fxml` bağımlılığı yok; uygulama aynı görünüyor |
| F2.3 | `module-info.java` ekle (`requires javafx.controls`, `java.desktop`, `java.logging`; `opens` gerekmiyorsa yok) | `src/main/java/module-info.java` | `mvn test` modüler yapıda yeşil (`useModulePath=false`) |
| F2.4 | `package.sh`: `jlink` ile runtime üret (`--add-modules`), `jpackage --runtime-image`; `--enable-native-access` ayarla; `-XX:-UsePerfData` ekle | `package.sh` | İmaj ≤70 MB; `~/.openjfx` **yok**; `/tmp`de `hsperfdata` yok |
| F2.5 | Başlangıç ölçümü ve regresyon kontrolü | — | Medyan ≤1,5 sn, baseline'a göre ≤%20 kötüleşme |

**Kapı:** `javafx.verbose` çıktısında "extracted to cache" satırı yok; `~/.openjfx` hiç oluşmuyor.

#### 2.5.4 F3 — Dosya Sistemi Dayanıklılığı (2 gün)

| # | Adım | Dosyalar | Kabul |
|---|---|---|---|
| F3.1 | Açılışta atomik taşıma sondası + sonucu önbelleğe al; desteklenmiyorsa doğrulamalı yazma yolu | `AtomicFiles`, `AppGraph` | FAT32'de uyarı bir kez gösterilir |
| F3.2 | Yazma sonrası SHA-256 doğrulama (yalnız atomik olmayan yolda) | `AtomicFiles` | Bozuk yazma sessizce geçmiyor |
| F3.3 | Çıkarılabilir medya kopması: hata yakalama, dirty kalma, "farklı kaydet" diyaloğu | `FileService`, `DialogService` | USB çekilince uygulama çökmüyor, metin kaybolmuyor |
| F3.4 | Açılışta `jtw-*.tmp` temizliği (yalnız kendi veri/dizin kapsamında) | `AtomicFiles`/`AppGraph` | Kalıntı temizleniyor; başka dosyaya dokunulmuyor |
| F3.5 | FAT32/exFAT USB üzerinde gerçek senaryo testi (yaz, kaydet, çıkar, geri tak) | — | Belge + yedek sağlam |

**Kapı:** FAT32 USB'de zorla kesme sonrası belge ya da `file.bak` açılabiliyor.

#### 2.5.5 F4 — Paketleme ve Başlatıcılar (3-4 gün)

| # | Adım | Çıktı | Kabul |
|---|---|---|---|
| F4.1 | Artefakt adlandırma standardı: `JTypeWriter-<sürüm>-<os>-<arch>.zip` (Windows/macOS) ve `.tar.gz` (Linux) | — | Sürüm tek kaynak: `pom.xml` |
| F4.2 | Windows: app-image zip; `data/` boş şablon; SmartScreen notu | `.zip` | Çift tıkla `JTypeWriter.exe` çalışır |
| F4.3 | macOS: `.app` + **ad-hoc imza** (`codesign -s -`), zip; Gatekeeper ilk açılış talimatı; notarization kararı (ücretli) ayrı madde | `.zip` | Temiz macOS'ta sağ tık → Aç ile çalışır |
| F4.4 | Linux: `tar.gz` + çalıştırılabilir `JTypeWriter` başlatıcı; **AppImage** (FUSE yoksa `--appimage-extract-and-run` notu) | `tar.gz`, `.AppImage` | Dosya yöneticisinden çift tıkla açılır |
| F4.5 | `PORTABLE-README.txt` (ilk çalıştırma, veri taşıma, güncelleme, bilinen sınırlar) + `LICENSE` | Dosyalar | 6 dilde kısa not (en az EN/TR) |
| F4.6 | CI: 3 OS portable artefakt + `SHA256SUMS` + tag tetiği; installer adımları **kaldırılır** | `.github/workflows/maven.yml` | Tag → 3 artefakt + checksum |
| F4.7 | Güncelleme akışı: yeni sürüm zip'i `data/` korunarak açılır; sürüm uyumsuzluğunda ayar şeması kontrolü | Doküman + `AppSettings` şema sürümü | `data/` korunuyor, ayarlar kaybolmuyor |

**Kapı:** Üç OS'te tek komutla artefakt; hiçbiri installer üretmiyor.

#### 2.5.6 F5 — Doğrulama ve Dokümantasyon (2-3 gün)

| # | Adım | Kabul |
|---|---|---|
| F5.1 | §2.6 test matrisinin tamamı | Tüm satırlar yeşil |
| F5.2 | İz denetimi: temiz VM/USB, `find ~ /tmp -newer marker` boş | Sıfır iz |
| F5.3 | `README.md` (EN) ve `README-TR.md` (TR) portable'a göre yeniden yazılır; "fat JAR portable" iddiası kaldırılır (AGENTS.md kuralı: iki dosya aynı bölümleri aynı sırada taşır) | Doküman gerçeği anlatıyor |
| F5.4 | Bilinen sınırlar sayfası: dosya ilişkilendirmesi yok, imza durumu, FAT32 uyarısı | Kullanıcı sürpriz yaşamıyor |
| F5.5 | Sürüm etiketi + checksum + yayın notu | Yayına hazır |

#### 2.5.7 Bağımlılık sırası

```
F0 (ölçüm + jlink spike)
 └─> F1 (veri yolu + kilit)  ──┐
 └─> F2 (medya/FXML/jlink)  ──┼─> F4 (paketleme) ─> F5 (doğrulama)
        F3 (FS dayanıklılığı) ┘
```

- **F0.3 kanıtlanmadan F2/F4 başlamaz.**
- **F3, P0 kurtarma düzeltmelerine bağlıdır** (RW-01..04, tamamlandı). Portable, veri kaybı riskini büyüttüğü için P0 kapısı kapanmadan portable yayın yapılmaz.
- F1 ve F2 paralel yürüyebilir (farklı dosyalar); F4 ikisini de bekler.
- F5, F4 bitmeden başlamaz.

### 2.6 Doğrulama matrisi (F5.1)

Her satır, yayın öncesi elle veya otomatik olarak kanıtlanır.

| # | Senaryo | Beklenen |
|---|---|---|
| T1 | Temiz Windows 11 VM, admin yok, Masaüstü'nden çalıştır | Açılır, yazar, kaydeder, kapanır; `%USERPROFILE%` altında iz yok |
| T2 | Windows, yol: `C:\Users\İsmail\Yeni Klasör\JTypeWriter` | Türkçe karakterli/boşluklu yolda sorunsuz |
| T3 | macOS arm64, indirilen zip (karantina) | Sağ tık → Aç ile çalışır; Dock'a eklenebilir |
| T4 | Ubuntu 24.04 X11, AppImage | Çift tıkla açılır |
| T5 | Ubuntu Wayland | Pencere/ölçekleme doğru; açılır |
| T6 | FAT32 USB: çalıştır → yaz → kaydet → USB'yi çıkar → geri tak | Belge ve `file.bak` sağlam; uyarı bir kez gösterildi |
| T7 | USB çekme (kaydetme sırasında) | Çökme yok; metin bellekte; "farklı kaydet" diyaloğu |
| T8 | Uygulama klasörü salt-okunur | Açılır; veri kullanıcı dizinine gider; görünür uyarı |
| T9 | Aynı `data/` ile ikinci örnek | "Zaten açık" diyaloğu; veri bozulmaz |
| T10 | `kill -9` sonrası yeniden açılış (USB) | Kurtarma taslağı teklif edilir; geri yüklenir |
| T11 | Sürüm güncelleme: yeni zip, `data/` korunur | Ayarlar/dil/tema/son dosya korunur |
| T12 | 50 MB belge, USB'de kaydet | UI donmaz; süre makul; hata yok |
| T13 | İz denetimi `find ~ /tmp -newer marker` | Boş (uygulama dizini hariç) |
| T14 | TR ve DE sistem locale'i ile ilk açılış | Doğru dil, bozuk metin yok |
| T15 | Ses aygıtı yok | Sessiz çalışır; hata diyaloğu yok |
| T16 | %150 ekran ölçekleme | Metin keskin; araç çubuğu taşmıyor |

### 2.7 Efor özeti

| Faz | İyimser | Kötümser |
|---|---|---|
| F0 Hazırlık + spike | 0,5 gün | 1 gün |
| F1 Veri yolu + kilit + göç | 2 gün | 3 gün |
| F2 Medya/FXML/jlink | 2 gün | 4 gün |
| F3 FS dayanıklılığı | 1,5 gün | 2 gün |
| F4 Paketleme + CI + launcher | 3 gün | 4 gün |
| F5 Doğrulama + doküman | 2 gün | 3 gün |
| **Toplam** | **~11 gün** | **~17 gün** (yarı zamanlı ~4 hafta) |

### 2.8 Kapsam dışı (bilinçli)

- Installer (`.deb`, `.msi`, `.dmg`) ve paket yöneticisi dağıtımı
- Dosya ilişkilendirmesi / "Birlikte aç" kaydı (RW-41)
- Otomatik güncelleme (yalnız "yeni sürüm var" bilgisi opsiyonel)
- Bulut senkronizasyonu, hesap sistemi
- Aynı anda iki farklı `data/`yı birleştirme
- Çoklu belge/sekme (P4)

### 2.9 Yayın kontrol listesi

- [ ] F0.3 spike kanıtlandı, `~/.openjfx` oluşmuyor
- [ ] Hiçbir kod yolu `user.home`a doğrudan yazmıyor
- [ ] Tek örnek kilidi çalışıyor
- [ ] FAT32 USB testi (T6, T7, T10) geçti
- [ ] Salt-okunur fallback (T8) geçti
- [ ] 3 OS artefaktı + `SHA256SUMS` üretildi
- [ ] macOS ad-hoc imza + ilk açılış talimatı hazır
- [ ] Linux AppImage ve tar.gz doğrulandı
- [ ] `mvn clean test` + TR/DE locale koşuları yeşil
- [ ] `README.md` + `README-TR.md` ve `PORTABLE-README.txt` güncel
- [ ] Bilinen sınırlar yayınlandı
- [ ] `data/` korunarak güncelleme adımı dokümante edildi

### 2.10 P1 çıkış kriterleri (Definition of Done)

1. §2.2'deki sekiz kabul kriterinin tamamı sağlanıyor; §2.6 matrisi yeşil.
2. `~` ve `/tmp` altında uygulama izi yok (T13); `data/` ile birlikte başka makinede aynı durum açılıyor (T11).
3. Salt-okunur medya ve USB kopması senaryolarında veri kaybı yok (T6-T8).
4. Üç OS'te tek komutla artefakt; installer üretilmiyor; `SHA256SUMS` yayınlanıyor.
5. Runtime ≤ 70 MB, zip ≤ 45 MB; soğuk başlangıç medyanı ≤ 1,5 sn.
6. `README.md` + `README-TR.md` + `PORTABLE-README.txt` gerçeği anlatıyor; bilinen sınırlar yayınlandı.

**Başlama koşulu:** P0 çıkış kriterleri (RW-15..20 dahil) kapanmış olmalı. Tek istisna F0.3 spike'ıdır: kod değiştirmediği ve kalan 10-16 günün planını belirlediği için P0 sürerken paralel yürütülebilir. *Durum: P0'ın güvenlik ayağı (RW-01..04) tamamlandı; görünürlük ayağı (RW-15..20) açık.*

---

## 3. Faz P2 — "Editör Olma" (Hafta 6-9)

Amaç: bir yazarın **bir gün boyunca tek başına** kullanabileceği akış. P0 güveni, P1 dağıtımı kurdu; P2 yetenekleri ekler.

### 3.1 Sıra ve işler

| ID | İş | Efor | Bağımlılık | Kabul kriteri |
|---|---|---|---|---|
| RW-21 | **Ara / Değiştir** paneli (`Ctrl+F`, `Ctrl+H`), büyük/küçük harf ve tam kelime seçenekleri, "sonrakini bul", "tümünü değiştir" | L | — | 10k satırlık belgede arama <100 ms; bulunamadı durumu erişilebilir; YOLO modunda değiştirme engelli |
| RW-22 | **Belge G/Ç'sini FX thread'inden çıkar** (worker + ilerleme + iptal) | M | — | 50 MB dosyada UI donmuyor; kaydetme sırasında pencere kapanmıyor; hata diyaloğu FX thread'inde |
| RW-23 | **Dış değişiklik algılama** (mtime + boyut; isteğe bağlı SHA-256) + çakışma diyaloğu | M | RW-22 | Dosya dışarıda değiştiyse kaydetmede "üzerine yaz / vazgeç / farklı kaydet" soruluyor |
| RW-24 | **Uçtan uca çökme/kurtarma testi**: taslak yaz → süreç öldür → başlat → geri yükle → kaydet | M | P0 | Test otomatikleştirildi (ayrı JVM, kill); kurtarma yolu regresyon koruması altında |
| RW-25 | Kurtarma + ayar + yedek için **hata enjeksiyonu testleri** (disk dolu, salt-okunur dizin, fsync hatası) | M | RW-24 | Her senaryoda kullanıcıya net mesaj; veri kaybı yok |
| RW-28 | Araç çubuğu düğmelerinin YOLO'da **devre dışı** görünmesi; kısayol listesinin platforma göre `⌘`/`Ctrl` üretmesi | S | — | YOLO'da undo/redo gri; macOS'ta `⌘` |
| RW-29 | `Messages` placeholder doğrulama testi (argüman sayısı/tipi, apostrof) | S | — | Bozuk placeholder testte yakalanıyor |
| RW-30 | JaCoCo + kapsam eşiği (çekirdek servisler için %70) | S | — | CI'da rapor; eşik altı build kırıyor |

> RW-26 (release pipeline) ve RW-27 (imzalama) bu fazdan çıktı; portable karşılıkları F4.6 ve F4.3'tür (§2.5.0).

### 3.2 P2 çıkış kriterleri

1. Yeni belge → yaz → ara → değiştir → kaydet → kapat → yeniden aç akışı kesintisiz.
2. Çökme kurtarma senaryosu otomatik testle kanıtlı.
3. Büyük dosyada (≥10 MB) UI donması yok.
4. Ara/değiştir, dış değişiklik çakışması ve hata enjeksiyonu senaryoları otomatik testle kanıtlı; YOLO modunda değiştirme engelli.
5. Kapsam raporu CI'da; çekirdek servislerde regresyon görünür.

---

## 4. Faz P3 — "Ürünleşme" (Hafta 10-14)

Amaç: rakiplerle aynı masaya oturabilmek ve bakım maliyetini düşürmek.

### 4.1 Günlük kullanım özellikleri

| ID | İş | Efor | Not |
|---|---|---|---|
| RW-31 | **Son dosyalar** menüsü (son 10) + eksik dosya temizliği | M | `AppSettings`'e liste; TR/DE/FR/IT/ES metinleri |
| RW-32 | **Sürükle-bırak** ile dosya açma | S | `setOnDragOver`/`setOnDragDropped`; aynı `FileService` akışı |
| RW-33 | **Tercihler penceresi** (dil, tema, renk, yazı boyutu, ses, otomatik kayıt aralığı, toolbar) | L | Döngüsel düğmeler kalkar; erişilebilir form — **kısmen ilerledi:** döngüsel düğmeler kaldırıldı, altı ayar artık araç çubuğunda açılır liste (tek `refresh()` senkron noktası, kısayollar dahil); tercihler penceresi hâlâ açık |
| RW-34 | **Dışa aktarma**: düz metin, Markdown, PDF/HTML (yazdırma dahil) | L | JavaFX `PrinterJob` + basit HTML/MD üretimi |
| RW-35 | **Kodlama matrisini genişlet**: Windows-1252, ISO-8859-9, UTF-32, Shift-JIS | M | `TextCodec` + kodlama diyaloğu; her biri round-trip testli |
| RW-36 | **Belge başına otomatik kayıt slotu** + "kurtarılabilir taslaklar" görünümü | M | RW-04'ün üzerine |
| RW-37 | Yazdırma/dışa aktarma sırasında **YOLO ve kodlama** etkileşimlerinin netleştirilmesi | S | Dokümantasyon + test |

### 4.2 Tanılama

| ID | İş | Efor | Kazanç |
|---|---|---|---|
| RW-45 | `AppLogging`: yapılandırılabilir seviye, sürüm/OS metadata, tanılama paketi | S-M | Destek yükü düşer. Portable modda log yolu artık `data/logs` (F1.2); tanılama paketi bu dizinden toplanır |

> Paketleme ve performans işleri (eski RW-38..44) P1'e taşındı — bkz. §2.5.0.

### 4.3 P3 çıkış kriterleri

1. Yeni Belge, Ara/Değiştir, son dosyalar, sürükle-bırak, dışa aktarma çalışıyor.
2. Tercihler penceresi tüm görünür ayarları kapsıyor; döngüsel kontroller kalktı.
3. Başlangıç medyanı P1 hedefinde (≤1,5 sn) korunuyor; yeni özellikler %20'den fazla regresyon yaratmıyor.
4. `README.md` + `README-TR.md` yeni özelliklerle güncel (AGENTS.md kuralı: iki dosya aynı bölümleri aynı sırada taşır).

---

## 5. Faz P4 — "Platform" (v2 ve sonrası)

Bunlar ürünü büyütür ama P0-P3'ün yerine geçmez. Her biri ayrı bir sürüm hattı olarak ele alınmalı.

| ID | İş | Efor | Neden ertelendi |
|---|---|---|---|
| RW-46 | **Çoklu belge / sekmeler** | XL | `FileService` tek `currentFilePath`, `AutoSaveService` tek `Timeline`; yeniden tasarım gerekir (`FileService.java:53-57`) |
| RW-47 | **Depolama soyutlaması** (`DocumentStore`) + yaşam döngüsü/dispose sözleşmesi | L | Bulut/şifreli depolama ve çoklu pencere için ön koşul; portable `data/` çözümleyicisi (F1.1) bunun ilk somut adımıdır |
| RW-48 | **Yazım denetimi** (TR/EN/DE sözlükleri) | L | Dil başına sözlük ve UI maliyeti; ürün kimliği için zorunlu değil |
| RW-49 | **Tam erişilebilirlik denetimi** (NVDA/VoiceOver, klavye-only senaryolar) | M | RW-16/17 tabanı kurduktan sonra anlamlı |
| RW-50 | **TestFX/JaCoCo ile UI E2E** katmanı | L | P2'deki çökme testi çekirdeği kurduktan sonra |
| RW-51 | **ARM64** (Linux/Windows) + universal macOS artefaktı | M | Kullanıcı tabanı sinyali gelene kadar; portable artefakt adlandırması (F4.1) arch boyutunu zaten taşıyor |
| RW-52 | **Eklenti/komut paleti** veya oturum hedefleri (NaNoWriMo tarzı) | L | Konumlandırma netleştikten sonra |
| RW-53 | **Senkronizasyon / bulut** | XL | RW-47 olmadan imkânsız; stratejik karar gerektirir |

**P4 kuralı:** P4 işlerinden biri ancak P3 kapısı kapandıktan ve en az bir sürüm sahada geri bildirim topladıktan sonra başlar. RW-46 (sekmeler), RW-47 olmadan başlanırsa teknik borç ikiye katlanır.

---

## 6. Zaman Çizelgesi ve Sürüm Planı

| Sürüm | İçerik | Hedef |
|---|---|---|
| **0.9.0-beta** | P0 tamamı | Hafta 4 — genel beta |
| **0.9.1-beta** | P1 F0-F3 (veri yolu, tek örnek, runtime temizliği, FS dayanıklılığı) | Hafta 5-7 |
| **1.0.0-rc** | P1 F4-F5 (portable artefaktlar + doğrulama matrisi) | Hafta 8-9 |
| **1.0.0** | P1 kapısı kapandı + P2 çekirdeği (RW-21..25) | Hafta 11 |
| **1.1.0** | P3 (RW-31..37, RW-45) | Hafta 15 |
| **2.0.0** | P4 (RW-46/47) | Belirsiz |

**Sürüm kuralı:** `pom.xml` tek doğruluk kaynağı kalır (`package.sh:36-67`). Her sürüm tag'lenir, `SHA256SUMS` üretilir, yayın notları iki dilli (EN + TR) yazılır. 1.0.0'dan itibaren tek dağıtım biçimi portable zip/tar.gz/AppImage'dır — installer yoktur.

---

## 7. Risk Kaydı

| Faz | Risk | Olasılık | Etki | Azaltım |
|---|---|---|---|---|
| P0 | Kurtarma alt sistemi değişirken yeni veri kaybı | Orta | Yüksek | RW-24 testi P0'da başlatılır (erken); değişiklikler tek PR'da ve kill-testiyle |
| P1 | jlink + JavaFX modüler jar'larda native'ler yine çıkarılıyor | Orta | Yüksek | F0.3 spike'ı bunu **ilk gün** çürütür; başarısızsa `-Djavafx.cachedir` `data/` içine alınır. CI'da platform kırılmaları için ayrı PR + `--skip-build` geri dönüş yolu |
| P1 | macOS Gatekeeper imzasız uygulamayı engelliyor | Yüksek | Orta | Ad-hoc imza + sağ tık talimatı; notarization ayrı (ücretli) karar |
| P1 | Windows SmartScreen uyarısı | Yüksek | Düşük | "Ek bilgi → Yine de çalıştır" talimatı; imza opsiyonel |
| P1 | Linux AppImage FUSE eksikliği | Orta | Orta | `--appimage-extract-and-run` notu; ayrıca `tar.gz` yedeği |
| P1 | FAT32/exFAT'te atomik olmayan yazma | Yüksek | Yüksek | F3.1-3.2 sondası + doğrulama + zorunlu yedek + uyarı |
| P1 | Kullanıcı USB'yi kaydetme sırasında çekiyor | Orta | Yüksek | F3.3 hata yolu; belge dirty kalır |
| P1 | `data/` yazılamıyor (Program Files, ağ paylaşımı) | Orta | Orta | F1.1 fallback + görünür uyarı |
| P1 | Kullanıcılar "kurulum yok" beklentisini yanlış anlıyor | Orta | Düşük | `PORTABLE-README.txt` + ilk çalıştırma ekranı |
| P1 | Antivirüs yanlış pozitifi | Düşük | Orta | İmza (uzun vadede); checksum + kaynak derleme talimatı |
| P1 | Portable modda yeni test edilebilir sınıfların kapsamı düşük | Orta | Orta | F1/F3 birim testleri + F5 matrisi |
| P1 | Veri göçü eski ayarları bozuyor | Düşük | Yüksek | İçe aktarma kaynak dizini salt-okunur açar; kopya üzerinde çalışır |
| P2 | Ara/değiştir (RW-21) YOLO ile çakışır | Orta | Orta | Değiştirme YOLO'da devre dışı; `YoloPolicy`'ye test |
| P2 | Performans regresyonu (async I/O sonrası) | Düşük | Orta | RW-22 sonrası 10 MB/50 MB ölçüm; README'de belge boyutu beklentisi |
| Genel | Tek geliştirici → kapsam kayması | Yüksek | Yüksek | Her fazda çıkış kriteri; P4'e erken geçiş yasak |
| Genel | Çeviri kalitesi 6 dilde düşer | Orta | Orta | RW-29 placeholder testi + her yeni anahtar için 6 bundle kuralı (`MessagesTest`) |

---

## 8. Başarı Ölçütleri (KPI)

Ürünleşme "bitmiş" sayılmaz, ölçülür. İlk 3 ay için:

| Metrik | Hedef | Ölçüm |
|---|---|---|
| Çökme sonrası kurtarılan taslak oranı | %100 | RW-24 testi + saha logları |
| Kurtarma/veri kaybı şikâyeti | 0 | Issue etiketleri |
| İlk açılış → yazmaya başlama | ≤ 10 sn (hoş geldin dahil) | Elle ölçüm, 3 OS |
| İzsiz çalışma | `~` ve `/tmp` altında 0 uygulama dosyası | F0.2 iz denetimi (T13) |
| Portable artefakt boyutu | dizin ≤ 70 MB, zip ≤ 45 MB | `du -sh` / CI artefaktı |
| Başlangıç medyanı | ≤ 1,5 sn | 10 koşu, soğuk/sıcak |
| Test sayısı / kapsam | ≥ 140 test, çekirdek ≥ %70 | Surefire + JaCoCo |
| İlk 30 günde beta geri bildirimi | ≥ 20 nitelikli rapor | Issue/anket |
| Yayın artefaktı doğrulanabilirliği | checksum + tag | Release sayfası |

---

## 9. Yapılmayacaklar (Anti-Kapsam)

Bilinçli olarak **yapılmayacak** işler — kapsam kaymasını önlemek için yazılı:

- **Installer / kurulu mod** (`.deb`, `.msi`, `.dmg`, paket yöneticisi) — tek kanal portable (§2.1). İkinci bir kurulum kanalı bakım maliyetini ikiye katlar ve "sistemimde iz bırakma" garantisini çürütür.
- **Dosya ilişkilendirmesi** (`.txt`/`.md` çift tıklama, MIME kaydı, "Open with") — portable modda kayıt yazılamaz; "birlikte aç" manuel anlatılır (RW-41 kapsam dışı).
- **Otomatik güncelleme** — yalnız yeni sürüm bilgisi/sürüm notu; zip üzerine `data/` korunarak açılır.
- **Swing'e geçiş** (gerekçe: çözülebilir JavaFX problemleri — medya bağımlılığı, jlink eksikliği, boş FXML — çözülemez Swing problemleriyle takas edilir; F2.1-F2.4 bu işlerin doğru çözümü).
- **Markdown/WYSIWYG editör** — Typora/Obsidian rekabeti; ürün kimliği düz metin.
- **Plugin sistemi** — bakım yükü, güvenlik yüzeyi; P4'te bile ancak talep varsa.
- **Bulut/hesap sistemi** — "yerel-öncelikli, hesapsız" konumlandırmasıyla çelişir.
- **Mobil/web portu** — JavaFX masaüstü kimliği; ayrı ürün olur.
- **AI yazma asistanı** — ürünün "dikkat dağıtmayan" vaadine aykırı.

---

## 10. İş Paketleri ve Sahiplik (tek geliştirici varsayımıyla)

| Lane | Kapsam | Neden ayrı |
|---|---|---|
| **A — Güven** | RW-01..04, RW-20, RW-24, RW-25 | Veri yolu; en yüksek risk, en sıkı test |
| **B — Arayüz** | RW-08, RW-09, RW-15..19, RW-28, RW-33 | Görsel/erişilebilirlik; ancak A ile çakışmaz |
| **C — i18n** | RW-10..12, RW-29, yeni anahtarlar, README bakımı (RW-13 tamamlandı) | 6 bundle kuralı; B ile koordineli |
| **D — Portable çekirdek** | F1-F3 (RW-38..40, RW-42..44) | Veri yolu, kilit, runtime, FS; A ile `AtomicFiles`/`AppDirs` dosyalarında koordine olur |
| **E — Portable dağıtım** | F4-F5 (RW-14, RW-26, RW-27 karşılıkları) | Paketleme/CI/matris; D bitmeden başlamaz |
| **F — Yetenek** | RW-21..23, RW-31..37, RW-45 | Editör özellikleri; A tamamlanmadan başlamaz |

**Paralel çalışma kuralı:** B ve C aynı anda yürüyebilir. D, B/C ile paralel yürüyebilir (farklı dosyalar) ama A ile `AtomicFiles`/`AppDirs` üzerinde koordine edilir. E, D kapandıktan sonra açılır. F, A kapandıktan sonra açılır.

---

## 11. İlk Üç Adım (bugün başlanacaksa)

1. **RW-15 + RW-16 + RW-17** — araç çubuğu taşma stratejisi, tooltip + Tab erişilebilirliği, odak halkaları. P0'ın geri kalan görünür yarısı. *Durum: RW-15 + RW-16 + RW-17 tamamlandı (FR ölçüldü, tooltip/Tab erişilebilirliği ve odak halkaları geldi); P0 kapısı için kalan iş 2. adım (RW-18/19/20).*
2. **RW-18 + RW-19 + RW-20** — kalıcı görünüm durumu, otomatik kayıt göstergesi, BOM'lu yolda strict decode. P0 kapısını kapatır.
3. **P1 F0.3 jlink spike'ı (yarım gün)** — JavaFX modüllerini jlink ile runtime'a alıp iki soruyu kesin yanıtla: `~/.openjfx` oluşuyor mu, dizin ≤70 MB'a iniyor mu? Cevap, P1'in kalan 10-16 gününü ya doğrular ya `-Djavafx.cachedir` alternatifine yönlendirir. Kod değiştirmediği için P0 sürerken paralel yürütülebilir.

> İlk iki adım ~3 günde P0'ı kapatır; üçüncü adım P1'in en pahalı varsayımını ilk günden test eder.
