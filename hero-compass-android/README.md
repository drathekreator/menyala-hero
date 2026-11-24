# Menyala-HERO Android

Native Android application using Kotlin and Jetpack Compose. The Figma export in `../hero-compass-app` is retained as a visual reference; app code and build configuration live here.

## Implemented vertical slices

- First launch offers profile creation or sign-in.
- Profile contains mentor name, Indonesian phone number, and a 4–6 digit PIN.
- One mentor profile is allowed per device installation. Phone and PIN are checked locally; no SMS, network, or server is used.
- Profile data is encrypted with an AES-GCM key held by Android Keystore. The PIN is stored as a salted PBKDF2-HMAC-SHA256 verifier, never as plain text.
- After authentication, the app opens a five-tab workspace: Home, Cari, Anak, Panduan, and Profil. Home shows the program phases as a horizontal stepper, the child count, active identity pre-tests, and progress. The same locally saved mentor photo appears in Profil and Home. Panduan keeps assessment scales and score ranges in one place so entry screens can stay focused on answering. The profile screen can lock the app and enable biometric sign-in backed by a Keystore key; PIN sign-in remains available.
- In Anak, the mentor can add a child using their name, date of birth, gender, and involvement in scavenging, view all saved children, and search by name from Cari. Future birth dates and invalid names are rejected.
- From a child's details, the mentor can correct the profile or request deletion. Deletion requires confirmation and removes the child, every saved/draft assessment across all phases and instruments, and all follow-up notes; the UI updates only after storage confirms success. Follow-up notes can also be edited or deleted individually.
- Child records are encrypted with AES-GCM using a key held by Android Keystore and remain available after repository recreation. The Android date picker prevents selecting a future date.
- Gender is stored with the dataset codes `L/P`; scavenging involvement is stored as `Ya/Tidak`. Earlier child records remain readable with the new attributes unset, without inventing values.
- Anak dapat mengisi pre-test dan post-test U-MICS Identity Foreclosure (masing-masing 15 butir), menyimpan jawaban parsial sebagai draf, lalu melihat total dan kategori setelah semua butir dijawab. OCR tersedia untuk gambar/foto/PDF dan hasil scan dapat dikoreksi pendamping sebelum disimpan. Jawaban disimpan per butir dan terenkripsi lokal.
- Interpretasi skor mengikuti tabel instrumen: 15–29 tinggi, 30–44 sedang, 45–60 rendah untuk kecenderungan identity foreclosure.
- Panduan memisahkan rubrik penilaian dari katalog kegiatan resmi HERO Asa, Aksi, Rasa, dan Karsa. Rubrik dan batas skor HERO Karsa belum tersedia, jadi Karsa hanya muncul sebagai panduan kegiatan program.
- Orientasi Masa Depan tersedia untuk pre-test dan post-test melalui input manual 15 butir maupun OCR. Skor dijumlahkan (tanpa reverse-scoring yang ditentukan dalam panduan): 15–29 rendah, 30–44 sedang, 45–60 tinggi. Hasil disimpan terenkripsi terpisah per anak dan fase.
- Form manual dan OCR HERO Asa/Forethought, HERO Aksi/Performance, dan HERO Rasa/Reflection tersedia untuk pre-test dan post-test. Halaman statistik anak membandingkan skor pre/post per instrumen tanpa menggabungkan skala yang berbeda. Catatan tindak lanjut lokal terenkripsi tersedia dengan nama kegiatan bebas dari pendamping.
- Setelah pre-test Identity Foreclosure dan Orientasi Masa Depan lengkap, statistik anak menampilkan empat label area `PERLU`/`TIDAK DITANDAI`, fokus utama, dan contoh kegiatan. Model `0.1.0-prototype` memakai lima input baseline dan dilatih pada 51 rekaman historis. Kartu utama tidak menampilkan sigmoid sebagai persentase; arti model dan Macro-F1 validasi silang (0,432 ± 0,110) ada di bagian “Tentang model”. Target skor pascaprogram <30 adalah label proxy, dan contoh kegiatan merupakan pemetaan area, bukan aktivitas terbaik yang dipelajari model.
- Profil menyediakan ekspor dan pemulihan backup portabel terenkripsi dengan frasa sandi (PBKDF2-HMAC-SHA256 + AES-256-GCM). Backup mencakup data anak, jawaban dan draf lima instrumen, serta catatan tindak lanjut; profil pendamping/PIN dan kunci perangkat tidak disertakan. Pemulihan hanya diizinkan ke instalasi yang belum memiliki data anak.
- Foto profil dipilih melalui Android Photo Picker, diperkecil, lalu disimpan di penyimpanan privat aplikasi. Login biometrik memakai BiometricPrompt dan kunci AES Android Keystore yang meminta autentikasi biometrik kuat setiap kali; jika tidak tersedia atau dibatalkan, mitra tetap bisa masuk dengan PIN.
- OCR memakai ML Kit Text Recognition v2 untuk mencocokkan judul, pernyataan, dan skala; pembacaan pilihan 1–4 memakai deteksi geometri grid dan kontras tinta per sel. Jawaban yang ambigu dibiarkan kosong; setiap hasil menampilkan gambar sumber, bisa diedit, dan memerlukan konfirmasi pendamping sebelum diselesaikan. OCR bukan model terlatih khusus untuk membaca centang.

