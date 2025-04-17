package com.akumm7491.pokedex.data.sources.local

import com.akumm7491.pokedex.domain.models.PokemonListResponse
import com.akumm7491.pokedex.data.sources.local.db.CachedPageData
import com.akumm7491.pokedex.data.sources.local.db.PokemonDao
import com.akumm7491.pokedex.data.sources.local.entities.toEntity
import javax.inject.Inject

interface PokemonLocalDataSource {
    suspend fun getCachedPage(pageUrl: String): CachedPageData?
    suspend fun savePageData(pageUrl: String, response: PokemonListResponse)
    suspend fun clearCache()
}

class PokemonLocalDataSourceImpl @Inject constructor(
    private val pokemonDao: PokemonDao
) : PokemonLocalDataSource {

    override suspend fun getCachedPage(pageUrl: String): CachedPageData? {
        return pokemonDao.getCachedPageResponse(pageUrl)
    }

    override suspend fun savePageData(pageUrl: String, response: PokemonListResponse) {
        val pokemonEntities = response.results
            .mapNotNull { it.toEntity(pageUrl = pageUrl) }

        if (pokemonEntities.isNotEmpty()) {
            pokemonDao.insertPageData(
                pageUrl = pageUrl,
                nextUrl = response.next,
                pokemonItems = pokemonEntities
            )
        }
    }

    override suspend fun clearCache() {
        pokemonDao.clearAllPokemon()
        pokemonDao.clearAllMetadata()
    }
}