# JTypeWriter — Ürünleştirme Analizi

**Baseline:** `v1.1.7` (`pom.xml`), 28 Kasım 2026
**Yöntem:** Bu belge sıfırdan yazıldı. Önceki analizler okunmadı ve kullanılmadı;
tek taşınan bilgi, projenin **P0–P3 arasında dört ürünleştirme fazı** geçirmiş
olduğudur. Aşağıdaki her iddia depodaki koddan, betiklerden, iş akışlarından,
testlerden ve belgelerden doğrudan çıkarıldı; kanıt ilgili maddede verildi.

**Ürünleştirme tanımı:** Çalışan bir programı; indirilebilen, güvenilen,
kurulabilen, anlaşılabilen ve terk edilmeden kullanılmaya devam eden bir ürüne
dönüştürmek. Kod kalitesi bu tanımın yalnızca bir parçasıdır; dağıtım güveni,
ilk kullanım deneyimi, destek ve sürdürülebilirlik de en az onun kadar
belirleyicidir.

---

## 0. Ölçüm kartı

| Gösterge | Değer | Kaynak |
|---|---|---|
| Üretim kodu | 83 sınıf / 17.185 satır | `src/main/java` |
| Test kodu | 75 test sınıfı / 728 test / 15.870 satır | `src/test/java` |
| Arayüz dili | 6 (EN, TR, DE, FR, ES, IT), paket başına 193 anahtar | `messages*.properties` |
| Yayınlanan sürüm | 12 etiket: v1.0.0 (4 Kasım) → v1.1.7 (28 Kasım) | `git tag` |
| Platform | Linux x64, Windows x64 | CI matrisi, `package.sh` |
| Kurulum boyutu | açılmış imaj ≈ 73 MiB, `.deb` ≈ 44 MiB | README'de ölçülmüş |
| Çalışma zamanı bağımlılığı | Yok (gömülü JDK + JavaFX) | `scripts/package.sh` |
| Derleme tabanı | JDK 25, Maven 3.8+, JavaFX 26.0.1 | `pom.xml` |
| Lisans | Apache-2.0, ücretli katman yok | `pom.xml`, `LICENSE` |
| Ağ kullanımı | Kullanıcının başlattığı iki eylem dışında yok | `UpdateService`, About kartı |
| Ürün yüzeyi | README (EN/TR) + altı dilli tek sayfa + hero GIF | `README*.md`, `site/`, `docs/media/` |

---

## 1. Ürün tek cümlede

JTypeWriter; **tek belge**, **düz metin**, **çevrimdışı** ve **veri kaybına
takıntılı** bir masaüstü yazı aracıdır. Farklılaştığı yer özellik sayısı değil,
üç iddiadır: metni geri alınamaz biçimde silmeyi engelleyen YOLO modu, daktilo
sesiyle kurulan yazma ritüeli ve her kayıtta/çökmede metni kurtaran veri
güvenliği.

Hedef kullanıcı: roman, öykü veya günlük yazan; bulut, hesap ve dikkat dağıtan
arayüzlerden yorulmuş kişi. Bu dar ama savunulabilir bir niş.

Ürünün bugünkü sınırı da bu tanımdan çıkıyor: JTypeWriter kendini **yazma
eyleminin içinde** kanıtlıyor, ama o eyleme **ulaşma yolunda** (imza, dosya
ilişkilendirmesi, paket kanalları) hâlâ sürtünme üretiyor.

---

## 2. Güçlü yönler

### 2.1 Veri güvenliği — ürünün en olgun ekseni

- **Atomik yazma:** her kayıt geçici dosyaya yazılıp `fsync` + rename ile yerine
  konuyor; yarım belge oluşması yapısal olarak engelli (`AtomicFiles`,
  `FileService`).
- **Dönen yedekler:** üzerine yazma `<belge>.bak`, `.bak.1`, `.bak.2` bırakıyor
  ve dosya izinlerini koruyor (`BackupKeeper`).
- **Kurtarma taslağı:** 30 saniyede bir ve odak kaybında anlık görüntü; çökme
  sonrası geri yükleme teklifi; ertelenen taslak temiz çıkışta bile yaşıyor
  (`AutoSaveService`, `RecoveryStore`).
