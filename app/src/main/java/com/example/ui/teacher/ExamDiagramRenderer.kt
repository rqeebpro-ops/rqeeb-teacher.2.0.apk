package com.example.ui.teacher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ExamDiagramTypes {
    const val NONE = "NONE"
    const val GEOMETRY_TRIANGLE = "GEOMETRY_TRIANGLE"
    const val GEOMETRY_CIRCLE = "GEOMETRY_CIRCLE"
    const val GEOMETRY_PARALLEL = "GEOMETRY_PARALLEL"
    const val PHYSICS_CIRCUIT = "PHYSICS_CIRCUIT"
    const val PHYSICS_FORCE = "PHYSICS_FORCE"
    const val BIOLOGY_CELL = "BIOLOGY_CELL"
    const val CHEMISTRY_ATOM = "CHEMISTRY_ATOM"
    const val COORDINATE_GRAPH = "COORDINATE_GRAPH"
    const val MATH_COMPARE_CIRCLES = "MATH_COMPARE_CIRCLES"
    const val MATH_PLACE_VALUE_TABLE = "MATH_PLACE_VALUE_TABLE"
    const val MATH_WORD_PROBLEM_BOX = "MATH_WORD_PROBLEM_BOX"
    const val MATH_CONNECT_DOTS = "MATH_CONNECT_DOTS"
    const val MATH_SHAPE_MATCH = "MATH_SHAPE_MATCH"
    const val MATH_FRACTION_SHAPES = "MATH_FRACTION_SHAPES"
    const val MATH_NUMBER_LINE = "MATH_NUMBER_LINE"

    val ALL_DIAGRAM_TYPES = listOf(
        NONE to "بدون رسم",
        MATH_COMPARE_CIRCLES to "مقارنة الأعداد بالدوائر (> أو < أو =)",
        MATH_PLACE_VALUE_TABLE to "جدول القيمة المكانية والجمع الرأسي (عشرات وآحاد)",
        MATH_WORD_PROBLEM_BOX to "مسألة رياضية ومربع الناتج",
        MATH_CONNECT_DOTS to "توصيل النقاط لقطعة مستقيمة وأشكال",
        MATH_SHAPE_MATCH to "مطابقة الأشكال الهندسية مع أسمائها",
        MATH_FRACTION_SHAPES to "الكسور والأشكال المجزأة المظللة",
        MATH_NUMBER_LINE to "خط الأعداد والقفزات الحسابية",
        GEOMETRY_TRIANGLE to "مثلث هندسي وقائم الزاوية (فيثاغورس)",
        GEOMETRY_CIRCLE to "دائرة وأوتار ومماسات",
        GEOMETRY_PARALLEL to "مستقيمات متوازية وقاطع وزوايا",
        PHYSICS_CIRCUIT to "دائرة كهربائية (بطارية، مفتاح، مصباح)",
        PHYSICS_FORCE to "قوى ومستويات واحتكاك",
        BIOLOGY_CELL to "تركيب الخلية الحية",
        CHEMISTRY_ATOM to "التركيب الذري ومستويات الطاقة",
        COORDINATE_GRAPH to "شبكة إحداثيات ومستوى ديكارتي"
    )

    fun getDisplayName(type: String?): String {
        return ALL_DIAGRAM_TYPES.firstOrNull { it.first == type }?.second ?: "رسم توضيحي"
    }
}

