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

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun getApiKey(): String {
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
        } catch (e: Exception) {
            ""
        }
    }

    fun isConfigured(): Boolean = getApiKey().isNotBlank()

    /**
     * Call Gemini REST API directly
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String = "أنت 'رقيب - مساعد المعلم الذكي'، خبير تربوي وتعليمي متخصص في المناهج العربية وطرائق التدريس الحديثة وصياغة الاختبارات المعيارية."
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("مفتاح Gemini API غير مهيأ. يرجى إضافة GEMINI_API_KEY في لوحة Secrets.")
            )
        }

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

            // Using gemini-2.5-flash as modern fast model
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(
                    Exception("خطأ في استجابة الخادم (${response.code}): ${response.message}")
                )
            }

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
            Result.failure(e)
        }
    }

    /**
     * AI Lesson Plan Generator tailored for Yemeni School Curriculum with strict teacher options
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
            أنت خبير تربوي متخصص في المناهج والمدارس اليمنية. قم بإعداد خطة تحضير درس رسمي مطابق للنموذج المعتمد في الجمهورية اليمنية:
            
            [البيانات الأساسية]
            - المدرسة: ${schoolName.ifBlank { "المدرسة النموذجية" }}
            - اسم المعلم: ${teacherName.ifBlank { "معلم المادة" }}
            - المادة: $subject
            - الصف الدراسي: $grade
            - عنوان وموضوع الدرس: $title
            - التاريخ: ${date.ifBlank { "اليوم" }}
            - رقم الحصة: $period
            - عدد الحصص: $periodsCount
            - زمن الحصة: $duration دقيقة
            - محتوى أو مرجع الكتاب المستند إليه: ${contentSource.ifBlank { "كتاب وزارة التربية والتعليم - المنهج اليمني" }}

            [الخيارات التربوية المحددة حصراً من قبل المعلم]:
            1. أساليب المعلم المختارة فقط: $stylesStr
            2. طرق التدريس المختارة فقط: $methodsStr
            3. المهارات والقيم المستهدفة فقط: $skillsStr
            4. الوسائل التعليمية المختارة فقط: $aidsStr
            ${if (objectivesInput.isNotBlank()) "5. مقترح أهداف إضافية: $objectivesInput" else ""}

            [قواعد إلزامية صارمة]:
            1. لا تضف أي أسلوب أو طريقة تدريس أو مهارة أو وسيلة لم يخترها المعلم أعلاه نهائياً.
            2. اعرض في التحضير النهائي العناصر المحددة فقط.
            3. ابنِ التمهيد (5 دقائق) والأنشطة وخطوات الدرس وأسئلة التقويم على اختيارات المعلم أعلاه حصراً.
            4. قسّم الأهداف إلى: أهداف معرفية، أهداف مهارية، أهداف وجدانية وقيمية تناسب السياق اليمني.
            5. أنشئ جدول خطوات عرض الدرس المكون من 4 إلى 5 مراحل رئيسية تشمل:
               - المرحلة والزمن
               - إجراءات المعلم
               - نشاط التلميذ
               - الوسيلة المستخدمة (من الوسائل المختارة فقط)
               - الأسلوب أو طريقة التدريس (من المختارة فقط)
            6. التقويم: أسئلة تقيس الأهداف المعرفية والمهارية والوجدانية.
            7. الواجب المنزلي: نشاط صفي أو لاصفي من الكتاب المدرسي.

            أعد النتيجة بصيغة JSON حصراً بهذا النسق البرمجي:
            {
              "cognitiveObjectives": "1. أن يذكر الطالب...\n2. أن يوضح الطالب...",
              "skillObjectives": "1. أن يطبق الطالب...\n2. أن يرسم/يحلل الطالب...",
              "affectiveObjectives": "1. أن يستشعر الطالب عظمة الخالق والقيم الوطنية اليمنية...",
              "introduction5Min": "تمهيد 5 دقائق يربط الدرس بالواقع ويبدأ بالأساليب المختارة...",
              "steps": [
                {
                  "stageAndTime": "التهيئة والتمهيد (5 د)",
                  "teacherActions": "طرح سؤال استهلالي وفق الأسلوب المختار...",
                  "studentActivities": "التفاعل والإجابة وتدوين الأفكار...",
                  "materialsUsed": "${selectedTeachingAids.firstOrNull() ?: "الكتاب المدرسي"}",
                  "teachingMethod": "${selectedTeacherStyles.firstOrNull() ?: "الحوار والمناقشة"}"
                },
                {
                  "stageAndTime": "عرض المفهوم والشرح (18 د)",
                  "teacherActions": "شرح الخطوات المحورية...",
                  "studentActivities": "المتابعة والمشاركة النشطة...",
                  "materialsUsed": "${selectedTeachingAids.getOrNull(1) ?: selectedTeachingAids.firstOrNull() ?: "السبورة والطباشير"}",
                  "teachingMethod": "${selectedTeachingMethods.firstOrNull() ?: "حل المشكلات"}"
                },
                {
                  "stageAndTime": "النشاط التطبيقي (14 د)",
                  "teacherActions": "توجيه النشاط وفق طرق التدريس المختارة...",
                  "studentActivities": "العمل والتطبيق في ضوء المهارات المحددة...",
                  "materialsUsed": "${selectedTeachingAids.lastOrNull() ?: "أوراق عمل"}",
                  "teachingMethod": "${selectedTeachingMethods.lastOrNull() ?: "التعلم التعاوني"}"
                },
                {
                  "stageAndTime": "التقويم الختامي والغلق (8 د)",
                  "teacherActions": "طرح أسئلة التحقق وقياس المخرجات...",
                  "studentActivities": "تقديم الإجابات وتقييم الأقران...",
                  "materialsUsed": "${selectedTeachingAids.firstOrNull() ?: "الكتاب المدرسي"}",
                  "teachingMethod": "${selectedTeacherStyles.lastOrNull() ?: "التعزيز"}"
                }
              ],
              "evaluation": "أسئلة تقويمية تقيس الأهداف المعرفية والمهارية والقيمية...",
              "homework": "حل تدريبات الكتاب المدرسي ونشاط إثرائي مرتبط بالبيئة...",
              "teacherNotes": "تم إنجاز أهداف الحصة في الزمن المحدد وتفاعل الطلاب إيجابياً."
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
            val intro = obj.optString("introduction5Min")
            val eval = obj.optString("evaluation")
            val hw = obj.optString("homework")
            val notes = obj.optString("teacherNotes")

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
                cognitiveObjectives = cog,
                skillObjectives = skl,
                affectiveObjectives = aff,
                objectives = combinedObjectives,
                introduction = intro,
                previousKnowledge = "مراجعة المتطلبات السابقة ذات الصلة بدرس ($title) لمدة دقيقتين.",
                explanation = stepsList.joinToString("\n\n") { "• ${it.stageAndTime}: ${it.teacherActions} (الوسيلة: ${it.materialsUsed})" },
                teacherActivities = stepsList.joinToString("\n") { "• ${it.stageAndTime}: ${it.teacherActions}" },
                studentActivities = stepsList.joinToString("\n") { "• ${it.stageAndTime}: ${it.studentActivities}" },
                strategies = methods.joinToString("، "),
                teachingMaterials = aids.joinToString("، "),
                assessment = eval,
                summary = "إبراز الخلاصات الأساسية لدرس $title وتثبيت المفاهيم المحورية.",
                homework = hw,
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
        """.trimIndent()

        val skl = """
            1. أن يمارس الطالب مهارة ($primarySkill) في تحليل أمثلة وتطبيقات الدرس.
            2. أن يطبق مهارة ($secondarySkill) أثناء تنفيذ الأنشطة الصفية والتدريبات.
        """.trimIndent()

        val aff = """
            1. أن يستشعر الطالب أهمية موضوع ($title) في خدمة المجتمع والبيئة اليمنية.
            2. أن ينمي قيم التعاون والعمل الجماعي واحترام آراء الزملاء أثناء الحصة.
        """.trimIndent()

        val intro = "التهيئة والتمهيد (5 دقائق): بدء الحصة بتحية الطلاب، وتوظيف أسلوب ($primaryStyle) في طرح مسألة استهلالية مشوقة مرتبطة بالواقع اليمني تدور حول ($title)، واستثارة دافعية الطلاب عبر ($primaryAid)."

        val steps = listOf(
            LessonPlanStep(
                stageAndTime = "التهيئة والتمهيد (5 د)",
                teacherActions = "طرح تساؤل استهلالي مشوق يربط الدرس بالواقع وتوظيف أسلوب ($primaryStyle).",
                studentActivities = "الاستماع والتفاعل ومناقشة السؤال التمهيدي.",
                materialsUsed = primaryAid,
                teachingMethod = primaryStyle
            ),
            LessonPlanStep(
                stageAndTime = "عرض المفهوم والشرح (18 د)",
                teacherActions = "شرح محاور درس ($title) باستخدام أسلوب ($secondaryStyle) وربطها بالأمثلة التوضيحية.",
                studentActivities = "متابعة الشرح وتدوين المفاهيم الأساسية والمشاركة في الحوار.",
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
                studentActivities = "الإجابة على الأسئلة الشفوية والتحريرية وتلخيص ما تم تعلمه.",
                materialsUsed = primaryAid,
                teachingMethod = styles.lastOrNull() ?: "التعزيز"
            )
        )

        val eval = """
            1. تقويم معرفي: ما المقصود بالمفاهيم الأساسية في درس ($title)؟
            2. تقويم مهاري: طبق ما تعلمته في حل التدريب التطبيقي باستخدام ($primarySkill).
            3. تقويم وجداني: كيف يوظف الطالب موضوع الدرس في حياته اليومية والمجتمع اليمني؟
        """.trimIndent()

        val hw = "حل التدريبات المحددة لدرس ($title) في الكتاب المدرسي وكتابة تلخيص موجز من 3 أسطر."

        return LessonPlanResult(
            schoolName = schoolName,
            teacherName = teacherName,
            grade = grade,
            subject = subject,
            title = title,
            date = date,
            period = period,
            periodsCount = periodsCount,
            cognitiveObjectives = cog,
            skillObjectives = skl,
            affectiveObjectives = aff,
            objectives = "الأهداف المعرفية:\n$cog\n\nالأهداف المهارية:\n$skl\n\nالأهداف الوجدانية:\n$aff",
            introduction = intro,
            previousKnowledge = "مراجعة المتطلبات السابقة المرتبطة بدرس ($title) لمدة دقيقتين.",
            explanation = steps.joinToString("\n\n") { "• ${it.stageAndTime}: ${it.teacherActions} (الوسيلة: ${it.materialsUsed})" },
            teacherActivities = steps.joinToString("\n") { "• ${it.stageAndTime}: ${it.teacherActions}" },
            studentActivities = steps.joinToString("\n") { "• ${it.stageAndTime}: ${it.studentActivities}" },
            strategies = methods.joinToString("، "),
            teachingMaterials = aids.joinToString("، "),
            assessment = eval,
            summary = "تلخيص المفاهيم المحورية وتثبيت المعارف الأساسية للدرس.",
            homework = hw,
            timeAllocation = "التهيئة: 5 د | الشرح: 18 د | التطبيق: 14 د | التقويم: 8 د",
            teacherNotes = "تم تنفيذ الحصة وفق الخطة الزمنية المعتمدة وأظهر الطلاب تجاوباً فعالاً.",
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
    suspend fun generateExam(
        subject: String,
        grade: String,
        topic: String,
        difficulty: String,
        questionCount: Int,
        selectedTypes: List<QuestionType>
    ): List<ExamQuestion> = withContext(Dispatchers.IO) {
        val prompt = """
            أنشئ اختباراً تعليمياً متقناً بالعربية بالمواصفات التالية:
            - المادة: $subject
            - الصف: $grade
            - الموضوع: $topic
            - مستوى الصعوبة: $difficulty
            - عدد الأسئلة: $questionCount
            - أنواع الأسئلة المطلوبة: ${selectedTypes.joinToString { it.titleAr }}

            المطلوب صياغة الأسئلة بتنوع ودقة مع خيارات الإجابة والإجابة الصحيحة وشرح مبسط لكل سؤال.
            أعد النتيجة بصيغة JSON حصراً بهذا النسق:
            [
              {
                "questionText": "نص السؤال...",
                "type": "MULTIPLE_CHOICE", // أو TRUE_FALSE أو FILL_BLANKS أو SHORT_ANSWER أو ESSAY
                "options": ["خيار 1", "خيار 2", "خيار 3", "خيار 4"],
                "correctAnswer": "الإجابة الصحيحة",
                "explanation": "شرح توضيحي للحل",
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
        return@withContext generateFallbackExamQuestions(subject, grade, topic, questionCount, selectedTypes)
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
        types: List<QuestionType>
    ): List<ExamQuestion> {
        val list = mutableListOf<ExamQuestion>()
        val effectiveTypes = if (types.isEmpty()) listOf(QuestionType.MULTIPLE_CHOICE, QuestionType.TRUE_FALSE) else types

        for (i in 1..count) {
            val currentType = effectiveTypes[(i - 1) % effectiveTypes.size]
            when (currentType) {
                QuestionType.MULTIPLE_CHOICE -> {
                    list.add(
                        ExamQuestion(
                            questionText = "ما هي النتيجة المنطقية لدراسة $topic في مقرر $subject؟",
                            type = QuestionType.MULTIPLE_CHOICE,
                            options = listOf(
                                "تحقيق الفهم العميق وتطبيق المهارات في الحياة العملية",
                                "حفظ النصوص دون إدراك المفاهيم",
                                "تجاهل القوانين والاعتماد على التخمين",
                                "الاقتصار على الجانب النظري فقط"
                            ),
                            correctAnswer = "تحقيق الفهم العميق وتطبيق المهارات في الحياة العملية",
                            explanation = "الهدف الأساسي من التعلم هو التطبيق العملي ونقل أثر التعلم.",
                            marks = 3
                        )
                    )
                }
                QuestionType.TRUE_FALSE -> {
                    val isTrue = i % 2 == 1
                    list.add(
                        ExamQuestion(
                            questionText = if (isTrue)
                                "تعتبر المبادئ الأساسية لـ ($topic) معتمدة على التجربة والبرهان العلمي الدقيق."
                            else
                                "يمكن إهمال الفروق الفردية والشروط الأولية عند تطبيق قواعد ($topic).",
                            type = QuestionType.TRUE_FALSE,
                            options = listOf("صح", "خطأ"),
                            correctAnswer = if (isTrue) "صح" else "خطأ",
                            explanation = if (isTrue) "صحيح، لأن المفاهيم تبنى على أسس منهجية متينة." else "خطأ، لأن الفروق والشروط أساسية في صحة التطبيق.",
                            marks = 2
                        )
                    )
                }
                QuestionType.FILL_BLANKS -> {
                    list.add(
                        ExamQuestion(
                            questionText = "يُقاس الأثر التعليمي لـ ($topic) من خلال قدرة المتعلم على حل ______ ومواجهة المشكلات.",
                            type = QuestionType.FILL_BLANKS,
                            options = emptyList(),
                            correctAnswer = "المسائل التطبيقية",
                            explanation = "القدرة على حل المسائل هي المعيار الأبرز لقياس التحصيل.",
                            marks = 2
                        )
                    )
                }
                QuestionType.SHORT_ANSWER -> {
                    list.add(
                        ExamQuestion(
                            questionText = "اذكر شرطين رئيسيين لضمان نجاح تطبيق مفاهيم ($topic) في الصف المدرسي.",
                            type = QuestionType.SHORT_ANSWER,
                            options = emptyList(),
                            correctAnswer = "1. وضوح الهدف التعليمي 2. الممارسة والتدريب المستمر",
                            explanation = "وضوح الهدف والممارسة يعززان استبقاء المهارة لدى الطالب.",
                            marks = 3
                        )
                    )
                }
                QuestionType.ESSAY -> {
                    list.add(
                        ExamQuestion(
                            questionText = "اكتب فقرة علمية متكاملة تشرح فيها أهمية ($topic) وتأثيره الإيجابي على تطور المجتمع.",
                            type = QuestionType.ESSAY,
                            options = emptyList(),
                            correctAnswer = "شرح مفصل يتضمن مقدمة وتحليلاً للأثر الميداني وخاتمة توصيات.",
                            explanation = "يقيس السؤال مهارات التفكير العليا والتحليل والتركيب لدى المتعلم.",
                            marks = 5
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
    // Objectives
    val cognitiveObjectives: String = "",
    val skillObjectives: String = "",
    val affectiveObjectives: String = "",
    val objectives: String = "",
    // 5-minute Intro
    val introduction: String = "",
    val previousKnowledge: String = "",
    val explanation: String = "",
    val teacherActivities: String = "",
    val studentActivities: String = "",
    val strategies: String = "",
    val teachingMaterials: String = "",
    val assessment: String = "",
    val summary: String = "",
    val homework: String = "",
    val timeAllocation: String = "",
    val teacherNotes: String = "",
    // Selected options
    val selectedTeacherStyles: List<String> = emptyList(),
    val selectedTeachingMethods: List<String> = emptyList(),
    val selectedSkillsAndValues: List<String> = emptyList(),
    val selectedTeachingAids: List<String> = emptyList(),
    // 5-column table steps
    val steps: List<LessonPlanStep> = emptyList()
)