- **Gerçek çökme kanıtı:** `ProcessCrashRecoveryTest` alt JVM'i
  `destroyForcibly()` ile öldürüp ikinci JVM'de ASCII dışı metni birebir
  kurtarıyor. İddianın testle kanıtlandığı nadir yerlerden biri.
- **Kodlama disiplini:** BOM tutarlılığı, BOM'suz dosyada UTF-8/UTF-16 ayrımı,
  Windows-1254 sorusu, kayıpsız `?` yasağı (`TextCodec`, `EncodingMatrixTest`).
- **Sınırlar tanımlı:** 64 MiB belge tavanı (`MAX_DOCUMENT_BYTES`), 64 MiB
  taslak bütçesi, 30 gün / 20 dosya park sınırı (`RecoveryRetention`).
- **Görünür arıza:** yazılamayan yedek durum çubuğunda uyarı bırakıyor;
  başarısız taslak silme kendi uyarısını gösteriyor; yakalanmayan istisna
  loglanıp kullanıcıya gösteriliyor (`CrashHandler`).
- **Kaydetme kararı kullanıcıya ait:** yeni belge, dış değişiklik ve çıkış
  akışları aynı kaydet/vazgeç kapısından geçiyor (`ExitDisposition`,
  `ExternalChangeChoice`).

Bu eksen, ürünün "hobi projesi" çıtasını en net aştığı yerdir.

### 2.2 Mühendislik disiplini ve test güveni

- **728 test, JavaFX toolkit'i başlatmadan** koşuyor; CI'da `tr-TR` ve `de-DE`
  sistem yerelleriyle ayrıca tekrarlanıyor. Bu, i18n ve yerel biçimlendirme
  hatalarını gerçekten yakalıyor.
- **Test edilebilir tasarım:** kararlar, geometri ve durum makineleri JavaFX
  dışına çıkarılmış (`YoloPolicy`, `ToolbarFit`, `ExitDisposition`,
  `AutosaveHealth`, `DialogSizing`, `TextCodec`, `UpdateCheck`,
  `StartupArguments`); UI sınıfları ince kalıyor.
- **Değişmez kurallar testle korunuyor:** lisans başlığı, altı bundle'ın anahtar
  eşitliği, `THIRD-PARTY-NOTICES` byte eşitliği, README test tablosunun
  `src/test`'ten türetilmesi, CI kapılarının varlığı, sitenin altı dilli
  eşliği (`LicenseHeaderTest`, `MessagesTest`, `ReadmeTestTableTest`,
  `CiGatesTest`, `SitePageTest`).
- **Performans regresyon testi:** 10 MB belgede istatistik tazeleme için 50 ms
  CI eşiği (`StatsPerfHarnessTest`) — "hızlı" iddiası ölçülü.
- **Yeniden üretilebilirlik:** sabit `project.build.outputTimestamp`, tam SHA'ya
  sabitlenmiş GitHub Action'ları, sürümün tek kaynağı, ikon ve fontun tek
  kaynağı.

### 2.3 Dağıtım hattı olgunluğu

- **Aynı kapılar her yerde:** `maven.yml` ve `release.yml` ayrı adım listeleri
  değil, tek `verify-build.yml` iş akışını çağırıyor; etiket, dalın geçtiği ikon
  kontrolü, yerel testler, byte karşılaştırması ve smoke testinden geçmeden
  yayınlanamıyor.
- **Byte düzeyinde doğrulama:** `verify-artifacts.sh` depo ↔ uygulama imajı ↔
  `.deb` içeriğini karşılaştırıyor; lisans metinlerinin imajda, jarda ve pakette
  bulunduğunu ayrıca kontrol ediyor.
- **Gerçek başlatma testi:** Xvfb altında görüntü başlatılıyor; `.deb` kurulup
  çalıştırılıp kaldırılıyor, Windows'ta launcher'ın ayakta kalması doğrulanıyor.
- **Denetimli yayın:** elle çalıştırma yalnızca dry-run; yayın sadece
  `v<version>` etiketinden ve etiket/pom eşleşmesi doğrulandıktan sonra yapılıyor.
