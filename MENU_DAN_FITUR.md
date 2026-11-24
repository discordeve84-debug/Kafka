# Daftar Menu & Fitur Aplikasi Kafka (v1.1)

**Kafka** adalah aplikasi Android riset otonom berbasis AI (*Anime AI Research Studio*) yang menggabungkan penelusuran web real-time, riset jurnal ilmiah, verifikasi klaim anti-halusinasi, orkestrasi skill modular, ekspor dokumen profesional, percakapan suara dua arah, studio gambar AI, dan sinkronisasi Cloud Firestore.

---

## 1. Navigasi Utama (Bottom Bar / Navigation Rail)

### A. Beranda (`HomeScreen`)
- **Hero Companion Card:** Status kesiapan agen secara real-time, sapaan pengguna, dan tombol cepat **Buka Workspace Chat**.
- **Statistik Cepat (Quick Tiles):** Ringkasan jumlah **Proyek**, **Skill Aktif**, dan **Library** yang dapat diketuk langsung untuk membuka tab terkait.
- **Studio Kemampuan Kafka (Grid 2×2):** Pintasan menuju:
  1. *Live Web Search*
  2. *Jurnal & Verifikasi*
  3. *Skill Tree & GitHub*
  4. *Word, PDF & PPTX*
- **Tren Riset Saat Ini:** Karusel topik riset terkini yang langsung membuka pencarian web di Agent Workspace saat diketuk.
- **Proyek Riset Terakhir:** Daftar sesi terbaru untuk melanjutkan riset secara cepat dan tombol **+ Sesi Baru**.

---

### B. Agent (`AgentWorkspaceScreen`)
- **Header Workspace:**
  - Pemilih & pengelola **Sesi Riset** (ganti sesi atau buat sesi baru).
  - Indikator jumlah **Skill Aktif** untuk sesi berjalan.
  - Tombol **Mode Percakapan Suara (TTS)** untuk membacakan jawaban agen secara otomatis.
  - Tombol **Simpan Snapshot** ke Research Library.
- **3 Sub-Menu Workspace:**
  1. **Chat:**
     - **5 Mode Agen:** `Hermes Agent` (otonom), `Live Web`, `Jurnal`, `Verifikasi`, dan `Dokumen`.
     - **Input Teks & Suara (Mikrofon):** Rekam audio pertanyaan (16kHz) dan transkripsi langsung menjadi kueri riset.
     - **Bacakan (Text-to-Speech):** Putar/hentikan pembacaan suara pada setiap jawaban agen.
     - **Sitasi & Pratinjau Dokumen:** Kartu sitasi yang bisa dibuka ke tautan sumber dan pratinjau dokumen sebelum ekspor.
  2. **Dashboard:**
     - Ringkasan seluruh proyek riset, metrik skill/dokumen/library, topik tren dengan tombol refresh, dan peluncur riset cepat.
  3. **Library:**
     - Arsip *Research Snapshot* beserta tag, ringkasan, dan daftar sitasi yang tersinkronisasi di Cloud Firestore.

---

### C. Riset (`ResearchVerifyScreen`)
Memiliki **3 Sub-Tab** khusus:
1. **Live Web:**
   - Pencarian web real-time lintas sumber (*DuckDuckGo*, *Wikipedia EN/ID*, *Algolia Tech Index*, dan *GitHub Live*).
   - Buka tautan langsung di browser atau kirim hasil ke Kafka Agent untuk diaudit.
2. **Jurnal Ilmiah:**
   - Penelusuran literatur open-access & peer-reviewed dari **OpenAlex**, **Europe PMC**, **DOAJ**, **arXiv**, **Crossref DOI**, serta **Repositori GitHub Koleksi Paper/Jurnal** (mengekstrak tautan paper & PDF langsung dari `README.md`, misalnya `papers-we-love` dan `awesome-llm-papers`).
   - Badge **OPEN ACCESS**, tombol **Buka PDF** langsung (bila tersedia), **Buka Jurnal**, **Ringkas & Verifikasi AI** (lengkap dengan pengecekan status HTTP tautan), **Tanya Agent**, dan **Ekspor Review ke Word (.docx)**.
3. **Verifikasi Isi:**
   - Audit teks, klaim, atau artikel terhadap sumber web & jurnal ilmiah untuk mendeteksi halusinasi.
   - Menghasilkan **Skor Integritas**, status bukti per klaim, serta tautan pembanding terverifikasi.

---

### D. Skill Hub (`SkillHubScreen`)
Memiliki **4 Sub-Tab** manajemen skill + **2 Playground Interaktif**:
1. **Skill Tree:**
   - Peta visual node skill aktif/nonaktif per sesi riset, termasuk pengelompokan kategori dan toggle massal.
