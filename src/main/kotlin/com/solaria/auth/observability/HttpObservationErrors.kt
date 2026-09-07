package com.solaria.auth.observability

import io.micrometer.observation.Observation
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.filter.ServerHttpObservationFilter

/**
 * Marca a requisição HTTP atual como erro quando uma exceção já foi tratada pela aplicação
 *
 * Só atualiza a observabilidade da request
 * não altera a resposta HTTP nem o tratamento da exceção
 */
object HttpObservationErrors {

    /**
     * Atributo de request onde o `ServerHttpObservationFilter` guarda a `Observation`
     */
    private const val OBSERVATION_ATTRIBUTE =
        "org.springframework.web.filter.ServerHttpObservationFilter.observation"

    /**
     * Registra a exceção na observação da request atual
     *
     * Sem request, sem exception ou sem observação -> no-op
     */
    @JvmStatic
    fun mark(request: HttpServletRequest?, error: Throwable?) {
        if (request == null || error == null) {
            return
        }

        val attribute = request.getAttribute(OBSERVATION_ATTRIBUTE)
        if (attribute is Observation) {
            attribute.error(error)
            return
        }

        ServerHttpObservationFilter.findObservationContext(request)
            .ifPresent { context -> context.setError(error) }
    }
}
