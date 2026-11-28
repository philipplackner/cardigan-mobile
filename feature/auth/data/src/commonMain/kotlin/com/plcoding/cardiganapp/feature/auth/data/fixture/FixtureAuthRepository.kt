package com.plcoding.cardiganapp.feature.auth.data.fixture

import com.plcoding.cardiganapp.core.domain.text.normalizeText
import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.auth.domain.AuthError
import com.plcoding.cardiganapp.feature.auth.domain.AuthRepository
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * TEMPORARY (D3): picks the result from the email, debug builds only. Removed in task 9.1 once
 * mob-auth-connect binds the real repository.
 * - `wrong@…`: wrong credentials
 * - `taken@…`: email already registered
 * - `limited@…`: rate limited for 12 seconds
 * - `offline@…`: no connection
 * - anything else: signed in after [latency], so the loading state can be seen
 */
class FixtureAuthRepository(
    private val session: InMemorySessionManager,
    private val latency: Duration = 800.milliseconds,
) : AuthRepository {
    override suspend fun login(
        email: String,
        password: String,
    ): EmptyResult<AuthError> = respond(email)

    override suspend fun register(
        email: String,
        password: String,
    ): EmptyResult<AuthError> = respond(email)

    private suspend fun respond(email: String): EmptyResult<AuthError> {
        val error =
            when (normalizeText(email).lowercase().substringBefore('@')) {
                "wrong" -> AuthError.InvalidCredentials
                "taken" -> AuthError.EmailAlreadyUsed
                "limited" -> AuthError.RateLimited(12)
                "offline" -> AuthError.Unavailable
                else -> null
            }
        if (error != null) return Result.Error(error)
        delay(latency)
        session.signIn(email)
        return Result.Success(Unit)
    }
}
