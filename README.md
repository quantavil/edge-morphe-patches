<div align="center">
  <img src="logo.svg" width="128" height="128" alt="Edge Patches Logo" />
  <h1>Edge Morphe Patches</h1>
  <p>ReVanced-style patch set for Microsoft Edge and Edge Canary on Android.</p>

  <p>
    <a href="https://github.com/quantavil/edge-morphe-patches/releases"><img src="https://img.shields.io/github/v/release/quantavil/edge-morphe-patches?style=for-the-badge" alt="GitHub Release" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/quantavil/edge-morphe-patches?style=for-the-badge" alt="License" /></a>
    <a href="documentation/README.md"><img src="https://img.shields.io/badge/Documentation-local-blue?style=for-the-badge" alt="Developer Docs" /></a>
  </p>
</div>

> [!TIP]
> **Downloads**: Download pre-compiled `.mpp` patch bundles or pre-patched, signed APKs directly from [GitHub Releases](https://github.com/quantavil/edge-morphe-patches/releases):
> * `edge-patched-<version>-arm64.apk` — Patched Microsoft Edge (Stable)
> * `edge-canary-patched-<version>-arm64.apk` — Patched Microsoft Edge Canary

---

## 🩹 Patches<!-- PATCHES_START -->
> **[v1.36.1](https://github.com/quantavil/edge-morphe-patches/releases/tag/v1.36.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;10 patches total
<details open>
<summary>📦 Microsoft Edge&nbsp;&nbsp;•&nbsp;&nbsp;5 patches</summary>
<br>

**🎯 Supported versions:**

| 152.0.4191.65 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| [Copilot feature toggle](#copilot-feature-toggle) | Disables all Copilot and Bing Chat feature flags by forcing boolean feature evaluation methods to return false. |
| [Disable news notifications](#disable-news-notifications) | Disables Bing news and weather push notifications by short-circuiting device push token registration and notification dispatch services. |
| [Telemetry elimination](#telemetry-elimination) | Eliminates Microsoft Edge telemetry and tracking by redirecting data collection endpoints (OneCollector, AppCenter, Adjust) to localhost and short-circuiting OneDS and Adjust tracking methods. |
| [Change package name](#change-package-name) *(optional)* | Changes the application package name in AndroidManifest.xml (e.g. for media display whitelist compatibility or cloning). |
| [Disable Play Store updates](#disable-play-store-updates) *(optional)* | Disables Play Store updates by setting the version code to the maximum allowed. This patch does not work if the app is installed by mounting and may cause unexpected issues with some apps. |

</details>

<details open>
<summary>📦 Microsoft Edge Canary&nbsp;&nbsp;•&nbsp;&nbsp;5 patches</summary>
<br>

**🎯 Supported versions:**

| 155.0.4269.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| [Copilot feature toggle](#copilot-feature-toggle) | Disables all Copilot and Bing Chat feature flags by forcing boolean feature evaluation methods to return false. |
| [Disable news notifications](#disable-news-notifications) | Disables Bing news and weather push notifications by short-circuiting device push token registration and notification dispatch services. |
| [Telemetry elimination](#telemetry-elimination) | Eliminates Microsoft Edge telemetry and tracking by redirecting data collection endpoints (OneCollector, AppCenter, Adjust) to localhost and short-circuiting OneDS and Adjust tracking methods. |
| [Change package name](#change-package-name) *(optional)* | Changes the application package name in AndroidManifest.xml (e.g. for media display whitelist compatibility or cloning). |
| [Disable Play Store updates](#disable-play-store-updates) *(optional)* | Disables Play Store updates by setting the version code to the maximum allowed. This patch does not work if the app is installed by mounting and may cause unexpected issues with some apps. |

</details>

<!-- PATCHES_END -->

## 🚀 One-Click Build & Deploy

Automate patch compilation, cache clearing, base APK patching, signing, and device installation:

```bash
./run_pipeline.sh          # Build, patch, sign, install & launch
./run_pipeline.sh --force  # Bypass version compatibility checks (-f)
```

### 🔄 Updating Base APK & Compatibility

When updating `edge_base.apk` or targeting a newer release:
1. **Target Versions**: Update target version strings in [`EdgeCompatibility.kt`](patches/src/main/kotlin/app/morphe/patches/all/misc/EdgeCompatibility.kt).
2. **Clean Build**: Run `./gradlew clean buildAndroid generatePatchesList` to recompile and update metadata.
3. **Readme Sync**: Run `python3 .github/scripts/generate_patches_readme.py quantavil/edge-morphe-patches main` to synchronize the table above.

## 🤝 Contributing

Contributions and patch suggestions are welcome! Submit an issue or open a pull request.
For creating new patches, see the [Morphe Patches Template](https://github.com/morpheapp/morphe-patches-template).

## 📜 License

Licensed under the [GNU General Public License v3.0](LICENSE), with Section 7 terms:
- **Attribution (7b):** Retain all copyright and author notices.
- **Name Restriction (7c):** Derivative works must not use the name **"Morphe"**.

See [LICENSE](LICENSE) and [NOTICE](NOTICE) for full terms.
