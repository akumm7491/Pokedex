package com.akumm7491.pokedex.di

import android.app.Application
import androidx.room.Room
import com.akumm7491.pokedex.data.PokemonRepositoryImpl
import com.akumm7491.pokedex.data.sources.local.PokemonLocalDataSource
import com.akumm7491.pokedex.data.sources.local.PokemonLocalDataSourceImpl
import com.akumm7491.pokedex.data.sources.local.db.AppDatabase
import com.akumm7491.pokedex.data.sources.local.db.PokemonDao
import com.akumm7491.pokedex.data.sources.remote.PokemonApiService
import com.akumm7491.pokedex.data.sources.remote.PokemonRemoteDataSource
import com.akumm7491.pokedex.data.sources.remote.PokemonRemoteDataSourceImpl
import com.akumm7491.pokedex.domain.repositories.PokemonRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Network Setup ---

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun providePokemonApiService(retrofit: Retrofit): PokemonApiService {
        return retrofit.create(PokemonApiService::class.java)
    }

    // --- Database Setup ---

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "pokemon_database"
        ).build()
    }

    @Provides
    @Singleton
    fun providePokemonDao(database: AppDatabase): PokemonDao {
        return database.pokemonDao()
    }

    // --- DataSource Setup ---

    @Provides
    @Singleton
    fun providePokemonRemoteDataSource(apiService: PokemonApiService): PokemonRemoteDataSource {
        return PokemonRemoteDataSourceImpl(apiService)
    }

    @Provides
    @Singleton
    fun providePokemonLocalDataSource(pokemonDao: PokemonDao): PokemonLocalDataSource {
        return PokemonLocalDataSourceImpl(pokemonDao)
    }

    // --- Repository Setup ---

    @Provides
    @Singleton
    fun providePokemonRepository(
        remoteDataSource: PokemonRemoteDataSource,
        localDataSource: PokemonLocalDataSource
    ): PokemonRepository {
        return PokemonRepositoryImpl(remoteDataSource, localDataSource)
    }
}