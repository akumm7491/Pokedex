package com.akumm7491.pokedex.data

import android.util.Log
import com.akumm7491.pokedex.data.sources.local.PokemonLocalDataSource
import com.akumm7491.pokedex.data.sources.remote.PokemonRemoteDataSource
import com.akumm7491.pokedex.domain.models.PokemonDetailResponse
import com.akumm7491.pokedex.domain.models.PokemonListResponse
import com.akumm7491.pokedex.domain.repositories.PokemonRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

@Singleton
class PokemonRepositoryImpl @Inject constructor(
    private val remoteDataSource: PokemonRemoteDataSource,
    private val localDataSource: PokemonLocalDataSource
) : PokemonRepository {

    private val cacheValidity = 1.hours.inWholeMilliseconds

    override suspend fun fetchPokemon(url: String): Result<PokemonListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val cachedData = localDataSource.getCachedPage(url)
                val now = System.currentTimeMillis()

                if (cachedData != null && (now - cachedData.metadata.timestamp) < cacheValidity) {
                    Log.w("PokemonRepo", "Cache hit for $url")
                    Result.success(cachedData.toPokemonListResponse())
                } else {
                    Log.w("PokemonRepo", "No cache for $url so fetching")
                    val networkResponse = remoteDataSource.getPokemonList(url)

                    Log.w("PokemonRepo", "Caching network response for $url")
                    localDataSource.savePageData(url, networkResponse)
                    Result.success(networkResponse)
                }
            } catch (e: Exception) {
                val staleCache = localDataSource.getCachedPage(url)
                if (staleCache != null) {
                    Log.w("PokemonRepo", "Network failed for $url, returning stale cache.", e)
                    Result.success(staleCache.toPokemonListResponse())
                } else {
                    Log.e("PokemonRepo", "Network failed for $url and no cache available.", e)
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun fetchPokemonDetail(idOrName: String): Result<PokemonDetailResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val detailResponse = remoteDataSource.getPokemonDetail(idOrName)
                Result.success(detailResponse)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}