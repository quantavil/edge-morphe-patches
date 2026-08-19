package app.morphe.patches.all.misc.notifications

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.all.misc.EDGE_COMPATIBILITY
import app.morphe.util.returnEarly
import java.util.logging.Logger

private const val BING_NOTIFICATION_SERVICE_CLASS =
    "Lorg/chromium/chrome/browser/edge_bing_notification/BingNotificationService;"

private const val BING_NOTIFICATION_RECEIVER_CLASS =
    "Lorg/chromium/chrome/browser/edge_bing_notification/BingNotificationReceiver;"

private val logger = Logger.getLogger("DisableNewsNotificationsPatch")

@Suppress("unused")
val disableNewsNotificationsPatch = bytecodePatch(
    name = "Disable news notifications",
    description = "Disables Bing news and weather push notifications by short-circuiting " +
            "device push token registration and notification dispatch services.",
    default = true,
) {
    compatibleWith(EDGE_COMPATIBILITY)

    execute {
        var servicePatched = false
        var receiverPatched = false

        mutableClassDefByOrNull(BING_NOTIFICATION_SERVICE_CLASS)?.let { serviceClass ->
            serviceClass.methods.forEach { method ->
                if (method.implementation != null) {
                    when (method.name) {
                        "requestDeviceToken", "onMessageReceived" -> {
                            if (method.returnType == "V") {
                                method.returnEarly()
                                servicePatched = true
                                logger.info("  Short-circuited BingNotificationService.${method.name}()")
                            }
                        }
                        "isNotificationWeatherAllowedInEdge", "currentSignInAADAccountSupportNotification" -> {
                            if (method.returnType == "Z") {
                                method.returnEarly(false)
                                servicePatched = true
                                logger.info("  Disabled BingNotificationService.${method.name}()")
                            }
                        }
                    }
                }
            }
        }

        mutableClassDefByOrNull(BING_NOTIFICATION_RECEIVER_CLASS)?.let { receiverClass ->
            receiverClass.methods.forEach { method ->
                if (method.name == "onMAMReceive" && method.returnType == "V" && method.implementation != null) {
                    method.returnEarly()
                    receiverPatched = true
                    logger.info("  Short-circuited BingNotificationReceiver.onMAMReceive()")
                }
            }
        }

        if (!servicePatched) {
            throw PatchException("BingNotificationService methods not found — target class may have changed")
        }

        logger.info("Successfully disabled news and weather notifications (service: $servicePatched, receiver: $receiverPatched)")
    }
}
