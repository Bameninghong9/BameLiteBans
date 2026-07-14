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
            actor.reply(ColorParser.parse("<red>Verwendung: /lastseen <spieler>"))
            return
        }

        lastSeenService.fetchLastSeen(playerName).thenAccept { entry ->
            if (entry == null) {
                actor.reply(ColorParser.parse("<red>Keine Daten zu <yellow>$playerName <red>gefunden."))
                return@thenAccept
            }

            val rawPrefix = luckPermsService.getPrefix(entry.uuid).ifEmpty { "<green>" }
            val prefix = SmallCaps.convertPreservingTags(rawPrefix)
            val prefixName = "$prefix<!bold><!italic>${entry.name}"

            val instant = Instant.ofEpochMilli(entry.timestampMillis)
            val dateStr = DATE_FORMATTER.format(instant)
            val timeStr = TIME_FORMATTER.format(instant)

            val warZuletztAm = SmallCaps.convert("war zuletzt am")
            val um = SmallCaps.convert("um")
            val auf = SmallCaps.convert("auf")
            val serverStr = SmallCaps.convert(entry.server)
            val onlineStr = SmallCaps.convert("online")

            val formattedMessage = configService.lastSeenFormat(
                prefixName, warZuletztAm, dateStr, um, timeStr, auf, serverStr, onlineStr
            )

            actor.reply(ColorParser.parse(""))
            actor.reply(ColorParser.parse(configService.lastSeenHeader()))
            actor.reply(ColorParser.parse(formattedMessage))
            actor.reply(ColorParser.parse(""))
        }
    }
}
