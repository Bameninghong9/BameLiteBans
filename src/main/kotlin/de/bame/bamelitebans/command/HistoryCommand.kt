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
            actor.reply(ColorParser.parse("<red>Verwendung: /searchhistory <player> [grund]"))
            return
        }

        val keywords = configService.getSearchKeywords(reason)
        historyService.fetchHistory(playerName, keywords).thenAccept { entries ->
            if (entries.isEmpty()) {
                if (reason.isNullOrBlank()) {
                    actor.reply(ColorParser.parse(configService.playerNotFound))
                } else {
                    actor.reply(ColorParser.parse("<red>Keine Einträge mit Grund '<yellow>$reason<red>' für <#92F254>$playerName <red>gefunden."))
                }
                return@thenAccept
            }

            val headerText = if (!reason.isNullOrBlank()) {
                configService.headerSearch(playerName, reason, entries.size)
            } else {
                configService.headerAll(playerName, entries.size)
            }
            actor.reply(ColorParser.parse(headerText))

            for (entry in entries) {
                actor.reply(entry.toChatMessage(configService))
            }
        }
    }
}
