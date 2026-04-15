package com.gadget.ui.onboarding

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gadget.localization.S
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: @Composable () -> String,
    val description: @Composable () -> String,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = S.onboarding

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Dashboard,
            title = { strings.welcomeTitle },
            description = { strings.welcomeDesc },
        ),
        OnboardingPage(
            icon = Icons.Default.Build,
            title = { strings.toolsTitle },
            description = { strings.toolsDesc },
        ),
        OnboardingPage(
            icon = Icons.Default.Analytics,
            title = { strings.monitorTitle },
            description = { strings.monitorDesc },
        ),
        OnboardingPage(
            icon = Icons.Default.CheckCircle,
            title = { strings.logbookTitle },
            description = { strings.logbookDesc },
        ),
        OnboardingPage(
            icon = Icons.Default.Explore,
            title = { strings.moreTitle },
            description = { strings.moreDesc },
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            if (pagerState.currentPage < pages.lastIndex) {
                TextButton(onClick = {
                    markOnboardingComplete(context)
                    onComplete()
                }) {
                    Text(strings.skip)
                }
            }
        }

        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = pages[page].icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = pages[page].title(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = pages[page].description(),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    label = "indicator"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // Bottom button
        if (pagerState.currentPage == pages.lastIndex) {
            Button(
                onClick = {
                    markOnboardingComplete(context)
                    onComplete()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(strings.getStarted, style = MaterialTheme.typography.titleMedium)
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

private fun markOnboardingComplete(context: Context) {
    context.getSharedPreferences("gadget_settings", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("has_seen_onboarding", true)
        .apply()
}
