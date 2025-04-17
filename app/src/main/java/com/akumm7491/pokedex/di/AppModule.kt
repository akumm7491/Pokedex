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
        isLenient = true // If API might return unexpected fields sometimes
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // Optional: Add logging interceptor for debugging network calls
            // .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/") // Base URL needed by Retrofit
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun providePokemonApiService(retrofit: Retrofit): PokemonApiService {
        // Provides the Retrofit service implementation
        return retrofit.create(PokemonApiService::class.java)
    }

    // --- Database Setup ---

    @Provides
    @Singleton
    fun provideAppDatabase(app: Application): AppDatabase {
        // Provides the Room database instance
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "pokemon_database"
        )
            // .fallbackToDestructiveMigration() // Add proper migrations in production!
            .build()
    }

    @Provides
    @Singleton
    fun providePokemonDao(database: AppDatabase): PokemonDao {
        // Provides the Room Data Access Object (DAO)
        return database.pokemonDao()
    }

    // --- DataSource Setup ---

    @Provides
    @Singleton
    fun providePokemonRemoteDataSource(apiService: PokemonApiService): PokemonRemoteDataSource {
        // Provides the implementation bound to the interface
        return PokemonRemoteDataSourceImpl(apiService)
        // Note: Alternatively, if PokemonRemoteDataSourceImpl has @Inject constructor,
        // Hilt could create it automatically. Using @Provides here is explicit.
        // Or use @Binds in an abstract module for interface binding.
    }

    @Provides
    @Singleton
    fun providePokemonLocalDataSource(pokemonDao: PokemonDao): PokemonLocalDataSource {
        // Provides the implementation bound to the interface
        return PokemonLocalDataSourceImpl(pokemonDao)
    }

    // --- Repository Setup ---

    @Provides
    @Singleton
    fun providePokemonRepository(
        remoteDataSource: PokemonRemoteDataSource, // Depends on the provided DataSources
        localDataSource: PokemonLocalDataSource
    ): PokemonRepository { // Provides the interface type
        // Provides the implementation bound to the interface
        return PokemonRepositoryImpl(remoteDataSource, localDataSource)
    }
}