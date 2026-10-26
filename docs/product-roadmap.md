# JTypeWriter — Ürünleştirme Yol Haritası

> **Sürüm:** 2.0 · **Tarih:** 25 Ekim 2026
> **Dayanak:** `docs/analysis.md` (HEAD `d7bf8da`, **116 bulgu kimliği** — I4'ün 10 alt maddesi
> ayrı sayılırsa 126 — ve 9 kapanan bulgu; her madde `dosya:satır` kanıtına bağlıdır)
> **Varsayım:** 1 geliştirici, yarı zamanlı (~20 saat/hafta). Efor ölçeği: **S** ≤ 0,5 gün ·
> **M** 1–2 gün · **L** 3–5 gün · **XL** > 1 hafta.
> **Başlangıç noktası:** P0 fazı **kapalıdır** (§0.3). Bu belge P1 ile başlar.

---

## 0. Yol Haritasının Mantığı

### 0.1 Bu sürümde ne değişti

Önceki yol haritası (v1.1) **"yalnız portable" dağıtım kararına** dayanıyordu ve P1'in tamamını
o karara adıyordu (veri yolu taşıma, runtime temizliği, izsizlik).

**Bu karar ertelendi.** Kullanıcı kararı: *"portable kararını şu an için göz ardı et; ürün
yeterince olgunlaştıktan sonra o kararı vereceğim."* Yol haritası buna göre yeniden kuruldu:

| Eskiden | Şimdi |
|---|---|
| P1 = portable kanalının inşası | P1 = **görünür kalite + güven + yayın bütünlüğü** (kanal-nötr) |
| Dağıtım kararı önkoşul | Dağıtım kararı **P3'te bir ADR kapısı** (§4.4) |
| `--installer` yolu kaldırılacaktı | `--installer` **olduğu gibi kalır**; yalnız belgeler gerçeği söyler |
| jlink/P1'de zorunlu | jlink **P3'e** taşındı (kanal kararına bağlı) |

Sonuç: P1 artık bir mimari bahis değil, **ölçülebilir kazanç** turu. Bu, "en az eforla en çok
fayda" hedefiyle de uyumlu — çünkü portable işi pahalıydı (11–17 iş günü) ve getirisi
kullanıcıya *görünmüyordu*.

### 0.2 Pareto kuralı — sıralama nasıl hesaplandı

Her iş şu skorla sıralandı:

```
Öncelik = (Kullanıcı Etkisi × Güven) / Efor
```

- **Kullanıcı Etkisi (1–5):** veri kaybı / uygulamanın açılmaması / kaydedememek = **5**;
  görünen ve rahatsız eden kusur = **4**; kozmetik ama fark edilir = **3**; konfor = **1–2**.
- **Güven (1–3):** bulgu doğrulandı ve çözüm tek dosyada net = **3**; çözüm net ama yan etki
  riski var = **2**; keşif gerekiyor = **1**.
- **Efor (puan):** S = 1 · M = 2 · L = 3 · XL = 5.

**Sonuç — dağılım çarpık:** skoru ≥ 12 olan **19 işin tamamı S eforundadır**. Yani ürünün
algılanan kalitesini belirleyen işlerin çoğu *yarım günlük*. Bu, ilk dalganın neden 3 gün
sürdüğünü açıklıyor (§2).

### 0.3 P0 kapısı — kapalı

§0.2'nin aksine, Pareto **kapıları** ezmez. İki iş skoru düşük ama **geri dönüşsüz** olduğu
için sıralamadan bağımsız olarak P1'de kalır:

| İş | Skor | Neden yine de P1 |
|---|---:|---|
| V2 — kapatma isteği dışı çıkışta taslak silinmesi | 5 | Kaybedilen metin geri gelmiyor; kullanıcı sebebini asla öğrenemiyor |
| V4 — otomatik kaydın sessizce ölmesi | 5 | Ürünün tek vaadini sessizce iptal ediyor; arayüz hiçbir şey söylemiyor |

P0'ın çıkış kriterleri (`docs/analysis.md` §12.1) karşılanmış durumda: atomik yazma, BOM'da
sıkı çözme, ayar karantinası, toleranslı taslak ayrıştırma, "sonra sor" park etme, araç çubuğu
taşma stratejisi, tooltip/Tab/odak halkaları ve 218 yeşil test. **P0 yeniden açılmaz.**

### 0.4 Faz haritası

| Faz | Tema | Süre | Çıkış kapısı |
|---|---|---|---|
| **P0** | Güven + ilk izlenim | ✅ kapalı | — |
| **P1** | **Görünür kalite + güven + yayın bütünlüğü** | Hafta 1–4 (~18 iş günü) | "Bu bir ürün" dedirten 3 dakika + hiçbir yol sessizce veri kaybetmiyor + doğrulanabilir bir sürüm var |
| **P2** | **Editör olma** | Hafta 5–9 | Günlük yazma akışı: ara/değiştir, CLI ile aç, yazdır, dış değişikliği yakala, büyük belgede donma yok |
| **P3** | **Ürünleşme + kanal kararı** | Hafta 10–15 | Tercihler, dışa aktarma, tanılama + **dağıtım kanalı ADR'i** |
| **P4** | Platform (v2) | Belirsiz | Çoklu belge, sanallaştırılmış editör, modüler paketleme |

**Kural:** P1 bitmeden P2'ye, P2 bitmeden P3'e geçilmez. P4 hiçbir zaman P1/P2/P3 işlerinin
önüne alınmaz.

---

## 1. P1 Öncesi: Neden Bu Sıra?

`docs/analysis.md` §10.1'in bulgusu tek cümleyle şu: **motor ~3,8/5, deneyim ~2,5/5, ambalaj
~2,0/5.** P1 tam olarak bu iki zayıf halkaya nişan alıyor. Üç gerekçe:

1. **Görünür kalite ucuz.** Varsayılan tema **Dark** (`AppSettings.java:106-108`) ve o temada
   "kaydedildi" etiketi **1,32:1** kontrastla çiziliyor — yani ürünün **ilk açılışta** verdiği
   en önemli sinyal okunamıyor. Sebep tek bir eksik CSS kuralı (`StatsBar.java:63`).
   Aynı sınıfta: 5 metin rengi açık/sepya'da AA altı, karşılama/hakkında diyalogları 768 px
   ekranı aşıyor, kısayol bloğu 4 dilde kırpılıyor, açılışta editöre odak verilmiyor.
   **Hepsi S eforu, hepsi ilk 10 dakikada fark edilir.**

2. **Güven açıkları hâlâ pahalı sonuçlar üretiyor.** P0 kurtarma *altyapısını* kurdu, ama
   çıkış yolunu (`V2`), kilit sözleşmesini (`V1`), kurtarma sonrası kodlamayı (`V6`) ve yedek
   sırasını (`V7`) kapatmadı. Bunların dördü de tek dosyalık düzeltme; biri bile sahada
   yaşanırsa P0'ın tamamı itibar olarak kaybedilir.

3. **Yayın bütünlüğü kanal kararından bağımsız.** Tag, checksum, `LICENSE`, `THIRD-PARTY-
   NOTICES`, POM metadata, CI izinleri — bunlar ister portable ister installer seçilsin
   **aynı**. Kanal kararını beklemenin maliyeti yok, ama bu işleri ertelemenin maliyeti var:
   `LICENSE` bugün hiçbir artefaktta yok (`B28`) ve `.deb` telifi `License: Unknown` yazıyor.

---

## 2. Faz P1 — Görünür Kalite, Güven, Yayın Bütünlüğü

### 2.1 P1.0 — Üç Günlük Kazanım Dalgası (Hafta 1)

Amaç: **hiçbir mimari değişiklik yapmadan**, üç gün sonunda ürünün "amatör" görünmesine yol
açan tüm sinyalleri silmek. Kural: bu dalgada hiçbir iş **yarım günü** geçmez; geçiyorsa
kapsam yanlış kesilmiştir.

