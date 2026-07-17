package de.bame.bamelitebans.service

import de.bame.bamelitebans.config.ConfigService
import de.bame.bamelitebans.util.JsonUtil
import litebans.api.Entry
import org.slf4j.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service für das asynchrone Versenden von Strafen und Entbannungen an Discord Webhooks.
 * Enthält eigene Warteschlange und Rate-Limit-Schutz (~5 Requests/2 Sek. per Webhook).
 */
class DiscordWebhookService(
    private val configService: ConfigService,
    private val logger: Logger,
    private val luckPermsService: LuckPermsService
) {

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "BameLiteBans-Webhook").apply { isDaemon = true }
    }

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private data class WebhookTask(val entry: Entry, val removed: Boolean)
    private val queue = ConcurrentLinkedQueue<WebhookTask>()
    private val isProcessing = AtomicBoolean(false)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun sendPunishment(entry: Entry, removed: Boolean) {
        if (!configService.webhook.isEnabled) return

        val url = configService.webhook.url
        if (!url.startsWith("https://discord.com/api/webhooks/") && !url.startsWith("https://discordapp.com/api/webhooks/")) {
            logger.warn("[Webhook] Ungültige Discord Webhook-URL in config.toml strukturiert. Versand abgebrochen.")
            return
        }

        val type = entry.type ?: return
        if (!configService.webhook.isTypeEnabled(type, removed)) {
            return
        }

        queue.offer(WebhookTask(entry, removed))
        scheduleProcessQueue()
    }

    private fun scheduleProcessQueue() {
        if (isProcessing.compareAndSet(false, true)) {
            executor.execute { processNext() }
        }
    }

    private fun processNext() {
        val task = queue.poll()
        if (task == null) {
            isProcessing.set(false)
            return
        }

        val url = configService.webhook.url
        if (url.isBlank()) {
            isProcessing.set(false)
            return
        }

        val payload = buildJsonPayload(task.entry, task.removed)
        sendWithRetry(url, payload, retryCount = 0)
    }

    private fun sendWithRetry(url: String, payload: String, retryCount: Int) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/json; charset=utf-8")
            .header("User-Agent", "BameLiteBans/1.0.0 (Velocity)")
            .POST(HttpRequest.BodyPublishers.ofString(payload, Charsets.UTF_8))
            .build()

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .whenCompleteAsync({ response, exception ->
                if (exception != null) {
                    if (retryCount == 0) {
                        logger.warn("[Webhook] Netzwerkfehler beim Senden an Discord. Starte 1 Retry in 2 Sekunden...")
                        executor.schedule({ sendWithRetry(url, payload, 1) }, 2, TimeUnit.SECONDS)
                    } else {
                        logger.error("[Webhook] Versand an Discord nach Retry fehlgeschlagen. Eintrag verworfen: ${exception.message}")
                        scheduleNextDelay(400)
                    }
                    return@whenCompleteAsync
                }

                val status = response.statusCode()
                if (status in 200..299) {
                    scheduleNextDelay(400) // 400ms Abstand (~2.5 Requests pro Sekunde -> sicher unter Rate Limit)
                } else if (status == 429) {
                    if (retryCount == 0) {
                        logger.warn("[Webhook] Discord Rate-Limit erreicht (429). Warte 3 Sekunden und versuche 1x erneut...")
                        executor.schedule({ sendWithRetry(url, payload, 1) }, 3, TimeUnit.SECONDS)
                    } else {
                        logger.error("[Webhook] Rate-Limit nach Retry weiter aktiv. Eintrag verworfen.")
                        scheduleNextDelay(1000)
                    }
                } else {
                    logger.warn("[Webhook] Discord antwortete mit Status $status: ${response.body()}")
                    scheduleNextDelay(400)
                }
            }, executor)
    }

    private fun scheduleNextDelay(delayMs: Long) {
        if (!queue.isEmpty()) {
            executor.schedule({ processNext() }, delayMs, TimeUnit.MILLISECONDS)
        } else {
            isProcessing.set(false)
            // Falls während des Setzens noch ein Element reinkam
            if (!queue.isEmpty() && isProcessing.compareAndSet(false, true)) {
                executor.schedule({ processNext() }, delayMs, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun buildJsonPayload(entry: Entry, removed: Boolean): String {
        val type = entry.type ?: "unknown"
        val title = JsonUtil.escape(configService.webhook.getTitle(type, removed))
        val color = configService.webhook.getColor(type, removed)

        val targetName = if (entry.uuid != null) resolveOrFormatName(entry.uuid) else (entry.ip ?: "Unbekannt")
        val rawStaffName = if (removed) {
            entry.removedByName ?: "Konsole"
        } else {
            entry.executorName ?: "Konsole"
        }
        val staffUuidStr = if (removed) entry.removedByUUID else entry.executorUUID
        val staffPrefixRaw = if (!staffUuidStr.isNullOrBlank() && !"CONSOLE".equals(rawStaffName, ignoreCase = true)) {
            luckPermsService.getPrefix(staffUuidStr)
        } else ""
        val cleanPrefix = de.bame.bamelitebans.util.ColorParser.stripColors(staffPrefixRaw)
        val staffName = if (cleanPrefix.isNotBlank()) {
            "$cleanPrefix $rawStaffName"
        } else {
            rawStaffName
        }

        val reasonText = if (removed) {
            entry.removalReason ?: "Kein Grund angegeben"
        } else {
            entry.reason ?: "Kein Grund angegeben"
        }

        val timeFormatted = dateFormatter.format(Instant.ofEpochMilli(entry.dateStart))

        val durationStr = if (removed) {
            "Aufgehoben"
        } else if (entry.isPermanent) {
            "Permanent"
        } else {
            try {
                val cleanDuration = entry.durationString?.replace(Regex("§[0-9a-fk-orA-FK-OR]"), "") ?: "Temporär"
                cleanDuration.ifBlank { "Temporär" }
            } catch (_: Exception) {
                "Temporär"
            }
        }

        val serverOrigin = entry.serverOrigin ?: "Netzwerk"

        val avatarUrl = if (entry.uuid != null && entry.uuid!!.matches(LiteBansHistoryService.UUID_PATTERN)) {
            "https://mc-heads.net/avatar/${entry.uuid}/64"
        } else {
            "https://mc-heads.net/avatar/X/64"
        }

        val fieldsBuilder = java.lang.StringBuilder()
        fieldsBuilder.append("""{"name": "👤 **Spieler**", "value": "`${JsonUtil.escape(targetName)}`", "inline": true},""")
        fieldsBuilder.append("""{"name": "🛡️ **Staff**", "value": "`${JsonUtil.escape(staffName)}`", "inline": true},""")
        fieldsBuilder.append("""{"name": "⏱️ **Dauer**", "value": "`${JsonUtil.escape(durationStr)}`", "inline": true},""")
        fieldsBuilder.append("""{"name": "📝 **Grund**", "value": "${JsonUtil.escape(reasonText)}", "inline": false},""")
        fieldsBuilder.append("""{"name": "⌚ **Datum**", "value": "${JsonUtil.escape(timeFormatted)}", "inline": true}""")

        return """
        {
          "embeds": [
            {
              "title": "$title",
              "color": $color,
              "thumbnail": {
                "url": "$avatarUrl"
              },
              "fields": [
                $fieldsBuilder
              ],
              "footer": {
                "text": "Server: ${JsonUtil.escape(serverOrigin)} • BameLiteBans"
              },
              "timestamp": "${Instant.now()}"
            }
          ]
        }
        """.trimIndent()
    }

    private fun resolveOrFormatName(uuidStr: String?): String {
        if (uuidStr.isNullOrBlank()) return "Unbekannt"
        return try {
            val uuid = java.util.UUID.fromString(uuidStr)
            litebans.api.Database.get().getPlayerName(uuid) ?: uuidStr
        } catch (_: Exception) {
            uuidStr
        }
    }

    fun shutdown() {
        executor.shutdown()
    }
}

