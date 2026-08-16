package com.solaria.auth.outbox

import com.solaria.auth.repository.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

//  Procura por OutboxEvent não publicado e publica na redis stream pelo OutboxStreamPublisher
@Component
class OutboxPollingScheduler(
    private val outboxEventRepository: OutboxEventRepository,
    private val streamPublisher: OutboxStreamPublisher,
    private val properties: OutboxProperties
) {
    private val log = LoggerFactory.getLogger(OutboxPollingScheduler::class.java)

    // roda a cada app.outbox.poll-interval-ms;
    // @Transactional garante que o publishedAt só é persistido se a adição (XADD) teve sucesso
    @Scheduled(fixedDelayString = "\${app.outbox.poll-interval-ms}")
    @Transactional
    fun pollAndPublish() {
        // lote de eventos pendentes, do mais antigo para o mais novo
        val pending = outboxEventRepository.findUnpublished(PageRequest.of(0, properties.batchSize))
        for (event in pending) {
            try {
                // adicao (XADD) na stream
                streamPublisher.publish(event)
                // marca como publicado somente após o XADD confirmado
                event.publishedAt = Instant.now()
                // adiciona uma tentativa para adição na fila
                event.attempts += 1
                outboxEventRepository.save(event)
            } catch (publishFailure: Exception) {
                // a próxima execução deste poller tenta de novo
                log.warn("Falha ao publicar outbox_event {} na stream, tentando de novo na próxima execução", event.id, publishFailure)
            }
        }
    }
}
