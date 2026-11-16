# JTypeWriter — Sıfırdan Ürünleştirme Analizi

> **Sürüm:** 1.1.4 (`pom.xml:9`, etiket `v1.1.4` = `89bfc90`) · **Dal:** `main` · **Tarih:** 16 Kasım 2026
> **Kapsam:** Ürün tezi, mimari, veri güvenliği, kullanıcı deneyimi, yerelleştirme, ambalaj,
> dağıtım, kalite hattı, lisans ve desteklenebilirlik — bu depodaki kod ve betikler üzerinden
> yeniden ölçülmüş hâliyle.
> **Bu belge bir yol haritası değildir.** İş sırası, kart kimliği, takvim veya çıkış kapısı
> tanımlamaz. §14'teki boşluk envanteri sonraki kararın *girdisi*dir, kararın kendisi değil.

---

## 0. Belgenin sözleşmesi

Bu analiz, projenin daha önce geçirdiği ürünleştirme fazlarının (P0–P3) belgeleri üzerine
**kurulmaz**. O belgeler silinmiştir; geriye yalnızca ulaşılan durum kalmıştır: sürüm
numarası, test sayısı, paketleme hattı, kapanmış davranışlar ve kod. Bu belge o durumu
**kendi ölçümleriyle** yeniden okur. P0–P3'e yapılan atıflar yalnızca "buraya nasıl
gelindi" bağlamıdır; hiçbir bulgunun gerekçesi değildir, hiçbir önerinin dayanağı değildir.

Kanıt seviyeleri:

| Seviye | Anlamı | Bu belgedeki kullanımı |
|---|---|---|
| **Ö** | Bu makinede ölçüldü; komut ve çıktı §15'te tekrar üretilebilir | Satır/sınıf/test sayıları, dosya boyutları, süreler |
| **K** | Koddan doğrudan okundu (davranış, sabit, eşik) | Güvenlik matrisi, ayar anahtarları, akışlar |
| **T** | Bir testin pinlediği değişmez | Ambalaj/uyum/i18n iddiaları |
| **V** | Çıkarım — doğrulanması gereken yargı | Rekabet, talep, risk olasılığı |

Bir bulgu **V** ise yanına yazılmıştır. Aksi hâlde Ö/K/T'den biridir. Bu ayrım, belgenin
en önemli disiplinidir: ürün hakkındaki yargılar ölçüm gibi sunulmaz.

---

## 1. Yönetici özeti

### 1.1 Tek paragraf

JTypeWriter, tek bir düz metin belgesini açıp kullanıcının yolundan çekilen, JavaFX 26 /
Java 25 ile yazılmış, kendi Java çalışma ortamını taşıyan bir masaüstü yazı aracıdır.
Mühendislik tarafı **olağanüstü disiplinli**: veri güvenliği (atomik yazma, üç kuşak yedek,
kurtarma taslağı, kodlama sadakati), saf/test edilebilir model kültürü, 683 testlik duman
hızında bir süit ve bayt karşılaştırmalı ambalaj doğrulaması var. Ürün tarafı ise **ölçülebilir
biçimde zayıf**: tek dağıtım kanalı (imzasız GitHub Releases), sıfır keşfedilebilirlik,
kullanıcıya görünür hiçbir ilerleme göstergesi olmayan bir kaydetme modeli, zayıf erişilebilirlik
yüzeyi ve tek kişilik sürdürülebilirlik. Yani tablo, "çok iyi yapılmış ama duyulmamış ürün"ün
kendisidir — ve bu bir mühendislik eksiği değil, ürünleştirme eksiğidir.

### 1.2 En önemli on bulgu

1. **Veri güvenliği sınıfının üstünde.** Atomik yazma, fsync, öznitelik koruma, boş alan
   ön kontrolü, üç kuşak yedek, kodlama geri yazımı, dış değişiklik parmak izi ve kurtarma
   taslağı birlikte var (`AtomicFiles.java:142-166`, `BackupKeeper.java:35`,
   `FileService.java:1268-1294`, `RecoveryStore.java:262-269`). Bu, çoğu ticari metin
   editöründe bile eksik olan bir kümedir. (§6)
2. **Ama güvenlik iddiası ile kullanıcıya görünen güvenlik arasında boşluk var.** Yedek
   yazılamazsa kaydetme **yine de başarılı sayılır** (`FileService.java:919-922`); dizin
   fsync'i başarısız olsa da "kaydedildi" denir (`AtomicFiles.java:352-360`). Dürüstlük
   testte var, arayüzde yok.
3. **Tek belge varsayımı koda gömülü.** `AppGraph` bileşenleri tek `FileService` durumu,
   tek kurtarma yuvası ve tek `MainStage` üzerine kuruludur (`RecoveryStore.java:262-269`,
   `AutoSaveService.java:69`). Sekme/çoklu pencere, sıfırdan bir durum katmanı demektir —
   küçük bir özellik değil. (§5.4)
4. **Kaydetme görünmezliği ürünün en somut sürtünmesi.** 30 saniyelik otomatik kaydetme
   penceresi var ama kullanıcıya "ne zaman kaydedildi" bilgisi verilmez; durum çubuğu
   yalnızca kaydetme *sürerken* yazar (`MainController.java:161-163`). Bir yazma aracında
   güven, görünür geri bildirimden gelir. (§4, §7)
5. **Dağıtım tek kanal, imzasız, güncellemesiz.** Yalnızca GitHub Releases: `.deb`, `.msi`,
   taşınabilir `.tar.gz`, `SHA256SUMS` (`README.md:265-270`). Depo, winget/scoop,
   Flatpak/AppImage, mağaza, kod imzalama ve otomatik güncelleme yok. İmzasız `.msi`
   Windows'ta SmartScreen ile karşılaşır. (§9)
6. **Ambalaj doğrulaması ürünün en güçlü tarafı.** `verify-artifacts.sh` depo ↔ imaj ↔
   `.deb` yükünü bayt bayt karşılaştırır; `smoke-appimage.sh` Xvfb altında `xwininfo` ile
   gerçek pencere başlığını arar ve `.deb`'i kurup kaldırır (`smoke-appimage.sh:150-183`).
   Bu seviyede bir ambalaj kanıtı, açık kaynak masaüstü projelerinde nadirdir. (§9.1)
7. **Kalite hattı kapılı ama dar.** `mvn -B package` + `tr_TR`/`de_DE` yerel koşuları +
   imaj/`.deb` doğrulaması + duman işi var; buna karşılık SBOM, bağımlılık CVE taraması,
   kapsam eşiği, kod imzalama ve yeniden üretilebilirlik karşılaştırması yok
   (`.github/workflows/verify-build.yml`). (§10)
8. **Erişilebilirlik yüzeyi ince.** 18 `accessible*` kullanımı ve araç çubuğu dışında
   sınırlı ekran okuyucu kapsamı; karşıtlık WCAG AA eşikleriyle (metin 4.5:1, bileşen
   3.0:1) test ediliyor (`ContrastRatioTest.java:34-66`) ama RTL, hareket-azaltma ve
   arayüz yazı büyütmesi yok. (§7)
9. **Yerelleştirme mühendislik olarak örnek, süreç olarak yok.** Altı pakette 175 anahtar,
   birebir anahtar kümesi, `Messages` erişimcisi zorunluluğu (`MessagesTest`), sistem
   yereline düşmeme. Ama çeviri gözden geçirme süreci, çevirmenin bağlamı ve ham istisna
   metninin kullanıcıya ulaştığı yerler duruyor (`FileService.java:456,883,1185`). (§8)
10. **Tek kişilik sürdürülebilirlik.** 221 commit'in 215'i tek insan yazarda; 9 etiket,
   2026-07-02 → 2026-11-16. Otobüs faktörü 1. Bu, en yüksek olasılıklı **varoluşsal**
   risktir ve kodla çözülmez. (§12, §13)

### 1.3 Boyut skorları

