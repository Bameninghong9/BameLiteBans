package de.bame.bamelitebans.autocomplete

import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.velocity.actor.VelocityCommandActor

class StaffTopPeriodSuggestionProvider : SuggestionProvider<VelocityCommandActor> {
    override fun getSuggestions(context: ExecutionContext<VelocityCommandActor>): Collection<String> {
        val actor = context.actor()
        val hasGeneral = actor.source().hasPermission("bamelitebans.command.stafftop")
        val hasOwn = actor.source().hasPermission("bamelitebans.command.stafftop.own")

        val suggestions = mutableListOf<String>()
        if (hasGeneral) {
            suggestions.addAll(listOf("day", "week", "month", "all"))
        }
        if (hasGeneral || hasOwn) {
            suggestions.add("own")
        }
        return suggestions
    }
}
