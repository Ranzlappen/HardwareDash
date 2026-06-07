package dev.ranzlappen.gadget.feature.apps.ui.folder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.core.designsystem.theme.GadgetTheme
import dev.ranzlappen.gadget.feature.apps.AppLauncher
import dev.ranzlappen.gadget.feature.apps.R
import dev.ranzlappen.gadget.feature.apps.security.FolderLockManager
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Translucent / floating activity that mimics Android's native folder popup.
 * Reachable from the folder home-screen widget or via:
 *
 *     adb shell am start -n com.gadget/dev.ranzlappen.gadget.feature.apps.ui.folder.FolderPopupActivity \
 *         --el folder_id 1
 *
 * Locked folders gate their contents behind a biometric prompt before the
 * Compose grid renders. Extends FragmentActivity (not ComponentActivity)
 * because androidx.biometric.BiometricPrompt requires a FragmentActivity host.
 *
 * The `Theme.GadgetApps.FolderPopup` style sets `windowIsFloating=true` and
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
                folderLockManager.authenticate(
                    activity = this@FolderPopupActivity,
                    title = getString(R.string.apps_unlock_folder),
                    subtitle = folder.name,
                    cancelLabel = getString(R.string.apps_cancel),
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

        fun intent(context: Context, folderId: Long): Intent =
            Intent(context, FolderPopupActivity::class.java).apply {
                putExtra(EXTRA_FOLDER_ID, folderId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}
