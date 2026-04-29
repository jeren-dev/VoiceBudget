package com.example.voicebudget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFBB86FC),
                    secondary = Color(0xFF03DAC6),
                    background = Color(0xFF05050A),
                    surface = Color(0xFF12121A)
                )
            ) {
                DashboardScreen()
            }
        }
    }
}

data class CategoryAmount(val category: String, val amount: Int, val color: Color, val percentage: Float = 0f)
data class TradePoint(val date: Date, val amount: Float)
data class TransactionItem(val category: String, val amount: Int, val date: String)

// Extended color palette for dynamic categories
private val dynamicCategoryColors = listOf(
    Color(0xFF03DAC6), // Teal
    Color(0xFFBB86FC), // Purple
    Color(0xFFCF6679), // Red
    Color(0xFFFFB74D), // Orange
    Color(0xFF81C784), // Green
    Color(0xFF64B5F6), // Blue
    Color(0xFFE57373), // Light Red
    Color(0xFFBA68C8), // Pink
    Color(0xFF4DB6AC), // Light Teal
    Color(0xFF9575CD), // Light Purple
    Color(0xFF7986CB), // Indigo
    Color(0xFF4FC3F7), // Light Blue
    Color(0xFFAED581), // Light Green
    Color(0x00FF8A65), // Coral
    Color(0xFF90A4AE)  // Blue Grey
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val db = remember { DBHelper(context) }
    val scope = rememberCoroutineScope()
    val activity = (context as? Activity)

    var showExitDialog by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedChartIndex by remember { mutableIntStateOf(0) }
    val chartTitles = listOf("Line Chart", "Pie Chart", "Donut Chart", "Trade Graph")

    var today by remember { mutableIntStateOf(0) }
    var week by remember { mutableIntStateOf(0) }
    var month by remember { mutableIntStateOf(0) }
    var year by remember { mutableIntStateOf(0) }
    var todayLimit by remember { mutableIntStateOf(0) }

    var allTimeCategoryData by remember { mutableStateOf<List<CategoryAmount>>(emptyList()) }
    var tradeData by remember { mutableStateOf<List<TradePoint>>(emptyList()) }
    var recentTransactions by remember { mutableStateOf<List<TransactionItem>>(emptyList()) }

    var selectedCategory by remember { mutableStateOf<CategoryAmount?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val pulseAmount by produceState(initialValue = 0f) {
        while (true) {
            delay(2000)
            value = (-12..12).random().toFloat()
        }
    }

    val refreshRotation by animateFloatAsState(
        targetValue = if (isRefreshing) 360f else 0f,
        animationSpec = tween(800)
    )

    fun generateTradeData(db: DBHelper): List<TradePoint> {
        val points = mutableListOf<TradePoint>()
        val calendar = Calendar.getInstance()
        for (i in 0..29) {
            val date = calendar.time
            val dayTotal = db.getTotalForDate(date)
            points.add(TradePoint(date, dayTotal.toFloat()))
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        return points.reversed()
    }

    // Replace the getAllTimeCategoryBreakdown function with this version
    fun getAllTimeCategoryBreakdown(db: DBHelper): List<CategoryAmount> {
        val allCategories = db.getAllUniqueCategories()
        val categoryAmounts = mutableListOf<Pair<String, Int>>()

        for (cat in allCategories) {
            val total = db.getCategoryTotalAllTime(cat)
            if (total > 0) {
                categoryAmounts.add(Pair(cat, total))
            }
        }

        val sortedCategories = categoryAmounts.sortedByDescending { it.second }
        val total = sortedCategories.sumOf { it.second }

        return sortedCategories.map { (cat, amt) ->
            val percentage = if (total > 0) (amt.toFloat() / total * 100) else 0f
            // Use hash-based color for consistent color regardless of position
            val colorIndex = abs(cat.hashCode()) % dynamicCategoryColors.size
            CategoryAmount(
                cat,
                amt,
                dynamicCategoryColors[colorIndex],  // Color based on category name, not position
                percentage
            )
        }
    }

    fun getRecentTransactions(db: DBHelper, limit: Int): List<TransactionItem> {
        return db.getRecentTransactions(limit).map {
            TransactionItem(it.category, it.amount, it.date)
        }
    }

    fun loadData() {
        isRefreshing = true
        today = db.getTodayTotal()
        week = db.getWeekTotal()
        month = db.getMonthTotal()
        year = db.getYearTotal()
        todayLimit = db.getLimit("Today")
        allTimeCategoryData = getAllTimeCategoryBreakdown(db)
        tradeData = generateTradeData(db)
        recentTransactions = getRecentTransactions(db, 5)
        scope.launch {
            delay(800)
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    val scanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val amount = result.data?.getStringExtra("SCAN_RESULT") ?: "0"
            Toast.makeText(context, "Scanned: ₹$amount", Toast.LENGTH_LONG).show()
            loadData()
        }
    }

    BackHandler(enabled = true) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = Color(0xFF1E1E2A),
            title = { Text("Exit Application?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Do you want to close VoiceBudget ?", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { activity?.finish() }) {
                    Text("YES, EXIT", color = Color(0xFFCF6679), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("STAY", color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showCategoryDialog && selectedCategory != null) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            containerColor = Color(0xFF1E1E2A),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(selectedCategory!!.color.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getCategoryIcon(selectedCategory!!.category),
                            contentDescription = null,
                            tint = selectedCategory!!.color,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        selectedCategory!!.category.replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column {
                    Text("Amount: ₹${selectedCategory!!.amount}", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Percentage: ${selectedCategory!!.percentage.toInt()}%", color = Color(0xFF03DAC6), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = selectedCategory!!.percentage / 100f,
                        color = selectedCategory!!.color,
                        trackColor = Color.White.copy(0.1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Close", color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0A0A12),
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
                DrawerContent()
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            containerColor = Color(0xFF05050A),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "VOICE BUDGET ",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFBB86FC), modifier = Modifier.rotate(refreshRotation))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFF6200EA).copy(0.15f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.2f),
                            radius = size.width
                        )
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column {
                            Text(
                                "Financial Pulse",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF03DAC6))
                                        .animateContentSize()
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Live Real-time Tracking",
                                    fontSize = 11.sp,
                                    color = Color(0xFF03DAC6),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuickActionBtn(Icons.Default.Mic, "Voice", Color(0xFFBB86FC), Modifier.weight(1f)) {
                                context.startActivity(Intent(context, AddExpenseActivity::class.java))
                            }
                            QuickActionBtn(Icons.Default.QrCodeScanner, "Scan", Color(0xFF03DAC6), Modifier.weight(1f)) {
                                val intent = Intent(context, QRScannerActivity::class.java)
                                scanLauncher.launch(intent)
                            }
                            QuickActionBtn(Icons.Default.Calculate, "AI", Color(0xFFCF6679), Modifier.weight(1f)) {
                                context.startActivity(Intent(context, AnalyticsActivity::class.java))
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCard("TODAY", today, Color(0xFF03DAC6), Icons.Default.Today, Modifier.weight(1f))
                                StatCard("WEEKLY", week, Color(0xFFBB86FC), Icons.Default.Weekend, Modifier.weight(1f))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatCard("MONTHLY", month, Color(0xFFCF6679), Icons.Default.CalendarMonth, Modifier.weight(1f))
                                StatCard("YEARLY", year, Color(0xFFB0BEC5), Icons.Default.DateRange, Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        SmartAdvisorSection(today, todayLimit)
                    }

                    item {
                        Column {
                            ChartSelectorTabs(selectedChartIndex, chartTitles) { index ->
                                selectedChartIndex = index
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            GlassCard(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                                when (selectedChartIndex) {
                                    0 -> AdvancedLineChart(
                                        dataPoints = listOf(today.toFloat() + pulseAmount, (week / 7f), (month / 30f), (year / 365f)),
                                        labels = listOf("Today", "Weekly", "Monthly", "Yearly")
                                    )
                                    1 -> PieChart(allTimeCategoryData, onCategoryClick = { category ->
                                        selectedCategory = category
                                        showCategoryDialog = true
                                    })
                                    2 -> DonutChart(allTimeCategoryData, onCategoryClick = { category ->
                                        selectedCategory = category
                                        showCategoryDialog = true
                                    })
                                    3 -> TradeGraph(tradeData)
                                }
                            }
                        }
                    }

                    if (recentTransactions.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    "Recent Activity",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        recentTransactions.forEachIndexed { index, transaction ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(CircleShape)
                                                            .background(getDynamicCategoryColor(transaction.category, dynamicCategoryColors).copy(0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            getCategoryIcon(transaction.category),
                                                            contentDescription = null,
                                                            tint = getDynamicCategoryColor(transaction.category, dynamicCategoryColors),
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            transaction.category.replaceFirstChar {
                                                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                                            },
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Medium,
                                                            fontSize = 13.sp
                                                        )
                                                        Text(
                                                            transaction.date,
                                                            color = Color.Gray,
                                                            fontSize = 9.sp
                                                        )
                                                    }
                                                }
                                                Text(
                                                    "₹${transaction.amount}",
                                                    color = Color(0xFFCF6679),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            if (index < recentTransactions.size - 1) {
                                                HorizontalDivider(color = Color.White.copy(0.08f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SpendingInsightsMini(today, week, month, todayLimit)
                    }

                    item {
                        AboutJerrySection()
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

// Helper function to get dynamic category color
fun getDynamicCategoryColor(category: String, colorList: List<Color>): Color {
    val index = category.hashCode().mod(colorList.size)
    return colorList[index]
}

// ─────────────────────────────────────────────────────────────────────────────
// PIE CHART — shows ALL-TIME expense breakdown by category (DYNAMIC)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PieChart(data: List<CategoryAmount>, onCategoryClick: (CategoryAmount) -> Unit) {
    val total = if (data.isNotEmpty()) data.sumOf { it.amount } else 0

    if (data.isEmpty() || total == 0) {
        EmptyChartMessage()
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(data) { animatedProgress.animateTo(1f, tween(1000)) }

    val sweepAngles = remember(data, total) {
        data.map { (it.amount.toFloat() / total) * 360f }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy

                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0f) angle += 360f

                                var accumulated = 0f
                                var hit: CategoryAmount? = null
                                data.forEachIndexed { i, cat ->
                                    val sweep = sweepAngles[i] * animatedProgress.value
                                    if (angle >= accumulated && angle < accumulated + sweep) {
                                        hit = cat
                                    }
                                    accumulated += sweep
                                }
                                hit?.let { onCategoryClick(it) }
                            }
                        )
                    }
            ) {
                var startAngle = -90f
                data.forEachIndexed { index, item ->
                    val sweepAngle = sweepAngles[index] * animatedProgress.value
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = androidx.compose.ui.geometry.Size(size.width, size.height)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Total Expenses", color = Color.Gray, fontSize = 10.sp)
                Text(
                    text = "₹${String.format("%,d", total)}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (data.size > 5) 120.dp else 100.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            items(data) { item ->
                val avgAmount = total / data.size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .pointerInput(item) {
                            detectTapGestures(
                                onTap = { onCategoryClick(item) },
                                onLongPress = { onCategoryClick(item) }
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            item.category.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            },
                            color = Color.White.copy(0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "₹${item.amount}",
                        color = if (item.amount > avgAmount) Color(0xFF03DAC6) else Color(0xFFCF6679),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        " (${item.percentage.toInt()}%)",
                        color = item.color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DONUT CHART — shows ALL-TIME expense breakdown by category (DYNAMIC)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DonutChart(data: List<CategoryAmount>, onCategoryClick: (CategoryAmount) -> Unit) {
    val total = if (data.isNotEmpty()) data.sumOf { it.amount } else 0

    if (data.isEmpty() || total == 0) {
        EmptyChartMessage()
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(data) { animatedProgress.animateTo(1f, tween(1000)) }

    val sweepAngles = remember(data, total) {
        data.map { (it.amount.toFloat() / total) * 360f }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy

                                val dist = sqrt(dx * dx + dy * dy)
                                val outerR = minOf(size.width, size.height) / 2f
                                val strokeWidthPx = outerR * 0.24f
                                val innerR = outerR - strokeWidthPx

                                if (dist < innerR || dist > outerR) return@detectTapGestures

                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0f) angle += 360f

                                var accumulated = 0f
                                var hit: CategoryAmount? = null
                                data.forEachIndexed { i, cat ->
                                    val sweep = sweepAngles[i] * animatedProgress.value
                                    if (angle >= accumulated && angle < accumulated + sweep) {
                                        hit = cat
                                    }
                                    accumulated += sweep
                                }
                                hit?.let { onCategoryClick(it) }
                            }
                        )
                    }
            ) {
                var startAngle = -90f
                val strokeWidth = size.width * 0.12f
                data.forEachIndexed { index, item ->
                    val sweepAngle = sweepAngles[index] * animatedProgress.value
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        size = androidx.compose.ui.geometry.Size(size.width, size.height),
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Total Expenses", color = Color.Gray, fontSize = 10.sp)
                Text(
                    text = "₹${String.format("%,d", total)}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (data.size > 5) 120.dp else 100.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            items(data) { item ->
                val avgAmount = total / data.size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .pointerInput(item) {
                            detectTapGestures(
                                onTap = { onCategoryClick(item) },
                                onLongPress = { onCategoryClick(item) }
                            )
                        },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            item.category.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            },
                            color = Color.White.copy(0.8f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "₹${item.amount}",
                        color = if (item.amount > avgAmount) Color(0xFF03DAC6) else Color(0xFFCF6679),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        " (${item.percentage.toInt()}%)",
                        color = item.color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TRADE GRAPH — long press anywhere → show latest point details
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TradeGraph(data: List<TradePoint>) {
    if (data.isEmpty()) {
        EmptyChartMessage()
        return
    }
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(data) { animatedProgress.animateTo(1f, tween(1500)) }

    var selectedPoint by remember { mutableStateOf<TradePoint?>(null) }
    var showAmountDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF03DAC6)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bullish", color = Color(0xFF03DAC6), fontSize = 9.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFCF6679)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bearish", color = Color(0xFFCF6679), fontSize = 9.sp)
            }
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .pointerInput(data) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                val paddingLeft = 40f
                                val paddingRight = 15f
                                val chartWidth = size.width - paddingLeft - paddingRight
                                val spacing = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

                                var minDist = Float.MAX_VALUE
                                var nearest: TradePoint? = null
                                data.forEachIndexed { i, point ->
                                    val x = paddingLeft + i * spacing
                                    val dist = abs(offset.x - x)
                                    if (dist < minDist) {
                                        minDist = dist
                                        nearest = point
                                    }
                                }
                                nearest?.let {
                                    selectedPoint = it
                                    showAmountDialog = true
                                }
                            }
                        )
                    }
            ) {
                val maxAmount = data.maxOfOrNull { it.amount }?.coerceAtLeast(100f) ?: 100f
                val minAmount = data.minOfOrNull { it.amount } ?: 0f
                val range = if (maxAmount - minAmount > 0) maxAmount - minAmount else 100f
                val paddingLeft = 40f
                val paddingBottom = 25f
                val paddingRight = 15f
                val chartWidth = size.width - paddingLeft - paddingRight
                val chartHeight = size.height - paddingBottom - 15f
                val spacing = if (data.size > 1) chartWidth / (data.size - 1) else chartWidth

                for (i in 0..4) {
                    val y = paddingBottom + (i * chartHeight / 4)
                    drawLine(
                        color = Color.White.copy(0.05f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width - paddingRight, y),
                        strokeWidth = 1f
                    )
                    val amountLabel = (maxAmount - (i * (maxAmount - minAmount) / 4)).toInt()
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 20f
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                        drawText("₹$amountLabel", paddingLeft - 8, y + 4, paint)
                    }
                }

                data.forEachIndexed { i, point ->
                    val x = paddingLeft + (i * spacing)
                    val normalizedHeight = ((point.amount - minAmount) / range) * chartHeight * animatedProgress.value
                    val y = size.height - paddingBottom - normalizedHeight

                    val isUp = i > 0 && point.amount > data[i - 1].amount
                    val barColor = if (isUp) Color(0xFF03DAC6) else Color(0xFFCF6679)

                    drawRect(
                        color = barColor,
                        topLeft = Offset(x - 4f, y),
                        size = androidx.compose.ui.geometry.Size(8f, normalizedHeight.coerceAtLeast(2f))
                    )

                    drawLine(
                        color = barColor,
                        start = Offset(x, if (isUp) y - 3f else y + normalizedHeight + 3f),
                        end = Offset(x, if (isUp) y + normalizedHeight + 3f else y - 3f),
                        strokeWidth = 1.5f
                    )
                }

                val path = Path().apply {
                    data.forEachIndexed { i, point ->
                        val x = paddingLeft + (i * spacing)
                        val normalizedHeight = ((point.amount - minAmount) / range) * chartHeight * animatedProgress.value
                        val y = size.height - paddingBottom - normalizedHeight
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }
                drawPath(path, Color(0xFFBB86FC), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
    }

    if (showAmountDialog && selectedPoint != null) {
        AlertDialog(
            onDismissRequest = { showAmountDialog = false },
            containerColor = Color(0xFF1E1E2A),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFFBB86FC).copy(0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Trade Details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column {
                    Text("Amount: ₹${selectedPoint!!.amount.toInt()}", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    Text("Date: ${sdf.format(selectedPoint!!.date)}", color = Color.Gray, fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAmountDialog = false }) {
                    Text("Close", color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper Functions
// ─────────────────────────────────────────────────────────────────────────────

fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    val lowerCategory = category.lowercase(Locale.getDefault())
    return when {
        lowerCategory.contains("food") || lowerCategory.contains("saapadu") || lowerCategory.contains("restaurant") -> Icons.Default.Restaurant
        lowerCategory.contains("transport") || lowerCategory.contains("travel") || lowerCategory.contains("car") -> Icons.Default.DirectionsCar
        lowerCategory.contains("shop") || lowerCategory.contains("mall") -> Icons.Default.ShoppingCart
        lowerCategory.contains("entertain") || lowerCategory.contains("movie") || lowerCategory.contains("music") -> Icons.Default.MusicNote
        lowerCategory.contains("bill") || lowerCategory.contains("utility") || lowerCategory.contains("rent") -> Icons.Default.Receipt
        lowerCategory.contains("health") || lowerCategory.contains("medical") || lowerCategory.contains("hospital") -> Icons.Default.Favorite
        lowerCategory.contains("education") || lowerCategory.contains("school") || lowerCategory.contains("college") -> Icons.Default.School
        else -> Icons.Default.Category
    }
}

@Composable
fun ChartSelectorTabs(selectedIndex: Int, titles: List<String>, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        titles.forEachIndexed { index, title ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(index) },
                shape = RoundedCornerShape(10.dp),
                color = if (selectedIndex == index) Color(0xFFBB86FC).copy(0.15f) else Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (selectedIndex == index) Color(0xFFBB86FC).copy(0.5f) else Color.White.copy(0.1f)
                )
            ) {
                Text(
                    title,
                    modifier = Modifier.padding(vertical = 6.dp),
                    textAlign = TextAlign.Center,
                    color = if (selectedIndex == index) Color(0xFFBB86FC) else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun EmptyChartMessage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.BarChart, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text("No data available", color = Color.Gray, fontSize = 12.sp)
            Text("Add expenses to see charts", color = Color.Gray.copy(0.6f), fontSize = 10.sp)
        }
    }
}

@Composable
fun SpendingInsightsMini(today: Int, week: Int, month: Int, limit: Int) {
    val avgDaily = if (month > 0) month / 30 else 0
    val insight = when {
        today > limit && limit > 0 -> "You've exceeded your daily limit of ₹$limit"
        today > avgDaily * 1.5 && avgDaily > 0 -> "Today's spending is 50% above average"
        week > month / 4 * 1.2 && month > 0 -> "Weekly spending is above monthly average"
        else -> "You're on track with your budget"
    }

    val icon = when {
        today > limit && limit > 0 -> Icons.Default.Warning
        today > avgDaily * 1.5 && avgDaily > 0 -> Icons.Default.TrendingUp
        week > month / 4 * 1.2 && month > 0 -> Icons.Default.ShowChart
        else -> Icons.Default.CheckCircle
    }

    val iconColor = when {
        today > limit && limit > 0 -> Color(0xFFCF6679)
        today > avgDaily * 1.5 && avgDaily > 0 -> Color(0xFFFFB74D)
        week > month / 4 * 1.2 && month > 0 -> Color(0xFFBB86FC)
        else -> Color(0xFF03DAC6)
    }

    Column {
        Text("Smart Insight", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = iconColor.copy(0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = "Insight", tint = iconColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(insight, color = Color.LightGray, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AboutJerrySection() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(modifier = Modifier.size(60.dp), shape = CircleShape, color = Color(0xFFBB86FC).copy(0.15f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Developer", tint = Color(0xFFBB86FC), modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Voice Budget Pro Edition", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("Version 2.0.0 ", color = Color(0xFF03DAC6), fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FeaturePoint(Icons.Default.Mic, "Voice Command Integration")
                FeaturePoint(Icons.Default.QrCodeScanner, "QR Code Scanner")
                FeaturePoint(Icons.Default.AutoGraph, "AI-Powered Analytics")
                FeaturePoint(Icons.Default.ShowChart, "Real-time Spending Charts")
                FeaturePoint(Icons.Default.NotificationImportant, "Smart Budget Alerts")
                FeaturePoint(Icons.Default.Lock, "Secure Local Storage")
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(0.1f), modifier = Modifier.width(60.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("Designed and Developed by Jerry", textAlign = TextAlign.Center, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("© 2026 JERRY INNOVATIONS", color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun FeaturePoint(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.White.copy(0.9f), fontSize = 11.sp)
    }
}

@Composable
fun DrawerContent() {
    val context = LocalContext.current
    val menuItems = listOf(
        Triple("Dashboard", Icons.Default.Dashboard, DashboardActivity::class.java),
        Triple("Add Expense", Icons.Default.AddCircle, AddExpenseActivity::class.java),
        Triple("View History", Icons.Default.History, ViewExpenseActivity::class.java),
        Triple("Set Budget Limits", Icons.Default.TrackChanges, SetLimitsActivity::class.java),
        Triple("Limit Overview", Icons.Default.Visibility, ViewLimitsActivity::class.java),
        Triple("AI Analytics", Icons.Default.AutoGraph, AnalyticsActivity::class.java)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
            .padding(20.dp)
    ) {
        Text("VOICE BUDGET", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(40.dp))
        menuItems.forEach { (title, icon, activity) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.startActivity(Intent(context, activity)) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(32.dp), shape = RoundedCornerShape(8.dp), color = Color(0xFFBB86FC).copy(0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = title, tint = Color(0xFFBB86FC), modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun QuickActionBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.05f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(0.3f))
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, borderColor: Color = Color.White.copy(0.1f), content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(0.03f),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 6.dp,
        tonalElevation = 3.dp
    ) {
        content()
    }
}

@Composable
fun AdvancedLineChart(dataPoints: List<Float>, labels: List<String>) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) { animatedProgress.animateTo(1f, tween(1500)) }

    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        val maxAmount = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(500f)
        val paddingLeft = 40f
        val paddingBottom = 30f
        val chartWidth = size.width - paddingLeft - 15f
        val chartHeight = size.height - paddingBottom - 15f
        val spacing = if (dataPoints.size > 1) chartWidth / (dataPoints.size - 1) else chartWidth

        for (i in 0..4) {
            val y = 15f + (i * chartHeight / 4)
            drawLine(color = Color.White.copy(0.05f), start = Offset(paddingLeft, y), end = Offset(size.width - 15f, y), strokeWidth = 1f)
            val amountLabel = (maxAmount - (i * maxAmount / 4)).toInt()
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                drawText("₹$amountLabel", paddingLeft - 8, y + 4, paint)
            }
        }

        dataPoints.forEachIndexed { i, value ->
            val x = paddingLeft + (i * spacing)
            val y = size.height - paddingBottom - (value / maxAmount * chartHeight * animatedProgress.value)
            drawCircle(Color(0xFF03DAC6), 4.dp.toPx(), Offset(x, y))
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText("₹${value.toInt()}", x, y - 12, paint)
            }
        }

        val path = Path().apply {
            dataPoints.forEachIndexed { i, value ->
                val x = paddingLeft + (i * spacing)
                val y = size.height - paddingBottom - (value / maxAmount * chartHeight * animatedProgress.value)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(path, Color(0xFFBB86FC), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))

        val fillPath = Path().apply {
            dataPoints.forEachIndexed { i, value ->
                val x = paddingLeft + (i * spacing)
                val y = size.height - paddingBottom - (value / maxAmount * chartHeight * animatedProgress.value)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            lineTo(size.width - 15f, size.height - paddingBottom)
            lineTo(paddingLeft, size.height - paddingBottom)
            close()
        }
        drawPath(fillPath, Color(0xFFBB86FC).copy(0.1f))

        dataPoints.forEachIndexed { i, _ ->
            val x = paddingLeft + (i * spacing)
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 20f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText(labels[i], x, size.height - 8, paint)
            }
        }
    }
}

@Composable
fun SmartAdvisorSection(spent: Int, limit: Int) {
    val isOver = if (limit > 0) spent > limit else false
    val percentUsed = if (limit > 0) ((spent.toFloat() / limit) * 100).toInt().coerceIn(0, 100) else 0
    val overAmount = if (isOver) spent - limit else 0

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isOver) Color(0xFFCF6679).copy(0.5f) else if (limit > 0) Color(0xFF03DAC6).copy(0.5f) else Color.White.copy(0.1f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = if (isOver) Color(0xFFCF6679).copy(0.15f) else if (limit > 0) Color(0xFF03DAC6).copy(0.15f) else Color.Gray.copy(0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isOver) Icons.Default.Warning else if (limit > 0) Icons.Default.AutoGraph else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isOver) Color(0xFFCF6679) else if (limit > 0) Color(0xFF03DAC6) else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (isOver) "Budget Alert! Over by ₹$overAmount"
                    else if (limit > 0) "Budget Optimized"
                    else "No Budget Set",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            if (limit > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Daily Spending", color = Color.Gray, fontSize = 11.sp)
                    Text("₹$spent / ₹$limit", color = if (isOver) Color(0xFFCF6679) else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = (spent.toFloat() / limit).coerceIn(0f, 1f),
                    color = if (isOver) Color(0xFFCF6679) else Color(0xFF03DAC6),
                    trackColor = Color.White.copy(0.1f),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (isOver) "You've exceeded your daily limit by ₹$overAmount"
                    else if (percentUsed > 80) "You're close to your daily limit ($percentUsed% used)"
                    else "Great job! You've used $percentUsed% of your daily budget",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Set a daily budget limit to track your spending effectively", color = Color.LightGray, fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: Int, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    GlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "₹${String.format("%,d", value)}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = color.copy(0.15f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}