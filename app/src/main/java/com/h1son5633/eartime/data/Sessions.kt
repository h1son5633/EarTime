package com.h1son5633.eartime.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * One wearing session: a Bluetooth audio device stayed connected
 * from [startMs] until [endMs]. [endMs] is null while the session is
 * still open; [lastSeenMs] is a heartbeat so a killed process can close
 * a stale session at the last moment it was known alive.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceName: String,
    val deviceAddress: String,
    val startMs: Long,
    val endMs: Long? = null,
    val lastSeenMs: Long = startMs
) {
    fun effectiveEnd(now: Long): Long = endMs ?: now
    fun durationMs(now: Long): Long = (effectiveEnd(now) - startMs).coerceAtLeast(0)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startMs ASC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE endMs IS NULL")
    suspend fun openSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE endMs IS NULL AND deviceAddress = :address LIMIT 1")
    suspend fun openSessionFor(address: String): SessionEntity?

    @Query("DELETE FROM sessions WHERE deviceAddress = :address")
    suspend fun deleteForAddress(address: String)

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)
}

@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class EarTimeDb : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
