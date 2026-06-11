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

private const val LOCALHOST_REDIRECT = "http://127.0.0.1/"
private const val LOCALHOST_REDIRECT_NO_SLASH = "http://127.0.0.1"

private const val ONEDSLOGGER_CLASS = "Lcom/microsoft/applications/events/Logger;"

private val logger = Logger.getLogger("TelemetryEliminationPatch")

@Suppress("unused")
val telemetryEliminationPatch = bytecodePatch(
    name = "Telemetry elimination",
    description = "Eliminates Microsoft Edge telemetry by redirecting data collection endpoints " +
            "to localhost and short-circuiting OneDS Logger event methods.",
    default = true,
) {
    compatibleWith(EDGE_COMPATIBILITY)

    execute {
        // ──────────────────────────────────────────────────────────────────────
        // Step 1: Replace all telemetry endpoint strings with localhost.
        // ──────────────────────────────────────────────────────────────────────

        val endpointReplacements = mapOf(
            ONECOLLECTOR_ENDPOINT to LOCALHOST_REDIRECT,
            ONECOLLECTOR_ENDPOINT_NO_SLASH to LOCALHOST_REDIRECT_NO_SLASH,
            VORTEX_ENDPOINT to LOCALHOST_REDIRECT_NO_SLASH,
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

        logger.info("Replaced $stringReplacementCount telemetry endpoint string(s)")

        // ──────────────────────────────────────────────────────────────────────
        // Step 2: Short-circuit all event logging methods in the OneDS Logger.
        // ──────────────────────────────────────────────────────────────────────

        val loggerClass = mutableClassDefBy(ONEDSLOGGER_CLASS)
        var shortCircuitCount = 0

        loggerClass.methods.forEach { method ->
            // Target methods that start with "log" and return void.
            if (method.name.startsWith("log") && method.returnType == "V") {
                method.returnEarly()
                shortCircuitCount++
            }
        }

        if (shortCircuitCount == 0) {
            throw PatchException("No OneDS Logger methods found — Logger class may have changed")
        }

        logger.info("Short-circuited $shortCircuitCount Logger method(s)")
    }
}
