package org.heroism.herocompass.assessment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityForeclosureRulesTest {
    @Test fun exposesFifteenInstrumentItems() {
        assertEquals(15, IdentityForeclosureInstrument.items.size)
        assertEquals(IdentityForeclosureInstrument.TITLE, "Identity Foreclosure (U-MICS)")
    }

    @Test fun sumsAllAnswersAndKeepsPartialWorkUnscored() {
        assertEquals(15, IdentityForeclosureRules.score(List(15) { 1 }))
        assertEquals(60, IdentityForeclosureRules.score(List(15) { 4 }))
        assertNull(IdentityForeclosureRules.score(List(15) { if (it == 0) null else 2 }))
        assertNull(IdentityForeclosureRules.score(List(14) { 2 }))
    }

    @Test fun appliesInstrumentCategoryRangesAndDirection() {
        assertEquals(IdentityForeclosureCategory.HIGH, IdentityForeclosureRules.category(15))
        assertEquals(IdentityForeclosureCategory.HIGH, IdentityForeclosureRules.category(29))
        assertEquals(IdentityForeclosureCategory.MEDIUM, IdentityForeclosureRules.category(30))
        assertEquals(IdentityForeclosureCategory.MEDIUM, IdentityForeclosureRules.category(44))
        assertEquals(IdentityForeclosureCategory.LOW, IdentityForeclosureRules.category(45))
        assertEquals(IdentityForeclosureCategory.LOW, IdentityForeclosureRules.category(60))
        assertNull(IdentityForeclosureRules.category(14))
        assertNull(IdentityForeclosureRules.category(61))
        assertTrue(IdentityForeclosureRules.validateAnswers(listOf(1) + List(14) { null }))
    }
}
