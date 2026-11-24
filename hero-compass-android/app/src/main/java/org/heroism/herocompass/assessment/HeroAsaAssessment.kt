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

object HeroAsaInstrument {
    const val CODE = "hero_asa_forethought"
    const val TITLE = "HERO Asa · Forethought"

    val items = listOf(
        "Saya mempunyai tujuan yang ingin saya capai saat mengikuti kegiatan ini.",
        "Sebelum memulai kegiatan, saya memikirkan apa yang ingin saya capai.",
        "Saya percaya saya mampu menyelesaikan kegiatan yang diberikan.",
        "Saya memikirkan manfaat kegiatan ini untuk diri saya di masa depan.",
        "Saya mempunyai alasan mengapa saya ingin mengikuti kegiatan ini dengan baik.",
        "Saya menentukan target yang ingin saya capai sebelum mulai belajar atau berlatih.",
        "Saya memikirkan cara yang dapat membantu saya mencapai tujuan saya.",
        "Saya tetap bersemangat meskipun kegiatan terasa sulit.",
        "Saya percaya usaha saya dapat membantu saya mencapai tujuan.",
        "Saya ingin melakukan yang terbaik dalam kegiatan yang saya ikuti.",
    )
}

enum class HeroAsaCategory(val label: String) {
    LOW("Rendah"), MEDIUM("Sedang"), HIGH("Tinggi");

    companion object {
        fun fromScore(score: Int): HeroAsaCategory? = when (score) {
            in 10..19 -> LOW
            in 20..29 -> MEDIUM
            in 30..40 -> HIGH
            else -> null
        }
    }
}

data class HeroAsaAssessment(
    val id: String,
    val childId: String,
    val answers: List<Int?>,
    val totalScore: Int?,
    val category: HeroAsaCategory?,
    val completedAtEpochMillis: Long?,
    val phase: AssessmentPhase = AssessmentPhase.PRE_TEST,
) {
    val isComplete: Boolean get() = totalScore != null && category != null && completedAtEpochMillis != null
}

sealed interface HeroAsaSaveResult {
    data class SavedDraft(val assessment: HeroAsaAssessment) : HeroAsaSaveResult
    data class Completed(val assessment: HeroAsaAssessment) : HeroAsaSaveResult
    data object InvalidAnswers : HeroAsaSaveResult
    data object AlreadyCompleted : HeroAsaSaveResult
    data object StorageError : HeroAsaSaveResult
}

interface HeroAsaRepository {
    fun get(childId: String, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): HeroAsaAssessment?
    fun mergeImported(records: List<HeroAsaAssessment>): Boolean = false
    fun deleteForChild(childId: String): Boolean = false
    fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): HeroAsaSaveResult
    fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase = AssessmentPhase.PRE_TEST): HeroAsaSaveResult
}

object HeroAsaRules {
    fun validateAnswers(answers: List<Int?>): Boolean =
        answers.size == HeroAsaInstrument.items.size && answers.all { it == null || it in 1..4 }

    fun score(answers: List<Int?>): Int? {
        if (!validateAnswers(answers) || answers.any { it == null }) return null
        return answers.sumOf { it!! }
    }

    fun category(score: Int): HeroAsaCategory? = HeroAsaCategory.fromScore(score)
}

/** Stores item-level HERO Asa responses in a dedicated AES-GCM encrypted local file. */
class EncryptedHeroAsaRepository(context: Context) : HeroAsaRepository {
    private val file = File(context.applicationContext.filesDir, "hero_asa_assessments.enc")
    private val keyAlias = "hero_compass_hero_asa_v1"

    override fun get(childId: String, phase: AssessmentPhase): HeroAsaAssessment? =
        readAll().firstOrNull { it.childId == childId && it.phase == phase }

    override fun mergeImported(records: List<HeroAsaAssessment>): Boolean = runCatching {
        require(records.all { record ->
            record.id.isNotBlank() && record.childId.isNotBlank() && HeroAsaRules.validateAnswers(record.answers) &&
                if (record.isComplete) {
                    HeroAsaRules.score(record.answers) == record.totalScore &&
                        HeroAsaRules.category(record.totalScore!!) == record.category && record.completedAtEpochMillis != null
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

    override fun saveDraft(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroAsaSaveResult {
        if (childId.isBlank() || !HeroAsaRules.validateAnswers(answers)) return HeroAsaSaveResult.InvalidAnswers
        return save(childId, phase) { id -> HeroAsaAssessment(id, childId, answers.toList(), null, null, null, phase) }
    }

    override fun complete(childId: String, answers: List<Int?>, phase: AssessmentPhase): HeroAsaSaveResult {
        if (childId.isBlank()) return HeroAsaSaveResult.InvalidAnswers
        val score = HeroAsaRules.score(answers) ?: return HeroAsaSaveResult.InvalidAnswers
        val category = HeroAsaRules.category(score) ?: return HeroAsaSaveResult.InvalidAnswers
        return save(childId, phase) { id -> HeroAsaAssessment(id, childId, answers.toList(), score, category, System.currentTimeMillis(), phase) }
    }

    private fun save(childId: String, phase: AssessmentPhase, create: (String) -> HeroAsaAssessment): HeroAsaSaveResult = runCatching {
        val records = readAll().toMutableList()
        val index = records.indexOfFirst { it.childId == childId && it.phase == phase }
        if (index >= 0 && records[index].isComplete) return HeroAsaSaveResult.AlreadyCompleted
        val assessment = create(records.getOrNull(index)?.id ?: UUID.randomUUID().toString())
        if (index >= 0) records[index] = assessment else records += assessment
        write(records)
        if (assessment.isComplete) HeroAsaSaveResult.Completed(assessment) else HeroAsaSaveResult.SavedDraft(assessment)
    }.getOrElse { HeroAsaSaveResult.StorageError }

    private fun readAll(): List<HeroAsaAssessment> {
        if (!file.exists()) return emptyList()
        val root = JSONObject(String(decrypt(file.readBytes()), Charsets.UTF_8))
        require(root.getInt("schema") == 1) { "Unsupported HERO Asa assessment schema" }
        val array = root.getJSONArray("assessments")
        return (0 until array.length()).map { index ->
            val value = array.getJSONObject(index)
            val answers = value.getJSONArray("answers")
            val score = if (value.isNull("totalScore")) null else value.getInt("totalScore")
            HeroAsaAssessment(
                id = value.getString("id"),
                childId = value.getString("childId"),
                answers = (0 until answers.length()).map { answerIndex -> if (answers.isNull(answerIndex)) null else answers.getInt(answerIndex) },
                totalScore = score,
                category = score?.let(HeroAsaCategory::fromScore),
                completedAtEpochMillis = if (value.isNull("completedAt")) null else value.getLong("completedAt"),
                phase = AssessmentPhase.valueOf(value.getString("phase")),
            )
        }
    }

    private fun write(records: List<HeroAsaAssessment>) {
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
