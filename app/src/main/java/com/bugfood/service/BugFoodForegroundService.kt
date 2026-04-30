package com.bugfood.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.bugfood.utils.NotificationHelper

class BugFoodForegroundService : Service() {

    companion object {
        private const val TAG = "BugFoodFGS"
        const val ACTION_START = "com.bugfood.START"
        const val ACTION_STOP = "com.bugfood.STOP"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val notification = NotificationHelper.buildPersistentNotification(this)
                startForeground(NotificationHelper.PERSISTENT_NOTIF_ID, notification)
                Log.d(TAG, "BugFood rodando em segundo plano")
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Foreground service destruído")
    }
}
