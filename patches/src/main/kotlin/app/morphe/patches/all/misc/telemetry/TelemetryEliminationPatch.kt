package app.morphe.patches.all.misc.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.findMutableMethodOf
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
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
) {
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

        classDefForEach { classDef ->
            classDef.methods.forEach { method ->
                val implementation = method.implementation ?: return@forEach

                val matchingIndices = mutableListOf<Pair<Int, String>>()

                implementation.instructions.forEachIndexed { index, instruction ->
                    val ref = (instruction as? ReferenceInstruction)
                        ?.reference as? StringReference ?: return@forEachIndexed

                    endpointReplacements.keys.forEach { endpoint ->
                        if (ref.string == endpoint) {
                            matchingIndices.add(index to endpoint)
                        }
                    }
                }

                if (matchingIndices.isNotEmpty()) {
                    val mutableClass = mutableClassDefBy(classDef)
                    val mutableMethod = mutableClass.findMutableMethodOf(method)

                    // Process indices in reverse order to avoid index shifts.
                    matchingIndices.asReversed().forEach { (index, endpoint) ->
                        val register = mutableMethod.getInstruction<OneRegisterInstruction>(index).registerA
                        val replacement = endpointReplacements[endpoint]!!

                        mutableMethod.replaceInstruction(
                            index,
                            BuilderInstruction21c(
                                Opcode.CONST_STRING,
                                register,
                                ImmutableStringReference(replacement),
                            )
                        )

                        stringReplacementCount++
                    }
                }
            }
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

        logger.info("Short-circuited $shortCircuitCount Logger method(s)")
    }
}
