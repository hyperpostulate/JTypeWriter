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

### 1.4 Durum — 7 Kasım 2026 (`main` @ `2df2324`)

Bu bölüm, kart tablosunun üstünde duran **tek gerçek** kaynaktır: bir sayı burada yoksa
`analysis.md`'den geliyordur ve `analysis.md` 2 Kasım'dan beri güncellenmediği için bayattır.

| Ölçüt | 2 Kasım (`f32d3f8`) | 7 Kasım (`2df2324`) |
|---|---:|---:|
| Commit | 137 | **184** (+47) |
| Üretim sınıfı / satırı | 51 / 7.611 | **68 / 12.514** |
| Test dosyası / `@Test` | 42 / 350 | **58 / 528** |
| Bundle anahtarı | 6 × 90 | **6 × 146** |
| `mvn -B clean test` | 350/0/0 | **528/0/0** |
| Tag / Release | 0 / 0 | **4 / 4** (`v1.0.0`, `v1.0.1`, `v1.0.2`, `v1.0.3`) |
| Release varlığı | — | **6** (5 ikili/kaynak + `SHA256SUMS`) |
| Release işareti | — | **dördü de `prerelease=true`** |
| `verify-artifacts` karşılaştırması | 17 | **20** (gömülü font lisansı +2, font kaynağı +1) |
| Yıldız / çatal / issue | 0 / 0 / 0 | **0 / 0 / 0** |

**Dalgalar:** W0 ✅ · W1 ✅ · W2 ✅ · W3 ✅ (W3-01…W3-06 ✅) · W4 ◐ (W4-01 ✅, W4-02 ✅; W4-03…W4-05 ☐).
Kapılar: **G0′ ✅ · G1 ✅ · G2 ✅ · G3 ✅ · G4 ☐**.

**`v1.0.3` notu (7 Kasım 2026):** dördüncü yayın, W3-01…W3-06 ile kart takiplerini
(`ded67d9`, `0634a49`, `0738f18`) taşıyan bir **patch** sürümüdür; `v1.0.2..v1.0.3` aralığı
arama panelini de (`12945b2`) içerir, yani `Ctrl+F`/`F3`/`Ctrl+H` ilk kez bu sürümle kullanıcıya
ulaştı. Bu, W4-05'in sürüm numarası gerekçesini değiştirir (kartın içindeki düzeltmeye bakınız).
Tablodaki sayılar `2df2324` anındadır; **bu bölümü senkronlayan commit tabloya dahil
edilmemiştir** (sayıyı her belge düzeltmesinde bir artırmak tabloyu ölçüm olmaktan çıkarırdı).

İki sayı bu tablodan **türetilmemelidir**:

- `analysis.md` §12 skorları ve §13 aday havuzu 2 Kasım ölçümüdür. Beş boyutun beşi (motor,
  deneyim, ambalaj, güven, pazara erişim) o günden beri W0–W3 kartlarıyla hareket etti;
  güncel skor ancak G4'ten sonra yeniden ölçülür. §2.2'deki "beklenen sonuç" sütunu bu yüzden
  bir **hedef değil**, bir yön göstergesidir.
- `analysis.md` §8'in performans tabloları X5 için artık yanlış: 10 MB'da 163,7 ms/s olan
  istatistik yenilemesi W2-01'den sonra **0,13–0,21 ms** ölçülüyor (`StatsPerfHarnessTest`).
  Kalan tek sayısal iddia test sayılarıdır ve onlar da bu tablodadır.

---

## 2. Taban, Hedef ve Hedefin Niteliği

### 2.1 Bugünkü ölçüm

> **Bu tablo 2 Kasım 2026'dır, "bugün" değildir.** Skorlar `analysis.md` §12'nin ölçüm anına
> bağlıdır ve o an `f32d3f8`'dir; tablo bu yüzden yerinde bırakıldı. 7 Kasım itibarıyla gerçek
> durum §1.4'tedir: motor, deneyim, ambalaj ve güven boyutlarının hepsi W0–W3 ile hareket etti,
> güncel skor ancak G4'ten sonra ölçülür.

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

> **Kanıt satırlarındaki sayılar, o koşunun sayılarıdır.** B14 satırındaki `17/17` 3 Kasım'daki
> `v1.0.0` build'ine aittir; 7 Kasım'da gömülü font eklenince karşılaştırma kümesi **20/20**
> oldu (`legal/Bitstream-Vera.txt` imajda + jar'da + `.deb` yükünde). Bu yüzden G2/G4
> kapılarındaki beklenen değer 20'dir; geçmiş bir koşuyu 20'ye "düzeltmek" kanıtı tahrif
> etmek olurdu, ayrı bir koşu daha iyisini üretmediği sürece satır 17 kalır.

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
- **Uygulama (4 Kas 2026):** İki yeni saf sınıf: `RecoveryRetention` (yaş 30 gün, 10 dosya,
  64 MiB; yalnız park edilmiş dosyalar aday) ve `StorageFootprint` (`1 + BackupKeeper.generations()`
  kalıcı, `+2` geçici → 4×/6×). `RecoveryStore` taraması `NOFOLLOW_LINKS` ile symlink'i hiç
  okumuyor (WARNING), `readAllBytes` yerine `readBounded` (önce boyut, sonra `readNBytes`);
  `MAX_DRAFT_BYTES = 64 MiB` üstü **kesilmiyor, atlanıyor** — baştan eksik bir "kurtarma"nın
  sonra aslın üzerine kaydedilmesi, kurtarma görünen bir veri kaybıdır. Temizlik taramadan
  **önce** koşuyor, yoksa süresi dolmuş dosya yine okunurdu. Kartın `parked-*` dediği dosyalar
  kodda `pending-*` (park edilen slot adı); `draft-*` başka bir çalışan örneğe ait olabildiği
  için yaş kuralı ona hiç dokunmuyor. Disk çarpanı ilk **başarılı kayıtta** bir kez gösteriliyor
  (`notice.storage.shown`, `DialogService.showStorageFootprintNotice`); sayılar dialogda yazılı
  değil `BackupKeeper.generations()`'tan türetiliyor, çünkü bir kez gösterilen ve yanlış olan
  bir sayıyı kullanıcı düzeltemez. Testler: `RecoveryStoreTest` 19→25, `BackupKeeperTest` 7→9,
  `AppSettingsTest` 17→18. Üç güvenlik kuralı da mutasyonla doğrulandı: `NOFOLLOW_LINKS`
  kaldırılınca symlink testi, `MAX_DRAFT_BYTES` 200 MiB yapılınca büyük-taslak testi, temizlik
  kapatılınca yaş testi kırmızı. `mvn -B clean test` → **384/384**.

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
- **Uygulama (5 Kas 2026):** `SessionStats` artık sayaç tutuyor: `getWordCount`,
  `getCharCount`, `getCharCountNoSpaces`, `getLineCount` O(1) okuma; imzalar değişmedi, bu yüzden
  `StatsBarModel` hiç dokunulmadı. İki giriş noktası var: `updateText(String)` belge yüklenince
  tek tam tarama yapar (eski davranış, artık yalnız yüklemede), `applyChange(TextChange, String)`
  ise yalnız değişen pencereyi tarar — `TextChange` (`offset`, `removedLength`, `insertedText`)
  `stats` paketinde saf bir record, JavaFX tipi taşımıyor. Tanımlar eski tam taramayla birebir:
  kelime `trim().split("\\s+")` (ASCII `\s` sınıfı; `\u00A0` kelimeyi bölmez), satır yeni satır + 1
  (boş belge 0), boşluksuz yalnız `' '`, `\n`, `\r` atıldıktan sonraki uzunluk. `isBlank()` daha
  geniş bir küme olduğu için ayrı bir `Character.isWhitespace` sayacı tutuluyor: yalnız em-space
  içeren belge 0 kelime ama regex açısından "kelime başlangıcı" sayılırdı.
  Deltayı `EditorArea.replaceText` üretiyor — YOLO'nun tek uygulama noktası zaten orası — ve
  **uygulanan** değişikliği kaydediyor (`BLOCK` hiçbir şey yazmıyor, `INSERT_WITHOUT_DELETION`
  `(start, 0, text)` olarak). `EditorKeyHandler`'daki metin dinleyicisi ipucunu bir kez tüketiyor;
  ipucu yoksa (undo/redo, `setText`, belge yükleme) `updateText` ile tam tarama yapılıyor, yani
  sayaç asla ipucunun doğruluğuna bağlı değil — yalnız hız ona bağlı. Sahte ipucu (yeni metnin
  uzunluğu tutmuyorsa) da tam taramaya düşüyor.
  Testler: `SessionStatsTest` 12 → 21 (ekleme, silme, değiştirme, kelime sınırı, Unicode boşluk,
  tutarsız ipucu, tüm belge değişimi, hepsini silme ve 500 adımlık rastgele dizi — her adımda eski
  tam tarama implementasyonuyla birebir karşılaştırma). Yeni `StatsPerfHarnessTest` gerçek
  `StatsBarModel.refresh` yolunu 10 MiB'da ölçüyor: **0,116–0,212 ms** (CI eşiği 50 ms; eski
  ölçüm 163 ms). İki mutasyonla doğrulandı: `getWordCount` eski `split` gövdesine döndürülünce
  harness **123,2 ms** ile kırmızı; `applyChange` penceresinden `+1` atılınca property testleri
  (3 tanesi) kırmızı. `mvn -B clean test` → **396/396**.

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

**Uygulama (5 Kas 2026):** Yeni `IoExecutor` (kök paket) tek bir daemon işçi
thread'inde (`jtw-io-*`) çalışıyor: belge kaydetme, yedek rotasyonu, otomatik kayıt ve taslak
temizliği aynı kuyruğa girdiği için bir anlık görüntü, henüz rename etmekte olan bir kaydetmenin
yedek kuşağını döndüremiyor. Sayaç `IoExecutor.fxThreadTasks()`; üretimde sonda
`Platform.isFxApplicationThread()`, gönderim `Platform.runLater` — testler toolkit açmamak için
aynı arayüze saf bir thread probe'u enjekte ediyor. Servislerin varsayılan kurucusu
`IoExecutor.synchronous()` (başsız testlerin determinizmi için), `AppGraph` ise paylaşılan
`IoExecutor.background()` örneğini enjekte ediyor ve `FxApplication.stop()` onu kapatıyor.
`FileService` artık `openFileAsync`, `loadFileAsync`, `saveFileAsync`, `saveFileAsAsync`,
`newDocumentAsync` ve `requestExitAsync` sunuyor; araç çubuğu, kısayollar ve pencere kapatma
yolu bunları kullanıyor. Senkron imzalar (açılış geri yüklemesi ve testler için) korundu ama
artık G/Ç'yi yine işçiye gönderip yalnız çağıranı bekletiyor — disk erişimi hiçbir yolda FX
thread'ine dönmüyor. Kaydetme başlarken `Listener.onSavingChanged(true)` yayınlanıyor; durum
çubuğu altı dilde yeni `savingStatus` anahtarıyla "Kaydediliyor..." gösteriyor, commit gelince
"Kaydedildi {0}" geri geliyor. Eşzamanlılığın getirdiği yeni risk kapatıldı: kaydetme sürerken
yazılan metin commit'te ezilmiyor (`savedContent` diske giden anlık görüntü, `currentContent`
editörün yeni hâli; kirli işareti yeniden hesaplanıyor) ve belge jetonu eski bir tamamlanmanın
yeni belgenin durumunu ezmesini engelliyor. `AutoSaveService.snapshot` artık `Supplier<String>`
alıyor: önce `hasUnsavedChanges()`, sonra `getText()`; temiz tikte metin kopyası hiç alınmıyor.
Snapshot/clear/park işçide; `shutdown` yalnız son karar için kısa süre bekliyor (G/Ç yine
işçide). Testler: yeni `FxThreadIoTest` 5/5 (işçi FX thread'i değil, kaydetme/açma sırasında
`fxThreadTasks()==0`, temiz tikte kopya yok, kaydetme sürerken yazılan metin kirli kalıyor),
`FileServiceTest` 36/36, `AutoSaveServiceTest` 15/15. `mvn -B clean test` → **401/401**.

