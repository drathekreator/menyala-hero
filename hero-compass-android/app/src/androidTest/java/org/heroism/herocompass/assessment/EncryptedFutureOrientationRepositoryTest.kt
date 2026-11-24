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

class EncryptedFutureOrientationRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val file get() = File(context.filesDir, "future_orientation_assessments.enc")

    @Before fun clearAssessmentFile() { file.delete() }
    @After fun clearAssessmentFileAfterTest() { file.delete() }

    @Test
    fun itemLevelDraftIsEncryptedAndSurvivesRepositoryRecreation() {
        val answers = MutableList<Int?>(15) { null }.apply { this[0] = 1; this[12] = 4 }
        val result = EncryptedFutureOrientationRepository(context).saveDraft("future-child-private-marker", answers)
        assertTrue(result is FutureOrientationSaveResult.SavedDraft)
        assertTrue(file.exists())
        val cipherText = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(cipherText.contains("future-child-private-marker"))
        assertFalse(cipherText.contains(FutureOrientationInstrument.items.first()))

        val restored = EncryptedFutureOrientationRepository(context).get("future-child-private-marker")
        assertEquals(answers, restored?.answers)
        assertNull(restored?.totalScore)
        assertFalse(restored?.isComplete ?: true)
    }

    @Test
    fun preAndPostResultsAreSeparateAndCompletedResultCannotBeOverwritten() {
        val repository = EncryptedFutureOrientationRepository(context)
        val preAnswers = List(15) { 3 }
        val postAnswers = List(15) { 4 }

        val preResult = repository.complete("child-phases", preAnswers) as FutureOrientationSaveResult.Completed
        assertEquals(45, preResult.assessment.totalScore)
        assertEquals(FutureOrientationCategory.HIGH, preResult.assessment.category)
        assertTrue(repository.complete("child-phases", postAnswers, AssessmentPhase.POST_TEST) is FutureOrientationSaveResult.Completed)
        assertEquals(FutureOrientationSaveResult.AlreadyCompleted, repository.complete("child-phases", List(15) { 1 }))

        assertEquals(preAnswers, repository.get("child-phases", AssessmentPhase.PRE_TEST)?.answers)
        assertEquals(postAnswers, repository.get("child-phases", AssessmentPhase.POST_TEST)?.answers)
    }
}
