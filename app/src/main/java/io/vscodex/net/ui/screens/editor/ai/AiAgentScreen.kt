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

import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ai.client.generativeai.type.asTextOrNull
import com.itsvks.monaco.MonacoEditor
import dev.jeziellago.compose.markdowntext.MarkdownText
import io.vscodex.net.core.ai.AiChatHistory
import io.vscodex.net.core.ai.ChatMessage
import io.vscodex.net.core.ai.Gemini
import io.vscodex.net.core.ai.OpenRouter
import io.vscodex.net.ui.screens.editor.EditorViewModel
import io.vscodex.net.ui.screens.editor.components.view.CodeEditorView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// ─── Data models ─────────────────────────────────────────────────────────────

/** A file the user has attached to their next message. */
data class AttachedFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    /** Plain text (code / text files) — appended to the AI prompt. */
    val textContent: String? = null,
    /** Base64 string for images — noted in prompt but not sent inline yet. */
    val base64Len: Int = 0
)

private fun formatBytes(bytes: Long): String = when {
    bytes < 1_024       -> "${bytes}B"
    bytes < 1_048_576   -> "${bytes / 1_024}KB"
    else                -> "${"%.1f".format(bytes / 1_048_576.0)}MB"
}

private val IMAGE_MIMES = setOf("image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp")

private val TEXT_EXTENSIONS = setOf(
    "kt", "java", "py", "js", "ts", "jsx", "tsx",
    "json", "xml", "yaml", "yml", "toml", "gradle",
    "md", "txt", "sh", "html", "css", "sql", "rb",
    "cpp", "c", "h", "rs", "go", "swift", "dart"
)

// ─── Message model ────────────────────────────────────────────────────────────

data class AiMessage(
    val content: String,
    val isUser: Boolean,
    val isError: Boolean    = false,
    val isStreaming: Boolean = false,
    val sourcePrompt: String? = null,
    val sourceFull: String?   = null,
    val attachedFiles: List<AttachedFile> = emptyList(),
    val usedThinking: Boolean = false
)

// ─── Markdown segment parser ──────────────────────────────────────────────────

sealed class MessageSegment {
    data class Prose(val text: String) : MessageSegment()
    data class Code(val code: String, val lang: String) : MessageSegment()
}

fun parseSegments(raw: String): List<MessageSegment> {
    val result = mutableListOf<MessageSegment>()
    val fence  = Regex("```(\\w*)\\n?([\\s\\S]*?)```", RegexOption.MULTILINE)
    var cursor = 0
    for (match in fence.findAll(raw)) {
        if (match.range.first > cursor) {
            val prose = raw.substring(cursor, match.range.first).trim()
            if (prose.isNotEmpty()) result += MessageSegment.Prose(prose)
        }
        result += MessageSegment.Code(
            code = match.groupValues[2].trimEnd(),
            lang = match.groupValues[1].ifBlank { "code" }
        )
        cursor = match.range.last + 1
    }
    if (cursor < raw.length) {
        val tail = raw.substring(cursor).trim()
        if (tail.isNotEmpty()) result += MessageSegment.Prose(tail)
    }
    return result.ifEmpty { listOf(MessageSegment.Prose(raw)) }
}

// ─── Quick prompts ────────────────────────────────────────────────────────────

