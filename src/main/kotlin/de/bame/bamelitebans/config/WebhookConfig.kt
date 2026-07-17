package de.bame.bamelitebans.config

import com.moandjiezana.toml.Toml

class WebhookConfig(private val webhookToml: Toml, private val toml: Toml) {
    val isEnabled: Boolean
        get() = webhookToml.getBoolean("webhook.enabled") ?: webhookToml.getBoolean("enabled") ?: toml.getBoolean("webhook.enabled", false)

    val url: String
        get() = webhookToml.getString("webhook.url") ?: webhookToml.getString("url") ?: toml.getString("webhook.url") ?: ""

    fun isTypeEnabled(type: String, removed: Boolean): Boolean {
        val key = when {
            removed && type == "ban" -> "send_unbans"
            removed && type == "mute" -> "send_unmutes"
            removed && type == "warn" -> "send_unwarns"
            type == "ban" -> "send_bans"
            type == "mute" -> "send_mutes"
            type == "warn" -> "send_warns"
            type == "kick" -> "send_kicks"
            else -> return false
        }
        return webhookToml.getBoolean("webhook.$key") ?: webhookToml.getBoolean(key) ?: toml.getBoolean("webhook.$key") ?: toml.getBoolean(key, true)
    }

    fun getTitle(type: String, removed: Boolean): String {
        val key = when {
            removed && type == "ban" -> "title_unban"
            removed && type == "mute" -> "title_unmute"
            removed && type == "warn" -> "title_unwarn"
            type == "ban" -> "title_ban"
            type == "mute" -> "title_mute"
            type == "warn" -> "title_warn"
            type == "kick" -> "title_kick"
            else -> "title_ban"
        }
        val defaultTitle = when (key) {
            "title_unban" -> "✔ Spieler unbanned"
            "title_unmute" -> "🔊 Spieler unmuted"
            "title_unwarn" -> "🗑 Verwarnung unwarned"
            "title_ban" -> "🔨 Spieler banned"
            "title_mute" -> "🔇 Spieler muted"
            "title_warn" -> "⚠ Spieler warned"
            "title_kick" -> "👢 Spieler kicked"
            else -> "🔨 Strafe"
        }
        return webhookToml.getString("webhook.$key") ?: webhookToml.getString(key) ?: toml.getString("webhook.$key", defaultTitle)
    }

    fun getColor(type: String, removed: Boolean): Int {
        val key = when {
            removed && type == "ban" -> "color_unban"
            removed && type == "mute" -> "color_unmute"
            removed && type == "warn" -> "color_unwarn"
            type == "ban" -> "color_ban"
            type == "mute" -> "color_mute"
            type == "warn" -> "color_warn"
            type == "kick" -> "color_kick"
            else -> "color_ban"
        }
        val defaultColor = when (key) {
            "color_unban" -> 65280
            "color_unmute" -> 49151
            "color_unwarn" -> 2142890
            "color_ban" -> 16711680
            "color_mute" -> 8421504
            "color_warn" -> 16776960
            "color_kick" -> 16753920
            else -> 16711680
        }
        return try {
            (webhookToml.getLong("webhook.$key") ?: webhookToml.getLong(key) ?: toml.getLong("webhook.$key"))?.toInt() ?: defaultColor
        } catch (_: Exception) {
            defaultColor
        }
    }
}
