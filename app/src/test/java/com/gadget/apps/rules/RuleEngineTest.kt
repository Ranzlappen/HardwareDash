package com.gadget.apps.rules

import com.gadget.data.db.apps.AppRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val now = 1_700_000_000_000L
    private val dayMs = 24L * 60 * 60 * 1000

    private fun installed(
        key: String,
        pkg: String,
        installedAt: Long = now - 365 * dayMs,
        webApk: Boolean = false,
    ) = AppRecord(
        appKey = key,
        packageName = pkg,
        activityClass = "$pkg.Main",
        label = pkg,
        userSerial = 0L,
        isWebApk = webApk,
        isWebLink = false,
        firstInstallTime = installedAt,
        lastSeen = now,
    )

    private fun webLink(id: Long, label: String) = AppRecord(
        appKey = "weblink:$id",
        packageName = "weblink",
        activityClass = null,
        label = label,
        userSerial = 0L,
        isWebApk = false,
        isWebLink = true,
        firstInstallTime = now - 30 * dayMs,
        lastSeen = now,
    )

    private val catalog = listOf(
        installed("installed:0:com.google.android.gm/Main", "com.google.android.gm"),
        installed("installed:0:com.google.maps/Main", "com.google.maps"),
        installed("installed:0:com.acme.note/Main", "com.acme.note", installedAt = now - 2 * dayMs),
        installed("installed:0:org.chromium.webapk.HN/Main", "org.chromium.webapk.HN", webApk = true),
        webLink(1L, "Hacker News"),
    )

    // ── Manual ──────────────────────────────────────────────────────────────

    @Test
    fun `manual rule returns only apps in the membership set`() {
        val members = setOf(
            "installed:0:com.google.android.gm/Main",
            "weblink:1",
        )
        val out = RuleEngine.materialize(FolderRule.Manual, members, catalog, usage = null, nowMillis = now)
        assertEquals(2, out.size)
        assertTrue(out.any { it.appKey == "installed:0:com.google.android.gm/Main" })
        assertTrue(out.any { it.appKey == "weblink:1" })
    }

    @Test
    fun `manual rule ignores allApps when membership set is empty`() {
        val out = RuleEngine.materialize(FolderRule.Manual, emptySet(), catalog, usage = null, nowMillis = now)
        assertTrue(out.isEmpty())
    }

    // ── PackagePrefix ───────────────────────────────────────────────────────

    @Test
    fun `package prefix matches multiple apps`() {
        val out = RuleEngine.materialize(
            FolderRule.PackagePrefix("com.google."),
            emptySet(), catalog, usage = null, nowMillis = now,
        )
        assertEquals(2, out.size)
        assertTrue(out.all { it.packageName.startsWith("com.google.") })
    }

    @Test
    fun `package prefix returns empty when nothing matches`() {
        val out = RuleEngine.materialize(
            FolderRule.PackagePrefix("io.nowhere."),
            emptySet(), catalog, usage = null, nowMillis = now,
        )
        assertTrue(out.isEmpty())
    }

    // ── RecentlyInstalled ───────────────────────────────────────────────────

    @Test
    fun `recently installed picks up apps within window and excludes web links`() {
        val out = RuleEngine.materialize(
            FolderRule.RecentlyInstalled(7),
            emptySet(), catalog, usage = null, nowMillis = now,
        )
        assertEquals(1, out.size)
        assertEquals("com.acme.note", out.single().packageName)
    }

    @Test
    fun `recently installed with zero days returns nothing`() {
        val out = RuleEngine.materialize(
            FolderRule.RecentlyInstalled(0),
            emptySet(), catalog, usage = null, nowMillis = now,
        )
        // cutoff equals now; only entries with firstInstallTime >= now pass.
        assertTrue(out.isEmpty())
    }

    // ── WebApkOnly ──────────────────────────────────────────────────────────

    @Test
    fun `web apk only returns just chromium webapks`() {
        val out = RuleEngine.materialize(
            FolderRule.WebApkOnly,
            emptySet(), catalog, usage = null, nowMillis = now,
        )
        assertEquals(1, out.size)
        assertTrue(out.single().isWebApk)
    }

    // ── UnusedSinceDays ─────────────────────────────────────────────────────

    @Test
    fun `unused since days returns empty when usage is null`() {
        val out = RuleEngine.materialize(
            FolderRule.UnusedSinceDays(30),
            emptySet(), catalog, usage = null, nowMillis = now,
        )
        assertTrue(out.isEmpty())
    }

    @Test
    fun `unused since days excludes recently used and web links`() {
        val usage = listOf(
            UsageEntry("com.google.android.gm", lastUsedMillis = now - 2 * dayMs),
            UsageEntry("com.acme.note", lastUsedMillis = now - 60 * dayMs),
        )
        val out = RuleEngine.materialize(
            FolderRule.UnusedSinceDays(30),
            emptySet(), catalog, usage = usage, nowMillis = now,
        )
        // Recently used: gm (2d ago) -> excluded.
        // Not recently used: maps (no entry -> unused), note (60d ago -> unused), webapk (no entry -> unused).
        // Web links: excluded by rule.
        val pkgs = out.map { it.packageName }.toSet()
        assertTrue("expected maps in result, got $pkgs", "com.google.maps" in pkgs)
        assertTrue("expected note in result, got $pkgs", "com.acme.note" in pkgs)
        assertTrue(
            "expected webapk in result, got $pkgs",
            "org.chromium.webapk.HN" in pkgs,
        )
        assertTrue("gm should be excluded, got $pkgs", "com.google.android.gm" !in pkgs)
        assertTrue("weblink should be excluded, got $pkgs", "weblink" !in pkgs)
    }

    // ── Codec roundtrip ─────────────────────────────────────────────────────

    @Test
    fun `RuleCodec roundtrips every variant`() {
        val rules = listOf(
            FolderRule.Manual,
            FolderRule.PackagePrefix("com.google."),
            FolderRule.RecentlyInstalled(7),
            FolderRule.WebApkOnly,
            FolderRule.UnusedSinceDays(30),
        )
        for (rule in rules) {
            val encoded = RuleCodec.encode(rule)
            val decoded = RuleCodec.decode(encoded)
            assertEquals("roundtrip mismatch for $rule (encoded=$encoded)", rule, decoded)
        }
    }

    @Test
    fun `RuleCodec returns null on garbage input`() {
        assertEquals(null, RuleCodec.decode("not-json"))
        assertEquals(null, RuleCodec.decode("""{"type":"unknown_kind"}"""))
    }
}
