# JTypeWriter — Sıfırdan Analiz ve Ürünleştirme Değerlendirmesi (P3 Girişi)

> **Bu belge bir yol haritası değildir.** Sıralama, sahiplik, efor, takvim ve çıkış
> kapıları burada yoktur. Belge yalnızca *ölçüm*, *yapı*, *kusur* ve *karar ihtiyacı*
> üretir; P3 planlaması bunun üzerine ayrı bir belgede kurulacaktır.
>
> **Yöntem:** Her hüküm ya bu makinede alınmış bir ölçüme, ya `dosya:satır` düzeyinde
> okunmuş koda, ya da çalıştırılmış bir betiğe bağlanmıştır. Ölçülmemiş iddialar açıkça
> `ölçülmedi` olarak işaretlenmiştir (§2.3). Üç bağımsız kod okuma hattı (servis/veri,
> UI/özellik, derleme/CI/paketleme) paralel yürütülmüş, bulguların kritik olanları
> bu oturumda bizzat yeniden üretilmiştir.
>
> **Önceki fazlarla ilişki:** Ürün P0, P1 ve P2 ürünleştirme fazlarından geçti; bu
> belge o fazların kartlarını, kimliklerini veya kapanış iddialarını **devralmaz**.
> P0/P1/P2 yalnızca "bugünkü ağacın hangi sırayla bu hâle geldiğini" açıklayan bir
> bağlam olarak §3'te tek paragrafta anılır. Aşağıdaki bulgu kimlikleri (`D`, `U`, `P`,
> `B`, `Q`, `M`) bu analize aittir ve daha önceki hiçbir kimlikle eşleştirilmemelidir.

- **Sürüm:** `1.1.0` (`pom.xml:9`), tag `v1.1.0` (2026-11-08)
- **Kod tabanı:** `main` @ `338f1c4`, 195 commit, ilk commit 2026-07-02
- **Lisans:** Apache-2.0; bağımlılıklar GPLv2+CPE / LGPL-2.1 / Bitstream Vera karışımı
- **Platform:** Linux x64 ve Windows x64 (yalnız)
- **Ölçüm ortamı:** Linux x64 (Ubuntu), X11 `DISPLAY=:0.0`, JDK 25.0.2 (Corretto),
  Maven 3.9.12, sıcak dosya önbelleği; artefakt ölçümleri bu oturumda üretilmiş taze
  build üzerinde alınmıştır (§17).

---

## 1. Yönetici Özeti

### 1.1 Tek paragraf

JTypeWriter, **veri kaybını önleme ve yalın yazma deneyimi ekseninde gerçekten olgun,
ama ürün olarak hâlâ tek kişilik bir zanaat nesnesi** olan bir masaüstü düz metin
editörüdür: kaydetme yolu atomik ve fsync'li, kodlama koruması gerçek (BOM'suz UTF-16
dahil), çökme kurtarması süreç öldürme testiyle kanıtlanmış, 575 testlik bir kapı var ve
bu kapı JavaFX toolkit'i başlatmadan 7,4 saniyede koşuyor. Buna karşılık ürün, **dağıtım
ve güven katmanında** zayıftır: 146 MiB'lik uygulama imajı gereğinden ~2 kat büyüktür,
kurulum paketleri imzasızdır, güncelleme kanalı yoktur, yayın hattı kendi kalite
kapılarını yeniden koşmaz, sürüm paketleyicisi bu makinede **yeniden üretilebilir biçimde
kırılır**, ve ürünün dış dünyadaki karşılığı ölçülebilir biçimde sıfırdır (0 yıldız, 0
fork, 0 issue, tüm sürümlerde toplam 36 varlık indirmesi). Editör yetkinliği açısından
da ürün "yaz" adımını iyi, "gözden geçir ve teslim et" adımlarını zayıf karşılıyor:
yazdırma, dışa aktarma, sözlük, çoklu belge, satır numarası ve arama genişletmeleri yok.

### 1.2 Ölçülen tablo (hepsi bu makinede, bu ağaçta)

| Ölçüt | Değer | Kanıt |
|---|---|---|
| Dil / UI / test | Java 25, JavaFX 26.0.1, JUnit Jupiter 6.0.3, Mockito 5.23.0 | `pom.xml:52,62-64` |
| Üretim kodu | **72 java dosyası, 13.495 satır** (7.407 kod, 4.576 yorum, **%33,9 yorum**; 684 javadoc bloğu) | `find`/satır sayımı |
| Test kodu | **63 dosya, 61 test sınıfı + 2 yardımcı, 575 `@Test`, 12.018 satır** (üretime oran 0,89) | `grep -c @Test`, `find` |
| Test sonucu | **575 koştu / 0 hata / 0 atlandı**, `@Disabled` yok | `mvn -B clean test` |
| Test duvar saati | **7,4 s** (`mvn clean test`, sıcak `~/.m2`) | `BUILD SUCCESS` çıktısı |
| Yerelleştirme | **6 bundle × 156 anahtar**, iki yönlü parite testli | `messages*.properties`, `MessagesTest` |
| CSS | 980 satır (`base.css` 812 + 3 tema × ~55) | `wc -l` |
| İnce JAR | 1.276.630 B = **1,2 MiB** | `target/jtypewriter-1.1.0.jar` |
| Şişman JAR | 11.796.287 B = **11,2 MiB**, 6.167 giriş, 5,7 MiB JavaFX `.so` | `unzip -l` |
| Uygulama imajı | 153.255.307 B = **146,2 MiB**, 126 dosya | `du -sb` |
| ├ Çalışma zamanı | 139.967.672 B = **133,5 MiB = %91,3**, **52 modül** | `lib/runtime/release` |
| └ Uygulama JAR'ı | 11,2 MiB | `lib/app/` |
| `.deb` | 50.242.466 B = **47,9 MiB**, `Installed-Size` 150.696 KiB, **12** bağımlılık | `dpkg-deb -f` |
| Çalışma zamanı ölü ağırlık | `lib/modules` 86,4 MiB, `libjvm.so` 27,4 MiB, `ct.sym` **10,4 MiB** | `ls -l` |
| jlink ile asgari çalışma zamanı | **71.553.083 B = 68,2 MiB** (ölçüldü, aynı JDK) | §10.4 |
| Başlatma — ana pencere | **1,63 / 1,65 / 1,66 s** (3 koşu, taze profil) | §9.1 |
| Bellek (RSS) | **298–300 MiB** (pencere anında), 318–321 MiB (1 s sonra) | §9.1 |
| 10 MiB atomik kaydetme | **10,4 ms** (fsync dahil, sıcak NVMe) | §9.2 |
| 10 MiB okuma / kod çözme / SHA-256 | **2,2 ms / 20,2 ms / 11,3 ms** | §9.2 |
| İstatistik yenilemesi (10 MiB) | **0,116–0,123 ms** (eşik 50 ms) | `StatsPerfHarnessTest` |
| Yayın | **5 tag, 5 Release, toplam 36 varlık indirmesi** (v1.1.0: 6) | GitHub API |
| Topluluk | **0 yıldız, 0 fork, 0 açık issue, 0 katkıcı (bot dışında)** | GitHub API |
| TODO/FIXME/XXX | **0** | `grep` |
| İmzalama / SBOM / kapsam ölçümü / statik analiz | **yok / yok / yok / yok** | §10.6, §12.3 |

### 1.3 En önemli on iki bulgu

1. **B1 — Sürüm paketleyicisi bu makinede deterministik olarak kırılıyor.** `package.sh`
   eski `.deb` dosyalarını `"$DEST/$APP_NAME"*.deb` ile silmeye çalışıyor, ama
   `APP_NAME="JTypeWriter"` (`package.sh:24`) iken jpackage paketi **küçük harfle**
   `jtypewriter_1.1.0_amd64.deb` yazıyor — bunu betiğin kendi yorumu da söylüyor
   (`package.sh:354`). Temizlik hiçbir zaman eşleşmiyor; ikinci sürümden itibaren
   `package.sh:420-425` "expected exactly one .deb ... found 2" diyerek **tüm paketlemeyi
   düşürüyor**. Bu oturumda bizzat üretildi: `./scripts/package.sh --skip-build
   --installer` → `installer written ... (deb)` ardından `expected exactly one .deb under
   releases/linux/, found 2`, çıkış kodu 1. Aynı varsayım `verify-artifacts.sh:180-186`
   içinde de var, o da aynı gerekçeyle düşüyor. Depoda duran
   `releases/linux/jtypewriter_1.0.2_amd64.deb` (47,9 MiB) bu kusurun kanıtı.
2. **D1 — "Çökme artığı" canlı taslaklar hiç temizlenmiyor.** `RecoveryRetention` yalnız
   `pending-*` dosyalarını yaşlandırıyor (`RecoveryRetention.java:25-29`,
   `RecoveryStore.java:240-248`); `draft-<uuid>.jtw` dosyaları yalnız *en yeni* olan
   benimsendiğinde siliniyor (`RecoveryStore.java:226-231,337-347`). Her çökme kalıcı bir
   dosya bırakır; kurulum ömrü boyunca `~/.jtypewriter/recovery/` **sınırsız büyür** —
   ve bu dosyalar kullanıcı metnini taşır.
3. **D2 — Kurtarma yolunda iki sessiz sınıf hatası.** `restoreRecovered`, dosya yoksa veya
   `file == null` ise `currentFilePath`'i **önceki belgenin yolu olarak bırakıyor**
   (`FileService.java:453-493`; atama yalnız `:482` içindeki dalda). Aynı metot orijinali
   `Files.readAllBytes` ile **boyut sınırı olmadan ve FX thread'inde** okuyor (`:458`) —
   64 MiB belge politikasıyla ve "disk işi yalnız `IoExecutor`'da" doktriniyle çelişiyor.
4. **D3 — BOM'u bozuk orijinalden kurtarma, varsayılan kodlamayı koruyor.**
   `MalformedDocumentException` dalı `currentFilePath`'i atıyor ama `currentCharset`/
   `currentBom`'a dokunmuyor (`FileService.java:475-481`); oturum başında bunlar UTF-8
   olduğu için eski (ör. windows-1254) belge bir sonraki kayıtta **sessizce UTF-8'e
   dönüşebilir** — ürünün "kodlama korunur" vaadinin doğrudan istisnası.
