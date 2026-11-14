package app.morphe.patches.all.misc.copilot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.all.misc.EDGE_CANARY_COMPATIBILITY
import app.morphe.patches.all.misc.EDGE_COMPATIBILITY
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import java.util.logging.Logger

private const val COPILOT_MODE_STRING = "msEdgeMobileCopilotMode"

private const val EDGE_ACCOUNT_MANAGER_CLASS =
    "Lorg/chromium/chrome/browser/edge_signin/account/EdgeAccountManager;"

/**
 * Whether this method only returns the Edge account sign-in state.
 *
 * The Copilot feature class also hosts generic account helpers such as
 * `isSignedIn()` and its negation. Those must not be forced to false, otherwise
 * signed-in and signed-out checks both report false and the account UI breaks.
 */
private fun Method.isAccountStateCheck(): Boolean {
    val instructions = implementation?.instructions?.toList() ?: return false
    if (instructions.any { it.opcode == Opcode.CONST_STRING || it.opcode == Opcode.CONST_STRING_JUMBO }) {
        return false
    }

    val references = instructions
        .filterIsInstance<ReferenceInstruction>()
        .mapNotNull { it.reference as? MethodReference }

    return references.size == 2 &&
            references.any {
                it.definingClass == EDGE_ACCOUNT_MANAGER_CLASS &&
                        it.name == "l" &&
                        it.returnType == "Z" &&
                        it.parameterTypes.isEmpty()
            } &&
            references.any {
                it.definingClass == EDGE_ACCOUNT_MANAGER_CLASS && it.name == "b"
            }
}

/**
 * Whether this method only negates one of the [accountStateMethods].
 *
 * These are helpers like `isSignedOut()` which must keep their real value.
 */
private fun Method.isAccountStateNegation(
    accountStateMethods: Set<String>,
    classType: String,
): Boolean {
    val instructions = implementation?.instructions?.toList() ?: return false
    if (instructions.any { it.opcode == Opcode.CONST_STRING || it.opcode == Opcode.CONST_STRING_JUMBO }) {
        return false
    }

    val references = instructions
        .filterIsInstance<ReferenceInstruction>()
        .mapNotNull { it.reference as? MethodReference }

    return references.size == 1 &&
            references.single().definingClass == classType &&
            references.single().name in accountStateMethods
}

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

        // The same class hosts account helpers that merely read the Edge sign-in
        // state (and their negations). Those are not feature flags and must keep
        // reporting the real value.
        val accountStateMethods = copilotClass.methods
            .filter { it.returnType == "Z" && it.parameters.isEmpty() && it.isAccountStateCheck() }
            .map { it.name }
            .toSet()

        if (accountStateMethods.isNotEmpty()) {
            logger.info(
                "  Preserving account state method(s): ${accountStateMethods.joinToString()}()",
            )
        }

        var patchedMethodCount = 0

        copilotClass.methods.forEach { method ->
            // Only target methods that return boolean (Z) and take no parameters
            // (excluding the implicit 'this' parameter for instance methods).
            // These are the feature flag evaluation methods.
            if (method.returnType != "Z" || method.parameters.isNotEmpty()) return@forEach

            if (method.name in accountStateMethods ||
                method.isAccountStateNegation(accountStateMethods, copilotClass.type)
            ) {
                logger.info("  Kept account state helper: ${method.name}()")
                return@forEach
            }

            method.returnEarly(false)
            patchedMethodCount++
            logger.info("  Disabled feature flag: ${method.name}()")
        }

        if (patchedMethodCount == 0) {
            throw PatchException("No Copilot feature flag methods found — target class may have changed")
        }

        logger.info("Disabled $patchedMethodCount Copilot/Bing Chat feature flag method(s)")
    }
}

