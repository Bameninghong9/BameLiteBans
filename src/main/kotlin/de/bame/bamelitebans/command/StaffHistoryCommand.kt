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

        val (actualReason, displayLimit) = parseReasonAndLimit(reason, 100)
        val keywords = configService.getSearchKeywords(actualReason)
        historyService.fetchStaffHistory(staffName, keywords).thenAccept { entries ->
            if (entries.isEmpty()) {
                if (actualReason.isNullOrBlank()) {
                    actor.reply(ColorParser.parse("<red>Keine Strafen von <#92F254>$staffName <red>gefunden."))
                } else {
                    actor.reply(ColorParser.parse("<red>Keine Strafen von <#92F254>$staffName <red>mit Grund '<yellow>$actualReason<red>' gefunden."))
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

    private fun parseReasonAndLimit(rawReason: String?, defaultLimit: Int = 100): Pair<String?, Int> {
        if (rawReason.isNullOrBlank()) return null to defaultLimit
        val trimmed = rawReason.trim()
        val pureNumber = trimmed.toIntOrNull()
        if (pureNumber != null && pureNumber > 0) {
            return null to pureNumber
        }
        val lastSpace = trimmed.lastIndexOf(' ')
        if (lastSpace != -1) {
            val possibleNumber = trimmed.substring(lastSpace + 1).toIntOrNull()
            if (possibleNumber != null && possibleNumber >= 5) {
                val actualReason = trimmed.substring(0, lastSpace).trim()
                return (if (actualReason.isEmpty()) null else actualReason) to possibleNumber
            }
        }
        return trimmed to defaultLimit
    }
}
