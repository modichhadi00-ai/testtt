# Building WORMGPT APK Without Android Studio

Two options: **easiest = build in the cloud (GitHub)**. Or build on your PC with the steps below.

---

## Easiest: Build on GitHub (no Java or Android SDK on your PC)

1. Push this project to **GitHub** (create a repo, then push the folder).
2. Open the repo on GitHub → **Actions** tab → workflow **"Build APK"**.
3. Click **Run workflow** → Run.
4. When the run finishes, open it and under **Artifacts** download **app-debug** (the APK).
5. Copy the APK to your phone and install.

You don’t need Java, Gradle, or the Android SDK on your computer.

---

## Option 2: Build on your PC (command line)

### 1. Install Java 17

The project needs **Java 17** to build.

**Option A – Winget (Windows):**
```powershell
winget install Microsoft.OpenJDK.17
```

**Option B – Manual:**  
Download and install from [Adoptium](https://adoptium.net/) or [Microsoft Build of OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/download).

Then close and reopen your terminal. Check:
```powershell
java -version
```
You should see version 17.

---

## 2. Create the Gradle Wrapper (one-time)

The project uses the Gradle wrapper (`gradlew.bat`). You need the wrapper JAR once.

**Easiest – run the setup script (uses a direct CDN download):**

```powershell
cd "c:\Users\modi\Desktop\wormfinal apk"
powershell -ExecutionPolicy Bypass -File setup-gradle-wrapper.ps1
```

This downloads `gradle-wrapper.jar` into `gradle\wrapper\`.

**Option B – Install Gradle, then generate wrapper:**

```powershell
winget install Gradle.Gradle
cd "c:\Users\modi\Desktop\wormfinal apk"
gradle wrapper
```

After this, you can build with `gradlew.bat` and do **not** need Gradle installed globally.

---

## 3. Install Android Command-Line Tools (no Android Studio)

1. **Download** the command-line tools for Windows:  
   https://developer.android.com/studio#command-tools  
   (Under “Command line tools only”, choose Windows.)

2. **Unzip** the downloaded file (e.g. `commandlinetools-win-*.zip`) into a folder, for example:  
   `C:\Android\cmdline-tools`

3. **Create** this folder (if it doesn’t exist):  
   `C:\Android\cmdline-tools\latest`

4. **Move** the contents of the unzipped `cmdline-tools` folder (the `bin`, `lib` folders, etc.) into `C:\Android\cmdline-tools\latest`, so you have:  
   `C:\Android\cmdline-tools\latest\bin\sdkmanager.bat`

5. **Set environment variables** (PowerShell, or System Properties → Environment Variables):

   ```powershell
   [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Android", "User")
   [System.Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\Android\cmdline-tools\latest\bin;C:\Android\platform-tools", "User")
   ```

   Close and reopen your terminal so the new `Path` is used.

6. **Install SDK components** (accept licenses when asked):

   ```powershell
   sdkmanager --sdk_root=C:\Android "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```

   If `sdkmanager` is not found, run it with the full path:

   ```powershell
   C:\Android\cmdline-tools\latest\bin\sdkmanager.bat --sdk_root=C:\Android "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```

---

## 4. Build the APK

In the project folder:

```powershell
cd "c:\Users\modi\Desktop\wormfinal apk"
.\gradlew.bat assembleDebug
```

- First run will download Gradle and dependencies (can take several minutes).
- When it finishes, the **debug APK** is at:  
  `app\build\outputs\apk\debug\app-debug.apk`

Install on a device (USB debugging on, or copy the APK and open it):

```powershell
C:\Android\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```

Or copy `app-debug.apk` to your phone and open it there (allow “Install from unknown sources” if asked).

---

## 5. Optional: Release APK (for distribution)

1. Create a keystore (one-time):

   ```powershell
   keytool -genkey -v -keystore wormgpt-release.keystore -alias wormgpt -keyalg RSA -keysize 2048 -validity 10000
   ```

2. In `app/build.gradle.kts`, add a `signingConfigs` block and use it in `buildTypes.release` (or follow Android’s “Sign your app” docs).

3. Build:

   ```powershell
   .\gradlew.bat assembleRelease
   ```

   Output: `app\build\outputs\apk\release\app-release.apk`

---

## Troubleshooting

| Problem | What to do |
|--------|------------|
| `JAVA_HOME is not set` | Set `JAVA_HOME` to your JDK 17 install (e.g. `C:\Program Files\Microsoft\jdk-17.0.x`) and add `%JAVA_HOME%\bin` to `Path`. |
| `ANDROID_HOME` or SDK not found | Make sure `ANDROID_HOME` is set to the folder that contains `platforms` and `build-tools` (e.g. `C:\Android`). |
| `gradle-wrapper.jar` missing | Do step 2 again (install Gradle and run `gradle wrapper`, or download the JAR into `gradle\wrapper\`). |
| Build fails on “SDK location not found” | Create `wormfinal apk\local.properties` with one line: `sdk.dir=C:\\Android` (use your actual SDK path; double backslashes). |

**Example `local.properties`** (create this file in the project root if the build can’t find the SDK):

```properties
sdk.dir=C:\\Android
```

Replace `C:\\Android` with the path where you installed the Android SDK (the folder that contains `platforms` and `build-tools`).
