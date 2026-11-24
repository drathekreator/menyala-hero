package org.heroism.herocompass.assessment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroRasaRulesTest {
    @Test
    fun exposesTenStatementsAndAcceptsOnlyOneToFourResponses() {
        assertEquals(10, HeroRasaInstrument.items.size)
        assertTrue(HeroRasaRules.validateAnswers(List(10) { 1 }))
        assertTrue(HeroRasaRules.validateAnswers(List(10) { 4 }))
        assertFalse(HeroRasaRules.validateAnswers(List(9) { 1 }))
        assertFalse(HeroRasaRules.validateAnswers(List(10) { 0 }))
    }

    @Test
    fun sumsAllResponsesAndRequiresACompleteAssessmentForScoring() {
        assertEquals(10, HeroRasaRules.score(List(10) { 1 }))
        assertEquals(40, HeroRasaRules.score(List(10) { 4 }))
        assertEquals(30, HeroRasaRules.score(List(10) { 3 }))
        assertNull(HeroRasaRules.score(List(10) { if (it == 9) null else 3 }))
    }

    @Test
    fun appliesInstrumentCategoryBoundaries() {
        assertNull(HeroRasaRules.category(9))
        assertEquals(HeroRasaCategory.LOW, HeroRasaRules.category(10))
        assertEquals(HeroRasaCategory.LOW, HeroRasaRules.category(19))
        assertEquals(HeroRasaCategory.MEDIUM, HeroRasaRules.category(20))
        assertEquals(HeroRasaCategory.MEDIUM, HeroRasaRules.category(29))
        assertEquals(HeroRasaCategory.HIGH, HeroRasaRules.category(30))
        assertEquals(HeroRasaCategory.HIGH, HeroRasaRules.category(40))
        assertNull(HeroRasaRules.category(41))
    }
}
