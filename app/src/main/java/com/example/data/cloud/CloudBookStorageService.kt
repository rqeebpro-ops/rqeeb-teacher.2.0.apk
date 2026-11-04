package com.example.data.cloud

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class CloudBookStorageService(private val context: Context) {
    private val cloudSyncService = CloudSyncService(context)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    val BUCKET_NAME = "teacher_smart_books_yemen"

    /**
     * Uploads PDF file from Uri to Supabase Storage Bucket 'teacher_smart_books_yemen'
     * Saves local persistent copy and returns storagePath + localFilePath
     */
    suspend fun uploadPdfBook(
        pdfUri: Uri,
        originalFileName: String,
        onProgress: (Int) -> Unit
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val supabaseUrl = cloudSyncService.getSupabaseUrl()
            val supabaseKey = cloudSyncService.getSupabaseAnonKey()

            // 1. Validate file extension/type
            val cleanName = originalFileName.ifBlank { "yemeni_textbook.pdf" }
            if (!cleanName.lowercase().endsWith(".pdf")) {
                return@withContext Result.failure(Exception("نوع الملف غير مدعوم. يرجى اختيار ملفات PDF فقط."))
            }

            // 2. Save local persistent copy first in app private directory
            val booksDir = File(context.filesDir, "yemen_books").apply { if (!exists()) mkdirs() }
            val sanitizedName = "${System.currentTimeMillis()}_${cleanName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"
            val localFile = File(booksDir, sanitizedName)

            val contentResolver = context.contentResolver
            contentResolver.openInputStream(pdfUri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("تعذر فتح وقراءة ملف PDF من ذاكرة الهاتف."))

            val fileSize = localFile.length()
            if (fileSize <= 0L) {
                return@withContext Result.failure(Exception("ملف PDF فارغ أو تالف (حجمه 0 بايت)."))
            }

            val storagePath = "textbooks/$sanitizedName"

            // 3. Stream upload to Supabase Storage if configured
            if (cloudSyncService.isConfigured()) {
                val uploadUrl = "$supabaseUrl/storage/v1/object/$BUCKET_NAME/$storagePath"

                val requestBody = object : RequestBody() {
                    override fun contentType() = "application/pdf".toMediaType()
                    override fun contentLength() = fileSize

                    override fun writeTo(sink: BufferedSink) {
                        localFile.inputStream().use { input ->
                            val buffer = ByteArray(32768)
                            var bytesCopied = 0L
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                                bytesCopied += read
                                val progress = ((bytesCopied.toDouble() / fileSize) * 100).toInt()
                                onProgress(progress.coerceIn(0, 100))
                            }
                        }
                    }
                }

                val request = Request.Builder()
                    .url(uploadUrl)
                    .addHeader("apikey", supabaseKey)
                    .addHeader("Authorization", "Bearer $supabaseKey")
                    .addHeader("Content-Type", "application/pdf")
                    .addHeader("x-upsert", "true")
                    .post(requestBody)
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errStr = response.body?.string().orEmpty()
                            if (response.code == 404 || errStr.contains("Bucket not found") || errStr.contains("not_found")) {
                                createBucketIfMissing(supabaseUrl, supabaseKey)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Upload proceeds locally and will sync when network is restored
                }
            } else {
                onProgress(100)
            }

            val publicUrl = if (cloudSyncService.isConfigured()) "$supabaseUrl/storage/v1/object/public/$BUCKET_NAME/$storagePath" else localFile.absolutePath
            Result.success(Pair(storagePath, localFile.absolutePath))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createBucketIfMissing(baseUrl: String, apiKey: String) {
        try {
            val json = JSONObject().apply {
                put("id", BUCKET_NAME)
                put("name", BUCKET_NAME)
                put("public", true)
            }
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val request = Request.Builder()
                .url("$baseUrl/storage/v1/bucket")
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(json.toString().toRequestBody(mediaType))
                .build()
            client.newCall(request).execute().close()
        } catch (_: Exception) {}
    }
}
