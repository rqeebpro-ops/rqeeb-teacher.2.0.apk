package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TeacherEntity::class,
        LessonEntity::class,
        ExamEntity::class,
        BookEntity::class,
        StudentEntity::class,
        TimetableSlotEntity::class,
        HomeworkEntity::class,
        TermPlanEntity::class,
        AppSettingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RqeebDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun lessonDao(): LessonDao
    abstract fun examDao(): ExamDao
    abstract fun bookDao(): BookDao
    abstract fun studentDao(): StudentDao
    abstract fun timetableDao(): TimetableDao
    abstract fun homeworkDao(): HomeworkDao
    abstract fun termPlanDao(): TermPlanDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: RqeebDatabase? = null

        fun getDatabase(context: Context): RqeebDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RqeebDatabase::class.java,
                    "rqeeb_yemeni_educational_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }

            private suspend fun seedInitialData(database: RqeebDatabase) {
                val settingDao = database.appSettingDao()
                val teacherDao = database.teacherDao()
                val lessonDao = database.lessonDao()
                val examDao = database.examDao()
                val bookDao = database.bookDao()
                val studentDao = database.studentDao()
                val timetableDao = database.timetableDao()
                val termPlanDao = database.termPlanDao()

                // Default Owner settings & Official Yemeni Contact Details
                settingDao.setValue(AppSettingEntity("owner_email", "admin@rqeeb.edu"))
                settingDao.setValue(AppSettingEntity("owner_password", "admin123456"))
                settingDao.setValue(AppSettingEntity("owner_phone", "785502919"))
                settingDao.setValue(AppSettingEntity("owner_whatsapp_url", "https://wa.me/967785502919"))
                settingDao.setValue(AppSettingEntity("auto_approve", "true"))

                // Seed Default Approved Yemeni Teacher
                val teacherId = teacherDao.insertTeacher(
                    TeacherEntity(
                        fullName = "أ. عبد الرقيب فرج",
                        phone = "785502919",
                        email = "abdulrqeeb@rqeeb.edu",
                        passwordHash = "123456",
                        schoolName = "مدرسة جمال عبد الناصر للمتفوقين",
                        governorate = "أمانة العاصمة",
                        directorate = "مديرية التحرير",
                        subject = "الرياضيات",
                        stage = "مرحلة التعليم الأساسي",
                        gradeTaught = "الصف التاسع الأساسي",
                        curriculumEdition = "طبعة صنعاء (وزارة التربية والتعليم - صنعاء)",
                        academicYear = "1446-1447هـ / 2024-2025م",
                        status = TeacherEntity.STATUS_APPROVED
                    )
                )

                // Seed Pending Teacher from Aden for Owner Review
                teacherDao.insertTeacher(
                    TeacherEntity(
                        fullName = "أ. منى ناصر اليافعي",
                        phone = "771234567",
                        email = "mona.aden@school.ye",
                        passwordHash = "123456",
                        schoolName = "ثانوية البيحاني النموذجية",
                        governorate = "عدن",
                        directorate = "مديرية صيرة",
                        subject = "اللغة العربية",
                        stage = "مرحلة التعليم الثانوي",
                        gradeTaught = "الصف الأول الثانوي",
                        curriculumEdition = "طبعة عدن (وزارة التربية والتعليم - عدن)",
                        academicYear = "1446-1447هـ / 2024-2025م",
                        status = TeacherEntity.STATUS_PENDING
                    )
                )

                // Seed Yemeni Lesson according to Ministry standards
                lessonDao.insertLesson(
                    LessonEntity(
                        teacherId = teacherId,
                        title = "حل المعادلات التربيعية بطريقة الفرق بين مربعين",
                        subject = "الرياضيات",
                        grade = "الصف التاسع الأساسي",
                        durationMinutes = 45,
                        schoolName = "مدرسة جمال عبد الناصر للمتفوقين",
                        directorate = "مديرية التحرير",
                        governorate = "أمانة العاصمة",
                        term = "الفصل الدراسي الأول",
                        unitNumber = "الوحدة الثانية: التحليل الرياضي",
                        lessonNumber = "الدرس الثالث",
                        bookPages = "صفحة 45 إلى 49",
                        period = "الحصة الثانية",
                        objectives = "1. أن يتعرف الطالب على الصورة العامة لمقدار الفرق بين مربعين (س² - ص²).\n2. أن يحلل الطالب المقادير الجبرية المعطاة إلى عاملين بدقة.\n3. أن يطبق خطوات الحل في إيجاد جذور المعادلة التربيعية بنجاح.",
                        introduction = "التهيئة ومراجعة التعلم السابق: سؤال عصف ذهني مدخلي: إذا كان لدينا قطعة أرض مربعة طول ضلعها (س) م، واقتطعنا منها مربعاً صغيراً طول ضلعه (ص) م، فما هي المساحة المتبقية؟ وكيف نعبر عنها جبرياً؟",
                        previousKnowledge = "مفهوم المربع الكامل، الضرب القياسي للحدوديات، والعمليات الحسابية الأساسية في مجموعة الأعداد الحقيقية (ح).",
                        detailedExplanation = "القاعدة: س² - ص² = (س - ص)(س + ص).\nخطوات الحل:\n1. التأكد من وجود علامة الطرح بين حدين مربعين.\n2. فتح قوسين متطابقين أحدهما موجب والآخر سالب.\n3. أخذ الجذر التربيعي لكل حد ووضعه داخل القوسين.\nمثال محلول: حلل المقدار: 4س² - 9 = (2س - 3)(2س + 3).\nمثال حياتي: حساب تكلفة رصف فناء مدرسي بمبلغ 5000 ريال يمني.",
                        teacherActivities = "الشرح على السبورة التفاعلية، توجيه الأسئلة السابرة، متابعة مجموعات التعلم التعاوني، وتقديم الدعم للطلاب ذوي التحصيل المتوسط.",
                        studentActivities = "المشاركة في حل الأمثلة الفردية على كراس النشاط، وتبادل الكراسات مع الزميل للتصحيح الثنائي (فكر-زاوج-شارك).",
                        strategies = "التعلم البنائي، حل المشكلات، الاستقصاء الموجه، والتطبيق الحياتي.",
                        teachingMaterials = "الكتاب المدرسي المعتمد (طبعة المنهج اليمني)، السبورة التفاعلية، بطاقات التمارين.",
                        formativeAssessment = "تمرين صفي سريع: حلل في ح كلاً من: (س² - 25 = 0) ومناقشة الحلول الفردية شفوياً.",
                        finalAssessment = "تقويم ختامي من ثلاث فقرات لقياس تحقق الأهداف الثلاثة.",
                        questionsAndAnswers = "س: ما الفرق بين س² - ص² و (س - ص)²؟\nج: س² - ص² هو فرق بين مربعين، بينما (س - ص)² هو مربع كامل لمقدار ذي حدين.",
                        lessonSummary = "الفرق بين مربعين يتحلل دائماً إلى حاصل ضرب مجموع الحدين في الفرق بينهما، ويستخدم لحل المعادلات الجبرية من الدرجة الثانية.",
                        homework = "حل التمارين (1، 3، 5) صفحة 50 من كتاب الرياضيات المعتمد للجمهورية اليمنية.",
                        timeAllocation = "التهيئة والمراجعة: 5 د | الشرح ونمذجة الحلول: 20 د | النشاط الصفي الفردي والتعاوني: 12 د | التقويم الختامي والغلق: 8 د",
                        teacherNotes = "أظهر الطلاب فهماً ممتازاً مع الحاجة إلى تأكيد عدم نسيان إشارة السالب في التحليل."
                    )
                )

                // Seed Yemeni Curricula Books
                bookDao.insertBook(
                    BookEntity(
                        teacherId = teacherId,
                        title = "كتاب الرياضيات - الصف التاسع الأساسي (الجزء الأول)",
                        subject = "الرياضيات",
                        grade = "الصف التاسع الأساسي",
                        edition = "طبعة صنعاء (وزارة التربية والتعليم - صنعاء)",
                        unit = "الوحدة الأولى والثانية",
                        lesson = "المجموعات والعلاقات والتحليل",
                        pages = "1 - 120",
                        description = "المنهج الرسمي المعتمد لمادة الرياضيات للصف التاسع الأساسي في مدارس الجمهورية اليمنية.",
                        extractedText = "الوحدة الأولى: المجموعات والعلاقات والتطبيقات. الوحدة الثانية: المقادير الجبرية وتحليلها، تشمل الفرق بين مربعين، مجموع مكعبين والفرق بينهما، المقدار الثلاثي البسيط وغير البسيط، والمعادلات الكسرية وتطبيقاتها."
                    )
                )

                bookDao.insertBook(
                    BookEntity(
                        teacherId = teacherId,
                        title = "كتاب العلوم - الصف الثامن الأساسي (الجزء الأول)",
                        subject = "العلوم العامة",
                        grade = "الصف الثامن الأساسي",
                        edition = "طبعة عدن (وزارة التربية والتعليم - عدن)",
                        unit = "الوحدة الأولى: المادة والطاقة",
                        lesson = "تركيب الذرة والعناصر الكيميائية",
                        pages = "1 - 95",
                        description = "كتاب العلوم لمدارس التعليم الأساسي وفق الطبعة المعتمدة في اليمن.",
                        extractedText = "تتناول الوحدة الأولى تركيب المادة والذرة والجسيمات دون الذرية (البروتونات والنيوترونات والإلكترونات)، مع تجارب استقصائية توضح التفاعلات الكيميائية ومصادر الطاقة المتجددة في البيئة اليمنية كالرياح والطاقة الشمسية."
                    )
                )

                bookDao.insertBook(
                    BookEntity(
                        teacherId = teacherId,
                        title = "كتاب الفيزياء - الصف الثالث الثانوي (القسم العلمي)",
                        subject = "الفيزياء",
                        grade = "الصف الثالث الثانوي (علمي)",
                        edition = "طبعة صنعاء (وزارة التربية والتعليم - صنعاء)",
                        unit = "الوحدة الأولى والثانية",
                        lesson = "الحركة الدائرية والجاذبية الأرضية والكهرباء الساكنة",
                        pages = "1 - 180",
                        description = "منهج الفيزياء لشهادة الثانوية العامة بالجمهورية اليمنية.",
                        extractedText = "الحركة الدائرية المنتظمة، العجلة المركزية، القوة الجاذبة المركزية، قانون الجذب العام لنيوتن، الأقمار الصناعية وحساب سرعتها المدارية وزمنها الدوري."
                    )
                )

                // Seed Yemeni Exam
                val yemeniQuestions = listOf(
                    ExamQuestion(
                        questionText = "حلل المقدار الجبري الآتي تحليلاً كاملاً في ح: (س² - 49)",
                        type = QuestionType.MULTIPLE_CHOICE,
                        options = listOf(
                            "(س - 7)(س + 7)",
                            "(س - 7)(س - 7)",
                            "(س + 7)(س + 7)",
                            "لا يمكن تحليله"
                        ),
                        correctAnswer = "(س - 7)(س + 7)",
                        explanation = "الفرق بين مربعين هو حاصل ضرب مجموع الجذرين في الفرق بينهما.",
                        marks = 5
                    ),
                    ExamQuestion(
                        questionText = "مجموع مكعبي حدين (س³ + ص³) يساوي: (س + ص)(س² - س ص + ص²).",
                        type = QuestionType.TRUE_FALSE,
                        options = listOf("صح", "خطأ"),
                        correctAnswer = "صح",
                        explanation = "صحيح، وفق قاعدة تحليل مجموع مكعبين المعتمدة في منهج الصف التاسع.",
                        marks = 5
                    ),
                    ExamQuestion(
                        questionText = "اشترى تاجر في مدينة صنعاء بضاعة بمبلغ 80,000 ريال يمني وباعها بمبلغ 96,000 ريال يمني. ما مقدار النسبة المئوية للربح؟",
                        type = QuestionType.FILL_BLANKS,
                        options = emptyList(),
                        correctAnswer = "20%",
                        explanation = "مقدار الربح = 16,000 ريال. النسبة = (16000 / 80000) * 100 = 20%.",
                        marks = 5
                    ),
                    ExamQuestion(
                        questionText = "علل: يعتبر المقدار (س² + 9) أولياً في مجموعة الأعداد الحقيقية (ح).",
                        type = QuestionType.SHORT_ANSWER,
                        options = emptyList(),
                        correctAnswer = "لأنه مجموع مربعين ولا توجد أعداد حقيقية يكون مربعها سالباً.",
                        explanation = "مجموع المربعين لا يتحلل في مجموعة الأعداد الحقيقية.",
                        marks = 5
                    )
                )

                examDao.insertExam(
                    ExamEntity(
                        teacherId = teacherId,
                        title = "اختبار منتصف الفصل الأول في الرياضيات - الصف التاسع",
                        subject = "الرياضيات",
                        grade = "الصف التاسع الأساسي",
                        difficulty = "متوسط",
                        durationMinutes = 45,
                        totalMarks = 20,
                        examCode = "YE-901",
                        questionsJson = ExamQuestion.listToJsonString(yemeniQuestions),
                        instructions = "الجمهورية اليمنية - وزارة التربية والتعليم\nأجب عن الأسئلة بدقة ووضوح مع مراعاة الوقت المحدد.",
                        isPublishedToStudents = true
                    )
                )

                // Seed Term Plan (الخطة الفصلية)
                val sampleTermPlans = listOf(
                    TermPlanEntity(
                        teacherId = teacherId,
                        subject = "الرياضيات",
                        grade = "الصف التاسع الأساسي",
                        term = "الفصل الدراسي الأول",
                        weekNumber = 1,
                        unitTitle = "الوحدة الأولى: المجموعات والعلاقات",
                        lessonTitle = "مفهوم المجموعة والعمليات عليها (الاتحاد والتقاطع)",
                        objectives = "تمثيل المجموعات بأشكال فن وحل المسائل عليها",
                        assessmentPlan = "واجب منزلي ص 12 + بطاقة خروج سريعة"
                    ),
                    TermPlanEntity(
                        teacherId = teacherId,
                        subject = "الرياضيات",
                        grade = "الصف التاسع الأساسي",
                        term = "الفصل الدراسي الأول",
                        weekNumber = 2,
                        unitTitle = "الوحدة الأولى: المجموعات والعلاقات",
                        lessonTitle = "العلاقة والتطبيق وأنواعه (الشامل والمتباين والتقابل)",
                        objectives = "تمييز أنواع التطبيقات وإيجاد مداها ومجالها",
                        assessmentPlan = "اختبار قصير في الحصة 10 درجات"
                    ),
                    TermPlanEntity(
                        teacherId = teacherId,
                        subject = "الرياضيات",
                        grade = "الصف التاسع الأساسي",
                        term = "الفصل الدراسي الأول",
                        weekNumber = 3,
                        unitTitle = "الوحدة الثانية: المقادير الجبرية",
                        lessonTitle = "تحليل المقدار الجبري بإخراج العامل المشترك الأكبر",
                        objectives = "استخراج ع.م.أ وتطبيقه على حدوديات جبرية متنوعة",
                        assessmentPlan = "نشاط صفي تعاوني وحل تمارين الكتاب"
                    ),
                    TermPlanEntity(
                        teacherId = teacherId,
                        subject = "الرياضيات",
                        grade = "الصف التاسع الأساسي",
                        term = "الفصل الدراسي الأول",
                        weekNumber = 4,
                        unitTitle = "الوحدة الثانية: المقادير الجبرية",
                        lessonTitle = "الفرق بين مربعين وحل المعادلات التربيعية",
                        objectives = "تحليل س² - ص² وتطبيق الحل الجبري",
                        assessmentPlan = "اختبار شهري أول + تقييم الأداء الصفي"
                    )
                )
                sampleTermPlans.forEach { termPlanDao.insertTermPlan(it) }

                // Seed Students
                val sampleStudents = listOf(
                    StudentEntity(teacherId = teacherId, className = "9/1", studentName = "محمد عبد الله الحاشدي", attendanceStatus = StudentEntity.ATTENDANCE_PRESENT, quizScore = 9.5, homeworkScore = 10.0, examScore = 19.5),
                    StudentEntity(teacherId = teacherId, className = "9/1", studentName = "أسامة علي الصنعاني", attendanceStatus = StudentEntity.ATTENDANCE_PRESENT, quizScore = 8.5, homeworkScore = 9.0, examScore = 18.0),
                    StudentEntity(teacherId = teacherId, className = "9/1", studentName = "إبراهيم وليد الهمداني", attendanceStatus = StudentEntity.ATTENDANCE_PRESENT, quizScore = 9.0, homeworkScore = 9.5, examScore = 19.0),
                    StudentEntity(teacherId = teacherId, className = "9/1", studentName = "حمزة صالح الحضرمي", attendanceStatus = StudentEntity.ATTENDANCE_LATE, quizScore = 7.5, homeworkScore = 8.0, examScore = 16.0),
                    StudentEntity(teacherId = teacherId, className = "9/2", studentName = "يحيى قاسم التعزي", attendanceStatus = StudentEntity.ATTENDANCE_PRESENT, quizScore = 10.0, homeworkScore = 10.0, examScore = 20.0),
                    StudentEntity(teacherId = teacherId, className = "9/2", studentName = "بلال سالم العدني", attendanceStatus = StudentEntity.ATTENDANCE_ABSENT, quizScore = 6.5, homeworkScore = 7.0, examScore = 14.5)
                )
                sampleStudents.forEach { studentDao.insertStudent(it) }

                // Seed Timetable
                val slots = listOf(
                    TimetableSlotEntity(teacherId = teacherId, dayOfWeek = 0, periodNumber = 1, subject = "الرياضيات", className = "9/1", room = "شعبة 1"),
                    TimetableSlotEntity(teacherId = teacherId, dayOfWeek = 0, periodNumber = 2, subject = "الرياضيات", className = "9/2", room = "شعبة 2"),
                    TimetableSlotEntity(teacherId = teacherId, dayOfWeek = 1, periodNumber = 1, subject = "الرياضيات", className = "9/1", room = "شعبة 1"),
                    TimetableSlotEntity(teacherId = teacherId, dayOfWeek = 2, periodNumber = 3, subject = "نشاط علمي وتطبيقي", className = "9/1", room = "معمل العلوم"),
                    TimetableSlotEntity(teacherId = teacherId, dayOfWeek = 3, periodNumber = 2, subject = "الرياضيات", className = "9/2", room = "شعبة 2"),
                    TimetableSlotEntity(teacherId = teacherId, dayOfWeek = 4, periodNumber = 1, subject = "الرياضيات", className = "9/1", room = "شعبة 1")
                )
                slots.forEach { timetableDao.insertSlot(it) }
            }
        }
    }
}
