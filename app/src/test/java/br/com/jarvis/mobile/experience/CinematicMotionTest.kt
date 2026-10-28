package br.com.jarvis.mobile.experience

import org.junit.Assert.*
import org.junit.Test

class CinematicMotionTest {
    @Test fun hiddenAndReducedMotionNeverScheduleAClock() {
        for (p in CorePhase.entries) for (m in OperationMode.entries) {
            assertEquals(0L, CinematicMotion.interval(false, true, p, m))
            assertEquals(0L, CinematicMotion.interval(true, false, p, m))
        }
        assertEquals(0L, CinematicMotion.interval(true, true, CorePhase.IDLE, OperationMode.SILENT))
    }
    @Test fun activeDrawingHasAFrameBudgetAndSafeAmplitude() {
        for (p in CorePhase.entries) for (m in OperationMode.entries)
            assertTrue(CinematicMotion.interval(true, true, p, m).let { it == 0L || it >= 33L })
        assertNull(CinematicMotion.level(Float.NaN)); assertNull(CinematicMotion.level(Float.POSITIVE_INFINITY))
        assertEquals(0f, CinematicMotion.level(-8f)!!, 0f); assertEquals(1f, CinematicMotion.level(9f)!!, 0f)
        assertTrue(CinematicMotion.speed(CorePhase.PROCESSING) > CinematicMotion.speed(CorePhase.IDLE))
    }
}
