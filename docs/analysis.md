# JTypeWriter — Ürünleştirme Analizi

**Baseline:** `v1.1.4` (pom.xml), 27 Kasım 2026
**Kapsam:** Bu belge sıfırdan yazıldı. Önceki analizler okunmadı ve kullanılmadı;
tek taşınan bilgi, projenin **P0–P3 arasında dört ürünleştirme fazı** geçirmiş olduğudur.
Aşağıdaki her iddia depodaki koddan, betiklerden, iş akışlarından ve testlerden
doğrudan çıkarıldı; satır/dosya kanıtı ilgili maddede verildi.

**Ürünleştirme tanımı:** Çalışan bir programı; indirilebilen, güvenilen, kurulabilen,
anlaşılabilen ve terk edilmeden kullanılmaya devam eden bir ürüne dönüştürmek.
Kod kalitesi bu tanımın yalnızca bir parçası; dağıtım güveni, ilk kullanım deneyimi,
destek ve sürdürülebilirlik de en az onun kadar belirleyici.

---

## 0. Ölçüm kartı

| Gösterge | Değer | Kaynak |
|---|---|---|
| Üretim kodu | 79 sınıf / 16.179 satır | `src/main/java` |
| Test kodu | 67 test sınıfı / 685 test / 14.509 satır | `src/test/java`, `mvn test` |
| Dil | 6 (EN, TR, DE, FR, IT, ES), paket başına 176 anahtar | `messages*.properties` |
| Yayınlanan sürüm | 9 etiket: v1.0.0 → v1.1.4 (4–16 Kasım) | `git tag` |
| Platform | Linux x64, Windows x64 | CI matrisi, `package.sh` |
| Kurulum boyutu | `.deb` ≈ 45 MB, açılmış imaj ≈ 73 MB (runtime 60 MB) | `releases/linux` |
| Çalışma zamanı bağımlılığı | Yok (gömülü JDK + JavaFX) | `package.sh` |
| Derleme tabanı | JDK 25, Maven 3.8, JavaFX 26.0.1 | `pom.xml` |
| Lisans | Apache-2.0, ticari model yok | `pom.xml`, `LICENSE` |
| Ağ kullanımı | Yok (telemetri, güncelleme denetimi, hesap yok) | kod + README |

---

## 1. Ürün tek cümlede

JTypeWriter; **tek belge**, **düz metin**, **çevrimdışı** ve **veri kaybına takıntılı**
bir masaüstü yazı aracıdır. Farklılaştığı yer özellik sayısı değil, üç iddiadır:
metni geri alınamaz biçimde silmeyi engelleyen YOLO modu, daktilo sesiyle kurulan
yazma ritüeli ve her kayıtta/çökmede metni kurtaran veri güvenliği.

Hedef kullanıcı: roman/öykü/günlük yazan, dikkat dağıtıcı editörlerden yorulmuş,
bulut ve hesap istemeyen kişi. Bu, dar ama savunulabilir bir niş.

---

## 2. Güçlü yönler

### 2.1 Veri güvenliği — ürünün en olgun ekseni

- **Atomik yazma:** her kayıt geçici dosyaya yazılıp `fsync` + rename ile yerine konuyor;
  yarım belge oluşması yapısal olarak engelli (`AtomicFiles`, `FileService`).
- **Dönen yedekler:** üzerine yazma `<belge>.bak`, `.bak.1`, `.bak.2` bırakıyor ve dosya
  izinlerini koruyor (`BackupKeeper`).
- **Kurtarma taslağı:** 30 saniyede bir ve odak kaybında anlık görüntü; çökme sonrası
  geri yükleme teklifi; ertelenen taslak temiz çıkışta bile yaşıyor (`AutoSaveService`,
  `RecoveryStore`).
- **Gerçek çökme kanıtı:** `ProcessCrashRecoveryTest` alt JVM'i `destroyForcibly()` ile
  öldürüp ikinci JVM'de ASCII dışı metni birebir kurtarıyor — bu, iddianın testle
  kanıtlandığı nadir yerlerden biri.
- **Kodlama disiplini:** BOM tutarlılığı, BOM'suz dosyada UTF-8/UTF-16 ayrımı,
  Windows-1254 sorusu, kayıpsız `?` yasağı (`TextCodec`, `EncodingMatrixTest`).
