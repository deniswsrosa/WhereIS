#!/usr/bin/env python3
"""Generate the short PCM sound effects in assets/audio/ (16-bit mono 22050 Hz, like type_click.wav).

  step.wav  footstep "tap" for the walk-to-venue and chase animations: a low-passed noise
            scuff over a quick decaying low thump.
  tick.wav  soft clock tick for each hour that passes in the last day before the deadline:
            a short woodblock-like click.

Deterministic (fixed seed) so reruns are byte-identical.
"""
import math
import random
import struct
import wave
from pathlib import Path

RATE = 22050
AUDIO = Path(__file__).resolve().parent.parent / "android/app/src/main/assets/audio"


def write(name, samples, level):
    peak = max(abs(x) for x in samples)
    out = AUDIO / name
    with wave.open(str(out), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(x / peak * level * 32767)) for x in samples))
    print(f"wrote {out} ({len(samples)} frames)")


def step():
    rng = random.Random(7)
    samples, lp = [], 0.0
    for i in range(int(RATE * 0.09)):
        t = i / RATE
        lp += 0.18 * (rng.uniform(-1, 1) - lp)            # sole-on-pavement texture
        scuff = lp * math.exp(-t * 55)
        freq = 70 + 70 * math.exp(-t * 40)                # heel thump, 140 -> 70 Hz
        thump = math.sin(2 * math.pi * freq * t) * math.exp(-t * 45)
        attack = min(1.0, i / (RATE * 0.002))
        samples.append(attack * (0.55 * thump + 0.9 * scuff))
    return samples


def tick():
    rng = random.Random(11)
    samples = []
    for i in range(int(RATE * 0.035)):
        t = i / RATE
        body = math.sin(2 * math.pi * 1900 * t) + 0.5 * math.sin(2 * math.pi * 3100 * t)
        click = rng.uniform(-1, 1) * math.exp(-t * 900)
        samples.append((0.7 * body * math.exp(-t * 160)) + 0.4 * click)
    return samples


write("step.wav", step(), 0.8)
write("tick.wav", tick(), 0.7)
