#!/usr/bin/env python3
"""Generate android/.../data/CountryShapes.kt for the Passport (C4).

Source of truth: scripts/geodata/ne_110m_admin_0_countries.geojson — Natural Earth
1:110m admin-0 countries, which is public domain (CC0). Re-download with:

  curl -s -o scripts/geodata/ne_110m_admin_0_countries.geojson \\
    https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_110m_admin_0_countries.geojson

We project every country border through the SAME Mercator transform the game already
uses to place city dots (data.Expansion.project + data.WorldMap), so the filled
silhouettes land exactly on top of the raster DEPART map. Points are simplified in map-
pixel space (300x107 interior) and emitted as normalised 0..1 coords, packed one string
per country. Micro-states with no 110m polygon fall back to a stamped dot in the app.

DO NOT hand-edit the generated Kotlin — edit PLACE_COUNTRY / the tolerances here and rerun.
"""
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
GEO = os.path.join(HERE, "geodata", "ne_110m_admin_0_countries.geojson")
EXPANSION2_CARDS = os.path.join(HERE, "expansion_data", "all_cards_133.json")
OUT = os.path.join(HERE, "..", "android", "app", "src", "main",
                   "java", "com", "acme", "clara", "data", "CountryShapes.kt")

# Map interior in the 320x200 screen: 300px wide x 107px tall (see data.WorldMap).
WV, HV = 300.0, 107.0

# --- projection: identical to data.Expansion.project(lat, lon) --------------------------
def project(lat, lon):
    merc = math.log(math.tan(math.pi / 4 + math.radians(lat) / 2))
    x = 0.0029627 * lon + 0.49449
    y = -0.35748 * merc + 0.5672
    return (min(max(x, 0.0), 1.0), min(max(y, 0.0), 1.0))

# --- every game place -> the ADM0_A3 country code Natural Earth keys on ------------------
# Landmarks resolve to their containing country; several places share a country (all of
# GBR/USA/ITA... paint the one silhouette). Antarctica + micro-states have no usable 110m
# polygon and are intentionally left to the dot fallback (see MISSING report at the end).
PLACE_COUNTRY = {
    # -- original 30 (free tier) --
    "Athens": "GRC", "Baghdad": "IRQ", "Bamako": "MLI", "Bangkok": "THA",
    "Budapest": "HUN", "Buenos Aires": "ARG", "Cairo": "EGY", "Colombo": "LKA",
    "Istanbul": "TUR", "Kathmandu": "NPL", "Kigali": "RWA", "Lima": "PER",
    "London": "GBR", "Mexico City": "MEX", "Montreal": "CAN", "Moroni": "COM",
    "Moscow": "RUS", "New Delhi": "IND", "New York": "USA", "Oslo": "NOR",
    "Paris": "FRA", "Beijing": "CHN", "Port Moresby": "PNG", "Reykjavik": "ISL",
    "Rio de Janeiro": "BRA", "Rome": "ITA", "San Marino": "SMR", "Singapore": "SGP",
    "Sydney": "AUS", "Tokyo": "JPN",
    # -- paid expansion (68) --
    "Abu Simbel": "EGY", "Amsterdam": "NLD", "Antarctica": "ATA", "Bali": "IDN",
    "Berlin": "DEU", "Bhutan": "BTN", "Cambodia": "KHM", "Cape Town": "ZAF",
    "Cappadocia": "TUR", "Casablanca": "MAR", "Chichen Itza": "MEX", "Cologne": "DEU",
    "Copenhagen": "DNK", "Dubai": "ARE", "Dubrovnik": "HRV", "Easter Island": "CHL",
    "Edinburgh": "GBR", "Mount Everest": "NPL", "Grand Canyon": "USA", "Greenland": "GRL",
    "Gujarat": "IND", "Ha Long Bay": "VNM", "Hong Kong": "CHN", "Honolulu": "USA",
    "Iguazu Falls": "ARG", "Ireland": "IRL", "Jerusalem": "ISR", "Jordan": "JOR",
    "Kenya": "KEN", "Krakow": "POL", "Kuala Lumpur": "MYS", "Las Vegas": "USA",
    "Lisbon": "PRT", "Los Angeles": "USA", "Maranhao": "BRA", "Marrakech": "MAR",
    "Mauritius": "MUS", "Monaco": "MCO", "Mongolia": "MNG", "Mont Saint-Michel": "FRA",
    "Mount Rushmore": "USA", "Myanmar": "MMR", "Neuschwanstein": "DEU",
    "Northern Ireland": "GBR", "Novosibirsk": "RUS", "Palau": "PLW", "Philippines": "PHL",
    "Pisa": "ITA", "Pompeii": "ITA", "Prague": "CZE", "Saint Petersburg": "RUS",
    "Salar de Uyuni": "BOL", "Samarkand": "UZB", "San Francisco": "USA", "Santorini": "GRC",
    "Scottish Highlands": "GBR", "Seattle": "USA", "Stonehenge": "GBR", "Svalbard": "NOR",
    "Taipei": "TWN", "Toronto": "CAN", "Turkmenistan": "TKM", "Tuvalu": "TUV",
    "Valencia": "ESP", "Vatican City": "VAT", "Venice": "ITA", "Vienna": "AUT",
    "Washington DC": "USA",
}

