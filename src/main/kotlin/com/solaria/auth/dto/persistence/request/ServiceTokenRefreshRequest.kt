package com.solaria.auth.dto.persistence.request

// Espelha ServiceTokenRefreshRequestDTO de api-persistence (POST /internal/service-tokens/refresh)
data class ServiceTokenRefreshRequest(

    // refresh token opaco (JJWT/HS256) recebido num mint/refresh anterior
    val refreshToken: String
)
