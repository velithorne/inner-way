package com.falcor.civilization.engine.ledger

import com.falcor.civilization.data.dao.LedgerEntryDao
import com.falcor.civilization.data.entities.LedgerEntryEntity
import com.falcor.civilization.domain.LedgerPayload
import com.falcor.civilization.engine.util.HashUtil
import com.falcor.civilization.util.IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class LedgerService(
    private val ledgerDao: LedgerEntryDao,
    private val ledgerFile: File
) {
    private val json = Json { prettyPrint = false }

    suspend fun append(payload: LedgerPayload): LedgerEntryEntity = withContext(Dispatchers.IO) {
        val payloadJson = json.encodeToString(payload)
        val payloadHash = HashUtil.sha256(payloadJson)
        val last = ledgerDao.getLast()
        val prevHash = last?.entryHash ?: "0"
        val createdAt = System.currentTimeMillis()
        val entryHash = HashUtil.sha256(prevHash, payloadHash, createdAt.toString())
        val id = IdGenerator.generate()
        val entry = LedgerEntryEntity(
            id = id,
            prevHash = prevHash,
            payloadJson = payloadJson,
            payloadHash = payloadHash,
            entryHash = entryHash,
            createdAt = createdAt
        )
        ledgerDao.insert(entry)
        appendToFile(entry)
        entry
    }

    private fun appendToFile(entry: LedgerEntryEntity) {
        ledgerFile.parentFile?.mkdirs()
        val line = json.encodeToString(LedgerFileEntry(
            id = entry.id,
            prevHash = entry.prevHash,
            payloadJson = entry.payloadJson,
            payloadHash = entry.payloadHash,
            entryHash = entry.entryHash,
            createdAt = entry.createdAt
        )) + "\n"
        ledgerFile.appendText(line)
    }

    suspend fun verifyChain(): LedgerVerificationResult = withContext(Dispatchers.IO) {
        val entries = ledgerDao.getAllSync()
        if (entries.isEmpty()) {
            return@withContext LedgerVerificationResult(valid = true, totalEntries = 0, firstFailure = null)
        }
        var prevHash = "0"
        for ((i, entry) in entries.withIndex()) {
            val expectedEntryHash = HashUtil.sha256(prevHash, entry.payloadHash, entry.createdAt.toString())
            if (entry.entryHash != expectedEntryHash) {
                return@withContext LedgerVerificationResult(
                    valid = false,
                    totalEntries = entries.size,
                    firstFailure = i
                )
            }
            val expectedPayloadHash = HashUtil.sha256(entry.payloadJson)
            if (entry.payloadHash != expectedPayloadHash) {
                return@withContext LedgerVerificationResult(
                    valid = false,
                    totalEntries = entries.size,
                    firstFailure = i
                )
            }
            prevHash = entry.entryHash
        }
        LedgerVerificationResult(valid = true, totalEntries = entries.size, firstFailure = null)
    }

    suspend fun getSliceForRun(runStartedAt: Long, runEndedAt: Long): List<LedgerEntryEntity> {
        return ledgerDao.getSlice(runStartedAt - 1000, runEndedAt + 1000)
    }
}

@kotlinx.serialization.Serializable
private data class LedgerFileEntry(
    val id: String,
    val prevHash: String,
    val payloadJson: String,
    val payloadHash: String,
    val entryHash: String,
    val createdAt: Long
)

data class LedgerVerificationResult(
    val valid: Boolean,
    val totalEntries: Int,
    val firstFailure: Int?
)
