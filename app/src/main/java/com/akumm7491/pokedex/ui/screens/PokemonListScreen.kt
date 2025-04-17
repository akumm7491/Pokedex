package com.akumm7491.pokedex.ui.screens

import android.media.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.akumm7491.pokedex.R
import com.akumm7491.pokedex.domain.models.PokemonListItem
import com.akumm7491.pokedex.ui.components.InfiniteScrollList
import com.akumm7491.pokedex.ui.theme.PokedexTheme
import com.akumm7491.pokedex.ui.viewmodels.PokemonListIntent
import com.akumm7491.pokedex.ui.viewmodels.PokemonListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonListScreen(
    viewModel: PokemonListViewModel = hiltViewModel(),
    onPokemonClick: (Int) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val filteredItems by viewModel.filteredPokemonList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pokédex") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // --- Search Bar ---
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { query ->
                    viewModel.processIntent(PokemonListIntent.UpdateSearchQuery(query))
                },
                label = { Text("Search Name or Number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search Icon")
                },
                trailingIcon = {
                    // Show clear button only if query is not empty
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.processIntent(PokemonListIntent.UpdateSearchQuery("")) }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )

            // --- Content Area ---
            Box(modifier = Modifier.weight(1f)) { // Box takes remaining space

                // Determine UI state based on loading, error, and filtered items
                val showEmptySearch = filteredItems.isEmpty() && state.searchQuery.isNotEmpty() && !state.isLoadingInitial && state.error == null
                val showEmptyInitial = filteredItems.isEmpty() && state.searchQuery.isBlank() && !state.isLoadingInitial && state.error == null && state.items.isNotEmpty()
                val showErrorFullScreen = state.error != null && filteredItems.isEmpty() && !state.isLoadingInitial

                when {
                    // 1. Full Screen Error (if error occurred before any items were loaded/filtered)
                    showErrorFullScreen -> {
                        ErrorState(
                            errorMessage = state.error ?: "An unknown error occurred.",
                            onRetry = { viewModel.processIntent(PokemonListIntent.RetryInitialLoad) }
                        )
                    }
                    // 2. Empty state when search yields no results
                    showEmptySearch -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No Pokémon found matching '${state.searchQuery}'", textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                        }
                    }
                    // 3. Optional: Handle case where initial load finished but list is somehow empty
                    showEmptyInitial -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No Pokémon available.", textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                        }
                    }
                    // 4. Default: Show the list
                    else -> {
                        InfiniteScrollList(
                            modifier = Modifier.fillMaxSize(),
                            items = filteredItems,
                            itemKey = { pokemon -> pokemon.id ?: pokemon.url },
                            itemContent = { pokemon ->
                                PokemonItem(
                                    item = pokemon,
                                    modifier = Modifier.clickable {
                                        pokemon.id?.let { id -> onPokemonClick(id) }
                                    }
                                )
                            },
                            isLoadingMore = state.isLoadingMore,
                            // Only allow loading more if NOT searching and API indicates more are available
                            canLoadMore = state.canLoadMore && state.searchQuery.isBlank(),
                            onLoadMore = {
                                // Only trigger load more if search is not active
                                if (state.searchQuery.isBlank()) {
                                    viewModel.processIntent(PokemonListIntent.LoadMoreItems)
                                }
                            },
                        )

                        // Overlay small error at the bottom if loading more fails while items are shown
                        if (state.error != null && filteredItems.isNotEmpty() && !showErrorFullScreen) {
                            Text(
                                text = "Error loading more: ${state.error}",
                                color = Color.Red,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter) // Align error to bottom
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                    .padding(8.dp)
                            )
                        }
                    }
                }

                // Overlay: Initial Loading Spinner (shows only during the very first load)
                // Check filteredItems empty as well, so it doesn't overlay "No Results" state
                if (state.isLoadingInitial && filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun PokemonItem(
    item: PokemonListItem,
    modifier: Modifier = Modifier.fillMaxSize()
) {

    val errorDrawableIds = remember {
        listOf(
            R.drawable.crying_1,
            R.drawable.crying_2,
            R.drawable.crying_3
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium) // Add border
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "${item.name} image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                contentScale = ContentScale.Fit,

                // Loading state Composable
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    }
                },

                // Error state Composable
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        val randomErrorDrawableId = remember { errorDrawableIds.random() }

                        // Use a random error image if loading failed.
                        Image(
                            painter = painterResource(id = randomErrorDrawableId),
                            contentDescription = "Error loading image placeholder",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
            )
            HorizontalDivider() // Add a divider between image and text

            // Column for Text content below the image
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = item.name.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "#${item.id?.toString()?.padStart(4, '0') ?: "???"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

}

@Composable
fun ErrorState(errorMessage: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Oops! Something went wrong.",
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

@Preview(showBackground = true)
@Composable
fun PokemonItemPreview() {
    PokedexTheme {
        PokemonItem(
            item = PokemonListItem(
                name = "bulbasaur",
                url = "https://pokeapi.co/api/v2/pokemon/1/"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PokemonItemWithErrorPreview() {
    PokedexTheme {
        PokemonItem(
            item = PokemonListItem(
                name = "missingno",
                url = "invalid-url/0/"
            ),
            Modifier.fillMaxSize()
        )
    }
}