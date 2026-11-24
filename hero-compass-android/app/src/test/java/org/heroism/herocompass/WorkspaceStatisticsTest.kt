package org.heroism.herocompass

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceStatisticsTest {
    @Test
    fun progressUsesOnlyAvailableChildrenAndHandlesEmptyLists() {
        assertEquals(50, pretestProgressPercent(totalChildren = 2, completedChildren = 1))
        assertEquals(100, pretestProgressPercent(totalChildren = 1, completedChildren = 1))
        assertEquals(0, pretestProgressPercent(totalChildren = 3, completedChildren = 0))
        assertEquals(67, pretestProgressPercent(totalChildren = 3, completedChildren = 2))
        assertEquals(null, pretestProgressPercent(totalChildren = 0, completedChildren = 0))
    }

    @Test
    fun invalidCompletedCountIsClampedToValidRange() {
        assertEquals(0, pretestProgressPercent(totalChildren = 2, completedChildren = -1))
        assertEquals(100, pretestProgressPercent(totalChildren = 2, completedChildren = 3))
    }

    @Test
    fun programStageFollowsBaselineAndPostTestProgress() {
        assertEquals(ProgramStage.PRE_PROGRAM, currentProgramStage(0, 0, false))
        assertEquals(ProgramStage.PRE_PROGRAM, currentProgramStage(3, 2, false))
        assertEquals(ProgramStage.INTERVENTION, currentProgramStage(3, 3, false))
        assertEquals(ProgramStage.POST_PROGRAM, currentProgramStage(3, 3, true))
    }
}
