/*
 * This file is part of VSCodeX.
 *
 * VSCodeX is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * VSCodeX is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with VSCodeX.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.vscodex.net.ui.screens.editor.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.FolderCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import com.google.ai.client.generativeai.type.asTextOrNull
import dev.jeziellago.compose.markdowntext.MarkdownText
import io.vscodex.net.core.ai.AiChatHistory
import io.vscodex.net.core.ai.ChatMessage
import io.vscodex.net.core.ai.Gemini
import io.vscodex.net.core.ai.OpenRouter
import io.vscodex.net.file.File as VsxFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Constants ────────────────────────────────────────────────────────────────

private val CODE_EXTENSIONS = setOf(
    "kt", "java", "py", "js", "ts", "jsx", "tsx", "json", "xml",
    "yaml", "yml", "toml", "gradle", "kts", "md", "txt", "sh",
    "html", "css", "sql", "rb", "cpp", "c", "h", "rs", "go",
    "swift", "dart", "properties", "pro", "gitignore", "env"
)

private val SKIP_DIRS = setOf(
    ".git", ".gradle", "build", ".idea", "node_modules",
    "__pycache__", ".DS_Store", "intermediates", "generated"
)

private const val MAX_FILE_CHARS  = 3_000   // per file
private const val MAX_TOTAL_CHARS = 80_000  // whole codebase budget

private val CODEBASE_QUICK_PROMPTS = listOf(
    "Overview"    to "Give a high-level overview of this codebase. What does it do, how is it structured, and what are the main modules or layers?",
    "Architecture" to "Describe the architecture of this project: design patterns used, how modules communicate, data flow, and any notable architectural decisions.",
    "Entry points" to "What are the main entry points of this application? Describe the startup flow and how different parts are initialized.",
    "Find bugs"   to "Review the codebase for bugs, logic errors, potential crashes, null pointer risks, or security issues. List each with file and explanation.",
    "Improve"     to "Suggest the most impactful improvements for code quality, performance, maintainability, and best practices across the codebase.",
    "Dependencies" to "List and explain the key external dependencies. Are there any that seem outdated, unnecessary, or could be replaced?"
)

// ─── Codebase snapshot ────────────────────────────────────────────────────────

data class CodebaseSnapshot(
    val rootPath: String,
    val rootName: String,
    val fileCount: Int,
    val totalChars: Int,
    val context: String,            // full text sent to AI
    val fileTree: String,           // pretty-printed tree for display
    val skippedFiles: Int = 0
)

// ─── AI message model (re-uses same data class shape) ─────────────────────────

private data class VsAiMessage(
    val content: String,
    val isUser: Boolean,
    val isError: Boolean    = false,
    val isStreaming: Boolean = false,
    val sourcePrompt: String? = null,
    val sourceFull: String?   = null
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun VsCodeXAiScreen(
    modifier: Modifier = Modifier,
    initialFolder: VsxFile? = null,
    onBackToFiles: () -> Unit = {}
) {
    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val clipboard    = LocalClipboardManager.current
    val listState    = rememberLazyListState()

    val messages     = remember { mutableStateListOf<VsAiMessage>() }
    var inputText    by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var activeJob    by remember { mutableStateOf<Job?>(null) }

    var snapshot     by remember { mutableStateOf<CodebaseSnapshot?>(null) }
    var isIndexing   by remember { mutableStateOf(false) }
    var indexError   by remember { mutableStateOf<String?>(null) }

    // Auto-index the folder opened in the Files panel (no manual selection needed)
    LaunchedEffect(initialFolder) {
        if (initialFolder != null && snapshot == null && !isIndexing) {
            isIndexing = true
            indexError = null
            try {
                val snap = withContext(Dispatchers.IO) {
                    buildSnapshotFromVsxFile(context, initialFolder)
                }
                snapshot = snap
            } catch (e: Exception) {
                indexError = "Failed to index folder: ${e.message}"
            } finally {
                isIndexing = false
            }
        }
    }

    var thinkingEnabled  by remember { mutableStateOf(false) }
    var thinkingMenuOpen by remember { mutableStateOf(false) }
    var thinkingLevel    by remember { mutableStateOf("medium") }
    var moreMenuOpen     by remember { mutableStateOf(false) }
    var selectedTab      by remember { mutableStateOf<String?>(null) }

    // ── Folder picker ─────────────────────────────────────────────────────────
    var launcherReady by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isIndexing = true
        indexError = null
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        scope.launch {
            try {
                val snap = withContext(Dispatchers.IO) {
                    val docFile = DocumentFile.fromTreeUri(context, uri)
                        ?: throw IllegalStateException("Cannot open folder: $uri")
                    buildSnapshotFromDocumentFile(context, docFile)
                }
                snapshot = snap
                messages.clear()
            } catch (e: Exception) {
                indexError = "Failed to index folder: ${e.message}"
            } finally {
                isIndexing = false
            }
        }
    }

    // ── Single-file picker ("Select File from folder") ────────────────────────
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val fileName = DocumentFile.fromSingleUri(context, uri)?.name ?: "file"
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                }
                // Inject file content as a user context message
                val fileContext = "Here is the content of `$fileName` from the project:\n```\n${
                    if (text.length > 8000) text.take(8000) + "\n// …[truncated]" else text
                }\n```"
                messages.add(VsAiMessage(content = fileContext, isUser = true))
            } catch (e: Exception) {
                indexError = "Failed to read file: ${e.message}"
            }
        }
    }

    SideEffect { launcherReady = true }

    // ── Auto-scroll ───────────────────────────────────────────────────────────
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf true
            info.visibleItemsInfo.last().index >= info.totalItemsCount - 2
        }
    }
    var autoScrollEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(messages.size) {
        autoScrollEnabled = true
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LaunchedEffect(listState) {
        snapshotFlow { isAtBottom }.distinctUntilChanged().collect { atBottom ->
            if (!atBottom) autoScrollEnabled = false
        }
    }
    LaunchedEffect(messages.lastOrNull()?.content) {
        if (autoScrollEnabled && messages.isNotEmpty())
            listState.scrollToItem(messages.size - 1)
    }

    // ── Thinking suffix ───────────────────────────────────────────────────────
    fun thinkingSuffix(): String = if (!thinkingEnabled) "" else when (thinkingLevel) {
        "low"  -> "\n\nBriefly think before answering."
        "high" -> "\n\nThink step-by-step and show your reasoning before giving the answer."
        else   -> "\n\nThink carefully before answering."
    }

    // ── Prompt builder ────────────────────────────────────────────────────────
    fun buildFullPrompt(userPrompt: String): String {
        val snap = snapshot ?: return userPrompt + thinkingSuffix()
        return """
You are an expert software engineer. You have been given the full source code of the project "${snap.rootName}".

--- CODEBASE CONTEXT (${snap.fileCount} files, ${snap.totalChars} chars) ---
${snap.context}
--- END OF CODEBASE ---

User request: $userPrompt${thinkingSuffix()}
        """.trimIndent()
    }

    // ── History helper ────────────────────────────────────────────────────────
    fun buildHistory(upTo: Int): List<ChatMessage> =
        messages.take(upTo)
            .filter { !it.isStreaming && it.content.isNotBlank() && !it.isError }
            .map { ChatMessage(if (it.isUser) "user" else "assistant", it.content) }

    // ── Core send ─────────────────────────────────────────────────────────────
    fun sendPromptInternal(
        displayText: String,
        fullPrompt: String,
        replaceIndex: Int? = null
    ) {
        if (isLoading) return
        if (replaceIndex == null) {
            messages.add(VsAiMessage(displayText, isUser = true))
        }
        isLoading = true

        val streamIdx = replaceIndex ?: messages.size
        val placeholder = VsAiMessage("", isUser = false, isStreaming = true,
            sourcePrompt = displayText, sourceFull = fullPrompt)
        if (replaceIndex != null && replaceIndex < messages.size) {
            messages[replaceIndex] = placeholder
        } else {
            messages.add(placeholder)
        }

        val history = buildHistory(streamIdx) + ChatMessage("user", fullPrompt)

        val job = scope.launch {
            try {
                if (OpenRouter.isConfigured()) {
                    val accumulated = StringBuilder()
                    OpenRouter.chatStream(history = history, onToken = { token ->
                        accumulated.append(token)
                        if (streamIdx < messages.size) {
                            messages[streamIdx] = messages[streamIdx].copy(
                                content = accumulated.toString(), isStreaming = true)
                        }
                    }).onSuccess {
                        if (streamIdx < messages.size) {
                            messages[streamIdx] = messages[streamIdx].copy(
                                content = accumulated.toString(), isStreaming = false)
                        }
                    }.onFailure { e ->
                        if (streamIdx < messages.size) {
                            messages[streamIdx] = VsAiMessage(
                                "⚠ ${e.message ?: "Error"}", isUser = false, isError = true)
                        }
                    }
                } else {
                    Gemini.chat(fullPrompt)
                        .onSuccess { response ->
                            val text = response.candidates.firstOrNull()
                                ?.content?.parts?.firstOrNull()?.asTextOrNull()
                                ?: "No response."
                            if (streamIdx < messages.size) {
                                messages[streamIdx] = VsAiMessage(
                                    text, isUser = false, isStreaming = false,
                                    sourcePrompt = displayText, sourceFull = fullPrompt)
                            }
                        }
                        .onFailure { e ->
                            if (streamIdx < messages.size) {
                                messages[streamIdx] = VsAiMessage(
                                    "⚠ ${e.message ?: "Error"}", isUser = false, isError = true)
                            }
                        }
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                if (streamIdx < messages.size) {
                    val partial = messages[streamIdx].content
                    messages[streamIdx] = messages[streamIdx].copy(
                        content = partial.ifBlank { "_(stopped)_" }, isStreaming = false)
                }
            } catch (e: Exception) {
                if (streamIdx < messages.size) {
                    messages[streamIdx] = VsAiMessage(
                        "⚠ ${e.message ?: "Unknown error"}", isUser = false, isError = true)
                }
            } finally {
                isLoading = false
                activeJob = null
            }
        }
        activeJob = job
    }

    fun sendMessage() {
        val prompt = inputText.trim()
        if (prompt.isEmpty() || isLoading) return
        inputText = ""
        sendPromptInternal(prompt, buildFullPrompt(prompt))
    }

    fun sendQuickPrompt(label: String, prompt: String) {
        if (isLoading) return
        sendPromptInternal(label, buildFullPrompt(prompt))
    }

    fun regenerate(idx: Int) {
        val msg = messages.getOrNull(idx) ?: return
        val full = msg.sourceFull ?: return
        sendPromptInternal(msg.sourcePrompt ?: "Regenerate", full, replaceIndex = idx)
    }

    val isThinking = isLoading &&
            messages.lastOrNull()?.let { it.isStreaming && it.content.isEmpty() } == true
    val canSend = inputText.isNotBlank() && !isLoading

    // ─── UI ───────────────────────────────────────────────────────────────────

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sparkle avatar
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null,
                        tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                // Title + subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text("VSCodeX AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Text(
                        snapshot?.rootName
                            ?: if (OpenRouter.isConfigured()) "OpenRouter · no codebase"
                               else "Gemini · no codebase",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                // "N files" pill (visible when indexed)
                snapshot?.let { snap ->
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.FolderCopy, null,
                                    modifier = Modifier.size(11.dp))
                                Text("${snap.fileCount} files",
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier.padding(end = 2.dp),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
                // Thinking dropdown
                Box {
                    IconButton(onClick = { thinkingMenuOpen = true },
                        modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Psychology, "Thinking",
                            modifier = Modifier.size(20.dp),
                            tint = if (thinkingEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = thinkingMenuOpen,
                        onDismissRequest = { thinkingMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Off", style = MaterialTheme.typography.bodySmall) },
                            onClick = { thinkingEnabled = false; thinkingMenuOpen = false })
                        DropdownMenuItem(
                            text = { Text("Low", style = MaterialTheme.typography.bodySmall) },
                            onClick = { thinkingEnabled = true; thinkingLevel = "low"; thinkingMenuOpen = false })
                        DropdownMenuItem(
                            text = { Text("Medium", style = MaterialTheme.typography.bodySmall) },
                            onClick = { thinkingEnabled = true; thinkingLevel = "medium"; thinkingMenuOpen = false })
                        DropdownMenuItem(
                            text = { Text("High", style = MaterialTheme.typography.bodySmall) },
                            onClick = { thinkingEnabled = true; thinkingLevel = "high"; thinkingMenuOpen = false })
                    }
                }
                // Clear
                AnimatedVisibility(visible = messages.isNotEmpty()) {
                    IconButton(onClick = {
                        activeJob?.cancel(); messages.clear(); selectedTab = null
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Clear, "Clear",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // ⋯ More options
                Box {
                    IconButton(onClick = { moreMenuOpen = true },
                        modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.MoreVert, "More",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = moreMenuOpen,
                        onDismissRequest = { moreMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select File from folder",
                                style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                Icon(Icons.Outlined.InsertDriveFile, null,
                                    modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                moreMenuOpen = false
                                if (launcherReady) filePicker.launch(arrayOf("*/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Select Folder",
                                style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = {
                                Icon(Icons.Outlined.FolderOpen, null,
                                    modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                moreMenuOpen = false
                                if (launcherReady) folderPicker.launch(null)
                            }
                        )
                    }
                }
            }

            // ── Indexing progress ─────────────────────────────────────────────
            AnimatedVisibility(visible = isIndexing) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    LinearProgressIndicator(
                        modifier   = Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)),
                        color      = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(3.dp))
                    Text("Indexing codebase…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Index error banner ────────────────────────────────────────────
            AnimatedVisibility(visible = indexError != null) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(indexError ?: "", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                    IconButton(onClick = { indexError = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // ── Streaming progress ────────────────────────────────────────────
            AnimatedVisibility(visible = isLoading && !isThinking) {
                LinearProgressIndicator(
                    modifier   = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(2.dp).clip(RoundedCornerShape(1.dp)),
                    color      = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // ── Message list ──────────────────────────────────────────────────
            LazyColumn(
                state   = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(6.dp)) }

                if (messages.isEmpty() && !isLoading) {
                    item {
                        VsAiEmptyState(
                            hasSnapshot   = snapshot != null,
                            isIndexing    = isIndexing,
                            initialFolder = initialFolder,
                            onPickFolder  = { if (launcherReady) folderPicker.launch(null) }
                        )
                    }
                }

                items(messages, key = { System.identityHashCode(it).toLong() }) { msg ->
                    if (msg.isStreaming && msg.content.isEmpty()) return@items
                    val idx = messages.indexOf(msg)
                    VsAiBubble(
                        message      = msg,
                        onCopy       = { clipboard.setText(AnnotatedString(msg.content)) },
                        onCopyCode   = { clipboard.setText(AnnotatedString(it)) },
                        onRegenerate = if (!msg.isUser && !msg.isStreaming && msg.sourceFull != null)
                            {{ regenerate(idx) }} else null
                    )
                }

                item { Spacer(Modifier.height(4.dp)) }
            }

            // ── Tab bar: Overview | Architecture | Entry points ───────────────
            AnimatedVisibility(
                visible = snapshot != null && !isLoading,
                enter   = fadeIn() + slideInVertically { it / 2 },
                exit    = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CODEBASE_QUICK_PROMPTS.forEach { (label, prompt) ->
                        val isSelected = selectedTab == label
                        FilterChip(
                            selected = isSelected,
                            onClick  = {
                                selectedTab = if (isSelected) null else label
                                if (!isSelected) sendQuickPrompt(label, prompt)
                            },
                            label    = {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            },
                            colors   = FilterChipDefaults.filterChipColors(
                                containerColor         = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor             = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor     = MaterialTheme.colorScheme.surface
                            ),
                            border   = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = isSelected,
                                borderColor         = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // ── Input bar ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text(
                            if (snapshot != null) "Ask about the codebase…"
                            else "Pick a folder, then ask anything…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    },
                    shape     = RoundedCornerShape(24.dp),
                    maxLines  = 5,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors    = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(
                        if (isLoading)  MaterialTheme.colorScheme.errorContainer
                        else if (canSend) MaterialTheme.colorScheme.primary
                        else            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        IconButton(onClick = { activeJob?.cancel() }, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.Stop, "Stop",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    } else {
                        IconButton(
                            onClick  = { sendMessage() },
                            enabled  = canSend,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Rounded.Send, "Send",
                                modifier = Modifier.size(20.dp),
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                       else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Thinking overlay
        AnimatedVisibility(
            visible  = isThinking,
            enter    = fadeIn(tween(200)),
            exit     = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        ) { VsAiThinkingPill() }

        // Scroll FAB
        AnimatedVisibility(
            visible  = !isAtBottom && isLoading,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 100.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    autoScrollEnabled = true
                    scope.launch { listState.animateScrollToItem(messages.size - 1) }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation      = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Rounded.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun VsAiEmptyState(
    hasSnapshot  : Boolean,
    isIndexing   : Boolean,
    initialFolder: VsxFile? = null,
    onPickFolder : () -> Unit
) {
    Column(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 24.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer
                ))
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.AutoAwesome, null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary)
        }

        Text("VSCodeX AI",
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface)

        Text(
            if (hasSnapshot) "Codebase indexed! Ask anything about the project."
            else "Select your project folder and I'll read the entire codebase — then answer questions, find bugs, explain architecture, and more.",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier  = Modifier.padding(horizontal = 8.dp)
        )

                if (!hasSnapshot && !isIndexing && initialFolder == null) {
                    Button(
                        onClick = onPickFolder,
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Select Project Folder", fontWeight = FontWeight.SemiBold)
                    }
                }
    }
}

// ─── Bubble ───────────────────────────────────────────────────────────────────

@Composable
private fun VsAiBubble(
    message     : VsAiMessage,
    onCopy      : () -> Unit,
    onCopyCode  : (String) -> Unit,
    onRegenerate: (() -> Unit)?
) {
    val isUser = message.isUser
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(
                    Brush.linearGradient(listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    ))
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AutoAwesome, null,
                    tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier            = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                Card(
                    shape  = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    SelectionContainer {
                        Text(message.content,
                            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = MaterialTheme.colorScheme.onPrimary,
                            lineHeight = 20.sp)
                    }
                }
            } else {
                if (message.isStreaming) {
                    Card(
                        shape  = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column {
                            SelectionContainer {
                                Text(message.content,
                                    modifier   = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
                                    style      = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp)
                            }
                            VsStreamingCursor()
                        }
                    }
                } else {
                    val segments = remember(message.content) { parseSegments(message.content) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        segments.forEach { seg ->
                            when (seg) {
                                is MessageSegment.Prose -> {
                                    if (seg.text.isNotBlank()) {
                                        Card(
                                            shape  = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (message.isError)
                                                    MaterialTheme.colorScheme.errorContainer
                                                else MaterialTheme.colorScheme.surfaceContainer),
                                            elevation = CardDefaults.cardElevation(0.dp)
                                        ) {
                                            MarkdownText(
                                                markdown         = seg.text,
                                                isTextSelectable = true,
                                                style            = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize = 14.sp, lineHeight = 20.sp),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                                is MessageSegment.Code -> {
                                    VsCodeBlock(seg.code, seg.lang) { onCopyCode(seg.code) }
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment     = Alignment.CenterVertically) {
                            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.ContentCopy, "Copy",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                            if (onRegenerate != null) {
                                IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Refresh, "Retry",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                        // ── Fix it / View diff action buttons ─────────────────
                        val hasCode = remember(message.content) {
                            message.content.contains("```")
                        }
                        if (hasCode) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Fix it
                                Surface(
                                    onClick = { /* TODO: apply fix */ },
                                    shape   = RoundedCornerShape(20.dp),
                                    color   = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Outlined.AutoAwesome, null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSurface)
                                        Text("Fix it",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                // View diff
                                Surface(
                                    onClick = { /* TODO: show diff */ },
                                    shape   = RoundedCornerShape(20.dp),
                                    color   = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Rounded.Code, null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSurface)
                                        Text("View diff",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Code block ───────────────────────────────────────────────────────────────

@Composable
private fun VsCodeBlock(code: String, lang: String, onCopy: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(lang.lowercase().ifBlank { "code" },
                style      = MaterialTheme.typography.labelSmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace)
            TextButton(
                onClick        = onCopy,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier       = Modifier.height(26.dp)
            ) {
                Text("Copy", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
        SelectionContainer {
            Text(code,
                modifier   = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                fontFamily = FontFamily.Monospace,
                fontSize   = 12.sp,
                lineHeight = 18.sp,
                color      = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ─── Thinking pill ────────────────────────────────────────────────────────────

@Composable
private fun VsAiThinkingPill() {
    val infiniteTransition = rememberInfiniteTransition(label = "vs_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue  = -200f, targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmer")
    val pillAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.88f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha")
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f * pillAlpha))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.matchParentSize().background(
            Brush.linearGradient(
                colors = listOf(Color.Transparent,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), Color.Transparent),
                start = Offset(shimmerOffset, 0f),
                end   = Offset(shimmerOffset + 200f, 60f))))
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.AutoAwesome, null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = pillAlpha),
                modifier = Modifier.size(14.dp))
            Text("Thinking…", style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = pillAlpha))
            VsThinkingDots()
        }
    }
}

@Composable
private fun VsThinkingDots() {
    val t = rememberInfiniteTransition(label = "vsdots")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { i ->
            val scale by t.animateFloat(
                initialValue  = 0.4f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = i * 140, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "dot$i")
            Box(modifier = Modifier.size((5 * scale).dp.coerceAtLeast(2.dp)).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f + 0.4f * scale)))
        }
    }
}

// ─── Streaming cursor ─────────────────────────────────────────────────────────

@Composable
private fun VsStreamingCursor() {
    val t = rememberInfiniteTransition(label = "vscursor")
    val alpha by t.animateFloat(
        initialValue  = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha")
    Box(modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        .size(width = 8.dp, height = 14.dp)
        .background(color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            shape = RoundedCornerShape(2.dp)))
}

// ─── DocumentFile-based codebase indexer (handles content:// URIs) ───────────

private fun buildSnapshotFromDocumentFile(
    context: Context,
    root: DocumentFile
): CodebaseSnapshot {
    val sb         = StringBuilder()
    val treeLines  = StringBuilder()
    var fileCount  = 0
    var skipped    = 0
    var totalChars = 0

    fun walkDir(dir: DocumentFile, prefix: String) {
        if (!dir.exists() || !dir.canRead()) return
        if (dir.name in SKIP_DIRS) return

        val children = dir.listFiles()
            .sortedWith(compareBy({ !it.isDirectory }, { it.name?.lowercase() ?: "" }))

        children.forEachIndexed { i, child ->
            val last      = i == children.lastIndex
            val connector = if (last) "└── " else "├── "
            val childPfx  = prefix + (if (last) "    " else "│   ")
            treeLines.appendLine("$prefix$connector${child.name ?: "(unknown)"}")

            if (child.isDirectory) {
                if (child.name !in SKIP_DIRS) walkDir(child, childPfx)
            } else if (child.isFile) {
                val ext = child.name?.substringAfterLast('.', "")?.lowercase() ?: ""
                if (ext in CODE_EXTENSIONS && child.canRead()) {
                    if (totalChars < MAX_TOTAL_CHARS) {
                        try {
                            val text = context.contentResolver
                                .openInputStream(child.uri)
                                ?.bufferedReader(Charsets.UTF_8)
                                ?.use { it.readText() } ?: ""
                            val clamped = if (text.length > MAX_FILE_CHARS)
                                text.take(MAX_FILE_CHARS) + "\n// … [truncated]"
                            else text
                            sb.appendLine("\n// FILE: ${child.name}")
                            sb.appendLine(clamped)
                            totalChars += clamped.length
                            fileCount++
                        } catch (_: Exception) { skipped++ }
                    } else {
                        skipped++
                    }
                }
            }
        }
    }

    treeLines.appendLine(root.name ?: "(root)")
    walkDir(root, "")

    return CodebaseSnapshot(
        rootPath     = root.uri.toString(),
        rootName     = root.name ?: "(root)",
        fileCount    = fileCount,
        totalChars   = totalChars,
        context      = sb.toString(),
        fileTree     = treeLines.toString(),
        skippedFiles = skipped
    )
}

// ─── Custom-File-interface-based indexer (used when folder comes from Files panel) ──

private suspend fun buildSnapshotFromVsxFile(
    context: Context,
    root: VsxFile
): CodebaseSnapshot {
    val sb         = StringBuilder()
    val treeLines  = StringBuilder()
    var fileCount  = 0
    var skipped    = 0
    var totalChars = 0

    suspend fun walkDir(dir: VsxFile, prefix: String) {
        if (!dir.exists()) return
        if (dir.name in SKIP_DIRS) return

        val children = dir.listFiles()
            ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: return

        children.forEachIndexed { i, child ->
            val last      = i == children.lastIndex
            val connector = if (last) "└── " else "├── "
            val childPfx  = prefix + (if (last) "    " else "│   ")
            treeLines.appendLine("$prefix$connector${child.name}")

            if (child.isDirectory) {
                if (child.name !in SKIP_DIRS) walkDir(child, childPfx)
            } else {
                val ext = child.name.substringAfterLast('.', "").lowercase()
                if (ext in CODE_EXTENSIONS) {
                    if (totalChars < MAX_TOTAL_CHARS) {
                        try {
                            val text = child.readFile2String(context) ?: ""
                            val clamped = if (text.length > MAX_FILE_CHARS)
                                text.take(MAX_FILE_CHARS) + "\n// … [truncated]"
                            else text
                            sb.appendLine("\n// FILE: ${child.name}")
                            sb.appendLine(clamped)
                            totalChars += clamped.length
                            fileCount++
                        } catch (_: Exception) { skipped++ }
                    } else {
                        skipped++
                    }
                }
            }
        }
    }

    treeLines.appendLine(root.name)
    walkDir(root, "")

    return CodebaseSnapshot(
        rootPath     = root.absolutePath,
        rootName     = root.name,
        fileCount    = fileCount,
        totalChars   = totalChars,
        context      = sb.toString(),
        fileTree     = treeLines.toString(),
        skippedFiles = skipped
    )
}
