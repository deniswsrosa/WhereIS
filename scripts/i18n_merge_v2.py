#!/usr/bin/env python3
"""Merge + QA a language's ui2 translation into its runtime bundle.

  python3 scripts/i18n_merge_v2.py check <lang>   # validate only (no write)
  python3 scripts/i18n_merge_v2.py merge <lang>   # validate, then merge into assets/i18n/<lang>.json

Checks: key parity vs the language's source batch, placeholder integrity, length budgets for the
tight 320x200 spots, a diacritics-density heuristic (the accent-loss bug class from the 2026-08
humor pass), and untranslatable tokens (Clara San Diego / Interpol / WDB must survive).
Merging never overwrites a key already present in the bundle.
"""
import json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

BUDGETS = [
    (re.compile(r"^ui:(Game|Options|Bureau|Dossiers)$"), 9),
    (re.compile(r"^ui:(SEX|HOBBY|HAIR|FEATURE|VEHICLE)$"), 8),
    (re.compile(r"^ui:COMPUTE$"), 10),
    # CLOSE/GOT IT render via self-sizing wrap-content buttons (DosButton/Pill) -- no hard cap.
    (re.compile(r"^ui:(Yes|No|SHARE|HIDE)$"), 8),
    (re.compile(r"^tval\."), 14),
    (re.compile(r"^venue\."), 16),
    (re.compile(r"^rank\."), 18),
    (re.compile(r"^ui:PRESS  ANY  KEY  TO  BEGIN$"), 30),
]
# languages where Latin diacritics are expected in bulk (heuristic floor: share of values
# with any non-ASCII letter). ru uses Cyrillic -> checked as >90% non-ASCII instead.
DIACRITIC_FLOOR = {"pt": .18, "es": .12, "fr": .25, "de": .10, "it": .10, "pl": .30, "tr": .25, "cs": .3}

def main():
    mode, lang = sys.argv[1], sys.argv[2]
    src = json.load(open(f"{ROOT}/translation/_batches/ui2_{lang}/source.json"))
    tr = json.load(open(f"{ROOT}/translation/{lang}/ui2.json"))
    ph = re.compile(r"\{[0-9]\}|%s|\\n")
    problems = []

    missing = [k for k in src if k not in tr]
    extra = [k for k in tr if k not in src]
    if missing: problems.append(f"{len(missing)} missing keys, e.g. {missing[:3]}")
    if extra: problems.append(f"{len(extra)} unexpected keys, e.g. {extra[:3]}")

    for k, en in src.items():
        v = tr.get(k)
        if v is None: continue
        if sorted(ph.findall(en)) != sorted(ph.findall(v)):
            problems.append(f"placeholder mismatch {k!r}: {en!r} -> {v!r}")
        for pat, lim in BUDGETS:
            if pat.match(k) and len(v) > lim:
                problems.append(f"over budget ({lim}) {k!r}: {v!r} ({len(v)})")
        for token in ("Clara San Diego", "Interpol", "WDB"):
            if token in en and token not in v:
                problems.append(f"token {token!r} lost in {k!r}: {v!r}")

    vals = [tr[k] for k in tr]
    nonascii = sum(1 for v in vals if any(ord(c) > 127 for c in v)) / max(1, len(vals))
    if lang == "ru":
        if nonascii < .85: problems.append(f"ru: only {nonascii:.0%} values contain Cyrillic")
    elif lang in DIACRITIC_FLOOR and nonascii < DIACRITIC_FLOOR[lang]:
        problems.append(f"{lang}: diacritics density {nonascii:.0%} below floor "
                        f"{DIACRITIC_FLOOR[lang]:.0%} — accents may have been dropped")

    print(f"{lang}: {len(tr)}/{len(src)} translated, {len(problems)} problems")
    for p in problems[:25]: print("  !", p)
    if problems and mode == "merge":
        sys.exit("refusing to merge with problems — fix and re-run")
    if mode == "merge":
        path = f"{ROOT}/android/app/src/main/assets/i18n/{lang}.json"
        bundle = json.load(open(path))
        added = {k: v for k, v in tr.items() if k not in bundle}
        bundle.update(added)
        json.dump(bundle, open(path, "w"), ensure_ascii=False, indent=1, sort_keys=True)
        print(f"merged {len(added)} new keys -> {path} (now {len(bundle)})")
    sys.exit(1 if problems else 0)

if __name__ == "__main__":
    main()
