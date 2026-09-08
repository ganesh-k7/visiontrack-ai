package com.visiontrack.standalone

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.visiontrack.standalone.ai.YoloDetector
import com.visiontrack.standalone.data.LocalHistoryStore
import com.visiontrack.standalone.model.Detection
import com.visiontrack.standalone.model.HistoryItem
import com.visiontrack.standalone.ui.DetectionOverlay
import com.visiontrack.standalone.ui.theme.Accent
import com.visiontrack.standalone.ui.theme.AccentBlue
import com.visiontrack.standalone.ui.theme.Ink
import com.visiontrack.standalone.ui.theme.Line
import com.visiontrack.standalone.ui.theme.Muted
import com.visiontrack.standalone.ui.theme.SurfaceHigh
import com.visiontrack.standalone.ui.theme.VisionTrackTheme
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VisionTrackTheme { VisionTrackApp() } }
    }
}

private enum class Tab(
    val label: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Default.Home),
    DETECT("Detect", Icons.Default.CenterFocusStrong),
    BARCODE("Scan", Icons.Default.QrCodeScanner),
    HISTORY("History", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisionTrackApp() {
    val context = LocalContext.current
    val historyStore = remember { LocalHistoryStore(context) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var historyRefresh by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = Ink,
        topBar = {
            if (tab != Tab.HOME) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                when (tab) {
                                    Tab.DETECT -> "Object Detection"
                                    Tab.BARCODE -> "Code Scanner"
                                    Tab.HISTORY -> "Scan History"
                                    else -> "VisionTrack AI"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "VISIONTRACK AI",
                                fontSize = 9.sp,
                                color = Muted,
                                letterSpacing = 1.8.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { tab = Tab.HOME }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Ink
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D1119),
                tonalElevation = 0.dp
            ) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Accent,
                            selectedTextColor = Accent,
                            indicatorColor = Accent.copy(alpha = 0.12f),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Ink)
        ) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    store = historyStore,
                    refreshKey = historyRefresh,
                    onDetect = { tab = Tab.DETECT },
                    onBarcode = { tab = Tab.BARCODE },
                    onHistory = { tab = Tab.HISTORY }
                )

                Tab.DETECT -> DetectionScreen(
                    historyStore = historyStore,
                    onHistoryChanged = { historyRefresh++ }
                )

                Tab.BARCODE -> BarcodeScreen(
                    historyStore = historyStore,
                    onHistoryChanged = { historyRefresh++ }
                )

                Tab.HISTORY -> key(historyRefresh) {
                    HistoryScreen(historyStore)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    store: LocalHistoryStore,
    refreshKey: Int,
    onDetect: () -> Unit,
    onBarcode: () -> Unit,
    onHistory: () -> Unit
) {
    key(refreshKey) {
        val history = remember { store.load() }
        val todayStart = remember {
            val now = java.util.Calendar.getInstance()
            now.set(java.util.Calendar.HOUR_OF_DAY, 0)
            now.set(java.util.Calendar.MINUTE, 0)
            now.set(java.util.Calendar.SECOND, 0)
            now.set(java.util.Calendar.MILLISECOND, 0)
            now.timeInMillis
        }
        val todayCount = history.count { it.timestamp >= todayStart }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Accent, AccentBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF061217)
                        )
                    }

                    Column(Modifier.padding(start = 12.dp)) {
                        Text(
                            "VisionTrack AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "ON-DEVICE VISUAL INTELLIGENCE",
                            color = Muted,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            item {
                HeroCard()
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        value = todayCount.toString(),
                        label = "Scans today",
                        icon = Icons.Default.CenterFocusStrong
                    )
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        value = "Offline",
                        label = "AI status",
                        icon = Icons.Default.CloudOff
                    )
                    MiniStatCard(
                        modifier = Modifier.weight(1f),
                        value = "Local",
                        label = "Privacy",
                        icon = Icons.Default.Memory
                    )
                }
            }

            item {
                SectionHeading(
                    title = "Explore",
                    subtitle = "Everything runs directly on this device"
                )
            }

            item {
                FeatureCard(
                    icon = Icons.Default.PhotoCamera,
                    title = "Object Detection",
                    subtitle = "Recognize everyday objects with YOLO26n",
                    badge = "ONNX",
                    gradient = listOf(Color(0xFF16332D), Color(0xFF101B20)),
                    onClick = onDetect
                )
            }

            item {
                FeatureCard(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Barcode Scanner",
                    subtitle = "QR, DataMatrix, EAN, Code 128 and more",
                    badge = "ML KIT",
                    gradient = listOf(Color(0xFF16273D), Color(0xFF101722)),
                    onClick = onBarcode
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionHeading(
                        title = "Recent activity",
                        subtitle = if (history.isEmpty()) "Your scans will appear here" else "Latest on-device results"
                    )

                    if (history.isNotEmpty()) {
                        TextButton(onClick = onHistory) {
                            Text("View all")
                        }
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    EmptyRecentCard()
                }
            } else {
                items(history.take(3)) { item ->
                    RecentActivityRow(item)
                }
            }

            item {
                TechnologyStrip()
            }
        }
    }
}