2. **Daftar Skill:**
   - Mengelola seluruh skill bawaan maupun kustom (*toggle aktif/nonaktif*, *uji di Agent*, atau *hapus*):
     - *Document Architect (.docx)*, *Editorial PDF*, *Slide Deck (.pptx)*, *Structured Data & CSV*
     - *Academic Journal Deep-Researcher*, *Hermes Claim & Fact Verifier*, *Live Web & Search Grounding*
     - *Agency Research Auditor* (`msitarzewski/agency-agents`), *Android Kotlin Architect* (`GuillemRoca/agent-skills-android`), *GitHub Open-Paper & PDF Hunter* (`papers-we-love`), *MCP Fetch & Memory Bridge* (`modelcontextprotocol/servers`), *Google & Firebase AI Samples Pack* (`android/ai-samples`)
     - *Gemini Multi-Model Chatbot*, *Voice & Audio Transcriber*, *Voice Conversation (Mic + TTS)*
     - *Create & Edit Image (Nano Banana 2.1)*, *Interactive 2D Cyber-Grid Game*, *Location & Maps Link Explorer*, *Cloud Firestore & Google Auth*
3. **Cari GitHub:**
   - Mencari repositori skill/MCP/agent/jurnal di GitHub dengan preset kueri cepat (`papers-we-love`, `awesome llm papers arxiv pdf`, `msitarzewski/agency-agents`, `GuillemRoca/agent-skills-android`, `ourresearch/openalex-api`, `modelcontextprotocol/servers`, `android/ai-samples`, `firebase/quickstart-android`) dan mengimpornya menjadi skill Kafka.
4. **Buat dari 0 (AI Skill Architect):**
   - Merancang skill agen baru dari nol hanya dari deskripsi tujuan pengguna.
- **Playground Chips:**
  - **Studio Gambar (`ImageStudioDialog`):** Membuat gambar dari teks dan mengedit foto galeri (Android Photo Picker) via Firebase AI Logic `gemini-nano-banana-2.1`, lalu simpan/bagikan hasilnya.
  - **Game 2D 2048 (`KafkaCyberGameDialog`):** Mini-game puzzle angka interaktif *Cyber Matrix 2048* di dalam aplikasi.

---

### E. Dokumen (`DocStudioScreen`)
- **Document & Deck Generator:**
  - Menyusun dokumen asli langsung di perangkat dalam format **Word (`.docx`)**, **PDF (`.pdf`)**, **PowerPoint (`.pptx`)**, **Markdown (`.md`)**, dan **CSV (`.csv`)**.
  - Opsi **Riset Web & Jurnal + Verifikasi sebelum ekspor** agar isi dokumen tervalidasi sumber.
- **Arsip Dokumen:**
  - Daftar seluruh file yang telah dibuat beserta aksi **Buka**, **Bagikan**, **Buka Sesi Asal**, dan **Hapus**.

---

## 2. Menu Tambahan, Profil & Keamanan
- **Masuk dengan Google (`AuthScreen`):** Autentikasi akun Google menggunakan Android Credential Manager & Firebase Authentication.
- **Profil & Preferensi Kafka (Sheet Pojok Kanan Atas):**
  - Edit **Nama Tampilan** dan **Peran / Bidang Riset**.
  - Pilih **Format Dokumen Default** (`DOCX`, `PDF`, `PPTX`, `MD`, `CSV`) dan **Bahasa Agen Default** (`Indonesia` / `English`).
  - Toggle **Mode Gelap Hermes Studio** dan **Auto-Verifikasi sebelum Ekspor**.
  - Statistik & pintasan **Riwayat Dokumen**, serta tombol **Keluar**.
- **Fallback Non-Firebase (`FirebaseConfigMissingScreen`):**
  - Pengaman anti-crash jika APK dibangun tanpa `app/google-services.json`, menampilkan panduan setup Firebase, tombol langsung **Buka Modul Live Search, Jurnal & Verifikasi** (modul `:research` independen tanpa perlu Firebase), tombol **Coba Inisialisasi Ulang Firebase**, dan akses **Mini-Game Offline (Cyber Grid 2D)**.

---

## 3. Arsitektur Modul `:research` (Android Library Module)
- **`LiveResearchModule`:** Modul Gradle terpisah (`:research`) untuk:
  - **Live Web Search:** DuckDuckGo Live, Wikipedia (EN/ID), Algolia Tech Index, dan GitHub Live.
  - **Journal Search:** OpenAlex OA, Europe PMC, DOAJ, arXiv PDF, Crossref DOI, dan kurasi paper GitHub.
  - **Multi-Source Verifier:** Audit keterjangkauan URL HTTP/HTTPS, verifikasi jurnal (`verifyJournalQuick`), dan pencocokan klaim multi-sumber (`verifyContentMultiSource`) yang berjalan mandiri maupun bersama Gemini AI.
- **`ResearchModuleDialog`:** Antarmuka Compose modular siap pakai di dalam `:research`.
