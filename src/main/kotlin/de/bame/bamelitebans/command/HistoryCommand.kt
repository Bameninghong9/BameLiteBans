package de.bame.bamelitebans.command

import de.bame.bamelitebans.autocomplete.OnlinePlayerSuggestionProvider
import de.bame.bamelitebans.config.ConfigService
import de.bame.bamelitebans.service.LiteBansHistoryService
import de.bame.bamelitebans.util.ColorParser
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Optional
import revxrsal.commands.annotation.SuggestWith
import revxrsal.commands.velocity.actor.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission

/**
 * Befehlsklasse für /searchhistory <player> [reason]
 */
class HistoryCommand(
    private val historyService: LiteBansHistoryService,
    private val configService: ConfigService
) {

    @Command("searchhistory", "searchhist")
    @CommandPermission("bamelitebans.command.searchhistory")
    fun onSearchHistory(
        actor: VelocityCommandActor,
        @Optional @SuggestWith(OnlinePlayerSuggestionProvider::class) playerName: String?,
        @Optional reason: String?
    ) {
        if (playerName.isNullOrBlank()) {
            de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Verwendung: /searchhistory <player> [grund]")
            return
        }

        val (actualReason, displayLimit) = de.bame.bamelitebans.util.CommandUtil.parseReasonAndLimit(reason, 100)
        val keywords = configService.getSearchKeywords(actualReason)
        historyService.fetchHistory(playerName, keywords).thenAccept { entries ->
            if (entries.isEmpty()) {
                if (actualReason.isNullOrBlank()) {
                    de.bame.bamelitebans.util.CommandUtil.replyError(actor, configService.playerNotFound)
                } else {
                    val safeReason = ColorParser.escape(actualReason)
                    val safePlayer = ColorParser.escape(playerName)
                    de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Keine Einträge mit Grund '<yellow>$safeReason<white>' für <#92F254>$safePlayer <white>gefunden.")
                }
                return@thenAccept
            }

            val headerText = if (!actualReason.isNullOrBlank()) {
                configService.headerSearch(playerName, actualReason, entries.size)
            } else {
                configService.headerAll(playerName, entries.size)
            }
            actor.reply(ColorParser.parse(headerText))

            val displayEntries = entries.take(displayLimit)

            for (entry in displayEntries) {
                actor.reply(entry.toChatMessage(configService))
            }

            if (entries.size > displayLimit) {
                val remaining = entries.size - displayLimit
                val safePlayer = ColorParser.escape(playerName)
                val safeReason = ColorParser.escape(actualReason ?: "")
                actor.reply(ColorParser.parse("<gray>... und <#FFFE00>$remaining <gray>weitere ältere Einträge ausgeblendet (Limit: $displayLimit). Nutze z.B. <yellow>/searchhist $safePlayer $safeReason ${entries.size} <gray>um alle anzuzeigen."))
            }
        }
    }
}

