package com.example.data.model

/**
 * Educational Diagrams Library for Yemeni School Curricula
 * (Physics, Biology, Mathematics, Chemistry, Geography)
 * Generates crisp, lightweight vector SVG schematics for mobile, tablet, and PDF export.
 */
object EducationalDiagramLibrary {

    data class DiagramTemplate(
        val key: String,
        val title: String,
        val category: String,
        val categoryTitleAr: String,
        val defaultLabels: Map<String, String>,
        val generator: (labels: Map<String, String>, hideLabels: Boolean) -> String
    )

    val ALL_TEMPLATES: List<DiagramTemplate> = listOf(
        // ==================== 1. الفيزياء والكهرباء ====================
        DiagramTemplate(
            key = "physics_circuit_simple",
            title = "دائرة كهربائية بسيطة (بطارية، مقاومة، مفتاح)",
            category = "PHYSICS",
            categoryTitleAr = "الفيزياء والكهرباء",
            defaultLabels = mapOf(
                "v" to "V = 12V",
                "r" to "R = 4Ω",
                "i" to "I = ?",
                "s" to "مفتاح كهربائي (مغلق)"
            ),
            generator = { labels, hide ->
                val vText = if (hide) "(1) مصدر جهد" else labels["v"] ?: "12V"
                val rText = if (hide) "(2) مقاومة" else labels["r"] ?: "4Ω"
                val iText = if (hide) "(3) شدة التيار" else labels["i"] ?: "I = ?"
                val sText = if (hide) "(4) مفتاح" else labels["s"] ?: "مفتاح"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <!-- Border Frame -->
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <!-- Circuit Wires -->
                    <path d="M 50 100 L 50 40 L 270 40 L 270 100 M 50 120 L 50 170 L 270 170 L 270 120" fill="none" stroke="#0284c7" stroke-width="3.5" stroke-linecap="round"/>
                    
                    <!-- Battery (DC Voltage Source) -->
                    <line x1="35" y1="100" x2="65" y2="100" stroke="#0f172a" stroke-width="4"/>
                    <line x1="42" y1="120" x2="58" y2="120" stroke="#0f172a" stroke-width="2.5"/>
                    <text x="75" y="115" font-size="12" font-weight="bold" fill="#0369a1">$vText</text>
                    <text x="45" y="92" font-size="11" font-weight="bold" fill="#dc2626">+</text>
                    <text x="45" y="133" font-size="11" font-weight="bold" fill="#475569">-</text>

                    <!-- Resistor Symbol -->
                    <rect x="256" y="85" width="28" height="50" rx="3" fill="#fef08a" stroke="#ca8a04" stroke-width="2.5"/>
                    <text x="210" y="112" font-size="12" font-weight="bold" fill="#854d0e" text-anchor="end">$rText</text>

                    <!-- Switch -->
                    <circle cx="160" cy="40" r="4" fill="#0284c7"/>
                    <circle cx="190" cy="40" r="4" fill="#0284c7"/>
                    <line x1="160" y1="40" x2="188" y2="35" stroke="#0f172a" stroke-width="3" stroke-linecap="round"/>
                    <text x="175" y="26" font-size="11" fill="#475569" text-anchor="middle">$sText</text>

                    <!-- Current Arrow -->
                    <path d="M 100 35 L 120 35 L 115 30 M 120 35 L 115 40" fill="none" stroke="#dc2626" stroke-width="2.5"/>
                    <text x="110" y="55" font-size="11" font-weight="bold" fill="#dc2626">$iText</text>
                </svg>
                """.trimIndent()
            }
        ),

        DiagramTemplate(
            key = "physics_circuit_parallel",
            title = "توصيل مقاومتين على التوازي",
            category = "PHYSICS",
            categoryTitleAr = "الفيزياء والكهرباء",
            defaultLabels = mapOf(
                "v" to "V = 24V",
                "r1" to "R1 = 6Ω",
                "r2" to "R2 = 12Ω"
            ),
            generator = { labels, hide ->
                val vText = if (hide) "(1) مصدر جهد" else labels["v"] ?: "24V"
                val r1Text = if (hide) "(2) المقاومة الأولى" else labels["r1"] ?: "R1 = 6Ω"
                val r2Text = if (hide) "(3) المقاومة الثانية" else labels["r2"] ?: "R2 = 12Ω"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <!-- Main Loop -->
                    <path d="M 60 100 L 60 30 L 150 30 M 60 120 L 60 170 L 150 170" fill="none" stroke="#0284c7" stroke-width="3"/>
                    <!-- Battery -->
                    <line x1="45" y1="100" x2="75" y2="100" stroke="#0f172a" stroke-width="4"/>
                    <line x1="52" y1="120" x2="68" y2="120" stroke="#0f172a" stroke-width="2.5"/>
                    <text x="25" y="115" font-size="12" font-weight="bold" fill="#0369a1">$vText</text>
                    <!-- Branch 1 -->
                    <path d="M 150 30 L 150 65 L 250 65 L 250 30 M 150 30 L 150 135 L 250 135 L 250 170 M 250 30 L 250 170" fill="none" stroke="#0284c7" stroke-width="2.5"/>
                    <rect x="185" y="55" width="36" height="20" rx="3" fill="#fed7aa" stroke="#ea580c" stroke-width="2"/>
                    <text x="203" y="46" font-size="11" font-weight="bold" fill="#c2410c" text-anchor="middle">$r1Text</text>
                    <!-- Branch 2 -->
                    <rect x="185" y="125" width="36" height="20" rx="3" fill="#fed7aa" stroke="#ea580c" stroke-width="2"/>
                    <text x="203" y="163" font-size="11" font-weight="bold" fill="#c2410c" text-anchor="middle">$r2Text</text>
                </svg>
                """.trimIndent()
            }
        ),

        DiagramTemplate(
            key = "physics_magnet_field",
            title = "قضيب مغناطيسي وخطوط المجال المغناطيسي",
            category = "PHYSICS",
            categoryTitleAr = "الفيزياء والكهرباء",
            defaultLabels = mapOf(
                "n" to "قطب شمالي (N)",
                "s" to "قطب جنوبي (S)",
                "lines" to "خطوط المجال المغناطيسي"
            ),
            generator = { labels, hide ->
                val nText = if (hide) "(1) قطب" else labels["n"] ?: "N"
                val sText = if (hide) "(2) قطب" else labels["s"] ?: "S"
                val linesText = labels["lines"] ?: "خطوط المجال"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <!-- Field curves -->
                    <path d="M 110 85 C 110 30, 210 30, 210 85" fill="none" stroke="#3b82f6" stroke-width="2" stroke-dasharray="4,3"/>
                    <path d="M 110 115 C 110 170, 210 170, 210 115" fill="none" stroke="#3b82f6" stroke-width="2" stroke-dasharray="4,3"/>
                    <path d="M 80 85 C 80 10, 240 10, 240 85" fill="none" stroke="#93c5fd" stroke-width="1.5"/>
                    <path d="M 80 115 C 80 190, 240 190, 240 115" fill="none" stroke="#93c5fd" stroke-width="1.5"/>
                    <!-- Magnet Bar -->
                    <rect x="95" y="80" width="65" height="40" rx="3" fill="#ef4444"/>
                    <text x="127" y="105" font-size="16" font-weight="bold" fill="#ffffff" text-anchor="middle">$nText</text>
                    <rect x="160" y="80" width="65" height="40" rx="3" fill="#3b82f6"/>
                    <text x="192" y="105" font-size="16" font-weight="bold" fill="#ffffff" text-anchor="middle">$sText</text>
                    <!-- Label -->
                    <text x="160" y="35" font-size="11" font-weight="bold" fill="#1e40af" text-anchor="middle">$linesText</text>
                </svg>
                """.trimIndent()
            }
        ),

        // ==================== 2. الرياضيات والهندسة ====================
        DiagramTemplate(
            key = "math_right_triangle",
            title = "مثلث قائم الزاوية (مبرهنة فيثاغورس)",
            category = "MATHEMATICS",
            categoryTitleAr = "الرياضيات والهندسة",
            defaultLabels = mapOf(
                "a" to "أ = 3 سم",
                "b" to "ب = 4 سم",
                "c" to "جـ (الوتر) = 5 سم",
                "angle" to "زاوية قائمة (90°)"
            ),
            generator = { labels, hide ->
                val aText = if (hide) "(1) الضلع أ" else labels["a"] ?: "3 سم"
                val bText = if (hide) "(2) الضلع ب" else labels["b"] ?: "4 سم"
                val cText = if (hide) "(3) الوتر جـ" else labels["c"] ?: "جـ = ؟"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <!-- Triangle Polygon -->
                    <polygon points="70,160 250,160 70,40" fill="#e0f2fe" stroke="#0284c7" stroke-width="3" stroke-linejoin="round"/>
                    <!-- Right Angle Symbol -->
                    <rect x="70" y="142" width="18" height="18" fill="none" stroke="#0f172a" stroke-width="2"/>
                    <!-- Labels -->
                    <text x="45" y="105" font-size="13" font-weight="bold" fill="#0369a1" text-anchor="end">$aText</text>
                    <text x="160" y="180" font-size="13" font-weight="bold" fill="#0369a1" text-anchor="middle">$bText</text>
                    <text x="175" y="90" font-size="13" font-weight="bold" fill="#dc2626">$cText</text>
                </svg>
                """.trimIndent()
            }
        ),

        DiagramTemplate(
            key = "math_circle_geometry",
            title = "الدائرة وعناصرها (نصف القطر، القطر، الوتر)",
            category = "MATHEMATICS",
            categoryTitleAr = "الرياضيات والهندسة",
            defaultLabels = mapOf(
                "center" to "م (المركز)",
                "radius" to "نق = 7 سم",
                "diameter" to "القطر"
            ),
            generator = { labels, hide ->
                val rText = if (hide) "(1) نصف القطر" else labels["radius"] ?: "نق = 7 سم"
                val cText = if (hide) "(2) المركز" else labels["center"] ?: "م"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <!-- Circle -->
                    <circle cx="160" cy="100" r="70" fill="#f0fdf4" stroke="#16a34a" stroke-width="3"/>
                    <!-- Center Dot -->
                    <circle cx="160" cy="100" r="4" fill="#0f172a"/>
                    <text x="155" y="92" font-size="12" font-weight="bold" fill="#0f172a">$cText</text>
                    <!-- Radius Line -->
                    <line x1="160" y1="100" x2="230" y2="100" stroke="#dc2626" stroke-width="2.5"/>
                    <text x="195" y="92" font-size="12" font-weight="bold" fill="#dc2626" text-anchor="middle">$rText</text>
                    <!-- Chord / Diameter dashed -->
                    <line x1="90" y1="100" x2="160" y2="100" stroke="#94a3b8" stroke-width="2" stroke-dasharray="3,3"/>
                </svg>
                """.trimIndent()
            }
        ),

        DiagramTemplate(
            key = "math_cartesian_plane",
            title = "المحاور الإحداثية (س، ص) وتعيين نقطة",
            category = "MATHEMATICS",
            categoryTitleAr = "الرياضيات والهندسة",
            defaultLabels = mapOf(
                "point" to "النقطة أ (3، 4)",
                "x_axis" to "محور السينات (س)",
                "y_axis" to "محور الصادات (ص)"
            ),
            generator = { labels, hide ->
                val pText = if (hide) "(1) إحداثيات النقطة أ" else labels["point"] ?: "أ (3، 4)"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <!-- Grid Lines -->
                    <path d="M 60 40 L 60 160 M 110 40 L 110 160 M 210 40 L 210 160 M 260 40 L 260 160 M 30 70 L 290 70 M 30 130 L 290 130" stroke="#e2e8f0" stroke-width="1"/>
                    <!-- Axes -->
                    <line x1="30" y1="100" x2="290" y2="100" stroke="#0f172a" stroke-width="2.5"/>
                    <line x1="160" y1="20" x2="160" y2="180" stroke="#0f172a" stroke-width="2.5"/>
                    <text x="285" y="92" font-size="12" font-weight="bold" fill="#0f172a">س</text>
                    <text x="168" y="32" font-size="12" font-weight="bold" fill="#0f172a">ص</text>
                    <!-- Plotted Point -->
                    <circle cx="210" cy="60" r="5" fill="#dc2626"/>
                    <text x="220" y="58" font-size="12" font-weight="bold" fill="#b91c1c">$pText</text>
                    <line x1="210" y1="100" x2="210" y2="60" stroke="#dc2626" stroke-width="1.5" stroke-dasharray="3,3"/>
                    <line x1="160" y1="60" x2="210" y2="60" stroke="#dc2626" stroke-width="1.5" stroke-dasharray="3,3"/>
                </svg>
                """.trimIndent()
            }
        ),

        // ==================== 3. العلوم والأحياء ====================
        DiagramTemplate(
            key = "biology_plant_cell",
            title = "الخلية النباتية وأجزاؤها الرئيسية",
            category = "BIOLOGY",
            categoryTitleAr = "العلوم والأحياء",
            defaultLabels = mapOf(
                "p1" to "جدار الخلية والغشاء الخلوي",
                "p2" to "النواة والمادة الوراثية",
                "p3" to "البلاستيدات الخضراء",
                "p4" to "الفجوة العصارية الكبيرة"
            ),
            generator = { labels, hide ->
                val p1 = if (hide) "(1)" else labels["p1"] ?: "جدار الخلية"
                val p2 = if (hide) "(2)" else labels["p2"] ?: "النواة"
                val p3 = if (hide) "(3)" else labels["p3"] ?: "البلاستيدات"
                val p4 = if (hide) "(4)" else labels["p4"] ?: "الفجوة العصارية"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <!-- Cell Wall -->
                    <rect x="50" y="25" width="220" height="150" rx="18" fill="#dcfce7" stroke="#15803d" stroke-width="4"/>
                    <!-- Plasma Membrane -->
                    <rect x="58" y="33" width="204" height="134" rx="14" fill="#bbf7d0" stroke="#22c55e" stroke-width="2"/>
                    <!-- Large Vacuole -->
                    <ellipse cx="140" cy="100" rx="45" ry="32" fill="#bae6fd" stroke="#0284c7" stroke-width="2"/>
                    <text x="140" y="104" font-size="10" font-weight="bold" fill="#0369a1" text-anchor="middle">$p4</text>
                    <!-- Nucleus -->
                    <circle cx="215" cy="80" r="20" fill="#fbcfe8" stroke="#db2777" stroke-width="2"/>
                    <circle cx="215" cy="80" r="7" fill="#be185d"/>
                    <text x="215" y="112" font-size="10" font-weight="bold" fill="#9d174d" text-anchor="middle">$p2</text>
                    <!-- Chloroplasts -->
                    <ellipse cx="90" cy="60" rx="14" ry="9" fill="#16a34a"/>
                    <ellipse cx="90" cy="140" rx="14" ry="9" fill="#16a34a"/>
                    <text x="90" y="160" font-size="10" font-weight="bold" fill="#14532d" text-anchor="middle">$p3</text>
                    <!-- Pointer to Wall -->
                    <line x1="250" y1="20" x2="230" y2="30" stroke="#15803d" stroke-width="1.5"/>
                    <text x="255" y="18" font-size="10" font-weight="bold" fill="#15803d">$p1</text>
                </svg>
                """.trimIndent()
            }
        ),

        DiagramTemplate(
            key = "biology_plant_parts",
            title = "أجزاء النبات (الجذور، الساق، الأوراق، الزهرة)",
            category = "BIOLOGY",
            categoryTitleAr = "العلوم والأحياء",
            defaultLabels = mapOf(
                "flower" to "الزهرة (التكاثر)",
                "leaf" to "الأوراق (البناء الضوئي)",
                "stem" to "الساق (نقل الغذاء والماء)",
                "roots" to "الجذور (التثبيت والامتصاص)"
            ),
            generator = { labels, hide ->
                val fText = if (hide) "(1)" else labels["flower"] ?: "الزهرة"
                val lText = if (hide) "(2)" else labels["leaf"] ?: "الأوراق"
                val sText = if (hide) "(3)" else labels["stem"] ?: "الساق"
                val rText = if (hide) "(4)" else labels["roots"] ?: "الجذور"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <!-- Ground Line -->
                    <line x1="50" y1="145" x2="270" y2="145" stroke="#78350f" stroke-width="3"/>
                    <rect x="50" y="146" width="220" height="40" fill="#fef3c7" opacity="0.6"/>
                    <!-- Stem -->
                    <line x1="160" y1="50" x2="160" y2="145" stroke="#15803d" stroke-width="6" stroke-linecap="round"/>
                    <text x="180" y="105" font-size="11" font-weight="bold" fill="#15803d">$sText</text>
                    <!-- Flower -->
                    <circle cx="160" cy="40" r="14" fill="#fbbf24"/>
                    <circle cx="160" cy="40" r="7" fill="#ea580c"/>
                    <text x="160" y="22" font-size="11" font-weight="bold" fill="#d97706" text-anchor="middle">$fText</text>
                    <!-- Leaves -->
                    <path d="M 160 80 Q 120 70 110 90 Q 140 100 160 85" fill="#22c55e" stroke="#16a34a"/>
                    <text x="100" y="85" font-size="11" font-weight="bold" fill="#15803d" text-anchor="end">$lText</text>
                    <!-- Roots -->
                    <path d="M 160 145 L 140 175 M 160 145 L 160 185 M 160 145 L 180 175 M 140 160 L 125 170" stroke="#92400e" stroke-width="2.5" fill="none"/>
                    <text x="200" y="170" font-size="11" font-weight="bold" fill="#78350f">$rText</text>
                </svg>
                """.trimIndent()
            }
        ),

        // ==================== 4. الكيمياء ====================
        DiagramTemplate(
            key = "chemistry_bohr_atom",
            title = "نموذج بور الذري (النواة والإلكترونات والمدارات)",
            category = "CHEMISTRY",
            categoryTitleAr = "الكيمياء",
            defaultLabels = mapOf(
                "nucleus" to "النواة (بروتونات + نيوترونات)",
                "shell_k" to "المدار K (2 إلكترون)",
                "shell_l" to "المدار L (8 إلكترونات)",
                "electron" to "إلكترون سالف الشحنة (e-)"
            ),
            generator = { labels, hide ->
                val nuc = if (hide) "(1) النواة" else labels["nucleus"] ?: "النواة"
                val el = if (hide) "(2) إلكترون" else labels["electron"] ?: "إلكترون"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <!-- Orbits -->
                    <circle cx="160" cy="100" r="40" fill="none" stroke="#94a3b8" stroke-width="1.5" stroke-dasharray="3,3"/>
                    <circle cx="160" cy="100" r="70" fill="none" stroke="#94a3b8" stroke-width="1.5" stroke-dasharray="3,3"/>
                    <!-- Nucleus -->
                    <circle cx="160" cy="100" r="18" fill="#f87171" stroke="#dc2626" stroke-width="2"/>
                    <text x="160" y="104" font-size="9" font-weight="bold" fill="#ffffff" text-anchor="middle">+ / 0</text>
                    <text x="160" y="130" font-size="11" font-weight="bold" fill="#b91c1c" text-anchor="middle">$nuc</text>
                    <!-- Electrons on orbit 1 -->
                    <circle cx="160" cy="60" r="4.5" fill="#3b82f6"/>
                    <circle cx="160" cy="140" r="4.5" fill="#3b82f6"/>
                    <!-- Electrons on orbit 2 -->
                    <circle cx="90" cy="100" r="4.5" fill="#3b82f6"/>
                    <circle cx="230" cy="100" r="4.5" fill="#3b82f6"/>
                    <text x="245" y="95" font-size="11" font-weight="bold" fill="#1d4ed8">$el</text>
                </svg>
                """.trimIndent()
            }
        ),

        // ==================== 5. الجغرافيا والاجتماعيات ====================
        DiagramTemplate(
            key = "geography_compass",
            title = "وردة البوصلة والاتجاهات الجغرافية الأربعة",
            category = "GEOGRAPHY",
            categoryTitleAr = "الجغرافيا والاجتماعيات",
            defaultLabels = mapOf(
                "n" to "الشمال (ش)",
                "s" to "الجنوب (ج)",
                "e" to "الشرق (ق)",
                "w" to "الغرب (غ)"
            ),
            generator = { labels, hide ->
                val nText = if (hide) "(1)" else labels["n"] ?: "شمال"
                val sText = if (hide) "(2)" else labels["s"] ?: "جنوب"
                val eText = if (hide) "(3)" else labels["e"] ?: "شرق"
                val wText = if (hide) "(4)" else labels["w"] ?: "غرب"

                """
                <svg viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg" style="background:#f8fafc; border-radius:8px; font-family:sans-serif;">
                    <rect x="2" y="2" width="316" height="196" rx="8" fill="none" stroke="#cbd5e1" stroke-width="1.5"/>
                    <circle cx="160" cy="100" r="60" fill="#f1f5f9" stroke="#94a3b8" stroke-width="2"/>
                    <!-- Compass Stars -->
                    <polygon points="160,35 152,95 160,90 168,95" fill="#dc2626"/>
                    <polygon points="160,165 152,105 160,110 168,105" fill="#475569"/>
                    <polygon points="225,100 165,92 170,100 165,108" fill="#0284c7"/>
                    <polygon points="95,100 155,92 150,100 155,108" fill="#475569"/>
                    <!-- Labels -->
                    <text x="160" y="26" font-size="12" font-weight="bold" fill="#dc2626" text-anchor="middle">$nText</text>
                    <text x="160" y="182" font-size="12" font-weight="bold" fill="#334155" text-anchor="middle">$sText</text>
                    <text x="240" y="104" font-size="12" font-weight="bold" fill="#0369a1">$eText</text>
                    <text x="80" y="104" font-size="12" font-weight="bold" fill="#334155" text-anchor="end">$wText</text>
                </svg>
                """.trimIndent()
            }
        )
    )

    fun getTemplateByKey(key: String): DiagramTemplate? {
        return ALL_TEMPLATES.find { it.key == key }
    }

    fun buildDiagramInfo(
        templateKey: String,
        customLabels: Map<String, String> = emptyMap(),
        hideLabelsForQuiz: Boolean = false
    ): DiagramInfo {
        val template = getTemplateByKey(templateKey) ?: ALL_TEMPLATES.first()
        val mergedLabels = template.defaultLabels.toMutableMap().apply { putAll(customLabels) }
        val svg = template.generator(mergedLabels, hideLabelsForQuiz)
        return DiagramInfo(
            title = template.title,
            category = template.category,
            svgContent = svg,
            labelsJson = org.json.JSONObject(mergedLabels).toString(),
            hideLabelsForQuiz = hideLabelsForQuiz
        )
    }
}