- **Sınırlar tanımlı:** 64 MiB belge tavanı (`MAX_DOCUMENT_BYTES`), 64 MiB taslak tavanı,
  30 gün / 10 dosya park sınırı; multi-gigabyte dosyanın uygulamayı `OutOfMemoryError`
  ile düşürmesi engellenmiş.
- **Görünür arıza:** yazılamayan yedek durum çubuğunda uyarı bırakıyor, başarısız taslak
  silme kendi uyarısını gösteriyor; `CrashHandler` yakalanmayan istisnayı loglayıp
  kullanıcıya gösteriyor.

### 2.2 Mühendislik disiplini ve test güveni

- **685 test, JavaFX toolkit'i başlatmadan** koşuyor; CI'da `tr-TR` ve `de-DE` sistem
  yerelleriyle ayrıca tekrarlanıyor. Bu, i18n ve yerel biçimlendirme hatalarını
  gerçekten yakalıyor.
- **Test edilebilir tasarım:** kararlar, geometri ve durum makineleri JavaFX dışına
  çıkarılmış (`YoloPolicy`, `ToolbarFit`, `ExitDisposition`, `AutosaveHealth`,
  `DialogSizing`, `TextCodec`); UI sınıfları ince kalıyor.
- **Değişmez kurallar testle korunuyor:** lisans başlığı, altı bundle'ın anahtar eşitliği,
  `THIRD-PARTY-NOTICES` byte eşitliği, README test tablosunun `src/test`'ten türetilmesi,
  CI kapılarının varlığı (`LicenseHeaderTest`, `MessagesTest`, `ReadmeTestTableTest`,
  `CiGatesTest`).
- **Performans regresyon testi:** 10 MB belgede istatistik tazeleme için 50 ms CI eşiği
  (`StatsPerfHarnessTest`) — "hızlı" iddiası ölçülü.
- **Yeniden üretilebilirlik:** sabit `project.build.outputTimestamp`, sabitlenmiş GitHub
  Action SHA'ları, sürümün tek kaynağı, ikon ve fontun tek kaynağı.

### 2.3 Dağıtım hattı olgunluğu (ürün seviyesinde)

- **Aynı kapılar her yerde:** `maven.yml` ve `release.yml` ayrı adım listeleri değil,
  tek `verify-build.yml` iş akışını çağırıyor; etiket, dalın geçtiği ikon kontrolü,
  yerel testler, byte karşılaştırması ve smoke testinden geçmeden yayınlanamıyor.
- **Byte düzeyinde doğrulama:** `verify-artifacts.sh` depo ↔ uygulama imajı ↔ `.deb`
  içeriğini karşılaştırıyor; lisans metinlerinin imajda, jarda ve pakette bulunduğunu
  ayrıca kontrol ediyor.
- **Gerçek başlatma testi:** Xvfb altında görüntü başlatılıyor, `.deb` kurulup
  çalıştırılıp kaldırılıyor; Windows'ta launcher'ın ayakta kalması doğrulanıyor.
- **Denetimli yayın:** elle çalıştırma yalnızca dry-run; yayın sadece `v<version>`
  etiketinden ve etiket/pom eşleşmesi doğrulandıktan sonra yapılıyor.
- **Bütünlük:** `SHA256SUMS` üretiliyor; indirme sayfası doğrulama dosyasıyla birlikte
  sunuluyor.

### 2.4 Ürün kapsamı — niş için yeterli ve tutarlı

- Altı arayüz dili, üç tema, altı metin rengi; hepsi yazarken değiştirilebiliyor.
- Ara/değiştir kartı, son dosyalar, hoş geldin ve hakkında kartları; hepsi modal olmayan,
  belgeyi kilitlemeyen paneller.
- Yazdırma ve PDF: kaydedilmiş `.txt` dosyasından, tek `DocumentLayout` ile, gömülü
  fontla, **byte-deterministik** PDF. Bu, "yazıyı teslim etme" ihtiyacını karşılıyor.
- Komut envanterinin tek kaynaktan üretilmesi (`EditorAction`): menü, kısayol ve araç
  çubuğu birbirinden kopamıyor.
