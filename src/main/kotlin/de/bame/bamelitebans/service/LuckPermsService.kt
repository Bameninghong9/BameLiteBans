package de.bame.bamelitebans.service

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import java.util.UUID

/**
 * Service für die Integration von LuckPerms (z.B. Rang-Prefixe abrufen).
 */
class LuckPermsService {

    private var luckPerms: LuckPerms? = null

    init {
        try {
            luckPerms = LuckPermsProvider.get()
        } catch (e: Exception) {
            // LuckPerms ist evtl. nicht installiert oder noch nicht geladen
        }
    }

    fun getPrefix(uuidStr: String?): String = runCatching {
        if (uuidStr.isNullOrBlank()) return ""
        val lp = luckPerms ?: LuckPermsProvider.get().also { luckPerms = it }
        val uuid = UUID.fromString(uuidStr)
        val user = lp.userManager.getUser(uuid) ?: lp.userManager.loadUser(uuid).join()
        user?.cachedData?.metaData?.prefix ?: ""
    }.getOrDefault("")
}
