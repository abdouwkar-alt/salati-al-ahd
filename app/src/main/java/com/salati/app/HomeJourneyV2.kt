package com.salati.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * الاستخدام: في MainActivity.kt غيّر استدعاء HomeDashboard( إلى HomeDashboardV2( — نفس المعاملات بالضبط.
 * كل الأسماء هنا بادئتها Jv عشان ما تتعارض مع NavigationShell.kt.
 */

private val JvGreen = Color(0xFF164B3A)
private val JvDeep = Color(0xFF0E3226)
private val JvCream = Color(0xFFF6F2E9)
private val JvGold = Color(0xFFD9B66B)

/** يحفظ آخر باب وصل له الماشي، فالرحلة تكمل من مكانها بدل ما ترجع من الأول كل مرة. */
private object JvJourneyMemory { var node = 0 }

private class JvPalette(
    val stoneTop: Color, val stoneBottom: Color, val stoneLine: Color,
    val woodTop: Color, val woodBottom: Color, val woodLine: Color, val accent: Color
)

private val JvWoodPalette = JvPalette(
    Color(0xFFF0E7D2), Color(0xFFCDBE9C), Color(0xFFA89A78),
    Color(0xFFA67B52), Color(0xFF7A5434), Color(0xFF5E4029), Color(0xFFE0C18A)
)
private val JvPrayerPalette = JvPalette(
    Color(0xFFF6EBC8), Color(0xFFD9B66B), Color(0xFFB18F45),
    Color(0xFF2A6A52), Color(0xFF164B3A), Color(0xFF0E3226), Color(0xFFF1D58F)
)

private class JvDoorSpec(
    val title: String, val section: String, val destination: String,
    val x: Float, val y: Float, val icon: ImageVector
)

@Composable
fun HomeDashboardV2(
    dateText: String,
    onOpen: (section: String, destination: String, subTab: String) -> Unit,
    modifier: Modifier = Modifier,
    nextPrayerName: String = "",
    nextPrayerTime: String = "",
    nextPrayerCountdown: String = ""
) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        JvHeader(
            dateText,
            onOpenLearning = { onOpen("البداية", "التعلم_الأسبوعي", "حديث الأسبوع") },
            onOpenRoutine = { onOpen("روتين يومك", "روتين_اليوم", "التذكيرات") },
            onOpenAwareness = { onOpen("وعي", "وعي", "مسارات الوعي") }
        )
        if (nextPrayerName.isNotBlank()) {
            JvPrayerFocus(
                prayerName = nextPrayerName,
                prayerTime = nextPrayerTime,
                countdown = nextPrayerCountdown,
                onOpenPrayer = { onOpen("الصلاة", "الصلاة", "تسجيل الصلاة") }
            )
        }
        JvJourney(onOpen, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun JvPrayerFocus(
    prayerName: String,
    prayerTime: String,
    countdown: String,
    onOpenPrayer: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenPrayer),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFE9E4D8),
        border = androidx.compose.foundation.BorderStroke(1.dp, JvGold.copy(alpha = .55f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("الصلاة القادمة", color = JvGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(prayerName, color = JvDeep, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(prayerTime, color = JvGreen, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("متبقي على الأذان", color = Color(0xFF756A55), style = MaterialTheme.typography.labelSmall)
                Text(countdown, color = JvDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("افتح متابعة الصلاة", color = JvGreen, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun JvHeader(dateText: String, onOpenLearning: () -> Unit, onOpenRoutine: () -> Unit, onOpenAwareness: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(JvDeep, JvGreen, Color(0xFF1F5E48))))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "أشهد أن لا إله إلا الله، وأشهد أن محمدًا رسول الله",
                color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "رضيت بالله ربًا، وبالإسلام دينًا، وبمحمدٍ رسولًا",
                color = JvGold, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall
            )
            Text(dateText, color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "وعي",
                    modifier = Modifier.clickable(onClick = onOpenAwareness).padding(vertical = 2.dp),
                    color = JvGold, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "تعلّم حديث الأسبوع",
                    modifier = Modifier.clickable(onClick = onOpenLearning).padding(vertical = 2.dp),
                    color = JvGold, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    "روتين يومك",
                    modifier = Modifier.clickable(onClick = onOpenRoutine).padding(vertical = 2.dp),
                    color = Color.White, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(.35f).height(1.dp)
                .background(JvGold.copy(alpha = .55f))
        )
    }
}

// ───────────────────────── المسار ─────────────────────────

private class JvRoute(val xs: FloatArray, val ys: FloatArray, val dist: FloatArray, val nodeDist: FloatArray) {
    val length: Float get() = dist[dist.lastIndex]
}

private fun jvBuildRoute(points: List<Offset>): JvRoute {
    val perSeg = 26
    val n = (points.size - 1) * perSeg + 1
    val xs = FloatArray(n); val ys = FloatArray(n); val dist = FloatArray(n)
    val nodeDist = FloatArray(points.size)
    xs[0] = points[0].x; ys[0] = points[0].y
    var idx = 0
    for (seg in 0 until points.size - 1) {
        val p0 = points[seg]; val p1 = points[seg + 1]
        val dy = p1.y - p0.y
        val c1 = Offset(p0.x, p0.y + dy * .55f)
        val c2 = Offset(p1.x, p1.y - dy * .55f)
        for (step in 1..perSeg) {
            val t = step / perSeg.toFloat(); val u = 1f - t
            val x = u * u * u * p0.x + 3f * u * u * t * c1.x + 3f * u * t * t * c2.x + t * t * t * p1.x
            val y = u * u * u * p0.y + 3f * u * u * t * c1.y + 3f * u * t * t * c2.y + t * t * t * p1.y
            idx++
            xs[idx] = x; ys[idx] = y
            dist[idx] = dist[idx - 1] + hypot(x - xs[idx - 1], y - ys[idx - 1])
        }
        nodeDist[seg + 1] = dist[idx]
    }
    return JvRoute(xs, ys, dist, nodeDist)
}

private fun JvRoute.pointAt(d: Float): Offset {
    val dd = d.coerceIn(0f, length)
    var i = java.util.Arrays.binarySearch(dist, dd)
    if (i < 0) i = -i - 1
    i = i.coerceIn(1, dist.lastIndex)
    val seg = (dist[i] - dist[i - 1]).coerceAtLeast(0.001f)
    val f = ((dd - dist[i - 1]) / seg).coerceIn(0f, 1f)
    return Offset(xs[i - 1] + (xs[i] - xs[i - 1]) * f, ys[i - 1] + (ys[i] - ys[i - 1]) * f)
}

// ───────────────────────── المشهد ─────────────────────────

@Composable
private fun JvJourney(onOpen: (String, String, String) -> Unit, modifier: Modifier = Modifier) {
    val doors = remember {
        listOf(
            JvDoorSpec("وعي", "وعي", "وعي", .76f, .31f, Icons.Filled.School),
            JvDoorSpec("الخير والناس", "الإنفاق", "الإنفاق", .24f, .47f, Icons.Filled.VolunteerActivism),
            JvDoorSpec("القرآن", "تدبر القرآن", "تدبر القرآن", .76f, .63f, Icons.Filled.MenuBook),
            JvDoorSpec("روتين اليوم", "روتين يومك", "روتين_اليوم", .24f, .79f, Icons.Filled.AutoAwesome)
        )
    }
    val verses = remember {
        listOf(
            "وَاللَّهُ يَهْدِي مَن يَشَاءُ إِلَىٰ صِرَاطٍ مُّسْتَقِيمٍ",
            "وَأَنْ لَيْسَ لِلْإِنْسَانِ إِلَّا مَا سَعَىٰ",
            "وَأَنَّ سَعْيَهُ سَوْفَ يُرَىٰ",
            "فَإِذَا فَرَغْتَ فَانْصَبْ",
            "وَأَنَّ إِلَىٰ رَبِّكَ الْمُنْتَهَىٰ",
            "إِنَّ سَعْيَكُمْ لَشَتَّىٰ",
            "فَاسْتَبِقُوا الْخَيْرَاتِ",
            "وَسَارِعُوا إِلَىٰ مَغْفِرَةٍ مِنْ رَبِّكُمْ"
        )
    }
    val idleVerse = "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ"

    var target by remember { mutableIntStateOf(-1) }
    var walking by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val inf = rememberInfiniteTransition(label = "jv")
    val breath = inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "breath")
    val flicker = inf.animateFloat(.7f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "flicker")
    val pulse = inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val moving = animateFloatAsState(if (walking) 1f else 0f, tween(220), label = "moving")
    val doorOpen = remember { Animatable(0f) }
    val enter = remember { Animatable(0f) }

    // نخلي المشهد LTR دايمًا عشان الإحداثيات والأبواب ما تتعكسش لو الجهاز RTL.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier) {
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val doorW = (maxWidth * .20f).coerceIn(58.dp, 80.dp)
            val doorH = (maxHeight * .095f).coerceIn(46.dp, 64.dp)
            val prayerW = (maxWidth * .28f).coerceIn(84.dp, 118.dp)
            val prayerH = (maxHeight * .14f).coerceIn(72.dp, 100.dp)
            val labelH = 18.dp
            val walkerW = 34.dp
            val walkerH = 56.dp

            val nodes = remember(wPx, hPx) {
                buildList {
                    add(Offset(wPx * .5f, hPx * .165f))
                    doors.forEach { add(Offset(wPx * it.x, hPx * it.y)) }
                    add(Offset(wPx * .5f, hPx * .965f))
                }
            }
            val route = remember(nodes) { jvBuildRoute(nodes) }
            val progress = remember(route) { Animatable(route.nodeDist[JvJourneyMemory.node.coerceIn(0, doors.size + 1)]) }
            val stridePx = with(density) { 26.dp.toPx() }

            // ── الخلفية: تدرج + زخرفة نجمية خفيفة + هالة الباب الأخير
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFF8F4EA), Color(0xFFE6EBDD), Color(0xFFCFDCCB))))
                val cell = 46.dp.toPx()
                val a = cell * .30f
                val line = JvGreen.copy(alpha = .05f)
                val cols = (size.width / cell).toInt() + 2
                val rows = (size.height / (cell * .86f)).toInt() + 2
                for (r in 0 until rows) for (c in 0 until cols) {
                    val cx = c * cell + if (r % 2 == 0) 0f else cell / 2f
                    val cy = r * cell * .86f
                    drawRect(line, Offset(cx - a, cy - a), Size(2 * a, 2 * a), style = Stroke(1.dp.toPx()))
                    rotate(45f, Offset(cx, cy)) {
                        drawRect(line, Offset(cx - a, cy - a), Size(2 * a, 2 * a), style = Stroke(1.dp.toPx()))
                    }
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val p = pulse.value
                val c = nodes.last().copy(y = nodes.last().y - prayerH.toPx() * .45f)
                val r = size.width * (.36f + .05f * p)
                drawCircle(
                    brush = Brush.radialGradient(listOf(JvGold.copy(alpha = .22f + .14f * p), Color.Transparent), center = c, radius = r),
                    radius = r, center = c
                )
            }

            // ── الطريق: عرضه يكبر كل ما نزل (منظور)
            Canvas(Modifier.fillMaxSize()) {
                val n = route.xs.size
                for (i in 1 until n) {
                    val k = (route.ys[i] / size.height).coerceIn(0f, 1f)
                    drawLine(
                        Color(0xFFBFAE86).copy(alpha = .55f),
                        Offset(route.xs[i - 1], route.ys[i - 1]), Offset(route.xs[i], route.ys[i]),
                        strokeWidth = (11f + 10f * k).dp.toPx(), cap = StrokeCap.Round
                    )
                }
                for (i in 1 until n) {
                    val k = (route.ys[i] / size.height).coerceIn(0f, 1f)
                    drawLine(
                        Color(0xFFEFE4C9),
                        Offset(route.xs[i - 1], route.ys[i - 1]), Offset(route.xs[i], route.ys[i]),
                        strokeWidth = (8f + 9f * k).dp.toPx(), cap = StrokeCap.Round
                    )
                }
                val center = Path().apply {
                    moveTo(route.xs[0], route.ys[0])
                    for (i in 1 until n) lineTo(route.xs[i], route.ys[i])
                }
                drawPath(
                    center, Color(0xFFB59F6B).copy(alpha = .7f),
                    style = Stroke(1.3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 7.dp.toPx())))
                )
            }
            // الجزء اللي اتمشى: خط ذهبي بيلمع ورا الماشي
            Canvas(Modifier.fillMaxSize()) {
                val cur = progress.value
                if (cur > 1f) {
                    val path = Path().apply {
                        moveTo(route.xs[0], route.ys[0])
                        var i = 1
                        while (i < route.xs.size && route.dist[i] <= cur) { lineTo(route.xs[i], route.ys[i]); i++ }
                        val p = route.pointAt(cur); lineTo(p.x, p.y)
                    }
                    drawPath(path, JvGold.copy(alpha = .28f), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
                    drawPath(path, JvGold, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
                }
            }

            // ── الأبواب
            doors.forEachIndexed { index, door ->
                val node = nodes[index + 1]
                val totalPx = with(density) { (labelH + 8.dp + doorH).toPx() }
                val wHalf = with(density) { doorW.toPx() } / 2f
                val depth = .86f + .14f * (door.y)
                Column(
                    Modifier
                        .absoluteOffset { IntOffset((node.x - wHalf).roundToInt(), (node.y - totalPx).roundToInt()) }
                        .width(doorW)
                        .graphicsLayer { scaleX = depth; scaleY = depth; transformOrigin = TransformOrigin(.5f, 1f) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    JvLabel(door.title, Modifier.height(labelH))
                    Spacer(Modifier.height(8.dp))
                    JvDoorView(
                        title = door.title, icon = door.icon, palette = JvWoodPalette,
                        doorW = doorW, doorH = doorH,
                        open = { if (target == index) doorOpen.value else 0f },
                        onClick = { if (target < 0) target = index }
                    )
                }
            }
            run {
                val node = nodes.last()
                val totalPx = with(density) { (labelH + 8.dp + prayerH).toPx() }
                val wHalf = with(density) { prayerW.toPx() } / 2f
                Column(
                    Modifier
                        .absoluteOffset { IntOffset((node.x - wHalf).roundToInt(), (node.y - totalPx).roundToInt()) }
                        .width(prayerW),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    JvLabel("الصلاة جامعة", Modifier.height(labelH), bold = true)
                    Spacer(Modifier.height(8.dp))
                    JvDoorView(
                        title = "الصلاة", icon = null, palette = JvPrayerPalette,
                        doorW = prayerW, doorH = prayerH,
                        open = { if (target == doors.size) doorOpen.value else 0f },
                        onClick = { if (target < 0) target = doors.size }
                    )
                }
            }

            // ── الماشي
            val faceHolder = remember { FloatArray(1).also { it[0] = 1f } }
            Canvas(
                Modifier
                    .size(walkerW, walkerH)
                    .absoluteOffset {
                        val p = route.pointAt(progress.value)
                        IntOffset(
                            (p.x - walkerW.toPx() / 2f).roundToInt(),
                            (p.y - walkerH.toPx() + 3.dp.toPx()).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        val p = route.pointAt(progress.value)
                        val s = .8f + .2f * (p.y / hPx).coerceIn(0f, 1f)
                        val e = enter.value
                        scaleX = s * (1f - .3f * e); scaleY = s * (1f - .3f * e)
                        alpha = 1f - e
                        transformOrigin = TransformOrigin(.5f, 1f)
                    }
            ) {
                val d = progress.value
                val ahead = route.pointAt(d + 14.dp.toPx())
                val here = route.pointAt(d)
                val dx = ahead.x - here.x
                if (dx > .8f) faceHolder[0] = 1f else if (dx < -.8f) faceHolder[0] = -1f
                val phase = d / stridePx * PI.toFloat()
                jvWalker(
                    phase = phase, moving = moving.value,
                    breath = sin(breath.value * 2f * PI.toFloat()),
                    lanternRight = faceHolder[0] > 0f, flicker = flicker.value
                )
            }

            // ── شريط الآية
            val verseText = if (target < 0) idleVerse else verses[(target + 1) % verses.size]
            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp).fillMaxWidth(.94f),
                color = JvGreen.copy(alpha = .94f), shape = RoundedCornerShape(14.dp), shadowElevation = 3.dp
            ) {
                Crossfade(targetState = verseText, animationSpec = tween(350), label = "verse") { v ->
                    Text(
                        v, color = JvCream, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ── تسلسل الحركة: مشي ← فتح الباب ← دخول ← انتقال
            LaunchedEffect(target) {
                if (target < 0) return@LaunchedEffect
                val goalNode = target + 1
                val goal = route.nodeDist[goalNode]
                val distDp = abs(goal - progress.value) / density.density
                if (distDp > 1f) {
                    walking = true
                    val ms = (distDp / 130f * 1000f).toInt().coerceIn(650, 4200)
                    progress.animateTo(goal, tween(ms, easing = CubicBezierEasing(.45f, 0f, .2f, 1f)))
                    walking = false
                }
                JvJourneyMemory.node = goalNode
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                doorOpen.animateTo(1f, spring(dampingRatio = .8f, stiffness = 90f))
                delay(180)
                enter.animateTo(1f, tween(420))
                if (target == doors.size) onOpen("الصلاة", "الصلاة", "")
                else onOpen(doors[target].section, doors[target].destination, "")
                doorOpen.snapTo(0f); enter.snapTo(0f); target = -1
            }
        }
    }
}

@Composable
private fun JvLabel(text: String, modifier: Modifier = Modifier, bold: Boolean = false) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text, color = JvGreen, fontSize = 11.sp, maxLines = 1,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .background(JvCream.copy(alpha = .88f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

// ───────────────────────── الباب ─────────────────────────

private fun jvArch(l: Float, t: Float, r: Float, b: Float): Path {
    val w = r - l; val h = b - t
    return Path().apply {
        moveTo(l, b)
        lineTo(l, t + h * .40f)
        cubicTo(l, t + h * .16f, l + w * .30f, t + h * .04f, l + w * .5f, t)
        cubicTo(l + w * .70f, t + h * .04f, r, t + h * .16f, r, t + h * .40f)
        lineTo(r, b)
        close()
    }
}

private fun DrawScope.jvLeaf(right: Boolean, p: JvPalette) {
    val lw = size.width; val h = size.height; val wi = lw * 2f
    fun x(v: Float) = if (right) lw - v else v
    val path = Path().apply {
        moveTo(x(0f), h); lineTo(x(0f), h * .40f)
        cubicTo(x(0f), h * .16f, x(wi * .30f), h * .04f, x(lw), 0f)
        lineTo(x(lw), h); close()
    }
    drawPath(path, Brush.verticalGradient(listOf(p.woodTop, p.woodBottom)))
    // ألواح الخشب
    val plank = p.woodLine.copy(alpha = .45f)
    drawLine(plank, Offset(lw * .34f, h * .14f), Offset(lw * .34f, h), 1.dp.toPx())
    drawLine(plank, Offset(lw * .67f, h * .08f), Offset(lw * .67f, h), 1.dp.toPx())
    // إطار داخلي (حشوة)
    drawRoundRect(
        p.accent.copy(alpha = .38f), Offset(lw * .16f, h * .36f), Size(lw * .68f, h * .5f),
        CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx())
    )
    // مسامير
    val stud = p.accent.copy(alpha = .9f)
    drawCircle(stud, 1.4.dp.toPx(), Offset(x(lw * .2f), h * .30f))
    drawCircle(stud, 1.4.dp.toPx(), Offset(x(lw * .2f), h * .92f))
    // حلقة المقبض
    drawCircle(p.accent, 3.dp.toPx(), Offset(x(lw * .82f), h * .60f), style = Stroke(1.4.dp.toPx()))
    drawPath(path, p.woodLine, style = Stroke(1.dp.toPx()))
}

@Composable
private fun JvDoorView(
    title: String,
    icon: ImageVector?,
    palette: JvPalette,
    doorW: Dp,
    doorH: Dp,
    open: () -> Float,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed = source.collectIsPressedAsState()
    val pressScale = animateFloatAsState(if (pressed.value) .94f else 1f, tween(90), label = "press")
    val frame = doorW * .09f
    Box(
        Modifier
            .size(doorW, doorH)
            .graphicsLayer {
                scaleX = pressScale.value; scaleY = pressScale.value
                transformOrigin = TransformOrigin(.5f, 1f)
            }
            .semantics { contentDescription = "باب $title" }
            .clickable(interactionSource = source, indication = null, onClickLabel = title, role = Role.Button, onClick = onClick)
    ) {
        // الإطار الحجري + الضوء الداخلي + بقعة ضوء على الأرض
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height; val f = frame.toPx()
            val o = open()
            if (o > 0.01f) {
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFFE9A8), Color.Transparent),
                        center = Offset(w / 2f, h), radius = w * .9f
                    ),
                    topLeft = Offset(-w * .25f, h - h * .05f), size = Size(w * 1.5f, h * .22f), alpha = o * .85f
                )
            }
            val outer = jvArch(0f, 0f, w, h)
            drawPath(outer, Brush.verticalGradient(listOf(palette.stoneTop, palette.stoneBottom)))
            drawPath(outer, palette.stoneLine, style = Stroke(1.2.dp.toPx()))
            val inner = jvArch(f, f, w - f, h)
            drawPath(
                inner,
                Brush.radialGradient(
                    listOf(Color(0xFFFFF6D8), Color(0xFFF2C766), Color(0xFFB5762A)),
                    center = Offset(w / 2f, h * .62f), radius = h * .8f
                )
            )
        }
        // الضلفتان
        Row(Modifier.fillMaxSize().padding(start = frame, end = frame, top = frame)) {
            for (right in listOf(false, true)) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().graphicsLayer {
                        rotationY = (if (right) 72f else -72f) * open()
                        transformOrigin = TransformOrigin(if (right) 1f else 0f, .5f)
                        cameraDistance = 14f * density
                    }
                ) { Canvas(Modifier.fillMaxSize()) { jvLeaf(right, palette) } }
            }
        }
        // شارة الباب على القمة: أيقونة القسم (أو هلال للصلاة)
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-9).dp).size(20.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFF1D58F), Color(0xFFC99A3E))), CircleShape)
                .border(1.dp, Color(0xFF8A6A2A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = JvDeep)
            } else {
                Canvas(Modifier.size(12.dp)) {
                    val r = size.minDimension / 2f
                    val moon = Path.combine(
                        PathOperation.Difference,
                        Path().apply { addOval(Rect(Offset.Zero, size)) },
                        Path().apply { addOval(Rect(Offset(r * .55f, -r * .1f), Size(size.width * .9f, size.height * .9f))) }
                    )
                    drawPath(moon, JvDeep)
                }
            }
        }
    }
}

