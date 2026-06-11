package dev.ranzlappen.gadget.core.data.automation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY created_at ASC")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: String): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE rules SET enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    /** Cooldown clock (ADR-0002 Decision 8) — survives process death/reboot. */
    @Query("UPDATE rules SET last_fired_at = :firedAtMs WHERE id = :id")
    suspend fun markFired(id: String, firedAtMs: Long)

    @Query("SELECT last_fired_at FROM rules WHERE id = :id")
    suspend fun lastFiredAt(id: String): Long?
}
