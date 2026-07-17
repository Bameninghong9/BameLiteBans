package de.bame.bamelitebans.service

import com.velocitypowered.api.proxy.ProxyServer
import de.bame.bamelitebans.model.LastSeenEntry
import litebans.api.Database
import org.slf4j.Logger
import java.sql.SQLException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

/**
 * Service zum Verwalten, Speichern und Abfragen von Zuletzt-Online (LastSeen) Daten.
 */
class LastSeenService(
    private val proxy: ProxyServer,
    private val historyService: LiteBansHistoryService,
    private val logger: Logger,
    private val executor: Executor = ForkJoinPool.commonPool()
) {

    private var cachedTableName: String? = null
    private val debounceMap = ConcurrentHashMap<String, Long>()
    private val DEBOUNCE_INTERVAL_MS = 60_000L // 60 seconds

    fun getTableName(): String {
        cachedTableName?.let { return it }
        val prefix = try {
            Database.get().prepareStatement("SELECT 1 FROM {bans} WHERE 1=0").use { st ->
                val meta = st.metaData
                val fullBansName = if (meta != null && meta.columnCount > 0) meta.getTableName(1) else ""
                if (fullBansName.endsWith("bans")) {
                    fullBansName.substring(0, fullBansName.length - 4)
                } else ""
            }
        } catch (_: Exception) {
            ""
        }
        val name = if (prefix.isNotBlank()) "${prefix}bamelitebans_lastseen" else "bamelitebans_lastseen"
        cachedTableName = name
        return name
    }

    fun initTable() {
        val table = getTableName()
        val query = """
            CREATE TABLE IF NOT EXISTS $table (
                uuid VARCHAR(36) PRIMARY KEY,
                name VARCHAR(64) NOT NULL,
                last_seen BIGINT NOT NULL,
                server VARCHAR(64) NOT NULL
            )
        """.trimIndent()
        try {
            Database.get().prepareStatement(query).use { st ->
                st.executeUpdate()
            }
        } catch (e: SQLException) {
            logger.error("Fehler beim Erstellen der Tabelle $table", e)
        }
    }

    fun recordLastSeen(uuid: String, name: String, server: String?) {
        val now = System.currentTimeMillis()
        val lastUpdate = debounceMap[uuid]
        if (lastUpdate != null && (now - lastUpdate) < DEBOUNCE_INTERVAL_MS) {
            return
        }
        debounceMap[uuid] = now

        executor.execute {
            val table = getTableName()
            val serverName = server ?: "Netzwerk"
            val query = """
                INSERT INTO $table (uuid, name, last_seen, server) VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE name = VALUES(name), last_seen = VALUES(last_seen), server = VALUES(server)
            """.trimIndent()

            try {
                Database.get().prepareStatement(query).use { st ->
                    st.setString(1, uuid)
                    st.setString(2, name)
                    st.setLong(3, now)
                    st.setString(4, serverName)
                    st.executeUpdate()
                }
            } catch (e: SQLException) {
                // Fallback für SQL-Dialekte ohne ON DUPLICATE KEY UPDATE (z.B. SQLite/PostgreSQL)
                val fallbackUpdate = if (server != null) {
                    "UPDATE $table SET name=?, last_seen=?, server=? WHERE uuid=?"
                } else {
                    "UPDATE $table SET name=?, last_seen=? WHERE uuid=?"
                }
                try {
                    val updated = Database.get().prepareStatement(fallbackUpdate).use { st ->
                        st.setString(1, name)
                        st.setLong(2, now)
                        if (server != null) {
                            st.setString(3, server)
                            st.setString(4, uuid)
                        } else {
                            st.setString(3, uuid)
                        }
                        st.executeUpdate()
                    }
                    if (updated == 0) {
                        val fallbackInsert = "INSERT INTO $table (uuid, name, last_seen, server) VALUES (?, ?, ?, ?)"
                        Database.get().prepareStatement(fallbackInsert).use { st ->
                            st.setString(1, uuid)
                            st.setString(2, name)
                            st.setLong(3, now)
                            st.setString(4, serverName)
                            st.executeUpdate()
                        }
                    }
                } catch (fallbackEx: SQLException) {
                    logger.error("Fehler beim atomaren und Fallback-Upsert in $table für $name ($uuid)", fallbackEx)
                }
            }
        }
    }

    fun fetchLastSeen(playerOrUuid: String): CompletableFuture<LastSeenEntry?> {
        return CompletableFuture.supplyAsync({
            // 1. Prüfen, ob der Spieler aktuell auf Velocity online ist
            val onlinePlayer = proxy.getPlayer(playerOrUuid)
            if (onlinePlayer.isPresent) {
                val p = onlinePlayer.get()
                val serverName = p.currentServer.map { it.serverInfo.name }.orElse("Netzwerk")
                return@supplyAsync LastSeenEntry(
                    uuid = p.uniqueId.toString(),
                    name = p.username,
                    timestampMillis = System.currentTimeMillis(),
                    server = serverName,
                    isOnline = true
                )
            }

            // 2. Abfrage in unserer eigenen bamelitebans_lastseen Tabelle
            val table = getTableName()
            val query = "SELECT * FROM $table WHERE LOWER(name)=LOWER(?) OR uuid=? ORDER BY last_seen DESC LIMIT 1"
            try {
                Database.get().prepareStatement(query).use { st ->
                    st.setString(1, playerOrUuid)
                    st.setString(2, playerOrUuid)
                    st.executeQuery().use { rs ->
                        if (rs.next()) {
                            return@supplyAsync LastSeenEntry(
                                uuid = rs.getString("uuid"),
                                name = rs.getString("name"),
                                timestampMillis = rs.getLong("last_seen"),
                                server = rs.getString("server")
                            )
                        }
                    }
                }
            } catch (e: SQLException) {
                logger.error("Fehler bei Abfrage von $table für $playerOrUuid", e)
            }

            // 3. Fallback: Abfrage der LiteBans {history} Tabelle (für ältere Spieler)
            val uuid = historyService.resolvePlayerUuid(playerOrUuid) ?: return@supplyAsync null
            val resolvedName = historyService.resolvePlayerName(uuid)

            val historyQuery = "SELECT * FROM {history} WHERE uuid=? ORDER BY date DESC LIMIT 1"
            try {
                Database.get().prepareStatement(historyQuery).use { st ->
                    st.setString(1, uuid)
                    st.executeQuery().use { rs ->
                        if (rs.next()) {
                            val timeMillis = rs.getTimestamp("date")?.time ?: System.currentTimeMillis()
                            val serverCol = try { rs.getString("server") } catch (e: Exception) { null }
                            val serverName = if (!serverCol.isNullOrBlank()) serverCol else "Netzwerk"
                            return@supplyAsync LastSeenEntry(
                                uuid = uuid,
                                name = resolvedName,
                                timestampMillis = timeMillis,
                                server = serverName
                            )
                        }
                    }
                }
            } catch (e: SQLException) {
                logger.error("Fehler bei Fallback-Abfrage aus {history} für $playerOrUuid", e)
            }

            null
        }, executor)
    }
}
