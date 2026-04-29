package com.example.voicebudget

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

class SplashScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIX: Use WindowCompat to set up Edge-to-Edge safely
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Hiding System Bars safely using a post-attach strategy to avoid NullPointerException
        window.decorView.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
        }

        setContent {
            MaterialTheme {
                SplashScreenContent()
            }
        }
    }
}

@Composable
fun SplashScreenContent() {
    val context = LocalContext.current

    // Smooth Breathing Animation for the Logo (UX improvement)
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    // Navigation: 3 Seconds Delay to Dashboard
    LaunchedEffect(Unit) {
        delay(3000)
        context.startActivity(Intent(context, DashboardActivity::class.java))
        (context as? Activity)?.finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05050A)), // True black for OLED premium feel
        contentAlignment = Alignment.Center
    ) {
        // Mesh Gradient Background (Matching Dashboard Theme)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6200EA).copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = size.width
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF03DAC6).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                    radius = size.width
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Glassmorphism Logo Box
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scale)
                    .shadow(40.dp, RoundedCornerShape(35.dp), ambientColor = Color(0xFFBB86FC).copy(0.4f))
                    .clip(RoundedCornerShape(35.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(0.1f), Color.White.copy(0.02f))
                        )
                    )
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(35.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💰",
                    fontSize = 65.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(44.dp))

            // Text Branding
            Text(
                text = "VOICE BUDGET",
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 6.sp
            )

            Text(
                text = "EXPENSE TRACKER",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Premium Loading bar
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.05f))
            ) {
                val progressAnimation by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(2800, easing = LinearEasing),
                    label = "progress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressAnimation)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFBB86FC), Color(0xFF03DAC6))
                            )
                        )
                )
            }
        }

        // Bottom Branding
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Expense Tracker",
                fontSize = 11.sp,
                color = Color.White.copy(0.25f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "© 2026 VoiceBudget | Made in India",
                fontSize = 10.sp,
                color = Color.White.copy(0.15f)
            )
        }
    }
}