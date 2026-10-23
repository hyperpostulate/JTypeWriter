# JTypeWriter — Ürünleştirme Yol Haritası

> Sürüm: 1.0 · Tarih: 23 Ekim 2026 · Dayanak: `analysis.md` (bulgu dizini ve `file:line` kanıtları)
> Varsayım: 1 geliştirici, yarı zamanlı (~20 saat/hafta). Efor ölçeği: **S** ≤ 0,5 gün · **M** 1-2 gün · **L** 3-5 gün · **XL** > 1 hafta.

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
| **P1** | Editör olma | Hafta 2-4 | Günlük sürdürülebilir yazma akışı: yeni belge, ara/değiştir, çökme testi |
| **P2** | Ürünleşme | Hafta 5-10 | Yayınlanabilir 1.0: imzalı paket, tercihler, son dosyalar, performans |
| **P3** | Platform | v2 ve sonrası | Çoklu belge, depolama soyutlaması, erişilebilirlik denetimi |

**Kural:** P0 bitmeden P1'e, P1 bitmeden P2'ye geçilmez. P3 hiçbir zaman P0/P1 işlerinin önüne alınmaz.

---

## 1. Hızlı Kazanımlar — İlk 5 Gün (P0)

Tek tek ele alındığında 1-2 saatlik, toplamda ürün algısını değiştiren işler. Hiçbiri mimari değişiklik gerektirmez.

| ID | İş | Efor | Neden şimdi | Kanıt / dosya |
|---|---|---|---|---|
| RW-01 | Kurtarma diyaloğuna **"İptal / Sonra sor"** ekle; pencere kapatma taslağı silmesin | S | Ürünün tek vaadini çürüten en kısa yol | `DialogService.java`, `AutoSaveService.java` — **tamamlandı (ab6f92f):** üç seçenek (geri yükle / sil / sonra sor); kapatma = sonra sor; ertelenen taslak temiz çıkışta, yeni snapshot'larda ve belge açılışlarında korunur; `DialogServiceTest` + `AutoSaveServiceTest` regresyon testleri |
| RW-02 | Bozuk zaman damgası/metadata **taslağı düşürmesin**; metin sağlamsa teklif edilsin | S | Kurtarılabilir metni çöpe atan hata | `RecoveryStore.java:81-90` — **tamamlandı:** başlık alanları tek tek ve toleranslı çözülüyor; bozuk `saved.at` → dosya mtime, bozuk/eksik yol → `null`, bölücü/magic eksik dosya → tüm içerik metin olarak sunulur, geçersiz UTF-8 → değiştirme karakterleriyle; her durumda uyarı loglanır, metin korunur. `RecoveryStoreTest` 13 test |
| RW-03 | Draft + metadata'yı **tek atomik dosyaya** indir | S | İki yazma arası çökme yarım durum bırakıyor | `RecoveryStore.java:49-58` — **tamamlandı:** taslak tek `draft-<uuid>.jtw` dosyası (magic satırı + `saved.at` + Base64 yol + boş satır + metin), tek `AtomicFiles.writeMoving`; yarım durum fiziksel olarak imkânsız |
| RW-04 | Kurtarma dosyasını **örnek/oturum başına** ayır | S-M | İki pencere birbirinin taslağını eziyor | `RecoveryStore.java:35`, `AppDirs.java:20-23` — **tamamlandı:** oturum başına UUID'li dosya; `loadDraft` en yenisini seçer ve başka oturumun dosyasını kendi slotuna devralır (adoption), `clear()` yalnız kendi dosyasını siler. Legacy `draft.txt` göçü **bilinçli olarak yapılmadı** (henüz kullanıcı kitlesi yok) |
| RW-05 | **Yeni Belge** (`Ctrl+N`) + belge yüklerken undo geçmişini sıfırla | S | Editörün en temel eksiği; YOLO açıkken yeni belge imkânsız | `FileService.java:87-170`, `FxApplication.java:77-80` |
| RW-06 | Log formatter'a **stack trace** ekle | S | Şu an hata raporu teşhis edilemez | `AppLogging.java:37-43` |
| RW-07 | Bozuk ayarları `.corrupt` olarak **karantinaya al** | S | Sessiz üzerine yazma | `AppSettings.java:179-199` |
| RW-08 | **Uygulama ikonu** ekle (`--icon`, pencereler, görev çubuğu) | S | İlk izlenimde "gerçek uygulama" sinyali | `package.sh`, `src/main/resources` |
| RW-09 | Diyaloglara pencere **başlığı** ver; `showError`'ı ana pencereye **sahiplendir** | S | Görev değiştiricide boş isim, arkada kalan hata | `WelcomeDialog.java:66`, `AboutDialog.java:99`, `DialogService.java:203-210` |
| RW-10 | DE `toolbarRedo` → **Wiederholen**; TR hoş geldin metnini tamamla; `recoveryNo` → **Vazgeç** (TR/FR/IT) | S | Yanlış çeviri veri kaybı riskini artırıyor | `messages_de.properties:56`, `messages_tr.properties:28,66` |
| RW-11 | Dil değişiminde **pencere başlığını** yenile | S | Başlık eski dilde kalıyor | `MainController.java:35-67` |
| RW-12 | `Ctrl+Shift+T` sonrası **tema etiketini** güncelle | S | Etiket ile gerçek tema ayrışıyor | `MainController.java:123-128` vs `ToolbarView.java:189-196` |
| RW-13 | README dürüstlüğü: fat JAR platforma özgü; WAV kaynağı; macOS `⌘` | S | Yanlış vaat güven kaybı | `README.md` — **tamamlandı (17b72cf):** EN + TR eşlendi; ince/fat JAR ayrımı, fat JAR'ın platforma özgülüğü, atomik move fallback'i, BOM tabanlı kodlama tespiti, tema döngüsü, çoklu dil kapsamı, tek örnek kilidi, tam ekran/araç çubuğu, durum çubuğu boyut döngüsü, mimari ve test tabloları gerçek davranışa göre düzeltildi |
| RW-14 | `--license-file` ile kuruluma **lisans dosyası** ekle | S | GPLv3 dağıtım uyumu | `package.sh` |

