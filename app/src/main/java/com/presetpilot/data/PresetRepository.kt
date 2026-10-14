package com.presetpilot.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "presetpilot")

/**
 * Persists the whole library as one JSON blob in DataStore. Simple, atomic,
 * and plenty for a preset librarian.
 */
class PresetRepository(private val context: Context) {

    private object Keys {
        val LIBRARY = stringPreferencesKey("library_json")
    }

    val library: Flow<Library> = context.dataStore.data.map { p ->
        p[Keys.LIBRARY]?.let { fromJson(it) } ?: seed()
    }

    suspend fun update(transform: (Library) -> Library) {
        val current = context.dataStore.data.map { p ->
            p[Keys.LIBRARY]?.let { fromJson(it) } ?: seed()
        }.first()
        val next = transform(current)
        context.dataStore.edit { e -> e[Keys.LIBRARY] = toJson(next) }
    }

    // ---- convenience ops ----

    suspend fun addBank(bank: Bank) = update { it.copy(banks = it.banks + bank) }

    suspend fun updateBank(bank: Bank) = update { lib ->
        lib.copy(banks = lib.banks.map { if (it.id == bank.id) bank else it })
    }

    suspend fun deleteBank(bankId: String) = update { lib ->
        val doomed = lib.presets.filter { it.bankId == bankId }.map { it.id }.toSet()
        lib.copy(
            banks = lib.banks.filter { it.id != bankId },
            presets = lib.presets.filter { it.bankId != bankId },
            setlist = lib.setlist.filter { it !in doomed },
        )
    }

    suspend fun addPreset(preset: Preset) = update { it.copy(presets = it.presets + preset) }

    suspend fun updatePreset(preset: Preset) = update { lib ->
        lib.copy(presets = lib.presets.map { if (it.id == preset.id) preset else it })
    }

    suspend fun deletePreset(presetId: String) = update { lib ->
        lib.copy(
            presets = lib.presets.filter { it.id != presetId },
            setlist = lib.setlist.filter { it != presetId },
        )
    }

    suspend fun addToSetlist(presetId: String) = update { lib ->
        if (presetId in lib.setlist) lib
        else lib.copy(setlist = lib.setlist + presetId)
    }

    suspend fun removeFromSetlist(presetId: String) = update { lib ->
        lib.copy(setlist = lib.setlist.filter { it != presetId })
    }

    suspend fun moveSetlist(from: Int, to: Int) = update { lib ->
        val list = lib.setlist.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
        }
        lib.copy(setlist = list)
    }

    // ---- JSON ----

    private fun toJson(lib: Library): String {
        val root = JSONObject()
        root.put("onboardingDone", lib.onboardingDone)
        root.put("midiTarget", lib.midiTarget)
        root.put("banks", JSONArray().apply {
            lib.banks.forEach { b ->
                put(JSONObject().apply {
                    put("id", b.id); put("name", b.name)
                    put("channel", b.channel); put("msb", b.msb); put("lsb", b.lsb)
                })
            }
        })
        root.put("presets", JSONArray().apply {
            lib.presets.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("bankId", p.bankId)
                    put("name", p.name); put("program", p.program)
                })
            }
        })
        root.put("setlist", JSONArray(lib.setlist))
        return root.toString()
    }

    private fun fromJson(raw: String): Library {
        return try {
            val root = JSONObject(raw)
            Library(
                onboardingDone = root.optBoolean("onboardingDone", false),
                midiTarget = root.optString("midiTarget", "virtual"),
                banks = root.optJSONArray("banks")?.let { arr ->
                    List(arr.length()) { i ->
                        arr.getJSONObject(i).let { o ->
                            Bank(
                                id = o.getString("id"), name = o.getString("name"),
                                channel = o.optInt("channel", 0),
                                msb = o.optInt("msb", 0), lsb = o.optInt("lsb", 0),
                            )
                        }
                    }
                } ?: emptyList(),
                presets = root.optJSONArray("presets")?.let { arr ->
                    List(arr.length()) { i ->
                        arr.getJSONObject(i).let { o ->
                            Preset(
                                id = o.getString("id"), bankId = o.getString("bankId"),
                                name = o.getString("name"), program = o.optInt("program", 0),
                            )
                        }
                    }
                } ?: emptyList(),
                setlist = root.optJSONArray("setlist")?.let { arr ->
                    List(arr.length()) { arr.getString(it) }
                } ?: emptyList(),
            )
        } catch (_: Exception) {
            seed()
        }
    }

    private fun seed(): Library {
        val guitar = Bank(name = "Guitar Rig", channel = 0)
        val keys = Bank(name = "Keys", channel = 1)
        return Library(
            banks = listOf(guitar, keys),
            presets = listOf(
                Preset(bankId = guitar.id, name = "Clean", program = 0),
                Preset(bankId = guitar.id, name = "Crunch", program = 1),
                Preset(bankId = guitar.id, name = "Lead", program = 2),
                Preset(bankId = guitar.id, name = "Ambient", program = 3),
                Preset(bankId = keys.id, name = "Piano", program = 0),
                Preset(bankId = keys.id, name = "Pad", program = 1),
            ),
        )
    }
}
