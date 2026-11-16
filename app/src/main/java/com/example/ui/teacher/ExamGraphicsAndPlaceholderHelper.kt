package com.example.ui.teacher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.model.ExamGraphicBlock
import com.example.data.model.ExamQuestion
import com.example.data.model.ExamTableBlock
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class LibraryGraphicTemplate(
    val id: String,
    val label: String,
    val emoji: String,
    val category: String,
    val diagramType: String? = null,
    val keywords: List<String> = emptyList()
)

object ExamGraphicsLibrary {
    const val CAT_ALL = "الكل"
    const val CAT_GEOMETRY = "أشكال هندسية"
    const val CAT_PLANTS = "نباتات"
    const val CAT_ANIMALS = "حيوانات"
    const val CAT_SCIENCE = "أدوات علمية"

    val CATEGORIES = listOf(CAT_ALL, CAT_GEOMETRY, CAT_PLANTS, CAT_ANIMALS, CAT_SCIENCE)

    val ITEMS = listOf(
        // Geometric Shapes & Math
        LibraryGraphicTemplate("geo_triangle", "مثلث قائم", "🔺", CAT_GEOMETRY, ExamDiagramTypes.GEOMETRY_TRIANGLE, listOf("مثلث", "هندسة", "فيثاغورس")),
        LibraryGraphicTemplate("geo_circle", "دائرة ومماس", "⚪", CAT_GEOMETRY, ExamDiagramTypes.GEOMETRY_CIRCLE, listOf("دائرة", "نصف القطر", "مماس")),
        LibraryGraphicTemplate("geo_square", "مربع", "🟦", CAT_GEOMETRY, null, listOf("مربع", "شكل رباعي")),
        LibraryGraphicTemplate("geo_parallel", "خطوط متوازية", "⏸️", CAT_GEOMETRY, ExamDiagramTypes.GEOMETRY_PARALLEL, listOf("توازي", "متوازي", "زاوية", "زوايا")),
        LibraryGraphicTemplate("geo_ruler", "مسطرة وزاوية", "📐", CAT_GEOMETRY, null, listOf("زاوية", "قائمة", "قياس", "منقلة")),
        LibraryGraphicTemplate("geo_graph", "شبكة إحداثيات", "📈", CAT_GEOMETRY, ExamDiagramTypes.COORDINATE_GRAPH, listOf("إحداثي", "بياني", "منحنى", "محور")),
        LibraryGraphicTemplate("geo_numline", "خط الأعداد", "↔️", CAT_GEOMETRY, ExamDiagramTypes.MATH_NUMBER_LINE, listOf("خط الأعداد", "أعداد")),
        LibraryGraphicTemplate("geo_fractions", "أشكال الكسور", "🍕", CAT_GEOMETRY, ExamDiagramTypes.MATH_FRACTION_SHAPES, listOf("كسر", "كسور", "مظلل")),
        LibraryGraphicTemplate("geo_cube", "مكعب / مجسم", "🧊", CAT_GEOMETRY, null, listOf("مكعب", "مجسم", "حجم")),

        // Plants
        LibraryGraphicTemplate("plant_sprout", "نبتة", "🌱", CAT_PLANTS, null, listOf("نبتة", "نبات", "بذرة", "إنبات", "جذور", "برعم")),
        LibraryGraphicTemplate("plant_tree", "شجرة", "🌳", CAT_PLANTS, null, listOf("شجرة", "أشجار", "غابة")),
        LibraryGraphicTemplate("plant_palm", "نخلة", "🌴", CAT_PLANTS, null, listOf("نخلة", "نخيل", "تمور")),
        LibraryGraphicTemplate("plant_flower", "زهرة", "🌸", CAT_PLANTS, null, listOf("زهرة", "تفتح", "أزهار", "تلقيح")),
        LibraryGraphicTemplate("plant_sunflower", "دوار الشمس", "🌻", CAT_PLANTS, null, listOf("دوار الشمس", "بناء ضوئي", "ضوء")),
        LibraryGraphicTemplate("plant_leaf", "ورقة نبات", "🌿", CAT_PLANTS, null, listOf("ورقة", "أوراق", "تمثيل ضوئي", "يخضور")),
        LibraryGraphicTemplate("plant_wheat", "قمح / سنبلة", "🌾", CAT_PLANTS, null, listOf("قمح", "زرع", "محصول", "زراعة")),
        LibraryGraphicTemplate("plant_apple", "ثمرة / تفاحة", "🍎", CAT_PLANTS, null, listOf("ثمرة", "فاكهة", "تفاح")),
        LibraryGraphicTemplate("plant_cactus", "صبار صحراوي", "🌵", CAT_PLANTS, null, listOf("صبار", "صحراء", "تكيف")),

        // Animals & Biology
        LibraryGraphicTemplate("ani_lion", "أسد", "🦁", CAT_ANIMALS, null, listOf("أسد", "آكل لحوم", "ثدييات")),
        LibraryGraphicTemplate("ani_bird", "طائر", "🐦", CAT_ANIMALS, null, listOf("طائر", "طيور", "عصفور", "جناح")),
        LibraryGraphicTemplate("ani_fish", "سمكة", "🐟", CAT_ANIMALS, null, listOf("سمكة", "أسماك", "خياشيم", "بحر")),
        LibraryGraphicTemplate("ani_butterfly", "فراشة", "🦋", CAT_ANIMALS, null, listOf("فراشة", "حشرة", "تحول")),
        LibraryGraphicTemplate("ani_bee", "نحلة", "🐝", CAT_ANIMALS, null, listOf("نحلة", "نحل", "عسل", "خلية")),
        LibraryGraphicTemplate("ani_turtle", "سلحفاة", "🐢", CAT_ANIMALS, null, listOf("سلحفاة", "زواحف")),
        LibraryGraphicTemplate("ani_camel", "جمل", "🐪", CAT_ANIMALS, null, listOf("جمل", "إبل", "صحراء")),
        LibraryGraphicTemplate("ani_sheep", "خروف", "🐑", CAT_ANIMALS, null, listOf("خروف", "أغنام", "آكل أعشاب")),
        LibraryGraphicTemplate("bio_cell", "خلية حية", "🦠", CAT_ANIMALS, ExamDiagramTypes.BIOLOGY_CELL, listOf("خلية", "نواة", "غشاء", "أحياء")),

        // Scientific Tools
        LibraryGraphicTemplate("sci_microscope", "مجهر ضوئي", "🔬", CAT_SCIENCE, null, listOf("مجهر", "فحص", "ميكروسكوب")),
        LibraryGraphicTemplate("sci_tube", "أنبوب اختبار", "🧪", CAT_SCIENCE, null, listOf("أنبوب", "تفاعل", "كيمياء", "تجربة", "محلول")),
        LibraryGraphicTemplate("sci_flask", "دورق مخبري", "⚗️", CAT_SCIENCE, null, listOf("دورق", "تقطير", "مختبر")),
        LibraryGraphicTemplate("sci_magnet", "مغناطيس", "🧲", CAT_SCIENCE, null, listOf("مغناطيس", "جذب", "أقطاب", "مغناطيسية")),
        LibraryGraphicTemplate("sci_bulb", "مصباح كهربائي", "💡", CAT_SCIENCE, null, listOf("مصباح", "ضوء", "إنارة")),
        LibraryGraphicTemplate("sci_battery", "بطارية", "🔋", CAT_SCIENCE, null, listOf("بطارية", "جهد", "شحن")),
        LibraryGraphicTemplate("sci_circuit", "دائرة كهربائية", "⚡", CAT_SCIENCE, ExamDiagramTypes.PHYSICS_CIRCUIT, listOf("دائرة كهربائية", "تيار", "كهرباء", "مقاومة", "مفتاح")),
        LibraryGraphicTemplate("sci_scale", "ميزان", "⚖️", CAT_SCIENCE, null, listOf("ميزان", "كتلة", "وزن", "توازن")),
        LibraryGraphicTemplate("sci_thermometer", "ميزان حرارة", "🌡️", CAT_SCIENCE, null, listOf("حرارة", "ترمومتر", "درجة الحرارة")),
        LibraryGraphicTemplate("sci_compass", "بوصلة", "🧭", CAT_SCIENCE, null, listOf("بوصلة", "اتجاه", "شمال")),
        LibraryGraphicTemplate("sci_atom", "التركيب الذري", "⚛️", CAT_SCIENCE, ExamDiagramTypes.CHEMISTRY_ATOM, listOf("ذرة", "إلكترون", "بروتون", "نيوترون")),
        LibraryGraphicTemplate("sci_telescope", "تلسكوب فلكي", "🔭", CAT_SCIENCE, null, listOf("تلسكوب", "فلك", "كوكب", "نجم"))
    )

