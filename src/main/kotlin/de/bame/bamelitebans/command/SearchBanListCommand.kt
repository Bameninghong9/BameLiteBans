package de.bame.bamelitebans.command

import de.bame.bamelitebans.config.ConfigService
import de.bame.bamelitebans.service.LiteBansHistoryService
import de.bame.bamelitebans.util.ColorParser
import de.bame.bamelitebans.util.CommandUtil
import de.bame.bamelitebans.util.SmallCaps
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.velocity.actor.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission

/**
 * Befehlsklasse für /searchbanlist <grund> [seite]
 */
class SearchBanListCommand(
    private val historyService: LiteBansHistoryService,
    private val configService: ConfigService
) {

    @Command("searchbanlist", "sbanlist", "sbans")
    @CommandPermission("bamelitebans.command.searchbanlist")
    fun onSearchBanList(
        actor: VelocityCommandActor,
        @Optional rawInput: String?
    ) {
        val (actualReason, pageNum) = parseReasonAndPage(rawInput)
        val keywords = configService.getSearchKeywords(actualReason)

        historyService.fetchGlobalBans(keywords, pageNum, 10).thenAccept { (entries, page, totalPages) ->
            if (entries.isEmpty()) {
                if (actualReason.isNullOrBlank()) {
                    CommandUtil.replyError(actor, "Keine Bans gefunden.")
                } else {
                    val safeReason = ColorParser.escape(actualReason)
                    CommandUtil.replyError(actor, "Keine Bans mit Grund '<yellow>$safeReason<white>' gefunden.")
                }
                return@thenAccept
            }

            for (i in 1..40) {
                actor.reply(ColorParser.parse(" "))
            }
            actor.reply(ColorParser.parse(configService.searchBanListHeader(page, totalPages)))

            for (entry in entries) {
                actor.reply(entry.toChatMessage(configService))
            }

            val prevCmd = if (actualReason != null) "/searchbanlist $actualReason ${page - 1}" else "/searchbanlist ${page - 1}"
            val nextCmd = if (actualReason != null) "/searchbanlist $actualReason ${page + 1}" else "/searchbanlist ${page + 1}"

            val prevText = "<click:run_command:'$prevCmd'><hover:show_text:'<green>Zu Seite ${page - 1}'>← <gold>${SmallCaps.convert("eine seite zurück")}</hover></click>"
            val nextText = "<click:run_command:'$nextCmd'><hover:show_text:'<green>Zu Seite ${page + 1}'>${SmallCaps.convert("nächste seite")} <gold>→</hover></click>"

            val footerButtons = when {
                page > 1 && page < totalPages -> "$prevText <dark_gray>| $nextText"
                page > 1 -> prevText
                page < totalPages -> nextText
                else -> "<gray>${SmallCaps.convert("seite $page von $totalPages")}"
            }

            actor.reply(ColorParser.parse("<white>=== $footerButtons <white>==="))
            actor.reply(ColorParser.parse(""))
        }
    }

    private fun parseReasonAndPage(rawInput: String?): Pair<String?, Int> {
        if (rawInput.isNullOrBlank()) return null to 1
        val trimmed = rawInput.trim()
        val pureNumber = trimmed.toIntOrNull()
        if (pureNumber != null && pureNumber > 0) {
            return null to pureNumber
        }
        val lastSpace = trimmed.lastIndexOf(' ')
        if (lastSpace != -1) {
            val possibleNumber = trimmed.substring(lastSpace + 1).toIntOrNull()
            if (possibleNumber != null && possibleNumber > 0) {
                val actualReason = trimmed.substring(0, lastSpace).trim()
                return (if (actualReason.isEmpty()) null else actualReason) to possibleNumber
            }
        }
        return trimmed to 1
    }
}
