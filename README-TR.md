<p align="center">
  <img src="src/main/resources/icons/jtypewriter-512.png" alt="JTypeWriter" width="512" height="512">
</p>

<h1 align="center">JTypeWriter</h1>

<p align="center">
  <a href="https://github.com/hyperpostulate/JTypeWriter/blob/main/pom.xml"><img src="https://img.shields.io/badge/Java-25-orange.svg" alt="Java 25"></a>
  <img src="https://img.shields.io/badge/Platforms-Linux%20%7C%20Windows-lightgrey.svg" alt="Platforms">
  <img src="https://img.shields.io/badge/Languages-6-blue.svg" alt="6 languages">
  <a href="https://www.apache.org/licenses/LICENSE-2.0"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License: Apache 2.0"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml/badge.svg" alt="Java CI"></a>
  <a href="https://github.com/hyperpostulate/JTypeWriter/actions/workflows/maven.yml"><img src="https://img.shields.io/endpoint?url=https%3A%2F%2Fraw.githubusercontent.com%2Fhyperpostulate%2FJTypeWriter%2Fgh-pages%2Fbadges%2Ftests.json" alt="Tests"></a>
</p>

<p align="center">Yaratıcı yazarlar için sessiz, çevrimdışı bir masaüstü metin editörü.</p>
<p align="center">Daktilo gibi ses çıkarır, yazdığını sayar ve o yazıyı kaybetmeyi olabildiğince zorlaştırır.</p>

<p align="center">English version: <a href="README.md">README.md</a></p>

JTypeWriter tek bir düz metin belgesi açar ve sonra geri planda kalır: hesap yok, bulut yok, proje biçimi yok. Metniniz seçtiğiniz dosyada ve o dosyanın zaten sahip olduğu kodlamada kalır. Paketlenmiş uygulama kendi Java çalışma zamanını taşır, yani önce hiçbir şey kurmanız gerekmez. Linux ve Windows'ta çalışır ve arayüzü İngilizce, Türkçe, Almanca, Fransızca, İspanyolca ve İtalyanca konuşur.

## Neler var?

- **Kazara mahvedemeyeceğiniz bir taslak.** YOLO modu Backspace, Delete, Kes ve Geri Al/İleri Al'ı kapatır; yalnızca ileri yazabilirsiniz. Her kayıt atomiktir, dönen yedekler tutar ve kaydedilmemiş iş 30 saniyede bir yedeklenir.
- **Görünür bir güvenlik ağı.** Durum çubuğu her otomatik yedeği saatiyle onaylar; çökmeden sonra taslak geri teklif edilir ve o iletiyi kapatmak taslağı asla silmez.
- **Yazma hissi.** Yazarken, Backspace veya Delete'e bastığınızda - seçtiğiniz ses düzeyinde - daktilo sesi, üç tema ve altı metin rengi; hepsi yazarken değiştirilebilir.
- **Yazdıkça sayılar.** Kelime, boşluklu ve boşluksuz karakter, satır, tuş vuruşu, oturum süresi ve molaları saymayan sürdürülebilir kelime/dakika.
- **Ekranda yalnızca metin.** `F11` tam ekran, `Ctrl+T` araç çubuğunu ve içinde duran komut menüsünü birlikte gizler; pencere başlığı hangi belgenin açık olduğunu ve kaydedilip kaydedilmediğini söyler.
- **Kaldığınız yerden devam eden oturum.** Dil, tema, metin rengi, yazı boyutu, son belge ve onun imleç/scroll konumu ile pencere geometrisi bir sonraki açılışta geri yüklenir.
- **Hiçbir şey makinenizden çıkmaz.** Hesap yok, telemetri yok, güncelleme denetimi yok, ağ isteği yok.

---

## İçindekiler

