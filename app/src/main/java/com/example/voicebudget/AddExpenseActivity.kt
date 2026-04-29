package com.example.voicebudget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

data class TempExpense(val amount: Int, val category: String, val note: String)

class AddExpenseActivity : ComponentActivity() {
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
                AddExpenseScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen() {
    val context = LocalContext.current
    val db = remember { DBHelper(context) }
    val activity = context as Activity
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("English") } // "English" or "Tamil"

    // Voice language codes
    val languageMap = mapOf(
        "English" to "en-IN",
        "Tamil" to "ta-IN"
    )

    // --- QR DATA FETCH LOGIC ---
    val fetchedAmount = activity.intent.getStringExtra("FETCHED_AMOUNT") ?: ""
    val fetchedCategory = activity.intent.getStringExtra("FETCHED_CATEGORY") ?: ""

    var amount by remember { mutableStateOf(fetchedAmount) }
    var category by remember { mutableStateOf(fetchedCategory) }
    var note by remember { mutableStateOf(if(fetchedAmount.isNotEmpty()) "Scanned via QR" else "") }
    var isExpenseMode by remember { mutableStateOf(true) }
    val toDoList = remember { mutableStateListOf<TempExpense>() }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var savedCount by remember { mutableStateOf(0) }

    // Toast if coming from Scanner
    LaunchedEffect(fetchedAmount) {
        if (fetchedAmount.isNotEmpty()) {
            Toast.makeText(context, "QR Data Loaded Successfully", Toast.LENGTH_SHORT).show()
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            val parsed = parseVoice(spokenText.lowercase(), selectedLanguage)
            amount = if (parsed.first != 0) parsed.first.toString() else amount
            category = parsed.second
            note = spokenText
            val languageHint = if (selectedLanguage == "Tamil") "தமிழில்" else "in English"
            Toast.makeText(context, "Voice recognized $languageHint: ${parsed.second} - ₹${parsed.first}", Toast.LENGTH_LONG).show()
        }
    }

