package com.solaria.auth.outbox

import com.solaria.auth.entity.OutboxEvent
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

// publica um OutboxEvent em uma Redis stream (fila)
@Component
class OutboxStreamPublisher(
    private val redisTemplate: StringRedisTemplate,
    private val properties: OutboxProperties
) {
    // publica este evento na stream
    fun publish(event: OutboxEvent) {
        // campos do registro todos como String (StringRedisTemplate opera só String)
        val fields = mapOf(
            "eventId" to event.id.toString(),
            "aggregateType" to event.aggregateType,
            "aggregateId" to event.aggregateId.toString(),
            "eventType" to event.eventType,
            "payload" to event.payload,
            "createdAt" to DateTimeFormatter.ISO_INSTANT.format(event.createdAt)
        )
        // adiciona evento na stream configurada (XADD)
        redisTemplate.opsForStream<String, String>().add(properties.streamKey, fields)
    }
}
