package com.gadget.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gadget.apps.icons.AppIcon
import com.gadget.apps.rules.FolderRule
import com.gadget.data.db.apps.AppRecord
import com.gadget.localization.S
import com.gadget.ui.folder.FolderPopupActivity
import kotlinx.coroutines.launch

/**
 * Per-folder editor: rename, recolor, toggle which apps belong, and add
 * web-link "apps" inline. All edits persist immediately so back-pressing
 * never loses work. Rule editor + cover-icon picker land in 6d.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderEditorScreen(
    onBack: () -> Unit,
) {
    val viewModel = hiltViewModel<FolderEditorViewModel>()
    val folder by viewModel.folder.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val membership by viewModel.membership.collectAsState()
    val rule by viewModel.rule.collectAsState()
    val apps = S.apps
    val common = S.common

    var nameDraft by remember { mutableStateOf("") }
    var showWebLinkDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(folder?.id, folder?.name) {
        folder?.let { if (nameDraft != it.name) nameDraft = it.name }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: apps.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = common.cancel,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        if (folder == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = apps.title, style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = {
                        nameDraft = it
                        viewModel.rename(it)
                    },
                    label = { Text(apps.folderName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                ColorPickerRow(
                    selectedArgb = folder!!.baseColorArgb,
                    onSelect = viewModel::setBaseColor,
                )
            }
            item {
                RuleSection(
                    rule = rule,
                    onSetRule = viewModel::setRule,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = apps.lockFolder,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(
                        checked = folder!!.locked,
                        onCheckedChange = viewModel::setLocked,
                    )
                }
            }
            item {
                OutlinedButton(
                    onClick = {
                        if (!viewModel.pinToHome()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(apps.pinUnsupported)
                            }
                        }
                    },
                ) { Text(apps.pinToHome) }
            }
            item {
                Spacer(Modifier.height(4.dp))
                // Section header on its own line — the previous Row collapsed
                // this Text into a 1-character-wide column when the two
                // trailing buttons claimed all the horizontal space.
                Text(
                    text = apps.appsInFolder,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            folder?.let {
                                context.startActivity(FolderPopupActivity.intent(context, it.id))
                            }
                        },
                    ) { Text(apps.previewFolder) }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { showWebLinkDialog = true },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(apps.addWebLink)
                    }
                }
            }
            if (rule !is FolderRule.Manual) {
                // Smart rules compute their own membership; manual toggles
                // would be misleading. Tap "Preview" to see what the rule
                // currently materializes.
            } else if (allApps.isEmpty()) {
                item {
                    Text(
                        text = apps.noApps,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(allApps, key = { it.appKey }) { record ->
                    AppRow(
                        record = record,
                        selected = record.appKey in membership,
                        onToggle = { viewModel.toggleMember(record.appKey) },
                    )
                }
            }
        }
    }

    if (showWebLinkDialog) {
        AddWebLinkDialog(
            onDismiss = { showWebLinkDialog = false },
            onConfirm = { url, label ->
                viewModel.addWebLink(url, label)
                showWebLinkDialog = false
            },
        )
    }
}

@Composable
private fun RuleSection(
    rule: FolderRule,
    onSetRule: (FolderRule) -> Unit,
) {
    val apps = S.apps
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = apps.rule,
            style = MaterialTheme.typography.titleSmall,
        )
        // 5-way segmented selector. Use a Row of OutlinedButton "chips" since
        // SegmentedButton requires width constraints and overflows on phones.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RuleChip(label = apps.ruleManual, selected = rule is FolderRule.Manual,
                onClick = { onSetRule(FolderRule.Manual) })
            RuleChip(label = apps.rulePackagePrefix, selected = rule is FolderRule.PackagePrefix,
                onClick = { onSetRule(FolderRule.PackagePrefix("com.")) })
            RuleChip(label = apps.ruleRecentlyInstalled, selected = rule is FolderRule.RecentlyInstalled,
                onClick = { onSetRule(FolderRule.RecentlyInstalled(7)) })
            RuleChip(label = apps.ruleWebApks, selected = rule is FolderRule.WebApkOnly,
                onClick = { onSetRule(FolderRule.WebApkOnly) })
            RuleChip(label = apps.ruleUnused, selected = rule is FolderRule.UnusedSinceDays,
                onClick = { onSetRule(FolderRule.UnusedSinceDays(30)) })
        }
        when (val r = rule) {
            is FolderRule.PackagePrefix -> {
                OutlinedTextField(
                    value = r.prefix,
                    onValueChange = { onSetRule(FolderRule.PackagePrefix(it)) },
                    singleLine = true,
                    label = { Text(apps.rulePackagePrefix) },
                    placeholder = { Text(apps.rulePackagePrefixHint) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is FolderRule.RecentlyInstalled -> {
                DaysField(
                    value = r.days,
                    label = "${apps.ruleRecentlyInstalled} (${apps.ruleDays})",
                    onChange = { onSetRule(FolderRule.RecentlyInstalled(it)) },
                )
            }
            is FolderRule.UnusedSinceDays -> {
                DaysField(
                    value = r.days,
                    label = "${apps.ruleUnused} (${apps.ruleDays})",
                    onChange = { onSetRule(FolderRule.UnusedSinceDays(it)) },
                )
                Text(
                    text = apps.ruleUsageHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FolderRule.Manual,
            FolderRule.WebApkOnly -> Unit
        }
    }
}

@Composable
private fun RuleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val onContainer = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = onContainer,
        )
    }
}

@Composable
private fun DaysField(value: Int, label: String, onChange: (Int) -> Unit) {
    var draft by remember(value) { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = draft,
        onValueChange = {
            draft = it.filter { ch -> ch.isDigit() }.take(4)
            draft.toIntOrNull()?.let(onChange)
        },
        singleLine = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColorPickerRow(
    selectedArgb: Int,
    onSelect: (Int) -> Unit,
) {
    val swatches = remember {
        listOf(
            Color(0xFF6750A4), // Material primary purple
            Color(0xFF1E88E5), // Blue
            Color(0xFF43A047), // Green
            Color(0xFFEF6C00), // Orange
            Color(0xFFE53935), // Red
            Color(0xFF8E24AA), // Magenta
            Color(0xFF00897B), // Teal
            Color(0xFF546E7A), // Slate
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = S.apps.color,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.CenterVertically),
        )
        swatches.forEach { swatch ->
            val argb = swatch.toArgb()
            val isSelected = argb == selectedArgb
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    record: AppRecord,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(record = record, sizeDp = 36.dp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val badge = when {
                    record.isWebLink -> S.apps.webLinkBadge
                    record.isWebApk -> S.apps.pwaBadge
                    else -> record.packageName
                }
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun AddWebLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    val apps = S.apps
    val common = S.common

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(apps.addWebLink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    singleLine = true,
                    label = { Text(apps.webLinkUrl) },
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    label = { Text(apps.webLinkLabel) },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank(),
                onClick = { onConfirm(url, label) },
            ) { Text(common.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(common.cancel) }
        },
    )
}
