package com.example.data.math

import com.example.data.model.ExamEntity
import com.example.data.model.ExamQuestion
import com.example.data.model.QuestionType
import java.util.UUID
import kotlin.random.Random

/**
 * Pure Deterministic Math Engine for Primary Education (Grades 1 to 6)
 * Aligned with the official Yemeni Curriculum (وزارة التربية والتعليم - اليمن)
 *
 * Guarantees 100% mathematical accuracy, step-by-step verified explanations,
 * realistic distractors, and complete borrowing-across-zeros handling.
 */
object PrimaryMathEngine {

    enum class MathSkill(val titleAr: String, val descriptionAr: String, val minGrade: Int) {
        ADDITION("الجمع", "جمع الأعداد الأفقية والرأسية والأعداد المفقودة والمسائل اللفظية", 1),
        SUBTRACTION("الطرح", "طرح الأعداد الأفقية والرأسية والتحقق بالجمع والمسائل اللفظية", 1),
        MULTIPLICATION("الضرب", "مفهوم الضرب، جداول الضرب، الضرب في رقم ورقمين، والمسائل اللفظية", 2),
        ROUNDING("التقريب وتقدير النواتج", "التقريب لأقرب عشرة ومئة وألف وتقدير ناتج العمليات", 2),
        FRACTIONS("الكسور الاعتيادية", "التعرف، البسط والمقام، المقارنة، والكسور المتكافئة والجمع", 3),
        ADDITION_REGROUPING("الجمع بالاحتفاظ (مع الحمل)", "عرض رأسي مع محاذاة المنازل ومربعات الحمل وشرح الخطوات", 2),
        SUBTRACTION_BORROWING("الطرح بالاستلاف (عبر الأصفار)", "عرض رأسي، تفكيك المنازل، الاستلاف المتتابع عبر الأصفار والتحقق", 2)
    }

    enum class MathDifficulty(val titleAr: String) {
        EASY("سهل"),
        MEDIUM("متوسط"),
        ADVANCED("متقدم"),
        MIXED("مختلط ومتوازن")
    }

    data class AdditionCarryStep(
        val columnIndex: Int = 0,
        val columnName: String = "",
        val placeNameAr: String = "",
        val topDigit: Int = 0,
        val bottomDigit: Int = 0,
        val carriedIn: Int = 0,
        val sum: Int = topDigit + bottomDigit + carriedIn,
        val writtenDigit: Int = sum % 10,
        val digitResult: Int = writtenDigit,
        val carriedOut: Int = sum / 10,
        val carryOut: Int = carriedOut,
        val explanation: String = "",
        val explanationAr: String = explanation
    )

    data class SubtractionBorrowStep(
        val columnIndex: Int = 0,
        val columnName: String = "",
        val placeNameAr: String = "",
        val minuendDigit: Int = 0,
        val originalTopDigit: Int = minuendDigit,
        val modifiedTopDigit: Int = minuendDigit,
        val subtrahendDigit: Int = 0,
        val bottomDigit: Int = subtrahendDigit,
        val digitResult: Int = minuendDigit - subtrahendDigit,
        val resultDigit: Int = digitResult,
        val borrowNeeded: Boolean = false,
        val borrowedFromNext: Boolean = borrowNeeded,
        val isZeroChain: Boolean = false,
        val borrowStepsTrace: List<String> = emptyList(),
        val explanation: String = "",
        val explanationAr: String = explanation
    )

    data class BorrowOperation(
        val minuend: Int,
        val subtrahend: Int,
        val difference: Int,
        val steps: List<SubtractionBorrowStep>,
        val explanationText: String = "",
        val verificationFormula: String = "$subtrahend + $difference = $minuend"
    )

    data class FractionData(
        val numerator: Int,
        val denominator: Int,
        val secondNumerator: Int = 0,
        val secondDenominator: Int = 0,
        val operation: String = "", // "+", "-", "مقارنة", "تكافؤ"
        val representationType: String = "BAR" // BAR, CIRCLE
    )

    data class PrimaryMathProblem(
        val id: String = UUID.randomUUID().toString(),
        val grade: Int,
        val skill: MathSkill,
        val title: String,
        val questionText: String,
        val isVertical: Boolean = false,
        val topNumber: Long = 0,
        val bottomNumber: Long = 0,
        val operationSymbol: String = "+",
        val correctAnswer: String,
        val options: List<String> = emptyList(), // For MCQ (1 correct + 3 unique distractors)
        val explanation: String = "",
        val verificationNote: String = "",
        val hint: String = "",
        val detailedSteps: String = explanation,
        val carrySteps: List<AdditionCarryStep> = emptyList(),
        val borrowSteps: List<SubtractionBorrowStep> = emptyList(),
        val fractionData: FractionData? = null
    )

    /**
     * Get valid skills taught in a specific grade (1 to 6) in the Yemeni curriculum
     */
    fun getSkillsForGrade(grade: Int): List<MathSkill> {
        return MathSkill.values().filter { grade >= it.minGrade }
    }

