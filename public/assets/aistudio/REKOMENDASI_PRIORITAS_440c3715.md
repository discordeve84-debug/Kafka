# Rekomendasi Prioritas untuk Kafka

## Prioritas 1 — paling relevan
| Urutan | Repository | Peran di Kafka | Cara pakai |
|---|---|---|---|
| 1 | [msitarzewski/agency-agents](https://github.com/msitarzewski/agency-agents) | Katalog persona spesialis: Android engineer, researcher, UI/UX, QA, documentation, dll. | Clone lalu pilih persona dan adaptasi ke format agent yang dipakai. |
| 2 | [GuillemRoca/agent-skills-android](https://github.com/GuillemRoca/agent-skills-android) | Skill engineering khusus Android Kotlin/Compose. | Clone, audit skill, salin skill yang relevan ke workspace coding agent. |
| 3 | [android/nowinandroid](https://github.com/android/nowinandroid) | Referensi arsitektur aplikasi Android modern yang utuh. | Clone sebagai referensi; jangan salin seluruh aplikasi. |
| 4 | [firebase/quickstart-android](https://github.com/firebase/quickstart-android) | Contoh resmi Auth, Firestore, Firebase AI Logic, dan layanan Firebase. | Clone, pelajari sample terkait, cocokkan konfigurasi Firebase. |
| 5 | [android/ai-samples](https://github.com/android/ai-samples) | Contoh resmi AI Android cloud/on-device dan agentic workflow. | Clone dan pelajari sample yang sesuai; periksa prasyarat masing-masing. |
| 6 | [modelcontextprotocol/servers](https://github.com/modelcontextprotocol/servers) | Contoh server MCP: fetch, memory, git, filesystem, dll. | Jalankan di host/backend MCP; bukan dependency Android langsung. |
| 7 | [android/compose-samples](https://github.com/android/compose-samples) | Pola UI Compose, Material 3, chat, adaptive layout, animasi. | Clone untuk referensi komponen dan pola UI. |
| 8 | [maestro-org/maestro](https://github.com/mobile-dev-inc/maestro) | Otomasi tes alur aplikasi Android. | Pasang CLI di komputer pengembang/CI. |

## Prioritas 2 — sesuai kebutuhan fitur
- [airbnb/lottie-android](https://github.com/airbnb/lottie-android): animasi JSON/Lottie.
- [coil-kt/coil](https://github.com/coil-kt/coil): pemuatan gambar untuk Compose.
- [langgraph4j/langgraph4j](https://github.com/langgraph4j/langgraph4j): workflow multi-agent Java; lebih cocok untuk service JVM/backend, bukan otomatis cocok ditanam ke APK.
- [anthropics/skills](https://github.com/anthropics/skills): contoh struktur Agent Skills.
- [google/skills](https://github.com/google/skills): skills untuk produk dan teknologi Google.
- [OpenAlex/openalex-api](https://github.com/ourresearch/openalex-api): API riset OpenAlex; gunakan dokumentasi API untuk integrasi aktual.
- [crossref/rest-api-doc](https://github.com/CrossRef/rest-api-doc): dokumentasi REST API Crossref.

## Integrasi yang disarankan
- **Di APK:** Kotlin/Compose, Firebase AI Logic, Firestore/Auth, Coil/Lottie, repository/network client.
- **Di komputer coding:** Agency Agents, Android Agent Skills, Maestro CLI.
- **Di backend/host MCP:** MCP servers, akses Git/web/filesystem dengan allowlist dan autentikasi.
- **Jangan menambah semua repositori sekaligus.** Pilih berdasarkan bug/fitur yang sedang dikerjakan dan lakukan satu integrasi per perubahan.
