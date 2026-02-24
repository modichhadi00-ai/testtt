package com.wormgpt.app.ui.chat

import android.net.Uri
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.wormgpt.app.BuildConfig
import com.wormgpt.app.data.model.Message
import com.wormgpt.app.data.remote.WormGptApi
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.data.repository.ChatRepository
import com.wormgpt.app.data.repository.StorageRepository
import com.wormgpt.app.ui.theme.SurfaceCard
import com.wormgpt.app.ui.theme.SurfaceVariant
import com.wormgpt.app.ui.theme.WormRed
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
        if (userId.isNotEmpty()) {
            runCatching { userRepository.getProfile(userId) }.onSuccess { userProfile = it }
        }
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
            val extra = if (state.streamingContent.isNotEmpty()) 1 else (if (state.isLoading) 1 else 0)
            val lastIndex = (state.messages.size + extra - 1).coerceAtLeast(0)
            runCatching { listState.animateScrollToItem(lastIndex) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.error != null) {
            val err = state.error!!
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.messages, key = { msg -> msg.id.ifEmpty { "local-${msg.createdAt}-${msg.content.hashCode()}" } }) { msg ->
                if (msg.role == "user") {
                    UserMessageBubble(message = msg)
                } else {
                    AssistantMessageBubble(content = msg.content, context = context)
                }
            }
            if (state.isLoading && state.streamingContent.isEmpty()) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = WormRed
                                )
                                Text("Thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (state.streamingContent.isNotEmpty()) {
                item(key = "streaming") {
                    AssistantMessageBubble(content = state.streamingContent, context = context)
                }
            }
        }

        // Input bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    enabled = canAttachFiles && !state.isLoading && !isUploading,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = if (canAttachFiles) "Attach" else "Attach (premium)",
                        tint = if (canAttachFiles) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                var input by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WormRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = WormRed,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                IconButton(
                    onClick = {
                        viewModel.sendMessage(input, pendingAttachmentUrls)
                        input = ""
                        pendingAttachmentUrls = emptyList()
                    },
                    enabled = (input.isNotBlank() || pendingAttachmentUrls.isNotEmpty()) && !state.isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WormRed)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        if (pendingAttachmentUrls.isNotEmpty()) {
            Text(
                "${pendingAttachmentUrls.size} file(s) attached",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        if (!canAttachFiles) {
            Text(
                "Premium: attach files",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun UserMessageBubble(message: Message) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(20.dp, 20.dp, 8.dp, 20.dp),
            colors = CardDefaults.cardColors(containerColor = WormRed),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(
    content: String,
    context: android.content.Context
) {
    val markwon = remember { Markwon.create(context) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextColor(android.graphics.Color.WHITE)
                        setPadding(0, 0, 0, 0)
                        textSize = 15f
                        setLineSpacing(4f, 1.2f)
                    }
                },
                update = { textView ->
                    runCatching {
                        markwon.setMarkdown(textView, content.ifEmpty { "_Thinking..._" })
                    }
                }
            )
        }
    }
}
