package com.solaria.auth.outbox.consumer

import com.solaria.auth.outbox.OutboxProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamMessageListenerContainer
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions
import java.time.Duration

// Registra consumer group e conecta listeners a ele por StreamMessageListenerContainer
@Configuration
@ConditionalOnProperty(prefix = "app.outbox.consumer", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OutboxStreamConsumerConfig(
    private val redisTemplate: StringRedisTemplate,
    private val properties: OutboxProperties,
    private val listener: UserRegisteredEventListener
) {
    private fun consumerName(): String = "auth-${ProcessHandle.current().pid()}"

    // cria o consumer group na stream
    private fun ensureConsumerGroup() {
        try {
            redisTemplate.opsForStream<String, String>()
                .createGroup(properties.streamKey, ReadOffset.from("0"), properties.consumerGroup)
        } catch (alreadyExists: Exception) {
            // grupo já existe —> comportamento esperado
        }
    }

    // sobe o container que lê a stream e despacha cada entrada para os listeners
    @Bean(destroyMethod = "stop")
    fun outboxStreamListenerContainer(
        connectionFactory: RedisConnectionFactory
    ): StreamMessageListenerContainer<String, MapRecord<String, String, String>> {
        // garante que o consumer group esteja criado
        ensureConsumerGroup()

        // seta consumer que trabalha com registros MapRecord<String, String, String>
        // e espera até 2 segundos para um novo pooling(request para fila redis)
        val options: StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> =
            StreamMessageListenerContainerOptions.builder()
                .pollTimeout(Duration.ofSeconds(2))
                .build()
        val container = StreamMessageListenerContainer.create(connectionFactory, options)

        // receber mensagem != confirmar -> UserRegisteredEventListener decide quando confirmar
        container.receive(
            Consumer.from(properties.consumerGroup, consumerName()),
            StreamOffset.create(properties.streamKey, ReadOffset.lastConsumed()),
            listener
        )
        // liga o container
        container.start()
        return container
    }
}