- **Bütünlük:** `SHA256SUMS` üretiliyor; beş varlık (iki kurulum, iki taşınabilir
  arşiv, kaynak arşivi) sabit adlarla yayınlanıyor.
- **Sürüm notu disiplini:** şablon iki dilli, `@VERSION@`/`@TAG@`/`@COMMITS@` ile
  besleniyor; imzasızlık yayın notunda açıkça yazılıyor.

### 2.4 Ürün kapsamı — niş için yeterli ve tutarlı

- Altı arayüz dili, üç tema, altı metin rengi; hepsi yazarken değiştirilebiliyor.
- Bul/değiştir kartı, son dosyalar, hoş geldin ve hakkında kartları; hepsi modal
  olmayan, belgeyi kilitlemeyen paneller.
- Yazdırma ve PDF: kaydedilmiş `.txt` dosyasından, tek `DocumentLayout` ile,
  gömülü fontla, **byte-deterministik** PDF (`PdfExport`, `PdfExportTest`).
- Komut envanterinin tek kaynaktan üretilmesi (`EditorAction`): menü, kısayol ve
  araç çubuğu birbirinden kopamıyor.
- Tek örnek kilidi, komut satırı argümanı, sürükle-bırak.
- **Oturum geri yükleme artık konum dahil:** imleç ve dikey kaydırma konumu
  belgeyle birlikte saklanıyor (`document.position.*`), pencere geometrisi ve
  tam ekran durumu da geri geliyor. Bu, günlük kullanımda hissedilen eski
  boşluğu kapatmış durumda.

### 2.5 Gizlilik ve konumlandırma

- **Sıfır arka plan ağı:** hesap yok, telemetri yok, açılışta istek yok, zamanlı
  denetim yok.
- **Tek ağ eylemi kullanıcıya bağlı:** `Help → Check for updates` yalnızca
  kullanıcı tıkladığında, tek bir HTTPS GET ile GitHub'ın `/releases/latest`
  yönlendirmesini okuyor; belge, dosya adı veya tercih göndermiyor
  (`UpdateService`, `UpdateCheck`).
- **Belge ve arayüz aynı şeyi söylüyor:** README'deki "yalnızca sizin
  başlattığınız iki eylem" cümlesi kodla eşleşiyor; site de üçüncü taraf istek
  yapmıyor.
- **Apache-2.0:** kurumsal kullanım ve katkı için lisans engeli yok.

### 2.6 Ürün yüzeyi — P0–P3'ün görünür çıktısı

- **Tek sayfalık tanıtım sitesi** (`site/`): altı dil, JavaScript olmadan da
  okunabilen işaretleme, GitHub Pages'e **artifact** ile yayın, üçüncü taraf
  kaynak yok.
- **Hero GIF'ler:** README ve site, paketlenmiş uygulamanın gerçek kaydını
  kullanıyor (`docs/media/hero-en.gif`, `hero-tr.gif`; 900 px, 2 MiB bütçesi
  testle pinli).
- **İki dilli README**, kullanıcıyı önce ne yaptığıyla karşılayacak sırada;
  teknik malzeme sona taşınmış.
- **Kart metinleri tek kaynak:** hoş geldin ve hakkında kartlarının açılış
  cümleleri sitenin ilk iki paragrafıyla aynı; `SitePageTest` altı dili
  karşılaştırıyor.
- **Footer, About kartının hukuki bloğunu taşıyor:** telif, e-posta, lisans ve
  garanti reddi; metinler bundle'lardan karşılaştırılıyor.

---

## 3. Zayıf yönler

Aşağıdakiler "eksik özellik" listesi değil; ürünün **indirilme, güvenilme,
benimsenme, elde tutma ve sürdürülme** aşamalarındaki gerçek sürtünmelerdir.

### 3.1 Dağıtım, güven ve keşfedilebilirlik — en kritik küme

- **İmzasız ikili dosyalar.** Windows `.msi` kod imzalı değil; depoda
  `signtool`, sertifika veya imzalama adımı yok. Linux `.deb` GPG imzalı değil ve
  bir depo üzerinden dağıtılmıyor. Sonuç: Windows'ta SmartScreen "bilinmeyen
  yayıncı" uyarısı; Linux'ta `apt` imza doğrulaması yok. `SHA256SUMS`
  bütünlüğü kanıtlıyor, **kaynağı** kanıtlamıyor. Yayın notu bunu dürüstçe
  yazıyor, ancak dürüstlük uyarının maliyetini ortadan kaldırmıyor.
