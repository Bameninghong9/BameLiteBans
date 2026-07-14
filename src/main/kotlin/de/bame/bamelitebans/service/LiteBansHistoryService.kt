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
        private val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
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
    fun fetchHistory(playerName: String, keyword: String?): CompletableFuture<List<PunishmentEntry>> {
        val keywords = if (keyword.isNullOrBlank()) emptyList() else listOf(keyword)
        return fetchHistory(playerName, keywords)
    }

    fun fetchHistory(playerName: String, keywords: List<String>): CompletableFuture<List<PunishmentEntry>> {
        return CompletableFuture.supplyAsync({
            val uuid = resolvePlayerUuid(playerName) ?: return@supplyAsync emptyList()

            val entries = mutableListOf<PunishmentEntry>()

            // Alle vier Strafentypen abfragen
            entries.addAll(queryTable(uuid, playerName, "{bans}", PunishmentType.BAN))
            entries.addAll(queryTable(uuid, playerName, "{mutes}", PunishmentType.MUTE))
            entries.addAll(queryTable(uuid, playerName, "{warnings}", PunishmentType.WARN))
            entries.addAll(queryTable(uuid, playerName, "{kicks}", PunishmentType.KICK))

            // Nach Keywords filtern (falls angegeben)
            val filtered = if (keywords.isNotEmpty()) {
                entries.filter { entry ->
                    keywords.any { kw -> entry.reason.contains(kw, ignoreCase = true) }
                }
            } else {
                entries
            }

            // Chronologisch absteigend sortieren und Duplikate ausschließen
            filtered
                .distinctBy { "${it.type}_${it.id}" }
                .sortedWith(compareByDescending<PunishmentEntry> { it.timestampMillis }.thenByDescending { it.id })
        }, executor)
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
                        val id = rs.getLong("id")
                        val rawReason = safeGetString(rs, "reason") ?: "Kein Grund angegeben"
                        val staffName = safeGetString(rs, "banned_by_name") ?: "Konsole"
                        val time = safeGetTimeMillis(rs, "time")
                        val until = safeGetTimeMillis(rs, "until")
                        val active = try { rs.getBoolean("active") } catch (e: Exception) { false }

                        val removedByName = safeGetString(rs, "removed_by_name")
                        val removedByReason = safeGetString(rs, "removed_by_reason")
                        val removedByDateMillis = safeGetLong(rs, "removed_by_date")

                        list.add(
                            PunishmentEntry(
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
                        )
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

    fun fetchStaffHistory(staffNameOrUuid: String, keyword: String?): CompletableFuture<List<PunishmentEntry>> {
        val keywords = if (keyword.isNullOrBlank()) emptyList() else listOf(keyword)
        return fetchStaffHistory(staffNameOrUuid, keywords)
    }

    fun fetchStaffHistory(staffNameOrUuid: String, keywords: List<String>): CompletableFuture<List<PunishmentEntry>> {
        return CompletableFuture.supplyAsync({
            val staffUuid = resolvePlayerUuid(staffNameOrUuid) ?: staffNameOrUuid

            val entries = mutableListOf<PunishmentEntry>()
            val nameCache = mutableMapOf<String, String>()

            entries.addAll(queryStaffTable(staffNameOrUuid, staffUuid, "{bans}", PunishmentType.BAN, nameCache))
            entries.addAll(queryStaffTable(staffNameOrUuid, staffUuid, "{mutes}", PunishmentType.MUTE, nameCache))
            entries.addAll(queryStaffTable(staffNameOrUuid, staffUuid, "{warnings}", PunishmentType.WARN, nameCache))
            entries.addAll(queryStaffTable(staffNameOrUuid, staffUuid, "{kicks}", PunishmentType.KICK, nameCache))

            val filtered = if (keywords.isNotEmpty()) {
                entries.filter { entry ->
                    keywords.any { kw -> entry.reason.contains(kw, ignoreCase = true) }
                }
            } else {
                entries
            }

            filtered
                .distinctBy { "${it.type}_${it.id}" }
                .sortedWith(compareByDescending<PunishmentEntry> { it.timestampMillis }.thenByDescending { it.id })
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
        val query = "SELECT * FROM $tableToken WHERE LOWER(banned_by_name)=LOWER(?) OR banned_by_uuid=?"

        try {
            Database.get().prepareStatement(query).use { st ->
                st.setString(1, staffName)
                st.setString(2, staffUuid)
                st.executeQuery().use { rs: ResultSet ->
                    while (rs.next()) {
                        val id = rs.getLong("id")
                        val targetUuid = safeGetString(rs, "uuid") ?: "unbekannt"
                        val targetName = nameCache.computeIfAbsent(targetUuid) { resolvePlayerName(it) }

                        val executorName = safeGetString(rs, "banned_by_name") ?: staffName
                        val rawReason = safeGetString(rs, "reason") ?: ""
                        val time = safeGetTimeMillis(rs, "time")
                        val until = safeGetTimeMillis(rs, "until")
                        val active = try { rs.getBoolean("active") } catch (e: Exception) { false }
                        val removedByName = safeGetString(rs, "removed_by_name")
                        val removedByDateMillis = safeGetLong(rs, "removed_by_date")
                        val removedByReason = safeGetString(rs, "removed_by_reason")

                        list.add(
                            PunishmentEntry(
                                id = id,
                                type = type,
                                targetName = targetName,
                                staffName = executorName,
                                reason = rawReason,
                                timestampMillis = time,
                                untilMillis = until,
                                active = active,
                                removedByName = removedByName,
                                removedByDateMillis = removedByDateMillis,
                                removedByReason = removedByReason
                            )
                        )
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error("Fehler bei DB-Query in queryStaffTable ($tableToken)", e)
        }

        return list
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
                .take(10)

            val prefixFutures = topList.map { entry ->
                CompletableFuture.runAsync({
                    val uuidToUse = entry.staffUuid ?: resolvePlayerUuid(entry.staffName)
                    entry.luckPermsPrefix = luckPermsService.getPrefix(uuidToUse)
                }, executor)
            }
            CompletableFuture.allOf(*prefixFutures.toTypedArray()).join()

            topList
        }, executor)
    }

    private fun queryStaffTopCounts(
        tableToken: String,
        sinceMillis: Long,
        map: MutableMap<String, StaffTopEntry>,
        adder: (StaffTopEntry, Int) -> Unit
    ) {
        val query = "SELECT banned_by_name, banned_by_uuid, COUNT(*) AS cnt FROM $tableToken WHERE time >= ? AND banned_by_name IS NOT NULL GROUP BY banned_by_name, banned_by_uuid"
        try {
            Database.get().prepareStatement(query).use { st ->
                st.setLong(1, sinceMillis)
                st.executeQuery().use { rs ->
                    while (rs.next()) {
                        val staffName = safeGetString(rs, "banned_by_name") ?: continue
                        if (staffName.isBlank()) continue
                        val staffUuid = safeGetString(rs, "banned_by_uuid")
                        val count = rs.getInt("cnt")
                        val entry = map.computeIfAbsent(staffName) { StaffTopEntry(it) }
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
