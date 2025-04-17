package com.akumm7491.pokedex.data

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
    // Inject DataSources instead of ApiService/Dao directly
    private val remoteDataSource: PokemonRemoteDataSource,
    private val localDataSource: PokemonLocalDataSource
) : PokemonRepository { // Implement the interface

    private val cacheValidity = 1.hours.inWholeMilliseconds

    override suspend fun fetchPokemon(url: String): Result<PokemonListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Check Cache validity using LocalDataSource
                val cachedData = localDataSource.getCachedPage(url)
                val now = System.currentTimeMillis()

                if (cachedData != null && (now - cachedData.metadata.timestamp) < cacheValidity) {
                    // Cache hit and is valid
                    println("Cache hit for $url")
                    Result.success(cachedData.toPokemonListResponse())
                } else {
                    println("No cache for $url so fetching")
                    // 2. Cache miss or expired, fetch from Network using RemoteDataSource
                    val networkResponse = remoteDataSource.getPokemonList(url)

                    // 3. Store successful response in DB using LocalDataSource
                    localDataSource.savePageData(url, networkResponse)

                    Result.success(networkResponse)
                }
            } catch (e: Exception) { // Catch exceptions from Remote or Local DataSources
                // Log e
                // Attempt to return stale cache on network failure
                val staleCache = localDataSource.getCachedPage(url)
                if (staleCache != null) {
                    // Log.w("PokemonRepo", "Network failed for $url, returning stale cache.", e)
                    Result.success(staleCache.toPokemonListResponse())
                } else {
                    // No cache and network failed
                    // Log.e("PokemonRepo", "Network failed for $url and no cache available.", e)
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun fetchPokemonDetail(idOrName: String): Result<PokemonDetailResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch directly from remote source for now
                val detailResponse = remoteDataSource.getPokemonDetail(idOrName)
                Result.success(detailResponse)
            } catch (e: Exception) {
                // Log e
                Result.failure(e)
            }
        }
    }
}