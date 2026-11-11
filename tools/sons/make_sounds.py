# SPDX-License-Identifier: AGPL-3.0-or-later
# Copyright (C) 2026 Hanry Franco
# Kit de sons do Cryo (RF-SOM), versão da 1.9: todos sintetizados aqui, do zero (nada gravado nem copiado).
#
# Pedido do Hanry (11/11/2026): os sons da 1.7 eram "cintilantes, estridentes". Agora a base é um toque abafado, curto e
# grave, como um trinco macio fechando, e o cristal de gelo fica só como um fio por cima: a mesma família de notas, uma
# oitava abaixo, sem o tilintar do ataque e sem os modos agudos do vidro.
#
# Uso (precisa de numpy; o ffmpeg só para gerar os .ogg do app):
#   python3 tools/sons/make_sounds.py <pasta-dos-wav>                # escreve os .wav e o demo.wav
#   python3 tools/sons/make_sounds.py <pasta-dos-wav> --ogg app/src/main/res/raw   # e também os som_*.ogg do app
#   python3 tools/sons/make_sounds.py <pasta-dos-wav> --medir <pasta-com-wav-antigos>   # compara o brilho com outro kit
import subprocess, sys, wave, numpy as np

SR = 44100
out = sys.argv[1]

def note(name):
    names = {"C": -9, "D": -7, "E": -5, "F": -4, "G": -2, "A": 0, "B": 2}
    n, o = name[:-1], int(name[-1])
    return 440.0 * 2 ** ((names[n] + (o - 4) * 12) / 12)

def shape(y, lo=None, hi=None, order=2):
    """Filtro suave feito na frequência (sem atraso): corta abaixo de [lo] e acima de [hi]."""
    spec = np.fft.rfft(y)
    f = np.fft.rfftfreq(len(y), 1 / SR)
    gain = np.ones_like(f)
    if hi: gain *= 1 / np.sqrt(1 + (f / hi) ** (2 * order))
    if lo: gain *= 1 / np.sqrt(1 + (lo / np.maximum(f, 1e-6)) ** (2 * order))
    return np.fft.irfft(spec * gain, len(y))

def thock(body=340.0, dur=0.13, amp=1.0, seed=11):
    """O toque abafado: um corpo grave que cai de tom (a batida do trinco) e um estalo curto e escuro por cima."""
    n = int(SR * dur)
    t = np.arange(n) / SR
    f = body * (1 + 0.40 * np.exp(-t / 0.012))
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.040)
    click = shape(np.random.default_rng(seed).standard_normal(n), lo=500, hi=1500)
    click = click / (np.max(np.abs(click)) + 1e-9) * np.exp(-t / 0.009)
    y = (0.9 * tone + 0.40 * click) * np.minimum(1, t / 0.003)
    return amp * shape(y, hi=2000)

def soft(f, dur, decay=0.20, amp=1.0, air=0.16):
    """O cristal, de longe: a nota e só um fio do segundo modo do vidro (2,32x), com ataque macio e sem agudos."""
    t = np.arange(int(SR * dur)) / SR
    y = np.sin(2 * np.pi * f * t) * np.exp(-t / decay)
    y += air * np.sin(2 * np.pi * f * 2.32 * t) * np.exp(-t / (decay * 0.4))
    return amp * shape(y * (1 - np.exp(-t / 0.007)), hi=3000)

def blip(f0, f1, dur, amp=0.6):
    t = np.arange(int(SR * dur)) / SR
    f = f0 + (f1 - f0) * (t / dur) ** 0.7
    env = np.sin(np.pi * np.minimum(1, t / dur)) ** 1.5
    return amp * shape(np.sin(2 * np.pi * np.cumsum(f) / SR) * env, hi=2500)

def mix(parts, total):
    y = np.zeros(int(SR * total))
    for start, sig in parts:
        i = int(SR * start)
        j = min(len(y), i + len(sig))
        y[i:j] += sig[: j - i]
    return y

def room(y, wet=0.10):
    # Sala pequena e escura: poucos ecos curtos, já sem agudos (o brilho da sala antiga somava ao tilintar).
    tail = np.zeros_like(y)
    for ms, g in [(19, 0.5), (31, 0.36), (47, 0.24), (71, 0.14)]:
        d = int(SR * ms / 1000)
        tail[d:] += g * y[:-d]
    return y + wet * shape(tail, hi=1800)

