package io.vscodex.net.core.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Context exposed to local AI tools. No shell or arbitrary-path access is provided. */
data class AgentToolContext(
    val workspaceRoot: File?,
    val currentFile: File? = null,
    val selectedText: String? = null,
)

data class AgentToolResult(
    val output: String,
    val requiresApproval: Boolean = false,
    val preview: String? = null,
)

private fun File.safeChild(relativePath: String): File? {
    val root = canonicalFile
    val candidate = File(root, relativePath).canonicalFile
    return candidate.takeIf { it == root || it.path.startsWith(root.path + File.separator) }
}

object AgentToolRegistry {
    private const val MAX_READ_CHARS = 20_000
    private const val MAX_RESULTS = 200
    private const val MAX_SEARCH_FILES = 600

    /** OpenAI-compatible tool declarations. */
    fun definitions(): JSONArray = JSONArray().apply {
        put(functionTool("list_workspace_files", "List files and directories in the workspace.", mapOf(
            "path" to JSONObject().put("type", "string").put("description", "Relative directory path; empty for workspace root")
        ), listOf("path")))
        put(functionTool("read_workspace_file", "Read a text file from the workspace.", mapOf(
            "path" to JSONObject().put("type", "string").put("description", "Relative file path")
        ), listOf("path")))
        put(functionTool("search_workspace", "Search text across workspace source files.", mapOf(
            "query" to JSONObject().put("type", "string"),
            "path" to JSONObject().put("type", "string").put("description", "Optional relative directory path")
        ), listOf("query")))
        put(functionTool("propose_file_edit", "Prepare an edit proposal. The user must approve before any file is changed.", mapOf(
            "path" to JSONObject().put("type", "string"),
            "content" to JSONObject().put("type", "string")
        ), listOf("path", "content")))
    }

    fun execute(name: String, args: JSONObject, context: AgentToolContext): AgentToolResult {
        return when (name) {
            "list_workspace_files" -> listFiles(args.optString("path", ""), context)
            "read_workspace_file" -> readFile(args.optString("path"), context)
            "search_workspace" -> search(args.optString("query"), args.optString("path", ""), context)
            "propose_file_edit" -> proposeEdit(args.optString("path"), args.optString("content"), context)
            else -> AgentToolResult("Unknown tool: $name")
        }
    }

    private fun listFiles(path: String, context: AgentToolContext): AgentToolResult {
        val root = context.workspaceRoot ?: return AgentToolResult("No workspace is open.")
        val directory = root.safeChild(path)?.takeIf { it.isDirectory }
            ?: return AgentToolResult("Directory not found: $path")
        val entries = directory.listFiles()?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase() }))
            ?.take(MAX_RESULTS).orEmpty()
        val prefix = if (path.isBlank()) "" else path.trimEnd('/') + "/"
        return AgentToolResult(entries.joinToString("\n") { if (it.isDirectory) "${prefix}${it.name}/" else "$prefix${it.name}" }.ifBlank { "(empty directory)" })
    }

    private fun readFile(path: String, context: AgentToolContext): AgentToolResult {
        val root = context.workspaceRoot ?: return AgentToolResult("No workspace is open.")
        val file = root.safeChild(path)?.takeIf { it.isFile }
            ?: return AgentToolResult("File not found: $path")
        val text = runCatching { file.readText(Charsets.UTF_8) }.getOrElse { return AgentToolResult("Cannot read $path: ${it.message}") }
        return AgentToolResult(text.take(MAX_READ_CHARS) + if (text.length > MAX_READ_CHARS) "\n…[truncated]" else "")
    }

    private fun search(query: String, path: String, context: AgentToolContext): AgentToolResult {
        if (query.isBlank()) return AgentToolResult("Search query is empty.")
        val root = context.workspaceRoot ?: return AgentToolResult("No workspace is open.")
        val base = root.safeChild(path)?.takeIf { it.isDirectory } ?: root
        val results = mutableListOf<String>()
        var inspected = 0
        base.walkTopDown().onEnter { !it.name.startsWith(".") && it.name !in setOf("build", "node_modules", ".gradle") }.forEach { file ->
            if (inspected++ >= MAX_SEARCH_FILES || results.size >= MAX_RESULTS) return@forEach
            if (!file.isFile || file.length() > 1_000_000) return@forEach
            val match = runCatching { file.readText(Charsets.UTF_8).contains(query, ignoreCase = true) }.getOrDefault(false)
            if (match) results += file.relativeTo(root).path
        }
        return AgentToolResult(results.joinToString("\n").ifBlank { "No matches found." })
    }

    private fun proposeEdit(path: String, content: String, context: AgentToolContext): AgentToolResult {
        val root = context.workspaceRoot ?: return AgentToolResult("No workspace is open.")
        val file = root.safeChild(path) ?: return AgentToolResult("Unsafe path: $path")
        val old = if (file.isFile) runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("") else ""
        val preview = "Edit proposal for ${file.relativeTo(root).path}: ${old.length} -> ${content.length} characters"
        return AgentToolResult("I prepared an edit proposal. Nothing has been changed.", requiresApproval = true, preview = preview)
    }

    private fun functionTool(name: String, description: String, properties: Map<String, JSONObject>, required: List<String>): JSONObject =
        JSONObject().put("type", "function").put("function", JSONObject()
            .put("name", name)
            .put("description", description)
            .put("parameters", JSONObject().put("type", "object").put("properties", JSONObject().apply { properties.forEach { (key, value) -> put(key, value) } }).put("required", JSONArray(required))))
}
