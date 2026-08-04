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


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "chunk":
        chunk(sys.argv[2], sys.argv[3], int(sys.argv[4]) if len(sys.argv) > 4 else 30)
    elif cmd == "merge":
        merge(sys.argv[2], sys.argv[3])
    elif cmd == "into":
        into(sys.argv[2], *sys.argv[3:])
