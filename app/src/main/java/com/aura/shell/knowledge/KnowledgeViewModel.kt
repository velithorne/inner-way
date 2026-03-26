package com.aura.shell.knowledge

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.shell.AuraApplication
import com.aura.shell.knowledge.db.KnowledgeItemEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class KnowledgeListUiState(
    val items: List<KnowledgeListItem> = emptyList(),
    val filterImportsOnly: Boolean = false,
    val filterTagKey: String? = null,
    val popularTags: List<String> = emptyList(),
)

class KnowledgeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repo: KnowledgeRepository = (application as AuraApplication).knowledgeRepository

    private val importsOnly = MutableStateFlow(false)
    private val filterTag = MutableStateFlow<String?>(null)
    private val popularTags = MutableStateFlow<List<String>>(emptyList())

    init {
        viewModelScope.launch {
            while (isActive) {
                popularTags.value = repo.getPopularTagKeys(14)
                delay(3500)
            }
        }
    }

    val listState: StateFlow<KnowledgeListUiState> = combine(
        repo.observeRecent(500),
        importsOnly,
        filterTag,
        popularTags,
    ) { recent, imp, tagKey, popular ->
        val byImport = if (imp) recent.filter { it.sourceType == KnowledgeSourceType.IMPORTED_FILE } else recent
        val filtered = if (tagKey == null) byImport else {
            byImport.filter { item -> item.tagKeys.any { it.equals(tagKey, ignoreCase = true) } }
        }
        KnowledgeListUiState(
            items = filtered,
            filterImportsOnly = imp,
            filterTagKey = tagKey,
            popularTags = popular,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KnowledgeListUiState(),
    )

    private val _detailUi = MutableStateFlow<KnowledgeDetailUi?>(null)
    val detailUi: StateFlow<KnowledgeDetailUi?> = _detailUi

    private val _detailState = MutableStateFlow<KnowledgeItemEntity?>(null)
    val detailState: StateFlow<KnowledgeItemEntity?> = _detailState

    fun setImportsOnly(value: Boolean) {
        importsOnly.value = value
    }

    fun setTagFilter(tagKey: String?) {
        filterTag.value = tagKey?.let { TagNormalizer.normalize(it) }.takeIf { (it?.length ?: 0) >= 2 }
    }

    fun onEnterDetail(itemId: String) {
        KnowledgeContextStore.recordOpenInSession(itemId)
    }

    fun onLeaveDetail(itemId: String) {
        if (KnowledgeContextStore.currentKnowledgeItemId == itemId) {
            KnowledgeContextStore.setCurrentKnowledgeItem(null)
        }
    }

    fun loadDetail(id: String) {
        viewModelScope.launch {
            KnowledgeContextStore.setCurrentKnowledgeItem(id)
            val entity = repo.getById(id) ?: run {
                _detailUi.value = null
                _detailState.value = null
                return@launch
            }
            val tags = repo.getTagKeysForItem(id)
            val sug = repo.tagSuggestionsFor(id)
            val rel = repo.relatedFor(id, 14)
            _detailUi.value = KnowledgeDetailUi(
                entity = entity,
                tags = tags,
                tagSuggestions = sug.filter { !tags.contains(it) },
                related = rel,
            )
            _detailState.value = entity
        }
    }

    fun clearDetail() {
        _detailUi.value = null
        _detailState.value = null
    }

    fun refreshDetail(id: String) = loadDetail(id)

    fun addTag(itemId: String, raw: String) {
        viewModelScope.launch {
            repo.addTagToItem(itemId, raw)
            loadDetail(itemId)
        }
    }

    fun removeTag(itemId: String, raw: String) {
        viewModelScope.launch {
            repo.removeTagFromItem(itemId, raw)
            loadDetail(itemId)
        }
    }

    fun applySuggestion(itemId: String, tag: String) {
        addTag(itemId, tag)
    }

    fun linkToItem(fromId: String, toId: String) {
        viewModelScope.launch {
            repo.linkItemsManually(fromId, toId)
            loadDetail(fromId)
        }
    }

    fun unlinkManual(fromId: String, relatedId: String) {
        viewModelScope.launch {
            repo.unlinkItemsManually(fromId, relatedId)
            loadDetail(fromId)
        }
    }

    suspend fun itemsToLink(excludeId: String, query: String) = repo.itemsForPickLinkDialog(excludeId, query)

    fun saveNote(title: String, body: String) {
        viewModelScope.launch {
            repo.insertNote(title, body)
        }
    }

    fun updateNote(id: String, title: String, body: String) {
        viewModelScope.launch {
            repo.updateAuraNote(id, title, body)
            loadDetail(id)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repo.delete(id)
            onLeaveDetail(id)
            _detailUi.update { cur -> if (cur?.entity?.id == id) null else cur }
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

    suspend fun continueCluster() = repo.continueRecentCluster(8)
}
