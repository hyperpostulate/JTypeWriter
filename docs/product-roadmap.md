# JTypeWriter — P2 Ürün Yol Haritası (Pareto Sıralı)

> **Sürüm:** 2.0 · **Tarih:** 2 Kasım 2026 · **Taban:** `docs/analysis.md` v4.1 (`main` @ `f32d3f8`)
> **Kapsam:** **Yalnız P2.** P2'nin iş sırası, kartları, kapıları, izleme biçimi ve takvimi.
> P0/P1/P1.2/G0 kapanmıştır; macOS 2 Kasım 2026'da üründen çıkarılmıştır (`analysis.md` §2.4).
> **Atıf kuralı:** yalın `§N` bu belgenin bölümüne, `analysis.md §N` taban belgeye işaret eder.
> **Bulgu kimlikleri** (`D/E/X/U/B/M/T`) `analysis.md` §14'ten alınmıştır; burada yeniden
> tanımlanmaz, yalnız kullanılır.
> **Bu belge bir istek listesi değildir.** Neyin **yapılmayacağını** ve neden yapılmayacağını da
> tanımlar (§8). Bir kart "bitti" sayılmaz; **kanıtı** olmadan "bitti" sayılmaz (§6).

---

## 1. Yönetici Özeti

### 1.1 Tek cümle

**Altı iş, havuzun %45'i, beş kapının beşi:** önce yayın kanıtını üret (A6), sonra yazarken
donmayı bitir (A3), sonra dosyaya kimlik ver (A1), sonra komut yüzeyini kur (A4), sonra
ara/değiştir'i ekle (A2), ve P1'den devreden diyalog/yerel biçim borcunu kapat (A5) — bu sırayla;
P1'in kalan üç açığı (U1 taşma, U5 sabit tarih, B1/B14/B15 paket kanıtı) bu kümenin **içine**
yerleştirilmiştir, ayrı bir kuyruk oluşturmaz — üçü de kapatıldı (W0-01…W0-04, W1-01).

### 1.2 Bu belgenin taban belgeden devraldığı üç karar

| Karar | Kaynak | Yol haritasına etkisi |
|---|---|---|
| macOS desteklenmez | `analysis.md` §2.4 | A11 (Cmd+Q doğrulama) havuzdan düştü; CI matrisi 2 OS × 3 locale; beklenen yayın varlığı **5 + 1 (`SHA256SUMS`)** |
| Gerçek darboğaz istatistik yenilemesi | `analysis.md` §8.3 (ölçüm) | X5, A3'ün **birinci** kartı oldu; "10 MB'da kaydetme donar" iddiası geçersiz |
| Bir kapıyı kapatan iş ≠ kapının ön koşulu | `analysis.md` §12.3 | W0 kartları kapı saymaz; "üç kart bitti, skor artmalı" yanılgısı burada engellenir |

### 1.3 Sıralamanın tek cümlelik gerekçesi

> **Önce yayınla**, çünkü yayınlanmamış bir üründe diğer her iş tahmindir (`analysis.md` §4.4);
> **sonra donmayı bitir**, çünkü ürünün adı "kesintisiz yazma" ve ölçülen tek darboğaz orada;
> **sonra kimlik ver**, çünkü ürünün tek savunulabilir farklılaştırıcısı veri bütünlüğü;
> **sonra editör yap**, çünkü "metin alanı" ile "editör" arasındaki fark arama ve menüde.

---

## 2. Taban, Hedef ve Hedefin Niteliği

### 2.1 Bugünkü ölçüm

| Boyut | Ağırlık | Bugün | Kaynak |
|---|---:|---:|---|
| Motor | 0,25 | **3,6** | `analysis.md` §12 |
| Deneyim | 0,25 | **2,2** | aynı |
| Ambalaj | 0,20 | **2,1** | aynı |
| Güven | 0,15 | **2,9** | aynı |
| Pazara erişim | 0,15 | **1,5** | aynı |
| **Endeks** | 1,00 | **2,53** | aynı |

### 2.2 P2'nin beklenen sonucu (hedef değil, **sonuç**)

| Boyut | P2 sonrası (beklenen) | Neyle hareket eder | Neden tavana vurmaz |
|---|---:|---|---|
| Motor | **~4,3** | A1 (D1/D2/D3/D8), A3 (X1–X5) | Symlink/dış değişiklik kapanır; tek-yazar ve sağlam-FS varsayımı sürer |
| Deneyim | **~3,4** | A2, A4, A5, A7, A17 | Yazdırma (A12) yok; sekmeler/satır numarası P3 |
| Ambalaj | **~3,0** | A6 (yayın + smoke + CI kapıları) | jlink (A13) yok → imaj 145,4 MiB kalır; imza yok |
| Güven | **~3,3** | A6, A8, A16 | `SECURITY.md`/SBOM (A9, A15) yok |
| Pazara erişim | **~2,6** | A4 (CLI), A6 (yayın) | MIME/`.desktop` (A14) yok → "OS'ten aç" gerçekleşmez |
| **Endeks** | **~3,4** | | |

> **Uyarı (`analysis.md` §12.2):** Endeks beş boyutlu, ağırlıklı, tek kişilik bir yargıdır;
> istatistik değildir ve **bu yol haritasının hedefi değildir**. Hedef §7'deki beş
> yanlışlanabilir kapıdır. Endeks yalnız *beklenen sonucu* gösterir; kapılar geçilip endeks
> 3,4 çıkmazsa yanlış olan endekstir, kapılar değil.

---

## 3. Pareto Yöntemi ve Kesme Noktası

### 3.1 Üç kural, üç ölçülebilir karşılık

| # | Pareto kuralı | Bu belgedeki karşılığı |
|---|---|---|
| 1 | **Hayati azınlık** | Havuzdaki 16 adaydan **6'sı** (`analysis.md` §13.1) beş kapının **beşini** kapatır: A6, A3, A1, A4, A2, A5 |
| 2 | **Kuyruğu kes, sıralamakla uğraşma** | A9, A10, A12, A13, A14, A15 **yapılmaz** (§8). Gerekçe "değersiz" değil, **"hiçbir kapıyı kapatmıyor ve kritik yolu uzatıyor"** |
| 3 | **Kesme noktası sıralamadan önemlidir** | 6 kapı işi = **14 puan** (`analysis.md` §13.1 birimi). P2 havuzdan **20 puan** alır; 31 puanlık havuzun **11 puanı bilinçli harcanmaz**. Buna havuzda olmayan zorunlu bağımlılık D13 (+1) eklenir |

**Dürüstlük notu:** Bu repoda katı 20/80 tutmuyor ve tutması beklenmemeli — P1 ucuz kazançları
(veri güvenliği, kodlama, çıkış matrisi) zaten toplamıştır (`analysis.md` §3.2). Geriye kalan her
iş **kesişen** iştir: 6 bundle + test + paket + belge aynı anda dokunur. Doğru ifade
*"işin %20'si kazancın %80'i"* değil, **"havuzun %45'i, beş kapının beşi"**dir. Pareto'nun
buradaki değeri sıralama değil, **kesme cesaretidir**.

### 3.2 Düzeltilmiş sıra

| Sıra | İş | Skor (`analysis.md` §13.1) | Kapı | Efor (puan) | Bağımlılık | Neden bu sırada |
|---:|---|---:|---|---:|---|---|
| 1 | **A6** Yayın + CI artefakt smoke + CI kapıları | 7,5 | **G1 / 5** | 2 | — | Tek başına **3 boyutu** hareket ettirir; diğer işlerin doğrulama zemini (K5) |
| 2 | **A3** Sayaç + G/Ç FX thread'inden çıksın | 6,0 | **G2 / 4** | 2 | A6 | Ölçülen tek darboğaz (§8.3); A2'nin önkoşulu (K2) |
| 3 | **A1** Belge kimliği | 3,3 | **G2 / 1** | 3 | A6 | En düşük skorlu çekirdek iş, ama tezi **koşullu olmaktan çıkaran tek iş**; A3'ten **sonra** (K4) |
| 4 | **A4** Komut katmanı + menü + CLI + D&D | 4,0 | **G3 / 2** | 2 | A6 | A2'nin önkoşulu (K1) |
| 5 | **A2** Ara / Değiştir | 5,0 | **G3 / 2** | 3 | **A4 + A3** | Skoru en yüksek ikinci iş; önkoşulsuz yapılırsa iki kez yazılır |
| 6 | **A5** Diyalog taşması + yerel biçim | 4,5 | **G3 / 3** | 2 | — | **P1 borcu**; kapsamı (`DialogService`/`Messages`/CSS) hiçbir kapı işiyle kesişmez → en erken ve izole koşar |
| — | **A16** Kontrast kapsamı | 6,0 | — | 1 | — | **P1 kriter 1**'i kapatır; S eforla en yüksek getiri |
| — | **A8** Taslak yaşam döngüsü + gizlilik | 4,5 | — | 2 | — | Yayın öncesi sertleştirme: taslak tarayıcı keyfî dosya okumamalı |
| — | **A7** Yazar ergonomisi | 4,5 | — | 2 | A4 (menü) | Binici; kendi ayar yüzeyini menüden alır |
| — | **A17** Ses politikası | 6,0 | — | 1 | A4 (menü) | Binici; **ürünün adı** olan özellik |
| — | **D13** Ayar şema sürümü | — | — | 1 | A7 | K6: yeni ayar → göç borcu aynı PR'da |

