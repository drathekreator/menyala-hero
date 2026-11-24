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

class EncryptedHeroAsaRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val file get() = File(context.filesDir, "hero_asa_assessments.enc")

    @Before fun clearAssessmentFile() { file.delete() }
    @After fun clearAssessmentFileAfterTest() { file.delete() }

    @Test
    fun draftAnswersAreEncryptedAndRestoredAfterRepositoryRecreation() {
        val answers = MutableList<Int?>(10) { null }.apply { this[0] = 1; this[8] = 4 }
        val result = EncryptedHeroAsaRepository(context).saveDraft("asa-child-private-marker", answers)
        assertTrue(result is HeroAsaSaveResult.SavedDraft)
        val ciphertext = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(ciphertext.contains("asa-child-private-marker"))
        assertFalse(ciphertext.contains(HeroAsaInstrument.items.first()))

        val restored = EncryptedHeroAsaRepository(context).get("asa-child-private-marker")
        assertEquals(answers, restored?.answers)
        assertNull(restored?.totalScore)
    }

    @Test
    fun preAndPostAreSeparateAndCompletedPhaseCannotBeOverwritten() {
        val repository = EncryptedHeroAsaRepository(context)
        val preAnswers = List(10) { 3 }
        val postAnswers = List(10) { 4 }

        val completed = repository.complete("asa-child-phases", preAnswers) as HeroAsaSaveResult.Completed
        assertEquals(30, completed.assessment.totalScore)
        assertEquals(HeroAsaCategory.HIGH, completed.assessment.category)
        assertTrue(repository.complete("asa-child-phases", postAnswers, AssessmentPhase.POST_TEST) is HeroAsaSaveResult.Completed)
        assertEquals(HeroAsaSaveResult.AlreadyCompleted, repository.complete("asa-child-phases", List(10) { 1 }))
        assertEquals(preAnswers, repository.get("asa-child-phases", AssessmentPhase.PRE_TEST)?.answers)
        assertEquals(postAnswers, repository.get("asa-child-phases", AssessmentPhase.POST_TEST)?.answers)
    }
}
