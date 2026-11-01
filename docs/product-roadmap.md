# JTypeWriter — Ürün Yol Haritası (Pareto Sıralı)

> **Sürüm:** 1.0 · **Tarih:** 31 Ekim 2026 · **Taban:** `docs/analysis.md` v3.1 (`main` @ `95b386e`)
> **Kapsam:** P2'nin iş sırası, sahipliği, kapıları ve takvimi.
> **Yöntem:** Sıralama, analizdeki `(Etki × Güven) / Efor` skoruna **Pareto kesme çizgisi** ve
> **bağımlılık kısıtları** uygulanarak türetilmiştir. Her ölçüt ya taban belgedeki bir bulgu
> kimliğine ya da burada açıkça yazılmış bir varsayıma bağlanmıştır; varsayımlar "çıkarıldı"
> olarak işaretlenmiştir.
> **Atıf kuralı:** yalın `§N` bu belgenin bölümüne, `analysis.md §N` taban belgeye işaret eder.
> **Bu belge bir istek listesi değildir.** Hangi işin **yapılmayacağını** ve neden
> yapılmayacağını da tanımlar (§7, §11).

---

## 1. Yönetici Özeti

### 1.1 Tek cümle

**Altı iş, eforun ~%57'si, beş kapının beşi:** erken ama gerçek bir sürüm yayınla (A6), komut
yüzeyini kur (A4), G/Ç'yi FX thread'inden çıkar (A3), belgeye kimlik ver (A1), ara/değiştir'i
ekle (A2), diyalogları yerelleştir (A5) — bu sırayla, kapasite boşluğunda üç binici ile.

### 1.2 Pareto'nun bu repoda nasıl uygulandığı

Pareto ilkesi burada üç ayrı karara dönüşür; üçü de ölçülebilir:

| # | Pareto kuralı | Bu yol haritasındaki karşılığı |
|---|---|---|
| 1 | **Hayati azınlık** — kazancın çoğunu veren az sayıda iş | 12 aday işten **6'sı** (A6, A4, A3, A1, A2, A5) beş çıkış kapısının **beşini** kapatır; kalan 6 iş **hiçbir kapıyı** kapatmaz (§4.1–4.2) |
| 2 | **Kuyruğu kes, sıralamakla uğraşma** — değerin %80'ini tüketen %80 | A9, A11, A12 ve tüm P3 listesi **bilinçli olarak yapılmıyor** (§11); gerekçe "düşük değer" değil, "kapıyı geciktiriyor" |
| 3 | **Kesme noktası, sıralamadan önemli** | 6 kapı işi = 14 puan = eforun **%57'si**; A7/A8/A10 bu işlerin yanına **binicidir** (5 puan); A9/A11/A12 **kesilir**. P2, 19,5 puanda biter (§4.3) |

**Dürüstlük notu:** Bu repoda **katı 20/80 tutmuyor** ve tutması da beklenmemeli. P1, "ucuz
kazançları" (veri güvenliği sınıfı, kodlama, çıkış matrisi) zaten toplamış durumda
(`analysis.md` §2.2, §5.1). Geriye kalan her iş **kesişen** bir iştir: 6 bundle, test, paket
ve belge aynı anda dokunur. Bu yüzden doğru ifade *"işin %20'si kazancın %80'i"* değil,
**"işlerin yarısı, eforun %57'si, beş kapının beşi"**dir. Pareto'nun buradaki değeri sıralama
değil, **kesme cesaretidir**: 12 adaydan 6'sını seçip kalanı gerekçesiyle kesmek.

### 1.3 Sıralamanın tek cümlelik gerekçesi

> **Önce yayınla, çünkü hiç yayınlanmamış bir ürünün diğer bütün işleri tahmindir; sonra
> editörü kur, çünkü "metin alanı" ile "editör" arasındaki fark orada; sonra güvenceyi
> koşulsuz yap, çünkü ürünün tek savunulabilir farklılaştırıcısı orada.**

---

## 2. Ölçüm Tabanı ve Hedef

`analysis.md` §11'deki beş boyut, burada bir **ağırlıklı olgunluk endeksi**ne çevrilir.
Ağırlıklar, ürün tezinin sırasına göre seçildi: motor ve deneyim ürünün kendisi, ambalaj
"çalıştırabilir misin", güven ve erişim "bulup güvenebilir misin".

| Boyut | Ağırlık | Bugün | P2 çıkışı | Gerekçe |
|---|---:|---:|---:|---|
| Motor | 0,25 | 3,6 | 4,4 | Farklılaştırıcı: silme engeli + kaybetmeme |
| Deneyim | 0,25 | 2,2 | 3,6 | Günlük döngünün yarısı yok (`analysis.md` §6.2) |
| Ambalaj | 0,20 | 2,0 | 3,2 | İndirilen ikili açılmıyor/denenmemiş |
| Güven | 0,15 | 2,8 | 3,4 | Altyapı gerçek, kanıt yok |
| Pazara erişim | 0,15 | 1,5 | 3,1 | Yayın, CLI, OS entegrasyonu yok |
| **Endeks** | 1,00 | **2,50** | **3,60** | +1,10 |

