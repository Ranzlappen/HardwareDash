package com.gadget.ui.onboarding

import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gadget.localization.Language
import com.gadget.localization.LocalizationManager
import com.gadget.localization.S
import com.gadget.permissions.PermissionsOnboardingCoordinator
import com.gadget.permissions.SpecialPermissionStep
import com.gadget.permissions.rememberPermissionsResumeAdvancer
import com.gadget.root.RootFeaturesEntryPoint
import com.gadget.root.ui.RootedFirstAckDialog
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

private sealed class OnboardingPage {
    data class Standard(
        val icon: ImageVector,
        val title: @Composable () -> String,
        val description: @Composable () -> String,
    ) : OnboardingPage()

    data object RootedLegal : OnboardingPage()
    data object BackgroundBehavior : OnboardingPage()
    data object Permissions : OnboardingPage()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = S.onboarding
    val lang = LocalizationManager.loadLanguage(context)

    val rootEntryPoint = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            RootFeaturesEntryPoint::class.java,
        )
    }
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(rootEntryPoint) {
        rootAvailable = rootEntryPoint.capabilityRegistry().hasRootAccess()
    }

    val rootToggles = rootEntryPoint.featureToggles()
    val acknowledged by rootToggles.isRootedAcknowledged().collectAsState(initial = false)
    var checkboxTicked by rememberSaveable { mutableStateOf(false) }
    val effectiveAck = acknowledged || checkboxTicked

    // First-launch ack dialog. Renders only on rooted+!ack; no-op on standard.
    RootedFirstAckDialog()

    val standardPages = listOf(
        OnboardingPage.Standard(
            icon = Icons.Default.Dashboard,
            title = { strings.welcomeTitle },
            description = { strings.welcomeDesc },
        ),
        OnboardingPage.Standard(
            icon = Icons.Default.Build,
            title = { strings.toolsTitle },
            description = { strings.toolsDesc },
        ),
        OnboardingPage.Standard(
            icon = Icons.Default.Analytics,
            title = { strings.monitorTitle },
            description = { strings.monitorDesc },
        ),
        OnboardingPage.Standard(
            icon = Icons.Default.CheckCircle,
            title = { strings.logbookTitle },
            description = { strings.logbookDesc },
        ),
        OnboardingPage.Standard(
            icon = Icons.Default.Explore,
            title = { strings.moreTitle },
            description = { strings.moreDesc },
        ),
    )

    val pages: List<OnboardingPage> = buildList {
        if (rootAvailable) add(OnboardingPage.RootedLegal)
        addAll(standardPages)
        add(OnboardingPage.BackgroundBehavior)
        add(OnboardingPage.Permissions)
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    val finishedAndComplete: () -> Unit = {
        if (rootAvailable && checkboxTicked && !acknowledged) {
            scope.launch { rootToggles.setRootedAcknowledged(true) }
        }
        markOnboardingComplete(context)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Skip button — never shows on the rooted Legal page.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            val onLast = pagerState.currentPage == pages.lastIndex
            val onRootedLegal = pages.getOrNull(pagerState.currentPage) is OnboardingPage.RootedLegal
            // Hide Skip on the last page, the rooted Legal page, and for any
            // rooted user who hasn't yet acknowledged — Skip would otherwise
            // bypass the legal gate.
            val rootedGateBlocking = rootAvailable && !effectiveAck
            if (!onLast && !onRootedLegal && !rootedGateBlocking) {
                TextButton(onClick = finishedAndComplete) {
                    Text(strings.skip)
                }
            }
        }

        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (val current = pages[page]) {
                is OnboardingPage.Standard -> StandardPage(current)
                OnboardingPage.RootedLegal -> RootedLegalPage(
                    checkboxTicked = effectiveAck,
                    onCheckboxToggle = { checkboxTicked = it },
                )
                OnboardingPage.BackgroundBehavior -> BackgroundBehaviorPage(lang)
                OnboardingPage.Permissions -> PermissionsPage(lang)
            }
        }

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            repeat(pages.size) { index ->
                val color by animateColorAsState(
                    if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    label = "indicator",
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        // Bottom button
        if (pagerState.currentPage == pages.lastIndex) {
            val gateOpen = !rootAvailable || effectiveAck
            Button(
                onClick = finishedAndComplete,
                enabled = gateOpen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(strings.getStarted, style = MaterialTheme.typography.titleMedium)
            }
            if (!gateOpen) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = S.OnboardingExtras.rootedLegalGetStartedDisabledHint(lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(strings.next, style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StandardPage(page: OnboardingPage.Standard) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RootedLegalPage(
    checkboxTicked: Boolean,
    onCheckboxToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lang = LocalizationManager.loadLanguage(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = S.OnboardingExtras.rootedLegalPageTitle(lang),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = S.RootedLegalNotice.cardTitle(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = S.RootedLegalNotice.body(lang),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = S.OnboardingExtras.rootedLegalRisksHeader(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                RiskBullet(S.OnboardingExtras.rootedLegalRiskThermal(lang))
                RiskBullet(S.OnboardingExtras.rootedLegalRiskRegulatory(lang))
                RiskBullet(S.OnboardingExtras.rootedLegalRiskBattery(lang))
                RiskBullet(S.OnboardingExtras.rootedLegalRiskHearing(lang))
                RiskBullet(S.OnboardingExtras.rootedLegalRiskUx(lang))
                RiskBullet(S.OnboardingExtras.rootedLegalRiskOther(lang))
            }
        }
        Text(
            text = S.OnboardingExtras.rootedLegalEmergencyResetReminder(lang),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = checkboxTicked,
                onCheckedChange = onCheckboxToggle,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = S.OnboardingExtras.rootedLegalAcknowledgeCheckbox(lang),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun RiskBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun BackgroundBehaviorPage(lang: Language) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = S.OnboardingExtras.backgroundBehaviorTitle(lang),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = S.OnboardingExtras.backgroundBehaviorBody(lang),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = S.OnboardingExtras.privacyNote(lang),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PermissionsPage(lang: Language) {
    val context = LocalContext.current
    var pendingSpecial by remember { mutableStateOf<List<SpecialPermissionStep>>(emptyList()) }
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }

    val awaitingResume = rememberPermissionsResumeAdvancer(onResume = {
        val refreshed = PermissionsOnboardingCoordinator.pendingSpecialSteps(context)
        pendingSpecial = refreshed
        if (refreshed.isEmpty()) {
            status = S.PermissionsOnboarding.complete(lang)
        } else {
            stepIndex = 0
            status = S.PermissionsOnboarding.progress(lang, 1, refreshed.size)
            launchNextSpecialStep(context, refreshed, 0)
        }
    })

    val runtimeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        pendingSpecial = PermissionsOnboardingCoordinator.pendingSpecialSteps(context)
        stepIndex = 0
        status = if (pendingSpecial.isEmpty()) {
            S.PermissionsOnboarding.complete(lang)
        } else {
            S.PermissionsOnboarding.progress(lang, 1, pendingSpecial.size)
        }
        if (pendingSpecial.isNotEmpty()) {
            launchNextSpecialStep(context, pendingSpecial, stepIndex)
            awaitingResume.value = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = S.OnboardingExtras.permissionsTitle(lang),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = S.OnboardingExtras.permissionsBody(lang),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                val missing = PermissionsOnboardingCoordinator.missingRuntimePermissions(context)
                if (missing.isEmpty()) {
                    pendingSpecial = PermissionsOnboardingCoordinator.pendingSpecialSteps(context)
                    stepIndex = 0
                    status = if (pendingSpecial.isEmpty()) {
                        S.PermissionsOnboarding.complete(lang)
                    } else {
                        S.PermissionsOnboarding.progress(lang, 1, pendingSpecial.size)
                    }
                    if (pendingSpecial.isNotEmpty()) {
                        launchNextSpecialStep(context, pendingSpecial, stepIndex)
                        awaitingResume.value = true
                    }
                } else {
                    runtimeLauncher.launch(missing.toTypedArray())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(S.OnboardingExtras.permissionsGrantAll(lang))
        }
        status?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = S.OnboardingExtras.privacyNote(lang),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }

    LaunchedEffect(stepIndex, pendingSpecial.size) {
        if (pendingSpecial.isNotEmpty() && stepIndex >= pendingSpecial.size) {
            status = S.PermissionsOnboarding.complete(lang)
        }
    }
}

private fun launchNextSpecialStep(
    context: Context,
    steps: List<SpecialPermissionStep>,
    index: Int,
) {
    if (index !in steps.indices) return
    val intent = steps[index].buildIntent(context) ?: return
    runCatching { context.startActivity(intent) }
        .onFailure {
            if (it is ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "Settings activity not found for ${steps[index].id}",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
}

private fun markOnboardingComplete(context: Context) {
    context.getSharedPreferences("gadget_settings", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("has_seen_onboarding", true)
        .apply()
}
