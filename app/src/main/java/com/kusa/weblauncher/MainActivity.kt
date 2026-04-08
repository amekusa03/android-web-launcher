package com.kusa.weblauncher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.kusa.weblauncher.data.SlotInfo
import com.kusa.weblauncher.data.SlotRepository
import com.kusa.weblauncher.ui.theme.WebLauncherTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val repository by lazy { SlotRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialSlot = intent.getStringExtra("TARGET_SLOT")

        enableEdgeToEdge()
        setContent {
            WebLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SlotManagerScreen(
                        repository = repository,
                        initialSlot = initialSlot,
                        onUpdateAlias = { slotKey, enabled ->
                            updateAliasEnabled(slotKey, enabled)
                        }
                    )
                }
            }
        }
    }

    private fun updateAliasEnabled(slotKey: String, enabled: Boolean) {
        val aliasName = slotKey.replace("slot_", "Slot").replaceFirstChar { it.uppercase() }
        val componentName = ComponentName(packageName, "$packageName.$aliasName")

        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        try {
            packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                0 
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun formatSlotDisplayName(slotKey: String): String {
    return when (slotKey) {
        "slot_1" -> "Web①"
        "slot_2" -> "Web②"
        "slot_3" -> "Web③"
        "slot_4" -> "Web④"
        "slot_5" -> "Web⑤"
        else -> slotKey
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotManagerScreen(
    repository: SlotRepository,
    initialSlot: String?,
    onUpdateAlias: (String, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val slots = listOf("slot_1", "slot_2", "slot_3", "slot_4", "slot_5")
    var selectedSlot by remember { mutableStateOf<String?>(initialSlot) }

    if (selectedSlot != null) {
        val slotKey = selectedSlot!!
        val currentInfo by repository.getSlotInfo(slotKey).collectAsState(initial = null)

        EditSlotDialog(
            slotKey = slotKey,
            initialInfo = currentInfo ?: SlotInfo(),
            onDismiss = { selectedSlot = null },
            onSave = { newInfo ->
                scope.launch {
                    repository.saveSlotInfo(slotKey, newInfo)
                    onUpdateAlias(slotKey, newInfo.url.isNotEmpty())
                    selectedSlot = null
                }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("WebLauncher Slots") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(slots) { slotKey ->
                val info by repository.getSlotInfo(slotKey).collectAsState(initial = SlotInfo())
                ListItem(
                    headlineContent = {
                        val baseName = formatSlotDisplayName(slotKey)
                        Text(if (info?.label.isNullOrEmpty()) "未設定 ($baseName)" else info!!.label)
                    },
                    supportingContent = { Text(info?.url ?: "URL未設定") },
                    trailingContent = {
                        if (info?.url?.isNotEmpty() == true) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("有効") }
                        }
                    },
                    modifier = Modifier.clickable { selectedSlot = slotKey }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun EditSlotDialog(
    slotKey: String,
    initialInfo: SlotInfo,
    onDismiss: () -> Unit,
    onSave: (SlotInfo) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var label by remember(initialInfo) { mutableStateOf(initialInfo.label) }
    var url by remember(initialInfo) { mutableStateOf(initialInfo.url) }
    var iconColor by remember(initialInfo) { mutableStateOf(initialInfo.iconColor) }
    var useCustomTabs by remember(initialInfo) { mutableStateOf(initialInfo.useCustomTabs) }

    // 選びやすいプリセットカラー
    val presetColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7", 
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", 
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39", 
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#000000"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("編集: ${formatSlotDisplayName(slotKey)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("名前") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (http://〜)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // アイコン色の選択（カラーチップ）
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("アイコンの色", style = MaterialTheme.typography.labelLarge)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(presetColors) { colorHex ->
                            val colorInt = try { Color.parseColor(colorHex) } catch (e: Exception) { Color.GRAY }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(androidx.compose.ui.graphics.Color(colorInt))
                                    .clickable { iconColor = colorHex }
                                    .border(
                                        width = if (iconColor.equals(colorHex, ignoreCase = true)) 4.dp else 0.dp,
                                        color = if (iconColor.equals(colorHex, ignoreCase = true)) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useCustomTabs = !useCustomTabs }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = useCustomTabs, onCheckedChange = { useCustomTabs = it })
                    Text("Chrome Custom Tabsを使用する")
                }

                Button(
                    onClick = {
                        createHomeScreenShortcut(context, label, url, iconColor, slotKey)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = url.startsWith("http") && label.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("ホーム画面に追加")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(SlotInfo(label, url, iconColor, useCustomTabs)) },
                enabled = url.isEmpty() || url.startsWith("http")
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        }
    )
}

private fun createHomeScreenShortcut(
    context: android.content.Context,
    label: String,
    url: String,
    colorHex: String,
    slotKey: String
) {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
        return
    }

    val shortcutIntent = Intent(context, LauncherActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        val aliasName = slotKey.replace("slot_", "Slot").replaceFirstChar { it.uppercase() }
        setClassName(context.packageName, "${context.packageName}.$aliasName")
    }

    val colorInt = try { Color.parseColor(colorHex) } catch (e: Exception) { Color.BLUE }
    val iconDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(colorInt)
        setSize(128, 128)
    }
    val bitmap = android.graphics.Bitmap.createBitmap(128, 128, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    iconDrawable.setBounds(0, 0, canvas.width, canvas.height)
    iconDrawable.draw(canvas)

    val shortcut = ShortcutInfoCompat.Builder(context, "shortcut_${System.currentTimeMillis()}")
        .setShortLabel(label)
        .setIcon(IconCompat.createWithBitmap(bitmap))
        .setIntent(shortcutIntent)
        .build()

    ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}