// ───────────────────────── الماشي ─────────────────────────

/** شخصية مبسطة بثوب وطاقية وفانوس: خطوات متبادلة، اهتزاز خفيف في الجسم، وحركة ذيل الثوب. */
private fun DrawScope.jvWalker(phase: Float, moving: Float, breath: Float, lanternRight: Boolean, flicker: Float) {
    val u = size.height / 56f
    val cx = size.width / 2f
    val swing = sin(phase)
    val bob = (abs(swing) * 1.7f * moving + breath * .6f * (1f - moving)) * u
    val top = 19f * u - bob
    val sway = swing * 1.6f * moving * u
    val robeDark = Color(0xFF164B3A)
    val robeLight = Color(0xFF2F6B55)
    val skin = Color(0xFFD3A57A)

    // ظل
    drawOval(Color.Black.copy(alpha = .16f), Offset(cx - 11f * u, 50f * u), Size(22f * u, 5f * u))
    // القدمان (تتناوبان في الرفع)
    for (k in 0..1) {
        val sgn = if (k == 0) 1f else -1f
        val lift = max(0f, sin(phase + k * PI.toFloat())) * 2.4f * moving
        drawRoundRect(
            Color(0xFF2E2A24),
            Offset(cx + sgn * 4.2f * u - 3f * u, 49.6f * u - lift * u), Size(6f * u, 3f * u), CornerRadius(1.5f * u)
        )
    }
    // الرقبة
    drawRect(skin, Offset(cx - 1.8f * u, 16f * u - bob), Size(3.6f * u, 4.5f * u))
    // الثوب
    val robe = Path().apply {
        moveTo(cx - 6.5f * u, top + 1.5f * u)
        quadraticBezierTo(cx, top - 1.5f * u, cx + 6.5f * u, top + 1.5f * u)
        cubicTo(cx + 8f * u, top + 14f * u, cx + 10.5f * u + sway, 40f * u, cx + 10f * u + sway, 47.5f * u)
        quadraticBezierTo(cx + sway * .5f, 49.8f * u, cx - 10f * u + sway, 47.5f * u)
        cubicTo(cx - 10.5f * u + sway, 40f * u, cx - 8f * u, top + 14f * u, cx - 6.5f * u, top + 1.5f * u)
        close()
    }
    drawPath(robe, Brush.verticalGradient(listOf(robeLight, robeDark), startY = top, endY = 48f * u))
    // حزام ذهبي
    drawLine(JvGold.copy(alpha = .9f), Offset(cx - 7.4f * u, 30f * u - bob), Offset(cx + 7.4f * u, 30f * u - bob), 1.8f * u)

    // الذراعان
    val armColor = Color(0xFF1D5A46)
    val lanternSide = if (lanternRight) 1f else -1f
    val backHand = Offset(cx - lanternSide * (8.6f * u + swing * 1.1f * u * moving), top + 15f * u)
    drawLine(armColor, Offset(cx - lanternSide * 6.2f * u, top + 3.5f * u), backHand, 3.4f * u, cap = StrokeCap.Round)
    val frontHand = Offset(cx + lanternSide * (9.4f * u - swing * 0.9f * u * moving), top + 14.5f * u)
    drawLine(armColor, Offset(cx + lanternSide * 6.2f * u, top + 3.5f * u), frontHand, 3.4f * u, cap = StrokeCap.Round)
    // الفانوس
    val lanternCenter = Offset(frontHand.x, frontHand.y + 5f * u)
    drawCircle(
        brush = Brush.radialGradient(listOf(JvGold.copy(alpha = .55f * flicker), Color.Transparent), center = lanternCenter, radius = 15f * u),
        radius = 15f * u, center = lanternCenter
    )
    drawLine(Color(0xFF6B5320), frontHand, Offset(lanternCenter.x, lanternCenter.y - 3f * u), 0.8f * u)
    drawRoundRect(
        Color(0xFFF1CF7A).copy(alpha = .75f + .25f * flicker),
        Offset(lanternCenter.x - 2.2f * u, lanternCenter.y - 3f * u), Size(4.4f * u, 6f * u), CornerRadius(1.2f * u)
    )
    // الرأس + الطاقية
    val headC = Offset(cx, 11.5f * u - bob)
    drawCircle(skin, 5.4f * u, headC)
    drawArc(
        Color(0xFFF3EEDF), startAngle = 188f, sweepAngle = 164f, useCenter = true,
        topLeft = Offset(headC.x - 5.6f * u, headC.y - 5.6f * u), size = Size(11.2f * u, 11.2f * u)
    )
}