- **Dosya ilişkilendirmesi yok.** `package.sh` yalnızca `--linux-shortcut` ve
  `--win-menu/--win-shortcut` kullanıyor; `--file-associations` veya `.desktop`
  `MimeType` girdisi yok. Yani `.txt`/`.md` çift tıklaması JTypeWriter'ı
  açmıyor. README ise "bir dosya ilişkilendirmesi ya da çift tıklama aynı yolu
  aynı koddan geçirir" diyor — bu, **kurulumun sağlamadığı bir şeyi vaat eden**
  bir doğruluk riski.
- **Tek örnek, ama devir yok.** İkinci başlatma kendi penceresini açmıyor, uyarı
  verip çıkıyor; çalışan pencereye dosya **aktarmıyor** (IPC/socket yok). Dosya
  ilişkilendirmesi eklense bile, uygulama açıkken ikinci bir dosyaya çift
  tıklamak çalışmaz.
- **Güncelleme bildirimi yok.** Elle `Help → Check for updates` var; ama
  kullanıcı menüyü açmazsa yeni sürümden haberdar olmuyor. Gizlilik duruşuyla
  çelişmeden çözülebilecek bir tasarım (varsayılan kapalı, **opt-in** denetim)
  henüz yok.
- **Kanal kapsamı dar.** Yalnızca GitHub Releases. `winget`, Chocolatey, Scoop,
  apt deposu, Flatpak, Snap veya AppImage yok. Debian türevi olmayan Linux
  dağıtımlarında kullanıcı `.tar.gz` ile baş başa; Windows'ta paket yöneticisiyle
  kurulum yok.
- **Boyut ve açılış.** Açılmış imaj ≈ 73 MiB, `.deb` ≈ 44 MiB. Aynı işi yapan
  yerel editörlere göre büyük; splash/preloader yok, soğuk açılışta görsel geri
  bildirim yok.
- **Mimari kapsamı.** CI matrisi yalnızca `ubuntu-latest` ve `windows-latest`
  (x64). `release-assets.sh` `arm64` adını tanıyor ama hiçbir iş ARM64 imajı
  üretmiyor. macOS bilinçli olarak kapsam dışı (karar, ama pazar kaybı).
- **SBOM / köken kanıtı yok.** Pom yorumu SBOM okuyucularından söz ediyor ama
  depoda CycloneDX/SPDX üretimi veya SLSA provenance adımı yok. Kurumsal bir
  alıcı için tedarik zinciri kanıtı eksik.

### 3.2 Yazma deneyimi boşlukları

- **Yazım denetimi yok.** Hedef kitle için masaüstü araçların neredeyse
  tamamında var. Yeni bağımlılık gerektirdiği için bilinçli bir erteleme
  olabilir; ama ürün boşluğu olarak duruyor.
- **Yazma motivasyonu araçları yok.** Kelime hedefi, günlük seri, oturum
  geçmişi, ilerleme grafiği yok. İstatistikler yalnızca anlık; "bugün ne
  yazdım?" sorusu cevapsız (`SessionStats` oturumla sınırlı).
- **Son dosyalar listesi yönetilemiyor.** "Listeyi temizle" veya "sabitle"
  komutu yok; yalnızca en yeni 10 kayıt gösteriliyor (`RecentFiles.MAX`).
- **Otomatik kayıt beklentisi yönetilmiyor.** 30 saniyelik döngü yalnızca
  kurtarma taslağı yazıyor, gerçek `.txt` dosyasına dokunmuyor. Doğru ve güvenli
  bir karar, ama "autosave" kelimesini duyan kullanıcı bunu diske yazma sanıyor.
  Görünür bir açıklama ve isteğe bağlı "gerçek dosyaya otomatik kaydet" seçeneği
  yok.
- **Bul/değiştir düz metin.** Regex yok (bilinçli), ama büyük metinlerde "tümünü
  değiştir" öncesi eşleşme sayısı dışında önizleme yok.
