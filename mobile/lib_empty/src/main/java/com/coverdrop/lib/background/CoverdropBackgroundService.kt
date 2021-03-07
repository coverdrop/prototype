package com.coverdrop.lib.background

import android.app.IntentService
import android.content.Context
import android.content.Intent


internal const val ALARM_INTENT_REQUEST_CODE = 10
internal const val NOTIFICATION_CHANNEL_ID = "coverdrop"
internal const val NOTIFICATION_CHANNEL_NAME = "coverdrop"
internal const val NOTIFICATION_CODE = 10
internal const val TAG = "CoverdropBackgroundService"

class CoverdropBackgroundService : IntentService("coverdrop_bg") {

    override fun onHandleIntent(intent: Intent?) {
        TODO("stub")
    }

    companion object {
        fun schedule(context: Context) {
            TODO("stub")
        }
    }
}
