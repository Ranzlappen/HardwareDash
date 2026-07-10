package dev.ranzlappen.gadget.core.data.logbook

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real-Room instrumented test for [LogbookDao] — mirrors
 * `AutomationEngineIntegrationTest`'s in-memory-database setup, the
 * established `:core:data` pattern for exercising a DAO against the actual
 * generated Room implementation rather than a JVM fake (Room's generated
 * `_Impl` classes and SQLite query validation only run under
 * `Room.inMemoryDatabaseBuilder`, which requires an instrumented Android
 * runtime).
 */
@RunWith(AndroidJUnit4::class)
class LogbookDaoTest {

    private lateinit var database: LogbookDatabase
    private lateinit var dao: LogbookDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LogbookDatabase::class.java,
        ).build()
        dao = database.logbookDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObserveEntries_newestFirst() = runTest {
        dao.insertEntry(LogbookEntryEntity(timestampMillis = 1_000L, text = "first", tag = LogbookTagColor.Teal))
        dao.insertEntry(LogbookEntryEntity(timestampMillis = 2_000L, text = "second", tag = LogbookTagColor.Rose))

        val entries = dao.observeEntries().first()

        assertEquals(2, entries.size)
        assertEquals("second", entries[0].text)
        assertEquals(LogbookTagColor.Rose, entries[0].tag)
        assertEquals("first", entries[1].text)
    }

    @Test
    fun deleteEntry_removesIt() = runTest {
        val id = dao.insertEntry(LogbookEntryEntity(timestampMillis = 1_000L, text = "temp"))

        dao.deleteEntry(id)

        assertTrue(dao.observeEntries().first().isEmpty())
    }

    @Test
    fun insertProcessWithCheckpoints_andObserveAllCheckpoints_groupedByProcess() = runTest {
        val processId = dao.insertProcess(LogbookProcessEntity(name = "Release checklist", createdAtMillis = 0L))
        dao.insertCheckpoints(
            listOf(
                LogbookCheckpointEntity(processId = processId, orderIndex = 0, name = "Tag build"),
                LogbookCheckpointEntity(processId = processId, orderIndex = 1, name = "Publish notes"),
            ),
        )

        val checkpoints = dao.getCheckpointsForProcess(processId)

        assertEquals(2, checkpoints.size)
        assertEquals("Tag build", checkpoints[0].name)
        assertEquals("Publish notes", checkpoints[1].name)
        assertTrue(checkpoints.none { it.completed })
    }

    @Test
    fun deletingProcess_cascadesToItsCheckpoints() = runTest {
        val processId = dao.insertProcess(LogbookProcessEntity(name = "Onboarding", createdAtMillis = 0L))
        val checkpointIds = dao.insertCheckpoints(
            listOf(LogbookCheckpointEntity(processId = processId, orderIndex = 0, name = "Step 1")),
        )

        dao.deleteProcess(processId)

        assertTrue(dao.getCheckpointsForProcess(processId).isEmpty())
        assertNull(dao.getCheckpoint(checkpointIds.single()))
    }

    @Test
    fun openCheckpointCount_excludesCompletedCheckpoints() = runTest {
        val processId = dao.insertProcess(LogbookProcessEntity(name = "Weekly review", createdAtMillis = 0L))
        val ids = dao.insertCheckpoints(
            listOf(
                LogbookCheckpointEntity(processId = processId, orderIndex = 0, name = "Open A"),
                LogbookCheckpointEntity(processId = processId, orderIndex = 1, name = "Open B"),
                LogbookCheckpointEntity(processId = processId, orderIndex = 2, name = "Will complete"),
            ),
        )

        assertEquals(3, dao.observeOpenCheckpointCount().first())

        val toComplete = dao.getCheckpoint(ids.last())!!
        dao.updateCheckpoint(toComplete.copy(completed = true, completedAtMillis = 5_000L))

        assertEquals(2, dao.observeOpenCheckpointCount().first())
    }
}
