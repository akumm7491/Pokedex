package com.akumm7491.pokedex.data.sources.local.db

import androidx.room.*
import com.akumm7491.pokedex.domain.models.PokemonListResponse
import com.akumm7491.pokedex.data.sources.local.entities.PageMetadataEntity
import com.akumm7491.pokedex.data.sources.local.entities.PokemonEntity
import com.akumm7491.pokedex.data.sources.local.entities.toListItem

@Dao
interface PokemonDao {

    // Insert multiple Pokemon items, replace if conflict (e.g., fetching same page again)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPokemonList(pokemon: List<PokemonEntity>)

    // Insert page metadata, replace if conflict
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageMetadata(metadata: PageMetadataEntity)

    // Get all Pokemon items associated with a specific page URL
    @Query("SELECT * FROM pokemon WHERE page_url = :pageUrl")
    suspend fun getPokemonForPage(pageUrl: String): List<PokemonEntity>

    // Get the metadata for a specific page URL
    @Query("SELECT * FROM page_metadata WHERE page_url = :pageUrl LIMIT 1")
    suspend fun getPageMetadata(pageUrl: String): PageMetadataEntity?

    // Clear Pokemon items for a specific page (optional, if needed)
    @Query("DELETE FROM pokemon WHERE page_url = :pageUrl")
    suspend fun clearPokemonForPage(pageUrl: String)

    // Clear metadata for a specific page (optional)
    @Query("DELETE FROM page_metadata WHERE page_url = :pageUrl")
    suspend fun clearPageMetadata(pageUrl: String)

    // Clear all Pokemon data (e.g., for cache invalidation)
    @Query("DELETE FROM pokemon")
    suspend fun clearAllPokemon()

    // Clear all metadata (e.g., for cache invalidation)
    @Query("DELETE FROM page_metadata")
    suspend fun clearAllMetadata()

    // Transaction to insert page data consistently
    @Transaction
    suspend fun insertPageData(pageUrl: String, nextUrl: String?, pokemonItems: List<PokemonEntity>) {
        // Clear previous items for this page before inserting new ones (optional, depends on strategy)
        // clearPokemonForPage(pageUrl)
        insertPokemonList(pokemonItems)
        insertPageMetadata(PageMetadataEntity(pageUrl, nextUrl, System.currentTimeMillis()))
    }

    // Transaction to retrieve a full page response from cache
    @Transaction
    @Query("SELECT * FROM page_metadata WHERE page_url = :pageUrl LIMIT 1")
    suspend fun getCachedPageResponse(pageUrl: String): CachedPageData?

}

// Helper data class to combine results from the transaction query
data class CachedPageData(
    @Embedded val metadata: PageMetadataEntity,
    @Relation(
        parentColumn = "page_url",
        entityColumn = "page_url"
    )
    val pokemon: List<PokemonEntity>
) {
    // Convert cached data back to the API response format
    fun toPokemonListResponse(): PokemonListResponse {
        return PokemonListResponse(
            // We don't store count/previous in this simplified cache, return defaults
            count = -1, // Or fetch count separately if needed
            previous = null, // Or store if needed
            next = metadata.nextUrl,
            results = pokemon.map { it.toListItem() }
        )
    }
}