package com.example.data

import com.example.model.Member
import com.example.model.MonthlySalesRecord
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

object SalesSheetParser {

    private val banglaToAsciiDigits = mapOf(
        '\u09E6' to '0',
        '\u09E7' to '1',
        '\u09E8' to '2',
        '\u09E9' to '3',
        '\u09EA' to '4',
        '\u09EB' to '5',
        '\u09EC' to '6',
        '\u09ED' to '7',
        '\u09EE' to '8',
        '\u09EF' to '9'
    )

    fun parseBytes(
        bytes: ByteArray,
        yearMonth: String,
        fileName: String? = null
    ): Result<List<MonthlySalesRecord>> {
        if (bytes.isEmpty()) {
            return Result.failure(IllegalArgumentException("The selected file is empty."))
        }
        val isZipXlsx = (bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) ||
            fileName.orEmpty().endsWith(".xlsx", ignoreCase = true)

        return if (isZipXlsx) {
            parseXlsxBytes(bytes, yearMonth)
        } else {
            val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
            parseDelimitedText(text, yearMonth)
        }
    }

    fun parseDelimitedText(
        rawText: String,
        yearMonth: String
    ): Result<List<MonthlySalesRecord>> {
        val cleaned = rawText.removePrefix("\uFEFF").trim()
        if (cleaned.isEmpty()) {
            return Result.failure(IllegalArgumentException("No sales data found."))
        }

        val lines = splitLogicalLines(cleaned).filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return Result.failure(IllegalArgumentException("No sales rows found."))
        }

