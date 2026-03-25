package com.aura.shell.knowledge

object KnowledgeRoutes {
    const val List = "knowledge_list"
    const val EditorNew = "knowledge_editor_new"
    const val Detail = "knowledge_detail/{id}"
    const val Editor = "knowledge_editor/{id}"
    fun detail(id: String) = "knowledge_detail/$id"
    fun editor(id: String) = "knowledge_editor/$id"
}
