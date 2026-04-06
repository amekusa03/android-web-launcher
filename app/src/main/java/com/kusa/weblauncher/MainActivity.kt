package com.kusa.weblauncher

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    // updateAliasEnabled メソッドを関数リファレンスとして渡す
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

    /**
     * [FR-06] エイリアスの動的無効化
     * URLが設定されている場合はドロワーに表示し、空の場合は非表示にする
     */
    private fun updateAliasEnabled(slotKey: String, enabled: Boolean) {
        // slot_1 -> com.kusa.weblauncher.Slot1 に変換
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
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        // val currentInfo by repository.getSlotInfo(slotKey).collectAsState(initial = SlotInfo())
        val currentInfo by repository.getSlotInfo(slotKey).collectAsState(initial = null)

        // データがロードされた（nullでなくなった）らダイアログを表示
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
                        Text(if (info?.label.isNullOrEmpty()) "未設定 ($slotKey)" else info!!.label)
                    },
                    supportingContent = { Text(info?.url ?: "URL未設定") },
                    trailingContent = {
                        if (info?.url?.isNotEmpty() == true) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("有効") }
                        }
                    },
                    modifier = Modifier.clickable { selectedSlot = slotKey }
                )
                HorizontalDivider() // ここに不要な引数がないことを確認
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

    //var label by remember { mutableStateOf(initialInfo.label) }
    //var url by remember { mutableStateOf(initialInfo.url) }
    //var iconColor by remember { mutableStateOf(initialInfo.iconColor) }
    //var useCustomTabs by remember { mutableStateOf(initialInfo.useCustomTabs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("スロット編集: $slotKey") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                OutlinedTextField(
                    value = iconColor,
                    onValueChange = { iconColor = it },
                    label = { Text("アイコン色 (Hex: #RRGGBB)") },
                    modifier = Modifier.fillMaxWidth()
                )
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

                // [FR-02] ホーム画面ショートカット追加ボタン
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
/**
 * [FR-02] ホーム画面に動的ショートカットを作成する
 */
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

    // ショートカット起動時のIntent (LauncherActivity経由)
    val shortcutIntent = Intent(context, LauncherActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        // SlotIDを埋め込むことで、起動時にどの設定を使うかLauncherActivityが判断できる
        // activity-alias名に合わせて Slot1, Slot2... の形式で送る
        val aliasName = slotKey.replace("slot_", "Slot").replaceFirstChar { it.uppercase() }
        setClassName(context.packageName, "${context.packageName}.$aliasName")
    }

    // アイコンの生成 (指定された色で円形アイコンを作成)
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