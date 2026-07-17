package de.bame.bamelitebans.config

import com.moandjiezana.toml.Toml
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class ConfigService(private val dataDirectory: Path) {

    private var toml: Toml = Toml()
    private var webhookToml: Toml = Toml()
    private var automodToml: Toml = Toml()

    lateinit var messages: MessageConfig
        private set
    lateinit var punishment: PunishmentConfig
        private set
    lateinit var webhook: WebhookConfig
        private set
    lateinit var automod: AutoModConfig
        private set

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

        // Initialisiere die modularen Config-Klassen
        messages = MessageConfig(toml)
        punishment = PunishmentConfig(toml)
        webhook = WebhookConfig(webhookToml, toml)
        automod = AutoModConfig(automodToml)
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

            # --- /staffdashboard (Link zur Webseite) ---
            staffdashboard_message = "<gray>🌐 <white>Öffne das Staff-Dashboard: <click:open_url:'https://bamenetwork.de/staff'><hover:show_text:'<green>Klicke hier, um die Webseite zu öffnen'><#6366f1><u>https://bamenetwork.de/staff</u></hover></click>"

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
}
