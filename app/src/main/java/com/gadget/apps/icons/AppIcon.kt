package com.gadget.apps.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gadget.apps.AppsEntryPoint
import com.gadget.data.db.apps.AppRecord
import dagger.hilt.android.EntryPointAccessors

/**
 * Compose surface for [AppIconLoader]. Loads a real launcher / WebAPK / favicon
 * bitmap off the main thread (cached so subsequent renders are zero-IO) and
 * renders it via [Image]. While the IO is in flight on first sight, a soft
 * surface-variant circle stands in so the layout doesn't shift.
 *
 * The loader is reached via [EntryPointAccessors] rather than `hiltViewModel()`
 * so this composable can be called from anywhere — including small reusable
 * cells like the folder editor's AppRow that don't own a ViewModel.
 */
@Composable
fun AppIcon(
    record: AppRecord,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp,
) {
    val context = LocalContext.current
    val loader = remember {
        EntryPointAccessors
            .fromApplication(context.applicationContext, AppsEntryPoint::class.java)
            .appIconLoader()
    }
    val density = LocalDensity.current
    val sizePx = remember(sizeDp) { with(density) { sizeDp.toPx().toInt() } }
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, record.appKey, sizePx) {
        value = loader.loadImageBitmap(record, sizePx)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = record.label,
            modifier = modifier.size(sizeDp).clip(CircleShape),
        )
    } else {
        // Placeholder while the loader resolves; never the generic 3×3 grid.
        Box(
            modifier = modifier
                .size(sizeDp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}
