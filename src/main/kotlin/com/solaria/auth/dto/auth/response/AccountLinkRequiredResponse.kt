package com.solaria.auth.dto.auth.response

data class AccountLinkRequiredResponse(
    val code: String = "ACCOUNT_LINK_REQUIRED",
    val message: String = "Authenticate with the existing account before linking Firebase",
    val email: String
)