| Boyut | Skor (1–5) | Gerekçe (kanıt) |
|---|---:|---|
| Ürün tezi ve konumlandırma | 3 | Net bir his var ("daktilo + YOLO + tek dosya"), doğrulanmış bir kitle yok (V) |
| Mimari ve kod | 4 | Katmanlar, saf modeller, elle DI kültürü güçlü; 6 dosya ≥700 satır |
| Veri güvenliği | 5 | Atomik yazma + yedek + kurtarma + kodlama + dış değişiklik tam küme |
| Deneyim ve erişilebilirlik | 3 | Klavye/odak/karşıtlık iyi; kaydetme görünürlüğü ve ekran okuyucu zayıf |
| Yerelleştirme | 4 | 6 dil × 175 anahtar, testli; gözden geçirme süreci yok |
| Ambalaj | 5 | jlink + jpackage, 2 platform, bayt karşılaştırması, duman testi |
| Dağıtım ve erişim | 2 | Tek kanal, imzasız, güncelleme yok, depo yok |
| Kalite hattı | 4 | Kapılar gerçek; SBOM/CVE/kapsam yok |
| Uyum ve lisans | 4 | Örnek niteliğinde; SBOM ve kaynak teklifi görünürlüğü eksik |
| Desteklenebilirlik | 2 | Log var, telemetri bilinçli olarak yok, sorun toplama akışı yok |

Ortalama **3,6/5**. Yüksek skorlar **mühendislik**, düşük skorlar **ürün ve pazar**
tarafında toplanıyor. Dağılımın kendisi teşhis: eksik olan yapım kalitesi değil, ürüne
dönüşme adımı.

---

## 2. Ölçüm tabanı

| Ölçüt | Değer | Seviye |
|---|---|---|
| Üretim sınıfı / satır | 79 / 16.090 | Ö |
| Test dosyası / satır / test | 70 / 14.457 / 683 | Ö |
| Test süresi (tüm süit, toplam rapor) | ~5,3 s | Ö |
| Test/satır oranı (test:üretim) | 0,90 : 1 | Ö |
| Test sınıfı dağılımı | ui 27, root 15, service 14, audio 3, locale 3, print 3, stats 2 | Ö |
| Üretim satırı dağılımı | ui 40 dosya, service 13, root 12, print 6, locale 3, audio 3, stats 2 | Ö |
| ≥400 satır dosya | 10 | Ö |
| ≥700 satır dosya | 6 | Ö |
| En büyük sınıflar | `FileService` 1325, `Messages` 898, `ToolbarView` 767, `RecoveryStore` 757, `AppSettings` 739, `DialogService` 611 | Ö |
| Mesaj anahtarı | 175 × 6 paket (birebir aynı küme) | Ö/T |
| CSS | 986 satır (base 818 + dark 55 + light 57 + sepia 56) | Ö |
| Font kaynağı | 1,7 MB, 3 yüz (Condensed Regular/Bold + Mono) | Ö |
| İkon kaynağı | 260 KB, 12 dosya + `icons.manifest` | Ö |
| İnce JAR | 1.529.679 B | Ö |
| Şişkin (fat) JAR | 12.049.336 B | Ö |
| Uygulama imajı | 73 MB (`lib/runtime` 60 MB, `lib/javafx` 11 MB, `lib/app` 1,3 MB) | Ö |
| `.deb` | 45.423.052 B (sürüm 1.1.2 — bkz. §9.3) | Ö |
| jlink modülleri | `java.base, java.desktop, java.logging, java.scripting, jdk.unsupported` | K |
| Commit / etiket | 221 / 9 (`v1.1.0`…`v1.1.4`) | Ö |
| Tarih aralığı | 2026-07-02 → 2026-11-16 | Ö |
| Yazar dağılımı (HEAD) | 221 commit: 215 `hyperpostulate`, 4 `Mesut ORMANLI`, 2 `dependabot[bot]` | Ö |
| İş parçacığı | FX + `jtw-io-1` + `jtw-find-1` | K |
| Komut (EditorAction) | 21 | K |
| Menü | 4 sayfa (Dosya/Düzen/Görünüm/Yardım); Yardım yalnızca Hakkında | K |
| Diyalog giriş noktası | 14 | K |
| Kalıcı ayar anahtarı | ~20 (şema sürümü dâhil) | K |
| Minimum pencere genişliği | 889 px (873 + 16, altı dilden türetilmiş) | T |

Bu sayılar bir "kalite notu" değil, bir **kapasite** fotoğrafıdır. 16 bin satır üretim
kodu, 14 bin satır testiyle birlikte, tek kişi tarafından dört ayda yazılmış ve
kendi Java çalışma ortamını taşıyan bir masaüstü uygulamasına dönüşmüş. Kapasitenin
nerede harcandığı §5'te, nerede harcanmadığı §9 ve §12'de görünüyor.

---

## 3. Ürün: ne satıyor, kime, nerede duruyor

### 3.1 Değer önerisi (koddan çıkan)

Kodun kendisi ürün tezini üç cümlede söylüyor ve bunlar pazarlama metni değil, davranış:

1. **Tek belge, tek dosya, biçim yok.** `README.md:21`: "opens a single plain-text document
   and then stays out of the way: no accounts, no cloud, no project format". `FileService`
   ve `AppGraph` bunu mimari olarak da dayatır (§5.4).
2. **Kullanıcının dosyasına dokunmama sözü.** Kodlama korunur, BOM korunur, satır sonları
   korunur, temsil edilemeyen karakter `?` ile değiştirilmez (`TextCodec.java:160-163`,
   `FileService.java:880-885`). Bu, "senin dosyan senin"in teknik hâli.
3. **YOLO modu.** `EditorArea` içinde, kullanıcı düzenlemelerinin hiçbirinin var olan
   karakteri silememesi kuralı (`EditorArea.java:19-32`). Bu bir özellik değil, bir
   **davranış sözleşmesi**: ileri yaz, geri silme. Niş ama gerçek bir yazma tekniğini
   (sansürsüz taslak) ürüne çeviriyor.

Bu üçü birlikte tutarlı bir kişilik oluşturuyor: **müdahale etmeyen, kaybetmeyen, tek
işlik bir yazma aracı.** Ürünün gerçek değeri burada; "daha çok özellik" burada değil.

### 3.2 Kime?

Kodun hedeflediği kullanıcı, özellik kümesinden okunabiliyor (V):

- Not/taslak/metin dosyasıyla çalışan, markdown/office biçimi istemeyen kişi.
- Klavye kısayoluyla yaşayan, fareyi nadiren kullanan kişi (21 komut, 4 menü, `F3`/`Shift+F3`).
- Daktilo sesi, tema ve YOLO gibi "hissetme" ayarlarını önemseyen kişi (`audio/`,
  `theme/`, `ToolbarState`).
- Türkçe/İngilizce/Almanca/Fransızca/İspanyolca/İtalyanca konuşan kişi.

Hedeflenmeyen: ekip işbirliği, kod düzenleme (sözdizimi yok), uzun belge yönetimi
(proje/klasör yok), çoklu belge. Bu sınır **bilinçli** ve tutarlı; ürünleştirme
tartışması bu sınırı genişletmek değil, sınırın içindeki deneyimi tamamlamak olmalı (V).

### 3.3 Rekabet ve boşluk

| Rakip sınıfı | Örnek | JTypeWriter'ın farkı | JTypeWriter'ın eksiği |
|---|---|---|---|
| Sistem editörü | Notepad, gedit, TextEdit | Veri güvenliği, kodlama sadakati, kurtarma | Kurulu geliyor; keşfedilmesi için sebep yok |
| Programcı editörü | VS Code, Sublime | Sadelik, odak, YOLO | Sözdizimi, eklenti, tema ekosistemi yok |
| Yazma uygulaması | iA Writer, Ulysses, FocusWriter | Ücretsiz, yerel, hesapsız, tek dosya | Bulut, senkron, mobil, markdown önizleme yok |
| Metin aracı | Notepad++, Kate | Modern JavaFX arayüz, tek dosya felsefesi | Sekme, makro, eklenti yok |

Boşluk şurada: **"hesapsız, bulutsuz, tek dosyaya sadık, ama veri kaybetmeyen ve
şık duran"** masaüstü yazma aracı. Bu boşluk gerçek (V), ama JTypeWriter'ın orada
olduğunu kimse bilmiyor — çünkü dağıtım ve keşfedilebilirlik sıfır (§9.2).

### 3.4 Talep kanıtı: en büyük boşluk

