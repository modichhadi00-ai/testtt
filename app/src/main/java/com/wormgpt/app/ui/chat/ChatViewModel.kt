package com.wormgpt.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
                chatRepository.getMessages(chatId).collect { list ->
                    _state.update { it.copy(messages = list) }
                }
            }
        }
    }

    fun sendMessage(text: String, attachmentUrls: List<String> = emptyList()) {
        if (text.isBlank()) return
        viewModelScope.launch {
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
            chatRepository.addMessage(cid, "user", text, attachmentUrls)
            val messages = _state.value.messages + Message(id = "", chatId = cid, role = "user", content = text, attachmentUrls = attachmentUrls, createdAt = System.currentTimeMillis() / 1000)
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
                _state.update {
                    it.copy(isLoading = false, error = e.message ?: "Request failed", streamingContent = "")
                }
                return@launch
            }
            val fullContent = _state.value.streamingContent
            chatRepository.addMessage(cid!!, "assistant", fullContent)
            val updatedMessages = _state.value.messages + Message(id = "", chatId = cid, role = "assistant", content = fullContent, createdAt = System.currentTimeMillis() / 1000)
            _state.update {
                it.copy(
                    messages = updatedMessages,
                    streamingContent = "",
                    isLoading = false
                )
            }
            if (messages.size == 1) {
                chatRepository.updateChatTitle(cid, text.take(50).ifEmpty { "New chat" })
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
