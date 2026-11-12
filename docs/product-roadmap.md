# JTypeWriter — P3 Yol Haritası

> **Kaynak:** Yalnız `docs/analysis.md` (1.1.0 / `main` @ `338f1c4`). Bu belge yeni ölçüm
> üretmez, yeni bulgu eklemez; analizin `S1`/`S2`/`S3` kimliklerini sıraya ve iş paketine
> çevirir. Bir bulgu burada yoksa ya analizde yoktur ya da §4'teki kapsama tablosunda
> eşlenmemiştir — ikisi de kabul edilemez, tablo tamdır.
>
> **Sıralama ilkesi:** en hızlı kazanım en üstte. "Hızlı" = değişikliğin boyutu değil,
> **kapatılana kadar geçen insan kararı sayısı × doğrulama maliyeti**. Bir satırlık ama
> ürün kararı bekleyen iş, üç dosyalık ama kapısı hazır bir işten sonra gelir.
>
> **Efor gösterimi:** `AI` = ajanın doğrudan üretebildiği iş (dakika–saat); `İNSAN` =
> yalnız sahibin verebileceği karar, gözden geçirme veya dış dünya beklemesi. Toplam
> efor, baskın taraf belirtilerek aralık olarak verilir (§5). Takvim tarihi yoktur;
> sıra vardır.

---

## 1. P3'ün tek cümlesi

Analiz §15 ve §18 aynı teşhisi veriyor: **motor 4+, ürün 1–3**. P3 "daha çok özellik"
değil, üç boşluğu sırayla kapatma işidir:

1. **Kendi vaadini tutmayan küçük delikler** (B1, D1–D4, U1) — küçük, ucuz, güveni
   doğrudan etkiliyor.
2. **Dağıtım güveni** (P1, P2, B2, B3, B4) — ölçülmüş %49 kazanç ve doğrulanmamış yayın.
3. **Talep** (M1 ve türevleri) — kodla kapanmayan, kararla açılan eksen.

Bu sıra aynı zamanda **risk sırasıdır**: 1. maddedeki işler mevcut test kültürüyle
kanıtlanabilir; 2. madde yeni ölçüm ister; 3. madde kod dışıdır.

---

## 2. Dalga haritası (özet)

| Dalga | Ad | Kapsam | Baskın efor | Çıkış kapısı |
|---|---|---|---|---|
| **P3.0** ✅ Tamamlandı | Yol açıcı düzeltmeler | B1, M2, U2, U8, B5, D5, D13, D14, D2b (U1 ürün kararıyla kapsam dışı) | AI (saatler) | `package.sh` iki kez üst üste geçer; ağaç temiz |
| **P3.1** ✅ Tamamlandı | Veri güvenliği kapanışı | D1, D2, D3, D4, D6–D10, D4b–D4d, A4, D11, D12 | AI (1–2 gün) + 2 karar | `FxThreadIoTest` kurtarma yolunu kapsar; kodlama matrisi büyür |
| **P3.2** (3.2.1 ✅ Tamamlandı) | Dağıtım ve yayın güveni | P1, P2, B2, B3, B4, B6–B8, Q1 | AI + dış karar | İmaj ≤ ~75 MiB; yayın hattı kapıları koşar |
| **P3.3** | Editör yetkinliği | U3–U7, U9, A1, A7, P4, §8.2 özellikleri | Karara bağlı | Ürün sınırı kararı yazılı; seçilen özellik testli |
| **P3.4** | Talep ve sürdürülebilirlik | M1, M3, M4, M5 | İNSAN | Dağıtım kanalı + katkı altyapısı canlı |

Dalgalar **sıralıdır**, çünkü P3.2 P3.1'in ürettiği yeşil ağaca dayanır (B1 düzelmeden
paketleme ölçümü alınamaz), P3.3 ise §16.3 kararı olmadan başlamaz.

---

## 3. Dalga detayı

### P3.0 — Yol açıcı düzeltmeler (en hızlı kazanımlar) ✅ Tamamlandı

Amaç: sürümü **fiilen kıran** ve **yanlış bilgi veren** işleri bitirmek. Hepsi mevcut
test kültürüyle kanıtlanabilir; hiçbiri mimari karar gerektirmez.

> **Durum (2026-11-11):** tamamlandı. Kapı kanıtı: `mvn clean test` → 598 test, 0 hata;
> `./scripts/package.sh --skip-build --installer` iki kez → `0`/`0`, `releases/linux/`
> içinde tek `.deb`; `./scripts/verify-artifacts.sh --skip-build` → `0`, "all 20
> comparisons passed". U1 ürün kararıyla yapılmayacak olarak kapatıldı (§3.0.3).

#### 3.0.1 B1 — `.deb` temizliği (S1, kanıt düzeyi **A**) ✅ Tamamlandı

- **Sorun:** `package.sh:115` / `verify-artifacts.sh:180-186` `"$DEST/$APP_NAME"*.deb`
  (büyük harf) arıyor; jpackage küçük harfli `jtypewriter_<sürüm>_amd64.deb` yazıyor
  (`package.sh:354` kendi yorumunda söylüyor). Sonuç: ikinci sürümde "found 2", çıkış 1.
- **İş:** temizliği gerçek üretim adına bağla (glob'u üretim adıyla aynı kaynaktan türet,
  iki betikte tek kural). Sayım assert'i kalmalı — ama artık *yanlış sayıda paket*
  gerçekten hata olsun.
- **Neden en üstte:** tek kural, iki dosya, sıfır karar; düzeltilmeden P3.2'nin hiçbir
  ölçümü alınamaz ve yerel sürüm mühendisi her sürümde düşer.
- **Kapı:** `./scripts/package.sh --skip-build --installer` iki kez art arda → iki kez de
  çıkış 0, `releases/linux/` içinde **tek** `.deb`. `verify-artifacts.sh --skip-build`
  aynı ağaçta geçer. Depodaki `jtypewriter_1.0.2_amd64.deb` bu işin kanıtı olarak
  kullanılır (artefakt commit edilmez).
- **Efor:** AI dakikalar; İNSAN 5 dk (diff onayı).
- **Sonuç:** `DEB_PACKAGE` (küçük harfli ad) tek yerden türetiliyor; temizlik, "tek `.deb`"
  assert'i ve `verify_deb_license` aynı değeri kullanıyor; `verify-artifacts.sh` aynı
  türetmeyi yapıyor. İkinci paketleme koşusu artık geçiyor; bayat `1.0.2` paketi ilk
  koşuda silindi.

#### 3.0.2 M2 — Bayat doküman/iddia paketi (S2, kanıt **A**) ✅ Tamamlandı

Dört ayrı yanlış, tek iş paketi (hepsi aynı "tek kaynak" ilkesinin ihlali):

1. README test tablosu 46 sınıf diyor, gerçek 61; 15 sınıf hiç anılmıyor
   (`README.md:499-548`, `README-TR.md:481-547`). Liste elle tutulduğu için zaten
   bayatlamaya mahkûm: tabloyu **testlerden türetilebilir** hâle getirmek ya da "tam
   liste" iddiasını kaldırmak gerekir.
2. `NOTICE:14` hâlâ "PNG, ICO ve ICNS" diyor; üreteç yalnız PNG+ICO yazıyor
   (`generate-icons.py:59-65`).
3. README mimari tablosunda `AppIcons` için "dock icon" (macOS dili), ağaçta var olmayan
   `UndoHistoryReset.java`, 18 gerçek sınıf ağaçta yok (`README.md:385,444`; §13.2).
4. Javadoc'lar belge tavanını "10 MB" diyor, gerçek 64 MiB — altı dosya:
   `FindScanService.java:25,60`, `FindModel.java:139`, `FindPanel.java:41`,
   `SessionStats.java:23`, `AutoSaveService.java:346`.
5. README "ikinci örnek uyarı gösterip çıkar" diyor; davranış modal uyarı + onay
   (§7.6, bu oturumda `timeout 8` → çıkış 124).

