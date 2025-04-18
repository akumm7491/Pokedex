package com.akumm7491.pokedex.data.sources.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.akumm7491.pokedex.domain.models.PokemonListItem

@Entity(
    tableName = "pokemon",
    indices = [Index(value = ["page_url"])]
)
data class PokemonEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "page_url") val pageUrl: String
)

fun PokemonListItem.toEntity(pageUrl: String): PokemonEntity? {
    val pokemonId = this.id ?: return null
    return PokemonEntity(
        id = pokemonId,
        name = this.name,
        url = this.url,
        pageUrl = pageUrl
    )
}

fun PokemonEntity.toListItem(): PokemonListItem {
    return PokemonListItem(
        name = this.name,
        url = this.url
    )
}