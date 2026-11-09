# SPDX-License-Identifier: AGPL-3.0-or-later
# Copyright (C) 2026 Hanry Franco
# Kit de sons do Cryo (proposta da 1.7): todos sintetizados aqui, do zero (nada gravado nem copiado).
# Timbre "cristal de gelo": parciais inarmônicos de um copo de vidro, ataque curto e cauda que some.
# Notas da escala pentatônica (combinam entre si em qualquer ordem). Uso: python3 tools/sons/make_sounds.py <pasta> (precisa de numpy). Proposta da 1.7 (RF-SOM-01): ainda não vai para o app.
import sys, wave, numpy as np

SR = 44100
out = sys.argv[1]

def note(name):
    names = {"C": -9, "D": -7, "E": -5, "F": -4, "G": -2, "A": 0, "B": 2}
    n, o = name[:-1], int(name[-1])
    return 440.0 * 2 ** ((names[n] + (o - 4) * 12) / 12)

def glass(f, dur, decay=0.35, bright=1.0, amp=1.0):
    t = np.arange(int(SR * dur)) / SR
    # Razões dos modos de um copo/sino pequeno (inarmônicos = "vidro").
    partials = [(1.0, 1.0, 1.0), (2.32, 0.45 * bright, 0.55), (4.25, 0.22 * bright, 0.35), (6.63, 0.10 * bright, 0.22)]
    y = np.zeros_like(t)
    for ratio, a, dk in partials:
        fr = f * ratio
        if fr > SR / 2.2: continue
        shimmer = 1 + 0.0015 * np.sin(2 * np.pi * 5.5 * t)   # brilho levemente vivo
        y += a * np.sin(2 * np.pi * fr * t * shimmer) * np.exp(-t / (decay * dk))
    attack = np.minimum(1, t / 0.002)
    return amp * y * attack

def blip(f0, f1, dur, amp=0.6):
    t = np.arange(int(SR * dur)) / SR
    f = f0 + (f1 - f0) * (t / dur) ** 0.7
    ph = 2 * np.pi * np.cumsum(f) / SR
    env = np.sin(np.pi * np.minimum(1, t / dur)) ** 1.5
    return amp * (np.sin(ph) + 0.15 * np.sin(2 * ph)) * env

def crack(dur=0.09, amp=0.35):
    rng = np.random.default_rng(7)
    n = int(SR * dur)
    x = rng.standard_normal(n)
    x = np.convolve(x, np.ones(6) / 6, mode="same")      # tira o chiado mais agudo
    t = np.arange(n) / SR
    return amp * x * np.exp(-t / 0.02)

def mix(parts, total):
    y = np.zeros(int(SR * total))
    for start, sig in parts:
        i = int(SR * start)
        j = min(len(y), i + len(sig))
        y[i:j] += sig[: j - i]
    return y

def reverb(y, wet=0.18):
    # Sala pequena e clara: alguns ecos curtos que somem (sem biblioteca).
    out = y.copy()
    for ms, g in [(23, 0.5), (37, 0.42), (53, 0.33), (79, 0.25), (113, 0.17)]:
        d = int(SR * ms / 1000)
        out[d:] += wet * g * y[:-d]
    return out

def save(name, y, peak_db=-6.0):
    y = reverb(y)
    fade = int(SR * 0.03)
    y[-fade:] *= np.linspace(1, 0, fade)
    y = y / (np.max(np.abs(y)) + 1e-9) * 10 ** (peak_db / 20)
    pcm = (y * 32767).astype("<i2")
    with wave.open(f"{out}/{name}.wav", "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
    return y

S = {}
# 1. Salvar um gasto: um "tim" curto de gelo no copo.
S["salvar"] = save("salvar", mix([(0, glass(note("E6"), 0.45, decay=0.22))], 0.5), -9)
# 2. Entrou dinheiro: duas notas subindo.
S["entrada"] = save("entrada", mix([(0, glass(note("C6"), 0.5, 0.25)), (0.085, glass(note("G6"), 0.6, 0.3, amp=0.9))], 0.75), -7)
# 3. Meta, conquista: arpejo de cristal com brilho no fim.
S["conquista"] = save("conquista", mix([(0.00, glass(note("C6"), 0.9, 0.35)), (0.09, glass(note("E6"), 0.9, 0.35, amp=0.9)),
                                        (0.18, glass(note("G6"), 0.9, 0.38, amp=0.85)), (0.30, glass(note("C7"), 1.2, 0.5, bright=1.3, amp=0.9))], 1.6), -5)
# 4. Atenção (80% do limite): duas notas suaves descendo, mais quentes (menos brilho).
S["atencao"] = save("atencao", mix([(0, glass(note("A5"), 0.6, 0.3, bright=0.5)), (0.16, glass(note("E5"), 0.8, 0.35, bright=0.5))], 1.0), -8)
# 5. Limite estourado: gelo trincando + nota grave curta.
S["limite"] = save("limite", mix([(0, crack()), (0.02, glass(note("E4"), 0.7, 0.25, bright=0.35)), (0.02, glass(note("B4"), 0.5, 0.18, bright=0.3, amp=0.5))], 0.85), -7)
# 6 e 7. Voz começou / terminou: "blip" que sobe e que desce.
S["voz_inicio"] = save("voz_inicio", mix([(0, blip(620, 930, 0.11))], 0.18), -15)
S["voz_fim"] = save("voz_fim", mix([(0, blip(930, 620, 0.11))], 0.18), -15)
# 8. Notificação padrão: sininho de duas notas.
S["notificacao"] = save("notificacao", mix([(0, glass(note("G5"), 0.7, 0.32)), (0.12, glass(note("D6"), 0.9, 0.38))], 1.1), -6)
# 9. Sequência de dias (streak): três notas rápidas subindo.
S["sequencia"] = save("sequencia", mix([(0, glass(note("D6"), 0.5, 0.2)), (0.07, glass(note("E6"), 0.5, 0.2)), (0.14, glass(note("A6"), 0.8, 0.35, bright=1.2))], 1.0), -7)

# Demonstração com tudo em sequência e um silêncio entre cada som.
order = ["salvar", "entrada", "atencao", "limite", "conquista", "sequencia", "notificacao", "voz_inicio", "voz_fim"]
gap = np.zeros(int(SR * 0.9))
demo = np.concatenate(sum([[S[k], gap] for k in order], []))
pcm = (demo * 32767).astype("<i2")
with wave.open(f"{out}/demo.wav", "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())
print("ok", order)