Depoda talep kanıtı **yok**: indirme sayısı, issue, yıldız, kullanıcı geri bildirimi,
analitik, hiçbiri ölçülmüyor (ve telemetri bilinçli olarak yok — `README` ve kod
taramasında ağ çağrısı yalnızca `mailto:`; `AboutCard.java:84`). Bu bir eksiklik değil,
bir tercih; ama sonucu şu: **ürünün değer önerisi hiçbir yerde doğrulanmamış.** Dört
aylık mühendislik, sıfır pazar geri bildirimiyle ilerlemiş (V). Ürünleştirmenin ilk
gerçek sorusu "sırada ne var" değil, "bunu kimin kullandığını nereden bileceğiz"dir.

---

## 4. Kullanıcı yolculuğu ve sürtünme noktaları

Yolculuk, koddan adım adım okunmuştur.

| Adım | Ne oluyor | Sürtünme | Kanıt |
|---|---|---|---|
| İndirme | GitHub Releases → `.deb`/`.msi`/`.tar.gz` | İmza yok; Windows SmartScreen; Linux'ta depo yok | §9 |
| Kurulum | `.deb` → `/opt/jtypewriter`, menü girdisi | `Categories=Unknown`, dosya ilişkisi yok | `package.sh`, §9.3 |
| İlk açılış | Hoş geldin kartı + kısayol kartı | Kart kapatılabilir ve bir daha kendiliğinden gelmez | `WelcomeCard`, `MainController:115-119` |
| Belge açma | `Ctrl+O` veya sürükle-bırak | Kodlama çözülemezse soru sorulur (iyi) | `DialogService:326-354` |
| Yazma | `EditorArea`, YOLO opsiyonel | Otomatik kaydetme görünmez; 30 s'lik kayıp penceresi | `AutoSaveService:69` |
| Kaydetme | `Ctrl+S`, atomik + yedek | Yedek başarısızsa sessiz devam; "kaydedildi" yanıltıcı olabilir | `FileService:919-922` |
| Dış değişiklik | Parmak izi uyuşmazsa çakışma diyaloğu | Kontrol–rename arası TOCTOU | `FileService:889,925` |
| Yazdırma / PDF | `Ctrl+P` / `Ctrl+Shift+E` | Kaydedilmemişse önce Kaydet/Vazgeç; sonra **diskten** okur | `FileService.savedDocumentAsync` |
| Çıkış | Kirli belgede Kaydet/Vazgeç/İptal | Cevapsız çıkışta taslak park edilir, sonraki açılışta teklif edilir | `ExitDisposition:55-59` |
| Kurtarma | Yalnızca **en yeni** taslak teklif edilir | 20 taslak saklanır ama 19'u görünmez | `RecoveryStore:315-320` |
| Açılışta kurtarma | Modal kurtarma diyaloğu, otomatik kaydetme bağlanmadan ve CLI argümanlarından önce | Komut satırından dosya açan kullanıcı önce diyaloğu kapatır | `FxApplication:78-85` |
| Büyük dosya | 64 MiB sert tavan | Daha büyük bir dosya hiç açılmıyor (mesaj çevrilmiş) | `FileService:115,450-452` |
| Güncelleme | Elle yeni sürüm indirme | Uygulama içi güncelleme yok; ayarlar şema sürümüne bağlı | §9.4 |

**En pahalı dört sürtünme (V):**

1. **Kaydetmenin görünmezliği.** Bir yazma uygulamasında "yazdığım yerde mi" sorusu
   sürekli arka planda çalışır. JTypeWriter bu soruyu yalnızca *kaydederken* yanıtlıyor
   (`MainController:161-163`); otomatik kaydetmenin başarısı hiçbir yerde görünmüyor.
   Tek satırlık bir durum göstergesi, algılanan güvenilirliği kodun gerçek güvenilirliğine
   yaklaştırır.
2. **Kurtarmanın tek yuvalı olması.** İki kez çöken bir kullanıcı, ikinci taslağını
   göremez (`RecoveryStore:315-320`). Saklanan 20 dosyanın 19'u ölü ağırlık.
3. **Tek üst üste binme yuvası.** Find, Welcome, About ve Recent aynı sağ üst köşeyi
   paylaşıyor; biri açılınca öteki kapanıyor (`OverlaySlot:13-22`). Karar gerekçeli —
   kartlar üst üste konamayacak kadar uzun (welcome kartı 1080 px ekranda 750 px) — ama
   bedeli, aynı anda iki şeye bakamayan kullanıcı.
4. **Kurulumun görünmezliği.** Linux'ta depo yok, Windows'ta imza yok. Ürünün en iyi
   tarafı (ambalaj) en görünmez tarafıyla (dağıtım) dengeleniyor.

---

## 5. Mimari değerlendirmesi

### 5.1 Ölçülen şekil

```
JTypeWriterApp (Application değil)  →  FxApplication (JavaFX yaşam döngüsü)
        ↓
AppGraph (kompozisyon kökü; alan sırası = bağımlılık sırası)
        ↓
ui/ (40 dosya)   service/ (13)   print/ (6)   locale/ (3)   audio/ (3)   stats/ (2)
```

- **Elle DI.** Çerçeve yok. `AppGraph` public final alanlar barındırır ve çapraz
  dinleyicileri kurucusunda bağlar. Bu, küçük bir uygulamada en okunabilir seçim;
  bedeli, bileşen eklemenin `AppGraph`'u düzenlemeyi gerektirmesi.
- **Saf model kültürü.** `YoloPolicy`, `StatsBarModel`, `ToolbarFit`, `ExitDisposition`,
  `AutosaveHealth`, `DialogSizing`, `ShortcutRows`, `TextCodec`, `DocumentLayout`,
  `PageGeometry` — hepsi JavaFX'siz, hepsi başsız test edilebilir. Bunun sonucu somut:
  **683 test hiçbir JavaFX toolkit'i başlatmıyor.** Bu, projenin en değerli kültürel
  mirası ve korunmalı.
- **`service/` JavaFX'siz.** Kullanıcıya dönük `FileChooser`/`Alert` işi `DialogService`'te
  izole, testler onu mock'luyor. Doğru sınır.
- **`print/` yeni ve doğru yerde.** `DocumentLayout` tek sayfalama modeli; `PdfExport`
  elle PDF 1.7 yazıyor (kütüphane yok, bağımlılık yok, bayt-deterministik). `Printing`
  tek JavaFX'li sınıf (PrinterJob). Bu, "saf model + ince adaptör" kültürünün yeni
  pakette de sürdüğünü gösteriyor.
- **JavaFX sızıntısı sınırlı.** `ui/` dışında JavaFX import eden 10 dosya var; bunların
  çoğu bilinçli adaptör (`FxApplication`, `IoExecutor`, `DialogService`, `AutoSaveService`).
  `audio/`, `print/`, `AppGraph` gibi yerlerde sızıntı var ama gerekçeli.

### 5.2 Korunması gereken kültürel miras

Bunlar "iyi uygulama" değil, **bu deponun imzası**; bir yeniden yazımda kaybedilirlerse
ürün sıradanlaşır:

1. Kararları JavaFX sınıflarından çıkarıp saf statik yöntemlere taşıma alışkanlığı.
2. Testin değişmezi adıyla söylemesi (uzun `lowerCamelCase` cümle, `@DisplayName` yok).
3. Veri güvenliği semantiğinin sınıf Javadoc'unda gerekçesiyle yazılı olması
   ("neden X değil").
4. Paketlemenin üç kopyayı bayt bayt karşılaştırması.
5. Yerelleştirmede sistem yereline düşmeme ve `Locale.ROOT` disiplini.
6. Lisans başlığının her dosyada byte 0'da olması ve testle pinlenmesi.

### 5.3 Yapısal borç