**P0 ilk gün sırası:** RW-01 → RW-02 → RW-03 → RW-04 (tek oturum, kurtarma alt sistemi) → RW-05 → RW-06/07 (bağımsız, paralel) → RW-10/11/12 (i18n/UI tek oturum) → RW-08/09/14. RW-01 (kurtarma iptali), RW-02..04 (kurtarma deposu) ve RW-13 (README hizalaması) **tamamlandı**; kalan P0 işleri (RW-05..09, RW-15..20) uygulandıkça EN + TR README'ye işlenir.

### P0 kalan işler (gün 2-5) — "profesyonel görünüm"

| ID | İş | Efor | Kabul kriteri |
|---|---|---|---|
| RW-15 | Araç çubuğu taşma/sarma stratejisi; literal `Label("\|")` ayırıcıları kaldır | M | 1280 px'te DE/FR metinleri kırpılmıyor; 900 px'te taşmıyor |
| RW-16 | Tüm araç çubuğu düğmelerine **tooltip**; araç çubuğu **Tab ile gezilebilir** | M | Klavye ile aç/kaydet/tema erişilebilir; tooltip'ler bundle'dan |
| RW-17 | **Odak halkalarını** geri getir; durum metni ≥13 px ve WCAG AA kontrast | M | `base.css`'te `-fx-focus-color: transparent` kaldırıldı; 3 temada kontrast ölçüldü |
| RW-18 | Kalıcı görünüm durumu: araç çubuğu görünürlüğü, fullscreen, istatistik boyutu | S | Yeniden başlatmada korunuyor |
| RW-19 | Otomatik kayıt için görünür durum ("kaydedildi 12:04") | S | Kirli temiz geçişinde UI'da görünür |
| RW-20 | `TextCodec` BOM'lu yolda **strict decode**; başarısızsa kullanıcıya sor | S | Bozuk BOM'lu dosya sessizce `U+FFFD` üretmiyor; test eklendi |

### P0 çıkış kriterleri (Definition of Done)

