package com.h1son5633.eartime.tracking

import com.h1son5633.eartime.data.SessionDao
import com.h1son5633.eartime.data.SessionEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns session start/stop so broadcasts, profile callbacks and service
 * restarts can all race without double-counting. A session is keyed by
 * device address and persisted open (endMs = null) from the moment it
 * starts, so process death never loses the in-progress count.
 */
class SessionManager(private val dao: SessionDao) {
    private val mutex = Mutex()

    suspend fun onConnected(name: String, address: String) {
        mutex.withLock {
            if (dao.openSessionFor(address) != null) return
            val now = System.currentTimeMillis()
            dao.insert(
                SessionEntity(
                    deviceName = name.ifBlank { address },
                    deviceAddress = address,
                    startMs = now,
                    lastSeenMs = now
                )
            )
        }
    }

    suspend fun onDisconnected(address: String) {
        mutex.withLock {
            val open = dao.openSessionFor(address) ?: return
            val now = System.currentTimeMillis()
            dao.update(open.copy(endMs = now, lastSeenMs = now))
        }
    }

    /** Remove every session belonging to a device that is not an earphone. */
    suspend fun purgeDevices(addresses: Set<String>) {
        if (addresses.isEmpty()) return
        mutex.withLock {
            for (address in addresses) dao.deleteForAddress(address)
        }
    }

    /** Heartbeat while the service is alive. */
    suspend fun heartbeat() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            for (open in dao.openSessions()) dao.update(open.copy(lastSeenMs = now))
        }
    }

    /**
     * After a (re)start: sessions whose device is connected again resume
     * untouched; any other stale open session is closed at its last
     * heartbeat, which is the last moment it was provably alive.
     */
    suspend fun reconcile(connectedAddresses: Set<String>) {
        mutex.withLock {
            for (open in dao.openSessions()) {
                if (open.deviceAddress !in connectedAddresses) {
                    dao.update(open.copy(endMs = open.lastSeenMs))
                }
            }
        }
    }
}
