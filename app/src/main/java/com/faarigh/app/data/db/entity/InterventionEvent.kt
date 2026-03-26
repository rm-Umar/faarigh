package com.faarigh.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "intervention_events",
    indices = [
        Index("timestamp"),
        Index("moduleId"),
        Index("appPackage"),
    ],
)
data class InterventionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val moduleId: String,        // "app_pause", "nsfw_detection", "shorts_blocker"
    val appPackage: String,      // which app triggered it
    val appName: String,         // display name
    val action: String,          // "shown", "allowed", "blocked", "dismissed"
    val durationMs: Long = 0,    // how long they paused before deciding
    val escalationLevel: String? = null,  // "light", "medium", "deep", "wind_down"
    val promptShown: String? = null,       // the reflective prompt text shown (if any)
)