1. Kurtarma: diyaloğu kapatmak, bozuk metadata, iki eşzamanlı örnek — **hiçbiri metin kaybetmiyor**; her biri için otomatik test. *(RW-01..04 tamamlandı: `DialogServiceTest`, `AutoSaveServiceTest`, `RecoveryStoreTest`; uçtan uca kill-testi RW-24'te.)*
2. `Ctrl+N` çalışıyor; yeni belgede `Ctrl+Z` önceki belgeye gitmiyor.
3. Uygulama ikonu, pencere başlıkları, tooltip'ler, odak halkaları mevcut; toolbar DE/FR'de taşmıyor.
4. 6 bundle'da çeviri hatası yok; dil değişimi tüm görünür metni yeniliyor.
5. `mvn clean test` yeşil; TR/DE locale koşuları yeşil; `package.sh --installer` çalışıyor.
6. README (EN + TR) ürünün gerçekten yaptığını anlatıyor — temel hizalama **tamamlandı**; kalan P0 işleri (RW-01..05, RW-08/09) uygulandıkça iki dilde güncellenir.

---

## 2. Faz P1 — "Editör Olma" (Hafta 2-4)

Amaç: bir yazarın **bir gün boyunca tek başına** kullanabileceği akış. P0 güveni kurdu; P1 yetenekleri ekler.

### 2.1 Sıra ve işler

| ID | İş | Efor | Bağımlılık | Kabul kriteri |
|---|---|---|---|---|
| RW-21 | **Ara / Değiştir** paneli (`Ctrl+F`, `Ctrl+H`), büyük/küçük harf ve tam kelime seçenekleri, "sonrakini bul", "tümünü değiştir" | L | — | 10k satırlık belgede arama <100 ms; bulunamadı durumu erişilebilir; YOLO modunda değiştirme engelli |
| RW-22 | **Belge G/Ç'sini FX thread'inden çıkar** (worker + ilerleme + iptal) | M | — | 50 MB dosyada UI donmuyor; kaydetme sırasında pencere kapanmıyor; hata diyaloğu FX thread'inde |
| RW-23 | **Dış değişiklik algılama** (mtime + boyut; isteğe bağlı SHA-256) + çakışma diyaloğu | M | RW-22 | Dosya dışarıda değiştiyse kaydetmede "üzerine yaz / vazgeç / farklı kaydet" soruluyor |
| RW-24 | **Uçtan uca çökme/kurtarma testi**: taslak yaz → süreç öldür → başlat → geri yükle → kaydet | M | P0 | Test otomatikleştirildi (ayrı JVM, kill); kurtarma yolu regresyon koruması altında |
| RW-25 | Kurtarma + ayar + yedek için **hata enjeksiyonu testleri** (disk dolu, salt-okunur dizin, fsync hatası) | M | RW-24 | Her senaryoda kullanıcıya net mesaj; veri kaybı yok |
| RW-26 | **Release pipeline**: tag tetiği, sürüm etiketi, SHA-256 checksum, artefakt saklama | M | — | `v1.0.0` tag'i → 3 OS artefaktı + `SHA256SUMS` |
| RW-27 | **İmzalama hazırlığı**: macOS notarization + Windows kod imzalama adımları (secrets varsa aktif) | M-L | RW-26 | Sırlar yoksa iş akışı "unsigned" olarak açıkça işaretliyor ve yayın notunda belirtiyor |
| RW-28 | Araç çubuğu düğmelerinin YOLO'da **devre dışı** görünmesi; kısayol listesinin platforma göre `⌘`/`Ctrl` üretmesi | S | — | YOLO'da undo/redo gri; macOS'ta `⌘` |
| RW-29 | `Messages` placeholder doğrulama testi (argüman sayısı/tipi, apostrof) | S | — | Bozuk placeholder testte yakalanıyor |
| RW-30 | JaCoCo + kapsam eşiği (çekirdek servisler için %70) | S | — | CI'da rapor; eşik altı build kırıyor |

### 2.2 P1 çıkış kriterleri

1. Yeni belge → yaz → ara → değiştir → kaydet → kapat → yeniden aç akışı kesintisiz.
2. Çökme kurtarma senaryosu otomatik testle kanıtlı.
3. Büyük dosyada (≥10 MB) UI donması yok.
4. Tag'li sürüm üretilebiliyor; checksum yayınlanıyor; imza durumu dürüstçe belirtiliyor.
5. Kapsam raporu CI'da; çekirdek servislerde regresyon görünür.

---

## 3. Faz P2 — "Ürünleşme" (Hafta 5-10)

Amaç: rakiplerle aynı masaya oturabilmek ve bakım maliyetini düşürmek.

### 3.1 Günlük kullanım özellikleri

| ID | İş | Efor | Not |
|---|---|---|---|
| RW-31 | **Son dosyalar** menüsü (son 10) + eksik dosya temizliği | M | `AppSettings`'e liste; TR/DE/FR/IT/ES metinleri |
| RW-32 | **Sürükle-bırak** ile dosya açma | S | `setOnDragOver`/`setOnDragDropped`; aynı `FileService` akışı |
| RW-33 | **Tercihler penceresi** (dil, tema, renk, yazı boyutu, ses, otomatik kayıt aralığı, toolbar) | L | Döngüsel düğmeler kalkar; erişilebilir form |
| RW-34 | **Dışa aktarma**: düz metin, Markdown, PDF/HTML (yazdırma dahil) | L | JavaFX `PrinterJob` + basit HTML/MD üretimi |
| RW-35 | **Kodlama matrisini genişlet**: Windows-1252, ISO-8859-9, UTF-32, Shift-JIS | M | `TextCodec` + kodlama diyaloğu; her biri round-trip testli |
| RW-36 | **Belge başına otomatik kayıt slotu** + "kurtarılabilir taslaklar" görünümü | M | RW-04'ün üzerine |
| RW-37 | Yazdırma/dışa aktarma sırasında **YOLO ve kodlama** etkileşimlerinin netleştirilmesi | S | Dokümantasyon + test |

### 3.2 Paketleme ve performans (ertelenen "orta yol" işleri)

| ID | İş | Efor | Kazanç |
|---|---|---|---|
| RW-38 | `javafx-media` → `javax.sound.sampled.Clip` | S | ~1,6 MB jar + native medya yükü kalkar; `AudioClip`'in temp dosya hilesi gerekmez |
| RW-39 | Boş `main.fxml` + `FXMLLoader` kaldır; sahne doğrudan `AppGraph`'tan kurulsun | S | `javafx-fxml` bağımlılığı ve bir dolaylılık katmanı gider |
| RW-40 | `module-info.java` + jlink ile runtime imajı | L | 134 MB runtime → hedef 50-70 MB; native çözümleme deterministik |
| RW-41 | `package.sh`: `--icon`, `--file-associations` (`.txt`, `.md`), Linux MIME, Windows "Open with" | M | Çift tıklamayla açılır |
| RW-42 | `AppDirs` platforma doğru: XDG / `%APPDATA%` / `Application Support` + **taşınabilir mod** | M | Yedekleme/senkronizasyon uyumu; eski dizinden geçiş |
| RW-43 | **Tek örnek kilidi** (single-instance) + ikinci çağrıyı mevcut pencereye yönlendirme | M | Ayar/taslak çakışması biter — **kısmen tamamlandı:** kilit + yerelleştirilmiş uyarı pop-up'ı (`InstanceLock`, `SingleInstanceWarning`); mevcut pencereye odaklanma kullanıcı kararıyla kapsam dışı |
| RW-44 | `AtomicFiles`: atomik olmayan move fallback'ini kaldır veya açıkça raporla; açılışta `jtw-*.tmp` temizliği | S | "Yarım dosya yok" garantisi gerçek olur |
| RW-45 | `AppLogging`: yapılandırılabilir seviye, sürüm/OS metadata, tanılama paketi | S-M | Destek yükü düşer |

### 3.3 P2 çıkış kriterleri (1.0 kapısı)

1. Yeni Belge, Ara/Değiştir, son dosyalar, sürükle-bırak, dışa aktarma çalışıyor.
2. Tercihler penceresi tüm görünür ayarları kapsıyor; döngüsel kontroller kalktı.
3. Runtime imajı ≤ %50 küçüldü; başlangıç medyanı regresyona uğramadı.
4. `.txt` dosyası çift tıklamayla JTypeWriter'da açılıyor (3 OS).
5. Tek örnek kilidi ve platforma doğru veri dizini doğrulandı.
6. README + Türkçe kopyası yeni özelliklerle güncel (AGENTS.md kuralı).

---

## 4. Faz P3 — "Platform" (v2 ve sonrası)

Bunlar ürünü büyütür ama P0-P2'nin yerine geçmez. Her biri ayrı bir sürüm hattı olarak ele alınmalı.

| ID | İş | Efor | Neden ertelendi |
|---|---|---|---|
| RW-46 | **Çoklu belge / sekmeler** | XL | `FileService` tek `currentFilePath`, `AutoSaveService` tek `Timeline`; yeniden tasarım gerekir (`FileService.java:53-57`) |
| RW-47 | **Depolama soyutlaması** (`DocumentStore`) + yaşam döngüsü/dispose sözleşmesi | L | Bulut/şifreli depolama ve çoklu pencere için ön koşul |
| RW-48 | **Yazım denetimi** (TR/EN/DE sözlükleri) | L | Dil başına sözlük ve UI maliyeti; ürün kimliği için zorunlu değil |
| RW-49 | **Tam erişilebilirlik denetimi** (NVDA/VoiceOver, klavye-only senaryolar) | M | RW-16/17 tabanı kurduktan sonra anlamlı |
| RW-50 | **TestFX/JaCoCo ile UI E2E** katmanı | L | P1'deki çökme testi çekirdeği kurduktan sonra |
| RW-51 | **ARM64** (Linux/Windows) + universal macOS artefaktı | M | Kullanıcı tabanı sinyali gelene kadar |
| RW-52 | **Eklenti/komut paleti** veya oturum hedefleri (NaNoWriMo tarzı) | L | Konumlandırma netleştikten sonra |
| RW-53 | **Senkronizasyon / bulut** | XL | RW-47 olmadan imkânsız; stratejik karar gerektirir |

**P3 kuralı:** P3 işlerinden biri ancak P2 kapısı kapandıktan ve en az bir sürüm sahada geri bildirim topladıktan sonra başlar. RW-46 (sekmeler), RW-47 olmadan başlanırsa teknik borç ikiye katlanır.

---

## 5. Zaman Çizelgesi ve Sürüm Planı

| Sürüm | İçerik | Hedef |
|---|---|---|
| **0.9.0-beta** | P0 tamamı + RW-21/22 (ara/değiştir, async I/O) | Hafta 4 — genel beta, DMG atlanır (jpackage `0.x` kısıtı) |
| **0.9.x** | RW-23/24/25 (çakışma + test sertleştirme) | Hafta 5-6 |
| **1.0.0-rc** | P1 tamamı + RW-38/39 (medya/FXML temizliği) | Hafta 7-8 |
| **1.0.0** | P2'nin 3.1 ve 3.2'si + 1.0 kapısı | Hafta 10 |
| **1.1.0** | RW-31/32/33/34/35 | Hafta 14 |
| **2.0.0** | RW-46/47 (çoklu belge + depolama) | Belirsiz |

**Sürüm kuralı:** `pom.xml` tek doğruluk kaynağı kalır (`package.sh:36-67`). Her sürüm tag'lenir, `SHA256SUMS` üretilir, yayın notları iki dilli (EN + TR) yazılır.

---

## 6. Risk Kaydı

| Risk | Olasılık | Etki | Azaltım |
|---|---|---|---|
| P0'da kurtarma alt sistemi değişirken yeni veri kaybı | Orta | Yüksek | RW-24 testi P0'da başlatılır (erken); değişiklikler tek PR'da ve kill-testiyle |
| jlink (RW-40) CI'da platform kırılmaları | Orta | Orta | RW-38/39 önce; jlink ayrı PR, `--skip-build` ile geri dönüş yolu |
| İmzalama sırları yok | Yüksek | Orta | RW-27 "unsigned" işaretleme ile yayın; sırlar gelince aynı iş akışı aktive olur |
| Ara/değiştir (RW-21) YOLO ile çakışır | Orta | Orta | Değiştirme YOLO'da devre dışı; `YoloPolicy`'ye test |
| Tek geliştirici → kapsam kayması | Yüksek | Yüksek | Her fazda çıkış kriteri; P3'e erken geçiş yasak |
| Çeviri kalitesi 6 dilde düşer | Orta | Orta | RW-29 placeholder testi + her yeni anahtar için 6 bundle kuralı (`MessagesTest`) |
| Performans regresyonu (async I/O sonrası) | Düşük | Orta | RW-22 sonrası 10 MB/50 MB ölçüm; README'de belge boyutu beklentisi |

---

## 7. Başarı Ölçütleri (KPI)

Ürünleşme "bitmiş" sayılmaz, ölçülür. İlk 3 ay için:

| Metrik | Hedef | Ölçüm |
|---|---|---|
| Çökme sonrası kurtarılan taslak oranı | %100 | RW-24 testi + saha logları |
| Kurtarma/veri kaybı şikâyeti | 0 | Issue etiketleri |
| İlk açılış → yazmaya başlama | ≤ 10 sn (hoş geldin dahil) | Elle ölçüm, 3 OS |
| Runtime imajı | ≤ 70 MB | `du -sh` CI artefaktı |
| Başlangıç medyanı | ≤ 1,5 sn | 10 koşu, soğuk/sıcak |
| Test sayısı / kapsam | ≥ 140 test, çekirdek ≥ %70 | Surefire + JaCoCo |
| İlk 30 günde beta geri bildirimi | ≥ 20 nitelikli rapor | Issue/anket |
| Yayın artefaktı doğrulanabilirliği | checksum + tag | Release sayfası |

---

## 8. Yapılmayacaklar (Anti-Kapsam)

Bilinçli olarak **yapılmayacak** işler — kapsam kaymasını önlemek için yazılı:

- **Swing'e geçiş** (gerekçe: çözülebilir JavaFX problemleri — medya bağımlılığı, jlink eksikliği, boş FXML — çözülemez Swing problemleriyle takas edilir; RW-38/39/40 bu işlerin doğru çözümü).
- **Markdown/WYSIWYG editör** — Typora/Obsidian rekabeti; ürün kimliği düz metin.
- **Plugin sistemi** — bakım yükü, güvenlik yüzeyi; P3'te bile ancak talep varsa.
- **Bulut/hesap sistemi** — "yerel-öncelikli, hesapsız" konumlandırmasıyla çelişir.
- **Mobil/web portu** — JavaFX masaüstü kimliği; ayrı ürün olur.
- **AI yazma asistanı** — ürünün "dikkat dağıtmayan" vaadine aykırı.

---

## 9. İş Paketleri ve Sahiplik (tek geliştirici varsayımıyla)

| Lane | Kapsam | Neden ayrı |
|---|---|---|
| **A — Güven** | RW-01..04, RW-20, RW-24, RW-44 | Veri yolu; en yüksek risk, en sıkı test |
| **B — Arayüz** | RW-08, RW-09, RW-15..19, RW-28, RW-33 | Görsel/erişilebilirlik; ancak A ile çakışmaz |
| **C — i18n** | RW-10..12, RW-29, yeni anahtarlar, README bakımı (RW-13 tamamlandı) | 6 bundle kuralı; B ile koordineli |
| **D — Dağıtım** | RW-14, RW-26, RW-27, RW-38..43, RW-51 | Paketleme/CI; A ve B'den bağımsız ilerleyebilir |
| **E — Yetenek** | RW-21..23, RW-31..37 | Editör özellikleri; A tamamlanmadan başlamaz |

**Paralel çalışma kuralı:** A ve D aynı anda yürüyebilir (farklı dosyalar). B ve C aynı anda yürüyebilir. E, A kapandıktan sonra açılır.

---

## 10. İlk Üç Adım (bugün başlanacaksa)

1. **RW-01 + RW-02 + RW-03** — kurtarma alt sistemi tek oturumda; ardından RW-24'ün ilk sürümü (kill-testi) yazılır. Bu, ürünün en büyük riskini kapatır ve regresyon koruması kurar. *Durum: RW-01..04 tamamlandı; sırada RW-24 (uçtan uca kill-testi).*
2. **RW-05 + RW-06 + RW-07** — bağımsız, düşük riskli; ilk gün içinde üç görünür kazanç.
3. **RW-08 + RW-09 + RW-10/11/12** — "profesyonel görünüm" partisi; ilk ekran görüntüsü yayınlanabilir hale gelir.

> Bu üç adım ~3 günde tamamlanır ve P0'ın en görünür %60'ını kapatır.
