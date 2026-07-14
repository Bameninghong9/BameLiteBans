package de.bame.bamelitebans.autocomplete

import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.velocity.actor.VelocityCommandActor

class StaffTopPeriodSuggestionProvider : SuggestionProvider<VelocityCommandActor> {
    override fun getSuggestions(context: ExecutionContext<VelocityCommandActor>): Collection<String> {
        val actor = context.actor()
        val src = actor.source()
        val hasGeneral = src.hasPermission("bamelitebans.command.stafftop")

        val suggestions = mutableListOf<String>()
        if (hasGeneral || src.hasPermission("bamelitebans.command.stafftop.day")) {
            suggestions.add("day")
        }
        if (hasGeneral || src.hasPermission("bamelitebans.command.stafftop.week")) {
            suggestions.add("week")
        }
        if (hasGeneral || src.hasPermission("bamelitebans.command.stafftop.month")) {
            suggestions.add("month")
        }
        if (hasGeneral || src.hasPermission("bamelitebans.command.stafftop.all")) {
            suggestions.add("all")
        }
        if (hasGeneral || src.hasPermission("bamelitebans.command.stafftop.own")) {
            suggestions.add("own")
        }

        val source = try {
            context.input().source()
        } catch (_: Exception) {
            ""
        }
        val prefix = if (source.isEmpty() || source.endsWith(" ")) {
            ""
        } else {
            source.substringAfterLast(' ')
        }
        if (prefix.isEmpty()) {
            return suggestions
        }
        return suggestions.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
