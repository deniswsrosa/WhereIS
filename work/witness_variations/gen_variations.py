#!/usr/bin/env python3
"""Generate 4 nano-banana (gemini-2.5-flash-image) variations for each witness sprite."""
import base64
import glob
import io
import json
import os
import sys
import time
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed

from PIL import Image

SRC_DIR = "/home/deniswsrosa/Documents/projects/WhereIS/android/app/src/main/res/drawable-nodpi"
OUT_DIR = "/home/deniswsrosa/Documents/projects/WhereIS/work/witness_variations"
RAW_DIR = os.path.join(OUT_DIR, "raw")
PROC_DIR = os.path.join(OUT_DIR, "processed")
MODEL = "gemini-2.5-flash-image"

with open(os.path.expanduser("~/.claude.json")) as f:
    for line in f:
        if '"GEMINI_API_KEY"' in line:
            API_KEY = line.split('"')[3]
            break

CLOTHES = (
    "Also make ONE small clothing change: a different color or pattern (from the palette) for the "
    "tie, collar, hat band, or garment — but keep the same type of outfit for the same role."
)

HINTS = [
    "Change ONLY the hairstyle and hair color to a different one from the palette. " + CLOTHES,
    "Change ONLY the facial features slightly: a different nose, eyes, and eyebrows. " + CLOTHES,
    "Change ONLY one accessory-level detail: add or remove glasses, or add or remove a mustache/beard "
    "(for a man), or change the hairstyle (for a woman). " + CLOTHES,
    "Change ONLY the skin tone (to another tone available in the palette) and the hair color. " + CLOTHES,
]

# Per-witness hint overrides: bolder, tailored variation directions
WITNESS_HINTS = {
    "witness_docent": [
        "Change her hairstyle to a clearly different short bob with a clean light outline. Her hair "
        "color must be BLACK, GREY-WHITE, or DARK RED — NEVER a beige or skin tone. Keep the red bow.",
        "Give her grey-white hair in an updo, clearly outlined against the background; slightly "
        "different nose and eye. Hair must clearly contrast with her skin color.",
        "Give her glasses (eyes must stay clearly visible inside the lenses) and a different, "
        "shorter BLACK hairstyle with a light outline. Hair must never match her skin tone.",
        "Change her skin tone to a different tone from the palette and give her a clearly different "
        "DARK hairstyle that contrasts with the new skin tone; shade her face only with darker skin "
        "tones, never grey.",
    ],
    "witness_bellhop": [
        "Different hairstyle and hair color under the cap. His eyes must be LARGE, ROUND, and "
        "clearly drawn with white and a dark pupil, like the original. Do not use ANY grey on his "
        "face — shade skin only with darker skin tones.",
        "Different nose and eyebrows; a different cap color from the palette. Eyes clearly drawn "
        "with dark pupils; absolutely no grey shading on the face or neck.",
        "Make him noticeably older with clearly drawn eyes (white plus dark pupil). Face shading "
        "only in skin tones, never grey.",
        "Change his skin tone to a different tone from the palette; keep large clearly drawn eyes "
        "with dark pupils, and shade the face only with darker versions of the new skin tone.",
    ],
    "witness_harbor_master": [
        "Give him a clearly DIFFERENT beard: a big full white beard with a light outline, and a "
        "different hat band color.",
        "Remove the beard entirely and give him a large mustache instead; different hair color at "
        "the temples.",
        "Give him a clearly different hat color from the palette and a short dark beard with a "
        "light outline where it meets the background.",
        "Make him noticeably younger, clean-shaven, with a different hair color under the cap.",
    ],
    "witness_soldier": [
        "Give him a clearly different face: big nose, different chin, and a visible mustache.",
        "Change the beret color to a different palette color and give him clearly different, "
        "lighter hair.",
        "Make him noticeably older with a weathered face and grey sideburns (outlined if dark).",
        "Change his skin tone to a different tone from the palette and give him a different "
        "expression showing teeth.",
    ],
}

PARROT_HINTS = [
    "Change ONLY the feather colors, using other colors from the palette.",
    "Change ONLY the beak color and the eye.",
    "Change ONLY the head and chest feather colors.",
    "Change ONLY the wing markings.",
]

