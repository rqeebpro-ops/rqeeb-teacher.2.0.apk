package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import com.example.util.WhatsAppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    companion object {
        private const val TAG = "GeminiService"
        private const val PREFS_NAME = "rqeeb_gemini_prefs"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

        @Volatile
        private var INSTANCE: GeminiService? = null

        fun getInstance(@Suppress("UNUSED_PARAMETER") context: Context): GeminiService {
            return INSTANCE ?: synchronized(this) {
                val instance = GeminiService()
                INSTANCE = instance
                instance
            }
        }
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(50, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    fun getCustomApiKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_API_KEY, "") ?: ""
    }

    fun setCustomApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_API_KEY, key.trim())
            .apply()
    }

    fun getEffectiveApiKey(context: Context? = null): String {
        if (context != null) {
            val custom = getCustomApiKey(context)
            if (custom.isNotBlank()) return custom
        }
        return try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val key = keyField.get(null) as? String
            if (key != null && key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun isConfigured(context: Context? = null): Boolean {
        return getEffectiveApiKey(context).isNotBlank()
    }

    data class FullLessonPlanAiResult(
        val cognitiveObjectives: String,
        val skillObjectives: String,
        val affectiveObjectives: String,
        val expectedOutcomes: String,
        val targetKnowledgeAndSkills: String,
        val prerequisites: String,
        val introduction: String,
        val teachingSteps: String,
        val teacherStudentActivities: String,
        val discussionQuestions: String,
        val diagnosticAssessment: String,
        val formativeAssessment: String,
        val summativeAssessment: String,
        val homework: String,
        val remedialActivities: String,
        val enrichmentActivities: String,
        val timeDistribution: String,
        val teachingAids: String,
        val teacherNotes: String,
        val isFromGeminiApi: Boolean = false
    )

    suspend fun generateFullLessonPlan(
        subject: String,
        grade: String,
        unitName: String,
        lessonTitle: String,
        selectedStrategies: List<String>,
        context: Context? = null
    ): FullLessonPlanAiResult = withContext(Dispatchers.IO) {
        val dg = YemeniCurriculum.toDisplayGrade(grade)
        val strategiesText = if (selectedStrategies.isNotEmpty()) {
            selectedStrategies.joinToString("، ")
        } else {
            "الحوار والمناقشة، التعلم التعاوني، حل المشكلات"
        }

        val apiKey = getEffectiveApiKey(context)
        if (apiKey.isNotBlank()) {
            try {
                val apiResult = callGeminiForLessonPlan(apiKey, subject, dg, unitName, lessonTitle, strategiesText)
                if (apiResult != null) {
                    return@withContext apiResult.copy(isFromGeminiApi = true)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Gemini API call failed, falling back to rich curriculum generator: ${e.message}")
            }
        }

        // High quality Yemeni Curriculum Fallback generator
        createCurriculumFallbackPlan(subject, dg, unitName, lessonTitle, strategiesText)
    }

    private fun callGeminiForLessonPlan(
        apiKey: String,
        subject: String,
        grade: String,
        unitName: String,
        lessonTitle: String,
        strategies: String
    ): FullLessonPlanAiResult? {
        val prompt = """
            أنت موجه تربوي وخبير في المناهج الدراسية لوزارة التربية والتعليم في الجمهورية اليمنية.
            قم بإعداد خطة تحضير درس تفاعلي متكامل ونموذجي ومفصل للدرس التالي:
            - المادة: $subject
            - الصف الدراسي: $grade
            - الوحدة التعليمية: $unitName
            - عنوان الدرس: $lessonTitle
            - استراتيجيات التدريس المختارة: $strategies

            أجب حصراً بصيغة JSON صالحة تماماً، تحتوي على المفاتيح التالية باللغة العربية مع محتوى تعليمي رصين ومفصل ودقيق:
            {
              "cognitiveObjectives": "3 أهداف معرفية سلوكية قابلة للقياس تبدأ بـ (أن يتعرف/أن يفسر/أن يستنتج الطالب...)",
              "skillObjectives": "3 أهداف مهارية أدائية (أن يطبق/أن يرسم/أن يحل...)",
              "affectiveObjectives": "3 أهداف وجدانية وقيمية تعزز الهوية اليمنية والأمانة العلمية والتعاون",
              "expectedOutcomes": "مخرجات ونواتج التعلم المتوقعة بنهاية الحصة",
              "targetKnowledgeAndSkills": "المعارف والمفاهيم والمهارات المستهدفة في الدرس",
              "prerequisites": "المتطلبات السابقة ومراجعة ما يلزم للانطلاق في الدرس الجديد",
              "introduction": "التهيئة الحافزة والمقدمة الشيقة المستمدة من البيئة والواقع اليمني (5 دقائق)",
              "teachingSteps": "خطوات الشرح المتسلسلة وإجراءات التنفيذ مع الأمثلة (25 دقيقة)",
              "teacherStudentActivities": "بيان دور المعلم (الميسر والموجه) ودور الطالب (المشارك والباحث)",
              "discussionQuestions": "3 أسئلة صفية مثيرة للتفكير والنقاش",
              "diagnosticAssessment": "سؤال تشخيصي قبلي للتحقق من المكتسبات السابقة",
              "formativeAssessment": "تقويم تكويني مستمر أثناء الحصة مع أنشطة التحقق",
              "summativeAssessment": "تقويم ختامي في نهاية الحصة لقياس تحقق الأهداف",
              "homework": "واجب منزلي وتكليفات صفية محددة من الكتاب المدرسي",
              "remedialActivities": "أنشطة وتدابير علاجية لدعم الطلاب الضعاف والمتعثرين",
              "enrichmentActivities": "أنشطة إثرائية ومسائل تحدٍ للطلاب المتفوقين",
              "timeDistribution": "توزيع مقترح لدقائق الحصة (45 دقيقة)",
              "teachingAids": "الوسائل التعليمية ومصادر التعلم المتاحة",
              "teacherNotes": "ملاحظات وتأملات المعلم بعد تنفيذ الدرس"
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        // Try gemini-2.5-flash first, then gemini-1.5-flash
        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash")
        for (model in models) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: return@use
                        val rootJson = JSONObject(bodyString)
                        val candidates = rootJson.optJSONArray("candidates") ?: return@use
                        if (candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content") ?: return@use
                            val parts = content.optJSONArray("parts") ?: return@use
                            if (parts.length() > 0) {
                                var rawText = parts.getJSONObject(0).optString("text", "")
                                rawText = rawText.trim()
                                if (rawText.startsWith("```json")) {
                                    rawText = rawText.removePrefix("```json").trim()
                                }
                                if (rawText.startsWith("```")) {
                                    rawText = rawText.removePrefix("```").trim()
                                }
                                if (rawText.endsWith("```")) {
                                    rawText = rawText.removeSuffix("```").trim()
                                }

                                val parsed = JSONObject(rawText)
                                return FullLessonPlanAiResult(
                                    cognitiveObjectives = parsed.optString("cognitiveObjectives", ""),
                                    skillObjectives = parsed.optString("skillObjectives", ""),
                                    affectiveObjectives = parsed.optString("affectiveObjectives", ""),
                                    expectedOutcomes = parsed.optString("expectedOutcomes", ""),
                                    targetKnowledgeAndSkills = parsed.optString("targetKnowledgeAndSkills", ""),
                                    prerequisites = parsed.optString("prerequisites", ""),
                                    introduction = parsed.optString("introduction", ""),
                                    teachingSteps = parsed.optString("teachingSteps", ""),
                                    teacherStudentActivities = parsed.optString("teacherStudentActivities", ""),
                                    discussionQuestions = parsed.optString("discussionQuestions", ""),
                                    diagnosticAssessment = parsed.optString("diagnosticAssessment", ""),
                                    formativeAssessment = parsed.optString("formativeAssessment", ""),
                                    summativeAssessment = parsed.optString("summativeAssessment", ""),
                                    homework = parsed.optString("homework", ""),
                                    remedialActivities = parsed.optString("remedialActivities", ""),
                                    enrichmentActivities = parsed.optString("enrichmentActivities", ""),
                                    timeDistribution = parsed.optString("timeDistribution", ""),
                                    teachingAids = parsed.optString("teachingAids", ""),
                                    teacherNotes = parsed.optString("teacherNotes", ""),
                                    isFromGeminiApi = true
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing model $model: ${e.message}")
            }
        }
        return null
    }

    private fun createCurriculumFallbackPlan(
        subject: String,
        grade: String,
        unitName: String,
        lessonTitle: String,
        strategiesText: String
    ): FullLessonPlanAiResult {
        val cognitive = "1. أن يستوعب الطالب المفاهيم والمصطلحات الأساسية لدرس ($lessonTitle) في مقرر $subject لصف $grade وفق معايير وزارة التربية والتعليم.\n" +
                "2. أن يستنتج الطالب القواعد والنظريات العلمية المرتبطة بموضوع الدرس بدقة.\n" +
                "3. أن يفسر العلاقات المنطقية والرياضية المطروحة في الدرس ويطبقها في سياقات جديدة."

        val skill = "1. أن يطبق الطالب خطوات التحليل والحل المنهجي على مسائل وتدريبات متنوعة.\n" +
                "2. أن يوظف مهارات التفكير الناقد واستراتيجية ($strategiesText) في الأنشطة الصفية.\n" +
                "3. أن يتقن رسم وتدوين النتائج والاستنتاجات بدقة في كراسة النشاط الصفي."

        val affective = "1. أن يقدر الطالب أهمية موضوع ($lessonTitle) وتطبيقاته في خدمة المجتمع والتنمية في اليمن.\n" +
                "2. أن ينمي روح التعاون والعمل الفريقي الإيجابي أثناء المناقشة وحل التدريبات.\n" +
                "3. ترسيخ قيم الأمانة العلمية والاعتزاز بالهوية الوطنية والتفوق الدراسي."

        val expectedOutcomes = "• إتقان 85% على الأقل من مخرجات تعلم درس ($lessonTitle).\n" +
                "• القدرة على حل التمارين المستقلة دون أخطاء مفاهيمية.\n" +
                "• ربط المعارف الجديدة بالبيئة الحياتية والتجارب اليومية."

        val knowledgeAndSkills = "• المعارف المستهدفة: المفاهيم، القواعد، النظريات، المصطلحات الأساسية لـ ($lessonTitle).\n" +
                "• المهارات المستهدفة: التحليل، الاستنتاج، المقارنة، التطبيق العملي، حل المشكلات.\n" +
                "• القيم والاتجاهات: الدقة، احترام الرأي الآخر، الشغف العلمي، حب الاستطلاع والمثابرة."

        val prerequisites = "• مراجعة المفاهيم والخبرات السابقة المرتبطة بوحدة ($unitName).\n" +
                "• طرح سؤال استكشافي تشخيصي: ماذا نعرف سابقاً عن ($lessonTitle)؟\n" +
                "• رصد الفروق الفردية وتهيئة أذهان الطلاب عبر قصة قصيرة أو موقف تعليمي واقعي من البيئة المحلية."

        val introduction = "التهيئة الحافزة (5 دقائق):\n" +
                "عقد مقارنة سريعة أو عرض صورة/مسألة واقعية مستمدة من الحياة اليومية بالجمهورية اليمنية، تثير تساؤل الطلاب حول ($lessonTitle)، ثم استنتاج موضوع الدرس بمشاركة الطلبة وتدوين العنوان بوضوح على السبورة."

        val teachingSteps = "خطوات وإجراءات سير الدرس (25 دقيقة):\n" +
                "1. العرض والتوضيح: تقديم الفكرة الأساسية باستخدام الكتاب المدرسي والوسيلة المساعدة.\n" +
                "2. النمذجة (دور المعلم): حل نموذج توضيحي خطوة بخطوة مع التعليق على الأخطاء الشائعة.\n" +
                "3. الممارسة الموجهة: تطبيق استراتيجية ($strategiesText) عبر تقسيم الطلاب إلى مجموعات لحل تدريب صفي.\n" +
                "4. الممارسة المستقلة: تكليف الطلاب بحل تدريب فردي في الكراسة مع المتابعة الفردية وتقديم التغذية الراجعة.\n" +
                "5. الغلق والتلخيص: استخلاص الأفكار الرئيسية مع الطلاب وتدوين خلاصة سبورية موجزة."

        val teacherStudentActivities = "• دور المعلم: الموجه والميسر، طرح الأسئلة السابرة، إدارة النقاش الجماعي، توزيع أوراق العمل، متابعة الضعاف، تعزيز الإجابات المتميزة.\n" +
                "• دور الطالب: الاستماع الإيجابي، المشاركة في العصف الذهني، التعاون مع أفراد المجموعة، حل التدريبات الصفيّة، تدوين الملاحظات في الدفتر."

        val discussionQuestions = "1. كيف نفسر أهمية ($lessonTitle) في حياتنا اليومية؟\n" +
                "2. ما الفرق الجوهري بين هذه الحالة والحالات التي درسناها سابقاً؟\n" +
                "3. كيف نتأكد من صحة النتيجة التي توصلنا إليها؟"

        val diagnosticAssessment = "سؤال تشخيصي قبلي لقياس المتطلبات الأساسية ومستوى تمكن الطلاب قبل الشروع في الشرح."
        val formativeAssessment = "تقويم تكويني مستمر أثناء الحصة عبر الملاحظة المباشرة، الأسئلة الشفوية، بطاقات الإشارة، وفحص حلول التدريبات الصفيّة."
        val summativeAssessment = "تقويم ختامي (5 دقائق): سؤال شامل يقيس مدى تحقق الأهداف المعرفية والمهارية لدى جميع الطلاب."

        val homework = "حل التدريبات المقررة ص (...) من الكتاب المدرسي المعتمد، ونشاط إثرائي اختياري للبحث والاستكشاف."

        val remedial = "أنشطة علاجية للطلاب الذين يواجهون صعوبة: تبسيط المفاهيم عبر أمثلة حسية إضافية، إقران الطالب مع زميل متميز (تعليم الأقران)، تقديم أوراق عمل داعمة تحتوي خطوات استرشادية."
        val enrichment = "أنشطة إثرائية للطلاب المتفوقين: حل مسألة تحدٍ ذات مستويات تفكير عليا، إعداد ملخص بصري (خريطة مفاهيم)، أو مساعدة زملائهم كمعلمين صغار."

        val timeDistribution = "• التهيئة والربط بالدرس السابق: 5 دقائق\n" +
                "• الشرح والنمذجة والتوضيح: 15 دقيقة\n" +
                "• الأنشطة والممارسة الصفية (جماعي وفردي): 15 دقيقة\n" +
                "• التقويم الختامي والغلق: 6 دقائق\n" +
                "• تحديد الواجب المنزلي والتأمل: 4 دقائق\n" +
                "(إجمالي: 45 دقيقة)"

        val teachingAids = "الكتاب المدرسي، السبورة والأقلام الملونة، بطاقات الأنشطة التعاونية، وسائل توضيحية ومجسمات مناسبة للموضوع."

        val notes = "ملاحظات المعلم والتأمل الذاتي: تفاعل الطلاب كان إيجابياً، التمكن من استيعاب المفاهيم بنسبة عالية، والتأكيد على متابعة التدريبات الصفية في الحصة القادمة."

        return FullLessonPlanAiResult(
            cognitiveObjectives = cognitive,
            skillObjectives = skill,
            affectiveObjectives = affective,
            expectedOutcomes = expectedOutcomes,
            targetKnowledgeAndSkills = knowledgeAndSkills,
            prerequisites = prerequisites,
            introduction = introduction,
            teachingSteps = teachingSteps,
            teacherStudentActivities = teacherStudentActivities,
            discussionQuestions = discussionQuestions,
            diagnosticAssessment = diagnosticAssessment,
            formativeAssessment = formativeAssessment,
            summativeAssessment = summativeAssessment,
            homework = homework,
            remedialActivities = remedial,
            enrichmentActivities = enrichment,
            timeDistribution = timeDistribution,
            teachingAids = teachingAids,
            teacherNotes = notes,
            isFromGeminiApi = false
        )
    }

    suspend fun executeTextPrompt(prompt: String, context: Context? = null): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(context)
        if (apiKey.isBlank()) return@withContext ""
        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-pro")
        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val jsonBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply { put("text", prompt) })
                            })
                        })
                    })
                }
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string().orEmpty()
                        val rootJson = JSONObject(bodyString)
                        val candidates = rootJson.optJSONArray("candidates") ?: return@use
                        if (candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content") ?: return@use
                            val parts = content.optJSONArray("parts") ?: return@use
                            if (parts.length() > 0) {
                                return@withContext parts.getJSONObject(0).optString("text", "").trim()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing text prompt on model $model: ${e.message}")
            }
        }
        return@withContext ""
    }

    suspend fun generateStudentPedagogicalReport(
        student: StudentEntity,
        schoolName: String,
        teacherName: String,
        subjectName: String,
        reportCategory: String
    ): String = withContext(Dispatchers.IO) {
        val dg = YemeniCurriculum.toDisplayGrade(student.grade)
        val prompt = """
            أنت مساعد تربوي خبير في المدارس اليمنية. اكتب تقريراً أو رسالة موجهة لولي أمر الطالب التالية بياناته:
            - اسم الطالب: ${student.fullName}
            - الصف والشعبة: $dg - شعبة (${student.section})
            - المادة: $subjectName
            - اسم المدرسة: $schoolName
            - اسم المعلم: $teacherName
            - الدرجات المسجلة: اختبار شهر: ${student.monthlyTestScore}، منتصف الفصل: ${student.termScore}، واجبات: ${student.homeworkScore}، مشاركة: ${student.activityScore}
            - التقدير العام: ${student.generalRating}
            - نقاط القوة المسجلة: ${student.strengths.ifBlank { "انضباط ومواظبة" }}
            - جوانب التحسين المسجلة: ${student.weaknesses.ifBlank { "الاستمرار في المراجعة" }}
            - نوع التقرير المطلوب: $reportCategory

            شروط صياغة التقرير:
            1. صغ التقرير بأسلوب عربي تربوي مهذب، مشجع ومحترم جداً لولي الأمر.
            2. اعتمد حصراً على البيانات المسجلة أعلاه بدون اختراع درجات أو وقائع لم تسجل.
            3. حدد نقاط القوة والتوصيات العملية للمتابعة المنزلية.
            4. لا تتجاوز 150 كلمة ليكون التقرير مناسباً للإرسال عبر واتساب.
        """.trimIndent()

        val aiResult = executeTextPrompt(prompt)
        if (aiResult.isNotBlank()) {
            aiResult
        } else {
            WhatsAppUtils.buildReportMessage(reportCategory, student, schoolName, teacherName, subjectName)
        }
    }

    suspend fun generateLessonObjectives(
        subject: String,
        grade: String,
        unitName: String,
        lessonTitle: String
    ): Triple<String, String, String> = withContext(Dispatchers.IO) {
        val result = generateFullLessonPlan(subject, grade, unitName, lessonTitle, emptyList())
        Triple(result.cognitiveObjectives, result.skillObjectives, result.affectiveObjectives)
    }

    suspend fun generateExamQuestions(
        subject: String,
        grade: String,
        topic: String,
        examType: String,
        count: Int,
        difficulty: String,
        context: Context? = null
    ): List<ExamQuestion> = generateRichExamQuestions(
        subject = subject,
        grade = grade,
        topic = topic,
        examType = examType,
        count = count,
        totalScore = 20,
        difficulty = difficulty,
        selectedTypes = listOf("اختيار من متعدد", "صح وخطأ", "أكمل الفراغ", "مسائل رياضية وفيزيائية"),
        includeDiagrams = true,
        context = context
    )

    suspend fun generateRichExamQuestions(
        subject: String,
        grade: String,
        topic: String,
        examType: String,
        count: Int,
        totalScore: Int,
        difficulty: String,
        selectedTypes: List<String>,
        includeDiagrams: Boolean,
        context: Context? = null
    ): List<ExamQuestion> = withContext(Dispatchers.IO) {
        val dg = YemeniCurriculum.toDisplayGrade(grade)
        val availableTypes = if (selectedTypes.isNotEmpty()) selectedTypes else YemeniCurriculum.EXAM_QUESTION_TYPES

        val apiKey = getEffectiveApiKey(context)
        if (apiKey.isNotBlank()) {
            try {
                val aiQuestions = callGeminiForRichExam(
                    apiKey = apiKey,
                    subject = subject,
                    grade = dg,
                    topic = topic,
                    examType = examType,
                    count = count,
                    totalScore = totalScore,
                    difficulty = difficulty,
                    selectedTypes = availableTypes,
                    includeDiagrams = includeDiagrams
                )
                if (!aiQuestions.isNullOrEmpty()) {
                    return@withContext com.example.ui.teacher.ExamPlaceholderParser.parseAndEnrichQuestions(aiQuestions)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Gemini rich exam call failed, falling back to structured generator: ${e.message}")
            }
        }

        val questions = mutableListOf<ExamQuestion>()
        val baseScore = (totalScore / count.coerceAtLeast(1)).coerceAtLeast(1)
        val remainder = totalScore % count.coerceAtLeast(1)

        for (i in 1..count) {
            val qTypeStr = availableTypes[(i - 1) % availableTypes.size]
            val scoreForThis = baseScore + (if (i <= remainder) 1 else 0)

            when {
                qTypeStr.contains("اختيار من متعدد") -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.MULTIPLE_CHOICE,
                            text = "في موضوع ($topic) بمقرر $subject لصف $dg: أي العبارات التالية تعتبر القاعدة الصحيحة المعتمدة في المنهج؟",
                            options = listOf(
                                "أ) الاستنتاج العلمي المطابق للتعريف الرسمي",
                                "ب) الاستنتاج المعاكس للشرط الأساسي",
                                "ج) تطبيق يقتصر على الحالات الخاصة فقط",
                                "د) لا شيء مما سبق"
                            ),
                            correctAnswer = "أ) الاستنتاج العلمي المطابق للتعريف الرسمي",
                            score = scoreForThis,
                            explanation = "مستند إلى نص الدرس والقاعدة المقررة في الكتاب الوزاري."
                        )
                    )
                }

                qTypeStr.contains("صح") -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.TRUE_FALSE,
                            text = "ضع علامة (✓) أمام العبارة الصحيحة وعلامة (✗) أمام العبارة الخطأ: يعتبر تطبيق مفهوم ($topic) في مادة $subject ضرورياً لتفسير النتائج العلمية.",
                            options = listOf("صح (✓)", "خطأ (✗)"),
                            correctAnswer = "صح (✓)",
                            score = scoreForThis,
                            explanation = "قاعدة أساسية مثبتة في المنهج الدراسي المعتمد."
                        )
                    )
                }

                qTypeStr.contains("أكمل الفراغ") -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.FILL_BLANKS,
                            text = "أكمل الفراغ بما يناسبه علمياً: عند دراسة ($topic)، فإن النتيجة الأساسية تعتمد على .................... وعكسياً على ....................",
                            options = emptyList(),
                            correctAnswer = "القيمة الأساسية / المعامل الثابت",
                            score = scoreForThis,
                            explanation = "الاعتماد العلمي المباشر لقاعدة الدرس."
                        )
                    )
                }

                qTypeStr.contains("عرف") || qTypeStr.contains("المصطلحات") -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.DEFINITIONS,
                            text = "عرّف المصطلح العلمي التالي تعريفاً دقيقاً وفق المنهج: ($topic).",
                            options = emptyList(),
                            correctAnswer = "هو المفهوم العلمي/التربوي الدال على الإجراء المنظم الذي يربط بين المدخلات والنتائج وفق شروط محددة.",
                            score = scoreForThis,
                            explanation = "التعريف النصي الوارد في مسرد مصطلحات الكتاب المدرسي."
                        )
                    )
                }

                qTypeStr.contains("علل") -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.EXPLAIN_WHY,
                            text = "علّل واذكر السبب العلمي: لماذا يجب مراعاة الشروط المحددة عند دراسة ($topic) في $subject؟",
                            options = emptyList(),
                            correctAnswer = "لأن الإخلال بأحد الشروط يؤدي إلى تغير النتيجة وعدم انطباق القانون العلمي المقرر.",
                            score = scoreForThis,
                            explanation = "التفسير المنطقي والفيزيائي/العلمي المعتمد في المنهج."
                        )
                    )
                }

                qTypeStr.contains("قارن") -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.COMPARE,
                            text = "قارن بين المفهومين الرئيسيين في درس ($topic) من حيث التعريف والخصائص والتطبيق العملي: [جدول مقارنة]",
                            options = emptyList(),
                            correctAnswer = "الحالة الأولى: تتميز بالثبات والانتظام. الحالة الثانية: تتميز بالتغير والاعتماد على المتغيرات المحيطة.",
                            score = scoreForThis,
                            explanation = "المقارنة المنهجية المعتمدة في التقويم الفصلي والنهائي."
                        )
                    )
                }

                (qTypeStr.contains("الرسم") || includeDiagrams) && i == count -> {
                    val subjectTag = when {
                        subject.contains("الرياضيات") || subject.contains("هندسة") -> "[صورة: مثلث قائم]"
                        subject.contains("الفيزياء") -> "[صورة: دائرة كهربائية]"
                        subject.contains("الكيمياء") -> "[صورة: ذرة]"
                        subject.contains("الأحياء") || subject.contains("العلوم") -> "[صورة: نبتة]"
                        else -> "[صورة: مثلث قائم]"
                    }
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.LABEL_DIAGRAM,
                            text = "تأمل الشكل التوضيحي التالي ثم أجب عن المطلوب واكتب البيانات الأساسية عليه بدقة: $subjectTag",
                            options = emptyList(),
                            correctAnswer = "البيان [1]: المكون الأساسي، البيان [2]: الجزء الرابط، البيان [3]: الناتج النهائي المكتمل.",
                            score = scoreForThis,
                            explanation = "تطبيق صفي مباشر على قراءة وتحليل الرسوم التوضيحية العلمية والهندسية."
                        )
                    )
                }

                else -> {
                    questions.add(
                        ExamQuestion(
                            id = i,
                            type = QuestionType.MATH_PHYSICS_PROBLEM,
                            text = "مسألة تطبيقية حول ($topic): احسب القيمة المطلوبة موضحاً خطوات الحل الرياضي والقوانين المستخدمة والوحدات: [جدول بيانات 2×3]",
                            options = emptyList(),
                            correctAnswer = "خطوة 1: كتابة المعطيات والقانون المقرر.\nخطوة 2: التعويض المباشر بالأرقام.\nخطوة 3: استخراج الناتج النهائي مع كتابة وحدة القياس الصحيحة.",
                            score = scoreForThis,
                            explanation = "خطوات حل نموذجية معتمدة في سلم التصحيح."
                        )
                    )
                }
            }
        }

        com.example.ui.teacher.ExamPlaceholderParser.parseAndEnrichQuestions(questions)
    }

    private fun callGeminiForRichExam(
        apiKey: String,
        subject: String,
        grade: String,
        topic: String,
        examType: String,
        count: Int,
        totalScore: Int,
        difficulty: String,
        selectedTypes: List<String>,
        includeDiagrams: Boolean
    ): List<ExamQuestion>? {
        val prompt = """
            أنت خبير تربوي ومصمم اختبارات رسمية للمناهج الدراسية بالجمهورية اليمنية.
            صمم اختباراً دقيقاً ومنسقاً وفق المواصفات التالية:
            - المادة: $subject
            - الصف الدراسي: $grade
            - الموضوع / الوحدة: $topic
            - نوع الاختبار: $examType
            - مستوى الصعوبة: $difficulty
            - عدد الأسئلة المطلوب: $count
            - مجموع الدرجات الكلي: $totalScore (تأكد أن مجموع درجات الأسئلة يساوي $totalScore تماماً)
            - أنواع الأسئلة المفضلة: ${selectedTypes.joinToString("، ")}
            - تضمين رسومات وجداول: ${if (includeDiagrams) "نعم، مهم جداً" else "حسب الحاجة"}

            تعليمات مهمة حول العلامات (Placeholders):
            عند الحاجة لرسومات توضيحية أو جداول في أي سؤال، ضع علامة (Placeholder) واضحة داخل نص السؤال مثل:
            - [صورة: نبتة] أو [صورة: مثلث قائم] أو [صورة: دائرة كهربائية] أو [صورة: خلية حيوانية] أو [صورة: مجهر] أو [صورة: أسد] أو [صورة: مغناطيس] أو [صورة: ذرة]
            - [جدول مقارنة] أو [جدول بيانات 3×3]
            سيقوم التطبيق تلقائياً بتحويل هذه العلامات إلى رسومات وأيقونات وجداول فعلية داخل ورقة الاختبار.

            أجب حصراً بصيغة JSON صالحة (JSON Object) بالشكل التالي:
            {
              "questions": [
                {
                  "id": 1,
                  "type": "MULTIPLE_CHOICE | TRUE_FALSE | FILL_BLANKS | COMPARE | LABEL_DIAGRAM | DEFINITIONS | EXPLAIN_WHY | MATH_PHYSICS_PROBLEM | ESSAY",
                  "text": "نص السؤال باللغة العربية وقد يتضمن [صورة: ...] أو [جدول مقارنة]",
                  "options": ["أ) ...", "ب) ...", "ج) ...", "د) ..."],
                  "correctAnswer": "الإجابة الصحيحة النموذجية",
                  "score": 4,
                  "explanation": "توزيع الدرجات وشرح الحل"
                }
              ]
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.35)
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash")

        for (model in models) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder().url(url).post(requestBody).build()
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val rawBody = response.body?.string() ?: return@use
                        val root = JSONObject(rawBody)
                        val candidates = root.optJSONArray("candidates") ?: return@use
                        if (candidates.length() > 0) {
                            var textOut = candidates.getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                                .trim()

                            if (textOut.startsWith("```json")) textOut = textOut.removePrefix("```json").trim()
                            if (textOut.startsWith("```")) textOut = textOut.removePrefix("```").trim()
                            if (textOut.endsWith("```")) textOut = textOut.removeSuffix("```").trim()

                            val parsedObj = JSONObject(textOut)
                            val arr = parsedObj.optJSONArray("questions") ?: return@use
                            val list = mutableListOf<ExamQuestion>()
                            for (i in 0 until arr.length()) {
                                val item = arr.getJSONObject(i)
                                val typeStr = item.optString("type", "ESSAY").uppercase()
                                val qType = QuestionType.entries.firstOrNull { it.name == typeStr } ?: QuestionType.ESSAY
                                val opts = mutableListOf<String>()
                                val optsArr = item.optJSONArray("options")
                                if (optsArr != null) {
                                    for (j in 0 until optsArr.length()) {
                                        val optVal = optsArr.optString(j)
                                        if (optVal.isNotBlank()) opts.add(optVal)
                                    }
                                }
                                list.add(
                                    ExamQuestion(
                                        id = item.optInt("id", i + 1),
                                        type = qType,
                                        text = item.optString("text", ""),
                                        options = opts,
                                        correctAnswer = item.optString("correctAnswer", ""),
                                        score = item.optInt("score", 2).coerceAtLeast(1),
                                        explanation = item.optString("explanation", "")
                                    )
                                )
                            }
                            if (list.isNotEmpty()) return list
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "callGeminiForRichExam error on $model: ${e.message}")
            }
        }
        return null
    }

    /**
     * Generates an official Yemeni Mathematics Exam strictly conforming to the reference paper layout.
     * Accurately creates question items with diagrams, comparison circles, place value tables,
     * word problems, and shape matching according to the grade.
     */
    fun generateOfficialYemeniMathExam(
        grade: String,
        randomize: Boolean = true
    ): List<ExamQuestion> {
        val dg = YemeniCurriculum.toDisplayGrade(grade)
        val rndSeed = if (randomize) (System.currentTimeMillis() % 100).toInt() else 0

        return when {
            // Grades 1, 2, 3: Directly corresponds to the reference examination sheet
            dg.contains("1/") || dg.contains("2/") || dg.contains("3/") -> {
                val p1 = if (rndSeed % 2 == 0) "80 ○ 50" else "90 ○ 70"
                val p2 = if (rndSeed % 2 == 0) "31 ○ 36" else "42 ○ 48"
                val p3 = if (rndSeed % 2 == 0) "30 ○ 45" else "55 ○ 60"
                val fishTotal = 30 + (rndSeed % 15)
                val fishSold = 20 + (rndSeed % 9)
                val fishRemaining = fishTotal - fishSold

                listOf(
                    ExamQuestion(
                        id = 1,
                        type = QuestionType.PRACTICAL_APPLIED,
                        text = "ضع إشارة > أو < أو = في ○:",
                        options = emptyList(),
                        correctAnswer = "80 > 50 ، 31 < 36 ، 30 < 45",
                        score = 3,
                        explanation = "درجة واحدة لكل مقارنة صحيحة توضع داخل الدائرة.",
                        diagramType = "MATH_COMPARE_CIRCLES",
                        diagramInstruction = "اكتب علامة المقارنة المناسبة داخل كل دائرة"
                    ),
                    ExamQuestion(
                        id = 2,
                        type = QuestionType.TABLE_GRAPH_READING,
                        text = "أوجد ناتج الجمع:",
                        options = emptyList(),
                        correctAnswer = "الجدول 1: 21 + 35 = 56\nالجدول 2: 46 + 30 = 76\nالجمع الرأسي الأول: 34 + 44 = 78\nالجمع الرأسي الثاني: 63 + 32 = 95",
                        score = 8,
                        explanation = "درجتان لكل جدول قيمة مكانية، ودرجتان لكل مسألة جمع رأسي.",
                        diagramType = "MATH_PLACE_VALUE_TABLE",
                        diagramInstruction = "اجمع الآحاد مع الآحاد والعشرات مع العشرات واكتب الناتج في المربع المخصص"
                    ),
                    ExamQuestion(
                        id = 3,
                        type = QuestionType.MATH_PHYSICS_PROBLEM,
                        text = "اصطاد مروان $fishTotal سمكة باع منها $fishSold سمكة. كم سمكة بقيت؟",
                        options = emptyList(),
                        correctAnswer = "$fishTotal - $fishSold = $fishRemaining سمكة",
                        score = 2,
                        explanation = "درجة لكتابة العملية الحسابية ودرجة للناتج الصحيح مع التمييز.",
                        diagramType = "MATH_WORD_PROBLEM_BOX",
                        diagramInstruction = "اكتب ناتج العملية الحسابية داخل المربع"
                    ),
                    ExamQuestion(
                        id = 4,
                        type = QuestionType.LABEL_DIAGRAM,
                        text = "صل بين النقاط لتحصل على قطعة مستقيمة:",
                        options = emptyList(),
                        correctAnswer = "التوصيل بالمسطرة بين النقطتين لتكوين قطعة مستقيمة مستوية ومحددة.",
                        score = 3,
                        explanation = "درجتان لدقة الاستقامة، ودرجة لتحديد نقطتي البداية والنهاية.",
                        diagramType = "MATH_CONNECT_DOTS",
                        diagramInstruction = "استخدم المسطرة للتوصيل بين النقاط بخط مستقيم واضح"
                    ),
                    ExamQuestion(
                        id = 5,
                        type = QuestionType.MATCHING,
                        text = "صل الصورة بالتسمية المناسبة لها:",
                        options = emptyList(),
                        correctAnswer = "الدائرة -> دائرة\nالمستطيل -> مستطيل\nالمثلث -> مثلث\nالمربع -> مربع\nشبه المنحرف -> شبه منحرف",
                        score = 5,
                        explanation = "درجة واحدة لكل شكل موصول بصورة صحيحة باسمه العلمي.",
                        diagramType = "MATH_SHAPE_MATCH",
                        diagramInstruction = "صل كل مجسم أو شكل هندسي باسمه المطابق في القائمة"
                    )
                )
            }

            // Grades 4, 5, 6
            dg.contains("4/") || dg.contains("5/") || dg.contains("6/") -> {
                listOf(
                    ExamQuestion(
                        id = 1,
                        type = QuestionType.TABLE_GRAPH_READING,
                        text = "اكتب الكسر الدال على الجزء المظلل في كل شكل هندسي مما يلي:",
                        options = emptyList(),
                        correctAnswer = "الشكل الأول (الدائرة): 1/4 (ربع)، الشكل الثاني (المستطيل): 1/3 (ثلث)",
                        score = 4,
                        explanation = "درجتان لكل كسر مكتوب بصيغة بسط ومقام صحيحة.",
                        diagramType = "MATH_FRACTION_SHAPES",
                        diagramInstruction = "حدد عدد الأجزاء المظللة إلى إجمالي عدد الأجزاء"
                    ),
                    ExamQuestion(
                        id = 2,
                        type = QuestionType.PRACTICAL_APPLIED,
                        text = "مثّل عملية الجمع على خط الأعداد الموضح أدناه:",
                        options = emptyList(),
                        correctAnswer = "الانطلاق من الصفر ثم القفز بمقدار 20 ثم 20 لنصل إلى 40.",
                        score = 4,
                        explanation = "توزيع الدرجات: درجتان للتمثيل السهمي ودرجتان للناتج.",
                        diagramType = "MATH_NUMBER_LINE",
                        diagramInstruction = "ارسم القفزات الحسابية بالسهم على الخط"
                    ),
                    ExamQuestion(
                        id = 3,
                        type = QuestionType.MATH_PHYSICS_PROBLEM,
                        text = "مستطيل طوله 8 سم وعرضه 5 سم. احسب كلاً من محيطه ومساحته موضحاً القانون وخطوات الحل:",
                        options = emptyList(),
                        correctAnswer = "المحيط = 2 × (الطول + العرض) = 2 × (8 + 5) = 26 سم\nالمساحة = الطول × العرض = 8 × 5 = 40 سم²",
                        score = 4,
                        explanation = "درجتان للمحيط ودرجتان للمساحة مع الوحدات.",
                        diagramType = "NONE"
                    ),
                    ExamQuestion(
                        id = 4,
                        type = QuestionType.MULTIPLE_CHOICE,
                        text = "أي الزوايا التالية تعتبر زاوية منفرجة وفق التصنيف الهندسي؟",
                        options = listOf("أ) 45°", "ب) 90°", "ج) 120°", "د) 180°"),
                        correctAnswer = "ج) 120°",
                        score = 4,
                        explanation = "لأن قياس الزاوية المنفرجة يقع بين 90° و 180°."
                    ),
                    ExamQuestion(
                        id = 5,
                        type = QuestionType.MATCHING,
                        text = "صل كل شكل هندسي رباعي بالخاصية المميزة له:",
                        options = emptyList(),
                        correctAnswer = "المربع -> جميع أضلاعه متطابقة وزواياه قائمة\nالمعين -> جميع أضلاعه متطابقة وأقطاره متعامدة\nشبه المنحرف -> فيه ضلعان فقط متوازيان\nالمستطيل -> كل ضلعين متقابلين متساويان وزواياه قائمة",
                        score = 4,
                        explanation = "درجة واحدة لكل توصيل صحيح.",
                        diagramType = "MATH_SHAPE_MATCH"
                    )
                )
            }

            // Grades 7, 8, 9 (المرحلة الإعدادية)
            dg.contains("7/") || dg.contains("8/") || dg.contains("9/") -> {
                listOf(
                    ExamQuestion(
                        id = 1,
                        type = QuestionType.MATH_PHYSICS_PROBLEM,
                        text = "في المثلث القائم الزاوية الموضح في الرسم: إذا كان طول الضلع أ ب = 3 سم، وطول ب ج = 4 سم، فاحسب طول الوتر (أ ج) مستخدماً مبرهنة فيثاغورس:",
                        options = emptyList(),
                        correctAnswer = "(أ ج)² = (أ ب)² + (ب ج)² = 3² + 4² = 9 + 16 = 25\nأ ج = √25 = 5 سم",
                        score = 5,
                        explanation = "درجتان للقانون، درجتان للتعويض، ودرجة للناتج مع الوحدة.",
                        diagramType = "GEOMETRY_TRIANGLE",
                        diagramInstruction = "طبّق نظرية فيثاغورس لحساب طول الوتر س"
                    ),
                    ExamQuestion(
                        id = 2,
                        type = QuestionType.PRACTICAL_APPLIED,
                        text = "في الرسم البياني للمستوى الإحداثي: عيّن النقطتين أ(1, 2) و ب(-1, 0) ثم ارسم الخط المستقيم المار بهما واحسب ميله:",
                        options = emptyList(),
                        correctAnswer = "الميل (م) = (ص2 - ص1) / (س2 - س1) = (0 - 2) / (-1 - 1) = -2 / -2 = 1\nمعادلة المستقيم: ص = س + 1",
                        score = 5,
                        explanation = "درجتان لتعيين النقاط، درجتان لحساب الميل، ودرجة لرسم المستقيم.",
                        diagramType = "COORDINATE_GRAPH",
                        diagramInstruction = "عيّن النقطتين على شبكة الإحداثيات ثم صل بينهما"
                    ),
                    ExamQuestion(
                        id = 3,
                        type = QuestionType.LABEL_DIAGRAM,
                        text = "بالاعتماد على شكل الدائرة المرفق: اذكر المسميات الرياضية للقطع والعناصر المشار إليها بالأرقام (1)، (2)، (3):",
                        options = emptyList(),
                        correctAnswer = "العنصر (1): مركز الدائرة (م)\nالعنصر (2): نصف القطر (نق)\nالعنصر (3): المماس الخارجي",
                        score = 3,
                        explanation = "درجة واحدة لكل مسمى هندسي دقيق.",
                        diagramType = "GEOMETRY_CIRCLE",
                        diagramInstruction = "اكتب اسم كل عنصر هندسي مشار إليه بالسهم"
                    ),
                    ExamQuestion(
                        id = 4,
                        type = QuestionType.MATH_PHYSICS_PROBLEM,
                        text = "حل المعادلة الجبرية التالية موضحاً خطوات التبسيط والتحقق: 3س + 5 = 20",
                        options = emptyList(),
                        correctAnswer = "3س = 20 - 5\n3س = 15\nس = 15 / 3\nس = 5\nالتحقق: 3(5) + 5 = 15 + 5 = 20 (صحيح)",
                        score = 4,
                        explanation = "درجة لنقل الثابت، درجة للقسمة، درجة لقيمة س، ودرجة للتحقق."
                    ),
                    ExamQuestion(
                        id = 5,
                        type = QuestionType.MULTIPLE_CHOICE,
                        text = "إذا قطع مستقيم مستقيمين متوازيين، فإن كل زاويتين متبادلتين داخلياً تكونان:",
                        options = listOf("أ) متكاملتين", "ب) متطابقتين (متساويتين بالقياس)", "ج) متتامتين", "د) غير متساويتين"),
                        correctAnswer = "ب) متطابقتين (متساويتين بالقياس)",
                        score = 3,
                        explanation = "مبرهنة أساسية في هندسة التوازي والتقاطع المقررة في المنهج اليمني."
                    )
                )
            }

            // Grades 10, 11, 12 (المرحلة الثانوية)
            else -> {
                listOf(
                    ExamQuestion(
                        id = 1,
                        type = QuestionType.MATH_PHYSICS_PROBLEM,
                        text = "إذا كانت د(س) = 2س³ - 5س² + 4س - 7، فأوجد كلاً من المشتقة الأولى د'(س) والمشتقة الثانية د''(س) وقيمة د'(2):",
                        options = emptyList(),
                        correctAnswer = "د'(س) = 6س² - 10س + 4\nد''(س) = 12س - 10\nد'(2) = 6(4) - 10(2) + 4 = 24 - 20 + 4 = 8",
                        score = 5,
                        explanation = "درجتان للمشتقة الأولى، درجتان للمشتقة الثانية، ودرجة للتعويض والناتج."
                    ),
                    ExamQuestion(
                        id = 2,
                        type = QuestionType.PRACTICAL_APPLIED,
                        text = "بالاستعانة بشبكة الإحداثيات المتعامدة: ارسم منحنى الدالة الخطية ص = س + 1 واكتب نقطتي تقاطعه مع المحورين س و ص:",
                        options = emptyList(),
                        correctAnswer = "نقطة التقاطع مع محور الصادات: (0, 1)\nنقطة التقاطع مع محور السينات: (-1, 0)",
                        score = 4,
                        explanation = "درجتان لتعيين نقاط التقاطع، ودرجتان للرسم البياني المستقيم.",
                        diagramType = "COORDINATE_GRAPH",
                        diagramInstruction = "حدد نقاط التقاطع وارسم المستقيم البياني"
                    ),
                    ExamQuestion(
                        id = 3,
                        type = QuestionType.MATH_PHYSICS_PROBLEM,
                        text = "أوجد قيمة النهاية التالية: نـهـا (س² - 9) / (س - 3) عندما س تؤول إلى 3:",
                        options = emptyList(),
                        correctAnswer = "التعويض المباشر يعطي 0/0 (كمية غير معينة).\nبالتحليل إلى فرق بين مربعين: (س - 3)(س + 3) / (س - 3) = س + 3\nالناتج = 3 + 3 = 6",
                        score = 4,
                        explanation = "درجة للتعويض المباشر، درجتان للتحليل والاختصار، ودرجة للناتج النهائي."
                    ),
                    ExamQuestion(
                        id = 4,
                        type = QuestionType.MULTIPLE_CHOICE,
                        text = "في حساب المثلثات، المقدار المتطابق مع (جا² هـ + جتا² هـ) يساوي دائماً:",
                        options = listOf("أ) 0", "ب) 1", "ج) ظا هـ", "د) 2"),
                        correctAnswer = "ب) 1",
                        score = 3,
                        explanation = "متطابقة فيثاغورس المثلثية الأساسية المقررة في المنهج اليمني."
                    ),
                    ExamQuestion(
                        id = 5,
                        type = QuestionType.MATH_PHYSICS_PROBLEM,
                        text = "احسب التكامل المحدد التالي: تكامل من 0 إلى 2 للمقدار (3س² + 2س) بالنسبة لـ س:",
                        options = emptyList(),
                        correctAnswer = "[س³ + س²] من 0 إلى 2 = (2³ + 2²) - (0) = 8 + 4 = 12",
                        score = 4,
                        explanation = "درجتان لإجراء التكامل ودرجتان للتعويض بحدود التكامل واستخراج 12."
                    )
                )
            }
        }
    }

    suspend fun generateSmartExplanation(
        subject: String,
        grade: String,
        unitName: String,
        lessonTitle: String,
        lessonContentText: String,
        expansionLevel: String = "أعمق (مفصل وشامل)",
        dialectStyle: String = "لهجة بيضاء يمنية",
        studentLevel: String = "شامل لجميع المستويات",
        examplesCount: Int = 5,
        selectedFeatures: List<String> = listOf("تشبيهات يمنية", "تجارب وأنشطة", "أسئلة تفاعلية", "سيناريو إلقاء", "تخطيط السبورة"),
        context: Context? = null
    ): SmartExplanationResult = withContext(Dispatchers.IO) {
        val dg = YemeniCurriculum.toDisplayGrade(grade)
        val apiKey = getEffectiveApiKey(context)

        if (apiKey.isNotBlank()) {
            try {
                val apiResult = callGeminiForSmartExplanation(
                    apiKey = apiKey,
                    subject = subject,
                    grade = dg,
                    unitName = unitName,
                    lessonTitle = lessonTitle,
                    lessonContentText = lessonContentText,
                    expansionLevel = expansionLevel,
                    dialectStyle = dialectStyle,
                    studentLevel = studentLevel,
                    examplesCount = examplesCount,
                    selectedFeatures = selectedFeatures
                )
                if (apiResult != null) {
                    return@withContext apiResult.copy(isFromGeminiApi = true)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Gemini smart explanation API call failed, using curriculum fallback: ${e.message}")
            }
        }

        // High-fidelity Yemeni Pedagogical fallback generator
        createCurriculumFallbackSmartExplanation(
            subject = subject,
            grade = dg,
            unitName = unitName,
            lessonTitle = lessonTitle,
            lessonContentText = lessonContentText,
            expansionLevel = expansionLevel,
            dialectStyle = dialectStyle,
            studentLevel = studentLevel,
            examplesCount = examplesCount
        )
    }

    private fun callGeminiForSmartExplanation(
        apiKey: String,
        subject: String,
        grade: String,
        unitName: String,
        lessonTitle: String,
        lessonContentText: String,
        expansionLevel: String,
        dialectStyle: String,
        studentLevel: String,
        examplesCount: Int,
        selectedFeatures: List<String>
    ): SmartExplanationResult? {
        val prompt = """
            أنت الموجه التربوي الأول وخبير تعليمي متخصص في المناهج الدراسية للجمهورية اليمنية.
            مهمتك ليست مجرد تلخيص الدرس، بل تقديم «شرح ذكي وموسع ودليل تدريس بيداغوجي متكامل» للمعلم لتمكينه من إيصال المعلومة للطلاب بأفضل وأمتع طريقة داخل الفصل.

            بيانات الدرس:
            - المادة: $subject
            - الصف: $grade
            - الوحدة: $unitName
            - عنوان الدرس: $lessonTitle
            - نص الدرس أو محاوره الإضافية: ${lessonContentText.ifBlank { "وفق المنهج اليمني المعتمد" }}
            - مستوى التوسع المطلوب: $expansionLevel
            - اللهجة المطلوبة للتشبيهات: $dialectStyle (استخدم عبارات مهذبة وقريبة ومحببة للطلاب)
            - مستوى الطلاب المستهدف: $studentLevel
            - عدد الأمثلة المطلوبة: $examplesCount
            - الميزات المطلوبة: ${selectedFeatures.joinToString("، ")}

            تعليمات إلزامية بالغة الأهمية:
            1. الشرح العميق لكل فكرة: فكك الدرس إلى مفاهيم أساسية، واشرح كيف ولماذا تحدث الظاهرة أو القاعدة، وبيّن الأسباب والنتائج والمفاهيم الخاطئة وإجابات أسئلة الطلاب.
            2. أمثلة من البيئة اليمنية الأصيلة: استعن بالأسواق (سوق الملح، أسواق الخضار، دكاكين القرى)، والزراعة (مزارع البن، العنب الرازقي، الذرة، النخيل)، والطبيعة (مدرجات جبال حراز وصعدة، الأمطار والسيول، السدود، حرارة تهامة، برودة ذمار)، والحياة اليومية (منظومات الطاقة الشمسية والبطاريات والإنفرتر، خزانات المياه والدينامو، الباصات والدبابات وفرزات النقل، مواقد الحطب، بيوت الحجر والطين).
            3. التشبيهات باللهجة اليمنية: اكتب التشبيه تحت بند "قرّب المعلومة للطالب" بصيغة (تخيل يا بني زي لما...) لتبسيط المفهوم المجرد.
            4. دليل إلقاء الفكرة (10 خطوات عملية للمعلم).
            5. استراتيجيات التدريس الصفية وتجارب عملية بأدوات متوفرة منخفضة التكلفة.
            6. علاج تعثر الفهم («إذا لم يفهم الطالب»).
            7. أسئلة صفية مثيرة للتفكير مع الإجابات المتوقعة وكيفية تصحيح أخطاء الطلاب.
            8. أمثلة متدرجة من المبتدئ إلى المتقدم مع حلول خطواتية كاملة.
            9. سيناريو إلقاء الحصة كاملاً وتخطيط السبورة ومعلومات إثرائية متقدمة لثقافة المعلم.

            أجب حصراً بـ JSON صالح تماماً بالبنية الآتية:
            {
              "concepts": [
                {
                  "title": "اسم المفهوم/الفكرة الأساسية",
                  "deepExplanation": "شرح علمي وتربوي موسع وعميق يفكك الفكرة ويوضح جوهرها",
                  "whyAndHow": "توضيح لماذا وكيف تحدث الظاهرة أو تطبق القاعدة خطوة بخطوة",
                  "causeAndEffect": "الأسباب والنتائج والارتباط مع المفاهيم السابقة واللاحقة",
                  "commonMisconception": "المفهوم الخاطئ الشائع لدى الطلاب وكيف يعالجه المعلم فوراً",
                  "studentQuestion": "سؤال متوقع يطرحه الطالب في الفصل",
                  "studentQuestionAnswer": "إجابة مقنعة مبسطة يقدمها المعلم للطالب"
                }
              ],
              "yemeniAnalogies": [
                {
                  "title": "الفكرة العلمية المقابلة",
                  "analogyText": "تخيل يا بني زي لما...",
                  "spokenDialect": "العبارة المنطوقة باللهجة المحددة",
                  "explanationLink": "كيف يربط المعلم هذا التشبيه بالمصطلح العلمي بدقة"
                }
              ],
              "yemeniExamples": [
                {
                  "title": "عنوان المثال",
                  "contextType": "سوق / زراعة / طاقة شمسية / جبال ومياه / مواصلات",
                  "description": "سياق المثال من الواقع المعاش في اليمن",
                  "howToApply": "طريقة توظيفه وشرحه للطلاب في الحصة"
                }
              ],
              "deliveryGuide": [
                {
                  "stepNumber": 1,
                  "stepTitle": "1. المدخل والتهيئة الحافزة",
                  "stepDetail": "إجراء المعلم لبدء الدرس",
                  "teacherScript": "ما يقوله المعلم شفوياً في الصف"
                }
              ],
              "teachingMethods": [
                {
                  "strategyName": "اسم الاستراتيجية (حوار / استقصاء / لعب أدوار / تجربة...)",
                  "executionSteps": "خطوات التطبيق العملي داخل غرفة الفصل",
                  "expectedOutcome": "الأثر التعليمي في فهم وتفاعل الطلاب"
                }
              ],
              "remediation": [
                {
                  "difficulty": "نقطة الصعوبة المتوقعة",
                  "simplerExplanation": "صياغة بديلة شديدة البساطة",
                  "visualOrPhysicalDemonstration": "توضيح عملي بأدوات بسيطة في الفصل",
                  "scaffoldQuestions": ["سؤال تدرج 1", "سؤال تدرج 2"]
                }
              ],
              "interactiveQuestions": [
                {
                  "question": "السؤال المثير للتفكير",
                  "expectedAnswer": "الإجابة النموذجية المتوقعة من الطالب",
                  "teacherFeedbackIfWrong": "كيف يصحح المعلم الخطأ دون إحباط الطالب",
                  "followUpQuestion": "سؤال تعميق الفهم التالي"
                }
              ],
              "gradedExamples": [
                {
                  "level": "مبتدئ / واقعي يمني / نشاط صفي / متوسط / متقدم وتحدي",
                  "problem": "نص المسألة أو التطبيق",
                  "stepByStepSolution": "الحل المفصل خطوة بخطوة باللغة العربية والرموز الرياضية/العلمية",
                  "pedagogicalTip": "توجيه للمعلم حول كيفية تقديم الحل على السبورة"
                }
              ],
              "blackboardPlan": "تقسيم السبورة بدقة: يمين، منتصف، يسار، والرسومات والمخططات المقترحة",
              "classroomScript": "سيناريو متكامل للحصة من التهيئة إلى الختام (ماذا يقول المعلم حرفياً)",
              "teacherEnrichment": "معلومات إثرائية تخصصية وتاريخية وتطبيقات حديثة لتعميق تمكن المعلم"
            }
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.35)
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash")
        for (model in models) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: return@use
                        val parsed = parseSmartExplanationJson(bodyString, subject, grade, lessonTitle, expansionLevel, dialectStyle)
                        if (parsed != null) return parsed
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Gemini smart explanation call failed on model $model: ${e.message}")
            }
        }
        return null
    }

    private fun parseSmartExplanationJson(
        jsonString: String,
        subject: String,
        grade: String,
        lessonTitle: String,
        expansionLevel: String,
        dialectStyle: String
    ): SmartExplanationResult? {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates")
            val textContent = if (candidates != null && candidates.length() > 0) {
                candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            } else {
                jsonString
            }

            val cleanJson = if (textContent.contains("{")) {
                val start = textContent.indexOf('{')
                val end = textContent.lastIndexOf('}')
                if (start != -1 && end != -1) textContent.substring(start, end + 1) else textContent
            } else textContent

            val obj = JSONObject(cleanJson)

            val conceptsList = mutableListOf<ConceptExplanation>()
            val conceptsArr = obj.optJSONArray("concepts")
            if (conceptsArr != null) {
                for (i in 0 until conceptsArr.length()) {
                    val c = conceptsArr.getJSONObject(i)
                    conceptsList.add(
                        ConceptExplanation(
                            title = c.optString("title", "فكرة أساسية"),
                            deepExplanation = c.optString("deepExplanation", ""),
                            whyAndHow = c.optString("whyAndHow", ""),
                            causeAndEffect = c.optString("causeAndEffect", ""),
                            commonMisconception = c.optString("commonMisconception", ""),
                            studentQuestion = c.optString("studentQuestion", ""),
                            studentQuestionAnswer = c.optString("studentQuestionAnswer", "")
                        )
                    )
                }
            }

            val analogiesList = mutableListOf<YemeniAnalogy>()
            val analogiesArr = obj.optJSONArray("yemeniAnalogies")
            if (analogiesArr != null) {
                for (i in 0 until analogiesArr.length()) {
                    val a = analogiesArr.getJSONObject(i)
                    analogiesList.add(
                        YemeniAnalogy(
                            title = a.optString("title", "تشبيه يمني"),
                            analogyText = a.optString("analogyText", ""),
                            spokenDialect = a.optString("spokenDialect", ""),
                            explanationLink = a.optString("explanationLink", "")
                        )
                    )
                }
            }

            val examplesList = mutableListOf<YemeniExample>()
            val examplesArr = obj.optJSONArray("yemeniExamples")
            if (examplesArr != null) {
                for (i in 0 until examplesArr.length()) {
                    val e = examplesArr.getJSONObject(i)
                    examplesList.add(
                        YemeniExample(
                            title = e.optString("title", "مثال من البيئة"),
                            contextType = e.optString("contextType", "البيئة اليمنية"),
                            description = e.optString("description", ""),
                            howToApply = e.optString("howToApply", "")
                        )
                    )
                }
            }

            val deliveryList = mutableListOf<DeliveryStep>()
            val deliveryArr = obj.optJSONArray("deliveryGuide")
            if (deliveryArr != null) {
                for (i in 0 until deliveryArr.length()) {
                    val d = deliveryArr.getJSONObject(i)
                    deliveryList.add(
                        DeliveryStep(
                            stepNumber = d.optInt("stepNumber", i + 1),
                            stepTitle = d.optString("stepTitle", "خطوة إلقاء"),
                            stepDetail = d.optString("stepDetail", ""),
                            teacherScript = d.optString("teacherScript", "")
                        )
                    )
                }
            }

            val methodsList = mutableListOf<TeachingMethodGuide>()
            val methodsArr = obj.optJSONArray("teachingMethods")
            if (methodsArr != null) {
                for (i in 0 until methodsArr.length()) {
                    val m = methodsArr.getJSONObject(i)
                    methodsList.add(
                        TeachingMethodGuide(
                            strategyName = m.optString("strategyName", "استراتيجية تدريس"),
                            executionSteps = m.optString("executionSteps", ""),
                            expectedOutcome = m.optString("expectedOutcome", "")
                        )
                    )
                }
            }

            val remediationList = mutableListOf<RemediationItem>()
            val remediationArr = obj.optJSONArray("remediation")
            if (remediationArr != null) {
                for (i in 0 until remediationArr.length()) {
                    val r = remediationArr.getJSONObject(i)
                    val qList = mutableListOf<String>()
                    val qArr = r.optJSONArray("scaffoldQuestions")
                    if (qArr != null) {
                        for (q in 0 until qArr.length()) {
                            qList.add(qArr.getString(q))
                        }
                    }
                    remediationList.add(
                        RemediationItem(
                            difficulty = r.optString("difficulty", ""),
                            simplerExplanation = r.optString("simplerExplanation", ""),
                            visualOrPhysicalDemonstration = r.optString("visualOrPhysicalDemonstration", ""),
                            scaffoldQuestions = qList
                        )
                    )
                }
            }

            val questionsList = mutableListOf<InteractiveQuestion>()
            val questionsArr = obj.optJSONArray("interactiveQuestions")
            if (questionsArr != null) {
                for (i in 0 until questionsArr.length()) {
                    val q = questionsArr.getJSONObject(i)
                    questionsList.add(
                        InteractiveQuestion(
                            question = q.optString("question", ""),
                            expectedAnswer = q.optString("expectedAnswer", ""),
                            teacherFeedbackIfWrong = q.optString("teacherFeedbackIfWrong", ""),
                            followUpQuestion = q.optString("followUpQuestion", "")
                        )
                    )
                }
            }

            val gradedList = mutableListOf<GradedExample>()
            val gradedArr = obj.optJSONArray("gradedExamples")
            if (gradedArr != null) {
                for (i in 0 until gradedArr.length()) {
                    val g = gradedArr.getJSONObject(i)
                    gradedList.add(
                        GradedExample(
                            level = g.optString("level", "مثال"),
                            problem = g.optString("problem", ""),
                            stepByStepSolution = g.optString("stepByStepSolution", ""),
                            pedagogicalTip = g.optString("pedagogicalTip", "")
                        )
                    )
                }
            }

            SmartExplanationResult(
                subject = subject,
                grade = grade,
                lessonTitle = lessonTitle,
                expansionLevel = expansionLevel,
                dialectStyle = dialectStyle,
                concepts = conceptsList,
                yemeniAnalogies = analogiesList,
                yemeniExamples = examplesList,
                deliveryGuide = deliveryList,
                teachingMethods = methodsList,
                remediation = remediationList,
                interactiveQuestions = questionsList,
                gradedExamples = gradedList,
                blackboardPlan = obj.optString("blackboardPlan", ""),
                classroomScript = obj.optString("classroomScript", ""),
                teacherEnrichment = obj.optString("teacherEnrichment", ""),
                isFromGeminiApi = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing smart explanation JSON: ${e.message}", e)
            null
        }
    }

    suspend fun refineExplanationConcept(
        subject: String,
        grade: String,
        lessonTitle: String,
        conceptTitle: String,
        currentExplanation: String,
        actionType: String,
        dialectStyle: String = "لهجة بيضاء يمنية",
        context: Context? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(context)
        val actionInstruction = when (actionType) {
            "EXPAND_MORE" -> "قم بتوسيع هذه الفكرة أكثر علمياً وتربوياً مع تفصيل القوانين والأسباب التاريخية والارتباطات التطبيقية."
            "SIMPLIFY" -> "اشرح هذه الفكرة بأسلوب قصصي بسيط وميسر جداً ومفردات سهلة يفهمها أصغر الطلاب فوراً."
            "YEMENI_ANALOGY" -> "أعطني تشبيهين أصيلين من واقع الحياة والبيئة اليمنية باللهجة ($dialectStyle) يبدآن بـ (تخيل يا بني زي لما...)."
            "MORE_EXAMPLES" -> "أعطني 3 مسائل وأمثلة جديدة إضافية محلولة بالكامل خطوة بخطوة من واقع بيئة الطالب اليمني."
            "WEAK_STUDENT" -> "كيف يشرح المعلم هذه الفكرة خطوة بخطوة لطالب يعاني من صعوبة بالغة في الفهم، وما هي الأسئلة التدرجية المبسطة؟"
            "PRACTICAL_EXPERIMENT" -> "اقترح تجربة صفية عملية فورية لا تتطلب أي تكلفة (أوراق، خيوط، زجاجة ماء، عملة معدنية) تثبت الفكرة أمام الطلاب."
            "VERBATIM_SCRIPT" -> "اكتب ما يقوله المعلم حرفياً للطلاب باللهجة الفصحى الميسرة مع نبرة التشويق وإشراك الطلاب."
            else -> "قدم شرحاً معمقاً لهذه الفكرة مع أمثلة وتطبيقات عملية."
        }

        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    أنت خبير تربوي وموجه أول لمناهج الجمهورية اليمنية.
                    - المادة: $subject ($grade)
                    - الدرس: $lessonTitle
                    - الفكرة: $conceptTitle
                    - الشرح الحالي: $currentExplanation
                    - المطلوب منك حصراً: $actionInstruction
                    اكتب إجابة تربوية مباشرة وغنية وممتعة للمعلم والطلاب دون مقدمات شكلية طويلة.
                """.trimIndent()

                val jsonRequest = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.4)
                    })
                }

                val requestBody = jsonRequest.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val request = Request.Builder().url(url).post(requestBody).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val resJson = JSONObject(body)
                        val text = resJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text")
                        if (text.isNotBlank()) return@withContext text
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Refine concept API call failed: ${e.message}")
            }
        }

        // Fallback response for interactive action
        when (actionType) {
            "EXPAND_MORE" -> """
                🔬 توسع علمي عميق في: ($conceptTitle):
                • الجوهر النظري: يقوم هذا المفهوم على العلاقة الرياضية/العلمية الدقيقة التي تفسر كيف تتفاعل المتغيرات.
                • التفسير السببي: لا تحدث هذه النتيجة اعتباطاً؛ بل لأن القوة/القاعدة المؤثرة تفرض توازناً مستقراً.
                • الأثر التراكمي: فهم هذا المبدأ هو الحجر الأساس الذي سيبنى عليه المقرر في الفصول القادمة في المنهج اليمني.
                • تطبيق في الهندسة والحياة: هذا القانون عينه هو المعتمد في معايرة محولات الطاقة الشمسية وحساب أحمال الشبكات المنزلية باليمن.
            """.trimIndent()
            "SIMPLIFY" -> """
                ✨ تبسيط الفكرة للأطفال والطلاب:
                «يا أولادي.. انظروا للأمر كأنه ميزان دقيق في دكان البقالة: إذا وضعت كيلوجرام سكر في الكفة اليمنى، لا يستقر الميزان حتى تضع وزنًا مطابقًا في الكفة اليسرى! كذلك هي قاعدتنا اليوم: ما يدخل في المعادلة يجب أن يقابله أثر مساوٍ له تماماً».
            """.trimIndent()
            "YEMENI_ANALOGY" -> """
                🇾🇪 تشبيهات يمنية مقربة ($dialectStyle):
                1. «تخيل يا بني زي لما تعبي خزان الماء في السطح بالدينامو.. كلما زاد ضغط الماطور ملأ الخزان أسرع، لكن لو ارتخى اللي (الأنبوب) أو ضاق، تضعف الحنفية تحت! فالجهد هو الدينامو، والمقاومة هي ضيق الأنبوب، والتيار هو تدفق الماء!».
                2. «تخيل زي باص الفرزة في باب اليمن: إذا الباص فاضي، الركاب يدخلوا ويخرجوا بسلاسة.. لكن لو الباص مليان زحمة، تصبح الحركة صعبة وبطيئة بسبب الاحتكاك والتدافع!».
            """.trimIndent()
            "MORE_EXAMPLES" -> """
                📝 أمثلة إضافية محلولة بالتفصيل:
                1. مثال بيئي (سوق الحبوب): اشترى مزارع في قاع جهران 4 أكياس ذرة بيضاء بسعر 18,000 ريال للكيس الواحد، ودفع 2,000 ريال أجور نقل. كم التكلفة الإجمالية؟
                   • الحل: (4 × 18,000) + 2,000 = 72,000 + 2,000 = 74,000 ريال يمني.
                2. مثال تطبيقي (الطاقة الشمسية): لوح شمسي قدرته 300 وات يعمل لمدة 5 ساعات ذروة مشمسة في تهامة. كم إجمالي الطاقة المنتجة؟
                   • الحل: الطاقة = القدرة × الزمن = 300 وات × 5 ساعات = 1500 وات.ساعة (1.5 كيلووات.ساعة).
                3. مثال صفي مباشر: تدرج في حل المسألة بمشاركة 3 طلاب على السبورة بالترتيب.
            """.trimIndent()
            "WEAK_STUDENT" -> """
                🤝 المعالجة الفردية للطلاب المتعثرين:
                1. ابدأ معه بسؤال ثقة سهل جداً: «كم معك في يدك اليمين؟ وكم في اليسار؟».
                2. تجنب الرموز المجردة واستخدم أشياء ملموسة (أقلام السبورة، حبات فاصوليا، ريالات ورقية).
                3. سلالم التدرج:
                   - أ: التعرف على المفهوم بالنظر.
                   - ب: محاكاة المعلم في مثال محلول.
                   - ج: حل خطوة واحدة فقط بينما يكمل المعلم باقي الخطوات.
            """.trimIndent()
            "PRACTICAL_EXPERIMENT" -> """
                🧪 تجربة صفية فورية بدون تكاليف:
                • الأدوات: ورقة كراس عادية + قلم جاف + كتاب مدرسي.
                • الإجراء: اطلب من طالبين أن يمسكا طرفي الورقة مشدودين، ثم ضع القلم في المنتصف. اطلب منهم تخفيف الشد تدريجياً وملاحظة متى يسقط القلم.
                • الاستنتاج الصفي: استنتج مع الطلاب مفهوم توازن القوى ونقطة الانهيار عملياً في دقيقتين دون الحاجة إلى معمل معقد!
            """.trimIndent()
            "VERBATIM_SCRIPT" -> """
                🗣️ ماذا تقول للطلاب حرفياً (السيناريو الصفي):
                «صباح الخير يا أبطال.. قبل أن نفتح الدفاتر، من منكم لاحظ كيف يدور عداد الطاقة الشمسية في بيته وقت الظهيرة مقارنة بوقت المغرب؟ (انتظر ثوانٍ لسماع الإجابات).. ممتاز يا أحمد! هذا الاختلاف ليس صدفة، بل هو السر العظيم لدرسنا اليوم.. ركزوا معي في هذا الشكل على السبورة..».
            """.trimIndent()
            else -> "تم توسيع الفكرة وشرحها بنجاح."
        }
    }

    private fun createCurriculumFallbackSmartExplanation(
        subject: String,
        grade: String,
        unitName: String,
        lessonTitle: String,
        lessonContentText: String,
        expansionLevel: String,
        dialectStyle: String,
        studentLevel: String,
        examplesCount: Int
    ): SmartExplanationResult {
        val cleanSubject = subject.trim()
        val isMath = cleanSubject.contains("رياضيات") || cleanSubject.contains("جبر") || cleanSubject.contains("تفاضل") || cleanSubject.contains("هندسة")
        val isScience = cleanSubject.contains("علوم") || cleanSubject.contains("فيزياء") || cleanSubject.contains("كيمياء") || cleanSubject.contains("أحياء")
        val isArabic = cleanSubject.contains("عربي") || cleanSubject.contains("لغة") || cleanSubject.contains("نحو") || cleanSubject.contains("قراءة")
        val isIslamic = cleanSubject.contains("إسلام") || cleanSubject.contains("قرآن") || cleanSubject.contains("توحيد") || cleanSubject.contains("فقه") || cleanSubject.contains("حديث")

        val concepts = mutableListOf<ConceptExplanation>()
        val analogies = mutableListOf<YemeniAnalogy>()
        val yemeniExamples = mutableListOf<YemeniExample>()
        val deliverySteps = mutableListOf<DeliveryStep>()
        val teachingMethods = mutableListOf<TeachingMethodGuide>()
        val remediation = mutableListOf<RemediationItem>()
        val questions = mutableListOf<InteractiveQuestion>()
        val graded = mutableListOf<GradedExample>()

        if (isMath) {
            concepts.add(
                ConceptExplanation(
                    title = "بناء المفهوم الرياضي والقاعدة الأساسية",
                    deepExplanation = "يقوم درس ($lessonTitle) على العلاقة المنطقية بين المقادير والعمليات الحسابية، حيث يتم الانتقال من التمثيل المحسوس إلى شبه المحسوس ثم الرمز المجرد لترسيخ المفهوم الرياضي دون تلقين أعمى.",
                    whyAndHow = "تتحقق القاعدة لأن العمليات الرياضية تحفظ التكافؤ؛ فإذا أجرينا أي تعديل على أحد الطرفين يجب أن يقابله تعديل موازن في الطرف الآخر لصيانة المساواة.",
                    causeAndEffect = "إتقان هذه المهارة يجنب الطالب الوقوع في أخطاء الإشارات وحذف الحدود بطريقة غير مبررة في المعادلات المتقدمة.",
                    commonMisconception = "اعتقاد الطلاب أن التغيير في موضع الأعداد أو نقل الحدود يغير قيمتها دون تغيير الإشارة أو العملية المعاكسة.",
                    studentQuestion = "أستاذ.. لماذا لا يمكننا الجمع المباشر دون توحيد المقامات أو فك الأقواس؟",
                    studentQuestionAnswer = "لأننا نجمع أشياء من نفس الجنس والقيمة؛ تماماً مثلما لا نجمع 3 كراتين طماطم مع 2 كراتين بصل لنقول 5 كراتين طماطم!"
                )
            )
            concepts.add(
                ConceptExplanation(
                    title = "إجراءات التحليل وحل المسألة متعددة الخطوات",
                    deepExplanation = "تتطلب المسألة قراءة واعية للمسألة اللفظية، استخراج المعطيات والمطلوب، وضع خطة الحل (النمذجة الرياضية)، والتنفيذ الدقيق مع التحقق من معقولية الناتج.",
                    whyAndHow = "يتم التحقق بالعكس؛ أي باستخدام العملية العكسية (الطرح عكس الجمع، والقسمة عكس الضرب، والتكامل عكس التفاضل).",
                    causeAndEffect = "إهمال التحقق يؤدي إلى تراكم أخطاء الإشارات والعمليات في الامتحانات الوزارية والنهائية.",
                    commonMisconception = "التسرع في كتابة الناتج دون كتابة خطوات التبرير الرياضي التي يُحاسب عليها الطالب في سلم التصحيح.",
                    studentQuestion = "هل أنقص درجات لو كتبت الناتج النهائي صح بدون تفصيل الخطوات؟",
                    studentQuestionAnswer = "نعم، ففي الاختبارات الوزارية اليمنية 80% من درجات المسألة مخصصة لخطوات التبرير والقانون والتعويض!"
                )
            )

            analogies.add(
                YemeniAnalogy(
                    title = "المعادلة الرياضية وتوازن الكفتين",
                    analogyText = "تخيل يا بني زي لما توزن في دكان سوق الملح بصنعاء.. الميزان ذو الكفتين لو زدت رطل بن في اليمين لازم تزيد صنجة في الشمال عشان يظل الميزان مستوي! هكذا علامة (=) تماماً، ما تفعله بالطرف الأيمن افعله بالأيسر.",
                    spokenDialect = "«زي ميزان البقال حق الحارة.. ما يسبر يوزن لك طماط إلا لما الكفتين يتساوين تماماً»",
                    explanationLink = "يربط التشبيه خاصية التساوي ونقل الحدود مع تغيير الإشارة بصرياً بميزان الكفتين المألوف لكل طالب."
                )
            )

            yemeniExamples.add(
                YemeniExample(
                    title = "حساب عوائد محصول البن الحرازي",
                    contextType = "الزراعة اليمنية وسوق المحاصيل",
                    description = "مزارع في جبال حراز حصد 85 كيلوجرام من البن المجفف الممتاز، باع الكيلو بسعر 6,500 ريال يمني بعد خصم 10% تكاليف تجهيز وتغليف. احسب صافي دخل المزارع.",
                    howToApply = "تطبيق العمليات الحسابية والنسبة المئوية في سياق افتخار الطلاب بالمنتجات الوطنية اليمنية."
                )
            )
            yemeniExamples.add(
                YemeniExample(
                    title = "حساب سعة وتكلفة خزان المياه المنزلي",
                    contextType = "الحياة اليومية - خزانات المياه (الوايتات)",
                    description = "خزان أسطواني بقطر 2 متر وارتفاع 3 أمتار، احسب حجمه بالمتر المكعب وكم وايت ماء يلزمه لتعبئته كاملاً إذا كان سعة الوايت 6 أمتار مكعبة.",
                    howToApply = "ربط الهندسة الفراغية وحساب الحجوم بمشكلة المياه اليومية التي يعيشها الطالب وأسرته."
                )
            )
        } else if (isScience) {
            concepts.add(
                ConceptExplanation(
                    title = "المبدأ العلمي والظاهرة الطبيعية في المنهج",
                    deepExplanation = "يتناول درس ($lessonTitle) القوانين الفيزيائية والكيميائية والحيوية التي تفسر سلوك المادة والطاقة في الطبيعة، مع التركيز على الملاحظة والقياس والاستنتاج العلمي الرصين.",
                    whyAndHow = "تحدث الظاهرة بفعل قوى الجذب أو التفاعلات الكيميائية أو انتقال الطاقة الحرارية والكهربائية وفق قوانين حفظ المادة والطاقة.",
                    causeAndEffect = "أي تغير في درجة الحرارة أو الضغط أو التركيز يزيح التوازن ويغير من سرعة العمليات الحيوية أو التفاعلات الفيزيائية.",
                    commonMisconception = "الخلط بين مفهومي الكتلة والوزن، أو الاعتقاد بأن الحرارة مادة تتدفق وليست صورة من صور الطاقة الحركية للجزيئات.",
                    studentQuestion = "أستاذ.. لماذا لا نشعر بضغط الهواء الجوي الهائل الواقع على رؤوسنا وأجسامنا؟",
                    studentQuestionAnswer = "لأن ضغط السوائل والغازات داخل أجسامنا يعادل الضغط الجوي الخارجي تماماً؛ مثل بالون منفوخ في الهواء المتوازن!"
                )
            )

            analogies.add(
                YemeniAnalogy(
                    title = "الدائرة الكهربائية ومضخة المياه المنزلية",
                    analogyText = "تخيل يا بني منظومة الطاقة الشمسية في بيتكم: اللوح الشمسي على السطح يولد الجهد مثل الدينامو، والبطارية تخزن الشحنات مثل الخزان، والسلك مثل الأنبوب.. والمحول (الإنفرتر) ينظم التدفق لتشغيل اللمبة والتلفزيون!",
                    spokenDialect = "«زي الماطور والدينامو حق الماء.. كلما قوي الماطور دفع الماء في الأنابيب بقوة أسرع، والسلك الكهربائي مثل اللي النحيف!»",
                    explanationLink = "تبسيط مفاهيم فرق الجهد الكهربائي وشدة التيار والمقاومة بأجهزة يعرفها كل منزل يمني."
                )
            )

            yemeniExamples.add(
                YemeniExample(
                    title = "منظومات الطاقة الشمسية والبطاريات المنزلية في اليمن",
                    contextType = "الطاقة الشمسية وتطبيقات الكهرباء اليومية",
                    description = "دراسة كفاءة تحويل الطاقة الشمسية إلى طاقة كيميائية في بطاريات الجل وتأثير حرارة الصيف في تهامة أو برودة شتاء ذمار على كفاءة البطارية.",
                    howToApply = "شرح العوامل المؤثرة في المقاومة الداخلية وسرعة التفاعلات الكيميائية للبطاريات من واقع تجربة الطلاب المنزلية."
                )
            )
            yemeniExamples.add(
                YemeniExample(
                    title = "مدرجات جبال حراز وإب وحماية التربة من الانجراف",
                    contextType = "الجغرافيا والبيئة الزراعية اليمنية",
                    description = "كيف استغل الأجداد اليمنيون هندسة المدرجات الجبلية لتقليل سرعة تدفق مياه السيول ومنع انجراف التربة الخصبة وزيادة تغذية المياه الجوفية.",
                    howToApply = "تطبيق قوانين الجاذبية الأرضية والطاقة الحركية للسيول ودور الزراعة في التوازن البيئي."
                )
            )
        } else if (isArabic) {
            concepts.add(
                ConceptExplanation(
                    title = "المعنى الوظيفي للقاعدة النحوية والبلاغية",
                    deepExplanation = "القواعد النحوية في لغتنا العربية لم توضع للتعقيد، بل لحفظ المعنى وصيانة اللسان؛ فالإعراب هو 'الإبانة والإيضاح'، والحركة على آخر الكلمة هي العلامة التي تحدد الفاعل من المفعول به في المعنى.",
                    whyAndHow = "تتغير العلامة الإعرابية بتغير العامل الداخل على الكلمة وموقعها المعنوي في التركيب الإسنادي.",
                    causeAndEffect = "إهمال الضبط يفسد المعنى تماماً؛ فقد يتحول الفاعل إلى مفعول أو يتغير الحكم الشرعي في الآيات القرآنية.",
                    commonMisconception = "حفظ الإعراب حفظاً آلياً دون فهم المعنى وسياق الجملة وموقع الفاعل الحقيقي.",
                    studentQuestion = "لماذا يجب أن نعرف الفاعل والمفعول طالما المعنى مفهوم في كلامنا العامي؟",
                    studentQuestionAnswer = "لأن لغة القرآن والشعر العربي تتيح التقديم والتأخير للبلاغة؛ فالعلامة الإعرابية هي التي تكشف لك من ضرب من، ومن رحم من!"
                )
            )

            analogies.add(
                YemeniAnalogy(
                    title = "الحركات الإعرابية مثل لوحات المرور في فرزة النقل",
                    analogyText = "تخيل يا بني الحركات الإعرابية زي إشارات رجل المرور في جولة كنتاكي أو التحرير: الضمة تقول للكلمة أنتِ الرئيس والفاعل تقدمي! والفتحة تقول للكلمة وقع عليك الفعل! والكسرة تسحبها بحرف الجر زي الحبل!",
                    spokenDialect = "«زي لما تنادي صاحبك في الفرزة.. بنبرة صوتك يفهم هل أنت تأمره أو تسأله أو تعاتبه.. الحركات هي نبرة لغة الضاد!»",
                    explanationLink = "ربط وظيفة الحركات الإعرابية الإشارية بالدلالة المعنوية وتوجيه الإسناد في الجملة."
                )
            )

            yemeniExamples.add(
                YemeniExample(
                    title = "شواهد من ديوان البردوني والأمثال الشعبية اليمنية",
                    contextType = "الأدب والتراث اللغوي اليمني الأصيل",
                    description = "توظيف أبيات الشاعر اليمني الكبير عبدالله البردوني أو مطهر الإرياني لاستخراج القواعد النحوية والصور البيانية والبلاغية.",
                    howToApply = "قراءة النص بنبرة شعرية ملهمة ومطالبة الطلاب باكتشاف جماليات التقديم والتأخير والاستعارة."
                )
            )
        } else {
            // General / Islamic / Social Studies
            concepts.add(
                ConceptExplanation(
                    title = "القيم التربوية والمفاهيم الجوهرية للدرس",
                    deepExplanation = "يهدف درس ($lessonTitle) إلى ترسيخ الوعي القيمي والمعرفي والسلوكي في شخصية الطالب اليمني، وربط المعرفة التاريخية أو الفقهية أو الاجتماعية بواقع الحياة اليومية وبناء المجتمع.",
                    whyAndHow = "يتحقق التعلم العميق من خلال استنباط المقاصد الكلية للأحكام والأحداث وربطها بسلوك الطالب في البيت والمدرسة والمجتمع.",
                    causeAndEffect = "تطبيق هذه المفاهيم ينشئ جيلاً متعلماً واعياً متمسكاً بأصالته وهويته وقادراً على الإسهام في نهضة وطنه.",
                    commonMisconception = "التعامل مع المادة كمجرد نصوص للحفظ من أجل الامتحان دون استشعار أثرها العملي في السلوك.",
                    studentQuestion = "كيف أطبق هذا الحكم أو المعنى في تعاملي مع زملائي في الحارة والمدرسة؟",
                    studentQuestionAnswer = "بالصدق في القول، والأمانة في التعامل، والتعاون مع الزملاء ومساعدة المحتاج وبر الوالدين."
                )
            )

            analogies.add(
                YemeniAnalogy(
                    title = "بناء الشخصية والقيم مثل بناء البيت الحجري الصنعاني",
                    analogyText = "تخيل يا بني البيت الصنعاني الشامخ.. حجارته السفلية صلبة ومرصوصة بدقة (وهي القيم والعقيدة)، ونوافذه القمرية الملونة تدخل النور (وهي العلم والأخلاق)! إذا اهتز الأساس تصدع البيت كله.",
                    spokenDialect = "«زي الحجر الحبش الأسود في ساس البيت.. يمسك المبنى مئات السنين ضد السيول والزلازل.. هكذا الأخلاق والأمانة في صدر الإنسان!»",
                    explanationLink = "ربط تماسك الشخصية المسلمة المتوازنة بصلابة العمارة الطينية والحجرية اليمنية التاريخية."
                )
            )

            yemeniExamples.add(
                YemeniExample(
                    title = "عادة الغرم والتعاون المجتمعي في القرى والمدن اليمنية",
                    contextType = "التكافل الاجتماعي والتراث الإنساني اليمني",
                    description = "مفهوم 'الغرم' و'النجدة' و'الفزعة' عند جني المحاصيل وبناء الطرقات في القرى، وتطبيقه على التعاون الصفي ومساندة الزملاء.",
                    howToApply = "توجيه نقاش صفي حول صور التكافل في الحي ومساندة الأسر المتعففة وتطوع الطلاب لنظافة المدرسة."
                )
            )
        }

        // Delivery 10 steps
        deliverySteps.add(DeliveryStep(1, "1. المدخل والتهيئة الحافزة (5 دقائق)", "استثارة فضول الطلاب بربط موضوع الدرس بحدث واقعي أو لغز صفي شيق.", "«صباح الخير يا أبنائي.. قبل أن أكتب كلمة واحدة على السبورة، من منكم يخبرني ما الذي يحدث عندما...؟»"))
        deliverySteps.add(DeliveryStep(2, "2. السؤال المثير للتفكير المحوري", "طرح السؤال الجوهري الذي يدور حوله الدرس لتوليد شغف الاستكشاف.", "«لو توقفت هذه القاعدة عن العمل لدقيقة واحدة في الكون، كيف ستتغير حياتنا؟ فكروا بصمت لمدة نصف دقيقة!»"))
        deliverySteps.add(DeliveryStep(3, "3. الشرح المنطوق المبسط (نبرة ودية)", "عرض الفكرة بلغة واضحة متدرجة ومخاطبة عقل الطالب مباشرة.", "«القاعدة ببساطة تتلخص في ثلاث كلمات محددة: السبب، الإجراء، والنتيجة الحتمية..»"))
        deliverySteps.add(DeliveryStep(4, "4. المثال الواقعي من البيئة اليمنية", "تقديم المثال المعاش من السوق أو الزراعة أو المنزل لكسر الجمود.", "«زي ما بنشوف كل يوم في سوق الباب الكبير أو دكاكين الحارة عند وزن السلع وتوزيعها..»"))
        deliverySteps.add(DeliveryStep(5, "5. التشبيه البصري المقرب (قرّب المعلومة)", "سرد التشبيه باللهجة المحببة لنقل المفهوم من التجريد للتصور الحسي.", "«تخيل يا بني زي لما تعبي خزان الماء في السطح بالدينامو.. السلك مثل الأنبوب والجهد مثل ضغط الماطور!»"))
        deliverySteps.add(DeliveryStep(6, "6. الرسم التوضيحي وتخطيط السبورة", "رسم مخطط سهمي أو جدول مقارنة بألوان مميزة يسهل تدوينه.", "رسم جدول بثلاث خانات: المدخلات، العمليات، والمخرجات مع أسهم ملونة تدل على الاتجاه."))
        deliverySteps.add(DeliveryStep(7, "7. النشاط والتفاعل الصفي السريع (دقيقتان)", "تكليف كل طالبين متجاورين (فكر - زاوج - شارك) بحل جزئية فورية.", "«التفت لزميلك في نفس المقعد، وتأكد أنكما متفقان على حل الخطوة الأولى في نصف دقيقة!»"))
        deliverySteps.add(DeliveryStep(8, "8. سؤال التحقق من الفهم (تقويم تكويني)", "سؤال مفصلي يقيس هل وصلت المعلومة للجميع قبل الانتقال للخطوة التالية.", "«من يفسر لي بكلماته الخاصة لماذا وضعنا إشارة الموجب هنا ولم نضع السالب؟»"))
        deliverySteps.add(DeliveryStep(9, "9. المفهوم الخاطئ والتحذير منه", "كتابة الخطأ الشائع على السبورة ووضع علامة خطأ حمراء عليه.", "«احذروا من هذا الخطأ الذي يقع فيه أغلب الطلاب في الامتحانات الوزارية: لا تنقل بدون تغيير الإشارة!»"))
        deliverySteps.add(DeliveryStep(10, "10. المعالجة الفورية في حال التعثر", "إجراء تدخلي بديل للطلاب الذين تبدو عليهم علامات الحيرة.", "«إذا شعرت أنك مشوش، انظر فقط إلى الخطوة الأولى في الدفتر وطبقها على رقمين أصغر أولاً!»"))

        // Teaching Methods
        teachingMethods.add(TeachingMethodGuide("استراتيجية الحوار والمناقشة السقراطية", "يبدأ المعلم بتوجيه أسئلة متدرجة تستنطق الإجابة من عقول الطلاب دون إعطائهم الحل جاهزاً.", "تحفيز التفكير النقدي والثقة بالنفس لدى الطالب."))
        teachingMethods.add(TeachingMethodGuide("استراتيجية الاستقصاء والاكتشاف الموجه", "عرض مسألة أو ظاهرة ومطالبة المجموعات الطلابية باكتشاف القاعدة الحاكمة بأنفسهم.", "بقاء أثر التعلم في الذاكرة طويلة المدى."))
        teachingMethods.add(TeachingMethodGuide("استراتيجية النمذجة والتطبيق العملي", "حل المعلم لمثال نموذجي بصوت مرتفع يعبر فيه عن طريقة تفكيره الذهنية (التفكير بصوت عالٍ).", "اكتساب الطالب لمنهجية التفكير والتحليل المنظم."))

        // Remediation
        remediation.add(
            RemediationItem(
                difficulty = "صعوبة الانتقال من المفهوم النظري إلى التطبيق العملي",
                simplerExplanation = "استبدال المصطلحات العلمية بمصطلحات حسية من حياة الطالب اليومية.",
                visualOrPhysicalDemonstration = "استخدام أقلام السبورة وأصابع اليد لتمثيل المقادير والعلاقات بصرياً على المنصة.",
                scaffoldQuestions = listOf("ما الذي نراه أولاً في المسألة؟", "ما هو الرقم المعروف وما هو الرقم المجهول؟", "أي عملية نحتاجها للوصول للمجهول؟")
            )
        )

        // Interactive questions
        questions.add(
            InteractiveQuestion(
                question = "لو غيرنا معطيات المسألة وضاعفنا القيمة الأولى، هل تتضاعف النتيجة أم تظل ثابتة؟ ولماذا؟",
                expectedAnswer = "تتضاعف النتيجة لأن العلاقة بينهما علاقة طردية مباشرة.",
                teacherFeedbackIfWrong = "«فكرتك قريبة يا بني.. لكن انظر لمعامل التناسب في البسط، ألا ترى أنه ينمو مع زيادة القيمة؟ أحسنت المحاولة!»",
                followUpQuestion = "وماذا يحدث لو كانت القيمة في المقام بدلاً من البسط؟"
            )
        )
        questions.add(
            InteractiveQuestion(
                question = "كيف تثبت لصديقك صحة هذه القاعدة لو اختلف معك فيها خارج الفصل؟",
                expectedAnswer = "بالتعويض العددي المباشر أو بإجراء التجربة العملية المعاكسة.",
                teacherFeedbackIfWrong = "«الاحتكام للرأي الشخصي لا يفيد في العلم، ما هو الدليل التجريبي أو الرياضي القاطع؟»",
                followUpQuestion = "أعطني مثالاً عددياً سريعاً يثبت ذلك في ثوانٍ."
            )
        )

        // Graded Examples
        graded.add(
            GradedExample(
                level = "مبتدئ ومباشر (بناء الثقة)",
                problem = "تطبيق فوري مباشر للقاعدة باستخدام أرقام صغيرة ومألوفة.",
                stepByStepSolution = "الخطوة 1: كتابة القانون بدقة.\nالخطوة 2: التعويض المباشر عن المعطيات.\nالخطوة 3: استخراج الناتج مع كتابة وحدة القياس أو التمييز.",
                pedagogicalTip = "اطلب من طالب متوسط أو خجول قراءة خطوات الحل بصوت واضح لتعزيز ثقته بنفسه."
            )
        )
        graded.add(
            GradedExample(
                level = "واقعي من البيئة اليمنية",
                problem = "مسألة تحاكي التجارة أو الزراعة أو الطاقة الشمسية في اليمن.",
                stepByStepSolution = "تحديد معطيات المسألة من النص اللفظي -> صياغة المعادلة -> تنفيذ الحسابات بدقة -> كتابة الاستنتاج النهائي بالريال أو الكيلوجرام.",
                pedagogicalTip = "أكد على الطلاب أن الرياضيات والعلوم هي لغة الحياة اليومية وليست مجرد صفحات في الكتاب."
            )
        )
        graded.add(
            GradedExample(
                level = "متقدم وتحدي للمتميزين",
                problem = "مسألة وزارية مركبة تتطلب الربط بين قاعدتين واستخدام مهارات تفكير عليا.",
                stepByStepSolution = "التحليل الاستراتيجي: فك المسألة إلى جزأين فرعيين -> حل الجزء الأول -> استخدام ناتجه كمعطى لحل الجزء الثاني -> التحقق من المنطق الرياضي/العلمي.",
                pedagogicalTip = "اترك الطلاب يفكرون في حلها كمسابقة بين المجموعات الصفية لمدة 3 دقائق قبل تقديم أي تلميح."
            )
        )

        val blackboardPlan = """
            📋 تخطيط وتنظيم السبورة الصفية لدرس ($lessonTitle):
            ┌────────────────────────┬───────────────────────────────────┬────────────────────────┐
            │ الجانب الأيمن (المفاهيم) │ المنتصف (الشرح والرسومات التوضيحية) │ الجانب الأيسر (الأمثلة) │
            ├────────────────────────┼───────────────────────────────────┼────────────────────────┤
            │ • عنوان الدرس والتاريخ │ • رسم المخطط السهمي للظاهرة      │ • مسألة تطبيقية 1      │
            │ • الأهداف الأساسية    │ • تفكيك خطوات القاعدة خطوة بخطوة │ • مسألة التحدي والواجب  │
            │ • المصطلحات العلمية    │ • التحذير من الخطأ الشائع (بالأحمر)│ • ملخص النقاط الذهبية  │
            └────────────────────────┴───────────────────────────────────┴────────────────────────┘
        """.trimIndent()

        val classroomScript = """
            ⏱️ سيناريو إلقاء الحصة كاملاً (45 دقيقة):
            • [00:00 - 05:00] التهيئة: الدخول بابتسامة، كتابة التاريخ والبسملة، إلقاء لغز أو سؤال مثير من البيئة يربط الدرس السابق بالجديد.
            • [05:00 - 15:00] العرض وبناء المفهوم: شرح الفكرة الأولى، عرض التشبيه اليمني المقرب («زي ميزان سوق الملح..»)، رسم المخطط على السبورة.
            • [15:00 - 25:00] النمذجة وحل الأمثلة: حل المثال الأول بصوت مسموع، إشراك الطلاب في استنتاج الخطوة الثانية، تنبيههم للخطأ الشائع.
            • [25:00 - 35:00] التفاعل والنشاط الصفي: تطبيق نشاط المجموعات (فكر - زاوج - شارك)، تجول المعلم بين الصفوف لتقديم الدعم الفردي للضعاف.
            • [35:00 - 42:00] التقويم الختامي: توجيه أسئلة التحقق من الفهم لثلاثة طلاب بمستويات مختلفة للتأكد من استيعاب الصف بأكمله.
            • [42:00 - 45:00] الغلق والتكليف: تلخيص الفكرة في جملة واحدة ذهبية، وتحديد أرقام تمارين الواجب المنزلي بدقة في الكتاب المدرسي.
        """.trimIndent()

        val teacherEnrichment = """
            💡 معلومات إثرائية لتعميق تمكن المعلم وثقافته التخصصية:
            1. الجذور التاريخية: ارتبط اكتشاف هذه القاعدة بالعلماء المسلمين الأوائل كالخوارزمي وابن الهيثم والبيروني، الذين أسسوا المنهج التجريبي قبل أوروبا بقرون.
            2. التطبيقات المعاصرة: هذا المبدأ هو الأساس البرمجي والفيزيائي الذي تعتمد عليه خوارزميات الذكاء الاصطناعي وهندسة الأقمار الصناعية والألواح الشمسية الحديثة.
            3. نصيحة للموجه التربوي: لا تنتقل من المفهوم الحسي إلى المفهوم الرمزي المجرد حتى تتأكد من أن 80% على الأقل من طلاب الصف أدركوا الصورة الحركية الذهنية للمفهوم.
        """.trimIndent()

        return SmartExplanationResult(
            subject = cleanSubject,
            grade = grade,
            lessonTitle = lessonTitle,
            expansionLevel = expansionLevel,
            dialectStyle = dialectStyle,
            concepts = concepts,
            yemeniAnalogies = analogies,
            yemeniExamples = yemeniExamples,
            deliveryGuide = deliverySteps,
            teachingMethods = teachingMethods,
            remediation = remediation,
            interactiveQuestions = questions,
            gradedExamples = graded,
            blackboardPlan = blackboardPlan,
            classroomScript = classroomScript,
            teacherEnrichment = teacherEnrichment,
            isFromGeminiApi = false
        )
    }
}
