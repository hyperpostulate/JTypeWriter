# JTypeWriter — Portable (Kurulumsuz) Sürüm Yol Haritası

> Tarih: 23 Ekim 2026 · Durum: **planlandı, henüz başlanmadı** · Kapsam: tek kanal — yalnız portable
> İlişkili: `analysis.md` (bulgular), `product-roadmap.md` (ürün fazları). Bu dosya, ürün yol haritasındaki RW-38/39/40/42/43/44 işlerini portable hedefe göre yeniden düzenler.

---

## 1. Karar ve Kapsam

**Karar:** JTypeWriter yalnız **portable** olarak dağıtılacak. Installer (`.deb`/`.msi`/`.dmg`), dosya ilişkilendirmesi ve otomatik güncelleme kapsam dışı. Kullanıcı uygulamayı indirir, açar, çalıştırır; verisi uygulama klasöründe taşınır.

Bunun kabul edilen sonuçları:

| Kaybedilen | Etki | Telafi |
|---|---|---|
| `.txt` dosyasına çift tıkla açma | Yok | "Birlikte aç" ile manuel; ilk çalıştırma ekranında anlatılır |
| Menü/başlatıcı kısayolu | Küçük | Windows'ta `.exe`e kısayol; macOS'ta Dock'a sürükleme |
| Otomatik güncelleme | Küçük | Yeni zip'i `data/` klasörünü koruyarak üzerine aç; sürüm notlarında anlatılır |
| Paket yöneticisi üzerinden kurulum | Küçük | Doğrudan indirme + SHA-256 |

**Kazanılan:** admin hakkı gerekmez, sisteme kurulum yok, veri taşınır, "sistemimde iz bırakma" garantisi, dağıtım sürtünmesi düşük.

> Not: Bu, uygulamanın **varsayılan ve tek** davranışı olacak. "Kurulu mod" için ikinci bir kanal **yok**; yalnızca yazılamayan medya gibi zorunlu durumlarda sessiz olmayan bir geri düşüş (fallback) var (bkz. §4.2).

---

## 2. "Portable" Tanımı — Kabul Kriterleri

Aşağıdakilerin tamamı sağlanmadan portable denilemez:

