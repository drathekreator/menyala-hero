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

object IdentityForeclosureInstrument {
    const val CODE = "u_mics_identity_foreclosure"
    const val TITLE = "Identity Foreclosure (U-MICS)"
    val items = listOf(
        "Saya ingin mencoba banyak cita-cita yang berbeda.",
        "Saya pernah berpikir ingin menjadi sesuatu selain pekerjaan di sekitar saya.",
        "Saya merasa bebas memilih cita-cita.",
        "Saya ingin menentukan sendiri pekerjaan yang saya inginkan saat dewasa.",
        "Saya ingin punya banyak pilihan saat menentukan masa depan.",
        "Saya merasa keputusan tentang masa depan saya tidak harus mengikuti pilihan orang lain.",
        "Saya ingin punya masa depan yang berbeda dari sekarang.",
        "Saya ingin tahu pekerjaan lain selain yang sering saya lihat.",
        "Saya ingin mengetahui lebih banyak pekerjaan yang bisa saya pilih ketika dewasa.",
        "Saya ingin menjadi seperti orang tua saya (menjadi pemulung).",
        "Saya ingin tahu apa kelebihan saya.",
        "Saya ingin mencari tahu hal apa yang saya sukai.",
        "Saya berani bermimpi menjadi sesuatu yang berbeda.",
        "Saya ingin mengenal orang dengan pekerjaan yang berbeda-beda.",
        "Saya ingin mengetahui berbagai pilihan masa depan sebelum menentukan cita-cita saya.",
    )
}

enum class IdentityForeclosureCategory(val label: String) {
    LOW("Rendah"), MEDIUM("Sedang"), HIGH("Tinggi");

    companion object {
        fun fromScore(score: Int): IdentityForeclosureCategory? = when (score) {
            in 45..60 -> LOW
            in 30..44 -> MEDIUM
            in 15..29 -> HIGH
            else -> null
        }
    }
}

enum class AssessmentPhase(val displayName: String) {
    PRE_TEST("Pre-test"), POST_TEST("Post-test")
}

data class IdentityForeclosureAssessment(
    val id: String,
    val childId: String,
    val answers: List<Int?>,
    val totalScore: Int?,
    val category: IdentityForeclosureCategory?,
    val completedAtEpochMillis: Long?,
    val phase: AssessmentPhase = AssessmentPhase.PRE_TEST,
) {
    val isComplete: Boolean get() = totalScore != null && category != null && completedAtEpochMillis != null
}

sealed interface AssessmentSaveResult {
    data class SavedDraft(val assessment: IdentityForeclosureAssessment) : AssessmentSaveResult
    data class Completed(val assessment: IdentityForeclosureAssessment) : AssessmentSaveResult
    data object InvalidAnswers : AssessmentSaveResult
    data object AlreadyCompleted : AssessmentSaveResult
    data object StorageError : AssessmentSaveResult
}

interface IdentityForeclosureRepository {
    fun get(childId: String, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): IdentityForeclosureAssessment?
    fun mergeImported(records: List<IdentityForeclosureAssessment>): Boolean = false
    fun deleteForChild(childId: String): Boolean = false
    fun saveDraft(
        childId: String,
        answers: List<Int?>,
        phase: AssessmentPhase = AssessmentPhase.PRE_TEST,
    ): AssessmentSaveResult
    fun complete(
        childId: String,
        answers: List<Int?>,
        phase: AssessmentPhase = AssessmentPhase.PRE_TEST,
    ): AssessmentSaveResult
}

object IdentityForeclosureRules {
    fun validateAnswers(answers: List<Int?>): Boolean =
        answers.size == IdentityForeclosureInstrument.items.size && answers.all { it == null || it in 1..4 }

    fun score(answers: List<Int?>): Int? {
        if (!validateAnswers(answers) || answers.any { it == null }) return null
        return answers.sumOf { it!! }
    }

    fun category(score: Int): IdentityForeclosureCategory? = IdentityForeclosureCategory.fromScore(score)
}

