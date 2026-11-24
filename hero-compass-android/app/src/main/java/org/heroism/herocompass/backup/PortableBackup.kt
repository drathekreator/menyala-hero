package org.heroism.herocompass.backup

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupChild(
    val id: String,
    val name: String,
    val birthDate: String,
    val genderCode: String?,
    val involvedInScavenging: Boolean?,
)

data class BackupAssessment(
    val instrument: String,
    val id: String,
    val childId: String,
    val phase: String,
    val answers: List<Int?>,
    val totalScore: Int?,
    val completedAtEpochMillis: Long?,
)

data class BackupIntervention(
    val id: String,
    val childId: String,
    val date: String,
    val programName: String,
    val notes: String,
)

data class PortableBackupSnapshot(
    val createdAtEpochMillis: Long,
    val children: List<BackupChild>,
    val assessments: List<BackupAssessment>,
    val interventions: List<BackupIntervention>,
)

sealed interface BackupCryptoResult {
    data class Success(val bytes: ByteArray) : BackupCryptoResult
    data object InvalidPassphrase : BackupCryptoResult
    data object InvalidFile : BackupCryptoResult
    data object AuthenticationFailed : BackupCryptoResult
}

/** Versioned passphrase-encrypted backup format. No app/device keystore keys are included. */
object PortableBackupCrypto {
    const val MIN_PASSPHRASE_LENGTH = 12
    const val MAX_BACKUP_FILE_BYTES = 20 * 1024 * 1024 + 128
    private const val MAGIC = 0x4D48424B // MHBK
    private const val VERSION = 1
    private const val ITERATIONS = 240_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private const val MAX_PAYLOAD_BYTES = 20 * 1024 * 1024

