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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aura.shell.ui.theme.AuraShellTheme
import kotlinx.coroutines.delay

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
                val detail by viewModel.detailState.collectAsState()
                val navController = rememberNavController()
                val clipboard = LocalClipboardManager.current

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
                            // Still readable in-session for some providers
                        }
                        val mime = contentResolver.getType(uri)
                        val name = uri.lastPathSegment
                        viewModel.importUri(uri, name, mime)
                    }
                }

                LaunchedEffect(startRoute) {
                    when {
                        startRoute == null || startRoute == "list" -> {
                            navController.navigate(KnowledgeRoutes.List) {
                                popUpTo(KnowledgeRoutes.List) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                        startRoute == "imports" -> {
                            viewModel.setImportsOnly(true)
                            navController.navigate(KnowledgeRoutes.List) {
                                popUpTo(KnowledgeRoutes.List) { inclusive = true }
                                launchSingleTop = true
                            }
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
                            viewModel.loadDetail(id)
                        }
                        val entity = detail.takeIf { it?.id == id }
                        if (entity != null) {
                            KnowledgeDetailScreen(
                                entity = entity,
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
                            )
                        }
                    }
                    composable(
                        route = KnowledgeRoutes.Editor,
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id") ?: return@composable
                        LaunchedEffect(id) {
                            viewModel.loadDetail(id)
                        }
                        val entity = detail.takeIf { it?.id == id }
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