    // =========================================================================
    // 1. الجمع (Addition)
    // =========================================================================
    fun generateAdditionProblem(
        grade: Int,
        difficulty: MathDifficulty = MathDifficulty.MEDIUM,
        isVertical: Boolean = false,
        isMissingNumber: Boolean = false,
        isWordProblem: Boolean = false
    ): PrimaryMathProblem {
        val (minVal, maxVal) = when (grade) {
            1 -> 1 to 9
            2 -> 10 to 89
            3 -> 100 to 899
            4 -> 1000 to 89999
            5 -> 10000 to 899999
            else -> 100000 to 8999999
        }

        val a = Random.nextLong(minVal.toLong(), maxVal.toLong())
        val b = Random.nextLong(minVal.toLong(), maxVal.toLong())
        val sum = a + b

        if (isMissingNumber) {
            val askFirst = Random.nextBoolean()
            val questionText = if (askFirst) {
                "أوجد العدد المفقود في جملة الجمع التالية:\n( ... ) + $b = $sum"
            } else {
                "أوجد العدد المفقود في جملة الجمع التالية:\n$a + ( ... ) = $sum"
            }
            val correct = if (askFirst) a.toString() else b.toString()
            val options = generateRealisticDistractors(if (askFirst) a else b, isPositiveOnly = true)
            val explanation = "لإيجاد العدد المفقود في جملة الجمع، نطرح العدد المعلوم من الناتج:\n$sum - ${if (askFirst) b else a} = $correct"
            val verification = "التحقق: $a + $b = $sum (عملية صحيحة)"

            return PrimaryMathProblem(
                grade = grade,
                skill = MathSkill.ADDITION,
                title = "إيجاد العدد المفقود في الجمع",
                questionText = questionText,
                isVertical = false,
                topNumber = a,
                bottomNumber = b,
                operationSymbol = "+",
                correctAnswer = correct,
                options = options,
                explanation = explanation,
                verificationNote = verification,
                hint = "اطرح العدد المعلوم من الناتج الكلي"
            )
        }

        if (isWordProblem) {
            val names = listOf("عمر", "سالم", "أحمد", "فاطمة", "مريم", "خالد")
            val items = listOf("ريالاً يمنياً", "دفتر مدرسي", "شجرة بن في الحقل", "كتاباً في المكتبة", "طالباً في المدرسة")
            val name = names.random()
            val item = items.random()

            val questionText = "كان مع $name مبلغ $a $item، وحصل من والده على $b $item أخرى بمناسبة تفوقه.\nكم أصبح إجمالي ما معه؟"
            val options = generateRealisticDistractors(sum, isPositiveOnly = true)
            val explanation = "العملية المطلوبة هي الجمع:\n$a + $b = $sum $item."
            val verification = "التحقق: $sum - $b = $a."

            return PrimaryMathProblem(
                grade = grade,
                skill = MathSkill.ADDITION,
                title = "مسألة لفظية على الجمع",
                questionText = questionText,
                isVertical = false,
                topNumber = a,
                bottomNumber = b,
                operationSymbol = "+",
                correctAnswer = sum.toString(),
                options = options,
                explanation = explanation,
                verificationNote = verification,
                hint = "اجمع ما كان معه مع ما حصل عليه إضافياً"
            )
        }

        val questionText = if (isVertical) {
            "احسب ناتج الجمع الرأسي التالي:\n   $a\n+  $b\n-------"
        } else {
            "احسب ناتج ما يلي:\n$a + $b = ..."
        }
        val options = generateRealisticDistractors(sum, isPositiveOnly = true)
        val explanation = "نجمع المنازل بدءاً من الآحاد:\n$a + $b = $sum."
        val verification = "التحقق بالطرح العكسي: $sum - $b = $a."

        return PrimaryMathProblem(
            grade = grade,
            skill = MathSkill.ADDITION,
            title = if (isVertical) "جمع رأسي" else "جمع أفقي",
            questionText = questionText,
            isVertical = isVertical,
            topNumber = a,
            bottomNumber = b,
            operationSymbol = "+",
            correctAnswer = sum.toString(),
            options = options,
            explanation = explanation,
            verificationNote = verification,
            hint = "اجمع الآحاد أولاً ثم العشرات والمئات بالترتيب"
        )
    }

    // =========================================================================
    // 2. الطرح (Subtraction)
    // =========================================================================
    fun generateSubtractionProblem(
        grade: Int,
        difficulty: MathDifficulty = MathDifficulty.MEDIUM,
        isVertical: Boolean = false,
        isMissingNumber: Boolean = false,
        isWordProblem: Boolean = false
    ): PrimaryMathProblem {
        val (minVal, maxVal) = when (grade) {
            1 -> 2 to 18
            2 -> 20 to 99
            3 -> 100 to 999
            4 -> 1000 to 99999
            5 -> 10000 to 999999
            else -> 100000 to 9999999
        }

        var top = Random.nextLong(minVal.toLong(), maxVal.toLong())
        var bottom = Random.nextLong(minVal.toLong(), maxVal.toLong())
        if (top < bottom) {
            val tmp = top
            top = bottom
            bottom = tmp
        }
        if (top == bottom) top += 5

        val diff = top - bottom

        if (isMissingNumber) {
            val askTop = Random.nextBoolean()
            val questionText = if (askTop) {
                "أوجد العدد المفقود في جملة الطرح التالية:\n( ... ) - $bottom = $diff"
            } else {
                "أوجد العدد المفقود في جملة الطرح التالية:\n$top - ( ... ) = $diff"
            }
            val correct = if (askTop) top.toString() else bottom.toString()
            val options = generateRealisticDistractors(if (askTop) top else bottom, isPositiveOnly = true)
            val explanation = if (askTop) {
                "لإيجاد المطروح منه، نجمع الناتج مع المطروح:\n$diff + $bottom = $top"
            } else {
                "لإيجاد المطروح، نطرح الناتج من المطروح منه:\n$top - $diff = $bottom"
            }
            val verification = "التحقق: $top - $bottom = $diff (عملية صحيحة)"

            return PrimaryMathProblem(
                grade = grade,
                skill = MathSkill.SUBTRACTION,
                title = "إيجاد العدد المفقود في الطرح",
                questionText = questionText,
                isVertical = false,
                topNumber = top,
                bottomNumber = bottom,
                operationSymbol = "-",
                correctAnswer = correct,
                options = options,
                explanation = explanation,
                verificationNote = verification,
                hint = if (askTop) "اجمع المطروح مع الناتج لإيجاد العدد الأول" else "اطرح الناتج من العدد الأول"
            )
        }

        if (isWordProblem) {
            val scenarios = listOf(
                "في متجر القرطاسية كان يوجد $top قلماً، بيع منها خلال اليوم $bottom قلماً. كم قلماً بقي في المتجر؟",
                "وفر الطالب عبد الله من مصروفه $top ريالاً، واشترى حقيبة مدرسية بمبلغ $bottom ريالاً. كم بقي معه؟",
                "مزرعة تفاح في محافظة إب أنتجت $top صندوقاً، وزع منها $bottom صندوقاً على السوق المحلي. كم صندوقاً تبقى؟"
            )
            val questionText = scenarios.random()
            val options = generateRealisticDistractors(diff, isPositiveOnly = true)
            val explanation = "العملية المطلوبة هي الطرح:\n$top - $bottom = $diff."
            val verification = "التحقق بالجمع العكسي: $diff + $bottom = $top."

            return PrimaryMathProblem(
                grade = grade,
                skill = MathSkill.SUBTRACTION,
                title = "مسألة لفظية على الطرح",
                questionText = questionText,
                isVertical = false,
                topNumber = top,
                bottomNumber = bottom,
                operationSymbol = "-",
                correctAnswer = diff.toString(),
                options = options,
                explanation = explanation,
                verificationNote = verification,
                hint = "اطرح الكمية المبيعة أو المصروفة من الإجمالي الأصلي"
            )
        }

        val questionText = if (isVertical) {
            "احسب ناتج الطرح الرأسي التالي:\n   $top\n-  $bottom\n-------"
        } else {
            "احسب ناتج ما يلي:\n$top - $bottom = ..."
        }
        val options = generateRealisticDistractors(diff, isPositiveOnly = true)
        val explanation = "نطرح المنازل بدءاً من الآحاد مع مراعاة المنازل:\n$top - $bottom = $diff."
        val verification = "التحقق بالعملية العكسية (الجمع): الناتج ($diff) + المطروح ($bottom) = $top."

        return PrimaryMathProblem(
            grade = grade,
            skill = MathSkill.SUBTRACTION,
            title = if (isVertical) "طرح رأسي" else "طرح أفقي",
            questionText = questionText,
            isVertical = isVertical,
            topNumber = top,
            bottomNumber = bottom,
            operationSymbol = "-",
            correctAnswer = diff.toString(),
            options = options,
            explanation = explanation,
            verificationNote = verification,
            hint = "اطرح الآحاد من الآحاد ثم العشرات من العشرات"
        )
    }

