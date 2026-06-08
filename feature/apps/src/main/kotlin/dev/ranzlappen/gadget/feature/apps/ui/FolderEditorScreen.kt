@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package dev.ranzlappen.gadget.feature.apps.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.data.apps.AppRecord
import dev.ranzlappen.gadget.core.data.apps.Folder
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetColorPicker
import dev.ranzlappen.gadget.core.ui.component.GlassSurface
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.feature.apps.R
import dev.ranzlappen.gadget.feature.apps.icons.AppIcon
import dev.ranzlappen.gadget.feature.apps.icons.MaterialSymbol
import dev.ranzlappen.gadget.feature.apps.rules.FolderRule
import dev.ranzlappen.gadget.feature.apps.rules.FolderRuleSet
import dev.ranzlappen.gadget.feature.apps.ui.folder.FolderPopupActivity
import kotlinx.coroutines.launch

/**
 * Per-folder editor (Hilt route). Thin wrapper over the Hilt-free
 * [FolderEditorContent].
 */
@Composable
fun FolderEditorScreen(onBack: () -> Unit) {
    val viewModel = hiltViewModel<FolderEditorViewModel>()
    val folder by viewModel.folder.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val membership by viewModel.membership.collectAsState()
    val ruleSet by viewModel.ruleSet.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val otherFolderMembership by viewModel.otherFolderMembership.collectAsState()

    FolderEditorContent(
        state = FolderEditorState(
            folder = folder,
            filteredApps = filteredApps,
            membership = membership,
            ruleSet = ruleSet,
            searchQuery = searchQuery,
            otherFolderMembership = otherFolderMembership,
        ),
        onBack = onBack,
        onRename = viewModel::rename,
        onSetColor = viewModel::setBaseColor,
        onSetLocked = viewModel::setLocked,
        onPinToHome = viewModel::pinToHome,
        onToggleMember = viewModel::toggleMember,
        onAddWebLink = viewModel::addWebLink,
        onSetCoverSymbol = viewModel::setCoverSymbol,
        onClearCover = viewModel::clearCover,
        onSetCoverImage = viewModel::setCoverImageFromUri,
        onAddOrReplaceRule = viewModel::addOrReplaceRule,
        onRemoveRuleOfKind = viewModel::removeRuleOfKind,
        onSearchChange = { viewModel.searchQuery.value = it },
    )
}

/** Rendered state for [FolderEditorContent]. */
data class FolderEditorState(
    val folder: Folder?,
    val filteredApps: List<AppRecord>,
    val membership: Set<String>,
    val ruleSet: FolderRuleSet,
    val searchQuery: String,
    val otherFolderMembership: Map<String, List<String>>,
)

/**
 * Stateless folder editor: rename, recolor, cover icon, smart rules, lock,
 * pin-to-home, manual membership, and inline web-links. No ViewModel/Hilt, so
 * it's preview- and instrumented-test-friendly. All edits persist immediately
 * via the callbacks so a back-press never loses work.
 */
