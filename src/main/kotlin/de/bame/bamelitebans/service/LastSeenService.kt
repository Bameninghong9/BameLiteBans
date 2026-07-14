package de.bame.bamelitebans.service

import com.velocitypowered.api.proxy.ProxyServer
import de.bame.bamelitebans.model.LastSeenEntry
import litebans.api.Database
import org.slf4j.Logger
import java.sql.SQLException
import java.util.concurrent.CompletableFuture
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

    fun initTable() {
        executor.execute {
            val query = """
                CREATE TABLE IF NOT EXISTS bamelitebans_lastseen (
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
                logger.error("Fehler beim Erstellen der Tabelle bamelitebans_lastseen", e)
            }
        }
    }

    fun recordLastSeen(uuid: String, name: String, server: String?) {
        executor.execute {
            val now = System.currentTimeMillis()
            val updateQuery = if (server != null) {
                "UPDATE bamelitebans_lastseen SET name=?, last_seen=?, server=? WHERE uuid=?"
            } else {
                "UPDATE bamelitebans_lastseen SET name=?, last_seen=? WHERE uuid=?"
            }

            try {
                val updated = Database.get().prepareStatement(updateQuery).use { st ->
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
                    val fallbackServer = server ?: "Netzwerk"
                    val insertQuery = "INSERT INTO bamelitebans_lastseen (uuid, name, last_seen, server) VALUES (?, ?, ?, ?)"
                    try {
                        Database.get().prepareStatement(insertQuery).use { st ->
                            st.setString(1, uuid)
                            st.setString(2, name)
                            st.setLong(3, now)
                            st.setString(4, fallbackServer)
                            st.executeUpdate()
                        }
                    } catch (_: SQLException) {}
                }
            } catch (e: SQLException) {
                logger.error("Fehler beim Speichern in bamelitebans_lastseen ($name / $server)", e)
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
                    server = serverName
                )
            }

            // 2. Abfrage in unserer eigenen bamelitebans_lastseen Tabelle
            val query = "SELECT * FROM bamelitebans_lastseen WHERE LOWER(name)=LOWER(?) OR uuid=? ORDER BY last_seen DESC LIMIT 1"
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
                logger.error("Fehler bei Abfrage von bamelitebans_lastseen für $playerOrUuid", e)
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
