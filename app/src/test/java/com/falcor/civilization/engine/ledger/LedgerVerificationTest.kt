package com.falcor.civilization.engine.ledger

import com.falcor.civilization.data.dao.LedgerEntryDao
import com.falcor.civilization.data.entities.LedgerEntryEntity
import com.falcor.civilization.domain.LedgerPayload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LedgerVerificationTest {

    @Test
    fun emptyLedgerVerifies() = runTest {
        val dao = InMemoryLedgerDao()
        val file = File.createTempFile("ledger", ".jsonl")
        try {
            val service = LedgerService(dao, file)
            val result = service.verifyChain()
            assertTrue(result.valid)
            assertEquals(0, result.totalEntries)
        } finally {
            file.delete()
        }
    }

    @Test
    fun validChainVerifies() = runTest {
        val dao = InMemoryLedgerDao()
        val file = File.createTempFile("ledger", ".jsonl")
        try {
            val service = LedgerService(dao, file)
            service.append(LedgerPayload("TEST", "e1", "data1", 1000L))
            service.append(LedgerPayload("TEST", "e2", "data2", 2000L))
            val result = service.verifyChain()
            assertTrue(result.valid)
            assertEquals(2, result.totalEntries)
        } finally {
            file.delete()
        }
    }

    private class InMemoryLedgerDao : LedgerEntryDao {
        private val entries = mutableListOf<LedgerEntryEntity>()

        override suspend fun getAllSync(): List<LedgerEntryEntity> = entries.toList()
        override suspend fun getLast(): LedgerEntryEntity? = entries.lastOrNull()
        override suspend fun getSlice(fromTs: Long, toTs: Long): List<LedgerEntryEntity> =
            entries.filter { it.createdAt in fromTs..toTs }
        override suspend fun insert(entry: LedgerEntryEntity) { entries.add(entry) }
        override fun getAll(): kotlinx.coroutines.flow.Flow<List<LedgerEntryEntity>> =
            kotlinx.coroutines.flow.flowOf(entries.toList())
    }
}
