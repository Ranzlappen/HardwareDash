# Complete Android Permissions Reference

Covers every Android permission an app can declare, grouped by what's actually grantable on an **unrooted** device versus what requires **root / system signature / privileged status**. All grant scopes for each category included at the bottom.

> **Note on totals:** Android's `Manifest.permission` class lists ~250 public constants as of API 35/36 (Android 15/16). On top of those there are dozens more `@hide` / OEM / AOSP-internal permissions that only system or root-level processes can hold. This list is exhaustive of the public surface and includes the most relevant hidden ones.

---

## 1. PROTECTION LEVELS (the single most important concept)

Every permission has a `protectionLevel`. This is what determines whether root is needed.

| Level | Granted to | Root needed? |
|---|---|---|
| `normal` | Any app, automatically at install | No |
| `dangerous` (runtime) | Any app, after user prompt | No |
| `appop` (special) | Any app, after user toggle in Settings | No |
| `signature` | Apps signed with the **same cert** as the declarer (usually the platform key) | Effectively yes — you need to be a system app or have signed with `platform.x509.pem` |
| `signature\|privileged` | Apps in `/system/priv-app/` allowlisted in `privapp-permissions.xml` | Yes — needs system image access |
| `signature\|system` (legacy, pre-O) | Apps installed in `/system` partition | Yes |
| `internal` / `role` / `installer` / `verifier` / `preinstalled` | Specific OS roles only | Yes |
| `development` | Granted via `adb pm grant` only | No (just adb), but acts root-adjacent |
| `oem` | OEM partition apps only | Yes |
| `vendor` | Vendor partition apps only | Yes |
| `companion` | Companion device manager only | No (special pairing flow) |

Modifiers can stack (e.g. `signature|privileged|appop|development`).

---

## 2. UNROOTED — Permissions any app can request

### 2.1 Install-time / Normal permissions (auto-granted, no prompt, no scope choice)

Granted automatically when the app is installed. User can see them but cannot toggle.

**Network & Connectivity**
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `ACCESS_WIFI_STATE`
- `CHANGE_WIFI_STATE`
- `CHANGE_NETWORK_STATE`
- `CHANGE_WIFI_MULTICAST_STATE`
- `NFC`
- `NFC_TRANSACTION_EVENT`
- `NFC_PREFERRED_PAYMENT_INFO`
- `TRANSMIT_IR`
- `BROADCAST_STICKY`
- `REQUEST_COMPANION_RUN_IN_BACKGROUND`
- `REQUEST_COMPANION_USE_DATA_IN_BACKGROUND`
- `REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND`
- `USE_FULL_SCREEN_INTENT`

**System interaction**
- `WAKE_LOCK`
- `VIBRATE`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_CAMERA`
- `FOREGROUND_SERVICE_CONNECTED_DEVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `FOREGROUND_SERVICE_HEALTH`
- `FOREGROUND_SERVICE_LOCATION`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `FOREGROUND_SERVICE_MEDIA_PROJECTION`
- `FOREGROUND_SERVICE_MICROPHONE`
- `FOREGROUND_SERVICE_PHONE_CALL`
- `FOREGROUND_SERVICE_REMOTE_MESSAGING`
- `FOREGROUND_SERVICE_SPECIAL_USE`
- `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`
- `FOREGROUND_SERVICE_FILE_MANAGEMENT` (Android 16)
- `FOREGROUND_SERVICE_MEDIA_PROCESSING` (Android 15)
- `RECEIVE_BOOT_COMPLETED`
- `REORDER_TASKS`
- `KILL_BACKGROUND_PROCESSES`
- `EXPAND_STATUS_BAR`
- `GET_TASKS` *(deprecated)*
- `GET_PACKAGE_SIZE`
- `MODIFY_AUDIO_SETTINGS`
- `MANAGE_OWN_CALLS`
- `BROADCAST_PACKAGE_REMOVED` (signature too — usually system)
- `SET_WALLPAPER`
- `SET_WALLPAPER_HINTS`
- `INSTALL_SHORTCUT` *(legacy launcher API)*
- `UNINSTALL_SHORTCUT` *(deprecated)*

**Bluetooth (legacy)**
- `BLUETOOTH` *(API ≤30)*
- `BLUETOOTH_ADMIN` *(API ≤30)*

**Sync & Accounts**
- `READ_SYNC_SETTINGS`
- `WRITE_SYNC_SETTINGS`
- `READ_SYNC_STATS`
- `AUTHENTICATE_ACCOUNTS` *(deprecated)*
- `MANAGE_ACCOUNTS` *(deprecated)*
- `USE_CREDENTIALS` *(deprecated)*
- `GET_ACCOUNTS_PRIVILEGED` (signature)
- `CREDENTIAL_MANAGER_SET_ORIGIN`
- `CREDENTIAL_MANAGER_QUERY_CANDIDATE_CREDENTIALS`

**Notifications & UI**
- `ACCESS_NOTIFICATION_POLICY`
- `DISABLE_KEYGUARD`
- `SUBSCRIBED_FEEDS_READ` *(deprecated)*
- `SUBSCRIBED_FEEDS_WRITE` *(deprecated)*
- `READ_PROFILE` *(deprecated)*
- `WRITE_PROFILE` *(deprecated)*

**Misc**
- `USE_FINGERPRINT` *(deprecated, replaced by USE_BIOMETRIC)*
- `USE_BIOMETRIC`
- `BROADCAST_BADGE` *(launcher integration; on some OEMs)*
- `READ_USER_DICTIONARY` *(deprecated/normal)*
- `WRITE_USER_DICTIONARY` *(deprecated)*
- `BIND_VPN_SERVICE` (technically signature too, used by VPN apps via `prepare()` flow)
- `QUERY_ALL_PACKAGES` *(was install-time, now restricted by Play policy)*
- `HIGH_SAMPLING_RATE_SENSORS`
- `UPDATE_PACKAGES_WITHOUT_USER_ACTION`
- `ENFORCE_UPDATE_OWNERSHIP` (Android 14)
- `RUN_USER_INITIATED_JOBS` (Android 14)
- `DETECT_SCREEN_CAPTURE` (Android 14)
- `DETECT_SCREEN_RECORDING` (Android 15)

---

### 2.2 Runtime / Dangerous permissions (user prompt at runtime)

These show a system dialog. Possible **scopes** vary by permission — see §4.

**Location**
- `ACCESS_FINE_LOCATION` — scope: Precise / Approximate, While in use / One-time / Don't allow
- `ACCESS_COARSE_LOCATION` — scope: Approximate, While in use / One-time / Don't allow
- `ACCESS_BACKGROUND_LOCATION` — scope: Allow all the time / Allow only while using / Don't allow
  *(Android 11+ requires separate request; system redirects to Settings)*

**Camera**
- `CAMERA` — scope: While in use / One-time / Don't allow

**Microphone**
- `RECORD_AUDIO` — scope: While in use / One-time / Don't allow
- `CAPTURE_AUDIO_OUTPUT` *(privileged on most builds)*

**Contacts**
- `READ_CONTACTS`
- `WRITE_CONTACTS`
- `GET_ACCOUNTS`

**Phone**
- `READ_PHONE_STATE`
- `READ_PHONE_NUMBERS`
- `CALL_PHONE`
- `READ_CALL_LOG`
- `WRITE_CALL_LOG`
- `ADD_VOICEMAIL`
- `USE_SIP`
- `ANSWER_PHONE_CALLS`
- `ACCEPT_HANDOVER`

**SMS / Messaging**
- `SEND_SMS`
- `RECEIVE_SMS`
- `READ_SMS`
- `RECEIVE_WAP_PUSH`
- `RECEIVE_MMS`
- `READ_CELL_BROADCASTS`

**Calendar**
- `READ_CALENDAR`
- `WRITE_CALENDAR`

