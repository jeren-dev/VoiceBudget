package com.example.voicebudget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SetLimitsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SetLimitsScreen { finish() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetLimitsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { DBHelper(context) }

    var amount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val periods = listOf("Today", "Weekly", "Monthly", "Annual")
    var selectedPeriod by remember { mutableStateOf(periods[0]) }

    Scaffold(
        containerColor = Color(0xFF05050A),
        topBar = {
            TopAppBar(
                title = { Text("SET BUDGET LIMITS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Background Glow
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF6200EA).copy(0.1f), Color.Transparent))
            ))

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.TrackChanges, null, tint = Color(0xFFBB86FC), modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Define Your Boundaries", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Set maximum spending targets to stay on track", color = Color.Gray, fontSize = 14.sp)

                Spacer(Modifier.height(40.dp))

                // --- PERIOD SELECTOR ---
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White.copy(0.05f)),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFFBB86FC), Color(0xFF03DAC6))))
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, null, tint = Color(0xFF03DAC6))
                            Spacer(Modifier.width(12.dp))
                            Text(selectedPeriod, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF161622))
                    ) {
                        periods.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period, color = Color.White) },
                                onClick = {
                                    selectedPeriod = period
                                    expanded = false
                                    // Pre-load existing limit if any
                                    val current = db.getLimit(period)
                                    if(current > 0) amount = current.toString()
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // --- AMOUNT INPUT ---
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Limit Amount (₹)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFBB86FC),
                        unfocusedBorderColor = Color.White.copy(0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, null, tint = Color(0xFFBB86FC)) }
                )

                Spacer(Modifier.height(40.dp))

                // --- SAVE BUTTON ---
                Button(
                    onClick = {
                        val limitVal = amount.toIntOrNull()
                        if (limitVal != null) {
                            db.saveLimit(selectedPeriod, limitVal)
                            Toast.makeText(context, "Limit set for $selectedPeriod!", Toast.LENGTH_SHORT).show()
                            onBack()
                        } else {
                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("SAVE LIMIT", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}