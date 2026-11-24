package org.heroism.herocompass.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class MentorAuthRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var repository: MentorAuthRepository

    @Before fun clearPreviousProfile() {
        context.deleteFile("mentor_profile.enc")
        context.deleteFile("mentor_biometric.enc")
        context.deleteFile("mentor_avatar.jpg")
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("hero_compass_local_profile_v1") } }
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("hero_compass_biometric_login_v1") } }
        repository = MentorAuthRepository(context)
    }

    @After fun cleanProfile() {
        context.deleteFile("mentor_profile.enc")
        context.deleteFile("mentor_biometric.enc")
        context.deleteFile("mentor_avatar.jpg")
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("hero_compass_local_profile_v1") } }
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry("hero_compass_biometric_login_v1") } }
    }

    @Test fun createProfileEncryptsDataAndAuthenticatesAfterRepositoryRecreation() {
        val created = repository.createProfile("Maimunah", "081234567890", "4826")
        assertTrue(created is AuthResult.Success)
        assertTrue(repository.hasProfile())

        val encryptedFile = context.openFileInput("mentor_profile.enc").use { it.readBytes() }
        val fileText = encryptedFile.toString(Charsets.UTF_8)
        assertFalse(fileText.contains("Maimunah"))
        assertFalse(fileText.contains("081234567890"))
        assertFalse(fileText.contains("4826"))

        val newRepositoryInstance = MentorAuthRepository(context)
        val result = newRepositoryInstance.authenticate("+62 812-3456-7890", "4826")
        assertEquals(AuthResult.Success(MentorProfile("Maimunah", "6281234567890")), result)
    }

    @Test fun rejectsIncorrectCredentialsAndDuplicateLocalProfile() {
        repository.createProfile("Pendamping", "081234567890", "4826")

        assertEquals(AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS), repository.authenticate("081234567891", "4826"))
        assertEquals(AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS), repository.authenticate("081234567890", "1111"))
        assertEquals(AuthResult.Failure(AuthFailure.PROFILE_EXISTS), repository.createProfile("Lainnya", "081234567891", "1234"))
    }

    @Test fun rejectsInvalidProfileInputsWithoutWritingStorage() {
        assertEquals(AuthResult.Failure(AuthFailure.INVALID_NAME), repository.createProfile("A", "081234567890", "1234"))
        assertEquals(AuthResult.Failure(AuthFailure.INVALID_PHONE), repository.createProfile("Nama", "0812", "1234"))
        assertEquals(AuthResult.Failure(AuthFailure.INVALID_PIN), repository.createProfile("Nama", "081234567890", "12ab"))
        assertFalse(repository.hasProfile())
    }

    @Test fun biometricLoginStartsDisabledAndCanBeDisabledRepeatedly() {
        assertFalse(repository.isBiometricEnabled())
        repository.disableBiometric()
        assertFalse(repository.isBiometricEnabled())
    }

    @Test fun avatarImageIsCopiedIntoPrivateProfileStorage() {
        val sourceDirectory = java.io.File(context.cacheDir, "assessment-scans").apply { mkdirs() }
        val source = java.io.File(sourceDirectory, "avatar-test.png")
        val sourceBitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        source.outputStream().use { sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sourceBitmap.recycle()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", source)

        assertTrue(repository.saveAvatar(uri))
        val stored = repository.avatarFile()
        assertTrue(stored?.exists() == true)
        val decoded = BitmapFactory.decodeFile(stored!!.absolutePath)
        assertTrue(decoded.width in 1..80)
        assertTrue(decoded.height in 1..60)
        decoded.recycle()
        source.delete()
    }
}
