package com.wormgpt.app.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    suspend fun uploadFile(userId: String, chatId: String, messageId: String, uri: Uri, mimeType: String?): String {
        val ext = mimeType?.let { it.split("/").lastOrNull() } ?: "bin"
        val path = "users/$userId/uploads/$chatId/$messageId/${UUID.randomUUID()}.$ext"
        val ref = storage.reference.child(path)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
