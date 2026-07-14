package de.bame.bamelitebans.model

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

/**
 * Repräsentiert die Art der Strafe in LiteBans.
 */
enum class PunishmentType(
    val displayName: String,
    val color: TextColor
) {
    BAN("ᴡᴜʀᴅᴇ ɢᴇʙᴀɴɴᴛ", NamedTextColor.RED),
    MUTE("ᴡᴜʀᴅᴇ ɢᴇᴍᴜᴛᴇᴛ", NamedTextColor.GOLD),
    WARN("ᴡᴜʀᴅᴇ ɢᴇᴡᴀʀɴᴛ", NamedTextColor.YELLOW),
    KICK("ᴡᴜʀᴅᴇ ɢᴇᴋɪᴄᴋᴛ", NamedTextColor.LIGHT_PURPLE);
}
