package com.miedit.app.ui.home

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miedit.app.data.DeviceProfile
import com.miedit.app.data.DeviceProfiles
import com.miedit.app.data.WatchfaceDesign
import com.miedit.app.data.WatchfaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WatchfaceRepository(app)

    private val _designs = MutableStateFlow<List<WatchfaceDesign>>(emptyList())
    val designs: StateFlow<List<WatchfaceDesign>> = _designs.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _designs.value = repo.list()
        }
    }

    fun createNew(profile: DeviceProfile, onCreated: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val design = WatchfaceDesign(name = "New Display", modelId = profile.id)
            repo.save(design)
            withContext(Dispatchers.Main) { onCreated(design.id) }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.delete(id)
            refresh()
        }
    }
}

private val dateFormat = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenEditor: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val designs by viewModel.designs.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WatchfaceDesign?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("MiEdit") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPicker = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Display") }
            )
        }
    ) { padding ->
        if (designs.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(designs, key = { it.id }) { design ->
                    DesignCard(
                        design = design,
                        onOpen = { onOpenEditor(design.id) },
                        onDelete = { pendingDelete = design }
                    )
                }
            }
        }
    }

    if (showPicker) {
        ModelPickerDialog(
            onDismiss = { showPicker = false },
            onCreate = { profile ->
                showPicker = false
                viewModel.createNew(profile) { id -> onOpenEditor(id) }
            }
        )
    }

    pendingDelete?.let { design ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete display?") },
            text = { Text("\"${design.name}\" will be removed from your device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(design.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Watch, contentDescription = null, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("No displays yet")
        Spacer(Modifier.height(4.dp))
        Text(
            "Create a display from scratch, or connect\nyour Mi Band to import its designs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesignCard(
    design: WatchfaceDesign,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val profile = DeviceProfiles.byId(design.modelId)
    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    design.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${profile.displayName} · ${profile.width}×${profile.height}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (design.updatedAt > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        dateFormat.format(Date(design.updatedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun ModelPickerDialog(
    onDismiss: () -> Unit,
    onCreate: (DeviceProfile) -> Unit
) {
    var selected by remember { mutableStateOf(DeviceProfiles.all.first { it.id == "band7" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Which band?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                DeviceProfiles.all.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = profile },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected.id == profile.id,
                            onClick = { selected = profile }
                        )
                        Column {
                            Text(profile.displayName)
                            Text(
                                "${profile.width} × ${profile.height}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(selected) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
