package org.heroism.herocompass.assessment

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt as roundDoubleToInt
import java.io.ByteArrayOutputStream

internal data class IdentityOcrResult(
    val previewBitmap: Bitmap,
    val answers: List<Int?>,
    val confidence: List<Float>,
    val detectedAnswerCount: Int,
    val recognizedText: String,
    val sourcePage: Int = 1,
)

internal sealed interface IdentityOcrFailure {
    data object NotIdentityForeclosureForm : IdentityOcrFailure
    data class UnsupportedInstrument(val name: String) : IdentityOcrFailure
    data object ResponseGridNotFound : IdentityOcrFailure
    data class ReadError(val cause: Throwable) : IdentityOcrFailure
}

internal sealed interface IdentityOcrOutcome {
    data class Read(val result: IdentityOcrResult) : IdentityOcrOutcome
    data class Failed(val reason: IdentityOcrFailure) : IdentityOcrOutcome
}

/** Uses bundled on-device text recognition and the printed U-MICS answer grid; no image is written to disk. */
internal object IdentityForeclosureOcr {
    suspend fun read(
        context: Context,
        uri: Uri,
        expectedInstrument: String = IdentityInstrumentLabel,
        itemStatements: List<String> = IdentityForeclosureInstrument.items,
    ): IdentityOcrOutcome {
        require(itemStatements.isNotEmpty())
        val isPdf = context.contentResolver.getType(uri)?.equals("application/pdf", ignoreCase = true) == true ||
            uri.lastPathSegment?.endsWith(".pdf", ignoreCase = true) == true
        val page = try {
            if (isPdf) {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    findIdentityPageInPdf(context, uri, recognizer, expectedInstrument, itemStatements)
                } finally {
                    recognizer.close()
                }
            } else readIdentityImage(context, uri, expectedInstrument, itemStatements)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Log.e("AssessmentFormOcr", "Failed to read selected assessment image/document", error)
            return IdentityOcrOutcome.Failed(IdentityOcrFailure.ReadError(error))
        }
        if (page is PageRead.Failed) return IdentityOcrOutcome.Failed(page.reason)
        page as PageRead.Found
        val bitmap = page.bitmap
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val grid = IdentityResponseGridReader.read(
            bitmap.width,
            bitmap.height,
            pixels,
            expectedInstrument,
            page.scaleLabels,
            page.questionRowStarts,
            itemStatements,
        )
            ?: run {
                Log.w("AssessmentFormOcr", "Could not locate response grid ${bitmap.width}x${bitmap.height}; labels=${page.scaleLabels.map { it.value to it.centerX }} questionRows=${page.questionRowStarts}")
                bitmap.recycle()
                return IdentityOcrOutcome.Failed(IdentityOcrFailure.ResponseGridNotFound)
            }

