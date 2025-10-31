package com.example.coinflippro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coinflippro.data.FlipRepository
import com.example.coinflippro.data.local.FlipEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FlipViewModel(private val repo: FlipRepository) : ViewModel() {

    val flips: StateFlow<List<FlipEntity>> =
        repo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun recordFlip(isHeads: Boolean) {
        viewModelScope.launch { repo.addFlip(isHeads) }
    }

    fun deleteFlip(id: Long) {
        viewModelScope.launch { repo.deleteFlip(id) }
    }

    fun updateFlip(id: Long, isHeads: Boolean) {
        viewModelScope.launch { repo.updateFlip(id, isHeads) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }
}
