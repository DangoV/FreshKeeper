package com.freshkeeper.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerRoute(
    viewModel: FreshKeeperViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BarcodeScannerScreen(
        barcode = uiState.form.barcode,
        onBarcodeDetected = viewModel::onBarcodeScanned,
        onBack = viewModel::closeScanner,
    )
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
private fun BarcodeScannerScreen(
    barcode: String,
    onBarcodeDetected: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)

    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasCameraPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Нет доступа к камере", style = MaterialTheme.typography.titleMedium)
            Text("Разрешите доступ к камере в настройках приложения")
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Назад")
            }
        }
        return
    }

    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_QR_CODE,
                )
                .build(),
        )
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    var lastScannedValue by remember { mutableStateOf<String?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var isAnalyzerBusy by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                imageAnalysis.clearAnalyzer()
                scanner.close()
            } catch (_: Exception) {
            }
            cameraExecutor.shutdown()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val bindCamera = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null || isAnalyzerBusy) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    isAnalyzerBusy = true
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            val rawValue = barcodes.firstOrNull()?.rawValue
                            if (!rawValue.isNullOrBlank() && rawValue != lastScannedValue) {
                                lastScannedValue = rawValue
                                currentOnBarcodeDetected(rawValue)
                            }
                        }
                        .addOnCompleteListener {
                            isAnalyzerBusy = false
                            imageProxy.close()
                        }
                }

                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            } catch (_: Exception) {
            }
        }

        cameraProviderFuture.addListener(bindCamera, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                val provider = cameraProviderFuture.get()
                provider.unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(torchEnabled, camera) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    LaunchedEffect(barcode) {
        if (barcode.isNotBlank() && barcode != lastScannedValue) {
            lastScannedValue = barcode
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Наведите камеру на штрихкод", style = MaterialTheme.typography.titleMedium)
                Text("Сканирование активно")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color(0xFF2E7D32))
                        .padding(vertical = 6.dp),
                ) {
                    Text(
                        text = "Камера активна",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                Button(
                    onClick = { torchEnabled = !torchEnabled },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (torchEnabled) "Выключить фонарик" else "Включить фонарик")
                }

                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Отмена")
                }
            }
        }
    }
}
