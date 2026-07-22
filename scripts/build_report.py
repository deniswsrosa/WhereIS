#!/usr/bin/env python3
"""Build curated structured views + markdown report + game_data.json from carmen_corpus.json."""
import json, os, collections
BASE=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT=os.path.join(BASE,"corpus")
c=json.load(open(os.path.join(OUT,"carmen_corpus.json")))

by_sec=collections.OrderedDict()
for r in c["exe_strings"]:
    by_sec.setdefault(r["section"],[]).append(r)

# --- structured suspect dossiers: section 'suspect-dossier', 8 fields each ---
dfields=["name","sex","occupation","hobby","hair","auto","feature_1","feature_2"]
dvals=[r["canonical_text"] for r in by_sec.get("suspect-dossier",[])]
suspects=[]
for i in range(0,len(dvals),8):
    chunk=dvals[i:i+8]
    if len(chunk)==8:
        suspects.append(dict(zip(dfields,chunk)))

# --- placeholder inventory across templates ---
import re
placeholders=collections.Counter()
templates=[]
for r in c["exe_strings"]:
    if r["kind"]=="template":
        for m in re.findall(r'%\-?\d*\.?\d*F?[sdc]', r["canonical_text"]):
            placeholders[m]+=1
        templates.append(r)

# --- curated game_data.json for the remake ---
def sec_texts(name): return [r["canonical_text"] for r in by_sec.get(name,[])]
game={
 "edition": c["provenance"]["edition"],
 "cities":[x["canonical_text"] for x in c["cities"]],
 "suspects": suspects,
 "hobbies":["tennis","music","mt. climbing","skydiving","swimming","croquet"],
 "hair_colors":["brown","blond","black"],
 "features":["limps","ring","tattoo","scar","jewelry"],
 "vehicles":["convertible","limousine","race car","motorcycle"],
 "sexes":["male","female"],
 "venues": sec_texts("venue"),
 "occupations": sec_texts("occupation"),
 "no_information_responses": sec_texts("no-information-response"),
 "clue_lead_ins": sec_texts("clue-lead-in"),
 "witness_clue_fragments": sec_texts("witness-clue-fragment"),
 "danger_messages": sec_texts("danger-message"),
 "ranks": sec_texts("rank"),
 "roster_names": sec_texts("roster-name"),
 "menus": sec_texts("menu"),
 "crime_computer_labels": sec_texts("crime-computer-label"),
 "dossier_field_labels": sec_texts("dossier-field-label"),
 "case_flow_templates":[r["canonical_text"] for r in by_sec.get("case-flow",[])],
 "signon_interpol":[r["canonical_text"] for r in by_sec.get("signon-interpol",[])],
 "pronoun_tokens": sec_texts("pronoun-token"),
 "placeholder_inventory": dict(placeholders),
}
json.dump(game, open(os.path.join(OUT,"game_data.json"),"w"), indent=2, ensure_ascii=False)

# --- markdown report ---
L=[]
P=L.append
ed=c["provenance"]["edition"]
P("# Where in the World is Carmen Sandiego? (Enhanced) — Extracted Corpus\n")
P("## Edition (pinned from the binary)\n")
P(f"- **Title:** {ed['title']}")
P(f"- **In-binary version string:** `{ed['in_binary_version_string']}`")
P(f"- **In-binary copyright:** `{ed['in_binary_copyright']}`")
P(f"- **Compressor:** `{ed['compressor']}`")
P(f"- **Source:** [{ed['archive_identifier']}]({ed['archive_url']})")
P(f"- **Note:** {ed['note']}\n")
P("## Hashes (SHA-256)\n")
for k,v in c["provenance"]["hashes_sha256"].items():
    P(f"- `{v}`  {k}")
P(f"\n- Internet Archive MD5 verified on download: `{c['provenance']['archive_md5_verified']}`\n")
P("## Counts\n")
P(f"- EXE byte-exact strings: **{len(c['exe_strings'])}**")
P(f"- Cities (CITIES.DAT): **{len(c['cities'])}**")
P(f"- Suspects (structured dossiers): **{len(suspects)}**\n")
P("## Cities (CITIES.DAT, byte-exact, in file order)\n")
for x in c["cities"]:
    P(f"- `{x['offset_hex']}` **{x['canonical_text']}**")
P("\n> Note the outdated toponym **Peking** (not Beijing) — preserved verbatim.\n")
P("## Suspect dossiers (CARMEN.EXE, byte-exact)\n")
for s in suspects:
    P(f"### {s['name']}")
    P(f"- Sex: {s['sex']}")
    P(f"- Occupation: {s['occupation']}")
    P(f"- Hobby: {s['hobby']}")
    P(f"- Hair: {s['hair']}")
    P(f"- Auto: {s['auto']}")
    P(f"- Feature: {s['feature_1']}")
    P(f"- Other: {s['feature_2']}\n")
P("## Placeholder inventory (runtime-assembled templates)\n")
P("Placeholders are C `printf`-style. `%Fs` is a far-pointer string. Assembly is inferred from")
P("string layout, **not** runtime-verified (no DOSBox capture in this pass).\n")
for ph,n in sorted(placeholders.items()):
    P(f"- `{ph}` × {n}")
P("\n## Preserved anomalies (NOT corrected)\n")
P("- `tennis raquet` (misspelling of *racquet*) — witness fragment `0x1996e`")
P("- `Ukranian` (misspelling of *Ukrainian*) — Ihor Ihorovich dossier")
P("- `Peking` (outdated toponym) — CITIES.DAT")
P("- `Bröderbund` with the German ö in the copyright line\n")
P("## Section index (every EXE string is classified)\n")
for sec,rows in by_sec.items():
    P(f"- **{sec}** — {len(rows)} strings (`{rows[0]['offset_hex']}`–`{rows[-1]['offset_hex']}`)")
P("\n## Non-canonical / excluded files\n")
P("- `ACME.DAT` — saved detective **roster/scores** (player names, GAME*.SAV refs). Player data, not game text.")
P("- `DESKTOPD.CFG` — launcher config.")
P("- `CARMEN.DAT` — graphics bank (no readable prose).")
P("- `*.BMP` — Windows-3.x city/character art (320×200×8).")
P("- `DIGISND.DAT`, `MIDISND.DAT` — audio.\n")
P("## Negative findings (edition-specific)\n")
P("- **No textual city descriptions / population passages / country names exist in this edition.**")
P("  City knowledge is conveyed by the photographic city art (CITIES.DAT images), not prose.")
P("- Witness clues describe the **suspect** (hobby/hair/feature/vehicle/food) for the crime computer,")
P("  **not** the travel destination. The spec's 'Bamako ~800,000 population' text is absent here —")
P("  it belongs to a different build or is rendered graphically.\n")
open(os.path.join(OUT,"carmen_corpus.md"),"w").write("\n".join(L))
print("suspects:",len(suspects),"| templates:",len(templates),"| placeholders:",dict(placeholders))
print("wrote game_data.json and carmen_corpus.md")