| Borç | Ölçü | Etki |
|---|---|---|
| Dev sınıflar | `FileService` 1325, `RecoveryStore` 757, `AppSettings` 739, `DialogService` 611 | Değişiklik yarıçapı büyük; tek sınıfta çok sorumluluk |
| `Messages` 898 satır | 175 anahtar × erişimci | Mekanik ama büyüyen; anahtar eklemek 7 dosya düzenlemek |
| `ToolbarView` 767 satır | 6 düğme + 6 açılır kutu + komut menüsü + fit modeli | UI değişikliği ölçüm modelini de etkiliyor |
| Belge dokümantasyon kayması | `ToolbarView`/`ToolbarFit` Javadoc'u "beş düz düğme" diyor; gerçek 6 düz düğme + 6 açılır kutu (`ToolbarView:41-57,177-186`, `ToolbarFit:52-57`) | Küçük ama gerçek: Find düğmesi eklenince düzyazı güncellenmemiş |
| `AppSettings` yazımı FX iş parçacığında | `AppSettings:605-619` | Yavaş diskte arayüz kısa süre donabilir |
| Sürüm kayması | `pom.xml` 1.1.4, `target/` 1.1.3, `releases/` 1.1.2 | Üretilen kanıt ile ağaç uyuşmuyor (§9.3) |

### 5.4 Tek belge varsayımı — nerede kırılır

Çoklu belge/sekme istenirse dokunulması gereken **spesifik** yerler:

| Yer | Neden |
|---|---|
| `AppGraph` alanları | `fileService`, `editorArea`, `mainController` tek örnek |
| `FileService` durumu | `currentFile`, `expectedFingerprint`, `dirty` tek belge |
| `AutoSaveService` | Tek `Timeline`, tek yuva |
| `RecoveryStore` | `draft-<UUID>.jtw` tek canlı yuva (`:262-269`) |
| `MainStage` / `MainController` | Tek `stage`, tek başlık (`MainController:220-231`) |
| `EditorArea` | Tek `TextArea` |
| `StatsBarModel` | Tek `SessionStats` |

Yani sekme, "bir liste daha" değil; **durumun belge başına taşınması** demektir. Bunun
maliyeti, veri güvenliği sözleşmesinin (parmak izi, yedek, kurtarma) her belge için ayrı
yürütülmesini gerektirir. Bu, ürünün en pahalı olası özelliğidir (V).

### 5.5 Komut katmanının sınırı

`EditorAction` 21 komutluk tek bir envanter: etiket, ipucu ve tuş takımı aynı yerden
türetiliyor (`EditorAction.java`), menü ve kısayol kartı bu envanteri okuyor. Bu tasarım
komut eklemeyi tek noktaya indiriyor ve tutarsızlığı yapısal olarak engelliyor. Ancak:

- Komut *eylemleri* hâlâ `KeyboardShortcutHandler` ve `MainMenu`'de dağınık; envanter
  yalnızca meta veri.
- `MainMenu` içinde Yardım sayfası boş listeyle tanımlanıp özel olarak dolduruluyor
  (`MainMenu:220,328`) — envanterin dışında kalan tek menü.

Bu bir kusur değil, olgunlaşmamış bir soyutlama: meta veri birleşti, yürütme birleşmedi.
Komut sayısı 30'u geçerse bu ayrım acıtır (V).

---

## 6. Veri güvenliği: kanıt matrisi

Bu bölüm, ürünün en güçlü tarafının **dürüst** haritasıdır: ne var, ne yok, nerede
zayıf.

### 6.1 Yazma yolu

| Adım | Davranış | Kanıt |
|---|---|---|
| Hedef çözümleme | Gerçek yol; sarkan sembolik bağ reddedilir; çözülemeyen "adıyla" yazılır | `AtomicFiles.java:189-194` |
| Boş alan ön kontrolü | `requiredBytes > usable` → `IOException`; ölçülemeyen depo atlanır | `AtomicFiles.java:210-219` |
| Geçici dosya | Aynı dizinde `createTempFile` | `AtomicFiles.java:142-148` |
| Veri fsync | `channel.force(true)` rename'den **önce** | `AtomicFiles.java:151-157` |
| Öznitelik koruma | POSIX izin/sahip/grup, DOS özniteliği, ACL | `AtomicFiles.java:262-305` |
| Yeni dosya izni | Umask'tan türetilen sonda | `AtomicFiles.java:323-348` |
| Rename | `ATOMIC_MOVE` + `REPLACE_EXISTING`; desteklenmezse düz `move` | `AtomicFiles.java:161-164` |
| Dizin fsync | En iyi çaba; başarısızsa `FINE`, yutulur | `AtomicFiles.java:352-360` |

**Zayıflık:** `ATOMIC_MOVE` desteklenmezse düz `move`'a düşülüyor; çökme yarım hedef
bırakabilir (`AtomicFiles.java:162-164`). Dizin fsync'i başarısız olsa da "kaydedildi"
deniyor (`:356-360`). Parmak izi **yazılması amaçlanan** baytlardan hesaplanıyor, diskten
geri okunmuyor (`FileService.java:945`) — yani "kaydettim" diskteki hâlin kanıtı değil.

### 6.2 Yedekler

| Özellik | Değer | Kanıt |
|---|---|---|
| Kuşak sayısı | 3 (`.bak`, `.bak.1`, `.bak.2`) | `BackupKeeper.java:35` |
| Ne zaman | Kaydetmeden önce, `<name>.bak.new` sahneleme + `COPY_ATTRIBUTES` | `BackupKeeper.java:60-76` |
| Düzenli dosya değilse | Atlanır | `BackupKeeper.java:60-76` |
| Döndürme | En yüksekten aşağı; sahneleme terfi eder | `BackupKeeper.java:91-101` |
| İçerik değişmediyse | Döndürme atlanır | `FileService.java:901-923` |
| Sahneleme başarısız | `WARNING` log; **kaydetme devam eder** | `FileService.java:919-922` |
| Kurulum başarısız | Belge kaydedilmiş sayılır, yedek atılır, yalnızca log | `FileService.java:932-941` |
| Yaş/boyut budama | Yok, yalnızca sayı | `BackupKeeper.java` |

**Zayıflık:** Yedek yazılamadığında kullanıcıya **hiçbir şey söylenmiyor**. Veri
güvenliği iddiasının en görünür parçası (yedek) sessizce devre dışı kalabiliyor. Ayrıca
sabit `.bak.new` adı, aynı dosyayı iki süreç kaydederse çakışır (`BackupKeeper.java:137`).

### 6.3 Dış değişiklik algılama

| Özellik | Değer | Kanıt |
|---|---|---|
| Parmak izi | `size + lastModifiedMillis + sha256` | `FileService.java:1268-1294` |
| Karşılaştırma | Yalnızca SHA-256 eşitliği | `sameContentAs` |
| Dosya yoksa | Değişmiş sayılır | `FileService.java:968-985` |
| Parmak izi null | Save As / yeni dosya → çakışma yok | `FileService.java:968-985` |
| Seçenekler | ÜZERİNE YAZ / FARKLI KAYDET / VAZGEÇ | `FileService.java:817-828` |
| Kalan yarış | Kontrol `:889` ile rename `:925` arasında kilit/CAS yok | `FileService.java:889,925` |

**Zayıflık:** Bu, sınıfın en dürüst yazılmış ama en kapatılamamış boşluğu; TOCTOU
penceresi kısa ama gerçek. Bir kilit dosyası veya rename-sonrası doğrulama kapatırdı.

### 6.4 Kodlama

| Özellik | Değer | Kanıt |
|---|---|---|
| Algılama sırası | UTF-8/16 BOM → BOM'suz UTF-16 çiftlik sezgisi → katı UTF-8 → `null` (sor) | `TextCodec.java:100-120` |
| BOM'lu bozuk yük | Katı çözüm, `MalformedDocumentException` | `TextCodec.java:194-205` |
| Kullanıcıya sorma | Değiştirme öncesi onay | `FileService.java:1084-1094` |
| BOM | Okurken atılır, `bom` bayrağıyla geri yazılır | `TextCodec.java:167-173` |
| Kodlayıcı | `REPORT` → asla `?` | `TextCodec.java:160-163` |
| Temsil edilemeyen | UTF-8'e yükseltme diyaloğu | `FileService.java:880-885,810-816` |
| Eski kodlama seçenekleri | Yalnızca `windows-1254` veya UTF-8 | `FileService.java:1099-1103` |

**Zayıflık:** `decodeWith` hoşgörülü (U+FFFD); kullanıcı geçersiz baytlar için UTF-8'i
seçerse ve kaydederse, U+FFFD baytları geri yazılır (`TextCodec.java:149-151`).
Kurtarma geri yüklemesi orijinali okuyamazsa `windows-1254` varsayıyor
(`FileService.java:577-583`) — yanlış varsayım sessiz bozulma üretebilir.

