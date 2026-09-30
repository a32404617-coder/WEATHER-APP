package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.model.AlertSeverity
import com.example.model.SevereAlert

class StormNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "severe_storm_alerts_channel"
        const val CHANNEL_NAME = "Severe Storm Warnings & Alerts"
        const val EXTRA_TARGET_TAB = "extra_target_tab"
        const val EXTRA_ALERT_ID = "extra_alert_id"
        const val TAB_RADAR = "radar"
        const val TAB_ALERTS = "alerts"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Critical alerts for severe storms, tornadoes, hail, and flash floods with affected areas"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun postSevereAlertNotification(alert: SevereAlert) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_TAB, TAB_RADAR)
            putExtra(EXTRA_ALERT_ID, alert.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityColor = when (alert.severity) {
            AlertSeverity.EXTREME -> Color.parseColor("#DC2626") // Red
            AlertSeverity.SEVERE -> Color.parseColor("#EA580C")  // Orange
            AlertSeverity.MODERATE -> Color.parseColor("#D97706")// Amber
            AlertSeverity.MINOR -> Color.parseColor("#2563EB")   // Blue
        }

        val summaryText = "⚠️ ${alert.event.uppercase()}: ${alert.affectedAreaName}"
        val bigBody = buildString {
            append("• Hazard: ${alert.affectedArea.hazardType}\n")
            append("• Affected Area: ${alert.affectedAreaName}\n")
            if (alert.maxWindGustMph > 0) {
                append("• Wind Gusts: Up to ${alert.maxWindGustMph} mph\n")
            }
            if (alert.hailSizeInches > 0) {
                append("• Hail Size: ${alert.hailSizeInches}\"\n")
            }
            append("• Action: ${alert.instruction}")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⛈️ ${alert.event} Alert!")
            .setContentText(summaryText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("⚠️ ${alert.event} - Affected Area: ${alert.affectedAreaName}")
                    .bigText(bigBody)
            )
            .setColor(priorityColor)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(alert.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission might not be granted yet
        }
    }
}
