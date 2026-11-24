package org.heroism.herocompass.assessment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityForeclosureOcrTest {
    @Test
    fun recognizesIdentityForeclosureSheetByItsPrintedTitle() {
        assertTrue(IdentityForeclosureOcr.isIdentityForeclosureForm("INSTRUMEN PENILAIAN IDENTITY FORECLOSURE"))
        assertTrue(IdentityForeclosureOcr.isIdentityForeclosureForm("U-MICS Identity Commitments Scale"))
        assertFalse(IdentityForeclosureOcr.isIdentityForeclosureForm("Instrumen Penilaian Orientasi Masa Depan"))
        assertEquals("HERO Asa · Forethought", IdentityForeclosureOcr.identifyInstrument("HERO Asa (FORETHOUGHT PHASE)"))
        assertEquals("HERO Aksi · Performance", IdentityForeclosureOcr.identifyInstrument("HERO Aksi (PERFORMANCE PHASE)"))
        assertEquals("HERO Rasa · Reflection", IdentityForeclosureOcr.identifyInstrument("HERO Rasa (REFLECTION PHASE)"))
        assertEquals("Orientasi Masa Depan", IdentityForeclosureOcr.identifyInstrument("ORIENTASI MASA DEPAN"))
    }

    @Test
    fun scaleLabelsLocateAllFourResponseColumnsOnScannedPage() {
        val labels = listOf(
            IdentityForeclosureOcr.IdentityScaleLabel(value = 1, centerX = 617f),
            IdentityForeclosureOcr.IdentityScaleLabel(value = 2, centerX = 661.5f),
        )

        assertEquals(listOf(595, 639, 684, 728, 773), IdentityResponseGridReader.inferScaleBoundaries(labels, 1020))
    }

    @Test
    fun blankGridLeavesAnswersUnselectedInsteadOfGuessing() {
        val fixture = FormGridFixture()

        val result = IdentityResponseGridReader.read(fixture.width, fixture.height, fixture.pixels)

        assertNotNull(result)
        assertEquals(List(15) { null }, result!!.answers)
        assertEquals(List(15) { 0f }, result.confidence)
    }

    @Test
    fun oneClearMarkIsReadAsThatScaleAndOtherRowsStayEmpty() {
        val fixture = FormGridFixture().apply { mark(row = 6, column = 2) }

        val result = IdentityResponseGridReader.read(fixture.width, fixture.height, fixture.pixels)

        assertNotNull(result)
        assertEquals(3, result!!.answers[6])
        assertTrue(result.answers.count { it != null } == 1)
        assertTrue(result.confidence[6] > 0.6f)
    }

    @Test
    fun twoMarkedChoicesInOneRowAreLeftForManualCorrection() {
        val fixture = FormGridFixture().apply {
            mark(row = 2, column = 0)
            mark(row = 2, column = 1)
        }

        val result = IdentityResponseGridReader.read(fixture.width, fixture.height, fixture.pixels)

        assertNotNull(result)
        assertNull(result!!.answers[2])
    }

    @Test
    fun shiftedAndUnevenlySpacedRowsAreFoundWithoutAssumingFixedPagePosition() {
        val rowGaps = listOf(37, 53, 40, 52, 39, 48, 43, 46, 41, 54, 37, 51, 40, 49, 42)
        val lines = mutableListOf(330).apply { rowGaps.forEach { add(last() + it) } }
        val fixture = FormGridFixture(rowLines = lines).apply {
            mark(row = 0, column = 0)
            mark(row = 7, column = 2)
            mark(row = 14, column = 3)
        }

        val result = IdentityResponseGridReader.read(fixture.width, fixture.height, fixture.pixels)

        assertNotNull(result)
        assertEquals(1, result!!.answers[0])
        assertEquals(3, result.answers[7])
        assertEquals(4, result.answers[14])
        assertEquals(3, result.answers.count { it != null })
    }

    @Test
    fun tenItemSrlFormUsesItsOwnRowCountAndStatementAnchors() {
        val statements = HeroRasaInstrument.items
        val fixture = FormGridFixture(rowLines = (0..10).map { 360 + it * 78 }).apply {
            mark(row = 0, column = 0)
            mark(row = 4, column = 2)
            mark(row = 9, column = 3)
        }

        val result = IdentityResponseGridReader.read(
            fixture.width,
            fixture.height,
            fixture.pixels,
            questionRowStarts = (0..9).map { 366 + it * 78 },
            itemStatements = statements,
        )

        assertNotNull(result)
        assertEquals(10, result!!.answers.size)
        assertEquals(1, result.answers[0])
        assertEquals(3, result.answers[4])
        assertEquals(4, result.answers[9])
        assertEquals(3, result.answers.count { it != null })
    }

    private class FormGridFixture(private val rowLines: List<Int> = (0..15).map { 420 + it * 50 }) {
        val width = 1000
        val height = 1400
        val pixels = IntArray(width * height) { White }
        private val columnLines = listOf(600, 655, 710, 765, 820)

        init {
            rowLines.forEach { y -> horizontal(y) }
            columnLines.forEach { x -> vertical(x) }
        }

        fun mark(row: Int, column: Int) {
            val left = columnLines[column] + 12
            val top = rowLines[row] + 10
            line(left + 5, top + 10, left + 15, top + 23)
            line(left + 15, top + 23, left + 38, top + 5)
        }

        private fun horizontal(y: Int) {
            for (x in 200..900) pixel(x, y)
        }

        private fun vertical(x: Int) {
            for (y in rowLines.first()..rowLines.last()) pixel(x, y)
        }

        private fun line(x1: Int, y1: Int, x2: Int, y2: Int) {
            val steps = maxOf(kotlin.math.abs(x2 - x1), kotlin.math.abs(y2 - y1))
            for (step in 0..steps) {
                val x = x1 + (x2 - x1) * step / steps
                val y = y1 + (y2 - y1) * step / steps
                pixel(x, y)
            }
        }

        private fun pixel(x: Int, y: Int) {
            if (x in 0 until width && y in 0 until height) pixels[y * width + x] = Black
        }
    }

    private companion object {
        const val White = -1
        const val Black = -0x1000000
    }
}
