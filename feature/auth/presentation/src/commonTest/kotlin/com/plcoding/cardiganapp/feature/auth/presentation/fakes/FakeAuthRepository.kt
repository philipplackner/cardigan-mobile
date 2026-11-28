package com.plcoding.cardiganapp.feature.auth.presentation.fakes

import com.plcoding.cardiganapp.core.domain.util.EmptyResult
import com.plcoding.cardiganapp.core.domain.util.Result
import com.plcoding.cardiganapp.feature.auth.domain.AuthError
import com.plcoding.cardiganapp.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CompletableDeferred

class FakeAuthRepository : AuthRepository {
    val loginCalls = mutableListOf<Pair<String, String>>()
    val registerCalls = mutableListOf<Pair<String, String>>()

    /** The next result. Null keeps the call suspended until [gate] completes. */
    var result: EmptyResult<AuthError> = Result.Success(Unit)
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun login(
        email: String,
        password: String,
    ): EmptyResult<AuthError> {
        loginCalls += email to password
        gate?.await()
        return result
    }

    override suspend fun register(
        email: String,
        password: String,
    ): EmptyResult<AuthError> {
        registerCalls += email to password
        gate?.await()
        return result
    }

    fun failWith(error: AuthError) {
        result = Result.Error(error)
    }
}
