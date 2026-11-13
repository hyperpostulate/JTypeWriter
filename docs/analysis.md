# JTypeWriter — Sıfırdan Ürünleştirme Analizi

> **Sürüm:** 1.1.3 · **Dal:** `main` · **Tarih:** 13 Kasım 2026
> **Kapsam:** Ürün, mimari, veri güvenliği, deneyim, yerelleştirme, ambalaj, dağıtım,
> kalite hattı ve uyum — baştan sona, bu depodaki kod ve betikler üzerinden.
> **Bu belge bir yol haritası değildir.** İş sırası, kart, takvim ve kapı tanımlamaz;
> yalnızca durumu ve boşlukları ölçer. P4 planı ayrı bir belgenin konusudur.

---

## 0. Belgenin sözleşmesi

Bu analiz, depoda daha önce yürütülmüş ürünleştirme fazlarının (P0–P3) üzerine
**kurulmaz**. O fazların belgeleri `338f1c4` ile silinmiştir; geriye yalnızca
ulaşılan durum kalmıştır: sürüm numarası, test sayısı, paketleme hattı ve kod.
Bu belge o durumu **kendi ölçümleriyle** yeniden okur. P0–P3'e yapılan atıflar,
yalnızca "buraya nasıl gelindi" bağlamıdır; bir bulgunun gerekçesi değildir.

Kanıt seviyeleri:

| Seviye | Anlamı | Bu belgedeki kullanımı |
|---|---|---|
| **Ö** | Bu makinede ölçüldü (komut ve çıktı tekrar üretilebilir) | Satır/sınıf/test sayıları, dosya envanteri |
| **K** | Koddan doğrudan okundu (davranış, sabit, eşik) | Güvenlik matrisi, ayar anahtarları |
| **T** | Testin pinlediği değişmez | Ambalaj/uyum iddiaları |
| **V** | Çıkarım — doğrulanması gerekir | Pazar, çeviri kalitesi, erişilebilirlik |

Bir sayı burada yoksa, ölçülmemiştir. Tahmin edilen sayı **V** ile işaretlenir.

---

## 1. Yönetici özeti

### 1.1 Tek paragraf

JTypeWriter, olgunluğu **ürünleşme ekseninde dengesiz** bir masaüstü metin
editörüdür: veri güvenliği ve paketleme kanıtı, aynı büyüklükteki açık kaynak
projelerin çok üzerinde; buna karşılık **ürün tezi, dağıtım erişimi ve
desteklenebilirlik** aynı ölçüde olgunlaşmamıştır. Kod, "saf model + JavaFX'ten
sıyrılmış karar" disiplinini gerçekten uyguluyor ve test paketi bunu zorluyor;
bu, projenin en değerli ve en kolay kaybedilecek varlığıdır. Buna karşılık uygulama
hâlâ **tek belge, tek pencere, tek yazma biçimi** etrafında kurulmuştur ve
"editör" ile "metin alanı" arasındaki farkı kapatan yüzey (dışa aktarma, yazım
denetimi, belge içi gezinme, çoklu belge) yoktur. Yayınlanan sürümler imzasız,
güncelleme kanalı yok, paket deposu yok; kullanıcıya ulaşan yol tek bir GitHub
Releases sayfasıdır. Yani ürün **güvenilir** ama **görünmez**; **sağlam** ama
**dar**.

### 1.2 En önemli on bulgu

1. **Veri güvenliği gerçekten sınıfının üstünde.** Atomik + fsync'li yazma,
   dönen `.bak` nesilleri, içerik-hash'ine dayalı dış değişiklik algılama,
   kodlama sabitliği, kurtarma taslağı ve çıkış matrisi aynı üründe ve
   birbirini kapatacak biçimde tasarlanmış. Bu, pazarlama cümlesi değil,
   `AtomicFiles`, `FileService`, `RecoveryStore`, `ExitDisposition` ve
   testlerinde okunan bir mimari. **(K/T)**
2. **Bu güvenlik görünmüyor.** Kullanıcı ilk kaydedişte "belgenin 4× yer
   kapladığı" uyarısını bir kez görüyor; ama "dış değişiklik algılandı",
   "taslak park edildi", "yedek tutuldu" gibi olumlu sinyaller arayüzde
   kutlanmıyor. Güven, kanıtla değil, **hissedilen davranışla** oluşur. **(V)**
3. **Tek belge mimarisi ürünün tavanını belirliyor.** Sekme yok, bölünmüş
   görünüm yok, oturum geri yüklemede yalnızca *son* belge var. Bu bir eksik
   değil, bilinçli bir sadeleştirme; ama "yazma aracı" iddiası ile "tek dosya
   açık tutma" gerçeği arasındaki mesafe hiçbir yerde yazılı değil. **(K)**
4. **Dağıtım tek kanala bağlı ve imzasız.** `.deb`, `.msi`, `.tar.gz` ve
   `SHA256SUMS` üretiliyor; ama Authenticode/GPG imzası, apt/winget/Flatpak
   deposu, otomatik güncelleme ve sürümden bağımsız bir indirme sayfası yok.
   Windows'ta SmartScreen, Linux'ta "bu `.deb` nereden geldi" sorusu ilk
   temasta ürünün önüne geçiyor. **(K/V)**
5. **Test paketi kalitenin tek kapısı ve bunu iyi taşıyor.** 640 `@Test`
   işaretlemesi, 66 test dosyası, iki işletim sisteminde ve üç yerelde koşan
   bir hat, ikon hash kapısı, bayt karşılaştırma kapısı ve gerçek başlatma
   dumanı testi. Lint/formatter yokluğu bilinçli; ama **kapsam ölçümü, SBOM,
   SAST ve imzalı commit** de yok. **(Ö/T)**
6. **Performans kapısı var, ama tek noktada.** `StatsPerfHarnessTest` 10 MB
   metinde tek yenilemeyi 50 ms eşiğiyle koruyor. Arama taraması, açılış süresi,
   bellek ve paket boyutu için eşik yok; regresyon ancak kullanıcı hissederse
   görülür. **(K/T)**
