package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    version = 4,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE teachers ADD COLUMN section TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): RqeebDatabase = getDatabase(context)

        fun getDatabase(context: Context): RqeebDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RqeebDatabase::class.java,
                    "rqeeb_yemeni_educational_db"
                )
                    .addMigrations(MIGRATION_3_4)
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

                // Default Owner settings & Official Yemeni Contact Details
                settingDao.setValue(AppSettingEntity("owner_email", YemeniCurriculum.OWNER_EMAIL_DEFAULT))
                settingDao.setValue(AppSettingEntity("owner_password", "admin123456"))
                settingDao.setValue(AppSettingEntity("owner_phone", YemeniCurriculum.OWNER_PHONE_DEFAULT))
                settingDao.setValue(AppSettingEntity("owner_whatsapp_url", YemeniCurriculum.OWNER_WHATSAPP_URL_DEFAULT))
                settingDao.setValue(AppSettingEntity("auto_approve", "false"))

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
                        gradeTaught = "9/ إعدادي",
                        section = "أ",
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
                        gradeTaught = "10/ ثانوي",
                        section = "ب",
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
                        grade = "9/ إعدادي",
                        durationMinutes = 45,
                        schoolName = "مدرسة جمال عبد الناصر للمتفوقين",
                        directorate = "مديرية التحرير",
                        governorate = "أمانة العاصمة",
                        term = "الفصل الدراسي الأول",
                        unitName = "الوحدة الأولى: التحليل الرياضي",
                        lessonDate = "1446/04/15هـ",
                        periodNumber = "الحصة الثالثة",
                        periodsCount = 1,
                        studentLevel = "متوسط / اعتيادي",
                        preparationType = "تحضير يومي قياسي",
                        cognitiveObjectives = "1. أن يتعرف الطالب على الصورة العامة للفرق بين مربعين أ² - ب².\n2. أن يستنتج الطالب قاعدة التحليل (أ - ب)(أ + ب).\n3. أن يحل الطالب معادلات تربيعية بسيطة بدقة.",
                        skillObjectives = "1. تمييز الحدود التي تمثل مربعاً كاملاً.\n2. تطبيق خطوات التحليل بالترتيب الصحيح.\n3. التحقق من صحة الحل بالضرب العكسي.",
                        affectiveObjectives = "1. تقدير أهمية الرياضيات في حل المشكلات الحياتية.\n2. المشاركة الإيجابية في الأنشطة الصفية والتعاون مع الزملاء.",
                        introduction = "مراجعة مفهوم المربع الكامل ومساحة المربع (طول الضلع في نفسه)، ثم طرح مسألة استكشافية: إذا كانت س² = 25 فما قيم س الممكنة؟",
                        teachingSteps = "الخطوة 1: عرض أمثلة توضيحية على السبورة.\nالخطوة 2: استنتاج القاعدة الرياضية وتدوينها.\nالخطوة 3: حل تدريب فردي بمتابعة المعلم.\nالخطوة 4: نشاط تعاوني بين المجموعات لحل مسائل متقدمة.",
                        formativeAssessment = "سؤال سريع للتقويم التكويني: حلل المقدار: 4س² - 49.",
                        homework = "تمارين الكتاب المدرسي صفحة 45، المسائل رقم (1، 3، 5).",
                        teacherNotes = "لوحظ تفاعل ممتاز وتفوق ملحوظ في شعبة (أ).",
                        teacherStyles = "الحوار والمناقشة، التعزيز الإيجابي",
                        teachingMethods = "العصف الذهني، التعلم التعاوني",
                        skillsAndValues = "التفكير الناقد والتحليل، التعاون",
                        teachingAids = "السبورة الملونة، الكتاب المدرسي",
                        textbookReference = "كتاب الرياضيات - الصف التاسع الأساسي - ص 42-45",
                        curriculumEdition = "طبعة صنعاء (وزارة التربية والتعليم - صنعاء)"
                    )
                )

                // Seed Yemeni Exam
                examDao.insertExam(
                    ExamEntity(
                        teacherId = teacherId,
                        title = "اختبار منتصف الفصل الدراسي الأول في الرياضيات",
                        subject = "الرياضيات",
                        grade = "9/ إعدادي",
                        examType = "اختبار شهري",
                        durationMinutes = 45,
                        totalScore = 20,
                        schoolName = "مدرسة جمال عبد الناصر للمتفوقين",
                        term = "الفصل الدراسي الأول",
                        academicYear = "1446-1447هـ",
                        questionsJson = "[]"
                    )
                )

                // Seed Official Yemeni Books
                val sampleBooks = listOf(
                    BookEntity(
                        title = "الرياضيات - الجزء الأول",
                        subject = "الرياضيات",
                        grade = "9/ إعدادي",
                        edition = "طبعة صنعاء",
                        summary = "منهج وزارة التربية والتعليم - يتضمن وحدات التحليل، المعادلات، والمجموعات",
                        unitsCount = 5
                    ),
                    BookEntity(
                        title = "اللغة العربية (المطالعة والنصوص)",
                        subject = "اللغة العربية",
                        grade = "9/ إعدادي",
                        edition = "طبعة صنعاء",
                        summary = "مختارات أدبية وقواعد لغوية وبلاغة",
                        unitsCount = 6
                    ),
                    BookEntity(
                        title = "العلوم والحياة",
                        subject = "العلوم",
                        grade = "9/ إعدادي",
                        edition = "طبعة صنعاء",
                        summary = "الفيزياء والكيمياء والأحياء الأساسية لطلاب المرحلة الإعدادية",
                        unitsCount = 6
                    ),
                    BookEntity(
                        title = "القرآن الكريم وعلومه",
                        subject = "القرآن الكريم",
                        grade = "9/ إعدادي",
                        edition = "المنهج العام الموحد",
                        summary = "السور المقررة حفظاً وتلاوة وأحكام التجويد",
                        unitsCount = 4
                    )
                )
                bookDao.insertBooks(sampleBooks)
            }
        }
    }
}
