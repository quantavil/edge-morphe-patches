<div align="center">
  <img src="logo.svg" width="128" height="128" alt="Edge Patches Logo" />
  <h1>Edge Morphe Patches</h1>
  <p>Unofficial ReVanced-style patches for Microsoft Edge on Android.</p>

  <p>
    <a href="https://github.com/quantavil/edge-morphe-patches/releases"><img src="https://img.shields.io/github/v/release/quantavil/edge-morphe-patches?style=for-the-badge" alt="GitHub Release" /></a>
    <a href="LICENSE"><img src="https://img.shields.io/github/license/quantavil/edge-morphe-patches?style=for-the-badge" alt="License" /></a>
    <a href="documentation/README.md"><img src="https://img.shields.io/badge/Documentation-local-blue?style=for-the-badge" alt="Developer Docs" /></a>
  </p>
</div>

&nbsp;

> 📥 **Downloads**: You can download the compiled patches (`.mpp` file) or the pre-compiled, fully patched APK file directly from the [GitHub Releases](https://github.com/quantavil/edge-morphe-patches/releases) page.

---

&nbsp;
## 🩹 Patches<!-- PATCHES_START -->
> **[v1.32.0](https://github.com/quantavil/edge-morphe-patches/releases/tag/v1.32.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 Microsoft Edge&nbsp;&nbsp;•&nbsp;&nbsp;3 patches</summary>
<br>

**🎯 Supported versions:**

| 148.0.3967.97 | 149.0.4022.53 |
| :---: | :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description |
|----------|----------------|
| [Copilot feature toggle](#copilot-feature-toggle) | Disables all Copilot and Bing Chat feature flags by forcing boolean feature evaluation methods to return false. |
| [Disable Play Store updates](#disable-play-store-updates) | Disables Play Store updates by setting the version code to the maximum allowed. This patch does not work if the app is installed by mounting and may cause unexpected issues with some apps. |
| [Telemetry elimination](#telemetry-elimination) | Eliminates Microsoft Edge telemetry by redirecting data collection endpoints to localhost and short-circuiting OneDS Logger event methods. |

</details>

<!-- PATCHES_END -->

## 🚀 One-Click Build & Deploy

This repository contains a helper script to build patches, clean temporary cache, patch the base APK, sign it, install it, and launch it on your connected device in one step:

```bash
./run_pipeline.sh
```

### 🔄 Updating the Base APK & Compatibility

If you replace `edge_base.apk` in the root directory with a newer version of Edge:
1. **Compatibility Verification**: The patcher automatically checks the version of the new APK against the compatibility definitions in the patches.
2. **Troubleshooting Version Mismatch**:
   * If the version of the new base APK is not declared in the patch files, `morphe-cli` will fail.
   * You can force the application of patches without compatibility checks by running the script with the `--force` (or `-f`) flag:
     ```bash
     ./run_pipeline.sh --force
     ```
   * To add permanent compatibility, edit the target versions list in the patches source code (e.g. `Constants.kt` or the patch files) to include the new package version.

## Contributing

Contributions are welcome! If you'd like to improve existing patches or propose new ones for Microsoft Edge, feel free to open a pull request.
See the [Patches template](https://github.com/morpheapp/morphe-patches-template) for guidance on creating new patches.


## 📜 License

Morphe Patches are licensed under the [GNU General Public License v3.0](LICENSE), with additional conditions under GPLv3 Section 7:

- **Attribution (7b):** Any use of this code, including derivatives, must preserve all notices.
  
- **Name Restriction (7c):** The name **"Morphe"** may not be used for derivative works.  
  Derivatives must adopt a distinct identity unrelated to "Morphe".

See the [LICENSE](LICENSE) file for the full GPLv3 terms and the [NOTICE](NOTICE) file for full conditions of GPLv3 Section 7
