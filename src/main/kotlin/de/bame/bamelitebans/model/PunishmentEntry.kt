package de.bame.bamelitebans.model

import de.bame.bamelitebans.util.ColorParser
import net.kyori.adventure.text.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Repräsentiert einen einzelnen Datenbankeintrag aus der LiteBans Historie.
 */
data class PunishmentEntry(
    val id: Long,
    val type: PunishmentType,
    val targetName: String,
    val staffName: String,
    val reason: String,
    val timestampMillis: Long,
    val untilMillis: Long,
    val active: Boolean,
    val removedByName: String? = null,
    val removedByDateMillis: Long? = null,
    val removedByReason: String? = null
) {
    companion object {
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())
    }

    /**
     * Gibt das Erstellungsdatum als formatierten String zurück.
     */
    fun formattedDate(): String {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timestampMillis))
    }

    /**
     * Gibt das Datum der Aufhebung (Entbannung/Entmutung) als formatierten String zurück.
     */
    fun formattedRemovedDate(): String {
        if (removedByDateMillis == null || removedByDateMillis <= 0) return formattedDate()
        return DATE_FORMATTER.format(Instant.ofEpochMilli(removedByDateMillis))
    }

    /**
     * Formatiert Zeiträume auf Deutsch exakt wie in LiteBans (z.B. "7 Tage", "4 Tage , 1 Stunde , 39 Minuten").
     */
    private fun formatGermanDuration(millis: Long): String {
        if (millis <= 0) return "Permanent"
        val days = millis / (1000 * 60 * 60 * 24)
        val hours = (millis / (1000 * 60 * 60)) % 24
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60

        val parts = buildList {
            if (days == 1L) add("1 Tag") else if (days > 1) add("$days Tage")
            if (hours == 1L) add("1 Stunde") else if (hours > 1) add("$hours Stunden")
            if (minutes == 1L) add("1 Minute") else if (minutes > 1) add("$minutes Minuten")
            if (isEmpty() && seconds > 0) add(if (seconds == 1L) "1 Sekunde" else "$seconds Sekunden")
        }

        return if (parts.isEmpty()) "0 Sekunden" else parts.joinToString(" , ")
    }

    /**
     * Gibt die Gesamtdauer der Strafe auf Deutsch zurück (z.B. "7 Tage" oder "Permanent").
     */
    fun formattedDurationSpan(): String {
        if (untilMillis <= 0) return "Permanent"
        val durationMillis = untilMillis - timestampMillis
        if (durationMillis <= 0) return "Permanent"
        return formatGermanDuration(durationMillis)
    }

    /**
     * Formatiert diesen Eintrag exakt im gewünschten LiteBans-Layout ohne führende Leerzeichen.
     */
    fun toChatMessage(configService: de.bame.bamelitebans.config.ConfigService? = null): Component {
        val dateStr = formattedDate()
        val durationStr = formattedDurationSpan()

        val isCurrentlyActive = active && (untilMillis <= 0 || untilMillis > System.currentTimeMillis())
        val isTemporaryActive = active && untilMillis > System.currentTimeMillis()
        val wasUnbannedOrUnmuted = !removedByName.isNullOrBlank()

        val statusTag = when {
            isCurrentlyActive -> configService?.activeTag ?: "<white> [<red>ᴀᴋᴛɪᴠ<white>]"
            wasUnbannedOrUnmuted -> ""
            else -> configService?.expiredTag ?: "<white> [<#828FE7>ᴀʙɢᴇʟᴀᴜꜰᴇɴ<white>]"
        }

        val cleanReason = reason.ifEmpty { "Kein Grund angegeben" }

        val dateColor = when (type) {
            PunishmentType.BAN -> "<red>"
            PunishmentType.MUTE -> "<yellow>"
            PunishmentType.WARN, PunishmentType.KICK -> "<green>"
        }

        val actionText = when (type) {
            PunishmentType.BAN -> "<#92F254>$targetName<gray> ᴡᴜʀᴅᴇ <#FF0000>ɢᴇʙᴀɴɴᴛ<gray> ꜰüʀ <#FF4B81>$durationStr <gray>ᴠᴏɴ <white>$staffName<gray>: '<#A7FFEA>$cleanReason<white>'$statusTag"
            PunishmentType.MUTE -> "<#92F254>$targetName<gray> ᴡᴜʀᴅᴇ <#E9FF00>ɢᴇᴍᴜᴛᴇᴅ<gray> ꜰüʀ <#FF4B81>$durationStr <gray>ᴠᴏɴ <white>$staffName<gray>: '<#A7FFEA>$cleanReason<white>'$statusTag"
            PunishmentType.WARN -> "<#92F254>$targetName<gray> ᴡᴜʀᴅᴇ <#FF7200>ᴠᴇʀᴡᴀʀɴᴛ <gray>ᴠᴏɴ <white>$staffName<gray>: '<#BCFFA7>$cleanReason<white>'"
            PunishmentType.KICK -> "<#92F254>$targetName<gray> ᴡᴜʀᴅᴇ <#FFE681>ɢᴇᴋɪᴄᴋᴛ <gray>ᴠᴏɴ <white>$staffName<gray>: '<#BCFFA7>$cleanReason<white>'"
        }

        val lines = mutableListOf<String>()
        lines.add("$dateColor -- <gray>[<#6586FF>$dateStr<gray>] $dateColor--<white>")
        lines.add(actionText)

        if (isTemporaryActive && (type == PunishmentType.BAN || type == PunishmentType.MUTE)) {
            lines.add("<white>ʟäᴜꜰᴛ ᴀʙ ɪɴ ${formatGermanDuration(untilMillis - System.currentTimeMillis())}.")
        }

        if (wasUnbannedOrUnmuted) {
            val dateRemovedStr = formattedRemovedDate()
            val cleanRemovalReason = removedByReason?.ifEmpty { "Kein Grund angegeben" } ?: "Kein Grund angegeben"
            val action = if (type == PunishmentType.BAN) "<#C06F6A>ᴇɴᴛʙᴀɴɴᴛ" else "<#A89958>ᴇɴᴛᴍᴜᴛᴇᴅ"
            lines.add("")
            lines.add("<#92F254>$targetName<gray> ᴡᴜʀᴅᴇ $action <gray>ᴠᴏɴ <white>$removedByName<gray> ᴀᴍ <#65BAFF>$dateRemovedStr<gray>: '<#D6DFB9>$cleanRemovalReason<white>'")
        }

        return ColorParser.parse(lines.joinToString("\n"))
    }
}
