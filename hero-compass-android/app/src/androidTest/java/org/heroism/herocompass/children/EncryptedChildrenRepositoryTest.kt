package org.heroism.herocompass.children

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.KeyStore
import java.time.LocalDate
import javax.crypto.Cipher
import javax.crypto.SecretKey

class EncryptedChildrenRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val file get() = File(context.filesDir, "children.enc")

    @Before fun clearChildFile() { file.delete() }
    @After fun clearChildFileAfterTest() { file.delete() }

    @Test
    fun recordsAreEncryptedAndPersistAcrossRepositoryRecreation() {
        val repository = EncryptedChildrenRepository(context)
        val expected = (repository.add("Ayu Lestari", LocalDate.of(2015, 4, 12), ChildGender.Girl, true) as AddChildResult.Added).child

        assertTrue(file.exists())
        assertFalse(String(file.readBytes(), Charsets.ISO_8859_1).contains("Ayu Lestari"))
        assertEquals(listOf(expected), EncryptedChildrenRepository(context).getAll())
    }

    @Test
    fun rejectsBlankNamesAndFutureBirthDates() {
        val repository = EncryptedChildrenRepository(context)
        assertEquals(AddChildResult.InvalidName, repository.add(" ", LocalDate.of(2015, 4, 12), ChildGender.Boy, false))
        assertEquals(AddChildResult.InvalidBirthDate, repository.add("Ayu", LocalDate.now().plusDays(1), ChildGender.Boy, false))
        assertFalse(file.exists())
    }

    @Test
    fun readsPreviousChildSchemaWithoutInventingNewAttributes() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = keyStore.getKey("hero_compass_children_v1", null) as? SecretKey
            ?: run {
                EncryptedChildrenRepository(context).add("Lama", LocalDate.of(2014, 6, 3), ChildGender.Boy, false)
                keyStore.load(null)
                keyStore.getKey("hero_compass_children_v1", null) as SecretKey
            }
        val legacyPayload = """{"schema":1,"children":[{"id":"legacy-1","name":"Data Lama","birthDate":"2014-06-03"}]}"""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        file.writeBytes(cipher.iv + cipher.doFinal(legacyPayload.toByteArray(Charsets.UTF_8)))

        val legacy = EncryptedChildrenRepository(context).getAll().single()
        assertEquals("Data Lama", legacy.name)
        assertEquals(null, legacy.gender)
        assertEquals(null, legacy.involvedInScavenging)
    }
}