private val QUICK_PROMPTS = listOf(
    "Analyze"   to "Analyze this file thoroughly. Describe what it does, its structure, key functions/classes, and any potential issues.",
    "Find bugs" to "Look for bugs, logic errors, null pointer risks, or incorrect assumptions in this code. List each issue with line context.",
    "Explain"   to "Explain this code in plain language. What is its purpose, how does it work, and what are the main components?",
    "Improve"   to "Suggest concrete improvements for readability, performance, best practices, and maintainability.",
    "Tests"     to "Write unit tests for the key logic in this file. Include edge cases.",
    "Refactor"  to "Refactor this code for clarity and modern best practices. Show before/after for each change.",
    "Docs"      to "Write comprehensive KDoc/Javadoc comments for all public symbols in this file."
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun AiAgentScreen(
    editorViewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val messages  = remember { mutableStateListOf<AiMessage>() }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope     = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val context   = LocalContext.current

    var activeJob by remember { mutableStateOf<Job?>(null) }

    val editorUiState by editorViewModel.uiState.collectAsStateWithLifecycle()
    var lastAnalyzedFilePath by remember { mutableStateOf<String?>(null) }

    // ── Thinking mode ─────────────────────────────────────────────────────────
    var thinkingEnabled by remember { mutableStateOf(false) }
    var thinkingMenuOpen by remember { mutableStateOf(false) }
    var thinkingLevel   by remember { mutableStateOf("medium") }

    // ── File attachments ──────────────────────────────────────────────────────
    val pendingFiles = remember { mutableStateListOf<AttachedFile>() }
    var attachError  by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        scope.launch {
            for (uri in uris) {
                runCatching {
                    val cr   = context.contentResolver
                    val mime = cr.getType(uri) ?: "application/octet-stream"

                    var name = "file"
                    var size = 0L
                    cr.query(uri, null, null, null, null)?.use { cur ->
                        val ni = cur.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val si = cur.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (cur.moveToFirst()) {
                            if (ni >= 0) name = cur.getString(ni)
                            if (si >= 0) size = cur.getLong(si)
                        }
                    }

                    val ext    = name.substringAfterLast('.', "").lowercase()
                    val isText = mime.startsWith("text/") || ext in TEXT_EXTENSIONS
                    val isImg  = mime in IMAGE_MIMES

                    var textContent: String? = null
                    var b64Len = 0
                    cr.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        when {
                            isText -> textContent = String(bytes, Charsets.UTF_8)
                                .let { if (it.length > 8_000) it.take(8_000) + "\n… [truncated]" else it }
                            isImg  -> b64Len = bytes.size
                        }
                    }

                    pendingFiles.add(
                        AttachedFile(
                            uri         = uri,
                            name        = name,
                            mimeType    = mime,
                            sizeBytes   = size,
                            textContent = textContent,
                            base64Len   = b64Len
                        )
                    )
                    attachError = null
                }.onFailure { e ->
                    attachError = "Could not read file: ${e.message ?: "unknown error"}"
                }
            }
        }
    }

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

    // ── Editor helpers ────────────────────────────────────────────────────────

    fun getFileContent(): Pair<String, String>? {
        val state      = editorUiState
        val openedFile = state.openedFiles.getOrNull(state.selectedFileIndex) ?: return null
        val file       = openedFile.file
        val editorContent: String? = when (val v = editorViewModel.getSelectedEditor()) {
            is CodeEditorView -> v.editor.text.toString().takeIf { it.isNotEmpty() }
            is MonacoEditor   -> v.text.takeIf { it.isNotEmpty() }
            else              -> editorViewModel.getEditorForFile(file)?.editor?.text?.toString()
        }
        if (!editorContent.isNullOrEmpty()) return file.name to editorContent
        val disk = file.asRawFile()?.takeIf { it.exists() }?.readText() ?: return null
        return file.name to disk
    }

    fun currentFilePath(): String? {
        val state = editorUiState
        return state.openedFiles.getOrNull(state.selectedFileIndex)?.file?.absolutePath
    }

    fun getSelectedText(): String? {
        val editor = editorViewModel.getSelectedEditor()
        if (editor is CodeEditorView) {
            val cursor = editor.editor.cursor
            if (cursor.isSelected) {
                return editor.editor.text
                    .subContent(cursor.leftLine, cursor.leftColumn, cursor.rightLine, cursor.rightColumn)
                    .toString()
                    .takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    fun applyCodeToEditor(newCode: String) {
        val editor = editorViewModel.getSelectedEditor()
        if (editor is CodeEditorView) {
            val text     = editor.editor.text
            val lastLine = (text.lineCount - 1).coerceAtLeast(0)
            val lastCol  = text.getColumnCount(lastLine)
            text.replace(0, 0, lastLine, lastCol, newCode)
        } else if (editor is MonacoEditor) {
            editor.evaluateJavascript(
                """(function(){
                    var model = editor.getModel();
                    var range = model.getFullModelRange();
                    editor.pushUndoStop();
                    model.pushEditOperations([],
                        [{range: range, text: ${org.json.JSONObject.quote(newCode)}}], null);
                    editor.pushUndoStop();
                })();""", null
            )
        }
    }

    // ── Prompt builders ───────────────────────────────────────────────────────

    fun thinkingInstruction(): String = if (!thinkingEnabled) "" else when (thinkingLevel) {
        "low"  -> "\n\nBriefly think before answering."
        "high" -> "\n\nThink step-by-step and show your reasoning before giving the final answer."
        else   -> "\n\nThink through the problem carefully before answering."
    }

    fun attachmentSection(files: List<AttachedFile>): String {
        if (files.isEmpty()) return ""
        return "\n\n--- Attached files ---\n" + files.joinToString("\n\n") { f ->
            when {
                f.textContent != null ->
                    "File \"${f.name}\":\n```\n${f.textContent}\n```"
                f.base64Len > 0 ->
                    "[Image attached: ${f.name} (${formatBytes(f.sizeBytes)})]"
                else ->
                    "[File attached: ${f.name} (${f.mimeType}, ${formatBytes(f.sizeBytes)})]"
            }
        }
    }

    fun buildPrompt(
        userPrompt: String,
        fileName: String,
        fileContent: String,
        selectedText: String? = null,
        attachments: List<AttachedFile> = emptyList()
    ): String {
        val truncated = if (fileContent.length > 12_000)
            fileContent.take(12_000) + "\n... [file truncated]" else fileContent
        val selSection = if (!selectedText.isNullOrBlank())
            "\n\nCurrently selected snippet:\n```\n$selectedText\n```" else ""
        return """
            File: $fileName
            ```
            $truncated
            ```$selSection${attachmentSection(attachments)}

            User request: $userPrompt${thinkingInstruction()}
        """.trimIndent()
    }

    fun buildPromptNoFile(
        userPrompt: String,
        attachments: List<AttachedFile> = emptyList()
    ): String = userPrompt + attachmentSection(attachments) + thinkingInstruction()

    fun buildHistory(upToIndex: Int = messages.size): List<ChatMessage> =
        messages.take(upToIndex)
            .filter { !it.isStreaming && it.content.isNotBlank() && !it.isError }
            .map { ChatMessage(role = if (it.isUser) "user" else "assistant", content = it.content) }

    fun persistHistory(filePath: String) {
        val toSave = messages
            .filter { !it.isStreaming && it.content.isNotBlank() }
            .map { AiChatHistory.PersistedMessage(it.content, it.isUser, it.isError) }
        AiChatHistory.save(context, filePath, toSave)
    }

    // ── Core send / stream ────────────────────────────────────────────────────

    fun sendPromptInternal(
        userDisplayText: String,
        fullPrompt: String,
        attachments: List<AttachedFile> = emptyList(),
        replaceIndex: Int? = null
    ) {
        if (isLoading) return

        val usedThinking = thinkingEnabled
        if (replaceIndex == null) {
            messages.add(
                AiMessage(
                    content       = userDisplayText,
                    isUser        = true,
                    attachedFiles = attachments,
                    usedThinking  = usedThinking
                )
            )
        }
        isLoading = true

        val streamingIndex = replaceIndex ?: messages.size
        val placeholder = AiMessage(
            content      = "",
            isUser       = false,
            isStreaming  = true,
            sourcePrompt = userDisplayText,
            sourceFull   = fullPrompt
        )
        if (replaceIndex != null && replaceIndex < messages.size) {
            messages[replaceIndex] = placeholder
        } else {
            messages.add(placeholder)
        }

        val history = buildHistory(upToIndex = streamingIndex) + ChatMessage("user", fullPrompt)

        val job = scope.launch {
            try {
                if (OpenRouter.isConfigured()) {
                    val accumulated = StringBuilder()
                    val streamResult: Result<String> = OpenRouter.chatStream(
                        history = history,
                        onToken = { token: String ->
                            accumulated.append(token)
                            if (streamingIndex < messages.size) {
                                messages[streamingIndex] = messages[streamingIndex].copy(
                                    content    = accumulated.toString(),
                                    isStreaming = true
                                )
                            }
                        }
                    )
                    streamResult.onSuccess { _: String ->
                        if (streamingIndex < messages.size) {
                            messages[streamingIndex] = messages[streamingIndex].copy(
                                content    = accumulated.toString(),
                                isStreaming = false
                            )
                            currentFilePath()?.let { persistHistory(it) }
                        }
                    }.onFailure { error: Throwable ->
                        if (streamingIndex < messages.size) {
                            messages[streamingIndex] = AiMessage(
                                content    = "⚠ ${error.message ?: "Something went wrong."}",
                                isUser     = false,
                                isError    = true,
                                isStreaming = false
                            )
                        }
                    }
                } else {
                    Gemini.chat(fullPrompt)
                        .onSuccess { response ->
                            val text = response.candidates.firstOrNull()
                                ?.content?.parts?.firstOrNull()?.asTextOrNull()
                                ?: "No response received."
                            if (streamingIndex < messages.size) {
                                messages[streamingIndex] = AiMessage(
                                    content      = text,
                                    isUser       = false,
                                    isStreaming  = false,
                                    sourcePrompt = userDisplayText,
                                    sourceFull   = fullPrompt
                                )
                                currentFilePath()?.let { persistHistory(it) }
                            }
                        }
                        .onFailure { error ->
                            if (streamingIndex < messages.size) {
                                messages[streamingIndex] = AiMessage(
                                    content    = "⚠ ${error.message ?: "Something went wrong."}",
                                    isUser     = false,
                                    isError    = true,
                                    isStreaming = false
                                )
                            }
                        }
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                if (streamingIndex < messages.size) {
                    val partial = messages[streamingIndex].content
                    messages[streamingIndex] = messages[streamingIndex].copy(
                        content    = partial.ifBlank { "_(generation stopped)_" },
                        isStreaming = false
                    )
                }
            } catch (e: Exception) {
                if (streamingIndex < messages.size) {
                    messages[streamingIndex] = AiMessage(
                        content    = "⚠ ${e.message ?: "Unknown error"}",
                        isUser     = false,
                        isError    = true,
                        isStreaming = false
                    )
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
        val atts   = pendingFiles.toList()
        if (prompt.isEmpty() && atts.isEmpty()) return
        if (isLoading) return
        inputText = ""
        pendingFiles.clear()
        attachError = null

        val fileInfo     = getFileContent()
        val selectedText = getSelectedText()

        val full = if (fileInfo != null)
            buildPrompt(prompt, fileInfo.first, fileInfo.second, selectedText, atts)
        else
            buildPromptNoFile(prompt, atts)

        val displayText = buildString {
            if (!selectedText.isNullOrBlank()) append("[selection] ")
            if (prompt.isNotBlank()) append(prompt)
            if (atts.isNotEmpty()) {
                if (isNotEmpty()) append(" ")
                append("[+${atts.size} file${if (atts.size > 1) "s" else ""}]")
            }
        }

        sendPromptInternal(displayText, full, atts)
    }

    fun sendQuickPrompt(label: String, prompt: String) {
        if (isLoading) return
        val fileInfo = getFileContent() ?: return
        sendPromptInternal(label, buildPrompt(prompt, fileInfo.first, fileInfo.second))
    }

    fun stopGeneration() { activeJob?.cancel() }

    fun regenerateMessage(aiMessageIndex: Int) {
        val msg  = messages.getOrNull(aiMessageIndex) ?: return
        val full = msg.sourceFull ?: return
        sendPromptInternal(msg.sourcePrompt ?: "Regenerate", full, replaceIndex = aiMessageIndex)
    }

    // ── Restore saved history ─────────────────────────────────────────────────
    val currentPath = currentFilePath()

    LaunchedEffect(currentPath) {
        val path = currentPath ?: return@LaunchedEffect
        if (path == lastAnalyzedFilePath) return@LaunchedEffect
        kotlinx.coroutines.delay(500)
        val saved = AiChatHistory.load(context, path)
        messages.clear()
        saved.forEach { m ->
            messages.add(AiMessage(content = m.content, isUser = m.isUser, isError = m.isError))
        }
        lastAnalyzedFilePath = path
    }

    // ── Derived state ─────────────────────────────────────────────────────────
    val fileInfo = remember(editorUiState.selectedFileIndex, editorUiState.openedFiles.size) {
        getFileContent()
    }
    val lineCount = remember(currentPath) {
        currentPath?.let { p -> try { java.io.File(p).readLines().size } catch (_: Exception) { null } }
    }
    val isThinking   = isLoading && messages.lastOrNull()?.let { it.isStreaming && it.content.isEmpty() } == true
    val selectedText = remember(editorUiState) { getSelectedText() }
    val canSend      = (inputText.isNotBlank() || pendingFiles.isNotEmpty()) && !isLoading

    // ─────────────────────────────────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────────────────────────────────

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null,
                            tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("AI Agent",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            if (OpenRouter.isConfigured()) "OpenRouter" else "Gemini",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ── Thinking button + dropdown ─────────────────────────────
                    Box {
                        IconButton(
                            onClick  = { thinkingMenuOpen = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Psychology,
                                contentDescription = "Thinking mode",
                                modifier = Modifier.size(20.dp),
                                tint = if (thinkingEnabled)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded         = thinkingMenuOpen,
                            onDismissRequest = { thinkingMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text    = { Text("Off", style = MaterialTheme.typography.bodySmall) },
                                onClick = { thinkingEnabled = false; thinkingMenuOpen = false }
                            )
                            DropdownMenuItem(
                                text    = { Text("Low", style = MaterialTheme.typography.bodySmall) },
                                onClick = { thinkingEnabled = true; thinkingLevel = "low"; thinkingMenuOpen = false }
                            )
                            DropdownMenuItem(
                                text    = { Text("Medium", style = MaterialTheme.typography.bodySmall) },
                                onClick = { thinkingEnabled = true; thinkingLevel = "medium"; thinkingMenuOpen = false }
                            )
                            DropdownMenuItem(
                                text    = { Text("High", style = MaterialTheme.typography.bodySmall) },
                                onClick = { thinkingEnabled = true; thinkingLevel = "high"; thinkingMenuOpen = false }
                            )
                        }
                    }

                    // ── Clear chat ─────────────────────────────────────────────
                    AnimatedVisibility(visible = messages.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                activeJob?.cancel()
                                messages.clear()
                                lastAnalyzedFilePath = null
                                currentPath?.let { AiChatHistory.clear(context, it) }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.Clear, "Clear chat",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Context chips (file, selection, thinking badge) ───────────────
            AnimatedVisibility(
                visible = fileInfo != null || !selectedText.isNullOrBlank() || thinkingEnabled,
                enter   = fadeIn() + slideInVertically(),
                exit    = fadeOut()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fileInfo != null) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AutoAwesome, null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${fileInfo.first} · ${if (lineCount != null && lineCount > 0) "$lineCount lines" else "loading…"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    AnimatedVisibility(visible = !selectedText.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("selection attached",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    AnimatedVisibility(visible = thinkingEnabled) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Psychology, null,
                                modifier = Modifier.size(10.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("thinking: $thinkingLevel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // ── Streaming progress bar ────────────────────────────────────────
            AnimatedVisibility(visible = isLoading && !isThinking) {
                LinearProgressIndicator(
                    modifier   = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(2.dp).clip(RoundedCornerShape(1.dp)),
                    color      = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            // ── Attach error banner ───────────────────────────────────────────
            AnimatedVisibility(visible = attachError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        attachError ?: "",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { attachError = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Message list ──────────────────────────────────────────────────
            LazyColumn(
                state   = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(Modifier.height(6.dp)) }

                if (messages.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.AutoAwesome, null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    if (fileInfo != null) "Context loaded — ask anything about ${fileInfo.first}"
                                    else "Open a file or attach files below to get started",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                items(messages, key = { msg -> System.identityHashCode(msg).toLong() }) { message ->
                    if (message.isStreaming && message.content.isEmpty()) return@items
                    val idx = messages.indexOf(message)
                    MessageBubble(
                        message      = message,
                        onCopy       = { clipboard.setText(AnnotatedString(message.content)) },
                        onCopyCode   = { code -> clipboard.setText(AnnotatedString(code)) },
                        onRegenerate = if (!message.isUser && !message.isStreaming && message.sourceFull != null)
                            {{ regenerateMessage(idx) }} else null,
                        onApplyCode  = { code -> applyCodeToEditor(code) }
                    )
                }

                item { Spacer(Modifier.height(4.dp)) }
            }

            // ── Pending file chips ────────────────────────────────────────────
            AnimatedVisibility(visible = pendingFiles.isNotEmpty()) {
                LazyRow(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pendingFiles) { f ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Outlined.AttachFile, null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    if (f.name.length > 20) f.name.take(18) + "…" else f.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    formatBytes(f.sizeBytes),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(
                                onClick  = { pendingFiles.remove(f) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Rounded.Close, "Remove",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── Quick-action chips ────────────────────────────────────────────
            AnimatedVisibility(
                visible = fileInfo != null && !isLoading,
                enter   = fadeIn() + slideInVertically { it / 2 },
                exit    = fadeOut()
            ) {
                LazyRow(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(QUICK_PROMPTS) { (label, prompt) ->
                        FilterChip(
                            selected = false,
                            onClick  = { sendQuickPrompt(label, prompt) },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                containerColor         = MaterialTheme.colorScheme.surfaceContainer,
                                labelColor             = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled             = true,
                                selected            = false,
                                borderColor         = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
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
                // Attach button
                BadgedBox(
                    badge = {
                        if (pendingFiles.isNotEmpty()) {
                            Badge { Text(pendingFiles.size.toString(),
                                style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                ) {
                    IconButton(
                        onClick  = { filePicker.launch("*/*") },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AttachFile,
                            contentDescription = "Attach file",
                            modifier = Modifier.size(20.dp),
                            tint = if (pendingFiles.isNotEmpty())
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value         = inputText,
                    onValueChange = { inputText = it },
                    modifier      = Modifier.weight(1f),
                    placeholder   = {
                        Text(
                            when {
                                pendingFiles.isNotEmpty() -> "Ask about your files…"
                                fileInfo != null -> "Ask about ${fileInfo.first}…"
                                else -> "Ask about your code…"
                            },
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
                        IconButton(onClick = { stopGeneration() }, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Rounded.Stop, "Stop generation",
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
                                tint = if (canSend)
                                    MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // ── Thinking overlay ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = isThinking,
            enter    = fadeIn(tween(200)),
            exit     = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        ) { ThinkingPill() }

        // ── Scroll-to-bottom FAB ──────────────────────────────────────────────
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
                Icon(Icons.Rounded.KeyboardArrowDown, "Scroll to bottom",
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─── Message bubble ───────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message     : AiMessage,
    onCopy      : () -> Unit,
    onCopyCode  : (String) -> Unit,
    onRegenerate: (() -> Unit)?,
    onApplyCode : (String) -> Unit
) {
    val isUser = message.isUser

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AutoAwesome, null,
                    tint     = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            modifier            = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                // Attachment chips above user bubble
                if (message.attachedFiles.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        items(message.attachedFiles) { f ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.AttachFile, null,
                                    modifier = Modifier.size(11.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (f.name.length > 16) f.name.take(14) + "…" else f.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                // Thinking badge
                if (message.usedThinking) {
                    Text(
                        "thinking mode",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (message.content.isNotBlank()) {
                    Card(
                        shape  = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text       = message.content,
                                modifier   = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style      = MaterialTheme.typography.bodyMedium,
                                color      = MaterialTheme.colorScheme.onPrimary,
                                lineHeight = 20.sp
                            )
                        }
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
                                Text(
                                    message.content,
                                    modifier   = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
                                    style      = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp
                                )
                            }
                            StreamingCursor()
                        }
                    }
                } else {
                    val segments = remember(message.content) { parseSegments(message.content) }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        segments.forEach { segment ->
                            when (segment) {
                                is MessageSegment.Prose -> {
                                    if (segment.text.isNotBlank()) {
                                        Card(
                                            shape  = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (message.isError)
                                                    MaterialTheme.colorScheme.errorContainer
                                                else MaterialTheme.colorScheme.surfaceContainer
                                            ),
                                            elevation = CardDefaults.cardElevation(0.dp)
                                        ) {
                                            MarkdownText(
                                                markdown         = segment.text,
                                                isTextSelectable = true,
                                                style            = MaterialTheme.typography.bodyMedium.copy(
                                                    fontSize   = 14.sp,
                                                    lineHeight = 20.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                                is MessageSegment.Code -> {
                                    CodeBlock(
                                        code    = segment.code,
                                        lang    = segment.lang,
                                        onCopy  = { onCopyCode(segment.code) },
                                        onApply = { onApplyCode(segment.code) }
                                    )
                                }
                            }
                        }
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.ContentCopy, "Copy",
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                            if (onRegenerate != null) {
                                IconButton(onClick = onRegenerate, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.Refresh, "Regenerate",
                                        modifier = Modifier.size(13.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
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
private fun CodeBlock(
    code   : String,
    lang   : String,
    onCopy : () -> Unit,
    onApply: () -> Unit
) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick        = onApply,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier       = Modifier.height(26.dp)
                ) {
                    Text("Apply",
                        style      = MaterialTheme.typography.labelSmall,
                        color      = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick        = onCopy,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier       = Modifier.height(26.dp)
                ) {
                    Text("Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        SelectionContainer {
            Text(
                text       = code,
                modifier   = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                fontFamily = FontFamily.Monospace,
                fontSize   = 12.sp,
                lineHeight = 18.sp,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─── Thinking pill ────────────────────────────────────────────────────────────

@Composable
private fun ThinkingPill() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking_shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue  = -200f,
        targetValue   = 400f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerOffset"
    )
    val pillAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.88f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pillAlpha"
    )
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f * pillAlpha))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.matchParentSize().background(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), Color.Transparent),
                    start = Offset(shimmerOffset, 0f),
                    end   = Offset(shimmerOffset + 200f, 60f)
                )
            )
        )
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.AutoAwesome, null,
                tint     = MaterialTheme.colorScheme.primary.copy(alpha = pillAlpha),
                modifier = Modifier.size(14.dp))
            Text("Thinking…",
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = pillAlpha))
            ThinkingDots()
        }
    }
}

@Composable
private fun ThinkingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue  = 0.4f,
                targetValue   = 1f,
                animationSpec = infiniteRepeatable(
                    tween(500, delayMillis = index * 140, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "dot_scale_$index"
            )
            Box(
                modifier = Modifier
                    .size((5 * scale).dp.coerceAtLeast(2.dp))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f + 0.4f * scale))
            )
        }
    }
}

// ─── Streaming cursor ─────────────────────────────────────────────────────────

@Composable
private fun StreamingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "cursorAlpha"
    )
    Box(
        modifier = Modifier
            .padding(start = 12.dp, bottom = 8.dp)
            .size(width = 8.dp, height = 14.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                shape = RoundedCornerShape(2.dp)
            )
    )
}
