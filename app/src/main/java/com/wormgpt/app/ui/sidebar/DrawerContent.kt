package com.wormgpt.app.ui.sidebar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wormgpt.app.data.model.Chat
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.data.repository.ChatRepository

@Composable
fun DrawerContent(
    authRepository: AuthRepository,
    onChatSelected: (String) -> Unit,
    onNewChat: () -> Unit,
    onManageSubscription: () -> Unit,
    onSignOut: () -> Unit
) {
    val userId = authRepository.currentUserId ?: return
    val chatRepository = remember { ChatRepository() }
    val chats by chatRepository.getChats(userId).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WORMGPT",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        authRepository.currentUser?.email?.let { email ->
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNewChat)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("New chat")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Recent chats",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(count = chats.size, key = { chats[it].id }) { index ->
                val chat = chats[index]
                DrawerChatItem(
                    chat = chat,
                    onClick = { onChatSelected(chat.id) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onManageSubscription)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.CardMembership, contentDescription = null)
            Text("Manage subscription")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSignOut)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Text("Sign out")
        }
    }
}

@Composable
private fun DrawerChatItem(
    chat: Chat,
    onClick: () -> Unit
) {
    Text(
        text = chat.title.ifEmpty { "New chat" },
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    )
}