    fun toGraphicBlock(template: LibraryGraphicTemplate, sizeDp: Int = 68): ExamGraphicBlock {
        return ExamGraphicBlock(
            id = UUID.randomUUID().toString(),
            label = template.label,
            emoji = template.emoji,
            category = template.category,
            diagramType = template.diagramType,
            sizeDp = sizeDp
        )
    }

    fun findBestMatch(query: String): LibraryGraphicTemplate {
        val clean = query.trim().lowercase()
        return ITEMS.firstOrNull { item ->
            clean.contains(item.label.lowercase()) ||
                item.keywords.any { kw -> clean.contains(kw.lowercase()) }
        } ?: when {
            clean.contains("نبات") || clean.contains("شجر") || clean.contains("زهر") -> ITEMS.first { it.id == "plant_sprout" }
            clean.contains("حيوان") || clean.contains("ثدي") -> ITEMS.first { it.id == "ani_lion" }
            clean.contains("مختبر") || clean.contains("أداة") || clean.contains("علوم") -> ITEMS.first { it.id == "sci_microscope" }
            clean.contains("هندس") || clean.contains("شكل") -> ITEMS.first { it.id == "geo_triangle" }
            else -> LibraryGraphicTemplate(
                id = "custom_${clean.hashCode()}",
                label = query.trim().ifBlank { "رسم توضيحي" },
                emoji = "🖼️",
                category = CAT_SCIENCE
            )
        }
    }
}

