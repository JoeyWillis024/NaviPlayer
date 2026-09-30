package com.antiwilly.naviplayer.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.antiwilly.naviplayer.core.model.PlaybackState
import com.antiwilly.naviplayer.core.model.Song
import com.antiwilly.naviplayer.core.model.Playlist
import com.antiwilly.naviplayer.feature.equalizer.EqualizerSheet
import com.antiwilly.naviplayer.feature.sleeptimer.SleepTimerSheet
import com.antiwilly.naviplayer.ui.components.SongListItem
import com.antiwilly.naviplayer.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackState,
    coverArtUrl: String?,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleStar: (Song) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onPresetSelected: (String) -> Unit,
    onEqualizerEnabledChanged: (Boolean) -> Unit,
    isEqualizerEnabled: Boolean,
    currentEqualizerPreset: String,
    currentBassBoost: Int,
    onBassBoostChanged: (Int) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSelectQueueItem: (Song) -> Unit,
    playlists: List<Playlist>,
    onAddToPlaylist: (Song, String, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableFloatStateOf(0f) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val currentPositionMs = if (isSeeking) {
        (seekProgress * playbackState.durationMs).toLong()
    } else {
        playbackState.currentPositionMs
    }

    val sliderValue = if (playbackState.durationMs > 0) {
        (currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            val artworkSize = minOf(maxWidth * 0.9f, maxHeight * 0.32f)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse Player",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(onClick = { showQueueSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Play Queue"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Artwork Card
                Box(
                    modifier = Modifier
                        .size(artworkSize)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!coverArtUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = coverArtUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(96.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Song Title & Artist + Star/Download Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { onDownloadSong(song) }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Song",
                            tint = if (song.isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showAddToPlaylistDialog = true }) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist")
                    }

                    IconButton(onClick = { onToggleStar(song) }) {
                        Icon(
                            imageVector = if (song.isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite Song",
                            tint = if (song.isStarred) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Slider & Timestamps
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isSeeking = true
                        seekProgress = it
                    },
                    onValueChangeFinished = {
                        isSeeking = false
                        onSeekTo((seekProgress * playbackState.durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration((currentPositionMs / 1000).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration((playbackState.durationMs / 1000).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.shuffleMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onSkipPrevious) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = onSkipNext) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = onCycleRepeat) {
                        val (icon, tint) = when (playbackState.repeatMode) {
                            1 -> Pair(Icons.Default.RepeatOne, MaterialTheme.colorScheme.primary)
                            2 -> Pair(Icons.Default.Repeat, MaterialTheme.colorScheme.primary)
                            else -> Pair(Icons.Default.Repeat, MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Tools: Equalizer & Sleep Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showEqualizerSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showSleepTimerSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (playbackState.sleepTimerRemainingSec != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Queue Sheet
    if (showQueueSheet) {
        ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Playing Queue (${playbackState.queue.size} songs)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    itemsIndexed(playbackState.queue) { _, item ->
                        SongListItem(
                            song = item,
                            coverArtUrl = null,
                            isPlaying = item.id == song.id,
                            onClick = {
                                onSelectQueueItem(item)
                                showQueueSheet = false
                            }
                        )
                    }
                }
            }
        }
    }

    // Equalizer Sheet
    if (showEqualizerSheet) {
        EqualizerSheet(
            onDismissRequest = { showEqualizerSheet = false },
            onPresetSelected = onPresetSelected,
            onBassBoostChanged = onBassBoostChanged,
            onEqualizerEnabledChanged = onEqualizerEnabledChanged,
            currentPreset = currentEqualizerPreset,
            currentBassBoost = currentBassBoost,
            isEqualizerEnabled = isEqualizerEnabled
        )
    }

    if (showAddToPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("Add to playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists available. Create a playlist in Library first.")
                } else {
                    LazyColumn {
                        itemsIndexed(playlists) { _, playlist ->
                            TextButton(
                                onClick = {
                                    onAddToPlaylist(song, playlist.id) { success ->
                                        if (success) showAddToPlaylistDialog = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(playlist.name) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) { Text("Close") }
            }
        )
    }

    // Sleep Timer Sheet
    if (showSleepTimerSheet) {
        SleepTimerSheet(
            onDismissRequest = { showSleepTimerSheet = false },
            onStartTimer = onStartSleepTimer,
            onCancelTimer = onCancelSleepTimer,
            activeRemainingSec = playbackState.sleepTimerRemainingSec
        )
    }
}
