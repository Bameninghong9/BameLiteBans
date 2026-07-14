package de.bame.bamelitebans.command

import de.bame.bamelitebans.autocomplete.StaffTopPeriodSuggestionProvider
import de.bame.bamelitebans.config.ConfigService
import de.bame.bamelitebans.service.LiteBansHistoryService
import de.bame.bamelitebans.service.LuckPermsService
import de.bame.bamelitebans.util.ColorParser
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.SuggestWith
import revxrsal.commands.velocity.actor.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission
import java.time.LocalDate
import java.time.ZoneId

/**
 * Befehlsklasse für /stafftop [day|week|month|all|own]
 */
class StaffTopCommand(
    private val historyService: LiteBansHistoryService,
    private val configService: ConfigService,
    private val luckPermsService: LuckPermsService = LuckPermsService()
) {

    @Command("stafftop")
    fun onStaffTop(
        actor: VelocityCommandActor,
        @Optional @SuggestWith(StaffTopPeriodSuggestionProvider::class) period: String?
    ) {
        if (period?.lowercase() == "own") {
            if (!actor.source().hasPermission("bamelitebans.command.stafftop.own") &&
                !actor.source().hasPermission("bamelitebans.command.stafftop")) {
                actor.reply(ColorParser.parse("<red>Dazu hast du keine Berechtigung."))
                return
            }
            handleOwn(actor)
            return
        }

        if (!actor.source().hasPermission("bamelitebans.command.stafftop")) {
            if (actor.source().hasPermission("bamelitebans.command.stafftop.own")) {
                if (period == null) {
                    handleOwn(actor)
                } else {
                    actor.reply(ColorParser.parse("<red>Du hast nur die Berechtigung für <yellow>/stafftop own<red>."))
                }
                return
            }
            actor.reply(ColorParser.parse("<red>Dazu hast du keine Berechtigung."))
            return
        }

        val (periodSmallCaps, sinceMillis) = when (period?.lowercase()) {
            "day", "today", "heute", "tag", "1d" -> "ᴛᴏᴅᴀʏ" to LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            "monat", "month", "30d" -> "ʟᴀsᴛ ᴍᴏɴᴛʜ" to (System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
            "all", "gesamt", "alltime" -> "ᴀʟʟ ᴛɪᴍᴇ" to 0L
            else -> "ʟᴀsᴛ ᴡᴇᴇᴋ" to (System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000)
        }

        historyService.fetchStaffTop(sinceMillis).thenAccept { entries ->
            if (entries.isEmpty()) {
                actor.reply(ColorParser.parse(configService.stafftopEmpty(periodSmallCaps)))
                return@thenAccept
            }

            actor.reply(ColorParser.parse(""))
            actor.reply(ColorParser.parse(configService.stafftopHeader(periodSmallCaps)))
            actor.reply(ColorParser.parse(""))

            entries.forEachIndexed { index, entry ->
                val rawPrefix = entry.luckPermsPrefix.ifEmpty { "<green>" }
                val prefix = de.bame.bamelitebans.util.SmallCaps.convertPreservingTags(rawPrefix)
                val lineText = "<green>#${index + 1}<white>: $prefix<!bold><!italic>${entry.staffName}<reset><white>: <red>${entry.bans} Bans <gray>| <yellow>${entry.mutes} Mutes <gray>| <green>${entry.warns} Warns"
                actor.reply(ColorParser.parse(lineText))
            }

            actor.reply(ColorParser.parse(""))
        }
    }

    private fun handleOwn(actor: VelocityCommandActor) {
        val playerName = actor.name()
        historyService.fetchStaffTop(0L).thenAccept { entries ->
            val index = entries.indexOfFirst { it.staffName.equals(playerName, ignoreCase = true) }
            val rank: Int
            val prefix: String
            val name: String
            val bans: Int
            val mutes: Int
            val warns: Int

            if (index != -1) {
                val entry = entries[index]
                rank = index + 1
                prefix = de.bame.bamelitebans.util.SmallCaps.convertPreservingTags(entry.luckPermsPrefix.ifEmpty { "<green>" })
                name = entry.staffName
                bans = entry.bans
                mutes = entry.mutes
                warns = entry.warns
            } else {
                rank = entries.size + 1
                prefix = try {
                    de.bame.bamelitebans.util.SmallCaps.convertPreservingTags(luckPermsService.getPrefix(actor.uniqueId().toString()).ifEmpty { "<green>" })
                } catch (_: Exception) {
                    "<green>"
                }
                name = playerName
                bans = 0
                mutes = 0
                warns = 0
            }

            actor.reply(ColorParser.parse(""))
            actor.reply(ColorParser.parse("<gold>🏆 <green>Staff-Leaderboard (own):"))
            actor.reply(ColorParser.parse(""))
            val lineText = "<green>#$rank<white>: $prefix<!bold><!italic>$name<reset><white>: <red>$bans Bans <gray>| <yellow>$mutes Mutes <gray>| <green>$warns Warns"
            actor.reply(ColorParser.parse(lineText))
            actor.reply(ColorParser.parse(""))
        }
    }
}
