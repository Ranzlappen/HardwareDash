# HardwareDash — Claude notes

Android app, Kotlin + Jetpack Compose + Hilt + Room. minSdk 29, targetSdk 35,
Java/Kotlin target 17.

## Build verification

There is no Android SDK in the local container, so `./gradlew compileDebugKotlin`
won't run here — CI catches build errors. Be extra careful about the things
that wouldn't show up in a local syntax check:

### Kotlin visibility — internal must not leak through public API

The Kotlin compiler rejects this with
`'public' function exposes its 'internal' return type containing declaration X`.
Common ways this happens in this repo:

- A `public` function (default) returns or accepts an `internal` type / nested
  type (`internal object Foo { data class Bar(...) }` referenced by a public
  signature).
- A `public class` has a `public` member that touches an `internal` type in its
  signature (parameter, return type, or property type).

Rules of thumb when writing new code:

1. If a type is `internal` (or nested in an `internal` object/class), every
   public function or property that mentions it in its **signature** must also
   be `internal`. Bodies are fine.
2. Prefer `internal` for module-private helpers (codecs, framing, wire format)
   and keep the public API of a feature small and stable.
3. After adding new files, scan for `^internal (object|class|interface)` and
   confirm every consumer of those types is also `internal`, OR widen the type
   to `public`.

### Other things that won't show up locally

- `compileSdk = 35` constants (e.g. `Context.RECEIVER_NOT_EXPORTED`, API 33+)
  must be guarded with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`.
  Do not use `VERSION_CODES.S` (= 31) as a guard for API-33-only APIs.
- `usb-serial-for-android` exposes `setDTR/getDTR/setRTS/getRTS`. Kotlin maps
  those to property `DTR`/`RTS` (uppercase), not `dtr`/`rts`. Call the methods
  explicitly to avoid surprises.
- `return@OutlinedButton` / `return@Button` for lambdas passed as **named**
  arguments (e.g. `OutlinedButton(onClick = { … })`) is unreliable — the label
  resolves to the parameter name, not the function. Restructure with `if/else`
  instead of early-returning.
- Compose `@Composable val foo: Foo @Composable get()` accessors must be
  evaluated inside a `@Composable` function. Capture the resolved object into a
  local once and read its plain `val`s from non-composable callbacks.

## Layout

- Settings: `app/src/main/java/com/gadget/ui/screens/SettingsScreen.kt`
- Radios (Sub-GHz / IR / NFC / WiFi / Cell): `…/ui/screens/RadiosScreen.kt`
- Backup: `…/backup/BackupManager.kt` — ZIP of Room DB + `shared_prefs/*.xml` +
  `datastore/*`. WAL must be checkpointed via `query("PRAGMA wal_checkpoint(FULL)")`,
  not `execSQL` — `execSQL` rejects statements that return rows.
- Flipper Zero connection: `…/flipper/` (USB CDC-ACM and BLE GATT) + `…/flipper/rpc/`
  (hand-rolled minimal protobuf encoder; field numbers track flipperzero-protobuf ≥0.80).
- Localized strings: single source of truth at `…/localization/Strings.kt`,
  `m(lang, en, de, es, fr)` helper. Add new feature blocks following the
  `Backup` / `Flipper` pattern.
- Hilt: `@HiltAndroidApp` on `GadgetApplication`; non-Composable Compose
  consumers reach singletons via `EntryPointAccessors.fromApplication(...)`
  (see the `BackupManagerEntryPoint` / `FlipperManagerEntryPoint` examples).
