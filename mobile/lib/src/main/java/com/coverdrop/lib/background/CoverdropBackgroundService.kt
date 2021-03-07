package com.coverdrop.lib.background

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.coverdrop.lib.CoverdropLib
import com.coverdrop.lib.R


internal const val ALARM_INTENT_REQUEST_CODE = 10
internal const val NOTIFICATION_CHANNEL_ID = "coverdrop"
internal const val NOTIFICATION_CHANNEL_NAME = "coverdrop"
internal const val NOTIFICATION_CODE = 10
internal const val TAG = "CoverdropBackgroundService"

/**
 * Once started the [CoverdropBackgroundService] will schedule itself as a [ForegroundService] according to the sync
 * interval stored in the PublicData.
 */
class CoverdropBackgroundService : IntentService("coverdrop_bg") {

    override fun onHandleIntent(intent: Intent?) {
        logEntry("Starting with intent: $intent")

        // schedule for next run
        schedule(this)

        try {
            // setup
            createNotificationChannel()

            // start foreground operations
            val notification = Notification.Builder(this, "coverdrop")
                .setSmallIcon(R.drawable.coverdrop_notification)
                .setContentTitle("Coverdrop working...")
                .setContentText("We are sending cover traffic to keep everyone secure")
                .build()
            startForeground(NOTIFICATION_CODE, notification)

            doWork()

            // clear notification
            stopForeground(true)
            logEntry("Finished with success")
        } catch (e: Exception) {
            logEntry("Failure: ${e.message}")
        } finally {
        }
    }

    private fun logEntry(text: String) {
//        // just for debugging
//        val dateString = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(Date())
//        val message = "$dateString $text"
//
//        Log.d(TAG, message)
//
//        val logFile = File(
//            applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
//            "coverdrop.debug.log"
//        )
//        Log.d(TAG, logFile.absolutePath)
//        logFile.appendText(message + '\n')
    }

    private fun doWork() {
        val coverdropLib = CoverdropLib.getInstance()
        coverdropLib.doRegularBackgroundOperation()
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel =
            NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance)
        notificationManager.createNotificationChannel(mChannel)
    }

    companion object {
        fun schedule(context: Context) {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = PendingIntent.getForegroundService(
                context,
                ALARM_INTENT_REQUEST_CODE,
                Intent(context, CoverdropBackgroundService::class.java),
                0
            )

            // clean current alarm
            alarmMgr.cancel(alarmIntent)

            val publicData = CoverdropLib.getInstance().getCoverdropPublicData()

            val intervalMs = publicData.getSyncIntervalMs()
            if (intervalMs < 0) {
                // negative values indicate a disabled sync service
                publicData.setSyncNextTimeMs(-1)
                return
            }

            val nextScheduledTimeRtc = System.currentTimeMillis() + intervalMs
            publicData.setSyncNextTimeMs(nextScheduledTimeRtc)

            val nextScheduledTimeElapsed = SystemClock.elapsedRealtime() + intervalMs
            alarmMgr.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                nextScheduledTimeElapsed,
                alarmIntent
            )

            Log.d(TAG, "Scheduled alarm for in ${intervalMs / 1000} seconds")
        }
    }
}
