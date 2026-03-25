package com.aura.shell.knowledge

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.shell.ui.theme.AuraShellTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Share target: user explicitly sends text or a link into Aura. Local save only.
 */
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.action != Intent.ACTION_SEND) {
            finish()
            return
        }
        val shared = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (shared.isEmpty()) {
            finish()
            return
        }

        val repo = KnowledgeRepository(applicationContext)
        val titleSuggestion = shared.lines().firstOrNull()?.trim()?.take(72)
            ?: KnowledgeTextExtractor.snippet(shared, 72)

        setContent {
            AuraShellTheme {
                val scope = rememberCoroutineScope()
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "Save to Aura Knowledge?",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.padding(8.dp))
                        Text(
                            text = "Stored only on this device. You can review or delete anytime in Knowledge.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.padding(12.dp))
                        Text(
                            text = titleSuggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                        )
                        Spacer(Modifier.padding(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        if (looksLikeUrl(shared)) {
                                            repo.insertSharedUrl(shared)
                                        } else {
                                            repo.insertSharedText(shared, suggestedTitle = titleSuggestion)
                                        }
                                    }
                                    startActivity(
                                        Intent(this@ShareReceiverActivity, KnowledgeActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        },
                                    )
                                    finish()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Save")
                        }
                        TextButton(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    private fun looksLikeUrl(s: String): Boolean {
        return s.startsWith("http://", ignoreCase = true) ||
            s.startsWith("https://", ignoreCase = true)
    }
}