- Tek örnek kilidi, komut satırı argümanı, sürükle-bırak, oturum geri yükleme.

### 2.5 Konumlandırma ve gizlilik

- **Sıfır ağ:** hesap yok, telemetri yok, güncelleme denetimi yok. Bu, hedef kullanıcı
  için gerçek bir satış argümanı ve rakiplerin çoğunda yok.
- **Sessiz arayüz:** tek belge, kalıcı menü şeridi yok, tam ekranda yalnızca metin.
- **Apache-2.0:** kurumsal kullanım ve katkı için engel yok.

### 2.6 Dokümantasyon

- İki dilli README, kullanıcıyı önce ne yaptığıyla karşılayacak sırada; teknik malzeme
  sona taşınmış.
- Sürüm notu şablonu iki dilli ve `@VERSION@`/`@TAG@` ile tek kaynaktan besleniyor.
- Üçüncü taraf bildirimleri, font kökeni ve lisans metinleri depoda ve pakette.

---

## 3. Zayıf yönler

Aşağıdakiler "eksik özellik" listesi değil; ürünün **indirilme, güvenilme, benimsenme
ve elde tutma** aşamalarındaki gerçek sürtünmeler. Her madde gözlenen durumu ve ürün
etkisini birlikte veriyor.

### 3.1 Dağıtım, güven ve keşfedilebilirlik — en kritik küme

- **İmzasız ikili dosyalar.** Windows `.msi` kod imzalı değil (depoda `signtool`,
  sertifika veya imzalama adımı yok), Linux `.deb` GPG imzalı değil ve bir depo
  üzerinden dağıtılmıyor. Sonuç: Windows'ta SmartScreen "bilinmeyen yayıncı" uyarısı;
  Linux'ta `apt` imza doğrulaması yok, kullanıcı yalnızca `SHA256SUMS` ile yetiniyor.
  `SHA256SUMS` bütünlüğü kanıtlıyor, **kaynağı** kanıtlamıyor. Ürün için en pahalı
  tek eksik bu.
- **Dosya ilişkilendirmesi yok.** `package.sh` yalnızca `--linux-shortcut` ve
  `--win-menu/--win-shortcut` kullanıyor; `--file-associations` veya `.desktop`
  `MimeType` girdisi yok. Yani `.txt`/`.md` çift tıklaması JTypeWriter'ı açmıyor.
  README ise "dosya ilişkilendirmesi veya çift tıklama aynı yolu aynı koddan geçirir"
  diyor — bu, **kurulumun sağlamadığı bir şeyi vaat eden** bir doğruluk riski.
- **Tek örnek, ama devir yok.** İkinci başlatma kendi penceresini açmıyor, uyarı verip
  çıkıyor; çalışan pencereye dosya **aktarmıyor** (IPC/socket yok). Dosya ilişkilendirmesi
  eklense bile, uygulama açıkken ikinci bir dosyaya çift tıklamak çalışmaz.
- **Güncelleme yolu yok.** Güncelleme denetimi bilinçli olarak kapalı ve alternatif bir
  bildirim kanalı da yok. Uygulama içinden v1.0.0'daki bir kullanıcı v1.1.4'ten haberdar
  olamıyor; güvenlik/düzeltme sürümleri kullanıcıya ulaşmıyor. Gizlilik duruşuyla
  çelişmeden çözülebilecek bir tasarım kararı (varsayılan kapalı, **opt-in** denetim)
  henüz yok.
- **Kanal kapsamı dar.** Yalnızca GitHub Releases. `winget`, Chocolatey, Scoop, apt
  deposu, Flatpak, Snap veya AppImage yok. Debian türevi olmayan Linux dağıtımlarında
  kullanıcı `.tar.gz` ile baş başa; Windows'ta paket yöneticisiyle kurulum yok.
  Bu, keşfedilebilirliği doğrudan sınırlıyor.
- **Boyut ve açılış.** Açılmış imaj ≈ 73 MB, `.deb` ≈ 45 MB; runtime tek başına 60 MB.
  Aynı işi yapan yerel editörlere göre büyük. Splash/preloader yok; soğuk açılışta
  görsel geri bildirim yok.
