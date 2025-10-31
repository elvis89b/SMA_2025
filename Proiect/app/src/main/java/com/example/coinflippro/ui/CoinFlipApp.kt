package com.example.coinflippro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coinflippro.data.local.FlipEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinFlipApp(viewModel: FlipViewModel) {
    val flips by viewModel.flips.collectAsState()
    var currentTab by rememberSaveable { mutableStateOf(0) } // 0=Flip, 1=History
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (currentTab == 0) "Coin Flip" else "History") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    label = { Text("Flip") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text("History") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (currentTab) {
                0 -> FlipScreen(onResult = { isHeads -> viewModel.recordFlip(isHeads) })
                1 -> HistoryScreen(
                    flips = flips,
                    onDelete = { id -> viewModel.deleteFlip(id) },
                    onUpdate = { id, isHeads -> viewModel.updateFlip(id, isHeads) },
                    onClearAll = { viewModel.clearAll() },
                    onUndo = { wasHeads -> viewModel.recordFlip(wasHeads) },
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}


// Flip Screen
@Composable
private fun FlipScreen(onResult: (Boolean) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = LocalHapticFeedback.current

    var isFlipping by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<Boolean?>(null) }
    var showConfetti by remember { mutableStateOf(false) }

    var shakeEnabled by rememberSaveable { mutableStateOf(true) }
    var sensitivity by rememberSaveable { mutableStateOf(0.5f) }

    val rotationY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    //Vibration
    val vibrator: Any? = remember {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
    }
    fun vibrateShort() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                val effect = android.os.VibrationEffect.createOneShot(90L, 180)
                when (vibrator) {
                    is android.os.Vibrator -> vibrator.vibrate(effect)
                }
            } else {
                if (vibrator is android.os.Vibrator) {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(70L)
                }
            }
        } catch (_: Throwable) {}
    }

    // Sound
    val mediaPlayer = remember {
        try { android.media.MediaPlayer.create(context, com.example.coinflippro.R.raw.coin_flip) }
        catch (_: Throwable) { null }
    }
    DisposableEffect(Unit) { onDispose { try { mediaPlayer?.release() } catch (_: Throwable) {} } }
    fun playSound() {
        try {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    mp.pause()
                    mp.seekTo(0)
                }
                mp.start()
            }
        } catch (_: Throwable) {}
    }

    // Accelerometer
    fun thresholdFor(sens: Float): Float = 18f - 8f * sens
    val sensorManager = remember {
        context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
    }
    val accelerometer = remember { sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) }

    var lastTriggerTime by remember { mutableStateOf(0L) }
    var gravityX by remember { mutableStateOf(0f) }
    var gravityY by remember { mutableStateOf(0f) }
    var gravityZ by remember { mutableStateOf(0f) }
    val alpha = 0.8f

    fun runFlip() {
        if (isFlipping) return
        isFlipping = true
        showConfetti = false

        scope.launch {
            val spins = Random.nextInt(2, 5) * 360f
            val headsWins = Random.nextBoolean()
            val resultOffset = if (headsWins) 0f else 180f
            val target = rotationY.value + spins + resultOffset

            rotationY.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            rotationY.snapTo(rotationY.value % 360f)

            lastResult = headsWins
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            vibrateShort()
            playSound()

            onResult(headsWins)

            showConfetti = true
            delay(900)
            showConfetti = false
            isFlipping = false
        }
    }

    DisposableEffect(shakeEnabled, sensitivity, isFlipping) {
        if (!shakeEnabled || accelerometer == null) return@DisposableEffect onDispose { }
        val listener = object : android.hardware.SensorEventListener {
            override fun onSensorChanged(event: android.hardware.SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                gravityX = alpha * gravityX + (1 - alpha) * x
                gravityY = alpha * gravityY + (1 - alpha) * y
                gravityZ = alpha * gravityZ + (1 - alpha) * z

                val linX = x - gravityX
                val linY = y - gravityY
                val linZ = z - gravityZ

                val mag = kotlin.math.sqrt(linX * linX + linY * linY + linZ * linZ)
                val threshold = thresholdFor(sensitivity)
                val now = System.currentTimeMillis()

                if (!isFlipping && mag > threshold && (now - lastTriggerTime) > 600) {
                    lastTriggerTime = now
                    runFlip()
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(
            listener,
            accelerometer,
            android.hardware.SensorManager.SENSOR_DELAY_GAME
        )
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // UI
    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.align(Alignment.TopCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            Coin3D(rotationYDeg = rotationY.value)
            Spacer(Modifier.height(18.dp))
            Text(
                text = lastResult?.let { if (it) "Heads!" else "Tails!" } ?: "Ready?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { runFlip() }, enabled = !isFlipping) {
                Text(if (isFlipping) "Flipping..." else "Flip")
            }
            Spacer(Modifier.height(28.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = shakeEnabled, onCheckedChange = { shakeEnabled = it })
                Spacer(Modifier.width(8.dp)); Text("Shake to flip")
            }
            Column(Modifier.padding(horizontal = 12.dp)) {
                Text("Sensitivity")
                Slider(value = sensitivity, onValueChange = { sensitivity = it }, valueRange = 0f..1f)
            }
        }
        AnimatedVisibility(visible = showConfetti, modifier = Modifier.align(Alignment.Center)) {
            ConfettiOverlay()
        }
    }
}

// Coin

@Composable
private fun Coin3D(rotationYDeg: Float) {
    val angle = ((rotationYDeg % 360f) + 360f) % 360f
    val frontVisible = angle <= 90f || angle >= 270f
    val cameraDistancePx = with(LocalDensity.current) { 48.dp.toPx() }

    Box(
        modifier = Modifier
            .size(180.dp)
            .graphicsLayer { rotationY = angle; cameraDistance = cameraDistancePx }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (frontVisible) "HEADS" else "TAILS",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer { if (!frontVisible) scaleX = -1f }
        )
    }
}

//Confetti

@Composable
private fun ConfettiOverlay() {
    val particles = remember {
        List(30) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.2f,
                size = Random.nextFloat().coerceIn(0.006f, 0.02f),
                speed = Random.nextFloat().coerceIn(0.3f, 0.9f)
            )
        }
    }
    var progress by remember { mutableStateOf(0f) }
    val confettiColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        val durationMs = 900; val steps = 45
        repeat(steps) { progress = it / steps.toFloat(); delay((durationMs / steps).toLong()) }
        progress = 1f
    }

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        particles.forEach { p ->
            val px = p.x * w; val py = (p.y + progress * p.speed) * h
            drawCircle(
                color = confettiColor,
                radius = (p.size * w),
                center = androidx.compose.ui.geometry.Offset(px, py),
                style = Fill
            )
        }
    }
}
private data class ConfettiParticle(val x: Float, val y: Float, val size: Float, val speed: Float)

