package com.example.coinflippro.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.coinflippro.data.FlipRepository
import com.example.coinflippro.data.local.AppDatabase

class FlipViewModelFactory(private val appContext: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(appContext)
        val repo = FlipRepository(db.flipDao())
        return FlipViewModel(repo) as T
    }
}
