package com.tesfa.dualbeat.receiver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tesfa.dualbeat.MainActivity
import com.tesfa.dualbeat.R

class DualBeatCastService : Service() {

    companion object {
        const val CHANNEL_ID = "dualbeat_cast_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START_BROADCAST = "com.tesfa.dualbeat.ACTION_START_BROADCAST"
        const val ACTION_STOP_CAST = "com.tesfa.dualbeat.ACTION_STOP_CAST"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_CAST) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun buildForegroundNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("DualBeat Wi-Fi Multi-Speaker")
            .setContentText("Active synchronized audio distribution over Wi-Fi network")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DualBeat Cast Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Wi-Fi multi-speaker broadcast stream alive in background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