| ID | Efor | Skor | İş | Kaynak | Kabul kriteri |
|---|---|---:|---|---|---|
| ✅ PW-01 | S | **15** | **`InstanceLock`'u üç durumlu yap.** `acquired` / `already-running` / `unavailable`. Yalnız `already-running` engeller; `unavailable` uyarı loglayıp devam eder. | V1 | Salt-okunur ev dizininde uygulama **açılır**; ikinci örnek hâlâ engellenir; `InstanceLockTest` üç durumu da doğrular |
| ✅ PW-02 | S | **15** | **`restoreRecovered` kodlamayı korusun.** `decode == null` (legacy) dalında dosyanın gerçek kodlaması korunur; sessiz UTF-8'e düşmez. Testi **ASCII olmayan** legacy içerikle yazılır. | V6 | windows-1254 bir belge taslaktan geri yüklenip kaydedildiğinde bayt-özdeş kalır; test legacy dalı **gerçekten** çalıştırır (`analysis.md` §4.2/V6'nın "test bu hatayı göremez" notu kapanır) |
| ✅ PW-03 | S | **15** | **Yedek hatası kaydı iptal etmesin.** Sıra `AtomicFiles.writeMoving` → **sonra** `backupKeeper.backup`; yedek hatası loglanır, kayıt başarılı sayılır. Rotasyon, yeni yedek başarılı olduktan sonra yapılır. | V7, V14 | Disk dolu benzeri hata enjekte edildiğinde dosya **yazılır**; en eski yedek nesli boşuna silinmez |
| ✅ PW-04 | S | **12** | **`MessageFormat` apostrof düzeltmesi** (EN/FR/IT) + **6 dilde biçimlendirme testi**: her argümanlı anahtar, her dilde, gerçek argümanlarla biçimlendirilir; sonuçta `{n}` kalıntısı aranmaz. | I1, I3 | `encodingMalformedContent` EN/FR/IT'de `{0}` yerine charset adını **yazar**; test bu sınıfı kalıcı olarak kapatır |
| ✅ PW-05 | S | **12** | **`statsFormat` arayüz dilini kullansın** (JVM varsayılanı değil). Aynı geçiş `overwriteContent`, `recoveryContent`, `encodingUpgradeContent`, `errorLogged`, `savedStatus` için de yapılır. | I2, I4 | TR arayüzde sayılar `1.234` (virgül değil nokta); 6 dilde sayı biçimi UI diliyle uyumlu |
| ✅ PW-06 | S | **12** | **`.stats-save` için gerçek CSS kuralı** (3 temada) + `ContrastRatioTest`'e ekleme. | U1 | Koyu temada kontrast **≥ 4,5:1**; test eşiğin altına düşüşte **kırılır** |
| ✅ PW-07 | S | **12** | **5 metin rengini açık/sepya için koyulaştır** (`TextColor` sabitleri) + kontrast testi. | U2 | 5 renk × 2 tema = 10 kombinasyon **≥ 4,5:1**; koyu tema değişmez |
| ✅ PW-08 | S | **12** | **`Alert` diyalogları temayı alsın.** `DialogService`, `CrashHandler`, `SingleInstanceWarning` → `getDialogPane().getStylesheets()` sahneden kopyalanır (mevcut `WelcomeDialog` deseni). | U3 | Koyu/sepya temada hata diyaloğu artık beyaz kutu değil; tek bir yardımcı metot, üç çağrı |
| ✅ PW-09 | S | **12** | **Açılışta editöre odak.** Karşılama modali kapandıktan sonra `editorArea.requestFocus()`; araç çubuğu gizlenince odak editöre taşınır. | U7, U8 | Açılıştan sonra yazılan ilk harf belgeye gider; `Ctrl+T` sonrası odak görünmez düğümde kalmaz |
| ✅ PW-10 | S | **12** | **Kirli durumu O(1) yap.** `FileService`'te `dirty` bayrağı; `hasUnsavedChanges()` tam `String.equals` yapmaz; başlık yalnız dirty↔clean geçişinde ve `Objects.equals` korumasıyla yazılır. | M4 | 50 MB belgede tuş başına tam tarama **yok**; `FileServiceTest` + `MainControllerTest` davranışı korur |
| ✅ PW-11 | S | **12** | **Dinleyici bildirimi `try` dışına.** `savedContent` güncellendikten sonra dinleyici hatası "kayıt başarısız"a dönüşmesin. | V8 | İstisna atan bir `Listener` ile dosya yazılır, çağıran `Optional.of(path)` alır, hata loglanır |
| ✅ PW-12 | S | **12** | **CI tedarik zinciri sertleştirmesi:** action'ları commit SHA'sı ile pinle, `permissions`'ı `contents: read`'e indir, `persist-credentials: false`, `upload-artifact`'a `retention-days`. | B22, B23, B26 | Workflow yeşil; `contents: write` yalnız rozet işinde ve yalnız o adımda |
| ✅ PW-13 | S | **12** | **`LICENSE` ikiliye girsin.** `package.sh`'a jpackage `--license-file`; JAR'a `META-INF/LICENSE`; app-image köküne `LICENSE` kopyası. | B28 | Üretilen her artefaktta lisans metni (Apache-2.0) **var**; `.deb` telifi `License: Unknown` demiyor |
| ✅ PW-14 | S | **9** | **Kısayol bloğu sarsın + diyaloglar ekrana sığsın.** `setWrapText(true)`; karşılama/hakkında yüksekliği ekran görsel sınırına kelepçelenir ve `setResizable(true)`. | U4, U5 | DE/FR/IT/ES metinleri kırpılmıyor; 768 px ekranda hiçbir pencere taşmıyor |
| ✅ PW-15 | S | **9** | **CTA/hover/e-posta/tagline kontrastı.** Tek token değişikliği + 3 tema. | U6 | Karşılama CTA ≥ 4,5 (3 temada), hover ≥ 4,5, About e-postası ≥ 4,5; `ContrastRatioTest` kapsar |
| ✅ PW-16 | S | **9** | **`About` diyaloğuna sürüm + telif + garanti reddi**; sürüm `pom.properties`'ten okunur. | F11, B30 | About'ta `1.0.0` görünür; Apache-2.0 bildirimi ve "no warranty" satırı var — hata bildirimi yapılabilir hale gelir |
| ✅ PW-17 | S | **9** | **`Alert` düğmelerine `ButtonData`** (OK → `OK_DONE`, İptal → `CANCEL_CLOSE`). | U11 | Enter = varsayılan, Escape = iptal; 7 diyalogda tek tek doğrulanır |
| ✅ PW-18 | S | **9** | **Tema döngüsü tutarlılığı.** Karşılama metinleri gerçek sırayı anlatsın **veya** varsayılan `LIGHT` olsun — biri seçilir, ikisi birlikte kalmaz. | U14 | README + 6 bundle + `Theme.next` aynı şeyi söyler |
| ✅ PW-19 | S | **9** | **Rozet betiğini güvenli hale getir:** repo adı tek yerden, token URL'de değil, hata `exit 1`; rozet **tüm locale koşularının toplamını** yayınlar. | B24 | Publish hatası sessizce yutulmaz; rozet tek locale sonucu değil toplamı gösterir |
| ✅ PW-20 | S | **7** | **`StatsBar` ilk karede dolsun + dil değişiminde anında yenilensin.** Kurucuda `update()`; locale dinleyicisi. | U9, U10 | Açılışın ilk saniyesinde durum çubuğu dolu; dil değişimi ≤ 1 kare |
| ✅ PW-21 | S | **6** | **Ölü i18n API'sini sil:** 7 ölü anahtar × 6 bundle, 8 ölü getter, `Language.toggle`. | M12, I5 | `MessagesTest` hâlâ yeşil; paket başına anahtar sayısı 93 → 86 (PW-16 dört `about*` anahtarı ekleyip `aboutAuthor`'ı kaldırdığı için taban 90 değil 93) |
| ✅ PW-22 | S | **6** | **Karşılama kısayol listesi gerçeği söylesin:** `Ctrl+S` = Kaydet, `Ctrl+Shift+S` = Farklı Kaydet eklensin, `Ctrl+Shift+Z` listeye girsin. | U23, I4 | Liste `KeyboardShortcutHandler` ile birebir eşleşir; `MessagesTest` bunu sabitler |
| ✅ PW-23 | S | **6** | **POM metadata + `.gitattributes` + `outputTimestamp`.** | B33, B6, B5 | `mvn help:effective-pom` lisans/developer/scm gösterir; Windows'ta CRLF script kırmaz |
| ✅ PW-24 | S | **6** | **`ContrastRatio` test kapsamına taşınsın**; `-fx-letter-spacing` / `-fx-transition` gibi geçersiz özellikler silinsin. | M13, U17 | Üretim JAR'ında test yardımcısı yok; CSS'te yok sayılan özellik kalmaz |
| ✅ PW-25 | S | **6** | **`NaN`/`Infinity` geometri reddedilsin.** `optionalDouble` sonlu olmayan değeri varsayılana düşürür. | V15 | Bozuk `window.width=Infinity` açılışı bozmaz |

