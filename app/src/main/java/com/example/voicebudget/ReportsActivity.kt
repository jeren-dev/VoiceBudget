package com.example.voicebudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ReportFilter { TODAY, WEEK, MONTH, YEAR }

class ReportsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ReportsScreen()
            }
        }
    }
}

@Composable
fun ReportsScreen() {
    val context = LocalContext.current
    val db = remember { DBHelper(context) }

    var selectedFilter by remember { mutableStateOf(ReportFilter.TODAY) }
    var totalAmount by remember { mutableStateOf(0) }

    LaunchedEffect(selectedFilter) {
        totalAmount = when (selectedFilter) {
            ReportFilter.TODAY -> db.getTodayTotal()
            ReportFilter.WEEK -> db.getWeekTotal()
            ReportFilter.MONTH -> db.getMonthTotal()
            ReportFilter.YEAR -> db.getYearTotal()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05050A))) {
        // Decorative Background Glow
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF03DAC6).copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(size.width, size.height),
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

            Text(
                text = "Financial Summary",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Insights into your spending habits",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // PREMIUM TAB SWITCHER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReportFilter.values().forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFFBB86FC) else Color.Transparent)
                            .clickable { selectedFilter = filter }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.name.take(1) + filter.name.lowercase().drop(1),
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // MAIN REPORT CARD (Glowing Bento Box)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(25.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFF03DAC6))
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(0.08f), Color.White.copy(0.02f))
                        )
                    )
                    .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(32.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL EXPENDITURE",
                        color = Color.Gray,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "₹$totalAmount",
                        color = Color(0xFF03DAC6),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Period: ${selectedFilter.name.lowercase()}",
                        color = Color.White.copy(0.5f),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // STATS BREAKDOWN SECTION
            Text(
                text = "Key Metrics",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(
                    label = "Avg/Day",
                    value = "₹${if(totalAmount > 0) totalAmount/30 else 0}",
                    color = Color(0xFFBB86FC),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "Status",
                    value = if(totalAmount > 5000) "High" else "Stable",
                    color = if(totalAmount > 5000) Color(0xFFCF6679) else Color(0xFF03DAC6),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}