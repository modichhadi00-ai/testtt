package com.wormgpt.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.wormgpt.app.data.model.Chat
import com.wormgpt.app.data.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val chatsCollection = firestore.collection("chats")
    private val messagesCollection = firestore.collection("messages")

    suspend fun createChat(userId: String, title: String = "New chat"): Chat {
        val now = System.currentTimeMillis() / 1000
        val data = mapOf(
            "userId" to userId,
            "title" to title,
            "updatedAt" to Timestamp(now, 0),
            "createdAt" to Timestamp(now, 0)
        )
        val ref = chatsCollection.document()
        ref.set(data).await()
        return Chat(
            id = ref.id,
            userId = userId,
            title = title,
            updatedAt = now,
            createdAt = now
        )
    }

    fun getChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val listener = chatsCollection
            .whereEqualTo("userId", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    val data = doc.data ?: return@map null
                    Chat(
                        id = doc.id,
                        userId = (data["userId"] as? String) ?: "",
                        title = (data["title"] as? String) ?: "New chat",
                        updatedAt = (data["updatedAt"] as? Timestamp)?.seconds ?: 0L,
                        createdAt = (data["createdAt"] as? Timestamp)?.seconds ?: 0L
                    )
                }?.filterNotNull() ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun getChat(chatId: String): Chat? {
        val doc = chatsCollection.document(chatId).get().await()
        val data = doc.data ?: return null
        return Chat(
            id = doc.id,
            userId = (data["userId"] as? String) ?: "",
            title = (data["title"] as? String) ?: "New chat",
            updatedAt = (data["updatedAt"] as? Timestamp)?.seconds ?: 0L,
            createdAt = (data["createdAt"] as? Timestamp)?.seconds ?: 0L
        )
    }

    suspend fun updateChatTitle(chatId: String, title: String) {
        val now = System.currentTimeMillis() / 1000
        chatsCollection.document(chatId).update(
            mapOf(
                "title" to title,
                "updatedAt" to Timestamp(now, 0)
            )
        ).await()
    }

    suspend fun updateChatTimestamp(chatId: String) {
        val now = System.currentTimeMillis() / 1000
        chatsCollection.document(chatId).update("updatedAt", Timestamp(now, 0)).await()
    }

    suspend fun deleteChat(chatId: String) {
        val messageDocs = messagesCollection.whereEqualTo("chatId", chatId).get().await()
        messageDocs.documents.forEach { it.reference.delete().await() }
        chatsCollection.document(chatId).delete().await()
    }

    fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = messagesCollection
            .whereEqualTo("chatId", chatId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.map { doc ->
                    val data = doc.data ?: return@map null
                    val urls = (data["attachmentUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    Message(
                        id = doc.id,
                        chatId = (data["chatId"] as? String) ?: "",
                        role = (data["role"] as? String) ?: "user",
                        content = (data["content"] as? String) ?: "",
                        attachmentUrls = urls,
                        createdAt = (data["createdAt"] as? Timestamp)?.seconds ?: 0L
                    )
                }?.filterNotNull() ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addMessage(chatId: String, role: String, content: String, attachmentUrls: List<String> = emptyList()): Message {
        val now = System.currentTimeMillis() / 1000
        val ref = messagesCollection.document()
        val data = mapOf(
            "chatId" to chatId,
            "role" to role,
            "content" to content,
            "attachmentUrls" to attachmentUrls,
            "createdAt" to Timestamp(now, 0)
        )
        ref.set(data).await()
        updateChatTimestamp(chatId)
        return Message(
            id = ref.id,
            chatId = chatId,
            role = role,
            content = content,
            attachmentUrls = attachmentUrls,
            createdAt = now
        )
    }
}
