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

JTypeWriter tek bir düz metin belgesi açar ve sonra geri planda kalır: hesap yok, bulut yok, proje biçimi yok. Metnin, seçtiğin dosyada ve o dosyanın zaten sahip olduğu kodlamada kalır. Paketlenmiş uygulama kendi Java çalışma zamanını taşır, yani önce hiçbir şey kurmanız gerekmez; kaynaktan derleme veya fat JAR'ı çalıştırma JDK 25 ve Maven 3.8+ gerektirir. Linux ve Windows'ta çalışır ve arayüzü İngilizce, Türkçe, Almanca, Fransızca, İspanyolca ve İtalyanca konuşur.

**Neler var?**

- **Kazara mahvedemeyeceğin bir taslak.** YOLO modu Backspace, Delete, Kes ve Geri Al/İleri Al'ı kapatır; yalnızca ileri yazabilirsin. Her kayıt atomiktir ve dönen yedekler tutar; kaydedilmemiş iş 30 saniyede bir anlık görüntülenir - çökmeden sonra taslak geri teklif edilir ve o iletiyi kapatmak taslağı asla silmez.
- **Yazma hissi.** Yazarken, Backspace veya Delete'e bastığında - seçtiğin ses düzeyinde - daktilo sesi, üç tema (koyu, açık, sepya) ve altı metin rengi; hepsi yazarken değiştirilebilir.
- **Yazdıkça sayılar.** Kelime, boşluklu ve boşluksuz karakter, satır, tuş vuruşu, oturum süresi ve molaları saymayan sürdürülebilir kelime/dakika.
- **Ekranda yalnızca metin.** `F11` tam ekran, `Ctrl+T` araç çubuğunu ve içinde duran komut menüsünü birlikte gizler; pencere başlığı hangi belgenin açık olduğunu ve kaydedilip kaydedilmediğini söyler.
- **Kaldığın yerden devam eden oturum.** Dil, tema, metin rengi, yazı boyutu, son belge ve pencere geometrisi bir sonraki açılışta geri yüklenir.

---

## İçindekiler

