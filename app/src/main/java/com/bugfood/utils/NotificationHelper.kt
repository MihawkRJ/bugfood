package com.bugfood.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bugfood.R
import com.bugfood.ui.MainActivity

object NotificationHelper {

    private const val CHANNEL_PERSISTENT = "bugfood_persistent"
    private const val CHANNEL_EVENTS = "bugfood_events"
    const val PERSISTENT_NOTIF_ID = 1001
    private const val EVENT_NOTIF_ID = 1002

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_PERSISTENT,
                "BugFood Ativo",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitorando iFood Entregadores em segundo plano"
                setShowBadge(false)
            })

            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_EVENTS,
                "Capturas e Auto-Fill",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificações de dados capturados e auto-preenchimento"
            })
        }
    }

    fun buildPersistentNotification(context: Context): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_PERSISTENT)
            .setContentTitle("BugFood 🐛")
            .setContentText("Monitorando iFood Entregadores…")
            .setSmallIcon(R.drawable.ic_bug_notif)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showCaptureNotification(context: Context, name: String, code: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setContentTitle("🐛 Novo pedido capturado")
            .setContentText("$name · Código: $code")
            .setSmallIcon(R.drawable.ic_bug_notif)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(EVENT_NOTIF_ID, notification)
        } catch (_: SecurityException) {}
    }

    fun showAutoFillNotification(context: Context, code: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_EVENTS)
            .setContentTitle("✅ Código auto-preenchido")
            .setContentText("Código $code inserido automaticamente")
            .setSmallIcon(R.drawable.ic_bug_notif)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(EVENT_NOTIF_ID + 1, notification)
        } catch (_: SecurityException) {}
    }
}