Endeks hesabı: `0,25·3,6 + 0,25·2,2 + 0,20·2,0 + 0,15·2,8 + 0,15·1,5 = 2,50`.

**Hedef seçimi:** 3,60 "olgun ürün" değil, **"iddia edilebilir ürün"** eşiğidir: bir romancı
bir bölümü bitirebilir (`analysis.md` §6.2'deki 9 adımın 8'i) ve indirdiği ikili açılır. 4,0+
hedefi P3'e aittir ve bu belgede bilinçli olarak yoktur — aksi halde §11 ihlal edilir.

---

## 3. Sıralama Yöntemi (yanlışlanabilir)

### 3.1 Girdiler

Analizin skoru **göreli öncelik** göstergesidir, sıralama değil (`analysis.md` §12.2). Bu
belge skoru üç kısıtla düzeltir:

1. **Bağımlılık** — bir iş, önkoşulu bitmeden başlayamaz (§3.3).
2. **Boyut yayılımı** — aynı skora sahip iki işten **daha çok boyuta dokunan** önce gelir;
   olgunluk endeksi boyutların ortalamasıdır, tek boyutu tavana vurmak endeksi taşımaz.
3. **Bilgi değeri** — efor/ölçüm oranı değil, **belirsizliği azaltma** değeri. Bu yüzden A6
   (yayın) skoru en yüksek olmasa da ilk sıradadır: yayınlanana kadar diğer her şey tahmindir
   (`analysis.md` §12.4).

### 3.2 Düzeltilmiş sıra

| Sıra | İş | Skor (§12.2) | Etki alanı (boyut) | Efor | Bağımlılık | Neden bu sırada |
|---:|---|---:|---|---:|---|---|
| 1 | **A6** Yayın + CI artefakt smoke | 7,5 | Ambalaj, Erişim, Güven | 2 | — | Tek başına **3 boyutu** hareket ettiren tek iş; diğer işlerin doğrulama zemini |
| 2 | **A4** Komut katmanı + menü + CLI + D&D | 4,0 | Deneyim, Erişim | 2 | A6 (yayın hattı hazır olmalı) | A2'nin önkoşulu; kurulmazsa arama iki kez kablolanır (`analysis.md` §12.2 not 1) |
| 3 | **A3** G/Ç'yi FX thread'inden çıkar | 4,0 | Deneyim, Motor | 2 | A6 | A2'nin önkoşulu; büyük belgede arama UI'yı kilitlememeli (`analysis.md` §12.2 not 3) |
| 4 | **A1** Belge kimliği | 3,3 | Motor, Güven | 3 | A6; A3 ile aynı dosyalara dokunmaz | En düşük skorlu çekirdek iş, ama ürün tezini **koşullu olmaktan çıkaran tek iş**; A4/A3 ile paralel koşabilir |
| 5 | **A2** Ara / Değiştir | 5,0 | Deneyim, Motor | 3 | **A4 + A3** | Skoru en yüksek ikinci iş; ama önkoşulsuz yapılırsa iki kez yazılır. Beklemesi ucuz, yanlış yapılması pahalı |
| 6 | **A5** Diyalog taşması + yerel biçim | 4,5 | Deneyim | 2 | — | G3'ün (yerel ayar sığması) tek sahibi; kapsamı kesişmez (DialogService/CSS/Messages) → her dalgaya paralel koşar |
| — | **A7, A8, A10** biniciler | 4,5 / 4,5 / 6,0 | Deneyim, Güven, Ambalaj | 2 / 2 / 1 | — | Hiçbir kapıyı kapatmaz; kapı işinin dokunduğu dosyalara dokunmadıkları için **boş kapasiteye** binerler, takvimi uzatmazlar |

### 3.3 Bağımlılık grafiği

```text
W0   B13 ──► G0
W1   A6 ──► G1        (+ A10 binici)
W2   A1 ──┐
     A3 ──┤──► G2     (+ A5: G3/2–3 hazırlanır)
     A5 ──┘
W3   A4 ──► A2 ──► G3   (+ A8 binici)
     A5 ──► (G3/2–3 tamamlanır)
W4   A7 ──► v1.1.0 ──► G4
```

Sert kısıtlar: **A2, A4 ve A3'ten sonra gelir** (menü/arama aynı komut katmanı; büyük belgede
arama UI'yı kilitlemez). Yumuşak kısıt: A1 → A3 (A1 `FileService` sözleşmesini tanımlar, A3
onu async'e taşır).

Kritik yol: **A6 → A4 → A2** (7 puan). A1 ve A3 kritik yolda değildir; W2'de paralel
koşarlar. Bu, tek geliştirici varsayımında bile takvimi kısaltan tek manevradır.

---

## 4. Pareto Kümesi ve Kesme Noktası

### 4.1 Hayati azınlık — beş kapıyı kapatan altı iş

