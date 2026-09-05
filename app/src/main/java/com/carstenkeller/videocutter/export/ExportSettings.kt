package com.carstenkeller.videocutter.export

enum class ExportResolution(val label: String, val targetHeight: Int?) {
    ORIGINAL("Original", null),
    UHD_2160("2160p (4K)", 2160),
    FHD_1080("1080p", 1080),
    HD_720("720p", 720),
    SD_480("480p", 480),
}

enum class ExportQuality(val label: String, val videoBitrateBps: Int?) {
    ORIGINAL("Original", null),
    HIGH("Hoch", 12_000_000),
    MEDIUM("Mittel", 6_000_000),
    LOW("Niedrig", 2_500_000),
}
