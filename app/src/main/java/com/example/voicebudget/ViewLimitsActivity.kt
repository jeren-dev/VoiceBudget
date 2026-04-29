package com.example.voicebudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ViewLimitsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ViewLimitsScreen { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewLimitsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { DBHelper(context) }

    // Fetching data for comparison
    val stats = remember {
        listOf(
            LimitData("Today", db.getTodayTotal(), db.getLimit("Today")),
            LimitData("Weekly", db.getWeekTotal(), db.getLimit("Weekly")),
            LimitData("Monthly", db.getMonthTotal(), db.getLimit("Monthly")),
            LimitData("Annual", db.getYearTotal(), db.getLimit("Annual"))
        )
    }

    Scaffold(
        containerColor = Color(0xFF05050A),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("BUDGET ANALYSIS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text("Limit vs Reality", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("Monitoring your financial boundaries", color = Color.Gray, fontSize = 14.sp)

            Spacer(Modifier.height(24.dp))

            stats.forEach { data ->
                BudgetComparisonCard(data)
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(30.dp))
        }
    }
}

data class LimitData(val label: String, val spent: Int, val limit: Int)

@Composable
fun BudgetComparisonCard(data: LimitData) {
    val progress = if (data.limit > 0) (data.spent.toFloat() / data.limit.toFloat()).coerceIn(0f, 1.2f) else 0f
    val isOverLimit = data.limit in 1..data.spent
    val color = when {
        data.limit == 0 -> Color.Gray
        progress > 0.9f -> Color(0xFFCF6679) // Red alert
        else -> Color(0xFF03DAC6) // Healthy Teal
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(data.label.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                if (isOverLimit) {
                    Icon(Icons.Default.Warning, "Over limit", tint = Color(0xFFCF6679), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Custom Progress Bar
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(0.05f))) {
                Box(modifier = Modifier
                    .fillMaxWidth(progress.coerceAtMost(1f))
                    .fillMaxHeight()
                    .background(Brush.linearGradient(listOf(color, color.copy(0.6f))))
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("SPENT", color = Color.Gray, fontSize = 10.sp)
                    Text("₹${data.spent}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("LIMIT", color = Color.Gray, fontSize = 10.sp)
                    Text(if (data.limit > 0) "₹${data.limit}" else "NOT SET",
                        color = if (data.limit > 0) Color(0xFFBB86FC) else Color.DarkGray,
                        fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }

            if (data.limit > 0) {
                val remaining = data.limit - data.spent
                Text(
                    text = if (remaining >= 0) "₹$remaining remaining" else "Overspent by ₹${-remaining}",
                    color = color.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}