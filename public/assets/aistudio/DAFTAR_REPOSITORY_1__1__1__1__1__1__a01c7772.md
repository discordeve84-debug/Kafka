# Utilitas Network & Asynchronous

## 1. square/okhttp
- URL: https://github.com/square/okhttp
- Fungsi: HTTP client modern dengan TLS, HTTP/2, connection pooling, dan cache.
- Gradle (contoh README 5.3.0):
```kotlin
dependencies {
    implementation("com.squareup.okhttp3:okhttp:5.3.0")
}
```
- Catatan: Kafka sudah memiliki modul network. Audit dependency dan client yang ada sebelum menambah.

## 2. Kotlin/kotlinx.coroutines
- URL: https://github.com/Kotlin/kotlinx.coroutines
- Fungsi: coroutine, cancellation, async/await, Flow.
- Gradle (contoh):
```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
}
```
- Catatan: proyek Kafka sudah memakai coroutine/Flow. Hindari menambahkan versi kedua.

## 3. coil-kt/coil
- URL: https://github.com/coil-kt/coil
- Fungsi: memuat gambar asynchronous di Compose.
- Gradle:
```kotlin
dependencies {
    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.3")
}
```
