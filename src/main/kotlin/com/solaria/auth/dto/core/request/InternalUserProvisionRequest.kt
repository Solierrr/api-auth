package com.solaria.auth.dto.core.request

import java.util.UUID

// Espelha InternalUserProvisionRequestDTO de api-core (POST /internal/users)
data class InternalUserProvisionRequest(

    // sub do JWT de usuário emitido por este serviço; chave de idempotência do lado de api-core
    val authId: UUID
)