# Human-readable label per country (for the Passport list / accessibility).
COUNTRY_NAME = {}

# ADM0_A3 codes we never want a filled silhouette for, even if a polygon exists:
# Antarctica clamps to an ugly full-width bottom band under Mercator -> dot fallback.
FORCE_DOT = {"ATA"}

# Natural Earth 1:110m omits these small island/micro states. Their standard alpha-3 codes keep
# Passport identity stable; the representative lat/lon gives each one a real map/flight dot.
MICRO_ALPHA3 = {
    "AD": "AND", "AG": "ATG", "BB": "BRB", "BH": "BHR", "CV": "CPV",
    "DM": "DMA", "FM": "FSM", "GD": "GRD", "KI": "KIR", "KN": "KNA",
    "LC": "LCA", "LI": "LIE", "MH": "MHL", "MT": "MLT", "MV": "MDV",
    "NR": "NRU", "SC": "SYC", "ST": "STP", "TO": "TON", "VC": "VCT",
    "WS": "WSM",
}
MICRO_LAT_LON = {
    "AD": (42.51, 1.60), "AG": (17.06, -61.80), "BB": (13.19, -59.54),
    "BH": (26.07, 50.55), "CV": (15.12, -23.61), "DM": (15.42, -61.36),
    "FM": (6.92, 158.19), "GD": (12.12, -61.68), "KI": (1.87, -157.36),
    "KN": (17.36, -62.78), "LC": (13.91, -60.98), "LI": (47.14, 9.55),
    "MH": (7.13, 171.18), "MT": (35.90, 14.45), "MV": (3.20, 73.22),
    "NR": (-0.52, 166.93), "SC": (-4.68, 55.49), "ST": (0.19, 6.61),
    "TO": (-21.18, -175.20), "VC": (13.25, -61.20), "WS": (-13.76, -172.10),
}


def emoji_alpha2(flag):
    return "".join(chr(ord(ch) - 127397) for ch in flag)


def douglas_peucker(pts, tol):
    """Simplify a polyline (pixel space) with Ramer-Douglas-Peucker."""
    if len(pts) < 3:
        return pts[:]
    dmax, idx = 0.0, 0
    ax, ay = pts[0]
    bx, by = pts[-1]
    dx, dy = bx - ax, by - ay
    seg2 = dx * dx + dy * dy
    for i in range(1, len(pts) - 1):
        px, py = pts[i]
        if seg2 == 0:
            d = math.hypot(px - ax, py - ay)
        else:
            t = ((px - ax) * dx + (py - ay) * dy) / seg2
            t = min(max(t, 0.0), 1.0)
            d = math.hypot(px - (ax + t * dx), py - (ay + t * dy))
        if d > dmax:
            dmax, idx = d, i
    if dmax > tol:
        left = douglas_peucker(pts[:idx + 1], tol)
        right = douglas_peucker(pts[idx:], tol)
        return left[:-1] + right
    return [pts[0], pts[-1]]


def ring_area_px(ring):
    a = 0.0
    for i in range(len(ring)):
        x0, y0 = ring[i]
        x1, y1 = ring[(i + 1) % len(ring)]
        a += x0 * y1 - x1 * y0
    return abs(a) / 2.0


