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
 * Befehlsklasse für /searchstaffhistory <name> [reason]
 */
class StaffHistoryCommand(
    private val historyService: LiteBansHistoryService,
    private val configService: ConfigService
) {

    @Command("searchstaffhistory", "searchstaffhist")
    @CommandPermission("bamelitebans.command.searchstaffhistory")
    fun onSearchStaffHistory(
        actor: VelocityCommandActor,
        @Optional @SuggestWith(OnlinePlayerSuggestionProvider::class) staffName: String?,
        @Optional reason: String?
    ) {
        if (staffName.isNullOrBlank()) {
            actor.reply(ColorParser.parse("<red>Verwendung: /searchstaffhistory <teamler> [grund]"))
            return
        }

        val keywords = configService.getSearchKeywords(reason)
        historyService.fetchStaffHistory(staffName, keywords).thenAccept { entries ->
            if (entries.isEmpty()) {
                if (reason.isNullOrBlank()) {
                    actor.reply(ColorParser.parse("<red>Keine Strafen von <#92F254>$staffName <red>gefunden."))
                } else {
                    actor.reply(ColorParser.parse("<red>Keine Strafen von <#92F254>$staffName <red>mit Grund '<yellow>$reason<red>' gefunden."))
                }
                return@thenAccept
            }

            val headerText = if (!reason.isNullOrBlank()) {
                configService.staffHeaderSearch(staffName, reason, entries.size)
            } else {
                configService.staffHeaderAll(staffName, entries.size)
            }
            actor.reply(ColorParser.parse(headerText))

            for (entry in entries) {
                actor.reply(entry.toChatMessage(configService))
            }
        }
    }
}