- **Yazdırma sabit.** A4, 20 mm, tek font, sayfa aralığı/önizleme yok
  (`PageGeometry`). PDF için doğru; yazdırma için esneklik bekleyen kullanıcıyı
  kaybediyor.
- **Tek belge.** Sekme/bölünmüş görünüm yok. Bu bir kapsam kararı; ama rakip
  editörlerden gelen kullanıcı için ilk gün sürtünmesi.
- **Arayüz ölçeklenmiyor.** Chrome fontu sabit (11 px etiketler), yalnızca
  editör yazı boyutu değişiyor. Yüksek DPI veya görme zorluğu olan kullanıcı
  için arayüzü büyütme yolu yok.

### 3.3 Erişilebilirlik

- **Kısmi erişilebilirlik.** `AccessibleText` yalnızca dört yerde: araç çubuğu
  (`ToolbarView`), ara/değiştir paneli, kartlar ve son dosyalar. Menü, durum
  çubuğu ve diyaloglar için sistematik bir ekran okuyucu hikâyesi yok.
- **Erişilebilirlik testi yok.** `src/test` altında `Accessible` geçen tek bir
  test bulunmuyor; klavye sırası ve odak davranışı uçtan uca doğrulanmıyor.
- **Kontrast testi kapsamı dar** (`ContrastRatioTest`): durum çubuğu metinleri,
  kayıt onayı, otomatik yedek uyarısı, odak halkası ve klavye vurgulu menü
  satırı; araç çubuğu, menü ve kart gövdeleri için AA kanıtı yok.
- **Yüksek kontrast modu yok.** Üç tema var, ancak işletim sisteminin yüksek
  kontrast tercihini izleyen bir tema veya ayar bulunmuyor.

### 3.4 Destek, operasyon ve sürdürülebilirlik

- **Tek kişi (bus factor = 1).** Depo, sürüm, imzalama, destek ve yayın tek
  kişide. Ürünleşme arttıkça bu, en büyük tek risk.
- **Gelir modeli yok.** Apache-2.0; bağış/sponsorluk/ücretli katman yok. Kod
  imzalama sertifikası gibi maliyetli ama gerekli adımlar bu yüzden erteleniyor
  olabilir.
- **Destek girişi zayıf.** Issue/PR şablonu, `CONTRIBUTING.md`,
  `CODE_OF_CONDUCT.md`, SSS veya destek politikası yok. Hata bildirimi için tek
  yol e-posta/GitHub; kullanıcıya ne göndereceğini söyleyen bir "tanılama paketi"
  de yok.
- **Saha görünürlüğü yok.** Telemetri ve çökme raporlama bilinçli olarak yok; bu
  gizlilik açısından doğru, ama hangi hatanın kaç kullanıcıda olduğu bilinmiyor.
  Alternatif kanıt yolu (opt-in tanılama, görüşme, indirme metrikleri)
  tanımlanmamış.
- **UI/E2E testi yok (tasarım gereği).** Smoke testi, paketlenmiş uygulamanın
  penceresinin açıldığını doğruluyor; davranışı veya görünümü değil. Görsel/layout
  regresyonu ancak kullanıcı fark ederse görülüyor.
- **Bağımlılık güncellemesi kısmi.** Dependabot yalnızca GitHub Actions'ı
  izliyor; JavaFX, JUnit ve Mockito elle güncelleniyor.
- **Derleme tabanı yüksek.** JDK 25 + Maven zorunlu, wrapper yok (bilinçli).
  Katkıda bulunmak isteyen için ek kurulum yükü.
- **JPMS modül tanımı yok.** `module-info.java` bulunmuyor; uygulama sınıf
  yolunda çalışıyor. Sorun değil, ama jlink/dağıtım hikâyesini el yapımı tutuyor.

### 3.5 Dokümantasyon doğruluğu — küçük ama güvenilirlik maliyeti olan açıklar

- **Kurtarma taslağı sayısı iki yerde farklı.** README (EN ve TR) "park edilmiş
  küme 10 dosya / 64 MiB ile sınırlıdır" diyor; `RecoveryRetention.MAX_FILES`
  ve onu pinleyen test **20** diyor. Kullanıcıya veri politikası anlatan bir
  cümlede yanlış sayı, ürünün en güçlü olduğu eksende güven kaybı.
