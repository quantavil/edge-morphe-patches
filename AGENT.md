# Project: Edge Morphe Patches

## Overview
Repository containing Android application patches (Disable Play Store updates, Telemetry elimination, Copilot feature toggle, Disable news notifications, Disable promotional upsells, Disable NTP news feed) based on ReVanced patcher architecture. Tech stack: Gradle, Kotlin.

## Structure
.
├── .github/
│   └── workflows/release.yml     # CI workflow for patch compilation and semantic-release
├── .releaserc                    # Configuration for semantic-release automation
├── AGENT.md                      # Project agent instructions and memory
├── logo.svg                      # Microsoft Edge brand logo
├── build.gradle.kts              # Root Gradle build configuration
├── settings.gradle.kts           # Gradle settings (composite build integration)
├── patches-list.json             # Metadata listing compatible patches and targets
├── patches-bundle.json           # Release bundle metadata for Morphe Manager
├── sync_version.py               # Version synchronization script
├── run_pipeline.sh               # One-click build, patch, sign, install, and launch script
├── documentation/                # Cloned Morphe patcher developer documentation
└── patches/                      # Patches source code
    └── src/main/kotlin/app/morphe/patches/all/misc/
        ├── EdgeCompatibility.kt  # Shared Compatibility constant for com.microsoft.emmx
        ├── copilot/              # Copilot/Bing Chat feature toggle patch
        ├── notifications/        # Disable news & weather push notifications patch
        ├── packagename/          # Change package name resource patch (configurable string option)
        ├── telemetry/            # Telemetry & tracking elimination patch (OneDS, Adjust, AppCenter)
        └── updates/              # Disable Play Store updates patch

## Conventions
- Follow standard Kotlin development style with ReVanced/Morphe patch DSL (`bytecodePatch()`, `resourcePatch()`).
- Use `returnEarly()` / `returnEarly(false)` from `app.morphe.util` to short-circuit methods.
- Use `mutableClassDefByOrNull(classDescriptor)` directly in `execute {}` to patch non-obfuscated SDK classes.
- Fingerprints use `Fingerprint(filters = listOf(string(...)))` for string-based matching.
- Fingerprint `.classDef` and `.method` properties return mutable instances directly.
- Every patch must call `compatibleWith(EDGE_COMPATIBILITY)` to declare package compatibility.

## Dependencies & Setup
- JDK 17+ and Gradle (wrapper `gradlew`).
- Local composite build: `../morphe-patches-library` must exist (cloned tag `v1.4.0-dev.5`).
- Kotlin compiler flag: `-Xcontext-parameters` enabled in `patches/build.gradle.kts`.

## Critical Information
- Derivative patch sets must not use the name "Morphe" (GPLv3 Section 7c).
- 2-File System Convention:
  1. Unpatched Base APK: `edge_base.apk` (Source base APK in root).
  2. Patched Output APK (Local & GitHub identical): `edge-patched-<edge_version>-arm64.apk`. No redundant intermediate APK names.

## Insights
- Telemetry: OneDS Logger (`Lcom/microsoft/applications/events/Logger;`) and Adjust SDK (`Lcom/adjust/sdk/Adjust;`) methods short-circuited; endpoints (OneCollector, Vortex, AppCenter, Adjust) redirected to `127.0.0.1`.
- News Notifications: Push token registration and message dispatch short-circuited at `BingNotificationService` and `BingNotificationReceiver`.
- Promos: Default browser startup prompt and hub sign-in upsells disabled via feature flag strings.
- NTP Feed: Background stream update and prefetch short-circuited at `FeedServiceBridge` and `FeedSurfaceRendererBridge`.
- Copilot: Feature flag class located via `"msEdgeMobileCopilotMode"` string; all `()Z` methods return `false`.
- Deployment: `run_pipeline.sh` compiles patches, purges `morphe-data/tmp`, patches APK, signs, and deploys via ADB.
- Upgrading APK: Update `EdgeCompatibility.kt`, run `./gradlew clean buildAndroid generatePatchesList`, strip 'v' in `patches-list.json`, and run `python3 .github/scripts/generate_patches_readme.py quantavil/edge-morphe-patches main`.
- Obfuscation rule: Avoid matching obfuscated classes (`Lerh`, `Lkrh`) to prevent update fragility.

## Blunders
- [2026-06-05] `morphe-cli` patch failed due to modified base APK → `morphe-data/tmp/` dirty from prior runs → Purge `morphe-data/tmp/*` before patching.
- [2026-06-05] Patches showed "any package, any version" in Manager → None called `compatibleWith()` → Created `EdgeCompatibility.kt` with `compatibleWith(EDGE_COMPATIBILITY)` on all patches.
- [2026-06-05] Manager repo source error → Missing remote branch and 'v' prefix mismatch on `patches-list.json` → Pushed branch and stripped 'v' from version in `patches-list.json`.
- [2026-06-05] Manager showed 'metadata N/A' → Suffix 'Z' on `created_at` timestamp in `patches-bundle.json` broke parser → Removed 'Z' suffix.
- [2026-06-05] GPLv3 Section 7c violation in `patches/build.gradle.kts` → About block used upstream identity → Updated about block with developer details.
- [2026-06-11] `generatePatchesList` re-adds 'v' prefix to version in `patches-list.json` → Must manually strip 'v' after regeneration.
- [2026-06-11] Incremental build didn't pick up `EdgeCompatibility.kt` change → Cached `.mpp` retained old version → Must `./gradlew clean buildAndroid` on compatibility changes.
- [2026-06-11] Manual release tagging caused semantic-release CI failure → Tag already existed on remote → Never tag releases manually.
- [2026-08-19] Android ART VerifyError when short-circuiting Adjust methods → `returnEarly()` on `<init>()` omitted `super.<init>()` → Skip `<init>` and `<clinit>` methods when short-circuiting entire classes.
