package dev.ranzlappen.gadget.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * JVM tests for [FeaturePreferences]. Uses a real
 * [PreferenceDataStoreFactory] backed by a temporary file so the
 * save / load / delete round-trip exercises the actual disk path
 * — no fakes that hide encoding bugs.
 */
class FeaturePreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Serializable
    private data class Sample(val name: String, val score: Int)

    private lateinit var ioScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: FeaturePreferences<Sample>

    @Before
    fun setUp() {
        ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val file = File(tempFolder.root, "feature_prefs_test.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = ioScope,
            produceFile = { file },
        )
        prefs = FeaturePreferences(
            dataStore = dataStore,
            keyPrefix = "sample_",
            serializer = Sample.serializer(),
            json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
        )
    }

    @After
    fun tearDown() {
        ioScope.cancel()
    }

    @Test
    fun `save then getAll returns the entry`() = runTest {
        prefs.save(1, Sample(name = "alpha", score = 10))

        val all = prefs.getAll()

        assertEquals(mapOf(1 to Sample("alpha", 10)), all)
    }

    @Test
    fun `save then get by id returns the entry`() = runTest {
        prefs.save(42, Sample("ultimate", 100))

        val got = prefs.get(42)

        assertEquals(Sample("ultimate", 100), got)
    }

    @Test
    fun `delete removes the entry from getAll`() = runTest {
        prefs.save(1, Sample("alpha", 1))
        prefs.save(2, Sample("beta", 2))

        prefs.delete(1)

        assertEquals(mapOf(2 to Sample("beta", 2)), prefs.getAll())
    }

    @Test
    fun `get returns null after delete`() = runTest {
        prefs.save(7, Sample("seven", 7))
        prefs.delete(7)

        assertNull(prefs.get(7))
    }

    @Test
    fun `all flow emits saved entries`() = runTest {
        prefs.all.test {
            // Initial empty emission.
            assertEquals(emptyMap(), awaitItem())

            prefs.save(1, Sample("first", 1))
            assertEquals(mapOf(1 to Sample("first", 1)), awaitItem())

            prefs.save(2, Sample("second", 2))
            assertEquals(
                mapOf(1 to Sample("first", 1), 2 to Sample("second", 2)),
                awaitItem(),
            )

            prefs.delete(1)
            assertEquals(mapOf(2 to Sample("second", 2)), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `keys outside prefix are ignored`() = runTest {
        // Create a sibling FeaturePreferences with a different prefix on the
        // SAME DataStore; entries should not leak between the two collections.
        val other = FeaturePreferences(
            dataStore = dataStore,
            keyPrefix = "other_",
            serializer = Sample.serializer(),
            json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
        )
        prefs.save(1, Sample("mine", 1))
        other.save(1, Sample("yours", 99))

        assertEquals(mapOf(1 to Sample("mine", 1)), prefs.getAll())
        assertEquals(mapOf(1 to Sample("yours", 99)), other.getAll())
    }
}
