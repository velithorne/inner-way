package com.collide.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.data.repository.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(private val repo: EventRepository) : ViewModel() {

    val eventCount = repo.getEventCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    class Factory(private val repo: EventRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repo) as T
    }
}
