package com.example.ui.teacher

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DrawPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

enum class BoardStyle(val titleAr: String, val bgColor: Color, val gridColor: Color) {
    WHITE("سبورة بيضاء", Color(0xFFFAFAFA), Color(0xFFE2E8F0)),
    GREEN("سبورة خضراء (طباشير)", Color(0xFF1E3A2F), Color(0xFF2D5544))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveWhiteboardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val paths = remember { mutableStateListOf<DrawPath>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var boardStyle by remember { mutableStateOf(BoardStyle.GREEN) }
    var currentColor by remember { mutableStateOf(Color.White) }
    var currentStrokeWidth by remember { mutableFloatStateOf(8f) }
    var isEraser by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }

    // Palette depending on board style
    val colors = if (boardStyle == BoardStyle.GREEN) {
        listOf(
            Color.White,
            Color(0xFFFEF08A), // أصفر طباشيري
            Color(0xFF67E8F9), // أزرق سماوي
            Color(0xFFF472B6), // وردي
            Color(0xFF86EFAC)  // أخضر فاتح
        )
    } else {
        listOf(
            Color(0xFF0F172A), // أسود
            Color(0xFF0D47A1), // أزرق
            Color(0xFFDC2626), // أحمر
            Color(0xFF16A34A), // أخضر
            Color(0xFFD97706)  // ذهبي
        )
    }

    LaunchedEffect(boardStyle) {
        currentColor = if (boardStyle == BoardStyle.GREEN) Color.White else Color(0xFF0D47A1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("السبورة الصفية التفاعلية (${boardStyle.titleAr})") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("whiteboard_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    // Switch Board Style (White / Green)
                    IconButton(onClick = {
                        boardStyle = if (boardStyle == BoardStyle.GREEN) BoardStyle.WHITE else BoardStyle.GREEN
                    }) {
                        Icon(
                            if (boardStyle == BoardStyle.GREEN) Icons.Default.Square else Icons.Default.Palette,
                            contentDescription = "تبديل لون السبورة"
                        )
                    }
                    IconButton(onClick = { showGrid = !showGrid }) {
                        Icon(
                            if (showGrid) Icons.Default.GridOn else Icons.Default.GridOff,
                            contentDescription = "الشبكة الهندسية"
                        )
                    }
                    IconButton(onClick = {
                        if (paths.isNotEmpty()) paths.removeAt(paths.lastIndex)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "تراجع")
                    }
                    IconButton(onClick = {
                        paths.clear()
                        Toast.makeText(context, "تم مسح السبورة", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "مسح الكل")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Color Palette
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .clickable {
                                            currentColor = c
                                            isEraser = false
                                        }
                                        .then(
                                            if (!isEraser && currentColor == c) {
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            } else Modifier
                                        )
                                )
                            }
                        }

                        // Tools (Pen, Eraser, Width)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isEraser = false },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (!isEraser) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "قلم")
                            }

                            IconButton(
                                onClick = { isEraser = true },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isEraser) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = "ممحاة")
                            }

                            FilterChip(
                                selected = currentStrokeWidth == 6f,
                                onClick = { currentStrokeWidth = 6f },
                                label = { Text("رفيع", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = currentStrokeWidth == 14f,
                                onClick = { currentStrokeWidth = 14f },
                                label = { Text("عريض", fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(boardStyle.bgColor)
                .pointerInput(isEraser, currentColor, currentStrokeWidth, boardStyle) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newPath = Path().apply { moveTo(offset.x, offset.y) }
                            currentPath = newPath
                            paths.add(
                                DrawPath(
                                    path = newPath,
                                    color = if (isEraser) boardStyle.bgColor else currentColor,
                                    strokeWidth = if (isEraser) currentStrokeWidth * 3.5f else currentStrokeWidth,
                                    isEraser = isEraser
                                )
                            )
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentPath?.lineTo(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            currentPath = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background grid lines if enabled
                if (showGrid) {
                    val step = 40.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = boardStyle.gridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = boardStyle.gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                }

                // Draw existing paths
                paths.forEach { drawPath ->
                    drawPath(
                        path = drawPath.path,
                        color = drawPath.color,
                        style = Stroke(
                            width = drawPath.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (paths.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (boardStyle == BoardStyle.GREEN) "استخدم إصبعك للكتابة بالطباشير على السبورة الخضراء" else "اكتب واشرح بالقلم على السبورة البيضاء",
                        color = if (boardStyle == BoardStyle.GREEN) Color(0xFF6EE7B7) else Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
