package de.bame.bamelitebans.util

import revxrsal.commands.velocity.actor.VelocityCommandActor

object CommandUtil {

    fun replyError(actor: VelocityCommandActor, message: String) {
        val cleanMsg = message.removePrefix("<red>").removePrefix("<white>")
        val smallCapsMsg = SmallCaps.convertPreservingTags(cleanMsg)
        actor.reply(ColorParser.parse(""))
        actor.reply(ColorParser.parse("<white>✘ <b><#fb0000>ᴇʀʀᴏʀ"))
        actor.reply(ColorParser.parse("<white>$smallCapsMsg"))
        actor.reply(ColorParser.parse(""))
    }

    fun parseReasonAndLimit(rawReason: String?, defaultLimit: Int = 100): Pair<String?, Int> {
        if (rawReason.isNullOrBlank()) return null to defaultLimit
        val trimmed = rawReason.trim()
        val pureNumber = trimmed.toIntOrNull()
        if (pureNumber != null && pureNumber > 0) {
            return null to pureNumber
        }
        val lastSpace = trimmed.lastIndexOf(' ')
        if (lastSpace != -1) {
            val possibleNumber = trimmed.substring(lastSpace + 1).toIntOrNull()
            if (possibleNumber != null && possibleNumber > 0) {
                val actualReason = trimmed.substring(0, lastSpace).trim()
                return (if (actualReason.isEmpty()) null else actualReason) to possibleNumber
            }
        }
        return trimmed to defaultLimit
    }
}
