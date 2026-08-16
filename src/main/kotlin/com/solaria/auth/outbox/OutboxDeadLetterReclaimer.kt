package com.solaria.auth.outbox

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions
import org.springframework.data.redis.connection.stream.PendingMessage
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration


// procura as mensagens pendentes do consumer group e decide entre mandar pra retry ou pra DLQ
// [BACKLOG]-> **analisar futura integração com grafana ou outro sistema de observabilidade para DLQs**
@Component
class OutboxDeadLetterReclaimer(
    private val redisTemplate: StringRedisTemplate,
    private val properties: OutboxProperties
) {
    private val log = LoggerFactory.getLogger(OutboxDeadLetterReclaimer::class.java)

    private fun consumerName(): String = "auth-${ProcessHandle.current().pid()}-reclaimer"

    // roda a cada app.outbox.reclaim-interval-ms
    @Scheduled(fixedDelayString = "\${app.outbox.reclaim-interval-ms}")
    fun reclaimStalePending() {
        val ops = redisTemplate.opsForStream<String, String>()
        // até 100 mensagens pendentes por execução
        val pending = ops.pending(properties.streamKey, properties.consumerGroup, Range.unbounded<String>(), 100L)
            ?: return

        for (message in pending) {
            // barreira para caso outro consumer esteja processando a mensagem
            if (message.elapsedTimeSinceLastDelivery < Duration.ofMillis(properties.visibilityTimeoutMs)) continue

            if (message.totalDeliveryCount >= properties.maxDeliveryAttempts) {
                // excedeu o limite de tentativas -> move para a DLQ
                moveToDeadLetter(message)
            } else {
                // abaixo do limite -> reclama para este consumer, que a pegará na próxima leitura
                reclaimForRetry(message)
            }
        }
    }

    // assume posse de mensagem pendente de outro consumer (XCLAIM) para permitir uma nova tentativa de processamento
    private fun reclaimForRetry(message: PendingMessage) {
        redisTemplate.opsForStream<String, String>().claim(
            properties.streamKey,
            properties.consumerGroup,
            consumerName(),
            XClaimOptions.minIdle(Duration.ofMillis(properties.visibilityTimeoutMs)).ids(message.idAsString)
        )
    }


    // publica uma copia da mensagem na DLQ e confirma a mensagem original, tirando ela das mensagens pendentes
    private fun moveToDeadLetter(message: PendingMessage) {
        val ops = redisTemplate.opsForStream<String, String>()
        // registro na DLQ para localizar/inspecionar manualmente o evento original
        ops.add(
            properties.dlqStreamKey,
            mapOf(
                "originalRecordId" to message.idAsString,
                "totalDeliveryCount" to message.totalDeliveryCount.toString()
            )
        )
        // remove a entrada original da lista de pendentes do grupo, já que foi "resolvida" (via DLQ)
        ops.acknowledge(properties.streamKey, properties.consumerGroup, message.idAsString)
        log.warn(
            "Evento {} excedeu {} tentativas, movido para a DLQ {}",
            message.idAsString, properties.maxDeliveryAttempts, properties.dlqStreamKey
        )
    }
}
