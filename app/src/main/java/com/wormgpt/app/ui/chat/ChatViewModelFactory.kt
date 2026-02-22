package com.wormgpt.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wormgpt.app.data.remote.WormGptApi
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.data.repository.ChatRepository

class ChatViewModelFactory(
    private val chatId: String?,
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val api: WormGptApi
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(chatId, authRepository, chatRepository, api) as T
    }
}
