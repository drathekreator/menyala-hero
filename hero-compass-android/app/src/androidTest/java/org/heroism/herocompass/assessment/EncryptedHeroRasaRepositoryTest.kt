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

class EncryptedHeroRasaRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val file get() = File(context.filesDir, "hero_rasa_assessments.enc")

    @Before fun clearAssessmentFile() { file.delete() }
    @After fun clearAssessmentFileAfterTest() { file.delete() }

    @Test
    fun draftAnswersAreEncryptedAndRestoredAfterRepositoryRecreation() {
        val answers = MutableList<Int?>(10) { null }.apply { this[0] = 1; this[8] = 4 }
        val result = EncryptedHeroRasaRepository(context).saveDraft("rasa-child-private-marker", answers)
        assertTrue(result is HeroRasaSaveResult.SavedDraft)
        val ciphertext = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(ciphertext.contains("rasa-child-private-marker"))
        assertFalse(ciphertext.contains(HeroRasaInstrument.items.first()))

        val restored = EncryptedHeroRasaRepository(context).get("rasa-child-private-marker")
        assertEquals(answers, restored?.answers)
        assertNull(restored?.totalScore)
    }

    @Test
    fun preAndPostAreSeparateAndCompletedPhaseCannotBeOverwritten() {
        val repository = EncryptedHeroRasaRepository(context)
        val preAnswers = List(10) { 3 }
        val postAnswers = List(10) { 4 }

        val completed = repository.complete("rasa-child-phases", preAnswers) as HeroRasaSaveResult.Completed
        assertEquals(30, completed.assessment.totalScore)
        assertEquals(HeroRasaCategory.HIGH, completed.assessment.category)
        assertTrue(repository.complete("rasa-child-phases", postAnswers, AssessmentPhase.POST_TEST) is HeroRasaSaveResult.Completed)
        assertEquals(HeroRasaSaveResult.AlreadyCompleted, repository.complete("rasa-child-phases", List(10) { 1 }))
        assertEquals(preAnswers, repository.get("rasa-child-phases", AssessmentPhase.PRE_TEST)?.answers)
        assertEquals(postAnswers, repository.get("rasa-child-phases", AssessmentPhase.POST_TEST)?.answers)
    }
}
