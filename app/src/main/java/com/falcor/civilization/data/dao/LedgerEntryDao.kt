package com.falcor.civilization.data.dao

import androidx.room.*
import com.falcor.civilization.data.entities.LedgerEntryEntity

@Dao
interface LedgerEntryDao {
    @Query("SELECT * FROM ledger_entries ORDER BY createdAt ASC")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries ORDER BY createdAt ASC")
    suspend fun getAllSync(): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger_entries WHERE createdAt >= :fromTs AND createdAt <= :toTs ORDER BY createdAt ASC")
    suspend fun getSlice(fromTs: Long, toTs: Long): List<LedgerEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: LedgerEntryEntity)

    @Query("SELECT * FROM ledger_entries ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLast(): LedgerEntryEntity?
}
