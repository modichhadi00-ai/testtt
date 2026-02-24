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

/**
 * Calls the Firebase Cloud Function chatStream (HTTPS POST, SSE response).
 */
class WormGptApi(
    private val cloudFunctionBaseUrl: String,
    private val getToken: suspend () -> String?
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    data class ChatMessage(val role: String, val content: String)

    suspend fun chatStream(messages: List<ChatMessage>, onChunk: (String) -> Unit, onDone: () -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext Result.failure(SecurityException("Not authenticated"))
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
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: response.message
                    val err = try {
                        if (errBody.startsWith("{")) JSONObject(errBody).optString("error", errBody) else errBody
                    } catch (_: Exception) { errBody }
                    return@withContext Result.failure(RuntimeException("${response.code} $err"))
                }
                val contentType = response.header("Content-Type") ?: ""
                val stream = response.body?.byteStream() ?: run {
                    onDone()
                    return@withContext Result.success(Unit)
                }
                if (contentType.contains("application/json")) {
                    val str = stream.bufferedReader().use { it.readText() }
                    try {
                        val json = JSONObject(str)
                        val err = json.optString("error", "")
                        if (err.isNotEmpty()) return@withContext Result.failure(RuntimeException(err))
                        val content = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content", "") ?: ""
                        if (content.isNotEmpty()) onChunk(content)
                    } catch (_: Exception) { }
                    onDone()
                    return@withContext Result.success(Unit)
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
            Result.failure(e)
        }
    }
}
