package de.bame.bamelitebans.config

import com.moandjiezana.toml.Toml
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ConfigService(private val dataDirectory: Path) {

    private var toml: Toml = Toml()
    private var webhookToml: Toml = Toml()
    private var automodToml: Toml = Toml()

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

        val webhookFile = dataDirectory.resolve("webhook.toml").toFile()
        if (!webhookFile.exists()) {
            writeDefaultWebhookConfig(webhookFile)
        }
        webhookToml = Toml().read(webhookFile)

        val automodFile = dataDirectory.resolve("automod.toml").toFile()
        if (!automodFile.exists()) {
            writeDefaultAutoModConfig(automodFile)
        }
        automodToml = Toml().read(automodFile)
    }

    private fun writeDefaultConfig(file: File) {
        val defaultContent = """
            # =========================================================
            #             BAME LITEBANS - HAUPTKONFIGURATION
            #   High-Performance Moderation & History Suite für Velocity
            # =========================================================

            # ---------------------------------------------------------
            # [messages] - Alle Chat-Ausgaben und Nachrichten
            # Unterstützt MiniMessage (<gold>, <#FFB700>, <b>) & Legacy Hex
            # ---------------------------------------------------------
            [messages]

            # --- /searchhistory (Spieler-Verlauf) ---
            # Header bei Suche MIT spezifischem Grund (z.B. /searchhistory Spieler hacks)
            header_search = "<#FFB700>History for <#92F254>{player} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"
            # Header bei Suche OHNE spezifischen Grund (z.B. /searchhistory Spieler)
            header_all = "<#FFB700>History for <#92F254>{player} <#FFB700>('<yellow>{count}<#FFB700>'):"

            # --- /searchstaffhistory (Staff-Verlauf) ---
            # Header bei Staff-Suche MIT spezifischem Grund
            staff_header_search = "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"
            # Header bei Staff-Suche OHNE spezifischen Grund
            staff_header_all = "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>('<yellow>{count}<#FFB700>'):"

            # --- /stafftop (Leaderboard) ---
            # Header des Staff-Leaderboards
            stafftop_header = "<gold>🏆 <green>Staff-Leaderboard ({period}):"
            # Meldung, wenn im gewählten Zeitraum keine Strafen gefunden wurden
            stafftop_empty = "<red>Keine Moderationsaktivität im Zeitraum <yellow>{period} <red>gefunden."

            # --- /lastseen (Online-/Offline-Status & Letzter Server) ---
            # Header über der LastSeen-Ausgabe
            lastseen_header = "<white>⌚ <b><gradient:#FFFE00:#F9F869>ʟᴀsᴛ sᴇᴇɴ"
            # Formatzeile für die Ausgabe (Platzhalter: {prefix_name}, {war_zuletzt_am}, {date}, {um}, {time}, {auf}, {server}, {online})
            lastseen_format = "{prefix_name}<reset> <gray>{war_zuletzt_am} <#FFFE00>{date} <gray>{um} <#FFFE00>{time} <gray>{auf} <#FFFE00>{server} <gray>{online}"

            # --- /searchbanlist (Paginierte Bannliste nach Grund) ---
            # Header der seitenbasierten Bannliste
            searchbanlist_header = "<white>=== <green>ᴘᴀɢᴇ <gold>{page} <green>ᴏᴜᴛ ᴏꜰ <gold>{total} <white>==="

            # --- Allgemeine System-Nachrichten ---
            # Fehlermeldung, wenn ein Spieler oder Teammitglied in der Datenbank nicht existiert
            player_not_found = "<red>sᴘɪᴇʟᴇʀ ɴɪᴄʜᴛ ɢᴇꜰᴜɴᴅᴇɴ."
            # Reload-Header und Nachricht (/bamelitebans reload)
            reload_header = "<white>🗘 <b><gradient:#F92727:#FFFFFF>ʀᴇʟᴏᴀᴅ"
            reload_success = "<white>config und webhook.toml wurden reloaded"


            # ---------------------------------------------------------
            # [punishment] - Strafanzeigen & Synonym-Gruppen
            # ---------------------------------------------------------
            [punishment]

            # Tag hinter einer aktiven Strafe in der Historie
            active_tag = "<white> [<red>ᴀᴋᴛɪᴠ<white>]"
            # Tag hinter einer abgelaufenen oder aufgehobenen Strafe
            expired_tag = "<white> [<#828FE7>ᴀʙɢᴇʟᴀᴜꜰᴇɴ<white>]"

            # Suchgruppen / Synonyme für /searchhistory & /searchstaffhistory:
            # Wenn ein Moderator nach einem Begriff aus einer Gruppe sucht (z.B. "Cheats"),
            # durchsucht das Plugin die Datenbank automatisch nach allen Begriffen dieser Gruppe.
            search_groups = [
                ["hacks", "cheats", "unerlaubte clientmodifikation", "clientmodifikation"]
            ]
        """.trimIndent()

        file.writeText(defaultContent, Charsets.UTF_8)
    }

    private fun writeDefaultWebhookConfig(file: File) {
        val defaultContent = """
            # =========================================================
            #          BAME LITEBANS - DISCORD WEBHOOK CONFIG
            #   Automatische Benachrichtigungen bei Strafen & Entbannungen
            # =========================================================

            [webhook]
            # Discord Webhook aktivieren / deaktivieren
            enabled = true
            url = "DEINE_WEBHOOK_URL"

            # Welche Strafen & Entbannungen sollen als Webhook gesendet werden?
            send_bans = true
            send_mutes = true
            send_warns = false
            send_kicks = true
            send_unbans = true
            send_unmutes = true
            send_unwarns = false

            # Titel für Discord Embeds
            title_ban = "🔨 Spieler banned"
            title_mute = "🔇 Spieler muted"
            title_warn = "⚠ Spieler warned"
            title_kick = "👢 Spieler kicked"
            title_unban = "✔ Spieler unbanned"
            title_unmute = "🔊 Spieler unmuted"
            title_unwarn = "🗑 Verwarnung unwarned"

            # Farben in Hex/Dezimal
            color_ban = 16711680      # Rot
            color_mute = 8421504      # Grau
            color_warn = 16776960     # Gelb
            color_kick = 16753920     # Orange
            color_unban = 65280       # Grün
            color_unmute = 49151      # Hellblau
            color_unwarn = 2142890    # Türkis
        """.trimIndent()

        file.writeText(defaultContent, Charsets.UTF_8)
    }

    private fun writeDefaultAutoModConfig(file: File) {
        val defaultContent = """
            # ==============================================================================
            #                 BAME LITEBANS - AUTO MODERATION & KI ENGINE
            #          Automatische Erkennung, Blockierung & Bestrafung bei Verstößen
            # ==============================================================================

            [automod]
            # Hauptschalter: Das gesamte AutoMod-System aktivieren oder deaktivieren
            # Standardmäßig auf 'false' (zum Aktivieren auf 'true' setzen und /bamelitebans reload ausführen)
            enabled = true


            # ==============================================================================
            # 1. SPAM-SCHUTZ (Nachrichten-Duplikate verhindern)
            # ==============================================================================
            [spam]
            enabled = true
            # Nach wie vielen gleichen Nachrichten soll blockiert werden? (2 = ab der 3. Nachricht blocken)
            max_duplicates = 3
            # Nach wie vielen Minuten wird der Zähler für dieselbe Nachricht zurückgesetzt?
            cooldown_minutes = 10
            # Nachricht an den Spieler bei Blockierung wegen Spam
            player_message = "<red>Please do not spam the same message!"
            # Benachrichtigung an das Team im Chat und an die Konsole
            staff_message = "<red>Player %player% tried to spam!"
            # Ab wie vielen Versuchen soll zusätzlich automatisch gemutet werden? (0 = nur blockieren & warnen)
            mute_after_attempts = 1000
            command = ""


            # ==============================================================================
            # 2. MUTES (Stummschaltungen)
            # ==============================================================================
            [mutes]
            # Mutes aktivieren/deaktivieren.
            # Wenn 'enabled = false': Nachrichten mit Mute-Gründen werden TROTZDEM im Chat GEBLOCKT
            # und die 'blocked_message' gesendet, aber es wird KEIN Mute-Befehl (/tempmute) ausgeführt!
            enabled = false
            blocked_message = "<red>Deine Message wurde blockiert, da du dich nicht an die Chat Regeln gehalten hast"

                [mutes.reasons]
                beleidigung = "/tempmute %player% 1h Beleidigung"
                provokation = "/tempmute %player% 1h Provokation"
                sexistisches_verhalten = "/tempmute %player% 6h Sexistisches Verhalten"
                server_hetze = "/tempmute %player% 7d Server hetze"
                nsfw = "/tempmute %player% 7d NSFW / Unangemessene Inhalte"


            # ==============================================================================
            # 3. BANS (Ausschlüsse)
            # Bans werden bei Verstoß IMMER ausgeführt (solange [automod] enabled = true ist)
            # ==============================================================================
            [bans]
                [bans.reasons]
                werbung = "/tempban %player% 7d Werbung"
                rassismus = "/tempban %player% 30d Rassismus"
                nationalsozialismus = "/tempban %player% 30d Nationalsozialismus"
                antisemitismus = "/tempban %player% 30d Antisemitismus"

                # Erlaubte Domains/IPs, die NICHT als Werbung erkannt und nicht gebannt werden:
                allowed_domains = ["srino.net", "test.net"]


            # ==============================================================================
            # 4. KI / AI HEURISTIK & API EINSTELLUNGEN
            # ==============================================================================
            [ai]
            # Optional: Falls API-Key leer ("") ist, läuft unsere blitzschnelle lokale Smart-Regex direkt am Proxy (empfohlen)
            api_key = ""
            api_url = "https://api.openai.com/v1/chat/completions"
            model = "gpt-4o-mini"
            # Team-Benachrichtigung bei Auto-Punish (Mute / Ban) im Chat?
            notify_team = true
            notify_message = "<red>🤖 <b>[KI-AutoMod]</b> <yellow>%player% <gray>wurde automatisch bestraft wegen: <red><b>%category%</b> <gray>(%message%)"
        """.trimIndent()

        file.writeText(defaultContent, Charsets.UTF_8)
    }

    fun headerSearch(player: String, reason: String, count: Int): String {
        val template = toml.getString("messages.header_search") ?: "<#FFB700>History for <#92F254>{player} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"
        return template
            .replace("{player}", de.bame.bamelitebans.util.ColorParser.escape(player))
            .replace("{reason}", de.bame.bamelitebans.util.ColorParser.escape(reason))
            .replace("{count}", count.toString())
    }

    fun headerAll(player: String, count: Int): String {
        val template = toml.getString("messages.header_all") ?: "<#FFB700>History for <#92F254>{player} <#FFB700>('<yellow>{count}<#FFB700>'):"
        return template
            .replace("{player}", de.bame.bamelitebans.util.ColorParser.escape(player))
            .replace("{count}", count.toString())
    }

    fun staffHeaderSearch(staff: String, reason: String, count: Int): String {
        val template = toml.getString("messages.staff_header_search") ?: "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>({reason} '<yellow>{count}<#FFB700>'):"
        return template
            .replace("{staff}", de.bame.bamelitebans.util.ColorParser.escape(staff))
            .replace("{reason}", de.bame.bamelitebans.util.ColorParser.escape(reason))
            .replace("{count}", count.toString())
    }

    fun staffHeaderAll(staff: String, count: Int): String {
        val template = toml.getString("messages.staff_header_all") ?: "<#FFB700>Staff-History for <#92F254>{staff} <#FFB700>('<yellow>{count}<#FFB700>'):"
        return template
            .replace("{staff}", de.bame.bamelitebans.util.ColorParser.escape(staff))
            .replace("{count}", count.toString())
    }

    val playerNotFound: String
        get() = toml.getString("messages.player_not_found") ?: "<red>sᴘɪᴇʟᴇʀ ɴɪᴄʜᴛ ɢᴇꜰᴜɴᴅᴇɴ."

    val reloadHeader: String
        get() = toml.getString("messages.reload_header") ?: "<white>🗘 <b><gradient:#F92727:#FFFFFF>ʀᴇʟᴏᴀᴅ"

    val reloadSuccess: String
        get() = toml.getString("messages.reload_success") ?: "<white>config und webhook.toml wurden reloaded"

    val activeTag: String
        get() = toml.getString("punishment.active_tag") ?: "<white> [<red>ᴀᴋᴛɪᴠ<white>]"

    val expiredTag: String
        get() = toml.getString("punishment.expired_tag") ?: "<white> [<#828FE7>ᴀʙɢᴇʟᴀᴜꜰᴇɴ<white>]"

    fun stafftopHeader(period: String): String {
        val template = toml.getString("messages.stafftop_header") ?: "<gold>🏆 <green>Staff-Leaderboard ({period}):"
        return template.replace("{period}", de.bame.bamelitebans.util.ColorParser.escape(period))
    }

    fun stafftopEmpty(period: String): String {
        val template = toml.getString("messages.stafftop_empty") ?: "<red>Keine Moderationsaktivität im Zeitraum <yellow>{period} <red>gefunden."
        return template.replace("{period}", de.bame.bamelitebans.util.ColorParser.escape(period))
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

    fun searchBanListHeader(page: Int, total: Int): String {
        val template = toml.getString("messages.searchbanlist_header")
            ?: "<white>=== <green>ᴘᴀɢᴇ <gold>{page} <green>ᴏᴜᴛ ᴏꜰ <gold>{total} <white>==="
        return template
            .replace("{page}", page.toString())
            .replace("{total}", total.toString())
    }

    val isWebhookEnabled: Boolean
        get() = webhookToml.getBoolean("webhook.enabled") ?: webhookToml.getBoolean("enabled") ?: toml.getBoolean("webhook.enabled", false)

    val webhookUrl: String
        get() = webhookToml.getString("webhook.url") ?: webhookToml.getString("url") ?: toml.getString("webhook.url") ?: ""

    fun isWebhookTypeEnabled(type: String, removed: Boolean): Boolean {
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

    fun getWebhookTitle(type: String, removed: Boolean): String {
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

    fun getWebhookColor(type: String, removed: Boolean): Int {
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

    val isAutoModEnabled: Boolean
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

    val aiApiKey: String
        get() = automodToml.getString("ai.api_key") ?: automodToml.getString("api_key") ?: ""

    val aiApiUrl: String
        get() = automodToml.getString("ai.api_url") ?: automodToml.getString("api_url") ?: "https://api.openai.com/v1/chat/completions"

    val aiModel: String
        get() = automodToml.getString("ai.model") ?: automodToml.getString("model") ?: "gpt-4o-mini"

    val notifyTeamAutoMod: Boolean
        get() = automodToml.getBoolean("ai.notify_team") ?: automodToml.getBoolean("notify_team") ?: true

    val notifyMessageAutoMod: String
        get() = automodToml.getString("ai.notify_message") ?: automodToml.getString("notify_message") ?: "<red>🤖 <b>[KI-AutoMod]</b> <yellow>%player% <gray>wurde automatisch bestraft wegen: <red><b>%category%</b> <gray>(%message%)"

    fun isMuteCategory(category: String): Boolean {
        val cat = category.lowercase().trim()
        if (cat in listOf("beleidigung", "provokation", "sexistisches_verhalten", "server_hetze", "nsfw", "spam")) return true
        return automodToml.getString("mutes.reasons.$cat") != null
    }

    fun getAutoModCommand(category: String): String? {
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
