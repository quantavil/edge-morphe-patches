package app.morphe.patches.all.misc

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

/**
 * Shared compatibility declaration for Microsoft Edge.
 * Add new [AppTarget] entries here when upgrading `edge_base.apk`.
 */
val EDGE_COMPATIBILITY = Compatibility(
    packageName = "com.microsoft.emmx",
    name = "Microsoft Edge",
    targets = listOf(
        AppTarget(version = "148.0.3967.97"),
        AppTarget(version = "149.0.4022.53"),
    ),
)
