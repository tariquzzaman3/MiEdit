package com.miedit.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miedit.app.data.DeviceProfiles
import com.miedit.app.data.WatchfaceDesign
import com.miedit.app.data.WidgetSpec
import com.miedit.app.data.WidgetType
import com.miedit.app.data.WatchfaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    designId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { WatchfaceRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var design by remember { mutableStateOf<WatchfaceDesign?>(null) }

    LaunchedEffect(designId) {
        withContext(Dispatchers.IO) { repo.get(designId) }?.let { design = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(design?.name ?: "Loading…") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            design?.let { d ->
                                scope.launch(Dispatchers.IO) {
                                    repo.save(d)
                                    withContext(Dispatchers.Main) {
                                        snackbar.showSnackbar("Saved to device")
                                    }
                                }
                            }
                        }
                    ) { Icon(Icons.Default.Save, contentDescription = "Save") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val d = design
        if (d == null) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val profile = DeviceProfiles.byId(d.modelId)
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BandPreview(
                    design = d,
                    profileWidth = profile.width,
                    profileHeight = profile.height
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "Live preview of your display.\nDrag & drop editing arrives in the next update.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BandPreview(design: WatchfaceDesign, profileWidth: Int, profileHeight: Int) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(profileWidth.toFloat() / profileHeight.toFloat())
            .background(parseColorSafe(design.background.color)),
        contentAlignment = Alignment.TopStart
    ) {
        val scale = maxWidth.value / profileWidth
        design.widgets.forEach { w ->
            Text(
                text = previewText(w),
                color = parseColorSafe(w.color),
                fontSize = (w.size * scale).sp,
                modifier = Modifier.offset((w.x * scale).dp, (w.y * scale).dp)
            )
        }
    }
}

private fun previewText(w: WidgetSpec): String = when (w.type) {
    WidgetType.TEXT -> w.text.ifEmpty { "Text" }
    WidgetType.TIME -> "10:09"
    WidgetType.DATE -> "Mon 01"
    WidgetType.STEPS -> "8,452"
    WidgetType.BATTERY -> "72%"
    WidgetType.HEARTRATE -> "68"
    WidgetType.WEATHER -> "24°"
    WidgetType.IMAGE -> "[IMG]"
}

private fun parseColorSafe(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.White)
