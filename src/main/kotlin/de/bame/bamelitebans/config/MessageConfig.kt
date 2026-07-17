package de.bame.bamelitebans.config

import com.moandjiezana.toml.Toml
import de.bame.bamelitebans.util.ColorParser

class MessageConfig(private val toml: Toml) {
    fun headerSearch(player: String, reason: String, count: Int): String {
        val template = toml.getString("messages.header_search") ?: "<#FFB700>History for <#92F254>{player} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"
        return template
            .replace("{player}", ColorParser.escape(player))
            .replace("{reason}", ColorParser.escape(reason))
            .replace("{count}", count.toString())
    }

    fun headerAll(player: String, count: Int): String {
        val template = toml.getString("messages.header_all") ?: "<#FFB700>History for <#92F254>{player} <#FFB700>('<yellow>{count}<#FFB700>'):"
        return template
            .replace("{player}", ColorParser.escape(player))
            .replace("{count}", count.toString())
    }

    fun staffHeaderSearch(staff: String, reason: String, count: Int): String {
        val template = toml.getString("messages.staff_header_search") ?: "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"
        return template
            .replace("{staff}", ColorParser.escape(staff))
            .replace("{reason}", ColorParser.escape(reason))
            .replace("{count}", count.toString())
    }

    fun staffHeaderAll(staff: String, count: Int): String {
        val template = toml.getString("messages.staff_header_all") ?: "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>('<yellow>{count}<#FFB700>'):"
        return template
            .replace("{staff}", ColorParser.escape(staff))
            .replace("{count}", count.toString())
    }

    val playerNotFound: String
        get() = toml.getString("messages.player_not_found") ?: "<red>sᴘɪᴇʟᴇʀ ɴɪᴄʜᴛ ɢᴇꜰᴜɴᴅᴇɴ."

    val reloadHeader: String
        get() = toml.getString("messages.reload_header") ?: "<white>🗘 <b><gradient:#F92727:#FFFFFF>ʀᴇʟᴏᴀᴅ"

    val reloadSuccess: String
        get() = toml.getString("messages.reload_success") ?: "<white>config und webhook.toml wurden reloaded"

    fun stafftopHeader(period: String): String {
        val template = toml.getString("messages.stafftop_header") ?: "<gold>🏆 <green>Staff-Leaderboard ({period}):"
        return template.replace("{period}", ColorParser.escape(period))
    }

    fun stafftopEmpty(period: String): String {
        val template = toml.getString("messages.stafftop_empty") ?: "<red>Keine Moderationsaktivität im Zeitraum <yellow>{period} <red>gefunden."
        return template.replace("{period}", ColorParser.escape(period))
    }

    fun lastSeenHeader(): String {
        return toml.getString("messages.lastseen_header")
            ?: "<white>⌚ <b><gradient:#FFFE00:#F9F869>ʟᴀsᴛ sᴇᴇɴ"
    }

    fun lastSeenFormat(
        prefixName: String,
        warZuletztAm: String,
        dateStr: String,
        um: String,
        timeStr: String,
        auf: String,
        serverStr: String,
        onlineStr: String
    ): String {
        var template = toml.getString("messages.lastseen_format")
            ?: "{prefix_name}<reset> <gray>{war_zuletzt_am} <#FFFE00>{date} <gray>{um} <#FFFE00>{time} <gray>{auf} <#FFFE00>{server} <gray>{online}"
        if (!template.contains("{online}")) {
            template = "$template <gray>{online}"
        }
        return template
            .replace("{prefix_name}", prefixName)
            .replace("{war_zuletzt_am}", warZuletztAm)
            .replace("{date}", dateStr)
            .replace("{um}", um)
            .replace("{time}", timeStr)
            .replace("{auf}", auf)
            .replace("{server}", serverStr)
            .replace("{online}", onlineStr)
    }

    fun searchBanListHeader(page: Int, total: Int): String {
        val template = toml.getString("messages.searchbanlist_header")
            ?: "<white>=== <green>ᴘᴀɢᴇ <gold>{page} <green>ᴏᴜᴛ ᴏꜰ <gold>{total} <white>==="
        return template
            .replace("{page}", page.toString())
            .replace("{total}", total.toString())
    }
}
