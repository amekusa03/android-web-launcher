package com.kusa.weblauncher

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.kusa.weblauncher.data.SlotInfo
import com.kusa.weblauncher.data.SlotRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LauncherActivity : ComponentActivity() {
    private val repository by lazy { SlotRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val componentName = intent.component?.className ?: ""
        val slotKey = when {
            componentName.endsWith("Slot1") -> "slot_1"
            componentName.endsWith("Slot2") -> "slot_2"
            componentName.endsWith("Slot3") -> "slot_3"
            componentName.endsWith("Slot4") -> "slot_4"
            componentName.endsWith("Slot5") -> "slot_5"
            else -> null
        }

        if (slotKey != null) {
            lifecycleScope.launch {
                val info = repository.getSlotInfo(slotKey).first()
                if (info != null && info.url.isNotEmpty()) {
                    launchBrowser(info)
                } else {
                    navigateToAdmin(slotKey)
                }
            }
        } else {
            navigateToAdmin(null)
        }
    }

    private fun launchBrowser(info: SlotInfo) {
        if (info.useCustomTabs) {
            val colorInt = try {
                Color.parseColor(info.iconColor)
            } catch (e: Exception) {
                Color.BLUE
            }
            val colorParams = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(colorInt)
                .build()

            val customTabsIntent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colorParams)
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(info.url))
        } else {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.url))
            startActivity(intent)
        }
        finish()
    }

    private fun navigateToAdmin(slotKey: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("TARGET_SLOT", slotKey)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}