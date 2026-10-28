package br.com.jarvis.mobile.experience

/** Frame budget, not a perpetual animation: no clock when hidden, silent idle or motion disabled. */
object CinematicMotion {
    fun interval(visible: Boolean, animations: Boolean, phase: CorePhase, mode: OperationMode): Long = when {
        !visible || !animations || (mode == OperationMode.SILENT && phase == CorePhase.IDLE) -> 0L
        mode == OperationMode.SILENT -> 100L
        phase == CorePhase.IDLE && mode == OperationMode.ASSISTANT -> 125L
        // Leave CPU for local inference; full-rate processing remains explicit in Cinema.
        phase == CorePhase.PROCESSING && mode == OperationMode.ASSISTANT -> 100L
        phase == CorePhase.IDLE -> 50L
        else -> 33L
    }
    fun level(value: Float?) = value?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
    fun speed(phase: CorePhase) = when (phase) {
        CorePhase.INITIALIZING -> 1.1f
        CorePhase.IDLE -> 0.35f
        CorePhase.LISTENING -> 0.65f
        CorePhase.PROCESSING -> 1.9f
        CorePhase.RESPONDING -> 0.85f
        CorePhase.ERROR -> 0.3f
    }
}
