package com.wormgpt.app.ui.chat

import android.net.Uri
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var input by remember { mutableStateOf("") }
    val canSend = (input.isNotBlank() || pendingAttachmentUrls.isNotEmpty()) && !state.isLoading

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
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        if (state.messages.isEmpty() && !state.isLoading && state.streamingContent.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WORMGPT", style = MaterialTheme.typography.headlineMedium, color = WormRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ask me anything", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.messages, key = { msg -> msg.id.ifEmpty { "local-${msg.createdAt}-${msg.content.hashCode()}" } }) { msg ->
                    if (msg.role == "user") {
                        UserBubble(message = msg)
                    } else {
                        AiBubble(content = msg.content, context = context)
                    }
                }
                if (state.isLoading && state.streamingContent.isEmpty()) {
                    item {
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = WormRed)
                            Text("Thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (state.streamingContent.isNotEmpty()) {
                    item(key = "streaming") {
                        AiBubble(content = state.streamingContent, context = context)
                    }
                }
            }
        }

        if (pendingAttachmentUrls.isNotEmpty()) {
            Text(
                "${pendingAttachmentUrls.size} file(s) attached",
                style = MaterialTheme.typography.labelSmall,
                color = WormRed,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = { filePickerLauncher.launch("*/*") },
                enabled = canAttachFiles && !state.isLoading && !isUploading,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = if (canAttachFiles) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceCard)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (input.isEmpty()) {
                    Text("Message...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                    cursorBrush = SolidColor(WormRed),
                    maxLines = 4,
                    singleLine = false
                )
            }
            IconButton(
                onClick = {
                    if (canSend) {
                        viewModel.sendMessage(input, pendingAttachmentUrls)
                        input = ""
                        pendingAttachmentUrls = emptyList()
                    }
                },
                enabled = canSend,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (canSend) WormRed else MaterialTheme.colorScheme.outline)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun UserBubble(message: Message) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp),
            colors = CardDefaults.cardColors(containerColor = WormRed),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun AiBubble(content: String, context: android.content.Context) {
    val markwon = remember { Markwon.create(context) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                TextView(ctx).apply {
                    setTextColor(android.graphics.Color.WHITE)
                    setPadding(32, 24, 32, 24)
                    textSize = 15f
                    setLineSpacing(4f, 1.15f)
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
