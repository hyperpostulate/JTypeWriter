JTypeWriter @VERSION@, built from tag `@TAG@`.

## English

Supported platforms: **Linux x64 and Windows x64**. Both channels are published
for each of them: the installer (`.deb`, `.msi`) and a portable `.tar.gz`. Which
one becomes the product default is still an open decision; neither is deprecated.

The installers are **not code-signed**. Windows SmartScreen will warn on first
launch.

Verify your download before running it:

```
sha256sum -c SHA256SUMS
```

On Windows, where `sha256sum` is not available by default, compare the printed
value with:

```
certutil -hashfile <file> SHA256
```

Every asset carries `LICENSE`, `NOTICE` and `THIRD-PARTY-NOTICES`; the source
archive next to the binaries is the exact tree the release was built from.

## Türkçe

JTypeWriter @VERSION@, `@TAG@` etiketinden derlendi.

Desteklenen platformlar: **Linux x64 ve Windows x64**. Her biri için iki kanal da
yayınlanır: kurulum paketi (`.deb`, `.msi`) ve taşınabilir `.tar.gz`. Hangisinin
ürün varsayılanı olacağı hâlâ açık bir karardır; ikisi de kullanımdan kaldırılmış
değildir.

Kurulum paketleri **kod imzalı değildir**. Windows SmartScreen ilk açılışta uyarı
gösterir.

İndirdiğiniz dosyayı çalıştırmadan önce doğrulayın:

```
sha256sum -c SHA256SUMS
```

Windows'ta `sha256sum` varsayılan olarak bulunmadığı için, yazdırılan değeri
şununla karşılaştırın:

```
certutil -hashfile <dosya> SHA256
```

Her artefakt `LICENSE`, `NOTICE` ve `THIRD-PARTY-NOTICES` dosyalarını taşır;
ikililerin yanındaki kaynak arşivi, sürümün derlendiği ağacın kendisidir.
