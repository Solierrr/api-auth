package com.solaria.auth.dto.persistence.response

import java.util.UUID

// Espelha InternalUserProvisionResponseDTO de api-persistence
data class InternalUserProvisionResponse(

    // id interno do User em dbsolier
    val id: UUID,

    // mesmo authId enviado na requisição, repetido para conferência
    val authId: UUID,

    // true só na primeira vez que este authId é provisionado
    val created: Boolean
)
