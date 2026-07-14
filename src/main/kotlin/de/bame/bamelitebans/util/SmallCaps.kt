package de.bame.bamelitebans.util

/**
 * Hilfsklasse zur Konvertierung von Text in Small Caps (Kapitälchen).
 */
object SmallCaps {
    private val SMALL_CAPS_MAP = mapOf(
        'a' to 'ᴀ',
        'b' to 'ʙ',
        'c' to 'ᴄ',
        'd' to 'ᴅ',
        'e' to 'ᴇ',
        'f' to 'ꜰ',
        'g' to 'ɢ',
        'h' to 'ʜ',
        'i' to 'ɪ',
        'j' to 'ᴊ',
        'k' to 'ᴋ',
        'l' to 'ʟ',
        'm' to 'ᴍ',
        'n' to 'ɴ',
        'o' to 'ᴏ',
        'p' to 'ᴘ',
        'q' to 'ǫ',
        'r' to 'ʀ',
        's' to 's',
        't' to 'ᴛ',
        'u' to 'ᴜ',
        'v' to 'ᴠ',
        'w' to 'ᴡ',
        'x' to 'x',
        'y' to 'ʏ',
        'z' to 'ᴢ'
    )

    fun convert(text: String): String {
        return text.map { ch ->
            SMALL_CAPS_MAP[ch.lowercaseChar()] ?: ch
        }.joinToString("")
    }

    fun convertPreservingTags(text: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '<') {
                val closeIdx = text.indexOf('>', i)
                if (closeIdx != -1) {
                    sb.append(text.substring(i, closeIdx + 1))
                    i = closeIdx + 1
                    continue
                }
            } else if (c == '&' || c == '§') {
                if (i + 1 < text.length) {
                    val next = text[i + 1]
                    if (next == '#') {
                        if (i + 8 <= text.length) {
                            sb.append(text.substring(i, i + 8))
                            i += 8
                            continue
                        }
                    } else if (next == 'x' || next == 'X') {
                        if (i + 14 <= text.length) {
                            sb.append(text.substring(i, i + 14))
                            i += 14
                            continue
                        }
                    } else {
                        sb.append(c).append(next)
                        i += 2
                        continue
                    }
                }
            }
            sb.append(SMALL_CAPS_MAP[c.lowercaseChar()] ?: c)
            i++
        }
        return sb.toString()
    }
}
