# Android & Kotlin

## 1. android/nowinandroid
- URL: https://github.com/android/nowinandroid
- Fungsi: aplikasi contoh Android lengkap berbasis Kotlin/Jetpack Compose dengan pola arsitektur modern.
- Cocok untuk: struktur feature/data layer, state, navigation, testing, modularisasi.
- Instalasi: clone untuk dipelajari; bukan library Gradle.
```bash
git clone https://github.com/android/nowinandroid.git
cd nowinandroid
```
- Integrasi Kafka: bandingkan struktur ViewModel/repository dan alur state. Jangan mengganti struktur Kafka sekaligus.

## 2. android/architecture-samples
- URL: https://github.com/android/architecture-samples
- Fungsi: contoh beberapa pendekatan arsitektur Android, termasuk Compose, Flow, repository, dan testing.
- Instalasi:
```bash
git clone https://github.com/android/architecture-samples.git
cd architecture-samples
```
- Integrasi: gunakan sebagai bahan evaluasi arsitektur dan testability.

## 3. android/compose-samples
- URL: https://github.com/android/compose-samples
- Fungsi: contoh resmi Jetpack Compose, Material 3, chat UI, adaptive UI, tema, dan animasi.
- Instalasi:
```bash
git clone https://github.com/android/compose-samples.git
cd compose-samples
```
- Integrasi: ambil pola UI spesifik, bukan seluruh sample app.

## 4. GuillemRoca/agent-skills-android
- URL: https://github.com/GuillemRoca/agent-skills-android
- Fungsi: kumpulan instruksi/skill engineering Android untuk coding agents; Kotlin, Compose, MVVM/MVI, DI, testing, CI.
- Instalasi:
```bash
git clone https://github.com/GuillemRoca/agent-skills-android.git
```
- Integrasi: baca `docs/getting-started.md`; pilih skill yang cocok dan salin ke direktori skill agent yang digunakan. Skill berupa Markdown, bukan dependency aplikasi.

## 5. almasumdev/awesome-kotlin-android-agent-skills
- URL: https://github.com/almasumdev/awesome-kotlin-android-agent-skills
- Fungsi: koleksi skill dan checklist coding agent untuk Kotlin Android.
- Instalasi:
```bash
git clone https://github.com/almasumdev/awesome-kotlin-android-agent-skills.git
```
- Integrasi: evaluasi kualitas dan duplikasi dengan skill lain sebelum dipasang.

## 6. Kotlin/kotlinx.coroutines
- URL: https://github.com/Kotlin/kotlinx.coroutines
- Fungsi: coroutine dan Flow untuk operasi asynchronous.
- Instalasi Gradle (contoh dari README; sesuaikan versi Kotlin):
```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
```
- Catatan: proyek Kafka sudah menggunakan Flow/coroutine; cek `gradle/libs.versions.toml` sebelum menambah dependency agar tidak duplikat.

## 7. Coil-kt/coil
- URL: https://github.com/coil-kt/coil
- Fungsi: pemuatan gambar asinkron di Android/Compose.
- Instalasi Gradle (versi contoh dokumentasi 3.6.3):
```kotlin
dependencies {
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
}
```
- Integrasi: berguna untuk avatar, thumbnail riset, cover dokumen. Periksa apakah Coil sudah ada sebelum menambah.

## 8. square/okhttp
- URL: https://github.com/square/okhttp
- Fungsi: HTTP client untuk request web/API dengan caching dan TLS.
- Instalasi Gradle (contoh versi 5.3.0):
```kotlin
dependencies {
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
}
```
- Integrasi: Kafka sudah memiliki network client; audit `LiveResearchClient.kt` dan version catalog dahulu.
