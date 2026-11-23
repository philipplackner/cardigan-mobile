package com.plcoding.cardiganapp.core.domain.session

/**
 * An in-memory store of data loaded for the signed-in user. Every such store is bound to this
 * interface in Koin, so it is cleared whenever the session state becomes signed out.
 */
interface SessionScopedStore {
    /**
     * Drops all data held for the user. The store must also drop the result of any request that
     * started before this call, so a late response can't write the old user's data back.
     */
    fun clear()
}