### 6.5 Kurtarma / otomatik kaydetme

| Özellik | Değer | Kanıt |
|---|---|---|
| Aralık | 30 s + odak kaybı | `AutoSaveService.java:69,160-169` |
| Yuva | Oturum başına tek `draft-<UUID>.jtw` | `RecoveryStore.java:262-269` |
| Biçim | v3 başlık + düz UTF-8 metin, **şifresiz** | `RecoveryStore.java:262-269` |
| Silme | Kaydet/yükle/yeni belge ve cevaplanmış çıkış | `AutoSaveService.java:127-150` |
| Çıkış matrisi | Donmuş → KEEP, cevaplı → CLEAR, aksi → PARK | `ExitDisposition.java:55-59` |
| Saklama | 30 gün / 20 dosya / 64 MiB; en yeni her zaman kalır | `RecoveryRetention.java:60,68,71` |
| Bayat taslak koruması | Dosya mtime > taslak `savedAt` ise yol null'lanır → Save As | `AutoSaveService.java:282-286,298-318` |
| Teklif | Yalnızca **en yeni** taslak | `RecoveryStore.java:315-320` |

**Zayıflık:** 30 saniyelik pencere 30 saniyelik iş kaybı demek. Dizin izinleri
sıkılaştırılmıyor; taslaklar düz metin (`:264`). mtime okunamazsa bayat taslak koruması
devre dışı kalıp orijinal yolun üzerine yazıyor (`AutoSaveService.java:306-310`).
Ve en önemlisi: **20 taslak saklanıyor, 1 tanesi teklif ediliyor** — kurtarma özelliğinin
algılanan değeri, gerçek değerinin yirmide biri.

### 6.6 Eşzamanlılık

| Öğe | Davranış | Kanıt |
|---|---|---|
| İş parçacıkları | FX + tek `jtw-io-1` + tek `jtw-find-1` | `IoExecutor.java:94-105`, `FindScanService.java:93-104` |
| Serileştirme | "Tek işçi kilit olmadan serileştirir" | `IoExecutor.java:31-36` |
| Tamamlama | `completeOnFx` | `IoExecutor.java:130-147` |
| Dinleyici listesi | `CopyOnWriteArrayList` | `FileService.java:149` |
| FX'te bloklayan | Ayar yazımı (`AppSettings:605-619`), açılış yüklemesi, çıkış join'i | çeşitli |
| İki kaydetme | `operation` jetonu commit'i kapılıyor ama kaydetmede artırılmıyor | `FileService.java:989-994` |

**Zayıflık:** Ayarların fsync+rename'i FX iş parçacığında çalışıyor; yavaş diskte her
ayar değişikliği arayüzü kısa süre kilitler. İki eşzamanlı kaydetmede son biten kazanır.

### 6.7 Hata raporlama

| Sınıf | Örnek | Kanıt |
|---|---|---|
| Uyarı gösterir | Açma hatası, çok büyük dosya, kaydetme hatası, dışa aktarma hatası, ilk otomatik kaydetme hatası, açılış çökmesi | `FileService:455-457,446-453,1020-1024`, `ExportService:110-118`, `AutoSaveService:490-499`, `CrashHandler:48-62` |
| Yalnızca log | Yedek sahneleme/kurulum, dış değişiklik satırı, dizin fsync, öznitelik kopyası, ayar yazımı | `FileService:919-922,937-940,978-979`, `AtomicFiles:269,280,298,307,359`, `AppSettings:621-624` |
| Sessiz | Kurtarma dizini listelenemezse boş, süresi geçmiş taslak silinemezse `FINE`, kilit alınamazsa yalnızca stderr | `RecoveryStore:286-288,364-366`, `InstanceLock:125-128` |

Veri yolunda boş `catch` yok; en yakın şey `BackupKeeper.java:143` (`deleteQuietly`).

### 6.8 Gizlilik yüzeyi (dürüst envanter)

- Ağ çağrısı yok; tek dış eylem `mailto:` (`AboutCard.java:84`). Telemetri, güncelleme
  denetimi, çökme raporlama yok.
- Diske yazılanlar: belge, `.bak*`, `jtw-*.tmp`, `~/.jtypewriter/` (ayar, log, kurtarma,
  kilit). Kurtarma taslakları **düz metin**.
- Üçüncü taraf hiçbir şey yok: ses dosyası, fontlar ve ikonlar paket içinde.

Bu, "gizlilik dostu" iddiasını gerçekten taşıyor; ama aynı tercih desteklenebilirliği
(§12) ve ürün öğrenmesini (§3.4) körleştiriyor. Denge, ürünleştirmenin cevaplaması
gereken en net soru.

---

## 7. Deneyim ve erişilebilirlik

### 7.1 Güçlü yanlar

- **Klavye birinci sınıf.** 21 komut, `Ctrl+Z/Y`, `F3`/`Shift+F3`, `F11`, `Esc`, sayısal
  tuş takımı eşdeğerleri (`EditorAction.java`, `KeyboardShortcutHandler`). Kısayol kartı
  envanterden üretildiği için menüyle asla ayrışmıyor (`ShortcutRows.fromActions`).
- **Odak sırası bilinçli.** Araç çubuğunun 13 kontrolü `viewOrder` ile görsel sıraya
  sabitlenmiş; JavaFX 26'da `nextFocus` olmadığı için elle kurulmuş (`ToolbarView:230-240`).
  Kart açıldığında odak kapat düğmesine gidiyor (`OverlayCard:258-262`).
- **Karşıtlık ölçülüyor.** WCAG AA: metin 4.5:1, bileşen 3.0:1; sınır 4.49/4.50 olarak
  pinlenmiş (`ContrastRatioTest:34-66`). "Yaklaşık 4.5" değil, tam 4.5. Kapsam gerçek:
  durum çubuğu, kaydetme durumu, otomatik kaydetme uyarısı, odak halkası, araç çubuğu,
  kartlar — hepsi yayımlanan CSS'ten yeniden hesaplanıyor (`ContrastRatioTest:126-135`).
- **Araç çubuğu taşmıyor ve alt sınırı türetilmiş.** Tek satır kalır; genişlik yetmezse
  "Tema: Koyu" → "Koyu" (`ToolbarFit`). 40 px histerezis eşikte titremeyi engelliyor.
  Minimum pencere genişliği **889 px** (en geniş daraltılmış satır 873 + 16 px süsleme) ve
  bu sayı altı dilin paketlerinden yeniden türetilerek test ediliyor
  (`MainStage:37-55`, `ToolbarFitTest`); daha uzun bir çeviri etiketi sessizce kırpılmak
  yerine derlemeyi düşürüyor.
- **İpucu kapsamı araç çubuğunda tam.** 13 kontrolün 13'ünde ipucu var ve her ipucu
  `EditorAction`'ın kendi tuş takımından okunuyor, dil değişiminde yeniden yazılıyor
  (`ToolbarView:203-219,748`). Bul panelinin 5 düğmesi, kart kapatma ve son dosyalar
  satırları da ipuçlu (`FindPanel:475,490-493`, `OverlayCard:127`, `RecentFilesCard:114`).
- **Tema ve tipografi tutarlı.** Üç tema, kullanıcı metin rengi, font boyutu; hepsi
  `.css` üzerinden.
- **Durum çubuğu yerelleştirilmiş saat** kullanıyor (`FormatStyle.SHORT`), sabit desen yok
  (`StatsBarModel:56-63`).

### 7.2 Eksikler