7. **Yerelleştirme disiplinli, çeviri kalitesi doğrulanmamış.** Altı paket
   birebir aynı 156 anahtarı taşıyor ve `MessagesTest` ölü/eksik anahtarı
   yakalıyor. Buna karşılık çoğul kuralları, RTL, biçimlendirici tarih dışı
   kültürel farklar ve **anadil gözden geçirmesi** kapsam dışı. **(K/V)**
8. **Sınıf boyutları bakım riski üretiyor.** `FileService` 1.192,
   `ToolbarView` 767, `RecoveryStore` 757, `AppSettings` 739, `Messages` 822,
   `FindPanel` 502 satır. Hepsi testli; ama değişiklik maliyeti ve gözden
   geçirme yükü tek kişilik bir projede doğrusal büyümüyor. **(Ö)**
9. **Tek kişi = tek hata noktası.** 217 commit, 8 etiket, tek geliştirici,
   sorun şablonu yok, katkı rehberi README içinde bir paragraf. Otobüs
   faktörü 1; bu bir ürün riski olduğu kadar bir *süreklilik* riskidir. **(Ö)**
10. **Lisans/uyum tarafı nadiren görülen bir olgunlukta.** SPDX başlıkları,
    `THIRD-PARTY-NOTICES`, `legal/`, fat JAR içinde `META-INF/licenses/`,
    paket içinde lisans dosyası ve bunları pinleyen testler. Eksik olan tek
    şey makine-okunur SBOM ve GPL kaynak teklifinin **yayın sayfasında**
    görünür olması. **(T/V)**

### 1.3 Boyut skorları

| Boyut | Skor (1–5) | Gerekçe |
|---|---:|---|
| Ürün tezi ve konumlandırma | 3 | Net bir his var ("daktilo + YOLO"), doğrulanmış bir kitle yok |
| Mimari ve kod | 4 | Katmanlar, saf modeller ve DI kültürü güçlü; birkaç dev sınıf |
| Veri güvenliği | 5 | Atomik yazma, yedek, kurtarma, kodlama, dış değişiklik: tam küme |
| Deneyim ve erişilebilirlik | 3 | Klavye/odak/karşıtlık iyi; ekran okuyucu ve RTL yok |
| Yerelleştirme | 4 | 6 dil, 156 anahtar, testli; gözden geçirme süreci yok |
| Ambalaj | 5 | jlink + jpackage, iki platform, bayt karşılaştırması, duman testi |
| Dağıtım ve erişim | 2 | Tek kanal, imzasız, güncelleme yok, depo yok |
| Kalite hattı | 4 | Kapılar gerçek; kapsam/SBOM/SAST eksik |
| Uyum ve lisans | 4 | Örnek niteliğinde; SBOM ve kaynak teklifi görünürlüğü eksik |
| Desteklenebilirlik | 2 | Log var, telemetri yok (bilinçli), sorun toplama akışı yok |

Ortalama **3,6/5**. Yüksek skorlar *mühendislik* tarafında, düşük skorlar
*ürün ve pazar* tarafında toplanıyor. Bu, "iyi yapılmış ama satılmayan/duyulmayan
ürün" profilinin tam olarak kendisidir.

---

## 2. Ölçüm tabanı

Aşağıdaki sayılar bu çalışma sırasında, bu depoda okundu. Yeniden üretmek için
komutlar §15'te.

| Ölçüt | Değer | Seviye |
|---|---:|---|
| Sürüm (`pom.xml`) | 1.1.3 | Ö |
| Commit / etiket | 217 / 8 (`v1.0.0`…`v1.1.3`) | Ö |
| Üretim sınıfı | 72 | Ö |
| Üretim Java satırı | 14.329 | Ö |
| Test dosyası | 66 | Ö |
| `@Test` işaretlemesi | 640 (AGENTS.md 637 diyor; sayım yöntemi farkı) | Ö |
| Test Java satırı | 13.581 | Ö |
| Test/üretim satır oranı | 0,95 | Ö |
| Bundle anahtarı | 156 × 6 dil = 936 dizgi | Ö |
| CSS | 980 satır (base 812 + 3 tema × ~56) | Ö |
| Çalışma zamanı modülü | 5 (`java.base`, `java.desktop`, `java.logging`, `java.scripting`, `jdk.unsupported`) | K |
| İş akışı | 3 (`verify-build.yml` 267, `release.yml` 329, `maven.yml` 64) | Ö |
| Betik | 7 (2.303 satır) | Ö |
| Platform | Linux x64, Windows x64 | K |
| Dil | EN, TR, DE, FR, IT, ES | K |

Dikkat: 640 `@Test` ile 637 arasındaki fark, bazı dosyalarda yardımcı test
şablonları veya devre dışı bırakılmış işaretlemeler olabileceğini gösterir;
sayı bir *taban* olarak okunmalı, hedef olarak değil. Kurucu kültür bunu
AGENTS.md'de açıkça söylüyor ve doğru yapıyor.

---

## 3. Ürün: ne satıyor, kime, nerede duruyor

### 3.1 Değer önerisi (koddan çıkan)

Ürünün üç somut iddiası var ve üçü de kodda karşılığını buluyor:

1. **"Yazarken kaybetme."** Atomik yazma + yedek + taslak + kodlama sabitliği.
   Bu iddia *doğru*. Nadir görülen bir şey: pazarlama cümlesi ile hata yolunun
   aynı özeni görmesi.
2. **"Geri dönmeden yaz."** YOLO modu (silme/cut/undo kilitli) + daktilo sesi +
   istatistik çubuğu. Bu iddia *tutarlı*: mod, `EditorAction` envanteri
   üzerinden menüye, araç çubuğuna ve kısayola aynı yerden bağlanıyor; YOLO
   açıkken "replace" bile önce soruyor.
3. **"Dikkat dağıtmayan, tek ekran."** `F11`, `Ctrl+T`, kart tabanlı paneller,
   modal olmayan arama. Bu iddia *kısmen* tutarlı: tek pencere gerçekten sade,
   ama sadeleştirme "özellik yokluğu"na dönüştüğünde kullanıcı bunu "eksik"
   olarak okuyor.

