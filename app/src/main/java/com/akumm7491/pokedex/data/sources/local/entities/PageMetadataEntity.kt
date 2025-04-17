package com.akumm7491.pokedex.data.sources.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "page_metadata")
data class PageMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "page_url") val pageUrl: String, // The URL of the fetched page (e.g., ...?limit=20&offset=0)
    @ColumnInfo(name = "next_url") val nextUrl: String?, // The 'next' URL provided by the API for this page
    @ColumnInfo(name = "timestamp") val timestamp: Long // When this page data was fetched/cached
)