package com.example

import com.example.data.SalesSheetParser
import com.example.model.Member
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SalesSheetParserUnitTest {

    @Test
    fun parseDelimitedText_parsesCsvWithBanglaDigitsAndHeaders() {
        val csv = """
            code,name,TP sales,TP+Vat
            C-101,রফিকুল ইসলাম,"50,000",57500
            C-102,করিম আহমেদ,৩৫০০০,৪০২৫০
        """.trimIndent()

        val result = SalesSheetParser.parseDelimitedText(csv, "2026-11")
        assertTrue(result.isSuccess)
        val rows = result.getOrThrow()
        assertEquals(2, rows.size)

        assertEquals("2026-11", rows[0].yearMonth)
        assertEquals("C-101", rows[0].code)
        assertEquals("রফিকুল ইসলাম", rows[0].name)
        assertEquals(50000.0, rows[0].tpSales, 0.001)
        assertEquals(57500.0, rows[0].tpPlusVat, 0.001)

        assertEquals("C-102", rows[1].code)
        assertEquals("করিম আহমেদ", rows[1].name)
        assertEquals(35000.0, rows[1].tpSales, 0.001)
        assertEquals(40250.0, rows[1].tpPlusVat, 0.001)
    }

    @Test
    fun parseDelimitedText_parsesTabSeparatedAndReorderedHeaderColumns() {
        val tsv = "name\tcode\tTP+Vat\tTP sales\nNetai Das\tN-01\t23000\t20000"
        val result = SalesSheetParser.parseDelimitedText(tsv, "2026-10")
        assertTrue(result.isSuccess)
        val row = result.getOrThrow().single()
        assertEquals("N-01", row.code)
        assertEquals("Netai Das", row.name)
        assertEquals(20000.0, row.tpSales, 0.001)
        assertEquals(23000.0, row.tpPlusVat, 0.001)
    }

    @Test
    fun parseBytes_parsesMinimalXlsxWorkbook() {
        val sharedStringsXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <si><t>code</t></si>
              <si><t>name</t></si>
              <si><t>TP sales</t></si>
              <si><t>TP+Vat</t></si>
              <si><t>M-201</t></si>
              <si><t>Sajib</t></si>
            </sst>
        """.trimIndent()

        val sheetXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>
                <row r="1">
                  <c r="A1" t="s"><v>0</v></c>
                  <c r="B1" t="s"><v>1</v></c>
                  <c r="C1" t="s"><v>2</v></c>
                  <c r="D1" t="s"><v>3</v></c>
                </row>
                <row r="2">
                  <c r="A2" t="s"><v>4</v></c>
                  <c r="B2" t="s"><v>5</v></c>
                  <c r="C2"><v>40000</v></c>
                  <c r="D2"><v>46000</v></c>
                </row>
              </sheetData>
            </worksheet>
        """.trimIndent()

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zos.write(sharedStringsXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(sheetXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val parsed = SalesSheetParser.parseBytes(baos.toByteArray(), "2026-11", "sales.xlsx")
        assertTrue(parsed.isSuccess)
        val row = parsed.getOrThrow().single()
        assertEquals("M-201", row.code)
        assertEquals("Sajib", row.name)
        assertEquals(40000.0, row.tpSales, 0.001)
        assertEquals(46000.0, row.tpPlusVat, 0.001)
    }

    @Test
    fun buildSampleCsv_usesMembersAndExpectedHeaders() {
        val members = listOf(
            Member(
                id = "m1",
                name = "Kamal",
                designation = "Sales",
                phone = "01711",
                depot = "Dhaka",
                code = "K-01"
            )
        )
        val csv = SalesSheetParser.buildSampleCsv(members).removePrefix("\uFEFF")
        val lines = csv.trim().lines()
        assertEquals("code,name,TP sales,TP+VAT sales", lines[0])
        assertEquals("\"K-01\",\"Kamal\",\"0\",\"0\"", lines[1])
    }

    @Test
    fun parseDelimitedText_parsesTpPlusVatSalesHeader() {
        val csv = """
            code,name,TP sales,TP+VAT sales
            C-301,নাহিদ হাসান,30000,34500
        """.trimIndent()
        val result = SalesSheetParser.parseDelimitedText(csv, "2026-11")
        assertTrue(result.isSuccess)
        val row = result.getOrThrow().single()
        assertEquals("C-301", row.code)
        assertEquals(30000.0, row.tpSales, 0.001)
        assertEquals(34500.0, row.tpPlusVat, 0.001)
    }

    @Test
    fun parseDelimitedText_doesNotFallbackTpPlusVatToTpSalesWhenBlank() {
        val csv = """
            code,name,TP sales,TP+Vat
            C-201,Jamal,25000,
        """.trimIndent()
        val result = SalesSheetParser.parseDelimitedText(csv, "2026-11")
        assertTrue(result.isSuccess)
        val row = result.getOrThrow().single()
        assertEquals(25000.0, row.tpSales, 0.001)
        assertEquals(0.0, row.tpPlusVat, 0.001)
    }
}
