# JTypeWriter — Kapsamlı Analiz ve Ürünleştirme Değerlendirmesi (P2 Girişi)

> **Sürüm:** 4.1 · **Tarih:** 2 Kasım 2026 · **Kod tabanı:** `main` @ `f32d3f8` (137 commit, ilk commit 2026-07-02)
> **Kapsam:** P0, P1, P1.2 ve G0 kapanışının doğrulanması; mevcut ürün durumunun **yeniden ölçülmesi**;
> P2 için karar girdileri. **Bu belge bir yol haritası değildir.** İş sıralaması, sahiplik, kapılar
> ve takvim ayrı belgededir: `docs/product-roadmap.md` v2.0 (2 Kasım 2026) bu analizin §13'ünü
> girdi olarak kullanır (§13 yalnız *aday havuzu* ve *bağımlılık kısıtları* verir, sıra vermez).
> **Yöntem:** Her bulgu ya bu makinede alınmış bir ölçüme ya da `dosya:satır` düzeyinde okunmuş
> koda bağlanmıştır. Ölçülemeyen iddialar açıkça "ölçülmedi" olarak işaretlenmiştir (§2.1).
> **Önceki belgeyle ilişki:** v3.1 (`95b386e`, 30 Ekim 2026) bu belgenin atasıdır. İki belge
> arasında yalnız dört commit vardır (`67a36a8`, `03c277c`, `f32d3f8`, `8a649ed`), bu yüzden
> yapısal bulguların çoğu taşınmıştır; ama **her biri yeniden doğrulanmış**, performans iddiaları
> ise ilk kez **ölçülmüştür** — ve ölçüm, v3.1'in en çok tekrarlanan performans iddiasını
> çürütmüştür (§8.3). Bulgu kimlikleri (`D/E/X/U/B/M/T`) korunmuş, yeni bulgular eklendi,
> çürütülenler `GEÇERSİZ` olarak işaretlenmiştir.
> **Ölçüm ortamı:** Linux x64 (Ubuntu 26.04, ThinkPad), sıcak önbellek, `DISPLAY=:0.0`, X11;
> JDK 25.0.2 (Corretto), Maven 3.9.12. Artefakt ölçümleri 2 Kasım 2026 tarihli **taze** build
> üzerinde alındı (§10.1). Ölçüm komutları §15'tedir.
> **v4.1 değişikliği:** macOS desteklenen platform kümesinden **tamamen çıkarıldı** (kod, CI,
> paketleme, ikonlar, testler ve dokümantasyon). Gerekçe, kapsam ve düşen hükümler §2.4'te;
> artefakt sayılarındaki küçük fark §10.1'de açıklanmıştır. Belgenin geri kalanı — bulgular,
> ölçümler, skorlar — bu karardan **etkilenmemiştir**; yalnız macOS'a özgü kalemler düştü.

---

## 1. Yönetici Özeti

### 1.1 Tek cümle

JTypeWriter bugün **veri kaybını engelleme konusunda gerçekten iyi, ama bir yazarın günlük
döngüsünü tamamlayamayan, hiç yayınlanmamış bir masaüstü uygulamasıdır**: kaydetme yolu sağlam,
kodlama koruması sağlam, çökme kurtarması sağlam; buna karşılık cümle arama, menü, komut
satırından açma ve yazdırma yok — ve depoda tek bir tag, GitHub'da tek bir Release, tek bir
yıldız yok.

### 1.2 Ölçülen tablo (hepsi bu makinede, bu kod tabanında)

| Ölçüt | Değer | Kaynak |
|---|---|---|
| Dil / UI / test | Java 25, JavaFX 26.0.1, JUnit Jupiter 6.0.3, Mockito 5.23.0 | `pom.xml:52,62-64` |
| Üretim kodu | **51 sınıf, 7.611 satır** | `find src/main/java` |
| Test kodu | **42 dosya, 40 test sınıfı, 350 test, 7.041 satır** (üretime oran **0,93**) | `grep -c @Test`, `find` |
| Test sonucu | **350 koştu / 0 hata / 0 atlandı** | `mvn -B clean test` (bu makine) |
| Yerelleştirme | **6 bundle × 90 anahtar**, EN taban, iki yönlü parite testli | `src/main/resources/messages*.properties` |
| Uygulama imajı | **152.465.247 B = 145,4 MiB**, 125 dosya | `releases/linux/JTypeWriter` |
| ├ çalışma zamanı | **139.967.672 B = 133,5 MiB = %91,7**, **52 modül** | `lib/runtime`, `release` dosyası |
| └ uygulama JAR'ı | **11.009.822 B = 10,5 MiB**, 6.105 giriş, 91 uygulama sınıfı | `lib/app/*-jar-with-dependencies.jar` |
| JAR açılmış boyut | 23.174.417 B | `unzip -l` toplamı |
| `.deb` | **49.488.700 B = 47,2 MiB**, `Installed-Size` 149.140 KiB, **12** runtime bağımlılığı | `dpkg-deb -f` |
| Açılış — ana pencere | **576–588 ms** (5 koşu) | X11 eşlemesi, taze profil |
| Açılış — karşılama kartı | **1.616–1.668 ms** (5 koşu) | aynı koşular |
| Bellek (RSS) | **296.880–303.720 KiB ≈ 290–297 MiB** | `ps -o rss`, karşılama açıldıktan sonra |
| Kaydetme (10 MB, yerel NVMe) | **3,9 ms** (sıcak) / 7,1 ms (soğuk), fsync dahil | §8.2 ölçümü |
| İstatistik yenilemesi (10 MB) | **163,7 ms — saniyede bir** | §8.3 ölçümü |
| Depo disk çarpanı | belge + 3 yedek nesli = **4,00×**; kaydetme anında tepe ≈6× | §6.2 ölçümü |
| Yayınlanmış sürüm | **0 tag, 0 Release, 0 yıldız, 0 fork, 0 issue** | `git tag -l`, GitHub API |
| CI | 2 OS × 3 locale, 350 test, paket üretimi — **artefakt hiç çalıştırılmıyor** | `.github/workflows/maven.yml` |
| TODOsuzluk / javadoc | **0** TODO/FIXME/XXX, **0** `@author`, **347** javadoc bloğu | `grep` |

### 1.3 En önemli on bulgu

1. **D1 — Dış değişiklik algılama yok.** Yüklemede ne mtime, ne boyut, ne içerik özeti saklanıyor;
   kaydetme koşulsuz yazıyor (`FileService.java:69-74`, `:310`). `git checkout`, senkron istemcisi
   veya ikinci bir editörün yazdığı metin **uyarı vermeden** silinir. Ürünün "hiçbir şey kaybolmaz"
   tezinin en doğrudan çürütücüsü.
2. **D2 — Symlink'li belge kaydedilince link yok oluyor — artık kanıtlanmış.** Bu makinede
   yapılan deneyde (`AtomicFiles.writeMoving`, 11 satır): kayıttan sonra `isSymbolicLink=false`,
   link yolu yeni metni tutuyor, **gerçek hedef eski metinde kalıyor** ve iki dizinde de aynı adlı
   iki dosya oluşuyor (§6.3). v3.1 bunu kod okumasıyla söylüyordu; artık ölçüm var.
3. **X5 — Gerçek performans darboğazı otomatik kayıt değil, 1 Hz istatistik.** 10 MB'lık bir
   belgede kaydetme 7 ms (fsync dahil), ama `StatsBar`'ın saniyede bir çalışan yenilemesi
   **163,7 ms**. Yani saniyenin %16'sı, her saniye, FX thread'inde tam metin taramasına gidiyor —
   30 saniyede ~4,9 s, yani otomatik kaydın (~43 ms) **115 katı** (§8.3). v3.1'in "kaydetme donar"
   iddiası yerel diskte **yanlıştı**; doğru teşhis bu.
4. **E1/E2/E3 — Günlük döngünün üç ayağı yok.** Ara/Değiştir (ne model, ne kısayol, ne anahtar),
   menü çubuğu (Yeni ve Farklı Kaydet yalnız klavyede), komut satırından/OS'ten açma
   (`Application.getParameters()` hiç çağrılmıyor, `Dragboard` yok, MIME ilişkilendirmesi yok).
5. **B1/B2 — "Paketlendi" hiç "çalışıyor" olarak doğrulanmadı ve ürün hiç yayınlanmadı.**
   CI üretilen imajı açmıyor, `.deb`'i kurmuyor (`maven.yml`); `git tag -l` boş, GitHub
   Releases API `[]` döndürüyor. Release hattı uçtan uca **hiç koşmadı**.
6. **D3 — Taslak başlığındaki yol doğrulanmıyor.** Base64'ten çözülen değer doğrudan `Path.of`
   ile kabul ediliyor (`RecoveryStore.java:333-344`), sonra `FileService.restoreRecovered`
   üzerinden **sonraki Ctrl+S'in hedefi** oluyor.
7. **X1 — Mimarinin en zayıf ekseni hâlâ eşzamanlılık.** `src/main`'de `ExecutorService`,
   `CompletableFuture`, `Task`, `Service`, `new Thread`: **sıfır**. Tek eşzamanlılık izi
   `CrashHandler`'daki `Platform.runLater`. Açma, kaydetme, otomatik kayıt, kurtarma taraması ve
   1 Hz istatistik, hepsi FX thread'inde (§5.4).
8. **B4 — 145,4 MiB paketin %91,8'i tam JDK; jlink yok.** 52 modül taşınıyor (`jdk.compiler`,
   `jdk.javadoc`, `jdk.jshell`, `jdk.jfr`, `jdk.jconsole`, `jdk.attach` dahil). Bu makinede
   yapılan jlink denemesi gerekli 8 modülle **62.248.693 B (≈59 MiB)** verdi: kabaca **74 MiB**
   tasarruf, hiçbir özellik kaybı olmadan (§10.2).
9. **B14 — Tek yeniden üretilebilirlik garantisi CI'da koşmuyor.** `scripts/verify-artifacts.sh`
   üç kopyayı bayt bayt karşılaştırıyor (17 karşılaştırma), ilk koşusunda gerçek bir kusur buldu
   (`.deb` yükü lisans dosyalarını taşımıyordu) — ama **hiçbir workflow bu betiği çağırmıyor**
   (§10.4). Aynı şekilde ikon `--check` de CI'da değil.
10. **Pazar teması hâlâ kanıtsız ve bu, listedeki boşluklardan büyük risk.** 350 test, sıfır TODO,
    347 javadoc bloğu, titiz bir lisans/ambalaj hikâyesi var; buna karşılık 0 yıldız, 0 Release,
    0 indirme. Ürünü kendisi yazmayan bir yazarın açtığına dair **hiçbir kanıt yok** (§4.4).

### 1.4 Hüküm

**Motor ≠ ürün, ve motor artık gerçekten iyi.** P0 ve P1, ürünün en pahalı ve en az görünen
kısmını — veri bütünlüğü — büyük ölçüde çözdü: atomik yazma, dönen yedek, üç durumlu kilit,
çıkış matrisi, kodlama matrisi, süreç öldürme testi. Bu, aynı ölçekteki masaüstü projelerde nadir
bir tabandır ve P2'nin üzerine kurulacağı zemin budur.

Ama üç şey eksik ve üçü farklı katmanlarda:

| Katman | Durum | Eksik |
|---|---|---|
| **Motor** | İyi (3,6/5) | Belge kimliği: dış değişiklik, symlink, taslak yolu (§6) |
| **Deneyim** | Zayıf (2,2/5) | Ara/değiştir, menü, yazdırma, keşfedilebilirlik (§7) |
| **Ambalaj + erişim** | Zayıf (2,1 / 1,5) | Yayın yok, artefakt çalıştırılmıyor, jlink yok, imza yok, OS entegrasyonu yok (§10) |

Skorlar (§12): **motor 3,6 · deneyim 2,2 · ambalaj 2,1 · güven 2,9 · pazara erişim 1,5**;
ağırlıklı endeks **2,53/5**.

**P2'nin sorusu şu olmalı:** *"Bir romancı JTypeWriter'ı açtığı gün bir bölümü baştan sona yazıp
kaydedebilir mi, ertesi gün aynı dosyaya güvenle dönebilir mi — ve bunu indirdiği ikili
gerçekten yapabiliyor mu?"* Bugünkü cevap **hayır**: cümle arayamaz, dosyayı OS'ten açamaz,
yazdıramaz, dışarıdan değişen dosyayı fark etmez ve indirecek bir sürüm yok.

---

## 2. Yöntem, Kanıt Güvenilirliği ve Ölçüm Ortamı


### 2.1 Kanıt seviyeleri

| Seviye | Ne demek | Bu belgedeki bulgular |
|---|---|---|
| **Ölçüldü** | Bu makinede komut/araç çalıştırıldı, sayı veya süre alındı | Boyutlar, açılış, RSS, sınıf/test/anahtar sayıları, açma/kaydetme/istatistik süreleri, symlink deneyi, disk çarpanı, jlink denemesi, test sonucu, GitHub API |
| **Koddan okundu** | Davranış kaynakta izlendi, `dosya:satır` var | D, E, X, U, M bulgularının çoğu |
| **Çıkarıldı** | Koddan mantıksal sonuç, bu makinede ölçüm yok | 50 MB ve ağ dosya sistemi senaryoları, Windows artefakt boyutları, 1.000 taslaklı kurtarma dizini |
| **Doğrulanamadı** | Kod garanti vermiyor, test yok | Render edilmiş CSS yerleşimi, gerçek Tab sırası |

**Kural:** Bir sayı bu belgede varsa ya §15'teki bir komutla yeniden üretilebilir ya da
`dosya:satır` olarak gösterilmiştir. "Ölçülmedi" etiketi bir eksiklik itirafıdır; tahminle
doldurulmamıştır.

### 2.2 Bu belgenin v3.1'den farkı

