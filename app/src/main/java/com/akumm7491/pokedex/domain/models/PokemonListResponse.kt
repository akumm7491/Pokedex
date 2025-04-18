package com.akumm7491.pokedex.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class PokemonListResponse(
    val next: String?,
    val results: List<PokemonListItem>
)
