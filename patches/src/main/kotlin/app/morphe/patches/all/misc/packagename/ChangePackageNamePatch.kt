package app.morphe.patches.all.misc.packagename

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.all.misc.EDGE_CANARY_COMPATIBILITY
import app.morphe.patches.all.misc.EDGE_COMPATIBILITY
import app.morphe.util.getNode
import org.w3c.dom.Element
import java.util.logging.Logger

private val logger = Logger.getLogger("ChangePackageNamePatch")

@Suppress("unused")
val changePackageNamePatch = resourcePatch(
    name = "Change package name",
    description = "Changes the application package name in AndroidManifest.xml (e.g. for media display whitelist compatibility or cloning).",
    default = false,
) {
    compatibleWith(EDGE_COMPATIBILITY, EDGE_CANARY_COMPATIBILITY)

    val targetPackageName by stringOption(
        key = "packageName",
        default = null,
        title = "Package name",
        description = "New package name for Microsoft Edge (defaults to <originalPackage>.morphe if not specified)",
    )

    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.getNode("manifest") as Element
            val oldPackage = manifest.getAttribute("package")
            val newPackage = targetPackageName ?: "$oldPackage.morphe"

            logger.info("Changing package name from '$oldPackage' to '$newPackage'...")

            manifest.setAttribute("package", newPackage)

            val allElements = document.getElementsByTagName("*")
            for (i in 0 until allElements.length) {
                val element = allElements.item(i) as Element
                val attributes = element.attributes
                for (j in 0 until attributes.length) {
                    val attr = attributes.item(j)
                    if (attr.nodeValue.contains(oldPackage)) {
                        val newValue = attr.nodeValue.replace(oldPackage, newPackage)
                        attr.nodeValue = newValue
                    }
                }
            }

            logger.info("Successfully rewritten AndroidManifest.xml package references to '$newPackage'")
        }
    }
}
