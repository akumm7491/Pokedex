package com.akumm7491.pokedex.data.sources.local.db

import androidx.room.*
import com.akumm7491.pokedex.domain.models.PokemonListResponse
import com.akumm7491.pokedex.data.sources.local.entities.PageMetadataEntity
import com.akumm7491.pokedex.data.sources.local.entities.PokemonEntity
import com.akumm7491.pokedex.data.sources.local.entities.toListItem

@Dao
interface PokemonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokemonList(pokemon: List<PokemonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageMetadata(metadata: PageMetadataEntity)

    @Query("SELECT * FROM pokemon WHERE page_url = :pageUrl")
    suspend fun getPokemonForPage(pageUrl: String): List<PokemonEntity>

    @Query("SELECT * FROM page_metadata WHERE page_url = :pageUrl LIMIT 1")
    suspend fun getPageMetadata(pageUrl: String): PageMetadataEntity?

    @Query("DELETE FROM pokemon WHERE page_url = :pageUrl")
    suspend fun clearPokemonForPage(pageUrl: String)

    @Query("DELETE FROM page_metadata WHERE page_url = :pageUrl")
    suspend fun clearPageMetadata(pageUrl: String)

    @Query("DELETE FROM pokemon")
    suspend fun clearAllPokemon()

    @Query("DELETE FROM page_metadata")
    suspend fun clearAllMetadata()

    @Transaction
    suspend fun insertPageData(pageUrl: String, nextUrl: String?, pokemonItems: List<PokemonEntity>) {
        insertPokemonList(pokemonItems)
        insertPageMetadata(PageMetadataEntity(pageUrl, nextUrl, System.currentTimeMillis()))
    }

    @Transaction
    @Query("SELECT * FROM page_metadata WHERE page_url = :pageUrl LIMIT 1")
    suspend fun getCachedPageResponse(pageUrl: String): CachedPageData?

}

data class CachedPageData(
    @Embedded val metadata: PageMetadataEntity,
    @Relation(
        parentColumn = "page_url",
        entityColumn = "page_url"
    )
    val pokemon: List<PokemonEntity>
) {
    fun toPokemonListResponse(): PokemonListResponse {
        return PokemonListResponse(
            next = metadata.nextUrl,
            results = pokemon.map { it.toListItem() }
        )
    }
}