@Composable
fun FolderEditorContent(
    state: FolderEditorState,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onSetColor: (Int) -> Unit,
    onSetLocked: (Boolean) -> Unit,
    onPinToHome: () -> Boolean,
    onToggleMember: (String) -> Unit,
    onAddWebLink: (String, String) -> Unit,
    onSetCoverSymbol: (String) -> Unit,
    onClearCover: () -> Unit,
    onSetCoverImage: (Uri) -> Unit,
    onAddOrReplaceRule: (FolderRule) -> Unit,
    onRemoveRuleOfKind: ((FolderRule) -> Boolean) -> Unit,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nameDraft by remember { mutableStateOf("") }
    var showWebLinkDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pinUnsupported = stringResource(R.string.apps_pin_unsupported)
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { onSetCoverImage(it) } }

    LaunchedEffect(state.folder?.id, state.folder?.name) {
        state.folder?.let { if (nameDraft != it.name) nameDraft = it.name }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.folder?.name ?: stringResource(R.string.apps_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.apps_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        val current = state.folder
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GadgetTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it; onRename(it) },
                    label = stringResource(R.string.apps_folder_name),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { FolderColorSection(current.baseColorArgb, onSetColor) }
            item {
                CoverIconSection(
                    folder = current,
                    onPickImage = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onPickSymbol = onSetCoverSymbol,
                    onClear = onClearCover,
                )
            }
            item {
                RuleSection(
                    ruleSet = state.ruleSet,
                    onAddOrReplace = onAddOrReplaceRule,
                    onRemoveKind = onRemoveRuleOfKind,
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.apps_lock_folder),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(checked = current.locked, onCheckedChange = onSetLocked)
                }
            }
            item {
                GadgetSecondaryButton(
                    onClick = {
                        if (!onPinToHome()) {
                            scope.launch { snackbarHostState.showSnackbar(pinUnsupported) }
                        }
                    },
                    text = stringResource(R.string.apps_pin_to_home),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.apps_apps_in_folder),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GadgetSecondaryButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            context.startActivity(FolderPopupActivity.intent(context, current.id))
                        },
                        text = stringResource(R.string.apps_preview_folder),
                    )
                    GadgetSecondaryButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showWebLinkDialog = true },
                        text = stringResource(R.string.apps_add_web_link),
                    )
                }
            }
            item {
                GadgetTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = stringResource(R.string.apps_search_apps_hint),
                    leadingIcon = Icons.Filled.Search,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.filteredApps.isEmpty()) {
                item {
                    Text(
                        text = if (state.searchQuery.isBlank()) {
                            stringResource(R.string.apps_no_apps)
                        } else {
                            stringResource(R.string.apps_no_search_matches)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.filteredApps, key = { it.appKey }) { record ->
                    AppRow(
                        record = record,
                        selected = record.appKey in state.membership,
                        otherFolders = state.otherFolderMembership[record.appKey].orEmpty(),
                        onToggle = { onToggleMember(record.appKey) },
                    )
                }
            }
        }
    }

    if (showWebLinkDialog) {
        AddWebLinkDialog(
            onDismiss = { showWebLinkDialog = false },
            onConfirm = { url, label ->
                onAddWebLink(url, label)
                showWebLinkDialog = false
            },
        )
    }
}

