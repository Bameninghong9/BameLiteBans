package de.bame.bamelitebans

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import de.bame.bamelitebans.command.*
import de.bame.bamelitebans.config.ConfigService
import de.bame.bamelitebans.service.LastSeenService
import de.bame.bamelitebans.service.LiteBansHistoryService
import de.bame.bamelitebans.service.LuckPermsService
import org.slf4j.Logger
import revxrsal.commands.velocity.VelocityLamp
import revxrsal.commands.velocity.VelocityVisitors
import java.nio.file.Path
import java.util.concurrent.Executors

@Plugin(
    id = "bamelitebans",
    name = "BameLiteBans",
    version = "1.0.0",
    description = "Erweiterte LiteBans History & Staff-History Suche für Velocity mit TOML-Config",
    authors = ["Bame"],
    dependencies = [Dependency(id = "litebans")]
)
class BameLiteBansPlugin @Inject constructor(
    val proxy: ProxyServer,
    private val logger: Logger,
    @DataDirectory val dataDirectory: Path
) {

    private lateinit var historyService: LiteBansHistoryService
    private lateinit var configService: ConfigService
    private lateinit var lastSeenService: LastSeenService
    private val luckPermsService = LuckPermsService()

    private val dbExecutor = Executors.newFixedThreadPool(4) { r ->
        Thread(r, "BameLiteBans-DB").apply { isDaemon = true }
    }

    companion object {
        var instance: BameLiteBansPlugin? = null
    }

    @Subscribe
    @Suppress("UNUSED_PARAMETER")
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        instance = this
        logger.info("""
            
             ____                   _      _ _       ____                   
            |  _ \                 | |    (_) |     |  _ \                  
            | |_) | __ _ _ __ ___  | |     _| |_ ___| |_) | __ _ _ __ ___   
            |  _ < / _` | '_ ` _ \ | |    | | __/ _ \  _ < / _` | '_ ` _ \  
            | |_) | (_| | | | | | || |____| | ||  __/ |_) | (_| | | | | | | 
            |____/ \__,_|_| |_| |_||______|_|\__\___|____/ \__,_|_| |_| |_| 
             v1.0.0 by Bame | High-Performance LiteBans Moderation Suite
        """.trimIndent())
        logger.info("⚡ [BameLiteBans] Initialisiere Datenbank-Pool (4 Threads) & Tabellen-Sync...")

        configService = ConfigService(dataDirectory)
        historyService = LiteBansHistoryService(logger, luckPermsService = luckPermsService, executor = dbExecutor)
        lastSeenService = LastSeenService(proxy, historyService, logger, dbExecutor)

        lastSeenService.initTable()

        val lamp = VelocityLamp.builder(this, proxy).build()

        logger.info("✔ [BameLiteBans] Registriere Commands über Lamp Framework...")
        lamp.register(HistoryCommand(historyService, configService))
        lamp.register(StaffHistoryCommand(historyService, configService))
        lamp.register(StaffTopCommand(historyService, configService))
        lamp.register(ReloadCommand(configService))
        lamp.register(LastSeenCommand(lastSeenService, luckPermsService, configService))
        lamp.register(SearchBanListCommand(historyService, configService))

        lamp.accept(VelocityVisitors.brigadier(proxy))

        logger.info("✔ [Commands] ➜ /searchhistory (<spieler> [grund/seite])")
        logger.info("✔ [Commands] ➜ /searchstaffhistory (<staff> [grund/seite])")
        logger.info("✔ [Commands] ➜ /searchbanlist (<grund> [seite])")
        logger.info("✔ [Commands] ➜ /stafftop ([day/week/month/all/own])")
        logger.info("✔ [Commands] ➜ /lastseen (<spieler>)")
        logger.info("✔ [Commands] ➜ /bamelitebans reload")
        logger.info("🚀 [BameLiteBans] Plugin erfolgreich geladen und einsatzbereit!")
    }

    @Subscribe
    fun onServerConnected(event: ServerConnectedEvent) {
        val player = event.player
        val serverName = event.server.serverInfo.name
        logger.info("🌐 [LastSeen] ${player.username} betritt Server [$serverName]. Aktualisiere Zeitstempel...")
        lastSeenService.recordLastSeen(player.uniqueId.toString(), player.username, serverName)
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player
        val serverName = player.currentServer.map { it.serverInfo.name }.orElse("Netzwerk")
        logger.info("👤 [LastSeen] ${player.username} verlässt den Proxy (Letzter Server: $serverName). Speichere Status...")
        lastSeenService.recordLastSeen(player.uniqueId.toString(), player.username, serverName)
    }

    @Subscribe
    @Suppress("UNUSED_PARAMETER")
    fun onProxyShutdown(event: com.velocitypowered.api.event.proxy.ProxyShutdownEvent) {
        logger.info("🔴 [BameLiteBans] Proxy stoppt. Schließe Datenbank-Threadpool & leere Caches...")
        luckPermsService.clearCache()
        dbExecutor.shutdown()
        logger.info("✔ [BameLiteBans] Shutdown sauber abgeschlossen.")
    }
}
