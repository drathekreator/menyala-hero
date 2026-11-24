package org.heroism.herocompass.intervention

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.time.LocalDate
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class InterventionRecord(
    val id: String,
    val childId: String,
    val date: LocalDate,
    val programName: String,
    val notes: String,
)

sealed interface SaveInterventionResult {
    data class Saved(val record: InterventionRecord) : SaveInterventionResult
    data object InvalidProgramName : SaveInterventionResult
    data object InvalidNotes : SaveInterventionResult
    data object InvalidDate : SaveInterventionResult
    data object StorageError : SaveInterventionResult
}

interface InterventionRepository {
    fun getForChild(childId: String): List<InterventionRecord>
    fun add(childId: String, date: LocalDate, programName: String, notes: String): SaveInterventionResult
    fun update(record: InterventionRecord): SaveInterventionResult = SaveInterventionResult.StorageError
    fun delete(recordId: String): Boolean = false
    fun mergeImported(records: List<InterventionRecord>): Boolean = false
    fun deleteForChild(childId: String): Boolean = false
}

object InterventionRules {
    fun validProgramName(value: String): Boolean = value.trim().length in 2..100
    fun validNotes(value: String): Boolean = value.length <= 500
    fun validDate(value: LocalDate, today: LocalDate = LocalDate.now()): Boolean = !value.isAfter(today)
}

/** Stores follow-up records in one AES-GCM file whose key is held by Android Keystore. */
class EncryptedInterventionRepository(context: Context) : InterventionRepository {
    private val file = File(context.applicationContext.filesDir, "interventions.enc")
    private val keyAlias = "hero_compass_interventions_v1"

    override fun getForChild(childId: String): List<InterventionRecord> {
        if (!file.exists()) return emptyList()
        val payload = JSONObject(String(decrypt(file.readBytes()), Charsets.UTF_8))
        require(payload.getInt("schema") == 1) { "Unsupported intervention data schema" }
        val records = payload.getJSONArray("interventions")
        return (0 until records.length())
            .map { index -> records.getJSONObject(index).toRecord() }
            .filter { it.childId == childId }
            .sortedWith(compareByDescending<InterventionRecord> { it.date }.thenByDescending { it.id })
    }

    override fun add(childId: String, date: LocalDate, programName: String, notes: String): SaveInterventionResult {
        if (childId.isBlank()) return SaveInterventionResult.StorageError
        if (!InterventionRules.validDate(date)) return SaveInterventionResult.InvalidDate
        val cleanProgramName = programName.trim()
        val cleanNotes = notes.trim()
        if (!InterventionRules.validProgramName(cleanProgramName)) return SaveInterventionResult.InvalidProgramName
        if (!InterventionRules.validNotes(cleanNotes)) return SaveInterventionResult.InvalidNotes
        return runCatching {
            val record = InterventionRecord(UUID.randomUUID().toString(), childId, date, cleanProgramName, cleanNotes)
            val existing = readAll()
            write(existing + record)
            SaveInterventionResult.Saved(record)
        }.getOrElse { SaveInterventionResult.StorageError }
    }

    override fun update(record: InterventionRecord): SaveInterventionResult {
        if (!InterventionRules.validDate(record.date)) return SaveInterventionResult.InvalidDate
        val cleanProgramName = record.programName.trim()
        val cleanNotes = record.notes.trim()
        if (!InterventionRules.validProgramName(cleanProgramName)) return SaveInterventionResult.InvalidProgramName
        if (!InterventionRules.validNotes(cleanNotes)) return SaveInterventionResult.InvalidNotes
        return runCatching {
            val existing = readAll().toMutableList()
            val index = existing.indexOfFirst { it.id == record.id && it.childId == record.childId }
            if (index < 0) return SaveInterventionResult.StorageError
            val updated = record.copy(programName = cleanProgramName, notes = cleanNotes)
            existing[index] = updated
            write(existing)
            SaveInterventionResult.Saved(updated)
        }.getOrElse { SaveInterventionResult.StorageError }
    }

    override fun delete(recordId: String): Boolean = runCatching {
        if (recordId.isBlank()) return false
        val existing = readAll()
        if (existing.none { it.id == recordId }) return true
        write(existing.filterNot { it.id == recordId })
        true
    }.getOrDefault(false)

    override fun mergeImported(records: List<InterventionRecord>): Boolean = runCatching {
        require(records.all {
            it.id.isNotBlank() && it.childId.isNotBlank() && InterventionRules.validDate(it.date) &&
                InterventionRules.validProgramName(it.programName) && InterventionRules.validNotes(it.notes)
        })
        require(records.map { it.id }.distinct().size == records.size)
        val existing = readAll()
        require((existing.map { it.id }.toSet() intersect records.map { it.id }.toSet()).isEmpty())
        write(existing + records)
        true
    }.getOrDefault(false)

    override fun deleteForChild(childId: String): Boolean = runCatching {
        if (childId.isBlank()) return false
        write(readAll().filterNot { it.childId == childId })
        true
    }.getOrDefault(false)

    private fun readAll(): List<InterventionRecord> {
        if (!file.exists()) return emptyList()
        val payload = JSONObject(String(decrypt(file.readBytes()), Charsets.UTF_8))
        require(payload.getInt("schema") == 1) { "Unsupported intervention data schema" }
        val records = payload.getJSONArray("interventions")
        return (0 until records.length()).map { index -> records.getJSONObject(index).toRecord() }
    }

    private fun JSONObject.toRecord() = InterventionRecord(
        id = getString("id"),
        childId = getString("childId"),
        date = LocalDate.parse(getString("date")),
        programName = getString("programName"),
        notes = getString("notes"),
    )

    private fun write(records: List<InterventionRecord>) {
        val array = JSONArray().apply {
            records.forEach { record ->
                put(JSONObject()
                    .put("id", record.id)
                    .put("childId", record.childId)
                    .put("date", record.date.toString())
                    .put("programName", record.programName)
                    .put("notes", record.notes))
            }
        }
        val encrypted = encrypt(JSONObject().put("schema", 1).put("interventions", array).toString().toByteArray(Charsets.UTF_8))
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeBytes(encrypted)
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
        require(encrypted.size > 12) { "Encrypted intervention file is incomplete" }
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
