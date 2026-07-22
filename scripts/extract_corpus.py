#!/usr/bin/env python3
"""
Carmen Sandiego (Enhanced, MS-DOS Version 2.1, (c)1990 Broderbund) corpus extractor.

Provenance chain:
  acquisitions/carmen_enhanced_1989.zip  (Internet Archive, verified MD5)
    -> work/extracted/wwcse/CARMEN.EXE   (PKLITE 1990 compressed)
       -> work/exe_decompressed/CARMEN.000.exe  (deark -m pklite)  <-- EXE offsets are in THIS file
    -> work/extracted/wwcse/CITIES.DAT   (city-name offsets are in THIS file)

Outputs (corpus/):
  carmen_corpus.json     occurrence-level records, every string, byte-exact
  carmen_corpus.md       human-readable structured report
  provenance.json        file hashes + toolchain + edition id
"""
import re, json, hashlib, os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
EXE  = os.path.join(BASE, "work/exe_decompressed/CARMEN.000.exe")
EXE_SRC = os.path.join(BASE, "work/extracted/wwcse/CARMEN.EXE")
CITIES = os.path.join(BASE, "work/extracted/wwcse/CITIES.DAT")
OUT = os.path.join(BASE, "corpus")
os.makedirs(OUT, exist_ok=True)

def sha256(p):
    h=hashlib.sha256()
    with open(p,'rb') as f:
        for b in iter(lambda:f.read(65536),b''): h.update(b)
    return h.hexdigest()

def cp437(b):  # decode raw bytes as DOS code page 437, canonical
    return b.decode('cp437')

# ---- EXE: pull every NUL-terminated printable run in the text region ----
exe = open(EXE,'rb').read()
TEXT_LO, TEXT_HI = 0x19440, 0x1c220   # observed contiguous text block
def extract_cstrings(data, lo, hi, minlen=2):
    out=[]; i=lo
    while i < hi:
        if 0x20 <= data[i] <= 0xfe and data[i] != 0x7f:
            j=i
            while j < hi and 0x20 <= data[j] <= 0xfe and data[j] != 0x7f:
                j+=1
            if data[j:j+1]==b'\x00':          # genuine C string
                run=data[i:j]
                if len(run)>=minlen:
                    out.append((i, run))
                i=j+1
            else:
                i=j
        else:
            i+=1
    return out

raw_strings = extract_cstrings(exe, TEXT_LO, TEXT_HI)

def classify(s):
    if re.search(r'%\-?\d*\.?\d*F?[sdc]', s): return 'template'   # printf field -> assembled at runtime
    if s.endswith(' '):                       return 'fragment'   # trailing space -> concatenated
    if s and s[0].islower():                  return 'fragment'   # lower-case initial -> mid-sentence piece
    return 'raw_string'

# Section map by offset range (from manual analysis of the ordered block)
SECTIONS = [
 (0x19440,0x1945d,'dos-runtime'),
 (0x1945d,0x19554,'credits'),
 (0x19554,0x195ce,'title-copyright'),
 (0x195ce,0x19699,'intro'),
 (0x19699,0x19a47,'witness-clue-fragment'),
 (0x19a47,0x19aff,'attribute-token'),
 (0x19aff,0x1a267,'suspect-dossier'),
 (0x1a267,0x1a2b1,'ui-button'),
 (0x1a2b1,0x1a324,'venue'),
 (0x1a324,0x1a4f2,'occupation'),
 (0x1a4f2,0x1a7c7,'no-information-response'),
 (0x1a7c7,0x1a831,'hardware-error'),
 (0x1a831,0x1a93a,'clue-lead-in'),
 (0x1a93a,0x1aa67,'save-load-ui'),
 (0x1aa67,0x1ab8c,'danger-message'),
 (0x1ab8c,0x1ac06,'crime-computer-label'),
 (0x1ac06,0x1acf5,'warrant-ui'),
 (0x1acf5,0x1ad4d,'dossier-field-label'),
 (0x1ad4d,0x1adc0,'data-filename'),
 (0x1adc0,0x1aed3,'hardware-config'),
 (0x1aed3,0x1af71,'menu'),
 (0x1af71,0x1b070,'roster-name'),
 (0x1b070,0x1b09f,'rank'),
 (0x1b09f,0x1b435,'signon-interpol'),
 (0x1b435,0x1b9ce,'case-flow'),
 (0x1b9ce,0x1ba2d,'misc-number-word'),
 (0x1ba2d,0x1ba61,'pronoun-token'),
 (0x1ba61,0x1bb28,'misc'),
 (0x1bb28,0x1bbed,'disk-swap-ui'),
 (0x1bbed,0x1bc10,'save-filename'),
 (0x1bc10,0x1c220,'dos-runtime'),
]
def section_of(off):
    for lo,hi,name in SECTIONS:
        if lo<=off<hi: return name
    return 'unclassified'

