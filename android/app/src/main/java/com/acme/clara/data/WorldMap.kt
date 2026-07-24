package com.acme.clara.data

import androidx.compose.ui.geometry.Offset

// Normalised (0..1) pixel position of each city on world_map_clean.png. The map asset is the
// authentic DEPART map interior recovered pixel-perfectly from original captures (median-combined
// across cases to remove labels/routes); it sits at (10,85)..(309,191) in the 320x200 screen,
// exactly where the original draws it. Lives in the data layer because travel times are
// derived from these positions (the original's flight duration depends on the distance).
object WorldMap {
    const val WV = 300f     // map interior virtual width  (native asset size, no stretch)
    const val HV = 107f     // map interior virtual height
    val pos: Map<String, Offset> = mapOf(
        "Athens" to Offset(0.5647f, 0.3268f), "Baghdad" to Offset(0.6259f, 0.3703f),
        "Bamako" to Offset(0.4708f, 0.5406f), "Bangkok" to Offset(0.7922f, 0.5324f),
        "Budapest" to Offset(0.5509f, 0.2324f), "Buenos Aires" to Offset(0.3215f, 0.7943f),
        "Cairo" to Offset(0.587f, 0.3996f), "Colombo" to Offset(0.731f, 0.5813f),
        "Istanbul" to Offset(0.5803f, 0.2976f), "Kathmandu" to Offset(0.7472f, 0.4199f),
        "Kigali" to Offset(0.5835f, 0.639f), "Lima" to Offset(0.2663f, 0.6967f),
        "London" to Offset(0.4941f, 0.1904f), "Mexico City" to Offset(0.2008f, 0.4887f),
        "Montreal" to Offset(0.2765f, 0.2529f), "Moroni" to Offset(0.6226f, 0.6949f),
        "Moscow" to Offset(0.6059f, 0.1444f), "New Delhi" to Offset(0.7232f, 0.4122f),
        "New York" to Offset(0.2752f, 0.3005f), "Oslo" to Offset(0.5263f, 0.0979f),
        "Paris" to Offset(0.5014f, 0.2184f), "Peking" to Offset(0.8393f, 0.3084f),
        "Port Moresby" to Offset(0.9304f, 0.6827f), "Reykjavik" to Offset(0.4295f, 0.0489f),
        "Rio de Janeiro" to Offset(0.3666f, 0.7491f), "Rome" to Offset(0.5315f, 0.2889f),
        "San Marino" to Offset(0.5313f, 0.2686f), "Singapore" to Offset(0.802f, 0.6184f),
        "Sydney" to Offset(0.9424f, 0.7918f), "Tokyo" to Offset(0.9081f, 0.3484f),
    )
}
