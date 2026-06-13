# Project: Edge Morphe Patches

## Overview
Edge Morphe Patches is a repository containing Android application patches (Disable Play Store updates, Telemetry elimination, and Copilot feature toggle) based on ReVanced patcher architecture. It uses Gradle and Kotlin as its core tech stack.

## Structure
.
├── .github/
│   └── workflows/
│       └── release.yml           # GitHub Actions workflow for automatic patch compiling and release publishing
├── MEMORY.md                     # Project memory
├── .releaserc                    # Configuration file for semantic-release automation
├── logo.svg                      # Microsoft Edge brand logo in SVG format
├── build.gradle.kts              # Gradle build configuration
├── settings.gradle.kts           # Gradle settings configuration
├── patches-list.json             # Lists details of available universal patches
├── patches-bundle.json           # Metadata bundle for the patches
├── sync_version.py               # Version synchronization script
├── run_pipeline.sh               # One-click build, patch, sign, install, and launch pipeline script
├── documentation/                # Cloned Morphe patcher developer documentation
├── extensions/                   # Proguard rules for extensions
│   └── proguard-rules.pro
└── patches/                      # Patches implementation (source code)
    └── src/main/kotlin/app/morphe/patches/all/misc/
        ├── EdgeCompatibility.kt  # Shared Compatibility constant for com.microsoft.emmx
        ├── copilot/              # Copilot/Bing Chat feature toggle patch
        ├── telemetry/            # Telemetry elimination patch
        └── updates/              # Disable Play Store updates patch

## Conventions
- Follow standard Kotlin development style.
- Patches are structured using ReVanced patch definitions.
- Bytecode patches use `bytecodePatch()` DSL; resource patches use `resourcePatch()`.
- Fingerprints use `Fingerprint(strings = listOf(...))` for string-based matching.
- Use `returnEarly()` / `returnEarly(false)` from `app.morphe.util` to short-circuit methods.
- Use `mutableClassDefBy(classDef)` to get mutable class from an immutable `ClassDef` (e.g. in `classDefForEach`).
- Fingerprint `.classDef` and `.method` properties return mutable instances directly, making `mutableClassDefBy` redundant.
- Use `classDefForEach {}` to iterate all classes in the APK.
- Every patch must call `compatibleWith(EDGE_COMPATIBILITY)` to declare package compatibility, otherwise Morphe Manager shows "any package, any version".

## Dependencies & Setup
- Java Development Kit (JDK) 17+ or compatible version.
- Gradle (provided wrapper `gradlew`).
- Local composite build: `../morphe-patches-library` must exist (cloned from GitHub, tag v1.4.0-dev.5).
- Kotlin compiler flag: `-Xcontext-parameters` enabled in `patches/build.gradle.kts`.

## Critical Information
- Any derivative patch set must not use the name "Morphe" as per the Section 7 terms of GPLv3 license.
- Base APK: `edge_base.apk` in project root (Edge 149.0.4022.53, com.microsoft.emmx, arm64-v8a).

## Insights
- Telemetry: OneDS Logger at `Lcom/microsoft/applications/events/Logger;` — short-circuit all `log*()V` methods.
- Telemetry: Endpoints `mobile.events.data.microsoft.com/OneCollector/1.0/` and `vortex.data.microsoft.com`.
- Copilot: Feature flag class found via `"msEdgeMobileCopilotMode"` string. All `()Z` methods are feature flags.
- One-Click Deployment: `run_pipeline.sh` builds patches, purges the `morphe-data/tmp` cache, runs patcher, signs the APK, and deploys it to the ADB connected device.
- Changing Base APK / Future Verification: When upgrading `edge_base.apk`, if version compatibility check fails, use `./run_pipeline.sh -f` to bypass, or add the new version to the `Compatibility` configuration in the patch classes source code.
- Branching: The dev branch has been removed; only the main branch is used, and pre-releases are not supported.
- Fingerprints: Prefer `filters = listOf(string(...))` over `strings = listOf(...)` per Morphe docs convention.
- Fingerprints: Use `matchAllMethodIndicesForEach` from `app.morphe.util` for replacing string literals/constants globally — handles fingerprint creation, matching, and reverse-index iteration in one call.
- README: `sync_version.py` only updates patch version, NOT the Edge app version table. Run `python3 .github/scripts/generate_patches_readme.py quantavil/edge-morphe-patches main` after `generatePatchesList` to update README supported versions.
- Patches: Crash Reporting and First-Run Experience (FRE) patches were excluded because their targets/classes (`Lerh`, `Lkrh`) and internal methods are obfuscated, which violates the no-obfuscation project rule.
- Extensions: Sideloading is impossible on the stable APK because the handler code for com.microsoft.edge.extensions.ACTION_INSTALL_EXTENSION_FOR_DEV_MODE that processes EXTENSION_CRX was completely compiled out/stripped by Proguard/R8 optimization in the production release.

## Blunders
- [2026-06-05] morphe-cli patch failed due to modified base APK → `morphe-data/tmp/` was dirty from prior runs → Purged `morphe-data/tmp/*` before patching.
- [2026-06-05] Patches showed "any package, any version" in Morphe Manager → None of the patches called `compatibleWith()` → Created `EdgeCompatibility.kt` and added `compatibleWith(EDGE_COMPATIBILITY)` to all patch classes.
- [2026-06-05] Adding repo source to Morphe Manager failed with source URL error → Missing `dev` branch on remote repository when pre-release was toggled, and prefix 'v' mismatch on `patches-list.json` → Pushed `dev` branch to remote and removed 'v' from version in `patches-list.json`.
- [2026-06-05] Morphe Manager displayed 'metadata N/A' for remote source → Suffix 'Z' on `created_at` timestamp in `patches-bundle.json` broke Android parser → Removed 'Z' suffix to match the local date-time format.
- [2026-06-05] GPLv3 Section 7c violation in `patches/build.gradle.kts` → About block used upstream Morphe identity → Updated about block to use developer's own values.
- [2026-06-11] `generatePatchesList` re-adds 'v' prefix to version in `patches-list.json` → Must manually strip it after each regeneration.
- [2026-06-11] Gradle incremental build didn't pick up `EdgeCompatibility.kt` change → Cached `.mpp` still had old version targets → Must `./gradlew clean buildAndroid` when changing compatibility.
- [2026-06-11] Manually pushing the release tag prior to CI run causes semantic-release to fail/skip because the tag already exists → Never tag releases manually when using semantic-release.

