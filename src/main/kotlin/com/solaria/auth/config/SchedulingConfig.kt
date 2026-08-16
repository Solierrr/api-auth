package com.solaria.auth.config

import com.solaria.auth.outbox.OutboxProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

// Habilita o processamento de @Scheduled — necessário para OutboxPollingScheduler e
// OutboxDeadLetterReclaimer, que não existiam antes desta integração (AuthApplication.kt não tem
// agendamento habilitado por padrão).
@Configuration
@EnableScheduling
// Liga OutboxProperties como Bean, lendo o prefixo "app.outbox" — sem isto, todo componente que
// depende dela (scheduler/publisher/reclaimer/consumer) falha no boot por dependência não satisfeita.
@EnableConfigurationProperties(OutboxProperties::class)
class SchedulingConfig
