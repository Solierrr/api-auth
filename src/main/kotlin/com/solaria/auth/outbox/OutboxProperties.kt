package com.solaria.auth.outbox

import org.springframework.boot.context.properties.ConfigurationProperties

// Liga esta classe ao prefixo "app.outbox" do application.properties
@ConfigurationProperties(prefix = "app.outbox")
data class OutboxProperties(

    // intervalo entre execuções do OutboxPollingScheduler
    val pollIntervalMs: Long,

    // quantidade máxima de eventos de outbox lidos por execução do poller
    val batchSize: Int,

    // chave da Redis Stream onde os eventos de outbox são publicados
    val streamKey: String,

    // nome do consumer group que lê a stream acima
    val consumerGroup: String,

    // chave da Redis Stream usada como DLQ
    val dlqStreamKey: String,

    // número máximo de tentativas de entrega antes de mover a mensagem para a DLQ
    val maxDeliveryAttempts: Int,

    // tempo que uma mensagem pode ficar pendente antes de ser tentada novamente
    val visibilityTimeoutMs: Long,

    // intervalo entre execuções do OutboxDeadLetterReclaimer
    val reclaimIntervalMs: Long
)
