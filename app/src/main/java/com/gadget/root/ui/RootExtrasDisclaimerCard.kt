package com.gadget.root.ui

import dev.ranzlappen.gadget.core.root.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shared disclaimer Card used by every Batch-6 / Batch-7 rooted-extras
 * section. Each surface passes context-appropriate body copy via
 * [text]; the title is fixed.
 */
@Composable
fun RootExtrasDisclaimerCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "⚠ Regulatory & safety notice",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
