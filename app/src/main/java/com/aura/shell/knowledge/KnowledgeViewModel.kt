package com.aura.shell.knowledge

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.shell.AuraApplication
import com.aura.shell.knowledge.db.KnowledgeItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KnowledgeListUiState(
    val items: List<KnowledgeListItem> = emptyList(),
    val filterImportsOnly: Boolean = false,
)

class KnowledgeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repo: KnowledgeRepository = (application as AuraApplication).knowledgeRepository

    private val importsOnly = MutableStateFlow(false)

    val listState: StateFlow<KnowledgeListUiState> = combine(
        repo.observeRecent(500),
        importsOnly,
    ) { recent, imp ->
        KnowledgeListUiState(
            items = if (imp) recent.filter { it.sourceType == KnowledgeSourceType.IMPORTED_FILE } else recent,
            filterImportsOnly = imp,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KnowledgeListUiState(),
    )

    private val _detailState = MutableStateFlow<KnowledgeItemEntity?>(null)
    val detailState: StateFlow<KnowledgeItemEntity?> = _detailState

    fun setImportsOnly(value: Boolean) {
        importsOnly.value = value
    }

    fun loadDetail(id: String) {
        viewModelScope.launch {
            _detailState.value = repo.getById(id)
        }
    }

    fun clearDetail() {
        _detailState.value = null
    }

    fun saveNote(title: String, body: String) {
        viewModelScope.launch {
            repo.insertNote(title, body)
        }
    }

    fun updateNote(id: String, title: String, body: String) {
        viewModelScope.launch {
            repo.updateAuraNote(id, title, body)
            _detailState.value = repo.getById(id)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repo.delete(id)
            _detailState.update { cur -> if (cur?.id == id) null else cur }
        }
    }

    fun importUri(uri: Uri, name: String?, mime: String?) {
        viewModelScope.launch {
            repo.importFromUri(uri, name, mime)
        }
    }

    fun saveClipboard(text: String) {
        viewModelScope.launch {
            repo.insertClipboardText(text)
        }
    }
}