# Hand-classified from the sprites; the model must not re-guess this.
GENDER = {
    "witness_ambassador": "man", "witness_analyst": "man", "witness_archivist": "man",
    "witness_attache": "man", "witness_baggage_clerk": "woman", "witness_bank_guard": "man",
    "witness_bartender": "man", "witness_bellhop": "man", "witness_circulation_clerk": "woman",
    "witness_curator": "man", "witness_customs_officer": "woman", "witness_docent": "woman",
    "witness_flight_attendant": "woman", "witness_harbor_master": "man", "witness_hawker": "man",
    "witness_hotel_manager": "man", "witness_house_detective": "man", "witness_messenger": "man",
    "witness_museum_guard": "man", "witness_palace_guard": "man", "witness_pilot": "man",
    "witness_privy_councillor": "woman", "witness_reference_librarian": "woman",
    "witness_sailor": "man", "witness_sailor_s_parrot": "parrot", "witness_soldier": "man",
    "witness_stevedore": "man", "witness_street_merchant": "man", "witness_teller": "woman",
    "witness_tennis_pro": "man", "witness_trader": "woman", "witness_tugboat_captain": "man",
    "witness_under_secretary": "woman", "witness_urchin": "boy", "witness_vice_president": "man",
    "witness_waiter": "man",
    "suspect_clara_san_diego": "woman", "suspect_dazzle_annie_nonker": "woman",
    "suspect_fast_eddie_b": "man", "suspect_ihor_ihorovich": "man",
    "suspect_katherine_boom_boom_drib": "woman", "suspect_lady_agatha_wayland": "woman",
    "suspect_len_red_bulk": "man", "suspect_merey_laroc": "woman",
    "suspect_nick_brunch": "man", "suspect_scar_graynolt": "man",
}

# Traits referenced by crime-computer clues (GameData.kt) — the art must not contradict them.
SUSPECT_LOCK = {
    "suspect_clara_san_diego": "a woman with REDDISH-BROWN hair; keep her elegant hat-and-fur-coat look",
    "suspect_merey_laroc": "a woman with BROWN hair who wears FANCY JEWELRY (keep visible jewelry); keep her props",
    "suspect_dazzle_annie_nonker": "a woman with BLOND hair; keep the headband style",
    "suspect_lady_agatha_wayland": "a woman with RED hair who wears a huge DIAMOND RING; keep the sun hat",
    "suspect_len_red_bulk": "a man with RED hair; keep the fanned-out banknotes prop",
    "suspect_scar_graynolt": "a man with RED hair who wears a PINKY RING; keep the hat and sunglasses look",
    "suspect_nick_brunch": "a man with BLACK hair, a MOUSTACHE, a snap-brimmed FEDORA and trenchcoat; keep the gun prop",
    "suspect_fast_eddie_b": "a man with BLACK hair, impeccably dressed; keep the beret, round glasses and cigarette",
    "suspect_ihor_ihorovich": "a man with BLOND hair and a TATTOO on his right shoulder (keep it visible)",
    "suspect_katherine_boom_boom_drib": "a woman with BROWN hair; keep the sunglasses, bandana and biker look",
}

SUSPECT_HINTS = [
    "Change the FACE STRUCTURE clearly: different nose, jaw, and eyes — a different person.",
    "Change the hairSTYLE clearly (same hair color!) and slightly restyle the face.",
    "Make the person noticeably older or younger, with a clearly different face.",
    "Keep the same face angle but change facial proportions, expression, and any clothing colors "
    "that are not part of the locked look.",
]


def role_from_name(fname):
    base = os.path.basename(fname)[:-len(".png")]
    if base.startswith("suspect_"):
        return "wanted criminal suspect " + base[len("suspect_"):].replace("_", " ").title()
    return base[len("witness_"):].replace("_s_", "'s ").replace("_", " ")


def palette_colors(path):
    """Exact palette for clean sprites; median-cut clusters for smoothed captures."""
    im = Image.open(path).convert("RGB")
    cols = im.getcolors(100000)
    if cols and len(cols) <= 16:
        return [c for _, c in sorted(cols, reverse=True)]
    q = im.quantize(colors=12, method=Image.MEDIANCUT)
    pal = q.getpalette()
    out = []
    for i in range(0, 36, 3):
        c = tuple(pal[i:i + 3])
        if c not in out:
            out.append(c)
    return out


