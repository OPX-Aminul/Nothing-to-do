#!/usr/bin/env python3
"""
Generates every sound the app ships with, procedurally (no third-party assets).

Output: app/src/main/res/raw/*.wav  (mono, 22050 Hz, 16-bit PCM)

Recipes follow common audio-design practice for subtle premium UI cues and
gentle, notice-me-not-jarring reminder motifs:
  * soft raised-cosine attack, exponential decay, short fade-out (no clicks)
  * additive partials kept quiet to avoid a piercing "cheap" character
  * UI cues normalised around −12 dBFS, reminder motifs around −6 dBFS
Run:  python3 tools/gen_sounds.py
"""
import math
import os
import struct
import wave

SR = 22050
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                   "app", "src", "main", "res", "raw")


def render(notes, total, peak=0.25):
    """notes: list of (freq, start_ms, dur_ms, attack_ms, tau_ms, [(mult, gain), ...])"""
    n = int(SR * total / 1000.0)
    buf = [0.0] * n
    for freq, start_ms, dur_ms, attack_ms, tau_ms, partials in notes:
        s = int(SR * start_ms / 1000.0)
        length = int(SR * dur_ms / 1000.0)
        a = max(1, int(SR * attack_ms / 1000.0))
        tau = SR * tau_ms / 1000.0
        for i in range(length):
            idx = s + i
            if idx >= n:
                break
            t = i
            # raised-cosine attack
            env = 0.5 - 0.5 * math.cos(math.pi * t / a) if t < a else 1.0
            env *= math.exp(-max(0, t - a) / tau)
            v = 0.0
            for mult, gain in partials:
                v += gain * math.sin(2 * math.pi * freq * mult * t / SR)
            buf[idx] += env * v
    # final fade to avoid a discontinuity
    fade = int(SR * 0.008)
    for i in range(1, min(fade, n) + 1):
        buf[n - i] *= i / fade
    peak_now = max(1e-9, max(abs(x) for x in buf))
    scale = peak / peak_now
    return [int(max(-32767, min(32767, x * scale * 32767))) for x in buf]


def write(name, samples):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name + ".wav")
    with wave.open(path, "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", s) for s in samples))
    print("wrote %-22s %5.0f ms  %6d bytes" %
          (name + ".wav", 1000.0 * len(samples) / SR, os.path.getsize(path)))


TRI = [(1.0, 1.0), (2.0, 0.12), (3.0, 0.035)]      # bright-ish tap
WARM = [(1.0, 1.0), (2.0, 0.10), (3.0, 0.025)]     # warm low tap

# ---- UI cues -------------------------------------------------------------
# soft button tap (~75 ms)
write("ui_tap", render([
    (740.0, 0, 75, 3, 19, TRI),
], 80, peak=0.25))

# task check-off: two gentle rising notes (~250 ms)
write("ui_complete", render([
    (659.0, 0, 130, 6, 38, [(1.0, 1.0), (2.0, 0.07)]),
    (880.0, 115, 140, 6, 38, [(1.0, 1.0), (2.0, 0.07)]),
], 275, peak=0.28))

# full-week celebration: four-note chime arpeggio (~1.3 s)
write("ui_celebrate", render([
    (659.25, 0, 620, 7, 300, [(1.0, 1.0), (2.01, 0.20), (3.0, 0.05)]),
    (783.99, 140, 620, 7, 300, [(1.0, 1.0), (2.01, 0.20), (3.0, 0.05)]),
    (987.77, 290, 650, 7, 320, [(1.0, 1.0), (2.01, 0.18), (3.0, 0.05)]),
    (1318.5, 460, 820, 8, 380, [(1.0, 1.0), (2.01, 0.16), (3.9, 0.05)]),
], 1320, peak=0.30))

# ---- reminder / alarm templates -----------------------------------------
CHIME = [(1.0, 1.0), (2.01, 0.22), (3.9, 0.06)]
BELL = [(1.0, 1.0), (2.76, 0.18), (5.4, 0.035)]
MARIMBA = [(1.0, 1.0), (3.2, 0.18), (6.0, 0.035)]

write("alarm_chime", render([                       # airy glass chime
    (659.0, 0, 880, 6, 250, CHIME),
    (831.0, 155, 880, 6, 250, CHIME),
    (988.0, 335, 880, 6, 250, CHIME),
], 1250, peak=0.50))

write("alarm_bell", render([                        # soft bell doublet
    (523.25, 0, 1000, 8, 300, BELL),
    (783.99, 260, 1000, 8, 300, BELL),
], 1350, peak=0.50))

write("alarm_marimba", render([                     # rounded marimba
    (587.33, 0, 800, 4, 105, MARIMBA),
    (739.99, 170, 800, 4, 105, MARIMBA),
    (880.0, 365, 800, 4, 110, MARIMBA),
], 1180, peak=0.50))

write("alarm_pulse", render([                       # gentle pulse
    (440.0, 0, 700, 12, 145, [(1.0, 1.0), (2.0, 0.06)]),
    (659.25, 350, 700, 12, 145, [(1.0, 1.0), (2.0, 0.06)]),
], 1100, peak=0.48))

write("alarm_digital", render([                     # clean digital pair
    (880.0, 0, 520, 5, 95, [(1.0, 1.0), (2.0, 0.035)]),
    (1046.5, 145, 520, 5, 95, [(1.0, 1.0), (2.0, 0.035)]),
], 720, peak=0.44))

# default reminder: chime repeated once after a quiet gap (~2.2 s)
write("alarm_default", render([
    (659.0, 0, 880, 6, 250, CHIME),
    (831.0, 155, 880, 6, 250, CHIME),
    (988.0, 335, 880, 6, 250, CHIME),
    (659.0, 1150, 880, 6, 250, CHIME),
    (831.0, 1305, 880, 6, 250, CHIME),
    (988.0, 1485, 880, 6, 250, CHIME),
], 2350, peak=0.50))
