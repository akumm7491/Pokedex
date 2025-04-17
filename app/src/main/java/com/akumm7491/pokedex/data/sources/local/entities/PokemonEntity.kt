package com.akumm7491.pokedex.data.sources.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.akumm7491.pokedex.domain.models.PokemonListItem

@Entity(
    tableName = "pokemon",
    // Index on pageUrl to speed up fetching items for a specific page
    indices = [Index(value = ["page_url"])]
)
data class PokemonEntity(
    @PrimaryKey val id: Int, // Use the Pokemon's ID from the URL as the primary key
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "page_url") val pageUrl: String // The API URL (page) this item belongs to
)

// Helper function to convert API model to DB entity
fun PokemonListItem.toEntity(pageUrl: String): PokemonEntity? {
    val pokemonId = this.id ?: return null // Need an ID for the primary key
    return PokemonEntity(
        id = pokemonId,
        name = this.name,
        url = this.url,
        pageUrl = pageUrl
    )
}

// Helper function to convert DB entity back to API model
fun PokemonEntity.toListItem(): PokemonListItem {
    return PokemonListItem(
        name = this.name,
        url = this.url
    )
}