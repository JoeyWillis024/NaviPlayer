package com.antiwilly.naviplayer.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.antiwilly.naviplayer.core.model.Album
import com.antiwilly.naviplayer.core.model.ServerProfile
import com.antiwilly.naviplayer.core.network.SubsonicUrlHelper
import com.antiwilly.naviplayer.ui.components.AlbumCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    activeProfile: ServerProfile?,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val randomAlbums by viewModel.randomAlbums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    if (activeProfile == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No server configured. Go to Settings to connect to your Navidrome server.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    if (isLoading && recentlyAdded.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Recently Added Section
        if (recentlyAdded.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently Added")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentlyAdded) { album ->
                        val coverUrl = album.coverArtId?.let {
                            SubsonicUrlHelper.buildCoverArtUrl(activeProfile, it)
                        }
                        AlbumCard(
                            album = album,
                            coverArtUrl = coverUrl,
                            onClick = { onAlbumClick(album) },
                            onPlayClick = { viewModel.playAlbum(album) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Most Played Section
        if (mostPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Most Played")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mostPlayed) { album ->
                        val coverUrl = album.coverArtId?.let {
                            SubsonicUrlHelper.buildCoverArtUrl(activeProfile, it)
                        }
                        AlbumCard(
                            album = album,
                            coverArtUrl = coverUrl,
                            onClick = { onAlbumClick(album) },
                            onPlayClick = { viewModel.playAlbum(album) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Random Discoveries Section
        if (randomAlbums.isNotEmpty()) {
            item {
                SectionHeader(title = "Discover Something New")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(randomAlbums) { album ->
                        val coverUrl = album.coverArtId?.let {
                            SubsonicUrlHelper.buildCoverArtUrl(activeProfile, it)
                        }
                        AlbumCard(
                            album = album,
                            coverArtUrl = coverUrl,
                            onClick = { onAlbumClick(album) },
                            onPlayClick = { viewModel.playAlbum(album) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
