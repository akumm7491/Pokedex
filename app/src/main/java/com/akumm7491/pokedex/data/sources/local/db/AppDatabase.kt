package com.akumm7491.pokedex.data.sources.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.akumm7491.pokedex.data.sources.local.entities.PageMetadataEntity
import com.akumm7491.pokedex.data.sources.local.entities.PokemonEntity

@Database(
    entities = [PokemonEntity::class, PageMetadataEntity::class],
    version = 1, // Increment version on schema changes
    exportSchema = false // Optional: Disable exporting schema file
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pokemonDao(): PokemonDao
}