    fun encrypt(snapshot: PortableBackupSnapshot, passphrase: CharArray): BackupCryptoResult {
        if (!validPassphrase(passphrase)) return BackupCryptoResult.InvalidPassphrase
        return runCatching {
            val payload = snapshot.toJson().toByteArray(Charsets.UTF_8)
            require(payload.size <= MAX_PAYLOAD_BYTES)
            val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
            val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
            val header = ByteArrayOutputStream().use { output ->
                DataOutputStream(output).use { data ->
                    data.writeInt(MAGIC)
                    data.writeByte(VERSION)
                    data.writeInt(ITERATIONS)
                    data.write(salt)
                    data.write(iv)
                }
                output.toByteArray()
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(header)
            BackupCryptoResult.Success(header + cipher.doFinal(payload))
        }.getOrElse { BackupCryptoResult.InvalidFile }
    }

    fun decrypt(bytes: ByteArray, passphrase: CharArray): BackupCryptoResult {
        if (!validPassphrase(passphrase)) return BackupCryptoResult.InvalidPassphrase
        val minimumSize = 4 + 1 + 4 + SALT_BYTES + IV_BYTES + 16
        if (bytes.size !in minimumSize..(MAX_PAYLOAD_BYTES + minimumSize)) return BackupCryptoResult.InvalidFile
        return runCatching {
            val input = java.io.DataInputStream(bytes.inputStream())
            require(input.readInt() == MAGIC)
            require(input.readUnsignedByte() == VERSION)
            val iterations = input.readInt()
            require(iterations in 100_000..1_000_000)
            val salt = ByteArray(SALT_BYTES).also(input::readFully)
            val iv = ByteArray(IV_BYTES).also(input::readFully)
            val headerSize = 4 + 1 + 4 + SALT_BYTES + IV_BYTES
            val header = bytes.copyOfRange(0, headerSize)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(header)
            val plain = cipher.doFinal(bytes.copyOfRange(headerSize, bytes.size))
            require(plain.size <= MAX_PAYLOAD_BYTES)
            BackupCryptoResult.Success(plain)
        }.fold(
            onSuccess = { result ->
                runCatching { parseSnapshot(String(result.bytes, Charsets.UTF_8)) }
                    .fold({ BackupCryptoResult.Success(result.bytes) }, { BackupCryptoResult.InvalidFile })
            },
            onFailure = { error ->
                if (error is AEADBadTagException) BackupCryptoResult.AuthenticationFailed else BackupCryptoResult.InvalidFile
            },
        )
    }

    fun decodeSnapshot(bytes: ByteArray): PortableBackupSnapshot = parseSnapshot(String(bytes, Charsets.UTF_8))

    fun validPassphrase(passphrase: CharArray): Boolean = passphrase.size in MIN_PASSPHRASE_LENGTH..128

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, iterations, KEY_BITS)
        val bytes = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
        return SecretKeySpec(bytes, "AES")
    }

    private fun PortableBackupSnapshot.toJson(): String = JSONObject()
        .put("schema", VERSION)
        .put("createdAt", createdAtEpochMillis)
        .put("children", JSONArray().apply {
            children.forEach { child ->
                put(JSONObject()
                    .put("id", child.id)
                    .put("name", child.name)
                    .put("birthDate", child.birthDate)
                    .put("gender", child.genderCode ?: JSONObject.NULL)
                    .put("involvedInScavenging", child.involvedInScavenging ?: JSONObject.NULL))
            }
        })
        .put("assessments", JSONArray().apply {
            assessments.forEach { assessment ->
                put(JSONObject()
                    .put("instrument", assessment.instrument)
                    .put("id", assessment.id)
                    .put("childId", assessment.childId)
                    .put("phase", assessment.phase)
                    .put("answers", JSONArray().apply { assessment.answers.forEach { put(it ?: JSONObject.NULL) } })
                    .put("totalScore", assessment.totalScore ?: JSONObject.NULL)
                    .put("completedAt", assessment.completedAtEpochMillis ?: JSONObject.NULL))
            }
        })
        .put("interventions", JSONArray().apply {
            interventions.forEach { intervention ->
                put(JSONObject()
                    .put("id", intervention.id)
                    .put("childId", intervention.childId)
                    .put("date", intervention.date)
                    .put("programName", intervention.programName)
                    .put("notes", intervention.notes))
            }
        })
        .toString()

    private fun parseSnapshot(json: String): PortableBackupSnapshot {
        val root = JSONObject(json)
        require(root.getInt("schema") == VERSION)
        val childrenJson = root.getJSONArray("children")
        val children = (0 until childrenJson.length()).map { index ->
            val child = childrenJson.getJSONObject(index)
            BackupChild(
                id = child.getString("id"),
                name = child.getString("name"),
                birthDate = child.getString("birthDate"),
                genderCode = child.optString("gender").takeIf(String::isNotEmpty),
                involvedInScavenging = if (child.isNull("involvedInScavenging")) null else child.getBoolean("involvedInScavenging"),
            )
        }
        val assessmentsJson = root.getJSONArray("assessments")
        val assessments = (0 until assessmentsJson.length()).map { index ->
            val value = assessmentsJson.getJSONObject(index)
            val answersJson = value.getJSONArray("answers")
            BackupAssessment(
                instrument = value.getString("instrument"),
                id = value.getString("id"),
                childId = value.getString("childId"),
                phase = value.getString("phase"),
                answers = (0 until answersJson.length()).map { answerIndex ->
                    if (answersJson.isNull(answerIndex)) null else answersJson.getInt(answerIndex)
                },
                totalScore = if (value.isNull("totalScore")) null else value.getInt("totalScore"),
                completedAtEpochMillis = if (value.isNull("completedAt")) null else value.getLong("completedAt"),
            )
        }
        val interventionsJson = root.getJSONArray("interventions")
        val interventions = (0 until interventionsJson.length()).map { index ->
            val value = interventionsJson.getJSONObject(index)
            BackupIntervention(
                id = value.getString("id"),
                childId = value.getString("childId"),
                date = value.getString("date"),
                programName = value.getString("programName"),
                notes = value.getString("notes"),
            )
        }
        require(children.size <= 100_000 && assessments.size <= 500_000 && interventions.size <= 500_000)
        require(children.all { it.id.isNotBlank() && it.name.isNotBlank() && it.birthDate.isNotBlank() })
        val childIds = children.map { it.id }.toSet()
        require(childIds.size == children.size)
        require(assessments.all { it.id.isNotBlank() && it.childId in childIds })
        require(interventions.all { it.id.isNotBlank() && it.childId in childIds })
        return PortableBackupSnapshot(root.getLong("createdAt"), children, assessments, interventions)
    }
}