1. **İzsiz çalışma:** Temiz bir makinede çalıştır → yaz → kaydet → kapat sonrası `~` altında, `~/.openjfx` dahil, **hiçbir** JTypeWriter dosyası kalmaz.
2. **Veri taşınır:** `data/` klasörü uygulamayla birlikte kopyalandığında ayarlar, son dosya, tema, dil ve kurtarma taslağı yeni makinede aynen açılır.
3. **Yetki gerekmez:** Yönetici/root hakkı olmadan, `Program Files` dışında herhangi bir klasörden çalışır.
4. **Yazılamayan medya:** Uygulama klasörü salt-okunur ise (CD, kilitli USB, ağ paylaşımı) açılış **başarısız olmaz**; kullanıcıya açık bir bildirimle kullanıcı dizinine geçer.
5. **Çıkarılabilir medya güvenliği:** FAT32/exFAT USB'de kaydetme sırasında elektrik/USB kesintisi belgeyi bozmaz; en kötü durumda son yedek (`file.bak`) geçerlidir.
6. **Tek örnek:** Aynı USB'den iki kez çalıştırıldığında ikinci örnek veriyi ezmez.
7. **Boyut:** Dizin ≤ 70 MB, zip ≤ 45 MB (mevcut 146 MB'a karşı).
8. **Başlangıç:** Soğuk başlangıç medyanı ≤ 1,5 sn (10 koşu).

---

## 3. Bugünkü Engeller (kanıtlı)

| # | Engel | Kanıt | Etki |
|---|---|---|---|
| E1 | Veri `~/.jtypewriter` altına yazılıyor | `AppDirs.java:15` | İz bırakır, veri taşınmaz |
| E2 | JavaFX native'leri `~/.openjfx/cache`e çıkarılıyor | `NativeLibLoader.cacheLibrary()` (OpenJFX kaynağı), bu makinede `~/.openjfx/cache` mevcut | İzsiz çalışma iddiasını bozar; salt-okunur home'da başlatma kırılır |
| E3 | Runtime 134 MB, tüm JDK modülleri (53 modül) | `releases/linux/JTypeWriter/lib/runtime/release` | USB dağıtımı için kabul edilemez |
| E4 | `javafx-media` yalnız tek WAV için taşınıyor (+ native medya) | `pom.xml`, `TypewriterSound.java` | Boyut + paketleme yüzeyi |
| E5 | WAV, `java.io.tmpdir`e çıkarılıyor | `TypewriterSound.java:103-105` | `/tmp` izi, çökmede artık |
| E6 | `ATOMIC_MOVE` yoksa atomik olmayan rename'e düşüyor | `AtomicFiles.java:66-69` | FAT32/exFAT USB'de "yarım dosya yok" garantisi zayıflar |
| E7 | Boş `main.fxml` + `FXMLLoader` | `FxApplication.java:29-39` | Gereksiz modül/dolaylılık |
| E8 | `AppDirs` her OS'te aynı yol | `AppDirs.java:14-25` | Portable'da anlamsız; yine de fallback için gerekli |
| E9 | Tek örnek kilidi yok | — | Aynı `data/` iki süreçte bozulur |
| E10 | Linux'ta tıkla-çalıştır yok (`bin/JTypeWriter` betiği) | `releases/linux/JTypeWriter/bin/` | Çift tıklama güvenilmez |
| E11 | macOS imzasız/notarize edilmemiş | `package.sh` | Gatekeeper "açılamıyor" der |

---

## 4. Hedef Mimari

### 4.1 Dizin düzeni (dağıtım artefaktı)

```
JTypeWriter-1.0.0-linux-x64/
├── JTypeWriter              # başlatıcı (Windows: JTypeWriter.exe, macOS: JTypeWriter.app)
├── runtime/                 # jlink ile üretilmiş minimal JRE + JavaFX native'leri
├── app/jtypewriter.jar      # modüler uygulama
├── data/                    # TAŞINAN kullanıcı verisi
│   ├── settings.properties
│   ├── recovery/
│   ├── logs/
│   └── instance.lock
├── LICENSE
└── PORTABLE-README.txt      # ilk çalıştırma notları, sürüm, checksum
```

**Değişmez kural:** `data/` dışında hiçbir yere yazılmaz. Güncelleme = yeni klasörü aç, `data/`yı koru.

### 4.2 Veri yolu çözümleme (tek kaynak, öncelik sırası)

1. `-Djtypewriter.home=<yol>` — geliştirici/test/ileri düzey.
2. `JTYPEWRITER_HOME` ortam değişkeni.
3. `<uygulama-dizini>/data` — **yazılabilirse varsayılan** (portable mod).
4. `<user.home>/.jtypewriter` — yalnızca (3) yazılamıyorsa; açılışta **bir kez** görünür uyarı: "Uygulama klasörü salt-okunur; veriler kullanıcı dizinine yazılıyor."

Uygulama dizini çözümü: `jpackage.app-path` → yoksa `ProtectionDomain` kod kaynağı → yoksa `user.dir`. Üçü de başarısızsa (4).

### 4.3 Native çözümü (E2'nin kesin çözümü)

OpenJFX `NativeLibLoader` arama sırası: **önce `${java.home}` (jlink'lenmiş JavaFX modülleri) veya jar'ın yanındaki dizin**, sonra jar içinden çıkarıp `javafx.cachedir` (varsayılan `~/.openjfx/cache/<sürüm>/<arch>`) altına yazar.

Bu yüzden doğru çözüm **jlink**: JavaFX modülleri `${java.home}` içine girdiğinde çıkarım hiç yapılmaz. Alternatif/geçici çözüm `-Djavafx.cachedir=<app>/data/.fx-cache` olur ama bu, "data dışına yazma yok" kuralıyla çelişmez; yine de `data/` içinde native kopyaları taşımak istemiyoruz — **tercih jlink**.

Doğrulama: `-Djavafx.verbose=true` ile çalıştır, `~/.openjfx` oluşmamalı.

### 4.4 Dosya sistemi dayanıklılığı

- Açılışta veri dizininde **atomik taşıma sondası** (küçük temp dosya + `ATOMIC_MOVE`); sonuç önbelleğe alınır.
- Sonda başarısızsa (FAT32/exFAT): yazma = temp + `force(true)` + **geri okuyup SHA-256 doğrulama** + rename + dizin `fsync` (mümkünse) + kullanıcıya bir kez "bu medya atomik kaydı desteklemiyor, yedekler kullanılıyor" bildirimi.
- `BackupKeeper` portable'da **zorunlu** (kapatılamaz).
- Çıkarılabilir medya kopması: `IOException` yakalanır, belge bellekte dirty kalır, "medya kayboldu; farklı kaydet" diyaloğu açılır.
- Açılışta eski `jtw-*.tmp` dosyaları temizlenir.

---

## 5. Fazlar

Sıra bağımlılığa göre; her faz kendi başına test edilebilir ve geri alınabilir. Efor: 1 geliştirici, yarı zamanlı.

### F0 — Hazırlık ve Ölçüm (0,5-1 gün)

| # | Adım | Çıktı |
|---|---|---|
| F0.1 | Baseline ölç: dizin boyutu, zip boyutu, soğuk/sıcak başlangıç (10 koşu medyanı), `~/.openjfx` ve `/tmp` iz envanteri | `docs/portable-baseline.md` |
| F0.2 | İz denetim betiği: zaman damgası işaretle → uygulamayı çalıştır/kapat → `find ~ /tmp -newer marker` | `scripts/portable-trace-audit.sh` |
| F0.3 | **Kritik spike (yarım gün):** JavaFX modüllerini jlink ile runtime'a al; `-Djavafx.verbose=true` ile `~/.openjfx` oluşmadığını ve boyutun ≤70 MB olduğunu kanıtla | Spike notu: jlink yolu doğrulandı/çürütüldü |
| F0.4 | Karar kaydı: spike başarısızsa alternatif `-Djavafx.cachedir=<app>/data/.fxcache` planı devreye girer | Kısa ADR |

**Kapı:** F0.3 kanıtlanmadan F2'ye başlanmaz (en pahalı varsayım).

### F1 — Veri Yolu, Tek Örnek, Geçiş (2-3 gün)

| # | Adım | Dosyalar | Kabul |
|---|---|---|---|
| F1.1 | `AppDirs`'i **çözümleyiciye** çevir: öncelik sırası (§4.2), uygulama dizini tespiti, yazılabilirlik testi, tek seferlik uyarı | `AppDirs.java` (veya yeni `AppHome.java`) | 4 önceliğin her biri testte; salt-okunur dizinde fallback |
| F1.2 | Tüm çağrıları enjekte edilebilir hâle getir: `AppLogging.init(Path)`, `AppSettings.load(Path)`, `RecoveryStore(Path)`, `CrashHandler` log yolu | `AppLogging`, `AppSettings`, `AppGraph`, `CrashHandler` | Testler gerçek `~` dizinine dokunmuyor (`@TempDir`) |
| F1.3 | İlk çalıştırmada `data/` oluştur; `data/` yoksa ve eski `~/.jtypewriter` varsa **tek seferlik içe aktarma** teklifi (kaynak salt-okunur, geri yazma yok) | `AppGraph` / küçük `PortableDataMigrator` | İçe aktarma sonrası eski dizin değişmiyor |
| F1.4 | **Tek örnek kilidi**: `data/instance.lock` üzerinde `FileChannel.tryLock`; kilit alınamazsa "zaten açık" diyaloğu ve çıkış | `JTypeWriterApp`/`FxApplication` başlangıcı | İkinci örnek veriye yazmıyor; kilit süreç ölünce serbest |
| F1.5 | Kilit/veri yolu için birim testleri + elle senaryo | `AppHomeTest`, `InstanceLockTest` | 2 süreçli test yeşil |

**Kapı:** Hiçbir kod yolu doğrudan `AppDirs.dataDir()` çağırmıyor; hepsi enjekte edilmiş yol kullanıyor.

### F2 — Runtime Temizliği ve Native İzsizlik (2-4 gün)

| # | Adım | Dosyalar | Kabul |
|---|---|---|---|
| F2.1 | `javafx-media` kaldır → `javax.sound.sampled.Clip`, kaynaktan belleğe yükle (temp dosya yok) | `pom.xml`, `TypewriterSound.java` | `/tmp`de WAV yok; hızlı ardışık tuşta ses gecikmesi artmadı; ses aygıtı yokken sessiz |
| F2.2 | Boş `main.fxml` + `FXMLLoader` + `Initializable` kaldır; kök düğüm `MainController`dan | `FxApplication`, `MainController`, `resources/fxml/` | `javafx-fxml` bağımlılığı yok; uygulama aynı görünüyor |
| F2.3 | `module-info.java` ekle (`requires javafx.controls`, `java.desktop`, `java.logging`; `opens` gerekmiyorsa yok) | `src/main/java/module-info.java` | `mvn test` modüler yapıda yeşil (`useModulePath=false`) |
| F2.4 | `package.sh`: `jlink` ile runtime üret (`--add-modules`), `jpackage --runtime-image`; `--enable-native-access` ayarla; `-XX:-UsePerfData` ekle | `package.sh` | İmaj ≤70 MB; `~/.openjfx` **yok**; `/tmp`de `hsperfdata` yok |
| F2.5 | Başlangıç ölçümü ve regresyon kontrolü | — | Medyan ≤1,5 sn, baseline'a göre ≤%20 kötüleşme |

**Kapı:** `javafx.verbose` çıktısında "extracted to cache" satırı yok; `~/.openjfx` hiç oluşmuyor.

### F3 — Dosya Sistemi Dayanıklılığı (2 gün)

| # | Adım | Dosyalar | Kabul |
|---|---|---|---|
| F3.1 | Açılışta atomik taşıma sondası + sonucu önbelleğe al; desteklenmiyorsa doğrulamalı yazma yolu | `AtomicFiles`, `AppGraph` | FAT32'de uyarı bir kez gösterilir |
| F3.2 | Yazma sonrası SHA-256 doğrulama (yalnız atomik olmayan yolda) | `AtomicFiles` | Bozuk yazma sessizce geçmiyor |
| F3.3 | Çıkarılabilir medya kopması: hata yakalama, dirty kalma, "farklı kaydet" diyaloğu | `FileService`, `DialogService` | USB çekilince uygulama çökmüyor, metin kaybolmuyor |
| F3.4 | Açılışta `jtw-*.tmp` temizliği (yalnız kendi veri/dizin kapsamında) | `AtomicFiles`/`AppGraph` | Kalıntı temizleniyor; başka dosyaya dokunulmuyor |
| F3.5 | FAT32/exFAT USB üzerinde gerçek senaryo testi (yaz, kaydet, çıkar, geri tak) | — | Belge + yedek sağlam |

**Kapı:** FAT32 USB'de zorla kesme sonrası belge ya da `file.bak` açılabiliyor.

### F4 — Paketleme ve Başlatıcılar (3-4 gün)

| # | Adım | Çıktı | Kabul |
|---|---|---|---|
| F4.1 | Artefakt adlandırma standardı: `JTypeWriter-<sürüm>-<os>-<arch>.zip` (Windows/macOS) ve `.tar.gz` (Linux) | — | Sürüm tek kaynak: `pom.xml` |
| F4.2 | Windows: app-image zip; `data/` boş şablon; SmartScreen notu | `.zip` | Çift tıkla `JTypeWriter.exe` çalışır |
| F4.3 | macOS: `.app` + **ad-hoc imza** (`codesign -s -`), zip; Gatekeeper ilk açılış talimatı; notarization kararı (ücretli) ayrı madde | `.zip` | Temiz macOS'ta sağ tık → Aç ile çalışır |
| F4.4 | Linux: `tar.gz` + çalıştırılabilir `JTypeWriter` başlatıcı; **AppImage** (FUSE yoksa `--appimage-extract-and-run` notu) | `tar.gz`, `.AppImage` | Dosya yöneticisinden çift tıkla açılır |
| F4.5 | `PORTABLE-README.txt` (ilk çalıştırma, veri taşıma, güncelleme, bilinen sınırlar) + `LICENSE` | Dosyalar | 6 dilde kısa not (en az EN/TR) |
| F4.6 | CI: 3 OS portable artefakt + `SHA256SUMS` + tag tetiği; installer adımları **kaldırılır** | `.github/workflows/maven.yml` | Tag → 3 artefakt + checksum |
| F4.7 | Güncelleme akışı: yeni sürüm zip'i `data/` korunarak açılır; sürüm uyumsuzluğunda ayar şeması kontrolü | Doküman + `AppSettings` şema sürümü | `data/` korunuyor, ayarlar kaybolmuyor |

**Kapı:** Üç OS'te tek komutla artefakt; hiçbiri installer üretmiyor.

### F5 — Doğrulama ve Dokümantasyon (2-3 gün)

| # | Adım | Kabul |
|---|---|---|
| F5.1 | §6 test matrisinin tamamı | Tüm satırlar yeşil |
| F5.2 | İz denetimi: temiz VM/USB, `find ~ /tmp -newer marker` boş | Sıfır iz |
| F5.3 | README (EN + TR kopyası, AGENTS.md kuralı) portable'a göre yeniden yazılır; "fat JAR portable" iddiası kaldırılır | Doküman gerçeği anlatıyor |
| F5.4 | Bilinen sınırlar sayfası: dosya ilişkilendirmesi yok, imza durumu, FAT32 uyarısı | Kullanıcı sürpriz yaşamıyor |
| F5.5 | Sürüm etiketi + checksum + yayın notu | Yayına hazır |

---

## 6. Doğrulama Matrisi (F5.1)

Her satır, yayın öncesi elle veya otomatik olarak kanıtlanır.

| # | Senaryo | Beklenen |
|---|---|---|
| T1 | Temiz Windows 11 VM, admin yok, Masaüstü'nden çalıştır | Açılır, yazar, kaydeder, kapanır; `%USERPROFILE%` altında iz yok |
| T2 | Windows, yol: `C:\Users\İsmail\Yeni Klasör\JTypeWriter` | Türkçe karakterli/boşluklu yolda sorunsuz |
| T3 | macOS arm64, indirilen zip (karantina) | Sağ tık → Aç ile çalışır; Dock'a eklenebilir |
| T4 | Ubuntu 24.04 X11, AppImage | Çift tıkla açılır |
| T5 | Ubuntu Wayland | Pencere/ölçekleme doğru; açılır |
| T6 | FAT32 USB: çalıştır → yaz → kaydet → USB'yi çıkar → geri tak | Belge ve `file.bak` sağlam; uyarı bir kez gösterildi |
| T7 | USB çekme (kaydetme sırasında) | Çökme yok; metin bellekte; "farklı kaydet" diyaloğu |
| T8 | Uygulama klasörü salt-okunur | Açılır; veri kullanıcı dizinine gider; görünür uyarı |
| T9 | Aynı `data/` ile ikinci örnek | "Zaten açık" diyaloğu; veri bozulmaz |
| T10 | `kill -9` sonrası yeniden açılış (USB) | Kurtarma taslağı teklif edilir; geri yüklenir |
| T11 | Sürüm güncelleme: yeni zip, `data/` korunur | Ayarlar/dil/tema/son dosya korunur |
| T12 | 50 MB belge, USB'de kaydet | UI donmaz; süre makul; hata yok |
| T13 | İz denetimi `find ~ /tmp -newer marker` | Boş (uygulama dizini hariç) |
| T14 | TR ve DE sistem locale'i ile ilk açılış | Doğru dil, bozuk metin yok |
| T15 | Ses aygıtı yok | Sessiz çalışır; hata diyaloğu yok |
| T16 | %150 ekran ölçekleme | Metin keskin; araç çubuğu taşmıyor |

---

## 7. Bağımlılıklar ve Sıra

```
F0 (ölçüm + jlink spike)
 └─> F1 (veri yolu + kilit)  ──┐
 └─> F2 (medya/FXML/jlink)  ──┼─> F4 (paketleme) ─> F5 (doğrulama)
        F3 (FS dayanıklılığı) ┘
```

- **F0.3 kanıtlanmadan F2/F4 başlamaz.**
- **F3, ürün yol haritasındaki P0 kurtarma düzeltmelerine bağlıdır** (RW-01..04). Portable, veri kaybı riskini büyüttüğü için P0 kapanmadan portable yayın yapılmaz.
- F1 ve F2 paralel yürüyebilir (farklı dosyalar); F4 ikisini de bekler.
- F5, F4 bitmeden başlamaz.

---

## 8. Risk Kaydı

| Risk | Olasılık | Etki | Azaltım |
|---|---|---|---|
| jlink + JavaFX modüler jar'larda native'ler yine çıkarılıyor | Orta | Yüksek | F0.3 spike'ı bunu **ilk gün** çürütür; başarısızsa `-Djavafx.cachedir` `data/` içine alınır |
| macOS Gatekeeper imzasız uygulamayı engelliyor | Yüksek | Orta | Ad-hoc imza + sağ tık talimatı; notarization ayrı (ücretli) karar |
| Windows SmartScreen uyarısı | Yüksek | Düşük | "Ek bilgi → Yine de çalıştır" talimatı; imza opsiyonel |
| Linux AppImage FUSE eksikliği | Orta | Orta | `--appimage-extract-and-run` notu; ayrıca `tar.gz` yedeği |
| FAT32/exFAT'te atomik olmayan yazma | Yüksek | Yüksek | F3.1-3.2 sondası + doğrulama + zorunlu yedek + uyarı |
| Kullanıcı USB'yi kaydetme sırasında çekiyor | Orta | Yüksek | F3.3 hata yolu; belge dirty kalır |
| `data/` yazılamıyor (Program Files, ağ paylaşımı) | Orta | Orta | F1.1 fallback + görünür uyarı |
| Kullanıcılar "kurulum yok" beklentisini yanlış anlıyor | Orta | Düşük | `PORTABLE-README.txt` + ilk çalıştırma ekranı |
| Antivirüs yanlış pozitifi | Düşük | Orta | İmza (uzun vadede); checksum + kaynak derleme talimatı |
| Portable modda yeni test edilebilir sınıfların kapsamı düşük | Orta | Orta | F1/F3 birim testleri + F5 matrisi |
| Veri göçü eski ayarları bozuyor | Düşük | Yüksek | İçe aktarma kaynak dizini salt-okunur açar; kopya üzerinde çalışır |

---

## 9. Kapsam Dışı (bilinçli)

- Installer (`.deb`, `.msi`, `.dmg`) ve paket yöneticisi dağıtımı
- Dosya ilişkilendirmesi / "Birlikte aç" kaydı
- Otomatik güncelleme (yalnız "yeni sürüm var" bilgisi opsiyonel)
- Bulut senkronizasyonu, hesap sistemi
- Aynı anda iki farklı `data/`yı birleştirme
- Çoklu belge/sekme (ürün yol haritası P3)

---

## 10. Efor Özeti

| Faz | İyimser | Kötümser |
|---|---|---|
| F0 Hazırlık + spike | 0,5 gün | 1 gün |
| F1 Veri yolu + kilit + göç | 2 gün | 3 gün |
| F2 Medya/FXML/jlink | 2 gün | 4 gün |
| F3 FS dayanıklılığı | 1,5 gün | 2 gün |
| F4 Paketleme + CI + launcher | 3 gün | 4 gün |
| F5 Doğrulama + doküman | 2 gün | 3 gün |
| **Toplam** | **~11 gün** | **~17 gün** (yarı zamanlı ~4 hafta) |

---

## 11. Yayın Kontrol Listesi

- [ ] F0.3 spike kanıtlandı, `~/.openjfx` oluşmuyor
- [ ] Hiçbir kod yolu `user.home`a doğrudan yazmıyor
- [ ] Tek örnek kilidi çalışıyor
- [ ] FAT32 USB testi (T6, T7, T10) geçti
- [ ] Salt-okunur fallback (T8) geçti
- [ ] 3 OS artefaktı + `SHA256SUMS` üretildi
- [ ] macOS ad-hoc imza + ilk açılış talimatı hazır
- [ ] Linux AppImage ve tar.gz doğrulandı
- [ ] `mvn clean test` + TR/DE locale koşuları yeşil
- [ ] README (EN + TR) ve `PORTABLE-README.txt` güncel
- [ ] Bilinen sınırlar yayınlandı
- [ ] `data/` korunarak güncelleme adımı dokümante edildi

---

## 12. Başlama Koşulu ve İlk Adım

**Başlama koşulu:** Ürün yol haritasındaki **P0 kurtarma düzeltmeleri (RW-01..04) tamamlanmış** olmalı. Portable, veri kaybı riskini büyütür; güvenlik ağı olmadan taşınabilirlik eklemek yanlış sıradır.

**İlk adım (yarım gün):** F0.3 spike'ı. JavaFX modüllerini jlink ile runtime'a alıp `-Djavafx.verbose=true` ile çalıştır; iki soruyu kesin yanıtla:

1. `~/.openjfx` oluşuyor mu? (oluşmuyorsa native izi sorunu kökten çözülür)
2. Dizin ≤70 MB'a iniyor mu?

Bu iki cevap, kalan 10-16 günün planını ya doğrular ya da `-Djavafx.cachedir` alternatifine yönlendirir.
