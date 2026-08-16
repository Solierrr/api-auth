package com.solaria.auth.dto.persistence.response

// Espelha ServiceTokenResponseDTO de api-persistence — resposta de mint e de refresh
data class ServiceTokenResponse(

    // token de serviço a ser enviado como "Authorization: Bearer" em chamadas a /internal/**
    val accessToken: String,

    // refresh token opaco, usado para renovar o access token sem repetir o clientSecret
    val refreshToken: String,

    // sempre "Bearer" hoje; mantido no contrato por completude
    val tokenType: String,

    // segundos até o access token expirar, contados a partir do instante desta resposta
    val expiresIn: Long
)
