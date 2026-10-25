package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.TeacherEntity

object RqeebNotificationHelper {

    const val CHANNEL_ID = "rqeeb_teacher_activations"
    const val CHANNEL_NAME = "طلبات تفعيل المعلمين"
    const val EXTRA_NAVIGATE_TO_OWNER = "extra_navigate_to_owner"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية عند وصول طلبات تسجيل وتفعيل جديدة من المعلمين إلى هاتف المالك"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun notifyNewTeacherRequest(context: Context, teacher: TeacherEntity) {
        initNotificationChannel(context)

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_OWNER, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            teacher.id.toInt().takeIf { it != 0 } ?: (1000..9999).random(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBody = "المعلم: ${teacher.fullName} | المدرسة: ${teacher.schoolName}"
        val bigText = """
            المعلم: ${teacher.fullName}
            المدرسة: ${teacher.schoolName}
            المادة: ${teacher.subject} | ${teacher.governorate}
            يوجد طلب جديد بانتظار مراجعتك في لوحة المالك.
        """.trimIndent()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("رقيب – طلب تفعيل جديد")
            .setContentText(notificationBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                teacher.id.toInt().takeIf { it != 0 } ?: System.currentTimeMillis().toInt(),
                notification
            )
        } catch (_: SecurityException) {
            // Handled safely
        }
    }
}
