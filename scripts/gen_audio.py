#!/usr/bin/env python3
"""Generate the game's MIDI soundtrack — original compositions, no third-party material.

Emits raw Standard MIDI Files (format 0) with no external dependencies, straight into
android/app/src/main/assets/audio/ where GameSound.kt loads them:

  theme.mid          looping title theme
  jingle_0..10.mid   short event stingers, one per SoundCue (see GameSound.kt for the map)

All melodies here are written from scratch for this game. What *is* modelled on the archived
originals (assets/audio/original/, analysed 2026-08-02) is the ARRANGEMENT STYLE only:
  - 4-6 voice polyphony per cue (the originals run 4-7)
  - the lead melody doubled on a second, brighter program (piano+glockenspiel etc.)
  - the bass line doubled on a contrasting program (the originals' twin low channels)
  - a repeated-note ostinato pulse inside the theme (the originals' theme pulses one pitch)
  - the originals' instrument palette: pianos 0-2, e-pianos 4-5, harpsichord 6, clav 7,
    celesta 8, glockenspiel 9, music box 10
  - jingle lengths ~4-8 beats and tempos in the 130-220 band
  - v3 (2026-08-02): re-voiced DEEPER to match the originals' register (median ~50-60,
    ceiling <=82): doublings at unison/-12 (never +12), high melodies dropped an octave
Re-run to regenerate; tweak the COMPOSITIONS below to taste.
"""
import os
import struct

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "android", "app", "src", "main", "assets", "audio")
DIV = 480  # ticks per quarter note


def vlq(n):
    """Variable-length quantity (MIDI delta-time encoding)."""
    out = bytearray([n & 0x7F])
    n >>= 7
    while n:
        out.append((n & 0x7F) | 0x80)
        n >>= 7
    out.reverse()
    return bytes(out)


class Midi:
    def __init__(self, bpm):
        self.ev = []  # (tick, order, data)
        mpq = 60_000_000 // bpm
        self.ev.append((0, -2, b"\xFF\x51\x03" + mpq.to_bytes(3, "big")))

    def program(self, ch, prog, tick=0):
        self.ev.append((tick, -1, bytes([0xC0 | ch, prog])))

    def note(self, ch, pitch, start, dur, vel=100):
        s = int(start * DIV)
        e = int((start + dur) * DIV)
        self.ev.append((s, 1, bytes([0x90 | ch, pitch, vel])))
        self.ev.append((e, 0, bytes([0x80 | ch, pitch, 0])))

    def chord(self, ch, pitches, start, dur, vel=100):
        for p in pitches:
            self.note(ch, p, start, dur, vel)

    def voice(self, ch, prog, seq, vel=100, gate=0.9, transpose=0):
        """Lay a whole voice down at once. seq = [(pitch | [chord] | None-for-rest, dur_beats)],
        starting at beat 0. gate scales each note's sounding length; transpose shifts pitches —
        together they make the originals' doubled-voice texture a one-liner."""
        self.program(ch, prog)
        t = 0.0
        for pitch, dur in seq:
            if pitch is not None:
                if isinstance(pitch, (list, tuple)):
                    self.chord(ch, [p + transpose for p in pitch], t, dur * gate, vel)
                else:
                    self.note(ch, pitch + transpose, t, dur * gate, vel)
            t += dur
        return t

    def serialize(self):
        evs = sorted(self.ev, key=lambda e: (e[0], e[1]))
        track = bytearray()
        prev = 0
        for tick, _, data in evs:
            track += vlq(tick - prev)
            track += data
            prev = tick
        track += vlq(0) + b"\xFF\x2F\x00"
        head = b"MThd" + struct.pack(">IHHH", 6, 0, 1, DIV)
        return head + b"MTrk" + struct.pack(">I", len(track)) + bytes(track)


# The originals' palette (GM, 0-indexed):
PIANO, BRIGHT, EPIANO, EPIANO2 = 0, 1, 4, 5
HARPSI, CLAV, CELESTA, GLOCK, MUSICBOX = 6, 7, 8, 9, 10


