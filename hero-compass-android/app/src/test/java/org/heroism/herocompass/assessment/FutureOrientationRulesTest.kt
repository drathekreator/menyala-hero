package org.heroism.herocompass.assessment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FutureOrientationRulesTest {
    @Test
    fun instrumentHasFifteenStatementsAndUsesScaleOneThroughFour() {
        assertEquals(15, FutureOrientationInstrument.items.size)
        assertTrue(FutureOrientationRules.validateAnswers(List(15) { 1 }))
        assertTrue(FutureOrientationRules.validateAnswers(List(15) { 4 }))
        assertFalse(FutureOrientationRules.validateAnswers(List(14) { 1 }))
        assertFalse(FutureOrientationRules.validateAnswers(List(15) { 5 }))
    }

    @Test
    fun scoreRequiresEveryItemAndSumsResponsesWithoutReverseScoring() {
        assertEquals(15, FutureOrientationRules.score(List(15) { 1 }))
        assertEquals(60, FutureOrientationRules.score(List(15) { 4 }))
        assertEquals(37, FutureOrientationRules.score(List(15) { if (it < 8) 2 else 3 }))
        assertNull(FutureOrientationRules.score(List(15) { if (it == 14) null else 3 }))
    }

    @Test
    fun categoryRangesMatchTheInstrumentGuideAtEveryBoundary() {
        assertNull(FutureOrientationRules.category(14))
        assertEquals(FutureOrientationCategory.LOW, FutureOrientationRules.category(15))
        assertEquals(FutureOrientationCategory.LOW, FutureOrientationRules.category(29))
        assertEquals(FutureOrientationCategory.MEDIUM, FutureOrientationRules.category(30))
        assertEquals(FutureOrientationCategory.MEDIUM, FutureOrientationRules.category(44))
        assertEquals(FutureOrientationCategory.HIGH, FutureOrientationRules.category(45))
        assertEquals(FutureOrientationCategory.HIGH, FutureOrientationRules.category(60))
        assertNull(FutureOrientationRules.category(61))
    }
}