/** Local encrypted storage for one pre-test per child; item-level answers are retained for audit/recalculation. */
class EncryptedIdentityForeclosureRepository(context: Context) : IdentityForeclosureRepository {
    private val file = File(context.applicationContext.filesDir, "identity_foreclosure_assessments.enc")
    private val keyAlias = "hero_compass_identity_foreclosure_v1"

    override fun get(childId: String, phase: AssessmentPhase): IdentityForeclosureAssessment? =
        readAll().firstOrNull { it.childId == childId && it.phase == phase }

    override fun mergeImported(records: List<IdentityForeclosureAssessment>): Boolean = runCatching {
        require(records.all { record ->
            record.id.isNotBlank() && record.childId.isNotBlank() && IdentityForeclosureRules.validateAnswers(record.answers) &&
                if (record.isComplete) {
                    IdentityForeclosureRules.score(record.answers) == record.totalScore &&
                        IdentityForeclosureRules.category(record.totalScore!!) == record.category && record.completedAtEpochMillis != null
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

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): AssessmentSaveResult {
        if (childId.isBlank() || !IdentityForeclosureRules.validateAnswers(answers)) return AssessmentSaveResult.InvalidAnswers
        return save(childId, phase) { id -> IdentityForeclosureAssessment(id, childId, answers.toList(), null, null, null, phase) }
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): AssessmentSaveResult {
        val score = IdentityForeclosureRules.score(answers)
            ?: return AssessmentSaveResult.InvalidAnswers
        val category = IdentityForeclosureRules.category(score)
            ?: return AssessmentSaveResult.InvalidAnswers
        return save(childId, phase) { id ->
            IdentityForeclosureAssessment(id, childId, answers.toList(), score, category, System.currentTimeMillis(), phase)
        }
    }

    private fun save(childId: String, phase: AssessmentPhase, create: (String) -> IdentityForeclosureAssessment): AssessmentSaveResult = runCatching {
        val records = readAll().toMutableList()
        val index = records.indexOfFirst { it.childId == childId && it.phase == phase }
        if (index >= 0 && records[index].isComplete) return AssessmentSaveResult.AlreadyCompleted
        val assessment = create(records.getOrNull(index)?.id ?: UUID.randomUUID().toString())
        if (index >= 0) records[index] = assessment else records += assessment
        write(records)
        if (assessment.isComplete) AssessmentSaveResult.Completed(assessment) else AssessmentSaveResult.SavedDraft(assessment)
    }.getOrElse { AssessmentSaveResult.StorageError }

    private fun readAll(): List<IdentityForeclosureAssessment> {
        if (!file.exists()) return emptyList()
        val root = JSONObject(String(decrypt(file.readBytes()), Charsets.UTF_8))
        val schema = root.getInt("schema")
        require(schema in 1..2) { "Unsupported assessment schema" }
        val array = root.getJSONArray("assessments")
        return (0 until array.length()).map { index ->
            val value = array.getJSONObject(index)
            val answers = value.getJSONArray("answers")
            val score = if (value.isNull("totalScore")) null else value.getInt("totalScore")
            IdentityForeclosureAssessment(
                id = value.getString("id"),
                childId = value.getString("childId"),
                answers = (0 until answers.length()).map { answerIndex ->
                    if (answers.isNull(answerIndex)) null else answers.getInt(answerIndex)
                },
                totalScore = score,
                category = score?.let(IdentityForeclosureCategory::fromScore),
                completedAtEpochMillis = if (value.isNull("completedAt")) null else value.getLong("completedAt"),
                phase = if (schema == 1 || value.optString("phase", "PRE_TEST") == "PRE_TEST") {
                    AssessmentPhase.PRE_TEST
                } else {
                    AssessmentPhase.POST_TEST
                },
            )
        }
    }

    private fun write(records: List<IdentityForeclosureAssessment>) {
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
        temp.writeBytes(encrypt(JSONObject().put("schema", 2).put("assessments", array).toString().toByteArray(Charsets.UTF_8)))
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
