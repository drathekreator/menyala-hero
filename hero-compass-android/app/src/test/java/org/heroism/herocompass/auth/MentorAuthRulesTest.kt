package org.heroism.herocompass.auth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MentorAuthRulesTest {
    @Test fun acceptsLocalIndonesianPhoneFormats() {
        assertTrue(MentorAuthRules.validatePhone("081234567890"))
        assertTrue(MentorAuthRules.validatePhone("6281234567890"))
        assertTrue(MentorAuthRules.validatePhone("+62 812-3456-7890"))
        assertFalse(MentorAuthRules.validatePhone("08123"))
        assertFalse(MentorAuthRules.validatePhone("abc"))
    }

    @Test fun normalizesIndonesianPrefix() {
        assertTrue(MentorAuthRules.normalizePhone("081234567890") == MentorAuthRules.normalizePhone("+62 81234567890"))
    }

    @Test fun validatesNameAndPinBoundaries() {
        assertTrue(MentorAuthRules.validateName("An"))
        assertTrue(MentorAuthRules.validateName("Pendamping Anak"))
        assertFalse(MentorAuthRules.validateName("A"))
        assertTrue(MentorAuthRules.validatePin("1234"))
        assertTrue(MentorAuthRules.validatePin("123456"))
        assertFalse(MentorAuthRules.validatePin("123"))
        assertFalse(MentorAuthRules.validatePin("12a4"))
        assertFalse(MentorAuthRules.validatePin("1234567"))
    }

    @Test fun pinHashIsStablePerSaltAndChangesWithSalt() {
        val saltA = ByteArray(16) { it.toByte() }
        val saltB = ByteArray(16) { (it + 1).toByte() }
        val first = PinHasher.hash("4826", saltA)
        assertArrayEquals(first, PinHasher.hash("4826", saltA))
        assertFalse(first.contentEquals(PinHasher.hash("4826", saltB)))
        assertFalse(first.contentEquals(PinHasher.hash("4827", saltA)))
    }
}
