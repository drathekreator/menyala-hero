package org.heroism.herocompass.assessment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroAsaRulesTest {
    @Test
    fun exposesTenStatementsAndAcceptsOnlyOneToFourResponses() {
        assertEquals(10, HeroAsaInstrument.items.size)
        assertTrue(HeroAsaRules.validateAnswers(List(10) { 1 }))
        assertTrue(HeroAsaRules.validateAnswers(List(10) { 4 }))
        assertFalse(HeroAsaRules.validateAnswers(List(9) { 1 }))
        assertFalse(HeroAsaRules.validateAnswers(List(10) { 0 }))
    }

    @Test
    fun sumsAllResponsesAndRequiresACompleteAssessmentForScoring() {
        assertEquals(10, HeroAsaRules.score(List(10) { 1 }))
        assertEquals(40, HeroAsaRules.score(List(10) { 4 }))
        assertEquals(30, HeroAsaRules.score(List(10) { 3 }))
        assertNull(HeroAsaRules.score(List(10) { if (it == 9) null else 3 }))
    }

    @Test
    fun appliesInstrumentCategoryBoundaries() {
        assertNull(HeroAsaRules.category(9))
        assertEquals(HeroAsaCategory.LOW, HeroAsaRules.category(10))
        assertEquals(HeroAsaCategory.LOW, HeroAsaRules.category(19))
        assertEquals(HeroAsaCategory.MEDIUM, HeroAsaRules.category(20))
        assertEquals(HeroAsaCategory.MEDIUM, HeroAsaRules.category(29))
        assertEquals(HeroAsaCategory.HIGH, HeroAsaRules.category(30))
        assertEquals(HeroAsaCategory.HIGH, HeroAsaRules.category(40))
        assertNull(HeroAsaRules.category(41))
    }
}
