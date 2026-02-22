package com.wormgpt.app.ui.chat

import android.net.Uri
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wormgpt.app.BuildConfig
import com.wormgpt.app.data.model.Message
import com.wormgpt.app.data.remote.WormGptApi
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.data.repository.ChatRepository
import com.wormgpt.app.data.repository.StorageRepository
import com.wormgpt.app.ui.theme.SurfaceVariant
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ChatScreen(
    chatId: String?,
    authRepository: AuthRepository
) {
    val context = LocalContext.current
    val chatRepository = remember { ChatRepository() }
    val api = remember(authRepository) {
        WormGptApi(BuildConfig.CLOUD_FUNCTIONS_URL) { authRepository.getIdToken() }
    }
    val viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = chatId ?: "new",
        factory = ChatViewModelFactory(chatId, authRepository, chatRepository, api)
    )
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var pendingAttachmentUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val storageRepository = remember { StorageRepository() }
    val userId = authRepository.currentUserId ?: ""
    val userRepository = remember { com.wormgpt.app.data.repository.UserRepository() }
    var userProfile by remember { mutableStateOf<com.wormgpt.app.data.model.UserProfile?>(null) }
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) userProfile = userRepository.getProfile(userId)
    }
    val canAttachFiles = userProfile?.isPremium() == true

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isUploading = true
            runCatching {
                storageRepository.uploadFile(userId, "pending", UUID.randomUUID().toString(), uri, context.contentResolver.getType(uri))
            }.onSuccess { url ->
                pendingAttachmentUrls = pendingAttachmentUrls + url
            }
            isUploading = false
        }
    }

    LaunchedEffect(state.messages.size, state.streamingContent) {
        if (state.messages.isNotEmpty() || state.streamingContent.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        state.error?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer)
                    IconButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.messages) { msg ->
                if (msg.role == "user") {
                    UserMessageBubble(message = msg)
                } else {
                    AssistantMessageFullWidth(content = msg.content, context = context)
                }
            }
            if (state.isLoading && state.streamingContent.isEmpty()) {
                item {
                    Row(Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    }
                }
            }
            if (state.streamingContent.isNotEmpty()) {
                item(key = "streaming") {
                    AssistantMessageFullWidth(content = state.streamingContent, context = context)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            var input by remember { mutableStateOf("") }
            IconButton(
                onClick = { filePickerLauncher.launch("*/*") },
                enabled = canAttachFiles && !state.isLoading && !isUploading
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = if (canAttachFiles) "Attach file" else "Attach file (premium)")
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") },
                maxLines = 4
            )
            Spacer(modifier = Modifier.widthIn(8.dp))
            IconButton(
                onClick = {
                    viewModel.sendMessage(input, pendingAttachmentUrls)
                    input = ""
                    pendingAttachmentUrls = emptyList()
                },
                enabled = (input.isNotBlank() || pendingAttachmentUrls.isNotEmpty()) && !state.isLoading
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
        if (pendingAttachmentUrls.isNotEmpty()) {
            Text(
                "${pendingAttachmentUrls.size} file(s) attached",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        if (!canAttachFiles) {
            Text(
                "Upgrade to premium to attach files",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun UserMessageBubble(
    message: Message
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun AssistantMessageFullWidth(
    content: String,
    context: android.content.Context
) {
    val markwon = remember { Markwon.create(context) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
    ) {
        AndroidView(
            factory = { ctx ->
                TextView(ctx).apply {
                    setTextColor(android.graphics.Color.WHITE)
                    setPadding(24, 24, 24, 24)
                    textSize = 16f
                }
            },
            update = { textView ->
                markwon.setMarkdown(textView, content.ifEmpty { "_Thinking..._" })
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )
    }
}
