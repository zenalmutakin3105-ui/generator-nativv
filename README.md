# Generator PO — Native Android Beta 1.0

**PT Reka Cipta Selera · SCM**

Project ini dibuat ulang sebagai **Android native Java**. **Tidak ada WebView, HTML, Javascript, Chrome Custom Tabs untuk konten aplikasi, maupun wrapper Google Apps Script.** Kelima tab menggunakan Android Views asli (`Activity`, `LinearLayout`, `TextView`, `EditText`, `Spinner`, `Dialog`, dan `ScrollView`). Alokasi dikerjakan lokal oleh `PoRules.java`; PDF dibuat oleh Android `PdfDocument`.

## Apa yang sudah tersedia

- Dashboard dan metrik hasil alokasi.
- Master 8 supplier, 29 outlet, 3 SKU, rute Bandung/Jakarta.
- Master harga, pembatasan 1 supplier→banyak outlet dengan toggle default OFF, pengecualian catatan, master rute.
- Stok per supplier/SKU, BPA terpisah gudang Bandung/Jakarta.
- Input kebutuhan PCS (dada & fillet) dan Ekor (utuh); input manual per outlet atau paste satu kolom.
- Konversi PCS÷10 kemudian pembulatan supplier terdekat (boleh turun menjadi 0), stok tak bisa negatif, pending dan rincian +/- per outlet.
- Output supplier/SKU/rute/tanggal, outlet A-Z, note otomatis, kode Noted.
- Salin hasil PO sebagai teks menggunakan Android Clipboard (tanpa HTML).
- PDF **A4 landscape native**, maksimal 4 tabel per halaman, 1 supplier per halaman, susunan kiri atas → kiri bawah → kanan atas → kanan bawah, warna sku.
- Backup JSON `PO_Ayam_YYYY-MM-DD.json`, restore, reset data transaksi tanpa menghapus master, rollback snapshot sebelum impor.
- Sinkronisasi **langsung dari Google Sheets API read-only**, hanya blok pink RO–PPIC di `Resume Ayam`, tanggal kirim dari header, menunggu preview & konfirmasi sebelum mengganti kebutuhan untuk tanggal itu.

## Yang berbeda dari Web App

- **Master dan transaksi disimpan secara lokal di HP**, bukan otomatis pada database Google Apps Script. Backup JSON dari V6/Beta bisa digunakan untuk pindah data; jika tim membutuhkan Master bersama lintas HP, diperlukan sync dua arah/layanan backend terautentikasi tambahan.
- Sinkronisasi sumber Qty **langsung dari Sheets API** dengan otorisasi Google pengguna (bukan Web App Apps Script dan bukan `google.script.run`). Anda tetap dapat menjalankan project Apps Script lama secara terpisah.
- Google login dipakai **hanya untuk meminta izin akses Sheets read-only**, melalui Google Play Services Authorization API, bukan sebagai antarmuka aplikasi.
- Android minimal API 26 (Android 8); memerlukan Google Play Services untuk login dan koneksi internet untuk sinkronisasi; alokasi offline tetap jalan.
- **Beta ini bukan APK terkompilasi.** Paket ini harus dibuild melalui GitHub Actions/Android Studio; UI, login Google, PDF dan kompatibilitas backup masih harus diuji di perangkat nyata.

## Susunan project

```
.github/workflows/build-native-apk.yml
app/src/main/java/com/rekaciptaselera/generatorpo/
  MainActivity.java       # Semua layar Android native
  PoRules.java            # Mesin alokasi pure Java
  StateStore.java         # JSON lokal & snapshot rollback
  SheetsReader.java       # Google Sheets API read-only
  PdfExporter.java        # PDF A4 native
app/src/main/AndroidManifest.xml
app/build.gradle
tests/EngineTest.java
```

## Cara build APK dengan GitHub

