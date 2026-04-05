package com.kusa.weblauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// [FR-03] のデータ構造
data class SlotInfo(
    val label: String = "",
    val url: String = "",
    val iconColor: String = "#6200EE",
    val useCustomTabs: Boolean = true
)

val Context.dataStore by preferencesDataStore(name = "settings")

class SlotRepository(private val context: Context) {
    private val gson = Gson()

    // 各スロットの設定を取得
    fun getSlotInfo(slotKey: String): Flow<SlotInfo?> = context.dataStore.data.map { preferences ->
        val json = preferences[stringPreferencesKey(slotKey)]
        if (json != null) gson.fromJson(json, SlotInfo::class.java) else null
    }

    // スロット情報を保存
    suspend fun saveSlotInfo(slotKey: String, info: SlotInfo) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(slotKey)] = gson.toJson(info)
        }
    }
}