def theme():
    """PICKED round 19: «She slips away — we grin» (DB) — CK's pedal-riff hook + build,
    closed by a cheeky staccato reply and the original-style double-tap. The WIN stinger
    (jingle_9) quotes this hook transposed up, exactly as the original pair does."""
    return multi(150, [
        (1, EPIANO, [(55, 0.5), (58, 0.5), (55, 0.5), (60, 0.5), (55, 0.5), (61, 0.5), (55, 0.5), (62, 0.5), (55, 0.5), (67, 0.5), (58, 0.5), (55, 0.5), (55, 0.5), (60, 0.5), (55, 0.5), (62, 0.5), (55, 0.5), (63, 0.5), (55, 0.5), (65, 0.5), (62, 1), (None, 1), (62, 0.25), (None, 0.25), (62, 0.25), (None, 0.25), (63, 0.25), (None, 0.25), (62, 0.25), (None, 0.25), (60, 0.5), (58, 0.5), (60, 0.5), (None, 0.5), (58, 1), (60, 1), (None, 1), (67, 0.25), (66, 0.25), (64, 0.25), (62, 0.25), (60, 0.25), (58, 0.25), (57, 0.25), (55, 0.25), (55, 0.5), (55, 0.5), (None, 2)], 98, .8, 0),
        (2, MUSICBOX, [(55, 0.5), (58, 0.5), (55, 0.5), (60, 0.5), (55, 0.5), (61, 0.5), (55, 0.5), (62, 0.5), (55, 0.5), (67, 0.5), (58, 0.5), (55, 0.5), (55, 0.5), (60, 0.5), (55, 0.5), (62, 0.5), (55, 0.5), (63, 0.5), (55, 0.5), (65, 0.5), (62, 1), (None, 1), (62, 0.25), (None, 0.25), (62, 0.25), (None, 0.25), (63, 0.25), (None, 0.25), (62, 0.25), (None, 0.25), (60, 0.5), (58, 0.5), (60, 0.5), (None, 0.5), (58, 1), (60, 1), (None, 1), (67, 0.25), (66, 0.25), (64, 0.25), (62, 0.25), (60, 0.25), (58, 0.25), (57, 0.25), (55, 0.25), (55, 0.5), (55, 0.5), (None, 2)], 50, .8, 0),
        (0, BRIGHT, [(None, 8), (67, 0.5), (79, 0.5), (None, 3), (67, 0.5), (77, 0.5), (None, 3), (77, 0.25), (None, 0.75), (77, 0.25), (None, 0.75), (79, 0.5), (76, 0.5), (None, 5)], 88, .8, 0),
        (4, PIANO, [(43, 1), (46, 1), (50, 1), (46, 1), (43, 1), (46, 1), (50, 1), (46, 1), (43, 1), (46, 1), (50, 1), (46, 1), (43, 1), (46, 1), (50, 1), (46, 1), (43, 0.5), (None, 0.5), (43, 0.5), (None, 0.5), (41, 0.5), (None, 0.5), (39, 1), (46, 0.5), (48, 0.5), (50, 0.5), (50, 0.5), (43, 0.5), (43, 0.5), (None, 2.5)], 84, .8, 0),
        (5, CLAV, [(43, 1), (46, 1), (50, 1), (46, 1), (43, 1), (46, 1), (50, 1), (46, 1), (43, 1), (46, 1), (50, 1), (46, 1), (43, 1), (46, 1), (50, 1), (46, 1), (43, 0.5), (None, 0.5), (43, 0.5), (None, 0.5), (41, 0.5), (None, 0.5), (39, 1), (46, 0.5), (48, 0.5), (50, 0.5), (50, 0.5), (43, 0.5), (43, 0.5), (None, 2.5)], 52, .8, 0),
        (3, CELESTA, [(62, 0.5), (62, 0.5), (60, 0.5), (62, 0.5), (62, 0.5), (62, 0.5), (60, 0.5), (62, 0.5), (62, 0.5), (62, 0.5), (60, 0.5), (62, 0.5), (62, 0.5), (62, 0.5), (60, 0.5), (62, 0.5), (62, 0.5), (62, 0.5), (60, 0.5), (62, 0.5), (62, 0.5), (62, 0.5), (60, 0.5), (62, 0.5), (62, 0.5), (62, 0.5), (60, 0.5), (62, 0.5)], 40, .5, 0),
    ])


def multi(bpm, voices):
    """voices: list of (ch, prog, seq, vel, gate, transpose)."""
    m = Midi(bpm)
    for ch, prog, seq, vel, gate, transpose in voices:
        m.voice(ch, prog, seq, vel=vel, gate=gate, transpose=transpose)
    return m