**W2-03 · A3c — Bellek, boyut ve hata yolu** · M · X4, M10, D7, D14
- **Kapsam:** `FileService.loadFile`, `AtomicFiles`, `AutoSaveService.java:317`
- **Yapılacak:** `readAllBytes` yerine boyut denetimli akış; üst sınır aşılırsa **açık uyarı**
  (sessiz kısaltma yok); `Error` da kaydetme hata yolundan geçer; disk alanı ön kontrolü.
- **Bitti tanımı:** (1) Sınır üstü dosya açılmıyor ve kullanıcıya **ne olduğu** söyleniyor
  (6 dilde metin). (2) `OutOfMemoryError` benzeri `Error` senaryosunda taslak korunuyor (test).
  (3) Disk dolu senaryosu (temp `user.home` + kotasız FS simülasyonu) kaydetmeyi "başarılı"
  göstermiyor.
- **Doğrulama:** `mvn test -Dtest=FileServiceTest,AtomicFilesTest` + yeni testler.

**Uygulama (5 Kas 2026):** `FileService.MAX_DOCUMENT_BYTES` = 64 MiB (kurtarma taslağı sınırıyla
aynı değer). Açma artık `Files.readAllBytes` değil: önce `Files.size`, sonra
`readNBytes(MAX+1)` ile sınırlı okuma; arada büyüyen/yalan söyleyen dosya da yakalanıyor. Sınır
aşılırsa dosya **açılmıyor**, kesilmiş bir önek editöre verilmiyor ve kullanıcıya dosya adı,
gerçek boyut ve 64 MB'lık sınırı söyleyen `errorOpenTooLargeHeader/Content` diyaloğu gösteriliyor
(altı bundle + `Messages` erişimcisi; `MessagesTest` 13/13). Kaydetme yolu artık
`IOException | RuntimeException | Error` yakalıyor: `OutOfMemoryError` gibi bir `Error` de
`WriteFailed` üretip kaydetme hata yolundan geçiyor, belge kirli kalıyor ve `onSaved`
yayınlanmadığı için otomatik kurtarma taslağı silinmiyor. Mesajsız `Error`'da diyalog içeriği
tür adıyla (`OutOfMemoryError`) dolduruluyor. Yedek hazırlama/yerleştirme hataları eskisi gibi
yalnız `IOException | RuntimeException` ile yutuluyor (yedek ikincil), böylece oradaki bir `Error`
asıl kaydetme hatasına dönüşüp görünür oluyor. Disk alanı ön kontrolü `AtomicFiles.writeMoving`'e
girdi: temp dosya oluşturulmadan önce `FileStore.getUsableSpace()` yeni içerik boyutuyla
karşılaştırılıyor; alanı bildirmeyen dosya sistemleri (ağ paylaşımları) engellenmiyor, gerçekten
dolu disk ise `IOException` ile kaydetme hatasına düşüyor. Testler: yeni
`anOversizedDocumentIsRefused...`, `aWriteRejectedByAFullDisk...`, `aFullDiskFailsBeforeAnyTempFileIsCreated`,
`aSnapshotThatFailsWithAnError...`, `aSaveThatFailsWithAnErrorKeepsTheRecoveryDraft`.
`mvn test -Dtest=FileServiceTest,AtomicFilesTest,AutoSaveServiceTest,MessagesTest` → 38/38 + 7/7 +
17/17 + 13/13; `mvn -B clean test` → **406/406**; `mvn -B test -Duser.language=de -Duser.country=DE`
→ **406/406**.

**W2-04 · A1a — Dış değişikliği yakala** · M · D1, M9
- **Kapsam:** `service/FileService.java:69-74,296,310`, `AtomicFiles`
- **Yapılacak:** Yüklemede `(mtime, boyut, içerik SHA-256)` saklanır; kaydetmeden hemen önce
  tekrar okunur; çelişkide **üç seçenekli** diyalog: üzerine yaz / yeni ada kaydet / vazgeç.
- **Bitti tanımı:** (1) Diskteki dosya yüklemeden sonra değişirse kaydetme **ya sorar ya
  reddeder** — sessiz üzerine yazma yok (test). (2) "Yeni ada kaydet" `ExitDisposition`'ı
  atlamaz. (3) Aynı saniye içinde yapılan değişiklik de yakalanıyor (hash, mtime'a bağımlı
  değil).
- **Doğrulama:** `mvn test -Dtest=FileServiceTest` (yeni `externalChange*` testleri).

**Uygulama (5 Kas 2026):** `FileService` artık yüklemede ve her başarılı kaydetmeden sonra hedefin
`DiskFingerprint`'ini (`boyut`, `mtime`, içerik SHA-256) saklıyor. Kaydetme, yedek hazırlanmadan
**önce** dosyayı akış üzerinden yeniden özetliyor; özet tutmuyorsa hiçbir şey yazmadan `Conflict`
dönüyor ve üç seçenekli `showExternalChangeDialog` açılıyor: **Üzerine Yaz** (dış sürüm `.bak`
olarak korunur, çünkü `unchanged` zorla false yapılır), **Farklı Kaydet** (normal `saveFileAs`
akışı; dosya seçici iptal edilirse çıkış `STAY` kalır, yani `ExitDisposition` atlanmaz) ve
**İptal** (hiçbir şey yazılmaz, belge kirli kalır). Karşılaştırma yalnız SHA-256 üzerinden: aynı
boyutlu ve aynı saniyedeki değişiklik ne boyutla ne mtime'la görülür (test mtime'ı geri alarak
bunu sabitliyor). Yüklemeden sonra silinen dosya da çelişki sayılıyor (yeniden oluşturmak sessiz
üzerine yazma olurdu) ve çelişki `WARNING` seviyesinde iki tarafın boyut/mtime'ıyla loglanıyor.
Kurtarma taslağında özgün dosyanın özeti `restoreRecovered`'da yakalanıyor; özet akış üzerinden
hesaplandığı için dış dosyanın boyutu belleğe alınmıyor (W2-03 sınırı korunuyor). Altı bundle'a
`externalChange*` anahtarları ve `Messages` erişimcileri eklendi. Testler: yeni dokuz test —
`anUntouchedFileOnDiskIsSavedWithoutAnExternalChangePrompt`,
`anExternalChangeIsCaughtAndCancellingTouchesNeitherVersion`,
`anExternalChangeWithinTheSameSecondIsCaughtByTheContentHash`,
`overwritingAnExternalChangeKeepsThatVersionAsABackup`,
`savingAnExternalChangeUnderANewNameKeepsBothFiles`,
`choosingSaveAsAfterAnExternalChangeDoesNotBypassTheExitCheck`,
`aFileDeletedSinceItWasOpenedCountsAsAnExternalChange`,
`anAsyncSaveAsksAboutAnExternalChangeAndHonoursCancel`,
`anAsyncSaveCanOverwriteAnExternalChange`.
`mvn test -Dtest=FileServiceTest,DialogServiceTest,MessagesTest` → 47/47 + 9/9 + 13/13;
`mvn -B clean test` → **415/415**.

**W2-05 · A1b — Symlink'i koru** · S · D2
- **Kapsam:** `AtomicFiles.java:54-74`
- **Yapılacak:** Hedef `toRealPath()` ile çözülür; yazma gerçek hedefe, atomik taşıma hedefin
  dizininde; `.bak` gerçek hedefin yanına.
- **Bitti tanımı:** Symlink'li belge kaydedildikten sonra **hâlâ symlink** ve hedefi
  güncellenmiş (bugün kusur: link yok oluyor).
- **Doğrulama:** `mvn test -Dtest=AtomicFilesTest` (yeni `symlink*` testi) + `analysis.md`
  §15.4'teki manuel deney.

**Uygulama (6 Kas 2026):** `AtomicFiles.writeMoving` artık hedefi temp dosyadan önce
`toRealPath()` ile çözüyor; temp dosya, fsync, atomik taşıma, dizin fsync'i ve izin kopyası
gerçek belgenin üzerinde çalışıyor, böylece symlink kaydetmeden sonra da symlink kalıyor (eski
kusur: link normal dosyayla değişiyordu). Yolu henüz olmayan belge (yeni dosya) veya kırık link
çözülemiyor; bu durumda adlandırıldığı yol kullanılıyor, yani yeni belge yine oluşturuluyor.
`BackupKeeper.prepare` da hedefi çözüyor: hazırlık dosyası ve dönen `.bak`/`.bak.N` zinciri
gerçek hedefin yanına yazılıyor, `PreparedBackup` gerçek hedefi taşıdığı için `install` de
oraya yerleştiriyor; link'in yanında yedek birikmiyor. Testler: yeni
`savingThroughASymbolicLinkKeepsTheLinkAndUpdatesItsTarget`,
`aBackupOfASymbolicLinkLandsNextToItsRealTarget`.
`mvn test -Dtest=AtomicFilesTest,BackupKeeperTest` → 8/8 + 10/10; `mvn -B clean test` →
**417/417**; `analysis.md` §15.4 manuel deneyi → `isSymbolicLink(link)=true`, gerçek hedef
`yeni`, link dizininde yalnız link.



