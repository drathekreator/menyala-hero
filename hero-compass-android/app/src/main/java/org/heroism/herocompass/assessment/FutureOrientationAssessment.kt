package org.heroism.herocompass.assessment

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object FutureOrientationInstrument {
    const val CODE = "future_orientation"
    const val TITLE = "Orientasi Masa Depan"

    val items = listOf(
        "Saya sering memikirkan seperti apa kehidupan saya saat dewasa nanti.",
        "Saya mempunyai tujuan yang ingin saya capai di masa depan.",
        "Saya merasa masa depan saya penting untuk dipikirkan.",
        "Saya punya tujuan saat belajar sesuatu.",
        "Saya senang membayangkan masa depan.",
        "Saya percaya usaha saya sekarang penting untuk masa depan.",
        "Saya memikirkan apa yang perlu saya lakukan sekarang agar masa depan saya lebih baik.",
        "Saya mencoba hal-hal yang dapat membantu saya mencapai tujuan saya di masa depan.",
        "Saya ingin mencapai target yang saya buat sendiri.",
        "Saya merasa penting untuk mencapai tujuan yang sudah saya tetapkan.",
        "Saya ingin belajar agar bisa meraih cita-cita saya.",
        "Saya memikirkan perubahan yang ingin saya capai pada diri saya di masa depan.",
        "Saya berusaha menyelesaikan tugas dengan baik karena hal itu penting untuk masa depan saya.",
        "Ketika mengalami kesulitan, saya tetap berusaha karena saya memiliki tujuan yang ingin dicapai.",
        "Saya belajar meskipun tidak disuruh karena saya memikirkan masa depan saya.",
    )
}

enum class FutureOrientationCategory(val label: String) {
    LOW("Rendah"), MEDIUM("Sedang"), HIGH("Tinggi");

    companion object {
        fun fromScore(score: Int): FutureOrientationCategory? = when (score) {
            in 15..29 -> LOW
            in 30..44 -> MEDIUM
            in 45..60 -> HIGH
            else -> null
        }
    }
}

data class FutureOrientationAssessment(
    val id: String,
    val childId: String,
    val answers: List<Int?>,
    val totalScore: Int?,
    val category: FutureOrientationCategory?,
    val completedAtEpochMillis: Long?,
    val phase: AssessmentPhase = AssessmentPhase.PRE_TEST,
) {
    val isComplete: Boolean get() = totalScore != null && category != null && completedAtEpochMillis != null
}

sealed interface FutureOrientationSaveResult {
    data class SavedDraft(val assessment: FutureOrientationAssessment) : FutureOrientationSaveResult
    data class Completed(val assessment: FutureOrientationAssessment) : FutureOrientationSaveResult
    data object InvalidAnswers : FutureOrientationSaveResult
    data object AlreadyCompleted : FutureOrientationSaveResult
    data object StorageError : FutureOrientationSaveResult
}

interface FutureOrientationRepository {
    fun get(childId: String, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): FutureOrientationAssessment?
    fun mergeImported(records: List<FutureOrientationAssessment>): Boolean = false
    fun deleteForChild(childId: String): Boolean = false
    fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): FutureOrientationSaveResult
    fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): FutureOrientationSaveResult
}

object FutureOrientationRules {
    fun validateAnswers(answers: List<Int?>): Boolean =
        answers.size == FutureOrientationInstrument.items.size && answers.all { it == null || it in 1..4 }

    fun score(answers: List<Int?>): Int? {
        if (!validateAnswers(answers) || answers.any { it == null }) return null
        return answers.sumOf { it!! }
    }

    fun category(score: Int): FutureOrientationCategory? = FutureOrientationCategory.fromScore(score)
}

/** Stores item-level responses and pre/post results separately in an AES-GCM encrypted local file. */
class EncryptedFutureOrientationRepository(context: Context) : FutureOrientationRepository {
    private val file = File(context.applicationContext.filesDir, "future_orientation_assessments.enc")
    private val keyAlias = "hero_compass_future_orientation_v1"

    override fun get(childId: String, phase: AssessmentPhase): FutureOrientationAssessment? =
        readAll().firstOrNull { it.childId == childId && it.phase == phase }

