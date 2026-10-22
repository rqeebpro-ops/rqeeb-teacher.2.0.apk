package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.YemeniCurriculum

object ContactUtils {

    /**
     * Opens WhatsApp conversation with the owner/administration.
     * Initial link: https://wa.me/967785502919
     */
    fun openOwnerWhatsApp(context: Context, customUrl: String? = null) {
        val targetUrl = if (!customUrl.isNullOrBlank()) customUrl else YemeniCurriculum.OWNER_WHATSAPP_URL_DEFAULT
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: try direct browser opening or tel
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "تعذر فتح واتساب، رقم التواصل: +967785502919", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Validates Yemeni phone number format (9 digits, usually starting with 77, 78, 73, 71, or 70)
     */
    fun isValidYemeniPhone(phone: String): Boolean {
        val cleaned = phone.trim().replace("+967", "").replace("00967", "")
        return cleaned.length in 9..10 && cleaned.all { it.isDigit() }
    }
}
