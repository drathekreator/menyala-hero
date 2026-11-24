package org.heroism.herocompass.intervention

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

class EncryptedInterventionRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val file get() = File(context.filesDir, "interventions.enc")

    @Before fun clearRecords() { file.delete() }
    @After fun clearRecordsAfterTest() { file.delete() }

    @Test
    fun recordsAreEncryptedPersistentAndScopedToChild() {
        val repository = EncryptedInterventionRepository(context)
        val first = repository.add("child-a", LocalDate.of(2026, 11, 20), "Kegiatan aktual", "Catatan pendamping") as SaveInterventionResult.Saved
        repository.add("child-b", LocalDate.of(2026, 11, 21), "Kegiatan lain", "Catatan lain")

        assertTrue(file.exists())
        val ciphertext = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(ciphertext.contains("Kegiatan aktual"))
        assertFalse(ciphertext.contains("Catatan pendamping"))
        assertEquals(listOf(first.record), EncryptedInterventionRepository(context).getForChild("child-a"))
        assertEquals(1, EncryptedInterventionRepository(context).getForChild("child-b").size)
    }

    @Test
    fun rejectsInvalidProgramAndFutureDateWithoutWritingAnything() {
        val repository = EncryptedInterventionRepository(context)
        assertEquals(SaveInterventionResult.InvalidProgramName, repository.add("child-a", LocalDate.now(), " ", ""))
        assertEquals(SaveInterventionResult.InvalidDate, repository.add("child-a", LocalDate.now().plusDays(1), "Kegiatan", ""))
        assertEquals(SaveInterventionResult.InvalidNotes, repository.add("child-a", LocalDate.now(), "Kegiatan", "x".repeat(501)))
        assertFalse(file.exists())
    }
}