- **Dosya ilişkilendirmesi iddiası** (bkz. 3.1) kurulumla eşleşmiyor.
- Bu iki madde, "dokümantasyon testle pinlenir" disiplininin kapsamadığı
  boşlukları gösteriyor: `ReadmeTestTableTest` test sayısını, `ReadmeMediaTest`
  görselleri, `SitePageTest` site metinlerini koruyor; ama README'nin **sayısal
  ürün politikaları** ile kod sabitleri arasında bir test yok.

### 3.6 Kanıt ve pazarlama

- **Kullanıcı hikâyesi / referans yok.** "Kim, neden kullanıyor" kanıtı yok.
- **Site var, dönüşüm ölçümü yok.** Gizlilik gereği analitik yok; bu doğru bir
  tercih, ama indirme sayfasının işe yarayıp yaramadığı bilinmiyor. GitHub
  indirme sayıları tek dolaylı sinyal.
- **Sürüm notları teknik ağırlıklı.** Şablon var, ama kullanıcıya "ne değişti,
  neden önemli" diye anlatan kısa bir üst bölüm standardı yok.

---

## 4. Risk tablosu

| Risk | Olasılık | Etki | Mevcut azaltma | Kalan açık |
|---|---|---|---|---|
| İmzasız kurulum yüzünden indirme terk edilmesi | Yüksek | Yüksek | SHA256SUMS, kaynak açık, yayın notunda açık uyarı | Kod imzası / GPG yok |
| Çift tıkla açmanın çalışmaması | Yüksek | Orta | CLI argümanı, sürükle-bırak | Dosya ilişkilendirmesi yok |
| Uygulama açıkken ikinci dosyanın açılmaması | Orta | Orta | Tek örnek uyarısı | IPC devri yok |
| Kullanıcının eski sürümde kalması | Yüksek | Orta | Elle güncelleme denetimi | Bildirim yok |
| Büyük belgede UI takılması | Orta | Orta | 64 MiB tavanı, artımlı istatistik, arka plan arama | TextArea tabanlı, sanallaştırma yok |
| Saha hatasının görülmemesi | Orta | Orta | Yerel log, çökme diyaloğu | Opt-in tanılama yok |
| Tek geliştiriciye bağımlılık | Orta | Yüksek | Dokümantasyon, test disiplini | Bus factor = 1 |
| Erişilebilirlik davası / kurumsal red | Düşük | Orta | Kısmi `AccessibleText`, dar kontrast testi | Tam denetim yok |
| Dokümantasyon-kod çelişkisi | Orta | Düşük | Bazı metinler testle pinli | Sayısal politikalar pinli değil |
| Türkçe dışı yerel biçim hatası | Düşük | Düşük | TR/DE CI koşuları | FR/IT/ES sistem yereli koşusu yok |

---

## 5. P4 kararı için girdiler

### 5.1 Karar sorusu

P4, "daha çok özellik" fazı olmamalı. Tablo, özellik eksiğinden çok **ürünün
kendini kanıtlama ve kullanıcıya ulaşma** aşamasında tıkanmış durumda. Soru şu:

> JTypeWriter'ın bir sonraki sıçraması **güven ve dağıtım** tarafında mı,
> **yazma deneyimi** tarafında mı, yoksa **erişilebilirlik ve destek** tarafında
> mı?

### 5.2 Dört aday paket

**P4-A — Güven, dağıtım ve keşfedilebilirlik**

Kapsam:
1. Windows kod imzalama (OV/EV sertifika) + `.deb` için GPG imzası/apt deposu.
2. `--file-associations` ile `.txt`/`.md` ilişkilendirmesi ve `.desktop`
   `MimeType` girdisi.
3. Tek örnek **IPC devri**: ikinci çağrı dosyayı çalışan pencereye aktarsın.
4. Varsayılan kapalı, opt-in güncelleme bildirimi (tek HTTPS isteği) veya
   tamamen çevrimdışı kalıp sürüm notlarını uygulama içinde gösterme kararı.
5. Bir paket yöneticisi kanalı (winget) + bir taşınabilir Linux kanalı
   (AppImage).
