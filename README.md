# 🤖 Yapper Android Lab

Native Android application and client engine for **Yapper** (`studio-web` and `yap-worker`).

---

## 1. Scope & Ownership Mandate

The **Yapper Android Lab** owns:
- **Android Native Integration**: Modern Jetpack Compose, Material 3, Kotlin 2.x, Navigation 3, Unidirectional Data Flow.
- **Google Play Testing**: Closed beta (20 testers for 14 days), Play Integrity API, App Bundle (.aab).
- **Background Uploads**: WorkManager `dataSync` Foreground Service, resilient chunked TUS uploads, server upload-state reconciliation (resolving Backlog **B-1**).
- **Media Picker & Export**: Zero-permission Photo Picker (`PickMultipleVisualMedia` up to 15 clips), tap-order preservation, zero-RAM buffered staging, lossless 10-bit HDR/HEVC export to `Movies/Yapper`.
- **Notifications**: Android 13+ `POST_NOTIFICATIONS` runtime permissions, silent upload progress channels, high-priority render completion heads-up alerts.
- **Least-Privilege Permissions**: Zero broad file system permissions, Scoped Storage compliance.
- **Play Billing / Entitlements**: Google Play Billing Library 7.x, subscriptions (`yap_pro_monthly`, `yap_creator_annual`), consumable minute packs, pending purchase recovery, and Supabase server verification.
- **Account Deletion**: Google Play Policy compliance with in-app deletion pathway (`DELETE /api/me`) and public web portal (`https://yapperzdelight.com/account/delete`).
- **Real-Device Testing**: Samsung One UI, Google Pixel, Xiaomi, OnePlus fragmentation matrix across Android 10 through Android 16.

---

## 2. Directory Structure

```
yapper-android/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/
│       │       ├── com/example/yapper/ (UI, MainActivity, Theme, Navigation)
│       │       └── com/yapper/android/
│       │           ├── account/ (AccountDeletionManager)
│       │           ├── billing/ (PlayBillingManager)
│       │           ├── media/ (PhotoPickerManager, MediaStoreExporter)
│       │           ├── model/ (SelectionDraft, StagedClip)
│       │           └── upload/ (YapUploadManager)
│       └── test/ (SelectionDraftTest, ViewModel tests)
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── local.properties
```

---

## 3. Toolchain & Build Verification

- **Android CLI**: Official Google Android CLI `1.0.16457483` at `~/.local/bin/android`
- **Target Platform**: `platforms/android-36` (Android 16 API 36)
- **Compile / Test**:
  ```bash
  # Check Android environment
  android info

  # Run on connected device or emulator
  android run app
  ```

---

## 4. Integration Contract with Yapper Backend

- **Upload State Reconciliation**:
  `GET /api/jobs/{id}/upload-state` -> returns `receivedBytes`, `isFullyUploaded`. If `isFullyUploaded == true`, never force re-upload.
- **Job Confirmation**:
  `POST /api/jobs/{id}/uploaded` -> confirms chunk transfer complete and triggers Modal worker queue dispatch.
- **Push Notification Sync**:
  FCM device token registered with Supabase `user_devices` table; deep links to `yapper://yap/{jobId}`.