// History Page - CRUD Ops

@Composable
private fun HistoryScreen(
    flips: List<FlipEntity>,
    onDelete: (Long) -> Unit,
    onUpdate: (Long, Boolean) -> Unit,
    onClearAll: () -> Unit,
    onUndo: (wasHeads: Boolean) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        StatsCard(flips = flips, onClearAll = onClearAll)
        Spacer(Modifier.height(8.dp))

        if (flips.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No flips yet") }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(flips, key = { it.id }) { item ->
                    DismissibleHistoryRow(
                        item = item,
                        onEdit = { onUpdate(item.id, it) },
                        onDismiss = {
                            onDelete(item.id)
                            scope.launch {
                                val res = snackbarHostState.showSnackbar(
                                    message = "Deleted ${if (item.isHeads) "Heads" else "Tails"}",
                                    actionLabel = "Undo",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )
                                if (res == SnackbarResult.ActionPerformed) onUndo(item.isHeads)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DismissibleHistoryRow(
    item: FlipEntity,
    onEdit: (newIsHeads: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 120.dp.toPx() }
    val maxOffsetPx = with(density) { 160.dp.toPx() }
    val offsetX = remember { Animatable(0f) }

    var showEditDialog by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { Icon(Icons.Filled.Delete, contentDescription = null) }

        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.value.toInt(), 0) }
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.surface)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newX = (offsetX.value + delta).coerceIn(-maxOffsetPx, maxOffsetPx)
                        scope.launch { offsetX.snapTo(newX) }
                    },
                    onDragStopped = {
                        val shouldDismiss = abs(offsetX.value) > thresholdPx
                        scope.launch {
                            if (shouldDismiss) {
                                val target = maxOffsetPx * offsetX.value.sign
                                offsetX.animateTo(
                                    target,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                                onDismiss()
                                offsetX.snapTo(0f)
                            } else {
                                offsetX.animateTo(0f, spring())
                            }
                        }
                    }
                )
        ) {
            HistoryRow(item, onEditClick = { showEditDialog = true })
        }
    }

    if (showEditDialog) {
        EditFlipDialog(
            currentIsHeads = item.isHeads,
            onDismiss = { showEditDialog = false },
            onConfirm = { newIsHeads ->
                showEditDialog = false
                if (newIsHeads != item.isHeads) onEdit(newIsHeads)
            }
        )
    }
}