5. **D4 — "Kayıpsız UTF-8" seçeneği kayıplı.** Bilinmeyen kodlama diyaloğunda UTF-8
   seçildiğinde `TextCodec.decodeWith(..., REPLACE)` kullanılıyor
   (`FileService.java:829-833`, `TextCodec.java:265-275`) ve geçersiz baytlar `U+FFFD`
   oluyor; oysa aynı diyaloğun metni UTF-8'i **kayıpsız** seçenek olarak sunuyor
   (`messages*.properties`, `encodingUtf8`). Kullanıcı rızasıyla kalıcı bozulma.
6. **U1 — Araç çubuğu gizlenince menü de kayboluyor.** Komut menüsü araç çubuğunun
   *içinde* (`MainController.java:294-296`); `Ctrl+T` ile çubuk gizlendiğinde geri
   getirmenin tek yolu yine aynı kısayoldur. Fare kullanan bir kullanıcı için bu, geri
   dönüşü olmayan bir kilitlenme görüntüsüdür.
7. **P1 — Uygulama imajı ~2 kat şişkin.** 52 JDK modülü taşınıyor; `jdeps` yalnız
   `java.base, java.desktop, java.logging, javafx.controls, javafx.fxml, javafx.media`
   istiyor. Bu oturumda aynı JDK ile `jlink --strip-debug --compress=zip-6` denemesi
   **68,2 MiB** verdi (mevcut 133,5 MiB) — %49 azalma, hiçbir özellik kaybı olmadan.
   Ayrıca `ct.sym` (10,4 MiB) ve `jdk.compiler`/`jdk.jshell`/`jconsole` gibi hiç
   kullanılmayan modüller imajda.
8. **P2 — JavaFX her açılışta "unnamed module" uyarısı üretiyor.** `lib/runtime/release`
   içindeki 52 modül arasında **hiç `javafx.*` yok**; JavaFX sınıfları şişman JAR'dan
   classpath üzerinden yükleniyor ve JVM her başlatmada
   `Unsupported JavaFX configuration: classes were loaded from 'unnamed module'`
   yazıyor (3/3 koşuda `/tmp` loglarında). Bu aynı zamanda jpackage'ın jlink
   optimizasyonunu devre dışı bırakmasının da nedeni.
9. **B2 — Yayın hattı, CI'ın kalite kapılarını yeniden koşmuyor.** `release.yml` yalnız
   `mvn package` + `package.sh` koşuyor (`release.yml:132-141`); ikon `--check`,
   `verify-artifacts.sh`, `tr-TR`/`de-DE` yerel ayar koşuları ve smoke işi **yok**. Tag,
   bu kapılardan geçmemiş bir commit'e atılırsa yayın bunu fark etmez. Üstelik
   `.msi` yükü hiçbir yerde bağımsız doğrulanmıyor (`verify-artifacts.sh:170-175`).
10. **M1 — Ürünün dış karşılığı sıfır.** GitHub API: 0 yıldız, 0 fork, 0 açık issue,
    5 Release, **tüm sürümlerde toplam 36 varlık indirmesi**. README, CI, yayın notu ve
    lisans disiplini bir ürünün *üretim* tarafının hazır olduğunu gösteriyor; *talep*
    tarafında ölçülebilir hiçbir kanıt yok. Bu, kodla kapatılabilecek bir boşluk değil.
11. **U2 — WPM "vuruş/5" hesaplıyor, "karakter/5" değil.** `SessionStats.java:173-178`
    paydası `totalKeystrokes`; yapıştırma, IME ve otomatik düzeltme hızı hiç artırmıyor,
    silip yeniden yazmak ise şişiriyor. Aynı zamanda oturum süresi boşlukları sayarken
    WPM 5 s üstü boşlukları saymıyor; iki sayı farklı şeyleri ölçtüğü için ekranda
    tutarsız görünebiliyor.
12. **Q1 — Testler davranışı değil, çoğunlukla metni pinliyor.** `CiGatesTest`,
    `ReleaseNotesTest`, `EditorUndoContractTest`, `DialogSizingTest` ve
    `MainControllerTest`'in bir kısmı workflow/kaynak **metninde** dize arıyor. Bu
    bilinçli bir tasarım (toolkit yasağı), ama refactor'ları işlevsiz nedenlerle
    kırabiliyor ve gerçek UI regresyonlarını yakalamıyor; görsel/etkileşim katmanının
    tek gerçek kapısı `smoke-appimage.sh`.

---

## 2. Yöntem, Kanıt Düzeyleri ve Sınırlar

### 2.1 Kanıt seviyeleri

| Seviye | Anlamı | Bu belgede kullanımı |
|---|---|---|
| **A** | Bu makinede komut çalıştırıldı, çıktı belgede | Test, paketleme, başlatma, bellek, G/Ç, jlink, CLI, tek örnek, GitHub API |
| **B** | Kod `dosya:satır` düzeyinde okundu, davranış çıkarıldı | Mimari, veri akışları, hata yolları |
| **C** | Kaynak metni/veri yapısı üzerinden çıkarım | Testlerin neyi pinlediği, doküman-kod farkları |
| **D** | Ölçülmedi, açıkça işaretli | Gerçek UI gecikmeleri, Windows davranışı, kurulum deneyimi |

### 2.2 Bu analizin önceki belgelerden farkı

Bu belge, geçmiş analizlerin bulgu kimliklerini, skor tablolarını veya "P2'de şu kapandı"
hükümlerini **taşımaz**. Ağaç bugün 1.1.0'dır; önceki belgelerin dayandığı commit'lerden
bu yana paketleme, arayüz kartları, arama paneli, ayar şeması ve ses seviyesi değişti.
Burada yalnız bugünkü kod ve bugünkü ölçüm vardır. Önceki faz adları (P0/P1/P2) yalnız
§3'te tarihsel bağlam olarak geçer.

### 2.3 Ölçülmeyenler (dürüstlük notu)

- **Gerçek etkileşim gecikmeleri** (tuş basışından ekrana yansıma, arama paneli açılışı,
  büyük belgede kaydırma) ölçülmedi; JavaFX toolkit'i başlatmadan ölçülemez ve bu
  ortamda görsel doğrulama yapılmadı. Yalnız pencere eşleme süresi ve RSS ölçüldü.
- **Windows** tarafında hiçbir şey çalıştırılmadı; `.msi`, SmartScreen, dosya ilişkilendirme
  ve Windows yol davranışı yalnız kod/test düzeyinde (B/C) değerlendirildi.
- **Kurulum deneyimi** (dpkg kurulumu, menü kaydı, kaldırma) bu oturumda koşulmadı;
  `smoke-appimage.sh`'in iddiaları C seviyesindedir.
- **Erişilebilirlik** ekran okuyucu ile test edilmedi; yalnız `setAccessible*` sayımı,
  kontrast testleri ve odak sırası kaynak düzeyinde incelendi.
- **Pazar/talep** yalnız kamuya açık metriklerle (yıldız/indirme) değerlendirildi; pazar
  araştırması bu belgenin kapsamı dışıdır, rakip konumlandırması §4.3'te nitelikseldir.

---

## 3. Tarihsel Bağlam (yalnız referans)

