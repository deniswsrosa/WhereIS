#!/usr/bin/env python3
"""Generate android/.../data/Expansion.kt from the approved review set.

Source of truth: scratchpad/expansion-review.html, exported to cities.json / tips.json /
venues.json (see the extraction node one-liner in the build notes). City clue text is
hand-authored and human-reviewed. Re-run after editing the review source; do not hand-edit
the generated Kotlin.

Authored tips are written female-leaning ("boasted she'd...", "rolling her R's"). The game's
witness clues must match the culprit's sex, so we rewrite she/her -> pronoun slots ({s}/{S}/{p})
that ClaraViewModel.pronouns() fills at runtime, exactly like the original's trait clues.
"""
import json
import re
import os

HERE = os.path.dirname(os.path.abspath(__file__))
SCRATCH = os.environ["SCRATCH"]
OUT = os.path.join(HERE, "..", "android", "app", "src", "main", "java", "com", "acme", "clara", "data", "Expansion.kt")

cities = json.load(open(os.path.join(SCRATCH, "cities.json")))
tips = json.load(open(os.path.join(SCRATCH, "tips.json")))
venues_raw = json.load(open(os.path.join(SCRATCH, "venues.json")))

# --- real coordinates (lat, lon), representative point per destination ------------------
LATLON = {
    "abu_simbel": (22.34, 31.63), "amsterdam": (52.37, 4.90), "antarctica": (-75.0, 0.0),
    "bali": (-8.34, 115.09), "berlin": (52.52, 13.40), "bhutan": (27.47, 89.64),
    "cambodia": (13.36, 103.86), "cape_town": (-33.92, 18.42), "cappadocia": (38.65, 34.83),
    "casablanca": (33.57, -7.59), "chichen_itza": (20.68, -88.57), "cologne": (50.94, 6.96),
    "copenhagen": (55.68, 12.57), "dubai": (25.20, 55.27), "dubrovnik": (42.65, 18.09),
    "easter_island": (-27.11, -109.35), "edinburgh": (55.95, -3.19), "everest": (27.99, 86.93),
    "grand_canyon": (36.06, -112.14), "greenland": (64.18, -51.72), "gujarat": (22.30, 71.80),
    "ha_long_bay": (20.91, 107.18), "hong_kong": (22.32, 114.17), "honolulu": (21.31, -157.86),
    "iguazu_falls": (-25.69, -54.44), "ireland": (53.35, -7.50), "jerusalem": (31.77, 35.21),
    "jordan": (30.33, 35.44), "kenya": (-1.29, 36.82), "krakow": (50.06, 19.94),
    "kuala_lumpur": (3.14, 101.69), "las_vegas": (36.17, -115.14), "lisbon": (38.72, -9.14),
    "los_angeles": (34.05, -118.24), "maranhao": (-2.53, -43.50), "marrakech": (31.63, -7.99),
    "mauritius": (-20.35, 57.55), "monaco": (43.74, 7.42), "mongolia": (47.89, 106.91),
    "mont_saint_michel": (48.64, -1.51), "mount_rushmore": (43.88, -103.46),
    "myanmar": (21.17, 94.86), "neuschwanstein": (47.56, 10.75), "northern_ireland": (54.60, -6.20),
    "novosibirsk": (55.01, 82.93), "palau": (7.51, 134.58), "philippines": (12.88, 121.77),
    "pisa": (43.72, 10.40), "pompeii": (40.75, 14.49), "prague": (50.08, 14.44),
    "saint_petersburg": (59.94, 30.34), "salar_de_uyuni": (-20.13, -67.49),
    "samarkand": (39.63, 66.98), "san_francisco": (37.77, -122.42), "santorini": (36.39, 25.46),
    "scottish_highlands": (57.12, -4.70), "seattle": (47.61, -122.33), "stonehenge": (51.18, -1.83),
    "svalbard": (78.22, 15.65), "taipei": (25.03, 121.57), "toronto": (43.65, -79.38),
    "turkmenistan": (40.10, 58.44), "tuvalu": (-8.52, 179.20), "valencia": (39.47, -0.38),
    "vatican_city": (41.90, 12.45), "venice": (45.44, 12.32), "vienna": (48.21, 16.37),
    "washington_dc": (38.91, -77.04),
}

