package io.vscodex.net.core.ai

import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.coroutineContext

sealed class AgentEvent {
    data class AssistantText(val text: String) : AgentEvent()
    data class ToolStarted(val name: String, val arguments: JSONObject) : AgentEvent()
    data class ToolFinished(val name: String, val result: AgentToolResult) : AgentEvent()
    data class ApprovalRequired(val name: String, val preview: String) : AgentEvent()
}

data class AgentRunResult(
    val answer: String,
    val messages: JSONArray,
    val approvalRequired: Boolean = false,
    val approvalPreview: String? = null,
)

/**
 * Manus-like local agent loop. The model plans, requests bounded tools, observes
 * their results, and continues until it can answer. Mutating operations are
 * represented as proposals and never applied implicitly.
 */
class AiAgentOrchestrator(
    private val maxSteps: Int = 8,
) {
    suspend fun run(
        prompt: String,
        context: AgentToolContext,
        previousMessages: JSONArray = JSONArray(),
        onEvent: suspend (AgentEvent) -> Unit = {},
    ): Result<AgentRunResult> {
        return runCatching {
            val messages = copyArray(previousMessages)
            if (messages.length() == 0) {
                messages.put(JSONObject().put("role", "system").put("content", systemPrompt()))
            }
            messages.put(JSONObject().put("role", "user").put("content", prompt))

            var finalAnswer = ""
            var step = 0
            while (step++ < maxSteps) {
                coroutineContext.ensureActive()
                val turn = OpenRouter.agentTurn(messages, AgentToolRegistry.definitions()).getOrThrow()
                val assistant = JSONObject().put("role", "assistant").put("content", turn.content)
                if (turn.toolCalls.isNotEmpty()) {
                    val calls = JSONArray()
                    turn.toolCalls.forEach { call ->
                        calls.put(JSONObject()
                            .put("id", call.id)
                            .put("type", "function")
                            .put("function", JSONObject()
                                .put("name", call.name)
                                .put("arguments", call.arguments.toString())))
                    }
                    assistant.put("tool_calls", calls)
                }
                messages.put(assistant)
                if (turn.content.isNotBlank()) {
                    finalAnswer = turn.content
                    onEvent(AgentEvent.AssistantText(turn.content))
                }
                if (turn.toolCalls.isEmpty()) break

                for (call in turn.toolCalls) {
                    coroutineContext.ensureActive()
                    onEvent(AgentEvent.ToolStarted(call.name, call.arguments))
                    val result = AgentToolRegistry.execute(call.name, call.arguments, context)
                    onEvent(AgentEvent.ToolFinished(call.name, result))
                    if (result.requiresApproval) {
                        onEvent(AgentEvent.ApprovalRequired(call.name, result.preview ?: result.output))
                        return@runCatching AgentRunResult(
                            answer = finalAnswer.ifBlank { result.output },
                            messages = messages,
                            approvalRequired = true,
                            approvalPreview = result.preview,
                        )
                    }
                    messages.put(JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", call.id)
                        .put("name", call.name)
                        .put("content", result.output))
                }
            }

            AgentRunResult(
                answer = finalAnswer.ifBlank { "The agent reached its step limit without a final answer." },
                messages = messages,
            )
        }
    }

    private fun systemPrompt(): String = """
        You are the VSCodeX workspace agent. Work like a careful software agent:
        understand the request, inspect the workspace with tools when needed, cite
        file paths in your answer, and explain your plan before consequential work.
        You may list, read, and search workspace files. For edits, only create a
        proposal with propose_file_edit; never claim an edit was applied. Do not
        invent tool results. Keep responses concise but useful.
    """.trimIndent()

    private fun copyArray(source: JSONArray): JSONArray {
        val result = JSONArray()
        for (i in 0 until source.length()) result.put(source.get(i))
        return result
    }
}
