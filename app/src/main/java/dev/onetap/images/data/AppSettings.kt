package dev.onetap.images.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.settingsStore by preferencesDataStore("onetap-settings")

data class Preferences(
    val autoDownload: Boolean = false,
    val skipExisting: Boolean = true,
    val concurrency: Int = 2,
    val folder: String = "Pictures/OneTap Images",
)

data class SavedRecord(val key: String, val uri: String)

class AppSettings(private val context: Context) {
    private val auto = booleanPreferencesKey("auto")
    private val skip = booleanPreferencesKey("skip")
    private val concurrent = intPreferencesKey("concurrent")
    private val folder = stringPreferencesKey("folder")
    private val history = stringPreferencesKey("history")
    val prefs: Flow<Preferences> = context.settingsStore.data.map {
        Preferences(it[auto] ?: false, it[skip] ?: true,
            (it[concurrent] ?: 2).coerceIn(1, 3),
            it[folder] ?: "Pictures/OneTap Images")
    }
    suspend fun update(p: Preferences) {
        context.settingsStore.edit {
            it[auto] = p.autoDownload
            it[skip] = p.skipExisting
            it[concurrent] = p.concurrency.coerceIn(1, 3)
            it[folder] = if (p.folder == "Pictures/Paperize") p.folder else "Pictures/OneTap Images"
        }
    }
    suspend fun snapshot(): Preferences = prefs.first()
    suspend fun records(): List<SavedRecord> = parseHistory(context.settingsStore.data.first()[history])
    suspend fun add(key: String, uri: String) {
        context.settingsStore.edit { prefs ->
            val previous = parseHistory(prefs[history]).filterNot { it.key == key }
            val arr = JSONArray()
            (listOf(SavedRecord(key, uri)) + previous).take(2000).forEach { record ->
                arr.put(JSONObject().put("key", record.key).put("uri", record.uri))
            }
            prefs[history] = arr.toString()
        }
    }
    private fun parseHistory(text: String?): List<SavedRecord> = try {
        if (text.isNullOrBlank()) emptyList() else buildList {
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val row = arr.optJSONObject(i) ?: continue
                add(SavedRecord(row.getString("key"), row.getString("uri")))
            }
        }
    } catch (_: Exception) { emptyList() }
}
