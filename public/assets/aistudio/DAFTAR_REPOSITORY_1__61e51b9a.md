# UI, Visual, dan Animasi

## 1. airbnb/lottie-android
- URL: https://github.com/airbnb/lottie-android
- Fungsi: render animasi Lottie JSON hasil ekspor After Effects/Bodymovin.
- Cocok untuk: status agent, loading research, micro-interactions, animasi empty state.
- Instalasi Gradle:
```kotlin
dependencies {
    implementation("com.airbnb.android:lottie-compose:<VERSI_STABIL>")
}
```
- Catatan: ganti placeholder dengan versi stabil yang diperiksa dari release resmi. Jangan menambahkan library jika animasi Compose native sudah cukup.

## 2. android/compose-samples
- URL: https://github.com/android/compose-samples
- Fungsi: contoh layout, Material 3, chat, adaptive screen, theming.
- Instalasi:
```bash
git clone https://github.com/android/compose-samples.git
```
- Catatan: clone sebagai referensi; bukan dependency.

## 3. coil-kt/coil
- URL: https://github.com/coil-kt/coil
- Fungsi: menampilkan gambar avatar/thumbnail secara asynchronous.
- Instalasi Gradle (Coil 3.6.3 pada dokumentasi yang dirujuk):
```kotlin
dependencies {
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
}
```
- Catatan: cek version catalog dan kompatibilitas Kotlin/Compose.
