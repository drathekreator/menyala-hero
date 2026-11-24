package org.heroism.herocompass.auth

import android.content.Context
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

data class MentorProfile(val name: String, val phone: String)

sealed interface AuthResult {
    data class Success(val profile: MentorProfile) : AuthResult
    data class Failure(val reason: AuthFailure) : AuthResult
}

enum class AuthFailure { PROFILE_EXISTS, PROFILE_MISSING, INVALID_NAME, INVALID_PHONE, INVALID_PIN, INVALID_CREDENTIALS, STORAGE_ERROR }

object MentorAuthRules {
    fun normalizePhone(raw: String): String = raw.filter(Char::isDigit).let { digits ->
        when {
            digits.startsWith("62") -> digits
            digits.startsWith("0") -> "62${digits.drop(1)}"
            else -> digits
        }
    }

    fun validateName(name: String): Boolean = name.trim().length in 2..60
    fun validatePhone(phone: String): Boolean = normalizePhone(phone).matches(Regex("62[0-9]{8,13}"))
    fun validatePin(pin: String): Boolean = pin.matches(Regex("[0-9]{4,6}"))
}

/** One local mentor profile per installation. Profile and verifier are encrypted with an Android Keystore AES-GCM key. */
class MentorAuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val profileFile = File(appContext.filesDir, "mentor_profile.enc")
    private val biometricFile = File(appContext.filesDir, "mentor_biometric.enc")
    private val avatarFile = File(appContext.filesDir, "mentor_avatar.jpg")
    private val alias = "hero_compass_local_profile_v1"
    private val biometricAlias = "hero_compass_biometric_login_v1"

    fun hasProfile(): Boolean = profileFile.exists()

    fun isBiometricEnabled(): Boolean = biometricFile.exists()

    fun disableBiometric() {
        biometricFile.delete()
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(biometricAlias) }
        }
    }

    fun biometricCipherForEnrollment(): Cipher = biometricCipher(Cipher.ENCRYPT_MODE)

    fun biometricCipherForLogin(): Cipher {
        val data = biometricFile.readBytes()
        require(data.size > 12) { "Biometric login data is incomplete" }
        return biometricCipher(Cipher.DECRYPT_MODE, data.copyOfRange(0, 12))
    }

    fun enableBiometric(cipher: Cipher): Boolean = runCatching {
        val encrypted = cipher.doFinal(readEncrypted())
        val temp = File(biometricFile.parentFile, "${biometricFile.name}.tmp")
        temp.writeBytes(cipher.iv + encrypted)
        if (!temp.renameTo(biometricFile)) {
            temp.copyTo(biometricFile, overwrite = true)
            temp.delete()
        }
        true
    }.getOrDefault(false)

    fun authenticateBiometric(cipher: Cipher): AuthResult = runCatching {
        val payload = JSONObject(String(cipher.doFinal(biometricFile.readBytes().copyOfRange(12, biometricFile.length().toInt())), Charsets.UTF_8))
        AuthResult.Success(MentorProfile(payload.getString("name"), payload.getString("phone")))
    }.getOrElse { AuthResult.Failure(AuthFailure.STORAGE_ERROR) }

    fun avatarFile(): File? = avatarFile.takeIf(File::exists)

    fun saveAvatar(uri: Uri): Boolean = runCatching {
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 768)
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Image could not be opened")
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        val oriented = if (rotation == 0) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
        val temp = File(avatarFile.parentFile, "${avatarFile.name}.tmp")
        temp.outputStream().use { output -> oriented.compress(Bitmap.CompressFormat.JPEG, 86, output) }
        if (oriented !== decoded) oriented.recycle()
        decoded.recycle()
        if (!temp.renameTo(avatarFile)) {
            temp.copyTo(avatarFile, overwrite = true)
            temp.delete()
        }
        true
    }.getOrDefault(false)

    fun removeAvatar() { avatarFile.delete() }

    fun createProfile(name: String, phone: String, pin: String): AuthResult {
        val cleanName = name.trim()
        val normalizedPhone = MentorAuthRules.normalizePhone(phone)
        if (!MentorAuthRules.validateName(cleanName)) return AuthResult.Failure(AuthFailure.INVALID_NAME)
        if (!MentorAuthRules.validatePhone(normalizedPhone)) return AuthResult.Failure(AuthFailure.INVALID_PHONE)
        if (!MentorAuthRules.validatePin(pin)) return AuthResult.Failure(AuthFailure.INVALID_PIN)
        if (hasProfile()) return AuthResult.Failure(AuthFailure.PROFILE_EXISTS)

        return runCatching {
            val salt = ByteArray(16).also(SecureRandom()::nextBytes)
            val verifier = PinHasher.hash(pin, salt)
            val payload = JSONObject()
                .put("schema", 1)
                .put("name", cleanName)
                .put("phone", normalizedPhone)
                .put("salt", android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP))
                .put("pinVerifier", android.util.Base64.encodeToString(verifier, android.util.Base64.NO_WRAP))
                .toString()
            writeEncrypted(payload.toByteArray(Charsets.UTF_8))
            AuthResult.Success(MentorProfile(cleanName, normalizedPhone))
        }.getOrElse { AuthResult.Failure(AuthFailure.STORAGE_ERROR) }
    }

    fun authenticate(phone: String, pin: String): AuthResult {
        if (!MentorAuthRules.validatePhone(phone)) return AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
        if (!MentorAuthRules.validatePin(pin)) return AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
        if (!hasProfile()) return AuthResult.Failure(AuthFailure.PROFILE_MISSING)
        return runCatching {
            val payload = JSONObject(String(readEncrypted(), Charsets.UTF_8))
            val profile = MentorProfile(payload.getString("name"), payload.getString("phone"))
            val salt = android.util.Base64.decode(payload.getString("salt"), android.util.Base64.NO_WRAP)
            val expected = android.util.Base64.decode(payload.getString("pinVerifier"), android.util.Base64.NO_WRAP)
            val actual = PinHasher.hash(pin, salt)
            if (profile.phone == MentorAuthRules.normalizePhone(phone) && MessageDigest.isEqual(expected, actual)) {
                AuthResult.Success(profile)
            } else AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)
        }.getOrElse { AuthResult.Failure(AuthFailure.STORAGE_ERROR) }
    }

    private fun writeEncrypted(plainText: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText)
        val output = cipher.iv + encrypted
        val temp = File(profileFile.parentFile, "${profileFile.name}.tmp")
        temp.writeBytes(output)
        if (!temp.renameTo(profileFile)) {
            temp.copyTo(profileFile, overwrite = true)
            temp.delete()
        }
    }

    private fun readEncrypted(): ByteArray {
        val data = profileFile.readBytes()
        require(data.size > 12) { "Encrypted profile is incomplete" }
        val iv = data.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(data.copyOfRange(12, data.size))
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
            generateKey()
        }
    }

    private fun biometricCipher(mode: Int, iv: ByteArray? = null): Cipher {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = (keyStore.getKey(biometricAlias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            val builder = KeyGenParameterSpec.Builder(biometricAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }
            init(builder.build())
            generateKey()
        }
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            if (mode == Cipher.ENCRYPT_MODE) init(mode, key) else init(mode, key, GCMParameterSpec(128, requireNotNull(iv)))
        }
    }
}

internal object PinHasher {
    private const val ITERATIONS = 180_000
    private const val KEY_BITS = 256

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