    override fun mergeImported(records: List<FutureOrientationAssessment>): Boolean = runCatching {
        require(records.all { record ->
            record.id.isNotBlank() && record.childId.isNotBlank() && FutureOrientationRules.validateAnswers(record.answers) &&
                if (record.isComplete) {
                    FutureOrientationRules.score(record.answers) == record.totalScore &&
                        FutureOrientationRules.category(record.totalScore!!) == record.category && record.completedAtEpochMillis != null
                } else record.totalScore == null && record.category == null && record.completedAtEpochMillis == null
        })
        require(records.map { it.childId to it.phase }.distinct().size == records.size)
        val existing = readAll()
        require((existing.map { it.childId to it.phase }.toSet() intersect records.map { it.childId to it.phase }.toSet()).isEmpty())
        write(existing + records)
        true
    }.getOrDefault(false)

    override fun deleteForChild(childId: String): Boolean = runCatching {
        if (childId.isBlank()) return false
        write(readAll().filterNot { it.childId == childId })
        true
    }.getOrDefault(false)

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): FutureOrientationSaveResult {
        if (childId.isBlank() || !FutureOrientationRules.validateAnswers(answers)) return FutureOrientationSaveResult.InvalidAnswers
        return save(childId, phase) { id -> FutureOrientationAssessment(id, childId, answers.toList(), null, null, null, phase) }
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): FutureOrientationSaveResult {
        val score = FutureOrientationRules.score(answers) ?: return FutureOrientationSaveResult.InvalidAnswers
        val category = FutureOrientationRules.category(score) ?: return FutureOrientationSaveResult.InvalidAnswers
        return save(childId, phase) { id ->
            FutureOrientationAssessment(id, childId, answers.toList(), score, category, System.currentTimeMillis(), phase)
        }
    }

    private fun save(
        childId: String,
        phase: AssessmentPhase,
        create: (String) -> FutureOrientationAssessment,
    ): FutureOrientationSaveResult = runCatching {
        val records = readAll().toMutableList()
        val index = records.indexOfFirst { it.childId == childId && it.phase == phase }
        if (index >= 0 && records[index].isComplete) return FutureOrientationSaveResult.AlreadyCompleted
        val assessment = create(records.getOrNull(index)?.id ?: UUID.randomUUID().toString())
        if (index >= 0) records[index] = assessment else records += assessment
        write(records)
        if (assessment.isComplete) FutureOrientationSaveResult.Completed(assessment)
        else FutureOrientationSaveResult.SavedDraft(assessment)
    }.getOrElse { FutureOrientationSaveResult.StorageError }

    private fun readAll(): List<FutureOrientationAssessment> {
        if (!file.exists()) return emptyList()
        val root = JSONObject(String(decrypt(file.readBytes()), Charsets.UTF_8))
        require(root.getInt("schema") == 1) { "Unsupported future orientation assessment schema" }
        val array = root.getJSONArray("assessments")
        return (0 until array.length()).map { index ->
            val value = array.getJSONObject(index)
            val answers = value.getJSONArray("answers")
            val score = if (value.isNull("totalScore")) null else value.getInt("totalScore")
            FutureOrientationAssessment(
                id = value.getString("id"),
                childId = value.getString("childId"),
                answers = (0 until answers.length()).map { answerIndex ->
                    if (answers.isNull(answerIndex)) null else answers.getInt(answerIndex)
                },
                totalScore = score,
                category = score?.let(FutureOrientationCategory::fromScore),
                completedAtEpochMillis = if (value.isNull("completedAt")) null else value.getLong("completedAt"),
                phase = AssessmentPhase.valueOf(value.getString("phase")),
            )
        }
    }

    private fun write(records: List<FutureOrientationAssessment>) {
        val array = JSONArray().apply {
            records.forEach { record ->
                put(JSONObject()
                    .put("id", record.id)
                    .put("childId", record.childId)
                    .put("answers", JSONArray().apply { record.answers.forEach { put(it ?: JSONObject.NULL) } })
                    .put("totalScore", record.totalScore ?: JSONObject.NULL)
                    .put("completedAt", record.completedAtEpochMillis ?: JSONObject.NULL)
                    .put("phase", record.phase.name))
            }
        }
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeBytes(encrypt(JSONObject().put("schema", 1).put("assessments", array).toString().toByteArray(Charsets.UTF_8)))
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return cipher.iv + cipher.doFinal(plainText)
    }

    private fun decrypt(encrypted: ByteArray): ByteArray {
        require(encrypted.size > 12) { "Encrypted assessment file is incomplete" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, encrypted.copyOfRange(0, 12)))
        return cipher.doFinal(encrypted.copyOfRange(12, encrypted.size))
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
            generateKey()
        }
    }
}
