package de.bame.bamelitebans.model

/**
 * Repräsentiert einen LastSeen-Eintrag für einen Spieler.
 */
data class LastSeenEntry(
    val uuid: String,
    val name: String,
    val timestampMillis: Long,
    val server: String,
    val isOnline: Boolean = false
)
