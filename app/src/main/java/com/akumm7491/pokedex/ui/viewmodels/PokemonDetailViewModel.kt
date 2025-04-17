package com.akumm7491.pokedex.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akumm7491.pokedex.domain.models.PokemonDetailResponse
import com.akumm7491.pokedex.domain.repositories.PokemonRepository
import com.akumm7491.pokedex.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// State for the Detail Screen
data class PokemonDetailState(
    val isLoading: Boolean = true,
    val pokemon: PokemonDetailResponse? = null,
    val error: String? = null
)

@HiltViewModel
class PokemonDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PokemonRepository
) : ViewModel() {

    private val pokemonId: Int = checkNotNull(savedStateHandle[Screen.PokemonDetail.ARG_POKEMON_ID])

    private val _state = MutableStateFlow(PokemonDetailState())
    val state: StateFlow<PokemonDetailState> = _state.asStateFlow()

    init {
        fetchDetails()
    }

    fun fetchDetails() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) } // Start loading

            repository.fetchPokemonDetail(pokemonId.toString())
                .onSuccess { detailResponse ->
                    _state.update {
                        it.copy(isLoading = false, pokemon = detailResponse)
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to load details")
                    }
                }
        }
    }
}