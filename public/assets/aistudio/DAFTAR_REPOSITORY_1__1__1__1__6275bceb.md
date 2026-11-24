# AI, Gemini, dan Firebase

## 1. firebase/quickstart-android
- URL: https://github.com/firebase/quickstart-android
- Fungsi: sample resmi Firebase Android termasuk Auth, Firestore, Firebase AI Logic, Crashlytics, App Distribution, dan layanan lain.
- Instalasi:
```bash
git clone https://github.com/firebase/quickstart-android.git
```
- Langkah: buka sample yang relevan di Android Studio; konfigurasi Firebase project dan `google-services.json` sesuai package name sample. Jangan menyalin file konfigurasi milik sample ke aplikasi Kafka.

## 2. android/ai-samples
- URL: https://github.com/android/ai-samples
- Fungsi: contoh Android AI cloud/on-device, termasuk AI Sample Catalog dan Jetpacker.
- Instalasi:
```bash
git clone https://github.com/android/ai-samples.git
```
- Langkah: ikuti README di direktori sample yang ingin dipelajari. Cocok untuk ide multimodal, AI workflows, dan integrasi Android.

## 3. google/skills
- URL: https://github.com/google/skills
- Fungsi: kumpulan Agent Skills untuk produk/teknologi Google, termasuk Android, Firebase, Google Cloud, dan ADK.
- Instalasi untuk Claude Code (sesuai README repo):
```bash
claude plugin marketplace add google/skills
```
Lalu instal plugin yang dibutuhkan melalui marketplace:
```bash
claude plugin install <plugin>@google-plugins
```
- Untuk Codex: tambahkan marketplace `google/skills`, lalu pilih plugin dari browser `/plugins`.
- Catatan: ini menginstal skill/plugin ke coding agent yang didukung, bukan ke APK Kafka.

## 4. firebase/firebase-android-sdk
- URL: https://github.com/firebase/firebase-android-sdk
- Fungsi: source SDK Firebase Android untuk mempelajari implementasi dan isu SDK.
- Instalasi source (untuk kontribusi/pemeriksaan SDK, bukan penggunaan biasa):
```bash
git clone --recurse-submodules https://github.com/firebase/firebase-android-sdk.git
```
- Penggunaan Firebase di aplikasi normal dilakukan melalui dependency Gradle dan Firebase Console, bukan membangun SDK ini sendiri.
