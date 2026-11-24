# Riset, Pencarian, dan Data Akademik

## 1. ourresearch/openalex-api
- URL: https://github.com/ourresearch/openalex-api
- Fungsi: source/API project OpenAlex untuk data works, authors, institutions, topics, dan sumber ilmiah.
- Clone untuk studi:
```bash
git clone https://github.com/ourresearch/openalex-api.git
```
- Integrasi Kafka: gunakan dokumentasi API OpenAlex dan ketentuan rate limit/attribution; repo ini bukan library Android siap pakai.

## 2. Crossref/rest-api-doc
- URL: https://github.com/CrossRef/rest-api-doc
- Fungsi: dokumentasi REST API Crossref untuk metadata publikasi/DOI.
- Clone:
```bash
git clone https://github.com/CrossRef/rest-api-doc.git
```
- Integrasi Kafka: gunakan endpoint REST yang didokumentasikan, identifikasi aplikasi dengan contact email sesuai kebijakan Crossref, dan tangani rate limits.

## 3. arXiv API documentation
- URL: https://info.arxiv.org/help/api/user-manual.html
- Fungsi: dokumentasi API pencarian metadata dan abstrak preprint arXiv.
- Instalasi: tidak ada package yang wajib di-clone; akses API sesuai dokumentasi.
- Catatan: arXiv adalah preprint server; jangan menyebut semua hasilnya peer-reviewed.

## 4. Algolia
- URL: https://github.com/algolia
- Fungsi: platform search API dan client libraries.
- Instalasi: pilih client sesuai platform dari dokumentasi resmi Algolia; jangan memasang seluruh organisasi/repo.
- Catatan: gunakan hanya jika memang ada index yang relevan dan hak akses/konfigurasi tersedia.

## Catatan untuk modul Kafka
Proyek yang diberikan sudah memiliki `LiveResearchClient.kt` dan model riset/jurnal. Audit implementasi yang ada sebelum menambah klien API baru agar tidak membuat pencarian ganda, duplikasi model, atau klaim peer-review yang keliru.
