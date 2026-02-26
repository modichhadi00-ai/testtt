package com.wormgpt.app.ui.chat

import android.net.Uri
import android.widget.TextView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
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
import com.wormgpt.app.data.local.AppPreferences
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
    val prefs = remember { AppPreferences(context) }
    val api = remember(authRepository, prefs) {
        WormGptApi(
            cloudFunctionBaseUrl = BuildConfig.CLOUD_FUNCTIONS_URL,
            getToken = { authRepository.getIdToken() },
            getDeepSeekApiKey = { prefs.getDeepSeekApiKey() }
        )
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
            val err = state.error!!
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.error != null) {
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Something went wrong. Please try again.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    Spacer(modifier = Modifier.height(8.dp))
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        if (state.messages.isEmpty() && !state.isLoading && state.streamingContent.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(
                        "WORMGPT",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                        color = WormRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Ask me anything",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.messages, key = { msg -> msg.id.ifEmpty { "local-${msg.createdAt}-${msg.content.hashCode()}" } }) { msg ->
                    if (msg.role == "user") {
                        UserBubble(message = msg)
                    } else {
                        AiBubble(content = msg.content, context = context)
                    }
                }
                if (state.isLoading && state.streamingContent.isEmpty()) {
                    item(key = "typing") {
                        TypingIndicator()
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = { filePickerLauncher.launch("*/*") },
                enabled = canAttachFiles && !state.isLoading && !isUploading,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach",
                    tint = if (canAttachFiles) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceCard)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (canSend) WormRed else SurfaceCard)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.5f at 0
                1f at 150
                0.5f at 300
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.5f at 0
                0.5f at 150
                1f at 300
                0.5f at 450
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 900
                0.5f at 0
                0.5f at 300
                1f at 450
                0.5f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )
    Card(
        modifier = Modifier.widthIn(max = 80.dp),
        shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).scale(dot1).background(WormRed, CircleShape))
            Box(modifier = Modifier.size(8.dp).scale(dot2).background(WormRed, CircleShape))
            Box(modifier = Modifier.size(8.dp).scale(dot3).background(WormRed, CircleShape))
        }
    }
}

@Composable
private fun UserBubble(message: Message) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
            colors = CardDefaults.cardColors(containerColor = WormRed),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AiBubble(content: String, context: android.content.Context) {
    val markwon = remember { Markwon.create(context) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                val padPx = (24 * ctx.resources.displayMetrics.density).toInt()
                TextView(ctx).apply {
                    setTextColor(android.graphics.Color.WHITE)
                    setPadding(padPx, padPx, padPx, padPx)
                    textSize = 16f
                    setLineSpacing(6f, 1.2f)
                    setLinkTextColor(android.graphics.Color.parseColor("#E53935"))
                    movementMethod = android.text.method.LinkMovementMethod.getInstance()
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
