package com.example.voicebudget

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QRScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { QRScannerScreen() } }
    }
}

@Composable
fun QRScannerScreen() {
    val context = LocalContext.current
    val activity = (context as? Activity)

    var hasCamPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCamPermission = granted }
    )

    LaunchedEffect(Unit) { if (!hasCamPermission) launcher.launch(Manifest.permission.CAMERA) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCamPermission) {
            ScannerCameraPreview { rawResult ->
                // 1. Extract Amount
                val amountMatch = Regex("\\d+(\\.\\d{1,2})?").find(rawResult)
                val totalAmount = amountMatch?.value?.toDoubleOrNull() ?: 0.0

                // 2. Extract Category
                val categoryKeywords = listOf("Food", "Travel", "Fuel", "Bills", "Shopping", "Grocery")
                val foundCategory = categoryKeywords.firstOrNull { rawResult.contains(it, ignoreCase = true) } ?: "General"

                if (totalAmount > 0) {
                    // --- FIX: WE REMOVED DB SAVE HERE ---
                    // Just move to AddExpenseActivity with data
                    val intent = Intent(context, AddExpenseActivity::class.java).apply {
                        putExtra("FETCHED_AMOUNT", totalAmount.toInt().toString())
                        putExtra("FETCHED_CATEGORY", foundCategory)
                        putExtra("FETCHED_NOTE", "QR Scan: $rawResult")
                    }
                    context.startActivity(intent)
                    activity?.finish()
                }
            }
        }

        // Overlay UI
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { activity?.finish() }, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
            Box(modifier = Modifier.size(280.dp).border(BorderStroke(2.dp, Color(0xFF03DAC6)), RoundedCornerShape(24.dp)))
            Text("SCANNING QR FOR TOTAL AMOUNT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun ScannerCameraPreview(onResultFound: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val barcodeScanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                if (barcodes.isNotEmpty()) {
                                    barcodes[0].rawValue?.let {
                                        onResultFound(it)
                                        cameraProvider.unbindAll()
                                    }
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                } catch (e: Exception) { Log.e("Scanner", "Bind failed", e) }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}