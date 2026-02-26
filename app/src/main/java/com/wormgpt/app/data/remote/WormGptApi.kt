package com.wormgpt.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

private const val DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions"

/**
 * When user has set a DeepSeek API key in Settings, calls DeepSeek API directly.
 * Otherwise calls the Firebase Cloud Function chatStream.
 */
class WormGptApi(
    private val cloudFunctionBaseUrl: String,
    private val getToken: suspend () -> String?,
    private val getDeepSeekApiKey: () -> String? = { null }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(50, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    data class ChatMessage(val role: String, val content: String)

    suspend fun chatStream(messages: List<ChatMessage>, onChunk: (String) -> Unit, onDone: () -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        val apiKey = getDeepSeekApiKey()?.trim()?.takeIf { it.isNotBlank() }
        if (apiKey != null) {
            chatStreamDirectDeepSeek(messages, apiKey, onChunk, onDone)
        } else {
            chatStreamViaCloudFunction(messages, onChunk, onDone)
        }
    }

    /**
     * Call DeepSeek API directly. No Cloud Function needed.
     */
    private fun chatStreamDirectDeepSeek(
        messages: List<ChatMessage>,
        apiKey: String,
        onChunk: (String) -> Unit,
        onDone: () -> Unit
    ): Result<Unit> {
        val body = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", JSONArray(messages.map { JSONObject().apply { put("role", it.role); put("content", it.content) } }))
            put("stream", true)
            put("max_tokens", 4096)
        }
        val request = Request.Builder()
            .url(DEEPSEEK_API_URL)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()
        return executeStreamRequest(request, onChunk, onDone)
    }

    /**
     * Call Firebase Cloud Function chatStream (for users without their own API key).
     */
    private suspend fun chatStreamViaCloudFunction(
        messages: List<ChatMessage>,
        onChunk: (String) -> Unit,
        onDone: () -> Unit
    ): Result<Unit> {
        val token = getToken() ?: return Result.failure(SecurityException("Not authenticated"))
        val body = JSONObject().apply {
            put("messages", JSONArray(messages.map { JSONObject().apply { put("role", it.role); put("content", it.content) } }))
            put("stream", true)
            put("model", "deepseek-chat")
            put("max_tokens", 4096)
        }
        val url = cloudFunctionBaseUrl.trimEnd('/') + "/chatStream"
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $token")
            .build()
        return executeStreamRequest(request, onChunk, onDone)
    }

    private fun executeStreamRequest(
        request: Request,
        onChunk: (String) -> Unit,
        onDone: () -> Unit
    ): Result<Unit> {
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: response.message
                    val err = try {
                        if (errBody.startsWith("{")) JSONObject(errBody).optString("error", errBody) else errBody
                    } catch (_: Exception) { errBody }
                    return Result.failure(RuntimeException("${response.code}: $err"))
                }
                val contentType = response.header("Content-Type") ?: ""
                val stream = response.body?.byteStream() ?: run {
                    onDone()
                    return Result.success(Unit)
                }
                if (contentType.contains("application/json")) {
                    val str = stream.bufferedReader().use { it.readText() }
                    try {
                        val json = JSONObject(str)
                        val err = json.optString("error", "")
                        if (err.isNotEmpty()) return Result.failure(RuntimeException(err))
                        val content = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "") ?: ""
                        if (content.isNotEmpty()) onChunk(content)
                    } catch (_: Exception) { }
                    onDone()
                    return Result.success(Unit)
                }
                var doneCalled = false
                BufferedReader(InputStreamReader(stream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val l = line?.trim()?.trimEnd('\r') ?: continue
                        if (l.startsWith("data: ")) {
                            val data = l.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                if (!doneCalled) { onDone(); doneCalled = true }
                                break
                            }
                            if (data.isEmpty()) continue
                            try {
                                val json = JSONObject(data)
                                val delta = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
                                val content = delta?.optString("content", "") ?: ""
                                if (content.isNotEmpty()) onChunk(content)
                            } catch (_: Exception) { }
                        }
                    }
                    if (!doneCalled) onDone()
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            val message = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
            Result.failure(RuntimeException("$message", e))
        }
    }
}
