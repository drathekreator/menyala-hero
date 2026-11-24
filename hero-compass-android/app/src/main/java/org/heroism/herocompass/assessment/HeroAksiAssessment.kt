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

object HeroAksiInstrument {
    const val CODE = "hero_aksi_performance"
    const val TITLE = "HERO Aksi · Performance"

    val items = listOf(
        "Saya berusaha menyelesaikan tugas sampai selesai.",
        "Saya tetap fokus ketika mengerjakan tugas atau kegiatan.",
        "Saya berusaha melakukan tugas dengan sebaik mungkin.",
        "Saya mengikuti langkah-langkah yang diperlukan untuk menyelesaikan tugas.",
        "Saya tetap mencoba meskipun tugas terasa sulit.",
        "Saya mengetahui apa yang harus saya lakukan saat mengerjakan tugas.",
        "Jika mengalami kesulitan, saya mencoba cara lain untuk menyelesaikan tugas.",
        "Saya memperhatikan apakah pekerjaan saya sudah sesuai dengan yang diminta.",
        "Jika belum berhasil, saya mencoba lagi.",
        "Saya berusaha menjalankan rencana yang telah saya buat.",
    )
}

enum class HeroAksiCategory(val label: String) {
    LOW("Rendah"), MEDIUM("Sedang"), HIGH("Tinggi");

    companion object {
        fun fromScore(score: Int): HeroAksiCategory? = when (score) {
            in 10..19 -> LOW
            in 20..29 -> MEDIUM
            in 30..40 -> HIGH
            else -> null
        }
    }
}

data class HeroAksiAssessment(
    val id: String,
    val childId: String,
    val answers: List<Int?>,
    val totalScore: Int?,
    val category: HeroAksiCategory?,
    val completedAtEpochMillis: Long?,
    val phase: AssessmentPhase = AssessmentPhase.PRE_TEST,
) {
    val isComplete: Boolean get() = totalScore != null && category != null && completedAtEpochMillis != null
}

sealed interface HeroAksiSaveResult {
    data class SavedDraft(val assessment: HeroAksiAssessment) : HeroAksiSaveResult
    data class Completed(val assessment: HeroAksiAssessment) : HeroAksiSaveResult
    data object InvalidAnswers : HeroAksiSaveResult
    data object AlreadyCompleted : HeroAksiSaveResult
    data object StorageError : HeroAksiSaveResult
}

interface HeroAksiRepository {
    fun get(childId: String, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): HeroAksiAssessment?
    fun mergeImported(records: List<HeroAksiAssessment>): Boolean = false
    fun deleteForChild(childId: String): Boolean = false
    fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): HeroAksiSaveResult
    fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): HeroAksiSaveResult
}

object HeroAksiRules {
    fun validateAnswers(answers: List<Int?>): Boolean =
        answers.size == HeroAksiInstrument.items.size && answers.all { it == null || it in 1..4 }

    fun score(answers: List<Int?>): Int? {
        if (!validateAnswers(answers) || answers.any { it == null }) return null
        return answers.sumOf { it!! }
    }

    fun category(score: Int): HeroAksiCategory? = HeroAksiCategory.fromScore(score)
}

/** Stores item-level HERO Aksi responses in a dedicated AES-GCM encrypted local file. */
class EncryptedHeroAksiRepository(context: Context) : HeroAksiRepository {
    private val file = File(context.applicationContext.filesDir, "hero_aksi_assessments.enc")
    private val keyAlias = "hero_compass_hero_aksi_v1"

    override fun get(childId: String, phase: AssessmentPhase): HeroAksiAssessment? =
        readAll().firstOrNull { it.childId == childId && it.phase == phase }

    override fun mergeImported(records: List<HeroAksiAssessment>): Boolean = runCatching {
        require(records.all { record ->
            record.id.isNotBlank() && record.childId.isNotBlank() && HeroAksiRules.validateAnswers(record.answers) &&
                if (record.isComplete) {
                    HeroAksiRules.score(record.answers) == record.totalScore &&
                        HeroAksiRules.category(record.totalScore!!) == record.category && record.completedAtEpochMillis != null
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

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroAksiSaveResult {
        if (childId.isBlank() || !HeroAksiRules.validateAnswers(answers)) return HeroAksiSaveResult.InvalidAnswers
        return save(childId, phase) { id -> HeroAksiAssessment(id, childId, answers.toList(), null, null, null, phase) }
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroAksiSaveResult {
        if (childId.isBlank()) return HeroAksiSaveResult.InvalidAnswers
        val score = HeroAksiRules.score(answers) ?: return HeroAksiSaveResult.InvalidAnswers
        val category = HeroAksiRules.category(score) ?: return HeroAksiSaveResult.InvalidAnswers
        return save(childId, phase) { id -> HeroAksiAssessment(id, childId, answers.toList(), score, category, System.currentTimeMillis(), phase) }
    }

    private fun save(childId: String, phase: AssessmentPhase, create: (String) -> HeroAksiAssessment): HeroAksiSaveResult = runCatching {
        val records = readAll().toMutableList()
        val index = records.indexOfFirst { it.childId == childId && it.phase == phase }
        if (index >= 0 && records[index].isComplete) return HeroAksiSaveResult.AlreadyCompleted
        val assessment = create(records.getOrNull(index)?.id ?: UUID.randomUUID().toString())
        if (index >= 0) records[index] = assessment else records += assessment
        write(records)
        if (assessment.isComplete) HeroAksiSaveResult.Completed(assessment) else HeroAksiSaveResult.SavedDraft(assessment)
    }.getOrElse { HeroAksiSaveResult.StorageError }

    private fun readAll(): List<HeroAksiAssessment> {
        if (!file.exists()) return emptyList()
        val root = JSONObject(String(decrypt(file.readBytes()), Charsets.UTF_8))
        require(root.getInt("schema") == 1) { "Unsupported HERO Aksi assessment schema" }
        val array = root.getJSONArray("assessments")
        return (0 until array.length()).map { index ->
            val value = array.getJSONObject(index)
            val answers = value.getJSONArray("answers")
            val score = if (value.isNull("totalScore")) null else value.getInt("totalScore")
            HeroAksiAssessment(
                id = value.getString("id"),
                childId = value.getString("childId"),
                answers = (0 until answers.length()).map { answerIndex -> if (answers.isNull(answerIndex)) null else answers.getInt(answerIndex) },
                totalScore = score,
                category = score?.let(HeroAksiCategory::fromScore),
                completedAtEpochMillis = if (value.isNull("completedAt")) null else value.getLong("completedAt"),
                phase = AssessmentPhase.valueOf(value.getString("phase")),
            )
        }
    }

    private fun write(records: List<HeroAksiAssessment>) {
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
