package com.wfx.warungpos.feature.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SyncStatusBar(
    viewModel: SyncViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnimatedVisibility(visible = state != SyncBarState.HIDDEN) {
        val (bgColor, message) = when (state) {
            SyncBarState.SYNCING -> Color(0xFFFFA000) to "Menyinkronkan data... / Syncing..."
            SyncBarState.OFFLINE -> MaterialTheme.colorScheme.error to "Offline — data tersimpan lokal / Offline — saved locally"
            SyncBarState.HIDDEN -> Color.Transparent to ""
        }
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .padding(vertical = 6.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}
