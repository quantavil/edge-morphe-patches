package app.morphe.patches.all.misc.copilot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.all.misc.EDGE_CANARY_COMPATIBILITY
import app.morphe.patches.all.misc.EDGE_COMPATIBILITY
import app.morphe.patcher.util.returnEarly
import java.util.logging.Logger

private const val COPILOT_MODE_STRING = "msEdgeMobileCopilotMode"

/**
 * Fingerprint to locate the Copilot feature flag manager class.
 * Matches the no-argument boolean method that contains the "msEdgeMobileCopilotMode" string,
 * which identifies the class responsible for evaluating Copilot/Bing Chat features.
 *
 * The return type and parameters are required: the same string also appears in unrelated
 * classes' static initializers (e.g. `Ltie;` on Edge 157, `Lzla;` on Canary 159), which
 * would otherwise be matched first and cause the patch to fail.
 */
private object CopilotFeatureFlagFingerprint : Fingerprint(
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(string(COPILOT_MODE_STRING)),
)

private val logger = Logger.getLogger("CopilotFeatureTogglePatch")

@Suppress("unused")
val copilotFeatureTogglePatch = bytecodePatch(
    name = "Copilot feature toggle",
    description = "Disables all Copilot and Bing Chat feature flags by forcing " +
            "boolean feature evaluation methods to return false.",
    default = true,
) {
    compatibleWith(EDGE_COMPATIBILITY, EDGE_CANARY_COMPATIBILITY)

    execute {
        // Resolve the fingerprint to find the target class containing Copilot feature flags.
        val copilotClass = CopilotFeatureFlagFingerprint.classDef

        logger.info("Found Copilot feature flag class: ${copilotClass.type}")

        var patchedMethodCount = 0

        copilotClass.methods.forEach { method ->
            // Only target methods that return boolean (Z) and take no parameters
            // (excluding the implicit 'this' parameter for instance methods).
            // These are the feature flag evaluation methods.
            if (method.returnType == "Z" && method.parameters.isEmpty()) {
                method.returnEarly(false)
                patchedMethodCount++
                logger.info("  Disabled feature flag: ${method.name}()")
            }
        }

        if (patchedMethodCount == 0) {
            throw PatchException("No Copilot feature flag methods found — target class may have changed")
        }

        logger.info("Disabled $patchedMethodCount Copilot/Bing Chat feature flag method(s)")
    }
}

