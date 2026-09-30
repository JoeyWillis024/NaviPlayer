package com.antiwilly.naviplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.antiwilly.naviplayer.core.model.Playlist
import com.antiwilly.naviplayer.core.model.Song

@Composable
fun SongListItem(
    song: Song,
    coverArtUrl: String?,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onStarClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    playlists: List<Playlist> = emptyList(),
    onAddToPlaylist: ((Song, String, (Boolean) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (!coverArtUrl.isNullOrBlank()) {
                AsyncImage(
                    model = coverArtUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title & Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (song.artist.isNotBlank()) "${song.artist} • ${formatDuration(song.durationSec)}" else formatDuration(song.durationSec),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (onAddToPlaylist != null) {
            IconButton(onClick = { showPlaylistPicker = true }) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to playlist")
            }
        }

        // Download status / action
        if (song.isDownloaded) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Downloaded",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp)
            )
        } else if (onDownloadClick != null) {
            IconButton(onClick = onDownloadClick) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download song",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Star / Favorite
        if (onStarClick != null) {
            IconButton(onClick = onStarClick) {
                Icon(
                    imageVector = if (song.isStarred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Star song",
                    tint = if (song.isStarred) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            title = { Text("Add to playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("No playlists available. Create a playlist in Library first.")
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(playlists) { playlist ->
                            TextButton(
                                onClick = {
                                    onAddToPlaylist?.invoke(song, playlist.id) { success ->
                                        if (success) showPlaylistPicker = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(playlist.name) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistPicker = false }) { Text("Close") }
            }
        )
    }
}

fun formatDuration(durationSec: Int): String {
    val minutes = durationSec / 60
    val seconds = durationSec % 60
    return "%d:%02d".format(minutes, seconds)
}
