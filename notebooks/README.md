# Machine Learning notebooks — Menyala-HERO

Notebook dan script di folder ini mendokumentasikan eksperimen ML project.

## Isi

- `EDA_HEROISM.ipynb`: EDA dataset, statistik, dan visualisasi.
- `01_Logistic_Regression_9_Fitur.ipynb`, `02_Random_Forest_9_Fitur.ipynb`, `03_Extra_Trees_9_Fitur.ipynb`: eksperimen awal tiga algoritma dengan sembilan fitur.
- `train_baseline_only_model.py`: melatih Logistic Regression baseline-only yang koefisiennya digunakan oleh aplikasi Android.

## Dataset dan privasi

Workbook `Dataset_Heroism.xlsx` tidak disertakan di repository karena memuat data asesmen anak. Gunakan salinan yang aksesnya telah diizinkan. Untuk notebook tiga algoritma, letakkan workbook di root repository; notebook EDA juga mencari workbook di folder kerjanya. Jangan commit dataset, hasil identifikasi, atau data individual.

## Catatan interpretasi

Eksperimen 9 fitur dan model baseline-only aplikasi adalah jalur eksperimen berbeda. Notebook tiga algoritma memakai skor pre-test empat dimensi HERO selain IF/OMD; model di aplikasi hanya memakai fitur pra-intervensi yang tersedia saat pre-test awal. Jangan membandingkan angka keduanya seolah-olah berasal dari input model yang sama.

Label target berasal dari skor post-test `<30` dan masih berupa proxy eksperimental, bukan kriteria kebutuhan pendampingan yang tervalidasi. Lihat `MODEL_ARTIFACT_STATUS.md` di folder dokumentasi project untuk detail batas penggunaan.

## Menjalankan

Python 3.10+ disarankan. Paket utama: `numpy`, `pandas`, `scikit-learn`, `openpyxl`, `matplotlib`, `seaborn`, `joblib`, dan `iterative-stratification`.
