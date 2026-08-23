package com.miedit.app.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class WatchfaceRepository(context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val dir: File =
        File(context.filesDir, "watchfaces").apply { mkdirs() }

    fun list(): List<WatchfaceDesign> =
        dir.listFiles { f -> f.isFile && f.extension == "json" }
            ?.mapNotNull { file ->
                runCatching { json.decodeFromString<WatchfaceDesign>(file.readText()) }.getOrNull()
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()

    fun save(design: WatchfaceDesign): WatchfaceDesign {
        val stamped = design.copy(updatedAt = System.currentTimeMillis())
        File(dir, "${stamped.id}.json").writeText(json.encodeToString(stamped))
        return stamped
    }

    fun get(id: String): WatchfaceDesign? {
        val file = File(dir, "$id.json")
        if (!file.exists()) return null
        return runCatching { json.decodeFromString<WatchfaceDesign>(file.readText()) }.getOrNull()
    }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
    }
}