@Composable
private fun HeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF173B35),
                            Color(0xFF16293D),
                            Color(0xFF121824)
                        )
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(Icons.Default.CheckCircle, "Runs fully offline", Accent)
                    StatusPill(Icons.Default.AutoAwesome, "YOLO26n", AccentBlue)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "See more.\nSend nothing.",
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Private computer vision on your phone — no cloud upload, no backend, no network dependency.",
                        color = Color(0xFFBAC4D3),
                        lineHeight = 21.sp
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.10f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    HeroMetric("YOLO26n", "Detector")
                    HeroMetric("ONNX", "Runtime")
                    HeroMetric("100%", "On-device")
                }
            }
        }
    }
}

@Composable
private fun StatusPill(icon: ImageVector, text: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun HeroMetric(value: String, label: String) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceHigh)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(label, color = Muted, fontSize = 10.sp, lineHeight = 12.sp)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Accent, modifier = Modifier.size(26.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 15.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color.White.copy(alpha = 0.07f)
                    ) {
                        Text(
                            badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp,
                            color = Muted
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = Color(0xFFAFBAC9), fontSize = 12.sp)
            }

            Icon(Icons.Default.ArrowForward, null, tint = Muted)
        }
    }
}

@Composable
private fun EmptyRecentCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceHigh)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.History, null, tint = Accent)
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text("Nothing scanned yet", fontWeight = FontWeight.SemiBold)
                Text(
                    "Run object detection or scan a code to get started.",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun RecentActivityRow(item: HistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceHigh)
    ) {
        Row(
            Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (item.type == "barcode") AccentBlue.copy(alpha = 0.12f)
                        else Accent.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.type == "barcode") Icons.Default.QrCodeScanner else Icons.Default.CenterFocusStrong,
                    null,
                    tint = if (item.type == "barcode") AccentBlue else Accent,
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(
                    item.subtitle,
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(item.timestamp)),
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun TechnologyStrip() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1119))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Memory, null, tint = Muted, modifier = Modifier.size(17.dp))
            Text(
                "Kotlin  •  Jetpack Compose  •  CameraX  •  ONNX Runtime  •  ML Kit",
                color = Muted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun CameraPermission(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    if (granted) {
        content()
    } else {
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceHigh)
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, tint = Accent)
                    }
                    Text("Camera access needed", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "VisionTrack processes camera content locally on this device.",
                        color = Muted
                    )
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text("Allow camera")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectionScreen(
    historyStore: LocalHistoryStore,
    onHistoryChanged: () -> Unit
) {
    CameraPermission {
        val context = LocalContext.current
        val detector = remember { YoloDetector(context) }
        DisposableEffect(Unit) { onDispose { detector.close() } }

        var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
        var processing by remember { mutableStateOf(false) }
        var inferenceMs by remember { mutableStateOf<Long?>(null) }
        var status by remember {
            mutableStateOf(
                if (detector.isModelAvailable()) "Ready"
                else "Model missing — run tools\\prepare_model.ps1 and rebuild."
            )
        }

        val executor = remember { Executors.newSingleThreadExecutor() }
        DisposableEffect(Unit) { onDispose { executor.shutdown() } }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black)
            ) {
                if (bitmap == null) {
                    CameraPreviewForCapture(
                        modifier = Modifier.fillMaxSize(),
                        onReady = { imageCapture = it }
                    )

                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp),
                        shape = RoundedCornerShape(100.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Accent)
                            )
                            Text("YOLO26n · ON DEVICE", fontSize = 10.sp)
                        }
                    }

                    CameraFocusFrame(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.74f)
                            .aspectRatio(1f)
                    )
                } else {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Captured image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    DetectionOverlay(
                        detections = detections,
                        imageWidth = bitmap!!.width,
                        imageHeight = bitmap!!.height
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (bitmap == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = SurfaceHigh
                        ) {
                            Text(
                                if (processing) "Processing image…" else "Frame an object and capture",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (processing) Accent else Muted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        enabled = !processing && detector.isModelAvailable(),
                        onClick = {
                            val capture = imageCapture ?: return@Button
                            processing = true
                            status = "Capturing…"

                            val file = File.createTempFile("visiontrack_", ".jpg", context.cacheDir)
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                            capture.takePicture(
                                outputOptions,
                                executor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(
                                        outputFileResults: ImageCapture.OutputFileResults
                                    ) {
                                        try {
                                            val captured = BitmapFactory.decodeFile(file.absolutePath)
                                            val start = System.currentTimeMillis()
                                            val found = detector.detect(captured)
                                            val elapsed = System.currentTimeMillis() - start

                                            runOnMain(context) {
                                                bitmap = captured
                                                detections = found
                                                inferenceMs = elapsed
                                                processing = false
                                                status = "${found.size} object(s)"

                                                if (found.isNotEmpty()) {
                                                    historyStore.add(
                                                        HistoryItem(
                                                            type = "detection",
                                                            title = found.first().label,
                                                            subtitle = "${found.size} detections • ${(found.first().confidence * 100).toInt()}% top confidence",
                                                            timestamp = System.currentTimeMillis()
                                                        )
                                                    )
                                                    onHistoryChanged()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            runOnMain(context) {
                                                processing = false
                                                status = e.message ?: "Detection failed"
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        runOnMain(context) {
                                            processing = false
                                            status = exception.message ?: "Capture failed"
                                        }
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .align(Alignment.CenterHorizontally),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color(0xFF001A14)
                        )
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(30.dp))
                    }
                } else {
                    DetectionResultCard(
                        detections = detections,
                        inferenceMs = inferenceMs,
                        status = status,
                        onScanAgain = {
                            bitmap = null
                            detections = emptyList()
                            inferenceMs = null
                            status = "Ready"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraFocusFrame(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 4.dp.toPx()
        val segment = size.width * 0.17f
        val r = 22.dp.toPx()
        val c = Accent

        // corners
        drawLine(c, Offset(0f, segment), Offset(0f, r), stroke)
        drawLine(c, Offset(r, 0f), Offset(segment, 0f), stroke)

        drawLine(c, Offset(size.width - segment, 0f), Offset(size.width - r, 0f), stroke)
        drawLine(c, Offset(size.width, r), Offset(size.width, segment), stroke)

        drawLine(c, Offset(0f, size.height - segment), Offset(0f, size.height - r), stroke)
        drawLine(c, Offset(r, size.height), Offset(segment, size.height), stroke)

        drawLine(c, Offset(size.width - segment, size.height), Offset(size.width - r, size.height), stroke)
        drawLine(c, Offset(size.width, size.height - r), Offset(size.width, size.height - segment), stroke)
    }
}

@Composable
private fun DetectionResultCard(
    detections: List<Detection>,
    inferenceMs: Long?,
    status: String,
    onScanAgain: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceHigh)
    ) {
        Column(
            Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Detection result", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(status, color = Muted, fontSize = 12.sp)
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = Accent.copy(alpha = 0.10f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Default.Speed, null, tint = Accent, modifier = Modifier.size(15.dp))
                        Text("${inferenceMs ?: 0} ms", color = Accent, fontSize = 11.sp)
                    }
                }
            }

            if (detections.isEmpty()) {
                Text("No supported object detected.", color = Muted)
            } else {
                detections.take(4).forEach { detection ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            detection.label.replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${(detection.confidence * 100).toInt()}%",
                            color = Accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color(0xFF001A14)
                )
            ) {
                Text("Scan another", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CameraPreviewForCapture(
    modifier: Modifier,
    onReady: (ImageCapture) -> Unit
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )

                    onReady(capture)
                }, ContextCompat.getMainExecutor(ctx))
            }
        }
    )
}

@Composable
private fun BarcodeScreen(
    historyStore: LocalHistoryStore,
    onHistoryChanged: () -> Unit
) {
    CameraPermission {
        val context = LocalContext.current
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        var lastCode by remember { mutableStateOf<String?>(null) }
        var lastFormat by remember { mutableStateOf<String?>(null) }
        var lastSaved by remember { mutableStateOf<String?>(null) }

        Column(
            Modifier
                .fillMaxSize()
                .background(Ink)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                val scanner = BarcodeScanning.getClient(
                                    BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                        .build()
                                )

                                analysis.setAnalyzer(
                                    ContextCompat.getMainExecutor(ctx)
                                ) { proxy ->
                                    val mediaImage = proxy.image
                                    if (mediaImage == null) {
                                        proxy.close()
                                        return@setAnalyzer
                                    }

                                    val input = InputImage.fromMediaImage(
                                        mediaImage,
                                        proxy.imageInfo.rotationDegrees
                                    )

                                    scanner.process(input)
                                        .addOnSuccessListener { barcodes ->
                                            val first = barcodes.firstOrNull()
                                            val value = first?.rawValue

                                            if (!value.isNullOrBlank()) {
                                                lastCode = value
                                                lastFormat = barcodeFormatName(first.format)

                                                if (lastSaved != value) {
                                                    lastSaved = value

                                                    historyStore.add(
                                                        HistoryItem(
                                                            type = "barcode",
                                                            title = lastFormat ?: "Barcode",
                                                            subtitle = value,
                                                            timestamp = System.currentTimeMillis()
                                                        )
                                                    )

                                                    onHistoryChanged()
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { proxy.close() }
                                }

                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis
                                )
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    }
                )

                ScannerFrame(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.76f)
                        .aspectRatio(1.25f)
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    shape = RoundedCornerShape(100.dp),
                    color = Color.Black.copy(alpha = 0.58f)
                ) {
                    Text(
                        "QR · DATAMATRIX · EAN · CODE 128",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceHigh)
            ) {
                Column(
                    Modifier.padding(17.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (lastCode == null) {
                        Text("Ready to scan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Align a barcode or QR code inside the frame.",
                            color = Muted
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Accent)
                            Text(lastFormat ?: "Barcode", fontWeight = FontWeight.Bold)
                        }

                        Text(
                            lastCode!!,
                            color = Color(0xFFD9E2EF),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text("Saved locally", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 3.dp.toPx()
        val radius = 22.dp.toPx()

        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f),
            size = size,
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke)
        )

        drawLine(
            color = Accent,
            start = Offset(18.dp.toPx(), size.height / 2f),
            end = Offset(size.width - 18.dp.toPx(), size.height / 2f),
            strokeWidth = 2.dp.toPx()
        )
    }
}

private fun barcodeFormatName(format: Int): String = when (format) {
    Barcode.FORMAT_QR_CODE -> "QR Code"
    Barcode.FORMAT_DATA_MATRIX -> "DataMatrix"
    Barcode.FORMAT_CODE_128 -> "Code 128"
    Barcode.FORMAT_EAN_13 -> "EAN-13"
    Barcode.FORMAT_EAN_8 -> "EAN-8"
    Barcode.FORMAT_UPC_A -> "UPC-A"
    Barcode.FORMAT_UPC_E -> "UPC-E"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_AZTEC -> "Aztec"
    else -> "Barcode"
}

@Composable
private fun HistoryScreen(store: LocalHistoryStore) {
    var history by remember { mutableStateOf(store.load()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${history.size} saved result${if (history.size == 1) "" else "s"}",
                    color = Muted,
                    fontSize = 12.sp
                )
                Text(
                    "Stored only on this device",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (history.isNotEmpty()) {
                TextButton(
                    onClick = {
                        store.clear()
                        history = emptyList()
                    }
                ) {
                    Icon(
                        Icons.Default.ClearAll,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("Clear")
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Accent.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            null,
                            tint = Accent,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Text("No history yet", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        "Object detections and code scans will be saved here.",
                        color = Muted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { item ->
                    HistoryCard(item)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (item.type == "barcode") AccentBlue.copy(alpha = 0.12f)
                        else Accent.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.type == "barcode") Icons.Default.QrCodeScanner
                    else Icons.Default.CenterFocusStrong,
                    null,
                    tint = if (item.type == "barcode") AccentBlue else Accent
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 13.dp)
            ) {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(
                    item.subtitle,
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                DateFormat.getDateTimeInstance(
                    DateFormat.SHORT,
                    DateFormat.SHORT
                ).format(Date(item.timestamp)),
                color = Muted,
                fontSize = 10.sp
            )
        }
    }
}

private fun runOnMain(
    context: android.content.Context,
    block: () -> Unit
) {
    ContextCompat.getMainExecutor(context).execute(block)
}