@Composable
private fun HistoryRow(item: FlipEntity, onEditClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) { Text(if (item.isHeads) "H" else "T", fontWeight = FontWeight.Bold) }

        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (item.isHeads) "Heads" else "Tails", fontWeight = FontWeight.SemiBold)
            Text(
                text = formatMillis(item.flippedAtMillis),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onEditClick) { Text("Edit") }
    }
}

@Composable
private fun EditFlipDialog(
    currentIsHeads: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var selection by remember { mutableStateOf(if (currentIsHeads) "HEADS" else "TAILS") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(selection == "HEADS") }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Edit flip") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selection == "HEADS", onClick = { selection = "HEADS" })
                    Spacer(Modifier.width(6.dp)); Text("Heads")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selection == "TAILS", onClick = { selection = "TAILS" })
                    Spacer(Modifier.width(6.dp)); Text("Tails")
                }
            }
        }
    )
}

// Stats

@Composable
private fun StatsCard(flips: List<FlipEntity>, onClearAll: () -> Unit) {
    val total = flips.size
    val heads = flips.count { it.isHeads }
    val tails = total - heads
    val headsPct = if (total > 0) (heads * 100f / total) else 0f
    val tailsPct = if (total > 0) (tails * 100f / total) else 0f
    val (longestHeads, longestTails) = longestStreaks(flips)

    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onClearAll) { Text("Clear all") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatPill(label = "Total", value = "$total")
                StatPill(label = "Heads", value = "$heads (${headsPct.toInt()}%)")
                StatPill(label = "Tails", value = "$tails (${tailsPct.toInt()}%)")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatPill(label = "Longest Heads", value = "${longestHeads}×")
                StatPill(label = "Longest Tails", value = "${longestTails}×")
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

//Utils

private fun formatMillis(millis: Long): String {
    val dt = java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDateTime()
    val time = "%02d:%02d:%02d".format(dt.hour, dt.minute, dt.second)
    return "${dt.toLocalDate()} $time"
}
private fun longestStreaks(flips: List<FlipEntity>): Pair<Int, Int> {
    if (flips.isEmpty()) return 0 to 0
    val sorted = flips.sortedBy { it.flippedAtMillis }
    var curH = 0; var curT = 0; var maxH = 0; var maxT = 0
    for (f in sorted) {
        if (f.isHeads) { curH += 1; curT = 0; if (curH > maxH) maxH = curH }
        else { curT += 1; curH = 0; if (curT > maxT) maxT = curT }
    }
    return maxH to maxT
}
