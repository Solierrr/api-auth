package com.solaria.auth.dto.persistence.request

import java.util.UUID

// Espelha InternalUserProvisionRequestDTO de api-persistence (POST /internal/users)
data class InternalUserProvisionRequest(

    // sub do JWT de usuário emitido por este serviço; chave de idempotência do lado de api-persistence
    val authId: UUID
)
