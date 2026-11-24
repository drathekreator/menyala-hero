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

object HeroRasaInstrument {
    const val CODE = "hero_rasa_reflection"
    const val TITLE = "HERO Rasa · Reflection"

    val items = listOf(
        "Setelah menyelesaikan tugas, saya memikirkan apakah hasilnya sudah baik.",
        "Saya dapat menjelaskan apa yang sudah saya lakukan dengan baik.",
        "Saya mengetahui bagian yang masih perlu saya perbaiki.",
        "Saya memikirkan apa yang bisa saya lakukan lebih baik pada kesempatan berikutnya.",
        "Saya merasa bangga ketika berhasil mencapai tujuan yang saya tetapkan.",
        "Saya mencoba memahami alasan mengapa saya berhasil atau belum berhasil.",
        "Saya belajar dari kesalahan yang pernah saya lakukan.",
        "Saya memikirkan kembali pengalaman yang saya peroleh dari kegiatan yang telah dilakukan.",
        "Saya percaya bahwa saya dapat berkembang jika terus berusaha.",
        "Saya menggunakan pengalaman sebelumnya untuk membantu saya pada kegiatan berikutnya.",
    )
}

enum class HeroRasaCategory(val label: String) {
    LOW("Rendah"), MEDIUM("Sedang"), HIGH("Tinggi");

    companion object {
        fun fromScore(score: Int): HeroRasaCategory? = when (score) {
            in 10..19 -> LOW
            in 20..29 -> MEDIUM
            in 30..40 -> HIGH
            else -> null
        }
    }
}

data class HeroRasaAssessment(
    val id: String,
    val childId: String,
    val answers: List<Int?>,
    val totalScore: Int?,
    val category: HeroRasaCategory?,
    val completedAtEpochMillis: Long?,
    val phase: AssessmentPhase = AssessmentPhase.PRE_TEST,
) {
    val isComplete: Boolean get() = totalScore != null && category != null && completedAtEpochMillis != null
}

sealed interface HeroRasaSaveResult {
    data class SavedDraft(val assessment: HeroRasaAssessment) : HeroRasaSaveResult
    data class Completed(val assessment: HeroRasaAssessment) : HeroRasaSaveResult
    data object InvalidAnswers : HeroRasaSaveResult
    data object AlreadyCompleted : HeroRasaSaveResult
    data object StorageError : HeroRasaSaveResult
}

interface HeroRasaRepository {
    fun get(childId: String, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): HeroRasaAssessment?
    fun mergeImported(records: List<HeroRasaAssessment>): Boolean = false
    fun deleteForChild(childId: String): Boolean = false
    fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): HeroRasaSaveResult
    fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): HeroRasaSaveResult
}

object HeroRasaRules {
    fun validateAnswers(answers: List<Int?>): Boolean =
        answers.size == HeroRasaInstrument.items.size && answers.all { it == null || it in 1..4 }

    fun score(answers: List<Int?>): Int? {
        if (!validateAnswers(answers) || answers.any { it == null }) return null
        return answers.sumOf { it!! }
    }

    fun category(score: Int): HeroRasaCategory? = HeroRasaCategory.fromScore(score)
}

/** Stores item-level HERO Rasa responses in a dedicated AES-GCM encrypted local file. */
class EncryptedHeroRasaRepository(context: Context) : HeroRasaRepository {
    private val file = File(context.applicationContext.filesDir, "hero_rasa_assessments.enc")
    private val keyAlias = "hero_compass_hero_rasa_v1"

    override fun get(childId: String, phase: AssessmentPhase): HeroRasaAssessment? =
        readAll().firstOrNull { it.childId == childId && it.phase == phase }

    override fun mergeImported(records: List<HeroRasaAssessment>): Boolean = runCatching {
        require(records.all { record ->
            record.id.isNotBlank() && record.childId.isNotBlank() && HeroRasaRules.validateAnswers(record.answers) &&
                if (record.isComplete) {
                    HeroRasaRules.score(record.answers) == record.totalScore &&
                        HeroRasaRules.category(record.totalScore!!) == record.category && record.completedAtEpochMillis != null
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

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroRasaSaveResult {
        if (childId.isBlank() || !HeroRasaRules.validateAnswers(answers)) return HeroRasaSaveResult.InvalidAnswers
        return save(childId, phase) { id -> HeroRasaAssessment(id, childId, answers.toList(), null, null, null, phase) }
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroRasaSaveResult {
        if (childId.isBlank()) return HeroRasaSaveResult.InvalidAnswers
        val score = HeroRasaRules.score(answers) ?: return HeroRasaSaveResult.InvalidAnswers
        val category = HeroRasaRules.category(score) ?: return HeroRasaSaveResult.InvalidAnswers
        return save(childId, phase) { id -> HeroRasaAssessment(id, childId, answers.toList(), score, category, System.currentTimeMillis(), phase) }
    }

    private fun save(childId: String, phase: AssessmentPhase, create: (String) -> HeroRasaAssessment): HeroRasaSaveResult = runCatching {
        val records = readAll().toMutableList()
        val index = records.indexOfFirst { it.childId == childId && it.phase == phase }
        if (index >= 0 && records[index].isComplete) return HeroRasaSaveResult.AlreadyCompleted
        val assessment = create(records.getOrNull(index)?.id ?: UUID.randomUUID().toString())
        if (index >= 0) records[index] = assessment else records += assessment
        write(records)
        if (assessment.isComplete) HeroRasaSaveResult.Completed(assessment) else HeroRasaSaveResult.SavedDraft(assessment)
    }.getOrElse { HeroRasaSaveResult.StorageError }

    private fun readAll(): List<HeroRasaAssessment> {
        if (!file.exists()) return emptyList()
        val root = JSONObject(String(decrypt(file.readBytes()), Charsets.UTF_8))
        require(root.getInt("schema") == 1) { "Unsupported HERO Rasa assessment schema" }
        val array = root.getJSONArray("assessments")
        return (0 until array.length()).map { index ->
            val value = array.getJSONObject(index)
            val answers = value.getJSONArray("answers")
            val score = if (value.isNull("totalScore")) null else value.getInt("totalScore")
            HeroRasaAssessment(
                id = value.getString("id"),
                childId = value.getString("childId"),
                answers = (0 until answers.length()).map { answerIndex -> if (answers.isNull(answerIndex)) null else answers.getInt(answerIndex) },
                totalScore = score,
                category = score?.let(HeroRasaCategory::fromScore),
                completedAtEpochMillis = if (value.isNull("completedAt")) null else value.getLong("completedAt"),
                phase = AssessmentPhase.valueOf(value.getString("phase")),
            )
        }
    }

    private fun write(records: List<HeroRasaAssessment>) {
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
