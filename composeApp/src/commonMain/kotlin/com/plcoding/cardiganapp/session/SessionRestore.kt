package com.plcoding.cardiganapp.session

import com.plcoding.cardiganapp.core.domain.session.SessionManager
import com.plcoding.cardiganapp.core.domain.session.SessionState
import org.koin.mp.KoinPlatform

/** True while the stored session is being restored. The Android splash stays up until it is false. */
fun isRestoringSession(): Boolean = KoinPlatform.getKoin().get<SessionManager>().state.value == SessionState.Restoring
