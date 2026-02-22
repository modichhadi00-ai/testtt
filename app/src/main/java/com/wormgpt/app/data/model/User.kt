package com.wormgpt.app.data.model

data class UserProfile(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val subscriptionTier: String = "free",
    val subscriptionExpiresAt: Long? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun isPremium(): Boolean {
        if (subscriptionTier != "premium") return false
        val expires = subscriptionExpiresAt ?: return true
        return expires * 1000 > System.currentTimeMillis()
    }
}