- **Mimari kapsamı.** CI matrisi yalnızca `ubuntu-latest` ve `windows-latest` (x64);
  `release-assets.sh` ve iş akışı `arm64` adını tanıyor ama hiçbir iş ARM64 imajı
  üretmiyor. macOS bilinçli olarak kapsam dışı (bu bir eksik değil, karar — ama
  pazar kaybı).
- **SBOM / köken kanıtı yok.** Pom yorumu SBOM okuyucularından söz ediyor ama depoda
  CycloneDX/SPDX üretimi veya SLSA provenance adımı yok. Kurumsal bir alıcı için
  tedarik zinciri kanıtı eksik.

### 3.2 Yazma deneyimi boşlukları

- **İmleç/scroll konumu hatırlanmıyor.** `AppSettings` anahtarları arasında `caret` veya
  `scroll` yok. Uzun bir belgeyi kapatıp açan kullanıcı baştan başlıyor; bir yazı
  uygulamasında bu, günlük kullanımda hissedilen bir kayıp.
- **Otomatik kayıt beklentisi yönetilmiyor.** 30 saniyelik döngü yalnızca kurtarma
  taslağı yazıyor, gerçek `.txt` dosyasına dokunmuyor. Doğru ve güvenli bir karar, ama
  "autosave" kelimesini duyan kullanıcı bunu diske yazma sanıyor. Görünür bir açıklama
  ve isteğe bağlı "gerçek dosyaya otomatik kaydet" seçeneği yok.
- **Yazım denetimi yok.** Hedef kitle (romancı, öykücü, günlük yazarı) için masabaşı
  araçların neredeyse tamamında var. Yeni bağımlılık gerektirdiği için bilinçli bir
  erteleme olabilir; ama ürün boşluğu olarak duruyor.
- **Yazma motivasyonu araçları yok.** Kelime hedefi, günlük seri, oturum geçmişi,
  ilerleme grafiği yok. İstatistikler yalnızca anlık; "bugün ne yazdım" sorusu
  cevapsız.
- **Son dosyalar listesi yönetilemiyor.** "Listeyi temizle" veya "sabitle" komutu yok;
  yalnızca en yeni 10 kayıt gösteriliyor.
- **Bul/değiştir düz metin.** Regex yok (bilinçli), ama büyük metinlerde "tümünü
  değiştir" öncesi eşleşme sayısı dışında önizleme yok.
- **Yazdırma sabit.** A4, 20 mm, tek font, sayfa aralığı/önizleme yok. PDF için doğru;
  yazdırma için esneklik bekleyen kullanıcıyı kaybediyor.
- **Tek belge.** Sekme/bölünmüş görünüm yok. Bu bir kapsam kararı; ama rakip
  editörlerden gelen kullanıcı için ilk gün sürtünmesi.
- **Arayüz ölçeklenmiyor.** Chrome fontu sabit (11 px etiketler), yalnızca editör yazı
  boyutu 10–48 arasında değişiyor. Yüksek DPI veya görme zorluğu olan kullanıcı için
  arayüzü büyütme yolu yok.

### 3.3 Erişilebilirlik

- **Kısmi erişilebilirlik.** `AccessibleText` araç çubuğu kontrollerine, ara/değiştir
  alanlarına ve kart kapatma/satır etiketlerine verilmiş; buna karşın menüler, durum
  çubuğu ve diyaloglar için sistematik bir ekran okuyucu hikâyesi ve hiç erişilebilirlik
  testi yok.
- **Kontrast testi dar kapsamlı** (`ContrastRatioTest`): üç temada da yalnızca durum
  çubuğu renkleri için WCAG AA kanıtı var; araç çubuğu, menü ve kart metinleri için yok.
- **Yüksek kontrast modu yok.** Üç tema (Koyu, Açık, Sepya) var, ancak işletim sisteminin
  yüksek kontrast tercihini izleyen bir tema veya ayar bulunmuyor.
- **Klavye ile tam gezinme kanıtlanmamış.** `Tab` sırası araç çubuğu için var; kartlar,
  son dosyalar ve diyaloglar için uçtan uca klavye testi yok.

### 3.4 Sürdürülebilirlik ve operasyon

- **Tek kişi (bus factor = 1).** Depo, sürüm, imzalama, destek ve yayın tek kişide.
  Ürünleşme arttıkça bu, en büyük tek risk.
