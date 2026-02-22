package com.wormgpt.app

import android.app.Application
import com.google.firebase.FirebaseApp

class WormGptApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
