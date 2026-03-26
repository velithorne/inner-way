package com.aura.shell.knowledge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aura.shell.ui.theme.AuraShellTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KnowledgeActivity : ComponentActivity() {

    private val viewModel: KnowledgeViewModel by viewModels {
        KnowledgeViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startRoute = intent.getStringExtra(EXTRA_ROUTE)

        setContent {
            AuraShellTheme {
                val listState by viewModel.listState.collectAsState()
                val detailUi by viewModel.detailUi.collectAsState()
                val navController = rememberNavController()
                val clipboard = LocalClipboardManager.current
                val scope = rememberCoroutineScope()
                var showLinkPicker by remember { mutableStateOf(false) }

                val importLauncher = rememberLauncherForActivityResult(
                    contract = object : ActivityResultContract<Array<String>, Uri?>() {
                        override fun createIntent(context: Context, input: Array<String>): Intent {
                            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_MIME_TYPES, input)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            }
                        }
                        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                            if (resultCode != RESULT_OK || intent == null) return null
                            return intent.data
                        }
                    },
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        } catch (_: SecurityException) {
                        }
                        val mime = contentResolver.getType(uri)
                        val name = uri.lastPathSegment
                        viewModel.importUri(uri, name, mime)
                    }
                }

                LaunchedEffect(startRoute) {
                    when {
                        startRoute == null || startRoute == "list" -> {
                            viewModel.setTagFilter(null)
                            navController.navigate(KnowledgeRoutes.List) {
                                popUpTo(KnowledgeRoutes.List) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        startRoute == "imports" -> {
                            viewModel.setImportsOnly(true)
                            viewModel.setTagFilter(null)
                            navController.navigate(KnowledgeRoutes.List) {
                                popUpTo(KnowledgeRoutes.List) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        startRoute.startsWith("tag:") -> {
                            viewModel.setTagFilter(startRoute.removePrefix("tag:"))
                            viewModel.setImportsOnly(false)
                            navController.navigate(KnowledgeRoutes.List) {
                                popUpTo(KnowledgeRoutes.List) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        startRoute == "cluster" -> {
                            navController.navigate(KnowledgeRoutes.Cluster) { launchSingleTop = true }
                        }
                        startRoute == "import" -> {
                            navController.navigate(KnowledgeRoutes.List) {
                                popUpTo(KnowledgeRoutes.List) { inclusive = true }
                                launchSingleTop = true
                            }
                            delay(80)
                            importLauncher.launch(arrayOf("*/*"))
                        }
                        startRoute == "new" -> {
                            navController.navigate(KnowledgeRoutes.EditorNew) { launchSingleTop = true }
                        }
                        startRoute == "clipboard" -> {
                            val t = clipboard.getText()?.text?.trim().orEmpty()
                            if (t.isNotEmpty()) viewModel.saveClipboard(t)
                            navController.navigate(KnowledgeRoutes.List) {
                                popUpTo(KnowledgeRoutes.List) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        startRoute.startsWith("item:") -> {
                            val id = startRoute.removePrefix("item:")
                            navController.navigate(KnowledgeRoutes.detail(id)) { launchSingleTop = true }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = KnowledgeRoutes.List,
                ) {
                    composable(KnowledgeRoutes.List) {
                        KnowledgeListScreen(
                            state = listState,
                            onBack = { finish() },
                            onItemClick = { id ->
                                navController.navigate(KnowledgeRoutes.detail(id))
                            },
                            onImportsToggle = { viewModel.setImportsOnly(it) },
                            onNewNote = { navController.navigate(KnowledgeRoutes.EditorNew) },
                            onImportFile = {
                                importLauncher.launch(arrayOf("*/*"))
                            },
                            onClipboard = {
                                val t = clipboard.getText()?.text?.trim().orEmpty()
                                if (t.isNotEmpty()) viewModel.saveClipboard(t)
                            },
                            onTagFilter = { key -> viewModel.setTagFilter(key) },
                            onOpenCluster = {
                                navController.navigate(KnowledgeRoutes.Cluster)
                            },
                        )
                    }
                    composable(KnowledgeRoutes.Cluster) {
                        val clusterItems = remember { mutableStateOf<List<KnowledgeListItem>>(emptyList()) }
                        LaunchedEffect(Unit) {
                            clusterItems.value = viewModel.continueCluster()
                        }
                        KnowledgeClusterScreen(
                            items = clusterItems.value,
                            onBack = { navController.popBackStack() },
                            onItemClick = { id ->
                                navController.navigate(KnowledgeRoutes.detail(id))
                            },
                        )
                    }
                    composable(KnowledgeRoutes.EditorNew) {
                        NoteEditorScreen(
                            existingId = null,
                            initialTitle = "",
                            initialBody = "",
                            isAuraNote = true,
                            onSave = { title, body ->
                                viewModel.saveNote(title, body)
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = KnowledgeRoutes.Detail,
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: return@composable
                        LaunchedEffect(id) {
                            viewModel.onEnterDetail(id)
                            viewModel.loadDetail(id)
                        }
                        DisposableEffect(id) {
                            onDispose {
                                viewModel.onLeaveDetail(id)
                            }
                        }
                        val d = detailUi?.takeIf { it.entity.id == id }
                        if (d != null) {
                            KnowledgeDetailScreen(
                                detail = d,
                                onBack = { navController.popBackStack() },
                                onDelete = {
                                    viewModel.deleteItem(id)
                                    navController.popBackStack()
                                },
                                onEdit = { navController.navigate(KnowledgeRoutes.editor(id)) },
                                onCopy = { text ->
                                    clipboard.setText(AnnotatedString(text))
                                },
                                onOpenUri = { u ->
                                    try {
                                        startActivity(Intent(Intent.ACTION_VIEW, u))
                                    } catch (_: Exception) { }
                                },
                                onAddTag = { t -> viewModel.addTag(id, t) },
                                onRemoveTag = { t -> viewModel.removeTag(id, t) },
                                onApplySuggestion = { t -> viewModel.applySuggestion(id, t) },
                                onRelatedClick = { rid ->
                                    navController.navigate(KnowledgeRoutes.detail(rid)) {
                                        launchSingleTop = true
                                    }
                                },
                                onUnlinkManual = { rid -> viewModel.unlinkManual(id, rid) },
                                onOpenLinkPicker = { showLinkPicker = true },
                            )
                        }
                        if (showLinkPicker) {
                            LinkItemPickerDialog(
                                excludeItemId = id,
                                loadItems = { q -> viewModel.itemsToLink(id, q) },
                                onDismiss = { showLinkPicker = false },
                                onPick = { toId ->
                                    viewModel.linkToItem(id, toId)
                                    showLinkPicker = false
                                },
                            )
                        }
                    }
                    composable(
                        route = KnowledgeRoutes.Editor,
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: return@composable
                        val entity = detailUi?.entity?.takeIf { it.id == id }
                        LaunchedEffect(id) {
                            viewModel.loadDetail(id)
                        }
                        if (entity != null && entity.isAuraAuthored) {
                            NoteEditorScreen(
                                existingId = entity.id,
                                initialTitle = entity.title,
                                initialBody = entity.fullText,
                                isAuraNote = true,
                                onSave = { t, b ->
                                    viewModel.updateNote(id, t, b)
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ROUTE = "com.aura.shell.extra.KNOWLEDGE_ROUTE"
    }
}
