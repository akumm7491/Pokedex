package com.akumm7491.pokedex.data.sources.remote

import com.akumm7491.pokedex.domain.models.PokemonDetailResponse
import com.akumm7491.pokedex.domain.models.PokemonListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface PokemonApiService {
    @GET
    suspend fun getPokemonList(@Url url: String): PokemonListResponse

    @GET("pokemon/{idOrName}")
    suspend fun getPokemonDetail(@Path("idOrName") idOrName: String): PokemonDetailResponse
}