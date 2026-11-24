# Command Cheatsheet

## Clone semua sumber utama (opsional)
Jalankan di folder kerja yang kamu pilih:
```bash
git clone https://github.com/msitarzewski/agency-agents.git
git clone https://github.com/GuillemRoca/agent-skills-android.git
git clone https://github.com/android/nowinandroid.git
git clone https://github.com/android/compose-samples.git
git clone https://github.com/firebase/quickstart-android.git
git clone https://github.com/android/ai-samples.git
git clone https://github.com/modelcontextprotocol/servers.git
git clone https://github.com/modelcontextprotocol/kotlin-sdk.git
git clone https://github.com/maestro-org/maestro.git
```

## Build Kafka dari root proyek
```bash
./gradlew :app:assembleDebug
./gradlew test
```
Windows:
```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat test
```

## Perintah yang tidak sama
- `git clone`: mengambil source repository.
- `npm install`: memasang package Node dalam project yang memiliki `package.json`.
- `implementation(...)`: menambahkan dependency Android lewat Gradle.
- `npx ...` / `uvx ...`: menjalankan tool/server di lingkungan host.
- Menyalin file `.md`: memasang instruksi/skill ke coding agent; lokasi tujuan bergantung pada agent yang digunakan.

Jangan jalankan semua command tanpa membaca README masing-masing.