@Composable
private fun CoverIconSection(
    folder: Folder,
    onPickImage: () -> Unit,
    onPickSymbol: (String) -> Unit,
    onClear: () -> Unit,
) {
    var showSymbols by remember { mutableStateOf(false) }
    val accent = Color(folder.baseColorArgb)
    val cover = folder.coverIcon
    val hasCustom = cover.startsWith("image:") || cover.startsWith("symbol:")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CoverPreview(coverIcon = cover, accent = accent, sizeDp = 40.dp)
            Spacer(Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.apps_cover_icon),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onPickImage) {
                Icon(Icons.Filled.AddPhotoAlternate, stringResource(R.string.apps_cover_pick_image))
            }
            IconButton(onClick = { showSymbols = !showSymbols }) {
                Icon(Icons.Filled.GridView, stringResource(R.string.apps_cover_pick_symbol))
            }
            if (hasCustom) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, stringResource(R.string.apps_cover_clear))
                }
            }
        }
        if (showSymbols) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MaterialSymbol.entries.forEach { sym ->
                    IconButton(onClick = { onPickSymbol(sym.id); showSymbols = false }) {
                        Icon(sym.icon, contentDescription = sym.id, tint = accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverPreview(coverIcon: String, accent: Color, sizeDp: Dp) {
    Box(
        modifier = Modifier.size(sizeDp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            coverIcon.startsWith("image:") -> {
                val path = coverIcon.removePrefix("image:")
                val bmp = remember(path) { runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(sizeDp).clip(CircleShape),
                    )
                } else {
                    Icon(Icons.Filled.AddPhotoAlternate, null, tint = accent)
                }
            }
            coverIcon.startsWith("symbol:") -> {
                val sym = MaterialSymbol.fromId(coverIcon.removePrefix("symbol:"))
                Icon(
                    imageVector = sym?.icon ?: Icons.Filled.Folder,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size((sizeDp.value * 0.6f).dp),
                )
            }
            else -> Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = accent.copy(alpha = 0.65f),
                modifier = Modifier.size((sizeDp.value * 0.6f).dp),
            )
        }
    }
}

@Composable
private fun RuleSection(
    ruleSet: FolderRuleSet,
    onAddOrReplace: (FolderRule) -> Unit,
    onRemoveKind: ((FolderRule) -> Boolean) -> Unit,
) {
    val active = ruleSet.rules
    val packagePrefix = active.firstOrNull { it is FolderRule.PackagePrefix } as? FolderRule.PackagePrefix
    val recently = active.firstOrNull { it is FolderRule.RecentlyInstalled } as? FolderRule.RecentlyInstalled
    val unused = active.firstOrNull { it is FolderRule.UnusedSinceDays } as? FolderRule.UnusedSinceDays

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.apps_rule),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        RuleCheckRow(
            checked = packagePrefix != null,
            label = stringResource(R.string.apps_rule_package_prefix),
            onToggle = { on ->
                if (on) onAddOrReplace(FolderRule.PackagePrefix("com.")) else onRemoveKind { it is FolderRule.PackagePrefix }
            },
        ) {
            packagePrefix?.let { r ->
                GadgetTextField(
                    value = r.prefix,
                    onValueChange = { onAddOrReplace(FolderRule.PackagePrefix(it)) },
                    label = stringResource(R.string.apps_rule_package_prefix),
                    placeholder = stringResource(R.string.apps_rule_package_prefix_hint),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        RuleCheckRow(
            checked = recently != null,
            label = stringResource(R.string.apps_rule_recently_installed),
            onToggle = { on ->
                if (on) onAddOrReplace(FolderRule.RecentlyInstalled(7)) else onRemoveKind { it is FolderRule.RecentlyInstalled }
            },
        ) {
            recently?.let { r ->
                DaysField(r.days, stringResource(R.string.apps_rule_days)) {
                    onAddOrReplace(FolderRule.RecentlyInstalled(it))
                }
            }
        }
        RuleCheckRow(
            checked = active.any { it is FolderRule.WebApkOnly },
            label = stringResource(R.string.apps_rule_web_apks),
            onToggle = { on -> if (on) onAddOrReplace(FolderRule.WebApkOnly) else onRemoveKind { it is FolderRule.WebApkOnly } },
        )
        RuleCheckRow(
            checked = unused != null,
            label = stringResource(R.string.apps_rule_unused),
            onToggle = { on ->
                if (on) onAddOrReplace(FolderRule.UnusedSinceDays(30)) else onRemoveKind { it is FolderRule.UnusedSinceDays }
            },
        ) {
            unused?.let { r ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DaysField(r.days, stringResource(R.string.apps_rule_days)) {
                        onAddOrReplace(FolderRule.UnusedSinceDays(it))
                    }
                    Text(
                        text = stringResource(R.string.apps_rule_usage_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        RuleCheckRow(
            checked = active.any { it is FolderRule.OnInternalStorage },
            label = stringResource(R.string.apps_rule_on_internal_storage),
            onToggle = { on -> if (on) onAddOrReplace(FolderRule.OnInternalStorage) else onRemoveKind { it is FolderRule.OnInternalStorage } },
        )
        RuleCheckRow(
            checked = active.any { it is FolderRule.OnExternalStorage },
            label = stringResource(R.string.apps_rule_on_external_storage),
            onToggle = { on -> if (on) onAddOrReplace(FolderRule.OnExternalStorage) else onRemoveKind { it is FolderRule.OnExternalStorage } },
        )
        RuleCheckRow(
            checked = active.any { it is FolderRule.SystemApps },
            label = stringResource(R.string.apps_rule_system_apps),
            onToggle = { on -> if (on) onAddOrReplace(FolderRule.SystemApps) else onRemoveKind { it is FolderRule.SystemApps } },
        )
        RuleCheckRow(
            checked = active.any { it is FolderRule.UserApps },
            label = stringResource(R.string.apps_rule_user_apps),
            onToggle = { on -> if (on) onAddOrReplace(FolderRule.UserApps) else onRemoveKind { it is FolderRule.UserApps } },
        )
        Text(
            text = stringResource(R.string.apps_rule_union_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun RuleCheckRow(
    checked: Boolean,
    label: String,
    onToggle: (Boolean) -> Unit,
    config: @Composable (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle(!checked) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = onToggle)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        if (checked && config != null) {
            Box(modifier = Modifier.padding(start = 48.dp)) { config() }
        }
    }
}

@Composable
private fun DaysField(value: Int, label: String, onChange: (Int) -> Unit) {
    var draft by remember(value) { mutableStateOf(value.toString()) }
    GadgetTextField(
        value = draft,
        onValueChange = {
            draft = it.filter { ch -> ch.isDigit() }.take(4)
            draft.toIntOrNull()?.let(onChange)
        },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Folder colour picker: a row of quick preset swatches for fast selection,
 * plus a full HSV + hex [GadgetColorPicker] behind a "Custom" toggle for
 * arbitrary colours. When a custom (non-preset) colour is active, a live
 * indicator swatch leads the row (since no preset shows selected then).
 */
@Composable
private fun FolderColorSection(selectedArgb: Int, onSelect: (Int) -> Unit) {
    var showCustom by rememberSaveable { mutableStateOf(false) }
    val swatches = remember {
        listOf(
            Color(0xFF6750A4), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFEF6C00),
            Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF00897B), Color(0xFF546E7A),
        )
    }
    val isPreset = swatches.any { it.toArgb() == selectedArgb }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.apps_color),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            GadgetChip(
                selected = showCustom,
                onClick = { showCustom = !showCustom },
                label = stringResource(R.string.apps_color_custom),
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!isPreset) {
                ColorSwatch(
                    color = Color(selectedArgb),
                    selected = true,
                    onClick = { showCustom = true },
                )
            }
            swatches.forEach { swatch ->
                val argb = swatch.toArgb()
                ColorSwatch(
                    color = swatch,
                    selected = argb == selectedArgb,
                    onClick = { onSelect(argb) },
                )
            }
        }
        if (showCustom) {
            GadgetColorPicker(
                argb = selectedArgb.toLong() and 0xFFFFFFFFL,
                onArgbChange = { onSelect((it and 0xFFFFFFFFL).toInt()) },
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AppRow(
    record: AppRecord,
    selected: Boolean,
    otherFolders: List<String>,
    onToggle: () -> Unit,
) {
    val subtitle = when {
        record.isWebLink -> stringResource(R.string.apps_web_link_badge)
        record.isWebApk -> stringResource(R.string.apps_pwa_badge)
        else -> record.packageName
    }
    val inOther = otherFolders.isNotEmpty()
    val alreadyIn = stringResource(R.string.apps_already_in_folder)
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        intensity = GlassIntensity.Subtle,
        onClick = onToggle,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(record = record, sizeDp = 36.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = record.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (inOther) {
                    Text(
                        text = "$alreadyIn ${otherFolders.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun FolderColorSectionPreview() = GadgetThemedPreview {
    FolderColorSection(selectedArgb = 0xFF1E88E5.toInt(), onSelect = {})
}

@Composable
private fun AddWebLinkDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.apps_add_web_link)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GadgetTextField(value = url, onValueChange = { url = it }, label = stringResource(R.string.apps_web_link_url))
                GadgetTextField(value = label, onValueChange = { label = it }, label = stringResource(R.string.apps_web_link_label))
            }
        },
        confirmButton = {
            TextButton(enabled = url.isNotBlank(), onClick = { onConfirm(url, label) }) {
                Text(stringResource(R.string.apps_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.apps_cancel)) } },
    )
}