### 3.2 Kime?

Kodun varsaydığı kullanıcı profili şu: **klavye ile çalışan, tek dosya üzerine
uzun süre yazan, veri kaybından korkan, İngilizce dışında bir dilde de
çalışabilen masaüstü kullanıcısı.** Bu profil tutarlı biçimde uygulanmış:
kısayollar `EditorAction`'dan üretiliyor, odak sırası `viewOrder` ile
sabitlenmiş, durum çubuğu dilin kendi saat biçimini kullanıyor.

Ama bu profil **doğrulanmamış**. Depoda kullanıcı görüşmesi, anket, indirme
sayısı hedefi, başarı ölçütü veya bir "hangi sorunu çözüyoruz" cümlesi yok.
README bir özellik kataloğu; bir kullanıcı hikâyesi değil.

### 3.3 Rekabet ve boşluk

| Rakip sınıfı | Örnek | JTypeWriter'ın durumu |
|---|---|---|
| Sade yazma araçları | FocusWriter, iA Writer | Görsel sadelik denk; odak modu/çoklu belge/dışa aktarma eksik |
| Genel editörler | Notepad++, VS Code | Veri güvenliği ve UX tutarlılığı önde; eklenti, dil desteği, arama gücü geride |
| Not alma | Obsidian, Logseq | Kasıtlı olarak rakip değil (biçim yok, bağ yok, eklenti yok) |
| Platform varsayılanı | Notepad, Gedit | Hız ve güvenlik önde; ilişkilendirme, güncelleme, imza geride |

Gerçek boşluk **"güvenli + sade + yerelleştirilmiş + tek dosya"** kesişiminde.
Bu kesişim dar ama savunulabilir. Sorun, bu kesişimin **kimseye anlatılmıyor
olması**: indirme sayfasında tek cümlelik bir vaat, bir ekran görüntüsü ve
"neden bu" paragrafı yok.

### 3.4 Talep kanıtı: en büyük boşluk

Depoda yıldız/çatal/issue hedefi, indirme ölçümü, geri bildirim kanalı veya
bir başarı tanımı yok. Bu, ürünleştirmenin **tek ölçülemez** boyutu. Diğer
her boyut en azından "iyi/kötü" diye okunabiliyor; bu boyut "bilinmiyor".

---

## 4. Kullanıcı yolculuğu ve sürtünme noktaları

Yolculuk, koddan ve betiklerden izlenebildiği kadarıyla:

| Adım | Bugünkü deneyim | Sürtünme |
|---|---|---|
| Keşif | GitHub Releases sayfası | Ekran görüntüsü/tek cümle vaat yok; README geliştirici odaklı |
| İndirme | `.deb` / `.msi` / `.tar.gz` + `SHA256SUMS` | Dosya seçimi belirsiz ("hangisini alacağım?"); imza yok |
| Kurulum (Linux) | `dpkg -i`, menü girdisi | Depo yok; her sürümde elle indirme; bağımlılık denetimi jpackage'a bağlı |
| Kurulum (Windows) | `.msi`, SmartScreen | Yayıncı doğrulanmamış → "Bilinen yayıncı değil" ekranı |
| İlk açılış | Karşılama kartı, varsayılan Dark tema | İyi; ama "neyi neden yapıyor" turu yok, sesin açık/kapalı olduğu belirsiz |
| Günlük kullanım | Tek belge, otomatik kurtarma, istatistik | Çoklu belge, dışa aktarma, yazım denetimi yok |
| Dosya açma | Menü, sürükle-bırak, komut satırı | İkinci örnek çalışırken **devir yok**: dosya açılmıyor, uyarı çıkıyor |
| Çıkış | Kaydet/vazgeç/iptal + taslak parkı | Doğru; ama "taslak nerede" bilgisi yalnızca README'de |
| Kaldırma | Paket kaldırılır, `~/.jtypewriter` kalır | Belgelenmiş; yine de "verilerimi nasıl silerim" uygulama içinde yok |

En can sıkıcı iki nokta **keşif** ve **ikinci örnek devri**. İlki pazarlama,
ikincisi mimari: `InstanceLock` ikinci süreci uyarıp kapatıyor, ama zaten
çalışan pencereye "şu dosyayı aç" demiyor. Bu, dosya ilişkilendirmesi olan bir
sistemde günlük kullanımda sürekli karşılaşılan bir duvar.

---

## 5. Mimari değerlendirmesi

### 5.1 Ölçülen şekil

```
AppGraph (kompozisyon kökü, 206 satır, public final alanlar = bağımlılık sırası)
 ├─ service/     FileService, AutoSaveService, RecoveryStore, TextCodec,
 │               BackupKeeper, RecoveryRetention, StorageFootprint, ExitDisposition,
 │               DialogService (tek JavaFX sızan servis)
 ├─ ui/          MainStage, MainController, MainMenu, KeyboardShortcutHandler,
 │               FindPanel/FindModel/FindScanService, OverlayCard + kartlar,
 │               EditorArea, DropHandler, CrashHandler
 ├─ ui/component ToolbarView, ToolbarFit, StatsBar, EditorKeyHandler
 ├─ ui/theme     ThemeManager, FontSizeManager, TextColorManager, Contrast
 ├─ stats/       SessionStats, TextChange
 ├─ locale/      LocaleManager, Messages, Language
 └─ kök         AppSettings, AtomicFiles, IoExecutor, InstanceLock, AppLogging,
                StartupArguments, AppDirs, AppVersion
```

`service/` JavaFX'ten büyük ölçüde arındırılmış; tek istisna `DialogService`
ve bu istisna *bilinçli*: `FileChooser`/`Alert` oradan geçiyor, testler onu
mock'luyor. Bu, "temiz mimari" diye anlatılan şeyin gerçekten uygulanmış hâli.

### 5.2 Korunması gereken kültürel miras

