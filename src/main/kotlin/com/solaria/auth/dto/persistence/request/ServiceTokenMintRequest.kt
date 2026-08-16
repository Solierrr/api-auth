package com.solaria.auth.dto.persistence.request

// Espelha ServiceTokenMintRequestDTO de api-persistence (POST /internal/service-tokens)
data class ServiceTokenMintRequest(

    // segredo compartilhado (SERVICE_CLIENT_SECRET), precisa ser idêntico ao configurado em api-persistence
    val clientSecret: String
)
