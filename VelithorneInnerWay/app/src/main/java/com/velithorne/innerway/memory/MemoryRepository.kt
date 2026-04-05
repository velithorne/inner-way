package com.velithorne.innerway.memory

import kotlinx.coroutines.flow.Flow

class MemoryRepository(
    private val dao: MemoryDao,
) {

    suspend fun insert(memory: MemoryEntity): Long = dao.insert(memory)

    suspend fun countMemories(): Int = dao.count()

    fun observeRecent(limit: Int = 32): Flow<List<MemoryEntity>> = dao.observeRecent(limit)

    suspend fun recentSnapshot(limit: Int = 16): List<MemoryEntity> = dao.getRecent(limit)
}
