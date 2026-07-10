package dev.ranzlappen.gadget.core.data.logbook

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data-access surface for the Logbook tables. Lives in `:core:data`,
 * mirroring the `AppsDao` precedent (a feature-scoped Room store owned by
 * `:core:data`, consumed only through its DAO by `:feature:logbook`'s
 * `LogbookRepository` — never through `Room.databaseBuilder` directly from
 * a feature module).
 */
@Dao
interface LogbookDao {

    // ── Entries ──────────────────────────────────────────────────────────
    @Insert
    suspend fun insertEntry(entry: LogbookEntryEntity): Long

    @Query("DELETE FROM logbook_entry WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query("SELECT * FROM logbook_entry ORDER BY timestamp_millis DESC")
    fun observeEntries(): Flow<List<LogbookEntryEntity>>

    // ── Processes ────────────────────────────────────────────────────────
    @Insert
    suspend fun insertProcess(process: LogbookProcessEntity): Long

    @Query("DELETE FROM logbook_process WHERE id = :id")
    suspend fun deleteProcess(id: Long)

    @Query("SELECT * FROM logbook_process ORDER BY created_at_millis DESC")
    fun observeProcesses(): Flow<List<LogbookProcessEntity>>

    @Query("SELECT * FROM logbook_process WHERE id = :id")
    suspend fun getProcess(id: Long): LogbookProcessEntity?

    // ── Checkpoints ──────────────────────────────────────────────────────
    @Insert
    suspend fun insertCheckpoints(checkpoints: List<LogbookCheckpointEntity>): List<Long>

    @Update
    suspend fun updateCheckpoint(checkpoint: LogbookCheckpointEntity)

    /** All checkpoints across every process, ordered for grouping by
     *  [LogbookCheckpointEntity.processId] client-side (Room has no
     *  built-in `@Relation` grouping into a `Map`, and this table stays
     *  small enough that a single flow + in-memory `groupBy` is simpler
     *  than per-process flow fan-out). */
    @Query("SELECT * FROM logbook_checkpoint ORDER BY process_id ASC, order_index ASC")
    fun observeAllCheckpoints(): Flow<List<LogbookCheckpointEntity>>

    @Query("SELECT * FROM logbook_checkpoint WHERE process_id = :processId ORDER BY order_index ASC")
    suspend fun getCheckpointsForProcess(processId: Long): List<LogbookCheckpointEntity>

    @Query("SELECT * FROM logbook_checkpoint WHERE id = :id")
    suspend fun getCheckpoint(id: Long): LogbookCheckpointEntity?

    /** The Logbook `MetricSource` signal: how many checkpoints are still
     *  incomplete (open) right now, across every process. */
    @Query("SELECT COUNT(*) FROM logbook_checkpoint WHERE completed = 0")
    fun observeOpenCheckpointCount(): Flow<Int>
}