TOL_PX = 0.9          # RDP tolerance in map pixels (300x107 interior)
MIN_RING_AREA_PX = 1.4  # drop specks/tiny islands smaller than this (px^2)


def polygons_of(geom):
    """Yield each outer ring's lon/lat coords from a Polygon/MultiPolygon."""
    t = geom["type"]
    if t == "Polygon":
        yield geom["coordinates"][0]
    elif t == "MultiPolygon":
        for poly in geom["coordinates"]:
            yield poly[0]


def build_country(feature):
    rings_out = []
    for lonlat_ring in polygons_of(feature["geometry"]):
        # project to normalised 0..1, then to map-pixel space for simplification
        proj = []
        for lon, lat in lonlat_ring:
            x, y = project(lat, lon)
            proj.append((x * WV, y * HV))
        # drop consecutive duplicates
        dedup = [proj[0]]
        for p in proj[1:]:
            if p != dedup[-1]:
                dedup.append(p)
        if len(dedup) < 4:
            continue
        simp = douglas_peucker(dedup, TOL_PX)
        if len(simp) < 3 or ring_area_px(simp) < MIN_RING_AREA_PX:
            continue
        rings_out.append(simp)
    return rings_out


def encode(rings):
    """rings -> "x,y x,y;x,y ..." with 3-decimal normalised coords."""
    parts = []
    for ring in rings:
        pts = " ".join(f"{px / WV:.3f},{py / HV:.3f}" for px, py in ring)
        parts.append(pts)
    return ";".join(parts)


