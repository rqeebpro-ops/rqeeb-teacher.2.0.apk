package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ContactUtils {

    fun openOwnerWhatsApp(context: Context, customMessage: String = "") {
        val phoneWithCountry = "967785502919"
        val encodedMsg = Uri.encode(
            if (customMessage.isNotBlank()) customMessage
            else "السلام عليكم ورحمة الله، أتواصل معكم بخصوص تطبيق «رقيب – مساعد المعلم الذكي»."
        )
        val url = "https://api.whatsapp.com/send?phone=$phoneWithCountry&text=$encodedMsg"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق واتساب. رقم الإدارة: 785502919", Toast.LENGTH_LONG).show()
        }
    }

    fun openTeacherWhatsApp(context: Context, teacherPhone: String, message: String) {
        val cleanPhone = teacherPhone.trim().replace("+", "").replace(" ", "")
        val formattedPhone = if (cleanPhone.startsWith("7")) "967$cleanPhone" else cleanPhone
        val encodedMsg = Uri.encode(message)
        val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMsg"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح واتساب للمعلم: $teacherPhone", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ $label إلى الحافظة بنجاح", Toast.LENGTH_SHORT).show()
    }
}