object ExamPlaceholderParser {
    private val graphicTagRegex = Regex("""\[(صورة|رسم|شكل)\s*:?\s*([^\]]+)\]""")
    private val tableTagRegex = Regex("""\[(جدول)\s*:?\s*([^\]]*)\]""")

    /**
     * Parses any [صورة: ...] / [رسم: ...] / [جدول ...] placeholders in question text,
     * converts them automatically into actual ExamGraphicBlock / ExamTableBlock items,
     * and cleans the question text cleanly.
     */
    fun parseQuestionPlaceholders(question: ExamQuestion): ExamQuestion {
        var text = question.text
        val graphics = question.insertedGraphics.toMutableList()
        val tables = question.insertedTables.toMutableList()
        var assignedDiagramType = question.diagramType

        // 1. Parse [صورة: ...] / [رسم: ...] / [شكل: ...]
        val graphicMatches = graphicTagRegex.findAll(text).toList()
        for (match in graphicMatches) {
            val subjectDesc = match.groupValues[2].trim()
            val template = ExamGraphicsLibrary.findBestMatch(subjectDesc)
            val block = ExamGraphicBlock(
                id = UUID.randomUUID().toString(),
                label = subjectDesc.ifBlank { template.label },
                emoji = template.emoji,
                category = template.category,
                diagramType = template.diagramType,
                sizeDp = 72
            )
            // Avoid duplicating identical generated graphic if already parsed
            if (graphics.none { it.label == block.label && it.emoji == block.emoji }) {
                graphics.add(block)
            }
            if (assignedDiagramType.isNullOrBlank() && !template.diagramType.isNullOrBlank()) {
                assignedDiagramType = template.diagramType
            }
        }
        if (graphicMatches.isNotEmpty()) {
            text = graphicTagRegex.replace(text, "").replace("  ", " ").trim()
        }

        // 2. Parse [جدول ...]
        val tableMatches = tableTagRegex.findAll(text).toList()
        for (match in tableMatches) {
            val tableDesc = match.groupValues[2].trim()
            val generatedTable = createTableFromDescription(tableDesc)
            if (tables.none { it.title == generatedTable.title && it.rows.size == generatedTable.rows.size }) {
                tables.add(generatedTable)
            }
        }
        if (tableMatches.isNotEmpty()) {
            text = tableTagRegex.replace(text, "").replace("  ", " ").trim()
        }

        return question.copy(
            text = text,
            insertedGraphics = graphics,
            insertedTables = tables,
            diagramType = assignedDiagramType
        )
    }

