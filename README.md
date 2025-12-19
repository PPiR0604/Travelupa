# Travelupa

Aplikasi Android untuk rekomendasi tempat wisata di Indonesia.

## Deskripsi

Travelupa adalah aplikasi mobile yang membantu pengguna menemukan dan menyimpan informasi tentang tempat-tempat wisata di Indonesia. Aplikasi ini dibuat menggunakan Kotlin dan Jetpack Compose dengan arsitektur modern Android.

## Fitur

- 🔐 Autentikasi pengguna dengan Firebase
- 📍 Rekomendasi tempat wisata
- 🖼️ Galeri foto wisata
- 💾 Penyimpanan lokal dengan Room Database

## Teknologi yang Digunakan

- **Kotlin** - Bahasa pemrograman utama
- **Jetpack Compose** - UI framework modern untuk Android
- **Firebase Authentication** - Autentikasi pengguna
- **Firebase Firestore** - Database cloud
- **Room Database** - Database lokal
- **Coil** - Library untuk loading gambar
- **Navigation Compose** - Navigasi antar screen

## Requirements

- Android Studio Hedgehog atau lebih baru
- Minimum SDK 24 (Android 7.0)
- Target SDK 36
- Kotlin 1.9+

## Instalasi

1. Clone repository ini
```bash
git clone https://github.com/PPiR0604/Travelupa.git
```

2. Buka project di Android Studio

3. Sync Gradle dan tunggu sampai selesai

4. Setup Firebase:
   - Buat project di [Firebase Console](https://console.firebase.google.com/)
   - Download file `google-services.json`
   - Letakkan file di folder `app/`

5. Build dan jalankan aplikasi di emulator atau device Android

## Struktur Project

```
app/
├── src/main/
│   ├── java/com/example/travelupa/
│   │   ├── MainActivity.kt          # Activity utama
│   │   ├── AppDatabase.kt           # Konfigurasi Room Database
│   │   ├── ImageEntity.kt           # Entity untuk gambar
│   │   └── ImageDao.kt              # Data Access Object
│   └── res/                         # Resources (layout, drawable, dll)
└── build.gradle.kts                 # Konfigurasi Gradle
```

## Lisensi

Project ini dibuat untuk keperluan pembelajaran.