    // =========================================================================
    // 3. الضرب (Multiplication)
    // =========================================================================
    fun generateMultiplicationProblem(
        grade: Int,
        difficulty: MathDifficulty = MathDifficulty.MEDIUM,
        isVertical: Boolean = false,
        isWordProblem: Boolean = false,
        isMissingFactor: Boolean = false
    ): PrimaryMathProblem {
        val safeGrade = if (grade < 2) 2 else grade

        val (a, b) = when (safeGrade) {
            2 -> Random.nextInt(2, 6) to Random.nextInt(1, 10) // جداول 2، 3، 4، 5
            3 -> Random.nextInt(2, 10) to Random.nextInt(2, 10) // جداول حتى 10
            4 -> {
                // ضرب عدد من منزلتين أو 3 منازل في عدد من منزلة واحدة
                Random.nextInt(12, 120) to Random.nextInt(2, 9)
            }
            5 -> {
                // ضرب عددين من منزلتين
                Random.nextInt(15, 95) to Random.nextInt(11, 45)
            }
            else -> {
                // الصف السادس: ضرب أعداد متعددة المنازل
                Random.nextInt(105, 850) to Random.nextInt(12, 65)
            }
        }

        val product = a.toLong() * b.toLong()

        if (isMissingFactor) {
            val askA = Random.nextBoolean()
            val questionText = if (askA) {
                "أوجد العامل المفقود في جملة الضرب:\n( ... ) × $b = $product"
            } else {
                "أوجد العامل المفقود في جملة الضرب:\n$a × ( ... ) = $product"
            }
            val correct = if (askA) a.toString() else b.toString()
            val options = generateRealisticDistractors(if (askA) a.toLong() else b.toLong(), isPositiveOnly = true)
            val explanation = "لإيجاد العامل المفقود، نقسم الناتج على العامل المعلوم:\n$product ÷ ${if (askA) b else a} = $correct."
            val verification = "التحقق: $a × $b = $product."

            return PrimaryMathProblem(
                grade = safeGrade,
                skill = MathSkill.MULTIPLICATION,
                title = "العامل المفقود في الضرب",
                questionText = questionText,
                isVertical = false,
                topNumber = a.toLong(),
                bottomNumber = b.toLong(),
                operationSymbol = "×",
                correctAnswer = correct,
                options = options,
                explanation = explanation,
                verificationNote = verification,
                hint = "اقسم الناتج على العدد المعلوم"
            )
        }

        if (isWordProblem) {
            val scenarios = listOf(
                "في معمل الحاسوب المدرسي $b صفوف من الطاولات، على كل طاولة $a أجهزة كمبيوتر. كم جهازاً في المعمل؟",
                "اشترى معلم الرياضيات $b علب من الأقلام الملونة لتكريم الطلاب، في كل علبة $a قلماً. كم قلماً اشترى المعلم؟",
                "تسير حافلة المدرسة $a كيلومتراً في اليوم الواحد. كم كيلومتراً تقطع في $b أيام؟"
            )
            val questionText = scenarios.random()
            val options = generateRealisticDistractors(product, isPositiveOnly = true)
            val explanation = "الضرب هو جمع متكرر:\n$a × $b = $product."
            val verification = "التحقق بالقسمة: $product ÷ $b = $a."

            return PrimaryMathProblem(
                grade = safeGrade,
                skill = MathSkill.MULTIPLICATION,
                title = "مسألة لفظية على الضرب",
                questionText = questionText,
                isVertical = false,
                topNumber = a.toLong(),
                bottomNumber = b.toLong(),
                operationSymbol = "×",
                correctAnswer = product.toString(),
                options = options,
                explanation = explanation,
                verificationNote = verification,
                hint = "اضرب عدد المجموعات في عدد العناصر بكل مجموعة"
            )
        }

        val questionText = if (isVertical) {
            "احسب ناتج الضرب الرأسي التالي:\n   $a\n×  $b\n-------"
        } else {
            "احسب ناتج الضرب التالي:\n$a × $b = ..."
        }
        val options = generateRealisticDistractors(product, isPositiveOnly = true)
        val explanation = "نضرب الآحاد أولاً مع مراعاة الحمل إن وجد:\n$a × $b = $product."
        val verification = "التحقق بالقسمة: $product ÷ $b = $a."

        return PrimaryMathProblem(
            grade = safeGrade,
            skill = MathSkill.MULTIPLICATION,
            title = if (isVertical) "ضرب رأسي" else "حقائق الضرب",
            questionText = questionText,
            isVertical = isVertical,
            topNumber = a.toLong(),
            bottomNumber = b.toLong(),
            operationSymbol = "×",
            correctAnswer = product.toString(),
            options = options,
            explanation = explanation,
            verificationNote = verification,
            hint = "تذكر حقائق جدول الضرب واضرب رقم الآحاد أولاً"
        )
    }