> ✅ = tamamlandı (kod + test). İşaretlenen satırların kabul kriterleri `mvn clean test` ve
> TR/DE locale koşularında doğrulanmıştır. Proje lisansı GPLv3'ten Apache-2.0'a taşındı
> (README, LICENSE, NOTICE, jar/deb üst verisi); bu nedenle PW-35 kapsam dışıdır ve PW-37
> Apache-2.0 SPDX tanımlayıcısını kullanmalıdır.

**Dalga bittiğinde ne değişir (ölçülebilir):**

| Ölçüt | Önce | Sonra |
|---|---:|---:|
| Varsayılan temada okunamayan durum etiketi | 1 (1,32:1) | **0** |
| AA altı metin kombinasyonu (açık+sepya) | 10/10 | **0/10** |
| 768 px ekranda taşan diyalog | 2 | **0** |
| Uygulamanın açılmadığı ortam | salt-okunur ev dizini | **yok** |
| Kurtarma sonrası sessiz kodlama değişimi | var | **yok** |
| Yedek hatası yüzünden kaydedilemeyen belge | var | **yok** |
| Artefaktta lisans | yok | **var** |
| EN/FR/IT'de bozuk güvenlik diyaloğu | 3 dil | **0** |

> **25 iş, hepsi S, toplam ~5–6 iş günü.** Pareto'nun asıl iddiası bu: ürünün "amatör"
> görünmesine yol açan sinyallerin tamamı burada ve hepsi yarım günün altında.

### 2.2 P1.1 — Veri Güvenliği Kapısı (Hafta 2)

PW-01/02/03/11 dalgada kapandı; burada **geri dönüşsüz** kalanlar ve onları kanıtlayan testler
var. Bu fazın çıkışı bir **kapıdır**: kapanmadan P1 ilerlemez.

| ID | Efor | Skor | İş | Kaynak | Kabul kriteri |
|---|---|---:|---|---|---|
| ✅ PW-26 | M | **5 → kapı** | **Çıkış yolu güvene alınsın.** `Application.stop()` override edilir; `shutdown()` yalnız kapatma isteğinden sonra taslağı siler; çıkış isteği görülmemişse `WINDOW_HIDDEN` **silmez, park eder**. | V2 | `Platform.exit()` / oturum kapanışı / `stage.hide()` yollarının hiçbiri kirli taslağı sormadan silmez; kirli belge varsa taslak **yaşar** |
| ✅ PW-27 | M | **5 → kapı** | **Otomatik kayıt sessizce ölmesin.** Üç arıza yolu (`park` başarısız, `saveDraft` başarısız, `WINDOW_HIDDEN` sonrası) görünür duruma gelir: durum çubuğunda kalıcı uyarı + ilk seferde diyalog. | V4 | Kurtarma dizini salt-okunur yapıldığında kullanıcı **"otomatik kayıt çalışmıyor"** uyarısı görür; test bunu `StatsBarModel` uyarı metni ve `AutosaveHealth` geçişleri üzerinden doğrular |
| ✅ PW-28 | S | **12** | **`RecoveryStore.clear()` hatası görünür olsun** (FINE → WARNING + durum). | V17 | Silinemeyen taslak sessizce hayatta kalmaz; sonraki açılışta "daha yeni dosyanın üzerine yaz" senaryosu engellenir. Uygulama: `clear()` `false` döner ve `WARNING` yazar, `AutosaveHealth.DRAFT_DELETE` durum çubuğunda kendi metnini gösterir; ayrıca diskteki belge taslaktan kesinlikle yeniyse taslak **yolu olmadan** geri yüklenir, böylece kaydetme Farklı Kaydet'e zorlanır |
| ✅ PW-29 | S | **9** | **`AppLogging.init()` çökme güvenli olsun:** `SecurityException` yakalanır, `initialized` iş **sonunda** atanır. | V18 | Sandbox ev dizininde uygulama açılır (konsol loguna düşer) |
| ✅ PW-30 | S | **9** | **`SecurityException` + salt-okunur dizin testleri** (`InstanceLock`, `AppLogging`, `RecoveryStore`). | V1, V18 | Üç sınıf için hata enjeksiyon testi; CI'da yeşil. JDK 25'te `SecurityManager` kaldırıldığı için `SecurityException` doğrudan enjekte edilemiyor: `AppLogging` reddedilen ev dizinini bir `Supplier` ile (gerçek `SecurityException`) ve kapanmış dosya sistemini, `InstanceLock` ve `RecoveryStore` salt-okunur dizini (POSIX, root'ta atlanır) kullanır |
| ✅ PW-31 | M | **6** | **Gerçek süreç öldürme testi.** Uygulama fork edilir, yazı yazdırılır, `SIGKILL` ile öldürülür, yeniden başlatılır, taslak **kurtarılır**. CI'da Linux leg'inde koşar. | §9.5/1 | Test kırmızıya çevrilebilir (kurtarmayı kapatınca kırılır); tek senaryo, uçtan uca. Uygulama: `ProcessCrashRecoveryTest` + `CrashHarness` gerçek servis grafiğini (`AppSettings → … → AutoSaveService`) ekransız bir alt JVM'de kurar; GUI katmanı fork edilmez çünkü test edilen garanti kalıcılık yoludur ve GUI yalnızca aynı metodları çağırır. Yanlışlanabilirlik iki yönlü kanıtlandı: `snapshot()` kaldırılınca test `NO_DRAFT` ile kırılır, yanıtlanmış çıkışta `shutdown()` kaldırılınca ikinci test kırılır |
| ✅ PW-32 | S | **9** | **Kodlama matrisi testi:** BOM'suz UTF-16, legacy windows-1254, karışık; aç→düzenle→kaydet→bayt karşılaştır. | V6 | Her kodlama için gidiş-dönüş bayt-özdeş; UTF-16 vakası sessizce bozulmuyorsa **uyarır**. Uygulama: `EncodingMatrixTest` (11 test) dört kodlama satırını aç→kaydet ve aç→düzenle→kaydet olarak bayt karşılaştırır. Vakanın ikinci yarısı (BOM'suz UTF-16 geçerli UTF-8 sayılıp NUL'larla açılıyor, ilk kayıtta UTF-8'e çevriliyordu) `TextCodec.decode`'da bayt-parite tespitiyle kapatıldı: bir paritenin baytları ≥%80 `0x00`/`0x01`, diğeri ≤%10 ise dosya BOM'suz UTF-16LE/BE olarak sıkı çözülür ve aynı baytlarla geri yazılır; Türkçe UTF-16 geçerli UTF-8 olmadığı için kural UTF-8 geçerliliğine bağlı değildir. Her iki paritede NUL içeren metin (U+0100–U+01FF) bilinçli olarak talep edilmez, eski kodlama sorusuna düşer |

