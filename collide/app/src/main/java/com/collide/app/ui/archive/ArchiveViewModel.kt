package com.collide.app.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.data.repository.EventRepository
import com.collide.app.domain.model.SavedEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchiveViewModel(private val repo: EventRepository) : ViewModel() {

    val events = repo.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteEvent(id: String) {
        viewModelScope.launch { repo.deleteEvent(id) }
    }

    class Factory(private val repo: EventRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            ArchiveViewModel(repo) as T
    }
}
