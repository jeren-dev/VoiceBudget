package com.example.voicebudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

enum class ExpenseFilter { TODAY, WEEK, MONTH, YEAR }

class ViewExpenseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ViewExpenseScreen()
            }
        }
    }
}

@Composable
fun ViewExpenseScreen() {
    val context = LocalContext.current
    val db = remember { DBHelper(context) }

    var selectedFilter by remember { mutableStateOf(ExpenseFilter.TODAY) }
    var expenses by remember { mutableStateOf(listOf<ExpenseModel>()) }

    // Fetch data whenever filter changes
    LaunchedEffect(selectedFilter) {
        expenses = when (selectedFilter) {
            ExpenseFilter.TODAY -> db.getTodayExpenses()
            ExpenseFilter.WEEK -> db.getWeekExpenses()
            ExpenseFilter.MONTH -> db.getMonthExpenses()
            ExpenseFilter.YEAR -> db.getYearExpenses()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05050A))) {
        // Aesthetic Glow Background
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6200EA).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width, size.height * 0.2f),
                    radius = size.width
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "History",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "Analyze your previous spends",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // HORIZONTAL GLASS FILTERS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ExpenseFilter.values().forEach { filter ->
                    FilterChip(
                        label = filter.name.lowercase().replaceFirstChar { it.uppercase() },
                        isSelected = filter == selectedFilter,
                        onClick = { selectedFilter = filter }
                    )
                }
            }

            // EXPENSE LIST
            if (expenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records found 💸", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    items(expenses) { item ->
                        PremiumExpenseItem(item)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFBB86FC) else Color.White.copy(alpha = 0.05f))
            .border(
                1.dp,
                if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun PremiumExpenseItem(expense: ExpenseModel) {
    val formattedDate = remember(expense.datetime) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = parser.parse(expense.datetime)
            SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            expense.datetime
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category.uppercase(),
                    color = Color(0xFFBB86FC),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = expense.note.ifEmpty { "No description" },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formattedDate,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "₹${expense.amount}",
                color = if (expense.amount > 1000) Color(0xFFCF6679) else Color(0xFF03DAC6),
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}