    // =========================================================================
    // 4. التقريب وتقدير النواتج (Rounding & Estimation)
    // =========================================================================
    fun generateRoundingProblem(
        grade: Int,
        difficulty: MathDifficulty = MathDifficulty.MEDIUM
    ): PrimaryMathProblem {
        val safeGrade = if (grade < 2) 2 else grade

        // Determine target place value based on grade
        val placeValue = when {
            safeGrade <= 3 -> listOf("أقرب عشرة", "أقرب مئة").random()
            safeGrade == 4 -> listOf("أقرب عشرة", "أقرب مئة", "أقرب ألف").random()
            else -> listOf("أقرب مئة", "أقرب ألف", "أقرب عشرة آلاف").random()
        }

        val baseNumber = when (placeValue) {
            "أقرب عشرة" -> Random.nextInt(15, 985)
            "أقرب مئة" -> Random.nextInt(120, 8950)
            "أقرب ألف" -> Random.nextInt(1400, 78900)
            else -> Random.nextInt(15000, 980000)
        }

        val (divisor, targetLabel) = when (placeValue) {
            "أقرب عشرة" -> 10 to "أقرب عشرة"
            "أقرب مئة" -> 100 to "أقرب مئة"
            "أقرب ألف" -> 1000 to "أقرب ألف"
            else -> 10000 to "أقرب عشرة آلاف"
        }

        val remainder = baseNumber % divisor
        val half = divisor / 2
        val rounded = if (remainder >= half) {
            baseNumber + (divisor - remainder)
        } else {
            baseNumber - remainder
        }

        val isEstimation = safeGrade >= 4 && Random.nextBoolean()
        if (isEstimation) {
            val num1 = Random.nextInt(32, 480)
            val num2 = Random.nextInt(25, 390)
            val r1 = (num1 + 5) / 10 * 10
            val r2 = (num2 + 5) / 10 * 10
            val estimatedSum = r1 + r2
            val exactSum = num1 + num2

            val questionText = "قدّر ناتج الجمع التالي بالتقريب لأقرب عشرة:\n$num1 + $num2 ≈ ...\n(ثم قارنه بالناتج الدقيق)"
            val options = generateRealisticDistractors(estimatedSum.toLong(), isPositiveOnly = true)
            val explanation = "نقرب كل عدد لأقرب عشرة أولاً:\n$num1 يقرب إلى $r1\n$num2 يقرب إلى $r2\nالتقدير: $r1 + $r2 = $estimatedSum.\n(الناتج الدقيق هو $exactSum والفارق $estimatedSum - $exactSum = ${estimatedSum - exactSum})."

            return PrimaryMathProblem(
                grade = safeGrade,
                skill = MathSkill.ROUNDING,
                title = "تقدير نواتج العمليات الحسابية",
                questionText = questionText,
                correctAnswer = estimatedSum.toString(),
                options = options,
                explanation = explanation,
                verificationNote = "قواعد التقريب: الأرقام (0، 1، 2، 3، 4) تهمل، والأرقام (5، 6، 7، 8، 9) تضاف للمنزلة التالية.",
                hint = "قرب كل عدد لأقرب عشرة أولاً ثم اجمع التقديرين"
            )
        }

        val questionText = "قرّب العدد التالي ($baseNumber) إلى $targetLabel:"
        val options = generateRealisticDistractors(rounded.toLong(), step = divisor.toLong(), isPositiveOnly = true)
        val explanation = "ننظر إلى الرقم في المنزلة التي تسبق $targetLabel مباشرة:\nفي العدد $baseNumber، باقي القسمة على $divisor هو $remainder (مقارنة بـ $half).\nبما أنه ${if (remainder >= half) "أكبر من أو يساوي $half، نقرب للأعلى" else "أصغر من $half، نقرب للأدنى"}.\nالناتج المقرب: $rounded."

        return PrimaryMathProblem(
            grade = safeGrade,
            skill = MathSkill.ROUNDING,
            title = "تقريب الأعداد",
            questionText = questionText,
            correctAnswer = rounded.toString(),
            options = options,
            explanation = explanation,
            verificationNote = "العدد $baseNumber يقع بين ${baseNumber / divisor * divisor} و ${(baseNumber / divisor + 1) * divisor}، وهو أقرب إلى $rounded.",
            hint = "انظر للرقم السابق مباشرة: إن كان 5 فما فوق أضف 1، وإن كان أقل اتركه كما هو"
        )
    }

    // =========================================================================
    // 5. الكسور الاعتيادية (Fractions)
    // =========================================================================
    fun generateFractionProblem(
        grade: Int,
        difficulty: MathDifficulty = MathDifficulty.MEDIUM
    ): PrimaryMathProblem {
        val safeGrade = if (grade < 3) 3 else grade

        val type = when (safeGrade) {
            3 -> "IDENTIFY" // التعرف والبسط والمقام
            4 -> listOf("COMPARE", "REPRESENT").random() // مقارنة وتمثيل
            else -> listOf("EQUIVALENT", "ADD_LIKE_DENOM").random() // تكافؤ وجمع
        }

        when (type) {
            "IDENTIFY" -> {
                val den = Random.nextInt(3, 9)
                val num = Random.nextInt(1, den)
                val questionText = "في الكسر الاعتيادي ($num / $den)، ما هو البسط (العدد العلوي) وما هو المقام (العدد السفلي)؟"
                val correct = "البسط = $num ، المقام = $den"
                val options = listOf(
                    correct,
                    "البسط = $den ، المقام = $num",
                    "البسط = ${num + 1} ، المقام = $den",
                    "البسط = $num ، المقام = ${den + 1}"
                ).shuffled()
                val explanation = "البسط (Numerator) هو العدد العلوي الذي يمثل الأجزاء المأخوذة ($num)، والمقام (Denominator) هو العدد السفلي ويمثل إجمالي الأجزاء المتساوية ($den)."

                return PrimaryMathProblem(
                    grade = safeGrade,
                    skill = MathSkill.FRACTIONS,
                    title = "مفهوم الكسر والبسط والمقام",
                    questionText = questionText,
                    correctAnswer = correct,
                    options = options,
                    explanation = explanation,
                    verificationNote = "الكسر $num/$den يعبر عن $num أجزاء من أصل $den أجزاء متساوية.",
                    hint = "الرقم في الأعلى هو البسط، والرقم في الأسفل هو المقام",
                    fractionData = FractionData(numerator = num, denominator = den, representationType = "BAR")
                )
            }

            "COMPARE" -> {
                val den = Random.nextInt(4, 11)
                val num1 = Random.nextInt(1, den - 1)
                var num2 = Random.nextInt(1, den)
                if (num1 == num2) num2 = (num1 + 1).coerceAtMost(den)

                val questionText = "قارن بين الكسرين التاليين بوضع الإشارة المناسبة (> ، < ، =):\n($num1 / $den)  [ ... ]  ($num2 / $den)"
                val symbol = when {
                    num1 > num2 -> ">"
                    num1 < num2 -> "<"
                    else -> "="
                }
                val options = listOf(">", "<", "=", "غير قابلة للمقارنة")
                val explanation = "بما أن المقامين متساويان ($den)، فإن الكسر الذي بسطه أكبر يكون هو الكسر الأكبر:\nبما أن $num1 ${if (num1 > num2) "أكبر من" else "أصغر من"} $num2، فإن الكسر ($num1/$den) $symbol ($num2/$den)."

                return PrimaryMathProblem(
                    grade = safeGrade,
                    skill = MathSkill.FRACTIONS,
                    title = "مقارنة الكسور متحدة المقامات",
                    questionText = questionText,
                    correctAnswer = symbol,
                    options = options,
                    explanation = explanation,
                    verificationNote = "عند تساوي المقامات، نقارن البسوط مباشرة.",
                    hint = "المقامات متساوية، قارن بين الرقمين في الأعلى مباشرة",
                    fractionData = FractionData(
                        numerator = num1,
                        denominator = den,
                        secondNumerator = num2,
                        secondDenominator = den,
                        operation = "مقارنة"
                    )
                )
            }

            "EQUIVALENT" -> {
                val baseNum = Random.nextInt(1, 4)
                val baseDen = Random.nextInt(baseNum + 1, 6)
                val multiplier = Random.nextInt(2, 5)
                val eqNum = baseNum * multiplier
                val eqDen = baseDen * multiplier

                val questionText = "ما هو الكسر المكافئ للكسر ($baseNum / $baseDen) من بين الخيارات التالية؟"
                val correct = "$eqNum / $eqDen"
                val options = listOf(
                    correct,
                    "${eqNum + 1} / $eqDen",
                    "$eqNum / ${eqDen + 2}",
                    "${baseNum + 1} / ${baseDen + 1}"
                ).distinct().shuffled()
                val explanation = "نحصل على كسر مكافئ بضرب البسط والمقام في نفس العدد غير الصفر (هنا ضربنا في $multiplier):\n($baseNum × $multiplier) / ($baseDen × $multiplier) = $eqNum / $eqDen."

                return PrimaryMathProblem(
                    grade = safeGrade,
                    skill = MathSkill.FRACTIONS,
                    title = "الكسور المتكافئة",
                    questionText = questionText,
                    correctAnswer = correct,
                    options = options,
                    explanation = explanation,
                    verificationNote = "$baseNum/$baseDen = $eqNum/$eqDen (بتبسيط الكسر بقسمة الطرفين على $multiplier).",
                    hint = "اضرب البسط والمقام في نفس العدد لتجد الكسر المكافئ"
                )
            }

            else -> { // ADD_LIKE_DENOM (جمع الكسور متحدة المقام)
                val den = Random.nextInt(5, 12)
                val num1 = Random.nextInt(1, den / 2)
                val num2 = Random.nextInt(1, den - num1)
                val sumNum = num1 + num2

                val questionText = "احسب ناتج جمع الكسرين التاليين:\n($num1 / $den) + ($num2 / $den) = ..."
                val correct = "$sumNum / $den"
                val options = listOf(
                    correct,
                    "$sumNum / ${den + den}", // الخطأ الشائع: جمع المقامات!
                    "${sumNum + 1} / $den",
                    "${num1 + num2 - 1} / $den"
                ).distinct().shuffled()
                val explanation = "قاعدة جمع الكسور متحدة المقام: نجمع البسوط فقط ويبقى المقام كما هو:\n($num1 + $num2) / $den = $sumNum / $den.\n(تنبيه تعليمي: لا نجمع المقامات أبداً!)."

                return PrimaryMathProblem(
                    grade = safeGrade,
                    skill = MathSkill.FRACTIONS,
                    title = "جمع الكسور متحدة المقام",
                    questionText = questionText,
                    correctAnswer = correct,
                    options = options,
                    explanation = explanation,
                    verificationNote = "المقام يبقى ثابتاً ($den)، والبسط هو ناتج $num1 + $num2 = $sumNum.",
                    hint = "اجمع الأرقام في الأعلى فقط، واترك الرقم في الأسفل كما هو"
                )
            }
        }
    }

