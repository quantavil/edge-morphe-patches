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
- Every patch must call `compatibleWith(EDGE_COMPATIBILITY, EDGE_CANARY_COMPATIBILITY)` to declare package compatibility.

## Dependencies & Setup
- JDK 17+ for Gradle; JDK 21+ for the Morphe CLI (`morphe-cli.jar`).
- Toolchain: patches plugin `2.1.0`, Morphe Patcher `2.3.0`, patches library `2.2.0-dev.6`.
- Local composite builds (no GitHub Packages token needed):
  - `../morphe-patches-gradle-plugin` (tag `v2.1.0`, optionally auto-included by `settings.gradle.kts`)
  - `../morphe-patcher` (tag `v2.3.0`)
  - `../morphe-patches-library` (tag `v2.2.0-dev.6`)
- `ANDROID_HOME=~/Android/Sdk` is required when building the composite library (its signature extension uses AGP).
- CLI jar: `morphe-cli.jar` is the Morphe Desktop `v2.1.1` all-jar (embeds Patcher 2.3.0). It is gitignored; download it from the `morphe-cli` releases.

## Critical Information
- Derivative patch sets must not use the name "Morphe" (GPLv3 Section 7c).
- 2-File System Convention:
  1. Unpatched Base APK: `edge_base.apk`/`edge_base.apkm` (Edge 157.0.4341.53) or `edge_canary_base.apk` (Canary 159.0.4399.0), both gitignored in the root.
  2. Patched Output APK (Local & GitHub identical): `edge-patched-<edge_version>-arm64.apk` or `edge-canary-patched-<edge_version>-arm64.apk`. No redundant intermediate APK names.

## Insights
- Telemetry: OneDS Logger (`Lcom/microsoft/applications/events/Logger;`) and Adjust SDK (`Lcom/adjust/sdk/Adjust;`) methods short-circuited; endpoints (OneCollector, Vortex, AppCenter, Adjust) redirected to `127.0.0.1`. Canary 159 ships without OneDS `Logger` and OneCollector strings, so each component is optional and the patch only fails if nothing at all was patched.
- News Notifications: Push token registration and message dispatch short-circuited at `BingNotificationService` and `BingNotificationReceiver`.
- Promos: Default browser startup prompt and hub sign-in upsells disabled via feature flag strings.
- NTP Feed: Background stream update and prefetch short-circuited at `FeedServiceBridge` and `FeedSurfaceRendererBridge`.
- Copilot: Feature flag class located via `"msEdgeMobileCopilotMode"` string; all `()Z` methods return `false`. The fingerprint must constrain `returnType = "Z"` and `parameters = emptyList()` because the same string also sits in unrelated `<clinit>` methods (`Ltie;` on 157, `Lzla;` on Canary 159).
- Toolchain 2.x: the `app.morphe.patches` settings plugin (2.1.0) adds `morphe-patcher` and `smali` from the version catalog; `bytecodePatch()`/`resourcePatch()` still compile but are deprecated in Patcher 2.3.0 (new code should use `patch()`).
- Deployment: `run_pipeline.sh` compiles patches, purges `morphe-data/tmp`, patches APK, signs, and deploys via ADB.
- Upgrading APK: Update `EdgeCompatibility.kt` (replace the old `AppTarget`, do not keep older versions), run `./gradlew clean buildAndroid generatePatchesList`, and run `python3 .github/scripts/generate_patches_readme.py quantavil/edge-morphe-patches main`. `patches-list.json` no longer carries a 'v' prefix; do not re-add one.
- CLI install/patch: `./run_pipeline.sh` (stable) or `./run_pipeline.sh --canary`; the script auto-selects a Java 21+ binary and supports `.apk`/`.apkm` bases.
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
- [2026-09-14] ChangePackageNamePatch hardcoded `oldPackage = "com.microsoft.emmx"` → Broke Edge Canary `com.microsoft.emmx.canary` renaming → Read package dynamically from AndroidManifest.xml.
- [2026-09-14] `run_pipeline.sh` unhandled multi-target grep → Grepped multiple versions into `OUTPUT_APK` newline string → Filtered with `head -n 1`.
- [2026-09-14] Edge Canary `GcStateAssertQueue` crash on short-circuiting JNI callback → `returnEarly()` on `requestDeviceToken` left `JniOnceCallback` unexecuted, triggering `LifetimeAssertException` on GC in Canary → Invoke callback with empty string before returning.
- [2026-11-14] GitHub Packages returns 401 for the Morphe registry with a `gh auth token` (no `read:packages` scope) → Container builds fail on plugin/library resolution → Use the local composite builds (`../morphe-patches-gradle-plugin`, `../morphe-patcher`, `../morphe-patches-library`); CI's `GITHUB_TOKEN` still resolves the published artifacts.
- [2026-11-14] Patcher 2.x CLI requires Java 21+ but the system default is Java 17 → `UnsupportedClassVersionError` → `run_pipeline.sh` auto-detects `/usr/lib/jvm/java-27-openjdk/bin/java`; pass `JAVA_BIN` to override.
- [2026-11-14] CLI 2.1.0 threw `FileNotFoundException` in `ApkBundle.loadApkDirectory` for relative input paths on split bundles → Fixed upstream in 2.1.1 → `run_pipeline.sh` resolves the base APK, MPP, and output to absolute paths with `realpath`.
- [2026-11-14] Copilot fingerprint matched unrelated `<clinit>` methods containing `msEdgeMobileCopilotMode` (`Ltie;` on 157, `Lzla;` on 159) → Patch aborted with "No Copilot feature flag methods found" → Restrict the fingerprint to `()Z` methods.
- [2026-11-14] `ChangePackageNamePatch` rewrote the manifest `package` attribute twice because `getElementsByTagName("*")` includes the root element → `com.microsoft.emmx.morphe.morphe` → Skip the root `package` attribute in the replacement loop.

