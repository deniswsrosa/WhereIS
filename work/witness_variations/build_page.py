#!/usr/bin/env python3
"""Build the witness-replacement review page (data-URI images, single file)."""
import base64
import collections
import glob
import html
import json
import os
import sys

SRC_DIR = "/home/deniswsrosa/Documents/projects/WhereIS/android/app/src/main/res/drawable-nodpi"
WORK = "/home/deniswsrosa/Documents/projects/WhereIS/work/witness_variations"
PROC_DIR = os.path.join(WORK, "processed")
KEPT_DIR = os.path.join(WORK, "kept")
OUT = sys.argv[1]
PREVIEW = len(sys.argv) > 2 and sys.argv[2] == "preview"

from PIL import Image

decisions = json.load(open(os.path.join(WORK, "decisions.json"))) if os.path.exists(os.path.join(WORK, "decisions.json")) else {}
keeps = json.load(open(os.path.join(WORK, "keeps.json"))) if os.path.exists(os.path.join(WORK, "keeps.json")) else {}
decided = set(decisions)


def duri(path):
    with open(path, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode()


def role(name):
    if name.startswith("suspect_"):
        return "SUSPECT: " + name[len("suspect_"):].replace("_", " ")
    return name[len("witness_"):].replace("_s_", "'s ").replace("_", " ")


counts = collections.Counter(os.path.basename(f).rsplit("_v", 1)[0] for f in glob.glob(os.path.join(PROC_DIR, "*.png")))
all_witnesses = sorted(os.path.basename(f)[:-4] for f in glob.glob(os.path.join(SRC_DIR, "witness_*.png"))) + \
    sorted(os.path.basename(f)[:-4] for f in glob.glob(os.path.join(SRC_DIR, "suspect_*.png")))
complete = [w for w in all_witnesses if counts.get(w) == 4 and w not in decided]
pending = [w for w in all_witnesses if counts.get(w, 0) < 4 and w not in decided]


def alt_cell(w, pick, img_path, dw, dh, badge_cls, badge_txt, cap):
    return (
        f'<div class="cell alt" data-w="{w}" data-pick="{pick}" tabindex="0">'
        f'<span class="badge {badge_cls}">{badge_txt}</span>'
        f'<img src="{duri(img_path)}" style="width:{dw}px;height:{dh}px" alt="{role(w)} option {pick}">'
        f'<div class="btns"><span class="cap">{cap}</span>'
        f'<button class="kbtn" data-w="{w}" data-keep="{pick}" title="Shortlist: carry into next round and still generate 4 new options">KEEP</button></div></div>'
    )


cards = []
for w in complete:
    orig = os.path.join(SRC_DIR, w + ".png")
    im = Image.open(orig)
    zoom = max(1, round(160 / im.height))
    dw, dh = im.width * zoom, im.height * zoom
    r = role(w)
    cells = [
        f'<div class="cell original" data-w="{w}" data-pick="RETRY" tabindex="0" '
        f'title="Click = keep original for now, regenerate new options">'
        f'<span class="badge orig-badge">ORIGINAL</span>'
        f'<img src="{duri(orig)}" style="width:{dw}px;height:{dh}px" alt="{r} original">'
        f'<div class="btns"><span class="cap">in game now</span></div></div>'
    ]
    for i, kf in enumerate(keeps.get(w, [])):
        kp = os.path.join(KEPT_DIR, kf)
        if os.path.exists(kp):
            cells.append(alt_cell(w, f"K{i+1}", kp, dw, dh, "kept-badge", f"KEPT {i+1}", "from last round"))
    for i, letter in enumerate("ABCD"):
        p = os.path.join(PROC_DIR, f"{w}_v{i+1}.png")
        cells.append(alt_cell(w, letter, p, dw, dh, "alt-badge", letter, "new option"))
    cards.append(
        f'<section class="card" id="card-{w}"><header class="card-head">'
        f'<h2>{html.escape(r)}</h2><span class="fname">{w}.png · {im.width}×{im.height}</span>'
        f'<span class="state" data-state-for="{w}">— no choice yet</span></header>'
        f'<div class="row">{cells[0]}<div class="vsep"></div>{"".join(cells[1:])}</div></section>'
    )

notes = []
if decided:
    notes.append(f'{len(decided)} already replaced: {", ".join(role(d) for d in sorted(decided))}')
if pending:
    notes.append(f'{len(pending)} not yet generated')
note_html = f'<p class="pending">{"PREVIEW — " if PREVIEW else ""}{" · ".join(notes)}</p>' if notes else ""

css = """
:root{
  --bg:#0b0d12; --panel:#12151d; --panel2:#161a24; --line:#252b3c;
  --text:#d6dae3; --muted:#878ea1; --cyan:#53d3d1; --amber:#ffb454;
  --green:#62d980; --red:#ff7a6b; --violet:#b18cff;
}
*{box-sizing:border-box}
body{background:var(--bg);color:var(--text);margin:0;
  font-family:ui-monospace,'Cascadia Mono','JetBrains Mono',Menlo,Consolas,monospace;
  font-size:14px;line-height:1.5;padding-bottom:110px}
.wrap{max-width:1100px;margin:0 auto;padding:28px 20px}
h1{font-size:20px;letter-spacing:.12em;text-transform:uppercase;color:var(--cyan);margin:0 0 4px}
.sub{color:var(--muted);margin:0 0 6px;max-width:78ch}
.legend{display:flex;gap:16px;flex-wrap:wrap;margin:14px 0 14px;padding:10px 14px;
  background:var(--panel);border:1px solid var(--line);border-radius:4px;font-size:12.5px}
.legend b{font-weight:600}
.k{display:inline-block;width:11px;height:11px;border-radius:2px;margin-right:7px;vertical-align:-1px}
.k.o{background:var(--amber)} .k.u{background:var(--green)} .k.kp{background:var(--violet)} .k.r{background:var(--red)}
.pending{color:var(--amber);font-size:12.5px;border:1px dashed var(--amber);border-radius:4px;
  padding:8px 12px;margin:0 0 22px}
.card{background:var(--panel);border:1px solid var(--line);border-radius:6px;margin:0 0 22px;overflow:hidden}
.card-head{display:flex;align-items:baseline;gap:14px;padding:10px 16px;background:var(--panel2);
  border-bottom:1px solid var(--line);flex-wrap:wrap}
.card-head h2{font-size:15px;margin:0;text-transform:uppercase;letter-spacing:.08em}
.fname{color:var(--muted);font-size:11.5px}
.state{margin-left:auto;font-size:12px;color:var(--muted)}
.state.sel{color:var(--green)} .state.retry{color:var(--red)} .state.keep{color:var(--violet)}
.row{display:flex;gap:14px;padding:16px;align-items:stretch;overflow-x:auto}
.vsep{width:1px;background:var(--line);flex:0 0 1px;margin:0 4px}
.cell{position:relative;flex:0 0 auto;display:flex;flex-direction:column;align-items:center;gap:6px;
  padding:26px 12px 8px;border:2px solid var(--line);border-radius:4px;background:#000;cursor:pointer;
  transition:border-color .12s, box-shadow .12s}
.cell img{image-rendering:pixelated;display:block;background:#000}
.btns{display:flex;gap:8px;align-items:center;min-height:22px}
.cap{font-size:10.5px;color:var(--muted);letter-spacing:.06em;text-transform:uppercase}
.kbtn{background:transparent;color:var(--violet);border:1px solid var(--violet);border-radius:3px;
  font:inherit;font-size:10px;font-weight:700;letter-spacing:.1em;padding:2px 8px;cursor:pointer}
.kbtn:hover{background:var(--violet);color:#170b2e}
.cell.kept-state .kbtn{background:var(--violet);color:#170b2e}
.badge{position:absolute;top:0;left:0;right:0;text-align:center;font-size:10.5px;font-weight:700;
  letter-spacing:.14em;padding:3px 0}
.orig-badge{background:var(--amber);color:#1a1205}
.alt-badge{background:#1d3a3a;color:var(--cyan)}
.kept-badge{background:#2b2050;color:var(--violet)}
.cell.original{border-color:var(--amber)}
.cell:hover{border-color:var(--cyan)}
.cell.alt.picked{border-color:var(--green);box-shadow:0 0 0 2px rgba(98,217,128,.35)}
.cell.alt.picked .alt-badge,.cell.alt.picked .kept-badge{background:var(--green);color:#08210f}
.cell.alt.kept-state{border-color:var(--violet);box-shadow:0 0 0 2px rgba(177,140,255,.3)}
.cell.original.picked{border-color:var(--red);box-shadow:0 0 0 2px rgba(255,122,107,.35)}
.cell.original.picked .orig-badge{background:var(--red);color:#2a0b07}
footer{position:fixed;left:0;right:0;bottom:0;background:var(--panel2);border-top:1px solid var(--line);
  padding:12px 20px;display:flex;gap:16px;align-items:center;flex-wrap:wrap}
.count{color:var(--cyan);font-size:13px}
#copy{background:var(--cyan);color:#06282a;border:0;border-radius:4px;font:inherit;font-weight:700;
  padding:8px 16px;cursor:pointer;letter-spacing:.05em}
#copy:focus-visible,.cell:focus-visible,.kbtn:focus-visible{outline:2px solid var(--cyan);outline-offset:2px}
#out{flex:1 1 260px;min-width:220px;height:44px;background:#0a0c10;color:var(--text);
  border:1px solid var(--line);border-radius:4px;font:inherit;font-size:12px;padding:6px 8px;resize:none}
@media (prefers-reduced-motion: reduce){.cell{transition:none}}
"""

js = """
const use = {};    // witness -> option letter to install
const keeps = {};  // witness -> Set of shortlisted letters
const retry = {};  // witness -> true (original clicked)
function summaryLine(w){
  const short = w.replace('witness_','');
  if(use[w]) return short+': '+use[w];
  const ks = keeps[w] && [...keeps[w]].sort();
  if(ks && ks.length) return short+': keep '+ks.join(' ');
  if(retry[w]) return short+': retry';
  return null;
}
function refresh(){
  document.querySelectorAll('.card').forEach(card=>{
    const w = card.id.replace('card-','');
    card.querySelectorAll('.cell').forEach(c=>{
      const p = c.dataset.pick;
      c.classList.toggle('picked', p==='RETRY' ? !!retry[w] : use[w]===p);
      c.classList.toggle('kept-state', !!(keeps[w] && keeps[w].has(p)));
    });
    const st = card.querySelector('.state');
    const ks = keeps[w] && [...keeps[w]].sort();
    if(use[w]){ st.textContent='REPLACE WITH '+use[w]; st.className='state sel'; }
    else if(ks && ks.length){ st.textContent='KEEP '+ks.join('+')+' → +4 NEW NEXT ROUND'; st.className='state keep'; }
    else if(retry[w]){ st.textContent='KEEP ORIGINAL → RETRY'; st.className='state retry'; }
    else { st.textContent='— no choice yet'; st.className='state'; }
  });
  const total = document.querySelectorAll('.card').length;
  const lines = [...document.querySelectorAll('.card')].map(c=>summaryLine(c.id.replace('card-',''))).filter(Boolean);
  document.getElementById('count').textContent = lines.length+' / '+total+' decided';
  document.getElementById('out').value = lines.join(', ');
}
document.addEventListener('click',e=>{
  const kb = e.target.closest('.kbtn');
  if(kb){
    const w = kb.dataset.w, p = kb.dataset.keep;
    keeps[w] = keeps[w] || new Set();
    keeps[w].has(p) ? keeps[w].delete(p) : keeps[w].add(p);
    if(use[w]===p) delete use[w];   // keep and use are mutually exclusive per option
    refresh(); return;
  }
  const cell = e.target.closest('.cell'); if(!cell) return;
  const w = cell.dataset.w, p = cell.dataset.pick;
  if(p==='RETRY'){ retry[w] = !retry[w]; }
  else {
    use[w] = (use[w]===p) ? undefined : p;
    if(use[w]===undefined) delete use[w];
    if(use[w] && keeps[w]) keeps[w].delete(p);
  }
  refresh();
});
document.getElementById('copy').addEventListener('click',()=>{
  const t = document.getElementById('out');
  t.select(); document.execCommand('copy');
  navigator.clipboard && navigator.clipboard.writeText(t.value).catch(()=>{});
  document.getElementById('copy').textContent='COPIED ✓';
  setTimeout(()=>document.getElementById('copy').textContent='COPY CHOICES',1500);
});
refresh();
"""

title = "Witness Replacement Review" + (" — Preview" if PREVIEW else "")
doc = f"""<title>{title}</title>
<style>{css}</style>
<div class="wrap">
<h1>Witness replacement review{' — preview' if PREVIEW else ''}</h1>
<p class="sub">Each row is one witness. The <b style="color:var(--amber)">amber card is the ORIGINAL</b> in the game today. The other cards are nano-banana options: <b style="color:var(--violet)">violet KEPT cards</b> carried over from earlier rounds, and new options <b style="color:var(--cyan)">A–D</b>.</p>
<div class="legend">
  <span><span class="k o"></span><b>ORIGINAL</b> — click = reject all, regenerate</span>
  <span><span class="k u"></span><b>click an option</b> — USE it: replaces the sprite in the game</span>
  <span><span class="k kp"></span><b>KEEP button</b> — shortlist: shows again next round + 4 new options</span>
  <span><span class="k r"></span>red = retry with fresh options</span>
</div>
{note_html}
{''.join(cards)}
</div>
<footer>
  <span class="count" id="count"></span>
  <textarea id="out" readonly aria-label="Summary of choices"></textarea>
  <button id="copy">COPY CHOICES</button>
</footer>
<script>{js}</script>
"""

with open(OUT, "w") as f:
    f.write(doc)
print(f"wrote {OUT}: {len(doc)//1024} KB, {len(complete)} witnesses shown, {len(pending)} pending, {len(decided)} decided")
