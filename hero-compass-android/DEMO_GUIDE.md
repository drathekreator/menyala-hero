# Alur Demo Menyala-HERO

Durasi yang disarankan: 5–7 menit. Gunakan data sintetis, bukan data anak asli. Isi dua pre-test sebelum presentasi supaya waktu demo bisa dipakai untuk menjelaskan hasil model.

## Siapkan satu profil contoh

- Nama: Rani Demo
- Tanggal lahir: 24 November 2018 (usia 8 tahun pada 24 November 2026)
- Jenis kelamin: Perempuan
- Terlibat memulung: Tidak
- Pre-test Identity Foreclosure: 14 jawaban skala 1 dan 1 jawaban skala 3; total 17/60.
- Pre-test Orientasi Masa Depan: semua 15 jawaban skala 1; total 15/60.

Dengan data model saat ini, empat label keluarannya:

| Area | Label demo |
| --- | --- |
| HERO Asa · Potensi | Tidak ditandai |
| HERO Aksi · Eksplorasi | **Perlu** |
| HERO Rasa · Refleksi | Tidak ditandai |
| HERO Karsa · Perencanaan | Tidak ditandai |

Ambang klasifikasi saat ini 0,5. Data contoh menghasilkan skor internal sekitar 0,503 untuk Aksi, jadi hasilnya berada dekat ambang. Angka internal itu bukan persentase peluang yang tervalidasi.

## Urutan presentasi

1. **Buka data Rani Demo.** Jelaskan bahwa profil dan asesmen disimpan untuk mengikuti kondisi anak per orang.
2. **Tunjukkan dua hasil pre-test.** Identity Foreclosure 17/60 dan Orientasi Masa Depan 15/60 menjadi data awal sebelum intervensi.
3. **Tunjukkan kartu “Hasil Prediksi Model”.** Empat kartu warna memperlihatkan label Perlu/Tidak ditandai. Untuk contoh ini, HERO Aksi · Eksplorasi diberi label Perlu.
4. **Jelaskan tindak lanjut.** Aplikasi memberi contoh role-play profesi, wawancara narasumber, atau kunjungan edukatif seperti KidZania bila tersedia. Pendamping memilih kegiatan yang cocok dan mencatat kegiatan yang benar-benar dilakukan.
5. **Tunjukkan pengukuran selama intervensi.** Input HERO Asa/Aksi/Rasa dipakai untuk melihat hasil kegiatan per subtes. Jelaskan bahwa skor tersebut tidak menjadi input prediksi awal.
6. **Tunjukkan post-test.** Isi ulang Identity Foreclosure dan Orientasi Masa Depan, lalu bandingkan dengan baseline. Perubahan skor adalah evaluasi hasil; satu contoh anak belum membuktikan kegiatan tertentu menjadi penyebab perubahan.

## Naskah singkat untuk menjelaskan ML

> “Sebelum intervensi, model menerima usia, dua skor pre-test, jenis kelamin, dan status keterlibatan memulung. Model mengeluarkan empat label area: Perlu atau Tidak ditandai, untuk memperkirakan area yang mungkin mendapat skor pascaprogram di bawah 30. Di contoh Rani, area Eksplorasi pada HERO Aksi ditandai Perlu, jadi aplikasi menawarkan beberapa contoh kegiatan eksplorasi profesi. Model belum memilih kegiatan terbaik; pendamping tetap menentukan kegiatan dan mencatat pelaksanaannya. Skor HERO selama kegiatan dan post-test dipakai untuk evaluasi program.”

## Batas interpretasi yang perlu diingat

- Model versi `0.1.0-prototype` dilatih dengan 51 data historis; Macro-F1 validasi silang 0,432 ± 0,110.
- Label target berasal dari ambang skor pascaprogram <30. Ini label proxy, bukan diagnosis kebutuhan anak.
- Saran kegiatan dipetakan dari area yang ditandai; model belum dilatih membandingkan efektivitas KidZania, role-play, atau kegiatan lain.
- Untuk melatih pemilihan intervensi, catat kegiatan yang benar-benar diterima anak, intensitasnya, baseline, serta hasil sesudahnya pada banyak anak. Evaluasi itu menjadi pekerjaan model berikutnya.
- Bila menjalankan alur di perangkat, gunakan nama “Rani Demo” dan catatan kegiatan bertanda “data sintetis”; hapus profil demo setelah presentasi.
