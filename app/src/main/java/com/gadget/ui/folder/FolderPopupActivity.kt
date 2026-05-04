package com.gadget.ui.folder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.gadget.apps.AppLauncher
import com.gadget.localization.LocalizationManager
import com.gadget.ui.theme.AccessibilityPreferencesManager
import com.gadget.ui.theme.GadgetTheme
import com.gadget.ui.theme.ThemePreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Translucent / floating activity that mimics Android's native folder popup.
 * Reachable from a home-screen widget (batch 8) or via:
 *
 *     adb shell am start -n com.gadget/.ui.folder.FolderPopupActivity \
 *         --el folder_id 1
 *
 * The `Theme.Gadget.Translucent` style sets `windowIsFloating=true` and
 * `windowCloseOnTouchOutside=true`, so the system finishes us automatically
 * when the user taps outside the floating Card.
 */
@AndroidEntryPoint
class FolderPopupActivity : ComponentActivity() {

    @Inject lateinit var appLauncher: AppLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mirror MainActivity's bootstrap so locale/theme resolve correctly even
        // when this is the first activity in the process (e.g. cold-launched
        // from a widget tap).
        LocalizationManager.init(this)
        AccessibilityPreferencesManager.init(this)
        ThemePreferencesManager.init(this)

        val folderId = intent.getLongExtra(EXTRA_FOLDER_ID, -1L)
        if (folderId < 0L) {
            finish()
            return
        }

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