**P1.1 çıkış kriterleri (kapı):**

1. Kapatma isteği dışı hiçbir çıkış yolu veri kaybettirmiyor — `PW-26` + test.
2. Otomatik kayıt kapalıysa arayüz bunu söylüyor — `PW-27` + test.
3. `kill -9` sonrası belge kurtarılıyor — `PW-31` yeşil.
4. Dosya kodlaması hiçbir yolda sessizce değişmiyor — `PW-02` + `PW-32`.
5. `mvn clean test` ve TR/DE locale koşuları yeşil.

### 2.3 P1.2 — Yayın Bütünlüğü: Kanal-Nötr (Hafta 3)

Bu blok **dağıtım kanalı kararını beklemez**. Ne portable ne installer seçilmiş olsun, aşağıdaki
her madde aynı şekilde gereklidir.

| ID | Efor | Skor | İş | Kaynak | Kabul kriteri |
|---|---|---:|---|---|---|
| ✅ PW-33 | M | **7,5** | **Tag → GitHub Release.** `v*` tag'inde workflow, üç OS artefaktını **Release**'e yükler (Actions artefaktı değil) ve `SHA256SUMS` üretir. | B9, B11 | `v1.0.0` tag'i Release üretir; indirilen her dosya checksum ile doğrulanır; 90 günlük silinme sorunu biter. Uygulama: `.github/workflows/release.yml` + `scripts/release-assets.sh` + `scripts/checksums.sh`. `prepare` işi tag'i `pom.xml` sürümüyle karşılaştırır ve uyuşmazsa üç OS build'i başlamadan durur. "Üç OS artefaktı" **üç dosya değil**, üç işletim sisteminin artefaktları olarak uygulandı: kanal kararı açık olduğu için (bkz. §2.3 girişi) her OS hem installer'ı hem taşınabilir `.tar.gz`'yi yayınlar, adlandırma `JTypeWriter-<sürüm>-<os>-<arch>` olarak normalleştirilir, yanına `git archive` kaynak arşivi eklenir. Release job'ı yedi varlığı ve tek bir `SHA256SUMS`'ı **ada göre** doğrular; sessizce düşen bir Windows/macOS ayağı işi kırmızı yapar. Actions upload'ı yalnızca job'lar arası taşıma (1 gün); kalıcı kopya Release varlığı. `contents: write` yalnız `release` job'ında, action'lar `maven.yml`'deki pinli SHA'lar |
| ✅ PW-34 | S | **9** | **`THIRD-PARTY-NOTICES`** dosyası: JavaFX (GPLv2+CE), GStreamer-lite/FFmpeg plugin'leri, JUnit/Mockito. Artefakta girer. | B31 | JAR'da `META-INF/THIRD-PARTY-NOTICES`; app-image kökünde de var. Uygulama: kök `THIRD-PARTY-NOTICES` (158 satır) + `legal/GPL-2.0-with-classpath-exception.txt` ve `legal/LGPL-2.1.txt` (upstream'den bayt-özdeş), `META-INF/licenses/` altında; `PackagingNoticesTest` (6 test) yolu ve içerik sözleşmesini kilitler. Araştırma üç roadmap varsayımını düzeltti: **FFmpeg paketlenmiyor** (`libavplugin*.so` Oracle'ın GStreamer adaptörleri, gerçek libav kütüphaneleri sistemden `dlopen`; jar'da FFmpeg baytı yok), GStreamer-lite tek bir `libgstreamer-lite.so` ve **LGPL-2.1-or-later** (içinde KISS FFT, `dbesi0.c`, `gsturi.c` ayrı satır), JUnit/Mockito yalnız test kapsamında ve artefaktta yok. Ayrıca `NOTICE`'ın ve deb DEP-5 şablonunun "JavaFX bildirimleri `lib/runtime/legal/` altında" iddiası yanlıştı (o dizinde yalnız JDK modülleri var) — ikisi de düzeltildi |
| ✅ PW-35 | S | **9** | **~~GPLv3 §6 kaynak teklifi~~** — Apache-2.0'a geçişle **kapsam dışı**; kaynak arşivini yayınlamak artık yasal zorunluluk değil (isteğe bağlı, PW-33 kapsamında düşünülebilir). | B29 | Kapsam dışı (Apache-2.0) |
| ✅ PW-36 | M | **7,5** | **`keystroke.wav` kaynağını çöz.** Ya kendi üretimin olduğu belgelenir (üretici script + `NOTICE`), ya CC0/benzeri bir dosyayla değiştirilir. | B32 | Dosyanın lisansı repoda **yazılı**; belirsizlik sıfır — bu, yayın öncesi hukuki kapı. Uygulama: `src/main/resources/audio/PROVENANCE.md` (yazar `Mesut ORMANLI, 2026`, Audacity'de elle üretim, üçüncü taraf örnek yok, Apache-2.0, dosya kimliği + SHA-256) + `NOTICE` paragrafı + iki README'nin lisans paragrafında birer cümle; `AudioAssetTest` (5 test) WAV'ı ayrıştırıp beyandaki format/süre/hash ile karşılaştırır. **Bilinçli sapma:** script yok, uydurulmadı — dosya elle yapıldığı için yeniden üretilebilirlik iddia edilmiyor; yerine SHA-256 sözleşme görevi görüyor ve iki yönlü kaymayı (ses değişir/beyan kalır, beyan değişir/ses kalır) test yakalıyor. Kabul kriteri karşılanır: lisans ve kaynak yazılı, belirsizlik sıfır; beyan jar içinde `audio/PROVENANCE.md` olarak baytlarla birlikte gider |
| ✅ PW-37 | S | **9** | **SPDX başlıkları** (43 dosya, tek geçiş) + ikon/artwork lisans notu. | B34, B35 | Her `.java` dosyasında `SPDX-License-Identifier: Apache-2.0`; `NOTICE` ikonu kapsar. Uygulama: dosya sayısı 43 değil **91**'di (51 main, 40 test) - hepsine dosyanın ilk baytından itibaren lisans bloğu eklendi; `git diff -U0` 91 adet `@@ -0,0 +1,5 @@` (tek hunk, sıfır silme, yeniden biçimlendirme yok). `jtypewriter.svg`'nin mevcut tasarım-anlatımı bloğuna yazar/lisans ve "PNG/ICO/ICNS bu dosyadan üretilir" notu, `NOTICE`'a ikon paragrafı. `LicenseHeaderTest` (3 test) başlığı byte 0'da arar ve **tüm** eksikleri tek mesajda toplar - checkstyle/spotless bilinçli olarak yok, bu yüzden lisans eksiği inceleme notu değil derleme hatası olarak düşer. Kapsam yalnızca `.java` + SVG; `.fxml`/`.css`/`.properties` kendi yorum sözdizimlerini gerektirdiği için dışarıda |
| ✅ PW-38 | S | **9** | **Belgeler gerçeği söylesin.** README/README-TR: `--installer` **var olan** bir yol olarak dürüstçe anlatılır (kaldırılmaz), "Java 25 gerekir" yanlışı düzeltilir, taşınabilir/installer farkı netleşir, kanal kararının **açık** olduğu yazılır. | B20, §5.3 | README'deki 9 yanlış iddia (`analysis.md` §5.3) tek tek düzeltilir; iki dil birebir. §5.3'ün üç maddesi **zaten düzeltilmişti** (durum çubuğu AA kontrastı, dil değişiminin anındalığı, kısayol listesi) ve doğru oldukları için dokunulmadı. Düzeltilenler: paketlenmiş uygulama gömülü JDK 25.0.2 taşır (Java kurulumu yalnızca kaynak/fat JAR için), yeni "Download (end users)" bölümü (`git clone` geliştirici yolu olarak etiketlendi), iki kanalın da yayınlandığı ve varsayılanın açık karar olduğu, araç çubuğu için "ikinci satıra geçmez" + ölçülen 1280/900 px + pencere minimum genişliği olmadığı, `Tab` sırasının `viewOrder` ile sabitlendiği ve JavaFX'in bunu odak için garanti etmediği, yalnızca loglanan hata yolları (ayar yazımı/ses/undo sıfırlama) ve diyalogsuz taslak silme, `ContrastRatioTest`'in popup hücresi için yalnızca yazı boyutu eşitliğini denetlediği, CI tetikleyicisinin `main` olduğu, `package.sh`'nin her hedef platformda çalışması gerektiği. Parite: 15/15 `##`, 16/16 `###`, 79/79 tablo satırı, 18/18 iç madde, 52/52 madde |
| ✅ PW-39 | S | **9** | **Gizlilik notu:** verinin nerede durduğu, yerel olduğu, nasıl silineceği (loglar, taslaklar, ayarlar). | B36 | README'de "Your Data" / "Verileriniz" bölümü; iki dilde. İçerik koddan doğrulandı: `~/.jtypewriter/` (settings, `recovery/`, `logs/`, `instance.lock`), taslakların belge metnini birebir ve geldiği mutlak yolu tuttuğu (Base64 = kodlama, **şifreleme değil**), belge yanındaki `.bak*`/`jtw-*.tmp` dosyalarının da metin içerdiği, 1 MB x 3 kuşak dönen loglar. Ağ: `src/main`'de soket/`HttpClient` yok; tek dışa açılan eylem Hakkında'daki `mailto:` devri. Silme: önce çıkış, sonra dizini sil - uygulama içinde "tüm verileri sil" komutu **yok** ve kaldırma bu veriyi **silmez**; bu iki sınırlama açıkça yazıldı |
| ✅ PW-40 | S | **6** | **`maven-enforcer`:** JDK 25 + Maven sürüm kontrolü. | B4 | Yanlış JDK ile build **anında** ve anlaşılır hata verir. Uygulama: `maven-enforcer-plugin` 3.6.3, `validate` aşamasında (`enforce-build-toolchain`), iki kural - `requireJavaVersion [${java.version},)` ve `requireMavenVersion [3.8,)`. JDK alt sınırı `java.version`'dan türetilir, yani dil seviyesi yükseltilince kontrol geride kalamaz; Maven 3.8 iki README'nin de yazdığı tabandır. Mesaj iki satırlık `message` bloğu yerine tek satıra indirildi çünkü Enforcer `message` gövdesini satır satır girintileyerek basıyor. Doğrulandı: JDK 25 `BUILD SUCCESS`; JDK 21 ve 17 `validate` aşamasında, tek bir sınıf derlenmeden `BUILD FAILURE` - "needs JDK 25 or newer … point JAVA_HOME at a JDK 25 installation". İki README'ye birer paragraf eklendi |
| ✅ PW-42 | S | **6** | **Rozet/artefakt retention** ve sürüm notu şablonu (EN+TR). | B26, B27 | Release notu iki dilli; retention sınırlı. Uygulama: retention PW-12'de zaten konmuştu (badge raporları 1 gün, OS artefaktları 7 gün) ve rozet job'ı raporları **aynı koşuda** tükettiği için 1 gün yeterli - değişen tek şey bunun nedenini yazan yorum. Sürüm notu `release.yml` içindeki satır satır `printf` bloğundan çıkarılıp `docs/release-notes-template.md`'ye taşındı: EN ve TR yarıları aynı komutları (checksum doğrulama, imzasızlık uyarısı, kanal kararının açık olduğu) taşır, sürüm `@VERSION@`/`@TAG@` yer tutucularından gelir. `release.yml` şablonu `sed` ile render eder, **kalan her yer tutucuda** ve **iki dil başlığından biri yoksa** işi kırmızı yapar (negatif yol denendi: TR başlığı silinince adım `exit 1`). `ReleaseNotesTest` (4 test) sözleşmeyi kilitler: her `upload-artifact` adımında `retention-days` ≤ 7 (çizgi silinince test kırmızı), gövde şablondan geliyor, şablon iki dilli ve gömülü sürüm içermiyor, iki yarı aynı doğrulama komutlarını veriyor |

### 2.4 P1.3 — Doğrulama (Hafta 3–4)

| ID | Efor | Skor | İş | Kaynak | Kabul kriteri |
|---|---|---:|---|---|---|
| PW-43 | M | 7,5 | **Paket smoke testi:** CI'da üretilen app-image **başlatılır** (`--help` benzeri veya 5 sn açılıp kapanan bir mod); Linux'ta `.deb` kurulup kaldırılır. | B21 | Paket "üretildi" değil "açıldı" olarak doğrulanır; ilk gerçek regresyon kapısı |
| PW-44 | S | 9 | **`ContrastRatioTest` kapsamını genişlet** (CTA, hover, e-posta, tagline, `.stats-save`, 5 vurgu rengi, editör ve araç çubuğu metni). | U1–U6, §6.2 notu | Test, §6.2'deki **tüm** ölçülmüş kombinasyonları kapsar; README'nin fazla iddiası düzelir |
| PW-45 | M | 7,5 | **README/README-TR "iddia ↔ gerçek" denetimi** (iki dil, aynı bölüm sırası): toolbar genişlik iddiası, "anında dil değişimi", "hatalar diyalogda", `ContrastRatioTest` kapsamı, kısayol listesi. | §5.3, §6.2 | Hiçbir iddia kodla çelişmiyor; iki dosya birebir aynı bölüm sırasında |
| PW-46 | S | 6 | **`AGENTS.md`'yi sürüm kontrolüne al** (`analysis.md` §9.6: kuralları CI göremiyor). | §9.6 | `.gitignore` kuralı kaldırılır; kurallar PR'da görünür |

### 2.5 P1 çıkış kriterleri (Definition of Done)

1. **Okunabilirlik:** varsayılan temada AA altı tek bir metin yok; `ContrastRatioTest` bunu
   §6.2'deki tüm kombinasyonlar için zorluyor. *(PW-06/07/15/44)*
2. **İlk kullanım:** açılışta odak editörde; hiçbir diyalog 768 px ekranı aşmıyor; 4 dilde
   kırpılan metin yok. *(PW-09/14)*
3. **Güven:** kapatma isteği dışı çıkış, salt-okunur ev dizini, disk dolu, `kill -9` —
   dördünde de veri kaybı yok; otomatik kayıt kapalıysa arayüz söylüyor. *(PW-26/27/31)*
4. **Kodlama:** hiçbir yolda sessiz kodlama değişimi yok. *(PW-02/32)*
5. **i18n:** 6 dilde argümanlı anahtarların tamamı doğru biçimleniyor; sayı/tarih biçimi UI
   diliyle uyumlu; ölü anahtar yok. *(PW-04/05/21/22)*
6. **Yayın:** `v*` tag'i checksum'lı bir Release üretiyor; her artefaktta `LICENSE`,
   `THIRD-PARTY-NOTICES` ve kaynak teklifi var; POM metadata tam. *(PW-13/33/34/35)*
7. **Doğrulama:** `mvn clean test` + TR/DE locale + paket smoke testi yeşil.
   *(PW-43)*

---

## 3. Faz P2 — Editör Olma (Hafta 5–9)

P1 ürünü *dürüst ve okunabilir* yaptı; P2 onu *kullanılabilir* yapar. Giriş koşulu: P1'in
5. ve 3. maddesi yeşil.

### 3.1 Sıra

| ID | Efor | İş | Kaynak | Neden bu sırada |
|---|---|---:|---|---|
| PW-47 | M | **Ara / Değiştir.** Bul, sonrakini bul, tümünü değiştir; YOLO modunda değiştirme devre dışı (saf `FindPolicy` + test). | F1 | 300 KB metinde gezinmenin tek yolu; ürünün **en çok istenen** eksiği |
| PW-48 | S | **Komut satırından dosya açma.** `getParameters()` okunur; bilinmeyen argüman sessizce yok sayılmaz. | F4 | Paketlenmiş masaüstü uygulamasının temel beklentisi; OS entegrasyonunun önkoşulu |
| PW-49 | S | **Dış değişiklik algılama.** Yüklemede mtime+boyut saklanır; kaydetmeden önce karşılaştırılır, farklıysa kullanıcıya sorulur. | V3 | "Başkasının metnini sessizce ezme" — güven vaadinin ikinci ayağı |
| PW-50 | M | **`UndoHistoryReset` başarısızlığı yüksek sesle.** Dönüş değeri kontrol edilir; başarısızsa log + kullanıcı uyarısı. | M7 | Sessiz bozulma, modüler paketlemede çapraz belge sızıntısına dönüşür |
| PW-51 | L | **Büyük belge davranışı.** 5/10/50 MB için ölçüm (`analysis.md` §3.4); `SessionStats` artımlı sayaçlara çevrilir (1 Hz tam tarama biter); kabul edilemez boyut için açık sınır/uyarı. | M5, M6, M15 | "Kesintisiz yazma" vaadi ile FX thread'inde O(n) iş çelişiyor |
| PW-52 | L | **Belge G/Ç'si FX thread'inden çıksın.** Kaydet/yükle `Task` üzerinden; ilerleme göstergesi; aynı anda tek G/Ç. | M3 | 50 MB kayıt = saniyelerce donmuş arayüz |
| PW-53 | M | **Yazdırma / PDF.** JavaFX `PrinterJob` + sayfa düzeni; yazdırma önizlemesi yoksa en azından doğrudan yazdır. | F2 | "Tez yazan" profili için çıktı almanın tek yolu |
| PW-54 | M | **Otomatik kayıt ucuzlasın.** Kirli revizyon sayacı; metin değişmediyse snapshot atlanır; taslak serileştirmesinde kopya sayısı 3 → 1. | M6 | 30 s'de bir tam serileştirme pil ve CPU yakıyor |
| PW-55 | M | **Dispose sözleşmesi.** `AutoSaveService`/`MainController` dinleyicileri kaldırılabilir; `StatsBar` timeline'ı alan olarak tutulur ve durdurulur. | M10 | İkinci pencere/dinleyici sızıntısı P4'ün önkoşulu |
| PW-56 | M | **Dinleyici sırası açık hale gelsin.** `Messages.reload()` → `ToolbarView` → başlık sırası koda yazılır (veya tek koordinatör); test eklenir. | M11 | Alan sırası değişince sessizce bozuluyor, test kırılmıyor |
| PW-57 | M | **`AppLogging` enjekte edilebilir olsun** (`Path`/`Supplier`); testler gerçek `~/.jtypewriter`'a yazmayı bırakır. | M1, V19 | AGENTS.md kuralı ancak böyle uygulanabilir |
| PW-58 | S | **Araç çubuğu gizlenince odak + Tab sırası gerçek testi**; `viewOrder` yerine gerçek traversal doğrulaması. | U8, U21 | Erişilebilirlik iddiası ölçülmüş olur |
| PW-59 | S | **Yerelleştirme temizliği:** gömülü `dd.MM.yyyy` / `HH:mm(:ss)` desenleri bundle'a; `CrashHandler` başlıkları çevrilebilir; `": "` ayırıcı; tooltip'lerde platform-duyarlı Cmd/Ctrl. | I4, U24 | 4 dilde tarih biçimi artık kendi dilinde |
| PW-60 | S | **Küçük UX borçları:** `YOLO`'da ses çalmasın, durum çubuğu klavyeyle boyut değiştirsin, Yeni/Farklı Kaydet araç çubuğunda, başlık işaretleri OS kırpmasına dayanıklı. | U12, U15, U16, U18 | Günlük kullanımda sürtünme |
| PW-61 | M | **CI: JaCoCo + eşik.** Kapsam ölçülür, düşüş bloklanır. | §9.5/3 | P2 hacim büyütüyor; ölçüm olmadan korunamaz |

### 3.2 P2 çıkış kriterleri

1. Ara/Değiştir çalışıyor ve YOLO ile çakışmıyor — testli.
2. `jtypewriter dosya.txt` belgeyi açıyor; dış değişiklik uyarısı veriyor.
3. 10 MB belgede yazma gecikmesi ölçülmüş ve kabul edilebilir (hedef: tuş başına < 20 ms,
   kayıt sırasında arayüz donmuyor).
4. Yazdırma/PDF çıktısı alınabiliyor.
5. Kapsam ölçümü CI'da; `AppLogging` testleri gerçek ev dizinine dokunmuyor.

---

## 4. Faz P3 — Ürünleşme + Kanal Kararı (Hafta 10–15)

### 4.1 Günlük kullanım

| ID | Efor | İş | Kaynak |
|---|---|---:|---|
| PW-62 | M | **Dışa aktarma** (HTML + PDF; `.docx` yalnız talep varsa) | F3 |
| PW-63 | M | **Son dosyalar** listesi (ayar + araç çubuğu/karşılama erişimi) | F7 |
| PW-64 | S | **Sürükle-bırak ile açma** | F8 |
| PW-65 | M | **Menü çubuğu** (komut keşfedilebilirliği; kısayollar aynı kalır) | F9 |
| PW-66 | M | **Tercihler penceresi** (otomatik kayıt aralığı, yedek nesli, yazı tipi ailesi, UI ölçeği) | F13 |
| PW-67 | L | **Yazım denetimi** (TR + EN; sözlük kaynağı ve lisansı **önce** netleşir) | F5 |

### 4.2 Tanılama ve destek

| ID | Efor | İş | Kaynak |
|---|---|---:|---|
| PW-68 | S | **"Log klasörünü aç"** + **tanılama paketi** (log + ayar + sürüm, kullanıcı onayıyla) | U24, B36 |
| PW-69 | S | **Sürüm kontrolü (elle)** — ağ isteği yalnız kullanıcı istediğinde | F12 |
| PW-70 | S | **`SECURITY.md` + issue template + bilinen sınırlamalar** | B27 |
| PW-71 | S | **Ayar şeması sürümü + göç** (enum yeniden adlandırması tercihi sıfırlamasın) | M14, V16 |
| PW-72 | M | **`AppDirs` platforma uygun olsun** (macOS `Application Support`, Windows `%APPDATA%`) — kanal kararından bağımsız iyileştirme | B16 |

### 4.3 Paketleme olgunlaşması

| ID | Efor | İş | Kaynak |
|---|---|---:|---|
| PW-73 | M | **jlink spike + karar.** `--add-modules` ile runtime budama; `UndoHistoryReset` yansımasının modül yolunda kırılıp kırılmadığı **ölçülür**. | B1, B2, M7 |
| PW-74 | M | **`javafx-media` / `javafx-fxml` ağırlığı.** Media yalnız 71 KB WAV için; FXML boş `BorderPane` için. En az biri kaldırılır. | M8 |
| PW-75 | S | **Yeniden üretilebilirlik doğrulaması** (aynı tag → aynı checksum) | B5 |
| PW-76 | M | **Desktop entegrasyonu** (`Categories`, MIME, `/usr/share/doc`) — **yalnız installer kanalı seçilirse** | B19 |

### 4.4 🔑 Dağıtım Kanalı Kararı (ADR kapısı) — P3 sonu

Kullanıcının ertelediği karar burada, **bilgiyle** verilir. Girdi: PW-73/74 ölçümleri, P1/P2
sonrası ürün olgunluğu, imza maliyeti.

| Seçenek | Getiri | Maliyet | Gerektirdiği ek iş |
|---|---|---|---|
| **A — Taşınabilir + installer (mevcut durum)** | En geniş erişim; mevcut betikler çalışıyor | İki kanal bakımı; imza her platformda gerekir | B10, B13–B15, B19 |
| **B — Yalnız taşınabilir** | Tek kanal, izsiz, imza zorunlu değil | Linux'ta "indir-çalıştır" alışkanlığı zayıf; macOS'ta Gatekeeper yine sorun | AppImage/tar.gz, `AppDirs` taşıma |
| **C — Yalnız installer** | Sistem entegrasyonu, güncelleme kolay | İmza + notarization zorunlu; yönetici hakkı | B10, B13–B15 |

**Karar artefaktı:** `docs/adr/0001-dagitim-kanali.md` — seçenek, gerekçe, geri dönüş yolu,
imza/notarization planı. Bu ADR yazılmadan P4'e geçilmez.

> **Şimdilik geçerli olan:** hiçbiri seçilmedi. Bu yüzden P1/P2/P3 boyunca `package.sh`'ın
> iki yolu da (app-image + `--installer`) **korunur**, ama ikisi de "ürün kararı" olarak
> sunulmaz — belgeler yalnız *ne varsa onu* anlatır (PW-38).

### 4.5 P3 çıkış kriterleri

1. Günlük akış tamam: dışa aktarma, son dosyalar, menü, tercihler.
2. Tanılama paketi tek tıkla üretilebiliyor; `SECURITY.md` + issue template var.
3. Ayar şeması sürümlü; göç testi var.
4. **ADR yazıldı** ve kanal kararı gerekçelendirildi.
5. jlink'in kazancı (veya kaybı) ölçülmüş sayı olarak yazılı.

---

## 5. Faz P4 — Platform (v2)

| ID | Efor | İş | Kaynak | Not |
|---|---|---:|---|---|
| PW-77 | XL | **Çoklu belge / sekmeler** — `DocumentSession` nesnesi + `TabPane`; tek pencere varsayımı (M9) kırılır | F6, M9 | P1/P2/P3'ün üzerine kurulur, tersi olmaz |
| PW-78 | XL | **Sanallaştırılmış editör** (RichTextFX/`CodeArea` veya özel) | M15 | PW-51 ölçümleri karar verir |
| PW-79 | L | **`module-info` + modül yolu** (yansıma bağımlılıkları temizlenerek) | B2, M7 | PW-73'ün devamı |
| PW-80 | L | **Erişilebilirlik denetimi** (erişilebilir adlar, canlı bölgeler, yüksek kontrast, UI ölçeği, gerçek Tab traversal) | U21, U22, U13 | Uzman/araç desteği gerekir |
| PW-81 | S | **RTL hazırlığı** (`NodeOrientation`) | F14 | Dil paketi yoksa yalnız altyapı |
| PW-82 | M | **`ContrastRatio` yerine tam token mimarisi** — `Theme` içindeki kopya renkler CSS ile tek kaynaktan üretilsin | §3.3 | Renk drift'i sınıfını kapatır |

---

## 6. Zaman Çizelgesi ve Sürüm Planı

| Sürüm | İçerik | Hedef | Not |
|---|---|---|---|
| **0.9.0-beta.2** | P1.0 (25 hızlı kazanım) | Hafta 1 | İlk kez "görsel olarak bitmiş" hissi |
| **0.9.0-beta.3** | P1.1 (veri güvenliği kapısı) | Hafta 2 | `kill -9` testi yeşil |
| **0.9.0-beta.4** | P1.2 + P1.3 (yayın bütünlüğü + smoke) | Hafta 4 | İlk **checksum'lı** Release |
| **0.9.1-beta** | P2 çekirdeği (PW-47..50) | Hafta 6 | Ara/değiştir + CLI |
| **1.0.0-rc** | P2 tamamı | Hafta 9 | Performans ölçülmüş |
| **1.0.0** | P3 (dışa aktarma, tercihler, tanılama) + ADR | Hafta 14 | Kanal kararı bu sürümde |
| **1.1.0** | P3 kalanı / kanal uygulaması | Hafta 16 | ADR'e göre |
| **2.0.0** | P4 | Belirsiz | Çoklu belge + sanallaştırma |

**Sürüm kuralları:**

- `pom.xml` tek doğruluk kaynağı kalır.
- Her tag → GitHub Release + `SHA256SUMS` + iki dilli sürüm notu (EN + TR).
- Her sürümde 6 bundle pariteli (`MessagesTest` zorluyor) ve TR/DE locale koşusu yeşil.
- `1.0.0` etiketi, **P1 §2.5'in yedi maddesi** ve **P2 §3.2'nin beş maddesi** karşılanmadan
  atılmaz.

---

## 7. Risk Kaydı

| Faz | Risk | Olasılık | Etki | Azaltım |
|---|---|---|---|---|
| P1.0 | CSS/renk değişikliği bir temayı bozar (kontrast düzeltirken okunabilirliği düşürmek) | Orta | Orta | PW-44 testi **önce** yazılır; her renk değişikliği testten geçer |
| P1.1 | PW-26 çıkış yolu değişikliği yeni bir kayıp yolu açar (taslak hiç silinmez → disk büyür) | Orta | Yüksek | Taslak silme yalnız `requestClose` sonrası **bir kez**; `AutoSaveServiceTest` üç yolu da kapsar; PW-31 süreç testi regresyonu yakalar |
| P1.1 | `kill -9` testi CI'da kararsız (timing, dosya sistemi) | Orta | Orta | Tek senaryo, sabit bekleme yok (olay tabanlı), yalnız Linux leg |
| P1.2 | `keystroke.wav` lisansı çözülemez | Orta | **Yüksek** | PW-36 kendi üretimi bir sesle değiştirme yolunu **açık tutar**; yayın kapısı bunu bloklar |
| P1.2 | Release workflow'u imzasız artefakt yayınlar ve kullanıcı uyarı görür | Yüksek | Orta | PW-38 belgelerde açıkça yazar; imza ADR'e (P3) ertelenir, **gizlenmez** |
| P2 | Ara/değiştir YOLO ile çakışır | Orta | Orta | Değiştirme YOLO'da devre dışı; `YoloPolicy`'ye test |
| P2 | Async G/Ç sonrası yeni yarış koşulları | Orta | Yüksek | Tek G/Ç kuyruğu; durum makinesi testli; P1.1 testleri korunur |
| P3 | Kanal kararı yine ertelenir | Orta | Orta | ADR P3 çıkış kriteri; ertelenirse P4 açılmaz (bilinçli kilit) |
| Genel | Tek geliştirici → kapsam kayması | Yüksek | Yüksek | Her alt fazın çıkış kriteri; PW-* listesi dışına çıkılmaz |
| Genel | 6 dilde çeviri kalitesi düşer | Orta | Orta | PW-04 biçimlendirme testi + `MessagesTest` paritesi + her yeni anahtar 6 dosyada |

---

## 8. Başarı Ölçütleri (KPI)

| Metrik | Hedef | Ölçüm |
|---|---|---|
| Varsayılan temada AA altı metin | 0 | `ContrastRatioTest` |
| 768 px ekranda taşan diyalog | 0 | Elle + `WelcomeDialog` boyut testi |
| Kapatma isteği dışı çıkışta kaybedilen taslak | 0 | PW-26 testi + PW-31 süreç testi |
| `kill -9` sonrası kurtarma oranı | %100 | PW-31 (CI'da her push) |
| Kurtarma sonrası kodlama değişimi | 0 | PW-02/PW-32 matrisi |
| EN/FR/IT'de bozuk biçimlenmiş metin | 0 | PW-04 (6 dil × tüm argümanlı anahtarlar) |
| Artefakt doğrulanabilirliği | checksum + tag + `LICENSE` | Release sayfası |
| İlk açılış → yazmaya başlama | ≤ 5 sn | Elle, 3 OS |
| 10 MB belgede tuş gecikmesi | < 20 ms | PW-51 ölçümü |
| Test sayısı | 218 → ≥ 260 (P1), ≥ 320 (P2) | Surefire |
| Kapsam (P2 sonrası) | çekirdek ≥ %70 | JaCoCo |

---

## 9. Anti-Kapsam (yapılmayacaklar)

Bilinçli olarak **yapılmayacak** işler — kapsam kaymasını önlemek için yazılı:

- **Dağıtım kanalı seçimi (P3 ADR'ine kadar).** Ne portable ne installer "ürün kararı" ilan
  edilir; ikisi de mevcut haliyle kalır, belgeler yalnız gerçeği anlatır.
- **Kod imzalama / notarization (ADR'e kadar).** Ücretli sertifika kararı; P1'de checksum +
  şeffaf uyarı yeterli.
- **jlink / runtime budama (P3).** Kazancı ölçülmeden yapılmaz; `UndoHistoryReset` yansıması
  yüzünden riski var (M7).
- **Sekmeler / çoklu pencere (P4).** `DocumentSession` olmadan girişilirse 6 sınıfa yayılan
  bir refactor olur.
- **Sanallaştırılmış editör (P4).** PW-51'in ölçümü olmadan seçim yapılmaz.
- **Swing'e geçiş.** JavaFX sorunları (Media ağırlığı, FXML, jlink) çözülebilir; Swing
  sorunları değil.
- **Markdown/WYSIWYG.** Ürün kimliği düz metin.
- **Eklenti sistemi, bulut/hesap, mobil/web, AI asistanı.** Konumlandırmayla çelişir.
- **Otomatik güncelleme (P3'te yalnız *elle* kontrol).** Ağ erişimi kullanıcı isteğiyle sınırlı
  kalır — "yerel-öncelikli, telemetrisiz" vaadi korunur.

---

## 10. İş Paketi Kapsama Matrisi

`docs/analysis.md`'deki 116 bulgu kimliğinin **115'i** (I4'ün 10 alt maddesi ayrı sayılırsa
125'i) bir faza bağlanmıştır. Hiçbir bulgu "unutulmuş" değildir; bazıları bilinçli olarak
**P4'e veya ADR'e** ertelenmiştir, biri (**B7**) bilinçli olarak **yapılmayacak** sayılmıştır
(madde 4).

| Faz | Kapsanan analiz ID'leri | Adet |
|---|---|---:|
| **P1.0** | V1, V6, V7, V8, V14, V15 · M4, M12, M13 · F11 · U1–U11, U14, U17, U23 · I1, I2, I3, I5 · B5, B6, B22, B23, B24, B26, B28, B30, B33, B34, B35 | **39** |
| **P1.1** | V2, V4, V17, V18, V19 · B4 | **6** |
| **P1.2** | B9, B11, B20, B21, B27, B29, B31, B32, B36 | **9** |
| **P2** | V3, V5, V9, V10, V11, V12, V13 · M1, M2, M3, M5, M6, M7, M10, M11, M14, M16 · F1, F2, F4 · U12, U15, U16, U18, U20, U21, U24, U25 · I4 · B8, B17, B25 | **32** |
| **P3** | M8 · B3, F3, F5, F7, F8, F9, F10, F12, F13 · B1, B2, B10, B12, B13, B14, B15, B16, B18, B19, B37 · U13, U19, U22 · V16 | **25** |
| **P4** | M9, M15 · F6, F14 | **4** |
| **Toplam** | | **115** |

**Bulgu → faz eşlemesinde dikkat edilecek dört nokta:**

1. **B3 ve M8 aynı iştir** (PW-74): `javafx-media`/`javafx-fxml` ağırlığı. Tek paket, iki ID.
2. **V19'un kökü M1'dir** (PW-57): `AppLogging` enjekte edilebilir olmadan test hijyeni
   düzelmez. V19 P1.1'de "kısmi" (testler gerçek dizine yazmayı bırakır), M1/P2'de tam kapanır.
3. **U19 (her açılışta modal karşılama)** P1.0'da yalnız *boyut/kırpılma* tarafıyla (PW-14)
   ele alınır; "ilk çalıştırma bayrağı" ayar şeması gerektirdiği için P3/PW-66'ya bağlıdır.
4. **B7 (ikon drift'i) kapsam dışıdır** - bir iş paketi yoktur ve hiçbir faza sayılmaz.
   `scripts/generate-icons.py --check`, üretilen PNG/ICO/ICNS baytlarını o an kurulu
   ImageMagick yapısıyla (sürüm + HDRI + delegate seti) karşılaştırır; aynı SVG'den üretilen
   asset'ler başka bir ImageMagick yapısında bayt-özdeş çıkmaz - ölçüldü: 512 px'te piksellerin
   %0,3'ü farklı, fark gözle görülmüyor. Böyle bir adım CI'da "runner'ın ImageMagick'i commit'i
   üreten yapıyla aynı mı" sorusunu ölçer, ikonun kendisi hakkında bir şey söylemez ve her
   ImageMagick sürüm atlamasında kırmızıya döner. Asset'ler yalnızca SVG elle değiştiğinde
   yeniden üretilir ve o değişiklik zaten diff'te görünür; bu yüzden kontrol bir yayın kapısı
   değil, sürekli bakım borcudur.

**Bilinçli ertelemeler (tarih yok, sıra var):**

| Bulgu | Neden şimdi değil |
|---|---|
| B10 (imza/notarization) | Ücretli sertifika + kanal kararı gerektirir → ADR (§4.4) |
| B12–B15 (taşınabilir arşiv, Ubuntu sürümü, arm64) | Doğrudan kanal kararına bağlı |
| B19 (desktop entegrasyonu) | Yalnız installer kanalı seçilirse anlamlı |
| U22 (yüksek kontrast modu) | Tema mimarisi yenilenmesi istiyor (PW-82) |
| M9/M15/F6 (çoklu belge, sanallaştırma) | `DocumentSession` olmadan girişilirse 6 sınıfa yayılır |

---

## 11. İlk Üç Adım (bugün başlanacaksa)

1. **PW-06 + PW-44 — önce testi yaz, sonra rengi.** `ContrastRatioTest`'i `.stats-save`,
   karşılama CTA/hover, About e-postası, tagline ve 5 vurgu rengiyle genişlet; testin
   **kırmızı** olduğunu gör. Sonra PW-06/07/15 ile yeşile getir. Yarım gün. Bu sıra, P1'in
   geri kalanında renk değişikliklerini güvenli kılar.

2. **PW-01 + PW-02 + PW-03 — üç "tek dosyalık" güven düzeltmesi.** Üçü de bağımsız, üçü de
   farklı sınıfta, üçü de testle kapanabilir. Toplam bir gün. Bu üçü bittiğinde "uygulama
   açılmıyor", "kodlama bozuluyor", "kaydedemiyorum" sınıflarındaki bilinen yollar kapanmış
   olur — P0'ın bıraktığı üç açık.

3. **PW-12 + PW-13 — yayın bütünlüğünün ilk taşı.** CI izinleri + action pinning + `LICENSE`
   artefakta. Yarım gün. Kanal kararı ne olursa olsun geçerli; sonradan yapılırsa geçmiş
   sürümler lisanssız kalır.

> Bu üç adım ~2 iş günü sürer ve P1'in 25 maddelik dalgasının **en yüksek skorlu 5 işini**
> kapsar. Dördüncü adım, dalganın kalanıdır (skor 9–12 arası 20 iş).