- [Özellikler](#özellikler)
- [Verileriniz](#verileriniz)
- [Klavye Kısayolları](#klavye-kısayolları)
- [Temalar](#temalar)
- [Desteklenen Diller](#desteklenen-diller)
- [Gereksinimler](#gereksinimler)
- [Kurulum](#kurulum)
- [Derleme ve Çalıştırma](#derleme-ve-çalıştırma)
- [CI/CD](#cicd)
- [Mimari](#mimari)
- [Testler](#testler)
- [Lisans](#lisans)
- [Geliştirici](#geliştirici)
- [Katkıda Bulunma](#katkıda-bulunma)

---

## Özellikler

### Daktilo Ses Efektleri

Tuş sayfaya metin yazdığında otantik daktilo sesi. İmleç hareketi, fonksiyon tuşları ve kısayollar sessizdir, çünkü daktilo yalnızca yazdığında ya da üstünü çizdiğinde takırdar. Araç çubuğundaki Ses açılır listesi hızlı bir aç/kapat'tır; `Görünüm → Ses` düzey merdivenini açar - Kapalı, %25, %50, %75, %100 - ve seçim yeniden başlatmalar arasında hatırlanır; 0 tam sessizliktir. Her tık kendi sesini başlatır, öncekini kesmez; böylece hızlı yazım kekeleme değil hızlı yazım gibi duyulur.

### YOLO Modu

"YOLO Modu" (You Only Live Once) kazara silmeyi önler. Etkinleştirildiğinde Backspace, Delete, Cut ve Geri Al/İleri Al devre dışı kalır (sağ-tık menüsünde Kes soluk/devre dışı gösterilir; yalnızca Kopyala/Yapıştır/Tümünü Seç kullanılabilir); böylece hiçbir düzenleme belgedeki mevcut karakterleri silemez. İlk taslak yazımında geriye bakmadan ileriye giden yazım için mükemmeldir. Mod, `[YOLO]` başlık rozeti ve araç çubuğundaki YOLO açılır listesi ile görünür tutulur; tema renkleri her iki durumda da değişmez.

### Gerçek Zamanlı İstatistikler

Canlı istatistik çubuğu:
- Kelime sayısı, karakter sayısı (boşluklu/boşluksuz), satır sayısı
- Toplam tuş vuruşu
- Oturum süresi (SS:DD:SS)
- Dakikada kelime (WPM) - aktif yazım süresine dayalı kalıcı hız; molalar değeri düşürmez
- Durum çubuğu metni varsayılan olarak 13 px'tir ve çift tıklandığında küçük, orta ve büyük arasında döner; rengi üç temada da WCAG AA kontrastını (en az 4.5:1) karşılar
- Başarılı bir kayıt, saatini arayüz dilinin kendi saat biçimiyle durum çubuğuna yazar (ör. Türkçe `Kaydedildi 12:04`, İngilizce `Saved 12:04 PM`); sonraki düzenlemede temizlenir ve 30 saniyelik otomatik kayıt taslağı anlık görüntüsünü değil gerçek bir kaydı gösterir

### Tam Ekran ve Araç Çubuğu

- **`F11` ekranı devralır, `Escape` geri verir:** tam ekran `F11` ile açılıp kapanır, `Escape` ile çıkılır (platformun varsayılan çıkış ipucu gizlenir)
- **Araç çubuğu tamamen kaybolabilir:** ekranda yalnızca metin kalsın istediğinizde `Ctrl+T` onu kaldırır
- **Araç çubuğu ikinci satıra geçmez:** her genişlikte tek yatay satır kalır. Test edilen pencere boyutlarında altı açılır liste de değeriyle birlikte bir başlık gösterir ("Tema: Koyu", "Boyut: 16", "Dil: Deutsch") ve hiçbir şey taşmaz - 1280 px'te tam başlıklı, 900 px'te başlıklar yalın değere iner ("Koyu", "16", "Deutsch"), altı dilin tamamında, gerçek yerelleştirilmiş etiketlerden ölçülür. Pencere, en geniş daraltılmış satır ile pencere süslemesinin toplamının altına indirilemez (889 px, İspanyolca etiketlerden); böylece satır hiç kırpılmak zorunda kalmaz: minimum genişlik tahmin edilmez, altı paketten ölçülür ve bir çeviri etiketi büyütürse onu pinleyen test düşer. Eski bir yapıdan kalan ve bundan dar olan bir pencere boyutu minimumda geri gelir; saklanan değer o oturumun sonunda kendini onarır. Komut menüsü düğmesi ve altı düz düğme (ara, aç, kaydet, geri al, ileri al, hakkında) hiçbir zaman daralmaz ve her zaman görünür kalır
  - **Her kontrole ulaşılır ve her kontrol açıklanır.** `Tab` sabitlenmiş görsel sırayı izler: Menü, Ara, Aç, Kaydet, Geri Al, İleri Al, Tema, YOLO, Ses, Renk, Boyut, Hakkında, Dil. Sıra `viewOrder` ile ayarlanır; JavaFX bu özelliği çizim ve isabet alma için belgeler, yani odak sırası araç takımı garantisi değil uygulama sözleşmesidir. Her kontrol görünür bir odak halkası ve arayüz dilinden gelen, varsa kısayolu da içeren bir tooltip taşır, örneğin "Kaydet (Ctrl+S)"
- **Yerleşiminiz hatırlanır:** araç çubuğunun görünür veya gizli durumu, tam ekran ve durum çubuğunun boyutu yeniden başlatmalarda korunur; bir tercih yalnızca çıkışta değil, her değiştirdiğinizde kaydedilir
  - **Tam ekran pencere geometrisiyle çakışmaz.** Tam ekranda bırakılan bir pencere yeniden tam ekran açılır; açılışta tam ekran, hatırlanan konum ve boyuta karşı kazanır, sonradan çıkıldığında hatırlanan konuma dönülür

### Komut Menüsü

Araç çubuğunun en solundaki kontrol, dört menüyü taşıyan bir `☰` düğmesidir: Dosya, Düzen ve Görünüm editör komutlarını, Yardım ise Hakkında'yı içerir. Kalıcı bir menü şeridi değil bir menü düğmesidir: ikinci bir krom satırı tam ekranda da sizinle gelir ve `Ctrl+T` sözünü bozar; bu yüzden menü, içinde yaşadığı araç çubuğuyla birlikte gizlenir. Platformun kendisinin yürütmediği her editör komutu, kısayollar ve araç çubuğu düğmeleriyle aynı `EditorAction` envanterinden üretilir; böylece `Yeni` (`Ctrl+N`) ve `Farklı Kaydet` (`Ctrl+Shift+S`) yalnız klavyede kalmaz, görünür bir yere de kavuşur. Görünüm bir sıralı-komut listesi değil: Tema, YOLO, Renk ve Boyut'un her biri, araç çubuğundaki açılır listelerin aynı değerlerini listeleyen ve o anki değeri işaretli gösteren ikinci seviye bir alt menü açar - `Görünüm → Tema → Koyu` gerçekten Koyu demektir, "sıradaki tema" değil. Ses ise düzey merdivenini açar (Kapalı, %25, %50, %75, %100); araç çubuğundaki Açık/Kapalı listesi ikinci bir ayar değil, aynı değerin kısayoludur. Bu alt menüler araç çubuğuyla aynı seçenek listelerinden üretilir ve aynı uygulama yolundan yazar, yani iki giriş noktası çelişemez. Etiketler ve hızlandırıcılar arayüz dilini izler (hızlandırıcılar açıklamadır - tuşu tuş filtresi yürütür), açılan menü araç çubuğundaki açılır listelerle aynı tema belirteçlerinden boyanır (Koyu, Açık ve Sepya ile değişir) ve Yardım → Hakkında, Hakkında kartını açar. Menüler elle yazılmadığı için bir komut artık bir giriş noktasında varken diğerinde eksik kalamaz.

### Ara ve Değiştir

`Ctrl+F` pencerenin sağ üst köşesinde, metnin üzerinde kompakt bir ara/değiştir kartı açar; `Ctrl+H` aynı kartı değiştirme alanı odaklı açar. Notepad++'ın diyaloğu örnek alındı: tek "Aranan" alanı, tek "Yeni metin" alanı, "Büyük/küçük harf duyarlı" onay kutusu, Sonrakini bul / Öncekini bul / Değiştir / Tümünü değiştir düğmeleri ve canlı bir durum satırı ("3 / 12", "12 eşleşme", "Eşleşme yok"). Kart bir modal pencere değil panel: arama bir kiptir, belge görünür kalır, sorguyu yazıp `F3` ile art arda gezilir ve kart `Escape` ya da Kapat düğmesiyle kapansa bile sorgu bir sonraki `F3` için saklanır.

- **Aynı komutlar her yerde:** araç çubuğundaki `⚲` düğmesi, Düzen menüsü (`Ara`, `Sonrakini bul`, `Öncekini bul`, `Değiştir`) ve dört kısayol tek bir `EditorAction` yürütür, yani birbirinden kopamaz.
- **F3 ve Shift+F3 eşleşmeler arasında gezer** ve iki yönde sarar; kart kapandıktan sonra da çalışır. Henüz sorgu yoksa `F3` hiçbir şey yapmamak yerine kartı açar.
- **Arama düz metindir, regex değil:** `a.*b` o beş karakteri, `$1` dört karakteri anlatır; geçersiz desen diye ikinci bir hata yolu yoktur.
- **10 MB'lık belge pencereyi kilitlemez.** Tarama kendi iş parçacığında koşar ve yalnızca hâlâ güncel metni, sorguyu ve harf duyarlılığını anlatıyorsa FX thread'inde kurulur; siz bir karakter daha yazdıktan sonra gelen sonuç atılır. Gerçek modelle 10 MiB'lık belgede ölçüldü: tam belge taraması 26-53 ms (0 eşleşme, 197 845 eşleşme ve tek harflik sorguda 1 187 069 eşleşme), hiçbiri FX thread'inde değil.
- **Tümünü değiştir tek geri alma adımıdır:** yeni metin özgün eşleşmelerden hesaplanır ve tek düzenleme olarak uygulanır, böylece `Ctrl+Z` belgeyi tek hamlede geri alır. Değiştirme kendi çıktısını taramaz, yani `a` yerine `aa` yazmak sonlanır.
- **YOLO modu önce sorar.** Mod, hiçbir düzenlemenin karakter silmediği sözünü verir; değiştirme eşleşen aralığı siler, bu yüzden YOLO açıkken kart her işlem için bir kez sorar ve iptal hiçbir şey yazmaz. Değişiklik sonrasında normal ve geri alınabilir bir düzenlemedir.
- **Kart temayı izler:** yüzey, kenarlık, alanlar, onay kutusu, düğmeler ve durum satırı araç çubuğuyla aynı belirteçlerden çözülür; Koyu, Sepya ve Açık'ta yerel görünür - Modena'nın koyu temada beyaz boyayacağı giriş alanları dahil.

### Hoş Geldin, Son Dosyalar ve Hakkında Kartları

Hoş geldin ekranı, son dosyalar görünümü ve Hakkında görünümü artık ayrı pencereler değil, ara/değiştir panelinin durduğu sağ üst köşede birer kart. Köşeyi onunla paylaşıyorlar: biri açıldığında orada duran kart kapanır, yani gördüğünüz her zaman en son istediğiniz karttır.

- **Belge kullanılabilir kalır.** Kart modal değil, yani açıkken içinden bir kısayol kopyalayabilirsiniz; kart kapandığında imleç metne geri döner.
- **`Escape` ve `Enter` kartı kapatır**, diyalog olduğu zamanki gibi; kartın sağ üst köşesindeki `Kapat` düğmesi de aynısını yapar - ara paneldeki gibi bir simge değil, bir sözcük. Kartın tek düğmesi odur: içerik okunmak için vardır, alttaki ikinci bir eylem düğmesiyle kapatılmak için değil. Kart açılırken odağı bu düğme alır, yani bu tuşlar hemen çalışır; `Tab` yine kartın kontrollerini gezer.
- **Sınırı ekran değil pencere.** Kısa bir pencerede karttan daha uzun bir kart pencereye sığacak kadar küçülür ve kaydırılır, kenardan taşmaz; araç çubuğuyla aynı tema belirteçlerinden boyandığı için Koyu, Sepya ve Açık'ta yerel görünür.
- **Açıldığı dilde konuşur.** İçerik her açılışta ve dil değişiminde paketten yeniden kurulur, yani açık kalan bir kart dili değiştirdikten sonra önceki dilde kalamaz.
- **Karşılama sizi karşılamayı bırakabilir.** Hoş geldin kartındaki "bir daha gösterme" onay kutusu kalıcı bir tercihtir; işaretlendiğinde kart açılışta bir daha açılmaz. Geri dönüş yolu `Dosya → Karşılama Kartını Göster`'dir: tercihi temizler ve kartı hemen açar, yani vazgeçme bir daha açılamayan bir kapı olmaz.

**Son dosya listesi kendi kartındadır**, `Dosya → Son Dosyalar` ile açılır. Önceden hoş geldin kartının içindeydi ve listeyi, kullanıcının kapatabileceği bir karşılamaya bağlıyordu: karşılanmak istemeyen biri belgelerini yine de ister. İkisini ayırmak, karşılamanın kalıcı olarak kapatılabilmesini listeyi kaybetmeden sağlar; liste de her an yeniden açılabilir.

- **On belge, en yenisi başta.** Aynı belgeyi yeniden açmak ikinci bir satır eklemek yerine onu başa taşır; göreli bir yolla ya da `..` üzerinden ulaşılan aynı dosya da aynı belge sayılır. Liste dolduğunda düşen **en eski** girdidir, asla en yenisi.
- **Satır dosya adıyla adlandırılır**, böylece liste okunur kalır ve ev dizininiz ekrana gelmez; tam yol satırın ipucudur. Boş bir kart boş bir panel göstermek yerine bunu söyler.
- **`Aç` ile aynı kapıdan açar.** İptal, başarısız bir kaydetme ve kodlama sorusu menüden veya sürükle-bırakla açmaktakiyle birebir aynı davranır - kart sonraki belgeyi adlandırır, düzenleyicideki belgeyi atmanın ikinci bir yolu değildir.
- **Uygulama dışında silinen belge** listeden sessizce düşer, tıklandığında hata veren bir satıra dönüşmez.

### Veri Güvenliği ve Kurtarma

- **Atomik ve dayanıklı kayıt + yedek:** her kayıt geçici dosyaya yazılır, fsync edilir ve yeniden adlandırılarak yerine konur; böylece kayıt ortasında çökme ya da elektrik kesintisi belgeyi yarım bırakamaz
  - **Yedek yol da güvenli.** Dosya sistemi atomik taşımayı desteklemiyorsa yeniden adlandırma düz taşımaya düşer ve baytlar o anda zaten diskte olur.
  - **Üzerine yazma geçmişi ve izinleri korur.** Dönen yedekler (`dosya.bak`, `.bak.1`, `.bak.2`) tutulur ve belgenin mevcut dosya izinleri korunur.
- **Belge farkındalığı:** pencere başlığı açık belgenin adını ve kaydedilmemiş değişiklik varken bir `*` işaretini gösterir (`[YOLO]` rozetinin yanında)
  - **Yeni belge gerçek bir karardır.** `Ctrl+N` boş, adlandırılmamış bir belge açar - dosya açarken sorulan kaydet / kaydetme / iptal sorusu burada da sorulur - ve YOLO modu açıkken de çalışır.
  - **Geri alma belge sınırında durur.** Belge açmak veya yeni belge oluşturmak geri alma geçmişini sıfırlar, böylece `Ctrl+Z` önceki belgenin metnini asla geri getiremez.
- **Otomatik kayıt ve kurtarma:** kaydedilmemiş çalışma 30 saniyede bir ve odak kaybında `~/.jtypewriter/recovery/` altına alınır; çökmeden kalan taslak bir sonraki açılışta geri yükleme için teklif edilir. İstem geri yükle / sil / sonra sor seçeneklerini sunar; kapatmak hiçbir şeyi silmez.
  - **Silme istisnadır.** Taslak yalnızca pencerenin kapatma isteği kapatma yönünde yanıtlandığında silinir - belge kaydedildiyse ya da açıkça atmayı seçtiyseniz. Diğer tüm çıkış yolları (`Platform.exit()`, oturumdan çıkış, pencerenin yalnızca gizlenmesi) taslağı `pending-<uuid>.jtw` olarak park eder, böylece bir sonraki açılışta yeniden teklif edilir.
  - **Yazmaya devam edersiniz.** Ertelenen taslak aktif slotu serbest bırakır, yani üzerinde çalıştığınız belge için otomatik kayıt devam eder; park edilemezse slot dondurulur ve dosyaya hiç dokunulmaz.
  - **Bayat taslak belgeyi geriye çeviremez.** Diskteki dosya taslaktan kesinlikle daha yeniyse (eşit zaman damgaları bayat sayılmaz), metin yine de geri yüklenir - kurtarılabilir metin asla atılmaz - ancak özgün yolu olmadan; böylece sonraki kayıt Farklı Kaydet'e zorlanır.
  - **Arızalar görünür.** Başarısız bir kurtarma yazımı, sonraki yazım başarılı olana kadar durum çubuğunda uyarı bırakır ve ne yapılacağını açıklayan tek bir diyalog açar. Başarısız bir silme kendi uyarısını gösterir, bu uyarı sonraki başarılı silmede kalkar ve diyalog açılmaz - diyalog yalnızca kopya yazılamadığında gösterilir.
- **Kodlama güvenliği:** BOM'lu bir dosya, BOM'unun belirttiği kodlamayla çözülür; BOM'suz bir dosya geçerli UTF-8 olmalı ya da UTF-16'nın bayt düzenini taşımalıdır; aksi halde sessiz bozulma yerine Windows-1254 mü UTF-8 mi olduğu sorulur
  - **Uyuşmayan bayt sıra işareti tahmin değil, sorudur.** UTF-8 veya UTF-16 bildiren ancak bazı baytları o kodlama için geçersiz olan bir bayt sıra işareti sessizce çözülmez: uygulama işaretin içerikle eşleşmediğini söyler, bildirilen kodlamayı ve dosyayı adlandırır ve açmadan önce sorar. Yine de açmak bozuk baytları değiştirme karakterleri olarak gösterir ve bunu loglar; iptal etmek mevcut belgeye dokunmaz
  - **Kayıt kodlamayı korur.** Belgeler aynı kodlamayla geri yazılır; yalnızca metin o kodlamaya sığmadığında (örn. Windows-1254 dosyaya yapıştırılan emoji) sunulan UTF-8'e geçişi kabul ederseniz kodlama değişir - temsil edilemeyen karakterler asla sessizce `?` yapılmaz
  - **BOM'suz UTF-16 dosyası tahmin edilmez, tanınır.** Bayt sıra işareti olmayan ve baytları UTF-16 parite düzenine uyan bir belge, NUL yüklü UTF-8 olarak kabul edilip sonraki kayıtta yeniden yazılmak yerine UTF-16LE veya UTF-16BE olarak açılır (ve loglanır); iki düzene de uymayan metin sessiz bir varsayım yerine yine Windows-1254/UTF-8 sorusunu alır
- **Hata görünürlüğü:** beklenmeyen hatalar `~/.jtypewriter/logs/` altına tam yığın iziyle (neden zinciri dahil) yazılır; yakalanmamış çökmeler ve yazılamayan veya park edilemeyen bir kurtarma taslağı da sessizce kaybolmak yerine hata diyaloğu açar.
  - **Bazı yollar yalnızca loglanır.** Başarısız bir tuş sesi ve başarısız bir geri alma geçmişi sıfırlaması diyalog olmadan loglanır; başarısız bir ayar yazımı ise dosyayı adlandıran tek bir uyarı açar, çünkü aksi halde o andan sonra değiştirilen her tercih sessizce kaybolurdu. Başarısız bir taslak silme diyalog değil durum çubuğu uyarısı gösterir.
  - **Bozuk durum üzerine yazılmaz, kenara alınır.** Bozuk ayar dosyası `settings.properties.corrupt` olarak kenara alınır ve içeriği tanılama için korunur.
  - **Loglama açılışı engelleyemez.** Log dosyası oluşturulamazsa - JVM ev dizinine erişimi reddederse veya log konumu salt okunursa - uygulama yalnızca konsol loglamasıyla açılır.

### Ayarlar ve Oturum Geri Yükleme

- **Neler hatırlanır:** dil, tema, metin rengi, font boyutu, ses düzeyi, son belge ve klasörü ile pencere geometrisi `~/.jtypewriter/settings.properties` dosyasına kaydedilir; bir sonraki oturum öncekinin bittiği yerden başlar - aynı görünüm, aynı belge, aynı pencere
  - **Hiçbir şey açılışı engellemez.** Aradan geçen sürede silinmiş bir belge sessizce atlanır, bozuk bir ayar dosyası ise başlangıcı engellemek yerine varsayılanlara döner
  - **Dosya kendi şema sürümünü taşır.** `schema.version`, eski bir sürümün yazdığı dosyanın yeni sürümde değer kaybetmeden açılmasını sağlar; daha yeni bir sürümün dosyası ise bu sürümün anladığı kadarıyla okunur ve asla geriye düşürülmez. Göç bellekte yapılır; dosya ancak bir sonraki gerçek değişiklikte yeniden yazılır
  - **Yazılamayan ayar dosyası bir kez bildirilir.** Pencere çalışmaya devam eder ve belgelerinize dokunulmaz; uyarı dosyayı adlandırır ve dil, tema, yazı tipi boyutu ile pencere konumunun bu oturumdan sonra hatırlanmayacağını söyler
  - **YOLO modu bilinçli olarak *kaydedilmez*.** Silmeyi engelleyen bir modun bir sonraki açılışta hâlâ etkin olması istenmez

### Uygulama Dışından Belge Açma

`JTypeWriter notlar.txt` belgeyi doğrudan açar; dosya ilişkilendirmesi veya çift tıklama aynı yolu aynı koddan geçirir, böylece iki giriş noktası aynı davranır. Yalnızca bir dosya adlandırılabilir: ikinci argüman sessizce ilkini seçmek yerine adını söyleyen bir iletişim kutusuyla reddedilir. Var olmayan, normal bir dosya olmayan ya da yol olarak çözülemeyen bir metin arayüz dilinde bildirilir - başlangıç asla sessizce boş bir düzenleyiciye düşmez, çünkü bu tam olarak kaybolmuş bir belge gibi görünürdü. Argüman yoksa, kendi onay kutusuyla kapatmadığınız sürece karşılama kartı görünür (Dosya menüsü onu yeniden açar). Kurtarılan taslak komut satırındaki belgeden yine önce gelir: o metnin tek kopyası odur, adlandırılan dosya ise hâlâ diskte. Sembolik bağlantı kendi adını korur; kaydetme yolu bağlantıyı hedefine kadar zaten izler. Bir örnek çalışırken ikinci örnek başlatmak tek örnek kilidi tarafından yine reddedilir - devir yoktur, yani çalışan pencereye dosyayı açması söylenmez.

**Dosya sürükleyip bırakmak da aynı anlama gelir.** Bir `.txt` veya `.md` dosyasını pencerenin üzerine - metne, durum çubuğuna, herhangi bir yere - sürükleyin; dosya Aç ile aynı koddan geçerek açılır: kirli belge için önce sorulur ve İptal ya da iptal edilen bir kaydetme düzenleyiciyi olduğu gibi bırakır, yani bırakma kaydetme sorusunu atlayan bir kısayol değildir. Hedef tüm penceredir çünkü imleç oradadır ve jest, metin alanı onu sürüklenen metin sanmadan önce yakalanır. Düzenleyici içindeki düz metin sürüklemeleri eskisi gibi çalışır. Hiç dosya olmaması, aynı anda birden çok dosya, bir klasör, bir PDF ya da sürükleme ile bırakma arasında kaybolmuş bir dosya, neyin bırakıldığını söyleyen bir diyalogla reddedilir - sessizce hiçbir şey yapmayan bir bırakma, bozuk bir pencere gibi görünürdü.

### Tek Örnek

Kullanıcı hesabı başına yalnızca bir JTypeWriter örneği çalışır: `~/.jtypewriter/`
altındaki bir kilit dosyası ikinci başlatmayı durdurur. İkinci süreç kendi penceresini
açmaz - onaylanması gereken yerelleştirilmiş bir uyarı diyaloğu gösterir ve ancak ondan
sonra kapanır. Çalışan örnek çökerse kilit işletim sistemi tarafından otomatik bırakılır;
çökme sizi dışarıda bırakmaz.

### Çoklu Tema

Üç yerleşik tema. Varsayılan Koyu; `Ctrl+Shift+T` bu sırayla geçer: Koyu → Sepya → Açık (araç çubuğundaki Tema açılır listesinden doğrudan da seçilebilir):
- **Açık** - Temiz ve parlak
- **Koyu** - Varsayılan, göz yormaz
- **Sepya** - Sıcak, kağıt hissi

### Metin Renkleri

Altı metin rengi (bir varsayılan + beş vurgu rengi): Varsayılan, Adaçayı, Lavanta, Kum, Çelik, Gül

### Çoklu Dil Desteği

6 dil: İngilizce, Türkçe, Almanca, Fransızca, İtalyanca, İspanyolca. Dil değiştirildiğinde araç çubuğu, diyaloglar, pencere başlığı ve durum çubuğu anında güncellenir; sabit bir desen yerine her tarih ve saat o dilin kendi biçimiyle yazılır.

---

## Verileriniz

JTypeWriter'ın sakladığı her şey makinenizde kalır. Hesap yok, telemetri yok, güncelleme denetimi yok ve ağ isteği yok: uygulama kendisi hiçbir şey göndermez; dışa açılan tek eylem, Hakkında kartındaki e-posta bağlantısıdır ve `mailto:` adresini kendi posta istemcinize devreder.

- **Nerede durur:** `~/.jtypewriter/` - `settings.properties` (dil, tema, metin rengi, yazı boyutu, ses, araç çubuğu görünürlüğü, durum çubuğu boyutu, pencere geometrisi ve son belge ile klasörü mutlak yol olarak), `recovery/` (çökme kurtarma taslakları), `logs/` ve belge içeriği taşımayan danışma amaçlı tek örnek kilidi `instance.lock`.
- **Taslaklar metninizi içerir.** Her `recovery/draft-<uuid>.jtw` kaydedilmemiş belgenin metnini birebir tutar; ertelenen taslak `pending-<uuid>.jtw` olarak park edilir ve temiz çıkıştan sonra da kalır. Taslak, metnin geldiği mutlak yolu da yazar; ayırıcılar ve satır sonları bozulmasın diye Base64 ile kodlanmıştır - bu bir kodlamadır, şifreleme değildir - yani taslak, geldiği belgeden daha uzun yaşayabilir. Tarama yalnızca düz dosyaları okur: taslak gibi adlandırılmış bir sembolik bağ izlenmez, yok sayılır; 64 MiB'ın üzerindeki bir dosya başlangıçta yüklenmek yerine atlanır. Park edilmiş taslaklar bir temizlik meselesidir - 30 gün sonra süresi dolar ve küme 10 dosya / 64 MiB ile sınırlıdır - oysa etkin bir `draft-<uuid>.jtw` asla süresi dolmaz, çünkü başka bir çalışan örneğe ait olabilir.
- **Belgelerinizin yanında:** üzerine yazma `<document>.bak`, `.bak.1` ve `.bak.2` bırakır; kayıt, dosyayı yerine taşımadan önce yanına `jtw-<name>*.tmp` yazar. İkisi de belge metnini içerir. Bu yüzden bir belge diskte boyutunun dört katı yer kaplar, kayıt yazılırken kısa süreliğine altı katına çıkar; ilk kayıt bunu bir kez söyler.
- **Loglar:** `logs/jtypewriter-0.log` ile `jtypewriter-2.log` arası, her biri bir megabaytlık, dönen üç kuşak. Dosya yollarını, hata iletilerini ve tam yığın izlerini içerirler.
- **Hepsini silmek:** önce uygulamayı kapatın, aksi halde log tutucusu ve `instance.lock` yeniden oluşturulur; sonra `~/.jtypewriter` dizinini ve belgelerinizin yanındaki `<document>.bak*` ile artık kalmış `jtw-*.tmp` dosyalarını silin. Uygulama içinde "tüm verileri sil" komutu yoktur ve uygulamayı kaldırmak bu veriyi silmez - kurulu imajı veya paketi kaldırmak `~/.jtypewriter` dizinini yerinde bırakır.

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
| `Ctrl+F` | Ara/değiştir kartını aç |
| `F3` | Sonraki eşleşme (sarar) |
| `Shift+F3` | Önceki eşleşme (sarar) |
| `Ctrl+H` | Ara/değiştir kartını değiştirme alanı odaklı aç |
| `F11` | Tam ekranı aç/kapat |
| `Escape` | Tam ekrandan çık |

---

## Temalar

CSS dosyaları `src/main/resources/css/` konumundadır:

| Tema | Dosya | Açıklama |
|------|-------|----------|
| Temel | `base.css` | Tüm temalarda paylaşılan stiller |
| Koyu | `dark.css` | Koyu arka plan, açık metin |
| Açık | `light.css` | Açık arka plan, koyu metin |
| Sepya | `sepia.css` | Sıcak sepya tonlu arka plan |

---

## Desteklenen Diller

| Dil | Dosya | Kod |
|-----|-------|-----|
| İngilizce | `messages.properties` | `EN` |
| Türkçe | `messages_tr.properties` | `TR` |
| Almanca | `messages_de.properties` | `DE` |
| Fransızca | `messages_fr.properties` | `FR` |
| İtalyanca | `messages_it.properties` | `IT` |
| İspanyolca | `messages_es.properties` | `ES` |

---

## Gereksinimler

| Gereksinim | Sürüm |
|------------|-------|
| Paketlenmiş uygulama | Java gerekmez (gömülü JDK çalışma zamanı) |
| Kaynaktan derleme / fat JAR | Java 25+, Maven 3.8+ |

---

## Kurulum

### İndirme (son kullanıcılar)

Kurulum paketini (Linux'ta `.deb`, Windows'ta `.msi`) ya da taşınabilir `.tar.gz` dosyasını
[GitHub Releases sayfasından](https://github.com/hyperpostulate/JTypeWriter/releases),
doğrulamak için `SHA256SUMS` dosyasıyla birlikte
indirin. İki kanal da gömülü çalışma zamanı taşır, yani hiçbiri Java kurulumu
gerektirmez. Aşağıdaki `git clone` ve `mvn clean install` adımları geliştirici
yoludur, kullanıcı yolu değildir.

### Manuel Derleme

```bash
git clone https://github.com/hyperpostulate/JTypeWriter.git
cd JTypeWriter
mvn clean install
```

---

## Derleme ve Çalıştırma

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

---

## CI/CD

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

## Mimari

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
├── service/
│   ├── AutoSaveService.java              # Dönemsel taslak kaydı + kurtarma teklifi
│   ├── AutosaveHealth.java               # Yapışkan otomatik kayıt hata durumu (DRAFT_WRITE / DRAFT_PARK / DRAFT_DELETE)
│   ├── BackupKeeper.java                 # Yazımdan önce dönen .bak yedekleri
│   ├── DialogService.java                # FileChooser ve Alert dialog yönetimi
│   ├── ExitDisposition.java              # Kurtarma taslağı için çıkış kuralı (CLEAR / PARK / KEEP)
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

### Temel Bileşenler

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

### Bağımlılıklar

| Bağımlılık | Sürüm | Amaç |
|------------|-------|------|
| JavaFX Controls | 26.0.1 | UI bileşenleri |
| JavaFX FXML | 26.0.1 | FXML yükleme |
| JavaFX Media | 26.0.1 | Ses oynatma |
| JUnit Jupiter | 6.0.3 | Test çerçevesi |
| Mockito | 5.23.0 | Test mock'lama |

JavaFX Graphics ve Base, Controls ile dolaylı olarak gelir. Bağımlılık enjeksiyonu framework'süzdür: nesne grafiğini `AppGraph` elle kurar.

---

## Testler

### Test Çalıştırma

```bash
# Tüm testler
mvn test

# Tek test sınıfı
mvn test -Dtest=SessionStatsTest

# Tek test metodu
mvn test -Dtest=SessionStatsTest#wordCountIsCorrect
```

### Test Sınıfları

| Test Sınıfı | Kapsam |
|-------------|--------|
| `SessionStatsTest` | Kelime/karakter/satır sayısı, tuş vuruşları, sıfırlama, aktif yazım süreli WPM (durağanlık bozulması regresyonu) |
| `FileServiceTest` | Açık sırasında ve yeni belgede kaydedilmemiş değişiklik akışı (iptal/atla/önce kaydet), atomik yazım, dönen yedekler, kodlama gidiş-dönüşü (temsil edilemeyen karakterler için UTF-8'e geçiş), yeni belgede kodlama sıfırlama, yaşam döngüsü dinleyicileri, kurtarma bağlama |
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
| `AppSettingsTest` | Tercihlerin yeniden başlatmada korunması, geri yüklenen araç çubuğu/tam ekran/durum çubuğu boyutu durumu ve bozuk değerlerde varsayılana düşmesi, varsayılanlar, bilinmeyen değerlere ve kullanılamayan yollara tolerans, bozuk ayar dosyasının sessizce ezilmek yerine karantinaya alınması, ayar şema sürümü, ses düzeyi (eski aç/kapat anahtarının düzey olarak okunması dahil), son dosya listesi ve karşılama opt-out'u |
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
| `AppFontsTest` | Paketlenen arayüz fontu bir sürüm varlığıdır: iki yüz de `base.css`'in adlandırdığı aile altında sınıf yolunda, altı dilin araç çubuğu etiketlerini (`☰` menü glifi dâhil) çizebiliyor ve sabitlenmiş bir metin sabitlenmiş bir genişlik ölçüyor; böylece sistem fontuna sessiz dönüş derlemeyi düşürüyor |
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

---

## Lisans

JTypeWriter, Apache Lisansı 2.0 ile lisanslanmıştır. Tam metin için [LICENSE](LICENSE), atıf bildirimleri için [NOTICE](NOTICE) dosyasına bakın. [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) paketlenen her bileşeni lisans kimliği, telif satırı ve kaynağıyla listeler; tam metinler [legal/](legal) altındadır. İkisi de fat JAR içinde `META-INF/THIRD-PARTY-NOTICES` ve `META-INF/licenses/` olarak, native imajda ise kök dizinde bulunur. Paketlenen JavaFX kütüphaneleri GPLv2 + Classpath Exception, GStreamer Lite ise LGPL-2.1-or-later altında kalır; OpenJDK çalışma zamanı kendi bildirimlerini `lib/runtime/legal/` altında taşır. Paketlenen tuş sesi (`src/main/resources/audio/keystroke.wav`) yazarın kendi eseridir, üçüncü taraf bir örnek içermez ve aynı Apache-2.0 lisansı kapsamındadır; kaynağı ve SHA-256 özeti [src/main/resources/audio/PROVENANCE.md](src/main/resources/audio/PROVENANCE.md) dosyasında kayıtlıdır. Arayüz fontu DejaVu Sans Condensed (regular ve bold) değiştirilmeden, Bitstream Vera lisansı altında paketlenir; kaynağı ve dosya özetleri [src/main/resources/fonts/PROVENANCE.md](src/main/resources/fonts/PROVENANCE.md) dosyasındadır.

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
