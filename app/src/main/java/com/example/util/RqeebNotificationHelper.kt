package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.TeacherEntity

object RqeebNotificationHelper {

    private const val CHANNEL_ID = "rqeeb_teacher_requests_channel"
    private const val CHANNEL_NAME = "طلبات تفعيل المعلمين - رقيب"

    fun notifyNewTeacherRequest(context: Context, teacher: TeacherEntity) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فورية لمالك النظام عند تسجيل طلب معلم جديد"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_OWNER_PANEL", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            teacher.id.toInt().let { if (it != 0) it else (1000..9999).random() },
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sec = if (teacher.section.isNotBlank()) " (شعبة ${teacher.section})" else ""
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("رقيب – طلب تفعيل جديد")
            .setContentText("المعلم: ${teacher.fullName} | مدرسة: ${teacher.schoolName}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "المعلم: ${teacher.fullName}\n" +
                            "المدرسة: ${teacher.schoolName} (${teacher.governorate})\n" +
                            "المادة والصف: ${teacher.subject} - ${teacher.gradeTaught}$sec\n" +
                            "الهاتف: ${teacher.phone}\n" +
                            "يوجد طلب جديد بانتظار مراجعتك وتفعيله من هاتفك Realme."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(teacher.id.toInt().let { if (it != 0) it else 101 }, notification)
    }
}
