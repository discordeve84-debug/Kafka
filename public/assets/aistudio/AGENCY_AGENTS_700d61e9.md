# Agency Agents — repositori utama yang diminta

- Repository: https://github.com/msitarzewski/agency-agents
- Fungsi: katalog persona AI spesialis untuk tugas engineering, design, product, project management, QA/testing, research, dan fungsi lain.
- Kegunaan untuk Kafka: pilih persona seperti Android engineer, AI/ML engineer, UI/UX designer, researcher, technical writer, dan QA untuk membantu coding agent bekerja dengan peran dan checklist yang lebih fokus.
- Penting: ini adalah **persona/instruction files**, bukan aplikasi agent runtime dan bukan library Android.

## Instalasi dasar
```bash
git clone https://github.com/msitarzewski/agency-agents.git
cd agency-agents
```

## Cara menggunakan
1. Buka folder sesuai divisi.
2. Baca file persona `.md` yang relevan.
3. Pilih satu persona untuk satu pekerjaan yang jelas.
4. Gunakan file tersebut melalui mekanisme agent/instruction yang didukung IDE atau coding agent.
5. Jangan menyalin semua persona ke satu prompt; pilih spesialis sesuai tugas.
6. Simpan salinan yang digunakan di folder agent/skills proyek sesuai format tool yang dipakai, lalu dokumentasikan sumber dan tanggal pembaruan.

## Opsi: aplikasi Agency Agents
- Repository: https://github.com/msitarzewski/agency-agents-app
- Fungsi: aplikasi desktop native untuk menelusuri, memasang, dan melacak persona dari repository Agency Agents pada beberapa coding tools.
- Build development dari source:
```bash
git clone https://github.com/msitarzewski/agency-agents-app.git
cd agency-agents-app
npm install
npm run tauri dev
```
- Prasyarat build source menurut README: Rust stable dan Node.js 22+ dengan npm; pada macOS perlu Xcode Command Line Tools.
- Alternatif: unduh installer dari bagian Releases repository. Aplikasi ini mengelola persona di coding tools; bukan runtime untuk aplikasi Kafka.

## Referensi skill tambahan
### GuillemRoca/agent-skills-android
```bash
git clone https://github.com/GuillemRoca/agent-skills-android.git
```
Android-focused skills; ikuti `docs/getting-started.md`.

### anthropics/skills
```bash
git clone https://github.com/anthropics/skills.git
```
Contoh skill publik dan format direktori instruksi. Periksa kompatibilitas dengan coding agent yang dipakai.