COMPOSITIONS = {
    # jingle_0 CLUE — playful "discovery": rising motif, glock double, walking mini-bass
    "jingle_0": lambda: multi(150, [
        (0, BRIGHT,  [(60, .5), (64, .5), (67, .5), (65, .5), (67, .5), (72, 1.5)], 102, .9, 0),
        (1, GLOCK,   [(60, .5), (64, .5), (67, .5), (65, .5), (67, .5), (72, 1.5)], 64, .9, 0),
        (2, PIANO,   [(48, 1), (52, 1), (55, 1), (48, 1)], 84, .9, 0),
        (3, CLAV,    [(48, 1), (52, 1), (55, 1), (48, 1)], 54, .9, 0),
        (4, EPIANO,  [(None, 1), ([52, 55], 1), (None, 1), ([52, 55, 60], 1)], 62, .8, 0),
    ]),
    # jingle_1 DANGER — PICKED round 10: a shadow passes, two dark chords swaying a semitone
    "jingle_1": lambda: multi(96, [
        (0, EPIANO2, [([38, 45], 1.5), ([39, 46], 1.5), ([38, 45], 1), (None, 1)], 84, .9, 0),
        (1, PIANO,   [(26, 1.5), (27, 1.5), (26, 2)], 80, .9, 0),
        (2, MUSICBOX,[(None, 4), (50, 1)], 78, .8, 0),
    ]),
    # jingle_2 ARRIVE — PICKED round 6: three-note announcement up a fourth, thirds harmony
    "jingle_2": lambda: multi(176, [
        (0, BRIGHT,  [(55, .5), (55, .5), (55, .5), (60, 1), (58, .5), ([57, 60], 2)], 100, .8, 0),
        (1, GLOCK,   [(51, .5), (51, .5), (51, .5), (55, 1), (55, .5), ([53, 57], 2)], 62, .8, 0),
        (2, PIANO,   [(39, .5), (39, .5), (39, .5), (43, 1), (41, .5), ([36, 41], 2)], 88, .8, 0),
        (3, CLAV,    [(39, .5), (39, .5), (39, .5), (43, 1), (41, .5), (36, 2)], 54, .8, 0),
    ]),
    # jingle_3 WRONG_ARREST — PICKED round 5: three falling tritone steps into the cellar
    "jingle_3": lambda: multi(140, [
        (0, EPIANO,  [(57, .75), (54, .75), (51, .75), ([44, 50], 2.5)], 96, .85, 0),
        (1, GLOCK,   [(57, .75), (54, .75), (51, .75), (50, 2.5)], 54, .85, 0),
        (2, PIANO,   [(45, .75), (42, .75), (39, .75), ([32, 38], 2.5)], 92, .8, 0),
        (3, CLAV,    [(45, .75), (42, .75), (39, .75), (32, 2.5)], 56, .8, 0),
    ]),
    # jingle_4 FLASH — the villain's card: dim-7 arpeggio up, trembling echo, dark bass
    "jingle_4": lambda: multi(170, [
        (0, GLOCK,   [(60, .5), (63, .5), (66, .5), (69, .5), (72, 1), (None, .5), (72, .25), (72, .25), (72, 1)], 92, .8, 0),
        (1, BRIGHT,  [(60, .5), (63, .5), (66, .5), (69, .5), (72, 1), (None, 2)], 70, .8, -12),
        (2, PIANO,   [(42, 1), (45, 1), ([36, 42], 2.5)], 86, .8, 0),
        (3, HARPSI,  [(42, 1), (45, 1), (36, 2.5)], 52, .8, 0),
        (4, EPIANO2, [(None, 2), ([57, 60, 66], 2.5)], 58, .8, 0),
    ]),
    # jingle_5 WARRANT — PICKED round 7: a small herald, three rising chord steps, glock on top
    "jingle_5": lambda: multi(160, [
        (0, BRIGHT,  [([48, 52], .75), ([50, 53], .75), ([52, 55], .75), ([48, 55], 2.25)], 100, .8, 0),
        (1, GLOCK,   [(55, .75), (57, .75), (59, .75), (60, 2.25)], 64, .8, 0),
        (2, PIANO,   [(36, .75), (38, .75), (40, .75), ([29, 36], 2.25)], 90, .8, 0),
        (3, HARPSI,  [(36, .75), (38, .75), (40, .75), (29, 2.25)], 54, .8, 0),
    ]),
    # jingle_6 TRAVEL — dreamy pentatonic lift, e-piano wash, soft paired bass
    "jingle_6": lambda: multi(130, [
        (0, EPIANO2, [(48, .5), (50, .5), (52, .5), (55, .5), (57, .5), (60, .5), (62, .5), (67, 1.5)], 88, .95, 0),
        (1, CELESTA, [(None, 1), (52, .5), (55, .5), (57, .5), (60, .5), (62, .5), (67, 1.5)], 58, .95, 0),
        (2, PIANO,   [(48, 2), (43, 2), (48, 1.5)], 76, .95, 0),
        (3, HARPSI,  [(48, 2), (43, 2), (48, 1.5)], 46, .95, 0),
    ]),
    # jingle_7 CHASE — fast dark riff, clav bite, hammering paired bass
    "jingle_7": lambda: multi(210, [
        (0, BRIGHT,  [(57, .5), (57, .5), (60, .5), (57, .5), (62, .5), (60, .5), (57, .5), (55, .5), (57, 1.5)], 104, .7, 0),
        (1, CLAV,    [(57, .5), (57, .5), (60, .5), (57, .5), (62, .5), (60, .5), (57, .5), (55, .5), (57, 1.5)], 72, .7, -12),
        (2, PIANO,   [(33, 1), (33, 1), (36, 1), (38, .5), (36, .5), (33, 1.5)], 96, .6, 0),
        (3, HARPSI,  [(33, 1), (33, 1), (36, 1), (38, .5), (36, .5), (33, 1.5)], 58, .6, 0),
        (4, EPIANO,  [(None, 2), ([45, 48], .5), (None, .5), ([45, 48], .5), (None, .5), ([45, 52], 1.5)], 66, .6, 0),
    ]),
    # jingle_8 BRIEFING — PICKED round 9: the noir question a fourth lower, still unanswered
    "jingle_8": lambda: multi(144, [
        (0, CELESTA, [(43, .75), (46, .75), (50, .75), (49, .75), (52, 2)], 92, .9, 0),
        (1, PIANO,   [(43, .75), (46, .75), (50, .75), (49, .75), (52, 2)], 64, .9, 0),
        (2, EPIANO,  [(None, 1.5), ([38, 43], 1.5), ([37, 43], 2)], 60, .85, 0),
        (3, CLAV,    [(31, 1.5), (31, 1.5), (30, 2)], 54, .6, 0),
    ]),
    # jingle_9 WIN — FINAL (round 22, option N): DB's hook raised & harmonized in thirds
    # (quoting the theme, as the original pair does), flowing straight into the held spread.
    "jingle_9": lambda: multi(176, [
        (0, BRIGHT, [([67,70],.5),([67,70],.5),([70,74],.5),([67,70],.5),([72,76],.5),([67,70],.5),
                     ([73,77],.5),([74,78],.5),([74,79],2.5)], 106, .85, 0),
        (1, GLOCK,  [(64,.5),(64,.5),(67,.5),(64,.5),(69,.5),(64,.5),(70,.5),(71,.5),
                     (79,2.5)], 72, .85, 0),
        (2, PIANO,  [(43,.5),(43,.5),(46,.5),(43,.5),(48,.5),(43,.5),(49,.5),(50,.5),
                     ([31,43],2.5)], 94, .8, 0),
        (3, CLAV,   [(43,.5),(43,.5),(46,.5),(43,.5),(48,.5),(43,.5),(49,.5),(50,.5),
                     (31,2.5)], 58, .8, 0),
    ]),
    # jingle_10 OUT_OF_TIME — somber minor farewell: music box over a sinking pedal
    "jingle_10": lambda: multi(100, [
        (0, MUSICBOX, [(69, 1), (67, 1), (65, 1), (64, .75), (62, .25), (64, 2)], 84, .95, 0),
        (1, PIANO,    [(69, 1), (67, 1), (65, 1), (64, .75), (62, .25), (64, 2)], 56, .95, -12),
        (2, EPIANO2,  [(None, 2), ([57, 60], 2), ([56, 59], 2)], 54, .9, 0),
        (3, PIANO,    [(45, 2), (41, 2), ([40, 45], 2)], 74, .95, 0),
        (4, CELESTA,  [(45, 2), (41, 2), (40, 2)], 40, .95, 0),
    ]),
}


def main():
    os.makedirs(OUT, exist_ok=True)
    written = []
    with open(os.path.join(OUT, "theme.mid"), "wb") as f:
        f.write(theme().serialize())
    written.append("theme.mid")
    for name, build in COMPOSITIONS.items():
        with open(os.path.join(OUT, name + ".mid"), "wb") as f:
            f.write(build().serialize())
        written.append(name + ".mid")
    print(f"wrote {len(written)} files to {os.path.relpath(OUT, HERE)}:")
    for n in written:
        size = os.path.getsize(os.path.join(OUT, n))
        print(f"  {n:16} {size:5} bytes")


if __name__ == "__main__":
    main()