| Eksik | Ölçü / kanıt | Etki |
|---|---|---|
| Kaydetme görünürlüğü | Otomatik kaydetme başarısı gösterilmiyor; yalnızca "kaydediliyor" anlık (`MainController:161-163`) | Algılanan güvenilirlik düşük |
| Ekran okuyucu | 18 `accessible*` kullanımı; araç çubuğu ve bul paneli kapsanmış, geri kalan yüzey değil (`ToolbarView:487-493`, `FindPanel:474-497`) | Kör/az gören kullanıcı için ciddi |
| Yazı büyütme | Yalnızca editör metni büyüyor; arayüz 11 px'e sabit (`ToolbarFit:76`, `MainController:341`) | Düşük görüşlü kullanıcı arayüzü büyütemiyor |
| Durum çubuğu boyutu | Yalnızca çift tıklamayla değişiyor; menüde kısayolu yok (`StatsBar:98,170-180`) | Keşfedilemez ayar |
| Tek üst üste binme yuvası | Find/Welcome/About/Recent aynı köşe yuvasını paylaşıyor; biri açılınca öteki kapanıyor (`OverlaySlot:13-22`) | Bilgi kaybı, beklenti dışı kapanma |
| RTL | Yok (`NodeOrientation` hiç kullanılmıyor) | Arapça/İbranice yok (zaten çevrilmiyor) |
| Hareket azaltma | Yok | Erişilebilirlik tercihi yok sayılıyor |
| OS yüksek karşıtlık | Yok; yalnızca uygulama temaları | Sistem tercihine uymuyor |
| Boş durum | Belge yokken hoş geldin kartı; "boş belge" için ek yönlendirme yok | Küçük |
| Ham istisna metni | `FileService:456,1022`, `ExportService:79,112`, `DialogService:497`, `Printing:127` | Teknik metin kullanıcıya ulaşıyor |
| Diskte boş alan mesajı | Çevrilmemiş İngilizce düzyazı, hata gövdesine ham giriyor (`FileService:912`) | Tek bir hata yolu tamamen İngilizce |

Ham istisna metni, §8 ile kesişen en somut i18n/UX kusuru: çevrilemeyen, çoğu zaman
anlamsız bir dize hata diyaloğunun gövdesine giriyor. `WriteFailed.describe` gibi
sarmalayıcılar doğru yönde ama kapsamı tam değil. Diskte boş alan mesajı bunun en saf
örneği: `WriteFailed`'a doğrudan İngilizce bir cümle yazılmış (`FileService:912`).

---

## 8. Yerelleştirme

| Özellik | Durum | Kanıt |
|---|---|---|
| Diller | EN (taban), TR, DE, FR, IT, ES | `Language` |
| Anahtar sayısı | 175 × 6, birebir aynı küme | Ö |
| Parite zorlaması | Eksik **ve** ölü anahtar testi başarısız eder | `MessagesTest` |
| Erişimci zorlaması | Her anahtar `Messages`'ta olmalı | `MessagesTest` |
| Yerel seçimi | İlk açılışta sistem dili, sonra kullanıcı seçimi kalıcı | `LocaleManager:26-28` |
| Paket araması | Sistem yereline düşmez, `Locale.ROOT` | `Messages:35-49` |
| Türkçe noktasız-i tuzağı | Bilinçli kaçınılmış | `LocaleManager` |
| Yerel biçimlendirme | Saat `SHORT`, kurtarma zamanı `MEDIUM`; ikisi de UI yereline göre | `StatsBarModel:92-97`, `DialogService:591` |
| Dil adları | Java sabiti (çeviri anahtarı değil) | `Language` |
| CI yerel provası | `tr_TR` ve `de_DE` ile tüm süit | `verify-build.yml` |

Bu tablo, çoğu açık kaynak projede bulunmayan bir disiplin. Gerçek boşluklar:

1. **Çeviri gözden geçirme süreci yok.** 6 paket var ama kimin, neye dayanarak
   çevirdiğine dair kayıt yok; dilbilgisel doğruluk test edilmiyor (V).
2. **Çevrilemeyen dizeler.** Ham istisna metni (§7.2), yazıcı hatası ayrıntısı
   (`Printing:127`), dışa aktarma nedeni (`ExportService:126`).
3. **Genişleyen metin.** Almanca/Fransızca daha uzun; `ToolbarFit` bunu ölçüyor (iyi),
   ama diyalog düzeni için benzer ölçüm yok (`DialogSizing` yalnızca boyut sınırlıyor).
4. **Yeni dil eklemek** 175 anahtar + `Language` sabiti + altı paketin elle çevirisi
   demek; süreç dokümante değil.
5. **"Sistemi izle" yok.** Dil ilk açılışta sistemden seçiliyor, sonra sabitleniyor;
   işletim sistemi dilini değiştiren kullanıcı uygulamayı elle değiştirmek zorunda
   (`LocaleManager:26-28`).

Özet: yerelleştirme **mühendislik olarak 4/5, süreç olarak 2/5**. Fark, bir çeviri
katkısının nasıl kabul edileceği sorusunda ortaya çıkar ve şu an cevabı yok.

---

## 9. Paketleme ve dağıtım

### 9.1 Paketleme hattı (güçlü)

| Aşama | Ne yapıyor | Kanıt |
|---|---|---|
| Derleme | Enforcer (JDK ≥25, Maven ≥3.8), sabit `outputTimestamp` | `pom.xml:134-149,61` |
| JAR'lar | İnce + `jar-with-dependencies` + sınıflandırılmış JavaFX modülleri | `pom.xml` |
| `jlink` | 5 modül, `--strip-debug --no-header-files --no-man-pages --compress=zip-6` | `package.sh:54,220-226` |
| JavaFX | JAR olarak `lib/javafx`'e **betikle** kopyalanır (jlink `.so` düşürüyor) | `package.sh:157-162,260-273` |
| Başlatıcı | `--module-path` `$APPDIR`'a göre, `$ROOTDIR` **değil** | `package.sh:244` |
| `.deb` | `dpkg-deb -R` + yasal dosyalar + postinst'e hata yutma eki + md5sums + DEP-5 | `package.sh:390-511` |
| `.msi` | `--win-menu --win-shortcut --win-dir-chooser --license-file` | `package.sh:616-618` |
| Doğrulama | Depo ↔ imaj ↔ `.deb` bayt karşılaştırması + imaj düzeni iddiaları | `verify-artifacts.sh:170-313` |
| Duman | Xvfb + `xwininfo` başlık araması + `.deb` kur/başlat/kaldır | `smoke-appimage.sh:150-183,202-312` |
| Sağlama | Her dosya için SHA-256, `LC_ALL=C` | `checksums.sh:19-85` |
| Varlık adlandırma | `JTypeWriter-<v>-<os>-<arch>.<ext>`, tek üst dizinli `.tar.gz` | `release-assets.sh:37-128` |

