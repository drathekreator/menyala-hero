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

class EncryptedIdentityForeclosureRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val file get() = File(context.filesDir, "identity_foreclosure_assessments.enc")

    @Before fun clearAssessmentFile() { file.delete() }
    @After fun clearAssessmentFileAfterTest() { file.delete() }

    @Test
    fun draftKeepsEveryItemAnswerEncryptedAcrossRepositoryRecreation() {
        val answers = MutableList<Int?>(15) { null }.apply { this[0] = 1; this[7] = 4 }
        val result = EncryptedIdentityForeclosureRepository(context).saveDraft("child-private-marker", answers)
        assertTrue(result is AssessmentSaveResult.SavedDraft)
        assertTrue(file.exists())
        val cipherText = String(file.readBytes(), Charsets.ISO_8859_1)
        assertFalse(cipherText.contains("child-private-marker"))
        assertFalse(cipherText.contains(IdentityForeclosureInstrument.items.first()))

        val restored = EncryptedIdentityForeclosureRepository(context).get("child-private-marker")
        assertEquals(answers, restored?.answers)
        assertNull(restored?.totalScore)
        assertFalse(restored?.isComplete ?: true)
    }

    @Test
    fun completeAssessmentIsScoredOnceAndCannotBeOverwritten() {
        val answers = List(15) { 1 }
        val repository = EncryptedIdentityForeclosureRepository(context)

        val result = repository.complete("child-1", answers) as AssessmentSaveResult.Completed
        assertEquals(15, result.assessment.totalScore)
        assertEquals(IdentityForeclosureCategory.HIGH, result.assessment.category)
        assertEquals(true, result.assessment.isComplete)
        assertEquals(AssessmentSaveResult.AlreadyCompleted, repository.complete("child-1", List(15) { 4 }))
        assertEquals(answers, repository.get("child-1")?.answers)
    }

    @Test
    fun posttestIsStoredSeparatelyFromPretest() {
        val repository = EncryptedIdentityForeclosureRepository(context)
        val preAnswers = List(15) { 1 }
        val postAnswers = List(15) { 4 }

        assertTrue(repository.complete("child-phase", preAnswers) is AssessmentSaveResult.Completed)
        assertTrue(repository.saveDraft("child-phase", postAnswers, AssessmentPhase.POST_TEST) is AssessmentSaveResult.SavedDraft)

        assertEquals(preAnswers, repository.get("child-phase", AssessmentPhase.PRE_TEST)?.answers)
        assertEquals(postAnswers, repository.get("child-phase", AssessmentPhase.POST_TEST)?.answers)
        assertEquals(AssessmentPhase.POST_TEST, repository.get("child-phase", AssessmentPhase.POST_TEST)?.phase)
    }
}
