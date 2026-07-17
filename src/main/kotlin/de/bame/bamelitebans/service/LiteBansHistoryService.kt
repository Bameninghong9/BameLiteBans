package de.bame.bamelitebans.service

import de.bame.bamelitebans.model.PunishmentEntry
import de.bame.bamelitebans.model.PunishmentType
import de.bame.bamelitebans.model.StaffTopEntry
import litebans.api.Database
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.CompletableFuture

/**
 * Service für asynchrone Datenbankzugriffe auf LiteBans Tabellen.
 */
class LiteBansHistoryService(
    private val logger: org.slf4j.Logger,
    private val luckPermsService: LuckPermsService = LuckPermsService(),
    private val executor: java.util.concurrent.Executor = java.util.concurrent.ForkJoinPool.commonPool()
) {

    companion object {
        val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }

    /**
     * Sucht die Spieler-UUID zu einem gegebenen Namen in der LiteBans Historie.
     * Unterstützt auch die direkte Angabe einer UUID.
     */
    fun resolvePlayerUuid(playerOrUuid: String): String? {
        if (playerOrUuid.matches(UUID_PATTERN)) {
            return playerOrUuid
        }

        val query = "SELECT uuid FROM {history} WHERE name=? ORDER BY date DESC LIMIT 1"
        try {
            Database.get().prepareStatement(query).use { st ->
                st.setString(1, playerOrUuid)
                st.executeQuery().use { rs ->
                    if (rs.next()) {
                        return rs.getString("uuid")
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error("Fehler bei DB-Query in resolvePlayerUuid ($playerOrUuid)", e)
        }
        return null
    }

    /**
     * Lädt asynchron die gesamte Strafen-Historie eines Spielers und filtert optional nach einem Suchbegriff im Grund.
     */
    fun fetchHistory(playerName: String, keywords: List<String>): CompletableFuture<List<PunishmentEntry>> {
        return CompletableFuture.supplyAsync({
            val uuid = resolvePlayerUuid(playerName) ?: return@supplyAsync emptyList()

            val entries = mutableListOf<PunishmentEntry>()

            // Alle vier Strafentypen abfragen
            entries.addAll(queryTable(uuid, playerName, "{bans}", PunishmentType.BAN))
            entries.addAll(queryTable(uuid, playerName, "{mutes}", PunishmentType.MUTE))
            entries.addAll(queryTable(uuid, playerName, "{warnings}", PunishmentType.WARN))
            entries.addAll(queryTable(uuid, playerName, "{kicks}", PunishmentType.KICK))

            filterAndSort(entries, keywords)
        }, executor)
    }

    private fun filterAndSort(entries: List<PunishmentEntry>, keywords: List<String>): List<PunishmentEntry> {
        val filtered = if (keywords.isNotEmpty()) {
            entries.filter { entry ->
                keywords.any { kw -> entry.reason.contains(kw, ignoreCase = true) }
            }
        } else {
            entries
        }

        return filtered
            .distinctBy { "${it.type}_${it.id}" }
            .sortedWith(compareByDescending<PunishmentEntry> { it.timestampMillis }.thenByDescending { it.id })
    }

    private fun safeGetString(rs: ResultSet, col: String): String? {
        return try {
            rs.getString(col)
        } catch (e: Exception) {
            null
        }
    }

    private fun safeGetTimeMillis(rs: ResultSet, col: String): Long {
        return try {
            val l = rs.getLong(col)
            if (l > 0L) l else {
                rs.getTimestamp(col)?.time ?: 0L
            }
        } catch (e: Exception) {
            try {
                rs.getTimestamp(col)?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    private fun safeGetLong(rs: ResultSet, col: String): Long? {
        return try {
            val ts = rs.getTimestamp(col)
            ts?.time
        } catch (e: Exception) {
            try {
                val l = rs.getLong(col)
                if (l > 0) l else null
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun mapRowToEntry(
        rs: ResultSet,
        type: PunishmentType,
        targetName: String,
        staffName: String
    ): PunishmentEntry {
        val id = rs.getLong("id")
        val rawReason = safeGetString(rs, "reason") ?: "Kein Grund angegeben"
        val time = safeGetTimeMillis(rs, "time")
        val until = safeGetTimeMillis(rs, "until")
        val active = try { rs.getBoolean("active") } catch (e: Exception) { false }
        val removedByName = safeGetString(rs, "removed_by_name")
        val removedByReason = safeGetString(rs, "removed_by_reason")
        val removedByDateMillis = safeGetLong(rs, "removed_by_date")

        return PunishmentEntry(
            id = id,
            type = type,
            targetName = targetName,
            staffName = staffName,
            reason = rawReason,
            timestampMillis = time,
            untilMillis = until,
            active = active,
            removedByName = removedByName,
            removedByDateMillis = removedByDateMillis,
            removedByReason = removedByReason
        )
    }

    private fun queryTable(
        uuid: String,
        targetName: String,
        tableToken: String,
        type: PunishmentType
    ): List<PunishmentEntry> {
        val list = mutableListOf<PunishmentEntry>()
        val query = "SELECT * FROM $tableToken WHERE uuid=?"

        try {
            Database.get().prepareStatement(query).use { st ->
                st.setString(1, uuid)
                st.executeQuery().use { rs: ResultSet ->
                    while (rs.next()) {
                        val staffName = safeGetString(rs, "banned_by_name") ?: "Konsole"
                        list.add(mapRowToEntry(rs, type, targetName, staffName))
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error("Fehler bei DB-Query in queryTable ($tableToken)", e)
        }

        return list
    }

    fun resolvePlayerName(uuid: String): String {
        val query = "SELECT name FROM {history} WHERE uuid=? ORDER BY date DESC LIMIT 1"
        try {
            Database.get().prepareStatement(query).use { st ->
                st.setString(1, uuid)
                st.executeQuery().use { rs ->
                    if (rs.next()) {
                        return safeGetString(rs, "name") ?: uuid
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Fehler bei DB-Query in resolvePlayerName ($uuid)", e)
        }
        return uuid
    }

    fun fetchStaffHistory(staffNameOrUuid: String, keywords: List<String>): CompletableFuture<List<PunishmentEntry>> {
        return CompletableFuture.supplyAsync({
            val staffName = if (staffNameOrUuid.matches(UUID_PATTERN)) {
                resolvePlayerName(staffNameOrUuid)
            } else {
                staffNameOrUuid
            }
            val staffUuid = resolvePlayerUuid(staffNameOrUuid) ?: staffNameOrUuid

            val entries = mutableListOf<PunishmentEntry>()
            val nameCache = mutableMapOf<String, String>()

            entries.addAll(queryStaffTable(staffName, staffUuid, "{bans}", PunishmentType.BAN, nameCache))
            entries.addAll(queryStaffTable(staffName, staffUuid, "{mutes}", PunishmentType.MUTE, nameCache))
            entries.addAll(queryStaffTable(staffName, staffUuid, "{warnings}", PunishmentType.WARN, nameCache))
            entries.addAll(queryStaffTable(staffName, staffUuid, "{kicks}", PunishmentType.KICK, nameCache))

            filterAndSort(entries, keywords)
        }, executor)
    }

    private fun queryStaffTable(
        staffName: String,
        staffUuid: String,
        tableToken: String,
        type: PunishmentType,
        nameCache: MutableMap<String, String>
    ): List<PunishmentEntry> {
        val list = mutableListOf<PunishmentEntry>()
        val query = "SELECT * FROM $tableToken WHERE LOWER(banned_by_name)=LOWER(?) OR banned_by_uuid=? LIMIT 5000"

        try {
            Database.get().prepareStatement(query).use { st ->
                st.setString(1, staffName)
                st.setString(2, staffUuid)
                st.executeQuery().use { rs: ResultSet ->
                    while (rs.next()) {
                        val targetUuid = safeGetString(rs, "uuid") ?: "unbekannt"
                        val targetName = nameCache.computeIfAbsent(targetUuid) { resolvePlayerName(it) }
                        val executorName = safeGetString(rs, "banned_by_name") ?: staffName
                        list.add(mapRowToEntry(rs, type, targetName, executorName))
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error("Fehler bei DB-Query in queryStaffTable ($tableToken)", e)
        }

        return list
    }

    fun fetchGlobalBans(
        keywords: List<String>,
        page: Int,
        pageSize: Int
    ): CompletableFuture<Triple<List<PunishmentEntry>, Int, Int>> {
        return CompletableFuture.supplyAsync({
            var totalCount = 0
            val conditions = if (keywords.isNotEmpty()) keywords.joinToString(" OR ") { "LOWER(reason) LIKE ?" } else null
            val countQuery = if (conditions != null) "SELECT COUNT(*) AS cnt FROM {bans} WHERE ($conditions)" else "SELECT COUNT(*) AS cnt FROM {bans}"

            try {
                Database.get().prepareStatement(countQuery).use { st ->
                    if (conditions != null) {
                        keywords.forEachIndexed { i, kw ->
                            st.setString(i + 1, "%${kw.lowercase()}%")
                        }
                    }
                    st.executeQuery().use { rs ->
                        if (rs.next()) {
                            totalCount = rs.getInt("cnt")
                        }
                    }
                }
            } catch (e: SQLException) {
                logger.error("Fehler bei DB-Count in fetchGlobalBans", e)
            }

            val totalPages = if (totalCount == 0) 1 else ((totalCount - 1) / pageSize) + 1
            val validPage = page.coerceIn(1, totalPages)
            val offset = (validPage - 1) * pageSize

            val pageEntries = mutableListOf<PunishmentEntry>()
            if (totalCount > 0) {
                val selectQuery = if (conditions != null) {
                    "SELECT * FROM {bans} WHERE ($conditions) ORDER BY time DESC, id DESC LIMIT ? OFFSET ?"
                } else {
                    "SELECT * FROM {bans} ORDER BY time DESC, id DESC LIMIT ? OFFSET ?"
                }
                try {
                    Database.get().prepareStatement(selectQuery).use { st ->
                        var paramIndex = 1
                        if (conditions != null) {
                            keywords.forEach { kw ->
                                st.setString(paramIndex++, "%${kw.lowercase()}%")
                            }
                        }
                        st.setInt(paramIndex++, pageSize)
                        st.setInt(paramIndex, offset)

                        st.executeQuery().use { rs: ResultSet ->
                            while (rs.next()) {
                                val targetUuid = safeGetString(rs, "uuid") ?: "unbekannt"
                                val staffName = safeGetString(rs, "banned_by_name") ?: "Konsole"
                                pageEntries.add(mapRowToEntry(rs, PunishmentType.BAN, targetUuid, staffName))
                            }
                        }
                    }
                } catch (e: SQLException) {
                    logger.error("Fehler bei DB-Select in fetchGlobalBans", e)
                }
            }

            val nameCache = mutableMapOf<String, String>()
            val resolvedPageEntries = pageEntries.map { entry ->
                if (entry.targetName.matches(UUID_PATTERN)) {
                    val resolved = nameCache.computeIfAbsent(entry.targetName) { resolvePlayerName(it) }
                    entry.copy(targetName = resolved)
                } else {
                    entry
                }
            }

            Triple(resolvedPageEntries, validPage, totalPages)
        }, executor)
    }

    fun fetchStaffTop(sinceMillis: Long): CompletableFuture<List<StaffTopEntry>> {
        return CompletableFuture.supplyAsync({
            val map = mutableMapOf<String, StaffTopEntry>()

            queryStaffTopCounts("{bans}", sinceMillis, map) { entry, count -> entry.bans += count }
            queryStaffTopCounts("{mutes}", sinceMillis, map) { entry, count -> entry.mutes += count }
            queryStaffTopCounts("{warnings}", sinceMillis, map) { entry, count -> entry.warns += count }
            queryStaffTopCounts("{kicks}", sinceMillis, map) { entry, count -> entry.kicks += count }

            val topList = map.values
                .filter { it.total > 0 && !it.staffName.equals("Console", true) && !it.staffName.equals("Konsole", true) }
                .sortedByDescending { it.total }

            for (entry in topList.take(25)) {
                val uuidToUse = entry.staffUuid ?: resolvePlayerUuid(entry.staffName)
                if (!uuidToUse.isNullOrBlank()) {
                    val resolvedLatestName = resolvePlayerName(uuidToUse)
                    if (resolvedLatestName != uuidToUse && resolvedLatestName.isNotBlank()) {
                        entry.staffName = resolvedLatestName
                    }
                }
                entry.luckPermsPrefix = luckPermsService.getPrefix(uuidToUse)
            }

            topList
        }, executor)
    }

    private fun queryStaffTopCounts(
        tableToken: String,
        sinceMillis: Long,
        map: MutableMap<String, StaffTopEntry>,
        adder: (StaffTopEntry, Int) -> Unit
    ) {
        val query = "SELECT banned_by_name, banned_by_uuid, COUNT(*) AS cnt, MAX(time) AS max_time FROM $tableToken WHERE time >= ? AND banned_by_name IS NOT NULL GROUP BY banned_by_name, banned_by_uuid"
        try {
            Database.get().prepareStatement(query).use { st ->
                st.setLong(1, sinceMillis)
                st.executeQuery().use { rs ->
                    while (rs.next()) {
                        val staffName = safeGetString(rs, "banned_by_name") ?: continue
                        if (staffName.isBlank()) continue
                        val staffUuid = safeGetString(rs, "banned_by_uuid")
                        val key = if (!staffUuid.isNullOrBlank()) staffUuid.lowercase() else staffName.lowercase()
                        val count = rs.getInt("cnt")
                        val maxTime = safeGetTimeMillis(rs, "max_time")
                        val entry = map.computeIfAbsent(key) { StaffTopEntry(staffName) }
                        if (maxTime >= entry.latestTimeMillis) {
                            entry.latestTimeMillis = maxTime
                            entry.staffName = staffName
                        }
                        if (entry.staffUuid == null && !staffUuid.isNullOrBlank()) {
                            entry.staffUuid = staffUuid
                        }
                        adder(entry, count)
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error("Fehler bei DB-Query in queryStaffTopCounts ($tableToken)", e)
        }
    }
}
