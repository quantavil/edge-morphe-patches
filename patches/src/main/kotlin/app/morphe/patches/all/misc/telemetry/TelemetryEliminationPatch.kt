package app.morphe.patches.all.misc.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.string
import app.morphe.patches.all.misc.EDGE_COMPATIBILITY
import app.morphe.util.matchAllMethodIndicesForEach
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import java.util.logging.Logger

private const val ONECOLLECTOR_ENDPOINT =
    "https://mobile.events.data.microsoft.com/OneCollector/1.0/"

private const val ONECOLLECTOR_ENDPOINT_NO_SLASH =
    "https://mobile.events.data.microsoft.com/OneCollector/1.0"

private const val VORTEX_ENDPOINT =
    "https://vortex.data.microsoft.com"

private const val APPCENTER_ENDPOINT =
    "https://in.appcenter.ms/logs?api-version=1.0.0"

private const val ADJUST_APP_ENDPOINT =
    "https://app.adjust.com"

private const val ADJUST_GDPR_ENDPOINT =
    "https://gdpr.adjust.com"

private const val ADJUST_SSRV_ENDPOINT =
    "https://ssrv.adjust.com"

private const val ADJUST_SUBSCRIPTION_ENDPOINT =
    "https://subscription.adjust.com"

private const val LOCALHOST_REDIRECT = "http://127.0.0.1/"
private const val LOCALHOST_REDIRECT_NO_SLASH = "http://127.0.0.1"

private const val ONEDSLOGGER_CLASS = "Lcom/microsoft/applications/events/Logger;"
private const val ADJUST_CLASS = "Lcom/adjust/sdk/Adjust;"
private const val APPCENTER_CLASS = "Lcom/microsoft/appcenter/AppCenter;"

private val logger = Logger.getLogger("TelemetryEliminationPatch")

@Suppress("unused")
val telemetryEliminationPatch = bytecodePatch(
    name = "Telemetry elimination",
    description = "Eliminates Microsoft Edge telemetry and tracking by redirecting data collection endpoints " +
            "(OneCollector, AppCenter, Adjust) to localhost and short-circuiting OneDS and Adjust tracking methods.",
    default = true,
) {
    compatibleWith(EDGE_COMPATIBILITY)

    execute {
        // ──────────────────────────────────────────────────────────────────────
        // Step 1: Replace all telemetry & analytics endpoint strings with localhost.
        // ──────────────────────────────────────────────────────────────────────

        val endpointReplacements = mapOf(
            ONECOLLECTOR_ENDPOINT to LOCALHOST_REDIRECT,
            ONECOLLECTOR_ENDPOINT_NO_SLASH to LOCALHOST_REDIRECT_NO_SLASH,
            VORTEX_ENDPOINT to LOCALHOST_REDIRECT_NO_SLASH,
            APPCENTER_ENDPOINT to LOCALHOST_REDIRECT_NO_SLASH,
            ADJUST_APP_ENDPOINT to LOCALHOST_REDIRECT_NO_SLASH,
            ADJUST_GDPR_ENDPOINT to LOCALHOST_REDIRECT_NO_SLASH,
            ADJUST_SSRV_ENDPOINT to LOCALHOST_REDIRECT_NO_SLASH,
            ADJUST_SUBSCRIPTION_ENDPOINT to LOCALHOST_REDIRECT_NO_SLASH,
        )

        var stringReplacementCount = 0

        endpointReplacements.forEach { (endpoint, replacement) ->
            string(endpoint).matchAllMethodIndicesForEach(requireMatches = false) { index ->
                replaceInstruction(
                    index,
                    BuilderInstruction21c(
                        Opcode.CONST_STRING,
                        getInstruction<OneRegisterInstruction>(index).registerA,
                        ImmutableStringReference(replacement),
                    )
                )
                stringReplacementCount++
            }
        }

        if (stringReplacementCount == 0) {
            throw PatchException("No telemetry endpoint strings found — endpoints may have changed")
        }

        logger.info("Replaced $stringReplacementCount telemetry and analytics endpoint string(s)")

        // ──────────────────────────────────────────────────────────────────────
        // Step 2: Short-circuit event logging and tracking classes.
        // ──────────────────────────────────────────────────────────────────────

        var oneDsShortCircuitCount = 0
        var adjustShortCircuitCount = 0
        var appCenterShortCircuitCount = 0

        mutableClassDefByOrNull(ONEDSLOGGER_CLASS)?.let { loggerClass ->
            loggerClass.methods.forEach { method ->
                if (method.name.startsWith("log") &&
                    method.returnType == "V" &&
                    method.implementation != null
                ) {
                    method.returnEarly()
                    oneDsShortCircuitCount++
                }
            }
        }

        mutableClassDefByOrNull(ADJUST_CLASS)?.let { adjustClass ->
            adjustClass.methods.forEach { method ->
                // Skip constructors (<init>) and static initializers (<clinit>) to satisfy ART verifier
                if (!method.name.startsWith("<") && method.implementation != null) {
                    if (method.returnType == "V") {
                        method.returnEarly()
                        adjustShortCircuitCount++
                    } else if (method.returnType == "Z" && method.parameters.isEmpty()) {
                        method.returnEarly(false)
                        adjustShortCircuitCount++
                    }
                }
            }
        }

        mutableClassDefByOrNull(APPCENTER_CLASS)?.let { appCenterClass ->
            appCenterClass.methods.forEach { method ->
                if (!method.name.startsWith("<") && method.implementation != null) {
                    if (method.returnType == "V") {
                        method.returnEarly()
                        appCenterShortCircuitCount++
                    } else if (method.returnType == "Z" && method.parameters.isEmpty()) {
                        method.returnEarly(false)
                        appCenterShortCircuitCount++
                    }
                }
            }
        }

        if (oneDsShortCircuitCount == 0) {
            throw PatchException("No OneDS Logger methods found — Logger class may have changed")
        }

        logger.info("Short-circuited $oneDsShortCircuitCount OneDS Logger method(s)")
        logger.info("Short-circuited $adjustShortCircuitCount Adjust tracker method(s)")
        if (appCenterShortCircuitCount > 0) {
            logger.info("Short-circuited $appCenterShortCircuitCount AppCenter method(s)")
        }
    }
}
