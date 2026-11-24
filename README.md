# Menyala-HERO

Menyala-HERO adalah prototipe Android offline-first untuk membantu pendamping mencatat asesmen, kegiatan, dan perkembangan anak pada program HEROISM.

## Fitur

- Profil pendamping lokal dengan PIN dan login biometrik.
- Data anak, asesmen, dan catatan kegiatan tersimpan terenkripsi di perangkat.
- Pre-test dan post-test Identity Foreclosure serta Orientasi Masa Depan.
- Input manual dan OCR foto/PDF dengan pemeriksaan serta koreksi oleh pendamping.
- Pencatatan HERO Asa, HERO Aksi, HERO Rasa, dan kegiatan HERO Karsa.
- Perkiraan awal berbasis Logistic Regression lokal untuk uji alur pendampingan.
- Backup dan pemulihan data melalui berkas terenkripsi.

Model di aplikasi masih prototipe, dilatih dari data historis terbatas, dan belum diuji prospektif pada kohort baru. Hasilnya adalah bahan tinjauan pendamping, bukan diagnosis atau bukti efektivitas kegiatan.

## Build Android

Source aplikasi dan Gradle Wrapper ada di [`hero-compass-android/`](hero-compass-android/). Persyaratan build: JDK 17+, Android SDK API 37, dan Build Tools 36.0.0. Buat `local.properties` di folder tersebut dan isi `sdk.dir` sesuai lokasi Android SDK di komputer.

```bash
cd hero-compass-android
./gradlew testDebugUnitTest
./gradlew assemblePreview
./gradlew connectedDebugAndroidTest # memerlukan emulator atau perangkat Android
```

APK Preview dibuat di `hero-compass-android/app/build/outputs/apk/preview/app-preview.apk`. APK tidak disimpan dalam Git.

## Data dan privasi

Data anak dan catatan pendamping berada di penyimpanan lokal aplikasi. Repository ini tidak menyertakan dataset peserta, hasil asesmen, backup, atau data perangkat. Jangan menambahkan data pribadi anak ke commit atau issue publik.

## iOS

Repository ini hanya berisi aplikasi Android. APK tidak dapat dipasang di iPhone. Dukungan iOS memerlukan aplikasi iOS terpisah atau migrasi ke kerangka lintas platform, beserta build dan penandatanganan melalui Xcode di macOS. Versi sekarang tidak memiliki sinkronisasi server, sehingga data lokal Android tidak otomatis tersedia di iOS.
