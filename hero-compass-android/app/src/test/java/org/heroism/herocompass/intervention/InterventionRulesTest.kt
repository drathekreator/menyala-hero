package org.heroism.herocompass.intervention

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InterventionRulesTest {
    @Test
    fun validatesProgramNameNotesAndActivityDate() {
        val today = LocalDate.of(2026, 11, 22)
        assertFalse(InterventionRules.validProgramName(" "))
        assertTrue(InterventionRules.validProgramName("Program aktual"))
        assertFalse(InterventionRules.validProgramName("x"))
        assertFalse(InterventionRules.validProgramName("a".repeat(101)))
        assertTrue(InterventionRules.validNotes(""))
        assertTrue(InterventionRules.validNotes("Catatan"))
        assertFalse(InterventionRules.validNotes("x".repeat(501)))
        assertTrue(InterventionRules.validDate(today.minusDays(1), today))
        assertTrue(InterventionRules.validDate(today, today))
        assertFalse(InterventionRules.validDate(today.plusDays(1), today))
    }
}
