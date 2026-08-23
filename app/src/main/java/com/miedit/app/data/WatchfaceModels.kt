package com.miedit.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class WidgetType {
    TEXT, IMAGE, TIME, DATE, STEPS, BATTERY, HEARTRATE, WEATHER
}

@Serializable
data class BackgroundSpec(
    val color: String = "#000000",
    val imageRef: String? = null
)

@Serializable
data class WidgetSpec(
    val id: String = UUID.randomUUID().toString(),
    val type: WidgetType = WidgetType.TEXT,
    val x: Int = 0,
    val y: Int = 0,
    val text: String = "",
    val size: Int = 40,
    val color: String = "#FFFFFF",
    val imageRef: String? = null
)

@Serializable
data class WatchfaceDesign(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled",
    val modelId: String = "band7",
    val background: BackgroundSpec = BackgroundSpec(),
    val widgets: List<WidgetSpec> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
