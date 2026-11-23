package com.example.document

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.model.DocumentBlueprint
import com.example.model.OutputFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class GeneratedFileResult(
    val file: File,
    val wordCount: Int,
)

class DocumentGenerator(private val context: Context) {

    private fun exportsDir(): File {
        val dir = File(context.filesDir, "kafka_exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun generateDocument(
        blueprint: DocumentBlueprint,
        format: OutputFormat,
    ): GeneratedFileResult = withContext(Dispatchers.IO) {
        val safeBase = blueprint.title
            .replace(Regex("[^a-zA-Z0-9\\-_\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(40)
            .ifBlank { "Kafka_Dokumen" }
        val fileName = "${safeBase}_${System.currentTimeMillis() % 10000}.${format.extension}"
        val outFile = File(exportsDir(), fileName)

        when (format) {
            OutputFormat.DOCX -> writeDocx(outFile, blueprint)
            OutputFormat.PDF -> writePdf(outFile, blueprint)
            OutputFormat.PPTX -> writePptx(outFile, blueprint)
            OutputFormat.CSV -> writeCsv(outFile, blueprint)
            OutputFormat.MARKDOWN, OutputFormat.JSON, OutputFormat.RESEARCH ->
                writeMarkdown(outFile, blueprint)
        }

        val words = countWords(blueprint)
        GeneratedFileResult(file = outFile, wordCount = words)
    }

    fun getFileUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun shareFile(file: File, mimeType: String) {
        val uri = getFileUri(file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan ${file.name}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openFile(file: File, mimeType: String): Boolean {
        return try {
            val uri = getFileUri(file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            shareFile(file, mimeType)
            false
        }
    }

    suspend fun shareBitmap(bitmap: Bitmap, baseName: String = "Kafka_Image"): File =
        withContext(Dispatchers.IO) {
            val outFile = File(exportsDir(), "${baseName}_${System.currentTimeMillis() % 100000}.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 96, fos)
            }
            withContext(Dispatchers.Main) {
                shareFile(outFile, "image/png")
            }
            outFile
        }

    private fun countWords(bp: DocumentBlueprint): Int {
        val all = buildString {
            append(bp.title).append(' ')
            append(bp.subtitle).append(' ')
            append(bp.executiveSummary).append(' ')
            bp.sections.forEach {
                append(it.heading).append(' ')
                append(it.body).append(' ')
                it.bullets.forEach { b -> append(b).append(' ') }
            }
        }
        return all.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    // ==================== 1. REAL OOXML WORD (.docx) ====================

    private fun writeDocx(outFile: File, bp: DocumentBlueprint) {
        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.writeEntry("[Content_Types].xml", docxContentTypes())
            zip.writeEntry("_rels/.rels", docxRootRels())
            zip.writeEntry("word/_rels/document.xml.rels", docxDocumentRels())
            zip.writeEntry("word/styles.xml", docxStyles())
            zip.writeEntry("word/document.xml", docxDocumentXml(bp))
        }
    }

    private fun docxContentTypes() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>"""

    private fun docxRootRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private fun docxDocumentRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun docxStyles() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
    <w:name w:val="Normal"/>
    <w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/><w:sz w:val="22"/><w:color w:val="1E293B"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Title">
    <w:name w:val="Title"/>
    <w:rPr><w:b/><w:sz w:val="44"/><w:color w:val="0F766E"/></w:rPr>
  </w:style>
  <w:style w:type="paragraph" w:styleId="Heading1">
    <w:name w:val="heading 1"/>
    <w:rPr><w:b/><w:sz w:val="28"/><w:color w:val="0F172A"/></w:rPr>
  </w:style>
</w:styles>"""

    private fun docxDocumentXml(bp: DocumentBlueprint): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>""")

        sb.append(docxPara(bp.title, style = "Title", spaceAfter = 120))
        if (bp.subtitle.isNotBlank()) {
            sb.append(docxPara(bp.subtitle, color = "475569", italic = true, spaceAfter = 240))
        }
        sb.append(docxPara("Ringkasan Eksekutif", style = "Heading1", spaceBefore = 160, spaceAfter = 80))
        sb.append(docxPara(bp.executiveSummary, spaceAfter = 200))

        bp.sections.forEach { sec ->
            sb.append(docxPara(sec.heading, style = "Heading1", spaceBefore = 200, spaceAfter = 80))
            if (sec.body.isNotBlank()) {
                sb.append(docxPara(sec.body, spaceAfter = 120))
            }
            sec.bullets.forEach { bullet ->
                sb.append(docxPara("• $bullet", leftIndent = 360, spaceAfter = 80))
            }
        }

        if (bp.references.isNotEmpty()) {
            sb.append(docxPara("Referensi & Sumber Terverifikasi", style = "Heading1", spaceBefore = 240, spaceAfter = 100))
            bp.references.forEachIndexed { idx, ref ->
                sb.append(docxPara("[${idx + 1}] $ref", color = "334155", leftIndent = 240, spaceAfter = 60))
            }
        }

        sb.append("""<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr>""")
        sb.append("</w:body></w:document>")
        return sb.toString()
    }

    private fun docxPara(
        text: String,
        style: String? = null,
        color: String? = null,
        italic: Boolean = false,
        leftIndent: Int = 0,
        spaceBefore: Int = 0,
        spaceAfter: Int = 100
    ): String {
        val escaped = escapeXml(text)
        val styleTag = style?.let { """<w:pStyle w:val="$it"/>""" }.orEmpty()
        val indentTag = if (leftIndent > 0) """<w:ind w:left="$leftIndent"/>""" else ""
        val spacingTag = """<w:spacing w:before="$spaceBefore" w:after="$spaceAfter" w:line="276" w:lineRule="auto"/>"""
        val colorTag = color?.let { """<w:color w:val="$it"/>""" }.orEmpty()
        val italicTag = if (italic) "<w:i/>" else ""
        return """<w:p><w:pPr>$styleTag$indentTag$spacingTag</w:pPr><w:r><w:rPr>$colorTag$italicTag</w:rPr><w:t xml:space="preserve">$escaped</w:t></w:r></w:p>"""
    }

    // ==================== 2. REAL MULTI-PAGE PDF (.pdf) ====================

    private fun writePdf(outFile: File, bp: DocumentBlueprint) {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 46f
        val contentWidth = (pageWidth - margin * 2).toInt()

        val pdf = PdfDocument()
        var pageNum = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
        var canvas = page.canvas

        val headerBgPaint = Paint().apply { color = Color.parseColor("#0B0E14") }
        val accentPaint = Paint().apply { color = Color.parseColor("#2DD4BF") }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#99F6E4")
            textSize = 11f
        }
        val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F766E")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E293B")
            textSize = 10.5f
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
        }

        // Top dark Hermes-inspired cover band
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 118f, headerBgPaint)
        canvas.drawRect(0f, 114f, pageWidth.toFloat(), 118f, accentPaint)
        val titleLayout = StaticLayout.Builder.obtain(bp.title, 0, bp.title.length, titlePaint, contentWidth).build()
        canvas.save()
        canvas.translate(margin, 28f)
        titleLayout.draw(canvas)
        canvas.restore()

        val subText = bp.subtitle.ifBlank { "Dossier Terverifikasi oleh Kafka AI Agent" }
        val subLayout = StaticLayout.Builder.obtain(subText, 0, subText.length, subtitlePaint, contentWidth).build()
        canvas.save()
        canvas.translate(margin, 28f + titleLayout.height + 6f)
        subLayout.draw(canvas)
        canvas.restore()

        var y = 140f

        fun nextPage() {
            canvas.drawText("Kafka Research & Document Agent — Hal $pageNum", margin, pageHeight - 24f, footerPaint)
            pdf.finishPage(page)
            pageNum++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create())
            canvas = page.canvas
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 4f, accentPaint)
            y = 44f
        }

        fun drawWrapped(text: String, paint: TextPaint, extraSpaceAfter: Float = 8f, indent: Float = 0f) {
            if (text.isBlank()) return
            val layout = StaticLayout.Builder.obtain(
                text, 0, text.length, paint, (contentWidth - indent).toInt()
            ).setLineSpacing(2f, 1.15f).build()

            if (y + layout.height > pageHeight - 54f) {
                nextPage()
            }
            canvas.save()
            canvas.translate(margin + indent, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + extraSpaceAfter
        }

        drawWrapped("Ringkasan Eksekutif", headingPaint, 6f)
        drawWrapped(bp.executiveSummary, bodyPaint, 14f)

        bp.sections.forEach { sec ->
            drawWrapped(sec.heading, headingPaint, 6f)
            drawWrapped(sec.body, bodyPaint, 8f)
            sec.bullets.forEach { bullet ->
                drawWrapped("• $bullet", bodyPaint, 5f, indent = 12f)
            }
            y += 6f
        }

        if (bp.references.isNotEmpty()) {
            drawWrapped("Referensi & Sumber Terverifikasi", headingPaint, 6f)
            bp.references.forEachIndexed { idx, ref ->
                drawWrapped("[${idx + 1}] $ref", bodyPaint, 4f, indent = 8f)
            }
        }

        canvas.drawText("Kafka Research & Document Agent — Hal $pageNum", margin, pageHeight - 24f, footerPaint)
        pdf.finishPage(page)

        FileOutputStream(outFile).use { fos ->
            pdf.writeTo(fos)
        }
        pdf.close()
    }

    // ==================== 3. REAL OOXML POWERPOINT (.pptx) ====================

    private fun writePptx(outFile: File, bp: DocumentBlueprint) {
        val slides = mutableListOf<Pair<String, List<String>>>()
        slides += bp.title to listOf(
            bp.subtitle.ifBlank { "Presentasi Riset & Sintesis Kafka AI" },
            bp.executiveSummary.take(260)
        )
        bp.sections.forEach { sec ->
            val lines = buildList {
                if (sec.body.isNotBlank()) add(sec.body)
                addAll(sec.bullets)
            }.take(6)
            slides += sec.heading to lines
        }
        if (bp.references.isNotEmpty()) {
            slides += "Referensi & Verifikasi Sumber" to bp.references.take(6)
        }

        ZipOutputStream(FileOutputStream(outFile)).use { zip ->
            zip.writeEntry("[Content_Types].xml", pptxContentTypes(slides.size))
            zip.writeEntry("_rels/.rels", pptxRootRels())
            zip.writeEntry("ppt/presentation.xml", pptxPresentationXml(slides.size))
            zip.writeEntry("ppt/_rels/presentation.xml.rels", pptxPresentationRels(slides.size))
            zip.writeEntry("ppt/slideMasters/slideMaster1.xml", pptxSlideMaster())
            zip.writeEntry("ppt/slideMasters/_rels/slideMaster1.xml.rels", pptxSlideMasterRels())
            zip.writeEntry("ppt/slideLayouts/slideLayout1.xml", pptxSlideLayout())
            zip.writeEntry("ppt/slideLayouts/_rels/slideLayout1.xml.rels", pptxSlideLayoutRels())
            zip.writeEntry("ppt/theme/theme1.xml", pptxTheme())

            slides.forEachIndexed { idx, (heading, bullets) ->
                val slideNum = idx + 1
                zip.writeEntry("ppt/slides/slide$slideNum.xml", pptxSlideXml(heading, bullets, slideNum == 1))
                zip.writeEntry("ppt/slides/_rels/slide$slideNum.xml.rels", pptxSlideRels())
            }
        }
    }

    private fun pptxContentTypes(slideCount: Int): String {
        val overrides = (1..slideCount).joinToString("\n  ") {
            """<Override PartName="/ppt/slides/slide$it.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
  $overrides
</Types>"""
    }

    private fun pptxRootRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>"""

    private fun pptxPresentationXml(slideCount: Int): String {
        val sldIds = (1..slideCount).joinToString("") {
            """<p:sldId id="${255 + it}" r:id="rId$it"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rIdMaster"/></p:sldMasterIdLst>
  <p:sldIdLst>$sldIds</p:sldIdLst>
  <p:sldSz cx="12192000" cy="6858000"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>"""
    }

    private fun pptxPresentationRels(slideCount: Int): String {
        val slideRels = (1..slideCount).joinToString("\n  ") {
            """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide$it.xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdMaster" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
  <Relationship Id="rIdTheme" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
  $slideRels
</Relationships>"""
    }

    private fun pptxSlideMaster() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst>
</p:sldMaster>"""

    private fun pptxSlideMasterRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>"""

    private fun pptxSlideLayout() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank">
  <p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld>
</p:sldLayout>"""

    private fun pptxSlideLayoutRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>"""

    private fun pptxTheme() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="KafkaHermes">
  <a:themeElements>
    <a:clrScheme name="Kafka">
      <a:dk1><a:srgbClr val="0B0E14"/></a:dk1><a:lt1><a:srgbClr val="F8FAFC"/></a:lt1>
      <a:dk2><a:srgbClr val="1E293B"/></a:dk2><a:lt2><a:srgbClr val="E2E8F0"/></a:lt2>
      <a:accent1><a:srgbClr val="2DD4BF"/></a:accent1><a:accent2><a:srgbClr val="F59E0B"/></a:accent2>
      <a:accent3><a:srgbClr val="38BDF8"/></a:accent3><a:accent4><a:srgbClr val="10B981"/></a:accent4>
      <a:accent5><a:srgbClr val="6366F1"/></a:accent5><a:accent6><a:srgbClr val="EC4899"/></a:accent6>
      <a:hlink><a:srgbClr val="38BDF8"/></a:hlink><a:folHlink><a:srgbClr val="2DD4BF"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="KafkaFonts">
      <a:majorFont><a:latin typeface="Calibri"/></a:majorFont>
      <a:minorFont><a:latin typeface="Calibri"/></a:minorFont>
    </a:fontScheme>
    <a:fmtScheme name="KafkaFmt"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="9525"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme>
  </a:themeElements>
</a:theme>"""

    private fun pptxSlideXml(title: String, lines: List<String>, isCover: Boolean): String {
        val bodyParagraphs = lines.joinToString("") { line ->
            val prefix = if (isCover) "" else "• "
            """<a:p><a:pPr marL="228600" indent="-228600"><a:spcBef><a:spcPts val="1000"/></a:spcBef></a:pPr><a:r><a:rPr lang="en-US" sz="${if (isCover) 2000 else 1800}" dirty="0"><a:solidFill><a:srgbClr val="E2E8F0"/></a:solidFill></a:rPr><a:t>${escapeXml(prefix + line)}</a:t></a:r></a:p>"""
        }

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr><a:solidFill><a:srgbClr val="0B0E14"/></a:solidFill><a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr/>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="2" name="AccentBar"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="12192000" cy="110000"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:solidFill><a:srgbClr val="2DD4BF"/></a:solidFill></p:spPr>
      </p:sp>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="3" name="Title"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="685800" y="457200"/><a:ext cx="10820400" cy="1143000"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
        <p:txBody><a:bodyPr wrap="square"/><a:lstStyle/><a:p><a:r><a:rPr lang="en-US" sz="${if (isCover) 3600 else 2800}" b="1" dirty="0"><a:solidFill><a:srgbClr val="2DD4BF"/></a:solidFill></a:rPr><a:t>${escapeXml(title)}</a:t></a:r></a:p></p:txBody>
      </p:sp>
      <p:sp>
        <p:nvSpPr><p:cNvPr id="4" name="Content"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
        <p:spPr><a:xfrm><a:off x="685800" y="1750000"/><a:ext cx="10820400" cy="4500000"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr>
        <p:txBody><a:bodyPr wrap="square"/><a:lstStyle/>$bodyParagraphs</p:txBody>
      </p:sp>
    </p:spTree>
  </p:cSld>
</p:sld>"""
    }

    private fun pptxSlideRels() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
</Relationships>"""

    // ==================== 4. MARKDOWN & CSV ====================

    private fun writeMarkdown(outFile: File, bp: DocumentBlueprint) {
        val text = buildString {
            appendLine("# ${bp.title}")
            if (bp.subtitle.isNotBlank()) appendLine("*${bp.subtitle}*")
            appendLine()
            appendLine("## Ringkasan Eksekutif")
            appendLine(bp.executiveSummary)
            appendLine()
            bp.sections.forEach { sec ->
                appendLine("## ${sec.heading}")
                if (sec.body.isNotBlank()) appendLine(sec.body)
                sec.bullets.forEach { appendLine("- $it") }
                appendLine()
            }
            if (bp.references.isNotEmpty()) {
                appendLine("## Referensi Terverifikasi")
                bp.references.forEachIndexed { i, r -> appendLine("${i + 1}. $r") }
            }
        }
        outFile.writeText(text)
    }

    private fun writeCsv(outFile: File, bp: DocumentBlueprint) {
        val csv = buildString {
            appendLine("Bagian,Ringkasan,Poin_Kunci")
            appendLine("\"${csvEsc(bp.title)}\",\"${csvEsc(bp.executiveSummary)}\",\"Ringkasan Eksekutif\"")
            bp.sections.forEach { sec ->
                val bullets = sec.bullets.joinToString(" | ")
                appendLine("\"${csvEsc(sec.heading)}\",\"${csvEsc(sec.body)}\",\"${csvEsc(bullets)}\"")
            }
        }
        outFile.writeText(csv)
    }

    private fun csvEsc(s: String) = s.replace("\"", "\"\"").replace("\n", " ")

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