**Storage (legacy + scoped)**
- `READ_EXTERNAL_STORAGE` *(neutered on Android 13+, replaced by granular media)*
- `WRITE_EXTERNAL_STORAGE` *(no-op on Android 11+ for new apps)*
- `READ_MEDIA_IMAGES` (Android 13+) — scope: Allow all / Allow selected (Android 14+) / Don't allow
- `READ_MEDIA_VIDEO` (Android 13+) — scope: Allow all / Allow selected (Android 14+) / Don't allow
- `READ_MEDIA_AUDIO` (Android 13+)
- `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+) — replaces "selected photos" UI; explicit per-pick

**Sensors**
- `BODY_SENSORS` *(deprecated in API 35 in favor of HEALTH_CONNECT)*
- `BODY_SENSORS_BACKGROUND` (Android 13+) — scope: must enable from Settings (no "Allow all the time" in dialog)
- `ACTIVITY_RECOGNITION` (Android 10+)

**Bluetooth (Android 12+)**
- `BLUETOOTH_SCAN` — flag `usesPermissionFlags="neverForLocation"` to avoid location grant
- `BLUETOOTH_ADVERTISE`
- `BLUETOOTH_CONNECT`

**Wi-Fi (Android 13+)**
- `NEARBY_WIFI_DEVICES`

**UWB (Android 12+)**
- `UWB_RANGING`

**Notifications (Android 13+)**
- `POST_NOTIFICATIONS` — scope: Allow / Don't allow

**Health Connect (Android 14+, replaces BODY_SENSORS gradually)**
- `health.READ_HEART_RATE`
- `health.READ_STEPS`
- `health.READ_DISTANCE`
- `health.READ_ACTIVE_CALORIES_BURNED`
- `health.READ_TOTAL_CALORIES_BURNED`
- `health.READ_OXYGEN_SATURATION`
- `health.READ_BLOOD_PRESSURE`
- `health.READ_BLOOD_GLUCOSE`
- `health.READ_BODY_TEMPERATURE`
- `health.READ_SKIN_TEMPERATURE`
- `health.READ_SLEEP`
- `health.READ_EXERCISE`
- `health.READ_HYDRATION`
- `health.READ_NUTRITION`
- `health.READ_MENSTRUATION`
- `health.READ_BODY_FAT`
- `health.READ_LEAN_BODY_MASS`
- `health.READ_BONE_MASS`
- `health.READ_HEIGHT`
- `health.READ_WEIGHT`
- `health.READ_VO2_MAX`
- `health.READ_RESPIRATORY_RATE`
- `health.READ_RESTING_HEART_RATE`
- `health.READ_HEART_RATE_VARIABILITY`
- `health.READ_BASAL_BODY_TEMPERATURE`
- `health.READ_BASAL_METABOLIC_RATE`
- `health.READ_CERVICAL_MUCUS`
- `health.READ_INTERMENSTRUAL_BLEEDING`
- `health.READ_OVULATION_TEST`
- `health.READ_SEXUAL_ACTIVITY`
- `health.READ_FLOORS_CLIMBED`
- `health.READ_ELEVATION_GAINED`
- `health.READ_POWER`
- `health.READ_SPEED`
- `health.READ_WHEELCHAIR_PUSHES`
- `health.READ_HEALTH_DATA_HISTORY` (Android 15+)
- `health.READ_HEALTH_DATA_IN_BACKGROUND` (Android 15+)
- `health.READ_PLANNED_EXERCISE`
- `health.READ_TRAINING_PLANS`
- `health.READ_MINDFULNESS` (Android 16)
- All of the above with `WRITE_*` equivalents.

---

### 2.3 Special permissions (`appop` — toggle in Settings → Special app access)

The user has to leave the app, navigate to Settings, find your app in a list, and flip a switch. Cannot be granted by a normal runtime dialog.

- `SYSTEM_ALERT_WINDOW` — "Display over other apps". Scope: On / Off (per-app).
- `WRITE_SETTINGS` — "Modify system settings". Scope: On / Off.
- `REQUEST_INSTALL_PACKAGES` — "Install unknown apps". Scope: Per-source app on/off.
- `MANAGE_EXTERNAL_STORAGE` — "All files access". Scope: On / Off (Play policy restricted).
- `SCHEDULE_EXACT_ALARM` — User can revoke; Android 14+ default-denied for new installs. Scope: On / Off.
- `USE_EXACT_ALARM` — install-time but Play-restricted to alarm/clock/calendar apps. No toggle.
- `PACKAGE_USAGE_STATS` — "Usage access". Scope: On / Off.
- `BIND_NOTIFICATION_LISTENER_SERVICE` — "Notification access". Scope: On / Off (per-listener).
- `BIND_ACCESSIBILITY_SERVICE` — "Accessibility". Scope: On / Off + the "Restricted setting" warning on Android 13+.
- `BIND_DEVICE_ADMIN` — "Device admin apps". Scope: Activated / Deactivated.
- `BIND_VPN_SERVICE` — "VPN" prepare dialog. Scope: One-time approve + "Always-on VPN" toggle in Settings.
- `BIND_AUTOFILL_SERVICE` — "Autofill service". Scope: Selected as default service or not.
- `BIND_INPUT_METHOD` — IME selection in Settings.
- `BIND_TEXT_SERVICE` — Spell checker in Settings.
- `BIND_WALLPAPER` — Live wallpaper picker.
- `BIND_QUICK_SETTINGS_TILE` — User adds tile manually.
- `BIND_TV_INPUT` — TV input source picker.
- `BIND_VOICE_INTERACTION` — Default assistant role.
- `BIND_PRINT_SERVICE` — Default print service.
- `BIND_NFC_SERVICE` / `BIND_QUICK_ACCESS_WALLET_SERVICE` — Default payment / wallet.
- `BIND_CARRIER_MESSAGING_SERVICE`
- `BIND_CARRIER_SERVICES`
- `BIND_CHOOSER_TARGET_SERVICE` *(deprecated)*
- `BIND_DREAM_SERVICE` — Screensaver picker.
- `BIND_INCALL_SERVICE`
- `BIND_SCREENING_SERVICE` — Call screening default.
- `BIND_TELECOM_CONNECTION_SERVICE`
- `BIND_VISUAL_VOICEMAIL_SERVICE`
- `BIND_CONDITION_PROVIDER_SERVICE`
- `BIND_MIDI_DEVICE_SERVICE`
- `ACCESS_NOTIFICATION_POLICY` — "Do Not Disturb access".
- `MANAGE_MEDIA` (Android 12+) — modify/delete media without per-file confirmation. Scope: On / Off.
- `MANAGE_ONGOING_CALLS`
- `LOADER_USAGE_STATS`
- `LOCATION_HARDWARE` (signature, but listed for completeness)
- `REQUEST_DELETE_PACKAGES` — install-time, user-confirmed each time
- `BIND_CREDENTIAL_PROVIDER_SERVICE` (Android 14)
- `RUN_USER_INITIATED_JOBS` (visible in some UIs as "background activity")
- `READ_NEARBY_STREAMING_POLICY`

**Roles** (Android 10+ replaced some appops with "default app" roles):
- `ROLE_HOME` (default launcher)
- `ROLE_BROWSER`
- `ROLE_DIALER`
- `ROLE_SMS`
- `ROLE_EMERGENCY`
- `ROLE_CALL_REDIRECTION`
- `ROLE_CALL_SCREENING`
- `ROLE_ASSISTANT`
- `ROLE_NOTES` (Android 14)
- `ROLE_WALLET` (Android 15)
- `ROLE_RESERVED_FOR_TESTING_*`

---

### 2.4 Companion-device permissions (one-time pairing)

Granted via `CompanionDeviceManager` association flow — not a generic runtime prompt.
- `REQUEST_COMPANION_PROFILE_WATCH`
- `REQUEST_COMPANION_PROFILE_GLASSES` (Android 14)
- `REQUEST_COMPANION_PROFILE_APP_STREAMING`
- `REQUEST_COMPANION_PROFILE_AUTOMOTIVE_PROJECTION`
- `REQUEST_COMPANION_PROFILE_COMPUTER` (Android 14)
- `REQUEST_COMPANION_PROFILE_NEARBY_DEVICE_STREAMING`
- `REQUEST_COMPANION_SELF_MANAGED`

---

## 3. ROOTED / PRIVILEGED — Need root, system signature, or AOSP build

These have `protectionLevel="signature"`, `signature|privileged`, `signature|system`, or are flagged `@hide`. A normal third-party app cannot get them granted on a stock device, regardless of how aggressively the user clicks "Allow". You either need:
- Root (`su`) to use `pm grant` with arbitrary permissions, OR
- The platform signing key, OR
- Placement in `/system/priv-app/` + an entry in `privapp-permissions.xml`, OR
- An OEM/vendor partition with matching `oem`/`vendor` flag.

Some can also be granted via `adb pm grant` without root if they carry the `development` flag — these are listed.

### 3.1 Telephony (carrier / dialer / system)
- `MODIFY_PHONE_STATE`
- `CALL_PRIVILEGED`
- `READ_PRIVILEGED_PHONE_STATE`
- `READ_PRECISE_PHONE_STATE`
- `WRITE_SMS` *(deprecated, system only)*
- `BROADCAST_SMS`
- `BROADCAST_WAP_PUSH`
- `SEND_RESPOND_VIA_MESSAGE`
- `SEND_SMS_NO_CONFIRMATION`
- `RECEIVE_EMERGENCY_BROADCAST`
- `READ_BLOCKED_NUMBERS`
- `WRITE_BLOCKED_NUMBERS`
- `MANAGE_USB`
- `MANAGE_SIM_SUBSCRIPTIONS`
- `BIND_TELEPHONY_DATA_SERVICE`
- `BIND_TELEPHONY_NETWORK_SERVICE`
- `BIND_IMS_SERVICE`
- `BIND_CARRIER_MESSAGING_CLIENT_SERVICE`
- `MODIFY_CELL_BROADCASTS`
- `INTERACT_ACROSS_USERS`
- `INTERACT_ACROSS_USERS_FULL`
- `INTERACT_ACROSS_PROFILES`
- `START_ACTIVITIES_FROM_BACKGROUND`
- `BIND_CELL_BROADCAST_SERVICE`
- `READ_CARRIER_APP_INFO`
- `BIND_GBA_SERVICE`
- `WRITE_EMBEDDED_SUBSCRIPTIONS`
- `BIND_EUICC_SERVICE`
- `PERFORM_IMS_SINGLE_REGISTRATION`
- `ACCESS_LAST_KNOWN_CELL_ID`
- `ACCESS_RCS_USER_CAPABILITY_EXCHANGE`
- `BIND_DOMAIN_SELECTION_SERVICE`

### 3.2 Settings, packages, and system control
- `WRITE_SECURE_SETTINGS` *(grantable via `adb shell pm grant ... WRITE_SECURE_SETTINGS` — `development` flag)*
- `WRITE_GSERVICES`
- `WRITE_APN_SETTINGS`
- `INSTALL_PACKAGES`
- `DELETE_PACKAGES`
- `CLEAR_APP_USER_DATA`
- `CLEAR_APP_CACHE`
- `MANAGE_APP_OPS_MODES`
- `GET_APP_OPS_STATS`
- `UPDATE_APP_OPS_STATS`
- `MOVE_PACKAGE`
- `INSTALL_GRANT_RUNTIME_PERMISSIONS`
- `REVOKE_RUNTIME_PERMISSIONS`
- `OBSERVE_GRANT_REVOKE_PERMISSIONS`
- `ADJUST_RUNTIME_PERMISSIONS_POLICY`
- `WHITELIST_RESTRICTED_PERMISSIONS`
- `PACKAGE_VERIFICATION_AGENT`
- `BIND_PACKAGE_VERIFIER`
- `INSTALL_PACKAGE_UPDATES`
- `INSTALL_SELF_UPDATES`
- `INSTALL_TEST_ONLY_PACKAGE`
- `KEEP_UNINSTALLED_PACKAGES`
- `SUSPEND_APPS`
- `OBSERVE_APP_USAGE`
- `BIND_RESUME_ON_REBOOT_SERVICE`
- `MOUNT_UNMOUNT_FILESYSTEMS`
- `MOUNT_FORMAT_FILESYSTEMS`
- `WRITE_MEDIA_STORAGE`
- `INTERNAL_SYSTEM_WINDOW`
- `SYSTEM_APPLICATION_OVERLAY`
- `HIDE_OVERLAY_WINDOWS`
- `START_VIEW_PERMISSION_USAGE`
- `START_VIEW_APP_FEATURES`
- `MANAGE_ROLE_HOLDERS`
- `OBSERVE_ROLE_HOLDERS`

### 3.3 Power, battery, devices
- `DEVICE_POWER`
- `BATTERY_STATS` *(grantable via `adb pm grant`)*
- `POWER_SAVER`
- `REBOOT`
- `SHUTDOWN`
- `BRIGHTNESS_SLIDER_USAGE`
- `MANAGE_LOW_POWER_STANDBY` (Android 14)
- `STATUS_BAR`
- `STATUS_BAR_SERVICE`
- `CONTROL_DISPLAY_BRIGHTNESS`
- `CONTROL_KEYGUARD`
- `READ_DREAM_STATE`
- `WRITE_DREAM_STATE`
- `DISPATCH_NFC_MESSAGE`
- `MANAGE_USB`
- `ACCESS_MTP`
- `MANAGE_DEVICE_LOCK_STATE`
- `MANAGE_DEVICE_POLICY_*` (huge family — DPC only)

### 3.4 Networking (privileged)
- `CONTROL_VPN`
- `CONNECTIVITY_INTERNAL`
- `CONNECTIVITY_USE_RESTRICTED_NETWORKS`
- `MANAGE_TEST_NETWORKS`
- `NETWORK_SETTINGS`
- `NETWORK_STACK`
- `NETWORK_FACTORY`
- `NETWORK_BYPASS_PRIVATE_DNS`
- `NETWORK_SCAN`
- `NETWORK_SIGNAL_STRENGTH_WAKEUP`
- `NETWORK_STATS_PROVIDER`
- `OBSERVE_NETWORK_POLICY`
- `MANAGE_NETWORK_POLICY`
- `ACCESS_NETWORK_CONDITIONS`
- `RADIO_SCAN_WITHOUT_LOCATION`
- `OVERRIDE_WIFI_CONFIG`
- `MANAGE_WIFI_INTERFACES`
- `MANAGE_WIFI_NETWORK_SELECTION`
- `MANAGE_WIFI_AUTO_JOIN`
- `MANAGE_WIFI_COUNTRY_CODE`
- `READ_WIFI_CREDENTIAL`
- `RESTART_WIFI_SUBSYSTEM`
- `TETHER_PRIVILEGED`
- `MANAGE_USB`

### 3.5 Bluetooth (privileged)
- `BLUETOOTH_PRIVILEGED`
- `BLUETOOTH_MAP`
- `BLUETOOTH_STACK`
- `MANAGE_BLUETOOTH_WHEN_*` (variants)

### 3.6 Display, input, accessibility (signature)
- `READ_FRAME_BUFFER`
- `ACCESS_SURFACE_FLINGER`
- `MAGNIFY_DISPLAY`
- `WRITE_DISPLAY_COLOR_TRANSFORM`
- `CONTROL_DISPLAY_COLOR_TRANSFORMS`
- `CONTROL_DISPLAY_SATURATION`
- `CONFIGURE_DISPLAY_BRIGHTNESS`
- `INJECT_EVENTS` *(can be `adb pm grant` on debug builds)*
- `FILTER_EVENTS`
- `RETRIEVE_WINDOW_TOKEN`
- `RETRIEVE_WINDOW_INFO`
- `FRAME_STATS`
- `SET_ORIENTATION`
- `SET_POINTER_SPEED`
- `SET_KEYBOARD_LAYOUT`
- `MANAGE_ACCESSIBILITY`
- `BIND_ACCESSIBILITY_SHORTCUT_TARGET`

### 3.7 Media projection / screen capture
- `CAPTURE_VIDEO_OUTPUT`
- `CAPTURE_SECURE_VIDEO_OUTPUT`
- `CAPTURE_AUDIO_HOTWORD`
- `CAPTURE_MEDIA_OUTPUT`
- `CAPTURE_TUNER_AUDIO_INPUT`
- `CAPTURE_VOICE_COMMUNICATION_OUTPUT`
- `MEDIA_CONTENT_CONTROL`
- `MEDIA_RESOURCE_OVERRIDE_PID`
- `MANAGE_MEDIA_PROJECTION`
- `MODIFY_AUDIO_ROUTING`
- `RECORD_BACKGROUND_AUDIO`
- `MODIFY_DEFAULT_AUDIO_EFFECTS`
- `BIND_TV_REMOTE_SERVICE`
- `MANAGE_AUDIO_POLICY`
- `MODIFY_PHONE_AUDIO_ROUTING`

### 3.8 Backup / restore / device admin
- `BACKUP`
- `CONFIRM_FULL_BACKUP`
- `BIND_REMOTE_BACKUP_SERVICE`
- `RECOVERY`
- `CRYPT_KEEPER`
- `READ_LOGS` *(grantable via `adb pm grant`)*
- `DUMP` *(grantable via `adb pm grant`)*
- `SET_DEBUG_APP`
- `SET_PROCESS_LIMIT`
- `SET_ALWAYS_FINISH`
- `SET_ANIMATION_SCALE` *(grantable via `adb pm grant`)*
- `SET_TIME`
- `SET_TIME_ZONE`
- `SET_KEYBOARD_LAYOUT`
- `MANAGE_PROFILE_AND_DEVICE_OWNERS`
- `MANAGE_USERS`
- `CREATE_USERS`
- `QUERY_USERS`
- `OVERRIDE_DISPLAY_MODE_REQUESTS`
- `MANAGE_FACTORY_RESET_PROTECTION`
- `MASTER_CLEAR`
- `FORCE_BACK`
- `FORCE_STOP_PACKAGES`
- `FORCE_PERSISTABLE_URI_PERMISSIONS`
- `KILL_UID`
- `STOP_APP_SWITCHES`
- `RESTART_PACKAGES` *(deprecated)*

### 3.9 Location & sensors (privileged)
- `LOCATION_HARDWARE`
- `INSTALL_LOCATION_PROVIDER`
- `ACCESS_MOCK_LOCATION` *(grantable via `adb pm grant`, also dev option)*
- `ACCESS_LOCATION_EXTRA_COMMANDS`
- `CONTROL_LOCATION_UPDATES`
- `BIND_LOCATION_PROVIDER`
- `READ_DEVICE_CONFIG`
- `WRITE_DEVICE_CONFIG`
- `MANAGE_SENSOR_PRIVACY`
- `OBSERVE_SENSOR_PRIVACY`
- `BIND_SENSOR_SERVICE`

### 3.10 Camera / biometrics (privileged)
- `MANAGE_CAMERA`
- `SYSTEM_CAMERA`
- `CAMERA_OPEN_CLOSE_LISTENER`
- `CAMERA_DISABLE_TRANSMIT_LED`
- `MANAGE_BIOMETRIC`
- `USE_BIOMETRIC_INTERNAL`
- `RESET_FACE_LOCKOUT`
- `RESET_FINGERPRINT_LOCKOUT`
- `MANAGE_FINGERPRINT`
- `MANAGE_FACE`

### 3.11 Boot, init, kernel
- `BIND_JOB_SERVICE`
- `MANAGE_ACTIVITY_TASKS`
- `MANAGE_ACTIVITY_STACKS`
- `START_ACTIVITY_AS_CALLER`
- `START_ANY_ACTIVITY`
- `START_TASKS_FROM_RECENTS`
- `REMOVE_TASKS`
- `STOP_APP_SWITCHES`
- `READ_FRAME_BUFFER`
- `ACCESS_INSTANT_APPS`
- `VIEW_INSTANT_APPS`
- `WAKE_LOCK_OS`
- `INSTALL_DPC_PACKAGES`
- `MANAGE_PROFILE_AND_DEVICE_OWNERS`
- `LOCK_DEVICE`

### 3.12 Hidden / `@hide` / OEM specific
Hundreds of `@hide`-marked permissions exist; the full set varies per OEM. The most commonly cited:
- `READ_INPUT_STATE` *(deprecated)*
- `BROADCAST_PACKAGE_REMOVED`
- `BROADCAST_PACKAGE_ADDED`
- `BROADCAST_PACKAGE_REPLACED`
- `BROADCAST_PACKAGE_CHANGED`
- `WRITE_GSERVICES`
- `READ_GSERVICES`
- `READ_DREAM_STATE`
- `READ_DREAM_SUPPRESSION`
- `MANAGE_DREAMS`
- `READ_NETWORK_USAGE_HISTORY`
- `MANAGE_NETWORK_POLICY`
- `READ_NETWORK_POLICY`
- `WRITE_NETWORK_POLICY`
- `READ_PRECISE_PHONE_STATE`
- `READ_INSTALL_SESSIONS`
- `READ_SEARCH_INDEXABLES`
- `BIND_PRINT_SPOOLER_SERVICE`
- `BIND_TRUST_AGENT`
- `PROVIDE_TRUST_AGENT`
- `PEERS_MAC_ADDRESS`
- `LOCAL_MAC_ADDRESS`
- `BIND_KEYGUARD_APPWIDGET`
- `BIND_DEVICE_ADMIN`
- `MANAGE_USB`
- `HARDWARE_TEST`
- `DIAGNOSTIC`
- `READ_CHECKIN_PROPERTIES`
- `ACCESS_CHECKIN_PROPERTIES`
- `ACCOUNT_MANAGER`
- `BIND_APPWIDGET`
- `MANAGE_APP_TOKENS`
- `FREEZE_SCREEN`
- `SET_ACTIVITY_WATCHER`
- `SHUTDOWN`
- `STOP_APP_SWITCHES`
- `READ_INPUT_STATE`
- `READ_OWNER_DATA` *(deprecated)*
- `WRITE_OWNER_DATA` *(deprecated)*
- `WRITE_CONTACTS_PRIVILEGED`
- `BIND_NFC_SERVICE`
- `BIND_PRINT_SERVICE`
- `BIND_REMOTEVIEWS`
- `BIND_TEXT_SERVICE`
- `BIND_TV_REMOTE_SERVICE`
- `BIND_VOICE_INTERACTION`
- `MANAGE_VOICE_KEYPHRASES`
- `KEYPHRASE_ENROLLMENT_APPLICATION`
- `MANAGE_SOUND_TRIGGER`
- `CAPTURE_AUDIO_HOTWORD`
- `RECEIVE_DEVICE_CUSTOMIZATION_READY`
- `BIND_RESOLVER_RANKER_SERVICE`
- `BIND_TEXTCLASSIFIER_SERVICE`
- `BIND_CONTENT_CAPTURE_SERVICE`
- `BIND_CONTENT_SUGGESTIONS_SERVICE`
- `BIND_AUGMENTED_AUTOFILL_SERVICE`
- `BIND_INLINE_SUGGESTION_RENDER_SERVICE`
- `BIND_AMBIENT_CONTEXT_DETECTION_SERVICE`
- `BIND_FIELD_CLASSIFICATION_SERVICE`
- `BIND_TRANSLATION_SERVICE`
- `BIND_MUSIC_RECOGNITION_SERVICE`
- `BIND_SMARTSPACE_SERVICE`
- `BIND_ATTENTION_SERVICE`
- `BIND_ROTATION_RESOLVER_SERVICE`
- `BIND_GAME_SERVICE`
- `BIND_COMPANION_DEVICE_SERVICE`
- `BIND_TILE_SERVICE`
- `BIND_TIME_ZONE_PROVIDER_SERVICE`
- `BIND_HOME_CONTROLS`
- `BIND_PEOPLE_SERVICE`
- `BIND_HOTWORD_DETECTION_SERVICE`
- `BIND_VISUAL_QUERY_DETECTION_SERVICE`
- `BIND_REMOTE_LOCKSCREEN_VALIDATION_SERVICE`
- `BIND_FINANCIAL_SMS_SERVICE`
- `BIND_DIRECTORY_SEARCH`
- `BIND_INPUT_FLINGER`
- `ACCESS_INPUT_FLINGER`
- `ACCESS_DRM_CERTIFICATES`
- `ACCESS_KEYGUARD_SECURE_STORAGE`
- `ACCESS_FM_RADIO`
- `ACCESS_BROADCAST_RADIO`
- `ACCESS_VR_MANAGER`
- `ACCESS_VR_STATE`
- `RESTRICTED_VR_ACCESS`
- `ACCESS_AMBIENT_CONTEXT_EVENT`
- `ACCESS_AMBIENT_LIGHT_STATS`
- `ACCESS_BACKGROUND_VOICE_INTERACTION`
- `ACCESS_CACHE_FILESYSTEM`
- `ACCESS_CONTENT_PROVIDERS_EXTERNALLY`
- `ACCESS_DOWNLOAD_MANAGER`
- `ACCESS_DOWNLOAD_MANAGER_ADVANCED`
- `ACCESS_BLUETOOTH_SHARE`
- `ACCESS_ALL_EXTERNAL_STORAGE`
- `ACCESS_SHARED_LIBRARIES`
- `ACCESS_SMARTSPACE`
- `ACCESS_SHORTCUTS`
- `ACCESS_TV_TUNER`
- `ACCESS_TV_DESCRAMBLER`

### 3.13 ADB / development-flag (no root, but `adb pm grant` only)
The most useful "rooted-feeling" capabilities reachable without root:
- `WRITE_SECURE_SETTINGS`
- `READ_LOGS`
- `DUMP`
- `BATTERY_STATS`
- `CHANGE_CONFIGURATION`
- `SET_ANIMATION_SCALE`
- `ACCESS_MOCK_LOCATION`
- `INTERACT_ACROSS_USERS` *(some builds)*
- `PACKAGE_USAGE_STATS` *(also a normal special permission, but ADB grant works)*
- `FORCE_STOP_PACKAGES` *(some builds)*
- `MANAGE_USERS` *(some debug builds)*

### 3.14 True root-only (`su` required, not "permissions" but capabilities)

Strictly speaking these aren't `Manifest.permission` entries — they're Linux-level capabilities reached via the `su` binary or a Magisk module. Listed for completeness because the question asked for "everything imaginable":
- Read/write any file under `/data/` (including `/data/data/<other-app>/`)
- Read/write `/system/`, `/vendor/`, `/product/`, `/system_ext/` partitions
- Modify init.rc, build.prop, SELinux policy
- Hot-patch kernel modules (`insmod`)
- iptables / nftables firewall rules without VPN
- Change UID/GID, run as `system` / `radio` / `bluetooth` / `media` / `shell`
- Direct access to `/dev/block/*` (raw partitions)
- Direct access to `/proc/<pid>/mem` of other apps
- Bypass SELinux enforcement (set permissive)
- Modify `/data/system/packages.xml` and `runtime-permissions.xml` directly
- Use `pm grant` for **any** signature-level permission
- Install `/system/priv-app/` apps without OTA
- Modify `privapp-permissions.xml` to grant signature|privileged permissions to arbitrary apps
- Disable verified boot / dm-verity (with bootloader unlocked)
- Direct framebuffer / DRM access (`/dev/graphics/fb0`, `/dev/dri/*`)
- Read kernel ring buffer (`dmesg`)
- Use `setpriority`/`renice` outside app's own thread group
- Access raw audio devices `/dev/snd/*`
- Mount/unmount any filesystem
- Hot-swap APKs of running system services
- Modify `/efs/` (modem/IMEI partition on Samsung devices)

---

## 4. PERMISSION SCOPES (the "Allow all the time" question)

Different permissions have different runtime dialog options. Here's every scope the Android permission UI can show:

### 4.1 Generic dangerous permission (Camera, Mic, Contacts, etc.)
- **While using the app** — granted only while a foreground activity/service is running
- **Only this time** (one-time) — granted until the app process is killed; revoked on next launch *(Android 11+)*
- **Don't allow** — denied; app must call `shouldShowRequestPermissionRationale()` to know whether to re-prompt
- **Don't allow + Don't ask again** — implicit after 2 denials on Android 11+; only Settings can re-enable

### 4.2 Location (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` / `ACCESS_BACKGROUND_LOCATION`)
- **Allow all the time** — only via Settings on Android 11+; not in the runtime dialog
- **Allow only while using the app**
- **Ask every time** (one-time)
- **Don't allow**
- **Use precise location** toggle (Android 12+) — orthogonal switch: Approximate vs Precise. Granted permission is the same; precision is the scope.

### 4.3 Photos & Media (`READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`)
- **Allow all** — full access to the type
- **Allow limited access / Select photos and videos** *(Android 14+)* — user picks specific items via the photo picker; app gets `READ_MEDIA_VISUAL_USER_SELECTED`
- **Don't allow**

### 4.4 Body sensors (`BODY_SENSORS_BACKGROUND`)
- **Allow** — foreground only via runtime dialog
- **Allow all the time** — only reachable via Settings (no in-dialog option)
- **Don't allow**

### 4.5 Notifications (`POST_NOTIFICATIONS`)
- **Allow**
- **Don't allow**
- (Per-channel toggles in app settings are separate from the manifest permission.)

### 4.6 Special permissions (`appop`)
Always two states only: **On / Off**, set in Settings → Apps → Special app access → *<category>*.

Sub-modes for some:
- `MANAGE_EXTERNAL_STORAGE`: per-app on/off + Play store policy gate
- `SCHEDULE_EXACT_ALARM`: revocable post-Android 14
- `SYSTEM_ALERT_WINDOW`: per-app on/off; auto-granted to apps targeting low SDK
- `BIND_ACCESSIBILITY_SERVICE` / `BIND_NOTIFICATION_LISTENER_SERVICE`: per-service on/off + the "Restricted setting" warning that requires Settings → App info → ⋮ → Allow restricted settings (Android 13+) when the app was sideloaded

### 4.7 Device admin / VPN / etc.
Each presents a custom system dialog ("Activate", "Connect", "Always-on VPN", "Block connections without VPN"). Not modeled as a generic Allow/Deny; uses framework-specific UI.

### 4.8 Self-revocation
Android 13+: app can revoke its own runtime permissions via `Context.revokeSelfPermissionsOnKill()`. Auto-revocation on unused apps applies for any app targeting Android 11+.

---

## 5. PERMISSION GROUPS

Used to bundle related dangerous permissions in the runtime dialog (granting one usually grants the group historically; modern Android prompts per-permission inside a group).

- `CALENDAR`
- `CALL_LOG` (Android 9+, split from PHONE)
- `CAMERA`
- `CONTACTS`
- `LOCATION`
- `MICROPHONE`
- `PHONE`
- `SENSORS`
- `SMS`
- `STORAGE` (legacy; replaced by `READ_MEDIA_*`)
- `READ_MEDIA_AURAL` (audio)
- `READ_MEDIA_VISUAL` (images + video)
- `NEARBY_DEVICES` (Bluetooth, Wi-Fi, UWB)
- `ACTIVITY_RECOGNITION`
- `NOTIFICATIONS`

---

## 6. HARDWARE / SOFTWARE FEATURES (`<uses-feature>` — not permissions, but adjacent)

Not permissions, but Play Store filters apps by these. Worth listing because they're often confused with permissions:
- `android.hardware.camera` (+ `.autofocus`, `.flash`, `.front`, `.any`, `.external`, `.level.full`, `.capability.raw`, `.capability.manual_sensor`, `.capability.manual_post_processing`)
- `android.hardware.bluetooth`, `.bluetooth_le`
- `android.hardware.fingerprint`, `.biometrics.face`, `.biometrics.iris`
- `android.hardware.location`, `.gps`, `.network`
- `android.hardware.microphone`
- `android.hardware.nfc`, `.nfc.hce`, `.nfc.hcef`
- `android.hardware.sensor.*` (accelerometer, gyroscope, light, proximity, barometer, compass, heartrate, heartrate.ecg, stepcounter, stepdetector, hifi_sensors, relative_humidity, ambient_temperature)
- `android.hardware.telephony`, `.cdma`, `.gsm`, `.mbms`, `.euicc`
- `android.hardware.touchscreen` (+ `.multitouch`, `.multitouch.distinct`, `.multitouch.jazzhand`)
- `android.hardware.usb.host`, `.usb.accessory`
- `android.hardware.wifi`, `.wifi.direct`, `.wifi.aware`, `.wifi.passpoint`, `.wifi.rtt`
- `android.hardware.vulkan.compute`, `.level`, `.version`
- `android.hardware.opengles.aep`
- `android.hardware.vr.headtracking`, `.vr.high_performance`
- `android.hardware.type.watch`, `.automotive`, `.television`, `.pc`
- `android.software.app_widgets`
- `android.software.input_methods`
- `android.software.live_wallpaper`
- `android.software.midi`
- `android.software.print`
- `android.software.sip`, `.sip.voip`
- `android.software.connectionservice`
- `android.software.companion_device_setup`
- `android.software.activities_on_secondary_displays`
- `android.software.picture_in_picture`
- `android.software.leanback`, `.leanback_only`
- `android.software.managed_users`
- `android.software.securely_removes_users`
- `android.software.cant_save_state`
- `android.software.opengles.deqp.level`

---

## 7. DEPRECATED / LEGACY (still declarable, mostly no-op)

- `READ_OWNER_DATA`, `WRITE_OWNER_DATA`
- `READ_PROFILE`, `WRITE_PROFILE`
- `READ_SOCIAL_STREAM`, `WRITE_SOCIAL_STREAM`
- `READ_USER_DICTIONARY`, `WRITE_USER_DICTIONARY`
- `WRITE_HISTORY_BOOKMARKS`, `READ_HISTORY_BOOKMARKS`
- `SUBSCRIBED_FEEDS_*`
- `WRITE_SMS`
- `RESTART_PACKAGES`
- `GET_TASKS`
- `PERSISTENT_ACTIVITY`
- `FLASHLIGHT` *(replaced by Camera2 torch API; permission still declarable but unused)*
- `SET_PREFERRED_APPLICATIONS`
- `SET_PROCESS_FOREGROUND`
- `BROADCAST_PACKAGE_INSTALL`
- `BIND_CHOOSER_TARGET_SERVICE`
- `USE_FINGERPRINT`
- `BODY_SENSORS` (deprecated API 35; migrate to Health Connect)

---

## 8. VENDOR-SPECIFIC PERMISSIONS

These are NOT in AOSP. They're declared by OEM apps (Knox, MIUI Security, HMS Core, etc.) and only grantable on those vendors' devices. They follow the same protection-level rules — most are `signature` or `signature|privileged` and require either the vendor's signing key, an MDM/Knox license, or root + manual `pm grant` after editing the OEM allowlist XMLs.

### 8.1 Samsung (One UI / Knox / Galaxy)

**Knox SDK permissions** (require an activated KPE/Knox license; held by MDM/EMM apps):
- `com.samsung.android.knox.permission.KNOX_HW_CONTROL`
- `com.samsung.android.knox.permission.KNOX_NDA_PERIPHERAL_RT`
- `com.samsung.android.knox.permission.KNOX_NDA_KIOSK_MODE`
- `com.samsung.android.knox.permission.KNOX_BLUETOOTH`
- `com.samsung.android.knox.permission.KNOX_WIFI`
- `com.samsung.android.knox.permission.KNOX_LOCATION`
- `com.samsung.android.knox.permission.KNOX_USAGE_STATS`
- `com.samsung.android.knox.permission.KNOX_VPN`
- `com.samsung.android.knox.permission.KNOX_FIREWALL`
- `com.samsung.android.knox.permission.KNOX_SCEP_CLIENT`
- `com.samsung.android.knox.permission.KNOX_SE_FOR_ANDROID`
- `com.samsung.android.knox.permission.KNOX_LICENSE`
- `com.samsung.android.knox.permission.KNOX_REMOTE_CONTROL`
- `com.samsung.android.knox.permission.KNOX_SMARTCARD`
- `com.samsung.android.knox.permission.KNOX_USB`
- `com.samsung.android.knox.permission.KNOX_AUDIT_LOG`
- `com.samsung.android.knox.permission.KNOX_CCM` (Common Criteria Mode)
- `com.samsung.android.knox.permission.KNOX_CCM_KEYSTORE`
- `com.samsung.android.knox.permission.KNOX_TIMA_KEYSTORE`
- `com.samsung.android.knox.permission.KNOX_CONTAINER` (Workspace)
- `com.samsung.android.knox.permission.KNOX_CONTAINER_SDP`
- `com.samsung.android.knox.permission.KNOX_LSO` (Lockdown screen overlay)
- `com.samsung.android.knox.permission.KNOX_DEX`
- `com.samsung.android.knox.permission.KNOX_FOTA`
- `com.samsung.android.knox.permission.KNOX_BIOMETRIC`
- `com.samsung.android.knox.permission.KNOX_REMOTE_INJECTION`
- `com.samsung.android.knox.permission.KNOX_RESTRICTION_MGR`
- `com.samsung.android.knox.permission.KNOX_BROWSER`
- `com.samsung.android.knox.permission.KNOX_EMAIL`
- `com.samsung.android.knox.permission.KNOX_EXCHANGE`
- `com.samsung.android.knox.permission.KNOX_CERTIFICATE`
- `com.samsung.android.knox.permission.CUSTOM_SETTING`
- `com.samsung.android.knox.permission.CUSTOM_SEALEDMODE`
- `com.samsung.android.knox.permission.KNOX_APP_MGMT`
- `com.samsung.android.knox.permission.KNOX_NETWORK_STATS`

**Legacy `com.sec.enterprise` family** (Knox 2.x; still works on newer):
- `com.sec.enterprise.permission.MDM_PROXY_ADMIN_INTERNAL`
- `com.sec.enterprise.permission.MDM_APP_MGMT`
- `com.sec.enterprise.permission.MDM_FIREWALL`
- `com.sec.enterprise.permission.MDM_SECURITY`
- `com.sec.enterprise.permission.MDM_LOCATION`
- `com.sec.enterprise.permission.MDM_RESTRICTION`
- `com.sec.enterprise.permission.MDM_VPN`
- `com.sec.enterprise.permission.MDM_WIFI`
- `com.sec.enterprise.permission.MDM_BLUETOOTH`
- `com.sec.enterprise.permission.MDM_PHONE_RESTRICTION`
- `com.sec.enterprise.permission.MDM_SETTINGS`
- `com.sec.enterprise.permission.MDM_BROWSER`
- `com.sec.enterprise.permission.MDM_EMAIL`
- `com.sec.enterprise.permission.MDM_EXCHANGE`
- `com.sec.enterprise.permission.MDM_USB`
- `com.sec.enterprise.permission.MDM_DUAL_SIM`
- `com.sec.enterprise.permission.MDM_ENTERPRISE_ISL`
- `com.sec.enterprise.permission.MDM_KIOSK_MODE`
- `com.sec.enterprise.permission.MDM_SMARTCARD`
- `com.sec.enterprise.knox.cloudmdm.smdms.permission.SAMSUNG_MDM_SERVICE`
- `com.sec.enterprise.knox.permission.KNOX_CONTAINER_INTERNAL`
- `com.sec.enterprise.knox.permission.KNOX_VPN_INTERNAL`
- `com.sec.enterprise.mdm.permission.BROWSER_PROXY`
- `android.permission.sec.MDM_ENTERPRISE_DEVICE_ADMIN`
- `android.permission.sec.MDM_DUAL_SIM`
- `android.permission.sec.MDM_ENTERPRISE_ISL`
- `android.permission.sec.MDM_ENTERPRISE_CONTAINER`
- `android.permission.sec.ENTERPRISE_MOUNT_UNMOUNT_ENCRYPT`
- `android.permission.sec.ENTERPRISE_CONTAINER`

**Samsung consumer / framework permissions** (Galaxy-only system features):
- `com.samsung.android.permission.SAMSUNG_PUSH`
- `com.samsung.android.permission.SAMSUNG_OAUTH`
- `com.samsung.android.permission.SAMSUNG_ACCOUNT`
- `com.samsung.android.providers.context.permission.WRITE_USE_APP_FEATURE_SURVEY`
- `com.samsung.android.permission.WRITE_HEALTH_DATA`
- `com.samsung.android.permission.READ_HEALTH_DATA` (Samsung Health)
- `com.samsung.android.providers.health.permission.HEALTH_DATA_PROVIDER`
- `com.samsung.android.bixby.agent.permission.BIXBY_AGENT_SERVICE`
- `com.samsung.android.bixby.permission.BIXBY_VOICE`
- `com.samsung.android.bixby.permission.READ_BIXBY_DATA`
- `com.samsung.android.permission.GED_SERVICE` (Galaxy Enhance-X / Good Earth Data)
- `com.samsung.android.aremoji.permission.READ_EMOJI`
- `com.samsung.android.permission.READ_WEARABLE_APP_INSTALL`
- `com.samsung.android.permission.SAMSUNG_KEYGUARD`
- `com.samsung.permission.SSENSOR`
- `com.samsung.permission.HRM_EXT`
- `com.samsung.android.app.shealth.permission.READ`
- `com.samsung.android.app.shealth.permission.WRITE`
- `com.samsung.android.permission.LIVE_WALLPAPER`
- `com.sec.android.app.camera.permission.SCREEN_RECORDING`
- `com.samsung.accessory.permission.ACCESSORY_FRAMEWORK`
- `com.samsung.accessory.permission.ACCESSORY` (Samsung Accessory Protocol — Galaxy Watch comm.)
- `com.sec.android.permission.LOCAL_DRM`
- `com.sec.android.app.sbrowser.permission.READ_BOOKMARK`
- `com.sec.android.app.sbrowser.permission.WRITE_BOOKMARK`
- `com.sec.android.permission.PERSONAL_PAGE`
- `com.sec.android.diagmonagent.permission.DIAGMON`
- `com.samsung.android.permission.READ_PRECISE_PHONE_STATE`
- `com.samsung.android.permission.READ_PROFILE`
- `com.samsung.android.permission.SEC_INTERNAL_BROADCAST`
- `com.samsung.android.permission.READ_SYSTEM_SETTINGS`
- `com.samsung.android.permission.WRITE_SYSTEM_SETTINGS`
- `com.samsung.app.cocktailbarservice.permission.READ_COCKTAIL_PROVIDER` (Edge panel)
- `com.samsung.app.cocktailbarservice.permission.WRITE_COCKTAIL_PROVIDER`
- `com.samsung.android.permission.GET_MULTI_SIM_STATE`
- `com.samsung.android.permission.SAMSUNG_DEX`
- `com.samsung.android.spen.permission.READ_SPEN_USAGE`
- `com.samsung.android.app.galaxyfinder.permission.QUERY`
- `com.samsung.android.app.notes.permission.READ_NOTES`
- `com.samsung.android.app.notes.permission.WRITE_NOTES`

### 8.2 Xiaomi (MIUI / HyperOS)

MIUI splits OS-level permissions into a separate "MIUI permissions" section in `Settings → Apps → <app> → Other permissions`. These are special permissions in addition to AOSP ones. They're settable per-app but not declarable in a way Google Play accepts — most are signature-protected and granted to system apps; the user-facing toggles map to internal `appop`-like states managed by `com.miui.securitycenter`.

**User-facing toggles in MIUI Other permissions** (mapped to internal permission names):
- `Autostart` — internal: managed by `com.miui.permcenter.autostart`, persists in `power_keep_alive` table. No public manifest perm; toggled per-app in Security Center. Scope: On / Off.
- `Display pop-up windows while running in the background` — `miui.permission.SHOW_WHEN_LOCKED` / `BACKGROUND_START_ACTIVITY`. Scope: Allow always / Disallow.
- `Display pop-up window` — `miui.permission.POPUP_WHEN_LOCKED`. Scope: On / Off.
- `Show on Lock screen` — `miui.permission.SHOW_ON_LOCK_SCREEN`. Scope: On / Off.
- `Start in background` — same as Autostart for newer MIUI.
- `Background autostart` (MIUI 14+) — separate from above on newer builds. Scope: On / Off.
- `Display pop-up windows` (chat heads / bubbles)
- `Change Wi-Fi connectivity` — `miui.permission.ACCESS_WIFI_STATE_INTERNAL`. Without this MIUI shows confirmation popup each Wi-Fi switch. Scope: Allow / Ask / Deny.
- `Change Bluetooth connectivity` — same model as Wi-Fi.
- `Read installed apps list` — `com.android.permission.GET_INSTALLED_APPS` (MIUI custom). Scope: Allow / Ask every time / Deny. *(MIUI added this years before AOSP added `QUERY_ALL_PACKAGES`.)*
- `Create homescreen shortcuts` — `com.android.launcher.permission.INSTALL_SHORTCUT` (MIUI gates it). Scope: On / Off.
- `Persistent notifications`
- `Show notifications`
- `Modify system settings` (mirror of `WRITE_SETTINGS`)
- `Display over other apps` (mirror of `SYSTEM_ALERT_WINDOW`)
- `Trust this app` (MIUI Game Turbo / Lite Mode whitelist)

**MIUI / HyperOS internal permissions** (signature-protected, system-only):
- `miui.permission.USE_INTERNAL_GENERAL_API`
- `miui.permission.READ_PHONE_NUMBER`
- `miui.permission.READ_PHONE_STATE_FOR_OPEN_DOOR`
- `miui.permission.WALLPAPER`
- `miui.permission.READ_NETWORK_STATS`
- `miui.permission.WRITE_NETWORK_STATS`
- `miui.permission.SECURITY_CENTER_INTERNAL`
- `miui.permission.OP_ANTI_SPAM`
- `miui.permission.MIPUSH_RECEIVE` (Mi Push for client apps)
- `miui.permission.SYSTEM_OR_SIGNATURE`
- `miui.permission.MANAGE_LOCKED_APPS`
- `miui.permission.ACCESS_GAME_BOOSTER`
- `miui.permission.ACCESS_XIAOMI_ACCOUNT`
- `miui.permission.READ_DEVICE_INFO`
- `miui.permission.MANAGE_SUPER_WALLPAPER`
- `miui.permission.READ_PRIVACY_SETTINGS`
- `miui.permission.GAME_VIDEO_RECORDER`
- `miui.permission.ACCESS_THERMAL_SERVICE`
- `miui.permission.SYSTEM_API`
- `miui.permission.ACCESS_NEXT_DAY_BRIEF`
- `miui.permission.MANAGE_GUARD_PROVIDER`
- `miui.permission.ACCESS_GREENGUARD`
- `miui.permission.UPDATE_SECURITY_DATABASE`

**Xiaomi consumer / Mi services**:
- `com.xiaomi.permission.AUTH_SERVICE`
- `com.xiaomi.account.permission.ACCESS_XIAOMI_ACCOUNT`
- `com.xiaomi.market.permission.MIPUSH_RECEIVE`
- `com.xiaomi.xmsf.permission.MIPUSH_RECEIVE`
- `com.xiaomi.permission.MIPUSH_RECEIVE` (newer namespace)
- `com.xiaomi.permission.GET_INSTALLED_APPS`
- `com.xiaomi.smarthome.permission.READ`
- `com.xiaomi.smarthome.permission.WRITE`
- `com.xiaomi.permission.READ_HEALTH_DATA` (Mi Health)
- `com.xiaomi.permission.WRITE_HEALTH_DATA`
- `com.xiaomi.permission.IDENTITY_AUTH`
- `com.miui.miwallpaper.permission.READ_WALLPAPER`
- `com.miui.systemAdSolution.permission.GLOBAL_AD` (Mi adv. system)
- `com.miui.home.launcher.permission.READ_SETTINGS`
- `com.miui.home.launcher.permission.WRITE_SETTINGS`
- `com.miui.home.launcher.permission.UPDATE_SHORTCUT`
- `com.miui.gallery.permission.ACCESS_PROVIDER`

### 8.3 Huawei (EMUI / HarmonyOS in Android-compat mode)

Huawei has the largest custom-permission surface of the three because HMS Core replaces Google Play Services on newer devices.

**Huawei system permissions** (held by Huawei system apps and HMS Core):
- `com.huawei.permission.external_app_settings.USE_COMPONENT` — needed to launch `com.huawei.systemmanager` activities for protected-apps and autostart settings
- `com.huawei.permission.sec.MDM_PROXY`
- `com.huawei.permission.sec.MDM_APP_MGMT`
- `com.huawei.permission.sec.MDM_RESTRICTION`
- `com.huawei.permission.sec.MDM_VPN`
- `com.huawei.permission.sec.MDM_FIREWALL`
- `com.huawei.permission.sec.MDM_LOCATION`
- `com.huawei.permission.sec.MDM_PHONE_RESTRICTION`
- `com.huawei.permission.sec.MDM_SECURITY`
- `com.huawei.permission.sec.MDM_SETTINGS`
- `com.huawei.permission.sec.MDM_USB`
- `com.huawei.permission.sec.MDM_WIFI`
- `com.huawei.permission.sec.MDM_BLUETOOTH`
- `com.huawei.permission.sec.MDM_KIOSK_MODE`
- `com.huawei.permission.sec.MDM_SMARTCARD`
- `com.huawei.permission.HUAWEI_SYSTEM_NODE_ACCESS`
- `com.huawei.permission.ACCESS_WEATHERCLOCK_PROVIDER`
- `com.huawei.permission.READ_LATITUDE`
- `com.huawei.permission.WRITE_LATITUDE`
- `com.huawei.permission.READ_PHONE_STATE_HW`
- `com.huawei.android.permission.GET_DEFAULT_EMUI_LAUNCHER`
- `com.huawei.android.launcher.permission.READ_SETTINGS`
- `com.huawei.android.launcher.permission.WRITE_SETTINGS`
- `com.huawei.android.launcher.permission.CHANGE_BADGE` (unread badge integration)
- `com.huawei.permission.LAUNCH_FACEUNLOCK_ACTIVITY`
- `com.huawei.permission.RECEIVE_BIOMETRIC_BROADCAST`
- `com.huawei.permission.MANAGE_DRM_CERTIFICATES`
- `com.huawei.permission.MANAGE_TEE`
- `com.huawei.permission.systemmanager.WRITE_PRIVACY_SETTINGS`
- `com.huawei.permission.PROTECTED_APP` (gating which apps survive PowerGenie kill)
- `com.huawei.permission.USE_HEALTH_KIT`
- `com.huawei.permission.HW_OWNER_DATA`
- `com.huawei.systemmanager.permission.ACCESS_HSM` (HiSpace Manager)

**HMS Core / AppGallery permissions** (consumer-facing, declared in third-party apps that integrate HMS):
- `com.huawei.appmarket.service.commondata.permission.GET_COMMON_DATA` — minimum to use HMS Core
- `com.huawei.android.launcher.permission.CHANGE_BADGE`
- `com.huawei.hms.permission.PUSH_PROVIDER`
- `com.huawei.android.push.permission.MESSAGING` (Push Kit)
- `${applicationId}.permission.PUSH_PROVIDER` (app-specific)
- `com.huawei.hwid.permission.AUTH` (Account Kit)
- `com.huawei.hms.permission.LOCATION_VENDOR` (Location Kit)
- `com.huawei.hms.permission.HEALTHKIT` (+ `.HEIGHT_READ`, `.HEIGHT_WRITE`, `.WEIGHT_READ`, `.WEIGHT_WRITE`, `.STEP_READ`, `.STEP_WRITE`, `.HEARTRATE_READ`, `.HEARTRATE_WRITE`, `.SPEED_READ`, `.SPEED_WRITE`, `.DISTANCE_READ`, `.DISTANCE_WRITE`, `.ACTIVITY_READ`, `.ACTIVITY_WRITE`, `.SLEEP_READ`, `.SLEEP_WRITE`, `.NUTRITION_READ`, `.NUTRITION_WRITE`, `.BLOODPRESSURE_READ`, `.BLOODPRESSURE_WRITE`, `.BLOODGLUCOSE_READ`, `.BLOODGLUCOSE_WRITE`, `.OXYGENSATURATION_READ`, `.OXYGENSATURATION_WRITE`, `.BODYTEMPERATURE_READ`, `.BODYTEMPERATURE_WRITE`, `.MENSTRUATION_READ`, `.MENSTRUATION_WRITE`, `.REPRODUCTIVE_READ`, `.REPRODUCTIVE_WRITE`)
- `com.huawei.hms.permission.MAP` (Map Kit)
- `com.huawei.hms.permission.WALLET` (Wallet Kit)
- `com.huawei.hms.permission.SAFETYDETECT` (Safety Detect)
- `com.huawei.hms.permission.GAMECENTER` / `.GAME_SERVICE`
- `com.huawei.hms.permission.IAP` (In-App Purchases)
- `com.huawei.hms.permission.AWARENESS_BARRIER` (Awareness Kit)
- `com.huawei.hms.permission.NEARBY` (Nearby Service)
- `com.huawei.hms.permission.SCAN` (Scan Kit)
- `com.huawei.hms.permission.SITE` (Site Kit)
- `com.huawei.hms.permission.ML` (ML Kit)
- `com.huawei.hms.permission.AUTOPILOT` (Auto Kit)
- `com.huawei.hms.permission.WEAR_KIT`
- `com.huawei.hms.permission.DEVICE_VIRTUALIZATION` (HiCar / Smart Cabin)
- `com.huawei.hwid.permission.GET_USER_INFO`
- `com.huawei.hwid.permission.QUICKLOGIN`
- `com.huawei.appmarket.permission.SHOW_DOWNLOAD_NOTIFICATION`
- `com.huawei.android.thememanager.permission.ACCESS_THEMEMANAGER`
- `com.huawei.android.permission.HW_SIGNATURE_OR_SYSTEM`

**HarmonyOS / OpenHarmony permissions** (when Huawei HarmonyOS Next is present, an entirely new permission family applies — `ohos.permission.*`. These are not Android permissions but show up in dual-stack devices):
- `ohos.permission.READ_USER_STORAGE`
- `ohos.permission.WRITE_USER_STORAGE`
- `ohos.permission.LOCATION` / `LOCATION_IN_BACKGROUND`
- `ohos.permission.CAMERA`
- `ohos.permission.MICROPHONE`
- `ohos.permission.READ_CONTACTS` / `WRITE_CONTACTS`
- `ohos.permission.READ_CALL_LOG` / `WRITE_CALL_LOG`
- `ohos.permission.READ_CALENDAR` / `WRITE_CALENDAR`
- `ohos.permission.ACCESS_BLUETOOTH`
- `ohos.permission.GET_NETWORK_INFO`
- `ohos.permission.GET_WIFI_INFO`
- `ohos.permission.DISTRIBUTED_DATASYNC` (cross-device data flows)
- `ohos.permission.DISTRIBUTED_VIRTUALDEVICE` (Super Device features)
- `ohos.permission.MANAGE_MISSIONS`
- (full set runs ~150 entries; only relevant on HarmonyOS Next — your P30 won't see these.)

### 8.4 Other vendors (P30/14T context aside)

- **OPPO/Realme (ColorOS / RealmeUI):** `oppo.permission.OPPO_COMPONENT_SAFE`, `com.coloros.safecenter.permission.*`, `com.heytap.market.permission.*`
- **OnePlus (OxygenOS):** `com.oneplus.permission.*` (mostly merged with ColorOS post-2021)
- **Vivo / iQOO (Funtouch / OriginOS):** `com.vivo.permission.READ_INSTALLED_APPS`, `com.bbk.account.permission.AUTH`, `com.vivo.permission.MIPUSH_RECEIVE_VPUSH`
- **Honor (MagicOS):** mostly mirrors Huawei `com.hihonor.permission.*` plus the legacy `com.huawei.*` namespace
- **Sony (Xperia):** `com.sonyericsson.permission.*`
- **Motorola (My UX):** `com.motorola.permission.*`, `com.motorola.bach.permission.*`
- **Asus (ZenUI/ROG):** `com.asus.permission.*`

---

## 9. PLATFORM-VARIANT PERMISSIONS (Wear OS, Auto, TV, Automotive, XR)

### 9.1 Wear OS
- `com.google.android.wearable.permission.RECEIVE_PAYMENT_DATA`
- `com.google.android.permission.PROVIDE_BACKGROUND` (Wear tile bg)
- `android.permission.BIND_TILE_PROVIDER_SERVICE` (Wear tiles)
- `android.permission.PROVIDE_REMOTE_CREDENTIALS`
- `com.google.android.gms.permission.BIND_NETWORK_TASK_SERVICE`
- `com.google.android.wearable.app.permission.RECEIVE_DATA_ITEM`
- All Health Connect read/write permissions
- `BODY_SENSORS_BACKGROUND` — primary Wear use case

### 9.2 Android Auto (phone projection)
- `com.google.android.gms.permission.CAR_INFORMATION`
- `com.google.android.gms.permission.CAR_SPEED`
- `com.google.android.gms.permission.CAR_FUEL`
- `com.google.android.gms.permission.CAR_MILEAGE`
- `com.google.android.gms.permission.CAR_VENDOR_EXTENSION`

### 9.3 Android Automotive OS (in-car, not phone-projection)
- `android.car.permission.CAR_CONTROL_AUDIO_VOLUME`
- `android.car.permission.CAR_CONTROL_AUDIO_SETTINGS`
- `android.car.permission.CAR_INFO`
- `android.car.permission.CAR_VENDOR_EXTENSION`
- `android.car.permission.CAR_ENERGY`
- `android.car.permission.CAR_ENERGY_PORTS`
- `android.car.permission.CAR_EXTERIOR_ENVIRONMENT`
- `android.car.permission.CAR_IDENTIFICATION`
- `android.car.permission.CAR_MILEAGE`
- `android.car.permission.CAR_POWER`
- `android.car.permission.CAR_POWERTRAIN`
- `android.car.permission.CAR_SPEED`
- `android.car.permission.CAR_TIRES`
- `android.car.permission.CAR_DYNAMICS_STATE`
- `android.car.permission.CAR_DRIVING_STATE`
- `android.car.permission.CAR_DIAGNOSTICS`
- `android.car.permission.CONTROL_CAR_CLIMATE`
- `android.car.permission.CONTROL_CAR_DOORS`
- `android.car.permission.CONTROL_CAR_MIRRORS`
- `android.car.permission.CONTROL_CAR_SEATS`
- `android.car.permission.CONTROL_CAR_WINDOWS`
- `android.car.permission.READ_CAR_STEERING`
- `android.car.permission.READ_CAR_DISPLAY_UNITS`
- `android.car.permission.CONTROL_CAR_DISPLAY_UNITS`
- `android.car.permission.CAR_NAVIGATION_MANAGER`
- `android.car.permission.CAR_INSTRUMENT_CLUSTER_CONTROL`
- `android.car.permission.CAR_PROJECTION`
- `android.car.permission.BIND_VMS_CLIENT` (Vehicle Map Service)
- `android.car.permission.CAR_UX_RESTRICTIONS_CONFIGURATION`
- `android.car.permission.STORAGE_MONITORING`

### 9.4 Android TV
- `com.google.android.tv.permission.READ_CONTENT_RATING_SYSTEM`
- `android.permission.MODIFY_PARENTAL_CONTROLS`
- `android.permission.READ_TV_LISTINGS`
- `android.permission.TV_INPUT_HARDWARE`
- `android.permission.TUNE_TV_INPUT`

### 9.5 Android XR (new in 2025/26 for Samsung XR headset etc.)
- `android.permission.HEAD_TRACKING`
- `android.permission.EYE_TRACKING`
- `android.permission.HAND_TRACKING`
- `android.permission.SCENE_UNDERSTANDING`

---

## 10. GOOGLE / GMS PERMISSIONS (declared by manifest when integrating Play services)

These aren't AOSP — they're defined by Google Play Services. A normal app declares them; granting flows through GMS, not Android's permission system.

- `com.google.android.c2dm.permission.RECEIVE` (FCM legacy)
- `com.google.android.gms.permission.AD_ID` (Advertising ID; Android 12+ runtime)
- `com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE`
- `com.google.android.gms.permission.ACTIVITY_RECOGNITION`
- `com.google.android.providers.gsf.permission.READ_GSERVICES`
- `com.google.android.gms.permission.SHOW_PERMISSION_RATIONALE`
- `${applicationId}.permission.C2D_MESSAGE` (legacy GCM, deprecated)
- `${applicationId}.permission.MAPS_RECEIVE`
- `com.google.android.googleapps.permission.GOOGLE_AUTH`
- `com.google.android.googleapps.permission.GOOGLE_AUTH.*` (per-service variants)
- `com.google.android.gms.auth.api.signin.permission.SIGN_IN`
- `com.google.android.gms.permission.AUTOCOMPLETE_SUGGESTIONS`
- `com.google.android.gms.permission.READ_HEART_RATE` *(Fit; deprecated for Health Connect)*
- `com.google.android.gms.permission.WRITE_FITNESS_DATA`
- `com.google.android.gms.permission.READ_FITNESS_DATA`
- `com.google.android.gms.permission.LOCATION_HISTORY`
- `com.google.android.gms.permission.PROFILE_OWNER` *(Android Enterprise DPC handshake)*
- `com.google.android.gms.cast.permission.CAST_API`
- `com.google.android.gms.dck.permission.DIGITAL_KEY` (Digital Car Key)

---

## 11. PERMISSIONS YOU CAN DEFINE YOURSELF (custom)

Any app can declare its own permissions in its manifest with `<permission>`. Other apps then `<uses-permission>` them. Custom permissions inherit the same protection-level rules as platform ones — `normal`, `dangerous`, `signature`, `signature|privileged`. Common patterns:
```xml
<permission android:name="com.example.myapp.permission.READ_MY_DATA"
            android:protectionLevel="signature" />
```
- App-side broadcast guards: `${applicationId}.permission.INTERNAL_BROADCAST`
- Push integration: `${applicationId}.permission.C2D_MESSAGE`, `.MIPUSH_RECEIVE`, `.PUSH_PROVIDER`

---

## TL;DR cheat sheet

| Category | Count | Root needed? | User can grant? |
|---|---|---|---|
| AOSP normal (install-time) | ~50 | No | Auto |
| AOSP dangerous (runtime) | ~40 + ~50 health | No | Yes, via prompt |
| AOSP special (appop) | ~30 | No | Yes, via Settings |
| Companion-profile | 7 | No | Via pairing flow |
| AOSP signature | ~80 | Yes | No (platform key) |
| AOSP signature\|privileged | ~150 | Yes | No (priv-app + allowlist) |
| ADB-grantable (development) | ~10 | No (adb only) | Only via `pm grant` |
| Pure root capabilities | ~30+ | Yes | N/A — Linux level |
| **Samsung Knox + One UI** | ~80 | Yes (Knox license or system) | MDM-only |
| **Xiaomi MIUI/HyperOS** | ~30 + 14 user toggles | Toggles: no. Internal: yes | Per-app toggles in Security Center |
| **Huawei EMUI + HMS Core + HarmonyOS** | ~50 Android + ~150 ohos | Yes for system; HMS ones declarable | Pairing flow / runtime |
| Other OEM (OPPO/Vivo/etc.) | ~40 | Yes | Vendor-specific |
| Wear / Auto / TV / Automotive / XR | ~40 | Varies | Form-factor specific |
| Google Play Services | ~20 | No | Via GMS handshake |
| Custom (yours) | unlimited | Depends on protectionLevel | Same rules as AOSP |

**Grand total of grantable permissions across the Android ecosystem ≈ 700+**, of which ~250 are in the public AOSP `Manifest.permission` SDK reference; the rest are AOSP-hidden, OEM, GMS, Health Connect, vehicle, or HarmonyOS additions.

**For your devices specifically:**
- **Xiaomi 14T Pro** (HyperOS 2 / Android 15): full §2 + §3 AOSP + §8.2 MIUI/HyperOS toggles + §10 GMS. Watch the MIUI Other-permissions section — autostart, background pop-up, lock-screen display, and "read installed apps list" are killers for app behavior and aren't in any standard permissions library.
- **Huawei P30** (EMUI 12 / Android 10 base): full §2 + §3 AOSP at API-29 level + §8.3 Huawei + HMS Core. No GMS unless you sideloaded it. The `com.huawei.permission.external_app_settings.USE_COMPONENT` is what you'll hit if you try to open the protected-apps screen programmatically — it's signature-protected, so on a stock P30 you can only deeplink the user there via intent, not toggle it for them.
