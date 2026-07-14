package de.bame.bamelitebans.command

import de.bame.bamelitebans.config.ConfigService
import de.bame.bamelitebans.util.ColorParser
import revxrsal.commands.annotation.Command
import revxrsal.commands.velocity.actor.VelocityCommandActor
import revxrsal.commands.velocity.annotation.CommandPermission

/**
 * Befehlsklasse für /bamelitebans reload
 */
class ReloadCommand(
    private val configService: ConfigService
) {

    @Command("bamelitebans reload")
    @CommandPermission("bamelitebans.command.configreload")
    fun onReload(actor: VelocityCommandActor) {
        configService.loadConfig()
        actor.reply(ColorParser.parse(""))
        actor.reply(ColorParser.parse(configService.reloadHeader))
        actor.reply(ColorParser.parse(configService.reloadSuccess))
        actor.reply(ColorParser.parse(""))
    }
}
