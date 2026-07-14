package de.bame.bamelitebans.service

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import java.util.UUID

/**
 * Service für die Integration von LuckPerms (z.B. Rang-Prefixe abrufen).
 */
class LuckPermsService {

    private var luckPerms: LuckPerms? = null
    private val prefixCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
    private val CACHE_TTL_MS = 5 * 60 * 1000L // 5 Minuten Cache

    init {
        try {
            luckPerms = LuckPermsProvider.get()
        } catch (e: Exception) {
            // LuckPerms ist evtl. nicht installiert oder noch nicht geladen
        }
    }

    fun getPrefix(uuidStr: String?): String = runCatching {
        if (uuidStr.isNullOrBlank()) return ""
        val now = System.currentTimeMillis()
        val cached = prefixCache[uuidStr]
        if (cached != null && (now - cached.second) < CACHE_TTL_MS) {
            return cached.first
        }
        val lp = luckPerms ?: LuckPermsProvider.get().also { luckPerms = it }
        val uuid = UUID.fromString(uuidStr)
        val user = lp.userManager.getUser(uuid) ?: lp.userManager.loadUser(uuid).join()
        val prefix = user?.cachedData?.metaData?.prefix ?: ""
        prefixCache[uuidStr] = prefix to now
        prefix
    }.getOrDefault("")

    fun invalidateCache(uuidStr: String?) {
        if (uuidStr != null) prefixCache.remove(uuidStr)
    }

    fun clearCache() {
        prefixCache.clear()
    }
}