    fun parseAndEnrichQuestions(questions: List<ExamQuestion>): List<ExamQuestion> {
        return questions.map { parseQuestionPlaceholders(it) }
    }

    fun createEmptyTable(rows: Int, cols: Int, title: String = "جدول بيانات"): ExamTableBlock {
        val safeRows = rows.coerceIn(1, 12)
        val safeCols = cols.coerceIn(1, 8)
        val grid = (0 until safeRows).map { rIdx ->
            (0 until safeCols).map { cIdx ->
                if (rIdx == 0) "عمود ${cIdx + 1}" else ""
            }
        }
        return ExamTableBlock(
            id = UUID.randomUUID().toString(),
            title = title,
            rows = grid
        )
    }

    fun createTableFromDescription(desc: String): ExamTableBlock {
        val clean = desc.trim()
        // Check if dimensions like 3x4 or 3×4 are specified
        val dimMatch = Regex("""(\d+)\s*[x×*]\s*(\d+)""").find(clean)
        if (dimMatch != null) {
            val r = dimMatch.groupValues[1].toIntOrNull() ?: 3
            val c = dimMatch.groupValues[2].toIntOrNull() ?: 3
            return createEmptyTable(r, c, clean)
        }

        return when {
            clean.contains("مقارنة") -> ExamTableBlock(
                id = UUID.randomUUID().toString(),
                title = clean.ifBlank { "جدول مقارنة" },
                rows = listOf(
                    listOf("وجه المقارنة", "العنصر الأول", "العنصر الثاني"),
                    listOf("التعريف / المفهوم", "", ""),
                    listOf("الخصائص والمميزات", "", ""),
                    listOf("أمثلة وتطبيقات", "", "")
                )
            )
            clean.contains("آحاد") || clean.contains("عشرات") -> ExamTableBlock(
                id = UUID.randomUUID().toString(),
                title = clean.ifBlank { "جدول القيمة المكانية" },
                rows = listOf(
                    listOf("العدد", "آحاد", "عشرات", "مئات"),
                    listOf("", "", "", ""),
                    listOf("", "", "", "")
                )
            )
            clean.contains("توصيل") || clean.contains("مطابقة") -> ExamTableBlock(
                id = UUID.randomUUID().toString(),
                title = clean.ifBlank { "جدول مطابقة وتوصيل" },
                rows = listOf(
                    listOf("العمود (أ)", "العمود (ب)"),
                    listOf("1-", "أ-"),
                    listOf("2-", "ب-"),
                    listOf("3-", "ج-")
                )
            )
            else -> ExamTableBlock(
                id = UUID.randomUUID().toString(),
                title = clean.ifBlank { "جدول بيانات" },
                rows = listOf(
                    listOf("البند", "البيان / القيمة", "ملاحظات"),
                    listOf("", "", ""),
                    listOf("", "", "")
                )
            )
        }
    }
}

object ExamImageStorageHelper {
    fun savePickedImage(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "exam_images").apply { if (!exists()) mkdirs() }
            val outFile = File(dir, "exam_img_${System.currentTimeMillis()}_${(100..999).random()}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val decoded = BitmapFactory.decodeStream(input)
                if (decoded != null) {
                    // Scale down gently if oversized so printing & Word base64 remain crisp and lightweight
                    val maxDim = 960
                    val scaled = if (decoded.width > maxDim || decoded.height > maxDim) {
                        val ratio = minOf(maxDim.toFloat() / decoded.width, maxDim.toFloat() / decoded.height)
                        Bitmap.createScaledBitmap(
                            decoded,
                            (decoded.width * ratio).toInt().coerceAtLeast(1),
                            (decoded.height * ratio).toInt().coerceAtLeast(1),
                            true
                        )
                    } else decoded
                    FileOutputStream(outFile).use { fos ->
                        scaled.compress(Bitmap.CompressFormat.PNG, 92, fos)
                    }
                    if (scaled !== decoded) decoded.recycle()
                    return outFile.absolutePath
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }
}