- **Saf karar nesneleri.** `YoloPolicy`, `ToolbarFit`, `ExitDisposition`,
  `AutosaveHealth`, `DroppedFiles`, `StartupArguments`, `FindModel`,
  `StatsBarModel`, `TextCodec`, `DialogSizing`, `ShortcutRows`. Her biri
  JavaFX'siz, her biri testli. Yeni bir davranış eklerken "bunu bir saf
  fonksiyona çıkarabilir miyim?" sorusu projenin asıl mimari kuralı.
- **Tek komut envanteri.** `EditorAction` hem menüyü, hem araç çubuğunu, hem
  kısayolu, hem testleri besliyor. Bir komutun bir yüzeyde olup diğerinde
  olmaması yapısal olarak zor.
- **Tek iş parçacığı üzerinden I/O.** `IoExecutor` tek daemon thread kullanıyor
  ve bunu gerekçelendiriyor: yedek rotasyonu ile kaydetme aynı dizinde
  yarışamaz. Kilit yok, sıra var. `fxThreadTasks` sayacı, "disk işi FX
  thread'ine sızmadı" iddiasını **ölçülebilir** kılıyor — bu, projenin en
  zekice test kurgularından biri.
- **Javadoc bir tasarım aracı.** Sınıf başlıkları "ne" değil "neden" anlatıyor;
  `FileService`'in 70 satırlık sınıf javadoc'u, veri güvenliği politikasının
  tek okunur kaynağı. Bu, kod ile belge arasındaki en sağlıklı ilişki biçimi.

### 5.3 Yapısal borç

| Borç | Ölçü | Etki | Not |
|---|---|---|---|
| `FileService` | 1.192 satır, 30+ public metot | Yüksek | Aç/kaydet/yeni/kurtar/kodlama/dış değişiklik tek sınıfta |
| `RecoveryStore` | 757 satır | Orta | Taslak biçimi + tarama + saklama + silme bir arada |
| `ToolbarView` | 767 satır | Orta | Altı açılır liste + düğmeler + odak + tema bağlama |
| `AppSettings` | 739 satır | Orta | Anahtar erişimi + şema göçü + geometri + bildirim |
| `Messages` | 822 satır, 156 accessor | Düşük | Mekanik; ama her yeni dizgi 6 dosya + 1 metot |
| `FindPanel` | 502 satır | Düşük | Görsel + model karışımı sınırda |
| `fxml/main.fxml` | Boş bir `BorderPane` | Düşük | FXML bağımlılığı ve `controllerFactory` altyapısı, taşıdığı hiçbir düzen olmadan duruyor |

Bunlar "yanlış" değil; tek kişilik bir projede **bilinçli bir ödünç**. Yine de
`FileService`'in bölünmesi (okuma / yazma / kodlama / dış değişiklik) aynı
davranışı koruyarak yapılabilir ve yeni katkıcı için giriş eşiğini düşürür.

### 5.4 Komut katmanının sınırı

`EditorAction` bir komut *envanteri*, bir komut *katmanı* değil: her eylem
`MainController` içinde bir metoda, oradan da bir servise gidiyor. Bu, 15–20
komut için doğru; ama dışa aktarma, yazdırma, yazım denetimi gibi yeni
yüzeyler geldiğinde `MainController` (349 satır) büyüyecek. Komut nesnesi
(execute + enable + kısayol + etiket) soyutlaması bugün kurulmadığı için,
her yeni komut dört yerde (menü, araç çubuğu, kısayol, test) dokunma
gerektiriyor — gerçi bunların üçü üretiliyor, yani maliyet düşük.

### 5.5 Genişleme noktaları

Bugün **yok**: eklenti API'si, tema eklentisi, biçim sağlayıcı, sözdizimi
vurgulama, komut paleti. Tema/CSS üç dosyaya sabit; yeni tema kod değişikliği
istiyor (`themeManager` + `base.css` tokenları). Bu, ürünün sade kalmasını
sağlıyor; ama "kullanıcı kendi temasını yazar" beklentisi olan bir kitleye
kapı kapalı.

---

## 6. Veri güvenliği: kanıt matrisi

Bu bölüm, ürünün en güçlü tarafı olduğu için ayrıntılıdır.

| Garanti | Mekanizma | Kanıt | Kalan risk |
|---|---|---|---|
| Kısmi yazma yok | `jtw-*.tmp` + fsync + `ATOMIC_MOVE`; desteklenmezse düz move | `AtomicFilesTest`, `FileServiceTest` | Ağ dosya sisteminde fsync semantiği satıcıya bağlı |
| Güç kesintisi sonrası dizin tutarlılığı | Dizin tanıtıcısı fsync | `AtomicFilesTest` (POSIX) | Windows'ta dizin fsync yok; NTFS'e güveniliyor |
| Eski sürüm kaybı yok | `.bak`, `.bak.1`, `.bak.2` rotasyonu | `BackupKeeperTest` | Yedek de belge metni taşır → gizlilik yüzeyi |
| İzin korunması | Mevcut `PosixFilePermissions` devralınır | `AtomicFilesTest` | Windows ACL'leri POSIX modeline eşlenemez |
| Symlink korunması | Hedef çözülür, link yeniden yazılmaz | Testlerde ölçülmüş (önceki bulgu D2 kapandı) | — |
| Kodlama sabitliği | BOM önceliği, sıkı UTF-8, UTF-16 parite sezgisi, Windows-1254/UTF-8 sorusu | `EncodingMatrixTest` (359 satır, 6 kodlama × satır sonu × emoji) | Sezgisel UTF-16 yanlış pozitif üretebilir; kullanıcı sorusu yedek |
| Kayıpsız `?` yok | Eşlenemeyen karakterde UTF-8'e geçiş teklifi | `FileServiceTest` | Kullanıcı teklifi reddederse kaydetme başarısız olur (doğru davranış) |
| Dış değişiklik kaybı yok | Yükleme/kaydetme sonrası içerik SHA'sı; uyuşmazsa sor | `FileServiceTest` | Zaman damgası değil hash → doğru; ama dosya *açıkken* izlenmiyor |
| Çökme sonrası metin | 30 sn + odak kaybında `draft-<uuid>.jtw` | `AutoSaveServiceTest`, `ProcessCrashRecoveryTest` (gerçek JVM öldürme) | 30 sn'lik pencere; son saniyeler kaybolabilir |
| Temiz çıkışta veri kaybı yok | `ExitDisposition`: yalnız yanıtlanmış kapatma `CLEAR`; diğer her yol `PARK` | `ExitDispositionTest`, `AutoSaveServiceTest` | Park edilemezse slot donar (bilinçli) |
| Bayat taslak belgeyi geri almaz | Taslak belgeden eskiyse yolsuz geri yükleme → Save As zorunlu | `AutoSaveServiceTest` | — |
| Taslak taraması sınırlı | Symlink takip edilmez, >64 MiB atlanır, park edilenler 30 gün / 10 dosya / 64 MiB | `RecoveryStoreTest`, `RecoveryRetentionTest` | Aktif taslak hiç expire edilmez (başka örnek için doğru) |
| Disk dolması | Kaydetme öncesi kullanılabilir alan ölçümü + `Error` yolunda bildirim | `FileServiceTest` (enjekte edilebilir `LongSupplier`) | Ölçüm ile yazma arasındaki yarış |
| Ayar bozulması | `settings.properties.corrupt` olarak kenara alınır | `AppSettingsTest` | — |
| Tek örnek | `instance.lock` advisory kilit | `InstanceLockTest` | Devir yok; ağ ev dizininde kilit semantiği zayıf |

