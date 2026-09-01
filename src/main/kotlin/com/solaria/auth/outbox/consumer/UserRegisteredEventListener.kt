package com.solaria.auth.outbox.consumer

import com.solaria.auth.integration.core.CoreUserClient
import com.solaria.auth.outbox.OutboxProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.stream.MapRecord
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.stream.StreamListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

// Listener da fila de redis -> especifico para USER_REGISTERED -> faz request para api-core criar o user
@Component
class UserRegisteredEventListener(
    private val coreUserClient: CoreUserClient,
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val properties: OutboxProperties
) : StreamListener<String, MapRecord<String, String, String>> {

    private val log = LoggerFactory.getLogger(UserRegisteredEventListener::class.java)

    // chamado pelo StreamMessageListenerContainer para cada entrada ainda não confirmada da stream
    override fun onMessage(record: MapRecord<String, String, String>) {

        val fields = record.value
        val eventType = fields["eventType"]

        try {
            when (eventType) {
                // único evento que efetivamente provisiona um User em api-core
                "USER_REGISTERED" -> handleUserRegistered(fields["payload"])
                // outros eventos são ignorados por esse listener
                else -> log.debug("eventType diferente de USER_REGISTERED ignorado: {}", eventType)
            }
            //  após processamento ser concluído com sucesso -> confirma mensagem no consumer group (XACK)
            acknowledge(record)
        } catch (processingFailure: Exception) {
            // se não ->  mensagem continua pendente no consumer group
            log.warn("Falha ao processar evento de outbox {}, deixando pendente para retry", record.id, processingFailure)
        }
    }

    // extrai authId do JSON e delega para CoreUserClient fazer a request para api-core
    private fun handleUserRegistered(payloadJson: String?) {
        requireNotNull(payloadJson) { "payload ausente no evento USER_REGISTERED" }
        // payload gravado como {"authUserId":"<uuid>"} por UserServiceImpl/FirebaseAuthenticationServiceImpl
        val authUserId = objectMapper.readTree(payloadJson).get("authUserId").asString()
        coreUserClient.provisionUser(UUID.fromString(authUserId))
    }

    // confirma está entrada no consumer group configurado (XACK)
    private fun acknowledge(record: MapRecord<String, String, String>) {
        redisTemplate.opsForStream<String, String>().acknowledge(properties.consumerGroup, record)
    }
}
