package org.heroism.herocompass.assessment

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class EncryptedHeroAksiRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val file get() = File(context.filesDir, "hero_aksi_assessments.enc")

    @Before fun clearAssessmentFile() { file.delete() }
    @After fun clearAssessmentFileAfterTest() { file.delete() }

    @Test
    fun draftAnswersAreEncryptedAndRestoredAfterRepositoryRecreation() {
        val answers = MutableList<Int?>(10) { null }.apply { this[0] = 1; this[8] = 4 }
        val result = EncryptedHeroAksiRepository(context).saveDraft("aksi-child-private-marker", answers)
        assertTrue(result is HeroAksiSaveResult.SavedDraft)
        val ciphertext = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(ciphertext.contains("aksi-child-private-marker"))
        assertFalse(ciphertext.contains(HeroAksiInstrument.items.first()))

        val restored = EncryptedHeroAksiRepository(context).get("aksi-child-private-marker")
        assertEquals(answers, restored?.answers)
        assertNull(restored?.totalScore)
    }

    @Test
    fun preAndPostAreSeparateAndCompletedPhaseCannotBeOverwritten() {
        val repository = EncryptedHeroAksiRepository(context)
        val preAnswers = List(10) { 3 }
        val postAnswers = List(10) { 4 }

        val completed = repository.complete("aksi-child-phases", preAnswers) as HeroAksiSaveResult.Completed
        assertEquals(30, completed.assessment.totalScore)
        assertEquals(HeroAksiCategory.HIGH, completed.assessment.category)
        assertTrue(repository.complete("aksi-child-phases", postAnswers, AssessmentPhase.POST_TEST) is HeroAksiSaveResult.Completed)
        assertEquals(HeroAksiSaveResult.AlreadyCompleted, repository.complete("aksi-child-phases", List(10) { 1 }))
        assertEquals(preAnswers, repository.get("aksi-child-phases", AssessmentPhase.PRE_TEST)?.answers)
        assertEquals(postAnswers, repository.get("aksi-child-phases", AssessmentPhase.POST_TEST)?.answers)
    }
}