**Hüküm:** Veri güvenliği iddiası, bu projede **kanıtlanmış** tek iddiadır.
Eksik olan, bu kanıtın kullanıcıya *anlatılması* ve belge açıkken dış
değişikliğin izlenmesi (bugün yalnızca kaydetme anında yakalanıyor).

### 6.1 Gizlilik yüzeyi (dürüst envanter)

- Taslaklar **şifresiz** ve kullanıcı ev dizininde; README bunu açıkça söylüyor.
- Her belge için 4× kalıcı, ~6× geçici disk kullanımı var; ilk kaydetmede bir
  kez uyarılıyor.
- `.bak*` dosyaları belge silindikten sonra da kalır; "belgeyi sil" işlemi
  metni tamamen silmez.
- Loglar dosya yollarını ve tam yığın izlerini taşır.
- Telemetri, güncelleme denetimi, ağ isteği **yok**; tek dışa çıkış About
  kartındaki `mailto:`. Bu, GDPR/veri işleme açısından en kolay konumdur ve
  pazarlanabilir bir özelliktir — ama pazarlanmıyor.

---

## 7. Deneyim ve erişilebilirlik

### 7.1 Güçlü yanlar

- **Araç çubuğu hiç sarmıyor.** `ToolbarFit` gerçek yerelleştirilmiş etiketlerle
  ölçüyor; pencere minimum genişliği altı paketten türetiliyor ve bir çeviri
  etiketi büyütürse test kırılıyor. Bu, "taşma" sınıfı hatalarını yapısal olarak
  ortadan kaldırıyor.
- **Odak sırası sabit ve belgelenmiş.** `viewOrder` ile 13 denetim; her biri
  görünür odak halkası + kısayolu içeren ipucu taşıyor.
- **Karşıtlık ölçülüyor.** `ContrastRatioTest` (452 satır) üç temada WCAG AA
  eşiğini pinliyor; düzenleyici yazı boyutu 10–48 px aralığında, durum çubuğu
  üç kademeli (küçük/orta/büyük) ve varsayılan 13 px.
- **Modal olmayan kartlar.** Arama, karşılama, son dosyalar, hakkında aynı
  köşede; belge kullanılabilir kalıyor; Escape/Enter kapatıyor.
- **Klavye her yerde.** Menü, `F3`, `Ctrl+H`, `Ctrl+T`, `F11`, `Ctrl+Shift+T`.

### 7.2 Eksikler

| Eksen | Durum | Etki |
|---|---|---|
| Ekran okuyucu | Yalnız `AccessibleText`; rol/ilişki tanımları sınırlı | Orta — "erişilebilir" demek için yeterli değil |
| Yüksek karşıtlık / sistem teması | Üç sabit tema; OS yüksek karşıtlık modu izlenmiyor | Orta |
| RTL diller | Yok (Arapça/İbranice arayüz ve metin yönü) | Düşük (hedef kitle) |
| HiDPI / ölçekleme | JavaFX varsayılanı; görsel regresyon testi yok | Düşük |
| Metin içi gezinme | Satır numarası, gidin-satıra, MiniMap yok | Orta — uzun belgede kaybolma |
| Yazım denetimi | Yok | Orta — "editör" iddiası için beklenir |
| Dışa aktarma | Yok (PDF/HTML/ODT) | Orta — "yazdığımı paylaşamıyorum" |
| Yazdırma | Yok | Düşük/Orta |
| Çoklu belge | Yok | Yüksek — günlük kullanım tavanı |
| Oturum geri yükleme | Yalnız son belge | Orta |
| Markdown | Yalnız `.txt`/`.md` açılıyor; önizleme/vurgulama yok | Düşük (bilinçli sade) |

Bu eksiklerin çoğu **bilinçli** olabilir; sorun, hangisinin bilinçli olduğunun
yazılı olmaması. Bir sonraki fazın ilk işi "yapmayacağız" listesini yazmak
olmalı; aksi hâlde her eksik bir "ihmal" gibi okunur.

---

## 8. Yerelleştirme

- **Yapı:** `messages.properties` + 5 çeviri, her biri 156 anahtar; `Messages`
  accessor'ları; `LocaleManager` değişiklik dinleyicileri; sistem yereline
  düşmeyi bilinçli olarak kapatan `Locale.ROOT` araması (Türk
  noktasız-i tuzağına karşı).
- **Testler:** `MessagesTest` anahtar kümesi eşitliğini, ölü anahtarı ve
  erişimci eşleşmesini zorluyor; `AppSettingsTest` yerel ayarı kaydedip
  geri yüklüyor; CI `tr-TR` ve `de-DE` sistem yerellerinde tüm paketi tekrar
  koşuyor. Bu, çoğu ticari projede olmayan bir disiplin.
