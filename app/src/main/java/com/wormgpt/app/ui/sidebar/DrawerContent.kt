package com.wormgpt.app.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wormgpt.app.data.model.Chat
import kotlinx.coroutines.launch
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.data.repository.ChatRepository
import com.wormgpt.app.ui.theme.SurfaceDark
import com.wormgpt.app.ui.theme.WormRed

@Composable
fun DrawerContent(
    authRepository: AuthRepository,
    currentChatId: String?,
    onChatSelected: (String) -> Unit,
    onNewChat: () -> Unit,
    onDeleteChat: (String) -> Unit,
    onSettings: () -> Unit,
    onManageSubscription: () -> Unit,
    onSignOut: () -> Unit
) {
    val userId = authRepository.currentUserId ?: return
    val chatRepository = remember { ChatRepository() }
    val chats by chatRepository.getChats(userId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(SurfaceDark)
            .padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(WormRed),
                contentAlignment = Alignment.Center
            ) {
                Text("W", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text("WORMGPT", style = MaterialTheme.typography.titleMedium, color = WormRed)
                val label = authRepository.currentUser?.email ?: "Guest"
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
        DrawerItem(icon = Icons.Default.Add, label = "New chat", onClick = onNewChat)
        DrawerItem(icon = Icons.Default.Settings, label = "Settings", onClick = onSettings)
        Text(
            "Recent",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(count = chats.size, key = { chats[it].id }) { index ->
                val chat = chats[index]
                DrawerChatItem(
                    chat = chat,
                    isSelected = chat.id == currentChatId,
                    onClick = { onChatSelected(chat.id) },
                    onDelete = {
                        scope.launch {
                            runCatching { chatRepository.deleteChat(chat.id) }
                            onDeleteChat(chat.id)
                        }
                    }
                )
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp))
        DrawerItem(icon = Icons.Default.CardMembership, label = "Subscription", onClick = onManageSubscription)
        DrawerItem(icon = Icons.Default.Logout, label = "Sign out", onClick = onSignOut, tint = WormRed)
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}

@Composable
private fun DrawerChatItem(chat: Chat, isSelected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Chat,
            contentDescription = null,
            tint = if (isSelected) WormRed else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = chat.title.ifEmpty { "New chat" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp),
            content = {
                Icon(Icons.Default.Delete, contentDescription = "Delete chat", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        )
    }
}
