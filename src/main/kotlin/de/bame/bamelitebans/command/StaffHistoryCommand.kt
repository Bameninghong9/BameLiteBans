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
            de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Verwendung: /searchstaffhistory <teamler> [grund]")
            return
        }

        val (actualReason, displayLimit) = de.bame.bamelitebans.util.CommandUtil.parseReasonAndLimit(reason, 100)
        val keywords = configService.getSearchKeywords(actualReason)
        historyService.fetchStaffHistory(staffName, keywords).thenAccept { entries ->
            if (entries.isEmpty()) {
                if (actualReason.isNullOrBlank()) {
                    de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Keine Strafen von <#92F254>$staffName <white>gefunden.")
                } else {
                    de.bame.bamelitebans.util.CommandUtil.replyError(actor, "Keine Strafen von <#92F254>$staffName <white>mit Grund '<yellow>$actualReason<white>' gefunden.")
                }
                return@thenAccept
            }

            val headerText = if (!actualReason.isNullOrBlank()) {
                configService.staffHeaderSearch(staffName, actualReason, entries.size)
            } else {
                configService.staffHeaderAll(staffName, entries.size)
            }
            actor.reply(ColorParser.parse(headerText))

            val displayEntries = entries.take(displayLimit)

            for (entry in displayEntries) {
                actor.reply(entry.toChatMessage(configService))
            }

            if (entries.size > displayLimit) {
                val remaining = entries.size - displayLimit
                actor.reply(ColorParser.parse("<gray>... und <#FFFE00>$remaining <gray>weitere ältere Einträge ausgeblendet (Limit: $displayLimit). Nutze z.B. <yellow>/searchstaffhist $staffName $actualReason ${entries.size} <gray>um alle anzuzeigen."))
            }
        }
    }
}

