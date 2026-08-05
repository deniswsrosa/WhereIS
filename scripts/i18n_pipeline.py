#!/usr/bin/env python3
"""i18n pipeline helpers: chunk an English source catalog into per-batch files for the translation
workflow, and merge the workflow's per-batch results back into a language catalog.

Usage:
  chunk  <source.json> <out_dir> [batch_size]     # split {key:en} into out_dir/batch_N.json
  merge  <results_dir> <out.json>                 # merge out_dir/batch_N.result.json -> out.json
  into   <lang.json> <catalog.json> ...            # merge one or more catalogs into assets/i18n/<lang>.json
"""
import json, os, sys, glob


def chunk(src, out_dir, size=30):
    d = json.load(open(src))
    os.makedirs(out_dir, exist_ok=True)
    items = list(d.items())
    n = 0
    for i in range(0, len(items), size):
        batch = dict(items[i:i + size])
        json.dump(batch, open(f"{out_dir}/batch_{n}.json", "w"), ensure_ascii=False, indent=1)
        n += 1
    print(f"chunk: {len(items)} strings -> {n} batches of <= {size} in {out_dir}")
    return n


def merge(results_dir, out):
    merged = {}
    files = sorted(glob.glob(f"{results_dir}/batch_*.result.json"))
    for f in files:
        try:
            merged.update(json.load(open(f)))
        except Exception as e:
            print(f"  WARN: {f}: {e}")
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    json.dump(merged, open(out, "w"), ensure_ascii=False, indent=1, sort_keys=True)
    print(f"merge: {len(files)} batch files -> {len(merged)} strings in {out}")
    return merged


def into(lang_path, *catalogs):
    """Merge translated catalogs into assets/i18n/<lang>.json (create if missing)."""
    base = json.load(open(lang_path)) if os.path.exists(lang_path) else {}
    for c in catalogs:
        base.update(json.load(open(c)))
    json.dump(base, open(lang_path, "w"), ensure_ascii=False, indent=1, sort_keys=True)
    print(f"into: {lang_path} now has {len(base)} strings")


def validate(source, translated):
    """Report coverage + placeholder integrity of a translated catalog against its English source."""
    import re
    src = json.load(open(source)); tr = json.load(open(translated))
    missing = [k for k in src if k not in tr]
    ph = re.compile(r'\{[sSp01]\}|%s')
    bad = [k for k in src if k in tr and sorted(ph.findall(src[k])) != sorted(ph.findall(tr[k]))]
    print(f"validate {translated}: {len(tr)}/{len(src)} translated, {len(missing)} missing, "
          f"{len(bad)} placeholder mismatches")
    if missing[:5]: print("  missing:", missing[:5])
    if bad[:5]: print("  placeholder:", bad[:5])
    return missing, bad


def buildlang(code, root="."):
    """Merge a fanned-out language's city batches + gameplay into assets/i18n/<code>.json."""
    cdir = f"{root}/translation/_batches/cities_{code}"
    cjson = f"{root}/translation/{code}/cities.json"
    merge(cdir, cjson)
    validate(f"{root}/translation/source/cities.json", cjson)
    cats = [cjson]
    gp = f"{root}/translation/{code}/gameplay.json"
    if os.path.exists(gp):
        cats.append(gp)
    into(f"{root}/android/app/src/main/assets/i18n/{code}.json", *cats)


def buildhumor(code, root="."):
    """Assemble assets/humor_<code>.json: copy of humor.json with each line's `en` text replaced
    by the translated batch results (keys are global indices into the humor list)."""
    import re
    assets = f"{root}/android/app/src/main/assets"
    base = json.load(open(f"{assets}/humor.json"))
    src_ph = re.compile(r'\{[sSp]\}|%s')
    replaced, bad = 0, []
    for f in sorted(glob.glob(f"{root}/translation/_batches/humor_{code}/batch_*.result.json")):
        for k, v in json.load(open(f)).items():
            i = int(k)
            if sorted(src_ph.findall(base[i]["en"])) != sorted(src_ph.findall(v)):
                bad.append(i)
            base[i]["en"] = v
            replaced += 1
    if code == "ru":
        # Russian reuses {s}/{S}/{p} as gender-agreement markers elsewhere; humor lines must not
        # carry them (same strip as the ru city content).
        for e in base:
            e["en"] = re.sub(r'\{[sSp]\}', '', e["en"])
    out = f"{assets}/humor_{code}.json"
    json.dump(base, open(out, "w"), ensure_ascii=False, indent=1)
    print(f"buildhumor {code}: {replaced}/{len(base)} lines translated -> {out}; "
          f"{len(bad)} placeholder mismatches {bad[:8]}")
    return replaced, bad


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "chunk":
        chunk(sys.argv[2], sys.argv[3], int(sys.argv[4]) if len(sys.argv) > 4 else 30)
    elif cmd == "merge":
        merge(sys.argv[2], sys.argv[3])
    elif cmd == "into":
        into(sys.argv[2], *sys.argv[3:])
    elif cmd == "validate":
        validate(sys.argv[2], sys.argv[3])
    elif cmd == "buildlang":
        buildlang(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else ".")
    elif cmd == "buildhumor":
        buildhumor(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else ".")
