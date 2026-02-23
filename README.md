# WORMGPT – Android AI Chatbot

Native Android AI chatbot (Kotlin, Jetpack Compose) using Firebase (Auth, Firestore, Storage) and DeepSeek via Firebase Cloud Functions. Red/black theme, sidebar, full-screen markdown AI responses, file upload (premium), and subscription management.

---

## Get the APK and install it (easiest)

**No Android Studio or dev tools needed.** Follow **[EASIEST_WAY_TO_APK.md](EASIEST_WAY_TO_APK.md)**:

1. Upload this project to GitHub (drag-and-drop the folder).
2. In the repo: **Actions** → **Build APK** → **Run workflow** → wait, then download the **app-debug** artifact.
3. Unzip it, copy **app-debug.apk** to your phone, open it and install.

---

## Setup (Firebase, DeepSeek, etc.)

### 1. Firebase

- Create a project at [Firebase Console](https://console.firebase.google.com).
- Enable **Authentication** (Email/Password and Google Sign-In).
- Create a **Firestore** database.
- Enable **Storage**.
- Enable **Cloud Functions** (Blaze plan required for outbound calls).
- Add an Android app with package `com.wormgpt.app`, download `google-services.json` and replace `app/google-services.json`.
- In Project settings > General, copy the **Web client ID** and set it in `app/src/main/res/values/strings.xml` as `default_web_client_id`.

### 2. DeepSeek API key

- Get an API key from [DeepSeek](https://platform.deepseek.com).
- Set it for Cloud Functions (run from project root):
  - **Option A (one command):**  
    **PowerShell:** `$env:DEEPSEEK_API_KEY="sk-your-key"; node scripts/set-deepseek-key.js`  
    **CMD:** `set DEEPSEEK_API_KEY=sk-your-key && node scripts/set-deepseek-key.js`  
    Then run `firebase deploy --only functions`.
  - **Option B:** `firebase functions:config:set deepseek.api_key="YOUR_KEY"` then deploy.
  - **Option C:** Set env `DEEPSEEK_API_KEY` in your deployment (e.g. Secret Manager).

### 3. Deploy Cloud Functions

```bash
cd functions
npm install
npm run build
cd ..
firebase deploy --only functions
```

After deploy, copy the **chatStream** function URL (e.g. `https://us-central1-YOUR_PROJECT.cloudfunctions.net`) and set it in `app/build.gradle.kts` as `buildConfigField("String", "CLOUD_FUNCTIONS_URL", "\"https://...\"")`.

### 4. Firestore indexes

Deploy indexes (or let Firebase create them from the console when prompted):

```bash
firebase deploy --only firestore:indexes
```

### 5. Android (build the APK)

**With Android Studio:** Open the project, sync Gradle, and run on a device or emulator.

**Without Android Studio:** Use the command line to build the APK. See **[BUILD_WITHOUT_ANDROID_STUDIO.md](BUILD_WITHOUT_ANDROID_STUDIO.md)** for:

- Installing Java 17 and Android command-line tools only
- Creating the Gradle wrapper (one script or `gradle wrapper`)
- Running `.\gradlew.bat assembleDebug` to get `app\build\outputs\apk\debug\app-debug.apk`

## Features

- **Auth:** Email/password and Google Sign-In; user profile and subscription in Firestore.
- **Chat:** Create chats, send messages, streamed DeepSeek responses, full-width markdown (headers, bold, lists).
- **Sidebar:** Recent chats, New chat, Manage subscription, Sign out.
- **File upload:** Premium users can attach files (stored in Firebase Storage; URLs sent with the message).
- **Subscription:** Free vs premium; file attachments require premium. Set `subscriptionTier` and `subscriptionExpiresAt` in Firestore `users/{uid}` (e.g. from Firebase Console or a future payment flow).

## Project structure

- `app/` – Android app (Compose UI, ViewModels, repositories).
- `functions/` – Cloud Functions (DeepSeek proxy: callable `chat`, HTTPS `chatStream`).
- `firestore.rules`, `storage.rules`, `firestore.indexes.json` – Firebase config.
