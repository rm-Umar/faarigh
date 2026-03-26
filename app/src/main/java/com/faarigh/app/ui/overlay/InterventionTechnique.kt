package com.faarigh.app.ui.overlay

enum class InterventionTechnique(val key: String, val label: String, val description: String) {
    SIMPLE_PAUSE("simple_pause", "Simple pause", "Just a moment to decide"),
    PHYSIOLOGICAL_SIGH("sigh", "Breathing exercise", "Guided breathing (10s)"),
    COGNITIVE_DEFUSION("defusion", "Notice the urge", "Observe without acting (10s)"),
    URGE_SURFING("surfing", "Ride it out", "Surf the wave (15s)"),
    ;

    companion object {
        fun fromKey(key: String): InterventionTechnique =
            entries.firstOrNull { it.key == key } ?: PHYSIOLOGICAL_SIGH
    }
}
