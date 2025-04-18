package com.akumm7491.pokedex.data.sources.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "page_metadata")
data class PageMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "page_url") val pageUrl: String,
    @ColumnInfo(name = "next_url") val nextUrl: String?,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)