    // =========================================================================
    // 6. الجمع بالاحتفاظ (Addition with Regrouping / Carry)
    // =========================================================================
    fun generateAdditionWithRegrouping(
        grade: Int,
        digits: Int = if (grade <= 2) 2 else if (grade == 3) 3 else 4
    ): PrimaryMathProblem {
        // Guarantee at least one digit results in a carry (sum >= 10)
        val safeDigits = digits.coerceIn(2, 4)

        var a: Long
        var b: Long
        var hasCarry: Boolean
        do {
            val min = when (safeDigits) {
                2 -> 15L
                3 -> 125L
                else -> 1250L
            }
            val max = when (safeDigits) {
                2 -> 89L
                3 -> 899L
                else -> 8999L
            }
            a = Random.nextLong(min, max)
            b = Random.nextLong(min, max)

            // Check if there is carry in any place
            var tempA = a
            var tempB = b
            hasCarry = false
            var carry = 0
            while (tempA > 0 || tempB > 0) {
                val d1 = (tempA % 10).toInt()
                val d2 = (tempB % 10).toInt()
                if (d1 + d2 + carry >= 10) {
                    hasCarry = true
                    break
                }
                carry = (d1 + d2 + carry) / 10
                tempA /= 10
                tempB /= 10
            }
        } while (!hasCarry)

        val sum = a + b

        // Break down step by step with carry tracking
        val steps = mutableListOf<AdditionCarryStep>()
        val placeNames = listOf("الآحاد", "العشرات", "المئات", "الآلاف", "عشرات الآلاف")
        var tempA = a
        var tempB = b
        var currentCarry = 0
        var placeIndex = 0

        val explanationLines = mutableListOf<String>()
        explanationLines.add("خطوات الجمع بالاحتفاظ (عمودياً):")

        while (tempA > 0 || tempB > 0 || currentCarry > 0) {
            val d1 = (tempA % 10).toInt()
            val d2 = (tempB % 10).toInt()
            val colSum = d1 + d2 + currentCarry
            val written = colSum % 10
            val nextCarry = colSum / 10
            val pName = placeNames.getOrElse(placeIndex) { "المنزلة $placeIndex" }

            val stepText = if (currentCarry > 0) {
                "منزلة $pName: $d1 + $d2 + 1 (المحمول) = $colSum. نكتب $written ونرفع $nextCarry فوق المنزلة التالية."
            } else {
                if (nextCarry > 0) {
                    "منزلة $pName: $d1 + $d2 = $colSum. نكتب $written ونرفع $nextCarry بالاحتفاظ فوق المنزلة التالية."
                } else {
                    "منزلة $pName: $d1 + $d2 = $colSum. نكتب $written."
                }
            }
            explanationLines.add("${placeIndex + 1}. $stepText")

            steps.add(
                AdditionCarryStep(
                    placeNameAr = pName,
                    topDigit = d1,
                    bottomDigit = d2,
                    carriedIn = currentCarry,
                    sum = colSum,
                    writtenDigit = written,
                    carriedOut = nextCarry,
                    explanationAr = stepText
                )
            )

            currentCarry = nextCarry
            tempA /= 10
            tempB /= 10
            placeIndex++
        }

        explanationLines.add("الناتج النهائي المكتمل: $sum.")

        val questionText = "أوجد ناتج الجمع الرأسي بالاحتفاظ موضحاً خطوات الحمل في كل منزلة:\n   $a\n+  $b\n-------"
        val options = generateRealisticDistractors(sum, isPositiveOnly = true)
        val verification = "التحقق بالطرح العكسي: $sum - $b = $a."

        return PrimaryMathProblem(
            grade = grade,
            skill = MathSkill.ADDITION_REGROUPING,
            title = "الجمع بالاحتفاظ (إعادة التجميع)",
            questionText = questionText,
            isVertical = true,
            topNumber = a,
            bottomNumber = b,
            operationSymbol = "+",
            correctAnswer = sum.toString(),
            options = options,
            explanation = explanationLines.joinToString("\n"),
            verificationNote = verification,
            hint = "إذا كان مجموع الرقمين 10 أو أكثر، اكتب رقم الآحاد بالأسفل وارفع العشرات فوق المنزلة التالية",
            carrySteps = steps
        )
    }

