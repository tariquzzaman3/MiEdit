package com.miedit.app.data

data class DeviceProfile(
    val id: String,
    val displayName: String,
    val width: Int,
    val height: Int,
    val aliases: List<String> = emptyList()
)

data class DetectedDevice(
    val name: String,
    val address: String,
    val profile: DeviceProfile?,
    val firmwareVersion: String = "Unknown",
    val region: String = "Unknown"
)

object DeviceProfiles {

    val all = listOf(
        DeviceProfile("band3", "Mi Band 3", 80, 160, listOf("mi smart band 3")),
        DeviceProfile("band4", "Mi Band 4", 120, 240, listOf("mi smart band 4")),
        DeviceProfile("band5", "Mi Band 5", 126, 294, listOf("mi smart band 5")),
        DeviceProfile("band6", "Mi Band 6", 152, 486, listOf("mi smart band 6")),
        DeviceProfile("band7", "Mi Band 7", 192, 490, listOf("mi smart band 7")),
        DeviceProfile("band7pro", "Smart Band 7 Pro", 280, 456, listOf("smart band 7 pro")),
        DeviceProfile("band8", "Mi Band 8", 192, 490, listOf("mi smart band 8")),
        DeviceProfile("band8pro", "Smart Band 8 Pro", 336, 480, listOf("smart band 8 pro")),
        DeviceProfile("band9", "Smart Band 9", 192, 490, listOf("smart band 9")),
        DeviceProfile("band9pro", "Smart Band 9 Pro", 336, 480, listOf("smart band 9 pro"))
    )

    fun byId(id: String): DeviceProfile =
        all.firstOrNull { it.id == id } ?: all.first { it.id == "band7" }

    /** Matches an advertised Bluetooth name like "Mi Smart Band 7" to a profile. */
    fun detectFromName(name: String): DeviceProfile? {
        val n = name.trim().lowercase()
        return all.sortedByDescending { it.displayName.length }
            .firstOrNull { p ->
                (p.aliases + p.displayName).any { alias -> n.contains(alias.lowercase()) }
            }
    }
}
