# Kafka — GitHub Resource Pack

Paket kurasi repositori untuk proyek **Kafka**, berdasarkan ZIP proyek yang diberikan: aplikasi Android Kotlin + Jetpack Compose untuk AI research/document agent, Firebase AI Logic/Gemini, Firestore/Auth, live web & academic research, claim verification, skill hub, voice, image generation, serta ekspor DOCX/PDF/PPTX/Markdown/CSV.

## Cara membaca
- Mulai dari `00_PANDUAN/REKOMENDASI_PRIORITAS.md`.
- Setiap folder fungsi berisi `DAFTAR_REPOSITORY.md` dengan URL, kegunaan, jenis instalasi, command, dan catatan integrasi.
- `04_AGENCY_AGENTS_SKILLS/AGENCY_AGENTS.md` membahas repo Agency Agents yang diminta.
- `00_PANDUAN/PROMPT_IMPLEMENTASI_KAFKA.md` berisi prompt siap tempel untuk AI coding agent.

## Penting
1. **Clone bukan berarti otomatis terpasang ke aplikasi.** Repository contoh/agent perlu dibaca, dipilih, lalu diadaptasi.
2. Dependensi Android dipasang melalui Gradle di `app/build.gradle.kts`, bukan `npm install`.
3. Agency Agents dan Agent Skills adalah instruksi/persona untuk coding agent; bukan runtime AI yang otomatis berjalan di APK.
4. MCP server perlu MCP client/host. Jangan menaruh token rahasia di APK; gunakan backend aman untuk kredensial dan tool yang memerlukan akses privat.
5. Periksa lisensi, versi, izin, keamanan, kompatibilitas Kotlin/AGP, dan aktivitas repo sebelum mengambil kode.
6. Jangan mengganti arsitektur atau fitur yang sudah ada hanya karena sebuah repo menyediakan pendekatan berbeda.

## Struktur
- `01_ANDROID_KOTLIN`: arsitektur, Compose, Android AI samples, Firebase.
- `02_UI_UX_ANIMATION`: animasi UI dan pemuatan gambar.
- `03_AI_GEMINI_FIREBASE`: contoh integrasi AI/Gemini.
- `04_AGENCY_AGENTS_SKILLS`: Agency Agents dan skill coding Android.
- `05_MCP_INTEGRASI`: MCP server dan graph workflow.
- `06_RESEARCH_SEARCH_DATA`: jurnal, DOI, metadata riset.
- `07_TESTING_CI_CD`: pengujian Android dan pipeline.
- `08_UTILITAS_NETWORK`: HTTP dan coroutine.

Tanggal kurasi: 24 November 2026. Versi dependensi yang disebutkan adalah versi yang tampak pada dokumentasi/release yang dirujuk saat penyusunan; cek halaman resmi sebelum upgrade.
