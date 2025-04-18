package com.akumm7491.pokedex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.akumm7491.pokedex.domain.models.PokemonDetailResponse
import com.akumm7491.pokedex.domain.models.PokemonStatInfo
import com.akumm7491.pokedex.ui.viewmodels.PokemonDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonDetailScreen(
    viewModel: PokemonDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.pokemon?.name?.replaceFirstChar { it.titlecase() } ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()
                state.error != null -> ErrorDetailState(
                    errorMessage = state.error ?: "Unknown Error",
                    onRetry = { viewModel.fetchDetails() }
                )
                state.pokemon != null -> PokemonDetailContent(pokemon = state.pokemon!!)
            }
        }
    }
}

@Composable
fun PokemonDetailContent(pokemon: PokemonDetailResponse) {
    val movesGridColumns = 2
    val movesToShow = 12

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Top section with name, id and types
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(pokemon.sprites.other?.officialArtwork?.frontDefault ?: pokemon.sprites.frontDefault)
                        .crossfade(true)
                        .build(),
                    contentDescription = "${pokemon.name} image",
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .aspectRatio(1f)
                        .padding(bottom = 16.dp),
                    contentScale = ContentScale.Fit,
                    loading = { CircularProgressIndicator() },
                    error = { Icon(Icons.Filled.BrokenImage, "Error") }
                )

                Text(
                    text = pokemon.name.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "#${pokemon.id.toString().padStart(4, '0')}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pokemon.types.forEach { typeInfo ->
                        AssistChip(
                            onClick = { /* No action */ },
                            label = { Text(typeInfo.type.name.replaceFirstChar { it.titlecase() }) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        // Stats Section
        item {
            DetailSectionTitle("Base Stats")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pokemon.stats.forEach { statInfo ->
                    PokemonStat(statInfo)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        // Basic Info Section (Height/Weight)
        item {
            DetailSectionTitle("Basic Info")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BasicInfoItem("Height", "${pokemon.height / 10.0} m") // Convert decimetres to metres
                BasicInfoItem("Weight", "${pokemon.weight / 10.0} kg") // Convert hectograms to kg
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        // Moves Section (Limit displayed moves for brevity)
        item { DetailSectionTitle("Moves") }

        // Chunk the moves list into rows based on the number of columns
        val moveRows = pokemon.moves.take(movesToShow).chunked(movesGridColumns)
        items(items = moveRows, key = { row -> row.joinToString { it.move.name } }) { moveRow ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Display each move in the current row
                moveRow.forEach { moveInfo ->
                    Text(
                        text = moveInfo.move.name.replace('-', ' ').replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    )
                }

                // Add Spacers to fill the remaining space if the last row is not full
                // This ensures items in the last row align correctly under previous columns
                if (moveRow.size < movesGridColumns) {
                    for (i in 0 until (movesGridColumns - moveRow.size)) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun PokemonStat(statInfo: PokemonStatInfo) {
    val maxStatValue = 200f
    val statProgress = (statInfo.baseStat / maxStatValue).coerceIn(0f, 1f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = statInfo.stat.name.replaceFirstChar { it.titlecase() }.replace("-", " "),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(100.dp)
        )
        LinearProgressIndicator(
            progress = { statProgress },
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = statInfo.baseStat.toString(),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp)
        )
    }
}

@Composable
fun BasicInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}


@Composable
fun ErrorDetailState(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Failed to load details",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.Red
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}