        val delimiter = detectDelimiter(lines.first())
        val tableRows = lines.map { parseDelimitedRow(it, delimiter) }
        return mapRowsToRecords(tableRows, yearMonth)
    }

    fun parseXlsxBytes(
        bytes: ByteArray,
        yearMonth: String
    ): Result<List<MonthlySalesRecord>> {
        return runCatching {
            val sharedStrings = mutableListOf<String>()
            var sheetXml: ByteArray? = null

            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (!entry.isDirectory) {
                        when {
                            entryName.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
                                sharedStrings.addAll(parseSharedStringsXml(zis.readBytes()))
                            }
                            entryName.equals("xl/worksheets/sheet1.xml", ignoreCase = true) -> {
                                sheetXml = zis.readBytes()
                            }
                            sheetXml == null &&
                                entryName.startsWith("xl/worksheets/sheet", ignoreCase = true) &&
                                entryName.endsWith(".xml", ignoreCase = true) -> {
                                sheetXml = zis.readBytes()
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val xmlBytes = sheetXml
                ?: throw IllegalArgumentException("No data found in the Excel sheet. Please use a CSV file.")

            val rows = parseWorksheetXml(xmlBytes, sharedStrings)
            mapRowsToRecords(rows, yearMonth).getOrThrow()
        }
    }

    fun buildSampleCsv(members: List<Member>): String {
        val sb = StringBuilder("\uFEFF")
        sb.append("code,name,TP sales,TP+VAT sales\n")
        if (members.isNotEmpty()) {
            for (member in members) {
                val fields = listOf(
                    member.code.ifBlank { "" },
                    member.name,
                    "0",
                    "0"
                )
                sb.append(fields.joinToString(",") { escapeCsv(it) })
                sb.append('\n')
            }
        } else {
            sb.append("\"C-101\",\"Rafiqul Islam\",\"50000\",\"57500\"\n")
            sb.append("\"C-102\",\"Karim Ahmed\",\"35000\",\"40250\"\n")
        }
        return sb.toString()
    }

    fun buildSalesCsv(records: List<MonthlySalesRecord>): String {
        val sb = StringBuilder("\uFEFF")
        sb.append("code,name,TP sales,TP+VAT sales\n")
        for (item in records) {
            val fields = listOf(
                item.code,
                item.name,
                formatPlainNumber(item.tpSales),
                formatPlainNumber(item.tpPlusVat)
            )
            sb.append(fields.joinToString(",") { escapeCsv(it) })
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun formatPlainNumber(value: Double): String {
        val asLong = value.toLong()
        return if (value == asLong.toDouble()) asLong.toString() else value.toString()
    }

    private fun mapRowsToRecords(
        rows: List<List<String>>,
        yearMonth: String
    ): Result<List<MonthlySalesRecord>> {
        val nonEmptyRows = rows.filter { row -> row.any { it.isNotBlank() } }
        if (nonEmptyRows.isEmpty()) {
            return Result.failure(IllegalArgumentException("No sales rows found."))
        }

        val firstRow = nonEmptyRows.first()
        val columnMap = detectHeaderColumnMap(firstRow)
        val dataStartIndex = if (columnMap != null) 1 else 0
        val codeIdx = columnMap?.codeIdx ?: 0
        val nameIdx = columnMap?.nameIdx ?: 1
        val tpSalesIdx = columnMap?.tpSalesIdx ?: 2
        val tpVatIdx = columnMap?.tpVatIdx ?: 3

        val records = mutableListOf<MonthlySalesRecord>()
        val baseTime = System.currentTimeMillis()
        for (i in dataStartIndex until nonEmptyRows.size) {
            val row = nonEmptyRows[i]
            val code = row.getOrNull(codeIdx)?.trim().orEmpty()
            val name = row.getOrNull(nameIdx)?.trim().orEmpty()
            val rawTpSales = row.getOrNull(tpSalesIdx).orEmpty()
            val rawTpVat = row.getOrNull(tpVatIdx).orEmpty()

            if (code.isBlank() && name.isBlank()) continue
            val tpSales = parseAmount(rawTpSales)
            val tpPlusVat = parseAmount(rawTpVat)
            if (tpSales == null && tpPlusVat == null) continue

            val resolvedTpSales = (tpSales ?: 0.0).coerceAtLeast(0.0)
            val resolvedTpPlusVat = (tpPlusVat ?: 0.0).coerceAtLeast(0.0)
            val resolvedName = name.ifBlank { code }

            records.add(
                MonthlySalesRecord(
                    id = UUID.randomUUID().toString(),
                    yearMonth = yearMonth,
                    code = code,
                    name = resolvedName,
                    tpSales = resolvedTpSales,
                    tpPlusVat = resolvedTpPlusVat,
                    createdAtMs = baseTime + i
                )
            )
        }

        return if (records.isEmpty()) {
            Result.failure(
                IllegalArgumentException("No valid records found in (code, name, TP sales, TP+VAT sales) format.")
            )
        } else {
            Result.success(records)
        }
    }

    private data class ColumnMap(
        val codeIdx: Int,
        val nameIdx: Int,
        val tpSalesIdx: Int,
        val tpVatIdx: Int
    )

    private fun detectHeaderColumnMap(headerRow: List<String>): ColumnMap? {
        var codeIdx = -1
        var nameIdx = -1
        var tpSalesIdx = -1
        var tpVatIdx = -1

        headerRow.forEachIndexed { idx, rawCell ->
            val norm = rawCell.trim().lowercase()
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "")
            when {
                norm == "code" || norm == "membercode" || norm == "\u0995\u09CB\u09A1" -> codeIdx = idx
                norm == "name" || norm == "membername" || norm == "\u09A8\u09BE\u09AE" -> nameIdx = idx
                norm == "tpsales" || norm == "tpsale" || norm == "tp" -> tpSalesIdx = idx
                norm == "tp+vat" ||
                    norm == "tpvat" ||
                    norm == "tp+vatsales" ||
                    norm == "tpvatsales" ||
                    norm == "tp+vatsale" ||
                    norm == "tpvatsale" ||
                    norm == "tpwithvat" ||
                    norm == "tpwithvatsales" ||
                    norm == "vat" -> tpVatIdx = idx
            }
        }

        val recognizedCount = listOf(codeIdx, nameIdx, tpSalesIdx, tpVatIdx).count { it >= 0 }
        if (recognizedCount < 2) return null

        return ColumnMap(
            codeIdx = if (codeIdx >= 0) codeIdx else 0,
            nameIdx = if (nameIdx >= 0) nameIdx else 1,
            tpSalesIdx = if (tpSalesIdx >= 0) tpSalesIdx else 2,
            tpVatIdx = if (tpVatIdx >= 0) tpVatIdx else 3
        )
    }

    fun parseAmount(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val normalized = buildString {
            for (ch in raw.trim()) {
                when {
                    ch in banglaToAsciiDigits -> append(banglaToAsciiDigits[ch])
                    ch in '0'..'9' || ch == '.' || ch == '-' -> append(ch)
                }
            }
        }
        if (normalized.isBlank() || normalized == "-" || normalized == ".") return null
        return normalized.toDoubleOrNull()?.takeIf { !it.isNaN() && it >= 0.0 }
    }

    private fun splitLogicalLines(text: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && i + 1 < text.length && text[i + 1] == '\n') {
                        i++
                    }
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        if (current.isNotEmpty()) {
            result.add(current.toString())
        }
        return result
    }

    private fun detectDelimiter(sampleLine: String): Char {
        var inQuotes = false
        var commas = 0
        var tabs = 0
        var semis = 0
        for (ch in sampleLine) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                !inQuotes && ch == ',' -> commas++
                !inQuotes && ch == '\t' -> tabs++
                !inQuotes && ch == ';' -> semis++
            }
        }
        return when {
            tabs >= commas && tabs >= semis && tabs > 0 -> '\t'
            semis > commas && semis > tabs -> ';'
            else -> ','
        }
    }

    private fun parseDelimitedRow(line: String, delimiter: Char): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> {
                    cells.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        cells.add(current.toString().trim())
        return cells
    }

    private fun parseSharedStringsXml(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        val siNodes = doc.getElementsByTagName("si")
        for (i in 0 until siNodes.length) {
            val siElem = siNodes.item(i) as? Element ?: continue
            val tNodes = siElem.getElementsByTagName("t")
            val text = buildString {
                for (j in 0 until tNodes.length) {
                    append(tNodes.item(j).textContent.orEmpty())
                }
            }
            strings.add(text)
        }
        return strings
    }

    private fun parseWorksheetXml(
        bytes: ByteArray,
        sharedStrings: List<String>
    ): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
        }
        val doc = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        val rowNodes = doc.getElementsByTagName("row")
        for (i in 0 until rowNodes.length) {
            val rowElem = rowNodes.item(i) as? Element ?: continue
            val currentRow = mutableListOf<String>()
            val cellNodes = rowElem.getElementsByTagName("c")
            for (j in 0 until cellNodes.length) {
                val cellElem = cellNodes.item(j) as? Element ?: continue
                val ref = cellElem.getAttribute("r").orEmpty()
                val colIdx = columnIndexFromCellRef(ref, fallback = currentRow.size)
                val cellType = cellElem.getAttribute("t").orEmpty()

                val rawVal = if (cellType == "inlineStr") {
                    val tNodes = cellElem.getElementsByTagName("t")
                    buildString {
                        for (k in 0 until tNodes.length) {
                            append(tNodes.item(k).textContent.orEmpty())
                        }
                    }.trim()
                } else {
                    val vNodes = cellElem.getElementsByTagName("v")
                    if (vNodes.length > 0) vNodes.item(0).textContent.orEmpty().trim() else ""
                }

                val resolved = if (cellType == "s") {
                    val idx = rawVal.toIntOrNull()
                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                } else {
                    rawVal
                }

                while (currentRow.size <= colIdx) {
                    currentRow.add("")
                }
                currentRow[colIdx] = resolved.trim()
            }
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow)
            }
        }
        return rows
    }

    private fun columnIndexFromCellRef(cellRef: String, fallback: Int): Int {
        val letters = cellRef.takeWhile { it.isLetter() }.uppercase()
        if (letters.isEmpty()) return fallback
        var col = 0
        for (ch in letters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return (col - 1).coerceAtLeast(0)
    }
}