### 3.3 Bağımlılık grafiği

```text
W0  A5 ─┐
    A16 ─┼──► G0'   (P1 borcu kapanır; CI'ya iki kapı eklenir: B14, B15)
    B14/B15 ─┘

W1  A6 ──► G1       (+ A8: yayın öncesi gizlilik sertleştirmesi)

W2  A3 ──► A1 ──► G2      (K4: aynı FileService sözleşmesi → sıralı, paralel değil)

W3  A4 ──► A2 ──► G3      (A2 ayrıca A3'e bağlı: K2, W2'den miras)

W4  D13 ──► A7 ──► A17 ──► v1.1.0 ──► G4
```

**Sert kısıtlar** (`analysis.md` §13.2): K1 (A2 ← A4), K2 (A2 ← A3), K4 (A1 ⟂ A3 sıralı),
K5 (A6 önce), K6 (yeni ayar → D13), K7 (yeni metin → 6 bundle + `Messages`).
**Kritik yol:** A6 → A3 → A1 → G2 (7 puan) ve A6 → A4 → A2 → G3 (7 puan); iki kol A3'te
birleşir (K2). İki geliştirici varsa W2'nin A3 kolu ile W3'ün A4 kolu **paralel** koşabilir;
A1 ve A2 sırasıyla kendi kolunun önkoşulunu bekler. Tek geliştiricide sıra yukarıdaki gibidir.

### 3.4 Kesme noktası

| Küme | Aday | Puan | Kapatılan kapı |
|---|---:|---:|---|
| Kapı işleri (A6, A3, A1, A4, A2, A5) | 6 | **14** | **5 / 5** |
| P1 borcunu kapatan ek (A16) | 1 | 1 | 0 — P1 kriteri 1'i kapatır |
| Yayın sertleştirme (A8) | 1 | 2 | 0 |
| Cila (A7, A17) | 2 | 3 | 0 |
| Havuz dışı zorunlu bağımlılık (D13) | — | 1 | 0 — K6 |
| **P2 (havuzdan)** | **10 / 16** | **20** | **5 / 5** |
| **P2 (D13 dahil)** | — | **21** | |
| Kesilen (§8) | 6 | **11** | 0 |
| Havuz toplamı | 16 | **31** | |

**Okunuşu:** Altı kapı işi havuzun **%45'ini** (14/31) tüketir ve **beş kapının beşini** kapatır.
P2'nin tamamı 20 puanla havuzun **%65'i**dir; kalan **%35 (11 puan) kesilir**. Kapı açmayan 6
puan üç şeyi satın alır: P1'in "kısmi" damgasını silmek (A16), yayından önce bir gizlilik açığını
kapatmak (A8), ürünün adındaki özelliği düzeltmek (A17) — ve bu 6 puanın 3'ü (A7, A17) bir
kapıyı beklerken boş kapasiteye biner. **P2, 20 puanda biter (D13 ile 21).**

---

## 4. P1'den Devreden Açıklar — Nerede Kapanıyor

`analysis.md` §3.2'ye göre P1'in yedi çıkış kriterinden **dördü kısmi, biri açık** kaldı
(kriter 3 ve 4 gerçekten kapandı). Bunlara §3.3'ün P2'ye devrettiği üç iş eklenince yedi satır
çıkıyor. Hiçbiri ayrı bir "borç dalgası" oluşturmuyor; her biri zaten gerekli bir karta
**gömülmüştür**. Bu tablo, o gömülmenin denetlenebilir kaydıdır.

