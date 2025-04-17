package com.akumm7491.pokedex.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class PokemonListItem(
    val name: String,
    val url: String
) {
    // Helper to extract ID, assuming standard URL format like ".../pokemon/1/"
    val id: Int?
        get() = url.split("/").dropLast(1).lastOrNull()?.toIntOrNull()

    // Helper to construct the image URL
    val imageUrl: String?
        get() = id?.let { "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$it.png" }
}