**W2-06 · A1c — Taslak yolu ve belge kilidi** · M · D3, D8
- **Kapsam:** `RecoveryStore.java:333-344`, `InstanceLock`
- **Yapılacak:** Taslak başlığındaki yol mutlaklık + kanoniklik + izinli kök açısından
  doğrulanır; geçersizse kurtarma **hedefsiz** açılır (Farklı Kaydet'e zorlar). Belge düzeyinde
  `FileLock`; `UNAVAILABLE` ise uyarı, sessiz devam yok.
- **Bitti tanımı:** Geçersiz yol hedef olarak kabul edilmiyor (test); iki süreç aynı dosyayı
  açtığında ikincisi uyarıyor (test, `ProcessCrashRecoveryTest` deseniyle fork).
- **Doğrulama:** `mvn test -Dtest=RecoveryStoreTest,InstanceLockTest`.

**Uygulama (6 Kas 2026):** Taslak başlığındaki `original.path.b64` artık doğrudan `Path.of`
edilmiyor: yeni `service/RecoveryTargetPath` (saf, JavaFX'siz) dört kural uyguluyor — yol
mutlak olmalı; kanonikleşmeli (`toRealPath()`, yani symlink hedefiyle yargılanıyor); izinli
köklerden birinin altında olmalı; uygulamanın kendi veri dizininin (`AppDirs.dataDir()`)
altında olmamalı. İzinli kök kümesi boşsa **her şey reddediliyor** (fail closed). Kural
tutmayan taslak yine sunuluyor, yalnız **hedefsiz**: metin kurtarılıyor, `restoreRecovered`
yolu `null` alıyor, ilk kaydetme Farklı Kaydet'e düşüyor — yani çalıntı/başka makineden gelen
bir başlık kaydetmeyi keyfi bir dosyaya yönlendiremiyor. Henüz var olmayan belge (silinmiş
dosyanın taslağı) ebeveyni üzerinden çözülüp kabul ediliyor; kırık symlink reddediliyor
(W2-05'in yazma yolundaki kararıyla aynı). Üretimde izinli kök `user.home`; başka bir kökte
(ör. Windows'ta `D:\`) duran belgenin taslağı kurtarılırken hedefsiz açılır.

**Düzeltme (6 Kas 2026, CI):** İlk sürüm `accept()` doğrulamadan geçen **kanonik** yolu
döndürüyordu; Windows ayağı bunu kırmızıya çevirdi:
`AutoSaveServiceTest.anOrdinaryDraftStillRestoresOntoItsPath` `RUNNER~1` (8.3 kısa ad) beklerken
`runneradmin` (uzun ad) aldı, çünkü `toRealPath()` kısa adı genişletiyor. Kanonik biçim artık
yalnızca **karar** için kullanılıyor; dönen değer taslağın yazdığı yolun mutlak/normalize hâli.
Bu yalnız Windows kısa adı için değil, symlink'li belge için de doğru: aksi hâlde kurtarılan
belge hiç açılmadığı bir gerçek yolla anılırdı (yazma zaten W2-05 sayesinde linki takip ediyor).
Regresyon testi `anAcceptedSymbolicLinkKeepsItsOwnName`; `return canonical`'a geri döndürülünce
kırmızı. Testler: yeni
`RecoveryTargetPathTest` (10) ve `RecoveryStoreTest`'e iki vaka — dış kökteki yol düşüyor ama
metin korunuyor, izinli kökteki yol korunuyor. Mutasyon kanıtı: `accept` yerine `Path.of`
konunca dış-kök testi kırmızı; kanonikleştirme `toAbsolutePath`'e indirilince symlink ve
kırık-link testleri kırmızı. `mvn test -Dtest=RecoveryTargetPathTest,RecoveryStoreTest` →
10/10 + 27/27; `mvn -B clean test` → **429/429**.

**Belge kilidi (D8) kapsam dışı bırakıldı — ürün kararı:** `InstanceLock` aynı `user.home`
ile ikinci süreci zaten başlatmıyor (`JTypeWriterApp:26-29`); kalan boşluk yalnız farklı
`user.home` (taşınabilir çalıştırma, ikinci profil, sandbox) veya `UNAVAILABLE` durumundaki
kilit. O boşlukta W2-04 zaten sessiz ezmeyi bitirdi: ikinci süreç kaydedince birincinin
SHA-256 parmak izi kontrolü çakışmayı yakalayıp **soruyor**. Yani dosya başına `FileLock`'un
ekleyeceği şey koruma değil, açılışta erken uyarı olurdu; bedeli ise her açılan belgenin
yanında görünen bir `.jtwlock` yan dosyası (senkron istemcileri ve git onu da taşır, çökmede
kalıntı kalır) ve altı dilde iki yeni metin. Kilit veri dizinine (yol hash'i ile) konsaydı
hiçbir şey kazandırmazdı: aynı `user.home`'da `InstanceLock` zaten ikinciyi durduruyor, farklı
`user.home`'da o kayıt defteri de ayrı olurdu. Bu yüzden W2-06 yalnız D3'ü kapatıyor; D8
kabul edilen bir risk olarak `analysis.md` §6.2'de kalıyor.

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

**Uygulama (6 Kas 2026):** Tek kaynak artık yeni `ui/EditorAction`: 15 komut, 16 akor. Her
sabit (a) etiketini `Messages` üzerinden okuyor (yeni `shortcut*` anahtarları, altı bundle),
(b) akorlarını bildiriyor (kart metni + kabul edilen `KeyCode`'lar + Ctrl/Shift semantiği),
(c) etkisini `EditorAction.Context` üzerinden çalıştırıyor. `Ctrl+Shift+Z` ve `Ctrl+Y` aynı
`REDO` sabitinin iki akoru; `EXIT_FULLSCREEN` listede ama `handledByPlatform()` olduğu için
`resolve()` onu döndürmüyor (JavaFX'in `fullScreenExitKeyCombination`'ı Escape'i zaten
tüketiyor). `resolve()` bir `switch` değil, bu tabloda arayan saf bir fonksiyon
(`Optional<EditorAction>`); `KeyboardShortcutHandler` `Context`'i uyguluyor ve tuş filtresi de
araç çubuğu da aynı `run(action)` girişini kullanıyor. `ToolbarView` kendi `SHORTCUT_*`
sabitlerini, `fileService` alanını ve `requestUndo/requestRedo` çağrılarını kaybetti: düğme
etiketleri `action.label(messages)`, tooltip akorları `action.primaryChord()`, yürütme
`actionRunner.accept(action)`. Kart artık üretiliyor: `ShortcutRows.fromActions(messages)`
`EditorAction` sırasını geziyor; `welcomeShortcuts` blob'u, `ShortcutRows.parse` ve onun
boşluk-hizalama varsayımı silindi. Escape akoru `MainStage`'de de yalnız
`EditorAction.EXIT_FULLSCREEN.primaryChord()` üzerinden yazılıyor. Davranış korundu:
`Ctrl+N`/`Ctrl+O` Shift'le de çalışır, `F11` Shift'le de tam ekranı çevirir, sade `Ctrl+C`
hâlâ tüketilmez (`shift == null` "iki durum da" demek). Testler: yeni `EditorActionTest` (6 —
kayıt tutan `Context` ile her sabitin tek ve kendi etkisini çalıştırdığı, etki eşlemesinin tam
olduğu, `EditorAction` dışındaki kaynak dosyalarda akor literali kalmadığı — yorumlar
soyulmuş kod taranıyor — ve araç çubuğunun `actionRunner.accept(...)` üzerinden gittiği),
`KeyboardShortcutHandlerTest` 15 (iki yönlü akor sahipliği + kartın envanterden üretildiği),
`ShortcutRowsTest` 7 (altı dilde iki yönlü kapsam, sıra, tema döngüsü, dil değişimi),
`MessagesTest` 12, `ThemeCycleTest` 4. Mutasyon kanıtı: `CYCLE_THEME`'e `Ctrl+T` verilince
"aynı akoru iki komut" testi kırmızı; `CYCLE_THEME`'in etkisi `toggleToolbar`'a çevrilince
etki testi kırmızı; `ToolbarView`'a `"Ctrl+S"` literali geri konunca kaynak taraması kırmızı
(üçü de geri alındı). `mvn test -Dtest=KeyboardShortcutHandlerTest,EditorActionTest,ShortcutRowsTest`
→ 15/15 + 6/6 + 7/7; `mvn -B clean test` → **439/439**.

**W3-02 · A4b — Komut menüsü** · M · E2
- **Kapsam:** yeni `ui/MainMenu`, `ToolbarView` (düğmeyi satıra alır), `ToolbarFit` (ölçüm),
  `MainController`, `AppGraph`, `Messages`, 6 bundle, `base.css`
- **Yapılacak:** Dosya / Düzen / Görünüm / Yardım. **`Yeni` ve `Farklı Kaydet` görünür olur**
  (bugün 14 eylemin 2'si arayüzde yok). Etiketler bundle'dan, hızlandırıcılar komut katmanından.
- **Bitti tanımı:** (1) Menüdeki her yaprak öğe bir `EditorAction`'a bağlı ve her platform-dışı
  komut ağaçta tam bir kez görünür; ayar komutları değer alt menüsüyle temsil edilir (test: menü
  ağacı ↔ komut kümesi birebir). (2) Altı dilde menü etiketleri var (`MessagesTest` yeşil).
- **Doğrulama:** `mvn test -Dtest=MainStageTest,MainControllerTest,ToolbarFitTest,MessagesTest`.

> **Tasarım kararı (6 Kas 2026): menü şeridi değil, araç çubuğunda menü düğmesi.** Ürün
> kimliği "distraction-free": kalıcı bir `MenuBar` satırı metinden geri alınamayan bir krom
> satırı ekler, tam ekranda ve `Ctrl+T` sırasında da kalır ve uygulamanın "ekranda yalnızca
> metin" sözünü bozar. Bu yüzden menü, araç çubuğunun en solundaki tek `☰` `MenuButton`'ıdır:
> `Ctrl+T` onu da gizler, ayrı satır yoktur, 14 komutun tamamı yine tek yerden keşfedilir.
> Keşfedilebilirlik kapanmaz; aynı ağaç `layout()` saf fonksiyonundan üretilir, dolayısıyla
> W4-02 iki görünüm komutu ekledi (`Son Dosyalar`, `Karşılama Kartını Göster`); W4-04 (Görünüm → Ses) aynı yolu izleyecek.

**Uygulama (6 Kas 2026):** `ui/MainMenu` menü düğmesini (`MenuButton`, etiketi `☰`) üretiyor ve
sınıf `MenuButton`'ı da miras almıyor, kompozisyon kullanıyor: JavaFX `Control`'ün statik
başlatıcısı toolkit ister ve saf `layout()` testini "Toolkit not initialized" ile düşürürdü. Saf
`layout()` dört `MenuSpec` döndürüyor (Dosya 4, Düzen 2, Görünüm 8, Yardım 0) ve 14 platform-dışı
`EditorAction`'ın her birini tam bir kez içeriyor; `EXIT_FULLSCREEN` JavaFX'in kendi
`fullScreenExitKeyCombination`'ında kaldığı için menüde yok. Etiketler `action.label(messages)`,
hızlandırıcılar `action.primaryChord()`; JavaFX `"Ctrl++"` ayrıştıramadığı için yalnız bu akor
`Ctrl+Plus` olarak veriliyor ve ekranda yine `Ctrl++` görünüyor. Hızlandırıcılar **açıklamadır**:
tuşu sahne seviyesindeki `KeyboardShortcutHandler` filtrede yakalayıp tüketiyor, menü öğesinin
kendi akoru hiç çalışmıyor - ikisi de aynı `primaryChord()`'dan beslendiği için yalan söyleyemez.
`Yardım → Hakkında` bilinçli istisna: dialog bir görünüm, belge komutu değil; `EditorAction`'a
çevirmek kısayol kartına sahte bir akor eklerdi (gerekçe sınıf Javadoc'unda).

Düğme araç çubuğunun **çocuğu** (ilk sırada, `viewOrder` 10 = `Tab` sırasında ilk): böylece
`Ctrl+T` menüyü de gizliyor, ayrı bir üst satır kalmıyor ve genişlik `ToolbarFit` modeline dahil
oluyor (`fitControls()` artık `MainMenu.GLYPH` ile başlıyor; test bunu pinliyor). `ToolbarView`
düğmenin tooltip'ini ve erişilebilir adını diğer kontrollerle aynı `refresh()` yolundan okuyor.
`base.css` ona `.toolbar-button` görünümünü verip oku gizliyor (`.toolbar-menu-button`), metin
rengi `-toolbar-text` olduğundan `ContrastRatioTest`'in mevcut AA ölçümü onu da kapsıyor. Yeni
anahtar `toolbarMenu` (6 bundle + `Messages`); dört menü başlığı `menuFile/menuEdit/menuView/
menuHelp`. Ölçülen satır genişlikleri (compact): FR 817 px, ES 866 px - 900 px sınırının altında;
DE/FR tam satır 1280 px'te sığıyor. Testler: `MainControllerTest` 10/10 (3 yeni: komut kümesi ↔
menü ağacı birebir, altı dilde boş olmayan başlık/etiket, Yardım'ın komut taşımaması),
`ToolbarFitTest` 13/13 (1 yeni: menü düğmesi ölçülen satırın ilk kontrolü), `MessagesTest` 12/12.
Mutasyon: `layout()`'tan `SAVE_AS` çıkarılınca birebir kapsam testi kırmızı (`expected: <14> but
was: <13>`), `fitControls()`'tan menü satırı çıkarılınca ölçüm testi kırmızı (ikisi de geri
alındı). `mvn test -Dtest=MainControllerTest,ToolbarFitTest,MessagesTest` → 10/10 + 13/13 + 12/12;
`mvn -B clean test` → 443/443.

**Takip (6 Kas 2026): menü popup'ı da temalı.** `MenuButton`'ın açtığı şey bir
`ContextMenu`'dür - combobox listeleri gibi ayrı bir popup penceresi - ve Modena varsayılanıyla
geliyordu: üç temada da aynı gri kart, çevresindeki temalı araç çubuğunun yanında. `base.css`'e
`.context-menu` kuralları eklendi (yüzey `-toolbar-border`/`-toolbar-bg`, satır `-toolbar-text`,
vurgulu satır `-toolbar-hover`, ayırıcı `-toolbar-separator-color`, alt menü/check okları satır
rengi); hem üst düzey satırlar hem alt menüler aynı `ContextMenu` sınıfını taşıdığı için tek kural
kümesi ağacın tamamını boyuyor. Hızlandırıcı sütunu `-toolbar-text` + 10 px: `-stats-text` hover
yıkamasında açık temada 3,18:1'de kalıyordu, yani AA'yı geçmiyordu. İki yeni test:
`commandMenuPopupIsPaintedFromTheThemeTokensInEveryTheme` (popup yüzeyi ve vurgulu satır base.css'ten
okunup altı ölçümde AA'ya karşı doğrulanıyor - CSS bloğu silinince "no -fx-background-color found
for selector \.context-menu" ile kırmızı, geri alındı) ve `commandMenuAcceleratorRowsFollowTheThemeAndStayLegible`.
`mvn test -Dtest=ContrastRatioTest` → 25/25; `mvn -B clean test` → 445/445. Not: bu makinede
Xvfb/Monocle yok, popup'ın görsel doğrulaması yapılamadı; doğrulama CSS ayrıştırma (CssParser,
83 kural, hata yok) + kontrast ölçümü düzeyinde.

**Takip (6 Kas 2026): Görünüm artık değer alt menüleri.** Görünüm'deki Tema / YOLO / Ses / Renk /
Boyut satırları sıralı komut değil, ikinci seviye birer alt menü: araç çubuğundaki açılır listelerin
aynı seçeneklerini listeliyor ve o anki değeri işaretli gösteriyor (radio item). Karar nedeni:
araç çubuğu zaten doğrudan seçim sunuyor, yani menüdeki `Görünüm → Tema → Koyu` "sıradaki tema"
değil, Koyu demeli. Altı ayar komutu (`CYCLE_THEME`, `TOGGLE_YOLO`, `CYCLE_TEXT_COLOR`,
`FONT_UP/DOWN/RESET`) kısayollarını ve kısayol kartı satırlarını koruyor; yalnız menüdeki
temsilleri değişti. `Sound`'un `EditorAction`'ı yok, bu yüzden alt menüler komuttan değil
`MainMenu.Settings` portundan üretiliyor; portu `ToolbarView` uyguluyor (`choice(ChoiceKind)`)
ve aynı `selectTheme/selectYolo/selectSound/selectTextColor/selectFontSize` yollarını kullanıyor -
menü tek durumun ikinci görünümü, ikinci durumu değil. `MainMenu.layout()` artık `Entry`
(`ActionEntry` | `ChoiceEntry`) döndürüyor; `submenuBackedActions()` birebir kapsam testinin
dışladığı kümeyi pinliyor ve test iki yönlü: ne eksik ne tekrar. Popup her açılışta
(`ON_SHOWING`) yeniden kuruluyor, böylece işaretli satır canlı değeri izliyor. Ayrıca işaretli
radio/check imleci `-toolbar-text`'e bağlandı (Modena'nın checked kuralı daha yüksek özgüllükte).
`MainControllerTest` 13/13 (3 yeni: beş alt menü sırası, `resolve()` işaretleme+seçim, alt menü
kümesi pinli); mutasyon: `layout()`'tan `SOUND` çıkarılınca alt menü sırası testi kırmızı
(`[THEME, YOLO, TEXT_COLOR, FONT_SIZE]`, geri alındı). `mvn -B clean test` → 448/448.

**W3-03 · A4c — CLI / süreç argümanı** · S · E3
- **Kapsam:** `JTypeWriterApp`/`FxApplication` (`getParameters()`), `package.sh` launcher
- **Yapılacak:** `JTypeWriter <dosya>` belgeyi açar; çift tıklama yolu aynı kodu kullanır.
  İkinci örnek devir **yapılmaz** (kilit reddeder, mevcut davranış).
- **Bitti tanımı:** Argümanlı başlatmada dosya açılıyor; argüman yoksa karşılama; geçersiz yol
  sessizce yutulmuyor.
- **Doğrulama:** `scripts/smoke-appimage.sh --open-sample` (W1-01'e eklenir) +
  `mvn test -Dtest=JTypeWriterAppTests`.
- **Uygulama (7 Kasım):** `StartupArguments` (saf, JavaFX'siz) `getParameters().getRaw()`'ı
  çözer; `FxApplication.start` kurtarma teklifini önce sorar (taslak tek kopya, komut
  satırındaki dosya diskte), başarılı açılışta karşılama kartını bastırır ve reddedilen
  argümanı arayüz dilinde adıyla bildirir (`INVALID_PATH`/`MISSING`/`NOT_A_FILE`/
  `TOO_MANY_ARGUMENTS`). Sembolik bağlantı kendi adını korur
  (`toAbsolutePath().normalize()`, `toRealPath()` değil), çünkü yazma yolu bağlantıyı zaten
  izliyor. Birden çok argüman ilkini seçmek yerine reddedilir; ikinci örnek devri yoktur.
  5 yeni anahtar altı dilde (6 × 125) ve `Messages` erişimcileri `MessagesTest` ile eşleşiyor.
  `--open-sample` smoke'a eklendi: Linux'ta pencere başlığı `JTypeWriter - cli-sample.txt`
  doğrulanıyor, Windows'ta launcher argümanla ayakta kalıyor; CI smoke adımı artık
  `--skip-build --open-sample` çalıştırıyor ve `CiGatesTest` bunu pinliyor.
  İlk Windows koşusu bir test kusuru buldu: göreli yol testi `@TempDir` (C:) ile Surefire'ın
  çalışma dizini (D:) arasında `relativize` çağırıyordu ve Windows farklı köklerde bunu
  reddediyor. Test artık göreli yolu CWD'nin kendi ağacında (`target/`) kuruyor; üretim kodu
  değişmedi (`toAbsolutePath()` zaten kök bağımsız).
  **Kanıt:** `mvn -B clean test` → **466/0/0**; `mvn test -Dtest=StartupArgumentsTest,
  JTypeWriterAppTests,CiGatesTest,MessagesTest` → **34/0/0**;
  `scripts/smoke-appimage.sh --open-sample --skip-deb` → **PASS** (imaj 2 s'de, örnek belge
  başlığıyla 2 s'de açıldı); `scripts/verify-artifacts.sh --skip-build` → **20/20**.

**W3-04 · A4d — Sürükle-bırak** · S · E4
- **Kapsam:** `MainStage`/`EditorArea` (`Dragboard`)
- **Yapılacak:** `.txt`/`.md` bırakma; kirli belgede **mevcut çıkış matrisinden** geçer — yeni
  kaydetme yolu icat edilmez.
- **Bitti tanımı:** Kirli belgeye bırakma `ExitDisposition` üzerinden soruyor (test, saf model
  düzeyinde).
- **Doğrulama:** `mvn test -Dtest=ExitDispositionTest,MainStageTest`.
- **Uygulama (7 Kasım):** Karar saf bir kayda taşındı: `ui/DroppedFiles` bir dosya listesini
  tek okunabilir `.txt`/`.md` belgesine indirger; dosya olmaması, birden çok dosya, başka tür,
  klasör ve kaybolmuş yol ayrı `Problem` değerleri olarak adıyla bildirilir. `ui/DropHandler`
  yalnızca tutkal: jesti **sahne filtresi** olarak kurar (`DRAG_OVER`/`DRAG_DROPPED`), böylece
  bırakma pencerenin her yerinde çalışır ve `TextInputControl` onu sürüklenen metin sanmadan
  önce yakalanır; dosya taşımayan jest hiç dokunulmadan geçer, yani düzenleyici içi metin
  sürükleme bozulmaz. Açma yolu yeni bir kaydetme yolu icat etmiyor:
  `FileService.openPathAsync`, Aç/Yeni ile aynı `confirmDiscardOrSave` matrisinden geçiyor;
  kirli belgede SAVE/DISCARD/CANCEL aynen sorulur ve iptal edilen kaydetme düzenleyiciyi olduğu
  gibi bırakır. `DropContractTest` bunu kaynak düzeyinde pinliyor (ikinci soru yok, doğrudan
  `saveFile` yok, `FileService` dışında hiçbir kaynak `showUnsavedChangesDialog` çağırmıyor).
  Ret sessiz değil: kabul edilen jest sonra arayüz dilinde adıyla/adediyle hata diyalogu açar.
  6 yeni anahtar altı dilde (6 × 131). Bu kart yazılırken ayrıca bir i18n kusuru bulundu ve
  onarıldı: `MessageFormat`'a giden bir desende tek `'` sessizce yutuluyor, `getString` ile
  okunan düz bir değerde ise `''` kullanıcıya iki kesme işareti olarak ulaşıyor. `MessagesTest`'e
  bu sözleşmeyi anahtar bazında pinleyen bir test eklendi (formatted anahtarlar `''`, düz
  anahtarlar tek `'`); mutasyonla (FR `dropOpenFailedHeader` → `''`) kırmızı olduğu görüldü.
  W3-03'te sevk edilen `startupOpenFailedHeader` da aynı hatayı taşıyordu, o da düzeltildi.
  Kart "`ExitDisposition` üzerinden" diyor; ölçülen karşılığı `FileService.confirmDiscardOrSave`:
  `ExitDisposition` taslağın *çıkışta* ne olacağını söyleyen enum, belge değiştirmede hiç
  çağrılmıyor. Kapatma isteği de, Aç da, Yeni de aynı SAVE/DISCARD/CANCEL matrisinden geçiyor;
  bırakma da o matristen geçiyor.
  **Kanıt:** `mvn -B clean test` → **488/0/0**; `mvn test -Dtest=DroppedFilesTest,DropContractTest,FileServiceTest,MessagesTest`
  → **13/13 + 5/5 + 50/50 + 13/13**; `mvn test -Dtest=ExitDispositionTest,MainStageTest`
  → kartın istediği hedef yeşil.

**W3-05 · A2a — `FindModel` (saf model)** · M · E1
- **Kapsam:** yeni `ui/FindModel` (JavaFX'siz)
- **Yapılacak:** Eşleşme listesi, ileri/geri, büyük-küçük harf duyarlılığı, tümünü değiştir,
  sarmalama; `TextCodec`/`SessionStats` gibi toolkit'siz ve testli.
- **Bitti tanımı:** Model toolkit açmadan test ediliyor; boş desen, regex yok, taşan aralık,
  değiştir-sonrası-kayma senaryoları testli.
- **Doğrulama:** `mvn test -Dtest=FindModelTest` (yeni).
- **Uygulama (7 Kasım):** `ui/FindModel` tek dosyada üç kayıt + iki eşleşme kuralı taşıyor:
  `Match(start,end)` yarım-açık aralık, `Replacement(document,count,current)` sonucu.
  Kararlar: (1) **Sorgu düz metindir, regex değil** — `a.*b` beş karakter olarak aranıyor,
  `$1` dört karakter olarak yazılıyor; regex sözdizimi hiçbir yerde yorumlanmadığı için geçersiz
  desen diye ikinci bir hata yolu da yok. (2) **Eşleşmeler çakışmasız ve soldan sağa** —
  `aaaa` içinde `aa` iki eşleşme; her eşleşmeden sonra tarama onun sonundan devam ediyor, bu da
  "tümünü değiştir"i doğrusal tutuyor. (3) **Büyük-küçük harf varsayılan yerel ayara bağlı
  değil** — `regionMatches(...)` kullanılıyor, `toLowerCase()` değil; Türkçe varsayılanda `I`,
  `toLowerCase()` ile `ı` olurdu ve aynı sorgu makineye göre farklı metin bulurdu (test `tr-TR`
  varsayılanı altında koşuyor). (4) **Sarmalama iki yönde** — son eşleşmede `F3` ilkine, ilk
  eşleşmede `Shift+F3` sonuncusuna; arama ilk eşleşmenin *önünde* başlıyor ki ilk `F3` onu
  atlamasın. Eşleşme yoksa her iki yön de boş kalıyor (boş liste üzerinde sarmak sahte bir
  isabet olurdu). (5) **Değiştirme indeksleri elle kaydırmıyor, yeniden tarıyor** — eşleşme
  boyu değiştiği için tek geçişte yeni metin kuruluyor, ardından liste yeniden taranıyor; "tek
  değiştir" imleci eklenen metnin *ötesindeki* ilk eşleşmeye koyuyor, yani bir sonraki
  `Değiştir` kendi çıktısına basmıyor. "Tümünü değiştir" kendi çıktısını **taramasının
  sürücüsü yapmıyor** (`a` → `aa` sonsuz döngü olurdu); geçiş özgün metnin eşleşmeleriyle
  sürülüyor ve dönen sayı tam. (6) **Sığmayan aralık kırpılmıyor, reddediliyor** — panel
  önceki metnin taramasını tutarken kullanıcı yazmaya devam edebilir; belge sonunu aşan bir
  aralık `substring` ile `StringIndexOutOfBoundsException` atardı, bu yüzden `replace(...)`
  boş dönüyor ve `install(...)` sığmayan aralıkları düşürüyor.
  Thread sınırı da modelde: `findAll(document, needle, caseSensitive)` **saf statik** ve her
  iş parçacığında güvenli (W3-06 paneli 10 MB taramayı FX thread'inde yapmayacak), sonuç
  `install(...)` ile geri veriliyor; tarama sırasında belge ya da sorgu değiştiyse kurulum
  reddediliyor (bayat sonuç sessizce uygulanmıyor).
  **Kanıt:** `mvn test -Dtest=FindModelTest` → **30/30**; `mvn -B clean test` → **518/0/0**.
  Mutasyon: tarama `at + 1` ile ilerletilince (çakışan aralıklar) 3 test kırmızı;
  `startsWith` ile büyük-küçük harf duyarsızlığı atılınca 2 test kırmızı; değiştirme sonrası
  imleç `start`'ta bırakılınca 1 test kırmızı; taşma koruması kaldırılınca 1 hata (exception).

**W3-06 · A2b — Arama/değiştirme paneli** · M · E1
- **Kapsam:** yeni panel + `Ctrl+F` / `F3` / `Shift+F3` / `Ctrl+H` + `MainMenu` öğeleri
- **Yapılacak:** Panel araması FX thread'inde **tam metin taraması yapmaz** (W2-01/W2-02'nin
  üstüne kurulur); YOLO açıkken "tümünü değiştir" **onay ister**.
- **Bitti tanımı:** (1) 10 MB belgede arama UI'yı kilitlemiyor (ölçülen süre kaydedilir).
  (2) YOLO etkileşimi testli (`YoloPolicy` ile aynı dil). (3) Aynı işlevler komut menüsünden
  (araç çubuğundaki `☰`) de erişilir.
- **Doğrulama:** `mvn test -Dtest=FindModelTest,YoloPolicyTest` + manuel 10 MB ölçümü.
- **Uygulama (7 Kasım):** Panel bir modal pencere değil, düzenleyicinin üstünde sağ üstte
  duran **yüzen bir kart** (`MainController` onu bir `StackPane` içinde `TOP_RIGHT` hizalar):
  arama bir kiptir, belge görünür kalmalı ve `F3` ile art arda gezilirken pencere her adımda
  yeniden açılmamalıdır. Düzen Notepad++ diyaloğundan alındı: tek "Aranan" alanı, tek "Yeni
  metin" alanı, "Büyük/küçük harf duyarlı" onay kutusu, Sonrakini/Öncekini bul · Değiştir ·
  Tümünü değiştir ve canlı durum satırı ("3 / 12", "12 eşleşme", "Eşleşme yok", "Aranıyor...").
  Nedenler: (1) **Tarama FX thread'inde değil** — `FindScanService` kendi daemon worker'ında
  `FindModel.findAll` çağırıyor, sonucu FX thread'ine dağıtıyor; panel bir **nesil sayacı** ile
  eski tamamlanmayı düşürüyor ve model de `install(...)` ile ikinci kez doğruluyor, yani bir
  karakter daha yazıldıktan sonra gelen tarama seçim yapamıyor. Yazma 120 ms'lik bir
  `PauseTransition` ile birleştiriliyor. (2) **Kendi worker'ı, paylaşılan `IoExecutor` değil** —
  10 MB'lık bir tarama bir kaydetmenin önüne geçmemeli; disk işlerini serileştiren kuyruğun
  arama ile ilgisi yok. (3) **`F3`/`Shift+F3` kart kapalıyken de çalışır**; sorgu boşsa kart
  açılır, tarama sürüyorsa yön saklanıp sonuç kurulunca yürütülür. (4) **`F3` ile `Shift+F3`
  ayrı hızlandırıcılar** — `plain("F3")` her Shift durumunu kabul ettiği için `resolve` önce
  `FIND_NEXT`'e dönüyor ve `Shift+F3` ulaşılamaz kalıyordu; `FIND_NEXT` artık `noShift(...)`
  kullanıyor, `FIND_PREVIOUS` ise `shiftOnly(...)`; `KeyboardShortcutHandlerTest` bu ikisinin
  ayrı kaldığını pinliyor. (5) **YOLO onayı** — değiştirme eşleşen aralığı siler, yani YOLO'nun
  "hiçbir kullanıcı düzenlemesi karakter silmez" sözünün tam kapsamına girer; `FindReplacePolicy`
  açık/kapalı kararı veriyor (saf, testli) ve YOLO açıkken `DialogService.confirmReplaceInYolo`
  bir kez soruyor; iptal hiçbir şey yazmıyor. Değiştirme `EditorArea.replaceConfirmed` üzerinden
  gidiyor — YOLO hunisinden geçseydi `INSERT_WITHOUT_DELETION` eşleşmeyi koruyup yanına yeni
  metni ekler, yani metni çoğaltırdı; bu tek ve belgelenmiş baypas. (6) **Tümünü değiştir tek
  geri alma adımı** — yeni belge `0..length` aralığına tek düzenleme olarak yazılıyor (setText
  değil, o geri alma geçmişini silerdi); model kendi çıktısını taramadığı için `a` → `aa`
  sonlanıyor. (7) **Tema** — kart yalnızca mevcut belirteçlerden boyanıyor ve üç tema dosyasının
  belirteç seçicisine `.find-panel` eklendi; kart bir `.dialog-pane` olmadığı için Modena'nın
  metin alanı/onay kutusu renkleri koyu temada beyaz sızardı, bu yüzden alanlar açıkça
  boyandı. (8) **Menü ve araç çubuğu** — dört komut da `EditorAction` envanterinde, yani
  `Ctrl+F`/`F3`/`Shift+F3`/`Ctrl+H`, araç çubuğundaki `⚲` düğmesi ve Düzen menüsü tek tanımı
  paylaşıyor; `MainControllerTest` Düzen menüsündeki dört yaprağın sırasını pinliyor.
  (9) **Panelin kendi düzenlemesi taramayı yeniden başlatmaz** — değiştirme belgeyi yazar ve
  model o metni zaten benimsemiştir (imleç eklenen metnin ardında durur); belge dinleyicisi
  yalnızca modelin anlatmadığı yeni metinde tarama planlar. Aksi hâlde her `Değiştir` imleci
  -1'e düşürür ve sonraki `Değiştir` eklenen metnin içine inerdi (`a` → `aa` her tıklamada
  büyürdü). Değiştirme yolları ayrıca bayat modele karşı kapılıdır: belge kullanıcı tarafından
  değişmişse işlem taze taramanın üstünde yeniden çalışır, eski anlık görüntüden üretilmiş bir
  belge asla yazılmaz.
  **Ölçüm:** gerçek modelle 10 MiB belgede tam tarama 26–53 ms (0 eşleşme / 197 845 eşleşme /
  tek harf 1 187 069 eşleşme), hiçbiri FX thread'inde değil; `FindScanServiceTest`
  `fxThreadScans() == 0` diyor.
  **Kanıt:** `mvn -B clean test` → **524/0/0**; `mvn test -Dtest=FindModelTest,ToolbarFitTest,EditorActionTest,KeyboardShortcutHandlerTest,ShortcutRowsTest,FindReplacePolicyTest,FindScanServiceTest,MainControllerTest,MessagesTest`
  → **103/103**; taşma testleri DE/FR compact satırları ≤ 836 px, tüm diller ≤ 900 px.
- **Tasarım takibi (7 Kasım):** Hoş geldin ve Hakkında görünümleri de aynı köşeye taşındı.
  İkisi de çerçevesiz, modal, ayrı `Stage`'lerdi: ürünün ikinci penceresi, ikinci sahnesi ve
  ikinci stil sayfası listesi, üstelik belgeyi kilitliyorlardı - kullanıcı karttan bir kısayolu
  kopyalayamıyor, kapatınca imleç metne dönmüyordu. Artık `OverlayCard` (ortak kart tabanı:
  `welcome-root` yüzeyi, `dialog-scroll` kaydırma kabı, `Escape`/`Enter` kapatma, odak geri
  verme) ve `WelcomeCard`/`AboutCard` onun üzerinde. Köşe tek: `OverlaySlot` hangi kart
  açılırsa açılsın diğerlerini (ara paneli dahil) kapatıyor - iki kart yan yana o marginlere
  sığmadığı, üst üste de belgeyi iki kez örttüğü için tek yuva kuralı. Kartların sınırı ekran
  değil pencere: `maxHeight` sahne yığınının yüksekliğine bağlı, içerik kaydırılıyor. İçerik
  her açılışta paketten yeniden kurulduğu için dil değişimi açık kartı bayat bırakamıyor;
  odak bir darbe sonrasına erteleniyor çünkü menüden açılan kart, açılır listenin kapanırken
  odağı kendi düğmesine geri vermesiyle yarışıyor. Bu taşınma `DialogSizing.clampHeight`/
  `clampWidth`'i kartlardan çıkardı (`Theme.getDialogFill` de ölü kaldı ve silindi; Alert
  yolu aynı kelepçeleri kullanmaya devam ediyor) ve iki pencere başlığı anahtarını
  (`welcomeWindowTitle`, `aboutWindowTitle`) gereksiz bıraktı; köşe kapatma etiketi tek bir
  `overlayClose` anahtarı (`findClose` yerine) olarak yaşıyor.
  **Kanıt:** `mvn -B clean test` → **528/0/0**; `OverlaySlotTest` üç kuralı (biri açılınca
  diğerleri kapanır, aynı kart iki kez kaydolursa bir kez kapanır, kayıtsız sahip hepsini
  kapatır), `OverlayCardTest` `Escape`/`Enter` dışındaki tuşların karta bırakıldığını pinliyor.
- **Kart sadeleşmesi (7 Kasım):** Alttaki eylem düğmeleri kaldırıldı (hoş geldinde
  `welcomeButton`, Hakkında'da `aboutClose`) ve köşedeki `×` yerelleştirilmiş `Kapat`
  sözcüğüne döndü - ara panelin `.find-close` düğmesiyle aynı fikir, yani kartın tek düğmesi
  o. Odağın karta girmesi gerekiyordu ve artık tek düğme ona veriliyor; `Tab` kartın
  kontrollerini gezmeye devam ediyor ve düğme `:focused` durumunda odak halkasını gösteriyor.
  Kartın içeriği artık bir eylem değil, okunacak bir metin. Bu, `welcomeButton`/`aboutClose`
  anahtarlarını (6 × 146), `Messages` erişimcilerini, `-welcome-button-text` belirtecini ve
  boşta kalan `.welcome-button` kural setini ölü bıraktı; hepsi silindi.
  **Kanıt:** `mvn -B clean test` → **527/0/0** (`ContrastRatioTest` hâlâ `-welcome-button-bg`
  belirtecini e-posta bağlantısının metin rengi olarak denetliyor).
- **Ortak kolon (7 Kasım):** İki kart `Close` düğmesinden sonra neredeyse aynı görünüyordu
  (`welcomeTitle` altı pakette de `appName` ile birebir aynı). Bu yüzden "WelcomeCard'a gerek
  yok" sorusu haklıydı; ama birleştirmek yerine fark ayrıştırıldı: mark + başlık + tagline +
  kısayol ızgarası artık `OverlayCard.cardColumn(...)` içinde tek yerde üretiliyor, kartlar
  yalnızca kendi eklerini (About: sürüm satırı, telif/lisans/garanti, e-posta) ekliyor.
  `Mark` boyutu, içerik dolgusu ve genişlik türetmesi de tabana taşındı; `Messages` tabanın
  alanı oldu (kapatma etiketi de oradan). WelcomeCard 81 → 46, AboutCard 129 → 101 satır.
  Kartlar ayrı kaldı çünkü biri açılışta her seferinde görünen karşılama, diğeri bir kez
  sorulan referans: About'u açılışa koymak her başlatmada sürüm + lisans + garanti gösterirdi
  ve W4-02'nin son dosyalar + opt-out işini About'a yüklerdi.
  **Kanıt:** `mvn -B clean test` → **528/0/0**; `DialogSizingTest.neitherCardRebuildsTheSharedColumn`
  iki alt sınıfın tagline/kısayol ızgarasını yeniden kurmasını yasaklıyor.

> **Kapı G3** (`analysis.md` §13.6/2 ve /3): `Ctrl+F` bulur, `F3`/`Shift+F3` gezer, `Ctrl+H`
> bir/tümünü değiştirir; aynı işlevler komut menüsünden (araç çubuğundaki `☰` düğmesi) erişilir
> ve menüye eklenen her komut bir `EditorAction`'dır (menü ↔ envanter birebir testi); arama
> 10 MB'da kilitlenmez; tüm tarih/saat gösterimi UI dilini kullanır; bütün `Alert`'ler 768 px'e
> sığar (W0-01'den miras).

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
- **Uygulama (8 Kasım 2026):** Dosya artık kendisini yazan yapıyı `schema.version` ile
  adlandırıyor ve bu değer **ürün sürümüdür** - `pom.xml`'deki `<version>`. Ayrı bir "şema
  sayacı" yok: iki numara, "beni kim yazdı" ile "şeklim ne" sorusunu iki ayrı cevapla
  yanıtlar ve er geç çelişir. Tek numara ikisini de yanıtlar, çünkü kalıcı şekli yalnızca bir
  **sürüm** değiştirebilir: yeni bir ayar anahtarı eski yapının yok sayacağı bir anahtardır
  (göç gerekmez), yeniden adlandırma/kaldırma ise bir sürümle çıkar. Sürümün tek kaynağı
  `pom.xml`: çalışan programa değer, `maven-resources-plugin`'in `process-resources`'ta
  filtreleyip yazdığı `org/mesutormanli/jtypewriter/version.properties` üzerinden gider.
  Neden Maven'ın kendi `pom.properties` dosyası yetmedi: onu `maven-jar-plugin` `package`'ta ve
  yalnız **arşivin içine** yazıyor; sınıf dizininden (`target/classes`) çalışan her yol -
  `mvn javafx:run`, IDE, test koşusu - hiçbir sürüm dosyası bulamıyordu. Hakkında'da **"dev"**
  görülmesinin nedeni buydu: sürüm jar'ın içinde vardı, kullanıcının uygulamayı çalıştırdığı
  yerde yoktu. Filtrelenmiş kaynak derlemeden önce `target/classes`'a düşer ve her arşive
  paketlenir, yani dört yol da aynı numarayı okur.
  Göç tek adımdır: anahtarsız dosya (ya da sürüm olmayan değer) sürümsüzdür ve şekli bu
  yapının okuduğu şekille aynıdır (hiçbir anahtar yeniden adlandırılmadı/kaldırılmadı), yani
  göç sürümü damgalamaktır. Göç **yalnız bellekte** koşar (`AppSettings.migrate`, saf ve
  testli); okuma dosyayı yeniden yazmaz - eski bir yapının yeni bir dosyayı yalnızca bakarak
  yeniden yazması "yayınlanan şey değişmez" kuralının ihlali olurdu. Daha **yeni** bir sürüm
  (ör. `99`) reddedilmez: bilinmeyen anahtarlar ve sürüm değeri olduğu gibi korunur, sonraki
  kayıt onları silmez ve sürümü geriye düşürmez; aksi halde eski bir yapı, yeni yapının
  dosyasını kendi şekli sanıp damgalardı. Karşılaştırma sayısal parçalar üzerinden yapılır
  (`1.0 == 1.0.0`, `1.0.10 > 1.0.9`); eksik parça sıfır sayılır.
  Yazma hatası artık sessiz değil: `save()` `IOException` yanında `SecurityException`'ı da
  yakalar (ikisi de kullanıcı için "yazılmadı" demektir), değeri bellekte bırakır ve
  `isSettingsWriteFailurePending()` bayrağını kaldırır. Bayrak **hemen gösterilemez**: ayarlar
  pencere yokken yazılır (tema, geometri, son belge), o yüzden `AppGraph` bayrağı açar -
  açılışın sonunda ve her belge kaydında - ve diyalog döndükten **sonra** temizler; böylece
  gösterilemeyen bir uyarı kaybolmaz (depolama izi bildiriminin aynı deseni). Metin altı dilde,
  dosya adı argüman olarak: kullanıcının eyleme geçebileceği tek bilgi o.
- **Doğrulama (yeni testler):** `AppSettingsTest` 18 → **25**: şema öncesi dosya değer
  kaybetmeden okunuyor ve okuma dosyayı yeniden yazmıyor; ilk kayıt ürün sürümünü damgalıyor;
  eski bir sürümün (`0.9.9`) dosyası ilk kayıtta ileri damgalanıyor ve değerler korunuyor;
  `schema.version=99` + bilinmeyen anahtar kayıtta korunuyor; bozuk sürüm değeri sürümsüz
  sayılıyor; sürüm karşılaştırması sayısal parça testleriyle pinli; POSIX salt-okunur klasörde
  `setTheme` fırlatmıyor, bayrak kalkıyor ve bir kez raporlanıyor. `AppVersionTest` 4 → **5**:
  `pom.xml` sürümü = çalışan sürüm = şema sürümü, ve filtrelenmiş kaynak **var** (bu test
  olmasaydı eksik kaynak sessizce "dev"e düşerdi). `SecurityException` dalı unit test
  edilemiyor - JDK 25'te `SecurityManager` kaldırıldığı için onu üreten bir ortam yok; aynı
  raporlama yolunu `IOException` dalı (salt-okunur klasör) test ediyor.

**W4-02 · A7a — Son dosyalar + karşılama opt-out** · M · E5, U7
- **Kapsam:** `AppSettings`, `WelcomeCard`, `FxApplication` (karşılama kartının açıldığı yer), `MainMenu` (Dosya menüsü)
- **Yapılacak:** En çok 10 son dosya (silinmişse sessizce düşer); karşılamada "bir daha
  gösterme" (ayar olarak).
- **Bitti tanımı:** Silinmiş dosya listede kalıcı hata üretmiyor (test); opt-out sonrası
  karşılama açılmıyor (test, saf model).
- **Doğrulama:** `mvn test -Dtest=AppSettingsTest,MainControllerTest`.
- **Uygulama (8 Kasım 2026):** Liste kararının tamamı saf bir modelde: `RecentFiles`
  (`withNewest`, `normalized`, `keepingOnly`, `MAX = 10`). Kimlik **mutlak ve normalize
  yol**tur, yani aynı dosya göreli bir yolla ya da `..` üzerinden açıldığında ikinci bir satır
  olmaz, başa taşınır. Sıra yeniden-eskiyedir ve tavan aşıldığında **kuyruk** (en eski) düşer.
  Silinmiş dosya `Files::isRegularFile` ile süzülür: kart her açılışta değil ama tek tıkla
  açıldığı için tıklandığında hata veren bir satır, kullanıcının temizleyemediği kalıcı bir
  hata olurdu; var olmayan belge bu yüzden **sessizce** düşer - oturum geri yüklemenin
  kaybolmuş son belge için zaten izlediği kural. Bozuk/el ile düzenlenmiş bir anahtar
  (`InvalidPathException`) listenin geri kalanını gizlemez, atlanır.
  Depolama `recent.file.0…9` indeksli anahtarlardadır; yazarken önce **tüm** yuvalar silinir,
  çünkü liste yalnız kuyruğundan küçülür ve yeni boyutun ötesinde kalan eski bir anahtar
  kullanıcının açmadığı bir belge olarak geri gelirdi. `rememberFile` (açma **ve** kaydetme)
  son belgeyi ve son klasörü yazarken son dosya listesini de **aynı kayıtta** günceller: iki
  tüketici tek olayı paylaşıyor, tek dosya durumu kalıyor.
  Opt-out `welcome.enabled` (varsayılan açık) olarak saklanır; karttaki onay kutusu bunu
  **anında** yazar ve `FxApplication` açılışta yalnız bu değere bakar. Onay kutusu bir kez
  yönlü kapı değildir: `Dosya → Karşılama Kartını Göster` tercihi temizleyip kartı hemen
  açar, yani kart açıldığında onay kutusu yine işaretsizdir. Onay kutusu bir **düğme değil**:
  düğme "Kapat" ile "bir daha gösterme"nin aynı jest olup olmadığını tahmin etmek zorunda
  kalırdı; onay kutusu tercihi kendisi söyler ve kapatma sıradan bir Kapat olarak kalır.
  **Liste ayrı bir karttadır** (kullanıcı kararı, 8 Kasım 2026): ilk uygulama listeyi hoş
  geldin kartına koymuştu, ama bu, listeyi kullanıcının kapatabileceği bir karşılamaya
  bağlıyordu - karşılanmak istemeyen biri belgelerini yine de ister. `RecentFilesCard` dördüncü
  köşe yüzeyidir (`OverlaySlot`), `Dosya → Son Dosyalar` ile her an açılır, `welcome.enabled`
  kapalıyken de erişilebilir kalır. Satır **dosya adıyla** adlandırılır (tam yol ipucu ve
  erişilebilir ad olarak kalır): mutlak yollardan oluşan bir liste okunmaz ve ev dizinini her
  ekran görüntüsüne taşır. Boş liste boş panel göstermek yerine bunu söyler (`recentEmpty`).
  Satırlar `Open` ve sürükle-bırakla **aynı** kapıdan geçer (`FileService.openPathAsync`):
  İptal, başarısız kaydetme ve kodlama sorusu oradakiyle birebir aynıdır, kart düzenleyiciyi
  sessizce değiştiren ikinci bir yol olmaz.
  Metin altı dilde: `welcomeOptOut`, `recentTitle`, `recentEmpty`, `menuRecentFiles`,
  `menuShowWelcome` (6 bundle × 155 anahtar + beş `Messages` erişimcisi).
- **Doğrulama (yeni testler):** `RecentFilesTest` **6**: en yeni başta; yeniden açılan belge
  çoğalmaz, başa taşınır; tavan aşılmaz ve düşen **en eski**dir; var olmayan belge süzülür;
  boş liste iki yönde de geçerli; `null` yol yok sayılır. `AppSettingsTest` 25 → **31**:
  son dosyalar yeniden başlatmada en yeni başta; tavan + tekrar aynı testte; silinen belge
  listeden sessizce düşer ve okuma fırlatmaz; kullanılamayan girdi atlanır; karşılama
  varsayılan açık ve opt-out kalıcı; tercih geri açılabiliyor. Dosya silme/erişim testleri
  `@TempDir` altında gerçek dosyalarla koşar, gerçek `~/.jtypewriter`'a dokunmaz.
  `StartupGreetingTest` **3**: taze kurulum karşılanıyor; opt-out sonrası karşılama
  açılmıyor; komut satırında adlandırılan belge hiçbir durumda kartın altında kalmıyor. Karar
  `FxApplication.greetsAtStartup` içinde tek bir saf fonksiyondur, açılış sırasındaki gizli bir
  dal değil - yani "opt-out sonrası açılmıyor" iddiası toolkit olmadan pinlenmiş olur.
  `RecentFilesCardTest` **2**: satır dosya adıyla adlandırılır; son öğesi olmayan bir yol
  (kök) boş satır üretmek yerine tam yolu gösterir.
  **Windows CI düzeltmesi (8 Kasım 2026):** ilk koşuda iki test düştü ve ikisi de test
  kusuruydu, ürün kusuru değil. (1) `theNewestDocumentComesFirst` sonucu ham `Path` ile
  karşılaştırıyordu; model her zaman **mutlak** yolu sakladığı için bu yalnız POSIX'te
  geçiyordu (`/b.txt` zaten mutlak), Windows'ta `D:\b.txt` dönünce düştü - karşılaştırma
  `toAbsolutePath()`'e çevrildi. (2) `anUnusableRecentEntryIsIgnored` ayar dosyasını elle
  `recent.file.0=C:\Users\...` diye yazıyordu; `Properties.load` tanımadığı kaçışın
  ters bölüsünü **sessizce atar** (`\U` -> `U`, `\r` -> satır başı), yani iyi yol
  bozulmuş okunur, hiçbir dosyayı göstermez ve listeden süzülür - testin dışlamak istediği
  davranış. Dosya artık uygulamanın da kullandığı `Properties.store` ile yazılıyor. Aynı
  hata sınıfını pinlemek için `aPathContainingBackslashesSurvivesTheSettingsRoundTrip`
  eklendi (POSIX'e koşullu: Windows'ta ters bölü ayırıcıdır, dosya adı karakteri değil):
  adında ters bölü geçen bir belge ayarlardan bozulmadan geri geliyor. `DialogSizingTest` kart listesine
  `RecentFilesCard` eklendi: dördüncü yüzey de `OverlayCard`'ın ortak kaydırma kabını ve
  arka planını kullanır, kendi yerleşim kuralını kurmaz.

**W4-03 · A7b — Minimum pencere genişliği** · S · U8
- **Kapsam:** `MainStage.configure()` (geometri geri yükleme, `MainStage.java:44-62`),
  `ToolbarFit`, `README.md` + `README-TR.md`
- **Yapılacak:** `setMinWidth`; araç çubuğu kırpılması biter. Değer sihirli sayı olmaz:
  en geniş **compact** satırın ölçülen genişliğinden türetilir (`ToolbarFit` altı dilin
  tamamını ölçüyor; 7 Kasım ölçümünde en genişi ES).
- **Bitti tanımı:** Minimum genişlikte hiçbir araç çubuğu öğesi kırpılmıyor (`ToolbarFit`
  testiyle); kayıtlı geometri minimumdan darsa JavaFX kırpar, ayar sonraki kayıtta kendini
  onarır (test).
- **Kaynak referansı (7 Kasım 2026):** kartın verdiği `MainStage.java:53-54` eskimiştir;
  geometri geri yükleme `configure()` içinde `MainStage.java:44-62`'ye taşındı.
- **Doğrulama:** `mvn test -Dtest=ToolbarFitTest,MainStageTest`.

**W4-04 · A17 — Ses politikası** · S · U9
- **Kapsam:** `ui/component/EditorKeyHandler.java:41-52` (`handleKeyPressed`/`handleKeyTyped`),
  `audio/TypewriterSound.java:39,55` (sabit `volume`, `stop()+play()`), `AppSettings`
- **Durum notu (7 Kasım 2026):** Kartın "`MainMenu` (Görünüm → Ses)" maddesi **büyük ölçüde
  karşılanmıştır**: W3-02 takibiyle Görünüm → Ses artık bir değer alt menüsü (`ChoiceEntry`),
  yani aç/kapat satırı menüde var. Eksik olan **düzey** satırıdır; o yüzden bu kart yeni bir
  `EditorAction` değil, `MainMenu.Settings` portuna (`ChoiceKind`) bir değer kümesi ekler.
- **Yapılacak:** Ses yalnız **metin üreten** tuşlarda; ok/kısayol tuşları sessiz; ses düzeyi
  ayarı; hızlı yazımda `stop()+play()` yerine kesmesiz çalma.
- **Bitti tanımı:** (1) Ok tuşu ve kısayol ses çıkarmıyor (saf politika testi). (2) Düzey
  ayarı kalıcı ve 0'da tam sessiz. (3) 10 tuş/sn hızında ses kesilmiyor (elle, kayıtla).
- **Doğrulama:** `mvn test -Dtest=AudioAssetTest` + yeni `SoundPolicyTest`.

**W4-05 — `v1.1.0` yayını** · S · B2
- **Kapsam:** `pom.xml` (sürüm), `docs/release-notes-template.md` (erken erişim cümlesi),
  `.github/workflows/release.yml` (`--prerelease`), `ReleaseNotesTest` (iki pin), tag
- **Yapılacak:** W1-03'ün tekrarı; bu kez **pre-release işareti kaldırılır** — işaret `release.yml`'deki `gh release create ... --prerelease` satırıdır, kart o satırı kaldırır (`ReleaseNotesTest` bu satırı pinliyor; kart onu da güncellemeli).
  **Bayrak tek başına kaldırılamaz:** `docs/release-notes-template.md:5-7` iki yarıda da
  "Early access / pre-release" diyor ve `ReleaseNotesTest.theNoteMarksEarlyAccessAndMeasuresTheDownloadSizes`
  bu cümleyi **pinliyor**. Yalnız bayrağı silmek, W1-03'te düzeltilen kusuru **ters yönde**
  yeniden üretir: sayfa "Latest" der, gövdesi "pre-release" der. Üçü **tek commit'te** değişir:
  bayrak, şablon metni, iki test pinı.
- **Bitti tanımı:** Release'te 6 varlık; notlar iki dilli; `SHA256SUMS` doğrulanıyor; notta
  erken erişim işareti **yok** ve `ReleaseNotesTest` yeni sözleşmeyi (bayrak yok ↔ metin yok)
  iki yönde pinliyor; `pom.xml` sürümü tag ile aynı.
- **Doğrulama:** W1-03 ile aynı API sorgusu + `git tag -l | tail -1` = `v1.1.0` +
  `mvn test -Dtest=ReleaseNotesTest`.
- **Sürüm numarası — birinci düzeltme (7 Kas 2026):** Kart, "W2 işi `v1.1.0`'a gider" varsayımıyla
  yazılmıştı. Gerçek akış farklı oldu: W2 sertleştirmesi **`v1.0.1` ve `v1.0.2` patch'leri**
  olarak yayınlandı, çünkü W2'nin tamamı davranış düzeltmesiydi ve kapı G2 değişen dış sözleşme
  getirmedi.
- **Sürüm numarası — ikinci düzeltme (7 Kasım 2026, `2df2324`):** Bir önceki düzeltme `v1.1.0`'ın
  *minor* gerekçesini **W3-06 (arama)** diye yazmıştı; o gerekçe artık **geçersiz**, çünkü arama
  başka bir sürümde değil **`v1.0.3` patch'inde** yayınlandı (`v1.0.2..v1.0.3`, §1.4). Yani
  numara iki seçenekten birine bağlıdır ve **karar verilmeden kart yazılmamalıdır**:
  (a) **`v1.1.0`** — W4-02 (son dosyalar, karşılama opt-out) ve W4-04 (ses düzeyi + politika)
  yeni *kullanıcıya görünen yetenek* getirdiği için minor; (b) **`v1.0.4`** — W4 "davranış
  düzeltmesi + cila" sayılıyorsa patch. Semver'i belirleyen kart sayısı değil, **dış sözleşmede
  yeni yetenek olup olmadığıdır**; bu yüzden karar W4-02 ve W4-04 bitmeden verilemez.
  Yol haritasının geri kalanındaki `v1.1.0` adı bu karar verilene kadar **çalışma adıdır**.
- **Dört sürüm de pre-release (7 Kasım 2026):** `v1.0.0`, `v1.0.1`, `v1.0.2`, `v1.0.3` dördü de
  `prerelease=true` ve bu yüzden GitHub'da **"Latest release" etiketi yok** — sayfayı açan
  kişi yayında işe yarar bir sürüm görmüyor. W4-05 bu işareti kaldıran ilk sürüm olacak;
  patch'ler geriye dönük işaretlenmez (yayınlanmış bir artefaktın meta verisini sonradan
  değiştirmek "yayınlanan şey değişmez" ilkesini bozar).

> **Kapı G4:** Tam suite yeşil, `verify-artifacts` **20/20**, ikon `--check` 11 dosya,
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
| SPDX başlığı byte 0'da | `LicenseHeaderTest` | Yeni sınıflar (`FindModel`, `EditorAction`, `IoExecutor`, `TextChange`) |
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
| W1-04 | Taslak tarayıcı sertleştirme | D4, D5, D14 | M | — | ✅ | `mvn test -Dtest=RecoveryStoreTest,BackupKeeperTest` → 25/25 + 9/9; symlink taslağı okunmuyor, 100 MB taslak taramayı bloke etmiyor (sparse dosya, sınırlı okuma), `pending-*` 30 gün sonra siliniyor; dördü de mutasyonla kırmızıya döndü; `mvn -B clean test` → 384/384 |
| W2-01 | Artımlı istatistik | X5, X3 | M | W1-03 | ✅ | `mvn test -Dtest=SessionStatsTest` → 21/21 (her adımda eski tam taramayla birebir); `mvn test -Dtest=StatsPerfHarnessTest` → 10 MiB tek yenileme **0,116 ms** (CI eşiği 50, eski ölçüm 163); mutasyon: `getWordCount` eski gövdeye dönünce **123,2 ms** → kırmızı, delta penceresinden `+1` atılınca 3 property testi kırmızı; `getWordCount`/`getLineCount`/`getCharCount*` imzaları aynı, `StatsBarModel` değişmedi; `mvn -B clean test` → 396/396 |
| W2-02 | G/Ç executor | X1, X2 | M | W2-01 | ✅ | `mvn test -Dtest=FxThreadIoTest,FileServiceTest,AutoSaveServiceTest` → 5/5 + 36/36 + 15/15: kaydetme ve açma `IoExecutor` işçisinde, `fxThreadTasks()==0`; temiz otomatik kayıt tikinde metin kopyası yok; kaydetme sürerken yazılan metin kirli kalıyor. `AppGraph` paylaşılan `IoExecutor.background()` enjekte ediyor; `FxApplication.stop()` kapanışta işçiyi durduruyor; `savingStatus` altı bundle'da (`MessagesTest` 13/13); `mvn -B clean test` → 401/401 |
| W2-03 | Bellek/boyut/hata yolu | X4, M10, D7 | M | W2-02 | ✅ | `mvn test -Dtest=FileServiceTest,AtomicFilesTest,AutoSaveServiceTest,MessagesTest` → 38/38 + 7/7 + 17/17 + 13/13: sınır üstü dosya açılmıyor ve altı dilde açıklama gösteriliyor; `OutOfMemoryError` kaydetme hatasına düşüp taslak korunuyor; dolu disk (`usableSpace=0`) temp dosya oluşmadan `IOException` ile reddediliyor ve belge kirli kalıyor. `mvn -B clean test` → 406/406; `mvn -B test -Duser.language=de -Duser.country=DE` → 406/406 |
| W2-04 | Dış değişiklik | D1, M9 | M | W2-02 | ✅ | `mvn test -Dtest=FileServiceTest,DialogServiceTest,MessagesTest` → 47/47 + 9/9 + 13/13: yüklemeden sonra değişen/silinen dosya sessizce üzerine yazılmıyor, üç seçenekli diyalog (üzerine yaz / farklı kaydet / iptal) akışı testli; aynı boyut + aynı mtime değişikliği SHA-256 ile yakalanıyor; "Farklı Kaydet" iptalinde çıkış `STAY` kalıyor; senkron ve asenkron kaydetme yolları ayrı ayrı doğrulandı. `mvn -B clean test` → 415/415 |
| W2-05 | Symlink korunumu | D2 | S | W2-04 | ✅ | `mvn test -Dtest=AtomicFilesTest,BackupKeeperTest` → 8/8 + 10/10: symlink'li belge kaydedildikten sonra link korunuyor ve gerçek hedef güncelleniyor, `.bak` gerçek hedefin yanına düşüyor; `mvn -B clean test` → 417/417; `analysis.md` §15.4 manuel deneyi → `isSymbolicLink` true, gerçek içerik yeni |
| W2-06 | Taslak yolu doğrulama (D3) | D3 (D8 ürün kararıyla kapsam dışı) | M | W2-04 | ✅ | `mvn test -Dtest=RecoveryTargetPathTest,RecoveryStoreTest` → 10/10 + 27/27: mutlaklık, kanoniklik, izinli kök ve veri-dizini ret kuralları; kural tutmayan taslak hedefsiz açılıyor, metin korunuyor; kanonik biçim yalnız karar için (Windows 8.3 kısa adı ve symlink adı korunuyor); mutasyonla (accept→Path.of, toRealPath→toAbsolutePath, canonical→dönen değer) 4 test kırmızı; `mvn -B clean test` → 429/429. D8: `InstanceLock` aynı `user.home`'da ikinci süreci zaten durduruyor ve W2-04 çakışmayı soruyor → dosya başına `.jtwlock` eklenmedi, gerekçe kartta |
| W3-01 | `EditorAction` katmanı | M1, U13 | M | W1-03 | ✅ | `mvn test -Dtest=KeyboardShortcutHandlerTest,EditorActionTest,ShortcutRowsTest` → 15/15 + 6/6 + 7/7: akor tablosu, etki eşlemesi ve kısayol kartı tek `EditorAction` envanterinden üretiliyor; iki yönlü kapsam ve akor sahipliği testli; `EditorAction` dışındaki kaynak dosyalarda akor literali yok (kaynak taraması); araç çubuğu `actionRunner.accept(...)` üzerinden gidiyor. Mutasyonla (çift akor, yanlış etki, `ToolbarView`'a geri konan `"Ctrl+S"`) 3 test kırmızı. `mvn -B clean test` → 439/439 |
| W3-02 | Komut menüsü (araç çubuğunda `☰`) | E2 | M | W3-01 | ✅ | `mvn test -Dtest=MainControllerTest,ToolbarFitTest,MessagesTest` → 10/10 + 13/13 + 12/12: `MainMenu.layout()` dört menüde 14 platform-dışı `EditorAction`'ı tam bir kez içeriyor, altı dilde başlık/etiket dolu, Yardım komut taşımıyor (yalnız Hakkında dialogu); menü düğmesi `fitControls()`'un ilk kontrolü olduğu için genişlik modeli onu da ölçüyor (FR compact 817 px < 900). `Ctrl+T` araç çubuğuyla birlikte menüyü de gizliyor (ayrı krom satırı yok). Mutasyon: `SAVE_AS` layout'tan çıkarılınca kapsam testi kırmızı (14→13), menü satırı `fitControls()`'tan çıkarılınca ölçüm testi kırmızı (ikisi de geri alındı). `mvn -B clean test` → 443/443 |
| W3-03 | CLI argümanı | E3 | S | W3-01 | ✅ | `StartupArguments` + `FxApplication` bağlantısı: argümanlı başlatma belgeyi açıyor, geçersiz yol arayüz dilinde adıyla bildiriliyor, ikinci örnek devri yok; `mvn -B clean test` → 466/0/0, odaklı 34/0/0; paketli `smoke-appimage.sh --open-sample --skip-deb` → PASS (başlık `JTypeWriter - cli-sample.txt`); `verify-artifacts.sh --skip-build` → 20/20 |
| W3-04 | Sürükle-bırak | E4 | S | W3-01 | ✅ | `DroppedFiles` (saf karar) + `DropHandler` (sahne filtresi) + `FileService.openPathAsync`: `.txt`/`.md` bırakma Aç ile aynı SAVE/DISCARD/CANCEL matrisinden geçiyor, ret adıyla/adediyle bildiriliyor, dosya taşımayan jest düzenleyiciye bırakılıyor; `mvn -B clean test` → 488/0/0, odaklı DroppedFiles+DropContract+FileService+Messages 13/5/50/13, `ExitDispositionTest,MainStageTest` yeşil |
| W3-05 | `FindModel` | E1 | M | W2-01, W3-01 | ✅ | `ui/FindModel` (saf): düz metin sorgu/değiştirme (regex yok), çakışmasız soldan sağa tarama, varsayılan yerel ayara bağlı olmayan büyük-küçük harf (`regionMatches`), iki yönde saran ileri/geri, değiştirme sonrası yeniden tarama, sığmayan aralığı reddetme; `findAll` saf statik (W3-06 paneli FX thread'inde taramayacak) ve `install` bayat taramayı atıyor; `mvn test -Dtest=FindModelTest` → 30/30, `mvn -B clean test` → 518/0/0, dört mutasyon kırmızı |
| W3-06 | Arama paneli | E1 | M | W3-05 | ✅ | `ui/FindPanel` (Notepad++ tarzı yüzen kart) + `FindScanService` (FX dışı tarama) + `FindReplacePolicy` (YOLO onayı) + `FindCommands` portu; `Ctrl+F`/`F3`/`Shift+F3`/`Ctrl+H` + araç çubuğunda `⚲` + Düzen menüsü; tarama worker'da, 10 MiB'da 26–53 ms ve `fxThreadScans()==0`; tümünü değiştir tek geri alma adımı; YOLO açıkken onay diyaloğu; `mvn -B clean test` → 524/0/0 |
| W4-01 | Ayar şema sürümü | D13, D12 | S | — | ✅ | `AppSettings`: `schema.version` = **ürün sürümü** (`pom.xml`, tek kaynak; filtrelenmiş `version.properties` ile taşınıyor), anahtarsız/bozuk = sürümsüz → bellekte damgalanır, daha yeni sürüm ve bilinmeyen anahtarlar kayıtta korunuyor, karşılaştırma sayısal parçalarla; `save()` `SecurityException`'ı da yakalıyor, `isSettingsWriteFailurePending()` bayrağı `AppGraph` tarafından açılış sonunda ve her kayıtta bir kez gösteriliyor; About artık "dev" değil sürümü gösteriyor; 6 bundle + `Messages`; `mvn test -Dtest=AppSettingsTest,AppVersionTest,MessagesTest,DialogServiceTest` → yeşil |
| W4-02 | Son dosyalar + opt-out | E5, U7 | M | W4-01, W3-02 | ✅ | `RecentFiles` saf model (en yeni başta, mutlak+normalize kimlikle tekilleştirme, `MAX = 10`, kuyruktan düşme); `AppSettings.getRecentFiles`/`writeRecentFiles` (`recent.file.N`), `rememberFile` listeyi aynı kayıtta güncelliyor, silinmiş belge sessizce düşüyor; son dosyalar **ayrı kartta** (`RecentFilesCard`, `Dosya → Son Dosyalar`) ve `welcome.enabled` opt-out (`WelcomeCard` onay kutusu) + `Dosya → Karşılama Kartını Göster`; satırlar `FileService.openPathAsync` ile aynı kaydedilmemiş-değişiklik kapısından; 6 bundle × 155 + 5 `Messages`; `mvn test -Dtest=RecentFilesTest,RecentFilesCardTest,AppSettingsTest,StartupGreetingTest,MessagesTest,MainControllerTest` → yeşil |
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
mvn test -Dtest='SessionStatsTest,StatsPerfHarnessTest,FileServiceTest,AtomicFilesTest,RecoveryStoreTest,AutoSaveServiceTest'
bash scripts/verify-artifacts.sh --skip-build     # 20/20 (2 Kasım'da 17'ydi; gömülü font 3 karşılaştırma ekledi)
```
+ `StatsPerfHarnessTest` 10 MB çıktısı: **referansta < 5 ms**, CI'da < 50 ms. 7 Kasım koşusunda
  **0,133 ms** — eşik regresyonu yakalamak için var, hedef değil (§1.4).
  *(7 Kasım düzeltmesi: bu satır bir süre `-Dtest=StatsPerfHarness` yazıyordu; Surefire o ada
  sınıf eşlemediği için komut **hiçbir test koşmadan** `BUILD FAILURE` veriyor ya da
  `failIfNoSpecifiedTests=false` ile sessizce boş geçiyordu. Gerçek sınıf adı
  `StatsPerfHarnessTest`.)*

### G3 — Editör · W3 sonu

```bash
mvn test -Dtest='FindModelTest,KeyboardShortcutHandlerTest,ShortcutRowsTest,MainControllerTest,ToolbarFitTest,MainStageTest,DialogSizingTest,MessagesTest'
```
+ elle: `Ctrl+F` → bul; `F3`/`Shift+F3` → gez; `Ctrl+H` → bir/tümünü değiştir; aynı işlevler
araç çubuğundaki `☰` komut menüsünde; 10 MB belgede arama süresi kaydedildi.

### G4 — Sürüm · W4 sonu

```bash
mvn -B clean test
bash scripts/verify-artifacts.sh --skip-build     # 20/20
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
