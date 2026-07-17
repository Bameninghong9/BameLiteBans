package de.bame.bamelitebans.config

import com.moandjiezana.toml.Toml

class AutoModConfig(private val automodToml: Toml) {
    val isEnabled: Boolean
        get() = automodToml.getBoolean("automod.enabled") ?: automodToml.getBoolean("ai.enabled") ?: automodToml.getBoolean("enabled") ?: false

    val isSpamEnabled: Boolean
        get() = automodToml.getBoolean("spam.enabled") ?: true

    val spamMaxDuplicates: Int
        get() = (automodToml.getLong("spam.max_duplicates") ?: 2L).toInt()

    val spamCooldownMinutes: Long
        get() = automodToml.getLong("spam.cooldown_minutes") ?: 10L

    val spamPlayerMessage: String
        get() = automodToml.getString("spam.player_message") ?: "<red>Please do not spam the same message!"

    val spamStaffMessage: String
        get() = automodToml.getString("spam.staff_message") ?: "<red>Player %player% tried to spam!"

    val spamMuteAfterAttempts: Int
        get() = (automodToml.getLong("spam.mute_after_attempts") ?: 4L).toInt()

    val spamCommand: String
        get() = automodToml.getString("spam.command") ?: "/tempmute %player% 30m Spam"

    val areMutesEnabled: Boolean
        get() = automodToml.getBoolean("mutes.enabled") ?: true

    val mutesBlockedMessage: String
        get() = automodToml.getString("mutes.blocked_message") ?: "<red>Deine Message wurde blockiert, da du dich nicht an die Chat Regeln gehalten hast"

    val apiKey: String
        get() = automodToml.getString("ai.api_key") ?: automodToml.getString("api_key") ?: ""

    val apiUrl: String
        get() = automodToml.getString("ai.api_url") ?: automodToml.getString("api_url") ?: "https://api.openai.com/v1/chat/completions"

    val model: String
        get() = automodToml.getString("ai.model") ?: automodToml.getString("model") ?: "gpt-4o-mini"

    val notifyTeam: Boolean
        get() = automodToml.getBoolean("ai.notify_team") ?: automodToml.getBoolean("notify_team") ?: true

    val notifyMessage: String
        get() = automodToml.getString("ai.notify_message") ?: automodToml.getString("notify_message") ?: "<red>🤖 <b>[KI-AutoMod]</b> <yellow>%player% <gray>wurde automatisch bestraft wegen: <red><b>%category%</b> <gray>(%message%)"

    fun isMuteCategory(category: String): Boolean {
        val cat = category.lowercase().trim()
        if (cat in listOf("beleidigung", "provokation", "sexistisches_verhalten", "server_hetze", "nsfw", "spam")) return true
        return automodToml.getString("mutes.reasons.$cat") != null
    }

    fun getCommand(category: String): String? {
        val cat = category.lowercase().trim()
        return automodToml.getString("mutes.reasons.$cat")
            ?: automodToml.getString("bans.reasons.$cat")
            ?: automodToml.getString("$cat.command")
            ?: automodToml.getString("command", if (isMuteCategory(cat)) "/tempmute %player% 1d $cat" else "/tempban %player% 30d $cat")
    }

    val allowedAdvertisingDomains: List<String>
        get() {
            val list = try {
                automodToml.getList<String>("bans.allowed_domains")
                    ?: automodToml.getList<String>("werbung.allowed_domains")
                    ?: automodToml.getList<String>("werbung.allowedips")
                    ?: automodToml.getList<String>("werbung.allowed")
            } catch (_: Exception) { null }
            return list ?: listOf("srino.net", "test.net")
        }
}
