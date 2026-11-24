# Testing, QA, dan CI/CD

## 1. mobile-dev-inc/maestro
- URL: https://github.com/mobile-dev-inc/maestro
- Fungsi: UI test automation untuk Android/iOS melalui flow YAML.
- Instalasi: ikuti metode resmi sesuai OS dari dokumentasi Maestro; CLI dijalankan di komputer pengembang/CI, bukan dependency APK.
- Contoh awal (macOS dengan Homebrew, jika tersedia di dokumentasi versi yang dipakai):
```bash
brew install maestro
```
- Verifikasi:
```bash
maestro --version
```
- Integrasi: buat flow untuk login, buka workspace, kirim query, buka dokumen, dan navigasi utama.

## 2. android/architecture-samples
- URL: https://github.com/android/architecture-samples
- Fungsi: referensi unit/integration/UI tests, Flow, repository, dan Hilt.
- Clone:
```bash
git clone https://github.com/android/architecture-samples.git
```

## 3. actions/checkout
- URL: https://github.com/actions/checkout
- Fungsi: GitHub Actions action untuk checkout source code.
- Pemakaian workflow:
```yaml
- uses: actions/checkout@v4
```

## 4. actions/setup-java
- URL: https://github.com/actions/setup-java
- Fungsi: setup JDK pada GitHub Actions.
- Pemakaian workflow:
```yaml
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: '21'
```

## 5. gradle/actions
- URL: https://github.com/gradle/actions
- Fungsi: setup Gradle dan cache pada GitHub Actions.
- Pemakaian: ikuti README/action version resmi; cocok untuk mempercepat build Kafka di CI.

## Catatan
ZIP Kafka sudah memiliki `.github/workflows/build-apk.yml`. Perbaiki workflow yang ada secara terukur; jangan membuat pipeline kedua tanpa alasan. Validasi debug/release signing, keystore secrets, `google-services.json`, artifact upload, dan release tag.
