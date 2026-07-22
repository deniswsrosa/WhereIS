# 04 — Reverse-engineering `CARMEN.EXE` and the `CITIES.DAT` image codec

This is the record of what we learned decompiling the game and trying to decode
the 30 city photos from `CITIES.DAT` — including the wall we hit and why the
DOSBox capture pipeline (guide 02) won instead.

**Read this before re-attempting the codec.** Most of the dead ends are already
mapped here.

---

## 1. Getting readable code out of the EXE

`CARMEN.EXE` is **PKLITE-compressed** — you cannot read strings or disassemble it
directly. Decompress first:

```bash
deark -m pklite CARMEN.EXE          # → CARMEN.000.exe
```

The decompressed image is committed at
`work/exe_decompressed/CARMEN.000.exe` (this is the RE target — **not** the copy
you run in DOSBox; run the original there).

Disassemble any linear offset with **`tools/disl.py`** (16-bit x86 via capstone):

```bash
python3 tools/disl.py 0x1234 60      # disassemble 60 insns at linear offset 0x1234
```

It accounts for the 512-byte MZ load offset (`LOAD=512`) so the address you pass
is the file-linear code offset. Strings dump is cached at
`work/carmen_exe_strings.txt`.

Key finding: **the entire prose corpus lives in the EXE**, not the `.DAT` files
(that's how `scripts/extract_corpus.py` builds the corpus).

---

## 2. `CITIES.DAT` structure (what IS cracked)

- File size **168095 bytes**, **30 per-city records**.
- Each record: **plaintext city name** + **16-colour palette** + **compressed
  image**. Only the name is plaintext.
- Palette: **16 × 3 bytes RGB**, VGA 6-bit-per-channel (values 0–63; scale ×4 or
  `<<2 | >>4` for 8-bit).
- Decoded image geometry: **144 × 140**, **4 bpp** (16-colour indexed). This is
  confirmed — it matches the DOSBox-captured photos exactly (the oracles, §4).
- Example: Athens' compressed image stream starts at **`0x4d`** within its record.

So: geometry ✓, palette ✓, record framing ✓. The **only** unsolved piece is the
exact pixel bitstream layout after decompression.

---

## 3. The decompressor (cracked — `tools/sim.py`)

The compression is an **LZSS variant** matching the routine found in the EXE:

| Parameter | Value |
|-----------|-------|
| Ring buffer size `N` | **1024** bytes |
| Initial write position | **`N − 0x42` = 958** |
| Flag/bit reader | LSB-first, refill `ah` when empty (PKLITE-style `getbit`) |
| Match encoding | 2 bytes `B1 B2`: offset = `((B1<<8)|B2) & 0x3ff` (10 bits) |
| Match length | `(B1 >> 2) + 3` |
| Literal | one raw byte, copied to output and ring |

`tools/sim.py` is a faithful Python port of the EXE's decompress loop. It is
**provably correct** as a decompressor: run against a city's compressed stream it
consumes exactly the expected source bytes and emits a plausible output length
(≈20160 = 144×140 bytes for a 1-byte-per-pixel intermediate, or ≈10080 for
packed 4bpp). The byte stream *decompresses* cleanly.

```bash
python3 tools/sim.py     # decompresses Athens, reports produced/consumed lengths
```

---

## 4. The oracle harness (how we knew we were wrong)

Because guide 02 gave us the **real decoded photos**, we built ground-truth
"oracles" to score any candidate decoder:

- `tools/oracles.pkl` — dict keyed by city, each entry:
  `idx` (the 140×144 indexed image from the capture), `pal` (16×3), `comp` (the
  raw compressed bytes from `CITIES.DAT`), `hdr`, `H`, `W`, `complen`.
- A candidate decoder is scored by how well its output pixels match the oracle
  `idx` (fraction of pixels equal to the known palette index).

`tools/sweep.py` brute-forces the LZSS/bit-layout parameter space
(offset bits ∈ {10,11,12,13}, length bits, threshold, literal-bit polarity,
MSB/LSB flag order, hi/lo byte order, ring fill, plus 1bpp vs 4bpp-hi/lo nibble
order and top-down vs bottom-up) and ranks candidates against the oracle:

```bash
python3 tools/sweep.py    # prints top candidates by pixel-match score
```

`tools/allrecs.pkl` caches the parsed per-record framing for all 30 cities.

---

## 5. The wall

- The **decompressor is correct** (byte-exact decompression, right lengths).
- The **palettes are correct** (verified against captures).
- The **geometry is correct** (144×140, confirmed by the captures).
- **But no pixel layout produces a coherent image.** Every combination in the
  sweep — nibble order, plane layout, scan direction, threshold, bit order —
  scores at or barely above the ~1/16 random-match baseline against the oracles.

The most likely explanation (unconfirmed): the decompressed bytes are **not** a
straight linear framebuffer. Candidates never ruled out:
- **VGA planar / bit-plane** layout (4 separate 1bpp planes) rather than packed
  chunky 4bpp.
- A **row/column interleave or RLE-on-top** step between decompress and blit.
- The blit routine in the EXE applying a transform we didn't model.

Fully cracking it means disassembling the **blit path** (the code that consumes
the decompressed buffer and writes VGA memory) with `disl.py` and replicating its
addressing — not just the decompress routine, which is already done.

---

## 6. Why capture won (and the recommendation)

We shipped **all the city photos by screen-capturing the real game** (guide 02):
the game's own blitter is the ground truth, and a raw screenshot is the decoded
image at native resolution. That unblocked the app immediately.

**Recommendation for a future attempt:** the codec is a *trophy*, not a blocker.
If you want it, the remaining work is narrow and well-scoped — RE the blit/display
routine in `CARMEN.000.exe` and figure out how it addresses the decompressed
buffer — and you already have a perfect scoring oracle (`oracles.pkl`) to tell you
the instant you get it right. If you just need pixels, use guide 02.
