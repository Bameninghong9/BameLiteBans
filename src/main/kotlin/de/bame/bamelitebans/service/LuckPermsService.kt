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

    fun getPrefixAsync(uuidStr: String?): java.util.concurrent.CompletableFuture<String> {
        if (uuidStr.isNullOrBlank()) return java.util.concurrent.CompletableFuture.completedFuture("")
        val now = System.currentTimeMillis()
        val cached = prefixCache[uuidStr]
        if (cached != null && (now - cached.second) < CACHE_TTL_MS) {
            return java.util.concurrent.CompletableFuture.completedFuture(cached.first)
        }
        return try {
            val lp = luckPerms ?: LuckPermsProvider.get().also { luckPerms = it }
            val uuid = UUID.fromString(uuidStr)
            val existingUser = lp.userManager.getUser(uuid)
            if (existingUser != null) {
                val prefix = existingUser.cachedData.metaData.prefix ?: ""
                prefixCache[uuidStr] = prefix to now
                java.util.concurrent.CompletableFuture.completedFuture(prefix)
            } else {
                lp.userManager.loadUser(uuid).thenApply { user ->
                    val prefix = user?.cachedData?.metaData?.prefix ?: ""
                    prefixCache[uuidStr] = prefix to now
                    prefix
                }.exceptionally { "" }
            }
        } catch (_: Exception) {
            java.util.concurrent.CompletableFuture.completedFuture("")
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
        val user = lp.userManager.getUser(uuid) ?: try {
            lp.userManager.loadUser(uuid).get(3, java.util.concurrent.TimeUnit.SECONDS)
        } catch (_: Exception) {
            null
        }
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