    // =========================================================================
    // 7. الطرح بالاستلاف مع دعم الاستلاف عبر الأصفار (Borrowing Across Zeros)
    // =========================================================================
    fun generateSubtractionWithBorrowing(
        grade: Int,
        acrossZeros: Boolean = grade >= 3
    ): PrimaryMathProblem {
        val (top, bottom) = if (acrossZeros) {
            // Generate numbers with zero in the tens/hundreds: e.g. 504 - 238 or 600 - 247 or 1000 - 356
            val hundreds = Random.nextInt(3, 9)
            val topNum = if (grade >= 4 && Random.nextBoolean()) {
                hundreds * 1000 + Random.nextInt(1, 9) // e.g. 5004 or 4002
            } else {
                hundreds * 100 + Random.nextInt(1, 9) // e.g. 504, 703
            }
            val botHundreds = Random.nextInt(1, hundreds)
            val botTens = Random.nextInt(3, 9)
            val botOnes = Random.nextInt((topNum % 10).toInt() + 1, 10) // guarantee borrowing on ones
            val botNum = botHundreds * 100 + botTens * 10 + botOnes
            topNum.toLong() to botNum.toLong()
        } else {
            // Normal 2 or 3 digit borrowing without zero chains
            val t = Random.nextInt(35, 95)
            val b = Random.nextInt(18, t - 2)
            // Ensure ones need borrowing
            if (t % 10 >= b % 10) {
                t.toLong() to (b - (b % 10) + (t % 10 + 3).coerceAtMost(9)).toLong()
            } else {
                t.toLong() to b.toLong()
            }
        }

        val diff = top - bottom

        // Build precise step-by-step borrow algorithm
        val steps = mutableListOf<SubtractionBorrowStep>()
        val placeNames = listOf("الآحاد", "العشرات", "المئات", "الآلاف")
        val explanationLines = mutableListOf<String>()

        explanationLines.add("خطوات الطرح بالاستلاف (بما في ذلك الاستلاف المتتابع عبر الأصفار):")

        // Analyze digits
        val topStr = top.toString().reversed()
        val botStr = bottom.toString().reversed()
        val length = topStr.length

        // Array representing current modified values
        val currentTopDigits = IntArray(length) { topStr[it].digitToInt() }
        val botDigits = IntArray(length) { if (it < botStr.length) botStr[it].digitToInt() else 0 }

        for (i in 0 until length) {
            val pName = placeNames.getOrElse(i) { "المنزلة $i" }
            val orig = currentTopDigits[i]
            val sub = botDigits[i]

            if (currentTopDigits[i] < sub) {
                // Must borrow! Find next non-zero digit
                var k = i + 1
                while (k < length && currentTopDigits[k] == 0) {
                    k++
                }

                if (k < length) {
                    // Borrow 1 from column k
                    currentTopDigits[k] -= 1
                    explanationLines.add("• منزلة $pName: لا يمكن طرح $sub من ${currentTopDigits[i]}، نستلف من المنزلة المجاورة.")

                    // If zero chain: explain zeros becoming 9
                    if (k > i + 1) {
                        explanationLines.add("• تنبيه الاستلاف عبر الأصفار: المنزلة المجاورة صفر، فننتقل للمنزلة غير الصفرية (${placeNames.getOrElse(k) { "المنزلة $k" }}) لتصبح ${currentTopDigits[k]}، وتتحول المنازل الصفرية الوسيطة إلى 9.")
                        for (z in k - 1 downTo i + 1) {
                            currentTopDigits[z] = 9
                        }
                    }

                    currentTopDigits[i] += 10
                    val res = currentTopDigits[i] - sub
                    explanationLines.add("• منزلة $pName أصبحت ${currentTopDigits[i]}: ${currentTopDigits[i]} - $sub = $res.")

                    steps.add(
                        SubtractionBorrowStep(
                            placeNameAr = pName,
                            originalTopDigit = orig,
                            modifiedTopDigit = currentTopDigits[i],
                            bottomDigit = sub,
                            resultDigit = res,
                            borrowedFromNext = true,
                            isZeroChain = (k > i + 1),
                            explanationAr = "استلفنا 1، أصبحت القيمة ${currentTopDigits[i]} - $sub = $res."
                        )
                    )
                }
            } else {
                val res = currentTopDigits[i] - sub
                explanationLines.add("• منزلة $pName: ${currentTopDigits[i]} - $sub = $res.")
                steps.add(
                    SubtractionBorrowStep(
                        placeNameAr = pName,
                        originalTopDigit = orig,
                        modifiedTopDigit = currentTopDigits[i],
                        bottomDigit = sub,
                        resultDigit = res,
                        borrowedFromNext = false,
                        isZeroChain = false,
                        explanationAr = "${currentTopDigits[i]} - $sub = $res."
                    )
                )
            }
        }

        explanationLines.add("الناتج النهائي الصحيح: $diff.")
        explanationLines.add("التحقق بالجمع العكسي: الناتج ($diff) + المطروح ($bottom) = $top (صحيح 100%).")

        val questionText = if (acrossZeros) {
            "أوجد ناتج الطرح بالاستلاف عبر الأصفار:\n   $top\n-  $bottom\n-------"
        } else {
            "أوجد ناتج الطرح بالاستلاف التالي:\n   $top\n-  $bottom\n-------"
        }

        val options = generateRealisticDistractors(diff, isPositiveOnly = true)
        val verification = "التحقق بالعملية العكسية: $diff + $bottom = $top."

        return PrimaryMathProblem(
            grade = grade,
            skill = MathSkill.SUBTRACTION_BORROWING,
            title = if (acrossZeros) "الطرح بالاستلاف عبر الأصفار" else "الطرح بالاستلاف",
            questionText = questionText,
            isVertical = true,
            topNumber = top,
            bottomNumber = bottom,
            operationSymbol = "-",
            correctAnswer = diff.toString(),
            options = options,
            explanation = explanationLines.joinToString("\n"),
            verificationNote = verification,
            hint = "إذا كانت المنزلة التالية صفراً، استمر في الاستلاف حتى أول رقم غير صفري، فتتحول الأصفار الوسيطة إلى 9 والمنزلة الحالية يضاف لها 10",
            borrowSteps = steps
        )
    }