| İş | Hangi vaadi koşulsuz yapar | Kapattığı bulgular | Kapattığı çıkış kriteri (`analysis.md` §12.6) |
|---|---|---|---|
| A6 | "İndirdiğin ikili gerçekten açılır" + "sürüm var" | B1, B2 | **5** — sürüm var ve çalışıyor |
| A4 | "Editörü klavyeden değil, gözle de keşfedebilirsin" | E2, E3, E4, M1 | **2** (menü yarısı) |
| A3 | "Yazarken donmaz" (1 MB kayıtta duraklama, 10 MB'da donma) | X1, X2, X3, X4 | **4** — FX thread'inde G/Ç yok |
| A1 | "Dosyanı başkası değiştirdiyse sessizce ezmezsin" | D1, D2, D3, D8 | **1** — üzerine yazma güvenliği |
| A2 | "Uzun metinde cümle bulabilirsin" | E1 | **2** (arama yarısı) |
| A5 | "Almanca/Fransızca metin ekrana sığar, tarih kendi dilinde" | U1, U5 | **3** — yerel ayar sığması |

### 4.2 Kuyruk — hiçbir kapıyı kapatmayan işler

| İş | Neden yine de var | Neden kapıyı kapatmıyor |
|---|---|---|
| **A7** (W4) | Günlük sürtünmeyi azaltır; G3 sonrası boş kapasiteye biner | Son dosyalar, karşılama opt-out'u, min genişlik bir kapı kriteri değil |
| **A10** (W1) | En iyi skor/efor oranı (6,0); JAR'ı küçültür | Ambalaj boyutu bir kapı kriteri değil |
| **A8** (W2–W3) | D5 gizlilik açığını kapatır; izole kapsam (RecoveryStore) | Taslak yaşam döngüsü §12.6'da yok |
| **A9** (§11.1) | Test hijyeni gerçek bir kural ihlali (`AGENTS.md`) | Katı bir kapı kriteri değil → **kesildi** |
| **A11, A12** (§11.1) | macOS/çıktı hattı | §9 kararına ve kullanıcı kanıtına bağlı → **kesildi** |

### 4.3 Kesme noktası

| Küme | İş sayısı | Efor | Efor payı | Kapatılan kapı |
|---|---:|---:|---:|---:|
| Kapı işleri (A6, A4, A3, A1, A2, A5) | **6 / 12** | 14 | **%57** | **5 / 5** |
| Biniciler (A7, A8, A10) | 3 | 5 | %20 | 0 |
| W0 ön koşulları | 2 kart | 0,5 | %2 | 0 (kapıları mümkün kılar) |
| **P2 toplamı** | **9 / 12** | **19,5** | **%80** | **5 / 5** |
| Kesilen (A9, A11, A12) | 3 | 5 | %20 | 0 |

**Okunuşu:** İşlerin yarısı (6/12), eforun %57'si, **beş kapının beşini** kapatır. Geriye kalan
her şey — biniciler dahil — tek bir ölçülebilir çıkış kriterini bile açmaz. Yol haritasının en
önemli sayısı: **P2, 19,5 puanda biter; son 4,5 puan bilinçli harcanmaz.**

Binicilerin varlık sebebi takvim değil, **dolgu**: kapı işinin dokunmadığı dosyalara
dokundukları için (A10 → ses/JAR, A8 → RecoveryStore, A7 → Welcome/MainStage) bir darboğaz
oluştuğunda kapasiteyi kullanırlar. Darboğaz yoksa ilk kesilen onlardır (§6 son paragraf).

Bu, "iyi"den "rahat"a geçişi bilinçli olarak gerçek kullanıcıya bırakır — §11'deki yeniden
giriş koşulları tam olarak bunun içindir.

---

## 5. Dalgalar

Her dalga **tek bir çıkış kapısı** ve **tek bir ölçülebilir kanıt** ile biter. Kapı geçilmeden
sonraki dalga başlamaz; bu, P1'in kısmi kapanışının (§2.2) tekrarını önleyen kuraldır.

### W0 — Ön koşullar · 0,5 puan · ~2 saat

| Kart | İş | Kanıt |
|---|---|---|
| W0.1 | `AGENTS.md`'yi sürüm kontrolüne al (`.gitignore:57`) | `git ls-files AGENTS.md` dolu; PR'da görünür |
| W0.2 | Taze build yeniden üretim betiğini yaz (`scripts/verify-artifacts.sh`) | İmaj ↔ `.deb` ↔ repo payload'ı bayt karşılaştırması tek komut |

**Kapı G0:** `mvn clean test` yeşil + `git ls-files AGENTS.md` dolu.
**Neden burada:** B13 bir bulgu ama bir *saatlik* iş; ertelenmesi P1'de bir dönem maliyete
dönüştü (`analysis.md` §2.3).

### W1 — "Yayınlanabilir" · 3 puan · ~1 hafta

| Kart | İş | Kapattığı | Kanıt (kapı) |
|---|---|---|---|
| W1.1 | CI artefakt smoke testi: Linux'ta app-image'ı boş `user.home` + Xvfb ile başlat, ana pencereyi bekle, kapat; `.deb` kur → çalıştır → kaldır | B1 | Workflow'da yeni `smoke` işi yeşil; `upload-artifact` adımlarında `retention-days` ≤ 7 |
| W1.2 | `release.yml`: `gh release create --prerelease` desteği + SHA-256 checksum dosyası | B2 | Tag'sız koşuda `prepare` hâlâ reddediyor; checksum dosyası Release'te |
| W1.3 | İlk yayın: `v1.0.0` (GitHub **pre-release**, "Erken erişim" notuyla) | B2 | Release URL'i; 6 artefakt + kaynak arşivi |
| W1.4 | **A10** — sesi `javax.sound.sampled`'a taşı, JavaFX Media + FXML'i fat JAR'dan çıkar | B4 | JAR boyutu düşer; `THIRD-PARTY-NOTICES` güncel ve taze build'le bayt-özdeş |

**Sürüm notu (taban belgedeki çelişki):** `analysis.md` §12.6/5 "`v0.x` yayınlanmıştır" der,
ama `scripts/package.sh:79-83` ve `release.yml:55` **0.x'i reddeder** (jpackage/macOS kısıtı).
Bu yüzden ilk yayın **v1.0.0'dır** ve "1.0 kalitesi" iddiası **pre-release işareti + dürüst
sürüm notu** ile taşınır. Alternatif (pom'u 0.9 yapmak) yayın hattını kırar — yapılmaz.

**Kapı G1:** GitHub'da bir Release var; CI o Release'in imajını **açtı** ve `.deb`'i **kurup
kaldırdı**; ölçülen artefakt boyutları Release notunda yazılı.
**Neden ilk:** 7,5 skor + 3 boyut + diğer tüm işlerin doğrulama zemini. Ayrıca `analysis.md` §12.4'teki en
büyük riski (bir yıl daha mükemmelleştirip hiç yayınlamamak) **bir haftada** ortadan kaldıran
tek hamle.

### W2 — "Güven koşulsuz" · 7 puan (A1 3 · A3 2 · A5 2) · ~2 hafta (üç paralel hat)

| Hat | Kart | İş | Kapattığı |
|---|---|---|---|
| A | W2.1 | **A1** — yüklemede `(mtime, boyut, içerik hash'i)` sakla; kaydetmeden önce karşılaştır; çelişkide **sor** (üzerine yaz / yeni ada kaydet / vazgeç) | D1 |
| A | W2.2 | **A1** — `toRealPath()` ile symlink hedefe yaz; `.bak` gerçek hedefin yanına | D2 |
| A | W2.3 | **A1** — taslak başlığındaki yolu doğrula (mutlaklık + kanoniklik + izinli kök); geçersizse kurtarma **hedefsiz** açılır (Farklı Kaydet'e zorlar) | D3 |
| A | W2.4 | **A1** — belge düzeyi `FileLock`; kilit `UNAVAILABLE` ise uyarı | D8 |
| B | W2.5 | **A3** — `ExecutorService` + `Task`: açma, kaydetme, otomatik kayıt FX thread'i dışında; UI'da "kaydediliyor" durumu; 1 Hz istatistiğin O(n) taraması artımlı sayaçla değiştirilir | X1, X3 |
| B | W2.6 | **A3** — otomatik kayıtta `getText()`'i **kirli kontrolünden sonra** al | X2 |
| B | W2.7 | **A3** — boyut sınırı + ilerleme; `readAllBytes` yerine akış; `Error`'ı da kapsayan kaydetme hata yolu | X4 |
| C | W2.8 | **A5** — 9 `Alert` için `DialogSizing` + sarma + yeniden boyutlandırma (G3/2'nin motoru) | U1 |

**A9 neden yok:** testlerin gerçek `~/.jtypewriter`'a yazması (M4, T6) bir kural ihlali ama bir
kapı kriteri değil; A3'ün testleri zaten `-Duser.home=<temp>` ile koşacak (geçici çözüm). Kalıcı
enjeksiyon seam'i §11.1'de, tetikleyicisiyle bekliyor.

**Kapı G2** (yanlışlanabilir, `analysis.md` §12.6/1 ve /4):
1. Yüklemeden sonra diskte değişen dosyaya kaydetmek **ya sorar ya reddeder**; symlink'li belge
   kaydedildikten sonra **hâlâ symlink**; geçersiz taslak yolu hedef olarak **kabul edilmez**. Testli.
2. 2 MB'lık belge açılır, kaydedilir, otomatik kaydedilirken **FX thread'inde dosya G/Ç'si veya
   `fsync` yok** (test: G/Ç çağrılarını saran sayaç + FX thread kimliği kontrolü).

**Not:** A1 ve A3 **farklı dosyalara** dokunur (`FileService`/`AtomicFiles`/`RecoveryStore` ↔
`AutoSaveService`/yeni `IoExecutor`), ama ikisi de `FileService`'in sözleşmesini değiştirir.
Tek geliştirici varsayımında sıra: **A1 → A3** (A1 sözleşmeyi tanımlar, A3 onu async'e taşır).

### W3 — "Editör" · 7 puan (A4 2 · A2 3 · A8 2) · ~2,5 hafta

| Kart | İş | Kapattığı | Not |
|---|---|---|---|
| W3.1 | **A4** — `EditorAction` komut katmanı: kimlik + etiket anahtarı + hızlandırıcı + çalıştırma; mevcut 16 kısayol ve 5 araç çubuğu düğmesi buradan besleniyor | M1 | `KeyboardShortcutHandler` + `ToolbarView` + `MainController` üçlüsü tek yere iner |
| W3.2 | **A4** — menü çubuğu (Dosya/Düzen/Görünüm/Yardım), `Yeni` + `Farklı Kaydet` görünür | E2 | macOS'ta `SystemMenuBar` koşullu (§9 kararına bağlı) |
| W3.3 | **A4** — CLI/süreç argümanıyla açma; çift tıklama aynı yolu kullanır | E3 | İkinci örnek: mevcut kilit yolu **devredemiyor** → W3'te "reddet" davranışı korunur, devir P3 |
| W3.4 | **A4** — sürükle-bırak (`Dragboard`, `.txt`/`.md`), kirli belgede mevcut çıkış matrisinden geçer | E4 | Yeni bir kaydetme yolu **icat edilmez** |
| W3.5 | **A2** — saf `FindModel` (eşleşme listesi, ileri/geri, büyük/küçük harf, tümünü değiştir) — JavaFX'siz, testli | E1 | Toolkit'siz test kuralı korunur (`analysis.md` §10.3) |
| W3.6 | **A2** — `Ctrl+F` / `F3` / `Shift+F3` / `Ctrl+H` paneli; **YOLO açıkken "tümünü değiştir" onay ister** | E1 | YOLO ile etkileşim ayrı tasarlanmalı (`analysis.md` §12.2) |
| binici | W3.7 | **A8** — taslak taraması: symlink takibi yok, boyut/sayı sınırı, N taslağı birlikte sun | D4 |
| binici | W3.8 | **A8** — `pending-*.jtw` yaş/sayı temizliği; `ATOMIC_MOVE` fallback'te bayt doğrulama; temp süpürme; `Error` maskelemesi | D5, D6, D9, D10, D11 |
| kapı | W3.9 | **A5** — tarih/saat biçimleri UI diline (`DateTimeFormatter.ofLocalized…`), `" - "`/`": "` ayırıcıları bundle'a | U5, U12 |

**Kapı G3** (`analysis.md` §12.6/2 ve /3):
1. `Ctrl+F` bulur, `F3`/`Shift+F3` gezer, `Ctrl+H` bir/tümünü değiştirir; **aynı işlevler menüden
   de erişilebilir**; `FindModel` toolkit'siz testli; menüdeki her eylem komut katmanından gelir.
2. En uzun DE/FR metinleriyle **her** `Alert` 768 px'e sığar; tüm tarih/saat gösterimi UI dilini
   kullanır.
3. 6 bundle anahtar paritesi + `Messages` erişimcisi korunur (yeni anahtarlar 6 dosyada).

### W4 — "Cila ve sürüm" · 2 puan · ~1 hafta

> `v1.1.0` yayını (W4.5) **A6 kapsamındadır**, ek puan saymaz: hat W1'de kuruldu, burada yalnız
> pre-release işareti kaldırılır.

| Kart | İş | Kapattığı |
|---|---|---|
| W4.1 | **A7** — son dosyalar listesi (en çok 10, silinmişse sessizce düş) | E5 |
| W4.2 | **A7** — karşılama kartında "bir daha gösterme" + ayar | U7 |
| W4.3 | **A7** — minimum pencere genişliği; araç çubuğu kırpılması biter | U8 |
| W4.4 | **A7** — klavyeyle yazı boyutu (`Ctrl+=` / `Ctrl+-`), fare çift tıklaması yanında | U2 |
| W4.5 | **A6** — `v1.1.0` yayını (pre-release işareti **kaldırılır**) | B2 |

**Kapı G4:** `v1.1.0` yayınlandı, pre-release değil; G1–G3 kapılarının testleri hâlâ yeşil;
olgunluk endeksi yeniden ölçüldü ve **≥ 3,55**.

---

## 6. Takvim ve Efor Modeli

| Varsayım | Değer | Not |
|---|---|---|
| Efor birimi | **1 puan = 1 net geliştirme günü** | Analizdeki S/M/L/XL = 1/2/3/5 ile aynı ölçek |
| Kapasite | 3 net gün/hafta | Tek geliştirici; kalan 2 gün bakım, inceleme, sürüm işi |
| Belirsizlik | ±%50 | A1/A2 gibi L işlerde üst sınır gerçekçi |
| Toplam (P2) | **19,5 puan ≈ 6,5 net hafta ≈ 8 takvim haftası** | |
| İlk yayın | **W1 sonu: ~1 hafta** | Yol haritasının en önemli tarihi |

Dalga süreleri (3 net gün/hafta): W0 ~2 saat · W1 ~1 hafta · W2 ~2 hafta · W3 ~2,5 hafta ·
W4 ~1 hafta.

**Takvim baskısı altında kesme sırası (sondan başa):** W4.4 → W4.3 → W4.1 → W4.2 → W3.8 →
W3.7 → (gerekirse) `v1.1.0` işareti W4'ten W3 sonuna alınır. **Kesilmeyenler:** W1'in tamamı,
A1, A3, A4, A2, A5/U1. Bunlar kapı işleridir; kesilirse yol haritası P2 olmaktan çıkar.

---

## 7. Her Kartın Tanımı (Definition of Done)

Bu repoda "bitti" tanımı dilek değil, testle korunan kuraldır (`AGENTS.md`). Her kart şunları
**aynı PR'da** taşır:

1. **SPDX bloğu** dosyanın 0. baytında (`LicenseHeaderTest`).
2. **6 bundle × anahtar paritesi** + `Messages` erişimcisi; ölü anahtar yok (`MessagesTest`).
3. **Saf model + ince UI:** karar/geometri/ayrıştırma JavaFX'siz statik metot veya model
   nesnesinde; test toolkit açmadan koşar. `Alert` yalnız `DialogSizing` üzerinden.
4. **Bulgu testi:** kartın kapattığı her bulgu için, o bulgunun **yeniden üretilebilir**
   senaryosunu sabitleyen bir test (ör. symlink hedefi, dış değişiklik, 768 px sığma).
5. **Kullanıcıya görünen değişiklikte** `README.md` + `README-TR.md` birlikte.
6. **Bağımlılık değiştiyse** `THIRD-PARTY-NOTICES` güncel + taze build sonrası test.
7. `mvn clean test` + `tr-TR`/`de-DE` koşuları yeşil.
8. `AGENTS.md`'deki değişmezler değiştiyse `AGENTS.md` güncel (W0'dan sonra PR'da görünür).

**Yasaklar (kartı reddettiren):** JavaFX toolkit açan test; yeni bir yazma yolu (`AtomicFiles`
dışında); `AppGraph` dışında bileşen kurmak; sabit `dd.MM.yyyy`/`Ctrl+` metni; kelepçesiz yeni
`Alert`.

---

## 8. Kapıların Özeti

| Kapı | Ne zaman | Tek cümlelik kanıt | Yanlışlanma biçimi |
|---|---|---|---|
| G0 | W0 sonu | `mvn clean test` yeşil + `AGENTS.md` izleniyor | `git ls-files` boşsa |
| G1 | W1 sonu | Yayınlanmış Release + CI'da açılmış imaj + kurulmuş `.deb` | Release yoksa veya smoke işi kırmızıysa |
| G2 | W2 sonu | Dış değişiklik sorar; symlink korunur; FX thread'inde G/Ç yok | Test senaryosu üretilemiyorsa |
| G3 | W3 sonu | `Ctrl+F` bulur; menüden de bulunur; her `Alert` 768 px'e sığar | Menüde eksik eylem varsa |
| G4 | W4 sonu | `v1.1.0` yayınlandı, endeks ≥ 3,55 | Sürüm pre-release ise |

---

## 9. Açık Karar (yol haritasını dallandıran tek soru)

`analysis.md` §12.5: **İlk benimseyen kullanıcı kim ve hangi işletim sisteminde?**

Bu belge **Linux x64** varsayımıyla yazıldı — gerekçe: ölçülen tek artefakt kümesi Linux
(`analysis.md` §9.1), `.deb` hattı var, ikincil persona (§3.2) Linux kullanıcısı, imza bütçesi
gerekmiyor. Karar farklı çıkarsa değişenler:

| Karar | W1'e eklenen | P2'ye eklenen | Takvim etkisi |
|---|---|---|---|
| **Linux x64** (varsayılan) | — | — | 0 |
| + Windows x64 | MSI smoke (sessiz kur/kaldır) | SmartScreen uyarısını release notuna yaz | +2 gün |
| + macOS (arm64) | DMG smoke (`.app` aç, `Cmd+Q` → kaydetme akışı) | **A11** P2'ye girer: `QuitHandler` + `SystemMenuBar` doğrulaması; imza/notarization bütçesi | +1,5–2 hafta |

**Bu karar W1 başlamadan verilmeli.** Verilmezse varsayılan (Linux) uygulanır ve macOS/Windows
P3'e düşer; sonradan eklemek W1'i yeniden açmak demektir.

---

## 10. Risk Kaydı

| # | Risk | Olasılık | Etki | Azaltma |
|---|---|---:|---:|---|
| R1 | **Bir yıl daha mükemmelleştirip hiç yayınlamamak** (analizin en büyük riski, §12.4) | Orta | Yüksek | W1 **ilk** dalga ve **1 hafta** ile sınırlı; v1.0.0 pre-release çıkmadan hiçbir özellik işi başlamaz |
| R2 | A2, A4'ün komut katmanını beklemeden yazılır → arama iki kez kablolanır | Orta | Orta | Sıra kısıtı §3.3'te sertleştirildi; W3.1 bitmeden W3.5 başlamaz |
| R3 | A3 eşzamanlılık hatası veri güvenliğini bozar (P1'in kapattığı sınıf yeniden açılır) | Orta | **Yüksek** | A3'ün her kartı mevcut `ExitDisposition`/`AutosaveHealth` matrisini korur; `ProcessCrashRecoveryTest` her A3 PR'ında koşar |
| R4 | A1 yanlış pozitif verir (mtime çözünürlüğü, senkron istemcisi) → her kayıtta uyarı | Orta | Orta | Hash karşılaştırması ikincil; uyarı yalnız **içerik** değiştiyse; "yeni ada kaydet" varsayılan güvenli seçenek |
| R5 | 6 bundle × 90 anahtar borcu büyür, çeviri kalitesi düşer | Yüksek | Düşük | `MessagesTest` zaten zorluyor; yeni anahtarlar tek PR'da 6 dosyada |
| R6 | Yayın hattı ilk kez koşarken sürpriz (B2'nin asıl riski) | **Yüksek** | Orta | W1.2 önce `--prerelease` + checksum ile **kuru koşu**; G1 pre-release'e izin verir |

---

## 11. Bilinçli Olarak Yapılmayanlar

Pareto'nun ikinci yarısı: **kuyruğu kesmek.** Aşağıdakiler "sonra" için değil, **koşul**
belirtilerek ertelenmiştir. Koşul gerçekleşirse yol haritası yeniden planlanır — bu, takvimi
kaydırmaz, sırayı değiştirir.

### 11.1 P3'e ertelenen işler ve yeniden giriş koşulları

| İş | Neden şimdi değil | Yeniden giriş koşulu |
|---|---|---|
| **A9** Test hijyeni (`AppLogging` enjeksiyonu, gerçek eve yazmayı bitir) | Bir kapı kriteri değil; A3'ün testleri `-Duser.home=<temp>` ile geçici olarak çözülür | A3 sonrası bu geçici çözüm kaldırılırsa (ya da yeni bir test yolu eklenirse) **hemen P2'ye girer** |
| **A11** macOS çıkış yolu (Cmd+Q) | Yalnız macOS hedeflenirse anlamlı | §9 kararı macOS'u içeriyorsa **P2'ye girer** |
| **A12** Yazdırma / PDF | Bölüm bitirmeyi engellemiyor; E7 etkisi 3 | ≥5 kullanıcı "yazdıramıyorum" derse |
| **E10** Dosya ilişkilendirmesi / MIME | `.deb` `.desktop` düzeltmesi W1'e biniyor; tam ilişkilendirme paketleme kararına bağlı | W1 sonrası, kurulu taban ölçülünce |
| **E6** Tercihler penceresi | Ayarlar bugün varsayılanla çalışıyor | İkinci "ayar değiştiremiyorum" bildirimi |
| **E8** Satır numaraları | Düz `TextArea`; yön kaybı gerçek ama bölüm bitirmeyi engellemiyor | Komut katmanı sonrası ucuzlar (W3 sonrası) |
| **E9** Sarma anahtarı / yazı tipi ailesi | Zorunlu sarma bilinçli tasarım | Kullanıcı kanıtı |
| **jlink** ile runtime budama (145 → ~50 MiB) | 145 MiB ilk kullanıcıyı engellemiyor; ölçüm ve karar P3 | İndirme dönüşümü ölçülünce |
| **Kod imzalama / notarization** | Koşullu; §9 kararına bağlı | macOS/Windows hedefi |
| **U3, U4, U6, U9, U11** (kontrast kapsamı, macOS tooltip, erişilebilir adlar, ses politikası, çökme metni) | Etkisi 1–2; U1/U5 aynı dosyalara dokunurken bedava gelirse alınır | Yeni diyalog/kontrast dokunuşu zaten yapılıyorsa |
| **D12, D13** (ayar yazma hatası, şema sürümü) | Yeni ayar eklenmedikçe borç büyümüyor | W4'te "bir daha gösterme" ayarı eklenince **D13 aynı PR'da** |
| **M5, M6, M8** (dinleyici sırası, yansımalı undo, dispose) | jlink/modül yolu ve P4 işi | jlink kararı veya ikinci pencere |
| **M2, P4** (sekmeler / çoklu belge) | Tek belge romancı için tutarlı kimlik; çıkış/kirli/taslak matrisini ikiye katlar | İlk kullanıcı geri bildirimi |
| **B3, B5, B6, B7, B8, B9, B10, B12** | Ambalaj/kanal kararına bağlı | §9 kararı + kurulu taban |
| **Yazım denetimi, dışa aktarma (HTML/DOCX), otomatik güncelleme, telemetri** | XL veya gizlilik duruşuyla çelişiyor | Kanıtlı talep |

### 11.2 Yapılmama gerekçesinin testi

Bir işi ertelerken kullanılan ölçüt: **"Bu iş, G1–G4 kapılarından birini açıyor mu?"**
Açmıyorsa ve etkisi ≤3 ise P2'de değildir. Bu ölçüt, yol haritasının sessizce büyümesini
engelleyen tek kuraldır.

---

## 12. İzleme ve Yeniden Planlama

| Tetikleyici | Eylem |
|---|---|
| Bir kapı **bir kez** kaçırıldı | Dalga içinde kal; kartı böl, takvimi değil |
| Bir kapı **iki** kaçırıldı | Kapsamı kes (§6 kesme sırası); yol haritası sürümünü artır |
| İlk gerçek kullanıcı bildirimi geldi | §11 tablosunu yeniden sırala; P3 giriş koşullarını yeniden değerlendir |
| §9 kararı Linux dışında çıktı | W1 ve W4'ü yeniden planla; A11'i P2'ye al |
| Olgunluk endeksi G4'te < 3,55 | Hedef değil, **ölçüm** düzeltilir: hangi boyut eksik kaldıysa o boyutun işi öne alınır |

**Olgunluk yeniden ölçümü:** her kapıda, `analysis.md` §11 tablosundaki beş boyut aynı tanımla
yeniden puanlanır ve bu belgeye tarihli satır olarak eklenir. Puanlama, "hangi vaat artık
koşulsuz doğru" sorusuna verilen cevapla yapılır — test sayısıyla değil.

---

## 13. Ek — Bulgu İzlenebilirliği

Hiçbir bulgu sessizce düşmesin diye: her bulgu kimliği bir karta veya gerekçeli bir ertelemeye
bağlıdır.

| Bulgu | Kart / durum | Bulgu | Kart / durum |
|---|---|---|---|
| D1 | W2.1 | E10 | §11.1 (W1'e kısmi binici) |
| D2 | W2.2 | X1 | W2.5 |
| D3 | W2.3 | X2 | W2.6 |
| D4 | W3.7 | X3 | W2.5 (artımlı sayaç) |
| D5 | W3.8 | X4 | W2.7 |
| D6 | W3.8 | U1 | W2.8 |
| D7 | W2.7 + W3.8 | U2 | W4.4 |
| D8 | W2.4 | U3 | §11.1 |
| D9 | W3.8 | U4 | §11.1 (§9 kararına bağlı) |
| D10 | W3.8 | U5 | W3.9 |
| D11 | W3.8 | U6, U9, U11, U12 | §11.1 / W3.9 (U12) |
| D12 | §11.1 (W4 ayarıyla tetiklenir) | U7 | W4.2 |
| D13 | §11.1 (W4.2 ile **aynı PR'da**) | U8 | W4.3 |
| E1 | W3.5, W3.6 | U10 | A11 → §9 kararı |
| E2 | W3.2 | B1 | W1.1 |
| E3 | W3.3 | B2 | W1.2, W1.3, W4.5 |
| E4 | W3.4 | B3 | §11.1 |
| E5 | W4.1 | B4 | W1.4 (A10) |
| E6 | §11.1 | B5, B6, B7, B8, B9, B10, B12 | §11.1 |
| E7 | A12 → §11.1 | B11 | **kapalı** (analiz §9.8) |
| E8, E9 | §11.1 | B13 | W0.1 |
| M1 | W3.1 | M2 | P4 → §11.1 |
| M3 | W2.8 (A5 ile tek gerçek `Alert` fabrikası) | M4 | A9 → §11.1 |
| M5, M6, M8 | §11.1 | M7 | §11.1 (jlink/undo kararı) |
| T6 | A9 → §11.1 | — | — |

---

## 14. Tek Paragraf

Bu yol haritası, `analysis.md`'nin 12 aday işini Pareto kesme çizgisiyle **6 kapı işi + 3
binici + 3 kesilen** olarak böler. Sıra, "önce kanıt, sonra yetenek, sonra güvence"
mantığındadır: **W1** bir haftada yayınlanabilir bir ürün yaratır (A6, A10) ve ürünün en büyük
riskini — hiç yayınlamamayı — ortadan kaldırır; **W2** veri güvenliği vaadini koşulsuz hale
getirir (A1), donmayı bitirir (A3) ve diyalogları yerelleştirir (A5); **W3** ürünü "metin
alanı"ndan "editör"e çevirir (A4 → A2) ve menü/arama/CLI aynı komut katmanından beslenir;
**W4** cila ve `v1.1.0`dır. Toplam 19,5 net gün (~8 takvim haftası), olgunluk endeksi
2,50 → ≥3,55; işlerin yarısı ve eforun %57'si beş kapının beşini kapatır. Yol haritası, karar
verilmeden yazılabilecek tek açık soruyu (§9: ilk kullanıcı ve OS) varsayımla kapatır ve farklı
cevapların takvime etkisini açıkça yazar.