| P1 açığı | Bulgu | Durum (4 Kas 2026) | Hangi kartta kapanır | Kapanış kanıtı |
|---|---|---|---|---|
| **9 `Alert` kelepçesiz/sarmasız** (kriter 2) | U1, M3 | **Kapalı** | **W0-01** | `DialogSizing` tek yol; `DialogSizingTest` + `DialogServiceTest` yeşil, `new Alert` çağrıları sarmalanmış (W0-01 `✅`) |
| **Sabit tarih/saat biçimi** (kriter 5) | U5, U12 | **Kapalı** | **W0-02** | Tüm biçimler `LocaleManager` üzerinden; `StatsBarModelTest` + `MessagesTest` yeşil ve suite TR/DE sistem dillerinde de koşuyor (W0-02 `✅`) |
| **Kontrast kapsamı eksik** (kriter 1) | U3 | **Kapalı** | **W0-03** | `ContrastRatioTest` 23/23; `-toolbar-text-muted` üç temadan da silindi (W0-03 `✅`) |
| **CI artefaktı çalıştırmıyor** (kriter 7 / PW-43) | B1 | **Kapalı** | **W1-01** | `smoke` job'u iki OS'te yeşil ([run 41711024985](https://github.com/hyperpostulate/JTypeWriter/actions/runs/41711024985)): Ubuntu'da imaj Xvfb altında açıldı, `.deb` kuruldu/açıldı/kaldırıldı ve `dpkg -l` temiz döndü; Windows'ta launcher ayakta kaldı |
| **`verify-artifacts.sh` CI'da değil** | B14 | **Kapalı** | **W0-04** | CI'da gerçekten koşuyor: [run 41732440723](https://github.com/hyperpostulate/JTypeWriter/actions/runs/41732440723) ubuntu ayağında `Verify packaged artifacts match the repository` **success** (17/17); kasıtlı bozma (`.deb`'den `NOTICE` silindi) yerelde 1/17 → exit 1. `CiGatesTest` kapıyı pinliyor |
| **İkon `--check` CI'da değil** | B15 | **Kapalı** | **W0-04** | Aynı koşuda her iki ayakta `Verify icon assets match the canonical artwork` **success**: 11 dosya; düzenlenmiş SVG / elle değiştirilmiş PNG / eksik dosya exit 1. `CiGatesTest` kapıyı pinliyor |
| **Sürüm yok / hat koşmadı** (kriter 6) | B2 | **Kapalı** | **W1-02, W1-03** | [run 41729981210](https://github.com/hyperpostulate/JTypeWriter/actions/runs/41729981210): `v1.0.0` yayında, **6 varlık** (5 + `SHA256SUMS`); beşi indirilip `sha256sum -c SHA256SUMS` ile doğrulandı → 5/5 OK; not iki dilli ve ölçülen boyutlarla üretildi. Hat artık hipotez değil: iki kuru koşu + bir gerçek yayın |

**Kural:** Bu tablodaki hiçbir satır "P2 sırasında bir ara" diye bırakılmaz. Her satırın bir kart
numarası vardır; kart kapanmadan satır kapanmaz. P1'in kısmi kapanış hatası (`analysis.md` §3.2
"kriter metni geniş, uygulama dar") **tekrar etmeyecek** biçimde burada sabitlenmiştir.

---

## 5. Dalgalar ve Kartlar

> **Kart şablonu:** her kart `Bulgu · Bağımlılık · Kapsam · Bitti tanımı · Doğrulama` taşır.
> `Bitti tanımı` maddeleri **gözlemlenebilir** olmak zorundadır; "iyileştirildi" gibi bir madde
> kabul edilmez (§6.1).

### W0 — "P1 borcunu kapat" · ~4 iş günü

Bu dalga kritik yolda **değildir** ama ilk yapılır: en ucuz, en izole, en yüksek getirili küme.
Hiçbir kart `FileService`'e dokunmaz, yani W2'nin hazırlığını kilitlemez.

**W0-01 · A5a — Dokuz `Alert`'i kelepçele** · M · U1, M3
- **Kapsam:** `service/DialogService.java`, `ui/DialogSizing.java`, `ui/DialogStyling.java`,
  `ui/MainController.java` (uyarı çağrıları), `css/*`
- **Yapılacak:** `DialogSizing`'in kelepçe matematiği tüm `Alert`'lere uygulanır; içerik sarılır
  (`Label.setWrapText(true)` + `setMaxWidth`), pencere yeniden boyutlandırılabilir olur. Dokuz
  tekrar tek bir yardımcıdan geçer (M3 de kapanır).
- **Bitti tanımı:** (1) Hiçbir `Alert` doğrudan `showAndWait()` çağırmıyor — hepsi
  `DialogService` üzerinden geçiyor (kaynak taramasıyla kanıtlanır). (2) En uzun DE/FR metniyle
  dokuz diyalog da 768 px genişlikte yatay kaydırma çubuğu üretmiyor.
- **Doğrulama:** `mvn test -Dtest=DialogSizingTest,DialogServiceTest` + `grep -rn "new Alert" src/main/java`
  çıktısının yalnız `DialogService`'i göstermesi.

**W0-02 · A5b — Tarih, saat ve ayırıcılar UI diline** · S · U5, U12
- **Kapsam:** `ui/component/StatsBarModel.java:51-52`, `ui/MainController.java:33,184`,
  `ui/component/ToolbarFit.java:93`, `locale/Messages.java`, 6 × `messages*.properties`
- **Yapılacak:** `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)` + `LocaleManager`
  yerel ayarı; `": "` ve `" - "` ayırıcıları bundle anahtarına taşınır (FR'de iki nokta öncesi boşluk
  gibi tipografik kurallar dile ait).
- **Bitti tanımı:** (1) Kaynakta tarih/saat deseni **literal olarak yok** (`grep -n "HH:mm\|yyyy"`).
  (2) Aynı an, TR ve DE locale'de farklı biçim üretiyor (test).
- **Doğrulama:** `mvn test -Dtest=StatsBarModelTest,MessagesTest` +
  `mvn test -Duser.language=de -Duser.country=DE` yeşil.

**W0-03 · A16 — Kontrast kapsamını kapat** · S · U3
- **Kapsam:** `css/*.css` (`-toolbar-text`, `-toolbar-text-muted` ölü token), `ContrastRatioTest`
- **Yapılacak:** Araç çubuğu metni, düz popup satırları ve varsayılan editör metni ölçüme
  katılır; ölü token silinir veya kullanılır.
- **Bitti tanımı:** Üç yüzey de AA eşiğini geçiyor; `-toolbar-text-muted` ya kullanılıyor ya
  yok; test sayısı 19 → ≥22.
- **Doğrulama:** `mvn test -Dtest=ContrastRatioTest`.

**W0-04 · B14/B15 — Kanıt kapılarını CI'ya taşı** · S · B14, B15
- **Kapsam:** `.github/workflows/maven.yml` (yeni adımlar), `scripts/verify-artifacts.sh`,
  `scripts/generate-icons.py`
- **Yapılacak:** `verify-artifacts.sh --skip-build` ve `generate-icons.py --check` her PR'da ve
  `main` push'unda koşar. `--check` iki ayakta da koşar (özet karşılaştırması, rasterleştirici
  gerekmez), `verify-artifacts` yalnız Linux'ta (dpkg-deb).
- **Uygulama notu (3 Kas 2026):** İlk hâli ikon kontrolünü Windows ayağına kilitlemişti, çünkü
  `--check` SVG'yi yeniden rasterleştirip bayt eşitliği arıyordu. İlk CI koşusunda Windows ayağı
  **kırıldı**: aynı pikseller, farklı ImageMagick/zlib sürümü → farklı PNG baytları. Kontrol artık
  `icons.manifest`'teki SHA-256'lara bakıyor; ImageMagick gerekmediği için iki ayakta da koşuyor.
  Piksel karşılaştırması `--render` olarak duruyor (yerel, yeniden üretim sonrası kanıt).
- **Bitti tanımı:** İki adım da yeşil; bir kasıtlı bozma denemesinde (ör. `.deb` payload'ından
  `NOTICE` silinmiş bir yamayla) iş **kırmızıya** dönüyor.
- **Doğrulama:** Bozma denemesi PR'ı (kanıt için ekran/CI logu) + sonrasında geri alınmış hâli.

> **Kapı G0′:** P1 kriter 1, 2, 5 tam kapalı; B14 ve B15 CI'da; `mvn -B clean test` yeşil.
> Bu kapı **puan saymaz** — kapıların ön koşuludur (`analysis.md` §12.3'teki ayrım).

---

### W1 — "Yayınlanabilir" · ~6 iş günü

Bu dalganın tek amacı: **"paketlendi" ile "çalışıyor" arasındaki kanıtı üretmek ve sürümü
yayınlamak.** Yayınlanana kadar diğer bütün işler tahmindir (`analysis.md` §4.4).

**W1-01 · A6a — CI artefakt smoke testi** · M · B1
- **Kapsam:** `scripts/smoke-appimage.sh` (yeni), `.github/workflows/maven.yml` (yeni `smoke` job)
- **Yapılacak:** Linux: boş `user.home` + `Xvfb` altında app image başlatılır, `xwininfo` ile ana
  pencere beklenir, süreç kapatılır; ardından `.deb` kurulur, başlatılır, kaldırılır. Windows:
  image başlatılır, süreç N saniye ayakta kalır ve `jpackage` launcher'ı sıfırdan farklı kodla
  çıkmaz.
- **Bitti tanımı:** (1) `smoke` job iki OS'te yeşil. (2) Betik yerelde de koşuyor
  (`bash scripts/smoke-appimage.sh --skip-build`). (3) Kurulum sonrası `dpkg -l | grep jtypewriter`
  boş dönüyor (kaldırma gerçekten oldu).
- **Doğrulama:** CI run URL'i + yerel çalıştırma çıktısı.
- **Not:** `retention-days ≤ 7` zorunlu (`ReleaseNotesTest`).
- **Uygulama notu (3 Kas 2026):** `smoke` job'u build job'unun **yüklediği** artefaktı indirip
  `smoke-appimage.sh --skip-build` çalıştırır; ikinci kez paketlemek "paketleme aynı runner'da
  iki kez tekrarlanabiliyor"u kanıtlardı, "indirilen artefakt çalışıyor"u değil. Zip artefaktları
  Unix dosya kiplerini düşürdüğü için başlatıcı ve runtime'ın spawn ettiği parçalar için hedefli
  bir `chmod` adımı var; blanket `chmod` yok, çünkü hiç çalıştırılabilir olmayan bir başlatıcı
  yine kırmızı olmalı. Pencere sorgusu `"JTypeWriter - "` önekiyle eşleşiyor: yalnız uygulama adı
  arandığında, yeniden kullanılan bir geliştirici ekranındaki tarayıcı/IDE penceresi (yolunda
  proje adı geçtiği için) uygulama açılmadan "pencere bulundu" diyordu.
- **İlk CI koşusunun bulduğu kusur (3 Kas 2026):** Ubuntu ayağı `dpkg -i`'de kırmızıya döndü.
  Sebep uygulama değil, `jpackage`'ın ürettiği `postinst`/`prerm`: `set -e` altında
  `xdg-desktop-menu`'yu korumasız çağırıyorlar ve XDG menü dizini yazılabilir olmayan her makinede
  (CI runner, sunucu, container) `exit 3` alıp dpkg işlemini düşürüyor, paketi yarım
  yapılandırılmış bırakıyorlar. Yani normal kullanıcı da headless/minimal kurulumda aynı duvara
  çarpardı. `package.sh` bu iki betiğe yalnız `xdg-desktop-menu` satırları için `|| true` ekliyor
  (`relax_desktop_menu_registration`): çağrılar silinmediği için masaüstünde menü kaydı aynen
  sürüyor, yorum satırları atlanıyor ve ikinci rebuild suffix'i çoğaltmıyor. `CiGatesTest` bunu
  ve `smoke` job'unun `needs: build` + `--skip-build` sözleşmesini kilitliyor.
  `--open-sample` bayrağı bu karta **eklenmedi**: argümanlı açılış W3-03'te geliyor, bayrak da
  onunla birlikte.

**W1-02 · A6b — Release hattını yayından önce kurut** · S · B2
- **Kapsam:** `.github/workflows/release.yml`
- **Yapılacak:** `workflow_dispatch` + `dry_run` girdisi: `prepare` ve iki build ayağı aynen
  koşar, `release` job'u yayın yerine artefakt yükler. Böylece ilk gerçek yayın sürpriz olmaz.
- **Bitti tanımı:** (1) `dry_run` koşusu uçtan uca yeşil. (2) Tag↔pom uyuşmazlığı hâlâ
  reddediliyor. (3) Beklenen varlık assert'i **5 varlık** ile eşleşiyor (macOS sonrası, §1.2).
- **Doğrulama:** `dry_run` run URL'i; `git tag -l` hâlâ boş.
- **Uygulama notu (3 Kas 2026):** `dry_run` varsayılanı `true`; `prepare` artık `publish`
  çıktısını üretiyor (`push` → `true`, `workflow_dispatch` → `false`) ve elle başlatılan bir
  koşuda `dry_run=false` istenirse iki OS derlemesi başlamadan reddediyor. Sebep, dosyanın
  başındaki asıl iddia: dal ucundan üretilen bir sürüm `$GITHUB_REF_NAME`'den yeniden üretilemez,
  yani elle yayımlama "kanıtlanabilir sürüm" iddiasını bozar. Yayımlama adımı silinmedi,
  `publish == 'true'` koşuluna bağlandı; kuru koşuda yerine `dist/` yükleniyor
  (`JTypeWriter-release-dry-run-<version>`, `retention-days: 7`).
  İki küçük tasarım kararı: (a) kaynak arşivi artık `$GITHUB_SHA`'dan kesiliyor, çünkü kuru
  koşuda tag henüz yok; gerçek yolda tag push'u o SHA'yı tag'lenmiş commit yaptığı için içerik
  aynı ve tek kod yolu kalıyor. (b) `@TAG@` yerine `prepare.tag` çıktısı kullanılıyor, böylece
  kuru koşunun notu da gerçek notla aynı biçimde üretiliyor. Varlık assert'i yayın/kuru-koşu
  ayrımından **önce** duruyor: kuru koşunun kanıtlaması gereken şeylerden biri de odur.
  `ReleaseNotesTest` sözleşmeyi kilitliyor (3 yeni test: dispatch + guard'lar, elle yayımlama
  reddi, tam 5 varlık), `retention-days ≤ 7` kontrolü yeni yüklemeyi de kapsıyor.

- **Kuru koşunun bulduğu ikinci kusur (4 Kas 2026):** Ubuntu ayağı yeşilken Windows ayağı
  `Stage normalised release assets` adımında kırmızıydı:
  `tar (child): Cannot connect to D: resolve failed`. Sebep yol değil, **iki dilbilgisinin
  çakışması**: GNU tar arşiv argümanında `host:path` biçimini uzak kopya sanıyor ve Windows sürücü
  harfi tam o biçimde — `D:\a\...` yazılmadan önce `D` host'u olarak okunuyor. Kusur
  `release-assets.sh`'teydi ve **hiçbir yerel koşuda görünmüyordu**, çünkü mutlak bir yolda
  iki noktadan önce eğik çizgi bulunuyor ve tar o yolu yerel sayıyor; hata yalnız `D:` ile
  başlayan göreli biçimde tetikleniyor (bu yüzden Linux'ta `release-assets.sh` defalarca
  sorunsuz koştu).
  Düzeltme: `to_tar_path` — iki nokta içeren her yol, `cygpath -u` varsa POSIX biçimine çevriliyor
  (`/d/a/...`, iki nokta kalmıyor); Linux'ta `cygpath` yok, satır aynen koşuyor, yani tek
  uygulama kalıyor. Yeni `ReleaseAssetsScriptTest` (2 test) kusuru POSIX'te yeniden üretiyor:
  sahte `cygpath` + `D:` ile başlayan göreli yollarla arşiv üretiliyor; dönüşüm kaldırıldığında
  test **birebir aynı CI hatasıyla** ("Cannot connect to D") kırmızıya dönüyor. Bu, W1-02'nin
  neden var olduğunun en somut kanıtı: hat hiç koşmamış olsaydı bu kusur ilk gerçek tag'de,
  yayın ortasında çıkacaktı.

- **Kapanış (4 Kas 2026):** [run 41725168564](https://github.com/hyperpostulate/JTypeWriter/actions/runs/41725168564)
  dört job'u da yeşil bitirdi. `Assemble and publish (or stage a dry run)` içinde
  `Create the GitHub Release` **skipped**, `Upload the dry-run release bundle` **success**;
  `Verify the expected assets are present` yeşil (5 varlık), `Write release notes` yeşil.
  Artefakt: `JTypeWriter-release-dry-run-1.0.0`, 229.733.257 bayt, 7 gün saklama.
  `git tag -l` boş ve `GET /releases` uzunluğu 0 — kuru koşu hiçbir şey yayımlamadı.

**W1-03 · A6c — İlk yayın `v1.0.0` (pre-release)** · S · B2
- **Kapsam:** tag + `docs/release-notes-template.md` (yalnız doldurulur, yapısı değişmez)
- **Yapılacak:** `v1.0.0` etiketi; iki dilli not; "erken erişim" işareti; imza yokluğu dürüstçe
  yazılı; ölçülen artefakt boyutları notta.
- **Bitti tanımı:** Release'te **5 varlık + `SHA256SUMS`**; not iki dilli ve `@VERSION@`/`@TAG@`
  doldurulmuş; `SHA256SUMS` doğrulanabiliyor.
- **Doğrulama:** `curl -s https://api.github.com/repos/hyperpostulate/JTypeWriter/releases |
  jq '.[0].assets[].name'` → 6 satır. (`gh` bu makinede kurulu değil.)
- **Uygulama notu (4 Kas 2026):** Not artık kartın istediği üç noktayı da karşılıyor; boyutlar
  ise elle yazılmıyor.
  (a) Her iki dilde **erken erişim** işareti: "pre-release: usable, but the feature set can
  still change".
  (b) **Ölçülen indirme boyutları**: şablon `@ASSET_SIZES@` yer tutucusunu taşıyor ve
  `release.yml` onu `dist/` içindeki gerçek dosyalardan üretiyor (`stat -c %s` → MiB,
  `LC_ALL=C` ile ondalık ayırıcı sabit). İki sebep: Windows varlıkları Windows runner'da
  üretilir ve bu makinede **hiç ölçülemedi** (`analysis.md` §134 bu boşluğu zaten kaydediyor;
  uydurulmuş bir sayı notu yalancı yapardı), ayrıca elle yazılan bir sayı bir sonraki sürümde
  sessizce yanlış olur. Aynı liste iki yarıda da basılıyor (yer tutucu her görüldüğünde dosya
  yeniden okunuyor), yani hiçbir dil indirme boyutunu saklamıyor.
  (c) İmza yokluğu cümlesi korundu; zaten dürüsttü.
  `ReleaseNotesTest` 7 → 8 test: iki dilde erken erişim işareti, iki yarımda yer tutucu,
  `release.yml`'in boyutu ölçtüğü ve MiB'e çevirdiği, ölçümün "yer tutucu kaldı" kontrolünden
  **önce** koştuğu. Mutasyonla doğrulandı: ikinci yer tutucu silinince ve `stat` satırı
  kaldırılınca test kırmızı. Yerel render sahte bir `dist/` ile koşuldu: iki başlık yerinde,
  yer tutucu kalmıyor, beş varlık MiB cinsinden listeleniyor.
- **Kapanış (4 Kas 2026):** [run 41729981210](https://github.com/hyperpostulate/JTypeWriter/actions/runs/41729981210)
  (`push`, tag `v1.0.0`) dört job'u da yeşil bitirdi; Release yayımlandı ve **6 varlık** var:
  `linux-x64.deb` (49.977.836 B), `linux-x64.tar.gz` (62.654.421 B), `windows-x64.msi`
  (59.769.452 B), `windows-x64.tar.gz` (58.060.848 B), `source.tar.gz` (590.889 B) ve
  `SHA256SUMS` (500 B).
  Not, ölçülen boyutlarla **iki dilde** üretildi (`47.7 / 59.8 / 57.0 / 55.4 / 0.6 MiB`) —
  yani `@ASSET_SIZES@` gerçekten yayımlanan dosyalardan doldu, elle yazılmış bir sayı yok.
  Doğrulama: beş varlık indirilip `sha256sum -c SHA256SUMS` ile karşılaştırıldı → **5/5 OK**;
  kaynak arşivi `JTypeWriter-1.0.0/` tek üst dizinini taşıyor; taşınabilir arşivde `LICENSE`,
  `NOTICE`, `THIRD-PARTY-NOTICES` var.
- **Kapanışın bulduğu kusur (4 Kas 2026):** Release yayımlandı ama GitHub onu
  `prerelease=false` ile **"Latest release"** olarak etiketledi; notun kendisi ise "pre-release"
  diyordu. Yani aynı sürüm için sayfa ile gövdesi çelişiyordu — üstelik W4-05 "pre-release
  işaretini kaldır" diyor, yani işaret hiç kurulmamış olsaydı o kart da anlamsız kalırdı.
  Düzeltme `release.yml`'de: `gh release create` artık `--prerelease` taşıyor (yorumu,
  kaldırmanın bilinçli bir eylem ve W4-05'in işi olduğunu söylüyor).
  `ReleaseNotesTest` 8 → 9 test bunu kilitliyor; mutasyonla doğrulandı (satır silinince kırmızı).
  Mevcut `v1.0.0` de elle pre-release olarak işaretlendi (`GET /releases` →
  `prerelease=true`), yani yayındaki sürümle not artık aynı şeyi söylüyor.

**W1-04 · A8a — Taslak tarayıcısını sertleştir** · M · D4, D5, D14
- **Kapsam:** `service/RecoveryStore.java:131-139,145,271`, `service/BackupKeeper.java`
- **Yapılacak:** Tarama symlink takip etmez (`NOFOLLOW_LINKS`); taslak okumasına boyut sınırı;
  `readAllBytes` yerine sınırlı akış; `parked-*.jtw` için yaş/sayı/boyut temizliği; disk
  çarpanı (4,00× kalıcı, ≈6× tepe) kullanıcıya bir kez söylenir.
- **Bitti tanımı:** (1) Symlink'li taslak adayı **okunmuyor** (test). (2) 100 MB'lık bir taslak
  dosyası kurtarma taramasını bloke etmiyor (test, sınırlı okuma). (3) `parked-*` temizliği
  yaş eşiğiyle çalışıyor (test).
- **Doğrulama:** `mvn test -Dtest=RecoveryStoreTest,BackupKeeperTest`.
- **Neden W1'de:** Yayın öncesi gizlilik hijyeni — keyfî bir dosya yoluna işaret eden symlink'in
  içeriği kullanıcıya "kurtarılmış taslak" olarak sunulmamalı. Bir kapı kapatmaz, ama **yayın
  öncesi** olması gerekçelidir.

> **Kapı G1:** GitHub'da bir Release var; CI o Release'in imajını **açtı**, `.deb`'i **kurup
> kaldırdı**; `verify-artifacts` ve ikon `--check` CI'da yeşil; `SHA256SUMS` doğrulanıyor.

---

### W2 — "Donmayan ve ezmeyen editör" · ~9 iş günü

Sıra **serttir** (K4): A3 ve A1 aynı `FileService` sözleşmesine dokunur, paralel yazılmaz.
A3 önce, çünkü A2'nin önkoşulu ve ölçülmüş darboğaz orada.

**W2-01 · A3a — İstatistiği artımlı yap** · M · X5, X3
- **Kapsam:** `stats/SessionStats.java` (112 satır, JavaFX'siz), `SessionStatsTest`
- **Yapılacak:** Kelime/satır/boşluksuz karakter sayıları artımlı sayaçla tutulur; **public API
  korunur** (getter imzaları değişmez), böylece `StatsBarModel` dokunulmaz. Deltayı
  `EditorArea`'nın metin dinleyicisi besler; `SessionStats`'a JavaFX tipi **sızmaz** (küçük bir
  `before/after` çifti ya da offset+uzunluk kaydı). `getText()` kopyası yenileme yolundan çıkar.
- **Bitti tanımı:** (1) 10 MB metinde tek yenileme **< 5 ms** (referans makine). (2) Ekleme,
  silme, değiştirme, tümünü değiştirme ve dosya yükleme sonrası sayaçlar tam metin taramasıyla
  **birebir** aynı (property-style karşılaştırma testi). (3) `SessionStatsTest` (12) yeşil ve
  ≥6 test eklendi.
- **Doğrulama:** `StatsPerfHarness` (yeni, `src/test/java`) çıktısı; CI'da eşik **50 ms**
  (O(n)'e geri dönüş 163 ms → yakalanır), referans makinede eşik **5 ms**.

**W2-02 · A3b — G/Ç'yi FX thread'inden çıkar** · M · X1, X2
- **Kapsam:** `service/FileService.java`, `service/AutoSaveService.java`, yeni `IoExecutor`
- **Yapılacak:** Açma/kaydetme/otomatik kayıt `Task`/`ExecutorService` üzerinde; UI'da
  "kaydediliyor" durumu; otomatik kayıtta `getText()` **kirli kontrolünden sonra** alınır.
- **Bitti tanımı:** (1) FX thread kimliğinde yapılan `FileChannel`/`fsync` çağrısı **sıfır**
  (test: G/Ç sarmalayıcısına konan sayaç + `Platform.isFxApplicationThread()`).
  (2) Kaydetme sırasında arayüz donmuyor (elle: 10 MB belgede sürükleme akıcı).
  (3) Otomatik kayıt kirli değilken metin kopyası **almıyor** (test).
- **Doğrulama:** `mvn test -Dtest=FileServiceTest,AutoSaveServiceTest` +
  yeni `FxThreadIoTest`.

**W2-03 · A3c — Bellek, boyut ve hata yolu** · M · X4, M10, D7, D14
- **Kapsam:** `FileService.loadFile`, `AtomicFiles`, `AutoSaveService.java:317`
- **Yapılacak:** `readAllBytes` yerine boyut denetimli akış; üst sınır aşılırsa **açık uyarı**
  (sessiz kısaltma yok); `Error` da kaydetme hata yolundan geçer; disk alanı ön kontrolü.
- **Bitti tanımı:** (1) Sınır üstü dosya açılmıyor ve kullanıcıya **ne olduğu** söyleniyor
  (6 dilde metin). (2) `OutOfMemoryError` benzeri `Error` senaryosunda taslak korunuyor (test).
  (3) Disk dolu senaryosu (temp `user.home` + kotasız FS simülasyonu) kaydetmeyi "başarılı"
  göstermiyor.
- **Doğrulama:** `mvn test -Dtest=FileServiceTest,AtomicFilesTest` + yeni testler.

**W2-04 · A1a — Dış değişikliği yakala** · M · D1, M9
- **Kapsam:** `service/FileService.java:69-74,296,310`, `AtomicFiles`
- **Yapılacak:** Yüklemede `(mtime, boyut, içerik SHA-256)` saklanır; kaydetmeden hemen önce
  tekrar okunur; çelişkide **üç seçenekli** diyalog: üzerine yaz / yeni ada kaydet / vazgeç.
- **Bitti tanımı:** (1) Diskteki dosya yüklemeden sonra değişirse kaydetme **ya sorar ya
  reddeder** — sessiz üzerine yazma yok (test). (2) "Yeni ada kaydet" `ExitDisposition`'ı
  atlamaz. (3) Aynı saniye içinde yapılan değişiklik de yakalanıyor (hash, mtime'a bağımlı
  değil).
- **Doğrulama:** `mvn test -Dtest=FileServiceTest` (yeni `externalChange*` testleri).

**W2-05 · A1b — Symlink'i koru** · S · D2
- **Kapsam:** `AtomicFiles.java:54-74`
- **Yapılacak:** Hedef `toRealPath()` ile çözülür; yazma gerçek hedefe, atomik taşıma hedefin
  dizininde; `.bak` gerçek hedefin yanına.
- **Bitti tanımı:** Symlink'li belge kaydedildikten sonra **hâlâ symlink** ve hedefi
  güncellenmiş (bugün kusur: link yok oluyor).
- **Doğrulama:** `mvn test -Dtest=AtomicFilesTest` (yeni `symlink*` testi) + `analysis.md`
  §15.4'teki manuel deney.

**W2-06 · A1c — Taslak yolu ve belge kilidi** · M · D3, D8
- **Kapsam:** `RecoveryStore.java:333-344`, `InstanceLock`
- **Yapılacak:** Taslak başlığındaki yol mutlaklık + kanoniklik + izinli kök açısından
  doğrulanır; geçersizse kurtarma **hedefsiz** açılır (Farklı Kaydet'e zorlar). Belge düzeyinde
  `FileLock`; `UNAVAILABLE` ise uyarı, sessiz devam yok.
- **Bitti tanımı:** Geçersiz yol hedef olarak kabul edilmiyor (test); iki süreç aynı dosyayı
  açtığında ikincisi uyarıyor (test, `ProcessCrashRecoveryTest` deseniyle fork).
- **Doğrulama:** `mvn test -Dtest=RecoveryStoreTest,InstanceLockTest`.

> **Kapı G2** (`analysis.md` §13.6/1 ve /4):
> (a) Üzerine yazma güvenliği üç maddesi testli; (b) 10 MB'da istatistik yenilemesi < 5 ms
> (CI'da < 50 ms) ve FX thread'inde G/Ç yok.

---

### W3 — "Editör yüzeyi" · ~9 iş günü

**W3-01 · A4a — `EditorAction` komut katmanı** · M · M1, U13
- **Kapsam:** yeni `ui/EditorAction`, `KeyboardShortcutHandler`, `ToolbarView`, `MainController`,
  `ShortcutRows`, `ShortcutTable`
- **Yapılacak:** 16 kısayol + araç çubuğu düğmeleri tek kaynaktan (kimlik + etiket anahtarı +
  hızlandırıcı + çalıştırma). Kısayol listesi bu kaynaktan **üretilir** → U13 (liste ile
  handler iki yönde uyuşmuyor) yapısal olarak imkânsız hâle gelir.
- **Bitti tanımı:** (1) `resolve()` tablosu ve `ShortcutRows` aynı kaynaktan besleniyor; iki
  yönlü uyuşmazlık testi yeşil. (2) Hiçbir eylem iki yerde tanımlı değil (`grep` kanıtı).
- **Doğrulama:** `mvn test -Dtest=KeyboardShortcutHandlerTest,ShortcutRowsTest`.

**W3-02 · A4b — Menü çubuğu** · M · E2
- **Kapsam:** `MainStage`, yeni `ui/MainMenuBar`, `Messages`, 6 bundle
- **Yapılacak:** Dosya / Düzen / Görünüm / Yardım. **`Yeni` ve `Farklı Kaydet` görünür olur**
  (bugün 14 eylemin 2'si arayüzde yok). Etiketler bundle'dan, hızlandırıcılar komut katmanından.
- **Bitti tanımı:** (1) Menüdeki her öğe bir `EditorAction`'a bağlı (test: menü ağacı ↔ komut
  kümesi birebir). (2) Altı dilde menü etiketleri var (`MessagesTest` yeşil).
- **Doğrulama:** `mvn test -Dtest=MainStageTest,MainControllerTest,MessagesTest`.

**W3-03 · A4c — CLI / süreç argümanı** · S · E3
- **Kapsam:** `JTypeWriterApp`/`FxApplication` (`getParameters()`), `package.sh` launcher
- **Yapılacak:** `JTypeWriter <dosya>` belgeyi açar; çift tıklama yolu aynı kodu kullanır.
  İkinci örnek devir **yapılmaz** (kilit reddeder, mevcut davranış).
- **Bitti tanımı:** Argümanlı başlatmada dosya açılıyor; argüman yoksa karşılama; geçersiz yol
  sessizce yutulmuyor.
- **Doğrulama:** `scripts/smoke-appimage.sh --open-sample` (W1-01'e eklenir) +
  `mvn test -Dtest=JTypeWriterAppTests`.

**W3-04 · A4d — Sürükle-bırak** · S · E4
- **Kapsam:** `MainStage`/`EditorArea` (`Dragboard`)
- **Yapılacak:** `.txt`/`.md` bırakma; kirli belgede **mevcut çıkış matrisinden** geçer — yeni
  kaydetme yolu icat edilmez.
- **Bitti tanımı:** Kirli belgeye bırakma `ExitDisposition` üzerinden soruyor (test, saf model
  düzeyinde).
- **Doğrulama:** `mvn test -Dtest=ExitDispositionTest,MainStageTest`.

**W3-05 · A2a — `FindModel` (saf model)** · M · E1
- **Kapsam:** yeni `ui/FindModel` (JavaFX'siz)
- **Yapılacak:** Eşleşme listesi, ileri/geri, büyük-küçük harf duyarlılığı, tümünü değiştir,
  sarmalama; `TextCodec`/`SessionStats` gibi toolkit'siz ve testli.
- **Bitti tanımı:** Model toolkit açmadan test ediliyor; boş desen, regex yok, taşan aralık,
  değiştir-sonrası-kayma senaryoları testli.
- **Doğrulama:** `mvn test -Dtest=FindModelTest` (yeni).

**W3-06 · A2b — Arama/değiştirme paneli** · M · E1
- **Kapsam:** yeni panel + `Ctrl+F` / `F3` / `Shift+F3` / `Ctrl+H` + menü öğeleri
- **Yapılacak:** Panel araması FX thread'inde **tam metin taraması yapmaz** (W2-01/W2-02'nin
  üstüne kurulur); YOLO açıkken "tümünü değiştir" **onay ister**.
- **Bitti tanımı:** (1) 10 MB belgede arama UI'yı kilitlemiyor (ölçülen süre kaydedilir).
  (2) YOLO etkileşimi testli (`YoloPolicy` ile aynı dil). (3) Aynı işlevler menüden de erişilir.
- **Doğrulama:** `mvn test -Dtest=FindModelTest,YoloPolicyTest` + manuel 10 MB ölçümü.

> **Kapı G3** (`analysis.md` §13.6/2 ve /3): `Ctrl+F` bulur, `F3`/`Shift+F3` gezer, `Ctrl+H`
> bir/tümünü değiştirir; aynı işlevler menüden erişilir; arama 10 MB'da kilitlenmez; tüm
> tarih/saat gösterimi UI dilini kullanır; bütün `Alert`'ler 768 px'e sığar (W0-01'den miras).

---

### W4 — "Cila, ses ve sürüm" · ~6 iş günü

**W4-01 · D13 — Ayar şeması sürümü** · S · D13, D12
- **Kapsam:** `AppSettings.java:67-81,300-308`
- **Yapılacak:** Ayarlara `schemaVersion` eklenir; bilinmeyen/eksik alan göçü tanımlı; yazma
  hatası sessiz kalmaz (`SecurityException` yakalanır ve kullanıcıya söylenir).
- **Bitti tanımı:** Eski şema dosyası yeni sürümle açılıyor ve **veri kaybetmiyor** (test);
  yazılamayan ayar kullanıcıya bildiriliyor.
- **Doğrulama:** `mvn test -Dtest=AppSettingsTest` (yeni göç testleri).
- **Neden ilk:** K6 — A7'nin eklediği her ayar bu kartı tetikler; önce yapılmazsa göç borcu
  sessizce büyür.

**W4-02 · A7a — Son dosyalar + karşılama opt-out** · M · E5, U7
- **Kapsam:** `AppSettings`, `WelcomeDialog`, `FxApplication:70`, `MainMenuBar` (Dosya menüsü)
- **Yapılacak:** En çok 10 son dosya (silinmişse sessizce düşer); karşılamada "bir daha
  gösterme" (ayar olarak).
- **Bitti tanımı:** Silinmiş dosya listede kalıcı hata üretmiyor (test); opt-out sonrası
  karşılama açılmıyor (test, saf model).
- **Doğrulama:** `mvn test -Dtest=AppSettingsTest,MainControllerTest`.

**W4-03 · A7b — Minimum pencere genişliği** · S · U8
- **Kapsam:** `MainStage.java:53-54`, `ToolbarFit`
- **Yapılacak:** `setMinWidth`; araç çubuğu kırpılması biter.
- **Bitti tanımı:** Minimum genişlikte hiçbir araç çubuğu öğesi kırpılmıyor (`ToolbarFit`
  testiyle).
- **Doğrulama:** `mvn test -Dtest=ToolbarFitTest,MainStageTest`.

**W4-04 · A17 — Ses politikası** · S · U9
- **Kapsam:** `ui/component/EditorKeyHandler.java:31-40`, `audio/TypewriterSound`,
  `MainMenuBar` (Görünüm → Ses), `AppSettings`
- **Yapılacak:** Ses yalnız **metin üreten** tuşlarda; ok/kısayol tuşları sessiz; ses düzeyi
  ayarı; hızlı yazımda `stop()+play()` yerine kesmesiz çalma.
- **Bitti tanımı:** (1) Ok tuşu ve kısayol ses çıkarmıyor (saf politika testi). (2) Düzey
  ayarı kalıcı ve 0'da tam sessiz. (3) 10 tuş/sn hızında ses kesilmiyor (elle, kayıtla).
- **Doğrulama:** `mvn test -Dtest=AudioAssetTest` + yeni `SoundPolicyTest`.

**W4-05 — `v1.1.0` yayını** · S · B2
- **Yapılacak:** W1-03'ün tekrarı; bu kez **pre-release işareti kaldırılır** — işaret `release.yml`'deki `gh release create ... --prerelease` satırıdır, kart o satırı kaldırır (`ReleaseNotesTest` bu satırı pinliyor; kart onu da güncellemeli).
- **Bitti tanımı:** Release'te 6 varlık; notlar iki dilli; `SHA256SUMS` doğrulanıyor.
- **Doğrulama:** W1-03 ile aynı API sorgusu.

> **Kapı G4:** Tam suite yeşil, `verify-artifacts` 17/17, ikon `--check` 11 dosya,
> `v1.1.0` yayında, §5'teki tüm kartlar `✅`.

---

## 6. İzleme Protokolü — "Bitti" Nasıl Anlaşılır

### 6.1 Kartın bitti sayılma kuralları (her kart için geçerli)

Bir kart ancak **altı koşul birden** sağlanınca `✅` olur:

1. **Önce kırmızı test.** Bulgu kimliğini taşıyan ve düzeltme olmadan **başarısız olan** bir test
   yazıldı (`analysis.md` §6.4 dersi: testi olmayan düzeltme geri gelir).
2. **Suite yeşil, repo kökünden.** `mvn -B clean test` → 350 + eklenen testler, 0 hata.
3. **Değişmezler korundu** (§6.3).
4. **Yeni görünür metin varsa** 6 bundle + `Messages` erişimcisi aynı PR'da (`MessagesTest`).
5. **Kullanıcıya görünen değişiklik varsa** `README.md` + `README-TR.md` aynı PR'da.
6. **Kanıt satırı commit mesajında:** `Refs: W2-05 · Kanıt: mvn test -Dtest=AtomicFilesTest → 14/14`.
   Kanıtı olmayan kart, kodu yazılmış olsa da `◐` (WIP) kalır.

### 6.2 Durum işaretleri

| İşaret | Anlamı |
|---|---|
| `☐` | Başlamadı |
| `◐` | Başladı, kanıt yok |
| `⛔` | Bloke — engel tek satırda yazılır (kim/ne bekleniyor) |
| `✅` | Kanıtlı bitti (koşul 1–6 sağlandı) |

### 6.3 Her PR'da korunacak değişmezler (testler zorlar)

| Değişmez | Zorlayan test | Bu yol haritasında risk |
|---|---|---|
| SPDX başlığı byte 0'da | `LicenseHeaderTest` | Yeni sınıflar (`FindModel`, `EditorAction`, `IoExecutor`) |
| 6 bundle × aynı anahtar kümesi, iki yönlü erişim | `MessagesTest` | W0-01/02, W1-04, W3-02, W4 metinleri |
| `THIRD-PARTY-NOTICES` bayt-ödeş + LF + 100 kolon | `PackagingNoticesTest` | Bağımlılık değişirse |
| Sürüm notu iki dilli, `@VERSION@`/`@TAG@`, semver yok | `ReleaseNotesTest` | W1-03, W4-05 |
| `upload-artifact` → `retention-days ≤ 7` | `ReleaseNotesTest` | W1-01, W1-02 |
| İkon sihirli baytları (PNG/ICO) | `AppIconsTest` | Yalnız `--check` CI'ya taşınır (W0-04) |
| Toolkit açılmayan test | tüm suite | `FindModel`, `SoundPolicy` saf kalmalı |

### 6.4 İzleme tablosu (kopyala, PR'larda güncelle)

| ID | Kart | Bulgu | Efor | Bağımlılık | Durum | Kanıt |
|---|---|---|---|---|---|---|
| W0-01 | Dokuz `Alert` kelepçesi | U1, M3 | M | — | ✅ | `mvn test -Dtest=DialogSizingTest,DialogServiceTest` → 18/18; `grep -rn "new Alert" src/main/java` → yalnız `DialogService`; `mvn -B clean test` → 354/354 |
| W0-02 | Tarih/saat + ayırıcı yerelleştirmesi | U5, U12 | S | — | ✅ | `mvn test -Dtest=StatsBarModelTest,MessagesTest` → 25/25; `grep -rn "HH:mm\|yyyy" src/main/java` → boş; `mvn -B clean test` → 357/357; `mvn -B test -Duser.language=de -Duser.country=DE` → 357/357 |
| W0-03 | Kontrast kapsamı | U3 | S | — | ✅ | `mvn test -Dtest=ContrastRatioTest` → 23/23; `-toolbar-text-muted` üç temadan da silindi, `everyThemeTokenIsActuallyReadByBaseCss` ölü token'ı yakalıyor; `mvn -B clean test` → 361/361; `mvn -B test -Duser.language=de -Duser.country=DE` → 361/361 |
| W0-04 | `verify-artifacts` + ikon `--check` CI'da | B14, B15 | S | — | ✅ | `bash scripts/verify-artifacts.sh` → 17/17 (linux, 1.0.0); kasıtlı bozma (`.deb`'den `NOTICE` silindi) → 1/17, exit 1, sonra `package.sh --skip-build --installer` ile geri alındı → 17/17; `python3 scripts/generate-icons.py --check` → 11 dosya (manifest dahil), düzenlenmiş SVG / değiştirilmiş PNG / eksik PNG → exit 1, `--render` → piksel özdeş, palet değişikliği %0,33 → exit 1, ImageMagick'siz `PATH`'te `--check` yeşil; `mvn test -Dtest=CiGatesTest` → 5/5; `mvn -B clean test` → 366/366 |
| W1-01 | CI artefakt smoke | B1 | M | W0-04 | ✅ | CI [run 41711024985](https://github.com/hyperpostulate/JTypeWriter/actions/runs/41711024985): `Smoke test packaged app (ubuntu-latest)` ve `(windows-latest)` **success** (imaj açıldı, `.deb` kuruldu/çalıştırıldı/kaldırıldı, `dpkg -l` temiz); ilk koşu `7e474c1` Ubuntu'da `xdg-desktop-menu` exit 3 → dpkg abort ile kırmızıydı, `25ca53e` bunu `package.sh`'te onardı; `mvn -B clean test` → 368/368 (CiGatesTest 7/7, `needs: build` kaldırılınca kırmızı); `verify-artifacts.sh --skip-build` → 17/17; `smoke-appimage.sh --skip-build` yerelde imajı 2 s'de pencereyle açtı |
| W1-02 | Release dry-run | B2 | S | — | ✅ | Kuru koşu yeşil: [run 41725168564](https://github.com/hyperpostulate/JTypeWriter/actions/runs/41725168564) — 4/4 job success (`Assemble and publish` içinde `Create the GitHub Release` **skipped**, `Upload the dry-run release bundle` success); artefakt `JTypeWriter-release-dry-run-1.0.0` (7 gün, 229,7 MB); `Verify the expected assets are present` yeşil → tam 5 varlık; `git tag -l` boş, Release sayısı 0 → kuru koşu yayımlamıyor. Hat iki gerçek kusur buldu ve ikisi de onarıldı: (a) `xdg-desktop-menu`/dpkg (W1-01), (b) GNU tar'ın `D:` yolunu `host:path` sanması (`release-assets.sh` `to_tar_path` + `ReleaseAssetsScriptTest`). Yerel: `prepare` dört senaryoda doğru (`v0.9.9` → exit 1, dispatch `dry_run=false` → exit 1); `mvn -B clean test` → 373/373 |
| W1-03 | `v1.0.0` pre-release | B2 | S | W1-01, W1-02 | ✅ | Yayın yapıldı: [run 41729981210](https://github.com/hyperpostulate/JTypeWriter/actions/runs/41729981210) (4/4 job), Release'te **6 varlık** — 5 ikili/kaynak + `SHA256SUMS`; beşi indirilip `sha256sum -c SHA256SUMS` → **5/5 OK**. Not iki dilli ve ölçülen boyutlarla (`47.7/59.8/57.0/55.4/0.6 MiB`, `@ASSET_SIZES@` gerçek dosyalardan doldu), erken erişim işareti iki yarıda. Kapanış bir kusur daha buldu: GitHub sürümü `prerelease=false` ile Latest etiketledi, not ise pre-release diyordu → `release.yml` `gh release create` artık `--prerelease` taşıyor (`ReleaseNotesTest` 9/9, mutasyonla doğrulandı); mevcut `v1.0.0` de elle pre-release işaretlendi → `GET /releases` `prerelease=true`, yani yayındaki sürüm ile notu aynı şeyi söylüyor |
| W1-04 | Taslak tarayıcı sertleştirme | D4, D5, D14 | M | — | ☐ | |
| W2-01 | Artımlı istatistik | X5, X3 | M | W1-03 | ☐ | |
| W2-02 | G/Ç executor | X1, X2 | M | W2-01 | ☐ | |
| W2-03 | Bellek/boyut/hata yolu | X4, M10, D7 | M | W2-02 | ☐ | |
| W2-04 | Dış değişiklik | D1, M9 | M | W2-02 | ☐ | |
| W2-05 | Symlink korunumu | D2 | S | W2-04 | ☐ | |
| W2-06 | Taslak yolu + belge kilidi | D3, D8 | M | W2-04 | ☐ | |
| W3-01 | `EditorAction` katmanı | M1, U13 | M | W1-03 | ☐ | |
| W3-02 | Menü çubuğu | E2 | M | W3-01 | ☐ | |
| W3-03 | CLI argümanı | E3 | S | W3-01 | ☐ | |
| W3-04 | Sürükle-bırak | E4 | S | W3-01 | ☐ | |
| W3-05 | `FindModel` | E1 | M | W2-01, W3-01 | ☐ | |
| W3-06 | Arama paneli | E1 | M | W3-05 | ☐ | |
| W4-01 | Ayar şema sürümü | D13, D12 | S | — | ☐ | |
| W4-02 | Son dosyalar + opt-out | E5, U7 | M | W4-01, W3-02 | ☐ | |
| W4-03 | Minimum genişlik | U8 | S | — | ☐ | |
| W4-04 | Ses politikası | U9 | S | W3-02, W4-01 | ☐ | |
| W4-05 | `v1.1.0` | B2 | S | hepsi | ☐ | |

---

## 7. Çıkış Kapıları — Çalıştırılabilir Doğrulama

Her kapı, **tek komut kümesi** ve **kaydedilmiş çıktı** ile geçilir. Kapı geçilmeden sonraki
dalga başlamaz; bu kural P1'in kısmi kapanışının tekrarını önler (`analysis.md` §3.2).

### G0′ — P1 borcu kapandı · W0 sonu

```bash
cd /home/hyperpostulate/repositories/JTypeWriter
mvn -B clean test                      # 350+ / 0 / 0
grep -rn "new Alert" src/main/java     # yalnız DialogService
grep -rn "HH:mm\|yyyy" src/main/java   # boş
mvn test -Duser.language=de -Duser.country=DE
```

### G1 — Yayın var ve çalışıyor · W1 sonu

```bash
curl -s https://api.github.com/repos/hyperpostulate/JTypeWriter/releases \
  | python3 -c 'import json,sys; print("\n".join(a["name"] for a in json.load(sys.stdin)[0]["assets"]))'
# 5 varlık + SHA256SUMS
```
+ CI `smoke` job logu: imaj açıldı, `.deb` kuruldu/kaldırıldı.

### G2 — Veri ve akıcılık · W2 sonu

```bash
mvn test -Dtest='SessionStatsTest,StatsPerfHarness,FileServiceTest,AtomicFilesTest,RecoveryStoreTest,AutoSaveServiceTest'
bash scripts/verify-artifacts.sh --skip-build     # 17/17
```
+ `StatsPerfHarness` 10 MB çıktısı: **referansta < 5 ms**, CI'da < 50 ms.

### G3 — Editör · W3 sonu

```bash
mvn test -Dtest='FindModelTest,KeyboardShortcutHandlerTest,ShortcutRowsTest,MainStageTest,DialogSizingTest,MessagesTest'
```
+ elle: `Ctrl+F` → bul; `F3`/`Shift+F3` → gez; `Ctrl+H` → bir/tümünü değiştir; aynı işlevler
menüde; 10 MB belgede arama süresi kaydedildi.

### G4 — Sürüm · W4 sonu

```bash
mvn -B clean test
bash scripts/verify-artifacts.sh --skip-build     # 17/17
python3 scripts/generate-icons.py --check         # 11 dosya
```
+ `v1.1.0` Release'i (6 varlık, iki dilli not).

---

## 8. Kapsam Dışı — Ne Yapılmayacak ve Neden

| Öneri | Karar | Gerekçe |
|---|---|---|
| **Sekmeler / çoklu belge** | P3 | Tek belge romancı için tutarlı kimlik; P1'in çıkış/kirli/taslak matrisini ikiye katlar |
| **Yazım denetimi** | P3 | Sözlük lisansı + büyük UI yüzeyi (XL); farklılaştırıcı değil, kanıtsız talep |
| **Windows Authenticode imzası** | **P2'de yok, bütçe kararı** | SmartScreen **uyarır, engellemez**; ~1–2 gün sertifika + CI adımı. İlk 100 kullanıcı için kabul meşru |
| **Otomatik güncelleme / telemetri** | P3 | Kurulu taban yokken anlamsız; gizlilik duruşuyla çelişir |
| **Satır numaraları (E8), sarma anahtarı (E9)** | P3 | Komut katmanından sonra ucuzlar; bölüm bitirmeyi engellemiyor |
| **Yazdırma / PDF (A12)** | P3 | Ayrı çıktı hattı; hiçbir P2 kapısını kapatmaz |
| **jlink (A13) + JAR temizliği (A10)** | **P3, koşullu** | A13, M6 (yansımalı undo) çözülmeden riskli (K3). Kazanç gerçek (−74 MiB) ama **ambalaj kapısı değil**; indirme boyutu ilk sürümde şikâyet olursa P3'ün ilk işi |
| **OS entegrasyonu / MIME (A14)** | **P3, kanala bağlı** | Yalnız **installer** kanalında mümkün; taşınabilir kanalda imkânsız (`analysis.md` §13.5). Kanal kararı verilmeden kart yazılmaz |
| **`SECURITY.md`, `CHANGELOG`, tanılama (A15)** | P3 | Güven boyutunu besler ama kapı kapatmaz; yayın sonrası ilk turda ucuzlar |
| **Test hijyeni: gerçek `~/.jtypewriter` (A9)** | **P3, tetikli** | `AGENTS.md` ihlali; ama kartlar zaten `-Duser.home=<temp>` ile koşuyor. **Tetik:** süit yerelde bir geliştiricinin taslağını silerse derhal |
| **Kapsam ölçümü (JaCoCo) kapısı** | P3 | Suite kapı; yüzde metrik gürültü (`analysis.md` §11.3) |
| **XDG taşıması (B7)** | P3 | Kurulu taban yokken riskli; macOS dalı düştü, yalnız `XDG_CONFIG_HOME`/`XDG_DATA_HOME` kaldı |
| **ES/IT register, tipografik apostrof** | A5 yanında bedava gelirse alınır | Düşük etki |

### 8.1 Tek açık insan kararı — ve neden hiçbir kartı bloke etmiyor

`analysis.md` §13.5'teki **kanal ekseni** (installer / taşınabilir / ikisi) açıktır. Bu yol
haritası **kanal-nötrdür**: `release-assets.sh` bugün ikisini de üretiyor, iki kanalın da
varlıkları yayında olacak. Karar yalnız iki şeyi etkiler:

1. **Sürüm notunda "ürün varsayılanı"** hangisi denileceği (W1-03 metni);
2. **A14'ün** P3'te bir kart alıp almayacağı.

**Karar verilmezse varsayılan statükodur** (iki kanal, varsayılan ilan edilmez) ve hiçbir kart
gecikmez. Bu, Pareto'nun bu belgedeki **dördüncü** uygulamasıdır: karara bağlı olmayan işi
karardan ayır.

---

## 9. Riskler ve Kesme Sırası

### 9.1 Risk kaydı

| Risk | Olasılık | Etki | Erken sinyal | Karşılık |
|---|---|---|---|---|
| CI smoke testi Xvfb/`xwininfo` kırılganlığı | Orta | W1 gecikir | `smoke` job'u 3 ardışık koşuda kırılır | Zaman tabanlı bekleme yerine **pencere sorgusu** + 60 s üst sınır; olmazsa Windows ayağında "süreç ayakta" ile sınırla |
| Artımlı sayaç, tam tarama ile birebir uyuşmaz | Orta | G2 yanlış geçer | Property testinde tek karakter farkı | Sayaç yalnız `TextCodec`/`SessionStats` içinde; karşılaştırma testi **zorunlu** koşul |
| A3 + A1 çakışması (`FileService` sözleşmesi) | Yüksek (paralel yazılırsa) | İş iki kez yazılır | Aynı PR'da iki kart | K4: **sıralı**, ayrı PR'lar |
| `Alert` sarma, tema/ölçü nedeniyle 768 px'i aşar | Orta | G3 gecikir | DE/FR metin testi kırmızı | Metin kısaltma değil, **kelepçe + sarma**; gerekirse ayrıntı ikinci diyaloğa |
| İlk yayında asset adı/assert uyuşmazlığı | Orta | W1 gecikir | `dry_run` kırmızı | W1-02 tam bunun için var |
| Tek geliştirici + 6 dil çevirisi darboğazı | Yüksek | W3/W4 uzar | Kart başına >2 gün çeviri | Metinler **kart içinde** bitirilir, sona bırakılmaz; çeviri kalitesi P3'e devredilebilir (parite testi yine yeşil) |

### 9.2 Kesme sırası (kapasite düşerse **bu sırayla** kesilir)

1. **W4-04 (ses politikası, S)** — kapı yok; ürünün adındaki özellik ama eksikliği kimseyi
   engellemez.
2. **W4-03 (minimum genişlik, S)** — kapı yok; kozmetik kırpılma.
3. **W4-02 (son dosyalar + opt-out, M)** — kapı yok; sürtünme azaltıcı.
4. **W1-04 (taslak sertleştirme, M)** — kapı yok, **ama gizlilik gerekçesi var**: kesilirse
   `README` ve yayın notuna "bilinen sınır: taslak taraması symlink takip edebilir" yazılır.

**Kesilmeyenler:** W0–W3'ün tamamı. Bu dört dalga beş kapının beşini kapatır; buradan tek kart
kesmek, "kapı geçildi" iddiasını yalancı yapar.

---

## 10. Efor, Takvim ve Kapasite

| Dalga | Kart | İş günü (tahmin) | Kapı |
|---|---:|---:|---|
| W0 — P1 borcu | 4 | 4 | G0′ |
| W1 — Yayın | 4 | 6 | G1 |
| W2 — Veri + akıcılık | 6 | 9 | G2 |
| W3 — Editör | 6 | 9 | G3 |
| W4 — Cila + sürüm | 5 | 6 | G4 |
| **Toplam** | **25** | **~34** | **5/5** |

Kart sayısı ile puan farklı birimlerdir: §3.4'teki **21 puan**, `analysis.md` §13.1'in göreli
efor birimidir (S=1/M=2/L=3); buradaki **25 kart** iş kalemidir. Sayıların ikisi de tutarlıdır,
birbirine çevrilmez.

> **Tahmin uyarısı:** İş günü sayıları **"çıkarıldı"** seviyesindedir (`analysis.md` §2.1) —
> bu repoda tek bir kartın gerçek süresi ölçülmemiştir. Kartlar göreli büyüklüktür; takvim
> iddiası değildir. Tek geliştirici varsayımıyla ~7 hafta. İki kişide kazanç sınırlıdır:
> W2 içinde A1 ⟂ A3 ve W3 içinde A4 ⟶ A2 sıralıdır (K4, K1); paralellik yalnız
> **W2'nin A3 kolu ↔ W3'ün A4 kolu** arasında gerçektir ve A2, A3 bitene kadar beklemede
> kalır (K2).

**Görünmeyen maliyetler** (`analysis.md` §13.4) bu tahmine **dahildir**: ~15–20 anahtar × 6 dil
çeviri, README/README-TR çifti, her kart için test, bağımlılık değişiminde
`THIRD-PARTY-NOTICES` + `verify-artifacts`, Windows'u yalnız CI'da görmenin gecikmesi.

---

## 11. Sonraki Faz İçin Devredilenler (P3'e giriş listesi)

Aşağıdakiler **bilinçli** olarak P2 dışında bırakıldı ve P3 planlanırken ilk okunacak listedir:
A13+A10 (jlink + JAR), A14 (OS entegrasyonu, kanala bağlı), A12 (yazdırma), A15 (destek yüzeyi),
A9 (test hijyeni), B7 (XDG), M6 (yansımalı undo), M7 (sınırsız undo), B5/B6/B12 (yeniden
üretilebilirlik), E6 (tercihler penceresi), E8/E9 (satır numarası, sarma), B10 (SBOM).

**P3'ün ilk sorusu şu olmalı:** *P2'nin yayınladığı sürümü kaç kişi indirdi ve hangi kanaldan?*
Cevap yoksa P3 de tahminle planlanır — P2'nin tüm varlık sebebi bu soruya cevap üretmektir.

---

## 12. Tek Paragraf

JTypeWriter'ın P2'si, ürünün en pahalı kısmını (veri bütünlüğü) zaten çözmüş bir kod tabanına,
**görünür ve kanıtlanabilir bir ürün** katmanı eklemektir: önce yayınlanmamış olmanın bedelini
ödeyip `v1.0.0`'ı CI'da açılan bir artefaktla yayınlamak, sonra ölçülen tek darboğazı (10 MB'da
saniyede 163,7 ms istatistik taraması) artımlı sayaçla silmek, sonra dosyaya kimlik verip sessiz
üzerine yazmayı ve symlink kaybını bitirmek, sonra komut katmanı + menü + ara/değiştir ile
"metin alanı"nı editöre çevirmek, ve P1'den devreden üç açığı (diyalog taşması, sabit tarih
biçimi, paket smoke testi) ayrı bir kuyruk yapmadan bu işlerin içine gömmek. Altı iş, havuzun
%45'i, **beş kapının beşi**; kalan 11 puan — jlink, MIME, yazdırma, imza, destek yüzeyi —
gerekçesiyle kesilir, çünkü hiçbiri bir kapıyı kapatmaz ve kritik yolu uzatır. P2'nin başarı
ölçütü olgunluk endeksi değil, §7'deki beş komut kümesinin kaydedilmiş çıktısıdır.
