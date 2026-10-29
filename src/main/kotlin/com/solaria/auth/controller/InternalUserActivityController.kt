package com.solaria.auth.controller

import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.repository.UserAccountRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class UserActivityResponse(val authId: UUID, val active: Boolean)

data class UserActivityBatchRequest(val authIds: List<String?>? = null)

data class UserActivityBatchResponse(
    val users: List<UserActivityResponse>,
    val missingAuthIds: List<UUID>
)

@RestController
class InternalUserActivityController(
    private val userAccountRepository: UserAccountRepository
) {
    @GetMapping("/internal/users/{authId}/activity")
    fun getActivity(@PathVariable authId: String): UserActivityResponse {
        val id = parseCanonicalUuid(authId)
        val user = userAccountRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND)
        }
        return user.toActivityResponse()
    }

    @PostMapping("/internal/users/activity/batch")
    fun getActivityBatch(@RequestBody request: UserActivityBatchRequest): UserActivityBatchResponse {
        val ids = request.authIds?.map { value ->
            parseCanonicalUuid(value ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST))
        }?.distinct()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        if (ids.isEmpty()) return UserActivityBatchResponse(emptyList(), emptyList())

        val users = userAccountRepository.findAllById(ids)
        val foundIds = users.mapNotNull(UserAccount::id).toSet()
        return UserActivityBatchResponse(
            users = users.map { it.toActivityResponse() },
            missingAuthIds = ids.filterNot(foundIds::contains)
        )
    }

    private fun UserAccount.toActivityResponse() = UserActivityResponse(
        authId = requireNotNull(id),
        active = status == AccountStatus.ACTIVE
    )

    private fun parseCanonicalUuid(value: String): UUID {
        if (!CANONICAL_UUID.matches(value)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
        return UUID.fromString(value)
    }

    private companion object {
        val CANONICAL_UUID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}
