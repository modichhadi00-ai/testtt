package com.wormgpt.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wormgpt.app.data.model.Message
import com.wormgpt.app.data.remote.WormGptApi
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val streamingContent: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentChatId: String? = null
)

class ChatViewModel(
    private val chatId: String?,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val api: WormGptApi
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState(currentChatId = chatId))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        if (chatId != null) {
            viewModelScope.launch {
                runCatching {
                    chatRepository.getMessages(chatId).collect { list ->
                        _state.update { it.copy(messages = list) }
                    }
                }.onFailure { e ->
                    _state.update { it.copy(error = "Load failed: ${e.message}") }
                }
            }
        }
    }

    fun sendMessage(text: String, attachmentUrls: List<String> = emptyList()) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                var cid = _state.value.currentChatId
                if (cid == null) {
                    val uid = authRepository.currentUserId ?: run {
                        _state.update { it.copy(isLoading = false, error = "Not signed in") }
                        return@launch
                    }
                    val chat = chatRepository.createChat(uid, "New chat")
                    cid = chat.id
                    _state.update { it.copy(currentChatId = cid) }
                }

                runCatching { chatRepository.addMessage(cid, "user", text, attachmentUrls) }

                val userMsg = Message(id = "", chatId = cid, role = "user", content = text, attachmentUrls = attachmentUrls, createdAt = System.currentTimeMillis() / 1000)
                val messages = _state.value.messages + userMsg
                _state.update { it.copy(messages = messages, streamingContent = "") }

                val apiMessages = messages.map { msg ->
                    val content = if (msg.attachmentUrls.isNotEmpty())
                        msg.content + "\n\n[Attachments: ${msg.attachmentUrls.joinToString()}]"
                    else msg.content
                    WormGptApi.ChatMessage(msg.role, content)
                }
                val systemPrompt = "You are WORMGPT, a helpful AI assistant. Respond in clear, well-formatted markdown when appropriate (headers, bold, lists, code)."
                val fullMessages = listOf(WormGptApi.ChatMessage("system", systemPrompt)) + apiMessages

                var streamedContent = ""
                val result = api.chatStream(
                    messages = fullMessages,
                    onChunk = { chunk ->
                        streamedContent += chunk
                        _state.update { it.copy(streamingContent = streamedContent) }
                    },
                    onDone = { }
                )

                result.onFailure { e ->
                    val msg = e.message ?: "Request failed"
                    val display = when {
                        msg.contains("your-project-id") -> "Cloud Functions not deployed. Deploy: firebase deploy --only functions"
                        msg.contains("Unable to resolve host") || msg.contains("UnknownHostException") -> "Cannot reach server. Check internet or deploy Cloud Functions."
                        msg.contains("404") -> "Cloud Function not found. Deploy: firebase deploy --only functions"
                        msg.contains("401") || msg.contains("403") -> "Auth error. Try signing out and back in."
                        msg.contains("500") -> "Server error. Check Cloud Functions logs."
                        msg.contains("DEEPSEEK") || msg.contains("DeepSeek") || msg.contains("api_key") -> "DeepSeek API key invalid or not set. Check Settings."
                        msg.contains("SocketTimeout") || msg.contains("timeout") -> "Request timed out. Check internet and try again."
                        else -> msg
                    }
                    _state.update { it.copy(isLoading = false, error = display, streamingContent = "") }
                    return@launch
                }

                val fullContent = _state.value.streamingContent
                if (fullContent.isEmpty()) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            streamingContent = "",
                            error = "No response from AI. Check your DeepSeek API key in Settings and your internet connection."
                        )
                    }
                    return@launch
                }
                if (fullContent.isNotEmpty()) {
                    runCatching { chatRepository.addMessage(cid!!, "assistant", fullContent) }
                }
                val aiMsg = Message(id = "", chatId = cid!!, role = "assistant", content = fullContent, createdAt = System.currentTimeMillis() / 1000)
                _state.update {
                    it.copy(
                        messages = it.messages + aiMsg,
                        streamingContent = "",
                        isLoading = false
                    )
                }
                if (messages.size == 1) {
                    runCatching { chatRepository.updateChatTitle(cid, text.take(50).ifEmpty { "New chat" }) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unexpected error",
                        streamingContent = ""
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
