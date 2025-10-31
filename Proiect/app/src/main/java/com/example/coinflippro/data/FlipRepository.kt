package com.example.coinflippro.data

import com.example.coinflippro.data.local.FlipDao
import com.example.coinflippro.data.local.FlipEntity
import kotlinx.coroutines.flow.Flow

class FlipRepository(private val dao: FlipDao) {

    suspend fun addFlip(isHeads: Boolean, flippedAtMillis: Long = System.currentTimeMillis()) {
        dao.insert(
            FlipEntity(
                isHeads = isHeads,
                flippedAtMillis = flippedAtMillis
            )
        )
    }

    suspend fun deleteFlip(id: Long) = dao.deleteById(id)
    suspend fun clearAll() = dao.clearAll()
    fun observeAll(): Flow<List<FlipEntity>> = dao.observeAll()

    suspend fun updateFlip(id: Long, isHeads: Boolean) = dao.updateResult(id, isHeads)
}