- **Tarih/saat:** Durum çubuğu ve "kaydedildi" damgası dilin kendi biçimini
  kullanıyor; sabit desen yok.
- **Eksikler:**
  - Çoğul kuralları yok (`MessageFormat` choice/plural kullanılmıyor).
  - RTL yok.
  - Çevirilerin anadil gözden geçirmesi yok; `MessagesTest` *varlığı* ölçüyor,
    *doğruluğu* değil.
  - Arayüz yazı tipi tek aile (DejaVu Sans Condensed); CJK/Yunan/Kiril
    kullanıcı metni sistem yazı tipine düşer. Arayüz için sorun değil, ama
    "her dilde aynı görünüm" iddiası yalnız altı dil için geçerli.
  - Çeviri güncelleme akışı (yeni anahtar → çevirmen) tanımlı değil.

---

## 9. Paketleme ve dağıtım

### 9.1 Paketleme hattı (güçlü)

`scripts/package.sh` (620 satır) üç parçalı bir görüntü kuruyor:

1. **İnce JAR** (uygulama),
2. **jlink çalışma zamanı** — ölçülmüş 5 modül; `jdk.compiler` yoksa hata,
3. **JavaFX modülleri** `lib/javafx` altında **gömülü** (jlink'e verilmiyor,
   çünkü jlink modüler JAR'ın kökündeki `.so` dosyalarını taşımıyor — bu
   gerçekten yaşanmış bir hata ve betikte gerekçesiyle yazılı).

Launcher `--module-path`'i `$APPDIR`'a göre yazıyor, `$ROOTDIR`'a göre değil;
çünkü kurulu `.deb`'de `$ROOTDIR` boşalıyor ve uygulama "Module
javafx.controls not found" ile ölüyor. Bu tür bir hatanın **kurulum sonrası
duman testiyle** yakalanması, hattın olgunluğunu gösteriyor.

Doğrulama katmanı üç ayrı betik:

| Betik | Ne kanıtlıyor |
|---|---|
| `verify-artifacts.sh` (321) | Depo yükü ↔ uygulama görüntüsü ↔ `.deb` yükü bayt eşitliği; düzen varsayımları (runtime, `lib/javafx`, `jdk.compiler` yokluğu) |
| `smoke-appimage.sh` (483) | Xvfb altında gerçek başlatma + `xwininfo`; root/sudo varsa `.deb` kur/başlat/kaldır; yoksa `dpkg` sahtesiyle launcher'ın kurulu-paket dalı |
| `generate-icons.py` (413) | `--check` manifest hash karşılaştırması; `--render` makine-yerel piksel karşılaştırması |

Bu üçlü, "paket çalışıyor mu" sorusunu **insan gözü olmadan** yanıtlıyor.
Açık kaynak JavaFX masaüstü projelerinde bu seviye nadirdir.

### 9.2 Dağıtım (zayıf)

| Eksen | Durum |
|---|---|
| Kanallar | Yalnız GitHub Releases (iki `.tar.gz` görüntü, `.deb`, `.msi`, kaynak arşivi, `SHA256SUMS`) |
| Paket depoları | apt/PPA, winget, Chocolatey, Scoop, Flatpak, AppImage, Snap, rpm: **yok** |
| İmzalama | Authenticode **yok**, GPG ile `SHA256SUMS` imzası **yok** |
| Otomatik güncelleme | **Yok** (bilinçli olabilir; ama kullanıcı her sürümü elle indirir) |
| Delta güncelleme | Yok |
| ARM64 | Yok (Linux ve Windows ARM) |
| macOS | Bilinçli olarak kaldırıldı |
| İndirme sayfası | GitHub Release gövdesi; sürümden bağımsız tanıtım sayfası yok |
| Kurumsal dağıtım | MSI parametreleri/SCCM, `.deb` depo imzası belgelenmemiş |

Sonuç: Ürün **kurulabilir**, ama **yaygın dağıtılabilir** değil. İmzasız
Windows ikilisi, kurumsal bir kullanıcı için kurulum engeli; Linux'ta `.deb`
dosyasını çift tıklayan kullanıcı için "güvenilmez" uyarısı. Bu, teknik bir
eksik değil, bir **dağıtım stratejisi** eksikliğidir.

---

## 10. CI/CD ve test stratejisi

### 10.1 Hat

- **Tek yeniden kullanılabilir iş akışı** (`verify-build.yml`, `workflow_call`).
  `maven.yml` ve `release.yml` onu çağırıyor; `CiGatesTest` her iki çağıranı,
  yayın çağrısının boş `mvn`/`package.sh` listesini ve aşamalandırmanın verify
  işinin içinde kalmasını metin olarak pinliyor. "İki liste sürüklenir" hatası
  yapısal olarak kapatılmış.
- **Eylemler tam SHA'ya sabit** + Dependabot grubu. Tedarik zinciri hijyenı
  iyi.
- **Matris:** Ubuntu + Windows; JDK 25 Temurin; `mvn -B package`; ardından
  `package.sh --skip-build --installer`; Linux'ta `verify-artifacts.sh`;
  ayrı `smoke` işi iki platformda.
- **Yerel matrisi:** testler `tr-TR` ve `de-DE` sistem yerellerinde tekrar
  koşuyor; rapor dizinleri ayrı ayrı saklanıyor (badge toplamı için).
- **Kapılar:** ikon hash kontrolü (ikisi de leg), bayt karşılaştırma (Linux),
  duman başlatma (iki leg), yayın öncesi beş varlık iddiası.
- **Yayın:** etiket ↔ `pom.xml` sürümü eşleşmesi, kuru çalıştırma varsayılanı,
  manuel çalıştırmada yayın reddi, `prerelease` bayrağının kaldırılması ve
  bunun testle pinlenmesi. Bu, sürüm disiplininin nadir bir örneği.

### 10.2 Test piramidi

