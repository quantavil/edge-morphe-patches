package app.morphe.patches.all.misc

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

/**
 * Shared compatibility declaration for Microsoft Edge (Stable).
 * Add new [AppTarget] entries here when upgrading `edge_base.apk`.
 */
val EDGE_COMPATIBILITY = Compatibility(
    packageName = "com.microsoft.emmx",
    name = "Microsoft Edge",
    targets = listOf(
        AppTarget(version = "152.0.4191.65"),
    ),
)

/**
 * Shared compatibility declaration for Microsoft Edge Canary.
 */
val EDGE_CANARY_COMPATIBILITY = Compatibility(
    packageName = "com.microsoft.emmx.canary",
    name = "Microsoft Edge Canary",
    targets = listOf(
        AppTarget(version = "155.0.4269.0"),
    ),
)