1. **Buat repo baru** bernama `generator-po-native` (paling aman) supaya APK/native build tidak menimpa source dan workflow aplikasi web/beta stabil. Jika memakai branch pada repo lama, workflow manual `workflow_dispatch` dapat tidak muncul sampai file workflow juga tersedia di branch default; lebih mudah gunakan repo baru.
2. Ekstrak ZIP ini dan upload seluruh isinya **ke root** repo/branch termasuk folder `.github`, `app`, `tests`, `build.gradle` dan `settings.gradle`.
3. Buka **Actions → Build Generator PO Native APK → Run workflow** (branch yang berisi project native).
4. Job akan menjalankan tes mesin alokasi lalu build Android (JDK 17, Android SDK 35, Gradle 8.11.1).
5. Jika hijau, unduh **Artifacts → Generator-PO-Native-Beta-APK** → ekstrak `app-debug.apk` → instal di HP.

### Akses Google Sheets privat (khusus fitur sinkronisasi)

**Jangan taruh API key, token pribadi atau password dalam source/repository**. Aplikasi menggunakan `play-services-auth` untuk memperoleh token **sementara** dari Google Account pengguna.

1. Buat/pilih Google Cloud project organisasi, lalu **Enable Google Sheets API**.
2. Buka **Google Auth Platform → Branding/Audience** dan konfigurasi consent screen. Jika dalam mode Testing, masukkan akun SCM sebagai **Test user**.
3. Buat **OAuth Client ID → Android** dengan package `com.rekaciptaselera.generatorponative` dan **SHA-1 sertifikat penandatangan APK yang benar**.
4. Google Account yang dipilih di HP harus punya akses minimal *Viewer* ke spreadsheet sumber `1_Dyps9wFg5ZX4MZFqRptVzcK14CRF3lc1pGC7FWIn40`.
5. Di aplikasi tekan **Dashboard → Ambil Daftar Tanggal**, pilih akun, pilih tanggal kirim, periksa preview, baru **Impor 87 Qty**.
6. Jika error 403 pastikan API aktif, akun bisa membuka spreadsheet dan scope `spreadsheets.readonly` diizinkan; jika error OAuth `DEVELOPER_ERROR`, periksa package+SHA-1; jika 29 outlet tidak lengkap atau warna pink berubah, impor akan ditolak (data lama tidak ditimpa).

### Penting: keystore konsisten untuk OAuth & update APK

Debug APK GitHub Actions **tanpa signing secret** akan memakai kunci debug baru pada runner; SHA-1 bisa berubah dan update instalasi bisa gagal. Untuk pengujian berulang disarankan membuat **satu keystore tetap** dan memasukkannya sebagai GitHub Actions Secrets.

Di komputer yang memiliki JDK:

```bash
keytool -genkeypair -v -keystore generator-po-beta.jks -alias generatorpo -keyalg RSA -keysize 2048 -validity 3650
keytool -list -v -keystore generator-po-beta.jks -alias generatorpo
```

Simpan file `.jks` **di luar GitHub**, catat SHA-1. Gunakan SHA-1 tersebut pada OAuth Android client. Tambahkan secrets repo:

- `SIGNING_KEYSTORE_BASE64`: seluruh isi file JKS yang dienkode Base64 (bukan teks password)
- `SIGNING_STORE_PASSWORD`: password keystore
- `SIGNING_KEY_ALIAS`: `generatorpo`
- `SIGNING_KEY_PASSWORD`: password private key alias

PowerShell untuk menyalin isi Base64 ke clipboard:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('generator-po-beta.jks')) | Set-Clipboard
```

Jangan pernah commit `.jks` atau secrets. **Simpan backup keystore di tempat aman**; kehilangan kunci membuat update APK dengan package yang sama sulit.

## Pemeriksaan sebelum operasional

1. Validasi hasil `EngineTest` dari Actions (12 tes).
2. Cocokkan 29 outlet × 3 SKU = 87 Qty dengan sheet sumber untuk satu tanggal RO–PPIC.
3. Uji qty 0, stok tidak cukup, fallback, BPA dua gudang, pembatasan ON/OFF.
4. Uji rounding plus/minus dan nilai rupiah per supplier.
5. Simpan PDF A4 Landscape: 1 supplier per halaman, maksimal 4 tabel.
6. Uji backup/restore, reset input (Master tidak berubah), rollback sebelum impor dan penggantian versi APK.

> Migrasi ini **menjaga aturan alokasi** dari V6 tetapi merupakan implementasi Android native baru; belum diklaim identik 100% pada UX/PDF sebelum UAT perangkat.