        return IdentityOcrOutcome.Read(
            IdentityOcrResult(
                previewBitmap = bitmap,
                answers = grid.answers,
                confidence = grid.confidence,
                detectedAnswerCount = grid.answers.count { it != null },
                recognizedText = page.text,
                sourcePage = page.pageNumber,
            ),
        )
    }

    fun identifyInstrument(text: String): String? {
        val normalized = text.uppercase().replace(Regex("[^A-Z0-9]+"), " ").trim()
        return when {
            normalized.contains("IDENTITY FORECLOSURE") ||
                (normalized.contains("U MICS") && normalized.contains("IDENTITY")) -> "Identity Foreclosure (U-MICS)"
            normalized.contains("ORIENTASI MASA DEPAN") || normalized.contains("FUTURE ORIENTATION") -> "Orientasi Masa Depan"
            normalized.contains("FORETHOUGHT PHASE") || normalized.contains("HERO ASA") -> "HERO Asa · Forethought"
            normalized.contains("PERFORMANCE PHASE") || normalized.contains("HERO AKSI") -> "HERO Aksi · Performance"
            normalized.contains("REFLECTION PHASE") || normalized.contains("HERO RASA") -> "HERO Rasa · Reflection"
            normalized.contains("POSSIBLE SELVES") -> "Possible Selves"
            else -> null
        }
    }

    fun isIdentityForeclosureForm(text: String): Boolean = identifyInstrument(text) == IdentityInstrumentLabel

    private const val IdentityInstrumentLabel = "Identity Foreclosure (U-MICS)"

    private sealed interface PageRead {
        data class Found(
            val bitmap: Bitmap,
            val text: String,
            val pageNumber: Int,
            val scaleLabels: List<IdentityScaleLabel>,
            val questionRowStarts: List<Int>,
        ) : PageRead
        data class Failed(val reason: IdentityOcrFailure) : PageRead
    }

    internal data class IdentityScaleLabel(val value: Int, val centerX: Float)

    private suspend fun readIdentityImage(
        context: Context,
        uri: Uri,
        expectedInstrument: String,
        itemStatements: List<String>,
    ): PageRead {
        val input = InputImage.fromFilePath(context, uri)
        val bitmap = decodeAndRotate(context, uri, input.rotationDegrees)
            ?: return PageRead.Failed(IdentityOcrFailure.ReadError(IllegalArgumentException("Gambar tidak dapat dibaca.")))
        // Decode the user image before loading ML Kit's native text model. On memory-constrained
        // emulators the model initialization can otherwise make BitmapFactory fail to allocate.
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val recognized = try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        } finally {
            recognizer.close()
        }
        if (identifyInstrument(recognized.text) != expectedInstrument) {
            bitmap.recycle()
            val detected = identifyInstrument(recognized.text)
            return PageRead.Failed(detected?.let { IdentityOcrFailure.UnsupportedInstrument(it) }
                ?: IdentityOcrFailure.NotIdentityForeclosureForm)
        }
        val labels = readScaleLabels(recognized, bitmap.width, bitmap.height)
        return PageRead.Found(
            bitmap,
            recognized.text,
            1,
            labels,
            readQuestionRowStarts(recognized, bitmap.width, bitmap.height, labels, bitmap.width * .58f, itemStatements),
        )
    }

    private suspend fun findIdentityPageInPdf(
        context: Context,
        uri: Uri,
        recognizer: com.google.mlkit.vision.text.TextRecognizer,
        expectedInstrument: String,
        itemStatements: List<String>,
    ): PageRead {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return PageRead.Failed(IdentityOcrFailure.ReadError(IllegalArgumentException("PDF tidak dapat dibuka.")))
        var otherInstrument: String? = null
        descriptor.use { parcel ->
            PdfRenderer(parcel).use { renderer ->
                if (renderer.pageCount > 40) {
                    return PageRead.Failed(IdentityOcrFailure.ReadError(IllegalArgumentException("PDF terlalu panjang untuk dipindai sekaligus.")))
                }
                for (index in 0 until renderer.pageCount) {
                    val pageBitmap = renderer.openPage(index).use { pdfPage ->
                        val scale = 1900f / max(pdfPage.width, pdfPage.height).toFloat()
                        Bitmap.createBitmap(
                            max(1, (pdfPage.width * scale).toInt()),
                            max(1, (pdfPage.height * scale).toInt()),
                            Bitmap.Config.ARGB_8888,
                        ).also { bitmap -> pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                    }
                    val recognized = recognizer.process(InputImage.fromBitmap(pageBitmap, 0)).await()
                    val text = recognized.text
                    if (identifyInstrument(text) == expectedInstrument) {
                        val labels = readScaleLabels(recognized, pageBitmap.width, pageBitmap.height)
                        return PageRead.Found(
                            pageBitmap,
                            text,
                            index + 1,
                            labels,
                            readQuestionRowStarts(recognized, pageBitmap.width, pageBitmap.height, labels, pageBitmap.width * .58f, itemStatements),
                        )
                    }
                    identifyInstrument(text)?.let { if (otherInstrument == null) otherInstrument = it }
                    pageBitmap.recycle()
                }
            }
        }
        return PageRead.Failed(
            otherInstrument?.let { IdentityOcrFailure.UnsupportedInstrument(it) }
                ?: IdentityOcrFailure.NotIdentityForeclosureForm,
        )
    }

    private fun readScaleLabels(
        recognized: com.google.mlkit.vision.text.Text,
        width: Int,
        height: Int,
    ): List<IdentityScaleLabel> {
        val lines = recognized.textBlocks.flatMap { it.lines }
        val individualLabels = lines.mapNotNull { line ->
            val value = line.text.trim().toIntOrNull()?.takeIf { it in 1..4 } ?: return@mapNotNull null
            val bounds = line.boundingBox ?: return@mapNotNull null
            if (bounds.centerX() < width * .45f || bounds.centerX() > width * .9f || bounds.centerY() > height * .34f) return@mapNotNull null
            IdentityScaleLabel(value, bounds.exactCenterX())
        }
            .distinctBy { it.value }
        if (individualLabels.size >= 2) return individualLabels

        // ML Kit often returns the four small scale numbers as one line ("1 2 3 4"). Split its
        // box evenly across the answer grid instead of falling back to a dark-pixel threshold.
        val grouped = lines.firstNotNullOfOrNull { line ->
            val bounds = line.boundingBox ?: return@firstNotNullOfOrNull null
            val digits = line.text.filter(Char::isDigit)
            if (digits != "1234" || bounds.centerX() < width * .45f || bounds.centerY() > height * .34f ||
                bounds.width() !in (width * .12f).toInt()..(width * .5f).toInt()) return@firstNotNullOfOrNull null
            (1..4).map { value -> IdentityScaleLabel(value, bounds.left + bounds.width() * ((value - .5f) / 4f)) }
        }
        return grouped ?: individualLabels
    }

    private data class QuestionLine(val top: Int, val bottom: Int, val tokenScore: Double)

    internal fun readQuestionRowStarts(
        recognized: com.google.mlkit.vision.text.Text,
        width: Int,
        height: Int,
        scaleLabels: List<IdentityScaleLabel>,
        responseLeft: Float? = null,
        itemStatements: List<String> = IdentityForeclosureInstrument.items,
    ): List<Int> {
        if (scaleLabels.size < 2 && responseLeft == null) return emptyList()
        val headerBottom = if (scaleLabels.size < 2) 0 else scaleLabels.maxOf { label ->
            recognized.textBlocks.flatMap { it.lines }
                .firstOrNull { line -> line.text.trim().toIntOrNull() == label.value && line.boundingBox?.exactCenterX() == label.centerX }
                ?.boundingBox?.bottom ?: 0
        }
        // Let the OCR box include a statement row number if ML Kit merges it with the first words.
        val questionRight = if (scaleLabels.size >= 2) scaleLabels.minOf { it.centerX } - width * .02f else responseLeft!!
        val lines = recognized.textBlocks.flatMap { it.lines }.mapNotNull { line ->
            val bounds = line.boundingBox ?: return@mapNotNull null
            if (bounds.bottom <= headerBottom || bounds.left < width * .2f || bounds.right >= questionRight || bounds.top > height * .92f) return@mapNotNull null
            val tokens = line.text.lowercase().replace(Regex("[^a-z]+"), " ").trim().split(Regex("\\s+")).filter(String::isNotBlank).toSet()
            if (tokens.isEmpty()) return@mapNotNull null
            QuestionLine(bounds.top, bounds.bottom, 0.0)
                .let { candidate -> candidate.copy(tokenScore = tokens.size.toDouble()) to tokens }
        }
        if (lines.isEmpty()) {
            return emptyList()
        }
        val orderedLines = lines.sortedBy { it.first.top }
        val itemTokens = itemStatements.map { statement ->
            statement.lowercase().replace(Regex("[^a-z]+"), " ").trim().split(Regex("\\s+")).filter(String::isNotBlank).take(7).toSet()
        }
        val matchScores = itemTokens.map { expected ->
            orderedLines.map { (_, tokens) ->
                val shared = expected.count(tokens::contains)
                if (shared < 3) 0.0 else shared.toDouble() / expected.size
            }
        }
        val impossible = -1_000_000.0
        val scores = Array(itemTokens.size) { DoubleArray(orderedLines.size) { impossible } }
        val previous = Array(itemTokens.size) { IntArray(orderedLines.size) { -1 } }
        for (lineIndex in orderedLines.indices) {
            if (matchScores[0][lineIndex] >= .32) scores[0][lineIndex] = matchScores[0][lineIndex]
        }
        for (itemIndex in 1 until itemTokens.size) {
            for (lineIndex in orderedLines.indices) {
                val match = matchScores[itemIndex][lineIndex]
                if (match < .32) continue
                for (priorIndex in 0 until lineIndex) {
                    if (orderedLines[lineIndex].first.top <= orderedLines[priorIndex].first.top) continue
                    val priorScore = scores[itemIndex - 1][priorIndex]
                    if (priorScore <= impossible / 2) continue
                    if (priorScore + match > scores[itemIndex][lineIndex]) {
                        scores[itemIndex][lineIndex] = priorScore + match
                        previous[itemIndex][lineIndex] = priorIndex
                    }
                }
            }
        }
        val lastIndex = scores.last().indices.maxByOrNull { scores.last()[it] } ?: return emptyList()
        if (scores.last()[lastIndex] <= impossible / 2) return emptyList()
        val matches = IntArray(itemTokens.size)
        var cursor = lastIndex
        for (itemIndex in itemTokens.lastIndex downTo 0) {
            matches[itemIndex] = cursor
            if (itemIndex > 0) {
                cursor = previous[itemIndex][cursor]
                if (cursor < 0) return emptyList()
            }
        }
        return matches.map { orderedLines[it].first.top }
    }

    private fun decodeAndRotate(context: Context, uri: Uri, rotationDegrees: Int): Bitmap? {
        // Read the selected content URI once. Some document/photo providers return non-seekable
        // streams, so separate bounds and bitmap reads can fail even though the image is valid.
        val encodedImage = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > MaxEncodedImageBytes) {
                    throw IllegalArgumentException("Gambar terlalu besar untuk diproses.")
                }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(encodedImage, 0, encodedImage.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        val sampleSize = generateSequence(1) { it * 2 }
            .takeWhile { max(options.outWidth, options.outHeight) / it > 2000 }
            .lastOrNull() ?: 1
        val bitmap = BitmapFactory.decodeByteArray(
            encodedImage,
            0,
            encodedImage.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: return null
        if (rotationDegrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    private const val MaxEncodedImageBytes = 40 * 1024 * 1024
}

/** Reads ruled four-point response grids for all supported assessment instruments. */
internal object IdentityResponseGridReader {
    data class ReadResult(
        val answers: List<Int?>,
        val confidence: List<Float>,
    )

    fun read(
        width: Int,
        height: Int,
        pixels: IntArray,
        instrumentLabel: String = "Identity Foreclosure (U-MICS)",
        scaleLabels: List<IdentityForeclosureOcr.IdentityScaleLabel> = emptyList(),
        questionRowStarts: List<Int> = emptyList(),
        itemStatements: List<String> = IdentityForeclosureInstrument.items,
    ): ReadResult? {
        if (width < 320 || height < 480 || pixels.size != width * height) return null

        val columns = scaleBoundaries(scaleLabels, width) ?: run {
            val verticalSearchTop = roundToInt(height * .12f)
            val verticalSearchBottom = roundToInt(height * .93f)
            val verticalScores = DoubleArray(width) { x ->
                darkRatio(pixels, width, height, x, x + 1, verticalSearchTop, verticalSearchBottom)
            }.smooth(window = max(3, width / 220))
            selectColumnLines(verticalScores, width)
                ?: calibratedColumns(width, instrumentLabel).takeIf { itemStatements.size in setOf(10, 15) }
        } ?: run {
            Log.w("AssessmentGrid", "No four-column response grid found width=$width scaleLabels=${scaleLabels.map { it.value to it.centerX }}")
            return null
        }
        if (columns.size != 5) return null
        val columnWidths = columns.zipWithNext { a, b -> b - a }
        if (columnWidths.any { it < width * .025f || it > width * .10f }) {
            Log.w("AssessmentGrid", "Invalid response column widths=$columnWidths imageWidth=$width")
            return null
        }

        val horizontalScores = DoubleArray(height) { y ->
            darkRatio(pixels, width, height, columns.first(), columns.last(), y, y + 1, grayThreshold = 170)
        }.smooth(window = max(5, height / 160))
        val rowLines = run {
            if (questionRowStarts.size == itemStatements.size) {
                selectQuestionAnchoredRowLines(horizontalScores, height, questionRowStarts)
            } else null
        } ?: run {
            selectRowLines(horizontalScores, height, itemStatements.size + 1, pixels, width, columns.first(), requireStatementInk = true)
                ?: selectRowLines(horizontalScores, height, itemStatements.size + 1, pixels, width, columns.first())
        }
        // Use the printed horizontal grid lines when they were located. OCR statement boxes start
        // below each row's top border and can span very different heights as text wraps; treating
        // them as cell bounds clips marks near the row edge and shifts later rows.
        val rowRanges = if (rowLines != null) {
            rowLines.zipWithNext { top, bottom -> top to bottom }
        } else if (questionRowStarts.size == itemStatements.size) {
            val gaps = questionRowStarts.zipWithNext { a, b -> b - a }.sorted()
            val typicalGap = gaps[gaps.size / 2]
            questionRowStarts.mapIndexed { index, start ->
                val end = questionRowStarts.getOrNull(index + 1) ?: start + typicalGap
                max(0, start) to min(height, end)
            }
        } else {
            Log.w("AssessmentGrid", "No response row geometry rowLines=${rowLines?.size} questionRows=${questionRowStarts.size} itemCount=${itemStatements.size}")
            return null
        }
        if (rowRanges.any { (top, bottom) -> bottom - top < height * .012f || bottom - top > height * .10f }) {
            Log.w("AssessmentGrid", "Invalid response row ranges=$rowRanges imageHeight=$height")
            return null
        }

        val answers = mutableListOf<Int?>()
        val confidence = mutableListOf<Float>()
        for (row in itemStatements.indices) {
            val (top, bottom) = rowRanges[row]
            val scores = (0 until 4).map { column ->
                val left = columns[column]
                val right = columns[column + 1]
                val xInset = max(2, roundToInt((right - left) * .32f))
                val yInset = max(2, roundToInt((bottom - top) * .08f))
                adaptiveInkRatio(
                    pixels,
                    width,
                    height,
                    left + xInset,
                    right - xInset,
                    top + yInset,
                    bottom - yInset,
                )
            }
            val ranked = scores.withIndex().sortedByDescending { it.value }
            val best = ranked[0]
            val second = ranked[1].value
            val selected = best.value >= .012 && second <= best.value * .82
            answers += if (selected) best.index + 1 else null
            val separation = if (best.value <= 0.0) 0f else ((best.value - second) / best.value).toFloat()
            confidence += if (selected) {
                (0.55f + separation * .10f).coerceIn(0f, .99f)
            } else 0f
        }
        return ReadResult(answers, confidence)
    }

    internal fun inferScaleBoundaries(labels: List<IdentityForeclosureOcr.IdentityScaleLabel>, width: Int): List<Int>? =
        scaleBoundaries(labels, width)

    private fun calibratedColumns(width: Int, instrumentLabel: String): List<Int> {
        val fractions = when (instrumentLabel) {
            IdentityForeclosureInstrument.TITLE -> listOf(.628f, .713f, .798f, .883f, .968f)
            FutureOrientationInstrument.TITLE -> {
                // Calibrated against the printed response table in the Nurmi booklet page.
                listOf(.582f, .637f, .693f, .748f, .808f)
            }
            HeroAsaInstrument.TITLE -> listOf(.597f, .647f, .697f, .747f, .797f)
            HeroAksiInstrument.TITLE -> listOf(.620f, .673f, .724f, .778f, .832f)
            HeroRasaInstrument.TITLE -> listOf(.614f, .664f, .714f, .764f, .814f)
            else -> listOf(.60f, .655f, .71f, .765f, .82f)
        }
        return fractions.map { roundToInt(width * it) }
    }

    private fun scaleBoundaries(
        labels: List<IdentityForeclosureOcr.IdentityScaleLabel>,
        width: Int,
    ): List<Int>? {
        val ordered = labels.distinctBy { it.value }.sortedBy { it.value }
        if (ordered.size < 2) return null
        val meanValue = ordered.map { it.value.toFloat() }.average().toFloat()
        val meanX = ordered.map { it.centerX }.average().toFloat()
        val denominator = ordered.sumOf { (it.value - meanValue).let { diff -> (diff * diff).toDouble() } }
        if (denominator == 0.0) return null
        val gap = (ordered.sumOf { ((it.value - meanValue) * (it.centerX - meanX)).toDouble() } / denominator).toFloat()
        if (gap < width * .025f || gap > width * .12f) return null
        val intercept = meanX - gap * meanValue
        val boundaries = (0..4).map { index ->
            roundToInt(intercept + (index + .5f) * gap)
        }
        if (boundaries.first() < width * .35f || boundaries.last() > width * .99f) return null
        val widths = boundaries.zipWithNext { left, right -> right - left }
        if (widths.any { it < width * .025f || it > width * .12f }) return null
        return boundaries
    }

    private fun selectColumnLines(scores: DoubleArray, width: Int): List<Int>? {
        // The response grid occupies the right side of this instrument. Keep text strokes in
        // the statement column from becoming equally spaced false grid separators.
        val candidates = (roundToInt(width * .57f) until min(scores.lastIndex, roundToInt(width * .99f)))
            .filter { x -> scores[x] in .002..0.85 && scores[x] >= scores[x - 1] && scores[x] > scores[x + 1] }
            .map { RowCandidate(it, scores[it]) }
        if (candidates.size < 5) return null
        val expectedGap = width * .062
        val minimumGap = max(6, roundToInt(width * .025f))
        val maximumGap = roundToInt(width * .08f)
        val states = Array(5) { arrayOfNulls<RowPath>(candidates.size) }
        candidates.indices.forEach { index -> states[0][index] = RowPath(candidates[index].score, listOf(candidates[index].y)) }
        for (length in 1 until 5) {
            for (end in candidates.indices) {
                for (previous in 0 until end) {
                    val gap = candidates[end].y - candidates[previous].y
                    if (gap !in minimumGap..maximumGap) continue
                    val prior = states[length - 1][previous] ?: continue
                    val score = prior.score + candidates[end].score - .34 * kotlin.math.abs(gap - expectedGap) / expectedGap
                    val best = states[length][end]
                    if (best == null || score > best.score) states[length][end] = RowPath(score, prior.points + candidates[end].y)
                }
            }
        }
        return states.last().filterNotNull()
            .filter {
                it.points.first() >= roundToInt(width * .57f) &&
                    it.points.last() >= roundToInt(width * .80f) &&
                    it.points.last() - it.points.first() in roundToInt(width * .13f)..roundToInt(width * .34f)
            }
            .maxByOrNull { it.score }
            ?.points
    }

    private data class RowCandidate(val y: Int, val score: Double)
    private data class RowPath(val score: Double, val points: List<Int>)

    private fun selectQuestionAnchoredRowLines(scores: DoubleArray, height: Int, questionRowStarts: List<Int>): List<Int>? {
        val smoothRadius = max(4, height / 150)
        val candidates = (max(1, roundToInt(height * .08f)) until min(scores.lastIndex, roundToInt(height * .99f)))
            .filter { y ->
                scores[y] >= .12 && scores[y] >= scores[y - 1] && scores[y] > scores[y + 1] &&
                    (y - smoothRadius until y + smoothRadius).none { neighbor ->
                        neighbor in 1 until scores.lastIndex && neighbor != y && scores[neighbor] > scores[y]
                    }
            }
        val maxOffset = roundToInt(height * .10f)
        val rowTops = questionRowStarts.map { questionTop ->
            candidates.lastOrNull { it <= questionTop + 2 && questionTop - it <= maxOffset } ?: return null
        }
        if (rowTops.zipWithNext().any { (a, b) -> b <= a }) return null
        val bottom = candidates.firstOrNull { it > questionRowStarts.last() + 2 && it - questionRowStarts.last() <= maxOffset }
            ?: return null
        val boundaries = rowTops + bottom
        if (boundaries.zipWithNext().any { (top, end) ->
                end - top < height * .012f || end - top > height * .10f
            }) return null
        return boundaries
    }

    private fun selectRowLines(
        scores: DoubleArray,
        height: Int,
        lineCount: Int,
        pixels: IntArray,
        width: Int,
        responseLeft: Int,
        requireStatementInk: Boolean = false,
    ): List<Int>? {
        val smoothRadius = max(4, height / 150)
        val candidates = (max(1, roundToInt(height * .08f)) until min(scores.lastIndex, roundToInt(height * .99f)))
            .filter { y ->
                scores[y] >= .12 && scores[y] >= scores[y - 1] && scores[y] > scores[y + 1] &&
                    (y - smoothRadius until y + smoothRadius).none { neighbor ->
                        neighbor in 1 until scores.lastIndex && neighbor != y && scores[neighbor] > scores[y]
                    }
            }
            .map { RowCandidate(it, scores[it]) }
            .takeIf { it.size >= lineCount } ?: return null

        val expectedGap = height * .05
        val minimumGap = max(8, roundToInt(height * .012f))
        val maximumGap = roundToInt(height * .085f)
        val states = Array(lineCount) { arrayOfNulls<RowPath>(candidates.size) }
        candidates.indices.forEach { index -> states[0][index] = RowPath(candidates[index].score, listOf(candidates[index].y)) }
        for (length in 1 until lineCount) {
            for (end in candidates.indices) {
                val current = candidates[end]
                for (previous in 0 until end) {
                    val gap = current.y - candidates[previous].y
                    if (gap !in minimumGap..maximumGap) continue
                    val prior = states[length - 1][previous] ?: continue
                    val rowTop = candidates[previous].y + 2
                    val rowBottom = current.y - 2
                    val statementInk = if (rowBottom > rowTop) {
                        darkRatio(
                            pixels,
                            width,
                            height,
                            roundToInt(width * .20f),
                            responseLeft - roundToInt(width * .015f),
                            rowTop,
                            rowBottom,
                            grayThreshold = 130,
                        )
                    } else 0.0
                    if (requireStatementInk && statementInk < .015) continue
                    val rowTextScore = statementInk * 2.5 - if (statementInk < .015) .65 else 0.0
                    val pathScore = prior.score + current.score - .34 * kotlin.math.abs(gap - expectedGap) / expectedGap + rowTextScore
                    val best = states[length][end]
                    if (best == null || pathScore > best.score) {
                        states[length][end] = RowPath(pathScore, prior.points + current.y)
                    }
                }
            }
        }
        return states.last().filterNotNull()
            .filter { path ->
                val span = path.points.last() - path.points.first()
                span in roundToInt(height * .30f)..roundToInt(height * .92f)
            }
            .maxByOrNull { it.score }
            ?.points
    }

    private fun darkRatio(
        pixels: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        left: Int,
        right: Int,
        top: Int,
        bottom: Int,
        grayThreshold: Int = 96,
    ): Double {
        val x1 = left.coerceIn(0, imageWidth - 1)
        val x2 = right.coerceIn(x1 + 1, imageWidth)
        val y1 = top.coerceIn(0, imageHeight - 1)
        val y2 = bottom.coerceIn(y1 + 1, imageHeight)
        var dark = 0
        var count = 0
        for (y in y1 until y2) {
            val rowStart = y * imageWidth
            for (x in x1 until x2) {
                val color = pixels[rowStart + x]
                val red = color shr 16 and 0xff
                val green = color shr 8 and 0xff
                val blue = color and 0xff
                val gray = (red * 30 + green * 59 + blue * 11) / 100
                if (gray < grayThreshold) dark++
                count++
            }
        }
        return if (count == 0) 0.0 else dark.toDouble() / count
    }

    /** Counts pen strokes relative to the local paper brightness, so shaded photos do not bias a column. */
    private fun adaptiveInkRatio(
        pixels: IntArray,
        imageWidth: Int,
        imageHeight: Int,
        left: Int,
        right: Int,
        top: Int,
        bottom: Int,
    ): Double {
        val x1 = left.coerceIn(0, imageWidth - 1)
        val x2 = right.coerceIn(x1 + 1, imageWidth)
        val y1 = top.coerceIn(0, imageHeight - 1)
        val y2 = bottom.coerceIn(y1 + 1, imageHeight)
        val histogram = IntArray(256)
        var count = 0
        for (y in y1 until y2) {
            val rowStart = y * imageWidth
            for (x in x1 until x2) {
                val color = pixels[rowStart + x]
                val gray = ((color shr 16 and 0xff) * 30 + (color shr 8 and 0xff) * 59 + (color and 0xff) * 11) / 100
                histogram[gray]++
                count++
            }
        }
        if (count == 0) return 0.0
        val medianTarget = (count + 1) / 2
        var cumulative = 0
        var medianGray = 0
        for (gray in histogram.indices) {
            cumulative += histogram[gray]
            if (cumulative >= medianTarget) {
                medianGray = gray
                break
            }
        }
        val inkLimit = (medianGray - 12).coerceAtLeast(0)
        var ink = 0
        for (y in y1 until y2) {
            val rowStart = y * imageWidth
            for (x in x1 until x2) {
                val color = pixels[rowStart + x]
                val red = color shr 16 and 0xff
                val green = color shr 8 and 0xff
                val blue = color and 0xff
                val gray = (red * 30 + green * 59 + blue * 11) / 100
                val chroma = max(red, max(green, blue)) - min(red, min(green, blue))
                // Printed color artwork may sit behind response cells on booklet scans. Ignore
                // chromatic pixels so background illustrations do not become apparent pen marks.
                if (gray < inkLimit && chroma <= 48) ink++
            }
        }
        return ink.toDouble() / count
    }

    private fun peaks(
        scores: DoubleArray,
        start: Int,
        end: Int,
        minimumScore: Double,
        minimumDistance: Int,
        maxCount: Int,
    ): List<Int>? {
        val candidates = (max(1, start) until min(scores.lastIndex, end - 1))
            .filter { index -> scores[index] >= minimumScore && scores[index] >= scores[index - 1] && scores[index] > scores[index + 1] }
            .sortedByDescending { scores[it] }
        val selected = mutableListOf<Int>()
        for (candidate in candidates) {
            if (selected.none { kotlin.math.abs(it - candidate) < minimumDistance }) selected += candidate
            if (selected.size == maxCount) break
        }
        return selected.takeIf { it.size == maxCount }
    }

    private fun DoubleArray.smooth(window: Int): DoubleArray {
        if (window <= 1) return this
        val radius = window / 2
        return DoubleArray(size) { index ->
            val from = max(0, index - radius)
            val until = min(size, index + radius + 1)
            var total = 0.0
            for (i in from until until) total += this[i]
            total / (until - from)
        }
    }
}

private fun roundToInt(value: Float): Int = value.toDouble().roundDoubleToInt()

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result) }
        addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }
