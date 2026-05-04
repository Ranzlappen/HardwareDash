package com.gadget.ui.folder

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.gadget.apps.AppLauncher
import com.gadget.apps.security.FolderLockManager
import com.gadget.data.db.apps.AppsDao
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.ui.theme.AccessibilityPreferencesManager
import com.gadget.ui.theme.GadgetTheme
import com.gadget.ui.theme.ThemePreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Translucent / floating activity that mimics Android's native folder popup.
 * Reachable from a home-screen widget (batches 8/9) or via:
 *
 *     adb shell am start -n com.gadget/.ui.folder.FolderPopupActivity \
 *         --el folder_id 1
 *
 * Locked folders gate their contents behind a biometric prompt before the
 * Compose grid renders. Extends FragmentActivity (not ComponentActivity)
 * because androidx.biometric.BiometricPrompt requires a FragmentActivity host.
 *
 * The `Theme.Gadget.Translucent` style sets `windowIsFloating=true` and
 * `windowCloseOnTouchOutside=true`, so the system finishes us automatically
 * when the user taps outside the floating Card.
 */
@AndroidEntryPoint
class FolderPopupActivity : FragmentActivity() {

    @Inject lateinit var appLauncher: AppLauncher
    @Inject lateinit var folderLockManager: FolderLockManager
    @Inject lateinit var dao: AppsDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LocalizationManager.init(this)
        AccessibilityPreferencesManager.init(this)
        ThemePreferencesManager.init(this)

        val folderId = intent.getLongExtra(EXTRA_FOLDER_ID, -1L)
        if (folderId < 0L) {
            finish()
            return
        }

        lifecycleScope.launch {
            val folder = dao.getFolder(folderId)
            if (folder == null) {
                finish()
                return@launch
            }

            if (folder.locked && folderLockManager.canAuthenticate(this@FolderPopupActivity)) {
                // S.apps / S.common have @Composable getters; resolve the
                // language directly here so we can pass plain strings to
                // BiometricPrompt without needing to be inside a Composable.
                val lang = LocalizationManager.currentLanguage.value
                val apps = S.Apps(lang)
                val common = S.Common(lang)
                folderLockManager.authenticate(
                    activity = this@FolderPopupActivity,
                    title = apps.unlockFolder,
                    subtitle = folder.name,
                    cancelLabel = common.cancel,
                    onSuccess = { renderContent(folderId) },
                    onCancel = { finish() },
                )
            } else {
                renderContent(folderId)
            }
        }
    }

    private fun renderContent(folderId: Long) {
        setContent {
            GadgetTheme {
                FolderPopupContent(
                    folderId = folderId,
                    onAppClick = { record ->
                        lifecycleScope.launch {
                            appLauncher.launch(record)
                            finish()
                        }
                    },
                )
            }
        }
    }

    companion object {
        const val EXTRA_FOLDER_ID = "folder_id"

        fun intent(context: android.content.Context, folderId: Long): Intent =
            Intent(context, FolderPopupActivity::class.java).apply {
                putExtra(EXTRA_FOLDER_ID, folderId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}