- **Kapı:** `mvn test` yeşil (özellikle `CiGatesTest`, `ReleaseNotesTest`,
  `PackagingNoticesTest`) — doküman metni pinleyen testler burada kırılırsa *doğru*
  kırılır.
- **Efor:** AI ~1 saat; İNSAN 15 dk (iki dilli metin gözden geçirmesi).
- **Sonuç:** test tablosu artık elle tutulmuyor: `ReadmeTestTableTest` sınıf listesini
  `src/test`'ten türetiyor ve iki README'de de eksik/fazla satırı, ayrıca Türkçe tablonun
  İngilizceden eksik olmasını hatırlarsa kırıyor (63 sınıf, iki dilde). NOTICE ICNS
  iddiasını kaybetti; README ağacı 18 eksik sınıfı kazandı, var olmayan `UndoHistoryReset`
  ve "dock" dili çıktı; altı Javadoc tavanı 64 MiB oldu; ikinci örnek anlatımı modal
  uyarıyı doğru anlatıyor.

#### 3.0.3 U1 — menü/araç çubuğu: ürün kararıyla kapsam dışı ✅ Tamamlandı (ürün kararı: yapılmayacak)

- **Durum:** **yapılmayacak.** Bulgu analizde duruyor (`S1`, §8.3: komut menüsü araç
  çubuğunun içinde, `MainController.java:294-296`; çubuk `Ctrl+T` ile gizlenince menüye
  fareyle ulaşılamıyor), ancak menünün araç çubuğuyla birlikte gizlenmesi **ürün
  kararı** olarak korunuyor: `Ctrl+T` "metinden başka hiçbir şey" vaadinin parçası ve
  menüyü ayrı bir kalıcı üst katmana almak tam da o vaadi bozar. Karar 2026-11-11'de
  ürün sahibi tarafından verilmiştir; P3.0'ın çıkış kapısından ve aşağıdaki tablolardan
  çıkarıldı.
