package org.heroism.herocompass.assessment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroAksiRulesTest {
    @Test
    fun exposesTenStatementsAndAcceptsOnlyOneToFourResponses() {
        assertEquals(10, HeroAksiInstrument.items.size)
        assertTrue(HeroAksiRules.validateAnswers(List(10) { 1 }))
        assertTrue(HeroAksiRules.validateAnswers(List(10) { 4 }))
        assertFalse(HeroAksiRules.validateAnswers(List(9) { 1 }))
        assertFalse(HeroAksiRules.validateAnswers(List(10) { 0 }))
    }

    @Test
    fun sumsAllResponsesAndRequiresACompleteAssessmentForScoring() {
        assertEquals(10, HeroAksiRules.score(List(10) { 1 }))
        assertEquals(40, HeroAksiRules.score(List(10) { 4 }))
        assertEquals(30, HeroAksiRules.score(List(10) { 3 }))
        assertNull(HeroAksiRules.score(List(10) { if (it == 9) null else 3 }))
    }

    @Test
    fun appliesInstrumentCategoryBoundaries() {
        assertNull(HeroAksiRules.category(9))
        assertEquals(HeroAksiCategory.LOW, HeroAksiRules.category(10))
        assertEquals(HeroAksiCategory.LOW, HeroAksiRules.category(19))
        assertEquals(HeroAksiCategory.MEDIUM, HeroAksiRules.category(20))
        assertEquals(HeroAksiCategory.MEDIUM, HeroAksiRules.category(29))
        assertEquals(HeroAksiCategory.HIGH, HeroAksiRules.category(30))
        assertEquals(HeroAksiCategory.HIGH, HeroAksiRules.category(40))
        assertNull(HeroAksiRules.category(41))
    }
}
