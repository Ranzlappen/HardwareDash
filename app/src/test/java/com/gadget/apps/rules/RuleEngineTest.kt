package com.gadget.apps.rules

import com.gadget.data.db.apps.AppRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        onExternal: Boolean = false,
        system: Boolean = false,
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
        isOnExternalStorage = onExternal,
        isSystemApp = system,
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
        installed(
            "installed:0:com.google.android.gm/Main",
            "com.google.android.gm",
            system = true,
        ),
        installed(
            "installed:0:com.google.maps/Main",
            "com.google.maps",
        ),
        installed(
            "installed:0:com.acme.note/Main",
            "com.acme.note",
            installedAt = now - 2 * dayMs,
            onExternal = true,
        ),
        installed(
            "installed:0:org.chromium.webapk.HN/Main",
            "org.chromium.webapk.HN",
            webApk = true,
        ),
        webLink(1L, "Hacker News"),
    )

    private fun ruleSet(vararg rules: FolderRule) = FolderRuleSet(rules.toList())

    // ── Manual / empty rule set ─────────────────────────────────────────────

    @Test
    fun `empty rule set returns only manual entries`() {
        val members = setOf(
            "installed:0:com.google.android.gm/Main",
            "weblink:1",
        )
        val out = RuleEngine.materialize(FolderRuleSet(), members, catalog, nowMillis = now)
        assertEquals(2, out.size)
        assertTrue(out.any { it.appKey == "installed:0:com.google.android.gm/Main" })
        assertTrue(out.any { it.appKey == "weblink:1" })
    }

    @Test
    fun `empty rule set with no manual entries yields nothing`() {
        val out = RuleEngine.materialize(FolderRuleSet(), emptySet(), catalog, nowMillis = now)
        assertTrue(out.isEmpty())
    }

    // ── PackagePrefix ───────────────────────────────────────────────────────

    @Test
    fun `package prefix matches multiple apps`() {
        val out = RuleEngine.materialize(
            ruleSet(FolderRule.PackagePrefix("com.google.")),
            emptySet(), catalog, nowMillis = now,
        )
        assertEquals(2, out.size)
        assertTrue(out.all { it.packageName.startsWith("com.google.") })
    }

    // ── RecentlyInstalled ───────────────────────────────────────────────────

    @Test
    fun `recently installed picks up apps within window and excludes web links`() {
        val out = RuleEngine.materialize(
            ruleSet(FolderRule.RecentlyInstalled(7)),
            emptySet(), catalog, nowMillis = now,
        )
        assertEquals(1, out.size)
        assertEquals("com.acme.note", out.single().packageName)
    }

    // ── WebApkOnly ──────────────────────────────────────────────────────────

    @Test
    fun `web apk only returns just chromium webapks`() {
        val out = RuleEngine.materialize(
            ruleSet(FolderRule.WebApkOnly),
            emptySet(), catalog, nowMillis = now,
        )
        assertEquals(1, out.size)
        assertTrue(out.single().isWebApk)
    }

    // ── UnusedSinceDays ─────────────────────────────────────────────────────

    @Test
    fun `unused since days returns empty when usage is null`() {
        val out = RuleEngine.materialize(
            ruleSet(FolderRule.UnusedSinceDays(30)),
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
            ruleSet(FolderRule.UnusedSinceDays(30)),
            emptySet(), catalog, usage = usage, nowMillis = now,
        )
        val pkgs = out.map { it.packageName }.toSet()
        assertTrue(pkgs.contains("com.google.maps"))
        assertTrue(pkgs.contains("com.acme.note"))
        assertTrue(pkgs.contains("org.chromium.webapk.HN"))
        assertFalse(pkgs.contains("com.google.android.gm"))
        assertFalse(pkgs.contains("weblink"))
    }

    // ── Storage / system flags ──────────────────────────────────────────────

    @Test
    fun `on internal storage excludes external + web links`() {
        val out = RuleEngine.materialize(
            ruleSet(FolderRule.OnInternalStorage),
            emptySet(), catalog, nowMillis = now,
        )
        val pkgs = out.map { it.packageName }.toSet()
        assertFalse(pkgs.contains("com.acme.note")) // external
        assertFalse(pkgs.contains("weblink"))
        assertTrue(pkgs.contains("com.google.android.gm"))
    }

    @Test
    fun `on external storage returns only the SD-card app`() {
        val out = RuleEngine.materialize(
            ruleSet(FolderRule.OnExternalStorage),
            emptySet(), catalog, nowMillis = now,
        )
        assertEquals(1, out.size)
        assertEquals("com.acme.note", out.single().packageName)
    }

    @Test
    fun `system apps and user apps partition the installed catalog`() {
        val systemOut = RuleEngine.materialize(
            ruleSet(FolderRule.SystemApps), emptySet(), catalog, nowMillis = now,
        ).map { it.packageName }.toSet()
        val userOut = RuleEngine.materialize(
            ruleSet(FolderRule.UserApps), emptySet(), catalog, nowMillis = now,
        ).map { it.packageName }.toSet()

        assertTrue(systemOut.contains("com.google.android.gm"))
        assertFalse(userOut.contains("com.google.android.gm"))
        assertTrue(userOut.contains("com.google.maps"))
        assertFalse(systemOut.contains("com.google.maps"))
    }

    // ── Multi-rule (set union) ──────────────────────────────────────────────

    @Test
    fun `multi-rule unions matches and dedupes`() {
        val out = RuleEngine.materialize(
            ruleSet(
                FolderRule.PackagePrefix("com.google."),
                FolderRule.RecentlyInstalled(7),
            ),
            emptySet(), catalog, nowMillis = now,
        )
        val pkgs = out.map { it.packageName }.toSet()
        // gm + maps (PackagePrefix), note (RecentlyInstalled).
        assertEquals(3, out.size)
        assertTrue(pkgs.contains("com.google.android.gm"))
        assertTrue(pkgs.contains("com.google.maps"))
        assertTrue(pkgs.contains("com.acme.note"))
    }

    @Test
    fun `manual entries are unioned with rule matches`() {
        val out = RuleEngine.materialize(
            ruleSet(FolderRule.RecentlyInstalled(7)),
            manualMembership = setOf("weblink:1"),
            allApps = catalog,
            nowMillis = now,
        )
        val keys = out.map { it.appKey }.toSet()
        assertTrue(keys.contains("weblink:1")) // manual
        assertTrue(keys.contains("installed:0:com.acme.note/Main")) // rule
    }

    // ── Codec roundtrip ─────────────────────────────────────────────────────

    @Test
    fun `RuleCodec roundtrips every variant`() {
        val sets = listOf(
            FolderRuleSet(),
            ruleSet(FolderRule.PackagePrefix("com.google.")),
            ruleSet(FolderRule.RecentlyInstalled(7)),
            ruleSet(FolderRule.WebApkOnly),
            ruleSet(FolderRule.UnusedSinceDays(30)),
            ruleSet(FolderRule.OnInternalStorage),
            ruleSet(FolderRule.OnExternalStorage),
            ruleSet(FolderRule.SystemApps),
            ruleSet(FolderRule.UserApps),
            ruleSet(
                FolderRule.PackagePrefix("com.google."),
                FolderRule.RecentlyInstalled(14),
            ),
        )
        for (set in sets) {
            val encoded = RuleCodec.encode(set)
            val decoded = RuleCodec.decode(encoded)
            assertEquals("roundtrip mismatch for $set (encoded=$encoded)", set, decoded)
        }
    }

    @Test
    fun `RuleCodec decodes legacy single-rule JSON`() {
        val legacy = """{"type":"package_prefix","prefix":"com.google."}"""
        val decoded = RuleCodec.decode(legacy)
        assertEquals(1, decoded.rules.size)
        val rule = decoded.rules.single()
        assertTrue(rule is FolderRule.PackagePrefix)
        assertEquals("com.google.", (rule as FolderRule.PackagePrefix).prefix)
    }

    @Test
    fun `RuleCodec maps legacy manual JSON to empty set`() {
        val legacy = """{"type":"manual"}"""
        val decoded = RuleCodec.decode(legacy)
        assertTrue(decoded.rules.isEmpty())
    }

    @Test
    fun `RuleCodec returns empty set on garbage input`() {
        assertEquals(FolderRuleSet(), RuleCodec.decode("not-json"))
        assertEquals(FolderRuleSet(), RuleCodec.decode("""{"type":"unknown_kind"}"""))
    }
}