6. CycloneDX SBOM + SLSA provenance.
7. İmaj boyutunu küçültme (jlink modül listesi gözden geçirme).
8. ARM64 imajları.

Ne kazandırır: indirme engelini kaldırır, "Open with" beklentisini karşılar,
kullanıcıyı güncel tutar, kurumsal/tedarik zinciri sorularını cevaplar.
Ne kazandırmaz: yazma deneyimine yeni bir özellik eklemez.

**P4-B — Yazma deneyimi ve elde tutma**

Kapsam: yazım denetimi (yeni bağımlılık), kelime hedefi/günlük seri/oturum
geçmişi, son dosya listesi yönetimi, gerçek otomatik kayıt seçeneği, yazdırma
önizlemesi, arayüz ölçeği, isteğe bağlı sekme.

Ne kazandırır: mevcut kullanıcıyı daha uzun tutar, günlük kullanım sürtünmesini
azaltır.
Ne kazandırmaz: yeni kullanıcının ilk indirme kararını değiştirmez.

**P4-C — Erişilebilirlik, destek ve operasyon**

Kapsam: uçtan uca erişilebilirlik denetimi (ekran okuyucu, klavye, odak sırası,
UI zoom, yüksek kontrast), issue/PR şablonları, `CONTRIBUTING.md`, SSS ve destek
politikası, opt-in tanılama paketi, erişilebilirlik ve görsel regresyon testleri.

Ne kazandırır: ürünü savunulabilir ve desteklenebilir yapar; kurumsal/okul
kullanımı ve topluluk katkısı için kapı açar.
Ne kazandırmaz: çekirdek yazma döngüsünü iyileştirmez.

**P4-D — P4'e girmeyi ertele: önce kanıt topla**

Kapsam: 5–10 gerçek yazarla kısa kullanılabilirlik testi, GitHub indirme/issue
metriklerinin okunması, "ilk 10 dakika" deneyiminin elle izlenmesi (özellikle
temiz bir Windows makinesinde kurulum). Sonuçta P4'ün hangi paket olduğuna
kanıtla karar verilir.

Ne kazandırır: telemetri olmadığı için başka türlü elde edilemeyecek öncelik
bilgisi.
Ne kazandırmaz: kod tabanında hiçbir şeyi düzeltmez; birkaç hafta gecikme.

### 5.3 Öneri

**P4-A'yı ilk dilim olarak başlat; P4-D'yi P4-A ile paralel ve hafif tut.**

Gerekçe:

- Ürünün en güçlü tarafı veri güvenliği; en zayıf tarafı ise o güvenli ürünü
  **kullanıcıya ulaştırma** kanalı. İmzasız kurulum, çalışmayan çift tıklama ve
  güncelleme bildirimi yokluğu, özellik kalitesinden bağımsız olarak indirme
  kararını öldürüyor.
- İlk üç madde (imza, dosya ilişkilendirmesi, IPC) küçük, iyi tanımlı ve
  doğrudan doğrulanabilir; "P4 başladı" demek için yeterli somutlukta.
- P4-B'nin en değerli iki maddesi (yazım denetimi, hedef/seri) büyük ve
  stratejik; acele edilmemeli. Arayüz ölçeği ise küçük ve P4-C ile kesişiyor.
- P4-C, P4-A ile çakışmaz (destek/erişilebilirlik) ve ayrı bir iz olarak
  yürütülebilir; ancak imza ve ilişkilendirme çözülmeden erişilebilirlik
  yatırımının getirisi düşük.
- Dokümantasyon doğruluğu açıkları (3.5) P4'ten bağımsız olarak **hemen**
  kapatılmalı: bunlar yeni kapsam değil, mevcut güvenin korunması.

**Karar kuralı:**

| Öncelik hedefi | Seçilecek paket |
|---|---|
| Yeni kullanıcı / indirme dönüşümü | P4-A |
| Mevcut kullanıcıyı elde tutma | P4-B |
| Kurumsal, okul, topluluk, itibar | P4-C |
| Öncelik bilinmiyor | P4-D (en fazla 2 hafta), sonra A |

### 5.4 P4 için önerilen ilk dilim (P4.0)

Tek bir sürümde bitecek, dış bağımlılığı yönetilebilir paket:

