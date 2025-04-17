package com.akumm7491.pokedex.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonDetailResponse(
    val id: Int,
    val name: String,
    val height: Int, // In decimetres
    val weight: Int, // In hectograms
    val stats: List<PokemonStatInfo>,
    val types: List<PokemonTypeInfo>,
    val abilities: List<PokemonAbilityInfo>,
    val moves: List<PokemonMoveInfo>,
    val sprites: PokemonSprites
)

// Sub-Models for nested data
@Serializable
data class PokemonStatInfo(
    @SerialName("base_stat") val baseStat: Int,
    val effort: Int,
    val stat: PokemonResource
)

@Serializable
data class PokemonTypeInfo(
    val slot: Int,
    val type: PokemonResource
)

@Serializable
data class PokemonAbilityInfo(
    val ability: PokemonResource,
    @SerialName("is_hidden") val isHidden: Boolean,
    val slot: Int
)

@Serializable
data class PokemonMoveInfo(
    val move: PokemonResource
)

@Serializable
data class PokemonSprites(
    @SerialName("front_default") val frontDefault: String?,
    @SerialName("front_shiny") val frontShiny: String?,
    val other: PokemonOtherSprites?
)

@Serializable
data class PokemonOtherSprites(
    @SerialName("official-artwork") val officialArtwork: PokemonOfficialArtwork?
)

@Serializable
data class PokemonOfficialArtwork(
    @SerialName("front_default") val frontDefault: String?,
    @SerialName("front_shiny") val frontShiny: String?
)

@Serializable
data class PokemonResource(
    val name: String,
    val url: String
)