    fun saveExpenses() {
        scope.launch {
            isLoading = true
            delay(500)

            if (isExpenseMode) {
                if (amount.isNotEmpty() && amount.toIntOrNull() != null && amount.toInt() > 0) {
                    db.insertExpense(
                        amount.toInt(),
                        category.ifEmpty { "Other" },
                        note.ifEmpty { "Added via VoiceBudget" }
                    )
                    savedCount = 1
                    showSuccessDialog = true
                    delay(1000)
                    activity.finish()
                } else {
                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    isLoading = false
                }
            } else if (toDoList.isNotEmpty()) {
                toDoList.forEach {
                    db.insertExpense(it.amount, it.category.ifEmpty { "Other" }, it.note.ifEmpty { "Batch entry" })
                }
                savedCount = toDoList.size
                showSuccessDialog = true
                delay(1000)
                activity.finish()
            } else {
                Toast.makeText(context, "Please add items to the list", Toast.LENGTH_SHORT).show()
                isLoading = false
            }

            isLoading = false
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            containerColor = Color(0xFF1E1E2A),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color(0xFF03DAC6).copy(0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF03DAC6), modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Success!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = {
                Text(
                    if (isExpenseMode) "Expense saved successfully!"
                    else "$savedCount expenses saved successfully!",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK", color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF05050A))) {
        // Gradient Glow Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6200EA).copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = size.width
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF03DAC6).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width, size.height),
                    radius = size.width
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isExpenseMode) "Add Expense" else "Batch Entry",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = if (isExpenseMode) "Record your spending" else "Add multiple expenses at once",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = Color(0xFFBB86FC).copy(0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isExpenseMode) Icons.Default.Receipt else Icons.Default.List,
                            contentDescription = null,
                            tint = Color(0xFFBB86FC),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // MODE TOGGLE
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeButton(
                        title = "Single",
                        icon = Icons.Default.AddCircle,
                        isSelected = isExpenseMode,
                        onClick = { isExpenseMode = true },
                        modifier = Modifier.weight(1f)
                    )
                    ModeButton(
                        title = "Batch",
                        icon = Icons.Default.FormatListBulleted,
                        isSelected = !isExpenseMode,
                        onClick = { isExpenseMode = false },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // INPUT CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Amount Field with Icon
                    PremiumTextFieldWithIcon(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Amount",
                        icon = Icons.Default.CurrencyRupee,
                        placeholder = "Enter amount"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Field with Icon
                    PremiumTextFieldWithIcon(
                        value = category,
                        onValueChange = { category = it },
                        label = "Category",
                        icon = Icons.Default.Category,
                        placeholder = "e.g., Food, Transport, etc."
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Note Field with Icon
                    PremiumTextFieldWithIcon(
                        value = note,
                        onValueChange = { note = it },
                        label = "Note (Optional)",
                        icon = Icons.Default.Edit,
                        placeholder = "Add a description",
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Language Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Voice Language:",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        // English Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedLanguage = "English" },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedLanguage == "English") Color(0xFFBB86FC) else Color.White.copy(0.05f),
                            border = BorderStroke(1.dp, if (selectedLanguage == "English") Color(0xFFBB86FC) else Color.White.copy(0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🇬🇧",
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "English",
                                    color = if (selectedLanguage == "English") Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedLanguage == "English") FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        // Tamil Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedLanguage = "Tamil" },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selectedLanguage == "Tamil") Color(0xFFBB86FC) else Color.White.copy(0.05f),
                            border = BorderStroke(1.dp, if (selectedLanguage == "Tamil") Color(0xFFBB86FC) else Color.White.copy(0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🇮🇳",
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "தமிழ்",
                                    color = if (selectedLanguage == "Tamil") Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedLanguage == "Tamil") FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Voice Input Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageMap[selectedLanguage] ?: "en-IN")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (selectedLanguage == "Tamil") "உங்கள் செலவை கூறுங்கள்..." else "Speak your expense...")
                                }
                                speechLauncher.launch(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC).copy(0.2f)),
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.dp, Color(0xFFBB86FC).copy(0.5f))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = Color(0xFFBB86FC), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (selectedLanguage == "Tamil") "குரல் உள்ளீடு" else "Voice Input",
                                color = Color(0xFFBB86FC),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BATCH MODE LIST
            if (!isExpenseMode) {
                // Add to List Button
                Button(
                    onClick = {
                        if (amount.isNotEmpty() && amount.toIntOrNull() != null && amount.toInt() > 0) {
                            toDoList.add(TempExpense(amount.toInt(), category.ifEmpty { "Other" }, note))
                            amount = ""
                            category = ""
                            note = ""
                            Toast.makeText(context, "Added to list", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter valid amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC6).copy(0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF03DAC6).copy(0.3f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color(0xFF03DAC6), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ADD TO BATCH LIST", color = Color(0xFF03DAC6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Batch List Header
                if (toDoList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Batch Items (${toDoList.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        TextButton(
                            onClick = { toDoList.clear() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCF6679))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All", fontSize = 12.sp, color = Color(0xFFCF6679))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Batch List Items
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(toDoList) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = CircleShape,
                                        color = getCategoryColors(item.category).copy(0.15f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                getCategoryIcons(item.category),
                                                contentDescription = null,
                                                tint = getCategoryColors(item.category),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            item.category,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        if (item.note.isNotEmpty()) {
                                            Text(
                                                item.note.take(30),
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "₹${item.amount}",
                                    color = Color(0xFF03DAC6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SAVE BUTTON
            Button(
                onClick = { saveExpenses() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                shape = RoundedCornerShape(18.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVING...", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Black, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isExpenseMode) "SAVE EXPENSE" else "SAVE ALL (${toDoList.size})",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModeButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFBB86FC) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) Color.Black else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                title,
                color = if (isSelected) Color.Black else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTextFieldWithIcon(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String = "",
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = Color(0xFFBB86FC), modifier = Modifier.size(20.dp))
        },
        placeholder = {
            if (placeholder.isNotEmpty()) {
                Text(placeholder, color = Color.Gray.copy(0.5f), fontSize = 13.sp)
            }
        },
        label = {
            Text(label, color = Color.Gray, fontSize = 12.sp)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFFBB86FC),
            unfocusedBorderColor = Color.White.copy(0.1f),
            focusedLabelColor = Color(0xFFBB86FC),
            unfocusedLabelColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFFBB86FC)
        ),
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp)
    )
}

fun getCategoryColors(category: String): Color {
    return when (category.lowercase()) {
        "food" -> Color(0xFF03DAC6)
        "transport", "travel" -> Color(0xFFBB86FC)
        "shopping" -> Color(0xFFCF6679)
        "entertainment" -> Color(0xFFFFB74D)
        "bills" -> Color(0xFF81C784)
        "health" -> Color(0xFF64B5F6)
        "education" -> Color(0xFFE57373)
        "grocery" -> Color(0xFF4DB6AC)
        else -> Color(0xFFBA68C8)
    }
}

fun getCategoryIcons(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "transport", "travel" -> Icons.Default.DirectionsCar
        "shopping" -> Icons.Default.ShoppingCart
        "entertainment" -> Icons.Default.MusicNote
        "bills" -> Icons.Default.Receipt
        "health" -> Icons.Default.Favorite
        "education" -> Icons.Default.School
        "grocery" -> Icons.Default.LocalGroceryStore
        else -> Icons.Default.Category
    }
}

// ==================== ENHANCED VOICE PARSER WITH BILINGUAL SUPPORT ====================
fun parseVoice(text: String, language: String): Pair<Int, String> {
    var amount = 0
    var category = ""

    // Find numbers (supports both Tamil and English number words)
    val numberPattern = "\\d+".toRegex()
    val match = numberPattern.find(text)
    match?.let { amount = it.value.toInt() }

    // If no digits found, check for number words based on language
    if (amount == 0) {
        val numberWords = if (language == "Tamil") {
            mapOf(
                "ஒன்று" to 1, "ஒண்ணு" to 1, "இரண்டு" to 2, "ரெண்டு" to 2, "மூன்று" to 3, "மூணு" to 3,
                "நான்கு" to 4, "நாலு" to 4, "ஐந்து" to 5, "அஞ்சு" to 5, "ஆறு" to 6, "ஏழு" to 7,
                "எட்டு" to 8, "ஒன்பது" to 9, "ஒம்பது" to 9, "பத்து" to 10, "பதினொன்று" to 11,
                "பன்னிரண்டு" to 12, "பதின்மூன்று" to 13, "பதினான்கு" to 14, "பதினைந்து" to 15,
                "பதினாறு" to 16, "பதினேழு" to 17, "பதினெட்டு" to 18, "பத்தொன்பது" to 19, "இருபது" to 20,
                "முப்பது" to 30, "நாற்பது" to 40, "ஐம்பது" to 50, "அறுபது" to 60, "எழுபது" to 70,
                "எண்பது" to 80, "தொண்ணூறு" to 90, "நூறு" to 100, "ஆயிரம்" to 1000
            )
        } else {
            mapOf(
                "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
                "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
                "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15,
                "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19, "twenty" to 20,
                "thirty" to 30, "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
                "eighty" to 80, "ninety" to 90, "hundred" to 100, "thousand" to 1000
            )
        }
        for ((word, value) in numberWords) {
            if (text.contains(word)) {
                amount = value
                break
            }
        }
    }

    // Category detection based on language
    if (language == "Tamil") {
        // ==================== TAMIL CATEGORY KEYWORDS ====================
        when {
            // Food (உணவு)
            text.contains("சாப்பாடு") || text.contains("உணவு") || text.contains("ஹோட்டல்") ||
                    text.contains("பிரியாணி") || text.contains("தேநீர்") || text.contains("காபி") ||
                    text.contains("டிபன்") || text.contains("இரவு") || text.contains("மதியம்") ||
                    text.contains("உணவகம்") || text.contains("கஃபே") || text.contains("டோசை") ||
                    text.contains("இட்லி") || text.contains("வடை") || text.contains("பொங்கல்") ||
                    text.contains("சப்பாத்தி") || text.contains("கறி") || text.contains("சாதம்") ||
                    text.contains("நூடுல்ஸ்") || text.contains("பீஸ்ஸா") || text.contains("பர்கர்") ->
                category = "Food"

            // Transport (போக்குவரத்து)
            text.contains("பஸ்") || text.contains("பேருந்து") || text.contains("ஆட்டோ") ||
                    text.contains("ரிக்ஷா") || text.contains("ரயில்") || text.contains("பெட்ரோல்") ||
                    text.contains("டீசல்") || text.contains("எரிபொருள்") || text.contains("பைக்") ||
                    text.contains("கார்") || text.contains("ஓலா") || text.contains("உபர்") ||
                    text.contains("டாக்ஸி") || text.contains("மெட்ரோ") || text.contains("விமானம்") ||
                    text.contains("கப்பல்") || text.contains("படகு") || text.contains("சைக்கிள்") ->
                category = "Transport"

            // Shopping (ஷாப்பிங்)
            text.contains("ஷாப்பிங்") || text.contains("ஆடை") || text.contains("சட்டை") ||
                    text.contains("பேன்ட்") || text.contains("ஜீன்ஸ்") || text.contains("புடவை") ||
                    text.contains("மால்") || text.contains("சலூன்") || text.contains("ஹேர்கட்") ||
                    text.contains("அழகு") || text.contains("ஸ்பா") || text.contains("காலணி") ||
                    text.contains("கைப்பை") || text.contains("நகை") || text.contains("தங்கம்") ->
                category = "Shopping"

            // Bills (கட்டணங்கள்)
            text.contains("ரீசார்ஜ்") || text.contains("கரண்ட்") || text.contains("மின்சாரம்") ||
                    text.contains("தண்ணீர்") || text.contains("வாடகை") || text.contains("கேஸ்") ||
                    text.contains("சிலிண்டர்") || text.contains("வைஃபை") || text.contains("இணையம்") ||
                    text.contains("மொபைல்") || text.contains("பில்") || text.contains("கடன்") ||
                    text.contains("கிரெடிட் கார்டு") || text.contains("காப்பீடு") || text.contains("சப்ஸ்கிரிப்ஷன்") ->
                category = "Bills"

            // Health (உடல்நலம்)
            text.contains("டாக்டர்") || text.contains("மருந்து") || text.contains("மாத்திரை") ||
                    text.contains("மருத்துவமனை") || text.contains("ஆரோக்கியம்") || text.contains("பார்மசி") ||
                    text.contains("மருத்துவ") || text.contains("பரிசோதனை") || text.contains("அறுவை சிகிச்சை") ||
                    text.contains("சிகிச்சை") || text.contains("தடுப்பூசி") || text.contains("ஊசி") ||
                    text.contains("ரத்த பரிசோதனை") || text.contains("எக்ஸ்ரே") || text.contains("ஸ்கேன்") ->
                category = "Health"

            // Entertainment (பொழுதுபோக்கு)
            text.contains("படம்") || text.contains("சினிமா") || text.contains("திரையரங்கு") ||
                    text.contains("நெட்ஃபிக்ஸ்") || text.contains("பிரைம்") || text.contains("ஹாட்ஸ்டார்") ||
                    text.contains("இசை") || text.contains("கச்சேரி") || text.contains("கேம்") ||
                    text.contains("விளையாட்டு") || text.contains("கிரிக்கெட்") || text.contains("பூங்கா") ||
                    text.contains("கடற்கரை") || text.contains("வாட்டர் பார்க்") || text.contains("மியூசியம்") ->
                category = "Entertainment"

            // Education (கல்வி)
            text.contains("பள்ளி") || text.contains("கல்லூரி") || text.contains("பல்கலைக்கழகம்") ||
                    text.contains("கல்வி") || text.contains("டியூஷன்") || text.contains("கோச்சிங்") ||
                    text.contains("வகுப்பு") || text.contains("புத்தகம்") || text.contains("நோட்டுப் புத்தகம்") ||
                    text.contains("பேனா") || text.contains("கட்டணம்") || text.contains("சேர்க்கை") ||
                    text.contains("தேர்வு") || text.contains("பதிவு") || text.contains("சான்றிதழ்") ->
                category = "Education"

            // Grocery (மளிகை)
            text.contains("மளிகை") || text.contains("காய்கறி") || text.contains("பழம்") ||
                    text.contains("பால்") || text.contains("தயிர்") || text.contains("வெண்ணெய்") ||
                    text.contains("நெய்") || text.contains("எண்ணெய்") || text.contains("அரிசி") ||
                    text.contains("கோதுமை") || text.contains("சர்க்கரை") || text.contains("உப்பு") ||
                    text.contains("மசாலா") || text.contains("பருப்பு") || text.contains("முட்டை") ||
                    text.contains("கோழி") || text.contains("மீன்") || text.contains("சோப்") ->
                category = "Grocery"
        }
    } else {
        // ==================== ENGLISH CATEGORY KEYWORDS ====================
        when {
            // Food & Dining
            text.contains("food") || text.contains("hotel") || text.contains("restaurant") ||
                    text.contains("biryani") || text.contains("tea") || text.contains("coffee") ||
                    text.contains("dinner") || text.contains("lunch") || text.contains("breakfast") ||
                    text.contains("meal") || text.contains("cafe") || text.contains("snacks") ||
                    text.contains("pizza") || text.contains("burger") || text.contains("noodles") ||
                    text.contains("rice") || text.contains("curry") || text.contains("chapathi") ||
                    text.contains("juice") || text.contains("ice cream") || text.contains("cake") ||
                    text.contains("zomato") || text.contains("swiggy") ->
                category = "Food"

            // Transport
            text.contains("bus") || text.contains("auto") || text.contains("rickshaw") ||
                    text.contains("train") || text.contains("petrol") || text.contains("diesel") ||
                    text.contains("fuel") || text.contains("bike") || text.contains("car") ||
                    text.contains("ola") || text.contains("uber") || text.contains("cab") ||
                    text.contains("taxi") || text.contains("metro") || text.contains("flight") ||
                    text.contains("bicycle") || text.contains("parking") || text.contains("toll") ->
                category = "Transport"

            // Shopping
            text.contains("shopping") || text.contains("dress") || text.contains("clothes") ||
                    text.contains("shirt") || text.contains("jeans") || text.contains("saree") ||
                    text.contains("mall") || text.contains("salon") || text.contains("haircut") ||
                    text.contains("beauty") || text.contains("spa") || text.contains("shoes") ||
                    text.contains("jewelry") || text.contains("gold") || text.contains("watch") ||
                    text.contains("amazon") || text.contains("flipkart") ->
                category = "Shopping"

            // Bills & Utilities
            text.contains("recharge") || text.contains("electricity") || text.contains("eb bill") ||
                    text.contains("water") || text.contains("rent") || text.contains("gas") ||
                    text.contains("cylinder") || text.contains("wifi") || text.contains("internet") ||
                    text.contains("mobile") || text.contains("bill") || text.contains("maintenance") ||
                    text.contains("emi") || text.contains("credit card") || text.contains("insurance") ||
                    text.contains("subscription") || text.contains("netflix") || text.contains("prime") ->
                category = "Bills"

            // Health
            text.contains("doctor") || text.contains("medicine") || text.contains("tablet") ||
                    text.contains("hospital") || text.contains("clinic") || text.contains("pharmacy") ||
                    text.contains("medical") || text.contains("checkup") || text.contains("treatment") ||
                    text.contains("vaccine") || text.contains("injection") || text.contains("blood test") ||
                    text.contains("xray") || text.contains("scan") || text.contains("dental") ||
                    text.contains("gym") || text.contains("fitness") ->
                category = "Health"

            // Entertainment
            text.contains("movie") || text.contains("cinema") || text.contains("theatre") ||
                    text.contains("netflix") || text.contains("prime video") || text.contains("hotstar") ||
                    text.contains("music") || text.contains("concert") || text.contains("game") ||
                    text.contains("sports") || text.contains("cricket") || text.contains("party") ||
                    text.contains("club") || text.contains("pub") || text.contains("bar") ->
                category = "Entertainment"

            // Education
            text.contains("school") || text.contains("college") || text.contains("university") ||
                    text.contains("education") || text.contains("tuition") || text.contains("coaching") ||
                    text.contains("classes") || text.contains("books") || text.contains("stationery") ||
                    text.contains("fees") || text.contains("admission") || text.contains("exam") ||
                    text.contains("course") || text.contains("training") || text.contains("workshop") ->
                category = "Education"

            // Grocery
            text.contains("grocery") || text.contains("vegetables") || text.contains("fruits") ||
                    text.contains("milk") || text.contains("curd") || text.contains("butter") ||
                    text.contains("oil") || text.contains("rice") || text.contains("wheat") ||
                    text.contains("sugar") || text.contains("salt") || text.contains("spices") ||
                    text.contains("eggs") || text.contains("chicken") || text.contains("fish") ||
                    text.contains("soap") || text.contains("detergent") ->
                category = "Grocery"
        }
    }

    // Default fallback: use the longest word that isn't a number
    if (category.isEmpty()) {
        val words = text.split(" ").filter { it.toIntOrNull() == null && it.length > 3 }
        category = words.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Other"
    }

    return Pair(amount, category)
}