def save(name, y, peak_db):
    y = room(y)
    fade = int(SR * 0.03)
    y[-fade:] *= np.linspace(1, 0, fade)
    y = y / (np.max(np.abs(y)) + 1e-9) * 10 ** (peak_db / 20)
    with wave.open(f"{out}/{name}.wav", "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((y * 32767).astype("<i2").tobytes())
    return y

S = {}
# 1. Salvar um gasto: o toque abafado e um fio de cristal.
S["salvar"] = save("salvar", mix([(0, thock()), (0.012, soft(note("E5"), 0.30, 0.13, amp=0.42))], 0.36), -9)
# 2. Entrou dinheiro: o toque e duas notas macias subindo.
S["entrada"] = save("entrada", mix([(0, thock(amp=0.9)), (0.015, soft(note("C5"), 0.34, 0.15, amp=0.55)),
                                    (0.105, soft(note("G5"), 0.42, 0.19, amp=0.55))], 0.60), -8)
# 3. Meta, conquista: arpejo macio, o único som que se demora um pouco.
S["conquista"] = save("conquista", mix([(0, thock(amp=0.8)), (0.015, soft(note("C5"), 0.6, 0.24, amp=0.6)), (0.11, soft(note("E5"), 0.6, 0.24, amp=0.55)),
                                        (0.205, soft(note("G5"), 0.6, 0.26, amp=0.55)), (0.32, soft(note("C6"), 0.85, 0.36, amp=0.6, air=0.22))], 1.25), -7)
# 4. Atenção (80% do limite): duas notas graves descendo, sem batida.
S["atencao"] = save("atencao", mix([(0, soft(note("A4"), 0.42, 0.20, air=0.10)), (0.17, soft(note("E4"), 0.55, 0.24, air=0.10))], 0.78), -9)
# 5. Limite estourado: duas batidas de trinco (a segunda mais grave) e uma nota baixa.
S["limite"] = save("limite", mix([(0, thock(360)), (0.10, thock(280, amp=0.95, seed=5)), (0.115, soft(note("E4"), 0.5, 0.2, amp=0.4, air=0.08))], 0.68), -8)
# 6 e 7. Voz começou / terminou: um "blip" baixo que sobe e que desce.
S["voz_inicio"] = save("voz_inicio", mix([(0, blip(500, 700, 0.11))], 0.18), -15)
S["voz_fim"] = save("voz_fim", mix([(0, blip(700, 500, 0.11))], 0.18), -15)
# 8. Notificação: o toque e duas notas calmas.
S["notificacao"] = save("notificacao", mix([(0, thock(amp=0.85)), (0.015, soft(note("G4"), 0.5, 0.22, amp=0.6)), (0.14, soft(note("D5"), 0.6, 0.26, amp=0.6))], 0.85), -8)
# 9. Sequência de dias: três notas rápidas subindo, cada uma com um toque leve.
S["sequencia"] = save("sequencia", mix([(0, thock(amp=0.6)), (0.01, soft(note("D5"), 0.3, 0.13, amp=0.55)), (0.085, soft(note("E5"), 0.3, 0.13, amp=0.55)),
                                        (0.16, soft(note("A5"), 0.5, 0.22, amp=0.6))], 0.75), -8)

order = ["salvar", "entrada", "atencao", "limite", "conquista", "sequencia", "notificacao", "voz_inicio", "voz_fim"]
gap = np.zeros(int(SR * 0.9))
demo = np.concatenate(sum([[S[k], gap] for k in order], []))
with wave.open(f"{out}/demo.wav", "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes((demo * 32767).astype("<i2").tobytes())

def brightness(y):
    """Onde está o "peso" do som (centro, em Hz) e quanto da energia fica acima de 2 kHz, a faixa que soa estridente."""
    p = np.abs(np.fft.rfft(y)) ** 2
    f = np.fft.rfftfreq(len(y), 1 / SR)
    return (f * p).sum() / p.sum(), p[f > 2000].sum() / p.sum()

def read(path):
    with wave.open(path, "rb") as w:
        return np.frombuffer(w.readframes(w.getnframes()), dtype="<i2").astype(float) / 32767

if "--medir" in sys.argv:
    old = sys.argv[sys.argv.index("--medir") + 1]
    print(f"{'som':<12} {'centro antes':>12} {'agora':>7} {'>2 kHz antes':>13} {'agora':>7}")
    for k in order:
        c0, h0 = brightness(read(f"{old}/som_{k}.wav")); c1, h1 = brightness(S[k])
        print(f"{k:<12} {c0:>10.0f}Hz {c1:>5.0f}Hz {h0 * 100:>12.1f}% {h1 * 100:>6.1f}%")
if "--ogg" in sys.argv:
    raw = sys.argv[sys.argv.index("--ogg") + 1]
    for k in order:
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", f"{out}/{k}.wav", "-c:a", "libvorbis", "-q:a", "4", "-ac", "1", "-ar", str(SR),
                        "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", f"{raw}/som_{k}.ogg"], check=True)
print("ok", order)
