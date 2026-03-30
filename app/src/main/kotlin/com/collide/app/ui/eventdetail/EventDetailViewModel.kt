package com.collide.app.ui.eventdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.collide.app.data.repository.EventRepository
import com.collide.app.domain.model.SavedEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EventDetailViewModel(
    private val repository: EventRepository,
    private val eventId: Long
) : ViewModel() {

    private val _event = MutableStateFlow<SavedEvent?>(null)
    val event: StateFlow<SavedEvent?> = _event

    init {
        viewModelScope.launch {
            _event.value = repository.getEventById(eventId)
        }
    }

    class Factory(
        private val repository: EventRepository,
        private val eventId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EventDetailViewModel(repository, eventId) as T
        }
    }
}
