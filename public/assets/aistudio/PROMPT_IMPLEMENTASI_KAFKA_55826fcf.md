# Prompt Implementasi GitHub Resources untuk Kafka

Tempel prompt ini ke coding agent yang memiliki akses ke repository Kafka.

```text
Kamu bertindak sebagai Senior Android Kotlin Engineer, AI Agent Architect, dan QA Engineer untuk proyek Kafka.

KONTEKS PROYEK
Kafka adalah aplikasi Android native berbasis Kotlin + Jetpack Compose. Fitur yang sudah ada meliputi Agent Workspace, sesi riset, Live Web Search, pencarian jurnal, verifikasi klaim, Skill Hub, GitHub skill discovery, custom skill architect, Firebase AI Logic/Gemini, Firebase Auth, Cloud Firestore, voice input/TTS, image generation, serta ekspor DOCX/PDF/PPTX/Markdown/CSV.

TUGAS
1. Audit repository dan struktur proyek terlebih dahulu.
2. Baca daftar GitHub Resource Pack. Pilih hanya repositori yang benar-benar menyelesaikan kebutuhan yang ditemukan.
3. Agency Agents digunakan sebagai katalog persona/instruksi untuk proses coding; jangan menganggapnya sebagai runtime agent di APK.
4. Bedakan dependency Gradle, tool CLI, source sample, Agent Skill, dan MCP server.
5. Untuk setiap pilihan, jelaskan manfaat, file yang akan disentuh, dependensi, risiko, lisensi, dan cara rollback.
6. Jangan menambahkan MCP server ke aplikasi Android secara langsung tanpa desain MCP client/transport dan threat model yang benar. Utamakan backend terautentikasi untuk tool yang memerlukan akses jaringan/rahasia.
7. Jangan hardcode API key, token GitHub, Firebase secret, atau kredensial di source code/APK. Gunakan secret manager/backend yang tepat.
8. Pertahankan fitur, nama menu, navigasi, model, dan desain yang sudah ada. Jangan menghapus atau mengganti perilaku tanpa persetujuan eksplisit.
9. Gunakan versi kompatibel dengan Kotlin, Android Gradle Plugin, Gradle, minSdk, dan targetSdk proyek. Jangan menyalin nomor versi lama secara membabi buta.
10. Implementasikan dalam perubahan kecil. Setelah tiap tahap jalankan build dan tes yang relevan.

OUTPUT WAJIB
- Audit kondisi awal dan daftar file yang terlibat.
- Matriks repositori: dipakai / tidak dipakai / alasan.
- Rencana implementasi bertahap.
- Perubahan kode yang minimal dan terukur.
- Command build/test.
- Hasil build dan tes yang benar-benar dijalankan; jika belum dijalankan tulis NOT VERIFIED.
- Catatan lisensi, keamanan, dan rollback.

Jangan mengklaim build sukses jika command belum dijalankan dan hasilnya belum diperiksa.
```
