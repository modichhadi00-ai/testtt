# Full app setup: login, database, and chat

Follow these steps **in order** so login, Firestore, and the AI chat all work.

---

## 1. Create a Firebase project

1. Go to [Firebase Console](https://console.firebase.google.com).
2. Click **Add project** (or use an existing project).
3. Name it (e.g. **WORMGPT**), enable/disable Google Analytics as you like, then **Create project**.

---

## 2. Enable Storage (for file uploads; optional)

1. **Build** → **Storage** → **Get started**.
2. **Start in test mode** or **Production mode** → choose region → **Done**.
3. We’ll deploy storage rules in step 9.

---

## 3. Enable Firestore (database)

1. In the left menu: **Build** → **Firestore Database**.
2. Click **Create database**.
3. Choose **Start in test mode** (we’ll lock it down with rules in step 9) or **Production mode**.
4. Pick a region (e.g. **us-central1**) → **Enable**.
5. Wait until the database is created.

---

## 4. Enable Authentication (login)

1. **Build** → **Authentication** → **Get started**.
2. **Sign-in method** tab:
   - Click **Email/Password** → **Enable** → **Save**.
   - Click **Google** → **Enable**, set support email → **Save**.

---

## 5. Add the Android app and get `google-services.json`

1. **Project settings** (gear) → **General** → **Your apps**.
2. Click **Add app** → **Android**.
3. **Android package name:** `com.wormgpt.app` (must match exactly).
4. (Optional) App nickname, e.g. **WORMGPT**.
5. **Register app**.
6. **Download `google-services.json`** and **replace** the file in this project:  
   `app/google-services.json`
7. Click **Next** until done.

---

## 6. Set Web Client ID (required for Google Sign-In)

1. In **Project settings** → **General** → **Your apps**, select your **Android** app.
2. Find **Web client ID** (or open [Google Cloud Console](https://console.cloud.google.com) → same project → **APIs & Services** → **Credentials** → **OAuth 2.0 Client IDs** → **Web client**).
3. Copy the Client ID (looks like `123456789012-xxxx.apps.googleusercontent.com`).
4. In this project open **`app/src/main/res/values/strings.xml`**.
5. Replace the placeholder with your Web Client ID:
   ```xml
   <string name="default_web_client_id" translatable="false">123456789012-xxxx.apps.googleusercontent.com</string>
   ```

---

## 7. Enable Identity Toolkit API (fixes “configuration_not_found”)

1. Open [Google Cloud Console](https://console.cloud.google.com).
2. Select the **same project** as your Firebase project (top bar).
3. **APIs & Services** → **Library**.
4. Search **Identity Toolkit API** → open it → **Enable**.

---

## 8. Add SHA-1 for Google Sign-In (Android)

1. On your PC, open a terminal in a folder where you have `keytool` (e.g. Android SDK or Java bin).
2. Run (default debug keystore on Windows):
   ```bash
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
3. Copy the **SHA-1** line (e.g. `AA:BB:CC:...`).
4. In **Firebase Console** → **Project settings** → **Your apps** → your Android app → **Add fingerprint** → paste **SHA-1** → **Save**.

---

## 9. Deploy Firestore, Storage, and indexes

From your **project root** (where `firebase.json` is):

1. Install Firebase CLI if needed: `npm install -g firebase-tools`
2. Log in: `firebase login`
3. Select your project: `firebase use --add` (pick your project and optionally give it an alias).
4. Deploy Firestore and Storage:
   ```bash
   firebase deploy --only firestore
   firebase deploy --only storage
   ```
   This deploys `firestore.rules`, `firestore.indexes.json`, and `storage.rules` so the app can read/write chats, messages, and file uploads.

---

## 10. DeepSeek API key and Cloud Functions

1. Get an API key from [DeepSeek Platform](https://platform.deepseek.com).
2. Set it for Firebase (from project root):
   - **PowerShell:**  
     `$env:DEEPSEEK_API_KEY="sk-your-key"; node scripts/set-deepseek-key.js`
   - **CMD:**  
     `set DEEPSEEK_API_KEY=sk-your-key && node scripts/set-deepseek-key.js`
3. Deploy functions:
   ```bash
   cd functions
   npm install
   npm run build
   cd ..
   firebase deploy --only functions
   ```
   The app uses your **project ID** from `google-services.json` to build the Cloud Functions URL automatically (e.g. `https://us-central1-YOUR_PROJECT.cloudfunctions.net`). No need to edit `build.gradle.kts` for the URL if you use the default region **us-central1**.

---

## 11. Build and install the app

1. Put your real **`app/google-services.json`** in the project (step 4).
2. Set **`default_web_client_id`** in **`app/src/main/res/values/strings.xml`** (step 5).
3. Build the APK:
   - With Android Studio: open project → **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**.
   - From command line: `.\gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug` (Mac/Linux).
4. Install on device:
   - From Android Studio: Run on device/emulator.
   - Or copy `app/build/outputs/apk/debug/app-debug.apk` to your phone and install (you may need to allow installs from unknown sources).  
   - Or with ADB: `adb install -r -t app/build/outputs/apk/debug/app-debug.apk`.

---

## Checklist

- [ ] Firebase project created  
- [ ] Storage enabled (optional, for file uploads)  
- [ ] Firestore database created  
- [ ] Authentication: Email/Password and Google enabled  
- [ ] Android app added, **`google-services.json`** in `app/`  
- [ ] **Web Client ID** in `strings.xml`  
- [ ] **Identity Toolkit API** enabled (Google Cloud)  
- [ ] **SHA-1** added in Firebase (Android app)  
- [ ] **Firestore** and **Storage** deployed: `firebase deploy --only firestore` and `firebase deploy --only storage`  
- [ ] **DeepSeek** key set and **Functions** deployed: `firebase deploy --only functions`  
- [ ] App rebuilt and installed  

After this, you should be able to **sign up / sign in** (email or Google), and **chat** (messages and chats stored in Firestore, AI via Cloud Functions).

For more detail on login errors, see **FIREBASE_SETUP.md**.
