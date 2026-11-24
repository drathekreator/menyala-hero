<p align="center">
  <img src="hero-compass-android/app/src/main/res/drawable-nodpi/menyala_hero_logo.png" alt="Logo Menyala-HERO" width="220">
</p>

<h1 align="center">Menyala-HERO</h1>

<p align="center">
  Temani pendamping mencatat perkembangan anak dan menjaga keberlanjutan program HEROISM.
</p>

<p align="center">
  <a href="https://github.com/drathekreator/menyala-hero/releases/latest/download/Menyala-HERO-preview.apk">
    <img alt="Unduh APK Android" src="https://img.shields.io/badge/Unduh-APK%20Android-B72929?style=for-the-badge&logo=android&logoColor=white">
  </a>
  <a href="https://github.com/drathekreator/menyala-hero/releases">
    <img alt="Semua rilis" src="https://img.shields.io/badge/Rilis-Lihat%20semua-004772?style=for-the-badge&logo=github&logoColor=white">
  </a>
</p>

## Tentang aplikasi

Menyala-HERO adalah prototipe Android offline-first. Pendamping dapat mencatat hasil asesmen sebelum dan sesudah program, mengamati rekomendasi awal model, lalu menyimpan kegiatan yang dilakukan bersama anak.

### Yang bisa dilakukan

- Mengelola profil pendamping dan data anak.
- Mengisi Identity Foreclosure dan Orientasi Masa Depan lewat formulir, foto, atau PDF.
- Memeriksa dan mengoreksi jawaban hasil OCR sebelum disimpan.
- Mencatat hasil HERO Asa, HERO Aksi, HERO Rasa, serta kegiatan HERO Karsa.
- Melihat perkiraan awal Logistic Regression setelah data pre-test tersedia.
- Melihat perkembangan per anak dan mengekspor atau memulihkan backup terenkripsi.

## Unduh APK

**[⬇️ Unduh Menyala-HERO untuk Android](https://github.com/drathekreator/menyala-hero/releases/latest/download/Menyala-HERO-preview.apk)** · [Lihat semua rilis](https://github.com/drathekreator/menyala-hero/releases)

Rilis saat ini **v1.0.0-preview** adalah build prototipe bertanda tangan debug untuk uji coba, bukan aplikasi resmi di Google Play. Perangkat perlu Android 10 atau lebih baru. Saat pertama memasang, Android mungkin meminta izin instalasi dari browser atau pengelola berkas. Jika mengganti ke build dengan kunci tanda tangan berbeda, Android mungkin meminta aplikasi lama dihapus; ekspor backup sebelum melakukannya.

SHA-256: `ba697ec76384fc165cea867149627b5c6e4f64eb22a4b854db67667052651591`

## Cara kerja rekomendasi

Model menggunakan data yang tersedia pada pre-test untuk menandai area HERO yang perlu ditinjau pendamping. Model dilatih dari data historis terbatas dan belum diuji prospektif pada kohort baru. Hasilnya adalah bahan diskusi, bukan diagnosis atau bukti bahwa kegiatan tertentu pasti efektif. Pendamping tetap memilih dan mencatat tindak lanjut.

## Build dan tes

Source aplikasi ada di [`hero-compass-android/`](hero-compass-android/). Build memerlukan JDK 17+, Android SDK API 37, dan Build Tools 36.0.0. Buat `local.properties` di folder proyek Android dengan `sdk.dir` yang menunjuk ke Android SDK lokal.

```bash
cd hero-compass-android
./gradlew testDebugUnitTest
./gradlew assemblePreview
./gradlew connectedDebugAndroidTest # perlu emulator atau perangkat Android
```

## Privasi

Data anak dan catatan pendamping disimpan terenkripsi di perangkat. Repository ini tidak menyertakan dataset peserta, hasil asesmen, atau backup. Jangan unggah data pribadi anak ke issue atau commit publik.

## Dukungan platform

Saat ini aplikasi hanya tersedia untuk Android. APK tidak dapat dipasang di iPhone; dukungan iOS dan web memerlukan pengembangan lintas platform tersendiri. Data lokal Android juga tidak otomatis tersinkron ke perangkat lain.
