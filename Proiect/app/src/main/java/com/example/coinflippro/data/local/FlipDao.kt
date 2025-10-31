package com.example.coinflippro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FlipDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(flip: FlipEntity): Long

    @Query("DELETE FROM flips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM flips")
    suspend fun clearAll()

    @Query("SELECT * FROM flips ORDER BY flippedAtMillis DESC")
    fun observeAll(): Flow<List<FlipEntity>>

    @Query("UPDATE flips SET isHeads = :isHeads WHERE id = :id")
    suspend fun updateResult(id: Long, isHeads: Boolean)
}