Bu hat, projenin **en olgun** parçası. Üç bağımsız kanıt (depo, imaj, `.deb`) ve bir
çalıştırma kanıtı (duman) var. Kanıtlayamadığı: `.msi` kurulumu (yalnızca "10 saniye
yaşıyor", `smoke-appimage.sh:376-417`), imzalama, yeniden üretilebilirlik.

### 9.2 Dağıtım (zayıf)

| Kanal | Durum |
|---|---|
| GitHub Releases | Var: `.deb`, `.msi`, `.tar.gz`, `SHA256SUMS` |
| Dağıtım deposu / PPA | Yok |
| winget / scoop / Chocolatey | Yok |
| Flatpak / Snap / AppImage | Yok |
| Mağaza listesi | Yok |
| Kod imzalama | Yok |
| Otomatik güncelleme | Yok |
| Kaynak teklifi görünürlüğü | Yalnızca URL (`THIRD-PARTY-NOTICES:147-173`) |

Sonuç: Ürünün kurulumu, **teknik olarak doğru ama sosyal olarak görünmez**. Bir Linux
kullanıcısı `.deb`'i elle indirip `dpkg -i` çalıştırmak zorunda; bir Windows kullanıcısı
imzasız `.msi` yüzünden SmartScreen uyarısı görüyor. Ambalajın 5/5 kalitesi, dağıtımın
2/5'ini telafi etmiyor.

### 9.3 Sürüm ve kanıt kayması

- `pom.xml:9` → **1.1.4**; etiket `v1.1.4` = `89bfc90`.
- `target/*.jar` → **1.1.3** (eski derleme kalıntısı).
- `releases/linux/jtypewriter_1.1.2_amd64.deb` → **1.1.2**.

Yani depodaki üretilmiş kanıtlar üç farklı sürümü gösteriyor. `target/` ve `releases/`
üretilen dizinler olduğu için commit'e girmiyor (doğru), ama **doğrulama kanıtı olarak
sunulduklarında** hangi ağaca ait oldukları belirsiz. Bu, §9.1'deki güçlü hattın tek
zayıf noktası: kanıt taze değil.

### 9.4 Yükseltme ve ayarlar

| Konu | Davranış | Kanıt |
|---|---|---|
| Sürüm kaynağı | Yalnızca `pom.xml`, filtrelenmiş `version.properties` | `pom.xml`, `AppVersion` |
| Ayar şeması | **Ürün sürümüyle aynı sayı** | `AppSettings:95` |
| Yeni ayarları okuma | Olduğu gibi okur, bilinmeyen anahtarları korur | `AppSettings:441-464` |
| Düşürme | Daha yeni şema damgası korunur | `AppSettings:600-619` |
| Yükseltme yolu | Elle indirme; uygulama içi güncelleme yok | `README.md:265-270` |
| Kurtarma taslakları | Yükseltmeden sağ çıkar | `RecoveryStore` |

Şema sürümünün ürün sürümüne bağlı olması zarif ama kırılgan: her sürüm, ayar dosyasının
şemasını da "değişmiş" gösterir. Bir gün geriye dönük uyumluluk gerekirse bu bağ
çözülmek zorunda (V).

---

## 10. CI/CD ve test stratejisi

### 10.1 Hat

```
push/PR → maven.yml ─┐
                     ├→ verify-build.yml (workflow_call)
tag    → release.yml ┘
```

| Adım | Ne yapıyor | Kanıt |
|---|---|---|
| Matris | `ubuntu-latest`, `windows-latest`; Temurin JDK 25; Maven önbelleği | `verify-build.yml:43,58-63` |
| Kapı 1 | `generate-icons.py --check` (ImageMagick gerekmez) | `verify-build.yml` |
| Kapı 2 | `mvn -B package` | aynı |
| Kapı 3 | `tr_TR` ve `de_DE` ile tüm süit | aynı |
| Kapı 4 | `package.sh --skip-build --installer` | aynı |
| Kapı 5 | `verify-artifacts.sh --skip-build` (Linux) | aynı |
| Duman işi | `needs: verify`, indirilen eser, Xvfb, `.deb` kur/kaldır | `verify-build.yml` |
| Yayın | Etiket = `v<pom sürümü>`, elle çalıştırma yayınlayamaz, 5 varlık + `SHA256SUMS` | `release.yml:84-113,287-307` |
| Notlar | İki dilli, `@VERSION@`/`@TAG@`, yer tutucu reddi | `release.yml`, `docs/release-notes-template.md` |

Ek olarak `CiGatesTest` bu kapıların **varlığını** pinliyor: ikon kontrolü, iki yerel
koşusu, `verify-artifacts.sh --skip-build`, duman işinin `needs: verify` bağımlılığı.
Yani bir kapı sessizce silinemez. Bu, süitin en akıllı testi.

### 10.2 Test piramidi

| Katman | Sayı | Not |
|---|---|---|
| Saf model / birim | Çoğunluk | JavaFX yok, toolkit yok |
| Servis | 14 sınıf | Mockito yalnızca `Messages`/`DialogService` |
| Ambalaj/uyum | `PackagingNoticesTest`, `AppVersionTest`, `CiGatesTest`, `LicenseHeaderTest` | Değişmezleri kod olarak tutuyor |
| Süreç | `ProcessCrashRecoveryTest` | Linux'ta çocuk JVM + `destroyForcibly()`, en yavaş (0,71 s) |

683 test, ~5,3 s. Hız, süitin gerçekten çalıştırılmasını sağlıyor — bu, kapsamdan daha
önemli bir özellik.

### 10.3 Sınırlar

- Kod kapsamı ölçülmüyor, eşik yok.
- SBOM yok; bağımlılık CVE taraması yok (Dependabot yalnızca `github-actions`).
- Yeniden üretilebilirlik karşılaştırması yok (sabit timestamp var, kanıtı yok).
- `.msi` kurulum/kaldırma test edilmiyor; Windows dumanı derinliksiz.
- Performans regresyonu kapısı yok.
- Statik analiz / formatlayıcı yok (bilinçli — `AGENTS.md`).

---

## 11. Lisans, uyum ve yasal

| Öğe | Durum | Kanıt |
|---|---|---|
| Uygulama lisansı | Apache-2.0, her dosyada byte 0'da | `LicenseHeaderTest` |
| Üçüncü taraf | JavaFX GPL-2.0+CPE, GStreamer Lite LGPL-2.1+, gömülü KISS/dbesi0/gsturi, GCC runtime, IJG JPEG, DejaVu | `THIRD-PARTY-NOTICES:1-146` |
| LGPL yükümlülüğü | Üç yıllık kaynak teklifi + yeniden bağlama ifadesi | `THIRD-PARTY-NOTICES:147-173` |
| Font lisansı | Bitstream Vera, `legal/Bitstream-Vera.txt` | `resources/fonts/PROVENANCE.md` |
| `.deb` telif | DEP-5, bileşen başına stanza, doğru sıra | `package.sh:460-511` |
| JDK bildirimleri | `lib/runtime/legal/` | imaj |
| Bütünlük | `THIRD-PARTY-NOTICES` jar içindeki kopyayla bayt aynı | `PackagingNoticesTest` |

Uyum tarafı örnek niteliğinde. Kalan boşluklar:

1. **SBOM yok.** Bağımlılık grafiği makine okunur biçimde üretilmiyor.
2. **JavaFX GPL kaynağı yalnızca URL ile teklif ediliyor**; yazılı teklif yok.
3. **İmza yok** — sağlama toplamı yalnızca taşıma bütünlüğü kanıtlıyor, kaynak değil.
4. **Lisans bağlantısı yalnızca düzyazı olarak var.** Sürüm notları her varlığın
   `LICENSE`/`NOTICE`/`THIRD-PARTY-NOTICES` taşıdığını ve kaynak arşivini söylüyor
   (`docs/release-notes-template.md:34`), ama bunları sayfada tıklanabilir bağlantıya
   çevirmiyor; README'deki ayrıntılı lisans bölümü (`README.md:648`) indirme sayfasından
   erişilemiyor.

---

## 12. Desteklenebilirlik ve operasyon

| Konu | Durum | Kanıt |
|---|---|---|
| Log | Dosyaya, `AppLogging`; seviye yönetimi var | `AppLogging` |
| Çökme | `CrashHandler` diyaloğu | `CrashHandler:48-62` |
| Telemetri | **Yok** (bilinçli) | ağ taraması |
| Sorun toplama | Şablon yok, `SECURITY.md` yok, issue şablonu yok | §§2, 14 |
| Kullanıcı belgeleri | README ×2, kısayol kartı, hoş geldin kartı, Hakkında | `README.md`, `ui/` |
| Uygulama içi yardım | Help menüsünde yalnızca Hakkında var; çevrimiçi belge yok | `MainMenu:327-331` |
| Görsel tanıtım | README'de rozetler var, uygulamanın tek bir ekran görüntüsü yok | `README.md` |
| SSS / sorun giderme | Yok | `docs/` |
| Sürüm geçmişi | Etiketler var, `CHANGELOG` yok | §14 |
| Otobüs faktörü | **1** (215/221 commit tek insan) | Ö |

Desteklenebilirliğin en büyük sorunu teknik değil: **kullanıcı bir sorunla karşılaştığında
nereye gideceğini bilmiyor** ve proje sorunu nasıl alacağını tanımlamamış. Bir yazma
uygulamasında "dosyam bozuldu" diyen bir kullanıcıya verilecek ilk cevap (log nerede,
yedek nerede, taslak nerede) hiçbir belgede yazmıyor.

---

## 13. Risk kaydı

| # | Risk | Olasılık | Etki | Kanıt |
|---|---|---|---|---|
| R1 | Tek sürdürücü kaybolur, proje durur | Orta | Yüksek | 215/221 commit |
| R2 | Veri kaybı iddiası ile sessiz yedek hatası çelişir; kullanıcı güvenini kaybeder | Düşük–Orta | Yüksek | `FileService:919-922` |
| R3 | TOCTOU dış değişiklikte kullanıcı verisi ezilir | Düşük | Yüksek | `FileService:889,925` |
| R4 | İmzasız `.msi` SmartScreen ile kurulumu engeller | Yüksek | Orta | §9.2 |
| R5 | Kurtarma tek yuvalı; ikinci çökme taslağı kaybolur | Orta | Orta | `RecoveryStore:315-320` |
| R6 | Çoklu belge talebi gelir; mimari büyük refactor gerektirir | Orta | Yüksek | §5.4 |
| R7 | Ekran okuyucu kullanıcısı ürünü kullanamaz | Orta | Orta | 18 `accessible*` |
| R8 | Ham istisna metni hata diyaloğunda yanlış anlaşılır | Orta | Düşük–Orta | `FileService:456,883,1185` |
| R9 | Ürün hiç duyulmaz; mühendislik değeri kullanıcıya ulaşmaz | **Yüksek** | **Yüksek** | §3.4, §9.2 |
| R10 | Sürüm/şema çiftçiliği yükseltmede ayar kaybına yol açar | Düşük | Orta | `AppSettings:95` |

R9, listenin en yüksek olasılıklı maddesi ve diğer bütün risklerin önünde: kullanıcısı
olmayan bir ürünün veri güvenliği de, ambalaj kalitesi de değer üretmez.

---

## 14. Ürünleştirme boşlukları — aday havuzu

Bu bölüm **karar değil, envanter**. Her satır bir gözlem; hangisinin ne zaman yapılacağı
bu belgenin konusu değil.

### 14.1 Güven ve yayın kanıtı

| # | Boşluk | Kanıt |
|---|---|---|
| G1 | Yedek yazılamadığında kullanıcıya görünür uyarı yok | `FileService:919-922` |
| G2 | Dizin fsync başarısızken "kaydedildi" deniyor | `AtomicFiles:352-360` |
| G3 | Kaydetme sonrası diskten geri okuma doğrulaması yok | `FileService:945` |
| G4 | Dış değişiklikte TOCTOU penceresi | `FileService:889,925` |
| G5 | Kurtarmada yalnızca en yeni taslak teklif ediliyor | `RecoveryStore:315-320` |
| G6 | Otomatik kaydetme başarısı görünmüyor | `MainController:161-163` |
| G7 | Kurtarma taslakları düz metin, dizin izni sıkılaştırılmıyor | `RecoveryStore:264` |

### 14.2 Erişim ve dağıtım

| # | Boşluk | Kanıt |
|---|---|---|
| G8 | Kod imzalama yok | §9.2 |
| G9 | Dağıtım deposu / PPA yok | §9.2 |
| G10 | winget/scoop/Chocolatey yok | §9.2 |
| G11 | Flatpak/AppImage/Snap yok | §9.2 |
| G12 | Otomatik güncelleme yok | §9.2 |
| G13 | Dosya ilişkisi / "birlikte aç" yok | `package.sh` |
| G14 | `.desktop` kategorisi `Unknown` | §9.3 |

### 14.3 Ürün derinliği

| # | Boşluk | Kanıt |
|---|---|---|
| G15 | Çoklu belge / sekme yok | §5.4 |
| G16 | Ekran okuyucu gezintisi eksik (18 `accessible*`) | §7.2 |
| G17 | Arayüz yazı büyütmesi yok; yalnızca editör metni ölçekleniyor | §7.2 |
| G18 | Durum çubuğu boyutu yalnızca çift tıklamayla; menü/kısayol yok | `StatsBar:98,170-180` |
| G19 | Overlay kartları tek yuva paylaşıyor | `OverlaySlot:13-22` |
| G20 | Salt okunur dosya için ön kontrol yok; ham `AccessDeniedException` | `FileService:1022` |
| G21 | Ham istisna/İngilizce metin kullanıcıya ulaşıyor (boş alan mesajı dâhil) | `FileService:456,912,1022` |
| G22 | RTL / hareket azaltma / OS yüksek karşıtlık yok | §7.2 |
| G23 | SSS / sorun giderme belgesi yok | §12 |

### 14.4 Süreç ve sürdürülebilirlik

| # | Boşluk | Kanıt |
|---|---|---|
| G24 | `CHANGELOG` yok | §2 |
| G25 | `CONTRIBUTING` yok | §2 |
| G26 | `CODE_OF_CONDUCT` yok | §2 |
| G27 | `SECURITY.md` yok | §2 |
| G28 | Issue / PR şablonu yok | §2 |
| G29 | `ARCHITECTURE.md` yok (mimari yalnızca README bölümü) | §2 |
| G30 | SBOM yok | §10.3 |
| G31 | Bağımlılık CVE taraması yok | §10.3 |
| G32 | Kapsam eşiği yok | §10.3 |
| G33 | Çeviri gözden geçirme süreci yok | §8 |
| G34 | `.msi` kurulum testi yok | §10.3 |
| G35 | README'de uygulamaya ait tek bir ekran görüntüsü yok | `README.md` |

Toplam **35 aday**. Bunların bir kısmı tek oturumluk, bir kısmı mimari (G15). Sıralama
ve seçim, bu belgenin dışında verilecek bir karardır.

---

## 15. Yeniden üretim komutları

Aşağıdakiler §2'deki her sayıyı üretir; depo kökünde çalıştırılır.

```bash
# Sınıf / satır sayıları
find src/main/java -name '*.java' | wc -l
find src/main/java -name '*.java' -exec cat {} + | wc -l
find src/test/java -name '*.java' | wc -l
find src/test/java -name '*.java' -exec cat {} + | wc -l

# Paket dağılımı
find src/main/java -name '*.java' | sed 's|src/main/java/org/mesutormanli/jtypewriter/||' \
  | awk -F/ '{print (NF>1 ? $1 : "(root)")}' | sort | uniq -c | sort -rn

# En büyük dosyalar
find src/main/java -name '*.java' -exec wc -l {} + | sort -rn | head -18

# Test sayısı ve süresi (Surefire raporlarından)
python3 - <<'EOF'
import glob,re
tot=tests=0
for f in glob.glob('target/surefire-reports/*.xml'):
    s=open(f,encoding='utf-8',errors='replace').read(4000)
    m=re.search(r'time="([\d.]+)" tests="(\d+)"',s)
    if m: tot+=float(m.group(1)); tests+=int(m.group(2))
print("tests",tests,"sum_time_s",round(tot,1))
EOF

# Mesaj anahtarları (altı pakette aynı olmalı)
for f in src/main/resources/messages*.properties; do
  printf '%-46s %s\n' "$f" "$(grep -c '^[a-zA-Z].*=' "$f")"
done

# Kaynak boyutları
du -sh src/main/resources/fonts src/main/resources/icons
wc -l src/main/resources/css/*.css

# Eserler
ls -l target/*.jar
du -sh releases/linux/JTypeWriter/lib/{runtime,javafx,app}
ls -l releases/linux/*.deb

# Git fotoğrafı
git rev-list --count HEAD; git tag | wc -l; git shortlog -sn | head
git log --format='%ad' --date=short | sed -n '1p;$p'

# Süit
mvn -o test
LC_ALL=tr_TR.UTF-8 mvn -o test
```

---

## 16. Sonuç

JTypeWriter, ürünleştirmenin **iki yarısından birini** neredeyse kusursuz yapmış bir
projedir. Yapım yarısı — mimari, veri güvenliği, ambalaj, test, uyum — sınıfının
üstünde ve ölçülebilir. Ürüne dönüşme yarısı — keşfedilebilirlik, dağıtım, görünür
güven, desteklenebilirlik, sürdürülebilirlik — ölçülebilir biçimde eksik.

Bu dengesizlik bir başarısızlık değil, bir **sıra** sorunudur: proje, henüz sırası
gelmemiş işleri yapmamış; sırası gelmiş işleri de eksiksiz yapmış. §1.3'teki 3,6/5
ortalaması bunun tam sayısı: yüksek skorlar mühendislikte, düşük skorlar üründe.

Sonraki adımın (P4) sorusu bu yüzden "hangi özellik" değil, **"bu ürünün var olduğunu
kim, nereden öğrenecek ve ilk açılışta neden güvenecek"** olmalı. §14'teki 35 aday bu
sorunun cevabına göre sıralanacak; sıralamanın kendisi bu belgenin işi değil.

---

*Bu belge yalnızca ölçüm ve değerlendirme içerir. Yol haritası, iş paketi, takvim ve
çıkış kapısı tanımlamaz.*
