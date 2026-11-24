package org.heroism.herocompass.children

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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

enum class ChildGender(val datasetCode: String, val displayName: String) {
    Boy("L", "Laki-laki"),
    Girl("P", "Perempuan");

    companion object {
        fun fromDatasetCode(code: String): ChildGender? = entries.firstOrNull { it.datasetCode == code }
    }
}

data class ChildRecord(
    val id: String,
    val name: String,
    val birthDate: LocalDate,
    val gender: ChildGender?,
    val involvedInScavenging: Boolean?,
)

sealed interface AddChildResult {
    data class Added(val child: ChildRecord) : AddChildResult
    data object InvalidName : AddChildResult
    data object InvalidBirthDate : AddChildResult
    data object StorageError : AddChildResult
}

interface ChildrenRepository {
    fun getAll(): List<ChildRecord>
    fun add(name: String, birthDate: LocalDate, gender: ChildGender, involvedInScavenging: Boolean): AddChildResult
    fun update(child: ChildRecord): Boolean = false
    fun delete(childId: String): Boolean = false
    fun mergeImported(records: List<ChildRecord>): Boolean = false
}

object ChildRules {
    fun validName(name: String): Boolean = name.trim().length in 2..80
    fun validBirthDate(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean = !date.isAfter(today)
}

/** Child records are stored in one authenticated-encryption file protected by an Android Keystore key. */
class EncryptedChildrenRepository(context: Context) : ChildrenRepository {
    private val file = File(context.applicationContext.filesDir, "children.enc")
    private val keyAlias = "hero_compass_children_v1"

    override fun getAll(): List<ChildRecord> {
        if (!file.exists()) return emptyList()
        val payload = JSONObject(String(decrypt(file.readBytes()), Charsets.UTF_8))
        require(payload.getInt("schema") in 1..2) { "Unsupported children data schema" }
        val records = payload.getJSONArray("children")
        return (0 until records.length()).map { index ->
            val child = records.getJSONObject(index)
            ChildRecord(
                id = child.getString("id"),
                name = child.getString("name"),
                birthDate = LocalDate.parse(child.getString("birthDate")),
                gender = child.optString("gender", "").takeIf(String::isNotBlank)?.let { code ->
                    requireNotNull(ChildGender.fromDatasetCode(code)) { "Unsupported child gender code" }
                },
                involvedInScavenging = if (child.isNull("involvedInScavenging")) null else child.getBoolean("involvedInScavenging"),
            )
        }
    }

    override fun add(name: String, birthDate: LocalDate, gender: ChildGender, involvedInScavenging: Boolean): AddChildResult {
        val cleanName = name.trim()
        if (!ChildRules.validName(cleanName)) return AddChildResult.InvalidName
        if (!ChildRules.validBirthDate(birthDate)) return AddChildResult.InvalidBirthDate
        return runCatching {
            val child = ChildRecord(UUID.randomUUID().toString(), cleanName, birthDate, gender, involvedInScavenging)
            write(getAll() + child)
            AddChildResult.Added(child)
        }.getOrElse { AddChildResult.StorageError }
    }

    override fun update(child: ChildRecord): Boolean = runCatching {
        if (child.id.isBlank() || !ChildRules.validName(child.name) || !ChildRules.validBirthDate(child.birthDate)) return false
        val records = getAll().toMutableList()
        val index = records.indexOfFirst { it.id == child.id }
        if (index < 0) return false
        records[index] = child.copy(name = child.name.trim())
        write(records)
        true
    }.getOrDefault(false)

    override fun delete(childId: String): Boolean = runCatching {
        if (childId.isBlank()) return false
        val records = getAll()
        if (records.none { it.id == childId }) return true
        write(records.filterNot { it.id == childId })
        true
    }.getOrDefault(false)

    override fun mergeImported(records: List<ChildRecord>): Boolean = runCatching {
        require(records.all { it.id.isNotBlank() && ChildRules.validName(it.name) && ChildRules.validBirthDate(it.birthDate) })
        require(records.map { it.id }.distinct().size == records.size)
        val existing = getAll()
        require((existing.map { it.id }.toSet() intersect records.map { it.id }.toSet()).isEmpty())
        write(existing + records)
        true
    }.getOrDefault(false)

    private fun write(children: List<ChildRecord>) {
        val array = JSONArray().apply {
            children.forEach { child ->
                put(JSONObject()
                    .put("id", child.id)
                    .put("name", child.name)
                    .put("birthDate", child.birthDate.toString())
                    .put("gender", child.gender?.datasetCode ?: JSONObject.NULL)
                    .put("involvedInScavenging", child.involvedInScavenging ?: JSONObject.NULL))
            }
        }
        val encrypted = encrypt(JSONObject().put("schema", 2).put("children", array).toString().toByteArray(Charsets.UTF_8))
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
        require(encrypted.size > 12) { "Encrypted child file is incomplete" }
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