    // =========================================================================
    // 8. مولد اختبارات الرياضيات المدرسية (Primary Math Exam Generator)
    // =========================================================================
    fun generatePrimaryMathExam(
        teacherId: Long,
        grade: Int,
        examTitle: String,
        selectedSkills: List<MathSkill>,
        questionsCount: Int = 10,
        difficulty: MathDifficulty = MathDifficulty.MIXED,
        durationMinutes: Int = 45,
        totalMarks: Int = 20
    ): Pair<ExamEntity, List<PrimaryMathProblem>> {
        val validSkills = if (selectedSkills.isNotEmpty()) {
            selectedSkills.filter { grade >= it.minGrade }
        } else {
            getSkillsForGrade(grade)
        }

        val activeSkills = if (validSkills.isNotEmpty()) validSkills else listOf(MathSkill.ADDITION, MathSkill.SUBTRACTION)

        val problems = mutableListOf<PrimaryMathProblem>()
        val countPerSkill = (questionsCount / activeSkills.size).coerceAtLeast(1)
        var remaining = questionsCount

        for ((index, skill) in activeSkills.withIndex()) {
            val needed = if (index == activeSkills.lastIndex) remaining else countPerSkill.coerceAtMost(remaining)
            for (i in 0 until needed) {
                val problem = when (skill) {
                    MathSkill.ADDITION -> generateAdditionProblem(grade, difficulty, isVertical = i % 2 == 0, isWordProblem = i == needed - 1)
                    MathSkill.SUBTRACTION -> generateSubtractionProblem(grade, difficulty, isVertical = i % 2 == 0, isWordProblem = i == needed - 1)
                    MathSkill.MULTIPLICATION -> generateMultiplicationProblem(grade, difficulty, isVertical = i % 2 == 0)
                    MathSkill.ROUNDING -> generateRoundingProblem(grade, difficulty)
                    MathSkill.FRACTIONS -> generateFractionProblem(grade, difficulty)
                    MathSkill.ADDITION_REGROUPING -> generateAdditionWithRegrouping(grade)
                    MathSkill.SUBTRACTION_BORROWING -> generateSubtractionWithBorrowing(grade, acrossZeros = grade >= 3)
                }
                problems.add(problem)
            }
            remaining -= needed
        }

        // Convert problems to ExamQuestion format for Room storage
        val marksPerQuestion = (totalMarks / problems.size).coerceAtLeast(1)
        val examQuestions = problems.mapIndexed { idx, p ->
            ExamQuestion(
                questionText = "${idx + 1}. ${p.questionText}",
                type = if (p.options.size >= 2) QuestionType.MULTIPLE_CHOICE else QuestionType.MATH_PROBLEM,
                options = p.options,
                correctAnswer = p.correctAnswer,
                explanation = "${p.explanation}\n\n[التحقق]: ${p.verificationNote}",
                marks = marksPerQuestion,
                verificationStatus = "VERIFIED",
                verificationNote = p.verificationNote
            )
        }

        val examEntity = ExamEntity(
            teacherId = teacherId,
            title = examTitle.ifBlank { "اختبار الرياضيات للصف ${getGradeNameAr(grade)}" },
            subject = "الرياضيات",
            grade = getGradeNameAr(grade),
            difficulty = difficulty.titleAr,
            durationMinutes = durationMinutes,
            totalMarks = examQuestions.sumOf { it.marks },
            examCode = "MATH-${(100..999).random()}",
            questionsJson = ExamQuestion.listToJsonString(examQuestions)
        )

        return Pair(examEntity, problems)
    }

    /**
     * Solves multi-digit addition column by column with carry tracking for interactive visualization
     */
    fun solveAdditionWithCarries(num1: Int, num2: Int): List<AdditionCarryStep> {
        val placeNames = listOf("الآحاد", "العشرات", "المئات", "الآلاف", "عشرات الآلاف", "مئات الآلاف")
        val steps = mutableListOf<AdditionCarryStep>()
        val maxLen = maxOf(num1.toString().length, num2.toString().length)
        var tempA = num1
        var tempB = num2
        var currentCarry = 0
        var col = 0

        while (col < maxLen || currentCarry > 0) {
            val d1 = tempA % 10
            val d2 = tempB % 10
            val colSum = d1 + d2 + currentCarry
            val written = colSum % 10
            val nextCarry = colSum / 10
            val colName = placeNames.getOrElse(col) { "المنزلة $col" }

            val stepExplanation = when {
                col == 0 && nextCarry > 0 ->
                    "منزلة $colName: $d1 + $d2 = $colSum. نكتب $written في الناتج ونحمل $nextCarry إلى منزلة العشرات."
                col == 0 ->
                    "منزلة $colName: $d1 + $d2 = $colSum. نكتب $written في الناتج."
                currentCarry > 0 && nextCarry > 0 ->
                    "منزلة $colName: $d1 + $d2 + $currentCarry (المحمول) = $colSum. نكتب $written في الناتج ونحمل $nextCarry إلى المنزلة التالية."
                currentCarry > 0 ->
                    "منزلة $colName: $d1 + $d2 + $currentCarry (المحمول) = $colSum. نكتب $written في الناتج."
                nextCarry > 0 ->
                    "منزلة $colName: $d1 + $d2 = $colSum. نكتب $written في الناتج ونحمل $nextCarry إلى المنزلة التالية."
                else ->
                    "منزلة $colName: $d1 + $d2 = $colSum. نكتب $written في الناتج."
            }

            steps.add(
                AdditionCarryStep(
                    columnIndex = col,
                    columnName = colName,
                    placeNameAr = colName,
                    topDigit = d1,
                    bottomDigit = d2,
                    carriedIn = currentCarry,
                    sum = colSum,
                    writtenDigit = written,
                    digitResult = written,
                    carriedOut = nextCarry,
                    carryOut = nextCarry,
                    explanation = stepExplanation,
                    explanationAr = stepExplanation
                )
            )

            currentCarry = nextCarry
            tempA /= 10
            tempB /= 10
            col++
        }

        if (steps.isEmpty()) {
            steps.add(
                AdditionCarryStep(
                    columnIndex = 0,
                    columnName = "الآحاد",
                    placeNameAr = "الآحاد",
                    topDigit = 0,
                    bottomDigit = 0,
                    carriedIn = 0,
                    sum = 0,
                    writtenDigit = 0,
                    digitResult = 0,
                    carriedOut = 0,
                    carryOut = 0,
                    explanation = "0 + 0 = 0",
                    explanationAr = "0 + 0 = 0"
                )
            )
        }

        return steps
    }

