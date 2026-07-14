package de.bame.bamelitebans.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * Universeller ColorParser, der sowohl MiniMessage-Tags (<gold>, <b>, <green>, <#RRGGBB>, <gradient:...>)
 * als auch klassische Farbcodes (&c, &7 etc.) und Hex-Codes (&#RRGGBB) korrekt in Adventure Components umwandelt.
 */
object ColorParser {
    private val HEX_PATTERN = Regex("[&§]#([0-9a-fA-F]{6})")
    private val BUNGEE_HEX_PATTERN = Regex("[&§]x[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])[&§]([0-9a-fA-F])")
    private val LEGACY_MAP = mapOf(
        '0' to "<black>",
        '1' to "<dark_blue>",
        '2' to "<dark_green>",
        '3' to "<dark_aqua>",
        '4' to "<dark_red>",
        '5' to "<dark_purple>",
        '6' to "<gold>",
        '7' to "<gray>",
        '8' to "<dark_gray>",
        '9' to "<blue>",
        'a' to "<green>",
        'b' to "<aqua>",
        'c' to "<red>",
        'd' to "<light_purple>",
        'e' to "<yellow>",
        'f' to "<white>",
        'k' to "<obfuscated>",
        'l' to "<bold>",
        'm' to "<strikethrough>",
        'n' to "<underlined>",
        'o' to "<italic>",
        'r' to "<reset>"
    )

    fun escape(input: String?): String {
        if (input == null) return ""
        return MiniMessage.miniMessage().escapeTags(input)
    }

    fun parse(text: String): Component {
        // 1. Konvertiere &#RRGGBB oder §#RRGGBB in MiniMessage <#RRGGBB>
        var converted = HEX_PATTERN.replace(text) { match ->
            "<#${match.groupValues[1]}>"
        }
        converted = BUNGEE_HEX_PATTERN.replace(converted) { match ->
            "<#${match.groupValues[1]}${match.groupValues[2]}${match.groupValues[3]}${match.groupValues[4]}${match.groupValues[5]}${match.groupValues[6]}>"
        }

        // 2. Konvertiere Legacy &c / §c in MiniMessage Tags
        val sb = StringBuilder()
        var i = 0
        while (i < converted.length) {
            val c = converted[i]
            if ((c == '&' || c == '§') && i + 1 < converted.length) {
                val next = converted[i + 1].lowercaseChar()
                val miniTag = LEGACY_MAP[next]
                if (miniTag != null) {
                    sb.append(miniTag)
                    i += 2
                    continue
                }
            }
            sb.append(c)
            i++
        }

        val miniString = sb.toString()

        // 3. Versuche MiniMessage Deserialization
        return try {
            MiniMessage.miniMessage().deserialize(miniString)
        } catch (e: Exception) {
            // Fallback auf klassische Legacy-Deserialisierung
            var processed = HEX_PATTERN.replace(text) { match ->
                val hex = match.groupValues[1]
                "§x§${hex[0]}§${hex[1]}§${hex[2]}§${hex[3]}§${hex[4]}§${hex[5]}"
            }
            processed = processed.replace(Regex("&([0-9a-fk-orA-FK-OR])")) { match ->
                "§${match.groupValues[1]}"
            }
            LegacyComponentSerializer.legacySection().deserialize(processed)
        }
    }

    fun stripColors(text: String?): String {
        if (text.isNullOrBlank()) return ""
        var clean = try {
            MiniMessage.miniMessage().stripTags(text)
        } catch (_: Exception) {
            text
        }
        clean = clean.replace(Regex("[&§]x([&§][0-9a-fA-F]){6}"), "")
        clean = clean.replace(Regex("[&§]#[0-9a-fA-F]{6}"), "")
        clean = clean.replace(Regex("<[^>]+>"), "")
        clean = clean.replace(Regex("[&§][0-9a-fk-orA-FK-OR]"), "")
        return clean.trim()
    }
}