- [Neler var?](#neler-var)
- [Özellikler](#özellikler)
- [Verileriniz](#verileriniz)
- [Klavye Kısayolları](#klavye-kısayolları)
- [Temalar](#temalar)
- [Desteklenen Diller](#desteklenen-diller)
- [Kurulum](#kurulum)
- [Teknik Referans](#teknik-referans)
- [Lisans](#lisans)
- [Geliştirici](#geliştirici)
- [Katkıda Bulunma](#katkıda-bulunma)

---

## Özellikler

### Daktilo Ses Efektleri

- Karakter yazmak takırdar; imleç hareketi, fonksiyon tuşları ve kısayollar sessizdir.
- Ses düzeyi araç çubuğundaki Ses açılır listesinden ya da `Görünüm → Ses` menüsünden seçilir: Kapalı, %25, %50, %75, %100. Seçim yeniden başlatmalar arasında hatırlanır.
- Her tık kendi sesini başlatır, öncekini kesmez; böylece hızlı yazım kekeleme değil hızlı yazım gibi duyulur.

### YOLO Modu

- `Ctrl+Shift+Y` Backspace, Delete, Kes ve Geri Al/İleri Al'ı devre dışı bırakır; böylece hiçbir düzenleme belgedeki mevcut karakterleri silemez.
- Kopyala, Yapıştır ve Tümünü Seç çalışmaya devam eder; sağ-tık menüsünde Kes soluk gösterilir.
- Modun açık olduğunu `[YOLO]` başlık rozeti ve araç çubuğundaki YOLO açılır listesi gösterir.
- Bilinçli olarak hatırlanmaz: silmeyi engelleyen bir mod bir sonraki açılışta hâlâ etkin olmamalıdır.

### Gerçek Zamanlı İstatistikler

Alt çubuk canlı olarak şunları gösterir:

- Kelime sayısı, boşluklu ve boşluksuz karakter sayısı, satır sayısı.
- Toplam tuş vuruşu ve oturum süresi (`SS:DD:SS`).
- Yalnızca aktif yazım süresine dayanan kelime/dakika; molalar değeri düşürmez.
- `Ctrl+S` sonrası bir kayıt onayı (`Kaydedildi 12:04`) ve her otomatik yedekten sonra ayrı bir onay (`Yedek kaydedildi: 12:04`); böylece gerçek bir kayıt kurtarma kopyasıyla karışmaz.
- Varsayılan 13 px metin; çift tıklayarak küçük, orta ve büyük arasında geçiş yapılır ve seçim hatırlanır.

Kayıt onayı sonraki düzenlemede kaybolur; yedek onayı ise gerçek bir kayıt onun yerini alana ya da yeni bir belge açılana kadar kalır.

### Yazdırma ve PDF Dışa Aktarma

- `Ctrl+P` belgeyi yazdırır, `Ctrl+Shift+E` PDF olarak dışa aktarır; ikisi de Dosya menüsünde yer alır.
- İkisi de **kaydedilmiş `.txt` dosyasını** kullanır, editör tamponunu değil: kaydedilmemiş değişiklik - ya da hiç kaydedilmemiş bir belge - önce Kaydet / Vazgeç sorusu çıkarır ve vazgeçmek hiçbir şeyin yazdırılmaması, hiçbir şeyin dışa aktarılmaması demektir.
- Sayfa sabittir: paketlenmiş DejaVu Sans Mono, 12 punto, beyaz üzerine siyah, 20 mm kenar boşluklu A4 dikey. Tema, metin rengi ve editör yazı boyutu uygulanmaz.
- PDF'i uygulamanın kendisi yazar ve aynı yazı tipini gömer, yani metni seçilebilir ve kopyalanabilir kalır.
- Yazdırma ile PDF tek bir yerleşimi paylaşır; kâğıt ve dosya satırları aynı yerden kırar.
- Hatalar uyarı açar; tamamlanan yazdırma ya da yazılan dosya hiçbir şey söylemez.

### Tam Ekran ve Araç Çubuğu

- `F11` tam ekranı açar ve kapatır, `Escape` tam ekrandan çıkar.
- `Ctrl+T` araç çubuğunu - ve içinde yaşayan komut menüsünü - gizler, ekranda yalnızca metin kalır.
- Araç çubuğu her genişlikte tek satır kalır: dar pencerede açılır liste başlıkları yalın değere iner, hiçbir kontrol kırpılmaz veya alt satıra geçmez.
- Her kontrole `Tab` ile sabit bir sırada ulaşılır ve her kontrol, varsa kısayolunu da içeren arayüz dili tooltip'i taşır ("Kaydet (Ctrl+S)").
- Görünür veya gizli durum, tam ekran ve durum çubuğu boyutu hatırlanır; her değişiklik o anda kaydedilir.
- Tam ekranda bırakılan bir pencere yeniden tam ekran açılır; sonradan çıkıldığında hatırlanan konuma dönülür.

### Komut Menüsü

- Araç çubuğunun solundaki `☰` düğmesi Dosya, Düzen, Görünüm ve Yardım menülerini taşır.
- Kalıcı bir menü şeridi değil bir menü düğmesidir; içinde yaşadığı araç çubuğuyla birlikte gizlenir, tam ekranda da peşinizden gelmez.
- Platformun kendisinin yürütmediği her komut burada listelenir; `Yeni` (`Ctrl+N`) ve `Farklı Kaydet` (`Ctrl+Shift+S`) dahil.
- `Görünüm → Tema`, `YOLO`, `Renk` ve `Boyut` o anki değeri işaretli gösteren alt menüler açar; `Ses` düzey merdivenini açar; `Yardım → Hakkında` Hakkında kartını açar.
- Menü girdileri, kısayollar ve araç çubuğu düğmeleri tek bir komut listesinden üretilir, yani birbirinden kopamaz.

### Ara ve Değiştir

- `Ctrl+F` pencerenin sağ üst köşesinde, metnin üzerinde bir ara/değiştir kartı açar; `Ctrl+H` aynı kartı değiştirme alanı odaklı açar.
- Tek "Aranan" alanı, tek "Yeni metin" alanı, "Büyük/küçük harf duyarlı" onay kutusu, Sonrakini bul / Öncekini bul / Değiştir / Tümünü değiştir düğmeleri ve canlı bir durum satırı ("3 / 12", "Eşleşme yok").
- Kart bir panel, modal pencere değil: belge görünür ve düzenlenebilir kalır.
- `F3` ve `Shift+F3` eşleşmeler arasında iki yönde gezer ve sarar; kart kapandıktan sonra da çalışır, henüz sorgu yoksa `F3` kartı açar.
- Kartı `Escape` ya da Kapat düğmesiyle kapatmak sorguyu bir sonraki `F3` için saklar.
- Arama düz metindir, regex değil.
- `Tümünü değiştir` tek bir geri alma adımıdır ve değiştirme kendi çıktısını taramaz.
- YOLO açıkken değiştirme her işlem için bir kez sorar ve iptal hiçbir şey yazmaz.
- Büyük belgeler pencereyi kilitlemez: tarama arka planda koşar.

### Hoş Geldin, Son Dosyalar ve Hakkında Kartları

- Üçü de ara/değiştir panelinin durduğu sağ üst köşede, metnin üzerinde kart olarak açılır; biri açıldığında orada duran kart kapanır.
- Modal değildir: belge kullanılabilir kalır; `Escape`, `Enter` ya da `Kapat` düğmesi kartı kapatır ve imleci metne geri verir.
- Kısa bir pencerede karttan daha uzun bir kart pencereye sığacak kadar küçülür ve kaydırılır, kenardan taşmaz.
- İçerik her açılışta ve dil değişiminde yeniden kurulur, yani açık kalan bir kart önceki dilde kalamaz.
- Hoş geldin kartındaki "bir daha gösterme" onay kutusu hatırlanır; geri dönüş yolu `Dosya → Karşılama kartını göster`'dir.
- `Dosya → Son Dosyalar` son on belgeyi en yenisi başta olacak şekilde listeler. Satırlar dosya adını gösterir, tam yol ipucundadır; uygulama dışında silinen bir belge listeden sessizce düşer.
- Son dosya satırı `Aç` ile aynı kaydedilmemiş değişiklik kapısından geçer - düzenleyiciyi atmanın ikinci bir yolu değildir.

### Veri Güvenliği ve Kurtarma

- **Atomik kayıt.** Her kayıt geçici bir dosyaya yazılır, diske indirilir ve yerine taşınır; böylece kayıt ortasında çökme ya da elektrik kesintisi belgeyi yarım bırakamaz.
- **Dönen yedekler.** Üzerine yazma `<document>.bak`, `.bak.1` ve `.bak.2` bırakır ve belgenin dosya izinlerini korur.
- **Otomatik yedekler.** Kaydedilmemiş iş 30 saniyede bir ve pencere odağı kaybolduğunda anlık görüntülenir; durum çubuğu her birini saatiyle onaylar.
- **Çökmeden sonra kurtarma.** Geriye kalan taslak bir sonraki açılışta teklif edilir: geri yükle, sil ya da sonra sor. Bu iletiyi kapatmak hiçbir şeyi silmez.
- **Silme istisnadır.** Taslak yalnızca kapatma istemi yanıtlandığında silinir - belge kaydedildiyse ya da açıkça atmayı seçtiyseniz. Diğer tüm çıkışlar taslağı bir sonraki açılış için saklar.
- **Yedek belgeyi geriye çeviremez.** Diskteki dosya taslaktan daha yeniyse metin yine geri yüklenir, ancak özgün yolu olmadan; böylece kayıt Farklı Kaydet'e zorlanır.
- **Arızalar görünür.** Yazılamayan bir yedek, sonraki yazım başarılı olana kadar durum çubuğunda uyarı bırakır ve tek bir diyalogla ne yapılacağını açıklar. Başarısız bir taslak silme kendi kısa uyarısını gösterir ve diyalog açmaz.
- **Belge farkındalığı.** Başlık belgenin adını, kaydedilmemiş değişiklik varken `*` işaretini ve mod açıkken `[YOLO]` rozetini gösterir.
- **Yeni belge gerçek bir karardır.** `Ctrl+N` aynı kaydet / kaydetme / iptal sorusunu sorar ve geri alma geçmişi belge sınırında durur.

### Kodlama Güvenliği

- Belge, açıldığı kodlamayla geri kaydedilir.
- BOM'lu bir dosya, BOM'unun belirttiği kodlamayla çözülür.
- BOM'suz bir dosya geçerli UTF-8 olmalı ya da UTF-16 bayt düzenini izlemelidir; aksi halde sessiz bozulma yerine Windows-1254 mü UTF-8 mi olduğu sorulur.
- Bayt sıra işareti içerikle çelişiyorsa bu bir tahmin değil sorudur: uygulama bildirilen kodlamayı ve dosyayı adlandırır, açmadan önce sorar.
- Kodlamanın temsil edemediği karakterler sessizce `?` yapılmaz; UTF-8'e geçiş teklif edilir.

### Ayarlar ve Oturum Geri Yükleme

- Hatırlananlar: dil, tema, metin rengi, yazı boyutu, ses düzeyi, son belge, onun imleç/scroll konumu ve klasörü, pencere geometrisi, araç çubuğu görünürlüğü, durum çubuğu boyutu ve tam ekran.
- Hiçbir şey açılışı engellemez: kaybolmuş bir belge sessizce atlanır, bozuk bir ayar dosyası varsayılanlara döner.
- Yazılamayan bir ayar dosyası bir kez uyarır, dosyayı adlandırır ve neyin hatırlanmayacağını söyler.
- Eski bir sürümün yazdığı dosya değer kaybetmeden açılır; daha yeni bir sürümün dosyası bu sürümün anladığı kadarıyla okunur ve asla geriye düşürülmez.
- YOLO modu bilinçli olarak **geri yüklenmez**.

### Uygulama Dışından Belge Açma

- `JTypeWriter notlar.txt` o belgeyi açar; dosya ilişkilendirmesi veya çift tıklama aynı yolu aynı koddan geçirir.
- Yalnızca bir dosya adlandırılabilir: ikinci argüman adını söyleyen bir diyalogla reddedilir.
- Olmayan bir yol, bir klasör ya da çözülemeyen bir argüman, sessizce boş bir düzenleyiciye düşmek yerine arayüz dilinde bildirilir.
- Argüman yoksa, kendi onay kutusuyla kapatmadığınız sürece karşılama kartı görünür.
- Kurtarılan taslak komut satırındaki belgeden önce gelir: o metnin tek kopyası odur, adlandırılan dosya ise hâlâ diskte.
- **Sürükle-bırak da aynı şekilde çalışır.** Bir `.txt` veya `.md` dosyasını pencerenin herhangi bir yerine bırakın; dosya Aç ile aynı kapıdan, kaydetme sorusu dahil, açılır.
- Hiç dosya olmaması, birden çok dosya, bir klasör, bir PDF ya da bırakmadan önce kaybolmuş bir dosya, neyin bırakıldığını söyleyen bir diyalogla reddedilir.

### Tek Örnek

- Kullanıcı hesabı başına bir örnek; `~/.jtypewriter/` altındaki bir kilit dosyası ikinci başlatmayı durdurur.
- İkinci başlatma kendi penceresini açmaz: yerelleştirilmiş bir uyarı gösterir, onaylanmayı bekler ve kapanır.
- Çalışan örnek çökerse kilit işletim sistemi tarafından bırakılır, yani çökme sizi dışarıda bırakmaz.

### Temalar

- **Koyu** - varsayılan; göz yormaz.
- **Açık** - temiz ve parlak.
- **Sepya** - sıcak, kâğıt hissi.

`Ctrl+Shift+T` Koyu → Sepya → Açık sırasıyla gezer; araç çubuğundaki Tema açılır listesi ve `Görünüm → Tema` doğrudan seçer. Temalar yazarken değiştirilebilir ve durum çubuğu metni üç temada da WCAG AA kontrastını (en az 4.5:1) karşılar.

### Metin Renkleri

Altı renk - Varsayılan, Adaçayı, Lavanta, Kum, Çelik, Gül. `Ctrl+Shift+C` sırayla gezer; araç çubuğu ve `Görünüm → Renk` doğrudan seçer.

### Desteklenen Diller

Altı dil: İngilizce, Türkçe, Almanca, Fransızca, İtalyanca ve İspanyolca. Dil değiştirildiğinde araç çubuğu, menüler, diyaloglar, kartlar, pencere başlığı ve durum çubuğu anında güncellenir; her tarih ve saat o dilin kendi biçimiyle yazılır.

---

## Verileriniz

JTypeWriter'ın sakladığı her şey makinenizde kalır. Hesap yok, telemetri yok, güncelleme denetimi yok ve ağ isteği yok: uygulama kendisi hiçbir şey göndermez; dışa açılan tek eylem, Hakkında kartındaki e-posta bağlantısıdır ve `mailto:` adresini kendi posta istemcinize devreder.

- **Nerede durur:** `~/.jtypewriter/` - tercihler, kurtarma taslakları, loglar ve tek örnek kilidi.
- **Taslaklar metninizi içerir.** Her kurtarma taslağı kaydedilmemiş belgenin metnini birebir tutar; ertelediğiniz taslak temiz çıkıştan sonra da kalır ve bir sonraki açılışta yeniden teklif edilir.
- **Yedekler belgelerinizin yanında durur.** Üzerine yazma `<document>.bak`, `.bak.1` ve `.bak.2` bırakır; kayıt, dosyayı yerine taşımadan önce yanına `jtw-*.tmp` geçici dosyası yazar. İkisi de belge metnini içerir, bu yüzden bir belge diskte boyutunun dört katı yer kaplar - kayıt sırasında kısa süreliğine altı katına çıkar. İlk kayıt bunu bir kez söyler.
- **Loglar** her biri bir megabaytlık, dönen üç dosyada tutulur; dosya yollarını, hata iletilerini ve yığın izlerini içerir.
- **Kurtarma taslakları temizlenir.** Ertelenen taslakların süresi 30 gün sonra dolar ve park edilmiş küme 10 dosya / 64 MiB ile sınırlıdır; etkin bir taslağın süresi asla dolmaz - başka bir çalışan örneğe ait olabilir.
- **Hepsini silmek:** önce uygulamayı kapatın - aksi halde log tutucusu ve kilit yeniden oluşturulur - sonra `~/.jtypewriter` dizinini ve belgelerinizin yanındaki `<document>.bak*` ile artık kalmış `jtw-*.tmp` dosyalarını silin. Uygulama içinde "tüm verileri sil" komutu yoktur ve uygulamayı kaldırmak bu veriyi silmez.

Dosya adları, kodlamalar ve sınırların ayrıntısı [Diskteki veriler](#diskteki-veriler) bölümündedir.

---

## Klavye Kısayolları

Kısayollar Windows ve Linux'ta `Ctrl` ile çalışır. Numpad `+`, `-` ve `0` tuşları ana klavye karşılıklarının eşdeğeri olarak çalışır.

| Kısayol | İşlem |
|---------|-------|
| `Ctrl+T` | Araç çubuğunu aç/kapat |
| `Ctrl+Shift+T` | Üç tema arasında geçiş yap (Koyu → Sepya → Açık) |
| `Ctrl+Shift+Y` | YOLO modunu aç/kapat |
| `Ctrl+Shift+C` | Metin rengini değiştir |
| `Ctrl++` | Font boyutunu artır |
| `Ctrl+-` | Font boyutunu azalt |
| `Ctrl+0` | Font boyutunu 16px'e sıfırla |
| `Ctrl+Z` | Geri al |
| `Ctrl+Shift+Z` | İleri al |
| `Ctrl+Y` | İleri al |
| `Ctrl+N` | Yeni belge |
| `Ctrl+O` | Dosya aç |
| `Ctrl+S` | Dosyayı kaydet |
| `Ctrl+Shift+S` | Farklı kaydet |
| `Ctrl+P` | Kaydedilmiş belgeyi yazdır |
| `Ctrl+Shift+E` | Kaydedilmiş belgeyi PDF olarak dışa aktar |
| `Ctrl+F` | Ara/değiştir kartını aç |
| `F3` | Sonraki eşleşme (sarar) |
| `Shift+F3` | Önceki eşleşme (sarar) |
| `Ctrl+H` | Ara/değiştir kartını değiştirme alanı odaklı aç |
| `F11` | Tam ekranı aç/kapat |
| `Escape` | Tam ekrandan çık |

---

## Kurulum

Kurulum paketini (Linux'ta `.deb`, Windows'ta `.msi`) ya da taşınabilir `.tar.gz` dosyasını
[GitHub Releases sayfasından](https://github.com/hyperpostulate/JTypeWriter/releases),
doğrulamak için `SHA256SUMS` dosyasıyla birlikte indirin.

- İki kanal da gömülü çalışma zamanı taşır, yani hiçbiri Java kurulumu gerektirmez.
- `.tar.gz` arşivini açıp doğrudan çalıştırırsınız: kurulum adımı ve root hakkı gerekmez.
- Kaynaktan derlemek isterseniz [Derleme ve Çalıştırma](#derleme-ve-çalıştırma) bölümüne bakın.

---

## Teknik Referans

### Gereksinimler

| Gereksinim | Sürüm |
|------------|-------|
| Paketlenmiş uygulama | Java gerekmez (gömülü JDK çalışma zamanı) |
| Kaynaktan derleme / fat JAR | Java 25+, Maven 3.8+ |

### Derleme ve Çalıştırma

```bash
git clone https://github.com/hyperpostulate/JTypeWriter.git
cd JTypeWriter
mvn clean install
```

Günlük komutlar:

```bash
mvn clean javafx:run                 # Uygulamayı derle ve başlat
mvn clean test                       # Tüm testleri çalıştır
mvn clean package                    # Derleme + test + ince JAR + fat JAR
./scripts/package.sh                 # Derleme + yerel uygulama görüntüsü (releases/<os>/)
./scripts/package.sh --skip-build    # Son derlemeyi kullan, yalnızca yeniden sar
./scripts/package.sh --installer     # Aynı + platform kurulum paketi (.deb / .msi)
./scripts/verify-artifacts.sh        # Taze derleme, ardından her kopyayı bayt bayt karşılaştır
./scripts/smoke-appimage.sh --skip-build  # Derlenen imajı başlat; .deb paketini kur, çalıştır ve kaldır
mvn test -Dtest=SessionStatsTest     # Tek test sınıfı
mvn test -Dtest=FileServiceTest      # Tek test sınıfı
```

Paketleme iki JAR üretir: ince olanı (yalnızca uygulama sınıfları) ve JavaFX'i kendi native
kütüphaneleriyle birlikte taşıyan `jar-with-dependencies`. Bu native kütüphaneler derlemenin
yapıldığı makineden geldiği için fat JAR yalnızca derlendiği platformda çalışır; `java -jar`
için bir kolaylık artefaktıdır, platformlar arası dağıtım değildir ve yerel imajın taşıdığı
şey de değildir. `scripts/package.sh` yerel bir imaj üretir ve ana makinenin platformunu
seçtiği için her hedef platformda çalıştırılması gerekir - sürüm iş akışı bunu her işletim
sistemi için bir kez çalıştırır. Geliştirme girişi `mvn javafx:run` komutudur.

Yerel imaj üç parçadan kurulur; hiçbiri fat JAR değildir:

- uygulamanın kendisi olan ince JAR;
- `jlink` ile **ölçülmüş** bir modül listesinden üretilen çalışma zamanı
  (`java.base`, `java.desktop`, `java.logging`, `java.scripting`, `jdk.unsupported`),
  böylece imaj artık derleyici, JShell, konsol ve ana makine JDK'sının JDBC/XML kuyruğunu
  taşımıyor;
- `lib/javafx` altına konan ve `--module-path`/`--add-modules` ile çözülen JavaFX modül
  jar'ları, yani JavaFX "unnamed module"dan yüklenen sınıflar değil, adlandırılmış
  modüller. Jar'lar bilinçli olarak çalışma zamanına **linklenmez**: `jlink` modüler bir
  jar'dan yalnız `lib/` ve `bin/` taşır, JavaFX ise `.so` dosyalarını jar kökünde tutar;
  linklenmiş bir JavaFX `libglassgtk3.so` bulunamadan açılırdı. GPL metni yanlarında
  `lib/javafx/legal/` altında taşınır. Bu dizini jpackage'ın `--app-content` seçeneğine
  bırakmıyoruz, script kendisi yazıyor: Linux'ta o seçenek ağacın tamamını kopyaladı,
  Windows'ta aynı çağrı `lib/javafx` içinde jar'ları olup `legal/` alt dizini olmayan bir
  imaj üretti — yani lisans Windows kurulum paketinden sessizce düştü. Başlatıcının
  seçeneği bu dizini `$APPDIR`'a **göreli** adlandırır (`$APPDIR/../javafx` Linux'ta,
  `$APPDIR/../lib/javafx` Windows'ta; ikisi de `<imaj>/lib/javafx`), `$ROOTDIR` ile
  **değil**: o makro `imageRoot`'tan genişletilir ve başlatıcı onu yalnız düz app image
  olarak çalışırken kurar; kurulu bir `.deb`'te boş genişleyip modül yolunu `/lib/javafx`
  yapıyordu.

Etki iddia değil ölçüm: uygulama imajı 147 MiB'den 73 MiB'ye, `.deb` 48 MiB'den 44 MiB'ye
indi ve açılış logundaki `Unsupported JavaFX configuration: classes were loaded from
'unnamed module'` uyarısı kayboldu.

`--installer` seçeneği bu görüntüyü Linux ve Windows için platform kurulum paketine
(`.deb`, `.msi`) dönüştürür — CI iş akışının çalıştırdığı komut da budur. Her sürüm her
platform için iki kanalı da yayımlar: kurulum paketi ve taşınabilir `.tar.gz`; hangisinin
ürün varsayılanı olacağı hâlâ açık bir karardır, yani `--installer` desteklenen yollardan
biridir, kararlaştırılmış olan değil. Uygulama sürümünün tek kaynağı `pom.xml`'dir:
script sürümü üretilen artefaktten geri okur ve jpackage kurallarına karşı doğrular.

`scripts/verify-artifacts.sh` o döngünün diğer yarısını kapatır: depodaki yükü (`LICENSE`,
`NOTICE`, `THIRD-PARTY-NOTICES`, `legal/`, ince JAR, JavaFX modül jar'ları ve her jar'ın
kendi `META-INF` girdileri) yerel imajda ve kurulum paketinde gerçekten ne varsa bayt bayt
karşılaştırır. İmajın yerleşimini de doğrular, çünkü "uygulama açılıyor" bunun kanıtı
değildir: çalışma zamanı ana makine JDK'sı değil `jlink` çıktısı olmalı ve `lib/javafx`
var olmalı. `.deb` yükü `dpkg-deb` ile açılıp aynı şekilde karşılaştırılır, çünkü
`jpackage`'ın deb paketleyicisi imaj kökündeki dosyaları düşürür ve `scripts/package.sh`
yükü sonradan tamamlar.

`scripts/smoke-appimage.sh` dosya karşılaştırmasının kanıtlayamadığını kanıtlar:
artefaktların gerçekten çalıştığını. Linux'ta uygulama imajını boş bir `user.home` ile
(`DISPLAY` yoksa `Xvfb` altında) başlatır, ana pencereyi sabit bir uyku yerine `xwininfo`
ile bekler, ardından `.deb` paketini kurar, kurulu başlatıcıyı çalıştırır ve paketi
yeniden kaldırır; root (veya parolasız `sudo`) yoksa kurulumu atlamaz: `.deb` yükünü açar
ve yükün başlatıcısını, `dpkg -S`/`-L` sorgularına açılan ağacı gösteren bir `dpkg` stub'ı
ile çalıştırır — jpackage başlatıcısını "kurulu paket" dalına sokan şey tam olarak o iki
sorgudur. Windows'ta imajı başlatır ve jpackage başlatıcısının ayakta kalmasını
şart koşar. Her başlatmada log ayrıca `unnamed module` için denetlenir: JavaFX'in
yeniden classpath'e dönmesinin tek belirtisi odur. CI'daki `smoke` işi bunu iki işletim
sistemi ayağında, derleme işinin ürettiği artefakt üzerinde çalıştırır; böylece
"paketlendi" artık test edilmeden gönderilmez.

Yanlış araç zinciri derlemeyi başlamadan durdurur: `maven-enforcer-plugin` `validate`
aşamasında çalışır, JDK'nın `pom.xml`'deki `java.version` değerinden (25) eski olmamasını
ve Maven'ın 3.8 veya üstü olmasını şart koşar, eksiği tek satırda söyler; böylece
`release 25` hakkındaki bir derleyici hatasının peşine düşmezsiniz.

Başlatıcı, pencere ve görev çubuğu simgesi, görselin tek kaynağı olan
`src/main/resources/icons/jtypewriter.svg` dosyasından üretilir: `scripts/generate-icons.py`
PNG setini ve Windows `.ico` dosyasını `icons.manifest` ile birlikte yeniden
oluşturur; `scripts/package.sh` platforma göre doğru biçimi seçip `jpackage`'a
aktarır. `--check` her varlığın özetini manifesttekiyle karşılaştırır; asıl
kırılma noktası olan "görsel düzenlendi, script çalıştırılmadı" durumunu her
makinede yakalar, çünkü hiçbir rasterleştiriciye ihtiyaç duymaz. `--render` daha
güçlü, makineye özgü kanıttır: SVG'yi yeniden rasterleştirip pikselleri depodaki
varlıklarla karşılaştırır (tam eşleşme %0,00 fark; kasıtlı bir palet değişikliği
%0,33 ile yine kırmızıya döner). Script, ImageMagick 7'nin `magick` sürücüsünü de
eski sürüm 6'nın `convert` komutunu da kabul eder; böylece yeniden üretim hem
geliştirici makinesinde hem ImageMagick 6 paketleyen bir çalıştırıcıda çalışır.

### Tema ve dil kaynakları

CSS dosyaları `src/main/resources/css/` konumundadır:

| Tema | Dosya | Açıklama |
|------|-------|----------|
| Temel | `base.css` | Tüm temalarda paylaşılan stiller |
| Koyu | `dark.css` | Koyu arka plan, açık metin |
| Açık | `light.css` | Açık arka plan, koyu metin |
| Sepya | `sepia.css` | Sıcak sepya tonlu arka plan |

Yerelleştirilmiş metin, `src/main/resources/` altında dil başına bir pakette durur:

| Dil | Dosya | Kod |
|-----|-------|-----|
| İngilizce | `messages.properties` | `EN` |
| Türkçe | `messages_tr.properties` | `TR` |
| Almanca | `messages_de.properties` | `DE` |
| Fransızca | `messages_fr.properties` | `FR` |
| İtalyanca | `messages_it.properties` | `IT` |
| İspanyolca | `messages_es.properties` | `ES` |

### Diskteki veriler

JTypeWriter'ın kendi verisi `~/.jtypewriter/` altındadır:

| Yol | İçerik |
|-----|--------|
| `settings.properties` | dil, tema, metin rengi, yazı boyutu, ses düzeyi, araç çubuğu görünürlüğü, durum çubuğu boyutu, pencere geometrisi, son belge ve klasör (mutlak yollar), son belgenin imleç/scroll konumu ve `schema.version` |
| `recovery/draft-<uuid>.jtw` | etkin oturumun kaydedilmemiş taslağı |
| `recovery/pending-<uuid>.jtw` | kullanıcının "sonra" dediği, bir sonraki açılış için saklanan taslak |
| `logs/jtypewriter-0.log` .. `-2.log` | her biri bir megabayta kadar, dönen üç kuşak |
| `instance.lock` | belge içeriği taşımayan danışma amaçlı tek örnek kilidi |

- Taslak, küçük bir başlık ile metinden oluşur. Başlık, metnin geldiği mutlak yolu Base64 ile kodlanmış olarak tutar; böylece ayırıcılar ve satır sonları bozulmaz - bu bir kodlama, şifreleme değil - yani taslak, geldiği belgeden daha uzun yaşayabilir.
- Kurtarma taraması yalnızca düz dosyaları okur: taslak gibi adlandırılmış bir sembolik bağ izlenmez, yok sayılır; 64 MiB'ın üzerindeki bir dosya başlangıçta yüklenmek yerine atlanır.
- Park edilen taslakların süresi 30 gün sonra dolar ve park edilmiş küme 10 dosya / 64 MiB ile sınırlıdır; etkin bir `draft-<uuid>.jtw` asla süresi dolmaz, çünkü başka bir çalışan örneğe ait olabilir.
- Ayrıştırılamayan bir ayar dosyası `settings.properties.corrupt` olarak kenara alınır ve tanılama için saklanır.

### Mimari

```
org.mesutormanli.jtypewriter
├── audio/
│   ├── TypewriterSound.java              # Daktilo tuş sesi oynatma
│   ├── SoundLevel.java                   # Görünüm > Ses arkasındaki düzey merdiveni
│   └── SoundPolicy.java                  # Hangi tuşların takırdadığı (saf)
├── locale/
│   ├── Language.java                     # Desteklenen diller enum'u
│   ├── LocaleManager.java                # Dil değiştirme + dinleyici desteği
│   └── Messages.java                     # ResourceBundle tabanlı i18n erişici
├── print/
│   ├── DocumentLayout.java               # Yazdırma ve PDF için tek sayfalama modeli (saf)
│   ├── PageGeometry.java                 # Nokta cinsinden sayfa boyutu ve kenar boşlukları (saf değer)
│   ├── PdfExport.java                    # Gömülü mono yüzle bağımlılıksız PDF yazıcısı
│   ├── PdfWriter.java                    # PDF nesneleri, akışlar ve xref tablosu
│   ├── Printing.java                     # PrinterJob çıktısı: biçemsiz beyaz sayfada siyah 12 punto
│   └── TrueTypeFont.java                 # Asgari TTF okuyucusu: metrikler, cmap, glif kimlikleri
├── service/
│   ├── AutoSaveService.java              # Dönemsel taslak kaydı + kurtarma teklifi
│   ├── AutosaveHealth.java               # Yapışkan otomatik kayıt hata durumu (DRAFT_WRITE / DRAFT_PARK / DRAFT_DELETE)
│   ├── BackupKeeper.java                 # Yazımdan önce dönen .bak yedekleri
│   ├── DialogService.java                # FileChooser ve Alert dialog yönetimi
│   ├── ExitDisposition.java              # Kurtarma taslağı için çıkış kuralı (CLEAR / PARK / KEEP)
│   ├── ExportService.java                # Yazdırma/dışa aktarma için kayıt kapısı + PDF yazımı
│   ├── FileService.java                  # Atomik dosya G/Ç (kodlama korumalı)
│   ├── RecoveryChoice.java               # Kurtarma istemine yanıt (geri yükle / sil / sonra sor)
│   ├── RecoveryRetention.java            # Park edilen taslaklar için yaş/adet/boyut politikası (saf)
│   ├── RecoveryStore.java                # Çökme kurtarma taslak deposu
│   ├── RecoveryTargetPath.java           # Taslağın iddia ettiği orijinal yolun doğrulaması (saf)
│   ├── StorageFootprint.java             # Kaydedilmiş bir belgenin disk maliyeti (saf)
│   └── TextCodec.java                    # Kodlama tespiti + kayıpsız gidiş-dönüş
├── stats/
│   ├── SessionStats.java                 # Yazma oturumu istatistikleri (WPM, sayımlar)
│   └── TextChange.java                   # Editörün gördüğü tek değişiklik (saf değer)
├── ui/
│   ├── MainController.java               # Kök yerleşim denetleyicisi
│   ├── MainMenu.java                     # EditorAction'dan üretilen Dosya/Düzen/Görünüm/Yardım menü düğmesi
│   ├── MainStage.java                    # Sahne yapılandırması (tam ekran, kapatma)
│   ├── AppIcons.java                     # /icons/ altından pencere/görev çubuğu ikonları
│   ├── AppFonts.java                     # Paketlenen arayüz fontu + başsız ölçüm yüzü
│   ├── WelcomeCard.java                  # Hoş geldin kartı: kısayollar + karşılama opt-out'u
│   ├── RecentFilesCard.java              # Son dosyalar kartı (Dosya menüsünden açılır)
│   ├── AboutCard.java                    # Yazar bilgisiyle hakkında kartı
│   ├── EditorAction.java                 # Komut envanteri: komut başına etiket, kısayol ve etki
│   ├── KeyboardShortcutHandler.java      # Merkezi klavye kısayolu yönetimi
│   ├── CrashHandler.java                 # Global çökme ağı: log + hata diyaloğu
│   ├── DropHandler.java                  # Sahne düzeyinde dosya bırakma tutkalı (çöz, aç, bildir)
│   ├── DroppedFiles.java                 # Bırakma kararı: tek okunabilir .txt/.md (saf)
│   ├── FindCommands.java                 # Komut envanterinin açtığı arama/değiştirme portu
│   ├── FindModel.java                    # Arama/değiştirme taraması, imleç ve değiştirme kararları (saf)
│   ├── FindPanel.java                    # Editörün üzerinde yüzen arama/değiştirme kartı
│   ├── FindReplacePolicy.java            # Bir değiştirmenin YOLO onayı gerektirip gerektirmediği (saf)
│   ├── FindScanService.java              # Kendi daemon işçisinde arka plan arama taraması
│   ├── OverlayCard.java                  # Metnin üzerinde yüzen kartların taban sınıfı
│   ├── OverlaySlot.java                  # Aynı anda en fazla bir yüzen kart görünür
│   ├── ShortcutRows.java                 # EditorAction'dan üretilen kısayol kartı satırları
│   ├── ShortcutTable.java                # Kartların paylaştığı iki sütunlu kısayol ızgarası
│   ├── SingleInstanceWarning.java        # "Zaten çalışıyor" uyarı penceresi
│   ├── DialogButtons.java                # Uygulamanın Alert'leri için özel düğmeler
│   ├── DialogSizing.java                 # Alert boyutlandırma matematiği (saf)
│   ├── DialogStyling.java                # Alert diyalog paneline tema stil sayfaları
│   ├── component/
│   │   ├── EditorArea.java               # YOLO politikasını uygulayan TextArea
│   │   ├── YoloPolicy.java               # YOLO modu düzenleme politikası (saf mantık)
│   │   ├── EditorKeyHandler.java         # Tuş vuruşu sesi + istatistik takibi
│   │   ├── StatsBar.java                 # Alt durum çubuğu (saat + istatistikler)
│   │   ├── StatsBarModel.java            # Durum çubuğu metin kompozisyonu (JavaFX'siz)
│   │   ├── StatsBarState.java            # Kalıcı durum çubuğu boyutu (Node olmayan durum)
│   │   ├── ToolbarView.java              # Üst araç çubuğu (dosya/düzenleme düğmeleri + ayar açılır listeleri)
│   │   ├── ToolbarFit.java               # Araç çubuğu taşma politikası (saf, başsız ölçüm)
│   │   └── ToolbarState.java             # YOLO modu + ses aç/kapa durumu
│   └── theme/
│       ├── Theme.java                    # Tema enum'u (Açık/Koyu/Sepya)
│       ├── ThemeManager.java             # CSS stil sayfası yükleme ve değiştirme
│       ├── FontSizeManager.java          # Editör font boyutu durumu
│       ├── TextColorManager.java         # Metin rengi döngüsü durumu
│       └── TextColor.java                # CSS değerleriyle metin rengi enum'u
├── JTypeWriterApp.java                   # Ana giriş noktası
├── AppDirs.java                          # Kullanıcı veri/log/kurtarma/ayar dizinleri
├── AppGraph.java                         # Kompozisyon kökü (manuel bağımlılık enjeksiyonu)
├── AppLogging.java                       # Dönen log kurulumu (java.util.logging)
├── AppSettings.java                      # Kalıcı tercihler (settings.properties)
├── AppVersion.java                       # pom.xml'den derleme anında filtrelenen uygulama sürümü
├── AtomicFiles.java                      # Dayanıklı atomik dosya yazımı (fsync + rename)
├── InstanceLock.java                     # Kullanıcı başına örnek kilidi (tek örnek)
├── IoExecutor.java                       # Bloklayan dosya G/Ç için tek geçit
├── RecentFiles.java                      # Karşılama kartının son dosya listesi (saf)
├── StartupArguments.java                 # CLI/dosya ilişkilendirme argümanını çözer (saf)
└── FxApplication.java                    # JavaFX yaşam döngüsü köprüsü
```

#### Temel Bileşenler

| Bileşen | Açıklama |
|---------|----------|
| `EditorArea` | Her düzenlemeyi YOLO politikasından geçiren TextArea alt sınıfı; belgeleri `setDocument` ile yükler |
| `YoloPolicy` | YOLO modu düzenleme politikası (silmeyi engeller, eklemeye izin verir) |
| `DroppedFiles` | Dosya bırakmanın arkasındaki saf karar: tam olarak bir okunabilir `.txt`/`.md` açılacak belge olur; dosya olmaması, birden çok dosya, başka bir tür, bir klasör ya da kaybolmuş yol arayüz dilinde bir ret bildirimine dönüşür |
| `DropHandler` | Sahne düzeyinde sürükleme tutkalı: jesti olay filtresi olarak kabul eder (böylece metin alanı onu sürüklenen metin sanamaz), `DroppedFiles` ile çözer ve belgeyi Aç ile aynı kaydedilmemiş değişiklik kapısından geçirerek açar |
| `FindModel` | Saf ara/değiştir modeli: düz metin (regex değil) ve çakışmasız eşleşmeler, varsayılan yerel ayara bağlı olmayan büyük-küçük harf duyarlılığı, iki yönde saran ileri/geri, düzenlemeden sonra yeniden tarayan tek/tümünü değiştir; artık belgeye sığmayan aralık uygulanmaz, reddedilir |
| `StartupArguments` | `JTypeWriter <dosya>` arkasındaki süreç argümanının saf çözümü: normal bir dosya açılacak belge olur; olmayan yol, dizin, çözülemeyen yol ya da birden çok argüman sessizce boş bir düzenleyici yerine arayüz dilinde bir ret bildirimi olur; sembolik bağlantı kendi adını korur |
| `EditorKeyHandler` | Tuş vuruşu ses efektleri ve oturum istatistiklerini yönetir |
| `StatsBar` | Saati ve *kaydedildi ...* onayını arayüz dilinin tarih/saat biçimleriyle alt çubukta gösterir; otomatik kayıt hatasını adlandıran kalıcı bir uyarı gösterir, eski taslak için ayrı metin kullanır ve yazı boyutu çift tıklamayla döner |
| `KeyboardShortcutHandler` | Merkezi klavye kısayolu yönlendirmesi |
| `MainMenu` | Araç çubuğundaki Dosya/Düzen/Görünüm/Yardım komut menüsü; girdileri ve hızlandırıcıları `EditorAction`'dan üretilir |
| `ToolbarView` | Tüm editör kontrollerini içeren üst araç çubuğu |
| `ToolbarFit` | Araç çubuğu taşma politikası: başlıklı satır sığmadığında açılır liste başlıklarını değere indiren saf, JavaFX'siz ölçüm |
| `ToolbarState` | YOLO modu ve ses aç/kapa durumunu yönetir |
| `StatsBarState` | Kalıcı durum çubuğu boyutu; nötr `AppSettings.StatsBarSize` üzerinden Node olmayan durum |
| `AppIcons` | `/icons/` altından yüklenen pencere ve görev çubuğu ikon seti; eksik varlık açılışı bozmaz |
| `FileService` | Atomik dosya G/Ç (geçici dosya + fsync + taşıma), kodlama korumalı |
| `DialogService` | UI dialog yönetimi (FileChooser, Alert) |
| `TextCodec` | Kodlama tespiti (BOM/UTF-8), baytları `U+FFFD` ile değiştirmeyi reddeden sıkı çözme ve kayıpsız gidiş-dönüşler |
| `BackupKeeper` | Yazımdan önce alınan dönen `.bak` yedekleri |
| `RecoveryStore` | Çökme kurtarma taslakları: `~/.jtypewriter/recovery/` altında oturum başına tek atomik `draft-<uuid>.jtw` dosyası; ayrıca "sonra sor" denen taslak için `pending-<uuid>.jtw`; başarısız bir silme `WARNING` düzeyinde bildirilir ve `false` döner |
| `RecoveryChoice` | Kurtarma istemine yanıt: geri yükle, sil veya sonra sor; `LATER` hiçbir şeyi silmeyen ve geri yüklemeyen güvenli varsayılan |
| `ExitDisposition` | Kurtarma taslağı için saf çıkış matrisi: kapatma isteği kapatma yönünde yanıtlandığında `CLEAR`, sorulmamış çıkışta `PARK`, ertelenen taslak park edilemediğinde `KEEP`; dondurulmuş slot diğer tüm sinyallerin önüne geçer |
| `AutosaveHealth` | JavaFX'siz, yapışkan otomatik kayıt hata durumu (`DRAFT_WRITE` / `DRAFT_PARK` / `DRAFT_DELETE`); dinleyiciler yalnızca gerçek geçişlerde tetiklenir |
| `AutoSaveService` | Dönemsel taslak anlık görüntüleri, açılışta kurtarma teklifi, taslağı yalnızca yanıtlanmış bir kapatma isteminden sonra silen çıkış kuralı, otomatik kayıt hatalarının `AutosaveHealth`'e bildirilmesi ve bayat taslak yol koruması: belgesinden daha eski bir taslak, daha yeni dosyanın üzerine yazılmaması için yolu olmadan geri yüklenir |
| `CrashHandler` | Global çökme ağı: log + kullanıcıya hata diyaloğu |
| `InstanceLock` | İkinci örneği kullanıcı başına reddeden danışma amaçlı dosya kilidi |
| `SingleInstanceWarning` | "Zaten çalışıyor" uyarısını gösterip kapanan minimal pencere |
| `AppSettings` | `~/.jtypewriter/settings.properties` altında kalıcı tercihler |
| `AppVersion` | Uygulama sürümünü `pom.xml`'den alır ve filtrelenmiş bir kaynak olarak taşır; böylece Hakkında, ayar şema sürümü ve yayın tag'i tek bir numaradır |
| `AtomicFiles` | Dayanıklı atomik dosya yazımı (fsync, rename, dizin flush) |
| `ThemeManager` | CSS tema yükleme ve değiştirme |
| `FontSizeManager` | Editör font boyutu durumu (10-48px) |
| `TextColorManager` | Metin rengi döngüsü durumu |
| `SessionStats` | Gerçek zamanlı yazma istatistikleri (WPM, kelime/karakter/satır sayısı) |
| `TypewriterSound` | Daktilo tuş sesi oynatma |
| `SoundLevel` | `Görünüm > Ses` düzey merdiveni; `OFF` kalıcı sessizliktir |
| `SoundPolicy` | Hangi tuşların takırdadığına dair saf karar; tuş işleyicisi ve testleri paylaşır |
| `LocaleManager` | Değişiklik dinleyicileriyle çoklu dil desteği |
| `Messages` | Resource bundle tabanlı mesaj erişici |

#### Bağımlılıklar

| Bağımlılık | Sürüm | Amaç |
|------------|-------|------|
| JavaFX Controls | 26.0.1 | UI bileşenleri |
| JavaFX FXML | 26.0.1 | FXML yükleme |
| JavaFX Media | 26.0.1 | Ses oynatma |
| JUnit Jupiter | 6.0.3 | Test çerçevesi |
| Mockito | 5.23.0 | Test mock'lama |

JavaFX Graphics ve Base, Controls ile dolaylı olarak gelir. Bağımlılık enjeksiyonu framework'süzdür: nesne grafiğini `AppGraph` elle kurar.

### Testler

#### Test Çalıştırma

```bash
# Tüm testler
mvn test

# Tek test sınıfı
mvn test -Dtest=SessionStatsTest

# Tek test metodu
mvn test -Dtest=SessionStatsTest#wordCountIsCorrect
```

#### Test Sınıfları

| Test Sınıfı | Kapsam |
|-------------|--------|
| `SessionStatsTest` | Kelime/karakter/satır sayısı, tuş vuruşları, sıfırlama, aktif yazım süreli WPM (durağanlık bozulması regresyonu) |
| `FileServiceTest` | Açık sırasında ve yeni belgede kaydedilmemiş değişiklik akışı (iptal/atla/önce kaydet), atomik yazım, dönen yedekler, kodlama gidiş-dönüşü (temsil edilemeyen karakterler için UTF-8'e geçiş), yeni belgede kodlama sıfırlama, yaşam döngüsü dinleyicileri, kurtarma bağlama ve yazdırma/dışa aktarma kapısı: geri verilen metin dosyadan okunur (editör tamponundan asla), iptal edilen kayıt hiçbir şey döndürmez, temiz belge istem göstermeden okunur ve okuma hatası yazdırmak yerine bildirilir |
| `ExportServiceTest` | Yazdırma ve dışa aktarmanın kullanıcıya dönük yarısı: boş kapı hiçbir şey yazmaz ve seçici açmaz, seçilen hedef tam olarak çizicinin baytlarını alır, çizici hatası uyarı yoluyla bildirilir ve yarım dosya bırakmaz, PDF diyaloğuna ancak kayıt kapısı bir dosya ürettikten sonra ulaşılır |
| `TextCodecTest` | Kodlama tespiti (BOM, sıkı UTF-8), eski kodlama sorusu, kayıpsız kodlama gidiş-dönüşü, sıkı kodlama (temsil edilemeyen karakterler `?` olmak yerine hata verir) |
| `EncodingMatrixTest` | Her kodlama için aç→kaydet ve aç→düzenle→kaydet bayt matrisi (BOM'suz UTF-16LE/BE, eski kodlama sorusu üzerinden Windows-1254, CRLF, sekme ve emoji içeren karışık UTF-8); iki paritede de NUL içeren geçerli UTF-8 dosyasının UTF-8 kalmasını ve BOM'suz UTF-16'nın eski kodlama sorusuna asla ulaşmamasını sabitler |
| `BackupKeeperTest` | Yazımdan önce yedek rotasyonu ve saklama |
| `RecoveryStoreTest` | Çökme kurtarma taslağı yaşam döngüsü (kaydet/geri yükle/sil); oturum başına tek atomik `.jtw` dosyası, en yeni taslağın devralınması, eşzamanlı oturumların izolasyonu, ertelenen taslağın ayrı dosyaya park edilip aktif slotun serbest bırakılması, bozuk başlık, hatalı zaman damgası, kullanılamayan yol ve geçersiz UTF-8'e tolerans; salt okunur bir dizin `clear()` çağrısının başarısız olduğunu bildirmesini ve `WARNING` loglamasını sağlar |
| `RecoveryRetentionTest` | Park edilen taslak temizliği kuralı saf fonksiyon olarak: yaş, adet ve boyut bütçeleri, tek başına bütçeyi aşan en yeni girdinin korunması ve aşırı büyük bir girdinin arkasındaki daha küçüğü silmemesi |
| `AutoSaveServiceTest` | Belge açıldığında veya yeni belge oluşturulduğunda önceki belgenin bayat kurtarma taslağının silinmesi; ertelenen taslağın çıkışta korunması ve yeni belgenin otomatik kaydını artık susturmaması (regresyon); yanıtlanmış çıkışın taslağı silmesi, sorulmamış çıkışın taslağı park etmesi, dondurulmuş slotun dosyayı olduğu gibi koruması ve başarısız anlık görüntü veya park işlemlerinin sağlık durumunu bildirmesi; başarısız bir silmenin uyarıyı diyalog olmadan koruması ve sonraki başarılı silmenin uyarıyı kaldırması; diskteki belgeden daha eski bir taslağın yolu olmadan geri yüklenmesi |
| `ExitDispositionTest` | Dört yönlü çıkış matrisi: canlı ve dondurulmuş slot karşısında yanıtlanmış ve sorulmamış çıkış; dondurulmuş slot diğer tüm sinyallerin önüne geçer |
| `DroppedFilesTest` | Bırakma kararı: `null`/boş dosya listesi, tek dosya, `.md`, büyük harfli uzantı, birden çok dosya (sayıyla reddedilir), PDF ve uzantısız dosya, bırakmadan önce kaybolmuş yol, `*.txt` adlı bir klasör, adını koruyan symlink ve yalnız aynı harflerle biten adları reddeden sonek kontrolü |
| `FindModelTest` | Ara/değiştir modeli: düz sorgu ve değiştirme, çakışmasız tarama, Türkçe varsayılan altında büyük-küçük harf, iki yönde sarma, aramanın ilk eşleşmeden önce yeniden başlaması, eski ya da taşan bir arka plan taramasının atılması, tümünü değiştirmenin kendi çıktısını yeniden taramaması, tek değiştirmenin eklenen metnin ötesindeki sonraki eşleşmeyi seçmesi ve belge sonunu aşan aralığın reddedilmesi |
| `DropContractTest` | Bırakma için kaynak düzeyinde sözleşme: `MainStage` onu kuruyor, tüm sahnede `DRAG_OVER`/`DRAG_DROPPED` filtreliyor, yalnız `FileService.openPathAsync` üzerinden açıyor (ikinci kaydetme sorusu ve doğrudan kaydetme yok), yüklenen metni düzenleyiciye veriyor ve `FileService` dışında hiçbir kaynak kaydedilmemiş değişiklik sorusunu sormuyor |
| `StartupArgumentsTest` | `JTypeWriter <dosya>` kararı: argümansız/boş argüman, var olan normal dosya, göreli yol, olmayan yol, dizin, iki argüman, adını koruyan symlink ve platformun yol olarak çözemediği metin |
| `AutosaveHealthTest` | Yapışkan hata durumu (`DRAFT_WRITE` / `DRAFT_PARK` / `DRAFT_DELETE`); tekrarlanan hatalar boyunca korunur, başarılı bir yazımda temizlenir ve her gerçek geçişte tek bir bildirim gönderir |
| `ProcessCrashRecoveryTest` | Yalnızca Linux'ta çalışan uçtan uca öldürme testi: `CrashHarness` (test sınıfı değil, yardımcı) alt JVM'de JavaFX'i başlatmadan gerçek servis grafiğini kurar ve kirli bir belgenin anlık görüntüsünü üretim otomatik kayıt yoluyla alır; ardından üst süreç onu `SIGKILL` ile (`Process.destroyForcibly`) öldürür ve ikinci bir JVM tam olarak aynı ASCII dışı metni kurtarmak zorundadır. Eşleştirilmiş bir durum, yanıtlanmış bir kapatmanın taslağı yine de sildiğini kanıtlar; `-Duser.home` ise loglamayı geçici bir ev dizininde tutar. Eşzamanlama `Thread.sleep` değil, stdout işareti ve `waitFor` ile yapılır |
| `YoloPolicyTest` | YOLO modu düzenleme politikası (silme engelli, ekleme serbest, güvenli değiştirme) |
| `FindReplacePolicyTest` | Bir değiştirmenin ne zaman kullanıcı onayı istediği: YOLO açıkken sorar, kapalıyken hiçbir şey sormaz |
| `FindScanServiceTest` | Taramanın FX thread'i dışında koştuğu ve sonucun yalnızca enjekte edilen FX dağıtıcısından geldiği (`fxThreadScans()` sıfır kalır) |
| `KeyboardShortcutHandlerTest` | Kısayol çözümleme; shift hassas birleşimler (Ctrl+Shift+Y / Z / C / T / S), yeni belge için Ctrl+N ve numpad eşdeğerleri |
| `MainControllerTest` | Pencere başlığı oluşturma (uygulama + belge adı, kaydedilmemiş `*` işareti, `[YOLO]` rozeti), Düzen menüsündeki ara/değiştir komutlarının sırası ve komut menüsü sözleşmesi: `MainMenu.layout()` dört sayfada her platformdan bağımsız `EditorAction`'ı tam bir kez taşıyor (altı ayar komutu yerine değer alt menüsüyle temsil ediliyor), Görünüm alt menüleri araç çubuğu açılır listelerinin sunduğu aynı değerleri listeliyor ve işaretliyor, her sayfa başlığı ve satır etiketi altı dilde dolu |
| `MainStageTest` | Ekran dışı pencere konumu koruması (`intersectsAny`: artık bağlı olmayan monitörlerdeki konumlar) |
| `AppIconsTest` | Classpath'teki ikon seti: her PNG bildirilen boyutta çözülüyor, `.ico` dosyası beklenen sihirli baytlarla başlıyor (saf bayt kontrolü, toolkit yok) |
| `AppVersionTest` | Sürüm tek sayıdır: filtrelenen kaynak okunup kırpılır, eksik ya da boş değer hata vermek yerine boş döner, `get()` hiçbir zaman boş değildir ve her okuyucu (Hakkında, ayar şeması) `pom.xml` ile aynı fikirdedir |
| `AppSettingsTest` | Tercihlerin yeniden başlatmada korunması, geri yüklenen araç çubuğu/tam ekran/durum çubuğu boyutu durumu ve bozuk değerlerde varsayılana düşmesi, varsayılanlar, bilinmeyen değerlere ve kullanılamayan yollara tolerans, bozuk ayar dosyasının sessizce ezilmek yerine karantinaya alınması, ayar şema sürümü, ses düzeyi (eski aç/kapat anahtarının düzey olarak okunması dahil), son belgenin imleç/scroll konumu ve dosyaya bağlılığı, son dosya listesi ve karşılama opt-out'u |
| `AppDirsTest` | Veri dizini yerleşimi: ayarlanmamış ya da boş `user.home` hata vermek yerine mutlak, geçici dizin tabanlı bir veri dizinine düşer ve gerçek bir ev dizini `<home>/.jtypewriter` ile altında `recovery/` ve `logs/` verir |
| `StartupGreetingTest` | Karşılama kartının kendi kendine ne zaman gelebileceği: taze kurulum karşılanır, saklanan opt-out kartı uzak tutar, komut satırında adlandırılan belge asla kartın altında kalmaz |
| `RecentFilesCardTest` | Son dosya satırının adlandırılması: dosya adı, son öğesi olmayan bir yolda tam yol |
| `RecentFilesTest` | Son dosyalar kartının belge listesi saf liste işlemleri olarak: en yeni başta, mutlak ve normalleştirilmiş yolla belge başına tek satır, on girişlik tavan en eskiyi düşürüyor, kaybolan belgeler süzülüyor, boş/`null` durumlar ele alınıyor |
| `StatsBarStateTest` | Kalıcı durum çubuğu boyutu: ayardan başlar, yeni kurulum orta boyutta başlar, her döngü adımı kalıcıdır, bozuk değer orta boyuta düşer, eşleme her iki enum'u iki yönde kapsar |
| `StatsBarModelTest` | Durum çubuğunun metin mantığı, başsız: kurulumdan hemen sonra doludur, yenileme yeni istatistikleri alır, dil değişimi bildirmeden önce yeniler ve saati de uyarıyı da yeniden çizer, her otomatik kayıt hata türü kendi uyarısını gösterir |
| `FontSizeManagerTest` | Yazı boyutu kademeleri, kademede olmayan boyutun ayarı değiştirmeden listeye eklenmesi, Ctrl+± için kademe adımlama, desteklenen aralığa kısıtlama ve varsayılan boyuta kalıcı sıfırlama |
| `ToolbarOptionListsTest` | Altı araç çubuğu açılır listesi: seçenek sırası, yinelenen seçenek olmaması ve her listenin kendi güncel değerini gösterebilmesi |
| `AtomicFilesTest` | Dayanıklı atomik yazım: içerik bütünlüğü, artık temp dosyası yok, hata sonrası temizlik |
| `MessagesTest` | 6 dilin tamamında i18n anahtar eşitliği, yerelleştirilmiş tema/renk adları, sistem dilinden bağımsız bundle çözümlemesi |
| `DialogServiceTest` | "Farklı Kaydet" dosya adlandırma kuralları (uzantı işleme) |
| `DialogButtonsTest` | Paylaşılan uyarı düğmeleri: birincil Tamam/Bitti, iptal İptal/Kapat, alternatif üçüncü eylem, düğme metni birebir korunur, tek varsayılan ve tek iptal vardır ve kurtarma diyaloğu İptal'i Atla'ya değil Sonra'ya koyar |
| `DialogSizingTest` | Uyarı boyutlandırma matematiği: sığan tercih edilen boyut değişmeden döner, 768 px'lik ekrana sıkıştırılır, aşırı küçük ekranda tabana iner, kenar boşlukları simetriktir ve paylaşılan içerik sütunu diyalog çerçevesine yer bırakır |
| `DialogStylingTest` | Her uyarı temalıdır: tema başına stil sayfası sırası (önce base, sonra light/dark/sepia), listelenen her stil sayfası classpath'te çözülür, `base.css` diyalog panelini temalar ve her tema diyalog panellerinin okuduğu belirteçleri tanımlar |
| `JTypeWriterAppTests` | Ana metod varlığı |
| `AppLoggingTest` | Log biçimlendiricisinin başlık satırını hata vermeden üretmesi (regresyon) ve fırlatılan istisnalar için neden zinciriyle birlikte girintili yığın izi eklemesi; geçici dizinde log dosyası işleyicisi, kullanılamayan log dizini ve reddedilen ev dizininin yalnızca konsola düşmesi, tekrarlanan `init()` çağrısının işleyici biriktirmemesi |
| `InstanceLockTest` | Örnek kilidinin alınması, aynı JVM'de ikinci alımın reddedilmesi, bırakma ve yinelenebilir kapatma, kullanılamayan kilit dosyasında fail-open davranışı ve salt okunur bir veri dizininin açılışı engellemek yerine `UNAVAILABLE` bildirmesi |
| `LanguageTest` | Yerel dil adlarının her sistem dilinde sabit kod sabitleri olması (`getDisplayLanguage` tuzağı yok) |
| `ThemeCycleTest` | Tema döngüsü: enum sırası araç çubuğuyla aynıdır, varsayılan Koyu'dur, `next` başa döner ve her paket döngüyü varsayılandan başlayarak belgeler |
| `ToolbarStateTest` | Ses düzeyinin tek ayarlayıcısı: yeniden başlatmada kalıcılık, ses motorunun takip etmesi, sıfırın ikisini de susturması, Açık'ın son duyulur kademeyi geri getirmesi, tekrarların etkisiz olması |
| `ToolbarFitTest` | Araç çubuğu taşma politikası: başlıklı satırın (düğmeler, açılır listeler, ayırıcı, çerçeve) enjekte edilen ölçülerle nasıl hesaplandığı, FULL→COMPACT geçişi ve 40 px histerezisi; ayrıca Almanca ve Fransızcanın 1280 px'te kırpılmadığını, daraltılmış satırın altı dilde 900 px'e sığdığını ve pencerenin minimum genişliğinin altı pakette ölçülen en geniş daraltılmış satır olduğunu kanıtlayan kabul testleri |
| `AppFontsTest` | Paketlenen fontlar birer sürüm varlığıdır: arayüz yüzleri ve monospace yüz, `base.css`'in adlandırdığı aileler altında (sırasıyla `.root` ve `.editor-area`) sınıf yolunda, altı dilin araç çubuğu etiketlerini (`☰` menü glifi dâhil) çizebiliyor, monospace yüz gerçekten eşaralıklı ve sabitlenmiş bir metin sabitlenmiş bir genişlik ölçüyor; böylece sistem fontuna sessiz dönüş derlemeyi düşürüyor |
| `DocumentLayoutTest` | Yazdırma ve PDF'in paylaştığı tek sayfalama modeli: ölçülen genişlikte açgözlü sarma, açık ve sondaki satır sonları, sekmelerin dört hücrelik duraklara açılması, metin bloğundan geniş tek parçanın vekil çiftleri bozulmadan kod noktası sınırlarından kırılması, tam sığan satırın bölünmemesi, taban çizgisi aritmetiği ve her sayfada sıfırlanması, yeni sayfaya taşma, boş belgenin tek boş sayfa olarak yerleştirilmesi ve `PageGeometry`'nin kenar boşluğu hesabı (yer bırakmayan geometri dâhil) |
| `PdfExportTest` | PDF yazıcısı, ürettiği dosyanın kurallarına karşı: başlık ve sonlandırıcı, her xref ofsetinin kendi `<n> 0 obj` kaydına düşmesi, her akışın `/Length` değerinin baytlarıyla eşleşmesi (Flate akışları açılarak), A4 `/MediaBox`, sayfalama sayısı kadar sayfa, gömülü `/FontFile2`'nin paketlenmiş monospace TTF'ye bayt bayt açılması, `/ToUnicode`'un glifleri kod noktalarına geri eşlemesi (Türkçe karakterler dâhil), her içerik akışında beyaz sayfanın ve siyah metnin boyanması ve aynı metnin iki kez çiziminin bayt bayt aynı olması |
| `TrueTypeFontTest` | Dışa aktarma ve yazdırmanın arkasındaki asgari TTF okuyucusu: `unitsPerEm`, pozitif glif sayısı, 'A' ve 'ş' harflerinin gerçek gliflere eşlenmesi, 'i' ile 'W' ilerlemelerinin eşitliğiyle yüzün eşaralıklı olduğunun kanıtlanması, pozitif yükseliş ve font olmayan baytların çöp olarak ayrıştırılmak yerine reddedilmesi |
| `PackagingNoticesTest` | Paketleme sözleşmesi: `META-INF/THIRD-PARTY-NOTICES` ve üç lisans metni sınıf yolunda; bildirim her paketlenen bileşeni gerçek SPDX kimliğiyle adlandırıyor, FFmpeg'in dağıtılmadığını söylüyor, 100 sütun içinde yalnızca LF kullanıyor ve depodaki dosyayla bayt-özdeş, böylece bayat bir derleme çıktısı testi geçemiyor |
| `ContrastRatioTest` | WCAG kontrast matematiği (siyah-beyaz 21:1, aynı renkler 1:1, 4.5 sınırı) ve durum metni, otomatik kayıt uyarısı, araç çubuğu metni, düz popup satırı ya da varsayılan editör metni AA'nın veya 13 px'in altına düşerse (ya da bir tema `base.css`'in hiç okumadığı bir token tanımlarsa), odak halkası 3:1'in altına düşerse, vurgulanan popup satırı metni AA'nın altına düşerse, popup hücresi yazı boyutu kapalı combo'nun boyutundan saparsa, odak rengi yeniden saydamlaşırsa ya da var olmayan bir JavaFX CSS özelliği (`-fx-letter-spacing`, `-fx-transition`) geri dönerse derlemeyi başarısız kılan CSS ayrıştırma korumaları |
| `CiGatesTest` | CI kapı sözleşmesi: ortak iş akışı ikon güncellik kontrolünü ve artefakt doğrulamasını gerçekten çağırıyor, ikon kontrolü derlemeden önce ve yeniden rasterleştirmeden iki işletim sistemi ayağında koşuyor, artefakt kontrolü paketlemeden sonra koşuyor, artefakt kapısı Linux ayağında kalıyor (`.deb` paketini açıyor), `maven.yml` ile `release.yml` kendi adım listelerini tutmak yerine ortak iş akışını çağırıyor ve yayın yalnız normalleştirilmiş `dist/` varlıklarını indiriyor |
| `ReadmeTestTableTest` | Test sınıfı tablosu hatırlanmıyor, türetiliyor: `src/test` altındaki her `*Test.java` iki README'de de bir satıra sahiptir, Türkçe tablo İngilizce kadar tamdır ve hiçbir satır artık var olmayan bir sınıfı adlandırmaz |
| `ReleaseNotesTest` | Yayın sözleşmesi: her artefakt yüklemesi kısa bir saklama süresi taşır, yayın gövdesi depodaki şablondan gelir, şablon iki dilli kalır ve literal sürüm içermez, iki yarı aynı doğrulama komutlarını taşır, not indirme boyutlarını ölçer ve iş akışı kuru çalıştırma yapabilirken elle çalıştırma yayımlayamaz |
| `ReleaseAssetsScriptTest` | Taşınabilir arşiv betiği: sürücü harfli bir yol `tar` tarafından ana makine adı olarak okunmaz ve düz yollar hâlâ tek üst düzey dizinli bir arşiv üretir |
| `LicenseHeaderTest` | SPDX başlığı `src/` altındaki her `.java` dosyasının 0. baytında, `package`'tan önce durur ve kanonik ikon SVG'si aynı lisansı bildirir |
| `EditorActionTest` | Komut envanteri: her sabit kendi etkisini kaydeden bir `Context` üzerinden tam olarak uyguluyor (toolkit yok), her sabit bir etiket ve en az bir akor taşıyor, platforma bırakılan tek komut `EXIT_FULLSCREEN`, araç çubuğu düğmeleri ortak eylemleri çalıştırıyor ve `EditorAction` dışında bir akor literali yeniden yazılırsa kaynak taraması derlemeyi düşürüyor |
| `ShortcutRowsTest` | Kısayol kartı kopyalanmıyor, envanterden üretiliyor: `EditorAction` ile iki yönlü kapsam, yayımlanmış satır sırası, satır başına yerelleştirilmiş etiket ve açıklama, kartta hiçbir akorun iki kez geçmemesi — altı dilde |
| `EditorUndoContractTest` | Toolkit'siz geri al/ileri al sözleşmesi: hiçbir üretim kaynağı JavaFX'in özel undo-manager alanlarına uzanmaz ve belge yükleme metni hâlâ tek programatik çağrıda değiştirir |
| `OverlayCardTest` | Yüzen kartların kapanma sözleşmesi: Escape ve Enter kartı kapatır, diğer tuşlar karta bırakılır |
| `OverlaySlotTest` | Aynı anda en fazla bir yüzen kart: birini göstermek diğerlerini yok etmeden gizler, aynı kartı iki kez kaydetmek onu bir kez gizler ve kayıtsız bir sahip tüm üyeleri gizler |
| `AudioAssetTest` | Paketlenen tuş sesi bir yayın varlığıdır: classpath'te, biçimi ve SHA-256'sı `PROVENANCE.md` ile eşleşir, sessizlik değil duyulabilir bir geçiştir ve kaynak bilgisi yazarlığı ve lisansı belirtir |
| `SoundLevelTest` | Ses merdiveni: kademeler sessizlikten tama yükselir, yalnızca `OFF` sessizdir ve merdiven dışı kalıcı bir ses en yakın kademeye çözülür |
| `SoundPolicyTest` | Hangi tuşların takırdadığı: bir karakter yazmak takırdar, tanımsız karakter sessizdir, basışta yalnızca silme tuşları takırdar ve bir kısayol akoru hangi tuş olursa olsun sessizdir |
| `StatsPerfHarnessTest` | Artımlı sayaçların arkasındaki tavan: 10 MiB'lık belgede tek gerçek `StatsBarModel.refresh` (biçimlendirme dâhil) CI'da 50 ms'nin altında kalıyor. Tam metin sürümü saniyede bir ~164 ms ölçüyordu, yani O(n) bir yenileme geri dönerse derleme düşer |
| `FxThreadIoTest` | Toolkit açmadan FX thread değişmezi: açma, kaydetme ve otomatik kayıt diske yalnız `IoExecutor` üzerinden gidiyor (`fxThreadTasks()` sıfır kalıyor), temiz otomatik kayıt tikinde belge metni kopyalanmıyor ve kaydetme sürerken yazılan metin sonrasında kirli kalıyor |
| `RecoveryTargetPathTest` | Kurtarma taslağının başlığı ile sonraki kaydetme arasındaki koruma: yol mutlak, kanonik, izinli bir kökün altında ve uygulamanın kendi veri dizininin dışında olmalı. Göreli yol, kök dışına çıkan `..`, kırık symlink ya da izinli kök tanımlanmamış durum reddediliyor (fail closed) ama kurtarılan metin yine sunuluyor |

### CI/CD

GitHub Actions iş akışları: `main` dalına her push ve pull request'te derleme ve test (`.github/workflows/maven.yml`), sürüm tag'inde yayın (`.github/workflows/release.yml`). Kapıları ikisi de kendisi tanımlamıyor: ikisi de ortak `.github/workflows/verify-build.yml` iş akışını çağırıyor, böylece bir tag dalın koştuğu ikon kontrolünü, yerel ayar koşularını, bayt karşılaştırmasını ve smoke açılışını aynen koşuyor.

- **Tetikleme**: `main` dalına her push ve pull request'te çalışır
- **Ortam**: Ubuntu ve Windows (build matrisi)
- **JDK**: Temurin 25
- **Komut**: `mvn -B package` (derleme + testler), ardından `scripts/package.sh --skip-build --installer`
- **Kapılar**: `scripts/generate-icons.py --check` iki işletim sistemi ayağında da derlemeden önce koşar: her simgenin özetini `icons.manifest` ile karşılaştırır, böylece kanonik görselle artık eşleşmeyen bir varlık on dakika sonra değil bir dakika içinde kırmızı olur. Bilerek yeniden rasterleştirmez: ImageMagick aynı pikselleri farklı derlemelerde farklı baytlara kodlar, yani CI'da bayt ya da piksel karşılaştırması görseli değil araç zincirini karşılaştırırdı. `scripts/verify-artifacts.sh --skip-build` paketlemeden sonra koşar (Linux ayağı, çünkü `.deb` paketini `dpkg-deb` ile açar) ve depo yükü, uygulama görüntüsü ile kurulum paketi arasındaki bayt bayt karşılaştırmayı yeniden kanıtlar. Ayrı bir `smoke` işi paketlenen artefaktı indirip `scripts/smoke-appimage.sh --skip-build` betiğini iki işletim sistemi ayağında çalıştırır: Linux imajı Xvfb altında başlatır, `.deb` paketini kurar, çalıştırır ve kaldırır; Windows başlatıcının ayakta kalmasını şart koşar
- **Diller**: test paketi Türkçe ve Almanca sistem dillerinde de çalışır; böylece yerelleştirme regresyonları (ör. bir dilin sessizce sistem diline çözülmesi) gözden kaçamaz
- **Artefaktlar**: her işletim sistemi için yerel uygulama görüntüsü ve platform kurulum paketi (`.deb` / `.msi`)
- **Sürümler**: `pom.xml` sürümüyle eşleşen bir `v*` tag'i itildiğinde iki platform da derlenip bir GitHub Release'e yüklenir; yanına `SHA256SUMS` dosyası eklenir, böylece ikililer birkaç gün sonra silinen Actions artefaktlarına bağlı kalmaz. Aynı iş akışı elle **kuru koşu** olarak da başlatılabilir: `prepare`, iki işletim sistemi derlemesi, varlık/kaynak/özet birleştirmesi, beş varlık assert'i ve sürüm notu üretimi aynen koşar; yayımlama yerine bir artefakt yüklenir ve içinde tam olarak yayımlanacak set bulunur. Elle başlatılan bir koşu yayımlayamaz - dal ucundan üretilen bir sürüm kendi tag'inden yeniden üretilemez - bu yüzden `dry_run` varsayılanı `true`'dur ve `false` isteği `prepare` içinde reddedilir. Yayımlanan sürüm normal bir sürümdür, ön sürüm değil: `v1.1.0` `--prerelease` işaretini, nottaki erken erişim cümlesiyle birlikte kaldırdı, çünkü proje artık kullanıma hazır dediği bir sürümü yayımlıyor. Bayrak ile cümle aynı şeyin iki iddiasıdır; bu yüzden `ReleaseNotesTest` ikisini bir çift olarak pinliyor - yayımlama adımında bayrak yok, notun iki yarısında da erken derleme ifadesi yok - ve biri diğeri olmadan geri gelemez

---

## Lisans

JTypeWriter, Apache Lisansı 2.0 ile lisanslanmıştır. Tam metin için [LICENSE](LICENSE), atıf bildirimleri için [NOTICE](NOTICE) dosyasına bakın. [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) paketlenen her bileşeni lisans kimliği, telif satırı ve kaynağıyla listeler; tam metinler [legal/](legal) altındadır. İkisi de fat JAR içinde `META-INF/THIRD-PARTY-NOTICES` ve `META-INF/licenses/` olarak, native imajda ise kök dizinde bulunur. Paketlenen JavaFX kütüphaneleri GPLv2 + Classpath Exception, GStreamer Lite ise LGPL-2.1-or-later altında kalır; OpenJDK çalışma zamanı kendi bildirimlerini `lib/runtime/legal/` altında taşır. Paketlenen tuş sesi (`src/main/resources/audio/keystroke.wav`) yazarın kendi eseridir, üçüncü taraf bir örnek içermez ve aynı Apache-2.0 lisansı kapsamındadır; kaynağı ve SHA-256 özeti [src/main/resources/audio/PROVENANCE.md](src/main/resources/audio/PROVENANCE.md) dosyasında kayıtlıdır. Paketlenen fontlar DejaVu Sans Condensed (regular ve bold, arayüz) ve DejaVu Sans Mono (editör, yazdırma ve PDF dışa aktarma) olup değiştirilmeden Bitstream Vera lisansı altındadır; monospace dosya ayrıca her dışa aktarılan PDF'e gömülür, buna kurulabilir gömme izni elverir. Kaynakları ve dosya özetleri [src/main/resources/fonts/PROVENANCE.md](src/main/resources/fonts/PROVENANCE.md) dosyasındadır.

---

## Geliştirici

**Mesut ORMANLI**

- E-posta: [mesutormanli@gmail.com](mailto:mesutormanli@gmail.com)
- GitHub: [@hyperpostulate](https://github.com/hyperpostulate)

---

## Katkıda Bulunma

1. Depoyu fork edin
2. Bir özellik dalı oluşturun (`git checkout -b feature/yeni-ozellik`)
3. Değişikliklerinizi Commit edin (`git commit -m 'Yeni özellik ekle'`)
4. Dalı push edin (`git push origin feature/yeni-ozellik`)
5. Bir Pull Request oluşturun