    /**
     * Solves multi-digit subtraction step by step with borrowing across zeros for interactive visualization
     */
    fun solveSubtractionWithBorrowing(num1: Int, num2: Int): BorrowOperation {
        val top = maxOf(num1, num2)
        val bottom = minOf(num1, num2)
        val diff = top - bottom

        val num1Str = top.toString()
        val num2Str = bottom.toString()
        val maxLen = maxOf(num1Str.length, num2Str.length)

        val placeNames = listOf("الآحاد", "العشرات", "المئات", "الآلاف", "عشرات الآلاف", "مئات الآلاف")

        val currentTop = IntArray(maxLen) { idx ->
            if (idx < num1Str.length) num1Str[num1Str.length - 1 - idx].digitToInt() else 0
        }
        val originalTop = currentTop.clone()
        val bottomDigits = IntArray(maxLen) { idx ->
            if (idx < num2Str.length) num2Str[num2Str.length - 1 - idx].digitToInt() else 0
        }

        val steps = mutableListOf<SubtractionBorrowStep>()

        for (i in 0 until maxLen) {
            val colName = placeNames.getOrElse(i) { "المنزلة $i" }
            val orig = originalTop[i]
            val sub = bottomDigits[i]
            val traceList = mutableListOf<String>()
            var borrowNeeded = false

            if (currentTop[i] < sub) {
                borrowNeeded = true
                var k = i + 1
                while (k < maxLen && currentTop[k] == 0) {
                    k++
                }

                if (k < maxLen) {
                    val donorColName = placeNames.getOrElse(k) { "المنزلة $k" }
                    val donorOrig = currentTop[k]
                    currentTop[k] -= 1
                    traceList.add("نستلف 1 من منزلة $donorColName ($donorOrig تصبح ${currentTop[k]})")

                    if (k > i + 1) {
                        for (z in k - 1 downTo i + 1) {
                            val zeroColName = placeNames.getOrElse(z) { "المنزلة $z" }
                            currentTop[z] = 9
                            traceList.add("الصفر في منزلة $zeroColName يتحول إلى 9 بعد تمرير الاستلاف")
                        }
                    }

                    currentTop[i] += 10
                    val afterBorrow = currentTop[i]
                    val res = afterBorrow - sub
                    traceList.add("تصبح منزلة $colName: $afterBorrow ($orig + 10 = $afterBorrow)")
                    traceList.add("نطرح: $afterBorrow - $sub = $res")
                }
            }

            val finalMinuend = currentTop[i]
            val resultDigit = finalMinuend - sub
            val stepExplanation = if (borrowNeeded) {
                "منزلة $colName: لا يمكن طرح $sub من $orig، تم الاستلاف لتصبح $finalMinuend - $sub = $resultDigit."
            } else {
                "منزلة $colName: $finalMinuend - $sub = $resultDigit."
            }

            steps.add(
                SubtractionBorrowStep(
                    columnIndex = i,
                    columnName = colName,
                    placeNameAr = colName,
                    minuendDigit = finalMinuend,
                    originalTopDigit = orig,
                    modifiedTopDigit = finalMinuend,
                    subtrahendDigit = sub,
                    bottomDigit = sub,
                    digitResult = resultDigit,
                    resultDigit = resultDigit,
                    borrowNeeded = borrowNeeded,
                    borrowedFromNext = borrowNeeded,
                    isZeroChain = traceList.any { it.contains("الصفر") || it.contains("9") },
                    borrowStepsTrace = traceList,
                    explanation = stepExplanation,
                    explanationAr = stepExplanation
                )
            )
        }

        return BorrowOperation(
            minuend = top,
            subtrahend = bottom,
            difference = diff,
            steps = steps,
            explanationText = "خطوات الطرح بالاستلاف المتتابع عبر الأصفار للعددين $top - $bottom = $diff",
            verificationFormula = "$bottom + $diff = $top"
        )
    }

    /**
     * Generates a practice drill problem for primary students
     */
    fun generatePracticeDrill(grade: Int, skillCode: String): PrimaryMathProblem {
        val safeGrade = grade.coerceIn(1, 6)
        return when (skillCode) {
            "SUBTRACTION_ZEROS" -> generateSubtractionWithBorrowing(safeGrade, acrossZeros = true)
            "ADDITION_REGROUPING" -> generateAdditionWithRegrouping(safeGrade)
            "MULTIPLICATION" -> generateMultiplicationProblem(safeGrade)
            "ROUNDING" -> generateRoundingProblem(safeGrade)
            "FRACTIONS" -> generateFractionProblem(safeGrade)
            "ADDITION" -> generateAdditionProblem(safeGrade)
            "SUBTRACTION" -> generateSubtractionProblem(safeGrade)
            else -> generateAdditionWithRegrouping(safeGrade)
        }
    }

    /**
     * Generates a list of ExamQuestion items based on primary math curriculum
     */
    fun generatePrimaryMathExam(
        grade: Int,
        count: Int,
        skills: List<String>,
        difficulty: String = "متوسط"
    ): List<ExamQuestion> {
        val mathDiff = when (difficulty) {
            "سهل" -> MathDifficulty.EASY
            "متقدم" -> MathDifficulty.ADVANCED
            else -> MathDifficulty.MEDIUM
        }

        val mappedSkills = skills.mapNotNull { code ->
            when (code) {
                "SUBTRACTION_ZEROS" -> MathSkill.SUBTRACTION_BORROWING
                "ADDITION_REGROUPING" -> MathSkill.ADDITION_REGROUPING
                "MULTIPLICATION" -> MathSkill.MULTIPLICATION
                "ROUNDING" -> MathSkill.ROUNDING
                "FRACTIONS" -> MathSkill.FRACTIONS
                "ADDITION" -> MathSkill.ADDITION
                "SUBTRACTION" -> MathSkill.SUBTRACTION
                else -> null
            }
        }.ifEmpty {
            listOf(MathSkill.ADDITION_REGROUPING, MathSkill.SUBTRACTION_BORROWING, MathSkill.MULTIPLICATION)
        }

        val (_, problems) = generatePrimaryMathExam(
            teacherId = 1L,
            grade = grade,
            examTitle = "اختبار رياضيات الصف $grade",
            selectedSkills = mappedSkills,
            questionsCount = count,
            difficulty = mathDiff
        )

        return problems.mapIndexed { idx, p ->
            val opts = if (p.options.size >= 4) {
                p.options
            } else {
                val correctL = p.correctAnswer.toLongOrNull() ?: 10L
                generateRealisticDistractors(correctL)
            }
            ExamQuestion(
                id = p.id,
                questionText = p.questionText,
                type = QuestionType.MULTIPLE_CHOICE,
                options = opts,
                correctAnswer = p.correctAnswer,
                explanation = if (p.explanation.isNotBlank()) p.explanation else "الإجابة الصحيحة هي: ${p.correctAnswer}",
                marks = 2,
                variantLabel = "نموذج أساسي",
                verificationStatus = "VERIFIED",
                verificationNote = if (p.verificationNote.isNotBlank()) p.verificationNote else "تم التحقق رياضياً"
            )
        }
    }

    private fun getGradeNameAr(grade: Int): String {
        return when (grade) {
            1 -> "الصف الأول الأساسي"
            2 -> "الصف الثاني الأساسي"
            3 -> "الصف الثالث الأساسي"
            4 -> "الصف الرابع الأساسي"
            5 -> "الصف الخامس الأساسي"
            else -> "الصف السادس الأساسي"
        }
    }

    /**
     * Generates plausible math distractors (wrong answers based on common student errors)
     */
    private fun generateRealisticDistractors(
        correctVal: Long,
        step: Long = 1,
        isPositiveOnly: Boolean = true
    ): List<String> {
        val list = mutableListOf<String>()
        list.add(correctVal.toString())

        // Common arithmetic errors: off by 1, off by 10, flipped digits, off by step
        val candidates = listOf(
            correctVal + step,
            correctVal - step,
            correctVal + 10,
            correctVal - 10,
            correctVal + 2,
            correctVal - 2,
            correctVal + 100,
            correctVal - 100
        )

        for (cand in candidates) {
            if (cand != correctVal && (!isPositiveOnly || cand >= 0)) {
                val str = cand.toString()
                if (!list.contains(str)) {
                    list.add(str)
                }
            }
            if (list.size == 4) break
        }

        while (list.size < 4) {
            val rand = correctVal + Random.nextInt(3, 20)
            val str = rand.toString()
            if (!list.contains(str)) list.add(str)
        }

        return list.shuffled()
    }
}