| Katman | Örnek | Not |
|---|---|---|
| Saf model / birim | `FindModelTest`, `ToolbarFitTest`, `YoloPolicyTest`, `StatsBarModelTest` | Piramidin tabanı; JavaFX yok |
| Servis / dosya sistemi | `FileServiceTest` (1.033), `AtomicFilesTest`, `RecoveryStoreTest` (698) | `@TempDir`, gerçek dosya |
| Kodlama matrisi | `EncodingMatrixTest` (359) | 6 kodlama × satır sonu × emoji |
| Çökme | `ProcessCrashRecoveryTest` | Linux-only, alt JVM `destroyForcibly()` |
| Sözleşme / değişmez | `MessagesTest`, `LicenseHeaderTest`, `CiGatesTest`, `ReleaseNotesTest`, `PackagingNoticesTest`, `ContrastRatioTest` | Kod dışı değişmezleri pinliyor |
| Performans | `StatsPerfHarnessTest` | 10 MB'ta tek yenileme <50 ms (CI) |
| Duman | `smoke-appimage.sh` | Gerçek süreç başlatma |

### 10.3 Sınırlar

- **Kapsam ölçümü yok** (JaCoCo yok). Hangi sınıfın testsiz olduğu ancak elle
  bulunur.
- **UI davranışı toolkit'siz test ediliyor.** Bu bilinçli ve doğru (testler
  ekransız koşuyor), ama sonuç: gerçek render, gerçek odak halkası, gerçek
  tema geçişi, gerçek HiDPI **hiçbir testte görülmüyor**. `ContrastRatioTest`
  renkleri hesaplıyor, ekranı değil.
- **SBOM yok**, SAST/CodeQL yok, bağımlılık zafiyet taraması yok.
- **İmzalı commit / imzalı etiket** yok.
- **Yeniden üretilebilirlik** iddiası var (sabit zaman damgası) ama CI'da
  iki kez derleyip karşılaştıran bir iş yok; iddia test edilmiyor.
- **Windows'a özgü yollar** (sürücü harfi, UNC, `%APPDATA%`, uzun yol) test
  paketinde zayıf temsil ediliyor; CI Windows'ta koşuyor ama depo tarafındaki
  yollar POSIX varsayımıyla yazılmış olabilir.
- **Fuzz/property-based test** yok; `TextCodec` ve `RecoveryStore` ayrıştırıcıları
  bozuk girdiye karşı yalnız örnek tabanlı testlerle korunuyor.

---

## 11. Lisans, uyum ve yasal

- **Proje lisansı:** Apache-2.0, `pom.xml`, `LICENSE`, kaynak başlıkları ve
  `LicenseHeaderTest` ile tutarlı. Patent hibesi var; kurumsal kullanım için
  en kolay lisans.
- **Gömülü bileşenler:** JavaFX 26.0.1 (GPL-2.0-only + Classpath Exception),
  GStreamer Lite 1.26.5 (LGPL-2.1-or-later), DejaVu Sans Condensed
  (Bitstream Vera), ses örneği. `THIRD-PARTY-NOTICES` gerçek SPDX kimliklerini
  kullanıyor, LF-only, ≤100 sütun, sıfır genişlikli boşluk yok ve
  `target/classes/META-INF` kopyasıyla bayt-eşit — bu son madde
  `PackagingNoticesTest` ile pinli.
- **Lisans metinleri üç yerde:** `legal/`, uygulama görüntüsü kökü, fat JAR
  içinde `META-INF/licenses/`. `verify-artifacts.sh` bunların varlığını
  zorluyor.
