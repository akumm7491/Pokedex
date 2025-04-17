package com.akumm7491.pokedex.data.sources.remote

import com.akumm7491.pokedex.domain.models.PokemonDetailResponse
import com.akumm7491.pokedex.domain.models.PokemonListResponse
import javax.inject.Inject

interface PokemonRemoteDataSource {
    suspend fun getPokemonList(url: String): PokemonListResponse
    suspend fun getPokemonDetail(idOrName: String): PokemonDetailResponse
}

// Implementation using Retrofit ApiService
class PokemonRemoteDataSourceImpl @Inject constructor(
    private val apiService: PokemonApiService
) : PokemonRemoteDataSource {

    override suspend fun getPokemonList(url: String): PokemonListResponse {
         try {
             return apiService.getPokemonList(url)
         } catch (e: Exception) {
             throw Exception("Failed fetching from $url", e)
         }
    }

    override suspend fun getPokemonDetail(idOrName: String): PokemonDetailResponse {
        return apiService.getPokemonDetail(idOrName)
    }
}