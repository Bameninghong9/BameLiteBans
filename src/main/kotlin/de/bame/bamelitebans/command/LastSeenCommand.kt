package de.bame.bamelitebans.command

import de.bame.bamelitebans.autocomplete.OnlinePlayerSuggestionProvider
import de.bame.bamelitebans.config.ConfigService
import de.bame.bamelitebans.service.LastSeenService
import de.bame.bamelitebans.service.LuckPermsService
import de.bame.bamelitebans.util.ColorParser
import de.bame.bamelitebans.util.SmallCaps
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.SuggestWith
import revxrsal.commands.velocity.actor.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Befehlsklasse für /lastseen <spieler>
 */
class LastSeenCommand(
    private val lastSeenService: LastSeenService,
    private val luckPermsService: LuckPermsService,
    private val configService: ConfigService
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    }

    @Command("lastseen")
    @CommandPermission("bamelitebans.command.lastseen")
    fun onLastSeen(
        actor: VelocityCommandActor,
        @Optional @SuggestWith(OnlinePlayerSuggestionProvider::class) playerName: String?
    ) {
        if (playerName.isNullOrBlank()) {
            de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Verwendung: /lastseen <spieler>")
            return
        }

        lastSeenService.fetchLastSeen(playerName).thenAccept { entry ->
            if (entry == null) {
                val safeName = ColorParser.escape(playerName)
                de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Keine Daten zu <yellow>$safeName <white>gefunden.")
                return@thenAccept
            }

            val rawPrefix = luckPermsService.getPrefix(entry.uuid).ifEmpty { "<green>" }
            val prefix = SmallCaps.convertPreservingTags(rawPrefix)
            val safeName = ColorParser.escape(entry.name)
            val prefixName = "$prefix<!bold><!italic>$safeName"

            val instant = Instant.ofEpochMilli(entry.timestampMillis)
            val dateStr = DATE_FORMATTER.format(instant)
            val timeStr = TIME_FORMATTER.format(instant)

            val warZuletztAm = SmallCaps.convert("war zuletzt am")
            val um = SmallCaps.convert("um")
            val auf = SmallCaps.convert("auf")
            val safeServer = ColorParser.escape(entry.server)
            val serverStr = SmallCaps.convert(safeServer)
            val onlineStr = if (entry.isOnline) "<green>${SmallCaps.convert("[online]")}" else "<red>${SmallCaps.convert("[offline]")}"

            val formattedMessage = configService.messages.lastSeenFormat(
                prefixName, warZuletztAm, dateStr, um, timeStr, auf, serverStr, onlineStr
            )

            actor.reply(ColorParser.parse(""))
            actor.reply(ColorParser.parse(configService.messages.lastSeenHeader()))
            actor.reply(ColorParser.parse(formattedMessage))
            actor.reply(ColorParser.parse(""))
        }.exceptionally { e ->
            org.slf4j.LoggerFactory.getLogger(LastSeenCommand::class.java).error("Fehler bei /lastseen", e)
            de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Ein Fehler ist aufgetreten.")
            null
        }
    }
}

