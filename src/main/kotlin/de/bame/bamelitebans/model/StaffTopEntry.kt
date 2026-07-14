package de.bame.bamelitebans.model

/**
 * Repräsentiert einen Statistik-Eintrag für das /stafftop Leaderboard.
 */
data class StaffTopEntry(
    val staffName: String,
    var staffUuid: String? = null,
    var luckPermsPrefix: String = "",
    var bans: Int = 0,
    var mutes: Int = 0,
    var warns: Int = 0,
    var kicks: Int = 0
) {
    val total: Int
        get() = bans + mutes + warns + kicks
}
