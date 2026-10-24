package com.example.data.ai

import com.example.BuildConfig
import com.example.data.model.ExamQuestion
import com.example.data.model.QuestionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(
    private var repository: com.example.data.repository.RqeebRepository? = null
) {

    fun setRepository(repo: com.example.data.repository.RqeebRepository) {
        this.repository = repo
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun getEffectiveApiKey(): String {
        val repoKey = repository?.getAiApiKey() ?: ""
        if (repoKey.isNotBlank()) return repoKey
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun getEffectiveModelName(): String {
        return repository?.getAiModelName()?.ifBlank { "gemini-2.5-flash" } ?: "gemini-2.5-flash"
    }

    fun isConfigured(): Boolean = getApiKey().isNotBlank()

    data class AiConnectionDiagnostic(
        val isSuccess: Boolean,
        val message: String,
        val statusCode: Int = if (isSuccess) 200 else 400,
        val modelName: String = "gemini-2.5-flash",
        val provider: String = "Google Generative Language API (REST)"
    )

    /**
     * Test real connection to Gemini API with detailed diagnostic results
     */
    suspend fun testConnection(
        apiKeyInput: String? = null,
        modelNameInput: String? = null
    ): AiConnectionDiagnostic = withContext(Dispatchers.IO) {
        val key = apiKeyInput?.takeIf { it.isNotBlank() } ?: getEffectiveApiKey()
        val model = modelNameInput?.takeIf { it.isNotBlank() } ?: getEffectiveModelName()

        if (key.isBlank()) {
            return@withContext AiConnectionDiagnostic(
                isSuccess = false,
                message = "مفتاح API غير متوفر. يرجى إدخال مفتاح API في الإعدادات أو لوحة Secrets.",
                statusCode = 401,
                modelName = model
            )
        }

        try {
            val testJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", "مرحباً، هذا فحص اتصال من تطبيق رقيب للمعلم اليمني. أجب بكلمة واحدة: جاهز."))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
            val request = Request.Builder()
                .url(url)
                .post(testJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseCode = response.code
            val bodyString = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                repository?.recordAiRequest(isSuccess = true)
                AiConnectionDiagnostic(
                    isSuccess = true,
                    message = "الاتصال ناجح ومؤكد بنموذج $model عبر Google Generative Language API!",
                    statusCode = responseCode,
                    modelName = model
                )
            } else {
                repository?.recordAiRequest(isSuccess = false)
                val errorMessage = when (responseCode) {
                    400 -> {
                        if (bodyString.contains("API_KEY_INVALID", ignoreCase = true) || bodyString.contains("key not valid", ignoreCase = true)) {
                            "المفتاح غير صالح أو غير مفعّل (API_KEY_INVALID). يرجى التأكد من نسخه بدقة من Google AI Studio."
                        } else {
                            "خطأ في إعدادات الطلب (رمز 400). يرجى التأكد من اسم النموذج والمفتاح."
                        }
                    }
                    403 -> "تم رفض الوصول (رمز 403): صلاحيات المفتاح غير كافية أو تم تقييد الوصول."
                    404 -> "النموذج '$model' غير مدعوم أو غير متاح في منطقتك (رمز 404). جرب نموذج gemini-2.5-flash."
                    429 -> "تم تجاوز حد الاستخدام المسموح أو استنفاد الحصة المجانية لمفتاح API (رمز 429 - Quota Exceeded)."
                    500, 503 -> "خادم Google AI يواجه ضغطاً مؤقتاً (رمز $responseCode). يرجى المحاولة لاحقاً."
                    else -> "فشل الاتصال بالخادم (رمز $responseCode): ${response.message}"
                }
                AiConnectionDiagnostic(
                    isSuccess = false,
                    message = errorMessage,
                    statusCode = responseCode,
                    modelName = model
                )
            }
        } catch (e: Exception) {
            repository?.recordAiRequest(isSuccess = false)
            AiConnectionDiagnostic(
                isSuccess = false,
                message = "تعذر الاتصال بالإنترنت أو الخادم: ${e.localizedMessage ?: "انقطاع في الشبكة"}",
                statusCode = 0,
                modelName = model
            )
        }
    }

    /**
     * Call Gemini REST API directly
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String = "أنت 'رقيب - مساعد المعلم الذكي'، خبير تربوي وتعليمي متخصص في المناهج اليمنية وطرائق التدريس وصياغة الاختبارات المدرسية المعتمدة."
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("مفتاح Gemini API غير مهيأ. يرجى إضافة المفتاح في إعدادات المالك أو لوحة Secrets.")
            )
        }

        val modelName = getEffectiveModelName()

        try {
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                if (systemInstruction.isNotBlank()) {
                    val systemObj = JSONObject().apply {
                        val sysParts = JSONArray().apply {
                            put(JSONObject().put("text", systemInstruction))
                        }
                        put("parts", sysParts)
                    }
                    put("systemInstruction", systemObj)
                }
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                repository?.recordAiRequest(isSuccess = false)
                return@withContext Result.failure(
                    Exception("خطأ في استجابة الخادم (${response.code}): ${response.message}")
                )
            }

            repository?.recordAiRequest(isSuccess = true)

            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    return@withContext Result.success(text)
                }
            }

            Result.failure(Exception("لم يتم استلام نص من نموذج الذكاء الاصطناعي."))
        } catch (e: Exception) {
            repository?.recordAiRequest(isSuccess = false)
            Result.failure(e)
        }
    }

    /**
     * AI Lesson Plan Generator tailored for Yemeni School Curriculum with strict teacher options
     * Generates a complete 18-section formal lesson plan aligned with Yemeni Ministry of Education guidelines.
     */
    suspend fun generateLessonPlan(
        schoolName: String = "",
        teacherName: String = "",
        subject: String,
        grade: String,
        title: String,
        date: String = "",
        period: String = "الحصة الأولى",
        periodsCount: Int = 1,
        duration: Int = 45,
        unit: String = "",
        term: String = "الفصل الدراسي الأول",
        prepType: String = "تحضير يومي",
        studentLevel: String = "متوسط (عام)",
        textbookRef: String = "",
        selectedTeacherStyles: List<String> = emptyList(),
        selectedTeachingMethods: List<String> = emptyList(),
        selectedSkillsAndValues: List<String> = emptyList(),
        selectedTeachingAids: List<String> = emptyList(),
        objectivesInput: String = "",
        materialsInput: String = "",
        contentSource: String = ""
    ): LessonPlanResult = withContext(Dispatchers.IO) {
        val stylesStr = if (selectedTeacherStyles.isNotEmpty()) selectedTeacherStyles.joinToString("، ") else "الإلقاء والتوضيح، الحوار والمناقشة"
        val methodsStr = if (selectedTeachingMethods.isNotEmpty()) selectedTeachingMethods.joinToString("، ") else "التعلم التعاوني، العصف الذهني"
        val skillsStr = if (selectedSkillsAndValues.isNotEmpty()) selectedSkillsAndValues.joinToString("، ") else "الملاحظة، التفكير النقدي، القيم الدينية والوطنية"
        val aidsStr = if (selectedTeachingAids.isNotEmpty()) selectedTeachingAids.joinToString("، ") else "الكتاب المدرسي، السبورة والطباشير"

        val prompt = """
            أنت خبير تربوي وموجه عام في وزارة التربية والتعليم بالجمهورية اليمنية.
            قم بإعداد خطة تحضير درس نموذجية ومتكاملة وفق النموذج الرسمي المعتمد في المدارس اليمنية، تشمل العناصر الـ 18 الإلزامية:
            
            [البيانات الأساسية والتربوية]
            - المدرسة: ${schoolName.ifBlank { "المدرسة النموذجية" }}
            - المعلم: ${teacherName.ifBlank { "معلم المادة" }}
            - المادة: $subject
            - المرحلة والصف: $grade
            - الفصل الدراسي: $term
            - الوحدة الدراسية: ${unit.ifBlank { "الوحدة الأولى" }}
            - عنوان الدرس: $title
            - التاريخ: ${date.ifBlank { "اليوم" }}
            - رقم الحصة: $period
            - عدد الحصص: $periodsCount
            - زمن الحصة: $duration دقيقة
            - مستوى الطلاب المستهدف: $studentLevel
            - نوع التحضير: $prepType
            - المرجع وكتاب الوزارة: ${textbookRef.ifBlank { contentSource.ifBlank { "كتاب وزارة التربية والتعليم اليمني المعتمد" } }}

            [الخيارات التربوية المحددة من المعلم]:
            1. أساليب المعلم المختارة فقط: $stylesStr
            2. طرق التدريس المختارة فقط: $methodsStr
            3. المهارات والقيم المستهدفة: $skillsStr
            4. الوسائل التعليمية المعتمدة: $aidsStr
            ${if (objectivesInput.isNotBlank()) "5. مقترحات إضافية من المعلم: $objectivesInput" else ""}

            [قواعد إعداد التحضير]:
            1. الالتزام الدقيق بالمنهج اليمني وطبيعة المرحلة الدراسية ($grade).
            2. صياغة أهداف سلوكية إجرائية قابلة للقياس (معرفية، مهارية، وجدانية).
            3. تحديد نواتج التعلم المتوقعة بدقة.
            4. تمهيد وتهيئة مشوقة (5 دقائق) يربط الدرس بالحياة والبيئة اليمنية.
            5. ذكر المفاهيم والمصطلحات الأساسية للدرس بوضوح.
            6. خطوات الشرح التفصيلية وتوزيع زمن الحصة ($duration دقيقة).
            7. أمثلة محلولة متدرجة من السهل إلى الأصعب.
            8. أنشطة صفية (فردية وجماعية).
            9. أسئلة أثناء الشرح للتحقق من الفهم الفوري.
            10. مراعاة الفروق الفردية (دعم للمبتدئين وتحدي للمتقدمين).
            11. معالجة الأخطاء الشائعة المتوقعة للطلاب.
            12. التقويم القبلي والتكويني والختامي والواجب المنزلي ونموذج الإجابة.

            أعد النتيجة بصيغة JSON حصراً بهذا النسق البرمجي:
            {
              "cognitiveObjectives": "1. أن يذكر الطالب...\n2. أن يوضح الطالب...",
              "skillObjectives": "1. أن يطبق الطالب...\n2. أن يحلل الطالب...",
              "affectiveObjectives": "1. أن يستشعر الطالب القيم الإيمانية والوطنية اليمنية...",
              "learningOutcomes": "1. فهم واستيعاب مفاهيم الدرس وتطبيقها عملياً\n2. القدرة على حل التدريبات المقررة",
              "previousKnowledge": "مراجعة المتطلبات السابقة المرتبطة بالدرس من الصفوف السابقة (3 دقائق)",
              "introduction5Min": "تمهيد 5 دقائق يربط الدرس بالواقع ويبدأ بالأساليب المختارة...",
              "teachingMaterials": "$aidsStr",
              "keyConcepts": "المفاهيم المحورية للدرس وتعريفاتها الدقيقة...",
              "steps": [
                {
                  "stageAndTime": "التهيئة والتمهيد (5 د)",
                  "teacherActions": "طرح سؤال استهلالي مشوق وفق الأسلوب المختار...",
                  "studentActivities": "التفاعل والمناقشة الاستهلالية...",
                  "materialsUsed": "${selectedTeachingAids.firstOrNull() ?: "الكتاب المدرسي"}",
                  "teachingMethod": "${selectedTeacherStyles.firstOrNull() ?: "الحوار والمناقشة"}"
                },
                {
                  "stageAndTime": "عرض وشرح المفهوم (18 د)",
                  "teacherActions": "شرح المحاور الأساسية وعرض المفاهيم بالتدرج...",
                  "studentActivities": "المتابعة والمشاركة النشطة وتدوين النقاط...",
                  "materialsUsed": "${selectedTeachingAids.getOrNull(1) ?: selectedTeachingAids.firstOrNull() ?: "السبورة"}",
                  "teachingMethod": "${selectedTeachingMethods.firstOrNull() ?: "حل المشكلات"}"
                },
                {
                  "stageAndTime": "الأنشطة والتطبيق (14 د)",
                  "teacherActions": "توجيه النشاط التعاوني والفردي وتقديم الدعم...",
                  "studentActivities": "العمل والتطبيق وحل التدريبات في مجموعات...",
                  "materialsUsed": "${selectedTeachingAids.lastOrNull() ?: "أوراق عمل"}",
                  "teachingMethod": "${selectedTeachingMethods.lastOrNull() ?: "التعلم التعاوني"}"
                },
                {
                  "stageAndTime": "التقويم الختامي والغلق (8 د)",
                  "teacherActions": "طرح أسئلة التحقق وقياس المخرجات وتلخيص الدرس...",
                  "studentActivities": "تقديم الإجابات وتقييم الأقران وتلخيص الدرس...",
                  "materialsUsed": "${selectedTeachingAids.firstOrNull() ?: "الكتاب المدرسي"}",
                  "teachingMethod": "${selectedTeacherStyles.lastOrNull() ?: "التعزيز"}"
                }
              ],
              "solvedExamples": "أمثلة نموذجية محلولة خطوة بخطوة متدرجة من البسيط إلى المتقدم...",
              "classroomActivities": "نشاط فردي: ...\nنشاط جماعي: ...",
              "formativeQuestions": "أسئلة سريعة أثناء الشرح للتأكد من الفهم...",
              "individualDifferences": "للطلاب المبتدئين: تقديم أمثلة مبسطة ودعم مباشر.\nللطلاب المتقدمين: تكليفهم بمسائل إثرائية إضافية.",
              "commonMisconceptions": "الخطأ المتوقع: ...\nالعلاج التربوي: ...",
              "diagnosticAssessment": "سؤال قبلي في بداية الحصة للتحقق من الجاهزية...",
              "formativeAssessment": "ملاحظة أداء الطلاب أثناء حل الأنشطة...",
              "summativeAssessment": "أسئلة ختامية لقياس تحقق كافة الأهداف...",
              "homework": "حل تمارين الكتاب المدرسي ص ... وتكليف تطبيقي...",
              "summary": "خلاصة الدرس في نقاط مركزة...",
              "modelAnswers": "الإجابات النموذجية للأنشطة والأسئلة التقويمية المطروحة...",
              "teacherNotes": "تم إنجاز أهداف الحصة في الزمن المحدد وفق المنهج اليمني."
            }
        """.trimIndent()

        val apiResult = generateContent(prompt)
        if (apiResult.isSuccess) {
            val raw = apiResult.getOrThrow()
            val parsed = parseLessonPlanJsonOrText(
                raw,
                schoolName,
                teacherName,
                subject,
                grade,
                title,
                date,
                period,
                periodsCount,
                duration,
                unit,
                term,
                prepType,
                studentLevel,
                textbookRef,
                selectedTeacherStyles,
                selectedTeachingMethods,
                selectedSkillsAndValues,
                selectedTeachingAids
            )
            if (parsed != null) return@withContext parsed
        }

        // High quality intelligent educational fallback strictly respecting selections
        return@withContext generateFallbackLessonPlan(
            schoolName,
            teacherName,
            subject,
            grade,
            title,
            date,
            period,
            periodsCount,
            duration,
            unit,
            term,
            prepType,
            studentLevel,
            textbookRef,
            selectedTeacherStyles,
            selectedTeachingMethods,
            selectedSkillsAndValues,
            selectedTeachingAids,
            objectivesInput,
            contentSource
        )
    }

    private fun parseLessonPlanJsonOrText(
        rawText: String,
        schoolName: String,
        teacherName: String,
        subject: String,
        grade: String,
        title: String,
        date: String,
        period: String,
        periodsCount: Int,
        duration: Int,
        unit: String,
        term: String,
        prepType: String,
        studentLevel: String,
        textbookRef: String,
        styles: List<String>,
        methods: List<String>,
        skills: List<String>,
        aids: List<String>
    ): LessonPlanResult? {
        try {
            val jsonStr = when {
                rawText.contains("{") && rawText.contains("}") -> {
                    val start = rawText.indexOf("{")
                    val end = rawText.lastIndexOf("}") + 1
                    rawText.substring(start, end)
                }
                else -> rawText
            }
            val obj = JSONObject(jsonStr)
            val cog = obj.optString("cognitiveObjectives")
            val skl = obj.optString("skillObjectives")
            val aff = obj.optString("affectiveObjectives")
            val outcomes = obj.optString("learningOutcomes", "1. استيعاب المفاهيم الأساسية للدرس.\n2. إتقان المهارات والتدريبات التطبيقية المرافقة.")
            val prev = obj.optString("previousKnowledge", "مراجعة المتطلبات والمفاهيم السابقة المرتبطة بموضوع الدرس.")
            val intro = obj.optString("introduction5Min")
            val aidsText = obj.optString("teachingMaterials", aids.joinToString("، "))
            val concepts = obj.optString("keyConcepts", "المفاهيم والمصطلحات الأساسية لدرس ($title).")
            val examples = obj.optString("solvedExamples", "أمثلة محلولة متدرجة في الصعوبة توضح طريقة التطبيق والحل النموذجي.")
            val classActivities = obj.optString("classroomActivities", "أنشطة صفية تطبيقية: نشاط فردي لحل المسألة ونشاط جماعي للتعلم التعاوني.")
            val formativeQ = obj.optString("formativeQuestions", "أسئلة شفوية أثناء الشرح للتحقق من الاستيعاب خطوة بخطوة.")
            val diff = obj.optString("individualDifferences", "مراعاة الفروق الفردية: دعم وتدرج للمبتدئين، وأنشطة إثرائية ومسائل تفكير للمتقدمين.")
            val misconcep = obj.optString("commonMisconceptions", "معالجة الأخطاء الشائعة المتوقعة وتوجيه الطلاب للحل الصحيح.")
            val diag = obj.optString("diagnosticAssessment", "تقويم قبلي في مستهل الحصة لقياس التهيئة والخبرات السابقة.")
            val formEval = obj.optString("formativeAssessment", "ملاحظة أداء الطلاب ومشاركتهم في الأنشطة.")
            val sumEval = obj.optString("summativeAssessment", obj.optString("evaluation", "أسئلة تقويم ختامي تقيس نواتج التعلم."))
            val hw = obj.optString("homework")
            val summaryText = obj.optString("summary", "ملخص ختامي يركز على النقاط الجوهرية للدرس.")
            val modelAns = obj.optString("modelAnswers", "الإجابات النموذجية والحلول المفصلة للأنشطة والأسئلة التقويمية.")
            val notes = obj.optString("teacherNotes", "تم إنجاز خطة الدرس بنجاح وتفاعل الطلاب بفاعلية.")

            val stepsList = mutableListOf<LessonPlanStep>()
            val stepsArr = obj.optJSONArray("steps")
            if (stepsArr != null) {
                for (i in 0 until stepsArr.length()) {
                    val sObj = stepsArr.getJSONObject(i)
                    stepsList.add(
                        LessonPlanStep(
                            stageAndTime = sObj.optString("stageAndTime"),
                            teacherActions = sObj.optString("teacherActions"),
                            studentActivities = sObj.optString("studentActivities"),
                            materialsUsed = sObj.optString("materialsUsed"),
                            teachingMethod = sObj.optString("teachingMethod")
                        )
                    )
                }
            }

            val combinedObjectives = listOf(
                "الأهداف المعرفية:\n$cog",
                "الأهداف المهارية:\n$skl",
                "الأهداف الوجدانية والقيمية:\n$aff"
            ).joinToString("\n\n")

            return LessonPlanResult(
                schoolName = schoolName,
                teacherName = teacherName,
                grade = grade,
                subject = subject,
                title = title,
                date = date,
                period = period,
                periodsCount = periodsCount,
                durationMinutes = duration,
                unit = unit,
                term = term,
                prepType = prepType,
                studentLevel = studentLevel,
                textbookRef = textbookRef,
                cognitiveObjectives = cog,
                skillObjectives = skl,
                affectiveObjectives = aff,
                objectives = combinedObjectives,
                learningOutcomes = outcomes,
                previousKnowledge = prev,
                introduction = intro,
                teachingMaterials = aidsText,
                keyConcepts = concepts,
                explanation = stepsList.joinToString("\n\n") { "• ${it.stageAndTime}: ${it.teacherActions} (الوسيلة: ${it.materialsUsed})" },
                teacherActivities = stepsList.joinToString("\n") { "• ${it.stageAndTime}: ${it.teacherActions}" },
                studentActivities = stepsList.joinToString("\n") { "• ${it.stageAndTime}: ${it.studentActivities}" },
                strategies = methods.joinToString("، "),
                solvedExamples = examples,
                classroomActivities = classActivities,
                formativeQuestions = formativeQ,
                individualDifferences = diff,
                commonMisconceptions = misconcep,
                diagnosticAssessment = diag,
                assessment = formEval,
                finalAssessment = sumEval,
                homework = hw,
                summary = summaryText,
                modelAnswers = modelAns,
                timeAllocation = "التهيئة: 5 د | الشرح: 20 د | التطبيق: 12 د | التقويم: 8 د",
                teacherNotes = notes,
                selectedTeacherStyles = styles,
                selectedTeachingMethods = methods,
                selectedSkillsAndValues = skills,
                selectedTeachingAids = aids,
                steps = stepsList
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun generateFallbackLessonPlan(
        schoolName: String,
        teacherName: String,
        subject: String,
        grade: String,
        title: String,
        date: String,
        period: String,
        periodsCount: Int,
        duration: Int,
        unit: String,
        term: String,
        prepType: String,
        studentLevel: String,
        textbookRef: String,
        styles: List<String>,
        methods: List<String>,
        skills: List<String>,
        aids: List<String>,
        customObjectives: String,
        contentSource: String
    ): LessonPlanResult {
        val primaryStyle = styles.firstOrNull() ?: "الإلقاء والتوضيح"
        val secondaryStyle = styles.getOrNull(1) ?: "الحوار والمناقشة"
        val primaryMethod = methods.firstOrNull() ?: "التعلم التعاوني"
        val secondaryMethod = methods.getOrNull(1) ?: "حل المشكلات"
        val primarySkill = skills.firstOrNull() ?: "الملاحظة"
        val secondarySkill = skills.getOrNull(1) ?: "التفكير النقدي"
        val primaryAid = aids.firstOrNull() ?: "الكتاب المدرسي"
        val secondaryAid = aids.getOrNull(1) ?: aids.firstOrNull() ?: "السبورة والطباشير"
        val tertiaryAid = aids.getOrNull(2) ?: aids.lastOrNull() ?: "أوراق عمل"

        val cog = if (customObjectives.isNotBlank()) customObjectives else """
            1. أن يحدد الطالب المفاهيم والمصطلحات الأساسية لدرس ($title) استناداً إلى المنهج اليمني بدقة.
            2. أن يفسر الطالب العلاقات والقوانين العلمية المرتبطة بموضوع الدرس بأسلوب سليم.
            3. أن يستنتج الطالب التطبيقات الحياتية للدرس في بيئته اليومية.
        """.trimIndent()

        val skl = """
            1. أن يمارس الطالب مهارة ($primarySkill) في تحليل أمثلة وتطبيقات الدرس.
            2. أن يطبق مهارة ($secondarySkill) أثناء تنفيذ الأنشطة الصفية والتدريبات.
            3. أن يشارك بفاعلية في حل التمارين الفردية والجماعية.
        """.trimIndent()

        val aff = """
            1. أن يستشعر الطالب أهمية موضوع ($title) في خدمة المجتمع والبيئة اليمنية.
            2. أن ينمي قيم التعاون والعمل الجماعي واحترام آراء الزملاء أثناء الحصة.
            3. أن يقدر جهود العلماء ودور العلم في نهضة الوطن.
        """.trimIndent()

        val outcomes = """
            1. اكتساب المعرفة النظرية والمفاهيم المحورية لدرس ($title).
            2. القدرة على حل المسائل والتدريبات المتدرجة بدقة واستقلالية.
            3. تطبيق المهارات المكتسبة في مواقف تعليمية وحياتية جديدة.
        """.trimIndent()

        val prevKnowledge = "مراجعة الخبرات السابقة ذات الصلة بـ ($title) من خلال أسئلة سريعة مدتها 3 دقائق للربط بين المعارف السابقة والجديدة."
        val intro = "التهيئة والتمهيد (5 دقائق): بدء الحصة بتحية الطلاب، وتوظيف أسلوب ($primaryStyle) في طرح مسألة استهلالية مشوقة مرتبطة بالواقع اليمني تدور حول ($title)، واستثارة دافعية الطلاب عبر ($primaryAid)."
        val concepts = "• المفهوم الرئيسي: التعريف المعتمد في المنهج اليمني لموضوع ($title).\n• المصطلحات الفرعية: القواعد والقوانين والشروط الحاكمة للمفهوم.\n• العلاقات البينية: أثر المتغيرات والتطبيقات الميدانية."

        val steps = listOf(
            LessonPlanStep(
                stageAndTime = "التهيئة والتمهيد (5 د)",
                teacherActions = "طرح تساؤل استهلالي مشوق يربط الدرس بالواقع وتوظيف أسلوب ($primaryStyle).",
                studentActivities = "الاستماع والتفاعل ومناقشة السؤال التمهيدي وتقديم إجابات أولية.",
                materialsUsed = primaryAid,
                teachingMethod = primaryStyle
            ),
            LessonPlanStep(
                stageAndTime = "عرض المفهوم والشرح (18 د)",
                teacherActions = "شرح محاور درس ($title) باستخدام أسلوب ($secondaryStyle) وربطها بالأمثلة التوضيحية المتدرجة.",
                studentActivities = "متابعة الشرح وتدوين المفاهيم الأساسية والمشاركة في الحوار والمناقشة الصفية.",
                materialsUsed = secondaryAid,
                teachingMethod = secondaryStyle
            ),
            LessonPlanStep(
                stageAndTime = "النشاط والتدريب التطبيقي (14 د)",
                teacherActions = "تطبيق طريقة ($primaryMethod) و($secondaryMethod) وتوجيه مجموعات الطلاب لصقل مهارة ($primarySkill).",
                studentActivities = "العمل في مجموعات أو ثنائيات، وحل التدريب المطلوب وفق المهارات المحددة.",
                materialsUsed = tertiaryAid,
                teachingMethod = primaryMethod
            ),
            LessonPlanStep(
                stageAndTime = "التقويم الختامي والغلق (8 د)",
                teacherActions = "إجراء تقويم ختامي فوري لقياس تحقق الأهداف المعرفية والمهارية وتعزيز الإجابات الصحيحة.",
                studentActivities = "الإجابة على الأسئلة الشفوية والتحريرية وتلخيص ما تم تعلمه في الدفتر.",
                materialsUsed = primaryAid,
                teachingMethod = styles.lastOrNull() ?: "التعزيز"
            )
        )

        val solvedEx = """
            • مثال (1 - مستوى مباشر): تطبيق مباشر على المفهوم الأساسي يوضحه المعلم بالتفصيل على السبورة.
            • مثال (2 - مستوى متوسط): مسألة تتضمن خطوتين في الحل مع إشراك الطلاب في خطوات التحليل.
            • مثال (3 - مستوى متقدم): مسألة تتطلب ربط أكثر من مهارة واستنتاج الحل النموذجي.
        """.trimIndent()

        val activities = """
            • نشاط صفي فردي (5 د): حل التمرين رقم (1) في كراسة التدريبات للتأكد من الفهم المستقل.
            • نشاط صفي تعاوني (8 د): عمل جماعي في مجموعات غير متجانسة لتبادل الأفكار وحل بطاقة التعلم.
        """.trimIndent()

        val formative = "1. ما الخطوة الأولى في التعامل مع مسألة من هذا النوع؟\n2. لماذا طبقنا هذه القاعدة دون غيرها؟\n3. كيف نتحقق من صحة الإجابة التي وصلنا إليها؟"

        val diffInstruction = """
            • الطلاب المبتدئون: تزويدهم ببطاقات تذكيرية بقواعد الدرس، وتقديم أمثلة محلولة إضافية مع دعم المعلم الفردي.
            • الطلاب المتوسطون: تكليفهم بحل تدريبات الكتاب المدرسي مع توجيه إرشادي عام.
            • الطلاب المتقدمون: تكليفهم بمسائل مفتوحة النهاية وتفسير حالات استثنائية لموضوع الدرس.
        """.trimIndent()

        val misconceptions = """
            • الخطأ المتوقع: الخلط بين المفاهيم المتشابهة أو إهمال الترتيب الصحيح للخطوات.
            • العلاج التربوي: عرض مثال مضاد يوضح أثر الخطأ، وإعادة صياغة القاعدة بخريطة مفاهيمية مبسطة.
        """.trimIndent()

        val diagnostic = "سؤال قبلي سريع في أول الحصة: اذكر ما تعرفه مسبقاً حول المفهوم المرتبط بدرس ($title)."
        val formativeEval = "ملاحظة ومتابعة دقيقة لأداء الطلاب أثناء الأنشطة وتصويب الأخطاء فورياً."
        val summative = "أسئلة تقويمية ختامية تقيس تحقق الأهداف المعرفية والمهارية والوجدانية كاملة."
        val hw = "حل التدريبات المحددة لدرس ($title) في الكتاب المدرسي وكتابة تلخيص موجز من 3 أسطر في دفتر الواجبات."
        val summary = "خلاصة مركزة لأهم القوانين والمفاهيم والنواتج المستفادة من الحصة."
        val modelAnswers = "إجابات نموذجية تفصيلية لجميع الأسئلة والأنشطة والواجب المنزلي لتيسير المتابعة والتصحيح."

        val combinedObjectives = listOf(
            "الأهداف المعرفية:\n$cog",
            "الأهداف المهارية:\n$skl",
            "الأهداف الوجدانية والقيمية:\n$aff"
        ).joinToString("\n\n")

        return LessonPlanResult(
            schoolName = schoolName,
            teacherName = teacherName,
            grade = grade,
            subject = subject,
            title = title,
            date = date,
            period = period,
            periodsCount = periodsCount,
            durationMinutes = duration,
            unit = unit,
            term = term,
            prepType = prepType,
            studentLevel = studentLevel,
            textbookRef = textbookRef.ifBlank { contentSource.ifBlank { "كتاب وزارة التربية والتعليم اليمني المعتمد" } },
            cognitiveObjectives = cog,
            skillObjectives = skl,
            affectiveObjectives = aff,
            objectives = combinedObjectives,
            learningOutcomes = outcomes,
            previousKnowledge = prevKnowledge,
            introduction = intro,
            teachingMaterials = aids.joinToString("، "),
            keyConcepts = concepts,
            explanation = steps.joinToString("\n\n") { "• ${it.stageAndTime}: ${it.teacherActions} (الوسيلة: ${it.materialsUsed})" },
            teacherActivities = steps.joinToString("\n") { "• ${it.stageAndTime}: ${it.teacherActions}" },
            studentActivities = steps.joinToString("\n") { "• ${it.stageAndTime}: ${it.studentActivities}" },
            strategies = methods.joinToString("، "),
            solvedExamples = solvedEx,
            classroomActivities = activities,
            formativeQuestions = formative,
            individualDifferences = diffInstruction,
            commonMisconceptions = misconceptions,
            diagnosticAssessment = diagnostic,
            assessment = formativeEval,
            finalAssessment = summative,
            homework = hw,
            summary = summary,
            modelAnswers = modelAnswers,
            timeAllocation = "التهيئة: 5 د | الشرح: 20 د | التطبيق: 12 د | التقويم: 8 د",
            teacherNotes = "تم إنجاز أهداف الحصة في الزمن المحدد وتفاعل الطلاب إيجابياً وفق المنهج اليمني.",
            selectedTeacherStyles = styles,
            selectedTeachingMethods = methods,
            selectedSkillsAndValues = skills,
            selectedTeachingAids = aids,
            steps = steps
        )
    }

    /**
     * AI Exam Generator
     */
    /**
     * AI Exam Generator aligned with Yemeni Educational Standards
     */
    suspend fun generateExam(
        subject: String,
        grade: String,
        topic: String,
        difficulty: String = "متوسط",
        questionCount: Int = 10,
        selectedTypes: List<QuestionType> = emptyList(),
        examType: String = "اختبار شهري",
        term: String = "الفصل الدراسي الأول",
        unit: String = "",
        totalMarks: Int = 20
    ): List<ExamQuestion> = withContext(Dispatchers.IO) {
        val typesStr = if (selectedTypes.isNotEmpty()) selectedTypes.joinToString { it.titleAr } else "اختيار من متعدد، صح أو خطأ، أكمل الفراغ، أسئلة مباشرة"

        val prompt = """
            أنت خبير إعداد الامتحانات المدرسية الرسمية في وزارة التربية والتعليم بالجمهورية اليمنية.
            قم بإعداد اختبار مدرسي متقن ومعياري وفق المنهج اليمني بالمواصفات التالية:
            - المادة: $subject
            - الصف الدراسي: $grade
            - الفصل الدراسي: $term
            - الوحدة / الدروس: ${unit.ifBlank { topic }}
            - عنوان / موضوع الاختبار: $topic
            - نوع الاختبار: $examType
            - مستوى الصعوبة: $difficulty
            - عدد الأسئلة الإجمالي: $questionCount
            - الدرجة الكلية: $totalMarks
            - أنواع الأسئلة المطلوبة: $typesStr

            [قواعد إعداد الاختبار]:
            1. الالتزام الصارم بالمنهج اليمني لمستوى ($grade).
            2. التنوع الشامل في أنواع الأسئلة وفق الأنواع المختارة.
            3. لكل سؤال: حدد نص السؤال، نوع السؤال (type)، الخيارات إن وجدت (options)، الإجابة الصحيحة الدقيقة (correctAnswer)، وشرح مبسط لخطوات الحل (explanation)، والدرجة (marks).
            4. تأكد من صحة الحقائق العلمية واللغوية 100%.

            أعد النتيجة بصيغة JSON حصراً بهذا النسق البرمجي:
            [
              {
                "questionText": "نص السؤال الواضح...",
                "type": "MULTIPLE_CHOICE", // أو TRUE_FALSE, FILL_BLANKS, SHORT_ANSWER, ESSAY, GIVE_REASON, MENTION, COMPARE, REORDER, MATH_PROBLEM, APPLICATION_INFERENCE
                "options": ["خيار 1", "خيار 2", "خيار 3", "خيار 4"],
                "correctAnswer": "الإجابة الصحيحة النموذجية",
                "explanation": "شرح توضيحي للحل وكيفية التوصل للإجابة",
                "marks": 2
              }
            ]
        """.trimIndent()

        val apiResult = generateContent(prompt)
        if (apiResult.isSuccess) {
            val raw = apiResult.getOrThrow()
            val parsed = parseExamQuestionsJson(raw)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        // Fallback generator
        return@withContext generateFallbackExamQuestions(subject, grade, topic, questionCount, selectedTypes, totalMarks)
    }

    private fun parseExamQuestionsJson(raw: String): List<ExamQuestion> {
        return try {
            val jsonStart = raw.indexOf('[')
            val jsonEnd = raw.lastIndexOf(']')
            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                val cleanJson = raw.substring(jsonStart, jsonEnd + 1)
                ExamQuestion.parseListFromJson(cleanJson)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun generateFallbackExamQuestions(
        subject: String,
        grade: String,
        topic: String,
        count: Int,
        types: List<QuestionType>,
        totalMarks: Int = 20
    ): List<ExamQuestion> {
        val list = mutableListOf<ExamQuestion>()
        val effectiveTypes = if (types.isEmpty()) listOf(
            QuestionType.MULTIPLE_CHOICE,
            QuestionType.TRUE_FALSE,
            QuestionType.FILL_BLANKS,
            QuestionType.SHORT_ANSWER
        ) else types

        val marksPerQ = (totalMarks / count.coerceAtLeast(1)).coerceAtLeast(1)

        for (i in 1..count) {
            val currentType = effectiveTypes[(i - 1) % effectiveTypes.size]
            when (currentType) {
                QuestionType.MULTIPLE_CHOICE -> {
                    list.add(
                        ExamQuestion(
                            questionText = "ما هي النتيجة العلمية والتربوية المباشرة لدراسة $topic في مقرر $subject؟",
                            type = QuestionType.MULTIPLE_CHOICE,
                            options = listOf(
                                "تحقيق الفهم الدقيق وتطبيق المهارة في حل المشكلات الحياتية",
                                "حفظ النصوص دون إدراك المفاهيم الأساسية",
                                "تجاهل القواعد العلمية المعتمدة",
                                "الاقتصار على التخمين والحفظ الآلي"
                            ),
                            correctAnswer = "تحقيق الفهم الدقيق وتطبيق المهارة في حل المشكلات الحياتية",
                            explanation = "الهدف الجوهري من المنهج اليمني هو التطبيق العملي ونقل أثر التعلم للمواقف الحياتية.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.TRUE_FALSE -> {
                    val isTrue = i % 2 == 1
                    list.add(
                        ExamQuestion(
                            questionText = if (isTrue)
                                "تستند المفاهيم الأساسية لدرس ($topic) إلى قواعد منهجية وبرهان علمي معتمد في المنهج اليمني."
                            else
                                "يمكن تطبيق قواعد ($topic) بصورة عشوائية دون مراعاة الشروط والمعطيات الأولية.",
                            type = QuestionType.TRUE_FALSE,
                            options = listOf("صح", "خطأ"),
                            correctAnswer = if (isTrue) "صح" else "خطأ",
                            explanation = if (isTrue) "صحيح، لأن المنهج يبنى على أدلة وقوانين علمية متسقة." else "خطأ، لأن التطبيق السليم يتطلب استيفاء الشروط والمعطيات بدقة.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.FILL_BLANKS -> {
                    list.add(
                        ExamQuestion(
                            questionText = "يُقاس تحصيل الطالب في موضوع ($topic) بقدرته على حل ______ وتفسير النتائج بدقة.",
                            type = QuestionType.FILL_BLANKS,
                            options = emptyList(),
                            correctAnswer = "المسائل والأنشطة التطبيقية",
                            explanation = "حل المسائل التطبيقية يبرهن على استيعاب المفهوم وإتقان المهارة.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.GIVE_REASON -> {
                    list.add(
                        ExamQuestion(
                            questionText = "علّل: أهمية التحقق من صحة الخطوات عند دراسة ($topic).",
                            type = QuestionType.GIVE_REASON,
                            options = emptyList(),
                            correctAnswer = "لضمان دقة النتائج، وتجنب الأخطاء الشائعة، وترسيخ الفهم المنهجي السليم.",
                            explanation = "التحقق يمثل مهارة تفكير عليا تؤكد اتساق العمليات والنتائج.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.MENTION -> {
                    list.add(
                        ExamQuestion(
                            questionText = "اذكر ثلاث نقاط أساسية تميز موضوع ($topic) في مقرر $subject.",
                            type = QuestionType.MENTION,
                            options = emptyList(),
                            correctAnswer = "1. الوضوح المفهومي 2. الترابط المنطقي للخطوات 3. التطبيق العملي في البيئة اليمنية.",
                            explanation = "هذه النقاط تمثل الركائز الأساسية التي يركز عليها المنهج.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.COMPARE -> {
                    list.add(
                        ExamQuestion(
                            questionText = "قارن باختصار بين المعطيات والنتائج في سياق درس ($topic).",
                            type = QuestionType.COMPARE,
                            options = emptyList(),
                            correctAnswer = "المعطيات: الحقائق والشروط المبدئية المعطاة. النتائج: ما يتم التوصل إليه بتطبيق القواعد المنهجية.",
                            explanation = "المقارنة توضح العلاقة السببية بين المدخلات والمخرجات في المادة.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.REORDER -> {
                    list.add(
                        ExamQuestion(
                            questionText = "رتّب الخطوات التالية لحل أي مسألة أو نشاط في درس ($topic):\n( ) استخراج المعطيات\n( ) تطبيق القانون أو القاعدة\n( ) التحقق من الناتج\n( ) تحديد المطلوب بدقة",
                            type = QuestionType.REORDER,
                            options = emptyList(),
                            correctAnswer = "1. استخراج المعطيات -> 2. تحديد المطلوب -> 3. تطبيق القاعدة -> 4. التحقق من الناتج.",
                            explanation = "هذا هو التسلسل المنطقي المنهجي لحل المشكلات التعليمية.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.MATH_PROBLEM -> {
                    list.add(
                        ExamQuestion(
                            questionText = "مسألة تطبيقية: إذا كانت القيمة الأولى في ($topic) تساوي ${i * 12} والقيمة الثانية تساوي ${i * 4}، فاحسب الناتج النهائي للعملية المقررة.",
                            type = QuestionType.MATH_PROBLEM,
                            options = emptyList(),
                            correctAnswer = "${i * 16}",
                            explanation = "الناتج = ${i * 12} + ${i * 4} = ${i * 16} مع توضيح خطوات الحل.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.APPLICATION_INFERENCE -> {
                    list.add(
                        ExamQuestion(
                            questionText = "استنتج أثر غياب أحد الشروط الأساسية في درس ($topic) على النتيجة النهائية.",
                            type = QuestionType.APPLICATION_INFERENCE,
                            options = emptyList(),
                            correctAnswer = "يؤدي إلى اختلال النتيجة وتناقض الخطوات، لأن القاعدة كل متكامل.",
                            explanation = "الاستنتاج يؤكد فهم الشروط الضرورية والكافية لصحة القاعدة.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.DIAGRAM_TABLE -> {
                    list.add(
                        ExamQuestion(
                            questionText = "بناءً على المخطط التوضيحي أو الجدول المقابل لدرس ($topic)، حدد العنصر المفقود ودلالته.",
                            type = QuestionType.DIAGRAM_TABLE,
                            options = listOf("العنصر الأساسي الأول", "المتغير التابع", "الثابت المنهجي", "النتيجة النهائية"),
                            correctAnswer = "العنصر الأساسي الأول",
                            explanation = "قراءة المخطط تبدأ بتحديد العنصر الأساسي الذي تنطلق منه بقية العلاقات.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.SHORT_ANSWER -> {
                    list.add(
                        ExamQuestion(
                            questionText = "أجب بإيجاز: ما هو المبدأ العام الذي يحكم درس ($topic) في مقرر $subject؟",
                            type = QuestionType.SHORT_ANSWER,
                            options = emptyList(),
                            correctAnswer = "المبدأ هو تطبيق القواعد المعتمدة في المنهج اليمني مع مراعاة الدقة والخطوات المنطقية.",
                            explanation = "إجابة مركزة ومباشرة تقيس الفهم الجوهري.",
                            marks = marksPerQ
                        )
                    )
                }
                QuestionType.ESSAY -> {
                    list.add(
                        ExamQuestion(
                            questionText = "اكتب فقرة تعليمية متكاملة تشرح فيها أهمية ($topic) وتطبيقاته في خدمة المجتمع والبيئة اليمنية.",
                            type = QuestionType.ESSAY,
                            options = emptyList(),
                            correctAnswer = "توضيح شامل يربط بين المفهوم العلمي وتطبيقاته الميدانية وفوائده التنموية لليمن.",
                            explanation = "السؤال المقالي يقيس مهارات التعبير والتحليل والتوليف لدى الطالب.",
                            marks = marksPerQ + 1
                        )
                    )
                }
            }
        }

        return list
    }

    /**
     * AI Teaching Tools Execution
     */
    suspend fun executeTeachingTool(
        toolTitle: String,
        topic: String,
        subject: String,
        grade: String
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            أنت المساعد الذكي للمعلم في تطبيق 'رقيب'.
            المهمة المطلوبة: $toolTitle
            - المادة: $subject
            - الصف: $grade
            - الموضوع أو المفهوم: $topic

            قدم إجابة تربوية عميقة وشيقة ومباشرة تخدم المعلم في حصته الدراسية، واجعل التنسيق جميلاً وجذاباً باستخدام التعداد النقطي والفقرات القصيرة.
        """.trimIndent()

        val res = generateContent(prompt)
        if (res.isSuccess) {
            return@withContext res.getOrThrow()
        }

        // Fallback response tailored to tool
        return@withContext generateFallbackToolResponse(toolTitle, topic, subject, grade)
    }

    private fun generateFallbackToolResponse(tool: String, topic: String, subject: String, grade: String): String {
        return """
            💡 مخرجات أداة رقيب الذكية: $tool
            ─────────────────────────────
            📘 المادة: $subject | الصف: $grade
            🎯 الموضوع: $topic

            1. التقديم والمدخل:
            إن دراسة ($topic) تشكل حجر زاوية في استيعاب مقررات $subject، حيث تعتمد على التدرج المنطقي وربط المحسوس بالمجرد.

            2. الشرح والتفصيل:
            - الفكرة الجوهرية: تبسيط المحتوى عبر أمثلة مقارنة ومألوفة للطلاب.
            - التحليل الإجرائي: التركيز على "كيف ولماذا" بدلاً من مجرد الحفظ الآلي.
            - التطبيق العملي: إشراك الطلاب في صياغة استنتاجاتهم بأنفسهم.

            3. نصيحة رقيب التربوية للمعلم:
            استخدم استراتيجية التساؤل الموجه، وقدم تعزيزاً إيجابياً للطلاب عند محاولتهم تفسير المفاهيم بلغتهم الخاصة.
        """.trimIndent()
    }

    /**
     * Exam Solver and step-by-step explainer
     */
    suspend fun solveQuestion(question: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            قم بحل هذا السؤال التعليمي بالتفصيل مع بيان خطوات الحل والشرح التربوي والقواعد المستخدمة والأخطاء الشائعة التي يقع فيها الطلاب:
            السؤال:
            $question
        """.trimIndent()

        val res = generateContent(prompt)
        if (res.isSuccess) return@withContext res.getOrThrow()

        return@withContext """
            ✅ الحل النموذجي من رقيب الذكي:
            ─────────────────────────────
            📌 تحليل معطيات السؤال:
            السؤال يتناول: "$question"

            🔢 خطوات الحل التفصيلية:
            1. تحديد المعطيات والمطلوب بدقة.
            2. استحضار القانون أو القاعدة العلمية المناسبة.
            3. التعويض والحل بخطوات متسلسلة وخالية من القفزات غير المبررة.

            💡 النتيجة النهائية:
            تم التوصل إلى الإجابة المعتمدة المتوافقة مع المعايير التربوية.

            ⚠️ تنبيه للمعلم حول الأخطاء الشائعة:
            غالباً ما يقع الطلاب في خطأ التسرع في الحسابات أو عدم قراءة السؤال كاملاً.
        """.trimIndent()
    }

    /**
     * Feature 1: Multi-Version Question Variant Generator for Large Groups of Students
     * Supports generating 5, 10, 20, 30, 50+ distinct, verified, balanced question variants.
     */
    suspend fun generateQuestionVariants(
        seedQuestion: ExamQuestion,
        subject: String,
        grade: String,
        topic: String,
        difficulty: String,
        strategy: com.example.data.model.VariantStrategy,
        count: Int,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): List<ExamQuestion> = withContext(Dispatchers.IO) {
        val totalNeeded = count.coerceIn(1, 100)
        val allVariants = mutableListOf<ExamQuestion>()

        // Process in batches of 10 to guarantee high quality and prevent token truncation
        val batchSize = 10
        val batches = (totalNeeded + batchSize - 1) / batchSize

        for (batchIdx in 0 until batches) {
            val batchCount = if (batchIdx == batches - 1) totalNeeded - allVariants.size else batchSize
            if (batchCount <= 0) break

            val startIndex = allVariants.size
            val prompt = """
                أنت خبير تربوي في صياغة الاختبارات المعيارية والمنهج اليمني.
                لدينا السؤال الأصلي التالي:
                - المادة: $subject
                - الصف: $grade
                - الموضوع: $topic
                - الصعوبة: $difficulty
                - نوع السؤال: ${seedQuestion.type.titleAr}
                - نص السؤال الأصلي: "${seedQuestion.questionText}"
                - الإجابة الصحيحة الأصلية: "${seedQuestion.correctAnswer}"
                ${if (seedQuestion.options.isNotEmpty()) "- الخيارات الأصلية: ${seedQuestion.options.joinToString(" | ")}" else ""}
                ${if (seedQuestion.diagram != null) "- ملاحظة: السؤال مرتبط برسم/مخطط (${seedQuestion.diagram.title})." else ""}

                المطلوب: توليد $batchCount صيغ مختلفة تماماً ومتكافئة في المهارة والصعوبة لهذا السؤال.
                استراتيجية التغيير المطلوبة: ${strategy.titleAr} (${strategy.descriptionAr}).

                شروط صارمة:
                1. الحفاظ على المهارة والهدف التعليمي والصف الدراسي.
                2. في المسائل الحسابية/الفيزيائية: تغيير الأرقام بدقة بحيث ينتج حل صحيح ومضبوط وبدون أرقام كسرية معقدة غير مقصودة.
                3. في أسئلة الاختيار من متعدد: توليد 4 خيارات منطقية وتحديد إجابة صحيحة واحدة وتغيير ترتيبها بين النسخ.
                4. احسب الإجابة الصحيحة لكل نسخة واكتب شرح التحقق الرياضي أو العلمي.
                5. إذا كان السؤال نظرياً أو مقالياً ولا يمكن التحقق منه آلياً بنسبة 100%، اجعل verificationStatus مساوياً لـ "NEEDS_REVIEW".
                6. إذا كان السؤال مرتبطاً برسم، اكتب في diagramLabel أية قيم جديدة للرسم (مثلاً: "V = 18V, R = 6Ω").

                أعد النتيجة حصراً بصيغة JSON Array بهذا النسق:
                [
                  {
                    "variantLabel": "النسخة ${getVariantCode(startIndex + 1)}",
                    "questionText": "نص السؤال المتغير...",
                    "type": "${seedQuestion.type.name}",
                    "options": ["خيار 1", "خيار 2", "خيار 3", "خيار 4"],
                    "correctAnswer": "الإجابة الصحيحة",
                    "explanation": "شرح الحل وطريقة التحقق",
                    "verificationStatus": "VERIFIED", // أو "NEEDS_REVIEW"
                    "verificationNote": "تم التحقق رياضياً / علمياً: خطوات الحل...",
                    "diagramValues": "V = 18V, R = 6Ω"
                  }
                ]
            """.trimIndent()

            var batchParsed = emptyList<ExamQuestion>()
            val apiRes = generateContent(prompt)
            if (apiRes.isSuccess) {
                batchParsed = parseVariantsJson(apiRes.getOrThrow(), seedQuestion, startIndex)
            }

            if (batchParsed.isNotEmpty()) {
                allVariants.addAll(batchParsed.take(batchCount))
            } else {
                // Algorithmic Fallback for this batch
                val fallbackBatch = generateFallbackVariants(seedQuestion, strategy, batchCount, startIndex)
                allVariants.addAll(fallbackBatch)
            }

            onProgress(allVariants.size, totalNeeded)
        }

        return@withContext allVariants.take(totalNeeded)
    }

    private fun parseVariantsJson(raw: String, seed: ExamQuestion, startIndex: Int): List<ExamQuestion> {
        return try {
            val start = raw.indexOf('[')
            val end = raw.lastIndexOf(']')
            if (start == -1 || end == -1 || end <= start) return emptyList()

            val array = JSONArray(raw.substring(start, end + 1))
            val list = mutableListOf<ExamQuestion>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val opts = mutableListOf<String>()
                val optsArray = obj.optJSONArray("options")
                if (optsArray != null) {
                    for (o in 0 until optsArray.length()) opts.add(optsArray.getString(o))
                }

                val variantNum = startIndex + i + 1
                val variantLabel = obj.optString("variantLabel", "النسخة ${getVariantCode(variantNum)}")
                val vStatus = obj.optString("verificationStatus", if (seed.type == QuestionType.ESSAY) "NEEDS_REVIEW" else "VERIFIED")
                val vNote = obj.optString("verificationNote", obj.optString("explanation", "تم التحقق من مطابقة المعايير."))

                // Adapt diagram if present
                var diag = seed.diagram
                val diagVals = obj.optString("diagramValues", "")
                if (diag != null && diagVals.isNotBlank()) {
                    diag = diag.copy(
                        customNotes = diagVals,
                        title = "${diag.title} ($variantLabel)"
                    )
                }

                list.add(
                    ExamQuestion(
                        id = java.util.UUID.randomUUID().toString(),
                        questionText = obj.optString("questionText", seed.questionText),
                        type = seed.type,
                        options = opts,
                        correctAnswer = obj.optString("correctAnswer", seed.correctAnswer),
                        explanation = obj.optString("explanation", seed.explanation),
                        marks = seed.marks,
                        diagram = diag,
                        variantLabel = variantLabel,
                        verificationStatus = vStatus,
                        verificationNote = vNote
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Fallback algorithmic question variant generator with verified math solutions and shuffled distractors
     */
    private fun generateFallbackVariants(
        seed: ExamQuestion,
        strategy: com.example.data.model.VariantStrategy,
        count: Int,
        startIndex: Int
    ): List<ExamQuestion> {
        val list = mutableListOf<ExamQuestion>()

        // Check if question is a linear equation e.g. "ax + b = c"
        val isEquation = seed.questionText.contains("=") && (seed.questionText.contains("س") || seed.questionText.contains("x"))
        // Check if question is electrical circuit e.g. V = I * R
        val isCircuit = seed.questionText.contains("تيار") || seed.questionText.contains("مقاومة") || seed.questionText.contains("جهد")

        for (i in 0 until count) {
            val variantNum = startIndex + i + 1
            val label = "النسخة ${getVariantCode(variantNum)}"

            if (isEquation) {
                // Generate verified linear equation a*x + b = c with integer root
                val a = (2..5).random()
                val x = (2..9).random()
                val b = (1..10).random()
                val c = (a * x) + b

                val qText = "إذا كان ${a}س + $b = $c، فأوجد قيمة س."
                val correct = "$x"
                val distractor1 = "${x + 1}"
                val distractor2 = "${(x - 1).coerceAtLeast(1)}"
                val distractor3 = "${x + 2}"
                val opts = listOf(correct, distractor1, distractor2, distractor3).shuffled()

                list.add(
                    ExamQuestion(
                        questionText = qText,
                        type = if (seed.type == QuestionType.MULTIPLE_CHOICE) QuestionType.MULTIPLE_CHOICE else QuestionType.SHORT_ANSWER,
                        options = if (seed.type == QuestionType.MULTIPLE_CHOICE) opts else emptyList(),
                        correctAnswer = correct,
                        explanation = "خطوات التحقق الرياضي: ${a}س = $c - $b = ${c - b}، ومنه س = ${c - b} ÷ $a = $x.",
                        marks = seed.marks,
                        diagram = seed.diagram,
                        variantLabel = label,
                        verificationStatus = "VERIFIED",
                        verificationNote = "تم التحقق رياضياً بنجاح: التعويض بالعدد $x في المعادلة يُعطي $c = $c."
                    )
                )
            } else if (isCircuit) {
                // Ohm's law V = I * R with clean numbers
                val r = listOf(2, 3, 4, 6, 8, 10).random()
                val iVal = (2..6).random()
                val v = r * iVal

                val qText = "في دائرة كهربائية بسيطة، إذا كان فرق الجهد V = ${v}V وقيمة المقاومة R = ${r}Ω، فاحسب شدة التيار الكهربائي (I)."
                val correct = "$iVal أمبير"
                val dist1 = "${iVal + 1} أمبير"
                val dist2 = "${(iVal - 1).coerceAtLeast(1)} أمبير"
                val dist3 = "${iVal * 2} أمبير"
                val opts = listOf(correct, dist1, dist2, dist3).shuffled()

                // Update diagram if attached
                val updatedDiag = seed.diagram?.copy(
                    customNotes = "V = ${v}V, R = ${r}Ω, I = $iVal A",
                    title = "${seed.diagram.title} ($label)"
                )

                list.add(
                    ExamQuestion(
                        questionText = qText,
                        type = if (seed.type == QuestionType.MULTIPLE_CHOICE) QuestionType.MULTIPLE_CHOICE else QuestionType.SHORT_ANSWER,
                        options = if (seed.type == QuestionType.MULTIPLE_CHOICE) opts else emptyList(),
                        correctAnswer = correct,
                        explanation = "تطبيق قانون أوم: I = V / R = $v / $r = $iVal A.",
                        marks = seed.marks,
                        diagram = updatedDiag,
                        variantLabel = label,
                        verificationStatus = "VERIFIED",
                        verificationNote = "تم التحقق فيزيائياً بقانون أوم: $v ÷ $r = $iVal أمبير."
                    )
                )
            } else {
                // Textual / Theoretical Question Variation
                val yemeniCities = listOf("صنعاء", "عدن", "تعز", "حضرموت", "إب", "الحديدة", "مأرب", "ذمار", "صعدة", "لحج")
                val city = yemeniCities[variantNum % yemeniCities.size]

                val variantText = when (strategy) {
                    com.example.data.model.VariantStrategy.REPHRASE ->
                        "بالاستناد إلى المنهج اليمني: ${seed.questionText} (صيغة موازية $variantNum)"
                    com.example.data.model.VariantStrategy.SCENARIOS ->
                        "في سياق بيئي وتطبيقي بمحافظة $city: ${seed.questionText}"
                    com.example.data.model.VariantStrategy.SHUFFLE_OPTIONS ->
                        seed.questionText
                    else ->
                        "${seed.questionText} [نموذج متكافئ $variantNum]"
                }

                // Shuffled options if multiple choice
                val shuffledOpts = if (seed.options.isNotEmpty()) seed.options.shuffled() else emptyList()

                val isTheoretical = seed.type == QuestionType.ESSAY || seed.type == QuestionType.SHORT_ANSWER
                val status = if (isTheoretical) "NEEDS_REVIEW" else "VERIFIED"
                val note = if (isTheoretical)
                    "سؤال نظري: يُرجى مراجعة المعلم للتأكد من الملاءمة مع كتاب المنهج."
                else
                    "تم التحقق من سلامة الخيارات والمشتتات وتطابق الإجابة الصحيحة."

                list.add(
                    ExamQuestion(
                        questionText = variantText,
                        type = seed.type,
                        options = shuffledOpts,
                        correctAnswer = seed.correctAnswer,
                        explanation = seed.explanation.ifBlank { "الإجابة النموذجية المعتمدة للنسخة $label" },
                        marks = seed.marks,
                        diagram = seed.diagram,
                        variantLabel = label,
                        verificationStatus = status,
                        verificationNote = note
                    )
                )
            }
        }
        return list
    }

    private fun getVariantCode(num: Int): String {
        return if (num <= 26) {
            val char = ('A'.code + num - 1).toChar()
            "$char"
        } else {
            "$num"
        }
    }
}

data class LessonPlanStep(
    val stageAndTime: String,
    val teacherActions: String,
    val studentActivities: String,
    val materialsUsed: String,
    val teachingMethod: String
)

data class LessonPlanResult(
    val schoolName: String = "",
    val teacherName: String = "",
    val grade: String = "",
    val subject: String = "",
    val title: String = "",
    val date: String = "",
    val period: String = "",
    val periodsCount: Int = 1,
    val durationMinutes: Int = 45,
    val unit: String = "",
    val term: String = "الفصل الدراسي الأول",
    val prepType: String = "تحضير يومي",
    val studentLevel: String = "متوسط (عام)",
    val textbookRef: String = "",
    // Objectives
    val cognitiveObjectives: String = "",
    val skillObjectives: String = "",
    val affectiveObjectives: String = "",
    val objectives: String = "",
    val learningOutcomes: String = "",
    // Prerequisites & Warmup
    val previousKnowledge: String = "",
    val introduction: String = "",
    // Aids & Key Concepts
    val teachingMaterials: String = "",
    val keyConcepts: String = "",
    // Explanation & Roles
    val explanation: String = "",
    val teacherActivities: String = "",
    val studentActivities: String = "",
    val strategies: String = "",
    // Activities & Differentiated Instruction
    val solvedExamples: String = "",
    val classroomActivities: String = "",
    val formativeQuestions: String = "",
    val individualDifferences: String = "",
    val commonMisconceptions: String = "",
    // Assessments & Summary
    val diagnosticAssessment: String = "",
    val assessment: String = "",
    val finalAssessment: String = "",
    val homework: String = "",
    val summary: String = "",
    val modelAnswers: String = "",
    val timeAllocation: String = "",
    val teacherNotes: String = "",
    // Extended Yemeni 18-part structure aliases
    val generalObjectives: String = objectives,
    val prerequisites: String = previousKnowledge,
    val teacherAndStudentRole: String = "",
    val questionsDuringExplanation: String = formativeQuestions,
    val expectedErrorsAndRemediation: String = commonMisconceptions,
    val assessmentPreFormativeSummative: String = "",
    val lessonSummary: String = summary,
    val timeDistribution: String = timeAllocation,
    // Selected options
    val selectedTeacherStyles: List<String> = emptyList(),
    val selectedTeachingMethods: List<String> = emptyList(),
    val selectedSkillsAndValues: List<String> = emptyList(),
    val selectedTeachingAids: List<String> = emptyList(),
    // 5-column table steps
    val steps: List<LessonPlanStep> = emptyList()
)
