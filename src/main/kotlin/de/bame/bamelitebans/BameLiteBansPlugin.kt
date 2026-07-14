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
        logger.info("BameLiteBans wird initialisiert...")

        configService = ConfigService(dataDirectory)
        historyService = LiteBansHistoryService(logger, luckPermsService = luckPermsService, executor = dbExecutor)
        lastSeenService = LastSeenService(proxy, historyService, logger, dbExecutor)

        lastSeenService.initTable()

        val lamp = VelocityLamp.builder(this, proxy).build()

        lamp.register(HistoryCommand(historyService, configService))
        lamp.register(StaffHistoryCommand(historyService, configService))
        lamp.register(StaffTopCommand(historyService, configService))
        lamp.register(ReloadCommand(configService))
        lamp.register(LastSeenCommand(lastSeenService, luckPermsService, configService))
        lamp.register(SearchBanListCommand(historyService, configService))

        lamp.accept(VelocityVisitors.brigadier(proxy))

        logger.info("BameLiteBans erfolgreich initialisiert! Befehle /searchhistory, /searchstaffhistory, /searchbanlist, /stafftop, /lastseen und /bamelitebans reload sind einsatzbereit.")
    }

    @Subscribe
    fun onServerConnected(event: ServerConnectedEvent) {
        val player = event.player
        val serverName = event.server.serverInfo.name
        lastSeenService.recordLastSeen(player.uniqueId.toString(), player.username, serverName)
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player
        val serverName = player.currentServer.map { it.serverInfo.name }.orElse(null)
        if (serverName != null) {
            lastSeenService.recordLastSeen(player.uniqueId.toString(), player.username, serverName)
        }
    }
}
