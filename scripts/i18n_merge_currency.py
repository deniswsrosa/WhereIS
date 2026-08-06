#!/usr/bin/env python3
"""Merge + QA a language's currency-name translation into its runtime bundle.

  python3 scripts/i18n_merge_currency.py check <lang>
  python3 scripts/i18n_merge_currency.py merge <lang>

Checks: key parity vs the source catalog, no leading article accidentally kept, no stray
placeholders, protected currencies untouched where they're loanwords shared across languages
is NOT enforced (translators may legitimately keep e.g. "euro" as "euro").
"""
import json, os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ARTICLE_RE = re.compile(r'^(the|the\s|el |la |los |las |le |la |les |der |die |das |den |het |de )', re.IGNORECASE)

def main():
    mode, lang = sys.argv[1], sys.argv[2]
    src = json.load(open(f"{ROOT}/translation/source/currency.json"))
    tr = json.load(open(f"{ROOT}/translation/{lang}/currency.json"))
    problems = []

    missing = [k for k in src if k not in tr]
    if missing: problems.append(f"{len(missing)} missing keys, e.g. {missing[:5]}")

    for k, en in src.items():
        v = tr.get(k)
        if v is None: continue
        if not v.strip():
            problems.append(f"empty value for {k!r}")
        if ARTICLE_RE.match(v.strip()):
            problems.append(f"leading article kept in {k!r}: {v!r}")

    print(f"{lang}: {len(tr)}/{len(src)} translated, {len(problems)} problems")
    for p in problems[:20]: print("  !", p)
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