def main():
    data = json.load(open(GEO))
    by_code = {}
    by_alpha2 = {}
    for f in data["features"]:
        code = f["properties"].get("ADM0_A3")
        if code:
            by_code[code] = f
        alpha2 = f["properties"].get("ISO_A2_EH")
        if alpha2 and alpha2 != "-99":
            by_alpha2[alpha2] = f

    # Expansion2 is country-based, so Natural Earth's label points are the appropriate travel/map
    # positions. This supplies all 133 destinations from the same geographic source as the shapes.
    place_positions = {}
    for card in json.load(open(EXPANSION2_CARDS)):
        name = card["name"]
        alpha2 = emoji_alpha2(card["emoji"])
        feature = by_alpha2.get(alpha2)
        if feature:
            props = feature["properties"]
            code = props["ADM0_A3"]
            lat_lon = (props["LABEL_Y"], props["LABEL_X"])
        else:
            code = MICRO_ALPHA3.get(alpha2)
            lat_lon = MICRO_LAT_LON.get(alpha2)
        if not code or not lat_lon:
            raise ValueError(f"No Passport/map identity for {name} ({alpha2})")
        PLACE_COUNTRY[name] = code
        COUNTRY_NAME[code] = name
        place_positions[name] = project(*lat_lon)

    needed = sorted(set(PLACE_COUNTRY.values()))
    shapes = {}
    missing = []
    for code in needed:
        if code in FORCE_DOT:
            missing.append(code)
            continue
        f = by_code.get(code)
        if not f:
            missing.append(code)
            continue
        COUNTRY_NAME[code] = f["properties"].get("NAME") or f["properties"].get("ADMIN") or code
        rings = build_country(f)
        if not rings:
            missing.append(code)
            continue
        shapes[code] = rings

    # fill names for the dot-fallback codes too (best effort)
    for code in missing:
        if code not in COUNTRY_NAME:
            f = by_code.get(code)
            COUNTRY_NAME[code] = (f["properties"].get("NAME") if f else None) or code

    total_pts = sum(len(r) for rings in shapes.values() for r in rings)
    print(f"countries needed={len(needed)} with-silhouette={len(shapes)} "
          f"dot-fallback={len(missing)} total-points={total_pts}")
    print("dot-fallback codes:", ", ".join(sorted(missing)))

    lines = []
    lines.append("// AUTO-GENERATED by scripts/gen_country_shapes.py from Natural Earth 1:110m")
    lines.append("// admin-0 countries (public domain / CC0). DO NOT EDIT BY HAND — edit the")
    lines.append("// script's PLACE_COUNTRY map / tolerances and regenerate.")
    lines.append("package com.acme.clara.data")
    lines.append("")
    lines.append("import androidx.compose.ui.geometry.Offset")
    lines.append("")
    lines.append("/**")
    lines.append(" * Passport (C4) country silhouettes, projected through the same Mercator transform")
    lines.append(" * as [Expansion.project]/[WorldMap] so they overlay the raster DEPART map. Each")
    lines.append(" * country is a list of filled rings (islands); coords are normalised 0..1 in the")
    lines.append(" * map interior. Codes in [dotFallback] have no usable 110m polygon (micro-states,")
    lines.append(" * Antarctica) and are stamped at the visited place's map dot instead.")
    lines.append(" */")
    lines.append("object CountryShapes {")
    lines.append("    /** Every game place -> its ADM0_A3 country code. */")
    lines.append("    val placeCountry: Map<String, String> = mapOf(")
    for place in sorted(PLACE_COUNTRY):
        lines.append(f"        {kstr(place)} to {kstr(PLACE_COUNTRY[place])},")
    lines.append("    )")
    lines.append("")
    lines.append("    /** Representative map position for every country destination in Expansion2. */")
    lines.append("    val placePosition: Map<String, Offset> = mapOf(")
    for place in sorted(place_positions):
        x, y = place_positions[place]
        lines.append(f"        {kstr(place)} to Offset({x:.4f}f, {y:.4f}f),")
    lines.append("    )")
    lines.append("")
    lines.append("    /** ADM0_A3 -> display name. */")
    lines.append("    val countryName: Map<String, String> = mapOf(")
    for code in sorted(COUNTRY_NAME):
        lines.append(f"        {kstr(code)} to {kstr(COUNTRY_NAME[code])},")
    lines.append("    )")
    lines.append("")
    lines.append("    /** Countries with no silhouette — stamp the visited place's dot instead. */")
    lines.append("    val dotFallback: Set<String> = setOf(" +
                 ", ".join(kstr(c) for c in sorted(missing)) + ")")
    lines.append("")
    lines.append("    /** Whether this destination's Passport page is open at the current campaign rank. */")
    lines.append("    fun isPlaceUnlocked(place: String, rankIndex: Int, entitled: Boolean): Boolean {")
    lines.append("        val wave = Progression.wave[place] ?: return true")
    lines.append("        return entitled && wave <= Progression.unlockedMaxWave(rankIndex)")
    lines.append("    }")
    lines.append("")
    lines.append("    /** Countries still frosted in the Passport. A free/unlocked destination in a shared")
    lines.append("     * country opens that silhouette even if another landmark there belongs to a later wave. */")
    lines.append("    fun lockedCountryCodes(rankIndex: Int, entitled: Boolean): Set<String> {")
    lines.append("        val unlocked = placeCountry.entries.filter {")
    lines.append("            isPlaceUnlocked(it.key, rankIndex, entitled)")
    lines.append("        }.mapTo(hashSetOf()) { it.value }")
    lines.append("        return placeCountry.values.toSet() - unlocked")
    lines.append("    }")
    lines.append("")
    lines.append("    // packed silhouettes: \"x,y x,y;...\" rings, decoded lazily into Offsets.")
    lines.append("    private val packed: Map<String, String> = mapOf(")
    for code in sorted(shapes):
        lines.append(f"        {kstr(code)} to")
        lines.append(f"            {kstr(encode(shapes[code]))},")
    lines.append("    )")
    lines.append("")
    lines.append("    private val cache = HashMap<String, List<List<Offset>>>()")
    lines.append("")
    lines.append("    /** Filled rings (normalised 0..1) for a country, or empty if none. */")
    lines.append("    fun rings(code: String): List<List<Offset>> =")
    lines.append("        cache.getOrPut(code) {")
    lines.append("            val p = packed[code] ?: return@getOrPut emptyList()")
    lines.append("            p.split(';').map { ring ->")
    lines.append("                ring.split(' ').map { pt ->")
    lines.append("                    val c = pt.split(',')")
    lines.append("                    Offset(c[0].toFloat(), c[1].toFloat())")
    lines.append("                }")
    lines.append("            }")
    lines.append("        }")
    lines.append("}")

    with open(OUT, "w") as fh:
        fh.write("\n".join(lines) + "\n")
    print("wrote", os.path.relpath(OUT, HERE))


def kstr(s):
    return '"' + str(s).replace("\\", "\\\\").replace('"', '\\"') + '"'


if __name__ == "__main__":
    main()
