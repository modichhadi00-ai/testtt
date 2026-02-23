# Fix "configuration_not_found" and "Google sign in error"

Do these steps in order. Your app needs a **real** Firebase project and correct Web Client ID.

---

## 1. Use your real Firebase project

1. Go to [Firebase Console](https://console.firebase.google.com) and create or open a project.
2. **Authentication** → **Sign-in method**:
   - Enable **Email/Password**.
   - Enable **Google** (and set support email if asked).
3. **Project settings** (gear) → **General** → under **Your apps**:
   - Add an Android app if needed: package name **`com.wormgpt.app`**.
   - Download **`google-services.json`** and **replace** `app/google-services.json` in this project.

---

## 2. Fix "configuration_not_found" (email/password sign up)

1. Open [Google Cloud Console](https://console.cloud.google.com).
2. Select the **same project** as your Firebase project (top bar).
3. Go to **APIs & Services** → **Library**.
4. Search for **Identity Toolkit API** → open it → click **Enable**.

Without this API, email/password sign-in returns `configuration_not_found`.

---

## 3. Fix Google Sign-In (Web Client ID + SHA-1)

### A. Get your Web Client ID

1. In **Firebase Console** → **Project settings** → **General**.
2. Scroll to **Your apps** → select your Android app.
3. Find **"Web client ID"** (or **"Web API Key"** section). It looks like:  
   `123456789012-xxxxxxxxxx.apps.googleusercontent.com`
4. If you don’t see it: open **Google Cloud Console** → **APIs & Services** → **Credentials** → under **OAuth 2.0 Client IDs** open the **Web client** (type "Web application"). Copy the **Client ID** (same format as above).

### B. Put it in the app

1. In this project open **`app/src/main/res/values/strings.xml`**.
2. Replace the placeholder with your real Web Client ID:
   ```xml
   <string name="default_web_client_id" translatable="false">123456789012-xxxxxxxxxx.apps.googleusercontent.com</string>
   ```
   Use your **full** Client ID (including `.apps.googleusercontent.com`).

### C. Add SHA-1 to Firebase (required for Google Sign-In)

1. On your PC, open Command Prompt in the folder where **adb** is (e.g. `platform-tools`), or where Java’s `keytool` is.
2. Run (debug keystore; default path on Windows):
   ```bash
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
3. Copy the **SHA-1** line (e.g. `AA:BB:CC:...`).
4. In **Firebase Console** → **Project settings** → **Your apps** → your Android app → **Add fingerprint** → paste **SHA-1** → Save.

Then **rebuild the app** and install the new APK.

---

## 4. Rebuild and test

- Replace `app/google-services.json` with the one from Firebase.
- Set `default_web_client_id` in `strings.xml` to your Web Client ID.
- Enable Identity Toolkit API in Google Cloud.
- Add SHA-1 in Firebase.
- Rebuild: run your build (e.g. GitHub Actions or `./gradlew assembleDebug`) and install the new APK.

After this, email/password sign-up and Google Sign-In should work.
