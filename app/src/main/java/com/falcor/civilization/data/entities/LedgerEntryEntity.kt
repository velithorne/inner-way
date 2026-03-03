package com.falcor.civilization.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey val id: String,
    val prevHash: String,
    val payloadJson: String,
    val payloadHash: String,
    val entryHash: String,
    val createdAt: Long
)