- **Eksikler:**
  - **SBOM yok** (CycloneDX/SPDX). Maven Central tarzı araçlar lisansı
    okuyabiliyor, ama bileşen envanteri makine-okunur değil.
  - **GPL kaynak teklifi** yalnız `THIRD-PARTY-NOTICES` içinde; GitHub Release
    gövdesinde görünür bir "kaynak kodu şurada" bağlantısı yok. JavaFX'in
    Classpath Exception'ı bu yükümlülüğü hafifletiyor, ama GStreamer LGPL
    için **yeniden bağlanabilirlik** (kullanıcının `.so`'yu değiştirebilmesi)
    açıkça sağlanmalı; JAR içine gömülü `.so` bu tartışmada zayıf nokta.
  - Ticari marka tescili, üçüncü taraf marka bildirimi yok.
  - Katkı lisansı (CLA/DCO) politikası yazılı değil.

---

## 12. Desteklenebilirlik ve operasyon

- **İyi:** üç nesil dönen log (1 MB × 3), tam yığın izi, çökme iletişim
  kutusu, bozuk ayar dosyasını kenara alma, "log oluşturulamazsa konsolla
  başla" yolu. Kullanıcı bir hata bildirdiğinde okunacak bir iz var.
- **Zayıf:** telemetri/çökme raporu yok (bilinçli ve savunulabilir), ama
  *opt-in* bir "tanılama paketi dışa aktar" düğmesi de yok. Kullanıcıdan log
  istemek, tek kişilik bir projede destek yükünü doğrudan geliştiriciye
  yığıyor.
- **Süreç:** sorun/PR şablonu yok, `CONTRIBUTING.md` yok (README'de bir
  paragraf), sürüm notu şablonu iki dilli ve testle pinli (iyi), geri bildirim
  kanalı yalnız e-posta.
- **Otobüs faktörü 1.** 217 commit'in tamamı tek yazar. Bu, kod kalitesiyle
  telafi edilemeyecek bir süreklilik riski.

---

## 13. Risk kaydı

| # | Risk | Olasılık | Etki | Mevcut azaltma | Açık |
|---|---|---|---|---|---|
| R1 | İmzasız Windows ikilisi kurulumu engelliyor | Yüksek | Yüksek | README'de SHA256 | Authenticode + itibar |
| R2 | Kullanıcı kitlesi doğrulanmamış; ürün "kimse için" olabilir | Orta | Yüksek | Güçlü niş tez | Görüşme/ölçüm |
| R3 | Tek geliştirici projeyi bırakırsa bakım durur | Orta | Yüksek | Yoğun javadoc + test | Katkı yolu, şablonlar |
| R4 | Tek belge tavanı kullanıcıyı başka araca iter | Yüksek | Orta | Hızlı aç/kaydet | Sekme veya oturum |
| R5 | JavaFX 26 güncellemesi ambalajı kırar | Orta | Orta | Bayt karşılaştırma + duman | Sürüm yükseltme tatbikatı |
| R6 | GStreamer LGPL yeniden bağlanabilirlik itirazı | Düşük | Orta | Notices + legal | Sesin opsiyonel paketlenmesi |
| R7 | `FileService` büyümesi hata yüzeyini artırır | Orta | Orta | Test yoğunluğu | Sınıf bölme |
| R8 | Dış değişiklik yalnız kaydetmede yakalanıyor | Orta | Orta | Hash kontrolü | Dosya izleme |
| R9 | Kurtarma taslağı şifresiz | Düşük | Orta | Belgelenmiş | İsteğe bağlı şifreleme |
| R10 | Paket boyutu/güncelleme yok → kullanıcı eski sürümde kalır | Yüksek | Orta | Sürüm notları | Güncelleme denetimi (opt-in) |

---

## 14. Ürünleştirme boşlukları — aday havuzu

Aşağıdaki liste **sıralı bir plan değildir**; yalnızca analizden çıkan
boşlukların, etki ve gerekçeyle birlikte envanteridir. Hangi işin önce
yapılacağı, hangisinin bilinçli olarak yapılmayacağı bir sonraki belgenin
kararıdır.

### 14.1 Güven ve yayın kanıtı

| Boşluk | Neden | Kanıt eksikliği |
|---|---|---|
| Windows Authenticode imzası | SmartScreen ilk teması öldürüyor | İmzalı MSI üzerinde duman testi |
| `SHA256SUMS` GPG imzası | Linux'ta indirme bütünlüğü | İmza doğrulama adımı |
| Yayın sayfasında görünür kaynak teklifi + SBOM | GPL/LGPL uyumu ve kurumsal ön koşul | CycloneDX üretimi |
| Yeniden üretilebilirlik kanıtı | Sabit zaman damgası iddiası test edilmiyor | İki derleme karşılaştırması |
| Kapsam raporu | Testsiz sınıflar görünmüyor | JaCoCo + eşik |

### 14.2 Erişim ve dağıtım

| Boşluk | Neden |
|---|---|
| winget / Chocolatey / Scoop manifestosu | Windows'ta tek komutla kurulum |
| apt deposu (veya PPA) / Flatpak | Linux'ta güncelleme ve güven |
| Opt-in güncelleme denetimi | Kullanıcı eski sürümde kalıyor |
| ARM64 yapıları | Windows-on-ARM ve ARM sunucu/VM |
| Tanıtım sayfası + ekran görüntüleri | Keşif sorunu |
| Dosya ilişkilendirmesi ve "mevcut örneğe devir" | Günlük kullanım sürtünmesi |

### 14.3 Ürün derinliği

| Boşluk | Neden | Karar gerekiyor |
|---|---|---|
| Çoklu belge (sekme veya oturum) | Tek belge tavanı | Sadelik iddiasıyla çelişir mi? |
| Dışa aktarma (PDF/HTML) | "Yazdığımı paylaşamıyorum" | Editör mü, yazma aracı mı? |
| Yazım denetimi | Editör beklentisi | Yerelleştirme ile birleşir |
| Satır numarası / gidin-satıra | Uzun belge | Sadelik |
| Dosya izleme (dış değişiklik) | Belge açıkken çakışma | Veri güvenliği |
| Markdown önizleme/vurgulama | `.md` açılıyor ama render yok | Kapsam kayması riski |

### 14.4 Süreç ve sürdürülebilirlik

| Boşluk | Neden |
|---|---|
| `CONTRIBUTING.md` + sorun/PR şablonları | Otobüs faktörü |
| Çeviri gözden geçirme akışı | 6 dilin kalitesi ölçülmüyor |
| Opt-in tanılama paketi | Destek yükü |
| Fuzz testleri (`TextCodec`, `RecoveryStore`) | Bozuk girdi yüzeyi |
| Windows yol/Unicode test matrisi | Platform iddiası |
| Sürüm yükseltme tatbikatı (JavaFX/JDK) | Ambalaj kırılganlığı |

---

## 15. Ölçümleri yeniden üretme

```bash
# sayımlar
find src/main/java -name '*.java' | wc -l
find src/main/java -name '*.java' -print0 | xargs -0 cat | wc -l
find src/test/java -name '*.java' | wc -l
grep -rn '@Test' src/test/java | wc -l
for f in src/main/resources/messages*.properties; do grep -c '^[a-zA-Z]' "$f"; done

# kalite hattı
mvn -B clean test
mvn -B test -DargLine="-Duser.language=tr -Duser.country=TR"

# ambalaj ve kanıt
./scripts/package.sh --skip-build --installer
./scripts/verify-artifacts.sh --skip-build
./scripts/smoke-appimage.sh --skip-build
python3 scripts/generate-icons.py --check
```

Not: `mvn` komutları depo kökünden çalıştırılmalı; birkaç test `src/`,
`docs/` ve `.github/` yollarını göreli okuyor.

---

## 16. Sonuç

JTypeWriter'ın mühendislik tarafı, ürün tarafının önünde. Veri güvenliği,
paketleme kanıtı ve test disiplini, bu projeyi "hobi projesi" kategorisinden
çıkarıyor; tek belge mimarisi, imzasız dağıtım ve doğrulanmamış kitle ise onu
"ürün" kategorisine sokmuyor. Kapatılması gereken boşluk teknik değil:
**görünürlük, güven ve kapsam kararı.** Bir sonraki adım, hangi özelliklerin
bilinçli olarak *yapılmayacağını* yazmak ve yayın kanıtını (imza, SBOM,
tanıtım, güncelleme) ürünün geri kalanıyla aynı ciddiyetle ele almaktır.

---

*Bu belge yalnızca analizdir. P0–P3 fazlarına yapılan atıflar bağlam içindir;
burada tanımlanan hiçbir bulgu, sıralama veya kapı bir yol haritası taahhüdü
değildir.*