# structured attributes pulled from the approved clues (for the almanac + clue templates)
GREETING = {
    "bali": "ohm swah-stee-AH-stoo", "berlin": "GOO-ten tahk", "cambodia": "soo-s'DYE",
    "cappadocia": "mehr-hah-BAH", "copenhagen": "goh-DAH", "jerusalem": "shah-LOHM",
    "kenya": "JAHM-boh", "krakow": "jen DOH-brih", "mont_saint_michel": "bohn-ZHOOR",
    "myanmar": "min-gah-lah-BAH", "novosibirsk": "pree-VYET", "pisa": "bwohn-JOR-noh",
    "prague": "DOH-bree den", "saint_petersburg": "pree-VYET", "salar_de_uyuni": "OH-lah",
    "santorini": "YAH-sas", "turkmenistan": "sah-LAHM", "venice": "bwohn-JOR-noh",
}
CURRENCY = {
    "cape_town": "rand", "gujarat": "rupees", "krakow": "zloty", "kuala_lumpur": "ringgit",
    "marrakech": "dirham", "novosibirsk": "rubles", "philippines": "pesos",
}


def neutralize(t):
    """Rewrite female-specific words to pronoun slots the runtime fills for the culprit's sex."""
    t = t.replace("she'd", "{s}'d").replace("She'd", "{S}'d")
    t = t.replace("she'll", "{s}'ll").replace("she's", "{s}'s")
    t = re.sub(r"\bShe\b", "{S}", t)
    t = re.sub(r"\bshe\b", "{s}", t)
    t = re.sub(r"\bherself\b", "{p}self", t)
    t = re.sub(r"\bhers\b", "{p}s", t)
    t = re.sub(r"\bher\b", "{p}", t)
    return t


def kesc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")


# --- venues: exclude Riverfront (already a base venue). Split new vs reused witnesses -----
NEW_WITNESS = {}          # venue -> new occupation
REUSED_WITNESS = {}       # venue -> existing occupation (placeholder, swap in one line)
VENUE_ORDER = []
for name, occ, note in venues_raw:
    if name == "Riverfront":
        continue
    VENUE_ORDER.append(name)
    if note.startswith("yes"):
        NEW_WITNESS[name] = occ
    else:
        REUSED_WITNESS[name] = occ.replace(" (reused)", "")

# per-city venue bias (multiplicity = weight); ClaraViewModel draws 3 distinct venues from it.
AFFINITY = {
    "Las Vegas": ["Casino", "Casino", "Hotel"],
    "Monaco": ["Casino", "Harbor"],
    "Saint Petersburg": ["Palace", "Museum"],
    "Vienna": ["Opera House", "Palace"],
    "Novosibirsk": ["Railway Station", "Railway Station"],
    "Vatican City": ["Museum", "Palace"],
    "Dubai": ["Currency Exchange", "Hotel", "Bank"],
    "Los Angeles": ["Television Station", "Radio Station"],
    "Cologne": ["University", "Marketplace"],
    "Prague": ["Opera House", "University"],
    "Toronto": ["Newspaper Office", "Television Station"],
    "Seattle": ["Radio Station", "Restaurant / Café"],
    "Washington DC": ["Courthouse", "Newspaper Office"],
    "Marrakech": ["Marketplace", "Antique Shop"],
    "Hong Kong": ["Currency Exchange", "Harbor"],
    "San Francisco": ["Harbor", "University"],
}

# --------------------------------------------------------------------------------------------
lines = []
lines.append("// AUTO-GENERATED from the approved review set by scripts/gen_expansion.py.")
lines.append("// City clue text is hand-authored and human-reviewed. DO NOT EDIT BY HAND —")
lines.append("// edit the review source (scratchpad/expansion-review.html) and regenerate.")
lines.append("package com.acme.clara.data")
lines.append("")
lines.append("import androidx.compose.ui.geometry.Offset")
lines.append("import kotlin.math.PI")
lines.append("import kotlin.math.ln")
lines.append("import kotlin.math.tan")
lines.append("")
lines.append("/**")
lines.append(" * Paid-tier expansion: 68 new destinations + new venues, gated by GameState.expansionUnlocked.")
lines.append(" * The free game ships the original 30; unlocking merges these in for case routes, decoys,")
lines.append(" * map dots and flight times. Witnesses for the new venues reuse existing occupations via the")
lines.append(" * one-line placeholders in [venueOccupations] until bespoke sprites are drawn.")
lines.append(" */")
lines.append("object Expansion {")
lines.append("    // Map dot from real (lat, lon), calibrated to the base map's own hand-placed dots:")
lines.append("    //   x = 0.0029627*lon + 0.49449 (fits London/NY/Lima/Sydney to ~0.001)")
lines.append("    //   y = -0.35748*mercator(lat) + 0.5672 (Mercator; fits to ~0.03)")
lines.append("    private fun project(lat: Double, lon: Double): Offset {")
lines.append("        val merc = ln(tan(PI / 4 + Math.toRadians(lat) / 2))")
lines.append("        val x = (0.0029627 * lon + 0.49449).coerceIn(0.0, 1.0)")
lines.append("        val y = (-0.35748 * merc + 0.5672).coerceIn(0.0, 1.0)")
lines.append("        return Offset(x.toFloat(), y.toFloat())")
lines.append("    }")
lines.append("")
lines.append("    // Real coordinates (lat, lon) kept precise so distance-scaled flight times stay sensible.")
lines.append("    val latLon: Map<String, Pair<Double, Double>> = mapOf(")
for c in cities:
    lat, lon = LATLON[c["slug"]]
    lines.append('        "%s" to (%s to %s),' % (kesc(c["name"]), lat, lon))
