#!/usr/bin/env python3
"""Extract the v2 i18n source catalog (2026-08 full-coverage pass) from the Kotlin sources.

Produces translation/source/ui2.json — a flat {key: english} map covering:
  * every `Strings.ui("...")` / `i18n.ui("...")` / GameData `t("...")` chrome string (key "ui:<english>")
  * keyed display overlays: rank.*, venue.*, occ.*, tval.*, region.name.*, treasure.*, nemesis.*,
    noinfo.*, day.*, suspect.*, achv.*, country.*, city.<name>.name

Idempotent: re-run after adding strings; diff the output to see what's new.
"""
import json, os, re, glob

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "android/app/src/main/java")

def read(p): return open(os.path.join(SRC, p)).read()

def unesc(s):
    return (s.replace('\\"', '"').replace("\\'", "'").replace("\\n", "\n")
             .replace("\\\\", "\\").replace("\\$", "$"))

out = {}

# ---------- 1. ui: chrome strings from call sites ----------
ui_re = re.compile(r'(?:Strings|i18n)\.ui\(\s*"((?:[^"\\]|\\.)*)"')
t_re = re.compile(r'(?<![A-Za-z])t\(\s*"((?:[^"\\]|\\.)*)"\)')
for f in glob.glob(SRC + "/**/*.kt", recursive=True):
    txt = open(f).read()
    for m in ui_re.finditer(txt):
        s = unesc(m.group(1))
        out["ui:" + s] = s
# GameData's case-flow templates go through t(en) -> Strings.ui(en)
for m in t_re.finditer(read("com/acme/clara/data/GameData.kt")):
    s = unesc(m.group(1))
    out["ui:" + s] = s
# CRT/tutorial row labels are data-driven through Strings.ui(label)
for s in ["SEX", "HOBBY", "HAIR", "FEATURE", "VEHICLE"]:
    out["ui:" + s] = s
# Options menu toggles route through chk(on, label) -> Strings.ui(label)
for s in ["Sound", "Haptics", "Captions", "Reminders"]:
    out["ui:" + s] = s
# WelcomeBackWorker consts route through Strings.ui(TITLE/TEXT)
out["ui:A lead has gone cold"] = "A lead has gone cold"
out["ui:Come back, detective — a fresh hint is waiting."] = "Come back, detective — a fresh hint is waiting."

# ---------- 2. keyed overlays from the Kotlin data ----------
gd = read("com/acme/clara/data/GameData.kt")

def str_list(name, txt=None):
    txt = txt or gd
    m = re.search(name + r'\s*=\s*listOf\((.*?)\n    \)', txt, re.S)
    return re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(1))

for r in str_list("val ranks"):
    out[f"rank.{unesc(r)}"] = unesc(r)
for v in str_list("val venues"):
    out[f"venue.{unesc(v)}"] = unesc(v)
for o in str_list("val occupations"):
    out[f"occ.{unesc(o)}"] = unesc(o)
for grp in ["hobbies", "hairColors", "features", "vehicles"]:
    for v in str_list("val " + grp):
        out[f"tval.{unesc(v)}"] = unesc(v)
for v in ["female", "male"]:
    out[f"tval.{v}"] = v

# no-information lines, keyed by venue (GameData + Expansion)
exp = read("com/acme/clara/data/Expansion.kt")
for txt in (gd, exp):
    m = re.search(r'noInformationByVenue[^(]*mapOf\((.*?)\n    \)', txt, re.S)
    for k, v in re.findall(r'"((?:[^"\\]|\\.)*)" to "((?:[^"\\]|\\.)*)"', m.group(1)):
        out[f"noinfo.{unesc(k)}"] = unesc(v)

# expansion venues + bespoke witnesses
m = re.search(r'venueOccupations: Map<String, List<String>> = mapOf\((.*?)\n    \)', exp, re.S)
for k, vs in re.findall(r'"((?:[^"\\]|\\.)*)" to listOf\(([^)]*)\)', m.group(1)):
    out[f"venue.{unesc(k)}"] = unesc(k)
    for o in re.findall(r'"((?:[^"\\]|\\.)*)"', vs):
        out[f"occ.{unesc(o)}"] = unesc(o)

# regions (display names)
for r in ["Europe", "Asia", "Africa", "South America", "North America", "Oceania", "the Middle East"]:
    out[f"region.name.{r}"] = r

# mastermind arc families/roles (Masterminds.kt) — Strings.label("mastermind.family"/"mastermind.role", ...)
for r in ["Europe", "the Americas", "Asia", "Africa", "Oceania & the Frontiers"]:
    out[f"mastermind.family.{r}"] = r
for r in ["Boss", "Successor", "Finale"]:
    out[f"mastermind.role.{r}"] = r

# treasures / nemesis teases (ClaraViewModel data)
vm = read("com/acme/clara/game/ClaraViewModel.kt")
m = re.search(r'object Treasures \{.*?listOf\((.*?)\n    \)', vm, re.S)
for i, t in enumerate(re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(1))):
    out[f"treasure.{i}"] = unesc(t)
m = re.search(r'val teases = listOf\((.*?)\n        \)', vm, re.S)
for i, t in enumerate(re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(1))):
    out[f"nemesis.{i}"] = unesc(t)

# day names
for i, d in enumerate(["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]):
    out[f"day.{i}"] = d

# suspects (dossier facts; names stay English)
for i, m in enumerate(re.finditer(r'Suspect\((.*?)\n        \)', gd, re.S)):
    body = m.group(1)
    def field(name):
        fm = re.search(name + r'\s*=\s*"((?:[^"\\]|\\.)*)"', body)
        return unesc(fm.group(1))
    for f in ["sex", "occupation", "hobby", "hair", "auto", "feature1", "feature2"]:
        out[f"suspect.{i}.{f}"] = field(f)

# achievements
ach = read("com/acme/clara/game/Achievements.kt")
for aid, title, desc in re.findall(r'Achievement\("([^"]+)", "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)"\)', ach):
    out[f"achv.{aid}.title"] = unesc(title)
    out[f"achv.{aid}.desc"] = unesc(desc)

# countries (passport stamps)
cs = read("com/acme/clara/data/CountryShapes.kt")
m = re.search(r'countryName: Map<String, String> = mapOf\((.*?)\n    \)', cs, re.S)
for code, name in re.findall(r'"([A-Z0-9]{2,3})" to "((?:[^"\\]|\\.)*)"', m.group(1)):
    out[f"country.{code}"] = unesc(name)

# place display names: every city/place the game knows
cities = str_list("val cities")
for f in ["com/acme/clara/data/CityMeta.kt", "com/acme/clara/data/Expansion.kt",
          "com/acme/clara/data/Expansion2.kt"]:
    txt = open(os.path.join(SRC, f)).read()
    cities += re.findall(r'\bCityInfo\(\s*"((?:[^"\\]|\\.)*)"', txt)
for c in sorted(set(unesc(c) for c in cities)):
    out[f"city.{c}.name"] = c

dst = os.path.join(ROOT, "translation/source/ui2.json")
json.dump(dict(sorted(out.items())), open(dst, "w"), ensure_ascii=False, indent=1)
print(f"wrote {dst}: {len(out)} keys")
from collections import Counter
print(Counter(k.split(".")[0].split(":")[0] for k in out))
