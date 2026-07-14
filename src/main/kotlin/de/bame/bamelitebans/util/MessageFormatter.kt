package de.bame.bamelitebans.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * Hilfsklasse für einheitliche Benachrichtigungen im Plugin.
 */
object MessageFormatter {

    val PREFIX: Component = Component.text()
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("LiteBans", NamedTextColor.RED))
        .append(Component.text("] ", NamedTextColor.DARK_GRAY))
        .build()

    fun info(message: String): Component {
        return PREFIX.append(Component.text(message, NamedTextColor.GRAY))
    }

    fun success(message: String): Component {
        return PREFIX.append(Component.text(message, NamedTextColor.GREEN))
    }

    fun error(message: String): Component {
        return PREFIX.append(Component.text(message, NamedTextColor.RED))
    }
}