## Build and test

Install the Android SDK (API 37 and build tools 36.0.0), JDK 17+, then set `sdk.dir` in `local.properties`. The checked-in Gradle Wrapper pins Gradle 9.6.0. Agent workspace tooling is kept under the ignored `.devtools/` directory.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew connectedDebugAndroidTest # requires a running Android emulator or device
```

Verification on 24 November 2026: `testDebugUnitTest` passed 30/30 tests and `connectedDebugAndroidTest` passed 46/46 tests on Motorola edge 60 fusion. `assemblePreview` passed. The generated release APK and AAB are **unsigned**; they are not upload-ready Play Store packages. This project is a prototype for a supervised field pilot, not an official product release.

An earlier all-feature Logistic Regression report (Macro-F1 0.554 ± 0.091 versus 0.644 ± 0.048 for a simple matched pre-test rule) used inputs not all available before intervention and is not the model shipped in the prototype. The baseline-only candidate is intentionally displayed for supervised pilot evaluation, despite weak retrospective performance. The intended stage order is baseline pre-tests → experimental baseline estimate → HERO activities during intervention → Identity Foreclosure and Future Orientation post-tests. Current screens still support pre/post for HERO Asa/Aksi/Rasa; this remains a workflow mismatch to resolve before a public product release. No validated HERO Karsa scoring rubric was found in the program report or presentation.

HERO Asa, HERO Aksi, and HERO Rasa use their respective ten statements on a 1–4 scale. Each complete response total is categorized Low 10–19, Medium 20–29, or High 30–40. Answers are saved as encrypted drafts and completed pre-test/post-test records are kept separate.

OCR validation used six user-provided filled examples across varied HERO Rasa photos and HERO Asa/Aksi PDFs. All six passed item-level comparison; ambiguous faint marks remain available for manual entry. A separate blank Future Orientation booklet scan contains a printed watermark in the answer grid and produced several false candidates. A stricter experimental threshold reduced this to one candidate but has not been validated on filled Future Orientation scans, so it is not enabled. This is why OCR always requires the mentor to compare every row with the original before completing an assessment. Filled Future Orientation scans should receive additional validation when such examples are available.

The local SDK and JDK used in the agent workspace are installed under `.devtools/`; these files are ignored and are not part of the app source.

Presenter walkthrough and reproducible synthetic inputs are in [`DEMO_GUIDE.md`](DEMO_GUIDE.md).
