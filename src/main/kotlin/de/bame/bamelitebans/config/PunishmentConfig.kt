package de.bame.bamelitebans.config

import com.moandjiezana.toml.Toml

class PunishmentConfig(private val toml: Toml) {
    val activeTag: String
        get() = toml.getString("punishment.active_tag") ?: "<white> [<red>ᴀᴋᴛɪᴠ<white>]"

    val expiredTag: String
        get() = toml.getString("punishment.expired_tag") ?: "<white> [<#828FE7>ᴀʙɢᴇʟᴀᴜꜰᴇɴ<white>]"

    fun getSearchKeywords(query: String?): List<String> {
        if (query.isNullOrBlank()) return emptyList()
        val lowerQuery = query.lowercase().trim()
        val result = mutableSetOf(lowerQuery)

        try {
            val groups = toml.getList<List<String>>("punishment.search_groups")
                ?: toml.getList<List<String>>("search_groups")
            if (groups != null) {
                for (group in groups) {
                    if (group.any { it.equals(lowerQuery, ignoreCase = true) }) {
                        result.addAll(group)
                    }
                }
            }
        } catch (_: Exception) {}

        return result.toList()
    }
}