1. **Dosya ilişkilendirmesi** — `.txt` ve `.md`, Linux ve Windows,
   `verify-artifacts.sh` ile pakette doğrulanır.
2. **Tek örnek IPC devri** — ikinci çağrı çalışan pencereye dosya aktarır;
   eşzamanlı erişim ve yol doğrulama testleri eklenir.
3. **Opt-in güncelleme bildirimi** — varsayılan kapalı, tek HTTPS isteği,
   kapatılabilir; gizlilik iddiası README ve sitede buna göre güncellenir.
4. **Windows kod imzalama altyapısı** — sertifika temini dış bağımlılık;
   pipeline hazırlanır, sertifika yoksa yayın imzasız olarak açıkça işaretlenir.
5. **winget manifestosu** — ilk paket yöneticisi kanalı.
6. **Dokümantasyon doğruluğu** — README'deki taslak sayısı ve dosya
   ilişkilendirmesi cümleleri koda uydurulur; bu iki iddia için test eklenir.

P4.0 çıkış kriterleri (ölçülebilir):

- Temiz bir Windows makinesinde kurulumda "bilinmeyen yayıncı" uyarısı yok
  (sertifika varsa) veya yayın notunda imzasızlık açıkça belirtiliyor.
- Kurulumdan sonra `.txt` çift tıklaması JTypeWriter'ı açıyor.
- Uygulama açıkken ikinci bir `.txt` çift tıklaması mevcut pencereye düşüyor.
- `winget install` çalışıyor.
- README'deki kurtarma politikası ile `RecoveryRetention` aynı sayıyı söylüyor
  ve bu bir testle pinli.
- README'deki dosya ilişkilendirme iddiası artık kurulumla eşleşiyor.

### 5.5 P4'e girilmeyecek koşullar

Aşağıdakilerden biri doğruysa P4 ertelenmeli:

- Ürünün gerçek kullanıcı sayısı ve kullanım sıklığı bilinmiyor ve iki haftalık
  bir doğrulama bunu makul maliyetle ortaya çıkarabilir (P4-D).
- Kod imzalama sertifikası bütçesi yok; bu durumda P4-A'nın geri kalanı yine
  yapılabilir ama "güven" hedefi tam karşılanmaz.
- Tek geliştirici kapasitesi yalnızca bakım ve hata düzeltmeye yetiyor; yeni
  kapsam eklemek mevcut kalite çıtasını düşürür.
- P4.0'ın ilk iki maddesi (dosya ilişkilendirmesi + IPC) bir sürümde
  doğrulanamıyorsa; bu, dağıtım hattının taşıyamayacağı bir kapsam demektir ve
  paket küçültülmelidir.

### 5.6 Karar öncesi son not

JTypeWriter, "hobi projesi" çıtasının belirgin biçimde üzerinde: veri güvenliği,
test disiplini, dağıtım hattı ve ürün yüzeyi olgun. Eksik olan kod kalitesi
değil, **ürünün kendini kanıtlama yüzeyi**: imza, dosya ilişkilendirmesi,
güncelleme, paket kanalları ve destek. P4 bu yüzeye odaklanırsa mevcut
mühendislik birikimi doğrudan ürüne dönüşür; odaklanmazsa iyi yazılmış ama
kimsenin kolayca kuramadığı bir program olarak kalır.

---

## Ek: Bu belgenin dayandığı doğrulamalar

- `mvn -o clean test` → **728 test, 0 hata, 0 atlama** (28 Kasım 2026, `v1.1.7`).
- `find src/main/java -name '*.java'` → 83 sınıf; `find src/test/java -name '*Test.java'` → 75 test sınıfı.
- `grep -c '^[A-Za-z][A-Za-z0-9]*=' src/main/resources/messages.properties` → 193 anahtar (altı bundle'da eşit, `MessagesTest`).
- `git tag` → v1.0.0 … v1.1.7 (12 etiket).
- `scripts/package.sh` içinde `--file-associations` yok; yalnızca kısayol ve menü girdisi var.
- `grep -rn "signtool\|gpg\|codesign" scripts/ .github/` → imzalama adımı yok.
- `git for-each-ref` → ilk etiket 4 Kasım 2026, son etiket 28 Kasım 2026.
