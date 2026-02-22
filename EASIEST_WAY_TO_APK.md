# Easiest way: from this code to APK on your phone

**You need:** a GitHub account (free). **You do not need:** Android Studio, Java, or any SDK on your PC.

---

## Before Step 1: Set your Git name and email (one-time)

Git needs your name and email for commits. In **Cursor**, open the **Terminal** (`Ctrl+` backtick or View → Terminal) and run these two lines. Use your real name and the **same email as your GitHub account**:

```bash
git config --global user.name "Your Name"
git config --global user.email "your-email@example.com"
```

Example: if your GitHub email is `modi@gmail.com`, use:
```bash
git config --global user.name "Modi"
git config --global user.email "modi@gmail.com"
```

After that, you can do Step 1 (Source Control → commit → Publish to GitHub).

---

## Step 1: Push this project to GitHub (Cursor is already connected)

1. On [github.com](https://github.com), create a **New repository** (e.g. name `wormgpt`, Public). Do not add a README or .gitignore.
2. In **Cursor**: open **Source Control** (sidebar icon or `Ctrl+Shift+G`).
3. If this folder is not a Git repo: click **Initialize Repository**, then stage all files and commit (e.g. "Initial commit").
4. Click **Publish to GitHub** (or add the new repo as remote and push). Push to your new repo—all files, including `.github`, go up.

**Prefer upload?** On GitHub, create the repo, then use **Upload files** and drag everything from `wormfinal apk` (including `.github`).

---

## Step 2: Build the APK

1. In your repo, open the **Actions** tab.
2. In the left sidebar, click **"Build APK"**.
3. On the right, click **"Run workflow"** → green **Run workflow**.
4. Wait a few minutes until the run shows a green check.
5. Click the run, then under **Artifacts** click **app-debug** to download a zip.
6. Unzip it: inside is **app-debug.apk**. That's your installable APK.

---

## Step 3: Install on your phone

- **Option A:** Copy `app-debug.apk` to your phone (USB, email, Google Drive, etc.). On the phone, open the APK file and tap **Install** (allow "Install from unknown sources" if asked).
- **Option B:** If the APK is on your PC, you can also install via USB with [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) and:  
  `adb install -r app-debug.apk`

Done. The app will appear as **WORMGPT** on your phone.

---

**If the build fails:** Make sure the whole project was pushed (including `.github` and `gradle`). Then run the workflow again.
