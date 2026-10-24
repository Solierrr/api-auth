package com.solaria.auth

import com.solaria.auth.entity.OutboxEvent
import com.solaria.auth.outbox.OutboxDeadLetterReclaimer
import com.solaria.auth.outbox.OutboxProperties
import com.solaria.auth.outbox.OutboxStreamPublisher
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.PendingMessage
import org.springframework.data.redis.connection.stream.PendingMessages
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.StreamOperations
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutboxStreamTrimTests {
    private val properties = OutboxProperties(
        pollIntervalMs = 2000,
        batchSize = 50,
        streamKey = "auth:outbox:user-events",
        consumerGroup = "core-user-provisioner",
        dlqStreamKey = "auth:outbox:user-events:dlq",
        maxDeliveryAttempts = 5,
        visibilityTimeoutMs = 60000,
        reclaimIntervalMs = 30000,
        streamMaxlen = 1234
    )

    @Suppress("UNCHECKED_CAST")
    private val ops = Mockito.mock(StreamOperations::class.java) as StreamOperations<String, String, String>
    private val redisTemplate = Mockito.mock(StringRedisTemplate::class.java).also {
        Mockito.`when`(it.opsForStream<String, String>()).thenReturn(ops)
    }

    private fun assertApproximateTrim(streamKey: String) {
        val options = ArgumentCaptor.forClass(XAddOptions::class.java)
        Mockito.verify(ops).add(eq(streamKey), anyMap(), options.capture())
        assertEquals(1234L, options.value.maxlen)
        assertTrue(options.value.isApproximateTrimming)
    }

    @Test
    fun `publisher trims the outbox stream to the configured approximate maxlen`() {
        val event = OutboxEvent(
            aggregateType = "user",
            aggregateId = UUID.randomUUID(),
            eventType = "USER_REGISTERED",
            payload = "{}"
        )

        OutboxStreamPublisher(redisTemplate, properties).publish(event)

        assertApproximateTrim(properties.streamKey)
    }

    @Test
    fun `dead letter stream is trimmed to the configured approximate maxlen`() {
        val stale = PendingMessage(
            RecordId.of("1-0"),
            Consumer.from(properties.consumerGroup, "auth-consumer"),
            Duration.ofMinutes(5),
            properties.maxDeliveryAttempts.toLong()
        )
        Mockito.`when`(ops.pending(properties.streamKey, properties.consumerGroup, Range.unbounded<String>(), 100L))
            .thenReturn(PendingMessages(properties.consumerGroup, listOf(stale)))

        OutboxDeadLetterReclaimer(redisTemplate, properties, ObservationRegistry.NOOP).reclaimStalePending()

        assertApproximateTrim(properties.dlqStreamKey)
        Mockito.verify(ops).acknowledge(properties.streamKey, properties.consumerGroup, "1-0")
    }
}