Ağaç 2026-07-02'de başladı ve 195 commit'te bugünkü hâline geldi; commit yoğunluğu
2026-10 (%61) ve 2026-11 (%32) aylarında. Katkı neredeyse tamamen tek kişiliktir
(191 insan commit'i, 23 rozet botu, 1 Dependabot). Ürünleştirme üç fazda yürüdü: **P0**
görünür arayüz ve kaydetme sağlamlığı, **P1** veri güvenliği ve platform/dağıtım
temelleri, **P2** donmayan editör yüzeyi, yayın hattı ve cila. Bu fazların kart
kimlikleri, kapanış iddiaları ve o dönemin ölçümleri bu belgeye taşınmamıştır; bugünkü
davranış yalnız bugünkü koddan yeniden türetilmiştir. Fazların ortak mirası, kalıcı
mimari kültürdür: saf karar sınıfları, toolkit'siz test, tek kaynaklı sürüm/ikon/font,
ve "neden böyle" yazan javadoc.

---

## 4. Ürün Tanımı ve Konumlandırma

### 4.1 Ne satıyor?

Üç iddia: (1) **kaybetmeyen taslak** — YOLO modu silmeyi kapatır, her kayıt atomik,
dönen yedekler var, 30 s'de bir taslak, çökmeden sonra geri teklif; (2) **yazma hissi** —
daktilo tuş sesi, üç tema, altı metin rengi, yalın arayüz; (3) **sayan masa** — kelime,
karakter, satır, vuruş, süre ve WPM.

Bunlar tek bir kullanıcı tipine hitap ediyor: **çevrimdışı, tek dosya, düz metin üzerinde
ilk taslağını yazan kişi**. Ürün bunu tutarlı biçimde yapıyor; "proje yönetimi", "bulut",
"hesap" gibi vaatler hiç vermiyor. Konumlandırma netliği, ürünün en güçlü ticari
yanı.

### 4.2 Kimin için değil?

Kod, ürünün ne olmadığını da net biçimde gösteriyor: Markdown render/önizleme yok,
dışa aktarma yok, yazdırma yok, eşzamanlama yok, çoklu belge yok, eklenti yok, komut
paleti yok, sözlük yok. Yani yazarın **revizyon ve teslim** aşaması bilinçli olarak
kapsam dışı; ürün "taslak" aracı. Bu, FocusWriter'ın (ücretsiz, Win/Linux, düz metin)
konumuyla birebir örtüşüyor ve iA Writer/Typora'nın (Markdown + dışa aktarma) bir
alt kümesi.

### 4.3 Rekabet resmi (niteliksel, ölçülmedi)

| Ürün | Fiyat | Platform | Düz metin | Ayırt edici |
|---|---|---|---|---|
| **JTypeWriter** | ücretsiz (Apache-2.0) | Linux, Windows | ✔ | daktilo sesi + YOLO + çökme kurtarma |
| FocusWriter | ücretsiz (GPL) | Linux, Windows | ✔ | hedefler, zamanlayıcı, tam ekran sahne |
| iA Writer | ~30–50 USD/platform | Win, macOS, iOS | ✔ (Markdown) | odak modu, stil denetimi, yazılan/yapıştırılan ayrımı |
| Typora | ~15 USD | Win, macOS, Linux | ✔ (Markdown) | canlı Markdown, PDF/docx/ePub dışa aktarma |
| WriteMonkey / Calmly | ücretsiz–15 USD | Win, Linux | ✔ | özelleştirme / sadelik |

JTypeWriter'ın savunulabilir tek farkı **YOLO + kurtarma paketi**: silmeyi yasaklayan bir
mod ve süreç öldürme testiyle kanıtlanmış taslak kurtarma, rakiplerin hiçbirinde bu
kombinasyonda yok. Geri kalan her şey (ses, temalar, sayaçlar) taklit edilebilir.

### 4.4 Talep kanıtı — en büyük boşluk

| Metrik | Değer | Kaynak |
|---|---|---|
| Yıldız / fork / izleyici | **0 / 0 / 0** | GitHub API |
| Açık issue / PR | 0 / 0 | GitHub API |
| Release sayısı | 5 (v1.0.0 → v1.1.0) | GitHub API |
| Toplam varlık indirmesi | **36** (v1.1.0: 6; v1.0.0: 12) | GitHub API |
| Depo boyutu | ~3,0 MB | GitHub API |

Beş sürüm boyunca 36 indirme, sürüm başına ~7 indirme demek; bunun bir kısmı CI/doğrulama
trafiği olabilir. Ürün **teknik olarak yayınlanabilir**, **pazar olarak görünmez**
durumda. Bu, P3'ün en pahalı ve en az kod içeren problemidir; analiz belgesi bunu
kayda geçirmekle yetinir.

---

## 5. Ölçülen Şekil: Kod, Test, Artefakt

### 5.1 Kod dağılımı

| Paket | Dosya | Satır | İçerik |
|---|---|---|---|
| kök (`AppGraph`, `AppSettings`, `AtomicFiles`, …) | 12 | 2.286 | kompozisyon kökü, ayar, G/Ç ilkelleri, tek örnek |
| `service/` | 12 | 3.364 | dosya, kodlama, otomatik kayıt, kurtarma, yedek, diyalog |
| `ui/` | 26 | 4.170 | denetleyici, menü, kartlar, arama paneli, eylem envanteri |
| `ui/component/` | 9 | 1.839 | editör, araç çubuğu, durum çubuğu, YOLO/sığdırma |
| `ui/theme/` | 5 | 371 | tema, renk, yazı boyutu |
| `locale/` | 3 | 911 | dil, mesaj erişimcisi (156 anahtar × erişimci) |
| `stats/` | 2 | 279 | artımlı sayaçlar, WPM |
| `audio/` | 3 | 275 | tuş sesi, seviye, politika |

Yorum oranı **%33,9** ve javadoc blokları gerekçe anlatıyor ("neden X değil"). Bu, kod
okunabilirliğini yükseltiyor; bedeli, aynı gerekçelerin zamanla **bayatlaması** (bkz.
§13.2: javadoc'larda hâlâ "10 MB" yazıyor, gerçek sınır 64 MiB).

### 5.2 Sınıf boyutu ve borç eşiği

| Dosya | Satır | Değerlendirme |
|---|---|---|
| `service/FileService.java` | **998** | kaydet/aç/kodla/çakışma/kurtarma tek sınıfta; en riskli sınıf |
| `locale/Messages.java` | 822 | 156 erişimci; mekanik, kabul edilebilir |
| `ui/component/ToolbarView.java` | 767 | altı açılır liste + sığdırma + tema; bölünebilir |
| `AppSettings.java` | 669 | 22 anahtar + göç + karantina; bölünebilir |
| `service/RecoveryStore.java` | 560 | dosya biçimi + yaşlandırma + tarama |
| `ui/FindPanel.java` | 502 | kart + tarama iş birliği |
| `service/DialogService.java` | 473 | tüm Alert/FileChooser trafiği |
| `service/AutoSaveService.java` | 441 | zamanlayıcı + çıkış matrisi |
| `ui/MainMenu.java` | 422 | menü ağacı + yerelleştirme |

72 sınıfın 13'ü 300 satırın üzerinde. Mimari borç "dağınık" değil, **az sayıda büyük
sınıfta toplanmış**: `FileService` tek başına 998 satır ve ürünün tüm veri kaybı
yüzeyini taşıyor.

### 5.3 Artefakt envanteri (taze build)

| Varlık | Boyut | Not |
|---|---|---|
| `target/jtypewriter-1.1.0.jar` | 1,2 MiB | ince JAR, `META-INF` legal yükü dahil |
| `target/jtypewriter-1.1.0-jar-with-dependencies.jar` | 11,2 MiB | 6.167 giriş; **5,7 MiB JavaFX `.so`** (yalnız build makinesinin platformu) |
| `releases/linux/JTypeWriter/` | 146,2 MiB | jpackage app-image, 126 dosya |
| `releases/linux/jtypewriter_1.1.0_amd64.deb` | 47,9 MiB | 11 Debian bağımlılığı + `/opt/jtypewriter` yükü |

Yayındaki varlıklar (GitHub Release API): `.deb` 50,76 MB; Linux `tar.gz` 63,4 MB;
`.msi` 60,5 MB; Windows `tar.gz` 58,8 MB; kaynak arşivi 1,4 MB. Yani kullanıcı, 5–6 MB'lık
bir metin editörü için **50–63 MB** indiriyor.

### 5.4 Bağımlılık yüzeyi

Çalışma zamanı: JavaFX Controls/FXML/Media 26.0.1 (Graphics ve Base geçişli). Test:
JUnit Jupiter 6.0.3, Mockito 5.23.0. Başka üçüncü taraf **yok**. Bu, güvenlik ve lisans
açısından en iyi tarafta olunan nokta; aynı zamanda ekosistem entegrasyonunun (sözlük,
Markdown, PDF, otomatik güncelleme) neden hiç olmadığını da açıklıyor.

---

## 6. Mimari Değerlendirme

### 6.1 Güçlü yanlar (korunması gereken kültür)

1. **Tek kompozisyon kökü, çerçevesiz DI.** `AppGraph` alan sırası = bağımlılık sırası;
   çapraz bağlantılar tek kurucuda (`AppGraph.java:59-142`). 72 sınıf için Spring
   getirmemek doğru karar; testler nesne grafiğini doğrudan kurabiliyor.
2. **Saf karar sınıfları.** `YoloPolicy`, `ToolbarFit`, `StatsBarModel`, `FindModel`,
   `ExitDisposition`, `AutosaveHealth`, `RecoveryRetention`, `DroppedFiles`,
   `StartupArguments`, `TextCodec` — hepsi JavaFX'siz ve testli. Bu, "toolkit
   başlatmadan test" yasağının mimari karşılığı ve ürünün en değerli teknik mirası.
3. **Tek envanter ilkesi.** `EditorAction` kısayol, etiket, etki ve menü üretimini tek
   yerde tutuyor (`EditorAction.java:42-97`); kısayol kartı ve araç çubuğu buradan
   **üretiliyor**. Aynı ilke sürüm (`pom.xml` + filtrelenmiş resource), ikon (SVG +
   manifest) ve font (tek `AppFonts`) için de geçerli. Bu disiplin, sürüklenmeyi
   yapısal olarak imkânsız kılıyor.
4. **G/Ç tek iş parçacığında.** `IoExecutor` tek daemon thread; `FxThreadIoTest` FX
   thread'inde disk işi olmadığını sayarak kanıtlıyor (`fxThreadTasks()` = 0). Arama
   için ayrı bir executor (`jtw-find-N`) olması da doğru: uzun bir tarama kaydı
   geciktiremiyor.
5. **Diyalog izolasyonu.** `service/` JavaFX'siz; kullanıcıya görünen her `Alert`/
   `FileChooser` `DialogService`'ten geçiyor ve testler onu taklit ediyor.

### 6.2 Yapısal borç

| # | Bulgu | Kanıt | Etki |
|---|---|---|---|
| A1 | `FileService` 998 satırda yedi sorumluluk taşıyor (aç, kodla, kaydet, çakışma, yedek, kurtarma, kirlilik) | `service/FileService.java` | en yüksek veri kaybı yüzeyi tek dosyada; değişiklik maliyeti yüksek |
| A2 | İki ayrı "gerçek" kavramı: `savedContent` (String) ve `savedFingerprint` (SHA-256) | `FileService.java:144,750-772` | çakışma mantığı iki durumu elle senkron tutuyor |
| A3 | Kurtarma yolu, G/Ç doktrininin dışında (FX thread'inde `readAllBytes`, `loadDraft`, `clear`) | `FileService.java:458`, `AutoSaveService.java:213,227` | büyük belgede açılış donması; test bunu kapsamıyor |
| A4 | `AppSettings` senkronizasyonu yok: `set` + `save` atomik değil, `props.store` mutasyon sırasında geziniyor | `AppSettings.java:551-588` | eşzamanlı yazar olduğunda son yazan kazanır; ayar kaybı |
| A5 | `EditorArea.replaceConfirmed` public ve korumasız | `EditorArea.java:129-131` | YOLO sözü konvansiyonla ayakta, tiple değil |
| A6 | Araç çubuğu/menü/kart hiyerarşisi: menü çubuğun içinde, kartlar `StackPane` köşesinde | `MainController.java:276-296` | U1'deki keşfedilebilirlik kilidi |
| A7 | Testlerin bir kısmı kaynak metni pinliyor | `CiGatesTest`, `ReleaseNotesTest`, `EditorUndoContractTest` | refactor'da yalancı kırmızı; gerçek UI'ı kapsamıyor |

### 6.3 Genişleme noktaları — ve yokluğu

Ürünün komut katmanı **var** (`EditorAction` + `Context`), ama *eklenti*, *komut paleti*,
*çoklu belge* ve *dışa aktarma* için hiçbir uzantı noktası yok. Bugünkü tasarım tek
belge varsayımına gömülü: `FileService` tek `currentFilePath` tutuyor
(`FileService.java:144`), `AppSettings` tek `last.file` saklıyor
(`AppSettings.java:118-119`), `SessionStats` belge başına sıfırlanıyor
(`AppGraph.java:125-142`). Çoklu belge (sekmeler) P3'te gündeme gelirse bu **tek
noktadan** kırılır; her şeyden önce `FileService`'in belge başına örneğe ayrılması
gerekir. Bu, mimarinin en büyük tek kısıtı.

### 6.4 Threading — sınırlar ve kalan riskler

| Yol | Nerede | Risk |
|---|---|---|
| Belge aç/kaydet | `IoExecutor` (tek thread) | doğru; ama `saveFile`/`loadFile` bloklayan API'ler başlangıçta FX thread'inden çağrılıyor (`FxApplication.java:148-153,186`) |
| Otomatik kayıt | `IoExecutor`, 30 s `Timeline` + odak kaybı | doğru; ama `WINDOW_HIDDEN` sonrası zamanlayıcı bir daha başlamıyor (`AutoSaveService.java:300-303`) |
| Kurtarma teklifi | **FX thread'i** (`loadDraft`, `clear`) | A3 |
| Arama | `jtw-find` ayrı thread | iptal yok; eski taramalar kuyruğu tıkar (`FindPanel.java:269-302`) |
| Ayarlar | çağıran thread (FX ve I/O karışık) | A4 |
| İstatistik | FX thread'i, ama O(1) | doğru (0,12 ms/10 MiB) |

---

## 7. Veri Güvenliği — Ayrıntılı Değerlendirme

### 7.1 Kaydetme hattı (güçlü)

`AtomicFiles.writeMoving` (`AtomicFiles.java:60-102`): hedef `toRealPath()` ile
çözülüyor (symlink korunuyor), boş alan ön kontrolü, aynı dizinde `jtw-<ad>*.tmp`,
kısmi yazma döngüsü, `channel.force(true)`, izin/ACL kopyası, `ATOMIC_MOVE` (desteklenmezse
düz `move`), dizin fsync'i (en iyi çaba), hata hâlinde temp silme. Bu, masaüstü Java
uygulamalarında nadir görülen bir olgunluk.

Ek olarak: dış değişiklik algılama **içerik özeti** ile (boyut+mtime değil; aynı boyutlu
düzenlemeyi kaçırmamak için) yapılıyor (`FileService.java:730-748`), silinmiş hedef
"değişmiş" sayılıyor, çakışmada üç seçenek (üzerine yaz / farklı kaydet / vazgeç) var ve
üzerine yazmada dış sürüm `.bak`'a alınıyor.

Kalan açıklar:

- **Yarış (TOCTOU):** özet kontrolü ile rename arasında dosya yine değişebilir
  (`FileService.java:664-707`). Pratikte dar, ama sıfır değil.
- **Dış hedef hash'i sınırsız:** hedef 5 GB'lık bir dosyayla değiştirilmişse kaydetme
  işçisi bunu baştan sona hash'leyip bekliyor (`:950-964`).
- **Boş alan kontrolü tepe tüketimi karşılamıyor:** yedek hazırlığı (`prepared`) yazma
  kontrolünden **önce** diske çıkıyor (`:677-687`), yani `StorageFootprint`'in söylediği
  6× tepe için kontrol yok.
- **POSIX sahiplik korunmuyor:** yalnız izinler kopyalanıyor (`AtomicFiles.java:170-179`);
  başka kullanıcıya ait, yazma izinli bir dosya kaydedilince sahibi değişir.
- **Yeni belge 0600 oluyor** (`:166-169`): `umask`'lı normal oluşturmadan farklı.
- **`NAME_MAX` sınırı:** temp adı `"jtw-" + dosyaAdı + ".tmp"` (`:76-78`); 250
  karakterlik bir dosya adı okunup düzenlenebilir ama **kaydedilemez**.
- **Sarkan symlink** hedefe çözülemediğinde link **düz dosyayla değiştiriliyor**
  (`:109-117`) — bağ bozuluyor.

### 7.2 Kodlama (güçlü, iki delikli)

Algılama sırası: UTF-8 BOM → UTF-16LE BOM → UTF-16BE BOM → **BOM'suz UTF-16 sezgisi**
(≥%80 tek/çift bayt sıfır deseni, `TextCodec.java:236-263`) → katı UTF-8 → soru
(windows-1254 / UTF-8 / vazgeç). BOM'lu içerik katı çözülüyor; eşleşmeyen BOM bir
**soru**; yazma her zaman aynı kodlama+BOM ile ve **`REPORT`** ile yapılıyor, yani
temsil edilemeyen karakter `?`'ya dönüşmüyor, UTF-8'e yükseltme teklif ediliyor.
`EncodingMatrixTest` aç→kaydet ve aç→düzenle→kaydet bayt matrisini altı senaryoda
pinliyor. Bu, ürünün en iyi mühendislik işi.

Delikler:
- **D4** (yukarıda): kullanıcı rızasıyla seçilen "UTF-8" kayıplı çözüyor, sonra `U+FFFD`
  baytlarını UTF-8 olarak geri yazıyor.
- **D3** (yukarıda): bozuk BOM'lu orijinalden kurtarmada kodlama varsayılana düşüyor.
- Satır sonu stili hiç izlenmiyor; CRLF bir dosyada düzenleme karışık satır sonu
  üretebiliyor (kodlama katmanı karakter odaklı).
- Yalnız windows-1254 eski kodlama olarak sunuluyor; ISO-8859-9, CP1252, Shift-JIS vb.
  için yol yok.

### 7.3 Otomatik kayıt ve kurtarma (güçlü, üç açık)

Mekanizma: 30 s'de bir (ve odak kaybında) yalnız **kirli** belge tek bir atomik
`draft-<uuid>.jtw` dosyasına yazılıyor; başlık `JTypeWriter-Draft/2`, `saved.at`, Base64
orijinal yol; gövde metnin kendisi. Çıkış matrisi üç değerli ve frozen slot her sinyalin
önünde (`ExitDisposition.java:55-60`): yanıtlanmış kapanış → sil; yanıtlanmamış çıkış →
`pending-*` olarak park; park edilemeyen → dokunma. "Kapat"a basmak **asla** silmiyor
(`RecoveryChoice.LATER`). Diskten daha yeni bir orijinal varsa metin geri veriliyor ama
**yolsuz**, böylece kaydetme Save As'e zorluyor. `ProcessCrashRecoveryTest` bunu gerçek
`SIGKILL` ile iki JVM'de kanıtlıyor.

Açıklar:
- **D1** (yukarıda): canlı `draft-*` dosyaları süresiz birikiyor.
- **D2** (yukarıda): `currentFilePath` sızıntısı + sınırsız FX-thread okuması.
- **D5 — Park edilmiş taslak yaşlandırması fazla siliyor.** `RecoveryRetention.expired`
  boyut kuralında `break` kullanıyor (`:82-84`); yorumu "kalanlar daha yaşlı, en iyisi o
  kadar büyük olur" diyor ki bu **yanlış** — daha yaşlı bir taslak çok daha küçük olabilir
  ve sığardı. Ayrıca yaş kuralı `continue` kullanırken yorumu "sonrası da öyledir"
  diyor; sıralama gereği zararsız ama niyetle kod uyuşmuyor.
- **D6 — Başarısız "sil" sessiz.** Kurtarma teklifinde DISCARD seçilip `store.clear()`
  başarısız olursa dönüş değeri yok sayılıyor (`AutoSaveService.java:225-228`), oysa
  `RecoveryStore.clear()` tam bu nedenle `boolean` döndürüyor (`:308-329`).
- **D7 — Silme uyarısı yanlış zamanda susuyor.** `AutosaveHealth` tek slotlu; başarılı bir
  anlık görüntü `recover()` çağırıp `DRAFT_DELETE` uyarısını siliyor
  (`AutoSaveService.java:367-369`) ama bayat taslak hâlâ diskte duruyor.
- **D8 — Çöken oturumun undo geçmişi, imleci, kodlaması yok.** Taslak yalnız metin+ yol +
  zaman taşıyor. Kodlama kurtarmada orijinalden yeniden türetiliyor; satır sonları ve
  BOM kaybolabilir.
- **D9 — Zamanlayıcı yeniden başlamıyor:** pencere gizlenip yeniden gösterilirse
  (`WINDOW_HIDDEN` bir kez `shutdown`) otomatik kayıt sessizce ölür
  (`AutoSaveService.java:300-303`).
- **D10 — Sert öldürmede temp artıkları:** `jtw-draft-*.tmp` hiçbir yerde süpürülmüyor.

### 7.4 Yedekler

Üç nesil (`<ad>.bak`, `.bak.1`, `.bak.2`), gerçek hedefin yanında, iki fazlı
(`.bak.new` hazırla → döndür → kur). Yedek hatası **kaydetmeyi iptal etmiyor**; yalnız
uyarı logluyor (`FileService.java:677-704`). Bu doğru öncelik. Zayıflıklar: staging
kopyası fsync edilmiyor, döndürme adımları atomik değil (kısmi hatada boşluk/çift nesil
kalabilir), ve disk maliyeti belge başına **4×** kalıcı, kaydetme anında ~6× (kullanıcıya
ilk kayıtta bir kez söyleniyor — `AppGraph.announceStorageFootprintOnce`).

### 7.5 Ayarlar

`~/.jtypewriter/settings.properties`, 22 anahtar, `schema.version` = uygulama sürümü
(tek sayı), göç bellekte, dosya yalnız gerçek değişiklikte yazılıyor, bozuk dosya
`settings.properties.corrupt[.n]` olarak kenara alınıyor, NaN/Infinity geometri
reddediliyor, ses seviyesi [0,1] kırpılıyor, bilinmeyen enum değerleri varsayılana
düşüyor. YOLO'nun bilinçli olarak **kalıcı olmaması** doğru karar.

Açıklar: **A4** (senkronizasyon), **D11** — `props.load` kısmen başarılıysa karantinaya
rağmen bellekte kalan anahtarlar bir sonraki yazmada kalıcılaşıyor
(`AppSettings.java:478-511`), yani "varsayılanlarla aç" sözü tutulmuyor. Ayrıca
**D12** — sürüm segmenti `long`'a sığmazsa boş dizi dönüyor ve göç bunu "sürümsüz" sayıp
**daha yeni** dosyayı bellekte aşağı sürümlüyor (`:410-417,457-476`).

### 7.6 Tek örnek, günlükler, çökme

- `instance.lock` üzerinde `tryLock`; üç durum: alındı / zaten çalışıyor / kullanılamıyor.
  Kullanılamıyorsa **uygulama açılıyor** (fail-open) ve tek satır `stderr` yazıyor — doğru
  tercih, ama README'nin "ikinci başlatma uyarı gösterip çıkar" ifadesi bugünkü davranışla
  tam örtüşmüyor: uyarı **modal** (`DialogService.showWarning` → `showAndWait`), kullanıcı
  onaylayana kadar süreç yaşıyor. Bu oturumda `timeout 8` ile ölçüldü: süreç 8 s sonra
  hâlâ ayaktaydı (çıkış 124).
- Günlükler: `~/.jtypewriter/logs/jtypewriter-0..2.log`, 3 × 1 MB, `severe` yığın izleri
  nedensellik zinciriyle; kurulum başarısızsa yalnız konsol. Çökme yakalayıcı varsayılan
  işleyiciyi **değiştiriyor** ve öncekine zincirlemiyor (`CrashHandler.java:41-45`).
- `AppDirs` `user.home` yoksa korumasız (`AppDirs.java:19-21`); `AppLogging` ve
  `RecoveryStore` bu durumu korurken `AppDirs` korumuyor → `user.home` tanımsız bir
  ortamda açılışta NPE.

### 7.7 Kullanıcıya görünen kayıp matrisi

| Senaryo | Bugünkü davranış | Değerlendirme |
|---|---|---|
| Kaydetme sırasında güç kesintisi | atomik rename; dosya ya eski ya yeni | **güvenli** |
| Uygulama çökmesi (30 s içinde) | taslak → geri teklif | **güvenli** |
| Dışarıdan dosya değişti | özet karşılaştırması → üç seçenek | **güvenli** |
| Dosya dışarıdan silindi | "değişti" sayılıp soruluyor | **güvenli** |
| Symlink üzerinden kaydetme | link korunuyor, hedef yazılıyor | **güvenli** |
| Sarkan symlink | link düz dosyayla değişiyor | **bozulma** (D-küçük) |
| Bozuk BOM'lu orijinalden kurtarma + kaydet | UTF-8'e dönüşebilir | **bozulma** (D3) |
| Bilinmeyen kodlama + "UTF-8" seçimi | `U+FFFD` kalıcı | **bozulma** (D4) |
| Kurtarma sonrası eski belgeye yazma | teorik: `currentFilePath` sızıntısı | **bozulma** (D2) |
| Bir kez yedek yazılamaması | kayıt başarılı, yedek yok | kabul edilebilir |
| Ayar dosyası yazılamıyor | bir kez uyarı, oturum sürüyor | **güvenli** |
| Taslak silinemiyor | durum çubuğu uyarısı (erken susabiliyor) | D7 |

---

## 8. Editör Yetkinliği ve Günlük Döngü

### 8.1 Var olanlar (koddan doğrulanmış)

| Alan | Durum | Kanıt |
|---|---|---|
| Düz metin düzenleme, sarma, 10–48 px yazı | var | `EditorArea`, `FontSizeManager` |
| YOLO modu (silme yasak, değiştirme ekleme) | var | `YoloPolicy`, `EditorArea.replaceText` |
| Daktilo sesi + 5 kademeli ses | var | `TypewriterSound`, `SoundLevel` |
| Kelime/karakter/satır/vuruş/süre/WPM | var | `SessionStats`, `StatsBar` |
| Arama: sonraki/önceki, büyük-küçük harf, sarma, tümünü değiştir | var | `FindModel`, `FindPanel` |
| Üç tema, altı metin rengi | var | `Theme`, `TextColor` + CSS tokenları |
| Tam ekran, araç çubuğu gizleme | var | `MainStage`, `EditorAction` |
| Komut menüsü (File/Edit/View/Help), üretilen | var | `MainMenu` |
| Karşılama/son dosyalar/hakkında kartları | var | `WelcomeCard`, `RecentFilesCard`, `AboutCard` |
| Sürükle-bırak ile açma | var | `DropHandler`, `DroppedFiles` |
| Komut satırından açma (tek argüman) | var | `StartupArguments` (bu oturumda doğrulandı: başlık `JTypeWriter - sample.txt`) |
| Tek örnek kilidi | var | `InstanceLock` (bu oturumda doğrulandı) |
| Oturum geri yükleme (dil, tema, renk, yazı, ses, son dosya, geometri) | var | `AppSettings`, `MainStage` |
| 6 dil | var | `Language`, 6 × 156 anahtar |

### 8.2 Olmayanlar (ürün açığı)

| Yokluk | Neden önemli | Teknik maliyet |
|---|---|---|
| **Yazdırma / PDF** | yazarın taslağı paylaşma yolu | orta (JavaFX `PrinterJob` var, `javafx.print` modülü imajda **yok**) |
| **Dışa aktarma** (HTML/Markdown/RTF) | teslim ve yayın öncesi adım | orta |
| **Sözlük / yazım denetimi** | uzun metinde en çok istenen | yüksek (sözlük verisi + dil başına) |
| **Çoklu belge / sekme** | tek dosya varsayımı mimariye gömülü (§6.3) | yüksek |
| **Markdown (en azından vurgulama)** | rakip standart, `.md` zaten açılıyor | orta |
| **Satır numarası / cetvel** | düz metin editörü beklentisi | orta (JavaFX `TextArea` bunu doğrudan vermiyor) |
| **Odak/typewriter modu (satır ortalama)** | türün imza özelliği, seste var ama ekranda yok | düşük–orta |
| **Kelime hedefi / oturum hedefi** | FocusWriter'ın temel farkı | düşük |
| **Arama genişletmeleri** (regex, tam kelime, seçim içinde, tümünü vurgula) | "Notepad++ tarzı" panelin doğal beklentisi | düşük–orta |
| **Geri yükleme (versiyon geçmişi UI)** | `.bak` nesilleri var ama görünmüyor | orta |
| **Ayar arayüzü** | her şey menü/araç çubuğu, tek "tercihler" yok | düşük |
| **Otomatik güncelleme** | 5 sürüm, elle indirme | orta–yüksek (imza + kanal) |

### 8.3 Keşfedilebilirlik — ölçülen resim

- Kısayol kartı (`WelcomeCard`) 19 eylemden üretiliyor ve altı dilde tam; keşfedilebilirlik
  için iyi bir çözüm.
- **U1**: araç çubuğu gizliyken menüye erişim yok. Kısayolu bilmeyen kullanıcı için
  çıkmaz sokak.
- **U3**: menüdeki hızlandırıcı yazıları **yalnız gösterim** (`MainMenu.java:61-64,407-421`);
  gerçek yürütme sahne filtresinden. Davranış doğru ama modal diyalog açıkken menü
  hızlandırıcısı beklentisiyle çelişebilir.
- **U4**: durum çubuğu boyutu yalnız **çift tıklama** ile değişiyor (`StatsBar.java:96-112`);
  klavye/ekran okuyucu yolu yok, ipucu (tooltip) yok.
- **U5**: arama paneli `Dialog` değil `VBox` katmanı; diyalog semantiği ve odak tuzağı yok
  (Escape kapatıyor).

---

## 9. Performans ve Ölçek

### 9.1 Açılış, bellek

Yöntem: taze `HOME`, X11'de yeni pencere kimliği `xwininfo` ile yoklanarak (uyku yok,
5 ms aralık), 3 koşu. Sonuç: ana pencere **1,63 / 1,65 / 1,66 s**. Bu süre ilk pencere
eşlemesidir; karşılama kartının görünmesi ayrı bir ölçüm gerektirir (bu oturumda kart
açılmadı çünkü yeni profilde `welcome.enabled=true` olmasına rağmen kart ayrı bir
pencere değil, ana sahne içinde katman — pencere sayısıyla ölçülemez).

RSS: pencere anında **298–300 MiB**, 1 s sonra **318–321 MiB**. 50 MB'lık bir pakette
300 MiB RSS, JavaFX'in varsayılan ayarlarıyla beklenen aralıkta; yine de ürünün
"hafif" konumlandırmasıyla çelişen bir sayı. `-Xmx`/GC ayarı yapılmıyor
(`JTypeWriter.cfg` yalnız `file.encoding` ve `enable-native-access` taşıyor).

### 9.2 Belge G/Ç'si (10 MiB, sıcak önbellek)

| İşlem | En iyi | Not |
|---|---|---|
| `AtomicFiles.writeMoving` | **10,4 ms** | fsync + rename + izin kopyası dahil |
| Ham okuma | 2,2 ms | |
| `TextCodec.decode` | **20,2 ms** | katı UTF-8 doğrulaması + UTF-16 sezgisi |
| SHA-256 (10 MiB) | 11,3 ms | parmak izi; her kaydetmede bir kez |

Bu sayılar 30 s'lik otomatik kayıt için fazlasıyla yeterli. Kaydetme yolunun ölçülen
maliyeti kabaca **~32 ms/10 MiB** (hash + kodla + yaz), yani saniyenin %0,1'i.

### 9.3 Tekrarlayan maliyet

`StatsPerfHarnessTest` 10 MiB'de tam bir `StatsBarModel.refresh` için **0,116–0,123 ms**
ölçüyor (eşik 50 ms). Bu, artımlı sayaç tasarımının kanıtı: eski tam-metin taraması
saniyede bir 100+ ms yiyordu; bugün saniyede bir 0,1 ms. Aynı şekilde otomatik kayıt
temiz turda metni **kopyalamıyor** (`AutoSaveService.java:352-378`).

### 9.4 Ölçek sınırları

- Belge tavanı **64 MiB** (`FileService.java:114`), taslak tavanı aynı
  (`RecoveryStore.java:145`). Birçok javadoc hâlâ "10 MB" diyor
  (`FindScanService.java:25,60`, `FindModel.java:139`, `FindPanel.java:41`,
  `SessionStats.java:23`, `AutoSaveService.java:346`) — **6,4× yanlış** bir üst sınır
  anlatıyor.
- Arama 64 MiB'de O(n) ve iptalsiz; hızlı yazarken kuyruk birikir (§6.4).
- `replaceAll` tüm belgeyi tek FX-thread çağrısında yeniden kuruyor (`FindPanel.java:407,427`);
  tek geri-alma adımı olması iyi, ama 64 MiB'de tepe bellek ~3×.
- Belge `TextArea` ile tutuluyor; 64 MiB'lik bir metin için JavaFX `TextArea`nın
  kendisi pratikte sınırlayıcı olabilir (**ölçülmedi** — bu, P3'te ölçülmesi gereken
  gerçek bir sorudur).

### 9.5 Ölçülmemiş senaryolar

Gerçek tuş gecikmesi, 1/5/10/64 MiB belgede yazma akıcılığı, arama paneli açılış
gecikmesi, tema değişiminde tam yeniden çizim, ilk tuşta ses yükleme gecikmesi (JavaFX
`AudioClip` ilk yüklemesi + temp dosya çıkarma). Bunlar toolkit gerektirdiği için bu
belgede yok; P3'ün ölçüm planı bunları içermelidir.

---

## 10. Paketleme, Dağıtım, Güncelleme

### 10.1 Bugünkü hat

`package.sh` → jpackage `app-image` → legal yükü elle kopyalama + varlık assert'i →
(isteğe bağlı) `.deb`/`.msi`. `.deb` için jpackage'ın eksik bıraktığı legal yük
`complete_deb_payload` ile yeniden paketleniyor, menü kaydı `|| true` ile
yumuşatılıyor, DEP-5 copyright üretiliyor. Bu, depoda gördüğüm en özenli jpackage
sarmalayıcısı; ama üç somut kusur var.

### 10.2 B1 — deterministik paketleme arızası (bu oturumda üretildi)

```
$ ./scripts/package.sh --skip-build --installer
application image written to releases/linux/ (version 1.1.0)
installer written to releases/linux/ (deb)
expected exactly one .deb under releases/linux/, found 2      # çıkış 1
```

Kök neden: `package.sh:115` temizliği `"$DEST/$APP_NAME"*.deb` (= `JTypeWriter*.deb`) ile
yapıyor; jpackage ise **küçük harfli** `jtypewriter_<sürüm>_amd64.deb` üretiyor — betiğin
kendi yorumu bunu `:354`'te yazıyor. Dolayısıyla eski sürüm paketi asla silinmiyor ve
`:420-425` sayım kontrolü ikinci sürümden itibaren düşüyor. Aynı varsayım
`verify-artifacts.sh:180-186` içinde de var; o da aynı gerekçeyle düşüyor (bu oturumda
doğrulandı). CI temiz bir checkout'ta çalıştığı için arıza **yalnız yerel sürüm
mühendisini** vuruyor — yani sürümü üreten kişiyi. Depoda duran 1.0.2 `.deb`'i bunun
canlı kanıtı.

### 10.3 P1 — imaj şişkinliği (ölçüldü)

| Bileşen | Boyut | Gerekli mi? |
|---|---|---|
| `lib/runtime` toplamı | 133,5 MiB | kısmen |
| `lib/runtime/lib/modules` | 86,4 MiB | 52 modül; 6'sı yeterli |
| `lib/runtime/lib/server/libjvm.so` | 27,4 MiB | gerekli |
| `lib/runtime/lib/ct.sym` | **10,4 MiB** | **hayır** (derleyici sembolleri; `jdk.compiler` yüzünden) |
| `lib/app/*.jar` | 11,2 MiB | gerekli (JavaFX sınıfları + native'ler burada) |

`jdeps` (bu oturumda, JavaFX 26.0.1 modül yolunda):
`java.base, java.desktop, java.logging, javafx.controls, javafx.fxml, javafx.media`.
Aynı JDK ile `jlink` denemesi:
`--add-modules java.base,java.desktop,java.logging,javafx.controls,javafx.fxml,javafx.media,jdk.unsupported`
`--strip-debug --no-header-files --no-man-pages --compress=zip-6`
→ **71.553.083 B = 68,2 MiB**. Yani mevcut imajın **%49'u** atılabilir; indirme boyutu
50 MB'tan ~30 MB'a, kurulu boyut 147 MB'tan ~75 MB'a iner. Üstelik JavaFX modül yoluna
taşınırsa **P2**'deki "unnamed module" uyarısı da kaybolur.

### 10.4 P3 — jpackage'ın yaptığı iş ile JavaFX'in yerleşimi çelişiyor

jpackage çalışma zamanını **host JDK'dan** üretiyor ve modül listesinde `javafx.*` yok;
JavaFX sınıfları ve native kütüphaneleri şişman JAR'ın içinden classpath ile yükleniyor.
Sonuç: (a) JVM her başlatmada uyarı yazıyor, (b) JavaFX native'leri (`libglassgtk3.so`
2,1 MiB vb.) her çalışmada geçici dizine çıkarılıyor, (c) jlink kazancı kullanılamıyor,
(d) `lib/runtime/legal/` JavaFX lisans metinlerini içermiyor — onlar JAR içindeki
`META-INF/licenses/` ile karşılanıyor (dolaylı ama yasal olarak izlenebilir).

### 10.5 Kanal, imza, güncelleme

- İki kanal yayınlanıyor: kurulum paketi (`.deb`/`.msi`) ve taşınabilir `.tar.gz`.
  **Hangisinin varsayılan olacağı açık bir ürün kararı** olarak duruyor (release notu
  bunu açıkça söylüyor).
- **Kod imzası yok**: Windows SmartScreen uyarısı, Linux'ta paket/imza doğrulaması yok,
  `SHA256SUMS` GPG ile imzalanmıyor. İndirme doğrulaması yalnız hash.
- **Otomatik güncelleme yok**; sürüm kontrolü de yok (bilinçli "ağa çıkmama" kararı).
  Bu bir güvenlik artısı, bir sürdürülebilirlik eksisi.
- `.msi` yükü **hiçbir kapıda** bağımsız doğrulanmıyor (`verify-artifacts.sh:170-175`);
  release notunun "her varlık LICENSE/NOTICE/THIRD-PARTY-NOTICES taşır" iddiası Linux
  tarafında test ediliyor, Windows tarafında edilmiyor.
- `release-assets.sh` `arm64` kabul ediyor (`:62-68`) ve workflow `ARM64` eşliyor, ama
  yayın varlık assert'i `-x64` sabit (`release.yml:233-239`) → ARM runner'da yayın düşer.
  Karar verilmemiş bir eksenin kodda yarım bırakılmış izi.

### 10.6 Olgunluk eksenleri

| Eksen | Durum |
|---|---|
| Tekrarlanabilir derleme | Maven arşivleri sabit zaman damgalı (`pom.xml:61`); **jpackage imajı, `.deb`, `.msi` ve tar'lar normalize edilmiyor**, bağımsız yeniden üretim karşılaştırması yok (`verify-artifacts.sh` aynı koşunun kopyalarını karşılaştırıyor) |
| Araç zinciri pinleme | JDK `'25'` (yama sürümü kayar), Maven runner'a bağlı, wrapper yok (bilinçli) |
| Bağımlılık güncellemesi | Yalnız GitHub Actions için Dependabot; JavaFX/JUnit/Mockito/plugin'ler elle |
| SBOM | yok |
| Kod imzası | yok |
| Kapsam ölçümü | yok (JaCoCo yok) |
| Statik analiz | yok (bilinçli; `LicenseHeaderTest` tek başlık kapısı) |

---

## 11. CI/CD ve Yayın Hattı

### 11.1 CI (maven.yml)

İki OS × (`build`, `smoke`) + bir `badge` işi. `build`: pinlenmiş action SHA'ları,
Temurin 25 + Maven cache, **ikon `--check` (build'den önce, iki bacakta)**,
`mvn -B package`, `tr-TR` ve `de-DE` yeniden koşuları (rapor dizinleri ayrı ayrı),
Linux'ta `fakeroot` + `package.sh --skip-build --installer`, Linux'ta
`verify-artifacts.sh --skip-build`, artefakt yükleme (7 gün). `smoke`: artefaktı indirip
Xvfb altında başlatma + `.deb` kur/kaldır (root varsa), Windows'ta "launcher 10 s ayakta".
Bu, çoğu açık kaynak JavaFX projesinden ileride bir hat.

Sorunlar: testler 3 kez (varsayılan + TR + DE) koşuluyor — 575 × 3 = **1.725 koşu**,
rozet de bu toplamı yayınlıyor (`publish-test-badge.sh:79-127`), yani rozet "1.725
passing" diyor; bu sayı gerçek test sayısını 3 katı gösteriyor (kozmetik ama yanıltıcı).
`CiGatesTest` kapıların **metnini** pinliyor (§12.2).

### 11.2 Yayın (release.yml)

`v*` tag veya elle `dry_run`; `pom.xml` sürümü ile tag eşleşmesi zorunlu, elle yayın
reddediliyor, beş varlık + `SHA256SUMS` assert'i, iki dilli not şablonu (`@VERSION@`,
`@TAG@`, `@ASSET_SIZES@`), `gh release create --verify-tag`. Bu disiplin iyi.

**B2**: yayın hattı CI'ın hiçbir kalite kapısını yeniden koşmuyor
(`release.yml:132-141`): ikon kontrolü, `verify-artifacts.sh`, yerel ayar koşuları, smoke
yok. Tag, doğrulanmamış bir commit'e atılırsa yayın bunu bilmez. Ayrıca yayın notu
"normal sürüm" iddiasını `--prerelease` yokluğuyla pinliyor (`ReleaseNotesTest`), ama
sürümün **içeriği** hakkında hiçbir doğrulama yok (değişiklik listesi elle yazılmıyor;
şablon sürümden bağımsız sabit metin).

---

## 12. Test ve Kalite Güvencesi

### 12.1 Ölçülen şekil

- **61 test sınıfı + 2 yardımcı** (`CrashHarness`, `ContrastRatio`), **575 `@Test`**,
  12.018 satır; `@Disabled` yok; duvar saati **7,4 s**; en yavaş sınıflar
  `StatsBarModelTest` 0,71 s, `ProcessCrashRecoveryTest` 0,43 s, `ToolbarFitTest` 0,38 s.
- Üretim:test satır oranı **0,89**. Bu, "her sınıf testli" iddiasını destekliyor.
- JavaFX toolkit'i **hiçbir testte** başlatılmıyor; bu bilinçli ve tutarlı.

### 12.2 Gerçekten iyi kapsanan alanlar

| Alan | Kanıt |
|---|---|
| Kodlama matrisi (aç→kaydet baytları) | `EncodingMatrixTest`, `TextCodecTest` |
| Kurtarma yaşam döngüsü + gerçek `SIGKILL` | `RecoveryStoreTest`, `ProcessCrashRecoveryTest` |
| Çıkış matrisi, otomatik kayıt sağlığı | `ExitDispositionTest`, `AutosaveHealthTest`, `AutoSaveServiceTest` |
| Atomik yazma | `AtomicFilesTest` |
| Ayarlar + göç + karantina | `AppSettingsTest` (36 test) |
| i18n parite + yerel ayar bağımsızlığı | `MessagesTest` (13) |
| Kontrast + CSS token'ları | `ContrastRatioTest` (24) |
| Araç çubuğu sığdırma + minimum genişlik (6 dil) | `ToolbarFitTest` (16) |
| Komut envanteri ↔ menü ↔ kısayol kartı iki yönlü | `EditorActionTest`, `ShortcutRowsTest`, `MainControllerTest` |
| FX thread'inde disk işi olmaması | `FxThreadIoTest` |
| İstatistik performans tavanı | `StatsPerfHarnessTest` |
| Varlık bütünlüğü (ikon boyutları, `.ico` magic, font ölçümü, ses hash'i) | `AppIconsTest`, `AppFontsTest`, `AudioAssetTest` |
| Lisans başlığı / üçüncü taraf bildirimi / yayın notu / CI kapıları | `LicenseHeaderTest`, `PackagingNoticesTest`, `ReleaseNotesTest`, `CiGatesTest` |

### 12.3 Boşluklar ve yanlış güven

- **Gerçek UI yok**: hiçbir test gerçek bir düğmeye basmıyor, bir diyaloğu açmıyor,
  bir temayı çizmiyor. Bu boşluğu tek başına `smoke-appimage.sh` kapatıyor; o da yalnız
  "pencere açıldı" diyor.
- **Kaynak metni pinleyen testler** (`CiGatesTest`, `ReleaseNotesTest`,
  `EditorUndoContractTest`, `DialogSizingTest`, kısmen `MainControllerTest`): refactor'da
  yalancı kırmızı, gerçek regresyonda sessiz.
- **AGENTS kuralı ile kod çelişiyor**: "Mockito yalnız `Messages`/`DialogService`"
  deniyor, ama `AutoSaveServiceTest` `FileService` ve `RecoveryStore`'u da taklit ediyor
  (`AGENTS.md` ilgili madde).
- **Kurtarma yolunun FX-thread G/Ç'si test edilmiyor**: `FxThreadIoTest` yalnız
  aç/kaydet/otomatik kayıt yollarını sayıyor; `restoreRecovered`/`loadDraft`/`clear`
  kapsam dışı.
- **Kapsam ölçümü yok**; "hangi satır kapsanmıyor" sorusunun cevabı yok.
- **Windows yalnız birkaç testte** `Assumptions` ile atlanıyor; Windows'a özgü davranış
  (yol ayırıcı, dosya kilidi, `cygpath`) yalnız kaynak düzeyinde pinli.
- `.msi` ve jpackage hiçbir JUnit testinin konusu değil; yalnız betik assert'leri.

---

## 13. Dokümantasyon, Lisans ve Topluluk

### 13.1 Güçlü yanlar

- `README.md` (573 satır) ve `README-TR.md` (570 satır) aynı 15 bölümle paralel; veri
  konumları, kısayollar, mimari, test tablosu, lisans ayrıntısı var.
- `LICENSE` (Apache-2.0), `NOTICE`, `THIRD-PARTY-NOTICES` (LGPL kaynak teklifi dahil),
  `legal/` üç metin, font ve ses için `PROVENANCE.md` + SHA-256. Bu, ticari ürünlerin
  çoğunda bulunmayan bir izlenebilirlik.
- `docs/release-notes-template.md` iki dilli, yer tutuculu, sabit semver içermiyor;
  `ReleaseNotesTest` bunu pinliyor.
- `AGENTS.md`: mimari ve test kültürünü yazıya dökmüş; ajanlar için nadir bir olgunluk.

### 13.2 Bayat veya eksik doküman

| Bulgu | Kanıt |
|---|---|
| README test tablosu **46 sınıf** listeliyor, gerçek **61**; 15 sınıf hiç anılmıyor (`AppVersionTest`, `AudioAssetTest`, `Dialog*Test`, `EditorUndoContractTest`, `LicenseHeaderTest`, `Overlay*Test`, `Release*Test`, `Sound*Test`, `StatsBarModelTest`, `ThemeCycleTest`) | `README.md:499-548`, `README-TR.md:481-547` |
| `NOTICE:14` hâlâ "PNG, ICO ve **ICNS**" diyor; ikon üreteci yalnız PNG+ICO üretiyor, macOS ürün dışı | `generate-icons.py:59-65`, `AGENTS.md` |
| README mimari tablosu `AppIcons` için "dock icon" diyor (macOS dili) | `README.md:385,444` |
| README, ikinci örneğin "uyarı gösterip çıktığını" söylüyor; uyarı modal ve onay bekliyor | §7.6 |
| Kod javadoc'ları belge tavanını "10 MB" diyor, gerçek 64 MiB | §9.4 |
| Mimari ağacında `UndoHistoryReset.java` var, dosya yok; 18 gerçek sınıf ağaçta yok | README ağacı ↔ `find src/main/java` |
| `docs/` yalnız yayın notu şablonu içeriyor; **CONTRIBUTING, CHANGELOG, SECURITY, CODE_OF_CONDUCT yok** | depo kökü |
| Yayın notu şablonu sürümden bağımsız sabit metin; her sürümün **ne değiştiğini** anlatan bölüm yok | `docs/release-notes-template.md` |

### 13.3 Topluluk ve sürdürülebilirlik

Tek geliştirici, 195 commit, 1 Dependabot PR'ı, 0 dış katkı, 0 issue. Katkı rehberi
(tek bir README bölümü) ve issue şablonları yok. Bu, ürünün **bus factor = 1** olduğu
anlamına gelir; sürüm anahtarları, imzalama ve paketleme bilgisi tek kişide.

---

## 14. Bulgu Dizini

Önem: **S1** = ürün vaadini zedeleyen veya sürümü kıran; **S2** = ciddi sürtünme /
yanlış davranış; **S3** = kozmetik, doküman, hijyen. Sıralama yoktur (yol haritası
değildir).

### S1

| ID | Bulgu | Kanıt | Kanıt düzeyi |
|---|---|---|---|
| B1 | `package.sh`/`verify-artifacts.sh` eski `.deb`'i temizlemiyor → ikinci sürümde paketleme düşüyor | `package.sh:115,420-425`; `verify-artifacts.sh:180-186` | **A** (üretildi) |
| D1 | Canlı `draft-*.jtw` dosyaları hiç yaşlandırılmıyor; kullanıcı metni sınırsız birikiyor | `RecoveryStore.java:226-231,240-248`; `RecoveryRetention.java:25-29` | B |
| D2 | `restoreRecovered` eski `currentFilePath`'i bırakabiliyor; orijinali sınırsız ve FX thread'inde okuyor | `FileService.java:453-493` | B |
| D3 | Bozuk BOM'lu orijinalden kurtarmada kodlama varsayılana düşüyor | `FileService.java:475-481` | B |
| D4 | "Kayıpsız UTF-8" seçeneği `REPLACE` ile çözüyor → kalıcı `U+FFFD` | `FileService.java:829-833`; `TextCodec.java:265-275` | B |
| U1 | Araç çubuğu gizlenince menüye fareyle erişim yok | `MainController.java:294-296` | B |
| P1 | Uygulama imajı %49 gereksiz (52 modül, `ct.sym`); jlink ile 68,2 MiB mümkün | §10.3 ölçümü | **A** |
| B2 | Yayın hattı ikon/artefakt/locale/smoke kapılarını yeniden koşmuyor | `release.yml:132-141` | B |
| M1 | Talep kanıtı yok: 0 yıldız, 0 fork, 36 indirme | GitHub API | **A** |

### S2

| ID | Bulgu | Kanıt | Kanıt düzeyi |
|---|---|---|---|
| P2 | JavaFX "unnamed module" uyarısı her açılışta; JavaFX runtime modüllerinde değil | `lib/runtime/release`, 3/3 log | **A** |
| A4 | `AppSettings` yazma yolu senkronize değil | `AppSettings.java:551-588` | B |
| D5 | `RecoveryRetention` boyut kuralı gereğinden fazla siliyor; yorumlar kodla çelişiyor | `RecoveryRetention.java:79-84` | B |
| D6 | Başarısız DISCARD sessiz | `AutoSaveService.java:225-228` | B |
| D7 | `DRAFT_DELETE` uyarısı başarılı anlık görüntüde erken susuyor | `AutoSaveService.java:367-369` | B |
| D9 | Gizle/göster sonrası otomatik kayıt yeniden başlamıyor | `AutoSaveService.java:300-303` | B |
| D11 | Bozuk ayar dosyasının kısmen okunmuş anahtarları kalıcılaşıyor | `AppSettings.java:478-511` | B |
| D12 | Taşan sürüm segmenti "sürümsüz" sayılıp yeni dosyayı aşağı sürümlüyor | `AppSettings.java:410-417,457-476` | B |
| U2 | WPM vuruş/5 hesaplıyor; süre ile tutarsız | `SessionStats.java:173-178` | B |
| U3 | Menü hızlandırıcıları yalnız gösterim | `MainMenu.java:61-64,407-421` | B |
| U4 | Durum çubuğu boyutu yalnız çift tıklama; klavye/ipucu yok | `StatsBar.java:96-112` | B |
| U6 | Arama taramaları iptal edilemiyor; 64 MiB'de kuyruk birikir | `FindPanel.java:269-302` | B |
| A7 | Testlerin bir kısmı davranış değil kaynak metni pinliyor | §12.3 | C |
| M2 | README test tablosu 46/61, `NOTICE` ICNS, javadoc 10 MB/64 MiB | §13.2 | **A** |
| B3 | `.msi` yükü hiçbir kapıda doğrulanmıyor | `verify-artifacts.sh:170-175` | B |
| B4 | Kurulum paketleri imzasız; `SHA256SUMS` imzasız | release notu, workflow | B |
| A1 | `FileService` 998 satır, yedi sorumluluk | §5.2 | B |

### S3

| ID | Bulgu |
|---|---|
| D2b | `AtomicFiles` temp adı `NAME_MAX` sınırında kaydetmeyi bozuyor (`AtomicFiles.java:76-78`) |
| D4b | POSIX sahiplik korunmuyor; yeni belge 0600 (`AtomicFiles.java:166-179`) |
| D4c | Sarkan symlink düz dosyayla değiştiriliyor (`:109-117`) |
| D4d | Boş alan kontrolü yedek staging'ini hesaba katmıyor (`FileService.java:677-687`) |
| D8 | Kurtarma taslağı undo/imleç/kodlama/satır sonu taşımıyor |
| D10 | Sert öldürmede `jtw-*.tmp` artıkları temizlenmiyor |
| D13 | `AppDirs` `user.home` yoksa korumasız (`AppDirs.java:19-21`) |
| D14 | `RecoveryTargetPath` `SecurityException` sızdırıyor (`:105-123`) |
| U5 | Arama paneli diyalog semantiği/odak tuzağı taşımıyor |
| U7 | `replaceConfirmed` public, YOLO rızası tiple korunmuyor (`EditorArea.java:129-131`) |
| U8 | Pencere başlığı sabit `"JTypeWriter"`, `appName` anahtarı kullanılmıyor (`FxApplication.java:60`) |
| U9 | Ses ilk tuşta yükleniyor; soğuk ilk tuşta gecikme olabilir (ölçülmedi) |
| P4 | `-Xmx`/GC ayarı yok; RSS ~300 MiB |
| B5 | Rozet 3 locale toplamını yayınlıyor ("1.725 passing") |
| B6 | `arm64` yarım destekli: betik kabul ediyor, yayın assert'i `-x64` |
| B7 | Tekrarlanabilirlik jpackage/tar katmanında kanıtlanmamış |
| B8 | JDK yama sürümü ve Maven runner'a bağlı |
| Q1 | Kapsam/SBOM/statik analiz yok |
| M3 | CONTRIBUTING/CHANGELOG/SECURITY/CODE_OF_CONDUCT yok |
| M4 | Yayın notu sürüm başına değişiklik listesi içermiyor |
| M5 | Bus factor = 1 |

---

## 15. Ürünleştirme Olgunluk Değerlendirmesi

Skorlar 1–5; gerekçe her satırda. Bunlar karşılaştırma değil, **nerede durduğumuzu
gösteren** ölçeklerdir.

| Eksen | Skor | Gerekçe |
|---|---|---|
| **Veri güvenliği** | **4,5** | atomik+fsync kayıt, içerik özetli çakışma, üç nesil yedek, 30 s taslak, gerçek `SIGKILL` testi; eksiler D1–D4 ve kodlama delikleri (D3/D4) |
| **Kod mimarisi** | **4,0** | saf karar sınıfları, tek kompozisyon kökü, tek envanter ilkesi, G/Ç ayrımı; eksiler `FileService` şişkinliği, `AppSettings` eşzamanlılığı, tek-belge varsayımı |
| **Test güvenilirliği** | **2,5** | 575 hızlı test ve kusursuz disiplin, ama UI davranışı ve kurtarma G/Ç yolu kapsam dışı; bir kısım test metin pinliyor; kapsam ölçümü yok |
| **Editör yetkinliği** | **2,0** | "yaz" adımı iyi; revizyon/teslim (yazdırma, dışa aktarma, sözlük, çoklu belge, vurgulama) yok |
| **UX / keşfedilebilirlik** | **3,0** | tema/kart/kısayol disiplini, iyi hata metinleri; eksiler U1 (kilit), U4 (fare-only), U5 (diyalog semantiği) |
| **Erişilebilirlik** | **2,5** | kontrast ve odak halkası ölçülüp pinlenmiş (nadir); ama ekran okuyucu semantiği, klavye eşdeğerleri ve gerçek test yok |
| **Yerelleştirme** | **4,5** | 6 dil × 156 anahtar, parite testi, yerel ayar bağımsız tarih/sayı biçimi, Türkçe i-nokta tuzağı bilinçli kapatılmış |
| **Performans (belge G/Ç)** | **4,0** | 10 MiB'de ~32 ms kaydet, 0,12 ms istatistik; ölçülmemiş UI gecikmeleri eksik |
| **Paketleme / dağıtım** | **2,0** | özenli betikler ama B1 kırığı, %49 şişkin imaj, imza yok, `.msi` doğrulanmıyor |
| **CI / yayın mühendisliği** | **3,0** | CI iyi (kapılar, smoke, locale); yayın hattı kendi kapılarını koşmuyor, SBOM/imza/kapsam yok |
| **Dokümantasyon** | **2,5** | README iki dilli ve kapsamlı, lisans izlenebilirliği üstün; ama test tablosu/`NOTICE`/javadoc bayat, CONTRIBUTING/CHANGELOG yok |
| **Ürün/pazar** | **1,0** | 0 yıldız, 36 indirme, 0 dış katkı; talep kanıtı yok |

**Yorum:** "Motor" (veri güvenliği, yerelleştirme, G/Ç, test disiplini) 4+; "ürün"
(dağıtım güveni, editör yetkinliği, pazar) 1–3. Fark, kod yazma hızıyla değil,
**dağıtım ve talep** eksenleriyle kapanır. Aynı fark, P3'ün neden yalnız "daha çok
özellik" olamayacağını da gösteriyor.

---

## 16. İnsan Kararı Gerektiren Açık Sorular

Bunlar analizle kapanmaz; ürün sahibinin kararı gerekir. (Sıralama değildir.)

1. **Kanal ekseni:** kurulum paketi mi, taşınabilir arşiv mi ürün varsayılanı? Release
   notu bunu hâlâ "açık karar" olarak bırakıyor. Cevap; güncelleme, imza ve ilk çalıştırma
   deneyiminin tamamını belirler.
2. **Platform ekseni:** yalnız Linux+Windows doğrulanmış; arm64 desteği kodda yarım
   (B6). Desteklenecek mi, yoksa `arm64` kabulü kaldırılacak mı?
3. **Ürün sınırı:** "taslak aracı" olarak mı kalınacak, yoksa revizyon/teslim adımı
   (yazdırma, dışa aktarma, sözlük) kapsama alınacak mı? Bu, §8.2 tablosunun tamamını
   ve tek-belge mimarisinin geleceğini belirler.
4. **Çoklu belge:** sekme/çoklu belge isteniyor mu? İsteniyorsa `FileService`'in belge
   başına örneklenmesi şart (§6.3); istenmiyorsa bu kısıt dokümante edilmeli.
5. **İmza bütçesi:** kod imzalama sertifikası (Windows Authenticode) alınacak mı?
   Alınmazsa SmartScreen uyarısı ürünün ilk izlenimi olmaya devam eder.
6. **Ağ politikası:** "hiç ağa çıkmama" ilkesi otomatik güncellemeyi de dışlıyor.
   Güncelleme isteniyorsa bu ilke yazılı olarak yeniden tanımlanmalı (yalnız sürüm
   kontrolü? imzalı paket? kullanıcı onaylı?).
7. **Talep stratejisi:** 0 yıldız/36 indirme tablosunu değiştirmek kod işi değil;
   dağıtım kanalları (Flathub/AUR/winget/Chocolatey), tanıtım ve issue/katkı altyapısı
   kararı gerekiyor. Bu karar verilmezse teknik işin getirisi ölçülemez.
8. **Lisans/ticari model:** Apache-2.0 kalıcı mı? Ürün "ücretsiz araç" olarak mı
   kalacak, yoksa bağış/ticari destek ekseni mi kurulacak? (Yan ürün: imza ve depolama
   maliyetlerinin sahibi.)

---

## 17. Ölçüm Protokolü (yeniden üretilebilirlik)

Aşağıdaki komutlar bu belgedeki A-seviyesi sayıları üretir. Hepsi depo kökünden.

```bash
# 1) Test: sayı + süre + sonuç
mvn -B clean test | tail -20
grep -rc '@Test' src/test/java | awk -F: '{s+=$2} END{print s}'

# 2) Kod/test hacmi ve yorum oranı
find src/main/java -name '*.java' | wc -l
find src/main/java -name '*.java' -exec cat {} + | wc -l
find src/test/java -name '*.java' -exec cat {} + | wc -l

# 3) Artefaktlar (jpackage + installer)
mvn -B clean package
./scripts/package.sh --skip-build --installer
du -sb releases/linux/JTypeWriter
du -sb releases/linux/JTypeWriter/lib/runtime
dpkg-deb -f releases/linux/jtypewriter_1.1.0_amd64.deb Installed-Size Depends

# 4) B1 regresyonu (eski bir .deb dururken ikinci kez paketle)
./scripts/package.sh --skip-build --installer; echo "exit=$?"   # beklenen: 1, "found 2"
./scripts/verify-artifacts.sh --skip-build; echo "exit=$?"      # aynı nedenle düşer

# 5) Çalışma zamanı modül ihtiyacı ve jlink kazancı
MP=$(find ~/.m2/repository/org/openjfx -path '*26.0.1*' -name '*-linux.jar' | tr '\n' ':')
jdeps --multi-release 25 --module-path "$MP" --print-module-deps \
      --ignore-missing-deps target/jtypewriter-1.1.0.jar
jlink --module-path "$MP" \
      --add-modules java.base,java.desktop,java.logging,javafx.controls,javafx.fxml,javafx.media,jdk.unsupported \
      --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output /tmp/jtw-rt
du -sb /tmp/jtw-rt

# 6) Açılış + RSS (X11; uyku yok, pencere kimliği yoklanır)
APP=releases/linux/JTypeWriter/bin/JTypeWriter
H=$(mktemp -d); xwininfo -root -tree | grep -o '0x[0-9a-f]*' | sort -u > /tmp/before
start=$(date +%s%N); HOME="$H" "$APP" & PID=$!
until xwininfo -root -tree | grep -q FxApplication; do sleep 0.005; done
echo "startup ~$(( ($(date +%s%N)-start)/1000000 )) ms"
ps -o rss= -p $PID

# 7) Belge G/Ç'si (10 MiB)
#   AtomicFiles.writeMoving: 10,4 ms | readAllBytes: 2,2 ms
#   TextCodec.decode: 20,2 ms | SHA-256: 11,3 ms   (sıcak önbellek, 10 koşunun en iyisi)

# 8) Kamuya açık ürün metrikleri
curl -s https://api.github.com/repos/hyperpostulate/JTypeWriter | grep -E 'stargazers|forks|open_issues'
curl -s https://api.github.com/repos/hyperpostulate/JTypeWriter/releases | grep -E 'tag_name|download_count'
```

**Bilinen ölçüm sınırları:** başlangıç süresi X11 pencere eşlemesine dayanır (karşılama
kartının görünmesi değil); RSS JVM'in kendi raporu değil `ps` değeridir; G/Ç sayıları bu
makinenin NVMe'sine özgüdür; Windows'ta hiçbir ölçüm yapılmadı.

---

## 18. Tek Paragraf

JTypeWriter, tek kişinin elinden çıkmış ama mühendislik disiplini bir ekip ürününü
andıran bir düz metin editörü: atomik ve fsync'li kayıt, gerçek kodlama koruması, süreç
öldürme testiyle kanıtlanmış çökme kurtarması, 575 hızlı test, altı dilde kusursuz
parite, tek kaynaklı sürüm/ikon/font ve "neden böyle" yazan javadoc. Buna karşılık ürün,
üç yerde kendi iddiasının gerisinde kalıyor: **(1) dağıtım** — 146 MiB'lik imajın yarısı
gereksiz, kurulum paketleri imzasız, `.msi` doğrulanmıyor, sürüm paketleyicisi bu
makinede deterministik olarak kırılıyor ve yayın hattı CI'ın kapılarını yeniden koşmuyor;
**(2) editör yetkinliği** — yazma adımı iyi, revizyon ve teslim adımı (yazdırma, dışa
aktarma, sözlük, çoklu belge, arama genişletmeleri) hiç yok; **(3) talep** — beş sürüm,
36 indirme, 0 yıldız. Veri güvenliğinde kalan açıklar (D1–D4: sınırsız biriken canlı
taslaklar, kurtarmada yol/kodlama sızıntısı, kayıplı "kayıpsız UTF-8") küçük ama ürünün
en çok güvendiği vaade dokundukları için önemsiz sayılamaz. Bugünkü ağaç, "motoru bitmiş,
ambalajı ve pazarı başlamamış" bir üründür; P3'ün işi bu farkı kapatmaktır.