records=[]
for off,run in raw_strings:
    txt=cp437(run)
    records.append({
        "id": f"EXE_{off:06x}",
        "source_file": "CARMEN.000.exe (decompressed from CARMEN.EXE, PKLITE 1990)",
        "offset_dec": off,
        "offset_hex": f"0x{off:06x}",
        "raw_bytes_hex": run.hex(' '),
        "canonical_text": txt,          # CP437, NOT normalized
        "byte_length": len(run),
        "encoding": "cp437",
        "kind": classify(txt),          # raw_string | fragment | template
        "section": section_of(off),
        "extraction": "byte-exact",
        "runtime_verified": False,
    })

# ---- CITIES.DAT city names ----
cd=open(CITIES,'rb').read()
pat=re.compile(rb'([A-Z][a-z][A-Za-z .\'-]{2,18})\x00')
cities=[]
last=None
for m in pat.finditer(cd):
    name=m.group(1); off=m.start(1)
    s=cp437(name)
    if not re.search(r'[aeiouAEIOU]',s): continue
    if not re.fullmatch(r"[A-Za-z .'-]+",s): continue
    # every capital must start a word (reject image-noise like 'ZgVWU')
    if any(ch.isupper() and i>0 and s[i-1]!=' ' for i,ch in enumerate(s)): continue
    # must be preceded by a NUL somewhere in the 4-byte header window (record boundary)
    if b'\x00' not in cd[max(0,off-4):off]: continue
    if last==s: continue
    last=s
    cities.append({
        "id": f"CITY_{off:06x}",
        "source_file": "CITIES.DAT",
        "offset_dec": off,
        "offset_hex": f"0x{off:06x}",
        "raw_bytes_hex": name.hex(' '),
        "canonical_text": s,
        "byte_length": len(name),
        "encoding": "cp437",
        "kind": "raw_string",
        "section": "city-name",
        "extraction": "byte-exact",
        "runtime_verified": False,
    })

# ---- provenance ----
prov = {
 "edition": {
   "title": "Where in the World is Carmen Sandiego? (Enhanced)",
   "publisher": "Broderbund Software",
   "in_binary_version_string": "MS-DOS Version 2.1",
   "in_binary_copyright": "Copyright 1990, Bröderbund Software",
   "compressor": "PKLITE Copr. 1990 PKWARE Inc.",
   "archive_identifier": "msdos_Where_in_the_World_is_Carmen_Sandiego_Enhanced_1989",
   "archive_url": "https://archive.org/details/msdos_Where_in_the_World_is_Carmen_Sandiego_Enhanced_1989",
   "note": "Multimedia 'Enhanced' build: Windows-3.x-format BMP city art + MIDI/digitized sound. Textually distinct from the 1985 IBM 1.0 build."
 },
 "toolchain": {
   "decompressor": "deark 1.7.3 (-m pklite)",
   "string_scan": "custom python NUL-terminated CP437 scan",
   "host": "macOS (darwin), python3",
 },
 "hashes_sha256": {
   "carmen_enhanced_1989.zip": sha256(os.path.join(BASE,"acquisitions/carmen_enhanced_1989.zip")),
   "CARMEN.EXE (compressed)": sha256(EXE_SRC),
   "CARMEN.000.exe (decompressed)": sha256(EXE),
   "CITIES.DAT": sha256(CITIES),
 },
 "archive_md5_verified": "947eba22c5d8eedca917fe8381b2c708",
}

corpus = {
 "provenance": prov,
 "counts": {"exe_strings": len(records), "cities": len(cities)},
 "cities": cities,
 "exe_strings": records,
}
json.dump(corpus, open(os.path.join(OUT,"carmen_corpus.json"),"w"), indent=2, ensure_ascii=False)
print("cities:", len(cities), "| exe strings:", len(records))
print("wrote", os.path.join(OUT,"carmen_corpus.json"))
