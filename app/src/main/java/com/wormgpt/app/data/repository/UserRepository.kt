package com.wormgpt.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.wormgpt.app.data.model.UserProfile
import kotlinx.coroutines.tasks.await
class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("users")

    suspend fun getProfile(uid: String): UserProfile? {
        val doc = usersCollection.document(uid).get().await()
        return doc.toObject(UserProfile::class.java)?.copy(uid = uid)
            ?: doc.data?.let { data ->
                UserProfile(
                    uid = uid,
                    email = data["email"] as? String,
                    displayName = data["displayName"] as? String,
                    photoUrl = data["photoUrl"] as? String,
                    subscriptionTier = (data["subscriptionTier"] as? String) ?: "free",
                    subscriptionExpiresAt = (data["subscriptionExpiresAt"] as? com.google.firebase.Timestamp)?.seconds,
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0L,
                    updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0L
                )
            }
    }

    suspend fun createOrUpdateProfile(uid: String, email: String?, displayName: String?, photoUrl: String?) {
        val ref = usersCollection.document(uid)
        val existing = ref.get().await()
        val now = System.currentTimeMillis() / 1000
        if (!existing.exists()) {
            ref.set(
                mapOf(
                    "email" to email,
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "subscriptionTier" to "free",
                    "subscriptionExpiresAt" to null,
                    "createdAt" to com.google.firebase.Timestamp(now, 0),
                    "updatedAt" to com.google.firebase.Timestamp(now, 0)
                )
            ).await()
        } else {
            ref.update(
                mapOf(
                    "email" to email,
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "updatedAt" to com.google.firebase.Timestamp(now, 0)
                )
            ).await()
        }
    }
}