| Konu | v3.1 (30 Ekim, `95b386e`) | v4.0 (1 Kasım, `f32d3f8`) |
|---|---|---|
| Kod tabanı | 4 commit geride | HEAD |
| Test sonucu | "350 test yeşil" (CI'a atıf) | **Bu makinede koşuldu**: 350/0/0 |
| Açılış süresi | 601–772 ms / 1.881–2.228 ms | **576–588 ms / 1.616–1.668 ms** (5 koşu, taze profil) |
| RSS | ~274 MiB | **~297 MiB** (ölçüm daha yüksek; bkz. §8.1 not) |
| Kaydetme maliyeti | "1 MB'da duraklama, 10 MB'da donma" *(çıkarım)* | **10 MB'da 7,1 ms** — iddia **çürütüldü** (§8.3) |
| İstatistik maliyeti | "O(n), ölçülmedi" | **10 MB'da 163,7 ms/s** — iddia **doğrulandı ve ağırlaştı** |
| Symlink (D2) | Kod okuması | **Deneyle kanıtlandı** (§6.3) |
| Disk çarpanı | "belgenin ~5 katı olabilir" *(çıkarım)* | **Kalıcı durumda 4,00×, tepe ~6×** (§6.2) |
| jlink kazancı | "P3'e ertelendi, ~50 MiB hedefi" *(tahmin)* | **8 modülle 59 MiB ölçüldü** (§10.2) |
| Yeni bulgular | — | **B14** (verify-artifacts CI'da değil), **X5** (istatistik darboğazı), **B15** (ikon `--check` CI'da değil), **T7** (test sayısı ≠ yol kapsamı) |

### 2.3 Ölçüm artefaktlarının tazeliği

`releases/` ve `target/` sürüm kontrolünde **değil** (`.gitignore`), yani bir artefakt iddiası
ancak üretildiği koşu kaydedilirse anlamlıdır. §10.1'deki tüm sayılar **2 Kasım 2026'da macOS
kaldırıldıktan sonra alınmış taze bir build** kümesinden geliyor (1 Kasım kümesiyle farkı §10.1
notunda tek tek gösterilmiştir); `scripts/verify-artifacts.sh` bu build üzerinde **17/17
karşılaştırmayı geçti** (W0.2'nin kanıtı).

**Kalan hijyen borcu:** `releases/` hâlâ `.gitignore`'da (§10.9). Bu tablodaki hiçbir sayı git
tarafından korunmuyor; bir sonraki ölçüm §15'teki komutlarla baştan üretilmelidir.

### 2.4 Kapsam kararı — macOS üründen kaldırıldı (2 Kasım 2026)

Belgenin ilk hâli platform kümesini **Linux + Windows + macOS** varsayıyordu ve §13.5'teki
karar sorusunu bu üç OS üzerinden kuruyordu. Bu varsayım artık **geçersizdir**: macOS
desteklenen platform kümesinden **tamamen çıkarılmıştır**. Karar, §10.6b'nin analitik
sonucunun doğrudan uygulanmasıdır — notarization bir teknik engel değil bir **bütçe**
problemidir ve taşınabilir kanal onu çözmez; dolayısıyla macOS'u hedeflememek tek
sıfır-maliyetli seçenektir.

Kaldırma **kod, CI, paketleme ve dokümantasyon düzeyinde tamamlanmıştır**:

| Katman | Değişiklik |
|---|---|
| `scripts/package.sh` | `Darwin*` platform dalı, `.icns` ikon dalı, `.app`/`Contents/Resources` lisans yolu, `dmg` installer dalı ve `.dmg` temizliği silindi. Desteklenmeyen platform artık açık hata verir |
| `scripts/release-assets.sh` | `mac)` dalı (`JTypeWriter.app`, `*.dmg`) silindi; `<os>` kümesi `linux \| windows` |
| `scripts/verify-artifacts.sh` | `Darwin*` algılama, `.app` imaj yolu ve `Contents/Resources` dalları silindi |
| `scripts/generate-icons.py` | `ICNS_CHUNKS`, `build_icns()` ve `.icns` çıktısı silindi; `jtypewriter.icns` depodan çıkarıldı |
| `AppIconsTest` | ICNS sihirli-bayt iddiası kaldırıldı (ICO iddiası duruyor) |
| `KeyboardShortcutHandler` | macOS `Cmd` anlatan Javadoc düzeltildi; `isShortcutDown()` platform-nötr JavaFX API'si olduğu için kaldı |
| `.github/workflows/*` | Matris `[ubuntu-latest, windows-latest]`; `release.yml`'den `macOS)` dalı ve iki `mac-arm64` beklenen-varlık satırı çıktı (beklenen varlık sayısı **7 → 5**) |
| `docs/release-notes-template.md` | `.dmg` ve Gatekeeper referansları iki dilden de çıktı; Windows SmartScreen duruyor; iki dilli parite ve `@VERSION@`/`@TAG@` korundu |
| `README.md`, `README-TR.md` | Platform rozeti, tanıtım cümlesi, kısayol notu, indirme/derleme bölümleri, ikon ve CI satırları güncellendi |
| `scripts/checksums.sh` | `shasum` yedeği artık macOS gerekçesiyle değil, coreutils'siz ortam gerekçesiyle açıklanıyor |

**Bu kararın belgeye etkisi — hangi hükümler düştü, hangileri daraldı:**

| Öğe | Önce | Sonra |
|---|---|---|
| **B3** (imza yok) | macOS notarization + Windows Authenticode | Yalnız **Windows Authenticode/SmartScreen** kalır; notarization bütçesi tamamen düşer |
| **U10** (macOS çıkış yolu) | Orta bulgu | **Kapsam dışı — geçersiz** (§9.6 karar kaydı) |
| **U4** (tooltip `Ctrl` diyor) | macOS kullanıcısına yanlış tuş | **Etkisiz**; tooltip metni artık doğru (§9.4) |
| **A11** (Cmd+Q doğrula) | Aday, skor 4,0 | **Havuzdan çıktı** |
| **B7** (`~/.jtypewriter` her OS'te) | XDG + macOS Application Support | Yalnız **XDG** tartışması kalır |
| **§13.5 kararı** | İki eksen: OS × kanal | OS ekseni **kapandı** (Linux + Windows); kanal ekseni açık kalır |
| **§13.6 kapı 5** | "CI app image'ı başlatır, `.deb` kurar" | Değişmedi; ama artık iki OS doğrulanır, üç değil |
| **§12 pazara erişim skoru** | 1,5 (notarization yok dahil) | Gerekçeden notarization çıkar; skor **değişmedi** (0 tag / 0 Release / 0 MIME hâlâ bağlayıcı) |

Kaldırmanın **gizli faydası** ölçülebilir: `verify-artifacts.sh`'ın karşılaştırma kümesi
küçülür, CI matrisi 9 suite koşusundan 6'ya iner (2 OS × 3 locale), ve "yerelde yalnız Linux
görülür, macOS yalnız CI'da görülür" geri bildirim gecikmesi (§13.4) bir satır kısalır.
Bedeli: macOS kullanıcısı ürünü hiç kullanamaz — bu bilinçli bir pazar feragatidir, kusur değil.

---

## 3. Faz Tarihçesi — Ne Kapandı, Ne Devretti

Ürün bugüne kadar üç ürünleştirme fazı ve bir ön kapı geçirdi. Aşağıdaki tablo, her fazın
**iddiasını** commit kanıtıyla eşleştiriyor; amaç tarih yazmak değil, **hangi borcun hangi
fazda doğduğunu** görünür kılmak.

### 3.1 Fazlar

| Faz | Tema | Kanıt (commit) | Gerçekten kapanan | Kapanmayan / devreden |
|---|---|---|---|---|
| **P0** | "Güvenliği ve görünür kaliteyi kur" | `7f3a2e4`, `9e78478`, `f8f581a`, `37f38f0`, `02034d5` | Atomik kaydetme + dönen yedek, kurtarma taslakları, farklı kaydet üzerine yazma, kodlama katılığı, araç çubuğu sığdırma, tooltip'ler, odak halkaları, kalıcı görünüm, kayıt durumu, BOM katı çözme | Menü, arama, CLI, yazdırma (hiç başlamadı) |
| **P1** | "Veri kaybı sınıfını kapat, amatör görünümü sil" | 12 commit, `PW-01`…`PW-32`, `f69fc6a` | Üç durumlu kilit, çıkış matrisi (`CLEAR/PARK/KEEP`), otomatik kayıt sağlığı, legacy kodlama koruması, yedek sırası, dinleyici izolasyonu, kontrast (genişletilmiş), diyalog düğmeleri, i18n paritesi, gerçek süreç öldürme testi, kodlama matrisi | `Alert` taşması, sabit tarih biçimi, **paket smoke testi** (PW-43 hiç başlamadı); devredilen macOS çıkış yolu 2 Kasım 2026'da kapsamla birlikte düştü (§2.4) |
| **P1.2** | "Yayın bütünlüğü" | `PW-33`…`PW-40`, `PW-42`, `ba0d40c`, `f43c3d9`, `8a649ed` | Tag→Release workflow, `SHA256SUMS`, `THIRD-PARTY-NOTICES`, SPDX başlıkları (tüm `.java`), README'nin gerçekle hizalanması, gizlilik notu, enforcer, iki dilli sürüm notu şablonu, `licenses`+`NOTICE` her artefaktta | Release hattı **hiç koşmadı**; imza yok |
| **G0** (W0) | "P2 ön koşulları" | `67a36a8`, `03c277c`, `f32d3f8` | `AGENTS.md` sürüm kontrolünde; `verify-artifacts.sh` (17/17); `.deb` yükü lisans dosyalarını taşıyor | — |
| **P2 / W1–W4** | "Yayınla, editör ol, güvenceyi koşulsuz yap" | **commit yok** | **Başlamadı** | Tamamı |

**Sonuç:** P0 ve P1 gerçek iş yaptı; özellikle P1'in veri güvenliği dilimi, ürünün en savunulabilir
parçasıdır. Ama P2'nin ilk dalgası (W1: artefakt smoke testi + ilk yayın) **hiç başlamamıştır**.
Bugünkü tablo, "ürünleştirme yapıldı" ile "ürün var" arasındaki farkın tam olarak bu dalgada
olduğunu gösteriyor.

### 3.2 P1'in kapanış denetimi (v3.1'den taşınan, yeniden doğrulanan)

| # | P1 çıkış kriteri | Durum (1 Kasım) | Kanıt / eksik |
|---|---|---|---|
| 1 | Varsayılan temada AA altı tek metin yok | **Kısmi** | `ContrastRatioTest` 19 testle CTA, hover, e-posta, tagline, `.stats-save`, 5 vurgu rengini zorluyor; `-toolbar-text`, düz popup satırları, varsayılan editör metni **kapsam dışı** (§9.2) |
| 2 | Açılışta odak editörde; hiçbir diyalog 768 px'i aşmıyor | **Kısmi** | Özel diyaloglar `DialogSizing` ile kelepçeli; **9 `Alert` kelepçesiz, sarmasız** (§9.3) |
| 3 | Dört çıkış yolunda da kayıp yok | **Kapalı** | `ExitDisposition` + `AutoSaveService.shutdown` + `ProcessCrashRecoveryTest` |
| 4 | Hiçbir yolda sessiz kodlama değişimi yok | **Kapalı** | `EncodingMatrixTest` (11 test), bayt-özdeş gidiş-dönüş |
| 5 | 6 dilde argümanlı anahtarlar doğru; tarih/saat UI diliyle | **Kısmi** | Parite ve `MessageFormat` testli; **tarih/saat desenleri sabit** (§9.4) |
| 6 | `v*` tag'i checksum'lı Release üretiyor | **Kısmi** | Betikler hazır, hat **hiç koşmadı**, tag yok, imza yok |
| 7 | `mvn clean test` + TR/DE + **paket smoke testi** yeşil | **Açık** | İlk ikisi yeşil (350/0/0); **CI'da artefakt çalıştırma yok** (§10.4) |

**Okunuşu:** P1'in en zor kısmı (veri güvenliği + kodlama) gerçekten kapandı. Görünür kalite ve
yayın tarafında ise "kapandı" iddiası **kapsam daraltılarak** doğru: kriter metni geniş,
uygulama dar. P2 planlanırken bu ayrım korunmalı — aksi halde aynı kısmi kapanış tekrarlanır.

### 3.3 P2'ye devreden üç iş (hiç başlamamış)

| İş | Durum | Neden P2'nin ilk dalgasında olmalı |
|---|---|---|
| Üretilen artefaktı CI'da **aç** (imaj başlat, `.deb` kur/kaldır) | Yok | Bugün "paketlendi" ile "çalışıyor" arasındaki tek kanıt yok; ilk gerçek regresyon burada yakalanır. v3.1'in W1.1'i hiç yazılmadı |
| **İlk yayın** (`v1.0.0`) | Yok | `release.yml` uçtan uca hiç koşmadı; ilk sürümde sürpriz çıkma olasılığı yüksek (§10.4) |
| `verify-artifacts.sh` + ikon `--check` CI'ya | Yok (**B14**, **B15**) | Yerelde çalışan bir garanti, CI'da koşmuyorsa bir sonraki PR'da sessizce çürür |

---

## 4. Ürün Kimliği, Pazar Konumu ve Talep Kanıtı


### 4.1 Ne satıyor?

Üç katman var ve üçü farklı olgunlukta:

| Katman | İddia | Bugünkü gerçek |
|---|---|---|
| **Motor** | "Metnini kaybetmek çok zor" | Atomik yazma, dönen yedek, 30 s taslak, çıkış matrisi, kodlama koruması — **gerçek ve testli**. Ancak tek yazar / düz dosya / sağlam FS koşullu; dış değişiklik ve symlink açık (§6) |
| **Deneyim** | "Sessiz, dikkat dağıtmayan yazma" | Tek `TextArea`, zorunlu satır sarma, araç çubuğu, durum çubuğu. Arama, menü, yazdırma yok. "Sessiz" ama **çıplak** |
| **Ambalaj** | "İndir, çalıştır; Java gerekmez" | 145,4 MiB imaj, imzasız, **hiç yayınlanmamış**, OS'ten açılamıyor, jlink yok |

Ürünün **savunulabilir tek farklılaştırıcısı** daktilo sesi değil, **silme engeli + veri
güvenliği** kombinasyonudur. Ses bir tatlandırıcı; YOLO ve kurtarma bir ürün tezi. Bu ayrım
pazarlama cümlesinde de korunmalı: ses, ürünü "bir tür" yapar; veri güvenliği, ürünü "tercih
edilir" yapar.

### 4.2 Kimin için?

- **Birincil:** Tek bir düz metin dosyasında roman/öykü yazan, bulut ve hesap istemeyen, "ilk
  taslak" aşamasında geri silmeyi kapatmak isteyen yazar.
- **İkincil:** Dikkat dağıtıcı olmayan, hafif bir yerel editör arayan Linux/Windows kullanıcısı.
- **Üçüncül (henüz değil):** Kurumsal/atölye kullanımı — çoklu belge, yazım denetimi ve iş
  birliği olmadan uygun değil.

**Persona kanıtı yok.** Bu üç tanım koddan ve README'den çıkarılmıştır; gerçek bir kullanıcıyla
görüşülmemiştir. §4.4'teki boşluk buradan başlıyor.

### 4.3 Rekabet

| Ürün | Güçlü yanı | JTypeWriter'ın ayrıştığı yer | JTypeWriter'ın kaybettiği yer |
|---|---|---|---|
| Word / Google Docs | Her şey, ücretsiz, tanıdık | Hesapsız, çevrimdışı, düz metin, silme engeli | Ara/değiştir, yazdırma, biçimlendirme, güven |
| Scrivener | Proje/klasör, taslak yönetimi | Tek dosya, öğrenme eğrisi yok | Yapı, dışa aktarma, araştırma panosu |
| FocusWriter | Ücretsiz, tam ekran, odak | Silme engeli + kurtarma + istatistik | Tam ekran özelleştirme, günlük hedefler |
| iA Writer / Typora | Tipografi, Markdown | Düz `.txt`, proje formatı yok | Pazarlama, imza, marka |
| Not defteri / gedit | Her yerde var, zaten yüklü | YOLO, ses, kurtarma, otomatik kayıt | Hiçbiri — ama zaten yüklü |

**Konumlandırma cümlesi (öneri):** *"İlk taslağını yazarken yanlışlıkla silemeyeceğin, hesap
istemeyen, tek düz metin dosyası."* Bu cümle bugünkü kodla **kısmen** doğru: "yanlışlıkla
silemeyeceğin" doğru, "dosyanı kaybetmeyeceğin" **koşullu** (§6.5).

### 4.4 Talep kanıtı — en büyük boşluk

| Gösterge | Değer | Kaynak |
|---|---|---|
| Git tag | **0** | `git tag -l` |
| GitHub Release | **0** | GitHub API `/releases` |
| Yıldız / çatal / izleyici | **0 / 0 / 0** | GitHub API `/repos` |
| Açık issue | **0** | GitHub API |
| Depo açılışı | 2026-07-02 | API `created_at` |
| Son push | 2026-11-01 | API `pushed_at` |
| Topluluk dosyası (CONTRIBUTING, CODE_OF_CONDUCT, issue/PR şablonu) | **yok** | `ls .github/` |
| `SECURITY.md` / `CHANGELOG.md` | **yok** | depo kökü |

Dört ayda 137 commit, 350 test, sıfır kullanıcı. Bu bir başarısızlık kanıtı değil — **hiç
dağıtım yapılmadığı** için talep hiç test edilmedi. Ama analiz açısından kritik sonuç şu:
**P2'nin en büyük riski, listede bir maddenin eksik olması değil, bir faz daha
mükemmelleştirip yine yayınlamamaktır.** Bu risk, bulgu listesinin hiçbir satırında görünmez;
bu yüzden §13'te aday havuzunun en üstünde duran iş "en yüksek etkili özellik" değil, **"kanıt
üreten ilk hamle"**dir.

### 4.5 Tez ile gerçek arasındaki mesafe

Ürünün vaadi üç cümleye indirgenebilir; her biri bugün test edilebilir bir koşula bağlı:

| Vaat | Koşul | Bugün |
|---|---|---|
| "Yazdığın metin kaybolmaz" | Tek yazar, düz dosya, sağlam FS, çökme | ✅ Çoğu yol kapalı; **dış değişiklik (D1) ve symlink (D2) açık** |
| "Yanlışlıkla silemezsin" | YOLO açık | ✅ `YoloPolicy` + editör yolu testli |
| "Kaldığın yerden devam edersin" | Tek son dosya | ✅ Son dosya açılıyor; **son dosyalar listesi yok**, karşılama her açılışta |

---

## 5. Mimari Değerlendirme


### 5.1 Ölçülen şekil

| Paket | Sınıf | Satır | Pay |
|---|---:|---:|---:|
| `service/` | 9 | 2.014 | %26,5 |
| `ui/component/` | 10 | 1.697 | %22,3 |
| `ui/` | 13 | 1.414 | %18,6 |
| kök (`AppGraph`, `AppSettings`, `AppLogging`, `AtomicFiles`, `InstanceLock`, …) | 9 | 1.320 | %17,3 |
| `locale/` | 3 | 557 | %7,3 |
| `ui/theme/` | 5 | 384 | %5,0 |
| `audio/` + `stats/` | 2 | 225 | %3,0 |
| **Toplam** | **51** | **7.611** | **%100** |

En büyük on sınıf: `ToolbarView` 662, `Messages` 468, `FileService` 401, `RecoveryStore` 400,
`AppSettings` 394, `AutoSaveService` 352, `TextCodec` 287, `DialogService` 286,
`MainController` 259, `AppLogging` 248.

**Yorum:** `ToolbarView`'un 662 satırı, komut katmanının yokluğunun doğrudan sonucudur (M1):
her düğme, kendi kurulumu + tooltip'i + durum bağlaması + erişilebilirlik metniyle birlikte
orada yaşıyor. Aynı şekilde `Messages` 468 satırı, 90 anahtarın × 6 dilin çeviri erişimcisi
olmasından geliyor — sağlıklı, ama yeni anahtar maliyeti yüksek (§9.5).

### 5.2 Güçlü yanlar (korunması gereken kültürel miras)

- **Framework'süz kompozisyon kökü.** `AppGraph` alan sırası = bağımlılık sırası; dinleyiciler
  kurucuda bağlanıyor. Yeni servis eklemek tek dosyada okunabilir bir iş. `AGENTS.md` bunu
  açıkça kural yazmış.
- **Saf mantık ayrımı gerçekten uygulanmış.** `YoloPolicy`, `StatsBarModel`, `ToolbarFit`,
  `ExitDisposition`, `AutosaveHealth`, `DialogSizing`, `ShortcutRows`, `TextCodec`, `FontSizeManager`,
  `ToolbarState` JavaFX'siz; 350 testin tamamı toolkit açmadan koşuyor. Bu, projenin en değerli
  teknik kararı.
- **Veri yolu tek ilkelden geçiyor.** Tüm yazmalar `AtomicFiles.writeMoving`; yedek
  `BackupKeeper.prepare/install`; kodlama `TextCodec`. "Yeni bir yazma yolu" icat etmek
  mimari olarak zor — ve `AGENTS.md` bunu yasak sayıyor.
- **Test disiplini.** 7.041 test satırı / 7.611 üretim satırı (0,93); `@TempDir`, POSIX/root
  varsayımları, TR/DE locale koşuları, gerçek süreç öldürme testi.
- **Yorum kültürü.** 347 javadoc bloğu, 0 TODO/FIXME, 0 `@author`; yorumlar "ne" değil "neden
  böyle değil" anlatıyor (`AtomicFiles.preserveAttributes`, `FileService.writeFile`).

### 5.3 Yapısal borç

| ID | Borç | Etki | Kanıt |
|---|---|---|---|
| **M1** | **Komut katmanı yok.** Her eylem `KeyboardShortcutHandler` + `ToolbarView` + `MainController` üçlüsüne ayrı ayrı bağlanıyor; menü, CLI, arama, palet için ortak "Action" soyutlaması yok | Menü/CLI/arama her biri yeni kablolama demek; keşfedilebilirlik borcu kalıcılaşır | `KeyboardShortcutHandler`, `ToolbarView`, `MainController` |
| **M2** | **Tek belge varsayımı kök hücrelerde.** `FileService` tek `currentFilePath/charset/bom`; `AppGraph` tek `EditorArea`; `MainStage` tek sahne | Sekmeler (P4) için `DocumentSession` refactor'u şart; P2 kararları bunu **kilitlememeli** | `FileService.java:69-74` |
| **M3** | **Diyalog tekrarı.** 9 `Alert` aynı kurulumu tekrarlıyor; sarma/kelepçe/yeniden boyutlandırma yok | U1'in doğrudan nedeni; her yeni diyalog hatayı çoğaltır | §9.3 |
| **M4** | **`AppLogging` global statik.** `logger()` statik başlatıcılarda çağrılıyor; enjeksiyon seam'i yalnız test-görünürlüğünde (`openFileHandler(Path)`) | "Asla gerçek eve dokunma" kuralı uygulanamıyor; testler `~/.jtypewriter/logs`'a yazıyor | `AppLogging.java:37,153,210,223`; `pom.xml:145-149` |
| **M5** | **Dinleyici sırası örtük.** Dil değişiminde `Messages.reload → ToolbarView → başlık` sırası alan sırasına bağlı | Yeniden sıralama sessiz regresyon üretir | `AppGraph` |
| **M6** | **Yansıma ile undo temizliği.** `UndoHistoryReset` JavaFX iç alanlarına gidiyor; dönüş değeri kontrol edilmiyor | jlink/modül yolu engeli; belge sınırını aşan undo riski | `UndoHistoryReset`, `EditorArea` |
| **M7** | **Undo geçmişi sınırsız.** JavaFX varsayılanı | 10 MB+ belgede bellek büyümesi; **ölçülmedi** | `EditorArea` |
| **M8** | **Dispose sözleşmesi yok.** `StatsBar` timeline'ı ve dinleyiciler kaldırılamıyor | İkinci pencere/P4 için sızıntı | `StatsBar` |
| **M9** | **Tek yazma ilkelinin etrafında doğrulama boşluğu.** `AtomicFiles` atomikliği garanti ediyor ama *hedefin ne olduğunu* (symlink, gerçek yol) hiç sormuyor | D2 tam olarak bu boşluktan doğuyor | `AtomicFiles.java:54-74` |
| **M10** | **Boyut/sayı sınırı kavramı yok.** Ne belge boyutu, ne taslak sayısı, ne undo derinliği için sabit veya ayar var | Büyük girdi = tanımsız davranış (D4, D7, M7) | `FileService`, `RecoveryStore` |

### 5.4 Threading — mimarinin en zayıf ekseni

`src/main` içinde eşzamanlılık ilkelleri:

| İlkel | Sayı | Yer |
|---|---:|---|
| `Platform.runLater` | 1 | `CrashHandler.java:54` |
| `Thread.setDefaultUncaughtExceptionHandler` | 1 | `CrashHandler.java:42` |
| `ExecutorService` / `Executors` | **0** | — |
| `CompletableFuture` | **0** | — |
| `javafx.concurrent.Task` / `Service` | **0** | — |
| `new Thread` | **0** | — |

Zamanlama yalnız `javafx.animation.Timeline` (`AutoSaveService.java:136`, `StatsBar.java:105`).

Bunun sonuçları, hepsi FX thread'inde:

- **Açma:** `Files.readAllBytes` + kod çözme + `setText` (`FileService.loadFile`).
- **Kaydetme:** encode + temp yazma + `channel.force` + rename + dizin fsync (`FileService.writeFile`
  → `AtomicFiles.writeMoving`).
- **Otomatik kayıt:** 30 s'de bir tam metin kopyası + UTF-8 serileştirme + atomik yazma + fsync.
- **Kurtarma taraması:** açılışta, `primaryStage.show()` **sonrasında**, senkron; her aday tam
  okunuyor (`RecoveryStore.java:271`).
- **İstatistik:** saniyede bir tam metin taraması (§8.3 — ölçülen en pahalı tekrar eden iş).

**Ölçüm bunu nasıl düzeltiyor:** yerel NVMe'de kaydetme 10 MB'da 7 ms; yani "donma" iddiası
abartılıydı. Gerçek maliyet dağılımı §8.3'te: tekrar eden işin %99'u istatistik yenilemesi.
Bu, mimari kararı değiştirmiyor (G/Ç hâlâ FX thread'inde ve ağ dosya sisteminde aynı kod
saniyeler sürebilir) ama **önceliği** değiştiriyor: önce ölçülen darboğaz, sonra teorik olan.

### 5.5 Genişleme noktaları ve komut katmanının yokluğu

Bugün yeni bir özellik eklemenin maliyeti:

| Özellik | Gereken dosyalar | Neden pahalı |
|---|---|---|
| Ara/Değiştir | Yeni saf model + yeni UI paneli + `EditorArea` entegrasyonu + kısayol + 6 bundle | Ortak komut katmanı yok; YOLO etkileşimi ayrıca tasarlanmalı |
| Menü çubuğu | `ToolbarView`'daki her eylemin yeniden bağlanması + hızlandırıcı tablosu | Eylemler düğümlere gömülü, komut nesnesi yok |
| CLI ile açma | `FxApplication` + `FileService` + tek örnek kilidi ile el sıkışma | İkinci örnek yolu belgeyi **devredemiyor**, sadece reddediyor |
| Yazdırma | Yeni servis + sayfa düzeni + yazıcı diyaloğu | Metin modeli düz `TextArea`; sayfalama yok |

**Mimari sonuç:** P2'ye girmeden önce **küçük bir komut/eylem katmanı** (M1) kurulursa menü,
CLI, arama ve gelecekteki palet aynı yerden beslenir. Kurulmazsa her özellik kendi kablolamasını
getirir ve keşfedilebilirlik borcu kalıcılaşır.

---

## 6. Veri Güvenliği — P1 Sonrası Durum


### 6.1 P1 ile kapanan yollar (yeniden doğrulandı)

| Yol | Durum | Kanıt |
|---|---|---|
| Kilit "başka örnek var" ile "kilit kullanılamıyor"u ayırmıyordu | **Kapalı** — üç durumlu (`ACQUIRED/ALREADY_RUNNING/UNAVAILABLE`); yalnız `ALREADY_RUNNING` engeller | `InstanceLock.java:46-53,99-110`; `JTypeWriterApp.java:26-29` |
| Kapatma isteği dışı her çıkış taslağı siliyordu | **Kapalı** — `CLEAR/PARK/KEEP`; yalnız yanıtlanmış kapatma siler | `ExitDisposition`, `AutoSaveService.java:275-282` |
| Kurtarma sonrası legacy kodlama UTF-8'e düşüyordu | **Kapalı** — decode `null` ise Windows-1254 korunuyor | `FileService.restoreRecovered` |
| Yedek hatası kaydı iptal ediyordu | **Kapalı** — önce yaz, sonra yedek kur; hata yalnız uyarı | `FileService.java:303-326`, `BackupKeeper.java:49-79` |
| Başarılı kayıt dinleyici hatasıyla "başarısız" görünüyordu | **Kapalı** — dinleyiciler `try` dışında, tek tek izole | `FileService.java:342-349` |
| `NaN`/`Infinity` pencere geometrisi | **Kapalı** — `Double.isFinite` + pozitiflik | `AppSettings.java:339-350` |
| `AppLogging.init()` çökme güvensizdi | **Kapalı** — `initialized` en sonda, `RuntimeException` yakalanıyor | `AppLogging.java:163,232` |
| Bozuk ayar dosyası üzerine yazılıyordu | **Kapalı** — `.corrupt` karantinası | `AppSettings.java:236-270` |

Bu liste, P0/P1'in gerçek kazancıdır ve P2 planlanırken **varsayılan olarak korunmalıdır**:
her yeni veri yolu bu sekiz garantiyi bozmadan eklenmelidir.

### 6.2 Kalan açıklar

#### D1 — Dış değişiklik algılama yok — **Yüksek**

`loadFile` yalnız içerik/kodlama/yol saklıyor: `currentFilePath`, `savedContent`, `currentContent`,
`currentCharset`, `currentBom`, `dirty` (`FileService.java:69-74`). Ne `(mtime, size)` ne içerik
özeti var; `src/main`'de `MessageDigest` hiç geçmiyor. `writeFile` koşulsuz yazıyor
(`:310 AtomicFiles.writeMoving(path, bytes)`); tek karşılaştırma bellekteki
`path.equals(currentFilePath) && content.equals(savedContent)` (`:296`) — bu bir **yedek
rotasyonu** koruması, disk kontrolü değil.

Sonuç: `git checkout`, Dropbox/Syncthing, başka bir editör veya farklı `user.home` ile çalışan
ikinci bir örnek dosyayı değiştirdiyse, kullanıcının **başkasının metni** sessizce kaybolur.
Tek telafi 3 nesillik `.bak`; o da D10'a açık.

Bu, ürünün "hiçbir şey kaybolmaz" tezinin en doğrudan çürütücüsüdür ve P2'nin en yüksek getirili
tek işidir.

#### D2 — Symlink'li belge kaydedilince link yok oluyor — **Yüksek — ÖLÇÜLDÜ**

`AtomicFiles.writeMoving` yolu mutlaklaştırıyor ama `toRealPath()` kullanmıyor
(`AtomicFiles.java:54`); temp dosya linkin yanında oluşturulup `Files.move(..., REPLACE_EXISTING,
ATOMIC_MOVE)` ile **linkin üzerine** taşınıyor (`:71`).

Bu makinede çalıştırılan deney (`~/link-parent/notes.txt -> ~/real/notes.txt`):

```
before: isSymlink=true  target=ORIGINAL TARGET CONTENT
after : isSymlink=false
after : link path now holds   = NEW CONTENT FROM EDITOR
after : real target still holds = ORIGINAL TARGET CONTENT
after : link-parent entries   = [notes.txt]
after : real dir entries      = [notes.txt]
rel   : isSymlink after save  = false      (göreli link de aynı)
```

Yani: gerçek hedef **hiç güncellenmiyor**, link kayboluyor, iki dizinde aynı adlı iki dosya
kalıyor. Senkron klasörüne symlink ile bağlanan bir taslakta bu, "kaydettim" sanıp hiçbir yere
yazmamak demek. `.bak` yedeği de yanlış dizine düşer.

#### D3 — Taslak başlığındaki yol doğrulanmıyor — **Yüksek**

`RecoveryStore` `original.path` değerini Base64'ten çözüp doğrudan `Path.of` veriyor
(`RecoveryStore.java:333-344`); mutlaklık, varlık, kanoniklik veya izinli kök kontrolü yok.
Değer `FileService.restoreRecovered` (`:234,241`) üzerinden `currentFilePath` oluyor ve
**sonraki Ctrl+S'in hedefi** hâline geliyor. Kurtarma dizini kullanıcı tarafından yazılabilir;
başka makineden gelen bir taslak, kaydetmeyi keyfi bir dosyaya yönlendirebilir.

Tek hafifletici: `AutoSaveService.draftPrecedesFile` (`:217-221`) yalnız dosya **daha yeniyse**
yolu düşürüyor — bu bir tazelik kontrolü, yol doğrulaması değil; var olmayan veya keyfi yollar
hâlâ kabul ediliyor.

#### D4 — Taslak taraması symlink takip ediyor ve sınırsız — **Orta**

`Files.list(dir).filter(Files::isRegularFile)` symlink'leri takip ediyor (`RecoveryStore.java:131-139`);
her aday, en yenisi seçilmeden **tamamen** `Files.readAllBytes` ile okunuyor (`:271`). Boyut/sayı
sınırı yok → açılışta donma veya `OutOfMemoryError`. Ayrıca açılış başına yalnızca **en yeni**
taslak sunuluyor (`:145-150`); N ertelenmiş taslak N yeniden başlatma ister.

#### D5 — Park edilen taslaklar hiç toplanmıyor — **Orta**

`parked-*.jtw` dosyaları yaş/sayı/boyutla silinmiyor; yalnız kendisi seçilen taslak
`Files.deleteIfExists` ile gidiyor (`:223`). Disk sınırsız büyür ve **kullanıcının diğer
belgelerinin metinleri** profilde süresiz kalır (gizlilik sorunu).

#### D6 — Atomik olmayan fallback doğrulanmıyor; temp süpürme yok — **Orta**

`ATOMIC_MOVE` desteklenmediğinde `Files.move(temp, absolute, REPLACE_EXISTING)`'e düşülüyor ve
bu yolda boyut/hash doğrulaması yok (`AtomicFiles.java:72-74`). SMB/NFS'te süreç ölürse hedef
yarım yazılmış olabilir. `SIGKILL` sonrası `jtw-*.tmp` hiç süpürülmüyor; ayrıca `:77-79`
`finally` içindeki `Files.deleteIfExists(temp)` hata atarsa **asıl istisnayı maskeliyor**
(Java'da `finally`'de atılan istisna, `try`'daki istisnayı ezer).

#### D7 — Disk alanı kontrolü yok; `Error` kaydetme hata yolunu atlıyor — **Orta**

Boş alan ön kontrolü yok (`getFreeSpace` hiç geçmiyor). Yükleme ve kurtarma `readAllBytes`
kullanıyor. Kaydetme hataları yalnız `IOException | RuntimeException` yakalıyor
(`FileService.java:311,332`; otomatik kayıtta `catch (Exception e)`, `AutoSaveService.java:317`)
— bir `OutOfMemoryError`/`StackOverflowError` **kaydetme hata yolunu atlar** ve kullanıcıya
hiçbir şey söylenmez.

#### D14 — Disk çarpanı 4×, tepe ≈6× — **Orta — ÖLÇÜLDÜ**

Bir belge altı kez kaydedildikten sonra dizin içeriği (1 MB'lık belge):

```
novel.txt     1.048.576 B
novel.txt.bak 1.048.576 B
novel.txt.bak.1 1.048.576 B
novel.txt.bak.2 1.048.576 B
total 4.194.304 B = 4.00x
```

Kalıcı durumda **4,00×** (`GENERATIONS = 3`, `BackupKeeper.java:35`). Kaydetme anında buna
geçici staging kopyası + `jtw-*.tmp` eklenir → **tepe ≈6×**. 50 MB'lık bir belgeyi kaydetmek
300 MB anlık alan ister; disk dolu senaryosu (D7) ile birleştiğinde bu, D10'un tetikleyicisidir.
Hiçbir yerde kullanıcıya söylenmiyor.

#### D8 — Belge düzeyinde kilit yok — **Orta**

`InstanceLock` kullanıcı başına tek kilit (`InstanceLock.java:79,98`); belge üzerinde `FileLock`
yok. Farklı `user.home` ile çalışan ikinci bir örnek veya kilit `UNAVAILABLE` durumundaki ikinci
süreç aynı dosyayı yazabilir. D1 ile birlikte gerçek yarış senaryosu.

#### D9 — "Vazgeç" yolunda taslak silme hatası yutuluyor — **Düşük**

`AutoSaveService.java:201-203`: `if (choice == RecoveryChoice.DISCARD) { draftFrozen = false; store.clear(); }`
— dönüş değeri yok sayılıyor; diğer temizleme yolları `DRAFT_DELETE` uyarısı üretirken bu yol
yalnız log bırakıyor.

#### D10 — Yedek kurulumu en yeni nesli kaybedebilir — **Düşük**

`BackupKeeper.install` önce `.bak.1→.bak.2`, `.bak→.bak.1` döndürüp sonra yeni kopyayı terfi
ettiriyor (`:70-78`); son taşıma başarısız olursa geçmiş kaymış, yeni nesil yok. `FileService`
staged dosyayı siliyor (`:325`) ve kaydı **başarılı** sayıyor — doğru karar, ama sessiz.

#### D11 — `user.home` yoksa açılış NPE ile ölür — **Düşük olasılık / yüksek etki**

`AppDirs.dataDir()` → `Path.of(System.getProperty("user.home"), ".jtypewriter")`
(`AppDirs.java:20`); `JTypeWriterApp.java:25` bunu **ilk ifade** olarak çağırıyor — `Application.launch`
(`:32`) ve `CrashHandler` kurulumundan önce. `user.home` tanımsızsa NPE. Sandbox/konteyner
profillerinde gerçekleşebilir. (Loglama tarafı korumalı: `AppLogging.java:223-235`.)

#### D12 — Ayar yazma hatası sessiz; `SecurityException` kaçıyor — **Orta**

`AppSettings.save()` yalnız `IOException` yakalayıp WARNING'e indiriyor (`:300-308`);
`SecurityException` çağırana kaçar ve in-memory değer değişmişken UI eylemi kesilebilir.
Kalıcı yazılamayan ayar dosyası arayüzde görünmüyor — her tercih her açılışta kaybolur.

#### D13 — Ayar şeması sürümü yok — **Orta (gelecek)**

15 anahtar var (`AppSettings.java:67-81`), hiçbirinde `schema`/`version` yok; bilinmeyen değer
varsayılana düşüyor ama anahtar yeniden adlandırma/göç yok. P2'de eklenecek her yeni ayar
(son dosyalar, "bir daha gösterme", yazı tipi ailesi, otomatik kayıt aralığı) bu borcu büyütür.

### 6.3 Kullanıcıya görünen kayıp/bozulma tablosu

| # | Senaryo | Önem | Kanıt türü |
|---|---|---|---|
| D1 | Senkron/git/başka editör değişikliği uyarısız ezilir | Yüksek | Kod |
| D2 | Symlink'li belge kaydedilir; gerçek hedef sonsuza kadar eski kalır | Yüksek | **Ölçüm** |
| D3 | Taslak başlığı kaydetmeyi keyfi bir dosyaya yönlendirir | Yüksek | Kod |
| D14 | 50 MB belge kaydı ~300 MB anlık disk ister; kimse söylemez | Orta | **Ölçüm** |
| D4 | Çok sayıda/büyük taslak açılışı kilitler veya OOM | Orta | Kod |
| D5 | Diğer belgelerin taslak metinleri profilde süresiz kalır | Orta | Kod |
| D6 | SMB/NFS'te yarım hedef; `jtw-*.tmp` artıkları; maskelenmiş istisna | Orta | Kod |
| D7 | Disk dolu → kaydetme hata yolu atlanır; OOM sessiz | Orta | Kod |
| D8 | İki süreç aynı belgeyi yazar; bir yazarın metni hiçbir yerde kalmaz | Orta | Kod |
| D12 | Yazılamayan ayar dosyası → tercihler her açılışta kaybolur | Orta | Kod |
| D9 | "Vazgeç" sonrası silinemeyen taslak sessizce kalır | Düşük | Kod |
| D10 | Yedek terfisi başarısız → en yeni yedek nesli yok | Düşük | Kod |
| D11 | `user.home` yoksa uygulama açılmaz | Düşük olasılık | Kod |

### 6.4 Bu bölümün test boşlukları

Hiçbir test şunları kapsamıyor: dış değişiklik, symlink hedefi, hard link, taslak başlığında yol
doğrulama, `parked-*.jtw` toplama, disk dolu, `OutOfMemoryError`, iki süreç aynı dosya,
`ATOMIC_MOVE` desteklemeyen dosya sistemi, `DISCARD` sonrası başarısız silme, `install` sırasında
yedek terfi hatası, disk çarpanı.

Yani §6.2'deki **yedi** açığın hiçbiri kırmızıya çevrilebilir bir testle sabitlenmemiş. P2'de
bir bulgu "kapandı" sayılacaksa, önce **onun senaryosunu üreten bir test** yazılmalı; bu, P1'in
kısmi kapanışından çıkarılmış dersdir (§3.2).

### 6.5 Vaat koşulları

Güvenlik hikâyesi şu koşullarda **doğru**:

1. Belgeyi yazan tek süreç var (D1, D8 kapalı değil).
2. Belge symlink değil (D2).
3. Kurtarma dizini güvenilir (D3).
4. Dosya sistemi atomik rename destekliyor (D6).
5. Diskte yer var ve belge belleğe sığıyor (D7, D14).

Bir yazma aracı için bu koşulların **hiçbiri** kullanıcıya söylenmiyor. P2'nin "güven" işi, bu
listenin ilk üç maddesini kapatmak olmalı; dördüncü ve beşinci madde ise en azından **ölçülüp
belgelenmeli**.

---

## 7. Editör Yetkinliği — Günlük Döngü


### 7.1 Var olanlar

Tek düz metin belgesi (`EditorArea extends TextArea`, `EditorArea.java:40`); atomik kaydetme +
dönen yedek; kodlama koruması (BOM, UTF-16 parite, Windows-1254 sorusu); YOLO modu; kelime/
karakter/satır/tuş/süre/WPM; 3 tema; 6 metin rengi; yazı boyutu merdiveni; 6 dil; tam ekran;
araç çubuğunu gizleme; 30 s otomatik kayıt + çökme kurtarma; son dosya ve pencere geometrisi
geri yükleme; daktilo sesi.

Bu liste, **tek belgeli bir yazma aracı** için sağlam bir çekirdek. Eksik olan çekirdek değil,
**günlük döngü**.

### 7.2 Günlük döngü — dokuz adım

| # | Romancının adımı | Bugün | Kanıt |
|---|---|---|---|
| 1 | Belgeyi OS'ten aç (çift tık / "birlikte aç") | **Yok** | `getParameters()` hiç çağrılmıyor; `Dragboard` yok; MIME ilişkilendirmesi yok (E3, E4, E10) |
| 2 | Yaz | **Var** | `EditorArea`, YOLO |
| 3 | Bir cümleyi bul | **Yok** | Ne model, ne panel, ne `Ctrl+F`, ne tek bir bundle anahtarı (E1) |
| 4 | Bulduğunu değiştir | **Yok** | Aynı (E1) |
| 5 | Sahneyi kes/taşı | **Var** (YOLO kapalıyken) | JavaFX varsayılanları |
| 6 | Kaydet | **Var ve hızlı** | 10 MB'da 7 ms (§8.2) |
| 7 | Yazdır / PDF al | **Yok** | `PrinterJob` hiç geçmiyor (E7) |
| 8 | Ertesi gün devam et | **Kısmi** | Son dosya açılıyor; **son dosyalar listesi yok**, karşılama her açılışta (E5, U7) |
| 9 | Sorun bildir | **Kısmi** | Mail adresi + log yolu; tanılama paketi yok (§10.7) |

Dokuz adımın **dördü yok**, biri kısmi, biri kısmi-keşfedilemez. P2'nin gerekçesi bu tablodur —
ve dikkat: adım 6 (kaydet) artık ölçümle "sorunsuz" sayıldığı için bu tablodaki ağırlık
**tamamen yetenek eksikliğine** kaymıştır. v3.1 bu tabloya "biri performans riskli" notu
düşüyordu; ölçüm o notu kaldırdı.

### 7.3 Olmayanlar

| ID | Eksik | Etki | Kanıt (HEAD) |
|---|---|---:|---|
| **E1** | Ara / Değiştir | **5** | `KeyboardShortcutHandler.java:59-73`'te `F`/`H` yok; bundle'da `find`/`search`/`replace` anahtarı yok; `EditorArea.replaceText` (`:79`) YOLO ilkelidir, arama değil |
| **E2** | Menü çubuğu | **4** | `MenuBar`/`new Menu(` hiç yok; tek menü `ContextMenu` (`EditorArea.java:141`). **Yeni** ve **Farklı Kaydet** yalnız `KeyboardShortcutHandler.java:94-100`'de |
| **E3** | CLI ile dosya açma | **4** | `Application.getParameters()` hiç çağrılmıyor; ikinci örnek yolu devretmiyor |
| **E4** | Sürükle-bırak | **2** | `Dragboard`/`setOnDrag*`/`TransferMode` hiç yok |
| **E5** | Son dosyalar | **3** | Yalnız `KEY_LAST_FILE = "last.file"` (`AppSettings.java:74`), liste anahtarı yok |
| **E6** | Tercihler penceresi | **2** | Otomatik kayıt aralığı, yedek nesli, yazı tipi ailesi ayarlanamıyor |
| **E7** | Yazdırma / PDF | **3** | `PrinterJob` yok |
| **E8** | Satır numaraları | **3** | Düz `TextArea`; gutter yok; uzun taslakta yön kaybı |
| **E9** | Sarma anahtarı, yazı tipi ailesi, satır sonu, sekme politikası | **2** | `EditorArea.java:48` `setWrapText(true)` sabit; aile CSS'te (`base.css:15`); satır sonu/sekme politikası hiç yok |
| **E10** | Dosya ilişkilendirmesi / MIME / `.desktop` | **4** | `.deb` içinde `Categories=Unknown`, `MimeType=` boş (§10.6) |

**Şiddetli olan üçlü: E1, E2, E3.** Bunlar "özellik" değil, bir metin editörünün **asgari
tabanı**. E1 olmadan uzun metin, E2 olmadan keşfedilebilirlik, E3 olmadan masaüstü varlığı yok.

### 7.4 Keşfedilebilirlik — ölçülen resim

**Araç çubuğu: 5 düğme + 6 combo.**

| Düğme | Eylem | Tanım |
|---|---|---|
| Aç | `fileService.openFile` | `ToolbarView.java:134`, `:191-194` |
| Kaydet | `fileService.saveFile` | `:135`, `:195-199` |
| Geri Al | `editorArea.requestUndo` | `:136`, `:200-202` |
| Yinele | `editorArea.requestRedo` | `:137`, `:203-205` |
| Hakkında | `aboutDialog.show` | `:138`, `:206-208` |

Combo'lar: tema, YOLO, ses, metin rengi, yazı boyutu, dil (`:140-189`). Sahnede bu sırayla
(`:216-223`).

**Klavye: 15 akor / 14 eylem** (`KeyboardShortcutHandler.java:59-73`):

`Ctrl+T` (araç çubuğu) · `Ctrl+Shift+T` (tema) · `Ctrl+Z` (geri al) · `Ctrl+Y`, `Ctrl+Shift+Z`
(yinele) · `Ctrl+Shift+Y` (YOLO) · `Ctrl+Shift+C` (renk) · `Ctrl+O`/`Ctrl+Shift+O` (aç) ·
`Ctrl+N`/`Ctrl+Shift+N` (yeni) · `Ctrl+S` (kaydet) · `Ctrl+Shift+S` (farklı kaydet) ·
`Ctrl+0`/`Ctrl+Num0` (boyut sıfırla) · `Ctrl++` (büyüt) · `Ctrl+-` (küçült) · `F11` (tam ekran).

**Bulunan üç tutarsızlık:**

1. **Araç çubuğunda olmayan ama klavyede olan iki eylem: Yeni ve Farklı Kaydet.** Yani bir
   kullanıcı, arayüzü tarayarak "yeni bir belge nasıl açarım" sorusunun cevabını **bulamaz**.
2. **Karşılama kartı 16 satır listeliyor, handler 15 akor tanıyor** (`messages.properties:24`).
   Fazladan satır `Escape` (tam ekrandan çıkış) — ama o `KeyboardShortcutHandler`'da değil,
   `MainStage.java:60`'ta. Buna karşılık handler'ın kabul ettiği `Ctrl+Shift+N`, `Ctrl+Shift+O`,
   `Ctrl+Num0`, numpad `+/-` ve shift'li `F11` kartta **yok**. Yani liste ne eksik ne tam:
   **iki yönde de yanlış**.
3. **Menü olmadığı için hiçbir komut hiyerarşisi yok.** 14 eylem düz bir listeye, 5 düğmeye ve
   6 combo'ya dağılmış durumda; "Dosya/Düzen/Görünüm" gibi bir zihinsel model yok.

**Sonuç:** keşfedilebilirlik borcu **ölçüldü**: 14 eylemden **2'si** (%14) arayüzde hiç
görünmüyor, kısayol listesi her iki yönde de handler'la uyuşmuyor. Bu, M1'in (komut katmanı yok)
kullanıcıya görünen maliyetidir.

### 7.5 Düz metin editörü olmanın sınırları

Ürün bilinçli olarak "düz `.txt`, proje formatı yok" diyor ve bu doğru bir konumlanma. Ama
bunun iki sonucu var ve ikisi de bugün ele alınmamış:

1. **Biçim yok, yapı yok.** Bölüm başlığı, sahne ayırıcı, not için hiçbir işaretleme yok.
   Scrivener'a karşı kaybedilen yer burasıdır ve P2'de **kapatılmamalıdır** (kapsam patlar);
   ama kullanıcıya **açıkça söylenmelidir** (README bunu kısmen yapıyor).
2. **Büyük tek dosya normaldir.** Bir roman tek `.txt`'te 300 KB–1 MB olur. Bu boyutta bugünkü
   tek gerçek sorun istatistik yenilemesidir (§8.3), arama değil — çünkü arama yok. **Arama
   eklendiğinde** maliyeti doğru tasarlanmalı: naif `indexOf` döngüsü 1 MB'da kabul edilebilir,
   10 MB'da değil.

---

## 8. Performans ve Ölçek


Bu bölüm bu belgenin en çok değişen kısmıdır: v3.1 performansı **koddan çıkarıyordu**, burada
**ölçülüyor**. Sonuç, v3.1'in ana performans iddiasını çürütüyor ve yerine daha keskin bir
teşhis koyuyor.

### 8.1 Açılış, bellek, artefakt

| Ölçüt | Değer | Koşul |
|---|---|---|
| Ana pencere (X eşlemesi) | **576, 582, 583, 585, 588 ms** | 5 koşu, taze profil (`-Duser.home=<tmp>`), sıcak önbellek |
| Karşılama kartı | **1.616–1.668 ms** | aynı koşular, ilk modal pencere |
| RSS (karşılama sonrası) | **296.880–303.720 KiB ≈ 290–297 MiB** | `ps -o rss` |
| Sanal bellek | 13.461.504 KiB | `ps -o vsz` |
| İmaj | 145,4 MiB | §10.1 |

**v3.1 ile karşılaştırma:** açılış **%5 daha hızlı** (601–772 → 576–588 ms), karşılama kartı
**%14 daha hızlı** (1.881–2.228 → 1.616–1.668 ms), RSS **~23 MiB daha yüksek** (274 → ~297 MiB).
Açılış düzelmesi iki W0 commit'iyle açıklanabilir; RSS artışı bu build'in ölçüm farkıdır ve
**tek başına bir regresyon kanıtı değildir** (farklı JVM bayrakları, farklı font önbelleği).
Yine de not: 350 testlik bir metin editörü için ~300 MiB taban bellek yüksektir; asgari bir
bütçe (ör. boş belgede < 200 MiB) P2'de **tanımlanmalı**, ölçülmeli.

Her açılışta stderr'e şu uyarı düşüyor:

```
WARNING: Unsupported JavaFX configuration: classes were loaded from 'unnamed module @...'
```

Bu, classpath ile başlatmanın **bilinçli** sonucu (`JTypeWriterApp` javadoc'u; JDK launcher'ı
mutlu tutmak için `Application` alt sınıfı değil). Ama kullanıcıya/terminale sızan bir
"polish" sinyali ve jlink'e geçildiğinde **kendiliğinden kaybolacak** bir gürültüdür.

### 8.2 Açma, kodlama, kaydetme — ölçüldü

`TextCodec.decode` / `TextCodec.encode` / `AtomicFiles.writeMoving` doğrudan çağrılarak
(target/classes üzerinde, harness §15.3) ölçüldü. Kaydetme **fsync dahildir**
(`channel.force(true)` + dizin fsync).

| Belge | Karakter | decode | encode | kaydet (soğuk) | kaydet (sıcak) |
|---:|---:|---:|---:|---:|---:|
| 0 MB | 0 | 13,5 ms* | 0,1 ms | 1,1 ms | 0,2 ms |
| 1 MB | 1.048.582 | 14,1 ms | 13,0 ms | 1,4 ms | 0,6 ms |
| 2 MB | 2.097.164 | 5,6 ms | 13,1 ms | 1,6 ms | 0,8 ms |
| 10 MB | 10.485.820 | 23,9 ms | 36,0 ms | **7,1 ms** | **3,9 ms** |

\* 0 MB satırındaki decode süresi JIT ısınmasıdır; ölçüm sırası gereği ilk çağrıdır.

**Üç sonuç:**

1. **Yerel NVMe'de kaydetme hızlıdır.** 10 MB'lık bir belge 7,1 ms'de yazılıyor — fsync dahil.
   v3.1'in "1 MB'da hissedilir duraklama, 10 MB'da donma" iddiası **yerel diskte yanlıştır** ve
   bu belgede **GEÇERSİZ** sayılır. Kullanıcı bir kare bile görmez.
2. **Asıl maliyet kodlamadır, yazma değil.** 10 MB'da encode 36 ms, yazma 7 ms: oran **5:1**.
   Yani "G/Ç'yi thread'den çıkar" işi, ölçülen yükün yalnız **%16'sını** taşır. Geri kalanı CPU
   işidir ve thread değiştirmek onu hızlandırmaz — azaltmak gerekir (§8.4).
3. **Ağ dosya sistemleri hâlâ açık risktir.** Ölçüm yerel NVMe'de; SMB/NFS'te `channel.force`
   saniyeler sürebilir ve o zaman FX thread'i gerçekten donar. Bu senaryo **ölçülmedi** ve
   ölçülmeden "donma yok" denmemelidir.

### 8.3 Tekrar eden maliyet — gerçek darboğaz burada (X5)

Ürünün saniyede bir çalışan işi `StatsBar` yenilemesidir (`StatsBar.java:105-112`,
`new KeyFrame(Duration.seconds(1), …)`). `StatsBarModel.refresh` altı getter çağırıyor
(`StatsBarModel.java:86-99`), bunların dördü tam metin taraması:

| Metot | Karmaşıklık | Ne yapıyor |
|---|---|---|
| `SessionStats.getWordCount` | O(n) | `currentText.trim().split("\\s+").length` — tam dizi tahsisi |
| `getCharCountNoSpaces` | O(n) | `replace(" ","").replace("\n","").replace("\r","")` — **üç tam kopya** |
| `getLineCount` | O(n) | `split("\n", -1).length` — tam dizi tahsisi |
| `getCharCount` | O(1) | `currentText.length()` |
| `getWpm`, `getSessionDuration` | O(1) | sayaçlar |

Ölçülen bir yenileme maliyeti (dört O(n) getter, tek seferlik):

| Belge | 1 yenileme | 10 yenileme | Saniyede FX thread payı |
|---:|---:|---:|---:|
| 0 MB | 0,0 ms | 0,0 ms | %0 |
| 1 MB | 58,4 ms | 166,2 ms | **%5,8** |
| 2 MB | 27,2 ms | 234,8 ms | %2,7 |
| 10 MB | **163,7 ms** | 1.727,7 ms | **%16,4** |

(1 MB satırındaki ilk ölçüm JIT ısınması içerir; 10 tekrarın ortalaması gerçek eğimi verir:
1 MB ≈ 16,6 ms, 2 MB ≈ 23,5 ms, 10 MB ≈ 172,8 ms — yaklaşık doğrusal, O(n) doğrulandı.)

**Karşılaştırma — 30 saniyelik pencere:**

| İş | Sıklık | 10 MB'da 30 s'lik toplam |
|---|---:|---:|
| İstatistik yenilemesi | 1 Hz | **~4.910 ms** |
| Otomatik kayıt (encode + yazma) | 1/30 Hz | ~43 ms (encode 36 + yazma 7) |
| Kullanıcı kaydetmesi | elle | ~43 ms |

**Sonuç:** Tekrar eden FX-thread yükünün **%99'u istatistik yenilemesidir.** 10 MB'lık bir
taslakta yazarken saniyenin altıda biri sürekli olarak tam metin taramasına gidiyor; bu, tuş
gecikmesini değil **kare atlamasını** üretir — yani "kesintisiz yazma" hissini bozan şey
otomatik kayıt değil, **sayaçtır**. Bu, v3.1'in X2/X3 bulgusunu doğrular ama **önceliğini
tersine çevirir**: sıra "önce otomatik kayıt" değil, "önce sayaç".

Düzeltmenin maliyeti de ölçülmüş: artımlı sayaç (kelime/satır sayısını yalnız değişen bölgede
güncellemek) O(n) taramayı tamamen kaldırır; iş, `SessionStats`'ın API'sini koruyup içini
değiştirmektir — tek sınıf, 112 satır, JavaFX'siz, testli (`SessionStatsTest`, 12 test).

### 8.4 Bellek ve disk çarpanı

| Yol | Kopya sayısı (10 MB belge) |
|---|---|
| Yükleme | `byte[]` → `String` → `TextArea` metni: **≥3** |
| Kaydetme | `String` → `CharBuffer` → `byte[]` → temp: **≥3** |
| Otomatik kayıt | ek olarak `getText()` kopyası + `byte[]`: **+2** |
| Kurtarma taraması | her aday için tam `readAllBytes` (`RecoveryStore.java:271`): **N × belge** |
| Kalıcı disk | belge + 3 yedek = **4,00×** (ölçüldü, §6.2/D14); kaydetme anında tepe ≈**6×** |

Belge boyutunun 3–6 katı geçici bellek ve disk kullanımı normaldir; boyut sınırı, uyarı veya
ilerleme göstergesi yok.

### 8.5 Otomatik kayıt — ölçülen ve ölçülmeyen

`AutoSaveService.java:136`:

```java
new Timeline(new KeyFrame(Duration.seconds(AUTOSAVE_SECONDS), e -> snapshot(editorArea.getText())))
```

Argüman `snapshot` gövdesinden **önce** değerlendirildiği için, kirli kontrolü
(`:308 if (fileService.hasUnsavedChanges())`) çalışmadan **temiz belgede bile tam metin kopyası**
alınıyor; odak kaybında da aynı (`:140-144`). `getText()` maliyeti toolkit gerektirdiği için
**ölçülmedi**; ama tam bir `String` kopyası olduğu ve encode'un 10 MB'da 36 ms tuttuğu
düşünülürse, bu yolun tamamı (kopya + encode + yazma) ~40–60 ms mertebesindedir ve 30 saniyede
bir çalışır. Yani **kabul edilebilir** — yine de kirli kontrolünden önce alınması gereksiz bir
kopyadır ve düzeltmesi tek satırdır.

### 8.6 Ölçülmemiş senaryolar (P2'de iddia kurulacaksa önce ölçülmeli)

| Senaryo | Durum | Neden önemli |
|---|---|---|
| 50 MB belge: açma, tuş gecikmesi, undo belleği, OOM | **Ölçülmedi** | `readAllBytes` + sınırsız undo (M7) burada çöker |
| Ağ dosya sistemi (SMB/NFS) kaydetme | **Ölçülmedi** | §8.2'nin "hızlı" sonucu yalnız yerel disk için geçerli |
| 1.000 taslak dosyalı kurtarma dizini | **Ölçülmedi** | D4 bugün açılışı kilitler |
| `getText()` maliyeti (toolkit gerekir) | **Ölçülmedi** | §8.5'in tek eksik terimi |
| Windows açılış ve RSS | **Ölçülmedi** | `jpackage` yalnız host platformu için paket yazar; macOS kapsam dışı (§2.4) |

**Kural:** P2'de "hızlı/donmaz" iddiası kurulacaksa, önce bu beş satırın en az üçü ölçülmeli.
Bugünkü tek sağlam veri: 10 MB'a kadar yerel diskte açma ~24 ms, kaydetme ~7 ms, istatistik
~164 ms/s.

---

## 9. UX, Erişilebilirlik ve Yerelleştirme


### 9.1 Diyaloglar

- **Tema aktarımı merkezî ve doğru:** `DialogStyling` her `Alert`'a uygulanıyor (PW-08'in
  kazancı). Koyu/sepya temada beyaz kutu kalmıyor.
- **Boyut kelepçesi yalnız iki özel sahnede:** `WelcomeDialog.java:108-109`,
  `AboutDialog.java:138-139` (`DialogSizing.clampWidth/clampHeight`), `setResizable(true)`
  `:121`/`:151`.
- **9 `Alert`'ın hiçbirinde** `setResizable`, `setWrapText` veya yükseklik kelepçesi yok:

| Çağrı | Ne var | Ne yok |
|---|---|---|
| `DialogService.java:96` (üzerine yaz) | title/header/content/owner | sarma, kelepçe, resize |
| `:133` (kaydedilmemiş değişiklik) | aynı | aynı |
| `:164` (kodlama yükseltme) | aynı | aynı |
| `:187` (bozuk kodlama) | aynı | aynı |
| `:202` (kodlama) | aynı | aynı |
| `:230` (kurtarma) | aynı | aynı |
| `:263` (hata) | aynı | aynı |
| `CrashHandler.java:68` (çökme) | + tema | aynı |
| `SingleInstanceWarning.java:28` (ikinci örnek) | + tema | aynı |

**U1 hâlâ açık.** Almanca `encodingUpgradeContent` gibi uzun metinler 768 px ekranı aşabilir;
`Alert` varsayılan olarak sarmaz ve yeniden boyutlandırılamaz. Ölçülmedi (toolkit gerekir) ama
kod garanti vermiyor.

### 9.2 Kontrast kapsamı — ölçülen

`ContrastRatioTest` gerçek CSS dosyalarını ayrıştırıp şunları **zorluyor** (eşikler 4,5:1 metin,
3:1 bileşen):

`-stats-text` · `-stats-save-text` · `-stats-warning-text` (hepsi `-toolbar-bg`'ye karşı) ·
`-focus-ring` (`-toolbar-bg`, `-editor-bg`, `-toolbar-hover`'a karşı, 3:1) · `-focus-ring-text`
(`-focus-ring`'e karşı) · `-welcome-button-text` (dinlenme + hover) · `-welcome-button-bg`
(e-posta bağlantısı, `-welcome-bg`'ye karşı) · `-welcome-muted` (tagline) · **5 vurgu rengi × 3
tema** · yapısal: `.stats-*` ≥13 px, popup hücresi = kapalı combo boyutu, yasak özellik yok,
odak rengi saydam değil, koyu palet hex'leri sabitlenmiş.

**Ölçülmeyenler (U3):**

| Seçici / token | Neden önemli |
|---|---|
| `-toolbar-text` (`base.css:108,138,191,237`) | Düğme, combo ve popup satır metni — **arayüzün en çok okunan metni** |
| düz popup satırları | Combo açıldığında okunan metin |
| `.about-email:hover` (`base.css:486-488`) | Bağlantının hover hâli |
| varsayılan editör metni (`light.css:45`, `dark.css:45`, `sepia.css:44`) | `TextColor.DEFAULT` bilinçli atlanıyor (`ContrastRatioTest.java:250-251`) — **belgenin kendisi** ölçülmüyor |
| **`-toolbar-text-muted`** | Üç temada da **tanımlı** (`light/dark/sepia.css:10`) ve **hiçbir kuralda kullanılmıyor** — ölü token, bu yüzden test de görmüyor |

**Yorum:** Test, PW-44 ile genişletildi ve gerçekten iyi bir kapsama ulaştı; ama tam olarak
**kullanıcının en çok baktığı iki yüzey** (araç çubuğu metni ve editör metni) kapsam dışı.
Bu, "kontrast kapısı var" iddiasını koşullu yapar; P2'de kapatılması ucuz (aynı testin
token listesine iki giriş).

### 9.3 Klavye ve erişilebilirlik

- **Erişilebilir ad: yalnız 6 combo.** `setAccessibleText` 6 kez (`ToolbarView.java:399-404`),
  `setAccessibleRole(COMBO_BOX)` 1 kez (`:539`). Editör, durum çubuğu etiketleri, 5 araç çubuğu
  düğmesi, karşılama/Hakkında düğmeleri ve Hakkında bağlantısı **adsız** (U6). Düğmelerin
  görünür metni var, ama ekran okuyucu için rol/ad eşlemesi yok.
- **Yazı boyutu — düzeltilmiş bulgu.** v3.1 "yazı boyutu yalnız fare çift tıklamasıyla değişiyor"
  diyordu. **Doğrusu:** editör yazı boyutunun **klavye yolu var** —
  `KeyboardShortcutHandler.java:101-103` (`Ctrl+0`, `Ctrl++`, `Ctrl+-`) ve combo
  (`ToolbarView.java:174-181`). Yalnız **durum çubuğunun** boyutu (SMALL/MEDIUM/LARGE) farenin
  çift tıklamasına bağlı (`StatsBar.java:98-103`). Yani **U2 daraltıldı**: etki 3 → 1.
- Odak aktarımı saf politika olarak var ve testli; gerçek Tab sırası doğrulanmıyor.
- `viewOrder` ile görsel sıra sabitlenmiş; JavaFX bunu odak için garanti etmiyor.
- Mnemonic yok; tüm erişim Tab ve kısayollara bağlı.
- **Pencere minimum genişliği yok.** `src/main`'de `setMinWidth/setMinHeight` hiç geçmiyor;
  `MainStage.java:53-54` yalnız kayıtlı genişlik/yükseklik veriyor. 900 px altında araç çubuğu
  kırpılıyor (U8) — README bunu dürüstçe yazıyor.

### 9.4 Platform kısaltma gösterimi ve yerelleştirme borcu

| Sorun | Kanıt | Etki |
|---|---|---|
| Tooltip'ler sabit `Ctrl` yazıyor | `ToolbarView.java:84-91` (`"Ctrl+O"`…) | Desteklenen iki platformda da doğru; **U4 macOS kaldırılmasıyla etkisizleşti** (§2.4) |
| Sabit `dd.MM.yyyy` / `HH:mm:ss` | `StatsBarModel.java:51-52` | FR/EN kullanıcısı kendi biçimini görmez — **U5** |
| Sabit `HH:mm` kaydetme etiketi | `MainController.java:33` | aynı |
| `": "` ve `" - "` ayırıcıları kodda | `ToolbarFit.java:93`, `MainController.java:184`, `DialogService.java:189,204` | düşük (RTL dillerinde orta) — **U12** |
| Çökme metinleri İngilizce | `CrashHandler.java:43` (`"Uncaught exception in "`), `FxApplication.java:77` (`"Startup failed"`), ham `Throwable.toString()` `:71` | İngilizce JVM metni UI diline karışıyor — **U11** |
| Karşılama kartı her açılışta, opt-out yok | `FxApplication.java:70` | Günlük kullanıcı için sürtünme — **U7** |

**İyi olan:** 6 bundle × 90 anahtar, **iki yönlü parite testli**; her anahtar `Messages.java`'dan
erişilebilir (0 ölü, 0 sarkan); sayı biçimi UI dilini kullanıyor; TR/DE locale koşuları CI'da.
Bu, P1'in gerçek kazancı ve P2'de korunması gerekiyor.

**Yeni anahtar maliyeti:** 90 anahtar × 6 dil = 540 çeviri satırı; her yeni özellik (arama, menü,
yazdırma, son dosyalar) ortalama 10–20 anahtar getirecek → 60–120 yeni çeviri. Bu, P2'nin
**gizli efor kalemidir** ve yol haritasında görünmez (bkz. §13.4).

### 9.5 Ses politikası

- Ses her **modifier olmayan** `KEY_PRESSED`'da çalıyor (`EditorKeyHandler.java:31-34`): ok
  tuşları, Home/End, F tuşları, Caps Lock ve **`Ctrl+S` gibi akorlar** da tıkırdıyor.
- Sayaç yalnız **`KEY_TYPED` ve karakter var** olduğunda artıyor (`:37-40`).
- Yani **ses ile sayaç ayrı olaylara bağlı** ve çelişebiliyor (U9). Kullanıcı "1000 tuş yazdım"
  sanırken sayaç daha az gösterir; ya da gezinti tuşlarına tıklama sesi gelir.
- Ses seviyesi sabit `0.5` (`TypewriterSound.java:45-46`), private `final`, setter yok; yalnız
  aç/kapat var.
- Hızlı yazımda `stop()+play()` klibi her tuşta yeniden başlatıyor → takılma hissi.
- Ses hatası sessizce WARN'a düşüyor; kullanıcı sese ne olduğunu bilmiyor.

Bu, ürünün **adında olan** özellik (`JTypeWriter`) ve politika hataları doğrudan fark edilir;
U9'un etkisi 2 değil **3** olarak okunmalı.

### 9.6 macOS — kapsam dışı (karar kaydı)

macOS **desteklenen platform değildir** (§2.4). Bu bölüm, kaldırılan bir hedefin bıraktığı
teknik borcu kaydetmek için duruyor:

- Kodda `QuitHandler`, `SystemMenuBar`, Dock entegrasyonu yok; AWT `Desktop` yalnız e-posta
  için (`AboutDialog.java:80`).
- Kapatma yalnız `stage.setOnCloseRequest` + `Application.stop()`
  (`MainStage.java:83-89`, `FxApplication.java:107-113`). **macOS `Cmd+Q` davranışı kodla
  garanti edilmiyordu** ve testi yoktu — bu, P1'in "hiçbir çıkış taslağı silmez" garantisinin
  macOS ayağının kanıtsız kalması demekti (**U10**).

Kaldırma bu borcu **çözmedi, sildi**: artık doğrulanacak bir macOS çıkış yolu yok. Aynı şey
macOS'a özgü diğer iddialar için de geçerlidir (Dock, `~/.jtypewriter` yerine
`Application Support`, `.app` paket yerleşimi). Bir gün macOS yeniden gündeme gelirse bu
bölüm, o fazın başlangıç borç listesidir — ve §10.6b'deki kapı analizi hâlâ geçerlidir.

---

## 10. Paketleme, Dağıtım ve Destek


### 10.1 Artefakt envanteri (taze build, Linux x64, 2 Kasım 2026)

> **Sayılar neden 1 Kasım'a göre biraz küçük?** macOS kaldırılırken `jtypewriter.icns`
> (221.892 B ham) depodan çıkarıldı, yani fat JAR'dan da düştü: imaj 152.660.043 →
> **152.465.247 B**, JAR 11.204.618 → **11.009.822 B** (her ikisinde −194.796 B), `.deb`
> 49.686.050 → **49.488.700 B**, jar girişi 6.106 → **6.105**. Fark tam olarak tek bir ikon
> dosyasının JAR içindeki sıkıştırılmış katkısıdır; runtime boyutu (§10.2) değişmedi.

| Artefakt | Boyut | Not |
|---|---:|---|
| `JTypeWriter/` app image | **152.465.247 B = 145,4 MiB** | 125 dosya |
| ├ `lib/runtime` | **139.967.672 B = 133,5 MiB = %91,7** | **52 modül** |
| └ `lib/app` (tek fat JAR) | **11.009.822 B = 10,5 MiB** | 6.105 giriş, 91 uygulama sınıfı |
| `jtypewriter_1.0.0_amd64.deb` | **49.488.700 B = 47,2 MiB** | `Installed-Size` 149.140 KiB, **12** bağımlılık |
| JAR açılmış toplam | 23.174.417 B | sıkıştırma oranı ≈2,1× |

`dpkg-deb -f`: `Depends: libasound2t64, libc6, libx11-6, libxau6, libxcb1, libxdmcp6, libxext6,
libxi6, libxrender1, libxtst6, xdg-utils, zlib1g`.

İmajda `bin/java` yok (jpackage başlatıcısı) — doğru. Ama `lib/runtime/lib/modules` tek başına
**90.567.712 B (86,4 MiB)** ve taşınan modül listesi uygulamanın kullanmadıklarını içeriyor:

```
jdk.compiler  jdk.javadoc  jdk.jshell  jdk.jfr  jdk.jconsole  jdk.attach
jdk.jdi  jdk.jdwp.agent  jdk.internal.ed  jdk.internal.le  jdk.httpserver
jdk.incubator.vector  jdk.sctp  jdk.accessibility  ...
```

Bir metin editörü için gereken modül kümesi `java.base`, `java.desktop`, `java.logging`,
`java.xml` (JavaFX CSS/XML), `java.prefs`, `java.datatransfer`, `java.scripting`,
`jdk.unsupported` mertebesindedir.

> **Kapsam notu:** Windows artefaktı bu makinede üretilemez — `jpackage` yalnız kendi
> platformu için paket yazar (`AGENTS.md`). Bu platformun boyutu CI matrisinin (2 OS) çıktısına
> bağlıdır; bu belgede **yerel build yoktur** ve o boyut "çıkarıldı" seviyesindedir. macOS
> kapsam dışıdır (§2.4).

### 10.2 jlink — bu kez tahmin değil, ölçüm

v3.1 "145 → ~50 MiB" diyordu (çıkarım). Bu makinede gerekli 8 modülle denendi:

```bash
jlink --add-modules java.base,java.desktop,java.logging,java.xml,java.prefs,
      java.datatransfer,java.scripting,jdk.unsupported \
      --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output rt
```

Sonuç: **62.248.693 B ≈ 59 MiB** (JDK 25.0.2'nin kendi `jmods`'undan).

| Senaryo | Runtime | İmaj | Kazanç |
|---|---:|---:|---:|
| Bugün | 133,5 MiB | 145,4 MiB | — |
| jlink (8 modül, ölçüldü) | ~59 MiB | **~71 MiB** | **~74 MiB / %51** |

JavaFX kendi modüllerini/jar'larını getirdiği için ek modül gerekmiyor. **Sonuç:** jlink,
ürünün indirme boyutunu **yarıya** indirir ve bu, ölçülmüş bir gerçektir; "P3'e ertelendi"
kararı artık bir *ölçüm* değil bir *öncelik* kararıdır. Teknik engel yok; tek engel
`UndoHistoryReset`'in yansıma tabanlı undo temizliğinin modül yolunda sessizce çalışmaması
(M6) — yani jlink, M6'yı **önkoşul** yapar.

### 10.3 Ölü ağırlık — JAR içinde

Fat JAR 11,2 MiB; içinde:

| Bileşen | Boyut | Kullanım |
|---|---:|---|
| JavaFX Media sınıfları (75 giriş) | 173.704 B | yalnız **71.204 B'lik** `keystroke.wav` için |
| Media native: `libjfxmedia`, `libgstreamer-lite`, 9× `libavplugin*` | **3.353.648 B** | aynı |
| JavaFX FXML sınıfları (79 giriş) | 238.188 B | `fxml/main.fxml` **boş bir `BorderPane`** (280 B) |

Toplam **≈3,77 MiB = JAR'ın %34'ü**, iki bağımlılık (`javafx-media`, `javafx-fxml`, `pom.xml:73-82`)
yüzünden taşınıyor. Ses `javax.sound.sampled`'a, FXML yükleme düz Java'ya çevrilirse JAR ~7,4 MiB'e
iner ve `THIRD-PARTY-NOTICES`'tan GStreamer/KISS FFT/FFmpeg-plugin bölümleri **çıkabilir**
(lisans yükü azalır).

### 10.4 CI kapıları ve eksik smoke test

| Kapı | Var? | Not |
|---|---|---|
| Enforcer (JDK 25, Maven 3.8) | ✅ | `pom.xml:114-131` |
| 350 test × 2 OS | ✅ | `maven.yml:48` |
| TR/DE locale koşusu (3× suite) | ✅ | `maven.yml:66,68` |
| Lisans/telif bildirimleri | ✅ | `PackagingNoticesTest` + `package.sh` |
| Sürüm/tag eşleşmesi | ✅ | `release.yml:55` |
| Retention ≤ 7 gün | ✅ | `ReleaseNotesTest` |
| **Artefaktı çalıştırma** | ❌ **B1** | CI'da imaj açılmıyor, `.deb` kurulmuyor |
| **`verify-artifacts.sh`** | ❌ **B14** | Yerelde 17/17 geçiyor; hiçbir workflow çağırmıyor |
| **İkon `--check`** | ❌ **B15** | `scripts/generate-icons.py --check` CI'da değil (PW-41 kapandı sayılmış, ama sadece W0'da *belgelendi*) |
| Kod imzalama (Windows Authenticode) | ❌ B3 | Yok; macOS notarization'ı kapsamla birlikte düştü (§2.4) |
| Kapsam ölçümü (JaCoCo) | ❌ | Bilinçli (§11.3) |
| SBOM / zafiyet taraması | ❌ | — |
| Maven bağımlılık güncellemesi | ❌ | Dependabot yalnız `github-actions` |

**B14 neden ayrı bir bulgu:** W0.2'nin tüm değeri, "üç kopya bayt bayt aynı" garantisinin **her
PR'da** koşmasındadır. Yerelde elle çalışan bir betik, bir sonraki `package.sh` değişikliğinde
sessizce çürür ve o gün kimse fark etmez. Betik ilk koşusunda **gerçek bir kusur buldu**
(`.deb` yükü `LICENSE`/`NOTICE`/`THIRD-PARTY-NOTICES`/`legal/` taşımıyordu) — bu, kapının ne
kadar gerekli olduğunun kanıtı.

### 10.5 Sürüm, imza, güncelleme

- `pom.xml` tek sürüm kaynağı (**1.0.0**); tag `v<version>` olmalı; `package.sh:81-85` jpackage
  kurallarına uymayan sürümü reddediyor (`^[1-9][0-9]*(\.[0-9]+){0,2}$`); `release.yml:55` ayrıca
  `^[0-9]+\.[0-9]+\.[0-9]+$` istiyor. Doğru tasarım.
- **Hiç tag atılmamış, hiç Release yok.** Release hattı uçtan uca hiç çalışmadı.
- Release hattı kendi içinde tutarlı ve dikkatli: `prepare` tag↔pom doğrulaması, 2 OS build,
  `release-assets.sh` ile normalize adlar, `checksums.sh` ile `SHA256SUMS`, **5 varlık + tek
  `SHA256SUMS`** assert'i, iki dilli şablon, `--verify-tag`. Yani hat **iyi tasarlanmış, hiç
  koşmamış**. (macOS kaldırılmadan önce 3 OS ve 7 varlık bekleniyordu; §2.4.)
- İmza yok; `docs/release-notes-template.md` bunu dürüstçe yazıyor. Windows SmartScreen ilk
  açılışta uyaracak. macOS Gatekeeper uyarısı, platformla birlikte belgeden çıktı.
- Güncelleme denetimi yok (gizlilik duruşuyla tutarlı).
- Yeniden üretilebilirlik: `outputTimestamp` var (`pom.xml:61`), `verify-artifacts.sh` üç kopyayı
  karşılaştırıyor; ama çift build'in bayt-özdeşliği (jpackage mtime normalizasyonu) **doğrulanmadı**.

### 10.6 Masaüstü entegrasyonu

`.deb` içindeki `.desktop` (`/opt/jtypewriter/lib/jtypewriter-JTypeWriter.desktop`):

```
Categories=Unknown
MimeType=
Icon=/opt/jtypewriter/lib/JTypeWriter.png
Exec=/opt/jtypewriter/bin/JTypeWriter
Type=Application
Terminal=false
```

`--linux-app-category "Office"` (`package.sh:390`) yalnız `.deb` `Section:` alanına gidiyor,
`.desktop`'a değil. hicolor ikon seti, MIME XML, dosya ilişkilendirmesi yok. Yani **ürün
"uygulamalar" menüsünde "Diğer" altında, aranabilir bir tür olmadan** duruyor. Bu, E10'un
kullanıcıya görünen yüzü.

Ayrıca `AppDirs` tüm OS'lerde `~/.jtypewriter` kullanıyor (`AppDirs.java:20`); XDG değil (B7).
macOS `Application Support` dalı, platformla birlikte tartışmadan çıktı (§2.4); geriye yalnız
Linux'ta `XDG_CONFIG_HOME`/`XDG_DATA_HOME` sorusu kaldı.

### 10.6b Kanal ekseni — "kurulan" mı, "çalıştırılan" mı?

`release-assets.sh` bugün her platform için **iki** kanal üretiyor: installer (`.deb`/`.msi`)
ve taşınabilir `.tar.gz`. Bugüne kadarki hiçbir belge bu iki kanalı bir karar ekseni olarak
tartışmadı; hep "hangi OS" sorusu soruldu. Oysa kanal, iki bağımsız sonucu belirliyor.

**Birinci sonuç: OS güven kapısı kanaldan bağımsızdır.** Notarization/SmartScreen, paketleme
biçiminin değil **ikilinin ve indirme kaynağının** özelliğidir:

| Platform | Kapı | Tetikleyici | Taşınabilir kanal çözer mi? |
|---|---|---|---|
| macOS *(kapsam dışı)* | Gatekeeper + `com.apple.quarantine` | Dosya **indirildiği** anda işaretlenir — `.dmg`, `.zip`, `.tar.gz` ayırmaz | **Hayır.** `.app` yine karantinalı açılır; Developer ID imzası + notarization + staple gerekir |
| Windows | SmartScreen + Mark-of-the-Web | İndirilen arşivden çıkan `.exe` MOTW'u miras alır | **Hayır.** Authenticode imza olmadan uyarı çıkar |
| Linux | — | `dpkg`/`tar` imza kapısı sormaz | Zaten yok |

macOS'ta taşınabilir kanal kapıyı kaldırmaz, **kullanıcıyı daha kötü bir diyaloğa** sokar:
`.dmg` sürüklemek yerine arşiv açılır, ardından "uygulama hasarlı, çöp kutusuna taşı" mesajı
gelir ve `xattr -d com.apple.quarantine` gerekir. Notarize edilmiş bir `.app`'te bu adım yoktur.
Ayrıca `notarytool` yalnız `.zip`, `.dmg`, `.pkg` kabul eder — **`.tar.gz` gönderilemez**; yani
taşınabilir kanal notarization'ı atlatmaz, onu `.app` üzerinde yapıp staple'lamayı zorunlu kılar.

**Sonuç:** "notarization problemi" bir teknik engel değil, bir **bütçe** problemidir
($99/yıl + sertifika + CI secret'ları + ~1,5–2 hafta). Taşınabilirlik bu bütçeyi düşürmez.
Düşüren tek karar, macOS'u **hedeflememektir** — ve bu karar **2 Kasım 2026'da verilmiştir**
(§2.4). Aşağıdaki macOS satırları artık bir seçenek değil, kararın gerekçesidir.

**İkinci sonuç: taşınabilirliğin gerçek kazancı başka yerdedir — ve ölçülebilir.**

| Kazanç | Neden gerçek |
|---|---|
| **Installer'a özgü hata sınıfı düşer** | `03c277c`'de düzeltilen kusur tam bu sınıftandı: jpackage'ın `.deb` paketleyicisi kök dizindeki `LICENSE`/`NOTICE`/`THIRD-PARTY-NOTICES`/`legal/` dosyalarını sessizce düşürüyordu. MSI kısayol seçicisi, DMG yerleşimi, `fakeroot`, `Installed-Size` yeniden hesabı, `.desktop` gömme — hepsi kendi hata modunu getiriyor |
| **Sistem değiştirmeme dürüstlüğü** | Registry yok, root yok, kaldırma sorunu yok — tek dosya yazan bir editör için doğru duruş |
| **"Eski sürüm kurulu" destek yükü yok** | — |
| **Doğrulanacak kopya sayısı azalır** | `verify-artifacts.sh`'ın 17 karşılaştırmasının 6'sı `.deb`'e ait (5 yasal dosya + jar) |

**Bedeli de ölçülebilir:**

| Bedel | Etki |
|---|---|
| **Dosya ilişkilendirmesi (E10) imkansızlaşır** | `.desktop`/MIME kaydı olmadığı için "çift tıkla aç" yapısal olarak engellenir — yani P2'nin "OS'ten aç" hedefi taşınabilir kanalda **tek başına** gerçekleşemez |
| **Linux'ta daha büyük** | Ölçülen: `.deb` 47,2 MiB vs `.tar.gz` 59,3 MiB — taşınabilir ~%25 daha büyük (çift sıkıştırma yok) |
| **Keşfedilebilirlik** | Uygulamalar menüsünde görünmez; kullanıcı `bin/JTypeWriter`'ı kendisi bulmalı |
| **Güncelleme tamamen manuel** | Kurulu taban yokken sorun değil; ilk 100 kullanıcıda olur |

**Analitik hüküm:** kanal seçimi, notarization kararını **değiştirmez**; onu iki bağımsız eksene
böler (OS × kanal). Linux'ta iki kanal da bedava ve kapısızdır, bu yüzden ikisini birlikte
yayınlamak maliyet farkı küçük bir kazançtır. Windows'ta taşınabilirlik SmartScreen'i çözmez;
imza çözer — bu bir bütçe kararıdır. macOS'ta taşınabilirlik **en kötü** kanaldı: ya imzala +
notarize et, ya hedefleme — ve ürün ikincisini seçti (§2.4). Taşınabilirliğin gerçek değeri,
W1'i kısaltan installer-bakım kesintisidir — ürünün "kurulmuyor" olması değil.

### 10.7 Destek yüzeyi

| Beklenen | Durum |
|---|---|
| `SECURITY.md` | ❌ |
| `CHANGELOG.md` | ❌ |
| `CONTRIBUTING.md` / `CODE_OF_CONDUCT.md` | ❌ |
| Issue / PR şablonları | ❌ |
| Kurulum/kaldırma/sorun giderme belgesi | ❌ (yalnız README) |
| "Log klasörünü aç" | ❌ (yalnız mailto) |
| Tanılama paketi (log + ayar + sürüm) | ❌ |
| Çökme raporu (yerel) | ✅ log + diyalog |
| Uygulama içi sürüm | ✅ About |

Bir kullanıcı hata bildireceğinde elinde yalnızca bir e-posta adresi ve bir dizin yolu var.
Bu, ilk 100 kullanıcıda destek maliyetini doğrudan artırır — ve ilk 100 kullanıcı P2'nin
hedefi olduğu için bu bir "P3 cilası" değildir.

### 10.8 Lisans ve ticari model

- Lisans **Apache-2.0** (GPLv3'ten geçilmiş); patent grant ve değişiklik özgürlüğü var, copyleft
  yok. Üçüncü taraf bileşenler ayrı lisanslarla taşınıyor ve bu, dağıtımda doğru beyan edilmiş:
  JavaFX `GPL-2.0-only WITH Classpath-exception-2.0`, GStreamer Lite `LGPL-2.1-or-later`,
  KISS FFT `BSD-3-Clause`, GCC runtime `GPL-3.0-or-later WITH GCC-exception-3.1`, IJG JPEG.
- **Ticari model tanımlı değil:** fiyat, web sitesi, EULA, ticari destek, marka politikası yok.
  Apache-2.0, üçüncü bir tarafın ürünü markasız kopyalayıp satmasını engellemez.
- Katkı modeli tanımsız: `CONTRIBUTING.md`, CLA/DCO, kod sahipliği yok. Tek geliştirici
  varsayımı hem yol haritasının hem destek yükünün gerçek sınırı.

P2 bunu çözmek zorunda değil; ama "ilk kullanıcı" kararı verilirken lisans/ticari modelin
**açıkça ertelendiği** bilinmeli — aksi halde dağıtım ve imza yatırımı geri dönüşsüz bir
varsayıma dayanır.

### 10.9 Hijyen borçları

| Borç | Etki |
|---|---|
| `releases/` ve `target/` `.gitignore`'da | Ölçülen hiçbir artefakt sayısı git tarafından korunmuyor (§2.3) |
| `.idea/` çalışma dizininde, izlenmiyor | zararsız ama gürültü |
| `docs/` yalnız bu analiz, yol haritası ve sürüm notu şablonunu içeriyor | analiz/yol haritası belgeleri geçmişte iki kez silindi (`f5d00db`, `d7bf8da`); P2'de bu belgelerin **kalıcı** olması bir kültür kararı |

> **Not:** Bu belgeden önce `docs/analysis.md` ve `docs/product-roadmap.md` sürüm kontrolünden
> çıkarılmıştı (`f5d00db` "the analysis and roadmap are spent") ve ikinci kez çalışma ağacında
> silinmiş durumda. Aynı belgenin üçüncü kez silinmemesi için, bu analiz **bulgu kimlikleri ve
> ölçüm protokolüyle** birlikte yazıldı: silinse bile §15 tek komutla aynı sayıları üretir.

---

## 11. Test ve Kalite Güvencesi

### 11.1 Ölçülen şekil

| Ölçüt | Değer |
|---|---|
| Test dosyası | **42** (40 test sınıfı + 2 yardımcı: `CrashHarness`, `ContrastRatio`) |
| `@Test` | **350** |
| Test satırı | **7.041** (üretim 7.611 → oran **0,93**) |
| Bu makinedeki koşu | **350 koştu / 0 hata / 0 atlandı** (`mvn -B clean test`) |
| Toolkit açan test | **0** (`Platform.startup`, `Application.launch`, Monocle hiç yok) |
| CI | 2 OS × 3 locale (EN/TR/DE) = 6 suite koşusu |
| Kapsam ölçümü | **yok** |

Test sınıfları (grep'lenmiş `@Test` sayılarıyla): `FileServiceTest` 36 · `RecoveryStoreTest` 19 ·
`ContrastRatioTest` 19 · `AppSettingsTest` 17 · `TextCodecTest` 16 · `AutoSaveServiceTest` 15 ·
`AppLoggingTest` 13 · `FontSizeManagerTest` 13 · `MessagesTest` 12 · `SessionStatsTest` 12 ·
`ToolbarFitTest` 12 · `EncodingMatrixTest` 11 · `StatsBarModelTest` 11 · `KeyboardShortcutHandlerTest` 11 ·
`DialogSizingTest` 8 · `BackupKeeperTest` 7 · `ShortcutRowsTest` 7 · `ToolbarOptionListsTest` 7 ·
`MainControllerTest` 7 · `AtomicFilesTest` 6 · `DialogServiceTest` 6 · `AutosaveHealthTest` 6 ·
`PackagingNoticesTest` 6 · `DialogButtonsTest` 6 · `DialogStylingTest` 6 · `AppIconsTest` 6 ·
`MainStageTest` 6 · `AudioAssetTest` 5 · `InstanceLockTest` 5 · `YoloPolicyTest` 5 ·
`StatsBarStateTest` 5 · `AppVersionTest` 4 · `ThemeCycleTest` 4 · `ExitDispositionTest` 4 ·
`ReleaseNotesTest` 4 · `LicenseHeaderTest` 3 · `LanguageTest` 3 · `ToolbarStateTest` 3 ·
`ProcessCrashRecoveryTest` 3 · `JTypeWriterAppTests` 1.

### 11.2 Gerçekten iyi kapsanan alanlar

Kodlama matrisi (11 test, bayt-ödeş), kurtarma yaşam döngüsü (19), dosya servisi (36), çıkış
matrisi, otomatik kayıt sağlığı, yedek rotasyonu, atomik yazma, ayar karantinası, i18n paritesi
ve biçimlendirme, kontrast matematiği (19), araç çubuğu taşma politikası, kısayol çözümleme,
süreç öldürme (gerçek `destroyForcibly`).

**Bu, 7,6 bin satırlık bir masaüstü uygulaması için olağanüstü iyidir** ve ürünün en güvenilir
varlığıdır. P2'nin hiçbir işi bu disiplini gevşetmemelidir.

### 11.3 Toolkit duvarı — mimarinin bilinçli bedeli

Şunlar **hiç test edilmiyor** ve mevcut mimaride edilemez:

- gerçek diyalog yerleşimi/taşması (U1),
- odak ve Tab sırası, karşılama kartının iç odağı,
- render edilmiş CSS (kontrast yalnız hex matematiği),
- ses politikası (hangi tuş ses çıkarır — U9 tam olarak bu yüzden kaçtı),
- ilk karenin görünümü, menü/araç çubuğu etkileşimi,
- `getText()` maliyeti (§8.5).

Bu duvar, "her UI kararını saf sınıfa çıkar" kültürünün bedelidir. P2'de ara/değiştir gibi
özellikler bu yüzden **saf model + ince UI** olarak tasarlanmalı; aksi halde ya testsiz kalır ya
da toolkit kuralı delinir. `AGENTS.md` bu kuralı açıkça yazıyor ve 350 testin tamamı buna uyuyor.

### 11.4 Zayıf veya yanlış güven veren testler

| Test | Sorun |
|---|---|
| `ProcessCrashRecoveryTest` | Yalnız Linux; GUI değil servis grafiği fork ediliyor. Doğru kapsam, ama Windows çökme yolu doğrulanmıyor |
| `DialogSizingTest` | Yalnız iki özel diyaloğun kelepçe matematiği; **9 `Alert` kapsam dışı** |
| `AppSettingsTest` | `SecurityException` ve gerçek `save()` hatası yok (D12) |
| `ToolbarFitTest` | Kabul testleri ölçülen etiketlerle; gerçek render yok |
| `ContrastRatioTest` | Token çiftleri doğru, kapsam §9.2'deki seçicilerle sınırlı |
| `AtomicFilesTest` | 6 test; **symlink, fallback, temp süpürme, `finally` maskeleme yok** (D2, D6) |
| `MessagesTest` | Parite ve erişilebilirlik zorlanıyor; **çeviri kalitesi** ölçülmüyor |

### 11.5 Altyapı boşlukları

- **Kapsam ölçümü yok.** "350 test" hacim verir, *hangi yolların* koştuğunu vermez. Örneğin
  `FileServiceTest` 36 testle en büyük sınıf, ama `writeFile`'ın `Error` yakalamayan hata yolu
  (D7) hiç koşmuyor. **T7: test sayısı yol kapsamı değildir.**
- **Statik analiz/format yok** (bilinçli, `AGENTS.md`). Sözleşme kuralları testle korunuyor —
  ama *kod kokusu* korunmuyor (ör. `AtomicFiles`'taki `finally` maskeleme).
- **Artefakt düzeyi test yok** (B1, B14).
- **Testler gerçek `~/.jtypewriter`'a yazıyor** (M4/T6). Bu makinede doğrulandı:
  `AppLoggingTest` koşusundan sonra `~/.jtypewriter/logs/jtypewriter-*.log` büyüdü.
  `AGENTS.md`'nin "asla gerçek eve dokunma" kuralı **ihlal ediliyor** ve temiz CI'da görünmüyor
  (çünkü orada ev dizini zaten geçici).

### 11.6 Değişmezler (invariant) ve onları koruyan testler

| Değişmez | Test | Ne zorluyor |
|---|---|---|
| SPDX bloğu dosyanın 0. baytında | `LicenseHeaderTest` (3) | Her `src/**/*.java` ve kanonik SVG |
| i18n anahtar paritesi | `MessagesTest.everyBundleTranslatesTheSameKeys` | 6 bundle'ın anahtar kümesi = EN |
| Ölü/sarkan anahtar yok | `MessagesTest.everyKey…` | `Messages.java`'dan taranan küme = bundle kümesi |
| Üçüncü taraf bildirimleri | `PackagingNoticesTest` (6) | `META-INF` kopyası repoyla bayt-ödeş, SPDX kimlikleri, ZWSP/CR yok, ≤100 kolon |
| Sürüm notu iki dilli | `ReleaseNotesTest` (4) | `## English` önce, `## Türkçe` var, `@VERSION@`/`@TAG@`, literal semver yok |
| Retention ≤ 7 | `ReleaseNotesTest` | Her `upload-artifact` adımında `retention-days` 1–7 |
| Diyalog düğmeleri | `DialogButtonsTest` (6) | Her özel düğmede `ButtonData` |
| İkon kaynağı | `AppIconsTest` (6) | PNG/ICO baytları SVG'den üretilmiş |
| Toolkit açılmaz | (kural, test değil) | Hiçbir test `Platform.startup` çağırmıyor |

Bu tablo, "kaliteyi plugin değil test korur" kararının somut karşılığıdır ve P2'de yeni
değişmezler (ör. "her `Alert` `DialogSizing`'den geçer") buraya **test olarak** eklenmelidir.

---

## 12. Olgunluk Skorları

| Boyut | Ağırlık | Skor | Gerekçe | v3.1 |
|---|---:|---:|---|---:|
| **Motor** | 0,25 | **3,6** | Veri yolu tek ilkelden geçiyor; kodlama/çıkış/kurtarma gerçekten testli; kaydetme ölçümle hızlı (10 MB → 7 ms). Ama güvenlik vaadi tek yazar + düz dosya + sağlam FS koşullu (D1/D2/D3/D6) ve symlink kusuru **kanıtlandı** | 3,6 |
| **Deneyim** | 0,25 | **2,2** | Ara/değiştir, menü, yazdırma yok; 14 eylemin 2'si arayüzde görünmüyor; `Alert` taşması ve sabit tarih biçimi 6 dilin 3'ünde görünür kusur. "Sessiz" ama çıplak | 2,2 |
| **Ambalaj** | 0,20 | **2,1** | Artefaktlar üretiliyor, lisanslı, üç kopya bayt-ödeş (`verify-artifacts` 17/17); jlink kazancı **ölçüldü** (~74 MiB). Ama artefakt hiç çalıştırılmadı, hiç yayınlanmadı, imza yok, JAR'ın %34'ü ölü ağırlık | 2,0 |
| **Güven** | 0,15 | **2,9** | SPDX, üçüncü taraf bildirimleri, SHA pinleme, çıkış matrisi, 350 test/0 TODO gerçek altyapı. Ama yayın yok, `SECURITY.md` yok, smoke test yok, imza yok, testler gerçek eve yazıyor | 2,8 |
| **Pazara erişim** | 0,15 | **1,5** | 0 tag / 0 Release / 0 yıldız; `Categories=Unknown`, MIME yok, CLI yok: ürün bulunsa bile OS'ten açılamıyor. macOS'un düşmesi notarization kalemini sildi ama bulunabilirlik hiç değişmedi | 1,5 |
| **Endeks** | 1,00 | **2,53** | `0,25·3,6 + 0,25·2,2 + 0,20·2,1 + 0,15·2,9 + 0,15·1,5` | 2,50 |

### 12.1 Neden "motor ≠ ürün"

Motor skoru, "veri bozulmasın" tasarımının kalitesini ölçüyor ve 3,6 hak ediyor — hatta
ölçümler onu **destekledi** (kaydetme hızlı, kodlama bayt-ödeş, çökme kurtarması gerçek süreç
öldürmeyle testli). Ürün skoru ise kullanıcının **işini bitirip bitirmediğini** ölçüyor:
ara/değiştir olmadan uzun metin, menü olmadan keşfedilebilirlik, yayın olmadan erişim yok.

### 12.2 Skorların sınırı (dürüstlük notu)

Bu skorlar **beş boyutlu, ağırlıklı, tek kişilik bir yargıdır**; istatistik değildir. Ağırlıklar
ürün tezinin sırasına göre seçildi (motor + deneyim = ürünün kendisi; ambalaj = çalıştırabilir
misin; güven + erişim = bulup güvenebilir misin). Farklı bir ağırlık kümesi endeksi ±0,25
oynatabilir. Skorların değeri mutlak seviyede değil, **fazlar arası harekette** ve **hangi
boyutun eksik kaldığını** göstermesindedir. Yol haritası bu yüzden hedefi endeksle değil,
**yanlışlanabilir çıkış kriterleriyle** tanımlamalıdır.

### 12.3 Neden ambalaj ve güven v3.1'den çok az yükseldi

W0 üç kart kapattı (`AGENTS.md` izleniyor, `verify-artifacts.sh`, `.deb` lisans yükü) ve bunlar
gerçek kazanç. Ama ambalajın asıl sorunu — **artefaktın çalıştığının kanıtlanmaması ve hiç
yayınlanmaması** — W1'in işidir ve hiç başlamamıştır. Bir kapıyı kapatan iş, o kapının
*değişmezini* yükseltir; W0 kapıları kapı değil, **kapıların ön koşullarıydı**. Bu ayrım
yol haritasında korunmalı, aksi halde "üç kart bitti, skor artmalı" yanılgısı doğar.

---

## 13. P2 Girdileri — Aday Havuzu ve Kısıtlar

> **Bu bir plan değildir.** Sıra, sahiplik, dalga ve takvim `docs/product-roadmap.md` v2.0'da
> üretilmiştir. Buradaki skor **göreli öncelik göstergesidir**; bağımlılık kısıtları ise
> bağlayıcıdır, çünkü yanlış sıra işi iki kez yazdırır.

### 13.1 Aday havuzu

Formül: `(Etki × Güven) / Efor`; Etki 1–5, Güven 1–3, Efor S=1 / M=2 / L=3 / XL=5.

| ID | İş | Bulgular | Etki | Güven | Efor | Skor |
|---|---|---|---:|---:|---:|---:|
| **A1** | Belge kimliği: dış değişiklik + symlink'e doğru yazma + taslak yolu doğrulama + belge kilidi | D1, D2, D3, D8 | 5 | 2 | L | 3,3 |
| **A2** | Ara / Değiştir (saf model + YOLO etkileşimi + `Ctrl+F`/`F3`/`Ctrl+H`) | E1 | 5 | 3 | L | **5,0** |
| **A3** | Tekrar eden FX-thread yükünü kaldır: **artımlı istatistik sayaçları** + G/Ç'yi executor'a taşı | X5, X1, X2, X3, X4 | 4 | 3 | M | **6,0** |
| **A4** | Komut katmanı + menü çubuğu + CLI/süreç argümanı + sürükle-bırak | E2, E3, E4, M1 | 4 | 2 | M | 4,0 |
| **A5** | Diyalog taşması (9 `Alert`) + tarih/saat ve ayırıcı yerelleştirmesi | U1, U5, U12 | 3 | 3 | M | 4,5 |
| **A6** | İlk gerçek sürüm + CI artefakt smoke testi | B1, B2, B14 | 5 | 3 | M | **7,5** |
| **A7** | Yazar ergonomisi: son dosyalar, "bir daha gösterme", min pencere genişliği, kısayol listesinin dürüstlüğü | E5, U7, U8, U13 | 3 | 3 | M | 4,5 |
| **A8** | Taslak yaşam döngüsü: symlink takibi yok, boyut/sayı sınırı, `parked-*` temizliği, disk çarpanı, hata maskeleme | D4, D5, D6, D7, D9, D10, D11, D14 | 3 | 3 | M | 4,5 |
| **A9** | Test hijyeni: `AppLogging` enjeksiyonu, gerçek eve yazmayı bitir | M4, T6 | 2 | 3 | M | 3,0 |
| **A10** | JAR temizliği: sesi `javax.sound`'a taşı, JavaFX Media + FXML'i çıkar | B4 | 2 | 3 | S | 6,0 |
| **A12** | Yazdırma / PDF | E7 | 3 | 2 | M | 3,0 |
| **A13** | jlink ile runtime budama (ölçüldü: −74 MiB) — **M6'yı önkoşul yapar** | B4, M6 | 3 | 2 | M | 3,0 |
| **A14** | OS entegrasyonu: `.desktop` kategorisi/MIME, dosya ilişkilendirmesi, hicolor ikon | E10, B8 | 3 | 2 | M | 3,0 |
| **A15** | Destek yüzeyi: `SECURITY.md`, `CHANGELOG.md`, issue/PR şablonu, tanılama paketi | B9 | 2 | 3 | M | 3,0 |
| **A16** | Kontrast kapsamını kapat: `-toolbar-text`, popup satırları, editör metni, ölü token | U3 | 2 | 3 | S | 6,0 |
| **A17** | Ses politikası: ses ve sayaç aynı olaya bağlansın, seviye ayarı, kesme yerine yeniden başlatma | U9 | 3 | 2 | S | 6,0 |

### 13.2 Bağlayıcı kısıtlar (bunlar tercih değil)

| # | Kısıt | Neden |
|---|---|---|
| K1 | **A2, A4'ün komut katmanından sonra** | Aksi halde arama iki kez kablolanır (M1) |
| K2 | **A2, A3'ün sayaç işinden sonra** | Arama sonuçları da tam metin taramasıdır; naif döngü 10 MB'da UI'yı kilitler (§7.5) |
| K3 | **A13 (jlink), M6'nın (yansımalı undo) çözümünden sonra** | Modül yolunda `UndoHistoryReset` sessizce yetenek kaybeder |
| K4 | **A1, A3 ile aynı dosyalara dokunur** (`FileService` sözleşmesi): biri önce bitmeli, paralel yazılmamalı | İki yazar aynı sözleşmeyi değiştirirse çakışma |
| K5 | **A6 diğer her şeyden önce** | Kanıt zemini; yayınlanmamış üründe diğer her iş tahmindir (§4.4) |
| K6 | **Her yeni ayar (A7) `D13`'ü (ayar şema sürümü) aynı PR'da tetikler** | Yoksa göç borcu sessizce büyür |
| K7 | **Her yeni görünür metin 6 bundle + `Messages` erişimcisi** | `MessagesTest` aksi halde kırmızı |

### 13.3 P2 dışı bırakılması önerilenler

| Öneri | Gerekçe |
|---|---|
| **Sekmeler / çoklu belge** | Tek belge romancı için tutarlı bir kimlik; P1'in stabilize ettiği çıkış/kirli/taslak matrisini ikiye katlar. `DocumentSession` refactor'u ayrı bir faz |
| **Yazım denetimi** | Sözlük lisansı + büyük UI yüzeyi (XL); farklılaştırıcı değil; kanıtsız talep |
| **Kod imzalama / notarization** | macOS kapsam dışı olduğu için **notarization kalemi tamamen düştü** (§2.4). Geriye yalnız Windows Authenticode kalır: SmartScreen uyarısını kabul etmek meşru bir P2 tercihidir, çünkü kapı **engellemez**, uyarır |
| **Otomatik güncelleme / telemetri** | Yayınlanmış kurulu taban yokken anlamsız; gizlilik duruşuyla çelişir |
| **Satır numaraları (E8), sarma anahtarı (E9)** | A4'ün komut katmanından sonra ucuzlar; bölüm bitirmeyi engellemiyor |
| **Dışa aktarma (HTML/DOCX)** | A12 (yazdırma) ile aynı çıktı hattını paylaşır; sonraki faz |
| **Kapsam ölçümü (JaCoCo) kapısı** | Bu projede suite kapı; yüzde metrik gürültü. **Ama** `T7` için tek seferlik bir ölçüm yararlı olur |
| **XDG taşıması (B7)** | macOS dalı düştü (§2.4); geriye yalnız `XDG_CONFIG_HOME`/`XDG_DATA_HOME` kalıyor ve kurulu taban olmadan riskli |
| **ES/IT register temizliği, tipografik apostrof** | Düşük etki; A5'in yanında bedava gelirse alınır |

### 13.4 Yol haritasında görünmeyen maliyetler

P2 planlanırken **efor tahminlerine eklenmesi gereken** ama hiçbir bulgu satırında durmayan
kalemler:

| Kalem | Neden |
|---|---|
| **Çeviri yükü** | A2 + A4 + A5 + A7 ortalama 15–20 anahtar/dil → **~90–120 yeni çeviri satırı** ve 6 dilde kalite kontrolü |
| **Belge yükü** | README + README-TR birlikte (her kullanıcıya görünen değişiklikte), sürüm notu şablonu, `AGENTS.md` |
| **Test yükü** | Her bulgu için onu kırmızıya çeviren test (§6.4 dersi); A1 tek başına 4 senaryo |
| **Artefakt doğrulama** | Her bağımlılık değişiminde `THIRD-PARTY-NOTICES` + `verify-artifacts` + taze build |
| **İki OS doğrulaması** | Yerelde yalnız Linux; Windows yalnız CI'da görülür — geri bildirim döngüsü saatler. macOS'un düşmesi bu kalemi 3 satırdan 2'ye indirdi (§2.4) |

### 13.5 Yol haritasından önce insanın vermesi gereken karar — OS ekseni kapandı, kanal ekseni açık

> **İlk benimseyen kullanıcı kim — ve hangi kanaldan?**

§10.6b platformu ve kanalı iki eksen olarak ayırmıştı. **OS ekseni 2 Kasım 2026'da
kapanmıştır** (§2.4): desteklenen platform kümesi **Linux x64 + Windows x64**'tür; macOS
yoktur. Geriye tek açık eksen kalır:

| Kanal | Sonucu | Maliyet |
|---|---|---|
| **Installer** (`.deb`, `.msi`) | `.desktop`/menü/MIME entegrasyonunu (E10, A14) **mümkün kılar**; installer-only hata sınıfını getirir | `package.sh` ikisini de zaten üretiyor; bakım yükü bu sınıfta |
| **Taşınabilir** (`.tar.gz`) | Installer işini ve o hata sınıfını keser, W1'i kısaltır; **E10'u tek başına imkansız kılar** | Linux'ta ölçülen ~%25 daha büyük artefakt (§10.6b) |
| **İkisi birlikte** *(bugünkü durum)* | İki kanal da yayınlanır; varsayılan açık kalır | Neredeyse sıfır ek maliyet — `release-assets.sh` zaten ikisini üretiyor |

Kararın yol haritasına etkisi:

- **Windows Authenticode imzası zorunlu değildir.** SmartScreen uyarır, engellemez; ilk 100
  kullanıcı için uyarıyı kabul etmek meşru bir tercihtir. İmza bir **bütçe** kararıdır ve
  notarization'ın aksine takvimi ~1,5–2 hafta büyütmez (sertifika ~1–2 gün + CI adımı).
- **macOS'a ilişkin hiçbir bütçe yoktur** — ne Developer ID, ne `notarytool`, ne staple, ne CI
  secret'ı, ne A11. Bu, P2 takviminden ölçülebilir bir kalem sildi (§2.4).
- Yalnız taşınabilir kanal seçilirse **A14 (masaüstü entegrasyonu) kapsamdan düşer** ve
  "OS'ten aç" hedefi (§13.6/2'nin bir parçası) gerçekleşemez — bu, bilinçli bir feragat olarak
  yazılmalıdır, sessizce düşmemelidir.

Bu karar verilmeden yazılacak bir yol haritası, kodun ifade edemediği tek kısıtı tahmin etmiş
olur.

### 13.6 P2 için yanlışlanabilir çıkış kapıları (öneri)

1. **Üzerine yazma güvenliği:** Yüklemeden sonra diskte değişen bir dosyaya kaydetmek ya sorar
   ya reddeder; symlink'li belge kaydedildikten sonra **hâlâ symlink**'tir ve hedefi güncellenmiştir;
   geçersiz taslak yolu hedef olarak kabul edilmez. Hepsi testli.
2. **Asgari döngü:** `Ctrl+F` bulur, `F3`/`Shift+F3` gezer, `Ctrl+H` bir/tümünü değiştirir; aynı
   işlevler menüden de erişilebilir; arama modeli toolkit'siz testli; **arama 10 MB'da UI'yı
   kilitlemez** (ölçülmüş süre sınırı ile).
3. **Yerel ayar sığması:** En uzun DE/FR metinleriyle her `Alert` 768 px'e sığar (kelepçe +
   sarma + yeniden boyutlandırma); tüm tarih/saat gösterimi UI dilini kullanır.
4. **Yazarken donma yok — ölçülmüş:** 10 MB'lık belgede saniyede bir çalışan istatistik
   yenilemesi **< 5 ms** (bugün 163,7 ms); FX thread'inde dosya G/Ç'si veya `fsync` yok;
   `getText()` kirli kontrolünden önce kopyalanmaz.
5. **Sürüm var ve çalışıyor:** `v1.x` GitHub Releases'te yayınlanmıştır; CI, yayından **önce**
   app image'ı başlatıp `.deb`'i kurup kaldırmıştır; `verify-artifacts.sh` CI'da koşmuştur;
   `SHA256SUMS` yayındadır.

---

## 14. Bulgu Dizini

**Dağılım:** 0 kritik · **8 yüksek · 50 orta · 10 açık düşük** (+2 kapandı: B11, B13; +1 kapsam
dışı: U10).
v3.1'e göre değişim: **B14**, **B15**, **B16**, **X5**, **D14**, **M9**, **M10**, **T7**, **U13**, **B6**
eklendi; **D2** kod okumasından deneye, **D14** ve **jlink** tahminden ölçüme taşındı; **U2**
daraltıldı; "10 MB'da kaydetme donar" iddiası **geçersiz** sayıldı. B11 ve B13 kapandı. macOS'un
üründen kaldırılmasıyla **U10 kapsam dışına** düştü ve U4 etkisizleşti (§2.4).

### Yüksek

| ID | Bulgu | Kanıt | Doğrulama |
|---|---|---|---|
| **D1** | Dış değişiklik algılama yok; sessiz üzerine yazma | `FileService.java:69-74,296,310` | Kod |
| **D2** | Symlink'li belgede link yok oluyor, hedef güncellenmiyor | `AtomicFiles.java:54-74` | **Deney** |
| **D3** | Taslak başlığındaki yol doğrulanmıyor | `RecoveryStore.java:333-344` | Kod |
| **E1** | Ara/Değiştir yok | `KeyboardShortcutHandler.java:59-73`; bundle | Kod |
| **X1** | Dosya G/Ç'si FX thread'inde; eşzamanlılık ilkeli yok | §5.4 (0 executor) | Kod |
| **B1** | CI artefaktı çalıştırmıyor | `maven.yml` | Kod |
| **B2** | Hiç sürüm yayınlanmadı; hat denenmedi | `git tag -l` boş; API `/releases` = `[]` | **Ölçüm** |
| **U1** | 9 `Alert` kelepçesiz/sarmasız/yeniden boyutlandırılamaz | §9.1 | Kod |

### Orta

| ID | Bulgu | Kanıt |
|---|---|---|
| **D4** | Taslak taraması symlink takip ediyor, sınırsız okuma, yalnız en yeni | `RecoveryStore.java:131-139,145,271` |
| **D5** | `parked-*.jtw` toplanmıyor (yaş/sayı/boyut) | `RecoveryStore` |
| **D6** | Atomik olmayan fallback doğrulanmıyor; temp süpürme yok; `finally` istisnayı maskeliyor | `AtomicFiles.java:72-79` |
| **D7** | Disk kontrolü yok; `Error` kaydetme hata yolunu atlıyor | `FileService.java:311,332`; `AutoSaveService.java:317` |
| **D8** | Belge düzeyinde kilit yok | `InstanceLock` |
| **D12** | Ayar yazma hatası sessiz; `SecurityException` kaçıyor | `AppSettings.java:300-308` |
| **D13** | Ayar şeması sürümü yok | `AppSettings.java:67-81` |
| **D14** | Disk çarpanı 4,00× (tepe ≈6×); kullanıcıya söylenmiyor | **Ölçüm** |
| **E2** | Menü çubuğu yok; Yeni/Farklı Kaydet arayüzde görünmüyor | `ToolbarView.java:134-138` |
| **E3** | CLI/süreç argümanıyla açma yok | `getParameters()` yok |
| **E4** | Sürükle-bırak yok | `Dragboard` yok |
| **E7** | Yazdırma/PDF yok | `PrinterJob` yok |
| **E8** | Satır numaraları yok | düz `TextArea` |
| **E10** | Dosya ilişkilendirmesi/MIME `.desktop` yok | §10.6 |
| **X2** | Otomatik kayıt kirli kontrolünden önce tam kopya alıyor | `AutoSaveService.java:136,140-144` |
| **X3** | 1 Hz istatistik O(n), üç tam kopya | `SessionStats.java:71,79,84` |
| **X4** | `readAllBytes` bellek çarpanı, boyut sınırı yok | `FileService.loadFile`; `RecoveryStore.java:271` |
| **X5** | Tekrar eden FX yükünün %99'u istatistik: 10 MB'da 163,7 ms/s | **Ölçüm** |
| **U3** | `-toolbar-text`, popup satırları, editör metni ölçülmüyor; `-toolbar-text-muted` ölü | `ContrastRatioTest`; `*.css:10` |
| **U4** | Tooltip'ler sabit `Ctrl` yazıyor *(etkisiz: macOS kapsam dışı)* | `ToolbarView.java:84-91` |
| **U5** | Sabit tarih/saat biçimi | `StatsBarModel.java:51-52`; `MainController.java:33` |
| **U6** | Erişilebilir adlar yalnız 6 combo'da | `ToolbarView.java:399-404,539` |
| **U7** | Karşılama her açılışta, opt-out yok | `FxApplication.java:70` |
| **U8** | Minimum pencere genişliği yok | `MainStage.java:53-54` |
| **U9** | Ses politikası: ok/kısayol tuşları da ses çıkarıyor; seviye yok | `EditorKeyHandler.java:31-40` |
| **U10** | *(kapsam dışı)* macOS çıkış yolu garantisi yok — platformla birlikte düştü | `MainStage`, `FxApplication` |
| **U11** | Çökme metinleri İngilizce | `CrashHandler.java:43,71`; `FxApplication.java:77` |
| **U12** | `": "` / `" - "` ayırıcıları kodda | `ToolbarFit.java:93`; `MainController.java:184` |
| **U13** | Kısayol listesi handler'la iki yönde uyuşmuyor | §7.4 |
| **B3** | Windows Authenticode imzası yok *(notarization kalemi kapsamla düştü)* | `release.yml`, `package.sh` |
| **B4** | jlink yok; JAR'ın %34'ü ölü ağırlık (Media+FXML) | §10.2, §10.3 |
| **B5** | Yeniden üretilebilirlik (çift build) doğrulanmadı | `pom.xml:61` |
| **B7** | `~/.jtypewriter` tüm OS'lerde | `AppDirs.java:20` |
| **B8** | Masaüstü entegrasyonu eksik (`Categories=Unknown`, MIME yok) | §10.6 |
| **B9** | Destek yüzeyi yok (`SECURITY.md`, şablon, tanılama) | §10.7 |
| **B10** | Dependabot yalnız action'lar; SBOM/zafiyet taraması yok | `.github/dependabot.yml` |
| **B16** | Kanal ekseni (kurulan vs taşınabilir) hiç analiz edilmemişti; taşınabilirlik OS güven kapısını çözmez, E10'u tek başına imkansız kılar | §10.6b |
| **B14** | `verify-artifacts.sh` CI'da koşmuyor | `.github/`, `scripts/` |
| **B15** | İkon `--check` CI'da değil | `generate-icons.py`, `.github/` |
| **M1** | Komut/eylem katmanı yok | `KeyboardShortcutHandler`, `ToolbarView` |
| **M2** | Tek belge varsayımı kök hücrelerde | `FileService.java:69-74` |
| **M3** | 9 `Alert` tekrarı | §9.1 |
| **M4** | `AppLogging` global statik; enjeksiyon yok | `AppLogging.java:37,153`; `pom.xml:145-149` |
| **M5** | Dinleyici sırası örtük | `AppGraph` |
| **M6** | `UndoHistoryReset` yansıma; dönüş değeri yok sayılıyor | `EditorArea`; `UndoHistoryReset` |
| **M7** | Undo geçmişi sınırsız | JavaFX varsayılanı |
| **M8** | Dispose sözleşmesi yok | `StatsBar` |
| **M9** | `AtomicFiles` hedefin *kimliğini* sormuyor | `AtomicFiles.java:54` |
| **M10** | Boyut/sayı sınırı kavramı yok | §8.4 |
| **T6** | Testler gerçek `~/.jtypewriter`'a yazıyor | `pom.xml:145-149`; §11.5 |
| **T7** | Kapsam ölçümü yok; test sayısı yol kapsamı değil | §11.5 |

### Düşük

| ID | Bulgu |
|---|---|
| **D9** | "Vazgeç" sonrası başarısız taslak silme yutuluyor |
| **D10** | Yedek terfisi başarısız → en yeni nesil yok |
| **D11** | `user.home` yoksa açılış NPE ile ölür |
| **E5** | Son dosyalar listesi yok (tek `last.file`) |
| **E6** | Tercihler penceresi yok |
| **E9** | Sarma/yazı tipi ailesi/satır sonu/sekme politikası sabit |
| **U2** | Durum çubuğu boyutu yalnız fare çift tıklamasıyla *(daraltıldı: editörün klavye yolu var)* |
| **B6** | Çift build bayt-özdeşliği doğrulanmadı |
| **B12** | Yeniden üretilebilirlik/attestation yok |
| **B13** | *(kapandı)* `AGENTS.md` artık sürüm kontrolünde |
| **M11** | `releases/` ve `target/` git'te değil: ölçümler korunmuyor |
| **B11** | *(kapandı)* `.deb` lisans yükü düzeltildi (`03c277c`) |

### 14.1 v3.1'e göre değişen hükümler

| Bulgu | v3.1 | v4.0 | Neden |
|---|---|---|---|
| "Kaydetme 10 MB'da donar" | İddia (çıkarım) | **GEÇERSİZ** | 7,1 ms ölçüldü (§8.2) |
| **X5** | — | **Yeni, yüksek öncelikli** | 163,7 ms/s ölçüldü; gerçek darboğaz |
| **U2** | Etki 3 (yalnız fare) | Etki 1 | Editörün klavye yolu var |
| **B14, B15** | — | **Yeni** | W0 kanıtı CI'da koşmuyor |
| **D14** | "~5×" tahmini | **Ölçüldü: 4,00× kalıcı, ≈6× tepe** | §6.2 |
| **B4 / jlink** | "~50 MiB hedefi" tahmini | **Ölçüldü: 59 MiB runtime, ~71 MiB imaj** | §10.2 |
| **D2** | Kod okuması | **Deneyle kanıtlandı** | §6.2 |
| **M4** | "Enjeksiyon seam'i yok" | Kısmen yanlış: test-görünürlüğünde seam var (`openFileHandler(Path)`) | §5.3 |
| **T7** | — | **Yeni** | Kapsam ölçümü yok; "350 test" yol kapsamı vermiyor |

---

## 15. Ölçüm Protokolü (yeniden üretilebilirlik)

Bu belgedeki her sayı aşağıdaki komutlarla yeniden üretilebilir. Amaç, belge silinse bile
ölçümün kaybolmamasıdır (§10.9).

### 15.1 Kod tabanı ve test

```bash
cd /home/hyperpostulate/repositories/JTypeWriter
git log --oneline | wc -l                      # commit
find src/main/java -name '*.java' | wc -l      # 51 sınıf
find src/main/java -name '*.java' -exec cat {} + | wc -l   # 7611 satır
find src/test/java -name '*.java' | wc -l      # 42 dosya
grep -rho '@Test' src/test/java | wc -l        # 350
for f in src/main/resources/messages*.properties; do
  echo -n "$f "; grep -cE '^[a-zA-Z0-9_.]+=' "$f"; done   # 6 × 90
mvn -B clean test                              # 350/0/0  (repo kökünden)
```

### 15.2 Artefakt

```bash
du -sb releases/linux/JTypeWriter releases/linux/JTypeWriter/lib/runtime \
      releases/linux/JTypeWriter/lib/app releases/linux/*.deb
find releases/linux/JTypeWriter -type f | wc -l          # 125
unzip -l releases/linux/JTypeWriter/lib/app/*.jar | tail -2   # 6105 giriş
dpkg-deb -f releases/linux/*.deb Depends Package Version Installed-Size
cat releases/linux/JTypeWriter/lib/runtime/release | tr ' ' '\n' | grep -c '^[a-z]'  # modül
du -sb releases/linux/JTypeWriter/lib/runtime/lib/modules
bash scripts/verify-artifacts.sh --skip-build   # 17/17
```

### 15.3 Performans (harness)

`target/classes` üzerinde, fsync dahil:

```java
var bytes = text.getBytes(UTF_8);
long t0 = System.nanoTime(); TextCodec.decode(bytes);              long t1 = System.nanoTime();
byte[] enc = TextCodec.encode(text, UTF_8, false);                 long t2 = System.nanoTime();
AtomicFiles.writeMoving(file, enc);                                long t3 = System.nanoTime();
st.updateText(text);
st.getWordCount(); st.getCharCount(); st.getCharCountNoSpaces(); st.getLineCount();
```

`javac -cp target/classes` + `java -cp target/classes:<out>`. §8.2 ve §8.3 tabloları bu şekilde
üretildi.

### 15.4 Açılış, bellek, symlink, disk çarpanı

```bash
# açılış + RSS: taze profil, X11 eşlemesi beklenir
H=$(mktemp -d); JAVA_TOOL_OPTIONS="-Duser.home=$H" DISPLAY=:0.0 \
  releases/linux/JTypeWriter/bin/JTypeWriter & pid=$!
until xwininfo -root -tree | grep -q '"JTypeWriter"'; do sleep 0.05; done
ps -o rss= -p $pid

# symlink: hedef korunuyor mu (D2)
ln -s /tmp/real/notes.txt /tmp/link/notes.txt
# AtomicFiles.writeMoving(link, bytes) → Files.isSymbolicLink(link) == false  ⇒ kusur

# disk çarpanı: 6 kayıt sonrası dizin toplamı / belge boyu == 4,00 (D14)
```

### 15.5 jlink kazancı

```bash
jlink --add-modules java.base,java.desktop,java.logging,java.xml,java.prefs,\
java.datatransfer,java.scripting,jdk.unsupported --strip-debug --no-header-files \
--no-man-pages --compress=zip-6 --output rt && du -sb rt   # 62.248.693 B ≈ 59 MiB
```

### 15.6 Bilinen ölçüm sınırları

| Sınır | Etki |
|---|---|
| Tek makine (Linux x64, NVMe, X11) | Açılış/kaydetme süreleri başka donanımda değişir; ağ FS ölçülmedi |
| `getText()` toolkit gerektirir | Otomatik kaydın kopya maliyeti ölçülmedi (§8.5) |
| `jpackage` yalnız host platformu | Windows artefakt boyutları bu belgede yok; macOS kapsam dışı (§2.4) |
| RSS tek koşu kümesinden | ±%5 oynayabilir; mutlak değil eğilim okunmalı |
| Kontrast testi hex matematiği | Render edilmiş CSS doğrulanmıyor |

---

## 16. Tek Paragraf

JTypeWriter, dört ayda 137 commit ve 350 testle, bir metin editörünün **en pahalı ve en az
görünen** kısmını — veri bütünlüğünü — gerçekten çözmüş durumda: atomik yazma, dönen yedek,
üç durumlu kilit, çıkış matrisi, kodlama matrisi, gerçek süreç öldürmeyle doğrulanmış kurtarma.
Bu belge o tabanı doğruladı, ölçtü ve **abartılı bir iddiayı** düzeltti: kaydetme 10 MB'da
donmuyor, 7 ms sürüyor; asıl tekrar eden yük saniyede bir çalışan ve 10 MB'da 163,7 ms tutan
istatistik yenilemesidir — yani ürünü yavaşlatan şey G/Ç değil, sayaç. Buna karşılık üç şey
eksik ve üçü farklı katmanda: **güven** (dış değişiklik sessizce eziliyor, symlink'li belge
kaydedilince gerçek hedef hiç güncellenmiyor — bu artık kanıtlı), **deneyim** (ara/değiştir,
menü, yazdırma yok; 14 eylemin 2'si arayüzde hiç görünmüyor) ve **erişim** (0 tag, 0 Release,
0 yıldız; CI ürettiği artefaktı hiç açmıyor; 145,4 MiB'lik paketin jlink ile ~71 MiB'e
inebileceği ölçüldü ama uygulanmadı). Ağırlıklı olgunluk endeksi **2,53/5**. P2'nin sorusu
"daha çok özellik" değil: *bir romancı bir bölümü bitirip ertesi gün aynı dosyaya güvenle
dönebilir mi — ve indirdiği ikili bunu gerçekten yapıyor mu?* Bugünkü cevap hayır; ama zemin,
bu soruya bir fazda "evet" denebilecek kadar sağlam.
