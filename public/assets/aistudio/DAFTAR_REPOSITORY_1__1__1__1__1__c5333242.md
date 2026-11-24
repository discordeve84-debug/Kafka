# MCP dan Integrasi Tool

## 1. modelcontextprotocol/servers
- URL: https://github.com/modelcontextprotocol/servers
- Fungsi: reference implementations MCP, termasuk Fetch, Memory, Git, Filesystem, dan contoh lain.
- Clone:
```bash
git clone https://github.com/modelcontextprotocol/servers.git
```
- Menjalankan server memory (contoh dari README):
```bash
npx -y @modelcontextprotocol/server-memory
```
- Menjalankan server Git dengan uvx:
```bash
uvx mcp-server-git
```
- Catatan keamanan: server referensi bersifat edukatif, bukan otomatis production-ready. Filesystem harus dibatasi ke direktori yang diizinkan. Jalankan di host MCP/backend yang terkontrol.

## 2. modelcontextprotocol/kotlin-sdk
- URL: https://github.com/modelcontextprotocol/kotlin-sdk
- Fungsi: SDK Kotlin untuk membangun MCP client/server.
- Clone untuk studi:
```bash
git clone https://github.com/modelcontextprotocol/kotlin-sdk.git
```
- Integrasi: baca README dan dokumentasi SDK untuk versi/protokol saat implementasi. Jangan menganggap penambahan SDK saja langsung membuat Kafka mendukung MCP.

## 3. langgraph4j/langgraph4j
- URL: https://github.com/langgraph4j/langgraph4j
- Fungsi: workflow graph stateful dan multi-agent di ekosistem Java, dapat bekerja dengan LangChain4j/Spring AI.
- Clone:
```bash
git clone https://github.com/langgraph4j/langgraph4j.git
```
- Dependency: ikuti versi dan koordinat Maven/Gradle pada README/release resmi.
- Rekomendasi: evaluasi untuk backend JVM jika Kafka membutuhkan workflow agent kompleks; bukan pilihan pertama untuk dimasukkan ke APK Android.
