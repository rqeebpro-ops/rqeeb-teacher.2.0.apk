package com.example.data.math

import com.example.data.model.ExamQuestion
import com.example.data.model.QuestionType
import kotlin.random.Random

object PrimaryMathEngine {

    data class AdditionCarryStep(
        val position: Int,
        val colName: String,
        val d1: Int,
        val d2: Int,
        val carryIn: Int,
        val sum: Int,
        val digitResult: Int,
        val carryOut: Int,
        val explanation: String
    )

    data class SubtractionBorrowStep(
        val position: Int,
        val colName: String,
        val topDigit: Int,
        val bottomDigit: Int,
        val borrowedFrom: Int?,
        val newTopDigit: Int,
        val resultDigit: Int,
        val explanation: String
    )

    data class PracticeDrillQuestion(
        val id: Int,
        val num1: Int,
        val num2: Int,
        val operation: String, // "+" or "-"
        val grade: Int,
        val skillCode: String,
        val questionText: String,
        val correctAnswer: Int,
        val hint: String,
        val options: List<Int>
    )

    fun solveAdditionWithSteps(num1: Int, num2: Int): List<AdditionCarryStep> {
        val s1 = num1.toString().reversed()
        val s2 = num2.toString().reversed()
        val maxLen = maxOf(s1.length, s2.length)
        val steps = mutableListOf<AdditionCarryStep>()
        val colNames = listOf("الآحاد", "العشرات", "المئات", "آلاف", "عشرات الآلاف", "مئات الآلاف")

        var carry = 0
        for (i in 0 until maxLen) {
            val d1 = if (i < s1.length) s1[i].digitToInt() else 0
            val d2 = if (i < s2.length) s2[i].digitToInt() else 0
            val sum = d1 + d2 + carry
            val digitResult = sum % 10
            val nextCarry = sum / 10
            val col = if (i < colNames.size) colNames[i] else "المنزلة ${i + 1}"

            val explanation = if (carry > 0) {
                "نجمع في منزلة $col: $d1 + $d2 + الحمل السابق ($carry) = $sum. نكتب $digitResult في الناتج ونرفع $nextCarry إلى المنزلة التالية."
            } else if (sum >= 10) {
                "نجمع في منزلة $col: $d1 + $d2 = $sum. نكتب $digitResult في الناتج ونرفع $nextCarry باليد للمنزلة التالية."
            } else {
                "نجمع في منزلة $col: $d1 + $d2 = $digitResult (لا يوجد حمل)."
            }

            steps.add(
                AdditionCarryStep(
                    position = i,
                    colName = col,
                    d1 = d1,
                    d2 = d2,
                    carryIn = carry,
                    sum = sum,
                    digitResult = digitResult,
                    carryOut = nextCarry,
                    explanation = explanation
                )
            )
            carry = nextCarry
        }
        if (carry > 0) {
            steps.add(
                AdditionCarryStep(
                    position = maxLen,
                    colName = "المنزلة الأخيرة",
                    d1 = 0,
                    d2 = 0,
                    carryIn = carry,
                    sum = carry,
                    digitResult = carry,
                    carryOut = 0,
                    explanation = "ننزل الحمل الأخير ($carry) إلى يسار الناتج."
                )
            )
        }
        return steps
    }