def build_prompt(name, role, hint):
    pal = ["#%02x%02x%02x" % c for c in palette_colors(name)]
    gender = GENDER[os.path.basename(name)[:-4]]
    who = "a parrot" if gender == "parrot" else f"a {gender}"
    return (
        "This image is a tiny, very crude witness portrait from a 1989 EGA DOS detective game, "
        f"shown zoomed in so each game pixel is a large square. The character is {who}; "
        f"their role is: {role}. "
        "TASK: this is a MINIMAL EDIT, not a redesign. Reproduce the input image EXACTLY — same "
        "pixels, same shapes, same colors, same background, same pose, same clothing — and apply "
        f"only this one small change: {hint} "
        "Everything not covered by that change must stay pixel-identical to the input. "
        + (f"GAMEPLAY-LOCKED TRAITS: the character must remain {SUSPECT_LOCK[os.path.basename(name)[:-4]]}. "
           "Never change these locked attributes. "
           if os.path.basename(name)[:-4] in SUSPECT_LOCK else "")
        + f"Use ONLY these exact colors: {', '.join(pal)}. "
        "The background must stay exactly as in the input. STRICT QUALITY RULES: "
        "(a) The character's silhouette must always read clearly against the background — if the "
        "hair, BEARD/mustache, or clothing is black or very dark on a black background, draw a thin "
        "WHITE (or lightest-palette-color) outline around it, exactly like the original art does. "
        "The outline must NEVER be a saturated color like red, green, or blue. "
        "(b) The character must always have clearly drawn, visible eyes in the same style as the "
        "original (a profile view shows one eye, like the original). If the character wears glasses "
        "— original or added — the eyes MUST still be drawn visibly inside the lenses (white of the "
        "eye plus dark pupil pixels); never draw blank, solid, or opaque lenses, and never let the "
        "frames cover the eyes. "
        "(c) The hair color must be clearly different from the skin tone — never color the hair "
        "with the same color as the face. "
        "(d) Prefer LARGE FLAT single-color areas — use checkerboard dithering ONLY in the places "
        "where the original itself uses it, never as an overall texture. "
        "(f) Shade skin ONLY with darker tones of the same skin color (or a checkerboard of two "
        "skin tones), exactly like the original does — NEVER shade a face or neck with grey or any "
        "desaturated color. Grey is only allowed for genuinely grey things (grey hair, grey cloth). "
        "(e) The stated change must be OBVIOUS at a single glance. Do not return a near-copy of the "
        "input: the changed attribute must be clearly, unmistakably different from the original. "
        "Keep the crude 1989 look: flat color areas, thick black outlines, no gradients, no fine "
        "detail, no anti-aliasing. Keep the exact same canvas framing, edge to edge. "
        "Do not add any text, borders, frames, or watermarks."
    )


def upscaled_b64(path, target=480):
    im = Image.open(path).convert("RGB")
    scale = max(1, target // max(im.size))
    im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, format="PNG")
    return base64.b64encode(buf.getvalue()).decode()


