package com.akumm7491.pokedex.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akumm7491.pokedex.domain.models.PokemonListItem
import com.akumm7491.pokedex.domain.repositories.PokemonRepository
import com.akumm7491.pokedex.utils.Constants.BASE_URL
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


// Single State Data Class
data class PokemonListState(
    val items: List<PokemonListItem> = emptyList(),
    val isLoadingInitial: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val nextUrl: String? = BASE_URL,
    val canLoadMore: Boolean = true,
    val searchQuery: String = ""
)

// Intents (User Actions / Events)
sealed interface PokemonListIntent {
    data object LoadInitialList : PokemonListIntent
    data object LoadMoreItems : PokemonListIntent
    data object ClearError : PokemonListIntent
    data object RetryInitialLoad : PokemonListIntent
    data class UpdateSearchQuery(val query: String) : PokemonListIntent
}


@HiltViewModel
class PokemonListViewModel @Inject constructor(
    private val repository: PokemonRepository
) : ViewModel() {

    private val _intentChannel = Channel<PokemonListIntent>(Channel.UNLIMITED)
    private val intentFlow = _intentChannel.receiveAsFlow()

    private val _state = MutableStateFlow(PokemonListState())
    val state: StateFlow<PokemonListState> = _state.asStateFlow()

    val filteredPokemonList: StateFlow<List<PokemonListItem>> = createFilteredListFlow()

    init {
        handleIntents()
        // Trigger initial load automatically on ViewModel creation
        processIntent(PokemonListIntent.LoadInitialList)
    }

    fun processIntent(intent: PokemonListIntent) {
        viewModelScope.launch {
            _intentChannel.send(intent)
        }
    }

    private fun handleIntents() {
        viewModelScope.launch {
            intentFlow.collect { intent ->
                when (intent) {
                    is PokemonListIntent.LoadInitialList -> loadData(isInitialLoad = true, isRetry = false)
                    is PokemonListIntent.LoadMoreItems -> loadData(isInitialLoad = false, isRetry = false)
                    is PokemonListIntent.RetryInitialLoad -> loadData(isInitialLoad = true, isRetry = true)
                    is PokemonListIntent.ClearError -> updateState { copy(error = null) }
                    is PokemonListIntent.UpdateSearchQuery -> updateState { copy(searchQuery = intent.query) }
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun createFilteredListFlow(): StateFlow<List<PokemonListItem>> {
        return state
            .map { Triple(it.items, it.searchQuery, it.isLoadingInitial) }
            .distinctUntilChanged { old, new -> old.first == new.first && old.second == new.second }
            .debounce { (_, query, isLoading) -> if (!isLoading && query.isNotEmpty()) 300L else 0L }
            .map { (items, query, _) ->
                filterPokemon(items, query)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )
    }

    private fun filterPokemon(items: List<PokemonListItem>, query: String): List<PokemonListItem> {
        return if (query.isBlank()) {
            items
        } else {
            items.filter { item ->
                val queryLower = query.lowercase()
                item.name.lowercase().contains(queryLower) ||
                        item.id?.toString() == query
            }
        }
    }

    private fun loadData(isInitialLoad: Boolean, isRetry: Boolean) {
        // Prevent multiple simultaneous fetches or fetching when not needed
        if (state.value.isLoadingInitial || state.value.isLoadingMore) return
        if (!isInitialLoad && !state.value.canLoadMore) return
        if (!isRetry && isInitialLoad && state.value.items.isNotEmpty()) return // Don't initial load if already have items unless retrying

        val urlToFetch = if (isInitialLoad) {
            // Always use the initial URL for initial load or retry
            _state.value.copy().nextUrl ?: BASE_URL // Use state's nextUrl or default if null
        } else {
            state.value.nextUrl ?: return // If loading more, must have a nextUrl
        }

        viewModelScope.launch {
            // Update loading state
            updateState {
                copy(
                    isLoadingInitial = isInitialLoad,
                    isLoadingMore = !isInitialLoad,
                    error = null // Clear previous error on new attempt
                )
            }

            repository.fetchPokemon(urlToFetch)
                .onSuccess { response ->
                    updateState {
                        val updatedList = if (isInitialLoad) response.results else this.items + response.results
                        copy(
                            items = updatedList,
                            nextUrl = response.next,
                            canLoadMore = response.next != null,
                            isLoadingInitial = false,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure { error ->
                    updateState {
                        copy(
                            error = error.message ?: "An unknown error occurred",
                            isLoadingInitial = false,
                            isLoadingMore = false
                        )
                    }
                }
        }
    }

    // Helper to update state immutably (ensures state consistency)
    private fun updateState(handler: PokemonListState.() -> PokemonListState) {
        _state.update(handler) // Use update for thread-safe updates
    }
}