@Composable
fun ExamDiagramView(
    diagramType: String?,
    isAnswerKey: Boolean,
    modifier: Modifier = Modifier
) {
    if (diagramType.isNullOrBlank() || diagramType == ExamDiagramTypes.NONE) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .border(1.5.dp, Color(0xFF333333), RoundedCornerShape(10.dp))
            .background(Color.White, RoundedCornerShape(10.dp)),
        color = Color.White,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Tag indicating status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ExamDiagramTypes.getDisplayName(diagramType),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAnswerKey) Color(0xFF006874) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (isAnswerKey) "الرسم مكتمل (نموذج الإجابة)" else "ورقة الطالب (المطلوب ملء البيانات)",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAnswerKey) Color.White else Color(0xFF475569)
                    )
                }
            }

            // Canvas Drawing
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                val w = size.width
                val h = size.height
                val strokeBlack = Stroke(width = 3.5f)
                val strokeDashed = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
                val mainColor = Color(0xFF1A1A1A)
                val solvedColor = Color(0xFF006874)
                val accentColor = Color(0xFFD32F2F)

                when (diagramType) {
                    ExamDiagramTypes.GEOMETRY_TRIANGLE -> {
                        // Right-angled triangle ABC
                        val pA = Offset(w * 0.25f, h * 0.2f)
                        val pB = Offset(w * 0.25f, h * 0.85f)
                        val pC = Offset(w * 0.8f, h * 0.85f)

                        val path = Path().apply {
                            moveTo(pA.x, pA.y)
                            lineTo(pB.x, pB.y)
                            lineTo(pC.x, pC.y)
                            close()
                        }
                        drawPath(path, mainColor, style = strokeBlack)

                        // Right angle indicator at B
                        val squareSize = 22f
                        drawRect(
                            color = mainColor,
                            topLeft = Offset(pB.x, pB.y - squareSize),
                            size = Size(squareSize, squareSize),
                            style = Stroke(width = 2.5f)
                        )

                        // Hypotenuse dashed line highlight
                        if (!isAnswerKey) {
                            drawLine(
                                color = accentColor,
                                start = pA,
                                end = pC,
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                        }
                    }

                    ExamDiagramTypes.GEOMETRY_CIRCLE -> {
                        val center = Offset(w * 0.5f, h * 0.52f)
                        val radius = minOf(w, h) * 0.36f

                        // Main Circle
                        drawCircle(color = mainColor, radius = radius, center = center, style = strokeBlack)
                        // Center dot
                        drawCircle(color = accentColor, radius = 5f, center = center)

                        // Radius line
                        val pEdge = Offset(center.x + radius * 0.8f, center.y - radius * 0.6f)
                        drawLine(color = mainColor, start = center, end = pEdge, strokeWidth = 3f)

                        // Tangent Line
                        val pTan1 = Offset(center.x - radius * 1.1f, center.y + radius)
                        val pTan2 = Offset(center.x + radius * 1.1f, center.y + radius)
                        drawLine(
                            color = if (isAnswerKey) solvedColor else mainColor,
                            start = pTan1,
                            end = pTan2,
                            strokeWidth = 3.5f
                        )
                    }

                    ExamDiagramTypes.GEOMETRY_PARALLEL -> {
                        // Parallel Lines L1, L2
                        val y1 = h * 0.32f
                        val y2 = h * 0.72f
                        drawLine(color = mainColor, start = Offset(w * 0.1f, y1), end = Offset(w * 0.9f, y1), strokeWidth = 3f)
                        drawLine(color = mainColor, start = Offset(w * 0.1f, y2), end = Offset(w * 0.9f, y2), strokeWidth = 3f)

                        // Transversal Line
                        val pTop = Offset(w * 0.3f, h * 0.12f)
                        val pBottom = Offset(w * 0.7f, h * 0.92f)
                        drawLine(color = accentColor, start = pTop, end = pBottom, strokeWidth = 3.5f)

                        // Angle arcs
                        drawArc(
                            color = solvedColor,
                            startAngle = 0f,
                            sweepAngle = 65f,
                            useCenter = false,
                            topLeft = Offset(w * 0.35f, y1 - 25f),
                            size = Size(50f, 50f),
                            style = Stroke(width = 3f)
                        )
                        drawArc(
                            color = solvedColor,
                            startAngle = 180f,
                            sweepAngle = 65f,
                            useCenter = false,
                            topLeft = Offset(w * 0.55f, y2 - 25f),
                            size = Size(50f, 50f),
                            style = Stroke(width = 3f)
                        )
                    }

                    ExamDiagramTypes.PHYSICS_CIRCUIT -> {
                        // Schematic circuit box
                        val left = w * 0.18f
                        val right = w * 0.82f
                        val top = h * 0.22f
                        val bottom = h * 0.82f

                        // Outer loop wires
                        // Top wire with Battery in center
                        val midX = (left + right) / 2f
                        drawLine(color = mainColor, start = Offset(left, top), end = Offset(midX - 30f, top), strokeWidth = 3f)
                        drawLine(color = mainColor, start = Offset(midX + 30f, top), end = Offset(right, top), strokeWidth = 3f)

                        // Battery plates
                        drawLine(color = accentColor, start = Offset(midX - 15f, top - 25f), end = Offset(midX - 15f, top + 25f), strokeWidth = 4f)
                        drawLine(color = mainColor, start = Offset(midX + 5f, top - 15f), end = Offset(midX + 5f, top + 15f), strokeWidth = 6f)

                        // Left vertical wire with Switch
                        val midY = (top + bottom) / 2f
                        drawLine(color = mainColor, start = Offset(left, top), end = Offset(left, midY - 25f), strokeWidth = 3f)
                        drawLine(color = mainColor, start = Offset(left, midY + 25f), end = Offset(left, bottom), strokeWidth = 3f)
                        drawCircle(color = mainColor, radius = 5f, center = Offset(left, midY - 25f))
                        drawCircle(color = mainColor, radius = 5f, center = Offset(left, midY + 25f))

                        // Switch blade
                        if (isAnswerKey) {
                            // Closed switch
                            drawLine(color = solvedColor, start = Offset(left, midY - 25f), end = Offset(left, midY + 25f), strokeWidth = 3.5f)
                        } else {
                            // Open switch
                            drawLine(color = accentColor, start = Offset(left, midY - 25f), end = Offset(left - 20f, midY + 15f), strokeWidth = 3.5f)
                        }

                        // Right vertical wire
                        drawLine(color = mainColor, start = Offset(right, top), end = Offset(right, bottom), strokeWidth = 3f)

                        // Bottom wire with Bulb (Circle with X inside)
                        drawLine(color = mainColor, start = Offset(left, bottom), end = Offset(midX - 25f, bottom), strokeWidth = 3f)
                        drawLine(color = mainColor, start = Offset(midX + 25f, bottom), end = Offset(right, bottom), strokeWidth = 3f)
                        drawCircle(color = mainColor, radius = 22f, center = Offset(midX, bottom), style = strokeBlack)
                        drawLine(color = mainColor, start = Offset(midX - 14f, bottom - 14f), end = Offset(midX + 14f, bottom + 14f), strokeWidth = 2.5f)
                        drawLine(color = mainColor, start = Offset(midX + 14f, bottom - 14f), end = Offset(midX - 14f, bottom + 14f), strokeWidth = 2.5f)
                    }

                    ExamDiagramTypes.PHYSICS_FORCE -> {
                        // Horizontal ground surface
                        val groundY = h * 0.65f
                        drawLine(color = mainColor, start = Offset(w * 0.1f, groundY), end = Offset(w * 0.9f, groundY), strokeWidth = 3.5f)

                        // Surface hatch lines
                        for (xStep in (w * 0.12f).toInt()..(w * 0.88f).toInt() step 24) {
                            drawLine(color = Color(0xFF64748B), start = Offset(xStep.toFloat(), groundY), end = Offset(xStep.toFloat() - 12f, groundY + 14f), strokeWidth = 2f)
                        }

                        // Mass block
                        val boxW = 100f
                        val boxH = 70f
                        val boxLeft = w * 0.5f - boxW / 2f
                        val boxTop = groundY - boxH
                        drawRect(color = Color(0xFFE2E8F0), topLeft = Offset(boxLeft, boxTop), size = Size(boxW, boxH))
                        drawRect(color = mainColor, topLeft = Offset(boxLeft, boxTop), size = Size(boxW, boxH), style = strokeBlack)

                        val centerBox = Offset(w * 0.5f, groundY - boxH / 2f)

                        // Force Normal (Up)
                        drawArrow(centerBox, Offset(centerBox.x, centerBox.y - 75f), solvedColor)
                        // Force Weight (Down)
                        drawArrow(centerBox, Offset(centerBox.x, groundY + 50f), accentColor)
                        // Pull Force (Right)
                        drawArrow(centerBox, Offset(centerBox.x + 85f, centerBox.y), solvedColor)
                        // Friction Force (Left)
                        drawArrow(centerBox, Offset(centerBox.x - 70f, centerBox.y), Color(0xFFD97706))
                    }

                    ExamDiagramTypes.BIOLOGY_CELL -> {
                        val center = Offset(w * 0.5f, h * 0.52f)
                        val rx = w * 0.38f
                        val ry = h * 0.36f

                        // Outer Cell Wall
                        drawRoundRect(
                            color = Color(0xFF2E7D32),
                            topLeft = Offset(center.x - rx, center.y - ry),
                            size = Size(rx * 2, ry * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(30f, 30f),
                            style = Stroke(width = 4.5f)
                        )

                        // Inner Plasma Membrane
                        drawRoundRect(
                            color = Color(0xFF81C784),
                            topLeft = Offset(center.x - rx + 8f, center.y - ry + 8f),
                            size = Size((rx - 8f) * 2, (ry - 8f) * 2),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                            style = Stroke(width = 2.5f)
                        )

                        // Nucleus
                        val nCenter = Offset(center.x - rx * 0.35f, center.y)
                        drawCircle(color = Color(0xFF1565C0), radius = 32f, center = nCenter, style = Stroke(width = 3.5f))
                        drawCircle(color = Color(0xFF1565C0), radius = 10f, center = nCenter)

                        // Large Vacuole
                        drawRoundRect(
                            color = Color(0xFF0288D1),
                            topLeft = Offset(center.x + 10f, center.y - ry * 0.6f),
                            size = Size(rx * 0.75f, ry * 1.2f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f, 22f),
                            style = Stroke(width = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f))
                        )

                        // Chloroplasts (green ovals)
                        drawOval(color = Color(0xFF388E3C), topLeft = Offset(center.x - rx * 0.6f, center.y - ry * 0.6f), size = Size(36f, 20f))
                        drawOval(color = Color(0xFF388E3C), topLeft = Offset(center.x - rx * 0.5f, center.y + ry * 0.35f), size = Size(36f, 20f))
                    }

                    ExamDiagramTypes.CHEMISTRY_ATOM -> {
                        val center = Offset(w * 0.5f, h * 0.52f)

                        // Nucleus
                        drawCircle(color = Color(0xFFD32F2F), radius = 24f, center = center)
                        drawCircle(color = Color.White, radius = 24f, center = center, style = Stroke(width = 2.5f))

                        // Energy Shell K (radius 55)
                        drawCircle(color = mainColor, radius = 60f, center = center, style = strokeDashed)
                        // Energy Shell L (radius 100)
                        drawCircle(color = mainColor, radius = 95f, center = center, style = strokeDashed)

                        // Electrons on K (2 electrons)
                        drawCircle(color = solvedColor, radius = 6f, center = Offset(center.x, center.y - 60f))
                        drawCircle(color = solvedColor, radius = 6f, center = Offset(center.x, center.y + 60f))

                        // Electrons on L (8 electrons)
                        for (i in 0 until 8) {
                            val angle = Math.toRadians((i * 45).toDouble())
                            val ex = (center.x + 95f * Math.cos(angle)).toFloat()
                            val ey = (center.y + 95f * Math.sin(angle)).toFloat()
                            drawCircle(color = solvedColor, radius = 6f, center = Offset(ex, ey))
                        }
                    }

                    ExamDiagramTypes.COORDINATE_GRAPH -> {
                        val center = Offset(w * 0.5f, h * 0.52f)

                        // Grid lines
                        for (gx in -4..4) {
                            val xPos = center.x + gx * 32f
                            drawLine(color = Color(0xFFE2E8F0), start = Offset(xPos, 10f), end = Offset(xPos, h - 10f), strokeWidth = 1f)
                        }
                        for (gy in -3..3) {
                            val yPos = center.y + gy * 32f
                            drawLine(color = Color(0xFFE2E8F0), start = Offset(10f, yPos), end = Offset(w - 10f, yPos), strokeWidth = 1f)
                        }

                        // X & Y Axes
                        drawLine(color = mainColor, start = Offset(20f, center.y), end = Offset(w - 20f, center.y), strokeWidth = 3f)
                        drawLine(color = mainColor, start = Offset(center.x, h - 20f), end = Offset(center.x, 20f), strokeWidth = 3f)

                        // Linear function line: y = 0.8x + 20
                        val p1 = Offset(center.x - 120f, center.y + 75f)
                        val p2 = Offset(center.x + 120f, center.y - 75f)
                        drawLine(color = accentColor, start = p1, end = p2, strokeWidth = 3.5f)

                        // Points on line
                        drawCircle(color = solvedColor, radius = 6f, center = center)
                        drawCircle(color = solvedColor, radius = 6f, center = Offset(center.x + 64f, center.y - 40f))
                    }

                    ExamDiagramTypes.MATH_COMPARE_CIRCLES -> {
                        // 3 circle comparison pairs: [80 ○ 50], [31 ○ 36], [30 ○ 45]
                        val centerY = h * 0.5f
                        val centers = listOf(w * 0.22f, w * 0.5f, w * 0.78f)
                        val radius = 22f

                        centers.forEachIndexed { idx, cx ->
                            // Draw the comparison circle
                            drawCircle(color = mainColor, radius = radius, center = Offset(cx, centerY), style = strokeBlack)

                            if (isAnswerKey) {
                                // Draw > or < inside the circle
                                val isGreater = (idx == 0) // 80 > 50
                                val arrowPath = Path().apply {
                                    if (isGreater) {
                                        moveTo(cx - 8f, centerY - 10f)
                                        lineTo(cx + 8f, centerY)
                                        lineTo(cx - 8f, centerY + 10f)
                                    } else {
                                        moveTo(cx + 8f, centerY - 10f)
                                        lineTo(cx - 8f, centerY)
                                        lineTo(cx + 8f, centerY + 10f)
                                    }
                                }
                                drawPath(arrowPath, color = solvedColor, style = Stroke(width = 3.5f))
                            }
                        }
                    }

                    ExamDiagramTypes.MATH_PLACE_VALUE_TABLE -> {
                        // Two Place Value Tables [عشرات | آحاد] and two vertical additions
                        val tableY = h * 0.15f
                        val tableH = h * 0.7f

                        // Table 1: Width 110, Height tableH
                        val t1X = w * 0.1f
                        val t1W = 120f
                        drawRect(color = mainColor, topLeft = Offset(t1X, tableY), size = Size(t1W, tableH), style = strokeBlack)
                        drawLine(color = mainColor, start = Offset(t1X + t1W / 2, tableY), end = Offset(t1X + t1W / 2, tableY + tableH), strokeWidth = 2.5f)
                        drawLine(color = mainColor, start = Offset(t1X, tableY + 28f), end = Offset(t1X + t1W, tableY + 28f), strokeWidth = 2.5f)
                        drawLine(color = mainColor, start = Offset(t1X, tableY + tableH - 32f), end = Offset(t1X + t1W, tableY + tableH - 32f), strokeWidth = 2.5f)

                        // Table 2
                        val t2X = w * 0.42f
                        drawRect(color = mainColor, topLeft = Offset(t2X, tableY), size = Size(t1W, tableH), style = strokeBlack)
                        drawLine(color = mainColor, start = Offset(t2X + t1W / 2, tableY), end = Offset(t2X + t1W / 2, tableY + tableH), strokeWidth = 2.5f)
                        drawLine(color = mainColor, start = Offset(t2X, tableY + 28f), end = Offset(t2X + t1W, tableY + 28f), strokeWidth = 2.5f)
                        drawLine(color = mainColor, start = Offset(t2X, tableY + tableH - 32f), end = Offset(t2X + t1W, tableY + tableH - 32f), strokeWidth = 2.5f)

                        // Vertical arithmetic representation on the right
                        val vX = w * 0.78f
                        drawLine(color = mainColor, start = Offset(vX - 45f, tableY + tableH - 32f), end = Offset(vX + 45f, tableY + tableH - 32f), strokeWidth = 3f)
                        // Plus sign
                        drawLine(color = mainColor, start = Offset(vX + 38f, tableY + 45f), end = Offset(vX + 54f, tableY + 45f), strokeWidth = 3f)
                        drawLine(color = mainColor, start = Offset(vX + 46f, tableY + 37f), end = Offset(vX + 46f, tableY + 53f), strokeWidth = 3f)
                    }

                    ExamDiagramTypes.MATH_WORD_PROBLEM_BOX -> {
                        // Word problem rectangular response box
                        val boxW = 180f
                        val boxH = 55f
                        val boxX = w * 0.5f - boxW / 2
                        val boxY = h * 0.5f - boxH / 2
                        drawRect(color = mainColor, topLeft = Offset(boxX, boxY), size = Size(boxW, boxH), style = Stroke(width = 3.5f))

                        if (isAnswerKey) {
                            drawRect(color = Color(0xFFE8F5E9), topLeft = Offset(boxX + 4f, boxY + 4f), size = Size(boxW - 8f, boxH - 8f))
                        }
                    }

                    ExamDiagramTypes.MATH_CONNECT_DOTS -> {
                        // 5 Dots on a plane to connect
                        val dots = listOf(
                            Offset(w * 0.18f, h * 0.3f),
                            Offset(w * 0.82f, h * 0.3f),
                            Offset(w * 0.5f, h * 0.5f),
                            Offset(w * 0.35f, h * 0.75f),
                            Offset(w * 0.65f, h * 0.75f)
                        )

                        // Draw square dots
                        val dotSize = 12f
                        dots.forEach { dot ->
                            drawRect(color = mainColor, topLeft = Offset(dot.x - dotSize / 2, dot.y - dotSize / 2), size = Size(dotSize, dotSize))
                        }

                        // In answer key, connect dots with lines
                        if (isAnswerKey) {
                            drawLine(color = solvedColor, start = dots[0], end = dots[1], strokeWidth = 3.5f)
                            drawLine(color = solvedColor, start = dots[1], end = dots[4], strokeWidth = 3.5f)
                            drawLine(color = solvedColor, start = dots[4], end = dots[3], strokeWidth = 3.5f)
                            drawLine(color = solvedColor, start = dots[3], end = dots[0], strokeWidth = 3.5f)
                        }
                    }

                    ExamDiagramTypes.MATH_SHAPE_MATCH -> {
                        // Left: 5 Name Boxes, Right: 5 Shapes
                        val shapesX = w * 0.8f
                        val labelsX = w * 0.28f
                        val stepY = h / 6f

                        // Draw 5 Geometric Shapes
                        // 1. Circle
                        drawCircle(color = mainColor, radius = 18f, center = Offset(shapesX, stepY * 1), style = strokeBlack)
                        // 2. Rectangle
                        drawRect(color = mainColor, topLeft = Offset(shapesX - 24f, stepY * 2 - 14f), size = Size(48f, 28f), style = strokeBlack)
                        // 3. Triangle
                        val triPath = Path().apply {
                            moveTo(shapesX, stepY * 3 - 18f)
                            lineTo(shapesX - 20f, stepY * 3 + 14f)
                            lineTo(shapesX + 20f, stepY * 3 + 14f)
                            close()
                        }
                        drawPath(triPath, color = mainColor, style = strokeBlack)
                        // 4. Square
                        drawRect(color = mainColor, topLeft = Offset(shapesX - 18f, stepY * 4 - 18f), size = Size(36f, 36f), style = strokeBlack)
                        // 5. Trapezoid
                        val trapPath = Path().apply {
                            moveTo(shapesX - 12f, stepY * 5 - 14f)
                            lineTo(shapesX + 12f, stepY * 5 - 14f)
                            lineTo(shapesX + 24f, stepY * 5 + 14f)
                            lineTo(shapesX - 24f, stepY * 5 + 14f)
                            close()
                        }
                        drawPath(trapPath, color = mainColor, style = strokeBlack)

                        // Draw 5 Name Boxes on the other side
                        for (i in 1..5) {
                            drawRect(
                                color = mainColor,
                                topLeft = Offset(labelsX - 55f, stepY * i - 14f),
                                size = Size(110f, 28f),
                                style = Stroke(width = 2f)
                            )
                        }

                        // Connecting lines in answer key
                        if (isAnswerKey) {
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            drawLine(color = solvedColor, start = Offset(shapesX - 25f, stepY * 1), end = Offset(labelsX + 55f, stepY * 3), strokeWidth = 2.5f, pathEffect = dashEffect)
                            drawLine(color = solvedColor, start = Offset(shapesX - 25f, stepY * 2), end = Offset(labelsX + 55f, stepY * 4), strokeWidth = 2.5f, pathEffect = dashEffect)
                            drawLine(color = solvedColor, start = Offset(shapesX - 25f, stepY * 3), end = Offset(labelsX + 55f, stepY * 5), strokeWidth = 2.5f, pathEffect = dashEffect)
                            drawLine(color = solvedColor, start = Offset(shapesX - 25f, stepY * 4), end = Offset(labelsX + 55f, stepY * 1), strokeWidth = 2.5f, pathEffect = dashEffect)
                            drawLine(color = solvedColor, start = Offset(shapesX - 25f, stepY * 5), end = Offset(labelsX + 55f, stepY * 2), strokeWidth = 2.5f, pathEffect = dashEffect)
                        }
                    }

                    ExamDiagramTypes.MATH_FRACTION_SHAPES -> {
                        // Circle 1/4 and Rectangle 1/3
                        val c1 = Offset(w * 0.3f, h * 0.5f)
                        val r1 = 45f
                        drawCircle(color = mainColor, radius = r1, center = c1, style = strokeBlack)
                        drawLine(color = mainColor, start = Offset(c1.x - r1, c1.y), end = Offset(c1.x + r1, c1.y), strokeWidth = 2.5f)
                        drawLine(color = mainColor, start = Offset(c1.x, c1.y - r1), end = Offset(c1.x, c1.y + r1), strokeWidth = 2.5f)

                        // Rectangle 1/3
                        val rectX = w * 0.65f - 60f
                        val rectY = h * 0.5f - 30f
                        drawRect(color = mainColor, topLeft = Offset(rectX, rectY), size = Size(120f, 60f), style = strokeBlack)
                        drawLine(color = mainColor, start = Offset(rectX + 40f, rectY), end = Offset(rectX + 40f, rectY + 60f), strokeWidth = 2.5f)
                        drawLine(color = mainColor, start = Offset(rectX + 80f, rectY), end = Offset(rectX + 80f, rectY + 60f), strokeWidth = 2.5f)

                        if (isAnswerKey) {
                            drawRect(color = solvedColor.copy(alpha = 0.4f), topLeft = Offset(rectX, rectY), size = Size(40f, 60f))
                        }
                    }

                    ExamDiagramTypes.MATH_NUMBER_LINE -> {
                        val lineY = h * 0.55f
                        drawLine(color = mainColor, start = Offset(30f, lineY), end = Offset(w - 30f, lineY), strokeWidth = 3.5f)
                        // Arrow heads
                        drawArrow(Offset(50f, lineY), Offset(20f, lineY), mainColor)
                        drawArrow(Offset(w - 50f, lineY), Offset(w - 20f, lineY), mainColor)

                        // Ticks
                        for (i in 0..6) {
                            val tx = 60f + i * ((w - 120f) / 6f)
                            drawLine(color = mainColor, start = Offset(tx, lineY - 12f), end = Offset(tx, lineY + 12f), strokeWidth = 2.5f)
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color
) {
    drawLine(color = color, start = start, end = end, strokeWidth = 3.5f)
    val angle = Math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val arrowLen = 18.0
    val arrowAngle = Math.toRadians(28.0)

    val x1 = (end.x - arrowLen * Math.cos(angle - arrowAngle)).toFloat()
    val y1 = (end.y - arrowLen * Math.sin(angle - arrowAngle)).toFloat()
    val x2 = (end.x - arrowLen * Math.cos(angle + arrowAngle)).toFloat()
    val y2 = (end.y - arrowLen * Math.sin(angle + arrowAngle)).toFloat()

    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(x1, y1)
        lineTo(x2, y2)
        close()
    }
    drawPath(path, color)
}

object ExamDiagramSvg {

    fun getSvg(type: String?, isAnswerKey: Boolean): String {
        return when (type) {
            ExamDiagramTypes.GEOMETRY_TRIANGLE -> {
                val hypotenuseLabel = if (isAnswerKey) "الوتر = 5 سم (أ ج)" else "الوتر أ ج = [ س = ؟ ]"
                val angleLabel = if (isAnswerKey) "زاوية قائمة 90° عند ب" else "الزاوية [ 1 ] = ؟"
                """
                <svg viewBox="0 0 360 210" width="360" height="210" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block; border:1px solid #999; border-radius:6px;">
                    <polygon points="70,40 70,170 290,170" fill="#f8fafc" stroke="#111" stroke-width="3"/>
                    <rect x="70" y="145" width="25" height="25" fill="none" stroke="#111" stroke-width="2"/>
                    <text x="50" y="40" font-size="14" font-weight="bold" fill="#000">أ</text>
                    <text x="50" y="185" font-size="14" font-weight="bold" fill="#000">ب</text>
                    <text x="300" y="180" font-size="14" font-weight="bold" fill="#000">ج</text>
                    <text x="25" y="110" font-size="13" font-weight="bold" fill="#006874">3 سم</text>
                    <text x="160" y="195" font-size="13" font-weight="bold" fill="#006874">4 سم</text>
                    <text x="175" y="95" font-size="14" font-weight="bold" fill="${if (isAnswerKey) "#006874" else "#d32f2f"}">$hypotenuseLabel</text>
                    <text x="105" y="155" font-size="11" fill="#444">$angleLabel</text>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.GEOMETRY_CIRCLE -> {
                val label1 = if (isAnswerKey) "المركز م" else "(1) ........."
                val label2 = if (isAnswerKey) "نصف القطر نق" else "(2) ........."
                val label3 = if (isAnswerKey) "المماس س ص" else "(3) ........."
                """
                <svg viewBox="0 0 360 210" width="360" height="210" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block; border:1px solid #999; border-radius:6px;">
                    <circle cx="180" cy="100" r="70" fill="#f8fafc" stroke="#111" stroke-width="3"/>
                    <circle cx="180" cy="100" r="4" fill="#d32f2f"/>
                    <line x1="180" y1="100" x2="235" y2="55" stroke="#111" stroke-width="2.5"/>
                    <line x1="80" y1="170" x2="280" y2="170" stroke="#006874" stroke-width="3"/>
                    <text x="175" y="92" font-size="13" font-weight="bold" fill="#111">$label1</text>
                    <text x="200" y="65" font-size="13" font-weight="bold" fill="#006874">$label2</text>
                    <text x="145" y="192" font-size="13" font-weight="bold" fill="#d32f2f">$label3</text>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.PHYSICS_CIRCUIT -> {
                val batLabel = if (isAnswerKey) "البطارية (+ / -)" else "[1] مصدر التيار: ......"
                val swLabel = if (isAnswerKey) "المفتاح (مغلق)" else "[2] المفتاح: ......"
                val bulbLabel = if (isAnswerKey) "المصباح الكهربائي (مضاء)" else "[3] الأداة: ......"
                """
                <svg viewBox="0 0 360 210" width="360" height="210" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block; border:1px solid #999; border-radius:6px;">
                    <!-- Circuit Wires -->
                    <rect x="60" y="40" width="240" height="130" fill="none" stroke="#222" stroke-width="3"/>
                    <!-- Battery on top -->
                    <rect x="155" y="30" width="50" height="20" fill="#fff"/>
                    <line x1="165" y1="28" x2="165" y2="52" stroke="#d32f2f" stroke-width="4"/>
                    <line x1="185" y1="35" x2="185" y2="45" stroke="#222" stroke-width="6"/>
                    <text x="150" y="24" font-size="12" font-weight="bold" fill="#006874">$batLabel</text>
                    <!-- Switch on left -->
                    <rect x="50" y="90" width="20" height="30" fill="#fff"/>
                    <circle cx="60" cy="95" r="4" fill="#222"/>
                    <circle cx="60" cy="115" r="4" fill="#222"/>
                    <line x1="60" y1="95" x2="${if (isAnswerKey) "60" else "45"}" y2="${if (isAnswerKey) "115" else "110"}" stroke="#d32f2f" stroke-width="3"/>
                    <text x="10" y="110" font-size="11" font-weight="bold" fill="#d32f2f">$swLabel</text>
                    <!-- Bulb on bottom -->
                    <rect x="160" y="160" width="40" height="20" fill="#fff"/>
                    <circle cx="180" cy="170" r="16" fill="#fff" stroke="#222" stroke-width="2.5"/>
                    <line x1="169" y1="159" x2="191" y2="181" stroke="#222" stroke-width="2"/>
                    <line x1="191" y1="159" x2="169" y2="181" stroke="#222" stroke-width="2"/>
                    <text x="140" y="200" font-size="12" font-weight="bold" fill="#006874">$bulbLabel</text>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.BIOLOGY_CELL -> {
                val nuc = if (isAnswerKey) "النواة والمادة الوراثية" else "[1] ............"
                val cyto = if (isAnswerKey) "السيتوبلازم" else "[2] ............"
                val wall = if (isAnswerKey) "الجدار الخلوي" else "[3] ............"
                val chloro = if (isAnswerKey) "البلاستيدات الخضراء" else "[4] ............"
                """
                <svg viewBox="0 0 360 210" width="360" height="210" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block; border:1px solid #999; border-radius:6px;">
                    <rect x="50" y="25" width="260" height="160" rx="30" fill="#e8f5e9" stroke="#2e7d32" stroke-width="4"/>
                    <rect x="58" y="33" width="244" height="144" rx="24" fill="#f1f8e9" stroke="#81c784" stroke-width="2"/>
                    <!-- Nucleus -->
                    <circle cx="130" cy="105" r="28" fill="#bbdefb" stroke="#1565c0" stroke-width="3"/>
                    <circle cx="130" cy="105" r="9" fill="#1565c0"/>
                    <!-- Vacuole -->
                    <rect x="180" y="60" width="90" height="90" rx="20" fill="#e1f5fe" stroke="#0288d1" stroke-width="2" stroke-dasharray="6,4"/>
                    <!-- Pointer labels -->
                    <text x="80" y="160" font-size="12" font-weight="bold" fill="#1565c0">$nuc</text>
                    <text x="200" y="105" font-size="12" font-weight="bold" fill="#0288d1">$cyto</text>
                    <text x="50" y="18" font-size="12" font-weight="bold" fill="#2e7d32">$wall</text>
                    <text x="210" y="45" font-size="12" font-weight="bold" fill="#388e3c">$chloro</text>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.PHYSICS_FORCE -> {
                val fn = if (isAnswerKey) "قوة رد الفعل العمودي (N)" else "[1] القوة: ......"
                val fw = if (isAnswerKey) "قوة الجاذبية / الوزن (W=mg)" else "[2] القوة: ......"
                val fp = if (isAnswerKey) "قوة السحب (F)" else "[3] القوة: ......"
                val ff = if (isAnswerKey) "قوة الاحتكاك (f)" else "[4] القوة: ......"
                """
                <svg viewBox="0 0 360 210" width="360" height="210" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block; border:1px solid #999; border-radius:6px;">
                    <line x1="30" y1="130" x2="330" y2="130" stroke="#111" stroke-width="3"/>
                    <rect x="135" y="70" width="90" height="60" fill="#e2e8f0" stroke="#111" stroke-width="3"/>
                    <text x="160" y="105" font-size="15" font-weight="bold" fill="#111">كتلة (m)</text>
                    <!-- Normal Force Up -->
                    <line x1="180" y1="70" x2="180" y2="25" stroke="#006874" stroke-width="3"/>
                    <polygon points="180,18 175,28 185,28" fill="#006874"/>
                    <text x="130" y="20" font-size="11" font-weight="bold" fill="#006874">$fn</text>
                    <!-- Weight Down -->
                    <line x1="180" y1="130" x2="180" y2="175" stroke="#d32f2f" stroke-width="3"/>
                    <polygon points="180,182 175,172 185,172" fill="#d32f2f"/>
                    <text x="130" y="198" font-size="11" font-weight="bold" fill="#d32f2f">$fw</text>
                    <!-- Pull Right -->
                    <line x1="225" y1="100" x2="275" y2="100" stroke="#006874" stroke-width="3"/>
                    <polygon points="282,100 272,95 272,105" fill="#006874"/>
                    <text x="245" y="90" font-size="11" font-weight="bold" fill="#006874">$fp</text>
                    <!-- Friction Left -->
                    <line x1="135" y1="100" x2="85" y2="100" stroke="#d97706" stroke-width="3"/>
                    <polygon points="78,100 88,95 88,105" fill="#d97706"/>
                    <text x="40" y="90" font-size="11" font-weight="bold" fill="#d97706">$ff</text>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.MATH_COMPARE_CIRCLES -> {
                val sign1 = if (isAnswerKey) "&gt;" else ""
                val sign2 = if (isAnswerKey) "&lt;" else ""
                val sign3 = if (isAnswerKey) "&lt;" else ""
                """
                <svg viewBox="0 0 520 75" width="100%" height="75" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block;">
                    <!-- Pair 1: 80 ○ 50 -->
                    <text x="490" y="45" font-size="22" font-weight="bold" fill="#000" font-family="'Traditional Arabic', Arial">80</text>
                    <circle cx="430" cy="38" r="18" fill="#fff" stroke="#000" stroke-width="2.5"/>
                    <text x="430" y="46" font-size="22" font-weight="bold" fill="#006874" text-anchor="middle">$sign1</text>
                    <text x="380" y="45" font-size="22" font-weight="bold" fill="#000" font-family="'Traditional Arabic', Arial">50</text>
                    <text x="340" y="45" font-size="20" font-weight="bold" fill="#666">،</text>

                    <!-- Pair 2: 31 ○ 36 -->
                    <text x="300" y="45" font-size="22" font-weight="bold" fill="#000" font-family="'Traditional Arabic', Arial">31</text>
                    <circle cx="240" cy="38" r="18" fill="#fff" stroke="#000" stroke-width="2.5"/>
                    <text x="240" y="46" font-size="22" font-weight="bold" fill="#006874" text-anchor="middle">$sign2</text>
                    <text x="190" y="45" font-size="22" font-weight="bold" fill="#000" font-family="'Traditional Arabic', Arial">36</text>
                    <text x="150" y="45" font-size="20" font-weight="bold" fill="#666">،</text>

                    <!-- Pair 3: 30 ○ 45 -->
                    <text x="110" y="45" font-size="22" font-weight="bold" fill="#000" font-family="'Traditional Arabic', Arial">30</text>
                    <circle cx="50" cy="38" r="18" fill="#fff" stroke="#000" stroke-width="2.5"/>
                    <text x="50" y="46" font-size="22" font-weight="bold" fill="#006874" text-anchor="middle">$sign3</text>
                    <text x="10" y="45" font-size="22" font-weight="bold" fill="#000" font-family="'Traditional Arabic', Arial">45</text>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.MATH_PLACE_VALUE_TABLE -> {
                val ans1 = if (isAnswerKey) "5" else ""
                val ans2 = if (isAnswerKey) "6" else ""
                val ans3 = if (isAnswerKey) "7" else ""
                val ans4 = if (isAnswerKey) "6" else ""
                val vAns1 = if (isAnswerKey) "78" else ""
                val vAns2 = if (isAnswerKey) "95" else ""
                """
                <svg viewBox="0 0 540 145" width="100%" height="145" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block;">
                    <!-- Table 1: [عشرات | آحاد] (21 + 35) -->
                    <g transform="translate(410, 8)">
                        <rect x="0" y="0" width="110" height="125" fill="#fff" stroke="#000" stroke-width="2"/>
                        <line x1="55" y1="0" x2="55" y2="125" stroke="#000" stroke-width="1.5"/>
                        <line x1="0" y1="30" x2="110" y2="30" stroke="#000" stroke-width="1.5"/>
                        <line x1="0" y1="60" x2="110" y2="60" stroke="#000" stroke-width="1"/>
                        <line x1="0" y1="90" x2="110" y2="90" stroke="#000" stroke-width="2"/>
                        <!-- Headers -->
                        <text x="27" y="21" font-size="13" font-weight="bold" text-anchor="middle">عشرات</text>
                        <text x="82" y="21" font-size="13" font-weight="bold" text-anchor="middle">آحاد</text>
                        <!-- Row 1: 21 -->
                        <text x="27" y="52" font-size="16" font-weight="bold" text-anchor="middle">2</text>
                        <text x="82" y="52" font-size="16" font-weight="bold" text-anchor="middle">1</text>
                        <!-- Row 2: 35 -->
                        <text x="27" y="82" font-size="16" font-weight="bold" text-anchor="middle">3</text>
                        <text x="82" y="82" font-size="16" font-weight="bold" text-anchor="middle">5</text>
                        <!-- Answer Row -->
                        <text x="27" y="114" font-size="18" font-weight="bold" fill="#006874" text-anchor="middle">$ans1</text>
                        <text x="82" y="114" font-size="18" font-weight="bold" fill="#006874" text-anchor="middle">$ans2</text>
                    </g>

                    <!-- Table 2: [عشرات | آحاد] (46 + 30) -->
                    <g transform="translate(270, 8)">
                        <rect x="0" y="0" width="110" height="125" fill="#fff" stroke="#000" stroke-width="2"/>
                        <line x1="55" y1="0" x2="55" y2="125" stroke="#000" stroke-width="1.5"/>
                        <line x1="0" y1="30" x2="110" y2="30" stroke="#000" stroke-width="1.5"/>
                        <line x1="0" y1="60" x2="110" y2="60" stroke="#000" stroke-width="1"/>
                        <line x1="0" y1="90" x2="110" y2="90" stroke="#000" stroke-width="2"/>
                        <!-- Headers -->
                        <text x="27" y="21" font-size="13" font-weight="bold" text-anchor="middle">عشرات</text>
                        <text x="82" y="21" font-size="13" font-weight="bold" text-anchor="middle">آحاد</text>
                        <!-- Row 1: 46 -->
                        <text x="27" y="52" font-size="16" font-weight="bold" text-anchor="middle">4</text>
                        <text x="82" y="52" font-size="16" font-weight="bold" text-anchor="middle">6</text>
                        <!-- Row 2: 30 -->
                        <text x="27" y="82" font-size="16" font-weight="bold" text-anchor="middle">3</text>
                        <text x="82" y="82" font-size="16" font-weight="bold" text-anchor="middle">0</text>
                        <!-- Answer Row -->
                        <text x="27" y="114" font-size="18" font-weight="bold" fill="#006874" text-anchor="middle">$ans3</text>
                        <text x="82" y="114" font-size="18" font-weight="bold" fill="#006874" text-anchor="middle">$ans4</text>
                    </g>

                    <!-- Vertical Addition 1: 34 + 44 -->
                    <g transform="translate(150, 15)">
                        <text x="50" y="30" font-size="18" font-weight="bold">3 4</text>
                        <text x="18" y="30" font-size="20" font-weight="bold">+</text>
                        <text x="50" y="60" font-size="18" font-weight="bold">4 4</text>
                        <line x1="10" y1="72" x2="85" y2="72" stroke="#000" stroke-width="2.5"/>
                        <text x="50" y="102" font-size="20" font-weight="bold" fill="#006874">$vAns1</text>
                    </g>

                    <!-- Vertical Addition 2: 63 + 32 -->
                    <g transform="translate(20, 15)">
                        <text x="50" y="30" font-size="18" font-weight="bold">6 3</text>
                        <text x="18" y="30" font-size="20" font-weight="bold">+</text>
                        <text x="50" y="60" font-size="18" font-weight="bold">3 2</text>
                        <line x1="10" y1="72" x2="85" y2="72" stroke="#000" stroke-width="2.5"/>
                        <text x="50" y="102" font-size="20" font-weight="bold" fill="#006874">$vAns2</text>
                    </g>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.MATH_WORD_PROBLEM_BOX -> {
                val ansText = if (isAnswerKey) "35 - 29 = 6 سمكات" else ""
                """
                <svg viewBox="0 0 520 60" width="100%" height="60" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block;">
                    <rect x="50" y="8" width="150" height="44" rx="4" fill="${if (isAnswerKey) "#e8f5e9" else "#fff"}" stroke="#000" stroke-width="3"/>
                    <text x="125" y="36" font-size="15" font-weight="bold" fill="#006874" text-anchor="middle">$ansText</text>
                    <text x="500" y="36" font-size="16" font-weight="bold" fill="#000" text-anchor="end">اصطاد مروان ٣٥ سمكة باع منها ٢٩ سمكة. كم سمكة بقيت</text>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.MATH_CONNECT_DOTS -> {
                val lineCode = if (isAnswerKey) {
                    """
                    <line x1="85" y1="35" x2="435" y2="35" stroke="#006874" stroke-width="3"/>
                    <text x="260" y="25" font-size="12" font-weight="bold" fill="#006874" text-anchor="middle">قطعة مستقيمة واصلة بين النقطتين</text>
                    """.trimIndent()
                } else ""
                """
                <svg viewBox="0 0 520 95" width="100%" height="95" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block;">
                    $lineCode
                    <!-- 5 Black square dots -->
                    <rect x="80" y="30" width="11" height="11" fill="#000"/>
                    <rect x="430" y="30" width="11" height="11" fill="#000"/>
                    <rect x="255" y="55" width="11" height="11" fill="#000"/>
                    <rect x="160" y="80" width="11" height="11" fill="#000"/>
                    <rect x="350" y="80" width="11" height="11" fill="#000"/>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.MATH_SHAPE_MATCH -> {
                val linesCode = if (isAnswerKey) {
                    """
                    <!-- Connecting lines from shapes to labels in answer key -->
                    <line x1="390" y1="28" x2="220" y2="105" stroke="#006874" stroke-width="2.5" stroke-dasharray="6,4"/>
                    <line x1="390" y1="68" x2="220" y2="145" stroke="#006874" stroke-width="2.5" stroke-dasharray="6,4"/>
                    <line x1="390" y1="112" x2="220" y2="185" stroke="#006874" stroke-width="2.5" stroke-dasharray="6,4"/>
                    <line x1="390" y1="158" x2="220" y2="25" stroke="#006874" stroke-width="2.5" stroke-dasharray="6,4"/>
                    <line x1="390" y1="202" x2="220" y2="65" stroke="#006874" stroke-width="2.5" stroke-dasharray="6,4"/>
                    """.trimIndent()
                } else ""
                """
                <svg viewBox="0 0 520 225" width="100%" height="225" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block;">
                    $linesCode
                    <!-- Shapes Column on the right -->
                    <g transform="translate(390, 0)">
                        <!-- 1. Circle -->
                        <circle cx="28" cy="28" r="18" fill="#fff" stroke="#000" stroke-width="2.5"/>
                        <!-- 2. Rectangle -->
                        <rect x="5" y="55" width="48" height="26" fill="#fff" stroke="#000" stroke-width="2.5"/>
                        <!-- 3. Triangle -->
                        <polygon points="28,96 5,128 51,128" fill="#fff" stroke="#000" stroke-width="2.5"/>
                        <!-- 4. Square -->
                        <rect x="10" y="140" width="36" height="36" fill="#fff" stroke="#000" stroke-width="2.5"/>
                        <!-- 5. Trapezoid -->
                        <polygon points="16,188 40,188 52,216 4,216" fill="#fff" stroke="#000" stroke-width="2.5"/>
                    </g>

                    <!-- Text Boxes on the left -->
                    <g transform="translate(100, 10)">
                        <!-- Box 1: مربع -->
                        <rect x="0" y="0" width="120" height="30" fill="#fff" stroke="#000" stroke-width="2"/>
                        <text x="60" y="21" font-size="14" font-weight="bold" text-anchor="middle">مربع</text>

                        <!-- Box 2: شبه منحرف -->
                        <rect x="0" y="40" width="120" height="30" fill="#fff" stroke="#000" stroke-width="2"/>
                        <text x="60" y="61" font-size="14" font-weight="bold" text-anchor="middle">شبه منحرف</text>

                        <!-- Box 3: دائرة -->
                        <rect x="0" y="80" width="120" height="30" fill="#fff" stroke="#000" stroke-width="2"/>
                        <text x="60" y="101" font-size="14" font-weight="bold" text-anchor="middle">دائرة</text>

                        <!-- Box 4: مستطيل -->
                        <rect x="0" y="120" width="120" height="30" fill="#fff" stroke="#000" stroke-width="2"/>
                        <text x="60" y="141" font-size="14" font-weight="bold" text-anchor="middle">مستطيل</text>

                        <!-- Box 5: مثلث -->
                        <rect x="0" y="160" width="120" height="30" fill="#fff" stroke="#000" stroke-width="2"/>
                        <text x="60" y="181" font-size="14" font-weight="bold" text-anchor="middle">مثلث</text>
                    </g>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.MATH_FRACTION_SHAPES -> {
                val frac1Label = if (isAnswerKey) "الكسر = 1/4 (ربع)" else "الكسر المظلل = [ ...... ]"
                val frac2Label = if (isAnswerKey) "الكسر = 1/3 (ثلث)" else "الكسر المظلل = [ ...... ]"
                """
                <svg viewBox="0 0 520 120" width="100%" height="120" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block;">
                    <!-- Circle 1/4 -->
                    <g transform="translate(100, 10)">
                        <circle cx="50" cy="50" r="45" fill="#fff" stroke="#000" stroke-width="2.5"/>
                        <path d="M 50 50 L 50 5 A 45 45 0 0 1 95 50 Z" fill="#bbf7d0" stroke="#000" stroke-width="1.5"/>
                        <line x1="5" y1="50" x2="95" y2="50" stroke="#000" stroke-width="2"/>
                        <line x1="50" y1="5" x2="50" y2="95" stroke="#000" stroke-width="2"/>
                        <text x="50" y="112" font-size="13" font-weight="bold" fill="#006874" text-anchor="middle">$frac1Label</text>
                    </g>

                    <!-- Rectangle 1/3 -->
                    <g transform="translate(300, 20)">
                        <rect x="0" y="0" width="135" height="60" fill="#fff" stroke="#000" stroke-width="2.5"/>
                        <rect x="0" y="0" width="45" height="60" fill="#bbf7d0" stroke="#000" stroke-width="1.5"/>
                        <line x1="45" y1="0" x2="45" y2="60" stroke="#000" stroke-width="2"/>
                        <line x1="90" y1="0" x2="90" y2="60" stroke="#000" stroke-width="2"/>
                        <text x="67" y="95" font-size="13" font-weight="bold" fill="#006874" text-anchor="middle">$frac2Label</text>
                    </g>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.MATH_NUMBER_LINE -> {
                val hops = if (isAnswerKey) {
                    """
                    <path d="M 60 40 Q 110 10 160 40" fill="none" stroke="#006874" stroke-width="2.5"/>
                    <path d="M 160 40 Q 210 10 260 40" fill="none" stroke="#006874" stroke-width="2.5"/>
                    <text x="160" y="18" font-size="12" font-weight="bold" fill="#006874" text-anchor="middle">+ 20</text>
                    """.trimIndent()
                } else ""
                """
                <svg viewBox="0 0 520 85" width="100%" height="85" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block;">
                    $hops
                    <line x1="30" y1="50" x2="490" y2="50" stroke="#000" stroke-width="3"/>
                    <!-- Arrows -->
                    <polygon points="20,50 35,43 35,57" fill="#000"/>
                    <polygon points="500,50 485,43 485,57" fill="#000"/>
                    <!-- Ticks -->
                    <line x1="60" y1="42" x2="60" y2="58" stroke="#000" stroke-width="2.5"/>
                    <text x="60" y="75" font-size="14" font-weight="bold" text-anchor="middle">0</text>

                    <line x1="160" y1="42" x2="160" y2="58" stroke="#000" stroke-width="2.5"/>
                    <text x="160" y="75" font-size="14" font-weight="bold" text-anchor="middle">10</text>

                    <line x1="260" y1="42" x2="260" y2="58" stroke="#000" stroke-width="2.5"/>
                    <text x="260" y="75" font-size="14" font-weight="bold" text-anchor="middle">20</text>

                    <line x1="360" y1="42" x2="360" y2="58" stroke="#000" stroke-width="2.5"/>
                    <text x="360" y="75" font-size="14" font-weight="bold" text-anchor="middle">30</text>

                    <line x1="460" y1="42" x2="460" y2="58" stroke="#000" stroke-width="2.5"/>
                    <text x="460" y="75" font-size="14" font-weight="bold" text-anchor="middle">40</text>
                </svg>
                """.trimIndent()
            }

            ExamDiagramTypes.COORDINATE_GRAPH -> {
                val label = if (isAnswerKey) "المستقيم: ص = س + 1" else "عيّن النقطتين (1, 2) و (-1, 0)"
                """
                <svg viewBox="0 0 360 210" width="360" height="210" xmlns="http://www.w3.org/2000/svg" style="background:#fff; margin: auto; display:block; border:1px solid #999; border-radius:6px;">
                    <!-- Grid lines -->
                    <line x1="180" y1="10" x2="180" y2="200" stroke="#000" stroke-width="2.5"/>
                    <line x1="10" y1="105" x2="350" y2="105" stroke="#000" stroke-width="2.5"/>
                    <text x="340" y="120" font-size="12" font-weight="bold">س</text>
                    <text x="188" y="24" font-size="12" font-weight="bold">ص</text>
                    <!-- Points and line -->
                    <line x1="80" y1="180" x2="280" y2="30" stroke="#d32f2f" stroke-width="3"/>
                    <circle cx="180" cy="105" r="4" fill="#006874"/>
                    <circle cx="230" cy="67" r="5" fill="#006874"/>
                    <text x="120" y="195" font-size="12" font-weight="bold" fill="#006874">$label</text>
                </svg>
                """.trimIndent()
            }

            else -> ""
        }
    }
}
