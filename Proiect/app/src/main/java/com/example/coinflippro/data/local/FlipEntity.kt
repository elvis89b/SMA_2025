package com.example.coinflippro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flips")
data class FlipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val flippedAtMillis: Long = System.currentTimeMillis(),
    val isHeads: Boolean
)
