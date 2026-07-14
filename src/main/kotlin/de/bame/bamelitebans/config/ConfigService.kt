package de.bame.bamelitebans.config

import com.moandjiezana.toml.Toml
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ConfigService(private val dataDirectory: Path) {

    private var toml: Toml = Toml()

    init {
        loadConfig()
    }

    fun loadConfig() {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory)
        }

        val configFile = dataDirectory.resolve("config.toml").toFile()
        if (!configFile.exists()) {
            writeDefaultConfig(configFile)
        }

        toml = Toml().read(configFile)
    }

    private fun writeDefaultConfig(file: File) {
        val defaultContent = """
            # =========================================================
            #  BameLiteBans Konfiguration
            #  Erweiterte Such- & Staff-Historie für LiteBans
            # =========================================================

            [messages]
            # Header bei Spielersuche mit Suchbegriff (z.B. /searchhistory John6Pork7 hacks)
            header_search = "<#FFB700>History for <#92F254>{player} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"

            # Header bei Spielersuche ohne Suchbegriff (z.B. /searchhistory John6Pork7)
            header_all = "<#FFB700>History for <#92F254>{player} <#FFB700>('<yellow>{count}<#FFB700>'):"

            # Header bei Staff-Suche mit Suchbegriff (z.B. /searchstaffhistory Bameninghong9 hacks)
            staff_header_search = "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"

            # Header bei Staff-Suche ohne Suchbegriff (z.B. /searchstaffhistory Bameninghong9)
            staff_header_all = "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>('<yellow>{count}<#FFB700>'):"

            # Fehlermeldung wenn Spieler/Staff nicht gefunden wurde
            player_not_found = "<red>sᴘɪᴇʟᴇʀ ɴɪᴄʜᴛ ɢᴇꜰᴜɴᴅᴇɴ."

            # Header für /stafftop Leaderboard
            stafftop_header = "<gold>🏆 <green>Staff-Leaderboard ({period}):"

            # Header und Format für /lastseen <spieler> (MiniMessage & Legacy Hex unterstützt)
            lastseen_header = "<white>⌚ <b><gradient:#FFFE00:#F9F869>ʟᴀsᴛ sᴇᴇɴ"
            lastseen_format = "{prefix_name}<reset> <gray>{war_zuletzt_am} <#FFFE00>{date} <gray>{um} <#FFFE00>{time} <gray>{auf} <#FFFE00>{server} <gray>{online}"

            # Meldung wenn im gewählten Zeitraum keine Strafen gefunden wurden
            stafftop_empty = "<red>Keine Moderationsaktivität im Zeitraum <yellow>{period} <red>gefunden."

            # Nachricht nach erfolgreichem Reload (/bamelitebans reload)
            reload_success = "<green>[BameLiteBans] config.toml erfolgreich neu geladen!"

            [punishment]
            # Tag für aktive Strafen
            active_tag = "<white> [<red>ᴀᴋᴛɪᴠ<white>]"

            # Tag für abgelaufene Strafen
            expired_tag = "<white> [<#828FE7>ᴀʙɢᴇʟᴀᴜꜰᴇɴ<white>]"

            # Suchgruppen / Synonyme für /searchhistory & /searchstaffhistory:
            # Sucht man nach einem Begriff einer Gruppe (z.B. "Cheats" oder "Hacks"),
            # werden automatisch alle Gründe angezeigt, die irgendeinen Begriff aus dieser Gruppe enthalten.
            search_groups = [
                ["hacks", "cheats", "unerlaubte clientmodifikation", "clientmodifikation"]
            ]
        """.trimIndent()

        file.writeText(defaultContent, Charsets.UTF_8)
    }

    fun headerSearch(player: String, reason: String, count: Int): String {
        val template = toml.getString("messages.header_search") ?: "<#FFB700>History for <#92F254>{player} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"
        return template
            .replace("{player}", player)
            .replace("{reason}", reason)
            .replace("{count}", count.toString())
    }

    fun headerAll(player: String, count: Int): String {
        val template = toml.getString("messages.header_all") ?: "<#FFB700>History for <#92F254>{player} <#FFB700>('<yellow>{count}<#FFB700>'):"
        return template
            .replace("{player}", player)
            .replace("{count}", count.toString())
    }

    fun staffHeaderSearch(staff: String, reason: String, count: Int): String {
        val template = toml.getString("messages.staff_header_search") ?: "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"
        return template
            .replace("{staff}", staff)
            .replace("{reason}", reason)
            .replace("{count}", count.toString())
    }

    fun staffHeaderAll(staff: String, count: Int): String {
        val template = toml.getString("messages.staff_header_all") ?: "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>('<yellow>{count}<#FFB700>'):"
        return template
            .replace("{staff}", staff)
            .replace("{count}", count.toString())
    }

    val playerNotFound: String
        get() = toml.getString("messages.player_not_found") ?: "<red>sᴘɪᴇʟᴇʀ ɴɪᴄʜᴛ ɢᴇꜰᴜɴᴅᴇɴ."

    val reloadSuccess: String
        get() = toml.getString("messages.reload_success") ?: "<green>[BameLiteBans] config.toml erfolgreich neu geladen!"

    val activeTag: String
        get() = toml.getString("punishment.active_tag") ?: "<white> [<red>ᴀᴋᴛɪᴠ<white>]"

    val expiredTag: String
        get() = toml.getString("punishment.expired_tag") ?: "<white> [<#828FE7>ᴀʙɢᴇʟᴀᴜꜰᴇɴ<white>]"

    fun stafftopHeader(period: String): String {
        var template = toml.getString("messages.stafftop_header") ?: "<gold>🏆 <green>Staff-Leaderboard ({period}):"
        if (template.contains("Moderations-Leaderboard") || template.contains("sᴛᴀꜰꜰ-ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ")) {
            template = "<gold>🏆 <green>Staff-Leaderboard ({period}):"
        }
        return template.replace("{period}", period)
    }

    fun stafftopEmpty(period: String): String {
        val template = toml.getString("messages.stafftop_empty") ?: "<red>Keine Moderationsaktivität im Zeitraum <yellow>{period} <red>gefunden."
        return template.replace("{period}", period)
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
        if (template.startsWith("<white>⌚ ")) {
            template = template.removePrefix("<white>⌚ ").trimStart()
        }
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

        if (result.size == 1 && (lowerQuery == "cheats" || lowerQuery == "hacks" || lowerQuery == "clientmodifikation" || lowerQuery.contains("clientmod"))) {
            result.addAll(listOf("hacks", "cheats", "unerlaubte clientmodifikation", "clientmodifikation"))
        }

        return result.toList()
    }
}
