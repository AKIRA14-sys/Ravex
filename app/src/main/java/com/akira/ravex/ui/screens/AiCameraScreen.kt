package com.akira.ravex.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.akira.ravex.ai.AiChatMessage
import com.akira.ravex.ai.AiFeatureModule
import com.akira.ravex.ai.AiRequestRouter
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.ui.theme.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun AiCameraScreen(ravexPrefs: RavexPreferences) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val router = remember { AiRequestRouter(context) }

    var hasCameraPermission by remember { mutableStateOf(false) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var base64Image by remember { mutableStateOf<String?>(null) }
    var isConsentGiven by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission required for AI Camera Assistant.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("RAVEX AI CAMERA — GAMING SETUP ASSISTANT", color = RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Analyze physical gaming desk, monitor setup, posture, or photographed graphics settings page.", color = RavexTextMuted, fontSize = 10.sp)

        Spacer(modifier = Modifier.height(8.dp))

        if (!hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(RavexSurface, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = RavexCyan)
                ) {
                    Text("Grant Camera Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Camera Preview / Captured Photo Container
                Card(
                    colors = CardDefaults.cardColors(containerColor = RavexSurface),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (capturedBitmap == null) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageCapture
                                            )
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            Button(
                                onClick = {
                                    val photoFile = File(context.cacheDir, "temp_capture.jpg")
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                                    imageCapture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                val bmp = BitmapFactory.decodeFile(photoFile.absolutePath)
                                                capturedBitmap = bmp

                                                // Compress and Convert to Base64
                                                val baos = ByteArrayOutputStream()
                                                bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                                                val bytes = baos.toByteArray()
                                                base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                Toast.makeText(context, "Capture Error: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp)
                            ) {
                                Text("CAPTURE SETUP PHOTO", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Setup",
                                modifier = Modifier.fillMaxSize()
                            )

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        capturedBitmap = null
                                        base64Image = null
                                        analysisResult = null
                                        isConsentGiven = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RavexRed)
                                ) {
                                    Text("RETAKE", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // AI Image Analysis Panel
                Card(
                    colors = CardDefaults.cardColors(containerColor = RavexSurface),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("VISUAL AI ANALYSIS", color = RavexGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Send photo securely to Vision AI model for gaming setup review.", color = RavexTextMuted, fontSize = 9.sp)

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isConsentGiven,
                                onCheckedChange = { isConsentGiven = it },
                                colors = CheckboxDefaults.colors(checkedColor = RavexCyan)
                            )
                            Text("I consent to uploading photo to AI provider for analysis.", color = Color.White, fontSize = 9.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                if (base64Image == null) {
                                    Toast.makeText(context, "Please capture a photo first!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!isConsentGiven) {
                                    Toast.makeText(context, "Consent required before uploading image.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isAnalyzing = true
                                scope.launch {
                                    val msg = AiChatMessage(
                                        role = "user",
                                        content = "Analyze this gaming setup image. Evaluate desk ergonomics, monitor distance, phone stand posture, lighting, or photographed in-game settings.",
                                        imageBase64 = base64Image
                                    )
                                    val resp = router.executeRequest(
                                        feature = AiFeatureModule.CAMERA_VISION,
                                        messages = listOf(msg),
                                        requireVision = true
                                    )
                                    isAnalyzing = false
                                    analysisResult = resp.text
                                }
                            },
                            enabled = base64Image != null && isConsentGiven && !isAnalyzing,
                            colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text("ANALYZE SETUP PHOTO", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = analysisResult ?: "Capture a photo and enable consent to receive AI vision feedback.",
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
