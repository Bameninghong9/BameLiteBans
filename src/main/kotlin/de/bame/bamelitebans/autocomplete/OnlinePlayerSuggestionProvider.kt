package de.bame.bamelitebans.autocomplete

import de.bame.bamelitebans.BameLiteBansPlugin
import revxrsal.commands.autocomplete.SuggestionProvider
import revxrsal.commands.node.ExecutionContext
import revxrsal.commands.velocity.actor.VelocityCommandActor

/**
 * Liefert Tab-Completion für alle aktuell auf dem Velocity Proxy verbundenen Spieler.
 */
class OnlinePlayerSuggestionProvider : SuggestionProvider<VelocityCommandActor> {
    override fun getSuggestions(context: ExecutionContext<VelocityCommandActor>): Collection<String> {
        val proxy = BameLiteBansPlugin.instance?.proxy ?: return emptyList()
        val allNames = proxy.allPlayers.map { it.username }
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
            return allNames
        }
        return allNames.filter { it.startsWith(prefix, ignoreCase = true) }
    }
}