- **Gelir modeli yok.** Apache-2.0, bağış/sponsorluk/ücretli katman yok. Uzun vadeli
  bakım için ekonomik zemin tanımsız; kod imzalama sertifikası gibi maliyetli ama
  gerekli adımlar bu yüzden erteleniyor olabilir.
- **Destek girişi zayıf.** Issue şablonu, `CONTRIBUTING.md`, davranış kuralları, SSS
  veya destek politikası yok. Hata bildirimi için tek yol e-posta/GitHub; kullanıcıya
  ne göndereceğini söyleyen bir "tanılama paketi" de yok.
- **Saha görünürlüğü yok.** Telemetri ve çökme raporlama bilinçli olarak yok; bu gizlilik
  açısından doğru, ama hangi hatanın kaç kullanıcıda olduğu bilinmiyor. Alternatif
  kanıt yolu (opt-in tanılama, görüşme, indirme metrikleri) tanımlanmamış.
- **UI/E2E testi yok (tasarım gereği).** Smoke testi, paketlenmiş uygulamanın
  penceresinin açıldığını doğruluyor (Linux'ta `xwininfo`); davranışı veya görünümü
  değil. Görsel/layout regresyonu ancak kullanıcı fark ederse görülüyor.
- **Bağımlılık güncellemesi kısmi.** Dependabot yalnızca GitHub Actions'ı izliyor;
  JavaFX, JUnit ve Mockito elle güncelleniyor.
- **Derleme tabanı yüksek.** JDK 25 + Maven 3.8 zorunlu, wrapper yok (bilinçli).
  Katkıda bulunmak isteyen için ek kurulum yükü.
- **JPMS modül tanımı yok.** `module-info.java` bulunmuyor; uygulama sınıf yolunda
  çalışıyor. Sorun değil, ama jlink/dağıtım hikâyesini el yapımı tutuyor.

### 3.5 Pazarlama ve kanıt

- **README'de ekran görüntüsü/GIF yok.** Yalnızca uygulama ikonu var. Ürünün "nasıl
  hissettirdiği" iddiası (daktilo sesi, sade arayüz, temalar) görsel olarak
  kanıtlanmıyor; indirme kararı için en güçlü tek eksik.
- **Web sitesi / açılış sayfası yok.** Depo README'si hem tanıtım hem kılavuz olarak
  çalışıyor; arama motoru görünürlüğü ve "indir" dönüşümü depoya mahkûm.
- **Kullanıcı hikâyesi / referans yok.** "Kim, neden kullanıyor" kanıtı yok.
- **Sürüm notları teknik ağırlıklı.** Şablon var, ama kullanıcıya "ne değişti, neden
  önemli" diye anlatan kısa bir üst bölüm standardı yok.

---

## 4. Risk tablosu

| Risk | Olasılık | Etki | Mevcut azaltma | Kalan açık |
|---|---|---|---|---|
| İmzasız kurulum yüzünden indirme terk edilmesi | Yüksek | Yüksek | SHA256SUMS, kaynak açık | Kod imzası / GPG yok |
| Çift tıkla açmanın çalışmaması | Yüksek | Orta | CLI argümanı, sürükle-bırak | Dosya ilişkilendirmesi yok |
| Uygulama açıkken ikinci dosyanın açılmaması | Orta | Orta | Tek örnek uyarısı | IPC devri yok |
| Kullanıcının eski sürümde kalması | Yüksek | Orta | Sürüm notları, etiketler | Güncelleme bildirimi yok |
| Büyük belgede UI takılması | Orta | Orta | 64 MiB tavanı, artımlı istatistik, arka plan arama | TextArea tabanlı, sanallaştırma yok |
| Saha hatasının görülmemesi | Orta | Orta | Yerel log, çökme diyaloğu | Opt-in tanılama yok |
| Tek geliştiriciye bağımlılık | Orta | Yüksek | Dokümantasyon, test disiplini | Bus factor = 1 |
| Erişilebilirlik davası / kurumsal red | Düşük | Orta | Kısmi `AccessibleText`, kontrast testi | Tam denetim yok |
| Türkçe dışı yerel biçim hatası | Düşük | Düşük | TR/DE CI koşuları | FR/IT/ES sistem yereli koşusu yok |

---

## 5. P4 kararı

### 5.1 Karar sorusu

P4, "daha çok özellik" fazı olmamalı. Mevcut tablo, özellik eksiğinden çok **ürünün
kendini kanıtlama ve kullanıcıya ulaşma** aşamasında tıkanmış durumda. Bu yüzden soru
şu:

> JTypeWriter'ın bir sonraki sıçraması **güven ve dağıtım** tarafında mı, **yazma
> deneyimi** tarafında mı, yoksa **erişilebilirlik ve destek** tarafında mı?

### 5.2 Dört seçenek

**P4-A — Güven ve dağıtım (önerilen ilk dilim)**

Kapsam:
1. Windows kod imzalama (OV/EV sertifika) + `.deb` için GPG imzası/apt deposu.
2. `--file-associations` ile `.txt`/`.md` ilişkilendirmesi, `.desktop` `MimeType`.
3. Tek örnek **IPC devri**: ikinci çağrı dosyayı çalışan pencereye aktarsın.
4. Varsayılan kapalı, opt-in güncelleme bildirimi (tek HTTPS isteği) veya tamamen
   çevrimdışı kalıp sürüm notlarını uygulama içinde gösterme kararı.
5. Bir paket yöneticisi kanalı (winget) + bir taşınabilir Linux kanalı (AppImage).
6. CycloneDX SBOM + SLSA provenance (yayın varlıklarının kaynağını kanıtlama).
7. İmaj boyutunu küçültme (jlink modül listesi gözden geçirme; hedef < 50 MB).
8. ARM64 imajları.

Ne kazandırır: indirme engelini kaldırır, "Open with" beklentisini karşılar, kullanıcıyı
güncel tutar, kurumsal/tedarik zinciri sorularını cevaplar. Ne kazandırmaz: yazma
deneyimine tek bir yeni özellik eklemez.

**P4-B — Yazma deneyimi ve elde tutma**

Kapsam: imleç/scroll konumu hatırlama, gerçek otomatik kayıt seçeneği, son dosya
listesi yönetimi, kelime hedefi/günlük seri, yazım denetimi (yeni bağımlılık), arayüz
ölçeği, yazdırma önizlemesi, isteğe bağlı sekme.

Ne kazandırır: mevcut kullanıcıyı daha uzun tutar, günlük kullanım sürtünmesini azaltır.
Ne kazandırmaz: yeni kullanıcının ilk indirme kararını değiştirmez.

**P4-C — Erişilebilirlik, destek ve kanıt**

Kapsam: uçtan uca erişilebilirlik denetimi (ekran okuyucu, klavye, odak sırası, UI
zoom, yüksek kontrast), issue şablonları + `CONTRIBUTING.md` + SSS, opt-in tanılama
paketi, README'ye ekran görüntüsü/GIF, basit bir açılış sayfası.

Ne kazandırır: ürünü savunulabilir ve desteklenebilir yapar; kurumsal/okul kullanımı
ve topluluk katkısı için kapı açar. Ne kazandırmaz: çekirdek yazma döngüsünü iyileştirmez.

**P4-D — P4'e girmeyi ertele: önce kanıt topla**

Kapsam: 5–10 gerçek yazarla kısa kullanılabilirlik testi, GitHub indirme/issue
metriklerinin okunması, "ilk 10 dakika" deneyiminin elle izlenmesi. Sonuçta P4'ün
hangi paket olduğuna kanıtla karar verilir.

Ne kazandırır: telemetri olmadığı için başka türlü elde edilemeyecek öncelik bilgisi.
Ne kazandırmaz: kod tabanında hiçbir şeyi düzeltmez; birkaç hafta gecikme.

### 5.3 Öneri

**P4-A'yı ilk dilim olarak başlat; P4-D'yi P4-A ile paralel ve hafif tut.**

Gerekçe:

- Ürünün en güçlü tarafı veri güvenliği; en zayıf tarafı ise o güvenli ürünü **kullanıcıya
  ulaştırma** kanalı. İmzasız kurulum, çalışmayan çift tıklama ve güncelleme yokluğu,
  özellik kalitesinden bağımsız olarak indirme kararını öldürüyor.
- İlk üç madde (imza, dosya ilişkilendirmesi, IPC) küçük, iyi tanımlı ve doğrudan
  doğrulanabilir; "P4 başladı" demek için yeterli somutlukta.
- P4-B'nin en değerli iki maddesi (imleç konumu, gerçek otomatik kayıt) aslında küçük;
  bunlar P4-A'nın yanına "hızlı kazanç" olarak eklenebilir. Yazım denetimi ve sekmeler
  ise büyük ve stratejik; acele edilmemeli.
- P4-C, P4-A ile çakışmaz (destek/erişilebilirlik) ve ayrı bir iz olarak yürütülebilir;
  ancak imza ve ilişkilendirme çözülmeden erişilebilirlik yatırımının getirisi düşük.

**Karar kuralı:**

| Öncelik hedefi | Seçilecek paket |
|---|---|
| Yeni kullanıcı / indirme dönüşümü | P4-A |
| Mevcut kullanıcıyı elde tutma | P4-B |
| Kurumsal, okul, topluluk, itibar | P4-C |
| Öncelik bilinmiyor | P4-D (en fazla 2 hafta), sonra A |

### 5.4 P4 için önerilen ilk dilim (P4.0)

Tek bir sürümde bitecek, dış bağımlılığı yönetilebilir paket:

1. **Dosya ilişkilendirmesi** — `.txt` ve `.md`, Linux ve Windows, `verify-artifacts.sh`
   ile pakette doğrulanır.
2. **Tek örnek IPC devri** — ikinci çağrı çalışan pencereye dosya aktarır; eşzamanlı
   erişim ve yol doğrulama testleri eklenir.
3. **Opt-in güncelleme bildirimi** — varsayılan kapalı, tek HTTPS isteği, kapatılabilir;
   gizlilik iddiası README'de buna göre güncellenir.
4. **Windows kod imzalama altyapısı** — sertifika temini dış bağımlılık; pipeline
   hazırlanır, sertifika yoksa yayın imzasız olarak açıkça işaretlenir.
5. **winget manifestosu** — ilk paket yöneticisi kanalı.
6. **İmleç/scroll konumu hatırlama** — küçük, bağımsız, günlük faydası yüksek.

P4.0 çıkış kriterleri (ölçülebilir):

- Temiz bir Windows makinesinde kurulumda "bilinmeyen yayıncı" uyarısı yok (sertifika
  varsa) veya yayın notunda imzasızlık açıkça belirtiliyor.
- Kurulumdan sonra `.txt` çift tıklaması JTypeWriter'ı açıyor.
- Uygulama açıkken ikinci bir `.txt` çift tıklaması mevcut pencereye düşüyor.
- `winget install` çalışıyor.
- Uzun bir belge kapatılıp açıldığında imleç aynı yerde.
- README'deki dosya ilişkilendirme iddiası artık kurulumla eşleşiyor.

### 5.5 P4'e girilmeyecek koşullar

Aşağıdakilerden biri doğruysa P4 ertelenmeli:

- Ürünün gerçek kullanıcı sayısı ve kullanım sıklığı bilinmiyor ve iki haftalık bir
  doğrulama bunu makul maliyetle ortaya çıkarabilir (P4-D).
- Kod imzalama sertifikası bütçesi yok; bu durumda P4-A'nın geri kalanı yine yapılabilir
  ama "güven" hedefi tam karşılanmaz.
- Tek geliştirici kapasitesi yalnızca bakım ve hata düzeltmeye yetiyor; yeni kapsam
  eklemek mevcut kalite çıtasını düşürür.

---

## 6. Karar öncesi son not

JTypeWriter, "hobi projesi" çıtasının belirgin biçimde üzerinde: veri güvenliği,
test disiplini ve dağıtım hattı olgun. Eksik olan kod kalitesi değil, **ürünün
kendini kanıtlama yüzeyi**: imza, ilişkilendirme, güncelleme, görsel kanıt ve destek.
P4 bu yüzeye odaklanırsa, mevcut mühendislik birikimi doğrudan ürüne dönüşür;
odaklanmazsa, iyi yazılmış ama kimsenin kolayca kuramadığı bir program olarak kalır.
