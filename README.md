# Zayaan Apps — Complete Digital Application Store (Android APK + Responsive Web)

Official digital application marketplace and centralized APK distribution platform for **Zayaan Apps**.

- **Application Name:** Zayaan Apps
- **Android Application ID:** `com.zayaanapps.store`
- **GitHub Repository:** `zayaanapps/zayaan-apps`
- **Live GitHub Pages Website URL:** `https://zayaanapps.github.io/zayaan-apps/`
- **Preferred Firebase Project Name:** `Zayaan Apps` (Preferred Project ID: `zayaan-apps` — *Note: Firebase project IDs must be globally unique and lowercase across all of Google Cloud*).

---

## 1. Dual-Product Workspace Architecture

Because Google AI Studio's live streaming emulator compiles and runs native Android Gradle/Kotlin builds directly, this repository provides **both**:

1. **Live Native Android Store Application (`/app/`)**:
   - Runs directly in the Google AI Studio Streaming Emulator and builds a signed, installable Android APK (`com.zayaanapps.store`).
   - Connected to your live provisioned **Cloud Firestore** database and **Firebase Authentication** (Google Sign-In via Jetpack Credential Manager).
   - Includes real-time catalog browsing, search, category filtering (`Games`, `Tools`, `AI Apps`, `Entertainment`, `Productivity`, `Education`, `Utilities`, `Other`), **Android `PackageManager` installed-app detection**, **`versionCode` comparison**, **GitHub Releases APK downloading with byte progress**, and **Android `FileProvider` Package Installer integration**.
2. **Complete Shared Flutter/Dart Android + Responsive Web Codebase (`/flutter_zayaan_apps/`)**:
   - Shared Material 3 Flutter/Dart project targeting both **Android APK** (`flutter build apk --release`) and **Flutter Web** (`flutter build web --release --base-href "/zayaan-apps/"`).
   - Strictly separates Android-only package installation (`installer_service_android.dart`) from browser APK downloading (`installer_service_web.dart`) using Dart conditional exports (`installer_service.dart`).
3. **Automated GitHub Actions CI/CD (`/.github/workflows/deploy.yml`)**:
   - Automatically builds your Flutter Android Release APK and publishes it to **GitHub Releases** (checking existing tags first so releases are never accidentally overwritten).
   - Builds your responsive Flutter Web store with `--base-href "/zayaan-apps/"` and `404.html` SPA routing fallback, deploying automatically to **GitHub Pages** (`https://zayaanapps.github.io/zayaan-apps/`).
4. **Centralized Multi-Repo App Distribution (`/.github/workflows/publish-distributed-app-template.yml`)**:
   - Allows separate repositories (such as `zayaanapps/zayaan-calculator` or `zayaanapps/zayaan-notes`) to build their own APKs, publish to their own GitHub Releases, and automatically update the central Zayaan Apps Firestore `apps/{appId}` document without rebuilding Zayaan Apps.

---

## 2. Step-by-Step Beginner Guide (Managing from Your Android Smartphone)

### Step A: Push to GitHub (`zayaanapps/zayaan-apps`)
1. Create a new public repository named **`zayaan-apps`** under your **`zayaanapps`** GitHub account (`https://github.com/zayaanapps/zayaan-apps`).
2. In Google AI Studio, use the **Export to GitHub** or **Download ZIP** option in the settings menu to push this project to the `main` branch of `zayaanapps/zayaan-apps`.

### Step B: Enable GitHub Pages
1. On your phone browser, open `https://github.com/zayaanapps/zayaan-apps/settings/pages`.
2. Under **Build and deployment > Source**, select **GitHub Actions**.
3. Whenever you push to `main`, `.github/workflows/deploy.yml` will automatically deploy your website to `https://zayaanapps.github.io/zayaan-apps/` and publish the Android APK to `https://github.com/zayaanapps/zayaan-apps/releases`.

### Step C: Free Services & Firebase Spark Plan Notes
- **100% Free Services Used:** GitHub Free, GitHub Releases (for unlimited public APK hosting), GitHub Pages (for web hosting), Firebase Authentication (free Google Sign-In), and Cloud Firestore Spark free quota.
- **FCM Push Notifications vs. Spark Free Alternative:** Sending automated server-to-device push notifications via Firebase Cloud Functions requires upgrading to the paid Firebase Blaze plan. To keep Zayaan Apps **100% free on the Firebase Spark plan**, the app automatically compares installed `versionCode` values against real-time Firestore `apps` snapshots and triggers local Android system update notifications (`POST_NOTIFICATIONS`) whenever an installed Zayaan app has a newer release!