- **Yeniden açmanın koşulu:** ürün sınırı kararı (§7 #3) değişir ve kalıcı menü şeridi
  kapsama girerse U1 bu dalgada değil, P3.3'te yeniden değerlendirilir. Bu bölüm, bulgunun
  kaybolmadığını ve neden yapılmadığını kayda geçirmek için duruyor.

#### 3.0.4 U2 — WPM tanımı (S2) ✅ Tamamlandı

`SessionStats.java:173-178` paydası `totalKeystrokes`: yapıştırma/IME/otomatik düzeltme
hızı hiç artırmıyor, silip yeniden yazmak şişiriyor. Ayrıca süre 5 s üstü boşlukları
saymıyor, WPM saymıyor → ekranda tutarsız iki sayı.

- **İş:** tek bir tanım seç (öneri: **karakter/5 ÷ aktif süre**, yapıştırma hariç ama
  yapıştırılan karakter sayılır) ve hem süre hem WPM'i aynı "aktif süre" tanımına bağla.
  Analiz iki ölçünün farklı şeyleri ölçtüğünü söylüyor; düzeltme **tanımı** düzeltmeli,
  katsayıyı değil.
- **Kapı:** `SessionStatsTest` (yapıştırma, IME, sil-yeniden-yaz senaryoları),
  `StatsBarModelTest`.
- **Efor:** AI ~1 saat + tanım kararı (İNSAN 10 dk).
- **Sonuç:** tanım tek: **karakter/5 ÷ aktif süre**. Pay, `min(belge uzunluğu, bu oturumda
  düzenlemeyle gelen karakterler)` — yapıştırma ve IME karakteri sayar ama süre eklemez,
  diskten yüklenen metin yazma hızı sayılmaz, silip yeniden yazmak sayıyı belgede olandan
  fazla şişirmez. `getFormattedDuration()` aynı aktif süreyi gösteriyor, yani ekrandaki iki
  sayı aynı aralığı ölçüyor.

#### 3.0.5 Küçük hijyen kümesi (S2/S3, her biri tek nokta) ✅ Tamamlandı

| ID | İş | Dosya | Kapı | Durum |
|---|---|---|---|---|
| D5 | `expired` boyut kuralında `break` → `continue`; iki yorumu koda uydur | `RecoveryRetention.java:79-84` | `RecoveryRetentionTest` yeni vaka: yaşlı-küçük taslak sığarken silinmiyor | ✅ Tamamlandı |
| D6 | DISCARD sonrası `store.clear()` dönüşü yok sayılmasın | `AutoSaveService.java:225-228` | `AutoSaveServiceTest`: clear=false → kullanıcıya görünür uyarı | ✅ Tamamlandı |
| D13 | `AppDirs` `user.home` yoksa korunsun (AppLogging/RecoveryStore ile aynı davranış) | `AppDirs.java:19-21` | `AppDirsTest`: `-Duser.home=` boş → NPE yok | ✅ Tamamlandı |
| D14 | `RecoveryTargetPath` `SecurityException` sızdırmasın | `RecoveryTargetPath:105-123` | hedef yol okunamazsa "yolsuz kurtarma" dalı | ✅ Tamamlandı |
| D2b | temp adı `NAME_MAX` sınırında kaydetmeyi bozmasın (adı kısalt, hash'le) | `AtomicFiles.java:76-78` | `AtomicFilesTest`: 250 karakterlik adla aç→kaydet | ✅ Tamamlandı |
| U8 | pencere başlığı sabit `"JTypeWriter"` → mevcut `appName` anahtarı | `FxApplication.java:60` | `MessagesTest` + başlık testi | ✅ Tamamlandı |
| B5 | rozet 3 locale toplamını ("1.725") değil gerçek test sayısını yayınlasın | `publish-test-badge.sh:79-127` | badge metni 575'i söyler | ✅ Tamamlandı |

- **Efor:** AI ~2 saat toplam; İNSAN 15 dk.

**P3.0 çıkış kapısı:** `mvn clean test` yeşil, `package.sh` iki kez geçer, README/NOTICE
iki dilde tutarlı. U1 ürün kararıyla kapsam dışı olduğu için görsel onay kapıdan çıktı
(§3.0.3). Tek commit ya da bulgu başına küçük commit'ler
(AGENTS: bir commit = bir kendi kendine yeten değişiklik).

---

### P3.1 — Veri güvenliği kapanışı ✅ Tamamlandı

Amaç: ürünün **en çok güvendiği vaadi** (kaybetmeyen taslak + kodlama korunur) sıfır
bilinen delikle kapatmak. Analiz §7.7 kayıp matrisinde "bozulma" işaretli dört satır
tam olarak bu dalganın kapsamıdır.

> **Durum (2026-11-11):** tamamlandı. Kapı kanıtı: `mvn clean test` → **631 test, 0 hata**
> (P3.0 sonrası 598'di; yeni sınıf açılmadı, mevcut sınıflara 33 test eklendi);
> `./scripts/package.sh --skip-build --installer` iki kez → `0`/`0`, `releases/linux/`
> içinde tek `.deb`; `./scripts/verify-artifacts.sh --skip-build` → `0`, "all 20
> comparisons passed". §7.7 matrisinde **"bozulma" satırı kalmadı**; `FxThreadIoTest`
> kurtarma yolunu kapsıyor (`restoreRecovered` diski yalnız I/O işçisinde okuyor);
> `EncodingMatrixTest` üç yeni senaryo kazandı (bozuk BOM, silinmiş orijinal, kayıplı
> UTF-8'in açık seçim olması). İki kapı bilinçli olarak kod yorumuyla kapatıldı, testle
> değil: kodlama diyaloğunun varsayılan düğmesi bir `Stage`, gizle/göster sonrası
> zamanlayıcının yeniden başlaması bir `Timeline` ister; ikisi de JavaFX toolkit'i
> başlatır ve "hiçbir test toolkit başlatmaz" kuralını bozar.

#### 3.1.1 S1 dörtlüsü ✅ Tamamlandı

1. **D1 — canlı taslakların sınırsız birikmesi.** `RecoveryRetention` yalnız `pending-*`
   yaşlandırıyor; `draft-*` yalnız *en yeni* benimsendiğinde siliniyor
   (`RecoveryStore.java:226-231,240-248`). Her çökme kalıcı dosya bırakıyor ve dosyalar
   kullanıcı metni taşıyor.
   - **İş:** `draft-*` için de yaş + **adet tavanı** kuralı; "en yeni benimsenir" kuralı
     korunur, kalanlar yaşlandırılır. Kullanıcı metni taşıdığı için yaş sınırı cömert
     olmalı (öneri: 30 gün + 20 dosya) — **ürün kararı**, varsayılan öneriyle ilerlenir.
   - **Kapı:** `RecoveryStoreTest` + `RecoveryRetentionTest`: 25 taslak → en yeni kalır,
     yaşlı-küçük olan boyut kuralına takılmaz.
   - **Sonuç:** `RecoveryRetention.MAX_FILES` 20'ye çıktı ve yaşlandırma artık **iki**
     öneki kapsıyor (`expiredParked` → `expiredDrafts`). İki güvenlik filtresi
     `RecoveryStore`'da: bu oturumun kendi slotu ve son 10 dakikada yazılmış herhangi bir
     taslak aday değil — ikinci bir örnek 30 saniyede bir taslağını tazeliyor, yani taze
     bir dosya hâlâ birinin tek kopyası olabilir. "En yeni benimsenir" kuralı korunuyor.
2. **D2 — `currentFilePath` sızıntısı + FX-thread sınırsız okuma.**
   `restoreRecovered` dosya yoksa/`file == null` ise önceki belgenin yolunu bırakıyor
   (`FileService.java:453-493`, atama yalnız `:482`). Aynı metot orijinali
   `Files.readAllBytes` ile boyut sınırı olmadan **FX thread'inde** okuyor (`:458`) —
   64 MiB politikası ve "disk işi yalnız `IoExecutor`" doktriniyle çelişiyor.
   - **İş:** her iki dalda da durumu sıfırla; okumayı `IoExecutor`'a taşı, 64 MiB
     sınırını uygula. Bu, A3'ün de kapanışıdır.
   - **Kapı:** `FxThreadIoTest` kurtarma yolunu da saysın (`restoreRecovered`,
     `loadDraft`, `clear`) — bugün kapsam dışı (§12.3).
   - **Sonuç:** `restoreRecovered` durumu metodun başında sıfırlıyor (yol, kodlama, BOM,
     özet) ve her hata dalı `currentFilePath == null` bırakıyor; önceki belgenin yolu
     artık hiçbir koşulda devralınmıyor. Orijinalin okuması tek bir `ioExecutor.submit`
     içinde: önce `Files.size`, 64 MiB üstü "doğrulanamaz orijinal" sayılıp taslak
     ipucuyla devam ediyor, aksi hâlde `readNBytes(MAX + 1)`. `FxThreadIoTest`
     kurtarma okumasının yalnız I/O işçisinde çalıştığını sabitliyor.
3. **D3 — bozuk BOM'lu orijinalden kurtarmada kodlama kaybı.** `MalformedDocumentException`
   dalı `currentFilePath`'i atıyor ama `currentCharset`/`currentBom`'a dokunmuyor
   (`FileService.java:475-481`); eski windows-1254 belge sonraki kayıtta sessizce UTF-8'e
   dönebiliyor.
   - **İş:** kodlama/BOM'u orijinalden taşı (ya da "bilinmiyor" olarak işaretle ve
     kaydetmede sor). Sessiz varsayılan yasak.
   - **Kapı:** `EncodingMatrixTest`'e "bozuk BOM → kurtar → kaydet" bayt senaryosu.
   - **Sonuç:** istisna zaten bildirimi taşıyordu
     (`declaredCharset()`/`declaredByBom()`); dal artık onu `currentCharset`/`currentBom`'a
     yazıyor ve `currentFilePath`'i koruyor, yani belge bir sonraki kayıtta yine kendi
     BOM'unu ve kodlamasını üretiyor. `EncodingMatrixTest` senaryosu bozuk UTF-16LE BOM'lu
     orijinali kurtarıp kaydediyor ve yazılan baytların hâlâ UTF-16LE BOM'uyla başladığını
     doğruluyor.
4. **D4 — "kayıpsız UTF-8" seçeneğinin kayıplı olması.** `TextCodec.decodeWith(...,
   REPLACE)` (`FileService.java:829-833`, `TextCodec.java:265-275`) geçersiz baytları
   `U+FFFD` yapıyor; diyalog metni UTF-8'i kayıpsız diye sunuyor.
   - **İş:** ya `REPORT` (geçersiz bayt → hata → başka kodlama seç) ya da metni
     "kayıplı" olarak yeniden yaz + açık onay al. **Ürün kararı**, ama iki seçenek de
     mevcut yanlışı kapatır.
   - **Kapı:** `TextCodecTest` + `EncodingMatrixTest`: geçersiz bayt → `?`/`U+FFFD`
     sessizce yazılmaz.
   - **Sonuç:** ürün kararı "kayıplı seçenek açık kalsın, ama varsayılan olmasın" oldu.
     Sessiz yol zaten `REPORT` ile bildiriyor (`TextCodec.decode`); `decodeWith` yalnız
     kullanıcının açık seçimi için duruyor ve Javadoc'u bunu söylüyor. Diyalogda
     birincil/varsayılan düğme artık **Windows-1254** (tek baytlık kodlama her baytı
     kayıpsız çözer), UTF-8 ise "karakterler kaybolabilir" etiketiyle alternatif. Altı
     paketteki metinler zaten doğruydu, değişmedi.

#### 3.1.2 Kurtarma/otomatik kayıt kümesi (S2/S3) ✅ Tamamlandı

| ID | İş | Neden | Durum |
|---|---|---|---|
| D7 | `DRAFT_DELETE` uyarısı başarılı anlık görüntüde erken susmasın; uyarı **diskteki duruma** bağlanmalı | `AutoSaveService.java:367-369`; bugün bayat taslak dururken uyarı siliniyor | ✅ Tamamlandı |
| D9 | `WINDOW_HIDDEN` sonrası zamanlayıcı yeniden başlasın | `AutoSaveService.java:300-303`; gizle/göster otomatik kaydı sessizce öldürüyor | ✅ Tamamlandı |
| D8 | Taslak biçimine imleç/satır sonu/BOM/kodlama taşı (undo geçmişi taşınmayabilir — ama geri kalanı taşınmalı) | kurtarma bugün yalnız metin+yol+zaman | ✅ Tamamlandı |
| D10 | `jtw-draft-*.tmp` artıkları açılışta süpürülsün | sert öldürme artığı hiçbir yerde temizlenmiyor | ✅ Tamamlandı |
| D4b | POSIX sahipliği korunsun (izin kopyası yetmiyor); yeni belge `0600` yerine `umask`'a uysun | `AtomicFiles.java:166-179` | ✅ Tamamlandı |
| D4c | Sarkan symlink düz dosyayla değiştirilmesin (reddet + sor) | `AtomicFiles.java:109-117` | ✅ Tamamlandı (reddediyor; soru sormuyor — kaydetme başarısız olur, belge kirli kalır) |
| D4d | Boş alan kontrolü yedek staging'ini (`prepared`) hesaba katsın | `FileService.java:677-687`; `StorageFootprint` 6× diyor, kontrol yok | ✅ Tamamlandı |

- **Not (D7):** uyarı artık tek bir "her şey yolunda" bayrağı değil: `AutosaveHealth.clear(Problem)`
  yalnız adı geçen sorunu çekiyor, yani başarılı bir yazma başarısız bir silmenin uyarısını
  söndüremiyor. Her alt sistem kendi başarısıyla kendi uyarısını geri alıyor.
- **Not (D8):** biçim `JTypeWriter-Draft/3`; `cursor=`, `charset=`, `bom=` başlık anahtarları
  eklendi ve `/2` dosyaları hâlâ tam taslak olarak okunuyor (metin + yol + zaman, imleç -1).
  Satır sonu ayrı alan değil: gövde belgenin birebir metni, CRLF bayt-bayt korunuyor.
- **Not (D9):** `WINDOW_SHOWN` var olan zaman çizelgesini yeniden başlatıyor
  (`resumeSnapshots()`); ikinci bir `Timeline` yok ve dondurulmuş slot bastırılmış kalıyor.

#### 3.1.3 Ayarlar kümesi (S2) ✅ Tamamlandı

| ID | İş | Not | Durum |
|---|---|---|---|
| A4 | `set` + `save` atomik olsun; `props.store` mutasyon sırasında gezmesin | `AppSettings.java:551-588` | ✅ Tamamlandı |
| D11 | Kısmen okunmuş bozuk dosyanın anahtarları kalıcılaşmasın ("varsayılanlarla aç" sözü tutulsun) | `AppSettings.java:478-511` | ✅ Tamamlandı |
| D12 | Taşan sürüm segmenti "sürümsüz" sayılıp daha yeni dosyayı aşağı sürümlemesin | `AppSettings.java:410-417,457-476` | ✅ Tamamlandı |

- **Not (A4):** `save()` kopyayı `Properties` monitörü altında alıyor, dosyayı o monitörün
  dışında özel bir `writeLock` altında yazıyor. Ayar okuyucular çok anahtarlı değerleri aynı
  monitörde okuyor; `setSoundEnabled` kararını kilit altında verip yazmayı dışarıda yapıyor,
  böylece kilit sırası ters dönmüyor. Kilit altında yazmak yerine anlık görüntü almanın
  nedeni: `props.store` giriş kümesini gezerek yazar ve yavaş bir diskte monitörü dakikalarca
  tutmak her ayar okumasını bloke ederdi.
- **Not (D11):** yükleme taze bir `Properties`'e yapılıyor, `props`'a yalnız tam başarılı
  ayrıştırmadan sonra `putAll` ediliyor; bozuk dosya yine karantinaya taşınıyor ama artık
  yarısı okunmuş anahtarlar bir sonraki kayıtta dosyaya geri yazılmıyor.
- **Not (D12):** iki "boş" sonuç ayrıldı: hiç rakam dizisi yoksa dosya gerçekten sürümsüz
  (mevcut sürüm damgalanır), rakam dizisi `long`'a sığmıyorsa dosya bu derlemenin
  yorumlayamadığı bir sürümden geliyor (değer olduğu gibi bırakılır, FINE loglanır).

**P3.1 çıkış kapısı:** §7.7 kayıp matrisinde "**bozulma**" satırı kalmaz; `FxThreadIoTest`
kurtarma yolunu kapsar; `EncodingMatrixTest` en az üç yeni senaryo; `mvn clean test`
yeşil. Bu dalga bittiğinde **veri güvenliği 4,5 → 5,0** gerekçesi yazılabilir olmalı.

> **Kapı sonucu (2026-11-11):** karşılandı. §7.7'de bozulma satırı yok; `FxThreadIoTest`
> `restoreRecovered` okumasını kapsıyor; `EncodingMatrixTest` üç senaryo kazandı (bozuk
> UTF-16LE BOM, silinmiş windows-1254 orijinal, kayıplı UTF-8'in yalnız açık seçimde
> oluşması); `mvn clean test` → 631 test / 0 hata. **Veri güvenliği 4,5 → 5,0**: matriste
> kullanıcıya görünen tek "bozulma" sınıfı kalmadı, kalan iki satır (bir kez yazılamayan
> yedek) tasarım gereği kabul edilebilir ve artık diskteki durumu yansıtan uyarılarla
> görünür.

---

### P3.2 — Dağıtım ve yayın güveni

Amaç: "50 MB indirip 146 MB kuruyorsun, imzasız, doğrulanmamış" tablosunu kapatmak.
Bu dalga yeni ölçüm ister; analizin **A**-seviyesi iki sayısı hedefi veriyor:
`jlink` → 68,2 MiB (mevcut 133,5 MiB), `jdeps` → 6 modül.

#### 3.2.1 P1 + P2 — tek değişiklik, iki bulgu ✅ Tamamlandı

- **P1:** imaj 52 modül taşıyor; `ct.sym` 10,4 MiB; `jdk.compiler`/`jdk.jshell`/`jconsole`
  hiç kullanılmıyor. `jlink --add-modules java.base,java.desktop,java.logging,
  javafx.controls,javafx.fxml,javafx.media,jdk.unsupported --strip-debug
  --no-header-files --no-man-pages --compress=zip-6` → 68,2 MiB (ölçüldü).
- **P2:** JavaFX runtime modüllerinde **yok** (`lib/runtime/release` 52 modülün hiçbiri
  `javafx.*` değil); sınıflar şişman JAR'dan classpath ile yükleniyor →
  `Unsupported JavaFX configuration: ... 'unnamed module'` her açılışta (3/3 koşu).
- **Neden birlikte:** JavaFX modül yoluna taşınmadan jlink kazancı kullanılamaz ve uyarı
  susmaz; ayrı ayrı yapılırsa iki kez paketleme hattı değişir.
- **İş:** `package.sh` jlink adımını modül listesiyle üretir; JavaFX jar'ları/native'leri
  runtime imajına modül olarak girer; şişman JAR'daki `.so` kopyası imajdan çıkar.
  `verify-artifacts.sh` yeni yerleşimi denetler (legal metinler `lib/runtime/legal/`
  altına da girmeli — bugün JavaFX lisansları yalnız JAR içinde, §10.4d).
- **Kapı:** yeni imaj ≤ ~75 MiB; `smoke-appimage.sh` iki platformda açar; açılış
  logunda "unnamed module" **yok** (3 koşu); `.deb` `Installed-Size` düşer.
- **Efor:** AI ~1 gün + iki platformda smoke (İNSAN/CI zamanı baskın).
- **Sonuç:** imaj **147 MiB → 73 MiB**, `.deb` **48 MiB → 44 MiB**
  (`Installed-Size` 73 996 KiB), çalışma zamanı 134 MiB → 60 MiB ve 8 modül:
  `java.base java.datatransfer java.xml java.prefs java.desktop java.logging
  java.scripting jdk.unsupported`. Üç koşunun hiçbirinde "unnamed module" yok; açılış
  logu artık tamamen sessiz (jpackage'ın kendi `Picked up JAVA_TOOL_OPTIONS` satırı
  dışında). `smoke-appimage.sh --skip-build --open-sample` → PASS (imaj + komut satırı
  belgesi), `verify-artifacts.sh --skip-build` → **39 karşılaştırma**, hepsi geçti.

  Planın iki varsayımı ölçümle düzeltildi. **Birincisi:** JavaFX `jlink` ile çalışma
  zamanına *linklenmedi*, `lib/javafx` altına **paketlendi**. `jlink` modüler bir JAR'dan
  yalnız `lib/` ve `bin/` taşır; JavaFX `.so` dosyalarını JAR kökünde tuttuğu için
  linklenmiş bir JavaFX `libglassgtk3.so` bulunamadan açılırdı. Modül *yolu* P2'yi
  çözmek için yeterli, modül *linklemesi* değil. **İkincisi:** modül listesinde
  `javafx.*` yok ve olmamalı; `jdeps --print-module-deps` yalnız JDK modüllerini verir,
  JavaFX ayrı bir modül yolu olarak kalır. Buna karşılık listede planda olmayan
  `java.scripting` var: `javafx.fxml` onu zorunlu istiyor, yokluğunda uygulama
  `Module java.scripting not found, required by javafx.fxml` ile açılmadan ölür —
  ilk denemede tam olarak bu oldu.

  Yol boyunca iki tuzak daha ölçüldü, ikisi de kodda yorumla kayıtlı: jpackage bir
  java-option içindeki `$APPDIR` makrosunu **launcher'ın kendi dizini** (`lib/app`) olarak
  genişletiyor, yani `$APPDIR/lib/javafx` → `lib/app/lib/javafx`. İlk çözüm `$ROOTDIR`
  oldu (o zaman imaj kökü sanılıyordu), sonra kurulu pakette bu makronun boş genişlediği
  ortaya çıktı — bkz. aşağıdaki Ubuntu bacağı düzeltmesi. Kalıcı çözüm `$APPDIR`'a göre
  **göreli** yazmak: `$APPDIR/../javafx` (Linux) ve `$APPDIR/../lib/javafx` (Windows),
  ikisi de `<imaj>/lib/javafx`'e çıkıyor.
  Ve `--enable-native-access=ALL-UNNAMED` JavaFX modül yüklemesiyle artık yetmiyor;
  `javafx.graphics`/`javafx.media` açıkça listelenmezse her açılışta `System::load`
  kısıtlama uyarısı çıkıyor.

  JavaFX lisansı artık JAR'ın içinde değil, yanında: `lib/javafx/legal/GPL-2.0-with-
  classpath-exception.txt` (tek kaynak: depodaki `legal/`) ve `.deb` copyright belgesine
  `Files: opt/jtypewriter/lib/javafx/*` stanza'sı eklendi. Bu arada belgede var olan bir
  hata düzeldi: DEP-5 **son** eşleşen stanza'yı uygular, dolayısıyla `Files: *` sonda
  durduğu sürece runtime ve JavaFX stanza'ları dekorasyondu; catch-all artık başta ve
  `verify_deb_license` stanza sırasını da denetliyor.

  Testler: `CiGatesTest` dört yeni kapı pinliyor — modül yolu platforma göre `$APPDIR`'a
  göreli kurulmalı (`$ROOTDIR` **yasak**), `lib/javafx` jpackage'a değil script'e
  yazdırılmalı (`--app-content` yasak, çünkü Windows'ta `legal/` düşüyor),
  `RUNTIME_MODULES` listesi `java.scripting` içerip
  `jdk.compiler`/`jdk.jshell`/`jdk.jconsole` içermemeli, `verify-artifacts.sh` imaj
  yerleşimini (runtime modül listesi + `lib/javafx`) denetlemeli ve `smoke-appimage.sh`
  "unnamed module" görürse düşmeli. Liste **satırı** denetleniyor,
  dosyanın tamamı değil: yorumlar dışlanan modüllerin adını anıyor, bir metin araması
  dışlamayı dışlamanın açıklamasından ayıramaz. Aynı sebeple modül yolu da tek satırda
  aranıyor: yorumlar `$ROOTDIR`'ı *neden yanlış* olduğunu anlatmak için anıyor.

  **CI düzeltmesi (Windows bacağı):** ilk hâlde `lib/javafx` jpackage'ın
  `--app-content` seçeneğiyle kopyalanıyordu. Linux'ta ağaç olduğu gibi geçti, Windows
  bacağında imaj `lib/javafx` içinde beş jar'ı taşıyıp `legal/` alt dizinini taşımadı ve
  `package.sh` kendi lisans assert'inde düştü (`missing from the application image: ...
/lib/javafx/legal/GPL-2.0-with-classpath-exception.txt`). Yerleşim artık sözleşme
  sayılıp script tarafından yazılıyor ve hemen ardından sayılarak doğrulanıyor: beş jar +
  `legal/GPL-2.0-with-classpath-exception.txt`. `CiGatesTest` bunu `--app-content`'in
  **yokluğunu** arayarak pinliyor — platforma göre değişen bir kopyalama davranışına
  güvenmek, lisansı Windows kurulum paketinden sessizce düşüren şeydi.

  Aynı koşu ikinci bir platform varsayımını da ortaya çıkardı: jpackage'ın imaj yerleşimi
  platforma göre değişiyor — Linux'ta `lib/app` + `lib/runtime`, Windows'ta imaj kökünde
  `app/` + `runtime/`. Script'ler tek yazımı sabitlediği için Windows'ta runtime ve uygulama
  jar'ı assert'leri yanlış yere bakacaktı; ikisi de artık `IMAGE_APP_DIR` /
  `IMAGE_RUNTIME_DIR` üzerinden okunuyor. `lib/javafx` ise iki platformda da aynı: onu
  jpackage değil script yazıyor ve `$ROOTDIR` her iki platformda imaj kökünü veriyor.
  `CiGatesTest.packageScriptKnowsBothAppImageLayouts` iki yerleşimin de dosyada
  yazılı olmasını istiyor — birini unutmak, yazıldığı platformda yeşil, diğerinde ölü.

  **CI düzeltmesi (Ubuntu smoke bacağı):** Windows düzeltmesinden sonra imaj iki kez
  sorunsuz açıldı, ama smoke'un üçüncü adımı — kurulu `.deb` başlatıcısı — düştü:
  `java.lang.module.FindException: Module javafx.controls not found`. Sebep tek bir
  makroydu: `--module-path=$ROOTDIR/lib/javafx`. jpackage `$ROOTDIR`'ı `imageRoot`'tan
  genişletir ve Linux başlatıcı kütüphanesi `imageRoot`'ı **yalnız** paketlenmemiş
  dalda kurar (`ownerPackage.name().empty()`). Kurulu pakette bunun yerine `dpkg -S` +
  `dpkg -L` ile app ve runtime dizinleri bulunur (`AppLauncherInitializer::apply` yalnız
  `setDefaultRuntimePath`/`setAppDir` çağırır, `imageRoot`'a dokunmaz), dolayısıyla
  `$ROOTDIR` boş genişler ve seçenek `/lib/javafx` olur — var olmayan bir mutlak yol.
  Aynı imaj doğrudan çalıştığında sorun yok, çünkü o dalda `imageRoot` gerçekten imaj
  kökü. Çözüm `$APPDIR`'a göre yazmak: Linux'ta `$APPDIR/../javafx` (app dizini
  `lib/app`), Windows'ta `$APPDIR/../lib/javafx` (app dizini `app`), ikisi de
  `<imaj>/lib/javafx`'e çıkıyor ve `$APPDIR` her iki dalda da kuruluyor.

  Bu hata sınıfını yerelde görebilmek için `smoke-appimage.sh`'in root'suz dalı artık
  kurulumu atlamıyor: `.deb` yükünü `dpkg-deb -x` ile açıp başlatıcısını, `dpkg -S`/`-L`
  sorgularına açılan ağacı gösteren bir `dpkg` stub'ı ile çalıştırıyor. Kurulu dalın
  anahtarı tam olarak bu iki sorgu olduğu için stub, gerçek kurulumun üreteceği yolları
  üretiyor; ayrıca `lib/libapplauncher.so` yükün içinde yoksa ayrı bir assert düşüyor.
  Root varsa eski kur/kaldır kontrolü aynen koşuyor. `CiGatesTest` üç şeyi pinliyor:
  modül yolu satırında `$ROOTDIR` **olmamalı** ve `$JFX_MODULE_PATH` olmalı; iki
  platformun `JFX_MODULE_PATH` yazımı da dosyada bulunmalı; `smoke-appimage.sh` root'suz
  durumda yükü gerçekten çalıştırmalı.

#### 3.2.2 B2 — yayın hattı kendi kapılarını koşsun ✅ Tamamlandı

`release.yml:132-141` yalnız `mvn package` + `package.sh` koşuyor; ikon `--check`,
`verify-artifacts.sh`, `tr-TR`/`de-DE` koşuları ve smoke **yok**. Tag doğrulanmamış
commit'e atılırsa yayın bunu bilmez.

- **İş:** yayın işini CI ile **aynı** kapılara bağla (kopyala-yapıştır değil: ortak iş/
  yeniden kullanılabilir workflow). `CiGatesTest` bu kapıları metin olarak pinliyor —
  kapı taşınırsa test de taşınmalı (test burada doğru davranıyor).
- **Kapı:** kasıtlı bozuk bir tag'de (ör. ikon hash'i bozuk) yayın **düşer**.
- **Efor:** AI ~3 saat; İNSAN 20 dk.

**Sonuç:** kapılar tek bir yeniden kullanılabilir workflow'a taşındı:
`.github/workflows/verify-build.yml` (`on: workflow_call`) ikon hash kontrolünü, derlemeyi,
`tr-TR`/`de-DE` koşularını, `package.sh --skip-build --installer`'ı, `verify-artifacts.sh`'i
ve smoke'u içeriyor. `maven.yml` ve `release.yml` artık **yalnız** bu işi çağırıyor; yayın
bacağı `version` girdisini geçtiği için aynı iş normalleştirilmiş `dist/` varlıklarını da
üretip yükleyen tek tanım oluyor. Kopyala-yapıştır yerine `uses:` seçilmesinin nedeni
yapısal: bir iş ya `uses:` çağrısıdır ya adım listesidir; yayın işi geri dönerse kapıları
atlayan ayrı bir liste olur, ve `CiGatesTest` bu dönüşü (`mvn -B package` / `package.sh`
metinlerinin yayın build bloğunda **yokluğunu**) pinliyor.

İki ayrıntı ölçümden geldi. **Birincisi:** varlık sahneleme doğrulama işinin *içinde*
kalmalı; bir artifact indirildiğinde Unix dosya kipleri kayboluyor (smoke işinin `chmod`
adımı bunu zaten kaydediyor), indirilen `releases/` ağacından `.tar.gz` üretmek
çalıştırılamayan bir başlatıcı yayınlardı. **İkincisi:** yayın işi artık `pattern:
JTypeWriter-dist-*` ile yalnız normalleştirilmiş varlıkları indiriyor; geniş desen, smoke
için yüklenen ham `releases/` ağaçlarını da `dist/`e karıştırıp beş-varlık assert'ini
gürültüyle geçirirdi.

Testler taşındı ve genişledi: `CiGatesTest` artık paylaşılan dosyayı okuyor, iki giriş
noktasının da onu çağırdığını, yayın build'inin **yalnız** çağrı olduğunu ve publish
işinin yalnız `dist` desenini indirdiğini denetliyor. `ReleaseNotesTest`'in
`retention-days` taraması elle sayılmış iki dosyadan tüm `.github/workflows/*.yml`
dosyalarına çevrildi: yeni bir workflow aksi hâlde 90 günlük varsayılanı sessizce
miras alabilirdi.

**Kapı kanıtı (yerel):** `jtypewriter-16.png`'ye bir bayt eklenince
`generate-icons.py --check` → `out of date` / çıkış 1, `git checkout` sonrası → 11 dosya
eşleşti / çıkış 0; yani bozuk ikonlu bir tag paylaşılan işin ilk adımında düşer.
`mvn clean test` → **640 test / 0 hata** (637'den +3 yeni `CiGatesTest` senaryosu).
CI'da gerçek bir tag koşusu bu değişiklikle henüz yapılmadı; kapı bağlantısı testle,
ikon düşüşü yerel script koşusuyla doğrulandı.


#### 3.2.3 B3/B4/B6/B7/B8/Q1 — olgunluk eksenleri (karar bağımlı)

| ID | İş | Karar/bağımlılık |
|---|---|---|
| B3 | `.msi` yükü bağımsız doğrulansın (legal metin, jar girişleri, sürüm) | Windows'ta ölçüm gerekir (bu makinede yapılmadı, §2.3) |
| B4 | Kod imzası + `SHA256SUMS` imzası | **§16.5 imza bütçesi** kararı |
| B6 | `arm64` ya tam desteklenir ya `release-assets.sh:62-68` + workflow eşlemesi kaldırılır | **§16.2 platform ekseni** kararı |
| B7 | jpackage/tar katmanında yeniden üretilebilirlik (normalize + bağımsız karşılaştırma) | `verify-artifacts.sh` bugün aynı koşunun kopyalarını karşılaştırıyor |
| B8 | JDK `'25'` yaması ve Maven runner'ı pinlensin | AGENTS "wrapper yok" kararı korunur; yama sürümü pinlenir |
| Q1 | SBOM + kapsam ölçümü (JaCoCo) + statik analiz kararı | "lint yok" bilinçli; SBOM ve kapsam bilinçli değil |

**P3.2 çıkış kapısı:** imaj ≤ ~75 MiB, "unnamed module" yok, yayın hattı kapıları koşuyor;
B3/B4/B6 kararları yazılı. Bu dalga **paketleme/dağıtım 2,0 → 4,0** gerekçesini üretir.

---

### P3.3 — Editör yetkinliği (karar kapısı arkasında)

Bu dalga **§16.3 ürün sınırı** ve **§16.4 çoklu belge** kararları verilmeden başlamaz.
Analiz §15 "editör yetkinliği 2,0" diyor ama §8.2 tablosunun her satırı farklı bir ürün
kararına bağlı; sırasız özellik yığmak P3'ün teşhisini (motor iyi, ürün eksik) çözmez.

#### 3.3.1 Karardan bağımsız, ucuz UX/erişilebilirlik

| ID | İş | Neden |
|---|---|---|
| U4 | durum çubuğu boyutu klavyeyle de değişsin + tooltip | bugün yalnız çift tıklama (`StatsBar.java:96-112`); klavye/ekran okuyucu yolu yok |
| U5 | arama paneline diyalog semantiği/odak tuzağı | `VBox` katmanı, Escape kapatıyor, odak tuzağı yok |
| U3 | menü hızlandırıcılarının modal diyalogla çelişkisini çöz | `MainMenu.java:61-64,407-421` |
| U7 | `replaceConfirmed` public/korumasız → YOLO rızası **tiple** korunsun | `EditorArea.java:129-131`; YOLO sözü bugün konvansiyonla ayakta |
| U9 | ses ilk tuşta yüklenmesin (açılışta ısıt) | soğuk ilk tuş gecikmesi (ölçülmedi, §9.5) |
| P4 | `-Xmx`/GC ayarı (`JTypeWriter.cfg`) | RSS ~300 MiB; önce P3.3.1'deki ısıtma/ölçümle birlikte ölç |

#### 3.3.2 Borç ve test güvenilirliği (A1, A7, U6)

- **A1 — `FileService` 998 satır, yedi sorumluluk.** Bölünme sırası: kurtarma → kodlama →
  çakışma → yedek. **Yalnız ihtiyaç doğduğunda**: §16.4 çoklu belge "evet" ise bu bölünme
  zorunlu (belge başına örnek), "hayır" ise yalnız okunabilirlik kazancı. Erken bölmek
  P3.1'in taze testlerini yeniden yazmak demek — **P3.1'den sonra**.
- **A7 — kaynak metni pinleyen testler.** `CiGatesTest`, `ReleaseNotesTest`,
  `EditorUndoContractTest`, `DialogSizingTest`, kısmen `MainControllerTest`. Hedef testi
  silmek değil, pinlediği şeyi **davranışa** indirmek (ör. "şu satır var mı" yerine
  "üretilen eylem kümesi bu mu"). Toolkit yasağı korunur.
- **U6 — arama iptali.** 64 MiB'de iptalsiz tarama kuyruğu tıkıyor (`FindPanel.java:269-302`).
  İptal bayrağı + "son tarama kazanır" kuralı; `FindModel`/`FindScanService` saf katmanda
  test edilir.
- **AGENTS/kod çelişkisi:** `AutoSaveServiceTest` `FileService`/`RecoveryStore` taklit
  ediyor, AGENTS "Mockito yalnız `Messages`/`DialogService`" diyor — ya kural ya test
  düzeltilir (§12.3).

#### 3.3.3 Özellik sırası (yalnız §16.3 "revizyon/teslim kapsama girer" derse)

Analiz §8.2 maliyetleri veriyor; en düşük maliyet/en yüksek değer sırasıyla:

1. **Kelime/oturum hedefi** (düşük) — FocusWriter'ın temel farkı, `SessionStats` zaten var.
2. **Odak/typewriter modu** (düşük–orta) — türün imza özelliği; ses var, ekran yok.
3. **Arama genişletmeleri** (düşük–orta) — regex, tam kelime, seçim içinde, tümünü vurgula;
   `FindModel` saf katmanı hazır.
4. **Ayar arayüzü** (düşük) — bugün her şey menü/araç çubuğu.
5. **Dışa aktarma + yazdırma/PDF** (orta) — `javafx.print` modülü imajda **yok**; P3.2'nin
   modül listesine eklenmeli (yoksa yazdırma çalışmaz → P1 ile çakışır, sıra önemli).
6. **Satır numarası/cetvel** (orta) — `TextArea` doğrudan vermiyor; editör bileşeni kararı.
7. **Markdown vurgulama** (orta) — rakip standardı, `.md` zaten açılıyor.
8. **Sözlük** (yüksek) — dil başına veri; en son.
9. **Çoklu belge/sekme** (yüksek) — **A1 bölünmesi + `FileService` belge başına örnek**
   şartı; mimarinin en büyük tek kısıtı (§6.3). Karar "hayır" ise kısıt **dokümante
   edilmeli** (bugün yalnız analizde yazıyor).

**P3.3 çıkış kapısı:** §16.3/§16.4 kararları yazılı; seçilen her özellik saf karar
sınıfı + testle gelir (toolkit yasağı); A7'de hedeflenen testler davranışa indirgenmiş.

---

### P3.4 — Talep ve sürdürülebilirlik (kod değil)

Analiz §4.4 ve §15 en sert sayıyı veriyor: **0 yıldız, 0 fork, 0 issue, 36 indirme**.
Bu, "teknik işin getirisini ölçülemez kılan" boşluk; P3'ün en pahalı ve en az kod içeren
kısmı.

| ID | İş | Not |
|---|---|---|
| M1 | Dağıtım kanalı kararı + uygulama: Flathub/AUR/winget/Chocolatey | **§16.7 talep stratejisi**; §16.1 kanal ekseni (kurulum vs taşınabilir) bunun önkoşulu |
| M3 | `CONTRIBUTING`, `CHANGELOG`, `SECURITY`, `CODE_OF_CONDUCT` | bugün hiçbiri yok; issue/PR şablonu da yok |
| M4 | Yayın notuna sürüm başına **değişiklik listesi** | şablon sürümden bağımsız sabit metin; `ReleaseNotesTest` iki dilli yapıyı korumaya devam etmeli |
| M5 | Bus factor = 1: sürüm anahtarları, imza, paketleme bilgisi tek kişide | imza (B4) ve yayın (B2) kararlarıyla birlikte ele alınmalı |

**P3.4 çıkış kapısı:** bir dağıtım kanalı canlı, bir dış issue/PR akışı tanımlı, yayın notu
"ne değişti" sorusunu cevaplıyor. Ölçüt kod değil: indirme sayısı ve dış katkı.

---

## 4. Kapsama tablosu (analiz kimlikleri → dalga)

Hiçbir bulgu düşmemeli. `S1`/`S2`/`S3` sınıfları analiz §14'ten alınmıştır.

| ID | Sınıf | Dalga | Not |
|---|---|---|---|
| B1 | S1 | P3.0 | paketleme kilidi |
| M2 | S2 | P3.0 | README/NOTICE/javadoc |
| U2 | S2 | P3.0 | WPM tanımı |
| U8 | S3 | P3.0 | pencere başlığı |
| B5 | S3 | P3.0 | rozet sayısı |
| D5 | S2 | P3.0 | retention `break` |
| D6 | S2 | P3.0 | sessiz DISCARD |
| D13 | S3 | P3.0 | `user.home` |
| D14 | S3 | P3.0 | `SecurityException` |
| D2b | S3 | P3.0 | `NAME_MAX` |
| U1 | S1 | P3.0 (yapılmayacak) | menü/araç çubuğu — ürün kararı: menü çubukla gizli kalır (§3.0.3) |
| D1 | S1 | P3.1 | taslak yaşlandırma |
| D2 | S1 | P3.1 | yol sızıntısı + FX G/Ç |
| D3 | S1 | P3.1 | kodlama kurtarma |
| D4 | S1 | P3.1 | kayıplı "kayıpsız" |
| D7 | S2 | P3.1 | uyarı zamanlaması |
| D9 | S2 | P3.1 | zamanlayıcı |
| D8 | S3 | P3.1 | taslak biçimi |
| D10 | S3 | P3.1 | tmp artıkları |
| D4b | S3 | P3.1 | sahiplik/0600 |
| D4c | S3 | P3.1 | sarkan symlink |
| D4d | S3 | P3.1 | boş alan |
| A4 | S2 | P3.1 | ayar eşzamanlılığı |
| D11 | S2 | P3.1 | karantina |
| D12 | S2 | P3.1 | sürüm göçü |
| P1 | S1 | P3.2 | jlink |
| P2 | S2 | P3.2 | unnamed module |
| B2 | S1 | P3.2 | yayın kapıları |
| B3 | S2 | P3.2 | .msi |
| B4 | S2 | P3.2 | imza |
| B6 | S3 | P3.2 | arm64 |
| B7 | S3 | P3.2 | tekrarlanabilirlik |
| B8 | S3 | P3.2 | toolchain pin |
| Q1 | S3 | P3.2 | SBOM/kapsam |
| U4 | S2 | P3.3 | durum çubuğu |
| U5 | S3 | P3.3 | arama paneli |
| U3 | S2 | P3.3 | hızlandırıcı |
| U7 | S3 | P3.3 | `replaceConfirmed` |
| U9 | S3 | P3.3 | ses ısıtma |
| P4 | S3 | P3.3 | `-Xmx` |
| A1 | S2 | P3.3 | `FileService` |
| A7 | S2 | P3.3 | test pinleri |
| U6 | S2 | P3.3 | arama iptali |
| M1 | S1 | P3.4 | talep |
| M3 | S3 | P3.4 | topluluk dosyaları |
| M4 | S3 | P3.4 | yayın notu içeriği |
| M5 | S3 | P3.4 | bus factor |

`S1` sayısı analizde 9'dur (B1, D1, D2, D3, D4, U1, P1, B2, M1); hepsi burada ve
**hiçbiri P3.1'den sonraya bırakılmamıştır**, iki istisnayla: M1 kodla kapanmıyor, U1 ise
ürün kararıyla **yapılmayacak** olarak işaretlendi — bulgu analizde kalıyor, sıradan
düşmüyor.

---

## 5. Efor ve zaman

Efor, işin **AI tarafı** ile **insan/dış dünya tarafı** ayrılarak verilir; toplamı baskın
taraf belirler. Bu ayrım, analizin "kod yazma hızıyla değil, dağıtım ve talep ekseniyle
kapanır" teşhisinin doğrudan sonucudur.

| Dalga | AI tarafı | İnsan/dış dünya tarafı | Baskın | Toplam (gerçekçi aralık) |
|---|---|---|---|---|
| P3.0 | ~yarım gün (B1, M2, U2, hijyen) | ~30 dk karar/gözden geçirme (U1 kapsam dışı, görsel onay yok) | AI | **yarım gün** |
| P3.1 | ~1,5 gün (D1–D4 çekirdek + kümeler) | D1 yaş politikası, D4 kayıp onayı (2 karar, 1 saat) | AI | **1,5–2,5 gün** |
| P3.2 | ~1 gün (jlink/modül + yayın hattı) | **iki platformda smoke ve Windows ölçümü**; imza bütçesi kararı | dış dünya | **2–4 gün** (CI/paketleme beklemesi baskın) |
| P3.3 | ~1 gün (UX borç + test indirgeme) + özellik başına 2–6 saat | §16.3/§16.4 kararları; sözlük/çoklu belge için haftalar | karara bağlı | **2 gün – birkaç hafta** |
| P3.4 | ~yarım gün (dosyalar, şablon) | kanal başvuruları, tanıtım, katkı akışı | İNSAN | **haftalar–aylar** (dış onay) |

Bu tablo takvim değildir; **sıra korunursa** her dalganın kapanış kapısı bir öncekine
dayanır. P3.0 + P3.1 birlikte, ürünün "vaadini tutan" hâle gelmesi için gereken en kısa
yoldur; P3.2 ölçülebilir tek büyük kazançtır (imaj ~%49); P3.4 olmadan P3.1–P3.3'ün
getirisi ölçülemez (analiz §4.4, §16.7).

---

## 6. Doğrulama planı

Analiz §17'deki protokol bu yol haritasının **tekrar kullanılabilir kapısıdır**; aşağıdaki
komutlar oradan alınmıştır, yenisi değildir.

| Dalga | Kanıt komutu | Beklenen |
|---|---|---|
| P3.0 | `./scripts/package.sh --skip-build --installer; echo $?` (iki kez) | `0` / `0`, tek `.deb` |
| P3.0 | `./scripts/verify-artifacts.sh --skip-build; echo $?` | `0` |
| P3.0 | `mvn -B clean test \| tail -20` | 575+ yeşil, `@Disabled` yok |
| P3.1 | `mvn test -Dtest=EncodingMatrixTest,RecoveryStoreTest,AutoSaveServiceTest,FxThreadIoTest` | yeni senaryolar yeşil |
| P3.1 | `mvn test -Dtest=ProcessCrashRecoveryTest` (tek başına) | gerçek `SIGKILL` kurtarma hâlâ geçer |
| P3.2 ✅ | `du -sb releases/linux/JTypeWriter` | ≤ ~75 MiB (ölçüldü: 147 MiB → **73 MiB**) |
| P3.2 ✅ | `grep -c 'unnamed module' <açılış logu>` (3 koşu) | `0` (ölçüldü: 3/3 koşuda `0`) |
| P3.2 ✅ | `dpkg-deb -f ...deb Installed-Size` | belirgin düşüş (ölçüldü: 48 MiB → **44 MiB**, `Installed-Size` 73 996 KiB) |
| P3.3 | özellik başına `mvn test -Dtest=<SafKararSınıfı>Test` | toolkit'siz, hızlı |
| P3.4 | `curl -s .../releases \| grep download_count` + kanal sayfası | tablo değişiyor mu |

**Kapı disiplini:** her dalga sonunda `mvn clean test` + ilgili betik; kırmızı varsa
sonraki dalga başlamaz. CI üç locale koşuyor (575 × 3 = 1.725) — bu sayı **test sayısı
değildir**, rozet düzeltmesi (B5) bunu görünür kılmalı.

---

## 7. Karar kapıları (analiz §16, sıralı bağımlılıkla)

Bunlar analizle kapanmaz; her biri belirli bir dalgayı **açar veya kısıtlar**.

| # | Karar | Kilitlediği iş | Öneri (analizden) |
|---|---|---|---|
| 1 | Kanal: kurulum paketi mi, taşınabilir arşiv mi? | P3.2 (B4, B7), P3.4 (M1) | Analiz karar bırakıyor; **önce bu** çünkü imza/ilk çalıştırma/güncelleme buna bağlı |
| 2 | Platform: arm64 desteklenecek mi? | P3.2 (B6) | Desteklenmeyecekse `arm64` kabulü **kaldırılmalı** (bugün yarım: betik kabul, assert `-x64`) |
| 3 | Ürün sınırı: "taslak aracı" mı, revizyon/teslim mi? | P3.3 tamamı | Kapsam dışı kalacaksa §8.2 tablosu ürün dokümanına yazılmalı |
| 4 | Çoklu belge/sekme? | P3.3 (A1, §6.3) | "Evet" ise `FileService` belge başına örnek **zorunlu**; "hayır" ise kısıt dokümante edilmeli |
| 5 | İmza bütçesi (Windows Authenticode) | P3.2 (B4) | Alınmazsa SmartScreen ilk izlenim olmaya devam eder |
| 6 | Ağ politikası: "hiç ağa çıkmama" korunacak mı? | P3.2 (güncelleme), P3.4 | Güncelleme isteniyorsa ilke yazılı yeniden tanımlanmalı |
| 7 | Talep stratejisi: kanallar/tanıtım/altyapı | P3.4 (M1) | Bu karar verilmezse teknik işin getirisi ölçülemez |
| 8 | Lisans/ticari model | P3.2 (imza maliyeti), P3.4 | Apache-2.0 kalıcı mı, bağış/ticari destek ekseni mi? |

**Kritik bağımlılık:** 1↔2↔5 birbirine bağlı (kanal → platform → imza). Bu üçü
verilmeden P3.2'nin yarısı başlatılamaz; 3↔4 ise P3.3'ün tamamını kilitler.

---

## 8. Yapılmayacaklar (bilinçli)

- **Yeni özellik yarışı.** Analiz §15: fark kod yazma hızıyla değil, dağıtım ve talep
  ekseniyle kapanır. P3.0–P3.2 bitmeden özellik eklemek, "motor iyi, ürün eksik"
  teşhisini güçlendirir.
- **Toolkit başlatan test.** AGENTS ve analiz §12.1 aynı yerde: toolkit yasağı mimari
  kültürün parçası. Yeni UI davranışı **saf karar sınıfına** indirilerek test edilir.
- **Kapsam ölçümünü kapı yapmak.** Q1'de kapsam ölçümü istenir, ama eşik/kapı kararı
  verilmeden yalnız rapor üretilir; JaCoCo'yu zorunlu kapı yapmak bu ağacın test
  felsefesiyle çelişir.
- **`FileService`'i erken bölmek.** P3.1 taze testler üretmeden bölmek, aynı davranışı
  iki kez kanıtlamak demek; bölünme P3.3'e (ve §16.4 kararına) bağlı.
- **macOS geri getirmek.** AGENTS açık: platform kararı olmadan `Darwin*`, `.dmg`,
  `.icns` eklenmez. §13.2'deki ICNS/NOTICE düzeltmesi bunun tersi yönde değil,
  **artığı temizleme** yönündedir.

---

## 9. Tek paragraf

P3, ürünün kendi vaadini tutmadığı yerlerden başlar: bir satırlık `.deb` glob'u (B1)
sürümü fiilen kırıyor, bayat README/NOTICE/javadoc yanlış bilgi veriyor, gizlenen araç
çubuğu menüyü de götürüyor (U1 — ürün kararıyla korunuyor, yapılmayacak), WPM yanlış şeyi
sayıyor (U2) — bunlar saatler içinde
kapanır ve güveni en ucuza geri verir. Ardından veri güvenliğinin dört S1 deliği (D1–D4)
ve küçük kurtarma/ayar kümeleri kapatılır; bu dalga, ürünün en güçlü olduğu ekseni
tartışmasız hâle getirir. Sonra ölçülmüş %49'luk imaj kazancı (P1+P2) ve yayın hattının
kendi kapılarını koşması (B2) gelir — burada iş AI hızında değil, iki platformda smoke ve
imza bütçesi kararı hızında ilerler. Editör yetkinliği ve talep (P3.3–P3.4) ise ürün
sahibinin vereceği üç karara (kanal, ürün sınırı, platform) bağlıdır; o kararlar
verilmeden bu dalgalar başlamamalıdır.
