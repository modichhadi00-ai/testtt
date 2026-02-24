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
 * Calls the Firebase Cloud Function for chat (callable for non-stream, HTTPS for stream).
 * Set [cloudFunctionBaseUrl] to your Cloud Functions URL, e.g. https://us-central1-PROJECT_ID.cloudfunctions.net
 */
class WormGptApi(
    private val cloudFunctionBaseUrl: String,
    private val getToken: suspend () -> String?
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    data class ChatMessage(val role: String, val content: String)

    /**
     * Non-streaming chat via callable (requires Firebase SDK callable in future or HTTP wrapper).
     * For now we use the HTTPS chatStream with stream=false by calling a callable-equivalent.
     * Firebase Callable is invoked from Android with FirebaseFunctions.getInstance().httpsCallable("chat").call(...).
     * This class uses raw HTTP: so we need an HTTP endpoint. The plan had both callable (chat) and HTTPS (chatStream).
     * So: use callable from Android via Firebase Functions SDK, not this OkHttp client for non-stream.
     * For streaming we use this client to POST to chatStream with Bearer token.
     */
    suspend fun chatStream(messages: List<ChatMessage>, onChunk: (String) -> Unit, onDone: () -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext Result.failure(SecurityException("Not authenticated"))
        val body = JSONObject().apply {
            put("messages", JSONArray(messages.map { JSONObject().apply { put("role", it.role); put("content", it.content) } }))
            put("stream", true)
            put("model", "deepseek-chat")
            put("max_tokens", 4096)
        }
        val request = Request.Builder()
            .url("$cloudFunctionBaseUrl/chatStream")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $token")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: response.message
                    return@withContext Result.failure(RuntimeException("API error: ${response.code} $err"))
                }
                val reader = BufferedReader(InputStreamReader(response.body?.byteStream() ?: return@withContext Result.failure(RuntimeException("Empty body"))))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: continue
                    if (l.startsWith("data: ")) {
                        val data = l.removePrefix("data: ")
                        if (data == "[DONE]") {
                            onDone()
                            break
                        }
                        try {
                            val json = JSONObject(data)
                            val delta = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")
                            val content = delta?.optString("content", "") ?: ""
                            if (content.isNotEmpty()) onChunk(content)
                        } catch (_: Exception) { }
                    }
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
