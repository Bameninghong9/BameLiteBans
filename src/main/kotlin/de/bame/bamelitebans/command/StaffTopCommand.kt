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

    private fun hasPerm(actor: VelocityCommandActor, specificPerm: String): Boolean {
        val src = actor.source()
        return src.hasPermission(specificPerm) || src.hasPermission("bamelitebans.command.stafftop")
    }

    @Command("stafftop")
    fun onStaffTop(
        actor: VelocityCommandActor,
        @Optional @SuggestWith(StaffTopPeriodSuggestionProvider::class) period: String?
    ) {
        val normalized = period?.lowercase()

        if (normalized == "own") {
            if (!hasPerm(actor, "bamelitebans.command.stafftop.own")) {
                de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Dazu hast du keine Berechtigung.")
                return
            }
            handleOwn(actor)
            return
        }

        val (periodSmallCaps, sinceMillis, requiredPerm) = when (normalized) {
            "day", "today", "heute", "tag", "1d" -> Triple("ᴛᴏᴅᴀʏ", LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), "bamelitebans.command.stafftop.day")
            "monat", "month", "30d" -> Triple("ʟᴀsᴛ ᴍᴏɴᴛʜ", System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, "bamelitebans.command.stafftop.month")
            "all", "gesamt", "alltime" -> Triple("ᴀʟʟ ᴛɪᴍᴇ", 0L, "bamelitebans.command.stafftop.all")
            null, "week", "woche", "7d" -> Triple("ʟᴀsᴛ ᴡᴇᴇᴋ", System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000, "bamelitebans.command.stafftop.week")
            else -> {
                val safePeriod = ColorParser.escape(period)
                de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Ungültiger Zeitraum '<yellow>$safePeriod<white>'. Erlaubt: <yellow>tag, woche, monat, all, own<white>.")
                return
            }
        }

        if (!hasPerm(actor, requiredPerm)) {
            if (normalized == null && hasPerm(actor, "bamelitebans.command.stafftop.own")) {
                handleOwn(actor)
                return
            }
            de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Dazu hast du keine Berechtigung.")
            return
        }

        historyService.fetchStaffTop(sinceMillis).thenAccept { entries ->
            if (entries.isEmpty()) {
                de.bame.bamelitebans.util.CommandUtil.replyError(actor, configService.stafftopEmpty(periodSmallCaps))
                return@thenAccept
            }

            actor.reply(ColorParser.parse(""))
            actor.reply(ColorParser.parse(configService.stafftopHeader(periodSmallCaps)))
            actor.reply(ColorParser.parse(""))

            entries.take(10).forEachIndexed { index, entry ->
                val rawPrefix = entry.luckPermsPrefix.ifEmpty { "<green>" }
                val prefix = de.bame.bamelitebans.util.SmallCaps.convertPreservingTags(rawPrefix)
                val safeName = ColorParser.escape(entry.staffName)
                val lineText = "<green>#${index + 1}<white>: $prefix<!bold><!italic>$safeName<reset><white>: <red>${entry.bans} Bans <gray>| <yellow>${entry.mutes} Mutes <gray>| <green>${entry.warns} Warns <gray>| <#50BEBE>${entry.kicks} Kicks"
                actor.reply(ColorParser.parse(lineText))
            }

            actor.reply(ColorParser.parse(""))
        }.exceptionally { e ->
            org.slf4j.LoggerFactory.getLogger(StaffTopCommand::class.java).error("Fehler bei /stafftop", e)
            de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Ein Fehler ist aufgetreten.")
            null
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
            val kicks: Int

            if (index != -1) {
                val entry = entries[index]
                rank = index + 1
                prefix = de.bame.bamelitebans.util.SmallCaps.convertPreservingTags(entry.luckPermsPrefix.ifEmpty { "<green>" })
                name = entry.staffName
                bans = entry.bans
                mutes = entry.mutes
                warns = entry.warns
                kicks = entry.kicks
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
                kicks = 0
            }

            actor.reply(ColorParser.parse(""))
            actor.reply(ColorParser.parse(configService.stafftopHeader("ᴏᴡɴ")))
            actor.reply(ColorParser.parse(""))
            val safeName = ColorParser.escape(name)
            val lineText = "<green>#$rank<white>: $prefix<!bold><!italic>$safeName<reset><white>: <red>$bans Bans <gray>| <yellow>$mutes Mutes <gray>| <green>$warns Warns <gray>| <#50BEBE>$kicks Kicks"
            actor.reply(ColorParser.parse(lineText))
            actor.reply(ColorParser.parse(""))
        }.exceptionally { e ->
            org.slf4j.LoggerFactory.getLogger(StaffTopCommand::class.java).error("Fehler bei /stafftop own", e)
            de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Ein Fehler ist aufgetreten.")
            null
        }
    }
}
