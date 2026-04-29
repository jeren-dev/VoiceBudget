package com.example.voicebudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

class AnalyticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AnalyticsScreen()
            }
        }
    }
}

@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val db = remember { DBHelper(context) }

    // State for Analytics Data
    var totalToday by remember { mutableStateOf(0) }
    var countToday by remember { mutableStateOf(0) }
    var avgSpend by remember { mutableStateOf(0f) }
    var maxSpend by remember { mutableStateOf(0) }

    // Graph Data (Today, Week, Month, Year)
    val chartData = remember { mutableStateListOf(0f, 0f, 0f, 0f) }

    LaunchedEffect(Unit) {
        val todayExpenses = db.getTodayExpenses()
        totalToday = db.getTodayTotal()
        countToday = todayExpenses.size
        maxSpend = todayExpenses.maxOfOrNull { it.amount } ?: 0

        val weekTotal = db.getWeekTotal()
        avgSpend = (weekTotal / 7f)

        // Sync Chart
        chartData[0] = totalToday.toFloat()
        chartData[1] = (weekTotal / 7f)
        chartData[2] = (db.getMonthTotal() / 30f)
        chartData[3] = (db.getYearTotal() / 365f)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05050A))) {
        // Top Left Glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFBB86FC).copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = size.width
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text("Deep Analytics", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("AI-powered financial patterns", fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(30.dp))

            // 1. MAIN CURVE CHART CARD
            AnalyticsGlassCard(modifier = Modifier.height(280.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF03DAC6), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SPENDING VELOCITY", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
                        SmoothCurveChart(dataPoints = chartData, color = Color(0xFFBB86FC))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. BENTO STATS GRID
            Text("Real-time Stats", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatTile(
                    label = "Transactions",
                    value = "$countToday Today",
                    subValue = "Activity volume",
                    color = Color(0xFF03DAC6),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "Max Spend",
                    value = "₹$maxSpend",
                    subValue = "Single highest",
                    color = Color(0xFFCF6679),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StatTile(
                label = "Daily Average",
                value = "₹${String.format("%.2f", avgSpend)}",
                subValue = "Based on current week's performance",
                color = Color(0xFFBB86FC),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. INSIGHT CARD
            AnalyticsGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF03DAC6), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if(totalToday > avgSpend) "You are spending above your daily average today."
                        else "Good job! Your spending is currently below average.",
                        color = Color.White.copy(0.8f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

@Composable
fun AnalyticsGlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(0.04f))
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(30.dp))
    ) {
        content()
    }
}

@Composable
fun StatTile(label: String, value: String, subValue: String, color: Color, modifier: Modifier = Modifier) {
    AnalyticsGlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label.uppercase(), color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(subValue, color = Color.Gray.copy(0.6f), fontSize = 11.sp)
        }
    }
}

@Composable
fun SmoothCurveChart(dataPoints: List<Float>, color: Color) {
    val animateProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        animateProgress.animateTo(1f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (dataPoints.isEmpty()) return@Canvas

        val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(100f)
        val path = Path()
        val width = size.width
        val height = size.height
        val spacing = width / (dataPoints.size - 1)

        dataPoints.forEachIndexed { i, value ->
            val x = i * spacing
            val y = height - (value / maxVal * height * animateProgress.value)

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                // Cubic Bezier for "Smooth" curve
                val prevX = (i - 1) * spacing
                val prevY = height - (dataPoints[i-1] / maxVal * height * animateProgress.value)
                path.cubicTo(
                    (prevX + x) / 2f, prevY,
                    (prevX + x) / 2f, y,
                    x, y
                )
            }
        }

        // Draw Shadow
        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(color.copy(0.2f), Color.Transparent)))

        // Draw Line
        drawPath(path, color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
    }
}