    fun solveSubtractionWithSteps(num1: Int, num2: Int): List<SubtractionBorrowStep> {
        val topDigits = num1.toString().map { it.digitToInt() }.toMutableList()
        val bottomDigits = num2.toString().padStart(topDigits.size, '0').map { it.digitToInt() }
        val steps = mutableListOf<SubtractionBorrowStep>()
        val colNames = listOf("الآحاد", "العشرات", "المئات", "آلاف", "عشرات الآلاف", "مئات الآلاف")

        val n = topDigits.size
        for (idx in (n - 1) downTo 0) {
            val colPosFromRight = (n - 1) - idx
            val col = if (colPosFromRight < colNames.size) colNames[colPosFromRight] else "المنزلة"

            var topVal = topDigits[idx]
            val bottomVal = bottomDigits[idx]

            if (topVal < bottomVal) {
                var donorIdx = idx - 1
                while (donorIdx >= 0 && topDigits[donorIdx] == 0) {
                    donorIdx--
                }
                if (donorIdx >= 0) {
                    topDigits[donorIdx]--
                    for (zeroIdx in (donorIdx + 1) until idx) {
                        topDigits[zeroIdx] = 9
                    }
                    topVal += 10
                    topDigits[idx] = topVal
                }
            }

            val resultDigit = topVal - bottomVal
            val explanation = "في منزلة $col: نطرح $topVal - $bottomVal = $resultDigit."
            steps.add(
                SubtractionBorrowStep(
                    position = colPosFromRight,
                    colName = col,
                    topDigit = num1.toString().padStart(n, '0')[idx].digitToInt(),
                    bottomDigit = bottomVal,
                    borrowedFrom = null,
                    newTopDigit = topVal,
                    resultDigit = resultDigit,
                    explanation = explanation
                )
            )
        }
        return steps
    }

    fun generatePracticeDrill(grade: Int, skillCode: String): PracticeDrillQuestion {
        val rnd = Random(System.currentTimeMillis())
        val (num1, num2, op, ans, hint) = when (skillCode) {
            "SUBTRACTION_ZEROS" -> {
                val n1 = when (grade) {
                    1, 2 -> 100
                    3 -> listOf(500, 600, 700).random(rnd)
                    else -> listOf(1000, 2000, 5000, 8000).random(rnd)
                }
                val n2 = when (grade) {
                    1, 2 -> rnd.nextInt(15, 85)
                    3 -> rnd.nextInt(125, 450)
                    else -> rnd.nextInt(340, n1 - 100)
                }
                Quint(n1, n2, "-", n1 - n2, "تذكر: الصفر يستلف من الرقم الذي يليه فيتحول إلى 9 والآحاد إلى 10.")
            }
            "ADDITION_REGROUPING" -> {
                val n1 = when (grade) {
                    1, 2 -> rnd.nextInt(28, 69)
                    3 -> rnd.nextInt(158, 489)
                    else -> rnd.nextInt(1250, 4890)
                }
                val n2 = when (grade) {
                    1, 2 -> rnd.nextInt(15, 38)
                    3 -> rnd.nextInt(145, 385)
                    else -> rnd.nextInt(1450, 3850)
                }
                Quint(n1, n2, "+", n1 + n2, "اجمع الآحاد أولاً؛ إذا زاد الناتج عن 9 ضع الآحاد وارفع العشرات باليد.")
            }
            "MULTIPLICATION" -> {
                val n1 = rnd.nextInt(6, 12)
                val n2 = rnd.nextInt(4, 9)
                Quint(n1, n2, "×", n1 * n2, "تذكر جدول الضرب: $n1 × $n2 هو تكرار جمع $n1 بمقدار $n2 مرات.")
            }
            else -> {
                val n1 = rnd.nextInt(50, 200)
                val n2 = rnd.nextInt(20, 80)
                Quint(n1, n2, "+", n1 + n2, "اجمع المنازل بترتيب: آحاد ثم عشرات ثم مئات.")
            }
        }

        val options = mutableSetOf(ans)
        while (options.size < 4) {
            val offset = rnd.nextInt(-15, 16)
            if (offset != 0 && (ans + offset) > 0) {
                options.add(ans + offset)
            }
        }

        return PracticeDrillQuestion(
            id = (1000..9999).random(rnd),
            num1 = num1,
            num2 = num2,
            operation = op,
            grade = grade,
            skillCode = skillCode,
            questionText = "أوجد ناتج العملية التالية: $num1 $op $num2 = ؟",
            correctAnswer = ans,
            hint = hint,
            options = options.shuffled(rnd)
        )
    }

    private data class Quint(val a: Int, val b: Int, val op: String, val ans: Int, val hint: String)
}
