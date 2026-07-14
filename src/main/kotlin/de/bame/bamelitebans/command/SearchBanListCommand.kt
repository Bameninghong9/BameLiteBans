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
        val (actualReason, pageNum) = CommandUtil.parseReasonAndNumber(rawInput, 1)
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

            val safeReasonCmd = if (actualReason != null) {
                ColorParser.escape(actualReason).replace("'", "\\'")
            } else null

            val prevCmd = if (safeReasonCmd != null) "/searchbanlist $safeReasonCmd ${page - 1}" else "/searchbanlist ${page - 1}"
            val nextCmd = if (safeReasonCmd != null) "/searchbanlist $safeReasonCmd ${page + 1}" else "/searchbanlist ${page + 1}"

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
        }.exceptionally { e ->
            org.slf4j.LoggerFactory.getLogger(SearchBanListCommand::class.java).error("Fehler bei /searchbanlist", e)
            CommandUtil.replyError(actor, "Ein Fehler ist aufgetreten.")
            null
        }
    }
}
