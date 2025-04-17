package com.akumm7491.pokedex.domain.repositories

import com.akumm7491.pokedex.domain.models.PokemonDetailResponse
import com.akumm7491.pokedex.domain.models.PokemonListResponse

interface PokemonRepository {
    suspend fun fetchPokemon(url: String): Result<PokemonListResponse>
    suspend fun fetchPokemonDetail(idOrName: String): Result<PokemonDetailResponse>
}