def call_api(prompt, img_b64, tries=5):
    body = json.dumps({
        "contents": [{"parts": [
            {"inline_data": {"mime_type": "image/png", "data": img_b64}},
            {"text": prompt},
        ]}]
    }).encode()
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent"
    for attempt in range(tries):
        try:
            req = urllib.request.Request(url, data=body, headers={
                "x-goog-api-key": API_KEY, "Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=120) as r:
                resp = json.load(r)
            for part in resp["candidates"][0]["content"]["parts"]:
                if "inlineData" in part:
                    return base64.b64decode(part["inlineData"]["data"])
            raise RuntimeError("no image in response: " + json.dumps(resp)[:300])
        except urllib.error.HTTPError as e:
            detail = e.read()[:200]
            if e.code in (429, 500, 503) and attempt < tries - 1:
                time.sleep(15 * (attempt + 1))
                continue
            raise RuntimeError(f"HTTP {e.code}: {detail}") from e
        except Exception:
            if attempt < tries - 1:
                time.sleep(10)
                continue
            raise


def qc_check(proc_path, gender):
    """Ask a cheap text model to inspect the finished sprite for known defect modes."""
    im = Image.open(proc_path).convert("RGB")
    im = im.resize((im.width * 6, im.height * 6), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, format="PNG")
    b64 = base64.b64encode(buf.getvalue()).decode()
    subject = "parrot" if gender == "parrot" else "person"
    q = (
        f"This is a zoomed-in tiny pixel-art portrait of a {subject} from a retro DOS game, "
        "on a plain background. Inspect it and answer with STRICT JSON only, no prose: "
        '{"eyes_visible": bool,  // at least one clearly drawn eye with a pupil (profile views show one); '
        'if the character wears glasses the eyes must be visible INSIDE the lenses — blank/solid lenses = false\n'
        ' "silhouette_readable": bool,  // hair, beard, and head outline visible against the background, not melting into it\n'
        ' "hair_distinct_from_skin": bool,  // hair color clearly differs from face color (true for bald or a parrot)\n'
        ' "no_weird_outline": bool,  // no saturated red/green/blue outline scribbles around the figure\n'
        ' "skin_shading_ok": bool}  // face/neck shadows use darker skin tones, NOT grey or desaturated colors'
    )
    body = json.dumps({
        "contents": [{"parts": [
            {"inline_data": {"mime_type": "image/png", "data": b64}},
            {"text": q},
        ]}],
        "generationConfig": {"responseMimeType": "application/json"},
    }).encode()
    url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    try:
        req = urllib.request.Request(url, data=body, headers={
            "x-goog-api-key": API_KEY, "Content-Type": "application/json"})
        with urllib.request.urlopen(req, timeout=60) as r:
            resp = json.load(r)
        verdict = json.loads(resp["candidates"][0]["content"]["parts"][0]["text"])
        return all(bool(verdict.get(k, True)) for k in
                   ("eyes_visible", "silhouette_readable", "hair_distinct_from_skin",
                    "no_weird_outline", "skin_shading_ok")), verdict
    except Exception as e:
        return True, {"qc_error": str(e)}  # never block the pipeline on a QC hiccup


def postprocess(raw_bytes, orig_path, out_path):
    orig = Image.open(orig_path).convert("RGB")
    # Force the ORIGINAL sprite's palette so the result shares its design language
    pal_colors = palette_colors(orig_path)
    pal_img = Image.new("P", (1, 1))
    flat = [v for c in pal_colors for v in c]
    pal_img.putpalette(flat + flat[:3] * (256 - len(pal_colors)))
    im = Image.open(io.BytesIO(raw_bytes)).convert("RGB")
    im = im.resize(orig.size, Image.BOX)
    im = im.quantize(palette=pal_img, dither=Image.NONE).convert("RGB")
    im.save(out_path)


def one_job(args):
    fname, vidx = args
    name = os.path.basename(fname)[:-4]
    raw_path = os.path.join(RAW_DIR, f"{name}_v{vidx+1}.png")
    proc_path = os.path.join(PROC_DIR, f"{name}_v{vidx+1}.png")
    if os.path.exists(proc_path):
        return (name, vidx, "cached")
    role = role_from_name(fname)
    gender = GENDER[name]
    hints = WITNESS_HINTS.get(name) or (
        PARROT_HINTS if gender == "parrot"
        else SUSPECT_HINTS if name.startswith("suspect_")
        else HINTS)
    for attempt in range(3):
        raw = call_api(build_prompt(fname, role, hints[vidx]), upscaled_b64(fname))
        with open(raw_path, "wb") as f:
            f.write(raw)
        postprocess(raw, fname, proc_path)
        # near-duplicate guard: reject variants whose pixels barely differ from the original
        a = Image.open(fname).convert("RGB").getdata()
        b = Image.open(proc_path).convert("RGB").getdata()
        same = sum(1 for x, y in zip(a, b) if x == y) / len(a)
        if same > 0.90:
            verdict = {"near_duplicate": round(same, 3)}
            continue
        ok, verdict = qc_check(proc_path, gender)
        if ok:
            return (name, vidx, "ok" if attempt == 0 else f"ok after {attempt+1} tries")
    return (name, vidx, f"QC-FAILED after 3 tries, kept last: {verdict}")


def main():
    os.makedirs(RAW_DIR, exist_ok=True)
    os.makedirs(PROC_DIR, exist_ok=True)
    if len(sys.argv) > 1:
        files = [os.path.join(SRC_DIR, a + ".png") for a in sys.argv[1:]]
    else:
        files = sorted(glob.glob(os.path.join(SRC_DIR, "witness_*.png")))
    jobs = [(f, v) for f in files for v in range(4)]
    print(f"{len(files)} witnesses, {len(jobs)} jobs", flush=True)
    done = fail = 0
    with ThreadPoolExecutor(max_workers=6) as ex:
        futs = {ex.submit(one_job, j): j for j in jobs}
        for fut in as_completed(futs):
            f, v = futs[fut]
            try:
                name, vidx, status = fut.result()
                done += 1
                print(f"[{done}/{len(jobs)}] {name} v{vidx+1} {status}", flush=True)
            except Exception as e:
                fail += 1
                print(f"FAIL {os.path.basename(f)} v{v+1}: {e}", flush=True)
    print(f"DONE ok={done} fail={fail}", flush=True)


if __name__ == "__main__":
    main()
