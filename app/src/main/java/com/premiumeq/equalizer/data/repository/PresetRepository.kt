package com.premiumeq.equalizer.data.repository

import android.content.Context
import com.premiumeq.equalizer.data.model.EqualizerPreset
import com.premiumeq.equalizer.data.model.PresetBackup
import com.premiumeq.equalizer.data.model.PresetFolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Presets and folders are persisted as a single JSON document under the app's
 * private files directory. This is intentionally simple (no Room/SQLite) because
 * the data set is small (a list of named configs, not a large relational table)
 * and it makes export/import/share trivial: the on-disk format IS the share format.
 *
 * The in-memory [StateFlow]s are the source of truth for the UI; every mutation
 * updates memory first, then persists to disk on an IO dispatcher.
 */
@Singleton
class PresetRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storeFile: File
        get() = File(context.filesDir, "presets/presets_store.json")

    private val mutex = Mutex()

    private val _presets = MutableStateFlow<List<EqualizerPreset>>(emptyList())
    val presets: StateFlow<List<EqualizerPreset>> = _presets.asStateFlow()

    private val _folders = MutableStateFlow<List<PresetFolder>>(emptyList())
    val folders: StateFlow<List<PresetFolder>> = _folders.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!storeFile.exists()) return@withLock
            runCatching {
                val backup = json.decodeFromString(PresetBackup.serializer(), storeFile.readText())
                _presets.value = backup.presets
                _folders.value = backup.folders
            }
        }
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        mutex.withLock {
            storeFile.parentFile?.mkdirs()
            val backup = PresetBackup(presets = _presets.value, folders = _folders.value)
            storeFile.writeText(json.encodeToString(backup))
        }
    }

    suspend fun savePreset(preset: EqualizerPreset) {
        val updated = _presets.value.filterNot { it.id == preset.id } + preset
        _presets.value = updated.sortedByDescending { it.updatedAtEpochMillis }
        persist()
    }

    suspend fun deletePreset(presetId: String) {
        _presets.value = _presets.value.filterNot { it.id == presetId }
        persist()
    }

    suspend fun renamePreset(presetId: String, newName: String) {
        _presets.value = _presets.value.map {
            if (it.id == presetId) it.copy(name = newName, updatedAtEpochMillis = System.currentTimeMillis()) else it
        }
        persist()
    }

    suspend fun duplicatePreset(presetId: String): EqualizerPreset? {
        val original = _presets.value.find { it.id == presetId } ?: return null
        val copy = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${original.name} (Copy)",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis()
        )
        savePreset(copy)
        return copy
    }

    suspend fun setFavorite(presetId: String, favorite: Boolean) {
        _presets.value = _presets.value.map {
            if (it.id == presetId) it.copy(isFavorite = favorite) else it
        }
        persist()
    }

    suspend fun moveToFolder(presetId: String, folderId: String?) {
        _presets.value = _presets.value.map {
            if (it.id == presetId) it.copy(folderId = folderId) else it
        }
        persist()
    }

    suspend fun createFolder(name: String): PresetFolder {
        val folder = PresetFolder(name = name)
        _folders.value = _folders.value + folder
        persist()
        return folder
    }

    suspend fun deleteFolder(folderId: String, alsoDeletePresets: Boolean) {
        _folders.value = _folders.value.filterNot { it.id == folderId }
        _presets.value = if (alsoDeletePresets) {
            _presets.value.filterNot { it.folderId == folderId }
        } else {
            _presets.value.map { if (it.folderId == folderId) it.copy(folderId = null) else it }
        }
        persist()
    }

    /** Serializes the given presets (or all, if none specified) to a JSON string for export/sharing. */
    fun exportToJson(presetIds: Set<String>? = null): String {
        val toExport = if (presetIds == null) {
            _presets.value
        } else {
            _presets.value.filter { it.id in presetIds }
        }
        val relevantFolderIds = toExport.mapNotNull { it.folderId }.toSet()
        val backup = PresetBackup(
            presets = toExport,
            folders = _folders.value.filter { it.id in relevantFolderIds }
        )
        return json.encodeToString(backup)
    }

    /**
     * Parses a JSON export and merges it into the current library. New IDs are
     * generated for imported presets to avoid clobbering an existing preset that
     * happens to share an id (e.g. re-importing your own export).
     */
    suspend fun importFromJson(jsonText: String): Result<Int> = runCatching {
        val backup = json.decodeFromString(PresetBackup.serializer(), jsonText)
        val idRemap = mutableMapOf<String, String>()

        val newFolders = backup.folders.map { folder ->
            val newFolder = folder.copy(id = java.util.UUID.randomUUID().toString())
            idRemap[folder.id] = newFolder.id
            newFolder
        }
        _folders.value = _folders.value + newFolders

        val newPresets = backup.presets.map { preset ->
            preset.copy(
                id = java.util.UUID.randomUUID().toString(),
                folderId = preset.folderId?.let { idRemap[it] }
            )
        }
        _presets.value = _presets.value + newPresets
        persist()
        newPresets.size
    }
}