lines.append("    )")
lines.append("")
lines.append("    val cities: List<CityInfo> = listOf(")
for c in cities:
    slug = c["slug"]
    clue_list = ", ".join('"%s"' % kesc(neutralize(t)) for t in tips[slug])
    greeting = GREETING.get(slug)
    currency = CURRENCY.get(slug)
    extra = ", clues = listOf(%s)" % clue_list
    if greeting:
        extra += ', greeting = "%s"' % kesc(greeting)
    if currency:
        extra += ', currency = "%s"' % kesc(currency)
    lines.append('        CityInfo("%s", "%s", "%s",' % (kesc(c["name"]), kesc(c["region"]), kesc(c["landmark"])))
    lines.append('            "%s", false%s),' % (kesc(c["description"]), extra))
lines.append("    )")
lines.append("")
lines.append("    val byName: Map<String, CityInfo> = cities.associateBy { it.name }")
lines.append("    val names: List<String> = cities.map { it.name }")
lines.append("    val pos: Map<String, Offset> = latLon.mapValues { (_, ll) -> project(ll.first, ll.second) }")
lines.append("")
lines.append("    // New venues (Riverfront already exists in the base set, so it is not repeated here).")
lines.append("    val venues: List<String> = listOf(")
for v in VENUE_ORDER:
    lines.append('        "%s",' % kesc(v))
lines.append("    )")
lines.append("")
lines.append("    // Bespoke witnesses drawn for the expansion (need their own sprites eventually).")
lines.append("    val newWitnesses: List<String> = listOf(%s)" % ", ".join('"%s"' % kesc(o) for o in NEW_WITNESS.values()))
lines.append("")
lines.append("    // Which witness staffs each new venue. Reused venues point at an existing occupation —")
lines.append("    // swap any single line here to give that venue its own witness later.")
lines.append("    val venueOccupations: Map<String, List<String>> = mapOf(")
for v in VENUE_ORDER:
    occ = NEW_WITNESS.get(v) or REUSED_WITNESS.get(v)
    lines.append('        "%s" to listOf("%s"),' % (kesc(v), kesc(occ)))
lines.append("    )")
lines.append("")
lines.append("    // Per-venue apology shown when the suspect never passed through (wrong-city / off-track).")
lines.append("    val noInformationByVenue: Map<String, String> = mapOf(")
NOINFO = {
    "Casino": "No high-roller like that has played a hand here.",
    "Railway Station": "No one matching that description boarded a train here.",
    "Radio Station": "Nobody like that has been on air or in the booth.",
    "Opera House": "No such person has taken a seat for tonight's performance.",
    "University": "I haven't seen anyone like that around the campus.",
    "Antique Shop": "No one like that has browsed my antiques today.",
    "Courthouse": "I'm sorry, I have never seen the person you are looking for.",
    "Currency Exchange": "No one like that has changed money at my window.",
    "Hospital": "No patient or visitor like that has come through.",
    "Newspaper Office": "No one matching that has come by with a story.",
    "Police Headquarters": "We have no report on anyone like that.",
    "Power Plant": "No one like that is cleared to be on the plant floor.",
    "Prison": "No one like that has visited or been booked here.",
    "Research Laboratory": "No one like that has signed into the lab.",
    "Restaurant / Café": "No one like that has dined here today.",
    "Television Station": "Nobody like that has been in the studio.",
}
for v in VENUE_ORDER:
    lines.append('        "%s" to "%s",' % (kesc(v), kesc(NOINFO[v])))
lines.append("    )")
lines.append("")
lines.append("    // Per-city venue bias: characteristic venues appear more often (Las Vegas -> the casino).")
lines.append("    // A weight of N means the venue is listed N times; the game still shows 3 distinct venues.")
lines.append("    val cityVenueAffinity: Map<String, List<String>> = mapOf(")
for city, pool in AFFINITY.items():
    lines.append('        "%s" to listOf(%s),' % (kesc(city), ", ".join('"%s"' % kesc(p) for p in pool)))
lines.append("    )")
lines.append("}")

open(OUT, "w").write("\n".join(lines) + "\n")
print("wrote", os.path.normpath(OUT))
print("cities", len(cities), "venues", len(VENUE_ORDER), "new witnesses", len(NEW_WITNESS),
      "affinity", len(AFFINITY))
