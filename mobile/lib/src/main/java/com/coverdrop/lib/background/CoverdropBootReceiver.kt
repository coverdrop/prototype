package com.coverdrop.lib.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Called on boot by the system to re-instantiate the [CoverdropBackgroundService].
 */
class CoverdropBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            context.startForegroundService(Intent(context, CoverdropBackgroundService::class.java))
        }
    }
}
